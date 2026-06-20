package elite.intel.db.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import elite.intel.util.AppPaths;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class Database {

    private static final HikariDataSource DATA_SOURCE;
    private static final Jdbi JDBI;

    private static void migrateIfNeeded() {
        JDBI.useHandle(handle -> {
            try {
                DatabaseMigrator.migrate(handle);
            } catch (Exception e) {
                throw new RuntimeException("Migration failed - your DB might be b0rked", e);
            }
        });
    }

    public static <T, R> R withDao(Class<T> daoClass, java.util.function.Function<T, R> block) {
        // Use withExtension to get a thread-safe handle for this specific operation
        try {
            return JDBI.withExtension(daoClass, block::apply);
        } catch (Exception e) {
            throw new RuntimeException("DAO operation failed: " + daoClass.getSimpleName(), e);
        }
    }

    // shortcut if you just need a handle
    public static Handle init() {
        return JDBI.open();
    }

    // Shutdown the connection pool gracefully
    public static void shutdown() {
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }

    static {
        String overrideUrl = System.getProperty("elite.intel.db.url");
        String url;
        if (overrideUrl != null) {
            url = overrideUrl;
        } else {
            Path dbPath;
            try {
                dbPath = AppPaths.getDatabasePath();
            } catch (IOException e) {
                throw new RuntimeException("Unable to create database directory " + e.getMessage(), e);
            }
            url = "jdbc:sqlite:" + dbPath
                    + "?journal_mode=WAL"
                    + "&busy_timeout=5000"
                    + "&synchronous=NORMAL"
                    + "&foreign_keys=ON";
        }

        // Configure HikariCP connection pool
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setMaximumPoolSize(10);              // SQLite can handle ~10 concurrent connections with WAL
        config.setMinimumIdle(2);                   // Keep 2 connections ready
        config.setConnectionTimeout(30000);         // 30 seconds
        config.setIdleTimeout(600000);              // 10 minutes
        config.setMaxLifetime(1800000);             // 30 minutes
        config.setPoolName("EliteIntelSQLitePool");

        // SQLite-specific connection test query
        config.setConnectionTestQuery("SELECT 1");

        DATA_SOURCE = new HikariDataSource(config);
        JDBI = Jdbi.create(DATA_SOURCE).installPlugin(new SqlObjectPlugin());

        // Open once so the file gets created if missing and persistent pragmas are set
        JDBI.withHandle(h -> {
            h.execute("PRAGMA foreign_keys = ON;");           // always good
            h.execute("PRAGMA case_sensitive_like = OFF;");   // makes LIKE ignore case
            h.execute("PRAGMA journal_mode = WAL;");          // safe + fast
            h.execute("PRAGMA synchronous = NORMAL;");        // fast + still safe on Linux
            h.execute("PRAGMA busy_timeout = 5000;");         // avoid lock errors

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


        // Run migrations (see below)
        migrateIfNeeded();
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
