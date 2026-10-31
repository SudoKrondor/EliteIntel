package elite.intel.db.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import elite.intel.gameapi.JournalCommander;
import elite.intel.session.DirectorySetting;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

public class Database {

    private static final Logger log = LogManager.getLogger(Database.class);

    /**
     * Where each commander's own file is, with {@value #FID_PLACEHOLDER} standing for the commander. Tests point
     * this at a named in-memory database. Unset, it is {@code cmdr_<FID>.db} beside the shared file.
     */
    private static final String COMMANDER_URL_PROPERTY = "elite.intel.db.commander.url";
    private static final String FID_PLACEHOLDER = "{fid}";

    /**
     * The file used while no journal has named a commander yet: a fresh install, or a journal folder that cannot
     * be read. Everything gathered before the commander is known lands here.
     */
    static final String PENDING_COMMANDER = "pending";

    /**
     * The connection settings every pooled connection opens the shared file with.
     * <p>
     * WHY {@code transaction_mode=IMMEDIATE}: a transaction can write both files (a location and the
     * commander's visit to it, a player row and the user's preferences), and SQLite's default deferred
     * transaction takes each file's write lock only when it first writes there. Two such transactions writing
     * the files in opposite orders each held the lock the other needed: both waited out the busy timeout and
     * failed with SQLITE_BUSY, losing the scan or the setting. IMMEDIATE takes the write lock on every attached
     * file at BEGIN, always in the same order, so a second writer just waits its turn.
     */
    static final String CONNECTION_OPTIONS = "?journal_mode=WAL"
            + "&busy_timeout=5000"
            + "&synchronous=NORMAL"
            + "&foreign_keys=ON"
            + "&transaction_mode=IMMEDIATE";

    private static final String SHARED_URL;

    /**
     * The open pool and the commander whose file it attaches. Swapped as one when the commander changes, so a
     * caller never pairs one commander's pool with another's name.
     */
    private record Pool(HikariDataSource dataSource, Jdbi jdbi, String commander) {
    }

    private static volatile Pool pool;

    /**
     * Another commander's file, opened for one thread's work only - see {@link #inCommanderFile}. Null on every
     * thread that has not asked, which reads the open commander's pool as always.
     */
    private static final ThreadLocal<Pool> SCOPED = new ThreadLocal<>();

    /**
     * Whether a scope has already started a commander's file from the pending one. One commander inherits it, not
     * every commander an import happens to find.
     */
    private static boolean pendingCopiedOut;

    /**
     * The pool this thread reads and writes through: the commander file it opened for itself, else the open one.
     */
    private static Pool active() {
        Pool scoped = SCOPED.get();
        return scoped != null ? scoped : pool;
    }

    public static <T, R> R withDao(Class<T> daoClass, java.util.function.Function<T, R> block) {
        // Use withExtension to get a thread-safe handle for this specific operation
        try {
            return active().jdbi().withExtension(daoClass, block::apply);
        } catch (Exception e) {
            throw new RuntimeException("DAO operation failed: " + daoClass.getSimpleName(), e);
        }
    }

    // shortcut if you just need a handle
    public static Handle init() {
        return active().jdbi().open();
    }

    /**
     * The commander whose file is attached: their FID, or {@value #PENDING_COMMANDER} before any journal has named
     * one.
     */
    public static String currentCommander() {
        return active().commander();
    }

    /**
     * False until a journal has named a commander, while the {@value #PENDING_COMMANDER} file is attached.
     */
    public static boolean isCommanderKnown() {
        return !PENDING_COMMANDER.equals(currentCommander());
    }

    // Shutdown the connection pool gracefully
    public static void shutdown() {
        Pool open = pool;
        if (open != null && !open.dataSource().isClosed()) {
            open.dataSource().close();
        }
    }

    /**
     * Opens {@code fid}'s own file in place of the current commander's. Returns false when that commander's file
     * is already the one open, or the FID is not one that can name a file.
     * <p>
     * Called while the journal parser is delivering that commander's {@code Commander} event. The game bus
     * dispatches on the parser's thread, so every event after it (LoadGame, Loadout, Materials, Location...)
     * is written only once this returns, into the new file.
     * <p>
     * The first commander seen on a fresh install inherits whatever was gathered into the
     * {@value #PENDING_COMMANDER} file before any journal had named them.
     * <p>
     * A write still in flight on the old pool finishes in the old commander's file: the pool is retired, not
     * cut off, so nothing half-written is lost.
     */
    public static synchronized boolean switchCommander(String fid) {
        if (!isUsableFid(fid) || PENDING_COMMANDER.equals(fid)) {
            log.warn("Refusing to open a database for commander id {}", fid);
            return false;
        }
        Pool old = pool;
        if (fid.equals(old.commander())) return false;

        String target = commanderLocation(fid);
        Optional<Path> adopted = pendingFileToAdopt(old, target);
        if (adopted.isPresent()) {
            try (Handle h = old.jdbi().open()) {
                h.execute("VACUUM " + CommanderSplit.SCHEMA + " INTO '" + target.replace("'", "''") + "'");
            }
        }

        try (Handle commanderBootstrap = Jdbi.create("jdbc:sqlite:" + target).open()) {
            commanderBootstrap.execute("PRAGMA journal_mode = WAL;");
            migrate(commanderBootstrap, DatabaseMigrator.Tree.COMMANDER);
            pool = openPool(target, fid);
        }
        pool.jdbi().useHandle(Database::applyPragmas);
        log.info("Switched database from commander {} to {}{}", old.commander(), fid,
                adopted.isPresent() ? " (adopted the data gathered before the commander was known)" : "");

        Thread.ofVirtual().name("retire-commander-pool").start(() -> {
            old.dataSource().close();
            adopted.ifPresent(Database::deleteDatabaseFile);
        });
        return true;
    }

    /**
     * Runs {@code work} on this thread against {@code fid}'s own file, whichever commander's file the app has open.
     * Every manager and DAO called from {@code work} lands its commander tables in that file and its shared tables
     * in the shared file, exactly as it would for the open commander. Other threads are not affected.
     * <p>
     * WHY a scope rather than switching: the journal import files years of several commanders' flying while the
     * game runs one of them. Switching the app's file would point the live parser at the wrong commander, and
     * routing every manager through an explicit handle would mean a second copy of each. The open commander gets a
     * scope of their own as well, so a commander switch in the game during the import cannot move its writes.
     * <p>
     * {@code work} may open one transaction around many manager calls ({@link #inScopedTransaction}): JDBI hands a
     * nested call on the same thread the handle already open.
     * <p>
     * A commander with no file yet gets one built from the commander tree. While the app is still on the
     * {@value #PENDING_COMMANDER} file, the first file made this way (the caller decides which commander goes first)
     * starts as a copy of it - the switch that would
     * have handed it over finds the file there and adopts nothing, and what the commander set up before any journal
     * named them (their name, their ships' voices) must not be left behind.
     *
     * @throws IllegalArgumentException when the FID is not one that can name a file
     */
    public static <R> R inCommanderFile(String fid, java.util.function.Supplier<R> work) {
        if (!isUsableFid(fid) || PENDING_COMMANDER.equals(fid)) {
            throw new IllegalArgumentException("Not a commander id: " + fid);
        }
        if (SCOPED.get() != null) {
            throw new IllegalStateException("Already working in commander " + SCOPED.get().commander() + "'s file");
        }
        Pool scoped = openScopedPool(fid);
        SCOPED.set(scoped);
        try {
            return work.get();
        } finally {
            SCOPED.remove();
            scoped.dataSource().close();
        }
    }

    /**
     * One transaction on this thread's pool, which every {@link #withDao} inside it joins.
     */
    public static void inScopedTransaction(Runnable work) {
        active().jdbi().useTransaction(h -> work.run());
    }

    private static synchronized Pool openScopedPool(String fid) {
        String target = commanderLocation(fid);
        Optional<Path> adopt = pendingCopiedOut ? Optional.empty() : pendingFileToAdopt(pool, target);
        if (adopt.isPresent()) {
            pendingCopiedOut = true;
            try (Handle h = pool.jdbi().open()) {
                h.execute("VACUUM " + CommanderSplit.SCHEMA + " INTO '" + target.replace("'", "''") + "'");
            }
            log.info("Commander {}'s file started from the data gathered before any commander was known", fid);
        }
        Pool scoped;
        // The pool opens while the bootstrap handle is still open, as in switchCommander: under test the commander's
        // file is a named in-memory database, which is gone the moment its last connection closes.
        try (Handle commanderBootstrap = Jdbi.create("jdbc:sqlite:" + target).open()) {
            commanderBootstrap.execute("PRAGMA journal_mode = WAL;");
            migrate(commanderBootstrap, DatabaseMigrator.Tree.COMMANDER);
            HikariConfig config = poolConfig(target);
            config.setPoolName("EliteIntelSQLiteScope-" + fid);
            config.setMaximumPoolSize(2);
            config.setMinimumIdle(1);
            HikariDataSource dataSource = new HikariDataSource(config);
            scoped = new Pool(dataSource, Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin()), fid);
            scoped.jdbi().useHandle(Database::applyPragmas);
        }
        return scoped;
    }

    /**
     * The pending file, when it is the one open and the new commander has no file of their own yet: then it
     * becomes theirs. A commander who already has a file keeps it, and the pending file is left as it is.
     * Only real files are adopted; a test's in-memory databases never are.
     */
    private static Optional<Path> pendingFileToAdopt(Pool open, String target) {
        if (!PENDING_COMMANDER.equals(open.commander()) || System.getProperty(COMMANDER_URL_PROPERTY) != null) {
            return Optional.empty();
        }
        if (Files.exists(Path.of(target))) return Optional.empty();
        return Optional.of(Path.of(commanderLocation(PENDING_COMMANDER)));
    }

    private static void deleteDatabaseFile(Path file) {
        for (String suffix : new String[]{"", "-wal", "-shm"}) {
            Path part = file.resolveSibling(file.getFileName() + suffix);
            try {
                Files.deleteIfExists(part);
            } catch (IOException e) {
                log.warn("Could not delete {}: {}", part, e.getMessage());
            }
        }
    }

    static {
        SHARED_URL = sharedUrl();

        // 1. The shared file first, on its own. It names the journal folder, and the journals name the commander.
        //    The bootstrap handle stays open until the pool exists, so a named in-memory database under test
        //    outlives the hand-over.
        Handle sharedBootstrap = Jdbi.create(SHARED_URL).open();
        try {
            applyPragmas(sharedBootstrap);
            migrate(sharedBootstrap, DatabaseMigrator.Tree.SHARED);

            // 2. The commander's own file, built from the commander tree while it is still main. An unqualified
            //    CREATE TABLE lands in main, so this cannot be done through the attachment.
            String commander = commanderFromJournals(sharedBootstrap);
            String commanderLocation = commanderLocation(commander);
            try (Handle commanderBootstrap = Jdbi.create("jdbc:sqlite:" + commanderLocation).open()) {
                commanderBootstrap.execute("PRAGMA journal_mode = WAL;");
                migrate(commanderBootstrap, DatabaseMigrator.Tree.COMMANDER);

                // 3. The pool. Every connection attaches the commander's file, so the DAOs keep their unqualified
                //    table names: SQLite finds each one in whichever file holds it.
                pool = openPool(commanderLocation, commander);
            }
        } finally {
            // Held until here for the in-memory case only: the pool now keeps the shared file open.
            sharedBootstrap.close();
        }
        log.info("Database open for commander {}", pool.commander());

        pool.jdbi().withHandle(h -> {
            applyPragmas(h);

            // 4. Once per database: move the commander's tables out of the shared file.
            CommanderSplit.runIfNeeded(h);
            CommanderSplit.splitMixedIfNeeded(h);

            // Automatically attach all DAO classes from elite.intel.db.dao package
            try {
                Set<Class<?>> daoClasses = findDaoClasses("elite.intel.db.dao");
                for (Class<?> daoClass : daoClasses) {
                    h.attach(daoClass);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to attach DAO classes", e);
            }

            return null;
        });
    }

    private static String sharedUrl() {
        String overrideUrl = System.getProperty("elite.intel.db.url");
        if (overrideUrl != null) {
            return overrideUrl;
        }
        Path dbPath;
        try {
            dbPath = AppPaths.getDatabasePath();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create database directory " + e.getMessage(), e);
        }
        return "jdbc:sqlite:" + dbPath + CONNECTION_OPTIONS;
    }

    private static Pool openPool(String commanderLocation, String commander) {
        HikariDataSource dataSource = new HikariDataSource(poolConfig(commanderLocation));
        return new Pool(dataSource, Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin()), commander);
    }

    private static HikariConfig poolConfig(String commanderLocation) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(SHARED_URL);
        int maxPool = Integer.getInteger("elite.intel.db.pool.size", 10);
        config.setMaximumPoolSize(maxPool);
        config.setMinimumIdle(Math.min(2, maxPool));
        config.setConnectionTimeout(30000);         // 30 seconds
        config.setIdleTimeout(600000);              // 10 minutes
        config.setMaxLifetime(1800000);             // 30 minutes
        config.setPoolName("EliteIntelSQLitePool");
        config.setConnectionInitSql("ATTACH DATABASE '" + commanderLocation.replace("'", "''") + "' AS "
                + CommanderSplit.SCHEMA);

        // SQLite-specific connection test query
        config.setConnectionTestQuery("SELECT 1");
        return config;
    }

    private static void applyPragmas(Handle h) {
        h.execute("PRAGMA foreign_keys = ON;");           // always good
        h.execute("PRAGMA case_sensitive_like = OFF;");   // makes LIKE ignore case
        h.execute("PRAGMA journal_mode = WAL;");          // safe + fast
        h.execute("PRAGMA synchronous = NORMAL;");        // fast + still safe on Linux
        h.execute("PRAGMA busy_timeout = 5000;");         // avoid lock errors
    }

    private static void migrate(Handle handle, DatabaseMigrator.Tree tree) {
        try {
            DatabaseMigrator.migrate(handle, tree);
        } catch (Exception e) {
            throw new RuntimeException("Migration failed - your DB might be b0rked", e);
        }
    }

    /**
     * The commander in the newest journal that names one, or {@value #PENDING_COMMANDER}.
     * <p>
     * Read from the stored journal folder (falling back to the platform's usual one) straight off the shared
     * file, since the session that normally answers this needs the database to exist first.
     */
    private static String commanderFromJournals(Handle shared) {
        String stored = shared.createQuery("SELECT journal_dir FROM user_preferences WHERE id = 1")
                .mapTo(String.class)
                .findOne()
                .orElse(null);
        String trimmed = stored == null || stored.isBlank() ? null : stored.trim();
        Path journalDir = DirectorySetting.resolve(trimmed, DirectorySetting.defaultJournalPath());
        return JournalCommander.newestFid(journalDir)
                .filter(Database::isUsableFid)
                .orElse(PENDING_COMMANDER);
    }

    /**
     * An FID becomes part of a file name, so anything but the letters and digits Frontier uses is refused rather
     * than trusted.
     */
    public static boolean isUsableFid(String fid) {
        return fid != null && fid.matches("[A-Za-z0-9_-]{1,64}");
    }

    /**
     * The SQLite location of one commander's file: a path, or under test a named in-memory database.
     */
    static String commanderLocation(String fid) {
        String template = System.getProperty(COMMANDER_URL_PROPERTY);
        if (template != null) {
            return template.replace(FID_PLACEHOLDER, fid);
        }
        try {
            return AppPaths.getDatabasePath().resolveSibling("cmdr_" + fid + ".db").toString();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create database directory " + e.getMessage(), e);
        }
    }

    private static Set<Class<?>> findDaoClasses(String packageName) throws Exception {
        Set<Class<?>> classes = new HashSet<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Enumeration<URL> resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                Path root = Paths.get(resource.toURI());
                try (var walk = Files.walk(root)) {
                    walk.filter(p -> p.toString().endsWith(".class"))
                            .forEach(p -> {
                                try {
                                    String className = packageName + "." +
                                            root.relativize(p).toString()
                                                    .replace(FileSystems.getDefault().getSeparator(), ".")
                                                    .replace(".class", "");
                                    Class<?> clazz = Class.forName(className);
                                    if (clazz.isInterface() && clazz.getSimpleName().endsWith("Dao")) {
                                        classes.add(clazz);
                                    }
                                } catch (ClassNotFoundException e) {
                                    // Skip classes that can't be loaded
                                }
                            });
                }
            } else if ("jar".equals(protocol)) {
                String urlStr = resource.toString();
                int sep = urlStr.indexOf("!/");
                String jarPart = urlStr.substring(0, sep);
                URI jarUri = URI.create(jarPart);

                try (FileSystem fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
                    Path root = fs.getPath("/" + path);
                    try (var walk = Files.walk(root)) {
                        walk.filter(p -> p.toString().endsWith(".class"))
                                .forEach(p -> {
                                    try {
                                        String className = packageName + "." +
                                                root.relativize(p).toString()
                                                        .replace("/", ".")
                                                        .replace(".class", "");
                                        Class<?> clazz = Class.forName(className);
                                        if (clazz.isInterface() && clazz.getSimpleName().endsWith("Dao")) {
                                            classes.add(clazz);
                                        }
                                    } catch (ClassNotFoundException e) {
                                        // Skip classes that can't be loaded
                                    }
                                });
                    }
                }
            }
        }
        return classes;
    }
}
