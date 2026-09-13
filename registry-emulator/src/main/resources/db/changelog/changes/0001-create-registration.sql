--liquibase formatted sql

--changeset dmitrypoverov:0001-create-registration
CREATE SEQUENCE registry_record_seq MAXVALUE 999999;

CREATE TABLE registration (
    id                      UUID           PRIMARY KEY,
    record_seq              BIGINT         NOT NULL DEFAULT nextval('registry_record_seq'),
    registry_record_id      VARCHAR(32)    GENERATED ALWAYS AS (
        'GSR-' || extract(year FROM registered_at AT TIME ZONE 'UTC')::int::text
            || '-' || lpad(record_seq::text, 6, '0')
    ) STORED,
    contract_id             UUID           NOT NULL,
    contract_number         VARCHAR(32)    NOT NULL,
    insured_full_name       VARCHAR(255)   NOT NULL,
    insured_birth_date      DATE           NOT NULL,
    insured_document_number VARCHAR(64)    NOT NULL,
    coverage_amount         NUMERIC(15, 2) NOT NULL,
    premium                 NUMERIC(15, 2) NOT NULL,
    start_date              DATE           NOT NULL,
    end_date                DATE           NOT NULL,
    registered_at           TIMESTAMPTZ    NOT NULL,
    CONSTRAINT registration_contract_unique UNIQUE (contract_id),
    CONSTRAINT registration_record_unique UNIQUE (registry_record_id)
);

--rollback DROP TABLE registration;
--rollback DROP SEQUENCE registry_record_seq;
