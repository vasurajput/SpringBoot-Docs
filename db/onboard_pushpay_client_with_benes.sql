-- ============================================================================
-- onboard_pushpay_client_with_benes  (design.md §6)
-- One-time DDL, deployed by App Support/DBA. Signature is frozen - future column
-- changes touch only the function BODY (CREATE OR REPLACE, never DROP).
--
-- STATUS: skeleton only. The TODO blocks below correspond 1:1 to the open items in
-- design.md §10 and MUST be filled in against the real schema before this is deployed
-- to any environment. Do not run this as-is.
-- ============================================================================

CREATE OR REPLACE FUNCTION onboard_pushpay_client_with_benes(
    p_template_client_id BIGINT,
    p_client JSONB,
    p_benes  JSONB DEFAULT '[]'::jsonb
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_client        public.t_geo_client;
    v_client_id     BIGINT;
    v_bene          public.t_geo_beneficiary;
    v_bene_id       BIGINT;
    v_bene_json     JSONB;
    v_key           TEXT;

    -- Keep these two arrays in sync with pushpay.client.columns / pushpay.bene.columns
    -- in application.yml (see design.md §8 add/remove runbook). "name" is always
    -- allowed on the client side even though it isn't in pushpay.client.columns
    -- (it's the one mandatory field, handled separately from the optional overrides).
    v_client_allowed_keys TEXT[] := ARRAY[
        'name', 'client_address', 'country', 'client_city', 'deal_review_notification',
        'country_segment', 'website_url', 'contact_email', 'cct_account_number',
        'transaction_manager', 'legal_entity', 'primary_email', 'industry_segment',
        'seller_type', 'debtor_portal_default_lang', 'seller_payment_type'
    ];
    v_bene_allowed_keys TEXT[] := ARRAY[
        'bene_name', 'account_number', 'bene_bank_name', 'routing_code', 'swift_code',
        'bank_code', 'bene_addr_address', 'bene_addr_country', 'bene_addr_city',
        'bene_addr_province', 'bene_addr_post_code', 'bank_addr_address',
        'bank_addr_country', 'bank_addr_city', 'bank_addr_province', 'bank_addr_post_code'
        -- TODO (design.md §10.1/§10.3): confirm these against the real t_geo_beneficiary
        -- schema (e.g. cotact_email vs contact_email, provience vs province typos to check
        -- for), and mirror ANY change here into pushpay.bene.columns in application.yml.
    ];
BEGIN
    -- ------------------------------------------------------------------
    -- 1. Validate (fail fast)
    -- ------------------------------------------------------------------
    SELECT * INTO STRICT v_client FROM public.t_geo_client WHERE id = p_template_client_id;
    -- TODO (design.md §10.5): confirm the PushPay-enabled flag column on t_geo_client and
    -- assert it here, e.g.:
    --   IF NOT v_client.is_pushpay_enabled THEN
    --       RAISE EXCEPTION 'Template client % is not PushPay-enabled', p_template_client_id;
    --   END IF;

    IF p_client->>'name' IS NULL OR btrim(p_client->>'name') = '' THEN
        RAISE EXCEPTION 'p_client.name is required';
    END IF;

    FOR v_key IN SELECT jsonb_object_keys(p_client) LOOP
        IF NOT (v_key = ANY (v_client_allowed_keys)) THEN
            RAISE EXCEPTION 'Unknown client field "%" - update the function before onboarding with this column', v_key;
        END IF;
    END LOOP;

    FOR v_bene_json IN SELECT * FROM jsonb_array_elements(p_benes) LOOP
        FOR v_key IN SELECT jsonb_object_keys(v_bene_json) LOOP
            IF NOT (v_key = ANY (v_bene_allowed_keys)) THEN
                RAISE EXCEPTION 'Unknown bene field "%" - update the function before onboarding with this column', v_key;
            END IF;
        END LOOP;
    END LOOP;

    -- TODO (design.md §10.4): duplicate-client guard - business rule pending from product
    -- (reject if a PushPay client with the same name, or name+country, already exists?).
    -- IF EXISTS (SELECT 1 FROM public.t_geo_client WHERE name = p_client->>'name' ...) THEN
    --     RAISE EXCEPTION 'A PushPay client named "%" already exists', p_client->>'name';
    -- END IF;

    -- ------------------------------------------------------------------
    -- 2. Main client insert - template clone + Excel overrides
    -- ------------------------------------------------------------------
    v_client := jsonb_populate_record(v_client, p_client);
    v_client.id := nextval('cma_seq');
    v_client.created_at := now();
    -- TODO (design.md §10.2): list ALL fresh-value columns (created_at, updated_at, etc.)
    -- and any unique/per-client columns that must NOT be copied verbatim from the
    -- template (e.g. external reference codes) - reset/regenerate those here.

    INSERT INTO public.t_geo_client VALUES (v_client.*) RETURNING id INTO v_client_id;

    -- ------------------------------------------------------------------
    -- 3. Child config tables - plain INSERT..SELECT per table, client_id swapped
    -- ------------------------------------------------------------------
    -- TODO (design.md §10.2): final list of the ~34 config tables to clone (confirm with
    -- product which are config/copy-on-onboard vs history/transactional/never-copy). For
    -- each table: how its PK is generated (omit column if DEFAULT, nextval(...) if manual)
    -- and any unique columns that must be regenerated instead of copied verbatim.
    --
    -- Example shape once a table is confirmed:
    -- INSERT INTO public.t_client_currency (client_id, currency_code, is_default)
    -- SELECT v_client_id, currency_code, is_default
    -- FROM public.t_client_currency WHERE client_id = p_template_client_id;

    -- ------------------------------------------------------------------
    -- 4. Beneficiaries - pure Excel data, no template
    -- ------------------------------------------------------------------
    FOR v_bene_json IN SELECT * FROM jsonb_array_elements(p_benes) LOOP
        v_bene := NULL::public.t_geo_beneficiary;
        v_bene := jsonb_populate_record(v_bene, v_bene_json);
        v_bene.id               := nextval('bene_seq'); -- TODO (design.md §10.3): real sequence name
        v_bene.owning_client_id := v_client_id;
        v_bene.profile_id       := v_client_id;
        -- TODO (design.md §10.3): set created_at/updated_at or other system columns here
        -- if present - VALUES(v_bene.*) bypasses column DEFAULTs, so anything relying on
        -- a DEFAULT must be assigned explicitly.

        INSERT INTO public.t_geo_beneficiary VALUES (v_bene.*) RETURNING id INTO v_bene_id;

        -- TODO (design.md §10.3): bene reference/config child tables - list them and what
        -- each insert needs (direct inserts using v_bene_id / v_client_id; no template
        -- copy, since there is no bene template).

        RAISE NOTICE '  bene % created with id %', v_bene_json->>'bene_name', v_bene_id;
    END LOOP;

    RAISE NOTICE 'Client % created with id %', p_client->>'name', v_client_id;
    RETURN v_client_id;
END;
$$;
