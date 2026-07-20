package elite.intel.gameapi.search.edsm.utils;

import elite.intel.gameapi.search.spansh.starsystems.StarSystemResult;

public class EnemyStarSystemFilter {

    public static boolean isEnemyStrongholdPair(StarSystemResult a, StarSystemResult b, String pledgedPower) {
        if (a == null || b == null) return false;
        StarSystemResult.SystemRecord aRecord = a.getRecord();
        StarSystemResult.SystemRecord bRecord = b.getRecord();
        if (aRecord == null || bRecord == null) return false;

        boolean aIsEnemy = a.getRecord().getControllingPower() != null
                && !a.getRecord().getControllingPower().equalsIgnoreCase(pledgedPower)
                && "Stronghold".equalsIgnoreCase(a.getRecord().getPowerState());

        boolean bIsEnemy = b.getRecord().getControllingPower() != null
                && !b.getRecord().getControllingPower().equalsIgnoreCase(pledgedPower)
                && "Stronghold".equalsIgnoreCase(b.getRecord().getPowerState());

        return aIsEnemy || bIsEnemy;
    }

}
