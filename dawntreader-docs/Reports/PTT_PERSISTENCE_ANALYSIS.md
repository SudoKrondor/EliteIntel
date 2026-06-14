# Push-to-Talk Settings — Persistence Analysis

Analysis only — no code changes. Covers where/how to persist the four
`InputSettingsPanel` push-to-talk settings (enabled flag, controller name,
button index, toggle/hold mode) following existing DB conventions in
`elite.intel.db`.

## 1. Settings to persist

From `app/src/main/java/elite/intel/ui/view/settings/InputSettingsPanel.java`
(currently in-memory only, per its class javadoc "Session-only, no DB
persistence"):

| Field (current, in-memory)        | Type      | Persisted form                         |
|------------------------------------|-----------|-----------------------------------------|
| `pushToTalkEnabled`                | `boolean` | `boolean`                                |
| `selectedDevice` (`Device`)        | `Device`  | controller **name** (`Device.name()`), `String` |
| `selectedButtonIndex`              | `int`     | `int` (0-based SDL button index, `-1` = none) |
| `toggleMode`                       | `boolean` | `boolean` (`true` = toggle, `false` = hold) |

Controller identity is persisted as `Device.name()` rather than `Device.id()`
or the full record — SDL3 assigns `id()` as a runtime enumeration index that
can change across app restarts or device reconnects, while `name()` is the
stable human-readable identifier the user picked. This mirrors how
`SystemSession.getAudioInputDevice()`/`getAudioOutputDevice()` already persist
microphone/speaker selection as device-name strings (see §3).

## 2. Existing single-row settings tables

Three single-row settings tables exist, each with `id` fixed at `1`,
a `get()` query, and an `INSERT OR REPLACE ... VALUES (1, ...)` `save()`:

- **`global_settings`** (created `00051__schema.sql`) — 10 boolean
  FTL-automation toggles (`autoSpeedUpForFtl`, `autoLightsForFtl`, ...).
  Backed by `GlobalSettingsDao` / `GlobalSettingsManager`. Thematically this
  is "ship automation while jumping" — not a good fit for input/voice-mode
  settings.
- **`player` table** (created `00001__schema.sql`, extensively extended) —
  commander/gameplay state, backed by `PlayerDao` / `PlayerSession`. Holds
  the `*AnnouncementOn` toggles (radio, mining, navigation, discovery, route,
  radar contact).
- **`game_session` table** (created `00003__schema.sql`, extensively
  extended) — app/runtime configuration, backed by `GameSessionDao` /
  `SystemSession`. Holds voice/TTS settings, API keys, local-LLM config,
  STT thresholds, audio I/O device names, language, and the
  `privacyModeOn` / `conversationModeOn` mode toggles.

## 3. Pattern examples requested

### `conversationModeOn` (game_session) — boolean toggle, added via migration

- **Migration** `00049__schema.sql` (one line):
  ```sql
  alter table game_session
      add column conversationModeOn boolean default false;
  ```
  Followed by `00050__schema.sql`, a one-time backfill (`UPDATE game_session
  SET conversationModeOn = false`) — not needed for a brand-new column with a
  `default` clause, since SQLite applies the default to the existing row.

- **DAO** (`GameSessionDao.java`):
  - `GameSession` POJO field: `private boolean conversationModeOn;` with
    `isConversationModeOn()` / `setConversationModeOn(boolean)`.
  - Added to the `INSERT OR REPLACE` column list and `:conversationModeOn`
    bind parameter in `save()`.
  - Added `session.setConversationModeOn(rs.getBoolean("conversationModeOn"))`
    in `GameSessionMapper.map()`.

- **Manager / session facade** (`SystemSession.java`):
  ```java
  public void setConversationalMode(boolean b) {
      Database.withDao(GameSessionDao.class, dao -> {
          GameSessionDao.GameSession session = dao.get();
          session.setConversationModeOn(b);
          dao.save(session);
          return Void.TYPE;
      });
  }

  public boolean conversationalModeOn() {
      return Database.withDao(GameSessionDao.class, dao -> dao.get().isConversationModeOn());
  }
  ```
  Simple read-modify-write: `dao.get()` the whole row, mutate one field,
  `dao.save()` writes the whole row back via `INSERT OR REPLACE`.

### `audioInputDevice` / `audioOutputDevice` (game_session) — String, device-name precedent

- **Migration** `01001__schema.sql`:
  ```sql
  ALTER TABLE game_session
      ADD COLUMN audioInputDevice VARCHAR(256);

  ALTER TABLE game_session
      ADD COLUMN audioOutputDevice VARCHAR(256);
  ```
- Same DAO/POJO/mapper/save additions as above, types `String`.
- `SystemSession.setAudioOutputDevice(String device)` normalizes blank to
  `null` before saving:
  ```java
  session.setAudioOutputDevice(device == null || device.isBlank() ? null : device);
  ```
  This is the precedent for persisting "name of a selected hardware device"
  as a nullable `VARCHAR` — directly applicable to the PTT controller name.

### `radarAnnouncementOn` (player) — boolean toggle, added via migration

- **Migration** `00044__schema.sql`:
  ```sql
  alter table player
      add column radarAnnouncementOn boolean default false;
  ```
- **DAO** (`PlayerDao.java`): same shape as `conversationModeOn` —
  `private boolean radarAnnouncementOn;`, `isRadarAnnouncementOn()` /
  `setRadarAnnouncementOn(boolean)`, added to `save()`'s column list and
  `PlayerMapper`.
- **`PlayerSession.java`** (line ~770): same read-modify-write
  `Database.withDao(PlayerDao.class, ...)` getter/setter pair as
  `SystemSession.conversationalModeOn()`.

Both examples confirm a single, consistent pattern across `game_session` and
`player`: add column(s) via a new numbered migration with sensible
`default`s, extend the DAO's POJO + row mapper + `save()` SQL, then add a
thin getter/setter pair on the session facade that does
`dao.get()` → mutate → `dao.save()`.

## 4. Recommendation: extend `game_session`

**Extend the existing `game_session` table** with four new columns, rather
than creating a new table. Rationale:

- **Thematic fit.** `game_session` already owns the *exact* underlying state
  PTT manipulates — `privacyModeOn` is the sleep/listening flag that
  `InputSettingsPanel.toggleSleepWake()` / `wakeUp()` / `sleep()` already flip
  via `SystemSession.stopStartListening(boolean)`. `conversationModeOn` is
  another "how does voice input get triggered" mode toggle living in the same
  table. PTT settings are a third variation on the same theme: "how is the
  AI's listening state controlled."
- **Direct precedent for every field type needed:**
  - `pushToTalkEnabled` (boolean) → exactly like `conversationModeOn`.
  - `pushToTalkControllerName` (String) → exactly like `audioInputDevice` /
    `audioOutputDevice` (device-name `VARCHAR`, nullable, blank-normalized to
    `null`).
  - `pushToTalkButtonIndex` (int, `-1` = none) → new shape (first `INTEGER`
    with a non-boolean default in this table), but trivial — same
    `INSERT OR REPLACE` / mapper mechanics as every other column.
  - `pushToTalkToggleMode` (boolean) → exactly like `conversationModeOn`,
    default `true` (matches `InputSettingsPanel`'s current
    `toggleModeRadio = new JRadioButton(..., true)` default).
- **No new DAO/Manager class.** A dedicated `push_to_talk_settings` table
  would require a new `CREATE TABLE` + seed-row migration, a new
  `PushToTalkSettingsDao` interface (mapper, `get()`, `save()`), and either a
  new `PushToTalkSettingsManager` singleton or extra methods bolted onto
  `SystemSession` anyway — strictly more code for four scalar fields that are
  conceptually part of "session-level input configuration," which is what
  `game_session` already models.
- **Single source of truth for "how is listening controlled".** Any future
  code that needs to reason about the AI's input-trigger configuration (PTT
  vs. conversation mode vs. privacy/sleep) reads one table instead of two.

The one counter-consideration is that `game_session` is already a wide table
(38 columns spanning AI personality, API keys, LLM config, audio devices,
language, and mode toggles). Four more columns is a marginal addition to an
already-wide "app configuration" table and consistent with how
`conversationModeOn`/`audioInputDevice`/`audioOutputDevice` were added
incrementally — it does not change the table's character, just continues the
existing pattern. A dedicated table would only be worth the extra
boilerplate if PTT settings were expected to grow into a much larger,
independently-versioned settings group (e.g. per-device button maps for
multiple actions) — which is out of scope for these four fields.

## 5. Concrete plan (not implemented — analysis only)

### 5.1 New migration: `app/src/main/resources/db-migration/01004__schema.sql`

Next available number after `01003__schema.sql` (the most recent migration,
which only adds the `neutron_star_route` table). Four `ALTER TABLE`
statements, following the `00049`/`01001` style:

```sql
alter table game_session
    add column pushToTalkEnabled boolean default false;

alter table game_session
    add column pushToTalkControllerName VARCHAR(256);

alter table game_session
    add column pushToTalkButtonIndex integer default -1;

alter table game_session
    add column pushToTalkToggleMode boolean default true;
```

Defaults chosen to match `InputSettingsPanel`'s current in-memory defaults
(`pushToTalkEnabled = false`, `selectedButtonIndex = -1`,
`toggleMode = true`); `pushToTalkControllerName` is nullable (no controller
selected yet), matching `audioInputDevice`/`audioOutputDevice`.

### 5.2 `GameSessionDao.java` changes

1. Add four fields + getters/setters to the `GameSession` POJO:
   `boolean pushToTalkEnabled`, `String pushToTalkControllerName`,
   `int pushToTalkButtonIndex`, `boolean pushToTalkToggleMode`.
2. Add the four columns + `:bind` parameters to the `INSERT OR REPLACE INTO
   game_session (...)` statement in `save()`.
3. Add the four `rs.get*("...")` calls to `GameSessionMapper.map()`
   (`getBoolean`, `getString`, `getInt`, `getBoolean`).

### 5.3 `SystemSession.java` changes

Add four read-modify-write getter/setter pairs following the
`conversationalModeOn()` / `setConversationalMode()` shape:

- `isPushToTalkEnabled()` / `setPushToTalkEnabled(boolean)`
- `getPushToTalkControllerName()` / `setPushToTalkControllerName(String)`
  — normalize blank → `null` on save, matching `setAudioOutputDevice()`.
- `getPushToTalkButtonIndex()` / `setPushToTalkButtonIndex(int)`
- `isPushToTalkToggleMode()` / `setPushToTalkToggleMode(boolean)`

### 5.4 `InputSettingsPanel.java` changes (future, not analyzed in depth here)

High-level shape only:

- `initData()` would load the four persisted values from
  `SystemSession.getInstance()` and apply them to the UI (checkbox state,
  radio selection, and the volatile mirror fields), plus attempt to
  re-select the persisted controller by **name** in
  `refreshControllerCombo()` (currently that method only re-selects by
  matching `Device.id()` against `selectedDevice`, since it's only used for
  in-session reconnects — a name-based lookup against the persisted string
  would be the new first-run path).
- Each handler (`onPushToTalkToggled`, `onControllerSelected`,
  `onButtonSelected`, the toggle/hold radio listeners) would additionally
  call the corresponding `SystemSession` setter to persist the change.
- The class javadoc ("Session-only, no DB persistence") would need updating
  to reflect the new persistence.

This keeps the change additive and localized: one migration file, a handful
of fields/columns on an existing DAO, four getter/setter pairs on an existing
session facade, and wiring in `InputSettingsPanel` — no new tables, DAOs, or
manager classes.
