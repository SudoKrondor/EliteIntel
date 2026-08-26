-- How long the input executor pauses after each keystroke it sends to the game.
--
-- The pause was hard-coded at 99-201ms, which is comfortable on hardware that keeps up with the game.
-- On slower machines the game misses keystrokes in the middle of a sequence, so the pause is now a
-- setting on the Binding Profile tab, running from 99ms (FAST, the shipped behaviour) to 199ms (SLOW).
-- The executor keeps its own randomised spread on top of this floor.
--
-- 99 is the value in use today, so existing and fresh installations both keep the current pacing.
ALTER TABLE game_session
    ADD COLUMN keyInputDelayMs INTEGER NOT NULL DEFAULT 100;

UPDATE game_session
SET keyInputDelayMs = 100;
