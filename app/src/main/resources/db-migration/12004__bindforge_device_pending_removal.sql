-- Device entries Apply has taken out of the master but not yet out of the installations. Added 2026-10-04.
--
-- Apply adds and updates only. So when a draft that removed a device is applied, the device leaves the master
-- while its DeviceMappings.xml entry and its .buttonMap stay in every installation, until whatever removes them
-- (CLEAR) has run. Held only in memory, that list would be lost to a crash or a failed cleanup, after which
-- nothing in BindForge would remember owning those entries - and the startup check would read them as the
-- user's own edits.
--
-- The same idea as previous_name on the master, which keeps a rename's old name until every installation's
-- .buttonMap has been renamed. A row is added in the same transaction that applies the draft, cleared by
-- whatever removes the files, and cleared too if a later Apply puts a device of that name back in the master,
-- so a cleanup never removes an entry the user is using again.
CREATE TABLE IF NOT EXISTS bindforge_device_pending_removal
(
    device_name TEXT NOT NULL PRIMARY KEY
);
