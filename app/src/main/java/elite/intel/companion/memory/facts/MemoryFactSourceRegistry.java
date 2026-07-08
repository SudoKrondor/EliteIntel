package elite.intel.companion.memory.facts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans {@code elite.intel.companion.memory.facts} (recursively) for {@link RegisterMemoryFactSource} classes,
 * instantiates each via its no-arg constructor, and holds them as the ordered set of pluggable fact sources. Same
 * discovery pattern as {@link elite.intel.ai.brain.actions.command.CommandRegistry}; {@link #load()} is called once
 * at startup.
 */
public final class MemoryFactSourceRegistry {

    private static final Logger log = LogManager.getLogger(MemoryFactSourceRegistry.class);
    private static final String SCAN_PACKAGE = "elite.intel.companion.memory.facts";

    private static final MemoryFactSourceRegistry INSTANCE = new MemoryFactSourceRegistry();
    private final List<MemoryFactSource> sources = new ArrayList<>();

    private MemoryFactSourceRegistry() {
    }

    public static MemoryFactSourceRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        sources.clear();
        Reflections reflections = new Reflections(
                SCAN_PACKAGE,
                new TypeAnnotationsScanner(),
                new SubTypesScanner()
        );
        Set<String> seenIds = new HashSet<>();
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(RegisterMemoryFactSource.class);
        for (Class<?> type : annotated) {
            try {
                Object instance = type.getDeclaredConstructor().newInstance();
                if (!(instance instanceof MemoryFactSource source)) {
                    log.warn("@RegisterMemoryFactSource on non-MemoryFactSource class, skipping: {}", type.getName());
                    continue;
                }
                String id = source.id();
                if (id == null || id.isBlank()) {
                    log.warn("MemoryFactSource with blank id, skipping: {}", type.getName());
                    continue;
                }
                if (!seenIds.add(id)) {
                    // id is the provenance label; a collision is a config mistake. Keep the first, like CommandRegistry.
                    log.warn("Duplicate MemoryFactSource id '{}' from {} (skipping)", id, type.getName());
                    continue;
                }
                sources.add(source);
            } catch (Exception e) {
                // WHY: broad boundary - reflection instantiation fails many ways (missing no-arg ctor, class-init
                // error); isolate the one bad source instead of aborting discovery of the rest.
                log.error("Failed to instantiate MemoryFactSource: {}", type.getName(), e);
            }
        }
        log.info("MemoryFactSourceRegistry: discovered {} fact source(s)", sources.size());
    }

    /** The registered fact sources, in discovery order; unmodifiable. Empty until {@link #load()} runs. */
    public List<MemoryFactSource> sources() {
        return Collections.unmodifiableList(sources);
    }
}
