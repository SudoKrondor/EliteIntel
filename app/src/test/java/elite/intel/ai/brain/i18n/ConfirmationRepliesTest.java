package elite.intel.ai.brain.i18n;

import elite.intel.ai.brain.i18n.ConfirmationReplies.Reply;
import elite.intel.i18n.Language;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The commander's answer to "shall I proceed?" on a destructive action. Only a clear yes may run it; a no, a
 * hesitation or a new request must never be read as consent.
 */
class ConfirmationRepliesTest {

    @ParameterizedTest
    @ValueSource(strings = {"yes", "Yes.", "yes please", "yeah go ahead", "sure", "go ahead", "affirmative",
            "do it", "confirm", "okay do it", "that's right"})
    void aClearYesConfirms(String utterance) {
        assertEquals(Reply.AFFIRMATIVE, ConfirmationReplies.classify(Language.EN, utterance), utterance);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no", "No!", "negative", "belay that", "do not do that", "don't", "hold on",
            "wait a second", "never mind", "nope", "no yes"})
    void aNoCancels(String utterance) {
        assertEquals(Reply.NEGATIVE, ConfirmationReplies.classify(Language.EN, utterance), utterance);
    }

    /**
     * Not an answer: the commander moved on, and the pending action must not be taken as confirmed.
     */
    @ParameterizedTest
    @ValueSource(strings = {"plot route to the fleet carrier", "what's our fuel level", "",
            "yes and then plot a route to the nearest station please"})
    void anythingElseIsNotAnAnswer(String utterance) {
        assertEquals(Reply.OTHER, ConfirmationReplies.classify(Language.EN, utterance), utterance);
    }

    /**
     * Every authored yes must read as yes in its own language. A yes opened by one of the same language's no
     * phrases would silently cancel instead - the negative list is checked first.
     */
    @ParameterizedTest(name = "{0}: every authored answer reads as itself")
    @EnumSource(Language.class)
    void everyAuthoredAnswerReadsAsItself(Language language) {
        for (String phrase : authored(language, "affirmative")) {
            assertEquals(Reply.AFFIRMATIVE, ConfirmationReplies.classify(language, phrase), language + ": " + phrase);
        }
        for (String phrase : authored(language, "negative")) {
            assertEquals(Reply.NEGATIVE, ConfirmationReplies.classify(language, phrase), language + ": " + phrase);
        }
    }

    @ParameterizedTest(name = "{0}: English yes and no still work")
    @EnumSource(Language.class)
    void englishAnswersWorkInEveryLanguage(Language language) {
        assertEquals(Reply.AFFIRMATIVE, ConfirmationReplies.classify(language, "yes"));
        assertEquals(Reply.NEGATIVE, ConfirmationReplies.classify(language, "no"));
    }

    private static List<String> authored(Language language, String key) {
        Locale locale = AiActionAliasTextProvider.locale(language);
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.ai_confirmation_replies", locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
        return Arrays.stream(bundle.getString(key).split(",")).map(String::strip).filter(p -> !p.isEmpty()).toList();
    }
}
