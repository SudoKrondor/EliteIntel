package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.ai.brain.i18n.AliasPhrase;
import elite.intel.ai.brain.i18n.AliasVocabulary;
import elite.intel.ai.brain.vega.confirm.DangerousActionPolicy;
import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.ai.brain.vega.prompt.ReflexResolver.CommandPhrase;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The reflex gate: an input is a reflex only when it matches a training phrase - word for word, or as a
 * damaged transcript of one - and resolves to exactly one safe action whose every required argument is already
 * known. Word overlap, an ambiguous tie, an action still needing argument extraction and a dangerous action
 * are all rejected (they take the LLM / confirmation path instead).
 */
class ReflexResolverTest {

    private static final DangerousActionPolicy NOTHING_DANGEROUS = invocation -> false;

    /**
     * Injects the vocabulary alongside the commands, so a case is matched against its own aliases rather than
     * against whichever words the shipped bundles happen to contain. Mirrors production, where every word of
     * every authored alias is by definition a word we wrote.
     */
    private static ReflexResolver resolver(List<CommandPhrase> commands, DangerousActionPolicy danger) {
        return new ReflexResolver(() -> commands, danger, () -> vocabularyOf(commands));
    }

    private static Set<String> vocabularyOf(List<CommandPhrase> commands) {
        Set<String> words = new HashSet<>();
        for (CommandPhrase command : commands) {
            for (String phrase : AiActionLocalizations.splitPhraseGroup(command.phraseGroup())) {
                words.addAll(AliasVocabulary.tokenize(AliasPhrase.parse(phrase).spokenText()));
            }
        }
        return words;
    }

    /**
     * The resolved action id alone, for the cases that assert routing rather than arguments.
     */
    private static Optional<String> actionId(Optional<ReflexResolver.Reflex> reflex) {
        return reflex.map(ReflexResolver.Reflex::actionId);
    }

    @Test
    void verbatimSingleSafeParameterlessCommandIsAReflex() {
        ReflexResolver resolver = resolver(
                List.of(new CommandPhrase("open_nav", "open navigation, nav panel", true)), NOTHING_DANGEROUS);

        assertEquals(Optional.of("open_nav"), actionId(resolver.resolve("open navigation")));
        assertEquals(Optional.of("open_nav"), actionId(resolver.resolve("nav panel")),
                "any phrase in the group counts");
    }

    @Test
    void matchIsCaseInsensitiveAndTrimmed() {
        ReflexResolver resolver = resolver(List.of(new CommandPhrase("honk", "honk", true)), NOTHING_DANGEROUS);

        assertEquals(Optional.of("honk"), actionId(resolver.resolve("  HONK  ")));
    }

    @Test
    void exactReflexUsesTheSuppliedTurnSnapshot() {
        GameStateSnapshot turnState = GameStateSnapshot.capture(Status.detached(PlayerSituation.IN_SHIP_DEEP_SPACE));
        AtomicReference<GameStateSnapshot> observed = new AtomicReference<>();
        ReflexResolver resolver = new ReflexResolver(snapshot -> {
            observed.set(snapshot);
            return snapshot.visibilityStatus().isInMainShip()
                    ? List.of(new CommandPhrase("combat", "combat mode", true))
                    : List.of();
        }, NOTHING_DANGEROUS);

        assertEquals(Optional.of("combat"), actionId(resolver.resolve("combat mode", turnState)));
        assertSame(turnState, observed.get(), "exact matching must use the owning turn's immutable state");
    }

    @Test
    void trailingSentencePunctuationIsIgnored() {
        ReflexResolver resolver = resolver(List.of(new CommandPhrase(
                        "query_carrier_voyage", "qual è la destinazione finale della fleet carrier", true)),
                NOTHING_DANGEROUS);

        // A spoken question keeps its '?', but the alias is stored plain - the reflex must still match.
        assertEquals(Optional.of("query_carrier_voyage"),
                actionId(resolver.resolve("qual è la destinazione finale della fleet carrier?")));
        assertEquals(Optional.of("query_carrier_voyage"),
                actionId(resolver.resolve("Qual è la destinazione finale della fleet carrier!")));
    }

    @Test
    void looseWordOverlapIsNotAReflex() {
        ReflexResolver resolver = resolver(
                List.of(new CommandPhrase("open_nav", "open navigation", true)), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("open the navigation panel please").isEmpty(),
                "sharing words is not enough - the phrase must match the alias word for word, or be a "
                        + "same-length damaged rendering of it");
    }

    /**
     * The transcript is the only text in this pipeline nobody authored, so a mis-transcribed word is repaired
     * rather than escalated - but only while every other word of the phrase still agrees.
     */
    @Test
    void aDamagedWordIsStillAReflexWhenTheRestOfThePhraseAgrees() {
        ReflexResolver resolver = resolver(
                List.of(new CommandPhrase("open_nav", "open navigation", true)), NOTHING_DANGEROUS);

        assertEquals(Optional.of("open_nav"), actionId(resolver.resolve("open navigashon")));
        assertEquals(ReflexResolver.MatchKind.FUZZY,
                resolver.resolve("open navigashon").orElseThrow().matchKind());
        assertEquals(ReflexResolver.MatchKind.EXACT,
                resolver.resolve("open navigation").orElseThrow().matchKind(),
                "an authored alias must never be reported as a repair");
    }

    /**
     * A word that appears in some alias was written by us, so the commander said it. Without this the repair
     * would quietly rewrite one command into another that happens to sit one letter away.
     */
    @Test
    void aWordWeAuthoredIsNeverTreatedAsDamage() {
        ReflexResolver resolver = resolver(List.of(
                new CommandPhrase("launch_fighter", "launch fighter", true),
                new CommandPhrase("find_lunch", "lunch spot", true)), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("lunch fighter").isEmpty(),
                "\"lunch\" is a word we wrote, so it is not a mis-heard \"launch\"");
    }

    /**
     * Two actions reachable from one damaged phrase is the case the reflex must not gamble on, exactly as for
     * a shared verbatim phrase.
     */
    @Test
    void anAmbiguousRepairIsNotAReflex() {
        ReflexResolver resolver = resolver(List.of(
                new CommandPhrase("deploy_gear", "deploy gear", true),
                new CommandPhrase("deploy_nets", "deploy near", true)), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("deploy xear").isEmpty(), "one letter from both - the LLM disambiguates");
    }

    @Test
    void ambiguousMatchIsNotAReflex() {
        ReflexResolver resolver = resolver(List.of(
                new CommandPhrase("open_nav", "panel", true),
                new CommandPhrase("open_systems", "panel", true)), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("panel").isEmpty(), "two commands share the phrase - the LLM disambiguates");
    }

    @Test
    void parameterizedCommandIsNotAReflex() {
        ReflexResolver resolver = resolver(
                List.of(new CommandPhrase("set_speed", "set speed", false)), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("set speed").isEmpty(), "parameters need the LLM to extract arguments");
    }

    /**
     * The subsystem-targeting case this gate exists for: "target fsd" and the query "fsd target info" are one
     * token apart, so a small local model picks between them unreliably - but the alias already says which
     * subsystem it means, leaving nothing to extract.
     */
    @Test
    void aliasSuppliedArgumentIsAReflexAndCarriesItsValue() {
        ReflexResolver resolver = resolver(List.of(new CommandPhrase(
                "target_subsystem",
                "target fsd {key:fsd}, target engines {key:drive}",
                Set.of("key"))), NOTHING_DANGEROUS);

        assertEquals(Optional.of(new ReflexResolver.Reflex("target_subsystem", Map.of("key", "fsd"))),
                resolver.resolve("target fsd"));
        assertEquals(Optional.of(new ReflexResolver.Reflex("target_subsystem", Map.of("key", "drive"))),
                resolver.resolve("target engines"),
                "each phrase in the group carries its own subsystem");
    }

    @Test
    void placeholderArgumentStillNeedsTheLlm() {
        ReflexResolver resolver = resolver(List.of(new CommandPhrase(
                "increase_speed", "increase speed by {key:X}", Set.of("key"))), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("increase speed by").isEmpty(),
                "a placeholder value stands for commander wording - only the LLM can read it");
    }

    @Test
    void aliasMissingAnArgumentIsNotAReflex() {
        ReflexResolver resolver = resolver(List.of(new CommandPhrase(
                "find_commodity", "find gold {key:gold}", Set.of("key", "max_distance"))), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("find gold").isEmpty(),
                "the alias pins down key but not max_distance, so the action cannot run from the phrase alone");
    }

    /**
     * An unset optional parameter keeps the LLM path on purpose: the bare alias may be the start of a phrase the
     * commander qualifies ("navigate to the mission" vs "... to mission alpha"), and only the model can tell.
     */
    @Test
    void unsuppliedOptionalParameterIsNotAReflex() {
        ReflexResolver resolver = resolver(List.of(new CommandPhrase(
                "navigate_to_active_mission", "navigate to the active mission", Set.of("key"))), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("navigate to the active mission").isEmpty());
    }

    @Test
    void dangerousCommandIsNotAReflex() {
        DangerousActionPolicy selfDestructDangerous = invocation -> "self_destruct".equals(invocation.name());
        ReflexResolver resolver = resolver(
                List.of(new CommandPhrase("self_destruct", "self destruct", true)), selfDestructDangerous);

        assertTrue(resolver.resolve("self destruct").isEmpty(), "a dangerous command keeps its confirmation flow");
    }

    @Test
    void noCommandMatchIsNotAReflex() {
        ReflexResolver resolver = resolver(List.of(new CommandPhrase("honk", "honk", true)), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("how are you doing").isEmpty());
    }

    @Test
    void blankOrNullInputIsNotAReflex() {
        ReflexResolver resolver = resolver(List.of(new CommandPhrase("honk", "honk", true)), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("   ").isEmpty());
        assertTrue(resolver.resolve(null).isEmpty());
    }
}
