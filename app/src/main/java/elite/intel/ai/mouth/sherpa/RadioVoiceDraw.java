package elite.intel.ai.mouth.sherpa;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * How a local engine picks the voice on the other end of a comms link, for any cast.
 * <p>
 * A transmission is a stranger, so it draws from the whole cast - male and female, every accent - and never
 * the commander's own voice, so the two speakers never sound like the same person. A speaker the commander
 * will hear again keeps one voice. The rules are the same for every sherpa engine; only the cast differs,
 * which is why the enums ({@code KokoroVoices}, {@code SupertonicVoices}) hand their {@code values()} in
 * here rather than each carrying a copy of the walk.
 */
public final class RadioVoiceDraw {

    private RadioVoiceDraw() {
    }

    /**
     * A random voice for the next transmission, skipping the commander's own and any voice reserved for a
     * named speaker - a carrier whose traffic control the commander has given a voice. Recognising that
     * voice is the whole point of assigning it, and a passing station answering in it takes that away.
     *
     * @param ownVoiceName the commander's ship voice (an enum name), or {@code null} when none is resolvable
     * @param reserved     enum names spoken for elsewhere; an empty pool falls back to ignoring them, because
     *                     a cast with one usable voice must still be able to say something
     * @param fallback     the voice when the commander's own is the whole cast
     */
    public static <E extends Enum<E>> E random(E[] cast, E fallback, String ownVoiceName, Set<String> reserved) {
        E[] pool = Arrays.stream(cast)
                .filter(voice -> !voice.name().equals(ownVoiceName))
                .filter(voice -> reserved == null || !reserved.contains(voice.name()))
                .toArray(size -> Arrays.copyOf(cast, size));
        if (pool.length > 0) {
            return pool[ThreadLocalRandom.current().nextInt(pool.length)];
        }
        return reserved == null || reserved.isEmpty() ? fallback : random(cast, fallback, ownVoiceName, Set.of());
    }

    /**
     * A voice for a speaker the commander is going to hear again: the same speaker always draws the same
     * voice, for as long as the cast holds it.
     * <p>
     * A pirate is named on every line they transmit ("Dave Knowles" over three quarters of the named chatter
     * in a two-month journal sample), so drawing afresh each time makes one attacker sound like a crowd -
     * indistinguishable, mid-fight, from several attackers. Deriving the voice from the name instead costs
     * nothing to store, survives a restart, and holds across a whole encounter without anyone tracking when
     * an encounter began or ended.
     * <p>
     * The commander's own voice and any voice reserved for a carrier are still skipped, but by walking on to
     * the next speaker in the cast rather than picking again: that keeps every other speaker on the voice they
     * already had when a carrier is given one mid-session. An unnamed speaker - a transmission with nobody
     * attributed to it - is a stranger, and still draws at random.
     *
     * @param speaker who is transmitting, as the game names them; null or blank draws at random
     */
    public static <E extends Enum<E>> E forSpeaker(E[] cast, E fallback, String speaker, String ownVoiceName,
                                                   Set<String> reserved) {
        if (speaker == null || speaker.isBlank()) return random(cast, fallback, ownVoiceName, reserved);
        int start = Math.floorMod(speaker.trim().toLowerCase(Locale.ROOT).hashCode(), cast.length);
        for (int step = 0; step < cast.length; step++) {
            E candidate = cast[(start + step) % cast.length];
            if (candidate.name().equals(ownVoiceName)) continue;
            if (reserved != null && reserved.contains(candidate.name())) continue;
            return candidate;
        }
        // Everyone in the cast is spoken for. Reserving is best-effort, exactly as it is for the random draw.
        return random(cast, fallback, ownVoiceName, reserved);
    }
}
