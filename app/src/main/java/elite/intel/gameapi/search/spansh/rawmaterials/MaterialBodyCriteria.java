package elite.intel.gameapi.search.spansh.rawmaterials;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;
import java.util.Map;

/**
 * A Spansh body search for bodies that hold one raw material, nearest first: either brain tree sites, or
 * landable bodies to collect it from the surface.
 * <p>
 * Spansh filters and sorts: it knows of more than ten thousand brain tree bodies alone. Without the sort it
 * returns its matches in no useful order. Its material filter is "holds it at all" - a share range sent with
 * it was ignored (measured 2026-11-08), so ranking by share is the caller's job.
 */
public class MaterialBodyCriteria extends BaseJsonDto implements ToJsonConvertible {

    static final String BRAIN_TREES = "Brain Trees";

    /** Spansh's range comparison. Only the presence of the material is honoured. */
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

    private MaterialBodyCriteria(Filters filters, double x, double y, double z, int size) {
        this.filters = filters;
        this.referenceCoords = new ReferenceCoords(x, y, z);
        this.size = size;
    }

    /**
     * Brain tree bodies holding the material, at any distance: the caller decides whether the nearest is
     * close enough, and names it when it is not.
     *
     * @param englishMaterial the raw material by its English name, as Spansh spells it ("Tellurium")
     */
    public static MaterialBodyCriteria brainTrees(String englishMaterial, double x, double y, double z, int size) {
        return new MaterialBodyCriteria(
                new Filters(englishMaterial, Integer.MAX_VALUE, new Genuses(), null), x, y, z, size);
    }

    /**
     * Landable bodies holding the material within {@code rangeLy}, any of which a commander can collect it on.
     *
     * @param englishMaterial the raw material by its English name, as Spansh spells it ("Tellurium")
     */
    public static MaterialBodyCriteria landableBodies(String englishMaterial, double x, double y, double z,
                                                      int rangeLy, int size) {
        return new MaterialBodyCriteria(
                new Filters(englishMaterial, rangeLy, null, new Landable()), x, y, z, size);
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    private static final class Filters {
        @SerializedName("distance")
        private final Distance distance;

        @SerializedName("genuses")
        private final Genuses genuses;

        @SerializedName("is_landable")
        private final Landable landable;

        @SerializedName("materials")
        private final List<MaterialFilter> materials;

        private Filters(String englishMaterial, int maxLy, Genuses genuses, Landable landable) {
            this.distance = new Distance(maxLy);
            this.genuses = genuses;
            this.landable = landable;
            this.materials = List.of(new MaterialFilter(englishMaterial));
        }
    }

    private static final class Genuses {
        @SerializedName("value")
        private final List<String> value = List.of(BRAIN_TREES);
    }

    private static final class Landable {
        @SerializedName("value")
        private final boolean value = true;
    }

    private static final class Distance {
        @SerializedName("min")
        private final int min = 0;
        @SerializedName("max")
        private final int max;

        private Distance(int max) {
            this.max = max;
        }
    }

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
