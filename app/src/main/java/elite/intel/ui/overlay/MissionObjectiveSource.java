package elite.intel.ui.overlay;

import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Supplier;

/**
 * Projects the commander's accepted missions into a HUD objective: one featured
 * mission, plus what the rest of the stack is worth.
 * <p>
 * Missions are taken in stacks - eight courier runs from the same board, a wall
 * of massacre contracts - so the card has two jobs. It has to name the drop the
 * commander is flying to <em>now</em>, and it has to say how much work is still
 * outstanding behind it.
 *
 * <h2>Which mission is featured</h2>
 * In order:
 * <ol>
 *   <li><b>The plotted route's destination.</b> A plotted route is the
 *       commander stating where they are going, and it beats every guess this
 *       class could make. A mission whose destination system is the end of the
 *       route is the one being flown.</li>
 *   <li><b>The current system.</b> On arrival the game clears the route, and the
 *       mission that was being flown is the one whose cargo is about to be
 *       handed over - so it stays on the card through the docking rather than
 *       being replaced by an unrelated one.</li>
 *   <li><b>{@link #GAME_LIST_ORDER}</b>, otherwise.</li>
 * </ol>
 * Ties inside a tier fall through to the same order, so the card never depends
 * on map iteration order - which is what it used to do, by sorting a stack of
 * missions on an expiry field that nothing ever populated.
 *
 * <h2>Generic on purpose</h2>
 * It reports what a mission asks for, not how far along it is, because progress
 * means something different for every mission family and only some families can
 * supply it at all. A family that can do better gets its own source at
 * {@link HudObjective#PRIORITY_SPECIALISED} and outranks this card when it
 * applies. {@link MassacreObjectiveSource} is the worked example - it draws a
 * real kill bar from {@code MassacreProgress}, which the voice query shares, so
 * the bar and the spoken answer cannot disagree.
 */
public class MissionObjectiveSource implements HudObjectiveSource {

    /**
     * The order the game's transactions panel appears to list missions in:
     * newest accepted first.
     * <p>
     * The journal never states this order. {@code Missions} reports the active
     * set at login and nothing else reports it at all, so it is inferred from
     * observation - with a stack of ten couriers accepted over seven minutes,
     * the game's top entry was the last one accepted. It is deliberately one
     * comparator so that a counter-observation is a one-line correction here
     * rather than a rewrite.
     * <p>
     * Journal timestamps are fixed-width UTC ISO-8601, so comparing them as
     * strings is comparing them as instants. Mission IDs rise over time too and
     * break ties - and carry missions with no accepted time, which sort last.
     */
    static final Comparator<MissionDto> GAME_LIST_ORDER =
            Comparator.comparing(MissionObjectiveSource::acceptedAt, Comparator.reverseOrder())
                    .thenComparing(MissionDto::getMissionId, Comparator.reverseOrder());

    private final MissionManager missionManager;
    private final Supplier<String> routeDestination;
    private final Supplier<String> currentSystem;

    public MissionObjectiveSource() {
        this(MissionManager.getInstance(),
                () -> ShipRouteManager.getInstance().getDestination(),
                () -> PlayerSession.getInstance().getPrimaryStarName());
    }

    /**
     * Seam for tests.
     */
    MissionObjectiveSource(MissionManager missionManager,
                           Supplier<String> routeDestination,
                           Supplier<String> currentSystem) {
        this.missionManager = missionManager;
        this.routeDestination = routeDestination;
        this.currentSystem = currentSystem;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        // A malformed row deserialises to null, so the stack is cleaned once here
        // and every method below it can take a mission for granted.
        List<MissionDto> missions = missionManager.getMissions().values().stream()
                .filter(Objects::nonNull)
                .toList();

        return featured(missions, routeDestination.get(), currentSystem.get())
                .map(featured -> new HudObjective(
                        "mission:" + featured.getMissionId(),
                        title(featured),
                        subtitle(featured),
                        rows(featured, missions)));
    }

    // -- selection -------------------------------------------------------------

    /**
     * The mission the card is about, or empty when there are none.
     * <p>
     * Pure, so the precedence between route, current system and list order can be
     * tested without a database, a route or a position.
     *
     * @param missions the active stack, already free of nulls
     */
    static Optional<MissionDto> featured(List<MissionDto> missions,
                                         String routeDestination,
                                         String currentSystem) {
        List<MissionDto> ordered = missions.stream().sorted(GAME_LIST_ORDER).toList();
        if (ordered.isEmpty()) return Optional.empty();

        return firstBoundFor(ordered, routeDestination)
                .or(() -> firstBoundFor(ordered, currentSystem))
                .or(() -> Optional.of(ordered.getFirst()));
    }

    /**
     * The highest-listed mission heading for {@code system}, so that a system
     * holding several of them always resolves to the same one.
     */
    private static Optional<MissionDto> firstBoundFor(List<MissionDto> ordered, String system) {
        if (system == null || system.isBlank()) return Optional.empty();
        return ordered.stream().filter(mission -> system.equalsIgnoreCase(mission.getDestinationSystem())).findFirst();
    }

    // -- rows ------------------------------------------------------------------

    /**
     * The card's rows, in reading order.
     * <p>
     * Worst case is seven: kills, target, reward, stack, stack reward, same
     * system, expiry. The renderer keeps eight ({@code MAX_ROWS} in hud.h) and
     * silently drops the rest, so an eighth row added here would cost the expiry
     * line, which is the only row with a deadline attached.
     */
    private List<HudRow> rows(MissionDto featured, List<MissionDto> missions) {
        List<HudRow> rows = new ArrayList<>();
        addTargetRow(rows, featured);
        if (featured.getReward() > 0) {
            rows.add(HudRow.of("REWARD", credits(featured.getReward())));
        }
        addStackRows(rows, featured, missions);
        addSameSystemRow(rows, featured, missions);
        expiryRow(featured).ifPresent(rows::add);
        return rows;
    }

    /**
     * What is still outstanding in the featured mission's own family, because
     * that is what a commander means by a stack: eight couriers run as one job,
     * and a massacre contract sitting alongside them is not part of it.
     */
    private void addStackRows(List<HudRow> rows, MissionDto featured, List<MissionDto> missions) {
        List<MissionDto> stack = missions.stream()
                .filter(mission -> Objects.equals(mission.getMissionType(), featured.getMissionType()))
                .toList();
        if (stack.size() < 2) return;

        rows.add(HudRow.of("STACK", stack.size() + " MISSIONS"));
        long total = stack.stream().mapToLong(MissionDto::getReward).sum();
        if (total > 0) {
            rows.add(HudRow.of("STACK REWARD", credits(total)));
        }
    }

    /**
     * How many missions end at the featured one's destination system.
     * <p>
     * Deliberately not filtered by family: two deliveries to one system are one
     * trip whether or not they came off the same board.
     */
    private void addSameSystemRow(List<HudRow> rows, MissionDto featured, List<MissionDto> missions) {
        String system = featured.getDestinationSystem();
        if (system == null || system.isBlank()) return;

        long sameSystem = missions.stream()
                .filter(mission -> system.equalsIgnoreCase(mission.getDestinationSystem()))
                .count();
        if (sameSystem > 1) {
            rows.add(HudRow.of("SAME SYSTEM", sameSystem + " MISSIONS"));
        }
    }

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
     * Empty rather than null so the comparator can sort on it; an unset accepted
     * time sorts last under {@link #GAME_LIST_ORDER}.
     */
    private static String acceptedAt(MissionDto mission) {
        return mission.getAcceptedAt() == null ? "" : mission.getAcceptedAt();
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
