# ED_HUD_REFERENCE.md

A reference for the visual language of the **Elite Dangerous** HUD (vanilla Horizons/Odyssey)
for the
**EliteIntel** project. The single source of truth for HUD component design; a written specification of the language, not a copy of the game's art.

> **Who reads this.** You are an agent (Claude Code) working in the EliteIntel repository. This file is
> the HUD design canon; before any change that touches the UI, check it against the relevant section.

> **What this file holds.** Design ONLY: colour meanings, state behaviour
> (selected/hover/disabled/status), alignment, typography, when an accent frame and when
> flat, which component fits which job. Hex values, pixels and method signatures live in the theme
> layer classes (`HudPalette`, `HudGlyphs`, `HudForms`, `AppTheme`) and the sources. Constant and
> component names are the design→code anchors.

> **How to use it.** Colours are named in words (orange, green…); take the concrete values
> from `HudPalette` by name. If you change the palette or the components, update the matching
> section of this file in the same commit.

## Rules of application

- Styling means replacing raw Swing with the HUD components of the `elite.intel.ui` package layers, not local layout work.
- Colours, sizes, fonts, thicknesses and icon roles come ONLY from `HudPalette` by constant name; glyph primitives come from `HudGlyphs`. Hardcoding is forbidden.
- The colour layer in `HudPalette`: raw colours are named `HUD_COLOR_<HEX>` and reference only a literal colour code; roles are named `HUD_COLOR_ROLE_<SEMANTIC_NAME>` and reference `HUD_COLOR_*`
  directly, with no role→role chains.
- A pattern needed on more than one screen belongs in the HUD layer, not copied in place.

**The theme layer (`elite.intel.ui.theme`) is the source of truth, split by role:**
`HudPalette` holds the tokens (colours, metrics, `HUD_FONT_*` font roles); `HudGlyphs` holds the glyph and icon primitives (`paintHud*`, `*Icon`, `scaledIcon`/`tintIcon`/`dimIcon`); `HudForms` holds the GridBag form helpers (`baseGbc`/`addLabel`/`addField`/…); `AppTheme` holds the component and border factories, the stylers,
`hudModalScaffold` and `applyDarkPalette`. The other `ui` layers: `widget` (HUD components),
`screen`/`dialog` (screens and modals), `render` (table renderers), `support`, `controller`,
`telemetry`, `event`, `i18n`.

---

## I. The design language

## 0. General principles

1. **Dark background, thin lines, no
   depth.** A flat style ("Flat 2.0"). No gradients, shadows or rounded "pills". Frames are thin straight lines.
2. **Colour carries
   meaning.** Orange is the primary working colour. State is expressed by a CHANGE of colour (green/yellow/red/cyan), not by an icon or a pill fill.
3. **Caps plus letter-spacing.** Labels, titles and values are uppercase; section titles carry light letter-spacing.
4. **Selection means
   inversion.** The active or selected row is a solid bright fill with DARK text. This is the main "focus" device in every list and menu.
5. **Values to the right.** In key→value pairs and numeric columns the value is flushed right.
6. **Dimming means
   inactive.** Disabled is the same colour, heavily dimmed (`HUD_COLOR_ROLE_DISABLED`), never a grey "from another palette". Text and icon both fade in one warm tone.

## 1. Colour coding

The ED canon (the Odyssey radar: friendly=green, neutral=blue, alerted=yellow, hostile=red):

- `HUD_COLOR_ROLE_PRIMARY_ACTION` (orange) — normal/working · `HUD_COLOR_ROLE_SUCCESS` (green) — positive/OK/profit ·
  `HUD_COLOR_ROLE_WARNING` (yellow) — attention/expected waiting · `HUD_COLOR_ROLE_DANGER` (red) — danger/ failure/hostile · `HUD_COLOR_ROLE_INFORMATION` (blue) — neutral informational · `HUD_COLOR_ROLE_DISABLED`/`HUD_COLOR_ROLE_SECONDARY_TEXT` — inactive · `HUD_COLOR_ROLE_READOUT_LABEL` — the dimmed label in key→value readout and telemetry blocks (`HudTelemetryBlock`, §7); the same tone as `HUD_COLOR_ROLE_SECONDARY_TEXT` but a separate semantic — do NOT confuse it with disabled · `HUD_COLOR_ROLE_CREDITS_TEXT` — the CMDR credit balance (`HudCommanderBlock`, §7); the same tone as `HUD_COLOR_ROLE_SECONDARY_TEXT`, separate semantic.
- Backings: `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` (the row plate) · `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND` (hover state) ·
  `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` (the table body background and gaps, darker than the plate) · `HUD_COLOR_ROLE_DIALOG_BODY_BACKGROUND` (the modal body, between `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` and the plate, §10.1) · `HUD_COLOR_ROLE_MODAL_SCRIM` (the veil under a modal, §10.1).

**The
rule:** a status colour is the colour of the value TEXT, not a background fill. A fill marks only row SELECTION or ACTIVITY (§6, §4).

**`StatusBadge.State`** (the value colour plus the left tick in `HudStatusReadout`):
`OK`→`HUD_COLOR_ROLE_SUCCESS`, `STANDBY`→`HUD_COLOR_ROLE_WARNING`, `OFFLINE`→`HUD_COLOR_ROLE_DANGER`, `INFO`→`HUD_COLOR_ROLE_INFORMATION`,
`IDLE`→`HUD_COLOR_ROLE_DISABLED`. `STANDBY` (yellow) means normally waiting for the user; "asleep/switched off/ not initialized" is `IDLE` (§0.6). STT `SLEEPING` and stopped LLM/TTS are `IDLE`.

**A command name in a
dialog** (built-in or custom; the reference is `CommandDetailsDialog`) uses `HUD_COLOR_ROLE_INFORMATION`, a deliberate exception (a name would be `HUD_COLOR_ROLE_PRIMARY_TEXT` under §0.2/§11.1) to take load off the orange. NOT for the id or action key, NOT for the catalog table (§6), NOT for a binding id.

**A panel's contextual mode.** Orange and cyan by default; red and blue only if the task demands them.

**An indicator glyph in a status cell is a deliberate
exception.** The canon is status as text colour, with no icon. The exception is SPECIFIC: `CONFLICT` in `StatusCellRenderer` (the Import dialog) carries an orange `HUD_COLOR_ROLE_PRIMARY_ACTION` triangle plus "!" as REINFORCEMENT (the text is still `HUD_COLOR_ROLE_WARNING`). `INVALID`/`OK`
carry no glyph. Do not extend this to other cells without an explicit decision.
> **TODO.** The glyph is a raster (`ImageIcon`/`BufferedImage`). Move it to a `paintHud*` glyph
> the next time this is touched (§13).

**The red slider fill is a deliberate exception.** The canon is red = `HUD_COLOR_ROLE_DANGER`
(danger/failure). The exception is SPECIFIC: the active part of the `HudSlider` track (§4) is saturated red `HUD_COLOR_ROLE_SLIDER_VALUE_TRACK` as a LEVEL INDICATOR (it follows vanilla ED, chosen for readability on the warm track), not a danger signal. Do not extend it to other controls.

## 2. Typography

**Font size comes ONLY from the `HUD_FONT_*` roles; hardcoding (`deriveFont`/`new Font`) is
forbidden.** The weight (`BOLD`/`PLAIN`) is NOT carried by the role — the call site sets it through `deriveFont(Font.BOLD, ROLE)`.

**Base and
steps** (everything from one base): `HUD_FONT_BASE`; `HUD_FONT_XS` < `SM` < `MD` < `LG`. The values live in `HudPalette`.

- **XS:** `HUD_FONT_READOUT_KEY` (a readout/telemetry label, the version and header labels),
  `HUD_FONT_BADGE_ROLE` (`StatusBadge`), `HUD_FONT_BANNER` (`HudBanner`).
- **SM:** `HUD_FONT_TABLE_HEADER` (a table header; one step smaller in compact),
  `HUD_FONT_FIELD_VALUE` (field and metadata values), `HUD_FONT_READOUT_VALUE` (a readout value, the date and the clock balance), `HUD_FONT_SECTION_TITLE` (`hudSectionLabel`/`hudGroupLabel`),
  `HUD_FONT_TAB_COMPACT` (the dense inner `COMPACT` tabs), `HUD_FONT_BUTTON`, `HUD_FONT_CHECKBOX`.
- **MD:** `HUD_FONT_TABLE_ROW` (table rows), `HUD_FONT_COMMANDER_NAME` (CMDR/SHIP in the header),
  `HUD_FONT_TAB_SECTION` (second-level `SECTION` tabs).
-
**LG:** `HUD_FONT_TAB_MAIN` (`MAIN_NAV`), `HUD_FONT_APP_TITLE` (the application name; the dialog header title, §10.1), `HUD_FONT_ICON_BUTTON` (symbol buttons and the ⓘ/"i"/× glyphs).
- **One-off:** `HUD_FONT_CLOCK` (mono, the `HudCommanderBlock` clock), `HUD_FONT_STAT_LG`
  (the large stats in `UsageStatsTabPanel`).

**A new role, not a split of an existing
one.** Two unrelated sites happening to share a size is NOT a reason to split a role (the sizes will diverge later). Create a separate role with the same value.

**A relative
size (`getSize2D()±N`)** is for when the relativity itself carries meaning AND the base is already the right role: the technical sub-label of a two-line cell, decorative titles. Do NOT derive from the LAF default — convert that to an absolute role.

## 3. Tokens (spacing, heights, icons)

Every value lives in `HudPalette`; hardcoding is forbidden.

**Insets and gaps:** `HUD_GAP` (the base step) · `HUD_DIALOG_BODY_INSET` = `HUD_GAP×2`
(the dialog side inset) · `HUD_SEP_W` (the gap between the checkbox and field zones) ·
`HUD_PADDING` / `HUD_PADDING_SMALL`.

**Row and control heights:** `HUD_TABLE_ROW_HEIGHT` / `…_COMPACT`; `HUD_BUTTON_HEIGHT` /
`…_COMPACT`; `HUD_FIELD_HEIGHT`; `HUD_DIALOG_HEADER_HEIGHT` (do NOT confuse with `HUD_BUTTON_HEIGHT` — a header is not a button).

**Icons (`HUD_ICON_*`):** `MAIN` (large nav) · `NAV` (medium) · `SMALL` · `TABLE`
(an affordance inside a cell, smaller than the row height).

**Frames:** `HUD_BORDER_THICKNESS` (standard) · `HUD_BORDER_THICKNESS_ACCENT` (accent). **The typing
caret:** `HUD_CARET_WIDTH`; vertical alignment follows the marker's visual bounds through
`HudGlyphs.paintHudTextCaret`, not a manual pixel offset.

---

## II. Components

## 4. Buttons and actions

References: Station Services (shortcuts), Ship Functions (toggles).

**HudButton** is the action button. One size across a group: width, the `HUD_FONT_BUTTON` bold font, and `HUD_BUTTON_HEIGHT`.

-
**Rest:** `HUD_COLOR_ROLE_PRIMARY_ACTION` text on `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND`, outlined `HUD_COLOR_ROLE_CONTROL_DECORATION`.
- **Pressed:** inverted `HUD_COLOR_ROLE_PRIMARY_ACTION`+`HUD_COLOR_ROLE_SELECTED_TEXT`, only while held.
- **Hover:** `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND`.
  **Disabled:** `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` + `HUD_COLOR_ROLE_DISABLED` + a `HUD_COLOR_ROLE_SECONDARY_BORDER` outline.

**A toggle is a CHANGE OF ACTION
TEXT** (what will happen: `SLEEP`/`WAKE UP`/`STOP SERVICES`). The source of truth is external state, not `isSelected()`. It stays a verb button, not a status line.

**Showing state (not a
button).** When ON/OFF/RETRACTED has to be shown, use a value text in the right-hand column, NOT a tick or a pill slider.

**The discrete numeric
stepper `◄ value ►`** (`HudStepper`). Horizontal FILLED triangles at the LEFT and RIGHT edges of a filled `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND` plate with NO frame (like the OFF checkbox, §5.2), with the value centred. The arrow zones are separated from the value by vertical `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` gaps (like the checkbox gap, §5.2). Arrow states: at rest `HUD_COLOR_ROLE_PRIMARY_ACTION`; on hover a light accent wash over the zone; on press a full `HUD_COLOR_ROLE_PRIMARY_ACTION` fill with the arrow inverted to `HUD_COLOR_ROLE_SELECTED_TEXT` (like a pressed subtle button, §4); at the end of the range the arrow dims to `HUD_COLOR_ROLE_DISABLED`. The value is centred text with NO free input (as in the game). NOT a native `JSpinner` with vertical ▲▼. The arrows are the `paintHudArrowLeft`/`paintHudArrowRight` primitives (§13). Anchor: `HudStepper(min, max, step, initial)`,
`getValue()`/`setValue(int)`; in layout, a fixed width (`fill=NONE`/`weightx=0`).

**The `HudSlider`
scale** (the vanilla ED form; the reference is Options→Audio). A warm brown track plate `HUD_COLOR_ROLE_PANEL_SEPARATOR` across the full width; a dimmed `HUD_COLOR_ROLE_CONTROL_DECORATION` rail with an EDGE inset (`HUD_SLIDER_EDGE_INSET`, it does not touch the edges); the active part from the left up to the knob is a saturated red `HUD_COLOR_ROLE_SLIDER_VALUE_TRACK` fill (the deliberate exception in §1, a level indicator), drawn OVER the tall vertical start tick (0); the knob is a round `HUD_COLOR_ROLE_PRIMARY_ACTION` disc with a `HUD_COLOR_ROLE_BUTTON_TEXT` ring. The value sits above the knob (`HUD_COLOR_ROLE_PRIMARY_ACTION`, travelling with it); it switches easily to showing only while dragging. Snaps to the step. Disabled dims everything to `HUD_COLOR_ROLE_DISABLED` (§0.6). All metrics are `HUD_SLIDER_*` tokens; hardcoding is forbidden. Anchor: `HudSlider(min, max, step, value)`,
`getValue()`/`setValue(int)`/`addChangeListener(ChangeListener)`; in layout, `fill=HORIZONTAL`
(it stretches across the width). NOT a raw `JSlider`.

**The `HudMicMeter` segmented level
meter** (a vertical LED VU; the reference is the microphone monitor). It INDICATES a realtime level; it is NOT an input. Two columns of discrete segments:
**LIVE** (lit up to the current level, the zone colour being `HUD_COLOR_ROLE_DANGER` below the floor, `HUD_COLOR_ROLE_WARNING` from floor to gate, `HUD_COLOR_ROLE_SUCCESS` above the gate) and a narrow
**PEAK trail** (the held maximum, a dim `HUD_COLOR_ROLE_DISABLED` with a light `HUD_COLOR_ROLE_BUTTON_TEXT` cap;
`HUD_COLOR_ROLE_DANGER` on clipping, meaning "too hot"). Unlit segments are `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND`. The floor and gate thresholds are thin labelled rails (`HUD_COLOR_ROLE_SECONDARY_TEXT` / `HUD_COLOR_ROLE_INFORMATION`); below the columns sits the large current value in the status colour (`HUD_FONT_STAT_LG`) plus a `LIVE · OPEN/MARGINAL/CLOSED/HOT` line. It auto-scales to the running peak. Numeric labels sit on the control itself (the zone scale on the left, PEAK at the cap), with no wide side block. The data comes from `AudioMonitorBus` (off-EDT → volatile fields plus `invokeLater(repaint)`), registered through `addNotify`/`removeNotify`. Metrics are `HUD_METER_*` tokens. Anchor: `HudMicMeter`.

**Choosing between them:**

- VALUE indication (key→value) → `HudStatusReadout` (§7.1);
- a LIVE level or meter (realtime) → `HudMicMeter`;
- a TOGGLE button → `HudButton` with changing text;
- a SETTING in a form → the checkbox, §5.2;
- a RANGE with a visible position on a scale (volume, speed) → `HudSlider`;
- a few discrete values compactly, without a scale → `HudStepper` (`◄ value ►`).

**The compact square picker button beside a
field** (choosing a directory or file, to the right of the field). A square whose side equals the height of the ADJACENT field (it follows the field, NOT `HUD_BUTTON_HEIGHT`), and narrow. The glyph is a PRIMITIVE (§13), not Unicode text (the symbol depends on the font and breaks). On a primary fill the glyph is DARK (the inversion in §0.4), not white. The button's side text insets are zeroed in square mode — otherwise the glyph shifts. Add it to the layout with `fill=NONE`/`weightx=0`, or the square will stretch.

**→
Anchors:** `HudButton(label, boolean primary)` (primary=true is the orange fill, false the outline); navigation lists use `HudTabbedPane` (§11) / `HudSection` (§9); two-line items use `HudCommandNameCellRenderer`; the compact picker is `makeFieldButton(glyph|Icon, fieldHeight)` + `HudButton.setSquareSide`, with the ⋮ glyph as `verticalEllipsisIcon` / `paintHudVerticalEllipsis`; an affordance icon (close ×, save-to-file) is `HudGlyphButton(painter, restTint, hoverTint, tooltip, onClick)`
(a glyph primitive, §13, with a `HUD_TABLE_ROW_HEIGHT_COMPACT` footprint and a `HUD_ICON_TABLE` glyph; its only owner is `HudDialogHeader`, and the section header reuses it); into a section header it goes through `HudSection.setHeaderActions` (§9); the slider scale is `HudSlider(min, max, step, value)` (the `HUD_SLIDER_*` tokens, fill colour `HUD_COLOR_ROLE_SLIDER_VALUE_TRACK`); the segmented level meter is `HudMicMeter` (the `HUD_METER_*` tokens; subscribes to `AudioMonitorBus`).

## 5. Form fields

### 5.1 Label plus text field

For "label → field" rows in forms (the reference is TRADE PROFILE):

- **The
  label** is light `HUD_COLOR_ROLE_PRIMARY_TEXT` caps (as in vanilla ED: light label, orange value), at `HUD_FONT_SM`, with NO colon, and NOT mixed case. Strip colons coming from i18n in the bundles (ALL languages), not in the code. One style, `styleFieldLabel` (a single source for `addLabel` and `hudReadoutLabel`); colour and size change centrally inside it.
- **The
  field** is a `HudTextField`, with a warm `HUD_COLOR_ROLE_CONTROL_DECORATION` border (§8) and a `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` background. The value text is
  `HUD_COLOR_ROLE_PRIMARY_ACTION` (orange, pairing with the light label; `styleTextComponent`), the same for single-line and multi-line fields; the only difference is enabled/disabled (`HUD_COLOR_ROLE_DISABLED`). The colour does NOT depend on editable/read-only. A live console or log is its own role (`HudLogArea`), not a field.
- **Disabled (§0.6)** is centralized, with no local hacks: the border follows `isEnabled()` and dims to
  `HUD_COLOR_ROLE_DISABLED` (`hudFieldLine` inside `hudFieldBorder()`/`…WithInfo()`); the text in the field uses `disabledTextColor=HUD_COLOR_ROLE_DISABLED`
  (`styleTextComponent`); and the row label (`addLabel`) dims together with the field to `HUD_COLOR_ROLE_DISABLED`. A group is dimmed by one `setEnabled(false)` on the controls, each of which draws its own disabled state.
- **The info "i" (
  optional)** is a zone INSIDE the field on the right, separated by a `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` gap. Tints: at rest
  `HUD_COLOR_ROLE_CONTROL_DECORATION`; hover `HUD_COLOR_ROLE_PRIMARY_ACTION`; disabled `HUD_COLOR_ROLE_DISABLED`. The glyph is `paintHudInfoGlyph`. A click opens the help and does not place the caret. NOT a separate external button and NOT a Unicode glyph.
- **A picker at the field's edge** (choosing a directory or file) is a compact square button, §4.

**A read-only value in a form.** Choose:

- a short scalar read-only value with NO help → flat `hudReadoutValue` text (§7.2), no border;
- a value that needs an in-field info "i", or a long path with scrolling and selection →
  `HudTextField` + `setEditable(false)` (the border means "a bounded surface", not a sign of input);
- a compact "bounded surface" with no help → `makeMetadataField` (`HudMetadataField`).

Blue underlined action "links" are an ANTI-PATTERN: help is carried by the info "i" inside the control.

**The row
layout** "label→field[→picker/i]" uses the `baseGbc` / `addLabel` / `addField` helpers (see below), not a raw `GridBagConstraints` in place.

**→ Anchors:** `AppTheme.hudReadoutLabel`, `HudTextField.setInfoAction` / `makeTextField(infoAction)`,
`hudFieldBorderWithInfo()`, `HUD_SEP_W`; read-only is `hudReadoutValue` (§7.2) / `makeMetadataField`
(`HudMetadataField`); the picker is `makeFieldButton` (§4); the layout is `HudForms.baseGbc` / `addLabel` / `addField`.

### 5.2 Checkbox

The ED checkbox is NOT a LAF tick but a control row `[marker | gap | text]` whose state is carried by the fill (the inversion in §0.4):

-
**ON:** a `HUD_COLOR_ROLE_PRIMARY_ACTION` plate; the marker is a box outline plus a filled `HUD_COLOR_ROLE_SELECTED_TEXT` square; the text is `HUD_COLOR_ROLE_SELECTED_TEXT` caps.
-
**OFF:** a `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND` plate; the marker is an empty `HUD_COLOR_ROLE_CONTROL_DECORATION` box; the text is `HUD_COLOR_ROLE_SECONDARY_TEXT` caps.
-
**Disabled:** a `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND` plate; the marker is an empty `HUD_COLOR_ROLE_DISABLED` box; the text is `HUD_COLOR_ROLE_DISABLED`.

A `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` gap divides the marker from the text. The marker is straight lines, with no tick shape or rounding.

**The info "i" (optional)** makes the row `[marker | gap | text | gap | i]`. The tint follows the row: OFF
`HUD_COLOR_ROLE_CONTROL_DECORATION`; ON `HUD_COLOR_ROLE_SELECTED_TEXT`; disabled `HUD_COLOR_ROLE_DISABLED`; hover over the zone `HUD_COLOR_ROLE_PRIMARY_ACTION`. A click opens the help and does NOT toggle. With no help the zone is not drawn.

**→ Anchors:** `HudCheckBox` (height `HUD_TABLE_ROW_HEIGHT_COMPACT`), `setInfoAction` /
`makeCheckBox(label, sel, infoAction)`, `paintHudInfoGlyph`, `paintHudCheckMarker`, `HUD_SEP_W`.

### 5.3 Combo

The reference is the ED combo (PRIMARY/BORDERLESS): a warm dark background, orange text, and a flat ▼ with no button box. NOT the native LAF.

-
**Collapsed** — a `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` background, `HUD_COLOR_ROLE_PRIMARY_ACTION` text (the value is orange, as in vanilla ED; placeholder and muted text are `HUD_COLOR_ROLE_SECONDARY_TEXT`), and a `hudFieldBorder()` border (`HUD_COLOR_ROLE_CONTROL_DECORATION`). The ▼ is flat (`HUD_COLOR_ROLE_PRIMARY_ACTION`; disabled
`HUD_COLOR_ROLE_DISABLED`) at the edge, with no box or separator. The grey editor↔▼ separator is a FlatLaf bug, suppressed globally with `ComboBox.buttonSeparatorWidth=0`.
- **The list (
  popup)** — a `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` backing; `HUD_COLOR_ROLE_PRIMARY_ACTION` items; the selected one `HUD_COLOR_ROLE_PRIMARY_ACTION`+`HUD_COLOR_ROLE_SELECTED_TEXT`; a warm `HUD_COLOR_ROLE_CONTROL_DECORATION` border. The insets are `HUD_COMBO_ITEM_INSET_V/H` and the font `HUD_FONT_FIELD_VALUE`. The list is rendered by the factory's internal renderer, NOT an external `setRenderer`.
- **A combo is an INPUT
  field**: on a selected table row it stays warm and is not repainted `HUD_COLOR_ROLE_PRIMARY_ACTION`.
- **Text selection** is warm: `ComboBox.selectionBackground=HUD_COLOR_ROLE_PRIMARY_ACTION`,
  `selectionForeground=HUD_COLOR_ROLE_SELECTED_TEXT` (globally).
-
**Disabled** is warm and dimmed (§0.6): a `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` background, with the text and ▼ in `HUD_COLOR_ROLE_DISABLED`.

**One
API.** A combo goes ONLY through `HudComboBox`, never `new JComboBox` plus a manual `styleComboBox`. Item text comes from `labelFn`, not an external `setRenderer`. Three entry points:

- **The ordinary combo** — `new HudComboBox<>(E[]/ComboBoxModel[, labelFn[, mutedWhen]])`.
  `mutedWhen` (`Predicate<E>`) dims an unselected row to `HUD_COLOR_ROLE_SECONDARY_TEXT` (placeholder/none). The `ComboBoxModel` constructor is for dynamic models (audio devices, bindings).
- **An editable picker with
  search** — `HudComboBox.picker(E[], labelFn, BiPredicate matches)`. It is encapsulated (do not set the editable flag from outside). Behaviour: an empty field gives the full list; typing filters by `matches`. Do NOT overwrite the editor through `setSelectedItem` while filtering.
- **A table cell** — `HudComboCellEditor<E>`: it holds `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` and does not invert.

**Idempotence.** `styleComboBox` installs `HudComboBoxUI` ONLY if it is not there already (`setUI` recreates the editor → a new `Document`, orphaning the filter's `DocumentListener`).

**The opt-out editor (
§12).** A picker's editor is marked `HUD_COMBO_EDITOR_LOCKED` after the initial styling — otherwise `applyDarkPalette` overwrites its `EmptyBorder` with `hudFieldBorder()`.

> **TODO.** The search filter is manual (`DocumentListener` → rebuilding the model). If there come to be
> many pickers, consider GlazedLists `AutoCompleteSupport`, at the cost of integrating it with the HUD canon.
> Do NOT adopt it without an explicit decision.

**→
Anchors:** `HudComboBox` (constructors `E[]`/`ComboBoxModel` × `[labelFn][, mutedWhen]`; the `picker(E[], labelFn, matches)` factory) plus `HudComboBoxUI`. The ▼ is `paintHudArrowDown`. The style is `styleComboBox` (idempotent). The cell is `HudComboCellEditor<E>`. Globally in
`AppView.installDarkDefaults`: `ComboBox.disabled*` → warm, `selectionBackground=HUD_COLOR_ROLE_PRIMARY_ACTION`/
`selectionForeground=HUD_COLOR_ROLE_SELECTED_TEXT`, `buttonSeparatorWidth=0`. Tokens: `HUD_COMBO_ITEM_INSET_V/H`,
`HUD_PICKER_FIELD_WIDTH/HEIGHT`, opt-out `HUD_COMBO_EDITOR_LOCKED`.

### 5.4 Segmented selector (radio group)

A mutually exclusive "one of" choice is NOT a round LAF `JRadioButton` (a circle breaks §0.1, and as a checkbox row a radio is indistinguishable from §5.2). The canon already knows how to say "pick exactly one":
inversion by fill (§0.4, the selected row in §6, the active tab or item in §11). The control is a bar of equal segments separated by a `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` gap (like marker↔text in §5.2 or `intercellSpacing` in §6), with exactly one filled. The height is `HUD_TABLE_ROW_HEIGHT_COMPACT` and the font `HUD_FONT_CHECKBOX` bold caps — a relative of the checkbox. The palette is identical to §5.2:

- **Selected:** a `HUD_COLOR_ROLE_PRIMARY_ACTION` plate; `HUD_COLOR_ROLE_SELECTED_TEXT` text.
- **Not selected:** a `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND` plate; `HUD_COLOR_ROLE_SECONDARY_TEXT` text.
- **Hover (unselected):** the text becomes `HUD_COLOR_ROLE_PRIMARY_ACTION` (the plate is unchanged).
- **Disabled:** a `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND` plate; `HUD_COLOR_ROLE_DISABLED` text (§0.6).

No outline around the control itself (as with the checkbox; a box frame is an accent, §9, not the default). A programmatic
`setSelectedIndex` does NOT fire `ChangeListener` (as `setSelected` does not on a button) — the listener fires on clicks only.

**→ Anchors:** `HudSegmentedControl(String[] labels, int selectedIndex)`, `getSelectedIndex()` /
`setSelectedIndex(int)` / `addChangeListener(ChangeListener)`; opt-out `HUD_LOCKED_FOREGROUND` (§12). Height `HUD_TABLE_ROW_HEIGHT_COMPACT`, gap `HUD_SEP_W`, font `HUD_FONT_CHECKBOX`.

## 6. Tables

References: Commodities Market, Sub-Targets.

- **Column
  headers** are `HUD_COLOR_ROLE_SECONDARY_TEXT` caps with a thin warm `HUD_COLOR_ROLE_CONTROL_DECORATION` rail under the header (NOT the cold `HUD_COLOR_ROLE_SECONDARY_BORDER`). Mandatory on ALL tables.
- **Group separators** (CHEMICALS/FOODS) are bright caps with no fill, on their own row.
- **Data rows** sit on a `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` plate. NOT zebra, NOT a grid.
- **The body
  background** is `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` (darker than the plate). The division is a `HUD_COLOR_ROLE_APPLICATION_BACKGROUND` gap via `intercellSpacing`
  (horizontal and vertical). NOT transparency — the background is set explicitly.
-
**Values** are uppercased in the RENDERER (`toUpperCase`). Colour: a parameter or setting → `HUD_COLOR_ROLE_PRIMARY_ACTION`; an identifying name → `HUD_COLOR_ROLE_PRIMARY_TEXT`. Status data follows §1.
- **Numeric columns** align right; text columns align left.
- **A combo
  column** uses a renderer with a dimmed ▼ (`HUD_COLOR_ROLE_CONTROL_DECORATION`; `HUD_COLOR_ROLE_SELECTED_TEXT` on the selected row); the editor is `HudComboBox` (§5.3), always warm, and does NOT invert with the row selection.
- **An affordance icon** (the gear) is small, below the row height. Its tint follows the row:
  at rest `HUD_COLOR_ROLE_CONTROL_DECORATION`, on the selected row `HUD_COLOR_ROLE_SELECTED_TEXT`. The size is `HUD_ICON_TABLE`.

**Selected
row:** a `HUD_COLOR_ROLE_PRIMARY_ACTION` fill plus `HUD_COLOR_ROLE_SELECTED_TEXT` text, set EXPLICITLY in the renderer (FlatLaf overrides it). One at a time.
**Disabled row:** `HUD_COLOR_ROLE_DISABLED`.

**→
Anchors:** `HudTable.style()/styleCompact()`, `HudTable.dataPlaneScrollPane()` (§12, not a raw `JScrollPane`). Heights `HUD_TABLE_ROW_HEIGHT*`. Combo cells are a `DefaultCellEditor` over a `HudComboBox`. Icons are `HudGlyphs.scaledIcon`+`tintIcon`. Fonts: `style()`→`HUD_FONT_TABLE_ROW`,
`styleCompact()`→`HUD_FONT_SM`; the header is `HUD_FONT_TABLE_HEADER`.

## 7. Readouts and status lines

### 7.1 HudStatusReadout (key→value with state)

References: the Outpost dialog, Ship Functions, the Station faction block.

- **The label on the
  left** is `HUD_COLOR_ROLE_SECONDARY_TEXT` caps with no colon (ED separates by column, not punctuation).
- **The value on the right** is flushed RIGHT, coloured per §1 (`ON`→normal, `OFF`→dimmed, dangerous→red).
- A thin accent tick on the left. The right column stays even.

**→
Anchor:** `HudStatusReadout` (a `HUD_COLOR_ROLE_SECONDARY_TEXT` label, the value on the right in the `StatusBadge.State` colour).

### 7.2 Read-only key→value in detail dialogs

(the reference is `CommandDetailsDialog`) — a SEPARATE model from `HudStatusReadout` (which carries `StatusBadge.State`
and puts the value on the RIGHT). Two columns:

- **The label** is caps with no colon (`hudReadoutLabel`).
- **The
  value** is flat text with NO border, aligned left (NOT right). Colour: a name → `HUD_COLOR_ROLE_INFORMATION` (§1), anything else → `HUD_COLOR_ROLE_PRIMARY_ACTION` (orange, like field values). Do NOT force caps in the renderer — if caps are needed, uppercase at the source.
- **A border means INPUT**: read-only is flat text; multi-line or editable is an area with `hudFieldBorder()` (§5.1).

**→
Anchor:** `AppTheme.hudReadoutValue(value, color)` (a flat `JLabel` with no border), the counterpart to `hudReadoutLabel`. One helper for every key→value dialog.

### 7.3 Banners and progress bars

References: Quick Status, Community Goal tiers, market profit bars.

- An indicator line: a label plus a value or state in the §1 colour.
- Progress and levels are a row of thin segment divisions in the status colour (the cyan tier bars).
- Rating rows (S/A/B/C/D/E/F) are a letter in a box plus a line to the right; a failure (`INSUFFICIENT`) is red.

**A notification banner or a hint at the bottom of a panel is
ONLY `HudBanner`.** A left accent rail plus text in the state colour (`StatusBadge.State`). A caution hint is `STANDBY` (yellow) with a leading ⚠ glyph (the 3-argument constructor, `leadingWarnGlyph=true`). Hand-built warning strips (`JLabel` + a Unicode "⚠" + `HUD_COLOR_ROLE_WARNING_PANEL_BACKGROUND`)
are an ANTI-PATTERN: both the bindings hint and the "changes take effect" note go through `HudBanner`.

**A long hint in a narrow
column** uses `HudBanner.multiline(text, state)`: the text wraps by word (a `JTextArea` with a proportional font) instead of being clipped. Do NOT build an `<html width=…>` hack.

**Disabled (
§0.6).** `HudBanner` follows `setEnabled`: the rail and text dim to `HUD_COLOR_ROLE_DISABLED`, and return to the state colour when re-enabled. Dim it together with the inactive column or section.

**→ Anchors:** `HudBanner(text, state[, leadingWarnGlyph])` (single notifications; the ⚠ is
`warningGlyphIcon`/`paintHudWarningGlyph`, §13); `HudBanner.multiline(text, state)` (wrapping);
`HudStatusReadout`; progress is a segmented `HUD_COLOR_ROLE_INFORMATION`/`HUD_COLOR_ROLE_SUCCESS` bar.

### 7.4 The dialogue log

`HudLogArea.chat` puts CMDR lines on the left (a green rail) and Vega's on the right (cyan). The active Vega line gets an opaque rail and a cyan fill fading from the rail towards the text. This is a deliberate exception to the ban on gradients in §0. After the last character is typed, the fill and rail fade smoothly back to the ordinary look over `HUD_CHAT_ACTIVE_HOLD_MS`.

## 8. Scrollbars

Service chrome, not a carrier of meaning. Status colour (cyan included) does NOT apply.

- **The thumb** is a flat `fillRect` in warm `HUD_COLOR_ROLE_DISABLED`. **The
  track** is a `fillRect` in `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`. Hover does not "light up". The arrow buttons are removed and the bar is narrow.

> **Warm thumb, cold frames.** `HUD_COLOR_ROLE_FRAME_BORDER` (cold) is only for button and toolbar frames;
> do NOT use it for the thumb. **Field borders** are the warm `HUD_COLOR_ROLE_CONTROL_DECORATION`. **A table's
> scroll wrapper has no
border**: the table "floats" on `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`. Framing is the §9 device (FRAMED),
> and is not applied to tables by default.

**→
Anchor:** `HudScrollPane` → `AppTheme.styleScrollPane()`. Every scrollable area is a `HudScrollPane`, not a raw `JScrollPane`.

---

## III. Assembly patterns

## 9. Sections: FLAT vs FRAMED

A box frame is an ACCENT, not the default (several in a row become a "box in a box", which is noise).

- **FRAMED** (`new HudSection`, `compactCard`) is a `HUD_COLOR_ROLE_CONTROL_DECORATION` frame plus a
  `HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND` header fill. For separated accent widgets: sidebars, the commander block, cards.
- **FLAT** (`HudSection.flat`, `compactFlat`) has no frame or fill. A caps title (`HUD_COLOR_ROLE_PRIMARY_ACTION`)
  plus a warm `HUD_COLOR_ROLE_CONTROL_DECORATION` rail; the body background is TRANSPARENT. For a tab's working sections.

**The rule:** a working area is FLAT; a separated accent is FRAMED.

**Inside a modal, always
FLAT.** The window frame comes from the §10.1 scaffold; a FRAMED section under it is a second frame ("a box in a box"), an anti-pattern.

**Two columns side by
side** use `HudTwoColumns(left, right)`: equal halves (`GridLayout 1×2`) plus a central vertical `HUD_COLOR_ROLE_PANEL_SEPARATOR` divider (warm, quieter than the section rail in §10.1; NOT the cold `HUD_COLOR_ROLE_SECONDARY_BORDER`), drawn in `paintComponent` so the palette does not overwrite it. Children fill their half; to align content to the top, wrap the column in a `BorderLayout` and add it to `NORTH`. References: AI Services (local/cloud setup), `CustomCommandEditorDialog` (identity/steps). Do NOT build a local `GridBag` equal-column hack in place.

**Actions in a section
header** use `HudSection.setHeaderActions(JComponent...)`: one or more affordance icons (`HudGlyphButton`, §4) at the right edge of the title strip, opposite the title, left to right in argument order (the last one sits at the right inset), for actions over the section's content (for example save plus clear on a log panel). They go into a `GridLayout` strip with a `HUD_GAP_TIGHT` gap; the STRIP is pinned to the title row height (the header does not grow), while
`GridLayout` stretches each icon to that height (the glyph centres rather than being clipped). The right inset is the shared
`HEADER_H_INSET` (which carries the header border). The actions are uniform glyph buttons (equal cell widths). Do NOT lay out a button in place inside the section body. Reference: the AI tab, the "Diagnostics" header.

## 10. Dialogs

References: Universal Cartographics, Promotion to Master, Community Goal.

- A panel with a clear frame over a dimmed background. The title in caps, with an icon and a line.
- **Dimming the scene is MANDATORY** (the §10.1 scrim): without the veil the window merges into the screen.
- **Buttons:** primary → a bright fill with dark text (`makeButton`); the rest → a dim outline (`makeButtonSubtle`).
- **Footer
  layout:** the left slot goes LEFT; primary goes RIGHT; EXTRA sits left of primary. The layout comes from `HudModalSpec` — do not lay out WEST/EAST by hand. The former "primary on the left" canon is REVOKED.
- **One footer for
  everything:** both modals and tab footers are assembled by `HudFooter.build(modal, …)`. The only difference is the left slot: modal (`modal=true`) gets `BACK`/dismiss; non-modal (`modal=false`) gets status or info, and
  **`BACK` is forbidden** (the flag is what guarantees that).
- **"Unsaved changes" in a SAVE footer is the
  standard `HudUnsavedHint`** (`HUD_COLOR_ROLE_WARNING` plus the ⚠ glyph, hidden by default, `status.unsavedChanges`), immediately LEFT of `SAVE` in the right-hand group; shown and hidden by dirty state, with `SAVE` dimmed when there are no edits. NOT a full-width banner plate above the buttons.
- **Dismiss is
  always `BACK`** (the `button.back` key, subtle), not `CLOSE` or `CANCEL`. NOT a primary fill. Only in a modal footer.
- **The default
  button** is set by the dialog ITSELF after `setContentPane`. Usually primary; it may choose otherwise (the reference is `CommandDetailsDialog`: default=`BACK`, so that Enter does not run the command).
- **The object's title
  block** goes in NORTH: the name in `HUD_COLOR_ROLE_INFORMATION` bold and large (`HUD_FONT_APP_TITLE`, caps)
  plus the id or key in `HUD_COLOR_ROLE_SECONDARY_TEXT` (`HUD_FONT_READOUT_KEY`) beneath it. Duplicating it in the key→value body is situational (a form whose name is already in the title removes it from key→value).
- NPC lines use guillemets.

**Confirm and yes/no use `HudConfirmDialog`,
NOT `JOptionPane`.** A reusable HUD modal on the §10.1 scaffold: `HudConfirmDialog.confirm(parent, title, message, primary, dismiss)` (2 buttons → boolean) or
`HudConfirmDialog.show(parent, title, message, primary, extra, dismiss)` (3 buttons → a `Result`
of PRIMARY/EXTRA/DISMISS). ESC and the close × give DISMISS. A raw `JOptionPane.showConfirmDialog`/`showOptionDialog`
is an anti-pattern.

**→
Anchors:** assembly is ONLY `AppTheme.hudModalScaffold(HudModalSpec)` (§10.1). The title block is `AppTheme.commandTitleBlock`. Body sections are `HudSection.flat` (§9). Confirm is `HudConfirmDialog`.

### 10.1 The dialog scaffold (header + body + frame + footer + scrim)

The OS system title bar breaks §0.1/§10 → `setUndecorated(true)` plus a custom HUD header.

**Assembly goes ONLY through the single
scaffold.** `AppTheme.hudModalScaffold(HudModalSpec)` returns a wrapper `JPanel` for `setContentPane`. It is composition, NOT a base class. `HudDialogHeader` and
`HudFooter`/`hudFooterBorder()` are the scaffold's INTERNALS and are not laid out directly in windows.

**`HudModalSpec` (a builder):** `title` (nullable → no header), `onClose`, `body`, `scrollBody`
(bool → viewport bg `HUD_COLOR_ROLE_DIALOG_BODY_BACKGROUND`), and buttons with the `primary`/`dismiss`/`extra` roles (§10). The scaffold does NOT create buttons; it accepts finished ones. ESC and the default button are set by the window after `setContentPane`.

**The side inset** is the single `HUD_DIALOG_BODY_INSET` token (`HUD_GAP×2`). With `scrollBody`
the `body` itself carries no border of its own. The 18/16/12 literals are revoked.

**The header is a cold anchor over a warm body** (separated by a change of temperature, not brightness).

- **Background** — `HUD_COLOR_ROLE_DIALOG_HEADER_BACKGROUND`. NOT `HUD_COLOR_ROLE_PRIMARY_ACTION`, NOT warm tones.
- **Accent** — a bottom `HUD_COLOR_ROLE_PRIMARY_ACTION` rail (`HUD_BORDER_THICKNESS_ACCENT`).
- **Title** — bold caps `HUD_FONT_APP_TITLE` in `HUD_COLOR_ROLE_DIALOG_TITLE_TEXT`.
- **The logo anchor on the
  left** — `elite-logo`, tinted `HUD_COLOR_ROLE_CONTROL_DECORATION`, at `HUD_ICON_NAV`, decorative.
- **The close ×** — `paintHudCloseGlyph`: at rest `HUD_COLOR_ROLE_CONTROL_DECORATION`, hover `HUD_COLOR_ROLE_DANGER`.
- **Height** — `HUD_DIALOG_HEADER_HEIGHT` (NOT `HUD_BUTTON_HEIGHT`).

**The
body** is `HUD_COLOR_ROLE_DIALOG_BODY_BACKGROUND`: the semantic role of a modal's body; its value may be an alias of the base background. Do NOT use `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`/`HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND` directly in its place.

**The footer divider
rail** is `HUD_COLOR_ROLE_PANEL_SEPARATOR` (quieter than the section rail `HUD_COLOR_ROLE_CONTROL_DECORATION`
and the header rail `HUD_COLOR_ROLE_PRIMARY_ACTION` — three lines of different weight). `hudFooterBorder()`: side inset 0.

**The window
frame** is `HUD_COLOR_ROLE_PANEL_SEPARATOR` at `HUD_BORDER_THICKNESS_ACCENT`. NOT `HUD_COLOR_ROLE_PRIMARY_ACTION`
(it competes with the header rail), NOT `HUD_COLOR_ROLE_CONTROL_DECORATION` at 1px (it merges at the corners). A `MatteBorder`
on the scaffold wrapper. Drag by the header; the close × intercepts its own events.

**The
scrim** is a `HUD_COLOR_ROLE_MODAL_SCRIM` veil on the owner window's `glassPane`. It is set before showing and removed on close. The scaffold does NOT orchestrate the scrim — that happens outside through `runWithModalScrim(owner,
showModal)`, with the owner being `SwingUtilities.getWindowAncestor(parent)`.

> **TODO (transitional inconsistency).** The scrim is enabled only on `CommandDetailsDialog`;
> the other 9 modals call `setVisible(true)` with no veil. Enable them ALL AT ONCE, not one by one.

Do NOT duplicate a second title inside the window.

**→ Anchors:** `AppTheme.hudModalScaffold(HudModalSpec)` → a wrapper `JPanel` for `setContentPane`.
`HudModalSpec`: the primary/dismiss/extra roles, `scrollBody`. Internals: `HudModalScaffold.build`;
`HudDialogHeader(title, onClose)` (opt-out `HUD_LOCKED_FOREGROUND`; drag by the header); the footer is `HudFooter.build(modal, back, status, trailing)` / `hudFooterBorder()`. The scrim from outside:
`runWithModalScrim(owner, show)`, with the owner being
`SwingUtilities.getWindowAncestor(parent)`. A special case of manual scrolling: `SettingsPopup`
passes a `hudScrollPane` as the `body` with `scrollBody=false`.

## 11. Navbar and tabs

References: the Ship panel tabs, the ED top nav, Station Services.

**Tabs** are a row in caps with a thin rail beneath the row.

-
**Active**: a bright box fill (SUB-TARGETS) or a `HUD_COLOR_ROLE_PRIMARY_ACTION` underline; the inactive ones are dimmer.
- **`SECTION` (the second level,
  ACTIONS/SETTINGS)**: the active one is a filled box, `HUD_COLOR_ROLE_SECTION_TAB_ACTIVE_BACKGROUND`+`HUD_COLOR_ROLE_SELECTED_TEXT`
  (the inversion in §0.4). The box reaches the bottom rail; NOT an underline, because against a background of many section rails the tab strip would be lost. Beneath the row runs a full-width `HUD_COLOR_ROLE_SECTION_TAB_ACTIVE_UNDERLINE` rail. The first tab carries a light left inset from the start of the rail (`tabAreaInsets.left`).
  `COMPACT` (the dense inner ones) keeps the underline.
- Icon tabs: the active one has a solid fill and a dark icon.

**Navigation lists** (Station Services, the market sidebar) are items on a faint backing with thin dividers. **The
active one is a solid fill plus dark
text** (`HUD_COLOR_ROLE_PRIMARY_ACTION`+`HUD_COLOR_ROLE_SELECTED_TEXT`), one at a time. Group titles are dim caps. The icon on the left is monochrome in the item's colour.
**A two-line
item:** the top is the name (`HUD_COLOR_ROLE_PRIMARY_TEXT`), the bottom a `HUD_COLOR_ROLE_SECONDARY_TEXT` technical sub-label (which we do not recolour). On the selected row both become `HUD_COLOR_ROLE_SELECTED_TEXT`.

**→ Anchor:** `HudTabbedPane` at the `MAIN_NAV` (§11.1) and `SECTION`/`COMPACT` levels; two-line items use
`HudCommandNameCellRenderer`.

### 11.1 App header plus the MAIN_NAV navbar

References: the ED top nav plus the station info panel. The canon is `TopStatusBar` + `HudTabbedPane(MAIN_NAV)`.

**The
header (`TopStatusBar`).** On the left, the application name in caps (`HUD_COLOR_ROLE_PRIMARY_TEXT`, bold) plus the version (`HUD_COLOR_ROLE_SECONDARY_TEXT`). On the right, "label→value" pairs: the label (`CMDR`/`SHIP`) in `HUD_COLOR_ROLE_SECONDARY_TEXT` caps with no colon; the value in
`HUD_COLOR_ROLE_PRIMARY_TEXT` caps, bold. NOT cyan or `HUD_COLOR_ROLE_PRIMARY_ACTION`: a name is a value, not a status.

**The navbar
rails:** the upper one (header↔tabs) is `HUD_COLOR_ROLE_CONTROL_DECORATION` and thinner; the lower one (navbar↔body) is
`HUD_COLOR_ROLE_PRIMARY_ACTION` and thicker. Do NOT use the cold `HUD_COLOR_ROLE_SECONDARY_BORDER`.

**The active tab is an
inversion**: a `HUD_COLOR_ROLE_MAIN_TAB_ACTIVE_BACKGROUND` fill plus `HUD_COLOR_ROLE_SELECTED_TEXT`. The fill has a vertical gap from the rails. There is NO underline (that belongs to SECTION/COMPACT, §11).

**Inactive
ones:** `HUD_COLOR_ROLE_SECONDARY_TEXT` text with a `HUD_COLOR_ROLE_CONTROL_DECORATION` icon. Disabled is `HUD_COLOR_ROLE_DISABLED`.

**→ Anchors:** `TopStatusBar`, `HudTabbedPaneUi`.

---

## IV. Rules

## 12. Palette opt-out

A component that carries its own background or foreground marks itself with an opt-out client property, and the palette skips it. That is how the buttons, the tables (`dataPlaneScrollPane()`), the header (`HUD_LOCKED_FOREGROUND`) and a picker's editor (`HUD_COMBO_EDITOR_LOCKED`; otherwise the palette gives it `hudFieldBorder()` → a visible vertical line at the ▼) are done. `styleComboBox` is idempotent with respect to `setUI` (§5.3). The flags live in `AppTheme`.

**A field with an info zone (§5.1)** obliges the palette (`styleTextComponent`) to keep the wide
`hudFieldBorderWithInfo()` rather than resetting to `hudFieldBorder()`: otherwise the reserve for the "i" is lost and long text runs over the glyph. This is determined through `HudTextField.hasInfoZone()`, not a client property.

## 13. Checklist

- Colours, fonts, heights, icons and border thicknesses come ONLY from `HudPalette` by name. Hardcoding is forbidden. Raw colours are only `HUD_COLOR_<HEX>`; roles are only `HUD_COLOR_ROLE_<SEMANTIC_NAME>` as a direct alias of `HUD_COLOR_*`.
- Font size comes ONLY from the `HUD_FONT_*` roles (§2); a hardcoded size is forbidden.
- Icon size comes from a `HUD_ICON_*` role (§3); hardcoded pixels are forbidden.
- Border thickness comes from a role (`HUD_BORDER_THICKNESS` / `HUD_BORDER_THICKNESS_ACCENT`); hardcoding is forbidden.
- UI text comes only from `MultiLingualTextProvider.getText("key")`, with no literals.
- Row selection is `HUD_COLOR_ROLE_PRIMARY_ACTION`+`HUD_COLOR_ROLE_SELECTED_TEXT`. State is text colour (§1). Disabled is dimming in the same colour (`HUD_COLOR_ROLE_DISABLED`), not a "grey from another palette" (§0.6).
- Flat straight forms: no pills, gradients or shadows. Scrollbars are `fillRect`, with no cyan.
- Chrome and secondary labels use `HUD_COLOR_ROLE_SECONDARY_TEXT`/`HUD_COLOR_ROLE_DISABLED`, not a status colour and not the cold
  `HUD_COLOR_ROLE_FRAME_BORDER` (field borders are the warm `HUD_COLOR_ROLE_CONTROL_DECORATION`).
- Tables (§6) are a `HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND` plate on `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`, with no zebra or grid; division by `intercellSpacing`; selection set EXPLICITLY as `HUD_COLOR_ROLE_PRIMARY_ACTION`+`HUD_COLOR_ROLE_SELECTED_TEXT` in the renderer; hover `HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND`; caps, colour and alignment in the renderer.
- An affordance icon in a cell is `HUD_ICON_TABLE`, tinted by the row (`HUD_COLOR_ROLE_CONTROL_DECORATION` at rest, `HUD_COLOR_ROLE_SELECTED_TEXT` when selected).
- A combo (§5.3) is `HudComboBox`: warm background, flat ▼, warm list; it does NOT invert with the row selection. No `HUD_COLOR_ROLE_INFORMATION`/`HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND`.
- Primitives needed in more than one place live in `HudGlyphs`: ▼ `paintHudArrowDown`; ▲ `paintHudArrowUp`; ◄ `paintHudArrowLeft`; ► `paintHudArrowRight`;
  "i" `paintHudInfoGlyph`; × `paintHudCloseGlyph`; the checkbox marker `paintHudCheckMarker`; ⋮ `paintHudVerticalEllipsis`; ⚠ `paintHudWarningGlyph`; ⤓ save/download `paintHudSaveGlyph`; 🗑 clear/trash `paintHudTrashGlyph`; the caret `paintHudTextCaret`; tinting `tintIcon`; alpha dimming `dimIcon`. Glyphs are primitives, NOT `drawString`/Unicode and NOT rasters.
- The info "i" lives INSIDE the control (§5.2/§5.1) through `setInfoAction`. Blue links are an anti-pattern.
- Tooltips (`setToolTipText`) are styled GLOBALLY through the `UIManager` `ToolTip.*` keys in `AppView.installDarkDefaults`
  (a dark `HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND` plus a warm `HUD_COLOR_ROLE_CONTROL_DECORATION` rail at `HUD_BORDER_THICKNESS`,
  `HUD_COLOR_ROLE_PRIMARY_TEXT` text, and the `HUD_FONT_TOOLTIP` font — otherwise it inherits the large `HUD_FONT_UI_DEFAULT`); popup shadows are removed with `Popup.dropShadowPainted=false` (the HUD has no shadows). A custom tooltip in place is an anti-pattern.
- A key label is `hudReadoutLabel` (`HUD_COLOR_ROLE_SECONDARY_TEXT` caps with no colon). Strip colons in i18n (ALL languages).
- Read-only key→value (§7.2) is `hudReadoutValue(value, color)`: flat text, mixed case. A field border belongs only to an input or an area.
- A dialog's title block (§10) is `commandTitleBlock(name, id)` in NORTH.
- A modal dialog (§10.1): ONLY `AppTheme.hudModalScaffold(HudModalSpec)`. Undecorated. Dismiss=`BACK` subtle on the left, primary on the right. The scrim from outside is `runWithModalScrim` (the TARGET is all of them at once).
- Confirm, yes/no and save-discard use `HudConfirmDialog` (§10), NOT `JOptionPane.showConfirmDialog`/`showOptionDialog`.
- Warm styling outside the palette default is protected by an opt-out client property (§12).
- A modal body's sections are `HudSection.flat` (§9). FRAMED inside a modal is an anti-pattern.
- A shortcut panel uses `HudButton(primary=false)`, with the toggle changing the action text (§4).
- On/off in a form is `HudCheckBox` (§5.2), with no LAF tick.
- A "one of" choice is `HudSegmentedControl` (§5.4): a segmented bar, inversion fill on the selected one, divided by `HUD_COLOR_ROLE_APPLICATION_BACKGROUND`. NOT a round LAF `JRadioButton`.
- A range on a scale is `HudSlider` (§4): a brown track, a red `HUD_COLOR_ROLE_SLIDER_VALUE_TRACK` fill (the §1 exception), a round `HUD_COLOR_ROLE_PRIMARY_ACTION` knob with a `HUD_COLOR_ROLE_BUTTON_TEXT` ring, and the value above the knob; metrics `HUD_SLIDER_*`. NOT a raw `JSlider`.
- A realtime level meter is `HudMicMeter` (§4): segmented LIVE plus a PEAK trail, `HUD_COLOR_ROLE_DANGER`/`HUD_COLOR_ROLE_WARNING`/`HUD_COLOR_ROLE_SUCCESS` zones,
  `HUD_COLOR_ROLE_SECONDARY_TEXT`/`HUD_COLOR_ROLE_INFORMATION` threshold rails, and labels on the control; metrics `HUD_METER_*`. NOT a hardcoded palette or fonts.
- A pattern needed on more than one screen belongs in the HUD layer.

---

## Appendix: the Commander block

The `HudCommanderBlock` widget. The reference is the ED info panel (a large clock, the date, the balance).

- **The ED
  clock** is `HUD_COLOR_ROLE_PRIMARY_ACTION` mono bold and large; the date beneath it is `HUD_COLOR_ROLE_SECONDARY_TEXT` plain (`dd MMM yyyy`, the month in caps), the year is the real one
  **+1286**, and the time is **UTC**.
- **The
  balance** is `HUD_COLOR_ROLE_SECONDARY_TEXT` per §7.1, with comma thousands separators plus ` CR`. Hide it at 0 or below.
- **The logo** is dimmed with alpha so it does not compete with the buttons.
