package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.CommodityDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.search.spansh.station.outfitting.WantedModule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A broad request - "a size 3 gimbal laser", "size 2 lasers", "missile racks" - is searched as every module
 * it can mean, derived from the seeded catalogue, and a narrow one stays exactly the module named.
 */
class ModuleFamilyTest {

    private static final List<String> STANDARD_LASERS =
            List.of("Beam Laser", "Burst Laser", "Mining Laser", "Pulse Disruptor Laser", "Pulse Laser");

    /**
     * The support-bundle case: "gimbal laser size 3" left the name "laser", which no module is called.
     */
    @Test
    void aGimbalLaserIsEveryStandardLaser() {
        WantedModule wanted = FindCommodityCommand.wantedModule(SpokenModule.parse("gimbal laser size 3"));

        assertNotNull(wanted);
        assertEquals(STANDARD_LASERS, wanted.spellings());
        assertEquals("Laser", wanted.label());
        assertTrue(wanted.isFamily());
        assertEquals(3, wanted.size());
        assertEquals("Gimbal", wanted.mount());
    }

    @Test
    void thePluralIsTheFamily() {
        WantedModule wanted = FindCommodityCommand.wantedModule(SpokenModule.parse("size 2 lasers"));

        assertNotNull(wanted);
        assertEquals(STANDARD_LASERS, wanted.spellings());
        assertEquals(2, wanted.size());
    }

    /**
     * A variant built on a family member - an engineered or powerplay laser - is not what "a laser" means.
     */
    @Test
    void variantsOfAMemberAreLeftOut() {
        List<String> members = ModuleFamily.exact("laser", FuzzySearch.shipModuleNames()).orElseThrow().members();

        assertFalse(members.contains("Retributor Beam Laser"));
        assertFalse(members.contains("Long Range Mining Laser"));
        assertFalse(members.contains("Cytoscrambler Burst Laser"));
    }

    @Test
    void aMisheardFamilyIsStillFound() {
        WantedModule wanted = FindCommodityCommand.wantedModule(SpokenModule.parse("size 2 lazers"));

        assertNotNull(wanted);
        assertEquals(STANDARD_LASERS, wanted.spellings());
    }

    /**
     * Naming the laser narrows the search to it, even through a bent transcript.
     */
    @Test
    void aNamedLaserIsThatLaserAlone() {
        assertEquals(List.of("Beam Laser"), FindCommodityCommand.wantedModule(SpokenModule.parse("beam laser")).spellings());
        assertEquals(List.of("Mining Laser"), FindCommodityCommand.wantedModule(SpokenModule.parse("size 2 minning lazer")).spellings());
        assertFalse(FindCommodityCommand.wantedModule(SpokenModule.parse("beam laser")).isFamily());
    }

    /**
     * Every seeker and pack-hound rack is built on the plain Missile Rack, so the family is that rack alone
     * and is echoed under its own name.
     */
    @Test
    void missileRacksAreThePlainRack() {
        for (String spoken : List.of("missile rack", "missile racks", "missale rack")) {
            WantedModule wanted = FindCommodityCommand.wantedModule(SpokenModule.parse(spoken));

            assertNotNull(wanted, spoken);
            assertEquals(List.of("Missile Rack"), wanted.spellings(), spoken);
            assertEquals("Missile Rack", wanted.label(), spoken);
            assertFalse(wanted.isFamily(), spoken);
        }
    }

    @Test
    void hyphensAndArticlesDoNotMatter() {
        Optional<ModuleFamily> family = ModuleFamily.exact("a multi cannons", FuzzySearch.shipModuleNames());

        assertEquals(List.of("Multi-Cannon"), family.orElseThrow().members());
    }

    @Test
    void wordsNamingNoModuleFindNothing() {
        assertNull(FindCommodityCommand.wantedModule(SpokenModule.parse("banana")));
        assertNull(FindCommodityCommand.wantedModule(SpokenModule.parse("size 3 gimbal")));
    }

    /**
     * A family word found in a commodity's name would send the good down the module branch, where it is
     * unbuyable - so no commodity the catalogue seeds may read as one.
     */
    @Test
    void noCommodityReadsAsAModuleFamily() {
        List<String> commodities = Database.withDao(CommodityDao.class, CommodityDao::getAllNamesLowerCase);
        List<String> catalogue = FuzzySearch.shipModuleNames();
        assertFalse(commodities.isEmpty());
        for (String commodity : commodities) {
            assertTrue(ModuleFamily.exact(commodity, catalogue).isEmpty(), () -> commodity + " reads as a module family");
        }
    }
}
