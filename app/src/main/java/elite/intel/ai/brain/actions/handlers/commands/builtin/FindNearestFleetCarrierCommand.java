package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.search.spansh.findcarrier.CarrierAccess;
import elite.intel.gameapi.search.spansh.findcarrier.FleetCarrierSearch;
import elite.intel.gameapi.search.spansh.findcarrier.FleetCarrierSearchResultsDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.TimeUtils;
import elite.intel.util.json.GetNumberFromParam;

/**
 * Self-describing "find nearest fleet carrier" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindNearestFleetCarrierHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand(before = { NavigateToFleetCarrierCommand.ID })
public final class FindNearestFleetCarrierCommand implements IntelCommand {
    public static final String ID = "find_nearest_fleet_carrier";

    @Override
    public String llmDescription() {
        return "Find and report the nearest fleet carrier other than the commander's own, within range.";
    }


    @Override
    public String id() {
        return ID;
    }

    /// route plotting is accessible anywhere in the game
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {


        Number range = GetNumberFromParam.extractRangeParameter(params, 500);
        CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.fleetCarrier.searching", range.intValue()), false);

        PlayerSession playerSession = PlayerSession.getInstance();
        FleetCarrierSearchResultsDto fleetCarriers = FleetCarrierSearch.getInstance()
                .findFleetCarrier(
                        range.intValue(),
                        CarrierAccess.ALL,
                        LocationManager.getInstance().getGalacticCoordinates()
                );

        String playerCarrierCallSign = null;
        CarrierDataDto carrierData = playerSession.getFleetCarrierData();
        if (carrierData != null) {
            playerCarrierCallSign = carrierData.getCallSign();
        }

        if (fleetCarriers == null) {
            return StringUtls.localizedResponse("handler.fleetCarrier.spanshUnavailable");
        }

        final String finalPlayerCarrierCallSign = playerCarrierCallSign;
        fleetCarriers.getResults().stream()
                .filter(carrier -> finalPlayerCarrierCallSign == null || !finalPlayerCarrierCallSign.equals(carrier.getCallSign()))
                .findFirst()
                .ifPresentOrElse(
                        result -> {
                            RoutePlotter routePlotter = new RoutePlotter();
                            String dateAsString = result.getUpdatedAt();
                            String timeAgo = TimeUtils.transformToYMDHtimeAgo(dateAsString, TimeUtils.LOCAL_DATE_TIME);
                            CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.fleetCarrier.found", result.getCallSign(), result.getSystemName(), timeAgo), false);
                            routePlotter.plotRoute(result.getSystemName());
                        },
                        () -> CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.fleetCarrier.notFound"), false)
                );
        return null;
    }
}
