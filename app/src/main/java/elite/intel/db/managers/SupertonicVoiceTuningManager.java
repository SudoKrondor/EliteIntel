package elite.intel.db.managers;

import elite.intel.ai.mouth.VoiceTuning;
import elite.intel.ai.mouth.supertonic.SupertonicVoices;
import elite.intel.db.dao.SupertonicVoiceTuningDao;
import elite.intel.db.util.Database;

import java.util.HashMap;
import java.util.Map;

/**
 * Each Supertonic voice's own tuning (see {@link VoiceTuning}), read and written through
 * {@link SupertonicVoiceTuningDao}.
 * <p>
 * WHY a cache: every sentence the engine speaks asks for its voice's tuning, on the synthesis thread. The table is
 * small (one row per tuned voice) and only this manager writes it, so it is read once and kept in memory. A write
 * goes to the database first and then swaps in a new map, so a reader never sees a half-made one.
 */
public class SupertonicVoiceTuningManager {

    private static volatile SupertonicVoiceTuningManager instance;

    /**
     * Voice name to tuning; a voice that is not here plays at {@link VoiceTuning#DEFAULT}. {@code null} until first
     * read.
     */
    private volatile Map<String, VoiceTuning> cache;

    private SupertonicVoiceTuningManager() {
    }

    public static SupertonicVoiceTuningManager getInstance() {
        if (instance == null) {
            synchronized (SupertonicVoiceTuningManager.class) {
                if (instance == null) {
                    instance = new SupertonicVoiceTuningManager();
                }
            }
        }
        return instance;
    }

    /**
     * Reads the table into memory if it is not there yet, so a later {@link #tuningOf} does not.
     */
    public void load() {
        tunings();
    }

    public VoiceTuning tuningOf(SupertonicVoices voice) {
        return tunings().getOrDefault(voice.name(), VoiceTuning.DEFAULT);
    }

    public synchronized void save(SupertonicVoices voice, VoiceTuning tuning) {
        Database.withDao(SupertonicVoiceTuningDao.class, dao -> {
            dao.save(toRow(voice, tuning));
            return null;
        });
        Map<String, VoiceTuning> next = new HashMap<>(tunings());
        next.put(voice.name(), tuning);
        cache = Map.copyOf(next);
    }

    /**
     * Puts the voice back to {@link VoiceTuning#DEFAULT}.
     */
    public synchronized void reset(SupertonicVoices voice) {
        Database.withDao(SupertonicVoiceTuningDao.class, dao -> {
            dao.delete(voice.name());
            return null;
        });
        Map<String, VoiceTuning> next = new HashMap<>(tunings());
        next.remove(voice.name());
        cache = Map.copyOf(next);
    }

    private Map<String, VoiceTuning> tunings() {
        Map<String, VoiceTuning> loaded = cache;
        if (loaded != null) return loaded;
        synchronized (this) {
            if (cache == null) {
                Map<String, VoiceTuning> rows = new HashMap<>();
                Database.withDao(SupertonicVoiceTuningDao.class, SupertonicVoiceTuningDao::all)
                        .forEach(row -> rows.put(row.getVoice(), fromRow(row)));
                cache = Map.copyOf(rows);
            }
            return cache;
        }
    }

    private static VoiceTuning fromRow(SupertonicVoiceTuningDao.Tuning row) {
        return new VoiceTuning(row.getSpeed(), row.getVolume(), new int[]{
                row.getEq1(), row.getEq2(), row.getEq3(), row.getEq4(), row.getEq5(), row.getEq6()});
    }

    private static SupertonicVoiceTuningDao.Tuning toRow(SupertonicVoices voice, VoiceTuning tuning) {
        int[] eq = tuning.eq();
        SupertonicVoiceTuningDao.Tuning row = new SupertonicVoiceTuningDao.Tuning();
        row.setVoice(voice.name());
        row.setSpeed(tuning.speed());
        row.setVolume(tuning.volume());
        row.setEq1(eq[0]);
        row.setEq2(eq[1]);
        row.setEq3(eq[2]);
        row.setEq4(eq[3]);
        row.setEq5(eq[4]);
        row.setEq6(eq[5]);
        return row;
    }
}
