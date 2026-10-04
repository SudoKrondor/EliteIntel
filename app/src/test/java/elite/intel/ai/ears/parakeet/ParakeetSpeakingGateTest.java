package elite.intel.ai.ears.parakeet;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.actions.handlers.commands.builtin.InterruptCommand;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.vega.input.BargeInEvent;
import elite.intel.ai.ears.MicrophoneGate;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParakeetSpeakingGateTest {
    private final Recorder recorder = new Recorder();
    private ParakeetSTTImpl stt;
    private Language previousLanguage;

    @BeforeEach
    void setUp() throws Exception {
        previousLanguage = SystemSession.getInstance().getLanguage();
        SystemSession.getInstance().setLanguage(Language.EN);
        stt = new ParakeetSTTImpl();
        Field field = ParakeetSTTImpl.class.getDeclaredField("isSpeaking");
        field.setAccessible(true);
        ((AtomicBoolean) field.get(stt)).set(true);
        GameEventBus.register(recorder);
    }

    @AfterEach
    void tearDown() {
        SystemSession.getInstance().setLanguage(previousLanguage);
        GameEventBus.unregister(recorder);
        if (stt != null) {
            GameEventBus.unregister(stt);
        }
    }

    @Test
    void hotMicTranscriptUsesSingleBargeInOwnerAndStillDispatchesWhileVegaSpeaks() throws Exception {
        sendToAi("plot route home", false);

        assertFalse(recorder.has(TTSInterruptEvent.class),
                "Parakeet must not duplicate the TTS interrupt owned by BargeInController");
        assertTrue(recorder.has(BargeInEvent.class));
        assertTrue(recorder.has(UserInputEvent.class));
    }

    @Test
    void anInterruptPhraseHeardOverPlaybackStopsHerAndGoesNoFurther() throws Exception {
        String interrupt = AiActionLocalizations.phrasesForAction(InterruptCommand.ID).getFirst();

        admitInterruptOnly(interrupt, MicrophoneGate.OPEN_HANDS_FREE);

        assertTrue(recorder.has(BargeInEvent.class));
        assertFalse(recorder.has(UserInputEvent.class), "nothing heard over playback reaches the LLM");
    }

    @Test
    void anythingElseHeardOverPlaybackIsDropped() throws Exception {
        admitInterruptOnly("plot route home", MicrophoneGate.OPEN_HANDS_FREE);

        assertFalse(recorder.has(BargeInEvent.class));
        assertFalse(recorder.has(UserInputEvent.class));
    }

    @Test
    void asleepNotEvenAnInterruptPhraseGetsPastPlayback() throws Exception {
        String interrupt = AiActionLocalizations.phrasesForAction(InterruptCommand.ID).getFirst();

        admitInterruptOnly(interrupt, MicrophoneGate.CLOSED_ASLEEP);

        assertFalse(recorder.has(BargeInEvent.class));
        assertFalse(recorder.has(UserInputEvent.class));
    }

    private void admitInterruptOnly(String transcript, MicrophoneGate decision) throws Exception {
        Method method = ParakeetSTTImpl.class.getDeclaredMethod("admitInterruptOnly", String.class, MicrophoneGate.class);
        method.setAccessible(true);
        method.invoke(stt, transcript, decision);
    }

    private void sendToAi(String transcript, boolean pttCapture) throws Exception {
        Method method = ParakeetSTTImpl.class.getDeclaredMethod("sendToAi", String.class, boolean.class);
        method.setAccessible(true);
        method.invoke(stt, transcript, pttCapture);
    }

    private static final class Recorder {
        private final List<Object> events = new ArrayList<>();

        @Subscribe
        public void onAny(Object event) {
            events.add(event);
        }

        boolean has(Class<?> type) {
            return events.stream().anyMatch(type::isInstance);
        }
    }
}
