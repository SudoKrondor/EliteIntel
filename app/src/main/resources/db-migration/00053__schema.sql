BEGIN;

ALTER TABLE game_session
    ADD COLUMN ollamaAddress VARCHAR(255) NOT NULL DEFAULT 'http://localhost:11434/api/chat';
ALTER TABLE game_session
    ADD COLUMN ollamaCommandModel VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE game_session
    ADD COLUMN ollamaQueryModel VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE game_session
    ADD COLUMN lmStudioAddress VARCHAR(255) NOT NULL DEFAULT 'http://localhost:1234/v1/chat/completions';
ALTER TABLE game_session
    ADD COLUMN lmStudioCommandModel VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE game_session
    ADD COLUMN lmStudioQueryModel VARCHAR(255) NOT NULL DEFAULT '';

UPDATE game_session
SET lmStudioAddress      = localLlmAddress,
    lmStudioCommandModel = COALESCE(localLlmCommandModel, ''),
    lmStudioQueryModel   = COALESCE(localLlmQueryModel, '')
WHERE localLlmAddress LIKE '%1234/v1/chat/completions%'
   OR (localLlmProvider = 'LMSTUDIO' AND localLlmAddress NOT LIKE '%11434/api/chat%');

UPDATE game_session
SET ollamaAddress      = localLlmAddress,
    ollamaCommandModel = COALESCE(localLlmCommandModel, ''),
    ollamaQueryModel   = COALESCE(localLlmQueryModel, '')
WHERE localLlmAddress LIKE '%11434/api/chat%'
   OR (localLlmProvider = 'OLLAMA' AND localLlmAddress NOT LIKE '%1234/v1/chat/completions%');

COMMIT;
