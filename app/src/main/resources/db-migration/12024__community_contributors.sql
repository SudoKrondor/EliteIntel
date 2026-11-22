-- How many installs stand behind the downloaded massacre sets, as the nightly job publishes it in the
-- file's "contributors". Shown on the Massacre Sets page as "Commanders contributing". Null until a download
-- of a file that carries it.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
alter table community_sync
    add column contributors INTEGER;
