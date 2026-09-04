package elite.intel.gameapi.carrier;

import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;

import java.util.*;
import java.util.function.Consumer;

/**
 * Finding which of the commander's carriers an event is about, and writing back to that one.
 * <p>
 * A commander can own one fleet carrier and one squadron carrier, no more - but the two live in different
 * tables, and most carrier events identify their subject by only one of two handles: an id (a fuel deposit,
 * a trade order) or a station name (a market, the pad under the ship, which for a carrier IS its callsign).
 * Every caller that has to act on "our carrier" needs the same lookup, and a squadron carrier written back
 * to the fleet carrier's row is a silent swap of one ship for the other.
 */
public final class OurCarriers {

    private OurCarriers() {
    }

    /**
     * Which of the two a carrier is. The tables say it and the journal does not: a squadron carrier's
     * {@code StationType} is not in Frontier's published schema, so nothing in an event can be trusted to
     * tell the two apart.
     */
    public enum Kind {FLEET, SQUADRON}

    /**
     * One of the commander's carriers, with the way back to its own table.
     */
    public record Ours(Kind kind, CarrierDataDto data, Consumer<CarrierDataDto> save) {
        /**
         * Applies a change and files it. Read-change-write in one call, so no caller can do the first two
         * and forget the third.
         */
        public void update(Consumer<CarrierDataDto> change) {
            change.accept(data);
            save.accept(data);
        }
    }

    /**
     * The carrier bearing this station name, if either does.
     * <p>
     * By callsign, because a carrier's station name IS its callsign and the squadron carrier's
     * {@code StationType} is not in Frontier's published schema - see {@link OwnCarrierHold}.
     */
    public static Optional<Ours> byCallSign(String stationName) {
        if (stationName == null || stationName.isBlank()) return Optional.empty();
        String name = stationName.trim();
        return match(carrier -> name.equalsIgnoreCase(carrier.getCallSign()));
    }

    /**
     * The carrier transmitting on the comms channel, if either of ours is.
     * <p>
     * WHY not {@link #byCallSign}: {@code ReceiveText} signs a carrier's traffic control with its NAME and
     * callsign together - "LONE WOLF GHY-L8X" in 2002 transmissions across two months of journals - while
     * {@code DockingGranted} names the same carrier "GHY-L8X". The callsign is the half that identifies it,
     * and it is always the last word, so the suffix is what both forms have in common. Another commander's
     * carrier signs the same way, which is exactly why the match is against ours and not against the shape.
     */
    public static Optional<Ours> byRadioSender(String sender) {
        if (sender == null || sender.isBlank()) return Optional.empty();
        String from = sender.trim();
        return match(carrier -> {
            String callSign = carrier.getCallSign();
            if (callSign == null || callSign.isBlank()) return false;
            return from.equalsIgnoreCase(callSign) || from.toLowerCase().endsWith(" " + callSign.toLowerCase());
        });
    }

    /**
     * The voice assigned to whichever of our carriers is transmitting, or null - a station we have not
     * given a voice to, or anyone else on the channel, is a stranger drawn at random.
     */
    public static String radioVoiceOf(String sender) {
        return byRadioSender(sender).map(ours -> ours.data().getVoice()).orElse(null);
    }

    /**
     * Every voice a carrier's traffic control has been given.
     * <p>
     * Nobody else on the channel may draw one of these. The point of assigning a carrier a voice is that the
     * commander knows it when they hear it, and a passing station answering in their own carrier's voice
     * takes exactly that away.
     */
    public static Set<String> assignedVoices() {
        Set<String> voices = new LinkedHashSet<>();
        for (Ours ours : known()) {
            String voice = ours.data().getVoice();
            if (voice != null && !voice.isBlank()) voices.add(voice);
        }
        return voices;
    }

    /**
     * The carrier with this id, if either has it. A carrier's id is also its MarketID.
     * <p>
     * Zero never matches: it is what an id reads as before the commander has opened that carrier's
     * management panel, so treating it as a value would make every unidentified carrier the same carrier.
     */
    public static Optional<Ours> byId(long carrierId) {
        if (carrierId == 0) return Optional.empty();
        return match(carrier -> carrier.getCarrierId() == carrierId);
    }

    /**
     * The carriers the commander actually has, fleet first. For the screens that list what they own rather
     * than answering a question about one carrier.
     * <p>
     * WHY a callsign is required: both managers answer with a blank {@link CarrierDataDto} rather than null
     * for a commander who has no such carrier, so a row is not proof of a carrier. The callsign is what
     * {@code CarrierStats} fills in, and it is the handle the rest of this class matches on.
     */
    public static List<Ours> known() {
        List<Ours> carriers = new ArrayList<>(2);
        for (Ours ours : both()) {
            String callSign = ours.data().getCallSign();
            if (callSign != null && !callSign.isBlank()) carriers.add(ours);
        }
        return carriers;
    }

    private static List<Ours> both() {
        List<Ours> carriers = new ArrayList<>(2);
        CarrierDataDto fleet = FleetCarrierManager.getInstance().get();
        if (fleet != null) carriers.add(new Ours(Kind.FLEET, fleet, FleetCarrierManager.getInstance()::save));
        CarrierDataDto squadron = SquadronCarrierManager.getInstance().get();
        if (squadron != null)
            carriers.add(new Ours(Kind.SQUADRON, squadron, SquadronCarrierManager.getInstance()::save));
        return carriers;
    }

    private static Optional<Ours> match(java.util.function.Predicate<CarrierDataDto> test) {
        return both().stream().filter(ours -> test.test(ours.data())).findFirst();
    }
}
