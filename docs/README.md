# E-Pager Documentation Index

This folder contains the core project documentation for E-Pager.

Documents:

1. [Solution Architecture Document](01-solution-architecture.md)
2. [Low-Level Design](02-low-level-design.md)
3. [API Documentation](03-api-documentation.md)
4. [Installation and Deployment Guide](04-installation-deployment-guide.md)
5. [User and Admin Guide](05-user-admin-guide.md)
6. [NFR Compliance Report](06-nfr-compliance-report.md)
7. [Future Enhancements Proposal](07-future-enhancements-proposal.md)

Current implementation baseline:

- Java 17
- Spring Boot 3.3.5
- PostgreSQL
- Flyway migrations
- JWT-based API security
- Role-based access control
- HMAC-secured alert ingestion
- Dynatrace gateway
- Grafana and Dynatrace alert adapters
- Firebase Cloud Messaging provider, disabled by default
- Simulated push provider, enabled by default
