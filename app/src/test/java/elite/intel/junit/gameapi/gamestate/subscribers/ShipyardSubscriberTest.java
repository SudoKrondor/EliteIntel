package elite.intel.junit.gameapi.gamestate.subscribers;

import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.gamestate.subscribers.ShipyardSubscriber;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipyardSubscriberTest {

    private final ShipyardSubscriber subscriber = new ShipyardSubscriber();

    @Test
    void skipsEntryWhenShipTypeLocalisedIsNull() throws InterruptedException {
        GameEvents.ShipyardEvent.ShipPrice price = new GameEvents.ShipyardEvent.ShipPrice();
        price.setShipType("nulllocalisedship");
        price.setShipTypeLocalised(null);

        GameEvents.ShipyardEvent event = new GameEvents.ShipyardEvent();
        event.setPriceList(List.of(price));

        subscriber.onShipyardEvent(event);
        Thread.sleep(100);

        // Title-case fallback confirms no upsert occurred
        assertEquals("Nulllocalisedship", LoadoutConverter.toDisplayShipName(null, "nulllocalisedship"));
    }

    @Test
    void upsertsDisplayNameForLocalisedEntry() throws InterruptedException {
        GameEvents.ShipyardEvent.ShipPrice price = new GameEvents.ShipyardEvent.ShipPrice();
        price.setShipType("shipyardtest_mk1");
        price.setShipTypeLocalised("Shipyard Test Mk 1");

        GameEvents.ShipyardEvent event = new GameEvents.ShipyardEvent();
        event.setPriceList(List.of(price));

        subscriber.onShipyardEvent(event);
        Thread.sleep(100);

        assertEquals("Shipyard Test Mk 1", LoadoutConverter.toDisplayShipName(null, "shipyardtest_mk1"));
    }

    @Test
    void mixedPriceListOnlyUpsertsLocalisedEntries() throws InterruptedException {
        GameEvents.ShipyardEvent.ShipPrice withLocalised = new GameEvents.ShipyardEvent.ShipPrice();
        withLocalised.setShipType("mixedtest_ship");
        withLocalised.setShipTypeLocalised("Mixed Test Ship");

        GameEvents.ShipyardEvent.ShipPrice withoutLocalised = new GameEvents.ShipyardEvent.ShipPrice();
        withoutLocalised.setShipType("rawtest_control_ship");
        withoutLocalised.setShipTypeLocalised(null);

        GameEvents.ShipyardEvent event = new GameEvents.ShipyardEvent();
        event.setPriceList(List.of(withLocalised, withoutLocalised));

        subscriber.onShipyardEvent(event);
        Thread.sleep(100);

        assertEquals("Mixed Test Ship", LoadoutConverter.toDisplayShipName(null, "mixedtest_ship"));
        // Synthetic key not in seed table — title-case fallback confirms no upsert occurred
        assertEquals("Rawtest_control_ship", LoadoutConverter.toDisplayShipName(null, "rawtest_control_ship"));
    }
}
