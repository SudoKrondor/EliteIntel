package elite.intel.companion;

import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.speech.SpeechGateway;

/**
 * Static access point to the running companion subsystem, so self-describing tools (system functions
 * implemented as {@code IntelAction}) can reach the gateways, the action reducer, and the shared runtime
 * state ({@link CompanionState}) from their no-arg {@code handle} the same way legacy handlers reach their
 * singletons - without the {@code ExecutionGateway} having to inject anything into them.
 * <p>
 * Lifecycle: {@code CompanionSubsystemGate.start()} {@link #install}s the graph's instances and
 * {@link #clear()}s them on stop. Getters throw if accessed while the subsystem is not running, which is a
 * programming error (a tool ran outside companion mode), not a recoverable condition.
 */
public final class CompanionRuntime {

    private static volatile LlmGateway llm;
    private static volatile SpeechGateway speech;
    private static volatile ExecutionGateway execution;
    private static volatile MemoryGateway memory;
    private static volatile CompanionActionReducer reducer;
    private static volatile CompanionState state;

    private CompanionRuntime() {
    }

    /** Publishes the active companion services and state; called once when the subsystem starts. */
    public static void install(LlmGateway llm, SpeechGateway speech, ExecutionGateway execution,
                               MemoryGateway memory, CompanionActionReducer reducer, CompanionState state) {
        CompanionRuntime.llm = llm;
        CompanionRuntime.speech = speech;
        CompanionRuntime.execution = execution;
        CompanionRuntime.memory = memory;
        CompanionRuntime.reducer = reducer;
        CompanionRuntime.state = state;
    }

    /** Clears the references when the companion subsystem stops. */
    public static void clear() {
        llm = null;
        speech = null;
        execution = null;
        memory = null;
        reducer = null;
        state = null;
    }

    public static LlmGateway llm() {
        return require(llm, "LLM");
    }

    public static SpeechGateway speech() {
        return require(speech, "speech");
    }

    public static ExecutionGateway execution() {
        return require(execution, "execution");
    }

    public static MemoryGateway memory() {
        return require(memory, "memory");
    }

    public static CompanionActionReducer reducer() {
        return require(reducer, "reducer");
    }

    public static CompanionState state() {
        return require(state, "state");
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalStateException("Companion " + name + " is not installed (subsystem not running)");
        }
        return value;
    }
}
