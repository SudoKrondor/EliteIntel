package elite.intel.db.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.util.*;

/**
 * Moves the commander's own tables out of the shared database into the commander's file, once.
 * <p>
 * Until v1.1.0020 there was one database, and every commander played on the machine wrote into the same tables.
 * From then on each commander has a file of their own, attached as {@value #SCHEMA}. Every database passes
 * through this step exactly once: an upgraded one carries its data across, and a fresh one (whose legacy
 * migrations still create these tables in the shared file) moves nothing and simply loses the empty tables.
 * <p>
 * Data that had already leaked between commanders is moved as it is: V1.1 did not support several commanders
 * before this, and sorting the mix out is not attempted.
 * <p>
 * WHY copy everything first and drop afterwards: the app runs in WAL mode, where SQLite does not make one
 * transaction atomic across attached files. Nothing is dropped from the shared file until every table has been
 * copied and its row count checked, and each copy starts by emptying its target, so a crash at any point simply
 * repeats the step on the next start.
 */
final class CommanderSplit {

    private static final Logger log = LogManager.getLogger(CommanderSplit.class);

    static final String SCHEMA = "cmdr";

    /**
     * Every table that lives in the commander's file, parents before the tables whose foreign keys point at them.
     * Must match the commander tree ({@code db-migration/commander/}); {@code MigrationLayoutTest} holds the two
     * together.
     */
    static final List<String> COMMANDER_TABLES = List.of(
            "player",
            "player_status",
            "ranks_and_progress",
            "reputation",
            "ship",
            "ship_loadout",
            "ship_settings",
            "trade_profile",
            "cargo",
            "missions",
            "massacre_mission",
            "bounties",
            "combat_bond",
            "bio_samples",
            "ship_scans",
            "codex_entries",
            "fleet_carrier",
            "fleet_carrier_route",
            "squadron_carrier",
            "squadron_carrier_route",
            "ship_route",
            "neutron_star_route",
            "fsd_target",
            "target_location",
            "trade_route",
            "trade_tuple",
            "destination_reminder",
            "deferred_notifications",
            "mining_targets",
            "construction_site",
            "construction_requirement",
            "commodity_search_result",
            "commodity_search_line",
            "chat_history"
    );

    /**
     * The commander's half of the tables that stay shared (see {@link #splitMixedIfNeeded}). New in the commander
     * tree rather than moved there, so the split does not copy them whole.
     */
    static final List<String> COMMANDER_HALVES = List.of(
            "material_inventory",
            "hunting_ground_forgotten",
            "exo_mastery_harvest",
            "exo_mastery_sample",
            "location_visit"
    );

    private CommanderSplit() {
    }

    /**
     * Runs the split if any commander table is still in the shared file. The handle must have the commander's
     * file attached as {@value #SCHEMA}, with the commander tree already applied to it.
     */
    static void runIfNeeded(Handle handle) {
        List<String> pending = COMMANDER_TABLES.stream().filter(t -> existsIn(handle, "main", t)).toList();
        if (pending.isEmpty()) return;

        log.info("Moving {} commander tables into the commander's own database", pending.size());
        // Foreign keys stay off while moving: a child is copied after its parent, and dropping a parent in the
        // shared file must not cascade into a child that is still waiting to be dropped. The pragma cannot change
        // inside a transaction, so it is set on the connection around the whole step.
        handle.execute("PRAGMA foreign_keys = OFF");
        try {
            for (String table : pending) {
                copy(handle, table);
            }
            List<String> dropOrder = new ArrayList<>(pending);
            java.util.Collections.reverse(dropOrder);
            for (String table : dropOrder) {
                handle.execute("DROP TABLE main." + table);
            }
        } finally {
            handle.execute("PRAGMA foreign_keys = ON");
        }
        log.info("Commander tables moved");
    }

    /**
     * Moves the commander's half of the four mixed tables into their file, once each.
     * <p>
     * These tables are shared, but one or two columns in each recorded what this commander did. A moved column is
     * dropped from the shared table afterwards, which is also the record that the step is done. The location
     * flags live inside each row's JSON, so that step is recorded in {@code commander_split_step} instead.
     * <p>
     * Each step copies before it drops and empties its target before copying, so an interrupted step simply runs
     * again on the next start.
     */
    static void splitMixedIfNeeded(Handle handle) {
        moveColumn(handle, "material_names", List.of("amount"),
                "INSERT INTO " + SCHEMA + ".material_inventory (symbol, amount) "
                        + "SELECT symbol, amount FROM main.material_names WHERE amount > 0 AND symbol IS NOT NULL",
                "material_inventory", List.of(), List.of());
        moveColumn(handle, "hunting_ground", List.of("forgotten"),
                "INSERT INTO " + SCHEMA + ".hunting_ground_forgotten (starSystem) "
                        + "SELECT starSystem FROM main.hunting_ground WHERE forgotten = 1",
                "hunting_ground_forgotten", List.of(), List.of());
        // The body index covered the completed flag, and SQLite will not drop a column an index uses.
        moveColumn(handle, "exo_mastery_body", List.of("completed", "completedAt"),
                "INSERT INTO " + SCHEMA + ".exo_mastery_harvest (systemAddress, bodyId, value, completedAt) "
                        + "SELECT systemAddress, bodyId, value, completedAt FROM main.exo_mastery_body WHERE completed",
                "exo_mastery_harvest",
                List.of("DROP INDEX IF EXISTS main.idx_exo_mastery_body_system"),
                List.of("CREATE INDEX IF NOT EXISTS main.idx_exo_mastery_body_system ON exo_mastery_body (systemAddress)"));
        moveColumn(handle, "exo_mastery_species", List.of("sampled"),
                "INSERT INTO " + SCHEMA + ".exo_mastery_sample (systemAddress, bodyId, speciesSymbol) "
                        + "SELECT systemAddress, bodyId, speciesSymbol FROM main.exo_mastery_species WHERE sampled",
                "exo_mastery_sample", List.of(), List.of());
        moveLocationFlags(handle);
    }

    private static void moveColumn(Handle handle, String table, List<String> columns, String copy, String target,
                                   List<String> beforeDrop, List<String> afterDrop) {
        if (!existsIn(handle, "main", table) || !columnsOf(handle, "main", table).contains(columns.getFirst())) return;
        log.info("Moving {}.{} into the commander's own database", table, columns);
        handle.useTransaction(tx -> {
            tx.execute("DELETE FROM " + SCHEMA + "." + target);
            tx.execute(copy);
        });
        beforeDrop.forEach(handle::execute);
        for (String column : columns) {
            handle.execute("ALTER TABLE main." + table + " DROP COLUMN " + column);
        }
        afterDrop.forEach(handle::execute);
    }

    private static final String LOCATION_STEP = "location_flags";

    /**
     * Lifts the commander's fields out of every shared location row that carries any, into their
     * {@code location_visit}, and resets them to their defaults in the shared row. See {@link LocationFlags}.
     */
    private static void moveLocationFlags(Handle handle) {
        if (!existsIn(handle, "main", "location")) return;
        boolean done = handle.createQuery("SELECT COUNT(*) FROM main.commander_split_step WHERE step = :s")
                .bind("s", LOCATION_STEP).mapTo(Integer.class).one() > 0;
        if (done) return;

        List<Map.Entry<String, String>> rows = handle.createQuery("""
                        SELECT locationName, json FROM main.location
                         WHERE json ->> '$.ourDiscovery' = 1
                            OR json ->> '$.weMappedIt' = 1
                            OR json ->> '$.bioScansCompleted' = 1
                            OR json ->> '$.isHomeSystem' = 1
                            OR json_array_length(json, '$.partialBioSamples') > 0
                        """)
                .map((rs, ctx) -> Map.entry(rs.getString("locationName"), rs.getString("json")))
                .list();
        log.info("Moving the commander's own fields of {} locations into their database", rows.size());

        // Visits first, then the shared rows: stopped between the two, the step runs again and finds the same
        // rows still flagged in the shared file.
        handle.useTransaction(tx -> {
            PreparedBatch visits = tx.prepareBatch("""
                    INSERT INTO %s.location_visit (locationName, flags) VALUES (:n, :f)
                    ON CONFLICT(locationName) DO UPDATE SET flags = excluded.flags
                    """.formatted(SCHEMA));
            PreparedBatch shared = tx.prepareBatch("UPDATE main.location SET json = :j WHERE locationName = :n");
            for (Map.Entry<String, String> row : rows) {
                LocationFlags.Split split = LocationFlags.split(row.getValue());
                if (split.flags() != null) {
                    visits.bind("n", row.getKey()).bind("f", split.flags()).add();
                }
                shared.bind("n", row.getKey()).bind("j", split.sharedJson()).add();
            }
            if (visits.size() > 0) visits.execute();
            if (shared.size() > 0) shared.execute();
            tx.execute("INSERT INTO main.commander_split_step (step) VALUES (?)", LOCATION_STEP);
        });
    }

    private static void copy(Handle handle, String table) {
        if (!existsIn(handle, SCHEMA, table)) {
            throw new IllegalStateException("Commander tree does not create " + table + ", so it cannot be moved");
        }
        // Only the columns both sides have: the commander tree may have grown a column since, which takes its
        // default, and the shared table keeps columns that moved elsewhere (the user's half of player).
        Set<String> columns = new LinkedHashSet<>(columnsOf(handle, SCHEMA, table));
        columns.retainAll(columnsOf(handle, "main", table));
        String list = String.join(", ", columns.stream().map(c -> '"' + c + '"').toList());

        handle.useTransaction(tx -> {
            // The baseline seeds some rows (player, player_status). The moved data replaces them.
            tx.execute("DELETE FROM " + SCHEMA + "." + table);
            tx.execute("INSERT INTO " + SCHEMA + "." + table + " (" + list + ") SELECT " + list + " FROM main." + table);
        });

        long source = count(handle, "main", table);
        long target = count(handle, SCHEMA, table);
        if (source != target) {
            throw new IllegalStateException("Moving " + table + " copied " + target + " of " + source + " rows");
        }
        // An empty shared table must not leave the commander's file without the rows the baseline seeds.
        if (source == 0 && ("player".equals(table) || "player_status".equals(table))) {
            reseed(handle, table);
        }
    }

    private static void reseed(Handle handle, String table) {
        if ("player".equals(table)) {
            handle.execute("INSERT OR IGNORE INTO " + SCHEMA + ".player (id) VALUES (1)");
        } else {
            handle.execute("INSERT OR IGNORE INTO " + SCHEMA + ".player_status (id, timestamp, event) "
                    + "VALUES (1, '1984-01-01 00:00:00', 'init')");
        }
    }

    static boolean existsIn(Handle handle, String schema, String table) {
        return handle.createQuery("SELECT COUNT(*) FROM " + schema + ".sqlite_master WHERE type = 'table' AND name = :t")
                .bind("t", table)
                .mapTo(Integer.class)
                .one() > 0;
    }

    private static List<String> columnsOf(Handle handle, String schema, String table) {
        return handle.createQuery("SELECT name FROM pragma_table_info(:t, :s)")
                .bind("t", table)
                .bind("s", schema)
                .mapTo(String.class)
                .list();
    }

    private static long count(Handle handle, String schema, String table) {
        return handle.createQuery("SELECT COUNT(*) FROM " + schema + "." + table).mapTo(Long.class).one();
    }
}
