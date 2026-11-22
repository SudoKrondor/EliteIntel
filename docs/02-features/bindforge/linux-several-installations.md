# BindForge — Several Game Installations on Linux

**Status: specified and built, 2026-11-21 (Krondor; branch `V1.2-linux-multi-game`, merging into `V1.2`).** It replaces the interim rule in
[input environment, *What BindForge does with several Linux installations*](../../00-overview/input-environment.md#what-bindforge-does-with-several-linux-installations---2026-10-14-alan):
*"a Linux user with several installations has `.binds` changes made in one installation only, and Apply's confirm
says so"*. Windows and a single Linux installation behave exactly as before.

## The problem

On Windows every installation shares one bindings folder and one journal folder, the Windows user's own. On Linux each
installation runs in a Wine prefix of its own (Steam's Proton, or Heroic, Lutris or a hand-made prefix for Epic and the
Frontier launcher), and each prefix holds a Windows user folder of its own, so **each installation has its own bindings
folder and its own journal folder**.

Elite-Intel reads one of each, `bindings_dir` and `journal_dir`, which on Linux start out as the launcher's
`ed-bindings` and `ed-journal` symlinks. That is right for one installation. With several:

- the journals read are only those of the installation the symlinks point at, so playing another one goes unheard;
- BindForge writes `.binds` and `StartPreset.N.start` to that one folder, so the other installations never get the
  user's controls.

## The rules

### 1. An installation's two folders hang off one Windows user folder

Inside a Windows user folder the game keeps
`AppData/Local/Frontier Developments/Elite Dangerous/Options/Bindings` and
`Saved Games/Frontier Developments/Elite Dangerous`. That holds on Windows and in every Wine prefix alike, so one
relation serves both (`GameUserFolder`). **The journal folder is found from the bindings folder, never guessed apart
from it.**

- A Steam installation's user folder is `<library>/steamapps/compatdata/359320/pfx/drive_c/users/steamuser`, as today.
- An installation added by hand on Linux is asked for its **Wine prefix** (rule 6). Its user folder is the one under
  `drive_c/users/` that holds the game's folders.
- On Windows every installation reports the one shared folder, as today.

### 2. "The installation being played" is the choice of folders Elite-Intel reads

The choices are the reachable installations with a bindings folder of their own, **one per distinct bindings folder**
(compared by real path, so two routes to one folder are one choice). Picking one sets `bindings_dir` and `journal_dir`
to its two folders - both checked before either is stored, so they never point at different installations, and one
choice at a time, the user's and the follower's alike - and restarts only what reads them: the journal parser, the auxiliary-files monitor (`Status.json`
and the other files beside the journals) and the bindings monitor. Nothing else restarts (no push-to-talk, no VEGA,
no Hands thread, so no input-permission prompt).

**With fewer than two choices nothing is shown and nothing is followed.** That covers Windows (one shared folder) and
every single-installation Linux machine.

### 3. It follows the game, so nobody has to set it

There is no way to know which installation will be played while the game is off, and there is no need to: the game
says so when it starts, by opening a new journal (`Journal.<stamp>.<part>.log`) in its own journal folder. The stamp
in the name is the local time the game opened the file (`JournalFiles`).

**The rule:** every 5 seconds, the newest journal of each choice is found by its name. If the newest of them all
belongs to an installation other than the one being read, and **it was opened after the current choice was made**,
that installation becomes the one being played.

| Situation | What happens |
|---|---|
| The game starts after the app | Its new journal is newer than the choice, so within 5 s Elite-Intel switches to it, before `LoadGame` |
| The game was already running when the app started | Its journal is newer than the last choice, so it switches at the first look |
| The app runs and the game does not | Nothing new appears, so the last choice stands. It is the right guess for the next session, and is put right when the game starts |
| The user picked an installation by hand, then restarts the app with the game off | Every journal is older than the pick, so the pick stands |
| No journal anywhere yet | Nothing to follow; the launcher's symlinks (the defaults) stand |

The time of the choice is stored (`bindforge_settings.game_folders_chosen_at`, in the journal stamp's own form,
`yyyyMMddHHmmss` local time). An install that never chose has `0`, so on the first run with two installations the one
with the newest journal is taken. Only one game runs at a time, so the newest journal is the game running now.

The installations are worked out when the follower starts, which is when the services start. One added while they run
is followed from the next start. The follower decides and chooses in one step, under the same lock as a pick by hand, so
a pick the user makes while it looks is never undone by a decision taken before it.

### 4. The selector

A dropdown on the APPLY bar, left of the User Configuration Folder: **Playing:** followed by the installation, named by its
storefront, with its folder when two share one storefront. The tooltip names its bindings and journal folders. It shows what rule 3 chose and changes when
the follower switches. Picking another one is the manual choice of rule 2, stored with its time, so the game starting
anywhere later still wins. It is hidden with fewer than two choices.

A pick is taken once the selection has rested for a moment, so stepping through the list with the arrow keys chooses
only where it stops, and picking the installation already played does nothing - each choice restarts the journal
parser. Adding or removing an installation on Game Install Locations refreshes the list at once, so one added with its
Wine prefix can be picked without leaving the screen.

The User Configuration Folder then shows the chosen installation's bindings folder, since that is the setting.

### 5. Every installation gets the user's controls

Every write of the working copy (APPLY, the check's write, first-time startup and RESTORE, all through `WritePath`)
writes the binds and preset files to **the bindings folder of each other installation** as well as to the one being
played, under the same rules as the main folder: binds files before the preset file, the same removals, read-only
files kept read-only. Files that do not belong in the folder are moved to Leftover User Files from the main folder
only, since only that one was checked.

**A folder that was changed outside BindForge is left alone.** An installation's folder is written only when its binds
and preset files still hold exactly what BindForge last wrote (the working copy as it was before this write, compared
by value as the check compares). Otherwise it is left alone and the report says so
(`bindings.applyBar.result.otherBindingsDiffers`):

> *EPIC (/games/epic/EliteDangerous): its controls were changed outside BindForge, so they were left as they are. Play
> that installation once and BindForge offers the changes the next time you apply.*

This is the device-file rule (an installation whose device files differ is left alone) applied to the bindings folder.
It is what keeps an in-game rebind made in one installation from being written over unseen, and it settles itself:
once that installation is played it becomes the bindings folder the check reads, the check offers its changes as it
does today, and from then on its folder matches.

Consequences, all intended:

- **First-time startup** has no working copy before it, so the other installations are left alone by its write. Nothing
  says the user changed them, so the report does not say so either: it says each is taken in the first time it is
  played (`otherBindingsNotCompared`). A working copy that cannot be read before a write is reported the same way.
- **An installation that did not take its device files** keeps its binds files too, since the game rejects a binds file
  naming a controller the installation does not know. The report gives it one line of its own
  (`otherBindingsDeviceFiles`), not a line per file.
- **A folder that cannot be read** to compare is left alone and reported as the failure it is, with the system's
  message as the detail.
- A folder whose installation has never run the game is not there. It is not created (the game makes it) and not
  reported: there is nothing of the user's in it, and once the game has run there it is taken in as above.
- The before-changes backup already holds each installation's own bindings folder (`Backups`), so every folder written
  is backed up first, as rule 3 of storage.md requires.

The Linux note in APPLY's confirmation (*"bindings this Apply moves are moved only for the installation Elite-Intel
uses"*) is removed. It is no longer true, and the report names any installation that did not take the write.

### 6. Add Missing asks for the Wine prefix on Linux

Detection finds Steam only. An Epic or Frontier-launcher copy run through Heroic, Lutris or Wine reaches BindForge by
**Add Missing**, as today. On Linux, once the game folder is accepted, a second folder chooser asks for **its Wine
prefix, the folder holding `drive_c`**. Cancelling it keeps the installation without one: its device files are still
written, its bindings folder is not, and it is not offered in the selector. A folder without `drive_c` is refused
with what was expected.

The prefix is stored on the installation's row (`bindforge_installations.wine_prefix`). The user folder inside it is
found when the folders are needed, so a prefix the game has not yet run in is accepted and starts working once it has.
Which operating system asks is the provider's to say (`GameInstallationProvider.asksForWinePrefix`), not the screen's.

## What does not change

- Windows: one folder, no selector, no follower, no change in what is written.
- One Linux installation: one choice, so no selector and no follower. Its own folder is the bindings folder, so there
  is no other folder to write.
- The launcher's `ed-bindings` / `ed-journal` symlinks stay as they are. They are the defaults and nothing rewrites
  them; a choice is stored in the settings, which win over them.
- Device files go to every installation, under the reconcile's rules, as before.

## Built from

| Piece | Where |
|---|---|
| The user-folder relation, and finding it in a prefix | `bindforge.install.GameUserFolder` |
| The choices, choosing one, and the follow rule | `bindforge.install.PlayedInstallation` |
| The 5-second follower (service `PLAYED_INSTALLATION`, before the journal parser) | `bindforge.install.PlayedInstallationFollower` |
| Restarting what reads the folders | `AppController.onGameFoldersChanged`, `BindingsMonitor.watchAgain` |
| Writing every installation's bindings folder | `bindforge.storage.LiveCopies`, `WriteReport.otherBindings` (`WriteReport.NotWritten`: why one was left alone unchanged) |
| The selector | `ui.screen.bindings.PlayedInstallationField` |
| The stored prefix and choice time | migration `12028__bindforge_several_linux_installations.sql` |

## To test on a real machine

Recorded in [testing-required](../../00-overview/testing-required.md#core-platform):

- Two Steam libraries each holding the game: start the app, then the game in the second library; the bindings and
  journal folders switch within 5 s and the commander loads.
- That the game creates its journal at client start, before the main menu. If it does so only later, the switch
  happens later too, still before `LoadGame`.
- An Epic copy under Heroic added by hand with its prefix: APPLY writes its bindings folder.
