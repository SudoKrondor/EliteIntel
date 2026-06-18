package elite.intel.ai.brain.actions.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Scans elite.intel.ai.brain.actions.handlers.query (recursively) for @RegisterQuery
 * classes, instantiates them via no-arg constructor, stores by id. Read-only source of
 * self-describing queries. NOT wired to dispatch yet (parallel scaffold to
 * CommandRegistry).
 */
public final class QueryRegistry {

    private static final Logger log = LogManager.getLogger(QueryRegistry.class);
    private static final String SCAN_PACKAGE = "elite.intel.ai.brain.actions.handlers.query";

    private static final QueryRegistry INSTANCE = new QueryRegistry();
    private final Map<String, IntelQuery> byId = new LinkedHashMap<>();

    private QueryRegistry() {
    }

    public static QueryRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        byId.clear();
        Reflections reflections = new Reflections(
                SCAN_PACKAGE,
                new TypeAnnotationsScanner(),
                new SubTypesScanner()
        );
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(RegisterQuery.class);
        for (Class<?> type : annotated) {
            try {
                Object instance = type.getDeclaredConstructor().newInstance();
                if (!(instance instanceof IntelQuery query)) {
                    log.warn("@RegisterQuery on non-IntelQuery class, skipping: {}", type.getName());
                    continue;
                }
                String id = query.id();
                if (id == null || id.isBlank()) {
                    log.warn("IntelQuery with blank id, skipping: {}", type.getName());
                    continue;
                }
                IntelQuery previous = byId.putIfAbsent(id, query);
                if (previous != null) {
                    log.warn("Duplicate query id '{}' from {} (kept {})",
                            id, type.getName(), previous.getClass().getName());
                }
            } catch (Exception e) {
                log.error("Failed to instantiate IntelQuery: {}", type.getName(), e);
            }
        }
        log.info("QueryRegistry: discovered {} self-describing query(ies)", byId.size());
    }

    public Map<String, IntelQuery> byId() {
        return Collections.unmodifiableMap(byId);
    }

    public Optional<IntelQuery> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}
