package elite.intel.ai.mouth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeechEmotesTest {

    @Test
    void writesEachEmoteInTheCountTheEngineVoices() {
        assertEquals("ok <sigh> keep scanning.", SpeechEmotes.voice("ok <sigh> keep scanning."));
        assertEquals("<breath> <breath> finally made it.", SpeechEmotes.voice("<breath> finally made it."));
        assertEquals("Target down. <laugh> <laugh> <laugh>", SpeechEmotes.voice("Target down. <laugh>"));
    }

    @Test
    void collapsesARunToTheRightCountWhateverTheModelWrote() {
        assertEquals("<sigh> again.", SpeechEmotes.voice("<sigh> <sigh> <sigh> again."));
        assertEquals("Done. <laugh> <laugh> <laugh>", SpeechEmotes.voice("Done. <laugh><laugh>"));
        assertEquals("<breath> <breath> go.", SpeechEmotes.voice("<BREATH> <breath> <breath> <breath> go."));
    }

    @Test
    void keepsOnlyTheFirstEmoteOfAReply() {
        assertEquals("<sigh> long trek through the void. here we go again.",
                SpeechEmotes.voice("<sigh> long trek through the void. <breath> <breath> here we go again."));
    }

    @Test
    void aMixedRunGivesOnlyItsFirstKnownEmote() {
        assertEquals("Ha. <laugh> <laugh> <laugh>", SpeechEmotes.voice("Ha. <giggle> <laugh> <sigh>"));
    }

    @Test
    void dropsTagsTheEngineDoesNotKnow() {
        assertEquals("Scan complete.", SpeechEmotes.voice("Scan complete. <giggle>"));
        assertEquals("Scan complete.", SpeechEmotes.voice("Scan <cough> complete."));
        assertEquals("Fine. <sigh> whatever.", SpeechEmotes.voice("Fine. <sigh> whatever. </sigh>"));
    }

    @Test
    void stripRemovesEveryTagAndTheGapItLeaves() {
        assertEquals("ok keep scanning!", SpeechEmotes.strip("ok <sigh> keep scanning! <laugh> <laugh> <laugh>"));
        assertEquals("finally made it!", SpeechEmotes.strip("<breath> <breath> finally made it!"));
        assertEquals("Target down.", SpeechEmotes.strip("Target down <laugh>."));
    }

    @Test
    void textWithoutTagsIsLeftExactlyAsItWas() {
        String text = "Ratio 3 : 2 , as  written.";
        assertSame(text, SpeechEmotes.strip(text));
        assertSame(text, SpeechEmotes.voice(text));
    }

    @Test
    void readsATagWhoseBracketsTheModelEscaped() {
        assertEquals("Looks like Eurus couldn't handle a Fer de Lance, Your Grace. <laugh> <laugh> <laugh>",
                SpeechEmotes.voice("Looks like Eurus couldn't handle a Fer de Lance, Your Grace.&lt;laugh&gt;"));
        assertEquals("Your Grace.", SpeechEmotes.strip("Your Grace.&LT;laugh&GT;"));
    }

    @Test
    void aComparisonIsNotATag() {
        assertEquals("Hull < 50 and shields > 20.", SpeechEmotes.strip("Hull < 50 and shields > 20."));
    }

    @Test
    void recognisesAFragmentThatIsOnlyItsEmote() {
        assertTrue(SpeechEmotes.isEmoteOnly("<laugh> <laugh> <laugh>"));
        assertTrue(SpeechEmotes.isEmoteOnly("<sigh>."));
        assertFalse(SpeechEmotes.isEmoteOnly("<sigh> here we go."));
        assertFalse(SpeechEmotes.isEmoteOnly("Plain sentence."));
    }

    @Test
    void nullAndEmptyAreEmpty() {
        assertEquals("", SpeechEmotes.voice(null));
        assertEquals("", SpeechEmotes.strip(""));
        assertFalse(SpeechEmotes.isEmoteOnly(null));
    }
}
