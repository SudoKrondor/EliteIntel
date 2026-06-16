package elite.intel.ui.screen;

import elite.intel.ui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Animated log panel using the OBSOverlayWindow typewriter technique.
 * - Renders messages bottom-up (newest at bottom)
 * - Typewriter animation: one character per timer tick
 * - When a new message arrives, current animation fast-forwards
 * - Title strip painted at top
 * - Optional clickable token detection (snake_case and PascalCase+Handler/Check words)
 */
class LogPanel extends JPanel {

    private static final int MAX_MESSAGES = 20;
    private static final int PAD_X = 12;
    private static final int PAD_Y = 8;
    private static final int LINE_GAP = 4;
    private static final int TITLE_HEIGHT = 26;

    // Detect LLM action names (snake_case) and handler class names
    private static final Pattern SNAKE_CASE = Pattern.compile("\\b[a-z][a-z0-9]*(?:_[a-z0-9]+)+\\b");
    private static final Pattern HANDLER_NAME = Pattern.compile("\\b[A-Z][a-zA-Z]*(?:Handler|Check|Monitor)\\b");

    // --- Config (set in constructor) ---
    private final String title;
    private final Color titleBg;
    private final Color titleFg;
    private final Color panelBg;
    private final Color textFg;
    private final Font font;
    private final boolean enableClickableTokens;

    private static final int SCROLLBAR_W = 5;

    // --- State ---
    private final List<Message> messages = new ArrayList<>();
    private final List<ClickableToken> clickableTokens = new ArrayList<>();
    private BufferedImage offscreen;
    private Timer typewriterTimer;
    private int scrollOffset = 0;      // pixels scrolled up from bottom (0 = at bottom)
    private int totalContentHeight = 0;

    // --- Inner types ---
    private static final class Message {
        final String fullText;
        String visibleText = "";
        boolean complete = false;

        Message(String t) {
            this.fullText = t;
        }
    }

    private static final class ClickableToken {
        final Rectangle bounds;
        final String value;

        ClickableToken(Rectangle b, String v) {
            bounds = b;
            value = v;
        }
    }

    // --- Constructor ---
    LogPanel(String title, Color titleBg, Color titleFg,
             Color panelBg, Color textFg, Font font,
             int typewriterDelayMs, boolean enableClickableTokens) {
        this.title = title;
        this.titleBg = titleBg;
        this.titleFg = titleFg;
        this.panelBg = panelBg;
        this.textFg = textFg;
        this.font = font;
        this.enableClickableTokens = enableClickableTokens;

        setOpaque(true);
        setBackground(panelBg);

        typewriterTimer = new Timer(typewriterDelayMs, null);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (ClickableToken ct : clickableTokens) {
                    if (ct.bounds.contains(e.getPoint())) {
                        copyToClipboard(ct.value);
                        return;
                    }
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean onToken = clickableTokens.stream()
                        .anyMatch(ct -> ct.bounds.contains(e.getPoint()));
                setCursor(onToken
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }
        });
        addMouseWheelListener(e -> {
            int lineHeight = getFontMetrics(font).getHeight();
            scrollOffset += (int) (e.getPreciseWheelRotation() * lineHeight * 3);
            clampScroll();
            repaint();
        });
    }

    private void clampScroll() {
        int visible = getHeight() - TITLE_HEIGHT - PAD_Y;
        int maxScroll = Math.max(0, totalContentHeight - visible);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    // --- Public API ---
    void addMessage(String text) {
        if (text == null || text.isBlank()) return;
        // Fast-forward any current animation
        for (Message m : messages) {
            m.complete = true;
            m.visibleText = m.fullText;
        }
        typewriterTimer.stop();
        removeAllTimerListeners();
        scrollOffset = 0;

        Message msg = new Message("\n \u2022 " + text);
        messages.add(msg);
        while (messages.size() > MAX_MESSAGES) messages.remove(0);

        startTypewriter(msg);
        repaint();
    }

    void clear() {
        typewriterTimer.stop();
        removeAllTimerListeners();
        messages.clear();
        clickableTokens.clear();
        offscreen = null;
        repaint();
    }

    // --- Typewriter ---
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

    // --- Painting ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (offscreen == null || offscreen.getWidth() != w || offscreen.getHeight() != h) {
            offscreen = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D g2 = offscreen.createGraphics();

        // Full background
        g2.setColor(panelBg);
        g2.fillRect(0, 0, w, h);

        // Title strip
        g2.setColor(titleBg);
        g2.fillRect(0, 0, w, TITLE_HEIGHT);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        Font titleFont = font.deriveFont(Font.BOLD, font.getSize2D() - 4f);
        g2.setFont(titleFont);
        FontMetrics tfm = g2.getFontMetrics();
        g2.setColor(titleFg);
        g2.drawString(title, PAD_X, (TITLE_HEIGHT + tfm.getAscent() - tfm.getDescent()) / 2);

        if (messages.isEmpty()) {
            g2.dispose();
            g.drawImage(offscreen, 0, 0, null);
            return;
        }

        // Message rendering
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int maxW = w - PAD_X * 2 - SCROLLBAR_W;

        // Wrap all message texts
        List<List<String>> wrappedAll = new ArrayList<>();
        for (Message m : messages) {
            wrappedAll.add(wrapText(m.complete ? m.fullText : m.visibleText, fm, maxW));
        }

        // Compute total content height for scroll clamping
        int newTotalH = PAD_Y;
        for (List<String> lines : wrappedAll) {
            newTotalH += lines.size() * fm.getHeight() + LINE_GAP;
        }
        totalContentHeight = newTotalH;
        clampScroll();

        g2.setClip(0, TITLE_HEIGHT, w, h - TITLE_HEIGHT);

        List<ClickableToken> newTokens = new ArrayList<>();
        int y = h - PAD_Y + scrollOffset;

        for (int i = messages.size() - 1; i >= 0; i--) {
            List<String> lines = wrappedAll.get(i);
            int blockH = lines.size() * fm.getHeight();
            y -= blockH;
            if (y + blockH <= TITLE_HEIGHT) break;

            Message msg = messages.get(i);
            for (int li = 0; li < lines.size(); li++) {
                String line = lines.get(li);
                int lineBaselineY = y + li * fm.getHeight() + fm.getAscent();
                int lineTopY = y + li * fm.getHeight();

                if (enableClickableTokens && msg.complete) {
                    drawLineWithTokens(g2, fm, line, PAD_X, lineBaselineY, lineTopY, newTokens);
                } else {
                    g2.setColor(textFg);
                    g2.drawString(line, PAD_X, lineBaselineY);
                }
            }

            // Blinking cursor on the newest incomplete message
            if (!msg.complete && i == messages.size() - 1) {
                String lastLine = lines.get(lines.size() - 1);
                int cx = PAD_X + fm.stringWidth(lastLine);
                int cy = y + (lines.size() - 1) * fm.getHeight();
                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    g2.setColor(textFg);
                    g2.fillRect(cx + 2, cy + 2, fm.charWidth('M') / 2, fm.getAscent() - 2);
                }
            }

            y -= LINE_GAP;
        }

        clickableTokens.clear();
        clickableTokens.addAll(newTokens);

        // Draw thin scrollbar when content overflows
        int contentArea = h - TITLE_HEIGHT;
        if (totalContentHeight > contentArea) {
            g2.setClip(null);
            int barX = w - SCROLLBAR_W - 1;
            int barTop = TITLE_HEIGHT + 2;
            int barH = contentArea - 4;
            int thumbH = Math.max(16, (int) ((float) contentArea / totalContentHeight * barH));
            int maxScroll = Math.max(1, totalContentHeight - contentArea);
            int thumbY = barTop + (int) ((float) (maxScroll - scrollOffset) / maxScroll * (barH - thumbH));
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillRoundRect(barX, barTop, SCROLLBAR_W, barH, 3, 3);
            g2.setColor(new Color(255, 255, 255, scrollOffset > 0 ? 100 : 55));
            g2.fillRoundRect(barX, thumbY, SCROLLBAR_W, thumbH, 3, 3);
        }

        g2.dispose();
        g.drawImage(offscreen, 0, 0, null);
    }

    private void drawLineWithTokens(Graphics2D g2, FontMetrics fm, String line,
                                    int x, int baselineY, int topY,
                                    List<ClickableToken> tokens) {
        List<int[]> ranges = new ArrayList<>();
        Matcher m1 = SNAKE_CASE.matcher(line);
        while (m1.find()) ranges.add(new int[]{m1.start(), m1.end()});
        Matcher m2 = HANDLER_NAME.matcher(line);
        while (m2.find()) {
            int s = m2.start(), e = m2.end();
            if (ranges.stream().noneMatch(r -> r[0] == s)) ranges.add(new int[]{s, e});
        }

        if (ranges.isEmpty()) {
            g2.setColor(textFg);
            g2.drawString(line, x, baselineY);
            return;
        }

        ranges.sort((a, b) -> Integer.compare(a[0], b[0]));
        int curX = x, prev = 0;
        for (int[] range : ranges) {
            if (range[0] > prev) {
                String before = line.substring(prev, range[0]);
                g2.setColor(textFg);
                g2.drawString(before, curX, baselineY);
                curX += fm.stringWidth(before);
            }
            String tok = line.substring(range[0], range[1]);
            int tokW = fm.stringWidth(tok);
            g2.setColor(AppTheme.ACCENT);
            g2.drawString(tok, curX, baselineY);
            g2.drawLine(curX, baselineY + 2, curX + tokW, baselineY + 2);
            tokens.add(new ClickableToken(new Rectangle(curX, topY, tokW, fm.getHeight()), tok));
            curX += tokW;
            prev = range[1];
        }
        if (prev < line.length()) {
            g2.setColor(textFg);
            g2.drawString(line.substring(prev), curX, baselineY);
        }
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

    private static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }
}
