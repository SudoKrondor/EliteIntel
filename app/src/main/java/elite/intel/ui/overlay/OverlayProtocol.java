package elite.intel.ui.overlay;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Formats overlay commands as protocol lines. See {@code overlay/PROTOCOL.md}.
 * <p>
 * Kept free of process and I/O concerns so the wire format can be tested
 * directly - the two ends live in different languages, so a formatting mistake
 * would otherwise only show up as a rendering oddity on screen.
 */
public final class OverlayProtocol {

    /**
     * Bumped whenever the format changes; the overlay exits if it disagrees.
     */
    public static final int VERSION = 1;

    private static final char TAB = '\t';

    /**
     * Longest spoken text a line may carry, in UTF-8 bytes.
     * <p>
     * The renderer keeps 1024 bytes per line ({@code MAX_TEXT} in hud.h) and
     * discards the rest, so anything past this was never going to be displayed.
     * Bounding it here also keeps a whole protocol line well inside the reader's
     * 8 KB line buffer, which is the difference between a long AI reply being
     * trimmed and it being dropped.
     */
    private static final int MAX_SAY_BYTES = 900;

    /**
     * Marks text the commander can see was cut short rather than ended.
     */
    private static final String ELLIPSIS = "…";

    private OverlayProtocol() {
    }

    public static String handshake() {
        return "V" + TAB + VERSION;
    }

    public static String quit() {
        return "QUIT";
    }

    public static String clearObjective() {
        return "CLR";
    }

    /**
     * Background alpha in [0,1] and font scale, e.g. {@code CFG alpha=0.25 scale=1.0}.
     */
    public static String config(double alpha, double scale, int width) {
        return "CFG" + TAB + "alpha=" + trim(alpha)
                + TAB + "scale=" + trim(scale)
                + TAB + "width=" + width;
    }

    public static String position(int x, int y) {
        return "CFG" + TAB + "x=" + x + TAB + "y=" + y;
    }

    /**
     * Where the card hangs in the headset, e.g. {@code CFG vrpos=bottom_right}.
     * <p>
     * Sent to every child, not only a VR one: the desktop shells ignore keys
     * that mean nothing to them, and which child is which is not something this
     * layer tracks.
     */
    public static String vrPosition(HudVrPosition position) {
        return "CFG" + TAB + "vrpos=" + position.wireName();
    }

    /**
     * Reads a {@code POS x y} line, the one thing the overlay sends back.
     * <p>
     * The window is dragged with the mouse, so its position is the only piece of
     * state the overlay owns and the app cannot derive. Anything else on that
     * stream is ignored, which is what lets an older binary that reports nothing,
     * and a newer one that reports more, both work against this reader.
     */
    public static Optional<Point> parsePosition(String line) {
        if (line == null) return Optional.empty();
        String[] fields = line.split("\t", -1);
        if (fields.length != 3 || !"POS".equals(fields[0])) return Optional.empty();
        try {
            return Optional.of(new Point(Integer.parseInt(fields[1].trim()), Integer.parseInt(fields[2].trim())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static String say(String speaker, String text, boolean ai) {
        return "SAY" + TAB + sanitize(speaker) + TAB + (ai ? "1" : "0")
                + TAB + clamp(sanitize(text), MAX_SAY_BYTES);
    }

    /**
     * Trims text to a byte budget without splitting a character.
     * <p>
     * Byte-counted rather than character-counted because both limits downstream
     * are byte limits, and a Cyrillic or accented reply is roughly twice the
     * bytes per character of an English one - counting characters would let
     * exactly those languages overrun. Cutting mid-sequence is not an option
     * either: the renderer would be handed an invalid UTF-8 tail and draw a
     * broken glyph.
     */
    static String clamp(String value, int maxBytes) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) return value;

        int end = maxBytes - ELLIPSIS.getBytes(StandardCharsets.UTF_8).length;
        // Back off any continuation byte (10xxxxxx) so the cut lands on a
        // character boundary rather than inside one.
        while (end > 0 && (bytes[end] & 0xC0) == 0x80) end--;
        return new String(bytes, 0, Math.max(0, end), StandardCharsets.UTF_8) + ELLIPSIS;
    }

    /**
     * The objective card as an atomic OBJ/ROW/BAR/END block. The overlay only
     * swaps the visible card on END, so it never renders a half-built card.
     */
    public static List<String> objective(HudObjective objective) {
        List<String> out = new ArrayList<>();
        out.add("OBJ" + TAB + sanitize(objective.title())
                + TAB + sanitize(objective.subtitle() == null ? "" : objective.subtitle()));
        for (HudRow row : objective.rows()) {
            if (row.hasProgress()) {
                out.add("BAR" + TAB + sanitize(row.label())
                        + TAB + row.current() + TAB + row.max() + TAB + state(row.state()));
            } else {
                out.add("ROW" + TAB + sanitize(row.label())
                        + TAB + sanitize(row.value() == null ? "" : row.value())
                        + TAB + state(row.state()));
            }
        }
        out.add("END");
        return out;
    }

    private static String state(HudRow.State state) {
        return switch (state) {
            case GOOD -> "good";
            case WARN -> "warn";
            case CRITICAL -> "critical";
            case NORMAL -> "normal";
        };
    }

    /**
     * Fields are tab-delimited and the overlay does not unescape, so any tab or
     * newline in user/LLM text would silently shift every later field. Replace
     * them here, at the only place that builds the wire format.
     */
    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    /**
     * Compact fixed-point, so the C side's atof() never sees scientific notation.
     */
    private static String trim(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
