# ePager

A Spring Boot MVP for receiving monitoring alerts and escalating them until someone acknowledges the incident.

## What is included

- Grafana webhook endpoint
- Dynatrace webhook endpoint
- Source adapter pattern so new monitoring tools do not touch incident/escalation logic
- Incident lifecycle: `TRIGGERED`, `ACKNOWLEDGED`, `RESOLVED`
- Escalation policies by service name
- Scheduled escalation checks
- Push-first notification dispatcher with pluggable providers
- User device registration for browser/mobile push tokens
- H2 in-memory database for local development
- PostgreSQL profile placeholder for the next phase

## Run

The project is configured for Java 17.

```powershell
mvn spring-boot:run
```

Or package and run the JAR:

```powershell
mvn package -DskipTests
java -jar target/epager-0.0.1-SNAPSHOT.jar
```

The app starts on:

```text
http://localhost:8080
```

H2 console:

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

- `Shivam Engineer`
- `Ravi Lead`
- `Manish Manager`
- Escalation policy for service `payments`

The `payments` policy sends the first alert to Shivam, then escalates to Ravi after 5 minutes, then Manish after 10 more minutes.

Each seeded user also has a demo push device token so alert delivery is logged as a simulated push notification.

## Try a Grafana-style alert

```powershell
Invoke-RestMethod -Method Post http://localhost:8080/api/alerts/grafana `
  -ContentType "application/json" `
  -Body '{
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

GET  /api/users
POST /api/users
GET  /api/users/{userId}/devices
POST /api/users/{userId}/devices

GET  /api/escalation-policies
POST /api/escalation-policies
PUT  /api/escalation-policies/{policyId}

GET  /api/incidents
GET  /api/incidents/{incidentId}
POST /api/incidents/{incidentId}/acknowledge
POST /api/incidents/{incidentId}/resolve
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

The current `PushNotificationProvider` is a simulated provider. It logs the push payload and stores a `notification_logs` entry with:

```text
channel
destination
title
message
deepLink
providerMessageId
delivered
```

The deep link is:

```text
/incidents/{incidentId}
```

When a real web/mobile app is added, the push provider can send that deep link through Firebase Cloud Messaging, Web Push, APNS, or another push service. Clicking the notification should open the app directly on the incident details page.
