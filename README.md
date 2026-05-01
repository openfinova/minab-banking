# OpenFinova Minab Banking

Open-source core banking platform built with **Spring Boot 4** and **Java 25**.

## Modules

| Module | Description |
|---|---|
| `banking-app` | Main application entry point |
| `common` | Shared libraries, setup API & service |
| `identity` | Authentication & user management |
| `general-ledger` | Chart of accounts & journal entries |
| `exchange-rate` | FX rates & currency management |
| `customer` | Customer profiles & KYC |
| `customer-account` | Account lifecycle & operations |
| `transaction-processing` | Transaction execution & posting |
| `loan` | Loan origination & servicing |

## Quick Start

```bash
mvn clean install
mvn spring-boot:run -pl banking-app
```

## License

[AGPL-3.0](LICENSE)
