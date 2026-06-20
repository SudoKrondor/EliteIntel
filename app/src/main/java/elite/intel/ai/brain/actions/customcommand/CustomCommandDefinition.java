package elite.intel.ai.brain.actions.customcommand;

import elite.intel.ai.brain.actions.ActionParameterSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A user-defined customCommand: a named sequence of steps triggered by voice phrases.
 * Gson populates fields directly; call {@link #validate()} after deserialization.
 * <p>
 * Identity model:
 * <ul>
 *   <li>{@code id} – immutable UUID generated at creation; used as stable storage identity only.</li>
 *   <li>{@code actionKey} – human-readable routing token used by the LLM, handler map, and logs;
 *       editable via the custom command editor.</li>
 * </ul>
 */
public final class CustomCommandDefinition {

    private final String id;
    /** LLM-facing action token; editable, unique, validated by {@link CustomCommandValidator}. */
    private final String actionKey;
    private final String name;
    private final String description;
    /**
     * Comma-separated trigger phrases in the same format as alias provider keys,
     * e.g. {@code "navigate and set speed, go to coordinates at speed"}.
     * Phrases may include parameter placeholders such as {@code {lat:number, lon:number}} as
     * LLM training hints; these are parsed by {@code ActionParameterKeyExtractor} for type inference
     * but are not evaluated or substituted at custom command execution time.
     */
    private final String phrases;
    /** Optional parameter contract; may be null for parameterless customCommands (backward-compatible). */
    private final List<ActionParameterSpec> parameters;
    private final List<CustomCommandStep> steps;

    /**
     * Primary constructor for editor-created customCommands.
     *
     * @param id        immutable UUID; used only as stable storage identity
     * @param actionKey LLM-facing routing token; editable, must be unique among customCommands
     */
    public CustomCommandDefinition(String id, String actionKey, String name, String description, String phrases,
                           List<ActionParameterSpec> parameters, List<CustomCommandStep> steps) {
        this.id = id;
        this.actionKey = actionKey;
        this.name = name;
        this.description = description;
        this.phrases = phrases;
        this.parameters = parameters == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(parameters));
        this.steps = steps == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(steps));
    }

    /**
     * Backward-compatible constructor: {@code actionKey} defaults to {@code id}.
     * Prefer the 7-arg constructor for new code.
     */
    public CustomCommandDefinition(String id, String name, String description, String phrases,
                           List<ActionParameterSpec> parameters, List<CustomCommandStep> steps) {
        this(id, id, name, description, phrases, parameters, steps);
    }

    /**
     * Creates a parameterless custom command definition. Equivalent to passing {@code null} for parameters.
     */
    public CustomCommandDefinition(String id, String name, String description, String phrases, List<CustomCommandStep> steps) {
        this(id, id, name, description, phrases, null, steps);
    }

    @SuppressWarnings("unused")
    private CustomCommandDefinition() {
        id = null;
        actionKey = null;
        name = null;
        description = null;
        phrases = null;
        parameters = null;
        steps = null;
    }

    /** Validates all required fields and delegates to each parameter's and step's own validation. */
    public void validate() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("CustomCommand id is blank");
        }
        if (getActionKey() == null || getActionKey().isBlank()) {
            throw new IllegalArgumentException("CustomCommand '" + id + "': actionKey is blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("CustomCommand '" + id + "': name is blank");
        }
        if (phrases == null || phrases.isBlank()) {
            throw new IllegalArgumentException("CustomCommand '" + id + "': phrases is blank");
        }
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("CustomCommand '" + id + "': steps list is empty");
        }
        if (parameters != null) {
            for (ActionParameterSpec param : parameters) {
                if (param == null) {
                    throw new IllegalArgumentException("CustomCommand '" + id + "': parameter entry is null");
                }
                param.validate();
            }
        }
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i) == null) {
                throw new IllegalArgumentException("CustomCommand '" + id + "': step " + i + " is null");
            }
            steps.get(i).validate(i);
        }
    }

    /**
     * Returns the ordered list of distinct binding IDs used by {@link CustomCommandStep.Type#BINDING_TAP}
     * and {@link CustomCommandStep.Type#BINDING_HOLD} steps. {@code DELAY}, {@code SPEAK}, and
     * {@code RUN_COMMAND} steps are excluded. Duplicate binding IDs appear only once, in
     * first-occurrence order.
     */
    public List<String> distinctBindingIds() {
        if (steps == null) return List.of();
        return steps.stream()
                .filter(s -> s.getType() == CustomCommandStep.Type.BINDING_TAP
                          || s.getType() == CustomCommandStep.Type.BINDING_HOLD)
                .map(CustomCommandStep::getBindingId)
                .filter(bid -> bid != null && !bid.isBlank())
                .distinct()
                .toList();
    }

    public String getId() { return id; }
    /**
     * Returns the LLM-facing action token used for routing, handler lookup, and prompt output.
     * Older persisted customCommands did not store {@code actionKey}; those fall back to {@code id}.
     */
    public String getActionKey() { return actionKey != null && !actionKey.isBlank() ? actionKey : id; }
    public String getName() { return name; }
    /** Returns description or empty string if not set. */
    public String getDescription() { return description != null ? description : ""; }
    public String getPhrases() { return phrases; }
    /** Returns the parameter contract. Empty list means this customCommand has no declared parameters. */
    public List<ActionParameterSpec> getParameters() { return parameters != null ? parameters : List.of(); }
    public List<CustomCommandStep> getSteps() { return steps == null ? List.of() : steps; }
}
