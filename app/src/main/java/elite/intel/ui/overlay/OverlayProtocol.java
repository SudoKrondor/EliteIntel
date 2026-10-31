package elite.intel.ui.overlay;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    /**
     * The whole text palette, e.g. {@code CFG col_primary=FF7100 col_success=4FC56B ...}.
     * <p>
     * Every role every time, not just the ones the commander changed: the overlay
     * has no "back to default" verb, and giving it one would mean two ways to say
     * the same thing. Sending the resolved palette means a reset is an ordinary
     * palette line carrying the shipped colours, and a child that starts late in
     * {@link HudDisplayMode#BOTH} is coloured by the same line as the first one.
     * <p>
     * One line rather than one per role, because the overlay reads at most 16
     * tab-separated fields per line and eight colours plus the verb is nine.
     */
    public static String colors(Map<HudOverlayColor, Color> palette) {
        StringBuilder line = new StringBuilder("CFG");
        for (HudOverlayColor role : HudOverlayColor.values()) {
            Color color = palette.getOrDefault(role, role.defaultColor());
            line.append(TAB).append("col_").append(role.wireName())
                    .append('=').append(HudOverlayColor.hex(color));
        }
        return line.toString();
    }

    /**
     * "No position stored." Deliberately not {@code -1}: a screen coordinate is legitimately negative on
     * both platforms - a monitor placed left of or above the primary one starts at a negative offset, and a
     * window nudged past the top or left edge sits at a negative coordinate on its own. While the sentinel
     * was {@code -1}, the guards that applied a stored position could not tell such a coordinate from
     * "unset", so an overlay parked at a negative x came back centred with only its y remembered.
     * <p>
     * Must match {@code HUD_POS_UNSET} in {@code overlay/src/hud.h}.
     */
    public static final int POSITION_UNSET = Integer.MIN_VALUE;

    /**
     * A window top-left corner stored before the overlay placed its card by section. The overlay places it as
     * given, then reports the section it landed in, which converts the stored row on first start.
     */
    public static String legacyPosition(int x, int y) {
        return "CFG" + TAB + "x=" + x + TAB + "y=" + y;
    }

    /**
     * Puts the card back where it was last left, e.g. {@code CFG restore=ne x=1720 y=40}. Sent at start.
     */
    public static String restore(HudPlacement placement) {
        return "CFG" + TAB + "restore=" + placement.section().wireName() + point(placement.anchor());
    }

    /**
     * Moves the card to a section of the monitor it is on now, e.g. {@code CFG move=ne x=1720 y=40}. The
     * point is where the commander last left the card in that section; the overlay uses it only if it lies
     * on that monitor, and its default spot for the section otherwise or when no point is sent.
     */
    public static String move(HudScreenSection section, Optional<Point> rememberedAnchor) {
        return "CFG" + TAB + "move=" + section.wireName() + rememberedAnchor.map(OverlayProtocol::point).orElse("");
    }

    private static String point(Point point) {
        return TAB + "x=" + point.x + TAB + "y=" + point.y;
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
     * Reads a {@code PLACE <section> <x> <y>} line: which section the card is pinned to and where its
     * anchor is, sent when a drag ends and after a move.
     * <p>
     * The commander drags the card with the mouse and only the overlay knows the monitor a section is
     * measured against, so this is the one piece of state the overlay owns and the app cannot derive.
     * Anything else on that stream is ignored.
     */
    public static Optional<HudPlacement> parsePlacement(String line) {
        if (line == null) return Optional.empty();
        String[] fields = line.split("\t", -1);
        if (fields.length != 4 || !"PLACE".equals(fields[0])) return Optional.empty();
        Optional<HudScreenSection> section = HudScreenSection.fromWireName(fields[1].trim());
        if (section.isEmpty()) return Optional.empty();
        try {
            Point anchor = new Point(Integer.parseInt(fields[2].trim()), Integer.parseInt(fields[3].trim()));
            return Optional.of(new HudPlacement(section.get(), anchor));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Who a conversation line is from, which is all the overlay needs to colour it.
     * <p>
     * The wire codes extend the {@code 0}/{@code 1} boolean this field used to carry rather than replacing it,
     * so an overlay binary built before radio existed reads {@link #RADIO} as a non-zero "is AI" and draws it
     * in the AI colour - which is exactly what it did before. That keeps the protocol version at 1: a stale
     * binary renders the old way instead of refusing to start.
     */
    public enum Speaker {
        COMMANDER(0),
        AI(1),
        RADIO(2);

        private final int code;

        Speaker(int code) {
            this.code = code;
        }

        int code() {
            return code;
        }
    }

    public static String say(String speaker, String text, Speaker kind) {
        return "SAY" + TAB + sanitize(speaker) + TAB + kind.code()
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
                        + TAB + state(row.state())
                        + (row.hasTrack() ? TAB + track(row.track()) : ""));
            }
        }
        out.add("END");
        return out;
    }

    /**
     * The stops of a track as one comma-separated field, e.g. {@code good,good,critical}.
     * <p>
     * One field rather than one per stop, and appended after the state rather than given a verb of its own,
     * because an overlay built before tracks existed reads a ROW's first four fields and ignores the rest: it
     * draws the plain label/value row it always did instead of losing the row.
     */
    private static String track(List<HudRow.State> stops) {
        return String.join(",", stops.stream().map(OverlayProtocol::state).toList());
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
