package elite.intel.ai.hands;

import java.util.Map;

/**
 * Converts Frontier's layout-dependent character names into native character-position codes.
 * <p>
 * This resolver is used only for keys read from Elite Dangerous {@code .binds} files.
 * It does not affect text entry or raw-key commands.
 */
final class FrontierBindingKeyResolver {
    private static final Map<String, Character> NAMED_CHARACTERS = Map.ofEntries(
            Map.entry("KEY_SUPERSCRIPTTWO", '²'),
            Map.entry("KEY_LEFTPARENTHESIS", '('),
            Map.entry("KEY_RIGHTPARENTHESIS", ')'),
            Map.entry("KEY_CIRCUMFLEX", '^'),
            Map.entry("KEY_DOLLAR", '$'),
            Map.entry("KEY_ASTERISK", '*'),
            Map.entry("KEY_COMMA", ','),
            Map.entry("KEY_SEMICOLON", ';'),
            Map.entry("KEY_COLON", ':'),
            Map.entry("KEY_EXCLAMATIONPOINT", '!'),
            Map.entry("KEY_LESSTHAN", '<'),
            Map.entry("KEY_GREATERTHAN", '>'),
            Map.entry("KEY_MINUS", '-'),
            Map.entry("KEY_PERIOD", '.'),
            Map.entry("KEY_HASH", '#'),
            Map.entry("KEY_ACUTE", '´'),
            Map.entry("KEY_PLUS", '+'),
            Map.entry("KEY_GRAVE", '`'),
            Map.entry("KEY_EQUALS", '='),
            Map.entry("KEY_LEFTBRACKET", '['),
            Map.entry("KEY_RIGHTBRACKET", ']'),
            Map.entry("KEY_APOSTROPHE", '\''),
            Map.entry("KEY_BACKSLASH", '\\'),
            Map.entry("KEY_SLASH", '/'),
            Map.entry("KEY_TILDE", '~'),
            Map.entry("KEY_DOUBLEQUOTE", '"'),
            Map.entry("KEY_UMLAUT", '¨'),
            Map.entry("KEY_RING", '°'),
            Map.entry("KEY_HALF", '½'),
            Map.entry("KEY_MACRON", '¯'),
            Map.entry("KEY_UNDERLINE", '_'),
            Map.entry("KEY_AMPERSAND", '&'),
            Map.entry("KEY_CEDILLA", '¸'),
            Map.entry("KEY_AT", '@')
    );

    private FrontierBindingKeyResolver() {
    }

    static Integer resolve(String eliteKeyName, Map<String, Integer> standardMappings) {
        if (eliteKeyName == null || eliteKeyName.isBlank()) {
            return null;
        }

        String normalized = eliteKeyName.toUpperCase(java.util.Locale.ROOT);
        Character character = NAMED_CHARACTERS.get(normalized);
        if (character != null) {
            return KeyProcessor.nativeCharacterCode(character);
        }

        Integer standard = standardMappings.get(normalized);
        if (standard != null) {
            return standard;
        }

        // Frontier also writes literal non-ASCII characters, e.g. Key_é, Key_ä or Key_ñ.
        String prefix = "Key_";
        if (eliteKeyName.startsWith(prefix) && eliteKeyName.length() == prefix.length() + 1) {
            char value = eliteKeyName.charAt(prefix.length());
            if (!Character.isLetterOrDigit(value) || value > 0x7F) {
                return KeyProcessor.nativeCharacterCode(value);
            }
        }

        return null;
    }
}
