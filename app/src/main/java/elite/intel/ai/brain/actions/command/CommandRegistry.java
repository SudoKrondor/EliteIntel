package elite.intel.ai.brain.actions.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Scans elite.intel.ai.brain.actions.command (recursively) for @RegisterCommand
 * classes, instantiates them via no-arg constructor, stores by id. Read-only
 * source of self-describing commands. NOT wired to dispatch in Stage 1.
 */
public final class CommandRegistry {

    private static final Logger log = LogManager.getLogger(CommandRegistry.class);
    private static final String SCAN_PACKAGE = "elite.intel.ai.brain.actions.command";

    private static final CommandRegistry INSTANCE = new CommandRegistry();
    private final Map<String, IntelCommand> byId = new LinkedHashMap<>();

    private CommandRegistry() {
    }

    public static CommandRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        byId.clear();
        Reflections reflections = new Reflections(
                SCAN_PACKAGE,
                new TypeAnnotationsScanner(),
                new SubTypesScanner()
        );
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(RegisterCommand.class);
        for (Class<?> type : annotated) {
            try {
                Object instance = type.getDeclaredConstructor().newInstance();
                if (!(instance instanceof IntelCommand command)) {
                    log.warn("@RegisterCommand on non-IntelCommand class, skipping: {}", type.getName());
                    continue;
                }
                String id = command.id();
                if (id == null || id.isBlank()) {
                    log.warn("IntelCommand with blank id, skipping: {}", type.getName());
                    continue;
                }
                IntelCommand previous = byId.putIfAbsent(id, command);
                if (previous != null) {
                    log.warn("Duplicate command id '{}' from {} (kept {})",
                            id, type.getName(), previous.getClass().getName());
                }
            } catch (Exception e) {
                log.error("Failed to instantiate IntelCommand: {}", type.getName(), e);
            }
        }
        log.info("CommandRegistry: discovered {} self-describing command(s)", byId.size());
    }

    public Map<String, IntelCommand> byId() {
        return Collections.unmodifiableMap(byId);
    }

    public Optional<IntelCommand> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Appends parameter rules for migrated built-in commands that (a) survived
     * reduction and (b) declare a non-empty parameter schema. Mirrors
     * CustomCommandRegistry.appendCustomCommandParamRules but sources the schema
     * from the self-describing registry instead of custom-command definitions.
     * Uses the shared CommandParamRules.appendCommandBlock formatter so built-in
     * and custom param blocks stay identical. No-op (no header) when nothing active.
     *
     * @param reducedActions phrase -> actionId map after reduction (values are action ids)
     * @param sb             prompt buffer to append to
     */
    public void appendBuiltInParamRules(Map<String, String> reducedActions, StringBuilder sb) {
        Set<String> activeIds = new HashSet<>(reducedActions.values());
        List<IntelCommand> active = byId.values().stream()
                .filter(c -> activeIds.contains(c.id()))
                .filter(c -> !c.parameters().isEmpty())
                .toList();
        if (active.isEmpty()) return;

        sb.append("\n");
        sb.append("BUILT-IN COMMAND PARAMS (required for the built-in actions above include ALL required params):\n");
        sb.append("\n");
        for (IntelCommand command : active) {
            CommandParamRules.appendCommandBlock(command.id(), command.parameters(), sb);
        }
    }
}
