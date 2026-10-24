# BindForge — Preset Editor

**Purpose:** manage the preset file (`StartPreset.4.start`) directly, giving the player visibility into an otherwise-invisible system file, and support for the unusual-but-valid split preset file described in [Managed File Domains](overview.md#managed-file-domains).

## Edits and APPLY — same as every other section

**The Preset Editor holds its changes on screen as edits not applied, like every other section**, and the one
[APPLY above BindForge's tabs](overview.md#apply-and-discard--one-pair-above-bindforges-tabs-2026-10-24-alan) writes
them ([storage.md](storage.md) rules 6–7). The Preset Editor has no APPLY or DISCARD of its own. *Settled
2026-09-08, for consistency: the user should never have to remember which section of BindForge is the one that
writes as you go.*

### The four lines are four independent entities

General, Ship, SRV and On Foot are four separate values in one file, so in the
[merge](overview.md#reconciling-bindforge-edits-with-in-game-rebinds) each line is its own row.

## Layout

Four dropdowns — General, Ship, SRV, On Foot Controls — each populated with the **BindNames** of the binds files in the bindings folder, defaulting to whatever the preset file currently specifies. **A file is included on the strength of its contents, not its extension** (added 2026-09-17): the folder demonstrably holds files with a `.binds` name that are not binds files at all — most often a preset file picked by mistake, since it sits right beside them — and offering one as a binds file would point a section at something the game cannot load. See [what a `.binds` file might not be](file-manager.md#what-a-binds-file-might-not-be). A "Set All to Same" dropdown-plus-button bulk-sets all four at once; this is a shortcut — the change is an edit not applied, and APPLY writes it.

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

## Is there anything to apply?

**The APPLY / DISCARD bar above BindForge's tabs answers it** for the Preset Editor as for every tab
([overview](overview.md#apply-and-discard--one-pair-above-bindforges-tabs-2026-10-24-alan)). *Until 2026-10-24 the
Preset Editor had its own IN SYNC / DRAFT badge (settled 2026-10-20); the bar replaced it. The Vega tab's KEYMAP
readout in Quick Status, which showed the same thing for `.binds`, is removed too (2026-10-24, Alan).*

**A preset file changed outside BindForge** — the game, an update, a reset to one factory name on all four lines — is
found by the check like every other file and never taken quietly as the truth: the user is shown the change and
decides ([storage.md](storage.md) rule 14).

### What APPLY checks and writes for the preset file — 2026-10-24

*Until 2026-10-24 the preset file had its own master and draft files, a three-way per-line compare and merge, and Edit
History copies (P2, P3); [storage.md](storage.md) replaced them. Kept from P1 and P3 (Alan, 2026-10-21):*

- **Every one of the four lines must name a binds file the [BindName list](#the-bindname-list--settled-2026-10-17-alan-p1)
  offers** — unchanged lines included, and an empty line is refused too. A line that names nothing real fails
  silently in the game, so **APPLY checks this first and refuses as a whole, naming the sections, before anything is
  backed up or written** (2026-10-24, Alan).
- **The form written: always four lines, CRLF between them, no newline after the last** — the specimen byte for byte.
  A single-line file is written as four lines; the single-line form is pre-Odyssey and only the Live game is
  supported.
- **Values are compared, never bytes** ([Freshness Checks](overview.md#freshness-checks)): the single-line form equals
  four identical lines, and a line ending or byte-order mark changes nothing.
- **APPLY reads and writes the one file it found** — the file `findLiveStartPresetFile` names, looked up once (P3
  review, finding 2).
- **Not done by APPLY: telling the assistant.** `BindingsMonitor` reacts only to `.binds` names, so the assistant does
  not reload the newly active General binds file after APPLY — nor after the game changes the preset file. That is a
  V1.1 defect and is fixed on `V1.1-Release`, not here.

## Discard

The one DISCARD above BindForge's tabs puts the dropdowns back to what the working copy holds, with every other edit
not applied (2026-10-24, Alan).

## Relationship to Bind Editor

The [Bind Editor's](bind-editor.md) file selector controls what's open for viewing and editing. The Preset Editor controls what the game actually loads per binding section. These are independent of each other — editing a `.binds` file in the Bind Editor does not change which file is active, and vice versa.

## Relationship to First-Time Startup

**[CREATE](overview.md#create--a-new-players-first-binds-file--2026-10-24) points all four lines at the new binds file**
as edits not applied, so the Preset Editor shows them pointing there and the APPLY bar is lit until APPLY writes them
(2026-10-24, Alan). Switching a section back to a factory binds file later is an ordinary edit.

## What exists in code today — checked 2026-09-17

| Piece | State |
|---|---|
| `BindingsLoader.findStartPresetFile(Path)` | **exists** — finds `StartPreset.*.start` by prefix and extension, so it copes with the version suffix. ~~Reusable as-is.~~ **Not for this screen** (2026-10-20): it takes whichever file the listing yields first, so with both generations present it can name the old one. Use `findLiveStartPresetFile` — see [How the four lines are read](#how-the-four-lines-are-read--settled-2026-10-12-alan-foundation-f1). |
| `BindingsLoader.getActivePresetName()` | **exists, and reads one line** — see below |
| Writing the file | ~~**does not exist.**~~ **Built 2026-10-20 (P3)** as `preseteditor.StartPresetApply` — see [Apply](#what-apply-checks-and-writes-for-the-preset-file--2026-10-24). The only writer of the game's `StartPreset.*.start` — to be reworked: APPLY writes the preset file from the working copy ([storage.md](storage.md)). |
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

### How the four lines are read — settled 2026-10-12 (Alan, Foundation F1)

`BindingsLoader.readStartPresetSections(Path)` returns a `StartPresetSections` (General, Ship, SRV, On Foot),
built on the [foundation branch](../../05-build-plan/foundation.md) because `BindingsLoader` is a file V1.1 ships.

- **Which file:** the highest-numbered `StartPreset.N.start`, else `StartPreset.start`, through a new
  `findLiveStartPresetFile`. Only the Live game is supported, and it reads `.4.start`; the unnumbered file is
  the leftover pre-Odyssey form. `findStartPresetFile` keeps its listing-order answer for its existing callers.
- **One line** is the pre-Odyssey form: one BindName for all four, flagged `singleLine`. *Exactly* one non-blank
  line, with at most a line ending after it (tightened 2026-10-12, Alan, from the F1 code review): a name
  followed by blank lines is the four-line form with sections missing, so a file that lost three lines reads as
  General only, and the check can see it, rather than as a valid uniform preset file. The record
  refuses `singleLine` with differing names. How a write treats the single-line form is this screen's decision.
- **By position, not by skipping blanks.** Each line is trimmed, and a blank line is an empty value for its
  section wherever it is, so nothing moves up a section. That is the one place this read differs from
  `getActivePresetName()`.
- **Short or long files:** a missing section is empty and lines past the fourth are ignored. Whether that
  looks like a destructive reset is this screen's call, not the reader's.
- **No file** is an empty `Optional`; **a read failure** is an `IOException`, not an empty answer.

## Deferred

**Binds File Duplicate** — copying a binds file under a new BindName — is out of scope, tracked as Preset Editor's own future item. *Renaming a binds file is no longer deferred: it is the Bind Editor's [Name box](bind-editor.md#shell-common-to-all-modes) (2026-10-24, Alan).*

## Scope, Confirmed

Preset Editor only ever repoints the preset file — it never moves, hides, or deletes `.binds` files. This isn't just the simpler of two possible designs; it's the only one that makes sense given how the game actually finds `.binds` files: the game looks in exactly one shared bindings folder and lists every `.binds` file it finds there on its Controls screen. There's no mechanism by which "hiding" a file from the game by relocating it out of that folder would even be meaningful — a `.binds` file isn't something that can usefully live anywhere else. The only file movement Preset Editor's siblings ever perform is APPLY writing the working copy to the live file in the game's one bindings folder ([Live File Synchronization](overview.md#live-file-synchronization)). An earlier design generation had described BindForge itself controlling file visibility via an Active/Inactive distinction; that concept doesn't apply given how the game actually discovers these files.
