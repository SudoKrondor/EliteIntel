alter table global_settings
    add column announceJumpRoute boolean default true;

alter table global_settings
    add column announceJumpTraffic boolean default true;

alter table global_settings
    add column announceJumpDeaths boolean default true;

alter table global_settings
    add column announceRemainingJumps boolean default true;

alter table global_settings
    add column announceFuelAvailable boolean default true;