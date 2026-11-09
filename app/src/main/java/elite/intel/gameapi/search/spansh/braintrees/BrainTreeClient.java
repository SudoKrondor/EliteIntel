package elite.intel.gameapi.search.spansh.braintrees;

import com.google.gson.JsonObject;
import elite.intel.gameapi.search.spansh.client.SpanshClient;
import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.util.json.GsonFactory;

import java.util.List;

/**
 * Asks Spansh for the brain tree sites nearest a point that yield one raw material.
 * <p>
 * Asked live on every request, with nothing kept: Spansh knows of more than ten thousand such bodies, and
 * which is nearest depends on where the commander is when they ask.
 */
public class BrainTreeClient extends SpanshClient {

    private static final String BASE_URL = "https://spansh.co.uk/api/bodies/search/save";
    private static final String RESULTS_URL = "https://spansh.co.uk/api/bodies/search/recall/";

    private static volatile BrainTreeClient instance;

    /**
     * Package-private so a test can build one after pointing {@code spansh.base.url} at a stub.
     */
    BrainTreeClient() {
        super(BASE_URL, RESULTS_URL);
    }

    public static BrainTreeClient getInstance() {
        if (instance == null) {
            synchronized (BrainTreeClient.class) {
                if (instance == null) {
                    instance = new BrainTreeClient();
                }
            }
        }
        return instance;
    }

    /**
     * The {@code limit} brain tree bodies nearest the given point that hold {@code englishMaterial}, nearest
     * first. Empty when Spansh has none or does not answer: the caller says nothing was found either way.
     */
    public List<StellarObjectSearchResultDto.Result> nearestWithMaterial(
            String englishMaterial, double x, double y, double z, int limit) {
        JsonObject response = performSearch(new BrainTreeCriteria(englishMaterial, x, y, z, limit));
        if (response == null) return List.of();
        StellarObjectSearchResultDto result = GsonFactory.getGson().fromJson(response, StellarObjectSearchResultDto.class);
        if (result == null || result.getResults() == null) return List.of();
        return result.getResults();
    }
}
