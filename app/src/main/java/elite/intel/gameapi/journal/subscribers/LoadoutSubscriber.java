package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.LoadoutEvent;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.ActiveShipChangedEvent;
import elite.intel.ui.event.ShipProfileChangedEvent;
import elite.intel.util.StringUtls;

import java.util.Objects;


public class LoadoutSubscriber {

    private final ShipManager shipManager = ShipManager.getInstance();
    private final SystemSession systemSession = SystemSession.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final Status status = Status.getInstance();

    @Subscribe
    public void onLoadoutEvent(LoadoutEvent event) {
        Thread.ofVirtual().start(() -> {
            if (status.isInMainShip()) {
                playerSession.setShipAutoDeparted(false);
            }
            ShipDao.Ship currentShip = shipManager.getShip();
            Status.getInstance().setOkToAnnounceLoadout(
                    currentShip != null && !Objects.equals(currentShip.getShipId(), event.getShipId())
            );

            String shipName = LoadoutConverter.toDisplayShipName(event);
            playerSession.setCurrentShipName(shipName);
            playerSession.setCurrentShip(event.getShip());
            playerSession.setShipLoadout(LoadoutConverter.toShipLoadOutDto(event));

            String commanderName = playerSession.getInGameName();
            boolean hasCommander = commanderName != null && !commanderName.isBlank();

            ShipDao.Ship ship = shipManager.getShipById(event.getShipId());
            if (ship == null) {
                String shipDefaultVoice = KokoroVoices.BELLA.name();
                if (!systemSession.useLocalTTS()) {
                    shipDefaultVoice = GoogleVoices.STEVE.name();
                }
                shipManager.save(event.getShipId(), shipName, event.getCargoCapacity(), event.getShip(), shipDefaultVoice,
                        hasCommander ? commanderName : null);
                EventBusManager.publish(new ShipProfileChangedEvent());
            } else {
                ship.setCargoCapacity(event.getCargoCapacity());
                ship.setShipIdentifier(event.getShip());
                ship.setShipName(shipName);
                if (hasCommander) {
                    ship.setCommanderName(commanderName);
                }
                shipManager.saveShip(ship);
                EventBusManager.publish(new ShipProfileChangedEvent());
            }

            EventBusManager.publish(new ActiveShipChangedEvent(shipName));

            if (Status.getInstance().isOkToAnnounceLoadout()) {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.shipIntroduction(playerSession.getPlayerName(), shipName)));
            }
        });
    }
}
