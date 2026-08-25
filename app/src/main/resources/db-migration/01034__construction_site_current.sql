-- Which construction site the commander is actually working on right now.
--
-- "The construction site" is never a lookup by place: a squadron can be colonising several systems at once,
-- and one system can hold more than one depot. It is the build they were last standing on - so landing at
-- site B while site A is current makes B current, and A simply stops being volunteered.
--
-- An explicit flag rather than "the row with the newest visitedAt", because the commander can also say they
-- are done. That has to leave the HUD empty rather than promote whichever build they happened to visit
-- before this one, and it must NOT delete anything: the manifests are worth keeping, and docking at any
-- depot makes that one current again.

alter table construction_site
    add column isCurrent integer not null default 0;

-- Existing rows predate the flag. Promote the most recently visited one so a commander mid-haul does not
-- lose their card to this migration; the next manifest from any pad would have set it anyway.
update construction_site
set isCurrent = 1
where marketId = (select marketId from construction_site order by visitedAt desc limit 1);
