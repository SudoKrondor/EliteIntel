-- Keep the original 20% choice when upgrading and give tones a quieter starting level.
alter table game_session add column transmissionToneVolume integer not null default 35;
alter table game_session add column supertonicBoostPercent integer not null default 0;
update game_session set supertonicBoostPercent = 20 where supertonicBoost = 1;
