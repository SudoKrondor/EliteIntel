-- The "auto exit UI before opening another panel" toggle is gone: backing out of an open panel before
-- opening another one is now unconditional (UINavigator#closeOpenPanel).
--
-- WHY: with it off, closeOpenPanel() sent only three UI_Back taps, which is not enough to leave the galaxy
-- map, system map, FSS, station services or the codex. The next panel then opened on top of a stale focus
-- and the whole navigation went wrong - keystrokes typed into nothing, wrong tabs, targets never selected.
-- There is no situation in which a commander wants that, so the choice only ever confused people. The
-- column goes with the checkbox so the dead setting cannot be read again.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE global_settings
DROP
COLUMN autoExitUiBeforeOpeningAnotherWindow;
