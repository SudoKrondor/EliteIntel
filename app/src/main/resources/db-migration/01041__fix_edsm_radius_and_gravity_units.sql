-- Convert EDSM-sourced body radii from kilometres to metres, and undo the surface gravity they inflated.
--
-- EDSM reports a body radius in km while the journal reports it in metres. JumpCompletedSubscriber stored
-- EDSM's figure as-is and passed it to GravityCalculator, which is documented in metres, so every body
-- learned from EDSM ended up with a radius 1000x too small and a surface gravity 1000x1000 too large --
-- e.g. a 0.36g world stored as 355396.56. Bodies learned from a journal Scan were always correct, so the
-- table holds both units at once.
--
-- Radius is the discriminator, and the two populations do not overlap: the largest gas giant is about
-- 78000 km, the smallest body about 150 km, so a radius below 100000 is kilometres and one above it is
-- metres. On the author's 31244-row table this agrees with the independent gravity test (a real surface
-- gravity never exceeds a few tens of g, the inflated ones all exceed 1000) on every single row, with the
-- nearest values 77738 against 146666 and 35.77 against 1328.13.
--
-- Unlike the double-converted temperature in migration 01040 this IS invertible: the error is a fixed
-- factor rather than a count of repeats, so the original values are recovered exactly.
--
-- Gravity is only divided where one was actually computed. About 17 rows carry a km radius but no gravity
-- (no mass from EDSM), and they still need the radius corrected.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
UPDATE location
SET json = json_set(json, '$.radius', json_extract(json, '$.radius') * 1000.0)
WHERE json IS NOT NULL
  AND json_valid(json)
  AND json_extract(json, '$.radius') > 0
  AND json_extract(json, '$.radius') < 100000;

UPDATE location
SET json = json_set(json, '$.gravity', json_extract(json, '$.gravity') / 1000000.0)
WHERE json IS NOT NULL
  AND json_valid(json)
  AND json_extract(json, '$.gravity') > 1000;
