package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Onboarding's pages: one per attached controller with no name, each offered a default no other page shares, and a
 * typed name held to the rule against every other page (A6b, 2026-10-14).
 */
class OnboardingTest {

    private static final Map<String, String> LABELS = Map.of("Joy_1", "Button 1");

    @Test
    void onePagePerControllerThatNeedsAName() {
        MyDevice unnamed = attached("VIRPIL Controls 20220720", "3344", "0194");
        MyDevice named = new MyDevice("Stick", "LVWAP", "3344", "83F4", true, "LVWAP", false);
        MyDevice builtIn = new MyDevice("T-Rudder", "T-Rudder", "044F", "B679", true, null, true);

        List<Onboarding.Page> pages = pages(List.of(unnamed, named, builtIn), List.of(), Map.of());

        assertEquals(1, pages.size());
        assertEquals("VIRPILControls20220720", pages.getFirst().defaultName());
        assertEquals(LABELS, pages.getFirst().labels());
        assertFalse(pages.getFirst().waitsForApply(), "no binding names it by hardware id");
    }

    /** Named for Apply and not applied yet: waiting, not unnamed - asking again would give it a second name. */
    @Test
    void aControllerTheDraftNamesIsNotAskedAgain() {
        MyDevice unnamed = attached("VIRPIL Controls 20220720", "3344", "0194");

        assertTrue(pages(List.of(unnamed), List.of(new HardwareId("3344", "0194")), Map.of()).isEmpty());
    }

    @Test
    void twoControllersReportingOneNameAreOfferedDifferentDefaults() {
        List<Onboarding.Page> pages = pages(List.of(
                attached("VIRPIL Controls 20220720", "3344", "0194"),
                attached("VIRPIL Controls 20220720", "3344", "0195")), List.of(), Map.of());

        assertEquals("VIRPILControls20220720", pages.get(0).defaultName());
        assertEquals("VIRPILControls202207202", pages.get(1).defaultName());
    }

    /** A controller .binds names by hex waits for Apply, and the page says how many bindings move. */
    @Test
    void aControllerTheBindingsNameByHexWaitsForApply() {
        List<Onboarding.Page> pages = pages(List.of(attached("VIRPIL Controls 20220720", "3344", "83F3")), List.of(),
                Map.of("334483F3", 100, "83f33344", 31, "334403F5", 7));

        Onboarding.Page page = pages.getFirst();
        assertTrue(page.waitsForApply());
        assertEquals(131, page.bindings());
        assertEquals(Set.of("334483F3", "83f33344"), page.hardwareIds(), "as found, for the rewrite");
    }

    /** Two controllers on one VID/PID: only the first page promises the bindings, so no promise goes unkept. */
    @Test
    void aSharedHardwareIdIsPromisedByTheFirstPageOnly() {
        List<Onboarding.Page> pages = pages(List.of(
                attached("VIRPIL Controls 20220720", "3344", "83F3"),
                attached("VIRPIL Controls 20220720", "3344", "83F3")), List.of(), Map.of("334483F3", 100));

        assertTrue(pages.get(0).waitsForApply());
        assertEquals(100, pages.get(0).bindings());
        assertFalse(pages.get(1).waitsForApply(), "the second is named now, its bindings already promised");
    }

    @Test
    void aTypedNameMayNotBeAnotherPagesName() {
        List<String> chosen = List.of("LeftStick", "RightStick");

        DeviceNames.Verdict verdict = Onboarding.check("leftstick", 1, chosen, List.of());

        assertEquals(DeviceNames.Problem.CLASHES, verdict.problem());
        assertEquals("LeftStick", verdict.clashesWith());
        assertTrue(Onboarding.check("RightStick", 1, chosen, List.of()).valid(), "its own name is not a clash");
    }

    @Test
    void aTypedNameMayNotBeOneAlreadyTaken() {
        assertEquals(DeviceNames.Problem.CLASHES,
                Onboarding.check("T_Rudder", 0, List.of("Whatever"), List.of("T-Rudder")).problem());
    }

    private static List<Onboarding.Page> pages(List<MyDevice> devices, List<HardwareId> held,
                                               Map<String, Integer> hardwareIds) {
        return Onboarding.pages(devices, held, List.of("T-Rudder", "LVWAP"), hardwareIds, device -> LABELS);
    }

    private static MyDevice attached(String hardwareName, String vid, String pid) {
        return new MyDevice(hardwareName, null, vid, pid, true, null, false);
    }
}
