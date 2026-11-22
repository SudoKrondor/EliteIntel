-- useFsdToggle - the commander's choice of how supercruise and hyperspace are engaged. Off by default.
-- Off, the supercruise command presses the game's Supercruise control and the hyperspace command its
-- Hyperspace control, so each does only what it says. On, both press the game's Toggle Frame Shift Drive
-- (HyperSuperCombination), and the game decides from the FSD target which of the two happens.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
alter table global_settings
    add column useFsdToggle boolean default false;
