-- V1__initial_tenant_schema.sql
-- Initial schema setup for tenant databases
-- This runs in each tenant's schema (not public)

-- Business table
CREATE TABLE IF NOT EXISTS business (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    tax_id VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(30),
    website VARCHAR(200),
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(50) DEFAULT 'US',
    base_currency VARCHAR(3) DEFAULT 'USD',
    logo BYTEA,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    modified_at TIMESTAMP,
    modified_by VARCHAR(100)
);

-- Contact table
CREATE TABLE IF NOT EXISTS contact (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id),
    name VARCHAR(200) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(30),
    is_customer BOOLEAN DEFAULT FALSE,
    is_vendor BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    modified_at TIMESTAMP,
    modified_by VARCHAR(100)
);

-- Number sequences
CREATE TABLE IF NOT EXISTS number_sequence (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    sequence_key VARCHAR(50) NOT NULL,
    current_value BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_number_sequence UNIQUE (business_id, sequence_key)
);

-- Account groups
CREATE TABLE IF NOT EXISTS account_group (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    parent_group_id BIGINT REFERENCES account_group(id),
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP,
    CONSTRAINT uk_account_group_code UNIQUE (business_id, code)
);

-- General ledger accounts
CREATE TABLE IF NOT EXISTS general_ledger_account (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id),
    account_group_id BIGINT REFERENCES account_group(id),
    account_code VARCHAR(20) NOT NULL,
    short_code VARCHAR(10),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    account_type VARCHAR(30) NOT NULL,
    normal_balance VARCHAR(10) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP,
    CONSTRAINT uk_gl_account_code UNIQUE (business_id, account_code)
);

-- Fiscal years
CREATE TABLE IF NOT EXISTS fiscal_year (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id),
    year INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP,
    CONSTRAINT uk_fiscal_year UNIQUE (business_id, year)
);

-- Fiscal periods
CREATE TABLE IF NOT EXISTS fiscal_period (
    id BIGSERIAL PRIMARY KEY,
    fiscal_year_id BIGINT NOT NULL REFERENCES fiscal_year(id),
    period_number INTEGER NOT NULL,
    name VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP,
    CONSTRAINT uk_fiscal_period UNIQUE (fiscal_year_id, period_number)
);

-- Journals
CREATE TABLE IF NOT EXISTS journal (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id),
    fiscal_period_id BIGINT NOT NULL REFERENCES fiscal_period(id),
    journal_type VARCHAR(10) NOT NULL,
    entry_date DATE NOT NULL,
    posting_date DATE NOT NULL,
    document_id BIGINT,
    reference VARCHAR(100),
    description VARCHAR(500),
    reverses_journal_id BIGINT REFERENCES journal(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Journal lines
CREATE TABLE IF NOT EXISTS journal_line (
    id BIGSERIAL PRIMARY KEY,
    journal_id BIGINT NOT NULL REFERENCES journal(id),
    line_number INTEGER NOT NULL,
    account_id BIGINT NOT NULL REFERENCES general_ledger_account(id),
    entry_type VARCHAR(10) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    description VARCHAR(200),
    CONSTRAINT uk_journal_line UNIQUE (journal_id, line_number)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_contact_business ON contact(business_id);
CREATE INDEX IF NOT EXISTS idx_account_group_business ON account_group(business_id);
CREATE INDEX IF NOT EXISTS idx_gl_account_business ON general_ledger_account(business_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_year_business ON fiscal_year(business_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_period_year ON fiscal_period(fiscal_year_id);
CREATE INDEX IF NOT EXISTS idx_journal_business ON journal(business_id);
CREATE INDEX IF NOT EXISTS idx_journal_period ON journal(fiscal_period_id);
CREATE INDEX IF NOT EXISTS idx_journal_line_journal ON journal_line(journal_id);
CREATE INDEX IF NOT EXISTS idx_journal_line_account ON journal_line(account_id);
