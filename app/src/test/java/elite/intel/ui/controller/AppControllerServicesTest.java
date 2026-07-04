package elite.intel.ui.controller;

import elite.intel.ui.controller.AppController.ServiceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the service registry against a registration silently going missing (e.g. dropped by a bad
 * merge, as happened to MOUTH/EARS - which left the app with no audio in or out). Verifies the audio
 * services are always wired and that exactly one brain (legacy BRAIN or COMPANION) is registered.
 */
class AppControllerServicesTest {

    @Test
    void audioServicesAreAlwaysRegistered() {
        for (boolean companionMode : new boolean[]{false, true}) {
            for (boolean localTts : new boolean[]{false, true}) {
                Set<ServiceType> types = AppController.buildServices(companionMode, localTts).keySet();
                assertTrue(types.contains(ServiceType.MOUTH), "MOUTH (TTS) must be registered, companionMode=" + companionMode);
                assertTrue(types.contains(ServiceType.EARS), "EARS (STT) must be registered, companionMode=" + companionMode);
            }
        }
    }

    @Test
    void radioMouthOnlyRegisteredWhenMainMouthIsNotKokoro() {
        // Cloud/main mouth: radio is voiced by a dedicated always-on Kokoro engine.
        assertTrue(AppController.buildServices(false, false).keySet().contains(ServiceType.RADIO_MOUTH));
        // Local (Kokoro) main mouth: it voices radio through its own queue, no extra engine.
        assertFalse(AppController.buildServices(false, true).keySet().contains(ServiceType.RADIO_MOUTH));
    }

    @Test
    void exactlyOneBrainIsRegisteredPerMode() {
        Set<ServiceType> legacy = AppController.buildServices(false, false).keySet();
        assertTrue(legacy.contains(ServiceType.BRAIN));
        assertFalse(legacy.contains(ServiceType.COMPANION));

        Set<ServiceType> companion = AppController.buildServices(true, false).keySet();
        assertTrue(companion.contains(ServiceType.COMPANION));
        assertFalse(companion.contains(ServiceType.BRAIN));
    }

    @Test
    void audioComesUpBeforeTheBrainAndJournal() {
        List<ServiceType> order = List.copyOf(AppController.buildServices(false, false).keySet());
        // c5651efb intent: start Mouth and Ears before the journal/aux monitors and the brain.
        assertTrue(order.indexOf(ServiceType.MOUTH) < order.indexOf(ServiceType.JOURNAL_PARSER));
        assertTrue(order.indexOf(ServiceType.EARS) < order.indexOf(ServiceType.JOURNAL_PARSER));
        assertTrue(order.indexOf(ServiceType.EARS) < order.indexOf(ServiceType.BRAIN));
    }
}
