package elite.intel.ai.ears.parakeet;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParakeetTrashPrefixTest {

    private final SystemSession session = SystemSession.getInstance();
    private Language previousLanguage;

    @BeforeEach
    void useEnglish() {
        previousLanguage = session.getLanguage();
        session.setLanguage(Language.EN);
    }

    @AfterEach
    void restoreLanguage() {
        session.setLanguage(previousLanguage);
    }

    @Test
    void dropsALoneAndHeardFromAClick() {
        assertEquals("", ParakeetSTTImpl.stripTrashPrefix("and"));
        assertEquals("", ParakeetSTTImpl.stripTrashPrefix("and."));
    }

    @Test
    void stripsALeadingAndFromARealOrder() {
        assertEquals("deploy hardpoints", ParakeetSTTImpl.stripTrashPrefix("and deploy hardpoints"));
    }

    @Test
    void keepsAnAndInsideAnOrder() {
        assertEquals("retract landing gear and boost", ParakeetSTTImpl.stripTrashPrefix("retract landing gear and boost"));
    }
}
