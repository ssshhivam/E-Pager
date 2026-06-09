# ePager

A Spring Boot MVP for receiving monitoring alerts and escalating them until someone acknowledges the incident.

## What is included

- Grafana webhook endpoint
- Dynatrace webhook endpoint
- Source adapter pattern so new monitoring tools do not touch incident/escalation logic
- HMAC webhook signature validation and webhook audit logs
- Incident lifecycle: `TRIGGERED`, `ACKNOWLEDGED`, `RESOLVED`
- Escalation policies by service name
- Scheduled escalation checks
- Push-first notification dispatcher with pluggable providers
- Notification delivery event timeline for queued, sent, received, seen, and failed transitions
- JJWT-based access tokens, refresh-token rotation, password change, and role-based API access
- User device registration for browser/mobile push tokens
- PostgreSQL as the default database
- Flyway migrations for schema management
- H2 in-memory database profile for quick local experiments

## Run

The project is configured for Java 17.

Start PostgreSQL locally:

```powershell
docker compose up -d postgres
```

If you are using your locally installed PostgreSQL service instead of Docker, create/reset the `epager` database and user:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f database\postgres-setup.sql
```

If your PostgreSQL admin user is not `postgres`, replace `-U postgres` with your admin username.

```powershell
mvn spring-boot:run
```

Or package and run the JAR:

```powershell
mvn package -DskipTests
java -jar target/epager-0.0.1-SNAPSHOT.jar
```

Default PostgreSQL connection:

```text
jdbc:postgresql://localhost:5432/epager
username: epager
password: epager
```

For local-only overrides, create `application-local.yml` in the project root. It is ignored by Git:

```yaml
spring:
  datasource:
    password: your-local-password
```

Override with environment variables:

```powershell
$env:EPAGER_DB_URL="jdbc:postgresql://localhost:5432/epager"
$env:EPAGER_DB_USERNAME="epager"
$env:EPAGER_DB_PASSWORD="epager"
mvn spring-boot:run
```

Use the old in-memory H2 profile only for quick experiments:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Schema migrations are managed by Flyway under:

```text
src/main/resources/db/migration
```

Hibernate is configured to validate the schema instead of silently changing it.

## Test

Integration tests use PostgreSQL, not H2. Create a separate test database once:

```powershell
$env:PGPASSWORD="your-postgres-admin-password"
& "C:\Program Files\PostgreSQL\18\bin\createdb.exe" -U postgres -h localhost -O epager epager_test
```

Run tests with the test database password:

```powershell
$env:EPAGER_TEST_DB_PASSWORD="your-epager-db-password"
mvn test
```

The role-based access integration test connects to:

```text
jdbc:postgresql://localhost:5432/epager_test
```

The app starts on:

```text
http://localhost:8080
```

H2 console, only when using the `h2` profile:

```text
http://localhost:8080/h2-console
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

JDBC URL:

```text
jdbc:h2:mem:epager
```

## Seed data

On startup, the app creates:

- `E-Pager Admin`
- `Shivam Engineer`
- `Ravi Lead`
- `Manish Manager`
- Escalation policy for service `payments`
- Webhook sources `grafana` and `dynatrace` with HMAC secret `demo-webhook-token`

The `payments` policy sends the first alert to Shivam, then escalates to Ravi after 5 minutes, then Manish after 10 more minutes.

Each seeded user also has a demo push device token so alert delivery is logged as a simulated push notification.

Seeded login credentials use password `password`:

```text
admin@epager.local          ADMIN
shivam.engineer@example.com ENGINEER
ravi.lead@example.com       MANAGER
manish.manager@example.com  MANAGER
```

## Security and roles

Login returns an access token and refresh token:

```text
POST /api/auth/login
```

```json
{
  "email": "admin@epager.local",
  "password": "password"
}
```

Use the returned token in Swagger's Authorize button or in API headers:

```text
Authorization: Bearer <accessToken>
```

Refresh an expired or near-expired access token:

```text
POST /api/auth/refresh
```

Change the current user's password:

```text
POST /api/auth/change-password
```

Password change revokes active refresh tokens for that user.

Role permissions:

```text
ADMIN    -> manage users, projects, webhook sources, escalation policies
MANAGER  -> manage escalation policies and incidents
ENGINEER -> view, acknowledge, and resolve assigned incidents
```

## Try a Grafana-style alert

```powershell
$body = '{
  "title": "High CPU on payments",
  "message": "CPU usage crossed 90%",
  "commonLabels": {
    "service": "payments",
    "severity": "critical",
    "alertname": "HighCPU"
  },
  "alerts": [
    {
      "fingerprint": "grafana-payments-cpu-001",
      "labels": {
        "service": "payments",
        "severity": "critical"
      },
      "annotations": {
        "summary": "High CPU on payments",
        "description": "CPU usage crossed 90%"
      }
    }
  ]
}'

$timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
$secret = "demo-webhook-token"
$message = "$timestamp`:$body"
$hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($secret))
$signature = "sha256=" + [Convert]::ToHexString($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($message))).ToLower()

Invoke-RestMethod -Method Post http://localhost:8080/api/alerts/grafana `
  -Headers @{
    "X-EPAGER-TIMESTAMP" = $timestamp
    "X-EPAGER-SIGNATURE" = $signature
  } `
  -ContentType "application/json" `
  -Body $body
```

## Dynatrace gateway

For production-style Dynatrace integration, use the gateway endpoint instead of sending Dynatrace directly to `/api/alerts/dynatrace`:

```text
POST /gateway/webhooks/dynatrace
Authorization: Bearer <EPAGER_GATEWAY_DYNATRACE_TOKEN>
Content-Type: application/json
```

Flow:

```text
Dynatrace -> /gateway/webhooks/dynatrace -> /api/alerts/dynatrace -> incident/escalation/notification
```

The gateway validates Dynatrace with a static bearer token, then signs the raw JSON payload using E-Pager's HMAC format:

```text
X-EPAGER-TIMESTAMP: <current UTC timestamp>
X-EPAGER-SIGNATURE: sha256=<HMAC_SHA256(secret, timestamp + ":" + rawBody)>
```

Configure these values with environment variables:

```powershell
$env:EPAGER_GATEWAY_DYNATRACE_TOKEN="token-that-dynatrace-will-send"
$env:EPAGER_GATEWAY_EPAGER_ALERT_URL="http://localhost:8080/api/alerts/dynatrace"
$env:EPAGER_GATEWAY_EPAGER_HMAC_SECRET="demo-webhook-token"
```

In Dynatrace, create a custom problem notification:

```text
Settings Classic -> Integration -> Problem notifications -> Add notification -> Custom integration
```

Use this webhook URL:

```text
https://your-public-epager-host/gateway/webhooks/dynatrace
```

Add headers:

```text
Authorization: Bearer token-that-dynatrace-will-send
Content-Type: application/json
```

Example custom payload:

```json
{
  "problemId": "{ProblemID}",
  "problemTitle": "{ProblemTitle}",
  "problemImpact": "{ProblemImpact}",
  "state": "{State}",
  "impactedEntity": "{ImpactedEntity}",
  "impactedEntities": {ImpactedEntities},
  "problemDetails": {ProblemDetailsJSON},
  "tags": "{Tags}"
}
```

## Add another alert source

Incoming alert sources are intentionally decoupled from incident and escalation handling.

To support another tool, create a Spring component that implements:

```java
public interface AlertSourceAdapter {
    String source();
    UnifiedAlert toUnifiedAlert(JsonNode payload);
}
```

For example, a Prometheus adapter would return `source()` as `prometheus`, then alerts can be posted to:

```text
POST /api/alerts/prometheus
```

The rest of the system still receives only `UnifiedAlert`, so escalation policies, acknowledgement, and resolution do not depend on Grafana, Dynatrace, or any specific monitoring vendor.

## Useful APIs

```text
POST /api/alerts/grafana
POST /api/alerts/dynatrace
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/change-password

Headers required for alert webhooks:
X-EPAGER-TIMESTAMP: current UTC timestamp, for example 2026-06-09T10:30:00Z
X-EPAGER-SIGNATURE: sha256=<HMAC_SHA256(secret, timestamp + ":" + rawBody)>

GET  /api/users
POST /api/users
GET  /api/users/{userId}/devices
POST /api/users/{userId}/devices

GET  /api/projects
POST /api/projects
GET  /api/projects/{projectId}/groups
POST /api/projects/{projectId}/groups
GET  /api/projects/groups/{groupId}/members
POST /api/projects/groups/{groupId}/members

GET  /api/escalation-policies
POST /api/escalation-policies
PUT  /api/escalation-policies/{policyId}
GET  /api/escalation-policies/events

GET  /api/webhooks/sources
POST /api/webhooks/sources
GET  /api/webhooks/audit

GET  /api/incidents
GET  /api/incidents/{incidentId}
POST /api/incidents/{incidentId}/acknowledge
POST /api/incidents/{incidentId}/resolve

GET  /api/notifications
GET  /api/notifications/{notificationId}/events
POST /api/notifications/{notificationId}/received
POST /api/notifications/{notificationId}/seen
```

Acknowledgement body:

```json
{
  "userId": 1
}
```

Register a push device:

```json
{
  "platform": "WEB",
  "pushToken": "browser-or-mobile-push-token",
  "deviceName": "Chrome on laptop"
}
```

## Push notification flow

The escalation engine resolves the next user, then `NotificationService` sends `PUSH` notifications to every active device registered for that user.

By default, `PushNotificationProvider` is a simulated provider. It logs the push payload and stores a `notification_logs` entry with:

```text
channel
status
destination
title
message
deepLink
providerMessageId
delivered
sentAt
receivedAt
seenAt
failedAt
```

The deep link is:

```text
/incidents/{incidentId}?notificationId={notificationId}
```

When a real web/mobile app is added, the push provider can send that deep link through Firebase Cloud Messaging, Web Push, APNS, or another push service. Clicking the notification should open the app directly on the incident details page.

## Firebase Cloud Messaging

E-Pager can send real push notifications through Firebase Cloud Messaging.

Set these environment variables before starting the app:

```powershell
$env:EPAGER_FIREBASE_ENABLED="true"
$env:EPAGER_FIREBASE_SERVICE_ACCOUNT_PATH="C:\secure\firebase-service-account.json"
$env:EPAGER_FIREBASE_PROJECT_ID="your-firebase-project-id"
```

When Firebase is enabled:

```text
NotificationService -> FirebasePushNotificationProvider -> Firebase Cloud Messaging
```

The `pushToken` stored in `user_devices` must be the FCM registration token from the web or mobile client.

The FCM message contains:

```text
notification title/body
notificationLogId
incidentId
userId
severity
deepLink
```

If Firebase is disabled or not configured, E-Pager keeps using the simulated push provider.

Notification status lifecycle:

```text
QUEUED   -> notification row created before provider call
SENT     -> push provider accepted the notification
RECEIVED -> app/browser received the push event and called /received
SEEN     -> user clicked/opened the notification and app called /seen
FAILED   -> provider failed or no active device/provider exists
```

The client app should call:

```text
POST /api/notifications/{notificationId}/received
```

when the push event reaches the device/browser, and:

```text
POST /api/notifications/{notificationId}/seen
```

when the user clicks the notification or opens the incident from it.

Both tracking APIs can optionally include client context:

```json
{
  "clientInfo": "Chrome on Windows"
}
```

Delivery events can be viewed with:

```text
GET /api/notifications/{notificationId}/events
```
