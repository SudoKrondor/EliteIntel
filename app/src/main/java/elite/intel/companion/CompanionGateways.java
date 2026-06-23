package elite.intel.companion;

import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.speech.SpeechGateway;

/**
 * Static access point to the active companion gateways, so self-describing tools (system functions
 * implemented as {@code IntelAction}) can reach the speech/memory/execution/LLM doors from their no-arg
 * {@code handle} the same way legacy handlers reach their singletons - without the {@code ExecutionGateway}
 * having to inject anything into them.
 * <p>
 * Lifecycle: {@code CompanionSubsystemGate.start()} {@link #install} the graph's instances and
 * {@link #clear()}s them on stop. Getters throw if accessed while the subsystem is not running, which is
 * a programming error (a tool ran outside companion mode), not a recoverable condition.
 */
public final class CompanionGateways {

    private static volatile LlmGateway llm;
    private static volatile SpeechGateway speech;
    private static volatile ExecutionGateway execution;
    private static volatile MemoryGateway memory;

    private CompanionGateways() {
    }

    /** Publishes the active gateway instances; called once when the companion subsystem starts. */
    public static void install(LlmGateway llm, SpeechGateway speech, ExecutionGateway execution, MemoryGateway memory) {
        CompanionGateways.llm = llm;
        CompanionGateways.speech = speech;
        CompanionGateways.execution = execution;
        CompanionGateways.memory = memory;
    }

    /** Clears the references when the companion subsystem stops. */
    public static void clear() {
        llm = null;
        speech = null;
        execution = null;
        memory = null;
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

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalStateException("Companion " + name + " gateway is not installed (subsystem not running)");
        }
        return value;
    }
}
