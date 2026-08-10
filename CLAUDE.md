# truck-ai — Fleet Dispatch Dark-Data Structuring Agent

## What this project does
Ingests messy, unstructured, multilingual (Hindi/English/Hinglish) operational
messages about a small vehicle fleet from three channels (WhatsApp export,
plain text, admin form), extracts them into structured, validated records
using an LLM, tracks each vehicle's trips through a lifecycle, monitors
reporting reliability, and answers natural-language questions grounded in the
stored data (RAG).

Stack: Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, Spring AI +
Claude (Anthropic), RabbitMQ (async pipeline, added later).

## Core design principles — do not violate these
- **Never let raw LLM output be trusted without validation.** Every extracted
  field must be checked (vehicle exists, timestamp not in the future,
  location non-empty) before being marked CONFIRMED.
- **Never fabricate missing data.** If a field can't be determined (e.g. an
  unresolvable place name for distance estimation, or a skipped trip stage),
  leave it null / log the gap. Do not guess or backfill plausible-looking
  values.
- **Idempotency is mandatory**, not optional. Every TripEvent has an
  `idempotencyKey` = hash(tripId + sourceChannel + senderName + rawText),
  enforced as a unique DB constraint.
- **Only one active Trip per Vehicle at a time**, enforced via a partial
  unique index on `vehicle_id` where `status NOT IN ('COMPLETED','CANCELLED')`.
- **Admin manual entries do not skip LLM involvement.** They skip the
  *extraction* step (input is already structured), but must still pass
  through the same LLM-based *validation* step as every other channel.

## Entities

### Vehicle
- `vehicleId`, `currentDriverName`, `fleetOwnerId`

### Trip
- `tripId`, `vehicleId` (FK)
- `status`: enum `CREATED, LOADING, IN_TRANSIT, UNLOADING, COMPLETED, CANCELLED`
- `originPlace`, `destinationPlace` (free-text, set once at creation)
- `startedAt`, `completedAt` (nullable)
- Constraint: partial unique index on `vehicleId` where status is not
  COMPLETED or CANCELLED (at most one active trip per vehicle)
- State machine is intentionally loose: a trip may jump directly from any
  active state to COMPLETED even if intermediate events (e.g. an explicit
  UNLOAD) were never reported. If this happens, log it plainly (e.g. an
  AuditLog/flag noting "completed without an explicit unload event on
  file") — never synthesize the missing event.

### TripEvent (one row per incoming message)
- `eventId`, `tripId` (FK), `rawText`, `sourceChannel`: enum `WHATSAPP,
  PLAIN_TEXT, FORM`, `senderName`, `receivedAt`
- `eventType`: enum `LOAD, UNLOAD, LOCATION_UPDATE, INCIDENT, LOCATION_QUERY`
- `incidentSubType` (nullable, only when eventType=INCIDENT): enum
  `BREAKDOWN, ACCIDENT, DELIVERY_REFUSED, DRIVER_CHANGE,
  THEFT_OR_SECURITY, DOCUMENT_OR_CHECKPOINT_ISSUE, OTHER`
- `extractedLocation` (nullable), `extractedTimestamp` (nullable — when the
  event actually happened; use this for timeline ordering, NOT receivedAt)
- `detectedLanguage`, `confidence` (0.0–1.0)
- `status`: enum `RAW, EXTRACTED, CONFIRMED, PENDING_REVIEW, REJECTED`
- `estimatedDistanceFromPrevious` (nullable — only filled when this event
  and the prior one both geocode successfully; never guessed)
- `idempotencyKey` (unique, see above)

### ComplianceStatus (one row per active Trip)
- `tripId` (FK), `lastConfirmedCheckInAt`
- `complianceState`: enum `ON_TRACK, OVERDUE`
- `flaggedAt` (nullable), `adminNotified` (boolean)
- **Reset rule**: any CONFIRMED TripEvent with a valid extractedLocation +
  extractedTimestamp updates `lastConfirmedCheckInAt` — regardless of
  eventType — EXCEPT `LOCATION_QUERY`, which does not reset the clock
  (it's someone asking where the vehicle is, not the vehicle reporting in).
  An INCIDENT with a location counts as a full reset, same as a plain
  LOCATION_UPDATE.
- Row is removed/archived once the trip completes — nothing to monitor after
  that.

### AuditLog (one row per extraction/validation decision)
- `eventId` (FK), `rawText`, `promptVersion`, `modelUsed`, `confidence`,
  `extractedFields` (JSON), `finalStatus`, `timestamp`

## Pipeline flow
1. Ingestion (3 channels) → normalized raw TripEvent (status=RAW)
2. Extraction (LLM) — **skipped only for the admin/FORM channel**, since
   that input is already structured
3. Validation (LLM-based) — **runs for every channel, including admin
   entries** — checks plausibility against trip history, not just field
   well-formedness
4. Persist → TripEvent (CONFIRMED/PENDING_REVIEW/REJECTED) + AuditLog
5. Compliance job (scheduled, per active trip) → alert admin if overdue

## Timeline ordering
Always sort/display TripEvents by `extractedTimestamp`, not `receivedAt`.
Late-arriving messages (including admin backdated entries) slot into their
correct chronological position this way — no special-case logic needed.
`receivedAt` is used only for the compliance/overdue check, since that must
reflect when the system actually learned something, not when it happened.

## Explicitly out of scope for this build (do not add without asking)
Multi-agent orchestration, MCP server exposure, semantic caching, A/B
testing of prompts, real GPS/telematics integration, full route/waypoint
tracking, multi-tenant support, real-time WhatsApp Business API webhooks.
These are documented as future extensions in the README, not built now.
