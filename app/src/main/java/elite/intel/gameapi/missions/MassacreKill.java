package elite.intel.gameapi.missions;

import elite.intel.gameapi.journal.events.dto.BountyDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * One kill the journal paid the commander for, as {@link MassacreProgress} counts it toward a massacre contract.
 * <p>
 * A pirate massacre is paid in bounty vouchers ({@code Bounty}), a war massacre in combat bonds
 * ({@code FactionKillBond}): a conflict zone holds only two sides, so every bond against the contract's target
 * faction is a kill of one of its ships. Both say the same two things the count needs - whose ship it was and
 * when - and nothing else about them matters to it.
 *
 * @param victimFaction the faction the destroyed ship flew for
 * @param earnedAt      the journal timestamp of the payment, ISO-8601, or null on a record older than timestamps
 */
public record MassacreKill(String victimFaction, String earnedAt) {

    /**
     * The bounties that are kills. A voucher with no pilot is not a ship destroyed - the game pays bounties for
     * other things too - and was never counted.
     */
    public static List<MassacreKill> fromBounties(Collection<BountyDto> bounties) {
        return bounties.stream()
                .filter(Objects::nonNull)
                .filter(bounty -> bounty.getPilotName() != null)
                .map(bounty -> new MassacreKill(bounty.getVictimFaction(), bounty.getEarnedAt()))
                .toList();
    }

    /**
     * Every kill the journal paid for, whichever way it paid.
     */
    public static List<MassacreKill> of(Collection<BountyDto> bounties, Collection<MassacreKill> bonds) {
        List<MassacreKill> kills = new ArrayList<>(fromBounties(bounties));
        kills.addAll(bonds);
        return kills;
    }
}
