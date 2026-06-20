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

## C. Settled — Conflict Detection Assessment

- Real logic exists (`BindingsMonitor.checkForConflictsAndPersist()` + `binding_conflicts` table), not naive same-key matching. Genuinely modifier-aware; correctly suppresses cross-vehicle-state (Ship/SRV/On Foot) false positives in most cases.
- **Most significant gap (FN-1):** only compares each action's *Primary* slot (Primary wins over Secondary in `parseBindings()`). A real collision between one action's Secondary and another's Primary is never detected, even though both fire in-game.
- `Hold` vs. tap is not factored into combo identity → false positives (FP-1) for legitimately distinct held/tapped bindings on the same key.
- `ToggleOn` semantics (fire-on-release vs. fire-on-press) ignored entirely → can't reason about same-key ToggleOn/normal pairs (FN-4).
- HOTAS-as-modifier slots excluded from conflict map entirely (`keyboardUsable` filter) → real conflicts involving e.g. `AutoBreakBuggyButton`-style joystick modifiers invisible (FN-6).
- Vehicle-state suppression (`isSubStateModeAction()`) over-suppresses some cases — e.g. `ExplorationFSSEnter` vs. `DeployHardpointToggle` sharing a key is wrongly deemed always-safe (FP-3).
- No dedicated conflict UI exists at all today — surfaces only as a spoken count + log entry. No row highlighting, no inspect/fix-from-here flow.
- Verdict: **extend, not replace** — DB schema + live-diff approach are sound; detection algorithm needs upgrading to operate over full slot sets (Primary + Secondary both sides), fold in `Hold`, narrow the suppression list. UI surface is greenfield.

## D. Settled — `.binds` File Discovery & Lifecycle (informed by cross-referencing EDDiscovery/EliteChroma community tools)

- **The `.#.0` version suffix on a `.binds`/`StartPreset.#.start` filename is assigned by the
  game itself, tied to the game's own version** (e.g. Odyssey's on-foot component bump produced
  `.4.0.`) — **BindForge never mints this number**. It only ever reacts to whatever suffix the
  game already wrote; there is no "create a new preset at version X" code path that originates a
  version number independently.
- **Multiple versioned `.binds` files for the same preset can coexist** in the user's Bindings
  folder (e.g. `DawnTreader.1.0.binds` and `DawnTreader.2.0.binds` side by side, left over from
  before/after a game update). The candidate file is selected by combining both signals — highest
  `.#.0` version suffix **and** most recent modification date — and **the higher version number
  wins by default** when the two signals agree or only weakly disagree. If the two signals
  genuinely conflict (e.g. the higher-version file is stale-dated while a lower-version file was
  touched more recently by something else), **stop and ask the user**, with the UI nudging them
  toward the higher-version candidate as the recommended choice rather than presenting a neutral
  pick. (Community tools split on this: EDDiscovery's `BindingsFile.cs` uses mtime only,
  EliteChroma's `EliteFiles` library uses version-number only; neither alone is trusted here.)
- **First-run / never-customized case:** if the user has never created a custom `.binds` file
  (the file `StartPreset.4.start` points to doesn't exist yet, or the Bindings folder has no
  matching custom file), BindForge must read the game's stock/default preset (resolved via the
  install folder's `ControlSchemes` directory — see open item E.1's sibling problem for
  `DeviceMappings`/`buttonMap`, since `.binds` resolution turns out to have the same two-location
  shape), copy it into the user's actual binds folder, prompt the user to name the new custom
  file, and write `StartPreset.4.start` so the game recognizes it as the active preset. This is
  an onboarding flow, not a silent background file copy.
- **Mandatory first-load backup, non-negotiable:** the very first time Elite Intel runs, before
  BindForge touches anything, it must copy **every** `.binds` file, every `StartPreset.*.start`
  file, and every `.buttonMap` file found in the user's bindings location(s) — regardless of how
  many or how old — into a backup folder inside Elite Intel's own data folder. This applies even
  if there are dozens of stale files from years of game updates; none are skipped or judged
  irrelevant. User data safety takes priority over tidiness here — this backup step is separate
  from (and a prerequisite to) the "ask the user about cleaning up old versions" prompt above.

## E. Open — Not Yet Decided

1. **Discovery mechanism** for multiple Elite Dangerous installations (registry / launcher config / manual user pointing?) — needed before DeviceMappings/buttonMap multi-location save logic can be designed, and before the mandatory first-load backup (Section D) can know everywhere it needs to copy from. (Note: the `.binds` stock-vs-custom two-location resolution problem in Section D's first-run case is the sibling of this problem, now partially understood — but multi-*installation* discovery, e.g. Steam + Epic side by side, is still open.)
2. **Internal model shape for DeviceMappings + buttonMap**: one merged "device + its button labels" concept, or two separate structures that each get multi-location saves?
3. Whether to loop in **Krondor/Gnevko** before extending the parser/conflict logic — given Gnevko's very recent direct work in this exact area (`b4225482`, `a8b4b73e`) and Krondor's concurrent brain-package refactor. Also worth surfacing the modifier/Hold/non-keyboard-modifier complexity to them directly, since the existing capture UI was likely built without that knowledge.
4. Whether the keybind-**capture dialog** (for AXIS — if it even needs live capture vs. numeric/dropdown editing — and STANDALONE) should reuse Krondor's mandated `KeyCaptureMapper`/native-input-class pattern identically to BUTTON, or needs its own variant.
5. Actual **data model class design** (Game Mode → SubGroup → Element → properties) — schema and existing-code landscape are now understood; the Java-side class shapes themselves haven't been drafted yet.
6. Whether Purpose Mode's binding-reference key should be the raw XML element name (durable but at minor risk if Frontier renames elements) or a synthetic ID — implied-but-not-explicitly-confirmed decision from the Purpose Mode spec.

---

*Last updated from planning session — fold in future findings as they're settled.*
