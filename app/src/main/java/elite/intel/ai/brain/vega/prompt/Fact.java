package elite.intel.ai.brain.vega.prompt;

import java.util.Objects;
import java.util.regex.Pattern;

/** One prompt fact plus a safe provenance identifier for the {@code source} XML attribute. */
public record Fact(String text, String source) {

    private static final Pattern SOURCE_ID = Pattern.compile("[a-z][a-z0-9_]*");

    public Fact {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(source, "source");
        if (text.isBlank()) {
            throw new IllegalArgumentException("Fact text must not be blank");
        }
        if (!SOURCE_ID.matcher(source).matches()) {
            throw new IllegalArgumentException("Invalid fact source id: " + source);
        }
    }
}
