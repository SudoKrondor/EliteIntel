package elite.intel.companion;

import com.google.gson.JsonObject;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.execution.ExecutionGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the static holder publishes installed gateways and guards access before install / after clear.
 * The holder is process-global static state, so each test clears it afterwards.
 */
class CompanionGatewaysTest {

    private final SpeechGateway speech = (SpeechRequest r) -> CompletableFuture.completedFuture(null);
    private final ExecutionGateway execution = (ExecutionRequest r) -> CompletableFuture.completedFuture(new JsonObject());

    @AfterEach
    void clear() {
        CompanionGateways.clear();
    }

    @Test
    void installedGatewaysAreReturned() {
        CompanionGateways.install(null, speech, execution, null);

        assertSame(speech, CompanionGateways.speech());
        assertSame(execution, CompanionGateways.execution());
    }

    @Test
    void accessBeforeInstallThrows() {
        assertThrows(IllegalStateException.class, CompanionGateways::speech);
    }

    @Test
    void accessAfterClearThrows() {
        CompanionGateways.install(null, speech, execution, null);
        CompanionGateways.clear();

        assertThrows(IllegalStateException.class, CompanionGateways::speech);
    }
}
