package elite.intel.gameapi.signals;

/**
 * One conflict zone as the FSS reports it: its intensity and its index within the system.
 * <p>
 * The index is what makes a zone distinct. A system with three low-intensity zones announces
 * {@code #index=1}, {@code #index=2} and {@code #index=3}, and one FSS sweep can list the same index
 * twice, so counting events overstates the system and counting signals by identity does not.
 *
 * @param index as the symbol carries it, or 0 when the symbol had none
 */
public record ConflictZoneSignal(ConflictZoneIntensity intensity, int index) {

    private static final String INDEX_MARKER = "#index=";

    /**
     * Parses a {@code $Warzone_...:#index=N;} symbol, or returns null when it is not a conflict zone.
     */
    public static ConflictZoneSignal fromSymbol(String symbol) {
        ConflictZoneIntensity intensity = ConflictZoneIntensity.fromSymbol(symbol);
        if (intensity == null) return null;
        return new ConflictZoneSignal(intensity, indexOf(symbol));
    }

    private static int indexOf(String symbol) {
        int at = symbol.indexOf(INDEX_MARKER);
        if (at < 0) return 0;
        int start = at + INDEX_MARKER.length();
        int end = start;
        while (end < symbol.length() && Character.isDigit(symbol.charAt(end))) end++;
        if (end == start) return 0;
        try {
            return Integer.parseInt(symbol.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * The identity of this zone within a sweep.
     */
    public String key() {
        return intensity.name() + "#" + index;
    }
}
