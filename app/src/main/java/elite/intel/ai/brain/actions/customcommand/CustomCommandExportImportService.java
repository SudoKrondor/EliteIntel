package elite.intel.ai.brain.actions.customcommand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateless utilities for exporting and importing custom command definitions to/from JSON.
 * <p>
 * Export serializes a caller-supplied subset of commands to a pretty-printed JSON string.
 * Import parses a JSON string and classifies each entry as ready, conflict, or invalid
 * relative to the currently persisted commands.
 */
public final class CustomCommandExportImportService {

    private static final Logger log = LogManager.getLogger(CustomCommandExportImportService.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<CustomCommandDefinition>>() {}.getType();

    private CustomCommandExportImportService() {}

    /**
     * Serializes {@code commands} to a pretty-printed JSON string suitable for writing to an export file.
     */
    public static String toJson(List<CustomCommandDefinition> commands) {
        return GSON.toJson(commands);
    }

    /**
     * Parses a JSON export string and classifies each entry relative to {@code existing} commands.
     *
     * @param json     contents of a custom commands export file
     * @param existing currently persisted commands, used to detect actionKey conflicts
     * @return one {@link ImportCandidate} per array element in the file; never null
     * @throws IllegalArgumentException if the JSON is malformed or its top-level is not an array
     */
    public static List<ImportCandidate> parseImport(String json, List<CustomCommandDefinition> existing) {
        List<CustomCommandDefinition> parsed;
        try {
            parsed = GSON.fromJson(json, LIST_TYPE);
        } catch (JsonSyntaxException e) {
            log.warn("Failed to parse custom command import JSON: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
        if (parsed == null) {
            throw new IllegalArgumentException("File does not contain a JSON array");
        }

        List<ImportCandidate> candidates = new ArrayList<>(parsed.size());
        for (CustomCommandDefinition def : parsed) {
            candidates.add(classify(def, existing));
        }
        return candidates;
    }

    private static ImportCandidate classify(CustomCommandDefinition def, List<CustomCommandDefinition> existing) {
        if (def == null) {
            return new ImportCandidate(null, List.of("Entry is null"), false);
        }
        List<String> errors = new ArrayList<>();
        try {
            def.validate();
        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
        }
        if (errors.isEmpty()) {
            errors.addAll(CustomCommandValidator.validateFormat(def));
        }
        if (!errors.isEmpty()) {
            return new ImportCandidate(def, List.copyOf(errors), false);
        }
        boolean conflict = existing.stream()
                .anyMatch(e -> e.getActionKey().equalsIgnoreCase(def.getActionKey()));
        return new ImportCandidate(def, List.of(), conflict);
    }

    /**
     * A parsed candidate from an import file, classified for display in the import dialog.
     *
     * @param definition      the parsed definition; may be null if the source entry was null
     * @param validationErrors non-empty when the entry fails structural or format validation
     * @param hasConflict     true when an existing command has the same actionKey; always false
     *                        when validationErrors is non-empty
     */
    public record ImportCandidate(
            CustomCommandDefinition definition,
            List<String> validationErrors,
            boolean hasConflict
    ) {
        /** True when the entry passes all checks and may be selected for import. */
        public boolean isValid() {
            return validationErrors.isEmpty();
        }
    }
}
