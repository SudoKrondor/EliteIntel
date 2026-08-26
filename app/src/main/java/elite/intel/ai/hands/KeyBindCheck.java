package elite.intel.ai.hands;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class KeyBindCheck {

    private static final Logger log = LogManager.getLogger(KeyBindCheck.class);

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
        List<String> blocking = monitor.blockingConflicts();

        // Blocking conflicts first, and unconditionally: this is the one binding problem that stops
        // EliteIntel driving the game at all rather than degrading it, and the commander cannot discover
        // it by playing - by hand they click the search field with the mouse and never notice. Announced on
        // every start for as long as it is in the file, because a once-only warning leaves a permanently
        // broken setup permanently silent. See BindingConflictRules#isBlocking.
        if (!blocking.isEmpty()) {
            GameEventBus.publish(new AiVoxResponseEvent(
                    StringUtls.localizedSpeech("speech.bindingConflictsBlocking")
            ));
            blocking.forEach(c -> {
                UiBus.publish(new AppLogEvent("BLOCKING binding conflict: " + c));
                // ERROR so the line survives into elite-intel.log and the diagnostics bundle, which is
                // where a support request starts. A config the app cannot work around is an error here.
                log.error("Blocking binding conflict: {}", c);
            });
        }

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
