# BindForge — UI Component Map

**Written 2026-09-20**, answering the check [v1.2-scope.md](../../00-overview/v1.2-scope.md#host-prerequisites-elite-intel)
sets before any panel is built: *confirm the UI component set available from Gnevko's V1.1 interface redesign*,
so BindForge's screens are assembled from what the application already has rather than introducing a
parallel set.

**This is not a second design canon.** Elite-Intel has one:
[`ED_HUD_REFERENCE.md`](../../ED_HUD_REFERENCE.md), *"the single source of truth for HUD component design"*,
which every UI change is checked against. This document does one thing the canon does not: it walks each
BindForge screen, names the HUD component each element is built from, and lists what is missing. Where the two
disagree, the canon wins and this document is wrong.

Read from the tree at `V1.2-BindForge` `ce7aed408`: `ui.widget` (49 classes), `ui.theme`, `ui.render`,
`ui.dialog`, and the two existing binding screens.

---

## The verdict

**Almost all of BindForge assembles from components that already exist.** Tabs at three levels, the
segmented selector, combos, checkboxes, tables with boolean and combo cell editors, banners, the modal
scaffold, `HudConfirmDialog`, the unsaved-changes footer, the split pane — and, already built for the Bindings
section, the chord capture field, the live keyboard map, the conflict callout and the slot cell renderer.

**Six gaps, two of them load-bearing:**

| # | Missing | Needed by | Blocks, or degrades? |
|---|---|---|---|
| 1 | **A collapsible group** — a header row that shows and hides the rows beneath it | Game Mode grid, Settings mode, Alias Designer's inline editor | **Blocks** the grid as specified; static group headers (today's grid) are the fallback |
| 2 | **Editable table cells beyond boolean and combo** — free text, and numbers | Alias Designer's button and axis labels; the 21 free-number settings | **Blocks** in-cell editing; editing in a form beside the table is the fallback |
| 3 | **A HUD context menu** | Game Mode's right-click Expand All / Collapse All | Degrades — two buttons do the same job |
| 4 | **A badge on a tab** | the Anomalies count, the destructive-change Error signal | Degrades — the count goes in the tab title, the app's existing pattern |
| 5 | **Tabs inside a table row** | Alias Designer — one tab per detected installation | Blocks that one layout only |
| 6 | **Esc clears a search field** | the Bind Editor shell | Degrades — the × already clears |

**Two tensions with the canon**, which are design questions rather than missing code — see
[below](#tensions-with-the-canon).

**The rule that decides where each gap is built.** The canon: *"A pattern needed on more than one screen
belongs in the HUD layer, not copied in place"* — and *"if you change the palette or the components, update the
matching section of this file in the same commit."* So gaps 1, 2 and 3 are new HUD components **and** new canon
sections. Gap 5 has one user and may stay local to Alias Designer. Gaps 4 and 6 extend existing widgets.

---

## The rules from the canon that bite BindForge hardest

Not a summary of the canon — the handful a bind editor is most likely to break:

- **HUD components, never raw Swing** — `HudComboBox` not `JComboBox`, `HudScrollPane` not `JScrollPane`,
  `HudCheckBox` not a LAF tick, `HudSegmentedControl` not `JRadioButton`.
- **Every value from `HudPalette` by name** — colours, fonts (`HUD_FONT_*` roles), heights, icons, border
  thickness. No hardcoding, no `deriveFont(size)`.
- **Every modal through `AppTheme.hudModalScaffold(HudModalSpec)`**, undecorated, with `BACK` on the left and
  the primary action on the right. **Confirm is `HudConfirmDialog`, never `JOptionPane`.**
- **Status is text colour, not a fill.** A fill means selection or activity only (§1).
- **A working area is FLAT; a separated accent is FRAMED** — and inside a modal, always FLAT (§9).
- **UI text only through `MultiLingualTextProvider.getText`**, across all nine language bundles.

---

## The kit, by job

The components BindForge will actually reach for, grouped by what they are for. Section numbers are the
canon's.

| Job | Component | Canon |
|---|---|---|
| **Navigation** | `HudTabbedPane` at `MAIN_NAV`, `SECTION` and `COMPACT` levels — disabled tabs dim at every level | §11 |
| | `HudSegmentedControl` — exactly one of several, as a bar | §5.4 |
| **Structure** | `HudSection` (`flat` for working areas, framed for accents), `HudTwoColumns`, `HudSplitPane`, `HudScrollingPage` | §9 |
| **Inputs** | `HudTextField` (optional in-field info "i"), `HudComboBox` and its searchable `picker`, `HudCheckBox`, `HudSlider`, `HudStepper`, `HudSearchField` / `HudSearchToolbar` | §4, §5 |
| | `makeFieldButton` — the square picker beside a field (directory, file) | §4 |
| **Tables** | `HudTable.style()` / `styleCompact()`, `dataPlaneScrollPane()`; group separator rows | §6 |
| | Cell editors: `HudBooleanCellEditor`, `HudComboCellEditor`. Header: `HudCheckBoxHeaderRenderer` | §6 |
| **Readouts and feedback** | `HudStatusReadout`, `hudReadoutLabel` / `hudReadoutValue`, `HudBanner` (and `multiline`), `HudMetadataField` | §7 |
| **Dialogs** | `hudModalScaffold` + `HudModalSpec`, `commandTitleBlock`, `HudConfirmDialog`, `runWithModalScrim` | §10 |
| **Footers** | `HudFooter.build(modal, …)`, `HudUnsavedHint` | §10 |
| **Glyphs** | `HudGlyphButton`, `HudSection.setHeaderActions`, and the `paintHud*` primitives | §4, §13 |
| **Already built for binding** | `KeyChordCaptureField`, `KeyboardAvailabilityView`, `BindingConflictPopup`, `BindingSlotCellRenderer`, `BindingsGroupTableFactory` | — |

---

## Screen by screen

**✓** exists · **GAP** missing, numbered as in [the verdict](#the-verdict) · **TENSION** conflicts with the canon

### Bind Editor — shell

| Element | Built from |
|---|---|
| Bindings File dropdown and Load | `HudComboBox`, `HudButton` ✓ |
| Search, clearing with × | `HudSearchField` ✓ |
| Search, clearing with Esc | **GAP 6** — the widget has no Esc handling |
| Show Anomalies Only | `HudCheckBox` ✓ — exists today as SHOW CONFLICTS ONLY |
| Mode tabs — Game Mode, Anomalies, Settings | `HudTabbedPane` `SECTION` ✓ |
| Context Bar — All / General / Ship / SRV / On Foot | `HudSegmentedControl` ✓ — one of five, so a segmented bar and not five buttons |
| The roughly 75/25 split, grid against Binding Editor panel | `HudSplitPane` ✓ |

### Game Mode

| Element | Built from |
|---|---|
| The binding grid | `BindingsGroupTableFactory` tables on `HudTable` ✓ — grown, not rebuilt |
| Groups that collapse on a left-click | **GAP 1** — today's groups are static headers |
| Right-click Expand All / Collapse All | **GAP 3** |
| The ASSISTANT column | a column and a renderer ✓ |
| Conflict colour on the **slot cell** | a `BindingSlotCellRenderer` change ✓ — the scan's `Conflict` already carries the chord the cell matches on |
| Settings rows inline — toggles, choices | `HudBooleanCellEditor`, `HudComboCellEditor` ✓ |
| Settings rows inline — free numbers | **GAP 2** |
| Conflict hover callout | `BindingConflictPopup` ✓ |

### Anomalies

| Element | Built from |
|---|---|
| All / Reserved / Missing / Conflicts / Invalid, each with a count | `HudTabbedPane` `COMPACT` ✓, the count in the title |
| The mode tab's badge counting every anomaly | **GAP 4** |
| Severity on each group header | **TENSION 1** |
| Rows ranked and banded by certainty of failure | table order plus a renderer ✓ |
| Per-row remedy text | `HudBanner` or a readout column ✓ |

### Settings mode

| Element | Built from |
|---|---|
| All ~93 entries, grouped as the game groups them | the Game Mode grid's rows ✓ — and **GAP 1** if its groups collapse too |
| Toggle, choice, numeric choice | `HudBooleanCellEditor`, `HudComboCellEditor` ✓ — the three *numeric choices* are a dropdown wearing numbers |
| Free numbers | **GAP 2** |

**Free numbers are floats**, and both range controls in the kit are integer-only: `HudSlider(min, max, step,
value)` and `HudStepper` take and return `int`. A settings value such as a deadzone can be carried on an
integer scale (0–100 standing for 0.00–1.00), so the controls can serve — but neither is a cell editor, and
that part is missing.

### Capture Dialog

Grown from `AssignKeyboardBindingDialog`, which is already on the modal scaffold and already dims the window
behind it.

| Element | Built from |
|---|---|
| Tabs by input source — KEYBOARD, MOUSE, one per controller, with counts | `HudTabbedPane` ✓, added at runtime |
| Sub-tabs by input kind — BUTTONS, HATS, AXIS DIRECTIONS, AXES | a nested `HudTabbedPane` `COMPACT` ✓ |
| Grey out what cannot fill the slot | `setEnabledAt(i, false)` ✓ — `HudTabbedPane` dims a disabled tab at every level |
| Keyboard capture and the live keyboard map | `KeyChordCaptureField`, `KeyboardAvailabilityView` ✓ |
| A controller's inputs, grouped under `.buttonMap` headings | `HudTable` with group separator rows ✓ (§6) |
| Mouse — chosen, not captured | a list or `HudComboBox` ✓ |
| Title as the in-game name, then where it lives | `commandTitleBlock`, `hudReadoutLabel` / `hudReadoutValue` ✓ |

**Controller capture is new wiring, not a new component.** *"The input pressed decides the tab"* — pressing a
controller input moves to that device's tab and kind. That is `DeviceService` feeding the dialog while it is
open; what the commander sees is the tabs and the list above, both of which exist.

### Preset Editor

| Element | Built from |
|---|---|
| Four section dropdowns | `HudComboBox` ✓ |
| Set All to Same — a dropdown plus a button | `HudComboBox`, `HudButton` ✓ |
| The live banner when the four do not match | `HudBanner` in the `INFO` state ✓ |
| The sync state — IN SYNC / DRAFT | a readout value in its state colour, as `HudStatusReadout` does ✓ — **no pill**, see [TENSION 2](#2-statusbadge-draws-a-pill) |
| Revert and Save | `HudFooter.build(false, …)` with `HudUnsavedHint` ✓ |

**Preset Editor needs nothing new** — which is another reason, beside needing no install discovery, to build it
first.

### Alias Designer

| Element | Built from |
|---|---|
| The two views, toggled | `HudTabbedPane` `COMPACT` ✓ |
| The device list | `HudTable` ✓ |
| One tab per detected installation, **inside the row**, marked **M** or *not added* | **GAP 5** |
| The editor, **expanding inline beneath the open device** | **GAP 1** — the same disclosure family as a collapsible group |
| Alias, VID and PID | `HudTextField` ✓ |
| Button and axis labels | **GAP 2** if edited in cells; `HudTextField` rows in the editor if not |
| MIRROR, CLEAR, RESET LABELS, APPLY | `HudButton` ✓ |
| Rename and onboarding dialogs | `hudModalScaffold` ✓ |

**GAP 5 has one user**, so the canon lets it stay local to Alias Designer. It is also the most unusual thing
BindForge asks of the kit — a JTable row does not host components, so tabs inside one means painting them in a
renderer and hit-testing clicks. **Worth weighing against a plainer layout** before building it: the
installations could be a `COMPACT` tab row inside the inline editor, which the kit already does.

### File Manager

| Element | Built from |
|---|---|
| Three sub-tabs | `HudTabbedPane` `SECTION` ✓ |
| Game Install Locations — two sections, read-only paths | `HudSection.flat`, `HudMetadataField` ✓ |
| Player Backups | exists today as Binding Management ✓ |
| The restore checklist — *Everything* forcing the four domains on and disabled | `HudCheckBox` ✓ — the dependent-disable shape `SettingToggle` / `ToggleTreePanel` already express |
| Ticking device entries in a foreign archive | `HudBooleanCellEditor` with `HudCheckBoxHeaderRenderer` ✓ — the selector-column pattern the Import dialog already uses |
| Edit History — entries per domain, a preview, Restore | `HudTable` beside a detail pane in `HudSplitPane` ✓ |
| Edit History retention, 1–30 | `HudStepper` ✓ — the canon's *"a few discrete values compactly"* |
| Every restore confirmation | `HudConfirmDialog` ✓ |

---

## The gaps in detail

### 1. A collapsible group — the load-bearing one

**Three screens need it**: the Game Mode grid (*"left-click toggles one group"*), the Settings mode if its
groups behave the same way, and Alias Designer's editor opening beneath a device row. **Nothing in the kit
collapses** — searched for, not assumed.

The two shapes are related rather than identical. A group header hides and shows the rows beneath it; an
inline editor opens a panel beneath one row. They may or may not share an implementation, but both are a
**disclosure** — a row that reveals what sits under it — and both need a canon section saying how it looks:
the open and closed marker, whether the marker is a `paintHud*` primitive (the canon already has
`paintHudArrowDown` and `paintHudArrowRight`), and what the header row looks like in each state.

**The fallback is what ships today:** static group headers, every group open. The grid works; only the
collapsing is lost.

### 2. Editable cells beyond boolean and combo

**No table in the app edits free text or numbers inside a cell.** The only cell editors in the tree are
`HudBooleanCellEditor` and `HudComboCellEditor` — searched for across `ui`. Two BindForge features would be the
first: Alias Designer's button and axis labels, and the Settings mode's 21 free numbers.

**The fallback is to edit beside the table, not in it** — select a row, edit in a form. That costs a click and
keeps the grid read-only, which is arguably safer in a grid where a stray click already must never start a
rebind.

### 3. A HUD context menu

The canon has **no section on menus**, and the app already has two right-click menus built as raw `JPopupMenu`
— the Jukebox and `ReadoutWindow`. Game Mode would be the third. So this is less a BindForge gap than an
existing one BindForge would make impossible to ignore: three uses is past the canon's *more than one
screen* line.

**The fallback removes the need:** Expand All and Collapse All as two `HudGlyphButton`s in the grid's header
through `HudSection.setHeaderActions` — which the canon already specifies for exactly this, *"actions over the
section's content."*

### 4. A badge on a tab

Already a known host gap — [Status Badges](../../01-host-integration/elite-intel-platform-map.md#status-badges)
in the platform map, and *degradable* in the scope. **The degradation is already the app's pattern:** the count
in the title, as the Bindings tab does today with *USED BINDINGS (252)*. That serves the Anomalies count
outright. The destructive-change Error signal is the harder case, since a count cannot carry an error state.

### 5. Tabs inside a table row

See [Alias Designer](#alias-designer) — one user, and a plainer layout is available.

### 6. Esc clears a search field

`HudSearchField` clears on its ×, not on Esc. The Bind Editor spec requires both — *"which, in a bind editor,
is most of them."* A small change to the widget itself, so every search field in the app gains it.

---

## Tensions with the canon

### 1. Severity as a chip

The Anomalies spec shows severity *"as a chip on its group header."* The canon says *"No gradients, shadows or
rounded 'pills'"* (§0.1) and *"a status colour is the colour of the value TEXT, not a background fill"* (§1).
**The canon's answer is coloured text** — the severity word in its `StatusBadge.State` colour, the way
`HudStatusReadout` shows state. The spec's word *chip* should change to match; the design intent, severity
visible at a glance on the group, survives intact.

### 2. `StatusBadge` draws a pill

`StatusBadge` paints `fillRoundRect` and `drawRoundRect` — a rounded, tinted pill, the **IN SYNC** marker on
the Bindings tab today. The canon lists its deliberate exceptions by name, and this is not one of them. **That
is Krondor's to judge, not BindForge's to fix** — it predates BindForge and may be a known, accepted departure.
What it settles for BindForge is narrower: **new status gets the canon's treatment, not the pill.** ~~Preset
Editor's sync badge can reuse `StatusBadge` as it stands, since that is the existing app's own sync marker.~~

**Resolved 2026-09-21 — the pill goes** (Alan: *"there is already a status. we don't need the pill."*). Two
facts decided it. **The widget has one user** — the Binding Profile's sync marker; `StatusBadge.State`, its
colour list, is used everywhere and stays. And **that state is already shown the canon's way**: the AI tab's
Quick Status panel carries **KEYMAP — IN SYNC** as a `HudStatusReadout` row, label left, value right in its
state colour, no fill. So:

- **Preset Editor shows IN SYNC / DRAFT as a readout value in its state colour**, not a pill.
- **The Binding Profile's pill is replaced the same way** when the Bind Editor grows out of that panel. That
  leaves the `StatusBadge` widget with no caller, so it is deleted then — the enum stays.
- Not a bug ticket: a styling departure on one screen, fixed by the screen that owns it.

---

## What this is not

- **Not a list of every widget.** Forty-nine exist; this names the ones BindForge will reach for.
- **Not behaviour.** Controller capture, install discovery, the ZIP archive and the settings namespacing are
  real work, tracked in [v1.2-scope.md](../../00-overview/v1.2-scope.md) and the
  [porting notes](../../PORTING-NOTES.md#7-open-questions-this-port-creates). They need code, not components.
- **Not a mockup review.** The mockups predate the port; where this document and a mockup disagree, this
  document describes what the kit can build.
