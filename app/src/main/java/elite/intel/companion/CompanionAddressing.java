package elite.intel.companion;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Removes an optional leading vocative use of the configured companion name. */
public final class CompanionAddressing {

    private CompanionAddressing() {
    }

    /** Returns the command after one leading companion-name address, or the original text when none is present. */
    public static String stripLeadingName(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String alternatives = CompanionConfig.companionNameForms().stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> Pattern.quote(name.strip()))
                .collect(Collectors.joining("|"));
        if (alternatives.isEmpty()) {
            return input;
        }
        Pattern leadingName = Pattern.compile(
                "^\\s*(?:" + alternatives + ")\\b[\\s,.:;!?-]*",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        String stripped = leadingName.matcher(input).replaceFirst("");
        return stripped.isBlank() ? input : stripped;
    }
}
