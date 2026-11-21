package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Installation;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Kind;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Row;
import elite.intel.bindforge.storage.FirstSetupReading;
import elite.intel.bindforge.storage.InstallationFiles;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The reconcile wizard's content: what is asked, in what order, and what the result holds and why. */
class ReconcileQuestionsTest {

    private static final long STEAM = 1;
    private static final long EPIC = 2;
    private static final HardwareId RIGHT = new HardwareId("3344", "03F5");
    private static final HardwareId LEFT = new HardwareId("3344", "83F4");
    private static final HardwareId YOKE = new HardwareId("046D", "C259");
    private static final HardwareId X56 = new HardwareId("0738", "2221");
    private static final HardwareId PEDALS = new HardwareId("044F", "B679");

    /** The screenshot's machine, cut down: one rename, one name on two controllers, labels, and the extras. */
    private static ReconcileQuestions alansMachine() {
        Map<String, HardwareId> steam = new LinkedHashMap<>();
        steam.put("RVWAP", RIGHT);
        steam.put("Yoke", YOKE);
        steam.put("Pedals", PEDALS);
        steam.put("Throttle", LEFT);
        Map<String, HardwareId> epic = new LinkedHashMap<>();
        epic.put("RightStick", RIGHT);
        epic.put("Yoke", X56);
        epic.put("Throttle", LEFT);
        return questions(Map.of("main.4.2.binds", Map.of("RVWAP", 131, "RightStick", 12, "Yoke", 48, "Pedals", 20)),
                new Installation(STEAM, "STEAM", steam, Map.of(),
                        Map.of("Throttle", Map.of("Joy_1", "Boost", "Joy_2", "Gear"))),
                new Installation(EPIC, "EPIC", epic, Map.of(),
                        Map.of("Throttle", Map.of("Joy_1", "Afterburner", "Joy_3", "Scoop"))));
    }

    @Test
    void namesAreAskedFirstThenControllersThenButtonNames() {
        List<Kind> order = alansMachine().questions().stream().map(Row::kind).toList();
        assertEquals(List.of(Kind.NAME_DIFFERS, Kind.HARDWARE_DIFFERS, Kind.LABELS_DIFFER), order);
    }

    @Test
    void withinOneKindTheQuestionMoreControlsDependOnComesFirst() {
        ReconcileQuestions questions = questions(Map.of("a.4.2.binds", Map.of("Big", 50, "Small", 2)),
                new Installation(STEAM, "STEAM", Map.of("Small", LEFT, "Big", RIGHT), Map.of(), Map.of()),
                new Installation(EPIC, "EPIC", Map.of("Small", YOKE, "Big", X56), Map.of(), Map.of()));

        assertEquals(List.of("Big", "Small"), questions.questions().stream().map(Row::deviceName).toList());
    }

    @Test
    void countsAreThePlayersQuestionsWhatSettlesAloneAndWhatAlreadyMatches() {
        ReconcileQuestions questions = alansMachine();

        assertEquals(3, questions.questions().size());
        assertEquals(1, questions.settledAlone(), "the pedals, only in Steam");
        assertEquals(0, questions.alreadyMatching(), "the throttle is the same entry, but its labels are a question");
        assertEquals(143, questions.controlsOf(questions.questions().getFirst()), "131 on RVWAP + 12 on RightStick");
    }

    @Test
    void anAnswerIsNeededOnlyWhileTheQuestionStillMatters() {
        Map<String, HardwareId> steam = Map.of("Stick", RIGHT);
        Map<String, HardwareId> epic = new LinkedHashMap<>();
        epic.put("Other", RIGHT);
        epic.put("Stick", LEFT);
        ReconcileQuestions questions = questions(Map.of(),
                new Installation(STEAM, "STEAM", steam, Map.of(), Map.of()),
                new Installation(EPIC, "EPIC", epic, Map.of(), Map.of()));
        Row name = byKind(questions, Kind.NAME_DIFFERS);
        Row hardware = byKind(questions, Kind.HARDWARE_DIFFERS);

        assertTrue(questions.needed(hardware, Map.of()));
        Map<String, String> answers = Map.of(name.id(), "Other");
        assertFalse(questions.needed(hardware, answers), "Stick lost its name, so which controller it means is moot");
    }

    @Test
    void buttonNamesAreAnsweredInputByInput() {
        ReconcileQuestions questions = alansMachine();
        Row labels = byKind(questions, Kind.LABELS_DIFFER);
        Map<String, String> answers = new LinkedHashMap<>();

        assertFalse(questions.isAnswered(labels, answers));
        assertEquals(0, questions.buttonsAnswered(labels, answers));
        answers.putAll(questions.plan().labelAnswersFavouring(labels.deviceName(), EPIC));
        assertTrue(questions.isAnswered(labels, answers));
        assertEquals(1, questions.buttonsAnswered(labels, answers));
    }

    @Test
    void theControllersListGroupsEveryAliasOfOneControllerAndWhichCopiesKnowIt() {
        List<ReconcileQuestions.Controller> controllers = alansMachine().controllers();

        ReconcileQuestions.Controller right = controllers.stream().filter(c -> c.hardware().equals(RIGHT))
                .findFirst().orElseThrow();
        assertEquals(List.of("RVWAP", "RightStick"), right.aliases());
        assertEquals(Set.of(STEAM, EPIC), right.installs());
        assertEquals(143, right.controls());
        assertEquals(RIGHT, controllers.getFirst().hardware(), "the controller most controls use comes first");
        ReconcileQuestions.Controller pedals = controllers.stream().filter(c -> c.hardware().equals(PEDALS))
                .findFirst().orElseThrow();
        assertEquals(Set.of(STEAM), pedals.installs());
    }

    @Test
    void theResultSaysWhyEachDeviceIsThere() {
        ReconcileQuestions questions = alansMachine();
        Map<String, String> answers = answersFavouring(questions, STEAM);
        FirstSetupPlan.Resolution resolution = questions.plan().resolve(answers);

        Map<String, ReconcileQuestions.Outcome> outcomes = new LinkedHashMap<>();
        questions.outcomes(resolution, answers).forEach(outcome -> outcomes.put(outcome.kept().name(), outcome));
        assertEquals(Set.of("RVWAP", "Yoke", "Pedals", "Throttle"), outcomes.keySet());
        assertEquals(ReconcileQuestions.Reason.CHOSEN, outcomes.get("RVWAP").reason());
        assertEquals(12, outcomes.get("RVWAP").movedHere(), "RightStick's bindings move to the name kept");
        assertEquals(ReconcileQuestions.Reason.ADDED, outcomes.get("Pedals").reason());
        assertEquals(Set.of(STEAM), outcomes.get("Pedals").onlyIn());
        assertEquals(ReconcileQuestions.Reason.CHOSEN, outcomes.get("Throttle").reason(), "its button names were asked");
        assertTrue(outcomes.get("Throttle").labelsChosen());
    }

    @Test
    void aControllerThatLostASharedNameIsTidiedAway() {
        ReconcileQuestions questions = alansMachine();
        Map<String, String> answers = answersFavouring(questions, STEAM);
        FirstSetupPlan.Resolution resolution = questions.plan().resolve(answers);

        List<ReconcileQuestions.Tidied> tidied = questions.tidied(resolution, answers);
        assertEquals(1, tidied.size(), tidied.toString());
        assertEquals("Yoke", tidied.getFirst().name());
        assertEquals(X56, tidied.getFirst().hardware(), "Epic's X56 no longer answers to Yoke");
    }

    @Test
    void aDeadEntryIsTidiedAwayWithWhereItsBindingsGo() {
        Map<String, HardwareId> steam = new LinkedHashMap<>();
        steam.put("RightStick", RIGHT);
        steam.put("OldStick", RIGHT);
        ReconcileQuestions questions = questions(Map.of("a.4.2.binds", Map.of("OldStick", 4)),
                new Installation(STEAM, "STEAM", steam, Map.of(), Map.of()),
                new Installation(EPIC, "EPIC", Map.of("RightStick", RIGHT), Map.of(), Map.of()));
        FirstSetupPlan.Resolution resolution = questions.plan().resolve(Map.of());

        assertEquals(List.of(new ReconcileQuestions.Tidied("OldStick", "RightStick", true, null)),
                questions.tidied(resolution, Map.of()));
        assertEquals(4, questions.outcomes(resolution, Map.of()).getFirst().movedHere());
    }

    /** "I lost all my binds": a controller the bindings use, unplugged at launch, sends the game to keyboard and mouse. */
    @Test
    void theControllersToPlugInAreTheOnesTheBindingsUse() {
        ReconcileQuestions questions = alansMachine();
        Map<String, String> answers = answersFavouring(questions, STEAM);
        FirstSetupPlan.Resolution resolution = questions.plan().resolve(answers);

        List<String> unplugged = questions.unpluggedInUse(resolution, Set.of(RIGHT, YOKE)).stream()
                .map(FirstSetupPlan.Kept::name).toList();
        assertEquals(List.of("Pedals"), unplugged, "the throttle is unplugged too, but no binding uses it");
    }

    @Test
    void theShortcutOffersOnlyCopiesWithAListOfControllers() {
        ReconcileQuestions questions = new ReconcileQuestions(new FirstSetupReading(Path.of("bindings"), Map.of(),
                List.of(files(STEAM, true), files(EPIC, false)), List.of(),
                new FirstSetupPlan(List.of(), Set.of()), Map.of()));

        assertEquals(List.of(STEAM), questions.shortcutSides());
    }

    private static Map<String, String> answersFavouring(ReconcileQuestions questions, long side) {
        Map<String, String> answers = new LinkedHashMap<>(questions.plan().answersFavouring(side));
        assertEquals(List.of(), questions.plan().unanswered(answers));
        return answers;
    }

    private static Row byKind(ReconcileQuestions questions, Kind kind) {
        return questions.questions().stream().filter(row -> row.kind() == kind).findFirst().orElseThrow();
    }

    private static ReconcileQuestions questions(Map<String, Map<String, Integer>> references,
                                                Installation... installations) {
        Set<String> referenced = new java.util.TreeSet<>();
        references.values().forEach(counts -> referenced.addAll(counts.keySet()));
        FirstSetupPlan plan = new FirstSetupPlan(List.of(installations), referenced);
        List<InstallationFiles> files = java.util.Arrays.stream(installations)
                .map(install -> files(install.installId(), true)).toList();
        return new ReconcileQuestions(new FirstSetupReading(Path.of("bindings"), Map.of(), files, List.of(), plan,
                references));
    }

    private static InstallationFiles files(long id, boolean deviceMappings) {
        return new InstallationFiles(id, id == STEAM ? "STEAM" : "EPIC", Path.of("game-" + id),
                deviceMappings ? new byte[]{'<'} : null, Map.of());
    }
}
