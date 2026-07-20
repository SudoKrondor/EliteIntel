-- Data fix (V1.1): hot planets were persisted with locationType STAR.
--
-- ScanEvent's copy constructor never copied StarType, so getStarType() was always null and body
-- classification fell back entirely to a "surfaceTemperature > 1000" heuristic. A rocky body orbiting
-- close to a hot star routinely exceeds 1000 K, so those planets and moons were stored as stars. A
-- binary system with hot inner planets reported seven stars instead of two.
--
-- Recoverable here: planetClass is populated by the journal for planets and never for stars, so any
-- body stored as STAR that carries a planet class was misclassified. Planet classes are multi-word
-- phrases ("High metal content body", "Sudarsky class IV gas giant") while star classes are compact
-- codes with no space (M5, K9, TTS3, Y0), which makes the space a reliable discriminator. This matters
-- because a separate path (JumpCompletedSubscriber) writes a star's spectral class into planetClass,
-- so "has a planetClass" alone would wrongly demote real stars to planets.
--
-- PLANET vs MOON is taken from the ED naming convention "<parent> <letter>", confirmed against a
-- parent row that actually exists in the DB, the same technique 01012 used for rings.
--
-- NOT recoverable and deliberately left alone:
--   * starClass is empty on every pre-fix row (same copy-constructor bug). Re-scanning a body in-game
--     repopulates it now that the constructor is fixed.
--   * Genuinely cool stars (brown dwarfs below 1000 K) were stored as UNCLASSIFIED. Nothing in the row
--     distinguishes them from other unclassified bodies, so they are left for a re-scan.
--
-- Ordering matters: the MOON pass runs first and its rows stop matching the PLANET pass, which only
-- targets rows still typed STAR. Both passes are idempotent for that reason.

UPDATE location
SET json = json_set(json, '$.locationType', 'MOON')
WHERE json_extract(json, '$.locationType') = 'STAR'
  AND json_extract(json, '$.planetClass') GLOB '* *'
  AND locationName GLOB '* [a-z]'
  AND EXISTS (SELECT 1
              FROM location parent
              WHERE parent.locationName = substr(location.locationName, 1, length(location.locationName) - 2));

UPDATE location
SET json = json_set(json, '$.locationType', 'PLANET')
WHERE json_extract(json, '$.locationType') = 'STAR'
  AND json_extract(json, '$.planetClass') GLOB '* *';
