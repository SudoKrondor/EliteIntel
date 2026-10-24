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
   be changed at any time; an installation added later gets the working copy at the next APPLY (2026-10-24,
   Alan).

   The live copies were already backed up when Elite-Intel started ([rule 12](#backups)).
2. If the installations hold different versions of `DeviceMappings.xml` or the `.buttonMap`s, the user reconciles
   them: keep one over another, or merge. A merge with conflicting line items, such as two names for one button,
   needs a decision on each of those items.
3. Once every decision is made: back up everything — **this backup is kept forever**, as the user's files before
   BindForge ever wrote anything (2026-10-23, Alan) — then create the working copy — every `.binds`, the
   `StartPreset.N.start`, **one** `DeviceMappings.xml` and **one** of each `.buttonMap`, reconciled — and mark
   first-time startup complete.
4. From then on, **the working copy is the source of truth.**

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

## Restore

10. RESTORE: choose a backup. It backs up what is there now, then puts the whole backup back and replaces the working copy.
    No choices (2026-10-24, Alan). The `.binds` and `StartPreset.4.start` go to the bindings folder; the device files
    go to every installation on this machine now, as one set — the backup's, or, if the backup holds different device
    files for different installations, the set the user reconciles as in rule 2. Its confirmation says any edits on
    screen are thrown away. (2026-10-22, Alan: the working copy is replaced too, otherwise it would no longer be what
    BindForge last wrote, and the next check would ask the user about their own restore.)
    **If the working copy cannot be read,** BindForge says so and offers the most recent backup to restore from
    (2026-10-23, Alan).
11. One backup system. No versions, no history per file. **Backups older than the retention setting — 30 days by
    default — are deleted when Elite-Intel starts**, except the one first-time startup takes before creating the working copy (rule 3), which is kept
    forever (2026-10-21 and 2026-10-23, Alan).

## Backups

12. A backup is a folder holding a copy of the live copies, named with its date stamp, in Elite-Intel's BindForge
    backup folder — a plain folder, not a zip, so the user can look through it, as in V1.1 (2026-10-24, Alan). One is
    made before every write to the live copies. **Before a live copy replaces the working copy, the working copy is
    backed up the same way** (2026-10-23, Alan).

    **The backup folder is the record** — the list of backups is the folder's listing, sorted by the date stamp in
    each name, with no database table — and **it is in Elite-Intel's own data folder, never in a game folder**, where
    a game update would delete it (2026-10-24, Alan).

    Every backup holds the full set of live copies from every installation, even when they match — no comparison
    first (2026-10-24, Alan).

    **Every time Elite-Intel starts, it backs up all the live copies, in every installation, silently, in the
    background** — whether or not the user ever opens BindForge, and before first-time startup has ever run. The
    user is not told. The working copy is not included: after BindForge was last used it matches the live copy. If
    the backup fails, it is written to the application log and tried again the next time Elite-Intel starts
    (2026-10-23, Alan). It cannot be switched off (2026-10-24, Alan). *Why:* a user who lost their binds can be told where their backups are, or restore one in
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
