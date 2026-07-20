package elite.intel.junit.gameapi.journal.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.SellOrganicDataEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The narration payload must carry the finished totals: a real sale of ten samples was
 * announced as 132,500,000 credits instead of 221,670,000 because the LLM was left to add
 * up the rows, and it both dropped first-discovery bonuses and miscounted duplicate variants.
 */
class SellOrganicDataEventTest {

    @Test
    void totalsIncludeFirstDiscoveryBonus() {
        SellOrganicDataEvent event = sale();

        assertEquals(44_334_000L, event.getTotalValue());
        assertEquals(177_336_000L, event.getTotalBonus());
        assertEquals(221_670_000L, event.getTotalCredits());
    }

    @Test
    void saleByGenusRollsUpDuplicateVariants() {
        List<SellOrganicDataEvent.GenusSale> byGenus = sale().getSaleByGenus();

        assertEquals(5, byGenus.size(), "five distinct genera across ten samples");
        assertEquals(new SellOrganicDataEvent.GenusSale("Concha", 1, 11_762_000L), byGenus.get(0));
        assertEquals(new SellOrganicDataEvent.GenusSale("Bacterium", 3, 15_000_000L), byGenus.get(1));
        assertEquals(new SellOrganicDataEvent.GenusSale("Tubus", 3, 178_098_000L), byGenus.get(2));
        assertEquals(new SellOrganicDataEvent.GenusSale("Stratum", 1, 6_810_000L), byGenus.get(3));
        assertEquals(new SellOrganicDataEvent.GenusSale("Tussock", 2, 10_000_000L), byGenus.get(4));
    }

    @Test
    void genusTotalsSumToTheAnnouncedTotal() {
        SellOrganicDataEvent event = sale();

        long fromGenera = event.getSaleByGenus().stream()
                .mapToLong(SellOrganicDataEvent.GenusSale::credits)
                .sum();

        assertEquals(event.getTotalCredits(), fromGenera);
    }

    @Test
    void narrationPayloadCarriesTheTotals() {
        String yaml = sale().toYaml();

        assertTrue(yaml.contains("totalCredits: 221670000"), yaml);
        assertTrue(yaml.contains("totalBonus: 177336000"), yaml);
        assertTrue(yaml.contains("saleByGenus:"), yaml);
    }

    @Test
    void memorySummaryCountsSamplesAndSpeciesSeparately() {
        assertEquals("sold exobiology data: 10 samples across 5 species for 221670000 credits",
                sale().memorySummary());
    }

    @Test
    void aRowWithNoGenusFieldsIsLabelledBySpecies() {
        JsonObject row = new JsonObject();
        row.addProperty("Species_Localised", "Bacterium Aurasus");
        row.addProperty("Value", 100L);
        row.addProperty("Bonus", 0L);

        List<SellOrganicDataEvent.GenusSale> byGenus = saleOf(row).getSaleByGenus();

        assertEquals(new SellOrganicDataEvent.GenusSale("Bacterium Aurasus", 1, 100L), byGenus.get(0));
    }

    @Test
    void anUnnamedRowStillCountsTowardsTheTotal() {
        JsonObject row = new JsonObject();
        row.addProperty("Value", 100L);
        row.addProperty("Bonus", 50L);

        SellOrganicDataEvent event = saleOf(row);

        assertEquals(new SellOrganicDataEvent.GenusSale("unknown", 1, 150L), event.getSaleByGenus().get(0));
        assertEquals(event.getTotalCredits(), event.getSaleByGenus().get(0).credits(),
                "an unlabelled row must not vanish from the breakdown");
    }

    @Test
    void emptySaleCreditsNothing() {
        SellOrganicDataEvent event = saleOf();

        assertEquals(0L, event.getTotalCredits());
        assertTrue(event.getSaleByGenus().isEmpty());
    }

    /**
     * The real journal entry that was mis-announced, verbatim.
     */
    private static SellOrganicDataEvent sale() {
        JsonObject j = header();
        JsonArray bioData = new JsonArray();
        bioData.add(sample("Concha", "Concha Labiata", "Teal", 2_352_400L, 9_409_600L));
        bioData.add(sample("Bacterium", "Bacterium Aurasus", "Yellow", 1_000_000L, 4_000_000L));
        bioData.add(sample("Tubus", "Tubus Cavas", "Indigo", 11_873_200L, 47_492_800L));
        bioData.add(sample("Stratum", "Stratum Limaxus", "Lime", 1_362_000L, 5_448_000L));
        bioData.add(sample("Tussock", "Tussock Propagito", "Green", 1_000_000L, 4_000_000L));
        bioData.add(sample("Bacterium", "Bacterium Aurasus", "Green", 1_000_000L, 4_000_000L));
        bioData.add(sample("Tubus", "Tubus Cavas", "Maroon", 11_873_200L, 47_492_800L));
        bioData.add(sample("Tussock", "Tussock Propagito", "Green", 1_000_000L, 4_000_000L));
        bioData.add(sample("Bacterium", "Bacterium Aurasus", "Green", 1_000_000L, 4_000_000L));
        bioData.add(sample("Tubus", "Tubus Cavas", "Maroon", 11_873_200L, 47_492_800L));
        j.add("BioData", bioData);
        return new SellOrganicDataEvent(j);
    }

    private static SellOrganicDataEvent saleOf(JsonObject... rows) {
        JsonObject j = header();
        JsonArray bioData = new JsonArray();
        for (JsonObject row : rows) {
            bioData.add(row);
        }
        j.add("BioData", bioData);
        return new SellOrganicDataEvent(j);
    }

    private static JsonObject header() {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "SellOrganicData");
        j.addProperty("MarketID", 3712500736L);
        return j;
    }

    private static JsonObject sample(String genus, String species, String colour, long value, long bonus) {
        JsonObject entry = new JsonObject();
        entry.addProperty("Genus", "$Codex_Ent_" + genus + "_Genus_Name;");
        entry.addProperty("Genus_Localised", genus);
        entry.addProperty("Species_Localised", species);
        entry.addProperty("Variant_Localised", species + " - " + colour);
        entry.addProperty("Value", value);
        entry.addProperty("Bonus", bonus);
        return entry;
    }
}
