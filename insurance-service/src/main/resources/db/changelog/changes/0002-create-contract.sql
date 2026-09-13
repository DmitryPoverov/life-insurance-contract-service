--liquibase formatted sql

--changeset dmitrypoverov:0002-create-contract
CREATE SEQUENCE contract_number_seq MAXVALUE 999999;

CREATE TABLE contract (
    id                      UUID           PRIMARY KEY,
    application_id          UUID           NOT NULL,
    contract_seq            BIGINT         NOT NULL DEFAULT nextval('contract_number_seq'),
    contract_number         VARCHAR(32)    GENERATED ALWAYS AS (
        'LI-' || extract(year FROM issued_at AT TIME ZONE 'UTC')::int::text
            || '-' || lpad(contract_seq::text, 6, '0')
    ) STORED,
    policyholder_subject    VARCHAR(64)    NOT NULL,
    insured_full_name       VARCHAR(255)   NOT NULL,
    insured_birth_date      DATE           NOT NULL,
    insured_document_number VARCHAR(64)    NOT NULL,
    coverage_amount         NUMERIC(15, 2) NOT NULL,
    premium                 NUMERIC(15, 2) NOT NULL,
    start_date              DATE           NOT NULL,
    end_date                DATE           NOT NULL,
    issued_at               TIMESTAMPTZ    NOT NULL,
    issued_by_subject       VARCHAR(64)    NOT NULL,
    CONSTRAINT contract_application_fk FOREIGN KEY (application_id) REFERENCES application (id),
    CONSTRAINT contract_application_unique UNIQUE (application_id),
    CONSTRAINT contract_number_unique UNIQUE (contract_number),
    CONSTRAINT contract_coverage_amount_positive CHECK (coverage_amount > 0),
    CONSTRAINT contract_premium_non_negative CHECK (premium >= 0),
    CONSTRAINT contract_dates_ordered CHECK (end_date >= start_date)
);

CREATE INDEX idx_contract_policyholder_subject ON contract (policyholder_subject);

CREATE TABLE contract_registration (
    id                 UUID        PRIMARY KEY,
    contract_id        UUID        NOT NULL,
    status             VARCHAR(32) NOT NULL,
    attempts           INTEGER     NOT NULL,
    next_attempt_at    TIMESTAMPTZ NOT NULL,
    last_error         TEXT,
    registry_record_id VARCHAR(64),
    registered_at      TIMESTAMPTZ,
    request_id         VARCHAR(64),
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT contract_registration_contract_fk FOREIGN KEY (contract_id) REFERENCES contract (id),
    CONSTRAINT contract_registration_contract_unique UNIQUE (contract_id),
    CONSTRAINT contract_registration_attempts_non_negative CHECK (attempts >= 0),
    CONSTRAINT contract_registration_status_allowed
        CHECK (status IN ('PENDING', 'REGISTERED', 'REJECTED', 'FAILED'))
);

CREATE INDEX idx_contract_registration_pending
    ON contract_registration (next_attempt_at) WHERE status = 'PENDING';

--rollback DROP TABLE contract_registration;
--rollback DROP TABLE contract;
--rollback DROP SEQUENCE contract_number_seq;
