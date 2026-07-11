package elite.intel.companion.prompt;

import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.model.GameStateSnapshot;
import elite.intel.companion.prompt.ReflexResolver.CommandPhrase;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reflex gate: an input is a reflex only when it matches a training phrase verbatim and resolves to
 * exactly one safe, parameterless command. Word overlap, an ambiguous tie, a parameterized command and a
 * dangerous command are all rejected (they take the LLM / confirmation path instead).
 */
class ReflexResolverTest {

    private static final DangerousActionPolicy NOTHING_DANGEROUS = invocation -> false;

    private static ReflexResolver resolver(List<CommandPhrase> commands, DangerousActionPolicy danger) {
        return new ReflexResolver(() -> commands, danger);
    }

    @Test
    void verbatimSingleSafeParameterlessCommandIsAReflex() {
        ReflexResolver resolver = resolver(
                List.of(new CommandPhrase("open_nav", "open navigation, nav panel", true)), NOTHING_DANGEROUS);

        assertEquals(Optional.of("open_nav"), resolver.resolve("open navigation"));
        assertEquals(Optional.of("open_nav"), resolver.resolve("nav panel"), "any phrase in the group counts");
    }

    @Test
    void matchIsCaseInsensitiveAndTrimmed() {
        ReflexResolver resolver = resolver(List.of(new CommandPhrase("honk", "honk", true)), NOTHING_DANGEROUS);

        assertEquals(Optional.of("honk"), resolver.resolve("  HONK  "));
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

        assertEquals(Optional.of("combat"), resolver.resolve("combat mode", turnState));
        assertSame(turnState, observed.get(), "exact matching must use the owning turn's immutable state");
    }

    @Test
    void trailingSentencePunctuationIsIgnored() {
        ReflexResolver resolver = resolver(List.of(new CommandPhrase(
                        "query_carrier_voyage", "qual è la destinazione finale della fleet carrier", true)),
                NOTHING_DANGEROUS);

        // A spoken question keeps its '?', but the alias is stored plain - the reflex must still match.
        assertEquals(Optional.of("query_carrier_voyage"),
                resolver.resolve("qual è la destinazione finale della fleet carrier?"));
        assertEquals(Optional.of("query_carrier_voyage"),
                resolver.resolve("Qual è la destinazione finale della fleet carrier!"));
    }

    @Test
    void nonVerbatimInputIsNotAReflex() {
        ReflexResolver resolver = resolver(
                List.of(new CommandPhrase("open_nav", "open navigation", true)), NOTHING_DANGEROUS);

        assertTrue(resolver.resolve("open the navigation panel please").isEmpty(),
                "word overlap is not enough - the phrase must match verbatim");
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
