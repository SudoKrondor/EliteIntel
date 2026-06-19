alter table game_session
    add column noiseReductionEnabled boolean default false;

alter table game_session
    add column noiseReductionStrength integer default 1;
