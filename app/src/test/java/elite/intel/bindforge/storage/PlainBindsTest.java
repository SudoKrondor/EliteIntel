package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static elite.intel.bindforge.storage.BindingsComparisonTest.binds;
import static elite.intel.bindforge.storage.BindingsComparisonTest.binds43;
import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain {@code .binds} judged against the settled set (storage.md rule 14, <em>Settled with Storage S13b's
 * sources</em>): a twin or the factory file's values remove it, anything else converts it, and one that does not read
 * as a binds file is left alone.
 */
class PlainBindsTest {

    private static final Map<String, byte[]> CUSTOM_SET = Map.of("Custom.4.2.binds",
            bytes(binds("Custom", "Key_A", "Joy_XAxis", "")), "StartPreset.4.start", bytes("Custom"));

    @Test
    void aConvertedFileTakesTheVersionItsRootCarries() {
        PlainBinds.Plan plan = plan(Map.of("Spare.binds", binds43("Frontier", "Key_Z", "Joy_XAxis", "")), Map.of());

        assertEquals(List.of(new PlainBinds.Tidied("Spare.binds", PlainBinds.Fate.CONVERTED, "Spare.4.3.binds")),
                plan.tidied());
        assertEquals(binds43("Spare", "Key_Z", "Joy_XAxis", ""), text(plan.converted().get("Spare.4.3.binds")));
        assertEquals(Set.of("Spare.binds"), plan.remove());
    }

    /** CREATE's version rule: a root with no version names the file 4.2. */
    @Test
    void aRootWithNoVersionIsNamedFourTwo() {
        String unversioned = binds("Frontier", "Key_Z", "Joy_XAxis", "")
                .replace(" MajorVersion=\"4\" MinorVersion=\"2\"", "");

        PlainBinds.Plan plan = plan(Map.of("Spare.binds", unversioned), Map.of());

        assertTrue(plan.converted().containsKey("Spare.4.2.binds"), plan.converted().keySet().toString());
    }

    /** A factory file of that name whose values differ: converted, every byte kept. */
    @Test
    void aFileThatDiffersFromTheFactoryFileIsConverted() {
        PlainBinds.Plan plan = plan(Map.of("KeyboardMouseOnly.binds", binds("Copied", "Key_Z", "Joy_XAxis", "")),
                Map.of("KeyboardMouseOnly", bytes(binds("KeyboardMouseOnly", "Key_A", "Joy_XAxis", ""))));

        assertEquals(PlainBinds.Fate.CONVERTED, plan.tidied().getFirst().fate());
        assertEquals("KeyboardMouseOnly.4.2.binds", plan.tidied().getFirst().to());
    }

    @Test
    void aFileEqualToTheFactoryFileButItsNameIsRemoved() {
        PlainBinds.Plan plan = plan(Map.of("KeyboardMouseOnly.binds", binds("Copied", "Key_A", "Joy_XAxis", "")),
                Map.of("KeyboardMouseOnly", bytes(binds("KeyboardMouseOnly", "Key_A", "Joy_XAxis", ""))));

        assertEquals(PlainBinds.Fate.FACTORY, plan.tidied().getFirst().fate());
        assertTrue(plan.converted().isEmpty());
    }

    /** Two spellings, only where the file system heeds case: the first is converted, the second is its twin. */
    @Test
    void aSecondSpellingIsATwinOfTheFirstConverted() {
        PlainBinds.Plan plan = plan(Map.of("Spare.binds", binds("Spare", "Key_A", "Joy_XAxis", ""),
                "spare.binds", binds("Spare", "Key_B", "Joy_XAxis", "")), Map.of());

        assertEquals(List.of(new PlainBinds.Tidied("Spare.binds", PlainBinds.Fate.CONVERTED, "Spare.4.2.binds"),
                new PlainBinds.Tidied("spare.binds", PlainBinds.Fate.TWIN, "Spare.4.2.binds")), plan.tidied());
        assertEquals(1, plan.converted().size());
    }

    /** XML that is not under a {@code <Root>} has no version to name a file by - left alone. */
    @Test
    void xmlThatIsNotABindsFileIsLeftAlone() {
        PlainBinds.Plan plan = plan(Map.of("Notes.binds", "<Notes><Line>hello</Line></Notes>"), Map.of());

        assertEquals(List.of(new PlainBinds.Tidied("Notes.binds", PlainBinds.Fate.UNREADABLE, null)), plan.tidied());
        assertTrue(plan.remove().isEmpty());
        assertFalse(plan.anything());
    }

    /** A name a file name allows but XML does not, and a letter beyond ASCII, as the root's PresetName holds them. */
    @Test
    void aPresetNameIsEscapedAndWrittenAsUtf8() throws Exception {
        byte[] named = Create.withPresetName(bytes(binds("Frontier", "Key_Z", "Joy_XAxis", "")),
                PlainBinds.presetNameText("Tom & Jérôme"));

        assertTrue(text(named).contains("PresetName=\"Tom &amp; Jérôme\""), text(named));
        assertTrue(PlainBinds.readsAsBindsFile(named));
    }

    private static PlainBinds.Plan plan(Map<String, String> plain, Map<String, byte[]> factory) {
        Map<String, byte[]> files = new TreeMap<>();
        plain.forEach((name, content) -> files.put(name, bytes(content)));
        return PlainBinds.plan(PlainBinds.Found.of(files), CUSTOM_SET, factory);
    }
}
