package elite.intel.ai.brain.vega.mind;

/** Session observer state that is not part of routing decisions or memory records. */
public final class CompanionState {

    private volatile String lastCommanderMatchInput = "";

    /** Latest normalized commander input for UI refresh and diagnostics only. */
    public String lastCommanderMatchInput() {
        return lastCommanderMatchInput;
    }

    /** Updates the observer snapshot without changing an already-created thought. */
    public void setLastCommanderMatchInput(String input) {
        lastCommanderMatchInput = input == null ? "" : input;
    }
}
