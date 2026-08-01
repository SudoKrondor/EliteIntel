package elite.intel.ui.overlay;

import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MassacreProgress;
import elite.intel.session.PlayerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
 */
public class MassacreObjectiveSource implements HudObjectiveSource {

    private final MissionManager missionManager;
    private final PlayerSession playerSession;

    public MassacreObjectiveSource() {
        this(MissionManager.getInstance(), PlayerSession.getInstance());
    }

    /**
     * Seam for tests.
     */
    MassacreObjectiveSource(MissionManager missionManager, PlayerSession playerSession) {
        this.missionManager = missionManager;
        this.playerSession = playerSession;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        Map<Long, MissionDto> missions = missionManager.getMissions(missionManager.getPirateMissionTypes());
        if (missions == null || missions.isEmpty()) return Optional.empty();

        Set<BountyDto> bounties = playerSession.getBounties();
        MassacreProgress progress = MassacreProgress.compute(
                missions.values(), bounties == null ? Set.of() : bounties);
        if (!progress.hasMissions() || progress.killsRequired() <= 0) return Optional.empty();

        List<HudRow> rows = new ArrayList<>();
        rows.add(HudRow.progress("PIRATES",
                (int) progress.killsDone(), (int) progress.killsRequired(),
                progress.killsRemaining() == 0 ? HudRow.State.GOOD : HudRow.State.NORMAL));

        int stacked = missions.size();
        if (stacked > 1) {
            rows.add(HudRow.of("MISSIONS", String.valueOf(stacked)));
        }

        long reward = missions.values().stream()
                .filter(java.util.Objects::nonNull)
                .mapToLong(MissionDto::getReward)
                .sum();
        if (reward > 0) {
            rows.add(HudRow.of("REWARD", String.format("%,d cr", reward)));
        }

        return Optional.of(new HudObjective(
                "massacre-stack",
                "MASSACRE CONTRACT",
                progress.targetFaction() == null ? null : progress.targetFaction().toUpperCase(),
                rows,
                HudObjective.PRIORITY_SPECIALISED));
    }
}
