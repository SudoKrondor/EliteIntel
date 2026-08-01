package elite.intel.ui.overlay;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        String line = OverlayProtocol.say("CMDR", "left\tright\nsecond", false);

        assertEquals("SAY\tCMDR\t0\tleft right second", line);
        assertEquals(4, line.split("\t", -1).length);
    }

    @Test
    void aiFlagSelectsTheSpeakerColour() {
        assertTrue(OverlayProtocol.say("Nomad", "hello", true).startsWith("SAY\tNomad\t1\t"));
        assertTrue(OverlayProtocol.say("CMDR", "hello", false).startsWith("SAY\tCMDR\t0\t"));
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
        String line = OverlayProtocol.say("Nomad", "x".repeat(5000), true);

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

    @Test
    void positionIsSentAsACfgTheOverlayAlreadyUnderstands() {
        assertEquals("CFG\tx=100\ty=200", OverlayProtocol.position(100, 200));
    }
}
