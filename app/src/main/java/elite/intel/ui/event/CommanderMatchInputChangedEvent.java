package elite.intel.ui.event;

import elite.intel.companion.model.GameStateSnapshot;

/** Published when the companion freezes a commander's match input and visibility state for action reduction. */
public record CommanderMatchInputChangedEvent(String text, GameStateSnapshot gameStateSnapshot) {}
