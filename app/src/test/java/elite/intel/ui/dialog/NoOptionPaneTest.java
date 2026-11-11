package elite.intel.ui.dialog;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every confirm and message popup is a {@link HudConfirmDialog}, never a {@code JOptionPane} (the UI canon; HC, Alan,
 * 2026-11-10, when the last 31 were replaced). {@code JOptionPane} opens Swing's own decorated dialog with Swing's own
 * OK and Cancel, untranslated, beside the HUD's. Only {@code HudConfirmDialog} may name it, in its Javadoc.
 */
class NoOptionPaneTest {

    @Test
    void noSourceFileButHudConfirmDialogNamesJOptionPane() throws IOException {
        List<String> offenders;
        try (Stream<Path> files = Files.walk(mainSources())) {
            offenders = files.filter(file -> file.toString().endsWith(".java"))
                    .filter(file -> !file.getFileName().toString().equals("HudConfirmDialog.java"))
                    .filter(NoOptionPaneTest::namesOptionPane)
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertEquals(List.of(), offenders, "use HudConfirmDialog.confirm, show or info instead");
    }

    private static boolean namesOptionPane(Path file) {
        try {
            return Files.readString(file).contains("JOptionPane");
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + file, e);
        }
    }

    /**
     * Tests run from either the module directory or the repository root.
     */
    private static Path mainSources() {
        String relative = "src/main/java";
        Path fromModule = Path.of(relative);
        return Files.exists(fromModule) ? fromModule : Path.of("app").resolve(relative);
    }
}
