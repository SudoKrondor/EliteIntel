-- Several game installations on Linux (docs/02-features/bindforge/linux-several-installations.md, Krondor,
-- 2026-11-21).
--
-- wine_prefix: the Wine prefix of an installation added by hand on Linux, the folder holding drive_c - an Epic or
-- Frontier-launcher copy run through Heroic, Lutris or Wine. Its own bindings and journal folders are inside it, and
-- nothing on the machine records where it is. NULL for every detected installation, whose prefix detection finds, and
-- on Windows, where every installation shares the Windows user's folders.
ALTER TABLE bindforge_installations ADD COLUMN wine_prefix TEXT;

-- game_folders_chosen_at: when the installation being played was last chosen, by the user or by following the game,
-- as yyyyMMddHHmmss local time - the form of the start stamp in a journal's name. A journal opened after it in another
-- installation's journal folder means the game started there. 0 means never chosen, so the newest journal is taken.
ALTER TABLE bindforge_settings ADD COLUMN game_folders_chosen_at INTEGER NOT NULL DEFAULT 0;
