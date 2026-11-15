package elite.intel.ui.dialog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Lets the colour chooser's colour code field take a colour pasted the way people copy one: {@code #10141C},
 * {@code 10141c}, with spaces around it, or as {@code #AARRGGBB} from a theme file.
 * <p>
 * WHY it is needed: the JDK's field takes only hex digits, at most six, through a document filter that refuses
 * anything else without a sound. A pasted {@code #10141C} was refused for its {@code #}, and a code pasted without
 * first selecting the old one was refused for being twelve digits long - so paste seemed not to work at all. Copy is
 * left as the field had it.
 */
final class HexCodePaste extends TransferHandler {

    private static final Logger log = LogManager.getLogger(HexCodePaste.class);

    private static final Pattern HEX_CODE = Pattern.compile("(?:#|0x)?([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})",
            Pattern.CASE_INSENSITIVE);
    private static final int CODE_DIGITS = 6;

    private final JFormattedTextField field;
    private final TransferHandler original;

    private HexCodePaste(JFormattedTextField field) {
        this.field = field;
        this.original = field.getTransferHandler();
    }

    /** Fits every colour code field in {@code chooser} with the forgiving paste. */
    static void install(JColorChooser chooser) {
        installIn(chooser);
    }

    /**
     * The six-digit colour code in {@code text}, upper case, or empty when it holds none. An eight-digit
     * {@code AARRGGBB} gives its colour and drops its alpha, which the chooser does not edit.
     */
    static Optional<String> colorCode(String text) {
        if (text == null) return Optional.empty();
        var matcher = HEX_CODE.matcher(text.strip());
        if (!matcher.matches()) return Optional.empty();
        String digits = matcher.group(1);
        return Optional.of(digits.substring(digits.length() - CODE_DIGITS).toUpperCase(Locale.ROOT));
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        Optional<String> code = pastedText(support).flatMap(HexCodePaste::colorCode);
        if (code.isEmpty()) {
            UIManager.getLookAndFeel().provideErrorFeedback(field);
            return false;
        }
        field.setText(code.get());
        try {
            field.commitEdit();
        } catch (ParseException e) {
            log.error("The colour chooser refused the colour code {}", code.get(), e);
            return false;
        }
        return true;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return original == null ? NONE : original.getSourceActions(c);
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        if (original != null) original.exportToClipboard(comp, clip, action);
    }

    private static Optional<String> pastedText(TransferSupport support) {
        try {
            return Optional.ofNullable((String) support.getTransferable().getTransferData(DataFlavor.stringFlavor));
        } catch (UnsupportedFlavorException | IOException e) {
            log.error("Could not read the pasted colour code", e);
            return Optional.empty();
        }
    }

    /** WHY by width: the chooser's panels are the JDK's own, and the colour code field is the one six columns wide. */
    private static void installIn(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JFormattedTextField field && field.getColumns() == CODE_DIGITS) {
                field.setTransferHandler(new HexCodePaste(field));
            }
            if (child instanceof Container nested) installIn(nested);
        }
    }
}
