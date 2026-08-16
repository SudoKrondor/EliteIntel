-- Removes the Sleep/Wake feature from the schema.
--
-- Sleep/Wake was an artifact of the original always-listening design: the commander told the
-- companion to stop listening and later woke her again, by voice, by a button in the AI tab, or by
-- a controller button mapped to "toggle". Push-to-talk and the built-in noise gate cover the same
-- ground, and Sleep/Wake had fallen out of use, so it is gone from the UI, the settings, the STT
-- pipeline and the tool set as of this migration.
--
-- Both columns dropped here have had their read paths removed in the same change set:
--
--   privacyModeOn        - the sleeping flag itself, reached only through the SystemSession pair
--                          isSleepingModeOn()/stopStartListening(), both deleted. Named for an
--                          earlier "privacy mode" the flag outlived.
--   pushToTalkToggleMode - chose between the two push-to-talk behaviours, toggle-to-sleep and
--                          hold-to-talk. With sleep gone there is one behaviour left, so the
--                          setting has nothing to choose between and the MODE control that drove
--                          it is off the Push To Talk settings tab.

ALTER TABLE game_session
DROP
COLUMN privacyModeOn;

ALTER TABLE game_session
DROP
COLUMN pushToTalkToggleMode;
