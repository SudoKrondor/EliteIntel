package elite.intel.bindforge.preseteditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The list the four dropdowns offer: the player's binds files, from the set APPLY writes (Storage S6), the factory ones,
 * and the files that are neither.
 */
class BindNameCatalogTest {

    @TempDir
    Path root;

    /** The player's files, as the set holds them - by file name. */
    private final Map<String, byte[]> set = new LinkedHashMap<>();

    @Test
    void thePlayersBindsFilesAreOfferedAndTheRestAreNamed() throws IOException {
        specimen("Custom.4.2.binds");
        specimen("DualVirpilDawnTreader.4.1.binds");
        // WHY: the set holds only <name>.<major>.<minor>.binds names, so a name mismatch is a correctly named file
        // whose PresetName is another's - the case Custom.4.2 (2).binds showed, under a name the set can hold.
        set.put("Spare.4.2.binds", bytes(binds("Custom")));

        BindNameList list = BindNameCatalog.list(set, List.of());

        assertEquals(List.of(
                        new BindNameEntry("Custom", BindsFileOrigin.USER, List.of(Path.of("Custom.4.2.binds"))),
                        new BindNameEntry("DualVirpilDawnTreader", BindsFileOrigin.USER,
                                List.of(Path.of("DualVirpilDawnTreader.4.1.binds")))),
                list.entries());
        assertEquals(List.of(new NotOfferedFile(Path.of("Spare.4.2.binds"), BindsFileOrigin.USER,
                        NotOfferedReason.NAME_MISMATCH, "Custom")),
                list.notOffered());
    }

    @Test
    void theFactoryBindsFilesAreOfferedOnceWithAFileFromEachInstallation() throws IOException {
        specimen("Custom.4.2.binds");
        Path steam = folder("Steam/ControlSchemes");
        Path epic = folder("Epic/ControlSchemes");
        factory(steam, "KeyboardMouseOnly");
        factory(epic, "KeyboardMouseOnly");
        factory(epic, "SaitekX56");

        BindNameList list = BindNameCatalog.list(set, List.of(steam, epic));

        assertEquals(List.of(
                        new BindNameEntry("Custom", BindsFileOrigin.USER, List.of(Path.of("Custom.4.2.binds"))),
                        new BindNameEntry("KeyboardMouseOnly", BindsFileOrigin.FACTORY,
                                List.of(steam.resolve("KeyboardMouseOnly.binds"), epic.resolve("KeyboardMouseOnly.binds"))),
                        new BindNameEntry("SaitekX56", BindsFileOrigin.FACTORY, List.of(epic.resolve("SaitekX56.binds")))),
                list.entries());
    }

    /** The specimen StartPreset.4.start: General on a factory binds file, the rest on the player's. */
    @Test
    void everyLineOfTheSplitSpecimenFindsItsBindName() throws IOException {
        specimen("Custom.4.2.binds");
        Path controlSchemes = folder("Steam/ControlSchemes");
        factory(controlSchemes, "KeyboardMouseOnly");

        BindNameList list = BindNameCatalog.list(set, List.of(controlSchemes));

        assertEquals(BindsFileOrigin.FACTORY, list.find("KeyboardMouseOnly").orElseThrow().origin());
        assertEquals(BindsFileOrigin.USER, list.find("Custom").orElseThrow().origin());
        assertEquals(Optional.empty(), list.find("custom"), "a near miss is not a match");
    }

    @Test
    void aPlayersBindsFileShadowsAFactoryOneOfTheSameName() throws IOException {
        set.put("KeyboardMouseOnly.4.2.binds", bytes(binds("KeyboardMouseOnly")));
        Path controlSchemes = folder("Steam/ControlSchemes");
        factory(controlSchemes, "KeyboardMouseOnly");

        BindNameList list = BindNameCatalog.list(set, List.of(controlSchemes));

        assertEquals(List.of(new BindNameEntry("KeyboardMouseOnly", BindsFileOrigin.USER,
                List.of(Path.of("KeyboardMouseOnly.4.2.binds")))), list.entries());
    }

    /** Not expected - the game upgrades the old one in place - but not silently halved if it happens. */
    @Test
    void twoOfThePlayersFilesForOneNameAreBothKept() throws IOException {
        specimen("Custom.4.2.binds");
        specimen("Custom.3.0.binds");

        BindNameList list = BindNameCatalog.list(set, List.of());

        assertEquals(List.of(new BindNameEntry("Custom", BindsFileOrigin.USER,
                        List.of(Path.of("Custom.3.0.binds"), Path.of("Custom.4.2.binds")))),
                list.entries());
    }

    @Test
    void anInstallationThatCannotBeListedIsSkipped() throws IOException {
        specimen("Custom.4.2.binds");
        Path unplugged = root.resolve("Unplugged/ControlSchemes");

        BindNameList list = BindNameCatalog.list(set, List.of(unplugged));

        assertEquals(List.of("Custom"), list.entries().stream().map(BindNameEntry::bindName).toList());
        assertEquals(List.of(), list.notOffered());
    }

    @Test
    void aFactoryFileThatIsNotABindsFileIsNamedAsFactory() throws IOException {
        Path controlSchemes = folder("Steam/ControlSchemes");
        write(controlSchemes, "Broken.binds", "");

        BindNameList list = BindNameCatalog.list(set, List.of(controlSchemes));

        assertEquals(List.of(new NotOfferedFile(controlSchemes.resolve("Broken.binds"), BindsFileOrigin.FACTORY,
                NotOfferedReason.EMPTY, "")), list.notOffered());
    }

    private Path folder(String relative) throws IOException {
        return Files.createDirectories(root.resolve(relative));
    }

    private void specimen(String name) throws IOException {
        set.put(name, Files.readAllBytes(Specimens.bindsSpecimen(name)));
    }

    private static void factory(Path controlSchemes, String name) throws IOException {
        write(controlSchemes, name + ".binds", binds(name).replace(" MajorVersion=\"4\" MinorVersion=\"2\"", " SortOrder=\"0\""));
    }

    private static String binds(String bindName) {
        return "<Root PresetName=\"" + bindName + "\" MajorVersion=\"4\" MinorVersion=\"2\">"
                + "<YawLeftButton><Primary Device=\"Keyboard\" Key=\"Key_A\" /></YawLeftButton></Root>";
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static void write(Path folder, String name, String content) throws IOException {
        Files.writeString(folder.resolve(name), content, StandardCharsets.UTF_8);
    }
}
