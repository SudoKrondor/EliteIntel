package elite.intel.ai.brain.actions.handlers.commands.custom;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Loads and persists customCommands from {@code custom_commands.json} in the app data directory.
 * <p>
 * Saves are written to a {@code .tmp} file and atomically renamed to {@code custom_commands.json},
 * reducing the risk of corruption on crash. A {@code custom_commands.json.bak} backup of the previous
 * file is kept alongside. If the main file is unreadable or corrupt, the backup is automatically
 * tried and a diagnostic warning is logged.
 * <p>
 * Callers are responsible for threading - {@link #save} must be called on a background thread.
 */
public final class CustomCommandRepository {

    private static final Logger log = LogManager.getLogger(CustomCommandRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<CustomCommandDefinition>>() {}.getType();
    /**
     * A {@code ${name}} reference, as the withdrawn parameter feature wrote them into SPEAK text.
     */
    private static final Pattern PARAM_PLACEHOLDER = Pattern.compile("\\$\\{[^}]*}");
    /**
     * A {@code {name:type}} (or bare {@code ${name}}) placeholder, as it appeared in trigger phrases.
     */
    private static final Pattern PHRASE_PLACEHOLDER = Pattern.compile("\\$?\\{[^{}]*}");

    /** Number of customCommands skipped during the most recent {@link #load()} call due to validation failures. */
    private int lastSkippedCount = 0;
    /** Human-readable labels for customCommands skipped during the most recent {@link #load()} call. */
    private List<String> lastSkippedLabels = List.of();
    /** True if the most recent {@link #load()} call restored customCommands from the backup file. */
    private boolean restoredFromBackup = false;

    int getLastSkippedCount() { return lastSkippedCount; }
    List<String> getLastSkippedLabels() { return lastSkippedLabels; }
    boolean wasRestoredFromBackup() { return restoredFromBackup; }

    /**
     * Loads all customCommands from {@code custom_commands.json}. Returns an empty list if the file does not
     * exist or is empty. If the main file is corrupt, automatically attempts to restore from
     * {@code custom_commands.json.bak} with a logged warning. Invalid individual customCommands are skipped with
     * an error log; the remaining valid customCommands are still returned.
     */
    public List<CustomCommandDefinition> load() {
        try {
            return load(AppPaths.getCustomCommandsFilePath());
        } catch (Exception e) {
            log.error("Failed to resolve custom command file path - no customCommands will be available", e);
            return Collections.emptyList();
        }
    }

    /**
     * Package-private test seam - loads customCommands from an explicit {@link Path} without consulting
     * {@link AppPaths}. Production code always calls {@link #load()}.
     */
    List<CustomCommandDefinition> load(Path path) {
        resetLoadDiagnostics();
        Path backup = path.resolveSibling(path.getFileName() + ".bak");

        if (!Files.exists(path)) {
            log.info("{} not found at {} - no customCommands loaded", path.getFileName(), path);
            return Collections.emptyList();
        }

        try {
            return parseAndFilter(path);
        } catch (Exception e) {
            log.warn("{} at {} could not be read ({}), attempting restore from backup",
                    path.getFileName(), path, e.getMessage());
        }

        if (!Files.exists(backup)) {
            log.error("{} is corrupt and no backup exists - no customCommands will be available", path.getFileName());
            return Collections.emptyList();
        }

        log.warn("Restoring customCommands from backup: {}", backup);
        try {
            List<CustomCommandDefinition> restored = parseAndFilter(backup);
            log.warn("Custom command restore from backup succeeded: {} command(s) loaded. Inspect {} for corruption.",
                    restored.size(), path.getFileName());
            restoredFromBackup = true;
            return restored;
        } catch (Exception e) {
            log.error("{} is also invalid ({}) - no customCommands will be available", backup.getFileName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private void resetLoadDiagnostics() {
        lastSkippedCount = 0;
        lastSkippedLabels = List.of();
        restoredFromBackup = false;
    }

    /**
     * Writes the custom command list to {@code custom_commands.json}, overwriting any existing content.
     * Caller must invoke this on a background thread.
     */
    public void save(List<CustomCommandDefinition> customCommands) {
        trySave(customCommands);
    }

    /**
     * Writes customCommands and reports whether the runtime file was updated successfully.
     */
    public boolean trySave(List<CustomCommandDefinition> customCommands) {
        try {
            return save(customCommands, AppPaths.getCustomCommandsFilePath());
        } catch (Exception e) {
            log.error("Failed to resolve custom command file path for save", e);
            return false;
        }
    }

    /**
     * Package-private test seam - saves customCommands to an explicit {@link Path}.
     * Production code always calls {@link #save(List)}.
     * <p>
     * The existing file is backed up to {@code <name>.bak} before the new data is written.
     * Data is first serialized and validated by round-trip, then written to {@code <name>.tmp},
     * and finally atomically renamed to replace the target file.
     */
    boolean save(List<CustomCommandDefinition> customCommands, Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            String json = GSON.toJson(customCommands);

            // Guard: validate generated JSON parses back before touching any files.
            List<CustomCommandDefinition> parsed = GSON.fromJson(json, LIST_TYPE);
            if (parsed == null) {
                log.error("Generated custom command JSON failed round-trip validation - {} not updated", path.getFileName());
                return false;
            }

            // Back up the current file so restore is possible if the new write is somehow lost.
            if (Files.exists(path)) {
                Path backup = path.resolveSibling(path.getFileName() + ".bak");
                try {
                    Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("Backed up {} to {}", path.getFileName(), backup.getFileName());
                } catch (IOException e) {
                    log.warn("Could not create backup before saving customCommands - proceeding anyway: {}", e.getMessage());
                }
            }

            // Write to a temp file then atomically rename to minimize the corruption window.
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                log.debug("Atomic move not supported on this filesystem - falling back to non-atomic replace");
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Saved {} custom command(s) to {}", customCommands.size(), path);
            return true;
        } catch (Exception e) {
            log.error("Failed to save custom command file", e);
            return false;
        }
    }

    /**
     * Reads and parses customCommands from {@code path}, filtering out individually invalid entries.
     * Updates {@link #lastSkippedCount} and {@link #lastSkippedLabels} as a side-effect.
     *
     * @throws IOException              if the file cannot be read or its JSON top-level is not an array
     * @throws com.google.gson.JsonSyntaxException if the JSON is malformed
     */
    private List<CustomCommandDefinition> parseAndFilter(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        if (json.isBlank()) {
            log.info("Custom command file at {} is empty - no customCommands loaded", path);
            lastSkippedCount = 0;
            lastSkippedLabels = List.of();
            return Collections.emptyList();
        }
        JsonElement tree = JsonParser.parseString(json);
        if (!tree.isJsonArray()) {
            throw new IOException("JSON top-level is not an array in " + path.getFileName());
        }
        migrateLegacyParameterUsage(tree.getAsJsonArray());
        List<CustomCommandDefinition> raw = GSON.fromJson(tree, LIST_TYPE);
        List<CustomCommandDefinition> valid = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (CustomCommandDefinition def : raw) {
            String label = customCommandLabel(def);
            try {
                def.validate();
            } catch (IllegalArgumentException e) {
                log.warn("Skipping custom command {}: {}", label, e.getMessage());
                skipped.add(label);
                continue;
            }
            List<String> formatErrors = CustomCommandValidator.validateFormat(def);
            if (!formatErrors.isEmpty()) {
                log.warn("Skipping custom command {}: {}", label, String.join("; ", formatErrors));
                skipped.add(label);
                continue;
            }
            String ak = def.getActionKey();
            boolean duplicate = valid.stream().anyMatch(m -> m.getActionKey().equalsIgnoreCase(ak));
            if (duplicate) {
                log.warn("Skipping custom command {}: actionKey '{}' is a duplicate of an already-loaded custom command", label, ak);
                skipped.add(label);
                continue;
            }
            valid.add(def);
            log.info("Loaded custom command: '{}' (actionKey={} id={})", def.getName(), ak, def.getId());
        }
        lastSkippedCount = skipped.size();
        lastSkippedLabels = Collections.unmodifiableList(skipped);
        return Collections.unmodifiableList(valid);
    }

    /**
     * Strips every trace of the withdrawn custom-command parameter feature from definitions written by
     * older versions, so their files still load.
     * <p>
     * Custom commands are keystroke sequences now and take no arguments. Parameters reached the stored
     * file in four places, and all four are handled here: {@code RUN_COMMAND} steps, {@code ${param}}
     * references in SPEAK text, {@code {name:type}} placeholders in trigger phrases, and the declared
     * {@code parameters} array. Each is removed and logged, so an upgrading player can see what changed
     * rather than finding it silently gone.
     */
    private static void migrateLegacyParameterUsage(JsonArray customCommands) {
        for (JsonElement element : customCommands) {
            if (!element.isJsonObject()) continue;
            JsonObject def = element.getAsJsonObject();
            dropDelegationSteps(def);
            stripPhrasePlaceholders(def);
            dropDeclaredParameters(def);
        }
    }

    /**
     * Drops {@code RUN_COMMAND} steps, which used to delegate to a built-in handler and pass it arguments.
     * A delegating step has no keystroke equivalent, and the built-in it targeted is directly addressable
     * by voice on its own. Left in place the step would deserialize to a null step type and take the whole
     * custom command down with it. A custom command left with no steps at all fails validation downstream
     * and is reported as skipped.
     */
    private static void dropDelegationSteps(JsonObject def) {
        JsonElement steps = def.get("steps");
        if (steps == null || !steps.isJsonArray()) return;

        JsonArray kept = new JsonArray();
        int dropped = 0;
        for (JsonElement stepElement : steps.getAsJsonArray()) {
            if (isDelegationStep(stepElement)) {
                dropped++;
                continue;
            }
            stripParamReferences(stepElement);
            kept.add(stepElement);
        }
        def.add("steps", kept);
        if (dropped > 0) {
            log.warn("Custom command '{}': dropped {} RUN_COMMAND step(s) - custom commands no longer delegate to"
                    + " built-in handlers. Speak the built-in command directly instead.", nameOf(def), dropped);
        }
    }

    private static boolean isDelegationStep(JsonElement stepElement) {
        if (!stepElement.isJsonObject()) return false;
        JsonElement type = stepElement.getAsJsonObject().get("type");
        return type != null && type.isJsonPrimitive() && "RUN_COMMAND".equalsIgnoreCase(type.getAsString());
    }

    /**
     * Strips now-unresolvable {@code ${param}} tokens from a SPEAK step's text.
     */
    private static void stripParamReferences(JsonElement stepElement) {
        if (!stepElement.isJsonObject()) return;
        JsonObject step = stepElement.getAsJsonObject();
        JsonElement text = step.get("text");
        if (text == null || !text.isJsonPrimitive()) return;

        String original = text.getAsString();
        String stripped = collapseWhitespace(PARAM_PLACEHOLDER.matcher(original).replaceAll(""));
        if (!stripped.equals(original)) {
            log.warn("Custom command SPEAK text contained parameter references that can no longer be resolved -"
                    + " speaking \"{}\" instead of \"{}\"", stripped, original);
            step.addProperty("text", stripped);
        }
    }

    /**
     * Strips {@code {name:type}} placeholders from trigger phrases, where they used to hint the LLM at the
     * values to extract. Nothing extracts values now, so a surviving placeholder would reach the model
     * verbatim inside the tool description. A phrase that was nothing but a placeholder is dropped from
     * the group rather than left as an empty trigger.
     */
    private static void stripPhrasePlaceholders(JsonObject def) {
        JsonElement phrases = def.get("phrases");
        if (phrases == null || !phrases.isJsonPrimitive()) return;

        String original = phrases.getAsString();
        String stripped = Arrays.stream(PHRASE_PLACEHOLDER.matcher(original).replaceAll("").split(","))
                .map(CustomCommandRepository::collapseWhitespace)
                .filter(phrase -> !phrase.isBlank())
                .collect(Collectors.joining(", "));
        if (!stripped.equals(original)) {
            log.warn("Custom command '{}': trigger phrases contained parameter placeholders that no longer mean"
                    + " anything - using \"{}\" instead of \"{}\"", nameOf(def), stripped, original);
            def.addProperty("phrases", stripped);
        }
    }

    /**
     * Removes the declared {@code parameters} array. Gson would ignore the unknown field anyway, but
     * removing it explicitly is what makes the warning possible - a silently dropped contract is exactly
     * what an upgrading player needs told.
     */
    private static void dropDeclaredParameters(JsonObject def) {
        JsonElement parameters = def.get("parameters");
        if (parameters == null || !parameters.isJsonArray() || parameters.getAsJsonArray().isEmpty()) {
            def.remove("parameters");
            return;
        }
        log.warn("Custom command '{}': dropped {} declared parameter(s) - custom commands are keystroke"
                + " sequences and take no arguments.", nameOf(def), parameters.getAsJsonArray().size());
        def.remove("parameters");
    }

    private static String collapseWhitespace(String value) {
        return value.replaceAll("\\s{2,}", " ").trim();
    }

    private static String nameOf(JsonObject def) {
        JsonElement name = def.get("name");
        return name != null && name.isJsonPrimitive() ? name.getAsString() : "(unnamed)";
    }

    /** Returns the best human-readable identifier for a customCommand: name if present, else id + actionKey. */
    private static String customCommandLabel(CustomCommandDefinition def) {
        String name = def.getName();
        if (name != null && !name.isBlank()) return name;
        String id = def.getId();
        String ak = def.getActionKey();
        if (ak != null && !ak.isBlank()) return (id != null ? id + " / " : "") + ak;
        return id != null ? id : "(unnamed)";
    }
}
