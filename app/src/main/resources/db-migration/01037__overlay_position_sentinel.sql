-- Retire -1 as the "overlay never positioned" sentinel.
--
-- A screen coordinate is legitimately negative on both platforms. A monitor placed left of or above
-- the primary one starts at a negative offset, and a window nudged past the top or left edge sits at
-- a negative coordinate on its own. While -1 meant "unset", the guards that restore a stored position
-- could not tell such a coordinate from "unset" -- an overlay parked at x=-3 came back horizontally
-- centred with only its y remembered, on every launch, with nothing to say why.
--
-- The sentinel is now Integer.MIN_VALUE, which is outside any coordinate a display can produce. Rows
-- still carrying the old pair are moved onto it. Only the pair is matched: a row with one real
-- coordinate and one -1 cannot come from saveLayout, which always writes both together, and a row
-- with a real position must not be touched. A commander who genuinely left the card at exactly
-- (-1,-1) loses that placement once and the overlay opens where it always did.
--
-- The column DEFAULT of -1 is left alone. Every write binds these columns explicitly from the bean,
-- whose default now carries the new sentinel, so the schema default is never the value that lands in
-- a row -- and rebuilding a forty-column table to restate it would risk more than it settles.
--
-- NOTE: no semicolon may appear inside these comments. Migrations are split on a semicolon at end of
-- line before comments are stripped, so one here would cut the file mid-comment and hand SQLite a
-- statement with no SQL in it.
UPDATE game_session
SET overlayX = -2147483648,
    overlayY = -2147483648
WHERE overlayX = -1
  AND overlayY = -1;
