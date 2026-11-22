package elite.intel.ai.mouth.sherpa;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Cuts an overlong sentence into clause-sized pieces, so the voice starts sooner.
 *
 * <p>WHY: a local engine renders a whole queue entry before a sample of it plays, and the pipeline only
 * overlaps one entry's synthesis with the previous entry's playback. A sentence is normally the right
 * entry - short, and the engine phrases its own commas better than a queue gap does. But an LLM sometimes
 * writes one sentence that is a whole paragraph: the reported station announcement ran a faction clause
 * into a list of fifteen services behind one semicolon, and the commander waited for all of it to render
 * before hearing "Admiral". Past {@link #LONG_SENTENCE} characters the sentence is cut at its commas,
 * semicolons and colons, and the pieces packed back up to about {@link #TARGET_PIECE} characters, so the
 * first piece plays while the rest render.
 *
 * <p>A clause with no punctuation in it is never cut, however long: a cut mid-clause is heard as a stumble.
 * A comma with no space after it ({@code 1,000}, {@code 0,22}) is a number, not a clause, and is left alone.
 */
final class ClauseChunker {

    /** Longer than this and the sentence is cut. An ordinary spoken sentence stays under it, and stays whole. */
    static final int LONG_SENTENCE = 120;

    /** What the pieces of a long sentence are packed up to. */
    static final int TARGET_PIECE = 80;

    /**
     * Shorter than this and a piece joins its neighbour: a lone "Admiral," between two gaps sounds clipped,
     * and costs a queue boundary for nothing.
     */
    static final int MIN_PIECE = 20;

    private static final Pattern CLAUSE_END = Pattern.compile("(?<=[,;:])\\s+(?=\\S)");

    private ClauseChunker() {
    }

    /** The sentence as queue entries: itself when it is not long, its packed clauses when it is. */
    static List<String> chunk(String sentence) {
        if (sentence.length() <= LONG_SENTENCE) {
            return List.of(sentence);
        }
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String clause : CLAUSE_END.split(sentence)) {
            if (current.isEmpty()) {
                current.append(clause);
            } else if (current.length() < MIN_PIECE || current.length() + 1 + clause.length() <= TARGET_PIECE) {
                current.append(' ').append(clause);
            } else {
                pieces.add(current.toString());
                current.setLength(0);
                current.append(clause);
            }
        }
        if (current.length() < MIN_PIECE && !pieces.isEmpty()) {
            pieces.set(pieces.size() - 1, pieces.getLast() + " " + current);
        } else {
            pieces.add(current.toString());
        }
        return pieces;
    }
}
