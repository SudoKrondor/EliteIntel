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
 * Under the shared bar the card names each mission PROVIDER and what it still
 * wants, because the total on its own does not answer the question a commander
 * asks at the board: whose missions am I already holding? A stack is only a stack
 * while every contract names the same target faction, and the way to grow one
 * without overshooting a self-imposed kill count is to take the next mission from
 * a provider NOT already in the pile - a second contract from one that is queues
 * behind its first and adds its full count, while one from a fresh provider runs
 * alongside and may cost nothing extra at all.
 * <p>
 * These are counts, not bars. The stack has one bar and it is the one that
 * matters; a column of near-identical bars underneath it would say the same thing
 * five times over and leave no room for the name, which is the whole point of the
 * rows.
 * <p>
 * It stands down for a plotted route that is not its own, though. A commander who
 * takes massacre work rarely takes only massacre work, and a route plotted to a
 * delivery three systems over is them stating what they are doing right now -
 * which the kill bar would otherwise sit on top of for the whole flight, because
 * this card applies whenever any pirate contract is open and never asked where
 * the ship was pointed.
 */
public class MassacreObjectiveSource implements HudObjectiveSource {

    /**
     * Rows the renderer keeps - {@code MAX_ROWS} in {@code hud.h}. Rows past the eighth are dropped
     * where the protocol is parsed, so the card has to do its own budgeting.
     */
    private static final int MAX_CARD_ROWS = 8;

    /**
     * Characters a provider's name gets. Nothing on a card wraps or clips (see {@link HudText}), so a
     * longer name would be drawn into its own count - and unlike the labels this app writes, a
     * faction name is the game's and cannot be translated shorter. A row carrying a short value has
     * room for 24 upper-case characters, which all but the longest faction names fit inside whole.
     */
    private static final int MAX_PROVIDER_LABEL = 24;

    /**
     * Shortest a shortened provider name may be cut back to at a word boundary. Below this the words
     * that survive carry too little to tell two providers apart, and a mid-word cut reads better.
     */
    private static final int MIN_PROVIDER_LABEL = 14;

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

        int stacked = missions.size();
        long reward = missions.values().stream()
                .filter(java.util.Objects::nonNull)
                .mapToLong(MissionDto::getReward)
                .sum();

        List<HudRow> rows = new ArrayList<>();
        rows.add(HudRow.progress(HudText.get(confirmed
                        ? "overlay.card.row.pirates"
                        : "overlay.card.row.piratesEstimated"),
                (int) progress.killsDone(), (int) progress.killsRequired(),
                confirmed ? HudRow.State.GOOD : HudRow.State.NORMAL));

        // The rows below the providers are counted out first: they are dropped where the protocol is
        // parsed, not here, so an over-long provider list would silently cost the reward line rather
        // than its own tail.
        int reserved = rows.size() + (stacked > 1 ? 1 : 0) + (reward > 0 ? 1 : 0);
        addProviderRows(rows, progress, MAX_CARD_ROWS - reserved);

        if (stacked > 1) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.missions"), String.valueOf(stacked)));
        }
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
     * One row per mission provider - its name, and the kills it still wants - in the order their
     * first contract was accepted.
     * <p>
     * The count is the provider's WHOLE queue, not just the mission it has running: a second contract
     * from a provider already in the stack waits for the first and is paid for in full, so a provider
     * reading 200 is 200 more kills whatever the stack total says. That is the number that stops a
     * 60-kill run turning into a 200-kill one.
     * <p>
     * Order is acceptance order rather than anything derived from the numbers, because the card is
     * re-derived on every poll and only written when it differs from the last one: an order that
     * moved as kills landed would rewrite the whole card each time a provider changed places.
     * <p>
     * A single provider gets no rows at all - its count would be a copy of the total above it, since
     * a lone provider's queue IS the stack.
     *
     * @param budget rows left for providers once the total, the count and the reward are paid for
     */
    private static void addProviderRows(List<HudRow> rows, MassacreProgress progress, int budget) {
        Map<String, Integer> remainingByProvider = progress.queueRemainingByFaction();
        List<String> providers = remainingByProvider.keySet().stream()
                .filter(faction -> faction != null && !faction.isBlank())
                .toList();
        if (providers.size() < 2 || budget <= 0) return;

        // One provider over budget still costs a row to say so, so the overflow row pays for itself
        // only when it hides more than the one it displaces.
        int shown = providers.size() <= budget ? providers.size() : budget - 1;

        for (String provider : providers.subList(0, shown)) {
            int remaining = remainingByProvider.getOrDefault(provider, 0);
            // Nothing left to kill for this provider, which only the game's own redirect can bring
            // about - so it is worth saying outright rather than as a green zero.
            rows.add(remaining == 0
                    ? HudRow.of(providerLabel(provider), HudText.get("overlay.card.value.complete"),
                    HudRow.State.GOOD)
                    : HudRow.of(providerLabel(provider), HudText.count(remaining)));
        }

        int hidden = providers.size() - shown;
        if (hidden > 0) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.moreProviders"), "+" + hidden));
        }
    }

    /**
     * A provider's name as it fits on its row: upper-cased like every other name on a card, and cut
     * at a word boundary where one is close enough to the limit, since ED faction names carry what
     * tells them apart at the front.
     */
    static String providerLabel(String faction) {
        String label = faction.trim().toUpperCase(Locale.ROOT);
        if (label.length() <= MAX_PROVIDER_LABEL) return label;

        String cut = label.substring(0, MAX_PROVIDER_LABEL - 1);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace >= MIN_PROVIDER_LABEL) cut = cut.substring(0, lastSpace);
        return cut.strip() + "…";
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
