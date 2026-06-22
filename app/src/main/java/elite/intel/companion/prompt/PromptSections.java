package elite.intel.companion.prompt;

/**
 * Single owner of the prompt's markdown sectioning convention, shared by {@link CompanionSystemPromptPart}
 * and {@link PromptComposer} so the static and dynamic blocks are delimited the same way. Section
 * delimiters help the model parse structure; keeping the style here means changing it (e.g. to XML
 * tags) is one edit.
 */
final class PromptSections {

    private PromptSections() {
    }

    /** Appends a level-2 section header, preceded by a blank line unless the buffer is empty. */
    static void heading(StringBuilder sb, String title) {
        if (sb.length() > 0) {
            if (sb.charAt(sb.length() - 1) != '\n') {
                sb.append('\n');
            }
            sb.append('\n');
        }
        sb.append("## ").append(title).append('\n');
    }

    /** Appends a level-3 subsection header (no surrounding blank line; subsections are kept tight). */
    static void subheading(StringBuilder sb, String title) {
        sb.append("### ").append(title).append('\n');
    }
}
