package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What BindForge's log keeps of a reconcile, for a support bundle: which installation held what, what was asked, and
 * which side each answer took.
 */
class ReconcileRecordTest {

    private static final String BINDS = "Custom.4.2.binds";

    /** One controller under two names: Steam calls it RVWAP, Epic RightStick, and the player keeps Steam's. */
    @Test
    void theRecordNamesWhatEachInstallationHeldAndWhichSideTheAnswerTook() throws IOException {
        FirstSetupReading reading = FirstSetupReading.of(Path.of("Bindings"),
                Map.of(BINDS, binds("RightStick", "RightStick")),
                List.of(installation(1, "STEAM", "RVWAP"), installation(2, "EPIC", "RightStick")),
                List.of(new InstallationRow(3, "FRONTIER", "D:\\Frontier\\EliteDangerous", true, true)),
                FrontierStockDevices.getInstance(), FrontierStockButtonMaps.getInstance());
        FirstSetupPlan.Row question = reading.plan().rows().getFirst();
        Map<String, String> answers = Map.of(question.id(), "RVWAP");
        FirstSetupFiles.Merged merged = FirstSetupFiles.merge(reading, answers);

        String read = ReconcileRecord.describeReading("first-time startup", reading);
        String decided = ReconcileRecord.describeDecision("first-time startup", reading, answers, null, merged);

        assertTrue(read.contains("not reachable: FRONTIER #3 (D:\\Frontier\\EliteDangerous)"), read);
        assertTrue(read.contains("STEAM #1 (STEAM): DeviceMappings.xml"), read);
        assertTrue(read.contains("elements, in file order: RVWAP [3344:03F5]"), read);
        assertTrue(read.contains("elements, in file order: RightStick [3344:03F5]"), read);
        assertTrue(read.contains("bindings in " + BINDS + " name: {RightStick=2}"), read);
        assertTrue(read.contains("NAME_DIFFERS"), read);
        assertTrue(decided.contains("answered question by question"), decided);
        assertTrue(decided.contains("NAME_DIFFERS") && decided.contains("[STEAM #1]"), decided);
        assertTrue(decided.contains("kept RVWAP [3344:03F5] from STEAM #1"), decided);
        assertTrue(decided.contains("moved RightStick -> RVWAP"), decided);
        assertTrue(decided.contains("2 bindings moved"), decided);
    }

    private static InstallationFiles installation(long id, String storefront, String element) {
        byte[] mappings = ("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n<Root><" + element
                + "><PID>03F5</PID><VID>3344</VID></" + element + "></Root>\r\n").getBytes(StandardCharsets.UTF_8);
        return new InstallationFiles(id, storefront, Path.of(storefront), mappings, Map.of());
    }

    private static byte[] binds(String... devices) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n"
                + "<Root PresetName=\"Custom\" MajorVersion=\"4\" MinorVersion=\"2\">\r\n");
        for (int i = 0; i < devices.length; i++) {
            xml.append("\t<Action").append(i).append(">\r\n\t\t<Primary Device=\"").append(devices[i])
                    .append("\" Key=\"Joy_").append(i + 1).append("\" />\r\n\t</Action").append(i).append(">\r\n");
        }
        return xml.append("</Root>\r\n").toString().getBytes(StandardCharsets.UTF_8);
    }
}
