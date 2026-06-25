package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.search.spansh.findcarrier.CarrierAccess;
import elite.intel.search.spansh.findcarrier.FleetCarrierSearch;
import elite.intel.search.spansh.findcarrier.FleetCarrierSearchResultsDto;
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

    @Override public String llmDescription() { return "Find and report the nearest fleet carrier."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {


        Status status = Status.getInstance();
        if (!status.isInSrv() && !status.isInMainShip()) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv"));
        }

        Number range = GetNumberFromParam.extractRangeParameter(params, 500);
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
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.fleetCarrier.spanshUnavailable"));
        }

        final String finalPlayerCarrierCallSign = playerCarrierCallSign;
        var match = fleetCarriers.getResults().stream()
                .filter(carrier -> finalPlayerCarrierCallSign == null || !finalPlayerCarrierCallSign.equals(carrier.getCallSign()))
                .findFirst();
        if (match.isEmpty()) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.fleetCarrier.notFound"));
        }
        var result = match.get();
        String timeAgo = TimeUtils.transformToYMDHtimeAgo(result.getUpdatedAt(), TimeUtils.LOCAL_DATE_TIME);
        new RoutePlotter().plotRoute(result.getSystemName());
        return CommandOutcome.critical(StringUtls.localizedLlm("handler.fleetCarrier.found", result.getCallSign(), result.getSystemName(), timeAgo));
    }
}
