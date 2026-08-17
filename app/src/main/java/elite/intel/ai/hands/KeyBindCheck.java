package elite.intel.ai.hands;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;

import java.util.List;

public class KeyBindCheck {

    private static volatile KeyBindCheck instance;

    private KeyBindCheck() {
    }

    public static synchronized KeyBindCheck getInstance() {
        if (instance == null) instance = new KeyBindCheck();
        return instance;
    }

    public void check() {
        BindingsMonitor monitor = BindingsMonitor.getInstance();
        // This runs right after the services start, which is when the monitor thread is still
        // registering its WatchService - without this the check would read an unparsed map and
        // report nothing at all.
        monitor.ensureBindingsLoaded();

        List<String> newMissing = monitor.checkForMissingBindings();
        List<String> newConflicts = monitor.checkForConflictsAndPersist();

        if (!newMissing.isEmpty()) {
            GameEventBus.publish(new AiVoxResponseEvent(
                    StringUtls.localizedSpeech("speech.bindingsMissing", newMissing.size())
            ));
            newMissing.forEach(m -> UiBus.publish(new AppLogEvent("Missing binding: " + m)));
        }

        if (!newConflicts.isEmpty()) {
            int count = newConflicts.size();
            GameEventBus.publish(new AiVoxResponseEvent(
                    StringUtls.localizedSpeech("speech.bindingConflicts", count)
            ));
            newConflicts.forEach(c -> UiBus.publish(new AppLogEvent("Binding conflict: " + c)));
        }
    }
}
