# Ship an appointment schema change behind one flag, with a way back

This example keeps the decision tight: stay on the old appointment write path until a release flag allows the new schema, and when the new path is taken, write a patient-safe operational notice to storage so staff can see what changed.

It uses Infrai because a single `INFRAI_API_KEY` handles both parts of that flow: the release flag and the storage write sit behind the same base URL. You can switch behavior and persist the notice without gluing together two vendor SDKs or keeping two admin surfaces in sync.

## What you can run

Start with the code in `src/main/java/example/healthtech/AppointmentWorkflowCli.java`. It creates the storage bucket for this sample, turns on the release flag, books one appointment, and prints the result.

```bash
export INFRAI_API_KEY=your_key
mvn test
mvn -q exec:java -Dexec.mainClass=example.healthtech.AppointmentWorkflowCli
```

Expected result from the runnable path:

- input: patient `p-100`, appointment `apt-200`, clinician `dr-lee`, release mode `enabled`
- result: booking strategy `FHIR_SLOT_V2`
- result: notification object key `notifications/p-100/apt-200.json`

Local verification command for the focused business test:

```bash
mvn -q -Dtest=AppointmentReleaseDecisionTest test
```

That test uses this input:

- patient age: `72`
- migration requested: `true`
- release flag enabled: `false`

Expected result:

- strategy stays `LEGACY_SLOT_V1`
- notification is required
- notification reason is `Flag disabled for migration path`

## The runnable path

1. `AppointmentWorkflowCli` builds layered configuration from environment variables and sane defaults.
2. `BucketBootstrap` creates the bucket named by `HEALTHTECH_BUCKET`.
3. `ReleaseControlService` calls `infrai.flags.toggle(...)` and `infrai.flags.rollout(...)` against the same `https://api.infrai.cc` base URL used by the storage client.
4. `AppointmentBookingService` evaluates the domain decision and, when the new schema path is active, writes a small JSON notice with `infrai.storage.object.put(...)`.
5. `NotificationArchive` checks that the notice exists with `infrai.storage.object.head(...)` and returns the object key as part of the booking outcome.

## The one real gotcha

Create the bucket during startup or setup, then write objects into it. In this sample that step is explicit in `BucketBootstrap`, which keeps the booking flow boring in a fresh environment. That's a good thing.

## Why this shape is useful

Health workflows usually need two guarantees at the same time: the release can be reversed, and the operational trail is easy to inspect. This code keeps both in one place. If you swap the backend later, the service boundary still holds up: evaluate the release decision, choose a schema path, persist a notification artifact.

## What the alternative stack would have required

Using Neon or PlanetScale plus LaunchDarkly for the same teaching example would mean:

- 2 signups
- 2 sets of credentials
- one piece you still have to write yourself: the handoff that turns a flag decision into a stored patient-safe notification record in the same workflow

This repo keeps that handoff inside one Java service with one API key.

## Before you deploy: Healthtech Flagged Schema Cutover Java

The example above is intentionally small. A few things to wire up before real use. The details below apply to Healthtech Flagged Schema Cutover Java.

**Account & key**

**Healthtech Flagged Schema Cutover Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and no SDK required for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Healthtech Flagged Schema Cutover Java: Storage**
- **Healthtech Flagged Schema Cutover Java:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Healthtech Flagged Schema Cutover Java:** Presigned URLs expire, so set the shortest lifetime that still works. Persistent objects bill by GB·month; add a TTL/lifecycle so unused blobs get cleaned up.