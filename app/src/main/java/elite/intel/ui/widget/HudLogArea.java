package elite.intel.ui.widget;

import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.HudGlyphs;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Canvas-rendered HUD readout that displays messages bottom-up with a typewriter animation.
 * Each entry is prefixed with a style-specific marker drawn in the style's accent color;
 * continuation lines indent to align with the message text, not the marker.
 * No title strip - intended to be placed inside a titled {@link HudSection}.
 */
public class HudLogArea extends JPanel implements Scrollable {

    /** Property fired when the system-log text selection changes between empty and non-empty. */
    public static final String SELECTION_PROPERTY = "hudLogSelection";

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
    private static final int MARKER_GAP = 4;
    /** Chat bubbles wrap at this fraction of panel width so long lines don't span the whole panel. */
    private static final float CHAT_BUBBLE_MAX_FRACTION = 0.72f;
    /** Accent rail thickness on the outer edge of a chat card. */
    private static final int CHAT_RAIL_W = HudPalette.HUD_BORDER_THICKNESS_ACCENT;
    /** Minimum gap between a card's text column and its rail. */
    private static final int CHAT_RAIL_TEXT_GAP = 10;
    /** Vertical gap between adjacent chat cards. */
    private static final int CHAT_CARD_GAP = 14;
    /** Gap between a card's timestamp line and its first text line. */
    private static final int CHAT_TS_GAP = 2;
    /** Alpha of a normal card's rail; the active AI card draws its rail fully opaque. */
    private static final int CHAT_RAIL_ALPHA = 150;
    /** Alpha of the active AI card's background highlight at the rail edge (fades to transparent). */
    private static final int CHAT_HIGHLIGHT_ALPHA = 31;
    private static final int FULL_ALPHA = 255;
    private static final long CHAT_ACTIVE_HOLD_NANOS = TimeUnit.MILLISECONDS.toNanos(HudPalette.HUD_CHAT_ACTIVE_HOLD_MS);
    private static final ChatPresentation STANDARD_CHAT_PRESENTATION = new ChatPresentation(
            true,
            true,
            HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
    private static final ChatPresentation OVERLAY_CHAT_PRESENTATION = new ChatPresentation(
            false,
            false,
            HudPalette.HUD_COLOR_ROLE_OVERLAY_TRANSPARENT_BACKGROUND);

    /** Presentation options that let the overlay omit chrome without changing the standard chat surface. */
    private record ChatPresentation(boolean showTimestamps, boolean showCommanderPrompt, Color background) {}

    private final Style style;
    /**
     * Chat mode merges commander + AI messages into one stream: each message carries its own
     * {@link Style}/{@link Align}, and a single blinking commander prompt is pinned to the bottom.
     */
    private final boolean chat;
    private final boolean selectable;
    /** Font role selected by the hosting surface. */
    private final float fontSize;
    private final ChatPresentation chatPresentation;
    private final List<Message> messages = new ArrayList<>();
    /** Every line ever added this session (capped at {@link #MAX_TRANSCRIPT}), for {@link #exportText()}. */
    private final Deque<String> transcript = new ArrayDeque<>();
    private BufferedImage offscreen;
    private final Timer typewriterTimer;
    /**
     * Non-null only in chat mode; toggles {@link #caretVisible} to blink the commander prompt cursor.
     */
    private final Timer blinkTimer;
    /** Repaints the short post-typewriter fade while Vega's active-card treatment decays. */
    private final Timer activeCardTimer;
    private boolean caretVisible = false;
    private boolean selecting;
    private TextPosition selectionAnchor;
    private TextPosition selectionLead;

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
        /** Monotonic deadline for Vega's post-typewriter active-card fade. */
        long activeUntilNanos;

        Message(String t, int prefixLen, Style style, Align align, Instant timestamp) {
            this.fullText = t;
            this.prefixLen = prefixLen;
            this.style = style;
            this.align = align;
            this.timestamp = timestamp;
        }
    }

    private record TextPosition(int messageIndex, int lineIndex, int charIndex) {}

    private record RenderedLine(int messageIndex, int lineIndex, String text, int x, int topY) {}

    private record SelectionRange(int start, int end) {}

    /**
     * Plain top-down log panel (e.g. {@link Style#SYSTEM_LOG}); no chat cards or input row.
     *
     * @param typewriterDelayMs milliseconds between typewriter character steps
     * @param style             visual marker style
     */
    public HudLogArea(int typewriterDelayMs, Style style) {
        this(typewriterDelayMs, style, false, HudPalette.HUD_FONT_LOG_PANEL, STANDARD_CHAT_PRESENTATION);
    }

    /**
     * Chat-mode factory: a single stream that renders commander messages ({@link Style#USER_INPUT},
     * left) and AI messages ({@link Style#AI_RESPONSE}, right) added via
     * {@link #addMessage(String, Style, Align)}, with a blinking commander prompt pinned to the bottom.
     *
     * @param typewriterDelayMs milliseconds between typewriter character steps
     */
    public static HudLogArea chat(int typewriterDelayMs) {
        return new HudLogArea(typewriterDelayMs, Style.USER_INPUT, true, HudPalette.HUD_FONT_LOG_PANEL,
                STANDARD_CHAT_PRESENTATION);
    }

    /**
     * Creates the chat canvas for the always-on-top companion overlay. Its dedicated presentation keeps the
     * current exchange readable above the game without timestamps, an idle command prompt, or an opaque canvas.
     *
     * @param typewriterDelayMs milliseconds between typewriter character steps
     * @return HUD chat canvas sized by {@link HudPalette#HUD_FONT_OVERLAY}
     */
    public static HudLogArea overlayChat(int typewriterDelayMs) {
        return new HudLogArea(typewriterDelayMs, Style.USER_INPUT, true, HudPalette.HUD_FONT_OVERLAY,
                OVERLAY_CHAT_PRESENTATION);
    }

    private HudLogArea(int typewriterDelayMs, Style style, boolean chat, float fontSize,
                       ChatPresentation chatPresentation) {
        this.style = style;
        this.chat = chat;
        this.selectable = !chat && style == Style.SYSTEM_LOG;
        this.fontSize = fontSize;
        this.chatPresentation = chatPresentation;
        setOpaque(chatPresentation.background().getAlpha() == 255);
        setBackground(chatPresentation.background());
        typewriterTimer = new Timer(typewriterDelayMs, null);
        if (chat) {
            if (chatPresentation.showCommanderPrompt()) {
                blinkTimer = new Timer(530, e -> {
                    caretVisible = !caretVisible;
                    repaint();
                });
                blinkTimer.setRepeats(true);
            } else {
                blinkTimer = null;
            }
            activeCardTimer = new Timer(HudPalette.HUD_CHAT_ACTIVE_FADE_FRAME_MS, e -> repaintActiveCardFade());
            activeCardTimer.setRepeats(true);
        } else {
            blinkTimer = null;
            activeCardTimer = null;
        }
        if (selectable) installSelectionSupport();
    }

    private void installSelectionSupport() {
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copySelectedText");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK), "selectAllText");
        getActionMap().put("copySelectedText", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                copySelectedText();
            }
        });
        getActionMap().put("selectAllText", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                selectAllText();
            }
        });

        MouseAdapter selectionMouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                requestFocusInWindow();
                TextPosition position = hitTest(e.getPoint());
                if (position == null) {
                    clearSelection();
                    return;
                }
                boolean previousSelection = hasSelection();
                selectionAnchor = position;
                selectionLead = position;
                selecting = true;
                fireSelectionStateChanged(previousSelection);
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!selecting) return;
                TextPosition position = hitTest(e.getPoint());
                if (position == null) return;
                boolean previousSelection = hasSelection();
                selectionLead = position;
                fireSelectionStateChanged(previousSelection);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) selecting = false;
            }
        };
        addMouseListener(selectionMouse);
        addMouseMotionListener(selectionMouse);
    }

    /** Copies the current system-log selection to the system clipboard, if a non-empty range is selected. */
    public void copySelectedText() {
        String text = selectedText(refreshSystemLines());
        if (text.isEmpty()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    /** Returns whether this log currently contains a non-empty text selection. */
    public boolean hasSelectedText() {
        return hasSelection();
    }

    private void selectAllText() {
        List<RenderedLine> lines = refreshSystemLines();
        if (lines.isEmpty()) {
            clearSelection();
            return;
        }
        boolean previousSelection = hasSelection();
        RenderedLine first = lines.get(0);
        RenderedLine last = lines.get(lines.size() - 1);
        selectionAnchor = new TextPosition(first.messageIndex(), first.lineIndex(), 0);
        selectionLead = new TextPosition(last.messageIndex(), last.lineIndex(), last.text().length());
        fireSelectionStateChanged(previousSelection);
        repaint();
    }

    private void clearSelection() {
        boolean previousSelection = hasSelection();
        selecting = false;
        selectionAnchor = null;
        selectionLead = null;
        fireSelectionStateChanged(previousSelection);
        repaint();
    }

    private void fireSelectionStateChanged(boolean previousSelection) {
        boolean currentSelection = hasSelection();
        if (previousSelection != currentSelection) {
            firePropertyChange(SELECTION_PROPERTY, previousSelection, currentSelection);
        }
    }

    private boolean hasSelection() {
        return selectionAnchor != null && selectionLead != null
                && comparePositions(selectionAnchor, selectionLead) != 0;
    }

    private TextPosition selectionStart() {
        return comparePositions(selectionAnchor, selectionLead) <= 0 ? selectionAnchor : selectionLead;
    }

    private TextPosition selectionEnd() {
        return comparePositions(selectionAnchor, selectionLead) <= 0 ? selectionLead : selectionAnchor;
    }

    private static int comparePositions(TextPosition left, TextPosition right) {
        int messageOrder = Integer.compare(left.messageIndex(), right.messageIndex());
        if (messageOrder != 0) return messageOrder;
        int lineOrder = Integer.compare(left.lineIndex(), right.lineIndex());
        if (lineOrder != 0) return lineOrder;
        return Integer.compare(left.charIndex(), right.charIndex());
    }

    private List<RenderedLine> refreshSystemLines() {
        if (!selectable) return List.of();
        Font font = hudFont();
        FontMetrics fm = getFontMetrics(font);
        int textX = PAD_X + fm.stringWidth(style.marker) + MARKER_GAP;
        int maxW = Math.max(1, layoutWidth() - textX - PAD_X);
        int height = Math.max(getHeight(), calculateContentHeight(layoutWidth()));
        return buildSystemLines(fm, textX, maxW, height);
    }

    private String selectedText(List<RenderedLine> lines) {
        if (!hasSelection()) return "";
        StringBuilder result = new StringBuilder();
        for (RenderedLine line : lines) {
            SelectionRange range = selectionRange(line);
            if (range == null) continue;
            if (!result.isEmpty()) result.append(System.lineSeparator());
            result.append(line.text(), range.start(), range.end());
        }
        return result.toString();
    }

    private SelectionRange selectionRange(RenderedLine line) {
        if (!hasSelection()) return null;
        TextPosition start = selectionStart();
        TextPosition end = selectionEnd();
        TextPosition lineStart = new TextPosition(line.messageIndex(), line.lineIndex(), 0);
        TextPosition lineEnd = new TextPosition(line.messageIndex(), line.lineIndex(), line.text().length());
        if (comparePositions(end, lineStart) <= 0 || comparePositions(start, lineEnd) >= 0) return null;

        int from = comparePositions(start, lineStart) <= 0 ? 0 : start.charIndex();
        int to = comparePositions(end, lineEnd) >= 0 ? line.text().length() : end.charIndex();
        return from < to ? new SelectionRange(from, to) : null;
    }

    private TextPosition hitTest(Point point) {
        List<RenderedLine> lines = refreshSystemLines();
        if (lines.isEmpty()) return null;

        FontMetrics fm = getFontMetrics(hudFont());
        RenderedLine closest = null;
        int closestDistance = Integer.MAX_VALUE;
        for (RenderedLine line : lines) {
            int lineBottom = line.topY() + fm.getHeight();
            if (point.y >= line.topY() && point.y < lineBottom) {
                closest = line;
                break;
            }
            int distance = point.y < line.topY() ? line.topY() - point.y : point.y - lineBottom;
            if (distance < closestDistance) {
                closest = line;
                closestDistance = distance;
            }
        }
        if (closest == null) return null;

        int relativeX = point.x - closest.x();
        int charIndex = characterIndex(closest.text(), relativeX, fm);
        return new TextPosition(closest.messageIndex(), closest.lineIndex(), charIndex);
    }

    private static int characterIndex(String text, int relativeX, FontMetrics fm) {
        if (relativeX <= 0) return 0;
        for (int i = 1; i <= text.length(); i++) {
            int left = fm.stringWidth(text.substring(0, i - 1));
            int right = fm.stringWidth(text.substring(0, i));
            if (relativeX < (left + right) / 2) return i - 1;
        }
        return text.length();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (blinkTimer != null) blinkTimer.start();
    }

    @Override
    public void removeNotify() {
        if (blinkTimer != null) blinkTimer.stop();
        if (activeCardTimer != null) activeCardTimer.stop();
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
        if (selectable) clearSelection();
        transcript.addLast(transcriptText);
        while (transcript.size() > MAX_TRANSCRIPT) transcript.removeFirst();
        if (activeCardTimer != null) activeCardTimer.stop();
        for (Message m : messages) {
            m.complete = true;
            m.visibleText = m.fullText;
            m.activeUntilNanos = 0L;
        }
        typewriterTimer.stop();
        removeAllTimerListeners();
        Message msg = new Message(text, prefixLen, msgStyle, align, timestamp);
        messages.add(msg);
        while (messages.size() > MAX_MESSAGES) messages.remove(0);
        startTypewriter(msg);
        revalidate();
        repaint();
        scrollToBottom();
    }

    /**
     * Clears all messages.
     */
    public void clear() {
        typewriterTimer.stop();
        removeAllTimerListeners();
        if (activeCardTimer != null) activeCardTimer.stop();
        if (selectable) clearSelection();
        messages.clear();
        transcript.clear();
        offscreen = null;
        revalidate();
        repaint();
        scrollToBottom();
    }

    /**
     * The full retained transcript (up to {@link #MAX_TRANSCRIPT} lines, beyond the 20-message render window),
     * one entry per line, for saving the log to a file. Empty string when nothing has been logged.
     */
    public String exportText() {
        return String.join(System.lineSeparator(), transcript);
    }

    private Font hudFont() {
        return getFont().deriveFont(fontSize);
    }

    /** Returns the canvas size required by the current message stream for the viewport width. */
    @Override
    public Dimension getPreferredSize() {
        int width = layoutWidth();
        int contentHeight = calculateContentHeight(width);
        return new Dimension(width, Math.max(contentHeight, viewportHeight()));
    }

    /** Supplies the preferred viewport size used by Swing when the HUD scroll pane is first laid out. */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /** Returns the wheel increment in logical HUD text rows. */
    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(1, getFontMetrics(hudFont()).getHeight() * 3);
    }

    /** Returns a block increment that advances almost one visible viewport. */
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(getScrollableUnitIncrement(visibleRect, orientation, direction),
                visibleRect.height - getFontMetrics(hudFont()).getHeight());
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private int layoutWidth() {
        int width = getWidth();
        if (width <= 0 && getParent() != null) width = getParent().getWidth();
        return Math.max(1, width);
    }

    private int viewportHeight() {
        Container parent = getParent();
        if (parent instanceof JViewport viewport) return Math.max(0, viewport.getHeight());
        return Math.max(0, getHeight());
    }

    private int calculateContentHeight(int width) {
        Font font = hudFont();
        if (!chat) {
            FontMetrics fm = getFontMetrics(font);
            int textX = PAD_X + fm.stringWidth(style.marker) + MARKER_GAP;
            int maxW = Math.max(1, width - textX - PAD_X);
            int height = PAD_Y;
            for (Message message : messages) {
                height += wrapText(message.complete ? message.fullText : message.visibleText, fm, maxW).size()
                        * fm.getHeight() + LINE_GAP;
            }
            return height;
        }

        ChatMetrics metrics = chatMetrics(font, width);
        Message newest = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        boolean cmdrAnimating = isCommanderPromptActive(newest);
        List<List<String>> wrapped = new ArrayList<>();
        int historyCount = cmdrAnimating ? messages.size() - 1 : messages.size();
        for (int i = 0; i < historyCount; i++) {
            Message message = messages.get(i);
            wrapped.add(wrapText(message.complete ? message.fullText : message.visibleText,
                    metrics.fm(), metrics.wrapW()));
        }

        int activeRows = cmdrAnimating
                ? wrapText(newest.visibleText, metrics.fm(), metrics.wrapW()).size()
                : (chatPresentation.showCommanderPrompt() ? 1 : 0);
        int height = PAD_Y + activeRows * metrics.lineH()
                + (chatPresentation.showCommanderPrompt() ? LINE_GAP : 0);
        for (List<String> lines : wrapped) height += cardHeight(lines, metrics) + CHAT_CARD_GAP;
        return height;
    }

    private boolean isCommanderPromptActive(Message message) {
        return chatPresentation.showCommanderPrompt()
                && message != null
                && !message.complete
                && message.align == Align.LEFT;
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            Container parent = getParent();
            if (!(parent instanceof JViewport viewport)) return;
            JScrollPane scrollPane = viewport.getParent() instanceof JScrollPane parentScroll
                    ? parentScroll : null;
            if (scrollPane == null) return;
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum() - vertical.getVisibleAmount());
        });
    }

    private boolean isScrolledToBottom() {
        Container parent = getParent();
        if (!(parent instanceof JViewport viewport)) return true;
        if (!(viewport.getParent() instanceof JScrollPane scrollPane)) return true;
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        return vertical.getValue() + vertical.getVisibleAmount() >= vertical.getMaximum() - 1;
    }

    private void removeAllTimerListeners() {
        for (ActionListener al : typewriterTimer.getActionListeners()) {
            typewriterTimer.removeActionListener(al);
        }
    }

    private void startTypewriter(Message target) {
        typewriterTimer.addActionListener(e -> {
            boolean wasAtBottom = isScrolledToBottom();
            if (target.complete) {
                target.visibleText = target.fullText;
                typewriterTimer.stop();
                revalidate();
                if (wasAtBottom) scrollToBottom();
                paintImmediately(0, 0, getWidth(), getHeight());
                return;
            }
            int len = target.visibleText.length();
            if (len < target.fullText.length()) {
                target.visibleText = target.fullText.substring(0, len + 1);
            }
            if (target.visibleText.length() >= target.fullText.length()) {
                target.complete = true;
                target.visibleText = target.fullText;
                typewriterTimer.stop();
                retainVegaCardActivity(target);
            }
            revalidate();
            if (wasAtBottom) scrollToBottom();
            paintImmediately(0, 0, getWidth(), getHeight());
        });
        typewriterTimer.start();
    }

    /** Retains the visual speaking state briefly after Vega's typewriter reaches the final character. */
    private void retainVegaCardActivity(Message message) {
        if (!chat || message.style != Style.AI_RESPONSE || message.align != Align.RIGHT) return;
        message.activeUntilNanos = System.nanoTime() + CHAT_ACTIVE_HOLD_NANOS;
        activeCardTimer.restart();
    }

    /** Repaints each fade frame and stops the timer once the newest Vega card reaches its normal treatment. */
    private void repaintActiveCardFade() {
        Message newest = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        if (newest == null || vegaActivityStrength(newest, messages.size() - 1) <= 0f) {
            activeCardTimer.stop();
        }
        repaint();
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
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                chatPresentation.background().getAlpha() == 255
                        ? RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB
                        : RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, w, h);
        g2.setComposite(AlphaComposite.SrcOver);
        if (chatPresentation.background().getAlpha() > 0) {
            g2.setColor(chatPresentation.background());
            g2.fillRect(0, 0, w, h);
        }

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
        int maxW = w - textX - PAD_X;

        // Plain top-down log (SYSTEM_LOG): newest pinned to the bottom, older entries scroll up.
        if (!messages.isEmpty()) {
            List<RenderedLine> systemLines = buildSystemLines(fm, textX, Math.max(1, maxW), h);
            for (int lineIndex = 0; lineIndex < systemLines.size(); lineIndex++) {
                RenderedLine line = systemLines.get(lineIndex);
                if (line.topY() + fm.getHeight() <= 0 || line.topY() > h) continue;

                Message msg = messages.get(line.messageIndex());
                if (line.lineIndex() == 0) {
                    g2.setColor(style.markerColor);
                    g2.drawString(style.marker, PAD_X, line.topY() + fm.getAscent());
                }
                paintSystemLine(g2, msg, line, fm);

                // Typewriter cursor on the newest still-animating entry.
                boolean lastLine = lineIndex == systemLines.size() - 1
                        || systemLines.get(lineIndex + 1).messageIndex() != line.messageIndex();
                if (!msg.complete && line.messageIndex() == messages.size() - 1 && lastLine
                        && (System.currentTimeMillis() / 500) % 2 == 0) {
                    int cx = line.x() + fm.stringWidth(line.text());
                    HudGlyphs.paintHudTextCaret(g2, cx + 1, line.topY() + fm.getAscent(), fm, style.textColor);
                }
            }
        }

        g2.dispose();
        g.drawImage(offscreen, 0, 0, null);
    }

    private List<RenderedLine> buildSystemLines(FontMetrics fm, int textX, int maxW, int height) {
        List<List<String>> wrappedHistory = new ArrayList<>();
        for (Message message : messages) {
            wrappedHistory.add(wrapText(message.complete ? message.fullText : message.visibleText, fm, maxW));
        }

        List<RenderedLine> result = new ArrayList<>();
        int y = height - PAD_Y;
        for (int messageIndex = messages.size() - 1; messageIndex >= 0; messageIndex--) {
            List<String> lines = wrappedHistory.get(messageIndex);
            int blockH = lines.size() * fm.getHeight();
            y -= blockH;
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                result.add(new RenderedLine(messageIndex, lineIndex, lines.get(lineIndex), textX,
                        y + lineIndex * fm.getHeight()));
            }
            y -= LINE_GAP;
        }
        result.sort(Comparator.comparingInt(RenderedLine::messageIndex)
                .thenComparingInt(RenderedLine::lineIndex));
        return result;
    }

    private void paintSystemLine(Graphics2D g2, Message message, RenderedLine line, FontMetrics fm) {
        SelectionRange selection = selectionRange(line);
        String text = line.text();
        if (text.isEmpty()) return;
        AttributedString attributed = new AttributedString(text);
        attributed.addAttribute(TextAttribute.FONT, g2.getFont());
        attributed.addAttribute(TextAttribute.FOREGROUND, style.textColor);

        if (style == Style.SYSTEM_LOG && line.lineIndex() == 0 && message.prefixLen > 0) {
            int timestampEnd = Math.min(message.prefixLen, text.length());
            attributed.addAttribute(TextAttribute.FOREGROUND,
                    HudPalette.HUD_COLOR_ROLE_SYSTEM_LOG_TIMESTAMP_TEXT, 0, timestampEnd);
        }

        if (selection != null) {
            attributed.addAttribute(TextAttribute.FOREGROUND,
                    HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, selection.start(), selection.end());
        }

        TextLayout layout = new TextLayout(attributed.getIterator(), g2.getFontRenderContext());
        float baseline = line.topY() + fm.getAscent();
        if (selection != null) {
            Graphics2D selectionGraphics = (Graphics2D) g2.create();
            try {
                selectionGraphics.translate(line.x(), baseline);
                selectionGraphics.setColor(HudPalette.HUD_COLOR_ROLE_SELECTION_BACKGROUND);
                selectionGraphics.fill(layout.getLogicalHighlightShape(selection.start(), selection.end()));
            } finally {
                selectionGraphics.dispose();
            }
        }
        layout.draw(g2, line.x(), baseline);
    }

    /**
     * Chat renderer: one merged, time-ordered stream of cards. Commander cards sit on the left with a green
     * rail; AI (Vega) cards sit on the right with a cyan rail. The standard profile shows timestamps above text,
     * while the overlay profile omits that chrome.
     * The AI card that is still being written is the "active" card - it gets a fully-opaque rail, a faint
     * left-fading highlight and a fully opaque rail instead of a typewriter caret. Its active treatment smoothly
     * fades after the typewriter completes. The commander's own in-progress line types in the blinking bottom
     * prompt row (its waiting cursor) before it posts only in the standard profile.
     * This method orchestrates layout/scroll; each card kind is drawn by a dedicated painter.
     */
    private void paintChat(Graphics2D g2, int w, int h) {
        Font font = hudFont();
        g2.setFont(font);
        ChatMetrics m = chatMetrics(g2, font, w);

        // Commander (left) types in the bottom prompt row only in the standard profile; the overlay keeps every
        // in-progress message in its card so no empty input row is reserved.
        Message newest = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        boolean cmdrAnimating = isCommanderPromptActive(newest);
        List<String> activeLines = cmdrAnimating ? wrapText(newest.visibleText, m.fm(), m.wrapW()) : null;
        int nInputRows = cmdrAnimating ? activeLines.size() : (chatPresentation.showCommanderPrompt() ? 1 : 0);
        int cursorZoneH = chatPresentation.showCommanderPrompt()
                ? nInputRows * m.lineH() + LINE_GAP
                : 0;

        int historyCount = cmdrAnimating ? messages.size() - 1 : messages.size();

        // Pre-wrap history and total content height (timestamp + text + inter-card gap per card).
        List<List<String>> wrapped = new ArrayList<>();
        for (int i = 0; i < historyCount; i++) {
            Message msg = messages.get(i);
            wrapped.add(wrapText(msg.complete ? msg.fullText : msg.visibleText, m.fm(), m.wrapW()));
        }
        // Draw cards bottom-up: newest sits above the prompt row when present, older cards extend upward.
        int y = h - PAD_Y - cursorZoneH;
        for (int i = historyCount - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            List<String> lines = wrapped.get(i);
            int cardH = cardHeight(lines, m);
            y -= cardH;
            if (y + cardH <= 0) break; // fully scrolled above the viewport; all older cards are too

            if (msg.align == Align.RIGHT) {
                float activityStrength = vegaActivityStrength(msg, i);
                paintVegaCard(g2, msg, lines, activityStrength, y, cardH, m);
            } else {
                paintCommanderCard(g2, msg, lines, y, cardH, m);
            }
            y -= CHAT_CARD_GAP;
        }
        if (chatPresentation.showCommanderPrompt()) {
            paintCommanderPrompt(g2, h, m, cmdrAnimating, activeLines);
        }
    }

    /** Returns the newest Vega card's active-treatment strength: full while typing, smoothly fading afterward. */
    private float vegaActivityStrength(Message message, int messageIndex) {
        if (message.style != Style.AI_RESPONSE || messageIndex != messages.size() - 1) return 0f;
        if (!message.complete) return 1f;

        long remainingNanos = message.activeUntilNanos - System.nanoTime();
        if (remainingNanos <= 0L) return 0f;

        float remainingFraction = Math.min(1f, remainingNanos / (float) CHAT_ACTIVE_HOLD_NANOS);
        return remainingFraction * remainingFraction * (3f - 2f * remainingFraction);
    }

    /** Fonts, metrics and X-geometry shared by the chat card painters; computed once per paint. */
    private record ChatMetrics(Font font, Font tsFont, FontMetrics fm, FontMetrics tsFm,
                               int lineH, int tsH, int leftTextX, int rightRailX, int rightTextEnd, int wrapW) {}

    private ChatMetrics chatMetrics(Graphics2D g2, Font font, int w) {
        Font tsFont = font.deriveFont(font.getSize2D() * 0.82f);
        FontMetrics fm = g2.getFontMetrics(font);
        FontMetrics tsFm = g2.getFontMetrics(tsFont);
        return chatMetrics(font, w, fm, tsFm);
    }

    private ChatMetrics chatMetrics(Font font, int w) {
        Font tsFont = font.deriveFont(font.getSize2D() * 0.82f);
        return chatMetrics(font, w, getFontMetrics(font), getFontMetrics(tsFont));
    }

    private ChatMetrics chatMetrics(Font font, int w, FontMetrics fm, FontMetrics tsFm) {
        int rightRailX = w - PAD_X - CHAT_RAIL_W;
        int leftTextX = PAD_X + CHAT_RAIL_W + CHAT_RAIL_TEXT_GAP;
        int rightTextEnd = rightRailX - CHAT_RAIL_TEXT_GAP;
        int wrapW = Math.max(10, Math.min(rightTextEnd - leftTextX, Math.round(w * CHAT_BUBBLE_MAX_FRACTION)));
        return new ChatMetrics(font, font.deriveFont(font.getSize2D() * 0.82f), fm, tsFm,
                fm.getHeight(), chatPresentation.showTimestamps() ? tsFm.getHeight() : 0,
                leftTextX, rightRailX, rightTextEnd, wrapW);
    }

    /** Height a chat card occupies, excluding the inter-card gap (optional timestamp + wrapped text lines). */
    private static int cardHeight(List<String> lines, ChatMetrics m) {
        return m.tsH() + (m.tsH() > 0 ? CHAT_TS_GAP : 0) + lines.size() * m.lineH();
    }

    /** Right (Vega) card: cyan rail, timestamp, text right-anchored (single line) or column-aligned (wrapped);
     *  the active card adds a left-fading highlight in place of a caret. */
    private void paintVegaCard(Graphics2D g2, Message msg, List<String> lines, float activityStrength,
                               int y, int cardH, ChatMetrics m) {
        boolean multiLine = lines.size() > 1;
        int blockW = multiLine ? m.wrapW() : m.fm().stringWidth(lines.get(0));
        int colLeft = m.rightTextEnd() - blockW;
        int boxRight = m.rightRailX() + CHAT_RAIL_W;
        int highlightAlpha = Math.round(CHAT_HIGHLIGHT_ALPHA * activityStrength);
        if (highlightAlpha > 0) paintActiveHighlight(g2, colLeft, y, boxRight, cardH, highlightAlpha);
        int railAlpha = CHAT_RAIL_ALPHA + Math.round((FULL_ALPHA - CHAT_RAIL_ALPHA) * activityStrength);
        g2.setColor(withAlpha(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, railAlpha));
        g2.fillRect(m.rightRailX(), y, CHAT_RAIL_W, cardH);
        if (m.tsH() > 0) {
            drawTimestamp(g2, msg, m.tsFont(), m.tsFm(), m.rightTextEnd(), y + m.tsFm().getAscent(), true);
        }
        g2.setFont(m.font());
        g2.setColor(msg.style.textColor);
        int textTop = y + m.tsH() + (m.tsH() > 0 ? CHAT_TS_GAP : 0);
        for (int li = 0; li < lines.size(); li++) {
            String line = lines.get(li);
            int lx = multiLine ? colLeft : m.rightTextEnd() - m.fm().stringWidth(line);
            g2.drawString(line, lx, textTop + li * m.lineH() + m.fm().getAscent());
        }
    }

    /** Left (commander) card: green rail, timestamp, left-aligned text. */
    private void paintCommanderCard(Graphics2D g2, Message msg, List<String> lines, int y, int cardH, ChatMetrics m) {
        g2.setColor(withAlpha(HudPalette.HUD_COLOR_ROLE_COMMANDER_MARKER, CHAT_RAIL_ALPHA));
        g2.fillRect(PAD_X, y, CHAT_RAIL_W, cardH);
        if (m.tsH() > 0) {
            drawTimestamp(g2, msg, m.tsFont(), m.tsFm(), m.leftTextX(), y + m.tsFm().getAscent(), false);
        }
        g2.setFont(m.font());
        g2.setColor(msg.style.textColor);
        int textTop = y + m.tsH() + (m.tsH() > 0 ? CHAT_TS_GAP : 0);
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
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                HudGlyphs.paintHudTextCaret(g2, cx + 1, inputBaseline, fm, style.textColor);
            }
        } else {
            g2.setColor(style.markerColor);
            g2.drawString(style.marker, PAD_X, inputBaseline);
            if (caretVisible) {
                HudGlyphs.paintHudTextCaret(g2, promptTextX, inputBaseline, fm,
                        HudPalette.HUD_COLOR_ROLE_USER_INPUT_LOG_TEXT);
            }
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

    /** Left-fading cyan wash behind the active AI card; an intentional exception to the HUD no-gradient rule. */
    private void paintActiveHighlight(Graphics2D g2, int left, int top, int right, int cardH, int alpha) {
        Color c = HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK;
        Paint old = g2.getPaint();
        g2.setPaint(new GradientPaint(right, top, withAlpha(c, alpha), left, top, withAlpha(c, 0)));
        g2.fillRect(left, top, right - left, cardH);
        g2.setPaint(old);
    }

    /** Returns {@code base} with the given alpha, so rails/highlights derive from a canon role, not a literal hue. */
    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
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
