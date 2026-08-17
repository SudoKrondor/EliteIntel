package elite.intel.ai.brain.i18n.en;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** English STT acoustic corrections and input filters used by companion routing. */
public class EnglishInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public LinkedHashMap<String, String> buildPhoneticMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        // "off" is only ever heard as "of" inside these fixed phrases. A blanket of -> off correction used to
        // sit here and was worse than no correction at all: it rewrote the ordinary preposition too, so
        // "transfer power of the systems" reached the model as "power OFF the systems" and was refused as a
        // request to shut power down. Phrase at a time, never a word on its own.
        m.put("take of", "take off");
        m.put("lift of", "lift off");
        m.put("turn of", "turn off");
        m.put("lights of", "lights off");
        m.put("manax", "max");
        m.put("hard points", "hardpoints");
        m.put("scott", "scan");
        m.put("scale", "scan");
        m.put("mining spots", "mining hot spots");
        m.put("net vision", "toggle night vision");
        m.put("division", "toggle night vision");
        m.put("her style", "hostile");
        m.put("hair style", "hostile");
        m.put("did", "deploy");
        m.put("did ploy", "deploy");
        m.put("do they play", "deploy");
        m.put("perimeter", "enter");
        m.put("exit this window", "exit");
        m.put("spectrum scan", "scan system");
        m.put("full spectrum scan", "FSS");
        m.put("full-spectrum scan", "FSS");
        m.put("nicolai has", "equalize");
        m.put("mitigation", "navigation");
        m.put("codec", "codex");
        m.put("kodak", "codex");
        m.put("they make me", "take me");
        m.put("products", "codex");
        m.put("sleep carrier", "fleet carrier");
        m.put("navigate zip", "exit");
        m.put("first to", "what is");
        m.put("repair", "radar");
        m.put("scam", "scan");
        m.put("rfss", "fss");
        m.put("displayed", "display");
        m.put("i think it's it", "exit");
        m.put("are two", "power to");
        m.put("motor car of", "recover");
        m.put("allocation", "location");
        m.put("distance", "distance");
        m.put("fields", "shields");
        m.put("power two", "power to");
        // Unstressed "to" reaches the transcript as "of" or "two", and the article is usually spoken even
        // though the aliases are authored without it. Collapsing all of them onto "power to <capacitor>" is
        // what keeps the phrase a reflex instead of a question for the model. Ordered after "power two" so
        // that repair has already run when the article rule looks at the text.
        m.put("power of the", "power to");
        m.put("power to the", "power to");
        m.put("power of", "power to");
        m.put("what are the systems", "power to systems");
        m.put("continuation", "connection");
        m.put("and is it", "exit");
        m.put("think that it", "exit");
        m.put("I am going to cover", "recover");
        m.put("we recall the", "recover");
        m.put("carcass too", "cargo scoop");
        m.put("pergascope", "cargo scoop");
        m.put("flint", "fleet");
        m.put("fleet crater", "fleet carrier");
        m.put("litigation", "navigation");
        m.put("survey", "SRV");
        m.put("product center", "codex entry");
        m.put("council", "cancel");
        m.put("scalar", "scanner");
        // Parakeet returns all three of these for "landing", most often in "request landing permission".
        // None of them is an Elite word, so the correction is unconditional rather than phrase-bound.
        m.put("lensing", "landing");
        m.put("lending", "landing");
        m.put("lansing", "landing");
        m.put("team", "tin");
        m.put("karga", "cargo");
        m.put("skoop", "scoop");
        m.put("cmm commodities", "cmm composites");
        m.put("alex sounds right", "alexandrite");
        m.put("next zip", "exit");
        m.put("recovered", "recover");
        m.put("break over", "recover");
        m.put("from seoul", "from sol");
        m.put("seoul", "sol");
        m.put("roll", "role");
        m.put("career", "carrier");
        m.put("sip", "ship");
        m.put("aligns", "launch");
        m.put("flip", "fleet");
        return m;
    }

    @Override
    public List<String> trashPhrases() {
        return List.of(
                "--", "mm-hmm", "uh-huh", "hmm", "mm", "uh", "um", "ah", "oh", "huh", "eh",
                "yeah", "yep", "yup", "nope", "it", "an", "cool", "the",
                "okay", "ok", "got it", "alright", "alrighty", "sure", "right",
                "hello", "hi", "hey", "bye", "goodbye",
                "so", "well", "now", "anyway", "actually", "basically", "literally",
                "thanks", "thank you", "i'm sorry", "sorry", "excuse me", "pardon",
                "you know", "i see", "i mean", "of course", "no problem",
                "i got it", "don't i", "a ", "or ", "she can", "he can", "you can",
                "like they", "did you", "wh", "i'll", "like", "got a",
                "blow", "fuck", "shit", "just", "i ");
    }

    @Override
    public Set<String> stopWords() {
        return Set.of(
                "blow", "fuck", "shit", "piss", "cunt", "cock", "cocksucker", "motherfucker",
                "a", "an", "the", "to", "of", "in", "on", "at", "by", "for",
                "with", "and", "or", "is", "are", "am", "be", "do", "does",
                "what", "where", "how", "which", "any", "our", "my", "me",
                "we", "us", "i", "you", "it", "this", "that", "get", "have",
                "has", "can", "could", "would", "should", "not", "no", "up",
                "here", "there", "some", "much", "many");
    }
}
