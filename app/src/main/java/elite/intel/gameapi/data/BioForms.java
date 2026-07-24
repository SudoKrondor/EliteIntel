package elite.intel.gameapi.data;

import java.util.*;

/**
 * Static registry of known exobiology forms, keyed by Frontier's language-independent
 * organic symbol stems (the {@code Genus}/{@code Species} codex identifiers from the journal),
 * NOT localized display names. This is what makes lookups work on every game language.
 *
 * <p>Genus stem  = journal {@code Genus}   with the {@code $Codex_Ent_..._Genus_Name;} wrapper removed
 * (e.g. {@code Tussocks}). Species stem = journal {@code Species} with the {@code $Codex_Ent_..._Name;}
 * wrapper removed (e.g. {@code Tussocks_02}). A codex {@code Name} carries an extra colour-variant
 * suffix (e.g. {@code Tussocks_02_F}); {@link #normalizeSpecies} strips it back to the species stem
 * via longest-known-prefix match.</p>
 *
 * <p>Symbol stems and colony ranges are sourced from EDCD/EDDI (CC0-compatible facts) and verified
 * against real journals; credit values, colony ranges and biome-prediction strings are the project's
 * own curated data. Callers may pass a raw journal symbol, a bare stem, or an English display name -
 * all three normalize to the same key.</p>
 */
public class BioForms {

    /**
     * Per-species economic data. Colony range is genus-uniform but repeated here for convenience.
     */
    public record BioDetails(long creditValue, long firstDiscoveryBonus, Integer colonyRange) {
    }

    public record ProjectedPayment(Long payment, Long firstDiscoveryBonus) {
    }

    private record GenusInfo(String englishName, int colonyRange, String biome) {
    }

    private record SpeciesInfo(String genusStem, String englishName, BioDetails details) {
    }

    /**
     * genus stem -> genus info
     */
    private static final Map<String, GenusInfo> GENERA = new LinkedHashMap<>();
    /**
     * species stem -> species info (species stems are globally unique)
     */
    private static final Map<String, SpeciesInfo> SPECIES = new LinkedHashMap<>();
    /**
     * genus stem -> its species stems
     */
    private static final Map<String, List<String>> GENUS_SPECIES = new LinkedHashMap<>();
    /**
     * English display name (lower-case) -> genus stem, for callers that still hold a display name
     */
    private static final Map<String, String> ENGLISH_TO_GENUS = new LinkedHashMap<>();
    /**
     * species stems, longest first, for colour-variant longest-prefix resolution
     */
    private static final List<String> SPECIES_STEMS_LONGEST_FIRST = new ArrayList<>();

    private static void genus(String stem, String english, int colonyRange, String biome) {
        GENERA.put(stem, new GenusInfo(english, colonyRange, biome));
        GENUS_SPECIES.put(stem, new ArrayList<>());
        ENGLISH_TO_GENUS.put(english.toLowerCase(Locale.ROOT), stem);
    }

    private static void species(String genusStem, String stem, String english, long creditValue, long firstDiscoveryBonus, int colonyRange) {
        SPECIES.put(stem, new SpeciesInfo(genusStem, english, new BioDetails(creditValue, firstDiscoveryBonus, colonyRange)));
        GENUS_SPECIES.get(genusStem).add(stem);
    }

    static {
        // Aleoida
        genus("Aleoids", "Aleoida", 150, "Planet:Rocky,High Metal Content|Atmosphere:CO2,CO2-Rich,Ammonia|Gravity:<=0.27|Temperature:175-195K|Volcanism:None|System:Any");
        species("Aleoids", "Aleoids_01", "Aleoida Arcus", 7252500, 29010000, 150);
        species("Aleoids", "Aleoids_02", "Aleoida Coronamus", 6284600, 25138400, 150);
        species("Aleoids", "Aleoids_05", "Aleoida Gravis", 12934900, 51739600, 150);
        species("Aleoids", "Aleoids_04", "Aleoida Laminiae", 3385200, 13540800, 150);
        species("Aleoids", "Aleoids_03", "Aleoida Spica", 3385200, 13540800, 150);

        // Amphora Plant
        genus("Vents", "Amphora Plant", 100, "Planet:Any|Atmosphere:None|Gravity:Any|Temperature:Any|Volcanism:None|System:Star Type A,Earth-Like World,Ammonia World,Gas Giant with Life,>12000Ls from star");
        species("Vents", "Vents", "Amphora Plant", 3626400, 14505600, 100);

        // Anemone
        genus("Sphere", "Anemone", 100, "Planet:Any|Atmosphere:None,Thin|Gravity:Any|Temperature:Any|Volcanism:None|System:Star Type varies by species");
        species("Sphere", "SphereEFGH", "Anemone Blatteum Bioluminescent", 1499900, 5999600, 100);
        species("Sphere", "SphereABCD_01", "Anemone Croceum", 3399800, 13599200, 100);
        species("Sphere", "Sphere", "Anemone Luteolum", 1499900, 5999600, 100);
        species("Sphere", "SphereEFGH_02", "Anemone Prasinum Bioluminescent", 1499900, 5999600, 100);
        species("Sphere", "SphereABCD_02", "Anemone Puniceum", 1499900, 5999600, 100);
        species("Sphere", "SphereABCD_03", "Anemone Roseum", 1499900, 5999600, 100);
        species("Sphere", "SphereEFGH_03", "Anemone Roseum Bioluminescent", 1499900, 5999600, 100);
        species("Sphere", "SphereEFGH_01", "Anemone Rubeum Bioluminescent", 1499900, 5999600, 100);

        // Bacterium
        genus("Bacterial", "Bacterium", 500, "Planet:Any|Atmosphere:Any|Gravity:Any|Temperature:Any|Volcanism:Any|System:Any");
        species("Bacterial", "Bacterial_02", "Bacterium Nebulus", 9116600, 36466400, 500);
        species("Bacterial", "Bacterial_04", "Bacterium Acies", 1000000, 4000000, 500);
        species("Bacterial", "Bacterial_11", "Bacterium Omentum", 4638900, 18555600, 500);
        species("Bacterial", "Bacterial_03", "Bacterium Scopulum", 8633800, 34535200, 500);
        species("Bacterial", "Bacterial_13", "Bacterium Verrata", 3897000, 15588000, 500);
        species("Bacterial", "Bacterial_10", "Bacterium Bullaris", 1152500, 4610000, 500);
        species("Bacterial", "Bacterial_05", "Bacterium Vesicula", 1000000, 4000000, 500);
        species("Bacterial", "Bacterial_08", "Bacterium Informem", 8418000, 33672000, 500);
        species("Bacterial", "Bacterial_09", "Bacterium Volu", 7774700, 31098800, 500);
        species("Bacterial", "Bacterial_06", "Bacterium Alcyoneum", 1658500, 6634000, 500);
        species("Bacterial", "Bacterial_01", "Bacterium Aurasus", 1000000, 4000000, 500);
        species("Bacterial", "Bacterial_12", "Bacterium Cerbrus", 1689800, 6759200, 500);
        species("Bacterial", "Bacterial_07", "Bacterium Tela", 1949000, 7796000, 500);

        // Bark Mound
        genus("Cone", "Bark Mound", 100, "Planet:Any|Atmosphere:None|Gravity:Any|Temperature:Any|Volcanism:None|System:Within 150Ly of nebula center");
        species("Cone", "Cone", "Bark Mound", 1471900, 5887600, 100);

        // Brain Tree
        genus("Brancae", "Brain Tree", 100, "Planet:Any|Atmosphere:Any|Gravity:Any|Temperature:100-500K|Volcanism:None|System:Earth-Like World,Gas Giant with Water-based Life,Roseum Any");
        species("Brancae", "SeedEFGH_01", "Brain Tree Aureum", 3565100, 14260400, 100);
        species("Brancae", "SeedABCD_01", "Brain Tree Gypseeum", 3565100, 14260400, 100);
        species("Brancae", "SeedEFGH_03", "Brain Tree Lindigoticum", 3565100, 14260400, 100);
        species("Brancae", "SeedEFGH", "Brain Tree Lividum", 1593700, 6374800, 100);
        species("Brancae", "SeedABCD_02", "Brain Tree Ostrinum", 3565100, 14260400, 100);
        species("Brancae", "SeedEFGH_02", "Brain Tree Puniceum", 3565100, 14260400, 100);
        species("Brancae", "Seed", "Brain Tree Roseum", 1593700, 6374800, 100);
        species("Brancae", "SeedABCD_03", "Brain Tree Viride", 1593700, 6374800, 100);

        // Cactoida
        genus("Cactoid", "Cactoida", 300, "Planet:Rocky,High Metal Content|Atmosphere:CO2,CO2-Rich,Ammonia,Water|Gravity:Any|Temperature:180-195K|Volcanism:None|System:Any");
        species("Cactoid", "Cactoid_01", "Cactoida Cortexum", 3667600, 14670400, 300);
        species("Cactoid", "Cactoid_02", "Cactoida Lapis", 2483600, 9934400, 300);
        species("Cactoid", "Cactoid_05", "Cactoida Peperatis", 2483600, 9934400, 300);
        species("Cactoid", "Cactoid_04", "Cactoida Pullulanta", 3667600, 14670400, 300);
        species("Cactoid", "Cactoid_03", "Cactoida Vermis", 16202800, 64811200, 300);

        // Clypeus
        genus("Clypeus", "Clypeus", 150, "Planet:Rocky,High Metal Content|Atmosphere:Water,CO2|Gravity:<=0.27|Temperature:>=190K|Volcanism:None|System:Any");
        species("Clypeus", "Clypeus_01", "Clypeus Lacrimam", 8418000, 33672000, 150);
        species("Clypeus", "Clypeus_02", "Clypeus Margaritus", 11873200, 47492800, 150);
        species("Clypeus", "Clypeus_03", "Clypeus Speculumi", 16202800, 64811200, 150);

        // Concha
        genus("Conchas", "Concha", 150, "Planet:Rocky,High Metal Content|Atmosphere:Ammonia,Nitrogen,CO2,CO2-Rich,Water,Water-Rich|Gravity:Any|Temperature:180-195K|Volcanism:None|System:Any");
        species("Conchas", "Conchas_02", "Concha Aureolas", 7774700, 31098800, 150);
        species("Conchas", "Conchas_04", "Concha Biconcavis", 16777215, 67108860, 150);
        species("Conchas", "Conchas_03", "Concha Labiata", 2352400, 9409600, 150);
        species("Conchas", "Conchas_01", "Concha Renibus", 4572400, 18289600, 150);

        // Crystalline Shard
        genus("Ground_Struct_Ice", "Crystalline Shard", 100, "Planet:Any|Atmosphere:None|Gravity:Any|Temperature:Any|Volcanism:None|System:Star Type A,F,G,K,M,S,Earth-Like World,Ammonia World,Gas Giant with Life,>12000Ls from star");
        species("Ground_Struct_Ice", "Ground_Struct_Ice", "Crystalline Shard", 3626400, 14505600, 100);

        // Electricae
        genus("Electricae", "Electricae", 1000, "Planet:Icy|Atmosphere:Helium,Neon,Argon|Gravity:<=0.27|Temperature:Any|Volcanism:None|System:Any");
        species("Electricae", "Electricae_01", "Electricae Pluma", 6284600, 25138400, 1000);
        species("Electricae", "Electricae_02", "Electricae Radialem", 6284600, 25138400, 1000);

        // Fonticulua
        genus("Fonticulus", "Fonticulua", 500, "Planet:Icy|Atmosphere:Argon,Argon-Rich,Methane,Oxygen,Nitrogen,Neon,Neon-Rich|Gravity:Any|Temperature:Any|Volcanism:None|System:Any");
        species("Fonticulus", "Fonticulus_02", "Fonticulua Campestris", 1000000, 4000000, 500);
        species("Fonticulus", "Fonticulus_06", "Fonticulua Digitos", 1804100, 7216400, 500);
        species("Fonticulus", "Fonticulus_05", "Fonticulua Fluctus", 16777215, 67108860, 500);
        species("Fonticulus", "Fonticulus_04", "Fonticulua Lapida", 3111000, 12444000, 500);
        species("Fonticulus", "Fonticulus_01", "Fonticulua Segmentatus", 19010800, 76043200, 500);
        species("Fonticulus", "Fonticulus_03", "Fonticulua Upupam", 5727600, 22910400, 500);

        // Frutexa
        genus("Shrubs", "Frutexa", 150, "Planet:Rocky,High Metal Content|Atmosphere:CO2,CO2-Rich,SO2,Ammonia,Water,Water-Rich|Gravity:Any|Temperature:<=195K|Volcanism:None|System:Any");
        species("Shrubs", "Shrubs_02", "Frutexa Acus", 7774700, 31098800, 150);
        species("Shrubs", "Shrubs_07", "Frutexa Collum", 1639800, 6559200, 150);
        species("Shrubs", "Shrubs_05", "Frutexa Fera", 1632500, 6530000, 150);
        species("Shrubs", "Shrubs_01", "Frutexa Flabellum", 1808900, 7235600, 150);
        species("Shrubs", "Shrubs_04", "Frutexa Flammasis", 10326000, 41304000, 150);
        species("Shrubs", "Shrubs_03", "Frutexa Metallicum", 1632500, 6530000, 150);
        species("Shrubs", "Shrubs_06", "Frutexa Sponsae", 5988000, 23952000, 150);

        // Fumerola
        genus("Fumerolas", "Fumerola", 100, "Planet:Any|Atmosphere:Any|Gravity:Any|Temperature:Any|Volcanism:Water,Methane,CO2,Nitrogen,Ammonia,Silicate,Iron,Rocky|System:Any");
        species("Fumerolas", "Fumerolas_04", "Fumerola Aquatis", 6284600, 25138400, 100);
        species("Fumerolas", "Fumerolas_01", "Fumerola Carbosis", 6284600, 25138400, 100);
        species("Fumerolas", "Fumerolas_02", "Fumerola Extremus", 16202800, 64811200, 100);
        species("Fumerolas", "Fumerolas_03", "Fumerola Nitris", 7500900, 30003600, 100);

        // Fungoida
        genus("Fungoids", "Fungoida", 300, "Planet:Rocky,High Metal Content|Atmosphere:Argon,Argon-Rich,CO2,CO2-Rich,Water,Ammonia,Methane,Methane-Rich|Gravity:Any|Temperature:180-195K|Volcanism:None|System:Any");
        species("Fungoids", "Fungoids_03", "Fungoida Bullarum", 3703200, 14812800, 300);
        species("Fungoids", "Fungoids_04", "Fungoida Gelata", 3330300, 13321200, 300);
        species("Fungoids", "Fungoids_01", "Fungoida Setisis", 1670100, 6680400, 300);
        species("Fungoids", "Fungoids_02", "Fungoida Stabitis", 2680300, 10721200, 300);

        // Osseus
        genus("Osseus", "Osseus", 800, "Planet:Rocky,High Metal Content|Atmosphere:CO2,CO2-Rich,Water,Water-Rich,Ammonia,Methane,Methane-Rich,Argon,Argon-Rich,Nitrogen|Gravity:Any|Temperature:180-195K|Volcanism:None|System:Any");
        species("Osseus", "Osseus_05", "Osseus Cornibus", 1483000, 5932000, 800);
        species("Osseus", "Osseus_02", "Osseus Discus", 12934900, 51739600, 800);
        species("Osseus", "Osseus_01", "Osseus Fractus", 4027800, 16111200, 800);
        species("Osseus", "Osseus_06", "Osseus Pellebantus", 9739000, 38956000, 800);
        species("Osseus", "Osseus_04", "Osseus Pumice", 3156300, 12625200, 800);
        species("Osseus", "Osseus_03", "Osseus Spiralis", 2404700, 9618800, 800);

        // Recepta
        genus("Recepta", "Recepta", 150, "Planet:Any|Atmosphere:SO2|Gravity:<=0.27|Temperature:Any|Volcanism:None|System:Any");
        species("Recepta", "Recepta_03", "Recepta Conditivus", 14313700, 57254800, 150);
        species("Recepta", "Recepta_02", "Recepta Deltahedronix", 16202800, 64811200, 150);
        species("Recepta", "Recepta_01", "Recepta Umbrux", 12934900, 51739600, 150);

        // Sinuous Tuber
        genus("Tubers", "Sinuous Tuber", 100, "Planet:Any|Atmosphere:None|Gravity:Any|Temperature:Any|Volcanism:Any|System:Galactic Core Preferred");
        species("Tubers", "TubeABCD_02", "Sinuous Tuber Albidum", 3425600, 13702400, 100);
        species("Tubers", "TubeEFGH", "Sinuous Tuber Blatteum", 1514500, 6058000, 100);
        species("Tubers", "TubeABCD_03", "Sinuous Tuber Caeruleum", 1514500, 6058000, 100);
        species("Tubers", "TubeEFGH_01", "Sinuous Tuber Lindigoticum", 1514500, 6058000, 100);
        species("Tubers", "TubeABCD_01", "Sinuous Tuber Prasinum", 1514500, 6058000, 100);
        species("Tubers", "Tube", "Sinuous Tuber Roseus", 1514500, 6058000, 100);
        species("Tubers", "TubeEFGH_02", "Sinuous Tuber Violaceum", 1514500, 6058000, 100);
        species("Tubers", "TubeEFGH_03", "Sinuous Tuber Viride", 1514500, 6058000, 100);

        // Stratum
        genus("Stratum", "Stratum", 500, "Planet:Rocky,High Metal Content|Atmosphere:SO2,CO2,CO2-Rich,Ammonia,Water,Water-Rich|Gravity:Any|Temperature:>=165K|Volcanism:None|System:Any");
        species("Stratum", "Stratum_04", "Stratum Araneamus", 2448900, 9795600, 500);
        species("Stratum", "Stratum_06", "Stratum Cucumisis", 16777215, 67108860, 500);
        species("Stratum", "Stratum_01", "Stratum Excutitus", 2448900, 9795600, 500);
        species("Stratum", "Stratum_08", "Stratum Frigus", 2637500, 10550000, 500);
        species("Stratum", "Stratum_03", "Stratum Laminamus", 2788300, 11153200, 500);
        species("Stratum", "Stratum_05", "Stratum Limaxus", 1362000, 5448000, 500);
        species("Stratum", "Stratum_02", "Stratum Paleas", 1362000, 5448000, 500);
        species("Stratum", "Stratum_07", "Stratum Tectonicas", 19010800, 76043200, 500);

        // Tubus
        genus("Tubus", "Tubus", 800, "Planet:Rocky,High Metal Content|Atmosphere:CO2,CO2-Rich,Ammonia|Gravity:Any|Temperature:160-190K|Volcanism:None|System:Any");
        species("Tubus", "Tubus_03", "Tubus Cavas", 11873200, 47492800, 800);
        species("Tubus", "Tubus_05", "Tubus Compagibus", 7774700, 31098800, 800);
        species("Tubus", "Tubus_01", "Tubus Conifer", 2415500, 9662000, 800);
        species("Tubus", "Tubus_04", "Tubus Rosarium", 2637500, 10550000, 800);
        species("Tubus", "Tubus_02", "Tubus Sororibus", 5727600, 22910400, 800);

        // Tussock
        genus("Tussocks", "Tussock", 200, "Planet:Rocky|Atmosphere:CO2,CO2-Rich,Methane,Methane-Rich,Argon,Argon-Rich,Ammonia,Water,Water-Rich,SO2|Gravity:Any|Temperature:145-195K|Volcanism:None|System:Any");
        species("Tussocks", "Tussocks_08", "Tussock Albata", 3252500, 13010000, 200);
        species("Tussocks", "Tussocks_15", "Tussock Capillum", 7025800, 28103200, 200);
        species("Tussocks", "Tussocks_11", "Tussock Caputus", 3472400, 13889600, 200);
        species("Tussocks", "Tussocks_05", "Tussock Catena", 1766600, 7066400, 200);
        species("Tussocks", "Tussocks_04", "Tussock Cultro", 1766600, 7066400, 200);
        species("Tussocks", "Tussocks_10", "Tussock Divisa", 1766600, 7066400, 200);
        species("Tussocks", "Tussocks_03", "Tussock Ignis", 1849000, 7396000, 200);
        species("Tussocks", "Tussocks_01", "Tussock Pennata", 5853800, 23415200, 200);
        species("Tussocks", "Tussocks_06", "Tussock Pennatis", 1000000, 4000000, 200);
        species("Tussocks", "Tussocks_09", "Tussock Propagito", 1000000, 4000000, 200);
        species("Tussocks", "Tussocks_07", "Tussock Serrati", 4447100, 17788400, 200);
        species("Tussocks", "Tussocks_13", "Tussock Stigmasis", 19010800, 76043200, 200);
        species("Tussocks", "Tussocks_12", "Tussock Triticum", 7774700, 31098800, 200);
        species("Tussocks", "Tussocks_02", "Tussock Ventusa", 3277700, 13110800, 200);
        species("Tussocks", "Tussocks_14", "Tussock Virgam", 14313700, 57254800, 200);

        SPECIES_STEMS_LONGEST_FIRST.addAll(SPECIES.keySet());
        SPECIES_STEMS_LONGEST_FIRST.sort((a, b) -> Integer.compare(b.length(), a.length()));
    }


    /**
     * Normalize a genus reference to its symbol stem. Accepts a raw journal symbol
     * ({@code $Codex_Ent_Tussocks_Genus_Name;}), a bare stem ({@code Tussocks}) or an
     * English display name ({@code Tussock}). Returns {@code null} only for null input.
     */
    public static String normalizeGenus(String genus) {
        if (genus == null) return null;
        String s = genus.trim();
        if (s.startsWith("$Codex_Ent_")) {
            s = s.substring("$Codex_Ent_".length());
            if (s.endsWith("_Genus_Name;")) s = s.substring(0, s.length() - "_Genus_Name;".length());
            else if (s.endsWith("_Name;")) s = s.substring(0, s.length() - "_Name;".length());
            else if (s.endsWith(";")) s = s.substring(0, s.length() - 1);
            return s;
        }
        if (GENERA.containsKey(s)) return s;
        String byEnglish = ENGLISH_TO_GENUS.get(s.toLowerCase(Locale.ROOT));
        return byEnglish != null ? byEnglish : s;
    }

    /**
     * Normalize a species/variant reference to its species symbol stem. Accepts a raw
     * {@code Species} symbol ({@code $Codex_Ent_Tussocks_02_Name;}), a variant-level codex
     * {@code Name} ({@code $Codex_Ent_Tussocks_02_F_Name;} - the colour suffix is stripped by
     * longest-known-prefix match), or a bare stem.
     */
    public static String normalizeSpecies(String species) {
        if (species == null) return null;
        String s = species.trim();
        if (s.startsWith("$Codex_Ent_")) {
            s = s.substring("$Codex_Ent_".length());
            if (s.endsWith("_Name;")) s = s.substring(0, s.length() - "_Name;".length());
            else if (s.endsWith(";")) s = s.substring(0, s.length() - 1);
        }
        if (SPECIES.containsKey(s)) return s;
        // colour-variant suffix (e.g. Tussocks_02_F): strip back to the known species stem
        for (String stem : SPECIES_STEMS_LONGEST_FIRST) {
            if (s.startsWith(stem) && (s.length() == stem.length() || s.charAt(stem.length()) == '_')) {
                return stem;
            }
        }
        return s;
    }

    /**
     * Genus symbol stem that owns the given species reference, or null if unknown.
     */
    public static String genusStemForSpecies(String species) {
        SpeciesInfo info = SPECIES.get(normalizeSpecies(species));
        return info != null ? info.genusStem() : null;
    }

    /**
     * English display name for a genus reference (for the LLM / English speech), or null.
     */
    public static String englishGenusName(String genus) {
        GenusInfo info = GENERA.get(normalizeGenus(genus));
        return info != null ? info.englishName() : null;
    }

    /**
     * Per-species economic data (species symbol stems are globally unique), or null if unknown.
     */
    public static BioDetails getDetails(String species) {
        SpeciesInfo info = SPECIES.get(normalizeSpecies(species));
        return info != null ? info.details() : null;
    }

    /** Genus-level biome-prediction string, or null. */
    public static String getBiomeDescription(String genus) {
        GenusInfo info = GENERA.get(normalizeGenus(genus));
        return info != null ? info.biome() : null;
    }

    public static ProjectedPayment getProjectedPayment(String species) {
        SpeciesInfo info = SPECIES.get(normalizeSpecies(species));
        if (info == null) return null;
        return new ProjectedPayment(info.details().creditValue(), info.details().firstDiscoveryBonus());
    }

    public static ProjectedPayment getAverageProjectedPayment(String genus) {
        String stem = normalizeGenus(genus);
        List<String> speciesStems = GENUS_SPECIES.get(stem);
        if (speciesStems == null || speciesStems.isEmpty()) return null;
        long creditValue = 0;
        long firstDiscovery = 0;
        for (String s : speciesStems) {
            BioDetails d = SPECIES.get(s).details();
            creditValue += d.creditValue();
            firstDiscovery += d.firstDiscoveryBonus();
        }
        int n = speciesStems.size();
        return new ProjectedPayment(creditValue / n, firstDiscovery / n);
    }

    /**
     * Genus sample colony range in metres, or {@code null} when the genus is unknown (caller omits the value).
     */
    public static Integer colonyRangeOrNull(String genus) {
        GenusInfo info = GENERA.get(normalizeGenus(genus));
        return info != null ? info.colonyRange() : null;
    }

    /**
     * Genus sample colony range in metres, or 0 if unknown.
     */
    public static int getDistance(String genus) {
        Integer range = colonyRangeOrNull(genus);
        return range != null ? range : 0;
    }

    /**
     * Genus biome-prediction map keyed by ENGLISH display name (not symbol), for the
     * Biome Analysis LLM which reasons and outputs in English.
     */
    public static Map<String, String> getGenusToBiome() {
        Map<String, String> out = new LinkedHashMap<>();
        for (GenusInfo info : GENERA.values()) {
            out.put(info.englishName(), info.biome());
        }
        return out;
    }
}
