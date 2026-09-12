# Porting Notes — EDO StellarCore → Elite-Intel

**Ported:** 2026-08-30
**Ported from:** the EDO StellarCore documentation set, which is **superseded and not an authority**. It
was written to be platform-agnostic for a standalone build that is not happening; **anything in it useful
to Elite-Intel was brought across.** What was left behind is listed below, with the reason. Do not treat
it as a place to go looking — [`docs/`](README.md) plus the Elite-Intel codebase is the source
of truth.
**Target host:** [`SudoKrondor/EliteIntel`](https://github.com/SudoKrondor/EliteIntel) @ `8a4f535` (`v-1.1.0009`, master).

This document records what was carried over, what was changed, what was deliberately left behind, and what
questions the port creates. Nothing here needs to be taken on trust: every claim about Elite-Intel below was
read from that commit's source tree.

---

## 1. What Came Over

44 files. Both feature sets complete, including mockups, punchlists, reference data, and domain knowledge.

| From | To | Treatment |
|---|---|---|
| `02-plugins/bindforge/**` | `02-features/bindforge/**` | All 20 files. Host references adjusted. |
| `02-plugins/starvizion/**` | `02-features/starvizion/**` | All 9 files. Host references adjusted. |
| `00-overview/vision.md` | `00-overview/vision.md` | **Rewritten** — its premise was the shell. |
| `00-overview/glossary.md` | `00-overview/glossary.md` | **Rewritten** — host terms replaced, the third plugin dropped. |
| `00-overview/v1.2-scope.md` | `00-overview/v1.2-scope.md` | Scope sections rewritten; feature lists preserved verbatim. |
| `00-overview/conflicts-and-open-questions.md` | same | **Preserved as historical record**, with a standing-corrections header. |
| `00-overview/testing-required.md` | same | Unchanged but for link retargeting. Still fully valid. |
| `01-core-platform/domain-knowledge/` (2 of 3) | `01-host-integration/domain-knowledge/` | ED path knowledge kept; host-layout sections replaced. |
| `03-data-models/binding-schema.md` | same | Unchanged but for link retargeting. |
| `03-data-models/telemetry.md` | same | **Rewritten** — its premise inverted. See §4. |
| — | `01-host-integration/elite-intel-platform-map.md` | **New.** Replaces all of `01-core-platform/`. |

Both mockups (`BindForge_Mockup.html` 94 KB, `StarVizion_Mockup.html` 73 KB), the Virpil layout PNG, both
`.xlsx` reference tables, `ActionCatalog.json`, and the interactive ConflictMatrix pages all came over intact.

## 2. What Was Left Behind, and Why

| Not ported | Reason |
|---|---|
| All of `01-core-platform/` (8 specs) | Specified a host shell that no longer needs building. Superseded by the platform map. |
| `02-plugins/flightdeck/**` | Not being ported. Elite-Intel already parses journals and tracks commander/session state for its own purposes. |
| `00-overview/scope.md` | Described the StellarCore documentation pass itself, not the product. See §6 for why it is worth reading anyway. |
| `03-data-models/journal-and-commander-data.md`, `ship-type-lookup.md`, `EliteDangerous-ShipTypes.md`, `ShipType_DisplayName_Seed_Data.md` | Data models for the third plugin — see [Other Ideas](04-other-ideas/FlightDeck.md). |
| `01-core-platform/domain-knowledge/FrontierAuthService.md` | CAPI OAuth2 — only that plugin needed it. |

All of it remains in the source archive, untouched, if any of it is ever wanted.

## 3. Structural Changes

- `02-plugins/` → `02-features/`, because these are not plugins any more. Elite-Intel has no plugin framework.
- `01-core-platform/` → `01-host-integration/`, holding one new mapping document plus preserved ED domain
  knowledge.
- **All 251 internal links were rewritten and verified.** Every cross-reference that pointed into
  `01-core-platform/` now resolves to a real section of the platform map.

## 4. Substantive Content Changes

Beyond renaming, four documents changed in **meaning**:

**`telemetry.md` — premise inverted.** Under StellarCore this described a hard problem: two isolated
out-of-process plugins forbidden to talk, needing a sanctioned bridge. Elite-Intel is one in-process Java
application with shared event buses that already parses the journal and `Status.json` into structured session
state. The obstacle is gone. The document was rewritten around what actually remains hard — designing a stable,
named field registry that StarCalc expressions can reference without coupling saved Vizlets to Elite-Intel's
internals.

**`vision.md` — the platform goal was dropped, not deferred.** StellarCore's plugin-ecosystem goal (a
documented contract letting the community extend a shared platform) does not survive. Elite-Intel accepts
contributions to the application, not plugins to a framework. This is a real loss and is stated plainly rather
than quietly omitted.

**`v1.2-scope.md` — "core platform v1" became a gap list.** There is no service set to build. What v1 needs from
the host is now five specific gaps, tabulated.

**Install-locations domain knowledge — host sections replaced.** The Elite Dangerous path knowledge (the
valuable part) is untouched. The two sections describing StellarCore's own folder layout were replaced with
Elite-Intel's `AppPaths` reality.

### Dormant Mode removed (2026-08-31, post-port decision)

BindForge's **Dormant Mode** — a hard write-block for the entire time Elite Dangerous was running — was
removed at Alan's direction. It is gone from the specs, the glossary, the testing backlog and the mockup; the
historical entries in `conflicts-and-open-questions.md` are preserved with a standing correction.

Why it is defensible: the spec itself already recorded that the block window was wider than the risk — the game
only re-reads `.binds` when its in-game Controls menu opens — and flagged narrowing it as a testing-required
item rather than a settled rule.

What replaces it: nothing new. Two existing, independent protections carry the load — a timestamped backup
before every write, and destructive-change detection that catches the game overwriting a player's configuration
whatever the cause.

**Knock-on effect:** Dormant Mode was the only consumer of game-process detection, so removing it cleared what
was the port's single P1 blocker. Elite-Intel still cannot tell whether the game is running, and now nothing
needs it to. StarVizion's mode-based visibility is unaffected — it keys off journal events, not the process.

**Residual risk, recorded honestly:** a write issued at the exact moment the game reads one of these files can
still collide. The window is narrow and unmeasured. If it proves to matter, a warning at the point of writing
is the cheap mitigation — not a session-long block.

## 5. What the Port Gains

The host swap is not purely a subtraction. Elite-Intel already contains, as working code, things the StellarCore
specs listed as work to be done:

- **`elite.intel.devices`** is substantively the Device Service the specs describe — SDL3 at ~60 Hz, axes
  normalised to exactly the [−1.0, +1.0] range StarVizion assumes, VID/PID resolution producing a `bindsHexId`
  that matches `.binds` XML, SDL3↔`.binds` token translation, and duplicate-device detection with USB path.
  *Much of this is your own upstream work* on `V1.1-KAN-57-elite-intel-devices-dawntreader`.
- **`elite.intel.ui.inputmonitor`** — `ReadoutWindow`, `AxesReadout`, `ButtonReadout`, `KeyboardReadout` — is a
  working, small-scale ancestor of StarVizion.
- **`overlay/`** is a native C overlay with **an OpenVR backend already in the tree**, driven over a documented
  stdin protocol. StarVizion's VR requirement has a shipping implementation path, not a green field.
- **`elite.intel.ai.hands`** holds ~20 binding classes including `BindingsWorkingCopyRepository`,
  `BindingsBackupService`, `BindingConflictScanner`, and `BindingsApplyService` — the working-copy, backup, and
  conflict-detection concepts BindForge specifies, already partly built.
- **`AppPaths`** already provisions `getBindingsWorkingDir()` and `getBindingsBackupDir()`.

## 6. A Note Worth Recording

The original `scope.md` explicitly **excluded** a predecessor application from the StellarCore documentation
pass — a voice-companion tool whose bind-editor module had informed BindForge's design, judged out of scope
because its voice-assistant feature set was not StellarCore's concern.

That application was Elite Intel. It is now the host. The exclusion note is worth reading precisely because it
documents what was already known about Elite-Intel's bind editor back when it was treated as prior art rather
than as the destination.

## 7. Open Questions This Port Creates

Ordered by how much they block. These are **new** — questions created by the host change, not inherited. Older
unresolved questions stay in [conflicts-and-open-questions.md](00-overview/conflicts-and-open-questions.md).

### P1 — Blocking

1. **What does BindForge borrow from `elite.intel.ai.hands`, and who owns the write path?**

   **Settled (2026-09-08, replacing the 2026-09-01 answer):** BindForge **is** the existing bind editor,
   upgraded in place. Nothing is built separately and nothing is retired, so the question is no longer
   "what does BindForge borrow" — the pipeline below is already BindForge's. See
   [Phased rollout](00-overview/v1.2-scope.md#phased-rollout--revised-2026-09-08).

   *The earlier answer had BindForge as its own section borrowing selected classes. It was overtaken by
   how much Krondor built into the Bindings section through September; the inventory below survived the
   change intact, because reading the package is what produced it.*

   **Answered 2026-09-06 after reading the package.** `ai.hands` divides into a pipeline BindForge reuses
   whole and two doors it cannot fit through.

   **Reused as-is:** `BindingsApplyService` (validate → backup → atomic write; **this is who owns the write
   path**), `BindingsWorkingCopyRepository` (drafts, BOM-preserving import, baseline fingerprints),
   `BindingsLoader` (which `.binds` is live — not re-derivable from the directory alone),
   `BindingsBackupService`, `BindingConflictScanner`/`BindingConflictRules`, `UiNavigationTextTrap` plus
   `BindingsMonitor.textTrappedUiNavigation()`, and `Bindings.GameCommand` for the full control-set naming.

   **Two deliberate restrictions, and they do not get the same answer** (settled 2026-09-12; the
   2026-09-06 reading had both staying narrow and BindForge building its own wider writer beside them,
   which upgrading in place makes impossible).

   `KeyBindingsParser` **stays as narrow as it is** — a read-only, keyboard-only boundary feeding command
   execution. BindForge reads `.binds` for editing through its own full-fidelity path instead.

   `BindingsWriter` **widens to every device.** Its refusal — *"The commander bound that device in the
   game and this application does not take it away"* — is right for an assistant acting on its own
   initiative and stays right there, but it does not describe a commander deliberately editing their own
   bindings, which is what a bind editor is for. The boundary becomes **who initiated the write**, not
   which device is in the slot. Widening the writer does not widen the parser, so non-keyboard
   assignments still cannot reach command execution. See
   [Two narrow boundaries](02-features/bindforge/overview.md#two-narrow-boundaries--one-stays-one-widens).

   **Extend rather than reuse:** `PlayerBackupService` is the nearest thing to File Manager — user-facing
   snapshots of every `.binds` plus `StartPreset.*.start` — but covers neither `DeviceMappings.xml` nor
   `.buttonMap`, has no ZIP, and has no per-installation notion.

   The `BindingsUpdatedEvent` obligation stated here previously was **wrong and has been corrected** — see
   [BindForge must not publish `BindingsUpdatedEvent`](02-features/bindforge/overview.md#bindforge-must-not-publish-bindingsupdatedevent).

### P2 — Shapes major design

3. **How does StarVizion render?** Three viable paths: the existing native C overlay (which already has OpenVR
   and a stdin protocol), Swing windows like `ReadoutWindow`, or both. This is StarVizion's biggest
   architectural decision and it did not exist under StellarCore, where the shell had no renderer.

4. **~~Keyboard and mouse have no input source.~~ Half resolved 2026-09-07.** `DeviceService` covers
   joysticks, HOTAS, gamepads and pedals only — but **BindForge's keyboard capture already exists** as
   `util.KeyCaptureMapper`, and **mouse does not need capture at all** (bindable mouse inputs are chosen
   from a fixed list). What is still missing is **focus-independent** capture, which only StarVizion needs:
   a HUD showing keys pressed while the *game* has focus, rather than a dialog listening while it has focus
   itself. `KeyboardReadout` is the place to look first, and the ban on JNI to unsigned libraries still
   constrains the options.

5. **Push events versus the snapshot the specs assume.** The specs say StarVizion "just reads the current
   value" at frame rate. `DeviceService` pushes deltas and keeps no queryable snapshot. StarVizion must
   maintain its own cache. Small work, but the spec wording is now inaccurate in several places.

### P3 — Needs an answer before the relevant feature ships

6. **~~Hat switches are undocumented in `DeviceService`.~~ ANSWERED 2026-09-06 from real HID report
   descriptors.** Two DualShock 4 controllers were read with USB Device Tree Viewer. **The hardware does not
   report booleans.** A hat is one 4-bit field, logical `0–7` mapping to 0°–315°, with a null state for
   centred — nine states in one value, not four flags.

   So the boolean-per-direction model is a **translation**, and the specs' phrase *"regardless of how the
   hardware reports them"* is load-bearing rather than throwaway. Elite agrees: `.binds` names exactly four
   POV tokens per hat (`Joy_POV1Up/Down/Left/Right`) and **no diagonals**, so a hat reading `NE` becomes
   `Up` *and* `Right` held together. See
   [§5.4 of the binds format reference](02-features/bindforge/domain-knowledge/EliteDangerous-BindsFileFormat.md#54-pov--hat-codes).

   `PACKAGE.md` still says nothing about hats, so how `DeviceService` surfaces them is unverified in code.
   A HOTAS hat may be 4-way rather than 8-way; the model holds either way.

7. **`.binds` axis tokens stop at index 5.** `ButtonInputMapper.axisToBindsToken` throws for index ≥ 6. Devices
   with more than six axes have no mapping. Affects BindForge directly. *Datapoint 2026-09-06: a DualShock 4
   reports exactly six axes (X, Y, Z, Rx, Ry, Rz), sitting precisely on the boundary — the limit is not
   generous, it is exactly one common gamepad.*

8. **No status-badge affordance exists.** BindForge raises an Error badge on destructive-change detection.
   Elite-Intel's tabbed UI has nowhere to put it. Needs either new UI work or a different signal.

9. **Settings are not isolated.** Everything shares one SQLite store. BindForge and StarVizion settings need a
   key-namespacing convention.

10. **Archive backup/restore (ZIP) does not exist.** BindForge's File Manager is built on it.

11. **`DeviceMappings.xml` and `.buttonMap` handling appears entirely absent.** Two of BindForge's four managed
    file domains are greenfield, as is storefront install discovery, which they depend on.

### P4 — Cosmetic, decide when convenient

12. **Mockup visual language.** See below.

## 8. The Mockups

Both mockups are **host-agnostic** — zero references to StellarCore, plugins, or shell chrome. They mock the
feature UI only, so nothing structural needed changing. They came over byte-identical and render correctly.

**A shell mockup was added:** `01-host-integration/mockups/EliteIntel_Shell_Mockup.html`, a static reproduction
of the running Elite-Intel window. `BINDFORGE` replaces the app's current `BINDINGS` tab; `STARVIZION` is an
added tab. Everything else on it is paint.

The `VEGA` / `BINDFORGE` / `STARVIZION` tabs **switch the content area in place** — the feature mockups render
inside the shell chrome via lazy-loaded iframes, as they would in the real app, rather than opening a new page.
Confirmed working from `file://` in Edge. A fallback notice with a direct link appears if a browser refuses to
embed one local file inside another (Firefox restricts this across sibling directories; Chromium does not).

### Palette — resolved

Both mockups were re-tinted to Elite-Intel's palette on 2026-08-30. The mapping was **role-preserving**: each
colour kept its job and only shifted hue family, so contrast relationships are unchanged.

| Role | Was (StellarCore) | Now (Elite-Intel) |
|---|---|---|
| Primary accent, text + fills | `#d9791f` | `#FF7100` |
| Accent, borders only | `#d9791f` | `#B85A14` |
| Secondary orange | `#e8952f` | `#FF822E` |
| Amber / warning | `#e8c24a` | `#D39207` |
| Success green | `#4fd17a` | `#4FC56B` |
| Error red | `#e0524a` | `#D94F4F` |
| Secondary accent | `#5b9bd5`, `#3ba7ff` (blue) | `#33D7E8` (cyan) |
| Violet | `#b28ae0` | `#B78CD9` |
| Surfaces | `#0a0a0a`, `#1a1a1a`, `#2a2a2a` (neutral grey) | `#090D12`, `#151B22`, `#24313A` (blue-tinted) |
| Body / dim text | `#8a8f96`, `#6a6f76` | `#8FA0AD`, `#6F7D89` |

The accent split is the one judgement call worth knowing about: the mockups used a single orange for text,
fills, **and** borders, where Elite-Intel uses a brighter orange for text and a calmer one for borders. Borders
were therefore mapped to `#B85A14` and everything else to `#FF7100`.

474 colour values were remapped across the two files (311 + 163), plus 21 border declarations and 4 `rgba()`
values. No unmapped colours remain in either file.

**Active tabs** were then set to match the real app: amber `#D39207` fill with dark `#090D12` text, applied to
`.ei-toptab.active` and `.ei-subtab.active` in BindForge. StarVizion's tabs use an underline treatment with no
fill, so nothing there needed changing. The shell's own amber was snapped from an invented `#d9a01f` to the
palette value `#D39207` so all three files agree exactly.

### Nesting and layout tweaks

**Padding.** Standalone, each mockup insets its content by 42px (24px on `body` + 18px on `.ei-frame` /
`.sv-frame`). Inside the shell that sat on top of the shell's own content area and read as a double border.
Both mockups now detect embedding (`window.self !== window.top`, set as `html.embedded` before first paint) and
collapse to a single 14px inset with the body background matched to the shell. Standalone viewing is unchanged.

**HoloFrame cards (StarVizion).** Member Vizlet names used to sit below the poster area, stacking the card
taller than it needed to be. The `.sv-memberchips` block now renders *inside* `.sv-thumb`, under the
"N Vizlets" count, with side padding that keeps it clear of the corner buttons. The chip fill was darkened to
`#101721` so it stays visible against the thumb's `#151B22`, and chip labels no longer break mid-name. Applied
to both seeded cards and to the `svAddHoloFrame()` template, so new HoloFrames get the same layout.

The "N Vizlets" count above the chips was then removed — the chips already show what is in the HoloFrame, so
the count only restated it. It still renders as a **"Drag Vizlets here" hint when a HoloFrame is empty**, since
an empty poster otherwise gives no clue that Vizlets are added by dragging. The poster's `min-height` dropped
from 90px to 76px now that the count line is gone. Net effect is a noticeably shorter card, as intended.

### Alias Designer device selection (BindForge)

The **My Devices** list had DELETE buttons but no way to select a row and edit it, so the Device Editor below
was permanently pinned to hardcoded RHVCAP values. **This was never wired** — checked against the untouched
archive original, which has `fmSelect`, `geSelectRow`, `pmSelectSource` and `fhSelect` but nothing for these
rows. It is not a regression from the port.

Now implemented, taking both of the suggested behaviours rather than choosing between them:

- **The first device is selected on arrival**, and again whenever the Alias Designer or My Devices tab is
  re-entered, so the editor is never pointlessly blank.
- **The editor greys out only when the list is genuinely empty** — all devices deleted — with a short
  "No device selected" message. Its fields are cleared at the same time, so a deleted device's values are not
  left sitting behind the grey-out looking like live data.

Clicking a row selects it (orange left bar plus a raised background) and repopulates the alias, VID/PID,
connected-controller binding, axis names, button names, and the AXIS/BUTTONS counts. The two seeded devices now
differ meaningfully: RHVCAP has 32 buttons and named axes, Throttle has 24 buttons and almost no saved names,
which is what its `buttonMap missing` badge implies. DELETE stops propagation so it no longer doubles as a
select, updates the My Devices count, and falls through to the next device when the selected one is removed.

**Reverting** is straightforward if any of this is wrong: the untouched originals are in the source archive at
`02-plugins/{bindforge,starvizion}/mockups/`.

Separately, some mockup content still assumes host features that do not exist (nav badges, a relocatable data
root). Those are content adjustments to make alongside whichever spec changes follow from §7.

## 9. Verification Performed

- All 44 files copied and accounted for.
- 251 internal links machine-checked: file existence and heading anchors, GitHub slug rules.
- Four broken anchors found and fixed. **These were pre-existing in the source archive** — headings had been
  renamed without updating links; the port did not introduce them.
- Three references to deliberately-unported files neutralised rather than left dangling.
- File scanned clean for stray control characters.
- Every Elite-Intel class, package, and path named in the platform map was read from the tree at `8a4f535`.
