package elite.intel.gameapi.journalimport;

import java.util.*;

/**
 * Which commander each journal belongs to.
 * <p>
 * A journal belongs to the FID its header names. Two kinds name none, and each is settled from the others:
 * <ul>
 *   <li>A journal from before the game wrote FIDs (2018) names the commander only. The name is looked up among the
 *       journals that carry both - when exactly one FID ever went by that name. Names are only for display and two
 *       commanders can share one, so an ambiguous name settles nothing.</li>
 *   <li>A continuation part ({@code .02.log} on) repeats nothing and belongs to the part before it, when that
 *       part was found too.</li>
 * </ul>
 * A journal still unsettled after that belongs to no one: it teaches only what belongs to everyone.
 */
final class JournalOwners {

    private JournalOwners() {
    }

    /**
     * @param journals oldest first, each with its header
     * @return the owning FID of each journal, in the same order, empty for no one
     */
    static List<Optional<String>> resolve(List<FoundJournal> journals, List<JournalHeaderInfo> headers) {
        Map<String, String> fidByName = fidsByName(headers);
        List<Optional<String>> owners = new ArrayList<>(journals.size());
        for (int i = 0; i < journals.size(); i++) {
            JournalHeaderInfo header = headers.get(i);
            String fid = header.fid();
            if (fid == null && header.commanderName() != null) {
                fid = fidByName.get(header.commanderName());
            }
            if (fid == null && header.commanderName() == null && continues(journals, i)) {
                fid = owners.get(i - 1).orElse(null);
            }
            owners.add(Optional.ofNullable(fid));
        }
        return owners;
    }

    /**
     * Whether journal {@code i} is the next part of the session before it: one part on from the journal just before
     * it. The stamp is not compared - the game stamps a continuation with the moment it opened the new file.
     */
    private static boolean continues(List<FoundJournal> journals, int i) {
        if (i == 0) return false;
        var part = journals.get(i).stamp();
        var previous = journals.get(i - 1).stamp();
        return part.part() > 1 && part.part() == previous.part() + 1;
    }

    private static Map<String, String> fidsByName(List<JournalHeaderInfo> headers) {
        Map<String, Set<String>> seen = new HashMap<>();
        for (JournalHeaderInfo header : headers) {
            if (header.fid() != null && header.commanderName() != null) {
                seen.computeIfAbsent(header.commanderName(), name -> new HashSet<>()).add(header.fid());
            }
        }
        Map<String, String> unambiguous = new HashMap<>();
        seen.forEach((name, fids) -> {
            if (fids.size() == 1) unambiguous.put(name, fids.iterator().next());
        });
        return unambiguous;
    }
}
