package elite.intel.ui.overlay;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The overlay is a C process parsing these lines by splitting on tabs, so a
 * formatting slip here surfaces only as a visual oddity on screen. These tests
 * pin the format described in overlay/PROTOCOL.md.
 */
class OverlayProtocolTest {

    @Test
    void objectiveIsAnAtomicBlockEndingInEnd() {
        HudObjective objective = new HudObjective("m:1", "MASSACRE", "KREMAINN",
                List.of(HudRow.progress("PIRATES", 12, 20),
                        HudRow.of("REWARD", "4,120,000 cr")));

        List<String> lines = OverlayProtocol.objective(objective);

        assertEquals("OBJ\tMASSACRE\tKREMAINN", lines.get(0));
        assertEquals("BAR\tPIRATES\t12\t20\tnormal", lines.get(1));
        assertEquals("ROW\tREWARD\t4,120,000 cr\tnormal", lines.get(2));
        assertEquals("END", lines.get(3), "the overlay only swaps the card on END");
    }

    @Test
    void aMissingSubtitleStillEmitsItsField() {
        // The C side splits on tabs by position; dropping the field would shift
        // every later one.
        List<String> lines = OverlayProtocol.objective(
                new HudObjective("m:1", "TITLE", null, List.of()));

        assertEquals("OBJ\tTITLE\t", lines.get(0));
    }

    @Test
    void tabsAndNewlinesInTextAreNeutralised() {
        // LLM replies and commander speech are arbitrary text; an embedded tab
        // would silently shift the fields after it.
        String line = OverlayProtocol.say("CMDR", "left\tright\nsecond", OverlayProtocol.Speaker.COMMANDER);

        assertEquals("SAY\tCMDR\t0\tleft right second", line);
        assertEquals(4, line.split("\t", -1).length);
    }

    @Test
    void theSpeakerKindSelectsTheLineColour() {
        assertTrue(OverlayProtocol.say("CMDR", "hello", OverlayProtocol.Speaker.COMMANDER)
                .startsWith("SAY\tCMDR\t0\t"));
        assertTrue(OverlayProtocol.say("Nomad", "hello", OverlayProtocol.Speaker.AI)
                .startsWith("SAY\tNomad\t1\t"));
        assertTrue(OverlayProtocol.say("Jameson Memorial traffic control", "hello", OverlayProtocol.Speaker.RADIO)
                .startsWith("SAY\tJameson Memorial traffic control\t2\t"));
    }

    /**
     * The codes extend the 0/1 flag this field used to be rather than replacing it, which is what lets radio
     * ship without a protocol version bump: an overlay built before radio existed reads 2 as a non-zero
     * "is AI" and draws the line in the AI colour, exactly as it did before.
     */
    @Test
    void radioStaysTruthyForAnOverlayThatOnlyKnowsTheOldFlag() {
        String line = OverlayProtocol.say("Jameson Memorial traffic control", "hello",
                OverlayProtocol.Speaker.RADIO);

        int legacyFlag = Integer.parseInt(line.split("\t", -1)[2]);
        assertTrue(legacyFlag != 0, "an old binary must still read radio as \"not the commander\"");
        assertEquals(1, OverlayProtocol.VERSION, "extending the field must not need a new protocol version");
    }

    @Test
    void configUsesFixedPointSoAtofNeverSeesExponents() {
        String cfg = OverlayProtocol.config(0.000012, 1.0, 760);

        assertFalse(cfg.contains("E"), cfg);
        assertFalse(cfg.contains("e-"), cfg);
        assertEquals("CFG\talpha=0.000\tscale=1.000\twidth=760", cfg);
    }

    @Test
    void statesMapToTheNamesTheRendererParses() {
        List<String> lines = OverlayProtocol.objective(new HudObjective("i", "T", "", List.of(
                HudRow.of("A", "1", HudRow.State.GOOD),
                HudRow.of("B", "2", HudRow.State.WARN),
                HudRow.of("C", "3", HudRow.State.CRITICAL))));

        assertTrue(lines.get(1).endsWith("\tgood"), lines.get(1));
        assertTrue(lines.get(2).endsWith("\twarn"), lines.get(2));
        assertTrue(lines.get(3).endsWith("\tcritical"), lines.get(3));
    }

    @Test
    void handshakeCarriesTheVersionTheRendererChecks() {
        assertEquals("V\t" + OverlayProtocol.VERSION, OverlayProtocol.handshake());
    }

    /**
     * The reader drops a line it cannot fit, so an unbounded reply would not be
     * truncated on screen, it would be missing from it.
     */
    @Test
    void aLongReplyIsTrimmedRatherThanLeftForTheReaderToDrop() {
        String line = OverlayProtocol.say("Nomad", "x".repeat(5000), OverlayProtocol.Speaker.AI);

        assertTrue(line.getBytes(StandardCharsets.UTF_8).length < 1024, "line stays inside the reader's budget");
        assertTrue(line.endsWith("…"), "the commander can see it was cut short");
    }

    @Test
    void trimmingNeverSplitsACharacter() {
        // Cyrillic is two bytes per character, so a byte-counted cut lands mid
        // character unless it backs off - which would hand the renderer invalid
        // UTF-8 and draw a broken glyph.
        String trimmed = OverlayProtocol.clamp("я".repeat(2000), 101);

        assertEquals(trimmed, new String(trimmed.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertTrue(trimmed.getBytes(StandardCharsets.UTF_8).length <= 101);
        assertTrue(trimmed.endsWith("…"));
        assertFalse(trimmed.contains("�"), "no replacement character means no split sequence");
    }

    @Test
    void textInsideTheBudgetIsUntouched() {
        assertEquals("short enough", OverlayProtocol.clamp("short enough", 900));
    }

    @Test
    void aDraggedPositionComesBackUpThePipe() {
        assertEquals(new Point(1920, 40), OverlayProtocol.parsePosition("POS\t1920\t40").orElseThrow());
        assertEquals(new Point(-8, -8), OverlayProtocol.parsePosition("POS\t-8\t-8").orElseThrow(),
                "a window dragged partly off-screen still reports where it is");
    }

    /**
     * The overlay's stdout is a stream from another process, so the reader has to
     * survive anything on it - a startup banner from a library, a warning, a
     * newer binary reporting something this build does not know.
     */
    @Test
    void anythingElseOnThatStreamIsIgnored() {
        assertTrue(OverlayProtocol.parsePosition(null).isEmpty());
        assertTrue(OverlayProtocol.parsePosition("").isEmpty());
        assertTrue(OverlayProtocol.parsePosition("Gtk-WARNING: something").isEmpty());
        assertTrue(OverlayProtocol.parsePosition("POS\t12").isEmpty(), "a short line is not a position");
        assertTrue(OverlayProtocol.parsePosition("POS\tleft\ttop").isEmpty(), "non-numeric is not a position");
        assertTrue(OverlayProtocol.parsePosition("SIZE\t10\t20").isEmpty(), "an unknown verb is not a position");
    }

    /**
     * The overlay announces which shell came up - desktop or VR - and why, when
     * VR was asked for and could not be had. That line shares the stdout the
     * position reports travel on, and the three-field form has exactly the shape
     * a position has, so it is the one thing on that stream most likely to be
     * misread as one. Nothing moves the overlay window on a fallback.
     */
    @Test
    void aModeReportIsNotAPosition() {
        assertTrue(OverlayProtocol.parsePosition("MODE\tdesktop").isEmpty());
        assertTrue(OverlayProtocol.parsePosition("MODE\tvr").isEmpty());
        assertTrue(OverlayProtocol.parsePosition("MODE\tdesktop\tno headset detected").isEmpty());
    }

    @Test
    void positionIsSentAsACfgTheOverlayAlreadyUnderstands() {
        assertEquals("CFG\tx=100\ty=200", OverlayProtocol.position(100, 200));
    }

    @Test
    void theVrPlacementTravelsAsAName() {
        assertEquals("CFG\tvrpos=bottom_right", OverlayProtocol.vrPosition(HudVrPosition.BOTTOM_RIGHT));
        assertEquals("CFG\tvrpos=top", OverlayProtocol.vrPosition(HudVrPosition.TOP));
    }

    /**
     * Both ends keep their own list of placements, and the C side resolves a name
     * to a direction by its position in that list. A name added on one side only,
     * or added in a different order, would silently hang the HUD somewhere the
     * commander did not pick - and only a commander wearing a headset would ever
     * see it. So the two lists are compared here rather than trusted to stay in
     * step.
     */
    @Test
    void everyPlacementNameIsOneTheOverlayKnows() {
        List<String> inTheOverlay = vrPositionNamesFromTheCSide();

        assertEquals(
                Arrays.stream(HudVrPosition.values()).map(HudVrPosition::wireName).toList(),
                inTheOverlay,
                "VR_POSITION_NAMES in overlay/src/hud_model.c must match HudVrPosition, in order");
    }

    @Test
    void theWholePaletteTravelsOnOneLine() {
        String line = OverlayProtocol.colors(Map.of(HudOverlayColor.RADIO, new Color(0x102030)));

        String[] fields = line.split("\t", -1);
        assertEquals("CFG", fields[0]);
        assertEquals(HudOverlayColor.values().length + 1, fields.length,
                "every role is sent every time, so a reset needs no verb of its own");
        // hud_handle_command splits a line into at most 16 fields and keeps the
        // rest of the line in the last one, so a palette that outgrew that would
        // arrive as a colour with several colours glued onto it.
        assertTrue(fields.length <= 16, "a CFG line is read as at most 16 fields");
        assertTrue(line.contains("\tcol_radio=102030"), line);
    }

    @Test
    void aRoleTheCommanderNeverTouchedIsSentAtItsDefault() {
        String line = OverlayProtocol.colors(Map.of());

        for (HudOverlayColor role : HudOverlayColor.values()) {
            assertTrue(line.contains("\tcol_" + role.wireName() + "="
                            + HudOverlayColor.hex(role.defaultColor())),
                    role + " missing from " + line);
        }
    }

    /**
     * The same standing hazard as the placements above, one layer down: the C
     * side matches a colour key by name and falls back to its own compiled-in
     * table for anything it does not recognise. A role renamed on one side only
     * would leave that colour silently stuck at the default, with nothing logged
     * and only the commander's own eyes to catch it.
     */
    @Test
    void everyColorRoleIsOneTheOverlayKnows() {
        assertEquals(
                Arrays.stream(HudOverlayColor.values()).map(HudOverlayColor::wireName).toList(),
                namesFromTheCSide("HUD_COLOR_NAMES[HUD_COL_COUNT]", cSource("hud_model.c")),
                "HUD_COLOR_NAMES in overlay/src/hud_model.c must match HudOverlayColor, in order");
    }

    /**
     * Both ends also carry their own copy of the shipped palette - the binary
     * needs one for the runs where no app is driving it - so the two can drift
     * apart into a HUD that changes colour the moment the app connects to it.
     */
    @Test
    void theShippedPaletteIsTheSameOnBothSides() {
        String header = cSource("hud.h");
        for (HudOverlayColor role : HudOverlayColor.values()) {
            String macro = "HUD_COL_DEFAULT_" + role.name();
            Matcher value = Pattern.compile("#define\\s+" + macro + "\\s+0x([0-9A-Fa-f]{6})").matcher(header);
            assertTrue(value.find(), macro + " has gone from overlay/src/hud.h");
            assertEquals(HudOverlayColor.hex(role.defaultColor()), value.group(1).toUpperCase(Locale.ROOT),
                    macro + " must match " + role + " in HudOverlayColor");
        }
    }

    private static String cSource(String name) {
        Path source = Stream.of(Path.of("overlay/src/" + name), Path.of("../overlay/src/" + name))
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "overlay/src/" + name + " not found from " + Path.of("").toAbsolutePath()));
        try {
            return Files.readString(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("cannot read " + source, e);
        }
    }

    /**
     * The quoted names of a C string table, in declaration order.
     */
    private static List<String> namesFromTheCSide(String declaration, String text) {
        int start = text.indexOf(declaration);
        assertTrue(start > 0, declaration + " has gone from the overlay sources");
        String table = text.substring(text.indexOf('{', start), text.indexOf('}', start));

        List<String> names = new ArrayList<>();
        Matcher quoted = Pattern.compile("\"([a-z_]+)\"").matcher(table);
        while (quoted.find()) names.add(quoted.group(1));
        return names;
    }

    private static List<String> vrPositionNamesFromTheCSide() {
        Path source = Stream.of(Path.of("overlay/src/hud_model.c"), Path.of("../overlay/src/hud_model.c"))
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "overlay/src/hud_model.c not found from " + Path.of("").toAbsolutePath()));

        String table;
        try {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            int start = text.indexOf("VR_POSITION_NAMES[]");
            assertTrue(start > 0, "VR_POSITION_NAMES has gone from " + source);
            table = text.substring(text.indexOf('{', start), text.indexOf('}', start));
        } catch (IOException e) {
            throw new AssertionError("cannot read " + source, e);
        }

        List<String> names = new ArrayList<>();
        Matcher quoted = Pattern.compile("\"([a-z_]+)\"").matcher(table);
        while (quoted.find()) names.add(quoted.group(1));
        return names;
    }
}
