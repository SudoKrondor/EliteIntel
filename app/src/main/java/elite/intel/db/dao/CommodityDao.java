package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface CommodityDao {

    @SqlQuery("SELECT LOWER(commodity) FROM commodities ORDER BY commodity")
    List<String> getAllNamesLowerCase();

    @SqlQuery("SELECT commodity FROM commodities WHERE LOWER(commodity) = LOWER(:name) LIMIT 1")
    String getOriginalCase(@Bind("name") String name);

    @SqlQuery("SELECT LOWER(COALESCE(<col>, commodity)) FROM commodities ORDER BY COALESCE(<col>, commodity)")
    List<String> getAllLocalizedNamesLowerCase(@Define("col") String col);

    @SqlQuery("SELECT commodity FROM commodities WHERE LOWER(COALESCE(<col>, commodity)) = LOWER(:localizedName) LIMIT 1")
    String getEnglishByLocalizedName(@Define("col") String col, @Bind("localizedName") String localizedName);

    @SqlQuery("SELECT <col> FROM commodities WHERE LOWER(commodity) = LOWER(:englishName) LIMIT 1")
    String getLocalizedByEnglishName(@Define("col") String col, @Bind("englishName") String englishName);

    // optional – one-time init if table empty
    @SqlQuery("SELECT COUNT(*) FROM commodities")
    int count();

    @SqlBatch("INSERT OR IGNORE INTO commodities (commodity) VALUES (:name)")
    void insertAll(@Bind("name") List<String> names);
}