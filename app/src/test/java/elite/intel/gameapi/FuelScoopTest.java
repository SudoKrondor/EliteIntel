package elite.intel.gameapi;

import elite.intel.gameapi.journal.events.dto.shiploadout.ModuleDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A scoopable star is only news to a ship that can scoop.
 *
 * <p>The rule is read off the loadout's module names, and those names have already been through
 * {@code LoadoutConverter}: the journal's {@code int_fuelscoop_size6_class5} arrives here spaced and
 * title-cased. Matching the raw journal spelling instead would find a scoop on no ship at all, and the
 * announcement would go quiet for everybody - which looks exactly like the feature working.
 */
class FuelScoopTest {

    @Test
    void aShipCarryingAScoopIsToldAboutFuelStars() {
        assertTrue(FuelScoop.isFittedTo(loadout(
                "Int Powerplant Size6 Class5", "Int Fuelscoop Size6 Class5", "Int Cargorack Size5 Class1")));
    }

    @Test
    void anEngineeredScoopIsStillAScoop() {
        assertTrue(FuelScoop.isFittedTo(loadout("Int Fuelscoop Size7 Class5")));
    }

    @Test
    void aShipWithoutAScoopHearsNothingAboutFuelStars() {
        assertFalse(FuelScoop.isFittedTo(loadout(
                "Int Powerplant Size6 Class5", "Int Cargorack Size5 Class1", "Int Shieldgenerator Size5 Class3")));
    }

    /**
     * A cargo scoop is not a fuel scoop. They share a word and sit in comparable slots, so a looser match on
     * "scoop" would tell every ship in the game that it can refuel at a star.
     */
    @Test
    void aCargoScoopDoesNotCountAsAFuelScoop() {
        assertFalse(FuelScoop.isFittedTo(loadout("Cargo Scoop", "Int Powerplant Size6 Class5")));
    }

    /**
     * Until the game has sent a Loadout there is nothing to read, and the honest answer is the old
     * behaviour rather than silence - see {@link FuelScoop#isFittedTo}.
     */
    @Test
    void anUnknownLoadoutKeepsTheAnnouncement() {
        assertTrue(FuelScoop.isFittedTo(null));
        assertTrue(FuelScoop.isFittedTo(loadout()));

        ShipLoadOutDto noModules = new ShipLoadOutDto();
        noModules.setModules(null);
        assertTrue(FuelScoop.isFittedTo(noModules));
    }

    @Test
    void aModuleWithNoItemNameIsSkippedRatherThanThrowing() {
        ShipLoadOutDto loadout = loadout("Int Fuelscoop Size6 Class5");
        List<ModuleDto> modules = new ArrayList<>(loadout.getModules());
        modules.addFirst(new ModuleDto());
        loadout.setModules(modules);

        assertTrue(FuelScoop.isFittedTo(loadout));
    }

    private static ShipLoadOutDto loadout(String... moduleItems) {
        ShipLoadOutDto loadout = new ShipLoadOutDto();
        List<ModuleDto> modules = new ArrayList<>();
        for (String item : moduleItems) {
            ModuleDto module = new ModuleDto();
            module.setItem(item);
            modules.add(module);
        }
        loadout.setModules(moduleItems.length == 0 ? Collections.emptyList() : modules);
        return loadout;
    }
}
