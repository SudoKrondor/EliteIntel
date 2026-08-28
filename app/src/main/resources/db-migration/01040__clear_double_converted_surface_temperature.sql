-- Clear surface temperatures that were converted to Celsius in place, once per approach.
--
-- ApproachBodySubscriber narrated the surface temperature in Celsius but wrote that Celsius value
-- back onto the LocationDto before saving it, while every other reader treats the stored figure as
-- the journal's Kelvin. So each approach of the same body subtracted another 273 from the stored
-- value and persisted the result. Approach a body six times and a real 174 K reads as -1464, which
-- the companion then narrates as a surface temperature of minus 1464 Celsius.
--
-- The subtraction is not invertible: only the total is stored, never how many approaches went into
-- it, and a body that started above 273 K reaches a negative total by a different count than a cold
-- one. Guessing the smallest count that turns the value positive would put a plausible but wrong
-- number in front of the commander, which is the exact failure being fixed here. So a corrupted row
-- is set to 0 -- the "no data" value the readers already understand -- and the body reports no
-- temperature until the next surface scan writes a fresh Kelvin reading.
--
-- Only negative totals are detectable. A hot body whose running total is still positive is
-- indistinguishable from a correct reading and is deliberately left alone.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
UPDATE location
SET json = json_set(json, '$.surfaceTemperature', 0)
WHERE json IS NOT NULL
  AND json_valid(json)
  AND json_extract(json, '$.surfaceTemperature') < 0;
