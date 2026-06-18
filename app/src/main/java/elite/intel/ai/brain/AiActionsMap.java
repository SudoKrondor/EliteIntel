package elite.intel.ai.brain;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;

import java.util.LinkedHashMap;
import java.util.Map;

import static elite.intel.ai.brain.actions.Queries.CONNECTION_CHECK;
import static elite.intel.ai.brain.actions.Queries.GENERAL_CONVERSATION;
import static elite.intel.ai.brain.commons.AiEndPoint.CONNECTION_CHECK_COMMAND;

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
        Map<String, String> map = new LinkedHashMap<>();

        // Add aliases only for the currently selected language.
        AiActionLocalizations.addAliases(map, status, isDryRun);

        // Conversation / ignore fallback is language-independent because these are internal action names.
        if (systemSession.conversationalModeOn()) {
            map.put("general conversation", GENERAL_CONVERSATION.getAction());
        } else {
            map.put("ignore_nonsensical_input", CommandIds.IGNORE_NONSENSICAL_INPUT);
        }

        // Machine-only command, not user-facing language.
        map.put(CONNECTION_CHECK_COMMAND, CONNECTION_CHECK.getAction());

        CustomCommandRegistry.getInstance().contributeToActionMap(map);

        return map;
    }
}
