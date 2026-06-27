package elite.intel.ui.widget;

import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Canvas-rendered HUD readout that displays messages bottom-up with a typewriter animation.
 * Each entry is prefixed with a style-specific marker drawn in the style's accent color;
 * continuation lines indent to align with the message text, not the marker.
 * No title strip - intended to be placed inside a titled {@link HudSection}.
 */
public class HudLogArea extends JPanel {

    /**
     * Visual style variant controlling the marker glyph, its color, and the body text color.
     */
    public enum Style {
        /**
         * Pilot command input: {@code "} marker in muted orange; amber body text.
         */
        USER_INPUT("»", HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION, HudPalette.HUD_COLOR_ROLE_USER_INPUT_LOG_TEXT),
        /**
         * Ship-computer response stream: {@code "} marker in cyan; soft blue-grey body text.
         */
        AI_RESPONSE("»", HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, HudPalette.HUD_COLOR_ROLE_ASSISTANT_RESPONSE_LOG_TEXT),
        /**
         * System diagnostics readout: {@code -} marker in subdued gray; dim neutral-grey body text.
         */
        SYSTEM_LOG("·", HudPalette.HUD_COLOR_ROLE_DISABLED, HudPalette.HUD_COLOR_ROLE_SYSTEM_LOG_TEXT);

        final String marker;
        final Color markerColor;
        /**
         * Body text color for this role; SYSTEM_LOG timestamps use their own semantic alias.
         */
        final Color textColor;

        Style(String marker, Color markerColor, Color textColor) {
            this.marker = marker;
            this.markerColor = markerColor;
            this.textColor = textColor;
        }
    }

    private static final int MAX_MESSAGES = 20;
    private static final DateTimeFormatter LOG_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int PAD_X = 10;
    private static final int PAD_Y = 6;
    private static final int LINE_GAP = 4;
    private static final int SCROLLBAR_W = 5;
    private static final int MARKER_GAP = 4;

    private final Style style;
    private final List<Message> messages = new ArrayList<>();
    private BufferedImage offscreen;
    private final Timer typewriterTimer;
    /**
     * Non-null only for USER_INPUT; toggles {@link #caretVisible} to produce the blinking cursor.
     */
    private final Timer blinkTimer;
    private boolean caretVisible = false;
    private int scrollOffset = 0;
    private int totalContentHeight = 0;

    private static final class Message {
        final String fullText;
        /**
         * Number of leading characters drawn in timestamp color (0 = no prefix).
         */
        final int prefixLen;
        String visibleText = "";
        boolean complete = false;

        Message(String t) {
            this(t, 0);
        }

        Message(String t, int prefixLen) {
            this.fullText = t;
            this.prefixLen = prefixLen;
        }
    }

    /**
     * @param typewriterDelayMs milliseconds between typewriter character steps
     */
    public HudLogArea(int typewriterDelayMs) {
        this(typewriterDelayMs, Style.AI_RESPONSE);
    }

    /**
     * @param typewriterDelayMs milliseconds between typewriter character steps
     * @param style             visual marker style
     */
    public HudLogArea(int typewriterDelayMs, Style style) {
        this.style = style;
        setOpaque(true);
        setBackground(HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
        typewriterTimer = new Timer(typewriterDelayMs, null);
        if (style == Style.USER_INPUT) {
            blinkTimer = new Timer(530, e -> {
                caretVisible = !caretVisible;
                repaint();
            });
            blinkTimer.setRepeats(true);
        } else {
            blinkTimer = null;
        }
        addMouseWheelListener(e -> {
            int lineHeight = getFontMetrics(hudFont()).getHeight();
            // Negated: wheel-up -> older entries (higher scrollOffset), wheel-down -> newest/bottom
            scrollOffset -= (int) (e.getPreciseWheelRotation() * lineHeight * 3);
            clampScroll();
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (blinkTimer != null) blinkTimer.start();
    }

    @Override
    public void removeNotify() {
        if (blinkTimer != null) blinkTimer.stop();
        super.removeNotify();
    }

    /**
     * Appends a new message, fast-forwarding any in-progress animation.
     */
    public void addMessage(String text) {
        addMessageInternal(text, 0);
    }

    /**
     * Formats and appends a SYSTEM_LOG entry with a {@code HH:mm:ss} timestamp prefix.
     * The timestamp is rendered in {@link HudPalette#HUD_COLOR_ROLE_SYSTEM_LOG_TIMESTAMP_TEXT}.
     */
    public void addSystemLogEntry(LocalTime timestamp, String message) {
        String ts = timestamp.format(LOG_TIME_FMT);
        addMessageInternal(ts + "  " + message, ts.length() + 2);
    }

    private void addMessageInternal(String text, int prefixLen) {
        if (text == null || text.isBlank()) return;
        for (Message m : messages) {
            m.complete = true;
            m.visibleText = m.fullText;
        }
        typewriterTimer.stop();
        removeAllTimerListeners();
        scrollOffset = 0;
        Message msg = new Message(text, prefixLen);
        messages.add(msg);
        while (messages.size() > MAX_MESSAGES) messages.remove(0);
        startTypewriter(msg);
        repaint();
    }

    /**
     * Clears all messages.
     */
    public void clear() {
        typewriterTimer.stop();
        removeAllTimerListeners();
        messages.clear();
        offscreen = null;
        repaint();
    }

    private Font hudFont() {
        return getFont().deriveFont(HudPalette.HUD_FONT_LOG_PANEL);
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, totalContentHeight - getHeight());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void removeAllTimerListeners() {
        for (ActionListener al : typewriterTimer.getActionListeners()) {
            typewriterTimer.removeActionListener(al);
        }
    }

    private void startTypewriter(Message target) {
        typewriterTimer.addActionListener(e -> {
            if (target.complete) {
                target.visibleText = target.fullText;
                typewriterTimer.stop();
                paintImmediately(0, 0, getWidth(), getHeight());
                return;
            }
            int len = target.visibleText.length();
            if (len >= target.fullText.length()) {
                target.complete = true;
                target.visibleText = target.fullText;
                typewriterTimer.stop();
            } else {
                target.visibleText = target.fullText.substring(0, len + 1);
            }
            paintImmediately(0, 0, getWidth(), getHeight());
        });
        typewriterTimer.start();
    }

    /**
     * Returns true while the newest message is still being typewritten.
     */
    private boolean isAnimating() {
        return !messages.isEmpty() && !messages.get(messages.size() - 1).complete;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (offscreen == null || offscreen.getWidth() != w || offscreen.getHeight() != h) {
            offscreen = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D g2 = offscreen.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setColor(HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
        g2.fillRect(0, 0, w, h);

        Font font = hudFont();
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int markerW = fm.stringWidth(style.marker);
        int textX = PAD_X + markerW + MARKER_GAP;
        int maxW = w - textX - PAD_X - SCROLLBAR_W;

        // USER_INPUT keeps a virtual "bottom input row" pinned to the panel bottom at all times.
        //   idle:   the row shows the blinking " | prompt.
        //   active: the animating message occupies the row - its last wrapped line is pinned to
        //           the same baseline; additional lines from wrapping extend upward into history.
        // History messages always render above this input row.
        // For AI_RESPONSE / SYSTEM_LOG there is no input row (cursorZoneH = 0).
        boolean animating = style == Style.USER_INPUT && isAnimating();
        List<String> activeLines = null;
        if (animating) {
            activeLines = wrapText(messages.get(messages.size() - 1).visibleText, fm, maxW);
        }
        // Height of the bottom input zone: 1 idle row, or n rows if active message is wrapping.
        int nInputRows = (style == Style.USER_INPUT) ? (animating ? activeLines.size() : 1) : 0;
        int cursorZoneH = nInputRows > 0 ? nInputRows * fm.getHeight() + LINE_GAP : 0;

        // History = every message except the active one (excluded so it renders in the input row).
        int historyCount = animating ? messages.size() - 1 : messages.size();

        if (!messages.isEmpty()) {
            List<List<String>> wrappedHistory = new ArrayList<>();
            for (int i = 0; i < historyCount; i++) {
                Message m = messages.get(i);
                wrappedHistory.add(wrapText(m.complete ? m.fullText : m.visibleText, fm, maxW));
            }

            int newTotalH = PAD_Y + cursorZoneH;
            for (List<String> lines : wrappedHistory) {
                newTotalH += lines.size() * fm.getHeight() + LINE_GAP;
            }
            totalContentHeight = newTotalH;
            clampScroll();

            int y = h - PAD_Y - cursorZoneH + scrollOffset;
            for (int i = historyCount - 1; i >= 0; i--) {
                List<String> lines = wrappedHistory.get(i);
                int blockH = lines.size() * fm.getHeight();
                y -= blockH;
                if (y + blockH <= 0) break;

                int firstBaseline = y + fm.getAscent();
                g2.setColor(style.markerColor);
                g2.drawString(style.marker, PAD_X, firstBaseline);

                Message msg = messages.get(i);
                for (int li = 0; li < lines.size(); li++) {
                    int lineBaselineY = y + li * fm.getHeight() + fm.getAscent();
                    String lineText = lines.get(li);
                    if (style == Style.SYSTEM_LOG && li == 0 && msg.prefixLen > 0) {
                        // First line of a SYSTEM_LOG entry: timestamp in grey-blue, body in dim grey
                        int split = Math.min(msg.prefixLen, lineText.length());
                        String tsStr = lineText.substring(0, split);
                        String bodyStr = lineText.substring(split);
                        g2.setColor(HudPalette.HUD_COLOR_ROLE_SYSTEM_LOG_TIMESTAMP_TEXT);
                        g2.drawString(tsStr, textX, lineBaselineY);
                        if (!bodyStr.isEmpty()) {
                            g2.setColor(style.textColor);
                            g2.drawString(bodyStr, textX + fm.stringWidth(tsStr), lineBaselineY);
                        }
                    } else {
                        g2.setColor(style.textColor);
                        g2.drawString(lineText, textX, lineBaselineY);
                    }
                }

                // Typewriter cursor for AI_RESPONSE / SYSTEM_LOG newest animating message.
                // USER_INPUT's active message is rendered separately in the input row below.
                if (!animating && !msg.complete && i == messages.size() - 1) {
                    String lastLine = lines.get(lines.size() - 1);
                    int cx = textX + fm.stringWidth(lastLine);
                    int cy = y + (lines.size() - 1) * fm.getHeight();
                    if ((System.currentTimeMillis() / 500) % 2 == 0) {
                        drawCaret(g2, cx + 1, cy + fm.getAscent(), fm, style.textColor);
                    }
                }

                y -= LINE_GAP;
            }

            // Thin scrollbar when content overflows
            if (totalContentHeight > h) {
                int barX = w - SCROLLBAR_W - 1;
                int barTop = 2;
                int barH = h - 4;
                int thumbH = Math.max(16, (int) ((float) h / totalContentHeight * barH));
                int maxScroll = Math.max(1, totalContentHeight - h);
                int thumbY = barTop + (int) ((float) (maxScroll - scrollOffset) / maxScroll * (barH - thumbH));
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(barX, barTop, SCROLLBAR_W, barH, 3, 3);
                g2.setColor(new Color(255, 255, 255, scrollOffset > 0 ? 100 : 55));
                g2.fillRoundRect(barX, thumbY, SCROLLBAR_W, thumbH, 3, 3);
            }
        }

        // USER_INPUT: render the bottom input row.
        // The last text line is always pinned to inputRowTop so the row never jumps.
        if (style == Style.USER_INPUT) {
            int inputRowTop = h - PAD_Y - fm.getHeight();     // top of the pinned bottom line
            int inputBaseline = inputRowTop + fm.getAscent(); // baseline of the pinned bottom line

            if (animating) {
                int n = activeLines.size();
                // For multi-line wrap: first line sits (n-1) rows above the pinned bottom line.
                int inputBlockTop = inputRowTop - (n - 1) * fm.getHeight();

                g2.setColor(style.markerColor);
                g2.drawString(style.marker, PAD_X, inputBlockTop + fm.getAscent());

                for (int li = 0; li < n; li++) {
                    int lineBaselineY = inputBlockTop + li * fm.getHeight() + fm.getAscent();
                    g2.setColor(style.textColor);
                    g2.drawString(activeLines.get(li), textX, lineBaselineY);
                }

                // Typewriter cursor at end of the last active line (pinned bottom line)
                String lastLine = activeLines.get(n - 1);
                int cx = textX + fm.stringWidth(lastLine);
                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    drawCaret(g2, cx + 1, inputBaseline, fm, style.textColor);
                }
            } else {
                // Idle: blinking waiting-for-input prompt at the pinned bottom line.
                g2.setColor(style.markerColor);
                g2.drawString(style.marker, PAD_X, inputBaseline);
                if (caretVisible) {
                    int caretX = PAD_X + markerW + MARKER_GAP;
                    drawCaret(g2, caretX, inputBaseline, fm, HudPalette.HUD_COLOR_ROLE_USER_INPUT_LOG_TEXT);
                }
            }
        }

        g2.dispose();
        g.drawImage(offscreen, 0, 0, null);
    }

    /**
     * Draws the thin 2-px vertical caret shared by idle and typewriter cursor states.
     * Vertically aligned to the given baseline using the same +3 nudge as the idle prompt.
     */
    private void drawCaret(Graphics2D g2, int x, int baselineY, FontMetrics fm, Color color) {
        g2.setColor(color);
        g2.fillRect(x, baselineY - fm.getAscent() + 3, 2, fm.getAscent() - 3);
    }

    private static List<String> wrapText(String text, FontMetrics fm, int maxW) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            result.add("");
            return result;
        }
        String[] words = text.split(" ", -1);
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) <= maxW) {
                line = new StringBuilder(candidate);
            } else {
                if (!line.isEmpty()) result.add(line.toString());
                if (fm.stringWidth(word) > maxW) {
                    StringBuilder part = new StringBuilder();
                    for (char c : word.toCharArray()) {
                        if (fm.stringWidth(part + String.valueOf(c)) > maxW) {
                            result.add(part.toString());
                            part = new StringBuilder();
                        }
                        part.append(c);
                    }
                    line = part;
                } else {
                    line = new StringBuilder(word);
                }
            }
        }
        if (!line.isEmpty()) result.add(line.toString());
        return result;
    }
}
