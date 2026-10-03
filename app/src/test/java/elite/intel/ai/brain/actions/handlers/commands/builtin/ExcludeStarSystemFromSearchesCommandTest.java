package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.SearchExclusionManager;
import elite.intel.gameapi.search.PermitLockedSystems;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Measured 2026-09-27: a Battle Weapons search named Oz Prospect in Hyades Sector WI-S b4-4, the commander flew
 * there and found no starport, and asking again named the same system. The commander strikes it off - "this"
 * system from inside it, "that" one from anywhere - and every route search stops offering it.
 * <p>
 * The excluded list lives in the shared test database for the whole run and has no way to be emptied, so each
 * test excludes a system name nobody else uses.
 */
class ExcludeStarSystemFromSearchesCommandTest {

    private final ExcludeStarSystemFromSearchesCommand command = new ExcludeStarSystemFromSearchesCommand();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final ReminderManager reminders = ReminderManager.getInstance();
    private String previousPosition;

    @BeforeEach
    void clean() {
        previousPosition = playerSession.getPrimaryStarName();
        reminders.clear();
    }

    @AfterEach
    void tidy() {
        playerSession.setCurrentPrimaryStarName(previousPosition);
        reminders.clear();
    }

    @Test
    void thisSystemIsTheOneTheShipIsIn() {
        playerSession.setCurrentPrimaryStarName("Exclusion Test Here");
        reminders.setReminder("Buy Battle Weapons at Oz Prospect", "Exclusion Test Elsewhere", "Oz Prospect", null);

        command.execute(which(ExcludeStarSystemFromSearchesCommand.CURRENT), null);

        assertFalse(PermitLockedSystems.isReachable("Exclusion Test Here"));
        assertTrue(PermitLockedSystems.isReachable("Exclusion Test Elsewhere"), "only the system meant is struck off");
        assertNotNull(reminders.getReminder(), "an errand to another system is none of this command's business");
    }

    @Test
    void thatSystemIsWhereTheLastSearchSentUs() {
        playerSession.setCurrentPrimaryStarName("Exclusion Test Origin");
        reminders.setReminder("Buy Battle Weapons at Oz Prospect", "Exclusion Test Dead Port", "Oz Prospect", null);

        command.execute(which(ExcludeStarSystemFromSearchesCommand.DESTINATION), null);

        assertFalse(PermitLockedSystems.isReachable("Exclusion Test Dead Port"));
        assertTrue(PermitLockedSystems.isReachable("Exclusion Test Origin"));
        assertNull(reminders.getReminder(), "the errand would keep sending the commander to a port that is not there");
    }

    @Test
    void noArgumentMeansTheSystemTheShipIsIn() {
        playerSession.setCurrentPrimaryStarName("Exclusion Test Default");
        reminders.setReminder("Buy gold", "Exclusion Test Not Meant", null, null);

        command.execute(new JsonObject(), null);

        assertFalse(PermitLockedSystems.isReachable("Exclusion Test Default"));
        assertTrue(PermitLockedSystems.isReachable("Exclusion Test Not Meant"));
    }

    @Test
    void theConfirmationNamesTheSystemAndWhichOneItIs() {
        // "that" is worked out at run time, and a misheard this/that (Russian "эту"/"ту") must come back as an
        // audibly different question - not merely a different system name the commander has to catch.
        playerSession.setCurrentPrimaryStarName("Exclusion Test Prompt Here");
        reminders.setReminder("Buy gold", "Exclusion Test Prompt There", null, null);

        String destination = command.confirmationPrompt(which(ExcludeStarSystemFromSearchesCommand.DESTINATION));
        String current = command.confirmationPrompt(which(ExcludeStarSystemFromSearchesCommand.CURRENT));

        assertTrue(destination.contains("Exclusion Test Prompt There"), destination);
        assertTrue(current.contains("Exclusion Test Prompt Here"), current);
        assertNotEquals(destination.replace("Exclusion Test Prompt There", "X"),
                current.replace("Exclusion Test Prompt Here", "X"),
                "the two questions must differ in more than the system name");
        assertTrue(command.isDangerous());
    }

    @Test
    void anExcludedSystemIsPutBackFromInsideIt() {
        playerSession.setCurrentPrimaryStarName("Exclusion Test Rebuilt");
        command.execute(which(ExcludeStarSystemFromSearchesCommand.CURRENT), null);
        assertFalse(PermitLockedSystems.isReachable("Exclusion Test Rebuilt"));

        AllowStarSystemInSearchesAgainCommand allow = new AllowStarSystemInSearchesAgainCommand();
        allow.execute(new JsonObject(), null);

        assertTrue(PermitLockedSystems.isReachable("Exclusion Test Rebuilt"));
        assertFalse(allow.isDangerous(), "putting a system back loses nothing");
        assertFalse(SearchExclusionManager.getInstance().allow("Exclusion Test Rebuilt"),
                "a second allow finds nothing to take off the list");
    }

    @Test
    void withNoSearchDestinationNothingIsStruckOff() {
        playerSession.setCurrentPrimaryStarName("Exclusion Test Innocent");

        assertNull(command.confirmationPrompt(which(ExcludeStarSystemFromSearchesCommand.DESTINATION)),
                "no question to ask - the generic one stands and the command then says why it cannot");
        String answer = command.execute(which(ExcludeStarSystemFromSearchesCommand.DESTINATION), null);

        assertNotNull(answer);
        assertTrue(PermitLockedSystems.isReachable("Exclusion Test Innocent"),
                "a missing destination must never fall back to striking off the system we are in");
    }

    @Test
    void excludingTwiceIsHarmless() {
        playerSession.setCurrentPrimaryStarName("Exclusion Test Twice");
        command.execute(which(ExcludeStarSystemFromSearchesCommand.CURRENT), null);

        assertFalse(SearchExclusionManager.getInstance().exclude("exclusion test twice"),
                "names compare without regard to case");
        assertDoesNotThrow(() -> command.execute(which(ExcludeStarSystemFromSearchesCommand.CURRENT), null));
    }

    @Test
    void everyRouteSearchDropsAnExcludedSystem() {
        record Hit(String system) {
        }
        SearchExclusionManager.getInstance().exclude("Exclusion Test Filtered");

        List<Hit> kept = PermitLockedSystems.reachable(
                List.of(new Hit("Deciat"), new Hit("EXCLUSION TEST FILTERED"), new Hit("Eravate")), Hit::system);

        assertEquals(List.of(new Hit("Deciat"), new Hit("Eravate")), kept);
    }

    private static JsonObject which(String system) {
        JsonObject params = new JsonObject();
        params.addProperty(ExcludeStarSystemFromSearchesCommand.PARAM_SYSTEM, system);
        return params;
    }
}
