package elite.intel.companion.model.llm;

/**
 * Role of an {@link LlmMessage} in the OpenAI/Mistral chat protocol.
 */
public enum LlmMessageRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String wireValue;

    LlmMessageRole(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Protocol value as sent to the provider (e.g. "system"). */
    public String wireValue() {
        return wireValue;
    }
}
