package elite.intel.gameapi;

import elite.intel.gameapi.journal.events.dto.shiploadout.ModuleDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spotting a planetary vehicle hangar in a ship's loadout.
 *
 * <p>The module is matched on its symbol rather than by exact name, because every size and rating of
 * hangar is the same answer to "can this hull carry a buggy".
 */
class ShipVehicleHangarTest {

    @Test
    void aHangarIsFoundWhateverItsSizeOrRating() {
        for (String item : List.of("int_buggybay_size2_class1", "int_buggybay_size4_class2",
                "int_buggybay_size6_class2", "Int_BuggyBay_Size2_Class1")) {
            assertTrue(ShipVehicleHangar.isFitted(List.of(module(item))), item + " is a vehicle hangar");
        }
    }

    @Test
    void aHangarIsFoundAmongTheRestOfTheLoadout() {
        assertTrue(ShipVehicleHangar.isFitted(List.of(
                module("int_powerplant_size6_class5"),
                module("int_buggybay_size4_class2"),
                module("hpt_pulselaser_fixed_small"))));
    }

    @Test
    void aShipWithNoHangarIsReportedAsHavingNone() {
        assertFalse(ShipVehicleHangar.isFitted(List.of(
                module("int_powerplant_size6_class5"),
                module("int_fuelscoop_size6_class5"),
                module("hpt_pulselaser_fixed_small"))));
    }

    @Test
    void aLoadoutWeHaveNeverSeenReadsAsNoHangar() {
        // The safe way round: the commander is told something they can see is wrong, rather than the app
        // pressing a sequence of keys at a ship with no bay to open.
        assertFalse(ShipVehicleHangar.isFitted(null));
        assertFalse(ShipVehicleHangar.isFitted(List.of()));
    }

    @Test
    void aModuleWithNoSymbolIsSteppedOverRatherThanThrowing() {
        assertFalse(ShipVehicleHangar.isFitted(java.util.Arrays.asList(module(null), null)));
    }

    private static ModuleDto module(String item) {
        ModuleDto module = new ModuleDto();
        module.setItem(item);
        return module;
    }
}
