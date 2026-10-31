package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.PowerConflictZoneManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.gameapi.search.PermitLockedSystems;
import elite.intel.gameapi.signals.PowerWarZone;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Plots a route to the best power conflict zone system the app knows of for the power the commander
 * is pledged to.
 * <p>
 * The powerplay twin of {@link FindConflictZoneCommand}. A power conflict zone is fought for merits by
 * pledged commanders, so only systems where the commander's own power is present are offered - a zone
 * between two other powers is no fight of theirs. Within the range the commander named, the zone
 * sighted most recently (by day) is offered first, then the system with the most zones, then the
 * nearest.
 * <p>
 * Offered only when the commander says "power" (see {@code ai_action_cues}), so a plain "find a
 * conflict zone" is never shadowed by this sibling.
 */
@RegisterCommand
public final class FindPowerConflictZoneCommand implements IntelCommand {

    public static final String ID = "find_powerplay_conflict_zone_for_merits";

    private static final int DEFAULT_RANGE_LY = 100;

    private final PowerConflictZoneManager powerConflictZones = PowerConflictZoneManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Plot a route to a star system with power conflict zones (powerplay), where the power the commander "
                + "is pledged to is fighting, to earn merits. Only when the commander says power or powerplay - a "
                + "plain conflict or war zone is a different command. Answered from zones sighted in the last week, "
                + "within the range in light years given by 'key'.";
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return SearchRange.PARAMETERS;
    }

    /**
     * Route plotting is available anywhere in the game, so the command is offered everywhere.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        RankAndProgressDto progress = playerSession.getRankAndProgressDto();
        if (!progress.isPledged()) {
            return StringUtls.localizedResponse(RankAndProgressDto.PLEDGE_UNKNOWN.equals(progress.getPledgedToPower())
                    ? "handler.powerWar.pledgeUnknown"
                    : "handler.powerWar.notPledged");
        }
        String power = progress.getPledgedToPower();
        int range = SearchRange.lightYears(params, DEFAULT_RANGE_LY);

        Coordinates here = locationManager.getGalacticCoordinates();
        if (here == null) return StringUtls.localizedResponse("handler.pirate.positionUnknown");

        List<PowerWarZone> zones = PermitLockedSystems.reachable(
                powerConflictZones.bestZonesFor(power, here, range), PowerWarZone::starSystem);
        if (zones.isEmpty()) return StringUtls.localizedResponse("handler.powerWar.noZonesKnown", power, range);

        PowerWarZone best = zones.getFirst();
        if (best.starSystem().equalsIgnoreCase(here.starSystem())) {
            return StringUtls.localizedResponse("handler.powerWar.alreadyInZone", spoken(best));
        }

        // Non-terminal announcement: route plotting below must still run, so the finding is voiced via
        // filler rather than returned.
        VegaRuntime.narrator().filler(spoken(best) + " " + StringUtls.localizedResponse(
                "handler.pirate.groundDistance", Math.round(best.distanceLy())), false);

        return new RoutePlotter().plotRoute(best.starSystem());
    }

    /**
     * "Hyades Sector CA-P b6-4 holds 3 power conflict zones. Contested by A. Lavigny-Duval, Aisling Duval."
     */
    private static String spoken(PowerWarZone zone) {
        String held = StringUtls.localizedResponse("handler.powerWar.zonesIn", zone.starSystem(), zone.zones());
        List<String> contenders = zone.contenders();
        String who;
        if (zone.controllingPower() == null || zone.controllingPower().isBlank()) {
            who = StringUtls.localizedResponse("handler.powerWar.contested", String.join(", ", contenders));
        } else if (contenders.isEmpty()) {
            who = StringUtls.localizedResponse("handler.powerWar.controlled", zone.controllingPower());
        } else {
            who = StringUtls.localizedResponse("handler.powerWar.controlledAndContested",
                    zone.controllingPower(), String.join(", ", contenders));
        }
        return held + " " + who;
    }
}
