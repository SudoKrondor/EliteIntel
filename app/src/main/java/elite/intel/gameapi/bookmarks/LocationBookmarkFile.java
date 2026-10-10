package elite.intel.gameapi.bookmarks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * The bookmark export file: what one commander writes out and another (or the same one, on another computer)
 * reads back in.
 * <p>
 * A JSON object with a format number and the bookmarks in the order the list shows them. Row ids are
 * not written - they mean nothing in another database - and a bookmark the commander never renamed is
 * written without a name, so it comes in named the way the importing app names places.
 */
public final class LocationBookmarkFile {

    static final int FORMAT = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * How an entry in a file compares with the bookmarks already saved.
     */
    public enum Status {
        /**
         * A new place; it can be imported.
         */
        NEW,
        /**
         * The same place is already bookmarked, or comes earlier in the file. Never imported.
         */
        DUPLICATE,
        /**
         * The entry does not describe a place - a missing system, an unknown kind, coordinates off the globe.
         */
        INVALID
    }

    /**
     * One entry from a file, and whether it can be imported.
     *
     * @param bookmark   the place, or null when the entry is {@link Status#INVALID}
     * @param starSystem the entry's system as written, so an invalid entry can still be shown
     * @param name       the entry's own name as written, likewise; null when it had none
     */
    public record Candidate(LocationBookmark bookmark, String starSystem, String name, Status status) {
        public boolean importable() {
            return status == Status.NEW;
        }
    }

    private record Document(Integer format, List<Entry> bookmarks) {
    }

    private record Entry(String kind, String starSystem, String stationName, String planetName,
                         Double latitude, Double longitude, String displayName) {
    }

    private LocationBookmarkFile() {
    }

    public static String toJson(List<LocationBookmark> bookmarks) {
        List<Entry> entries = bookmarks.stream()
                .map(b -> new Entry(b.kind().name(), b.starSystem(), b.stationName(), b.planetName(),
                        b.latitude(), b.longitude(), b.displayName()))
                .toList();
        return GSON.toJson(new Document(FORMAT, entries));
    }

    /**
     * Reads a file and sorts its entries against the bookmarks already saved.
     *
     * @throws IllegalArgumentException when the text is not a bookmark file at all
     */
    public static List<Candidate> parse(String json, List<LocationBookmark> existing) {
        Document document;
        try {
            document = GSON.fromJson(json, Document.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Not a bookmark file: " + e.getMessage(), e);
        }
        if (document == null || document.bookmarks() == null) {
            throw new IllegalArgumentException("Not a bookmark file: no bookmarks");
        }
        if (document.format() == null || document.format() > FORMAT) {
            throw new IllegalArgumentException("Unsupported bookmark file format: " + document.format());
        }

        List<LocationBookmark> seen = new ArrayList<>(existing);
        List<Candidate> candidates = new ArrayList<>();
        for (Entry entry : document.bookmarks()) {
            if (entry == null) continue;
            LocationBookmark bookmark = toBookmark(entry);
            if (bookmark == null) {
                candidates.add(new Candidate(null, entry.starSystem(), entry.displayName(), Status.INVALID));
                continue;
            }
            boolean duplicate = seen.stream().anyMatch(bookmark::samePlaceAs);
            seen.add(bookmark);
            candidates.add(new Candidate(bookmark, bookmark.starSystem(), bookmark.displayName(),
                    duplicate ? Status.DUPLICATE : Status.NEW));
        }
        return candidates;
    }

    /**
     * The place an entry describes, or null when it does not describe one.
     */
    private static LocationBookmark toBookmark(Entry entry) {
        if (blank(entry.starSystem()) || entry.kind() == null) return null;
        LocationBookmark.Kind kind;
        try {
            kind = LocationBookmark.Kind.valueOf(entry.kind());
        } catch (IllegalArgumentException e) {
            return null;
        }
        String system = entry.starSystem().strip();
        LocationBookmark bookmark = switch (kind) {
            case SYSTEM -> LocationBookmark.system(system);
            case STATION -> blank(entry.stationName()) ? null : LocationBookmark.station(system, entry.stationName());
            case PLANETARY_PORT -> blank(entry.stationName()) || blank(entry.planetName()) ? null
                    : LocationBookmark.planetaryPort(system, entry.planetName(), entry.stationName());
            case PLANET -> blank(entry.planetName()) ? null : LocationBookmark.planet(system, entry.planetName());
            case SURFACE -> blank(entry.planetName()) || !onGlobe(entry.latitude(), entry.longitude()) ? null
                    : LocationBookmark.surface(system, entry.planetName(), entry.latitude(), entry.longitude());
        };
        return bookmark == null ? null : bookmark.withDisplayName(entry.displayName());
    }

    private static boolean onGlobe(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
