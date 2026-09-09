---
feature_id: MODUSERSKC-133
title: System-user event confirmation
updated: 2026-09-08
---

## What it does

After processing a system-user Kafka event (CREATE, UPDATE, or DELETE), the module publishes a confirmation message to a dedicated resource-result topic. The confirmation carries a SUCCESS or FAILURE status so that mgr-tenant-entitlements can track whether each entitlement step completed.

## Why it exists

mgr-tenant-entitlements orchestrates multi-module entitlement flows asynchronously. Without a feedback signal from each participant, it cannot determine whether a step succeeded or must be retried or rolled back.

## Entry points

### Kafka topics

| Direction | Topic pattern                                         | Event type            | Notes                                                     |
|-----------|-------------------------------------------------------|-----------------------|-----------------------------------------------------------|
| Consumes  | `(${ENV}\.)(.*\.)mgr-tenant-entitlements.system-user` | `SystemUserEvent`     | CREATE / UPDATE / DELETE                                  |
| Produces  | `${ENV}.mgr-tenant-entitlements.resource-result`      | `ResourceResultEvent` | Confirmation; only when `EVENT_CONFIRMATION_ENABLED=true` |

## Business rules and constraints

- The feature is **disabled by default** (`EVENT_CONFIRMATION_ENABLED=false`). It must be explicitly enabled per deployment.
- A **SUCCESS** confirmation is published synchronously at the end of each successfully completed `createOnEvent`, `updateOnEvent`, or `deleteOnEvent` call.
- A **FAILURE** confirmation is published by the `systemUsersRecoverer` when all retry attempts are exhausted. For transient errors (tenant disabled, schema not yet created) the module retries with configurable back-off before the recoverer fires. For all other `RuntimeException` types the back-off is `FixedBackOff(0, 0)` — the recoverer fires immediately with no retries.
- The confirmation message includes the original event `id`, `tenant`, `resourceName`, `moduleId` (from `SystemUser.moduleId`), `status`, and — on FAILURE — an error `details` payload.
- Only system-user events produce confirmations. Events on the `users.users` topic do not.

## Error behavior

| Condition                                         | Outcome                                                                                                                                        |
|---------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| Handler throws `RuntimeException`                 | Recoverer fires immediately; FAILURE confirmation published                                                                                    |
| Tenant disabled / schema missing                  | Handler retried per `KAFKA_SYS_USER_TOPIC_RETRY_ATTEMPTS` / `KAFKA_SYS_USER_TOPIC_RETRY_DELAY`; FAILURE published only after retries exhausted |
| `moduleId` cannot be extracted from event payload | `moduleId` field in confirmation is `null`; confirmation still published                                                                       |

## Configuration

| Environment variable         | Default                                          | Description                                        |
|------------------------------|--------------------------------------------------|----------------------------------------------------|
| `EVENT_CONFIRMATION_ENABLED` | `false`                                          | Enable publishing of SUCCESS/FAILURE confirmations |
| `EVENT_CONFIRMATION_TOPIC`   | `${ENV}.mgr-tenant-entitlements.resource-result` | Kafka topic for confirmation messages              |

## Dependencies

- `folio-kafka-consumer` (from `applications-poc-tools`) — provides `ResourceResultEventPublisher`, `ResourceResultEventPublishingRecoverer`, `ModuleIdExtractor`, and `LoggingRecoverer`.
- mgr-tenant-entitlements — the consumer of the confirmation topic.
