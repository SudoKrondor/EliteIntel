package elite.intel.ai.ears.parakeet;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.input.BargeInEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
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

    @BeforeEach
    void setUp() throws Exception {
        stt = new ParakeetSTTImpl();
        Field field = ParakeetSTTImpl.class.getDeclaredField("isSpeaking");
        field.setAccessible(true);
        ((AtomicBoolean) field.get(stt)).set(true);
        GameEventBus.register(recorder);
    }

    @AfterEach
    void tearDown() {
        GameEventBus.unregister(recorder);
        if (stt != null) {
            GameEventBus.unregister(stt);
        }
    }

    @Test
    void hotMicTranscriptInterruptsAndDispatchesWhileCompanionSpeaks() throws Exception {
        assertFalse(CompanionConfig.dropHotMicTranscriptsWhileCompanionSpeaks());

        sendToAi("plot route home", false);

        assertTrue(recorder.has(TTSInterruptEvent.class));
        assertTrue(recorder.has(BargeInEvent.class));
        assertTrue(recorder.has(UserInputEvent.class));
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
