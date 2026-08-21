package elite.intel.ui.overlay;

import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MissionCargo;
import elite.intel.gameapi.missions.MissionSelection;
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
 * Two rules, in order:
 * <ol>
 *   <li><b>The plotted route's destination.</b> A plotted route is the
 *       commander stating where they are going, and it beats every guess this
 *       class could make. A mission whose destination system is the end of the
 *       route is the one being flown.</li>
 *   <li><b>{@link MissionSelection#EXPIRY_ORDER}</b> when no route is plotted, or
 *       when the route ends somewhere no mission does: with nothing stating
 *       where the ship is going, the mission that matters is the one running
 *       out first.</li>
 * </ol>
 * Ties inside either rule fall through to {@link MissionSelection#GAME_LIST_ORDER},
 * so the card never depends on map iteration order - which is what it used to do,
 * by sorting a stack of missions on an expiry field that nothing ever populated.
 *
 * <h2>Generic on purpose</h2>
 * It reports what a mission asks for, because progress means something different
 * for every mission family and only some families can supply it at all. A family
 * that can do better gets its own source at
 * {@link HudObjective#PRIORITY_SPECIALISED} and outranks this card when it
 * applies. {@link MassacreObjectiveSource} is the worked example - it draws a
 * real kill bar from {@code MassacreProgress}, which the voice query shares, so
 * the bar and the spoken answer cannot disagree.
 * <p>
 * The one exception is cargo, which is measured here rather than in a source of
 * its own: a source-and-return mission is filled by buying on the open market, so
 * the journal reports no progress for it at all and there is nothing for a
 * specialised card to read. See {@code addCargoProgressRow}.
 */
public class MissionObjectiveSource implements HudObjectiveSource {

    private final MissionManager missionManager;
    private final Supplier<String> routeDestination;
    private final Supplier<GameEvents.CargoEvent> shipCargo;

    public MissionObjectiveSource() {
        this(MissionManager.getInstance(),
                () -> ShipRouteManager.getInstance().getDestination(),
                () -> PlayerSession.getInstance().getShipCargo());
    }

    /**
     * Seam for tests that do not care what is in the hold.
     */
    MissionObjectiveSource(MissionManager missionManager,
                           Supplier<String> routeDestination) {
        this(missionManager, routeDestination, () -> null);
    }

    /**
     * Seam for tests.
     */
    MissionObjectiveSource(MissionManager missionManager,
                           Supplier<String> routeDestination,
                           Supplier<GameEvents.CargoEvent> shipCargo) {
        this.missionManager = missionManager;
        this.routeDestination = routeDestination;
        this.shipCargo = shipCargo;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        // A malformed row deserialises to null, so the stack is cleaned once here
        // and every method below it can take a mission for granted.
        List<MissionDto> missions = missionManager.getMissions().values().stream()
                .filter(Objects::nonNull)
                .toList();

        return featured(missions, routeDestination.get())
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
     * Pure, so the precedence between the plotted route and the expiry order can
     * be tested without a database or a route.
     *
     * @param missions         the active stack, already free of nulls
     * @param routeDestination the end of the plotted route, or null when none is plotted
     */
    static Optional<MissionDto> featured(List<MissionDto> missions, String routeDestination) {
        List<MissionDto> ordered = missions.stream().sorted(MissionSelection.EXPIRY_ORDER).toList();
        if (ordered.isEmpty()) return Optional.empty();

        return firstBoundFor(ordered, routeDestination)
                .or(() -> Optional.of(ordered.getFirst()));
    }

    /**
     * The soonest-expiring mission heading for {@code system}, so that a system
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
     * Worst case is seven either way: kills, target, reward, stack, stack reward,
     * same system, expiry for a combat mission, and the same with cargo and its
     * progress bar in place of kills and target for a delivery. The renderer keeps
     * eight ({@code MAX_ROWS} in hud.h) and silently drops the rest, so an eighth
     * row added here would cost the expiry line, which is the only row with a
     * deadline attached.
     */
    private List<HudRow> rows(MissionDto featured, List<MissionDto> missions) {
        List<HudRow> rows = new ArrayList<>();
        addTargetRow(rows, featured, missions);
        if (featured.getReward() > 0) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.reward"), HudText.credits(featured.getReward())));
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

        rows.add(HudRow.of(HudText.get("overlay.card.row.stack"),
                HudText.plural("overlay.card.value.missionCount", stack.size())));
        long total = stack.stream().mapToLong(MissionDto::getReward).sum();
        if (total > 0) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.stackReward"), HudText.credits(total)));
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
            rows.add(HudRow.of(HudText.get("overlay.card.row.sameSystem"),
                    HudText.plural("overlay.card.value.missionCount", (int) sameSystem)));
        }
    }

    /**
     * The "what am I actually doing" row, which differs per mission family.
     * Kept deliberately small: anything not covered falls through to no row
     * rather than inventing a label.
     */
    private void addTargetRow(List<HudRow> rows, MissionDto mission, List<MissionDto> missions) {
        if (mission.getKillCount() > 0) {
            String faction = mission.getMissionTargetFaction();
            rows.add(HudRow.of(HudText.get("overlay.card.row.killsRequired"),
                    String.valueOf(mission.getKillCount())));
            if (faction != null && !faction.isBlank()) {
                rows.add(HudRow.of(HudText.get("overlay.card.row.target"), faction.toUpperCase()));
            }
        } else if (mission.getCommodityName() != null && !mission.getCommodityName().isBlank()) {
            String qty = mission.getCount() > 0 ? " x" + mission.getCount() : "";
            rows.add(HudRow.of(HudText.get("overlay.card.row.cargo"),
                    mission.getCommodityName().toUpperCase() + qty));
            addCargoProgressRow(rows, mission, missions);
        } else if (mission.getPassengerCount() > 0) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.passengers"),
                    String.valueOf(mission.getPassengerCount())));
        }
    }

    /**
     * How much of the cargo is already aboard.
     * <p>
     * This is the one delivery family the journal reports no progress for at all:
     * a source-and-return mission is filled by buying on the open market, so
     * nothing ties the purchase to the mission and the card would otherwise show
     * the same "x45" from acceptance to hand-over. {@link MissionCargo} counts it
     * off the hold instead - across the whole stack, so two missions for the same
     * good do not both claim the same tonnes.
     * <p>
     * Absent rather than empty when the commodity has no symbol: a mission stored
     * before symbols were recorded cannot be measured, and a bar reading zero
     * would say the hold is empty rather than that we do not know.
     */
    private void addCargoProgressRow(List<HudRow> rows, MissionDto mission, List<MissionDto> missions) {
        MissionCargo.outstanding(missions, MissionCargo.heldBySymbol(shipCargo.get())).stream()
                .filter(item -> item.mission().getMissionId() == mission.getMissionId())
                .findFirst()
                .ifPresent(item -> rows.add(HudRow.progress(
                        HudText.get("overlay.card.row.inHold"), item.held(), item.required(),
                        item.isSatisfied() ? HudRow.State.GOOD : HudRow.State.NORMAL)));
    }

    private Optional<HudRow> expiryRow(MissionDto mission) {
        Optional<Instant> expiry = expiryOf(mission);
        if (expiry.isEmpty()) return Optional.empty();
        Duration left = Duration.between(Instant.now(), expiry.get());
        if (left.isNegative()) {
            return Optional.of(HudRow.of(HudText.get("overlay.card.row.expired"), "-", HudRow.State.CRITICAL));
        }
        // Under six hours is the point where it starts driving decisions.
        HudRow.State state = left.toHours() < 6 ? HudRow.State.WARN : HudRow.State.NORMAL;
        return Optional.of(HudRow.of(HudText.get("overlay.card.row.expires"), humanDuration(left), state));
    }

    // -- text ------------------------------------------------------------------

    private String title(MissionDto mission) {
        if (mission.getMissionDescription() != null && !mission.getMissionDescription().isBlank()) {
            return mission.getMissionDescription();
        }
        // The type's own constant name is an identifier, not a label, and is only ever seen when the
        // journal gave the mission no description of its own - spell it out rather than show it.
        return mission.getMissionType() == null
                ? HudText.get("overlay.card.title.mission")
                : mission.getMissionType().label();
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

    /**
     * How long is left, in the commander's units. Package-private so the format can be pinned without a
     * mission, a stack or a clock, as {@link #featured} already is.
     */
    static String humanDuration(Duration d) {
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        // WHY Locale.ROOT: the pad is structure, not a figure. Bare String.format follows the JVM
        // default locale, which is the operating system's and not the language chosen here, so on a
        // machine set to a non-Western-digit locale it would put Arabic-Indic digits in this row while
        // every other number on the card stayed Western.
        //
        // WHY the units are read inside the branches: each lookup is a bundle read behind a session
        // read, and only two of the three are ever rendered. Hoisting them makes the card pay for one
        // it never shows, on every poll.
        if (days > 0) {
            return days + HudText.get("overlay.card.duration.days") + " "
                    + String.format(Locale.ROOT, "%02d", hours) + HudText.get("overlay.card.duration.hours");
        }
        if (hours > 0) {
            return hours + HudText.get("overlay.card.duration.hours") + " "
                    + String.format(Locale.ROOT, "%02d", minutes) + HudText.get("overlay.card.duration.minutes");
        }
        return minutes + HudText.get("overlay.card.duration.minutes");
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
