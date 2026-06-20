# Binding Conflict Detection Audit

**Date:** 2026-06-17
**Branch audited:** `V1.1-KAN-6-push-to-talk-dawntreader`
**Scope:** Read-only. No source files were modified during this audit.

---

## 1. `binding_conflicts` Table and Seed Data

### Schema

The `binding_conflicts` table is defined in migration `00051__schema.sql` (not `00038` as the prior
audit summary indicated — the prior audit extracted the table definition accurately but gave the
wrong migration number):

```sql
CREATE TABLE IF NOT EXISTS binding_conflicts
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    conflict_key TEXT NOT NULL UNIQUE,
    description  TEXT NOT NULL
);
```

### Seed data

Migration `00051__schema.sql` seeds twelve rows at schema-create time:

```sql
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('QuitCamera|UI_Back', 'UI Back and Quit Camera share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('ExitSettlementPlacementCamera|UI_Back',
        'UI Back and Exit Settlement Placement Camera share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('DownThrustButton|UI_Down', 'UI Down and Down Thrust Button share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('FStopDec|UI_Left', 'UI Left and FStop Dec share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('CyclePreviousSubsystem|UI_Left', 'UI Left and Cycle Previous Subsystem share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('CycleNextSubsystem|UI_Right', 'UI Right and Cycle Next Subsystem share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('FStopInc|UI_Right', 'UI Right and FStop Inc share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('UI_Up|UpThrustButton', 'UI Up and Up Thrust Button share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('ChangeConstructionOption|Hyperspace',
        'Hyperspace and Change Construction Option share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('ToggleDriveAssist|ToggleFlightAssist',
        'Toggle Drive Assist and Toggle Flight Assist share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('ToggleAdvanceMode|ToggleDriveAssist',
        'Toggle Drive Assist and Toggle Advance Mode share a key and may interfere');
INSERT INTO binding_conflicts (conflict_key, description)
VALUES ('SelectTargetsTarget|ToggleFreeCam', 'Select Targets Target and Toggle Free Cam share a key and may interfere');
```

**Critical observation:** These twelve seed rows are **bootstrapped at migration time and then
immediately superseded at runtime.** The live detection logic (see Section 2) does a full
diff against persisted state on every check cycle: it adds rows that are newly detected and
removes rows that are no longer current. The seed rows therefore only survive if the live
detection independently rediscovers those same conflict pairs from the user's actual `.binds`
file. If the user's file does not have, say, `UI_Up` and `UpThrustButton` on the same key, that
seed row will be deleted on the first run of `checkForConflictsAndPersist()`.

The `conflict_key` values in the seed data use raw game action-ID strings (e.g.
`"ToggleDriveAssist"`, `"UI_Back"`), not humanized labels. This is correct — they are stable
internal identifiers.

### What `BindingConflictDao` and `BindingConflictManager` do

`BindingConflictDao` is a JDBI3 SQL Object interface with four operations:

```java
void save(@BindBean ConflictRecord record);      // INSERT OR IGNORE
void remove(@Bind("conflictKey") String conflictKey);
List<ConflictRecord> listAll();
void clear();
```

`BindingConflictManager` is a singleton thin wrapper over the DAO:

```java
public void save(String conflictKey, String description)
public void remove(String conflictKey)
public List<BindingConflictDao.ConflictRecord> getConflicts()
```

The `save` uses `INSERT OR IGNORE`, so duplicate writes on successive check cycles are silently
discarded. There is no `clear()` path called at runtime — the live detection code only calls
`save()` for newly detected conflicts and `remove()` for conflicts that have disappeared. The
`clear()` method exists but is not called from `BindingsMonitor`.

---

## 2. Conflict Detection Logic

### Entry point: `BindingsMonitor.checkForConflictsAndPersist()`

This is the sole conflict-detection path in the codebase. Full method:

```java
public List<String> checkForConflictsAndPersist() {
    List<String> newDescriptions = new ArrayList<>();
    Map<String, KeyBindingsParser.KeyBinding> currentBindings = getBindings();
    if (currentBindings == null || currentBindings.isEmpty())
        return newDescriptions;

    // Invert: keyCombo → all action names sharing it
    Map<String, List<String>> byCombo = new HashMap<>();
    for (Map.Entry<String, KeyBindingsParser.KeyBinding> e : currentBindings.entrySet()) {
        String combo = normalizeCombo(e.getValue());
        if (!combo.isEmpty())
            byCombo.computeIfAbsent(combo, k -> new ArrayList<>()).add(e.getKey());
    }

    // Find conflicts: for each distinct GameCommand binding, check what else shares its key
    Set<String> currentConflictKeys = new HashSet<>();
    Map<String, String> currentConflictDescriptions = new LinkedHashMap<>();
    Set<String> processedCombos = new HashSet<>();

    for (Bindings.GameCommand cmd : Bindings.GameCommand.values()) {
        String gameBinding = cmd.getGameBinding();
        KeyBindingsParser.KeyBinding kb = currentBindings.get(gameBinding);
        if (kb == null)
            continue;

        String combo = normalizeCombo(kb);
        if (!processedCombos.add(combo))
            continue; // same combo already checked via another GameCommand

        List<String> sharingActions = byCombo.getOrDefault(combo, List.of());
        for (String other : sharingActions) {
            if (other.equals(gameBinding))
                continue;
            if (BindingConflictRules.isSafeOverlap(gameBinding, other))
                continue;

            String conflictKey = BindingConflictRules.makeKey(gameBinding, other);
            if (currentConflictKeys.add(conflictKey)) {
                currentConflictDescriptions.put(conflictKey, BindingConflictRules.describe(gameBinding, other));
            }
        }
    }

    // Diff against persisted state
    Set<String> persistedKeys = new HashSet<>(
            conflictManager.getConflicts().stream()
                    .map(r -> r.getConflictKey())
                    .toList());

    for (Map.Entry<String, String> entry : currentConflictDescriptions.entrySet()) {
        if (!persistedKeys.contains(entry.getKey())) {
            conflictManager.save(entry.getKey(), entry.getValue());
            newDescriptions.add(entry.getValue());
        }
    }

    for (String persisted : persistedKeys) {
        if (!currentConflictKeys.contains(persisted)) {
            conflictManager.remove(persisted);
        }
    }

    return newDescriptions;
}
```

### Input: what `currentBindings` contains

`currentBindings` is the result of `KeyBindingsParser.parseBindings()`, which:

1. Calls `parseBindingSlots()` to get keyboard-usable Primary/Secondary pairs per action.
2. For each action, picks **Primary if non-null, else Secondary** — a single `KeyBinding` per
   action name.

The `KeyBinding` record:

```java
public class KeyBinding {
    public String key;       // e.g. "Key_E"
    public String[] modifiers; // e.g. ["Key_LeftControl"]
    public boolean hold;
}
```

**Consequence:** The map passed into conflict detection has at most one `KeyBinding` per action
name. If an action has both a keyboard Primary and a keyboard Secondary, only the Primary enters
the conflict check. The Secondary is silently discarded before conflict detection runs.

### Combo normalization

```java
private String normalizeCombo(KeyBindingsParser.KeyBinding kb) {
    if (kb == null || kb.key == null || kb.key.isBlank() || kb.key.equals("Key_"))
        return "";
    if (kb.modifiers == null || kb.modifiers.length == 0)
        return kb.key;
    String[] sorted = kb.modifiers.clone();
    Arrays.sort(sorted);
    return kb.key + "|" + String.join("|", sorted);
}
```

The combo string is `key` for unmodified bindings (e.g. `"Key_E"`), or
`key|modifier1|modifier2|...` sorted for modifiers (e.g.
`"Key_E|Key_LeftControl"`). This means **modifier-differentiated chords are treated as
distinct combos** — `Key_E` and `Key_LeftControl+Key_E` are different strings and will not
collide with each other in the `byCombo` map.

### What the detection actually compares

The algorithm:

1. Builds `byCombo` — an inverted index of `normalizedCombo → [action names]` across the
   **entire** keyboard-usable binding map (all actions, not just `GameCommand` ones).
2. Iterates only `Bindings.GameCommand` enum entries as the **anchor** set. For each
   `GameCommand` that has a live keyboard binding, it looks up which other actions share the
   same normalized combo.
3. For each sharing pair `(gameBinding, other)`, calls `BindingConflictRules.isSafeOverlap()`
   to filter out known-safe overlaps.
4. If not safe, records the pair.

This means the check is **asymmetric in scope**: one side of each conflict pair must be a
`GameCommand` action, but the other side can be any action in the entire `.binds` file. A
conflict between two non-`GameCommand` actions is never detected.

### `BindingConflictRules.isSafeOverlap()`

```java
public static boolean isSafeOverlap(String a, String b) {
    if (isSubStateModeAction(a) || isSubStateModeAction(b)) return true;
    return !vehicleStateOf(a).equals(vehicleStateOf(b));
}
```

Two suppression mechanisms:

**Sub-state mode suppression** — if either action name starts with one of these prefixes, the
overlap is considered safe:
`FreeCam`, `MoveFreeCam`, `CamTranslate`, `CamYaw`, `CamZoom`, `FixCamera`, `Vanity`,
`PhotoCamera`, `MovePlacement`, `Placement`, `GalnetAudio`, `ExplorationFSS`, `ExplorationSAA`.

**Vehicle-state suppression** — actions are classified into three mutually-exclusive states:

```java
private static String vehicleStateOf(String action) {
    if (action.endsWith("_Buggy")) return "buggy";
    if (action.contains("Humanoid")) return "humanoid";
    return "ship";
}
```

If the two actions are in different vehicle states, the overlap is suppressed. So
`ToggleCargoScoop` (ship) and `ToggleCargoScoop_Buggy` (buggy) sharing a key is allowed —
the player can only be in one state at a time.

### Modifier awareness

**Yes, the system is modifier-aware.** Two bindings sharing the same base key but with
different modifier combinations produce **different** normalized combo strings, so they do not
appear together in `byCombo` and generate no conflict. For example:

- `LandingGearToggle` on `Key_G` (no modifier) → combo `"Key_G"`
- `ShipSpotLightToggle` on `Key_LeftControl+Key_G` → combo `"Key_G|Key_LeftControl"`

These two are not a conflict. The check correctly handles this case.

**Limitation:** The modifier check only covers modifiers on the **selected slot** (Primary, since
Primary wins). If action A has Primary=`Ctrl+G` and action B has Secondary=`Ctrl+G`, and both
also have Primaries that don't collide, no conflict is detected — because Secondary slots are
discarded before conflict detection runs (see "Input" above).

### Game-mode (vehicle-state) context awareness

**Partially, but with gaps.** The three-state vehicle model (`ship` / `buggy` / `humanoid`) is
sound for the split between ship, SRV, and on-foot modes. The suppression via suffix `_Buggy`
and substring `Humanoid` correctly identifies most cross-mode pairs.

**Gap 1 — Sub-state suppression is overly broad.** `isSubStateModeAction()` suppresses any
overlap involving an `ExplorationFSS*` or `ExplorationSAA*` action. But several FSS actions
(e.g. `ExplorationFSSEnter` itself) can be genuinely dangerous if shared with the wrong key,
since the player must press a key to enter FSS mode — that key could trigger something else.
The current logic blanket-suppresses all FSS actions regardless of what they share with.

**Gap 2 — No awareness of UI-overlay modes vs. normal flight.** Elite Dangerous has additional
mode layers beyond the three vehicle states: the galaxy/system map overlay, the station menu
stack, the comms panel, the role panel, etc. The detection does not model these. For example,
`FocusLeftPanel` and `UI_Back` on the same key: opening the left panel would also close a panel
if one is already open. This is a real conflict, but since neither action is in a different
vehicle state or sub-state prefix, it depends entirely on whether it happens to appear in the
`DESCRIPTIONS` map in `BindingConflictRules`.

**Gap 3 — No `ToggleOn` awareness.** The XML parser never reads the `<ToggleOn>` element. An
action that fires only on toggle-off vs. one that fires on any keypress cannot be distinguished.
Conflicts involving Toggle behavior cannot be flagged or suppressed based on actual game
semantics.

**Gap 4 — No `Hold` awareness.** Both `KeyBindingsParser` and `normalizeCombo` extract the
`hold` flag from the binding, but `normalizeCombo` does not include `hold` in the combo string.
Two actions sharing the same `Key_G` with one requiring a hold and one requiring a tap are
treated as fully conflicting. In practice the game engine distinguishes hold from tap, so this
could generate a false positive.

### `BindingConflictRules.DESCRIPTIONS` — the curated description table

`BindingConflictRules` also contains a static `DESCRIPTIONS` map of known-dangerous pairs with
hand-written, action-specific warning text:

```java
put("UI_Back", "ToggleFlightAssist", "Closing a panel will toggle flight assist");
put("UI_Back", "DeployHardpointToggle", "Closing a panel will deploy or retract hardpoints");
put("UI_Back", "ToggleCargoScoop", "Closing a panel will toggle the cargo scoop");
put("UI_Back", "LandingGearToggle", "Closing a panel will toggle landing gear");
put("UI_Back", "ShipSpotLightToggle", "Closing a panel will toggle ship lights");
put("UI_Back", "NightVisionToggle", "Closing a panel will toggle night vision");
put("UI_Back", "Supercruise", "Closing a panel will engage or exit supercruise");
put("UI_Back", "Hyperspace", "Closing a panel will initiate a hyperspace jump");
put("UI_Back", "RecallDismissShip", "Closing a panel will recall or dismiss the ship");
put("UI_Toggle", "ToggleFlightAssist", "Toggling the UI will toggle flight assist");
put("UI_Toggle", "DeployHardpointToggle", "Toggling the UI will deploy or retract hardpoints");
put("UI_Toggle", "ToggleCargoScoop", "Toggling the UI will toggle the cargo scoop");
put("UI_Toggle", "NightVisionToggle", "Toggling the UI will activate night vision");
put("UI_Toggle", "Supercruise", "Toggling the UI will engage or exit supercruise");
put("DeployHardpointToggle", "LandingGearToggle", "Deploying hardpoints will also toggle landing gear");
put("DeployHardpointToggle", "ToggleCargoScoop", "Deploying hardpoints will also toggle the cargo scoop");
put("LandingGearToggle", "ToggleCargoScoop", "Toggling landing gear will also toggle the cargo scoop");
put("Supercruise", "Hyperspace", "Supercruise and hyperspace share a key - jump type will depend on target");
```

This is used only for the description text shown to the user. It does **not** control which
pairs are flagged — that is determined by the combo-matching algorithm. If a pair matches
combos but is not in `DESCRIPTIONS`, `describe()` falls back to the generic:

```java
return StringUtls.humanizeBindingName(a) + " and " + StringUtls.humanizeBindingName(b)
       + " share a key and may interfere";
```

---

## 3. `BindingsMonitor` — Trigger and Persistence

### When `checkForConflictsAndPersist()` runs

The monitor runs on a dedicated background thread (`BindingsMonitorThread`) started via
`startMonitoring()`. The thread uses a `WatchService` polling loop on the bindings directory.

**On initial startup:** `parseAndUpdateBindings()` is called immediately (before the watch
loop begins), but `checkForConflictsAndPersist()` is **not** called at startup — it is only
called inside the watch event loop.

**On each file-system event cycle:** After processing any `ENTRY_MODIFY` or `ENTRY_CREATE`
events for `.binds` files, the monitor unconditionally calls:

```java
checkForMissingBindingsAndPersist();
checkForConflictsAndPersist();
```

This means the conflict check runs **every time the WatchService cycle processes at least one
event** — even if the event was for a different `.binds` file than the active one, and even if
the bindings were not actually reloaded. If the directory is quiet (no events), the check does
not run — the poll timeout is 1 second, but the check block is inside `for (WatchEvent<?> event
: key.pollEvents())` which only executes if there were events.

**Important:** `checkForConflictsAndPersist()` is also callable externally via `KeyBindCheck`.
`KeyBindCheck.check()` calls both `checkForMissingBindingsAndPersist()` and
`checkForConflictsAndPersist()` on the monitor instance. `KeyBindCheck` appears to be used from
at least one other code path (the voice command pipeline's bind-check flow).

### What persists to the DB

The full current set of detected conflicts is maintained in the DB as a running diff:

- New conflicts (not in the persisted set) → `conflictManager.save()` (`INSERT OR IGNORE`).
- Stale conflicts (in the persisted set but not in the current detection) → `conflictManager.remove()`.

The result is that the DB always reflects the current state of the user's `.binds` file, not a
historical accumulation. The seed rows from migration time are removed on the first check cycle
if they are not present in the user's actual bindings.

### What gets published to the EventBus

`checkForConflictsAndPersist()` itself does **not** publish any EventBus event. It only returns
a `List<String>` of newly detected conflict descriptions. The callers decide what to do:

- `BindingsMonitor.monitorBindings()` (the watch loop): ignores the return value — no event published from the watch loop path.
- `KeyBindCheck.check()`: if `newConflicts` is non-empty, publishes:
  - `AiVoxResponseEvent` (TTS announcement of the count, e.g. "You have 2 binding conflicts")
  - `AppLogEvent` per conflict (appears in the app log panel)

There is **no** `BindingsUpdatedEvent` or similar published specifically for conflicts. The UI
does not subscribe to any conflict-specific event.

---

## 4. UI Presentation

### No dedicated conflict UI exists in the current codebase

`BindingsTabPanel` (`app/src/main/java/elite/intel/ui/screen/BindingsTabPanel.java`) is the
only bindings-related UI panel. It does **not**:

- Import or reference `BindingConflictManager` or `BindingConflictDao`.
- Display the `binding_conflicts` table contents anywhere.
- Show per-row conflict indicators on the Used Bindings table.
- Maintain a "conflicts" third tab or footer badge.

The only conflict-related UI is:

1. **TTS announcement** via `KeyBindCheck.check()` → `AiVoxResponseEvent` — the app speaks a
   count of new conflicts aloud.
2. **App log entries** via `AppLogEvent("Binding conflict: " + description)` — each new conflict
   description appears in the in-app log panel.

The `AssignKeyboardBindingDialog` has its own inline "already in use" warning, but this
operates via `KeyboardKeyAvailabilityService.isKeyOccupiedByOtherSlot()` — a completely
separate code path that checks raw key occupation across the whole `.binds` file and is not
related to the `binding_conflicts` table at all.

**Summary:** Conflict detection produces DB rows and (on new detections) TTS + log entries.
There is no visual conflict overlay, no highlighting of conflicting rows in the bindings table,
and no dedicated conflict list anywhere in the UI.

---

## 5. Accuracy Assessment

### 5a. False Positives

**FP-1: Hold vs. Tap differentiation**

The `normalizeCombo()` method produces the same string for `(key="Key_G", hold=false)` and
`(key="Key_G", hold=true)`. If action A is bound to hold-`Key_G` and action B to tap-`Key_G`,
they are flagged as conflicting. Elite Dangerous distinguishes these at the input layer, so
this is a false positive. The fix would be to include `hold` in the normalized combo:
`"Key_G|HOLD"` vs. `"Key_G"`.

**FP-2: Primary-on-one vs. Secondary-on-another (only when both happen to be keyboard)**

Consider: action A has Primary=`Key_F` and Secondary=`Key_G`. Action B has Primary=`Key_G`.
`parseBindings()` selects Primary wins, so action A enters the conflict map with `Key_F`, not
`Key_G`. No conflict is detected between A and B. This is actually **correct** (no false
positive here) — see FN-2 below for the false negative version.

The false positive version: if action A has Primary=`Key_G` and Secondary=`Key_H`, and action
B has Primary=`Key_G`. Both enter the map with `Key_G` (A's Primary). This is correctly
flagged as a real conflict. No false positive here either.

**FP-3: Sub-state mode actions that are not actually safe**

`isSubStateModeAction()` returns `true` for any action starting with `ExplorationFSS`.
`ExplorationFSSEnter` is the key the player presses to *enter* FSS mode from normal flight.
If `ExplorationFSSEnter` shares a key with, say, `DeployHardpointToggle`, the current logic
suppresses this as a safe overlap. In reality, pressing the FSS key while hardpoints are
deployed (or vice versa) could cause unintended behavior. This is a false positive suppression
— the system incorrectly deems the pair safe when it may not be.

**FP-4: Non-`GameCommand` pair suppressed by a `GameCommand` anchor**

The detection only flags pairs where at least one action is a `GameCommand`. If two
non-`GameCommand` actions share a key, they are never flagged. This is a false negative (see
FN-3), not a false positive.

### 5b. False Negatives

**FN-1: Secondary keyboard slot conflicts are invisible**

`parseBindings()` selects Primary over Secondary. If action A has only a Secondary keyboard
binding (Primary is unbound or HOTAS), and action B has only a Secondary binding on the same
key, both enter the conflict map as their Secondaries — they will be compared correctly.

But if action A has Primary=`Key_F` and Secondary=`Key_G`, and action B has Primary=`Key_G`,
action A enters the map as `Key_F` (Primary wins). The conflict between A's Secondary and B's
Primary is **never detected.** In real gameplay both bindings fire — the Secondary on one
action and Primary on the other are simultaneously active.

This is the most significant systematic false negative. The fix requires conflict detection to
run over all `(Primary, Secondary, Primary of other, Secondary of other)` combinations, not
just the single winner per action.

**FN-2: Both-sides non-`GameCommand` conflicts**

The outer loop in `checkForConflictsAndPersist()` iterates `Bindings.GameCommand.values()` as
anchors. Two actions that both fall outside `GameCommand` (e.g., two obscure camera actions
not in the enum) can share a key and will never be flagged, regardless of how dangerous that
sharing is.

**FN-3: No conflict detection at all before the first watch event**

`checkForConflictsAndPersist()` is not called during initial startup `parseAndUpdateBindings()`
— only `parseAndUpdateBindings()` runs at start. The conflict check only fires after the first
`WatchService` event. On a quiet system where the user never touches the bindings directory, the
check may not run until the app has been open for some time, or until the next game launch
modifies the preset file.

`KeyBindCheck.check()` can be called externally to force a check, but it is not apparent from
the code that this is triggered automatically at startup.

**FN-4: `ToggleOn` semantics are ignored**

Some actions in `.binds` use `<ToggleOn Value="1"/>` — meaning the action fires on key-release,
not key-press. Two actions sharing a key where one is ToggleOn and one is normal cannot be
distinguished by the current system; both are treated as fire-on-press. A ToggleOn action on
`Key_G` alongside a normal action on `Key_G` may or may not be a real conflict depending on
game engine behavior, but the system cannot reason about this.

**FN-5: HOTAS/joystick conflicts are entirely invisible**

The conflict detection operates on `parseBindings()` output, which is keyboard-only. An action
bound to a HOTAS button that collides with a GameCommand keyboard binding is not detected, because
the HOTAS binding is filtered out before the conflict check runs. In practice this is unlikely to
matter (keyboard and HOTAS share no physical buttons), but it means the system cannot warn about
double-binding the same HOTAS button to two different actions.

**FN-6: Multi-modifier bindings with unsupported modifiers are excluded**

`parseBindingSlots()` filters to `keyboardUsable` slots. A slot is `keyboardUsable` only if all
modifier children also have `Device="Keyboard"`. A slot with a HOTAS button as a modifier (rare
but valid in Elite .binds files) is excluded from the conflict map entirely. If such a slot
shares a conceptual key with a GameCommand binding, it is not detected.

### 5c. Overall Verdict

The conflict detection system is **contextually smarter than a naive same-key check** but has
meaningful gaps that will cause both false positives and false negatives.

**What it does well:** It is genuinely modifier-aware — chords are treated as distinct from bare
keys. It correctly suppresses same-key use across vehicle states (ship/SRV/on-foot), which is
the most common source of false positives in players with split HOTAS+keyboard setups. It
maintains a live-diff model rather than accumulating stale data. The DESCRIPTIONS table provides
human-meaningful warnings for the most dangerous pairs. The algorithm correctly covers the entire
keyboard-usable binding map (not just GameCommand-to-GameCommand pairs) on the `other` side of
each anchor.

**Where it is unreliable:** The most significant structural limitation is that `parseBindings()`
discards Secondary slots before conflict detection — this produces systematic false negatives for
any real collision that involves one action's Secondary and another's Primary. The three-tier
vehicle-state suppression is sound in principle but the sub-state overlay logic over-suppresses
FSS/camera actions. Hold vs. tap is not factored into combo identity, producing false positives
when game semantics actually differentiate hold from tap. There is no UI surface for conflict
results — they reach the user only as a TTS count and a log entry, with no way to inspect the
specific key or take remedial action from within the app.

For BindForge extension purposes: the DB schema and diff logic are a good foundation. The
detection algorithm should be upgraded to operate over `parseBindingSlots()` (both Primary and
Secondary per action) rather than the flattened `parseBindings()` output, and the combo
normalization should include the `hold` flag. The sub-state overlay suppression list
(`isSubStateModeAction()`) should be narrowed to actions that are genuinely isolated from
main-flight context. A dedicated conflict UI surface is entirely absent and would need to be
built from scratch.
