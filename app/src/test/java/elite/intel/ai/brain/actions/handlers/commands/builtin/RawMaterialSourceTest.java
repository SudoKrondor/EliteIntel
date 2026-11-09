package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto.Result;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where to collect a raw material. A brain tree yields more than any planet surface at every grade, so one in
 * range always wins. Without one, the richest surface within two jumps wins, since two jumps cost next to
 * nothing, else the nearest surface holding it. A commander who asked for brain trees by name is never sent
 * to a bare surface.
 */
class RawMaterialSourceTest {

    private static final String TELLURIUM = "Tellurium";
    private static final int RANGE = 500;
    private static final double TWO_JUMPS = 60;

    @Test
    void aBrainTreeWithinRangeBeatsAnySurface() {
        AtomicBoolean surfacesAsked = new AtomicBoolean();
        Result tree = body("Tree System", 420, 0.5);

        RawMaterialSource.Pick pick = RawMaterialSource.pick(TELLURIUM, List.of(tree),
                asked(surfacesAsked, List.of(body("Rich Surface", 5, 3.0))), RANGE, TWO_JUMPS, false);

        assertEquals(new RawMaterialSource.BrainTree(tree), pick);
        assertFalse(surfacesAsked.get(), "a brain tree in range settles it, so the second Spansh search is not made");
    }

    @Test
    void withNoBrainTreeInRangeTheRichestSurfaceWithinTwoJumpsWins() {
        Result nearPoor = body("Near Poor", 10, 0.9);
        Result rich = body("Rich", 55, 1.5);
        Result beyondTwoJumps = body("Richer But Far", 200, 2.5);

        RawMaterialSource.Pick pick = RawMaterialSource.pick(TELLURIUM, List.of(body("Far Tree", 900, 1)),
                () -> List.of(nearPoor, rich, beyondTwoJumps), RANGE, TWO_JUMPS, false);

        assertEquals(new RawMaterialSource.Surface(rich, 1.5), pick,
                "within two jumps the share decides, and a richer body further out does not count");
    }

    @Test
    void withNothingWithinTwoJumpsTheNearestSurfaceWins() {
        Result nearest = body("Nearest", 150, 0.7);

        RawMaterialSource.Pick pick = RawMaterialSource.pick(TELLURIUM, List.of(),
                () -> List.of(nearest, body("Richer", 300, 2.0)), RANGE, TWO_JUMPS, false);

        assertEquals(new RawMaterialSource.Surface(nearest, 0.7), pick);
    }

    @Test
    void withNoKnownJumpRangeTheNearestSurfaceWins() {
        Result nearest = body("Nearest", 1, 0.7);

        RawMaterialSource.Pick pick = RawMaterialSource.pick(TELLURIUM, List.of(),
                () -> List.of(nearest, body("Richer", 2, 2.0)), RANGE, 0, false);

        assertEquals(new RawMaterialSource.Surface(nearest, 0.7), pick,
                "before the game reports a loadout there is no two-jump radius to rank within");
    }

    @Test
    void askedForBrainTreesByNameTheNearestBeyondRangeIsNamedAndNoSurfaceIsOffered() {
        AtomicBoolean surfacesAsked = new AtomicBoolean();
        Result farTree = body("Far Tree", 900, 1);

        RawMaterialSource.Pick pick = RawMaterialSource.pick(TELLURIUM, List.of(farTree),
                asked(surfacesAsked, List.of(body("Surface", 5, 3.0))), RANGE, TWO_JUMPS, true);

        assertEquals(new RawMaterialSource.BrainTreeBeyondRange(farTree), pick);
        assertFalse(surfacesAsked.get());
    }

    @Test
    void askedForBrainTreesByNameWithNoneKnownSaysSo() {
        RawMaterialSource.Pick pick = RawMaterialSource.pick(TELLURIUM, List.of(),
                () -> List.of(body("Surface", 5, 3.0)), RANGE, TWO_JUMPS, true);

        assertEquals(new RawMaterialSource.NoBrainTree(), pick);
    }

    @Test
    void nothingInRangeIsNothing() {
        RawMaterialSource.Pick pick = RawMaterialSource.pick(TELLURIUM, List.of(), List::of, RANGE, TWO_JUMPS, false);

        assertEquals(new RawMaterialSource.Nothing(), pick);
    }

    @Test
    void theShareIsReadByTheMaterialsEnglishNameInAnyCase() {
        assertEquals(1.25, RawMaterialSource.share(body("A", 1, 1.25), "tellurium"));
        assertEquals(0, RawMaterialSource.share(body("A", 1, 1.25), "Polonium"));
    }

    private static Supplier<List<Result>> asked(AtomicBoolean flag, List<Result> bodies) {
        return () -> {
            flag.set(true);
            return bodies;
        };
    }

    private static Result body(String system, double distance, double tellurium) {
        String json = """
                { "name": "%s 1", "system_name": "%s", "distance": %s,
                  "materials": [ { "name": "Iron", "share": 20.0 }, { "name": "Tellurium", "share": %s } ] }
                """.formatted(system, system, distance, tellurium);
        return GsonFactory.getGson().fromJson(json, Result.class);
    }
}
