package elite.intel.bindforge.preseteditor;

import elite.intel.bindforge.preseteditor.BindsContent.Judgement;
import elite.intel.bindforge.preseteditor.BindsContent.NotOffered;
import elite.intel.bindforge.preseteditor.BindsContent.Offered;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A file is a binds file on its contents, not its name - against the specimens in {@code reference-data/}, and the
 * shapes bind-editor.md's corpus count found under a {@code .binds} name.
 */
class BindsContentTest {

    private static final String BINDING = "<Root PresetName=\"%s\" MajorVersion=\"4\" MinorVersion=\"2\">"
            + "<KeyboardLayout>en-US</KeyboardLayout>"
            + "<YawLeftButton><Primary Device=\"Keyboard\" Key=\"Key_A\" />"
            + "<Secondary Device=\"{NoDevice}\" Key=\"\" /></YawLeftButton></Root>";

    @Test
    void theBindsSpecimensAreOfferedUnderTheirPresetName() {
        assertEquals(new Offered("Custom"), judgeSpecimen("Custom.4.2.binds"));
        assertEquals(new Offered("Custom"), judgeSpecimen("Custom.3.0.binds"));
        assertEquals(new Offered("DualVirpilDawnTreader"), judgeSpecimen("DualVirpilDawnTreader.4.1.binds"));
    }

    /** Its PresetName is "Custom", and its file name gives "Custom.4.2 (2)": the dropdown would lie either way. */
    @Test
    void aFileWhosePresetNameIsNotItsFileNameIsNotOffered() {
        assertEquals(new NotOffered(NotOfferedReason.NAME_MISMATCH, "Custom"), judgeSpecimen("Custom.4.2 (2).binds"));
    }

    @Test
    void aFactoryBindsFileWithNoSuffixIsOffered() {
        // The factory files write SortOrder and no version, as the installations on Alan's machine do.
        String factory = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n"
                + "<Root PresetName=\"KeyboardMouseOnly\" SortOrder=\"0\">"
                + "<YawLeftButton><Primary Device=\"Keyboard\" Key=\"Key_A\" /></YawLeftButton></Root>";

        assertEquals(new Offered("KeyboardMouseOnly"), judge("KeyboardMouseOnly.binds", factory));
    }

    @Test
    void anAxisBindingCounts() {
        String axisOnly = "<Root PresetName=\"Axes\"><YawAxisRaw><Binding Device=\"vJoy\" Key=\"Joy_XAxis\" />"
                + "</YawAxisRaw></Root>";

        assertEquals(new Offered("Axes"), judge("Axes.4.2.binds", axisOnly));
    }

    @Test
    void aPresetFileSavedUnderABindsNameIsNamedAsOne() throws IOException {
        assertEquals(new NotOffered(NotOfferedReason.PRESET_FILE, "KeyboardMouseOnly\nCustom\nCustom\nCustom"),
                judge("StartPreset.binds", startPresetSpecimen("StartPreset.4.start")));
        assertEquals(new NotOffered(NotOfferedReason.PRESET_FILE, "Custom"),
                judge("StartPreset.binds", startPresetSpecimen("StartPreset.start")));
    }

    @Test
    void textLongerThanAPresetFileIsNotXml() {
        assertEquals(NotOfferedReason.NOT_XML, reason(judge("Notes.binds", "one\ntwo\nthree\nfour\nfive")));
    }

    @Test
    void anotherGamesConfigIsNamedByItsRoot() {
        assertEquals(new NotOffered(NotOfferedReason.OTHER_XML, "ActionMaps"),
                judge("StarCitizen.binds", "<ActionMaps version=\"1\"><actionmap name=\"x\"/></ActionMaps>"));
    }

    /** The corpus's two binds files with their {@code <Root>} truncated away. */
    @Test
    void aBindsFileWithoutItsRootIsOtherXml() {
        assertEquals(new NotOffered(NotOfferedReason.OTHER_XML, "KeyboardLayout"),
                judge("Custom.4.2.binds", "<KeyboardLayout>en-US</KeyboardLayout>"));
    }

    @Test
    void aRootWithOnlySettingsIsNotABindsFile() {
        assertEquals(NotOfferedReason.ROOT_WITHOUT_BINDINGS, reason(judge("Custom.4.2.binds", "<Root/>")));
        assertEquals(NotOfferedReason.ROOT_WITHOUT_BINDINGS, reason(judge("Custom.4.2.binds",
                "<Root PresetName=\"Custom\"><KeyboardLayout>en-US</KeyboardLayout>"
                        + "<MouseXMode Value=\"Bindings_MouseYaw\" /></Root>")));
    }

    @Test
    void aBindsFileWithNoPresetNameCannotBeNamedByALine() {
        String bare = BINDING.formatted("").replace(" PresetName=\"\"", "");

        assertEquals(NotOfferedReason.NO_PRESET_NAME, reason(judge("Custom.4.2.binds", bare)));
    }

    @Test
    void aFileCutShortIsNotXml() {
        String truncated = BINDING.formatted("Custom").substring(0, 120);

        assertEquals(NotOfferedReason.NOT_XML, reason(judge("Custom.4.2.binds", truncated)));
    }

    @Test
    void htmlIsNotXmlAndNoDoctypeIsFollowed() {
        assertEquals(NotOfferedReason.NOT_XML,
                reason(judge("Custom.4.2.binds", "<!DOCTYPE html><html><body>Not found</body></html>")));
    }

    @Test
    void anEmptyFileIsEmpty() {
        assertEquals(new NotOffered(NotOfferedReason.EMPTY, ""), judge("Custom.4.2.binds", "  \r\n "));
    }

    @Test
    void aByteOrderMarkIsNotAProblem() {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = BINDING.formatted("Custom").getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, content, 0, bom.length);
        System.arraycopy(body, 0, content, bom.length, body.length);

        assertEquals(new Offered("Custom"), BindsContent.judge("Custom.4.2.binds", content));
    }

    @Test
    void aFileThatCannotBeReadIsUnreadableNotAnException(@TempDir Path dir) throws IOException {
        Path folderWithABindsName = Files.createDirectory(dir.resolve("Custom.4.2.binds"));

        assertEquals(NotOfferedReason.UNREADABLE, reason(BindsContent.judge(folderWithABindsName)));
    }

    private static Judgement judgeSpecimen(String name) {
        return BindsContent.judge(Specimens.bindsSpecimen(name));
    }

    private static Judgement judge(String fileName, String content) {
        return BindsContent.judge(fileName, content.getBytes(StandardCharsets.UTF_8));
    }

    private static NotOfferedReason reason(Judgement judgement) {
        return assertInstanceOf(NotOffered.class, judgement).reason();
    }

    private String startPresetSpecimen(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/bindforge/startpreset/" + name)) {
            assertNotNull(in, "missing test resource " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
