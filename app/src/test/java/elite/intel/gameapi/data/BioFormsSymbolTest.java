package elite.intel.gameapi.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BioForms is keyed by Frontier's language-independent organic symbols, so every lookup must resolve
 * from a raw journal symbol regardless of the game client language. These tests pin the symbol
 * normalization (including the colour-variant codex form) and the fact that a symbol lookup never
 * returns null for a known genus - the root cause of the RU-only ScanOrganic NPE.
 */
class BioFormsSymbolTest {

    @Test
    void normalizeGenusAcceptsRawSymbolStemAndEnglish() {
        assertEquals("Tussocks", BioForms.normalizeGenus("$Codex_Ent_Tussocks_Genus_Name;"));
        assertEquals("Tussocks", BioForms.normalizeGenus("Tussocks"));   // bare stem
        assertEquals("Tussocks", BioForms.normalizeGenus("Tussock"));    // English display name
        assertEquals("Bacterial", BioForms.normalizeGenus("$Codex_Ent_Bacterial_Genus_Name;"));
    }

    @Test
    void normalizeSpeciesAcceptsSpeciesSymbolAndColourVariant() {
        // ScanOrganic Species form
        assertEquals("Fonticulus_02", BioForms.normalizeSpecies("$Codex_Ent_Fonticulus_02_Name;"));
        // CodexEntry Name form carries an extra colour-variant suffix that must be stripped
        assertEquals("Shrubs_01", BioForms.normalizeSpecies("$Codex_Ent_Shrubs_01_F_Name;"));
        assertEquals("Fungoids_01", BioForms.normalizeSpecies("$Codex_Ent_Fungoids_01_Antimony_Name;"));
        assertEquals("Tussocks_02", BioForms.normalizeSpecies("$Codex_Ent_Tussocks_02_F_Name;"));
        // species stem whose stem differs from the genus stem, no numeric suffix
        assertEquals("SeedEFGH", BioForms.normalizeSpecies("$Codex_Ent_SeedEFGH_Name;"));
    }

    @Test
    void genusResolvesFromSpeciesEvenWhenStemDiffers() {
        assertEquals("Shrubs", BioForms.genusStemForSpecies("$Codex_Ent_Shrubs_01_F_Name;"));
        // Brain Tree: genus stem "Brancae" but species stem "Seed*"
        assertEquals("Brancae", BioForms.genusStemForSpecies("$Codex_Ent_SeedEFGH_Name;"));
        // Sinuous Tuber: genus stem "Tubers" but species stem "Tube*"
        assertEquals("Tubers", BioForms.genusStemForSpecies("$Codex_Ent_TubeABCD_02_Name;"));
    }

    @Test
    void getDistanceFromSymbolNeverNullForKnownGenus() {
        // The RU NPE root cause: localized genus missed the English-keyed table and unboxed null.
        // With symbol keys this is language-independent and always an int.
        assertEquals(200, BioForms.getDistance("$Codex_Ent_Tussocks_Genus_Name;"));
        assertEquals(500, BioForms.getDistance("$Codex_Ent_Bacterial_Genus_Name;"));
        assertEquals(1000, BioForms.getDistance("$Codex_Ent_Electricae_Genus_Name;"));
        assertEquals(0, BioForms.getDistance("$Codex_Ent_NoSuchThing_Genus_Name;"));
    }

    @Test
    void detailsAndPaymentResolveFromSymbols() {
        BioForms.BioDetails details = BioForms.getDetails("$Codex_Ent_Fonticulus_02_Name;");
        assertNotNull(details);
        assertEquals(500, details.colonyRange());
        assertTrue(details.creditValue() > 0);

        BioForms.ProjectedPayment payment = BioForms.getProjectedPayment("$Codex_Ent_Fonticulus_02_Name;");
        assertNotNull(payment);
        assertNotNull(payment.payment());

        assertNotNull(BioForms.getAverageProjectedPayment("$Codex_Ent_Tussocks_Genus_Name;"));
    }

    @Test
    void genusToBiomeIsKeyedByEnglishForTheLlm() {
        Map<String, String> map = BioForms.getGenusToBiome();
        // The Biome Analysis LLM reasons in English, so keys must be display names, not symbols.
        assertTrue(map.containsKey("Tussock"), "expected English genus key");
        assertTrue(map.containsKey("Bacterium"), "expected English genus key");
        assertNull(map.get("Tussocks"), "must not be keyed by the symbol stem");
    }

    @Test
    void englishGenusNameResolvesForSpeechFallback() {
        assertEquals("Tussock", BioForms.englishGenusName("$Codex_Ent_Tussocks_Genus_Name;"));
    }
}
