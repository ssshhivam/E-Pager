# API Documentation

## 1. Swagger

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Authentication in Swagger:

1. Call `POST /api/auth/login`.
2. Copy `accessToken`.
3. Click `Authorize`.
4. Enter:

```text
Bearer <accessToken>
```

## 2. Authentication API

### POST `/api/auth/login`

Purpose:

Authenticate an E-Pager user and return a bearer token.

Request:

```json
{
  "email": "admin@epager.local",
  "password": "password"
}
```

Response:

```json
{
  "tokenType": "Bearer",
  "accessToken": "jwt-token",
  "userId": 4,
  "name": "E-Pager Admin",
  "email": "admin@epager.local",
  "role": "ADMIN"
}
```

## 3. Alert Ingestion API

### POST `/api/alerts/{source}`

Purpose:

Receive alerts from monitoring systems.

Current sources:

```text
grafana
dynatrace
```

Headers:

```text
Content-Type: application/json
X-EPAGER-TIMESTAMP: 2026-06-09T10:30:00Z
X-EPAGER-SIGNATURE: sha256=<hmac>
```

Signature:

```text
HMAC_SHA256(secret, timestamp + ":" + rawBody)
```

Response:

Returns `IncidentResponse`.

Status:

```text
202 Accepted
401 Unauthorized if HMAC validation fails
404 Not Found if source adapter is unsupported
```

Example Grafana request body:

```json
{
  "title": "High CPU on payments",
  "message": "CPU usage crossed 90%",
  "alerts": [
    {
      "fingerprint": "grafana-payments-cpu-001",
      "labels": {
        "projectKey": "payments",
        "groupKey": "primary-support",
        "service": "payments",
        "severity": "critical"
      },
      "annotations": {
        "summary": "High CPU on payments",
        "description": "CPU usage crossed 90%"
      }
    }
  ]
}
```

Example Dynatrace request body:

```json
{
  "problemId": "P-12345",
  "problemTitle": "Service failure rate increased",
  "problemImpact": "SERVICE",
  "state": "OPEN",
  "impactedEntity": "payments",
  "tags": {
    "projectKey": "payments",
    "groupKey": "primary-support",
    "service": "payments"
  }
}
```

## 4. Dynatrace Gateway API

### POST `/gateway/webhooks/dynatrace`

Purpose:

Public endpoint for Dynatrace custom problem notifications.

Headers:

```text
Authorization: Bearer <EPAGER_GATEWAY_DYNATRACE_TOKEN>
Content-Type: application/json
```

Behavior:

- Validates Dynatrace bearer token.
- Creates E-Pager HMAC signature.
- Forwards payload to `/api/alerts/dynatrace`.

Status:

```text
200/202 based on downstream E-Pager response
401 if gateway token is missing or invalid
```

## 5. User API

Base path:

```text
/api/users
```

Access:

```text
ADMIN
```

### GET `/api/users`

Returns all users.

Response item:

```json
{
  "id": 1,
  "name": "Shivam Engineer",
  "email": "shivam.engineer@example.com",
  "phoneNumber": "+10000000001",
  "role": "ENGINEER"
}
```

### POST `/api/users`

Creates a user.

Request:

```json
{
  "name": "New Engineer",
  "email": "new.engineer@example.com",
  "phoneNumber": "+911234567890",
  "password": "password",
  "role": "ENGINEER"
}
```

### GET `/api/users/{userId}/devices`

Returns active devices for a user.

### POST `/api/users/{userId}/devices`

Registers a push device.

Request:

```json
{
  "platform": "WEB",
  "pushToken": "fcm-registration-token",
  "deviceName": "Chrome on Windows"
}
```

## 6. Project API

Base path:

```text
/api/projects
```

Access:

```text
ADMIN
```

### GET `/api/projects`

Returns projects.

### POST `/api/projects`

Creates a project.

Request:

```json
{
  "projectKey": "payments",
  "name": "Payments Project",
  "description": "Payment service alerts"
}
```

### GET `/api/projects/{projectId}/groups`

Returns support groups for a project.

### POST `/api/projects/{projectId}/groups`

Creates a support group.

Request:

```json
{
  "groupKey": "primary-support",
  "name": "Primary Support"
}
```

### GET `/api/projects/groups/{groupId}/members`

Returns support group members.

### POST `/api/projects/groups/{groupId}/members`

Adds a user to a support group.

Request:

```json
{
  "userId": 1
}
```

## 7. Webhook Admin API

Base path:

```text
/api/webhooks
```

Access:

```text
ADMIN
```

### GET `/api/webhooks/sources`

Returns configured webhook sources.

### POST `/api/webhooks/sources`

Creates or updates a webhook source.

Request:

```json
{
  "sourceName": "grafana",
  "secretToken": "demo-webhook-token",
  "description": "Grafana webhook source",
  "enabled": true
}
```

### GET `/api/webhooks/audit`

Returns recent webhook audit logs.

## 8. Escalation Policy API

Base path:

```text
/api/escalation-policies
```

Access:

```text
ADMIN, MANAGER
```

### GET `/api/escalation-policies`

Returns escalation policies.

### POST `/api/escalation-policies`

Creates a policy.

Request:

```json
{
  "projectKey": "payments",
  "groupKey": "primary-support",
  "serviceName": "payments",
  "enabled": true,
  "levels": [
    {
      "levelNumber": 1,
      "userId": 1,
      "waitMinutes": 5
    },
    {
      "levelNumber": 2,
      "userId": 2,
      "waitMinutes": 10
    }
  ]
}
```

### PUT `/api/escalation-policies/{policyId}`

Updates a policy.

### GET `/api/escalation-policies/events`

Returns recent escalation events.

## 9. Incident API

Base path:

```text
/api/incidents
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

Role behavior:

- Admin and manager see all incidents.
- Engineer sees only assigned incidents.

### GET `/api/incidents`

Returns visible incidents.

### GET `/api/incidents/{incidentId}`

Returns incident details if visible to current user.

### POST `/api/incidents/{incidentId}/acknowledge`

Acknowledges an incident.

Engineer behavior:

- Request body is optional.
- The authenticated engineer is used as acknowledgement user.

Admin/manager request:

```json
{
  "userId": 1
}
```

### POST `/api/incidents/{incidentId}/resolve`

Resolves an incident.

## 10. Notification API

Base path:

```text
/api/notifications
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

### GET `/api/notifications`

Returns notification logs.

### POST `/api/notifications/{notificationId}/received`

Marks notification as received by client.

Request:

```json
{
  "clientInfo": "Chrome on Windows"
}
```

### POST `/api/notifications/{notificationId}/seen`

Marks notification as opened or seen.

Request:

```json
{
  "clientInfo": "Chrome on Windows"
}
```

### GET `/api/notifications/{notificationId}/events`

Returns notification delivery timeline.

## 11. Common Error Response

Example:

```json
{
  "timestamp": "2026-06-09T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Engineer can access only assigned incidents"
}
```

## 12. Seeded Credentials

```text
admin@epager.local          password  ADMIN
shivam.engineer@example.com password  ENGINEER
ravi.lead@example.com       password  MANAGER
manish.manager@example.com  password  MANAGER
```
