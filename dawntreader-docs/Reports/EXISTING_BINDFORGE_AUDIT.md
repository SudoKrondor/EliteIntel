# Existing BindForge / Binding System Audit

**Date:** 2026-06-17
**Branch audited:** `V1.1-KAN-6-push-to-talk-dawntreader`
**Scope:** Read-only. No files were modified during this audit.

---

## 1. Database Schema

### 1a. `player.bindings_dir` column — migration `00004__schema.sql`

```sql
alter table player add bindings_dir text;
```

The `player` table gained a `bindings_dir` column that stores the path to the Elite Dangerous bindings directory the user has pointed the app at. This is runtime configuration, not binding content. No other binding-relevant column was added in migration 00004.

---

### 1b. `bindings` table — migration `00023__schema.sql`

```sql
CREATE TABLE IF NOT EXISTS bindings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    key_binding TEXT UNIQUE
);
```

**Column-by-column notes:**

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | Surrogate key, not used externally |
| `key_binding` | TEXT UNIQUE | Stores a *human-readable, humanized* binding name — e.g. `"Landing Gear Toggle"` — **not** the raw Elite action ID (`LandingGearToggle`). Written by `StringUtls.humanizeBindingName()` before insert. |

**Purpose:** This table records which game bindings are currently **missing** (i.e., have no keyboard assignment in the parsed `.binds` file). It is a diagnostic list, not a full binding registry. A binding is added here when the app discovers it is absent, and removed when the binding later appears in the file. The method that reads it is called `getMissingBindings()` — deliberately named that way.

---

### 1c. `binding_conflicts` table — migration `00038__schema.sql`

```sql
CREATE TABLE IF NOT EXISTS binding_conflicts
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    conflict_key TEXT NOT NULL UNIQUE,
    description  TEXT NOT NULL
);
```

**Column-by-column notes:**

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | Surrogate key |
| `conflict_key` | TEXT UNIQUE | Sorted, order-independent pair of action IDs joined by `\|` — e.g. `"QuitCamera|UI_Back"`. Format produced by `BindingConflictRules.makeKey()`. |
| `description` | TEXT | Human-readable description of why this conflict is dangerous, e.g. `"UI Back and Quit Camera share a key and may interfere"`. |

The migration also seeds twelve known-dangerous conflict definitions. More are added/removed at runtime by `BindingConflictManager` as the monitor re-checks the live bindings file.

---

**Summary of binding-related tables:**

| Migration file | Table/column | What it stores |
|---|---|---|
| `00004__schema.sql` | `player.bindings_dir` | Path to user's Elite bindings folder |
| `00023__schema.sql` | `bindings` | Human-readable names of currently-missing game bindings |
| `00038__schema.sql` | `binding_conflicts` | Action-pair conflict keys and their warning descriptions |

There is **no database table** that stores a parsed representation of the full `.binds` file (action names, device/key pairs, modifiers, axis parameters, etc.). The DB only stores negative diagnostic state (what is missing, what conflicts).

---

## 2. DAO and Manager Layer

### 2a. `KeyBindingDao` — `app/src/main/java/elite/intel/db/dao/KeyBindingDao.java`

JDBI3 SQL Object interface. Operates on the `bindings` table.

**Method signatures:**

```java
void save(@Bind String binding);
void removeBinding(@Bind String binding);
void clear();
List<KeyBinding> listAll();
```

**SQL issued:**

| Method | SQL |
|---|---|
| `save` | `INSERT OR REPLACE INTO bindings (key_binding) VALUES(:binding)` |
| `removeBinding` | `DELETE FROM bindings WHERE key_binding = :binding` |
| `clear` | `DELETE FROM bindings` |
| `listAll` | `SELECT * FROM bindings` |

**Returns:** `List<KeyBindingDao.KeyBinding>` — an inner class with `int id` and `String keyBinding`. The `keyBinding` field holds the humanized label, not the raw game action string.

---

### 2b. `BindingConflictDao` — `app/src/main/java/elite/intel/db/dao/BindingConflictDao.java`

JDBI3 SQL Object interface. Operates on the `binding_conflicts` table.

**Method signatures:**

```java
void save(@BindBean ConflictRecord record);
void remove(@Bind("conflictKey") String conflictKey);
List<ConflictRecord> listAll();
void clear();
```

**SQL issued:**

| Method | SQL |
|---|---|
| `save` | `INSERT OR IGNORE INTO binding_conflicts (conflict_key, description) VALUES (:conflictKey, :description)` |
| `remove` | `DELETE FROM binding_conflicts WHERE conflict_key = :conflictKey` |
| `listAll` | `SELECT * FROM binding_conflicts` |
| `clear` | `DELETE FROM binding_conflicts` |

**Returns:** `List<BindingConflictDao.ConflictRecord>` — inner class with `String conflictKey` and `String description`.

---

### 2c. `KeyBindingManager` — `app/src/main/java/elite/intel/db/managers/KeyBindingManager.java`

Singleton manager wrapping `KeyBindingDao`.

**Method signatures:**

```java
public static KeyBindingManager getInstance()
public void addBinding(String binding)
public void removeBinding(String binding)
public void clear()
public List<KeyBinding> getMissingBindings()
```

No logic beyond delegation to the DAO. `getMissingBindings()` returns `List<KeyBindingDao.KeyBinding>`. Note: the singleton uses a non-synchronized double-check pattern (field is `volatile` but the `if (instance == null)` check is not synchronized) — a minor concurrency smell but not the focus here.

---

### 2d. `BindingConflictManager` — `app/src/main/java/elite/intel/db/managers/BindingConflictManager.java`

Singleton manager wrapping `BindingConflictDao`. Uses proper double-checked locking.

**Method signatures:**

```java
public static BindingConflictManager getInstance()
public void save(String conflictKey, String description)
public void remove(String conflictKey)
public List<BindingConflictDao.ConflictRecord> getConflicts()
```

---

## 3. Existing Binding UI

### 3a. `BindingsTabPanel` — `app/src/main/java/elite/intel/ui/screen/BindingsTabPanel.java`

The primary bindings UI screen, rendered as a tab inside the main app window.

**What the UI does:**

- Shows a profile card at the top with three read-only fields: bindings directory path (with a folder-picker button), the active preset name, and the absolute path of the active `.binds` file.
- Displays a two-tab pane: **Used Bindings** (commands that have a keyboard assignment) and **Missing Bindings** (commands needed by EliteIntel that have no keyboard assignment).
- Each tab renders grouped tables (grouped by `BindingGroup` category) with columns: Action, Primary, Secondary. Primary and Secondary cells show the formatted slot content for all devices — not just keyboard.
- Clicking a Primary or Secondary cell opens `AssignKeyboardBindingDialog` if the slot is "basic-editable" (keyboard-only, zero or one supported modifier). Non-editable slots (HOTAS, joystick, multiple modifiers, etc.) show an informational message instead.
- Footer has a sync-status badge ("Synced" / "Draft"), a REVERT button, and an APPLY button. APPLY copies the working copy to the game directory after XML validation and backup. REVERT discards the working copy and reloads from the game file.
- Detects unapplied changes on app close and offers Apply / Keep Draft / Discard.

**Data flow — reads:**
1. `BindingsLoader.getLatestBindsFile()` — finds the active `.binds` file via `StartPreset.*.start` lookup.
2. `BindingsMonitor.getCurrentBindsFile()` — if the monitor has already loaded the file (preferred source).
3. `BindingsWorkingCopyRepository.loadOrImportFromGame()` — returns or creates the working copy path.
4. `KeyBindingsParser.parseReadOnlyBindingSlots(workingCopyFile)` — parses all Primary/Secondary slots from the working copy.
5. `BindingsMonitor.findFoundGameBindings()` / `findMissingGameBindings()` — computes which of the app's required GameCommands are present or absent.

**Data flow — writes:**
1. `BindingsWriter.assignKeyboardKey()` or `assignKeyboardKeyWithModifier()` — raw-text XML edit on the working copy.
2. `BindingsApplyService.apply()` — copies working copy to game directory after validation + backup.

**Events consumed:** `@Subscribe` on `BindingsUpdatedEvent` — triggers `initData()` to reload.
**Events published:** `BindingsSummaryChangedEvent` (missing/found counts), `KeymapSyncStateChangedEvent` (sync state).
**DAOs called:** None directly. All data flows through the `KeyBindingsParser`, `BindingsMonitor`, `BindingsWriter`, and `BindingsWorkingCopyRepository` layer.

---

### 3b. `AssignKeyboardBindingDialog` — `app/src/main/java/elite/intel/ui/dialog/AssignKeyboardBindingDialog.java`

Modal dialog for assigning or clearing one keyboard binding slot.

**What it does:**

- Shows the binding ID, slot type (Primary/Secondary), and current formatted value as read-only labels.
- Provides a key dropdown (`HudComboBox<KeyOption>`) populated by `KeyboardKeyAvailabilityService.availableKeys()` — only keys not already used elsewhere in the file. The current key is always shown even if occupied.
- Provides a modifier dropdown (`HudComboBox<ModifierOption>`) with "None" plus the six supported keyboard modifiers (Left/Right Ctrl, Shift, Alt). Selection in the modifier dropdown re-queries available keys for that chord.
- Shows an "already in use" warning label (red, bold) if the selected key+modifier combination is occupied by another slot.
- On Save: returns an `AssignKeyboardBindingSelection` record containing `(BindingSlotType slotType, String key, BindingModifier modifier)`.
- Refuses to open for slots with unsupported XML (multiple modifiers, non-keyboard main key on an occupied slot, etc.) — the tab panel shows an info dialog instead.

**Constraints:**
- Keyboard-only. Joystick, mouse, HOTAS bindings cannot be assigned through this dialog.
- At most one modifier per binding.
- Only keys in `EliteKeyboardKeys.ASSIGNABLE_KEYS` are offered (a curated static list of ~90 keys).

---

### 3c. `BindingsGroupTableFactory` — `app/src/main/java/elite/intel/ui/support/BindingsGroupTableFactory.java`

Factory that creates per-group `JScrollPane`-wrapped `JTable` instances for the bindings tab.

- Each table has three columns: action name (raw binding ID), primary slot display, secondary slot display.
- Column 1 (Primary) and column 2 (Secondary) are clickable and open the assign dialog. Column 0 (action name) is not clickable.
- Uses `BindingSlotCellRenderer` for the Primary/Secondary cells.
- Delegates selection management to `BindingsSelectionController` (cross-table single-selection).

---

### 3d. `BindingSlotDisplayFormatter` — `app/src/main/java/elite/intel/ui/support/BindingSlotDisplayFormatter.java`

Formats a `ReadOnlyBindingSlot` for display. Handles all device types (keyboard, joystick, raw hex device IDs). Does not restrict to keyboard-only — this is the display path, not the execution path.

Key method: `formatSlot(ReadOnlyBindingSlot slot)` returns strings like `"Keyboard | Left Ctrl + E"` or `"Device 044F0422 | Joystick 9"` or `"— | —"` for empty slots.

Also provides `formatRawKeyStep()` for custom-command step display and `toEliteKeyFormat()` for converting uppercase key names to Elite's `Key_XXX` format.

---

### 3e. Other relevant UI support classes

| Class | File | Role |
|---|---|---|
| `BindingsSelectionController` | `ui/support/` | Cross-table single-selection state for the bindings tab |
| `BindingSaveResultPresenter` | `ui/support/` | Maps `BindingSaveResult` enum values to user-visible messages |
| `AssignKeyboardBindingSelection` | `ui/support/` | Record: `(BindingSlotType, String key, BindingModifier modifier)` |
| `BindingSlotCellRenderer` | `ui/render/` | Custom cell renderer for Primary/Secondary table cells |

---

## 4. .binds XML Parser

**File:** `app/src/main/java/elite/intel/ai/hands/KeyBindingsParser.java`

This is a singleton class with three public parse methods, a set of internal helper types, and a thin filter layer that separates "diagnostic" (all devices) from "executable" (keyboard-only) representations.

### What it extracts

**DOM-level parsing in `parseReadOnlyBindingSlots(File file)`:**

1. Opens the file with `javax.xml.parsers.DocumentBuilderFactory` (no `setFeature` hardening — unlike `KeyboardKeyAvailabilityService` which does set `FEATURE_SECURE_PROCESSING`).
2. Iterates all direct children of the document root (`doc.getDocumentElement().getChildNodes()`).
3. For each child that is an `Element`, reads its tag name as the `actionName`.
4. Looks for `<Primary>` and `<Secondary>` child elements via `element.getElementsByTagName("Primary")` and `element.getElementsByTagName("Secondary")`.
5. For each slot element found, calls `readOnlySlot(Element, BindingSlotType)`:
   - Reads `Device` attribute.
   - Reads `Key` attribute.
   - Reads `Hold` attribute (mapped to `boolean hold` — `"1"` means hold).
   - Calls `getBindingModifiers(slot)`: iterates `slot.getChildNodes()` looking for children with tag name `"Modifier"`, reads their `Device` and `Key` attributes.
   - Computes `keyboardUsable`: `Device == "Keyboard"`, key non-blank and not `"Key_"`, **AND** all modifiers also have `Device == "Keyboard"`.
   - Computes `editable` (narrower): keyboard main key, AND (zero modifiers OR exactly one supported modifier from the allow-list of 6).
6. Returns a `Map<String, ReadOnlyBindingSlots>` keyed by action name.

**`parseBindingSlots(File file)`:** Filters the above to only `keyboardUsable` slots, returns `Map<String, BindingSlots>` with `KeyBinding` (executable) objects.

**`parseBindings(File file)`:** Further reduces to a flat `Map<String, KeyBinding>` by picking primary over secondary.

### What it extracts (summary)

| Element/attribute | Extracted? | Notes |
|---|---|---|
| Action tag name | YES | Used as map key |
| `<Primary>` Device | YES | Stored in `ReadOnlyBindingSlot.device()` |
| `<Primary>` Key | YES | Stored in `ReadOnlyBindingSlot.key()` |
| `<Primary>` Hold | YES | Mapped to `boolean hold` |
| `<Primary>` Modifier children (Device+Key) | YES | Stored as `List<BindingModifier>` |
| `<Secondary>` Device/Key/Hold/Modifiers | YES | Same as above |
| `<Binding>` element (AXIS) | NO | Silently ignored |
| `<Inverted>` element (AXIS) | NO | Ignored |
| `<Deadzone>` element (AXIS) | NO | Ignored |
| `<ToggleOn>` element | NO | Ignored |
| STANDALONE SETTING `Value=` attribute | NO | Ignored |
| `KeyboardLayout` text-content node | NO | Silently ignored (not an Element with Primary/Secondary) |
| Duplicate action tag names | PARTIAL | The last one wins silently; `Map.put()` overwrites |

### Where parsed data goes

- `parseReadOnlyBindingSlots()` result → held in `BindingsTabPanel.currentSlots` (in-memory only, not DB).
- `parseBindings()` result → held in `BindingsMonitor.bindings` (in-memory live map used by `KeyBindingExecutor` for command dispatch).
- `BindingsMonitor` also calls `checkForMissingBindingsAndPersist()` which writes humanized names to the `bindings` DB table, and `checkForConflictsAndPersist()` which writes to `binding_conflicts`.

---

## 5. Gap Analysis vs. Known .binds Schema

Reference: `AUDIT_v2_FULL.md` — analysis of `DualVirpilDawnTreader.4.2.binds`, 457 elements (72 AXIS, 268 BUTTON, 117 STANDALONE SETTING).

### 5a. AXIS elements (72 in the specimen file)

| Capability | Status | Detail |
|---|---|---|
| AXIS detection (`<Binding>` child vs `<Primary>`/`<Secondary>`) | NOT SUPPORTED | Parser only looks for `<Primary>` and `<Secondary>`. Any action whose binding is only on `<Binding>` produces no entry in the map. |
| `<Binding Device="">` attribute | NOT SUPPORTED | Never read |
| `<Binding Key="">` attribute | NOT SUPPORTED | Never read |
| `<Inverted Value="">` | NOT SUPPORTED | Never read |
| `<Deadzone Value="">` | NOT SUPPORTED | Never read |
| AXIS elements in the missing-binding check | PARTIALLY OK | They simply won't appear in the parsed map. If a GameCommand references an AXIS action name, it will always count as "missing". None of the current `Bindings.GameCommand` enum values map to known AXIS actions. |

### 5b. BUTTON elements (268 in the specimen file)

| Capability | Status | Detail |
|---|---|---|
| `<Primary>` Device + Key | SUPPORTED | Fully extracted |
| `<Secondary>` Device + Key | SUPPORTED | Fully extracted |
| `<Primary>` Modifier children (Device + Key) | SUPPORTED | `getBindingModifiers()` reads all `<Modifier>` children |
| `<Secondary>` Modifier children (Device + Key) | SUPPORTED | Same method covers both slots |
| Multiple simultaneous Modifiers on one slot (e.g. `FixCameraRelativeToggle`: Shift+Ctrl) | PARSED but marked non-editable | `ReadOnlyBindingSlot.editable = false` when `modifiers.size() > 1`. The dialog refuses to open. The slot IS displayed in the table as-is. |
| Non-keyboard Modifier on a joystick slot (e.g. `AutoBreakBuggyButton`: RVWAP/Joy_32 Modifier) | PARSED and correctly classified non-keyboard-usable | `isKeyboardUsable()` requires all modifiers to have `Device == "Keyboard"`. This slot appears in the diagnostic table but is not offered for editing. |
| `<ToggleOn Value="">` | NOT SUPPORTED | Never read |
| Half-axis key strings on BUTTON slots (e.g. `Neg_Joy_YAxis`, `Pos_Joy_UAxis`) | PARSED as raw strings, not interpreted | Device and Key are stored verbatim. Displayed by `BindingSlotDisplayFormatter.formatBindingToken()` — a `Joy_`-prefix handler exists but only strips "Joy_" (e.g. returns "Joystick Neg_YAxis"). |
| Mouse axis keys on BUTTON slots (e.g. `Pos_Mouse_ZAxis`) | PARSED as raw strings, not interpreted | No special handling; `formatBindingToken()` has no Mouse case |
| Mouse button keys (e.g. `Mouse_1`, `Mouse_2`) | PARSED as raw strings, not interpreted | Same — device shown as raw "Mouse" |
| `Hold` attribute | SUPPORTED | `"1"` → `boolean hold = true` |

### 5c. STANDALONE SETTING elements (117 in the specimen file)

| Capability | Status | Detail |
|---|---|---|
| STANDALONE SETTING detection (bare `Value=` attribute) | NOT SUPPORTED | Parser iterates all root child elements and calls `getElementsByTagName("Primary")` on each. STANDALONE SETTING elements like `<MouseGUI Value="1"/>` have no Primary/Secondary children, so they return `primaryList.getLength() == 0` and `secondaryList.getLength() == 0`, producing no map entry. They are silently ignored. |
| `Value=` attribute | NOT SUPPORTED | Never read |
| `EnableMenuGroups`, `MouseGUI`, and the other 115 settings | ALL IGNORED | |

### 5d. Special structural cases

| Capability | Status | Detail |
|---|---|---|
| Duplicate root-level element names (`MouseGUI` appears twice) | PROBLEMATIC | `parseReadOnlyBindingSlots()` uses `Map.put()` with the action name as key. The second `MouseGUI` silently overwrites the first. For write operations, `BindingsWriter.locateAction()` calls `openingTagStarts()` which returns a list of starts — and explicitly returns `UNSUPPORTED_XML` if it finds more than one occurrence of a tag. This means writing to any duplicated action is blocked, but reading silently takes the last. |
| `KeyboardLayout` text-content node | HARMLESS — ignored | Parser only processes `Element` nodes; text nodes are not `instanceof Element` and are skipped. No crash. |
| Action with only a Secondary keyboard binding, Primary unbound (`HeadLookToggle_Buggy`) | HANDLED CORRECTLY | Both slots are independently read. `parseBindings()` picks primary if non-null, else secondary. |
| `yawRotateHeadlook` (lowercase first character) | HANDLED CORRECTLY | Tag name is used as-is as the map key. No transform is applied. |

### 5e. Conflict with `getElementsByTagName` scope

There is a subtle issue in `parseReadOnlyBindingSlots`. It calls `element.getElementsByTagName("Primary")` which searches the **entire subtree** of each action element, not just direct children. For normal `.binds` files this is harmless because Primary/Secondary are never nested more than one level deep. However, if an action had unusual structure (e.g., a Primary inside a ToggleOn), it would incorrectly capture it. This has not been observed in practice.

### 5f. Parser hardening comparison

`KeyBindingsParser.parseReadOnlyBindingSlots()` creates its `DocumentBuilder` without `setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)` or the doctype-blocking feature. `KeyboardKeyAvailabilityService.parse()` does set both features. This is an inconsistency — the parser used for the UI reads is less hardened than the one used for availability checks. In practice it does not matter for local `.binds` files, but it is worth noting.

---

## 6. Assessment

### What works and is solid

The existing binding system is a well-structured V1 keyboard editor. The design is intentionally narrow and the code is clean within that scope:

- The read path (`KeyBindingsParser`) correctly extracts Primary, Secondary, device, key, Hold, and Modifier children from all BUTTON-type actions.
- The display path (`BindingSlotDisplayFormatter`, `BindingsTabPanel`) shows all device types — keyboard, HOTAS, joystick, mouse — in the diagnostic tables without trying to make them executable.
- The write path (`BindingsWriter`) is safe and conservative: it works on a working copy, uses text-range replacement to avoid reformatting the file, validates stale-file conditions before and after backup, and rejects unsupported XML explicitly.
- The working-copy + apply pattern (`BindingsWorkingCopyRepository`, `BindingsApplyService`) means the game directory is never touched until the user explicitly requests it, with a backup taken first.
- Conflict detection (`BindingsMonitor.checkForConflictsAndPersist()`, `BindingConflictRules`) is functional and keyed on actual parsed keyboard combos.
- The `BindingGroup` classifier and the Used/Missing two-tab layout are clean foundations for UI extension.

### What is missing and what it means for a BindForge extension

The system was explicitly designed as "keyboard-only MVP." Everything outside keyboard BUTTON slots is either ignored or marked non-editable. Concretely:

1. **AXIS elements are completely invisible.** 72 of the 457 elements in the specimen file are AXIS type. No data model for `<Binding>`, `<Inverted>`, or `<Deadzone>` exists anywhere in the codebase. An AXIS editor would need a new parse path, new model objects, and new write logic in `BindingsWriter`.

2. **STANDALONE SETTINGS are completely invisible.** 117 of 457 elements. No data model exists. These control things like `MouseGUI`, `EnableMenuGroups`, and similar options.

3. **`<ToggleOn>` is never read.** Present on some BUTTON actions but the parser has no code to read it.

4. **Modifier constraints are V1 keyboard-only.** The dialog enforces "at most one modifier, from the list of 6 keyboard modifiers." Multiple simultaneous modifiers, joystick modifiers, and non-keyboard main-key slots cannot be assigned. The parser does read them for display, but the writer and dialog both refuse them. Extending to handle multi-modifier or HOTAS-modifier combinations requires new dialog UI, a new `assignKeyboardKey`-equivalent for those cases, and a relaxed `isEditableKeyboardSlot()` check.

5. **Duplicate element names cause silent last-write-wins in reads and hard blocks in writes.** For `MouseGUI` this is irrelevant. If the user's file had duplicate BUTTON action names (not observed in practice but technically valid XML), the behavior would be unpredictable.

6. **No persistence of the full parsed binding map.** The DB stores only negative state (missing, conflicts). A BindForge feature that needs to query "what key is currently bound to action X across all 457 actions" must re-parse the file every time. This is fine for the current use case (command execution looks up the in-memory `BindingsMonitor.bindings` map) but would need an indexed store for a full editor.

### Verdict: Extend, not replace

The foundations are solid and deliberately layered. The parser, writer, working-copy system, and apply pipeline are all production-quality. None of them need to be replaced to extend BindForge.

The required additions to support a full BindForge (all three element types):

| Gap | Work required |
|---|---|
| AXIS read support | Add `<Binding>`, `<Inverted>`, `<Deadzone>` extraction to `KeyBindingsParser.parseReadOnlyBindingSlots()`. New model type alongside `ReadOnlyBindingSlots`. |
| AXIS write support | New method in `BindingsWriter` (or a parallel `AxisBindingsWriter`) to replace the `<Binding Device/Key>` attribute text on an AXIS element. |
| STANDALONE SETTING read support | Parse `Value=""` attribute on elements with no Primary/Secondary children. New model type. |
| STANDALONE SETTING write support | New method in `BindingsWriter` to replace the `Value=""` attribute. Simpler than BUTTON/AXIS since there are no child slots. |
| `ToggleOn` support | Read `<ToggleOn Value="">` in `readOnlySlot()`, add field to `ReadOnlyBindingSlot`, surface in UI and write path. |
| Multi-modifier BUTTON editing | Relax `isEditableKeyboardSlot()` and `isEditableMainSlot()` for the extended dialog. New `replacementSlot()` variant in `BindingsWriter` that emits multiple `<Modifier>` children. |
| Non-keyboard device BUTTON editing (HOTAS/joystick/mouse) | New write path entirely; `EliteKeyboardKeys` is keyboard-specific. Needs device enumeration, key enumeration per device (from the file itself), and an extended availability-check that covers non-keyboard slots. |
| `KeyboardLayout` metadata | Trivial: detect text-content child nodes and expose as a read-only metadata field. |
| Duplicate element name handling | `BindingsWriter.locateAction()` already blocks writes to duplicates. Read-side deduplication (e.g., collect as list rather than last-wins map) would be a clean-up. |
| Parser hardening consistency | Add `setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)` to the `DocumentBuilderFactory` in `KeyBindingsParser.parseReadOnlyBindingSlots()`, matching `KeyboardKeyAvailabilityService`. |

None of these gaps require touching the EventBus, the DB schema for other tables, the command-execution pipeline, or the `BindingsMonitor` monitoring loop. The extension surface is bounded to `KeyBindingsParser`, `BindingsWriter`, new model types, and new/extended dialog UI.
