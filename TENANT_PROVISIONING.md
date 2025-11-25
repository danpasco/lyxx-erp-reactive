# Tenant Provisioning

Lynx ERP uses a two-phase tenant onboarding process to separate administrative provisioning from business setup.

## Overview

**Phase 1: Tenant Provisioning (Admin Only)**  
Creates the tenant record and database schema. Restricted to platform administrators.

**Phase 2: Business Setup (Tenant Admin)**  
Authenticated tenant admin completes business configuration (owner, ledger, fiscal periods).

---

## Phase 1: Tenant Provisioning

### Admin API Endpoint

**Endpoint:** `POST /api/tenants/provision`  
**Authentication:** Requires `X-Admin-Token` header  
**Purpose:** Creates tenant infrastructure without requiring full business details

### Request

```bash
curl -X POST http://localhost:8080/api/tenants/provision \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: lynx-admin-secret-2024" \
  -d '{
    "tenantIdentifier": "next-swd",
    "tenantName": "Next SWD, LLC",
    "adminEmail": "admin@nextswdwy.com",
    "description": "SWD Disposal LLC",
    "phone": "307-555-1234"
  }'
```

### Response

```json
{
  "tenantId": 1,
  "tenantIdentifier": "acme-corp",
  "schemaName": "lynx_acme_corp",
  "setupUrl": "/setup?tenant=acme-corp",
  "message": "Tenant provisioned successfully. Admin invitation sent."
}
```

### What Happens

1. Validates tenant identifier is unique
2. Creates tenant record in `public.tenant` table
3. Creates isolated database schema: `lynx_[tenant_identifier]`
4. Runs Flyway migrations in tenant schema to create tables
5. Returns setup URL for admin to complete configuration

### Admin Token

The admin token is currently hardcoded in `TenantResource.java`:

```java
private static final String ADMIN_TOKEN = "lynx-admin-secret-2024";
```

**TODO:** Move to environment variable or configuration file for production deployment.

---

## Phase 2: Business Setup

After provisioning, the tenant admin:

1. Receives invitation email (not yet implemented)
2. Logs in to the application
3. Completes business setup wizard:
    - Owner contact information
    - Ledger configuration
    - Fiscal period initialization

This phase uses the existing `/onboarding` endpoint but requires authentication.

---

## Multi-Tenancy Architecture

### Database Isolation

Each tenant gets a dedicated PostgreSQL schema:

```
public
├── tenant (tenant registry)
├── flyway_schema_history
└── system tables

lynx_acme_corp
├── t_contacts
├── t_ledgers
├── t_fiscal_periods
└── all business tables

lynx_other_corp
├── t_contacts
└── ...
```

### Tenant Context

The `X-Tenant-ID` header routes requests to the correct schema:

```bash
# Request uses tenant schema automatically
curl -H "X-Tenant-ID: acme-corp" http://localhost:8080/api/contacts
```

The `TenantFilter` intercepts requests and sets `search_path` appropriately.

---

## Security Notes

- `/api/tenants/provision` requires admin token - **never expose publicly**
- Regular `/api/tenants` CRUD operations are excluded from tenant filtering (operate on public schema)
- Business data endpoints require both authentication AND tenant context
- Tenant provisioning should be restricted to internal admin tools only

---

## Future Enhancements

- [ ] Move admin token to environment variable
- [ ] Implement email invitation system
- [ ] Add tenant quotas/limits
- [ ] Add tenant suspension/reactivation
- [ ] Support custom subdomain routing (`acme-corp.tahawus.io`)
- [ ] Add audit logging for tenant provisioning
