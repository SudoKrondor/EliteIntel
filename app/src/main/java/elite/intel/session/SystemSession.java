package elite.intel.session;

import elite.intel.ai.brain.LocalLlmProvider;
import elite.intel.ai.brain.ShipPersonality;
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


    public GoogleVoices getGoogleVoice() {
        ShipDao.Ship ship = ShipManager.getInstance().getShip();
        if (ship == null) return GoogleVoices.STEVE;
        String voice = ship.getVoice();
        if (voice == null) return GoogleVoices.STEVE;
        try {
            return GoogleVoices.valueOf(voice);
        } catch (IllegalArgumentException e) {
            return GoogleVoices.STEVE;
        }
    }


    public KokoroVoices getKokoroVoice() {
        ShipDao.Ship ship = ShipManager.getInstance().getShip();
        if (ship == null) return KokoroVoices.BELLA;
        String voice = ship.getVoice();
        if (voice == null) return KokoroVoices.BELLA;
        try {
            return KokoroVoices.valueOf(voice);
        } catch (IllegalArgumentException e) {
            return KokoroVoices.BELLA;
        }
    }

    private void setShipVoice(String voice) {
        ShipManager shipManager = ShipManager.getInstance();
        ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) return;
        ship.setVoice(voice);
        shipManager.saveShip(ship);
    }


    public ShipPersonality getAIPersonality() {
        ShipDao.Ship ship = shipManager.getShip();
        if (ship == null) return ShipPersonality.CASUAL;
        return ShipPersonality.valueOf(ship.getPersonality());
    }



    public boolean isSleepingModeOn() {
        return Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            return session.getPrivacyModeOn();
        });
    }

    public void stopStartListening(boolean streamingModeOn) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setPrivacyModeOn(streamingModeOn);
            dao.save(session);
            return null;
        });
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
                session.setTtsApiKey(null);
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
                session.setAiApiKey(null);
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

    public void setUseLocalTTS(boolean b) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setUseLocalTTS(b);
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

    public boolean useLocalTTS() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().isUseLocalTTS());
    }

    public LocalLlmProvider getLocalLlmProvider() {
        String raw = Database.withDao(GameSessionDao.class, dao -> dao.get().getLocalLlmProvider());
        try {
            return LocalLlmProvider.valueOf(raw);
        } catch (Exception e) {
            return LocalLlmProvider.OLLAMA;
        }
    }

    public void setLocalLlmProvider(LocalLlmProvider provider) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setLocalLlmProvider(provider.name());
            dao.save(session);
            return Void.class;
        });
    }

    public String getDesignation() {
        ShipDao.Ship ship = shipManager.getShip();
        return ship == null ? "I have no designation" : ship.getShipName();
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

    public void setOllamaSettings(String address, String commandModel, String queryModel) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setOllamaAddress(address);
            session.setOllamaCommandModel(commandModel);
            session.setOllamaQueryModel(queryModel);
            dao.save(session);
            return Void.class;
        });
    }

    public void setLmStudioSettings(String address, String commandModel, String queryModel) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setLmStudioAddress(address);
            session.setLmStudioCommandModel(commandModel);
            session.setLmStudioQueryModel(queryModel);
            dao.save(session);
            return Void.class;
        });
    }

    public String getOllamaAddress() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getOllamaAddress());
    }

    public String getOllamaCommandModel() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getOllamaCommandModel());
    }

    public String getOllamaQueryModel() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getOllamaQueryModel());
    }

    public String getLmStudioAddress() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getLmStudioAddress());
    }

    public String getLmStudioCommandModel() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getLmStudioCommandModel());
    }

    public String getLmStudioQueryModel() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().getLmStudioQueryModel());
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

    public void setConversationalMode(boolean b) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setConversationModeOn(b);
            dao.save(session);
            return Void.TYPE;
        });
    }

    public boolean conversationalModeOn() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().isConversationModeOn());
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

    public boolean isPushToTalkToggleMode() {
        return Database.withDao(GameSessionDao.class, dao -> dao.get().isPushToTalkToggleMode());
    }

    public void setPushToTalkToggleMode(boolean toggleMode) {
        Database.withDao(GameSessionDao.class, dao -> {
            GameSessionDao.GameSession session = dao.get();
            session.setPushToTalkToggleMode(toggleMode);
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
}
