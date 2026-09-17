-- Cargo-scoop pickup announcements on their own switch. Until now every MaterialCollected
-- was spoken unconditionally, so a commander scooping fragments got a running commentary
-- with no way to silence it short of muting everything. On by default for everyone,
-- existing commanders included: that is the behaviour they have today.
alter table player
    add column is_cargo_scoop_pickup_announcement_on boolean not null default 1;

update player
set is_cargo_scoop_pickup_announcement_on = 1;
