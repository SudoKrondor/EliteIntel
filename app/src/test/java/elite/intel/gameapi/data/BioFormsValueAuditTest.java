package elite.intel.gameapi.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.data.BioFormsValueAudit.Finding;
import elite.intel.gameapi.data.BioFormsValueAudit.Verdict;
import elite.intel.gameapi.journal.events.SellOrganicDataEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The payout table is curated by hand and drifts silently; a sale is the only moment the game states
 * the truth. These pin the comparison that turns a sale into a defect report.
 * <p>
 * Fixtures are real journal lines, because the thing being tested is a claim about the journal's own
 * shape - a hand-written row that happens to parse proves nothing about what Frontier writes.
 */
class BioFormsValueAuditTest {

    /**
     * A real sale, verbatim from a commander's journal: two species, both first-logged.
     */
    private static final String REAL_SALE = """
            {"timestamp":"2026-07-13T22:42:40Z","event":"SellOrganicData","MarketID":3712500736,"BioData":[
             {"Genus":"$Codex_Ent_Tussocks_Genus_Name;","Genus_Localised":"Tussock",
              "Species":"$Codex_Ent_Tussocks_03_Name;","Species_Localised":"Tussock Ignis",
              "Variant":"$Codex_Ent_Tussocks_03_G_Name;","Variant_Localised":"Tussock Ignis - Lime",
              "Value":1849000,"Bonus":7396000},
             {"Genus":"$Codex_Ent_Shrubs_Genus_Name;","Genus_Localised":"Frutexa",
              "Species":"$Codex_Ent_Shrubs_02_Name;","Species_Localised":"Frutexa Acus",
              "Variant":"$Codex_Ent_Shrubs_02_G_Name;","Variant_Localised":"Frutexa Acus - Emerald",
              "Value":7774700,"Bonus":31098800}]}""";

    private static List<Finding> auditOf(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return BioFormsValueAudit.findings(new SellOrganicDataEvent(obj).getBioData());
    }

    @Test
    @DisplayName("a real sale that agrees with the table reports nothing wrong")
    void aCorrectTableProducesOnlyMatches() {
        List<Finding> findings = auditOf(REAL_SALE);

        assertEquals(2, findings.size());
        assertTrue(findings.stream().allMatch(f -> f.verdict() == Verdict.MATCH),
                () -> "expected all matches, got " + findings);
    }

    /**
     * The defect this whole class exists for. Tussock Ventusa was carried as 3,277,700 against a real
     * payout of 3,227,700 - transposed digits, undetectable from inside the app, and it survived until
     * two months of journals were mined by hand.
     */
    @Test
    @DisplayName("a table value that disagrees with Vista Genomics is reported with the correction")
    void aWrongTableValueIsReported() {
        // The corrected table agrees with what Vista really pays for this species.
        assertEquals(Verdict.MATCH,
                auditOf(sale("$Codex_Ent_Tussocks_02_Name;", "Tussock Ventusa", 3_227_700, 0)).getFirst().verdict());

        // The figure the table used to carry would now be caught the first time it was sold.
        Finding f = auditOf(sale("$Codex_Ent_Tussocks_02_Name;", "Tussock Ventusa", 3_277_700, 0)).getFirst();
        assertEquals(Verdict.WRONG_VALUE, f.verdict());
        assertEquals(3_277_700, f.paidValue(), "what Vista actually paid is the authority");
        assertEquals(3_227_700, f.tableValue());
        assertEquals(50_000, f.delta());
    }

    /**
     * How a new organism arrives: Frontier ships one, our table predates it, and the first commander to
     * sell it hands us both the symbol and the price.
     */
    @Test
    @DisplayName("an organism the table has never heard of is reported, not silently skipped")
    void anUnknownSpeciesIsReported() {
        List<Finding> findings = auditOf(sale("$Codex_Ent_Ingensradices_Unicus_Name;", "Radicoida Unicus", 952_296, 0));

        Finding f = findings.getFirst();
        assertEquals(Verdict.UNKNOWN_SPECIES, f.verdict());
        assertEquals(952_296, f.paidValue());
    }

    /**
     * The 4x multiplier underpins every bonus figure in the table, so a sale contradicting it is worth
     * saying on its own rather than being reported as one species being wrong.
     */
    @Test
    @DisplayName("a first-logged bonus that is not 4x breaks the model and says so")
    void aBonusThatIsNotFourTimesIsReported() {
        List<Finding> findings = auditOf(sale("$Codex_Ent_Tussocks_03_Name;", "Tussock Ignis", 1_849_000, 9_245_000));

        assertEquals(Verdict.UNEXPECTED_BONUS, findings.getFirst().verdict());
    }

    @Test
    @DisplayName("the real 4x bonus is not mistaken for a defect")
    void afourTimesBonusIsAMatch() {
        List<Finding> findings = auditOf(sale("$Codex_Ent_Tussocks_03_Name;", "Tussock Ignis", 1_849_000, 7_396_000));

        assertEquals(Verdict.MATCH, findings.getFirst().verdict());
    }

    /**
     * A sale with no rows is a real journal shape and must not throw on the way past the clear.
     */
    @Test
    @DisplayName("an empty sale is not an error")
    void anEmptySaleProducesNoFindings() {
        assertTrue(BioFormsValueAudit.findings(null).isEmpty());
        assertTrue(auditOf("""
                {"timestamp":"2026-07-13T22:42:40Z","event":"SellOrganicData","MarketID":1,"BioData":[]}""").isEmpty());
    }

    private static String sale(String speciesSymbol, String localised, long value, long bonus) {
        return """
                {"timestamp":"2026-07-13T22:42:40Z","event":"SellOrganicData","MarketID":3712500736,"BioData":[
                 {"Species":"%s","Species_Localised":"%s","Value":%d,"Bonus":%d}]}"""
                .formatted(speciesSymbol, localised, value, bonus);
    }
}
