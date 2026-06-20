package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.util.NavigationUtils;
import elite.intel.util.StringUtls;

@RegisterQuery
public class AnalyzeDistanceFromTheBubbleQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_distance_to_bubble_earth_sol_civilization";


    @Override public String id() { return ID; }


    private final LocationManager locationManager = LocationManager.getInstance();
    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {

        LocationDao.Coordinates galacticCoordinates = locationManager.getGalacticCoordinates();

        if (galacticCoordinates.x() == 0 && galacticCoordinates.y() == 0 && galacticCoordinates.z() == 0) {
            return process(StringUtls.localizedLlm("query.noLocalCoords"));
        }

        double distance = NavigationUtils.calculateGalacticDistance(
                0.0, 0.0, 0.0,
                galacticCoordinates.x(),
                galacticCoordinates.y(),
                galacticCoordinates.z()
        );
        int distLy = (int) Math.round(distance);
        double jumps = distLy / 500.0;
        int fuelTons = (int) Math.round(jumps * 100);
        double totalMinutes = jumps * 20;
        int hours = (int) (totalMinutes / 60);
        int minutes = (int) (totalMinutes % 60);

        return process(StringUtls.localizedLlm("query.bubble.distance", distLy, fuelTons, hours, minutes));
    }
}
