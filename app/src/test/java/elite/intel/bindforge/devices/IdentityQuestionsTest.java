package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devices.IdentityQuestions.Known;
import elite.intel.bindforge.devices.IdentityQuestions.Question;
import elite.intel.bindforge.devices.IdentityQuestions.Reading;
import elite.intel.db.dao.BindForgeIdentityAnswerDao.AnswerRow;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The identity question: a named controller bindings use is missing and an unnamed one is attached (spec,
 * alias-designer.md, <em>What Changes When Hardware Changes</em>; Alan, 2026-10-15, A12).
 */
class IdentityQuestionsTest {

    private static final HardwareId LVWAP_WAS = new HardwareId("3344", "83F3");
    private static final HardwareId RVWAP_WAS = new HardwareId("3344", "43F4");

    private static final MyDevice ALPHA =
            new MyDevice("VIRPIL Constellation ALPHA", null, "3344", "83F4", true, null, false);
    private static final MyDevice MONGOOSE =
            new MyDevice("VIRPIL MongoosT-50CM3", null, "3344", "03F5", true, null, false);
    private static final MyDevice LVWAP_ROW = new MyDevice(null, "LVWAP", "3344", "83F3", false, "LVWAP", false);

    private static final Known LVWAP = new Known("LVWAP", List.of("LVWAP"), LVWAP_WAS);
    private static final Known RVWAP = new Known("RVWAP", List.of("RVWAP"), RVWAP_WAS);

    private static final Map<String, Integer> BOUND = Map.of("LVWAP", 131, "RVWAP", 12);

    @Test
    void aMissingBoundControllerBesideAnUnnamedOneIsAsked() {
        Reading reading = read(List.of(LVWAP), List.of(LVWAP_ROW, ALPHA), BOUND, List.of());

        assertEquals(1, reading.toAsk().size());
        Question question = reading.toAsk().getFirst();
        assertEquals("LVWAP", question.deviceName());
        assertEquals(LVWAP_WAS, question.was());
        assertEquals(131, question.bindings());
        assertEquals(List.of(ALPHA), question.candidates());
        assertEquals(Set.of(ALPHA.hardware()), reading.withheld(), "onboarding leaves the candidate alone meanwhile");
    }

    /** One question per missing controller, each listing every candidate - never paired by order. */
    @Test
    void twoMissingAreTwoQuestionsEachListingEveryCandidate() {
        Reading reading = read(List.of(LVWAP, RVWAP), List.of(ALPHA, MONGOOSE), BOUND, List.of());

        assertEquals(List.of("LVWAP", "RVWAP"), reading.toAsk().stream().map(Question::deviceName).toList());
        reading.toAsk().forEach(question -> assertEquals(List.of(ALPHA, MONGOOSE), question.candidates()));
    }

    @Test
    void aCandidatePickedForOneQuestionDropsOutOfTheNext() {
        Question rvwap = read(List.of(RVWAP), List.of(ALPHA, MONGOOSE), BOUND, List.of()).toAsk().getFirst();

        assertEquals(List.of(MONGOOSE), rvwap.without(ALPHA.hardware()).candidates());
    }

    /** Only when bindings use it (Alan) - an entry for a stick sold years ago is not worth a question. */
    @Test
    void aMissingControllerNoBindingUsesIsNotAsked() {
        Reading reading = read(List.of(LVWAP), List.of(ALPHA), Map.of("RVWAP", 12), List.of());

        assertTrue(reading.toAsk().isEmpty());
        assertTrue(reading.withheld().isEmpty(), "its candidate is onboarding's to name");
    }

    /** Bindings may still name it by the old name a rename is moving away from. */
    @Test
    void bindingsOnAnOldNameCount() {
        Known renamed = new Known("LeftStick", Arrays.asList("LeftStick", "LeftStick", "LVWAP", null), LVWAP_WAS);

        Reading reading = read(List.of(renamed), List.of(ALPHA), Map.of("lvwap", 131), List.of());

        assertEquals(131, reading.toAsk().getFirst().bindings());
    }

    @Test
    void anAttachedControllerIsNotMissing() {
        MyDevice lvwapAttached = new MyDevice("VIRPIL", "LVWAP", "3344", "83F3", true, "LVWAP", false);

        assertTrue(read(List.of(LVWAP), List.of(lvwapAttached, ALPHA), BOUND, List.of()).toAsk().isEmpty());
    }

    @Test
    void nothingUnnamedAttachedMeansNoQuestion() {
        MyDevice named = new MyDevice("VIRPIL", "RVWAP", "3344", "43F4", true, "RVWAP", false);
        MyDevice builtIn = new MyDevice("T-Rudder", null, "044F", "B679", true, null, true);

        assertTrue(read(List.of(LVWAP), List.of(named, builtIn), BOUND, List.of()).toAsk().isEmpty(),
                "a named controller and a built-in are recognised, so neither is a candidate");
    }

    /** A controller the draft already names - named for Apply, or the target of a retarget - is spoken for. */
    @Test
    void hardwareTheDraftHoldsIsNoCandidate() {
        Reading reading = IdentityQuestions.read(List.of(LVWAP), List.of(ALPHA), List.of(ALPHA.hardware()), BOUND,
                name -> false, List.of());

        assertTrue(reading.toAsk().isEmpty());
    }

    @Test
    void frontiersNamesAreNeverAsked() {
        Known rudder = new Known("T-Rudder", List.of("T-Rudder"), new HardwareId("044F", "B679"));

        Reading reading = IdentityQuestions.read(List.of(rudder), List.of(ALPHA), List.of(),
                Map.of("T-Rudder", 4), "T-Rudder"::equals, List.of());

        assertTrue(reading.toAsk().isEmpty());
    }

    @Test
    void notNowForTheSameCandidatesDefersTheQuestion() {
        Reading reading = read(List.of(LVWAP), List.of(ALPHA), BOUND,
                List.of(new AnswerRow("lvwap", "3344:83F4", "NOT_NOW")));

        assertTrue(reading.toAsk().isEmpty());
        assertEquals(List.of("LVWAP"), reading.deferred().stream().map(Question::deviceName).toList());
        assertEquals(Set.of(ALPHA.hardware()), reading.withheld(), "deferred, so onboarding still leaves it alone");
        assertEquals(1, reading.askingDeferred().size(), "CHECK AGAIN asks it again");
        assertTrue(reading.forget().isEmpty());
    }

    @Test
    void noneOfTheseForTheSameCandidatesIsNeitherAskedNorWithheld() {
        Reading reading = read(List.of(LVWAP), List.of(ALPHA), BOUND,
                List.of(new AnswerRow("LVWAP", "3344:83F4", "NONE_OF_THESE")));

        assertTrue(reading.askingDeferred().isEmpty());
        assertTrue(reading.withheld().isEmpty(), "a different controller - onboarding may name it");
    }

    /** "Until the hardware changes": another candidate plugged in gives another set, and the question comes back. */
    @Test
    void anAnswerStopsHoldingWhenTheCandidatesChange() {
        Reading reading = read(List.of(LVWAP), List.of(ALPHA, MONGOOSE), BOUND,
                List.of(new AnswerRow("LVWAP", "3344:83F4", "NONE_OF_THESE")));

        assertEquals(1, reading.toAsk().size());
    }

    @Test
    void anAnswerIsForgottenOnceItsControllerIsAttachedAgain() {
        MyDevice lvwapAttached = new MyDevice("VIRPIL", "LVWAP", "3344", "83F3", true, "LVWAP", false);

        Reading reading = read(List.of(LVWAP, RVWAP), List.of(lvwapAttached, ALPHA), BOUND, List.of(
                new AnswerRow("LVWAP", "3344:83F4", "NOT_NOW"),
                new AnswerRow("RVWAP", "3344:83F4", "NOT_NOW"),
                new AnswerRow("SoldStick", "3344:83F4", "NONE_OF_THESE")));

        assertEquals(Set.of("LVWAP", "SoldStick"), reading.forget(),
                "RVWAP is still missing, so its answer still holds");
    }

    @Test
    void theSameVendorIsEvidence() {
        MyDevice other = new MyDevice("Thrustmaster", null, "044F", "0402", true, null, false);
        Question question = read(List.of(LVWAP), List.of(ALPHA, other), BOUND, List.of()).toAsk().getFirst();

        assertTrue(question.sameVendor(ALPHA));
        assertFalse(question.sameVendor(other));
    }

    @Test
    void theKeyIsTheSameHoweverTheSetWasGathered() {
        assertEquals("3344:03F5,3344:83F4", IdentityQuestions.key(List.of(
                new HardwareId("3344", "83f4"), new HardwareId("3344", "03F5"), new HardwareId("3344", "83F4"))));
    }

    private static Reading read(List<Known> known, List<MyDevice> devices, Map<String, Integer> counts,
                                List<AnswerRow> answers) {
        return IdentityQuestions.read(known, devices, List.of(), counts, name -> false, answers);
    }
}
