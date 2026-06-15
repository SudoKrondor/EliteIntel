package elite.intel.devices.model;

/**
 * Translates between SDL3 button/axis indices and Elite Dangerous {@code .binds} token format.
 */
public class ButtonInputMapper {

    private static final String[] AXIS_TOKENS = {
            "Joy_XAxis", "Joy_YAxis", "Joy_ZAxis", "Joy_RXAxis", "Joy_RYAxis", "Joy_RZAxis"
    };

    private ButtonInputMapper() {}

    /** 0-based SDL3 button index -> "Joy_N" (1-based). */
    public static String toBindsToken(int sdlButtonIndex) {
        return "Joy_" + (sdlButtonIndex + 1);
    }

    /** "Joy_N" (1-based) -> 0-based SDL3 button index. */
    public static int fromBindsToken(String token) {
        if (token == null || !token.startsWith("Joy_")) {
            throw new IllegalArgumentException("Not a Joy_N button token: " + token);
        }
        try {
            return Integer.parseInt(token.substring(4)) - 1;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a Joy_N button token: " + token, e);
        }
    }

    /** 0-based SDL3 axis index -> "Joy_XAxis"/"Joy_YAxis"/etc. */
    public static String axisToBindsToken(int sdlAxisIndex) {
        if (sdlAxisIndex < 0 || sdlAxisIndex >= AXIS_TOKENS.length) {
            throw new IllegalArgumentException("Unsupported SDL3 axis index: " + sdlAxisIndex);
        }
        return AXIS_TOKENS[sdlAxisIndex];
    }
}
