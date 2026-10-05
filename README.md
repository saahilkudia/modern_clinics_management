<div align="center">

# 🏥 Modern Clinics Management System

### Clinic Operations • Patient Management • Financial Accounting • Business Automation

A full-stack clinic management system built around real operational requirements,
combining patient administration, financial workflows, reporting, and clinic
operations in a centralized application.

<br>

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Templates-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

</div>

---

## Overview

**Modern Clinics Management System** is a clinic operations platform developed
around the requirements of a real clinic environment.

The project goes beyond basic patient record management by bringing together
clinical administration and financial operations within one system.

It currently handles areas including:

- Patient records and clinical information
- Doctor management
- Treatments
- Patient transactions
- Expenses
- Chart of Accounts
- Journal vouchers
- Double-entry accounting
- Financial summaries
- Excel import and export
- System maintenance controls

The project continues to evolve as additional operational requirements are
identified.

---

## The Problem

Clinic operations can quickly become fragmented when patient information,
payments, expenses, accounting records, and administrative processes are
managed independently.

That creates problems such as:

- duplicated information
- difficult financial reconciliation
- limited visibility into clinic activity
- manual reporting
- inconsistent records
- difficulty tracing transactions
- administrative overhead

Modern Clinics Management System centralizes these workflows so operational
and financial information can be managed through a single application.

---

# ✨ Implemented Features

## 👤 Patient Management

The system provides centralized patient record management.

Current functionality includes:

- Create patient records
- View registered patients
- Retrieve individual patient information
- Update patient information
- Delete patient records
- Store clinical notes
- Track patient transactions
- Department classification
- Consultant association
- Patient registration / MR numbering

Patient records act as one of the central components connecting clinical and
financial workflows.

---

## 👨‍⚕️ Doctor Management

Doctors can be maintained separately from patient records.

The system supports:

- Doctor records
- Adding doctors
- Retrieving doctors
- Removing doctors
- Associating consultants with clinic records

---

## 🩺 Treatment Management

Treatment information can be maintained through the application and used as
part of clinic workflows.

Supported operations include:

- Create treatments
- Retrieve available treatments
- Delete treatments
- Use treatment information alongside patient operations

---

# 💰 Financial Management

One of the larger parts of the project is its integrated financial management
functionality.

Rather than treating payments as isolated values, the application contains
accounting structures for recording and tracking financial activity.

---

## 📚 Chart of Accounts

The system maintains a structured **Chart of Accounts (COA)** used by the
accounting layer.

Accounts can represent categories such as:

```text
Assets
Expenses
Revenue
Liabilities
Sub-expenses
```

The Chart of Accounts is used by journal entries and other financial operations
throughout the system.

---

## 🧾 Journal Vouchers

Financial activity can be recorded using journal vouchers containing multiple
accounting lines.

Each line records debit or credit movement against an account.

```text
                Journal Voucher

        ┌─────────────────────────┐
        │      Voucher Header     │
        │  Number • Date • Memo   │
        └────────────┬────────────┘
                     │
             ┌───────┴───────┐
             ▼               ▼
      ┌─────────────┐   ┌─────────────┐
      │ Journal Line│   │ Journal Line│
      │    Debit    │   │    Credit   │
      └─────────────┘   └─────────────┘
```

Before a voucher is posted, the accounting engine validates that:

```text
Total Debits = Total Credits
```

A voucher must also contain at least two accounting lines before it can be
posted.

---

## ⚖️ Double-Entry Accounting Engine

The application contains an accounting engine responsible for posting journal
vouchers and updating account balances.

The engine applies normal balance rules based on account type and rejects
unbalanced journal entries.

Conceptually:

```text
Transaction
     │
     ▼
Journal Voucher
     │
     ▼
Validate Debit = Credit
     │
     ▼
Update Chart of Accounts
     │
     ▼
Store Financial Record
```

This provides a structured accounting layer instead of simply storing isolated
payment values.

---

## ↩️ Voucher Reversal

Posted vouchers can be reversed for error correction.

Instead of simply deleting the original accounting record, the system creates
a corresponding reversal entry by switching the debit and credit values.

```text
Original

Cash              Debit   10,000
Revenue            Credit  10,000


Reversal

Revenue            Debit   10,000
Cash               Credit  10,000
```

The original voucher is then marked as reversed.

---

## 💸 Expense Management

The application supports clinic expense workflows including:

- Expense recording
- Direct expense payments
- Accounts Payable handling
- Expense settlement
- Asset / cash account selection
- Accounting entries generated from expense activity

This allows operational expenses to participate in the same accounting system
used by other financial records.

---

## 💳 Patient Transactions

Patient financial activity can be recorded and associated with individual
patient records.

This allows the system to retrieve transaction history for a specific patient
while keeping financial activity connected to the wider accounting workflow.

---

# 📊 Dashboard & Financial Summary

The backend provides summarized operational and financial information,
including:

```text
Total Patients
Dental Patients
Aesthetic Patients

Accounts Receivable
Liquid Reserves
Revenue
Expenses
Net Profit
Cash Position
```

These values are calculated from live application data and the Chart of
Accounts.

---

# 📥 Excel Import

Existing patient information can be imported into the application from Excel.

This is useful when migrating clinic information from spreadsheet-based
workflows into the centralized system.

```text
Existing Spreadsheet
        │
        ▼
   Excel Import
        │
        ▼
Data Processing
        │
        ▼
    Firestore
```

---

# 📤 Excel Reporting

The system can generate an Excel workbook containing clinic information.

The current export includes separate worksheets for:

### Patient Data

```text
Registration Number
Full Name
Phone
Department
Consultant
Balance
```

### Financial Data

```text
Date
Reference Number
Description
Debit
Credit
```

Excel generation is implemented using **Apache POI**.

---

# 🔌 Application Structure

The application currently follows a Spring-based structure:

```text
src/main/
│
├── java/com/modernclinic/patientmanagement/
│   │
│   ├── config/
│   │   ├── FirebaseConfig.java
│   │   ├── MaintenanceInterceptor.java
│   │   └── WebMvcConfig.java
│   │
│   ├── controllers/
│   │   └── ClinicController.java
│   │
│   ├── models/
│   │   ├── Appointment.java
│   │   ├── Doctor.java
│   │   ├── Expense.java
│   │   ├── FinanceModels.java
│   │   ├── Patient.java
│   │   ├── Transaction.java
│   │   ├── Treatment.java
│   │   └── User.java
│   │
│   ├── services/
│   │   ├── AccountingEngine.java
│   │   ├── ExcelExportService.java
│   │   └── Services.java
│   │
│   └── PatientmanagementApplication.java
│
└── resources/
    │
    ├── static/
    │   └── images/
    │
    ├── templates/
    │   ├── index.html
    │   └── maintenance.html
    │
    └── application.properties
```

---

# 🏗️ Architecture

```text
┌───────────────────────────────────────────────┐
│                 Web Interface                 │
│              Thymeleaf / HTML                 │
└──────────────────────┬────────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────────┐
│               Spring MVC Layer                │
│                                               │
│              ClinicController                 │
└──────────────────────┬────────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────────┐
│                Service Layer                  │
│                                               │
│  Services                                     │
│  AccountingEngine                             │
│  ExcelExportService                           │
└──────────────────────┬────────────────────────┘
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
┌──────────────────────┐  ┌─────────────────────┐
│ Firebase / Firestore │  │     Apache POI      │
│                      │  │                     │
│ Application Data     │  │ Excel Import/Export │
└──────────────────────┘  └─────────────────────┘
```

---

# 🛠️ Technology Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot |
| Web Layer | Spring MVC |
| Templates | Thymeleaf |
| Database | Firebase Firestore |
| Excel Processing | Apache POI |
| Build Tool | Maven |
| Dependency Management | Maven |
| Utilities | Lombok |

---

# 🔗 API Structure

The application exposes backend endpoints for its frontend and operational
workflows.

Examples include:

```text
/api/auth/...

/api/patients/...
/api/doctors/...
/api/treatments/...
/api/transactions/...

/api/expenses/...

/api/finance/coa
/api/finance/vouchers
/api/finance/stats

/api/summary
```

These endpoints support the application's patient, administrative, and
financial functionality.

---

# 🚧 In Development

## 📦 Inventory & Purchase Order Management

The next major module being developed for the system focuses on **inventory
control and procurement accountability**.

The requirement emerged from a real operational problem: purchases need a
clear authorization trail so that vendor invoices can be verified against
what the clinic actually requested and approved.

### Planned Workflow

```text
┌─────────────────────┐
│    Staff Request    │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Purchase Order    │
│      Generated      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│      Approval       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Approved PO       │
│   Sent to Vendor    │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Goods Received    │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Vendor Invoice    │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ PO / Invoice /      │
│ Inventory Audit     │
└─────────────────────┘
```

The goal is to establish traceability across:

```text
Request
   ↓
Approval
   ↓
Purchase Order
   ↓
Vendor
   ↓
Goods Received
   ↓
Invoice
   ↓
Verification
```

This will allow clinic management to determine:

- what was requested
- who requested it
- what was approved
- what was ordered
- which vendor received the order
- what inventory was received
- whether the resulting bill matches an authorized purchase

> **Status:** Planned / In Development  
> This module is not represented as a completed feature of the current codebase.

---

# 🔐 Configuration

The application uses Firebase Firestore and therefore requires valid Firebase
credentials.

Firebase service-account credentials are intentionally **not included in this
repository**.

Place the required service-account configuration in the appropriate local
environment before running the application.

Sensitive configuration files such as:

```text
serviceAccountKey.json
.env
```

must never be committed to source control.

---

# 🚀 Running Locally

## Prerequisites

Make sure the following are available:

```text
Java 21+
Firebase project / Firestore
Firebase Admin service account
```

The repository includes the Maven Wrapper, so a separate Maven installation
is not required.

---

## 1. Clone the Repository

```bash
git clone https://github.com/saahilkudia/modern_clinics_management.git
```

```bash
cd modern_clinics_management
```

---

## 2. Configure Firebase

Provide your own Firebase service-account credentials.

The credentials are deliberately excluded from Git using `.gitignore`.

> Never commit Firebase private keys or production credentials.

---

## 3. Run the Application

### Linux / macOS

```bash
./mvnw spring-boot:run
```

### Windows

```powershell
mvnw.cmd spring-boot:run
```

---

## 4. Build

### Linux / macOS

```bash
./mvnw clean package
```

### Windows

```powershell
mvnw.cmd clean package
```

---

# 📸 Screenshots

> Application screenshots will be added here as the repository presentation is
> prepared.

Recommended screenshots:

```text
Dashboard
Patient Management
Patient Profile
Financial Dashboard
Chart of Accounts
Journal Voucher
Expense Management
Excel Import / Export
```

---

# 🗺️ Development Direction

The project is being expanded based on operational requirements rather than a
fixed academic specification.

Current development direction includes:

```text
Clinic Operations
      │
      ├── Patient Management
      ├── Treatment Management
      ├── Financial Management
      │
      ├── Reporting
      │
      └── Inventory & Procurement
              │
              ├── Inventory Tracking
              ├── Purchase Requests
              ├── PO Approval
              ├── Vendor Orders
              └── Invoice Verification
```

---

# 👨‍💻 Developer

**Muhammad Saahil Kudia**

Software Engineer focused on backend systems, business applications, and
workflow automation.

[LinkedIn](https://www.linkedin.com/in/saahilkudia/) •
[GitHub](https://github.com/saahilkudia) •
[SyntaxLoops](https://syntaxloops.com)

---

<div align="center">

### Built around real clinic operations.

`Java` • `Spring Boot` • `Firebase` • `Thymeleaf` • `Apache POI`

</div>