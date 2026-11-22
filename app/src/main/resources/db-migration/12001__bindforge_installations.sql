-- The game installations BindForge knows about, and how it came to know about them.
--
-- Detection re-runs every launch, but the list is not rebuilt from it. Rows persist because things hang off
-- them: device records are keyed by installation, and a row also carries facts detection cannot supply -
-- that the user added it by hand, or relocated it after moving the game.
--
-- root_path is the installation folder, the one holding Products. It is unique: the same folder reached by
-- two routes - two Steam library entries, or a symlink - is one installation, not two.
--
-- added_by_hand separates the two ways a row arrives. It matters because the storefronts are not equally
-- findable: Steam and Epic keep manifests, so a moved install is still found, while nothing records where a
-- Frontier-launcher install went. For those, adding by hand is the only mechanism, and a rescan must never
-- treat a hand-added row as stale merely because detection did not produce it.
--
-- is_missing is a state, never a reason to delete. An unmounted drive and an uninstall look identical from
-- here, and dropping the row would take its device records with it, so a row whose folder has gone stays in
-- the list marked missing and the user decides.

CREATE TABLE IF NOT EXISTS bindforge_installations
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    storefront    TEXT    NOT NULL,
    root_path     TEXT    NOT NULL UNIQUE,
    added_by_hand BOOLEAN NOT NULL DEFAULT 0,
    is_missing    BOOLEAN NOT NULL DEFAULT 0
);
