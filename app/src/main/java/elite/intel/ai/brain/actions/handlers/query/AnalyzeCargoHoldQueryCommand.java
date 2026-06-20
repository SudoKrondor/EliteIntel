package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzeCargoHoldQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_cargo_hold_contents";


    @Override public String id() { return ID; }


    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing cargo data. Stand by."));
        PlayerSession playerSession = PlayerSession.getInstance();

        String instructions = """
                Answer the user's question about cargo.
                
                Data fields:
                - cargoCapacity: maximum cargo space in tons
                - cargo: items currently in the cargo hold (1 unit = 1 ton)
                
                Rules:
                - If asked about cargo contents: list items from cargo. If empty, say cargo hold is empty.
                - List items in cargo hold and number of units (tonnes).
                - If asked about cargo capacity: state the cargoCapacity value in tons.
                - No follow-up questions.
                """;
        if (playerSession.getShipLoadout() != null) {
            return process(new AiDataStruct(
                            instructions,
                            new DataDto(playerSession.getShipLoadout().getCargoCapacity(), playerSession.getShipCargo())),
                    originalUserInput
            );
        } else {
            return process(StringUtls.localizedLlm("query.cargo.noLoadout"));
        }
    }

    record DataDto(int cargoCapacity, ToYamlConvertable cargo) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
