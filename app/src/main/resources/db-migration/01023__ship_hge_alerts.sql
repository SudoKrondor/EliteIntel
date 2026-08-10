-- Per-ship opt-in for High Grade Emissions material alerts.
--
-- Per ship rather than global because the decision is about the hull the commander is flying: an
-- exploration or combat build carries no spare material capacity and has no use for being told a
-- Very Rare manufactured drop is nearby, while a dedicated grinder wants every one of them.
--
-- Defaults to false so an existing installation upgrading into this migration stays exactly as quiet
-- as it was, and the commander turns the alerts on per hull when they want them.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
ALTER TABLE ship_settings
    ADD COLUMN hgeAlerts BOOLEAN NOT NULL DEFAULT FALSE;
