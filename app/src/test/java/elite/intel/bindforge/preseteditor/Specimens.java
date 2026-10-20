package elite.intel.bindforge.preseteditor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real {@code .binds} files in {@code docs/02-features/bindforge/reference-data/}, read where they are rather
 * than copied: a claim in the specs is only as good as the file it came from.
 */
final class Specimens {

    private Specimens() {
    }

    static Path bindsSpecimen(String name) {
        Path specimen = projectRoot().resolve("docs/02-features/bindforge/reference-data").resolve(name);
        assertTrue(Files.isRegularFile(specimen), "missing specimen " + specimen);
        return specimen;
    }

    private static Path projectRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null && !Files.isDirectory(candidate.resolve("docs"))) {
            candidate = candidate.getParent();
        }
        assertNotNull(candidate, "could not find the repository root from " + Path.of("").toAbsolutePath());
        return candidate;
    }
}
