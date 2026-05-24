-- Local deterministic demo seed (Liquibase context=local only).
INSERT INTO public.holidays (country_code, holiday_date, holiday_year, is_bank_holiday, is_observed_holiday, created_at, updated_at, version, region_code, id, holiday_type, created_by, name, updated_by, description)
VALUES
('NL', DATE '2026-05-08', 2026, TRUE, FALSE, TIMESTAMP '2026-05-01 10:00:00', NULL, 0, NULL, 'a1000001-1111-4111-a111-111111111111', 'PUBLIC', 'seed', 'Liberation Day NL', NULL, NULL),
('BE', DATE '2025-07-21', 2025, TRUE, FALSE, TIMESTAMP '2026-05-01 10:00:00', NULL, 0, NULL, 'a1000001-1111-4111-a111-111111111112', 'PUBLIC', 'seed', 'National Day Belgium', NULL, NULL);

INSERT INTO public.customers (
    annual_income, date_of_birth, incorporation_country, incorporation_date, nationality, pep_flag, residence_country,
    sanction_flag, created_at, updated_at, version, id, linked_identity_user_id, customer_number, gender,
    kyc_status, status, type, legal_entity_type, marital_status, segment,
    business_registration_number, tax_id, linked_identity_username, first_name, last_name,
    mother_maiden_name, occupation, place_of_birth, business_name, blocked_reason)
VALUES
(65000.0000, DATE '1985-03-12', NULL, NULL, 'FI', FALSE, 'FI', FALSE, TIMESTAMP '2026-04-01 10:00:00', TIMESTAMP '2026-05-01 10:00:00', 0,
 '10000000-0000-4000-8000-000000000001', NULL, 'CN00010001', 'MALE', 'VERIFIED', 'ACTIVE', 'INDIVIDUAL', NULL, 'MARRIED', 'PREMIUM',
 NULL, 'FI19850312AAA', NULL, 'Matti', 'Virtanen', NULL, 'Engineer', 'Helsinki', NULL, NULL),
(92000.0000, DATE '1978-11-03', NULL, NULL, 'SE', FALSE, 'SE', FALSE, TIMESTAMP '2026-03-18 09:30:00', TIMESTAMP '2026-05-01 11:00:00', 0,
 '10000000-0000-4000-8000-000000000002', NULL, 'CN00010002', 'FEMALE', 'PENDING', 'ACTIVE', 'INDIVIDUAL', NULL, 'DIVORCED', 'RETAIL',
 NULL, 'SE19781103999', NULL, 'Astrid', 'Lindqvist', NULL, 'Architect', NULL, NULL, NULL),
(48000.0000, DATE '1992-06-01', NULL, NULL, 'EE', FALSE, 'EE', FALSE, TIMESTAMP '2026-04-10 12:15:00', TIMESTAMP '2026-05-02 09:45:00', 0,
 '10000000-0000-4000-8000-000000000003', NULL, 'CN00010003', NULL, 'IN_REVIEW', 'ACTIVE', 'INDIVIDUAL', NULL, 'SINGLE', 'MASS_MARKET',
 NULL, 'EE19920601999', NULL, 'Kai', 'Tamm', NULL, NULL, NULL, NULL, NULL),
(NULL, DATE '1955-09-09', NULL, NULL, NULL, FALSE, NULL, FALSE, TIMESTAMP '2025-06-01 08:00:00', TIMESTAMP '2026-01-03 09:10:00', 0,
 '10000000-0000-4000-8000-000000000004', NULL, 'CN00010004', NULL, 'VERIFIED', 'BLOCKED', 'INDIVIDUAL', NULL, NULL, 'RETAIL',
 NULL, NULL, NULL, 'Blocked', 'User', NULL, NULL, NULL, NULL, 'PEP review'),
(NULL, DATE '1980-01-01', NULL, NULL, 'DE', FALSE, 'DE', FALSE, TIMESTAMP '2019-03-03 07:07:07', TIMESTAMP '2025-06-06 06:06:06', 0,
 '10000000-0000-4000-8000-000000000005', NULL, 'CN00010005', NULL, 'VERIFIED', 'CLOSED', 'INDIVIDUAL', NULL, NULL, 'RETAIL',
 NULL, 'DE0475100005', NULL, 'Closed', 'Customer', NULL, NULL, NULL, NULL, NULL),
(31000.0000, DATE '1999-07-07', NULL, NULL, 'PL', FALSE, 'PL', FALSE, TIMESTAMP '2024-07-07 07:07:07', TIMESTAMP '2026-04-07 07:07:07', 0,
 '10000000-0000-4000-8000-000000000006', NULL, 'CN00010006', NULL, 'VERIFIED', 'INACTIVE', 'INDIVIDUAL', NULL, NULL, 'MASS_MARKET',
 NULL, 'PL99100999906', NULL, 'Quiet', 'Dormant', NULL, NULL, NULL, NULL, NULL),
(NULL, NULL, 'BE', DATE '2002-07-07', NULL, FALSE, NULL, FALSE, TIMESTAMP '2022-07-07 07:07:07', TIMESTAMP '2026-05-01 07:07:07', 0,
 '10000000-0000-4000-8000-000000000007', NULL, 'CNBUS7700707', NULL, 'VERIFIED', 'ACTIVE', 'BUSINESS', 'LLC', NULL, NULL,
 NULL, 'BE0475100707', NULL, NULL, NULL, NULL, NULL, NULL, 'Demo Holdings NV', NULL),
(120000.0000, DATE '1996-07-07', NULL, NULL, NULL, FALSE, NULL, FALSE, TIMESTAMP '2026-05-01 08:09:09', TIMESTAMP '2026-05-03 07:07:07', 0,
 '10000000-0000-4000-8000-000000000008', NULL, 'CN00010008', NULL, 'PENDING', 'PROSPECT', 'INDIVIDUAL', NULL, NULL, 'PRIVATE_BANKING',
 NULL, 'LU99100999108', NULL, 'Sophie', 'Prospect', NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.banking_preferences (
    large_transaction_alert_threshold, low_balance_alert_threshold, notify_email, notify_push, notify_sms,
    preferred_currency, updated_at, version, preferred_language, customer_id, id,
    preferred_branch_code, statement_delivery, statement_frequency, time_zone)
VALUES
(9000.0000, 50.0000, TRUE, FALSE, FALSE, 'EUR', TIMESTAMP '2026-05-01 10:05:00', 0,
 'en', '10000000-0000-4000-8000-000000000001', 'b2000002-2222-4222-a222-222222222201',
 NULL, NULL, NULL, NULL);

INSERT INTO public.customer_addresses (country, is_primary, valid_from, valid_to, deleted_at, updated_at, version, customer_id, id,
    postal_code, type, city, line1, line2, state)
VALUES
('FI', TRUE, DATE '2020-01-01', NULL, NULL, TIMESTAMP '2026-05-01 10:06:00', 0, '10000000-0000-4000-8000-000000000001',
 'b2111112-2222-4222-a222-211111221101', '00100', 'PHYSICAL', 'Helsinki', 'Aleksanterinkatu 1', NULL, NULL),
('SE', TRUE, DATE '2021-06-06', NULL, NULL, TIMESTAMP '2026-05-01 10:06:00', 0, '10000000-0000-4000-8000-000000000002',
 'b2111112-2222-4222-a222-211111221102', '11122', 'PHYSICAL', 'Stockholm', 'Drottninggatan 9', NULL, NULL),
('NL', FALSE, DATE '2023-01-09', NULL, NULL, TIMESTAMP '2026-05-01 10:06:00', 0, '10000000-0000-4000-8000-000000000003',
 'b2111112-2222-4222-a222-211111221103', '1012LG', 'MAILING', 'Amsterdam', 'Damrak 70', NULL, NULL),
('BE', TRUE, DATE '2022-07-07', NULL, NULL, TIMESTAMP '2026-05-01 10:06:00', 0, '10000000-0000-4000-8000-000000000007',
 'b2111112-2222-4222-a222-211111221104', '1000', 'REGISTERED_OFFICE', 'Brussels', 'Rue de la Loi 42', NULL, NULL);

INSERT INTO public.contact_details (is_primary, is_verified, deleted_at, updated_at, verified_at, version,
    customer_id, id, type, verified_by, value)
VALUES
(TRUE, TRUE, NULL, TIMESTAMP '2026-05-01 10:06:00', TIMESTAMP '2026-05-01 10:06:01', 0,
 '10000000-0000-4000-8000-000000000001', 'c3111114-3333-4333-a333-411111431101', 'EMAIL', 'seed', 'matti.virtanen@demo.local'),
(TRUE, FALSE, NULL, TIMESTAMP '2026-05-01 10:06:00', NULL, 0,
 '10000000-0000-4000-8000-000000000002', 'c3111114-3333-4333-a333-411111431102', 'EMAIL', NULL, 'astrid.demo@demo.local'),
(TRUE, TRUE, NULL, TIMESTAMP '2026-05-01 10:06:00', TIMESTAMP '2026-05-01 10:06:05', 0,
 '10000000-0000-4000-8000-000000000007', 'c3111114-3333-4333-a333-411111431103', 'EMAIL', 'seed', 'holdings-frontdesk@demo.local');

INSERT INTO public.customer_consents (granted, expires_at, recorded_at, customer_id, id, capture_channel,
    consent_type, policy_version, recorded_by)
VALUES
(TRUE, NULL, TIMESTAMP '2026-04-01 10:10:00', '10000000-0000-4000-8000-000000000001',
 'd4111115-4444-4444-a444-411511541401', NULL, 'PRIVACY_POLICY', '2026-03', NULL),
(TRUE, TIMESTAMP '2027-03-03 03:03:03', TIMESTAMP '2026-04-07 07:07:07', '10000000-0000-4000-8000-000000000001',
 'd4111115-4444-4444-a444-411511541402', NULL, 'MARKETING_EMAIL', NULL, NULL);

INSERT INTO public.customer_audit_logs (
    changed_at, customer_id, id, ip_address, action, changed_by)
VALUES
(TIMESTAMP '2026-05-03 06:06:06', '10000000-0000-4000-8000-000000000001',
 'e5111116-5555-4555-a555-511611651601', NULL, 'KYC_UPDATE', 'admin'),
(TIMESTAMP '2026-05-03 07:07:07', '10000000-0000-4000-8000-000000000002',
 'e5111116-5555-4555-a555-511611651602', NULL, 'ADDRESS_UPDATED', 'staff-seed');

INSERT INTO public.data_subject_requests (
    deferred_until, due_by, extended, fulfilled_at, received_at, received_at_ts, updated_at, version,
    customer_id, id, request_type, status)
VALUES
(NULL, DATE '2026-05-31', FALSE, NULL, DATE '2026-05-02', TIMESTAMP '2026-05-02 08:09:09', TIMESTAMP '2026-05-02 08:09:09', 0,
 '10000000-0000-4000-8000-000000000003', 'f6111117-6666-4666-a666-611711761701', 'ACCESS', 'IN_REVIEW');

INSERT INTO public.kyc_workflows (
    completed_at, initiated_at, reviewed_at, updated_at, version, customer_id, id,
    status, initiated_by, reviewed_by, comments)
VALUES
(NULL, TIMESTAMP '2026-04-12 07:07:07', NULL, TIMESTAMP '2026-05-03 06:06:06', 0,
 '10000000-0000-4000-8000-000000000003', 'f7111118-7777-4777-a777-711817871801',
 'IN_REVIEW', 'staff-seed', NULL, NULL),
(TIMESTAMP '2026-03-07 06:06:06', TIMESTAMP '2026-03-05 06:06:06', TIMESTAMP '2026-03-06 06:06:06',
 TIMESTAMP '2026-03-07 06:06:06', 0,
 '10000000-0000-4000-8000-000000000001', 'f7111118-7777-4777-a777-711817871802',
 'VERIFIED', 'staff-seed', 'staff-seed', 'Auto-verified demo');

INSERT INTO public.kyc_review_steps (reviewed_at, id, kyc_workflow_id, decision, reviewed_by, step_name, comments)
VALUES
(TIMESTAMP '2026-05-03 06:06:06', 'f8111119-8888-4888-a888-811918981901',
 'f7111118-7777-4777-a777-711817871801', 'REQUIRES_ADDITIONAL_INFO', 'reviewer-demo', 'ID_VERIFICATION', NULL),
(TIMESTAMP '2026-03-06 06:06:06', 'f8111119-8888-4888-a888-811918981902',
 'f7111118-7777-4777-a777-711817871802', 'APPROVED', 'reviewer-demo', 'AML_SCREENING', NULL);

INSERT INTO public.identification_documents (
    expiry_date, is_verified, issue_date, issuing_country, deleted_at, updated_at, verified_at,
    version, customer_id, id, document_status, type, document_number, issuing_authority, verified_by)
VALUES (
 DATE '2033-06-06', TRUE, DATE '2020-02-03', 'FI', NULL,
 TIMESTAMP '2026-05-02 06:06:06', TIMESTAMP '2026-05-02 06:06:07', 0,
 '10000000-0000-4000-8000-000000000001',
 'f9111120-9999-4999-a999-911929919201',
 'VERIFIED', 'PASSPORT', 'FI-P-1239988', NULL, NULL);

INSERT INTO public.customer_risk_profiles (
    pep_match, sanction_match, last_evaluated_at, customer_id, risk_rating)
VALUES
(FALSE, FALSE, TIMESTAMP '2026-05-01 12:01:03', '10000000-0000-4000-8000-000000000001', 'LOW'),
(FALSE, TRUE, TIMESTAMP '2026-04-15 11:05:06', '10000000-0000-4000-8000-000000000002', 'MEDIUM'),
(FALSE, FALSE, TIMESTAMP '2026-05-02 06:06:06', '10000000-0000-4000-8000-000000000007', 'HIGH');

INSERT INTO public.exchange_rates (ask_rate, bid_rate, rate, rate_date, source_currency, target_currency,
 created_at, updated_at, version, id, rate_type, created_by)
VALUES
(NULL, NULL, 1.0950000000000000, DATE '2026-05-01', 'EUR', 'USD',
 TIMESTAMP '2026-05-01 06:06:06', TIMESTAMP '2026-05-01 06:06:06', 0,
 '091000aa-aaaa-4aaa-a000-aaaaaaaa9201', 'EOD', 'seed'),
(NULL, NULL, 11.4520000000000000, DATE '2026-05-01', 'EUR', 'SEK',
 TIMESTAMP '2026-05-01 06:06:06', TIMESTAMP '2026-05-01 06:06:06', 0,
 '091000aa-aaaa-4aaa-a000-aaaaaaaa9202', 'EOD', 'seed');

INSERT INTO public.fx_spreads (
    source_currency, spread_rate, target_currency, created_at, updated_at, version, id, created_by)
VALUES
('EUR', 0.001500000000, 'USD', TIMESTAMP '2026-05-01 07:07:07', TIMESTAMP '2026-05-01 07:07:07', NULL,
 '092000bb-bbbb-4bbb-b000-bbbbbbb99301', 'seed');

INSERT INTO public.fiscal_periods (
 end_date, fiscal_year, period_number, start_date, closed_at, created_at, reopened_at,
 updated_at, id, status, name, reopened_by, closed_by)
VALUES (
 DATE '2026-05-31', 2026, 5, DATE '2026-05-01', NULL,
 TIMESTAMP '2026-05-02 06:06:06', NULL, TIMESTAMP '2026-05-02 06:06:06',
 '093000cc-cccc-4ccc-c000-ccccccccc301',
 'OPEN', '2026-05 opening', NULL, NULL);

INSERT INTO public.gl_accounts (contra, currency, created_at, updated_at, version,
 cash_flow_category, normal_balance, id, status, type, code, created_by,
 name)
VALUES
(FALSE, 'EUR', TIMESTAMP '2026-04-05 06:06:06', TIMESTAMP '2026-04-05 06:06:06', 0,
 'OPERATING', 'DEBIT', '094100dd-dddd-41dd-a000-dddddddd9311', 'ACTIVE', 'ASSET',
 'GL100DEMOCASH', 'seed',
 'Liquidity suspense — demo'),
(FALSE, 'EUR', TIMESTAMP '2026-04-05 06:06:06', TIMESTAMP '2026-04-05 06:06:06', 0,
 'OPERATING', 'CREDIT', '094200dd-dddd-41dd-a000-dddddddd9322', 'ACTIVE', 'LIABILITY',
 'GL210DEPOSITS', 'seed',
 'Customer deposits liability — synthetic');

INSERT INTO public.gl_transactions (currency, transaction_date, created_at, posting_date, submitted_at,
 transaction_number, updated_at, version, id, status, source, created_by, posted_by,
 reference_id, description)
VALUES
('EUR', DATE '2026-05-02', TIMESTAMP '2026-05-02 11:06:06', TIMESTAMP '2026-05-02 11:06:06', NULL,
 9100420001, TIMESTAMP '2026-05-02 11:06:06', 0,
 '095000ee-eeee-41ee-a000-eeeeeeee9411',
 'POSTED', 'MANUAL_ENTRY', 'seed', 'seed', 'JRNL-9100420001', 'Synthetic postings for demo GL');

INSERT INTO public.gl_journal_entries (
 base_credit_amount, base_debit_amount, credit_amount, currency,
 debit_amount, exchange_rate, line_number, value_date, account_id, id, transaction_id, description)
VALUES
(0.0000, 10000.0000, 0.0000, 'EUR',
 10000.0000, 1.000000, 1, DATE '2026-05-02',
 '094100dd-dddd-41dd-a000-dddddddd9311',
 '096100ff-ffff-41ff-b000-ffffffffff91',
 '095000ee-eeee-41ee-a000-eeeeeeee9411', 'Debit cash / liquidity'),
(10000.0000, 0.0000, 10000.0000, 'EUR',
 0.0000, 1.000000, 2, DATE '2026-05-02',
 '094200dd-dddd-41dd-a000-dddddddd9322',
 '096100ff-ffff-41ff-b000-ffffffffff92',
 '095000ee-eeee-41ee-a000-eeeeeeee9411', 'Credit customer deposits control');

INSERT INTO public.gl_reconciliations (
    reconciliation_date, id, transaction_id, status, reconciled_by)
VALUES (
 DATE '2026-05-03', '09711011-1111-4111-a911-aaaaaaaa9511',
 '095000ee-eeee-41ee-a000-eeeeeeee9411', 'RECONCILED', 'staff-seed');

INSERT INTO public.accounts (
 available_balance, currency, ledger_balance, closed_at, created_at, updated_at,
 version, id, primary_user_profile_id, account_number, status, product_type,
 created_by, display_name)
VALUES
(15420.5700, 'EUR', 15420.5700, NULL, TIMESTAMP '2026-04-18 06:06:06', TIMESTAMP '2026-05-02 06:06:06', 0,
 '20000000-0000-4000-8000-000000000001',
 '10000000-0000-4000-8000-000000000001',
 'CHKDEMOEUR00001', 'ACTIVE', 'CHECKING',
 'seed', 'Primary EUR checking'),
(40210.9100, 'EUR', 40210.9100, NULL, TIMESTAMP '2026-03-07 06:06:06', TIMESTAMP '2026-05-01 06:06:06', 0,
 '20000000-0000-4000-8000-000000000002',
 '10000000-0000-4000-8000-000000000002',
 'SAVESTKDEMO0922', 'ACTIVE', 'SAVINGS',
 'seed', 'Savings buffer'),
(1250.0000, 'USD', 1250.0000, NULL, TIMESTAMP '2026-03-17 06:06:06', TIMESTAMP '2026-05-01 06:06:06', 0,
 '20000000-0000-4000-8000-000000000003',
 '10000000-0000-4000-8000-000000000001',
 'MMKTUSDEMU01', 'ACTIVE', 'MONEY_MARKET',
 'seed', 'USD petty cash'),
(500.5000, 'EUR', 500.5000, TIMESTAMP '2026-03-06 06:06:06', TIMESTAMP '2025-06-06 06:06:06', TIMESTAMP '2025-06-07 06:06:06', 0,
 '20000000-0000-4000-8000-000000000004',
 '10000000-0000-4000-8000-000000000005',
 'CLOSDEMOCUST05', 'CLOSED', 'CHECKING',
 'seed', 'Closed retail'),
(8840.0400, 'EUR', 8840.0400, NULL, TIMESTAMP '2026-03-07 06:06:06', TIMESTAMP '2026-05-02 06:06:06', 0,
 '20000000-0000-4000-8000-000000000005',
 '10000000-0000-4000-8000-000000000007',
 'CORPEURCHK0707', 'ACTIVE', 'CHECKING',
 'seed', 'Operating account'),
(21025.7700, 'EUR', 21025.7700, NULL, TIMESTAMP '2026-03-01 06:06:06', TIMESTAMP '2026-05-02 06:06:06', 0,
 '20000000-0000-4000-8000-000000000006',
 '10000000-0000-4000-8000-000000000002',
 'SAVEHIGHINT99', 'ACTIVE', 'SAVINGS',
 'seed', 'High-yield saver');

INSERT INTO public.account_limits (is_regulatory, max_amount, override_allowed,
 created_at, effective_from, updated_at,
 customer_account_id, id,
 limit_period, limit_type,
 created_by)
VALUES
(FALSE, 500000.0000, TRUE,
 TIMESTAMP '2026-04-25 06:06:06', TIMESTAMP '2026-05-02 06:06:06', TIMESTAMP '2026-05-02 06:06:06',
 '20000000-0000-4000-8000-000000000001',
 '09812012-2121-4212-a921-aaaaaaaa9612',
 'DAILY',
 'TRANSFER_LIMIT',
 'seed');

INSERT INTO public.account_holds (amount, currency, created_at, expires_at, customer_account_id, id,
    status, reference_id)
VALUES (
 300.2500, 'EUR', TIMESTAMPTZ '2026-05-02 12:06:06+00',
 TIMESTAMP '2026-05-06 06:06:06',
 '20000000-0000-4000-8000-000000000001',
 '09813013-3131-4313-a931-aaaaaaaa9713',
 'ACTIVE', 'HOLD-DEMO-001');

INSERT INTO public.account_relationships (
 beneficiary_percentage, is_beneficiary, percentage_ownership, created_at, effective_from, customer_account_id,
 id, user_profile_id,
 relationship_type, status, created_by)
VALUES (
 NULL, TRUE, NULL,
 TIMESTAMP '2026-04-06 06:06:06', TIMESTAMP '2023-06-06 06:06:06',
 '20000000-0000-4000-8000-000000000005',
 '09814014-4141-4414-a941-aaaaaaaa9814',
 '10000000-0000-4000-8000-000000000007',
 'PRIMARY_HOLDER', 'ACTIVE', 'seed');

INSERT INTO public.loan_products (
 active, collateral_required, currency, grace_period_days, guarantor_required,
 interest_rate, late_fee_fixed, late_fee_percentage, max_amount, max_tenor_months,
 min_amount, min_tenor_months, processing_fee_percentage, created_at, updated_at, id,
 amortization_type, interest_calculation_method, product_type, repayment_frequency,
 product_code, product_name)
VALUES (
 TRUE, TRUE, 'EUR', 10, TRUE,
 4.9500, NULL, 1.0000, 550000.0000, 84,
 5500.0000, 6, NULL,
 TIMESTAMPTZ '2026-03-06 06:06:06+00', TIMESTAMPTZ '2026-03-06 06:06:06+00',
 '40000000-0000-4000-8000-000000000001',
 'EQUAL_INSTALLMENTS', 'REDUCING_BALANCE', 'PERSONAL_LOAN', 'MONTHLY',
 'PLN-DEMO-PRIME', 'Retail prime personal loans');

INSERT INTO public.loan_applications (
 approval_date, approved_amount, approved_interest_rate, approved_tenor_months, credit_score,
 currency, existing_obligations, guarantors_required, monthly_income, requested_amount,
 requested_tenor_months,
 created_at, underwriter_assigned_at, updated_at,
 customer_id, id, product_id, risk_rating, status,
 application_number, approved_by, purpose,
 underwriter_assigned_by)
VALUES (
 NULL, NULL, NULL, NULL, NULL,
 'EUR', NULL, NULL, NULL, 250000.0000,
 24,
 TIMESTAMPTZ '2026-05-01 06:06:06+00', NULL,
 TIMESTAMPTZ '2026-05-01 06:06:06+00',
 '10000000-0000-4000-8000-000000000001',
 '41000000-0000-4000-8000-000000000001',
 '40000000-0000-4000-8000-000000000001',
 'MEDIUM', 'UNDER_REVIEW',
 'LAAPP2026000401',
 NULL, 'Debt consolidation demo',
 NULL),
(
 DATE '2026-03-06', 120000.0000, 4.7500, 36, NULL,
 'EUR', NULL, 0, 7200.5000, 118000.0000,
 36,
 TIMESTAMPTZ '2026-03-06 06:06:06+00', TIMESTAMPTZ '2026-03-06 07:06:06+00',
 TIMESTAMPTZ '2026-03-06 08:06:06+00',
 '10000000-0000-4000-8000-000000000002',
 '41000000-0000-4000-8000-000000000002',
 '40000000-0000-4000-8000-000000000001',
 'LOW', 'APPROVED',
 'LAAPP2026000302',
 'underwriter-demo', 'Bridging liquidity',
 NULL),
(NULL, NULL, NULL, NULL, NULL,
 'EUR', NULL, NULL, 3800.0000, 18000.0000,
 12,
 TIMESTAMPTZ '2026-05-03 06:06:06+00', TIMESTAMPTZ '2026-05-03 08:06:06+00',
 TIMESTAMPTZ '2026-05-03 08:06:06+00',
 '10000000-0000-4000-8000-000000000006',
 '41000000-0000-4000-8000-000000000003',
 '40000000-0000-4000-8000-000000000001',
 NULL, 'UNDERWRITING',
 'LAAPP2026000903',
 NULL, 'Starter credit demo',
 NULL);

INSERT INTO public.loan_accounts (
 closed_date, currency, days_past_due, disbursement_date, first_payment_date, interest_rate, is_restructured,
 is_top_up, last_payment_date, maturity_date, outstanding_fees, outstanding_interest, outstanding_penalties,
 outstanding_principal, principal_amount, tenor_months, total_paid, created_at, updated_at, version,
 application_id, customer_id, id,
 product_id, amortization_type, delinquency_bucket, interest_calculation_method,
 repayment_frequency, status, loan_account_number)
VALUES (
 NULL, 'EUR', 0, DATE '2026-03-22', DATE '2026-04-22', 4.7500,
 FALSE, FALSE,
 DATE '2026-04-02', DATE '2029-03-22',
 220.5600, 420.8700, 0.0000,
 111800.9100,
 118000.0000, 36,
 6200.0000,
 TIMESTAMPTZ '2026-03-06 06:06:06+00', TIMESTAMPTZ '2026-05-03 06:06:06+00',
 NULL,
 '41000000-0000-4000-8000-000000000002',
 '10000000-0000-4000-8000-000000000002',
 '42000000-0000-4000-8000-000000000001',
 '40000000-0000-4000-8000-000000000001',
 'EQUAL_INSTALLMENTS',
 'CURRENT',
 'REDUCING_BALANCE',
 'MONTHLY',
 'ACTIVE',
 'LOANACTIVE2026000222'),
(
 NULL, 'EUR', 62,
 DATE '2025-08-06', DATE '2025-09-06',
 9.9900,
 FALSE, TRUE,
 DATE '2025-10-06', DATE '2027-02-06',
 330.9100, 910.8700, 55.9100,
 21800.0000,
 25000.0000,
 18,
 2800.0000,
 TIMESTAMPTZ '2025-08-06 06:06:06+00', TIMESTAMPTZ '2026-05-03 06:06:06+00',
 NULL,
 '41000000-0000-4000-8000-000000000003',
 '10000000-0000-4000-8000-000000000006',
 '42000000-0000-4000-8000-000000000002',
 '40000000-0000-4000-8000-000000000001',
 'EQUAL_INSTALLMENTS',
 'DPD_61_90',
 'REDUCING_BALANCE',
 'MONTHLY',
 'ACTIVE',
 'LOANCOLLECT0625');

INSERT INTO public.loan_schedules (
 days_past_due, due_date, fees_paid, installment_number,
 interest_due, interest_paid,
 is_overdue, outstanding_balance, paid_date, penalties_paid,
 principal_due, principal_paid, total_due,
 created_at, updated_at,
 id,
 loan_account_id, status)
VALUES
(0, DATE '2026-05-06', 0.0000, 2,
 190.8700, 190.8700,
 FALSE,
 114100.7400,
 DATE '2026-05-01',
 0.0000,
 920.7400,
 920.7400,
 111211.3500,
 TIMESTAMPTZ '2026-03-22 06:06:06+00', TIMESTAMPTZ '2026-05-03 06:06:06+00',
 '0a101010-1010-4110-ad10-aaaaaaaaa101',
 '42000000-0000-4000-8000-000000000001',
 'PAID'),
(0, DATE '2026-07-06', 0.0000, 4,
 181.7400,
 0.0000,
 TRUE,
 111800.9100,
 NULL,
 0.0000,
 901.7400,
 0.0000,
 108583.9100,
 TIMESTAMPTZ '2026-03-22 06:06:06+00', TIMESTAMPTZ '2026-05-03 06:06:06+00',
 '0a101010-1010-4110-ad10-aaaaaaaaa102',
 '42000000-0000-4000-8000-000000000001',
 'PENDING'),
(30, DATE '2025-12-06', 0.0000, 4,
 240.8700,
 0.0000,
 TRUE,
 23910.8700,
 NULL,
 0.0000,
 900.8700,
 0.0000,
 121151.6110,
 TIMESTAMPTZ '2025-08-06 06:06:06+00', TIMESTAMPTZ '2026-05-03 06:06:06+00',
 '0a101010-1010-4110-ad10-aaaaaaaaa103',
 '42000000-0000-4000-8000-000000000002',
 'OVERDUE');

INSERT INTO public.loan_payments (
 currency, fees_paid, interest_paid, is_reversed, payment_amount, payment_date,
 penalties_paid, principal_paid,
 created_at, updated_at,
 id,
 loan_account_id,
 payment_method, payment_type, payment_reference)
VALUES
('EUR', 120.8700,
 910.8700,
 FALSE,
 3820.7400,
 DATE '2026-02-06',
 0.0000,
 2388.9980,
 TIMESTAMPTZ '2026-02-06 06:06:06+00', TIMESTAMPTZ '2026-02-06 06:06:06+00',
 '0b202020-2020-4220-ae20-bbbbbbbbb201',
 '42000000-0000-4000-8000-000000000001',
 'BANK_TRANSFER', 'REGULAR_PAYMENT',
 'LPAYALDEMO02601'),
('EUR', 0.0000, 910.8700,
 FALSE,
 3920.0000,
 DATE '2025-12-06',
 0.0000,
 3019.9120,
 TIMESTAMPTZ '2025-12-06 06:06:06+00', TIMESTAMPTZ '2025-12-06 06:06:06+00',
 '0b202020-2020-4220-ae20-bbbbbbbbb202',
 '42000000-0000-4000-8000-000000000002',
 'DIRECT_DEBIT', 'REGULAR_PAYMENT',
 'LPAYCOLLECT1215');

INSERT INTO public.collection_activities (
 activity_date, follow_up_date, created_at, updated_at, id, loan_account_id,
 status,
 activity_type, assigned_to,
 notes)
VALUES (
 DATE '2026-03-06', DATE '2026-03-12',
 TIMESTAMPTZ '2026-03-06 07:06:06+00', TIMESTAMPTZ '2026-03-06 07:06:06+00',
 '0c303030-3030-4330-ae30-ccccccccc301',
 '42000000-0000-4000-8000-000000000002',
 'IN_PROGRESS',
 'PHONE_CALL',
 'collections-demo',
 'Reminder call — borrower committed to arrears repayment plan');

INSERT INTO public.early_settlements (
 approved_date, cancelled_date, currency,
 outstanding_fees, outstanding_interest, outstanding_principal, penalty_amount, quote_date, rebate_amount,
 rejected_date,
 settled_date, settlement_amount,
 valid_until, created_at, updated_at, id,
 loan_account_id,
 status, calculation_method, quote_reference)
VALUES (
 NULL, NULL,
 'EUR',
 110.8700, 410.8700, 108210.7400, 0.0000,
 DATE '2026-04-06',
 -220.7400,
 NULL,
 NULL,
 108611.9700,
 DATE '2026-06-06',
 TIMESTAMPTZ '2026-04-06 06:06:06+00', TIMESTAMPTZ '2026-04-06 06:06:06+00',
 '0d404040-4040-4440-ae40-ddddddddd401',
 '42000000-0000-4000-8000-000000000001',
 'QUOTE',
 'FULL_OUTSTANDING',
 'QUOTE-ALDEMO-2026901');

INSERT INTO public.transaction_requests (
 amount, currency, created_at, version,
 destination_account_id,
 id,
 source_account_id, ip_address, transaction_type, created_by,
 idempotency_key)
VALUES
(140.7500, 'EUR', TIMESTAMP '2026-05-03 07:01:06', NULL,
 '20000000-0000-4000-8000-000000000002',
 '30000003-0303-4003-8003-031300031301',
 '20000000-0000-4000-8000-000000000001', '192.0.2.71', 'TRANSFER', 'seed',
 'idem-tx-demo-03301');

INSERT INTO public.tp_transactions (
 currency, principal_amount,
 transaction_date, value_date,
 completed_at, created_at, updated_at,
 version,
 destination_account_id, id,
 request_id,
 source_account_id,
 status)
VALUES (
 'EUR', 140.7500,
 DATE '2026-05-03',
 DATE '2026-05-03',
 TIMESTAMP '2026-05-03 07:11:06',
 TIMESTAMPTZ '2026-05-03 07:01:06+00',
 TIMESTAMPTZ '2026-05-03 07:11:06+00',
 0,
 '20000000-0000-4000-8000-000000000002',
 '31000004-0404-4004-8044-042400042401',
 '30000003-0303-4003-8003-031300031301',
 '20000000-0000-4000-8000-000000000001',
 'POSTED');

INSERT INTO public.transaction_requests (amount, currency, created_at, version, destination_account_id,
 id, source_account_id, ip_address, transaction_type, created_by, idempotency_key)
VALUES
(88.2000, 'EUR', TIMESTAMP '2026-05-03 07:03:06', NULL, '20000000-0000-4000-8000-000000000002',
 '30000003-0303-4003-8003-031300031302', '20000000-0000-4000-8000-000000000003', '192.0.2.73', 'TRANSFER',
 'seed', 'idem-tx-demo-03302'),
(910.9100, 'EUR', TIMESTAMP '2026-05-03 07:04:06', NULL, NULL,
 '30000003-0303-4003-8003-031300031303', '20000000-0000-4000-8000-000000000001', '192.0.2.74', 'DEPOSIT', 'seed',
 'idem-tx-demo-03303'),
(412.0900, 'EUR', TIMESTAMP '2026-05-03 07:06:06', NULL, NULL,
 '30000003-0303-4003-8003-031300031304', '20000000-0000-4000-8000-000000000006', '192.0.2.76', 'CASH_OUT', 'seed',
 'idem-tx-demo-03304'),
(77.7700, 'EUR', TIMESTAMP '2026-05-03 07:06:22', NULL, '20000000-0000-4000-8000-000000000001',
 '30000003-0303-4003-8003-031300031305', '20000000-0000-4000-8000-000000000005', '192.0.2.91', 'P2P', 'seed',
 'idem-tx-demo-03305'),
(2550.9100, 'EUR', TIMESTAMP '2026-05-03 07:06:52', NULL, '20000000-0000-4000-8000-000000000002',
 '30000003-0303-4003-8003-031300031324', '20000000-0000-4000-8000-000000000001', '192.0.2.99', 'TRANSFER', 'seed',
 'idem-fail-comp'),
(55.0000, 'EUR', TIMESTAMP '2026-05-03 07:07:52', NULL, '20000000-0000-4000-8000-000000000006',
 '30000003-0303-4003-8003-031300031325', '20000000-0000-4000-8000-000000000005', '192.0.2.121', 'P2P', 'seed',
 'idem-auth-hold');

INSERT INTO public.tp_transactions (currency, principal_amount,
 transaction_date, value_date, completed_at, created_at, updated_at,
 version,
 destination_account_id, id,
 request_id, source_account_id,
 status, failure_reason)
VALUES
('EUR', 88.2000,
 DATE '2026-05-03', DATE '2026-05-03', TIMESTAMP '2026-05-03 07:13:06',
 TIMESTAMPTZ '2026-05-03 07:03:06+00', TIMESTAMPTZ '2026-05-03 07:13:06+00', 0,
 '20000000-0000-4000-8000-000000000002',
 '31000004-0404-4004-8044-042400042402',
 '30000003-0303-4003-8003-031300031302',
 '20000000-0000-4000-8000-000000000003',
 'AUTHORIZED',
 NULL),
('EUR', 910.9100,
 DATE '2026-05-03', DATE '2026-05-03', TIMESTAMP '2026-05-03 07:13:52',
 TIMESTAMPTZ '2026-05-03 07:04:06+00', TIMESTAMPTZ '2026-05-03 07:13:52+00', 0,
 NULL,
 '31000004-0404-4004-8044-042400042403',
 '30000003-0303-4003-8003-031300031303',
 '20000000-0000-4000-8000-000000000001',
 'POSTED',
 NULL),
('EUR', 412.0900,
 DATE '2026-05-03', DATE '2026-05-03', TIMESTAMP '2026-05-03 07:17:52',
 TIMESTAMPTZ '2026-05-03 07:06:06+00', TIMESTAMPTZ '2026-05-03 07:17:52+00', 0,
 NULL,
 '31000004-0404-4004-8044-042400042404',
 '30000003-0303-4003-8003-031300031304',
 '20000000-0000-4000-8000-000000000006',
 'POSTED',
 NULL),
('EUR', 77.7700,
 DATE '2026-05-03', DATE '2026-05-03', TIMESTAMP '2026-05-03 07:08:52',
 TIMESTAMPTZ '2026-05-03 07:06:22+00', TIMESTAMPTZ '2026-05-03 07:08:52+00', 0,
 '20000000-0000-4000-8000-000000000001',
 '31000004-0404-4004-8044-042400042405',
 '30000003-0303-4003-8003-031300031305',
 '20000000-0000-4000-8000-000000000005',
 'POSTED',
 NULL),
('EUR', 2550.9100,
 DATE '2026-05-03', DATE '2026-05-03', NULL,
 TIMESTAMPTZ '2026-05-03 07:06:52+00', TIMESTAMPTZ '2026-05-03 07:14:52+00', 0,
 '20000000-0000-4000-8000-000000000002',
 '31000004-0404-4004-8044-042400049999',
 '30000003-0303-4003-8003-031300031324',
 '20000000-0000-4000-8000-000000000001',
 'FAILED',
 'Counterparty AML hold — routed to ops queue'),
('EUR', 55.0000,
 DATE '2026-05-03', DATE '2026-05-03', NULL,
 TIMESTAMPTZ '2026-05-03 07:07:52+00', TIMESTAMPTZ '2026-05-03 07:07:53+00', 0,
 '20000000-0000-4000-8000-000000000006',
 '31000004-0404-4004-8044-042401111101',
 '30000003-0303-4003-8003-031300031325',
 '20000000-0000-4000-8000-000000000005',
 'AUTHORIZED',
 NULL);

INSERT INTO public.transaction_events (event_sequence, created_at, id, transaction_id,
 error_code, new_status, previous_status, created_by, event_type, error_message, event_data)
VALUES
(1, TIMESTAMPTZ '2026-05-03 07:01:06+00', '51111151-5151-4151-ae51-dddddddda501',
 '31000004-0404-4004-8044-042400042401', NULL,
 'AUTHORIZED',
 'INITIATED', 'seed', 'STATE_TRANSITION', NULL, NULL),
(2, TIMESTAMPTZ '2026-05-03 07:11:06+00', '51111151-5151-4151-ae51-dddddddda502',
 '31000004-0404-4004-8044-042400042401', NULL,
 'POSTED',
 'AUTHORIZED', 'seed', 'STATE_TRANSITION', NULL, NULL);

INSERT INTO public.compensation_workflows (max_retries, retry_count,
 created_at, updated_at,
 version,
 compensation_transaction_id,
 gl_reversal_transaction_id,
 original_transaction_id, id,
 compensation_type, workflow_status,
 failure_reason)
VALUES (3, 0,
 TIMESTAMPTZ '2026-05-03 07:08:52+00', TIMESTAMPTZ '2026-05-03 07:08:52+00',
 0,
 NULL,
 NULL,
 '31000004-0404-4004-8044-042400049999',
 '52121212-5252-4252-ae52-eeeeeeee8522',
 'PARTIAL',
 'IN_PROGRESS',
 'Automated remediation — retry scheduled');

INSERT INTO public.account_transactions (amount, currency, created_at,
 transaction_date, updated_at,
 customer_account_id, gl_transaction_id, id,
 status, transaction_type, reference_id, description)
VALUES
(140.7500, 'EUR', TIMESTAMP '2026-05-03 07:01:06',
 TIMESTAMP '2026-05-03 06:06:06', TIMESTAMP '2026-05-03 07:01:52',
 '20000000-0000-4000-8000-000000000001',
 '095000ee-eeee-41ee-a000-eeeeeeee9411',
 '53131313-5353-4353-ae53-ffffffff9533',
 'POSTED',
 'DEPOSIT', 'acct-tx-opening-balance-demo', NULL);

INSERT INTO public.aml_alerts (currency, investigation_hold_placed, monitored_amount, created_at,
 customer_party_id, id, source_account_id,
 transaction_id, severity, status, rule_code, transaction_type_name, detail_summary)
VALUES
('EUR', TRUE, 2550.910000, TIMESTAMPTZ '2026-05-03 07:09:52+00',
 '10000000-0000-4000-8000-000000000001',
 '54141414-5454-4454-ae54-aaaaaaaaa544',
 '20000000-0000-4000-8000-000000000002',
 '31000004-0404-4004-8044-042400049999',
 'HIGH', 'OPEN', 'LOC_VELOCITY_BREACH', 'TRANSFER',
 'Local demo: flagged large transfer spike'),
('EUR', FALSE, 140.750000, TIMESTAMPTZ '2026-05-03 07:12:52+00',
 NULL,
 '54141414-5454-4454-ae54-aaaaaaaaa545',
 '20000000-0000-4000-8000-000000000001',
 '31000004-0404-4004-8044-042400042401',
 'WARNING', 'ACKNOWLEDGED',
 'RULE_HIGH_RISK_COUNTRY_COUNTERPARTY',
 'TRANSFER',
 'Counterparty routed via high-risk corridor (demo marker)'),
('EUR', TRUE, 412.090000, TIMESTAMPTZ '2026-05-03 07:17:53+00',
 '10000000-0000-4000-8000-000000000006',
 '54141414-5454-4454-ae54-aaaaaaaaa546',
 '20000000-0000-4000-8000-000000000006',
 '31000004-0404-4004-8044-042400042404',
 'INFO', 'OPEN',
 'RULE_STRUCTURING_PATTERN',
 'CASH_OUT',
 'Round-number cash-out clustered with inbound credits — queue for analyst');

INSERT INTO public.compliance_operator_notes (created_at,
 entity_id, id, entity_type, author_username, body)
VALUES
(TIMESTAMPTZ '2026-05-03 07:20:52+00',
 '54141414-5454-4454-ae54-aaaaaaaaa546',
 '55151515-6565-4565-ae55-bbbbbbbbb655',
 'AML_ALERT',
 'compliance_demo',
 'Initial triage captured in local seed — escalate if wire docs missing.');

INSERT INTO public.fee_waivers (
 is_active, is_global,
 max_usage_count, usage_count, created_at, effective_from, updated_at,
 version, account_id, id,
 customer_tier, campaign_code,
 transaction_type, created_by, waiver_name, description)
VALUES
(TRUE, TRUE, 1000,
 41, TIMESTAMPTZ '2026-04-01 09:06:06+00', TIMESTAMP '2026-03-06 06:06:06',
 TIMESTAMPTZ '2026-05-03 06:06:52+00', NULL,
 NULL, '56161616-6666-4666-ae66-ddddddddb666',
 NULL, NULL,
 NULL, 'seed', 'Q2 fee holiday', 'Synthetic campaign for UI validation'),
(TRUE, TRUE, NULL,
 2, TIMESTAMPTZ '2026-04-06 06:06:52+00', TIMESTAMP '2026-03-06 06:06:52',
 TIMESTAMPTZ '2026-05-03 06:06:52+00', NULL,
 NULL, '56161616-6666-4666-ae66-ddddddddb667',
 'PREMIUM', 'PREMIUM201',
 'DEPOSIT', 'seed', 'VIP deposit waive', NULL);

INSERT INTO public.velocity_limit_breaches (
 attempted_amount, attempted_count, limit_amount, limit_count,
 breach_timestamp, account_id, id,
 breach_type,
 reason,
 limit_period, transaction_type)
VALUES
(2550.9100, NULL, 2000.0000, NULL,
 TIMESTAMPTZ '2026-05-03 07:08:52+00',
 '20000000-0000-4000-8000-000000000001',
 '57171717-7777-4777-ae77-dddddddde777',
 'AMOUNT_THRESHOLD',
 'Single transfer breached configured demo ceiling',
 'DAILY', 'TRANSFER');

INSERT INTO public.notifications (is_read,
 created_at, id,
 message, channel, recipient_id,
 severity,
 subject)
VALUES
(FALSE, TIMESTAMP '2026-05-03 05:56:52',
 '58181818-8888-4888-ae88-ffffffff8888',
 'Two AML alerts queued for QA — open compliance workspace.',
 'INBOX_ONLY', 'admin',
 'WARNING',
 'Local seed: AML queue warmup'),
(TRUE, TIMESTAMP '2026-05-02 06:56:52',
 '58181818-8888-4888-ae88-ffffffff8889',
 'FX batch completed with zero variance vs ECB mid.',
 'INBOX_ONLY', 'admin',
 'INFO',
 'Local seed: FX sync notice');
