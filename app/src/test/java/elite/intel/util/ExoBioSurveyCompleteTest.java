package elite.intel.util;

import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A body yields its organics exactly once, so "is this survey finished" is the question that decides
 * whether the commander is offered the work again or warned off it. These pin the two ways the answer
 * used to come out wrong: reading an absent genus list as a finished survey, and reading one sampled
 * genus as the whole body.
 */
class ExoBioSurveyCompleteTest {

    private static final String BODY = "Boeff WX-N b8-0 2";

    @Test
    void everyDetectedGenusSampledIsAFinishedSurvey() {
        assertTrue(ExoBio.isSurveyComplete(
                List.of(genus("Bacterium", "Bacterial"), genus("Fonticulua", "Fonticulus")),
                List.of(sample("Bacterium", "Bacterial"), sample("Fonticulua", "Fonticulus")),
                BODY));
    }

    @Test
    void oneGenusOfTwoIsNotAFinishedSurvey() {
        assertFalse(ExoBio.isSurveyComplete(
                List.of(genus("Bacterium", "Bacterial"), genus("Fonticulua", "Fonticulus")),
                List.of(sample("Bacterium", "Bacterial")),
                BODY));
    }

    /**
     * A body sampled on foot without a DSS has no genus list at all. Nothing minus nothing is empty,
     * which read as "that was the last one" and declared the survey complete after the first organism.
     */
    @Test
    void aBodyWithNoDetectedGenusListIsNotFinished() {
        assertFalse(ExoBio.isSurveyComplete(List.of(), List.of(sample("Bacterium", "Bacterial")), BODY));
        assertFalse(ExoBio.isSurveyComplete(null, List.of(), BODY));
    }

    /**
     * Samples taken on the moon next door say nothing about this body.
     */
    @Test
    void samplesFromAnotherBodyDoNotFinishThisOne() {
        BioSampleDto elsewhere = sample("Bacterium", "Bacterial");
        elsewhere.setPlanetName("Boeff WX-N b8-0 3");

        assertFalse(ExoBio.isSurveyComplete(List.of(genus("Bacterium", "Bacterial")), List.of(elsewhere), BODY));
    }

    private static GenusDto genus(String localised, String symbol) {
        GenusDto dto = new GenusDto();
        dto.setGenusLocalised(localised);
        dto.setGenusSymbol(symbol);
        dto.setPlanetName(BODY);
        return dto;
    }

    private static BioSampleDto sample(String localised, String symbol) {
        BioSampleDto dto = new BioSampleDto();
        dto.setPlanetName(BODY);
        dto.setGenus(localised);
        dto.setGenusSymbol(symbol);
        dto.setScanXof3(3);
        dto.setBioSampleCompleted(true);
        return dto;
    }
}
