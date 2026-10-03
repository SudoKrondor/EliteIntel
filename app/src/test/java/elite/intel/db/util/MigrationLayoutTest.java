package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Holds the two migration trees to the rules that keep them sequential and apart.
 * <p>
 * The shared file and each commander's file are built by separate trees, and a table must live in exactly one
 * of them: if a name existed in both, SQLite would silently resolve it in the shared file and one commander's
 * rows would be read as everyone's. Numbers are banded by release (the first two digits are the version), so a
 * number says which release introduced a change, and two writers can never collide inside one tree.
 */
class MigrationLayoutTest {

    /**
     * The last number written before the version-banded rule. Older files keep their numbers forever.
     */
    private static final int LAST_LEGACY_NUMBER = 1058;

    /**
     * Releases whose band is open: V1.1 and V1.2. Add the next one when its integration branch opens.
     */
    private static final Set<Integer> OPEN_BANDS = Set.of(11, 12);

    private static final Pattern NUMBER = Pattern.compile("^(\\d{1,6})__");

    @Test
    void noTwoFilesInOneTreeShareANumber() throws Exception {
        for (DatabaseMigrator.Tree tree : DatabaseMigrator.Tree.values()) {
            Map<Integer, String> seen = new HashMap<>();
            for (String file : DatabaseMigrator.findMigrationFiles(tree)) {
                String clash = seen.put(numberOf(file), file);
                assertTrue(clash == null, tree + " tree: " + file + " and " + clash + " share a number");
            }
        }
    }

    @Test
    void everyNewFileSitsInAnOpenReleaseBand() throws Exception {
        for (DatabaseMigrator.Tree tree : DatabaseMigrator.Tree.values()) {
            for (String file : DatabaseMigrator.findMigrationFiles(tree)) {
                int number = numberOf(file);
                if (number <= LAST_LEGACY_NUMBER && tree == DatabaseMigrator.Tree.SHARED) continue;
                assertTrue(number >= 10_000 && OPEN_BANDS.contains(number / 1000),
                        tree + " tree: " + file + " is outside the release bands (11XXX = V1.1, 12XXX = V1.2)");
            }
        }
    }

    @Test
    void noMigrationWrittenUnderTheBandRuleHasASemicolonInAComment() throws Exception {
        // DatabaseMigrator splits on a semicolon at end of line before comments are stripped, so one inside a
        // comment cuts the file mid-sentence and hands SQLite a statement with no SQL in it.
        for (DatabaseMigrator.Tree tree : DatabaseMigrator.Tree.values()) {
            for (String file : DatabaseMigrator.findMigrationFiles(tree)) {
                if (numberOf(file) <= LAST_LEGACY_NUMBER) continue;
                for (String line : read(tree, file).split("\n")) {
                    int comment = line.indexOf("--");
                    assertTrue(comment < 0 || !line.substring(comment).contains(";"),
                            tree + " tree: " + file + " has a semicolon inside a comment: " + line.trim());
                }
            }
        }
    }

    @Test
    void afterTheSplitEveryTableLivesInExactlyOneFile() throws Exception {
        try (Built built = Built.freshAndSplit()) {
            Set<String> shared = tables(built.handle, "main");
            Set<String> commander = tables(built.handle, CommanderSplit.SCHEMA);
            Set<String> both = new TreeSet<>(shared);
            both.retainAll(commander);
            both.remove("schema_migration");
            assertTrue(both.isEmpty(), "tables in both the shared and the commander file: " + both);
        }
    }

    @Test
    void theSplitMovesExactlyTheTablesTheCommanderTreeBuilds() throws Exception {
        try (Built built = Built.freshAndSplit()) {
            Set<String> commander = tables(built.handle, CommanderSplit.SCHEMA);
            commander.remove("schema_migration");
            Set<String> expected = new TreeSet<>(CommanderSplit.COMMANDER_TABLES);
            expected.addAll(CommanderSplit.COMMANDER_HALVES);
            assertEquals(expected, commander,
                    "CommanderSplit.COMMANDER_TABLES + COMMANDER_HALVES and the commander tree disagree");
        }
    }

    @Test
    void noForeignKeyReachesIntoTheOtherFile() throws Exception {
        try (Built built = Built.freshAndSplit()) {
            for (String schema : List.of("main", CommanderSplit.SCHEMA)) {
                Set<String> own = tables(built.handle, schema);
                for (String table : own) {
                    List<String> targets = built.handle
                            .createQuery("SELECT \"table\" FROM pragma_foreign_key_list(:t, :s)")
                            .bind("t", table).bind("s", schema)
                            .mapTo(String.class).list();
                    for (String target : targets) {
                        assertTrue(own.contains(target),
                                schema + "." + table + " has a foreign key onto " + target + " in the other file");
                    }
                }
            }
        }
    }

    private static int numberOf(String file) {
        Matcher m = NUMBER.matcher(file);
        assertTrue(m.find(), file + " does not start with a number");
        return Integer.parseInt(m.group(1));
    }

    private static String read(DatabaseMigrator.Tree tree, String file) throws IOException {
        try (InputStream in = MigrationLayoutTest.class.getResourceAsStream("/" + tree.path() + "/" + file)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static Set<String> tables(Handle handle, String schema) {
        return new TreeSet<>(handle.createQuery("SELECT name FROM " + schema
                        + ".sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")
                .mapTo(String.class).list());
    }

    /**
     * A shared database built from its whole tree, a commander file built from its own, attached and split: the
     * state every install reaches.
     */
    static final class Built implements AutoCloseable {
        final Handle handle;
        private final Handle commanderKeeper;

        private Built(Handle handle, Handle commanderKeeper) {
            this.handle = handle;
            this.commanderKeeper = commanderKeeper;
        }

        static Built freshAndSplit() throws Exception {
            Built built = fresh();
            CommanderSplit.runIfNeeded(built.handle);
            CommanderSplit.splitMixedIfNeeded(built.handle);
            return built;
        }

        /**
         * Both trees applied and the commander file attached, but not yet split.
         */
        static Built fresh() throws Exception {
            String name = UUID.randomUUID().toString().replace("-", "");
            String sharedUrl = "jdbc:sqlite:file:layout_shared_" + name + "?mode=memory&cache=shared";
            String commanderLocation = "file:layout_cmdr_" + name + "?mode=memory&cache=shared";

            Handle commander = Jdbi.create("jdbc:sqlite:" + commanderLocation).open();
            DatabaseMigrator.migrate(commander, DatabaseMigrator.Tree.COMMANDER);

            Handle shared = Jdbi.create(sharedUrl).open();
            DatabaseMigrator.migrate(shared, DatabaseMigrator.Tree.SHARED);
            shared.execute("ATTACH DATABASE '" + commanderLocation + "' AS " + CommanderSplit.SCHEMA);
            return new Built(shared, commander);
        }

        @Override
        public void close() {
            handle.close();
            commanderKeeper.close();
        }
    }
}
