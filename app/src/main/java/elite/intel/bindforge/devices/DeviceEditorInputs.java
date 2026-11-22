package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.ButtonMapLabels;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The inputs the Device Editor offers a label box for, the label each box starts with, and what SAVE writes
 * (alias-designer.md, <em>the Device Editor</em>).
 * <p>
 * <strong>Built from what the controller reports, never a fixed layout.</strong> An attached controller offers its
 * buttons, the axes it has - by which axis they are, read in A8b - and four directions per hat. An unattached one
 * cannot report anything, so it offers the inputs its saved labels name. Where the axes cannot be read, or are
 * counted other than SDL counts them, all eight are offered, as {@code .buttonMap} generation labels them (Alan,
 * 2026-10-10).
 * <p>
 * <strong>Every box holds a label.</strong> One with nothing saved starts at the default - the generated labels,
 * so the dialog and the generated files agree - and clearing a box puts the default back.
 * <p>
 * Pure, so the rules can be tested without a screen or a controller.
 *
 * @param attached        whether the inputs came from the controller rather than from its saved labels
 * @param axesKnown       whether the axes are the controller's own; when not, all eight are offered and none can
 *                        be lit by identity
 * @param axesWithoutSlot how many of the controller's axes the game cannot see, for the editor to say so
 * @param axes            the axis tokens, in the game's order
 * @param buttons         the button tokens, in the game's numbering
 * @param hats            how many hats, each with four direction tokens
 */
public record DeviceEditorInputs(boolean attached, boolean axesKnown, int axesWithoutSlot, List<String> axes,
                                 List<String> buttons, int hats) {

    /** The game shows at most this many characters of a label - confirmed in game. */
    public static final int MAX_LABEL_LENGTH = 18;

    private static final Pattern BUTTON = Pattern.compile("Joy_(\\d+)");
    private static final Pattern AXIS = Pattern.compile("Joy_(X|Y|Z|RX|RY|RZ|U|V)Axis");
    private static final Pattern HAT = Pattern.compile("Joy_POV(\\d+)(Up|Right|Down|Left)");

    /**
     * The most hats a controller can report to the game: DirectInput's joystick state carries four POVs
     * ({@code DIJOYSTATE2.rgdwPOV[4]}), the game's {@code Joy_POV1} to {@code Joy_POV4}. A saved label naming a higher
     * one - hand-edited, or from a damaged file - names nothing the game can bind, and must not build a box for every
     * hat up to it (review, 2026-10-11).
     */
    static final int MAX_HATS = 4;

    public DeviceEditorInputs {
        axes = List.copyOf(axes);
        buttons = List.copyOf(buttons);
        if (hats < 0 || axesWithoutSlot < 0) throw new IllegalArgumentException("Counts cannot be negative");
    }

    /**
     * The inputs of a controller plugged in now.
     *
     * @param axisCount how many axes SDL reports, checked against the description
     */
    public static DeviceEditorInputs ofController(int buttonCount, int axisCount, DeviceDescription description) {
        boolean known = description.agreesWith(axisCount);
        List<String> axes = ButtonMapLabels.axesToLabel(axisCount, description).stream().map(GameAxis::token).toList();
        List<String> buttons = new ArrayList<>();
        for (int button = 1; button <= buttonCount; button++) buttons.add(ButtonMapLabels.buttonToken(button));
        return new DeviceEditorInputs(true, known, known ? description.axesWithoutSlot() : 0, axes, buttons,
                description.hatCount());
    }

    /**
     * The inputs a controller that is not attached is known to have: those its saved labels name. A hat is offered
     * whole - all four directions - when any of its directions is labelled, and every hat up to the highest one, up to
     * {@link #MAX_HATS}; a higher one is kept and written out unchanged, like a half-axis label. Half-axis labels
     * ({@code Pos_}, {@code Neg_}) name no box: they are kept and written out unchanged.
     */
    public static DeviceEditorInputs ofSavedLabels(Map<String, String> saved) {
        TreeSet<Integer> buttons = new TreeSet<>();
        TreeSet<GameAxis> axes = new TreeSet<>();
        int hats = 0;
        for (String token : saved.keySet()) {
            Matcher matcher;
            if ((matcher = BUTTON.matcher(token)).matches()) {
                buttonNumber(matcher.group(1)).ifPresent(buttons::add);
            } else if ((matcher = AXIS.matcher(token)).matches()) {
                axes.add(GameAxis.valueOf(matcher.group(1)));
            } else if ((matcher = HAT.matcher(token)).matches()) {
                int hat = buttonNumber(matcher.group(1)).orElse(0);
                if (hat <= MAX_HATS) hats = Math.max(hats, hat);
            }
        }
        return new DeviceEditorInputs(false, true, 0,
                axes.stream().map(GameAxis::token).toList(),
                buttons.stream().map(ButtonMapLabels::buttonToken).toList(),
                hats);
    }

    private static Optional<Integer> buttonNumber(String digits) {
        try {
            int number = Integer.parseInt(digits);
            return number >= 1 ? Optional.of(number) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** The four direction tokens of hat {@code hat}, 1-based, up, right, down, left. */
    public static List<String> hatTokens(int hat) {
        return ButtonMapLabels.HAT_DIRECTIONS.stream().map(direction -> ButtonMapLabels.hatToken(hat, direction))
                .toList();
    }

    /** Every token a box is offered for: axes, buttons, then hats. */
    public List<String> tokens() {
        List<String> tokens = new ArrayList<>(axes);
        tokens.addAll(buttons);
        for (int hat = 1; hat <= hats; hat++) tokens.addAll(hatTokens(hat));
        return tokens;
    }

    public boolean isEmpty() {
        return axes.isEmpty() && buttons.isEmpty() && hats == 0;
    }

    /** What each box starts with: the saved label, or the default where there is none. */
    public Map<String, String> startingLabels(Map<String, String> saved) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (String token : tokens()) {
            String label = saved.get(token);
            labels.put(token, label == null || label.isBlank() ? defaultLabel(token) : label);
        }
        return labels;
    }

    /**
     * The generated label for a token - {@code Button 14}, {@code RZ Axis}, {@code Hat 1 Up} - or the token itself
     * for one the editor has no box for.
     */
    public static String defaultLabel(String token) {
        Matcher matcher;
        if ((matcher = BUTTON.matcher(token)).matches()) {
            return buttonNumber(matcher.group(1)).map(ButtonMapLabels::buttonLabel).orElse(token);
        }
        if ((matcher = AXIS.matcher(token)).matches()) {
            return ButtonMapLabels.axisLabel(GameAxis.valueOf(matcher.group(1)));
        }
        if ((matcher = HAT.matcher(token)).matches()) {
            String direction = matcher.group(2);
            return buttonNumber(matcher.group(1)).map(hat -> ButtonMapLabels.hatLabel(hat, direction)).orElse(token);
        }
        return token;
    }

    /**
     * A box's text as it is kept: spaces at either end trimmed, cut to {@link #MAX_LABEL_LENGTH}, and the default
     * when nothing is left.
     */
    public static String clean(String token, String typed) {
        String label = typed == null ? "" : typed.strip();
        if (label.length() > MAX_LABEL_LENGTH) label = label.substring(0, MAX_LABEL_LENGTH).strip();
        return label.isEmpty() ? defaultLabel(token) : label;
    }

    /**
     * The labels SAVE writes for the device - its whole set.
     * <ul>
     *     <li><strong>Every box</strong>, defaults included: once a device has a {@code .buttonMap}, APPLY's default
     *     labels skip it ({@code Apply.DefaultLabels}), so a default not written would never reach the game, which
     *     would show a raw
     *     {@code Joy_14} (Alan, 2026-10-10).</li>
     *     <li><strong>Except on a device the game draws itself</strong> - {@code <SupportsIcons>}, {@code <GamePad>}
     *     - where a box is written only when it already had a saved label or was changed from the default, so a
     *     {@code Button 3} never replaces an icon nobody touched (Alan, 2026-10-10).</li>
     *     <li><strong>Every saved label with no box is kept</strong> - a half-axis label, or an input the controller
     *     does not report today.</li>
     * </ul>
     *
     * @param saved       the labels saved before the dialog opened
     * @param boxes       each box's text now, by token
     * @param gameDrawsIt whether the game draws this device's inputs itself
     */
    public static Map<String, String> toSave(Map<String, String> saved, Map<String, String> boxes,
                                             boolean gameDrawsIt) {
        Map<String, String> labels = new LinkedHashMap<>(saved);
        boxes.forEach((token, text) -> {
            String label = clean(token, text);
            if (!gameDrawsIt || saved.containsKey(token) || !label.equals(defaultLabel(token))) {
                labels.put(token, label);
            }
        });
        return labels;
    }

    /** Whether any box differs from what it started with, compared as SAVE would keep it. */
    public static boolean changed(Map<String, String> starting, Map<String, String> boxes) {
        for (Map.Entry<String, String> box : boxes.entrySet()) {
            String token = box.getKey();
            if (!Objects.equals(clean(token, box.getValue()), clean(token, starting.get(token)))) return true;
        }
        return false;
    }
}
