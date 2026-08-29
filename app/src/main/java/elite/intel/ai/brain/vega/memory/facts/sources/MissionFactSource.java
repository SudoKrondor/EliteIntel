package elite.intel.ai.brain.vega.memory.facts.sources;

import elite.intel.ai.brain.vega.memory.facts.MemoryFactContext;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactSource;
import elite.intel.ai.brain.vega.memory.facts.RegisterMemoryFactSource;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MissionSelection;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Always-on fact source for the job the commander is being paid for: the featured mission of the accepted stack,
 * what it asks for, where it has to be delivered and how long is left on it.
 * <p>
 * Ambient for a commander turn rather than gated on a subject, because an accepted contract is the frame the
 * commander is flying in and not the answer to one question - the deadline in particular changes what an answer
 * about anything else should say. It is silent whenever the stack is empty.
 * <p>
 * Which mission is "the" mission comes from {@link MissionSelection#featured}, the same rule the HUD's mission card
 * uses: the plotted route's destination first, then soonest expiry. A card and a spoken line naming different
 * missions is the app arguing with itself.
 * <p>
 * The full stack stays with {@code query_missions_and_rewards}; this is one line of grounding, and the block's own
 * rules already tell the model that relevance-limited facts cannot prove a complete list.
 */
@RegisterMemoryFactSource
public final class MissionFactSource implements MemoryFactSource {

    /**
     * Provenance label for the {@code <fact source="...">} attribute.
     */
    private static final String ID = "mission";

    /**
     * Room the mission's own name may take on the line. The journal's {@code LocalisedName} is unbounded and
     * localized, and a long one would otherwise crowd out the destination and the deadline, which are the parts
     * the commander can act on.
     */
    private static final int TITLE_MAX_CHARS = 60;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isRelevant(MemoryFactContext context) {
        return context != null && context.source() == ThoughtSource.COMMANDER;
    }

    @Override
    public boolean isAmbient() {
        return true;
    }

    @Override
    public List<String> factsFor(MemoryFactContext context) {
        // A malformed row deserialises to null, so the stack is cleaned before the selection rule sees it.
        List<MissionDto> missions = MissionManager.getInstance().getMissions().values().stream()
                .filter(Objects::nonNull)
                .toList();
        if (missions.isEmpty()) {
            return List.of();
        }
        // A non-empty stack always has a featured mission, so this is the last decision the source makes.
        return MissionSelection.featured(missions, ShipRouteManager.getInstance().getDestination())
                .map(featured -> List.of(format(featured, missions.size(), Instant.now())))
                .orElseGet(List::of);
    }

    /**
     * Builds the single mission line: what it is, then where it goes, then what it asks for, then its deadline, and
     * last how much else is accepted. Ordered so the shared line cap trims the stack size rather than the delivery.
     * Pure and package-visible so the wording and the deadline can be pinned without a database or a clock.
     */
    static String format(MissionDto mission, int stackSize, Instant now) {
        List<String> parts = new ArrayList<>();
        addIf(parts, destination(mission));
        addIf(parts, requirement(mission));
        addIf(parts, deadline(mission, now));
        if (stackSize > 1) {
            parts.add(stackSize + " missions accepted");
        }
        String head = FactLine.shortened(FactLine.value(title(mission)), TITLE_MAX_CHARS);
        return FactLine.capped(head == null ? "current mission" : "current mission " + head, parts);
    }

    /**
     * The journal's own name for the mission, falling back to its family when the journal named it nothing.
     */
    private static String title(MissionDto mission) {
        String description = FactLine.value(mission.getMissionDescription());
        if (description != null) {
            return description;
        }
        // The type's constant name is an identifier, never a label; label() is the readable form.
        return mission.getMissionType() == null ? null : mission.getMissionType().label();
    }

    private static String destination(MissionDto mission) {
        String system = FactLine.value(mission.getDestinationSystem());
        String station = FactLine.value(mission.getDestinationStation());
        if (system == null) {
            return station == null ? null : "hand in at " + station;
        }
        return station == null ? "to " + system : "to " + system + ", " + station;
    }

    /**
     * What the contract asks for, in the same small set of families the HUD card covers: anything else falls
     * through to no part rather than inventing a label for it.
     */
    private static String requirement(MissionDto mission) {
        if (mission.getKillCount() > 0) {
            String faction = FactLine.value(mission.getMissionTargetFaction());
            return "kill " + mission.getKillCount() + (faction == null ? "" : " of " + faction);
        }
        String commodity = FactLine.value(mission.getCommodityName());
        if (commodity != null) {
            return mission.getCount() > 0 ? "deliver " + mission.getCount() + " " + commodity : "deliver " + commodity;
        }
        if (mission.getPassengerCount() > 0) {
            return "carry " + mission.getPassengerCount() + " passengers";
        }
        return null;
    }

    /**
     * How long is left, or that it has already run out. Missions that never expire carry no expiry at all and get
     * no part; an unparseable one is treated the same way rather than guessed at.
     */
    private static String deadline(MissionDto mission, Instant now) {
        Optional<Instant> expiry = expiryOf(mission);
        if (expiry.isEmpty()) {
            return null;
        }
        Duration left = Duration.between(now, expiry.get());
        if (left.isNegative() || left.isZero()) {
            return "expired";
        }
        return "expires in " + shortDuration(left);
    }

    /**
     * Compact English duration for the model; the commander's own units are the HUD card's job.
     */
    static String shortDuration(Duration left) {
        long days = left.toDays();
        if (days > 0) {
            return days + "d " + left.toHoursPart() + "h";
        }
        long hours = left.toHours();
        if (hours > 0) {
            return hours + "h " + left.toMinutesPart() + "m";
        }
        return Math.max(1, left.toMinutes()) + "m";
    }

    /**
     * Journal expiry timestamps are ISO-8601; anything unparseable is treated as absent.
     */
    private static Optional<Instant> expiryOf(MissionDto mission) {
        String raw = mission.getExpiry();
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(raw.trim()));
        } catch (DateTimeParseException unparseable) {
            return Optional.empty();
        }
    }

    private static void addIf(List<String> parts, String value) {
        if (value != null) {
            parts.add(value);
        }
    }
}
