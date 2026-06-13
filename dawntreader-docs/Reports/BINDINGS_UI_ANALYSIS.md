# Bindings UI Analysis

Analysis of the current state of the key-bindings viewer/editor in EliteIntel, post
upstream-merge (`V1.1` @ `a7194e6c`). Covers `BindingsTabPanel` and its supporting classes.

Primary class: `app/src/main/java/elite/intel/ui/view/BindingsTabPanel.java`

Supporting UI classes (all in `elite.intel.ui.view`, package-private unless noted):
- `BindingsGroupTableFactory` — builds the per-category `JTable`s
- `BindingSlotCellRenderer` — cell coloring/alignment for Primary/Secondary columns
- `BindingSlotDisplayFormatter` — turns raw slot data into human-readable strings
- `BindingsSelectionController` — keeps a single selection across many tables
- `AssignKeyboardBindingDialog` (public) — modal "assign a key" editor
- `AssignKeyboardBindingSelection` (public record) — dialog result DTO
- `BindingSaveResultPresenter` — shows save-result message dialogs

Supporting domain/service classes (all in `elite.intel.ai.hands`):
- `KeyBindingsParser` — parses `.binds` XML into `ReadOnlyBindingSlots`/`KeyBinding`
- `BindingModifier`, `BindingSlotType` — small value types for slots/modifiers
- `BindingGroup`, `BindingGroupClassifier` — the 9-category grouping taxonomy
- `BindingsMonitor` — watches the bindings directory, computes used/missing sets
- `BindingsLoader` — finds the active `.binds` file for the active preset
- `BindingsWorkingCopyRepository` — per-preset draft copy on disk
- `BindingsApplyService` / `BindingsBackupService` — writes draft back to the game dir
- `KeyboardKeyAvailabilityService` — figures out which keyboard keys are free
- `BindingsWriter` / `KeyboardBindingEdit` / `BindingSaveResult` — surgical XML edit of one slot

---

## 1. What the UI currently shows

The Bindings tab is split into three vertical regions: a **profile card** at the top, a
**tabbed table area** in the middle, and a **status/action footer** at the bottom.

### Profile card (top)

A framed "Binding Profile" section (orange-accented `HudSection`) containing:

- **Bindings Directory** — a read-only text field showing the folder EliteIntel is reading
  `.binds` files from, plus a small "⋮" button that opens a directory chooser
  (`JFileChooser`, directories only) to point at a non-default location.
- **Profile** — a read-only metadata field showing the *name* of the active Elite Dangerous
  control preset (e.g. `Custom`), auto-detected from `StartPreset.*.start`, with an "ⓘ" info
  button explaining how that detection works.
- **File** — a read-only metadata field showing the full path of the `.binds` file currently
  being read/edited, with its own "ⓘ" info button.
- A full-width **warning strip** along the bottom of the card (yellow/orange background):
  *"⚠ Elite Intel uses keyboard bindings for execution. Other devices are shown for
  diagnostics only."*

If no bindings file can be resolved (e.g. bad directory), Profile/File show "Not available"
and both tables render empty.

### Tabbed table area (center)

A compact `JTabbedPane` with two tabs:

- **"Used bindings (N)"** — every EliteIntel command (`Bindings.GameCommand`) that currently
  has a usable keyboard binding (Primary or Secondary).
- **"Missing bindings (N)"** — every EliteIntel command that does **not** currently have a
  usable keyboard binding. `N` is the live count, shown in the tab title.

Within each tab, rows are split into up to nine collapsible-looking section blocks (groups
with zero rows are simply omitted), each with an uppercase header label:

`SHIP / FLIGHT`, `COMBAT`, `UI PANELS`, `MAPS`, `EXPLORATION`, `CAMERA`, `SRV`, `ON-FOOT`,
`MISCELLANEOUS`

Each group is its own small `JTable` with three columns — **Binding**, **Primary**,
**Secondary** — sorted alphabetically (case-insensitive) by binding name. The whole thing
scrolls as one long page (mouse wheel on any inner table is forwarded to the single outer
scrollbar).

Cell values for Primary/Secondary are formatted as `"<Device> | <Modifier> + <Key>"`, e.g.
`Keyboard | Left Ctrl + G`, `Joystick 12`, or `Device 044F0422 | Left Alt + Joystick 4`. A
completely empty slot (`{NoDevice}`, no key) renders as `—`; a device that's set but with no
usable key renders as `Not defined`. Critically, **the "Missing bindings" tab still shows
whatever is actually assigned** (including HOTAS/joystick/gamepad assignments) — it's
"missing" only in the sense that EliteIntel can't *press* it, not that the slot is empty.

Switching between the two tabs clears any selected row.

### Footer (bottom)

- A **status badge** reading either **"IN SYNC"** (green) or **"DRAFT"** (yellow), with a
  tooltip explaining "In sync with game" / "Draft — not applied to game".
- **Revert** button (subtle styling) — discards the in-progress draft.
- **Apply** button (primary styling) — writes the draft to the game's `.binds` file; disabled
  whenever the badge already says "IN SYNC".

---

## 2. What it can do

### Viewing
- Shows **both** Primary and Secondary slots for every binding the game file defines,
  including non-keyboard devices (HOTAS, joystick, gamepad, mouse) — for diagnostics, even
  though EliteIntel can't execute those.
- Groups ~all Elite Dangerous binding IDs into 9 human-friendly categories via a
  keyword-matching classifier (`BindingGroupClassifier`) — e.g. anything containing `Fire`,
  `Hardpoint`, `Target`, `Shield`, `Chaff` → **Combat**; anything containing `Humanoid` →
  **On-foot**; anything containing `GalaxyMap`/`SystemMap` → **Maps**, etc., with a
  `MISCELLANEOUS` catch-all.
- Automatically refreshes the whole view when `BindingsMonitor` detects the game has
  rewritten the active `.binds` file on disk (`BindingsUpdatedEvent`).
- Hover-highlights table rows and shows a hand cursor over clickable (editable) cells.
- Lets the user pick a non-default bindings directory if their `.binds` files live somewhere
  other than the standard Elite Dangerous location.

### Editing — "basic editable slots" only
A Primary/Secondary cell is editable only if it is **already** a plain keyboard assignment
with at most one supported modifier (Left/Right Ctrl, Shift, or Alt), **or** it is completely
empty (`{NoDevice}`, no key — i.e. cleared). Clicking such a cell opens a modal
**"Assign keyboard key"** dialog (`AssignKeyboardBindingDialog`) showing:

- The binding ID, which slot (Primary/Secondary), and the current value (read-only).
- A **New key** dropdown — every assignable keyboard key not currently occupied elsewhere
  for the selected modifier (sorted alphabetically), plus an explicit **"Not defined"**
  option to clear the slot. If the dialog is opened on an already-occupied key, that key
  remains visible/selected even if "occupied" so the user can see what they're changing from.
- A **Modifier** dropdown — None, or one of Left/Right Ctrl/Shift/Alt.
- A live **"Already in use"** warning if the chosen key+modifier combination collides with
  another binding's slot.
- A **"No free keyboard keys are available"** message if every assignable key is taken for
  the selected modifier.
- **Save** is enabled only when the selection actually differs from the original and isn't a
  conflicting combination; **Cancel** closes without changes.

On save, the result is shown via a small `JOptionPane` (`BindingSaveResultPresenter`) with
one of these outcomes: Saved / No change / Stale file (reload needed) / Key occupied /
Unsupported key / Binding not found / Unsupported XML / Backup failed / Write failed. A
successful (or no-op/stale) save triggers a full table refresh.

### Draft / working-copy workflow
- Edits never touch the live game `.binds` file directly. The first time a profile is opened,
  EliteIntel makes a byte-for-byte working copy under `elite-intel/bindings/` (preserving any
  UTF-8 BOM). All edits go to that **draft** copy via targeted text-range XML rewrites (not a
  full DOM re-serialization), so unrelated formatting in the file is untouched.
- **Apply** validates the draft as XML, backs up the current game `.binds` file to
  `elite-intel/bindings/backups/`, then atomically replaces the game file with the draft.
  Success message names the backup file (or notes there was nothing to back up).
- **Revert** asks for confirmation, then deletes the draft and re-imports fresh from the game
  file.
- The sync badge is a literal byte-for-byte file comparison between draft and game file.
- If the app is closed with an unapplied draft, a modal dialog offers **Apply to Game / Keep
  Draft / Discard**.

### Selection
- Only one row, across *all* of the many per-group tables, can be selected at a time
  (`BindingsSelectionController`); selecting a row in one table clears selection everywhere
  else.

---

## 3. What it cannot do

- **Cannot edit non-keyboard slots at all.** Clicking a HOTAS/joystick/gamepad/mouse cell
  shows a read-only info dialog: *"This slot uses an advanced or unsupported binding. The
  basic editor will not modify it."*
- **Cannot edit slots with more than one modifier**, or with a modifier that isn't one of the
  6 supported keyboard modifiers (Left/Right Ctrl/Shift/Alt) — these are treated as
  non-editable/read-only even if the main key is a keyboard key.
- **Cannot edit "structurally unusual" XML** — if a slot/action element has unexpected
  attributes, multiple `<Modifier>` children, non-`Modifier` child elements, duplicate slot
  elements, or the action tag appears more than once, the writer refuses with
  `UNSUPPORTED_XML` rather than risk corrupting the file.
- **Cannot create new binding entries** — only slots that already exist as `<Primary>`/
  `<Secondary>` elements under a known action tag can be assigned; a binding entirely absent
  from the XML reports `BINDING_NOT_FOUND`.
- **Cannot assign analog/axis controls** — the editor only ever writes a discrete
  `Device="Keyboard" Key="..."` (optionally with one `<Modifier>`).
- **Cannot change the `Hold` flag** or any attribute other than `Device`/`Key`/`Modifier`.
- **Cannot switch between multiple presets/profiles** — only the single currently-active
  preset (as resolved by `BindingsLoader`/`BindingsMonitor`) is shown/edited; there's no UI to
  browse other `.binds` files in the directory.
- **No search/filter** — finding a specific binding relies entirely on category grouping +
  alphabetical order; there is no text search box.
- **No batch/multi-edit** — one slot, one dialog, one save at a time.
- **No distinction in the "Missing" tab** between "completely unbound" and "bound to a
  non-keyboard device" beyond what's visible in the Primary/Secondary columns themselves —
  both count identically toward the "missing" total used elsewhere in the app (e.g. for
  conflict/missing-binding tracking).
- **No drag-and-drop, no keyboard-only navigation** for reassigning bindings — editing is
  entirely mouse-driven via the modal dialog.

---

## 4. Swing component structure

```
BindingsTabPanel  (JPanel, BorderLayout)
│
├── NORTH: bindingProfileCard()
│     └── JPanel (transparent, BorderLayout)
│           └── HudSection "Binding Profile"  (FRAMED, orange border)
│                 ├── body(): JPanel (transparent, BorderLayout)
│                 │     └── details: JPanel (transparent, GridBagLayout)
│                 │           Row 0: JLabel "Bindings Directory:"
│                 │                  | bindingsDirField (read-only JTextField, fills width)
│                 │                  | "⋮" JButton  → JFileChooser (directories only)
│                 │           Row 1: JLabel "Profile:"
│                 │                  | profileField (HudMetadataField, read-only)
│                 │                  | "ⓘ" JButton  → info JOptionPane
│                 │                  | spacer (24px)
│                 │                  | JLabel "File:"
│                 │                  | filePathField (HudMetadataField, read-only, spans 2 cols)
│                 │                  | "ⓘ" JButton  → info JOptionPane
│                 └── footer (setFooter, warning background):
│                       keyboardOnlyWarningStrip(): JPanel (GridBagLayout)
│                             └── JLabel "⚠ Elite Intel uses keyboard bindings..."
│
├── CENTER: tabs  (AppTheme.makeCompactTabs() → JTabbedPane, compact style)
│     ├── Tab 0 "Used bindings (N)"
│     │     └── nestedTabContent: JPanel (transparent, BorderLayout)
│     │           └── usedBindingsScrollPane (hudScrollPane, data-plane border)
│     │                 └── usedBindingsPanel (transparent JPanel, BoxLayout.Y_AXIS)
│     │                       for each non-empty BindingGroup, in enum order:
│     │                         ├── sectionHeader: JLabel (hudGroupLabel, uppercase)
│     │                         ├── groupTable: JScrollPane (scrollbars hidden)
│     │                         │     └── JTable (DefaultTableModel, 3 cols, non-editable)
│     │                         │           - columns: Binding(320px) | Primary(270px) | Secondary(270px)
│     │                         │           - HudTable.styleCompact + BindingSlotCellRenderer
│     │                         │           - header: GroupTableHeaderRenderer
│     │                         │           - mouse listeners:
│     │                         │               click Primary/Secondary cell → openAssignKeyboardBindingDialog
│     │                         │               hover → row highlight (HUD_TABLE_ROW_HOVER)
│     │                         │               wheel  → forwarded to outer scroll pane
│     │                         └── Box.createVerticalStrut(6)
│     │                       + Box.createVerticalGlue()
│     │
│     └── Tab 1 "Missing bindings (N)"
│           └── (identical structure, backed by missingBindingsPanel/missingBindingsScrollPane)
│
│     tabs.addChangeListener → selectionController.clearSelection()
│
└── SOUTH: buildFooter()
      └── JPanel (BorderLayout, hudFooterSeparatorBorder, HUD_BG background)
            ├── CENTER: statusArea: JPanel (transparent, GridBagLayout)
            │     └── syncStatusBadge: StatusBadge ("IN SYNC" / "DRAFT")
            └── EAST: buttonBar: JPanel (transparent, GridBagLayout)
                  ├── revertButton (JButton, subtle style) → revertFromGame()
                  └── applyButton  (JButton, primary style) → performApply()
```

### Modal edit dialog (separate window)

```
AssignKeyboardBindingDialog  (JDialog, APPLICATION_MODAL)
└── contentPane: JPanel (transparent, BorderLayout, HUD_BG bg, HUD_PADDING border)
      ├── CENTER: HudSection "Keyboard Assignment"
      │     └── body: JPanel (transparent, GridBagLayout)
      │           Row: "Selected binding:"  | read-only JTextField (bindingId)
      │           Row: "Slot:"              | read-only JTextField ("Primary"/"Secondary")
      │           Row: "Current value:"     | read-only JTextField (formatted current slot)
      │           Row: "New key:"           | keyCombo: JComboBox<KeyOption>  (custom renderer)
      │           Row: "Modifier:"          | modifierCombo: JComboBox<ModifierOption> (custom renderer)
      │           Row:                      | noFreeKeysLabel: JLabel (hidden unless no keys free)
      └── SOUTH: buttons: JPanel (FlowLayout RIGHT)
            ├── alreadyInUseLabel: JLabel (red/bold, hidden unless conflicting)
            ├── Cancel: JButton (subtle) → dispose()
            └── Save:   JButton (primary, default button) → saveSelection() → dispose()
```

### Helper classes and their roles

| Class | Role |
|---|---|
| `BindingsGroupTableFactory` | Builds each group's `JTable` + wrapping `JScrollPane`: column widths, compact HUD styling, click/hover/wheel-forward listeners. |
| `BindingSlotCellRenderer` | Extends `HudTable.CellRenderer`; right-aligns Primary/Secondary columns, colors "Not defined" cells `HUD_DISABLED` and real values `ACCENT`, applies hover background. |
| `BindingSlotDisplayFormatter` | Pure formatting: `ReadOnlyBindingSlot` → `"Device \| Modifier + Key"` strings, handles raw device IDs, `{NoDevice}`, joystick/keyboard token naming. |
| `BindingsSelectionController` | Registers every group `JTable`, enforces a single selected row across all of them, exposes `selectRow`/`clearSelection`/`resetTables`. |
| `AssignKeyboardBindingDialog` | The modal editor described above; queries `KeyboardKeyAvailabilityService` for free keys and conflict checks. |
| `AssignKeyboardBindingSelection` | Record `(BindingSlotType, key, modifier)` returned from the dialog; `key == null` means "clear slot". |
| `BindingSaveResultPresenter` | Maps `BindingSaveResult` to a localized message and shows it via `JOptionPane`. |

### Backing data flow (for context)

`BindingsTabPanel.initData()`:
1. Resolves the active game `.binds` file (`BindingsMonitor.getCurrentBindsFile()` or
   `BindingsLoader.getLatestBindsFile()`).
2. Ensures/loads the per-preset working copy (`BindingsWorkingCopyRepository`).
3. Parses it via `KeyBindingsParser.parseReadOnlyBindingSlots(...)` → `Map<String,
   ReadOnlyBindingSlots>` (raw, diagnostic, includes non-keyboard devices).
4. Derives `effectiveBindings(...)` — keyboard-usable-only `KeyBinding`s (mirrors the
   executable model used by command dispatch).
5. Splits `Bindings.GameCommand` IDs into found/missing via `BindingsMonitor
   .findFoundGameBindings(...)` / `findMissingGameBindings(...)`.
6. Groups both lists via `BindingGroupClassifier.classify(...)` into the 9 `BindingGroup`
   buckets and renders one table per non-empty bucket.
7. Publishes `BindingsSummaryChangedEvent(missingCount, usedCount)` for other UI (e.g. a
   summary/status widget elsewhere in the app).

Editing a slot (`saveKeyboardBinding`):
1. Builds a `KeyboardBindingEdit` (file, binding ID, slot type, new key, expected
   last-modified time + size for staleness checking).
2. Calls `BindingsWriter.assignKeyboardKey(...)` or `assignKeyboardKeyWithModifier(...)`,
   which does a targeted text-range replacement of just that `<Primary>`/`<Secondary>`
   element in the working-copy XML, after re-validating staleness and key availability.
3. Shows the `BindingSaveResult` via `BindingSaveResultPresenter`, then calls `initData()`
   again on success/no-change/stale to refresh the tables and sync badge.
