package elite.intel.db.managers;

import elite.intel.ai.mouth.VoiceTuning;
import elite.intel.ai.mouth.supertonic.SupertonicVoices;
import elite.intel.db.dao.SupertonicVoiceTuningDao;
import elite.intel.db.util.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupertonicVoiceTuningManagerTest {

    private final SupertonicVoiceTuningManager tunings = SupertonicVoiceTuningManager.getInstance();

    @AfterEach
    void untuneTheVoicesTouched() {
        tunings.reset(SupertonicVoices.LEWIS);
        tunings.reset(SupertonicVoices.NOVA);
    }

    @Test
    void aVoiceNeverTunedPlaysAtTheDefaults() {
        assertEquals(VoiceTuning.DEFAULT, tunings.tuningOf(SupertonicVoices.NOVA));
    }

    @Test
    void aSavedTuningIsStoredAndReadBack() {
        VoiceTuning tuning = new VoiceTuning(35, 120, new int[]{-3, 0, 2, 0, 4, -6});

        tunings.save(SupertonicVoices.LEWIS, tuning);

        assertEquals(tuning, tunings.tuningOf(SupertonicVoices.LEWIS));
        SupertonicVoiceTuningDao.Tuning row = rows().stream()
                .filter(r -> r.getVoice().equals("LEWIS")).findFirst().orElseThrow();
        assertEquals(35, row.getSpeed());
        assertEquals(120, row.getVolume());
        assertEquals(-3, row.getEq1());
        assertEquals(-6, row.getEq6());
        assertEquals(VoiceTuning.DEFAULT, tunings.tuningOf(SupertonicVoices.NOVA), "one voice's tuning is its own");
    }

    @Test
    void resetPutsTheVoiceBackToTheDefaultsAndDropsItsRow() {
        tunings.save(SupertonicVoices.LEWIS, VoiceTuning.DEFAULT.withVolume(80));

        tunings.reset(SupertonicVoices.LEWIS);

        assertEquals(VoiceTuning.DEFAULT, tunings.tuningOf(SupertonicVoices.LEWIS));
        assertTrue(rows().stream().noneMatch(r -> r.getVoice().equals("LEWIS")));
    }

    private static List<SupertonicVoiceTuningDao.Tuning> rows() {
        return Database.withDao(SupertonicVoiceTuningDao.class, SupertonicVoiceTuningDao::all);
    }
}
