package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The commander's own word about a body they sampled before this app existed - the one thing no
 * journal event will ever supply. It writes to a stored row on nothing but a spoken phrase, so what
 * matters here is the guard: which bodies it can be said about at all, and that it goes both ways.
 */
class SetCurrentBodyExobiologySurveyCompleteCommandTest {

    private static final long BODY = 2L;
    private static final long BARREN_BODY = 3L;
    /**
     * Locations live in one shared test database with no delete-by-key, so each run gets its own star
     * and system rather than cleaning up after itself.
     */
    private static final AtomicInteger RUN = new AtomicInteger();

    private final PlayerSession session = PlayerSession.getInstance();
    private final LocationManager locations = LocationManager.getInstance();
    private final SetCurrentBodyExobiologySurveyCompleteCommand command =
            new SetCurrentBodyExobiologySurveyCompleteCommand();

    private long system;
    private String star;
    private String planet;
    private LocationData<Long, Long> previousLocation;

    @BeforeEach
    void setUp() {
        int run = RUN.incrementAndGet();
        star = String.format("Exobio Command Test %03d", run);
        system = 581167680000L + run;
        planet = star + " " + BODY;

        previousLocation = session.getLocationData();
        saveBody(BODY, planet, "Bacterium");
        saveBody(BARREN_BODY, star + " " + BARREN_BODY);
        session.setCurrentLocationId(BODY, system);
    }

    @AfterEach
    void restoreSession() {
        if (previousLocation != null && previousLocation.getSystemAddress() != null) {
            session.setCurrentLocationId(
                    previousLocation.getInGameId() == null ? 0 : previousLocation.getInGameId(),
                    previousLocation.getSystemAddress());
        }
    }

    @Test
    void markingRecordsTheSurveyAsCompleteOnTheStoredBody() {
        command.execute(state(true), null);

        assertTrue(locations.findBySystemAddress(system, BODY).isBioScansCompleted());
    }

    /**
     * The undo half. Without it a misheard phrase - or a model that guessed the boolean - would take a
     * body off the sampling list for good, with nothing the commander could say to get it back.
     */
    @Test
    void clearingPutsTheBodyBackOnTheSamplingList() {
        command.execute(state(true), null);

        command.execute(state(false), null);

        assertFalse(locations.findBySystemAddress(system, BODY).isBioScansCompleted());
    }

    /**
     * The guard that makes the command hard to fire by accident: a body with no biology on it is not a
     * body a survey can be finished on, so the command is never offered there.
     */
    @Test
    void aBodyWithNoBiologyDoesNotOfferTheCommand() {
        session.setCurrentLocationId(BARREN_BODY, system);

        assertFalse(command.isVisibleForLLM(Status.getInstance()));
    }

    @Test
    void aBodyWithBiologyOffersTheCommand() {
        assertTrue(command.isVisibleForLLM(Status.getInstance()));
    }

    /**
     * Still offered once recorded, because that body is precisely the one the undo is for.
     */
    @Test
    void aBodyAlreadyRecordedStillOffersTheCommand() {
        command.execute(state(true), null);

        assertTrue(command.isVisibleForLLM(Status.getInstance()));
    }

    /**
     * Visibility is re-checked at execution: the commander can leave the body between the turn being
     * offered and the words arriving, and this writes to a stored row.
     */
    @Test
    void aBodyWithNoBiologyIsNotRecordedEvenIfTheCommandIsReached() {
        session.setCurrentLocationId(BARREN_BODY, system);

        command.execute(state(true), null);

        assertFalse(locations.findBySystemAddress(system, BARREN_BODY).isBioScansCompleted());
    }

    // -- fixtures --------------------------------------------------------------

    private static JsonObject state(boolean value) {
        JsonObject params = new JsonObject();
        params.addProperty("state", value);
        return params;
    }

    private void saveBody(long bodyId, String planetName, String... genusNames) {
        LocationDto body = new LocationDto(bodyId, system);
        body.setStarName(star);
        body.setPlanetName(planetName);
        body.setPlanetShortName(planetName.substring(planetName.lastIndexOf(' ') + 1));
        body.setBioSignals(genusNames.length);
        body.setGenus(List.of(genusNames).stream().map(name -> {
            GenusDto genus = new GenusDto();
            genus.setGenusLocalised(name);
            genus.setGenusSymbol("$Codex_Ent_" + name + "_Genus_Name;");
            genus.setPlanetName(planetName);
            return genus;
        }).toList());
        locations.save(body);
    }
}
