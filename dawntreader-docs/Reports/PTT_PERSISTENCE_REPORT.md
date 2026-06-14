# Push-to-Talk Settings — Persistence Implementation

Implements the plan from `dawntreader-docs/Reports/PTT_PERSISTENCE_ANALYSIS.md`: persists the
four `InputSettingsPanel` push-to-talk settings (enabled flag, controller name, button index,
toggle/hold mode) by extending the existing `game_session` table, following the
`conversationModeOn` / `audioInputDevice` precedents.

## 1. New migration: `app/src/main/resources/db-migration/01004__schema.sql`

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

Defaults match `InputSettingsPanel`'s prior in-memory defaults (`pushToTalkEnabled = false`,
`selectedButtonIndex = -1`, `toggleMode = true`); `pushToTalkControllerName` is nullable, matching
`audioInputDevice` / `audioOutputDevice`.

## 2. `GameSessionDao.java`

- `GameSession` POJO: added `boolean pushToTalkEnabled`, `String pushToTalkControllerName`,
  `int pushToTalkButtonIndex = -1`, `boolean pushToTalkToggleMode = true`, with
  `is`/`get`/`set` accessors following the existing style (`isConversationModeOn()` /
  `setConversationModeOn(...)`).
- `save()`'s `INSERT OR REPLACE` column list and `VALUES` clause extended with the four new
  columns/bind parameters.
- `GameSessionMapper.map()` extended with
  `rs.getBoolean("pushToTalkEnabled")`, `rs.getString("pushToTalkControllerName")`,
  `rs.getInt("pushToTalkButtonIndex")`, `rs.getBoolean("pushToTalkToggleMode")`.

## 3. `SystemSession.java`

Added four read-modify-write getter/setter pairs, same shape as `conversationalModeOn()` /
`setConversationalMode()`:

- `isPushToTalkEnabled()` / `setPushToTalkEnabled(boolean)`
- `getPushToTalkControllerName()` / `setPushToTalkControllerName(String)` — blank strings
  normalized to `null` on save, matching `setAudioOutputDevice()`.
- `getPushToTalkButtonIndex()` / `setPushToTalkButtonIndex(int)`
- `isPushToTalkToggleMode()` / `setPushToTalkToggleMode(boolean)`

## 4. `InputSettingsPanel.java`

- Class javadoc no longer states "Session-only, no DB persistence."
- `initData()` now loads `pushToTalkEnabled`, `toggleMode`, and the persisted controller name
  from `SystemSession`, applies them to the checkbox/radio buttons, starts `DeviceService` if PTT
  was enabled, and calls a new `reconcileControllerSelection()` helper to re-select the persisted
  controller by name and restore the persisted button index.
- All four settings now persist via `SystemSession` on every change:
  - `onPushToTalkToggled()` → `setPushToTalkEnabled(boolean)`
  - `onControllerSelected()` → `setPushToTalkControllerName(String)`
  - `onButtonSelected()` → `setPushToTalkButtonIndex(int)`
  - the toggle/hold radio button listeners → `setPushToTalkToggleMode(boolean)`

### Re-selecting the controller by name

`refreshControllerCombo()` previously only re-selected a device by matching `Device.id()` against
the in-memory `selectedDevice` (for in-session reconnects). It now has a second branch: when
`selectedDevice` is `null` (i.e. nothing selected yet — the initial-load case), it looks for a
connected device whose `name()` equals the persisted `pushToTalkControllerName` and selects that.

### Avoiding self-clobbering on combo refresh

A new field, `suppressPersistence`, and helper `reconcileControllerSelection()` were added to
solve a correctness problem inherent to "persist on every handler call":

- `onControllerSelected()` / `onButtonSelected()` aren't only fired by direct user clicks — they
  also fire as a side effect of `refreshControllerCombo()` / `populateButtonCombo()` rebuilding
  the combo boxes (on initial load, and on `DeviceConnectedEvent` / `DeviceDisconnectedEvent`).
  Rebuilding the button combo always passes through a transient "no button selected" (`-1`)
  state.
- Without a guard, the very first SDL device-enumeration pass at startup (or a momentary
  controller disconnect/reconnect) would fire `onButtonSelected()` with `-1` and immediately
  overwrite the user's saved `pushToTalkButtonIndex` — and, on a disconnect with no matching
  device, `onControllerSelected()` would overwrite `pushToTalkControllerName` with `null` — even
  though the user never touched the controls.
- `reconcileControllerSelection()` wraps `refreshControllerCombo()` (plus restoring the button
  combo to the previously-known button index) in `suppressPersistence = true`, and
  `onDeviceDisconnected` does the same around its combo-reset logic. `onControllerSelected()` /
  `onButtonSelected()` only call into `SystemSession` when `suppressPersistence` is `false`,
  i.e. when the change came from a genuine user interaction with the combo/checkbox/radio
  buttons.

`initData()`, `onPushToTalkToggled()` (when enabling), and the `DeviceConnectedEvent` handler now
call `reconcileControllerSelection()` instead of the raw `refreshControllerCombo()`.

## 5. Build

```
./gradlew shadowJar
```

**BUILD SUCCESSFUL in 11s** (7 actionable tasks: 3 executed, 4 up-to-date).

## 6. Files changed

- `app/src/main/resources/db-migration/01004__schema.sql` (new)
- `app/src/main/java/elite/intel/db/dao/GameSessionDao.java`
- `app/src/main/java/elite/intel/session/SystemSession.java`
- `app/src/main/java/elite/intel/ui/view/settings/InputSettingsPanel.java`
