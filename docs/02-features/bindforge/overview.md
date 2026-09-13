# BindForge — Overview

**Elite-Intel feature:** BindForge
**Function:** A game control configuration editor for viewing and editing a player's Elite Dangerous bindings outside the game — `.binds` files, `DeviceMappings.xml`, and `.buttonMap` files.

---

## Purpose

BindForge exists to protect Elite Dangerous control configuration from loss and to let the player view and edit that configuration without leaving the game's own file conventions. It is the most design-complete of the two features in this documentation set.

### Scope: every control, every input device

**BindForge covers the whole of Elite's control set and every device that can drive it — keyboard, mouse, joystick, HOTAS, gamepad, pedals, and any combination of them.** It is not a controller-only tool. Where this documentation says "controller", read it as shorthand for *whatever device holds the binding*, never as an exclusion of keyboard and mouse.

This matters more than it looks, because Elite-Intel's existing binding code is deliberately narrower. `KeyBindingsParser` is a read-only, keyboard-only boundary feeding command execution, and `BindingsWriter` refuses to edit any slot holding a controller assignment. Both are correct for what they protect today, and because **BindForge is that same code grown**, neither can simply be routed around.

They do not get the same answer: the parser stays exactly as narrow as it is, and the writer widens — see [Two narrow boundaries](#two-narrow-boundaries--one-stays-one-widens).

**Priority order, in order:**
1. Back up and restore all managed file domains reliably — see [Managed File Domains](#managed-file-domains) and [File Manager](file-manager.md).
2. View and edit bindings — see [Bind Editor](bind-editor.md).
3. Manage device mappings and button names — see [Alias Designer](alias-designer.md).

BindForge does not interact with the running game process. It only touches files the game itself reads at its own startup or preset-load time — see [Writing While the Game Is Running](#writing-while-the-game-is-running).

BindForge runs on both of Elite-Intel's target platforms. On Linux, there is effectively one installation (via Steam/Proton); on Windows, a player may have several storefront installations simultaneously.

## Managed File Domains

BindForge manages four related but distinct file domains, described fully (with real confirmed paths and format detail) in [BindForge's binds-file domain knowledge](domain-knowledge/EliteDangerous-BindsFileFormat.md) and [device-mapping/button-map domain knowledge](domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md):

| Domain | File(s) | Location | Loss impact |
|---|---|---|---|
| Bind Domain | `.binds` | User configuration folder — one single shared location regardless of storefront | High — a lost or corrupted `.binds` file affects actual gameplay controls |
| Device Identity Domain | `DeviceMappings.xml` | Inside the game **installation** folder — genuinely duplicated per storefront install | **High — reclassified 2026-09-08, see below.** Previously read: cosmetic only — affects button-label display, not gameplay |
| Button Map Domain | `.buttonMap` (one per named device) | Inside the game installation folder, alongside `DeviceMappings.xml` | Cosmetic only |
| Active Preset Domain | `StartPreset.#.start` | Same user configuration folder as `.binds` | Low — governs which `.binds` file is loaded per binding section |

The Active Preset file has four lines, one per binding section, in a fixed order: General, Ship, SRV, On Foot. All four normally point at the same `.binds` filename (the common "single preset" case), but each line is independently settable — a valid, if unusual, "split preset" configuration. See [Preset Editor](preset-editor.md).

### Why `DeviceMappings.xml` is not cosmetic — reclassified 2026-09-08

It was filed as cosmetic because its visible effect is button labels. That is what it *shows*. What it *does* is decouple a commander's bindings from a hardware ID they do not control.

`.binds` names a device by its `DeviceMappings.xml` element name, or by raw VID+PID hex when no entry matches ([§4.0](domain-knowledge/EliteDangerous-BindsFileFormat.md#40-the-rule-confirmed-2026-09-08)). So the entry is an **indirection layer**, and whether one exists decides what happens when the hardware ID changes:

| State | `.binds` holds | A firmware update changes the PID |
|---|---|---|
| **No entry** | `Device="334483F3"` | every binding on that device is orphaned, **silently** |
| **Entry exists** | `Device="RVWAP"` | edit one PID field; every binding keeps working |

**And hardware IDs do change.** See
[VID/PID is not stable](alias-designer.md#vidpid-is-not-a-stable-identity). One commander's two VIRPIL devices carried six different PIDs across three snapshots between 2024-08 and 2026-09, while a Razer device and two Frontier-recognised devices in the same files never moved.

So the loss impact is **high, not cosmetic**: a `DeviceMappings.xml` lost to a game update takes the indirection with it, and the bindings that depended on it revert to depending on an ID the vendor can change. `.buttonMap` remains genuinely cosmetic — it really is only labels.

A confirmed real-world incident — a game update that overwrote a user's `DeviceMappings.xml` — is the direct reason Alias Designer and File Manager were prioritized ahead of the rest of the BindForge suite. Game updates can overwrite or erase these files, but this is confirmed **not** universal or guaranteed — it happens on some updates for some users, not reliably reproducibly — so BindForge must always validate presence/correctness rather than assume either outcome.

**Confirmed by direct testing:** `.buttonMap` files cannot be relocated to the user configuration bindings folder alongside `.binds`, despite documentation suggesting otherwise — removing `DeviceMappings.xml` from the game install folder causes the game to load no bindings at all, not even defaults. The game installation folder is the only valid location for both `DeviceMappings.xml` and `.buttonMap` files.

## Live File Synchronization

Every managed file domain follows the same synchronization model: the live file inside the game's own folders is the source of truth, and BindForge keeps a local working copy in its own user-data folder that mirrors it for editing. This applies uniformly across all four domains — Bind, Device Identity, Button Map, and Active Preset.

### Freshness Checks

Whenever BindForge is opened, or whenever a live file changes while BindForge is running (detected by `ai.hands.BindingsMonitor`, which already watches the bindings directory and is [extended to cover the game installation folders too](#external-change-detection-stays-in-one-place) rather than BindForge adding a second watcher; see [File Access](../../01-host-integration/elite-intel-platform-map.md#file-access)), BindForge compares its local working copy against the live file's current state.

- If the local copy is simply behind — the live file changed in a way that doesn't lose anything the user has customized — BindForge treats the live file as newer truth and offers to refresh the local working copy to match it.
- If the local working copy already has unsaved edits at the moment a live-file change is detected, BindForge does not silently pick a winner. It surfaces the conflict to the user, using the same pattern as [Unsaved Work on Exit](#unsaved-work-on-exit): keep editing the current local draft (ignoring the incoming live change for now), or refresh from the live file (discarding the unsaved local edits).

### Destructive Change Detection

Not every live-file change is legitimate. Elite Dangerous itself has, at least once, overwritten a player's `DeviceMappings.xml` back to factory defaults during a game update — the same real-world incident referenced above — and there is no guarantee a future update couldn't do the same to any of the other three domains.

BindForge does not treat "the live file changed" and "the live file is now correct" as the same thing. Before refreshing its local working copy from a changed live file, BindForge checks whether the change looks like **data loss** rather than a legitimate update — specifically, whether content the working copy (or a recent File Manager backup) already knows about is now missing from the live file: custom device aliases gone from `DeviceMappings.xml`, bindings reverted to defaults, or anything else that would mean a normal freshness refresh would silently make BindForge complicit in overwriting the user's own configuration with a damaged file.

**If a change looks like data loss:** BindForge does not sync its working copy to match. Instead, it raises this as an Error-level condition (Elite-Intel's tabbed UI has no badge affordance yet — some visible equivalent is required, see [Status Badges](../../01-host-integration/elite-intel-platform-map.md#status-badges)) and offers to **restore** the missing configuration back onto the live file immediately — the roles reverse, and the working copy (or, if the working copy itself is somehow unavailable, the most recent File Manager backup) becomes the source used to overwrite the damaged live file, through the same backed-up, atomic write every other BindForge write already uses (see [Data Integrity Principle](#data-integrity-principle)).

This is a general behavior, not specific to Alias Designer — see also [Bind Editor — Shell](bind-editor.md#shell-common-to-all-modes) and [Preset Editor — Sync Badge](preset-editor.md#sync-badge) for how each section surfaces it.

### First-Time Startup

Live File Synchronization assumes a live custom `.binds` file already exists for BindForge to mirror. That assumption doesn't hold for a player who has never customized a binding in-game — Elite Dangerous runs perfectly well on one of its own bundled factory presets (from the game install's `ControlSchemes` folder) indefinitely, and never writes anything to the player's own bindings folder until the player changes a binding in-game for the first time. Left unhandled, BindForge would show an empty or broken state for exactly the players who'd benefit from it most: newcomers who haven't touched their bindings yet.

**Detection:** the Active Preset file (`StartPreset.#.start` — see [Preset Editor](preset-editor.md)) records, independently per section (General/Ship/SRV/On Foot, the same four sections Preset Editor's own dropdowns manage), which preset is currently active for that section — stock or custom, always known, never guessed. BindForge checks this per section on startup.

**If a section is still pointing at a stock, factory preset:** BindForge asks the player whether to create a real custom `.binds` file for that section, seeded from a copy of the *exact* stock preset currently active for it (read from the Active Preset file, not assumed). If the player agrees:
1. BindForge copies that stock preset's content into a new file in the player's own bindings folder.
2. BindForge repoints that section's entry in the Active Preset file to the new custom file, through Preset Editor's own existing write path (Save, backed by the standard timestamped-backup-before-write) — the same mechanism the player would use to manually switch presets themselves.

From that point on, the section behaves exactly like any other managed domain — normal Live File Synchronization takes over.

**If the player declines**, that section is simply left alone. BindForge doesn't force the issue, and re-offers the next time that section is found still on a stock preset.

**Per-section, not all-or-nothing:** because the Active Preset file tracks each of the four sections independently — the same "split preset" configuration Preset Editor already supports — a player can be partway customized (some sections on a real custom file, others still on factory defaults), and this flow only prompts for the sections that actually need it.

#### When it fires — settled 2026-09-12

**When the player first opens BindForge, not when Elite-Intel launches.** The check is cheap and could run at startup, but a newcomer who launched the application to do something else has not asked about bindings, and a prompt about factory presets at that moment is an interruption rather than an offer. Opening the Bindings section is the moment the question is *in context*, and it is also the first moment the answer matters — the section cannot show a real state until it exists.

The four sections are gathered into one prompt rather than asked about one at a time:

```
2 of 4 sections are on factory presets.
Ship and SRV cannot be edited until they
have a custom preset of their own.

  [ Create both ]  [ Choose ]  [ Not now ]
```

**Choose** opens the per-section picker, for a partly-customized player who wants one section and not another.

#### It applies immediately — settled 2026-09-12

**This is the one documented exception to [Apply is the only thing that writes a live game file](#live-file-synchronization).** The click on **Create** is the consent, and setup completes when the player gives it. Leaving a draft here would mean they pressed Create and nothing was created, then asked them to **Apply** a change they cannot yet evaluate, in a product they opened seconds ago.

**Why the exception is safe rather than merely convenient.** The draft model exists to stop BindForge overwriting a commander's work. **This case is defined by there being none** — it runs only where no custom `.binds` exists for that section. Concretely:

| The write | Why it cannot lose anything |
|---|---|
| Create the new `.binds` | **Additive.** The file did not exist; nothing is overwritten. |
| Repoint `StartPreset.#.start` | **One line, reversible.** [Preset Editor](preset-editor.md) switches it back at any time. |
| The factory preset | **Never touched.** `ControlSchemes` is read-only to BindForge; the original remains exactly as shipped. |

So the player is told what happened and how to undo it, rather than being asked to authorize it twice:

```
Created Custom.binds for Ship and SRV,
and set both sections to use them.

Your factory presets are unchanged.
You can switch back any time in Preset Editor.
```

**The exception does not generalize, and its boundary is the point.** It covers *creating* a preset where none existed, at first open, with explicit consent. Every subsequent write to that section — every binding the player then edits — goes through the draft and Apply like everything else. A write that modifies existing player data is never an exception, whatever the flow.

**Testing-required, and now load-bearing:** whether a `ControlSchemes` preset file can be copied byte-for-byte into the player's bindings folder as a valid starting `.binds` file, or whether the game's own first-customization save does something to the file beyond a plain copy, hasn't been confirmed against a real install — see [testing-required.md](../../00-overview/testing-required.md). **Applying immediately raises the stakes on that test:** a bad copy reaches the live bindings folder with no review step in front of it. The timestamped backup still runs, so it is recoverable rather than dangerous — but this should be confirmed before the flow ships, not after.

## Top-Level Structure

BindForge's UI is organized into four top-level sections, in this order:

1. **Alias Designer** — device mapping and button/axis naming
2. **Bind Editor** — viewing and editing bindings, in multiple modes
3. **Preset Editor** — which `.binds` file loads for each binding section
4. **File Manager** — backup and restore of all four managed file domains; BindForge's primary feature

## Data Integrity Principle

BindForge inherits and applies Elite-Intel's project-wide [Data Integrity Principle](../../01-host-integration/elite-intel-platform-map.md#data-integrity-principle): every write either fully succeeds or fully rolls back. Nothing is written to a live game file without a timestamped backup first, using an atomic write-to-temp-then-replace pattern everywhere.

## Reconciling BindForge Edits With In-Game Rebinds

**Settled 2026-09-06.** A player can rebind inside Elite's own controls screen while BindForge is open. Both sides then hold a changed file, and something has to decide what happens. Elite-Intel already implements the mechanism, in `BindingsWorkingCopyRepository`.

Three SHA-256 fingerprints are compared:

- **baseline** - the game file as it stood when the draft was taken, stored beside the draft as
  `<preset>.elite-intel.base.sha256`
- **draft** - the player's copy, carrying BindForge's edits
- **live** - the game's file as it stands right now

| Draft changed | Live changed | Meaning | Behaviour |
|---|---|---|---|
| no | no | idle | nothing |
| **yes** | no | ordinary editing | apply proceeds |
| no | **yes** | player rebound in-game, BindForge untouched | `refreshFromGameIfClean` adopts the change into the draft and re-baselines, silently. Nothing is lost and nothing is asked. |
| **yes** | **yes** | both sides edited | **apply refuses**, via `BindingsApplyService.verifyGameFileDidNotChange` |

The refusal is what makes it safe to leave BindForge open while the game runs: an in-game rebind cannot be silently overwritten by a stale draft. The current message offers only *reload from game* or *discard the draft* - both of which lose somebody's work.

### Direction: merge rather than choose a loser

**Agreed in principle 2026-09-06; dialog settled 2026-09-12 — [see below](#the-conflict-dialog--settled-2026-09-12).** The last row deserves better than a forced choice. `.binds` is a list of actions with stable names, each holding a primary and a secondary slot, so a three-way compare against the baseline resolves per action rather than per line:

- changed in the draft only -> take the draft
- changed live only -> take live
- changed both sides to the same value -> not a conflict
- changed both sides differently -> a real conflict, and the only case worth interrupting for

**Show without asking, where nothing conflicts.** The silent adoption in row three is correct behaviour and must survive: a player who has not touched BindForge should never be prompted about their own in-game rebind. Non-conflicting merges are reported - a summary the player can open - rather than confirmed. Confirmation is reserved for genuine conflicts.

### The conflict dialog — settled 2026-09-12

**It never interrupts.** Detection raises a banner on the BindForge screen and nothing more — no modal,
no stolen focus. The player may be mid-flight when their in-game rebind collides with a draft edit, and
**nothing is about to be written either way**, so there is nothing urgent to decide.

```
⚠ 3 controls were rebound in-game while you were
  editing. 2 merged cleanly. 1 needs your decision.

                              [ Review ]  [ Later ]

  APPLY is unavailable until the conflict is resolved.
```

The banner reports the clean merges in the same breath as the conflict, because *"2 merged cleanly"* is
what tells the player the remaining one is genuinely worth their attention rather than the usual noise.
**Later** dismisses it; the conflict stays, and **Apply stays unavailable** until it is resolved. That
is the existing refusal in `BindingsApplyService.verifyGameFileDidNotChange` doing its job — it is not
being relaxed, it is being given a way out that does not cost somebody their work.

#### The resolution list

**One row per conflicting slot**, both values side by side, and clicking a value picks it:

```
1 conflict     [ Keep all mine ]  [ Keep all in-game ]

CONTROL           SECONDARY
                  YOURS        IN-GAME
Landing Gear      ( ) L        ( ) G
Cargo Scoop       ( ) Home     ( ) End

  2 of 2 still to decide
                          [ Merge ]  disabled
```

**A row is a slot, not a control** — the column header says which, because
[Primary and Secondary are separate entities](#merge-grain-the-slot-not-the-action--settled-2026-09-07)
and one control can appear twice with different answers. The layout deliberately echoes the bind grid:
the player is choosing between two keys for a named control, which is what they do all day in
[Game Mode](bind-editor.md#game-mode).

**A full-file diff was rejected.** A `.binds` holds 774 slots, and showing two versions of it asks a
commander to think like a programmer about their joystick. One-at-a-time was rejected too: it hides how
much they are agreeing to.

#### Nothing is pre-selected

**Merge stays disabled until every row has an answer**, and the count says how many are left.

**Why no default, when a default would be one click.** Either default silently destroys work in the case
the player does not read the dialog. Pre-selecting *in-game* discards BindForge edits, which is the exact
outcome the apply refusal exists to prevent. Pre-selecting *yours* overwrites a binding the game is
already using. **A genuine conflict is rare by construction** — it needs both sides to change the same
slot to different values between one apply and the next — so the cost of asking is a few clicks in an
uncommon case, and the cost of guessing is lost work with nobody aware it happened.

**The bulk buttons are not a default.** *Keep all mine* and *Keep all in-game* set every row at once, but
pressing one is itself the decision. They exist so that twenty conflicts after a long session are not
twenty clicks; they do not pre-empt the choice.

#### Where the result goes

**Merge writes the draft, not the game.** The merged set — clean merges and resolved conflicts together —
becomes the new draft, and the baseline is reset to the live file it was reconciled against. The player
then reviews and applies like any other edit, so **the merge itself is discardable**: Discard puts the
draft back and the live file was never touched.

**Cancel resolves nothing.** The draft and the conflict both stand, and Apply remains unavailable. There
is no state in which closing the dialog loses an edit from either side.

**If the game changes again mid-resolution**, the comparison is simply re-run against the new live file.
Decisions already made on slots that did not change again are kept; a slot that moved underneath the
dialog returns to undecided rather than silently resolving to a value the player never saw.

### Merge grain: the slot, not the action — settled 2026-09-07

**Primary and Secondary are separate entities, not two halves of one.** A live change to *Primary* and a draft change to *Secondary* of the same action are **two independent edits and merge cleanly** — there is nothing to ask the player about.

It is not simply twice the element count, because the three element types carry different numbers of slots. From the 457-element reference specimen in [the format documentation](domain-knowledge/EliteDangerous-BindsFileFormat.md):

| Element type | Count | Slots each | Slots |
|---|---|---|---|
| **BUTTON** | 352 | 2 — Primary and Secondary | 704 |
| **AXIS** | 70 | **1** — never a Primary/Secondary pair | 70 |
| **STANDALONE SETTING** | 92 | **0** — a bare value; cannot conflict with anything | 0 |
| `KeyboardLayout` | 1 | 0 — the one text-content element | 0 |
| | 515 | | **774** |

*Counts refreshed 2026-09-08 against two independent `4.2` files, which agree exactly; an earlier 457-element specimen gave 268/72/117.*

So around 600 addressable slots on that file rather than the ~900 a flat doubling would suggest, and 117 elements with no slot to merge at all. Counts differ per file; the proportions are the point.

| Element type | Merge unit | Both sides changed it |
|---|---|---|
| Button | **per slot** | Primary vs Secondary — independent, no conflict |
| Axis | per element (its single slot) | conflict |
| Standalone setting | per element (its single value) | conflict — only one value can win |

**One exception, and it is a property rather than an input.** `ToggleOn` is **element-level, covering both Primary and Secondary as one shared behaviour** — explicitly unlike `Modifier` and `Hold`, which are per-slot. So on a button element the two input slots merge independently while the toggle flag sitting above them does not: both sides changing `ToggleOn` is a real conflict even when the slots either side of it merged cleanly.

The same reasoning covers file-level non-binding settings such as `KeyboardLayout`: one value, no slot, so both sides changing it is a conflict.

**Settled 2026-09-12:** [the conflict dialog](#the-conflict-dialog--settled-2026-09-12) resolves per slot, which is why the grain above is the thing it needed to know.

**Gap:** all of the above exists for `.binds` only. `DeviceMappings.xml` and `.buttonMap` have no draft, no baseline and no staleness check. Extending the same three-fingerprint treatment to them is new work.

## Writing While the Game Is Running

BindForge does not block writes while Elite Dangerous is running, and does not detect whether it is running. Reinstating such a block was reconsidered on 2026-09-06, on the strength of the in-game-rebind problem above, and rejected again: the three-fingerprint check catches precisely the case that is dangerous and leaves every other session unpenalised, which a blanket block does not.

Earlier designs specified a **Dormant Mode** that hard-blocked every write for the entire time the game was up. That was dropped in August 2026. Two reasons: the block window was far wider than the actual risk — the game only re-reads the Bind Domain (`.binds`) when its own in-game Controls menu is opened, so an edit made while the player is out flying has no effect until they next open that menu — and it made BindForge depend on game-process detection that the host does not provide.

Configuration is protected instead by two mechanisms that never depended on Dormant Mode and are unchanged:

- **A timestamped backup before every write**, with the atomic write-to-temp-then-replace pattern described
  under [Data Integrity Principle](#data-integrity-principle). Nothing reaches a live game file un-backed-up.
- **[Destructive Change Detection](#destructive-change-detection)**, which catches the case that motivated the
  original caution — the game overwriting a player's configuration — and offers to restore it, regardless of
  what caused the loss.

**Residual risk, stated plainly:** a write issued at the exact moment the game reads one of these files could still collide. That window is narrow and unmeasured. If it turns out to matter in practice, the cheapest mitigation is a warning at the point of writing rather than a session-long block.

## Upgrading the Existing Bind Editor

Elite-Intel already ships a bind editor — `ui.screen.BindingsTabPanel`, backed by ~20 classes in `elite.intel.ai.hands`. **BindForge is that editor, upgraded.** It is not a second section built beside it, and there is nothing to retire: anything BindForge was specified to do is built *into* the existing editor and grown up to these specs. See [Phased rollout](../../00-overview/v1.2-scope.md#phased-rollout--revised-2026-09-08) for the order, and [why the build-alongside plan was replaced](../../00-overview/v1.2-scope.md#the-bindforge-name-is-returning-not-arriving) for the reasoning.

That settles what used to be this document's hardest open question — which component owns the write path — by removing the second component. What follows is what the decision obliges.

### There is one writer, and it already exists

`BindingsApplyService`, `BindingsWorkingCopyRepository`, `BindingsWriter`, `BindingsBackupService` and `AppPaths.getBindingsWorkingDir()` are **BindForge's pipeline already**. They are not borrowed from a neighbour; the neighbour is the thing being upgraded. The [draft model](#live-file-synchronization) this document describes is the working-copy repository that exists today, with Apply as the only path to a live file — save one
documented exception at [First-Time Startup](#it-applies-immediately--settled-2026-09-12), which creates
a preset where none existed rather than modifying one.

**The hazard this replaces is worth naming, so that nobody rebuilds it.** A second editor would have meant two working copies and two writers over the same `.binds`. Each would have been individually safe — backup, atomic write, the [Data Integrity Principle](#data-integrity-principle) satisfied — and **jointly they would not have been:** one saves, the other is still holding a stale in-memory model, the player applies there, and the first editor's work is gone with no operation having misbehaved. The Data Integrity Principle does not
catch that, because it governs atomicity *within* an operation, not ownership *across* components.

**Under upgrade-in-place that failure cannot occur, because the second owner is never built.** The rule that keeps it that way is simple: BindForge adds no working copy, no writer and no working directory of its own. Where a capability is missing, the existing class grows it.

### Two narrow boundaries — one stays, one widens

`elite.intel.ai.hands` holds two deliberate restrictions. Under the old plan BindForge sidestepped both by building its own. Under this one they have to be faced directly, and **they do not have the same answer.**

**`KeyBindingsParser` stays exactly as narrow as it is.** It is a read-only, keyboard-only boundary feeding command execution — the path that presses real keys in the player's ship. Its narrowness is what stops a non-keyboard assignment from becoming executable by accident. BindForge reads `.binds` for editing through its own full-fidelity path and **must not widen the parser to do it**; the two readers answer different questions, and the parser's answer must stay small.

**`BindingsWriter` widens — settled 2026-09-12, reversing an earlier position.** It refuses today to edit any slot holding a controller assignment, allowing only an existing `Keyboard` slot or an empty `{NoDevice}` one (`isEditableMainSlot`). BindForge's remit is [every control on every device](#scope-every-control-every-input-device), so as the editor
becomes BindForge, the writer must be able to write what the editor can edit.

The refusal's stated reason survives the change, because it was never really about controllers:

> *The commander bound that device in the game and this application does not take it away.*

That is exactly right for an **assistant acting on its own initiative**, and it stays right — nothing in the voice or command path may touch a controller binding. It does not describe a **commander editing their own bindings on purpose**, which is the one thing a bind editor is for. A commander who rebinds a HOTAS button in the capture dialog *is* taking it away, intentionally, having been shown what was there.

So the boundary moves from *device type* to *who initiated the write*: **deliberate, commander-initiated edits may write any device; nothing automatic may write a controller slot.** Widening the writer does not widen the parser, so the protection the javadoc actually describes — keeping non-keyboard assignments out of command execution — is untouched.

#### Editing a binding is not the same as being able to press it

**Stated by Krondor 2026-09-12, and it is a hard capability limit rather than a policy:**

> *"While we may let users edit the HOTAS assignment as part of the binding management (which is the
> goal for BindForge) the app can't use / simulate any input except keyboard."*

So the writer widening is real but **one-directional**. BindForge gains the ability to *write* a
controller assignment into `.binds`; Elite-Intel gains no ability to *send* one. The reason the editor
refused controller slots in the first place was not caution about overwriting the commander's work —
it was that **there was no support for those devices at all.**

Two consequences worth building for:

1. **`KeyBindingsParser` staying narrow is not a stylistic choice.** It is the shape of what the
   application can actually do. Widening it would not make a HOTAS binding executable; it would only
   let an unpressable one reach the executor.
2. **A control the assistant drives, bound only to a controller, is unusable by the assistant** even
   though the commander sees it as bound. That is a real state with a real remedy, and it belongs in
   [Anomalies](bind-editor.md#four-kinds-one-question) rather than only in a log line.

### External change detection stays in one place

`BindingsMonitor` already watches the bindings directory with a `java.nio.file.WatchService`, re-parsing and
swapping a `volatile` map when the file changes. **It is extended to also register the detected game
installation folders**, rather than BindForge adding a watcher of its own — one component owns
external-change detection across all four [managed file domains](#managed-file-domains).

The install folders differ from the bindings directory in one way worth building for: there may be several,
and they appear and disappear as the player adds or removes installations, so registration is dynamic where
the bindings directory is fixed. Nothing in Elite-Intel reads `DeviceMappings.xml` or `.buttonMap` at
runtime, so this watch serves BindForge's own freshness checks rather than any live consumer.

### BindForge must not publish `BindingsUpdatedEvent`

**Verified 2026-09-06 — and the conclusion is the opposite of what this section first assumed.**

The concern was that changing a binding without telling the command-execution path — `KeyBindingExecutor`,
`InputSequenceExecutor`, `CustomCommandHandler`, `SafeKeyboardKeys` — would make the assistant press the
wrong key mid-flight. Worse than a stale screen: a wrong action in the game.

**That path does not subscribe to the event, and does not need to.** `InputSequenceExecutor` calls
`BindingsMonitor.getBindings()` fresh on every step, and the monitor's watcher re-parses on change. **Reload
is driven by the filesystem, not by the event.** There is no staleness gap and no pre-existing bug.

`BindingsUpdatedEvent` is a UI notification — its subscribers are UI panels because that is all it is for. It
is published by `BindingsMonitor` (the watcher noticed a change) and `PlayerBackupService` (restore).
Applying to a live `.binds` trips the watcher, which publishes the event itself; **a manual publish on top of
that double-fires.**

What BindForge owes instead is **one write per apply.** The monitor de-duplicates repeat notifications by
fingerprinting the file it last parsed — last-modified time plus size — and a write delivered in pieces
defeats that.

## Unsaved Work on Exit

**Settled 2026-09-06.** Leaving BindForge with unsaved edits offers **two** options: **Save** (write the
edits into the draft) or **Discard** (throw them away). Both are cheap and neither touches a game
installation. It fires on switching to another Elite-Intel tab and on application shutdown.

**Apply is deliberately not on this dialog.** Writing to a live game installation is the one action
BindForge takes that a player cannot shrug off, and it must never be the incidental outcome of navigating
away from a screen. It stays a button the player goes and presses on purpose - the same principle
[Alias Designer's action table](alias-designer.md#actions-and-what-each-one-reaches) states, and the same
one `BindingsApplyService` already enforces in code: *"The game directory is never touched until the user
explicitly requests apply."*

An earlier revision of this section specified a three-option dialog (Apply to Game / Keep Draft /
Discard). It put the only irreversible option first, in a dialog raised because the player was already
leaving. Recorded here because the wording survives in older notes.

## Document Map

- [Alias Designer](alias-designer.md)
- [Bind Editor](bind-editor.md) — Game Mode, Action Groups, Input Mode, Control Types, and shared conflict detection
- [Preset Editor](preset-editor.md)
- [File Manager](file-manager.md)
- [Roadmap and Open Items](roadmap.md)
- [Binding Schema (data model)](../../03-data-models/binding-schema.md)
- [Domain Knowledge](domain-knowledge/) — Elite Dangerous file formats, action catalog, conflict rules, and the interactive bind-conflict test pages
- [Reference Data](reference-data/) — the raw action catalog and binding-zone spreadsheet BindForge is seeded
  from, Frontier's stock files, and the real `.binds` specimens several findings rest on (see
  [SPECIMENS-README.md](reference-data/SPECIMENS-README.md))
