package elite.intel.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransmissionAudioSettingsTest {

    private final SystemSession settings = SystemSession.getInstance();

    @AfterEach
    void restoreDefaults() {
        settings.setTransmissionTones(false);
        settings.setEnhancedRadioEffect(false);
        settings.setEffectsOnRadio(false);
        settings.setEffectsOnVegaAway(false);
        settings.setTransmissionToneVolume(35);
        settings.setSupertonicBoostPercent(0);
    }

    @Test
    void optionsSurviveAnUnrelatedSettingsSave() {
        settings.setTransmissionTones(true);
        settings.setEnhancedRadioEffect(true);
        settings.setEffectsOnRadio(true);
        settings.setEffectsOnVegaAway(true);
        settings.setTransmissionToneVolume(24);
        settings.setSupertonicBoostPercent(100);
        settings.setRadioVolume(settings.getRadioVolume());

        assertTrue(settings.isTransmissionTones());
        assertTrue(settings.isEnhancedRadioEffect());
        assertTrue(settings.isEffectsOnRadio());
        assertTrue(settings.isEffectsOnVegaAway());
        assertEquals(24, settings.getTransmissionToneVolume());
        assertEquals(100, settings.getSupertonicBoostPercent());
    }
}
