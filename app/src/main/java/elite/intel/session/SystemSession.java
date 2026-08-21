package elite.intel.session;

import elite.intel.ai.brain.ShipPersonality;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.edge.EdgeVoices;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.db.dao.ChatHistoryDao;
import elite.intel.db.dao.GameSessionDao;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ShipManager;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.util.Cypher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SystemSession {

    public static final int GOOGLE_WAVENET_PITCH_MIN = -20;
    public static final int GOOGLE_WAVENET_PITCH_MAX = 20;

    private Double rms = 0.0;
    private Double floor = 0.0;
    private static volatile SystemSession instance;
    private final ShipManager shipManager = ShipManager.getInstance();

    private SystemSession() {
    }


    public static SystemSession getInstance() {
        if (instance == null) {
            synchronized (Status.class) {
                if (instance == null) {
                    instance = new SystemSession();
                }
            }
        }
        return instance;
    }


    // Voice and personality are per-ship: each ship carries one voice (ship.voice, interpreted against the
    // active TTS provider's enum) and one personality (ship.personality). Switching TTS provider reinterprets
    // the stored voice name and falls back to the provider's default when it isn't a valid voice there. The
    // companion and the legacy brain both read getAIPersonality(), so they follow the active ship's personality.

    // Ship voices are female-only: femaleOrDefault() resolves the stored name to a female voice (the named
    // voice if it's a valid female voice for this provider, otherwise the provider's default female). That
    // seam also heals existing commanders whose ship still carries a legacy male voice. Radio transmissions
    // are a separate channel and still use the full voice set (see VocalisationRouter).

    public GoogleVoices getGoogleVoice() {
        ShipDao.Ship ship = shipManager.getShip();
        return GoogleVoices.femaleOrDefault(ship == null ? null : ship.getVoice());
    }


    public String getEdgeVoiceName() {
        ShipDao.Ship ship = shipManager.getShip();
        return EdgeVoices.femaleShortNameOrDefault(ship == null ? null : ship.getVoice());
    }


    public KokoroVoices getKokoroVoice() {
        ShipDao.Ship ship = shipManager.getShip();
        return KokoroVoices.femaleOrDefault(ship == null ? null : ship.getVoice());
    }


    public ShipPersonality getAIPersonality() {
        ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) return ShipPersonality.CASUAL;
        String personality = ship.getPersonality();
        if (personality == null) return ShipPersonality.CASUAL;
        try {
            return ShipPersonality.valueOf(personality);
        } catch (IllegalArgumentException e) {
            return ShipPersonality.CASUAL;
        }
    }


    public Double getRmsThresholdHigh() {
        if (rms == null || rms == 0.0) {
            return Database.withDao(GameSessionDao.class, dao -> {
                GameSessionDao.GameSession session = dao.get();
                Double rmsThresholdHigh = session.getRmsThresholdHigh();
                this.rms = rmsThresholdHigh;
                return rmsThresholdHigh;
            });
        } else {
            return rms;
        }
    }

    public void setRmsThresholdHigh(Double rmsThresholdHigh) {
        this.rms = rmsThresholdHigh;
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setRmsThresholdHigh(rmsThresholdHigh == null ? 0.0 : rmsThresholdHigh);
            dao.save(session);
            return Void.TYPE;
        });
    }


    public Double getRmsThresholdLow() {
        if (floor == null || floor == 0.0) {
            return Database.withDao(GameSessionDao.class, dao -> {
                GameSessionDao.GameSession session = dao.get();
                Double rmsThresholdLow = session.getRmsThresholdLow();
                this.floor = rmsThresholdLow;
                return rmsThresholdLow;
            });
        } else {
            return floor;
        }
    }

    public void setRmsThresholdLow(Double rmsThresholdLow) {
        this.floor = rmsThresholdLow;
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setRmsThresholdLow(rmsThresholdLow == null ? 0.0 : rmsThresholdLow);
            dao.save(session);
            return Void.TYPE;
        });
    }


    public String getTtsApiKey() {
        return Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            return Cypher.decrypt(session.getEncryptedTTSKey());
        });
    }


    public void setTtsApiKey(String ttsApiKey) {
        if (ttsApiKey == null || ttsApiKey.isBlank()) {
            Database.withDao(GameSessionDao.class, dao -> {
                GameSessionDao.GameSession session = dao.get();
                session.setEncryptedTTSKey(null);
                dao.save(session);
                return Void.class;
            });
        } else {
            Database.withDao(GameSessionDao.class, dao -> {
                GameSessionDao.GameSession session = dao.get();
                session.setEncryptedTTSKey(Cypher.encrypt(ttsApiKey.trim()));
                dao.save(session);
                return null;
            });
        }
    }

    public String getAiApiKey() {
        return Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            return Cypher.decrypt(session.getEncryptedLLMKey());
        });
    }


    public void setAiApiKey(String aiApiKey) {
        if (aiApiKey == null || aiApiKey.isBlank()) {
            Database.withDao(GameSessionDao.class, dao -> {
                GameSessionDao.GameSession session = dao.get();
                session.setEncryptedLLMKey(null);
                dao.save(session);
                return Void.class;
            });
        } else {
            Database.withDao(GameSessionDao.class, dao -> {
                GameSessionDao.GameSession session = dao.get();
                session.setEncryptedLLMKey(Cypher.encrypt(aiApiKey.trim()));
                dao.save(session);
                return null;
            });
        }
    }


    public String readVersionFromResources() {
        try {
            InputStream is = getClass().getResourceAsStream("/version.txt");
            return new BufferedReader(new InputStreamReader(is)).readLine();
        } catch (IOException e) {
            return "Unknown";
        }
    }


    public void setSpeechSpeed(float speed) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setSpeechSpeed(speed);
            dao.save(session);
            return Void.class;
        });
    }

    public void setBeepVolume(float volume) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setNotificationVolume(volume);
            dao.save(session);
            return Void.class;
        });
    }

    public float getBeepVolume() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getNotificationVolume());
    }

    public Float getSpeechSpeed() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getSpeechSpeed());
    }

    public int getGoogleWaveNetPitch() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getGoogleWaveNetPitch());
    }

    public void setGoogleWaveNetPitch(int pitch) {
        if (pitch < GOOGLE_WAVENET_PITCH_MIN || pitch > GOOGLE_WAVENET_PITCH_MAX) {
            throw new IllegalArgumentException("Google WaveNet pitch must be between -20 and 20 semitones");
        }
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setGoogleWaveNetPitch(pitch);
            dao.save(session);
            return null;
        });
    }

    public int getSttThreads() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getSttThreads());
    }

    public void setSttThreads(int threads) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setSttThreads(threads);
            dao.save(session);
            return Void.class;
        });
    }


    public void setUseLocalCommandLlm(boolean b) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setUseLocalCommandLlm(b);
            dao.save(session);
            return Void.class;
        });
    }

    public void setUseLocalQueryLlm(boolean b) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setUseLocalQueryLlm(b);
            dao.save(session);
            return Void.class;
        });
    }

    /**
     * Selects the engine that voices the companion.
     * <p>
     * The legacy {@code useLocalTTS} flag is written alongside it. Nothing in this build reads that flag, but a
     * commander who rolls back to an earlier jar lands on a database where it still describes their choice.
     */
    public void setTtsProvider(TtsProvider provider) {
        TtsProvider selected = provider == null ? TtsProvider.KOKORO : provider;
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setTtsProvider(selected.name());
            session.setUseLocalTTS(selected.isLocal());
            dao.save(session);
            return Void.class;
        });
    }


    public boolean useLocalCommandLlm() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().isUseLocalCommandLlm());
    }


    public boolean useLocalQueryLlm() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().isUseLocalQueryLlm());
    }

    /**
     * The engine that voices the companion. Never {@code null}.
     */
    public TtsProvider getTtsProvider() {
        return Database.withDao(GameSessionDao.class, dao -> TtsProvider.fromStored(dao.get().getTtsProvider()));
    }

    /**
     * Whether the companion is voiced by the local engine. Derived from {@link #getTtsProvider()}: the stored
     * {@code useLocalTTS} flag is a rollback mirror, not a second source of truth, so it is never read here.
     */
    public boolean useLocalTTS() {
        return getTtsProvider().isLocal();
    }

    /**
     * The active ship's name, or {@code null} when no ship is known.
     * <p>
     * It used to answer "I have no designation" - a spoken sentence, from when the prompt made the AI the ship
     * itself. Its only caller writes this into a prompt as a ship name, so an unknown ship is now absent, not a
     * sentence pretending to be one.
     */
    public String getDesignation() {
        ShipDao.Ship ship = shipManager.getShip();
        return ship == null ? null : ship.getShipName();
    }


    /// 0 to 100 %
    public int getVoiceVolume() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getVoiceVolume());
    }

    public void setVoiceVolume(int volume) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setVoiceVolume(volume);
            dao.save(session);
            return null;
        });
    }

    public void clearChatHistory() {
        Database.withDao(ChatHistoryDao.class, dao -> {
            dao.clear();
            return Void.class;
        });
    }

    public void setLmStudioSettings(String address, String commandModel) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setLmStudioAddress(address);
            session.setLmStudioCommandModel(commandModel);
            dao.save(session);
            return Void.class;
        });
    }

    public String getLmStudioAddress() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getLmStudioAddress());
    }

    public String getLmStudioCommandModel() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getLmStudioCommandModel());
    }

    // Language is shared by GUI and command aliases, but still persisted in the legacy aiLanguage column.
    public Language getLanguage() {
        String raw = Database.withDao(GameSessionDao.class, dao -> dao.get().getAiLanguage());
        try {
            return Language.valueOf(raw);
        } catch (Exception e) {
            return Language.EN;
        }
    }

    public void setLanguage(Language language) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setAiLanguage(language.name());
            dao.save(session);
            return null;
        });
    }

    public String getAudioInputDevice() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getAudioInputDevice());
    }

    public void setAudioInputDevice(String device) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setAudioInputDevice(device == null || device.isBlank() ? null : device);
            dao.save(session);
            return null;
        });
    }

    public String getAudioOutputDevice() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getAudioOutputDevice());
    }

    public void setAudioOutputDevice(String device) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setAudioOutputDevice(device == null || device.isBlank() ? null : device);
            dao.save(session);
            return null;
        });
    }

    /**
     * Always false: conversation mode was a flag on the legacy brain pipeline, which no longer
     * exists. The backing column was dropped in migration 01018. Kept as a seam because the
     * action-map generator still branches on it.
     */
    public boolean conversationalModeOn() {
        return false;
    }

    /**
     * True while the Sleep/Wake gate is closed: the STT pipeline discards every transcript instead of routing
     * it, so nothing the microphone hears reaches the companion.
     * <p>
     * Only consulted while push-to-talk is off. With push-to-talk on, the mapped button is already the only
     * thing that opens the microphone, so this flag has nothing left to gate — see {@code ParakeetSTTImpl}.
     */
    public boolean isSleeping() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().isSleepWake());
    }

    public void setSleeping(boolean sleeping) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setSleepWake(sleeping);
            dao.save(session);
            return null;
        });
    }

    public boolean isPushToTalkEnabled() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().isPushToTalkEnabled());
    }

    public void setPushToTalkEnabled(boolean enabled) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setPushToTalkEnabled(enabled);
            dao.save(session);
            return null;
        });
    }

    public String getPushToTalkControllerName() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getPushToTalkControllerName());
    }

    public void setPushToTalkControllerName(String controllerName) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setPushToTalkControllerName(controllerName == null || controllerName.isBlank() ? null : controllerName);
            dao.save(session);
            return null;
        });
    }

    public int getPushToTalkButtonIndex() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getPushToTalkButtonIndex());
    }

    public void setPushToTalkButtonIndex(int buttonIndex) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setPushToTalkButtonIndex(buttonIndex);
            dao.save(session);
            return null;
        });
    }

    public boolean isNoiseReductionEnabled() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().isNoiseReductionEnabled());
    }

    public void setNoiseReductionEnabled(boolean enabled) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setNoiseReductionEnabled(enabled);
            dao.save(session);
            return null;
        });
    }

    public int getNoiseReductionStrength() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getNoiseReductionStrength());
    }

    public void setNoiseReductionStrength(int strength) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setNoiseReductionStrength(strength);
            dao.save(session);
            return null;
        });
    }

    /**
     * How the HUD overlay was last laid out.
     * <p>
     * Read and written as one value rather than five accessors because the overlay always sets all of
     * it at once (it sends a single CFG line), and because a partial write here is a layout the
     * commander never chose.
     *
     * @param alpha     background transparency in [0,1]
     * @param fontScale text size multiplier; {@code 0} means the commander has not chosen one, so the
     *                  caller derives it from screen height
     * @param width     card width in pixels
     * @param x         screen position, or {@code -1} for "wherever the overlay opens"
     * @param y         screen position, or {@code -1} for "wherever the overlay opens"
     * @param displayMode where the HUD is drawn - {@code DESKTOP}, {@code VR} or {@code BOTH}. Text
     *                  rather than the enum so the session layer does not depend on the UI layer,
     *                  and so a value written by a newer build is something an older one can fall
     *                  back from rather than fail on
     */
    public record HudOverlayLayout(double alpha, double fontScale, int width, int x, int y,
                                   String displayMode, String vrPosition) {
    }

    public HudOverlayLayout getHudOverlayLayout() {
        return Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            return new HudOverlayLayout(
                    session.getOverlayAlpha(),
                    session.getOverlayFontScale(),
                    session.getOverlayWidth(),
                    session.getOverlayX(),
                    session.getOverlayY(),
                    session.getOverlayDisplayMode(),
                    session.getOverlayVrPosition());
        });
    }

    public void setHudOverlayLayout(HudOverlayLayout layout) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setOverlayAlpha(layout.alpha());
            session.setOverlayFontScale(layout.fontScale());
            session.setOverlayWidth(layout.width());
            session.setOverlayX(layout.x());
            session.setOverlayY(layout.y());
            session.setOverlayDisplayMode(layout.displayMode());
            session.setOverlayVrPosition(layout.vrPosition());
            dao.save(session);
            return null;
        });
    }
}
