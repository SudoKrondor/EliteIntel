package elite.intel.gameapi.search.spansh.braintrees;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;
import java.util.Map;

/**
 * A Spansh body search for brain tree sites on bodies that hold one raw material, nearest first.
 * <p>
 * Spansh does the filtering and the sorting: it knows of more than ten thousand brain tree bodies, so no
 * local sample can say which is nearest. Without the sort, Spansh returns its matches in no useful order.
 */
public class BrainTreeCriteria extends BaseJsonDto implements ToJsonConvertible {

    static final String GENUS = "Brain Trees";

    /**
     * Spansh's range comparison: a share between the two values, inclusive.
     */
    private static final String BETWEEN = "<=>";

    @SerializedName("filters")
    private final Filters filters;

    @SerializedName("sort")
    private final List<Map<String, Map<String, String>>> sort =
            List.of(Map.of("distance", Map.of("direction", "asc")));

    @SerializedName("size")
    private final int size;

    @SerializedName("page")
    private final int page = 0;

    @SerializedName("reference_coords")
    private final ReferenceCoords referenceCoords;

    /**
     * @param englishMaterial the raw material by its English name, as Spansh spells it ("Tellurium")
     * @param size            how many of the nearest sites to return
     */
    public BrainTreeCriteria(String englishMaterial, double x, double y, double z, int size) {
        this.filters = new Filters(englishMaterial);
        this.referenceCoords = new ReferenceCoords(x, y, z);
        this.size = size;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    private static final class Filters {
        @SerializedName("distance")
        private final Distance distance = new Distance();

        @SerializedName("genuses")
        private final Genuses genuses = new Genuses();

        @SerializedName("materials")
        private final List<MaterialFilter> materials;

        private Filters(String englishMaterial) {
            this.materials = List.of(new MaterialFilter(englishMaterial));
        }
    }

    private static final class Genuses {
        @SerializedName("value")
        private final List<String> value = List.of(GENUS);
    }

    /**
     * No upper bound: the sort puts the nearest first, and a far site is still better than no answer.
     */
    private static final class Distance {
        @SerializedName("min")
        private final int min = 0;
        @SerializedName("max")
        private final int max = Integer.MAX_VALUE;
    }

    /**
     * Any share of the material at all: the body holding it is what makes the site yield it.
     */
    private static final class MaterialFilter {
        @SerializedName("name")
        private final String name;
        @SerializedName("comparison")
        private final String comparison = BETWEEN;
        @SerializedName("value")
        private final List<Integer> value = List.of(0, 100);

        private MaterialFilter(String name) {
            this.name = name;
        }
    }

    private record ReferenceCoords(
            @SerializedName("x") double x,
            @SerializedName("y") double y,
            @SerializedName("z") double z) {
    }
}
