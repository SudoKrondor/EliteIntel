package elite.intel.gameapi.search.spansh.station.outfitting;

import java.util.List;

/**
 * A ship module the commander wants to buy, in the terms Spansh matches it on.
 * <p>
 * The designation is split the way a pilot says it - "size 6, class B" - and not the way Spansh files
 * it, where the size is the {@code class} and the class is the {@code rating}. That renaming happens once,
 * in the request body, and nowhere else.
 *
 * @param label     what the commander hears the request echoed as: the module's name, or for a broad request
 *                  such as "lasers" the family word ("Laser")
 * @param spellings every name Spansh is asked for: one module in every spelling the catalogue holds it under,
 *                  or every module of a family
 * @param size      the slot size 1 to 8, or null when the commander named none
 * @param rating    the quality letter A to E, or null when the commander named none
 * @param mount     for a weapon, Spansh's mount word ({@code Fixed}, {@code Gimbal} or {@code Turret}), or
 *                  null when the commander named none. Not a Spansh filter: it is applied to what comes back
 */
public record WantedModule(String label, List<String> spellings, Integer size, String rating, String mount) {

    public WantedModule {
        if (spellings == null || spellings.isEmpty()) {
            throw new IllegalArgumentException("A wanted module needs at least one name to search for");
        }
        spellings = List.copyOf(spellings);
        if (label == null || label.isBlank()) label = spellings.getFirst();
    }

    /**
     * One module, echoed under its canonical spelling, the first.
     */
    public WantedModule(List<String> spellings, Integer size, String rating, String mount) {
        this(null, spellings, size, rating, mount);
    }

    /**
     * Whether a broad request stands for several modules, so each listing has to say which one it is.
     */
    public boolean isFamily() {
        return spellings.stream().noneMatch(label::equalsIgnoreCase);
    }

    /**
     * Whether a listing is the module asked for: the name in any of its spellings, and the size, rating and
     * mount wherever one was stated.
     */
    public boolean matches(String name, Integer size, String rating, String mount) {
        if (name == null || spellings.stream().noneMatch(name::equalsIgnoreCase)) return false;
        if (this.size != null && !this.size.equals(size)) return false;
        if (this.rating != null && !this.rating.equalsIgnoreCase(rating)) return false;
        return this.mount == null || this.mount.equalsIgnoreCase(mount);
    }
}
