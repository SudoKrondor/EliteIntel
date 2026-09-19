# BindForge — Preset Editor

**Purpose:** manage the Active Preset file (`StartPreset.#.start`) directly, giving the player visibility into an otherwise-invisible system file, and support for the unusual-but-valid "split preset" configuration described in [Managed File Domains](overview.md#managed-file-domains).

## Draft model — same as every other section

**Settled 2026-09-08.** Preset Editor uses the same draft-and-apply model as the rest of BindForge: **Save**
writes to the draft, **Discard** throws draft edits away, and **Apply** is what reaches the live
`StartPreset.#.start` — through the shared apply pipeline, with its backup-before-write.

An earlier revision had Save writing directly to the live file with no Apply step, on the reasoning that
there is only one Active Preset file, in one stable location shared across every storefront, so a draft layer
buys little. That reasoning is fine on its own terms and it is still true that this file is simpler than the
others. It was **overruled for consistency**: `file-manager.md` states that *exactly one thing in BindForge
writes to a live game file, and it is Apply*, and a guarantee with an exception in it is not a guarantee.
The player should never have to remember which section of BindForge is the one that writes as you go.

### The four lines are four independent entities

The [merge grain](overview.md#merge-grain-the-slot-not-the-action--settled-2026-09-07) applies here in its
simplest possible form. General, Ship, SRV and On Foot are four independent values in one file, exactly as
Primary and Secondary are independent slots on one binding — so a change made in-game to the Ship line and a
draft change to the SRV line **merge cleanly, with nothing to ask about**. Only the same line changed on both
sides is a conflict.

## Layout

Four dropdowns — General, Ship, SRV, On Foot Controls — each populated with the **preset names** of the `.binds` files in the bindings folder, defaulting to whatever the Active Preset file currently specifies. **A file is included on the strength of its contents, not its extension** (added 2026-09-17): the folder demonstrably holds files with a `.binds` name that are not binds files at all — most often a `StartPreset` picked by mistake, since it sits right beside them — and offering one as a preset would point a section at something the game cannot load. See [what a `.binds` file might not be](file-manager.md#what-a-binds-file-might-not-be). A "Set All to Same" dropdown-plus-button bulk-sets all four at once; this is a shortcut, not a separate commit path — Save is still required afterward.

## What a line actually holds — the preset name, not the file name

**Added 2026-09-17.** Every other part of this document assumes the four lines are understood, and nothing
said what goes in them. Since writing this file is the whole job of the screen, the rule belongs here.

**A line holds the preset name: the filename with the game-assigned version suffix and the extension
removed.** Verified against the live folder and the
[specimens](reference-data/SPECIMENS-README.md):

| File in the bindings folder | The line that names it |
|---|---|
| `DualVirpilDawnTreader.4.2.binds` | `DualVirpilDawnTreader` |
| `Custom.4.2.binds` | `Custom` |
| `KeyboardMouseOnly.binds` (a shipped preset, no suffix) | `KeyboardMouseOnly` |

**Get this wrong and the failure is silent.** A section repointed at a name no file answers to does not
raise an error — the game simply loads something else, and the commander discovers it in the cockpit. This
is the one write in BindForge where a single mistyped string costs a flight.

### A name does not identify a file

**One name can mean several files.** `reference-data/` holds both `Custom.3.0.binds` and
`Custom.4.2.binds`, and each reduces to `Custom`. The game resolves which one to load by its own version, so
**BindForge must not assume a name maps to exactly one file** — the dropdowns offer *names*, which is what the
file stores and what the game's own preset list shows.

**And the suffix is not guaranteed.** `Custom.4.2 (2).binds` — a real file, from a download that de-duplicated
by adding ` (2)` — does not fit `name.major.minor.binds`, so its whole basename is the name. A transform that
assumes a version suffix would mangle it. Strip a suffix **only when one is there**.

*Both are in the shipped specimens rather than hypothetical, which is why they are written down here rather
than discovered during implementation.*

## Consistency Warning

A live, informational banner (updates as the dropdowns change, not only on save) appears if the four values do not all match. It never blocks saving, since the game genuinely supports a split configuration.

## Sync Badge

- **IN SYNC** (green) — the draft matches the live Active Preset file.
- **DRAFT** (amber) — the draft differs from the live file, whether or not those edits have been saved.

Preset Editor participates in [Live File Synchronization](overview.md#live-file-synchronization)'s [Destructive Change Detection](overview.md#destructive-change-detection): if the live Active Preset file changes externally to something that looks like data loss (for example, all four lines reset to a single default preset name that doesn't match what the user had configured), Preset Editor does not just quietly update its dropdowns to match. It flags the change as a suspected destructive reset rather than legitimate truth, and offers to restore the prior configuration, the same as every other managed domain.

## Discard

Throws away unsaved dropdown changes, returning all four to the draft. *Named "Revert" in an earlier
revision; renamed 2026-09-08 to match Save/Discard/Apply everywhere else.*

## Relationship to Bind Editor

The [Bind Editor's](bind-editor.md) file selector controls what's open for viewing and editing. The Preset Editor controls what the game actually loads per binding section. These are independent of each other — editing a `.binds` file in the Bind Editor does not change which file is active, and vice versa.

## Relationship to First-Time Startup

[First-Time Startup](overview.md#first-time-startup) reads the Active Preset file to detect any section still on a stock, factory preset, and — if the player agrees to create a custom file for it — repoints that section's entry through this same mechanism, not a separate write path.

**Settled 2026-09-12: First-Time Startup applies immediately.** It does not leave a draft, and it is the
only place in BindForge that writes a live game file outside Apply — see
[It applies immediately](overview.md#it-applies-immediately--settled-2026-09-12) for the reasoning and
the limits of the exception.

**What that means for this screen:** the repoint arrives here already done. A player who opened
BindForge for the first time and accepted the offer finds their sections **IN SYNC**, pointing at the
new custom files, with nothing pending. Preset Editor is then how they change their mind — switching a
section back to a factory preset is an ordinary edit through the normal draft-and-Apply path, because
by that point there is real configuration to protect.

## What exists in code today — checked 2026-09-17

| Piece | State |
|---|---|
| `BindingsLoader.findStartPresetFile(Path)` | **exists** — finds `StartPreset.*.start` by prefix and extension, so it copes with the version suffix. Reusable as-is. |
| `BindingsLoader.getActivePresetName()` | **exists, and reads one line** — see below |
| Writing the file | **does not exist.** Nothing in the codebase writes `StartPreset.*.start`. |
| Backup coverage | `PlayerBackupService` already includes the file, and `SetupCheck` reads it. |

**`getActivePresetName()` collapses the four sections into one.** It takes the first non-empty line, and its
own comment explains why: *“The file repeats the preset name on multiple lines; we take the first non-empty
one.”* That holds for the common case where all four lines match — and it is wrong for exactly the
configuration this screen exists to support. The shipped specimen `StartPreset.4.start` reads
`KeyboardMouseOnly / Custom / Custom / Custom`, so today's code would report *KeyboardMouseOnly* as **the**
active preset and silently ignore what Ship, SRV and On Foot are pointing at.

**This is not a bug in what it was written for.** It answers *“which preset is the commander on?”* for a
single-preset setup, which is what the assistant needed. BindForge needs the four values, so the honest
change is a **second method returning all four** rather than altering what the existing one promises — the
same shape as [the Missing scan's two questions](bind-editor.md#missing-has-two-shapes--added-2026-09-12),
where one caller wants a summary and another wants the truth per slot.

**So the build order for this screen is:** read all four lines, then the draft model, then the write path
(which is new code, guarded by the same backup-before-write as every other Apply).

## Deferred

**Preset Rename and Duplicate** — renaming `.binds` files, or duplicating a preset under a new name — is explicitly out of scope, tracked as Preset Editor's own future item rather than the project-wide roadmap.

## Scope, Confirmed

Preset Editor only ever repoints the Active Preset file — it never moves, hides, or deletes `.binds` files. This isn't just the simpler of two possible designs; it's the only one that makes sense given how the game actually finds `.binds` files: the game looks in exactly one shared bindings folder and lists every `.binds` file it finds there as a selectable preset. There's no mechanism by which "hiding" a file from the game by relocating it out of that folder would even be meaningful — a `.binds` file isn't something that can usefully live anywhere else. The only file movement Preset Editor's siblings ever perform is the standard edit-and-apply flow already described in [Live File Synchronization](overview.md#live-file-synchronization): copying an edited working copy over the live file in the game's one bindings folder on Save/Apply. An earlier design generation had described BindForge itself controlling file visibility via an Active/Inactive distinction; that concept doesn't apply given how the game actually discovers these files.
