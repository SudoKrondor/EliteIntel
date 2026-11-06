-- A new install starts asleep, so a microphone next to loud speakers or a television cannot trigger VEGA while the
-- commander is still setting up. The startup notice says she is asleep and how to wake her, and once woken she stays
-- awake across restarts.
--
-- The migrations cannot tell a new database from an old one, so this asks the same question as SetupCheck: no local
-- model named and no cloud key stored. A commander with either one has set the app up and keeps the state they chose.
UPDATE game_session
SET sleepWake = 1
WHERE TRIM(COALESCE(lmStudioCommandModel, '')) = ''
  AND TRIM(COALESCE(encryptedLLMKey, '')) = '';
