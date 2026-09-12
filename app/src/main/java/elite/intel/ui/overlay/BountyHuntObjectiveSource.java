package elite.intel.ui.overlay;

import elite.intel.db.dao.BountyDao;
import elite.intel.db.managers.BountyManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.signals.ResourceSiteGrade;
import elite.intel.session.PlayerSession;
import elite.intel.session.ResourceSite;
import elite.intel.session.Status;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Projects a bounty hunt into a HUD objective: which resource site the ship dropped into, and what
 * the vouchers in the hold are worth so far.
 * <p>
 * The other half of {@link MassacreObjectiveSource}. That card owns the screen when the commander is
 * working a contract stack; this one is for diving a resource site for the bounties alone, which is
 * the same flying with none of the paperwork and had nothing on the overlay at all.
 * <p>
 * Ranked {@link HudObjective#PRIORITY_AMBIENT}: the commander never accepted this the way they accept
 * a mission, so a contract, a trade route or a construction haul all take the card back on their own.
 * It is registered ahead of the plotted-route card, though, because a route to the station to cash in
 * is part of the hunt rather than a reason to stop showing it.
 * <p>
 * <b>The mining card is not a rival, despite ranking the same.</b> A resource extraction site holds
 * nothing to mine - the name is a misnomer, and the minerals are in hotspots somewhere else entirely -
 * and a mining build carries no weapons worth the name. The two cards describe activities that use
 * different ships in different places, so the tie between them is theoretical.
 * <p>
 * The tally is the vouchers not yet cashed in, which is why redeeming them empties the card rather
 * than the app having to track a session of its own.
 */
public class BountyHuntObjectiveSource implements HudObjectiveSource {

    private final ResourceSite resourceSite;
    private final BountyManager bountyManager;
    private final MissionManager missionManager;
    private final LongSupplier currentSystemAddress;
    private final Supplier<String> currentSystemName;
    private final BooleanSupplier inMainShip;

    public BountyHuntObjectiveSource() {
        this(ResourceSite.getInstance(), BountyManager.getInstance(), MissionManager.getInstance(),
                () -> PlayerSession.getInstance().getLocationData().getSystemAddress(),
                () -> PlayerSession.getInstance().getPrimaryStarName(),
                () -> Status.getInstance().isInMainShip());
    }

    /**
     * Seam for tests.
     */
    BountyHuntObjectiveSource(ResourceSite resourceSite, BountyManager bountyManager,
                              MissionManager missionManager, LongSupplier currentSystemAddress,
                              Supplier<String> currentSystemName, BooleanSupplier inMainShip) {
        this.resourceSite = resourceSite;
        this.bountyManager = bountyManager;
        this.missionManager = missionManager;
        this.currentSystemAddress = currentSystemAddress;
        this.currentSystemName = currentSystemName;
        this.inMainShip = inMainShip;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        if (!inMainShip.getAsBoolean()) return Optional.empty();

        ResourceSite.Hunt hunt = resourceSite.in(currentSystemAddress.getAsLong());
        if (hunt == null) return Optional.empty();
        if (holdsMassacreContracts()) return Optional.empty();

        BountyDao.Pending pending = bountyManager.pending();

        List<HudRow> rows = new ArrayList<>();
        rows.add(HudRow.of(HudText.get("overlay.card.row.bounties"), HudText.credits(pending.credits()),
                pending.credits() > 0 ? HudRow.State.GOOD : HudRow.State.NORMAL));
        rows.add(HudRow.of(HudText.get("overlay.card.row.kills"), HudText.count(pending.kills())));
        if (hunt.threat() > 0) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.threat"), HudText.count(hunt.threat())));
        }

        return Optional.of(new HudObjective(
                "bounty-hunt",
                HudText.get("overlay.card.title.bountyHunting"),
                subtitle(hunt.grade()),
                rows,
                HudObjective.PRIORITY_AMBIENT));
    }

    /**
     * WHY this is asked rather than left to the priority ladder: a massacre stack outranks this card
     * anyway, but it stands itself down while a route is plotted somewhere else. Without this the
     * bounty card would slide into the gap and tell a commander mid-contract that they were hunting
     * for pocket money.
     */
    private boolean holdsMassacreContracts() {
        Map<Long, ?> missions = missionManager.getMissions(missionManager.getPirateMissionTypes());
        return missions != null && !missions.isEmpty();
    }

    /**
     * The system and the grade of site being fought in. The system name is the game's own and passes
     * through untouched; the grade is a word this app writes, so it is translated.
     */
    private String subtitle(ResourceSiteGrade grade) {
        String starSystem = currentSystemName.get();
        String site = HudText.get(gradeKey(grade)).toUpperCase(Locale.ROOT);
        return starSystem == null || starSystem.isBlank()
                ? site
                : starSystem.toUpperCase(Locale.ROOT) + " - " + site;
    }

    private static String gradeKey(ResourceSiteGrade grade) {
        return switch (grade) {
            case LOW -> "overlay.card.value.resLow";
            case STANDARD -> "overlay.card.value.resStandard";
            case HIGH -> "overlay.card.value.resHigh";
            case HAZARDOUS -> "overlay.card.value.resHazardous";
        };
    }
}
