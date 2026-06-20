package elite.intel.ai.brain.actions.customcommand;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.util.AppPaths;
import elite.intel.util.StringUtls;

/**
 * Announces customCommand load results via TTS on application startup.
 * Call {@link #announce()} after {@link CustomCommandRegistry#load()} has completed and
 * TTS subscribers are active.
 */
public class CustomCommandLoadAnnouncement {

    private static volatile CustomCommandLoadAnnouncement instance;

    private CustomCommandLoadAnnouncement() {
    }

    public static synchronized CustomCommandLoadAnnouncement getInstance() {
        if (instance == null) instance = new CustomCommandLoadAnnouncement();
        return instance;
    }

    /**
     * Speaks a localized summary of the customCommand load result.
     * Reports the number of successfully loaded customCommands and, separately,
     * the number of customCommands skipped due to validation failures.
     */
    public void announce() {
        int loaded = CustomCommandRegistry.getInstance().getCustomCommands().size();
        int skipped = CustomCommandRegistry.getInstance().getSkippedOnLastLoad();
        String playerName = PlayerSession.getInstance().getVariablePlayerName();

        if (loaded > 0 && skipped > 0) {
            GameEventBus.publish(new AiVoxResponseEvent(
                    StringUtls.localizedSpeech("speech.customCommandsLoadedWithSkipped", playerName, loaded, skipped)
            ));
        } else if (loaded > 0) {
            GameEventBus.publish(new AiVoxResponseEvent(
                    StringUtls.localizedSpeech("speech.customCommandsLoaded", playerName, loaded)
            ));
        } else if (skipped > 0) {
            GameEventBus.publish(new AiVoxResponseEvent(
                    StringUtls.localizedSpeech("speech.customCommandsSkipped", playerName, skipped)
            ));
        }

        if (skipped > 0) {
            CustomCommandRegistry.getInstance().getSkippedLabelsOnLastLoad()
                    .forEach(label -> UiBus.publish(new AppLogEvent(
                            MultiLingualTextProvider.getText("actions.customCommands.load.invalid", label))));
        }

        if (CustomCommandRegistry.getInstance().wasLastLoadRestoredFromBackup()) {
            GameEventBus.publish(new AiVoxResponseEvent(
                    StringUtls.localizedSpeech("speech.customCommandsRestoredFromBackup", playerName)
            ));
            UiBus.publish(new AppLogEvent(
                    MultiLingualTextProvider.getText(
                            "actions.customCommands.load.corruptRestoredFromBackup",
                            AppPaths.CUSTOM_COMMANDS_FILE_NAME)));
        }
    }
}
