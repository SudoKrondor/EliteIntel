-- Two opt-in combat remarks VEGA phrases with the LLM, both off until the commander turns them on.
--
-- aiRespondsToPirateScan - a sarcastic retort when a pirate starts scanning the cargo hold. It is a
-- dependent of the radio-transmissions toggle and says nothing while the radio is off.
-- aiBountyKillGloat - a gloat over the wreck when a kill pays a bounty, after the kill-confirmed callout.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
alter table global_settings
    add column aiRespondsToPirateScan boolean default false;

alter table global_settings
    add column aiBountyKillGloat boolean default false;
