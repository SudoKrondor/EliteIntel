package elite.intel.ui.controller;

import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ui.controller.AppController.ServiceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the service registry against a registration silently going missing (e.g. dropped by a bad
 * merge, as happened to MOUTH/EARS - which left the app with no audio in or out). Verifies the audio
 * services are always wired and that the COMPANION subsystem (the sole LLM service) is registered.
 */
class AppControllerServicesTest {

    @Test
    void audioServicesAreAlwaysRegistered() {
        for (TtsProvider mainMouth : TtsProvider.values()) {
            Set<ServiceType> types = AppController.buildServices(mainMouth, TtsProvider.KOKORO).keySet();
            assertTrue(types.contains(ServiceType.MOUTH), "MOUTH (TTS) must be registered, mouth=" + mainMouth);
            assertTrue(types.contains(ServiceType.EARS), "EARS (STT) must be registered, mouth=" + mainMouth);
        }
    }

    @Test
    void radioMouthOnlyRegisteredWhenTheMainMouthIsNotTheRadioEngine() {
        // Cloud main mouth, Latin-script language: radio is voiced by a dedicated always-on Kokoro engine.
        assertTrue(AppController.buildServices(TtsProvider.GOOGLE, TtsProvider.KOKORO)
                .containsKey(ServiceType.RADIO_MOUTH));
        // Kokoro main mouth, Cyrillic language: Kokoro cannot pronounce it, so Edge runs alongside for radio.
        assertTrue(AppController.buildServices(TtsProvider.KOKORO, TtsProvider.EDGE)
                .containsKey(ServiceType.RADIO_MOUTH));
        // The main mouth IS the radio engine: it voices radio through its own queue, no extra engine. A second
        // holder would hand the same singleton the RADIO role and silence all narration.
        assertFalse(AppController.buildServices(TtsProvider.KOKORO, TtsProvider.KOKORO)
                .containsKey(ServiceType.RADIO_MOUTH));
        assertFalse(AppController.buildServices(TtsProvider.EDGE, TtsProvider.EDGE)
                .containsKey(ServiceType.RADIO_MOUTH));
    }

    @Test
    void companionSubsystemIsRegistered() {
        assertTrue(AppController.buildServices(TtsProvider.GOOGLE, TtsProvider.KOKORO)
                .containsKey(ServiceType.COMPANION));
    }

    /**
     * Push-to-talk used to run off a Swing settings panel, so it worked only because that tab is built at
     * startup. As a service it needs the registry entry, and it needs the device poll loop feeding it and a
     * microphone to put to sleep before it arms.
     */
    @Test
    void pushToTalkComesUpAfterTheDevicesAndTheMicrophone() {
        for (TtsProvider mainMouth : TtsProvider.values()) {
            List<ServiceType> order =
                    List.copyOf(AppController.buildServices(mainMouth, TtsProvider.KOKORO).keySet());
            assertTrue(order.contains(ServiceType.PUSH_TO_TALK),
                    "the controller button must work without opening settings, mouth=" + mainMouth);
            assertTrue(order.indexOf(ServiceType.DEVICE) < order.indexOf(ServiceType.PUSH_TO_TALK));
            assertTrue(order.indexOf(ServiceType.EARS) < order.indexOf(ServiceType.PUSH_TO_TALK));
        }
    }

    @Test
    void audioComesUpBeforeTheCompanionAndJournal() {
        List<ServiceType> order =
                List.copyOf(AppController.buildServices(TtsProvider.GOOGLE, TtsProvider.KOKORO).keySet());
        // c5651efb intent: start Mouth and Ears before the journal/aux monitors and the LLM subsystem.
        assertTrue(order.indexOf(ServiceType.MOUTH) < order.indexOf(ServiceType.JOURNAL_PARSER));
        assertTrue(order.indexOf(ServiceType.EARS) < order.indexOf(ServiceType.JOURNAL_PARSER));
        assertTrue(order.indexOf(ServiceType.EARS) < order.indexOf(ServiceType.COMPANION));
    }

    @Test
    void liveGameFileMonitorsStartAfterCompanionAndEventConsumers() {
        for (TtsProvider mainMouth : TtsProvider.values()) {
            List<ServiceType> order =
                    List.copyOf(AppController.buildServices(mainMouth, TtsProvider.KOKORO).keySet());
            assertTrue(order.indexOf(ServiceType.COMPANION) < order.indexOf(ServiceType.JOURNAL_PARSER));
            assertTrue(order.indexOf(ServiceType.WEB_SOCKET) < order.indexOf(ServiceType.JOURNAL_PARSER));
            assertTrue(order.indexOf(ServiceType.MISSING_MISSION_MONITOR) < order.indexOf(ServiceType.JOURNAL_PARSER));
            assertEquals(
                    List.of(ServiceType.JOURNAL_PARSER, ServiceType.AUXILIARY_FILES_MONITOR),
                    order.subList(order.size() - 2, order.size()));
        }
    }

    @Test
    void serviceHolderStartIsIdempotentUntilStopped() {
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger stops = new AtomicInteger();
        AppController.ServiceHolder holder = new AppController.ServiceHolder(() -> new ManagedService() {
            @Override
            public void start() {
                starts.incrementAndGet();
            }

            @Override
            public void stop() {
                stops.incrementAndGet();
            }
        });

        holder.start();
        holder.start();
        assertEquals(1, starts.get());

        holder.stop();
        holder.stop();
        assertEquals(1, stops.get());

        // Idempotent "until stopped": a fresh start after a stop creates a new instance and starts again.
        holder.start();
        assertEquals(2, starts.get());
    }
}
