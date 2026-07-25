package elite.intel.junit.db;

import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the material catalogue seeded by migration 01017 — the reference data every inventory
 * lookup depends on. Sources are Frontier's own localization export (names, translations), EDCD's
 * FDevIDs (grade) and EDDI (grade, for the newer Thargoid items FDevIDs omits).
 */
class MaterialCatalogueTest {

    /**
     * Frontier's storage caps by grade. Identical across raw, manufactured and encoded.
     */
    private static final Map<Integer, Integer> CAP_BY_GRADE =
            Map.of(1, 300, 2, 250, 3, 200, 4, 150, 5, 100);

    private static List<MaterialNameDao.Material> all() {
        return Database.withDao(MaterialNameDao.class, MaterialNameDao::listAll);
    }

    private static MaterialNameDao.Material bySymbol(String symbol) {
        return Database.withDao(MaterialNameDao.class, dao -> dao.findBySymbol(symbol));
    }

    @Test
    void everyMaterialIsSeeded() {
        assertEquals(147, Database.withDao(MaterialNameDao.class, MaterialNameDao::count));
    }

    @Test
    void everyMaterialHasASymbol() {
        List<String> missing = all().stream()
                .filter(m -> m.getSymbol() == null || m.getSymbol().isBlank())
                .map(MaterialNameDao.Material::getName)
                .toList();
        assertTrue(missing.isEmpty(), "materials without a journal symbol: " + missing);
    }

    @Test
    void symbolsAreLowerCaseAsTheJournalWritesThem() {
        List<String> wrong = all().stream()
                .map(MaterialNameDao.Material::getSymbol)
                .filter(s -> s != null && !s.equals(s.toLowerCase()))
                .toList();
        assertTrue(wrong.isEmpty(), "symbols must match the journal's lower-case Name: " + wrong);
    }

    @Test
    void storageCapMatchesGradeForEveryRealMaterial() {
        List<String> wrong = all().stream()
                .filter(m -> m.getGrade() > 0)
                .filter(m -> m.getMaxCapacity() != CAP_BY_GRADE.get(m.getGrade()))
                .map(m -> m.getName() + " G" + m.getGrade() + " cap=" + m.getMaxCapacity())
                .toList();
        assertTrue(wrong.isEmpty(), "storage cap must follow grade: " + wrong);
    }

    @Test
    void focusCrystalChainRunsGradeOneToFive() {
        // A material's grade is only trustworthy if the engineering progression it belongs to reads
        // in order. This chain is the canonical example.
        assertEquals(1, bySymbol("crystalshards").getGrade());
        assertEquals(2, bySymbol("uncutfocuscrystals").getGrade());
        assertEquals(3, bySymbol("focuscrystals").getGrade());
        assertEquals(4, bySymbol("refinedfocuscrystals").getGrade());
        assertEquals(5, bySymbol("exquisitefocuscrystals").getGrade());
    }

    @Test
    void rawMaterialsAreSevenPerGradeAcrossFourGrades() {
        // Frontier's raw material table is exactly 4 grades x 7 elements; raw never reaches grade 5.
        List<MaterialNameDao.Material> raw = all().stream()
                .filter(m -> "Raw".equals(m.getMaterialType()))
                .toList();
        assertEquals(28, raw.size());
        for (int grade = 1; grade <= 4; grade++) {
            int g = grade;
            assertEquals(7, raw.stream().filter(m -> m.getGrade() == g).count(), "raw grade " + g);
        }
        assertEquals(0, raw.stream().filter(m -> m.getGrade() == 5).count(), "raw has no grade 5");
    }

    @Test
    void displayNamesCarryNoExportToolDisambiguationSuffix() {
        // Both Frontier's localization export and EDDI append their own bracketed marker; live
        // journals show the game string has none, e.g. Name_Localised="Pattern Alpha Obelisk Data".
        List<String> suffixed = all().stream()
                .map(MaterialNameDao.Material::getName)
                .filter(n -> n.endsWith("(Thargoid)") || n.endsWith("(Guardian)"))
                .toList();
        assertTrue(suffixed.isEmpty(), "display names must match the journal string: " + suffixed);
    }

    @Test
    void symbolsSeenInLiveJournalsAllResolve() {
        // Sampled from real Journal files: raw elements with no Name_Localised, multi-word
        // manufactured and encoded items, and the underscored Guardian symbols.
        for (String symbol : List.of("carbon", "nickel", "sulphur", "salvagedalloys",
                "fedcorecomposites", "shieldsoakanalysis", "fsdtelemetry", "unknownenergysource",
                "guardian_powercell", "guardian_sentinel_wreckagecomponents", "ancientbiologicaldata")) {
            MaterialNameDao.Material m = bySymbol(symbol);
            assertNotNull(m, "journal symbol not in catalogue: " + symbol);
            assertTrue(m.getMaxCapacity() > 0, "no storage cap for " + symbol);
        }
    }

    @Test
    void officiallyLocalizedLanguagesAreFullyTranslated() {
        // Frontier ships DE, ES, FR, RU and PT-BR clients. Where a cell is blank in Frontier's own
        // export, that client displays English, and we mirror it — so the check is that Spanish and
        // Russian, which Frontier translated completely, are complete here too.
        for (String col : List.of("name_es", "name_ru")) {
            assertEquals(0, blankCount(col), col + " should have no gaps");
        }
    }

    @Test
    void ukrainianAndItalianAreFullyTranslated() {
        // Not Frontier languages, so every string is ours and none may be missing.
        assertEquals(0, blankCount("name_uk"));
        assertEquals(0, blankCount("name_it"));
    }

    /**
     * {@code Database.init()} hands back an open pooled handle, so it must be closed or the pool
     * (size 1 under test) starves and every later query times out.
     */
    private static int blankCount(String column) {
        try (Handle handle = Database.init()) {
            return handle.createQuery(
                            "SELECT COUNT(*) FROM material_names WHERE <col> IS NULL OR <col> = ''")
                    .define("col", column)
                    .mapTo(Integer.class)
                    .one();
        }
    }
}
