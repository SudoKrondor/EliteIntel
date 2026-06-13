package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeQueryHandler extends BaseQueryAnalyzer implements QueryHandler {

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {

        if (SystemSession.getInstance().useLocalQueryLlm()) {
            ZonedDateTime localNow = ZonedDateTime.now(ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String formattedTime = localNow.format(formatter);
            return process(StringUtls.localizedLlm("query.time.local", formattedTime));
        } else {
            ////EventBusManager.publish(new AiVoxResponseEvent("Analyzing temporal data. Stand by."));

            Instant instant = Clock.systemUTC().instant();
            String utcTime = instant.toString();
            String instructions = """
                    Calculate current time at the location requested by user. The data provides current UTC time
                    """;
            return process(new AiDataStruct(instructions, new DataDto(utcTime)), originalUserInput);
        }
    }


    record DataDto(String utcTime) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
