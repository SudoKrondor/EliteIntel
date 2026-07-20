package elite.intel.ai.brain.vega.prompt;

/**
 * Small XML-fragment helper for prompt payloads that must mark trusted data without letting its text break tags.
 */
public final class PromptXml {

    private PromptXml() {
    }

    /** Wraps text in a simple prompt XML element, escaping XML metacharacters in the text content. */
    public static String element(String tag, String text) {
        return "<" + tag + ">\n" + text(text) + "\n</" + tag + ">";
    }

    /** Escapes XML metacharacters in text embedded inside prompt XML tags. */
    public static String text(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
