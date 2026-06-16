package elite.intel.ui.screen;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.currentLanguageTag;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Scrollable, read-only Markdown viewer panel.
 * Converts a subset of Markdown to HTML and renders it via JEditorPane.
 * Supported: headings (H1-H4), bold, italic, inline code, fenced code blocks,
 * unordered/ordered lists, blockquotes, horizontal rules, and [text](url) links.
 * External links open in the system browser.
 */
public class MarkdownViewPanel extends JPanel {

    private final String filename;
    private JEditorPane editorPane;
    private JScrollPane scrollPane;

    public MarkdownViewPanel(String filename) {
        this.filename = filename;
        buildUi();
        loadContent();
    }

    private void buildUi() {
        setLayout(new BorderLayout(AppTheme.HUD_GAP, AppTheme.HUD_GAP));
        setOpaque(false);

        editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);

        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    // silently ignore - no browser or unsupported platform
                }
            }
        });

        this.scrollPane = AppTheme.hudScrollPane(editorPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Working zone of the tab → FLAT section (HUD §9), not a framed accent box.
        HudSection documentSection = new HudSection(
                getText("manual.section.document"),
                new BorderLayout(),
                HudPanel.Variant.FLAT,
                AppTheme.HUD_GAP);
        documentSection.body().add(scrollPane, BorderLayout.CENTER);

        // RELOAD belongs in the shared tab footer (HUD §10), not a top toolbar.
        JButton reloadButton = AppTheme.makeButtonSubtle(getText("button.reload"));
        reloadButton.addActionListener(e -> loadContent());
        JPanel footer = HudFooter.build(false, null, null, List.of(reloadButton));

        add(documentSection, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        applyTransparentSurface();

        // applyDarkPalette (run later in AppView) re-paints the JEditorPane white and gives it a
        // warm field border + a lighter HUD_PANEL_BG viewport. Re-assert the transparent surface
        // once that pass has completed so the document reads flush on the HUD background.
        SwingUtilities.invokeLater(this::applyTransparentSurface);
    }

    /**
     * Strips the field border and opaque fills that {@link AppTheme#applyDarkPalette} installs, so the
     * editor and its scroll viewport are transparent and the markdown sits flush on the HUD background.
     */
    private void applyTransparentSurface() {
        editorPane.setOpaque(false);
        editorPane.setBorder(BorderFactory.createEmptyBorder());
        // Viewport/scroll pane stay opaque (a non-opaque viewport smears on scroll) but carry the
        // screen background HUD_BG, not the lighter HUD_PANEL_BG, so no panel slab shows behind text.
        scrollPane.setBackground(AppTheme.HUD_BG);
        scrollPane.getViewport().setBackground(AppTheme.HUD_BG);
    }

    private void loadContent() {
        String markdown = readFile();
        String html;
        if (markdown == null) {
            html = wrapHtml("<p><i>File not found: <code>" + escapeHtml(filename) + "</code></i></p>"
                    + "<p>Place it in the same directory as the JAR (or in <code>distribution/</code> when running from IDE).</p>");
        } else {
            html = wrapHtml(convertToHtml(markdown));
        }
        editorPane.setText(html);
        SwingUtilities.invokeLater(() -> editorPane.setCaretPosition(0));
    }

    private String readFile() {
        // Prefer the variant for the active language (e.g. user-manual-ru.md); fall back to the base
        // file when no localized variant exists (English, or docs like credits.md that aren't translated).
        for (String name : candidateFileNames()) {
            String content = readNamed(name);
            if (content != null) {
                return content;
            }
        }
        return null;
    }

    /**
     * Returns the file names to try in order: the localized variant for the active UI language first,
     * then the base file as a fallback. English (the base) yields just the base name.
     */
    private List<String> candidateFileNames() {
        String localized = localizedName(filename, currentLanguageTag());
        return localized.equals(filename) ? List.of(filename) : List.of(localized, filename);
    }

    /**
     * Inserts a {@code -<tag>} language suffix before the file extension, e.g.
     * {@code ("user-manual.md", "ru")} → {@code "user-manual-ru.md"}. The English base tag and a
     * blank tag return the base name unchanged.
     */
    private static String localizedName(String base, String tag) {
        if (tag == null || tag.isBlank() || "en".equals(tag)) {
            return base;
        }
        int dot = base.lastIndexOf('.');
        return dot < 0
                ? base + "-" + tag
                : base.substring(0, dot) + "-" + tag + base.substring(dot);
    }

    /** Reads the named file from the working directory (production) or {@code distribution/} (IDE). */
    private String readNamed(String name) {
        Path p = Path.of(System.getProperty("user.dir")).resolve(name);
        if (Files.exists(p)) {
            try {
                return Files.readString(p);
            } catch (IOException ignored) {
            }
        }
        p = Path.of("distribution").resolve(name);
        if (Files.exists(p)) {
            try {
                return Files.readString(p);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    // -- Markdown → HTML converter ---------------------------------------------

    private String convertToHtml(String md) {
        String[] lines = md.split("\r?\n", -1);
        StringBuilder out = new StringBuilder();

        boolean inCode = false;
        boolean inUl = false;
        boolean inOl = false;
        List<String> paraLines = new ArrayList<>();

        for (String raw : lines) {

            // -- Fenced code block ------------------------------------------
            if (raw.startsWith("```")) {
                if (inCode) {
                    out.append("</code></pre>\n");
                    inCode = false;
                } else {
                    flushParagraph(out, paraLines);
                    inUl = closeLists(out, inUl, inOl);
                    inOl = false;
                    out.append("<pre><code>");
                    inCode = true;
                }
                continue;
            }

            if (inCode) {
                out.append(escapeHtml(raw)).append("\n");
                continue;
            }

            // -- Blank line -------------------------------------------------
            if (raw.trim().isEmpty()) {
                flushParagraph(out, paraLines);
                if (inUl) {
                    out.append("</ul>\n");
                    inUl = false;
                }
                if (inOl) {
                    out.append("</ol>\n");
                    inOl = false;
                }
                continue;
            }

            // -- Headings ---------------------------------------------------
            if (raw.startsWith("#### ")) {
                flushAndCloseLists(out, paraLines, inUl, inOl);
                inUl = inOl = false;
                out.append("<h4>").append(inline(raw.substring(5))).append("</h4>\n");
                continue;
            }
            if (raw.startsWith("### ")) {
                flushAndCloseLists(out, paraLines, inUl, inOl);
                inUl = inOl = false;
                out.append("<h3>").append(inline(raw.substring(4))).append("</h3>\n");
                continue;
            }
            if (raw.startsWith("## ")) {
                flushAndCloseLists(out, paraLines, inUl, inOl);
                inUl = inOl = false;
                out.append("<h2>").append(inline(raw.substring(3))).append("</h2>\n");
                continue;
            }
            if (raw.startsWith("# ")) {
                flushAndCloseLists(out, paraLines, inUl, inOl);
                inUl = inOl = false;
                out.append("<h1>").append(inline(raw.substring(2))).append("</h1>\n");
                continue;
            }

            // -- Horizontal rule --------------------------------------------
            if (raw.matches("^[-*_]{3,}\\s*$")) {
                flushAndCloseLists(out, paraLines, inUl, inOl);
                inUl = inOl = false;
                out.append("<hr/>\n");
                continue;
            }

            // -- Blockquote -------------------------------------------------
            if (raw.startsWith("> ")) {
                flushAndCloseLists(out, paraLines, inUl, inOl);
                inUl = inOl = false;
                out.append("<blockquote>").append(inline(raw.substring(2))).append("</blockquote>\n");
                continue;
            }

            // -- Unordered list ---------------------------------------------
            if (raw.matches("^[*\\-+] .+")) {
                flushParagraph(out, paraLines);
                if (inOl) {
                    out.append("</ol>\n");
                    inOl = false;
                }
                if (!inUl) {
                    out.append("<ul>\n");
                    inUl = true;
                }
                out.append("<li>").append(inline(raw.substring(2))).append("</li>\n");
                continue;
            }

            // -- Ordered list -----------------------------------------------
            if (raw.matches("^\\d+[.)].+")) {
                flushParagraph(out, paraLines);
                if (inUl) {
                    out.append("</ul>\n");
                    inUl = false;
                }
                if (!inOl) {
                    out.append("<ol>\n");
                    inOl = true;
                }
                String content = raw.replaceFirst("^\\d+[.)]\\s*", "");
                out.append("<li>").append(inline(content)).append("</li>\n");
                continue;
            }

            // -- Regular paragraph text -------------------------------------
            if (inUl || inOl) {
                // indented continuation → treat as new paragraph after closing list
                closeLists(out, inUl, inOl);
                inUl = inOl = false;
            }
            paraLines.add(inline(raw.trim()));
        }

        // Flush remaining
        flushParagraph(out, paraLines);
        if (inCode) out.append("</code></pre>\n");
        if (inUl) out.append("</ul>\n");
        if (inOl) out.append("</ol>\n");

        return out.toString();
    }

    private void flushParagraph(StringBuilder out, List<String> lines) {
        if (lines.isEmpty()) return;
        out.append("<p>").append(String.join(" ", lines)).append("</p>\n");
        lines.clear();
    }

    private void flushAndCloseLists(StringBuilder out, List<String> lines, boolean inUl, boolean inOl) {
        flushParagraph(out, lines);
        closeLists(out, inUl, inOl);
    }

    /**
     * Closes open lists, returns false (for inUl assignment).
     */
    private boolean closeLists(StringBuilder out, boolean inUl, boolean inOl) {
        if (inUl) out.append("</ul>\n");
        if (inOl) out.append("</ol>\n");
        return false;
    }

    /**
     * Apply inline markdown: bold, italic, code, links. HTML-escapes first.
     */
    private String inline(String text) {
        text = escapeHtml(text);
        // Bold: **text** or __text__
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        text = text.replaceAll("__(.+?)__", "<b>$1</b>");
        // Italic: *text* (after bold so ** is not consumed as two *)
        text = text.replaceAll("\\*([^*]+?)\\*", "<i>$1</i>");
        // Inline code: `code`
        text = text.replaceAll("`([^`]+?)`", "<code>$1</code>");
        // Links: [label](url)
        text = text.replaceAll("\\[([^\\]]+?)\\]\\(([^)]+?)\\)", "<a href=\"$2\">$1</a>");
        return text;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String wrapHtml(String body) {
        // Colors from AppTheme palette
        // BG_PANEL #1F2032, FG #E6E6E6, ACCENT #FF7100, BUTTON_BG #03529F,
        // BG #141622, CONSOLE_FG #E0FFEF, FG_MUTED #B0B0B0
        return """
                <html>
                <head>
                <style type="text/css">
                body   { color: #E6E6E6; margin: 20px; font-size: 18pt; }
                h1     { color: #FF7100; font-size: 30pt; }
                h2     { color: #FF7100; font-size: 26pt; }
                h3     { color: #FF7100; font-size: 22pt; }
                h4     { color: #FF7100; font-size: 20pt; }
                a      { color: #4E9AF1; }
                b      { color: #FFFFFF; }
                code   { font-family: monospace; color: #E0FFEF; background-color: #141622; }
                pre    { font-family: monospace; color: #E0FFEF; background-color: #141622; padding: 10px; }
                blockquote { color: #B0B0B0; margin-left: 16px; }
                hr     { color: #03529F; }
                ul, ol { margin-left: 24px; }
                li     { margin-bottom: 4px; }
                </style>
                </head>
                <body>
                """ + body + """
                </body>
                </html>
                """;
    }
}
