package elite.intel.ui.widget;

import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
        USER_INPUT("»", HudPalette.HUD_COLOR_ROLE_COMMANDER_MARKER, HudPalette.HUD_COLOR_ROLE_USER_INPUT_LOG_TEXT),
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

    private static final int MAX_MESSAGES = 40;
    /**
     * Per-message horizontal alignment, used by the chat panel to put commander lines on the left and
     * AI lines on the right. Non-chat panels always use {@link #LEFT}.
     */
    public enum Align {LEFT, RIGHT}

    /** Full-session transcript kept for export, independent of the {@link #MAX_MESSAGES}-message render window; bounded so it cannot grow without limit. */
    private static final int MAX_TRANSCRIPT = 5000;
    private static final int PAD_X = 10;
    private static final int PAD_Y = 6;
    private static final int LINE_GAP = 4;
    private static final int SCROLLBAR_W = 5;
    private static final int MARKER_GAP = 4;
    /** Chat bubbles wrap at this fraction of panel width so long lines don't span the whole panel. */
    private static final float CHAT_BUBBLE_MAX_FRACTION = 0.72f;
    /** Accent rail thickness on the outer edge of a chat card. */
    private static final int CHAT_RAIL_W = HudPalette.HUD_BORDER_THICKNESS_ACCENT;
    /** Gap between a card's rail and its text column. */
    private static final int CHAT_RAIL_TEXT_GAP = 10;
    /** Vertical gap between adjacent chat cards. */
    private static final int CHAT_CARD_GAP = 14;
    /** Gap between a card's timestamp line and its first text line. */
    private static final int CHAT_TS_GAP = 2;
    /** Arm length of the L-shaped corner brackets drawn around the active (speaking) AI card. */
    private static final int CHAT_BRACKET_ARM = 14;
    /** Alpha of a normal card's rail; the active AI card draws its rail fully opaque. */
    private static final int CHAT_RAIL_ALPHA = 150;
    /** Alpha of the active AI card's background highlight at the rail edge (fades to transparent). */
    private static final int CHAT_HIGHLIGHT_ALPHA = 31;

    private final Style style;
    /**
     * Chat mode merges commander + AI messages into one stream: each message carries its own
     * {@link Style}/{@link Align}, and a single blinking commander prompt is pinned to the bottom.
     */
    private final boolean chat;
    private final List<Message> messages = new ArrayList<>();
    /** Every line ever added this session (capped at {@link #MAX_TRANSCRIPT}), for {@link #exportText()}. */
    private final Deque<String> transcript = new ArrayDeque<>();
    private BufferedImage offscreen;
    private final Timer typewriterTimer;
    /**
     * Non-null only in chat mode; toggles {@link #caretVisible} to blink the commander prompt cursor.
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
        /** Rendering style for this entry (marker glyph + colors). */
        final Style style;
        /** Horizontal alignment for this entry. */
        final Align align;
        /** Wall-clock time the entry was added; rendered as the card timestamp in chat mode (null otherwise). */
        final Instant timestamp;
        String visibleText = "";
        boolean complete = false;

        Message(String t, int prefixLen, Style style, Align align, Instant timestamp) {
            this.fullText = t;
            this.prefixLen = prefixLen;
            this.style = style;
            this.align = align;
            this.timestamp = timestamp;
        }
    }

    /**
     * Plain top-down log panel (e.g. {@link Style#SYSTEM_LOG}); no chat cards or input row.
     *
     * @param typewriterDelayMs milliseconds between typewriter character steps
     * @param style             visual marker style
     */
    public HudLogArea(int typewriterDelayMs, Style style) {
        this(typewriterDelayMs, style, false);
    }

    /**
     * Chat-mode factory: a single stream that renders commander messages ({@link Style#USER_INPUT},
     * left) and AI messages ({@link Style#AI_RESPONSE}, right) added via
     * {@link #addMessage(String, Style, Align)}, with a blinking commander prompt pinned to the bottom.
     *
     * @param typewriterDelayMs milliseconds between typewriter character steps
     */
    public static HudLogArea chat(int typewriterDelayMs) {
        return new HudLogArea(typewriterDelayMs, Style.USER_INPUT, true);
    }

    private HudLogArea(int typewriterDelayMs, Style style, boolean chat) {
        this.style = style;
        this.chat = chat;
        setOpaque(true);
        setBackground(HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
        typewriterTimer = new Timer(typewriterDelayMs, null);
        if (chat) {
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
        addMessageInternal(text, 0, text, style, Align.LEFT, null);
    }

    /**
     * Chat-mode append: adds a message with an explicit {@link Style} and {@link Align} so a single
     * panel can interleave commander (left) and AI (right) lines with their own colors. The card
     * timestamp is stamped now, when the message is added.
     */
    public void addMessage(String text, Style style, Align align) {
        addMessageInternal(text, 0, text, style, align, Instant.now());
    }

    /**
     * Formats and appends a SYSTEM_LOG entry. The panel shows the local {@code HH:mm:ss} time (rendered in
     * {@link HudPalette#HUD_COLOR_ROLE_SYSTEM_LOG_TIMESTAMP_TEXT}); the exported transcript uses the UTC
     * {@code yyyy-MM-dd'T'HH:mm:ss'Z'} journal form so a saved log lines up with the running game journal.
     */
    public void addSystemLogEntry(Instant timestamp, String message) {
        String tsScreen = LogTimestampFormat.screen(timestamp);
        String tsFile = LogTimestampFormat.file(timestamp);
        addMessageInternal(tsScreen + "  " + message, tsScreen.length() + 2, tsFile + "  " + message, style, Align.LEFT, null);
    }

    /**
     * Appends a message. {@code renderText} is shown in the panel (its first {@code prefixLen} chars drawn in
     * timestamp color); {@code transcriptText} is what {@link #exportText()} returns. The two differ only for
     * SYSTEM_LOG entries, where the panel shows local time and the export uses a UTC journal timestamp.
     * {@code msgStyle}/{@code align} set the per-entry rendering (chat mode uses both; other panels pass the
     * panel style and {@link Align#LEFT}). {@code timestamp} is the chat card time (null for non-chat panels).
     */
    private void addMessageInternal(String text, int prefixLen, String transcriptText, Style msgStyle, Align align,
                                    Instant timestamp) {
        if (text == null || text.isBlank()) return;
        transcript.addLast(transcriptText);
        while (transcript.size() > MAX_TRANSCRIPT) transcript.removeFirst();
        for (Message m : messages) {
            m.complete = true;
            m.visibleText = m.fullText;
        }
        typewriterTimer.stop();
        removeAllTimerListeners();
        scrollOffset = 0;
        Message msg = new Message(text, prefixLen, msgStyle, align, timestamp);
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
        transcript.clear();
        offscreen = null;
        repaint();
    }

    /**
     * The full retained transcript (up to {@link #MAX_TRANSCRIPT} lines, beyond the 20-message render window),
     * one entry per line, for saving the log to a file. Empty string when nothing has been logged.
     */
    public String exportText() {
        return String.join(System.lineSeparator(), transcript);
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

        if (chat) {
            paintChat(g2, w, h);
            g2.dispose();
            g.drawImage(offscreen, 0, 0, null);
            return;
        }

        Font font = hudFont();
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int textX = PAD_X + fm.stringWidth(style.marker) + MARKER_GAP;
        int maxW = w - textX - PAD_X - SCROLLBAR_W;

        // Plain top-down log (SYSTEM_LOG): newest pinned to the bottom, older entries scroll up.
        if (!messages.isEmpty()) {
            List<List<String>> wrappedHistory = new ArrayList<>();
            for (Message m : messages) {
                wrappedHistory.add(wrapText(m.complete ? m.fullText : m.visibleText, fm, maxW));
            }

            int newTotalH = PAD_Y;
            for (List<String> lines : wrappedHistory) {
                newTotalH += lines.size() * fm.getHeight() + LINE_GAP;
            }
            totalContentHeight = newTotalH;
            clampScroll();

            int y = h - PAD_Y + scrollOffset;
            for (int i = messages.size() - 1; i >= 0; i--) {
                List<String> lines = wrappedHistory.get(i);
                int blockH = lines.size() * fm.getHeight();
                y -= blockH;
                if (y + blockH <= 0) break;

                g2.setColor(style.markerColor);
                g2.drawString(style.marker, PAD_X, y + fm.getAscent());

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

                // Typewriter cursor on the newest still-animating entry.
                if (!msg.complete && i == messages.size() - 1) {
                    String lastLine = lines.get(lines.size() - 1);
                    int cx = textX + fm.stringWidth(lastLine);
                    int cy = y + (lines.size() - 1) * fm.getHeight();
                    if ((System.currentTimeMillis() / 500) % 2 == 0) {
                        drawCaret(g2, cx + 1, cy + fm.getAscent(), fm, style.textColor);
                    }
                }

                y -= LINE_GAP;
            }

            if (totalContentHeight > h) drawScrollbar(g2, w, h);
        }

        g2.dispose();
        g.drawImage(offscreen, 0, 0, null);
    }

    /**
     * Chat renderer: one merged, time-ordered stream of cards. Commander cards sit on the left with a green
     * rail; AI (Vega) cards sit on the right with a cyan rail. Each card shows a timestamp above its text.
     * The AI card that is still being written is the "active" card - it gets a fully-opaque rail, a faint
     * left-fading highlight and L-shaped corner brackets instead of a typewriter caret. The commander's own
     * in-progress line types in the blinking bottom prompt row (its waiting cursor) before it posts.
     * This method orchestrates layout/scroll; each card kind is drawn by a dedicated painter.
     */
    private void paintChat(Graphics2D g2, int w, int h) {
        Font font = hudFont();
        g2.setFont(font);
        ChatMetrics m = chatMetrics(g2, font, w);

        // Commander (left) newest message types in the bottom prompt row; Vega (right) types in-place as the
        // active card. So only a commander in-progress line is excluded from history here.
        Message newest = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        boolean cmdrAnimating = newest != null && !newest.complete && newest.align == Align.LEFT;
        List<String> activeLines = cmdrAnimating ? wrapText(newest.visibleText, m.fm(), m.wrapW()) : null;
        int nInputRows = cmdrAnimating ? activeLines.size() : 1;
        int cursorZoneH = nInputRows * m.lineH() + LINE_GAP;

        int historyCount = cmdrAnimating ? messages.size() - 1 : messages.size();

        // Pre-wrap history and total content height (timestamp + text + inter-card gap per card).
        List<List<String>> wrapped = new ArrayList<>();
        for (int i = 0; i < historyCount; i++) {
            Message msg = messages.get(i);
            wrapped.add(wrapText(msg.complete ? msg.fullText : msg.visibleText, m.fm(), m.wrapW()));
        }
        int total = PAD_Y + cursorZoneH;
        for (List<String> lines : wrapped) {
            total += cardHeight(lines, m) + CHAT_CARD_GAP;
        }
        totalContentHeight = total;
        clampScroll();

        // Draw cards bottom-up: newest sits just above the prompt row, older cards extend upward.
        int y = h - PAD_Y - cursorZoneH + scrollOffset;
        for (int i = historyCount - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            List<String> lines = wrapped.get(i);
            int cardH = cardHeight(lines, m);
            y -= cardH;
            if (y + cardH <= 0) break; // fully scrolled above the viewport; all older cards are too

            if (msg.align == Align.RIGHT) {
                boolean active = !msg.complete && i == messages.size() - 1;
                paintVegaCard(g2, msg, lines, active, y, cardH, m);
            } else {
                paintCommanderCard(g2, msg, lines, y, cardH, m);
            }
            y -= CHAT_CARD_GAP;
        }

        if (totalContentHeight > h) drawScrollbar(g2, w, h);
        paintCommanderPrompt(g2, h, m, cmdrAnimating, activeLines);
    }

    /** Fonts, metrics and X-geometry shared by the chat card painters; computed once per paint. */
    private record ChatMetrics(Font font, Font tsFont, FontMetrics fm, FontMetrics tsFm,
                               int lineH, int tsH, int leftTextX, int rightRailX, int rightTextEnd, int wrapW) {}

    private ChatMetrics chatMetrics(Graphics2D g2, Font font, int w) {
        Font tsFont = font.deriveFont(font.getSize2D() * 0.82f);
        FontMetrics fm = g2.getFontMetrics(font);
        FontMetrics tsFm = g2.getFontMetrics(tsFont);
        int rightRailX = w - PAD_X - SCROLLBAR_W - CHAT_RAIL_W;
        int leftTextX = PAD_X + CHAT_RAIL_W + CHAT_RAIL_TEXT_GAP;
        int rightTextEnd = rightRailX - CHAT_RAIL_TEXT_GAP;
        int wrapW = Math.max(10, Math.min(rightTextEnd - leftTextX, Math.round(w * CHAT_BUBBLE_MAX_FRACTION)));
        return new ChatMetrics(font, tsFont, fm, tsFm,
                fm.getHeight(), tsFm.getHeight(), leftTextX, rightRailX, rightTextEnd, wrapW);
    }

    /** Height a chat card occupies, excluding the inter-card gap (timestamp line + wrapped text lines). */
    private static int cardHeight(List<String> lines, ChatMetrics m) {
        return m.tsH() + CHAT_TS_GAP + lines.size() * m.lineH();
    }

    /** Right (Vega) card: cyan rail, timestamp, text right-anchored (single line) or column-aligned (wrapped);
     *  the active card adds a highlight wash and corner brackets in place of a caret. */
    private void paintVegaCard(Graphics2D g2, Message msg, List<String> lines, boolean active, int y, int cardH, ChatMetrics m) {
        boolean multiLine = lines.size() > 1;
        int blockW = multiLine ? m.wrapW() : m.fm().stringWidth(lines.get(0));
        int colLeft = m.rightTextEnd() - blockW;
        int boxRight = m.rightRailX() + CHAT_RAIL_W;
        if (active) paintActiveHighlight(g2, colLeft, y, boxRight, cardH);
        g2.setColor(active ? HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK
                : withAlpha(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, CHAT_RAIL_ALPHA));
        g2.fillRect(m.rightRailX(), y, CHAT_RAIL_W, cardH);
        drawTimestamp(g2, msg, m.tsFont(), m.tsFm(), m.rightTextEnd(), y + m.tsFm().getAscent(), true);
        g2.setFont(m.font());
        g2.setColor(msg.style.textColor);
        int textTop = y + m.tsH() + CHAT_TS_GAP;
        for (int li = 0; li < lines.size(); li++) {
            String line = lines.get(li);
            int lx = multiLine ? colLeft : m.rightTextEnd() - m.fm().stringWidth(line);
            g2.drawString(line, lx, textTop + li * m.lineH() + m.fm().getAscent());
        }
        if (active) paintCornerBrackets(g2, colLeft, y, boxRight, cardH);
    }

    /** Left (commander) card: green rail, timestamp, left-aligned text. */
    private void paintCommanderCard(Graphics2D g2, Message msg, List<String> lines, int y, int cardH, ChatMetrics m) {
        g2.setColor(withAlpha(HudPalette.HUD_COLOR_ROLE_COMMANDER_MARKER, CHAT_RAIL_ALPHA));
        g2.fillRect(PAD_X, y, CHAT_RAIL_W, cardH);
        drawTimestamp(g2, msg, m.tsFont(), m.tsFm(), m.leftTextX(), y + m.tsFm().getAscent(), false);
        g2.setFont(m.font());
        g2.setColor(msg.style.textColor);
        int textTop = y + m.tsH() + CHAT_TS_GAP;
        for (int li = 0; li < lines.size(); li++) {
            g2.drawString(lines.get(li), m.leftTextX(), textTop + li * m.lineH() + m.fm().getAscent());
        }
    }

    /** Bottom prompt row: types the commander's in-progress line, else shows the blinking waiting cursor. */
    private void paintCommanderPrompt(Graphics2D g2, int h, ChatMetrics m, boolean animating, List<String> activeLines) {
        FontMetrics fm = m.fm();
        g2.setFont(m.font());
        int promptTextX = PAD_X + fm.stringWidth(style.marker) + MARKER_GAP;
        int inputRowTop = h - PAD_Y - m.lineH();
        int inputBaseline = inputRowTop + fm.getAscent();
        if (animating) {
            int n = activeLines.size();
            int blockTop = inputRowTop - (n - 1) * m.lineH();
            g2.setColor(style.markerColor);
            g2.drawString(style.marker, PAD_X, blockTop + fm.getAscent());
            g2.setColor(style.textColor);
            for (int li = 0; li < n; li++) {
                g2.drawString(activeLines.get(li), promptTextX, blockTop + li * m.lineH() + fm.getAscent());
            }
            int cx = promptTextX + fm.stringWidth(activeLines.get(n - 1));
            if ((System.currentTimeMillis() / 500) % 2 == 0) drawCaret(g2, cx + 1, inputBaseline, fm, style.textColor);
        } else {
            g2.setColor(style.markerColor);
            g2.drawString(style.marker, PAD_X, inputBaseline);
            if (caretVisible) drawCaret(g2, promptTextX, inputBaseline, fm, HudPalette.HUD_COLOR_ROLE_USER_INPUT_LOG_TEXT);
        }
    }

    /** Timestamp above a chat card: dim mono time, right-anchored for AI cards, left for commander cards. */
    private void drawTimestamp(Graphics2D g2, Message msg, Font tsFont, FontMetrics tsFm, int anchorX, int baselineY, boolean right) {
        if (msg.timestamp == null) return;
        String ts = LogTimestampFormat.screen(msg.timestamp);
        g2.setFont(tsFont);
        g2.setColor(HudPalette.HUD_COLOR_ROLE_SYSTEM_LOG_TIMESTAMP_TEXT);
        int x = right ? anchorX - tsFm.stringWidth(ts) : anchorX;
        g2.drawString(ts, x, baselineY);
    }

    /** Left-fading cyan wash behind the active AI card (strongest at the rail, transparent at the text edge). */
    private void paintActiveHighlight(Graphics2D g2, int left, int top, int right, int cardH) {
        Color c = HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK;
        Paint old = g2.getPaint();
        g2.setPaint(new GradientPaint(right, top, withAlpha(c, CHAT_HIGHLIGHT_ALPHA), left, top, withAlpha(c, 0)));
        g2.fillRect(left, top, right - left, cardH);
        g2.setPaint(old);
    }

    /** L-shaped corner brackets on the top-right and bottom-right of the active AI card. */
    private void paintCornerBrackets(Graphics2D g2, int left, int top, int right, int cardH) {
        g2.setColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK);
        int t = CHAT_RAIL_W, arm = CHAT_BRACKET_ARM, bottom = top + cardH;
        g2.fillRect(right - arm, top, arm, t);          // top-right horizontal
        g2.fillRect(right - t, top, t, arm);            // top-right vertical
        g2.fillRect(right - arm, bottom - t, arm, t);   // bottom-right horizontal
        g2.fillRect(right - t, bottom - arm, t, arm);   // bottom-right vertical
    }

    /** Thin translucent scrollbar drawn when content overflows; shared by the log and chat renderers. */
    private void drawScrollbar(Graphics2D g2, int w, int h) {
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

    /** Returns {@code base} with the given alpha, so rails/highlights derive from a canon role, not a literal hue. */
    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
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
