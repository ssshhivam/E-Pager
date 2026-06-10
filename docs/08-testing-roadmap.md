# E-Pager Testing Roadmap

## 1. Goal

Testing must prove that E-Pager can safely receive alerts, create incidents, notify the correct user, escalate correctly, and protect all sensitive operations.

Use this order:

```text
Manual Swagger sanity testing
  -> integration tests for critical flows
  -> service/unit tests for business rules
  -> negative/security tests
  -> regression suite before every commit
```

## 2. Testing Environments

### Local Manual Testing

Use this when learning the flow or demonstrating in Swagger.

```text
App: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui/index.html
Database: PostgreSQL epager
Firebase: disabled unless real FCM is configured
```

### Automated Integration Testing

Use this for real verification.

```text
Database: PostgreSQL epager_test
Test framework: Spring Boot Test + MockMvc
Command: mvn test
```

## 3. Manual Swagger Testing Flow

### Step 1: Login

Call:

```text
POST /api/auth/login
```

Use admin:

```text
admin@epager.local / password
```

Copy `accessToken`, click Swagger `Authorize`, and enter:

```text
Bearer <accessToken>
```

Expected:

```text
200 OK
Response contains accessToken, refreshToken, userId, role
```

### Step 2: Verify Admin Data

Call:

```text
GET /api/users
GET /api/projects
GET /api/escalation-policies
GET /api/webhooks/sources
```

Expected:

```text
200 OK
Seeded users, project, policy, and webhook sources are visible
```

### Step 3: Send Alert

For manual Swagger testing, the easiest source is:

```text
POST /gateway/webhooks/dynatrace
```

This endpoint needs only a gateway bearer token and JSON body. It then creates the HMAC request internally.

For direct source testing:

```text
POST /api/alerts/grafana
POST /api/alerts/dynatrace
```

Direct alert APIs require HMAC headers, so they are better tested using automated integration tests or scripts.

### Step 4: Verify Incident

Call:

```text
GET /api/incidents
```

Expected:

```text
New incident exists
Status is TRIGGERED
Assigned user is escalation level 1 user
```

### Step 5: Verify Notification

Call:

```text
GET /api/notifications
```

Expected:

```text
Notification log exists
Status is SENT if simulated push provider accepted it
Status is FAILED if no active device/provider exists
```

### Step 6: Track Delivery

Call:

```text
POST /api/notifications/{notificationId}/received
POST /api/notifications/{notificationId}/seen
GET  /api/notifications/{notificationId}/events
```

Expected:

```text
Timeline contains QUEUED/SENT, RECEIVED, and SEEN events
```

### Step 7: Acknowledge Incident

Call:

```text
POST /api/incidents/{incidentId}/acknowledge
```

Expected:

```text
Status changes to ACKNOWLEDGED
Escalation should stop for this incident
```

### Step 8: Resolve Incident

Call:

```text
POST /api/incidents/{incidentId}/resolve
```

Expected:

```text
Status changes to RESOLVED
```

## 4. Automated Integration Test Roadmap

### Phase 1: Security and Auth

Already started:

- Admin can access admin APIs.
- Manager can access policies/incidents but not admin APIs.
- Engineer can access only assigned incidents.
- Protected APIs reject unauthenticated calls.
- Login returns access and refresh token.
- Refresh token rotates and old token is rejected.
- Password change revokes refresh tokens.
- User creation requires a password.

Add next:

- Invalid JWT rejected.
- Expired JWT rejected.
- Wrong role cannot acknowledge another engineer's incident.
- Refresh token cannot be reused concurrently.

### Phase 2: Webhook/HMAC Alert Ingestion

Must test:

- Valid Grafana HMAC alert creates incident.
- Valid Dynatrace HMAC alert creates incident.
- Missing signature returns 401.
- Invalid signature returns 401.
- Expired timestamp returns 401.
- Unknown source returns 404.
- Disabled webhook source rejects alert.
- Webhook audit log is created for success and failure.

Why first:

This is the public alert entry point. If this is wrong, fake alerts can enter the system.

### Phase 3: Alert Adapter Mapping

Must test:

- Grafana payload maps to correct `UnifiedAlert`.
- Dynatrace payload maps to correct `UnifiedAlert`.
- Missing optional fields use safe defaults.
- Duplicate external alert ID updates existing active incident instead of creating duplicates.

### Phase 4: Incident Lifecycle

Must test:

- New alert creates `TRIGGERED` incident.
- Existing open alert updates same incident.
- Acknowledge sets status, user, and timestamp.
- Resolve sets status and timestamp.
- Engineer cannot access unassigned incident.
- Admin/manager can view all incidents.

### Phase 5: Escalation

Must test:

- Incident starts at escalation level 1.
- `nextEscalationAt` is calculated from policy.
- Due incident escalates to next level.
- Escalation stops when incident is ACKNOWLEDGED.
- Escalation stops when incident is RESOLVED.
- No escalation happens after last level.
- Escalation event is recorded.

### Phase 6: Notification Delivery

Must test:

- Notification log is created when incident is assigned.
- Active user devices receive push attempt.
- Simulated provider marks notification as SENT.
- No active devices marks notification as FAILED.
- `/received` updates status to RECEIVED.
- `/seen` updates status to SEEN.
- Delivery events return full timeline.

### Phase 7: Admin Configuration

Must test:

- Admin can create users.
- Admin can create projects.
- Admin can create support groups.
- Admin can add group members.
- Admin/manager can create escalation policies.
- Engineer cannot change configuration.
- Invalid configuration returns 400.

### Phase 8: Dynatrace Gateway

Must test:

- Missing gateway token returns 401.
- Invalid gateway token returns 401.
- Valid gateway token forwards payload to alert ingestion.
- Forwarded request includes valid E-Pager HMAC signature.
- Downstream Dynatrace adapter creates incident.

## 5. Manual Test Checklist

Use this checklist before demo:

```text
[ ] App starts on port 8080
[ ] Swagger opens
[ ] Admin login works
[ ] JWT Authorize works
[ ] GET /api/users works as admin
[ ] Manager cannot GET /api/users
[ ] Engineer sees only assigned incidents
[ ] Dynatrace gateway accepts valid payload
[ ] Incident is created
[ ] Notification log is created
[ ] Notification received/seen tracking works
[ ] Incident acknowledge works
[ ] Incident resolve works
[ ] Webhook audit shows alert attempt
```

## 6. Recommended First Integration Tests To Write

Start with these four:

1. `validGrafanaHmacAlertCreatesIncident`
2. `missingHmacSignatureRejectsAlert`
3. `invalidHmacSignatureRejectsAlert`
4. `webhookAuditIsStoredForAlertAttempt`

These give maximum confidence because they test the public alert boundary and incident creation in one flow.

## 7. Test Data Strategy

Use seeded data when possible:

```text
Project: payments
Group: primary-support
Service: payments
Engineer: Shivam Engineer
Manager: Ravi Lead
Manager: Manish Manager
Webhook secret: demo-webhook-token
```

Use unique external alert IDs in every test:

```text
grafana-test-<timestamp>
dynatrace-test-<timestamp>
```

This prevents accidental duplicate incident behavior from hiding test failures.

## 8. Definition Of Done

A flow is considered tested when:

- positive case is covered
- at least one negative case is covered
- database side effect is verified
- role/security behavior is verified if endpoint is protected
- expected HTTP status is asserted
- expected response fields are asserted
