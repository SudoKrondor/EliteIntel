package elite.intel.gameapi.search.spansh.neutronroute;

import com.google.gson.JsonObject;
import elite.intel.gameapi.search.spansh.client.SpanshClient;
import elite.intel.gameapi.search.spansh.client.StringQuery;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NeutronStarRouteClient extends SpanshClient {

    private static final Logger log = LogManager.getLogger(NeutronStarRouteClient.class);
    private static final String BASE_URL = "https://spansh.co.uk/api/route";
    private static final String RESULTS_URL = "https://spansh.co.uk/api/results/";

    /**
     * How Spansh encodes its supercharge checkbox. It is a tick box on the form and a number in the
     * request, with exactly two values it ever sends - 6 for ticked, 4 for unticked. The numbers are not
     * a multiplier we get to choose; anything else is not a state the planner offers, so the two live
     * here as the encoding of a yes and a no rather than as a caller's parameter.
     */
    private static final int SUPERCHARGE_ON = 6;
    private static final int SUPERCHARGE_OFF = 4;

    public NeutronStarRouteClient() {
        super(BASE_URL, RESULTS_URL);
    }

    public NeutronStarRoute calculateRoute(NeutronStarRouteCalculatorCriteria criteria) {
        String rangeStr = criteria.range() > 0 ? String.valueOf(criteria.range()) : "";
        String query = "efficiency=" + criteria.efficiency()
                + "&range=" + rangeStr
                + "&from=" + URLEncoder.encode(criteria.from(), StandardCharsets.UTF_8)
                + "&to=" + URLEncoder.encode(criteria.to(), StandardCharsets.UTF_8)
                + "&supercharge_multiplier=" + (criteria.supercharge() ? SUPERCHARGE_ON : SUPERCHARGE_OFF);

        log.info("Requesting neutron star route from {} to {}", criteria.from(), criteria.to());
        JsonObject result = performSearch(new Request(query));
        if (result == null) {
            log.warn("No result returned for neutron route {} -> {}", criteria.from(), criteria.to());
            return null;
        }
        return GsonFactory.getGson().fromJson(result, NeutronStarRoute.class);
    }

    record Request(String query) implements StringQuery {
        @Override
        public String getQuery() {
            return query;
        }
    }
}
