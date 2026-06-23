package elite.intel.companion.input;

/**
 * Barge-in signal: the commander spoke over the companion (PTT or an interrupt phrase while TTS is
 * playing). The {@link BargeInController} fans it out as a split signal (§2.15) - a speech interrupt and a
 * thought interrupt. Published by the input layer when true barge-in is detected (STT); that publisher is
 * a later input-layer task.
 */
public final class BargeInEvent {
}
