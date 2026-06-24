package elite.intel.search.spansh.neutronroute;

import com.google.gson.JsonObject;
import elite.intel.search.spansh.client.SpanshClient;
import elite.intel.search.spansh.client.StringQuery;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NeutronStarRouteClient extends SpanshClient {

    private static final Logger log = LogManager.getLogger(NeutronStarRouteClient.class);
    private static final String BASE_URL = "https://spansh.co.uk/api/route";
    private static final String RESULTS_URL = "https://spansh.co.uk/api/results/";

    public NeutronStarRouteClient() {
        super(BASE_URL, RESULTS_URL);
    }

    public NeutronStarRoute calculateRoute(NeutronStarRouteCalculatorCriteria criteria) {
        String rangeStr = criteria.range() > 0 ? String.valueOf(criteria.range()) : "";
        String query = "efficiency=" + criteria.efficiency()
                + "&range=" + rangeStr
                + "&from=" + URLEncoder.encode(criteria.from(), StandardCharsets.UTF_8)
                + "&to=" + URLEncoder.encode(criteria.to(), StandardCharsets.UTF_8)
                + "&supercharge_multiplier=" + criteria.superchargeMultiplier();

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
