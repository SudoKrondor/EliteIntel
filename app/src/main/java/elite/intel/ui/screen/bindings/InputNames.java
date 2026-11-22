package elite.intel.ui.screen.bindings;

import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * A {@code .buttonMap} input code in the player's words - {@code Joy_1} is <em>Button 1</em>, {@code Joy_POV2Up}
 * <em>Hat 2, up</em>, {@code Joy_RZAxis} <em>Z rotation axis</em> (multi-install-reconcile.md, <em>Button by
 * button</em>). A code it does not know is given back as it is.
 */
final class InputNames {

    private static final Pattern BUTTON = Pattern.compile("Joy_(\\d+)");
    private static final Pattern HAT = Pattern.compile("Joy_POV(\\d+)(Up|Down|Left|Right)");
    private static final Pattern AXIS = Pattern.compile("Joy_(R?)([XYZUV])Axis");
    private static final Pattern DIGITS = Pattern.compile("(\\d+)");

    private InputNames() {
    }

    /** Buttons in number order, then hats, then axes, then anything else by its code - the order a player counts in. */
    static final Comparator<String> ORDER = Comparator.comparingInt(InputNames::group)
            .thenComparingInt(InputNames::number).thenComparing(Comparator.naturalOrder());

    private static int group(String token) {
        if (BUTTON.matcher(token).matches()) return 0;
        if (HAT.matcher(token).matches()) return 1;
        if (AXIS.matcher(token).matches()) return 2;
        return 3;
    }

    private static int number(String token) {
        Matcher digits = DIGITS.matcher(token);
        return digits.find() ? Integer.parseInt(digits.group(1)) : 0;
    }

    static String spoken(String token) {
        Matcher button = BUTTON.matcher(token);
        if (button.matches()) return getText("bindings.reconcile.input.button", button.group(1));
        Matcher hat = HAT.matcher(token);
        if (hat.matches()) {
            return getText("bindings.reconcile.input.hat", hat.group(1),
                    getText("bindings.reconcile.input.hat." + hat.group(2).toLowerCase(Locale.ROOT)));
        }
        Matcher axis = AXIS.matcher(token);
        if (axis.matches()) {
            return getText(axis.group(1).isEmpty() ? "bindings.reconcile.input.axis"
                    : "bindings.reconcile.input.rotation", axis.group(2));
        }
        return token;
    }
}
