-- V1__public_schema_setup.sql
-- Public schema tables for multi-tenant management
-- These tables live in the public schema, not per-tenant schemas

-- Tenant registry
CREATE TABLE IF NOT EXISTS tenant (
    id BIGSERIAL PRIMARY KEY,
    tenant_identifier VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    schema_name VARCHAR(63) NOT NULL UNIQUE,
    description VARCHAR(500),
    contact_email VARCHAR(100),
    contact_phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_date TIMESTAMP,
    subscription_plan VARCHAR(50),
    max_users INTEGER,
    notes VARCHAR(1000)
);

-- User-to-tenant access mapping
CREATE TABLE IF NOT EXISTS user_tenant_access (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    role VARCHAR(50) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    granted_by VARCHAR(255),
    expires_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_user_tenant UNIQUE (user_id, tenant_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_tenant_identifier ON tenant(tenant_identifier);
CREATE INDEX IF NOT EXISTS idx_tenant_active ON tenant(is_active);
CREATE INDEX IF NOT EXISTS idx_user_tenant_user ON user_tenant_access(user_id);
CREATE INDEX IF NOT EXISTS idx_user_tenant_tenant ON user_tenant_access(tenant_id);
