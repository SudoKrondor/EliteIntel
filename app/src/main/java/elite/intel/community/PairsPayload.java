package elite.intel.community;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.dao.MassacreMissionDao.Shareable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The {@code pairs} string this install uploads to {@code reports/{uid}}: payload format 1.
 * <p>
 * It is a JSON array with one object per provider faction and target:
 * <pre>
 * [{"ps":"Bean","st":"Bruce Dock","pf":"Earls of Bean","ts":"Bellite","tf":"Bellite Raiders","r":[0,1,2,0],"d":"2026-11-13"}]
 * </pre>
 * The nightly job's {@code PairsPayload} reads this, and the two must agree. It drops an entry with a name longer
 * than {@value #MAX_NAME} characters, a resource-site count outside 0 to {@value #MAX_SITES}, or a date that is
 * not {@code YYYY-MM-DD}, so this encoder never writes one. Keys are short because the security rules cap the
 * whole string at {@value #MAX_LENGTH} characters.
 */
final class PairsPayload {

    static final int FORMAT = 1;
    static final int MAX_LENGTH = 64_000;
    private static final int MAX_NAME = 64;
    private static final int MAX_SITES = 30;
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private PairsPayload() {
    }

    /**
     * The entries in a stable order, so the same ledger always encodes to the same string and an unchanged
     * report is recognised as unchanged. Entries the nightly job would refuse are left out.
     * <p>
     * WHY the most recently seen entries win when the ledger outgrows the limit: seven years of heavy massacre
     * play came to a fifth of it, so this only matters for a ledger far larger than any seen, and then the
     * contracts taken lately say the most about what is still on offer.
     */
    static String encode(List<Shareable> entries) {
        List<Shareable> newestFirst = entries.stream()
                .filter(PairsPayload::isAcceptable)
                .sorted(Comparator.comparing(Shareable::lastSeen, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<JsonObject> kept = new ArrayList<>();
        int length = 2; // the brackets
        for (Shareable e : newestFirst) {
            JsonObject o = entry(e);
            int added = o.toString().length() + (kept.isEmpty() ? 0 : 1);
            if (length + added > MAX_LENGTH) break;
            kept.add(o);
            length += added;
        }
        JsonArray array = new JsonArray();
        kept.stream().sorted(Comparator.comparing(JsonObject::toString)).forEach(array::add);
        return array.toString();
    }

    static String digest(String payload) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    private static boolean isAcceptable(Shareable e) {
        return isName(e.providerSystem()) && isName(e.providerFaction())
                && isName(e.targetSystem()) && isName(e.targetFaction())
                && !e.providerSystem().strip().equalsIgnoreCase(e.targetSystem().strip());
    }

    private static JsonObject entry(Shareable e) {
        JsonObject o = new JsonObject();
        o.addProperty("ps", e.providerSystem().strip());
        if (isName(e.providerStation())) o.addProperty("st", e.providerStation().strip());
        o.addProperty("pf", e.providerFaction().strip());
        o.addProperty("ts", e.targetSystem().strip());
        o.addProperty("tf", e.targetFaction().strip());
        JsonArray sites = new JsonArray();
        sites.add(clamp(e.standard()));
        sites.add(clamp(e.low()));
        sites.add(clamp(e.high()));
        sites.add(clamp(e.hazardous()));
        o.add("r", sites);
        if (e.lastSeen() != null && DATE.matcher(e.lastSeen()).matches()) o.addProperty("d", e.lastSeen());
        return o;
    }

    private static boolean isName(String value) {
        if (value == null) return false;
        String s = value.strip();
        return !s.isEmpty() && s.length() <= MAX_NAME && s.chars().noneMatch(Character::isISOControl);
    }

    private static int clamp(int sites) {
        return Math.max(0, Math.min(MAX_SITES, sites));
    }
}
