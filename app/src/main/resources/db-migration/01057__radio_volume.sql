-- A level of its own for the radio: station, police and NPC chatter is voiced by the radio engine at
-- the same volume as the ship's own voice, and a commander who wants the ship loud and the traffic
-- quiet (or the reverse) had no way to say so. 0 to 100 percent, the same scale as voiceVolume, and
-- it starts level with the shipped speech volume so nothing changes until the slider is moved.
alter table game_session
    add column radioVolume integer not null default 100;
