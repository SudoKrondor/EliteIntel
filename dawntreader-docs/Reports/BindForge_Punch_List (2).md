# BindForge — Running Punch List

**Status:** Planning consolidation. Not a spec — a tracking list of decided items and open items to work through before/during data model design.

---

## A. Settled — `.binds` Schema (fully evidence-backed)

- Three element types: AXIS (`<Binding>` + optional `Inverted`/`Deadzone`), BUTTON (`<Primary>`/`<Secondary>` + optional element-wide `<ToggleOn>`), STANDALONE SETTING (bare `Value=`, no slots).
- Slot-level properties apply to any `Primary`/`Secondary`/`Binding` slot:
  - `Modifier`: zero to **three** per slot (hard cap — 4th overflows into becoming the base key). Any device type (not keyboard-only — confirmed via `AutoBreakBuggyButton`/RVWAP joystick-as-modifier and the Virpil slider end-of-travel button quirk). Left/Right tracked as distinct values, freely mixable. Same key can't be both base and modifier on one slot.
  - `Hold`: per-slot, boolean-ish (`Value="1"` observed), origin is *how the key was captured* (held ~1+ sec during detection vs. tapped), not a user-chosen setting. Never appears on AXIS. Independent of and can coexist with `ToggleOn` on the same element (confirmed via `ToggleCargoScoop`).
  - `ToggleOn`: element-level (covers both Primary and Secondary as one shared behavior, not per-slot). Only two observed values (0/1). BUTTON-only, never AXIS.
  - `Deadzone`: AXIS-only, range 0–1, never on BUTTON.
  - `Inverted`: AXIS-only.
- Edge cases confirmed in full-file extraction (`AUDIT_v2_FULL.md`): duplicate element names possible (`MouseGUI` ×2); half-axis/mouse-wheel key strings (`Neg_Joy_YAxis`, `Pos_Mouse_ZAxis`) can appear on BUTTON slots, not just AXIS; named devices beyond RVWAP/Keyboard/Mouse exist (T-Rudder, LVWAP, vJoy) — device field must accept arbitrary names; one non-conforming node (`KeyboardLayout`, text-content not attribute) needs its own handling path; element *names* are not reliable type indicators (`*ButtonPartial` elements classify as AXIS).
- Source of truth = the `.binds` XML schema itself, not any single file's populated/unpopulated state. Audit files are schema specimens, not personal-config trackers.
- `.binds` has exactly **one** canonical file location system-wide (`%LOCALAPPDATA%\Frontier Developments\Elite Dangerous\Options\Bindings\`) — shared across all storefront installs, no multiplicity.
- `DeviceMappings.xml` + `.buttonMap` files are cosmetic-only (button labeling), **not** game config — but are duplicated per-installation (one full copy under each storefront's own `ControlSchemes` folder). Multiplicity confirmed real (Steam + Epic side by side).

## B. Settled — Existing Codebase Assessment

- `KeyBindingsParser` / writer / working-copy / apply pipeline: production-quality for **BUTTON only**. AXIS (72 elements) and STANDALONE SETTING (117 elements) are completely unhandled — not buggy, never written. Verdict: **extend, not replace**.
- No database table stores parsed `.binds` content today. Pattern is parse-live → in-memory model → working-copy (with SHA-256 baseline hash, hardened in `b4225482`) → apply-back-to-file.
- `b4225482` added conflict-aware apply safety (detects concurrent game-client edits via baseline hash) — independent of and complementary to the `binding_conflicts` same-key detection system (Section C).
- `BindForgeTabPanel` (renamed from `BindingsTabPanel` in `a8b4b73e`) is now a top-level tab. Purely structural promotion — no new binding capability added. Still keyboard-only editor with Used/Missing tables.
- Decision: AXIS/STANDALONE extension should follow the **same non-database, parse+working-copy pattern** as BUTTON, for consistency — not a new persisted table for binding *content*.
- DeviceMappings/buttonMap editing should follow the same general pattern (in-memory model, transparent to user) but with a **fan-out write step** across N installation locations, since BUTTON-style single-location apply doesn't fit their multiplicity.
- **Settled — Purpose Mode binding-reference key (was open item D.4):** Use the raw XML element name directly as the stable identity reference for Purpose Mode's binding-membership records. No synthetic ID needed. Rationale: the original synthetic-ID instinct came from database normalization concerns (stable foreign keys), but since binding content is not persisted to a database table (parse-live/in-memory/working-copy pattern), there is no foreign-key relationship to protect. The XML element name is already the game's own identity for each binding — used by the parser, the writer, and the game itself — so it is the correct and natural reference key. A synthetic ID would provide no protection against Frontier renaming elements in a future game update (that would break either approach equally), while adding unnecessary indirection. Live capture is proven working in this codebase via two existing implementations — SDL3 (StarVizion prototype branch `V1.1-starvizion-prototype-dawntreader`, proved live button-press AND axis-movement detection cross-platform on Windows and Linux, known Linux refresh issue but fundamentally solved) and `KeyCaptureMapper`/native input classes (`WindowsNativeKeyInput`/`LinuxX11NativeKeyInput`, Krondor's mandated keyboard-capture path, reference-only — do not modify). BindForge reuses these patterns without copying code from StarVizion (StarVizion must never be included in any PR). Capture mechanism per element type: BUTTON binding slots → live keypress detection via `KeyCaptureMapper`/native input classes; AXIS binding slots → live axis-movement detection via SDL3; STANDALONE SETTING → no live capture, direct value UI (toggle/slider/dropdown matching game's own presentation style for each value type); DeviceMappings/buttonMap label editing → no capture at all, text/icon-picker in a visually immersive controller-diagram UI. The existing PTT Settings panel (Controller + Button dropdowns, SDL3-backed) is an already-shipped example of SDL3 device enumeration in production, though it uses dropdown selection rather than live capture — confirming SDL3 is fully integrated and functioning in the codebase today. one loaded model composed of **three separate in-memory collections** (plain Java object composition — NOT database tables, no DB involved here per the no-persistence decision above): the parsed `.binds` elements (full AXIS/BUTTON/STANDALONE schema per `AUDIT_v2_FULL.md`), a `Devices` collection (from `DeviceMappings.xml` — friendly name ↔ VID/PID), and a `ButtonLabels` collection per device (from each `.buttonMap` — raw input ID ↔ friendly label). Each collection stays close to its own source file's shape (for clean round-tripping); the model exposes cross-referencing lookups (e.g. "resolve the friendly label for this binding's device+key") rather than flattening everything into one merged structure. Rationale: displaying a single binding meaningfully to a user requires joining across all three sources, but the *files themselves* are genuinely separate concerns with separate save destinations (`.binds` → one canonical location; `DeviceMappings`/`buttonMap` → fan-out across every discovered install). UI/UX transparency (user never sees "now editing DeviceMappings.xml") is a presentation-layer concern, fully decoupled from this internal structure — e.g. a Virpil-style clickable controller-diagram tab for placing/labeling commands, separate from the existing flat-list `.binds` editor view.

## C. Settled — Conflict Detection Assessment

- Real logic exists (`BindingsMonitor.checkForConflictsAndPersist()` + `binding_conflicts` table), not naive same-key matching. Genuinely modifier-aware; correctly suppresses cross-vehicle-state (Ship/SRV/On Foot) false positives in most cases.
- **Most significant gap (FN-1):** only compares each action's *Primary* slot (Primary wins over Secondary in `parseBindings()`). A real collision between one action's Secondary and another's Primary is never detected, even though both fire in-game.
- `Hold` vs. tap is not factored into combo identity → false positives (FP-1) for legitimately distinct held/tapped bindings on the same key.
- `ToggleOn` semantics (fire-on-release vs. fire-on-press) ignored entirely → can't reason about same-key ToggleOn/normal pairs (FN-4).
- HOTAS-as-modifier slots excluded from conflict map entirely (`keyboardUsable` filter) → real conflicts involving e.g. `AutoBreakBuggyButton`-style joystick modifiers invisible (FN-6).
- Vehicle-state suppression (`isSubStateModeAction()`) over-suppresses some cases — e.g. `ExplorationFSSEnter` vs. `DeployHardpointToggle` sharing a key is wrongly deemed always-safe (FP-3).
- No dedicated conflict UI exists at all today — surfaces only as a spoken count + log entry. No row highlighting, no inspect/fix-from-here flow.
- Verdict: **extend, not replace** — DB schema + live-diff approach are sound; detection algorithm needs upgrading to operate over full slot sets (Primary + Secondary both sides), fold in `Hold`, narrow the suppression list. UI surface is greenfield.

## D. Open — Not Yet Decided

0. **Cross-mode conflict classification map** (not critical — deferred research, separate test install planned). The `.binds` schema tells us *what fields exist*, but not *which same-physical-input pairings are actually safe vs. actually dangerous in real gameplay*. Web research (Frontier/Steam community threads) confirms three real categories exist: (a) explicitly mode-gated reuse that's safe by design — Alternate Flight Controls and Landing-mode overrides are confirmed to be mutually-exclusive context switches, not simultaneous, so reusing the same axis/button across them is intentional, not a conflict; (b) genuinely simultaneous collisions the game does NOT warn about — e.g. one community example of SRV jump-jet "up" sharing a control with turret pitch, causing both to fire at once while driving with turret deployed; (c) presumably-safe default cases (unrelated actions, unrelated contexts). No one — not the game, not the community — appears to have a complete map of which Game Mode / SubGroup pairings fall into which bucket; this would have to be built through deliberate, systematic in-game testing. **Plan: set up a separate Elite Dangerous installation/profile specifically for this testing**, to avoid corrupting the real play profile with deliberately conflicting test binds. Lower priority than the core data model work — log and revisit later.

1. **Discovery mechanism** for multiple Elite Dangerous installations (registry / launcher config / manual user pointing?) — needed before DeviceMappings/buttonMap multi-location save logic can be designed.

   **Investigation complete (web research + existing-codebase audit, see `INSTALLATION_DISCOVERY_RESEARCH.md`):**
   - Web research confirmed: on Windows, `.binds`/`ControlSchemes` live at `%LOCALAPPDATA%\Frontier_Developments\Products\<product-name>\ControlSchemes\`, a path keyed by product name, not by storefront — Steam and Frontier-launcher installs of the same product share this location (Epic may differ; unverified, treat as caveat). Steam installs are enumerable via `libraryfolders.vdf` (check `apps` map for AppID `359320`); Epic installs are enumerable via `.item` JSON manifests under `ProgramData\Epic\EpicGamesLauncher\Data\Manifests\`. On Linux, bindings live inside a Steam-managed Proton `compatdata` folder keyed by AppID — Steam VDF parsing is *required*, not optional, to find it.
   - **Existing EliteIntel code does NO automatic discovery at all.** `PlayerSession.getBindingsDir()` (the actual resolution method, used by `BindingsLoader`, `BindingsMonitor`, `BindingsTabPanel`, etc. — all rooted in this one method) is a DB-column override (`player.bindings_dir`, populated only by a manual `JFileChooser` picker in `BindingsTabPanel`) falling back to a **hardcoded literal-string guess per OS** if unset. No registry, VDF, or manifest reading exists anywhere in the codebase.
   - **Two real bugs found as a byproduct, unrelated to BindForge scope but worth fixing:** (a) the Linux default path (`~/.var/app/elite.intel.app/ed-bindings`) is fictional — doesn't match the real Proton/`compatdata` location at all; (b) the "Mac" branch in `getBindingsDir()` is dead code, since `OsDetector.getOs()` only ever returns WINDOWS or LINUX, so any non-Linux OS (including real macOS) silently falls through to the Windows guess. **Action: file these as a small, separate bug-fix item — not BindForge work, but worth a quick fix since it was found in passing.**
   - `ControlSchemes`/`DeviceMappings.xml`/`.buttonMap`/`DeviceButtonMaps` are referenced **nowhere** in app code (confirmed via repo-wide search) — this is genuine greenfield, no existing logic to extend or conflict with.
   - `PlayerSession.getJournalPath()` has the exact same structural pattern/flaw (DB override + hardcoded guess, no auto-detection) — worth keeping in mind as a parallel fix opportunity if the bindings-dir discovery logic gets improved, since the same improvement would likely apply there too.
   - **Remaining decision:** given automatic detection doesn't exist today and the current manual-picker-with-guessed-default pattern technically "works," does BindForge invest in building real automatic detection (Frontier `Products\*\ControlSchemes\` enumeration as primary, Steam/Epic as confirmatory, per the web research) for both `.binds`-dir resolution *and* the new `DeviceMappings`/`buttonMap` multi-install fan-out, or keep the existing manual-picker pattern and just extend it to also ask for/remember `ControlSchemes` location(s)?

2. **(Resolved — 2026-06-18)** Krondor and Gnevko looped in via dev channel. Krondor gave architecture sign-off and flagged three requirements (see Section F). Gnevko response pending. Modifier/Hold/non-keyboard-modifier schema complexity not yet explicitly surfaced to Krondor — still worth sharing the audit docs as offered in the proposal.
3. Actual **data model class design** (Game Mode → SubGroup → Element → properties) — schema and existing-code landscape are now understood; the Java-side class shapes themselves haven't been drafted yet. This is the last remaining open item before implementation planning can begin.

## E. Incidental Bugs Found (Not BindForge Scope — Separate Fixes)

These were discovered as byproducts of BindForge investigation but are pre-existing, unrelated bugs worth fixing on their own, independent of any BindForge work:

1. **`PlayerSession.getBindingsDir()` Linux default path is wrong.** Falls back to `~/.var/app/elite.intel.app/ed-bindings` when `player.bindings_dir` is unset — this is not a real Elite Dangerous path (looks like a leftover/placeholder Flatpak-style path for EliteIntel itself), and doesn't match the real Proton `compatdata` location identified in installation-discovery research.
2. **`OsDetector.getOs()` has a dead "Mac" code path.** `getBindingsDir()` contains a `MAC` branch with its own hardcoded default, but `OsDetector.getOs()` only ever returns `WINDOWS` or `LINUX` — any non-Linux OS, including real macOS, silently falls through to the Windows default. The Mac branch can never execute as written.
3. **`PlayerSession.getJournalPath()` shares the identical structural pattern/flaw** as `getBindingsDir()` (DB-column override + hardcoded per-OS guess, zero auto-detection). Not necessarily wrong today, but worth revisiting in tandem if/when bindings-dir discovery logic is improved, since the same fix would likely apply.

## F. Team Feedback — Krondor (2026-06-18)

Krondor responded to the BindForge proposal in the dev channel. Key points confirmed:

- **Architecture validated:** "The EI integration with the game - that pipeline is agnostic to what is editing the binds, or what the binds are. It just pushes buttons. Which buttons? The ones we read from whatever the current bindings file is. What edited it and how - that pipeline does not care." — This is explicit confirmation that the clean-separation architecture (BindForge owns all editing; command pipeline just reads the file) is correct and has Krondor's sign-off.
- **Overall direction approved:** "I think a good and comprehensive editor for the game controls is a great addition to the application."
- **LWJGL/reverse-pipeline observation:** Krondor noticed the `org.lwjgl` library added for PTT and speculated whether the app could push a button on a controller (reverse direction). Not relevant to BindForge now — filed as a future research item ("test and research required" per Krondor).

**Three requirements Krondor flagged:**

### F.1 — Auto-generate missing bindings (REQUIRED)
BindForge needs a way to automatically generate a working set of missing bindings for users who don't have all the keyboard keys set that Elite Intel needs for voice commands. Users can edit these after the fact. This replaces/supersedes the current "Missing Bindings" tab's diagnostic-only approach with an actual fix-it capability.

### F.2 — Safe key list for auto-generation (REQUIRED)
Auto-generated bindings must only use keys confirmed safe across QWERTY, QWERTZ, and AZERTY keyboard layouts. Krondor's confirmed safe list:
- **Safe letters (same physical position and label across all three layouts):** E, R, T, U, I, O, P, S, D, F, G, H, J, K, L, B, N (18 keys)
- **Safe modifiers:** LeftCtrl, LeftShift, RightShift, LeftAlt
- **Avoid for auto-generation:** Q, W, A, Z, M, Y (context-dependent), and all punctuation keys
- This gives 18 base keys × combinations of up to 3 modifiers from 4 options — sufficient combinatorial space for a full auto-generated default set.
- For user-entered bindings via live capture (`KeyCaptureMapper`/SDL3): no restriction, but BindForge should warn if a captured key falls outside the safe list on a non-QWERTY system.
- **Decision needed:** apply safe-key constraint universally for auto-generation (simplest, protects all layouts) or detect keyboard layout first and apply selectively? Given cross-platform Java keyboard layout detection complexity, universal application is likely the pragmatic call.

### F.3 — AZERTY scan code bug (GAME BUG — requires workaround in BindForge)
Confirmed game bug (Frontier's, not ours) affecting French OS/keyboard/game setups:
- **The bug:** Game records binding as `Key_M` (scan code `0x32`). On French AZERTY, the physical key at the `M` position has scan code `0x32`, but the French OS maps that position to `,` (comma, scan code `0x33`). Elite Intel correctly sends `0x32` for `Key_M`, but the game is listening for `0x33` — mismatch, binding doesn't fire.
- **Root cause:** Frontier's input system mixes key names (what gets stored in `.binds`) and scan codes (what gets checked at runtime) inconsistently across layouts.
- **BindForge workaround:** The safe key list (F.2) is the direct technical response — keys on that list have consistent physical position AND scan code AND key name across all three layouts, so the mismatch cannot occur for those keys. Auto-generation must use only safe keys. Live capture via `KeyCaptureMapper`/native input classes handles this correctly for user-entered bindings (reads actual physical scan code). No further workaround available beyond these two mitigations — this is ultimately Frontier's bug to fix.

### F.4 — KeyboardLayout-aware scan code resolution (Totem, 2026-06-18)

A proper fix for the AZERTY scan code bug (F.3) has been proposed by Totem (not yet implemented/tested):

**The fix:** Read `<KeyboardLayout>` from the `.binds` file and carry it with every binding. On Windows, layout-sensitive keys are resolved through that specific installed layout and injected as physical scan codes rather than logical key names. Example: `Key_M` with `fr-FR` resolves to scan code `0x27` (the AZERTY M position) rather than the QWERTY M position. If the declared layout cannot be resolved safely, the key is skipped rather than sending an incorrect one. This avoids Java Robot, QWERTY assumptions, and dependence on the currently active OS layout (which is unreliable since the layout active *now* may differ from the layout used when the `.binds` file was created).

**Impact on BindForge:**
- `<KeyboardLayout>` is no longer just an edge-case node requiring special handling — it is a **first-class piece of data** that BindForge's model must read, preserve, and write back correctly on every save. Stripping or ignoring it would break Totem's fix.
- This supersedes the "AUDIT_v2_FULL.md edge case — one non-conforming text-content node" note from Section A. `<KeyboardLayout>` needs explicit model support, not just graceful handling.
- The safe-key-list constraint for auto-generation (F.2) remains the conservative fallback until Totem's fix is confirmed implemented and working. Once it is, auto-generation could theoretically use any key safely since the resolution layer handles scan codes correctly — but do not relax the safe-key constraint until that's confirmed.
---

*Last updated from planning session — fold in future findings as they're settled.*
