package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.builtin.CalculateFleetCarrierRouteCommand;
import elite.intel.ai.brain.actions.handlers.commands.builtin.EnterFleetCarrierDestinationCommand;
import elite.intel.ai.brain.actions.handlers.commands.builtin.SetCarrierFuelReserveCommand;
import elite.intel.ai.brain.actions.handlers.queries.carrier.CarrierOwnership;
import elite.intel.ai.brain.actions.handlers.queries.carrier.CarrierView;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.ai.brain.vega.SpokenAmounts;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

/**
 * A carrier's standing state: what it holds and what it can afford. Serves the commander's own fleet carrier and
 * their squadron's alike - {@link CarrierOwnership} reads which one from the utterance.
 */
@RegisterQuery(before = {
        AnalyzeCarrierVoyageQuery.ID,
        AnalyzeCarrierDepartureEtaQuery.ID,
        AnalyzeDistanceFromFleetCarrierQuery.ID,
        CalculateFleetCarrierRouteCommand.ID,
        EnterFleetCarrierDestinationCommand.ID,
        SetCarrierFuelReserveCommand.ID
})
public class AnalyzeCarrierStatusQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_carrier_status";

    @Override
    public String llmDescription() {
        return "Report a carrier's standing state: tritium fuel supply and reserve, jump range, bank balance and "
                + "finances. Covers the commander's own fleet carrier by default, and the squadron carrier when "
                + "the commander says \"squadron\".";
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        CarrierView carrier = CarrierView.forUtterance(originalUserInput);
        if (!carrier.hasData()) {
            return process(noDataMessage(carrier.ownership()));
        }
        CarrierDataDto stats = carrier.data();
        String instructions = """
                Answer the commander's question about carrier status.
                
                Data fields:
                - carrier: which carrier this is. Name it in the answer so the commander knows which one you mean.
                - reserveBalance: credits reserved for weekly operations
                - totalBalance: total credits in the carrier bank (includes reserveBalance)
                - marketBalance: credits available for market purchases. A negative value is money already
                  committed to pending purchases, not debt.
                - fuelSupply: current tritium in the supply depot, in tons
                - fuelSupplyMeasured: true when the game itself last reported fuelSupply, false when it is
                  our own running total since that reading and so an estimate
                - fuelSupplyReserve: tritium held in reserve, in tons
                - totalFuelAvailable: fuelSupply and fuelSupplyReserve combined
                - maxRangeOnCurrentSupply: range in light years on fuelSupply alone
                - maxRangeUsingReserve: range in light years drawing on the reserve as well
                - fundedOperation: weeks of operation funded at the current balance
                """ + SpokenAmounts.RULE + """
                
                Rules:
                - Answer only the specific field the commander asks about.
                - If no specific field is mentioned, give a summary.
                - Do not invent or assume values not in the data.
                - If a value is zero or missing, state that clearly.
                - When reporting fundedOperation, always mention the calculation is approximate, based on
                  31 million credits per week.
                - When fuelSupplyMeasured is false, say the tritium figure is approximate. It is a running
                  total since the last reading, so quoting it flatly would claim knowledge we do not have.
                """;
        return process(
                new AiDataStruct(
                        instructions,
                        new DataDto(
                                carrier.ownership().label(),
                                stats.getReserveBalance(),
                                stats.getTotalBalance(),
                                stats.getMarketBalance(),
                                stats.getFuelLevel(),
                                stats.isFuelLevelMeasured(),
                                stats.getFuelReserve(),
                                stats.getFuelLevel() + stats.getFuelReserve(),
                                stats.getRangeExcludingReserve(),
                                stats.getRange(),
                                stats.getFundedOperation()
                        )
                ),
                originalUserInput
        );
    }

    private static String noDataMessage(CarrierOwnership ownership) {
        return ownership == CarrierOwnership.SQUADRON
                ? StringUtls.localizedResponse("query.squadronCarrier.noData")
                : StringUtls.localizedResponse("query.carrier.noDataOpenPanel");
    }

    record DataDto(
            String carrier,
            long reserveBalance,
            long totalBalance,
            long marketBalance,
            int fuelSupply,
            boolean fuelSupplyMeasured,
            int fuelSupplyReserve,
            int totalFuelAvailable,
            int maxRangeOnCurrentSupply,
            int maxRangeUsingReserve,
            int fundedOperation
    ) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            // Spoken siblings for the bank balances, appended the same way as the finance announcements
            // so amounts are spoken one way everywhere. See SpokenAmounts.RULE.
            return YamlFactory.toYaml(this)
                    + SpokenAmounts.yamlLine("reserveBalance", reserveBalance)
                    + SpokenAmounts.yamlLine("totalBalance", totalBalance)
                    + SpokenAmounts.yamlLine("marketBalance", marketBalance);
        }
    }
}