package elite.intel.ui.overlay;

import elite.intel.db.dao.CombatBondDao;
import elite.intel.db.managers.CombatBondManager;
import elite.intel.gameapi.signals.ConflictZoneIntensity;
import elite.intel.session.ConflictZone;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Projects a conflict-zone fight into a HUD objective: which zone the ship dropped into, which side
 * it is fighting for, and what the combat bonds in hand are worth so far.
 * <p>
 * The twin of {@link BountyHuntObjectiveSource} for the other kind of fighting, ranked the same
 * ({@link HudObjective#PRIORITY_AMBIENT}) for the same reason: the commander never accepted this
 * the way they accept a mission, so a contract, a trade route or a construction haul all take the
 * card back on their own. Registered right after the bounty card, and the two never compete - a
 * ship is in a resource site or in a war zone, not both.
 * <p>
 * The tally is the bonds not yet cashed in, which is why redeeming them empties the card rather
 * than the app having to track a session of its own.
 */
public class ConflictZoneObjectiveSource implements HudObjectiveSource {

    private final ConflictZone conflictZone;
    private final CombatBondManager combatBonds;
    private final LongSupplier currentSystemAddress;
    private final Supplier<String> currentSystemName;
    private final BooleanSupplier inMainShip;

    public ConflictZoneObjectiveSource() {
        this(ConflictZone.getInstance(), CombatBondManager.getInstance(),
                () -> PlayerSession.getInstance().getLocationData().getSystemAddress(),
                () -> PlayerSession.getInstance().getPrimaryStarName(),
                () -> Status.getInstance().isInMainShip());
    }

    /**
     * Seam for tests.
     */
    ConflictZoneObjectiveSource(ConflictZone conflictZone, CombatBondManager combatBonds,
                                LongSupplier currentSystemAddress, Supplier<String> currentSystemName,
                                BooleanSupplier inMainShip) {
        this.conflictZone = conflictZone;
        this.combatBonds = combatBonds;
        this.currentSystemAddress = currentSystemAddress;
        this.currentSystemName = currentSystemName;
        this.inMainShip = inMainShip;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        if (!inMainShip.getAsBoolean()) return Optional.empty();

        ConflictZone.Engagement engagement = conflictZone.in(currentSystemAddress.getAsLong());
        if (engagement == null) return Optional.empty();

        CombatBondDao.Pending pending = combatBonds.pending();

        List<HudRow> rows = new ArrayList<>();
        rows.add(HudRow.of(HudText.get("overlay.card.row.bonds"), HudText.credits(pending.credits()),
                pending.credits() > 0 ? HudRow.State.GOOD : HudRow.State.NORMAL));
        rows.add(HudRow.of(HudText.get("overlay.card.row.kills"), HudText.count(pending.kills())));
        if (pending.side() != null && !pending.side().isBlank()) {
            // A faction name is the game's own and passes through untouched.
            rows.add(HudRow.of(HudText.get("overlay.card.row.side"), pending.side()));
        }

        return Optional.of(new HudObjective(
                "conflict-zone",
                HudText.get("overlay.card.title.conflictZone"),
                subtitle(engagement.intensity()),
                rows,
                HudObjective.PRIORITY_AMBIENT));
    }

    /**
     * The system and the intensity of zone being fought in. The system name is the game's own and
     * passes through untouched; the intensity is a word this app writes, so it is translated.
     */
    private String subtitle(ConflictZoneIntensity intensity) {
        String starSystem = currentSystemName.get();
        String zone = HudText.get(intensityKey(intensity)).toUpperCase(Locale.ROOT);
        return starSystem == null || starSystem.isBlank()
                ? zone
                : starSystem.toUpperCase(Locale.ROOT) + " - " + zone;
    }

    private static String intensityKey(ConflictZoneIntensity intensity) {
        return switch (intensity) {
            case LOW -> "overlay.card.value.czLow";
            case MEDIUM -> "overlay.card.value.czMedium";
            case HIGH -> "overlay.card.value.czHigh";
            case POWERPLAY -> "overlay.card.value.czPowerplay";
        };
    }
}
