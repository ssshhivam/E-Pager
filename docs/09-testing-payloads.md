# E-Pager Testing Payloads

Use this file while manually testing from Swagger.

Swagger URL:

```text
http://localhost:8080/swagger-ui/index.html
```

Base URL:

```text
http://localhost:8080
```

## 1. Seeded Users

```text
ADMIN    admin@epager.local              password
ENGINEER shivam.engineer@example.com     password
MANAGER  ravi.lead@example.com           password
MANAGER  manish.manager@example.com      password
```

## 2. Auth Payloads

### 2.1 Admin Login

Endpoint:

```text
POST /api/auth/login
```

Body:

```json
{
  "email": "admin@epager.local",
  "password": "password"
}
```

Expected:

```text
200 OK
Copy accessToken for Swagger Authorize
Copy refreshToken for refresh testing
```

### 2.2 Engineer Login

```json
{
  "email": "shivam.engineer@example.com",
  "password": "password"
}
```

Use this to verify engineer-only assigned incident behavior.

### 2.3 Manager Login

```json
{
  "email": "ravi.lead@example.com",
  "password": "password"
}
```

Use this to verify manager permissions.

### 2.4 Refresh Token

Endpoint:

```text
POST /api/auth/refresh
```

Body:

```json
{
  "refreshToken": "paste-refresh-token-from-login-response"
}
```

Expected:

```text
200 OK
Returns new accessToken and new refreshToken
Old refreshToken must not work again
```

### 2.5 Change Password

Endpoint:

```text
POST /api/auth/change-password
```

Header:

```text
Authorization: Bearer <accessToken>
```

Body:

```json
{
  "currentPassword": "password",
  "newPassword": "password123"
}
```

Expected:

```text
200 OK
Old refresh tokens for this user become invalid
```

Important:

For seeded demo users, change the password back if you are using the same local database for repeated demos.

## 3. Admin Configuration Payloads

Use admin token for this section.

### 3.1 Create User

Endpoint:

```text
POST /api/users
```

Body:

```json
{
  "name": "Test Engineer",
  "email": "test.engineer@example.com",
  "phoneNumber": "+919999999999",
  "password": "password123",
  "role": "ENGINEER"
}
```

Expected:

```text
200 OK
User is created
```

Negative test:

```json
{
  "name": "No Password User",
  "email": "no.password@example.com",
  "phoneNumber": "+919999999998",
  "role": "ENGINEER"
}
```

Expected:

```text
400 Bad Request
```

### 3.2 Create Project

Endpoint:

```text
POST /api/projects
```

Body:

```json
{
  "projectKey": "orders",
  "name": "Orders Project",
  "description": "Order service alert testing",
  "active": true
}
```

### 3.3 Create Support Group

Endpoint:

```text
POST /api/projects/{projectId}/groups
```

Replace `{projectId}` with the ID returned by `GET /api/projects`.

Body:

```json
{
  "groupKey": "orders-primary",
  "name": "Orders Primary Support",
  "active": true
}
```

### 3.4 Add Group Member

Endpoint:

```text
POST /api/projects/groups/{groupId}/members
```

Replace `{groupId}` with the support group ID.

Body:

```json
{
  "userId": 1
}
```

### 3.5 Create Or Update Webhook Source

Endpoint:

```text
POST /api/webhooks/sources
```

Body:

```json
{
  "sourceName": "grafana",
  "secretToken": "demo-webhook-token",
  "description": "Grafana webhook source for manual testing",
  "enabled": true
}
```

Disable source negative test:

```json
{
  "sourceName": "grafana",
  "secretToken": "demo-webhook-token",
  "description": "Grafana disabled for negative testing",
  "enabled": false
}
```

Expected direct alert result after disabling:

```text
401 Unauthorized
```

Remember to enable it again after the negative test.

### 3.6 Create Escalation Policy

Endpoint:

```text
POST /api/escalation-policies
```

Body using seeded users:

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
    },
    {
      "levelNumber": 3,
      "userId": 3,
      "waitMinutes": 15
    }
  ]
}
```

Note:

Verify user IDs from `GET /api/users` before using this payload. Seed IDs can differ if the database was recreated or data was added.

## 4. Alert Payloads

### 4.0 Mock Monitoring Trigger APIs

These endpoints are for manual demos and testing. You hit E-Pager directly, and E-Pager generates a realistic critical monitoring payload, processes it through the same adapter/incident/escalation/notification flow, and returns both the payload and created incident.

Use admin or manager token.

Dynatrace simulation:

```text
POST /api/testing/alerts/dynatrace/critical
```

Grafana simulation:

```text
POST /api/testing/alerts/grafana/critical
```

Body:

```json
{}
```

Expected:

```text
202 Accepted
Response contains payload and incident
Incident status is TRIGGERED
Assigned user is Shivam Engineer
Notification log is created
```

Example response shape:

```json
{
  "source": "dynatrace",
  "severity": "critical",
  "simulatedExternalPostTarget": "/gateway/webhooks/dynatrace",
  "payload": {
    "problemId": "DT-SIM-123456789",
    "problemTitle": "Payments service failure rate is critical",
    "problemImpact": "SERVICE",
    "severityLevel": "critical",
    "state": "OPEN",
    "impactedEntity": "payments",
    "tags": {
      "projectKey": "payments",
      "groupKey": "primary-support",
      "service": "payments",
      "severity": "critical"
    }
  },
  "incident": {
    "id": 10,
    "source": "DYNATRACE",
    "status": "TRIGGERED",
    "assignedUserName": "Shivam Engineer"
  }
}
```

### 4.1 Direct Grafana Alert Body

Endpoint:

```text
POST /api/alerts/grafana
```

Required headers:

```text
Content-Type: application/json
X-EPAGER-TIMESTAMP: <current-utc-timestamp>
X-EPAGER-SIGNATURE: sha256=<hmac>
```

Body:

```json
{
  "title": "High CPU on payments",
  "message": "CPU usage crossed 90%",
  "commonLabels": {
    "projectKey": "payments",
    "groupKey": "primary-support",
    "service": "payments",
    "severity": "critical",
    "alertname": "HighCPU"
  },
  "alerts": [
    {
      "fingerprint": "grafana-manual-payments-cpu-001",
      "labels": {
        "projectKey": "payments",
        "groupKey": "primary-support",
        "service": "payments",
        "severity": "critical"
      },
      "annotations": {
        "summary": "High CPU on payments",
        "description": "CPU usage crossed 90% for five minutes"
      }
    }
  ]
}
```

Expected:

```text
202 Accepted
Incident is created or updated
```

### 4.2 Direct Dynatrace Alert Body

Endpoint:

```text
POST /api/alerts/dynatrace
```

Required headers:

```text
Content-Type: application/json
X-EPAGER-TIMESTAMP: <current-utc-timestamp>
X-EPAGER-SIGNATURE: sha256=<hmac>
```

Body:

```json
{
  "problemId": "P-MANUAL-1001",
  "problemTitle": "Service failure rate increased",
  "problemImpact": "SERVICE",
  "state": "OPEN",
  "impactedEntity": "payments",
  "tags": {
    "projectKey": "payments",
    "groupKey": "primary-support",
    "service": "payments",
    "severity": "critical"
  }
}
```

Expected:

```text
202 Accepted
Incident is created or updated
```

### 4.3 Dynatrace Gateway Payload

This is easiest to test manually in Swagger because the gateway calculates E-Pager HMAC internally.

Endpoint:

```text
POST /gateway/webhooks/dynatrace
```

Header:

```text
Authorization: Bearer change-this-dynatrace-gateway-token
Content-Type: application/json
```

Body:

```json
{
  "problemId": "P-GATEWAY-1001",
  "problemTitle": "Payments service is unavailable",
  "problemImpact": "SERVICE",
  "state": "OPEN",
  "impactedEntity": "payments",
  "tags": {
    "projectKey": "payments",
    "groupKey": "primary-support",
    "service": "payments",
    "severity": "critical"
  }
}
```

Expected:

```text
200 OK or 202 Accepted based on downstream response
Incident exists in GET /api/incidents
Webhook audit exists in GET /api/webhooks/audit
```

### 4.4 HMAC Generation Script For Direct Alert APIs

Swagger cannot calculate HMAC dynamically. Use this PowerShell script when testing `/api/alerts/{source}` directly.

```powershell
$body = '{
  "title": "High CPU on payments",
  "message": "CPU usage crossed 90%",
  "commonLabels": {
    "projectKey": "payments",
    "groupKey": "primary-support",
    "service": "payments",
    "severity": "critical",
    "alertname": "HighCPU"
  },
  "alerts": [
    {
      "fingerprint": "grafana-manual-payments-cpu-001",
      "labels": {
        "projectKey": "payments",
        "groupKey": "primary-support",
        "service": "payments",
        "severity": "critical"
      },
      "annotations": {
        "summary": "High CPU on payments",
        "description": "CPU usage crossed 90% for five minutes"
      }
    }
  ]
}'

$timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
$secret = "demo-webhook-token"
$message = "$timestamp`:$body"
$hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($secret))
$signature = "sha256=" + [Convert]::ToHexString($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($message))).ToLower()

Invoke-RestMethod -Method Post "http://localhost:8080/api/alerts/grafana" `
  -Headers @{
    "X-EPAGER-TIMESTAMP" = $timestamp
    "X-EPAGER-SIGNATURE" = $signature
  } `
  -ContentType "application/json" `
  -Body $body
```

## 5. Incident Payloads

### 5.1 Acknowledge As Engineer

Endpoint:

```text
POST /api/incidents/{incidentId}/acknowledge
```

Use engineer token.

Body:

```json
{}
```

Expected:

```text
200 OK
Status becomes ACKNOWLEDGED
Acknowledged user is current authenticated engineer
```

### 5.2 Acknowledge As Admin Or Manager

Endpoint:

```text
POST /api/incidents/{incidentId}/acknowledge
```

Body:

```json
{
  "userId": 1
}
```

Expected:

```text
200 OK
```

### 5.3 Resolve Incident

Endpoint:

```text
POST /api/incidents/{incidentId}/resolve
```

Body:

```json
{}
```

Expected:

```text
200 OK
Status becomes RESOLVED
```

## 6. Notification Payloads

### 6.1 Register Device

Endpoint:

```text
POST /api/users/{userId}/devices
```

Body:

```json
{
  "platform": "WEB",
  "pushToken": "manual-test-fcm-or-browser-token",
  "deviceName": "Chrome on Windows"
}
```

Allowed platform values:

```text
WEB
ANDROID
IOS
DESKTOP
```

### 6.2 Mark Notification Received

Endpoint:

```text
POST /api/notifications/{notificationId}/received
```

Body:

```json
{
  "clientInfo": "Chrome on Windows manual test"
}
```

Expected:

```text
200 OK
Notification status becomes RECEIVED
Delivery event is created
```

### 6.3 Mark Notification Seen

Endpoint:

```text
POST /api/notifications/{notificationId}/seen
```

Body:

```json
{
  "clientInfo": "Opened from browser notification"
}
```

Expected:

```text
200 OK
Notification status becomes SEEN
Delivery event is created
```

## 7. Negative Security Payloads

### 7.1 Login With Wrong Password

```json
{
  "email": "admin@epager.local",
  "password": "wrong-password"
}
```

Expected:

```text
403 Forbidden
```

### 7.2 Refresh With Reused Token

Use a refresh token once successfully, then send the same token again:

```json
{
  "refreshToken": "old-refresh-token-used-before"
}
```

Expected:

```text
401 Unauthorized
```

### 7.3 Missing HMAC Headers

Endpoint:

```text
POST /api/alerts/grafana
```

Body:

```json
{
  "title": "Missing HMAC test",
  "message": "This should be rejected",
  "alerts": []
}
```

Expected:

```text
401 Unauthorized
```

### 7.4 Invalid Gateway Token

Endpoint:

```text
POST /gateway/webhooks/dynatrace
```

Header:

```text
Authorization: Bearer wrong-token
```

Body:

```json
{
  "problemId": "P-BAD-TOKEN",
  "problemTitle": "Should be rejected",
  "state": "OPEN"
}
```

Expected:

```text
401 Unauthorized
```

### 7.5 Engineer Accessing Admin API

Login as:

```text
shivam.engineer@example.com / password
```

Call:

```text
GET /api/users
```

Expected:

```text
403 Forbidden
```

## 8. Recommended Manual Test Order In Swagger

```text
1. POST /api/auth/login as admin
2. Authorize with Bearer access token
3. GET /api/users
4. GET /api/escalation-policies
5. POST /gateway/webhooks/dynatrace
6. GET /api/incidents
7. GET /api/notifications
8. POST /api/notifications/{notificationId}/received
9. POST /api/notifications/{notificationId}/seen
10. GET /api/notifications/{notificationId}/events
11. POST /api/incidents/{incidentId}/acknowledge
12. POST /api/incidents/{incidentId}/resolve
13. Login as engineer and verify role restriction
14. Login as manager and verify manager access
```
