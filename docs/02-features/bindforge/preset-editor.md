# BindForge — Preset Editor

**Purpose:** manage the preset file (`StartPreset.4.start`) directly, giving the player visibility into an otherwise-invisible system file, and support for the unusual-but-valid split preset file described in [Managed File Domains](overview.md#managed-file-domains).

## Draft model — same as every other section

**Settled 2026-09-08.** Preset Editor uses the same draft-and-apply model as the rest of BindForge: **Save**
writes to the draft, **Discard** throws draft edits away, and **Apply** is what reaches the live
`StartPreset.4.start` — through the shared apply pipeline, with its backup-before-write.

An earlier revision had Save writing directly to the live file with no Apply step, on the reasoning that
there is only one preset file, in one stable location shared across every storefront, so a draft layer
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

Four dropdowns — General, Ship, SRV, On Foot Controls — each populated with the **BindNames** of the binds files in the bindings folder, defaulting to whatever the preset file currently specifies. **A file is included on the strength of its contents, not its extension** (added 2026-09-17): the folder demonstrably holds files with a `.binds` name that are not binds files at all — most often a preset file picked by mistake, since it sits right beside them — and offering one as a binds file would point a section at something the game cannot load. See [what a `.binds` file might not be](file-manager.md#what-a-binds-file-might-not-be). A "Set All to Same" dropdown-plus-button bulk-sets all four at once; this is a shortcut, not a separate commit path — Save is still required afterward.

**The dropdowns also offer the factory binds files** (settled 2026-10-17, Alan, P1) — see [the BindName list](#the-bindname-list--settled-2026-10-17-alan-p1).

### The BindName list — settled 2026-10-17 (Alan, P1)

Built as `elite.intel.bindforge.preseteditor.BindNameCatalog`.

- **Factory binds files are listed beside the player's own.** A section can sit on a factory binds file — the specimen's
  General line is `KeyboardMouseOnly` — and [switching back to one](#relationship-to-first-time-startup) is an
  ordinary edit, so the list reads each installation's `ControlSchemes` as well as the bindings folder. Each
  entry is marked as the player's or factory. The same factory binds file in several installations is **one** entry,
  holding a file from each; an installation whose folder is missing or cannot be listed is skipped, so an
  unplugged drive does not empty the list.
- **A player's binds file with the BindName of a factory one is offered as the player's**, and a warning is logged. No
  factory binds file has a version suffix, so it is not expected.
- **Two of the player's files for one BindName cannot happen** in a live folder (Alan: the game upgrades
  `Custom.3.0.binds` to `Custom.4.2.binds` in place). If the list meets it anyway, it keeps both files under
  the one BindName and logs a warning, rather than letting one silently replace the other.
- **What is a binds file:** a `<Root>` element with at least one child holding a `<Primary>`, `<Secondary>` or
  `<Binding>`. Settings alone (`<KeyboardLayout>`, `<MouseXMode>`) do not count. Every other file with a
  `.binds` name comes back with a reason the screen turns into a sentence: a preset file (*"a preset file, not a binds file"*), another kind of
  XML (named by its root element), a `<Root>` with no bindings, no `PresetName`, a name mismatch (below), not
  XML, empty, or unreadable. A file whose name does not end `.binds` — the `.binds.backup` specimen — is not
  looked at.
- **A file whose `PresetName` does not match its file name is not offered.** `Custom.4.2 (2).binds` says
  `PresetName="Custom"`: offered as `Custom` it would claim a BindName `Custom.4.2.binds` already answers to, and
  offered as `Custom.4.2 (2)` it would write a line no file's `PresetName` matches. It is reported, never
  offered.

## What a line actually holds — the BindName, not the file name

**Added 2026-09-17.** Every other part of this document assumes the four lines are understood, and nothing
said what goes in them. Since writing this file is the whole job of the screen, the rule belongs here.

**A line holds the BindName: the filename with the game-assigned version suffix and the extension
removed.** Verified against the live folder and the
[specimens](reference-data/SPECIMENS-README.md):

| File in the bindings folder | The line that names it |
|---|---|
| `DualVirpilDawnTreader.4.2.binds` | `DualVirpilDawnTreader` |
| `Custom.4.2.binds` | `Custom` |
| `KeyboardMouseOnly.binds` (a factory binds file, no suffix) | `KeyboardMouseOnly` |

**Refined 2026-10-17 (Alan, P1): the line holds the file's own `PresetName`**, the attribute on its `<Root>`
element, and that must match the BindName the file name gives by the rule above. The factory binds files
on Alan's installation all match (30 of 30, `KeyboardMouseOnly.binds` → `PresetName="KeyboardMouseOnly"`), as do
the specimens, except `Custom.4.2 (2).binds`, whose `PresetName` is `Custom`. So the file name rule is the check,
and `PresetName` is the value. A file where the two disagree is [not offered](#the-bindname-list--settled-2026-10-17-alan-p1).

**Get this wrong and the failure is silent.** A section repointed at a BindName no file answers to does not
raise an error — the game simply loads something else, and the user discovers it in the cockpit. This
is the one write in BindForge where a single mistyped string costs a flight.

### A name does not identify a file

~~**One name can mean several files.**~~ **Withdrawn 2026-09-21: a live folder never holds two versions of
one binds file** (Alan). `reference-data/` does hold both `Custom.3.0.binds` and `Custom.4.2.binds`, but they are
collected specimens, not one user's folder. In a live install Elite migrates an older-version file to the
current version under the same name and the old one goes, and **only the Live game is supported** — so the BindName
in `StartPreset.4.start` identifies exactly one file. The dropdowns still offer *BindNames*, because that is what
the file stores and what the list on the game's Controls screen shows.

**And the suffix is not guaranteed.** `Custom.4.2 (2).binds` — a real file, from a download that de-duplicated
by adding ` (2)` — does not fit `name.major.minor.binds`, so its whole basename is the BindName it gives — and since its `PresetName` is `Custom`, it is [not offered](#the-bindname-list--settled-2026-10-17-alan-p1) (2026-10-17). A transform that
assumes a version suffix would mangle it. Strip a suffix **only when one is there**.

*The `(2)` file is in the shipped specimens rather than hypothetical, which is why it is written down here
rather than discovered during implementation.*

## Consistency Warning

A live, informational banner (updates as the dropdowns change, not only on save) appears if the four values do not all match. It never blocks saving, since the game genuinely supports a split configuration.

## Sync Badge

- **IN SYNC** (green) — no saved draft differs from the master.
- **DRAFT** (amber) — a saved draft differs from the master: saved, not yet applied
  ([`hasDraftChanges()`](#the-master-and-the-draft--settled-2026-10-18-alan-p2)).

**Settled 2026-10-20 (Alan): the badge compares the draft with the master, not with the game's file.** It says
whether there is work for Apply to do. A change the game made to its own file is a separate signal — the three-way
compare's *game only* lines, shown by [change detection](#sync-badge) (P5) — so a game rewrite never lights DRAFT
on its own, and edits the user has not yet saved are the dropdowns' state, not the badge's.

**Shown as coloured text, not a pill** (settled 2026-09-21) — the state word in its colour, the way the AI tab's
Quick Status panel shows KEYMAP — IN SYNC. See [the UI component map](ui-component-map.md#2-statusbadge-draws-a-pill).

Preset Editor participates in [Live File Synchronization](overview.md#live-file-synchronization)'s [Destructive Change Detection](overview.md#destructive-change-detection): if the live preset file changes externally to something that looks like data loss (for example, all four lines reset to a single default BindName that doesn't match what the user had configured), Preset Editor does not just quietly update its dropdowns to match. It flags the change as a suspected destructive reset rather than legitimate truth, and offers to restore the prior configuration, the same as every other managed domain.

### The master and the draft — settled 2026-10-18 (Alan, P2)

Built as `elite.intel.bindforge.preseteditor.StartPresetStore` and `StartPresetComparison`.

- **The master** is `elite-intel/bindings/master/StartPreset.4.start`, a byte copy of the game's live file. **It is
  made from the game's file the first time it is asked for and none exists** — without one, the draft and the
  game's file have nothing to compare against, and only Elite-Intel's own folder is written. A file already wiped
  then becomes the master; there is no earlier state to know it by.
- **The draft** is `elite-intel/bindings/draft/StartPreset.4.start`, always four lines. **Save of values the same as
  the master's leaves no draft at all**, so a draft that exists holds a change when it is saved. "Has the draft
  changed" is *a draft that differs from the master*, which is saved-but-not-applied and [not unsaved work](overview.md#built-as-a-ruling-not-code--2026-10-12-alan-core-c6):
  compared rather than taken from the draft existing, because P3's promote or P5's adopt can move the master under
  a draft. **A draft with no master counts as changed** (Alan, 2026-10-20) — only something outside BindForge can
  cause it, and saved work is never hidden. The form the game's file gets is Apply's decision, not the draft's.
- **Values are compared, never bytes.** The game rewrites its files with nothing changed
  ([Freshness Checks](overview.md#freshness-checks)), so the single-line form equals four identical lines, and a
  line ending or byte-order mark changes nothing.
- **The three-way compare** is per line — master, draft (the master when there is none), game: unchanged, draft
  only, game only, both to the same value, or both to different values. Only the last is a conflict.
- **Discard below is the screen's** — the dropdowns back to the draft. The store's only counterpart is dropping the
  draft, so it is the master again, which Apply's promote needs.

### Apply — settled 2026-10-20 (Alan, P3)

Built as `elite.intel.bindforge.preseteditor.StartPresetApply`.

- **Merged per line, as the four lines are independent.** A line the draft changed takes the draft's value; a line
  only the game changed keeps the game's; a line both changed to the same value takes it. **A line both changed to
  different values is a conflict, and Apply writes nothing** until it is settled ([change detection](#sync-badge),
  P5). Refusing whenever the game's file moved at all, as `.binds` Apply does, was rejected: it would block Apply on a
  change to a line the draft never touched.
- **Every one of the four lines must name a binds file** that the [BindName list](#the-bindname-list--settled-2026-10-17-alan-p1)
  offers — a line the draft did not change included, and **an empty line is refused too**. The failure is silent in
  the game, so Apply refuses rather than writes it, and names the sections.
- **The game's form: always four lines, CRLF between them, no newline after the last** — the specimen byte for byte.
  A single-line file is written as four lines; the single-line form is pre-Odyssey and only the Live game is
  supported. Written to the file `findLiveStartPresetFile` names, through `AtomicFiles`.
- **Values the game's file already holds are not written.** Nothing goes to Edit History; the game's file becomes the
  master as it is, and the draft is dropped.
- **No draft, nothing applied** — even when the game's file has changed. Taking a game change into the master is
  adopt (P5), never a side effect of Apply. A draft with no master, and no game preset file at all, are refused.
- **Edit History:** the replaced file is copied to `AppPaths.getBindingsBackupDir()` — beside the `.binds` Apply
  copies, as `StartPreset.4.start.<yyyyMMdd-HHmmss>.bak` — and pruned to the Edit History retention, before the write.
  No copy, no write.
- **Then the written file becomes the master** (a byte copy) and the draft is dropped.
- **Not done by Apply: telling the assistant.** `BindingsMonitor` reacts only to `.binds` names, so the assistant does
  not reload the newly active General binds file after Apply — nor after the game changes the preset file. That is a
  V1.1 defect and is fixed on `V1.1-Release`, not here.

## Discard

Throws away unsaved dropdown changes, returning all four to the draft. *Named "Revert" in an earlier
revision; renamed 2026-09-08 to match Save/Discard/Apply everywhere else.*

## Relationship to Bind Editor

The [Bind Editor's](bind-editor.md) file selector controls what's open for viewing and editing. The Preset Editor controls what the game actually loads per binding section. These are independent of each other — editing a `.binds` file in the Bind Editor does not change which file is active, and vice versa.

## Relationship to First-Time Startup

[First-Time Startup](overview.md#first-time-startup) reads the preset file to detect any section still on a factory binds file, and — if the player agrees to create a custom file for it — repoints that section's entry through this same mechanism, not a separate write path.

**Settled 2026-09-12: First-Time Startup applies immediately.** It does not leave a draft, and it is the
only place in BindForge that writes a live game file outside Apply — see
[It applies immediately](overview.md#it-applies-immediately--settled-2026-09-12) for the reasoning and
the limits of the exception.

**What that means for this screen:** the repoint arrives here already done. A player who opened
BindForge for the first time and accepted the offer finds their sections **IN SYNC**, pointing at the
new custom files, with nothing pending. Preset Editor is then how they change their mind — switching a
section back to a factory binds file is an ordinary edit through the normal draft-and-Apply path, because
by that point there is real configuration to protect.

## What exists in code today — checked 2026-09-17

| Piece | State |
|---|---|
| `BindingsLoader.findStartPresetFile(Path)` | **exists** — finds `StartPreset.*.start` by prefix and extension, so it copes with the version suffix. ~~Reusable as-is.~~ **Not for this screen** (2026-10-20): it takes whichever file the listing yields first, so with both generations present it can name the old one. Use `findLiveStartPresetFile` — see [How the four lines are read](#how-the-four-lines-are-read--settled-2026-10-12-alan-foundation-f1). |
| `BindingsLoader.getActivePresetName()` | **exists, and reads one line** — see below |
| Writing the file | ~~**does not exist.**~~ **Built 2026-10-20 (P3)** as `preseteditor.StartPresetApply` — see [Apply](#apply--settled-2026-10-20-alan-p3). The only writer of the game's `StartPreset.*.start`. |
| Backup coverage | `PlayerBackupService` already includes the file, and `SetupCheck` reads it. |

**`getActivePresetName()` collapses the four sections into one.** It takes the first non-empty line, and its
own comment explains why: *“The file repeats the preset name on multiple lines; we take the first non-empty
one.”* That holds for the common case where all four lines match — and it is wrong for exactly the
configuration this screen exists to support. The shipped specimen `StartPreset.4.start` reads
`KeyboardMouseOnly / Custom / Custom / Custom`, so today's code would report *KeyboardMouseOnly* as **the**
active binds file and silently ignore what Ship, SRV and On Foot are pointing at.

**This is not a bug in what it was written for.** It answers *“which binds file is the user on?”* for a
setup with one BindName on every line, which is what the assistant needed. BindForge needs the four values, so the honest
change is a **second method returning all four** rather than altering what the existing one promises — the
same shape as [the Missing scan's two questions](bind-editor.md#missing-has-two-shapes--added-2026-09-12),
where one caller wants a summary and another wants the truth per slot.

**So the build order for this screen is:** read all four lines, then the draft model, then the write path
(which is new code, guarded by the same backup-before-write as every other Apply).

### How the four lines are read — settled 2026-10-12 (Alan, Foundation F1)

`BindingsLoader.readStartPresetSections(Path)` returns a `StartPresetSections` (General, Ship, SRV, On Foot),
built on the [foundation branch](../../05-build-plan/foundation.md) because `BindingsLoader` is a file V1.1 ships.

- **Which file:** the highest-numbered `StartPreset.N.start`, else `StartPreset.start`, through a new
  `findLiveStartPresetFile`. Only the Live game is supported, and it reads `.4.start`; the unnumbered file is
  the leftover pre-Odyssey form. `findStartPresetFile` keeps its listing-order answer for its existing callers.
- **One line** is the pre-Odyssey form: one BindName for all four, flagged `singleLine`. *Exactly* one non-blank
  line, with at most a line ending after it (tightened 2026-10-12, Alan, from the F1 code review): a name
  followed by blank lines is the four-line form with sections missing, so a file that lost three lines reads as
  General only, and Destructive Change Detection can see it, rather than as a valid uniform preset file. The record
  refuses `singleLine` with differing names. How a write treats the single-line form is this screen's decision.
- **By position, not by skipping blanks.** Each line is trimmed, and a blank line is an empty value for its
  section wherever it is, so nothing moves up a section. That is the one place this read differs from
  `getActivePresetName()`.
- **Short or long files:** a missing section is empty and lines past the fourth are ignored. Whether that
  looks like a destructive reset is this screen's call, not the reader's.
- **No file** is an empty `Optional`; **a read failure** is an `IOException`, not an empty answer.

## Deferred

**Binds File Rename and Duplicate** — renaming a binds file, or duplicating one under a new BindName — is explicitly out of scope, tracked as Preset Editor's own future item rather than the project-wide roadmap.

## Scope, Confirmed

Preset Editor only ever repoints the preset file — it never moves, hides, or deletes `.binds` files. This isn't just the simpler of two possible designs; it's the only one that makes sense given how the game actually finds `.binds` files: the game looks in exactly one shared bindings folder and lists every `.binds` file it finds there on its Controls screen. There's no mechanism by which "hiding" a file from the game by relocating it out of that folder would even be meaningful — a `.binds` file isn't something that can usefully live anywhere else. The only file movement Preset Editor's siblings ever perform is the standard edit-and-apply flow already described in [Live File Synchronization](overview.md#live-file-synchronization): copying an edited working copy over the live file in the game's one bindings folder on Save/Apply. An earlier design generation had described BindForge itself controlling file visibility via an Active/Inactive distinction; that concept doesn't apply given how the game actually discovers these files.
