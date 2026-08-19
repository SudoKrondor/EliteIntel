package elite.intel.ui.overlay;

import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MassacreProgress;
import elite.intel.session.PlayerSession;

import java.util.*;
import java.util.function.Supplier;

/**
 * Projects the pirate-massacre mission stack into a HUD objective with a real
 * progress bar.
 * <p>
 * Shares {@link MassacreProgress} with the voice query, so the bar and the
 * spoken answer can never disagree. Progress moves on its own as bounties
 * arrive: the source recomputes on each poll, so a kill advances the bar without
 * any event plumbing of its own.
 * <p>
 * Outranks the generic mission card - a stack of massacre missions is better
 * described by one shared kill count than by whichever single mission happens to
 * expire soonest.
 * <p>
 * It stands down for a plotted route that is not its own, though. A commander who
 * takes massacre work rarely takes only massacre work, and a route plotted to a
 * delivery three systems over is them stating what they are doing right now -
 * which the kill bar would otherwise sit on top of for the whole flight, because
 * this card applies whenever any pirate contract is open and never asked where
 * the ship was pointed.
 */
public class MassacreObjectiveSource implements HudObjectiveSource {

    private final MissionManager missionManager;
    private final PlayerSession playerSession;
    private final Supplier<String> routeDestination;

    public MassacreObjectiveSource() {
        this(MissionManager.getInstance(), PlayerSession.getInstance(),
                () -> ShipRouteManager.getInstance().getDestination());
    }

    /**
     * Seam for tests.
     */
    MassacreObjectiveSource(MissionManager missionManager, PlayerSession playerSession,
                            Supplier<String> routeDestination) {
        this.missionManager = missionManager;
        this.playerSession = playerSession;
        this.routeDestination = routeDestination;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        Map<Long, MissionDto> missions = missionManager.getMissions(missionManager.getPirateMissionTypes());
        if (missions == null || missions.isEmpty()) return Optional.empty();
        if (standsDownFor(routeDestination.get(), missions.values())) return Optional.empty();

        Set<BountyDto> bounties = playerSession.getBounties();
        MassacreProgress progress = MassacreProgress.compute(
                missions.values(), bounties == null ? Set.of() : bounties);
        if (!progress.hasMissions() || progress.killsRequired() <= 0) return Optional.empty();

        // Kills are inferred from bounty vouchers, which over-count: a voucher is paid for a kill
        // someone else finished off, and the journal never says who landed the final blow. So the
        // bar is an estimate that can only run high, and it says so - until MissionRedirected
        // confirms the contract, which is the one count the game gives us and is exact.
        boolean confirmed = progress.killsRemaining() == 0;

        List<HudRow> rows = new ArrayList<>();
        rows.add(HudRow.progress(HudText.get(confirmed
                        ? "overlay.card.row.pirates"
                        : "overlay.card.row.piratesEstimated"),
                (int) progress.killsDone(), (int) progress.killsRequired(),
                confirmed ? HudRow.State.GOOD : HudRow.State.NORMAL));

        int stacked = missions.size();
        if (stacked > 1) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.missions"), String.valueOf(stacked)));
        }

        long reward = missions.values().stream()
                .filter(java.util.Objects::nonNull)
                .mapToLong(MissionDto::getReward)
                .sum();
        if (reward > 0) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.reward"), HudText.credits(reward)));
        }

        return Optional.of(new HudObjective(
                "massacre-stack",
                HudText.get("overlay.card.title.massacre"),
                progress.targetFaction() == null ? null : progress.targetFaction().toUpperCase(),
                rows,
                HudObjective.PRIORITY_SPECIALISED));
    }

    /**
     * Whether the plotted route belongs to some other mission, in which case this card gets out of the
     * way and lets {@link MissionObjectiveSource} name the one being flown to.
     * <p>
     * Only a route that names a mission counts. A route to a material trader, an engineer or nowhere in
     * particular says nothing about the mission board, and standing down for it would cost the kill bar
     * for no gain - so the stand-down needs another mission actually ending there.
     *
     * @param destination where the plotted route ends, or null/blank when nothing is plotted
     * @param massacres   the open pirate contracts
     */
    private boolean standsDownFor(String destination, Collection<MissionDto> massacres) {
        if (destination == null || destination.isBlank()) return false;
        if (massacres.stream().anyMatch(mission -> endsAt(mission, destination))) return false;

        // Only now is the full board worth a read: the common case is a route to the hunting ground,
        // which the check above already settled.
        return missionManager.getMissions().values().stream().anyMatch(mission -> endsAt(mission, destination));
    }

    private static boolean endsAt(MissionDto mission, String system) {
        return mission != null && system.equalsIgnoreCase(mission.getDestinationSystem());
    }
}
