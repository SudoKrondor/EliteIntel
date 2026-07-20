package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * The countdown to a carrier's next scheduled jump, as set by the last {@code CarrierJumpRequest}.
 * <p>
 * Deliberately NOT carrier-scoped, unlike {@link AnalyzeCarrierStatusQuery} and {@link AnalyzeCarrierVoyageQuery}.
 * The session stores exactly one departure time, written by whichever carrier last scheduled a jump: the journal's
 * {@code CarrierJumpRequest} names its carrier, but the matching {@code CarrierJump} that clears the countdown does
 * not, so a per-carrier clock could be set but never reliably cleared. Reporting one shared countdown is what the
 * data actually supports; the previous pair of fleet/squadron ETA tools both read this same field and so only
 * differed in the claim they made about it.
 */
@RegisterQuery
public class AnalyzeCarrierDepartureEtaQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_carrier_departure_eta";

    private static final Logger log = LogManager.getLogger(AnalyzeCarrierDepartureEtaQuery.class);

    @Override
    public String llmDescription() {
        return "Report the time remaining until the carrier's next scheduled jump departs. Use for questions "
                + "about when the carrier leaves or arrives at its next system.";
    }

    @Override public String id() { return ID; }

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        String departureTime = PlayerSession.getInstance().getCarrierDepartureTime();
        if (departureTime == null || departureTime.isBlank()) {
            return process(StringUtls.localizedLlm("query.carrier.noDepartureTime"));
        }

        long minutesUntilArrival;
        try {
            ZonedDateTime arrival = ZonedDateTime.parse(departureTime, DateTimeFormatter.ISO_DATE_TIME);
            minutesUntilArrival = ChronoUnit.MINUTES.between(ZonedDateTime.now(), arrival);
        } catch (DateTimeParseException e) {
            // The journal writes ISO-8601, so an unparseable value means the stored session state is corrupt,
            // not that the carrier has no jump scheduled. Say we cannot tell, but leave a trail.
            log.warn("Unparseable carrier departure time in session: '{}'", departureTime, e);
            return process(StringUtls.localizedLlm("query.carrier.noEta"));
        }

        String instructions = """
                Report the carrier's estimated time of arrival.
                
                Data fields:
                - minutesUntilArrival: pre-computed minutes until the carrier arrives. Negative means it has
                  already arrived.
                
                State the arrival time in minutes. If negative, say the carrier has already arrived.
                """;
        return process(new AiDataStruct(instructions, new DataDto(minutesUntilArrival)), originalUserInput);
    }

    record DataDto(long minutesUntilArrival) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}