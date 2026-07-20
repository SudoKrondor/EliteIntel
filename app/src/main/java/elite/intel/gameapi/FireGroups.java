package elite.intel.gameapi;

import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.util.SleepNoThrow;

import java.util.HashMap;
import java.util.Map;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_CYCLE_NEXT_FIRE_GROUP;

public class FireGroups {
    public static final Map<String, Integer> fireGroups = new HashMap<>();

    static {
        fireGroups.put("A", 0);
        fireGroups.put("B", 1);
        fireGroups.put("C", 2);
        fireGroups.put("D", 3);
        fireGroups.put("E", 4);
        fireGroups.put("F", 5);
        fireGroups.put("G", 6);
        fireGroups.put("H", 7);
    }

    static final Map<String, String> natoAlphabet = new HashMap<>();

    static {
        natoAlphabet.put("alpha", "A");
        natoAlphabet.put("bravo", "B");
        natoAlphabet.put("charlie", "C");
        natoAlphabet.put("delta", "D");
        natoAlphabet.put("echo", "E");
        natoAlphabet.put("foxtrot", "F");
        natoAlphabet.put("golf", "G");
        natoAlphabet.put("hotel", "H");
    }

    public static int fireGroupInSettings(ShipSettingsDao.ShipSettings settings) {
        String fireGroup = settings.getHonkFireGroup();
        if (fireGroup == null) return 0;
        Integer result = fireGroups.get(fireGroup);
        return result == null ? 0 : result;
    }

    /**
     * The group letter an LLM-supplied argument names, or null when it names none.
     *
     * <p>The command's extraction hint asks for the lower-case NATO word, but the model is free to answer
     * "Bravo", "B" or "2" instead, so all three forms resolve here. Matching only the exact lower-case word
     * (as this did) sent every other form down the unknown branch.
     */
    private static String getNato(String key) {
        if (key == null) return null;
        String normalized = key.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) return null;

        String byWord = natoAlphabet.get(normalized);
        if (byWord != null) return byWord;

        // A bare letter ("b") or the group's 1-based position ("2"), both of which the model does emit.
        if (normalized.length() == 1) {
            char c = normalized.charAt(0);
            if (c >= 'a' && c <= 'h') return String.valueOf(Character.toUpperCase(c));
            if (c >= '1' && c <= '8') return String.valueOf((char) ('A' + (c - '1')));
        }
        return null;
    }

    /**
     * The zero-based fire group the argument names, or -1 when it names none.
     *
     * <p>Returns -1 rather than 0 for an unrecognized argument: a misheard or unmapped word used to resolve
     * to group A, so "group bravo" silently switched to the wrong group instead of doing nothing.
     */
    public static int fireGroupByNato(String nato) {
        String letter = getNato(nato);
        if (letter == null) return -1;
        Integer result = fireGroups.get(letter);
        return result == null ? -1 : result;
    }

    public static void cycleToGroup(int targetGroup) {
        Status status = Status.getInstance();
        for (int attempt = 0; attempt < 16; attempt++) {
            if (targetGroup == status.getFireGroup()) break;
            int groupBefore = status.getFireGroup();
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(BINDING_CYCLE_NEXT_FIRE_GROUP.getGameBinding()),
                    GameInputStep.delay(1000)
            ));
            long deadline = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < deadline) {
                SleepNoThrow.sleep(50);
                if (status.getFireGroup() != groupBefore) break;
            }
        }
    }
}
