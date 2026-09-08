package elite.intel.ui.overlay;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stored form of the overlay palette. It is one string in one column, read
 * back on every start, so a value this parser cannot make sense of has to cost
 * the commander that one colour and never the overlay.
 */
class HudOverlayColorTest {

    @Test
    void onlyTheRolesTheCommanderChangedAreStored() {
        Map<HudOverlayColor, Color> palette = new EnumMap<>(HudOverlayColor.class);
        palette.put(HudOverlayColor.RADIO, new Color(0x102030));
        palette.put(HudOverlayColor.AI, HudOverlayColor.AI.defaultColor());

        assertEquals("radio=102030", HudOverlayColor.formatOverrides(palette),
                "a role set back to its own default is not an override");
    }

    @Test
    void whatIsStoredIsWhatComesBack() {
        Map<HudOverlayColor, Color> palette = new EnumMap<>(HudOverlayColor.class);
        palette.put(HudOverlayColor.PRIMARY, new Color(0x00FF00));
        palette.put(HudOverlayColor.USER, new Color(0x000001));

        assertEquals(palette, HudOverlayColor.parseOverrides(HudOverlayColor.formatOverrides(palette)));
    }

    @Test
    void anEmptyPaletteIsTheShippedOne() {
        assertEquals("", HudOverlayColor.formatOverrides(new EnumMap<>(HudOverlayColor.class)));
        assertTrue(HudOverlayColor.parseOverrides("").isEmpty());
        assertTrue(HudOverlayColor.parseOverrides(null).isEmpty(),
                "a row written before the column existed reads back as null");
    }

    /**
     * A palette written by a newer build, then rolled back, names roles this one
     * has never heard of. Dropping just those keeps every colour the commander
     * chose that this build can still draw.
     */
    @Test
    void anUnreadableEntryCostsOnlyItsOwnColor() {
        Map<HudOverlayColor, Color> parsed =
                HudOverlayColor.parseOverrides("ai=72A2B4;cockpit=FFFFFF;user=GGGGGG;radio=102030;danger");

        assertEquals(Map.of(HudOverlayColor.AI, new Color(0x72A2B4),
                        HudOverlayColor.RADIO, new Color(0x102030)),
                parsed);
    }

    @Test
    void colorsTravelAsSixHexDigits() {
        assertEquals("000000", HudOverlayColor.hex(Color.BLACK));
        assertEquals("FFFFFF", HudOverlayColor.hex(Color.WHITE));
        // The alpha byte java.awt.Color carries has nowhere to go on the wire:
        // the overlay draws its text opaque and the background owns transparency.
        assertEquals("102030", HudOverlayColor.hex(new Color(0x10, 0x20, 0x30, 0x40)));
    }
}
