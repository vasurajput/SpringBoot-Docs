-- ============================================================================
-- LEARNING SANDBOX - not part of the real project, not called by anything.
-- Run this ENTIRE file against any scratch/throwaway Postgres database
-- (a local install, Docker, whatever) with:  psql -f learn_client_bene_demo.sql
-- or just paste it into a SQL client. It creates its own tiny made-up tables,
-- so it needs no access to the real PushPay schema at all.
--
-- Goal: see, with your own eyes, the 3 things you asked about:
--   1. create a client from an existing one + Excel overrides
--   2. get the new client's id back
--   3. loop over a bene JSON array, reusing that id for each bene
-- plus how validation fires when something's wrong.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1. Tiny made-up schema (4-5 columns each, not the real ~200/~16 columns -
--    the point is to see the MECHANICS, not memorize real column names)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS demo_beneficiary;
DROP TABLE IF EXISTS demo_client;
DROP SEQUENCE IF EXISTS demo_client_seq;
DROP SEQUENCE IF EXISTS demo_bene_seq;

CREATE SEQUENCE demo_client_seq;
CREATE SEQUENCE demo_bene_seq;

CREATE TABLE demo_client (
    id                  BIGINT PRIMARY KEY,
    name                TEXT NOT NULL,
    country             TEXT,
    transaction_manager TEXT,
    contact_email       TEXT,
    created_at          TIMESTAMP
);

CREATE TABLE demo_beneficiary (
    id               BIGINT PRIMARY KEY,
    bene_name        TEXT NOT NULL,
    account_number   TEXT NOT NULL,
    bene_bank_name   TEXT,
    owning_client_id BIGINT NOT NULL REFERENCES demo_client(id),
    profile_id       BIGINT NOT NULL
);

-- seed the ONE existing "template" client everything else gets cloned from
INSERT INTO demo_client (id, name, country, transaction_manager, contact_email, created_at)
VALUES (1001, 'Template Client Inc', 'USA', 'TM-DEFAULT', 'template@example.com', now());


-- ----------------------------------------------------------------------------
-- 2. The function itself - same shape/logic as the real one, tiny schema
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION demo_onboard_client_with_benes(
    p_template_client_id BIGINT,
    p_client              JSONB,
    p_benes               JSONB DEFAULT '[]'::jsonb
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_client    demo_client;      -- row-shaped variable (see WALKTHROUGH file)
    v_client_id BIGINT;
    v_bene      demo_beneficiary;
    v_bene_id   BIGINT;
    v_bene_json JSONB;
    v_key       TEXT;

    -- only fields listed here are allowed through - everything else is
    -- rejected loudly instead of silently ignored
    v_client_allowed_keys TEXT[] := ARRAY['name', 'country', 'transaction_manager', 'contact_email'];
    v_bene_allowed_keys   TEXT[] := ARRAY['bene_name', 'account_number', 'bene_bank_name'];
BEGIN
    -- ============================================================
    -- VALIDATE (fail fast, before touching any table)
    -- ============================================================

    -- (a) template client must exist - STRICT does this for free
    SELECT * INTO STRICT v_client FROM demo_client WHERE id = p_template_client_id;

    -- (b) name is mandatory
    IF p_client->>'name' IS NULL OR btrim(p_client->>'name') = '' THEN
        RAISE EXCEPTION 'p_client.name is required';
    END IF;

    -- (c) every client field Excel sent must be one we recognize
    FOR v_key IN SELECT jsonb_object_keys(p_client) LOOP
        IF NOT (v_key = ANY (v_client_allowed_keys)) THEN
            RAISE EXCEPTION 'Unknown client field "%"', v_key;
        END IF;
    END LOOP;

    -- (d) same check, for every bene's fields
    FOR v_bene_json IN SELECT * FROM jsonb_array_elements(p_benes) LOOP
        FOR v_key IN SELECT jsonb_object_keys(v_bene_json) LOOP
            IF NOT (v_key = ANY (v_bene_allowed_keys)) THEN
                RAISE EXCEPTION 'Unknown bene field "%"', v_key;
            END IF;
        END LOOP;
    END LOOP;

    -- ============================================================
    -- CREATE THE CLIENT: clone template row, overlay Excel fields, insert
    -- ============================================================
    v_client := jsonb_populate_record(v_client, p_client);
    -- v_client is now: template's values, except whatever p_client overrode

    v_client.id := nextval('demo_client_seq');   -- fresh id, not the template's
    v_client.created_at := now();

    INSERT INTO demo_client VALUES (v_client.*)
    RETURNING id INTO v_client_id;               -- <-- capture the new id here

    RAISE NOTICE 'Created client "%" with id %', v_client.name, v_client_id;

    -- ============================================================
    -- CREATE THE BENEFICIARIES: loop the JSON array, reuse v_client_id
    -- ============================================================
    FOR v_bene_json IN SELECT * FROM jsonb_array_elements(p_benes) LOOP
        v_bene := NULL::demo_beneficiary;          -- empty row - no template
        v_bene := jsonb_populate_record(v_bene, v_bene_json);

        v_bene.id               := nextval('demo_bene_seq');
        v_bene.owning_client_id := v_client_id;     -- <-- reused from above
        v_bene.profile_id       := v_client_id;     -- <-- reused from above

        INSERT INTO demo_beneficiary VALUES (v_bene.*)
        RETURNING id INTO v_bene_id;

        RAISE NOTICE '  Created bene "%" with id % (owning_client_id=%)',
            v_bene.bene_name, v_bene_id, v_client_id;
    END LOOP;

    RETURN v_client_id;
END;
$$;


-- ----------------------------------------------------------------------------
-- 3. Run a GOOD call and look at what actually landed in the tables
-- ----------------------------------------------------------------------------
SELECT demo_onboard_client_with_benes(
    p_template_client_id => 1001,
    p_client => '{"name": "Acme Trading LLC", "country": "India"}'::jsonb,
    p_benes  => '[
        {"bene_name": "Supplier One", "account_number": "004501234567"},
        {"bene_name": "Supplier Two", "account_number": "009988776655", "bene_bank_name": "ICICI Bank"}
    ]'::jsonb
);

-- notice: transaction_manager and contact_email are NOT in p_client above,
-- so this new row should show the TEMPLATE's values for those two columns.
SELECT id, name, country, transaction_manager, contact_email FROM demo_client;

-- notice: both benes show owning_client_id/profile_id pointing at the SAME
-- new client id, even though they came from different JSON array elements.
SELECT id, bene_name, account_number, owning_client_id, profile_id FROM demo_beneficiary;


-- ----------------------------------------------------------------------------
-- 4. Now break it on purpose, one at a time, and read the error each throws.
--    Uncomment ONE block at a time and re-run just that block.
-- ----------------------------------------------------------------------------

-- (a) template client id doesn't exist -> STRICT raises "no rows found"
-- SELECT demo_onboard_client_with_benes(9999, '{"name":"X"}'::jsonb);

-- (b) name missing -> our own explicit RAISE EXCEPTION fires
-- SELECT demo_onboard_client_with_benes(1001, '{"country":"India"}'::jsonb);

-- (c) unknown/typo'd client field -> allow-list RAISE EXCEPTION fires
-- SELECT demo_onboard_client_with_benes(1001, '{"name":"X","mad_up_field":"y"}'::jsonb);

-- (d) unknown/typo'd bene field -> allow-list RAISE EXCEPTION fires
-- SELECT demo_onboard_client_with_benes(1001, '{"name":"X"}'::jsonb,
--     '[{"bene_name":"B","account_number":"1","mad_up_field":"y"}]'::jsonb);

-- (e) bene missing a NOT NULL column (account_number) -> no explicit check
--     catches this; the plain INSERT itself fails with a constraint error,
--     because jsonb_populate_record just leaves account_number as NULL and
--     the table schema itself refuses to store it that way
-- SELECT demo_onboard_client_with_benes(1001, '{"name":"X"}'::jsonb,
--     '[{"bene_name":"B"}]'::jsonb);


-- ----------------------------------------------------------------------------
-- 5. Cleanup (drop everything this script created, once you're done playing)
-- ----------------------------------------------------------------------------
-- DROP TABLE demo_beneficiary;
-- DROP TABLE demo_client;
-- DROP SEQUENCE demo_client_seq;
-- DROP SEQUENCE demo_bene_seq;
-- DROP FUNCTION demo_onboard_client_with_benes(BIGINT, JSONB, JSONB);
