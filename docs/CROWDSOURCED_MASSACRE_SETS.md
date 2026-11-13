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
  check.
- **The pirate faction is an Anarchy, as far as anyone has seen.** This is experience, not confirmed, so it
  is a soft check that only lowers a pair's rank. A lawful faction's ship can be wanted and can turn pirate,
  so a ship's status proves nothing about its faction.
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
   elite-intel.org/data/massacre-index.json.gz ◀── app fetches when it changes (rarely)
   elite-intel.org/data/config.json            ◀── app fetches with the sets (kill switch)
   elite-intel.org/<unlisted>/stats.json       ◀── the unlisted stats page reads it
```

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
| `lastActive` | `2026-11-13` | date only, server-checked |

Written at most once a day. **No commander name, FID, rank, or anything about the game.**

### `reports/{uid}`: the install's massacre sets

One document holds **all** of the install's pairs as a map, keyed by `hash(providerSystem|providerFaction|targetSystem)`:

| Field | Note |
|---|---|
| `providerSystem`, `providerSystemAddress` | |
| `providerStation` | optional |
| `providerFaction` | one entry per faction, so the stack depth can be worked out |
| `targetSystem`, `targetSystemAddress`, `targetFaction` | |
| `res` | `{standard, low, high, hazardous}` as the reporter saw them |
| `lastSeen` | date only |
| `updatedAt` | server timestamp, which the nightly job queries on |

**Never uploaded:** MissionID, reward, kill count, exact timestamps. They do nothing for the next commander,
and together they could fingerprint one.

**A whole journal import is one write.** A Firestore document holds up to 1 MiB, which is thousands of pairs.
Writes per day stay at no more than the number of active installs, whatever the import size.

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

## Verification: in each app, against data the forger does not control

The app is open source. **A report cannot be made unforgeable**: journal files are not signed, and any
check the app runs, a forger can copy or skip. So the design does not try to stop bad writes. It makes
honest readers ignore them.

**Checks on the reading side only protect the reader.** Someone who edits the code so every check passes
fools only their own app. Every honest app still runs the real checks on the pairs it downloads.

### The verification index

A one-time pass over the commander's full Spansh dump (2026, 524 GiB, every signal included) produces a
small hosted index. **It runs on the NAS, reading one line at a time. The dump is never pulled over the
network.**

For every populated system (both A and Z are populated, since Z has a pirate faction):

- name, SystemAddress, coordinates
- factions, and each faction's government
- resource site counts by grade

The Bubble should come to a few MB gzipped. It is published as `massacre-index.json.gz` and fetched only when
it changes. It is rebuilt only from a newer dump, which is rarely needed because the data it checks hardly
changes.

### The checks

| Check | Kind | Source |
|---|---|---|
| A and Z exist | hard | index, or the app's own data |
| The provider faction is in A, the target faction is in Z | hard | index, or the EDDN faction ledger |
| Z is within 20 ly of A | hard | coordinates from the index or the app's own data, **never from the report** |
| Z has resource sites | hard | index, or `hunting_ground` |
| The target faction is an Anarchy | soft (rank only) | index, or the EDDN faction ledger |

"The app's own data" means the commander's journal (every FSDJump records the system and its coordinates),
`hunting_ground` (journal plus EDDN `fsssignaldiscovered`, with coordinates), and **a new EDDN faction
ledger**. EDDN `FSDJump` messages carry each system's factions and their governments. The listener receives
them already and keeps only the war and power data. Keeping the factions too covers systems colonised after
the dump.

The one claim nothing can confirm is that A's factions actually offer massacre contracts against Z. To fake
it, a forger has to name real factions in real systems within 20 ly of a real resource site. The worst
outcome is one commander's trip to a genuine resource site, followed by a blacklist.

### No timer

- A pair that passes the checks is **shown**.
- A pair that cannot be checked yet is **kept but hidden**. Storage is not a constraint, so nothing is decided
  by a deadline.
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

- **Firebase Anonymous Auth over REST** (Identity Toolkit `signUp`). Each install gets a uid. The refresh
  token is kept next to the install ID. There is no official client Java SDK. The Admin SDK is privileged and
  is **never shipped**.
- **Security rules:**
  - a uid writes only `installs/{uid}` and `reports/{uid}`
  - a fixed set of fields, with string length and map size limits
  - server timestamps
  - no reads at all
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

A scheduled GitHub Action holding a Firebase service account in the repo's secrets:

1. Reads `reports` documents whose `updatedAt` is later than its last run, and merges them into the
   previous `massacre-sets.json` (read back from `gh-pages`). A full rebuild from every document, done
   occasionally, removes blocked uids.
2. Drops any pair that fails a hard check against the index.
3. Reads `installs` and builds `stats.json`. Up to about 40K installs a full read fits the free read quota.
   Beyond that, it switches to `count()` aggregations.
4. Commits the files to `gh-pages`.

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

- [ ] **Measure the distance rule** against the journals of Alan and his son: every provider-to-target
      distance from their massacre contracts, using the provider coordinates in `massacre_mission` and the
      target's in `hunting_ground`. Confirm 20 ly has room to spare.
- [ ] **Check the Anarchy rule** against the same contracts: look up each target faction's government in the
      dump. If all of them are Anarchy, it can become a hard check.
- [ ] **Build the verification index** from the dump, on the NAS.
- [ ] Throwaway scripts for these stay out of the repo.

## Firebase setup (next session)

- [ ] Create the project on **Spark**.
- [ ] Create Firestore in Native mode. **The location is permanent**, so pick it deliberately.
- [ ] Enable Anonymous sign-in. Check: whether anonymous users count toward the 50K monthly active user
      (MAU) figure, the per-IP limit on anonymous sign-ups, and automatic cleanup of inactive anonymous
      accounts (may need the Identity Platform upgrade).
- [ ] Restrict the web API key to the Identity Toolkit and Firestore APIs.
- [ ] Write and test the security rules (emulator).
- [ ] Create the service account for the nightly job and add it to the repo's secrets.
- [ ] Set up the Action that writes to `gh-pages` (the site already serves `data/exomastery.json` from there).

## What changes in the app (outline, V1.2)

- A **shared table of crowd-sourced pairs** (new `12XXX` migration), separate from `massacre_mission`, with
  each pair's verification state. `findPairs` merges the commander's own pairs with the verified
  crowd-sourced ones.
- An **EDDN faction ledger** (system → factions, governments), filled silently from `FSDJump` like the other
  EDDN ledgers.
- The **verification index**, stored locally and replaced when the hosted one changes.
- An **uploader** (stats ping, and the reports document when sharing is on) and a **daily fetch** (sets, index,
  config), both non-blocking and failing silently.
- A **sharing setting**, off by default, plus the VEGA hint when a search comes up empty.
- **Speech** that marks crowd-sourced sets as reported by others, and localized strings for it.
- **Privacy notes** on the site and in the release notes.
