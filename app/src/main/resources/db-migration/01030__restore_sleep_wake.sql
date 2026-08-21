-- Restores the Sleep/Wake gate that 01027 dropped, at commander request.
--
-- Sleep/Wake is the microphone gate for the hands-free commander: with the gate closed the STT
-- pipeline discards every transcript instead of routing it, so the companion cannot be talked to
-- and cannot be triggered by room noise. It is the AI tab button, and only that button - there is
-- deliberately no wake phrase, because a sleeping companion that still listens for one is not
-- asleep.
--
-- Only meaningful while push-to-talk is off. With push-to-talk on, the mapped button is already the
-- only thing that opens the microphone, so there is nothing left for Sleep/Wake to gate and the
-- button that drives it is disabled.
--
--   sleepWake - 1 = sleeping (gate closed, transcripts discarded), 0 = listening. Persisted, so a
--               commander who put her to sleep finds her asleep next launch; startup says so out
--               loud, because an app that silently ignores you is indistinguishable from a broken
--               one. Named for the feature rather than the old privacyModeOn, which was named for
--               an earlier feature the column outlived.

ALTER TABLE game_session
    ADD COLUMN sleepWake INTEGER NOT NULL DEFAULT 0;
