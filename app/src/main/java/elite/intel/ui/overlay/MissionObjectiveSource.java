package elite.intel.ui.overlay;

import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.dto.MissionDto;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Projects the commander's accepted missions into a HUD objective, showing the
 * one expiring soonest.
 * <p>
 * Deliberately generic: it reports what a mission asks for, not how far along it
 * is, because progress means something different for every mission family and
 * only some families can supply it at all. A family that can do better gets its
 * own source at {@link HudObjective#PRIORITY_SPECIALISED} and outranks this card
 * when it applies. {@link MassacreObjectiveSource} is the worked example - it
 * draws a real kill bar from {@code MassacreProgress}, which the voice query
 * shares, so the bar and the spoken answer cannot disagree.
 */
public class MissionObjectiveSource implements HudObjectiveSource {

    private final MissionManager missionManager;

    public MissionObjectiveSource() {
        this(MissionManager.getInstance());
    }

    /**
     * Seam for tests.
     */
    MissionObjectiveSource(MissionManager missionManager) {
        this.missionManager = missionManager;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        MissionDto mission = missionManager.getMissions().values().stream()
                .filter(java.util.Objects::nonNull)
                .min(Comparator.comparing(m -> expiryOf(m).orElse(Instant.MAX)))
                .orElse(null);
        if (mission == null) return Optional.empty();

        List<HudRow> rows = new ArrayList<>();
        addTargetRow(rows, mission);
        if (mission.getReward() > 0) {
            rows.add(HudRow.of("REWARD", credits(mission.getReward())));
        }
        expiryRow(mission).ifPresent(rows::add);

        return Optional.of(new HudObjective(
                "mission:" + mission.getMissionId(),
                title(mission),
                subtitle(mission),
                rows));
    }

    // -- rows ------------------------------------------------------------------

    /**
     * The "what am I actually doing" row, which differs per mission family.
     * Kept deliberately small: anything not covered falls through to no row
     * rather than inventing a label.
     */
    private void addTargetRow(List<HudRow> rows, MissionDto mission) {
        if (mission.getKillCount() > 0) {
            String faction = mission.getMissionTargetFaction();
            rows.add(HudRow.of("KILLS REQUIRED", String.valueOf(mission.getKillCount())));
            if (faction != null && !faction.isBlank()) {
                rows.add(HudRow.of("TARGET", faction.toUpperCase()));
            }
        } else if (mission.getCommodityName() != null && !mission.getCommodityName().isBlank()) {
            String qty = mission.getCount() > 0 ? " x" + mission.getCount() : "";
            rows.add(HudRow.of("CARGO", mission.getCommodityName().toUpperCase() + qty));
        } else if (mission.getPassengerCount() > 0) {
            rows.add(HudRow.of("PASSENGERS", String.valueOf(mission.getPassengerCount())));
        }
    }

    private Optional<HudRow> expiryRow(MissionDto mission) {
        Optional<Instant> expiry = expiryOf(mission);
        if (expiry.isEmpty()) return Optional.empty();
        Duration left = Duration.between(Instant.now(), expiry.get());
        if (left.isNegative()) {
            return Optional.of(HudRow.of("EXPIRED", "-", HudRow.State.CRITICAL));
        }
        // Under six hours is the point where it starts driving decisions.
        HudRow.State state = left.toHours() < 6 ? HudRow.State.WARN : HudRow.State.NORMAL;
        return Optional.of(HudRow.of("EXPIRES", humanDuration(left), state));
    }

    // -- text ------------------------------------------------------------------

    private String title(MissionDto mission) {
        if (mission.getMissionDescription() != null && !mission.getMissionDescription().isBlank()) {
            return mission.getMissionDescription();
        }
        return mission.getMissionType() == null ? "MISSION" : mission.getMissionType().name();
    }

    private String subtitle(MissionDto mission) {
        StringBuilder sb = new StringBuilder();
        if (mission.getDestinationSystem() != null && !mission.getDestinationSystem().isBlank()) {
            sb.append(mission.getDestinationSystem());
        }
        if (mission.getDestinationStation() != null && !mission.getDestinationStation().isBlank()) {
            if (!sb.isEmpty()) sb.append(" - ");
            sb.append(mission.getDestinationStation());
        }
        if (sb.isEmpty() && mission.getFaction() != null) sb.append(mission.getFaction());
        return sb.isEmpty() ? null : sb.toString();
    }

    private static String credits(long amount) {
        return String.format("%,d cr", amount);
    }

    private static String humanDuration(Duration d) {
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        if (days > 0) return days + "d " + String.format("%02dh", hours);
        if (hours > 0) return hours + "h " + String.format("%02dm", minutes);
        return minutes + "m";
    }

    /**
     * Journal expiry timestamps are ISO-8601; anything unparseable sorts last.
     */
    private static Optional<Instant> expiryOf(MissionDto mission) {
        String raw = mission.getExpiry();
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            return Optional.of(Instant.parse(raw));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
