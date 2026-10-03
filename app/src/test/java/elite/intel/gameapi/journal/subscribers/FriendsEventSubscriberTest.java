package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.journal.subscribers.FriendsEventSubscriber.Changes;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendsEventSubscriberTest {

    private static Map<String, String> batch(String... nameStatus) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < nameStatus.length; i += 2) {
            m.put(nameStatus[i], nameStatus[i + 1]);
        }
        return m;
    }

    @Test
    void theFirstOnlineListIsReportedInArrivalOrder() {
        Changes changes = FriendsEventSubscriber.applyBatch(
                batch("Perilous Range", "Online", "Skinky B", "Online"), new HashMap<>());

        assertEquals(List.of("Perilous Range", "Skinky B"), changes.cameOnline());
        assertTrue(changes.wentOffline().isEmpty());
    }

    /**
     * Support bundle of 2026-09-30: the game re-sent the same four online friends two minutes later.
     */
    @Test
    void aResentListWithNothingChangedIsSilent() {
        Map<String, String> known = new HashMap<>();
        Map<String, String> list = batch("Perilous Range", "Online", "Skinky B", "Online",
                "Bravo17", "Online", "RUDOLPH KINGSFIELD", "Online");
        FriendsEventSubscriber.applyBatch(list, known);

        Changes repeat = FriendsEventSubscriber.applyBatch(list, known);

        assertTrue(repeat.cameOnline().isEmpty());
        assertTrue(repeat.wentOffline().isEmpty());
    }

    @Test
    void onlyTheFriendWhoseStatusChangedIsReported() {
        Map<String, String> known = new HashMap<>();
        FriendsEventSubscriber.applyBatch(batch("Bravo17", "Online", "Skinky B", "Online"), known);

        Changes changes = FriendsEventSubscriber.applyBatch(
                batch("Bravo17", "Online", "Skinky B", "Offline"), known);

        assertTrue(changes.cameOnline().isEmpty());
        assertEquals(List.of("Skinky B"), changes.wentOffline());
    }

    @Test
    void goingOfflineIsSilentForAFriendNeverSeenOnline() {
        Changes changes = FriendsEventSubscriber.applyBatch(batch("Bravo17", "Offline"), new HashMap<>());

        assertTrue(changes.wentOffline().isEmpty());
    }

    @Test
    void aFriendComingBackOnlineIsReportedAgain() {
        Map<String, String> known = new HashMap<>();
        FriendsEventSubscriber.applyBatch(batch("Bravo17", "Online"), known);
        FriendsEventSubscriber.applyBatch(batch("Bravo17", "Offline"), known);

        Changes changes = FriendsEventSubscriber.applyBatch(batch("Bravo17", "Online"), known);

        assertEquals(List.of("Bravo17"), changes.cameOnline());
    }

    @Test
    void friendRequestStatusesAreNotSpoken() {
        Changes changes = FriendsEventSubscriber.applyBatch(
                batch("Bravo17", "Requested", "Skinky B", "Added"), new HashMap<>());

        assertTrue(changes.cameOnline().isEmpty());
        assertTrue(changes.wentOffline().isEmpty());
    }
}
