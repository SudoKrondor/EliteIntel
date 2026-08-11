package elite.intel.ui.overlay;

import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;

import java.util.ArrayList;
import java.util.List;

/**
 * A surveyed body and the samples taken on it, built in memory.
 * <p>
 * Shared by the exobiology card tests in this package because they all need the same three shapes and
 * the card is derived from them alone. Kept apart from {@link HudCards}, which reads a finished card
 * rather than building what goes into one.
 * <p>
 * Deliberately not used by {@code ExobiologyObjectiveLifecycleTest}: that one saves through the location
 * manager with a per-run star and system so its rows cannot collide with another test's, which is a
 * different fixture with a different job.
 */
final class ExobiologyFixtures {

    /**
     * One body for every test here, so a sample and the body it was taken on always match.
     */
    static final String BODY_NAME = "Boeff WX-N b8-0 2";

    private ExobiologyFixtures() {
    }

    static LocationDto bodyWith(GenusDto... genuses) {
        LocationDto body = new LocationDto(2L, 1L);
        body.setPlanetName(BODY_NAME);
        body.setPlanetShortName("2");
        if (genuses.length > 0) body.setGenus(new ArrayList<>(List.of(genuses)));
        return body;
    }

    static GenusDto genus(String localised, String symbol) {
        GenusDto dto = new GenusDto();
        dto.setGenusLocalised(localised);
        dto.setGenusSymbol(symbol);
        dto.setPlanetName(BODY_NAME);
        return dto;
    }

    static BioSampleDto completed(String localised, String symbol) {
        BioSampleDto dto = new BioSampleDto();
        dto.setPlanetName(BODY_NAME);
        dto.setGenus(localised);
        dto.setGenusSymbol(symbol);
        dto.setScanXof3(3);
        dto.setBioSampleCompleted(true);
        return dto;
    }

    /**
     * A genus part way through its three samples, which is what puts a bar on its row instead of a payout.
     */
    static BioSampleDto partial(String localised, String symbol, int scanXof3) {
        BioSampleDto dto = completed(localised, symbol);
        dto.setScanXof3(scanXof3);
        dto.setBioSampleCompleted(false);
        return dto;
    }
}
