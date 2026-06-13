package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.search.spansh.station.vista.VistaGenomicsLocationDto;
import elite.intel.search.spansh.station.vista.VistaGenomicsSearch;
import elite.intel.search.spansh.station.vista.VistaSearchCriteria;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.List;
import java.util.Optional;

public class FindVistaGenomicsHandler implements CommandHandler {



    @Override public void handle(String action, JsonObject params, String responseText) {
        Number range = GetNumberFromParam.extractRangeParameter(params, 250);
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.vistaGenomics.searching")));


        VistaSearchCriteria criteria = new VistaSearchCriteria();
        VistaSearchCriteria.Filters filters = new VistaSearchCriteria.Filters();
        VistaSearchCriteria.Service service = new VistaSearchCriteria.Service();
        service.setName(List.of("Vista Genomics"));
        filters.setServices(List.of(service));

        VistaSearchCriteria.Distance distance = new VistaSearchCriteria.Distance();
        distance.setMin(0);
        distance.setMax(range.intValue());
        filters.setDistance(distance);
        criteria.setFilters(filters);

        LocationDao.Coordinates galacticCoordinates = LocationManager.getInstance().getGalacticCoordinates();

        VistaSearchCriteria.ReferenceCoords coords = new VistaSearchCriteria.ReferenceCoords();
        coords.setX(galacticCoordinates.x());
        coords.setY(galacticCoordinates.y());
        coords.setZ(galacticCoordinates.z());
        criteria.setReferenceCoords(coords);

        List<VistaGenomicsLocationDto.Result> results = VistaGenomicsSearch.findVistaGenomics(criteria);
        if (results == null || results.isEmpty()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.vistaGenomics.notFound")));
            return;
        }

        Optional<VistaGenomicsLocationDto.Result> first = results.stream().findFirst();
        RoutePlotter routePlotter = new RoutePlotter();
        VistaGenomicsLocationDto.Result result = first.get();

        String announcement = StringUtls.localizedLlm("handler.vistaGenomics.headTo", result.getSystemName(), result.getStationName());
        ReminderManager.getInstance().setReminder(result.getSystemName(), announcement);
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(announcement));
        routePlotter.plotRoute(result.getSystemName());
    }
}
