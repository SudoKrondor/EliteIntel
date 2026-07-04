package elite.intel.ui.screen;

/**
 * Holds transient UI state that survives panel rebuilds.
 * The controller or screen manager owns an instance of this class and passes it to each AiTabPanel.
 */
public class AiUiState {
    private Boolean llmConnected = null;

    public Boolean getLlmConnected() { return llmConnected; }
    public void setLlmConnected(Boolean val) { this.llmConnected = val; }
}
