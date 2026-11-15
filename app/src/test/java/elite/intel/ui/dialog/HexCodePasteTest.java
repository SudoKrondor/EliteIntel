package elite.intel.ui.dialog;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HexCodePasteTest {

    @Test
    void aColorCodeIsReadTheWaysPeopleCopyIt() {
        assertEquals(Optional.of("10141C"), HexCodePaste.colorCode("#10141C"));
        assertEquals(Optional.of("10141C"), HexCodePaste.colorCode("  10141c \n"));
        assertEquals(Optional.of("10141C"), HexCodePaste.colorCode("0x10141C"));
        assertEquals(Optional.of("33D7E8"), HexCodePaste.colorCode("#A633D7E8"), "a theme file's alpha is dropped");
        assertEquals(Optional.empty(), HexCodePaste.colorCode("orange"));
        assertEquals(Optional.empty(), HexCodePaste.colorCode("#1014"));
    }

    /** The JDK field refused a pasted "#10141C" without a sound: now it takes it, and the chooser follows. */
    @Test
    void aPastedCodeWithAHashReachesTheChooser() {
        JColorChooser chooser = new JColorChooser(new Color(0x33D7E8));
        HexCodePaste.install(chooser);
        JFormattedTextField field = codeField(chooser);
        TransferHandler handler = field.getTransferHandler();

        boolean pasted = handler.importData(new TransferHandler.TransferSupport(field, new StringSelection("#10141C")));

        assertTrue(pasted);
        assertEquals("10141C", field.getText());
        assertEquals(0x10141C, chooser.getColor().getRGB() & 0xFFFFFF);
    }

    @Test
    void textThatIsNoColorIsRefused() {
        JColorChooser chooser = new JColorChooser(new Color(0x33D7E8));
        HexCodePaste.install(chooser);
        JFormattedTextField field = codeField(chooser);

        assertFalse(field.getTransferHandler().importData(
                new TransferHandler.TransferSupport(field, new StringSelection("not a colour"))));
    }

    private static JFormattedTextField codeField(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JFormattedTextField field && field.getColumns() == 6) return field;
            if (child instanceof Container nested) {
                JFormattedTextField found = codeField(nested);
                if (found != null) return found;
            }
        }
        return null;
    }
}
