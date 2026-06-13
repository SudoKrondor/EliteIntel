package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface MaterialNameDao {

    @SqlQuery("SELECT LOWER(name) FROM material_names ORDER BY name")
    List<String> getAllNamesLowerCase();

    @SqlQuery("SELECT name FROM material_names WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    String getOriginalCase(@Bind("name") String name);

    @SqlQuery("SELECT LOWER(COALESCE(<col>, name)) FROM material_names ORDER BY COALESCE(<col>, name)")
    List<String> getAllLocalizedNamesLowerCase(@Define("col") String col);

    @SqlQuery("SELECT name FROM material_names WHERE LOWER(COALESCE(<col>, name)) = LOWER(:localizedName) LIMIT 1")
    String getEnglishByLocalizedName(@Define("col") String col, @Bind("localizedName") String localizedName);

    // optional – one-time init if table empty
    @SqlQuery("SELECT COUNT(*) FROM material_names")
    int count();

    @SqlBatch("INSERT OR IGNORE INTO material_names (name) VALUES (:name)")
    void insertAll(@Bind("name") List<String> names);

}
