package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MaterialManager;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.MaterialsEvent;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import elite.intel.ui.event.AppLogDebugEvent;

import java.util.List;

import static elite.intel.gameapi.search.edsm.dto.MaterialsType.*;

@SuppressWarnings("unused")
public class MaterialsEventSubscriber {

    private final MaterialManager materialManager = MaterialManager.getInstance();

    @Subscribe
    public void onMaterialsEvent(MaterialsEvent event) {
        Thread.ofVirtual().start(() -> {
            saveAll(event.getRaw(), GAME_RAW);
            saveAll(event.getManufactured(), GAME_MANUFACTURED);
            saveAll(event.getEncoded(), GAME_ENCODED);
        });
    }

    private void saveAll(List<MaterialsEvent.Material> materials, MaterialsType type) {
        if (materials == null) return;
        for (MaterialsEvent.Material material : materials) {
            saveMaterial(material, type);
        }
    }

    /**
     * Stores against the journal's {@code Name} — the FDev symbol, e.g. {@code basicconductors}. The
     * event is a full inventory snapshot, so its counts are absolute and replace what we held.
     * {@code Name_Localised} is passed only so an unrecognised material gets a readable name; Frontier
     * omits it whenever it would equal the raw name, which is the norm for raw elements.
     */
    private void saveMaterial(MaterialsEvent.Material material, MaterialsType type) {
        materialManager.snapshot(material.getName(), type, material.getCount(), material.getLocalisedName());
        UiBus.publish(new AppLogDebugEvent("\tProcessed " + material.getName() + " " + material.getCount() + " units."));
    }
}
