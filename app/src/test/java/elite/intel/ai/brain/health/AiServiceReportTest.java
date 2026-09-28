package elite.intel.ai.brain.health;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiServiceReportTest {

    @BeforeEach
    void english() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @AfterEach
    void restoreLanguage() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void aStandingRefusalSaysHowLongAndOffersTheKeyOnlyAsSomethingToCheck() {
        String spoken = AiServiceReport.spoken(
                new AiServiceVerdict.Refused("Mistral", Optional.of(Duration.ofMinutes(150)), true));

        assertEquals("Mistral is answering, but it has refused every request for 2 hours. "
                + "If you changed your API key recently, check it on the AI Services settings tab.", spoken);
    }

    @Test
    void aFreshRefusalHasNoLength() {
        assertEquals("Mistral is answering, but it refused my test request.",
                AiServiceReport.spoken(new AiServiceVerdict.Refused("Mistral", Optional.empty(), false)));
    }

    @Test
    void aConnectionGivesItsReplyTimeInWholeSecondsNeverZero() {
        assertEquals("LM Studio is answering. Replies take about 1 second.", AiServiceReport.spoken(
                new AiServiceVerdict.Connected("LM Studio", Optional.of(Duration.ofMillis(300)), false)));
        assertEquals("LM Studio is answering. Replies take about 6 seconds.", AiServiceReport.spoken(
                new AiServiceVerdict.Connected("LM Studio", Optional.of(Duration.ofMillis(5_600)), false)));
    }

    @Test
    void anUnsupportedModelIsMentionedAfterTheConnection() {
        assertEquals("LM Studio is answering. It is running a model other than the recommended one, "
                        + "so some commands may go wrong.",
                AiServiceReport.spoken(new AiServiceVerdict.Connected("LM Studio", Optional.empty(), true)));
    }

    @Test
    void noAnswerSaysHowLongItWaited() {
        assertEquals("OpenAI did not answer within 10 seconds.",
                AiServiceReport.spoken(new AiServiceVerdict.NoAnswer("OpenAI", Duration.ofSeconds(10))));
    }

    @Test
    void secondsFollowTheLanguagePluralRules() {
        SystemSession.getInstance().setLanguage(Language.RU);

        assertEquals("LM Studio отвечает. Время ответа примерно 3 секунды.", AiServiceReport.spoken(
                new AiServiceVerdict.Connected("LM Studio", Optional.of(Duration.ofSeconds(3)), false)));
    }
}
