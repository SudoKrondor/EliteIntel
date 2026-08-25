-- Planetary approach announcements, split out of the route-announcement toggle.
-- On by default for everyone, existing commanders included: the approach briefing is the
-- behaviour they have today, and it is now silenced on its own switch rather than by
-- turning route announcements off.
alter table player
    add column is_planetary_approach_announcement_on boolean not null default 1;

update player
set is_planetary_approach_announcement_on = 1;
