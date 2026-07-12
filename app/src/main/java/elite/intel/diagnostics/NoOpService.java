package elite.intel.diagnostics;

import elite.intel.ui.controller.ManagedService;

/**
 * A {@link ManagedService} that does nothing, used to disable live game-file monitors in diagnostics mode.
 * <p>
 * The file-driven harness owns {@link elite.intel.session.Status} and the game-event stream itself: context is
 * set deterministically via {@code @visible}/{@code @status} lines and game events are injected via {@code @event}
 * JSON lines (the {@code EventRegistry} → {@code GameEventBus} path). Left running, the real monitors fight that
 * control - {@code AuxiliaryFilesMonitor} re-reads the game's stale {@code Status.json} every 120 ms and overwrites
 * the {@code @visible}-set flags, so any command gated on {@code isVisibleForLLM} (e.g. HUD mode switches) silently
 * drops out of routing. Swapping those services for this no-op (mirroring the {@link DiagnosticsEars} STT stub)
 * makes the harness's context authoritative, exactly as the routing test's isolated harness is.
 */
public final class NoOpService implements ManagedService {

    @Override
    public void start() {
        // Intentionally does nothing: the diagnostics harness drives game state, not a live monitor.
    }

    @Override
    public void stop() {
        // Nothing to stop.
    }
}
