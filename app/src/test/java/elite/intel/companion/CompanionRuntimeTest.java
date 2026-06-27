package elite.intel.companion;

import com.google.gson.JsonObject;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.speech.SpeechGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the static runtime holder publishes installed services/state and guards access before install /
 * after clear. Process-global static state, so each test clears it afterwards.
 */
class CompanionRuntimeTest {

    private final SpeechGateway speech = (SpeechRequest r) -> CompletableFuture.completedFuture(null);
    private final ExecutionGateway execution = (ExecutionRequest r) -> CompletableFuture.completedFuture(new JsonObject());
    private final CompanionActionReducer reducer = (categories, input) -> List.of();
    private final CompanionState state = new CompanionState();

    @AfterEach
    void clear() {
        CompanionRuntime.clear();
    }

    @Test
    void installedServicesAreReturned() {
        CompanionRuntime.install(null, speech, execution, null, reducer, state);

        assertSame(speech, CompanionRuntime.speech());
        assertSame(execution, CompanionRuntime.execution());
        assertSame(reducer, CompanionRuntime.reducer());
        assertSame(state, CompanionRuntime.state());
    }

    @Test
    void accessBeforeInstallThrows() {
        assertThrows(IllegalStateException.class, CompanionRuntime::state);
    }

    @Test
    void accessAfterClearThrows() {
        CompanionRuntime.install(null, speech, execution, null, reducer, state);
        CompanionRuntime.clear();

        assertThrows(IllegalStateException.class, CompanionRuntime::reducer);
    }
}
