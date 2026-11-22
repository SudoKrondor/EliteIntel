package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto.Material;
import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto.Result;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Where to collect a raw material: the decision behind {@link FindWhereToCollectRawMaterialCommand}, kept
 * free of Spansh and the session so it can be tested on its own.
 * <p>
 * A brain tree yields more of a material than any planet surface, at every grade, so one within range always
 * wins. Without one, the commander is sent to a surface: the richest in the material among those within two
 * jumps, since two jumps cost next to nothing, else the nearest that holds it at all. A commander who asked
 * for brain trees by name is never sent to a bare surface.
 */
final class RawMaterialSource {

    sealed interface Pick permits BrainTree, BrainTreeBeyondRange, NoBrainTree, Surface, Nothing {
    }

    /** The nearest brain tree site within range. */
    record BrainTree(Result site) implements Pick {
    }

    /** Brain trees were asked for by name and the nearest lies beyond the range. Named, not plotted. */
    record BrainTreeBeyondRange(Result site) implements Pick {
    }

    /** Brain trees were asked for by name and Spansh knows of none holding the material. */
    record NoBrainTree() implements Pick {
    }

    /** No brain tree within range: a landable body holding the material, with its share in percent. */
    record Surface(Result site, double share) implements Pick {
    }

    /** Nothing within range holds the material. */
    record Nothing() implements Pick {
    }

    private RawMaterialSource() {
    }

    /**
     * @param englishMaterial the material by its English name, as Spansh lists it in a body's materials
     * @param brainTrees      brain tree sites holding the material, nearest first, at any distance
     * @param surfaces        landable bodies holding the material within range, nearest first; asked only when
     *                        no brain tree will do, because it is a second Spansh search
     * @param rangeLy         how far the commander will travel
     * @param twoJumpsLy      twice the ship's jump range, or 0 when the jump range is not known
     * @param brainTreesOnly  the commander asked for brain trees by name
     */
    static Pick pick(String englishMaterial, List<Result> brainTrees, Supplier<List<Result>> surfaces,
                     int rangeLy, double twoJumpsLy, boolean brainTreesOnly) {
        if (!brainTrees.isEmpty() && brainTrees.getFirst().getDistance() <= rangeLy) {
            return new BrainTree(brainTrees.getFirst());
        }
        if (brainTreesOnly) {
            return brainTrees.isEmpty() ? new NoBrainTree() : new BrainTreeBeyondRange(brainTrees.getFirst());
        }
        List<Result> bodies = surfaces.get();
        if (bodies.isEmpty()) {
            return new Nothing();
        }
        Result best = bodies.stream()
                .filter(body -> twoJumpsLy > 0 && body.getDistance() <= twoJumpsLy)
                .max(Comparator.comparingDouble(body -> share(body, englishMaterial)))
                .orElse(bodies.getFirst());
        return new Surface(best, share(best, englishMaterial));
    }

    /** The body's share of the material in percent, 0 when Spansh did not list it. */
    static double share(Result body, String englishMaterial) {
        return body.getMaterials().stream()
                .filter(material -> englishMaterial.equalsIgnoreCase(material.getName()))
                .mapToDouble(Material::getShare)
                .findFirst()
                .orElse(0);
    }
}
