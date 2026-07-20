package elite.intel.diagnostics;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.model.execution.ExecutionRequest;
import elite.intel.ai.brain.vega.tools.SystemFunction;
import elite.intel.ai.brain.vega.tools.SystemFunctionRegistry;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Diagnostics-mode {@link ExecutionGateway}. Records every dispatched tool to {@link DiagnosticsLog} as a
 * {@code DIAG dispatch} marker (the signal the tester matches against the expected action) and, like
 * {@code CompanionRoutingHarness}, executes only companion system functions while game commands and queries are
 * merely recorded,
 * never run. So a diagnostics session drives the real routing path (reflex &rarr; reducer &rarr; companion
 * LLM) without pressing keys into the game or calling EDSM/Spansh.
 * <p>
 * Constructing this gateway means the companion execution path is wired, so it writes an informational
 * {@code DIAG companion-started} marker. It is NOT the readiness signal: the tester waits for {@code DIAG ready}
 * (emitted by {@link DiagnosticsLogWriter} once the LLM endpoint is confirmed reachable, i.e. all services up).
 */
public final class DiagnosticsExecutionGateway implements ExecutionGateway {

    private static final String STATUS = "status";
    private static final String STATUS_RECORDED = "recorded";

    private final Map<String, SystemFunction> systemFunctions;

    public DiagnosticsExecutionGateway() {
        SystemFunctionRegistry registry = SystemFunctionRegistry.getInstance();
        if (registry.byId().isEmpty()) {
            registry.load();
        }
        this.systemFunctions = registry.byId();
        DiagnosticsLog.write("DIAG companion-started");
    }

    @Override
    public CompletableFuture<JsonObject> submit(ExecutionRequest request) {
        String toolName = request.toolName();
        DiagnosticsLog.write("DIAG dispatch tool=" + toolName);
        DiagnosticsPacer.getInstance().markActivity();

        SystemFunction fn = systemFunctions.get(toolName);
        if (fn != null) {
            try {
                JsonObject result = fn.handle(toolName, request.arguments(), request.commanderInput());
                if (result != null) {
                    return CompletableFuture.completedFuture(result);
                }
            } catch (Exception ignored) {
                // a system-function failure must not abort the turn (mirrors the routing harness)
            }
        }
        // Commands and queries are recorded, not executed: no keystrokes, no third-party REST calls.
        JsonObject recorded = new JsonObject();
        recorded.addProperty(STATUS, STATUS_RECORDED);
        return CompletableFuture.completedFuture(recorded);
    }
}
