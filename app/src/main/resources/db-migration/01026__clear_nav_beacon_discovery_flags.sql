-- Clears first-discovery claims that a nav beacon invented.
--
-- Scanning a nav beacon dumps every body in the system, and each Scan reports WasDiscovered:false for bodies
-- charted decades ago: the flag says how that scan learned about the body, not who found it first. The
-- subscriber stored the inverse as ourDiscovery=true, so the commander was credited with discovering bodies in
-- long-settled systems. That is not only cosmetic: ourDiscovery feeds the exobiology first-discovery bonus, so
-- the reported value of bio samples was inflated too. ScanEventSubscriber no longer records these flags for a
-- NavBeaconDetail scan; this repairs the rows written before that.
--
-- The rule is "a body in an inhabited system was not discovered by us". Nav beacons only exist in populated
-- systems, so population is the evidence the journal itself contradicts. Measured against a real 24644-row
-- database this clears 186 rows out of 13029 carrying ourDiscovery=true, and every one of the 130 rows holding
-- the journal's impossible combination (ourDiscovery=true with weMappedIt=false, meaning we discovered it but
-- somebody else had already mapped it) falls inside that set.
--
-- weMappedIt is deliberately left alone: surface-mapping a body in an inhabited system is perfectly possible,
-- so unlike a first discovery it is not self-contradictory, and nothing pays out on it.
--
-- Population is read from a sibling row, because only the primary star carries it (written on arrival by
-- FSDJump). A system whose star row was never stored therefore goes untouched, which is the right way to fail:
-- with no evidence the system is inhabited, there is nothing to contradict the claim.
--
-- json_set with json('false') writes a real JSON boolean; passing 0 would store a number where the DTO expects
-- a boolean.

UPDATE location
SET json = json_set(json, '$.ourDiscovery', json('false'))
WHERE json_extract(json, '$.ourDiscovery') = 1
  AND primaryStar IN (SELECT primaryStar
                      FROM location
                      WHERE COALESCE(json_extract(json, '$.population'), 0) > 0);
