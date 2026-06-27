package elite.intel.companion.tools;

import elite.intel.companion.model.ThoughtSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Scans {@code elite.intel.companion.tools} for {@link RegisterSystemFunction} classes, instantiates
 * them via no-arg constructor, and stores them by {@code id()}. Mirror of {@code CommandRegistry} for
 * companion system functions.
 */
public final class SystemFunctionRegistry {

    private static final Logger log = LogManager.getLogger(SystemFunctionRegistry.class);
    private static final String SCAN_PACKAGE = "elite.intel.companion.tools";

    private static final SystemFunctionRegistry INSTANCE = new SystemFunctionRegistry();
    private final Map<String, SystemFunction> byId = new LinkedHashMap<>();

    private SystemFunctionRegistry() {
    }

    public static SystemFunctionRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        byId.clear();
        Reflections reflections = new Reflections(
                SCAN_PACKAGE,
                new TypeAnnotationsScanner(),
                new SubTypesScanner()
        );
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(RegisterSystemFunction.class);
        for (Class<?> type : annotated) {
            try {
                Object instance = type.getDeclaredConstructor().newInstance();
                if (!(instance instanceof SystemFunction function)) {
                    log.warn("@RegisterSystemFunction on non-SystemFunction class, skipping: {}", type.getName());
                    continue;
                }
                String id = function.id();
                if (id == null || id.isBlank()) {
                    log.warn("SystemFunction with blank id, skipping: {}", type.getName());
                    continue;
                }
                SystemFunction previous = byId.putIfAbsent(id, function);
                if (previous != null) {
                    log.warn("Duplicate system function id '{}' from {} (kept {})",
                            id, type.getName(), previous.getClass().getName());
                }
            } catch (Exception e) {
                log.error("Failed to instantiate SystemFunction: {}", type.getName(), e);
            }
        }
        log.info("SystemFunctionRegistry: discovered {} system function(s)", byId.size());
    }

    public Map<String, SystemFunction> byId() {
        return Collections.unmodifiableMap(byId);
    }

    public Optional<SystemFunction> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** System functions available to the given source. */
    public List<SystemFunction> forSource(ThoughtSource source) {
        return byId.values().stream().filter(f -> f.availableFor(source)).toList();
    }
}
