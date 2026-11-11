# BindForge — Files: back up, edit, apply

**Settled 2026-10-22 (Alan).** How BindForge stores, edits, writes, backs up and restores the user's game input
configuration files. [The principles](principles.md) come first, and define the words used here — *working copy*,
*live copy*, *backup*, *edits not applied*. Where this page and another BindForge document disagree, the work stops
and Alan decides; the other documents have not all been brought into line with this page yet.

The files are the [game input configuration files](principles.md#words), and only these:

- from the bindings folder: **`StartPreset.4.start`** and **every `<name>.<major>.<minor>.binds`**;
- from each installation's `ControlSchemes` folder: **`DeviceMappings.xml`** and **every
  `DeviceButtonMaps\<device name>.buttonMap`**.

Every rule applies to all of them, in every installation, unless it says otherwise.

## First-time startup

Runs once, controlled by a stored "first-time startup complete" flag. *While V1.2 is tested, File Manager has a
button that clears the flag so first-time startup runs again — [Run first-time startup again](file-manager.md#game-install-locations),
to be hidden or removed before V1.2 ships (2026-10-24, Alan).*

1. When the user clicks the BindForge tab and the flag is not complete:
   - **Find installations.** BindForge lists every installation Elite-Intel already knows about, with two buttons:
     **Confirm** and **Add missing**.
     - **Confirm** goes ahead with the installations in the list.
     - **Add missing** lets the user browse to an installation's root folder. BindForge checks that folder for
       `EliteDangerous64.exe` and the other files that confirm it is an installation, then adds it to the list. The
       list is stored in the user's data like every other user setting.

   The same list, with **Remove** beside each row, is File Manager's **Game Install Locations** sub-tab, where it can
   be changed at any time. **An installation added later, or one that was unreachable at first setup and comes
   back,** is compared with the working copy: if its device files match, nothing is done; if they differ, it is
   reconciled as in rule 2 before anything is written to it. It is never overwritten automatically (Alan,
   2026-10-25, from the S2b review; until then it got the working copy at the next APPLY). **RESTORE is the one
   exception** ([rule 10](#restore)): it is the user asking to put a whole backup back, so it replaces every
   installation's device files, one the backup does not hold included.

   The live copies were already backed up when Elite-Intel started ([rule 12](#backups)).
2. If the installations hold different versions of `DeviceMappings.xml` or the `.buttonMap`s, the user reconciles
   them: keep one over another, or merge. A merge with conflicting line items, such as two names for one button,
   needs a decision on each of those items.
3. Once every decision is made: back up everything — **this backup is kept forever**, as the user's files before
   BindForge ever wrote anything (2026-10-23, Alan) — then create the working copy — every `.binds`, the
   `StartPreset.N.start`, **one** `DeviceMappings.xml` and **one** of each `.buttonMap`, reconciled — and mark
   first-time startup complete.
4. From then on, **the working copy is the source of truth.**

**Settled with Storage S2b's sources (Alan, 2026-10-25):**

- **The flag is `bindforge_activation.activated_at`**, set in the step that writes the working copy and cleared by
  Run first-time startup again; the "before BindForge" backup's `backup_folder` is dropped. **The migration clears
  it**, so every tester already set up the old way goes through first-time startup again.
- *Replaced 2026-11-03 by [read only while an installation is missing](#an-installation-missing--read-only-2026-11-03-alan):*
  **Every known installation unreachable stops it** (Alan, 2026-10-25, from the S2b review) - a drive unplugged. BindForge
  names them, says to plug the drive in or Remove the installation, and leaves the flag unset; a working copy with no
  device files would otherwise go over theirs when they come back. With no installation known at all it runs, with
  the binds and preset files alone.
- **Offered at every click on the BindForge tab while the flag is not set** — abandoning it asks again at the next
  click, not only the next run.
- **A new player with no bindings folder yet is not taken through it.** BindForge says the setup check's words
  (`speech.setup.noBindingsYet`), leaves the flag unset, and offers it again at the next click: nothing of theirs
  exists yet, and [CREATE](#create--a-new-players-first-binds-file) needs the folder.
- **No `.buttonMap` is written at Elite-Intel startup any more** — the startup generation is removed here, ahead of
  Storage S4, because once the flag is set it would write to the game's files unasked (principle 4). Default labels
  come back at APPLY in S4.

**Settled from Krondor's review (Alan, 2026-10-27; Storage S4R):**

- **First-time startup writes the installations it read, and no other.** An installation found only while the dialogs
  are open (a drive plugged in, say) is neither backed up nor written, and one that was unreachable when the files were
  read stays out of the write even if it is back by then. Either one is reconciled later, as rule 1 says. **RESTORE** also
  writes the installations its confirmation names.
- **"Reachable" means one thing everywhere:** the stored list does not mark the installation missing, and its
  `ControlSchemes` folder is there. First-time startup, the backup, the write and RESTORE all use this test
  (`BackupSources.Installation.reachable`). An installation folder with no `ControlSchemes` in it counts as unreachable.
- **If the flag cannot be set after the write,** the user is told the files were written and that first-time startup
  will be offered again at the next click. RESTORE already says this (rule 10). It never says *"Nothing was written"*.

## An installation missing — read only (2026-11-03, Alan)

[Principle 13](principles.md)'s first use. It replaces the S2b rule above that every known installation unreachable
stops first-time startup, with its message and its code.

- **Checked at every Elite-Intel start.** An installation is missing when it fails the one *reachable* test above:
  the stored list does not mark it removed, and its `ControlSchemes` folder is there. Nothing checks again while
  Elite-Intel runs.
- **Before first-time startup has ever completed it does not apply.** The very first setup runs with the
  installations that are there. After that — a later run of first-time startup included — any one listed
  installation missing puts BindForge in **read only**. It is never taken off the list for being missing.
- **The user is warned at start**, once: BindForge is read only because an installation of Elite Dangerous is
  missing, naming it.
- **Read only turns off every change; every screen stays viewable**, down to its dialogs. The Bind Editor's bind
  dialogs open and show what is bound, with nothing changeable; the Alias Designer's name dialogs open, the names not
  editable; the Preset Editor shows its four lines, the dropdowns locked. CREATE, RESTORE and first-time startup are
  off. The backup at every start still runs (it only reads), and Game Install Locations stays usable.
- **The bar carries it.** No edits exist while read only, so the message takes the bar's left side in warning text
  colour, naming every missing installation and the ways out — plug the drive in, or **Remove** it in Game Install
  Locations — and one **OVERRIDE** button takes the place of DISCARD and APPLY. The bar is above the tabs, not an
  editor, so it may name installations ([principle 12](principles.md)).
- **OVERRIDE** asks first (`HudConfirmDialog`): editing is turned on, and when the missing installation is back it
  is compared and reconciled at the next Elite-Intel start, as rule 1 says for one that comes back. An override
  lasts until Elite-Intel closes; the next start checks and warns again. After it the bar is the normal DISCARD /
  APPLY, and APPLY writes to the installations that are there.

**Settled with Storage S14a's sources (Alan, 2026-11-05):**

- **Three slices.** S14a: the state, the warning at start, the bar and OVERRIDE, the refusal, and what writes turned
  off. S14b: each editor screen and its dialogs viewable with nothing changeable. S14c: the startup notice.
- **"First-time startup has ever completed" is: a working copy exists** - its `Bindings` folder is there
  (`WorkingCopy.exists`). Only first-time startup and RESTORE make one, and Run first-time startup again leaves it, so a
  user who set up before is read only even with the flag cleared; a new user is not.
- **The check does not run while read only** - its three choices all write. Nor does first-time startup, or the offer
  of RESTORE for a working copy that cannot be read. *Decided while building:* they run as soon as the user presses
  OVERRIDE, on screen, rather than at the next click on the BindForge tab.
- **Remove in Game Install Locations takes that installation off the missing list**, and read only ends when none is
  left, without a restart. Plugging the drive back in is noticed only at the next start, so the bar says to restart.
- **Run first-time startup again is greyed while read only**, since first-time startup is off. RESTORE and Browse for
  Backup File are greyed too, and RENAME in the Bindings File section; the dropdown and LOAD stay, for viewing. **Backup
  Now, deleting a user backup and the backup settings stay usable** - they touch no game file.
- **The warning at start is spoken and logged once**, as rule 15's line is, naming each missing installation; the bar
  carries it after that. No dialog.
- **CREATE is not shown while read only.**
- **The edits refuse every change and write while read only**, whichever screen offered it, and the screen says
  *"BindForge is read only while an installation is missing, so nothing was changed."* The screens' own controls are
  turned off in S14b; the refusal stays behind them.
- **Worked out once a run, at the first ask**, from the stored list after this run's detection; a fault working it out
  is logged and BindForge is not read only - every write still backs up first.
- **First-time startup's summary names every installation it leaves
  out**, and with none read says the working copy will hold no device files, rather than that the installations match (S14a review, 2026-11-06). With S2b's stop gone, it is the last thing said before the working copy is made without them.

Built in Storage S14a as `bindforge.storage.ReadOnlyMode`, `Edits.ReadOnlyException`, the bar's line and OVERRIDE in
`ui.screen.bindings.ApplyBar`, and the warning in `AppController`.

**Settled with Storage S14b's sources (Alan, 2026-11-06):**

- **The bind dialogs are the same dialogs, opened view only** - not a second dialog to show a control. Both slots'
  cards, the keyboard map and the controller tabs show as usual; key capture and live controller input are off, a pick
  does nothing, and CLEAR, swap and SAVE are greyed. The axis dialog the same: invert, dead zone, throttle range, CLEAR
  and SAVE greyed. BACK and Esc close.
- **The grid's CLEAR (Used tab, Anomalies) and AUTO FIX (Missing tab) are inert** - greyed, saying why on hover, a click
  doing nothing - and **AUTO-ASSIGN is greyed**. Settings mode's numbers and sliders are greyed. All of them follow read
  only ending without a restart.
- **The Device Editor opens and its input tabs light as usual**, so the user can still find which input is which; the
  name and label boxes are not editable (still readable), and SAVE, CLEAR / RESET LABELS / UNDO and NAME are greyed.
- **Onboarding and the identity question are held while read only, not spent.** Nothing offers them - not the
  automatic offer, a controller plugged in, nor NAME - and the refresh after read only ends (OVERRIDE or Remove) offers
  them as it would have. **CHECK AGAIN is greyed**, since offering them is its one job; the screen still reads again on
  hot-plug.
- **One line says why, in warning text colour**, at the foot of the bind dialogs and the Device Editor: *"Read only
  while an installation is missing - nothing here can be changed."* (`bindings.readOnly.viewOnly`). Every greyed control
  carries S14a's tooltip.
- **The Preset Editor's locked dropdowns are P4's**, built with that screen.

**Settled for Storage S14c, from Krondor's S14a review (Alan, 2026-11-06):**

- **The warning at start names each missing installation by its store alone** - Steam, Epic, Frontier, Oculus, and
  *a manual installation* for one added by hand - never its path, which is no use spoken. The bar and the log keep the
  path.
- **Remove ends read only the way OVERRIDE does:** when the last missing installation is removed, what was held back -
  the check, first-time startup, the offer of the newest backup - runs straight away, not at the next click on the
  BindForge tab.
- **The *changed outside* line is not said while read only.** The read-only warning covers it, and the check it points
  to is off.

**Settled with Storage S14c's sources (Alan, 2026-11-06)** - the notice at Elite-Intel start:

- **Spoken and logged, a line each, no dialog**, once a run, in a fixed order: read only; changed outside BindForge or a
  newer version (rule 15), not while read only; a binds file named like a factory one. Nothing is blocked or asked.
- **The binding warnings stay Elite-Intel's** (`KeyBindCheck`: missing bindings, conflicts, reserved keys), said where
  they were, before the notice.
- **The missing controller stays the Alias Designer's**, said the first time it is opened in a run
  ([alias-designer.md, A13](alias-designer.md#a-missing-controller-is-worth-exactly-one-warning--settled-2026-09-24)).
- **A binds file named like a factory one is judged on the working copy**, once one exists, against the reachable
  installations' factory files - what the Bind Editor warns of. Before first-time startup nothing is said: the live
  files are not yet BindForge's.
- *Decided while building:* that line is said while read only too - it names a file, not a write, and the read-only
  line comes first. Two missing installations from one store are named once (*Steam*, not *Steam, Steam*).

Built in Storage S14c as `bindforge.storage.StartupNotice`, started by `AppController` after `KeyBindCheck`; it replaces
`StartupCheck.runInBackground` and `ReadOnlyMode.warnInBackground`. Read only ending - OVERRIDE or Remove - is heard by
`BindingsTabPanel`, which runs `FirstTimeStartupFlow.offerIfNeeded`; `ApplyBar.onOverride` is gone.

Built in Storage S14b: a view-only flag on `AssignKeyboardBindingDialog` and `AssignAxisDialog`, `OffCell` for the grid's
inert actions, `Opening.readOnly` on `DeviceEditorDialog`, and `BindForgeReadOnlyChangedEvent` heard by
`BindingProfilePanel` and `AliasDesignerController`.

## Opening BindForge

5. The first time the user clicks the BindForge tab in an Elite-Intel session, [the check](#when-a-live-copy-differs)
   runs. A session flag stops it from running again in that session. *Until 2026-10-23 a backup was taken here too;
   the startup backup ([rule 12](#backups)) replaced it.*

   **After the bindings folder changes, the check runs again at the next click on the BindForge tab** (Alan,
   2026-11-11). Until then a binding changed in the game after the first click was not seen until the next APPLY or
   the next Elite-Intel start: Alan changed one in the game's controls with BindForge already opened, and nothing
   asked. Now, when `BindingsMonitor` - which already watches the bindings folder for the assistant's keys - sees it
   change after the check last ran, the session flag is cleared, and the next click on the BindForge tab runs the
   check as the first click does. **No dialog opens while the user is on another tab, or while they stay on
   BindForge** (principle 5): the check waits for the click. BindForge still does not watch the files itself, and
   the check at APPLY is unchanged.

   *To confirm at the slice's sources (Storage S17):*
   - which changes count - the monitor reacts only to names ending `.binds`, so a change to `StartPreset.4.start`
     alone, or to an installation's device files, clears nothing;
   - BindForge's own writes change the folder too (APPLY, RESTORE, the check's write), and would send the next
     click through a check that finds nothing;
   - after **Not now**, whether the next click asks again or only a further change does;
   - a user who is on the BindForge tab when the game saves, and does not leave it;
   - edits not applied put the first click's check off to the next click, and read only turns it off - presumably
     the same here.

## Editing and applying

6. Alias Designer, Bind Editor and Preset Editor hold the user's changes on screen, as edits not applied. They never
   write a file themselves.
7. APPLY:
   - run [the check](#when-a-live-copy-differs);
   - back up everything;
   - save the edits into the working copy and write the working copy to the live copies, in one step. The one
     `DeviceMappings.xml` and the `.buttonMap`s go to every installation.

   **There is one APPLY and one DISCARD, above BindForge's four tabs and beneath Elite-Intel's main tabs** — no tab
   has its own. APPLY pushes the whole working copy, with every edit not applied from any tab; DISCARD throws away
   every edit not applied. Both are lit only when there are edits not applied, and greyed out otherwise
   (2026-10-24, Alan).

   **A binds file renamed in the Bind Editor** goes out under its new name, and APPLY removes the old-named file
   from the live bindings folder, after the backup (2026-10-24, Alan). **It is written as a new file and the old one
   removed, not renamed in place** (2026-11-03, Alan, Storage S12): the edits hold only what the files will be, with
   no record of which was which. So a rename that changes only case, `-` or `_` is refused in the Name box - on Windows the old name and the new would be one file, and removing the old would remove the one just written.
   **That holds across renames not
   applied:** the working copy's names count too, so `Custom` to `Mine` to `custom` is refused;
   `Custom` exactly is a rename back. **A rename changes the name, not the
   bindings** (review, 2026-11-03): a binding changed in-game while it waits is asked about at APPLY like any other, and what is settled goes out under the new name.
   **If a binds file cannot be written, the preset file is not written and nothing is
   removed,** so the game is never left on a line naming a file that is not there; the report says what was held back.

   **Before anything is backed up or written, APPLY checks that every one of the four lines of
   `StartPreset.4.start` names a binds file that exists.** If one does not — or is empty — APPLY refuses as a whole
   and names the sections (2026-10-24, Alan), because the game would silently load something else.
8. APPLY writes everything it can. If a file or an installation can't be written, the user is told — *"updated 2 of
   3"* — and the check finds the missed installation and offers to put the working copy there (rule 14). A missed
   installation now differs, so it is never written over unasked: the check at the next BindForge click, or at the next
   APPLY, asks about it (Alan, 2026-10-27; at APPLY since Storage S9b, 2026-10-31;
   [How the files are written](#how-the-files-are-written--2026-10-24)). Nothing is rolled back (2026-10-23, Alan).
9. Before the user leaves BindForge with edits not applied — another Elite-Intel tab, or closing Elite-Intel — they
   are reminded, and choose **Go back**, **Discard** or **Apply** (Apply with its usual confirmation and backup).
   Nothing is selected and Enter chooses nothing; Esc and the close glyph are Go back (2026-10-23, Alan).

   **Settled with Storage S7's sources (Alan, 2026-10-30):**

   - **Apply leaves only once the edits are gone.** It is the bar's APPLY, confirmation and report included; after
     the report the user leaves. If APPLY is cancelled, refused or fails before writing, the user stays, with the
     edits on screen - otherwise a refused preset file would close Elite-Intel and lose them.
   - **Discard asks nothing more.** The leave question is its confirmation.
   - **While a write runs** (APPLY, RESTORE, first-time startup), **closing Elite-Intel is refused** and the user is
     told the files are being written; switching to another tab is let through, since the edits are already being
     written. Closing part way would leave installations unwritten until the check.
   - **Update and restart** (`HudUpdateButton`) closes Elite-Intel without asking - the user asked for it, and it is
     not BindForge's screen. Left out of S7.

   Built in Storage S7 as `ui.screen.bindings.LeavePrompt`, asked from the main tabs' selection and the window close
   in `AppView`; `HudConfirmDialog.showWithoutDefault` is the dialog with no default button.

## How the files are written — 2026-10-24

Settled with Storage S2a's sources (Alan, 2026-10-24). Built as `bindforge.storage.WritePath`, `WorkingCopy` and
`LiveCopies`.

- **One write path.** First setup, APPLY, RESTORE and the check's reconcile all write the same way: back up, save the
  set as the working copy, write it to every live copy. If the backup cannot be taken or the working copy cannot be
  saved, it stops, and no live copy is written. The working copy is saved first so that it always holds what BindForge
  last wrote: a live copy the write missed then differs from it, and the check or the next write puts it right (rule 8).
- **The backup and the write cover the same places, and one write runs at a time** (review, 2026-10-24). The
  installations are found once per write, and that list is both backed up and written, so nothing is written that was
  not backed up (rule 12). A second write waits until the first has written its last live copy, so the working copy
  and the live copies always hold the same set.
- **The working copy is written in one step.** The new set is written into a folder beside it, marked complete, then
  swapped in. A write that stops before the mark leaves the old set; one that stops after it is finished by the next
  read or write. The working copy is never half one set and half another.
- **A live file that already holds the same bytes is not rewritten.** Bytes, not values: rewriting a file that differs
  only in line endings costs nothing, and what counts as a change is the check's question (rule 14), not the write's.
- **A missing bindings folder is reported, never created** — creating it would put the files where the game never
  reads them. A missing `DeviceButtonMaps` folder inside an installation is created.
- **File names are matched ignoring case, on every platform: two names that differ only in case are one file**
  (Krondor and Alan, 2026-11-07, settling Storage S13's D3). The game reads file names the Windows way, under Proton on
  Linux too: Alan renamed a binds file (`dualvirpildawntreadercopy.4.2.binds`), `DeviceMappings.xml` and `LVWAP.buttonMap` to
  lower case, and the game loaded them and wrote its own edits back (2026-11-06). Linux itself allows `Custom.4.2.binds` and
  `custom.4.2.binds` side by side, and Proton then breaks (Krondor). So:
  - **Every name match ignores case** - the check pairing a live file with the working copy's, the working copy's own
    names, a preset line naming a binds file, the device files, the plain `.binds` of S13b.
  - **BindForge never leaves two files whose names differ only in case.** A file is saved under whatever spelling it is
    given - the user's spelling is kept, never forced to one case (Alan, 2026-11-07); if a file of the same name in another case is already there, it is replaced, and the spelling saved is the
    one left - the file there is renamed to the spelling given, then written. *A rename that fails* is reported and the
    file written under its old spelling, never as a second file beside it; the next write tries the rename again (Alan,
    2026-11-07, Storage S13aR D1).
  - **Only the name ignores case.** The kinds - `.binds`, `.buttonMap`, `StartPreset.N.start` - are matched exactly:
    the game needs the preset file's name, and players do not rename these (Alan, 2026-11-07).

  **Settled with Storage S13a's sources (Alan, 2026-11-07):**

  - **D1: the rule lives in `bindforge.io.FileNames`** (`sameFile`, and `ONE_FILE` for a map keyed by file name),
    moved there from `bindforge.storage` so the BindName list and Elite-Intel's own loader can use it.
  - **D2: a live file whose name differs from the working copy's only in case is that file**, compared by value as
    usual: equal values ask nothing, and the next write leaves the working copy's spelling. A hand rename gets no guard
    of its own (principle 13); the game loads either spelling.
  - **D3: two spellings of one file** - only where the file system heeds case, made outside BindForge - are compared as
    the one spelled as the working copy's, else the first in name order. **Every write removes the others** after the
    backup, and the report names them; a read-only one is kept and reported, as any removal is. A backup holding two is
    restored with the first in name order; the backup still holds the other.
  - **D4: a `.buttonMap` follows the same rule** - the working copy's spelling is left, on Windows and Linux alike.
  - **D5: `DeviceMappings.xml` is found by listing its folder, ignoring case**, not by path - on Linux a
    `devicemappings.xml` would otherwise read as missing, and the write would put a second file beside it. The write
    leaves the game's spelling.
  - **D6: Elite-Intel's own loader** (`BindingsLoader`, the keys the assistant presses) finds the file a preset line
    names ignoring case too. *Since Storage S13aR (Alan, 2026-11-07, D7)* it reads a file's BindName as the rest of
    BindForge does (`FileNames.bindName`): `Custom.Old.4.2.binds` is BindName `Custom.Old`, not a file of preset
    `Custom`, and a name that is not `<name>.<major>.<minor>.binds` - a plain `Custom.binds`, a Windows copy
    `Custom.4.2 (2).binds` - is no preset's file, as [principles.md *Words*](principles.md#words) defines a binds
    file. Until then the loader matched any name starting `<preset>.`.
  - **Which file the loader reads (Alan, 2026-11-08, from Krondor's review of S13b), amending D6:** the true binds
    file - `<name>.<major>.<minor>.binds` - that `StartPreset.4.start` names, from the bindings folder. **A plain file
    never counts as a preset's file.** When a line names a preset with no true binds file in the bindings folder, the
    loader reads **that preset's factory binds file from an installation's `ControlSchemes`**, found the way BindForge
    finds factory files: reachable installations only, the first that holds it (rule 16, *Settled with Storage S11's
    sources*). **The newest-file fallback goes** - no file is ever read because it is the newest. **`sectionsNotIn`
    follows the same rule:** a section counts as read when its preset is the one the loader read, from the bindings
    folder or from `ControlSchemes`.
    *Why a plain file can be ignored:* the game converts one the moment it is picked. Alan put
    `keyboardandmouseonly2.binds` in the bindings folder, chose it for General in the game's controls, and on Apply the
    game at once wrote `keyboardandmouseonly2.4.2.binds` ([testing item 28](../../00-overview/testing-required.md),
    measured 2026-11-08). So the game never leaves a preset line naming a preset that has only a plain file; only a
    hand edit of `StartPreset.4.start` would, and that gets no guard (principle 13).
    **Settled with Storage S13c's sources (Alan, 2026-11-08):**
    - **The lines are taken in order, General first; the order matters.** Each resolves to its true file, else its
      factory file; a line resolving to neither (no reachable installation holds that factory file) passes to the next,
      and with none left nothing is read and it is logged - the assistant then says its bindings could not be read, as
      it did when no file was found.
    - **The installations come from `bindforge.install`:** the one reachable test moved to `GameInstallation.reachable`
      (`BackupSources` asks it), and `InstallationRegistry.reachableControlSchemes` lists their `ControlSchemes` in the
      stored list's order. `bindforge.io` and `bindforge.install` now depend on each other (Q2 (a)).
    - **A factory file is found by its file name**, `<preset>.binds` at the top of `ControlSchemes`, ignoring case - not
      judged by content: Frontier's files carry their name as `PresetName` (Q3 (a)).
    - **Two true files of one preset read the highest version in the name**, then name order. Only a hand rename makes
      two - the game upgrades a file in place, and every file has been `.4.2` since Odyssey - so it gets no guard of its
      own (principle 13; Q4).
    - **`listAllBindsFiles` stays for `SetupCheck`**, which asks only whether the folder holds any `.binds` (Q5).
    - *Planned as its own slice (Alan, 2026-11-08):* when the four lines name different binds files, the user is asked
      which to use, and whether that file should be the only one the game uses - yes sets all four lines to
      it, no uses it and warns that functions may not work when the game's binds and the assistant's differ. See the
      [brief](../../05-build-plan/storage.md#slices).

    **Settled with Storage S13f's sources (Krondor and Alan, 2026-11-10):**
    - **A dialog, never a conversation with VEGA.** It can come up before an LLM is set up, so the question and its
      YES / NO are buttons.
    - **Asked at the first BindForge click of a run, with BindForge's other questions** - after first-time startup and
      the check - not at Elite-Intel start, where a player who has never seen the app, perhaps a veteran with years of
      custom binds, would meet it with no context and dismiss it. Once a run, answered or not; again the next run while
      the lines still differ.
    - **Nothing is stored, and there is no *do not ask again*.** A box that still asks when the lines change would be a
      promise the app cannot keep. The file chosen is what the assistant reads for that run, while a line still names
      it; otherwise the lines are taken General first, as above.
    - **Offered:** each BindName the lines name that resolves to a file - its true binds file, else its factory one -
      with the sections naming it; names differing only in case are one. With fewer than two, nothing is asked.
    - **Yes** sets all four lines to it, as the Preset Editor's SET ALL TO SAME does (`PresetEdits.change`), and applies
      it at once - the answer is the user asking (principle 4) - through APPLY's check, backup and report, without
      APPLY's own confirmation.
    - **No** keeps the lines: the question says what that costs, and the log says it in ERROR for the support bundle;
      nothing is spoken.
    - **Only which file is asked** while read only, before first-time startup has made a working copy, and while edits
      not applied are on the bar (APPLY would write them too).
    - **Until the user answers, the warning at Elite-Intel start stays** (`KeyBindCheck`), its line for sections on
      another preset now also saying to open the BindForge tab to choose.

    Built in Storage S13f: `BindingsLoader.splitChoices` and the chosen file read first; `SplitPresetChoice`;
    `BindingsMonitor.reread`; `ui.screen.bindings.SplitPresetFlow`, `SplitPresetDialog`, `ApplyBar.applyAsked`, and
    `FirstTimeStartupFlow`'s next question at the click.

    Built in Storage S13c: `BindingsLoader` (`getLatestBindsFile` - `bindsFileToRead` since Storage S13eR - `sectionsNotIn`, a constructor taking the
    `ControlSchemes` folders), `GameInstallation.reachable`, `InstallationRegistry.reachableControlSchemes`.
    *Until 2026-11-08 a line whose preset had no file in the bindings folder passed to the next, and when no line had
    one the newest `.binds` in the folder was read, a plain file included.*
  - *Decided while building:* a file's `PresetName` that differs from its file name only in case is no mismatch - Alan's
    test file, renamed to lower case with its `PresetName` as it was, is offered and loaded. A set that holds two
    spellings of one file is refused; a working copy holding two, made by hand, cannot be read, so the newest backup is
    offered (rule 10). On Windows a move between two spellings does nothing, so a respelling goes through a name of its
    own (`.respelling`) in two steps. A set's file names are held one file each wherever a set is built - the working
    copy's, one installation's device files, first setup's - and two spellings handed in are refused, never one kept
    (Alan, 2026-11-07, Storage S13aR D2, `FileNames.oneFileEach`).

  Built in Storage S13a: `FileNames.sameFile` / `ONE_FILE` / `oneSpelling`; `ConfigurationFiles` keyed ignoring case;
  the check's pairing in `BindingsComparison`; `LiveCopies.Writing.writeAll` renaming another spelling to the set's;
  `GameInstallation.findDeviceMappingsIn`; `BindNameList.find`, `BindsContent`, `BindNameCatalog`;
  `BindingsLoader.isFileOfPreset`.
- **A `.buttonMap` the working copy lacks is removed**, except Frontier's shipped `VPCPanel` and `VPCThrottle`. An old
  spelling of a kept one (`lvwap.buttonMap` for `LVWAP.buttonMap`) is that same file (the case rule above), renamed to
  the working copy's spelling. *Until Storage S13a it was kept as it was on Windows and removed on a file system that
  heeds case, the kept spelling written beside it.*
- **A working copy with no `DeviceMappings.xml` leaves each installation's alone** (review, 2026-10-24). Without that
  file the game loads no bindings at all ([overview](overview.md#why-devicemappingsxml-is-not-cosmetic--reclassified-2026-09-08)),
  so the write never removes one — unlike a `.buttonMap`, which holds only labels and is removed when the working copy
  lacks it.
- **APPLY does not write an installation whose device files differ from the working copy** (Alan, 2026-10-26, from
  the S2b review and S4's sources). Its `DeviceMappings.xml` and `.buttonMap`s are compared with the working copy's
  **by value** - the device elements, and each map's labels - before the edits go on top; one that differs is left as
  it is and reported as *differs, not written until it is reconciled*, since who knows what the user is doing to that
  installation (rule 1). The bindings folder and every other installation are still written. Reconciling it is the
  check's (rule 14, Storage S9). First setup and RESTORE write every installation, as they always have: first setup
  has just reconciled them, and RESTORE is the user asking to replace them (rule 10). **An installation a write
  missed differs too**, so it also waits for the check (built in Storage S4a, 2026-10-26). A file there that
  cannot be read counts as differing - it could hold anything of the user's. An installation with no
  `DeviceMappings.xml` does not differ here: APPLY writes one. *The check names it as a reset since Storage S9a
  (Alan, 2026-10-31; rule 14, Settled with Storage S9's sources).* **Since Storage S9a the first BindForge click in a
  session reconciles an installation that differs, and since Storage S9b (2026-10-31) so does the check at APPLY**:
  APPLY now leaves alone only an installation its check could not read at all, and one it never read - unreachable
  when the check ran, back by the write - that differs (rule 1). *Until S9 nothing put the working copy on a missed
  installation; that gap was accepted while V1.2 was built (Alan, 2026-10-27: no catch-up record, no per-place
  overwrite before S9).*

  **Settled from the S4a review (Alan, 2026-10-27):**

  - **The report promises the next APPLY only for the bindings folder.** An installation a write missed part-way, or
    one that kept a read-only `.buttonMap`, is told it now differs and is **left as it is until the check reconciles
    it** - never *"written by the next APPLY"*, which no longer holds for an installation. A kept read-only map's line
    says to clear its flag if the user wants BindForge to remove it, and no longer promises *"the next write"*.
  - **An installation left alone is told only why:** *"…so nothing was written to it, to keep what is there safe."* No
    instruction to reconcile - nothing in the app can until S9.
  - **"By value" compares the elements' names and VID/PIDs and each map's labels - not an element's other
    children**, such as `<SupportsIcons>`. They are Frontier's and the user does not edit them, so one changed by hand
    inside an installation is overwritten at the next APPLY without a word. Accepted.
  - **A working copy BindForge cannot parse stops APPLY before the backup**, rather than leaving every installation
    alone as "differs": it is BindForge's own file, so that is a broken invariant, not the user's file to protect. An
    installation's file that cannot be parsed is logged and still left alone.
  - **The default labels are worked out as APPLY runs**, from the controllers attached then - not held from the
    screen's last refresh ([alias-designer.md](alias-designer.md#built-on-the-working-copy--2026-10-26-storage-s4a)).
- **A `.binds` the working copy lacks is left in the bindings folder.** Removing an old-named one is a rename's
  (rule 7, Storage S12).

**Built 2026-10-26 (Storage S4a):** `bindforge.storage.Edits` holds the edits not applied, in memory, shared by the
tabs; every write through `WritePath` hands it the set it wrote, so APPLY, RESTORE and first-time startup all leave no
edits on screen. `Apply` is the one APPLY, `ui.screen.bindings.ApplyBar` the bar above BindForge's tabs, and
`DeviceFilesComparison` the by-value comparison above. Each Alias Designer action is a change to the edits
(`DeviceEdits`) - see [alias-designer.md](alias-designer.md#built-on-the-working-copy--2026-10-26-storage-s4a).

## Restore

10. RESTORE: choose a backup. It backs up what is there now, then puts the whole backup back and replaces the working copy.
    No choices (2026-10-24, Alan). The `.binds` and `StartPreset.4.start` go to the bindings folder; the device files
    go to every installation on this machine now, as one set — the backup's, or, if the backup holds different device
    files for different installations, the set the user reconciles as in rule 2. **An installation the backup does not
    hold - one added since - is replaced too, without a reconcile** (Alan, 2026-10-25, from the S2b review): RESTORE is
    not automatic, it backs up what is there first, and its confirmation names every installation it replaces. Its
    confirmation says any edits on screen are thrown away. (2026-10-22, Alan: the working copy is replaced too, otherwise it would no longer be what
    BindForge last wrote, and the next check would ask the user about their own restore.)
    **If the working copy cannot be read,** BindForge says so and offers the most recent backup to restore from
    (2026-10-23, Alan).

    **Settled with Storage S3's sources (Alan, 2026-10-25):**

    - **A read-only live file stays read-only.** Players set the flag to stop the game resetting a binds file, and a
      file with it cannot be replaced, so every write - RESTORE and APPLY alike - clears it, writes, and sets it
      again. The flag is the live file's as it is now; backups do not record it. **On Linux it is the owner's write
      permission**, what Wine shows as read-only, kept the same way - otherwise the new file would come back writable
      (Alan, 2026-10-25, from the S3 review). **A read-only `.buttonMap` the working copy lacks is kept, on every
      system** - on Linux the delete would succeed - and the report says, in the user's language, to clear its flag
      (Alan, 2026-10-25, from the second review). *Accepted:* on Linux a protected file another user owns - one made
      through `sudo` - cannot have its protection lifted, so it is not written, and the report says so.
    - **A backup missing a half is refused, naming what is missing:** one with no binds or preset file, or one with
      no device files while this machine has installations. Put back, it would leave the working copy without binds,
      or remove every `.buttonMap` the user has.
    - **RESTORE before first-time startup is complete marks it complete**: it leaves the working copy and the live
      copies matching, as first-time startup does, and first-time startup run afterwards would take a "before
      BindForge" backup of files BindForge had already written.
    - **One confirmation.** When the backup's installations differ, the reconcile's summary is RESTORE's
      confirmation; otherwise RESTORE's own. Either names every installation it replaces, marks any not found, and
      says that edits on screen are thrown away.
    - **The working copy is read at every click on the BindForge tab** once first-time startup is complete; one that
      cannot be read, or is missing, is said so with the newest backup offered - **Restore** or **Not now**. **With no
      backup at all, first-time startup is offered instead**, from the live files - **Set Up Again** or **Not now** -
      the one way out once Run first-time startup again is gone (Alan, 2026-10-25, from the S3 review).
    - **If first-time startup cannot be marked complete** after a restore, the report says it will be offered again at
      the next click (review, 2026-10-25).
    - **No bindings folder: RESTORE refuses before the backup**, in the setup check's words
      (`speech.setup.noBindingsYet`), since the folder is never created (rule 7's write) and half a restore would follow.
    - **RESTORE publishes no `BindingsUpdatedEvent`** - `BindingsMonitor` sees the bindings folder change
      ([overview](overview.md#bindforge-must-not-publish-bindingsupdatedevent)).
11. One backup system. No versions, no history per file. **Four kinds of backup, each kept by its own rule**
    (2026-10-24, Alan), applied when Elite-Intel starts:
    - **daily** — kept by age: 30, 60, 90 or 180 days, the user's choice, 30 by default;
    - **before changes** — kept by count: the newest 100 by default, the user's choice from 10 to 1000;
    - **user** (Backup Now) — kept until the user deletes them;
    - **first setup** — the one first-time startup takes before creating the working copy (rule 3), kept forever.

    *Why two rules* (Krondor's review, 2026-10-24): age alone deletes every good backup when a user is away longer than
    the limit; a count alone loses last week's good copy to one evening of applies. *Until S1b (2026-10-24) every backup was
    deleted by age, 30 days by default.*

## Backups

12. A backup is a zip of the live copies, named with its date stamp, in Elite-Intel's BindForge backup folder — every
    backup a zip, so there is one format to restore (2026-10-24, Alan; folders were tried earlier that day). **A
    before-changes backup is made before every write to the live copies** — APPLY, RESTORE (of what is there now) —
    and **before a live copy replaces the working copy, the working copy is backed up the same way** (2026-10-23 and
    2026-10-24, Alan). Backup Now makes a **user** backup.

    **The backup folder is the record** — the list of backups is the folder's listing, sorted by the date stamp in
    each name, with no database table — and **it is in Elite-Intel's own data folder, never in a game folder**, where
    a game update would delete it (2026-10-24, Alan).

    Every backup holds the full set of live copies from every installation, even when they match — no comparison
    first (2026-10-24, Alan).

    **The first time Elite-Intel starts each day, it backs up all the live copies, in every installation, silently,
    in the background — the daily backup** (2026-10-24, Alan; every start until then) — whether or not the user ever
    opens BindForge, and before first-time startup has ever run. The user is not told. The working copy is not
    included: after BindForge was last used it matches the live copy. If the backup fails, it is written to the
    application log and tried again at the next start, the same day included (2026-10-23 and 2026-10-24, Alan). Once a
    day is enough because BindForge writes only at APPLY, and every APPLY takes its own backup. It cannot be switched off (2026-10-24, Alan). **The one exception to "not told": a new player
    with nothing to back up** — the game creates its bindings folder only once they have played through the training,
    so until then there is nothing of theirs to protect, and Elite-Intel cannot operate their ship either. They are
    told at every start, spoken and logged, by Elite-Intel's setup check rather than by the backup (2026-10-24, Alan;
    [file-manager.md](file-manager.md#how-the-backup-folder-is-laid-out)). *Why:* a user who lost their binds can be told where their backups are, or restore one in
    BindForge; and if a BindForge bug ever destroys someone's files, there is a backup from before it ran.

## When a live copy differs

13. Don't edit bindings in BindForge and in the game at once. BindForge is not responsible for problems or data loss
    caused by doing so. **APPLY's confirmation always says so** (2026-10-23, Alan): *"Close the game's controls
    screen before applying. Changes made in BindForge and in the game at the same time can be lost."*
14. **The check:** every live copy, in every installation, is compared with the working copy **by their values, never
    by date**. Any difference means the live copy changed since BindForge last wrote it — an edit in the game, or a
    game update adding new bind slots or other new parts. The user is warned and chooses one of three:
    - overwrite the live copy with the working copy;
    - merge — every line item where the live copy and the working copy differ is shown, and the user picks which to
      keep (2026-10-22, Alan: no record of what each side started from is kept, so the merge cannot tell which side
      changed a line; for a game update that adds slots the list is just the new slots. Revisit if users complain);
    - overwrite the working copy with the live copy.

    **Device files are reconciled the same way as first setup** (2026-10-23, Alan). When an installation's
    `DeviceMappings.xml` or `.buttonMap`s no longer match the working copy, the user reconciles them as in rule 2,
    with the working copy as one of the sides: keep the working copy, keep an installation's version, or merge and
    decide each conflict. The result becomes the working copy and goes to every installation after the backup.

    **A reset is named** (2026-10-23, Alan): when a live copy matches Frontier's factory file, the warning says a
    game update probably reset it, offers overwriting the live copy with the working copy first, and asks *"Are you
    sure?"* before the live copy replaces the working copy.

    **At APPLY, with edits on screen** (2026-10-23, Alan): the check settles the working copy first; APPLY then puts
    the on-screen edits on top and writes everything out. *For the device files the edits are instead the user's side
    of the reconcile (Alan, 2026-10-31, Storage S9b; Settled with Storage S9b's sources, below).*

    **A newer version is reconciled, never overwritten** (Krondor, 2026-09-24; Alan, 2026-10-23). When a live copy is
    a newer version than the working copy — `.4.2.binds` becoming `.4.3.binds` — "overwrite the live copy with the
    working copy" is not offered. *Only the file name's version counts since Storage S10 (Alan, 2026-10-31): a newer
    version on the XML root under the same name is an ordinary details row - below.* If the newer live copy holds every binding the
    working copy has, unchanged, the user is asked to approve it becoming the working copy, under its new name. If
    not, the user reconciles: the new slots are inserted into the working copy, every other difference is decided
    line by line, and the working copy is renamed to the new version. Either way the new slots are listed, so the
    user knows to bind them. See [overview](overview.md#when-a-game-update-brings-a-newer-version--2026-10-23).

    Large changes Frontier makes to anything other than `.binds` would need a BindForge code fix.

    **Settled with Storage S8's sources (Alan, 2026-10-30)** - the check for the binds and preset files; device
    files are Storage S9, a newer version S10 (below):

    - **S8 is two slices.** S8a: the comparison, the check at the first BindForge click with its three choices, merge
      and the reset, and the line at Elite-Intel start. S8b: the check at APPLY, the edits put on top of the settled
      working copy. The first click never has edits on screen - they start empty each run.
    - **A binds file on one side only is a difference.** Keeping the working copy's side removes a live-only file
      from the bindings folder, after the backup, which holds it; keeping the game's side adds it to the working copy.
      A file the live folder lacks is the same the other way round. Either way the two match afterwards, so nobody is
      asked about it again - it is how the game's own new `Custom.4.2.binds` reaches the working copy. This is the one
      write that removes a `.binds`, besides a rename's (rule 7), and only when the user chose it.
    - *Until Storage S10 a newer version of a working-copy binds file was left out of the check and logged - the newer
      one always, the older one too when the live folder lacked it (Alan, 2026-10-31, from the S7-S8b review). S10
      replaced that - below.*
    - **A reset, for these files:** a live binds file whose every element, and its root apart from `PresetName`,
      equals by value one of the factory binds files of a reachable installation; a live preset file whose four lines
      all name factory binds files while the working copy's name one of the user's own.
    - **The dialog does not watch the files.** Just before the write the live files are compared again; if they
      changed since the dialog opened, the user is told and the merge opens again, the answers kept on every row that
      did not change (overview.md, *Reconciling BindForge Edits With In-Game Rebinds*).
    - **The working copy is backed up inside the before-the-check backup:** one zip holding the live copies, as every
      backup does, and the working copy's files under `WorkingCopy\`, which RESTORE does not read
      ([file-manager.md](file-manager.md#how-the-backup-folder-is-laid-out)).
    - **One warning for the whole set.** It names every file that differs, and each one that looks reset; the three
      choices act on all of them, and the merge's rows are grouped by file. Taking a reset file into the working copy
      - overwriting it, or a merge row answered with the game's side - asks *"Are you sure?"* first.
      **The warning is a grid since 2026-11-10 (Alan):** one row for each binds file, installation and file to move,
      with three columns - the file, what was found, what happens to it (*Your choice*, *Yours put back*, *Left as it
      is*, *Moved to Leftover User Files*). A reset row is in warning colour, a missing, unparseable or unreadable
      device file in danger colour; the grid scrolls past eight rows. Each kind of row's explanation - a missing
      `DeviceMappings.xml`, an unparseable file, a plain file with or without lines its twin lacks - is said once below
      the grid, only when such a row is there, followed by the advice and the APPLY lines as before. *Until then each
      file was a sentence in one wrapped block, the long ones repeated on every row, and the dialog cut it off.* The
      message when only unreadable installations are found (`bindings.check.unreadableOnly`) stays text.
    - **Closing the warning is Not now** (Esc, or the close glyph): nothing is written, and it is not shown again in
      that session. The check at APPLY still runs (principle 5).

    **Settled with Storage S8b's sources (Alan, 2026-10-30)** - the check at APPLY:

    - **The check runs after APPLY's confirmation** - rule 7's order: confirm, check, back up, write. The warning shows
      only when something differs.
    - **A line the edits change is not asked about.** The edit goes on top of whichever side is kept, so the warning
      and the merge leave those rows out and say how many; *Mine* there would show the working copy, not the edit. When
      every row that differs is one of them, no warning is shown.
    - **Not now at APPLY cancels the APPLY**: nothing is written, and the edits stay on screen - so the leave prompt's
      Apply leaves the user where they are.
    - **If the check cannot read the game's files, APPLY stops** before the backup, says why, and keeps the edits.
    - **One backup**, before APPLY. When the check settled anything it also holds the working copy under
      `WorkingCopy\`, as the before-the-check backup does.
    - **The preset file is checked twice**: on the edits before the warning, so a refusal comes before any merge, and
      on the set APPLY will write, since a merge can take the game's preset line.
    - **An edit wins on every line item it changed.** Where the edit and the game's change meet on one element at a
      different grain - an edited `Primary` on a button whose shape the game changed - the edit's whole element wins;
      the backup holds the game's.

    **Settled from Krondor's branch review (Alan, 2026-10-31; Storage S8R2):** elements the game added that a merge
    keeps go into the working copy's file in the order the live copy has them - until then two or more came out
    reversed, harmless to the comparison but a drift in the file's layout.

    **Settled with Storage S9's sources (Alan, 2026-10-31)** - the check for device files:

    - **S9 is two slices**, as S8 was. S9a: the device files in the check at the first BindForge click, in the line at
      Elite-Intel start, and Game Install Locations' match states. S9b: the device files in the check at APPLY, with
      the edits. *Until S9b, APPLY left alone an installation whose device files differed, and the next session's
      first click asked about it.*
    - **Each reachable installation is compared by value**, as APPLY compares it (*How the files are written*), and is
      one of: *matches*; *differs*; *reset* - its `DeviceMappings.xml` is gone, or holds none of the user's elements
      (every element a name Frontier ships) while the working copy holds at least one; *cannot be parsed*; or *cannot
      be read*. The reset test does not depend on the game version Frontier's stock list was captured from.
    - **A missing `DeviceMappings.xml` is a difference at the check**, named as a reset - without it the game loads no
      bindings. Only putting the working copy back is offered for it. APPLY's write still writes one there as before.
    - **A file that cannot be parsed** is named, and putting the working copy back is offered: the backup holds its
      bytes. **A file that cannot be read at all** is named and left as it is - nothing is written to that
      installation, since the backup could not hold the file.
    - **One warning for the whole set**, binds files and installations together, then one backup and one write
      (principle 1). *Put mine back* puts the working copy's side everywhere. *Use the game's files* takes the one
      installation whose files can be taken, or opens the reconcile when several can, so the user picks - a missing or
      unparseable file is never a side (Storage S9R, 2026-10-31); *Merge* answers the binds
      rows, then opens the reconcile. The reconcile is first setup's (rule 2), with the working copy as one side. A
      reset installation's files replacing the working copy ask *"Are you sure?"* first.
    - **The result goes to every installation the check read**, after the backup. One it did not read - unreachable
      when it ran, back by the write - is left alone if it differs from the working copy (rule 1).
    - **Compared again before the write.** An installation whose device files changed while the dialogs were open
      stops the write; the user is told and the warning opens again. *Changed* is by value, both ways: every element
      and every `.buttonMap`'s labels, a Frontier-shipped map that came or went included - unlike the comparison with
      the working copy, which a shipped map the working copy lacks does not count against (Alan, 2026-10-31, Storage
      S9R; values, never bytes - [Freshness Checks](overview.md#freshness-checks)).
    - **Cancelling the reconcile in Merge goes back to the binds rows**, the answers kept; with no binds rows to
      answer it goes back to the warning (Alan, 2026-10-31, Storage S9R).
    - *Until Storage S9b the reconcile judged losses on the working copy's binds files* (Alan, 2026-10-31, from the
      S9a review), so a device only the game's side used could be removed without being named. S9b builds the reading
      from the settled binds files - below.

    **Settled with Storage S9b's sources (Alan, 2026-10-31)** - the device files at APPLY:

    - **APPLY's check judges the device files** as the first click's does, in the same warning, backup and write. An
      installation that differs is reconciled, no longer left alone, so a write missed earlier - in this session or
      another - is put right at the next APPLY.
    - **The edits are the user's side of the reconcile.** BindForge's side is the working copy with the edits on top,
      named on screen *"Your files, with the edits not applied"*. *Put mine back* writes the edits' device files; the
      reconcile weighs them against the installation's, conflict by conflict; taking an installation's files over them
      drops a device edit, by the user's choice, and the backup holds the working copy. So, unlike the binds and preset
      files, a device-file edit does not go on top of the settled set: an automatic merge per device would have to
      guess when an edited device and a reconciled one claim one VID/PID (principle 6). A reconciled set replaces the
      edits' device files whole, its moves made in the binds files written, the edits' included.
    - **An installation the check cannot read at all asks nothing at APPLY**: APPLY writes, leaves it alone, and the
      report says it was not written. Stopping every APPLY while a drive is unreadable would block the user's work.
    - **The reconcile reads the binds files the answers settle**, the edits on top at APPLY - built in storage once the
      binds rows are answered - so a device only the game's side uses ranks red, and *use one installation for all*
      names the bindings it would lose.
    **Settled with Storage S10's sources (Alan, 2026-10-31)** - a newer version:

    - **Asked on its own, before the warning**, at the first click and at APPLY, in the same backup and write as the
      rest. When every binding carried over: *Approve* or *Not now*. Otherwise: *Reconcile* - the merge for that file's
      rows, with *Keep all mine* and *Keep all in-game* - or *Not now*. Nothing is selected and Enter chooses nothing.
      Not now ends the check, or cancels APPLY, as the warning's does. A reset newer file asks *"Are you sure?"* before
      a line of it is taken. The warning follows for whatever else differs.
    - **Only the file name's version counts.** The root's `MajorVersion` / `MinorVersion` name the tool that last wrote
      the file ([bind-editor.md, *A fourth source*](bind-editor.md#a-fourth-source-48354-community-configs--2026-09-17)),
      so a higher one under the same name is the file's ordinary details row. The newer version is the newest live
      file of a BindName above the working copy's newest version of it.
    - **"Every binding carried over, unchanged":** every element of the working copy's file is in the newer one with
      the same value, the root's version and `PresetName` aside. **A new slot** is an element only the newer one has; it
      is always taken, never a row. A changed element, one the newer file dropped, or a new child inside an element is a
      row, at the merge grain.
    - **The result is the game's file**, its root and new slots kept, with each row answered as the working copy's
      spliced back in; it becomes the working copy's under the newer name, and the older name goes from the working
      copy. **An older-named file still in the bindings folder is removed after the backup**, as a rename's old name is
      (rule 7), and is not compared.
    - **The new slots are listed** by the game's element names, in the question and again in the report, to bind in the
      Bind Editor.
    - **At APPLY, an edit on the older file goes on top of the newer one** and covers the same line there, which is then
      not asked.
    - **A newer version that cannot be read element by element is left as it is** and named in the warning, with the
      file it replaces when the live folder lacks that.
    - **Device files and the preset file are left out.** A new Frontier entry in `DeviceMappings.xml` cannot be told
      from the user's - the stock list belongs to one game version - so it is an ordinary difference, and Merge keeps
      it; a `StartPreset.5.start` is hypothetical.

    **Settled with Storage S13b's sources (Alan, 2026-11-07)** - plain `.binds` in the bindings folder:

    - **What is plain:** `<name>.binds` with no period in the name - a factory file copied in by hand, which the game
      has not converted (`KeyboardMouseOnly.binds`, `KeyboardMouseOnly - Copy.binds`). `Custom.4.2 (2).binds` and
      `.binds.backup` are not one: they are no game file at all (Q1; `FileNames.isPlainBindsFile`). *They were left
      alone until 2026-11-08; since then they go to Leftover User Files, below.*
      **It never stays** in the bindings folder, a backup or the working copy - it confuses which file a preset line
      loads. **D1:** never backed up; it is not one of the user's files.
    - ~~**D2, judged against the settled set** - a twin there → removed, a hand edit in it lost; equal to the factory
      file → removed; else converted; a file that does not read as a binds file left alone and named.~~ **Replaced on
      2026-11-08** by *A plain file judged*, below. A converted file named like a factory one gets **no extra warning
      in the check's report** (Alan, 2026-11-04; worded so 2026-11-08, from the S13aR-S13b review's D2): S14c's
      startup line and the Bind Editor's warning still name it, and the user renames it there. **D3:** every match
      ignores case, and a second spelling of one plain file is a twin of the first converted.
    - **In the check, at the first click and at APPLY:** one more line in the same warning, backup and write; the preset
      file needs no change. Plain files are read again before the write, and one that changed meanwhile stops it, as a
      row does. **Any of the three choices tidies them up. A warning holding only plain files offers *Tidy up* and *Not
      now*** (Q2) - at APPLY too, where Not now cancels the APPLY (Q3). One that does not read as a binds file is only
      named: alone it asks nothing - the first click tells it, APPLY writes and the report names it (*until 2026-11-08;
      it now goes to Leftover User Files, below*). A converted file
      changes the working copy, so APPLY's backup holds the working copy as it was.
    - **D4: the report names each file removed, and why, or converted, and to what**; the log does too. ~~**A read-only
      plain file is kept and reported** with the kept-read-only line, as any removal is; a converted one's binds file is
      still written (Q4).~~ *Replaced 2026-11-09 (Alan, Storage S13eR): a plain file is moved whatever its read-only
      flag - below.*
    - **At Elite-Intel start** the plain files are the notice's fourth line, after the factory-name line - once a
      working copy exists, and **not while read only**, since the check that tidies them up is off then (Q5). *Since
      2026-11-08 the line names only the files the check will move* - below.
    - **Principle 13** listed a hand-copied factory file among the cases that get no guard; this tidy-up was planned
      after it, and principle 13's examples were amended to match (Alan, 2026-11-07, Q0).

    Built in Storage S13b: `FileNames.isPlainBindsFile` / `plainBindName`; `PlainBinds` (read, judge, log);
    `BindingsCheck.Found.plain` and `plainPlan`; the tidy-up in `BindingsCheck.settle` and `Apply.apply`;
    `LiveCopies` removing plain names; `CheckWarningDialog.Offer.TIDY_UP`; `CheckFlow.plainLines` / `plainReport`;
    `StartupNotice.Line.PlainFiles`. *Reworked in Storage S13d (2026-11-08) - below.*

    **Settled 2026-11-08 (Alan), from the S13aR-S13b code-integrity review and Krondor's review of S13b** - plain
    files judged again, and Leftover User Files. To be built in Storage S13d (Leftover User Files, the startup line) and
    S13e (a plain file judged against its twin) - the [brief](../../05-build-plan/storage.md#slices):

    - **A plain file judged** (replaces S13b's D2), against the settled set as before, each in name order:
      - **A true binds file of the same BindName is there:** the plain file is compared with it by value, as the
        check compares. Equal, or lacking only bindings the true file has → nothing more is needed. It has bindings the
        true file lacks → the user goes through the check's existing merge, the plain file one side and the true file
        the other, and what the user keeps goes into the true file. *Confirmed at Storage S13e's sources (below):* a
        binding both files hold with different values counts as one the true file lacks.
      - **No true binds file of that name:** equal by value to the factory file of that name (as the reset test
        judges, from a reachable installation) → a leftover, and the preset line loads the factory file; differing, or
        no factory file of that name → hand-edited, and **converted** as S13b does - `<name>.<root version, else
        4.2>.binds`, every byte kept but `PresetName`, into the working copy and the bindings folder.
      - **In every case, once the user has decided, the plain file leaves the bindings folder** - moved to Leftover
        User Files, never deleted. *Until 2026-11-08 a twin was removed, a hand edit in it lost, and so was a file equal
        to the factory one.*
    - **Leftover User Files.** Anything in the bindings folder that is **not a true binds file, a
      `StartPreset.N.start` or `BindingLoadingErrors.log`** is **moved, never deleted**, into a folder named
      `Leftover User Files` in Elite-Intel's user data folder - **`elite-intel/Leftover User Files`**, beside
      `working-copy` and `playerbackups`, named by `AppPaths` as they are - each move into **its own date-stamped
      subfolder**, stamped as a backup is. That covers plain files once judged, plain files that do not read as binds
      files, Windows copies such as `Custom.4.2 (2).binds`, `.binds.backup` files (probably old Elite-Intel backups),
      and any other stray file.
      - **The move happens only in the check** - at the first click and at APPLY, in its warning, backup and write -
        never unasked (principles 4 and 5). The report names each file moved and where it went; the log does too.
      - **Plain files still get no backup of their own** (S13b's D1 stands): the move keeps them. It also closes
        Krondor's finding that restoring APPLY's backup could lose a converted file's bindings - that backup, taken
        before the write, holds neither the plain file nor the converted one, and the plain file now survives the
        conversion in Leftover User Files.
      - ~~**A read-only file the check cannot move** stays where it is and is named once in the check's report, with the
        kept-read-only line, as any removal is (Q4).~~
        - **Replaced 2026-11-09 (Alan, at Storage S13eR's sources): a leftover is moved whatever its read-only flag.**
          There is no read-only rule for what does not belong in the bindings folder, plain files included: the flag
          protects a file from being deleted, and a leftover is kept, in Leftover User Files. Alan: *"A plain
          read-only file that ends up in the bindings folder is garbage... this never happens."* (principle 13). It
          was never asked for - the rule was carried over from removals (rule 10, *Settled with Storage S3's sources*)
          at S13b's Q4 and S13d's Q6 - and it made the check ask about such a file at every first click and every
          APPLY (the S13c-S13e review's finding 1). This also replaces that review's D1 of 2026-11-08, which would
          have left a read-only leftover out of the check and logged it; D1 was never built. The rule for a read-only
          binds file or `.buttonMap` a write would **delete** is unchanged.
        - **Settled from the S13eR review (Alan, 2026-11-09), built in Storage S13eR2 the same day:** **the move
          changes no flag.** S13eR cleared a set read-only flag before the move, for a move to another drive; that goes.
          *Corrected 2026-11-10 (the S13eR2 review's finding 1):* the flag does not stop the move either. On Windows the
          bindings folder and Leftover User Files are both under `LOCALAPPDATA`, one drive, so the move is a rename; on
          Linux a move to another drive copies and unlinks, which a read-only file allows. *Until then this said a
          read-only leftover on another drive is not moved.* A move that does fail is reported, as any failed move is. **A plain
          file converted, or merged into its twin, whose move fails keeps no report line of its own** - the failure
          line names it, and the next check moves it as a twin. Accepted (principle 13).
      - **BindForge is not a backup system.** After the move it does nothing with that folder: it is not listed in File
        Manager, nothing restores from it, and it is never cleaned. The user opens it in the file manager.
      - *To confirm at the sources:* a leftover `StartPreset.start` (no generation number) is moved too, as the rule's
        words say; a folder inside the bindings folder is left alone; a warning holding only files to move offers
        *Tidy up* / *Not now* as one holding only plain files does (Q2), Not now cancelling APPLY (Q3).
    - **The line at Elite-Intel start names only the files the check will actually move** (the review's D1). ~~A
      read-only file is not named at every start - only once, in the check's report.~~ *Since 2026-11-09 (Storage
      S13eR) the check moves a read-only one too, so the line names it.* *To confirm at the sources:* the
      line counts every file the check will move, not only plain ones.

    **Settled with Storage S13d's sources (Alan, 2026-11-08, Q1-Q7 as recommended)** - Leftover User Files:

    - **Q1: a leftover `StartPreset.start` is moved**, as the rule's words say. It also ends the loader's
      `findStartPresetFile` taking whichever `StartPreset.*.start` the folder lists first.
    - **Q2: a folder inside the bindings folder is left alone.** Only files are moved.
    - **Q3: a warning holding only files to move offers *Tidy up* / *Not now***, as one holding only plain files did; at
      APPLY Not now cancels it. So a stray file is never only named: it is something to ask about, like any other.
    - **Q4: the startup line counts every file the check will move**, not only plain ones. *Amended 2026-11-09
      (Krondor's code-integrity review, finding 4):* it names at most two. With more it says how many and names the
      first two in name order (`speech.bindforge.leftoverMore`), since it is said at every start until the check tidies
      up and a folder of old `.binds.backup` files would otherwise be read aloud name by name. The check's warning
      still names every file.
    - **Q5: what stays is matched ignoring case** - `<name>.<major>.<minor>.binds`, `StartPreset.N.start` and
      `BindingLoadingErrors.log` in any case - so nothing that could be the game's own file is moved
      (`FileNames.staysInBindingsFolder`). Every other match of a kind stays exact (S13a).
    - ~~**Q6: a read-only file is kept**, checked before the move as a removal checks it - Windows would let it move, but
      the user protected it. The report gives it its own line, saying it was left rather than moved; the startup line
      leaves it out.~~ **Replaced 2026-11-09 (Alan, Storage S13eR):** moved like any other, above.
    - **Q7: the move is `Files.move`**, which copies and deletes when the two folders are on different drives (possible
      under a Wine prefix). One that fails stays where it is and is reported.
    - *Decided while building:* one write's moves go into one date-stamped folder (`yyyy-MM-dd_HH-mm-ss`, as a backup is
      stamped; `-2` for a second write in the same second), made at the first move, so a write that moves nothing makes
      no folder. A first move that fails takes the folder away again (the S13eR2 review's finding 2, 2026-11-10): the
      file stays, every check after tries it again, and each try had left one more empty folder. The moves run after the write; if a binds file could not be written they are held back with the
      removals, since a converted file's binds file may be the one that failed. A file only moved changes nothing of the
      working copy, so APPLY's backup holds the working copy only when a plain file is converted (the S13aR-S13b
      review's finding 6, which S13e was to fold in). A plain file that reads as a binds file but has no `<Root>` to
      convert by is a bug, so it fails fast (the review's finding 4, likewise).

    *Built in Storage S13eR (2026-11-09):* `LiveCopies.moveAway` no longer asks the read-only flag - *until Storage
    S13eR2, the same day, it cleared one that was set, for a move to another drive; now it changes no flag (above)*;
    `StartupNotice.movable`, the report line `bindings.check.result.keptReadOnlyLeftover` (nine bundles) and
    `CheckFlow`'s kept branches are gone.

    Built in Storage S13d: `bindforge.storage.Leftovers` (read, plan, log); `FileNames.staysInBindingsFolder`;
    `AppPaths.getLeftoverUserFilesDir`; `LiveCopies` moving (`LeftoverFolder`), `WriteReport.Place.moved` / `movedTo`;
    `BindingsCheck.Found.leftovers` and `leftoversPlan`; `Written.leftovers`; `CheckFlow.leftoverLines` /
    `leftoversReport` / `keptReadOnly(Written)`; `StartupNotice.Line.ToMove`. `PlainBinds` keeps the judging only - its
    `Found`, `read` and *left alone* outcome went.

    **Settled with Storage S13e's sources (Alan, 2026-11-08, Q1-Q6 as recommended)** - a plain file judged against its
    twin:

    - **Q1: a binding both files hold with different values is one the twin lacks**, so it is a row - the plain file may
      hold a hand edit. Settings and `KeyboardLayout` count the same way: every line item the plain file holds
      differently, at the merge grain (per slot for a button).
    - **Q2: an unbound slot in the plain file (`{NoDevice}`), or an element only the twin has, is the plain file lacking
      it** - no row, and the twin keeps its own. Otherwise a hand-copied factory file would ask about every slot the user
      has bound. An element only the plain file has is a row when anything in it is bound. The root is no row: its
      `PresetName` is the plain file's own name.
    - **Q3: the rows are ordinary rows of the same warning and merge.** The File column names the plain file, *Mine* is
      the twin and *In-game* the plain file, which sits in the game's folder. The warning's line says how many lines it
      holds that the twin lacks, and the three choices act on them as on any row: *Put mine back* leaves the twin as it
      is, *Use the game's files* takes the plain file's lines into it, *Merge* asks row by row. A warning with such rows
      offers every choice, not Tidy up.
    - **Q4: compared with the twin as BindForge holds it** - the working copy's with the edits on top (at the first click
      the working copy) - or, when only the bindings folder holds it, the live one. The answers go into the twin the write
      holds, compared again there, and are put on last, so a line kept of the plain file wins over an edit on that line:
      the merge showed the edit as *Mine*. A row the warning did not show keeps the twin's. ~~If the answers leave no twin
      in the set (a live-only twin the user put back), the plain file is judged as one with no twin and its answers are
      dropped - rare, principle 13.~~ **Amended 2026-11-09 (Krondor's code-integrity review, finding 1):** judged as
      one with no twin, the plain file was converted into the very name the user had just put back - *Put mine back*
      ending with a binds file made of the plain one. Now **when the write takes a binds file of the plain file's
      BindName out of the bindings folder** - a live-only one the user kept the working copy's side of, or at APPLY a
      name the edits renamed away - and the set holds no other, the plain file is **only moved**
      (`PlainBinds.Fate.TWIN_LEAVES`), its answers dropped and its bytes kept in Leftover User Files. The report says
      so (`bindings.check.result.plainTwinLeaves`). A twin the set holds still wins: a newer version's older name
      leaves while the newer one is written.
    - **Q5: `Tidied.to` became `about`**, its Javadoc saying what it holds for each fate (the review's finding 7): a
      factory file's name would read *"KeyboardMouseOnly.binds holds the factory file KeyboardMouseOnly.binds"*.
    - **Q6: a plain file lacking only what its twin has** is moved with the existing *"is already in the game's folder"*
      report line. One whose kept lines went into its twin has a line of its own. *Until 2026-11-09 one kept read-only
      still had that line, since the twin was written; no leftover is kept for its flag now (Storage S13eR, above).*
    - *Decided while building:* APPLY's backup holds the working copy when a twin is merged into, as when a file is
      converted. The device files' reconcile reads the binds files the binds rows settle, without a plain file's lines -
      a device only a plain file names does not rank red there, as with a converted file since S13b. Left (principle 13).
    - ~~*Accepted (Alan, 2026-11-08, the S13c-S13e review's D2, principle 13):* when a plain file's only twin is a live-only
      binds file and the user answers *Put mine back*, that twin leaves the set, the plain file has no twin at the
      write and is converted - possibly onto the twin's own name - and the rows the warning showed go unused.~~
      *Replaced the next day by Q4's amendment above (Krondor, 2026-11-09): the plain file is only moved. Struck at
      Storage S13eR's sources (Alan, 2026-11-09, Q1).*
    - *Also, from the S13aR-S13b review (D5):* escaping the `PresetName` lives in `Create.withPresetName` (finding 5),
      and the support report names a second spelling it left out (finding 8).

    Built in Storage S13e: `BindingsComparison.plainAgainstTwin` / `mergedIntoTwin`; `PlainBinds.rows`, `Rows`,
    `Fate.MERGED`, `Plan.merged` / `changesTheSet`; `BindingsCheck.Found.plainRows` / `items` / `rows` / `anyRows`;
    `CheckFlow.leftoverLines` (`bindings.check.file.plainRows`) and `leftoversReport` (`bindings.check.result.plainMerged`).

    **Settled 2026-11-11 (Alan)** - an element the game writes twice:

    - **What was found.** Every binds file the game saves holds `<MouseGUI Value="…" />` twice at the top level: once
      among the mouse settings, once after `EnableMenuGroups`. Measured on Alan's four live files, the four reference
      files (`Custom.3.0.binds` included, so it is old) and his oldest backups; in all of them the two values agree.
      Frontier's 30 factory files hold it once, and the game adds the second when it saves
      ([EliteDangerous-BindsFileFormat.md](domain-knowledge/EliteDangerous-BindsFileFormat.md)). It is the only
      repeated top-level name in any of them.
    - **The bug.** The comparison took a file with a repeated top-level name as one it could not read element by
      element (`BindsElements.of`), so every file the game had saved gave one row, *the whole file*: Alan changed two
      bindings in the game and the merge offered only the file (2026-11-11). The tests were built from files holding
      it once.
    - **The rule: an element the file holds more than once is one line item.** It gives one row, and the side kept is
      written to every copy of it, each where it is. Every other element then gives its rows as this rule says - per
      slot for a button, else per element. The merge never writes a file whose copies differ.
    - **A whole-file row is only for a file that cannot be read at all** - not well-formed, or one whose scan and
      parse disagree - or one that is on one side only.
    - *To confirm at the slice's sources (Storage S18):* - answered in *Settled with Storage S18's sources*, below.
      - one file whose copies hold different values - none has been seen, and which copy the game reads is not known
        (a hand test: set them differently, start the game, see which the widget follows and what the game saves
        back - [testing item 31](../../00-overview/testing-required.md#bindforge));
      - the two sides holding a different number of copies - a file the game has not saved yet holds one - and
        whether a write ever adds or removes a copy;
      - a repeated element that has children - none has been seen;
      - everything else that reads a binds file this way: a newer version, a plain file against its twin, the reset
        test, the edits put on top at APPLY;
      - whether the Bind Editor's own write of a setting changes every copy.

    **Settled with Storage S18's sources (Alan, 2026-11-11, Q1-Q5 as recommended)** - an element held more than once:

    - **Q1: the number of copies does not count.** A repeated element is compared by its value, so one
      `<MouseGUI Value="0" />` equals two. Otherwise every file made from a factory file (CREATE, a converted plain
      file) would ask about `MouseGUI` once the game first saved it, and a reset file the game had saved would never
      be named a reset - the factory file holds it once.
    - **Q2: a write never adds or removes a copy of an element both sides hold.** The file written keeps its own
      copies, each replaced with the kept value where it is. An element only one side holds is added once, or every
      copy of it removed.
    - **Q3: a file whose copies differ** - none seen. Its element's value is every copy's value in order, so it
      differs from any file whose copies agree; the row shows them all (*0 / 1*). Keeping that side writes its
      **first** copy to every copy, so a merge never writes copies that differ - whichever side the row keeps,
      the side the file is written from included (the S18 code-integrity review's D1, 2026-11-11: until then a row
      kept on that side left its copies as they were). To revisit when
      [testing item 31](../../00-overview/testing-required.md#bindforge) says which copy the game reads.
    - **Q4: a repeated element is always one row, never per slot** - children or not (none with children seen); the
      kept side's first copy is written to every copy.
    - **Q5: an overwrite copies the file as it is.** *Put mine back* and *Use the game's files* write the whole file,
      copies that differ included; *the merge never writes a file whose copies differ* is about the rows a merge
      splices (principle 13). *Found while building (2026-11-11):* a merge with every row of a file answered in-game
      takes the game's file whole, as it always has, so it is that overwrite too.
    - **The Bind Editor's write of a setting is left alone.** `BindingsWriter.assignSetting` refuses a repeated setting
      and `BindsValues` reads it as not editable, so the Bind Editor never writes `MouseGUI` today; writing every copy
      there is [E10](../../05-build-plan/bind-editor-editing.md#slices)'s.

    Built in Storage S18: `BindsElements.Copy`, `Top.copies` / `values` / `sameValue` / `repeated` / `copiesDiffer`;
    `BindingsComparison`'s `elementSplices` (every copy), `copiesMadeEqual` and `describe(Top)`.

15. **When Elite-Intel starts**, once first-time startup is complete, it reads the live copies and compares them with
    the working copy, as the check does — reading only: nothing is written and no backup is made. If anything
    differs, it says once, spoken and logged: *"Your control files were changed outside BindForge. Open BindForge to
    check them."* Nothing is decided until the user opens BindForge (2026-10-23, Alan). *Since Storage S14c
    (2026-11-06) it is a line of the notice at start, and is not said while BindForge is read only.*

## Create — a new player's first binds file

16. **CREATE sits at the left end of the APPLY / DISCARD bar, shown only while the user has no custom binds file.**
    Until it is pressed, Alias Designer, Bind Editor and Preset Editor are greyed out; File Manager stays usable.
    CREATE asks which of Frontier's factory binds files to start from — `KeyboardMouseOnly` first and selected — and
    a name, pre-filled `Custom`. The new binds file and all four lines of `StartPreset.4.start` pointing at it are
    edits not applied: APPLY writes them and CREATE disappears; DISCARD puts the user back where they started, CREATE
    lit (2026-10-24, Alan). See [overview](overview.md#create--a-new-players-first-binds-file--2026-10-24).

    **Settled with Storage S11's sources (Alan, 2026-11-02):**

    - **"No custom binds file" means the working copy holds no `<name>.<major>.<minor>.binds`.** CREATE is shown while
      that is so, lit until pressed and dimmed while its file waits for APPLY; the editor tabs are greyed while the
      edits hold none. Before first-time startup, or while the working copy cannot be read, CREATE is not shown -
      first-time startup or RESTORE comes first.
    - **The new file is named with the version on the factory file's root** (`MajorVersion`, `MinorVersion`), else
      `4.2`, the Live game's: `Custom.4.2.binds`. It only names the file; nothing is decided by it (rule 14, *only the
      file name's version counts*).
    - **A factory file in several installations is taken from the first reachable one** - every installation runs one
      game version. Only reachable installations' factory files are offered; with none, CREATE says so and changes
      nothing.
    - **The name is held to the controller name's rule**, compared as it is - ignoring case, `-` and `_` - against every
      factory BindName and the set's own, so `custom` is refused beside `Custom`.
    - **The copy is the factory file's bytes with only the root's `PresetName` changed** to the new name, since a file
      whose `PresetName` differs from its name is not offered ([preset-editor.md](preset-editor.md#the-bindname-list--settled-2026-10-17-alan-p1)).
      Alan confirmed the game takes such a file (2026-11-02, [testing item 7](../../00-overview/testing-required.md)).
    - **Decided while building:** a greyed editor tab that is showing gives way to File Manager, so no editor is left on
      screen with nothing to edit.

    Built in Storage S11 as `bindforge.storage.Create`, `ui.screen.bindings.CreateDialog` and CREATE on `ApplyBar`;
    `BindingsTabPanel` greys the Bind Editor and Alias Designer tabs (the Preset Editor's joins them at P4).
