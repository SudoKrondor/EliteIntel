package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.builtin.FighterAttackTargetCommand;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.AliasPhrase;
import elite.intel.ai.brain.i18n.AliasVocabulary;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Every alias of {@code fighter_attack_target} must say, in its own language, "attack my target" - the verb,
 * the possessive and the noun, all three.
 *
 * <p>WHY: this is the one fighter order that cannot be taken back. The fighter opens fire on whatever is
 * selected, and if that is a clean or allied ship the commander carries the bounty until they fly to an
 * Interstellar Factors contact and buy it off. Every other fighter order (recall, defend, hold fire, deploy)
 * is free to be guessed at; this one is not.
 *
 * <p>The failure this locks out was real. The group used to carry bare "attack", "fighter attack", "focus
 * target" and "focus on target". Those short, generic phrases are what the semantic reducer embeds, so they
 * pulled the command into the shortlist for anything vaguely combat-shaped - and the transcript "fight the
 * return to base" (a misheard "fighter return to base") scored inside the cutoff band and was settled as
 * {@code fighter_attack_target}. The fighter shot an ally.
 *
 * <p>Requiring the possessive keeps the phrases long and specific enough that a damaged transcript of a
 * <em>different</em> fighter order cannot reach them, and it is what the commander actually says when they
 * mean it. The cost, taken deliberately: the in-game order is labelled "Focus Target", and that wording no
 * longer routes here - the model may still pick the command from its description, but no alias hands it over.
 *
 * @see FighterAttackTargetCommand
 * @see SemanticActionReducer
 */
class FighterAttackTargetPhrasingTest {

    /**
     * Per language, the word stems an alias must all contain: the attack verb, the possessive, the target
     * noun. Alternatives inside a group cover inflection and word order ("greife"/"angreifen",
     * "атакуй"/"атаковать"); a token counts when it starts with one of them.
     */
    private static final Map<Language, List<List<String>>> REQUIRED_STEMS = Map.ofEntries(
            entry(Language.EN, List.of(List.of("attack"), List.of("my"), List.of("target"))),
            entry(Language.RU, List.of(List.of("атак"), List.of("мою", "моей"), List.of("цел"))),
            entry(Language.UK, List.of(List.of("атак"), List.of("мою", "моїй"), List.of("ціл"))),
            entry(Language.DE, List.of(List.of("greif", "angreif"), List.of("mein"), List.of("ziel"))),
            entry(Language.FR, List.of(List.of("attaqu"), List.of("ma"), List.of("cible"))),
            entry(Language.ES, List.of(List.of("atac", "ataqu"), List.of("mi"), List.of("objetivo"))),
            entry(Language.IT, List.of(List.of("attacc"), List.of("mio"), List.of("bersaglio"))),
            entry(Language.PT, List.of(List.of("atac"), List.of("meu"), List.of("alvo"))),
            entry(Language.PTBZ, List.of(List.of("atac"), List.of("meu"), List.of("alvo")))
    );

    @ParameterizedTest(name = "{0} orders the attack in full words")
    @EnumSource(Language.class)
    void everyAliasNamesTheAttackAndTheTarget(Language language) {
        SystemSession.getInstance().setLanguage(language);
        List<List<String>> required = REQUIRED_STEMS.get(language);
        assertNotNull(required,
                () -> "No required phrasing declared for " + language + "; a new language must declare its own "
                        + "attack verb, possessive and target noun here before its aliases are trusted.");

        List<String> phrases = AiActionLocalizations.phrasesForAction(FighterAttackTargetCommand.ID);
        assertFalse(phrases.isEmpty(), () -> language + " has no aliases for " + FighterAttackTargetCommand.ID);

        for (String phrase : phrases) {
            List<String> tokens = AliasVocabulary.tokenize(AliasPhrase.parse(phrase).spokenText());
            for (List<String> group : required) {
                assertTrue(tokens.stream().anyMatch(token -> startsWithAny(token, group)),
                        () -> language + " alias \"" + phrase + "\" for " + FighterAttackTargetCommand.ID
                                + " is missing " + group + ". A phrase short of a full \"attack my target\" "
                                + "becomes a magnet for damaged transcripts of the other fighter orders.");
            }
        }
    }

    private static boolean startsWithAny(String token, List<String> stems) {
        String lower = token.toLowerCase(Locale.ROOT);
        return stems.stream().anyMatch(lower::startsWith);
    }
}
