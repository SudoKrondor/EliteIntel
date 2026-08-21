package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.eventbus.UiBus;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.ToggleSleepWakeEvent;

/**
 * "Go to sleep" - closes the Sleep/Wake gate by voice, the same gate the AI tab button drives.
 * <p>
 * Publishes the request rather than writing the setting, so the spoken confirmation, the persisted flag and
 * the button label all come from the one place that owns them ({@code AppController}).
 */
@RegisterCommand
public final class SleepCommand implements IntelCommand {
    public static final String ID = "sleep_ignore_do_not_monitor";

    @Override
    public String llmDescription() {
        return "Put the companion to sleep so it stops listening and responding until explicitly woken.";
    }

    @Override
    public String id() {
        return ID;
    }

    /**
     * Companion-side control, executable in any location - but pointless under push-to-talk, where the mapped
     * button already gates the microphone and the sleep flag is not consulted at all. Withheld there rather
     * than offered as a tool that would do nothing.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return !SystemSession.getInstance().isPushToTalkEnabled();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        UiBus.publish(new ToggleSleepWakeEvent(true));
        return null;
    }
}
