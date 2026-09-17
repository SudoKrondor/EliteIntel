-- A mouse button as a second push-to-talk trigger, beside the controller button. A commander who
-- flies on a HOTAS but walks and drives on mouse and keyboard has no controller in hand on foot,
-- and the app cannot hear a keyboard key while the game holds the focus - it can read the OS-wide
-- mouse button state. SDL 0-based index (1 = middle, 3 = back, 4 = forward), -1 = none.
alter table game_session
    add column pushToTalkMouseButton integer not null default -1;
