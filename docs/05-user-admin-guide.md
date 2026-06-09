# User and Admin Guide

## 1. Purpose

This guide explains how different users operate E-Pager.

Roles:

```text
ADMIN
MANAGER
ENGINEER
```

## 2. Seeded Users

| Name | Email | Password | Role |
|---|---|---|---|
| E-Pager Admin | `admin@epager.local` | `password` | ADMIN |
| Shivam Engineer | `shivam.engineer@example.com` | `password` | ENGINEER |
| Ravi Lead | `ravi.lead@example.com` | `password` | MANAGER |
| Manish Manager | `manish.manager@example.com` | `password` | MANAGER |

## 3. Login

Use:

```text
POST /api/auth/login
```

Request:

```json
{
  "email": "admin@epager.local",
  "password": "password"
}
```

Response contains:

```text
accessToken
```

Use token:

```text
Authorization: Bearer <accessToken>
```

## 4. Admin Guide

Admin can manage:

- users
- user devices
- projects
- support groups
- group members
- webhook sources
- escalation policies

### 4.1 Manage Users

List users:

```text
GET /api/users
```

Create user:

```text
POST /api/users
```

Example:

```json
{
  "name": "New Engineer",
  "email": "new.engineer@example.com",
  "phoneNumber": "+911234567890",
  "password": "password",
  "role": "ENGINEER"
}
```

### 4.2 Register User Device

Use this when the user has a browser/mobile/desktop push token.

```text
POST /api/users/{userId}/devices
```

Request:

```json
{
  "platform": "WEB",
  "pushToken": "fcm-registration-token",
  "deviceName": "Chrome on Windows"
}
```

### 4.3 Manage Projects

List:

```text
GET /api/projects
```

Create:

```text
POST /api/projects
```

Request:

```json
{
  "projectKey": "payments",
  "name": "Payments Project",
  "description": "Payment service alerts"
}
```

### 4.4 Manage Support Groups

Create support group under a project:

```text
POST /api/projects/{projectId}/groups
```

Request:

```json
{
  "groupKey": "primary-support",
  "name": "Primary Support"
}
```

Add member:

```text
POST /api/projects/groups/{groupId}/members
```

Request:

```json
{
  "userId": 1
}
```

### 4.5 Manage Webhook Sources

Webhook sources define which external tools are allowed to create incidents.

List:

```text
GET /api/webhooks/sources
```

Create/update:

```text
POST /api/webhooks/sources
```

Request:

```json
{
  "sourceName": "grafana",
  "secretToken": "secure-hmac-secret",
  "description": "Grafana alerts",
  "enabled": true
}
```

### 4.6 Review Webhook Audit

```text
GET /api/webhooks/audit
```

Use this to troubleshoot:

- rejected signatures
- missing timestamps
- unknown sources
- accepted alerts

## 5. Manager Guide

Manager can manage:

- escalation policies
- incidents

Manager cannot manage:

- users
- projects
- webhook sources

### 5.1 View Escalation Policies

```text
GET /api/escalation-policies
```

### 5.2 Create Escalation Policy

```text
POST /api/escalation-policies
```

Example:

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

### 5.3 Update Escalation Policy

```text
PUT /api/escalation-policies/{policyId}
```

### 5.4 View Escalation Events

```text
GET /api/escalation-policies/events
```

### 5.5 Manage Incidents

List incidents:

```text
GET /api/incidents
```

View incident:

```text
GET /api/incidents/{incidentId}
```

Acknowledge incident on behalf of a user:

```text
POST /api/incidents/{incidentId}/acknowledge
```

Request:

```json
{
  "userId": 1
}
```

Resolve:

```text
POST /api/incidents/{incidentId}/resolve
```

## 6. Engineer Guide

Engineer can:

- view assigned incidents
- acknowledge assigned incidents
- resolve assigned incidents
- view notification logs

Engineer cannot:

- view unassigned incidents
- manage users
- manage projects
- manage webhook sources
- manage escalation policies

### 6.1 View Assigned Incidents

```text
GET /api/incidents
```

Only incidents assigned to the current engineer are returned.

### 6.2 View Assigned Incident Detail

```text
GET /api/incidents/{incidentId}
```

If the incident is not assigned to the engineer:

```text
403 Forbidden
```

### 6.3 Acknowledge Incident

```text
POST /api/incidents/{incidentId}/acknowledge
```

Engineer does not need to send `userId`.

The system uses the logged-in engineer as the acknowledging user.

### 6.4 Resolve Incident

```text
POST /api/incidents/{incidentId}/resolve
```

## 7. Notification Guide

### 7.1 View Notifications

```text
GET /api/notifications
```

### 7.2 Mark Received

The client app should call this when the push notification reaches the device.

```text
POST /api/notifications/{notificationId}/received
```

Request:

```json
{
  "clientInfo": "Chrome on Windows"
}
```

### 7.3 Mark Seen

The client app should call this when the user opens the notification.

```text
POST /api/notifications/{notificationId}/seen
```

### 7.4 View Delivery Events

```text
GET /api/notifications/{notificationId}/events
```

## 8. Dynatrace Setup Summary

Use endpoint:

```text
POST /gateway/webhooks/dynatrace
```

Required headers:

```text
Authorization: Bearer <gateway-token>
Content-Type: application/json
```

Detailed setup:

```text
dynatraceConfiguration.txt
```

## 9. Operational Best Practices

- Admin should rotate webhook secrets periodically.
- Manager should verify escalation policy after user/team changes.
- Engineer should acknowledge only after owning the incident.
- Resolved incidents should mean the operational issue is fixed or no longer actionable.
- Delivery status should be used to troubleshoot notification problems.

## 10. Common Problems

### Engineer Cannot See Incident

Cause:

- Incident is assigned to another user.

Fix:

- Manager/admin checks assignment and escalation policy.

### Alert Not Creating Incident

Check:

- webhook source exists
- source is enabled
- HMAC signature is valid
- timestamp is recent
- adapter exists for source

### Notification Not Received

Check:

- user has active device
- token is correct
- Firebase enabled if using real push
- delivery logs show SENT or FAILED

### Dynatrace Test Fails

Check:

- public URL is reachable
- gateway token matches
- E-Pager is running
- gateway HMAC secret matches Dynatrace webhook source secret
