package elite.intel.ai.mouth.edge;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

/** Reads only the application settings Edge synthesis needs and provides an injectable test seam. */
interface EdgeTtsSettings {
    String selectedVoiceName();

    Language language();

    float speechSpeed();

    int voiceVolume();

    static EdgeTtsSettings system() {
        SystemSession session = SystemSession.getInstance();
        return new EdgeTtsSettings() {
            @Override
            public String selectedVoiceName() {
                return session.getEdgeVoiceName();
            }

            @Override
            public Language language() {
                return session.getLanguage();
            }

            @Override
            public float speechSpeed() {
                Float speed = session.getSpeechSpeed();
                return speed == null ? 0f : speed;
            }

            @Override
            public int voiceVolume() {
                return session.getVoiceVolume();
            }
        };
    }
}
