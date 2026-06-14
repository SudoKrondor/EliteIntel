package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(GameSessionDao.GameSessionMapper.class)
public interface GameSessionDao {


    @SqlUpdate("""
            INSERT OR REPLACE INTO game_session (id, aiPersonality,  aiCadence, aiVoice, aiApiKey, ttsApiKey, sttApiKey,
                                                             edsmApiKey, loggingEnabled, privacyModeOn, rmsThresholdHigh,
                                                             rmsThresholdLow, encryptedLLMKey, encryptedTTSKey,
                                                             encryptedEDSSMKey, speechSpeed, localLlmCommandModel, localLlmQueryModel,
                                                             useLocalCommandLlm, useLocalQueryLlm, useLocalTTS, notificationVolume, sttThreads, voiceVolume,
                                                             localLlmProvider, localLlmAddress, conversationModeOn,
                                                             ollamaAddress, ollamaCommandModel, ollamaQueryModel,
                                                             lmStudioAddress, lmStudioCommandModel, lmStudioQueryModel,
                                                             aiLanguage,
                                                             audioInputDevice, audioOutputDevice,
                                                             pushToTalkEnabled, pushToTalkControllerName,
                                                             pushToTalkButtonIndex, pushToTalkToggleMode
                                                )
                                  VALUES (1, :aiPersonality, :aiCadence, :aiVoice, :aiApiKey, :ttsApiKey, :sttApiKey,
                                                      :edsmApiKey, :loggingEnabled, :privacyModeOn, :rmsThresholdHigh,
                                                      :rmsThresholdLow, :encryptedLLMKey, :encryptedTTSKey,
                                                      :encryptedEDSSMKey,  :speechSpeed, :localLlmCommandModel, :localLlmQueryModel,
                                                      :useLocalCommandLlm, :useLocalQueryLlm, :useLocalTTS, :notificationVolume, :sttThreads, :voiceVolume,
                                                      :localLlmProvider, :localLlmAddress, :conversationModeOn,
                                                      :ollamaAddress, :ollamaCommandModel, :ollamaQueryModel,
                                                      :lmStudioAddress, :lmStudioCommandModel, :lmStudioQueryModel,
                                                      :aiLanguage,
                                                      :audioInputDevice, :audioOutputDevice,
                                                      :pushToTalkEnabled, :pushToTalkControllerName,
                                                      :pushToTalkButtonIndex, :pushToTalkToggleMode
                                          )
            """)
    void save(@BindBean GameSessionDao.GameSession data);

    @SqlQuery("SELECT * FROM game_session WHERE id = 1")
    GameSession get();


    class GameSessionMapper implements RowMapper<GameSessionDao.GameSession> {

        @Override public GameSession map(ResultSet rs, StatementContext ctx) throws SQLException {
            GameSession session = new GameSession();
            session.setAiPersonality(rs.getString("aiPersonality"));
            session.setAiCadence(rs.getString("aiCadence"));

            /// >> DEPRECATED
            session.setAiApiKey(rs.getString("aiApiKey"));
            session.setTtsApiKey(rs.getString("ttsApiKey"));
            session.setSttApiKey(rs.getString("sttApiKey"));
            /// <<

            session.setEncryptedLLMKey(rs.getString("encryptedLLMKey"));
            session.setEncryptedTTSKey(rs.getString("encryptedTTSKey"));
            session.setEncryptedEDSSMKey(rs.getString("encryptedEDSSMKey"));

            session.setLoggingEnabled(rs.getBoolean("loggingEnabled"));
            session.setAiVoice(rs.getString("aiVoice"));
            session.setPrivacyModeOn(rs.getBoolean("privacyModeOn"));
            session.setRmsThresholdHigh(rs.getDouble("rmsThresholdHigh"));
            session.setRmsThresholdLow(rs.getDouble("rmsThresholdLow"));
            session.setEdsmApiKey(rs.getString("edsmApiKey"));

            session.setLocalLlmCommandModel(rs.getString("localLlmCommandModel"));
            session.setLocalLlmQueryModel(rs.getString("localLlmQueryModel"));
            session.setSpeechSpeed(rs.getFloat("speechSpeed"));

            session.setUseLocalCommandLlm(rs.getBoolean("useLocalCommandLlm"));
            session.setUseLocalQueryLlm(rs.getBoolean("useLocalQueryLlm"));
            session.setUseLocalTTS(rs.getBoolean("useLocalTTS"));
            session.setNotificationVolume(rs.getFloat("notificationVolume"));
            session.setSttThreads(rs.getInt("sttThreads"));
            session.setVoiceVolume(rs.getInt("voiceVolume"));
            session.setLocalLlmProvider(rs.getString("localLlmProvider"));
            session.setLocalLlmAddress(rs.getString("localLlmAddress"));
            session.setConversationModeOn(rs.getBoolean("conversationModeOn"));
            session.setOllamaAddress(rs.getString("ollamaAddress"));
            session.setOllamaCommandModel(rs.getString("ollamaCommandModel"));
            session.setOllamaQueryModel(rs.getString("ollamaQueryModel"));
            session.setLmStudioAddress(rs.getString("lmStudioAddress"));
            session.setLmStudioCommandModel(rs.getString("lmStudioCommandModel"));
            session.setLmStudioQueryModel(rs.getString("lmStudioQueryModel"));
            session.setAiLanguage(rs.getString("aiLanguage"));
            session.setAudioInputDevice(rs.getString("audioInputDevice"));
            session.setAudioOutputDevice(rs.getString("audioOutputDevice"));
            session.setPushToTalkEnabled(rs.getBoolean("pushToTalkEnabled"));
            session.setPushToTalkControllerName(rs.getString("pushToTalkControllerName"));
            session.setPushToTalkButtonIndex(rs.getInt("pushToTalkButtonIndex"));
            session.setPushToTalkToggleMode(rs.getBoolean("pushToTalkToggleMode"));
            return session;
        }
    }


    class GameSession {
        private String aiPersonality;
        private String aiCadence;

        private String aiApiKey;
        private String ttsApiKey;
        private String sttApiKey;

        private String encryptedLLMKey;
        private String encryptedTTSKey;
        private String encryptedEDSSMKey;

        private Boolean loggingEnabled;
        private String aiVoice;
        private Boolean privacyModeOn;
        private Double rmsThresholdHigh = 460.00;
        private Double rmsThresholdLow = 100.00;
        private String edsmApiKey;

        private Float speechSpeed;
        private Float notificationVolume;
        private String localLlmCommandModel;
        private String localLlmQueryModel;
        private boolean useLocalCommandLlm;
        private boolean useLocalQueryLlm;
        private boolean useLocalTTS;
        private Integer sttThreads;
        private Integer voiceVolume;
        private String localLlmProvider;
        private String localLlmAddress;
        private boolean conversationModeOn;
        private String ollamaAddress;
        private String ollamaCommandModel;
        private String ollamaQueryModel;
        private String lmStudioAddress;
        private String lmStudioCommandModel;
        private String lmStudioQueryModel;
        private String aiLanguage;
        private String audioInputDevice;
        private String audioOutputDevice;
        private boolean pushToTalkEnabled;
        private String pushToTalkControllerName;
        private int pushToTalkButtonIndex = -1;
        private boolean pushToTalkToggleMode = true;


        public String getAiPersonality() {
            return aiPersonality;
        }

        public void setAiPersonality(String aiPersonality) {
            this.aiPersonality = aiPersonality;
        }

        public String getAiApiKey() {
            return aiApiKey;
        }

        public void setAiApiKey(String aiApiKey) {
            this.aiApiKey = aiApiKey;
        }

        public String getAiCadence() {
            return aiCadence;
        }

        public void setAiCadence(String aiCadence) {
            this.aiCadence = aiCadence;
        }

        public String getTtsApiKey() {
            return ttsApiKey;
        }

        public void setTtsApiKey(String ttsApiKey) {
            this.ttsApiKey = ttsApiKey;
        }

        public String getSttApiKey() {
            return sttApiKey;
        }

        public void setSttApiKey(String sttApiKey) {
            this.sttApiKey = sttApiKey;
        }

        public Boolean getLoggingEnabled() {
            return loggingEnabled;
        }

        public void setLoggingEnabled(Boolean loggingEnabled) {
            this.loggingEnabled = loggingEnabled;
        }

        public String getAiVoice() {
            return aiVoice;
        }

        public void setAiVoice(String aiVoice) {
            this.aiVoice = aiVoice;
        }

        public Boolean getPrivacyModeOn() {
            return privacyModeOn;
        }

        public void setPrivacyModeOn(Boolean privacyModeOn) {
            this.privacyModeOn = privacyModeOn;
        }

        public Double getRmsThresholdLow() {
            return rmsThresholdLow;
        }

        public void setRmsThresholdLow(Double rmsThresholdLow) {
            this.rmsThresholdLow = rmsThresholdLow;
        }

        public Double getRmsThresholdHigh() {
            return rmsThresholdHigh;
        }

        public void setRmsThresholdHigh(Double rmsThresholdHigh) {
            this.rmsThresholdHigh = rmsThresholdHigh;
        }

        public void setEdsmApiKey(String edsmApiKey) {
            this.edsmApiKey = edsmApiKey;
        }

        public String getEdsmApiKey() {
            return edsmApiKey;
        }

        public String getEncryptedLLMKey() {
            return encryptedLLMKey;
        }

        public void setEncryptedLLMKey(String encryptedLLMKey) {
            this.encryptedLLMKey = encryptedLLMKey;
        }


        public String getEncryptedTTSKey() {
            return encryptedTTSKey;
        }

        public void setEncryptedTTSKey(String encryptedTTSKey) {
            this.encryptedTTSKey = encryptedTTSKey;
        }

        public String getEncryptedEDSSMKey() {
            return encryptedEDSSMKey;
        }

        public void setEncryptedEDSSMKey(String encryptedEDSSMKey) {
            this.encryptedEDSSMKey = encryptedEDSSMKey;
        }

        public Float getSpeechSpeed() {
            return speechSpeed;
        }

        public void setSpeechSpeed(Float speechSpeed) {
            this.speechSpeed = speechSpeed;
        }

        public String getLocalLlmCommandModel() {
            return localLlmCommandModel;
        }

        public void setLocalLlmCommandModel(String localLlmCommandModel) {
            this.localLlmCommandModel = localLlmCommandModel;
        }

        public String getLocalLlmQueryModel() {
            return localLlmQueryModel;
        }

        public void setLocalLlmQueryModel(String localLlmQueryModel) {
            this.localLlmQueryModel = localLlmQueryModel;
        }


        public boolean isUseLocalCommandLlm() {
            return useLocalCommandLlm;
        }

        public void setUseLocalCommandLlm(boolean useLocalCommandLlm) {
            this.useLocalCommandLlm = useLocalCommandLlm;
        }

        public boolean isUseLocalQueryLlm() {
            return useLocalQueryLlm;
        }

        public void setUseLocalQueryLlm(boolean useLocalQueryLlm) {
            this.useLocalQueryLlm = useLocalQueryLlm;
        }

        public boolean isUseLocalTTS() {
            return useLocalTTS;
        }

        public void setUseLocalTTS(boolean useLocalTTS) {
            this.useLocalTTS = useLocalTTS;
        }

        public Float getNotificationVolume() {
            return notificationVolume;
        }

        public void setNotificationVolume(Float notificationVolume) {
            this.notificationVolume = notificationVolume;
        }

        public Integer getSttThreads() {
            return sttThreads;
        }

        public void setSttThreads(Integer sttThreads) {
            this.sttThreads = sttThreads;
        }

        public Integer getVoiceVolume() {
            return voiceVolume;
        }

        public void setVoiceVolume(Integer voiceVolume) {
            this.voiceVolume = voiceVolume;
        }

        public String getLocalLlmProvider() {
            return localLlmProvider;
        }

        public void setLocalLlmProvider(String localLlmProvider) {
            this.localLlmProvider = localLlmProvider;
        }

        public String getLocalLlmAddress() {
            return localLlmAddress;
        }

        public void setLocalLlmAddress(String localLlmAddress) {
            this.localLlmAddress = localLlmAddress;
        }

        public boolean isConversationModeOn() {
            return conversationModeOn;
        }

        public void setConversationModeOn(boolean conversationModeOn) {
            this.conversationModeOn = conversationModeOn;
        }

        public String getOllamaAddress() {
            return ollamaAddress;
        }

        public void setOllamaAddress(String ollamaAddress) {
            this.ollamaAddress = ollamaAddress;
        }

        public String getOllamaCommandModel() {
            return ollamaCommandModel;
        }

        public void setOllamaCommandModel(String ollamaCommandModel) {
            this.ollamaCommandModel = ollamaCommandModel;
        }

        public String getOllamaQueryModel() {
            return ollamaQueryModel;
        }

        public void setOllamaQueryModel(String ollamaQueryModel) {
            this.ollamaQueryModel = ollamaQueryModel;
        }

        public String getLmStudioAddress() {
            return lmStudioAddress;
        }

        public void setLmStudioAddress(String lmStudioAddress) {
            this.lmStudioAddress = lmStudioAddress;
        }

        public String getLmStudioCommandModel() {
            return lmStudioCommandModel;
        }

        public void setLmStudioCommandModel(String lmStudioCommandModel) {
            this.lmStudioCommandModel = lmStudioCommandModel;
        }

        public String getLmStudioQueryModel() {
            return lmStudioQueryModel;
        }

        public void setLmStudioQueryModel(String lmStudioQueryModel) {
            this.lmStudioQueryModel = lmStudioQueryModel;
        }

        public String getAiLanguage() {
            return aiLanguage;
        }

        public void setAiLanguage(String aiLanguage) {
            this.aiLanguage = aiLanguage;
        }

        public String getAudioInputDevice() {
            return audioInputDevice;
        }

        public void setAudioInputDevice(String audioInputDevice) {
            this.audioInputDevice = audioInputDevice;
        }

        public String getAudioOutputDevice() {
            return audioOutputDevice;
        }

        public void setAudioOutputDevice(String audioOutputDevice) {
            this.audioOutputDevice = audioOutputDevice;
        }

        public boolean isPushToTalkEnabled() {
            return pushToTalkEnabled;
        }

        public void setPushToTalkEnabled(boolean pushToTalkEnabled) {
            this.pushToTalkEnabled = pushToTalkEnabled;
        }

        public String getPushToTalkControllerName() {
            return pushToTalkControllerName;
        }

        public void setPushToTalkControllerName(String pushToTalkControllerName) {
            this.pushToTalkControllerName = pushToTalkControllerName;
        }

        public int getPushToTalkButtonIndex() {
            return pushToTalkButtonIndex;
        }

        public void setPushToTalkButtonIndex(int pushToTalkButtonIndex) {
            this.pushToTalkButtonIndex = pushToTalkButtonIndex;
        }

        public boolean isPushToTalkToggleMode() {
            return pushToTalkToggleMode;
        }

        public void setPushToTalkToggleMode(boolean pushToTalkToggleMode) {
            this.pushToTalkToggleMode = pushToTalkToggleMode;
        }
    }
}
