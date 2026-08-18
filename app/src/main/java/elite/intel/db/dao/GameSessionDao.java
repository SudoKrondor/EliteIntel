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
            INSERT OR REPLACE INTO game_session (id, kokoroVoice, googleVoice,
                                                             rmsThresholdHigh,
                                                             rmsThresholdLow, encryptedLLMKey, encryptedTTSKey,
                                                             speechSpeed, googleWaveNetPitch,
                                                             useLocalCommandLlm, useLocalQueryLlm, useLocalTTS, ttsProvider, notificationVolume, sttThreads, voiceVolume,
                                                             lmStudioAddress, lmStudioCommandModel,
                                                             aiLanguage,
                                                             audioInputDevice, audioOutputDevice,
                                                             pushToTalkEnabled, pushToTalkControllerName,
                                                             pushToTalkButtonIndex,
                                                             noiseReductionEnabled, noiseReductionStrength,
                                                             overlayAlpha, overlayFontScale, overlayWidth, overlayX, overlayY,
                                                             overlayDisplayMode, overlayVrPosition
                                                )
                                  VALUES (1, :kokoroVoice, :googleVoice,
                                                      :rmsThresholdHigh,
                                                      :rmsThresholdLow, :encryptedLLMKey, :encryptedTTSKey,
                                                      :speechSpeed, :googleWaveNetPitch,
                                                      :useLocalCommandLlm, :useLocalQueryLlm, :useLocalTTS, :ttsProvider, :notificationVolume, :sttThreads, :voiceVolume,
                                                      :lmStudioAddress, :lmStudioCommandModel,
                                                      :aiLanguage,
                                                      :audioInputDevice, :audioOutputDevice,
                                                      :pushToTalkEnabled, :pushToTalkControllerName,
                                                      :pushToTalkButtonIndex,
                                                      :noiseReductionEnabled, :noiseReductionStrength,
                                                      :overlayAlpha, :overlayFontScale, :overlayWidth, :overlayX, :overlayY,
                                                      :overlayDisplayMode, :overlayVrPosition
                                          )
            """)
    void save(@BindBean GameSessionDao.GameSession data);

    @SqlQuery("SELECT * FROM game_session WHERE id = 1")
    GameSession get();


    class GameSessionMapper implements RowMapper<GameSessionDao.GameSession> {

        @Override public GameSession map(ResultSet rs, StatementContext ctx) throws SQLException {
            GameSession session = new GameSession();

            session.setEncryptedLLMKey(rs.getString("encryptedLLMKey"));
            session.setEncryptedTTSKey(rs.getString("encryptedTTSKey"));

            session.setKokoroVoice(rs.getString("kokoroVoice"));
            session.setGoogleVoice(rs.getString("googleVoice"));
            session.setRmsThresholdHigh(rs.getDouble("rmsThresholdHigh"));
            session.setRmsThresholdLow(rs.getDouble("rmsThresholdLow"));

            session.setSpeechSpeed(rs.getFloat("speechSpeed"));
            session.setGoogleWaveNetPitch(rs.getInt("googleWaveNetPitch"));

            session.setUseLocalCommandLlm(rs.getBoolean("useLocalCommandLlm"));
            session.setUseLocalQueryLlm(rs.getBoolean("useLocalQueryLlm"));
            session.setUseLocalTTS(rs.getBoolean("useLocalTTS"));
            session.setTtsProvider(rs.getString("ttsProvider"));
            session.setNotificationVolume(rs.getFloat("notificationVolume"));
            session.setSttThreads(rs.getInt("sttThreads"));
            session.setVoiceVolume(rs.getInt("voiceVolume"));
            session.setLmStudioAddress(rs.getString("lmStudioAddress"));
            session.setLmStudioCommandModel(rs.getString("lmStudioCommandModel"));
            session.setAiLanguage(rs.getString("aiLanguage"));
            session.setAudioInputDevice(rs.getString("audioInputDevice"));
            session.setAudioOutputDevice(rs.getString("audioOutputDevice"));
            session.setPushToTalkEnabled(rs.getBoolean("pushToTalkEnabled"));
            session.setPushToTalkControllerName(rs.getString("pushToTalkControllerName"));
            session.setPushToTalkButtonIndex(rs.getInt("pushToTalkButtonIndex"));
            session.setNoiseReductionEnabled(rs.getBoolean("noiseReductionEnabled"));
            session.setNoiseReductionStrength(rs.getInt("noiseReductionStrength"));
            session.setOverlayAlpha(rs.getDouble("overlayAlpha"));
            session.setOverlayFontScale(rs.getDouble("overlayFontScale"));
            session.setOverlayWidth(rs.getInt("overlayWidth"));
            session.setOverlayX(rs.getInt("overlayX"));
            session.setOverlayY(rs.getInt("overlayY"));
            session.setOverlayDisplayMode(rs.getString("overlayDisplayMode"));
            session.setOverlayVrPosition(rs.getString("overlayVrPosition"));
            return session;
        }
    }


    class GameSession {
        private String encryptedLLMKey;
        private String encryptedTTSKey;

        private String kokoroVoice;
        private String googleVoice;
        private Double rmsThresholdHigh = 460.00;
        private Double rmsThresholdLow = 100.00;

        private Float speechSpeed;
        private int googleWaveNetPitch;
        private Float notificationVolume;
        private boolean useLocalCommandLlm;
        private boolean useLocalQueryLlm;
        /**
         * Legacy local/cloud flag. {@link #ttsProvider} is the engine selection this build reads - this is
         * kept in step with it purely so an older jar can still be rolled back onto the same database.
         */
        private boolean useLocalTTS;
        private String ttsProvider;
        private Integer sttThreads;
        private Integer voiceVolume;
        private String lmStudioAddress;
        private String lmStudioCommandModel;
        private String aiLanguage;
        private String audioInputDevice;
        private String audioOutputDevice;
        private boolean pushToTalkEnabled;
        private String pushToTalkControllerName;
        private int pushToTalkButtonIndex = -1;
        private boolean noiseReductionEnabled = false;
        private int noiseReductionStrength = 1;
        /**
         * HUD overlay layout. Scale 0 = derive from screen height; x/y -1 = leave where it opens.
         */
        private double overlayAlpha = 0.25;
        private double overlayFontScale = 0;
        private int overlayWidth = 760;
        private int overlayX = -1;
        private int overlayY = -1;
        private String overlayDisplayMode = "DESKTOP";
        private String overlayVrPosition = "BOTTOM";


        /**
         * App-global Kokoro (local TTS) voice, independent of {@link #googleVoice}.
         */
        public String getKokoroVoice() {
            return kokoroVoice;
        }

        public void setKokoroVoice(String kokoroVoice) {
            this.kokoroVoice = kokoroVoice;
        }

        /**
         * App-global Google (cloud TTS) voice, independent of {@link #kokoroVoice}.
         */
        public String getGoogleVoice() {
            return googleVoice;
        }

        public void setGoogleVoice(String googleVoice) {
            this.googleVoice = googleVoice;
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

        public Float getSpeechSpeed() {
            return speechSpeed;
        }

        public void setSpeechSpeed(Float speechSpeed) {
            this.speechSpeed = speechSpeed;
        }

        public int getGoogleWaveNetPitch() {
            return googleWaveNetPitch;
        }

        public void setGoogleWaveNetPitch(int googleWaveNetPitch) {
            this.googleWaveNetPitch = googleWaveNetPitch;
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

        public String getTtsProvider() {
            return ttsProvider;
        }

        public void setTtsProvider(String ttsProvider) {
            this.ttsProvider = ttsProvider;
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

        public boolean isNoiseReductionEnabled() {
            return noiseReductionEnabled;
        }

        public void setNoiseReductionEnabled(boolean noiseReductionEnabled) {
            this.noiseReductionEnabled = noiseReductionEnabled;
        }

        public int getNoiseReductionStrength() {
            return noiseReductionStrength;
        }

        public void setNoiseReductionStrength(int noiseReductionStrength) {
            this.noiseReductionStrength = noiseReductionStrength;
        }

        public double getOverlayAlpha() {
            return overlayAlpha;
        }

        public void setOverlayAlpha(double overlayAlpha) {
            this.overlayAlpha = overlayAlpha;
        }

        /**
         * {@code 0} means the commander has not chosen one, so it is derived from screen height.
         */
        public double getOverlayFontScale() {
            return overlayFontScale;
        }

        public void setOverlayFontScale(double overlayFontScale) {
            this.overlayFontScale = overlayFontScale;
        }

        public int getOverlayWidth() {
            return overlayWidth;
        }

        public void setOverlayWidth(int overlayWidth) {
            this.overlayWidth = overlayWidth;
        }

        /**
         * {@code -1} means no stored position, so the overlay opens wherever it defaults to.
         */
        public int getOverlayX() {
            return overlayX;
        }

        public void setOverlayX(int overlayX) {
            this.overlayX = overlayX;
        }

        public int getOverlayY() {
            return overlayY;
        }

        /**
         * Where the HUD is drawn: DESKTOP, VR or BOTH. Text, and read back
         * leniently, so a row written by a newer build cannot break an older one.
         */
        public String getOverlayDisplayMode() {
            return overlayDisplayMode;
        }

        public void setOverlayDisplayMode(String overlayDisplayMode) {
            this.overlayDisplayMode = overlayDisplayMode;
        }

        /**
         * Where the HUD hangs in the headset: TOP, TOP_RIGHT, RIGHT and so on. Text, and read back
         * leniently, so a row written by a newer build cannot break an older one.
         */
        public String getOverlayVrPosition() {
            return overlayVrPosition;
        }

        public void setOverlayVrPosition(String overlayVrPosition) {
            this.overlayVrPosition = overlayVrPosition;
        }

        public void setOverlayY(int overlayY) {
            this.overlayY = overlayY;
        }
    }
}
