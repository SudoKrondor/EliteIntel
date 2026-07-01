package elite.intel.ai.brain.actions.query;

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

    /** Number of @RegisterQuery built-in query handlers. Update deliberately when adding/removing one. */
    private static final int EXPECTED_QUERY_COUNT = 44;

    @BeforeAll
    static void loadRegistry() {
        QueryRegistry.getInstance().load();
    }

    // 1. Registry loaded and not empty
    @Test
    void registryIsNotEmpty() {
        assertFalse(QueryRegistry.getInstance().byId().isEmpty(),
                "QueryRegistry.byId() пуст — скан @RegisterQuery ничего не нашёл");
    }

    // 2. No duplicate ids among registry values
    @Test
    void noDuplicateIdsInRegistry() {
        Map<String, IntelQuery> byId = QueryRegistry.getInstance().byId();
        Set<String> ids = new HashSet<>();
        for (IntelQuery q : byId.values()) {
            assertTrue(ids.add(q.id()),
                    "Дубль id среди значений реестра: " + q.id() + " (" + q.getClass().getName() + ")");
        }
        assertEquals(byId.size(), ids.size(), "Размер byId() не совпал с числом уникальных id");
    }

    // 3. Hard size sentinel: adding/removing @RegisterQuery must be a conscious change
    @Test
    void registryHasExpectedSize() {
        assertEquals(EXPECTED_QUERY_COUNT, QueryRegistry.getInstance().byId().size(),
                "Ожидалось " + EXPECTED_QUERY_COUNT + " зарегистрированных запросов. " +
                        "Изменилось число @RegisterQuery? Обнови ожидание осознанно.");
    }

    // 4. Every id is non-blank and the key set is unique (safety net that used to be the enum cross-check)
    @Test
    void allIdsNonBlankAndUnique() {
        Map<String, IntelQuery> byId = QueryRegistry.getInstance().byId();
        for (String id : byId.keySet()) {
            assertFalse(id == null || id.isBlank(), "Пустой id-ключ в реестре");
        }
        assertEquals(byId.size(), new HashSet<>(byId.keySet()).size(),
                "Дублирующиеся ключи в byId()");
    }
}
