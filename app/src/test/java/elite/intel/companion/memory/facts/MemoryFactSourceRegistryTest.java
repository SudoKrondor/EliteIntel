package elite.intel.companion.memory.facts;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryFactSourceRegistryTest {

    @Test
    void discoversAnnotatedSourceByScan() {
        MemoryFactSourceRegistry registry = MemoryFactSourceRegistry.getInstance();
        registry.load();

        assertTrue(registry.sources().stream().anyMatch(s -> Probe.ID.equals(s.id())),
                "expected the annotated probe source to be discovered by the classpath scan");
    }

    /** A discoverable fact source that exists only to prove the registry's annotation scan wires it up. */
    @RegisterMemoryFactSource
    public static final class Probe implements MemoryFactSource {
        static final String ID = "test_probe_fact_source";

        @Override public String id() { return ID; }

        @Override public List<String> factsFor(MemoryFactContext context) { return List.of("probe fact"); }
    }
}
