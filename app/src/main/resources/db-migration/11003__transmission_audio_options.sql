-- Optional transmission treatment and Supertonic output gain. Existing radio audio retains its current filter.
alter table game_session add column transmissionTones integer not null default 0;
alter table game_session
    add column enhancedRadioEffect integer not null default 0;
alter table game_session
    add column effectsOnRadio integer not null default 0;
alter table game_session add column effectsOnVegaAway integer not null default 0;
alter table game_session add column supertonicBoost integer not null default 0;
