package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

/**
 * The ship module catalogue: the English names Spansh matches an outfitting search on, and nothing else.
 * <p>
 * No language columns, unlike the commodity and material tables: Spansh knows module names in English
 * only, and the game hands us no per-language module list to translate from, so the commander's words are
 * matched against English whatever they speak (see {@code FuzzySearch#fuzzyShipModuleMatch}).
 */
public interface ShipModuleDao {

    @SqlQuery("SELECT LOWER(name) FROM ship_modules ORDER BY name")
    List<String> getAllNamesLowerCase();

    @SqlQuery("SELECT name FROM ship_modules ORDER BY name")
    List<String> getAllNames();

    @SqlQuery("SELECT name FROM ship_modules WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    String getOriginalCase(@Bind("name") String name);

    /**
     * Every spelling of a module the catalogue holds, spaces and case aside: the Mk II passenger cabins are
     * filed under both "Mk II" and "MkII", and a search sent with one of them misses every station Spansh
     * filed under the other. The given name always leads.
     */
    @SqlQuery("SELECT name FROM ship_modules"
            + " WHERE REPLACE(LOWER(name), ' ', '') = REPLACE(LOWER(:name), ' ', '')"
            + " ORDER BY CASE WHEN name = :name THEN 0 ELSE 1 END, name")
    List<String> getSpellings(@Bind("name") String name);
}
