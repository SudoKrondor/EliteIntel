package elite.intel.db.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class DatabaseMigrator {

    private static final Logger log = LogManager.getLogger(DatabaseMigrator.class);
    private static final Pattern MIGRATION_PATTERN = Pattern.compile("^(\\d{1,6})(__.*)?\\.sql$");
    private static final Pattern DROP_COLUMN_PATTERN =
            Pattern.compile("^\\s*ALTER\\s+TABLE\\s+.+\\s+DROP\\s+(COLUMN\\s+)?\\S+\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /**
     * Leading run of lines that carry no SQL: {@code --} comments and blank lines, in any mix.
     */
    private static final Pattern LEADING_NON_SQL_LINES = Pattern.compile("^(?:[ \\t]*--[^\\n]*\\n|[ \\t]*\\n)+");
    private static final String MIGRATIONS_PATH = "/db-migration";

    public static void migrate(Handle handle) throws Exception {
        handle.execute("PRAGMA journal_mode = WAL;");

        handle.execute("""
                CREATE TABLE IF NOT EXISTS schema_migration (
                    version TEXT PRIMARY KEY,
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """);

        Set<String> allFiles = findMigrationFiles();

        var applied = handle.createQuery("SELECT version FROM schema_migration")
                .mapTo(String.class)
                .set();

        for (String file : allFiles) {
            if (applied.contains(file)) continue;

            log.info("Applying migration: {}", file);

            String sql;
            try (var in = DatabaseMigrator.class.getResourceAsStream(MIGRATIONS_PATH + "/" + file)) {
                if (in == null) throw new IllegalStateException("Migration not found: " + file);
                sql = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                        .replace("\r\n", "\n");
            }

            for (String stmt : sql.split(";\\s*\n")) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty() || isTransactionControl(trimmed)) {
                    continue;
                }
                try {
                    handle.execute(trimmed);
                } catch (Exception e) {
                    if (isDuplicateColumn(e)) {
                        log.warn("Migration {}: column already exists, skipping: {}", file, abbreviate(trimmed));
                    } else if (isDropColumn(trimmed) && isMissingColumn(e)) {
                        log.warn("Migration {}: column already gone, skipping: {}", file, abbreviate(trimmed));
                    } else {
                        throw e;
                    }
                }
            }

            handle.execute("INSERT INTO schema_migration (version) VALUES (?)", file);
        }

        log.info("Migrations complete. Applied: {}", allFiles.size());
    }


    /** Returns true for bare transaction-control keywords that JDBI manages itself. */
    private static boolean isTransactionControl(String stmt) {
        String upper = stmt.toUpperCase();
        return upper.equals("BEGIN") || upper.equals("COMMIT") || upper.equals("ROLLBACK");
    }

    /**
     * Returns true if the exception chain indicates a SQLite "duplicate column name" error.
     * This happens when a migration's ALTER TABLE ADD COLUMN was already applied outside
     * the migration system (e.g. manual DB edit or schema applied before the migration existed).
     * In that case, the column already exists so the migration intent is fulfilled.
     */
    private static boolean isDuplicateColumn(Exception e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains("duplicate column name")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the exception chain indicates SQLite could not find a column the migration
     * referenced. Only meaningful in combination with {@link #isDropColumn(String)}.
     */
    private static boolean isMissingColumn(Exception e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains("no such column")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true for an {@code ALTER TABLE ... DROP COLUMN} statement.
     * <p>
     * WHY: "no such column" is only a benign outcome for a drop, where it means the column is
     * already gone and the statement's intent is fulfilled. Any other statement hitting that error
     * is a genuine defect (a misspelled column in an UPDATE, say), and must abort the run rather
     * than be logged and skipped, which would record the migration as applied while leaving the
     * database half migrated. Unlike "duplicate column name", which can only arise from ADD COLUMN,
     * this message needs the statement kind to disambiguate it.
     */
    static boolean isDropColumn(String statement) {
        return DROP_COLUMN_PATTERN.matcher(stripLeadingComments(statement)).find();
    }

    /**
     * Removes leading {@code --} comment and blank lines from a statement, exposing its first
     * SQL keyword.
     * <p>
     * WHY: statements are split on {@code ;} and only whitespace-trimmed, so a statement carries
     * whatever comment block preceded it. SQLite ignores those, but they push the keyword off the
     * front and would otherwise hide a DROP COLUMN from {@link #isDropColumn(String)}. Blank lines
     * have to be skipped too, since a section banner is separated from the statement it introduces
     * by an empty line. Most of the drops in 01018 are commented one of these two ways.
     */
    private static String stripLeadingComments(String statement) {
        return LEADING_NON_SQL_LINES.matcher(statement).replaceAll("").stripLeading();
    }

    /**
     * Collapses whitespace and clips a statement for a single-line log message.
     */
    private static String abbreviate(String statement) {
        String oneLine = statement.replaceAll("\\s+", " ");
        return oneLine.length() <= 80 ? oneLine : oneLine.substring(0, 80);
    }

    private static Set<String> findMigrationFiles() throws IOException, URISyntaxException {
        Set<String> files = new TreeSet<>();
        ClassLoader cl = DatabaseMigrator.class.getClassLoader();
        String path = "db-migration";

        Enumeration<URL> urls = cl.getResources(path);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String protocol = url.getProtocol();

            if ("jar".equals(protocol)) {
                String urlStr = url.toString();
                int sep = urlStr.indexOf("!/");
                String jarPart = urlStr.substring(0, sep);
                URI jarUri = URI.create(jarPart);

                try (FileSystem fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
                    Path root = fs.getPath("/" + path);
                    try (var walk = Files.walk(root)) {
                        walk.filter(Files::isRegularFile)
                                .map(p -> p.getFileName().toString())
                                .filter(n -> MIGRATION_PATTERN.matcher(n).matches())
                                .forEach(files::add);
                    }
                }
            } else if ("file".equals(protocol)) {
                Path dir = Paths.get(url.toURI());
                try (var walk = Files.walk(dir)) {
                    walk.filter(Files::isRegularFile)
                            .map(p -> p.getFileName().toString())
                            .filter(n -> MIGRATION_PATTERN.matcher(n).matches())
                            .forEach(files::add);
                }
            }
        }

        if (files.isEmpty()) {
            throw new IllegalStateException("No migration files found in classpath: db-migration");
        }
        return files;
    }
}