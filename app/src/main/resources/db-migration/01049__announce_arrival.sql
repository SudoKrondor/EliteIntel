-- Whether arriving in a system is announced ("Arrived at X", "Arrived at final destination: X").
--
-- The route announcements are already granular - destination, traffic, deaths, remaining jumps, fuel
-- stars - but the arrival itself was welded to the master route toggle, so a commander who wanted just
-- the count of jumps left had to hear "Arrived at" every jump to get it. This is the missing switch.
-- Reminders left for a system are not gated by it: they are spoken on arrival whatever it says.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
alter table global_settings
    add column announceArrival boolean default true;
