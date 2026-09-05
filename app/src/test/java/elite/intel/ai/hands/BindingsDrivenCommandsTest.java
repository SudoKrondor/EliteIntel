package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Bindings.GameCommand} carries the whole of Elite's control set, but only the {@code DRIVEN}
 * entries are the ones EliteIntel presses itself, and only those are what the startup missing-binding
 * warning speaks about (see {@code BindingsMonitor#requiredGameBindings()}).
 *
 * <p>That flag has to stay in step with the code by hand, and nothing at runtime notices when it does
 * not: a new command pressing an unflagged control simply never warns that the control is unbound, and
 * the commander finds out when the command silently does nothing. So the driven set is checked against
 * the constants the main sources actually name, read out of the source tree rather than listed here -
 * a hand-kept list would go stale the first time someone writes a command.
 */
class BindingsDrivenCommandsTest {

    private static final Pattern CONSTANT = Pattern.compile("\\bBINDING_[A-Z0-9_]+\\b");

    @Test
    void everyGameCommandTheCodeUsesIsMarkedDriven() throws IOException {
        Set<String> referenced = constantsReferencedOutsideBindings();

        assertFalse(referenced.isEmpty(), "found no BINDING_* references at all - the source scan is broken");

        Set<String> unflagged = new TreeSet<>();
        for (String name : referenced) {
            if (!Bindings.GameCommand.valueOf(name).isDrivenByApp()) {
                unflagged.add(name);
            }
        }

        assertTrue(unflagged.isEmpty(),
                "these game commands are pressed by the app but not marked DRIVEN in Bindings.java, so the "
                        + "commander is never warned when they are unbound: " + unflagged);
    }

    /**
     * The flag is only worth carrying if it actually excludes most of the list; a change that quietly
     * marked everything would put the wall of irrelevant "missing binding" lines straight back.
     */
    @Test
    void drivenCommandsAreASmallPartOfElitesControlSet() {
        long driven = Stream.of(Bindings.GameCommand.values())
                .filter(Bindings.GameCommand::isDrivenByApp)
                .count();
        long all = Bindings.GameCommand.values().length;

        assertTrue(driven > 0, "no game command is marked DRIVEN");
        assertTrue(driven < all / 2,
                "most of Elite's control set is marked DRIVEN (" + driven + " of " + all
                        + "); the missing-binding warning is meant to name only what EliteIntel presses");
    }

    private static Set<String> constantsReferencedOutsideBindings() throws IOException {
        Path sources = mainSources();
        Set<String> names = new TreeSet<>();
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> !f.endsWith("ai/hands/Bindings.java"))
                    .toList()) {
                Matcher matcher = CONSTANT.matcher(Files.readString(file));
                while (matcher.find()) {
                    names.add(matcher.group());
                }
            }
        }
        // Names that no longer exist on the enum would be a compile error in the source that used them,
        // so anything found here resolves - except a stale reference inside a comment.
        names.removeIf(name -> Stream.of(Bindings.GameCommand.values())
                .noneMatch(command -> command.name().equals(name)));
        return names;
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
