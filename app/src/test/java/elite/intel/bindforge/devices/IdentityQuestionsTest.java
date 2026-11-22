package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devices.IdentityQuestions.ById;
import elite.intel.bindforge.devices.IdentityQuestions.Known;
import elite.intel.bindforge.devices.IdentityQuestions.Question;
import elite.intel.bindforge.devices.IdentityQuestions.Reading;
import elite.intel.db.dao.BindForgeIdentityAnswerDao.AnswerRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private static final Known LVWAP = new Known("LVWAP", LVWAP_WAS);
    private static final Known RVWAP = new Known("RVWAP", RVWAP_WAS);

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

    /** A controller the edits already name - named for APPLY, or the target of a retarget - is spoken for. */
    @Test
    void hardwareTheEditsHoldIsNoCandidate() {
        Reading reading = IdentityQuestions.read(List.of(LVWAP), List.of(ALPHA), List.of(ALPHA.hardware()), BOUND,
                name -> false, List.of());

        assertTrue(reading.toAsk().isEmpty());
    }

    @Test
    void frontiersNamesAreNeverAsked() {
        Known rudder = new Known("T-Rudder", new HardwareId("044F", "B679"));

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
        assertEquals("3344:03F5,3344:83F4", IdentityQuestions.hardwareSetKey(List.of(
                new HardwareId("3344", "83f4"), new HardwareId("3344", "03F5"), new HardwareId("3344", "83F4"))));
    }

    private static Reading read(List<Known> known, List<MyDevice> devices, Map<String, Integer> counts,
                                List<AnswerRow> answers) {
        return IdentityQuestions.read(known, devices, List.of(), counts, name -> false, answers);
    }

    // ------------------------------------------------------------------ known only by hardware id (A17)

    /** .binds names the old stick by its id only: 131 bindings on 334483F3, which nothing attached has. */
    private static final Map<String, Integer> BY_ID = Map.of("334483F3", 131);

    /** A controller the bindings know only by its id is asked about, under the id (Alan, 2026-10-17, A17). */
    @Test
    void aMissingControllerKnownOnlyByIdIsAsked() {
        Reading reading = readById(List.of(), List.of(ALPHA), BY_ID, List.of());

        Question question = reading.toAsk().getFirst();
        assertFalse(question.named());
        assertEquals(LVWAP_WAS, question.was());
        assertEquals("3344:83F3", question.answerKey(), "a colon, so no name BindForge accepts meets it");
        assertEquals("334483F3", question.idAsWritten(), "as .binds writes it");
        assertEquals(Set.of("334483F3"), question.foundAs());
        assertEquals(131, question.bindings());
        assertEquals(List.of(ALPHA), question.candidates());
        assertEquals(Set.of(ALPHA.hardware()), reading.withheld());
    }

    @Test
    void oneIdWrittenInBothOrdersIsOneQuestion() {
        Reading reading = readById(List.of(), List.of(ALPHA), Map.of("334483F3", 100, "83f33344", 31),
                List.of());

        assertEquals(1, reading.toAsk().size());
        assertEquals(131, reading.toAsk().getFirst().bindings());
        assertEquals(Set.of("334483F3", "83f33344"), reading.toAsk().getFirst().foundAs());
        assertEquals("334483F3", reading.toAsk().getFirst().idAsWritten(), "one id, in the game's order");
    }

    /** An attached controller with the id is onboarding's ordinary hex case, not a missing one. */
    @Test
    void anIdAnAttachedControllerHasIsNotMissing() {
        MyDevice old = new MyDevice("VIRPIL", null, "3344", "83F3", true, null, false);

        Reading reading = readById(List.of(), List.of(old, ALPHA), BY_ID, List.of());

        assertTrue(reading.toAsk().stream().allMatch(Question::named));
    }

    /** A device of the working copy on the id is APPLY's to move by its own hardware. */
    @Test
    void anIdADeviceOfTheUsersHasIsNotAsked() {
        assertTrue(readById(List.of(new HardwareId("3344", "83F3")), List.of(ALPHA), BY_ID, List.of())
                .toAsk().isEmpty());
    }

    @Test
    void anIdFrontierCoversIsNotAsked() {
        Reading reading = IdentityQuestions.read(List.of(), List.of(ALPHA), List.of(), Map.of(), name -> false,
                List.of(), new ById(Map.of("044FB679", 4), List.of(), new HardwareId("044F", "B679")::equals,
                        List.of()));

        assertTrue(reading.toAsk().isEmpty());
    }

    /**
     * A device may be named {@code DEADBEEF}, eight hex characters, which .binds is read as giving an id. It is no
     * missing controller - a yes would move a working controller's bindings away (review, 2026-10-17).
     */
    @Test
    void aDeviceNamedLikeAnIdIsNotAsked() {
        Map<String, Integer> named = Map.of("DEADBEEF", 40);
        Known deadbeef = new Known("DEADBEEF", new HardwareId("3344", "0194"));
        MyDevice entry = new MyDevice(null, "deadbeef", "1234", "0001", false, null, false);

        Reading inUse = IdentityQuestions.read(List.of(), List.of(ALPHA), List.of(), Map.of(), name -> false,
                List.of(), new ById(named, List.of(), hardware -> false, List.of("DEADBEEF")));
        Reading known = IdentityQuestions.read(List.of(deadbeef), List.of(ALPHA), List.of(), Map.of(),
                name -> false, List.of(), new ById(named, List.of(), hardware -> false, List.of()));
        Reading inAnInstallation = readById(List.of(), List.of(entry, ALPHA), named, List.of());

        assertTrue(inUse.toAsk().isEmpty(), "a name in the edits or the working copy");
        assertTrue(known.toAsk().isEmpty(), "a name the edits give a known device");
        assertTrue(inAnInstallation.toAsk().isEmpty(), "an installation's entry name, ignoring case");
    }

    /** The question and the name page after it show the old id the same way, as .binds writes it. */
    @Test
    void anOldIdIsShownAsTheBindsWriteIt() {
        HardwareId old = new HardwareId("3344", "83F3");

        assertEquals("334483f3", IdentityQuestions.asWritten(old, List.of("334483f3", "334483F4")));
        assertEquals("83F33344", IdentityQuestions.asWritten(old, List.of("83F33344")), "reversed, if that is all");
        assertEquals("334483F3", IdentityQuestions.asWritten(old, List.of()), "the game's order when none is found");
    }

    @Test
    void notNowIsStoredUnderTheIdAndDefersIt() {
        Reading reading = readById(List.of(), List.of(ALPHA), BY_ID,
                List.of(new AnswerRow("3344:83F3", "3344:83F4", "NOT_NOW")));

        assertTrue(reading.toAsk().isEmpty());
        assertEquals(List.of("3344:83F3"), reading.deferred().stream().map(Question::answerKey).toList());
        assertTrue(reading.forget().isEmpty(), "the id is still missing, so the answer holds");
    }

    @Test
    void anAnswerUnderAnIdIsForgottenOnceItsControllerIsAttachedAgain() {
        MyDevice old = new MyDevice("VIRPIL", null, "3344", "83F3", true, null, false);

        Reading reading = readById(List.of(), List.of(old, ALPHA), BY_ID,
                List.of(new AnswerRow("3344:83F3", "3344:83F4", "NOT_NOW")));

        assertEquals(Set.of("3344:83F3"), reading.forget());
    }

    @Test
    void aQuestionNamesTheControllerOrGivesItsIdsNeverBoth() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question("LVWAP", LVWAP_WAS, 1, List.of(), Set.of("334483F3")));
        assertThrows(IllegalArgumentException.class, () -> new Question(null, LVWAP_WAS, 1, List.of(), Set.of()));
    }

    private static Reading readById(List<HardwareId> heldByWorkingCopy, List<MyDevice> devices,
                                    Map<String, Integer> hardwareIds, List<AnswerRow> answers) {
        return IdentityQuestions.read(List.of(), devices, List.of(), Map.of(), name -> false, answers,
                new ById(hardwareIds, heldByWorkingCopy, hardware -> false, List.of()));
    }
}
