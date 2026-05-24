--
-- PostgreSQL database dump
--


-- Dumped from database version 16.13
-- Dumped by pg_dump version 16.13




--
-- Name: account_holds; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.account_holds (
    amount numeric(19,4) NOT NULL,
    currency character varying(3) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) without time zone,
    customer_account_id uuid NOT NULL,
    id uuid NOT NULL,
    status character varying(20) NOT NULL,
    reference_id character varying(100),
    reason character varying(255),
    CONSTRAINT account_holds_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RELEASED'::character varying, 'EXPIRED'::character varying, 'SETTLED'::character varying])::text[])))
);


--
-- Name: account_limits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.account_limits (
    is_regulatory boolean NOT NULL,
    max_amount numeric(19,4),
    max_count integer,
    min_amount numeric(19,4),
    override_allowed boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    effective_from timestamp(6) with time zone NOT NULL,
    effective_until timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    customer_account_id uuid NOT NULL,
    id uuid NOT NULL,
    limit_period character varying(20) NOT NULL,
    limit_type character varying(30) NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_by character varying(100),
    description character varying(500),
    regulatory_reference character varying(255),
    CONSTRAINT account_limits_limit_period_check CHECK (((limit_period)::text = ANY ((ARRAY['DAILY'::character varying, 'WEEKLY'::character varying, 'MONTHLY'::character varying, 'QUARTERLY'::character varying, 'ANNUAL'::character varying, 'LIFETIME'::character varying])::text[]))),
    CONSTRAINT account_limits_limit_type_check CHECK (((limit_type)::text = ANY ((ARRAY['DAILY_TRANSACTION'::character varying, 'WEEKLY_TRANSACTION'::character varying, 'MONTHLY_TRANSACTION'::character varying, 'ANNUAL_TRANSACTION'::character varying, 'MAXIMUM_BALANCE'::character varying, 'MINIMUM_BALANCE'::character varying, 'OVERDRAFT_LIMIT'::character varying, 'WITHDRAWAL_LIMIT'::character varying, 'TRANSFER_LIMIT'::character varying, 'VELOCITY_LIMIT'::character varying])::text[]))),
    CONSTRAINT account_limits_max_count_check CHECK ((max_count >= 0))
);


--
-- Name: account_relationships; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.account_relationships (
    beneficiary_percentage numeric(5,2),
    is_beneficiary boolean NOT NULL,
    percentage_ownership numeric(5,2),
    created_at timestamp(6) with time zone NOT NULL,
    effective_from timestamp(6) without time zone NOT NULL,
    effective_until timestamp(6) without time zone,
    customer_account_id uuid NOT NULL,
    id uuid NOT NULL,
    user_profile_id uuid NOT NULL,
    relationship_type character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    created_by character varying(100) NOT NULL,
    permissions text,
    CONSTRAINT account_relationships_relationship_type_check CHECK (((relationship_type)::text = ANY ((ARRAY['PRIMARY_HOLDER'::character varying, 'SECONDARY_HOLDER'::character varying, 'AUTHORIZED_USER'::character varying, 'BENEFICIARY'::character varying, 'GUARDIAN'::character varying])::text[]))),
    CONSTRAINT account_relationships_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying])::text[])))
);


--
-- Name: account_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.account_transactions (
    amount numeric(19,4) NOT NULL,
    currency character varying(3) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    transaction_date timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) with time zone,
    customer_account_id uuid NOT NULL,
    gl_transaction_id uuid,
    id uuid NOT NULL,
    status character varying(20) NOT NULL,
    transaction_type character varying(30) NOT NULL,
    reference_id character varying(100),
    description character varying(255),
    CONSTRAINT account_transactions_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['DEPOSIT'::character varying, 'WITHDRAWAL'::character varying, 'TRANSFER_IN'::character varying, 'TRANSFER_OUT'::character varying, 'FEE'::character varying, 'INTEREST_CREDIT'::character varying, 'INTEREST_CHARGE'::character varying, 'ADJUSTMENT'::character varying])::text[])))
);


--
-- Name: accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.accounts (
    available_balance numeric(19,4) NOT NULL,
    currency character varying(3) NOT NULL,
    ledger_balance numeric(19,4) NOT NULL,
    closed_at timestamp(6) without time zone,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    primary_user_profile_id uuid NOT NULL,
    account_number character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    product_type character varying(30) NOT NULL,
    iban character varying(34),
    created_by character varying(100) NOT NULL,
    display_name character varying(100),
    description character varying(500),
    closure_reason character varying(255),
    metadata text,
    CONSTRAINT accounts_product_type_check CHECK (((product_type)::text = ANY ((ARRAY['CHECKING'::character varying, 'SAVINGS'::character varying, 'MONEY_MARKET'::character varying, 'CERTIFICATE_OF_DEPOSIT'::character varying, 'CREDIT_LINE'::character varying, 'INVESTMENT'::character varying])::text[]))),
    CONSTRAINT accounts_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'FROZEN'::character varying, 'CLOSED'::character varying, 'DORMANT'::character varying])::text[])))
);


--
-- Name: aml_alerts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aml_alerts (
    currency character varying(3) NOT NULL,
    investigation_hold_placed boolean NOT NULL,
    monitored_amount numeric(21,6) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    customer_party_id uuid,
    id uuid NOT NULL,
    source_account_id uuid NOT NULL,
    transaction_id uuid NOT NULL,
    severity character varying(24) NOT NULL,
    status character varying(24) NOT NULL,
    rule_code character varying(64) NOT NULL,
    transaction_type_name character varying(64) NOT NULL,
    detail_summary character varying(2000) NOT NULL,
    CONSTRAINT aml_alerts_severity_check CHECK (((severity)::text = ANY ((ARRAY['INFO'::character varying, 'WARNING'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[]))),
    CONSTRAINT aml_alerts_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'ACKNOWLEDGED'::character varying, 'CLOSED'::character varying])::text[])))
);


--
-- Name: aml_monitoring_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aml_monitoring_rules (
    enabled boolean NOT NULL,
    investigation_hold_recommended boolean NOT NULL,
    sort_order integer NOT NULL,
    threshold_max_inclusive numeric(21,6),
    threshold_min_inclusive numeric(21,6),
    id uuid NOT NULL,
    severity character varying(24) NOT NULL,
    code character varying(64) NOT NULL,
    display_name character varying(160) NOT NULL,
    match_transaction_types character varying(512),
    CONSTRAINT aml_monitoring_rules_severity_check CHECK (((severity)::text = ANY ((ARRAY['INFO'::character varying, 'WARNING'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[])))
);


--
-- Name: balance_reservations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.balance_reservations (
    currency character varying(3) NOT NULL,
    original_amount numeric(19,4),
    reserved_amount numeric(19,4) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    released_at timestamp(6) without time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint,
    account_id uuid NOT NULL,
    id uuid NOT NULL,
    transaction_id uuid NOT NULL,
    reservation_type character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    release_reason character varying(255),
    reservation_key character varying(255) NOT NULL,
    reservation_metadata jsonb,
    reservation_reference character varying(255),
    CONSTRAINT balance_reservations_reservation_type_check CHECK (((reservation_type)::text = ANY ((ARRAY['DEBIT_HOLD'::character varying, 'CREDIT_HOLD'::character varying, 'FEE_HOLD'::character varying])::text[]))),
    CONSTRAINT balance_reservations_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RELEASED'::character varying, 'CONVERTED'::character varying, 'EXPIRED'::character varying])::text[])))
);


--
-- Name: banking_preferences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.banking_preferences (
    large_transaction_alert_threshold numeric(19,4),
    low_balance_alert_threshold numeric(19,4),
    notify_email boolean NOT NULL,
    notify_push boolean NOT NULL,
    notify_sms boolean NOT NULL,
    preferred_currency character varying(3),
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    preferred_language character varying(10),
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    preferred_branch_code character varying(20),
    statement_delivery character varying(20),
    statement_frequency character varying(20),
    time_zone character varying(50)
);


--
-- Name: collaterals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collaterals (
    currency character varying(3) NOT NULL,
    insurance_expiry_date date,
    release_date date,
    valuation_amount numeric(19,4) NOT NULL,
    valuation_date date NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    collateral_type character varying(30) NOT NULL,
    collateral_reference character varying(50) NOT NULL,
    insurance_policy_number character varying(100),
    registration_number character varying(100),
    valued_by character varying(100),
    location character varying(200),
    description character varying(500) NOT NULL,
    remarks character varying(1000),
    CONSTRAINT collaterals_collateral_type_check CHECK (((collateral_type)::text = ANY ((ARRAY['REAL_ESTATE'::character varying, 'VEHICLE'::character varying, 'GOLD'::character varying, 'SECURITIES'::character varying, 'FIXED_DEPOSIT'::character varying, 'EQUIPMENT'::character varying, 'INVENTORY'::character varying, 'ACCOUNTS_RECEIVABLE'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT collaterals_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RELEASED'::character varying, 'LIQUIDATED'::character varying, 'UNDER_VALUATION'::character varying])::text[])))
);


--
-- Name: collection_activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collection_activities (
    activity_date date NOT NULL,
    follow_up_date date,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    activity_type character varying(30) NOT NULL,
    assigned_to character varying(100),
    notes character varying(1000) NOT NULL,
    CONSTRAINT collection_activities_activity_type_check CHECK (((activity_type)::text = ANY ((ARRAY['PHONE_CALL'::character varying, 'SMS'::character varying, 'EMAIL'::character varying, 'LETTER'::character varying, 'FIELD_VISIT'::character varying, 'LEGAL_NOTICE'::character varying, 'PROMISE_TO_PAY'::character varying, 'PAYMENT_ARRANGEMENT'::character varying])::text[]))),
    CONSTRAINT collection_activities_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'ESCALATED'::character varying, 'CLOSED'::character varying])::text[])))
);


--
-- Name: compensation_workflows; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compensation_workflows (
    compensation_amount numeric(19,4),
    max_retries integer NOT NULL,
    retry_count integer NOT NULL,
    completed_at timestamp(6) without time zone,
    created_at timestamp(6) with time zone NOT NULL,
    escalated_at timestamp(6) without time zone,
    next_retry_at timestamp(6) without time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint,
    compensation_transaction_id uuid,
    gl_reversal_transaction_id uuid,
    id uuid NOT NULL,
    original_transaction_id uuid NOT NULL,
    compensation_type character varying(20),
    workflow_status character varying(50) NOT NULL,
    escalated_by character varying(100),
    compensation_steps jsonb,
    escalation_reason text,
    failure_reason text NOT NULL,
    CONSTRAINT compensation_workflows_compensation_type_check CHECK (((compensation_type)::text = ANY ((ARRAY['FULL'::character varying, 'PARTIAL'::character varying, 'RESERVATION_RELEASE'::character varying])::text[]))),
    CONSTRAINT compensation_workflows_workflow_status_check CHECK (((workflow_status)::text = ANY ((ARRAY['INITIATED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'ESCALATED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: compliance_operator_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compliance_operator_notes (
    created_at timestamp(6) with time zone NOT NULL,
    entity_id uuid NOT NULL,
    id uuid NOT NULL,
    entity_type character varying(48) NOT NULL,
    author_username character varying(128) NOT NULL,
    body text NOT NULL
);


--
-- Name: contact_details; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contact_details (
    is_primary boolean NOT NULL,
    is_verified boolean NOT NULL,
    deleted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    verified_at timestamp(6) without time zone,
    version bigint NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    type character varying(20) NOT NULL,
    verified_by character varying(100),
    value character varying(255) NOT NULL,
    CONSTRAINT contact_details_type_check CHECK (((type)::text = ANY ((ARRAY['EMAIL'::character varying, 'PHONE_MOBILE'::character varying, 'PHONE_HOME'::character varying, 'PHONE_WORK'::character varying, 'FAX'::character varying, 'WEBSITE'::character varying])::text[])))
);


--
-- Name: customer_addresses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_addresses (
    country character varying(2) NOT NULL,
    is_primary boolean NOT NULL,
    valid_from date,
    valid_to date,
    deleted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    postal_code character varying(20) NOT NULL,
    type character varying(20) NOT NULL,
    city character varying(100) NOT NULL,
    line1 character varying(100) NOT NULL,
    line2 character varying(100),
    state character varying(100),
    CONSTRAINT customer_addresses_type_check CHECK (((type)::text = ANY ((ARRAY['LEGAL'::character varying, 'PHYSICAL'::character varying, 'MAILING'::character varying, 'REGISTERED_OFFICE'::character varying])::text[])))
);


--
-- Name: customer_audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_audit_logs (
    changed_at timestamp(6) without time zone NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    related_entity_id uuid,
    ip_address character varying(45),
    channel character varying(50),
    action character varying(60) NOT NULL,
    related_entity_type character varying(60),
    changed_by character varying(100) NOT NULL,
    correlation_id character varying(100),
    field_name character varying(100),
    change_reason character varying(500),
    new_value text,
    old_value text,
    CONSTRAINT customer_audit_logs_action_check CHECK (((action)::text = ANY ((ARRAY['PROFILE_UPDATE'::character varying, 'STATUS_CHANGE'::character varying, 'ANONYMIZATION'::character varying, 'KYC_UPDATE'::character varying, 'ADDRESS_ADDED'::character varying, 'ADDRESS_UPDATED'::character varying, 'ADDRESS_REMOVED'::character varying, 'CONTACT_VERIFIED'::character varying, 'DOCUMENT_ADDED'::character varying, 'DOCUMENT_REMOVED'::character varying, 'CONSENT_CHANGE'::character varying, 'RELATIONSHIP_ADDED'::character varying, 'RELATIONSHIP_REMOVED'::character varying, 'DSAR_SUBMITTED'::character varying, 'DSAR_FULFILLED'::character varying, 'DSAR_REJECTED'::character varying])::text[])))
);


--
-- Name: customer_consents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_consents (
    granted boolean NOT NULL,
    expires_at timestamp(6) without time zone,
    recorded_at timestamp(6) without time zone NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    ip_address character varying(45),
    capture_channel character varying(50),
    consent_type character varying(50) NOT NULL,
    policy_version character varying(50),
    recorded_by character varying(100),
    notes character varying(500),
    CONSTRAINT customer_consents_consent_type_check CHECK (((consent_type)::text = ANY ((ARRAY['MARKETING_EMAIL'::character varying, 'MARKETING_SMS'::character varying, 'MARKETING_PHONE'::character varying, 'DATA_SHARING_THIRD_PARTY'::character varying, 'DATA_SHARING_AFFILIATES'::character varying, 'CREDIT_REPORTING'::character varying, 'AUTOMATED_DECISION_MAKING'::character varying, 'TERMS_AND_CONDITIONS'::character varying, 'PRIVACY_POLICY'::character varying, 'BIOMETRIC_DATA_PROCESSING'::character varying])::text[])))
);


--
-- Name: customer_data_retention; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_data_retention (
    anonymized boolean NOT NULL,
    relationship_ended_at date NOT NULL,
    retention_expires_at date NOT NULL,
    retention_years integer NOT NULL,
    anonymized_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    anonymization_job_ref character varying(100),
    anonymized_by character varying(100),
    legal_basis character varying(100) NOT NULL,
    notes character varying(1000)
);


--
-- Name: customer_onboardings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_onboardings (
    account_setup_at timestamp(6) without time zone,
    completed_at timestamp(6) without time zone,
    kyc_completed_at timestamp(6) without time zone,
    started_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    welcome_kit_sent_at timestamp(6) without time zone,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    originating_branch character varying(20),
    status character varying(30) NOT NULL,
    onboarding_channel character varying(50),
    referral_code character varying(50),
    external_reference character varying(100),
    initiated_by character varying(100),
    outcome_reason text,
    CONSTRAINT customer_onboardings_status_check CHECK (((status)::text = ANY ((ARRAY['INITIATED'::character varying, 'KYC_IN_PROGRESS'::character varying, 'KYC_COMPLETED'::character varying, 'ACCOUNT_SETUP'::character varying, 'WELCOME_KIT_SENT'::character varying, 'COMPLETED'::character varying, 'ABANDONED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: customer_outbox; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_outbox (
    retry_count integer,
    occurred_at timestamp(6) with time zone NOT NULL,
    processed_at timestamp(6) with time zone,
    aggregate_id uuid,
    id uuid NOT NULL,
    aggregate_type character varying(128) NOT NULL,
    event_type character varying(256) NOT NULL,
    payload_json text NOT NULL
);


--
-- Name: customer_relationships; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_relationships (
    active boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    removed_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    primary_customer_id uuid NOT NULL,
    related_customer_id uuid NOT NULL,
    relationship_type character varying(30) NOT NULL,
    created_by character varying(100),
    removed_by character varying(100),
    notes character varying(500),
    CONSTRAINT customer_relationships_relationship_type_check CHECK (((relationship_type)::text = ANY ((ARRAY['SPOUSE'::character varying, 'BUSINESS_PARTNER'::character varying, 'PARENT'::character varying, 'CHILD'::character varying, 'SIBLING'::character varying, 'AUTHORIZED_USER'::character varying, 'POWER_OF_ATTORNEY'::character varying, 'BENEFICIARY'::character varying, 'GUARDIAN'::character varying, 'CORPORATE_OFFICER'::character varying])::text[])))
);


--
-- Name: customer_risk_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_risk_profiles (
    pep_match boolean NOT NULL,
    sanction_match boolean NOT NULL,
    last_evaluated_at timestamp(6) without time zone NOT NULL,
    customer_id uuid NOT NULL,
    risk_rating character varying(255) NOT NULL,
    CONSTRAINT customer_risk_profiles_risk_rating_check CHECK (((risk_rating)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'UNACCEPTABLE'::character varying])::text[])))
);


--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    annual_income numeric(19,4),
    date_of_birth date,
    incorporation_country character varying(2),
    incorporation_date date,
    nationality character varying(2),
    pep_flag boolean NOT NULL,
    residence_country character varying(2),
    sanction_flag boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    linked_identity_user_id uuid,
    customer_number character varying(20) NOT NULL,
    gender character varying(20),
    kyc_status character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    type character varying(20) NOT NULL,
    legal_entity_type character varying(30),
    marital_status character varying(30),
    segment character varying(30),
    business_registration_number character varying(50),
    tax_id character varying(50),
    linked_identity_username character varying(80),
    first_name character varying(100),
    last_name character varying(100),
    mother_maiden_name character varying(100),
    occupation character varying(100),
    place_of_birth character varying(100),
    business_name character varying(200),
    blocked_reason character varying(500),
    CONSTRAINT customers_gender_check CHECK (((gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying, 'NON_BINARY'::character varying, 'PREFER_NOT_TO_SAY'::character varying])::text[]))),
    CONSTRAINT customers_kyc_status_check CHECK (((kyc_status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_REVIEW'::character varying, 'VERIFIED'::character varying, 'REJECTED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT customers_legal_entity_type_check CHECK (((legal_entity_type)::text = ANY ((ARRAY['LLC'::character varying, 'CORPORATION'::character varying, 'PARTNERSHIP'::character varying, 'LLP'::character varying, 'SOLE_PROPRIETORSHIP'::character varying, 'NON_PROFIT'::character varying, 'COOPERATIVE'::character varying, 'TRUST'::character varying, 'FOUNDATION'::character varying, 'GOVERNMENT_ENTITY'::character varying, 'BRANCH_OFFICE'::character varying])::text[]))),
    CONSTRAINT customers_marital_status_check CHECK (((marital_status)::text = ANY ((ARRAY['SINGLE'::character varying, 'MARRIED'::character varying, 'DIVORCED'::character varying, 'WIDOWED'::character varying, 'SEPARATED'::character varying, 'DOMESTIC_PARTNERSHIP'::character varying, 'PREFER_NOT_TO_SAY'::character varying])::text[]))),
    CONSTRAINT customers_segment_check CHECK (((segment)::text = ANY ((ARRAY['RETAIL'::character varying, 'PREMIUM'::character varying, 'PRIVATE_BANKING'::character varying, 'SME'::character varying, 'CORPORATE'::character varying, 'VIP'::character varying, 'MASS_MARKET'::character varying, 'NON_RESIDENT'::character varying])::text[]))),
    CONSTRAINT customers_status_check CHECK (((status)::text = ANY ((ARRAY['PROSPECT'::character varying, 'ACTIVE'::character varying, 'INACTIVE'::character varying, 'BLOCKED'::character varying, 'DECEASED'::character varying, 'CLOSED'::character varying, 'ANONYMIZED'::character varying])::text[]))),
    CONSTRAINT customers_type_check CHECK (((type)::text = ANY ((ARRAY['INDIVIDUAL'::character varying, 'BUSINESS'::character varying, 'TRUST'::character varying])::text[])))
);


--
-- Name: data_subject_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.data_subject_requests (
    deferred_until date,
    due_by date NOT NULL,
    extended boolean NOT NULL,
    extension_notified_at date,
    fulfilled_at date,
    received_at date NOT NULL,
    received_at_ts timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    channel character varying(30),
    reference_number character varying(30),
    request_type character varying(30) NOT NULL,
    status character varying(30) NOT NULL,
    handled_by character varying(100),
    customer_notes text,
    outcome_reason text,
    CONSTRAINT data_subject_requests_request_type_check CHECK (((request_type)::text = ANY ((ARRAY['ACCESS'::character varying, 'ERASURE'::character varying, 'PORTABILITY'::character varying, 'RECTIFICATION'::character varying, 'OBJECTION'::character varying, 'RESTRICTION'::character varying])::text[]))),
    CONSTRAINT data_subject_requests_status_check CHECK (((status)::text = ANY ((ARRAY['RECEIVED'::character varying, 'IDENTITY_VERIFICATION'::character varying, 'IN_REVIEW'::character varying, 'FULFILLED'::character varying, 'DEFERRED'::character varying, 'REJECTED'::character varying, 'WITHDRAWN'::character varying])::text[])))
);


--
-- Name: early_settlements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.early_settlements (
    approved_date date,
    cancelled_date date,
    currency character varying(3) NOT NULL,
    outstanding_fees numeric(19,4) NOT NULL,
    outstanding_interest numeric(19,4) NOT NULL,
    outstanding_principal numeric(19,4) NOT NULL,
    penalty_amount numeric(19,4) NOT NULL,
    quote_date date NOT NULL,
    rebate_amount numeric(19,4) NOT NULL,
    rejected_date date,
    settled_date date,
    settlement_amount numeric(19,4) NOT NULL,
    valid_until date NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    calculation_method character varying(30) NOT NULL,
    quote_reference character varying(50) NOT NULL,
    approved_by character varying(100),
    cancelled_by character varying(100),
    payment_reference character varying(100),
    rejected_by character varying(100),
    cancellation_reason character varying(500),
    rejection_reason character varying(500),
    remarks character varying(1000),
    CONSTRAINT early_settlements_calculation_method_check CHECK (((calculation_method)::text = ANY ((ARRAY['FULL_OUTSTANDING'::character varying, 'DISCOUNTED'::character varying])::text[]))),
    CONSTRAINT early_settlements_status_check CHECK (((status)::text = ANY ((ARRAY['QUOTE'::character varying, 'PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying, 'EXPIRED'::character varying])::text[])))
);


--
-- Name: exchange_rates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exchange_rates (
    ask_rate numeric(19,8),
    bid_rate numeric(19,8),
    rate numeric(19,8) NOT NULL,
    rate_date date NOT NULL,
    source_currency character varying(3) NOT NULL,
    target_currency character varying(3) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version bigint NOT NULL,
    id uuid NOT NULL,
    rate_type character varying(20) NOT NULL,
    created_by character varying(100),
    updated_by character varying(100),
    CONSTRAINT exchange_rates_rate_type_check CHECK (((rate_type)::text = ANY ((ARRAY['SPOT'::character varying, 'EOD'::character varying, 'AVG_MONTH'::character varying, 'OFFICIAL'::character varying])::text[])))
);


--
-- Name: fee_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_rules (
    currency character varying(3),
    fixed_amount numeric(19,4),
    is_active boolean NOT NULL,
    is_compoundable boolean NOT NULL,
    is_promotional boolean NOT NULL,
    max_transaction_amount numeric(19,4),
    maximum_fee numeric(19,4),
    min_transaction_amount numeric(19,4),
    minimum_fee numeric(19,4),
    percentage_rate numeric(5,4),
    priority integer,
    time_based_end time(0) without time zone,
    time_based_start time(0) without time zone,
    created_at timestamp(6) with time zone NOT NULL,
    effective_from timestamp(6) without time zone NOT NULL,
    effective_to timestamp(6) without time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint,
    gl_revenue_account_id uuid,
    id uuid NOT NULL,
    customer_tier character varying(20) NOT NULL,
    fee_type character varying(50) NOT NULL,
    transaction_type character varying(50) NOT NULL,
    created_by character varying(100) NOT NULL,
    rule_name character varying(100) NOT NULL,
    updated_by character varying(100),
    description text,
    metadata jsonb,
    tier_configuration jsonb,
    CONSTRAINT fee_rules_customer_tier_check CHECK (((customer_tier)::text = ANY ((ARRAY['BASIC'::character varying, 'PREMIUM'::character varying, 'VIP'::character varying, 'ENTERPRISE'::character varying])::text[]))),
    CONSTRAINT fee_rules_fee_type_check CHECK (((fee_type)::text = ANY ((ARRAY['FIXED_AMOUNT'::character varying, 'PERCENTAGE'::character varying, 'TIERED'::character varying, 'MINIMUM'::character varying, 'MAXIMUM'::character varying, 'FLAT'::character varying, 'NONE'::character varying])::text[]))),
    CONSTRAINT fee_rules_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['P2P'::character varying, 'CASH_IN'::character varying, 'CASH_OUT'::character varying, 'BILL_PAYMENT'::character varying, 'MERCHANT_PURCHASE'::character varying, 'TRANSFER'::character varying, 'DEPOSIT'::character varying, 'REFUND'::character varying])::text[])))
);


--
-- Name: fee_waivers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_waivers (
    is_active boolean NOT NULL,
    is_global boolean NOT NULL,
    max_usage_count integer,
    usage_count integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    effective_from timestamp(6) without time zone NOT NULL,
    effective_to timestamp(6) without time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint,
    account_id uuid,
    id uuid NOT NULL,
    customer_tier character varying(20),
    campaign_code character varying(50),
    transaction_type character varying(50),
    created_by character varying(100) NOT NULL,
    updated_by character varying(100),
    waiver_name character varying(100) NOT NULL,
    conditions jsonb,
    description text,
    metadata jsonb,
    CONSTRAINT fee_waivers_customer_tier_check CHECK (((customer_tier)::text = ANY ((ARRAY['BASIC'::character varying, 'PREMIUM'::character varying, 'VIP'::character varying, 'ENTERPRISE'::character varying])::text[]))),
    CONSTRAINT fee_waivers_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['P2P'::character varying, 'CASH_IN'::character varying, 'CASH_OUT'::character varying, 'BILL_PAYMENT'::character varying, 'MERCHANT_PURCHASE'::character varying, 'TRANSFER'::character varying, 'DEPOSIT'::character varying, 'REFUND'::character varying])::text[])))
);


--
-- Name: fiscal_periods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fiscal_periods (
    end_date date NOT NULL,
    fiscal_year integer NOT NULL,
    period_number integer NOT NULL,
    start_date date NOT NULL,
    closed_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    reopened_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    status character varying(20) NOT NULL,
    name character varying(50) NOT NULL,
    reopened_by character varying(100),
    closed_by character varying(255),
    CONSTRAINT fiscal_periods_fiscal_year_check CHECK (((fiscal_year >= 1900) AND (fiscal_year <= 2200))),
    CONSTRAINT fiscal_periods_period_number_check CHECK (((period_number >= 1) AND (period_number <= 13))),
    CONSTRAINT fiscal_periods_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'ADJUSTING'::character varying, 'CLOSED'::character varying, 'LOCKED'::character varying])::text[])))
);


--
-- Name: fx_spreads; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fx_spreads (
    source_currency character varying(3) NOT NULL,
    spread_rate numeric(19,6) NOT NULL,
    target_currency character varying(3) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint,
    id uuid NOT NULL,
    created_by character varying(100) NOT NULL
);


--
-- Name: gl_account_mappings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_account_mappings (
    is_active boolean NOT NULL,
    weight integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    deactivated_at timestamp(6) without time zone,
    customer_account_id uuid NOT NULL,
    gl_account_id uuid NOT NULL,
    id uuid NOT NULL,
    mapping_type character varying(30) NOT NULL,
    created_by character varying(100) NOT NULL,
    deactivated_by character varying(100),
    deactivation_reason character varying(255),
    description character varying(255),
    CONSTRAINT gl_account_mappings_mapping_type_check CHECK (((mapping_type)::text = ANY ((ARRAY['PRIMARY_BALANCE'::character varying, 'INTEREST_ACCRUAL'::character varying, 'FEE_COLLECTION'::character varying, 'OVERDRAFT_FACILITY'::character varying, 'ESCROW_HOLD'::character varying])::text[])))
);


--
-- Name: gl_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_accounts (
    contra boolean NOT NULL,
    currency character varying(3) NOT NULL,
    inactivated_on date,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint NOT NULL,
    cash_flow_category character varying(10) NOT NULL,
    normal_balance character varying(10) NOT NULL,
    id uuid NOT NULL,
    parent_id uuid,
    status character varying(20) NOT NULL,
    type character varying(20) NOT NULL,
    code character varying(50) NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_by character varying(100),
    inactivation_reason character varying(500),
    description character varying(1000),
    metadata text,
    name character varying(255) NOT NULL,
    CONSTRAINT gl_accounts_cash_flow_category_check CHECK (((cash_flow_category)::text = ANY ((ARRAY['OPERATING'::character varying, 'INVESTING'::character varying, 'FINANCING'::character varying, 'NONE'::character varying])::text[]))),
    CONSTRAINT gl_accounts_normal_balance_check CHECK (((normal_balance)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT'::character varying])::text[]))),
    CONSTRAINT gl_accounts_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[]))),
    CONSTRAINT gl_accounts_type_check CHECK (((type)::text = ANY ((ARRAY['ASSET'::character varying, 'LIABILITY'::character varying, 'EQUITY'::character varying, 'REVENUE'::character varying, 'EXPENSE'::character varying])::text[])))
);


--
-- Name: gl_audit_trail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_audit_trail (
    transaction_amount numeric(19,4),
    transaction_currency character varying(3),
    performed_at timestamp(6) with time zone NOT NULL,
    correlation_id uuid,
    entity_id uuid NOT NULL,
    id uuid NOT NULL,
    ip_address character varying(45),
    action character varying(50) NOT NULL,
    entity_type character varying(50) NOT NULL,
    performed_by character varying(100) NOT NULL,
    new_values text,
    old_values text,
    reason text,
    session_id character varying(255),
    CONSTRAINT gl_audit_trail_action_check CHECK (((action)::text = ANY ((ARRAY['CREATE'::character varying, 'UPDATE'::character varying, 'DELETE'::character varying, 'STATUS_CHANGE'::character varying, 'REVERSE'::character varying, 'REACTIVATE'::character varying, 'PERIOD_CLOSE'::character varying, 'PERIOD_REOPEN'::character varying, 'BALANCE_ADJUSTMENT'::character varying, 'APPROVAL'::character varying, 'REJECTION'::character varying, 'RECONCILIATION'::character varying, 'IMPORT'::character varying, 'EXPORT'::character varying, 'CONFIG_CHANGE'::character varying])::text[]))),
    CONSTRAINT gl_audit_trail_entity_type_check CHECK (((entity_type)::text = ANY ((ARRAY['GL_ACCOUNT'::character varying, 'GL_TRANSACTION'::character varying, 'GL_JOURNAL_ENTRY'::character varying, 'OPERATIONAL_CONFIG'::character varying, 'FISCAL_PERIOD'::character varying])::text[])))
);


--
-- Name: gl_authorization_limits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_authorization_limits (
    approval_limit numeric(19,4) NOT NULL,
    currency character varying(3) NOT NULL,
    is_active boolean NOT NULL,
    maker_limit numeric(19,4) NOT NULL,
    required_approvals integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    approval_role character varying(50) NOT NULL,
    transaction_source character varying(50),
    created_by character varying(100),
    CONSTRAINT gl_authorization_limits_approval_role_check CHECK (((approval_role)::text = ANY ((ARRAY['ACCOUNTANT'::character varying, 'SENIOR_ACCOUNTANT'::character varying, 'MANAGER'::character varying, 'CONTROLLER'::character varying, 'CFO'::character varying])::text[]))),
    CONSTRAINT gl_authorization_limits_transaction_source_check CHECK (((transaction_source)::text = ANY ((ARRAY['MANUAL_ENTRY'::character varying, 'CORRECTION'::character varying, 'RECLASSIFICATION'::character varying, 'SYSTEM_GENERATED'::character varying, 'BATCH_IMPORT'::character varying, 'INTERFACE_FEED'::character varying, 'AUTO_CALCULATED'::character varying])::text[])))
);


--
-- Name: gl_daily_balances; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_daily_balances (
    balance_date date NOT NULL,
    closing_balance numeric(19,4) NOT NULL,
    opening_balance numeric(19,4) NOT NULL,
    total_credits numeric(19,4) NOT NULL,
    total_debits numeric(19,4) NOT NULL,
    transaction_count integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    account_id uuid NOT NULL,
    id uuid NOT NULL,
    CONSTRAINT gl_daily_balances_transaction_count_check CHECK ((transaction_count >= 0))
);


--
-- Name: gl_journal_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_journal_entries (
    base_credit_amount numeric(19,4) NOT NULL,
    base_debit_amount numeric(19,4) NOT NULL,
    credit_amount numeric(19,4) NOT NULL,
    currency character varying(3) NOT NULL,
    debit_amount numeric(19,4) NOT NULL,
    exchange_rate numeric(19,6) NOT NULL,
    line_number integer NOT NULL,
    value_date date NOT NULL,
    account_id uuid NOT NULL,
    id uuid NOT NULL,
    transaction_id uuid NOT NULL,
    description text,
    CONSTRAINT gl_journal_entries_check CHECK (((debit_amount >= (0)::numeric) AND (credit_amount >= (0)::numeric) AND (((debit_amount > (0)::numeric) AND (credit_amount = (0)::numeric)) OR ((debit_amount = (0)::numeric) AND (credit_amount > (0)::numeric)))))
);


--
-- Name: gl_reconciliations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_reconciliations (
    reconciliation_date date NOT NULL,
    id uuid NOT NULL,
    transaction_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    reconciled_by character varying(100) NOT NULL,
    external_reference character varying(255),
    CONSTRAINT gl_reconciliations_status_check CHECK (((status)::text = ANY ((ARRAY['RECONCILED'::character varying, 'PENDING'::character varying, 'VOIDED'::character varying])::text[])))
);


--
-- Name: gl_revaluation_details; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_revaluation_details (
    account_balance numeric(19,4) NOT NULL,
    account_currency character varying(3) NOT NULL,
    new_base_value numeric(19,4) NOT NULL,
    new_exchange_rate numeric(19,8) NOT NULL,
    old_base_value numeric(19,4) NOT NULL,
    old_exchange_rate numeric(19,8) NOT NULL,
    unrealized_gain_loss numeric(19,4) NOT NULL,
    account_id uuid NOT NULL,
    id uuid NOT NULL,
    journal_transaction_id uuid,
    revaluation_run_id uuid NOT NULL
);


--
-- Name: gl_revaluation_runs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_revaluation_runs (
    accounts_failed integer NOT NULL,
    accounts_processed integer NOT NULL,
    accounts_revalued integer NOT NULL,
    base_currency character varying(3) NOT NULL,
    revaluation_date date NOT NULL,
    total_adjustment numeric(19,4) NOT NULL,
    executed_at timestamp(6) without time zone NOT NULL,
    correlation_id uuid,
    id uuid NOT NULL,
    trigger_type character varying(20) NOT NULL,
    executed_by character varying(100) NOT NULL,
    notes character varying(500),
    CONSTRAINT gl_revaluation_runs_accounts_failed_check CHECK ((accounts_failed >= 0)),
    CONSTRAINT gl_revaluation_runs_accounts_processed_check CHECK ((accounts_processed >= 0)),
    CONSTRAINT gl_revaluation_runs_accounts_revalued_check CHECK ((accounts_revalued >= 0))
);


--
-- Name: gl_suspense_clearing_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_suspense_clearing_rules (
    age_threshold_days integer,
    amount_threshold numeric(19,4),
    currency character varying(3),
    is_active boolean NOT NULL,
    priority integer NOT NULL,
    requires_approval boolean NOT NULL,
    approved_date timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    target_account_id uuid NOT NULL,
    rule_type character varying(50) NOT NULL,
    approved_by character varying(100),
    created_by character varying(100),
    source_system_filter character varying(100),
    name character varying(200) NOT NULL,
    match_pattern character varying(500),
    CONSTRAINT gl_suspense_clearing_rules_rule_type_check CHECK (((rule_type)::text = ANY ((ARRAY['PATTERN_MATCH'::character varying, 'AGE_THRESHOLD'::character varying, 'AMOUNT_THRESHOLD'::character varying, 'SOURCE_SYSTEM'::character varying, 'STANDING_INSTRUCTION'::character varying])::text[])))
);


--
-- Name: gl_suspense_escalations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_suspense_escalations (
    due_date date NOT NULL,
    escalated_date date NOT NULL,
    is_resolved boolean NOT NULL,
    resolved_date date,
    sla_breached boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    suspense_item_id uuid NOT NULL,
    escalation_level character varying(50) NOT NULL,
    assigned_to character varying(100) NOT NULL,
    created_by character varying(100),
    resolved_by character varying(100),
    escalation_notes character varying(2000),
    resolution_notes character varying(2000),
    CONSTRAINT gl_suspense_escalations_escalation_level_check CHECK (((escalation_level)::text = ANY ((ARRAY['LEVEL_1_SUPERVISOR'::character varying, 'LEVEL_2_MANAGER'::character varying, 'LEVEL_3_SENIOR_MANAGEMENT'::character varying, 'LEVEL_4_EXECUTIVE'::character varying, 'CRITICAL_BOARD_LEVEL'::character varying])::text[])))
);


--
-- Name: gl_suspense_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_suspense_items (
    amount numeric(19,4) NOT NULL,
    cleared_date date,
    currency character varying(3) NOT NULL,
    posting_date date NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    clearing_transaction_id uuid,
    gl_transaction_id uuid NOT NULL,
    id uuid NOT NULL,
    target_account_id uuid,
    status character varying(30) NOT NULL,
    reason_code character varying(50) NOT NULL,
    assigned_to character varying(100),
    cleared_by character varying(100),
    created_by character varying(100),
    external_reference character varying(100),
    source_system character varying(100),
    description character varying(500),
    investigation_notes character varying(2000),
    CONSTRAINT gl_suspense_items_reason_code_check CHECK (((reason_code)::text = ANY ((ARRAY['INVALID_ACCOUNT_NUMBER'::character varying, 'INCOMPLETE_BENEFICIARY_INFO'::character varying, 'MISSING_PAYMENT_REFERENCE'::character varying, 'SYSTEM_ERROR'::character varying, 'DUPLICATE_TRANSACTION'::character varying, 'RECONCILIATION_DIFFERENCE'::character varying, 'FX_CONVERSION_ERROR'::character varying, 'INTEGRATION_FAILURE'::character varying, 'AMOUNT_MISMATCH'::character varying, 'UNIDENTIFIED_DEPOSIT'::character varying, 'ORPHAN_REVERSAL'::character varying, 'MANUAL_ENTRY_PENDING'::character varying, 'AWAITING_DOCUMENTATION'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT gl_suspense_items_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'UNDER_INVESTIGATION'::character varying, 'ESCALATED'::character varying, 'CLEARED'::character varying, 'AUTO_CLEARED'::character varying, 'WRITTEN_OFF'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: gl_transaction_approvals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_transaction_approvals (
    approval_level integer NOT NULL,
    approval_timestamp timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    transaction_id uuid NOT NULL,
    action character varying(20) NOT NULL,
    ip_address character varying(50),
    approved_by character varying(100) NOT NULL,
    comments character varying(500),
    user_agent character varying(255),
    CONSTRAINT gl_transaction_approvals_action_check CHECK (((action)::text = ANY ((ARRAY['APPROVED'::character varying, 'REJECTED'::character varying, 'RETURNED'::character varying])::text[])))
);


--
-- Name: gl_transaction_sequences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_transaction_sequences (
    last_assigned_number bigint NOT NULL,
    last_updated timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    fiscal_period_id uuid NOT NULL,
    id uuid NOT NULL
);


--
-- Name: gl_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gl_transactions (
    currency character varying(3) NOT NULL,
    transaction_date date NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    posting_date timestamp(6) with time zone,
    submitted_at timestamp(6) without time zone,
    transaction_number bigint NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    reversed_by uuid,
    status character varying(20) NOT NULL,
    source character varying(50) NOT NULL,
    created_by character varying(100) NOT NULL,
    posted_by character varying(100),
    reference_id character varying(100) NOT NULL,
    submitted_by character varying(100),
    description text NOT NULL,
    CONSTRAINT gl_transactions_source_check CHECK (((source)::text = ANY ((ARRAY['MANUAL_ENTRY'::character varying, 'CORRECTION'::character varying, 'RECLASSIFICATION'::character varying, 'SYSTEM_GENERATED'::character varying, 'BATCH_IMPORT'::character varying, 'INTERFACE_FEED'::character varying, 'AUTO_CALCULATED'::character varying])::text[]))),
    CONSTRAINT gl_transactions_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING_APPROVAL'::character varying, 'POSTED'::character varying, 'REVERSED'::character varying, 'REJECTED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: guarantors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.guarantors (
    guarantee_percentage numeric(5,2),
    guaranteed_amount numeric(19,4) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    released_date timestamp(6) with time zone,
    removed_date timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    verified_date timestamp(6) with time zone,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid,
    loan_application_id uuid,
    status character varying(20) NOT NULL,
    guarantor_type character varying(30) NOT NULL,
    released_by character varying(100),
    removed_by character varying(100),
    verified_by character varying(100),
    removal_reason character varying(500),
    remarks character varying(1000),
    CONSTRAINT guarantors_guarantor_type_check CHECK (((guarantor_type)::text = ANY ((ARRAY['INDIVIDUAL'::character varying, 'CORPORATE'::character varying, 'GOVERNMENT'::character varying, 'BANK_GUARANTEE'::character varying])::text[]))),
    CONSTRAINT guarantors_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'RELEASED'::character varying, 'INVOKED'::character varying, 'REMOVED'::character varying])::text[])))
);


--
-- Name: holidays; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.holidays (
    country_code character varying(2) NOT NULL,
    holiday_date date NOT NULL,
    holiday_year integer NOT NULL,
    is_bank_holiday boolean NOT NULL,
    is_observed_holiday boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    version bigint,
    region_code character varying(10),
    id uuid NOT NULL,
    holiday_type character varying(20) NOT NULL,
    created_by character varying(100),
    name character varying(100) NOT NULL,
    updated_by character varying(100),
    description character varying(500),
    CONSTRAINT holidays_holiday_type_check CHECK (((holiday_type)::text = ANY ((ARRAY['PUBLIC'::character varying, 'BANK'::character varying, 'RELIGIOUS'::character varying, 'REGIONAL'::character varying, 'OBSERVANCE'::character varying])::text[])))
);


--
-- Name: identification_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identification_documents (
    expiry_date date,
    is_verified boolean NOT NULL,
    issue_date date,
    issuing_country character varying(2) NOT NULL,
    deleted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    verified_at timestamp(6) without time zone,
    version bigint NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    document_status character varying(20) NOT NULL,
    type character varying(30) NOT NULL,
    document_number character varying(50) NOT NULL,
    issuing_authority character varying(100),
    verified_by character varying(100),
    CONSTRAINT identification_documents_document_status_check CHECK (((document_status)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'UNDER_REVIEW'::character varying, 'VERIFIED'::character varying, 'REJECTED'::character varying, 'EXPIRED'::character varying, 'SUPERSEDED'::character varying])::text[]))),
    CONSTRAINT identification_documents_type_check CHECK (((type)::text = ANY ((ARRAY['PASSPORT'::character varying, 'DRIVERS_LICENSE'::character varying, 'NATIONAL_ID'::character varying, 'SOCIAL_SECURITY_CARD'::character varying, 'TAX_ID_DOCUMENT'::character varying, 'UTILITY_BILL'::character varying, 'INCORPORATION_CERTIFICATE'::character varying])::text[])))
);


--
-- Name: identity_approval_workflow_steps; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_approval_workflow_steps (
    step_order integer NOT NULL,
    acted_at timestamp(6) without time zone,
    acted_by_user_id uuid,
    id uuid NOT NULL,
    workflow_id uuid NOT NULL,
    step_status character varying(20) NOT NULL,
    required_gl_role character varying(30) NOT NULL,
    comments character varying(500),
    CONSTRAINT identity_approval_workflow_steps_step_status_check CHECK (((step_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'SKIPPED'::character varying])::text[])))
);


--
-- Name: identity_approval_workflows; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_approval_workflows (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    initiator_id uuid,
    status character varying(20) NOT NULL,
    initiator_username character varying(80),
    resource_type character varying(80) NOT NULL,
    resource_id character varying(120) NOT NULL,
    rejection_reason character varying(500),
    CONSTRAINT identity_approval_workflows_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: identity_audit_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_audit_events (
    approval_date timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    approved_by_user_id uuid,
    changed_by_user_id uuid,
    id uuid NOT NULL,
    user_id uuid,
    ip_address character varying(45),
    event_type character varying(50) NOT NULL,
    approved_by_username character varying(80),
    changed_by_username character varying(80),
    username character varying(80),
    user_agent character varying(500),
    details character varying(1000),
    current_value character varying(2000),
    previous_value character varying(2000),
    details_json text,
    CONSTRAINT identity_audit_events_event_type_check CHECK (((event_type)::text = ANY ((ARRAY['LOGIN_SUCCESS'::character varying, 'LOGIN_FAILURE'::character varying, 'LOGOUT'::character varying, 'OAUTH2_AUTHORIZATION_ISSUED'::character varying, 'OAUTH2_AUTHORIZATION_REVOKED'::character varying, 'MFA_CHALLENGE_PRESENTED'::character varying, 'MFA_SUCCESS'::character varying, 'MFA_FAILURE'::character varying, 'MFA_RECOVERY_CODE_USED'::character varying, 'MFA_ENABLED'::character varying, 'MFA_DISABLED'::character varying, 'PASSWORD_CHANGED'::character varying, 'PASSWORD_EXPIRED'::character varying, 'PASSWORD_FORCE_CHANGE_SET'::character varying, 'ACCOUNT_LOCKED_AUTO'::character varying, 'ACCOUNT_LOCKED_ADMIN'::character varying, 'ACCOUNT_UNLOCKED'::character varying, 'ACCOUNT_DISABLED'::character varying, 'ACCOUNT_ENABLED'::character varying, 'ACCOUNT_EXPIRY_SET'::character varying, 'ACCOUNT_EXPIRY_CLEARED'::character varying, 'ACCOUNT_EXPIRY_WARNING_SENT'::character varying, 'ACCOUNT_PROVISIONING_APPROVED'::character varying, 'ACCOUNT_PROVISIONING_REJECTED'::character varying, 'ACCOUNT_SUSPENDED'::character varying, 'ACCOUNT_REACTIVATED'::character varying, 'USER_DEPROVISIONED'::character varying, 'ROLE_ASSIGNED'::character varying, 'ROLE_ASSIGNMENT_HIERARCHY_DENIED'::character varying, 'ROLE_ASSIGNMENT_SOD_VIOLATION'::character varying, 'ROLE_REVOKED'::character varying, 'ROLE_CREATED'::character varying, 'ROLE_UPDATED'::character varying, 'ROLE_DELETED'::character varying, 'ROLE_PERMISSIONS_CHANGED'::character varying, 'PERMISSION_ADDED'::character varying, 'PERMISSION_REMOVED'::character varying, 'USER_CREATED'::character varying, 'USER_UPDATED'::character varying, 'COMPLIANCE_STATUS_CHANGED'::character varying, 'KYC_VERIFIED'::character varying, 'AML_SCREENING_COMPLETED'::character varying, 'PEP_SCREENING_COMPLETED'::character varying, 'DOA_CREATED'::character varying, 'DOA_REVOKED'::character varying, 'APPROVAL_WORKFLOW_STARTED'::character varying, 'APPROVAL_WORKFLOW_STEP_APPROVED'::character varying, 'APPROVAL_WORKFLOW_REJECTED'::character varying, 'APPROVAL_WORKFLOW_CANCELLED'::character varying, 'API_ACCESS_DENIED'::character varying])::text[])))
);


--
-- Name: identity_delegations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_delegations (
    approval_limit numeric(19,4),
    currency character varying(3),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    valid_from timestamp(6) without time zone NOT NULL,
    valid_until timestamp(6) without time zone,
    version bigint NOT NULL,
    delegated_from_id uuid NOT NULL,
    delegated_to_id uuid NOT NULL,
    id uuid NOT NULL,
    status character varying(20) NOT NULL,
    acting_gl_approval_role character varying(30),
    transaction_type character varying(80) NOT NULL,
    CONSTRAINT identity_delegations_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'REVOKED'::character varying])::text[])))
);


--
-- Name: identity_mfa_recovery_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_mfa_recovery_codes (
    user_id uuid NOT NULL,
    code_hash character varying(100) NOT NULL
);


--
-- Name: identity_password_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_password_history (
    seq integer NOT NULL,
    user_id uuid NOT NULL,
    password_hash character varying(255) NOT NULL
);


--
-- Name: identity_role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_role_permissions (
    role_id uuid NOT NULL,
    permission character varying(80),
    CONSTRAINT identity_role_permissions_permission_check CHECK (((permission)::text = ANY ((ARRAY['LOAN_READ'::character varying, 'LOAN_WRITE'::character varying, 'LOAN_APPROVE'::character varying, 'LOAN_DISBURSE'::character varying, 'LOAN_DISBURSE_APPROVE'::character varying, 'LOAN_RESTRUCTURE'::character varying, 'LOAN_RESTRUCTURE_APPROVE'::character varying, 'LOAN_COLLECT'::character varying, 'LOAN_COLLECT_APPROVE'::character varying, 'LOAN_WRITE_OFF'::character varying, 'LOAN_READ_OWN'::character varying, 'ACCOUNT_READ'::character varying, 'ACCOUNT_WRITE'::character varying, 'ACCOUNT_TRANSFER'::character varying, 'ACCOUNT_READ_OWN'::character varying, 'ACCOUNT_TRANSFER_OWN'::character varying, 'CUSTOMER_READ'::character varying, 'CUSTOMER_WRITE'::character varying, 'CUSTOMER_PII_READ'::character varying, 'CUSTOMER_READ_OWN'::character varying, 'CUSTOMER_WRITE_OWN'::character varying, 'PROFILE_READ_OWN'::character varying, 'PASSWORD_CHANGE_OWN'::character varying, 'MFA_MANAGE_OWN'::character varying, 'AUDIT_READ_OWN'::character varying, 'GL_READ'::character varying, 'GL_POST'::character varying, 'GL_APPROVE'::character varying, 'PAYMENT_INITIATE'::character varying, 'PAYMENT_INITIATE_OWN'::character varying, 'EXCHANGE_RATE_READ'::character varying, 'EXCHANGE_RATE_WRITE'::character varying, 'AUDIT_READ'::character varying, 'REPORT_READ'::character varying, 'REPORT_GENERATE'::character varying, 'TRANSACTION_READ'::character varying, 'TRANSACTION_WRITE'::character varying, 'VELOCITY_LIMIT_READ'::character varying, 'VELOCITY_LIMIT_WRITE'::character varying, 'FEE_READ'::character varying, 'FEE_WRITE'::character varying, 'COMPENSATION_READ'::character varying, 'COMPENSATION_WRITE'::character varying, 'GL_SUSPENSE_READ'::character varying, 'GL_SUSPENSE_WRITE'::character varying, 'GL_REVALUATION_READ'::character varying, 'GL_REVALUATION_WRITE'::character varying, 'GL_FISCAL_PERIOD_READ'::character varying, 'GL_FISCAL_PERIOD_WRITE'::character varying, 'GL_SETUP_READ'::character varying, 'GL_SETUP_WRITE'::character varying, 'BANK_CONFIG_READ'::character varying, 'BANK_CONFIG_WRITE'::character varying, 'HOLIDAY_READ'::character varying, 'HOLIDAY_WRITE'::character varying, 'CUSTOMER_ACCOUNT_READ'::character varying, 'CUSTOMER_ACCOUNT_WRITE'::character varying, 'CUSTOMER_ACCOUNT_LIMIT_READ'::character varying, 'CUSTOMER_ACCOUNT_LIMIT_WRITE'::character varying, 'SERVICE_EXCHANGE_RATE_READ'::character varying, 'SERVICE_EXCHANGE_RATE_WRITE'::character varying, 'SERVICE_CUSTOMER_READ'::character varying, 'SERVICE_CUSTOMER_WRITE'::character varying, 'SERVICE_SETUP_READ'::character varying, 'SERVICE_SETUP_WRITE'::character varying, 'SERVICE_ACCOUNT_READ'::character varying, 'SERVICE_ACCOUNT_WRITE'::character varying, 'SERVICE_TRANSACTION_READ'::character varying, 'SERVICE_TRANSACTION_WRITE'::character varying, 'SERVICE_GL_READ'::character varying, 'SERVICE_GL_WRITE'::character varying, 'SERVICE_LOAN_READ'::character varying, 'SERVICE_LOAN_WRITE'::character varying, 'ADMIN_USERS_READ'::character varying, 'ADMIN_USERS_WRITE'::character varying, 'ADMIN_ROLES_READ'::character varying, 'ADMIN_ROLES_WRITE'::character varying, 'ADMIN_CONFIG_READ'::character varying, 'ADMIN_CONFIG_WRITE'::character varying, 'ADMIN_DOA_READ'::character varying, 'ADMIN_DOA_WRITE'::character varying, 'COMPLIANCE_SCREENING_RUN'::character varying, 'COMPLIANCE_SCREENING_READ'::character varying, 'COMPLIANCE_ALERT_READ'::character varying, 'COMPLIANCE_ALERT_TRIAGE'::character varying, 'OPERATOR_NOTE_READ'::character varying, 'OPERATOR_NOTE_WRITE'::character varying, 'STAFF_NOTIFICATION_READ'::character varying, 'STAFF_NOTIFICATION_WRITE'::character varying, 'RECONCILIATION_READ'::character varying, 'RECONCILIATION_WRITE'::character varying, 'FEE_CAMPAIGN_WRITE'::character varying])::text[])))
);


--
-- Name: identity_role_scoped_grants; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_role_scoped_grants (
    time_window_end time(0) without time zone,
    time_window_start time(0) without time zone,
    role_id uuid NOT NULL,
    branch_code character varying(20),
    department_code character varying(40),
    permission character varying(80) NOT NULL,
    CONSTRAINT identity_role_scoped_grants_permission_check CHECK (((permission)::text = ANY ((ARRAY['LOAN_READ'::character varying, 'LOAN_WRITE'::character varying, 'LOAN_APPROVE'::character varying, 'LOAN_DISBURSE'::character varying, 'LOAN_DISBURSE_APPROVE'::character varying, 'LOAN_RESTRUCTURE'::character varying, 'LOAN_RESTRUCTURE_APPROVE'::character varying, 'LOAN_COLLECT'::character varying, 'LOAN_COLLECT_APPROVE'::character varying, 'LOAN_WRITE_OFF'::character varying, 'LOAN_READ_OWN'::character varying, 'ACCOUNT_READ'::character varying, 'ACCOUNT_WRITE'::character varying, 'ACCOUNT_TRANSFER'::character varying, 'ACCOUNT_READ_OWN'::character varying, 'ACCOUNT_TRANSFER_OWN'::character varying, 'CUSTOMER_READ'::character varying, 'CUSTOMER_WRITE'::character varying, 'CUSTOMER_PII_READ'::character varying, 'CUSTOMER_READ_OWN'::character varying, 'CUSTOMER_WRITE_OWN'::character varying, 'PROFILE_READ_OWN'::character varying, 'PASSWORD_CHANGE_OWN'::character varying, 'MFA_MANAGE_OWN'::character varying, 'AUDIT_READ_OWN'::character varying, 'GL_READ'::character varying, 'GL_POST'::character varying, 'GL_APPROVE'::character varying, 'PAYMENT_INITIATE'::character varying, 'PAYMENT_INITIATE_OWN'::character varying, 'EXCHANGE_RATE_READ'::character varying, 'EXCHANGE_RATE_WRITE'::character varying, 'AUDIT_READ'::character varying, 'REPORT_READ'::character varying, 'REPORT_GENERATE'::character varying, 'TRANSACTION_READ'::character varying, 'TRANSACTION_WRITE'::character varying, 'VELOCITY_LIMIT_READ'::character varying, 'VELOCITY_LIMIT_WRITE'::character varying, 'FEE_READ'::character varying, 'FEE_WRITE'::character varying, 'COMPENSATION_READ'::character varying, 'COMPENSATION_WRITE'::character varying, 'GL_SUSPENSE_READ'::character varying, 'GL_SUSPENSE_WRITE'::character varying, 'GL_REVALUATION_READ'::character varying, 'GL_REVALUATION_WRITE'::character varying, 'GL_FISCAL_PERIOD_READ'::character varying, 'GL_FISCAL_PERIOD_WRITE'::character varying, 'GL_SETUP_READ'::character varying, 'GL_SETUP_WRITE'::character varying, 'BANK_CONFIG_READ'::character varying, 'BANK_CONFIG_WRITE'::character varying, 'HOLIDAY_READ'::character varying, 'HOLIDAY_WRITE'::character varying, 'CUSTOMER_ACCOUNT_READ'::character varying, 'CUSTOMER_ACCOUNT_WRITE'::character varying, 'CUSTOMER_ACCOUNT_LIMIT_READ'::character varying, 'CUSTOMER_ACCOUNT_LIMIT_WRITE'::character varying, 'SERVICE_EXCHANGE_RATE_READ'::character varying, 'SERVICE_EXCHANGE_RATE_WRITE'::character varying, 'SERVICE_CUSTOMER_READ'::character varying, 'SERVICE_CUSTOMER_WRITE'::character varying, 'SERVICE_SETUP_READ'::character varying, 'SERVICE_SETUP_WRITE'::character varying, 'SERVICE_ACCOUNT_READ'::character varying, 'SERVICE_ACCOUNT_WRITE'::character varying, 'SERVICE_TRANSACTION_READ'::character varying, 'SERVICE_TRANSACTION_WRITE'::character varying, 'SERVICE_GL_READ'::character varying, 'SERVICE_GL_WRITE'::character varying, 'SERVICE_LOAN_READ'::character varying, 'SERVICE_LOAN_WRITE'::character varying, 'ADMIN_USERS_READ'::character varying, 'ADMIN_USERS_WRITE'::character varying, 'ADMIN_ROLES_READ'::character varying, 'ADMIN_ROLES_WRITE'::character varying, 'ADMIN_CONFIG_READ'::character varying, 'ADMIN_CONFIG_WRITE'::character varying, 'ADMIN_DOA_READ'::character varying, 'ADMIN_DOA_WRITE'::character varying, 'COMPLIANCE_SCREENING_RUN'::character varying, 'COMPLIANCE_SCREENING_READ'::character varying, 'COMPLIANCE_ALERT_READ'::character varying, 'COMPLIANCE_ALERT_TRIAGE'::character varying, 'OPERATOR_NOTE_READ'::character varying, 'OPERATOR_NOTE_WRITE'::character varying, 'STAFF_NOTIFICATION_READ'::character varying, 'STAFF_NOTIFICATION_WRITE'::character varying, 'RECONCILIATION_READ'::character varying, 'RECONCILIATION_WRITE'::character varying, 'FEE_CAMPAIGN_WRITE'::character varying])::text[])))
);


--
-- Name: identity_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_roles (
    enabled boolean NOT NULL,
    system_role boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    parent_role_id uuid,
    name character varying(60) NOT NULL,
    display_name character varying(120),
    description character varying(500)
);


--
-- Name: identity_user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_user_roles (
    role_id uuid NOT NULL,
    user_id uuid NOT NULL
);


--
-- Name: identity_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identity_users (
    account_locked boolean NOT NULL,
    enabled boolean NOT NULL,
    failed_login_attempts integer NOT NULL,
    force_password_change boolean NOT NULL,
    mfa_enabled boolean NOT NULL,
    account_expires_at timestamp(6) without time zone,
    account_expiry_warning_notified_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    disabled_at timestamp(6) without time zone,
    failed_login_locked_until timestamp(6) without time zone,
    last_login_at timestamp(6) without time zone,
    locked_at timestamp(6) without time zone,
    password_changed_at timestamp(6) without time zone,
    password_expires_at timestamp(6) without time zone,
    suspended_at timestamp(6) without time zone,
    suspension_until timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    created_by uuid,
    customer_party_id uuid,
    id uuid NOT NULL,
    branch_code character varying(20),
    user_type character varying(20) NOT NULL,
    provisioning_status character varying(24) NOT NULL,
    gl_approval_role character varying(30),
    department_code character varying(40),
    employee_id character varying(40),
    last_login_ip character varying(45),
    username character varying(80) NOT NULL,
    email character varying(150),
    locked_reason character varying(500),
    suspension_reason character varying(500),
    mfa_secret character varying(512),
    provisioning_eligibility_notes character varying(2000),
    password_hash character varying(255) NOT NULL,
    CONSTRAINT identity_users_provisioning_status_check CHECK (((provisioning_status)::text = ANY ((ARRAY['PENDING_APPROVAL'::character varying, 'ACTIVE'::character varying, 'REJECTED'::character varying, 'DEPROVISIONED'::character varying])::text[]))),
    CONSTRAINT identity_users_user_type_check CHECK (((user_type)::text = ANY ((ARRAY['STAFF'::character varying, 'CUSTOMER'::character varying, 'SYSTEM'::character varying])::text[])))
);


--
-- Name: interest_accruals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.interest_accruals (
    accrual_amount numeric(19,4) NOT NULL,
    accrual_date date NOT NULL,
    currency character varying(3) NOT NULL,
    interest_rate numeric(5,4) NOT NULL,
    is_posted boolean NOT NULL,
    principal_balance numeric(19,4) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    posted_at timestamp(6) with time zone,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL
);


--
-- Name: interest_rates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.interest_rates (
    annual_percentage_rate numeric(10,4) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    effective_from timestamp(6) without time zone NOT NULL,
    effective_until timestamp(6) without time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    customer_account_id uuid NOT NULL,
    id uuid NOT NULL,
    rate_type character varying(20) NOT NULL,
    created_by character varying(100),
    updated_by character varying(100),
    CONSTRAINT interest_rates_rate_type_check CHECK (((rate_type)::text = ANY ((ARRAY['CREDIT'::character varying, 'DEBIT'::character varying])::text[])))
);


--
-- Name: kyc_review_steps; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kyc_review_steps (
    reviewed_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    kyc_workflow_id uuid NOT NULL,
    decision character varying(30) NOT NULL,
    reviewed_by character varying(100),
    step_name character varying(100) NOT NULL,
    comments text,
    CONSTRAINT kyc_review_steps_decision_check CHECK (((decision)::text = ANY ((ARRAY['APPROVED'::character varying, 'REJECTED'::character varying, 'REQUIRES_ADDITIONAL_INFO'::character varying])::text[])))
);


--
-- Name: kyc_workflows; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kyc_workflows (
    completed_at timestamp(6) without time zone,
    initiated_at timestamp(6) without time zone NOT NULL,
    reviewed_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    status character varying(20) NOT NULL,
    initiated_by character varying(100),
    reviewed_by character varying(100),
    comments text,
    re_verification_reason text,
    rejection_reason text,
    CONSTRAINT kyc_workflows_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_REVIEW'::character varying, 'VERIFIED'::character varying, 'REJECTED'::character varying, 'EXPIRED'::character varying])::text[])))
);


--
-- Name: loan_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_accounts (
    closed_date date,
    currency character varying(3) NOT NULL,
    days_past_due integer NOT NULL,
    disbursement_date date NOT NULL,
    first_payment_date date,
    interest_rate numeric(5,2) NOT NULL,
    is_restructured boolean NOT NULL,
    is_top_up boolean NOT NULL,
    last_payment_date date,
    maturity_date date NOT NULL,
    outstanding_fees numeric(19,4) NOT NULL,
    outstanding_interest numeric(19,4) NOT NULL,
    outstanding_penalties numeric(19,4) NOT NULL,
    outstanding_principal numeric(19,4) NOT NULL,
    principal_amount numeric(19,4) NOT NULL,
    restructured_date date,
    tenor_months integer NOT NULL,
    total_paid numeric(19,4),
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint,
    application_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    original_loan_id uuid,
    product_id uuid NOT NULL,
    amortization_type character varying(30) NOT NULL,
    delinquency_bucket character varying(30),
    interest_calculation_method character varying(30) NOT NULL,
    repayment_frequency character varying(30) NOT NULL,
    status character varying(30) NOT NULL,
    loan_account_number character varying(50) NOT NULL,
    remarks character varying(1000),
    CONSTRAINT loan_accounts_amortization_type_check CHECK (((amortization_type)::text = ANY ((ARRAY['EQUAL_INSTALLMENTS'::character varying, 'EQUAL_PRINCIPAL'::character varying, 'BALLOON_PAYMENT'::character varying, 'BULLET_PAYMENT'::character varying, 'CUSTOM'::character varying])::text[]))),
    CONSTRAINT loan_accounts_days_past_due_check CHECK ((days_past_due >= 0)),
    CONSTRAINT loan_accounts_delinquency_bucket_check CHECK (((delinquency_bucket)::text = ANY ((ARRAY['CURRENT'::character varying, 'DPD_1_30'::character varying, 'DPD_31_60'::character varying, 'DPD_61_90'::character varying, 'DPD_91_180'::character varying, 'DPD_180_PLUS'::character varying])::text[]))),
    CONSTRAINT loan_accounts_interest_calculation_method_check CHECK (((interest_calculation_method)::text = ANY ((ARRAY['FLAT_RATE'::character varying, 'REDUCING_BALANCE'::character varying, 'SIMPLE_INTEREST'::character varying, 'COMPOUND_INTEREST'::character varying, 'DAILY_REDUCING'::character varying, 'RULE_OF_78'::character varying])::text[]))),
    CONSTRAINT loan_accounts_repayment_frequency_check CHECK (((repayment_frequency)::text = ANY ((ARRAY['DAILY'::character varying, 'WEEKLY'::character varying, 'BIWEEKLY'::character varying, 'MONTHLY'::character varying, 'QUARTERLY'::character varying, 'SEMI_ANNUALLY'::character varying, 'ANNUALLY'::character varying, 'BULLET'::character varying])::text[]))),
    CONSTRAINT loan_accounts_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'CLOSED'::character varying, 'WRITTEN_OFF'::character varying, 'RESTRUCTURED'::character varying, 'SETTLED'::character varying])::text[]))),
    CONSTRAINT loan_accounts_tenor_months_check CHECK ((tenor_months >= 1))
);


--
-- Name: loan_applications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_applications (
    approval_date date,
    approved_amount numeric(19,4),
    approved_interest_rate numeric(5,2),
    approved_tenor_months integer,
    credit_score numeric(5,2),
    currency character varying(3) NOT NULL,
    existing_obligations numeric(19,4),
    guarantors_required integer,
    monthly_income numeric(19,4),
    rejection_date date,
    requested_amount numeric(19,4) NOT NULL,
    requested_tenor_months integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    underwriter_assigned_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    risk_rating character varying(20),
    status character varying(30) NOT NULL,
    application_number character varying(50) NOT NULL,
    approved_by character varying(100),
    purpose character varying(100),
    rejected_by character varying(100),
    underwriter_assigned_by character varying(100),
    underwriter_id character varying(100),
    rejection_reason character varying(500),
    remarks character varying(1000),
    CONSTRAINT loan_applications_approved_tenor_months_check CHECK ((approved_tenor_months >= 1)),
    CONSTRAINT loan_applications_credit_score_check CHECK (((credit_score <= (1000)::numeric) AND (credit_score >= (0)::numeric))),
    CONSTRAINT loan_applications_guarantors_required_check CHECK ((guarantors_required >= 0)),
    CONSTRAINT loan_applications_requested_tenor_months_check CHECK ((requested_tenor_months >= 1)),
    CONSTRAINT loan_applications_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'UNDER_REVIEW'::character varying, 'PENDING_DOCUMENTS'::character varying, 'UNDERWRITING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'WITHDRAWN'::character varying, 'EXPIRED'::character varying])::text[])))
);


--
-- Name: loan_disbursements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_disbursements (
    currency character varying(3) NOT NULL,
    disbursement_amount numeric(19,4) NOT NULL,
    disbursement_date date NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    disbursement_method character varying(30) NOT NULL,
    disbursement_reference character varying(50) NOT NULL,
    beneficiary_account_number character varying(100),
    transaction_reference character varying(100),
    beneficiary_name character varying(200),
    remarks character varying(500),
    CONSTRAINT loan_disbursements_disbursement_method_check CHECK (((disbursement_method)::text = ANY ((ARRAY['BANK_TRANSFER'::character varying, 'CHEQUE'::character varying, 'CASH'::character varying, 'DIRECT_TO_VENDOR'::character varying, 'MOBILE_MONEY'::character varying])::text[]))),
    CONSTRAINT loan_disbursements_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'REVERSED'::character varying])::text[])))
);


--
-- Name: loan_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_documents (
    expiry_date date,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    document_type character varying(30) NOT NULL,
    document_number character varying(100),
    document_name character varying(200) NOT NULL,
    document_path character varying(500) NOT NULL,
    remarks character varying(500),
    CONSTRAINT loan_documents_document_type_check CHECK (((document_type)::text = ANY ((ARRAY['LOAN_AGREEMENT'::character varying, 'PROMISSORY_NOTE'::character varying, 'COLLATERAL_DOCUMENT'::character varying, 'INSURANCE_POLICY'::character varying, 'GUARANTOR_AGREEMENT'::character varying, 'INCOME_PROOF'::character varying, 'IDENTITY_PROOF'::character varying, 'ADDRESS_PROOF'::character varying, 'VALUATION_REPORT'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT loan_documents_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'EXPIRED'::character varying, 'ARCHIVED'::character varying, 'SUPERSEDED'::character varying])::text[])))
);


--
-- Name: loan_fees; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_fees (
    charge_date date NOT NULL,
    currency character varying(3) NOT NULL,
    fee_amount numeric(19,4) NOT NULL,
    is_waived boolean NOT NULL,
    outstanding_amount numeric(19,4) NOT NULL,
    waived_date date,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    fee_type character varying(30) NOT NULL,
    description character varying(500),
    waiver_reason character varying(500),
    CONSTRAINT loan_fees_fee_type_check CHECK (((fee_type)::text = ANY ((ARRAY['PROCESSING_FEE'::character varying, 'LATE_PAYMENT_FEE'::character varying, 'PREPAYMENT_PENALTY'::character varying, 'RESTRUCTURING_FEE'::character varying, 'LEGAL_FEE'::character varying, 'VALUATION_FEE'::character varying, 'INSURANCE_FEE'::character varying, 'DOCUMENTATION_FEE'::character varying, 'OTHER'::character varying])::text[])))
);


--
-- Name: loan_payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_payments (
    currency character varying(3) NOT NULL,
    fees_paid numeric(19,4) NOT NULL,
    interest_paid numeric(19,4) NOT NULL,
    is_reversed boolean NOT NULL,
    payment_amount numeric(19,4) NOT NULL,
    payment_date date NOT NULL,
    penalties_paid numeric(19,4) NOT NULL,
    principal_paid numeric(19,4) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    reversed_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    payment_method character varying(30) NOT NULL,
    payment_type character varying(30) NOT NULL,
    payment_reference character varying(50) NOT NULL,
    transaction_reference character varying(100),
    remarks character varying(500),
    reversal_reason character varying(500),
    CONSTRAINT loan_payments_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'BANK_TRANSFER'::character varying, 'CHEQUE'::character varying, 'DIRECT_DEBIT'::character varying, 'CARD'::character varying, 'MOBILE_MONEY'::character varying, 'ONLINE'::character varying])::text[]))),
    CONSTRAINT loan_payments_payment_type_check CHECK (((payment_type)::text = ANY ((ARRAY['REGULAR_PAYMENT'::character varying, 'PREPAYMENT'::character varying, 'EARLY_SETTLEMENT'::character varying, 'LATE_FEE'::character varying, 'PENALTY'::character varying, 'RESTRUCTURING_FEE'::character varying, 'REVERSAL'::character varying])::text[])))
);


--
-- Name: loan_products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_products (
    active boolean NOT NULL,
    collateral_required boolean NOT NULL,
    currency character varying(3) NOT NULL,
    grace_period_days integer NOT NULL,
    guarantor_required boolean NOT NULL,
    interest_rate numeric(5,2) NOT NULL,
    late_fee_fixed numeric(19,4),
    late_fee_percentage numeric(5,2),
    max_amount numeric(19,4) NOT NULL,
    max_tenor_months integer NOT NULL,
    min_amount numeric(19,4) NOT NULL,
    min_tenor_months integer NOT NULL,
    prepayment_penalty_percentage numeric(5,2),
    processing_fee_fixed numeric(19,4),
    processing_fee_percentage numeric(5,2),
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    amortization_type character varying(30) NOT NULL,
    interest_calculation_method character varying(30) NOT NULL,
    product_type character varying(30) NOT NULL,
    repayment_frequency character varying(30) NOT NULL,
    product_code character varying(50) NOT NULL,
    product_name character varying(200) NOT NULL,
    description character varying(1000),
    CONSTRAINT loan_products_amortization_type_check CHECK (((amortization_type)::text = ANY ((ARRAY['EQUAL_INSTALLMENTS'::character varying, 'EQUAL_PRINCIPAL'::character varying, 'BALLOON_PAYMENT'::character varying, 'BULLET_PAYMENT'::character varying, 'CUSTOM'::character varying])::text[]))),
    CONSTRAINT loan_products_grace_period_days_check CHECK ((grace_period_days >= 0)),
    CONSTRAINT loan_products_interest_calculation_method_check CHECK (((interest_calculation_method)::text = ANY ((ARRAY['FLAT_RATE'::character varying, 'REDUCING_BALANCE'::character varying, 'SIMPLE_INTEREST'::character varying, 'COMPOUND_INTEREST'::character varying, 'DAILY_REDUCING'::character varying, 'RULE_OF_78'::character varying])::text[]))),
    CONSTRAINT loan_products_max_tenor_months_check CHECK ((max_tenor_months >= 1)),
    CONSTRAINT loan_products_min_tenor_months_check CHECK ((min_tenor_months >= 1)),
    CONSTRAINT loan_products_product_type_check CHECK (((product_type)::text = ANY ((ARRAY['PERSONAL_LOAN'::character varying, 'HOME_LOAN'::character varying, 'AUTO_LOAN'::character varying, 'BUSINESS_LOAN'::character varying, 'EDUCATION_LOAN'::character varying, 'GOLD_LOAN'::character varying, 'OVERDRAFT'::character varying, 'CREDIT_LINE'::character varying])::text[]))),
    CONSTRAINT loan_products_repayment_frequency_check CHECK (((repayment_frequency)::text = ANY ((ARRAY['DAILY'::character varying, 'WEEKLY'::character varying, 'BIWEEKLY'::character varying, 'MONTHLY'::character varying, 'QUARTERLY'::character varying, 'SEMI_ANNUALLY'::character varying, 'ANNUALLY'::character varying, 'BULLET'::character varying])::text[])))
);


--
-- Name: loan_provisions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_provisions (
    currency character varying(3) NOT NULL,
    is_posted boolean NOT NULL,
    outstanding_balance numeric(19,4) NOT NULL,
    provision_amount numeric(19,4) NOT NULL,
    provision_date date NOT NULL,
    provision_rate numeric(5,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    posted_at timestamp(6) with time zone,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    provision_stage character varying(30) NOT NULL,
    CONSTRAINT loan_provisions_provision_stage_check CHECK (((provision_stage)::text = ANY ((ARRAY['STAGE_1_PERFORMING'::character varying, 'STAGE_2_UNDERPERFORMING'::character varying, 'STAGE_3_NON_PERFORMING'::character varying, 'SPECIFIC_PROVISION'::character varying])::text[])))
);


--
-- Name: loan_restructurings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_restructurings (
    new_interest_rate numeric(5,2),
    new_principal_balance numeric(19,4),
    new_tenor_months integer,
    old_interest_rate numeric(5,2),
    old_principal_balance numeric(19,4),
    old_tenor_months integer,
    restructuring_date date NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    restructuring_status character varying(20) NOT NULL,
    restructuring_type character varying(30) NOT NULL,
    approved_by character varying(100),
    reason character varying(1000),
    CONSTRAINT loan_restructurings_new_tenor_months_check CHECK ((new_tenor_months >= 1)),
    CONSTRAINT loan_restructurings_old_tenor_months_check CHECK ((old_tenor_months >= 1)),
    CONSTRAINT loan_restructurings_restructuring_status_check CHECK (((restructuring_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'PROCESSED'::character varying])::text[]))),
    CONSTRAINT loan_restructurings_restructuring_type_check CHECK (((restructuring_type)::text = ANY ((ARRAY['TERM_EXTENSION'::character varying, 'RATE_REDUCTION'::character varying, 'PAYMENT_HOLIDAY'::character varying, 'PRINCIPAL_MORATORIUM'::character varying, 'FULL_RESTRUCTURE'::character varying])::text[])))
);


--
-- Name: loan_schedules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_schedules (
    days_past_due integer NOT NULL,
    due_date date NOT NULL,
    fees_paid numeric(19,4) NOT NULL,
    installment_number integer NOT NULL,
    interest_due numeric(19,4) NOT NULL,
    interest_paid numeric(19,4) NOT NULL,
    is_overdue boolean NOT NULL,
    outstanding_balance numeric(19,4) NOT NULL,
    paid_date date,
    penalties_paid numeric(19,4) NOT NULL,
    principal_due numeric(19,4) NOT NULL,
    principal_paid numeric(19,4) NOT NULL,
    total_due numeric(19,4) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    CONSTRAINT loan_schedules_days_past_due_check CHECK ((days_past_due >= 0)),
    CONSTRAINT loan_schedules_installment_number_check CHECK ((installment_number >= 1)),
    CONSTRAINT loan_schedules_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PARTIALLY_PAID'::character varying, 'PAID'::character varying, 'OVERDUE'::character varying, 'WAIVED'::character varying])::text[])))
);


--
-- Name: loan_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_transactions (
    amount numeric(19,4) NOT NULL,
    currency character varying(3) NOT NULL,
    is_reversed boolean NOT NULL,
    transaction_date date NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    reversed_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    loan_account_id uuid NOT NULL,
    transaction_type character varying(30) NOT NULL,
    transaction_reference character varying(50) NOT NULL,
    external_reference character varying(100),
    description character varying(500),
    reversal_reason character varying(500),
    CONSTRAINT loan_transactions_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['DISBURSEMENT'::character varying, 'REPAYMENT'::character varying, 'FEE_CHARGE'::character varying, 'PENALTY_CHARGE'::character varying, 'INTEREST_ACCRUAL'::character varying, 'INTEREST_WAIVER'::character varying, 'FEE_WAIVER'::character varying, 'PENALTY_WAIVER'::character varying, 'WRITE_OFF'::character varying, 'RECOVERY'::character varying, 'REVERSAL'::character varying])::text[])))
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    is_read boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    message character varying(2000) NOT NULL,
    channel character varying(255) NOT NULL,
    recipient_id character varying(255) NOT NULL,
    severity character varying(255) NOT NULL,
    subject character varying(255) NOT NULL,
    CONSTRAINT notifications_channel_check CHECK (((channel)::text = ANY ((ARRAY['INBOX_ONLY'::character varying, 'EMAIL'::character varying, 'SMS'::character varying])::text[]))),
    CONSTRAINT notifications_severity_check CHECK (((severity)::text = ANY ((ARRAY['INFO'::character varying, 'WARNING'::character varying, 'CRITICAL'::character varying])::text[])))
);


--
-- Name: oauth2_authorization; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oauth2_authorization (
    access_token_expires_at timestamp(6) with time zone,
    access_token_issued_at timestamp(6) with time zone,
    authorization_code_expires_at timestamp(6) with time zone,
    authorization_code_issued_at timestamp(6) with time zone,
    device_code_expires_at timestamp(6) with time zone,
    device_code_issued_at timestamp(6) with time zone,
    oidc_id_token_expires_at timestamp(6) with time zone,
    oidc_id_token_issued_at timestamp(6) with time zone,
    refresh_token_expires_at timestamp(6) with time zone,
    refresh_token_issued_at timestamp(6) with time zone,
    user_code_expires_at timestamp(6) with time zone,
    user_code_issued_at timestamp(6) with time zone,
    access_token_type character varying(100),
    authorization_grant_type character varying(100) NOT NULL,
    id character varying(100) NOT NULL,
    registered_client_id character varying(100) NOT NULL,
    principal_name character varying(200) NOT NULL,
    state character varying(500),
    access_token_scopes character varying(1000),
    authorized_scopes character varying(1000),
    access_token_metadata text,
    access_token_value text,
    attributes text,
    authorization_code_metadata text,
    authorization_code_value text,
    device_code_metadata text,
    device_code_value text,
    oidc_id_token_metadata text,
    oidc_id_token_value text,
    refresh_token_metadata text,
    refresh_token_value text,
    user_code_metadata text,
    user_code_value text
);


--
-- Name: operational_gl_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.operational_gl_config (
    is_active boolean NOT NULL,
    priority integer,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone,
    gl_account_id uuid NOT NULL,
    id uuid NOT NULL,
    config_type character varying(50) NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_by character varying(100),
    description character varying(500),
    CONSTRAINT operational_gl_config_config_type_check CHECK (((config_type)::text = ANY ((ARRAY['FEE_INCOME'::character varying, 'CASH_VAULT'::character varying, 'SUSPENSE'::character varying, 'EXTERNAL_CLEARING'::character varying, 'INTEREST_EXPENSE'::character varying, 'INTEREST_INCOME'::character varying, 'LOAN_INTEREST_RECEIVABLE'::character varying, 'FX_GAIN'::character varying, 'FX_LOSS'::character varying, 'ATM_CASH'::character varying, 'CARD_PROCESSING_FEES'::character varying, 'OVERDRAFT_INTEREST_INCOME'::character varying, 'RETAINED_EARNINGS'::character varying, 'UNREALIZED_FX_GL'::character varying])::text[])))
);


--
-- Name: tp_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tp_transactions (
    currency character varying(3) NOT NULL,
    estimated_fee_amount numeric(19,4),
    fee_amount numeric(19,4),
    principal_amount numeric(19,4),
    reservation_timeout integer,
    transaction_date date NOT NULL,
    value_date date NOT NULL,
    completed_at timestamp(6) without time zone,
    created_at timestamp(6) with time zone NOT NULL,
    failed_at timestamp(6) without time zone,
    fee_calculation_at timestamp(6) without time zone,
    processing_started_at timestamp(6) without time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint NOT NULL,
    applied_fee_rule_id uuid,
    destination_account_id uuid,
    gl_transaction_id uuid,
    id uuid NOT NULL,
    request_id uuid NOT NULL,
    source_account_id uuid,
    gl_reference_number character varying(50),
    status character varying(50) NOT NULL,
    external_reference character varying(255),
    failure_reason text,
    fee_calculation_details text,
    gateway_transaction_id character varying(255),
    CONSTRAINT tp_transactions_status_check CHECK (((status)::text = ANY ((ARRAY['INITIATED'::character varying, 'PENDING_RESERVATION'::character varying, 'AUTHORIZED'::character varying, 'POSTED'::character varying, 'REVERSED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: transaction_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transaction_events (
    event_sequence integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    transaction_id uuid NOT NULL,
    error_code character varying(50),
    new_status character varying(50) NOT NULL,
    previous_status character varying(50),
    created_by character varying(100),
    event_type character varying(100) NOT NULL,
    error_message text,
    event_data jsonb,
    CONSTRAINT transaction_events_new_status_check CHECK (((new_status)::text = ANY ((ARRAY['INITIATED'::character varying, 'PENDING_RESERVATION'::character varying, 'AUTHORIZED'::character varying, 'POSTED'::character varying, 'REVERSED'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT transaction_events_previous_status_check CHECK (((previous_status)::text = ANY ((ARRAY['INITIATED'::character varying, 'PENDING_RESERVATION'::character varying, 'AUTHORIZED'::character varying, 'POSTED'::character varying, 'REVERSED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: transaction_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transaction_requests (
    amount numeric(19,4) NOT NULL,
    currency character varying(3) NOT NULL,
    requested_reservation_timeout integer,
    requested_transaction_date date,
    requested_value_date date,
    created_at timestamp(6) with time zone NOT NULL,
    version bigint,
    destination_account_id uuid,
    id uuid NOT NULL,
    source_account_id uuid,
    ip_address character varying(45),
    transaction_type character varying(50) NOT NULL,
    created_by character varying(100) NOT NULL,
    client_reference character varying(255),
    description text,
    idempotency_key character varying(255) NOT NULL,
    metadata jsonb,
    user_agent text,
    CONSTRAINT transaction_requests_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['P2P'::character varying, 'CASH_IN'::character varying, 'CASH_OUT'::character varying, 'BILL_PAYMENT'::character varying, 'MERCHANT_PURCHASE'::character varying, 'TRANSFER'::character varying, 'DEPOSIT'::character varying, 'REFUND'::character varying])::text[])))
);


--
-- Name: velocity_limit_breaches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.velocity_limit_breaches (
    attempted_amount numeric(19,4),
    attempted_count integer,
    limit_amount numeric(19,4),
    limit_count integer,
    breach_timestamp timestamp(6) with time zone NOT NULL,
    account_id uuid NOT NULL,
    id uuid NOT NULL,
    breach_type character varying(20),
    reason character varying(500),
    limit_period character varying(255) NOT NULL,
    transaction_type character varying(255) NOT NULL,
    CONSTRAINT velocity_limit_breaches_limit_period_check CHECK (((limit_period)::text = ANY ((ARRAY['DAILY'::character varying, 'WEEKLY'::character varying, 'MONTHLY'::character varying])::text[]))),
    CONSTRAINT velocity_limit_breaches_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['P2P'::character varying, 'CASH_IN'::character varying, 'CASH_OUT'::character varying, 'BILL_PAYMENT'::character varying, 'MERCHANT_PURCHASE'::character varying, 'TRANSFER'::character varying, 'DEPOSIT'::character varying, 'REFUND'::character varying])::text[])))
);


--
-- Name: velocity_limits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.velocity_limits (
    currency character varying(3),
    current_amount numeric(19,4) NOT NULL,
    current_count integer NOT NULL,
    is_active boolean NOT NULL,
    max_amount numeric(19,4),
    max_count integer,
    created_at timestamp(6) with time zone NOT NULL,
    last_reset_at timestamp(6) without time zone,
    period_end timestamp(6) without time zone NOT NULL,
    period_start timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint,
    account_id uuid NOT NULL,
    id uuid NOT NULL,
    customer_tier character varying(255) NOT NULL,
    limit_period character varying(255) NOT NULL,
    transaction_type character varying(255) NOT NULL,
    CONSTRAINT velocity_limits_customer_tier_check CHECK (((customer_tier)::text = ANY ((ARRAY['BASIC'::character varying, 'PREMIUM'::character varying, 'VIP'::character varying, 'ENTERPRISE'::character varying])::text[]))),
    CONSTRAINT velocity_limits_limit_period_check CHECK (((limit_period)::text = ANY ((ARRAY['DAILY'::character varying, 'WEEKLY'::character varying, 'MONTHLY'::character varying])::text[]))),
    CONSTRAINT velocity_limits_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['P2P'::character varying, 'CASH_IN'::character varying, 'CASH_OUT'::character varying, 'BILL_PAYMENT'::character varying, 'MERCHANT_PURCHASE'::character varying, 'TRANSFER'::character varying, 'DEPOSIT'::character varying, 'REFUND'::character varying])::text[])))
);


--
-- Name: account_holds account_holds_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_holds
    ADD CONSTRAINT account_holds_pkey PRIMARY KEY (id);


--
-- Name: account_limits account_limits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_limits
    ADD CONSTRAINT account_limits_pkey PRIMARY KEY (id);


--
-- Name: account_relationships account_relationships_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_relationships
    ADD CONSTRAINT account_relationships_pkey PRIMARY KEY (id);


--
-- Name: account_transactions account_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_transactions
    ADD CONSTRAINT account_transactions_pkey PRIMARY KEY (id);


--
-- Name: accounts accounts_account_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_account_number_key UNIQUE (account_number);


--
-- Name: accounts accounts_iban_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_iban_key UNIQUE (iban);


--
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);


--
-- Name: aml_alerts aml_alerts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aml_alerts
    ADD CONSTRAINT aml_alerts_pkey PRIMARY KEY (id);


--
-- Name: aml_monitoring_rules aml_monitoring_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aml_monitoring_rules
    ADD CONSTRAINT aml_monitoring_rules_pkey PRIMARY KEY (id);


--
-- Name: balance_reservations balance_reservations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.balance_reservations
    ADD CONSTRAINT balance_reservations_pkey PRIMARY KEY (id);


--
-- Name: balance_reservations balance_reservations_reservation_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.balance_reservations
    ADD CONSTRAINT balance_reservations_reservation_key_key UNIQUE (reservation_key);


--
-- Name: banking_preferences banking_preferences_customer_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.banking_preferences
    ADD CONSTRAINT banking_preferences_customer_id_key UNIQUE (customer_id);


--
-- Name: banking_preferences banking_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.banking_preferences
    ADD CONSTRAINT banking_preferences_pkey PRIMARY KEY (id);


--
-- Name: collaterals collaterals_collateral_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collaterals
    ADD CONSTRAINT collaterals_collateral_reference_key UNIQUE (collateral_reference);


--
-- Name: collaterals collaterals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collaterals
    ADD CONSTRAINT collaterals_pkey PRIMARY KEY (id);


--
-- Name: collection_activities collection_activities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_activities
    ADD CONSTRAINT collection_activities_pkey PRIMARY KEY (id);


--
-- Name: compensation_workflows compensation_workflows_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compensation_workflows
    ADD CONSTRAINT compensation_workflows_pkey PRIMARY KEY (id);


--
-- Name: compliance_operator_notes compliance_operator_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliance_operator_notes
    ADD CONSTRAINT compliance_operator_notes_pkey PRIMARY KEY (id);


--
-- Name: contact_details contact_details_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contact_details
    ADD CONSTRAINT contact_details_pkey PRIMARY KEY (id);


--
-- Name: customer_addresses customer_addresses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_addresses
    ADD CONSTRAINT customer_addresses_pkey PRIMARY KEY (id);


--
-- Name: customer_audit_logs customer_audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_audit_logs
    ADD CONSTRAINT customer_audit_logs_pkey PRIMARY KEY (id);


--
-- Name: customer_consents customer_consents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_consents
    ADD CONSTRAINT customer_consents_pkey PRIMARY KEY (id);


--
-- Name: customer_data_retention customer_data_retention_customer_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_data_retention
    ADD CONSTRAINT customer_data_retention_customer_id_key UNIQUE (customer_id);


--
-- Name: customer_data_retention customer_data_retention_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_data_retention
    ADD CONSTRAINT customer_data_retention_pkey PRIMARY KEY (id);


--
-- Name: customer_onboardings customer_onboardings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_onboardings
    ADD CONSTRAINT customer_onboardings_pkey PRIMARY KEY (id);


--
-- Name: customer_outbox customer_outbox_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_outbox
    ADD CONSTRAINT customer_outbox_pkey PRIMARY KEY (id);


--
-- Name: customer_relationships customer_relationships_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_relationships
    ADD CONSTRAINT customer_relationships_pkey PRIMARY KEY (id);


--
-- Name: customer_risk_profiles customer_risk_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_risk_profiles
    ADD CONSTRAINT customer_risk_profiles_pkey PRIMARY KEY (customer_id);


--
-- Name: customers customers_customer_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_customer_number_key UNIQUE (customer_number);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: customers customers_tax_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_tax_id_key UNIQUE (tax_id);


--
-- Name: data_subject_requests data_subject_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_subject_requests
    ADD CONSTRAINT data_subject_requests_pkey PRIMARY KEY (id);


--
-- Name: data_subject_requests data_subject_requests_reference_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_subject_requests
    ADD CONSTRAINT data_subject_requests_reference_number_key UNIQUE (reference_number);


--
-- Name: early_settlements early_settlements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.early_settlements
    ADD CONSTRAINT early_settlements_pkey PRIMARY KEY (id);


--
-- Name: early_settlements early_settlements_quote_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.early_settlements
    ADD CONSTRAINT early_settlements_quote_reference_key UNIQUE (quote_reference);


--
-- Name: exchange_rates exchange_rates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchange_rates
    ADD CONSTRAINT exchange_rates_pkey PRIMARY KEY (id);


--
-- Name: fee_rules fee_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_rules
    ADD CONSTRAINT fee_rules_pkey PRIMARY KEY (id);


--
-- Name: fee_waivers fee_waivers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_waivers
    ADD CONSTRAINT fee_waivers_pkey PRIMARY KEY (id);


--
-- Name: fiscal_periods fiscal_periods_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiscal_periods
    ADD CONSTRAINT fiscal_periods_name_key UNIQUE (name);


--
-- Name: fiscal_periods fiscal_periods_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiscal_periods
    ADD CONSTRAINT fiscal_periods_pkey PRIMARY KEY (id);


--
-- Name: fx_spreads fx_spreads_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fx_spreads
    ADD CONSTRAINT fx_spreads_pkey PRIMARY KEY (id);


--
-- Name: gl_account_mappings gl_account_mappings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_account_mappings
    ADD CONSTRAINT gl_account_mappings_pkey PRIMARY KEY (id);


--
-- Name: gl_accounts gl_accounts_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_accounts
    ADD CONSTRAINT gl_accounts_code_key UNIQUE (code);


--
-- Name: gl_accounts gl_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_accounts
    ADD CONSTRAINT gl_accounts_pkey PRIMARY KEY (id);


--
-- Name: gl_audit_trail gl_audit_trail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_audit_trail
    ADD CONSTRAINT gl_audit_trail_pkey PRIMARY KEY (id);


--
-- Name: gl_authorization_limits gl_authorization_limits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_authorization_limits
    ADD CONSTRAINT gl_authorization_limits_pkey PRIMARY KEY (id);


--
-- Name: gl_daily_balances gl_daily_balances_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_daily_balances
    ADD CONSTRAINT gl_daily_balances_pkey PRIMARY KEY (id);


--
-- Name: gl_journal_entries gl_journal_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_journal_entries
    ADD CONSTRAINT gl_journal_entries_pkey PRIMARY KEY (id);


--
-- Name: gl_reconciliations gl_reconciliations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_reconciliations
    ADD CONSTRAINT gl_reconciliations_pkey PRIMARY KEY (id);


--
-- Name: gl_reconciliations gl_reconciliations_transaction_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_reconciliations
    ADD CONSTRAINT gl_reconciliations_transaction_id_key UNIQUE (transaction_id);


--
-- Name: gl_revaluation_details gl_revaluation_details_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_revaluation_details
    ADD CONSTRAINT gl_revaluation_details_pkey PRIMARY KEY (id);


--
-- Name: gl_revaluation_runs gl_revaluation_runs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_revaluation_runs
    ADD CONSTRAINT gl_revaluation_runs_pkey PRIMARY KEY (id);


--
-- Name: gl_suspense_clearing_rules gl_suspense_clearing_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_suspense_clearing_rules
    ADD CONSTRAINT gl_suspense_clearing_rules_pkey PRIMARY KEY (id);


--
-- Name: gl_suspense_escalations gl_suspense_escalations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_suspense_escalations
    ADD CONSTRAINT gl_suspense_escalations_pkey PRIMARY KEY (id);


--
-- Name: gl_suspense_items gl_suspense_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_suspense_items
    ADD CONSTRAINT gl_suspense_items_pkey PRIMARY KEY (id);


--
-- Name: gl_transaction_approvals gl_transaction_approvals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_transaction_approvals
    ADD CONSTRAINT gl_transaction_approvals_pkey PRIMARY KEY (id);


--
-- Name: gl_transaction_sequences gl_transaction_sequences_fiscal_period_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_transaction_sequences
    ADD CONSTRAINT gl_transaction_sequences_fiscal_period_id_key UNIQUE (fiscal_period_id);


--
-- Name: gl_transaction_sequences gl_transaction_sequences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_transaction_sequences
    ADD CONSTRAINT gl_transaction_sequences_pkey PRIMARY KEY (id);


--
-- Name: gl_transactions gl_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_transactions
    ADD CONSTRAINT gl_transactions_pkey PRIMARY KEY (id);


--
-- Name: gl_transactions gl_transactions_reference_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_transactions
    ADD CONSTRAINT gl_transactions_reference_id_key UNIQUE (reference_id);


--
-- Name: guarantors guarantors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guarantors
    ADD CONSTRAINT guarantors_pkey PRIMARY KEY (id);


--
-- Name: holidays holidays_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.holidays
    ADD CONSTRAINT holidays_pkey PRIMARY KEY (id);


--
-- Name: identification_documents identification_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identification_documents
    ADD CONSTRAINT identification_documents_pkey PRIMARY KEY (id);


--
-- Name: identity_approval_workflow_steps identity_approval_workflow_steps_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_approval_workflow_steps
    ADD CONSTRAINT identity_approval_workflow_steps_pkey PRIMARY KEY (id);


--
-- Name: identity_approval_workflows identity_approval_workflows_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_approval_workflows
    ADD CONSTRAINT identity_approval_workflows_pkey PRIMARY KEY (id);


--
-- Name: identity_audit_events identity_audit_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_audit_events
    ADD CONSTRAINT identity_audit_events_pkey PRIMARY KEY (id);


--
-- Name: identity_delegations identity_delegations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_delegations
    ADD CONSTRAINT identity_delegations_pkey PRIMARY KEY (id);


--
-- Name: identity_mfa_recovery_codes identity_mfa_recovery_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_mfa_recovery_codes
    ADD CONSTRAINT identity_mfa_recovery_codes_pkey PRIMARY KEY (user_id, code_hash);


--
-- Name: identity_password_history identity_password_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_password_history
    ADD CONSTRAINT identity_password_history_pkey PRIMARY KEY (seq, user_id);


--
-- Name: identity_roles identity_roles_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_roles
    ADD CONSTRAINT identity_roles_name_key UNIQUE (name);


--
-- Name: identity_roles identity_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_roles
    ADD CONSTRAINT identity_roles_pkey PRIMARY KEY (id);


--
-- Name: identity_user_roles identity_user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_user_roles
    ADD CONSTRAINT identity_user_roles_pkey PRIMARY KEY (role_id, user_id);


--
-- Name: identity_users identity_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_users
    ADD CONSTRAINT identity_users_pkey PRIMARY KEY (id);


--
-- Name: identity_users identity_users_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_users
    ADD CONSTRAINT identity_users_username_key UNIQUE (username);


--
-- Name: interest_accruals interest_accruals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interest_accruals
    ADD CONSTRAINT interest_accruals_pkey PRIMARY KEY (id);


--
-- Name: interest_rates interest_rates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interest_rates
    ADD CONSTRAINT interest_rates_pkey PRIMARY KEY (id);


--
-- Name: kyc_review_steps kyc_review_steps_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kyc_review_steps
    ADD CONSTRAINT kyc_review_steps_pkey PRIMARY KEY (id);


--
-- Name: kyc_workflows kyc_workflows_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kyc_workflows
    ADD CONSTRAINT kyc_workflows_pkey PRIMARY KEY (id);


--
-- Name: loan_accounts loan_accounts_loan_account_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_accounts
    ADD CONSTRAINT loan_accounts_loan_account_number_key UNIQUE (loan_account_number);


--
-- Name: loan_accounts loan_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_accounts
    ADD CONSTRAINT loan_accounts_pkey PRIMARY KEY (id);


--
-- Name: loan_applications loan_applications_application_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_applications
    ADD CONSTRAINT loan_applications_application_number_key UNIQUE (application_number);


--
-- Name: loan_applications loan_applications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_applications
    ADD CONSTRAINT loan_applications_pkey PRIMARY KEY (id);


--
-- Name: loan_disbursements loan_disbursements_disbursement_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_disbursements
    ADD CONSTRAINT loan_disbursements_disbursement_reference_key UNIQUE (disbursement_reference);


--
-- Name: loan_disbursements loan_disbursements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_disbursements
    ADD CONSTRAINT loan_disbursements_pkey PRIMARY KEY (id);


--
-- Name: loan_documents loan_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_documents
    ADD CONSTRAINT loan_documents_pkey PRIMARY KEY (id);


--
-- Name: loan_fees loan_fees_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_fees
    ADD CONSTRAINT loan_fees_pkey PRIMARY KEY (id);


--
-- Name: loan_payments loan_payments_payment_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_payments
    ADD CONSTRAINT loan_payments_payment_reference_key UNIQUE (payment_reference);


--
-- Name: loan_payments loan_payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_payments
    ADD CONSTRAINT loan_payments_pkey PRIMARY KEY (id);


--
-- Name: loan_products loan_products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_products
    ADD CONSTRAINT loan_products_pkey PRIMARY KEY (id);


--
-- Name: loan_products loan_products_product_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_products
    ADD CONSTRAINT loan_products_product_code_key UNIQUE (product_code);


--
-- Name: loan_provisions loan_provisions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_provisions
    ADD CONSTRAINT loan_provisions_pkey PRIMARY KEY (id);


--
-- Name: loan_restructurings loan_restructurings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_restructurings
    ADD CONSTRAINT loan_restructurings_pkey PRIMARY KEY (id);


--
-- Name: loan_schedules loan_schedules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_schedules
    ADD CONSTRAINT loan_schedules_pkey PRIMARY KEY (id);


--
-- Name: loan_transactions loan_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_transactions
    ADD CONSTRAINT loan_transactions_pkey PRIMARY KEY (id);


--
-- Name: loan_transactions loan_transactions_transaction_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_transactions
    ADD CONSTRAINT loan_transactions_transaction_reference_key UNIQUE (transaction_reference);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: oauth2_authorization oauth2_authorization_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oauth2_authorization
    ADD CONSTRAINT oauth2_authorization_pkey PRIMARY KEY (id);


--
-- Name: operational_gl_config operational_gl_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.operational_gl_config
    ADD CONSTRAINT operational_gl_config_pkey PRIMARY KEY (id);


--
-- Name: tp_transactions tp_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tp_transactions
    ADD CONSTRAINT tp_transactions_pkey PRIMARY KEY (id);


--
-- Name: tp_transactions tp_transactions_request_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tp_transactions
    ADD CONSTRAINT tp_transactions_request_id_key UNIQUE (request_id);


--
-- Name: transaction_events transaction_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_events
    ADD CONSTRAINT transaction_events_pkey PRIMARY KEY (id);


--
-- Name: transaction_events transaction_events_transaction_id_event_sequence_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_events
    ADD CONSTRAINT transaction_events_transaction_id_event_sequence_key UNIQUE (transaction_id, event_sequence);


--
-- Name: transaction_requests transaction_requests_idempotency_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_requests
    ADD CONSTRAINT transaction_requests_idempotency_key_key UNIQUE (idempotency_key);


--
-- Name: transaction_requests transaction_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_requests
    ADD CONSTRAINT transaction_requests_pkey PRIMARY KEY (id);


--
-- Name: account_relationships uk_account_user_relationship; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_relationships
    ADD CONSTRAINT uk_account_user_relationship UNIQUE (customer_account_id, user_profile_id, relationship_type);


--
-- Name: aml_alerts uk_aml_alert_tx_rule; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aml_alerts
    ADD CONSTRAINT uk_aml_alert_tx_rule UNIQUE (transaction_id, rule_code);


--
-- Name: aml_monitoring_rules uk_aml_monitoring_rule_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aml_monitoring_rules
    ADD CONSTRAINT uk_aml_monitoring_rule_code UNIQUE (code);


--
-- Name: gl_authorization_limits uk_auth_limits_role_currency_source; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_authorization_limits
    ADD CONSTRAINT uk_auth_limits_role_currency_source UNIQUE (approval_role, currency, transaction_source);


--
-- Name: balance_reservations uk_balance_reservations_transaction_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.balance_reservations
    ADD CONSTRAINT uk_balance_reservations_transaction_id UNIQUE (transaction_id);


--
-- Name: customer_relationships uk_cust_rel_primary_related_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_relationships
    ADD CONSTRAINT uk_cust_rel_primary_related_type UNIQUE (primary_customer_id, related_customer_id, relationship_type);


--
-- Name: exchange_rates uk_exchange_rate; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchange_rates
    ADD CONSTRAINT uk_exchange_rate UNIQUE (source_currency, target_currency, rate_date, rate_type);


--
-- Name: fiscal_periods uk_fiscal_period_year_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiscal_periods
    ADD CONSTRAINT uk_fiscal_period_year_number UNIQUE (fiscal_year, period_number);


--
-- Name: gl_daily_balances uk_gl_daily_balances_account_date; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_daily_balances
    ADD CONSTRAINT uk_gl_daily_balances_account_date UNIQUE (account_id, balance_date);


--
-- Name: gl_account_mappings uk_gl_mapping_account_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_account_mappings
    ADD CONSTRAINT uk_gl_mapping_account_type UNIQUE (customer_account_id, mapping_type, gl_account_id);


--
-- Name: holidays uk_holiday_date_country_region; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.holidays
    ADD CONSTRAINT uk_holiday_date_country_region UNIQUE (holiday_date, country_code, region_code);


--
-- Name: operational_gl_config uk_op_gl_config_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.operational_gl_config
    ADD CONSTRAINT uk_op_gl_config_type UNIQUE (config_type);


--
-- Name: velocity_limit_breaches velocity_limit_breaches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.velocity_limit_breaches
    ADD CONSTRAINT velocity_limit_breaches_pkey PRIMARY KEY (id);


--
-- Name: velocity_limits velocity_limits_account_id_transaction_type_limit_period_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.velocity_limits
    ADD CONSTRAINT velocity_limits_account_id_transaction_type_limit_period_key UNIQUE (account_id, transaction_type, limit_period);


--
-- Name: velocity_limits velocity_limits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.velocity_limits
    ADD CONSTRAINT velocity_limits_pkey PRIMARY KEY (id);


--
-- Name: idx_acc_trx_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_trx_account ON public.account_transactions USING btree (customer_account_id);


--
-- Name: idx_acc_trx_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_trx_date ON public.account_transactions USING btree (transaction_date);


--
-- Name: idx_acc_trx_ref; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_trx_ref ON public.account_transactions USING btree (reference_id);


--
-- Name: idx_account_holds_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_holds_account ON public.account_holds USING btree (customer_account_id);


--
-- Name: idx_account_holds_expires; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_holds_expires ON public.account_holds USING btree (expires_at);


--
-- Name: idx_account_holds_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_holds_status ON public.account_holds USING btree (status);


--
-- Name: idx_account_limits_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_limits_account ON public.account_limits USING btree (customer_account_id);


--
-- Name: idx_account_limits_effective; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_limits_effective ON public.account_limits USING btree (effective_from, effective_until);


--
-- Name: idx_account_limits_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_limits_period ON public.account_limits USING btree (limit_period);


--
-- Name: idx_account_limits_regulatory; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_limits_regulatory ON public.account_limits USING btree (is_regulatory);


--
-- Name: idx_account_limits_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_limits_type ON public.account_limits USING btree (limit_type);


--
-- Name: idx_account_relationships_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_relationships_account ON public.account_relationships USING btree (customer_account_id);


--
-- Name: idx_account_relationships_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_relationships_type ON public.account_relationships USING btree (relationship_type);


--
-- Name: idx_account_relationships_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_relationships_user ON public.account_relationships USING btree (user_profile_id);


--
-- Name: idx_accounts_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_accounts_created ON public.accounts USING btree (created_at);


--
-- Name: idx_accounts_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_accounts_number ON public.accounts USING btree (account_number);


--
-- Name: idx_accounts_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_accounts_status ON public.accounts USING btree (status);


--
-- Name: idx_accounts_user_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_accounts_user_product ON public.accounts USING btree (primary_user_profile_id, product_type);


--
-- Name: idx_aml_alert_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_aml_alert_account ON public.aml_alerts USING btree (source_account_id);


--
-- Name: idx_aml_alert_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_aml_alert_created_at ON public.aml_alerts USING btree (created_at);


--
-- Name: idx_appr_step_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appr_step_order ON public.identity_approval_workflow_steps USING btree (workflow_id, step_order);


--
-- Name: idx_appr_step_workflow; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appr_step_workflow ON public.identity_approval_workflow_steps USING btree (workflow_id);


--
-- Name: idx_appr_wf_resource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appr_wf_resource ON public.identity_approval_workflows USING btree (resource_type, resource_id);


--
-- Name: idx_appr_wf_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appr_wf_status ON public.identity_approval_workflows USING btree (status);


--
-- Name: idx_approvals_approver; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_approvals_approver ON public.gl_transaction_approvals USING btree (approved_by);


--
-- Name: idx_approvals_timestamp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_approvals_timestamp ON public.gl_transaction_approvals USING btree (approval_timestamp);


--
-- Name: idx_approvals_transaction; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_approvals_transaction ON public.gl_transaction_approvals USING btree (transaction_id);


--
-- Name: idx_audit_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_action ON public.gl_audit_trail USING btree (action);


--
-- Name: idx_audit_changed_by_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_changed_by_user ON public.identity_audit_events USING btree (changed_by_user_id);


--
-- Name: idx_audit_correlation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_correlation ON public.gl_audit_trail USING btree (correlation_id);


--
-- Name: idx_audit_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_created_at ON public.identity_audit_events USING btree (created_at);


--
-- Name: idx_audit_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_entity ON public.gl_audit_trail USING btree (entity_type, entity_id, performed_at);


--
-- Name: idx_audit_event_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_event_type ON public.identity_audit_events USING btree (event_type);


--
-- Name: idx_audit_log_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_action ON public.customer_audit_logs USING btree (action);


--
-- Name: idx_audit_log_changed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_changed_at ON public.customer_audit_logs USING btree (changed_at);


--
-- Name: idx_audit_log_changed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_changed_by ON public.customer_audit_logs USING btree (changed_by);


--
-- Name: idx_audit_log_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_customer ON public.customer_audit_logs USING btree (customer_id);


--
-- Name: idx_audit_log_field; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_field ON public.customer_audit_logs USING btree (field_name);


--
-- Name: idx_audit_performed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_performed_at ON public.gl_audit_trail USING btree (performed_at);


--
-- Name: idx_audit_performed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_performed_by ON public.gl_audit_trail USING btree (performed_by);


--
-- Name: idx_audit_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_user_id ON public.identity_audit_events USING btree (user_id);


--
-- Name: idx_audit_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_username ON public.identity_audit_events USING btree (username);


--
-- Name: idx_auth_limits_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auth_limits_role ON public.gl_authorization_limits USING btree (approval_role);


--
-- Name: idx_auth_limits_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auth_limits_source ON public.gl_authorization_limits USING btree (transaction_source);


--
-- Name: idx_balance_reservations_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_balance_reservations_account ON public.balance_reservations USING btree (account_id, status);


--
-- Name: idx_balance_reservations_expires; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_balance_reservations_expires ON public.balance_reservations USING btree (expires_at);


--
-- Name: idx_balance_reservations_status_expiration; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_balance_reservations_status_expiration ON public.balance_reservations USING btree (status, expires_at);


--
-- Name: idx_balance_reservations_transaction; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_balance_reservations_transaction ON public.balance_reservations USING btree (transaction_id);


--
-- Name: idx_banking_pref_branch; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_banking_pref_branch ON public.banking_preferences USING btree (preferred_branch_code);


--
-- Name: idx_banking_pref_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_banking_pref_customer ON public.banking_preferences USING btree (customer_id);


--
-- Name: idx_clearing_rule_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clearing_rule_active ON public.gl_suspense_clearing_rules USING btree (is_active);


--
-- Name: idx_clearing_rule_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clearing_rule_priority ON public.gl_suspense_clearing_rules USING btree (priority);


--
-- Name: idx_clearing_rule_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clearing_rule_type ON public.gl_suspense_clearing_rules USING btree (rule_type);


--
-- Name: idx_collaterals_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collaterals_account ON public.collaterals USING btree (loan_account_id);


--
-- Name: idx_collaterals_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collaterals_reference ON public.collaterals USING btree (collateral_reference);


--
-- Name: idx_collaterals_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collaterals_status ON public.collaterals USING btree (status);


--
-- Name: idx_collaterals_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collaterals_type ON public.collaterals USING btree (collateral_type);


--
-- Name: idx_collection_activities_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_activities_account ON public.collection_activities USING btree (loan_account_id);


--
-- Name: idx_collection_activities_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_activities_date ON public.collection_activities USING btree (activity_date);


--
-- Name: idx_collection_activities_follow_up; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_activities_follow_up ON public.collection_activities USING btree (follow_up_date);


--
-- Name: idx_collection_activities_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_activities_status ON public.collection_activities USING btree (status);


--
-- Name: idx_collection_activities_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_activities_type ON public.collection_activities USING btree (activity_type);


--
-- Name: idx_compensation_workflows_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compensation_workflows_created ON public.compensation_workflows USING btree (created_at);


--
-- Name: idx_compensation_workflows_original; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compensation_workflows_original ON public.compensation_workflows USING btree (original_transaction_id);


--
-- Name: idx_compensation_workflows_retry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compensation_workflows_retry ON public.compensation_workflows USING btree (next_retry_at);


--
-- Name: idx_compensation_workflows_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compensation_workflows_status ON public.compensation_workflows USING btree (workflow_status);


--
-- Name: idx_consent_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consent_customer ON public.customer_consents USING btree (customer_id);


--
-- Name: idx_consent_granted; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consent_granted ON public.customer_consents USING btree (granted);


--
-- Name: idx_consent_recorded_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consent_recorded_at ON public.customer_consents USING btree (recorded_at);


--
-- Name: idx_consent_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consent_type ON public.customer_consents USING btree (consent_type);


--
-- Name: idx_contact_details_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contact_details_customer ON public.contact_details USING btree (customer_id);


--
-- Name: idx_contact_details_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contact_details_type ON public.contact_details USING btree (type);


--
-- Name: idx_cust_addr_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cust_addr_customer ON public.customer_addresses USING btree (customer_id);


--
-- Name: idx_cust_addr_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cust_addr_type ON public.customer_addresses USING btree (type);


--
-- Name: idx_cust_rel_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cust_rel_active ON public.customer_relationships USING btree (active);


--
-- Name: idx_cust_rel_primary; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cust_rel_primary ON public.customer_relationships USING btree (primary_customer_id);


--
-- Name: idx_cust_rel_related; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cust_rel_related ON public.customer_relationships USING btree (related_customer_id);


--
-- Name: idx_cust_rel_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cust_rel_type ON public.customer_relationships USING btree (relationship_type);


--
-- Name: idx_customers_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_number ON public.customers USING btree (customer_number);


--
-- Name: idx_customers_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_status ON public.customers USING btree (status);


--
-- Name: idx_customers_tax_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_tax_id ON public.customers USING btree (tax_id);


--
-- Name: idx_customers_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_type ON public.customers USING btree (type);


--
-- Name: idx_delegation_from; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delegation_from ON public.identity_delegations USING btree (delegated_from_id);


--
-- Name: idx_delegation_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delegation_status ON public.identity_delegations USING btree (status);


--
-- Name: idx_delegation_to; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delegation_to ON public.identity_delegations USING btree (delegated_to_id);


--
-- Name: idx_delegation_valid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_delegation_valid ON public.identity_delegations USING btree (valid_from, valid_until);


--
-- Name: idx_dsar_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dsar_customer ON public.data_subject_requests USING btree (customer_id);


--
-- Name: idx_dsar_due_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dsar_due_by ON public.data_subject_requests USING btree (due_by);


--
-- Name: idx_dsar_received_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dsar_received_at ON public.data_subject_requests USING btree (received_at);


--
-- Name: idx_dsar_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dsar_status ON public.data_subject_requests USING btree (status);


--
-- Name: idx_dsar_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dsar_type ON public.data_subject_requests USING btree (request_type);


--
-- Name: idx_early_settlements_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_early_settlements_account ON public.early_settlements USING btree (loan_account_id);


--
-- Name: idx_early_settlements_quote_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_early_settlements_quote_date ON public.early_settlements USING btree (quote_date);


--
-- Name: idx_early_settlements_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_early_settlements_reference ON public.early_settlements USING btree (quote_reference);


--
-- Name: idx_early_settlements_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_early_settlements_status ON public.early_settlements USING btree (status);


--
-- Name: idx_early_settlements_valid_until; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_early_settlements_valid_until ON public.early_settlements USING btree (valid_until);


--
-- Name: idx_escalation_assigned_to; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_escalation_assigned_to ON public.gl_suspense_escalations USING btree (assigned_to);


--
-- Name: idx_escalation_due_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_escalation_due_date ON public.gl_suspense_escalations USING btree (due_date);


--
-- Name: idx_escalation_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_escalation_level ON public.gl_suspense_escalations USING btree (escalation_level);


--
-- Name: idx_escalation_resolved; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_escalation_resolved ON public.gl_suspense_escalations USING btree (is_resolved);


--
-- Name: idx_escalation_suspense; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_escalation_suspense ON public.gl_suspense_escalations USING btree (suspense_item_id);


--
-- Name: idx_exchange_rates_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_exchange_rates_date ON public.exchange_rates USING btree (rate_date);


--
-- Name: idx_exchange_rates_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_exchange_rates_lookup ON public.exchange_rates USING btree (source_currency, target_currency, rate_date, rate_type);


--
-- Name: idx_fee_rules_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_rules_active ON public.fee_rules USING btree (is_active);


--
-- Name: idx_fee_rules_customer_tier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_rules_customer_tier ON public.fee_rules USING btree (customer_tier);


--
-- Name: idx_fee_rules_effective_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_rules_effective_date ON public.fee_rules USING btree (effective_from, effective_to);


--
-- Name: idx_fee_rules_transaction_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_rules_transaction_type ON public.fee_rules USING btree (transaction_type);


--
-- Name: idx_fee_waivers_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_waivers_account ON public.fee_waivers USING btree (account_id);


--
-- Name: idx_fee_waivers_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_waivers_active ON public.fee_waivers USING btree (is_active);


--
-- Name: idx_fee_waivers_campaign; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_waivers_campaign ON public.fee_waivers USING btree (campaign_code);


--
-- Name: idx_fee_waivers_effective_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_waivers_effective_date ON public.fee_waivers USING btree (effective_from, effective_to);


--
-- Name: idx_fee_waivers_transaction_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_waivers_transaction_type ON public.fee_waivers USING btree (transaction_type);


--
-- Name: idx_fiscal_period_dates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fiscal_period_dates ON public.fiscal_periods USING btree (start_date, end_date);


--
-- Name: idx_fiscal_period_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fiscal_period_status ON public.fiscal_periods USING btree (status);


--
-- Name: idx_fiscal_period_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fiscal_period_year ON public.fiscal_periods USING btree (fiscal_year);


--
-- Name: idx_fiscal_period_year_num; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fiscal_period_year_num ON public.fiscal_periods USING btree (fiscal_year, period_number);


--
-- Name: idx_fx_spreads_pair; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fx_spreads_pair ON public.fx_spreads USING btree (source_currency, target_currency);


--
-- Name: idx_gl_accounts_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_accounts_code ON public.gl_accounts USING btree (code);


--
-- Name: idx_gl_accounts_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_accounts_parent ON public.gl_accounts USING btree (parent_id);


--
-- Name: idx_gl_accounts_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_accounts_type ON public.gl_accounts USING btree (type);


--
-- Name: idx_gl_daily_balances_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_daily_balances_account ON public.gl_daily_balances USING btree (account_id);


--
-- Name: idx_gl_daily_balances_account_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_daily_balances_account_date ON public.gl_daily_balances USING btree (account_id, balance_date);


--
-- Name: idx_gl_daily_balances_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_daily_balances_date ON public.gl_daily_balances USING btree (balance_date);


--
-- Name: idx_gl_journal_entries_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_journal_entries_account ON public.gl_journal_entries USING btree (account_id);


--
-- Name: idx_gl_journal_entries_account_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_journal_entries_account_date ON public.gl_journal_entries USING btree (account_id, transaction_id);


--
-- Name: idx_gl_journal_entries_transaction; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_journal_entries_transaction ON public.gl_journal_entries USING btree (transaction_id);


--
-- Name: idx_gl_mappings_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_mappings_active ON public.gl_account_mappings USING btree (is_active);


--
-- Name: idx_gl_mappings_customer_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_mappings_customer_account ON public.gl_account_mappings USING btree (customer_account_id);


--
-- Name: idx_gl_mappings_gl_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_mappings_gl_account ON public.gl_account_mappings USING btree (gl_account_id);


--
-- Name: idx_gl_mappings_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_mappings_type ON public.gl_account_mappings USING btree (mapping_type);


--
-- Name: idx_gl_reconciliations_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_reconciliations_date ON public.gl_reconciliations USING btree (reconciliation_date);


--
-- Name: idx_gl_reconciliations_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_reconciliations_status ON public.gl_reconciliations USING btree (status);


--
-- Name: idx_gl_reconciliations_transaction; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_reconciliations_transaction ON public.gl_reconciliations USING btree (transaction_id);


--
-- Name: idx_gl_revaluation_details_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_revaluation_details_account ON public.gl_revaluation_details USING btree (account_id);


--
-- Name: idx_gl_revaluation_details_run; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_revaluation_details_run ON public.gl_revaluation_details USING btree (revaluation_run_id);


--
-- Name: idx_gl_revaluation_details_transaction; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_revaluation_details_transaction ON public.gl_revaluation_details USING btree (journal_transaction_id);


--
-- Name: idx_gl_revaluation_runs_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_revaluation_runs_date ON public.gl_revaluation_runs USING btree (revaluation_date);


--
-- Name: idx_gl_revaluation_runs_executed; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_revaluation_runs_executed ON public.gl_revaluation_runs USING btree (executed_at);


--
-- Name: idx_gl_transactions_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_transactions_date ON public.gl_transactions USING btree (transaction_date);


--
-- Name: idx_gl_transactions_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_transactions_reference ON public.gl_transactions USING btree (reference_id);


--
-- Name: idx_gl_transactions_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gl_transactions_status ON public.gl_transactions USING btree (status);


--
-- Name: idx_guarantors_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_guarantors_account ON public.guarantors USING btree (loan_account_id);


--
-- Name: idx_guarantors_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_guarantors_customer ON public.guarantors USING btree (customer_id);


--
-- Name: idx_guarantors_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_guarantors_status ON public.guarantors USING btree (status);


--
-- Name: idx_guarantors_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_guarantors_type ON public.guarantors USING btree (guarantor_type);


--
-- Name: idx_holiday_bank; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_holiday_bank ON public.holidays USING btree (is_bank_holiday);


--
-- Name: idx_holiday_country_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_holiday_country_year ON public.holidays USING btree (country_code, holiday_year);


--
-- Name: idx_holiday_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_holiday_date ON public.holidays USING btree (holiday_date);


--
-- Name: idx_id_docs_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_id_docs_customer ON public.identification_documents USING btree (customer_id);


--
-- Name: idx_id_docs_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_id_docs_number ON public.identification_documents USING btree (document_number);


--
-- Name: idx_id_docs_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_id_docs_type ON public.identification_documents USING btree (type);


--
-- Name: idx_identity_users_customer_party; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_identity_users_customer_party ON public.identity_users USING btree (customer_party_id);


--
-- Name: idx_identity_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_identity_users_email ON public.identity_users USING btree (email);


--
-- Name: idx_identity_users_employee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_identity_users_employee ON public.identity_users USING btree (employee_id);


--
-- Name: idx_identity_users_provisioning; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_identity_users_provisioning ON public.identity_users USING btree (provisioning_status);


--
-- Name: idx_interest_accruals_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interest_accruals_account ON public.interest_accruals USING btree (loan_account_id);


--
-- Name: idx_interest_accruals_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interest_accruals_date ON public.interest_accruals USING btree (accrual_date);


--
-- Name: idx_interest_accruals_posted; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interest_accruals_posted ON public.interest_accruals USING btree (is_posted);


--
-- Name: idx_interest_rates_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interest_rates_account ON public.interest_rates USING btree (customer_account_id);


--
-- Name: idx_interest_rates_effective; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interest_rates_effective ON public.interest_rates USING btree (effective_from, effective_until);


--
-- Name: idx_interest_rates_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_interest_rates_type ON public.interest_rates USING btree (rate_type);


--
-- Name: idx_kyc_step_decision; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_kyc_step_decision ON public.kyc_review_steps USING btree (decision);


--
-- Name: idx_kyc_step_reviewed; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_kyc_step_reviewed ON public.kyc_review_steps USING btree (reviewed_at);


--
-- Name: idx_kyc_step_workflow; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_kyc_step_workflow ON public.kyc_review_steps USING btree (kyc_workflow_id);


--
-- Name: idx_kyc_workflow_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_kyc_workflow_customer ON public.kyc_workflows USING btree (customer_id);


--
-- Name: idx_kyc_workflow_initiated; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_kyc_workflow_initiated ON public.kyc_workflows USING btree (initiated_at);


--
-- Name: idx_kyc_workflow_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_kyc_workflow_status ON public.kyc_workflows USING btree (status);


--
-- Name: idx_loan_accounts_application; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_accounts_application ON public.loan_accounts USING btree (application_id);


--
-- Name: idx_loan_accounts_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_accounts_customer ON public.loan_accounts USING btree (customer_id);


--
-- Name: idx_loan_accounts_delinquency; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_accounts_delinquency ON public.loan_accounts USING btree (days_past_due);


--
-- Name: idx_loan_accounts_disbursement; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_accounts_disbursement ON public.loan_accounts USING btree (disbursement_date);


--
-- Name: idx_loan_accounts_maturity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_accounts_maturity ON public.loan_accounts USING btree (maturity_date);


--
-- Name: idx_loan_accounts_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_accounts_number ON public.loan_accounts USING btree (loan_account_number);


--
-- Name: idx_loan_accounts_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_accounts_product ON public.loan_accounts USING btree (product_id);


--
-- Name: idx_loan_accounts_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_accounts_status ON public.loan_accounts USING btree (status);


--
-- Name: idx_loan_applications_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_applications_created ON public.loan_applications USING btree (created_at);


--
-- Name: idx_loan_applications_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_applications_customer ON public.loan_applications USING btree (customer_id);


--
-- Name: idx_loan_applications_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_applications_number ON public.loan_applications USING btree (application_number);


--
-- Name: idx_loan_applications_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_applications_product ON public.loan_applications USING btree (product_id);


--
-- Name: idx_loan_applications_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_applications_status ON public.loan_applications USING btree (status);


--
-- Name: idx_loan_disbursements_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_disbursements_account ON public.loan_disbursements USING btree (loan_account_id);


--
-- Name: idx_loan_disbursements_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_disbursements_date ON public.loan_disbursements USING btree (disbursement_date);


--
-- Name: idx_loan_disbursements_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_disbursements_reference ON public.loan_disbursements USING btree (disbursement_reference);


--
-- Name: idx_loan_disbursements_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_disbursements_status ON public.loan_disbursements USING btree (status);


--
-- Name: idx_loan_documents_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_documents_account ON public.loan_documents USING btree (loan_account_id);


--
-- Name: idx_loan_documents_expiry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_documents_expiry ON public.loan_documents USING btree (expiry_date);


--
-- Name: idx_loan_documents_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_documents_status ON public.loan_documents USING btree (status);


--
-- Name: idx_loan_documents_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_documents_type ON public.loan_documents USING btree (document_type);


--
-- Name: idx_loan_fees_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_fees_account ON public.loan_fees USING btree (loan_account_id);


--
-- Name: idx_loan_fees_charge_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_fees_charge_date ON public.loan_fees USING btree (charge_date);


--
-- Name: idx_loan_fees_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_fees_type ON public.loan_fees USING btree (fee_type);


--
-- Name: idx_loan_fees_waived; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_fees_waived ON public.loan_fees USING btree (is_waived);


--
-- Name: idx_loan_payments_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_payments_account ON public.loan_payments USING btree (loan_account_id);


--
-- Name: idx_loan_payments_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_payments_date ON public.loan_payments USING btree (payment_date);


--
-- Name: idx_loan_payments_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_payments_reference ON public.loan_payments USING btree (payment_reference);


--
-- Name: idx_loan_payments_reversed; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_payments_reversed ON public.loan_payments USING btree (is_reversed);


--
-- Name: idx_loan_payments_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_payments_type ON public.loan_payments USING btree (payment_type);


--
-- Name: idx_loan_products_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_products_active ON public.loan_products USING btree (active);


--
-- Name: idx_loan_products_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_products_code ON public.loan_products USING btree (product_code);


--
-- Name: idx_loan_products_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_products_type ON public.loan_products USING btree (product_type);


--
-- Name: idx_loan_provisions_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_provisions_account ON public.loan_provisions USING btree (loan_account_id);


--
-- Name: idx_loan_provisions_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_provisions_date ON public.loan_provisions USING btree (provision_date);


--
-- Name: idx_loan_provisions_posted; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_provisions_posted ON public.loan_provisions USING btree (is_posted);


--
-- Name: idx_loan_provisions_stage; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_provisions_stage ON public.loan_provisions USING btree (provision_stage);


--
-- Name: idx_loan_restructurings_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_restructurings_account ON public.loan_restructurings USING btree (loan_account_id);


--
-- Name: idx_loan_restructurings_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_restructurings_date ON public.loan_restructurings USING btree (restructuring_date);


--
-- Name: idx_loan_restructurings_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_restructurings_type ON public.loan_restructurings USING btree (restructuring_type);


--
-- Name: idx_loan_schedules_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_schedules_account ON public.loan_schedules USING btree (loan_account_id);


--
-- Name: idx_loan_schedules_account_installment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_schedules_account_installment ON public.loan_schedules USING btree (loan_account_id, installment_number);


--
-- Name: idx_loan_schedules_due_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_schedules_due_date ON public.loan_schedules USING btree (due_date);


--
-- Name: idx_loan_schedules_overdue; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_schedules_overdue ON public.loan_schedules USING btree (is_overdue);


--
-- Name: idx_loan_schedules_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_schedules_status ON public.loan_schedules USING btree (status);


--
-- Name: idx_loan_transactions_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_transactions_account ON public.loan_transactions USING btree (loan_account_id);


--
-- Name: idx_loan_transactions_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_transactions_date ON public.loan_transactions USING btree (transaction_date);


--
-- Name: idx_loan_transactions_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_transactions_reference ON public.loan_transactions USING btree (transaction_reference);


--
-- Name: idx_loan_transactions_reversed; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_transactions_reversed ON public.loan_transactions USING btree (is_reversed);


--
-- Name: idx_loan_transactions_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loan_transactions_type ON public.loan_transactions USING btree (transaction_type);


--
-- Name: idx_onboarding_channel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_onboarding_channel ON public.customer_onboardings USING btree (onboarding_channel);


--
-- Name: idx_onboarding_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_onboarding_customer ON public.customer_onboardings USING btree (customer_id);


--
-- Name: idx_onboarding_started_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_onboarding_started_at ON public.customer_onboardings USING btree (started_at);


--
-- Name: idx_onboarding_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_onboarding_status ON public.customer_onboardings USING btree (status);


--
-- Name: idx_op_gl_config_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_op_gl_config_active ON public.operational_gl_config USING btree (is_active);


--
-- Name: idx_op_gl_config_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_op_gl_config_type ON public.operational_gl_config USING btree (config_type);


--
-- Name: idx_retention_anonymized; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_retention_anonymized ON public.customer_data_retention USING btree (anonymized);


--
-- Name: idx_retention_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_retention_customer ON public.customer_data_retention USING btree (customer_id);


--
-- Name: idx_retention_expires; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_retention_expires ON public.customer_data_retention USING btree (retention_expires_at);


--
-- Name: idx_seq_fiscal_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_seq_fiscal_period ON public.gl_transaction_sequences USING btree (fiscal_period_id);


--
-- Name: idx_suspense_assigned_to; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_suspense_assigned_to ON public.gl_suspense_items USING btree (assigned_to);


--
-- Name: idx_suspense_gl_transaction; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_suspense_gl_transaction ON public.gl_suspense_items USING btree (gl_transaction_id);


--
-- Name: idx_suspense_posting_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_suspense_posting_date ON public.gl_suspense_items USING btree (posting_date);


--
-- Name: idx_suspense_reason; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_suspense_reason ON public.gl_suspense_items USING btree (reason_code);


--
-- Name: idx_suspense_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_suspense_status ON public.gl_suspense_items USING btree (status);


--
-- Name: idx_tp_transactions_destination_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tp_transactions_destination_account ON public.tp_transactions USING btree (destination_account_id);


--
-- Name: idx_tp_transactions_gateway; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tp_transactions_gateway ON public.tp_transactions USING btree (gateway_transaction_id);


--
-- Name: idx_tp_transactions_gl_transaction; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tp_transactions_gl_transaction ON public.tp_transactions USING btree (gl_transaction_id);


--
-- Name: idx_tp_transactions_processing_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tp_transactions_processing_time ON public.tp_transactions USING btree (processing_started_at, completed_at);


--
-- Name: idx_tp_transactions_source_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tp_transactions_source_account ON public.tp_transactions USING btree (source_account_id);


--
-- Name: idx_tp_transactions_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tp_transactions_status ON public.tp_transactions USING btree (status);


--
-- Name: idx_transaction_events_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transaction_events_created_at ON public.transaction_events USING btree (created_at);


--
-- Name: idx_transaction_events_transaction; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transaction_events_transaction ON public.transaction_events USING btree (transaction_id, event_sequence);


--
-- Name: idx_transaction_events_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transaction_events_type ON public.transaction_events USING btree (event_type);


--
-- Name: idx_transaction_requests_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transaction_requests_created_at ON public.transaction_requests USING btree (created_at);


--
-- Name: idx_transaction_requests_idempotency; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transaction_requests_idempotency ON public.transaction_requests USING btree (idempotency_key);


--
-- Name: idx_transaction_requests_source_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transaction_requests_source_account ON public.transaction_requests USING btree (source_account_id);


--
-- Name: data_subject_requests fk1dmpdvyvqki8usa2cjwvfiyig; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_subject_requests
    ADD CONSTRAINT fk1dmpdvyvqki8usa2cjwvfiyig FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: gl_transaction_sequences fk1o3rkg0ti1q8ausibwcoc3vur; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_transaction_sequences
    ADD CONSTRAINT fk1o3rkg0ti1q8ausibwcoc3vur FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);


--
-- Name: loan_disbursements fk1obpqjbgi0ntallkoqb6os22l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_disbursements
    ADD CONSTRAINT fk1obpqjbgi0ntallkoqb6os22l FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: gl_revaluation_details fk210mh6jto89l42s5wwyma5qoq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_revaluation_details
    ADD CONSTRAINT fk210mh6jto89l42s5wwyma5qoq FOREIGN KEY (revaluation_run_id) REFERENCES public.gl_revaluation_runs(id);


--
-- Name: gl_suspense_items fk26qpex91d4rw7us8l16yhns2d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_suspense_items
    ADD CONSTRAINT fk26qpex91d4rw7us8l16yhns2d FOREIGN KEY (target_account_id) REFERENCES public.gl_accounts(id);


--
-- Name: gl_revaluation_details fk29gaw0etu277lv56g85rwuohp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_revaluation_details
    ADD CONSTRAINT fk29gaw0etu277lv56g85rwuohp FOREIGN KEY (journal_transaction_id) REFERENCES public.gl_transactions(id);


--
-- Name: early_settlements fk2yk224i9fm7xomxsqhwqpocgc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.early_settlements
    ADD CONSTRAINT fk2yk224i9fm7xomxsqhwqpocgc FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: loan_restructurings fk2yvfvm6rj270q7vbqqwilpwgu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_restructurings
    ADD CONSTRAINT fk2yvfvm6rj270q7vbqqwilpwgu FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: banking_preferences fk31jhv9t8qgtxfbhpg3nvjgaot; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.banking_preferences
    ADD CONSTRAINT fk31jhv9t8qgtxfbhpg3nvjgaot FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: gl_revaluation_details fk378yxevnseihawa8b84e98va7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_revaluation_details
    ADD CONSTRAINT fk378yxevnseihawa8b84e98va7 FOREIGN KEY (account_id) REFERENCES public.gl_accounts(id);


--
-- Name: gl_reconciliations fk591xfpb6tpgjyi9413fb1hlkc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_reconciliations
    ADD CONSTRAINT fk591xfpb6tpgjyi9413fb1hlkc FOREIGN KEY (transaction_id) REFERENCES public.gl_transactions(id);


--
-- Name: identity_role_permissions fk5a3cbjdecavrm5ucgo2t9uyym; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_role_permissions
    ADD CONSTRAINT fk5a3cbjdecavrm5ucgo2t9uyym FOREIGN KEY (role_id) REFERENCES public.identity_roles(id);


--
-- Name: account_limits fk5c4llqfmxvlecdxpko9kpd8fo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_limits
    ADD CONSTRAINT fk5c4llqfmxvlecdxpko9kpd8fo FOREIGN KEY (customer_account_id) REFERENCES public.accounts(id);


--
-- Name: identity_mfa_recovery_codes fk64hb38o4mggycqkkf6p9gxhfe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_mfa_recovery_codes
    ADD CONSTRAINT fk64hb38o4mggycqkkf6p9gxhfe FOREIGN KEY (user_id) REFERENCES public.identity_users(id);


--
-- Name: customer_data_retention fk6li84vkcqhh7k54u04p1wg4tu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_data_retention
    ADD CONSTRAINT fk6li84vkcqhh7k54u04p1wg4tu FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: gl_suspense_escalations fk6rhyxnik1w19b9f731r3dro0c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_suspense_escalations
    ADD CONSTRAINT fk6rhyxnik1w19b9f731r3dro0c FOREIGN KEY (suspense_item_id) REFERENCES public.gl_suspense_items(id);


--
-- Name: interest_accruals fk77yqrw6jrfadkvhsw0ip08rsd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interest_accruals
    ADD CONSTRAINT fk77yqrw6jrfadkvhsw0ip08rsd FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: customer_consents fk8vxf5423q7odlib9unudi39fa; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_consents
    ADD CONSTRAINT fk8vxf5423q7odlib9unudi39fa FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: kyc_workflows fk97o3osacb43ukaeyb385p3ok5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kyc_workflows
    ADD CONSTRAINT fk97o3osacb43ukaeyb385p3ok5 FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: identity_user_roles fk9qj4blviadb3fka54on8ttxn8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_user_roles
    ADD CONSTRAINT fk9qj4blviadb3fka54on8ttxn8 FOREIGN KEY (role_id) REFERENCES public.identity_roles(id);


--
-- Name: loan_transactions fk9yye667d37k83gbtmt7dvud16; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_transactions
    ADD CONSTRAINT fk9yye667d37k83gbtmt7dvud16 FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: account_relationships fk_account_relationship_customer_account; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_relationships
    ADD CONSTRAINT fk_account_relationship_customer_account FOREIGN KEY (customer_account_id) REFERENCES public.accounts(id);


--
-- Name: gl_accounts fk_gl_account_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_accounts
    ADD CONSTRAINT fk_gl_account_parent FOREIGN KEY (parent_id) REFERENCES public.gl_accounts(id);


--
-- Name: gl_account_mappings fk_gl_mapping_customer_account; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_account_mappings
    ADD CONSTRAINT fk_gl_mapping_customer_account FOREIGN KEY (customer_account_id) REFERENCES public.accounts(id);


--
-- Name: operational_gl_config fk_op_gl_config_account; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.operational_gl_config
    ADD CONSTRAINT fk_op_gl_config_account FOREIGN KEY (gl_account_id) REFERENCES public.gl_accounts(id);


--
-- Name: tp_transactions fk_transaction_fee_rule; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tp_transactions
    ADD CONSTRAINT fk_transaction_fee_rule FOREIGN KEY (applied_fee_rule_id) REFERENCES public.fee_rules(id);


--
-- Name: tp_transactions fk_transaction_request; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tp_transactions
    ADD CONSTRAINT fk_transaction_request FOREIGN KEY (request_id) REFERENCES public.transaction_requests(id);


--
-- Name: account_holds fkam83rdf453kj1etpq368b2hnk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_holds
    ADD CONSTRAINT fkam83rdf453kj1etpq368b2hnk FOREIGN KEY (customer_account_id) REFERENCES public.accounts(id);


--
-- Name: identity_approval_workflow_steps fkbwdghhma1en6qrbee72ram7k0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_approval_workflow_steps
    ADD CONSTRAINT fkbwdghhma1en6qrbee72ram7k0 FOREIGN KEY (workflow_id) REFERENCES public.identity_approval_workflows(id);


--
-- Name: loan_provisions fkcjn3nuu22ggmgoeyow1hs7hqi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_provisions
    ADD CONSTRAINT fkcjn3nuu22ggmgoeyow1hs7hqi FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: gl_transactions fkcr7fi969up9h80v2g624blro3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_transactions
    ADD CONSTRAINT fkcr7fi969up9h80v2g624blro3 FOREIGN KEY (reversed_by) REFERENCES public.gl_transactions(id);


--
-- Name: contact_details fkcyrtetx4xuhkq0j8vmktrqesv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contact_details
    ADD CONSTRAINT fkcyrtetx4xuhkq0j8vmktrqesv FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: interest_rates fkd8i1xnyotpao7euyivq8tlk96; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.interest_rates
    ADD CONSTRAINT fkd8i1xnyotpao7euyivq8tlk96 FOREIGN KEY (customer_account_id) REFERENCES public.accounts(id);


--
-- Name: identity_user_roles fkdxtipao0k0xgb80ajvviilwwl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_user_roles
    ADD CONSTRAINT fkdxtipao0k0xgb80ajvviilwwl FOREIGN KEY (user_id) REFERENCES public.identity_users(id);


--
-- Name: kyc_review_steps fke3by0cim1ke271c5tp30yll4s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kyc_review_steps
    ADD CONSTRAINT fke3by0cim1ke271c5tp30yll4s FOREIGN KEY (kyc_workflow_id) REFERENCES public.kyc_workflows(id);


--
-- Name: guarantors fke3f3x6ytif5kpvmckkjtwsgtl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guarantors
    ADD CONSTRAINT fke3f3x6ytif5kpvmckkjtwsgtl FOREIGN KEY (loan_application_id) REFERENCES public.loan_applications(id);


--
-- Name: collaterals fke9ur0se37ahfjnw126kp9b5l6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collaterals
    ADD CONSTRAINT fke9ur0se37ahfjnw126kp9b5l6 FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: transaction_events fkedky8wyewtlk5srdaoyp3u2ew; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_events
    ADD CONSTRAINT fkedky8wyewtlk5srdaoyp3u2ew FOREIGN KEY (transaction_id) REFERENCES public.tp_transactions(id);


--
-- Name: balance_reservations fkeuvxjfjaea78eyua3hlus2rap; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.balance_reservations
    ADD CONSTRAINT fkeuvxjfjaea78eyua3hlus2rap FOREIGN KEY (transaction_id) REFERENCES public.tp_transactions(id);


--
-- Name: compensation_workflows fkg0si2ms96t73okqb0digslfxn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compensation_workflows
    ADD CONSTRAINT fkg0si2ms96t73okqb0digslfxn FOREIGN KEY (original_transaction_id) REFERENCES public.tp_transactions(id);


--
-- Name: identity_roles fkg52gdcaw1vyvxi5ujdd9g5bm3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_roles
    ADD CONSTRAINT fkg52gdcaw1vyvxi5ujdd9g5bm3 FOREIGN KEY (parent_role_id) REFERENCES public.identity_roles(id);


--
-- Name: gl_suspense_clearing_rules fkh5h0pp52ngaov3ajh3x2pssjr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_suspense_clearing_rules
    ADD CONSTRAINT fkh5h0pp52ngaov3ajh3x2pssjr FOREIGN KEY (target_account_id) REFERENCES public.gl_accounts(id);


--
-- Name: guarantors fkhgspn700vb53t3vrgib7y4am8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.guarantors
    ADD CONSTRAINT fkhgspn700vb53t3vrgib7y4am8 FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: loan_schedules fkhhd3juunxjyic8ciiknoqj2cf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_schedules
    ADD CONSTRAINT fkhhd3juunxjyic8ciiknoqj2cf FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: gl_journal_entries fkhpjpall1w3qcokfxs2ocbfg83; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_journal_entries
    ADD CONSTRAINT fkhpjpall1w3qcokfxs2ocbfg83 FOREIGN KEY (account_id) REFERENCES public.gl_accounts(id);


--
-- Name: identity_delegations fkidns8cotxkju93ab9rnupghsm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_delegations
    ADD CONSTRAINT fkidns8cotxkju93ab9rnupghsm FOREIGN KEY (delegated_from_id) REFERENCES public.identity_users(id);


--
-- Name: customer_audit_logs fkirp5gdydbfssyq0awrrswfjqp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_audit_logs
    ADD CONSTRAINT fkirp5gdydbfssyq0awrrswfjqp FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: loan_documents fkj8oto5ccuwm240p136n4en59t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_documents
    ADD CONSTRAINT fkj8oto5ccuwm240p136n4en59t FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: customer_relationships fkl5hw403g4sifbawnjawb0vvd8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_relationships
    ADD CONSTRAINT fkl5hw403g4sifbawnjawb0vvd8 FOREIGN KEY (related_customer_id) REFERENCES public.customers(id);


--
-- Name: collection_activities fkm27nltqpg462v6j8v5ovlxx0x; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_activities
    ADD CONSTRAINT fkm27nltqpg462v6j8v5ovlxx0x FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: account_transactions fkmagg0aj56ciab1puw1dyw9wcq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_transactions
    ADD CONSTRAINT fkmagg0aj56ciab1puw1dyw9wcq FOREIGN KEY (customer_account_id) REFERENCES public.accounts(id);


--
-- Name: customer_relationships fkmd6hi78d3ieg5ba41dipdl5tp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_relationships
    ADD CONSTRAINT fkmd6hi78d3ieg5ba41dipdl5tp FOREIGN KEY (primary_customer_id) REFERENCES public.customers(id);


--
-- Name: gl_suspense_items fkmpm942kbho8prpoy3a2dpmeqb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_suspense_items
    ADD CONSTRAINT fkmpm942kbho8prpoy3a2dpmeqb FOREIGN KEY (gl_transaction_id) REFERENCES public.gl_transactions(id);


--
-- Name: compensation_workflows fkn032vwvfkdis4nceng1h9m99a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compensation_workflows
    ADD CONSTRAINT fkn032vwvfkdis4nceng1h9m99a FOREIGN KEY (compensation_transaction_id) REFERENCES public.tp_transactions(id);


--
-- Name: loan_fees fknriujwmr14n50s538vv0nggn9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_fees
    ADD CONSTRAINT fknriujwmr14n50s538vv0nggn9 FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: identity_password_history fkopoy86k5tnj31lq8jjl36rgr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_password_history
    ADD CONSTRAINT fkopoy86k5tnj31lq8jjl36rgr FOREIGN KEY (user_id) REFERENCES public.identity_users(id);


--
-- Name: gl_daily_balances fkp69nn3lvtekadbbhwdu08crct; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_daily_balances
    ADD CONSTRAINT fkp69nn3lvtekadbbhwdu08crct FOREIGN KEY (account_id) REFERENCES public.gl_accounts(id);


--
-- Name: identity_delegations fkpb1s7n9w25orb1496pon5as1o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_delegations
    ADD CONSTRAINT fkpb1s7n9w25orb1496pon5as1o FOREIGN KEY (delegated_to_id) REFERENCES public.identity_users(id);


--
-- Name: gl_journal_entries fkplb9yig0o7py4t9w5p5rxptir; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_journal_entries
    ADD CONSTRAINT fkplb9yig0o7py4t9w5p5rxptir FOREIGN KEY (transaction_id) REFERENCES public.gl_transactions(id);


--
-- Name: gl_transaction_approvals fkqmeefkp1voakav3qr9dpwntnt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_transaction_approvals
    ADD CONSTRAINT fkqmeefkp1voakav3qr9dpwntnt FOREIGN KEY (transaction_id) REFERENCES public.gl_transactions(id);


--
-- Name: loan_payments fkr8x5t38d9i1hjqc9wdjik923b; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_payments
    ADD CONSTRAINT fkr8x5t38d9i1hjqc9wdjik923b FOREIGN KEY (loan_account_id) REFERENCES public.loan_accounts(id);


--
-- Name: customer_addresses fkrvr6wl9gll7u98cda18smugp4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_addresses
    ADD CONSTRAINT fkrvr6wl9gll7u98cda18smugp4 FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: identification_documents fks18vy575xdly38fqs75j5kgg7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identification_documents
    ADD CONSTRAINT fks18vy575xdly38fqs75j5kgg7 FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: customer_onboardings fks4tq3avlpycxudkfju8hvhjfq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_onboardings
    ADD CONSTRAINT fks4tq3avlpycxudkfju8hvhjfq FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: gl_suspense_items fksdakl2bsqmdf3pj308x1jsl6e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gl_suspense_items
    ADD CONSTRAINT fksdakl2bsqmdf3pj308x1jsl6e FOREIGN KEY (clearing_transaction_id) REFERENCES public.gl_transactions(id);


--
-- Name: identity_role_scoped_grants fkssijf7kcncx2b23viscgaj1sd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identity_role_scoped_grants
    ADD CONSTRAINT fkssijf7kcncx2b23viscgaj1sd FOREIGN KEY (role_id) REFERENCES public.identity_roles(id);


--
-- PostgreSQL database dump complete
--


