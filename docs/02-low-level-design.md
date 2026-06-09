# Low-Level Design

## 1. Purpose

This document describes the internal design of E-Pager at package, class, entity, flow, and API interaction level.

## 2. Package Structure

```text
com.example.epager
  alert
  escalation
  gateway
  incident
  notification
  project
  security
  user
  webhook
```

## 3. Domain Model

### 3.1 User Domain

Entity:

```text
AppUser
```

Fields:

- `id`
- `name`
- `email`
- `phoneNumber`
- `passwordHash`
- `role`

Roles:

```text
ADMIN
MANAGER
ENGINEER
```

DTOs:

- `AppUserRequest`
- `AppUserResponse`

Repository:

```text
AppUserRepository
```

Important methods:

- `findByEmailIgnoreCase`

Refresh token entity:

```text
RefreshToken
```

Fields:

- `id`
- `user`
- `tokenHash`
- `expiresAt`
- `createdAt`
- `revokedAt`

Design:

- The raw refresh token is returned only once to the client.
- Only a SHA-256 hash of the refresh token is stored.
- Refresh tokens are rotated on every refresh request.
- Password change revokes all active refresh tokens for that user.

### 3.2 Project Domain

Entities:

- `Project`
- `SupportGroup`
- `SupportGroupMember`

Purpose:

- Project identifies the business/service area.
- Support group identifies the responsible operational group.
- Members define who can participate in escalation.

Repositories:

- `ProjectRepository`
- `SupportGroupRepository`
- `SupportGroupMemberRepository`

### 3.3 Alert Domain

Main classes:

- `AlertController`
- `AlertService`
- `AlertSourceAdapter`
- `AlertSourceAdapterRegistry`
- `GrafanaAlertAdapter`
- `DynatraceAlertAdapter`
- `UnifiedAlert`

`UnifiedAlert` fields:

- `source`
- `externalAlertId`
- `projectKey`
- `groupKey`
- `serviceName`
- `severity`
- `title`
- `description`

Design:

```text
Raw JSON -> Adapter -> UnifiedAlert -> Incident
```

### 3.4 Incident Domain

Entity:

```text
Incident
```

Fields:

- `id`
- `externalAlertId`
- `source`
- `projectKey`
- `groupKey`
- `serviceName`
- `severity`
- `title`
- `description`
- `status`
- `currentEscalationLevel`
- `nextEscalationAt`
- `assignedUser`
- `acknowledgedBy`
- `createdAt`
- `acknowledgedAt`
- `resolvedAt`

Status:

```text
TRIGGERED
ACKNOWLEDGED
RESOLVED
```

Service:

```text
IncidentService
```

Key methods:

- `createOrUpdateIncident(UnifiedAlert alert)`
- `findVisibleTo(AuthenticatedUser user)`
- `findVisibleById(Long incidentId, AuthenticatedUser user)`
- `acknowledge(Long incidentId, Long userId)`
- `acknowledge(Long incidentId, AuthenticatedUser user)`
- `resolve(Long incidentId, AuthenticatedUser user)`

Access logic:

- Admin and manager can view all incidents.
- Engineer can view only incidents assigned to that engineer.
- Engineer acknowledgement uses the current authenticated user, not a request body user ID.

### 3.5 Escalation Domain

Entities:

- `EscalationPolicy`
- `EscalationLevel`
- `EscalationEvent`

Policy fields:

- `projectKey`
- `groupKey`
- `serviceName`
- `enabled`
- `levels`

Level fields:

- `levelNumber`
- `user`
- `waitMinutes`

Service:

```text
EscalationService
```

Responsibilities:

- Assign level 1 user.
- Calculate `nextEscalationAt`.
- Escalate when incident is still `TRIGGERED`.
- Stop escalation when incident becomes `ACKNOWLEDGED` or `RESOLVED`.
- Record escalation events.

### 3.6 Notification Domain

Entities:

- `UserDevice`
- `NotificationLog`
- `NotificationDeliveryEvent`

Device platform:

```text
WEB
ANDROID
IOS
DESKTOP
```

Notification status:

```text
QUEUED
SENT
RECEIVED
SEEN
FAILED
```

Provider interface:

```java
public interface NotificationProvider {
    NotificationChannel channel();
    NotificationResult send(NotificationRequest request);
}
```

Implemented providers:

- `PushNotificationProvider`, simulated default provider.
- `FirebasePushNotificationProvider`, real FCM provider when enabled.

Firebase activation:

```text
EPAGER_FIREBASE_ENABLED=true
```

### 3.7 Webhook Security Domain

Entities:

- `WebhookSource`
- `WebhookAuditLog`

Service:

```text
WebhookSecurityService
```

Validation:

```text
expected = HMAC_SHA256(secret, timestamp + ":" + rawPayload)
```

Headers:

```text
X-EPAGER-TIMESTAMP
X-EPAGER-SIGNATURE
```

Failure cases:

- Unknown source
- Disabled source
- Missing timestamp
- Expired timestamp
- Missing signature
- Invalid signature

### 3.8 Gateway Domain

Classes:

- `DynatraceWebhookGatewayController`
- `DynatraceWebhookGatewayService`
- `GatewayAuthenticationException`

Endpoint:

```text
POST /gateway/webhooks/dynatrace
```

Responsibilities:

- Validate static Dynatrace bearer token.
- Generate timestamp.
- Generate E-Pager HMAC signature.
- Forward to `/api/alerts/dynatrace`.

## 4. Controller Design

| Controller | Base Path | Responsibility |
|---|---|---|
| `AuthController` | `/api/auth` | Login, refresh token rotation, password change, JWT generation |
| `AlertController` | `/api/alerts` | HMAC-secured alert ingestion |
| `DynatraceWebhookGatewayController` | `/gateway/webhooks` | Dynatrace gateway input |
| `IncidentController` | `/api/incidents` | Incident list, detail, acknowledge, resolve |
| `EscalationPolicyController` | `/api/escalation-policies` | Escalation policy CRUD and events |
| `NotificationController` | `/api/notifications` | Notification logs and delivery tracking |
| `AppUserController` | `/api/users` | Users and device registration |
| `ProjectController` | `/api/projects` | Projects, groups, group members |
| `WebhookController` | `/api/webhooks` | Webhook sources and audit logs |

## 5. Security Design

### 5.1 JWT Authentication

Login:

```text
POST /api/auth/login
```

Response includes:

```text
tokenType
accessToken
accessTokenExpiresAt
refreshToken
refreshTokenExpiresAt
userId
name
email
role
```

Implementation:

- Access tokens are signed and validated using the standard JJWT library.
- Access token claims include user ID, email, and role.
- Refresh tokens are opaque random values, not JWTs.
- E-Pager stores only refresh token hashes in PostgreSQL.
- `POST /api/auth/refresh` revokes the old refresh token and issues a new token pair.
- `POST /api/auth/change-password` validates the current password, updates the password hash, and revokes active refresh tokens.

Filter:

```text
JwtAuthenticationFilter
```

User details:

```text
AuthenticatedUser
AppUserDetailsService
```

### 5.2 Authorization Rules

| Path | Access |
|---|---|
| `/api/auth/login`, `/api/auth/refresh` | Public |
| `/api/auth/change-password` | Authenticated |
| `/api/alerts/**` | Public at Spring Security layer, protected by HMAC |
| `/gateway/webhooks/**` | Public at Spring Security layer, protected by gateway token |
| `/swagger-ui/**`, `/v3/api-docs/**` | Public |
| `/api/users/**` | Admin |
| `/api/projects/**` | Admin |
| `/api/webhooks/**` | Admin |
| `/api/escalation-policies/**` | Admin, Manager |
| `/api/incidents/**` | Admin, Manager, Engineer |
| `/api/notifications/**` | Admin, Manager, Engineer |

## 6. Database Design

Flyway migrations:

| Migration | Purpose |
|---|---|
| `V1__initial_schema.sql` | Core tables |
| `V2__hmac_audit_and_delivery_events.sql` | Webhook audit and delivery events |
| `V3__user_roles_and_passwords.sql` | User security columns |
| `V4__reset_seed_user_passwords.sql` | Seed user password repair |
| `V5__refresh_tokens.sql` | Refresh token storage |
| `V6__refresh_token_user_not_null.sql` | Refresh token user constraint |

Important relationships:

- `incidents.assigned_user_id -> app_users.id`
- `incidents.acknowledged_by_id -> app_users.id`
- `escalation_levels.policy_id -> escalation_policies.id`
- `escalation_levels.user_id -> app_users.id`
- `notification_logs.incident_id -> incidents.id`
- `notification_logs.recipient_id -> app_users.id`
- `notification_delivery_events.notification_log_id -> notification_logs.id`
- `support_groups.project_id -> projects.id`
- `support_group_members.support_group_id -> support_groups.id`
- `support_group_members.user_id -> app_users.id`
- `refresh_tokens.user_id -> app_users.id`

## 7. Important Flows

### 7.1 HMAC Alert Flow

```text
POST /api/alerts/{source}
  -> WebhookSecurityService.validate
  -> AlertSourceAdapterRegistry.getAdapter
  -> adapter.toUnifiedAlert
  -> IncidentService.createOrUpdateIncident
  -> EscalationService.startEscalation
  -> NotificationService.notifyUser
```

### 7.2 Dynatrace Gateway Flow

```text
POST /gateway/webhooks/dynatrace
  -> validate Authorization bearer token
  -> generate HMAC timestamp and signature
  -> POST /api/alerts/dynatrace
  -> normal alert flow
```

### 7.3 Escalation Flow

```text
Incident created
  -> level 1 user assigned
  -> notification sent
  -> nextEscalationAt set
  -> scheduled job checks due incidents
  -> if still TRIGGERED, move to next level
```

### 7.4 Delivery Tracking Flow

```text
Notification queued
  -> provider sends
  -> status SENT or FAILED
  -> client calls /received
  -> client calls /seen
  -> delivery events show complete timeline
```

## 8. Configuration Keys

Database:

```text
EPAGER_DB_URL
EPAGER_DB_USERNAME
EPAGER_DB_PASSWORD
```

Gateway:

```text
EPAGER_GATEWAY_DYNATRACE_TOKEN
EPAGER_GATEWAY_EPAGER_ALERT_URL
EPAGER_GATEWAY_EPAGER_HMAC_SECRET
```

Firebase:

```text
EPAGER_FIREBASE_ENABLED
EPAGER_FIREBASE_SERVICE_ACCOUNT_PATH
EPAGER_FIREBASE_PROJECT_ID
```

JWT:

```text
epager.security.jwt-secret
epager.security.jwt-ttl-seconds
epager.security.refresh-token-ttl-seconds
```

Scheduler:

```text
epager.scheduler.escalation-check-rate-ms
```

## 9. Test Design

Current PostgreSQL integration test:

```text
RoleBasedAccessIntegrationTest
```

Covers:

- Admin access.
- Manager access.
- Engineer assigned incident access.
- Protected endpoint blocking.
- Gateway missing-token rejection.
- Refresh token rotation and old-token rejection.
- Password change revokes active refresh tokens.

Recommended next tests:

- Valid HMAC alert creates incident.
- Missing signature rejected.
- Invalid signature rejected.
- Old timestamp rejected.
- Webhook audit rows created.
- Gateway valid token forwards alert.
- FCM provider behavior with mocked FirebaseMessaging.
