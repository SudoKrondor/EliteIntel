package elite.intel.db.dao;

import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(MaterialsDao.MaterialMapper.class)
public interface MaterialsDao {

    @SqlUpdate("""
            INSERT INTO materials (materialName, materialType, amount, maxCapacity) 
            VALUES (:materialName, :materialType, :amount, :maxCapacity)
            ON CONFLICT(materialName) DO UPDATE SET
                amount = excluded.amount,
                maxCapacity = excluded.maxCapacity
            """)
    void upsert(@Bind("materialName") String materialName,
                @Bind("materialType") String materialType,
                @Bind("amount") int amount,
                @Bind("maxCapacity") int maxCap);


    @SqlQuery("SELECT LOWER(materialName) FROM materials ORDER BY materialName")
    List<String> getAllNamesLowerCase();

    @SqlQuery("SELECT materialName FROM materials WHERE LOWER(materialName) = LOWER(:materialName) LIMIT 1")
    String getOriginalCase(@Bind("materialName") String materialName);

    // JOIN with material_names to get localized display names for inventory items;
    // falls back to materialName when no translation exists for the given language column.
    @SqlQuery("""
            SELECT LOWER(COALESCE(mn.<col>, m.materialName))
            FROM materials m
            LEFT JOIN material_names mn ON LOWER(m.materialName) = LOWER(mn.name)
            ORDER BY COALESCE(mn.<col>, m.materialName)
            """)
    List<String> getAllLocalizedNamesLowerCase(@Define("col") String col);

    @SqlQuery("""
            SELECT m.materialName
            FROM materials m
            LEFT JOIN material_names mn ON LOWER(m.materialName) = LOWER(mn.name)
            WHERE LOWER(COALESCE(mn.<col>, m.materialName)) = LOWER(:localizedName)
            LIMIT 1
            """)
    String getEnglishByLocalizedName(@Define("col") String col, @Bind("localizedName") String localizedName);

    @SqlQuery("SELECT * FROM materials WHERE LOWER(materialName) = LOWER(:materialName)")
    Material findByExactName(String materialName);

    @SqlQuery("SELECT * FROM materials WHERE LOWER(materialName) LIKE LOWER(:pattern)")
    List<Material> search(@Bind("pattern") String pattern);

    @SqlQuery("SELECT * FROM materials WHERE materialType = :materialType")
    List<Material> findByMaterialType(@Bind("materialType") String materialType);

    @SqlQuery("SELECT * FROM materials")
    List<Material> listAll();

    @SqlUpdate("DELETE FROM materials")
    void clear();


    @SqlUpdate("""
            UPDATE materials
            SET amount = (SELECT MAX(amount - :amountUsed, 0) FROM materials WHERE materialName = :materialName)
            WHERE materialName = :materialName
            """)
    void updateAmount(@Bind("materialName") String materialName, @Bind("amountUsed") Integer amountUsed );


    class MaterialMapper implements RowMapper<Material> {
        @Override
        public Material map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Material(
                    rs.getLong("id"),
                    rs.getString("materialName"),
                    rs.getString("materialType"),
                    rs.getInt("amount"),
                    rs.getInt("maxCapacity")
            );
        }
    }

    class Material implements ToJsonConvertible {
        private long id;
        private String materialName;
        private String materialType;
        private int amount;
        private int maxCap;

        public Material(long id, String materialName, String materialType, int amount, int maxCap) {
            this.id = id;
            this.materialName = materialName;
            this.materialType = materialType;
            this.amount = amount;
            this.maxCap = maxCap;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public String getMaterialType() {
            return materialType;
        }

        public void setMaterialType(String materialType) {
            this.materialType = materialType;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public int getMaxCap() {
            return maxCap;
        }

        public void setMaxCap(int maxCap) {
            this.maxCap = maxCap;
        }

        @Override
        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }
}

