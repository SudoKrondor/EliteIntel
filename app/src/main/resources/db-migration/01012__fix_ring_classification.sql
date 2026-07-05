-- Data fix (V1.1): planetary rings were persisted with the wrong locationType.
--
-- SAASignalsFoundSubscriber built a correctly-typed PLANETARY_RING record but then a trailing
-- save() (and a later Scan event, which classified rings as MOON) overwrote it. As a result rings
-- ended up stored as MOON / PLANET / UNCLASSIFIED etc. This one-shot corrects the classification
-- that is recoverable from data already in the DB.
--
-- Recoverable here (deterministic from the body name / existing rows):
--   * locationType   -> PLANETARY_RING
--   * parentBodyName -> the body name with the trailing " X Ring" (7 chars) stripped
--   * hasRings=true  -> on any parent body that owns at least one ring
--
-- NOT recoverable: the ring's mining reserves (materials) and saaSignals were overwritten with
-- empty lists and the source journals no longer exist. Re-scanning a ring in-game repopulates them
-- (the fixed subscriber now persists them). This script intentionally leaves those fields alone.
--
-- Ring bodies are matched by the ED naming convention "<parent> <letter> Ring" via GLOB, which is
-- reliable: stations, carriers and other bodies are not named that way. Additive/idempotent.

UPDATE location
SET json = json_set(json,
                    '$.locationType', 'PLANETARY_RING',
                    '$.parentBodyName', substr(locationName, 1, length(locationName) - 7))
WHERE locationName GLOB '* [A-Z] Ring';

UPDATE location
SET json = json_set(json, '$.hasRings', json('true'))
WHERE (systemAddress || '|' || locationName) IN
      (SELECT r.systemAddress || '|' || substr(r.locationName, 1, length(r.locationName) - 7)
       FROM location r
       WHERE r.locationName GLOB '* [A-Z] Ring');
