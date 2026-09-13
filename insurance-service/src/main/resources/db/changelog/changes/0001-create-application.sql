--liquibase formatted sql

--changeset dmitrypoverov:0001-create-application
CREATE TABLE application (
                             id                      UUID           PRIMARY KEY,
                             applicant_subject       VARCHAR(64)    NOT NULL,
                             insured_full_name       VARCHAR(255)   NOT NULL,
                             insured_birth_date      DATE           NOT NULL,
                             insured_document_number VARCHAR(64)    NOT NULL,
                             coverage_amount         NUMERIC(15, 2) NOT NULL,
                             term_years              INTEGER        NOT NULL,
                             calculated_premium      NUMERIC(15, 2) NOT NULL,
                             status                  VARCHAR(32)    NOT NULL,
                             created_at              TIMESTAMPTZ    NOT NULL,
                             updated_at              TIMESTAMPTZ    NOT NULL,
                             decided_at              TIMESTAMPTZ,
                             decided_by_subject      VARCHAR(64),
                             rejection_reason        VARCHAR(500),
                             CONSTRAINT application_coverage_amount_positive CHECK (coverage_amount > 0),
                             CONSTRAINT application_premium_non_negative CHECK (calculated_premium >= 0),
                             CONSTRAINT application_term_years_range CHECK (term_years BETWEEN 1 AND 30),
                             CONSTRAINT application_status_allowed
                                 CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'CONTRACT_ISSUED'))
);

CREATE INDEX idx_application_applicant_subject ON application (applicant_subject);

--rollback DROP TABLE application;
