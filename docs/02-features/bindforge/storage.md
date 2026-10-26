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
- **Every known installation unreachable stops it** (Alan, 2026-10-25, from the S2b review) - a drive unplugged. BindForge
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
   from the live bindings folder, after the backup (2026-10-24, Alan).

   **Before anything is backed up or written, APPLY checks that every one of the four lines of
   `StartPreset.4.start` names a binds file that exists.** If one does not — or is empty — APPLY refuses as a whole
   and names the sections (2026-10-24, Alan), because the game would silently load something else.
8. APPLY writes everything it can. If a file or an installation can't be written, the user is told — *"updated 2 of
   3"* — and the check finds the missed installation and offers to put the working copy there (rule 14), or the
   next APPLY writes it. Nothing is rolled back (2026-10-23, Alan).
9. Before the user leaves BindForge with edits not applied — another Elite-Intel tab, or closing Elite-Intel — they
   are reminded, and choose **Go back**, **Discard** or **Apply** (Apply with its usual confirmation and backup).
   Nothing is selected and Enter chooses nothing; Esc and the close glyph are Go back (2026-10-23, Alan).

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
- **A `.buttonMap` the working copy lacks is removed**, except Frontier's shipped `VPCPanel` and `VPCThrottle`. An old
  spelling of a kept one (`lvwap.buttonMap` for `LVWAP.buttonMap`) is the same file on Windows and is kept as it; on a
  file system that heeds case it is a file of its own, and goes.
- **A working copy with no `DeviceMappings.xml` leaves each installation's alone** (review, 2026-10-24). Without that
  file the game loads no bindings at all ([overview](overview.md#why-devicemappingsxml-is-not-cosmetic--reclassified-2026-09-08)),
  so the write never removes one — unlike a `.buttonMap`, which holds only labels and is removed when the working copy
  lacks it.
- **A `.binds` the working copy lacks is left in the bindings folder.** Removing an old-named one is a rename's
  (rule 7, Storage S12).

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
    the on-screen edits on top and writes everything out.

    **A newer version is reconciled, never overwritten** (Krondor, 2026-09-24; Alan, 2026-10-23). When a live copy is
    a newer version than the working copy — `.4.2.binds` becoming `.4.3.binds`, or a newer version on the XML root —
    "overwrite the live copy with the working copy" is not offered. If the newer live copy holds every binding the
    working copy has, unchanged, the user is asked to approve it becoming the working copy, under its new name. If
    not, the user reconciles: the new slots are inserted into the working copy, every other difference is decided
    line by line, and the working copy is renamed to the new version. Either way the new slots are listed, so the
    user knows to bind them. See [overview](overview.md#when-a-game-update-brings-a-newer-version--2026-10-23).

    Large changes Frontier makes to anything other than `.binds` would need a BindForge code fix.
15. **When Elite-Intel starts**, once first-time startup is complete, it reads the live copies and compares them with
    the working copy, as the check does — reading only: nothing is written and no backup is made. If anything
    differs, it says once, spoken and logged: *"Your control files were changed outside BindForge. Open BindForge to
    check them."* Nothing is decided until the user opens BindForge (2026-10-23, Alan).

## Create — a new player's first binds file

16. **CREATE sits at the left end of the APPLY / DISCARD bar, shown only while the user has no custom binds file.**
    Until it is pressed, Alias Designer, Bind Editor and Preset Editor are greyed out; File Manager stays usable.
    CREATE asks which of Frontier's factory binds files to start from — `KeyboardMouseOnly` first and selected — and
    a name, pre-filled `Custom`. The new binds file and all four lines of `StartPreset.4.start` pointing at it are
    edits not applied: APPLY writes them and CREATE disappears; DISCARD puts the user back where they started, CREATE
    lit (2026-10-24, Alan). See [overview](overview.md#create--a-new-players-first-binds-file--2026-10-24).
