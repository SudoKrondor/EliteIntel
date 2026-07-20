package elite.intel.ai.brain.vega.memory.facts;

import elite.intel.ai.brain.vega.memory.facts.MergedFactCandidates;
import elite.intel.ai.brain.vega.prompt.Fact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergedFactCandidatesTest {

    @Test
    void preservesRegisteredSourceOrder() {
        List<Fact> sourceFacts = List.of(new Fact("ship a", "ship"), new Fact("system a", "system"));

        assertEquals(sourceFacts, MergedFactCandidates.merge(sourceFacts));
    }

    @Test
    void capsEachSourceAtTwo() {
        List<Fact> sourceFacts = List.of(
                new Fact("a", "s"), new Fact("b", "s"), new Fact("c", "s"));

        assertEquals(List.of(new Fact("a", "s"), new Fact("b", "s")),
                MergedFactCandidates.merge(sourceFacts));
    }

    @Test
    void capsTotalAtSixAndDeduplicatesCaseInsensitively() {
        List<Fact> sourceFacts = List.of(
                new Fact("Field is Bedlam", "a"), new Fact("a1", "a"),
                new Fact("field IS bedlam", "b"), new Fact("b1", "b"), new Fact("b2", "b"),
                new Fact("c1", "c"), new Fact("c2", "c"), new Fact("d1", "d"));

        List<Fact> result = MergedFactCandidates.merge(sourceFacts);

        assertEquals(6, result.size());
        assertTrue(result.stream().noneMatch(fact -> fact.text().equals("field IS bedlam")));
        assertEquals(new Fact("c2", "c"), result.getLast());
    }
}
