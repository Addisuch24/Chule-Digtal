# Software Requirements Specification (SRS)
## for Chule Digital (Family Business Management System)

**Version 1.0 approved**  
**Prepared by:** Addisu Hirbo & Chule Digital Engineering Team  
**Organization:** Chule Digital  
**Date Created:** October 2026  

---

## Table of Contents
- [Table of Contents](#table-of-contents)
- [Revision History](#revision-history)
- [1. Introduction](#1-introduction)
  - [1.1 Purpose](#11-purpose)
  - [1.2 Document Conventions](#12-document-conventions)
  - [1.3 Intended Audience and Reading Suggestions](#13-intended-audience-and-reading-suggestions)
  - [1.4 Product Scope](#14-product-scope)
  - [1.5 References](#15-references)
- [2. Overall Description](#2-overall-description)
  - [2.1 Product Perspective](#21-product-perspective)
  - [2.2 Product Functions](#22-product-functions)
  - [2.3 User Classes and Characteristics](#23-user-classes-and-characteristics)
  - [2.4 Operating Environment](#24-operating-environment)
  - [2.5 Design and Implementation Constraints](#25-design-and-implementation-constraints)
  - [2.6 User Documentation](#26-user-documentation)
  - [2.7 Assumptions and Dependencies](#27-assumptions-and-dependencies)
- [3. External Interface Requirements](#3-external-interface-requirements)
  - [3.1 User Interfaces](#31-user-interfaces)
  - [3.2 Hardware Interfaces](#32-hardware-interfaces)
  - [3.3 Software Interfaces](#33-software-interfaces)
  - [3.4 Communications Interfaces](#34-communications-interfaces)
- [4. System Features](#4-system-features)
  - [4.1 Authentication and Family Member Management](#41-authentication-and-family-member-management)
  - [4.2 Customer Record and Directory Management](#42-customer-record-and-directory-management)
  - [4.3 Customer Notes and Shared Business Notes](#43-customer-notes-and-shared-business-notes)
  - [4.4 Order Lifecycle and Fulfillment Management](#44-order-lifecycle-and-fulfillment-management)
  - [4.5 Financial Movements and Ledger Management](#45-financial-movements-and-ledger-management)
  - [4.6 Real-Time Notification and Event Syncing](#46-real-time-notification-and-event-syncing)
  - [4.7 System Activity Audit Logging](#47-system-activity-audit-logging)
  - [4.8 Business Intelligence and Executive Dashboard](#48-business-intelligence-and-executive-dashboard)
- [5. Other Nonfunctional Requirements](#5-other-nonfunctional-requirements)
  - [5.1 Performance Requirements](#51-performance-requirements)
  - [5.2 Safety Requirements](#52-safety-requirements)
  - [5.3 Security Requirements](#53-security-requirements)
  - [5.4 Software Quality Attributes](#54-software-quality-attributes)
  - [5.5 Business Rules](#55-business-rules)
- [6. Other Requirements](#6-other-requirements)
- [Appendix A: Glossary](#appendix-a-glossary)
- [Appendix B: Analysis Models](#appendix-b-analysis-models)
- [Appendix C: To Be Determined (TBD) List](#appendix-c-to-be-determined-tbd-list)

---

## Revision History

| Name | Date | Reason For Changes | Version |
| :--- | :--- | :--- | :--- |
| Addisu Hirbo | 2026-10-15 | Initial baseline draft based on MVP requirements | 0.1 |
| Chule Team | 2026-10-20 | Added multi-platform (Android Compose + Web React) specifications | 0.8 |
| Addisu Hirbo | 2026-10-23 | Finalized verification workflows, idempotency keys, and audit logging | 1.0 approved |

---

## 1. Introduction

### 1.1 Purpose
This Software Requirements Specification (SRS) document details the complete functional and non-functional specifications for **Chule Digital**, an integrated multi-platform Family Business Management System. It specifies Version 1.0, encompassing the backend REST/WebSocket micro-monolith API, the responsive React web management portal, the native Android mobile application (Jetpack Compose), and the underlying MySQL relational transactional database.

### 1.2 Document Conventions
- This document follows the **Karl E. Wiegers SRS Template (IEEE 830-1998 standard)**.
- Functional requirements are prioritized using the standard MoSCoW method and numerical priority levels:
  - **High**: Essential for system viability and business integrity (MVP).
  - **Medium**: Important operational feature, enhances multi-user trust and usability.
  - **Low**: Convenience feature or future phase roadmap.
- Every individual functional requirement is tagged with a unique, traceable identifier: `REQ-[FeatureAbbreviation]-[Number]`.
- Keyword conventions follow RFC 2119: **MUST**, **MUST NOT**, **REQUIRED**, **SHOULD**, and **MAY**.

### 1.3 Intended Audience and Reading Suggestions
This document is prepared for:
- **Academic and Technical Defense Committees / Evaluators:** Review Sections 1.4, 2, 4, and 5 to verify functional breadth, architecture decisions, and software engineering rigor.
- **Frontend / Mobile Developers:** Focus on Sections 3 (External Interfaces), 4 (Feature Stimulus/Response sequences), and Appendix B (Data models & state flows).
- **Backend / Database Engineers:** Focus on Sections 2.5, 3.3, 4.5 (Idempotency and Ledger), 5.3 (Security), and Appendix B.
- **Business Stakeholders / Family Operators:** Read Sections 1.4, 2.2, 2.3, and 5.5 to understand operational policies, verification gates, and accountability rules.

### 1.4 Product Scope
**Chule Digital** solves the fundamental governance, transparency, and tracking challenges inherent in family-run commercial enterprises. Traditional family businesses frequently suffer from informal oral agreements, unrecorded credit/debit transactions, untracked customer orders, and lack of accountability across siblings and managers. 

Chule Digital centralizes:
1. Complete customer relation profiles with auditable notes.
2. Two-step verification workflows for financial movements (credit and debit) and critical orders.
3. Strict idempotency to eliminate double-billing, duplicate transactions, and network-retry data corruption.
4. Real-time WebSocket notifications synchronized across mobile and web interfaces.
5. An immutable audit trail (`activities` table) capturing every actor, action type, and JSON metadata snapshot.

### 1.5 References
- IEEE Std 830-1998: *IEEE Recommended Practice for Software Requirements Specifications*.
- RFC 7519: *JSON Web Token (JWT) Architecture and Validation*.
- MySQL 8.0 Reference Manual: *InnoDB Transaction Model and Isolation Levels*.
- Kotlin & Android Jetpack Architecture Guides: *Guide to App Architecture (ViewModel, Repository, Compose)*.
- Express.js v5 Security & Best Practices Specification.

---

## 2. Overall Description

### 2.1 Product Perspective
Chule Digital is a complete, self-contained multi-tier enterprise management suite designed specifically for multi-operator family businesses. It replaces fragmented paper notebooks, personal messaging apps (e.g., Telegram/WhatsApp), and informal Excel sheets.

```
       +--------------------+          +--------------------+
       |  Android Mobile    |          |   React Web App    |
       |  (Jetpack Compose) |          |   (Vite + SPA)     |
       +---------+----------+          +---------+----------+
                 |                               |
                 | HTTP/REST (JSON) + Socket.IO  |
                 +---------------+---------------+
                                 |
                     +-----------v-----------+
                     |  Node.js / Express    |
                     |  Backend API Server   |
                     |  (JWT, Helmet, CORS)  |
                     +-----------+-----------+
                                 |
                         Connection Pool (mysql2)
                                 |
                     +-----------v-----------+
                     |  MySQL 8.0 Relational |
                     |  Database (InnoDB)    |
                     +-----------------------+
```

### 2.2 Product Functions
A high-level summary of functions includes:
- **Authentication & Family Security:** Secure login, bcrypt hash verification, JWT-based state-less authorization, active/inactive member status flags.
- **Customer Directory:** Register customer, search by name/phone, view complete historical balance, orders, and customer-specific notes.
- **Verification-Gated Notes:** Add customer notes and shared family business memos with verification status (`NOT_REQUIRED`, `PENDING`, `VERIFIED`, `REJECTED`).
- **Order Lifecycle Management:** Order creation with system-generated unique order numbers, state machine transitions (`DRAFT` $\rightarrow$ `PENDING` $\rightarrow$ `CONFIRMED` $\rightarrow$ `PROCESSING` $\rightarrow$ `READY` $\rightarrow$ `COMPLETED` / `CANCELLED`).
- **Idempotent Financial Ledger:** Double-entry capable debit and credit tracking, balance rollup, idempotency keys, and supervisor verification.
- **Real-time Alerting:** Immediate WebSocket pushes for pending verifications, customer creation, and order status updates.
- **Audit Logging:** System-wide activity recording storing actor ID, entity type, entity ID, and context metadata.
- **Executive Dashboard:** Live metrics displaying total customers, pending approvals, total turnover, and recent team activities.

### 2.3 User Classes and Characteristics
1. **Family Business Owner / Elder / Admin:**
   - *Characteristics:* Primary decision-maker, requires high-level transparency.
   - *Privileges:* Can verify/reject financial movements, verify critical orders and customer notes, view all audit logs, and oversee turnover.
2. **Family Member / Store Operator:**
   - *Characteristics:* High-frequency daily mobile user in physical shop or warehouse.
   - *Privileges:* Records customer interactions, enters orders, posts credit/debit movements, submits notes, and receives real-time operational notifications.
3. **Auditor / Finance Reviewer:**
   - *Characteristics:* Periodic desktop user reviewing daily books and ledger balances.
   - *Privileges:* Read-only inspection of activities, balance verification, filter movements by customer or date range.

### 2.4 Operating Environment
- **Server:** Node.js runtime (v18.x to v22.x LTS), Linux/Windows Server, MySQL 8.0+ RDBMS with InnoDB engine.
- **Web Client:** Modern evergreen web browsers (Google Chrome 110+, Mozilla Firefox 110+, Apple Safari 16+, Microsoft Edge 110+).
- **Mobile Client:** Android OS 7.0 (API level 24 - Nougat) up to Android 15 (API level 35).

### 2.5 Design and Implementation Constraints
- **Relational Integrity:** Strict foreign key constraints with `ON DELETE RESTRICT` on core financial and audit entities to eliminate orphan records and prevent accidental data loss.
- **Single Source of Truth:** Balance calculations are derived from verified `financial_movements` rather than mutable cached counters.
- **Stateless Application Server:** API server maintains no in-memory session states; all authorization is authenticated per request via cryptographic JWT tokens.
- **Offline / Transient Connectivity:** Mobile app must handle unstable cellular connections using OkHttp interceptors and timeout resilience.

### 2.6 User Documentation
- Comprehensive Readme and Quickstart guides in the source repository.
- Swagger / OpenAPI endpoint documentation.
- Integrated UI error messages and tooltips in both Web and Mobile client interfaces.

### 2.7 Assumptions and Dependencies
- Stable Internet or Local Area Network connection between client devices and the central API server.
- Database clock is synchronized via NTP (UTC timestamps used throughout).
- Mobile devices have Google Play Services or standard Android network socket capabilities.

---

## 3. External Interface Requirements

### 3.1 User Interfaces
- **Web UI:** Responsive single-page application (SPA) created in React 19, structured around a unified sidebar layout (`SidebarLayout`), modern color tokens, modal dialogs for verification actions, and dynamic data tables.
- **Mobile UI:** Native Android Jetpack Compose UI adhering to Material 3 design specifications, offering adaptive top app bars, bottom navigation (`Dashboard`, `Customers`, `Orders`, `Finance`, `Notifications`), pull-to-refresh, and status-coded badges (`StatusBadge`).

### 3.2 Hardware Interfaces
- No specialized proprietary hardware interfaces are required.
- Standard Android touch screen, biometric sensors (future optional enhancement for quick-unlock), and standard desktop keyboard/mouse input devices.

### 3.3 Software Interfaces
- **Database Engine:** MySQL Server 8.0 via `mysql2/promise` connection pooling with parametrized SQL to eliminate SQL injection vulnerabilities.
- **Password Hashing:** `bcryptjs` with salt round factor 10.
- **Token Signing:** `jsonwebtoken` utilizing HMAC-SHA256 algorithm.
- **Client Networking:**
  - Android: `Retrofit 2.x` + `OkHttp 3.x` with custom `AuthInterceptor` injecting `Authorization: Bearer <token>`.
  - Web: `Axios` instance configured with base URL and authorization interceptors.

### 3.4 Communications Interfaces
- **HTTP / HTTPS:** RESTful endpoints over TLS 1.3 for all CRUD operations using `application/json`.
- **WebSocket Protocol (WSS):** Persistent bi-directional channel powered by Socket.IO for real-time dispatch of notification events (`notification:new`, `notification:read`).

---

## 4. System Features

### 4.1 Authentication and Family Member Management
#### 4.1.1 Description and Priority
Enables family members to register, log into the system, obtain a secure JWT token, and maintain profile details.  
*Priority:* High (Cost: Low, Risk: Critical).

#### 4.1.2 Stimulus/Response Sequences
1. **User Action:** Submits username and password via Web or Mobile login screen.
2. **System Response:** Validates presence, retrieves record from `family_members`, verifies `password_hash` via bcrypt, checks `is_active == true`, returns signed JWT token and user profile object.
3. **Alternative (Invalid Credentials):** System responds with HTTP 401 Unauthorized (`Invalid credentials`).
4. **Alternative (Inactive User):** System responds with HTTP 403 Forbidden (`Account has been deactivated`).

#### 4.1.3 Functional Requirements
- **REQ-AUTH-1:** The system MUST hash all passwords using bcrypt prior to database insertion.
- **REQ-AUTH-2:** The system MUST reject login attempts for users with `is_active = FALSE`.
- **REQ-AUTH-3:** The system MUST issue a signed JWT token containing `id`, `username`, and `full_name` with a standard expiration window (e.g., 7 days).
- **REQ-AUTH-4:** The client applications MUST store the JWT securely (Android `DataStore/EncryptedSharedPreferences`, Web `localStorage`/in-memory).

### 4.2 Customer Record and Directory Management
#### 4.2.1 Description and Priority
Maintains a clean, centralized directory of business clients, tracking customer metadata and linking transactions.  
*Priority:* High.

#### 4.2.2 Stimulus/Response Sequences
1. **User Action:** Enters customer full name, optional phone number, and initial note.
2. **System Response:** Persists record in `customers` table with `created_by` foreign key set to authenticated user ID, writes activity log entry, and returns created customer details.
3. **Alternative (Duplicate Customer Check):** System alerts user if a customer with the identical name and phone already exists.

#### 4.2.3 Functional Requirements
- **REQ-CUST-1:** The system MUST require a non-empty `full_name` (VARCHAR 150) for every customer.
- **REQ-CUST-2:** The system MUST permit an optional `phone` string (VARCHAR 30) supporting regional and international formatting.
- **REQ-CUST-3:** The system MUST calculate real-time net balance (total credits minus total debits) per customer.
- **REQ-CUST-4:** Deletion of customers with existing financial movements or orders MUST be blocked via foreign key RESTRICT rules.

### 4.3 Customer Notes and Shared Business Notes
#### 4.3.1 Description and Priority
Provides an auditable memo system for customer agreements, credit terms, and shared family announcements.  
*Priority:* Medium.

#### 4.3.2 Stimulus/Response Sequences
1. **User Action:** Submits a customer note requiring verification.
2. **System Response:** Records note with `verification_status = 'PENDING'`, logs activity, and pushes notification to authorized verifiers via WebSocket.
3. **Reviewer Action:** Approves or rejects note with optional `verification_note`.
4. **System Response:** Sets `verification_status = 'VERIFIED' | 'REJECTED'`, sets `verified_by = actorId`, `verified_at = CURRENT_TIMESTAMP`.

#### 4.3.3 Functional Requirements
- **REQ-NOTE-1:** Customer notes MUST support 4 verification states: `NOT_REQUIRED`, `PENDING`, `VERIFIED`, `REJECTED`.
- **REQ-NOTE-2:** Shared business notes MUST be accessible to all active family members.
- **REQ-NOTE-3:** Any verification decision MUST record the reviewer ID and timestamp.

### 4.4 Order Lifecycle and Fulfillment Management
#### 4.4.1 Description and Priority
Tracks the full flow of customer purchase orders from draft to fulfillment.  
*Priority:* High.

#### 4.4.2 Stimulus/Response Sequences
1. **User Action:** Creates an order selecting a customer, optional title, and order description.
2. **System Response:** Generates a unique `order_number` (e.g., `ORD-YYMMDD-XXXX`), sets status to `PENDING`, records `taken_by`.
3. **User Action:** Updates status progressively (`CONFIRMED` $\rightarrow$ `PROCESSING` $\rightarrow$ `READY` $\rightarrow$ `COMPLETED`).
4. **System Response:** Updates status, broadcasts notification to family members, and records audit history.

#### 4.4.3 Functional Requirements
- **REQ-ORD-1:** Each order MUST have a system-guaranteed unique `order_number`.
- **REQ-ORD-2:** The order status MUST strictly adhere to the defined ENUM (`DRAFT`, `PENDING`, `CONFIRMED`, `PROCESSING`, `READY`, `COMPLETED`, `CANCELLED`).
- **REQ-ORD-3:** The system MUST track which family member registered the order (`taken_by`).

### 4.5 Financial Movements and Ledger Management
#### 4.5.1 Description and Priority
Handles all monetary inflows (`CREDIT`) and outflows/debts (`DEBIT`) associated with customers, ensuring zero loss and accountability.  
*Priority:* High (Critical Financial Integrity).

#### 4.5.2 Stimulus/Response Sequences
1. **User Action:** Submits a payment or debt record specifying customer, amount, movement type (`DEBIT`/`CREDIT`), description, and client-generated `idempotency_key`.
2. **System Response (First Request):** Begins database transaction, inserts record with `PENDING` verification status, commits transaction, generates notification and audit log.
3. **System Response (Duplicate Retry):** Detects existing `idempotency_key`, aborts duplicate insert, returns the existing record without double-crediting.
4. **Supervisor Action:** Verifies the movement.
5. **System Response:** Marks record `VERIFIED`, adjusts verified balance calculations.

#### 4.5.3 Functional Requirements
- **REQ-FIN-1:** All monetary amounts MUST be stored as `DECIMAL(15,2)` to prevent floating-point rounding errors.
- **REQ-FIN-2:** The system MUST enforce a UNIQUE constraint on `idempotency_key` across financial records.
- **REQ-FIN-3:** Financial movements MUST be categorized as either `DEBIT` (money owed / goods taken) or `CREDIT` (cash paid / settled).
- **REQ-FIN-4:** System balance summaries MUST differentiate between verified balance and unverified/pending movements.

### 4.6 Real-Time Notification and Event Syncing
#### 4.6.1 Description and Priority
Dispatches instant updates to active mobile and web clients when significant events occur.  
*Priority:* Medium.

#### 4.6.2 Stimulus/Response Sequences
1. **System Event:** An action (e.g., order created, payment awaiting verification) triggers `notificationService.createNotification()`.
2. **System Response:** Persists notification in `notifications` table, then emits `notification:new` event over Socket.IO room.
3. **Client Response:** Android and Web clients dynamically increment unread counter badge and display banner/in-app alert without page reload.

#### 4.6.3 Functional Requirements
- **REQ-NOTIF-1:** The system MUST persist every notification in the database with an `is_read` Boolean flag.
- **REQ-NOTIF-2:** The system MUST support individual user marking of notifications as read (`PATCH /api/notifications/:id/read`).
- **REQ-NOTIF-3:** Real-time push MUST fall back gracefully to REST polling if WebSocket disconnects.

### 4.7 System Activity Audit Logging
#### 4.7.1 Description and Priority
Maintains a tamper-evident event log recording what every family member did across the business.  
*Priority:* High.

#### 4.7.2 Functional Requirements
- **REQ-AUD-1:** The system MUST record `actor_id`, `activity_type`, `entity_type`, `entity_id`, and `metadata` (JSON) for every mutating operation.
- **REQ-AUD-2:** Activity records MUST NOT be editable or deletable via standard API endpoints.

### 4.8 Business Intelligence and Executive Dashboard
#### 4.8.1 Description and Priority
Aggregates key performance metrics for rapid operational awareness.  
*Priority:* Medium.

#### 4.8.2 Functional Requirements
- **REQ-DASH-1:** The dashboard endpoint MUST provide counts of total customers, total orders, pending verifications, and financial turnover totals in a single request (`GET /api/dashboard/stats`).

---

## 5. Other Nonfunctional Requirements

### 5.1 Performance Requirements
- **PERF-1 (API Latency):** Standard CRUD endpoints MUST respond in under 200 ms under a concurrent load of 50 active family members.
- **PERF-2 (Database Indexing):** All foreign keys, customer names, phone numbers, and idempotency keys MUST have dedicated B-Tree indices to ensure $O(\log n)$ lookup performance.
- **PERF-3 (Network Efficiency):** API responses MUST utilize GZIP compression (`compression` middleware) reducing mobile bandwidth usage by over 60%.

### 5.2 Safety Requirements
- **SAFE-1 (Financial Loss Prevention):** The database MUST enforce foreign key cascade protections: no financial entry can be purged without supervisor authorization.
- **SAFE-2 (Backup & Disaster Recovery):** The system MUST support scheduled logical database dumps (`mysqldump`) and point-in-time recovery via binary logs.

### 5.3 Security Requirements
- **SEC-1 (Password Hashing):** Passwords MUST be hashed using bcrypt with minimum cost factor 10. Cleartext passwords MUST NEVER be logged or returned in responses.
- **SEC-2 (HTTP Hardening):** The backend MUST employ HTTP security headers via `helmet()` (CSP, HSTS, X-Frame-Options, X-Content-Type-Options).
- **SEC-3 (Cross-Origin Resource Sharing):** CORS policies MUST explicitly restrict API access to authorized frontend origins.
- **SEC-4 (Injection Prevention):** All SQL interactions MUST utilize parametrized queries with prepared statements through `mysql2/promise`.

### 5.4 Software Quality Attributes
- **Maintainability:** Layered architectural pattern (Routes $\rightarrow$ Controllers $\rightarrow$ Services $\rightarrow$ Models) cleanly decoupling HTTP parsing from business logic and database execution.
- **Reliability:** Graceful error handling middleware returning standardized JSON responses:
  ```json
  { "success": false, "message": "Descriptive error message", "error": null }
  ```
- **Portability:** Container-ready Node.js backend; Android app adheres to Google Material Design Guidelines; Web app is responsive across mobile, tablet, and 4K desktop screens.

### 5.5 Business Rules
- **BR-1 (Dual-Control Verification):** An operator cannot verify their own financial movement unless explicitly configured as a system super-admin.
- **BR-2 (Customer Accountability):** Every customer record MUST be tied to the specific family member who introduced and registered them (`created_by`).
- **BR-3 (Idempotency Expiry):** Unique idempotency keys MUST be retained permanently to guarantee idempotent re-submissions never create duplicate debits or credits.

---

## 6. Other Requirements
- **Database Schema Migrations:** All database structural changes must be managed via versioned SQL migration scripts located in `database/migrations/`.
- **Diagnostic Logging:** Backend diagnostic logger MUST automatically redact sensitive fields (`password`, `authorization` headers) before writing to server logs.

---

## Appendix A: Glossary

| Term | Definition |
| :--- | :--- |
| **Bcrypt** | A password hashing function designed by Niels Provos and David Mazières based on the Blowfish cipher. |
| **Credit** | Money paid by a customer into the business, reducing outstanding debt or establishing a surplus balance. |
| **Debit** | Goods or services provided to a customer on credit, increasing their liability to the business. |
| **Idempotency** | The property of certain operations in computer science whereby they can be applied multiple times without changing the result beyond the initial application. |
| **Jetpack Compose**| Android’s modern, declarative UI toolkit built on Kotlin. |
| **JWT** | JSON Web Token, an open industry standard (RFC 7519) method for securely representing claims between two parties. |
| **MVVM** | Model-View-ViewModel architectural design pattern widely adopted in Android development. |
| **Socket.IO** | A library that enables low-latency, bi-directional, and event-based communication between a client and a server. |

---

## Appendix B: Analysis Models

### Entity-Relationship Diagram (ERD Summary)
```
+------------------+         +------------------+         +----------------------+
|  family_members  | 1     * |    customers     | 1     * | financial_movements  |
+------------------+---------+------------------+---------+----------------------+
| id (PK)          |         | id (PK)          |         | id (PK)              |
| full_name        |         | full_name        |         | customer_id (FK)     |
| username (UQ)    |         | phone            |         | amount DECIMAL(15,2) |
| password_hash    |         | created_by (FK)  |         | movement_type ENUM   |
| is_active        |         +--------+---------+         | idempotency_key (UQ) |
+--------+---------+                  |                   | verification_status  |
         | 1                          | 1                 | recorded_by (FK)     |
         |                            |                   +----------------------+
         | *                          | *
+--------v---------+         +--------v---------+
|  customer_notes  |         |      orders      |
+------------------+         +------------------+
| id (PK)          |         | id (PK)          |
| customer_id (FK) |         | customer_id (FK) |
| note_text        |         | order_number(UQ) |
| verification_st  |         | order_status ENUM|
| created_by (FK)  |         | taken_by (FK)    |
+------------------+         +------------------+
```

### Order Status State-Transition Model
```
[DRAFT] --> [PENDING] --> [CONFIRMED] --> [PROCESSING] --> [READY] --> [COMPLETED]
               \              \                \
                +--------------+----------------+--> [CANCELLED]
```

---

## Appendix C: To Be Determined (TBD) List

| Item No. | Description | Target Phase | Status |
| :--- | :--- | :--- | :--- |
| **TBD-1** | Integration with Ethiopian Telebirr / CBE Birr Payment Webhook API | Phase 2 | Planned |
| **TBD-2** | SMS gateway integration for automated customer receipts | Phase 2 | Pending Provider API key |
| **TBD-3** | Offline SQLite Room database sync for Android client | Phase 3 | Architecture Designed |
