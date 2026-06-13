package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class AnalyzeSquadronCarrierETAHandler extends BaseQueryAnalyzer implements QueryHandler {

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        PlayerSession playerSession = PlayerSession.getInstance();
        String carrierDepartureTime = playerSession.getCarrierDepartureTime();
        if (carrierDepartureTime == null) {
            return process(StringUtls.localizedLlm("query.squadronCarrier.noDepartureTime"));
        }

        long minutesUntilArrival;
        try {
            ZonedDateTime arrival = ZonedDateTime.parse(carrierDepartureTime, DateTimeFormatter.ISO_DATE_TIME);
            minutesUntilArrival = ChronoUnit.MINUTES.between(ZonedDateTime.now(), arrival);
        } catch (Exception e) {
            return process(StringUtls.localizedLlm("query.squadronCarrier.noEta"));
        }

        String instructions = """
                Report the squadron carrier estimated time of arrival.
                
                Data fields:
                - minutesUntilArrival: pre-computed minutes until the carrier arrives (negative means already arrived)
                
                State the arrival time in minutes. If negative, say the carrier has already arrived.
                """;

        return process(new AiDataStruct(instructions, new DataDto(minutesUntilArrival)), originalUserInput);
    }

    record DataDto(long minutesUntilArrival) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
