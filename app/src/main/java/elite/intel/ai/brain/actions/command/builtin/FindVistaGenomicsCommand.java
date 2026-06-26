package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.station.vista.VistaGenomicsLocationDto;
import elite.intel.search.spansh.station.vista.VistaGenomicsSearch;
import elite.intel.search.spansh.station.vista.VistaSearchCriteria;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.List;
import java.util.Optional;

/**
 * Self-describing "find vista genomics" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindVistaGenomicsHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindVistaGenomicsCommand implements IntelCommand {
    public static final String ID = "find_vista_genomics";

    @Override public String llmDescription() { return "Find the nearest Vista Genomics to sell exobiology data."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        Number range = GetNumberFromParam.extractRangeParameter(params, 250);

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
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.vistaGenomics.notFound"));
        }

        Optional<VistaGenomicsLocationDto.Result> first = results.stream().findFirst();
        RoutePlotter routePlotter = new RoutePlotter();
        VistaGenomicsLocationDto.Result result = first.get();

        String announcement = StringUtls.localizedLlm("handler.vistaGenomics.headTo", result.getSystemName(), result.getStationName());
        ReminderManager.getInstance().setReminder(result.getSystemName(), announcement);
        routePlotter.plotRoute(result.getSystemName());
        return CommandOutcome.critical(announcement);
    }
}
