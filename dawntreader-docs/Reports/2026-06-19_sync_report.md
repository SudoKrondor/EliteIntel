# Git Sync Report — 2026-06-19

## Starting state

- Branch: `V1.1-KAN-6-push-to-talk-dawntreader`
- 114 commits ahead of `origin/V1.1-KAN-6-push-to-talk-dawntreader`
- Working tree: modified `dawntreader-docs/Reports/SYNC_REPORT.md` + several untracked doc files (BindForge audits, binding spreadsheets, etc.) — left untouched throughout this sync

## Remotes

- `origin` → https://github.com/SudoKrondor/EliteIntel.git (DawnTreader fork)
- `upstream` → https://github.com/stone-alex/EliteIntel.git (stone-alex/EliteIntel)

## Fetch results

Both `origin` and `upstream` advanced `V1.1` identically: `368c6b34` → `7a73af3a` (fork is in sync with upstream, no divergence between the two).

## Local V1.1 update

Local `V1.1` was 19 commits behind `origin/V1.1` / `upstream/V1.1`, with no local-only commits — a clean fast-forward. Updated via `git fetch origin V1.1:V1.1` (without checking out the branch) to `7a73af3a`.

New commits pulled into `V1.1`:
- Reducer/STOP_WORDS localization + `LOCALIZATION_GUIDE.md`
- DevicePackage service moved to its own bus / gameapi package reorg (4 commits) — large reorg: `gameapi` → `eventbus` (AudioMonitorBus, GameControllerBus, EventBusManager→GameEventBus), `ai/hands` → `gameapi/inputs` (PreFtlChecks, RoutePlotter, UiNavCommon), new `DeviceBus`/`UiBus`, new `PACKAGE.md` docs in several packages
- Fix: HUD checkbox/segmented control text clipping when squeezed
- i18n: localized spoken audio calibration phrases
- Command catalog table: cap/shrink type column style, show built-in queries
- Fix: localize ship personality dropdown editor in fleet table
- Refactor: fold `CommandHandler` into `IntelAction` (deletes `CommandHandler.java`, `CommandIds.java`, `QueryHandler.java`, `QueryIds.java`; adds `IntelAction.java`, `RegisterCommand.java`, `RegisterQuery.java`, `AiActionMapGenerator.java`)
- Brain tree distance fix + RU test fix
- Refactor: rename `CustomCommandParameterSpec` → `ActionParameterSpec`
- Commands rework parts 3 & 4
- FR AZERTY keyboard fixes (`FrontierBindingKeyResolver.java` new, `KeyBindingExecutor.java`, `KeyProcessor.java` new, `LinuxX11NativeKeyInput.java`, `WindowsNativeKeyInput.java`)

## Merge into working branch

Merged `V1.1` (`7a73af3a`) into `V1.1-KAN-6-push-to-talk-dawntreader` via `git merge V1.1 --no-edit`.

**Result: clean merge, no conflicts** (merge strategy `ort`, merge commit `ef7674eb`). Verified no leftover conflict markers and no unmerged paths in `git status`.

413 files changed (5836 insertions, 3761 deletions), including the package reorg and `ui/event/PttButtonStateEvent.java` (auto-merged cleanly, +1 line) which sits directly in the push-to-talk feature's path.

Files flagged beforehand as conflict-risk (`KeyBindingExecutor.java`, `KeyProcessor.java`, `LinuxX11NativeKeyInput.java`, `WindowsNativeKeyInput.java`, `PttButtonStateEvent.java`) all merged without conflict.

## End state

- Branch `V1.1-KAN-6-push-to-talk-dawntreader` now 134 commits ahead of `origin/V1.1-KAN-6-push-to-talk-dawntreader`, up to date with `V1.1` (`7a73af3a`)
- HEAD: `ef7674eb`
- No `git push` performed — local only
