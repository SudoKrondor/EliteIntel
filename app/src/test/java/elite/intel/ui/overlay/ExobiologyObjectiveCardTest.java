package elite.intel.ui.overlay;

import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static elite.intel.ui.overlay.ExobiologyFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The sampling list is derived on every poll rather than maintained, so these
 * pin the derivation: a sampled genus has to leave the card by itself, and the
 * card has to disappear when the body is finished. Nothing else clears it.
 */
class ExobiologyObjectiveCardTest {

    private static final String ID = "exobiology:1:2";

    /**
     * Language is DB-backed and shared by every test in the fork, so it is restored here rather than by
     * the one test that changes it.
     */
    @AfterEach
    void restoreLanguage() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void aFreshlySurveyedBodyListsEveryGenus() {
        LocationDto body = bodyWith(genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;"),
                genus("Fonticulua", "$Codex_Ent_Fonticulus_Genus_Name;"));

        HudObjective card = ExobiologyObjectiveSource.card(body, List.of(), ID).orElseThrow();

        assertEquals("EXOBIOLOGY", card.title());
        assertEquals(HudObjective.PRIORITY_AMBIENT, card.priority());
        assertEquals(List.of("GENUS", "BACTERIUM", "FONTICULUA"), labels(card));
        assertEquals(0, card.rows().getFirst().current());
        assertEquals(2, card.rows().getFirst().max());
    }

    @Test
    void aCompletedGenusLeavesTheCard() {
        LocationDto body = bodyWith(genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;"),
                genus("Fonticulua", "$Codex_Ent_Fonticulus_Genus_Name;"));

        HudObjective card = ExobiologyObjectiveSource.card(
                body, List.of(completed("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;")), ID).orElseThrow();

        assertEquals(List.of("GENUS", "FONTICULUA"), labels(card));
        assertEquals(1, card.rows().getFirst().current(), "one of two genuses is done");
        assertEquals(2, card.rows().getFirst().max());
    }

    @Test
    void aFinishedBodyShowsNoCard() {
        LocationDto body = bodyWith(genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;"));

        assertTrue(ExobiologyObjectiveSource.card(
                body, List.of(completed("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;")), ID).isEmpty());
    }

    @Test
    void aBodyWithNoBiologyShowsNoCard() {
        assertTrue(ExobiologyObjectiveSource.card(bodyWith(), List.of(), ID).isEmpty());
    }

    @Test
    void theGenusBeingSampledShowsItsSampleProgress() {
        LocationDto body = bodyWith(genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;"));
        body.addBioScan(partial("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;", 1));
        body.addBioScan(partial("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;", 2));

        HudObjective card = ExobiologyObjectiveSource.card(body, List.of(), ID).orElseThrow();
        HudRow genusRow = card.rows().get(1);

        assertTrue(genusRow.hasProgress(), "an in-progress genus renders as a bar");
        assertEquals(2, genusRow.current(), "the furthest sample reached wins");
        assertEquals(3, genusRow.max());
    }

    /**
     * Pre-symbol partials carry only the localised name and must still bar.
     */
    @Test
    void aLegacyPartialStillShowsSampleProgress() {
        LocationDto body = bodyWith(genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;"));
        body.addBioScan(partial("Bacterium", null, 1));

        HudRow genusRow = ExobiologyObjectiveSource.card(body, List.of(), ID).orElseThrow().rows().get(1);

        assertTrue(genusRow.hasProgress());
        assertEquals(1, genusRow.current());
    }

    @Test
    void anUntouchedGenusShowsItsPayoutRatherThanABar() {
        GenusDto bacterium = genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;");
        bacterium.setRewardInCredits(1_000_000);
        bacterium.setBonusCreditsForFirstDiscovery(500_000);
        LocationDto body = bodyWith(bacterium);
        body.setOurDiscovery(true);

        HudObjective card = ExobiologyObjectiveSource.card(body, List.of(), ID).orElseThrow();
        HudRow genusRow = card.rows().get(1);

        assertFalse(genusRow.hasProgress());
        assertEquals("1,500,000 cr", genusRow.value());
    }

    /**
     * Vista Genomics pays the bonus for the first log of an organism, which is a different question
     * from who charted the body - another commander can have found the planet and never landed on it,
     * or landed and sampled it out. On a body we did not chart the answer is unknowable, so the row
     * quotes only what the sample is certainly worth rather than a figure that may never arrive.
     */
    @Test
    void aBodySomeoneElseChartedQuotesNoFirstDiscoveryBonus() {
        GenusDto bacterium = genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;");
        bacterium.setRewardInCredits(1_000_000);
        bacterium.setBonusCreditsForFirstDiscovery(500_000);
        LocationDto body = bodyWith(bacterium);
        body.setOurDiscovery(false);

        HudObjective card = ExobiologyObjectiveSource.card(body, List.of(), ID).orElseThrow();

        assertEquals("1,000,000 cr", card.rows().get(1).value());
    }

    /**
     * The completed samples the remainder is derived from are session state that a sale clears, so
     * without the flag on the body a sold-off survey came back onto the card as work still to do.
     */
    @Test
    void aBodyFlaggedSampledOutShowsNoCardEvenWithNoSamplesLeftInSession() {
        LocationDto body = bodyWith(genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;"),
                genus("Fonticulua", "$Codex_Ent_Fonticulus_Genus_Name;"));
        body.setBioScansCompleted(true);

        assertTrue(ExobiologyObjectiveSource.card(body, List.of(), ID).isEmpty());
    }

    /**
     * The payout is read by the commander, so it carries their own separators - not the developer's
     * machine's, and not the United States' either.
     */
    @Test
    void thePayoutIsGroupedInTheCommandersLanguage() {
        GenusDto bacterium = genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;");
        bacterium.setRewardInCredits(1_000_000);
        bacterium.setBonusCreditsForFirstDiscovery(500_000);
        LocationDto body = bodyWith(bacterium);
        body.setOurDiscovery(true);

        SystemSession.getInstance().setLanguage(Language.IT);

        HudObjective card = ExobiologyObjectiveSource.card(body, List.of(), ID).orElseThrow();

        assertEquals("1.500.000 cr", card.rows().get(1).value());
    }

    /**
     * The overlay silently drops rows past its own limit, so the overflow has to
     * be folded into a row that fits rather than sent and lost.
     */
    @Test
    void moreGenusesThanFitAreCountedInOneRow() {
        GenusDto[] many = new GenusDto[ExobiologyObjectiveSource.MAX_GENUS_ROWS + 2];
        for (int i = 0; i < many.length; i++) {
            many[i] = genus("Genus" + i, "$Codex_Ent_Genus" + i + "_Name;");
        }

        HudObjective card = ExobiologyObjectiveSource.card(bodyWith(many), List.of(), ID).orElseThrow();

        assertEquals(ExobiologyObjectiveSource.MAX_GENUS_ROWS + 2, card.rows().size(),
                "header + capped genus rows + overflow");
        assertEquals("MORE GENUS", card.rows().getLast().label());
        assertEquals("+2", card.rows().getLast().value());
    }

    /**
     * Pre-symbol samples only carry the localised name; they must still match.
     */
    @Test
    void aLegacySampleWithoutASymbolStillCountsAsDone() {
        LocationDto body = bodyWith(genus("Bacterium", "$Codex_Ent_Bacterial_Genus_Name;"));
        BioSampleDto legacy = completed("Bacterium", null);

        Optional<HudObjective> card = ExobiologyObjectiveSource.card(body, List.of(legacy), ID);

        assertTrue(card.isEmpty());
    }

    // -- fixtures --------------------------------------------------------------

    private static List<String> labels(HudObjective card) {
        return card.rows().stream().map(HudRow::label).toList();
    }
}
