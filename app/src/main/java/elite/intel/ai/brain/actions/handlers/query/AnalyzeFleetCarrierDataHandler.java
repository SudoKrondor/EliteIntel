package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class AnalyzeFleetCarrierDataHandler extends BaseQueryAnalyzer implements QueryHandler {


    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing fleet carrier data. Stand by."));
        PlayerSession playerSession = PlayerSession.getInstance();
        CarrierDataDto stats = playerSession.getFleetCarrierData();

        if (stats.getTotalBalance() == 0 && stats.getFuelLevel() == 0) {
            return process(StringUtls.localizedLlm("query.carrier.noDataOpenPanel"));
        } else {
            String instructions = """
                    Answer the user's question about fleet carrier status.
                    
                    Data fields:
                    - reserveBalance: credits reserved for weekly operations
                    - totalBalance: total credits in carrier bank (includes reserveBalance)
                    - marketBalance: credits available for market purchases. Negative balance is the money reserved for purchases not dept
                    - fuelSupply: current tritium fuel in supply depot
                    - fuelSupplyReserve: tritium held in reserve
                    - totalFuelAvailable: fuelSupply and fuelSupplyReserve combined
                    - maxRange: maximum jump range in light years
                    - fundedOperation: weeks of operation funded at current balance
                    
                    Rules:
                    - Answer only the specific field the user asks about.
                    - if specific field is not mentioned provide summary.
                    - Do not invent or assume values not in the data.
                    - If a value is zero or missing, state that clearly.
                    - If reporting fundedOperation always mention that calculation is approximate based on 31 million credits per week.
                    """;
            return process(
                    new AiDataStruct(
                            instructions,
                            new DataDto(
                                    stats.getReserveBalance(),
                                    stats.getTotalBalance(),
                                    stats.getMarketBalance(),
                                    stats.getFuelLevel(),
                                    stats.getFuelReserve(),
                                    (stats.getFuelLevel() + stats.getFuelReserve()),
                                    stats.getRange(),
                                    stats.getFundedOperation()
                            )

                    ),
                    originalUserInput
            );
        }
    }

    record DataDto(
            long reserveBalance,
            long totalBalance,
            long marketBalance,
            int fuelSupply,
            int fuelSupplyReserve,
            int totalFuelAvailable,
            int maxRange,
            int fundedOperation
    ) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
