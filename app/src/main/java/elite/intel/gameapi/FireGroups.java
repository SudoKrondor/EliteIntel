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

    private static String getNato(String key) {
        if (key == null) return null;
        return natoAlphabet.get(key.trim());
    }

    public static int fireGroupByNato(String nato) {
        if (nato == null) return -1;
        nato = getNato(nato);
        if (nato == null) return 0;
        Integer result = fireGroups.get(nato);
        return result == null ? 0 : result;
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
