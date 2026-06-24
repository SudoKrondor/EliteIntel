package elite.intel.ai.brain;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;

import java.util.Map;

public class AiActionsMap {

    private static final AiActionsMap INSTANCE = new AiActionsMap();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final Status status = Status.getInstance();

    private AiActionsMap() {
        // ensure singleton pattern
    }

    public static AiActionsMap getInstance() {
        return INSTANCE;
    }

    /**
     * Creates and returns a mapping of user actions to their associated commands.
     * The method initializes a map configured with numerous commands for controlling
     * navigation, speed, flight systems, market functions, fleet carrier operations,
     * trade profile configurations, announcements, app settings, and UI panel controls.
     * Each key in the map contains a string that represents various user inputs,
     * while the value is the associated command string to perform the expected action.
     * <p>
     * This map supports multiple aliases for actions, allowing flexible voice command
     * recognition and user interaction.
     *
     * @return A map of user input strings to corresponding command strings, represented
     * as a mapping between keys (user input phrases) and values (command actions).
     */
    public Map<String, String> actionMap(boolean isDryRun) {
        // Delegated to the self-describing registry generator (C1 migration). The generator
        // reproduces the full composition and the trailing additions (mode fallback,
        // CONNECTION_CHECK, custom commands) in the same order the manual addAliases path used.
        return new AiActionMapGenerator()
                .generate(status, isDryRun, systemSession.conversationalModeOn());
    }
}
