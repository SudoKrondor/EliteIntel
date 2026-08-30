package elite.intel.gameapi.journal.events.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two body flags exobiology money is derived from, and the directions each is allowed to move in.
 * They latch opposite ways on purpose, because the game makes one of them reversible and the other not.
 */
class LocationDtoDiscoveryFlagsTest {

    /**
     * The old latch ("once ours, always ours") made a wrong claim permanent, and one writer produced
     * wrong claims routinely: EDSM has no discovery record for most of the galaxy, so a missing record
     * read as "nobody found it before us". The journal's own scan has to be able to say otherwise, or
     * the body keeps paying an unearned first-discovery bonus into every projection.
     */
    @Test
    void aRealScanCanTakeBackAWrongDiscoveryClaim() {
        LocationDto body = new LocationDto(2L, 1L);
        body.setOurDiscovery(true);

        body.setOurDiscovery(false);

        assertFalse(body.isOurDiscovery());
    }

    /**
     * The other direction, which is what a body nobody had charted actually looks like.
     */
    @Test
    void aScanCanClaimAnUnchartedBody() {
        LocationDto body = new LocationDto(2L, 1L);

        body.setOurDiscovery(true);

        assertTrue(body.isOurDiscovery());
    }

    /**
     * A sampled-out body never goes back to having organics left, so evidence only ever moves the flag
     * one way. A later read of a sample list that a sale has emptied must not un-complete it.
     */
    @Test
    void evidenceCannotUncompleteAFinishedSurvey() {
        LocationDto body = new LocationDto(2L, 1L);
        body.markBioScansCompleted();

        body.markBioScansCompleted();

        assertTrue(body.isBioScansCompleted());
    }

    /**
     * The commander is not evidence, they are the correction: a misheard phrase or a guessed boolean
     * would otherwise retire a body from exobiology for good, with nothing to say to get it back.
     */
    @Test
    void theCommanderCanTakeBackAFinishedSurvey() {
        LocationDto body = new LocationDto(2L, 1L);
        body.markBioScansCompleted();

        body.setBioScansCompleted(false);

        assertFalse(body.isBioScansCompleted());
    }

    @Test
    void aBodyStartsWithItsSurveyUnfinished() {
        assertFalse(new LocationDto(2L, 1L).isBioScansCompleted());
    }
}
