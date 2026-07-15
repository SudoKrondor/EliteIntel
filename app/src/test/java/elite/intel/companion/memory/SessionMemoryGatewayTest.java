package elite.intel.companion.memory;

import elite.intel.ai.embed.AngleEmbedder;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticQuery;
import elite.intel.ai.embed.TextEmbedder;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.memory.ToolLink;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2 memory spine: short-term timeline and count/token eviction into mid-term by topic. A
 * fixed-cost token estimator makes the budget eviction deterministic.
 */
class SessionMemoryGatewayTest {

    /** Topics that currently hold mid-term entries, in enum order (observed via the public per-topic recall). */
    private static List<ConversationTopic> midTermTopics(SessionMemoryGateway gateway) {
        List<ConversationTopic> topics = new ArrayList<>();
        for (ConversationTopic topic : ConversationTopic.values()) {
            if (!gateway.recallTopicMemory(topic, null, 1000).isEmpty()) {
                topics.add(topic);
            }
        }
        return topics;
    }

    /** Every entry costs a constant number of tokens, independent of content length. */
    private static final class FixedTokenEstimator implements TokenEstimator {
        private final int perCall;

        FixedTokenEstimator(int perCall) {
            this.perCall = perCall;
        }

        @Override
        public int estimate(String text) {
            return perCall;
        }
    }

    /** Pauses the second batch member while the gateway monitor is held. */
    private static final class BlockingSecondEstimate implements TokenEstimator {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch secondEntered = new CountDownLatch(1);
        private final CountDownLatch releaseSecond = new CountDownLatch(1);

        @Override
        public int estimate(String text) {
            if (calls.incrementAndGet() == 2) {
                secondEntered.countDown();
                try {
                    if (!releaseSecond.await(2, TimeUnit.SECONDS)) {
                        throw new AssertionError("batch test did not release the second write");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                }
            }
            return 1;
        }
    }

    private static MemoryEntry entry(ConversationTopic topic, String content) {
        return new MemoryEntry(Instant.now(), topic, MemorySource.COMMANDER, content);
    }

    private static MemoryEntry entry(ConversationTopic topic, String content, MemoryImportance importance) {
        return new MemoryEntry(Instant.now(), topic, MemorySource.COMMANDER, content, importance);
    }

    @Test
    void shortTermKeepsEntriesBelowLimits() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));

        gateway.write(entry(ConversationTopic.NAVIGATION, "first"));
        gateway.write(entry(ConversationTopic.COMBAT, "second"));

        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(2, timeline.size());
        assertEquals("first", timeline.get(0).content());
        assertEquals("second", timeline.get(1).content());
        // Nothing evicted yet, so mid-term has no topics.
        assertTrue(midTermTopics(gateway).isEmpty());
    }

    @Test
    void batchIsInvisibleToConcurrentReadersUntilEveryEntryIsStored() throws InterruptedException {
        BlockingSecondEstimate estimator = new BlockingSecondEstimate();
        SessionMemoryGateway gateway = new SessionMemoryGateway(estimator);
        List<MemoryEntry> batch = List.of(
                entry(ConversationTopic.NAVIGATION, "question"),
                entry(ConversationTopic.NAVIGATION, "answer"));
        AtomicReference<List<MemoryEntry>> observed = new AtomicReference<>();
        CountDownLatch readerStarted = new CountDownLatch(1);
        CountDownLatch readerFinished = new CountDownLatch(1);

        Thread writer = new Thread(() -> gateway.writeBatch(batch), "memory-batch-writer-test");
        writer.start();
        assertTrue(estimator.secondEntered.await(1, TimeUnit.SECONDS));

        Thread reader = new Thread(() -> {
            readerStarted.countDown();
            observed.set(gateway.readShortTermTimeline());
            readerFinished.countDown();
        }, "memory-batch-reader-test");
        reader.start();
        assertTrue(readerStarted.await(1, TimeUnit.SECONDS));
        try {
            assertFalse(readerFinished.await(100, TimeUnit.MILLISECONDS),
                    "a reader must not observe the first half of an in-progress batch");
        } finally {
            estimator.releaseSecond.countDown();
        }
        writer.join(2000);
        reader.join(2000);

        assertFalse(writer.isAlive());
        assertFalse(reader.isAlive());
        assertEquals(List.of("question", "answer"),
                observed.get().stream().map(MemoryEntry::content).toList());
    }

    @Test
    void atomicBatchBoundsOversizedMemberWithoutDeferringPartOfTheContract() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        List<MemoryEntry> deferred = new ArrayList<>();
        gateway.setOversizedMemoryListener(deferred::add);
        String longAnswer = "x".repeat(CompanionConfig.memoryEntryMaxChars() + 50);
        ToolLink call = ToolLink.call("call-1", "query_inventory", "{}");
        ToolLink result = ToolLink.result("call-1");

        gateway.writeBatch(List.of(
                entry(ConversationTopic.SHIP_STATUS, "how many purifiers"),
                new MemoryEntry(Instant.now(), ConversationTopic.SHIP_STATUS, MemorySource.COMPANION,
                        "query_inventory", MemoryImportance.LOW, null, null, call),
                new MemoryEntry(Instant.now(), ConversationTopic.SHIP_STATUS, MemorySource.TOOL_RESULT,
                        longAnswer, MemoryImportance.LOW, null, null, result)));

        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(3, timeline.size(), "the complete query contract is stored together");
        assertTrue(deferred.isEmpty(), "a batch member cannot be deferred after the rest becomes visible");
        MemoryEntry storedResult = timeline.get(2);
        assertTrue(storedResult.content().length() <= CompanionConfig.memoryEntryMaxChars());
        assertNotNull(storedResult.toolLink());
        assertTrue(storedResult.toolLink().isResult());
        assertEquals("call-1", storedResult.toolLink().toolCallId());
    }

    @Test
    void countOverflowEvictsOldestIntoMidTermByTopic() {
        // Cost 1 per entry keeps the token budget irrelevant; only the count cap can bite.
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));

        for (int i = 0; i < CompanionConfig.shortTermMemorySize() + 3; i++) {
            ConversationTopic topic = i < 3 ? ConversationTopic.MINING : ConversationTopic.TRADE;
            gateway.write(entry(topic, "entry-" + i));
        }

        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(CompanionConfig.shortTermMemorySize(), timeline.size());
        // The three oldest (MINING) were evicted; the newest entry is still the last one written.
        assertEquals("entry-" + (CompanionConfig.shortTermMemorySize() + 2), timeline.get(timeline.size() - 1).content());

        List<ConversationTopic> topics = midTermTopics(gateway);
        assertTrue(topics.contains(ConversationTopic.MINING));
        assertFalse(topics.contains(ConversationTopic.TRADE));
    }

    @Test
    void sustainedWritesFillMidTermTopicsAndKeepTimelineBounded() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));

        // Two full timelines' worth, alternating topics, so both topics accumulate in mid-term.
        int total = CompanionConfig.shortTermMemorySize() * 2;
        for (int i = 0; i < total; i++) {
            ConversationTopic topic = (i % 2 == 0) ? ConversationTopic.NAVIGATION : ConversationTopic.COMBAT;
            gateway.write(entry(topic, "e" + i));
        }

        // Hot timeline stays capped at the newest MAX_ENTRIES.
        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(CompanionConfig.shortTermMemorySize(), timeline.size());
        assertEquals("e" + (total - 1), timeline.get(timeline.size() - 1).content());

        // Both topics filled mid-term, reported once each in enum order.
        assertEquals(
                List.of(ConversationTopic.NAVIGATION, ConversationTopic.COMBAT),
                midTermTopics(gateway));
    }

    @Test
    void tokenBudgetEvictsButAlwaysKeepsNewestEntry() {
        // One entry alone exceeds the whole budget.
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET + 1));

        gateway.write(entry(ConversationTopic.EXPLORATION, "a"));
        gateway.write(entry(ConversationTopic.EXPLORATION, "b"));

        // The token budget evicts down to the single newest entry, never to empty.
        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(1, timeline.size());
        assertEquals("b", timeline.get(0).content());
        assertTrue(midTermTopics(gateway).contains(ConversationTopic.EXPLORATION));
    }

    @Test
    void writePreservesToolLinkWhenNormalizingForStorage() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        ToolLink call = ToolLink.call("call-1", "query_carrier_voyage", "{}");

        gateway.write(new MemoryEntry(Instant.now(), ConversationTopic.NAVIGATION,
                MemorySource.COMPANION, "Query_Carrier_Voyage", MemoryImportance.LOW,
                null, null, call));

        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(1, timeline.size());
        MemoryEntry stored = timeline.get(0);
        assertEquals("query_carrier_voyage", stored.content());
        assertNotNull(stored.toolLink(), "tool-call linkage must survive lower-casing/embedding storage prep");
        assertTrue(stored.toolLink().isCall());
        assertEquals("call-1", stored.toolLink().toolCallId());
        assertEquals("query_carrier_voyage", stored.toolLink().toolName());
        assertEquals("{}", stored.toolLink().argumentsJson());
    }

    @Test
    void longTermSummaryDefaultsEmptyAndIsReplaceable() {
        SessionMemoryGateway gateway = new SessionMemoryGateway();
        assertEquals("", gateway.longTermSummary());

        gateway.replaceLongTermSummary("commander has been mining in Borann for hours");
        assertEquals("commander has been mining in Borann for hours", gateway.longTermSummary());
    }

    @Test
    void emptyMemoryRecallSkipsSemanticMatcher() {
        AtomicInteger matcherCalls = new AtomicInteger();
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1), () -> {
            matcherCalls.incrementAndGet();
            return null;
        });

        assertTrue(gateway.recallCandidates("increase speed by 10", 3).isEmpty());
        assertTrue(gateway.recallMatching("increase speed by 10", 3).isEmpty());
        assertEquals(0, matcherCalls.get(), "empty memory must not initialize or query the semantic matcher");
    }

    @Test
    void recallCandidatesReusesPreparedSemanticQuery() {
        AtomicInteger queryEmbeds = new AtomicInteger();
        AngleEmbedder vectors = new AngleEmbedder(Map.of(
                "known route", 0.0,
                "navigation clue", 5.0));
        TextEmbedder counting = new TextEmbedder() {
            @Override public float[] embed(String text) {
                if ("navigation clue".equals(text)) {
                    queryEmbeds.incrementAndGet();
                }
                return vectors.embed(text);
            }
            @Override public int dimensions() {
                return vectors.dimensions();
            }
        };
        SemanticPhraseMatcher matcher = new SemanticPhraseMatcher(counting);
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1), () -> matcher);
        gateway.write(entry(ConversationTopic.NAVIGATION, "known route"));

        SemanticQuery prepared = matcher.embedQueryContext("navigation clue");
        List<MemoryEntry> recalled = gateway.recallCandidates("navigation clue", 3, prepared);

        assertEquals(List.of("known route"), recalled.stream().map(MemoryEntry::content).toList());
        assertEquals(1, queryEmbeds.get(), "memory recall must reuse the intake query vector");
    }

    @Test
    void recallMatchingSearchesTheLongTermSummary() {
        SessionMemoryGateway gateway = new SessionMemoryGateway();
        gateway.replaceLongTermSummary("commander has been mining in Borann for hours");

        // The summary is no longer inlined into every prompt; memory_search must reach it, labelled [SYSTEM].
        assertEquals(List.of("[SYSTEM] commander has been mining in Borann for hours"),
                gateway.recallMatching("Borann", 10));
    }

    @Test
    void midTermOverflowIsHandedToTheEvictionListener() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        List<MemoryEntry> evicted = new java.util.ArrayList<>();
        gateway.setMidTermEvictionListener(evicted::add);

        // Fill short-term (kept) + mid-term to its per-topic cap + 2 more, all one topic, so 2 overflow mid-term.
        int writes = CompanionConfig.shortTermMemorySize() + CompanionConfig.midTermMemorySizePerTopic() + 2;
        for (int i = 0; i < writes; i++) {
            gateway.write(entry(ConversationTopic.MINING, "m-" + i));
        }

        assertEquals(2, evicted.size());
        // The two oldest mid-term entries overflowed first.
        assertEquals(List.of("m-0", "m-1"), evicted.stream().map(MemoryEntry::content).toList());
    }

    @Test
    void recallTopicMemoryReadsEvictedMidTermEntries() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        for (int i = 0; i < CompanionConfig.shortTermMemorySize() + 2; i++) {
            gateway.write(entry(ConversationTopic.NAVIGATION, "nav-" + i));
        }
        // The two oldest were evicted into mid-term; short-term recall does not see them, topic recall does.
        List<MemoryEntry> recalled = gateway.recallTopicMemory(ConversationTopic.NAVIGATION, null, 10);
        assertEquals(List.of("nav-0", "nav-1"), recalled.stream().map(MemoryEntry::content).toList());
    }

    @Test
    void recallMatchingFindsShortTermEntriesAndMergesWithMidTermNewestFirst() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        // Fill past the short-term cap so the oldest "borann" fact is evicted into mid-term while a fresh
        // "borann" fact stays in short-term; recall must see both, newest first.
        gateway.write(entry(ConversationTopic.MINING, "mining hotspot is borann"));
        for (int i = 0; i < CompanionConfig.shortTermMemorySize(); i++) {
            gateway.write(entry(ConversationTopic.TRADE, "filler-" + i));
        }
        gateway.write(entry(ConversationTopic.MINING, "returning to borann now"));

        List<String> recalled = gateway.recallMatching("borann", 10);
        // Both the short-term hit and the evicted mid-term hit are returned, freshest first.
        assertEquals(
                List.of("[COMMANDER] returning to borann now", "[COMMANDER] mining hotspot is borann"),
                recalled);
    }

    @Test
    void lowEntriesAreDroppedOnEvictionNotPromotedToMidTerm() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        // A LOW entry (idle banter / companion speech) and a NORMAL fact, both the oldest so both leave short-term.
        gateway.write(entry(ConversationTopic.SOCIAL, "болтовня которую не храним", MemoryImportance.LOW));
        gateway.write(entry(ConversationTopic.MINING, "цель по добыче низкотемпературные алмазы", MemoryImportance.NORMAL));
        for (int i = 0; i < CompanionConfig.shortTermMemorySize(); i++) {
            gateway.write(entry(ConversationTopic.TRADE, "filler-" + i));
        }

        // The NORMAL fact was promoted to mid-term; the LOW entry was dropped on eviction, not promoted.
        assertEquals(List.of("[COMMANDER] цель по добыче низкотемпературные алмазы"),
                gateway.recallMatching("алмазы", 10));
        assertTrue(gateway.recallMatching("болтовня", 10).isEmpty(), "a LOW entry must not reach mid-term");
    }

    @Test
    void recallMatchingRanksImportantMatchesAboveNewerRoutineOnes() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        // Same shared word ("granite") in three entries: an older MAX/HIGH fact and a newer NORMAL mention.
        // Recency alone would float the newest NORMAL to the top; importance-first must surface MAX then HIGH.
        gateway.write(entry(ConversationTopic.COMBAT, "abort word granite", MemoryImportance.MAX));
        gateway.write(entry(ConversationTopic.MINING, "granite deposits ahead", MemoryImportance.HIGH));
        gateway.write(entry(ConversationTopic.SOCIAL, "the floor is granite", MemoryImportance.NORMAL));

        List<String> recalled = gateway.recallMatching("granite", 10);

        assertEquals(
                List.of("[COMMANDER] abort word granite", "[COMMANDER] granite deposits ahead",
                        "[COMMANDER] the floor is granite"),
                recalled);
    }

    @Test
    void recallMatchingMatchesWholeWordsNotSharedStems() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        gateway.write(entry(ConversationTopic.NAVIGATION, "код стыковки сьерра девять четыре"));
        gateway.write(entry(ConversationTopic.COMBAT, "кодовое слово отход гранит"));

        // Word recall is exact now that meaning lives in the vector: the query token "код" must not fuzzily
        // match "кодовое", so the unrelated codeword fact is not dragged in by a shared stem. (Inflected and
        // paraphrased recall is the semantic vector's job, which is off in this word-only unit test.)
        assertEquals(List.of("[COMMANDER] код стыковки сьерра девять четыре"),
                gateway.recallMatching("код стыковки", 10));
    }

    @Test
    void recallMatchingSurfacesPinnedArchiveFacts() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        // A pinned MAX fact lives only in the archive (not short/mid-term); search must still find it.
        gateway.addLongTermPinned(entry(ConversationTopic.NAVIGATION, "docking code is sierra nine four", MemoryImportance.MAX));
        assertEquals(List.of("[COMMANDER] docking code is sierra nine four"),
                gateway.recallMatching("docking code", 10));
    }

    @Test
    void recallMatchingRanksStrongerOverlapAboveAWeaklyMatchingMax() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        // Pinned MAX shares one query word; a NORMAL short-term fact shares three. Relevance-first must put the
        // stronger (but lower-importance) match on top, so an accumulating archive cannot bury the relevant fact.
        gateway.addLongTermPinned(entry(ConversationTopic.COMBAT, "granite is the abort word", MemoryImportance.MAX));
        gateway.write(entry(ConversationTopic.MINING, "granite mining hotspot location", MemoryImportance.NORMAL));

        List<String> recalled = gateway.recallMatching("granite mining hotspot", 10);
        assertEquals("[COMMANDER] granite mining hotspot location", recalled.get(0));
    }

    @Test
    void recallMatchingCapsHowManyArchiveFactsEnterTheResult() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        // More equally-matching pinned MAX facts than the archive cap, plus one short-term match. The archive
        // must not flood the result: at most ARCHIVE_RECALL_LIMIT archive facts appear, room left for the rest.
        for (int i = 0; i < CompanionMemoryLimits.ARCHIVE_RECALL_LIMIT + 3; i++) {
            gateway.addLongTermPinned(entry(ConversationTopic.SOCIAL, "rendezvous point alpha " + i, MemoryImportance.MAX));
        }
        gateway.write(entry(ConversationTopic.NAVIGATION, "rendezvous point updated to beta", MemoryImportance.NORMAL));

        List<String> recalled = gateway.recallMatching("rendezvous point", 10);
        long archiveHits = recalled.stream().filter(s -> s.contains("alpha")).count();
        assertEquals(CompanionMemoryLimits.ARCHIVE_RECALL_LIMIT, archiveHits);
        assertTrue(recalled.stream().anyMatch(s -> s.contains("beta")), "the short-term match must still surface");
    }

    @Test
    void pinningTheSameFactTwiceArchivesItOnce() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        gateway.addLongTermPinned(entry(ConversationTopic.SOCIAL, "operation name is ebb", MemoryImportance.MAX));
        gateway.addLongTermPinned(entry(ConversationTopic.SOCIAL, "operation name is ebb", MemoryImportance.MAX));
        assertEquals(1, gateway.longTermPinnedFacts().size());
    }

    private static SessionMemoryGateway semanticGateway(Map<String, Double> anglesDeg) {
        SemanticPhraseMatcher matcher = new SemanticPhraseMatcher(new AngleEmbedder(anglesDeg));
        return new SessionMemoryGateway(new FixedTokenEstimator(1), () -> matcher);
    }

    @Test
    void recallMatchingFindsAMeaningMatchWithNoSharedWords() {
        // The query shares no word with the stored fact, but points the same way in meaning-space (8 degrees
        // apart, cosine ~0.99). Word-only recall would miss it; semantic recall surfaces it. The far-meaning,
        // no-shared-word distractor (90 degrees, cosine 0) stays out.
        SessionMemoryGateway gateway = semanticGateway(Map.of(
                "the beacon is lit", 0.0,
                "mining yield report", 90.0,
                "navigation light active", 8.0));
        gateway.write(entry(ConversationTopic.NAVIGATION, "the beacon is lit"));
        gateway.write(entry(ConversationTopic.MINING, "mining yield report"));

        assertEquals(List.of("[COMMANDER] the beacon is lit"),
                gateway.recallMatching("navigation light active", 10));
    }

    @Test
    void recallMatchingKeepsPureWordMatchesWhenSemanticSearchIsOn() {
        // A shared word still recalls a fact even when its meaning-vector is far (90 degrees from the query,
        // cosine 0): the word signal alone makes it eligible. An entry that matches neither by word nor by
        // meaning stays out. The two stored entries sit 50 degrees apart so de-duplication never merges them.
        SessionMemoryGateway gateway = semanticGateway(Map.of(
                "granite deposits ahead", 90.0,
                "trade route data", 40.0,
                "granite", 0.0));
        gateway.write(entry(ConversationTopic.MINING, "granite deposits ahead"));
        gateway.write(entry(ConversationTopic.TRADE, "trade route data"));

        assertEquals(List.of("[COMMANDER] granite deposits ahead"),
                gateway.recallMatching("granite", 10));
    }

    @Test
    void recallMatchingKeepsEverySemanticMatchAboveTheFloorNotJustTheClosest() {
        // Two facts both clear the meaning floor at different closeness (10 degrees ~0.985 and 29 degrees
        // ~0.875); recall must return BOTH, because a compound question needs the weaker one too. Only the
        // sub-floor distractor (55 degrees ~0.573) drops out. This guards against a relative "within a margin
        // of the best match" cut, which would discard the 29-degree fact sitting behind the 10-degree one.
        SessionMemoryGateway gateway = semanticGateway(Map.of(
                "alpha", 0.0,
                "bravo", 10.0,
                "delta", 29.0,
                "charlie", 55.0));
        gateway.write(entry(ConversationTopic.COMBAT, "bravo"));
        gateway.write(entry(ConversationTopic.MINING, "delta"));
        gateway.write(entry(ConversationTopic.TRADE, "charlie"));

        List<String> recalled = gateway.recallMatching("alpha", 10);
        assertTrue(recalled.contains("[COMMANDER] bravo"));
        assertTrue(recalled.contains("[COMMANDER] delta"));
        assertFalse(recalled.contains("[COMMANDER] charlie"));
    }

    @Test
    void writeKeepsNearIdenticalFactsInShortTermVerbatim() {
        // Short-term is no longer de-duplicated: a fact restated more strongly stays as its own entry in the
        // hot window (fact de-duplication happens in mid-term, not here). Both copies survive, in order.
        SessionMemoryGateway gateway = semanticGateway(Map.of(
                "docking code is sierra", 10.0,
                "remember docking code sierra nine four", 12.0));
        gateway.write(entry(ConversationTopic.NAVIGATION, "docking code is sierra"));
        gateway.write(entry(ConversationTopic.NAVIGATION, "remember docking code sierra nine four", MemoryImportance.MAX));

        List<MemoryEntry> timeline = gateway.readShortTermTimeline();
        assertEquals(2, timeline.size());
        assertEquals("docking code is sierra", timeline.get(0).content());
        assertEquals("remember docking code sierra nine four", timeline.get(1).content());
    }

    @Test
    void evictionCollapsesNearIdenticalCopiesIntoOneMidTermEntry() {
        // Two near-identical copies ride the verbatim hot window together; as they age out, the eviction
        // hand-off must collapse them into ONE mid-term entry keeping the most important wording, so they
        // never crowd the <facts> candidates as duplicates.
        SessionMemoryGateway gateway = semanticGateway(Map.of(
                "docking code is sierra", 10.0,
                "remember docking code sierra nine four", 12.0,
                "filler", 90.0));
        gateway.write(entry(ConversationTopic.NAVIGATION, "docking code is sierra"));
        gateway.write(entry(ConversationTopic.NAVIGATION, "remember docking code sierra nine four", MemoryImportance.MAX));
        for (int i = 0; i < CompanionConfig.shortTermMemorySize(); i++) {
            gateway.write(entry(ConversationTopic.SOCIAL, "filler")); // pushes both facts out of the window
        }

        List<MemoryEntry> nav = gateway.recallTopicMemory(ConversationTopic.NAVIGATION, null, 100);
        assertEquals(1, nav.size(), "the two near-identical copies must merge at the eviction hand-off");
        assertEquals("remember docking code sierra nine four", nav.get(0).content());
        assertEquals(MemoryImportance.MAX, nav.get(0).importance());
    }

    @Test
    void writeCollapsesAMidTermDuplicateKeepingTheMostImportantWording() {
        // A fact already evicted into mid-term is restated: the mid-term copy is removed and the survivor
        // (the more important wording, freshest mention) is stored as the one short-term copy.
        SessionMemoryGateway gateway = semanticGateway(Map.of(
                "docking code is sierra", 10.0,
                "remember docking code sierra nine four", 12.0,
                "filler", 90.0));
        gateway.write(entry(ConversationTopic.NAVIGATION, "docking code is sierra", MemoryImportance.MAX));
        for (int i = 0; i < CompanionConfig.shortTermMemorySize(); i++) {
            gateway.write(entry(ConversationTopic.SOCIAL, "filler")); // pushes the fact into mid-term
        }
        assertFalse(gateway.recallTopicMemory(ConversationTopic.NAVIGATION, null, 100).isEmpty());

        gateway.write(entry(ConversationTopic.NAVIGATION, "remember docking code sierra nine four"));

        // The mid-term copy collapsed into the surviving short-term one; the MAX wording won over NORMAL.
        assertTrue(gateway.recallTopicMemory(ConversationTopic.NAVIGATION, null, 100).isEmpty());
        List<MemoryEntry> sierra = gateway.readShortTermTimeline().stream()
                .filter(e -> e.content().contains("sierra")).toList();
        assertEquals(1, sierra.size());
        assertEquals("docking code is sierra", sierra.get(0).content());
        assertEquals(MemoryImportance.MAX, sierra.get(0).importance());
    }

    @Test
    void recallCollapsesADuplicateAcrossArchiveAndTimelineIntoTheImportantOne() {
        // A pinned MAX fact (archive) and a near-identical routine question (short-term) both match the query;
        // search de-duplication returns them as one - the important fact, not its paraphrased re-ask.
        SessionMemoryGateway gateway = semanticGateway(Map.of(
                "docking code is sierra nine four", 10.0,
                "what is the docking code", 12.0,
                "docking code", 11.0));
        gateway.addLongTermPinned(entry(ConversationTopic.NAVIGATION, "docking code is sierra nine four", MemoryImportance.MAX));
        gateway.write(entry(ConversationTopic.NAVIGATION, "what is the docking code"));

        assertEquals(List.of("[COMMANDER] docking code is sierra nine four"),
                gateway.recallMatching("docking code", 10));
    }

    @Test
    void oversizedWriteIsHandedToTheListenerAndNotStored() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        java.util.List<MemoryEntry> handed = new java.util.ArrayList<>();
        gateway.setOversizedMemoryListener(handed::add);
        String longText = "x".repeat(CompanionConfig.memoryEntryMaxChars() + 1);

        gateway.write(entry(ConversationTopic.SOCIAL, longText));

        assertEquals(1, handed.size());
        assertEquals(longText, handed.get(0).content(), "the original (uncompressed) entry is handed off");
        assertTrue(gateway.readShortTermTimeline().isEmpty(), "the over-long entry is not stored as-is");
    }

    @Test
    void writeAtTheSizeLimitIsStoredNormally() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        String atLimit = "y".repeat(CompanionConfig.memoryEntryMaxChars());

        gateway.write(entry(ConversationTopic.SOCIAL, atLimit));

        assertEquals(1, gateway.readShortTermTimeline().size(), "an entry at the limit is stored");
    }

    @Test
    void snapshotCapturesEveryAreaGroupedByTopic() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        // Push two past the short-term cap so the two oldest NAVIGATION entries evict into mid-term.
        for (int i = 0; i < CompanionConfig.shortTermMemorySize() + 2; i++) {
            gateway.write(entry(ConversationTopic.NAVIGATION, "nav-" + i));
        }
        gateway.addLongTermPinned(entry(ConversationTopic.SOCIAL, "operation name is ebb", MemoryImportance.MAX));
        gateway.replaceLongTermSummary("old jumps summarized");

        MemorySnapshot snapshot = gateway.snapshot();

        assertEquals(CompanionConfig.shortTermMemorySize(), snapshot.shortTerm().size());
        assertEquals(List.of("nav-0", "nav-1"),
                snapshot.midTermByTopic().get(ConversationTopic.NAVIGATION).stream().map(MemoryEntry::content).toList());
        assertEquals("old jumps summarized", snapshot.longTermSummary());
        assertEquals(List.of("operation name is ebb"),
                snapshot.longTermPinned().stream().map(MemoryEntry::content).toList());
    }

    @Test
    void snapshotIsDetachedFromLiveStores() {
        SessionMemoryGateway gateway = new SessionMemoryGateway(new FixedTokenEstimator(1));
        gateway.write(entry(ConversationTopic.NAVIGATION, "first"));

        MemorySnapshot snapshot = gateway.snapshot();
        gateway.write(entry(ConversationTopic.COMBAT, "second")); // a later write must not mutate the snapshot

        assertEquals(1, snapshot.shortTerm().size());
        assertEquals("first", snapshot.shortTerm().get(0).content());
    }

    @Test
    void heuristicEstimatorIsConservativeAndNonNegative() {
        TokenEstimator estimator = new HeuristicTokenEstimator();
        assertEquals(0, estimator.estimate(null));
        assertEquals(0, estimator.estimate("   "));
        // 6 chars / 3 = 2 tokens (ceiling division).
        assertEquals(2, estimator.estimate("привет"));
        // Any non-blank text costs at least one token.
        assertEquals(1, estimator.estimate("a"));
    }
}
