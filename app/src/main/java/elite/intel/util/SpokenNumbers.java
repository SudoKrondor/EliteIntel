package elite.intel.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reads a whole number out of text that may spell it in words - "two hundred" as 200.
 * <p>
 * WHY this exists: a search radius reaches us as an LLM parameter, and what the commander actually said was
 * "within two hundred light years". Speech-to-text writes that in words, and a local model asked for a
 * "number" parameter often hands the words straight back rather than converting them. The old reader kept
 * only {@code [0-9]}, so "two hundred" became an empty string and the radius silently became the default -
 * the commander asks for 200 ly, gets 40, and is told his commodity is nowhere to be found.
 * <p>
 * WHY one table for every language instead of one per language: the parameter carries no language tag, and
 * the commander's language is not necessarily the one the model echoed. Number words collide across our
 * nine locales only where they already agree ("seis" is six in Spanish and in Portuguese), so a single
 * table needs no disambiguation and cannot pick the wrong reading.
 * <p>
 * Scope is deliberately the magnitudes a search radius is spoken in - up to a few thousand light years.
 * Anything it cannot read it reports as absent, so the caller falls back to its default exactly as before.
 */
public final class SpokenNumbers {

    /**
     * Words that carry a value. Keys are lower case and accent-free; see {@link #normalise}.
     */
    private static final Map<String, Integer> UNITS = new HashMap<>();
    /**
     * Words that multiply what came before them ("two HUNDRED").
     */
    private static final Map<String, Integer> MULTIPLIERS = new HashMap<>();

    static {
        // English
        units("zero:0,one:1,two:2,three:3,four:4,five:5,six:6,seven:7,eight:8,nine:9,ten:10,eleven:11,"
                + "twelve:12,thirteen:13,fourteen:14,fifteen:15,sixteen:16,seventeen:17,eighteen:18,nineteen:19,"
                + "twenty:20,thirty:30,forty:40,fifty:50,sixty:60,seventy:70,eighty:80,ninety:90");
        multipliers("hundred:100,thousand:1000");
        // German. Compounds ("zweihundert") are split by decomposition, see readWords.
        units("null:0,eins:1,ein:1,eine:1,zwei:2,drei:3,vier:4,funf:5,sechs:6,sieben:7,acht:8,neun:9,zehn:10,"
                + "elf:11,zwolf:12,dreizehn:13,vierzehn:14,funfzehn:15,sechzehn:16,siebzehn:17,achtzehn:18,"
                + "neunzehn:19,zwanzig:20,dreissig:30,vierzig:40,funfzig:50,sechzig:60,siebzig:70,achtzig:80,neunzig:90");
        multipliers("hundert:100,tausend:1000");
        // Spanish / Portuguese. The shared words agree on value, so they share entries.
        units("cero:0,uno:1,una:1,un:1,dos:2,tres:3,cuatro:4,cinco:5,seis:6,siete:7,ocho:8,nueve:9,diez:10,"
                + "veinte:20,treinta:30,cuarenta:40,cincuenta:50,sesenta:60,setenta:70,ochenta:80,noventa:90,"
                + "zero:0,um:1,uma:1,dois:2,duas:2,tres:3,quatro:4,cinco:5,sete:7,oito:8,nove:9,dez:10,"
                + "vinte:20,trinta:30,quarenta:40,cinquenta:50,sessenta:60,oitenta:80");
        // Spanish and Portuguese inflect the hundreds; each is a standalone value, not a multiplier.
        units("cien:100,ciento:100,doscientos:200,trescientos:300,cuatrocientos:400,quinientos:500,"
                + "seiscientos:600,setecientos:700,ochocientos:800,novecientos:900,"
                + "cem:100,duzentos:200,trezentos:300,quatrocentos:400,quinhentos:500,seiscentos:600,"
                + "setecentos:700,oitocentos:800,novecentos:900");
        multipliers("mil:1000,cientos:100,centos:100");
        // French
        units("zero:0,un:1,une:1,deux:2,trois:3,quatre:4,cinq:5,six:6,sept:7,huit:8,neuf:9,dix:10,onze:11,"
                + "douze:12,treize:13,quatorze:14,quinze:15,seize:16,vingt:20,trente:30,quarante:40,"
                + "cinquante:50,soixante:60");
        multipliers("cent:100,cents:100,mille:1000");
        // Italian
        units("zero:0,uno:1,due:2,tre:3,quattro:4,cinque:5,sei:6,sette:7,otto:8,nove:9,dieci:10,venti:20,"
                + "trenta:30,quaranta:40,cinquanta:50,sessanta:60,settanta:70,ottanta:80,novanta:90");
        multipliers("cento:100,mille:1000,mila:1000");
        // Russian / Ukrainian. The hundreds are irregular, so they are values rather than multipliers.
        units("odin:1,dva:2,tri:3,chetyre:4,pyat:5,"
                + "один:1,одна:1,два:2,дві:2,двa:2,три:3,четыре:4,чотири:4,пять:5,п'ять:5,шесть:6,шість:6,"
                + "семь:7,сім:7,восемь:8,вісім:8,девять:9,дев'ять:9,десять:10,двадцать:20,двадцять:20,"
                + "тридцать:30,тридцять:30,сорок:40,пятьдесят:50,п'ятдесят:50,шестьдесят:60,шістдесят:60,"
                + "семьдесят:70,сімдесят:70,восемьдесят:80,вісімдесят:80,девяносто:90,дев'яносто:90,"
                + "сто:100,двести:200,двісті:200,триста:300,четыреста:400,чотириста:400,пятьсот:500,"
                + "п'ятсот:500,шестьсот:600,шістсот:600,семьсот:700,сімсот:700,восемьсот:800,вісімсот:800,"
                + "девятьсот:900,дев'ятсот:900");
        multipliers("тысяча:1000,тысяч:1000,тисяча:1000,тисяч:1000");
    }

    private SpokenNumbers() {
    }

    private static void units(String spec) {
        put(UNITS, spec);
    }

    private static void multipliers(String spec) {
        put(MULTIPLIERS, spec);
    }

    private static void put(Map<String, Integer> target, String spec) {
        for (String entry : spec.split(",")) {
            String[] pair = entry.split(":");
            // First writer wins: identical words across languages agree on value, so a later table must not
            // overwrite an earlier reading with its own.
            target.putIfAbsent(normalise(pair[0]), Integer.parseInt(pair[1]));
        }
    }

    /**
     * The number {@code text} states, or null when it states none.
     * <p>
     * Digits win over words: "200 ly" is read as 200 without consulting a table, which keeps the common case
     * - a model that did convert - exactly as fast and as predictable as it was.
     */
    public static Integer parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String digits = text.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException tooLong) {
                return null;
            }
        }
        return readWords(normalise(text));
    }

    /**
     * Reads every run of consecutive number words and returns the largest.
     * <p>
     * WHY runs and not one accumulation over the whole text: in most of our languages the indefinite article
     * IS the word for one - French "dans un rayon de deux cents" would otherwise read as 1 + 2 x 100 = 300.
     * A non-number word ends a run, so the article stands alone as its own run of 1 and the radius is read
     * from "deux cents".
     * <p>
     * WHY the largest and not the last: an article reads as 1, which is never a radius anybody means, and a
     * trailing unit ("two hundred light years, maybe one jump") must not displace the number that was asked
     * for. Within a run the accumulation is ordinary: a unit adds, a multiplier scales what came before it,
     * so "one thousand two hundred" stays a single run worth 1200.
     */
    private static Integer readWords(String normalised) {
        int largest = 0;
        int total = 0;
        int pending = 0;
        for (String token : normalised.split("[^\\p{L}']+")) {
            if (token.isEmpty()) {
                continue;
            }
            Integer unit = UNITS.get(token);
            Integer multiplier = MULTIPLIERS.get(token);
            // German writes "zweihundert" as one word; try to read an unknown token as its parts.
            Integer compound = unit == null && multiplier == null ? compoundValue(token) : null;
            if (unit == null && multiplier == null && compound == null) {
                largest = Math.max(largest, total + pending);
                total = 0;
                pending = 0;
                continue;
            }
            if (compound != null) {
                total += compound;
            } else if (multiplier != null) {
                // A bare "hundred" means one hundred, not zero hundreds.
                pending = (pending == 0 ? 1 : pending) * multiplier;
                if (multiplier >= 1000) {
                    total += pending;
                    pending = 0;
                }
            } else {
                pending += unit;
            }
        }
        largest = Math.max(largest, total + pending);
        return largest > 0 ? largest : null;
    }

    /**
     * Reads an agglutinated number word ("zweihundert", "dreihundertfunfzig") by taking the longest known
     * word at each position. Returns null when any part of the token is not a number word, so an ordinary
     * word that merely contains one ("sechsundzwanzigste") does not become a radius.
     */
    private static Integer compoundValue(String token) {
        int total = 0;
        int pending = 0;
        int at = 0;
        boolean matched = false;
        while (at < token.length()) {
            String best = null;
            for (int end = token.length(); end > at; end--) {
                String part = token.substring(at, end);
                if (UNITS.containsKey(part) || MULTIPLIERS.containsKey(part)) {
                    best = part;
                    break;
                }
            }
            if (best == null) {
                // German joins tens and units with "und": dreiundzwanzig.
                if (token.startsWith("und", at)) {
                    at += 3;
                    continue;
                }
                return null;
            }
            Integer multiplier = MULTIPLIERS.get(best);
            if (multiplier != null) {
                pending = (pending == 0 ? 1 : pending) * multiplier;
                if (multiplier >= 1000) {
                    total += pending;
                    pending = 0;
                }
            } else {
                pending += UNITS.get(best);
            }
            matched = true;
            at += best.length();
        }
        total += pending;
        return matched && total > 0 ? total : null;
    }

    /**
     * Lower cases and strips accents, so "années" and "annees" read alike.
     */
    private static String normalise(String text) {
        return java.text.Normalizer.normalize(text.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('’', '\'');
    }
}
