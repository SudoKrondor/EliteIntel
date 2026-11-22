package elite.intel.gameapi.search.spansh.rawmaterials;

import com.google.gson.JsonObject;
import elite.intel.gameapi.search.spansh.client.SpanshClient;
import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import elite.intel.util.json.GsonFactory;

import java.util.List;

/**
 * Asks Spansh for the bodies nearest a point that hold one raw material: brain tree sites, or landable
 * bodies to collect it from the surface.
 * <p>
 * Asked live on every request, with nothing kept: Spansh knows of more than ten thousand brain tree bodies,
 * and which is nearest depends on where the commander is when they ask.
 */
public class MaterialBodyClient extends SpanshClient {

    private static final String BASE_URL = "https://spansh.co.uk/api/bodies/search/save";
    private static final String RESULTS_URL = "https://spansh.co.uk/api/bodies/search/recall/";

    private static volatile MaterialBodyClient instance;

    /**
     * Package-private so a test can build one after pointing {@code spansh.base.url} at a stub.
     */
    MaterialBodyClient() {
        super(BASE_URL, RESULTS_URL);
    }

    public static MaterialBodyClient getInstance() {
        if (instance == null) {
            synchronized (MaterialBodyClient.class) {
                if (instance == null) {
                    instance = new MaterialBodyClient();
                }
            }
        }
        return instance;
    }

    /**
     * The {@code limit} brain tree bodies nearest the given point that hold {@code englishMaterial}, at any
     * distance, nearest first. Empty when Spansh has none or does not answer.
     */
    public List<StellarObjectSearchResultDto.Result> nearestBrainTrees(
            String englishMaterial, double x, double y, double z, int limit) {
        return search(MaterialBodyCriteria.brainTrees(englishMaterial, x, y, z, limit));
    }

    /**
     * The {@code limit} landable bodies nearest the given point, within {@code rangeLy}, that hold
     * {@code englishMaterial}, nearest first. Empty when Spansh has none or does not answer.
     */
    public List<StellarObjectSearchResultDto.Result> nearestLandableBodies(
            String englishMaterial, double x, double y, double z, int rangeLy, int limit) {
        return search(MaterialBodyCriteria.landableBodies(englishMaterial, x, y, z, rangeLy, limit));
    }

    private List<StellarObjectSearchResultDto.Result> search(MaterialBodyCriteria criteria) {
        JsonObject response = performSearch(criteria);
        if (response == null) return List.of();
        StellarObjectSearchResultDto result = GsonFactory.getGson().fromJson(response, StellarObjectSearchResultDto.class);
        if (result == null || result.getResults() == null) return List.of();
        return result.getResults();
    }
}
