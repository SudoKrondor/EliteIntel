package elite.intel.junit.db.managers;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Partial bio samples are the one thing on a body that nothing can re-derive: the game reports a
 * sample stage once and never again, so a write that loses one loses it permanently.
 * <p>
 * They were being lost two ways. A sample that opens a set arrives in the same journal instant as
 * the CodexEntry for that species, and both handlers used to load the body, change it, and save the
 * whole row - so the later save silently dropped the sample. And completing a set cleared every
 * partial on the body, not just the finished genus's.
 */
class LocationBioSampleWriteTest {

    private static final AtomicInteger RUN = new AtomicInteger();

    private final LocationManager locations = LocationManager.getInstance();

    private long systemAddress;
    private long bodyId;
    private String star;
    private String planet;

    @BeforeEach
    void setUp() {
        int run = RUN.incrementAndGet();
        star = String.format("Bio Write Test %03d", run);
        systemAddress = 771167680000L + run;
        bodyId = 26;
        planet = star + " 1 a";

        LocationDto body = new LocationDto(bodyId, systemAddress);
        body.setStarName(star);
        body.setPlanetName(planet);
        locations.save(body);
    }

    /**
     * The concurrent case, forced: several writers touching one body at once, one of them recording
     * a sample. Every change has to survive, whichever order they land in.
     */
    @Test
    void aSampleIsNotLostWhenAnotherHandlerWritesTheSameBodyAtTheSameInstant() throws Exception {
        int writers = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(writers);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < writers; i++) {
            int index = i;
            threads.add(Thread.ofVirtual().start(() -> {
                try {
                    start.await();
                    locations.updateBody(systemAddress, bodyId, body -> {
                        body.setStarName(star);
                        body.addBioScan(partial("Genus" + index, "$Codex_Ent_Genus" + index + "_Name;", 1));
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }));
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "writers did not finish");

        LocationDto stored = locations.findBySystemAddress(systemAddress, bodyId);
        assertEquals(writers, stored.getPartialBioSamples().size(), "a concurrent write dropped a sample");
    }

    @Test
    void completingOneGenusLeavesAnotherGenusPartialsAlone() {
        locations.updateBody(systemAddress, bodyId, body -> {
            body.addBioScan(partial("Concha", "$Codex_Ent_Conchas_Name;", 1));
            body.addBioScan(partial("Concha", "$Codex_Ent_Conchas_Name;", 2));
            body.addBioScan(partial("Tussock", "$Codex_Ent_Tussocks_Name;", 1));
        });

        locations.updateBody(systemAddress, bodyId,
                body -> body.deletePartialBioSamplesFor("$Codex_Ent_Conchas_Name;", "Concha"));

        LocationDto stored = locations.findBySystemAddress(systemAddress, bodyId);
        assertEquals(List.of("Tussock"), stored.getPartialBioSamples().stream().map(BioSampleDto::getGenus).toList());
    }

    /**
     * Samples recorded before genus symbols were captured carry only the localised name.
     */
    @Test
    void aLegacyPartialWithNoSymbolIsStillDeletedByName() {
        locations.updateBody(systemAddress, bodyId, body -> body.addBioScan(partial("Concha", null, 1)));

        locations.updateBody(systemAddress, bodyId,
                body -> body.deletePartialBioSamplesFor("$Codex_Ent_Conchas_Name;", "Concha"));

        assertTrue(locations.findBySystemAddress(systemAddress, bodyId).getPartialBioSamples().isEmpty());
    }

    private BioSampleDto partial(String genus, String genusSymbol, int scanXof3) {
        BioSampleDto sample = new BioSampleDto();
        sample.setPrimaryStar(star);
        sample.setPlanetName(planet);
        sample.setBodyId(bodyId);
        sample.setGenus(genus);
        sample.setGenusSymbol(genusSymbol);
        sample.setScanXof3(scanXof3);
        return sample;
    }
}
