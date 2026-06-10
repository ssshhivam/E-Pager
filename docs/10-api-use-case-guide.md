# E-Pager API Use-Case Guide

## 1. Purpose

This document explains every E-Pager API grouped by use case. It is designed for Swagger testing, team walkthroughs, and answering questions like:

- Which role can call this API?
- What does this API do?
- What sample payload should I send?
- What happens after the API succeeds?
- What are common negative scenarios?

Swagger URL:

```text
http://localhost:8080/swagger-ui/index.html
```

Base URL:

```text
http://localhost:8080
```

## 2. Authentication Setup For Swagger

Most APIs require a JWT access token.

### Login First

Endpoint:

```text
POST /api/auth/login
```

Payload:

```json
{
  "email": "admin@epager.local",
  "password": "password"
}
```

Expected response:

```json
{
  "tokenType": "Bearer",
  "accessToken": "jwt-access-token",
  "accessTokenExpiresAt": "2026-06-10T10:30:00Z",
  "refreshToken": "opaque-refresh-token",
  "refreshTokenExpiresAt": "2026-06-17T10:30:00",
  "userId": 4,
  "name": "E-Pager Admin",
  "email": "admin@epager.local",
  "role": "ADMIN"
}
```

In Swagger:

```text
Authorize -> Bearer <accessToken>
```

Seeded users:

```text
ADMIN    admin@epager.local              password
ENGINEER shivam.engineer@example.com     password
MANAGER  ravi.lead@example.com           password
MANAGER  manish.manager@example.com      password
```

## 3. Auth APIs

### 3.1 Login

Endpoint:

```text
POST /api/auth/login
```

Access:

```text
Public
```

What it does:

- Validates user email and password.
- Returns a signed JWT access token.
- Returns an opaque refresh token.
- Stores only the refresh token hash in PostgreSQL.

Payload:

```json
{
  "email": "ravi.lead@example.com",
  "password": "password"
}
```

Positive scenario:

```text
200 OK
User receives accessToken and refreshToken.
```

Negative scenarios:

```text
403 Forbidden if password is wrong.
400 Bad Request if email/password is blank.
```

### 3.2 Refresh Token

Endpoint:

```text
POST /api/auth/refresh
```

Access:

```text
Public
```

What it does:

- Accepts a valid refresh token.
- Revokes the old refresh token.
- Issues a new access token and new refresh token.
- Prevents refresh token reuse.

Payload:

```json
{
  "refreshToken": "paste-refresh-token-from-login"
}
```

Positive scenario:

```text
200 OK
New token pair returned.
Old refresh token becomes invalid.
```

Negative scenarios:

```text
401 Unauthorized if refresh token is expired, revoked, reused, or fake.
400 Bad Request if refreshToken is blank.
```

### 3.3 Change Password

Endpoint:

```text
POST /api/auth/change-password
```

Access:

```text
Authenticated user
```

What it does:

- Checks current password.
- Updates password hash.
- Revokes all active refresh tokens for the current user.

Payload:

```json
{
  "currentPassword": "password",
  "newPassword": "password123"
}
```

Positive scenario:

```text
200 OK
Password changed.
Existing refresh tokens are revoked.
```

Negative scenarios:

```text
403 Forbidden if current password is wrong.
400 Bad Request if new password is less than 8 characters.
401/403 if access token is missing or invalid.
```

## 4. User And Device APIs

These APIs are used to manage people who can receive alerts and the devices where push notifications can be sent.

### 4.1 List Users

Endpoint:

```text
GET /api/users
```

Access:

```text
ADMIN
```

What it does:

- Returns all configured E-Pager users.
- Useful before creating escalation policies because policy levels need user IDs.

Positive scenario:

```text
200 OK
Returns admin, engineer, and manager users.
```

Negative scenarios:

```text
403 Forbidden for MANAGER or ENGINEER.
401/403 if token is missing or invalid.
```

### 4.2 Create User

Endpoint:

```text
POST /api/users
```

Access:

```text
ADMIN
```

What it does:

- Creates a user.
- Encodes password using BCrypt.
- Defaults role to ENGINEER if role is not provided.
- Requires password with minimum 8 characters.

Payload:

```json
{
  "name": "New Engineer",
  "email": "new.engineer@example.com",
  "phoneNumber": "+919999999999",
  "password": "password123",
  "role": "ENGINEER"
}
```

Positive scenario:

```text
200 OK
User is created and can be used in support groups and escalation policies.
```

Negative scenarios:

```text
400 Bad Request if name/email/password is missing.
400 Bad Request if password is shorter than 8 characters.
403 Forbidden if caller is not ADMIN.
```

### 4.3 List User Devices

Endpoint:

```text
GET /api/users/{userId}/devices
```

Access:

```text
ADMIN
```

What it does:

- Lists active registered devices for a user.
- Devices hold push tokens used for notifications.

Positive scenario:

```text
200 OK
Returns WEB, ANDROID, IOS, or DESKTOP devices for the user.
```

Negative scenarios:

```text
403 Forbidden if caller is not ADMIN.
404 Not Found if user does not exist, depending on service behavior.
```

### 4.4 Register User Device

Endpoint:

```text
POST /api/users/{userId}/devices
```

Access:

```text
ADMIN
```

What it does:

- Registers a device token for push notifications.
- Required for real Firebase/Web Push/mobile notification delivery.

Payload:

```json
{
  "platform": "WEB",
  "pushToken": "fcm-or-browser-registration-token",
  "deviceName": "Chrome on Windows"
}
```

Allowed platforms:

```text
WEB
ANDROID
IOS
DESKTOP
```

Positive scenario:

```text
200 OK
Device is stored and can receive future notifications.
```

Negative scenarios:

```text
400 Bad Request if platform or pushToken is missing.
403 Forbidden if caller is not ADMIN.
404 Not Found if user does not exist.
```

## 5. Project, Support Group, And Membership APIs

These APIs define the ownership structure for alerts.

### 5.1 List Projects

Endpoint:

```text
GET /api/projects
```

Access:

```text
ADMIN
```

What it does:

- Lists projects such as `payments`.
- Alerts use `projectKey` to find the right support structure.

### 5.2 Create Project

Endpoint:

```text
POST /api/projects
```

Access:

```text
ADMIN
```

Payload:

```json
{
  "projectKey": "orders",
  "name": "Orders Project",
  "description": "Order service alerts",
  "active": true
}
```

Positive scenario:

```text
200 OK
Project is available for support groups and policies.
```

Negative scenarios:

```text
400 Bad Request if projectKey or name is blank.
403 Forbidden if caller is not ADMIN.
```

### 5.3 List Support Groups For Project

Endpoint:

```text
GET /api/projects/{projectId}/groups
```

Access:

```text
ADMIN
```

What it does:

- Lists support groups under a project.

### 5.4 Create Support Group

Endpoint:

```text
POST /api/projects/{projectId}/groups
```

Access:

```text
ADMIN
```

Payload:

```json
{
  "groupKey": "orders-primary",
  "name": "Orders Primary Support",
  "active": true
}
```

Positive scenario:

```text
200 OK
Support group is created under the project.
```

Negative scenarios:

```text
400 Bad Request if groupKey or name is blank.
404 Not Found if projectId does not exist.
403 Forbidden if caller is not ADMIN.
```

### 5.5 List Support Group Members

Endpoint:

```text
GET /api/projects/groups/{groupId}/members
```

Access:

```text
ADMIN
```

What it does:

- Lists users mapped to a support group.

### 5.6 Add Support Group Member

Endpoint:

```text
POST /api/projects/groups/{groupId}/members
```

Access:

```text
ADMIN
```

Payload:

```json
{
  "userId": 1
}
```

Positive scenario:

```text
200 OK
User is mapped to the support group.
```

Negative scenarios:

```text
400 Bad Request if userId is missing.
404 Not Found if user or group does not exist.
403 Forbidden if caller is not ADMIN.
```

## 6. Escalation Policy APIs

Yes, E-Pager can create new escalation policies.

Escalation policies define:

- Which project the policy belongs to.
- Which support group owns the alert.
- Which service name the policy applies to.
- Which user is level 1, level 2, level 3, etc.
- How many minutes to wait before escalating.

### 6.1 List Escalation Policies

Endpoint:

```text
GET /api/escalation-policies
```

Access:

```text
ADMIN, MANAGER
```

What it does:

- Lists all escalation policies.
- Use this to verify current policy configuration.

### 6.2 Create Escalation Policy

Endpoint:

```text
POST /api/escalation-policies
```

Access:

```text
ADMIN, MANAGER
```

Payload:

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

What happens after success:

```text
When an alert arrives with:
projectKey = payments
groupKey = primary-support
serviceName = payments

E-Pager assigns level 1 user first.
If incident remains TRIGGERED after waitMinutes, scheduled escalation moves it to next level.
```

Positive scenario:

```text
200 OK
Policy is created.
Levels are sorted by levelNumber before saving.
```

Negative scenarios:

```text
400 Bad Request if projectKey, groupKey, serviceName, or levels are missing.
400 Bad Request if levelNumber or waitMinutes is less than 1.
404 Not Found if any userId does not exist.
403 Forbidden if caller is ENGINEER.
```

Recommended creation sequence:

```text
1. GET /api/users
2. GET /api/projects
3. GET /api/projects/{projectId}/groups
4. POST /api/projects/groups/{groupId}/members if needed
5. POST /api/escalation-policies
6. POST /api/testing/alerts/{source}/critical to verify assignment
```

### 6.3 Update Escalation Policy

Endpoint:

```text
PUT /api/escalation-policies/{policyId}
```

Access:

```text
ADMIN, MANAGER
```

Payload:

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
      "waitMinutes": 3
    },
    {
      "levelNumber": 2,
      "userId": 2,
      "waitMinutes": 7
    }
  ]
}
```

Positive scenario:

```text
200 OK
Policy is replaced with updated values and levels.
```

Negative scenarios:

```text
404 Not Found if policyId does not exist.
404 Not Found if any userId does not exist.
403 Forbidden if caller is ENGINEER.
```

### 6.4 List Escalation Events

Endpoint:

```text
GET /api/escalation-policies/events
```

Access:

```text
ADMIN, MANAGER
```

What it does:

- Returns recent escalation events from the last 7 days.
- Useful to prove escalation happened.

Positive scenario:

```text
200 OK
Returns event history.
```

## 7. Webhook Source And Audit APIs

Webhook source APIs configure which external alert sources are trusted.

### 7.1 List Webhook Sources

Endpoint:

```text
GET /api/webhooks/sources
```

Access:

```text
ADMIN
```

What it does:

- Lists configured sources like `grafana` and `dynatrace`.
- Shows whether a source is enabled.

### 7.2 Create Or Update Webhook Source

Endpoint:

```text
POST /api/webhooks/sources
```

Access:

```text
ADMIN
```

Payload:

```json
{
  "sourceName": "grafana",
  "secretToken": "demo-webhook-token",
  "description": "Grafana alert source",
  "enabled": true
}
```

What it does:

- Creates a source if it does not exist.
- Updates an existing source if sourceName already exists.
- Secret is used for HMAC verification.

Positive scenario:

```text
200 OK
Source is trusted and enabled.
```

Negative scenarios:

```text
400 Bad Request if sourceName or secretToken is blank.
403 Forbidden if caller is not ADMIN.
401 when future alerts arrive with wrong HMAC secret.
```

### 7.3 List Webhook Audit Logs

Endpoint:

```text
GET /api/webhooks/audit
```

Access:

```text
ADMIN
```

What it does:

- Returns recent webhook audit logs.
- Shows accepted and rejected alert attempts.

Positive scenario:

```text
200 OK
Audit rows show accepted=true for valid alerts.
Audit rows show accepted=false for rejected alerts.
```

## 8. Alert Ingestion APIs

These APIs are the real alert entry points.

### 8.1 Direct Alert API

Endpoint:

```text
POST /api/alerts/{source}
```

Examples:

```text
POST /api/alerts/grafana
POST /api/alerts/dynatrace
```

Access:

```text
Public at Spring Security level
Protected by HMAC
```

Required headers:

```text
X-EPAGER-TIMESTAMP: 2026-06-10T10:30:00Z
X-EPAGER-SIGNATURE: sha256=<hmac>
Content-Type: application/json
```

HMAC formula:

```text
HMAC_SHA256(secret, timestamp + ":" + rawBody)
```

Grafana payload:

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
      "fingerprint": "grafana-payments-cpu-001",
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

Dynatrace payload:

```json
{
  "problemId": "P-12345",
  "problemTitle": "Payments service failure rate increased",
  "problemImpact": "SERVICE",
  "severityLevel": "critical",
  "state": "OPEN",
  "impactedEntity": "payments",
  "problemDetailsText": "Failure rate crossed threshold.",
  "tags": {
    "projectKey": "payments",
    "groupKey": "primary-support",
    "service": "payments",
    "severity": "critical"
  }
}
```

Positive scenario:

```text
202 Accepted
HMAC is valid.
Adapter converts raw JSON to UnifiedAlert.
Incident is created or updated.
Escalation starts.
Notification is created.
```

Negative scenarios:

```text
401 Unauthorized if timestamp is missing, stale, or invalid.
401 Unauthorized if signature is missing or invalid.
401 Unauthorized if source is disabled or not configured.
404 Not Found if no adapter exists for source.
400 Bad Request if JSON is invalid.
```

### 8.2 Dynatrace Gateway API

Endpoint:

```text
POST /gateway/webhooks/dynatrace
```

Access:

```text
Public at Spring Security level
Protected by static Dynatrace gateway bearer token
```

Headers:

```text
Authorization: Bearer change-this-dynatrace-gateway-token
Content-Type: application/json
```

Payload:

```json
{
  "problemId": "P-GATEWAY-1001",
  "problemTitle": "Payments service is unavailable",
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
}
```

What it does:

- Validates Dynatrace gateway bearer token.
- Generates E-Pager HMAC timestamp and signature.
- Forwards raw JSON to `/api/alerts/dynatrace`.
- The normal alert flow then continues.

Positive scenario:

```text
200 OK or 202 Accepted
Alert is forwarded and incident is created.
```

Negative scenarios:

```text
401 Unauthorized if gateway token is missing or invalid.
401 Unauthorized downstream if gateway HMAC secret does not match configured dynatrace source secret.
```

## 9. Alert Simulation APIs

These APIs are for demo and testing. They mock an external monitoring tool inside E-Pager.

### 9.1 Simulate Dynatrace Critical Alert

Endpoint:

```text
POST /api/testing/alerts/dynatrace/critical
```

Access:

```text
ADMIN, MANAGER
```

Payload:

```json
{}
```

What it does:

- Creates a realistic Dynatrace critical alert payload.
- Processes it through the real `DynatraceAlertAdapter`.
- Creates or updates an incident.
- Starts escalation.
- Creates notification logs.

Expected response:

```json
{
  "source": "dynatrace",
  "severity": "critical",
  "simulatedExternalPostTarget": "/gateway/webhooks/dynatrace",
  "payload": {
    "problemId": "DT-SIM-123456789",
    "problemTitle": "Payments service failure rate is critical",
    "severityLevel": "critical"
  },
  "incident": {
    "id": 10,
    "source": "DYNATRACE",
    "status": "TRIGGERED",
    "assignedUserName": "Shivam Engineer"
  }
}
```

### 9.2 Simulate Grafana Critical Alert

Endpoint:

```text
POST /api/testing/alerts/grafana/critical
```

Access:

```text
ADMIN, MANAGER
```

Payload:

```json
{}
```

Positive scenario:

```text
202 Accepted
Returns generated Grafana payload and created incident.
```

Negative scenarios:

```text
403 Forbidden if caller is ENGINEER.
401/403 if token is missing or invalid.
400 Bad Request if unsupported source is used, for example /api/testing/alerts/prometheus/critical.
```

## 10. Incident APIs

Incident APIs are used by admins, managers, and engineers to view and act on incidents.

### 10.1 List Incidents

Endpoint:

```text
GET /api/incidents
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

What it does:

- Admin and manager see all incidents.
- Engineer sees only incidents assigned to that engineer.

Positive scenario:

```text
200 OK
Returns visible incidents.
```

Negative scenarios:

```text
403 if unauthenticated.
Engineer will not see incidents assigned to others.
```

### 10.2 Get Incident By ID

Endpoint:

```text
GET /api/incidents/{incidentId}
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

What it does:

- Returns one incident if visible to current user.

Positive scenario:

```text
200 OK
Incident details returned.
```

Negative scenarios:

```text
403 Forbidden if engineer tries to access unassigned incident.
404 Not Found if incident does not exist.
```

### 10.3 Acknowledge Incident

Endpoint:

```text
POST /api/incidents/{incidentId}/acknowledge
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

Engineer payload:

```json
{}
```

Admin or manager payload:

```json
{
  "userId": 1
}
```

What it does:

- Changes incident status to `ACKNOWLEDGED`.
- Sets acknowledgement user and timestamp.
- Stops further escalation for that incident.

Positive scenario:

```text
200 OK
Incident status becomes ACKNOWLEDGED.
```

Negative scenarios:

```text
400 Bad Request if admin/manager does not send userId.
403 Forbidden if engineer acknowledges incident assigned to someone else.
404 Not Found if incident does not exist.
```

### 10.4 Resolve Incident

Endpoint:

```text
POST /api/incidents/{incidentId}/resolve
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

Payload:

```json
{}
```

What it does:

- Changes incident status to `RESOLVED`.
- Sets resolved timestamp.

Positive scenario:

```text
200 OK
Incident status becomes RESOLVED.
```

Negative scenarios:

```text
403 Forbidden if engineer resolves incident assigned to someone else.
404 Not Found if incident does not exist.
```

## 11. Notification APIs

Notification APIs track whether an alert notification was queued, sent, received, seen, or failed.

### 11.1 List Notifications

Endpoint:

```text
GET /api/notifications
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

What it does:

- Lists notification logs.
- Each log is linked to an incident and recipient.

Positive scenario:

```text
200 OK
Returns notification logs with status, destination, deepLink, timestamps.
```

### 11.2 Mark Notification Received

Endpoint:

```text
POST /api/notifications/{notificationId}/received
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

Payload:

```json
{
  "clientInfo": "Chrome on Windows"
}
```

What it does:

- Marks notification as `RECEIVED`.
- Records a delivery event.
- This should be called by the mobile/web client when the push event reaches the device.

Positive scenario:

```text
200 OK
Notification status becomes RECEIVED.
```

Negative scenarios:

```text
404 Not Found if notificationId does not exist.
```

### 11.3 Mark Notification Seen

Endpoint:

```text
POST /api/notifications/{notificationId}/seen
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

Payload:

```json
{
  "clientInfo": "Opened from browser notification"
}
```

What it does:

- Marks notification as `SEEN`.
- Records a delivery event.
- This should be called when user opens notification or incident deep link.

Positive scenario:

```text
200 OK
Notification status becomes SEEN.
```

### 11.4 List Notification Delivery Events

Endpoint:

```text
GET /api/notifications/{notificationId}/events
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

What it does:

- Returns notification timeline.
- Useful to answer: was it queued, sent, received, seen, or failed?

Positive scenario:

```text
200 OK
Timeline events returned.
```

## 12. Dashboard API

The dashboard API gives one consolidated view for operations.

### 12.1 Get Dashboard

Endpoint:

```text
GET /api/dashboard
```

Access:

```text
ADMIN, MANAGER, ENGINEER
```

What it does:

- Returns incident totals.
- Returns notification delivery totals.
- Returns webhook accepted/rejected counts for the last 24 hours.
- Returns latest 10 incidents.
- Returns latest 10 notifications.

Role behavior:

```text
ADMIN and MANAGER
  See global incident and notification dashboard data.

ENGINEER
  Sees only incidents assigned to that engineer.
  Sees only notifications sent to that engineer.
```

Expected response:

```json
{
  "generatedAt": "2026-06-10T13:45:00",
  "incidents": {
    "total": 12,
    "triggered": 4,
    "acknowledged": 6,
    "resolved": 2,
    "scheduledForEscalation": 4,
    "byStatus": [
      {
        "status": "TRIGGERED",
        "count": 4
      },
      {
        "status": "ACKNOWLEDGED",
        "count": 6
      },
      {
        "status": "RESOLVED",
        "count": 2
      }
    ]
  },
  "notifications": {
    "total": 15,
    "queued": 0,
    "sent": 7,
    "received": 3,
    "seen": 4,
    "failed": 1,
    "byStatus": [
      {
        "status": "SENT",
        "count": 7
      }
    ]
  },
  "webhooks": {
    "last24HoursTotal": 10,
    "last24HoursAccepted": 8,
    "last24HoursRejected": 2
  },
  "recentIncidents": [
    {
      "id": 11,
      "source": "DYNATRACE",
      "severity": "critical",
      "status": "TRIGGERED",
      "assignedUserName": "Shivam Engineer",
      "nextEscalationAt": "2026-06-10T13:50:00"
    }
  ],
  "recentNotifications": [
    {
      "id": 20,
      "incidentId": 11,
      "recipientName": "Shivam Engineer",
      "status": "SENT",
      "deepLink": "/incidents/11?notificationId=20"
    }
  ]
}
```

Positive scenario:

```text
200 OK
Dashboard data is returned for the current user's role.
```

Negative scenarios:

```text
403 Forbidden if token is missing or invalid.
ENGINEER will not see incidents or notifications belonging to other users.
```

Recommended Swagger check:

```text
1. POST /api/testing/alerts/dynatrace/critical
2. GET /api/dashboard
3. Confirm incidents.triggered increased.
4. Confirm recentIncidents[0] is the latest incident.
5. Confirm recentNotifications[0] has status SENT.
```

## 13. Recommended Swagger Demo Sequence

Use this for a clean team demo:

```text
1. POST /api/auth/login as admin
2. Authorize with Bearer accessToken
3. GET /api/users
4. GET /api/projects
5. GET /api/escalation-policies
6. POST /api/testing/alerts/dynatrace/critical
7. Copy incident.id from response
8. GET /api/dashboard
9. GET /api/incidents
10. GET /api/notifications
11. Copy notification.id
12. POST /api/notifications/{notificationId}/received
13. POST /api/notifications/{notificationId}/seen
14. GET /api/notifications/{notificationId}/events
15. POST /api/incidents/{incidentId}/acknowledge
16. POST /api/incidents/{incidentId}/resolve
```

## 14. Recommended New Escalation Policy Demo

Goal:

Create a new policy for a new service.

Sequence:

```text
1. Login as admin.
2. GET /api/users and note user IDs.
3. POST /api/projects if project does not exist.
4. POST /api/projects/{projectId}/groups if group does not exist.
5. POST /api/projects/groups/{groupId}/members for each user.
6. POST /api/escalation-policies with projectKey, groupKey, serviceName, and levels.
7. Send an alert with matching projectKey/groupKey/serviceName.
8. Confirm incident assigned to level 1 user.
```

Sample policy:

```json
{
  "projectKey": "orders",
  "groupKey": "orders-primary",
  "serviceName": "orders-api",
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

Matching Grafana alert labels:

```json
{
  "projectKey": "orders",
  "groupKey": "orders-primary",
  "service": "orders-api",
  "severity": "critical"
}
```

## 15. Role Summary

```text
ADMIN
  Can manage users, projects, webhook sources, escalation policies.
  Can view/handle incidents and notifications.

MANAGER
  Can manage escalation policies.
  Can view/handle incidents and notifications.
  Cannot manage users/projects/webhook sources.

ENGINEER
  Can view, acknowledge, and resolve assigned incidents.
  Can access notification tracking APIs.
  Cannot manage configuration.
```

## 16. Common Status Codes

```text
200 OK
  Normal successful API response.

202 Accepted
  Alert accepted and incident processing started/completed.

400 Bad Request
  Request payload validation failed.

401 Unauthorized
  Webhook/gateway token/HMAC failed, or refresh token invalid.

403 Forbidden
  Authenticated user does not have required role or access.

404 Not Found
  Entity or source adapter not found.
```
