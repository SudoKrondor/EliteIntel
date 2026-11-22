# Crowd-Sourced Massacre Sets and Install Stats

**Status:** design for V1.2, written 2026-11-13 after a brainstorming session. Nothing is built. The Firebase
project does not exist yet, and setting it up is the next session.

Two features share one piece of plumbing:

1. **Massacre sets.** Commanders share the pirate massacre provider/target pairs the app learns from their
   journals, and every participant can search everyone's pairs, not just their own.
2. **Install stats.** An anonymous daily ping says how many installs are active, per OS and per app version.

Both write to Firestore. Neither reads from it. The app reads static JSON files that a nightly job publishes
to `elite-intel.org`, the same way it already reads `data/exomastery.json`.

---

## Why

The app already learns massacre sets locally: `massacre_mission` (commander file) holds every contract the
commander took, and `MassacreMissionDao.findPairs` joins it to `hunting_ground` (shared, filled from the
journal and from EDDN). A commander only ever sees the sets they found themselves.

Nobody else offers this either. EDDN does not carry mission events, so Spansh and INARA cannot learn a pair
from it. INTRA was the only API for it and has been dead for months. INARA lists pairs, but has no API, and
some of its pairs name a target with no resource site at all.

For the stats, we have no idea how many people use the app, on which OS, or on which version, or whether a
user has stopped using it.

---

## What a massacre set is

Providers in systems A, B, C, ... have factions that issue massacre contracts against a pirate faction in
system Z, and Z has a resource site. **A single A/Z pair is already a set.** Several provider factions in A
make it a profitable stack. One makes it a modest one. The commander decides, and can blacklist it.

Facts that shape the design (from years of play, not from code):

- **Sets last for years.** Providers do not vanish with the background simulation. Conflict zones churn
  weekly, massacre pairs do not. **Pairs never expire.**
- **Z is always close to A:** 20 ly at the most, usually one jump with no star in between. This is a hard
  check. **Measured:** the 33 pairs in Alan's journals are 1.3 to 9.9 ly apart, so 20 ly leaves twice the room.
- **The pirate faction is an Anarchy.** **Measured:** all 33 target factions are Anarchies in the dump, and
  all 33 are still present in their target system years later. This is a hard check. It filters less than it
  sounds: 94,000 of the 120,000 populated systems have an Anarchy faction somewhere. What it rules out is
  naming the *wrong* faction in Z. A lawful faction's ship can be wanted and can turn pirate, so a ship's
  status proves nothing about its faction.
- **Pairs last. Provider factions move.** Every provider *system* is still populated, but provider factions
  come and go: of the factions that issued Alan's contracts, many have left their system since (for example,
  Col 285 Sector RO-Q d5-54 had 4 provider factions in 2023 and 1 in the dump). So "the reported provider
  faction is still in A" is a **soft** check that only lowers the stack depth shown, never a reason to drop a
  pair.
- **A blacklist is a matter of taste:** too easy, too hard, pledged to a different power. It says nothing
  about whether the data is right. **Blacklists stay local and are never uploaded.**
- **Large uploads are legitimate.** Importing years of journal backups produces a big ledger in one go, so
  volume is never a sign of fraud.

---

## Architecture

```
App ──(anonymous write, at most once a day, silent fail)──▶ Firestore
                                                               │
                          nightly GitHub Action (service account in repo secrets)
                          reads what changed, merges, writes the published files
                                                               ▼
   elite-intel.org/data/massacre-sets.json     ◀── app fetches once a day (ETag)
   elite-intel.org/data/config.json            ◀── app fetches with the sets (kill switch)
   elite-intel.org/ops-aec9172a/stats.json     ◀── the unlisted stats page reads it
```

The verification index (built from the Spansh dump) is used only by the nightly job. It is never published
or downloaded by the app.

Why the app reads static files instead of querying Firestore:

- **Reads are what cost money.** 2,000 installs each reading 2,000 pair documents would be 4M reads a day,
  and Spark allows 50K. The nightly job's reads come to about the number of documents that changed.
- **No public read rule exists.** Firestore cannot be scraped, and the stats page needs no Firebase SDK.
- **The nightly job does the server-side work** that Cloud Functions would, and Spark has no Cloud Functions.

Every network call the app makes here is non-blocking and fails silently. The app is whole without it.

---

## Firestore layout

One install has one anonymous Firebase uid (see [Authentication](#authentication)).

### `installs/{uid}`: the stats ping

| Field | Example | Note |
|---|---|---|
| `appVersion` | `1.2.0012` | |
| `os` | `windows` / `linux` | |
| `updatedAt` | server timestamp | the rules require it to equal `request.time` |

Written at most once a day. **No commander name, FID, rank, or anything about the game.** Rules cannot turn
`request.time` into a date string, so the time is stored as a server timestamp and the nightly job reduces it
to a date. The stats page never shows a time of day.

### `reports/{uid}`: the install's massacre sets

| Field | Note |
|---|---|
| `pairs` | **one string**: every pair the install knows, in payload format 1 (below) |
| `updatedAt` | server timestamp, equal to `request.time`, which the nightly job queries on |

Each encoded pair carries:

| Item | Note |
|---|---|
| provider system and SystemAddress | |
| provider station | optional |
| provider faction | one entry per faction, so the stack depth can be worked out |
| target system, SystemAddress, target faction | |
| resource sites | standard, low, high, hazardous, as the reporter saw them |
| last seen | date only |

**Payload format 1** (`format: 1`). The reference is `PairsPayload` in the nightly repo, and the app's
encoder must match it. It is a JSON array with one object per provider faction and target:

```json
[{"ps":"Bean","st":"Bruce Dock","pf":"Earls of Bean","ts":"Bellite","tf":"Bellite Raiders","r":[0,1,2,0],"d":"2026-11-13"}]
```

| Key | Meaning |
|---|---|
| `ps`, `pf`, `ts`, `tf` | required: provider system and faction, target system and pirate faction. 1-64 characters each, no control characters, and the two systems must differ |
| `st` | optional: the provider station |
| `r` | optional: the resource sites seen in the target as 4 integers 0-30 (standard, low, high, hazardous) |
| `d` | optional: last seen, as `YYYY-MM-DD` |

The job drops a malformed entry on its own and keeps the rest. Alan's 103 entries come to 13,278 characters
with stations and dates (the 7.3 KB measured earlier left them out), still a fifth of the 64,000 limit.

**Why one string and not a map:** security rules cannot loop, so they cannot check the entries of a map whose
keys are not known in advance. They **can** check a string's length. One string field, with a length limit
in the rules, puts a hard limit on the size of each install's document. The nightly job parses it and drops
every entry that does not match the schema. **The three layers each have one job:**

- rules limit volume
- the job checks the format
- each app checks whether the pair is true

One document per pair would let rules check every field, but at one write per pair a large journal import
would use up the day's quota on its own.

**The length limit, measured:** Alan's journals (3,777 files, 2019–2026, 1,086 pirate massacre contracts)
come to **33 pairs, 103 provider-faction entries and 7.3 KB** encoded without coordinates. Seven years of
heavy massacre play fits in under 10 KB. **A limit of 64,000 characters** leaves about eight times the room,
and it also limits storage abuse to about 6.4 MB an hour per IP.

**Never uploaded:** MissionID, reward, kill count, exact timestamps. They do nothing for the next commander,
and together they could fingerprint one.

**A whole journal import is one write.** Writes per day stay at no more than the number of active installs,
whatever the import size.

**Withdrawing:** turning sharing off deletes `reports/{uid}`. A delete cannot be rate-limited, but delete and
create again gains an abuser nothing that a new uid does not already give them.

**Only pairs the reporter can back up are reported:** the target must have resource sites in the reporter's
own `hunting_ground`. That is the same join `findPairs` already makes, so INARA's "resource site that isn't
there" cannot happen through an honest client.

The document is rewritten only when the install's pairs changed, and at most once a day.

---

## Sharing is opt-in: give to get

Sharing is **off by default**. A commander who opts in uploads their pairs and sees everyone else's. One
who does not, neither uploads nor sees them.

So that people find it, **VEGA mentions it when it would have helped:** when a massacre search finds nothing
nearby, or very little, and the shared data has sets in the area, VEGA says so and points to the setting.
(Speech text to be worded. It must say that other commanders reported the sets.)

**Opting in uploads the whole existing ledger**, including everything a journal import learned. One write
brings the new participant's history in straight away, which helps the network get started.

### How crowd-sourced sets are spoken

A set from the shared data is always presented as **reported by other commanders, not confirmed by you.**
This does two things:

- it sets expectations: the commander is flying to someone else's report and should treat it as something to
  investigate
- it shows the network effect: the more commanders use Elite Intel, the more sets there are

The commander's own blacklist (forget) applies to crowd-sourced sets the same way as to their own.

---

## Verification: against data the forger does not control

The app is open source. **A report cannot be made unforgeable**: journal files are not signed, and any
check the app runs, a forger can copy or skip. So the design does not try to stop bad writes. It filters
them out after they arrive, in two places that a forger does not control (decided 2026-11-13):

1. **The nightly job** checks every pair against the verification index. The index and the job are ours, so
   a pair the job publishes has passed checks nobody can switch off.
2. **Each app** checks only what the index cannot: resource sites, and systems colonised after the dump.
   Checks on the reading side only protect the reader. Someone who edits the code so every check passes fools
   only their own app.

### The verification index

A one-time pass over the commander's full Spansh dump (2026-03-02, 524 GiB, every signal Spansh had) produces
the index. **It runs on a local copy, reading one line at a time. The dump is never parsed over the network.**

For every populated system (both A and Z are populated, since Z has a pirate faction):

- name, SystemAddress, coordinates
- factions, and each faction's government

**Measured (2026-11-13, dump of 2026-03-02):** 181.7M systems, 119,625 populated. The compact index is
26.8 MB, or **5.9 MB gzipped**. The full pass takes 13.5 minutes once the dump is on local NVMe (it reads at
about 700 MB/s). All 51 systems in Alan's pairs are in it.

**The dump has no resource sites.** Spansh records body signals (SAA: human, biological, ring hotspots), but
not FSS signals, and resource sites are FSS signals. So "Z has resource sites" comes only from the app's own
`hunting_ground` (the journal and EDDN `fsssignaldiscovered`) and from the reporter's counts.

The index lives with the nightly job and is never published. It is rebuilt only from a newer dump, which is
rarely needed because the data it checks hardly changes. **Open:** which repository holds the job and the
index, and whether Spansh's terms allow a derived file to sit in a public one.

### The checks

| Check | Kind | Done by |
|---|---|---|
| A and Z exist | hard | **job**, against the index |
| The target faction is in Z, and is an Anarchy | hard | **job**, against the index |
| Z is within 20 ly of A | hard | **job**, with the index's coordinates, **never the report's** |
| The reported provider factions are still in A | soft (lowers the stack depth shown) | **job**, against the index |
| Z has resource sites | hard | **app**, against `hunting_ground` (the dump has no resource sites) |
| Pairs with a system missing from the index | the same hard checks | **app**, against its own data |

A pair that fails a hard check in the job is dropped. A pair with a system the index does not know (colonised
after the dump, most likely) is published marked **`unindexed`**, and only the app can check it.

"Its own data" means the commander's journal (every FSDJump records the system and its coordinates),
`hunting_ground` (journal plus EDDN `fsssignaldiscovered`, with coordinates), and **a new EDDN faction
ledger**. EDDN `FSDJump` messages carry each system's factions and their governments. The listener receives
them already and keeps only the war and power data. The ledger is needed only for `unindexed` pairs, so it
can come after the first release.

The one claim nothing can confirm is that A's factions actually offer massacre contracts against Z. To fake
it, a forger has to name real factions in real systems within 20 ly of a real resource site. The worst
outcome is one commander's trip to a genuine resource site, followed by a blacklist.

### No timer

- A pair that passes the job's checks, and whose target the app knows to have resource sites, is **shown**.
- A pair the app cannot check yet (no resource sites on file for Z, or `unindexed` and not covered by its own
  data) is **kept but hidden**. Storage is not a constraint, so nothing is decided by a deadline.
- When the commander's own data later covers the system, because they jump there or EDDN reports it, the pair
  appears.

This works the same at 100 users or 10,000. A pair that **fails** a hard check is dropped, not hidden.

### Publishing threshold

**One reporter is enough** for a pair to be published. The verification above confirms the details. The
number of distinct reporters is a ranking signal: more installs means more confidence.

---

## Authentication

**There is no "only Elite Intel can post".** Any secret shipped in an open-source app is public, and App
Check has no attestation provider for a desktop Java app. Hiding something would only stop casual tampering,
and it would cost maintenance on every release.

EDDN has no authentication either. It validates messages against a schema and lets the gateway block bad
software versions. What keeps Spansh and INARA clean is that their data heals itself: the next honest
observation overwrites a forged one. **Our pairs never expire, so they would not heal.** Read-side
verification does the job that constant overwriting does for EDDN.

What we use:

- **Firebase Anonymous Auth over REST**:
  - `POST identitytoolkit.googleapis.com/v1/accounts:signUp?key=…` with `{"returnSecureToken":true}`
  - refresh with `POST securetoken.googleapis.com/v1/token?key=…`
  - an ID token lasts 1 hour, and an unused refresh token does not expire
  - the app keeps the refresh token next to the install ID. Losing it means a new uid and an orphaned
    document, which the nightly job can clean up.
  - there is no official client Java SDK. The Admin SDK is privileged and is **never shipped**.
- **Minting uids is limited to 100 anonymous sign-ups per hour per IP** (HTTP 429). That is plenty for a
  shared or carrier-grade NAT address, and it is what makes "junk stays in its author's document" hold up.
- **The worst abuse is filling storage, not faking data.** One IP, minting 100 uids an hour with a
  64 KB payload each, adds about 6.4 MB an hour, so 1 GiB in about a week. On Spark, a full database refuses
  writes and never bills. The nightly job watches how many new uids appear and how fast storage grows, and
  can delete everything those uids wrote. The kill switch stops honest clients writing in the meantime.
- **Do not upgrade to Identity Platform.** It would add automatic cleanup of anonymous accounts, but it
  would also cap a Spark project at 3,000 daily active users. Orphaned anonymous accounts are harmless. If they
  are ever worth removing, the nightly job can delete them with the Admin SDK.
- **Security rules:**
  - a uid writes only `installs/{uid}` and `reports/{uid}` (document ID `== request.auth.uid`)
  - an exact field set (`keys().hasOnly(...)`), with string length limits
  - `updatedAt == request.time`
  - on update, `request.time >= resource.data.updatedAt + duration.value(24, 'h')`, so at most one write a
    day. A create has no earlier document to compare against, so the first write is always allowed. The app
    waits a full 24 hours after its last successful write before trying again.
  - no reads at all
- **Writes go through** `documents:commit`, with a `setToServerValue: REQUEST_TIME` field transform for
  `updatedAt`. When a quota is exhausted the write gets HTTP 429, and the app gives up until the next day.
  Quotas reset around midnight Pacific time.
- **Junk stays in its author's document.** The nightly job can drop or block a uid in one step.
- **The kill switch:** `config.json`, e.g. `{"reportPairs": true, "heartbeat": true, "minReportIntervalHours": 24}`,
  turns uploads down without a release if a quota runs short or someone abuses the endpoint.

---

## Install stats

**Always on, no opt-out** (decided 2026-11-13). The data is counts per OS and version and nothing else.
Support bundles stay a manual, in-person step as they are today. **Disclosed** in the site's privacy notes
and in the release notes of the version that adds it. **Independent of the sharing setting.**

The nightly job publishes:

- active installs in the last 1, 7 and 30 days, by OS and by version
- a "gone" install is one not seen for 30 days
- history: one row per night appended to `stats.json`, or simply the git history of the file

**The page is unlisted, not secret.** It has `<meta name="robots" content="noindex">` and is left out of
`robots.txt`, because listing it there would advertise it. Anyone who has the URL can look. It is for us to
see the install base and how usage grows, not to show off. It stays unlisted until there is a reason to show
the numbers. If the site repo is public, `stats.json` is also readable in its git history. That is fine for
"not advertised".

**Open:** where the install ID lives. In `database.db` it travels with Backup/Restore. That is right when a
commander moves to a new PC, but it merges two installs if one backup is restored onto a second machine. A
small file outside `AppDataArchive.FOLDERS` avoids that (the same kind of exception BindForge's working copy
has).

---

## The nightly job

**Built 2026-11-13** in the private repo `SudoKrondor/elite-intel-nightly` (Java 21, Gradle; its README
covers running and maintaining it). It runs at 04:17 UTC, and on demand from **Run workflow**:

1. Reads **every** `installs` and `reports` document. That is one read per install plus one per sharing
   install: a full read fits Spark's 50K/day up to about 40,000 installs, needs no state between runs, and
   handles withdrawn reports and blocked uids (`config/blocked-uids.txt`) on its own. Switch to reading only
   what changed (by `updatedAt`) when the stats approach that.
2. Decodes each `pairs` string, dropping malformed entries one by one, merges all reporters per provider and
   target system, and checks each set against the index (see [The checks](#the-checks)).
3. Builds the stats row for the night and appends it to the existing `stats.json`.
4. Writes `data/massacre-sets.json` and `ops-aec9172a/stats.json` (the unlisted stats folder) into a checkout
   of `gh-pages`, and commits only if something changed. The sets are sorted, so an unchanged result is
   byte-identical.

Dry run (`--fixtures fixtures/sample`) on one commander's 103 real entries plus a forged and a malformed
one: 33 of 33 real sets published, all indexed. The forged Shinrarta Dezhra → Sol pair was dropped for
distance, the made-up target faction was filtered out, and the malformed entry was skipped.

**Optional tidying:** drop pairs that still cannot be checked against the index after about 6 months. That
is housekeeping, not a judgement about trust.

---

## Cost

Spark (free, no card): Firestore 20K writes/day, 50K reads/day, 1 GiB stored, 10 GiB/month egress. When a
quota runs out, requests are refused, never billed. For uploads that fail silently, that is exactly the
behaviour we want.

| With D daily active installs | Per day |
|---|---|
| Stats writes | D |
| Report writes (only when pairs changed) | well under D |
| Nightly job reads | about D plus the reports that changed |
| Storage | about 300 B per pair per reporter, in MB even at 10K installs |
| Static files | GitHub Pages (100 GB/month soft limit). The sets file is a few hundred KB gzipped, fetched with ETag |

**Spark covers about 10,000 daily active installs.** Writes run out first. On Blaze the same free allowance
applies daily, and beyond it the cost is about $1 a month at 20K daily installs (rates to be confirmed on the
Firestore pricing page). **Stay on Spark.** On Blaze, abuse of the public key becomes a bill, and budget
alerts only notify. Move only when the stats show the app has outgrown Spark.

---

## Before building

- [x] **Distance rule** (2026-11-13): 33 pairs, 1.3 to 9.9 ly. 20 ly stays.
- [x] **Anarchy rule**: 33 of 33 target factions are Anarchies and still in Z. It is now a hard check.
- [x] **Report size**: 33 pairs, 103 entries, 7.3 KB. The limit is 64,000 characters.
- [x] **Index pass**: 119,625 populated systems, 5.9 MB gzipped, no resource sites in the dump. The extract is
      kept in `~/data/ed/massacre-analysis/`. It still has to be cut into the job's input format.
- [ ] Only one commander's journals were in the archive (one FID). A second commander's data would widen the
      sample.
- Throwaway scripts stay out of the repo.

## Firebase setup (next session)

Done 2026-11-13, project `elite-intel-firebase`:

- [x] Project on **Spark**.
- [x] `(default)` Firestore database, Standard edition, multi-region **`nam5`**, created in production mode.
- [x] Anonymous sign-in enabled. **Auto clean-up left off**: it deletes accounts 30 days after creation,
      which would give every install a new uid each month. No Identity Platform upgrade.
- [x] A Web app registered (that is what creates the API key). The key is restricted to **Identity Toolkit
      API, Token Service API, Cloud Firestore API**, with no application restriction (a desktop app cannot
      have one).
- [x] Security rules published, and **13 of 13 live tests pass**: wrong uid, extra field, client timestamp,
      bad OS, second write the same day, payload over 64,000 characters, wrong `format` type, any read,
      unsigned write, and deleting `installs` are refused. Valid creates and deleting `reports` are allowed.
      The update limit is **20 hours**, not 24, so a commander who starts the game a little earlier each
      evening is not refused. The rules should also be committed to the repo, e.g. as `firestore.rules`.
- [x] Service account `nightly-publisher` with **Cloud Datastore User** (it must delete junk). It has **no
      key**. Its reads count against Spark's 50K/day. `reports` is a top-level collection, so the
      `updatedAt` query needs only the automatic single-field index.
- [x] The job lives in the **private** repo `SudoKrondor/elite-intel-nightly`, together with the
      verification index. Nothing derived from the Spansh dump is published except the checked pairs.
- [x] **Workload Identity Federation**: pool `github`, OIDC provider `elite-intel-nightly` (issuer
      `https://token.actions.githubusercontent.com`), condition
      `assertion.repository == 'SudoKrondor/elite-intel-nightly'`. `nightly-publisher` is connected to
      `attribute.repository/SudoKrondor/elite-intel-nightly`. Provider name:
      `projects/102163744490/locations/global/workloadIdentityPools/github/providers/elite-intel-nightly`.
- [x] **Pushing to the site**: the organization has deploy keys switched off, so the job uses a **GitHub
      App**, `elite-intel-nightly-publisher` (App ID 5752566). It is installed on `EliteIntel` only, with
      Contents read and write. The `gh-pages` ruleset `web-site-main-protection` lets this app (and no deploy
      key) bypass its pull-request rule. `master`, `V1.2` and `V1.1-Release` have their own rulesets without
      that exception, so the app cannot push to them. Repo variable `PUBLISHER_APP_ID`, secret
      `PUBLISHER_APP_PRIVATE_KEY`. Each run gets a one-hour token.
- [x] Write the job and its workflow (17 unit tests, a dry run on real data).
- [x] First manual run, 2026-11-14: every step passed (Google sign-in, Firestore read, app token, push).
      The bot committed `20b617c` to `gh-pages`, and `https://www.elite-intel.org/data/massacre-sets.json`
      is live with an empty `sets` list. There are no stats until the first ping arrives.
- [ ] Set up the Action that writes to `gh-pages` (the site already serves `data/exomastery.json` from there).

## The app (V1.2)

**Built 2026-11-14** on branch `V1.2-crowdsourced-massacre-sets`:

- **Migration `12023`** (shared): `global_settings.shareMassacreSets` (off), `community_sync` (one row: the
  uid, the refresh token encrypted with `Cypher`, and when the ping, report and download last succeeded),
  `crowd_massacre_set` (the downloaded sets, replaced wholesale).
- **`elite.intel.community`**:
  - `FirebaseClient`: sign-up, refresh and the three writes over REST
  - `CommunityIdentity`: a new anonymous account when there is no usable refresh token
  - `PairsPayload`: format 1, with entries the job would refuse left out, and the most recently seen kept if
    a ledger ever outgrows 64,000 characters
  - `PublishedFiles`: the sets, with ETag, and `config.json`, where a missing file means everything is on
  - `CommunitySync`: hourly checks, the first two minutes after launch, plus an immediate pass when sharing
    is toggled. Every step fails silently and is retried at the next check.
- **The report covers every commander on the install** (`CommunityManager.shareable` reads the open
  commander's file and each other `cmdr_*.db` through `Database.inCommanderFile`), so a journal import's
  commanders are shared too. A target the commander forgot is still shared: forgetting is taste.
- **The download happens whether or not the commander shares.** The pair search uses the sets only while they
  share (`HuntingGroundManager`), where the commander's own pair wins a tie and replaces a report of the same
  two systems. Without sharing, the sets only let `find_pirate_massacre_missions` add the sharing hint when
  the commander's own record is empty and others reported sets in range.
- **A downloaded set is offered only when** this commander's `hunting_ground` knows the target has resource
  sites (not forgotten), and at least one reported provider faction is still in the provider system. Sets the
  job marked `unindexed` are not stored yet.
- **Speech:** `handler.pirate.pairReportedByOthers` and `handler.pirate.shareHint`, in all 9 languages.
- **UI:** Commander → **Massacre Sets**:
  - an on/off button with a status line beside it, like the Exo-Mastery page
  - **Your sets:** sets you share (distinct provider and target pairs across every commander file) and when
    they were last shared
  - **From other commanders:** sets downloaded; usable here (the ones that pass this install's resource-site
    check, so the ones VEGA can offer); when they were last updated; and commanders contributing
  - "Commanders contributing" is the `contributors` count the nightly job publishes: the installs behind at
    least one published set. It is stored by migration `12024`.
  - refreshed after every sync pass (`CommunitySyncedEvent`)
  - the intro also discloses the stats ping
- **Install ID:** it lives in `database.db`, so Backup/Restore carries it. A token the restoring computer
  cannot decrypt makes a new account, so a restore onto a second PC shows up as a new install.
- **Tests:** 24 new (the encoder, the REST client and published files against WireMock, a full sync pass
  over the in-memory database, and the merge into the pair search). The full suite of 5,707 passes.

**On `gh-pages-1.2` (2026-11-14):**
- The privacy policy, in all six site languages (`wiki/**/Privacy.md`, rebuilt with `node build.js`), now
  covers the stats ping, opt-in massacre sets, Firebase, and how to delete data. It corrects the old
  "nothing leaves the machine offline" and "no data is stored externally".
- The unlisted stats page is `ops-aec9172a/index.html`:
  - hand-written outside `wiki/`, so the build neither rebuilds nor deletes it, and it is not in the sitemap
  - `noindex`, and deliberately not in `robots.txt`
  - it reads `stats.json` from the same folder, which the nightly job writes on `gh-pages`, and shows a "no
    pings yet" note until then

**Not built yet:**
- the EDDN faction ledger, for `unindexed` sets
- `data/config.json` on the site (until then, everything counts as on)

**Round trip:**
1. Import a second commander's journals.
2. Turn sharing on. This uploads at once and covers every commander file.
3. Run the nightly workflow.
4. Restore a database from before the import. That means a new uid and an empty download state.
5. Start the app, turn sharing on, and ask for massacre missions near the imported commander's systems.
   Their targets must also be in this database's `hunting_ground` to be offered.
