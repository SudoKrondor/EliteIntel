ALTER TABLE game_session
    ADD COLUMN audioInputDevice VARCHAR(256);

ALTER TABLE game_session
    ADD COLUMN audioOutputDevice VARCHAR(256);
