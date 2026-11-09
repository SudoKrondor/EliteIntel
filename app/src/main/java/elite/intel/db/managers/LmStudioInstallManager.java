package elite.intel.db.managers;

import elite.intel.db.dao.LmStudioInstallDao;
import elite.intel.db.dao.LmStudioInstallDao.LmStudioInstallRow;
import elite.intel.db.util.Database;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The record of the LM Studio the app installed on this PC, read and written through {@link LmStudioInstallDao}.
 * <p>
 * A table of its own rather than columns on {@code game_session}, on the pattern of {@link BindForgeSettingsManager}.
 * <p>
 * This holds what was recorded and nothing more. Whether the record still describes this PC is decided by the
 * marker file in its home folder (see {@code elite.intel.ai.brain.inference.lmstudio.install.InstallMarker}).
 */
public class LmStudioInstallManager {

    private static volatile LmStudioInstallManager instance;

    private LmStudioInstallManager() {
    }

    public static LmStudioInstallManager getInstance() {
        if (instance == null) {
            synchronized (LmStudioInstallManager.class) {
                if (instance == null) {
                    instance = new LmStudioInstallManager();
                }
            }
        }
        return instance;
    }

    /**
     * An install the app made.
     *
     * @param installId      also written into the marker file in {@code homeDir}
     * @param homeDir        the LM Studio home it installed into
     * @param pointerWritten whether it wrote {@code ~/.lmstudio-home-pointer}, which uninstalling then removes
     * @param setupComplete  whether every step of the install finished
     */
    public record Install(UUID installId, Path homeDir, boolean pointerWritten, boolean setupComplete,
                          Instant installedAt) {
        public Install {
            Objects.requireNonNull(installId, "installId");
            Objects.requireNonNull(homeDir, "homeDir");
            Objects.requireNonNull(installedAt, "installedAt");
        }
    }

    /**
     * The install the app made, if it made one.
     */
    public Optional<Install> find() {
        LmStudioInstallRow row = Database.withDao(LmStudioInstallDao.class, LmStudioInstallDao::find);
        if (row == null) return Optional.empty();
        return Optional.of(new Install(UUID.fromString(row.installId()), Path.of(row.homeDir()),
                row.pointerWritten(), row.setupComplete(), Instant.parse(row.installedAt())));
    }

    /**
     * Records an install as it starts, unfinished.
     *
     * @throws IllegalStateException when a record already exists: there is one at most, so a stale one is
     *                               {@linkplain #forget() forgotten} first
     */
    public synchronized Install recordStarted(Path homeDir, boolean pointerWritten) {
        // WHY checked here as well as by the primary key: the key's failure reaches a caller only as Database's
        // generic "DAO operation failed", which says nothing about what to do instead.
        if (find().isPresent()) {
            throw new IllegalStateException("An LM Studio install is already recorded; forget it before recording another");
        }
        Install install = new Install(UUID.randomUUID(), homeDir, pointerWritten, false, Instant.now());
        Database.withDao(LmStudioInstallDao.class, dao -> {
            dao.insert(install.installId().toString(), homeDir.toString(), pointerWritten,
                    install.installedAt().toString());
            return Void.TYPE;
        });
        return install;
    }

    /**
     * Marks the recorded install finished.
     *
     * @throws IllegalStateException when there is no record to mark
     */
    public void markSetupComplete() {
        int changed = Database.withDao(LmStudioInstallDao.class, LmStudioInstallDao::markSetupComplete);
        if (changed == 0) {
            throw new IllegalStateException("No LM Studio install is recorded, so none can be marked complete");
        }
    }

    /**
     * Deletes the record, after an uninstall or when it no longer describes this PC.
     */
    public void forget() {
        Database.withDao(LmStudioInstallDao.class, dao -> {
            dao.delete();
            return Void.TYPE;
        });
    }
}
