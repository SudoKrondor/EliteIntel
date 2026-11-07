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
    one left - written as a new file and the old spelling removed, the way rule 7 renames.
  - Built in Storage S13a.
- **A `.buttonMap` the working copy lacks is removed**, except Frontier's shipped `VPCPanel` and `VPCThrottle`. An old
  spelling of a kept one (`lvwap.buttonMap` for `LVWAP.buttonMap`) is that same file (the case rule above). **Until
  Storage S13a** it is kept as it is on Windows and removed on a file system that heeds case, the kept spelling written
  beside it.
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
