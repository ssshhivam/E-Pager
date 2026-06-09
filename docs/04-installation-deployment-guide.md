# Installation and Deployment Guide

## 1. Purpose

This guide explains how to install, configure, run, test, and deploy E-Pager.

## 2. Prerequisites

Required:

- Java 17
- Maven
- PostgreSQL
- Git

Optional:

- Firebase project and service account JSON
- Public HTTPS domain or tunnel for Dynatrace webhook testing

## 3. Workspace

Current project workspace:

```text
C:\Users\shekh\Documents\Codex\2026-06-08\E-Pager
```

GitHub repository:

```text
https://github.com/ssshhivam/E-Pager
```

## 4. PostgreSQL Setup

Default application database:

```text
database: epager
username: epager
password: epager
```

If using local PostgreSQL with custom password, create or update the database and user using:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f database\postgres-setup.sql
```

If the database user password differs from default, set:

```powershell
$env:EPAGER_DB_PASSWORD="your-password"
```

For the current local machine used during development:

```text
EPAGER_DB_PASSWORD=Shivam@123
```

## 5. Application Configuration

Main config file:

```text
src/main/resources/application.yml
```

Local override file:

```text
application-local.yml
```

This file is ignored by Git and should be used for local secrets.

Example:

```yaml
spring:
  datasource:
    password: your-local-password
```

## 6. Important Environment Variables

### Database

```text
EPAGER_DB_URL
EPAGER_DB_USERNAME
EPAGER_DB_PASSWORD
```

Example:

```powershell
$env:EPAGER_DB_URL="jdbc:postgresql://localhost:5432/epager"
$env:EPAGER_DB_USERNAME="epager"
$env:EPAGER_DB_PASSWORD="Shivam@123"
```

### Dynatrace Gateway

```powershell
$env:EPAGER_GATEWAY_DYNATRACE_TOKEN="dynatrace-secret-token"
$env:EPAGER_GATEWAY_EPAGER_ALERT_URL="http://localhost:8080/api/alerts/dynatrace"
$env:EPAGER_GATEWAY_EPAGER_HMAC_SECRET="demo-webhook-token"
```

### Firebase

```powershell
$env:EPAGER_FIREBASE_ENABLED="true"
$env:EPAGER_FIREBASE_SERVICE_ACCOUNT_PATH="C:\secure\firebase-service-account.json"
$env:EPAGER_FIREBASE_PROJECT_ID="your-firebase-project-id"
```

Firebase is disabled by default.

## 7. Build

Run:

```powershell
mvn clean package
```

Skip tests:

```powershell
mvn clean package -DskipTests
```

Output:

```text
target/epager-0.0.1-SNAPSHOT.jar
```

## 8. Run Locally

Using Maven:

```powershell
mvn spring-boot:run
```

Using JAR:

```powershell
java -jar target/epager-0.0.1-SNAPSHOT.jar
```

Application URL:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

## 9. Test Setup

Integration tests use PostgreSQL.

Create test database:

```powershell
$env:PGPASSWORD="postgres-admin-password"
& "C:\Program Files\PostgreSQL\18\bin\createdb.exe" -U postgres -h localhost -O epager epager_test
```

Run tests:

```powershell
$env:EPAGER_TEST_DB_PASSWORD="your-epager-db-password"
mvn test
```

Current tested command:

```powershell
$env:EPAGER_TEST_DB_PASSWORD="Shivam@123"
$env:EPAGER_FIREBASE_ENABLED="false"
mvn test
```

## 10. Database Migration

Migrations are automatic on startup using Flyway.

Migration folder:

```text
src/main/resources/db/migration
```

Hibernate setting:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Meaning:

- Flyway owns schema changes.
- Hibernate validates mapping against schema.
- Hibernate does not silently alter production schema.

## 11. Deployment Recommendation

### Minimum Deployment

```text
VM / Server
  - Java 17
  - PostgreSQL
  - E-Pager JAR
```

Start command:

```powershell
java -jar target/epager-0.0.1-SNAPSHOT.jar
```

### Production Deployment

Recommended:

```text
HTTPS Load Balancer
  -> E-Pager Spring Boot service
  -> PostgreSQL managed database
  -> Firebase Cloud Messaging
```

Environment should provide:

- database credentials
- JWT secret
- gateway token
- webhook HMAC secrets
- Firebase service account path

## 12. Dynatrace Public URL

Dynatrace cloud cannot call:

```text
http://localhost:8080
```

Use a public HTTPS URL:

```text
https://your-domain.com/gateway/webhooks/dynatrace
```

For temporary local testing:

```text
ngrok -> http://localhost:8080
```

## 13. Firebase Setup

Steps:

1. Create Firebase project.
2. Create service account.
3. Download service account JSON.
4. Store it outside Git.
5. Set `EPAGER_FIREBASE_ENABLED=true`.
6. Set `EPAGER_FIREBASE_SERVICE_ACCOUNT_PATH`.
7. Ensure client app registers FCM token using Firebase SDK.
8. Store that token through `POST /api/users/{userId}/devices`.

## 14. Operational Startup Checklist

Before starting:

- PostgreSQL running.
- Database exists.
- User `epager` can connect.
- Required environment variables set.
- Port 8080 available.
- Firebase disabled unless credentials are ready.

After starting:

- Open Swagger.
- Login as admin.
- Check `/api/users`.
- Check `/api/escalation-policies`.
- Send one test alert.
- Verify incident created.

## 15. Troubleshooting

### Port 8080 Already In Use

Check:

```powershell
Get-NetTCPConnection -LocalPort 8080
```

Stop the owning process or change:

```yaml
server:
  port: 8081
```

### Database Login Fails

Verify:

```powershell
$env:PGPASSWORD="your-password"
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U epager -h localhost -d epager
```

### Flyway Checksum Error

Do not edit old migration files after they have run.

Correct approach:

- Add a new `Vx__description.sql` migration.

### Firebase Startup Fails

Check:

- `EPAGER_FIREBASE_ENABLED` is not accidentally set to `true`.
- service account path exists.
- service account file is valid JSON.
- Firebase project ID is correct.

## 16. Deployment Security Checklist

- Use HTTPS.
- Rotate default seeded passwords.
- Change JWT secret.
- Change webhook HMAC secrets.
- Change Dynatrace gateway token.
- Store Firebase service account securely.
- Do not commit secrets to Git.
- Restrict PostgreSQL network access.
- Enable central logging in production.
