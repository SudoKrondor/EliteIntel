package elite.intel.ai.brain;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.setup.SetupCheck;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;

/**
 * Startup guard that warns (via TTS, in the user's language) when the app is configured to run against a
 * local LLM whose served model is not the one this version is built around.
 * <p>
 * V1.0 targeted {@code tulu3.1:8b-supernova}; V1.1 is developed around {@code gemma-4-e4b} because the
 * companion pipeline needs tool calling. The served model can be namespaced (e.g. {@code google/gemma-4-e4b}),
 * so the match is a case-insensitive substring on the root name {@code gemma-4-e4b}. Cloud LLMs are not
 * checked - only a configured local model is. Runs on every startup and nags every time it fails, by design.
 */
public class LocalLlmModelCheck {

    /**
     * Root name of the supported local model; matched case-insensitively as a substring (see class doc).
     */
    static final String SUPPORTED_LOCAL_MODEL_ROOT = "gemma-4-e4b";

    private static volatile LocalLlmModelCheck instance;

    private LocalLlmModelCheck() {
    }

    public static synchronized LocalLlmModelCheck getInstance() {
        if (instance == null) instance = new LocalLlmModelCheck();
        return instance;
    }

    /**
     * Warns if a local LLM is configured with an unsupported model. No-op when a cloud LLM is in use.
     * <p>
     * The warning is published as a {@link VocalisationRequestEvent} straight to the TTS engine rather than
     * routed through {@code VocalisationRouter}/the companion: this warning is <em>about</em> a broken model,
     * so voicing it must not depend on the companion or the model itself. Going direct to the mouth bypasses
     * both the router's mode logic and any LLM round-trip. The line is spoken uninterruptible and logged.
     */
    public void check() {
        SystemSession session = SystemSession.getInstance();
        if (!session.useLocalCommandLlm()) return; // cloud LLM - nothing to verify
        // A fresh install has no model named at all, which is not a wrong model - SetupCheck owns that case and
        // says something useful about it. Without this guard the first thing a new commander hears is that their
        // model is not Gemma, when they have not chosen one yet.
        if (SetupCheck.isLlmUnconfigured()) return;

        String model = session.getLmStudioCommandModel();
        if (isSupported(model)) return;

        GameEventBus.publish(new VocalisationRequestEvent(
                StringUtls.localizedSpeech("speech.unsupportedLocalModel"),
                MissionCriticalAnnouncementEvent.class, false));
        UiBus.publish(new AppLogEvent(
                "Unsupported local LLM model configured: '" + model + "'. Supported: *" + SUPPORTED_LOCAL_MODEL_ROOT + "*"));
    }

    static boolean isSupported(String model) {
        return model != null && model.trim().toLowerCase().contains(SUPPORTED_LOCAL_MODEL_ROOT);
    }
}
