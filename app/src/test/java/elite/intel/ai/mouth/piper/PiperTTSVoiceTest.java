package elite.intel.ai.mouth.piper;

import elite.intel.ai.mouth.TtsProvider;
import elite.intel.db.dao.GameSessionDao;
import elite.intel.db.managers.ShipLoadoutManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PiperTTSVoiceTest {

    @Test
    void ordinarySpeechUsesTheSavedVoiceWhileAuditionsUseTheirExplicitVoice() {
        SystemSession session = SystemSession.getInstance();
        ShipLoadoutManager loadouts = ShipLoadoutManager.getInstance();
        ShipLoadOutDto previousLoadout = loadouts.get();
        String previousVoice = Database.withDao(GameSessionDao.class, dao -> dao.get().getPiperVoice());
        try {
            loadouts.clear();
            PiperTTS tts = PiperTTS.getInstance();
            for (PiperVoices voice : PiperVoices.values()) {
                session.setVoiceName(TtsProvider.PIPER, voice.name());
                assertEquals(voice, session.getPiperVoice());
                assertEquals(voice.getSid(), tts.sidOf(null), "ordinary speech must follow the saved voice");
                assertEquals(PiperVoices.UPMC_JESSICA.getSid(), tts.sidOf(PiperVoices.UPMC_JESSICA.name()));
                assertEquals(PiperVoices.UPMC_PIERRE.getSid(), tts.sidOf(PiperVoices.UPMC_PIERRE.name()));
            }
        } finally {
            loadouts.clear();
            if (previousLoadout != null) loadouts.save(previousLoadout);
            Database.withDao(GameSessionDao.class, dao -> {
                GameSessionDao.GameSession data = dao.get();
                data.setPiperVoice(previousVoice);
                dao.save(data);
                return Void.TYPE;
            });
        }
    }
}
