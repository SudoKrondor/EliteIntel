package elite.intel.ai.brain.actions.handlers.queries;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Self-check for the @RegisterQuery scan: proves QueryRegistry discovers a stable,
 * duplicate-free set of self-describing queries. The legacy enum Queries is gone;
 * the registry is now the single source of truth, so the expected size is a hard
 * sentinel - changing the number of @RegisterQuery classes must be a conscious edit.
 */
class QueryRegistryTest {

    /**
     * Number of @RegisterQuery built-in query handlers. Update deliberately when adding/removing one.
     */
    private static final int EXPECTED_QUERY_COUNT = 41;

    @BeforeAll
    static void loadRegistry() {
        QueryRegistry.getInstance().load();
    }

    // 1. Registry loaded and not empty
    @Test
    void registryIsNotEmpty() {
        assertFalse(QueryRegistry.getInstance().byId().isEmpty(),
                "QueryRegistry.byId() is empty - the @RegisterQuery scan found nothing");
    }

    // 2. No duplicate ids among registry values
    @Test
    void noDuplicateIdsInRegistry() {
        Map<String, IntelQuery> byId = QueryRegistry.getInstance().byId();
        Set<String> ids = new HashSet<>();
        for (IntelQuery q : byId.values()) {
            assertTrue(ids.add(q.id()),
                    "Duplicate id among registry values: " + q.id() + " (" + q.getClass().getName() + ")");
        }
        assertEquals(byId.size(), ids.size(), "byId() size does not match the number of unique ids");
    }

    // 3. Hard size sentinel: adding/removing @RegisterQuery must be a conscious change
    @Test
    void registryHasExpectedSize() {
        assertEquals(EXPECTED_QUERY_COUNT, QueryRegistry.getInstance().byId().size(),
                "Expected " + EXPECTED_QUERY_COUNT + " registered queries. " +
                        "Did the number of @RegisterQuery classes change? Update the expectation deliberately.");
    }

    // 4. Every id is non-blank and the key set is unique (safety net that used to be the enum cross-check)
    @Test
    void allIdsNonBlankAndUnique() {
        Map<String, IntelQuery> byId = QueryRegistry.getInstance().byId();
        for (String id : byId.keySet()) {
            assertFalse(id == null || id.isBlank(), "Blank id key in the registry");
        }
        assertEquals(byId.size(), new HashSet<>(byId.keySet()).size(),
                "Duplicate keys in byId()");
    }
}
