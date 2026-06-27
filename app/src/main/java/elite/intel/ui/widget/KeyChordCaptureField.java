package elite.intel.ui.widget;

import elite.intel.ai.hands.BindingModifier;
import elite.intel.ui.theme.AppTheme;
import elite.intel.util.KeyCaptureMapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A focusable HUD control that captures a full keyboard chord by listening for an
 * actual key press, rather than offering drop-downs.
 * <p>
 * When armed (the user clicks it) the field installs a {@link KeyEventDispatcher}
 * that swallows every key event for the owning window, so capturable keys such as
 * Enter and Tab reach the chord builder instead of triggering the dialog's default
 * button or focus traversal. Modifier keys (Ctrl/Shift/Alt, left/right distinct via
 * {@link KeyCaptureMapper}) accumulate in press order; the first non-modifier key
 * finalises the chord and disarms the field. Escape cancels capture without change.
 * <p>
 * The widget is layout- and modifier-aware purely through {@link KeyCaptureMapper};
 * it holds no game-binding policy of its own beyond "modifiers then one main key".
 */
public class KeyChordCaptureField extends JPanel {

    /**
     * One captured chord: zero or more keyboard modifiers plus exactly one main key token.
     */
    public record CapturedChord(List<BindingModifier> modifiers, String key) {
        public CapturedChord {
            modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
        }
    }

    private final JButton surface;
    private final String pressPrompt;
    private final Function<String, String> tokenLabeler;
    private final Consumer<CapturedChord> onCapture;

    private final LinkedHashSet<String> heldModifiers = new LinkedHashSet<>();
    private KeyEventDispatcher dispatcher;
    private boolean armed;
    private String committedText;

    /**
     * @param idleText     text shown when not capturing (e.g. the current value)
     * @param pressPrompt  text shown while armed and waiting for the chord
     * @param tokenLabeler maps an Elite key token (e.g. {@code Key_LeftControl}) to a human label
     * @param onCapture    invoked with the finalised chord on the EDT
     */
    public KeyChordCaptureField(
            String idleText,
            String pressPrompt,
            Function<String, String> tokenLabeler,
            Consumer<CapturedChord> onCapture
    ) {
        super(new BorderLayout());
        this.pressPrompt = pressPrompt;
        this.tokenLabeler = Objects.requireNonNull(tokenLabeler, "tokenLabeler");
        this.onCapture = Objects.requireNonNull(onCapture, "onCapture");
        this.committedText = idleText;
        setOpaque(false);

        this.surface = AppTheme.makeButton(idleText);
        this.surface.addActionListener(e -> toggleArmed());
        this.surface.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                cancelCapture();
            }
        });
        add(surface, BorderLayout.CENTER);
    }

    /**
     * Replaces the displayed value (used to reflect an externally set chord, e.g. on Clear).
     */
    public void showIdleText(String text) {
        this.committedText = text;
        if (!armed) {
            surface.setText(text);
        }
    }

    private void toggleArmed() {
        if (armed) {
            cancelCapture();
        } else {
            arm();
        }
    }

    private void arm() {
        if (armed) {
            return;
        }
        armed = true;
        heldModifiers.clear();
        renderArmedPreview();
        dispatcher = this::dispatchKeyEvent;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
    }

    private void cancelCapture() {
        if (!armed) {
            return;
        }
        disarm();
        surface.setText(committedText);
    }

    private void disarm() {
        armed = false;
        heldModifiers.clear();
        if (dispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
            dispatcher = null;
        }
    }

    @Override
    public void removeNotify() {
        // Never leave a global dispatcher installed once the field is gone.
        disarm();
        super.removeNotify();
    }

    private boolean dispatchKeyEvent(KeyEvent e) {
        if (!armed) {
            return false;
        }
        switch (e.getID()) {
            case KeyEvent.KEY_PRESSED -> handlePress(e);
            case KeyEvent.KEY_RELEASED -> handleRelease(e);
            default -> {
                // consume typed events too
            }
        }
        e.consume();
        return true;
    }

    private void handlePress(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            cancelCapture();
            return;
        }
        if (KeyCaptureMapper.isModifierOnly(e)) {
            KeyCaptureMapper.fromKeyEvent(e).ifPresent(heldModifiers::add);
            renderArmedPreview();
            return;
        }
        Optional<String> mainKey = KeyCaptureMapper.fromKeyEvent(e);
        if (mainKey.isEmpty()) {
            // Key with no Elite token (e.g. an OS key); ignore but stay armed.
            return;
        }
        finalizeChord(mainKey.get());
    }

    private void handleRelease(KeyEvent e) {
        if (KeyCaptureMapper.isModifierOnly(e)) {
            KeyCaptureMapper.fromKeyEvent(e).ifPresent(heldModifiers::remove);
            renderArmedPreview();
        }
    }

    private void finalizeChord(String mainKey) {
        // Capture text and chord before disarm() clears the held modifiers.
        CapturedChord chord = chordOf(heldModifiers, mainKey);
        String text = renderChord(heldModifiers, tokenLabeler.apply(mainKey));
        disarm();
        surface.setText(text);
        onCapture.accept(chord);
    }

    private void renderArmedPreview() {
        // "…" stands in for the still-awaited main key.
        surface.setText(heldModifiers.isEmpty() ? pressPrompt : renderChord(heldModifiers, "…"));
    }

    private String renderChord(Collection<String> modifierTokens, String tailLabel) {
        List<String> parts = new ArrayList<>();
        for (String token : modifierTokens) {
            parts.add(tokenLabeler.apply(token));
        }
        parts.add(tailLabel);
        return String.join(" + ", parts);
    }

    /**
     * Builds a chord from ordered keyboard modifier tokens plus one main key token.
     * Package-private and pure so the assembly can be unit-tested without AWT.
     */
    static CapturedChord chordOf(Collection<String> modifierTokens, String mainKey) {
        List<BindingModifier> modifiers = modifierTokens.stream()
                .map(token -> new BindingModifier("Keyboard", token))
                .toList();
        return new CapturedChord(modifiers, mainKey);
    }
}
