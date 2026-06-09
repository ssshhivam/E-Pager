# NFR Compliance Report

## 1. Purpose

This report describes E-Pager's current compliance against major non-functional requirements.

Status levels:

```text
Compliant
Partially Compliant
Not Yet Implemented
Planned
```

## 2. Availability

Requirement:

E-Pager should be available to receive monitoring alerts and support incident operations.

Current status:

```text
Partially Compliant
```

Implemented:

- Stateless Spring Boot service.
- PostgreSQL-backed persistence.
- Alert ingestion APIs.
- Retry-independent scheduled escalation loop.

Gaps:

- No HA deployment yet.
- No load balancer setup.
- No multi-instance locking for scheduler.
- No health-check endpoint documented, though Spring Boot Actuator can be added.

Recommendation:

- Add Spring Boot Actuator.
- Deploy at least two app instances.
- Use a distributed scheduler lock such as ShedLock.
- Use managed PostgreSQL with backups.

## 3. Security

Requirement:

Only authorized users and trusted external alert sources can perform sensitive actions.

Current status:

```text
Partially Compliant
```

Implemented:

- JWT login.
- Role-based API authorization.
- HMAC webhook validation.
- Webhook timestamp validation.
- Webhook audit logs.
- Dynatrace gateway bearer token.
- Firebase credentials externalized through file path.
- Standard JJWT library for access-token signing and validation.
- Refresh token rotation with hashed token storage.
- Authenticated password change with refresh-token revocation.

Gaps:

- No password reset flow.
- Seeded passwords must be changed before production.
- No rate limiting.
- No account lockout.
- No secret rotation API.

Recommendation:

- Add password reset and admin password recovery.
- Add rate limiting for login and webhook endpoints.
- Add secret rotation for webhook sources.
- Move secrets to vault or environment manager.

## 4. Performance

Requirement:

Alert ingestion and incident creation should be fast enough for operational response.

Current status:

```text
Partially Compliant
```

Implemented:

- Simple synchronous request processing.
- Indexed incident lookup by source and external alert ID.
- JPA repository pattern.

Gaps:

- Notification sending currently occurs in request/transaction path.
- No queue for bursty alert traffic.
- No load testing performed.

Recommendation:

- Introduce asynchronous queue for notification dispatch.
- Add load tests for alert ingestion.
- Track latency metrics.
- Add connection pool tuning.

## 5. Scalability

Requirement:

System should support more alert sources, users, and incidents over time.

Current status:

```text
Partially Compliant
```

Implemented:

- Adapter pattern for alert sources.
- Pluggable notification provider.
- Stateless application design.
- PostgreSQL database.

Gaps:

- Scheduler may duplicate work in multi-instance deployment.
- No partitioning/archive for incident and audit tables.
- No event queue.

Recommendation:

- Add queue-based notification and escalation processing.
- Add scheduler locks.
- Archive old incidents and audit logs.
- Add database indexes based on query patterns.

## 6. Reliability

Requirement:

System should not lose critical alert and notification state.

Current status:

```text
Partially Compliant
```

Implemented:

- Incidents persisted.
- Escalation events persisted.
- Notification logs persisted.
- Delivery events persisted.
- Webhook audit logs persisted.

Gaps:

- Notification provider call is not retried.
- No dead-letter handling.
- No transactional outbox.

Recommendation:

- Add notification retry policy.
- Add transactional outbox table.
- Add dead-letter status for permanent failures.
- Add alert ingestion idempotency tests.

## 7. Maintainability

Requirement:

Code should be modular and easy to extend.

Current status:

```text
Compliant for current scope
```

Implemented:

- Clear package separation.
- DTOs separated from entities.
- Source adapter abstraction.
- Provider abstraction for notifications.
- Flyway-controlled schema.
- README and docs folder.
- Standard JWT library instead of handwritten JWT parsing.

Gaps:

- More service-level unit tests needed.

Recommendation:

- Add unit tests for `IncidentService`, `EscalationService`, and adapters.
- Add module diagrams to architecture docs.

## 8. Observability

Requirement:

Operators should understand system behavior and failures.

Current status:

```text
Partially Compliant
```

Implemented:

- Webhook audit logs.
- Escalation events.
- Notification delivery events.
- Provider error captured in notification log.

Gaps:

- No metrics endpoint.
- No tracing.
- No structured logging standard.
- No dashboard.

Recommendation:

- Add Spring Boot Actuator.
- Expose Prometheus metrics.
- Add structured JSON logs.
- Add dashboards for alert rate, failure rate, escalation count, notification status.

## 9. Auditability

Requirement:

Important operational and security actions should be traceable.

Current status:

```text
Partially Compliant
```

Implemented:

- Webhook audit.
- Escalation event records.
- Notification delivery events.
- Incident acknowledgement and resolution timestamps.

Gaps:

- No audit trail for admin changes.
- No audit trail for user/project/policy updates.
- No actor tracking for some manager/admin operations.

Recommendation:

- Add `audit_events` table.
- Track actor, action, target, before/after summary, timestamp.

## 10. Usability

Requirement:

Users should operate the system efficiently.

Current status:

```text
Partially Compliant
```

Implemented:

- Swagger API for testing.
- Seeded data.
- User/admin guide.

Gaps:

- No frontend.
- No mobile/web client for push registration.
- No UI for incident workflow.

Recommendation:

- Build admin UI.
- Build engineer incident view.
- Build push registration client.

## 11. Portability

Requirement:

Application should run consistently across environments.

Current status:

```text
Partially Compliant
```

Implemented:

- Java 17.
- Maven build.
- Environment-variable configuration.
- PostgreSQL standard database.
- H2 profile for quick experiments.

Gaps:

- Dockerfile not yet implemented.
- Docker Compose not fully documented for app plus database.

Recommendation:

- Add Dockerfile.
- Add Docker Compose for local full stack.
- Add CI build workflow.

## 12. Compliance Summary

| NFR | Status |
|---|---|
| Availability | Partially Compliant |
| Security | Partially Compliant |
| Performance | Partially Compliant |
| Scalability | Partially Compliant |
| Reliability | Partially Compliant |
| Maintainability | Compliant for current scope |
| Observability | Partially Compliant |
| Auditability | Partially Compliant |
| Usability | Partially Compliant |
| Portability | Partially Compliant |

## 13. Top NFR Priorities

1. Add webhook/HMAC integration tests.
2. Add Actuator and metrics.
3. Add notification retry/outbox.
4. Add password reset and account lockout.
5. Add Docker deployment.
6. Add admin audit log.
7. Add real client for push token registration.
