-- The destination reminder carried only a star system and a spoken sentence. The HUD overlay shows
-- the reminder as an objective card when nothing else is on screen, and a card needs the port and
-- who to see there as data - the sentence is localized prose written for the voice, not something a
-- card can decompose.
--
-- contact stores the ReminderContact enum name, never a label: the display string is looked up in
-- the caller's language at paint time, so a commander who switches language does not keep reading
-- the old one out of the database.

alter table destination_reminder
    add column stationName text default null;
alter table destination_reminder
    add column contact text default null;
