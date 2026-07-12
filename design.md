# PushPay Client Onboarding Automation — Design Document

**Owner:** Vihu | **Status:** Final design, ready for implementation | **Date:** 2026-07-10

## 0. Tech Stack (fixed — do not deviate)

- **Java 21**, **Spring Boot 3.x** — CLI application using `CommandLineRunner`
  (NOT a web app: `spring.main.web-application-type=none`; no server starts)
- **NO database connectivity of any kind** — do not add spring-boot-starter-jdbc/jpa or
  any datasource config. This tool only reads an Excel file and writes .sql/.txt files.
  (Direct DB access from the utility is impossible in this environment — CyberArk, §1.)
- **Apache POI 5.x** (poi-ooxml) for .xlsx reading
- Column configuration via **`application.yml` + `@ConfigurationProperties`**
  (type-safe config class, e.g. prefix `pushpay`), externally overridable the standard
  Spring way (`--spring.config.additional-location=...` or env vars) so column
  add/remove needs no rebuild
- **Maven** build with spring-boot-maven-plugin (runnable fat jar `pushpay-gen.jar`)
- Generated SQL targets **PostgreSQL 14+** (PL/pgSQL, jsonb, jsonb_populate_record,
  named function parameters `=>`) — but the utility itself never connects to it
- CLI contract: `java -jar pushpay-gen.jar <input.xlsx> [--template-client <id>]
  [--out batch.sql]`; on validation failure print all errors and exit code 2 via
  `System.exit` / `ExitCodeGenerator`
- **Template client is per-row, not a single global value** (revised 2026-07-12): the
  Clients sheet carries a `template_client_id` column so different clients in the same
  batch can clone from different reference clients. `--template-client` is only a
  fallback default for rows that leave that column blank. If a row resolves to neither
  a filled-in column value nor a CLI default, validation fails and nothing is generated
  (fast-fail, same as any other validation error - see §5).

## 1. Problem

Product team needs to onboard new PushPay clients (with their beneficiaries) in bulk.
Client/bene data comes in an Excel file. A new client must be created in `t_geo_client`
plus ~34 related config tables. **Client model:** fields present in the Excel override;
everything else is copied from an existing "template" (reference) client that is already
PushPay-enabled. **Beneficiary model (no template):** the Excel carries ALL beneficiary
data; benes are inserted purely from Excel values into `t_geo_beneficiary` (+ related
bene tables). The only runtime-derived bene values are `owning_client_id` and
`profile_id` — BOTH are set to the newly created client's id.

**Environment constraint:** Production DB is managed via CyberArk; we cannot connect a
utility directly to prod, and file transfer into DB sessions is not possible. App
Support/DBA can only run SQL scripts we hand them. Therefore: the utility generates a
self-contained `.sql` file; humans execute it.

## 2. Architecture Overview

```
Product team fills Excel (Clients + Beneficiaries sheets)
        │
        ▼
Java utility (this repo):  read (Apache POI) → validate → generate batch.sql + summary.txt
        │
        ▼
App Support / DBA runs batch.sql (one flat SELECT per client)
        │
        ▼
Each SELECT calls DB function onboard_pushpay_client_with_benes(...)
  → creates client (template clone + Excel overrides)
  → creates all its beneficiaries in the same call (JSONB array; pure Excel data,
    no bene template — owning_client_id & profile_id = new client id)
  → atomic per client: ANY failure (any table, any bene) rolls back that entire client
        │
        ▼
DBA pastes console NOTICE output on the ticket = execution report
```

No staging/audit tables in v1 (deliberately deferred — see §9 Phase 2).

## 3. Excel Template

File has 3 sheets. Headers are **exact DB column names**. `client_ref` (C1, C2, ...) is a
file-internal linking key only — never stored in DB.

**Sheet "Clients"** — one row per new client:
- `client_ref` (mandatory, unique within file)
- `name` (mandatory)
- `template_client_id` (mandatory unless `--template-client <id>` was passed on the CLI
  as a batch-wide default; blank cells then fall back to that default). This is the id of
  the existing PushPay-enabled client to clone for this row — different rows may name
  different template clients. Not a data column: it selects `p_template_client_id` for
  that row's `onboard_pushpay_client_with_benes(...)` call, never part of `p_client`.
- Optional override columns (blank ⇒ value copied from template client):
  `client_address, country, client_city, deal_review_notification, country_segment,
  website_url, contact_email, cct_account_number, transaction_manager, legal_entity,
  primary_email, industry_segment, seller_type, debtor_portal_default_lang,
  seller_payment_type`

**Sheet "Beneficiaries"** — one row per beneficiary. **No template/reference for benes:
this sheet carries the COMPLETE bene data.** Blank is allowed only for genuinely nullable
columns (blank ⇒ NULL in DB, not a template value — there is no template).
- `client_ref` (mandatory, must match a Clients row)
- Data columns: `bene_name, account_number, bene_bank_name, routing_code, swift_code,
  bank_code, bene_addr_address, bene_addr_country, bene_addr_city, bene_addr_province,
  bene_addr_post_code, bank_addr_address, bank_addr_country, bank_addr_city,
  bank_addr_province, bank_addr_post_code`
  <!-- TODO: verify column names against the real t_geo_beneficiary schema -->
- Mandatory set = every NOT NULL column of t_geo_beneficiary (since nothing comes from a
  template). <!-- TODO: derive final mandatory list from schema (\d t_geo_beneficiary)
  and mirror it in pushpay.bene.mandatory in application.yml -->

**Sheet "READ_ME"** — instructions for the product team.

**Batch model:** every batch = a fresh file containing ONLY new clients. Never cumulative.
A client's beneficiaries must be in the same file as the client. Filename convention:
`PushPay_Onboarding_YYYY-MM-DD.xlsx`.

## 4. Column configuration (Spring `application.yml` + `@ConfigurationProperties`)

Single source of truth for which Excel headers the utility accepts. Bind to a type-safe
config record (prefix `pushpay`). Adding/removing a column later = edit an external yml
override (no rebuild): run with
`--spring.config.additional-location=file:./pushpay-columns.yml`.

```yaml
pushpay:
  client:
    columns: [name, client_address, country, client_city, deal_review_notification,
              country_segment, website_url, contact_email, cct_account_number,
              transaction_manager, legal_entity, primary_email, industry_segment,
              seller_type, debtor_portal_default_lang, seller_payment_type]
    mandatory: [name]
  bene:
    columns: [bene_name, account_number, bene_bank_name, routing_code, swift_code,
              bank_code, bene_addr_address, bene_addr_country, bene_addr_city,
              bene_addr_province, bene_addr_post_code, bank_addr_address,
              bank_addr_country, bank_addr_city, bank_addr_province, bank_addr_post_code]
    mandatory: [bene_name, account_number]   # TODO: mirror all NOT NULL cols of t_geo_beneficiary
```

## 5. Utility (Spring Boot CLI)

Runnable jar (spring-boot-maven-plugin), web-application-type=none, no datasource:
```
java -jar pushpay-gen.jar <input.xlsx> --template-client <id> [--out batch.sql]
```
(Only `--template-client` — benes have no template.)

Suggested structure (keep it small — this is a CLI, not a service):
- `PushpayGenApplication` — `@SpringBootApplication` + `CommandLineRunner`, arg parsing,
  exit codes (0 ok, 1 usage, 2 validation failed)
- `ColumnsConfig` — `@ConfigurationProperties(prefix="pushpay")` record (§4)
- `ExcelReader` — POI reading (both sheets → row maps + Excel row numbers)
- `BatchValidator` — all rules below, returns full error list
- `SqlGenerator` — batch.sql + summary.txt content (text blocks for templates)

Behaviour:
1. Config binds from application.yml (+ optional external override file).
2. Read both sheets with Apache POI (`DataFormatter` for cell text). Header matching is
   by name (order-insensitive). Skip fully blank rows. Keep Excel row numbers for errors.
3. Validate — if ANY error, print all errors with row numbers and generate NOTHING
   (exit code 2):
   - unknown header (not in config) / missing `client_ref` column
   - duplicate `client_ref` in Clients; duplicate client `name` within file
   - bene `client_ref` with no matching Clients row (orphan)
   - mandatory fields blank; basic email format for `contact_email`, `primary_email`
   - `template_client_id` blank with no `--template-client` CLI default given, or present
     but non-numeric (fast-fail: this alone is enough to abort generation for the whole
     batch, even if only one row is affected)
4. Generate `batch.sql`: one flat `SELECT onboard_pushpay_client_with_benes(...)` per
   client (see §7 for exact shape). Escaping rules:
   - SQL literals: double single-quotes
   - JSON strings: escape backslash, double-quote, newlines/tabs
   - Blank cells: OMIT the key from JSON entirely. Semantics differ by sheet:
     Clients ⇒ "use template client's value"; Beneficiaries ⇒ "NULL" (no bene template).
5. Also generate `batch_summary.txt` (batch tag, per-client template client id, client
   list with bene counts) for the change ticket.

## 6. DB Function (one-time DDL, deployed via App Support)

```sql
CREATE OR REPLACE FUNCTION onboard_pushpay_client_with_benes(
    p_template_client_id BIGINT,
    p_client JSONB,
    p_benes  JSONB DEFAULT '[]'::jsonb
) RETURNS BIGINT
```
(No bene-template parameter — beneficiaries are built entirely from Excel data.)

**Signature is frozen** — future column changes touch only the body (CREATE OR REPLACE,
no DROP). Internal steps:

1. **Validate (fail fast):**
   - template client exists (and is PushPay-enabled — TODO: confirm flag column)
   - `p_client->>'name'` present
   - every key of `p_client` is in an allowed-keys array; every key of each bene object
     likewise. Unknown key ⇒ `RAISE EXCEPTION 'Unknown client field "%" — update the
     function before onboarding with this column'`. (Prevents silent data loss when Excel
     gains a column before the function is updated.)
   - duplicate guard: <!-- TODO: business rule pending — reject if a PushPay client with
     same name (or name+country?) already exists -->

2. **Main client insert — ROWTYPE + jsonb_populate_record pattern** (t_geo_client has
   ~200 columns; only ~25 are Excel-driven, so no column list):
   ```plpgsql
   SELECT * INTO STRICT v_client FROM public.t_geo_client WHERE id = p_template_client_id;
   v_client := jsonb_populate_record(v_client, p_client);  -- overlay Excel overrides
   v_client.id := nextval('cma_seq');
   v_client.created_at := now();          -- TODO: list all fresh-value columns
   -- TODO: reset any unique/per-client columns that must NOT be copied from template
   INSERT INTO public.t_geo_client VALUES (v_client.*) RETURNING id INTO v_client_id;
   ```
   Note: relies on utility omitting blank keys (populate_record has no NULLIF semantics).

3. **~34 child config tables — plain INSERT..SELECT per table** (small tables, only
   client_id changes):
   ```sql
   INSERT INTO public.t_client_currency (client_id, currency_code, is_default)
   SELECT v_client_id, currency_code, is_default
   FROM public.t_client_currency WHERE client_id = p_template_client_id;
   ```
   <!-- TODO: final list of the 34 tables — confirm with product which are config
        (copy) vs history/transactional (never copy). For each: how its PK is generated
        (omit if DEFAULT, nextval(...) if manual) and any unique columns that must be
        regenerated instead of copied. -->

4. **Beneficiaries loop — pure Excel data, no template:**
   ```plpgsql
   FOR v_bene_json IN SELECT * FROM jsonb_array_elements(p_benes) LOOP
       -- start from an all-NULL row of the right shape, overlay Excel data
       v_bene := NULL::public.t_geo_beneficiary;
       v_bene := jsonb_populate_record(v_bene, v_bene_json);
       v_bene.id               := nextval('bene_seq');  -- TODO: real sequence name
       v_bene.owning_client_id := v_client_id;          -- runtime linking
       v_bene.profile_id       := v_client_id;          -- SAME new client id (schema quirk)
       -- TODO: created_at/updated_at or other system columns := now() if present
       INSERT INTO public.t_geo_beneficiary VALUES (v_bene.*)
       RETURNING id INTO v_bene_id;

       -- bene reference/config tables: direct inserts using v_bene_id / v_client_id
       -- (no template copy — values are fixed defaults or come from the bene JSON)
       -- TODO: list bene child tables + what each needs
       RAISE NOTICE '  bene % created with id %', v_bene_json->>'bene_name', v_bene_id;
   END LOOP;
   ```
   Notes: `NULL::public.t_geo_beneficiary` gives an empty row of the correct type, so
   jsonb_populate_record fills only the Excel-provided fields and everything else stays
   NULL (columns with DB DEFAULTs that must apply should be set explicitly, since
   VALUES(v_bene.*) bypasses defaults — capture those in the TODO above). NOT NULL
   columns without Excel data will correctly fail the insert ⇒ whole client rolls back.

5. `RAISE NOTICE 'Client % created with id %', p_client->>'name', v_client_id;` then
   `RETURN v_client_id;`

**Atomicity (decided):** all-or-nothing per client. No internal exception handling that
would swallow errors — any failure in any table/bene aborts the whole function call, so
the client and everything under it rolls back. The failed SELECT statement reports the
error; other clients' statements are unaffected (each top-level statement is its own
transaction under auto-commit).

**Conventions:** schema-qualify all tables (`public.` — search_path differs for App
Support sessions). Params prefixed `p_`, variables `v_` (avoids column-name ambiguity).
IDs come from the same sequences Hibernate uses (`cma_seq`) — safe alongside the app.

## 7. Generated batch.sql shape (example)

```sql
-- Client C1 : Acme Trading LLC (2 beneficiaries)
SELECT onboard_pushpay_client_with_benes(
    p_template_client_id => 1001,
    p_client => '{"name":"Acme Trading LLC","country":"India","transaction_manager":"TM-NORTH"}'::jsonb,
    p_benes  => '[
      {"bene_name":"Supplier One","account_number":"004501234567","swift_code":"HDFCINBB",
       "bene_bank_name":"HDFC Bank","bene_addr_country":"India","bene_addr_city":"Pune"},
      {"bene_name":"Supplier Two","account_number":"009988776655","bene_bank_name":"ICICI Bank"}
    ]'::jsonb);
-- bene JSON carries FULL data (no template); owning_client_id/profile_id are set by the
-- function to the newly created client id.
```

DBA instructions (include as header comment in generated file): run top-to-bottom in
auto-commit; a failed statement affects only that client; capture full console output
(NOTICEs + errors) and paste on the ticket.

## 8. Column add/remove procedure (runbook)

**Add** (order matters — consumer before producer):
1. Function body: add field to allowed-keys array (+ nothing else needed for the main
   table thanks to populate_record; child-table-driven fields would need their line).
   Deploy via CREATE OR REPLACE (light ticket, no DROP).
2. Column config (application.yml / external override yml): add the name.
3. Excel template: add header; distribute new template to product team.

**Remove:** delete from properties + template. Function body untouched. Absent key ⇒
client: template value; bene: NULL (verify the column is nullable before removing it
from the bene sheet).

## 9. Phase 2 (designed, deliberately deferred — do not implement yet)

Audit/staging layer for tracking & retry, agreed design if/when needed:
- Two tables: `pushpay_onboarding_log` (clients) + `pushpay_bene_log` (benes); columns:
  batch_id, client_ref, payload/fields, status PENDING/CREATED/FAILED, new_client_id /
  new_bene_id, error_msg, created_at. Never truncate; batch_id keeps history. A VIEW can
  join both for single-pane reporting.
- Generated script becomes 3 sections: (1) INSERT rows status=PENDING, (2) DO $$ driver
  loop processing PENDING rows via the same functions, with per-client exception capture
  writing FAILED + error_msg (including which bene failed via a tracker variable, and
  marking sibling benes 'rolled back'), (3) verification SELECTs as the report.
- Retry = fix data, flip rows back to PENDING, re-run section 2 (CREATED rows are skipped).
- Also enables "add bene to existing client" (bene_log row carrying an explicit
  existing client id) — out of scope for v1.

## 10. Open TODOs before coding

1. Real column list check: `cotact_email` vs `contact_email`; `provience` vs `province`;
   bene column names against actual `t_geo_beneficiary` schema.
2. Final list of client child tables to clone (of the ~34) + per-table PK strategy.
3. Bene: NOT NULL columns of t_geo_beneficiary (⇒ pushpay.bene.mandatory in application.yml),
   bene sequence name, bene child/reference tables and what each insert needs,
   system columns (created_at etc.) to set explicitly.
4. Duplicate-client guard rule from product (name? name+country?).
5. Confirm PushPay-enabled flag column on t_geo_client for template validation.
