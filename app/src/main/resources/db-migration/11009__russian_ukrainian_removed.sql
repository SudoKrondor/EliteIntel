-- Russian and Ukrainian are gone as app languages, along with their name columns.
--
-- A commander who had either one selected starts in English. SystemSession.getLanguage already reads
-- a value it does not recognise as English. This makes the row say so too, so the settings tab and the
-- support bundle show the language that is actually in use.
--
-- The Russian game client is still recognised, but only to voice its radio chatter. Names learned from it
-- were filed under name_ru and commodity_ru, and nothing reads those any more, so they go with the rest.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
UPDATE game_session
SET aiLanguage = 'EN'
WHERE aiLanguage IN ('RU', 'UK');

DELETE
FROM material_aliases
WHERE lang IN ('ru', 'uk');

ALTER TABLE material_names
    DROP COLUMN name_ru;
ALTER TABLE material_names
    DROP COLUMN name_uk;

ALTER TABLE commodities
    DROP COLUMN commodity_ru;
ALTER TABLE commodities
    DROP COLUMN commodity_uk;

ALTER TABLE sub_system
    DROP COLUMN label_ru;
ALTER TABLE sub_system
    DROP COLUMN label_uk;
