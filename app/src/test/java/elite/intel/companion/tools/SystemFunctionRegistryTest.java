package elite.intel.companion.tools;

import elite.intel.companion.model.ThoughtSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Self-check for the @RegisterSystemFunction scan: proves the registry discovers a stable,
 * duplicate-free set of self-describing system functions, and that the COMMANDER/EVENT source split
 * matches the architecture (§4.1/§4.2). The expected size is a hard sentinel - changing the number of
 * functions must be a conscious edit.
 */
class SystemFunctionRegistryTest {

    private static final SystemFunctionRegistry registry = SystemFunctionRegistry.getInstance();

    @BeforeAll
    static void load() {
        registry.load();
    }

    private static Set<String> ids(ThoughtSource source) {
        return registry.forSource(source).stream().map(SystemFunction::id).collect(Collectors.toSet());
    }

    @Test
    void discoversAllSystemFunctionsWithUniqueIds() {
        assertEquals(3, registry.byId().size());
        assertEquals(
                Set.of("speak", "classify_turn", "search_in_memory"),
                registry.byId().keySet());
    }

    @Test
    void eventSourceGetsOnlySpeak() {
        assertEquals(Set.of("speak"), ids(ThoughtSource.EVENT));
    }

    @Test
    void commanderSourceGetsEveryFunction() {
        assertEquals(registry.byId().keySet(), ids(ThoughtSource.COMMANDER));
    }

    @Test
    void commanderOnlyFunctionsAreNotOfferedToEvents() {
        Set<String> eventIds = ids(ThoughtSource.EVENT);
        for (String commanderOnly : Set.of("classify_turn", "search_in_memory")) {
            assertTrue(registry.find(commanderOnly).isPresent());
            assertTrue(!eventIds.contains(commanderOnly), commanderOnly + " must not reach EVENT thoughts");
        }
    }
}
