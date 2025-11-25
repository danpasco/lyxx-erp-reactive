# Lynx ERP - Multi-Tenant Accounting System

## Overview

Lynx ERP is a modern, multi-tenant ERP system built with Quarkus, focusing on accounting and financial management. The system uses a **schema-per-tenant** architecture with PostgreSQL and implements a novel **two-level chart of accounts** design that solves cross-subsidiary transaction problems inherent in classic accounting systems.

## Technology Stack

- **Framework**: Quarkus 3.28.3
- **Language**: Java 21
- **Database**: PostgreSQL (schema-per-tenant)
- **ORM**: Hibernate ORM with Panache (Active Record pattern)
- **Authentication**: Auth0 (OIDC)
- **Migration**: Flyway
- **API**: REST with Jackson
- **Testing**: JUnit 5, REST Assured

## Architecture Decisions

### Multi-Tenancy: Schema-Per-Tenant

**Decision**: Each tenant gets its own PostgreSQL schema (e.g., `lynx_acme_corp`, `lynx_xyz_inc`)

**Why**:
- Complete data isolation (security, compliance)
- Per-tenant schema evolution
- Simpler queries (no tenant_id in every WHERE clause)
- Better performance (smaller indexes, targeted vacuuming)

**Implementation**:
- `TenantResolver` extracts tenant from request (X-Tenant-ID header, subdomain, or JWT)
- `TenantContext` stores tenant info in thread-local storage
- `SchemaConnectionProvider` switches PostgreSQL search_path per request
- Flyway function `initialize_tenant_schema()` creates all tables for new tenants

### Authentication: Auth0 + User-Tenant Access Control

**Decision**: Auth0 for identity, custom user-tenant access mapping

**Why**:
- Don't build auth ourselves (security nightmare)
- Auth0 handles SSO, MFA, social login
- Custom `UserTenantAccess` table maps users to tenants with roles

**Flow**:
1. User logs in via Auth0 → gets JWT
2. User selects tenant (if they have access to multiple)
3. All API calls include tenant context (header or subdomain)
4. System validates user has access to that tenant

**Entities**:
- `User` - Auth0 user ID + profile info (stored in public schema)
- `Tenant` - Tenant metadata (stored in public schema)
- `UserTenantAccess` - User→Tenant mapping with role (stored in public schema)

### Novel Two-Level Chart of Accounts

**Decision**: Two-level hierarchy instead of unlimited depth or separate subsidiary ledgers

**Problem with Classic Model**:
Classic accounting systems have a G/L + separate subsidiary ledgers (A/R, A/P, Inventory). This creates problems:
- Cross-subsidiary transactions are painful (e.g., netting A/P against A/R for same vendor/customer)
- UI must know which subsidiary to show (complex logic)
- Journal entries require BOTH G/L account AND subsidiary account

**Our Solution: Qualified Account Numbers**
```
LedgerOrganization (e.g., "Acme Corp Books")
  └─ Controlling Account (e.g., "200" = A/R)
      └─ Subsidiary Account (e.g., "1" = Customer "Chris Date")
      
Qualified Account Number: "200.1"
```

**Benefits**:
- Journal entry just needs ONE account number: "200.1"
- Cross-subsidiary transactions work naturally: DR 300.ACME (A/P), CR 200.ACME (A/R)
- No complex UI logic - just enter qualified account number
- Subsidiary details (customer, vendor, item, bank) handled via composition (associative entities)

**Business Rules**:
1. Controlling accounts can have children, cannot have transactions
2. Subsidiary accounts cannot have children, can have transactions
3. Two-level limit enforced at entity level
4. Account numbers unique within (ledger + parent) scope
5. Accounts can be deactivated but not deleted if used in journals

## Core Entities

### Public Schema (Shared)
- **Tenant** - Tenant metadata (identifier, name, schema name)
- **User** - User profile (Auth0 sub, email, name)
- **UserTenantAccess** - User-to-tenant mapping with role

### Tenant Schema (Per-Tenant)

#### Accounting Core
- **LedgerOrganization** - Owns the chart of accounts (owner contact, fiscal year settings)
- **Account** - Two-level hierarchy (controlling → subsidiary)
    - Fields: accountNumber, name, accountCategory, accountCategory, subsidiaryType
    - Computed: qualifiedAccountNumber (e.g., "200.1"), fullPath (e.g., "A/R > Chris Date")
- **AccountType** enum: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
- **AccountCategory** enum: BALANCE_SHEET, OPERATIONAL
- **SubsidiaryType** enum: NONE, CUSTOMER, VENDOR, ITEM, BANK, EMPLOYEE

#### Subsidiary Account Associations (Composition Pattern)
- **CustomerAccount** - Links account to customer contact (credit limit, payment terms)
- **VendorAccount** - Links account to vendor contact (1099 status, payment terms)
- **ItemAccount** - Links account to inventory item (SKU, UOM, standard cost)
- **BankAccount** - Links account to bank details (routing, account number, SWIFT, IBAN)

#### Journals & Ledgers
- **Ledger** - Journal entry container (links to LedgerOrganization)
- **FiscalPeriod** - Year/Quarter/Month periods with status (OPEN, CLOSING, CLOSED)
- **LedgerAccount** - Account opening balances per fiscal period
- **JournalEntry** - Transaction header (date, description, posted status)
- **JournalEntryLine** - Transaction line (account, amount, description)

#### Contacts
- **Contact** (abstract) - Base for Person/Organization
- **Address** - Contact addresses
- **Telephone** - Contact phone numbers

#### Other
- **DisposalTicket** - Oil & gas disposal tracking (domain-specific)

## Key Design Patterns

### 1. Active Record (Panache)
Entities inherit from `PanacheEntity` and have static query methods:
```java
// Find accounts
List<Account> accounts = Account.findControllingAccounts(ledgerOrg);
Optional<Account> account = Account.findByQualifiedNumber(ledgerOrg, "200.1");
```

### 2. Composition Over Inheritance
Subsidiary account details use associative entities instead of inheritance:
```java
// Instead of: CustomerAccount extends Account (inheritance)
// We use: CustomerAccount has Account (composition)
CustomerAccount ca = new CustomerAccount();
ca.account = subsidiaryAccount;
ca.contact = customerContact;
ca.creditLimit = new BigDecimal("10000.00");
```

### 3. Computed Properties
Account hierarchy values computed on-demand, not persisted:
```java
@Transient
public String qualifiedAccountNumber;

public String getQualifiedAccountNumber() {
    if (parentAccount == null) return accountNumber;
    return parentAccount.accountNumber + "." + accountNumber;
}
```

### 4. Jackson Entity References
Avoid object swizzling by using `EntityManager.getReference()`:
```java
@JsonProperty("parentAccountId")
public void setParentAccountId(Long parentId) {
    if (parentId != null) {
        this.parentAccount = Account.getEntityManager()
            .getReference(Account.class, parentId);
    }
}
```

## Database Schema

### Schema Structure
```
public schema:
  - tenant (tenant metadata)
  - user (user profiles)
  - user_tenant_access (access control)
  
lynx_acme_corp schema:
  - ledger_organization
  - account
  - customer_account
  - vendor_account
  - item_account
  - bank_account
  - ledger
  - fiscal_period
  - journal_entry
  - journal_entry_line
  - t_contacts
  - t_addresses
  - Telephone
  - disposal_ticket
```

### Unique Constraints
- Account: `UNIQUE(ledger_organization_id, parent_account_id, account_number)`
- Top-level accounts: Partial unique index `WHERE parent_account_id IS NULL`
- Subsidiary associations: `UNIQUE(account_id)` (1:1 relationship)

## API Structure

All APIs use tenant context via `X-Tenant-ID` header (or subdomain in production):

```bash
# Create controlling account
POST /api/accounts
Headers: X-Tenant-ID: acme-corp
Body: {
  "ledgerOrganizationId": 1,
  "accountNumber": "200",
  "name": "Accounts Receivable",
  "accountCategory": "ASSET",
  "isControllingAccount": true
}

# Create subsidiary account
POST /api/accounts
Headers: X-Tenant-ID: acme-corp
Body: {
  "ledgerOrganizationId": 1,
  "parentAccountId": 5,
  "accountNumber": "1",
  "name": "Chris Date",
  "accountCategory": "ASSET",
  "subsidiaryType": "CUSTOMER"
}

# Find by qualified number
GET /api/accounts/by-qualified-number?ledgerOrganizationId=1&qualifiedNumber=200.1
```

## Configuration

### application.properties (key settings)
```properties
# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/lynx_erp

# Flyway
quarkus.flyway.migrate-at-start=true
quarkus.flyway.locations=db/migration
quarkus.flyway.baseline-on-migrate=true

# Multi-tenancy
quarkus.hibernate-orm.multitenant=SCHEMA
quarkus.hibernate-orm.multitenant-schema-datasource=default

# Auth (Auth0)
quarkus.oidc.auth-server-url=${AUTH0_DOMAIN}
quarkus.oidc.client-id=${AUTH0_CLIENT_ID}
quarkus.oidc.credentials.secret=${AUTH0_CLIENT_SECRET}

# Dev mode - disable auth
%dev.quarkus.oidc.tenant-enabled=false
```

## Development Setup

### Prerequisites
- Java 21+
- Docker (for PostgreSQL)
- Maven 3.8+

### Quick Start
```bash
# Clone repo
git clone <repo-url>
cd lynx-erp

# Start in dev mode (Quarkus Dev Services will start PostgreSQL automatically)
./mvnw quarkus:dev

# Access
http://localhost:8080

# API docs
http://localhost:8080/q/swagger-ui
```

### First-Time Bootstrap
On first startup, the system automatically:
1. Creates `public` schema with tenant/user tables
2. Creates `system` tenant with schema `lynx_system`
3. Runs Flyway migration to create all tenant tables
4. Creates default admin user: `system-admin`

**Important**: Change the `system-admin` credentials immediately!

## Testing

### Test Structure
- **Entity Tests** (e.g., `AccountTest`) - Unit tests for business logic
- **Resource Tests** (e.g., `AccountResourceTest`) - Integration tests for REST APIs

### Running Tests
```bash
# All tests
./mvnw test

# Specific test
./mvnw test -Dtest=AccountTest

# With fresh database
quarkus.datasource.devservices.reuse=false
```

### Test Properties
```properties
# src/test/resources/application.properties
quarkus.flyway.clean-at-start=true
quarkus.datasource.devservices.reuse=false
quarkus.oidc.tenant-enabled=false
```

## Current Status (as of 2025-11-09)

### ✅ Complete
- Multi-tenant infrastructure (schema-per-tenant)
- Auth0 integration framework
- User-tenant access control model
- Two-level account model with all entities
- Subsidiary account associations (Customer, Vendor, Item, Bank)
- Flyway migrations for tenant schema
- Bootstrap process
- REST APIs for all entities
- Core business logic (hierarchy validation, computed properties)

### 🚧 In Progress
- Test fixes (AccountResourceTest needs tenant context setup)
- Bootstrap seed data for chart of accounts template
- Onboarding flow for new tenants

### 📋 TODO
- Complete all unit tests
- Financial reporting roll-ups (soft/dynamic aggregations)
- Document/Register model for transaction context
- Fiscal period management
- Journal posting workflow
- Bank reconciliation
- Multi-currency support

## Key Files

```
src/main/java/io/tahawus/lynx/
├── core/
│   ├── model/
│   │   ├── Tenant.java
│   │   ├── User.java
│   │   └── UserTenantAccess.java
│   ├── tenant/
│   │   ├── TenantResolver.java
│   │   ├── TenantContext.java
│   │   ├── TenantFilter.java
│   │   └── SchemaConnectionProvider.java
│   └── startup/
│       └── SystemBootstrap.java
├── accounting/
│   ├── model/
│   │   ├── LedgerOrganization.java
│   │   ├── Account.java
│   │   ├── CustomerAccount.java
│   │   ├── VendorAccount.java
│   │   ├── ItemAccount.java
│   │   └── BankAccount.java
│   └── api/
│       ├── LedgerOrganizationResource.java
│       ├── AccountResource.java
│       └── [Subsidiary]AccountResource.java
└── contacts/
    └── model/
        └── Contact.java

src/main/resources/db/migration/
├── V0.1.0__public_schema.sql
├── V0.2.0__user_tenant_tables.sql
└── V0.3.0__complete_schema.sql

src/test/java/io/tahawus/lynx/accounting/
├── AccountTest.java (entity unit tests)
└── api/
    └── AccountResourceTest.java (REST integration tests)
```

## Philosophy & Design Principles

1. **Pragmatic over Pure** - One nullable FK is acceptable if it models reality (optional parent)
2. **GAAP Compliance** - Account structure must never change if ledgerJournal activity exists
3. **Audit Trail** - Never delete, only deactivate
4. **Composition** - Prefer associative entities over inheritance
5. **No Swizzling** - Use entity references (`getReference()`) to avoid loading entire object graphs
6. **Compute Don't Cache** - Qualified account numbers computed on-demand, not persisted
7. **Test Independence** - Every test can run alone, no inter-test dependencies

## Contributing

This is a learning/experimental project. Design decisions prioritize:
- Solving real accounting problems (cross-subsidiary transactions)
- Clean relational design (Chris Date would approve!)
- Developer ergonomics (Active Record, simple APIs)

## License

[TBD]

## Authors

Tahawus Development Group
