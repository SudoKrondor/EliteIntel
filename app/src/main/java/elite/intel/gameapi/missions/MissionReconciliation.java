package elite.intel.gameapi.missions;

import elite.intel.gameapi.journal.events.MissionsEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Which of our stored missions the game no longer has.
 *
 * <p>The {@code Missions} journal event is the game's own mission log, written in full at every game load:
 * everything the commander is holding, in {@code Active}. Anything we have stored that is not in that list
 * is a mission that ended while we were not watching, which is the ordinary case - a courier run handed in,
 * abandoned or expired with the app closed produces no event we ever see, so the entry simply stayed in the
 * database and kept its place on the HUD card for weeks.
 *
 * <p>Removing on {@code Complete} and {@code Failed} alone, as this used to, cannot catch that: a mission
 * dropped in an earlier session is in none of the three lists. Measured against a real diagnostics bundle,
 * the game reported {@code "Active":[], "Failed":[], "Complete":[]} - no missions at all - while the overlay
 * was still showing courier jobs, and nothing in that event was enough to clear one.
 *
 * <p>Pure and static so the rule can be exercised without a database or an event bus.
 */
public final class MissionReconciliation {

    private MissionReconciliation() {
    }

    /**
     * The ids to drop: stored missions absent from the snapshot's active list.
     *
     * <p>A mission accepted AFTER the snapshot was written is never dropped, however the two arrive here.
     * The snapshot describes one instant, and the journal replay at startup hands us that instant's mission
     * log alongside events from long after it - a mission accepted an hour later is missing from the list
     * because it did not exist yet, not because it ended. An unreadable or absent accept time counts as old,
     * the same reading {@link MassacreProgress} takes of one: a record from before we captured accept times
     * is by definition not one from the last few seconds.
     *
     * @param stored   the missions we hold, by id
     * @param snapshot the game's own mission log
     * @return the stale ids, never null; empty when nothing has ended or the snapshot cannot be dated
     */
    public static Set<Long> stale(Map<Long, MissionDto> stored, MissionsEvent snapshot) {
        if (stored == null || stored.isEmpty() || snapshot == null) return Set.of();

        Instant snapshotAt = instantOrNull(snapshot.getTimestamp());
        // Without a readable timestamp there is no way to tell a mission that ended from one accepted since,
        // and deleting is the half of this that cannot be undone.
        if (snapshotAt == null) return Set.of();

        Set<Long> stillHeld = activeIds(snapshot);
        Set<Long> stale = new TreeSet<>();
        for (Map.Entry<Long, MissionDto> entry : stored.entrySet()) {
            if (stillHeld.contains(entry.getKey())) continue;
            if (acceptedAfter(entry.getValue(), snapshotAt)) continue;
            stale.add(entry.getKey());
        }
        return stale;
    }

    private static Set<Long> activeIds(MissionsEvent snapshot) {
        if (snapshot.getActive() == null) return Set.of();
        return snapshot.getActive().stream()
                .map(MissionsEvent.Mission::getMissionID)
                .collect(Collectors.toSet());
    }

    private static boolean acceptedAfter(MissionDto mission, Instant snapshotAt) {
        Instant acceptedAt = mission == null ? null : instantOrNull(mission.getAcceptedAt());
        return acceptedAt != null && acceptedAt.isAfter(snapshotAt);
    }

    private static Instant instantOrNull(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) return null;
        try {
            return Instant.parse(timestamp);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
