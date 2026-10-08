package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceDivergence.Severity;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Installation;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Kind;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Option;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Resolution;
import elite.intel.bindforge.devicefiles.FirstSetupPlan.Row;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * First setup reconciles the user's elements per element, shows everything, and asks only about the genuine
 * conflicts (alias-designer.md, <em>First setup</em>).
 */
class FirstSetupPlanTest {

    private static final long STEAM = 1;
    private static final long EPIC = 2;
    private static final HardwareId LEFT = new HardwareId("3344", "83F4");
    private static final HardwareId RIGHT = new HardwareId("3344", "03F5");
    private static final HardwareId OLD_LEFT = new HardwareId("3344", "83F3");

    @Test
    void anElementTheSameEverywhereIsGreenAndAsksNothing() {
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, Map.of("LVWAP", LEFT)),
                install(EPIC, Map.of("LVWAP", LEFT)));

        Row row = only(plan.rows());
        assertEquals(Kind.IDENTICAL, row.kind());
        assertEquals(Severity.GREEN, row.severity());
        assertFalse(row.isQuestion());
        assertEquals(List.of(device("LVWAP", LEFT, Map.of())), plan.resolve(Map.of()).devices());
    }

    /** The measured outage: Epic lacks the sticks Steam's .binds names, so the whole preset is rejected there. */
    @Test
    void anElementInOneInstallationIsTakenAndRedWhenTheBindsNameIt() {
        FirstSetupPlan plan = plan(Set.of("RVWAP"),
                install(STEAM, Map.of("RVWAP", RIGHT, "Spare", LEFT)),
                install(EPIC, Map.of()));

        Map<String, Row> rows = byDevice(plan.rows());
        assertEquals(Kind.ONLY_IN_SOME, rows.get("RVWAP").kind());
        assertEquals(Severity.RED, rows.get("RVWAP").severity());
        assertEquals(Severity.YELLOW, rows.get("Spare").severity(), "nothing references it, so nothing breaks");
        assertTrue(plan.unanswered(Map.of()).isEmpty(), "one sensible outcome, so no question");
        assertEquals(List.of("RVWAP", "Spare"), names(plan.resolve(Map.of())));
    }

    @Test
    void oneNameOnDifferentHardwareIsAQuestionAnsweredByTheHardware() {
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, Map.of("LVWAP", LEFT)),
                install(EPIC, Map.of("LVWAP", OLD_LEFT)));

        Row row = only(plan.rows());
        assertEquals(Kind.HARDWARE_DIFFERS, row.kind());
        assertEquals(Severity.RED, row.severity());
        assertEquals(List.of(row.id()), plan.unanswered(Map.of()));
        assertThrows(IllegalStateException.class, () -> plan.resolve(Map.of()));

        Option epics = optionHeldByEpic(row);
        Resolution resolution = plan.resolve(Map.of(row.id(), epics.key()));
        assertEquals(List.of(device("LVWAP", OLD_LEFT, Map.of())), resolution.devices());
    }

    @Test
    void oneHardwareUnderTwoNamesIsAQuestionAndTheLoserIsAPendingRemoval() {
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, Map.of("RVWAP", RIGHT)),
                install(EPIC, Map.of("RightStick", RIGHT)));

        Row row = only(plan.rows());
        assertEquals(Kind.NAME_DIFFERS, row.kind());
        assertEquals(Severity.YELLOW, row.severity(), "no binding names either, so nothing breaks yet");

        Resolution resolution = plan.resolve(Map.of(row.id(), "RVWAP"));
        assertEquals(List.of("RVWAP"), names(resolution));
        assertEquals(List.of("RightStick"), resolution.pendingRemovals(),
                "its entry stays on disk below the winner until something removes it");
    }

    /** The rename transaction is not built, so a choice that would orphan bindings cannot be made yet. */
    @Test
    void aNameTheBindsUseCannotBeGivenUp() {
        FirstSetupPlan plan = plan(Set.of("RVWAP"),
                install(STEAM, Map.of("RVWAP", RIGHT)),
                install(EPIC, Map.of("RightStick", RIGHT)));

        Row row = only(plan.rows());
        assertEquals(Severity.RED, row.severity());
        Map<String, Option> options = new HashMap<>();
        row.options().forEach(option -> options.put(option.key(), option));
        assertTrue(options.get("RVWAP").available());
        assertFalse(options.get("RightStick").available(), "choosing it would orphan every RVWAP binding");
        assertEquals(List.of(row.id()), plan.unanswered(Map.of(row.id(), "RightStick")));
    }

    @Test
    void whenBothNamesAreInTheBindsNeitherCanBeChosen() {
        FirstSetupPlan plan = plan(Set.of("RVWAP", "RightStick"),
                install(STEAM, Map.of("RVWAP", RIGHT)),
                install(EPIC, Map.of("RightStick", RIGHT)));

        Row row = only(plan.rows());
        assertTrue(row.options().stream().noneMatch(Option::available));
        assertEquals(Map.of(), plan.answersFavouring(STEAM));
    }

    @Test
    void aLoserNeedsNoAnswerToItsOwnHardwareQuestion() {
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, Map.of("LVWAP", LEFT)),
                install(EPIC, Map.of("LVWAP", OLD_LEFT, "LeftStick", LEFT)));

        Row names = plan.rows().stream().filter(row -> row.kind() == Kind.NAME_DIFFERS).findFirst().orElseThrow();
        Resolution resolution = plan.resolve(Map.of(names.id(), "LeftStick"));

        assertEquals(List.of("LeftStick"), names(resolution));
        assertEquals(List.of("LVWAP"), resolution.pendingRemovals());
    }

    @Test
    void labelsInOneFileOnlyAreTakenAndIdenticalOnesAskNothing() {
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, Map.of("LVWAP", LEFT), Map.of("LVWAP", Map.of("Joy_1", "TRIGGER", "Joy_2", "PINKY"))),
                install(EPIC, Map.of("LVWAP", LEFT), Map.of("LVWAP", Map.of("Joy_1", "TRIGGER", "Joy_4", "PADDLE"))));

        assertEquals(Kind.IDENTICAL, only(plan.rows()).kind(), "twenty labels and five give twenty-five");
        assertEquals(Map.of("Joy_1", "TRIGGER", "Joy_2", "PINKY", "Joy_4", "PADDLE"),
                plan.resolve(Map.of()).devices().getFirst().labels());
    }

    /** A2 brings the per-input merge view. Until then the whole device is answered by one installation. */
    @Test
    void collidingLabelsAreAnsweredByAnInstallationAndTheRestAreStillTaken() {
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, Map.of("LVWAP", LEFT), Map.of("LVWAP", Map.of("Joy_1", "LV MAIN TRIGGER"))),
                install(EPIC, Map.of("LVWAP", LEFT), Map.of("LVWAP", Map.of("Joy_1", "TRIGGER 1", "Joy_4", "PINKY"))));

        Row labels = plan.rows().getFirst();
        assertEquals(Kind.LABELS_DIFFER, labels.kind(), "a question sorts first");
        Resolution resolution = plan.resolve(Map.of(labels.id(), String.valueOf(STEAM)));

        assertEquals(Map.of("Joy_1", "LV MAIN TRIGGER", "Joy_4", "PINKY"),
                resolution.devices().getFirst().labels());
        assertEquals(1, resolution.buttonMapCount());
    }

    @Test
    void labelsOnABuiltInAreTakenUnderFrontiersName() {
        HardwareId rudder = new HardwareId("044F", "B679");
        FirstSetupPlan plan = plan(Set.of(),
                new Installation(STEAM, "STEAM", Map.of(), Map.of("T-Rudder", rudder),
                        Map.of("T-Rudder", Map.of("Joy_XAxis", "X Axis"))),
                install(EPIC, Map.of()));

        Row row = only(plan.rows());
        assertEquals(Kind.BUILT_IN_LABELS, row.kind());
        assertEquals(List.of(device("T-Rudder", rudder, Map.of("Joy_XAxis", "X Axis"))),
                plan.resolve(Map.of()).devices());
    }

    /** The blunt route: one click, and still never drops an element only another installation holds. */
    @Test
    void usingOneInstallationAsTheMasterAnswersInItsFavourAndKeepsTheOthersExtras() {
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, Map.of("LVWAP", LEFT, "RVWAP", RIGHT)),
                install(EPIC, Map.of("LVWAP", OLD_LEFT, "Pedals", new HardwareId("044F", "B679"))));

        Map<String, String> answers = plan.answersFavouring(STEAM);
        Resolution resolution = plan.resolve(answers);

        assertEquals(List.of(device("LVWAP", LEFT, Map.of()), device("Pedals", new HardwareId("044F", "B679"), Map.of()),
                device("RVWAP", RIGHT, Map.of())), resolution.devices());
    }

    @Test
    void questionsSortFirstThenRedThenYellowThenGreen() {
        FirstSetupPlan plan = plan(Set.of("Bound"),
                install(STEAM, Map.of("Alpha", LEFT, "Bound", RIGHT, "Spare", new HardwareId("1", "1"),
                        "Twin", new HardwareId("2", "2"))),
                install(EPIC, Map.of("Alpha", LEFT, "Twin", new HardwareId("2", "3"))));

        assertEquals(List.of("Twin", "Bound", "Spare", "Alpha"),
                plan.rows().stream().map(Row::deviceName).toList());
    }

    /**
     * The review's crossing layout: a stale PID under one name in each installation, each pair also held under a
     * second name. Favouring Steam must keep Steam's own device.
     */
    @Test
    void aNameThatWinsOneConflictIsKeptEvenWhereItLosesAnother() {
        HardwareId h1 = new HardwareId("3344", "0001");
        HardwareId h2 = new HardwareId("3344", "0002");
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, ordered("X", h1, "Z", h2)),
                install(EPIC, ordered("X", h2, "Y", h1)));

        Resolution resolution = plan.resolve(plan.answersFavouring(STEAM));

        assertEquals(List.of(device("X", h1, Map.of()), device("Z", h2, Map.of())), resolution.devices());
        assertEquals(List.of("Y"), resolution.pendingRemovals());
    }

    /** Answers that would leave two devices on one VID/PID are not accepted until changed. */
    @Test
    void answersPuttingTwoDevicesOnOneHardwareAreUnanswered() {
        HardwareId h1 = new HardwareId("3344", "0001");
        HardwareId h2 = new HardwareId("3344", "0002");
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, ordered("X", h1, "Z", h2)),
                install(EPIC, ordered("X", h2, "Y", h1)));
        Map<String, String> answers = new LinkedHashMap<>();
        answers.put("name:3344:0001", "Y");
        answers.put("name:3344:0002", "X");
        answers.put("hardware:X", "3344:0001");

        assertEquals(List.of("hardware:X"), plan.unanswered(answers), "X would sit on Y's hardware");

        answers.put("hardware:X", "3344:0002");
        assertEquals(List.of(device("X", h2, Map.of()), device("Y", h1, Map.of())), plan.resolve(answers).devices());
    }

    /** The game uses the first of two elements on one VID/PID, so the second is not the user's choice to make. */
    @Test
    void aSecondNameForOneHardwareInsideOneInstallationIsShadowedNotAsked() {
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, ordered("RightStick", RIGHT, "RVWAP", RIGHT)),
                install(EPIC, Map.of("RightStick", RIGHT)));

        assertTrue(plan.unanswered(Map.of()).isEmpty());
        Map<String, Row> rows = byDevice(plan.rows());
        assertEquals(Kind.IDENTICAL, rows.get("RightStick").kind(), "the first in the file is the device");
        assertEquals(Kind.SHADOWED, rows.get("RVWAP").kind());
        Resolution resolution = plan.resolve(Map.of());
        assertEquals(List.of("RightStick"), names(resolution));
        assertEquals(List.of("RVWAP"), resolution.pendingRemovals());
    }

    @Test
    void aShadowedNameThatIsFirstElsewhereIsStillInPlay() {
        FirstSetupPlan plan = plan(Set.of(),
                install(STEAM, ordered("RVWAP", RIGHT, "RightStick", RIGHT)),
                install(EPIC, Map.of("RightStick", RIGHT)));

        Row row = only(plan.rows());
        assertEquals(Kind.NAME_DIFFERS, row.kind(), "Steam uses RVWAP, Epic uses RightStick - a real disagreement");
        assertEquals(List.of("RVWAP"), plan.resolve(plan.answersFavouring(EPIC)).pendingRemovals());
    }

    @Test
    void nothingOfTheUsersMeansNothingToSetUp() {
        assertTrue(plan(Set.of(), install(STEAM, Map.of()), install(EPIC, Map.of())).isEmpty());
    }

    private static FirstSetupPlan plan(Set<String> referenced, Installation... installations) {
        return new FirstSetupPlan(List.of(installations), referenced);
    }

    private static Installation install(long id, Map<String, HardwareId> elements) {
        return install(id, elements, Map.of());
    }

    private static Installation install(long id, Map<String, HardwareId> elements,
                                        Map<String, Map<String, String>> labels) {
        return new Installation(id, id == STEAM ? "STEAM" : "EPIC", elements, Map.of(), labels);
    }

    /** Elements in file order, which decides which of two on one VID/PID the game uses. */
    private static Map<String, HardwareId> ordered(String first, HardwareId firstId, String second, HardwareId secondId) {
        Map<String, HardwareId> elements = new LinkedHashMap<>();
        elements.put(first, firstId);
        elements.put(second, secondId);
        return elements;
    }

    private static StoredDevice device(String name, HardwareId hardware, Map<String, String> labels) {
        return new StoredDevice(null, name, hardware.vid(), hardware.pid(), true, null, labels);
    }

    private static Row only(List<Row> rows) {
        assertEquals(1, rows.size(), rows.toString());
        return rows.getFirst();
    }

    private static Option optionHeldByEpic(Row row) {
        return row.options().stream().filter(option -> option.installs().contains(EPIC)).findFirst().orElseThrow();
    }

    private static Map<String, Row> byDevice(List<Row> rows) {
        Map<String, Row> map = new LinkedHashMap<>();
        rows.forEach(row -> map.put(row.deviceName(), row));
        return map;
    }

    private static List<String> names(Resolution resolution) {
        return resolution.devices().stream().map(StoredDevice::deviceName).toList();
    }
}
