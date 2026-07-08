package elite.intel.ui.controller;

import elite.intel.ui.controller.AppController.ServiceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the service registry against a registration silently going missing (e.g. dropped by a bad
 * merge, as happened to MOUTH/EARS - which left the app with no audio in or out). Verifies the audio
 * services are always wired and that the COMPANION subsystem (the sole LLM service) is registered.
 */
class AppControllerServicesTest {

    @Test
    void audioServicesAreAlwaysRegistered() {
        for (boolean localTts : new boolean[]{false, true}) {
            Set<ServiceType> types = AppController.buildServices(localTts).keySet();
            assertTrue(types.contains(ServiceType.MOUTH), "MOUTH (TTS) must be registered, localTts=" + localTts);
            assertTrue(types.contains(ServiceType.EARS), "EARS (STT) must be registered, localTts=" + localTts);
        }
    }

    @Test
    void radioMouthOnlyRegisteredWhenMainMouthIsNotKokoro() {
        // Cloud/main mouth: radio is voiced by a dedicated always-on Kokoro engine.
        assertTrue(AppController.buildServices(false).keySet().contains(ServiceType.RADIO_MOUTH));
        // Local (Kokoro) main mouth: it voices radio through its own queue, no extra engine.
        assertFalse(AppController.buildServices(true).keySet().contains(ServiceType.RADIO_MOUTH));
    }

    @Test
    void companionSubsystemIsRegistered() {
        assertTrue(AppController.buildServices(false).keySet().contains(ServiceType.COMPANION));
    }

    @Test
    void audioComesUpBeforeTheCompanionAndJournal() {
        List<ServiceType> order = List.copyOf(AppController.buildServices(false).keySet());
        // c5651efb intent: start Mouth and Ears before the journal/aux monitors and the LLM subsystem.
        assertTrue(order.indexOf(ServiceType.MOUTH) < order.indexOf(ServiceType.JOURNAL_PARSER));
        assertTrue(order.indexOf(ServiceType.EARS) < order.indexOf(ServiceType.JOURNAL_PARSER));
        assertTrue(order.indexOf(ServiceType.EARS) < order.indexOf(ServiceType.COMPANION));
    }

    @Test
    void liveGameFileMonitorsStartAfterCompanionAndEventConsumers() {
        for (boolean localTts : new boolean[]{false, true}) {
            List<ServiceType> order = List.copyOf(AppController.buildServices(localTts).keySet());
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
