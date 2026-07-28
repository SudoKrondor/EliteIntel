-- V1.1 pre-release database work, in one migration.
--
-- Two separate jobs land together here because they were developed as one change set and none of
-- it had been applied yet:
--
--   1. Schema cleanup  - drops the legacy columns and tables that were deliberately kept while
--                        V1.0 and V1.1 testers shared a single database file.
--   2. Localization    - wires up the commodity and subsystem name columns that existed (or were
--                        missing) but had never been populated or read.
--
-- Order matters: columns are added before they are filled, so the sections below must stay in
-- sequence.
--
-- ============================================================================================
-- SECTION 1 - Schema cleanup
-- ============================================================================================

-- V1.1 is now the release, so the additive-only rule that governed 01000-01017 no longer applies.
--
-- Every column dropped here was verified to have no read path in application code. Where a
-- column was superseded rather than simply abandoned, the successor is named below.
--
-- NOTE: a column is only dropped when nothing reads it. Columns that are written but never read
-- back (trade_tuple's price/supply fields) and columns that mirror a documented Frontier contract
-- (player_status) are deliberately kept.

-- === game_session ===================================================================
-- SystemSession is the only gateway to this table; none of these are read there.

-- Superseded by per-ship personality (ship.personality, read via SystemSession#getAIPersonality).
ALTER TABLE game_session
    DROP COLUMN aiPersonality;

-- Superseded by the per-provider voice columns added in 01011 (kokoroVoice, googleVoice).
-- 01011's own comment slated these two for exactly this cleanup.
ALTER TABLE game_session
    DROP COLUMN aiVoice;
ALTER TABLE game_session
    DROP COLUMN aiCadence;

-- Plaintext key columns, superseded by the Cypher-encrypted encryptedLLMKey/encryptedTTSKey.
-- Never read since encryption was introduced.
ALTER TABLE game_session
    DROP COLUMN aiApiKey;
ALTER TABLE game_session
    DROP COLUMN ttsApiKey;
ALTER TABLE game_session
    DROP COLUMN sttApiKey;

-- EDSM's API is called unauthenticated, so neither the plaintext nor the encrypted EDSM key
-- column ever had a reader.
ALTER TABLE game_session
    DROP COLUMN edsmApiKey;
ALTER TABLE game_session
    DROP COLUMN encryptedEDSSMKey;

ALTER TABLE game_session
    DROP COLUMN loggingEnabled;

-- Superseded by the provider-specific columns (ollamaAddress/ollamaCommandModel,
-- lmStudioAddress/lmStudioCommandModel) once local LLM support grew past a single provider.
ALTER TABLE game_session
    DROP COLUMN localLlmCommandModel;
ALTER TABLE game_session
    DROP COLUMN localLlmQueryModel;
ALTER TABLE game_session
    DROP COLUMN localLlmAddress;

-- One local model now serves both the command and query roles. These were write-only on V1.1,
-- kept solely so a V1.0 client sharing this file still found its dual-model settings.
ALTER TABLE game_session
    DROP COLUMN ollamaQueryModel;
ALTER TABLE game_session
    DROP COLUMN lmStudioQueryModel;

-- Conversation mode was a legacy-brain flag; that pipeline is gone and the reader is hardcoded off.
ALTER TABLE game_session
    DROP COLUMN conversationModeOn;

-- Companion mode is unconditionally on; the toggle no longer exists.
ALTER TABLE game_session
    DROP COLUMN companionModeOn;

-- === player =========================================================================

ALTER TABLE player
    DROP COLUMN player_title;
ALTER TABLE player
    DROP COLUMN player_mission_statement;
ALTER TABLE player
    DROP COLUMN jumping_to_star_system;
ALTER TABLE player
    DROP COLUMN logging_enabled;

-- Superseded by the game_session TTS/LLM settings; never read from the player row.
ALTER TABLE player
    DROP COLUMN localTtsServer;
ALTER TABLE player
    DROP COLUMN localLlmAddress;

-- Added in 00052 and never wired to anything: absent from PlayerDao's INSERT as well as its mapper.
ALTER TABLE player
    DROP COLUMN useVm;

-- === ship ===========================================================================

-- Speech cadence was folded into the ship personality; ShipDao never mapped this column.
ALTER TABLE ship
    DROP COLUMN cadence;

-- === orphaned tables ================================================================

-- Retired by 01017, which moved on-hand amounts onto the symbol-keyed material_names table and
-- left `materials` orphaned rather than dropping it (V1.0 still read it at the time). No SQL in
-- the codebase references it any more.
DROP TABLE IF EXISTS materials;

-- A single-column key store that predates the file-based bindings system in AppPaths/BindingsLoader.
-- No DAO and no SQL anywhere reference it.
DROP TABLE IF EXISTS bindings;


-- ============================================================================================
-- SECTION 2 - Portuguese commodity columns
-- ============================================================================================

-- Portuguese commodity name columns, closing the gap where PT and PTBZ were the only supported
-- languages with no commodity_* column of their own and so fell through to English.
--
-- PTBZ matters most: Brazilian Portuguese is one of the six languages Frontier actually localizes
-- (see Language#isGameLocalized), so a pt-BR commander's HUD shows Portuguese commodity names while
-- the app was speaking and matching English ones.
--
-- PT (European Portuguese) is not a Frontier locale, so those commanders run an English client.
-- The column is added anyway for consistency with commodity_uk/commodity_it, which localize
-- commodities for non-Frontier locales too.
--
-- Both columns start NULL. That is safe and is a deliberate no-op until translations land:
-- CommodityDao's lookup and fuzzy-match queries wrap the locale column in
-- COALESCE(<col>, commodity), so a NULL row resolves to the English name exactly as today.
-- Rows can therefore be filled in incrementally without an intermediate broken state.

ALTER TABLE commodities
    ADD COLUMN commodity_pt TEXT;

ALTER TABLE commodities
    ADD COLUMN commodity_ptbz TEXT;


-- ============================================================================================
-- SECTION 3 - Subsystem label columns
-- ============================================================================================

-- Completes the sub_system label columns so subsystem targeting can resolve a spoken name in any
-- supported language, not just the four locales 01000 happened to ship.
--
-- 01000 added label_es/fr/pt/ru but nothing ever read them: FuzzySearch#fuzzySubSystemSearch
-- matched against the English `subsystem` column only, so a commander asking for a subsystem in
-- their own language never resolved and targeting silently refused to start. This migration adds
-- the remaining supported locales; FuzzySearch now selects the column for the active language.
--
-- An unpopulated column is safe: the lookups wrap the label in COALESCE(<col>, subsystem), so an
-- untranslated row stays reachable under its English name. Translated rows are covered separately
-- by FuzzySearch retrying the English candidate list when the localized pass misses, so a commander
-- can use either their own language or the English term regardless of which locale is active.

ALTER TABLE sub_system
    ADD COLUMN label_de TEXT;

ALTER TABLE sub_system
    ADD COLUMN label_it TEXT;

ALTER TABLE sub_system
    ADD COLUMN label_uk TEXT;

-- Brazilian Portuguese is a Frontier locale (a pt-BR client shows translated module names), so it
-- needs its own column rather than sharing PT's. The existing label_pt values are in fact Brazilian
-- ("Contramedida Eletrônica", not the European "Eletrónica"); Section 4 copies them across.
ALTER TABLE sub_system
    ADD COLUMN label_ptbz TEXT;


-- ============================================================================================
-- SECTION 4 - Subsystem labels: copy Brazilian values into the PTBZ column
-- ============================================================================================

-- Moves the subsystem module labels into the locale they actually belong to.
--
-- 01000 loaded Portuguese module names into label_pt, but the strings are Brazilian, not European:
-- "Contramedida Eletrônica" uses the Brazilian circumflex where European Portuguese writes
-- "Eletrónica". That matches the upstream source, since Frontier's only Portuguese client locale is
-- pt-BR -- so the data was always pt-BR sitting under the PT column.
--
-- PTBZ is the locale that needs it. A pt-BR client shows translated module names on the HUD, so a
-- Brazilian commander speaks and hears those names; PT commanders run an English client.
--
-- Copied rather than moved: label_pt keeps its values, so European Portuguese still resolves
-- Portuguese module names instead of regressing to English-only.

UPDATE sub_system
SET label_ptbz = label_pt
WHERE label_pt IS NOT NULL
  AND TRIM(label_pt) <> ''
  AND label_ptbz IS NULL;


-- ============================================================================================
-- SECTION 5 - Commodity name translations (Brazilian Portuguese, plus gap fills)
-- ============================================================================================

-- Brazilian Portuguese commodity names, closing the gap where PT and PTBZ had no commodity_*
-- column and fell through to English (Section 2 added the columns; this fills them).
--
-- Source: jixxed/ed-odyssey-materials-helper,
-- application/src/main/resources/locale/material/horizons/commodity.csv
-- -- the same upstream this database already uses for the sub_system module labels (01000) and
-- the Italian material names (01013). Its de/es/fr/ru values match the rows already stored here,
-- which is what confirms the shared provenance.
--
-- The file's `pt` column is Brazilian, not European: it writes "Tratamento Agronômico" and
-- "Sistemas aquapônicos" with the Brazilian circumflex and contains no European -ónico spellings.
-- That is expected, because pt-BR is Frontier's only Portuguese client locale. Both commodity_pt
-- and commodity_ptbz are set from it, matching how Section 4 handles the sub_system labels: PTBZ is
-- the locale that needs it, and PT keeps a Portuguese name rather than regressing to English.
--
-- Rows are matched on `symbol` where present, and on the English name for the 47 legacy/Powerplay
-- goods that carry no symbol. 416 of 446 rows are covered; the rest have no upstream entry (three
-- are category headers -- Chemicals, Legal Drugs, Weapons -- not tradable goods) or a blank `pt`.


UPDATE commodities
SET commodity_pt   = 'Catalizadores avançados',
    commodity_ptbz = 'Catalizadores avançados'
WHERE LOWER(symbol) = LOWER('AdvancedCatalysers');

UPDATE commodities
SET commodity_pt   = 'Remédios avançados',
    commodity_ptbz = 'Remédios avançados'
WHERE LOWER(symbol) = LOWER('AdvancedMedicines');

UPDATE commodities
SET commodity_pt   = 'Tratamento Agronômico',
    commodity_ptbz = 'Tratamento Agronômico'
WHERE LOWER(symbol) = LOWER('AgronomicTreatment');

UPDATE commodities
SET commodity_pt   = 'Relíquias de IA',
    commodity_ptbz = 'Relíquias de IA'
WHERE LOWER(symbol) = LOWER('AiRelics');

UPDATE commodities
SET commodity_pt   = 'Alexandrita',
    commodity_ptbz = 'Alexandrita'
WHERE LOWER(symbol) = LOWER('Alexandrite');

UPDATE commodities
SET commodity_pt   = 'Alumínio',
    commodity_ptbz = 'Alumínio'
WHERE LOWER(symbol) = LOWER('Aluminium');

UPDATE commodities
SET commodity_pt   = 'Carne animal',
    commodity_ptbz = 'Carne animal'
WHERE LOWER(symbol) = LOWER('Animalmeat');

UPDATE commodities
SET commodity_pt   = 'Monitores animais',
    commodity_ptbz = 'Monitores animais'
WHERE LOWER(symbol) = LOWER('AnimalMonitors');

UPDATE commodities
SET commodity_pt   = 'Unidade de contenção de antimatéria',
    commodity_ptbz = 'Unidade de contenção de antimatéria'
WHERE LOWER(symbol) = LOWER('AntimatterContainmentUnit');

UPDATE commodities
SET commodity_pt   = 'Joias antigas',
    commodity_ptbz = 'Joias antigas'
WHERE LOWER(symbol) = LOWER('AntiqueJewellery');

UPDATE commodities
SET commodity_pt   = 'Antiguidades',
    commodity_ptbz = 'Antiguidades'
WHERE LOWER(symbol) = LOWER('Antiquities');

UPDATE commodities
SET commodity_pt   = 'Motores de articulações',
    commodity_ptbz = 'Motores de articulações'
WHERE LOWER(symbol) = LOWER('ArticulationMotors');

UPDATE commodities
SET commodity_pt   = 'Planos de assalto',
    commodity_ptbz = 'Planos de assalto'
WHERE LOWER(symbol) = LOWER('AssaultPlans');

UPDATE commodities
SET commodity_pt   = 'Processadores atmosféricos',
    commodity_ptbz = 'Processadores atmosféricos'
WHERE LOWER(symbol) = LOWER('AtmosphericExtractors');

UPDATE commodities
SET commodity_pt   = 'Auto-fabricantes',
    commodity_ptbz = 'Auto-fabricantes'
WHERE LOWER(symbol) = LOWER('AutoFabricators');

UPDATE commodities
SET commodity_pt   = 'Remédios básicos',
    commodity_ptbz = 'Remédios básicos'
WHERE LOWER(symbol) = LOWER('BasicMedicines');

UPDATE commodities
SET commodity_pt   = 'Bauxita',
    commodity_ptbz = 'Bauxita'
WHERE LOWER(symbol) = LOWER('Bauxite');

UPDATE commodities
SET commodity_pt   = 'Armas de batalha',
    commodity_ptbz = 'Armas de batalha'
WHERE LOWER(symbol) = LOWER('BattleWeapons');

UPDATE commodities
SET commodity_pt   = 'Cerveja',
    commodity_ptbz = 'Cerveja'
WHERE LOWER(symbol) = LOWER('Beer');

UPDATE commodities
SET commodity_pt   = 'Benitoíta',
    commodity_ptbz = 'Benitoíta'
WHERE LOWER(symbol) = LOWER('Benitoite');

UPDATE commodities
SET commodity_pt   = 'Berílio',
    commodity_ptbz = 'Berílio'
WHERE LOWER(symbol) = LOWER('Beryllium');

UPDATE commodities
SET commodity_pt   = 'Líquens biorredutores',
    commodity_ptbz = 'Líquens biorredutores'
WHERE LOWER(symbol) = LOWER('BioReducingLichen');

UPDATE commodities
SET commodity_pt   = 'Bismuto',
    commodity_ptbz = 'Bismuto'
WHERE LOWER(symbol) = LOWER('Bismuth');

UPDATE commodities
SET commodity_pt   = 'Caixa preta',
    commodity_ptbz = 'Caixa preta'
WHERE LOWER(symbol) = LOWER('USSCargoBlackBox');

UPDATE commodities
SET commodity_pt   = 'Bebida alcoólica ilícita',
    commodity_ptbz = 'Bebida alcoólica ilícita'
WHERE LOWER(symbol) = LOWER('BootlegLiquor');

UPDATE commodities
SET commodity_pt   = 'Fabricantes de construções',
    commodity_ptbz = 'Fabricantes de construções'
WHERE LOWER(symbol) = LOWER('BuildingFabricators');

UPDATE commodities
SET commodity_pt   = 'Compostos cerâmicos',
    commodity_ptbz = 'Compostos cerâmicos'
WHERE LOWER(symbol) = LOWER('CeramicComposites');

UPDATE commodities
SET commodity_pt   = 'Resíduo químico',
    commodity_ptbz = 'Resíduo químico'
WHERE LOWER(symbol) = LOWER('ChemicalWaste');

UPDATE commodities
SET commodity_pt   = 'Vestuário',
    commodity_ptbz = 'Vestuário'
WHERE LOWER(symbol) = LOWER('Clothing');

UPDATE commodities
SET commodity_pt   = 'Compostos CMM',
    commodity_ptbz = 'Compostos CMM'
WHERE LOWER(symbol) = LOWER('CMMComposite');

UPDATE commodities
SET commodity_pt   = 'Cobalto',
    commodity_ptbz = 'Cobalto'
WHERE LOWER(symbol) = LOWER('Cobalt');

UPDATE commodities
SET commodity_pt   = 'Café',
    commodity_ptbz = 'Café'
WHERE LOWER(symbol) = LOWER('Coffee');

UPDATE commodities
SET commodity_pt   = 'Coltano',
    commodity_ptbz = 'Coltano'
WHERE LOWER(symbol) = LOWER('Coltan');

UPDATE commodities
SET commodity_pt   = 'Estabilizadores de combate',
    commodity_ptbz = 'Estabilizadores de combate'
WHERE LOWER(symbol) = LOWER('CombatStabilisers');

UPDATE commodities
SET commodity_pt   = 'Amostras comerciais',
    commodity_ptbz = 'Amostras comerciais'
WHERE LOWER(symbol) = LOWER('ComercialSamples');

UPDATE commodities
SET commodity_pt   = 'Componentes informáticos',
    commodity_ptbz = 'Componentes informáticos'
WHERE LOWER(symbol) = LOWER('ComputerComponents');

UPDATE commodities
SET commodity_pt   = 'Tecidos condutores',
    commodity_ptbz = 'Tecidos condutores'
WHERE LOWER(symbol) = LOWER('ConductiveFabrics');

UPDATE commodities
SET commodity_pt   = 'Tecnologia de consumo',
    commodity_ptbz = 'Tecnologia de consumo'
WHERE LOWER(symbol) = LOWER('ConsumerTechnology');

UPDATE commodities
SET commodity_pt   = 'Colheitadeiras',
    commodity_ptbz = 'Colheitadeiras'
WHERE LOWER(symbol) = LOWER('CropHarvesters');

UPDATE commodities
SET commodity_pt   = 'Criolita',
    commodity_ptbz = 'Criolita'
WHERE LOWER(symbol) = LOWER('Cryolite');

UPDATE commodities
SET commodity_pt   = 'Cápsula de escape danificada',
    commodity_ptbz = 'Cápsula de escape danificada'
WHERE LOWER(symbol) = LOWER('DamagedEscapePod');

UPDATE commodities
SET commodity_pt   = 'Núcleo de dados',
    commodity_ptbz = 'Núcleo de dados'
WHERE LOWER(symbol) = LOWER('DataCore');

UPDATE commodities
SET commodity_pt   = 'Maleta diplomática',
    commodity_ptbz = 'Maleta diplomática'
WHERE LOWER(symbol) = LOWER('DiplomaticBag');

UPDATE commodities
SET commodity_pt   = 'Eletrodomésticos',
    commodity_ptbz = 'Eletrodomésticos'
WHERE LOWER(symbol) = LOWER('DomesticAppliances');

UPDATE commodities
SET commodity_pt   = 'Relíquias terrestres',
    commodity_ptbz = 'Relíquias terrestres'
WHERE LOWER(symbol) = LOWER('EarthRelics');

UPDATE commodities
SET commodity_pt   = 'Baterias de emergência',
    commodity_ptbz = 'Baterias de emergência'
WHERE LOWER(symbol) = LOWER('EmergencyPowerCells');

UPDATE commodities
SET commodity_pt   = 'Correspondência criptografada',
    commodity_ptbz = 'Correspondência criptografada'
WHERE LOWER(symbol) = LOWER('EncryptedCorrespondence');

UPDATE commodities
SET commodity_pt   = 'Depósito de dados criptografados',
    commodity_ptbz = 'Depósito de dados criptografados'
WHERE LOWER(symbol) = LOWER('EncriptedDataStorage');

UPDATE commodities
SET commodity_pt   = 'Montagem de rede de energia',
    commodity_ptbz = 'Montagem de rede de energia'
WHERE LOWER(symbol) = LOWER('PowerGridAssembly');

UPDATE commodities
SET commodity_pt   = 'Tubo exaustor',
    commodity_ptbz = 'Tubo exaustor'
WHERE LOWER(symbol) = LOWER('ExhaustManifold');

UPDATE commodities
SET commodity_pt   = 'Abrigo de evacuação',
    commodity_ptbz = 'Abrigo de evacuação'
WHERE LOWER(symbol) = LOWER('EvacuationShelter');

UPDATE commodities
SET commodity_pt   = 'Explosivos',
    commodity_ptbz = 'Explosivos'
WHERE LOWER(symbol) = LOWER('Explosives');

UPDATE commodities
SET commodity_pt   = 'Peixe',
    commodity_ptbz = 'Peixe'
WHERE LOWER(symbol) = LOWER('Fish');

UPDATE commodities
SET commodity_pt   = 'Cartuchos de alimentos',
    commodity_ptbz = 'Cartuchos de alimentos'
WHERE LOWER(symbol) = LOWER('FoodCartridges');

UPDATE commodities
SET commodity_pt   = 'Vestígios de fósseis',
    commodity_ptbz = 'Vestígios de fósseis'
WHERE LOWER(symbol) = LOWER('FossilRemnants');

UPDATE commodities
SET commodity_pt   = 'Frutas e verduras',
    commodity_ptbz = 'Frutas e verduras'
WHERE LOWER(symbol) = LOWER('FruitAndVegetables');

UPDATE commodities
SET commodity_pt   = 'Gálio',
    commodity_ptbz = 'Gálio'
WHERE LOWER(symbol) = LOWER('Gallium');

UPDATE commodities
SET commodity_pt   = 'Galita',
    commodity_ptbz = 'Galita'
WHERE LOWER(symbol) = LOWER('Gallite');

UPDATE commodities
SET commodity_pt   = 'Banco de genes',
    commodity_ptbz = 'Banco de genes'
WHERE LOWER(symbol) = LOWER('GeneBank');

UPDATE commodities
SET commodity_pt   = 'Equipamento geológico',
    commodity_ptbz = 'Equipamento geológico'
WHERE LOWER(symbol) = LOWER('GeologicalEquipment');

UPDATE commodities
SET commodity_pt   = 'Amostras geológicas',
    commodity_ptbz = 'Amostras geológicas'
WHERE LOWER(symbol) = LOWER('GeologicalSamples');

UPDATE commodities
SET commodity_pt   = 'Ouro',
    commodity_ptbz = 'Ouro'
WHERE LOWER(symbol) = LOWER('Gold');

UPDATE commodities
SET commodity_pt   = 'Cereal',
    commodity_ptbz = 'Cereal'
WHERE LOWER(symbol) = LOWER('Grain');

UPDATE commodities
SET commodity_pt   = 'Grandidierita',
    commodity_ptbz = 'Grandidierita'
WHERE LOWER(symbol) = LOWER('Grandidierite');

UPDATE commodities
SET commodity_pt   = 'Caixão Guardian',
    commodity_ptbz = 'Caixão Guardian'
WHERE LOWER(symbol) = LOWER('AncientCasket');

UPDATE commodities
SET commodity_pt   = 'Orbe Guardian',
    commodity_ptbz = 'Orbe Guardian'
WHERE LOWER(symbol) = LOWER('AncientOrb');

UPDATE commodities
SET commodity_pt   = 'Relíquia Guardian',
    commodity_ptbz = 'Relíquia Guardian'
WHERE LOWER(symbol) = LOWER('AncientRelic');

UPDATE commodities
SET commodity_pt   = 'Tábua Guardian',
    commodity_ptbz = 'Tábua Guardian'
WHERE LOWER(symbol) = LOWER('AncientTablet');

UPDATE commodities
SET commodity_pt   = 'Totem Guardian',
    commodity_ptbz = 'Totem Guardian'
WHERE LOWER(symbol) = LOWER('AncientTotem');

UPDATE commodities
SET commodity_pt   = 'Urna Guardian',
    commodity_ptbz = 'Urna Guardian'
WHERE LOWER(symbol) = LOWER('AncientUrn');

UPDATE commodities
SET commodity_pt   = 'Traje espacial',
    commodity_ptbz = 'Traje espacial'
WHERE LOWER(symbol) = LOWER('HazardousEnvironmentSuits');

UPDATE commodities
SET commodity_pt   = 'Montagem de choque HN',
    commodity_ptbz = 'Montagem de choque HN'
WHERE LOWER(symbol) = LOWER('HNShockMount');

UPDATE commodities
SET commodity_pt   = 'Háfnio 178',
    commodity_ptbz = 'Háfnio 178'
WHERE LOWER(symbol) = LOWER('Hafnium178');

UPDATE commodities
SET commodity_pt   = 'Sensor de diagnóstico de equipamentos',
    commodity_ptbz = 'Sensor de diagnóstico de equipamentos'
WHERE LOWER(symbol) = LOWER('DiagnosticSensor');

UPDATE commodities
SET commodity_pt   = 'Dissipador térmico entrelaçado',
    commodity_ptbz = 'Dissipador térmico entrelaçado'
WHERE LOWER(symbol) = LOWER('HeatsinkInterlink');

UPDATE commodities
SET commodity_pt   = 'Reféns',
    commodity_ptbz = 'Reféns'
WHERE LOWER(symbol) = LOWER('Hostage');

UPDATE commodities
SET commodity_pt   = 'Combustível de hidrogênio',
    commodity_ptbz = 'Combustível de hidrogênio'
WHERE LOWER(symbol) = LOWER('HydrogenFuel');

UPDATE commodities
SET commodity_pt   = 'Peróxido de hidrogênio',
    commodity_ptbz = 'Peróxido de hidrogênio'
WHERE LOWER(symbol) = LOWER('HydrogenPeroxide');

UPDATE commodities
SET commodity_pt   = 'Escravos imperiais',
    commodity_ptbz = 'Escravos imperiais'
WHERE LOWER(symbol) = LOWER('ImperialSlaves');

UPDATE commodities
SET commodity_pt   = 'Indio',
    commodity_ptbz = 'Indio'
WHERE LOWER(symbol) = LOWER('Indium');

UPDATE commodities
SET commodity_pt   = 'Indita',
    commodity_ptbz = 'Indita'
WHERE LOWER(symbol) = LOWER('Indite');

UPDATE commodities
SET commodity_pt   = 'Membrana de isolamento',
    commodity_ptbz = 'Membrana de isolamento'
WHERE LOWER(symbol) = LOWER('InsulatingMembrane');

UPDATE commodities
SET commodity_pt   = 'Distribuidor de íon',
    commodity_ptbz = 'Distribuidor de íon'
WHERE LOWER(symbol) = LOWER('IonDistributor');

UPDATE commodities
SET commodity_pt   = 'Jadeíta',
    commodity_ptbz = 'Jadeíta'
WHERE LOWER(symbol) = LOWER('Jadeite');

UPDATE commodities
SET commodity_pt   = 'Enriquecedor de solo',
    commodity_ptbz = 'Enriquecedor de solo'
WHERE LOWER(symbol) = LOWER('TerrainEnrichmentSystems');

UPDATE commodities
SET commodity_pt   = 'Lantânio',
    commodity_ptbz = 'Lantânio'
WHERE LOWER(symbol) = LOWER('Lanthanum');

UPDATE commodities
SET commodity_pt   = 'Grande memória de dados de exploração',
    commodity_ptbz = 'Grande memória de dados de exploração'
WHERE LOWER(symbol) = LOWER('LargeExplorationDataCash');

UPDATE commodities
SET commodity_pt   = 'Couro',
    commodity_ptbz = 'Couro'
WHERE LOWER(symbol) = LOWER('Leather');

UPDATE commodities
SET commodity_pt   = 'Lepidolita',
    commodity_ptbz = 'Lepidolita'
WHERE LOWER(symbol) = LOWER('Lepidolite');

UPDATE commodities
SET commodity_pt   = 'Oxigênio liquido',
    commodity_ptbz = 'Oxigênio liquido'
WHERE LOWER(symbol) = LOWER('LiquidOxygen');

UPDATE commodities
SET commodity_pt   = 'Bebida alcoólica',
    commodity_ptbz = 'Bebida alcoólica'
WHERE LOWER(symbol) = LOWER('Liquor');

UPDATE commodities
SET commodity_pt   = 'Lítio',
    commodity_ptbz = 'Lítio'
WHERE LOWER(symbol) = LOWER('Lithium');

UPDATE commodities
SET commodity_pt   = 'Hidróxido de Lítio',
    commodity_ptbz = 'Hidróxido de Lítio'
WHERE LOWER(symbol) = LOWER('LithiumHydroxide');

UPDATE commodities
SET commodity_pt   = 'Diamantes Gélidos',
    commodity_ptbz = 'Diamantes Gélidos'
WHERE LOWER(symbol) = LOWER('LowTemperatureDiamond');

UPDATE commodities
SET commodity_pt   = 'Bobina de emissão magnética',
    commodity_ptbz = 'Bobina de emissão magnética'
WHERE LOWER(symbol) = LOWER('MagneticEmitterCoil');

UPDATE commodities
SET commodity_pt   = 'Equipamento marítimo',
    commodity_ptbz = 'Equipamento marítimo'
WHERE LOWER(symbol) = LOWER('MarineSupplies');

UPDATE commodities
SET commodity_pt   = 'Metano em clatrato',
    commodity_ptbz = 'Metano em clatrato'
WHERE LOWER(symbol) = LOWER('MethaneClathrate');

UPDATE commodities
SET commodity_pt   = 'Cristais de metanol monohidratado',
    commodity_ptbz = 'Cristais de metanol monohidratado'
WHERE LOWER(symbol) = LOWER('MethanolMonohydrateCrystals');

UPDATE commodities
SET commodity_pt   = 'Meta-ligas',
    commodity_ptbz = 'Meta-ligas'
WHERE LOWER(symbol) = LOWER('MetaAlloys');

UPDATE commodities
SET commodity_pt   = 'Microcontroladores',
    commodity_ptbz = 'Microcontroladores'
WHERE LOWER(symbol) = LOWER('MicroControllers');

UPDATE commodities
SET commodity_pt   = 'Fornalhas microbianas',
    commodity_ptbz = 'Fornalhas microbianas'
WHERE LOWER(symbol) = LOWER('HeliostaticFurnaces');

UPDATE commodities
SET commodity_pt   = 'Mangueiras de resfriamento por micro-ondas',
    commodity_ptbz = 'Mangueiras de resfriamento por micro-ondas'
WHERE LOWER(symbol) = LOWER('CoolingHoses');

UPDATE commodities
SET commodity_pt   = 'Inteligência militar',
    commodity_ptbz = 'Inteligência militar'
WHERE LOWER(symbol) = LOWER('MilitaryIntelligence');

UPDATE commodities
SET commodity_pt   = 'Planos militares',
    commodity_ptbz = 'Planos militares'
WHERE LOWER(symbol) = LOWER('USSCargoMilitaryPlans');

UPDATE commodities
SET commodity_pt   = 'Extratores minerais',
    commodity_ptbz = 'Extratores minerais'
WHERE LOWER(symbol) = LOWER('MineralExtractors');

UPDATE commodities
SET commodity_pt   = 'Óleo Mineral',
    commodity_ptbz = 'Óleo Mineral'
WHERE LOWER(symbol) = LOWER('MineralOil');

UPDATE commodities
SET commodity_pt   = 'Moissanita',
    commodity_ptbz = 'Moissanita'
WHERE LOWER(symbol) = LOWER('Moissanite');

UPDATE commodities
SET commodity_pt   = 'Terminais modulares',
    commodity_ptbz = 'Terminais modulares'
WHERE LOWER(symbol) = LOWER('ModularTerminals');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido cerebral de molusco',
    commodity_ptbz = 'Amostra de tecido cerebral de molusco'
WHERE LOWER(symbol) = LOWER('M_TissueSample_Nerves');

UPDATE commodities
SET commodity_pt   = 'Fluido de molusco',
    commodity_ptbz = 'Fluido de molusco'
WHERE LOWER(symbol) = LOWER('M_TissueSample_Fluid');

UPDATE commodities
SET commodity_pt   = 'Membrana de molusco',
    commodity_ptbz = 'Membrana de molusco'
WHERE LOWER(symbol) = LOWER('M3_TissueSample_Membrane');

UPDATE commodities
SET commodity_pt   = 'Micélio de molusco',
    commodity_ptbz = 'Micélio de molusco'
WHERE LOWER(symbol) = LOWER('M3_TissueSample_Mycelium');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido mole de molusco',
    commodity_ptbz = 'Amostra de tecido mole de molusco'
WHERE LOWER(symbol) = LOWER('M_TissueSample_Soft');

UPDATE commodities
SET commodity_pt   = 'Esporas de molusco',
    commodity_ptbz = 'Esporas de molusco'
WHERE LOWER(symbol) = LOWER('M3_TissueSample_Spores');

UPDATE commodities
SET commodity_pt   = 'Monazita',
    commodity_ptbz = 'Monazita'
WHERE LOWER(symbol) = LOWER('Monazite');

UPDATE commodities
SET commodity_pt   = 'Musgravita',
    commodity_ptbz = 'Musgravita'
WHERE LOWER(symbol) = LOWER('Musgravite');

UPDATE commodities
SET commodity_pt   = 'Nanodisjuntores',
    commodity_ptbz = 'Nanodisjuntores'
WHERE LOWER(symbol) = LOWER('Nanobreakers');

UPDATE commodities
SET commodity_pt   = 'Narcóticos',
    commodity_ptbz = 'Narcóticos'
WHERE LOWER(symbol) = LOWER('BasicNarcotics');

UPDATE commodities
SET commodity_pt   = 'Tecidos naturais',
    commodity_ptbz = 'Tecidos naturais'
WHERE LOWER(symbol) = LOWER('NaturalFabrics');

UPDATE commodities
SET commodity_pt   = 'Neotecido de isolamento',
    commodity_ptbz = 'Neotecido de isolamento'
WHERE LOWER(symbol) = LOWER('NeofabricInsulation');

UPDATE commodities
SET commodity_pt   = 'Agentes neurais',
    commodity_ptbz = 'Agentes neurais'
WHERE LOWER(symbol) = LOWER('NerveAgents');

UPDATE commodities
SET commodity_pt   = 'Armas não letais',
    commodity_ptbz = 'Armas não letais'
WHERE LOWER(symbol) = LOWER('NonLethalWeapons');

UPDATE commodities
SET commodity_pt   = 'Cepa gama de ceionhead',
    commodity_ptbz = 'Cepa gama de ceionhead'
WHERE LOWER(symbol) = LOWER('OnionHeadC');

UPDATE commodities
SET commodity_pt   = 'Ósmio',
    commodity_ptbz = 'Ósmio'
WHERE LOWER(symbol) = LOWER('Osmium');

UPDATE commodities
SET commodity_pt   = 'Painita',
    commodity_ptbz = 'Painita'
WHERE LOWER(symbol) = LOWER('Painite');

UPDATE commodities
SET commodity_pt   = 'Paládio',
    commodity_ptbz = 'Paládio'
WHERE LOWER(symbol) = LOWER('Palladium');

UPDATE commodities
SET commodity_pt   = 'Potenciadores de desempenho',
    commodity_ptbz = 'Potenciadores de desempenho'
WHERE LOWER(symbol) = LOWER('PerformanceEnhancers');

UPDATE commodities
SET commodity_pt   = 'Itens pessoais',
    commodity_ptbz = 'Itens pessoais'
WHERE LOWER(symbol) = LOWER('PersonalEffects');

UPDATE commodities
SET commodity_pt   = 'Armas pessoais',
    commodity_ptbz = 'Armas pessoais'
WHERE LOWER(symbol) = LOWER('PersonalWeapons');

UPDATE commodities
SET commodity_pt   = 'Pesticidas',
    commodity_ptbz = 'Pesticidas'
WHERE LOWER(symbol) = LOWER('Pesticides');

UPDATE commodities
SET commodity_pt   = 'Platina',
    commodity_ptbz = 'Platina'
WHERE LOWER(symbol) = LOWER('Platinum');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido nucleico de vagem',
    commodity_ptbz = 'Amostra de tecido nucleico de vagem'
WHERE LOWER(symbol) = LOWER('S_TissueSample_Cells');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido morto de vagem',
    commodity_ptbz = 'Amostra de tecido morto de vagem'
WHERE LOWER(symbol) = LOWER('S_TissueSample_Surface');

UPDATE commodities
SET commodity_pt   = 'Mesogleia de vagem',
    commodity_ptbz = 'Mesogleia de vagem'
WHERE LOWER(symbol) = LOWER('S6_TissueSample_Mesoglea');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido externo de vagem',
    commodity_ptbz = 'Amostra de tecido externo de vagem'
WHERE LOWER(symbol) = LOWER('S6_TissueSample_Cells');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido da couraça de vagem',
    commodity_ptbz = 'Amostra de tecido da couraça de vagem'
WHERE LOWER(symbol) = LOWER('S6_TissueSample_Coenosarc');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido superficial de vagem',
    commodity_ptbz = 'Amostra de tecido superficial de vagem'
WHERE LOWER(symbol) = LOWER('S_TissueSample_Core');

UPDATE commodities
SET commodity_pt   = 'Tecido de vagem',
    commodity_ptbz = 'Tecido de vagem'
WHERE LOWER(symbol) = LOWER('S9_TissueSample_Shell');

UPDATE commodities
SET commodity_pt   = 'Prisioneiros políticos',
    commodity_ptbz = 'Prisioneiros políticos'
WHERE LOWER(symbol) = LOWER('PoliticalPrisoner');

UPDATE commodities
SET commodity_pt   = 'Polímeros',
    commodity_ptbz = 'Polímeros'
WHERE LOWER(symbol) = LOWER('Polymers');

UPDATE commodities
SET commodity_pt   = 'Conversor de energia',
    commodity_ptbz = 'Conversor de energia'
WHERE LOWER(symbol) = LOWER('PowerConverter');

UPDATE commodities
SET commodity_pt   = 'Geradores de energia',
    commodity_ptbz = 'Geradores de energia'
WHERE LOWER(symbol) = LOWER('PowerGenerators');

UPDATE commodities
SET commodity_pt   = 'Dutos de transferência de energia',
    commodity_ptbz = 'Dutos de transferência de energia'
WHERE LOWER(symbol) = LOWER('PowerTransferConduits');

UPDATE commodities
SET commodity_pt   = 'Praseodímio',
    commodity_ptbz = 'Praseodímio'
WHERE LOWER(symbol) = LOWER('Praseodymium');

UPDATE commodities
SET commodity_pt   = 'Pedras preciosas',
    commodity_ptbz = 'Pedras preciosas'
WHERE LOWER(symbol) = LOWER('PreciousGems');

UPDATE commodities
SET commodity_pt   = 'Células progenitoras',
    commodity_ptbz = 'Células progenitoras'
WHERE LOWER(symbol) = LOWER('ProgenitorCells');

UPDATE commodities
SET commodity_pt   = 'Materiais de pesquisa proibidos',
    commodity_ptbz = 'Materiais de pesquisa proibidos'
WHERE LOWER(symbol) = LOWER('ProhibitedResearchMaterials');

UPDATE commodities
SET commodity_pt   = 'Protótipo tecnológico',
    commodity_ptbz = 'Protótipo tecnológico'
WHERE LOWER(symbol) = LOWER('USSCargoPrototypeTech');

UPDATE commodities
SET commodity_pt   = 'Pirofilita',
    commodity_ptbz = 'Pirofilita'
WHERE LOWER(symbol) = LOWER('Pyrophyllite');

UPDATE commodities
SET commodity_pt   = 'Defletor de radiação',
    commodity_ptbz = 'Defletor de radiação'
WHERE LOWER(symbol) = LOWER('RadiationBaffle');

UPDATE commodities
SET commodity_pt   = 'Arte rara',
    commodity_ptbz = 'Arte rara'
WHERE LOWER(symbol) = LOWER('USSCargoRareArtwork');

UPDATE commodities
SET commodity_pt   = 'Blindagem reativa',
    commodity_ptbz = 'Blindagem reativa'
WHERE LOWER(symbol) = LOWER('ReactiveArmour');

UPDATE commodities
SET commodity_pt   = 'Transmissões rebeldes',
    commodity_ptbz = 'Transmissões rebeldes'
WHERE LOWER(symbol) = LOWER('USSCargoRebelTransmissions');

UPDATE commodities
SET commodity_pt   = 'Placa de montagem reforçada',
    commodity_ptbz = 'Placa de montagem reforçada'
WHERE LOWER(symbol) = LOWER('ReinforcedMountingPlate');

UPDATE commodities
SET commodity_pt   = 'Separadores ressonantes',
    commodity_ptbz = 'Separadores ressonantes'
WHERE LOWER(symbol) = LOWER('ResonatingSeparators');

UPDATE commodities
SET commodity_pt   = 'Rhodplumsita',
    commodity_ptbz = 'Rhodplumsita'
WHERE LOWER(symbol) = LOWER('Rhodplumsite');

UPDATE commodities
SET commodity_pt   = 'Robótica',
    commodity_ptbz = 'Robótica'
WHERE LOWER(symbol) = LOWER('Robotics');

UPDATE commodities
SET commodity_pt   = 'Fertilizante Rockforth',
    commodity_ptbz = 'Fertilizante Rockforth'
WHERE LOWER(symbol) = LOWER('RockforthFertiliser');

UPDATE commodities
SET commodity_pt   = 'Rutilo',
    commodity_ptbz = 'Rutilo'
WHERE LOWER(symbol) = LOWER('Rutile');

UPDATE commodities
SET commodity_pt   = 'Samário',
    commodity_ptbz = 'Samário'
WHERE LOWER(symbol) = LOWER('Samarium');

UPDATE commodities
SET commodity_pt   = 'Sucata',
    commodity_ptbz = 'Sucata'
WHERE LOWER(symbol) = LOWER('Scrap');

UPDATE commodities
SET commodity_pt   = 'Semicondutores',
    commodity_ptbz = 'Semicondutores'
WHERE LOWER(symbol) = LOWER('Semiconductors');

UPDATE commodities
SET commodity_pt   = 'Serendibita',
    commodity_ptbz = 'Serendibita'
WHERE LOWER(symbol) = LOWER('Serendibite');

UPDATE commodities
SET commodity_pt   = 'Prata',
    commodity_ptbz = 'Prata'
WHERE LOWER(symbol) = LOWER('Silver');

UPDATE commodities
SET commodity_pt   = 'Escravos',
    commodity_ptbz = 'Escravos'
WHERE LOWER(symbol) = LOWER('Slaves');

UPDATE commodities
SET commodity_pt   = 'Pequena memória de dados de exploração',
    commodity_ptbz = 'Pequena memória de dados de exploração'
WHERE LOWER(symbol) = LOWER('SmallExplorationDataCash');

UPDATE commodities
SET commodity_pt   = 'Relíquias de pioneiros espaciais',
    commodity_ptbz = 'Relíquias de pioneiros espaciais'
WHERE LOWER(symbol) = LOWER('SpacePioneerRelics');

UPDATE commodities
SET commodity_pt   = 'Registradores estruturais',
    commodity_ptbz = 'Registradores estruturais'
WHERE LOWER(symbol) = LOWER('StructuralRegulators');

UPDATE commodities
SET commodity_pt   = 'Supercondutores',
    commodity_ptbz = 'Supercondutores'
WHERE LOWER(symbol) = LOWER('Superconductors');

UPDATE commodities
SET commodity_pt   = 'Estabilizadores de superfície',
    commodity_ptbz = 'Estabilizadores de superfície'
WHERE LOWER(symbol) = LOWER('SurfaceStabilisers');

UPDATE commodities
SET commodity_pt   = 'Equipamento de sobrevivência',
    commodity_ptbz = 'Equipamento de sobrevivência'
WHERE LOWER(symbol) = LOWER('SurvivalEquipment');

UPDATE commodities
SET commodity_pt   = 'Tecidos sintéticos',
    commodity_ptbz = 'Tecidos sintéticos'
WHERE LOWER(symbol) = LOWER('SyntheticFabrics');

UPDATE commodities
SET commodity_pt   = 'Carne sintética',
    commodity_ptbz = 'Carne sintética'
WHERE LOWER(symbol) = LOWER('SyntheticMeat');

UPDATE commodities
SET commodity_pt   = 'Reagentes sintéticos',
    commodity_ptbz = 'Reagentes sintéticos'
WHERE LOWER(symbol) = LOWER('SyntheticReagents');

UPDATE commodities
SET commodity_pt   = 'Dados táticos',
    commodity_ptbz = 'Dados táticos'
WHERE LOWER(symbol) = LOWER('TacticalData');

UPDATE commodities
SET commodity_pt   = 'Taaffeita',
    commodity_ptbz = 'Taaffeita'
WHERE LOWER(symbol) = LOWER('Taaffeite');

UPDATE commodities
SET commodity_pt   = 'Tântalo',
    commodity_ptbz = 'Tântalo'
WHERE LOWER(symbol) = LOWER('Tantalum');

UPDATE commodities
SET commodity_pt   = 'Chá',
    commodity_ptbz = 'Chá'
WHERE LOWER(symbol) = LOWER('Tea');

UPDATE commodities
SET commodity_pt   = 'Diagramas técnicos',
    commodity_ptbz = 'Diagramas técnicos'
WHERE LOWER(symbol) = LOWER('USSCargoTechnicalBlueprints');

UPDATE commodities
SET commodity_pt   = 'Conjunto de telemetria',
    commodity_ptbz = 'Conjunto de telemetria'
WHERE LOWER(symbol) = LOWER('TelemetrySuite');

UPDATE commodities
SET commodity_pt   = 'Unidades de resfriamento térmico',
    commodity_ptbz = 'Unidades de resfriamento térmico'
WHERE LOWER(symbol) = LOWER('ThermalCoolingUnits');

UPDATE commodities
SET commodity_pt   = 'Tálio',
    commodity_ptbz = 'Tálio'
WHERE LOWER(symbol) = LOWER('Thallium');

UPDATE commodities
SET commodity_pt   = 'Amostra de Tecido Thargoid Basilisk',
    commodity_ptbz = 'Amostra de Tecido Thargoid Basilisk'
WHERE LOWER(symbol) = LOWER('ThargoidTissueSampleType2');

UPDATE commodities
SET commodity_pt   = 'Matéria Biológica Thargoid',
    commodity_ptbz = 'Matéria Biológica Thargoid'
WHERE LOWER(symbol) = LOWER('UnknownBiologicalMatter');

UPDATE commodities
SET commodity_pt   = 'Amostra de Tecido Thargoid Cyclops',
    commodity_ptbz = 'Amostra de Tecido Thargoid Cyclops'
WHERE LOWER(symbol) = LOWER('ThargoidTissueSampleType1');

UPDATE commodities
SET commodity_pt   = 'Coração Thargoid',
    commodity_ptbz = 'Coração Thargoid'
WHERE LOWER(symbol) = LOWER('ThargoidHeart');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido da Thargoid Hydra',
    commodity_ptbz = 'Amostra de tecido da Thargoid Hydra'
WHERE LOWER(symbol) = LOWER('ThargoidTissueSampleType4');

UPDATE commodities
SET commodity_pt   = 'Link Thargoid',
    commodity_ptbz = 'Link Thargoid'
WHERE LOWER(symbol) = LOWER('UnknownArtifact3');

UPDATE commodities
SET commodity_pt   = 'Amostra de Tecido Thargoid Medusa',
    commodity_ptbz = 'Amostra de Tecido Thargoid Medusa'
WHERE LOWER(symbol) = LOWER('ThargoidTissueSampleType3');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido da Thargoid Orthrus',
    commodity_ptbz = 'Amostra de tecido da Thargoid Orthrus'
WHERE LOWER(symbol) = LOWER('ThargoidTissueSampleType5');

UPDATE commodities
SET commodity_pt   = 'Sonda Thargoid',
    commodity_ptbz = 'Sonda Thargoid'
WHERE LOWER(symbol) = LOWER('UnknownArtifact2');

UPDATE commodities
SET commodity_pt   = 'Resina Thargoid',
    commodity_ptbz = 'Resina Thargoid'
WHERE LOWER(symbol) = LOWER('UnknownResin');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido de Batedor Thargoid',
    commodity_ptbz = 'Amostra de tecido de Batedor Thargoid'
WHERE LOWER(symbol) = LOWER('ThargoidScoutTissueSample');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido de Scythe Thargoid',
    commodity_ptbz = 'Amostra de tecido de Scythe Thargoid'
WHERE LOWER(symbol) = LOWER('ThargoidTissueSampleType7');

UPDATE commodities
SET commodity_pt   = 'Sensor Thargoid',
    commodity_ptbz = 'Sensor Thargoid'
WHERE LOWER(symbol) = LOWER('UnknownArtifact');

UPDATE commodities
SET commodity_pt   = 'Amostras de Tecnologia Thargoid',
    commodity_ptbz = 'Amostras de Tecnologia Thargoid'
WHERE LOWER(symbol) = LOWER('UnknownTechnologySamples');

UPDATE commodities
SET commodity_pt   = 'Tório',
    commodity_ptbz = 'Tório'
WHERE LOWER(symbol) = LOWER('Thorium');

UPDATE commodities
SET commodity_pt   = 'Cápsula do tempo',
    commodity_ptbz = 'Cápsula do tempo'
WHERE LOWER(symbol) = LOWER('TimeCapsule');

UPDATE commodities
SET commodity_pt   = 'Tabaco',
    commodity_ptbz = 'Tabaco'
WHERE LOWER(symbol) = LOWER('Tobacco');

UPDATE commodities
SET commodity_pt   = 'Resíduo tóxico',
    commodity_ptbz = 'Resíduo tóxico'
WHERE LOWER(symbol) = LOWER('ToxicWaste');

UPDATE commodities
SET commodity_pt   = 'Dados comerciais',
    commodity_ptbz = 'Dados comerciais'
WHERE LOWER(symbol) = LOWER('USSCargoTradeData');

UPDATE commodities
SET commodity_pt   = 'Bugigangas da sorte oculta',
    commodity_ptbz = 'Bugigangas da sorte oculta'
WHERE LOWER(symbol) = LOWER('TrinketsOfFortune');

UPDATE commodities
SET commodity_pt   = 'Trítio',
    commodity_ptbz = 'Trítio'
WHERE LOWER(symbol) = LOWER('Tritium');

UPDATE commodities
SET commodity_pt   = 'Relíquia não classificada',
    commodity_ptbz = 'Relíquia não classificada'
WHERE LOWER(symbol) = LOWER('AncientRelicTG');

UPDATE commodities
SET commodity_pt   = 'Pods de fuga desocupada',
    commodity_ptbz = 'Pods de fuga desocupada'
WHERE LOWER(symbol) = LOWER('UnocuppiedEscapePod');

UPDATE commodities
SET commodity_pt   = 'Núcleo de dados instável',
    commodity_ptbz = 'Núcleo de dados instável'
WHERE LOWER(symbol) = LOWER('UnstableDataCore');

UPDATE commodities
SET commodity_pt   = 'Urânio',
    commodity_ptbz = 'Urânio'
WHERE LOWER(symbol) = LOWER('Uranium');

UPDATE commodities
SET commodity_pt   = 'Opala do Vazio',
    commodity_ptbz = 'Opala do Vazio'
WHERE LOWER(symbol) = LOWER('Opal');

UPDATE commodities
SET commodity_pt   = 'Água',
    commodity_ptbz = 'Água'
WHERE LOWER(symbol) = LOWER('Water');

UPDATE commodities
SET commodity_pt   = 'Vinho',
    commodity_ptbz = 'Vinho'
WHERE LOWER(symbol) = LOWER('Wine');

UPDATE commodities
SET commodity_pt   = 'Protótipos de processadores ultracompactos',
    commodity_ptbz = 'Protótipos de processadores ultracompactos'
WHERE LOWER(symbol) = LOWER('Advert1');

UPDATE commodities
SET commodity_pt   = 'Maçãs do Éden de Aerial',
    commodity_ptbz = 'Maçãs do Éden de Aerial'
WHERE LOWER(symbol) = LOWER('AerialEdenApple');

UPDATE commodities
SET commodity_pt   = 'Estimulante de Aganippe',
    commodity_ptbz = 'Estimulante de Aganippe'
WHERE LOWER(symbol) = LOWER('AganippeRush');

UPDATE commodities
SET commodity_pt   = 'Remédios agrícolas',
    commodity_ptbz = 'Remédios agrícolas'
WHERE LOWER(symbol) = LOWER('AgriculturalMedicines');

UPDATE commodities
SET commodity_pt   = 'Material midiático da Aisling',
    commodity_ptbz = 'Material midiático da Aisling'
WHERE LOWER(commodity) = LOWER('Aisling Media Materials');

UPDATE commodities
SET commodity_pt   = 'Contratos selados da Aisling',
    commodity_ptbz = 'Contratos selados da Aisling'
WHERE LOWER(commodity) = LOWER('Aisling Sealed Contracts');

UPDATE commodities
SET commodity_pt   = 'Materiais do programa da Aisling',
    commodity_ptbz = 'Materiais do programa da Aisling'
WHERE LOWER(commodity) = LOWER('Aisling Programme Materials');

UPDATE commodities
SET commodity_pt   = 'Tatuagens de Alacarakmo',
    commodity_ptbz = 'Tatuagens de Alacarakmo'
WHERE LOWER(symbol) = LOWER('AlacarakmoSkinArt');

UPDATE commodities
SET commodity_pt   = 'Carne de mamute albino de Quechua',
    commodity_ptbz = 'Carne de mamute albino de Quechua'
WHERE LOWER(symbol) = LOWER('AlbinoQuechuaMammoth');

UPDATE commodities
SET commodity_pt   = 'Algas',
    commodity_ptbz = 'Algas'
WHERE LOWER(symbol) = LOWER('Algae');

UPDATE commodities
SET commodity_pt   = 'Ovos coriáceos',
    commodity_ptbz = 'Ovos coriáceos'
WHERE LOWER(symbol) = LOWER('AlienEggs');

UPDATE commodities
SET commodity_pt   = 'Contratos legais da Aliança',
    commodity_ptbz = 'Contratos legais da Aliança'
WHERE LOWER(commodity) = LOWER('Alliance Legislative Contracts');

UPDATE commodities
SET commodity_pt   = 'Registros legais da Aliança',
    commodity_ptbz = 'Registros legais da Aliança'
WHERE LOWER(commodity) = LOWER('Alliance Legislative Records');

UPDATE commodities
SET commodity_pt   = 'Acordos comerciais da Aliança',
    commodity_ptbz = 'Acordos comerciais da Aliança'
WHERE LOWER(commodity) = LOWER('Alliance Trade Agreements');

UPDATE commodities
SET commodity_pt   = 'Pele Altairiana',
    commodity_ptbz = 'Pele Altairiana'
WHERE LOWER(symbol) = LOWER('AltairianSkin');

UPDATE commodities
SET commodity_pt   = 'Sabão corporal de Alya',
    commodity_ptbz = 'Sabão corporal de Alya'
WHERE LOWER(symbol) = LOWER('AlyaBodilySoap');

UPDATE commodities
SET commodity_pt   = 'Chave Antiga',
    commodity_ptbz = 'Chave Antiga'
WHERE LOWER(symbol) = LOWER('AncientKey');

UPDATE commodities
SET commodity_pt   = 'Fogos de artifício de Anduliga',
    commodity_ptbz = 'Fogos de artifício de Anduliga'
WHERE LOWER(symbol) = LOWER('AnduligaFireWorks');

UPDATE commodities
SET commodity_pt   = 'Fôlego prata de Crom',
    commodity_ptbz = 'Fôlego prata de Crom'
WHERE LOWER(symbol) = LOWER('AnimalEffigies');

UPDATE commodities
SET commodity_pt   = 'Café de Any Na',
    commodity_ptbz = 'Café de Any Na'
WHERE LOWER(symbol) = LOWER('AnyNaCoffee');

UPDATE commodities
SET commodity_pt   = 'Água da Vida',
    commodity_ptbz = 'Água da Vida'
WHERE LOWER(symbol) = LOWER('ApaVietii');

UPDATE commodities
SET commodity_pt   = 'Sistemas aquapônicos',
    commodity_ptbz = 'Sistemas aquapônicos'
WHERE LOWER(symbol) = LOWER('AquaponicSystems');

UPDATE commodities
SET commodity_pt   = 'Doces monásticos de Arouca',
    commodity_ptbz = 'Doces monásticos de Arouca'
WHERE LOWER(symbol) = LOWER('AroucaConventualSweets');

UPDATE commodities
SET commodity_pt   = 'Fórmula 42 de AZ Cancri',
    commodity_ptbz = 'Fórmula 42 de AZ Cancri'
WHERE LOWER(symbol) = LOWER('AZCancriFormula42');

UPDATE commodities
SET commodity_pt   = 'Cozido de greebles',
    commodity_ptbz = 'Cozido de greebles'
WHERE LOWER(symbol) = LOWER('BakedGreebles');

UPDATE commodities
SET commodity_pt   = 'Krill-do-vácuo de Baltah’sine',
    commodity_ptbz = 'Krill-do-vácuo de Baltah’sine'
WHERE LOWER(symbol) = LOWER('BaltahSineVacuumKrill');

UPDATE commodities
SET commodity_pt   = 'Couro anfíbio de Banki',
    commodity_ptbz = 'Couro anfíbio de Banki'
WHERE LOWER(symbol) = LOWER('BankiAmphibiousLeather');

UPDATE commodities
SET commodity_pt   = 'Gin de serpente de Bast',
    commodity_ptbz = 'Gin de serpente de Bast'
WHERE LOWER(symbol) = LOWER('BastSnakeGin');

UPDATE commodities
SET commodity_pt   = 'Couro de arraia de Belalans',
    commodity_ptbz = 'Couro de arraia de Belalans'
WHERE LOWER(symbol) = LOWER('BelalansRayLeather');

UPDATE commodities
SET commodity_pt   = 'Bertrandita',
    commodity_ptbz = 'Bertrandita'
WHERE LOWER(symbol) = LOWER('Bertrandite');

UPDATE commodities
SET commodity_pt   = 'Resíduo biológico',
    commodity_ptbz = 'Resíduo biológico'
WHERE LOWER(symbol) = LOWER('Biowaste');

UPDATE commodities
SET commodity_pt   = 'Leite de Azure',
    commodity_ptbz = 'Leite de Azure'
WHERE LOWER(symbol) = LOWER('BlueMilk');

UPDATE commodities
SET commodity_pt   = 'Patógenos de Borasetani',
    commodity_ptbz = 'Patógenos de Borasetani'
WHERE LOWER(symbol) = LOWER('BorasetaniPathogenetics');

UPDATE commodities
SET commodity_pt   = 'Bromelito',
    commodity_ptbz = 'Bromelito'
WHERE LOWER(symbol) = LOWER('Bromellite');

UPDATE commodities
SET commodity_pt   = 'Porta Copos Buckyball',
    commodity_ptbz = 'Porta Copos Buckyball'
WHERE LOWER(symbol) = LOWER('BuckyballBeerMats');

UPDATE commodities
SET commodity_pt   = 'Bile destilada de Burnham',
    commodity_ptbz = 'Bile destilada de Burnham'
WHERE LOWER(symbol) = LOWER('BurnhamBileDistillate');

UPDATE commodities
SET commodity_pt   = 'Café da marca CD-75 Kitten',
    commodity_ptbz = 'Café da marca CD-75 Kitten'
WHERE LOWER(symbol) = LOWER('CD75CatCoffee');

UPDATE commodities
SET commodity_pt   = 'Mega Gin de Centauri',
    commodity_ptbz = 'Mega Gin de Centauri'
WHERE LOWER(symbol) = LOWER('CentauriMegaGin');

UPDATE commodities
SET commodity_pt   = 'Chá cerimonial de Heike',
    commodity_ptbz = 'Chá cerimonial de Heike'
WHERE LOWER(symbol) = LOWER('CeremonialHeikeTea');

UPDATE commodities
SET commodity_pt   = 'Ovos de Aepyornis',
    commodity_ptbz = 'Ovos de Aepyornis'
WHERE LOWER(symbol) = LOWER('CetiAepyornisEgg');

UPDATE commodities
SET commodity_pt   = 'Coelhos de Ceti',
    commodity_ptbz = 'Coelhos de Ceti'
WHERE LOWER(symbol) = LOWER('CetiRabbits');

UPDATE commodities
SET commodity_pt   = 'Tecido camaleônico',
    commodity_ptbz = 'Tecido camaleônico'
WHERE LOWER(symbol) = LOWER('ChameleonCloth');

UPDATE commodities
SET commodity_pt   = 'Chateau de Aegaeon',
    commodity_ptbz = 'Chateau de Aegaeon'
WHERE LOWER(symbol) = LOWER('ChateauDeAegaeon');

UPDATE commodities
SET commodity_pt   = 'Cristais de sangue de Cherbones',
    commodity_ptbz = 'Cristais de sangue de Cherbones'
WHERE LOWER(symbol) = LOWER('CherbonesBloodCrystals');

UPDATE commodities
SET commodity_pt   = 'Pasta marinha de Chi Eridani',
    commodity_ptbz = 'Pasta marinha de Chi Eridani'
WHERE LOWER(symbol) = LOWER('ChiEridaniMarinePaste');

UPDATE commodities
SET commodity_pt   = 'Equipamento experimental classificado',
    commodity_ptbz = 'Equipamento experimental classificado'
WHERE LOWER(symbol) = LOWER('ClassifiedExperimentalEquipment');

UPDATE commodities
SET commodity_pt   = 'Cobre',
    commodity_ptbz = 'Cobre'
WHERE LOWER(symbol) = LOWER('Copper');

UPDATE commodities
SET commodity_pt   = 'Provisões espongiformes de Coquim',
    commodity_ptbz = 'Provisões espongiformes de Coquim'
WHERE LOWER(symbol) = LOWER('CoquimSpongiformVictuals');

UPDATE commodities
SET commodity_pt   = 'Mantimentos revolucionários',
    commodity_ptbz = 'Mantimentos revolucionários'
WHERE LOWER(commodity) = LOWER('Revolutionary supplies');

UPDATE commodities
SET commodity_pt   = 'Esferas cristalinas',
    commodity_ptbz = 'Esferas cristalinas'
WHERE LOWER(symbol) = LOWER('CrystallineSpheres');

UPDATE commodities
SET commodity_pt   = 'Carapaças de Damna',
    commodity_ptbz = 'Carapaças de Damna'
WHERE LOWER(symbol) = LOWER('DamnaCarapaces');

UPDATE commodities
SET commodity_pt   = 'Palmeiras de Delta Phoenicis',
    commodity_ptbz = 'Palmeiras de Delta Phoenicis'
WHERE LOWER(symbol) = LOWER('DeltaPhoenicisPalms');

UPDATE commodities
SET commodity_pt   = 'Trufas de Deuringas',
    commodity_ptbz = 'Trufas de Deuringas'
WHERE LOWER(symbol) = LOWER('DeuringasTruffles');

UPDATE commodities
SET commodity_pt   = 'Milho Ma de Diso',
    commodity_ptbz = 'Milho Ma de Diso'
WHERE LOWER(symbol) = LOWER('DisoMaCorn');

UPDATE commodities
SET commodity_pt   = 'Tecidos térmicos de Eleu',
    commodity_ptbz = 'Tecidos térmicos de Eleu'
WHERE LOWER(symbol) = LOWER('EleuThermals');

UPDATE commodities
SET commodity_pt   = 'Whisky perolado de Eranin',
    commodity_ptbz = 'Whisky perolado de Eranin'
WHERE LOWER(symbol) = LOWER('EraninPearlWhisky');

UPDATE commodities
SET commodity_pt   = 'Guarda-chuvas de Eshu',
    commodity_ptbz = 'Guarda-chuvas de Eshu'
WHERE LOWER(symbol) = LOWER('EshuUmbrellas');

UPDATE commodities
SET commodity_pt   = 'Caviar de Esuseku',
    commodity_ptbz = 'Caviar de Esuseku'
WHERE LOWER(symbol) = LOWER('EsusekuCaviar');

UPDATE commodities
SET commodity_pt   = 'Broto de chá de Ethgreze',
    commodity_ptbz = 'Broto de chá de Ethgreze'
WHERE LOWER(symbol) = LOWER('EthgrezeTeaBuds');

UPDATE commodities
SET commodity_pt   = 'Auxílio federal liberal',
    commodity_ptbz = 'Auxílio federal liberal'
WHERE LOWER(commodity) = LOWER('Liberal Federal Aid');

UPDATE commodities
SET commodity_pt   = 'Pacotes federais liberais',
    commodity_ptbz = 'Pacotes federais liberais'
WHERE LOWER(commodity) = LOWER('Liberal Federal Packages');

UPDATE commodities
SET commodity_pt   = 'Chá de Fujin',
    commodity_ptbz = 'Chá de Fujin'
WHERE LOWER(symbol) = LOWER('FujinTea');

UPDATE commodities
SET commodity_pt   = 'Guia de viagens da galáxia',
    commodity_ptbz = 'Guia de viagens da galáxia'
WHERE LOWER(symbol) = LOWER('GalacticTravelGuide');

UPDATE commodities
SET commodity_pt   = 'Pó de dança de Geawen',
    commodity_ptbz = 'Pó de dança de Geawen'
WHERE LOWER(symbol) = LOWER('GeawenDanceDust');

UPDATE commodities
SET commodity_pt   = 'Cerveja gerasiana de Gueuze',
    commodity_ptbz = 'Cerveja gerasiana de Gueuze'
WHERE LOWER(symbol) = LOWER('GerasianGueuzeBeer');

UPDATE commodities
SET commodity_pt   = 'Caramujos gigantes de Irukama',
    commodity_ptbz = 'Caramujos gigantes de Irukama'
WHERE LOWER(symbol) = LOWER('GiantIrukamaSnails');

UPDATE commodities
SET commodity_pt   = 'Verrix gigante',
    commodity_ptbz = 'Verrix gigante'
WHERE LOWER(symbol) = LOWER('GiantVerrix');

UPDATE commodities
SET commodity_pt   = 'Armas personalizadas de Gilya',
    commodity_ptbz = 'Armas personalizadas de Gilya'
WHERE LOWER(symbol) = LOWER('GilyaSignatureWeapons');

UPDATE commodities
SET commodity_pt   = 'Café Yaupon de Goman',
    commodity_ptbz = 'Café Yaupon de Goman'
WHERE LOWER(symbol) = LOWER('GomanYauponCoffee');

UPDATE commodities
SET commodity_pt   = 'Goslarita',
    commodity_ptbz = 'Goslarita'
WHERE LOWER(symbol) = LOWER('Goslarite');

UPDATE commodities
SET commodity_pt   = 'Contra-inteligência de Grom',
    commodity_ptbz = 'Contra-inteligência de Grom'
WHERE LOWER(commodity) = LOWER('Grom Counter Intelligence');

UPDATE commodities
SET commodity_pt   = 'Suprimentos Militares de Yuri Grom',
    commodity_ptbz = 'Suprimentos Militares de Yuri Grom'
WHERE LOWER(commodity) = LOWER('Yuri Grom’s Military Supplies');

UPDATE commodities
SET commodity_pt   = 'Infusão negra de Haiden',
    commodity_ptbz = 'Infusão negra de Haiden'
WHERE LOWER(symbol) = LOWER('HaidneBlackBrew');

UPDATE commodities
SET commodity_pt   = 'Rum Mar Prata de Harma',
    commodity_ptbz = 'Rum Mar Prata de Harma'
WHERE LOWER(symbol) = LOWER('HarmaSilverSeaRum');

UPDATE commodities
SET commodity_pt   = 'Apanhadores de sonhos de Havasupai',
    commodity_ptbz = 'Apanhadores de sonhos de Havasupai'
WHERE LOWER(symbol) = LOWER('HavasupaiDreamCatcher');

UPDATE commodities
SET commodity_pt   = 'Pérolas de Helvetitj',
    commodity_ptbz = 'Pérolas de Helvetitj'
WHERE LOWER(symbol) = LOWER('HelvetitjPearls');

UPDATE commodities
SET commodity_pt   = 'Carne de caça de HIP 10175',
    commodity_ptbz = 'Carne de caça de HIP 10175'
WHERE LOWER(symbol) = LOWER('HIP10175BushMeat');

UPDATE commodities
SET commodity_pt   = 'Proto-lula de HIP',
    commodity_ptbz = 'Proto-lula de HIP'
WHERE LOWER(symbol) = LOWER('HIP41181Squid');

UPDATE commodities
SET commodity_pt   = 'Enxame de HIP 118311',
    commodity_ptbz = 'Enxame de HIP 118311'
WHERE LOWER(symbol) = LOWER('HIP118311Swarm');

UPDATE commodities
SET commodity_pt   = 'Organofosfatos de HIP 80364',
    commodity_ptbz = 'Organofosfatos de HIP 80364'
WHERE LOWER(symbol) = LOWER('HIPOrganophosphates');

UPDATE commodities
SET commodity_pt   = 'Espadas de duelo de Holva',
    commodity_ptbz = 'Espadas de duelo de Holva'
WHERE LOWER(symbol) = LOWER('HolvaDuellingBlades');

UPDATE commodities
SET commodity_pt   = 'Pílulas da honestidade',
    commodity_ptbz = 'Pílulas da honestidade'
WHERE LOWER(symbol) = LOWER('HonestyPills');

UPDATE commodities
SET commodity_pt   = 'Trigo de HR 7221',
    commodity_ptbz = 'Trigo de HR 7221'
WHERE LOWER(symbol) = LOWER('HR7221Wheat');

UPDATE commodities
SET commodity_pt   = 'Pacote de contrabando do Kumo',
    commodity_ptbz = 'Pacote de contrabando do Kumo'
WHERE LOWER(commodity) = LOWER('Kumo Contraband Package');

UPDATE commodities
SET commodity_pt   = 'Prisioneiros políticos da Torval',
    commodity_ptbz = 'Prisioneiros políticos da Torval'
WHERE LOWER(commodity) = LOWER('Torval Political Prisoners');

UPDATE commodities
SET commodity_pt   = 'Bourbon de Epsilon Indi',
    commodity_ptbz = 'Bourbon de Epsilon Indi'
WHERE LOWER(symbol) = LOWER('IndiBourbon');

UPDATE commodities
SET commodity_pt   = 'Alambique de Jaques Quinentian',
    commodity_ptbz = 'Alambique de Jaques Quinentian'
WHERE LOWER(symbol) = LOWER('JaquesQuinentianStill');

UPDATE commodities
SET commodity_pt   = 'Caixa de quebra-cabeças de Jaradharre',
    commodity_ptbz = 'Caixa de quebra-cabeças de Jaradharre'
WHERE LOWER(symbol) = LOWER('JaradharrePuzzlebox');

UPDATE commodities
SET commodity_pt   = 'Arroz de Jaroua',
    commodity_ptbz = 'Arroz de Jaroua'
WHERE LOWER(symbol) = LOWER('JarouaRice');

UPDATE commodities
SET commodity_pt   = 'Mookah de Jotun',
    commodity_ptbz = 'Mookah de Jotun'
WHERE LOWER(symbol) = LOWER('JotunMookah');

UPDATE commodities
SET commodity_pt   = 'Sanguessugas filtradoras de Kachirigin',
    commodity_ptbz = 'Sanguessugas filtradoras de Kachirigin'
WHERE LOWER(symbol) = LOWER('KachiriginLeaches');

UPDATE commodities
SET commodity_pt   = 'Charutos de Kamitra',
    commodity_ptbz = 'Charutos de Kamitra'
WHERE LOWER(symbol) = LOWER('KamitraCigars');

UPDATE commodities
SET commodity_pt   = 'Armas históricas de Kamorin',
    commodity_ptbz = 'Armas históricas de Kamorin'
WHERE LOWER(symbol) = LOWER('KamorinHistoricWeapons');

UPDATE commodities
SET commodity_pt   = 'Alfaiataria de Karetii',
    commodity_ptbz = 'Alfaiataria de Karetii'
WHERE LOWER(symbol) = LOWER('KaretiiCouture');

UPDATE commodities
SET commodity_pt   = 'Lagostas de Karsuki Ti',
    commodity_ptbz = 'Lagostas de Karsuki Ti'
WHERE LOWER(symbol) = LOWER('KarsukiLocusts');

UPDATE commodities
SET commodity_pt   = 'Violinos de Kinago',
    commodity_ptbz = 'Violinos de Kinago'
WHERE LOWER(symbol) = LOWER('KinagoInstruments');

UPDATE commodities
SET commodity_pt   = 'Cerveja de Kongga',
    commodity_ptbz = 'Cerveja de Kongga'
WHERE LOWER(symbol) = LOWER('KonggaAle');

UPDATE commodities
SET commodity_pt   = 'Pastilhas de Korro Kung',
    commodity_ptbz = 'Pastilhas de Korro Kung'
WHERE LOWER(symbol) = LOWER('KorroKungPellets');

UPDATE commodities
SET commodity_pt   = 'Minas terrestres',
    commodity_ptbz = 'Minas terrestres'
WHERE LOWER(symbol) = LOWER('Landmines');

UPDATE commodities
SET commodity_pt   = 'Brandy laviano',
    commodity_ptbz = 'Brandy laviano'
WHERE LOWER(symbol) = LOWER('LavianBrandy');

UPDATE commodities
SET commodity_pt   = 'Relatórios de corrupção da Lavigny',
    commodity_ptbz = 'Relatórios de corrupção da Lavigny'
WHERE LOWER(commodity) = LOWER('Lavigny Corruption Reports');

UPDATE commodities
SET commodity_pt   = 'Mantimentos de campo da Lavigny',
    commodity_ptbz = 'Mantimentos de campo da Lavigny'
WHERE LOWER(commodity) = LOWER('Lavigny Field Supplies');

UPDATE commodities
SET commodity_pt   = 'Mantimentos de tropas da Lavigny',
    commodity_ptbz = 'Mantimentos de tropas da Lavigny'
WHERE LOWER(commodity) = LOWER('Lavigny Garrison Supplies');

UPDATE commodities
SET commodity_pt   = 'Suco do mal leestiano',
    commodity_ptbz = 'Suco do mal leestiano'
WHERE LOWER(symbol) = LOWER('LeestianEvilJuice');

UPDATE commodities
SET commodity_pt   = 'Extrato de café do vácuo',
    commodity_ptbz = 'Extrato de café do vácuo'
WHERE LOWER(symbol) = LOWER('LFTVoidExtractCoffee');

UPDATE commodities
SET commodity_pt   = 'Propaganda liberal',
    commodity_ptbz = 'Propaganda liberal'
WHERE LOWER(commodity) = LOWER('Liberal Propaganda');

UPDATE commodities
SET commodity_pt   = 'Oxigênio liquido',
    commodity_ptbz = 'Oxigênio liquido'
WHERE LOWER(symbol) = LOWER('LiquidOxygen');

UPDATE commodities
SET commodity_pt   = 'Vermes marinhos vivos de Hecate',
    commodity_ptbz = 'Vermes marinhos vivos de Hecate'
WHERE LOWER(symbol) = LOWER('LiveHecateSeaWorms');

UPDATE commodities
SET commodity_pt   = 'Armas militares marcadas',
    commodity_ptbz = 'Armas militares marcadas'
WHERE LOWER(commodity) = LOWER('Marked Military Arms');

UPDATE commodities
SET commodity_pt   = 'Hiperdoce de LTT',
    commodity_ptbz = 'Hiperdoce de LTT'
WHERE LOWER(symbol) = LOWER('LTTHyperSweet');

UPDATE commodities
SET commodity_pt   = 'Erva de Lyrae',
    commodity_ptbz = 'Erva de Lyrae'
WHERE LOWER(symbol) = LOWER('LyraeWeed');

UPDATE commodities
SET commodity_pt   = 'Escravos marcados',
    commodity_ptbz = 'Escravos marcados'
WHERE LOWER(commodity) = LOWER('Marked Slaves');

UPDATE commodities
SET commodity_pt   = 'Mestres Chefes',
    commodity_ptbz = 'Mestres Chefes'
WHERE LOWER(symbol) = LOWER('MasterChefs');

UPDATE commodities
SET commodity_pt   = 'Chá de jantar de Mechucos',
    commodity_ptbz = 'Chá de jantar de Mechucos'
WHERE LOWER(symbol) = LOWER('MechucosHighTea');

UPDATE commodities
SET commodity_pt   = 'Lubrificante de Medb',
    commodity_ptbz = 'Lubrificante de Medb'
WHERE LOWER(symbol) = LOWER('MedbStarlube');

UPDATE commodities
SET commodity_pt   = 'Equipamento de diagnóstico médico',
    commodity_ptbz = 'Equipamento de diagnóstico médico'
WHERE LOWER(symbol) = LOWER('MedicalDiagnosticEquipment');

UPDATE commodities
SET commodity_pt   = 'Tecidos de nível militar',
    commodity_ptbz = 'Tecidos de nível militar'
WHERE LOWER(symbol) = LOWER('MilitaryGradeFabrics');

UPDATE commodities
SET commodity_pt   = 'Banquete bestial de Mokojing',
    commodity_ptbz = 'Banquete bestial de Mokojing'
WHERE LOWER(symbol) = LOWER('MokojingBeastFeast');

UPDATE commodities
SET commodity_pt   = 'Cão do pântano de Momus',
    commodity_ptbz = 'Cão do pântano de Momus'
WHERE LOWER(symbol) = LOWER('MomusBogSpaniel');

UPDATE commodities
SET commodity_pt   = 'Gelatina da experiência de Motrona',
    commodity_ptbz = 'Gelatina da experiência de Motrona'
WHERE LOWER(symbol) = LOWER('MotronaExperienceJelly');

UPDATE commodities
SET commodity_pt   = 'Quitinaros de Mukusubii',
    commodity_ptbz = 'Quitinaros de Mukusubii'
WHERE LOWER(symbol) = LOWER('MukusubiiChitinOs');

UPDATE commodities
SET commodity_pt   = 'Fungo gigante de Mulachi',
    commodity_ptbz = 'Fungo gigante de Mulachi'
WHERE LOWER(symbol) = LOWER('MulachiGiantFungus');

UPDATE commodities
SET commodity_pt   = 'Escâner de múon',
    commodity_ptbz = 'Escâner de múon'
WHERE LOWER(symbol) = LOWER('MuTomImager');

UPDATE commodities
SET commodity_pt   = 'Ídolo misterioso',
    commodity_ptbz = 'Ídolo misterioso'
WHERE LOWER(symbol) = LOWER('MysteriousIdol');

UPDATE commodities
SET commodity_pt   = 'Nanomedicamentos',
    commodity_ptbz = 'Nanomedicamentos'
WHERE LOWER(symbol) = LOWER('Nanomedicines');

UPDATE commodities
SET commodity_pt   = 'Frutos de Neritus',
    commodity_ptbz = 'Frutos de Neritus'
WHERE LOWER(symbol) = LOWER('NeritusBerries');

UPDATE commodities
SET commodity_pt   = 'Opalas de fogo de Ngadandari',
    commodity_ptbz = 'Opalas de fogo de Ngadandari'
WHERE LOWER(symbol) = LOWER('NgadandariFireOpals');

UPDATE commodities
SET commodity_pt   = 'Antiguidades modernas de Nguna',
    commodity_ptbz = 'Antiguidades modernas de Nguna'
WHERE LOWER(symbol) = LOWER('NgunaModernAntiques');

UPDATE commodities
SET commodity_pt   = 'Selas de Njangari',
    commodity_ptbz = 'Selas de Njangari'
WHERE LOWER(symbol) = LOWER('NjangariSaddles');

UPDATE commodities
SET commodity_pt   = 'Exotanques não euclidianos',
    commodity_ptbz = 'Exotanques não euclidianos'
WHERE LOWER(symbol) = LOWER('NonEuclidianExotanks');

UPDATE commodities
SET commodity_pt   = 'Cápsula de escape ocupada',
    commodity_ptbz = 'Cápsula de escape ocupada'
WHERE LOWER(symbol) = LOWER('OccupiedCryoPod');

UPDATE commodities
SET commodity_pt   = 'Pimentas de Ochoeng',
    commodity_ptbz = 'Pimentas de Ochoeng'
WHERE LOWER(symbol) = LOWER('OchoengChillies');

UPDATE commodities
SET commodity_pt   = 'Cepa alfa de Onionhead',
    commodity_ptbz = 'Cepa alfa de Onionhead'
WHERE LOWER(symbol) = LOWER('OnionHeadA');

UPDATE commodities
SET commodity_pt   = 'Cepa beta de Onionhead',
    commodity_ptbz = 'Cepa beta de Onionhead'
WHERE LOWER(symbol) = LOWER('OnionHeadB');

UPDATE commodities
SET commodity_pt   = 'Derivados de Onionhead',
    commodity_ptbz = 'Derivados de Onionhead'
WHERE LOWER(commodity) = LOWER('Onionhead Derivatives');

UPDATE commodities
SET commodity_pt   = 'Amostras de Onionhead',
    commodity_ptbz = 'Amostras de Onionhead'
WHERE LOWER(commodity) = LOWER('Onionhead Samples');

UPDATE commodities
SET commodity_pt   = 'Artefatos de Ophiuch Exino',
    commodity_ptbz = 'Artefatos de Ophiuch Exino'
WHERE LOWER(symbol) = LOWER('OphiuchiExinoArtefacts');

UPDATE commodities
SET commodity_pt   = 'Fermentado perverso de Orrere',
    commodity_ptbz = 'Fermentado perverso de Orrere'
WHERE LOWER(symbol) = LOWER('OrrerianViciousBrew');

UPDATE commodities
SET commodity_pt   = 'Mercadorias fora de validade',
    commodity_ptbz = 'Mercadorias fora de validade'
WHERE LOWER(commodity) = LOWER('Out Of Date Goods');

UPDATE commodities
SET commodity_pt   = 'Incensos para orações de Pantaa',
    commodity_ptbz = 'Incensos para orações de Pantaa'
WHERE LOWER(symbol) = LOWER('PantaaPrayerSticks');

UPDATE commodities
SET commodity_pt   = 'Mantimentos de campo do Patreus',
    commodity_ptbz = 'Mantimentos de campo do Patreus'
WHERE LOWER(commodity) = LOWER('Patreus Field Supplies');

UPDATE commodities
SET commodity_pt   = 'Mantimentos de tropas do Patreus',
    commodity_ptbz = 'Mantimentos de tropas do Patreus'
WHERE LOWER(commodity) = LOWER('Patreus Garrison Supplies');

UPDATE commodities
SET commodity_pt   = 'Larvas auriculares de Pavonis',
    commodity_ptbz = 'Larvas auriculares de Pavonis'
WHERE LOWER(symbol) = LOWER('PavonisEarGrubs');

UPDATE commodities
SET commodity_pt   = 'Presentes pessoais',
    commodity_ptbz = 'Presentes pessoais'
WHERE LOWER(symbol) = LOWER('PersonalGifts');

UPDATE commodities
SET commodity_pt   = 'Liga de platina',
    commodity_ptbz = 'Liga de platina'
WHERE LOWER(symbol) = LOWER('PlatinumAloy');

UPDATE commodities
SET commodity_pt   = 'Multifornos de Rajukru',
    commodity_ptbz = 'Multifornos de Rajukru'
WHERE LOWER(symbol) = LOWER('RajukruStoves');

UPDATE commodities
SET commodity_pt   = 'Peles de cobra de Rapa Bao',
    commodity_ptbz = 'Peles de cobra de Rapa Bao'
WHERE LOWER(symbol) = LOWER('RapaBaoSnakeSkins');

UPDATE commodities
SET commodity_pt   = 'Mantimentos de campo do Hudson',
    commodity_ptbz = 'Mantimentos de campo do Hudson'
WHERE LOWER(commodity) = LOWER('Hudson’s Field Supplies');

UPDATE commodities
SET commodity_pt   = 'Mantimentos de tropas do Hudson',
    commodity_ptbz = 'Mantimentos de tropas do Hudson'
WHERE LOWER(commodity) = LOWER('Hudson Garrison Supplies');

UPDATE commodities
SET commodity_pt   = 'Inteligência restrita do Hudson',
    commodity_ptbz = 'Inteligência restrita do Hudson'
WHERE LOWER(commodity) = LOWER('Hudson’s Restricted Intel');

UPDATE commodities
SET commodity_pt   = 'Pacote restrito da Core',
    commodity_ptbz = 'Pacote restrito da Core'
WHERE LOWER(commodity) = LOWER('Core Restricted Package');

UPDATE commodities
SET commodity_pt   = 'Velho fumo de Rusani',
    commodity_ptbz = 'Velho fumo de Rusani'
WHERE LOWER(symbol) = LOWER('RusaniOldSmokey');

UPDATE commodities
SET commodity_pt   = 'Carne decorativa de Sanuma',
    commodity_ptbz = 'Carne decorativa de Sanuma'
WHERE LOWER(symbol) = LOWER('SanumaMEAT');

UPDATE commodities
SET commodity_pt   = 'Recipiente de núcleo SAP 8',
    commodity_ptbz = 'Recipiente de núcleo SAP 8'
WHERE LOWER(symbol) = LOWER('SAP8CoreContainer');

UPDATE commodities
SET commodity_pt   = 'Vinho saxão',
    commodity_ptbz = 'Vinho saxão'
WHERE LOWER(symbol) = LOWER('SaxonWine');

UPDATE commodities
SET commodity_pt   = 'Pesquisa científica',
    commodity_ptbz = 'Pesquisa científica'
WHERE LOWER(symbol) = LOWER('ScientificResearch');

UPDATE commodities
SET commodity_pt   = 'Amostras científicas',
    commodity_ptbz = 'Amostras científicas'
WHERE LOWER(symbol) = LOWER('ScientificSamples');

UPDATE commodities
SET commodity_pt   = 'Orquídea de Shan Charis',
    commodity_ptbz = 'Orquídea de Shan Charis'
WHERE LOWER(symbol) = LOWER('ShansCharisOrchid');

UPDATE commodities
SET commodity_pt   = 'Contratos corporativos do Sirius',
    commodity_ptbz = 'Contratos corporativos do Sirius'
WHERE LOWER(commodity) = LOWER('Sirius Corporate Contracts');

UPDATE commodities
SET commodity_pt   = 'Pacote de franquia do Sirius',
    commodity_ptbz = 'Pacote de franquia do Sirius'
WHERE LOWER(commodity) = LOWER('Sirius Franchise Package');

UPDATE commodities
SET commodity_pt   = 'Equipamento industrial do Sirius',
    commodity_ptbz = 'Equipamento industrial do Sirius'
WHERE LOWER(commodity) = LOWER('Sirius Industrial Equipment');

UPDATE commodities
SET commodity_pt   = 'Componentes de escumador',
    commodity_ptbz = 'Componentes de escumador'
WHERE LOWER(symbol) = LOWER('SkimerComponents');

UPDATE commodities
SET commodity_pt   = 'Relíquias de Soontill',
    commodity_ptbz = 'Relíquias de Soontill'
WHERE LOWER(symbol) = LOWER('SoontillRelics');

UPDATE commodities
SET commodity_pt   = 'Ouro cristalino de Sothis',
    commodity_ptbz = 'Ouro cristalino de Sothis'
WHERE LOWER(symbol) = LOWER('SothisCrystallineGold');

UPDATE commodities
SET commodity_pt   = 'Chá calmante de Tanmark',
    commodity_ptbz = 'Chá calmante de Tanmark'
WHERE LOWER(symbol) = LOWER('TanmarkTranquilTea');

UPDATE commodities
SET commodity_pt   = 'Especiaria de Tarach',
    commodity_ptbz = 'Especiaria de Tarach'
WHERE LOWER(symbol) = LOWER('TarachTorSpice');

UPDATE commodities
SET commodity_pt   = 'Sinos de vento de Tauri',
    commodity_ptbz = 'Sinos de vento de Tauri'
WHERE LOWER(symbol) = LOWER('TauriChimes');

UPDATE commodities
SET commodity_pt   = 'Potenciadores sanguíneos de Terra Mater',
    commodity_ptbz = 'Potenciadores sanguíneos de Terra Mater'
WHERE LOWER(symbol) = LOWER('TerraMaterBloodBores');

UPDATE commodities
SET commodity_pt   = 'Amostra de tecido da Thargoid Gerador',
    commodity_ptbz = 'Amostra de tecido da Thargoid Gerador'
WHERE LOWER(symbol) = LOWER('ThargoidGeneratorTissueSample');

UPDATE commodities
SET commodity_pt   = 'A caneca de Hutton',
    commodity_ptbz = 'A caneca de Hutton'
WHERE LOWER(symbol) = LOWER('TheHuttonMug');

UPDATE commodities
SET commodity_pt   = 'Creme de Thrutis',
    commodity_ptbz = 'Creme de Thrutis'
WHERE LOWER(symbol) = LOWER('ThrutisCream');

UPDATE commodities
SET commodity_pt   = 'Seda sintética de Tiegfries',
    commodity_ptbz = 'Seda sintética de Tiegfries'
WHERE LOWER(symbol) = LOWER('TiegfriesSynthSilk');

UPDATE commodities
SET commodity_pt   = 'Unidades Waste2Paste de Tiolce',
    commodity_ptbz = 'Unidades Waste2Paste de Tiolce'
WHERE LOWER(symbol) = LOWER('TiolceWaste2PasteUnits');

UPDATE commodities
SET commodity_pt   = 'Titânio',
    commodity_ptbz = 'Titânio'
WHERE LOWER(symbol) = LOWER('Titanium');

UPDATE commodities
SET commodity_pt   = 'Acordos comerciais da Torval',
    commodity_ptbz = 'Acordos comerciais da Torval'
WHERE LOWER(commodity) = LOWER('Torval Trade Agreements');

UPDATE commodities
SET commodity_pt   = 'Títulos da Torval',
    commodity_ptbz = 'Títulos da Torval'
WHERE LOWER(commodity) = LOWER('Torval Deeds');

UPDATE commodities
SET commodity_pt   = 'Virocida de Toxandji',
    commodity_ptbz = 'Virocida de Toxandji'
WHERE LOWER(symbol) = LOWER('ToxandjiVirocide');

UPDATE commodities
SET commodity_pt   = 'Onionhead lucana',
    commodity_ptbz = 'Onionhead lucana'
WHERE LOWER(symbol) = LOWER('TransgenicOnionHead');

UPDATE commodities
SET commodity_pt   = 'Suporte Secreto de Grom',
    commodity_ptbz = 'Suporte Secreto de Grom'
WHERE LOWER(commodity) = LOWER('Grom Underground Support');

UPDATE commodities
SET commodity_pt   = 'Desconhecido',
    commodity_ptbz = 'Desconhecido'
WHERE LOWER(commodity) = LOWER('Unknown');

UPDATE commodities
SET commodity_pt   = 'Mantimentos militares sem marca',
    commodity_ptbz = 'Mantimentos militares sem marca'
WHERE LOWER(commodity) = LOWER('Unmarked Military supplies');

UPDATE commodities
SET commodity_pt   = 'Uraninita',
    commodity_ptbz = 'Uraninita'
WHERE LOWER(symbol) = LOWER('Uraninite');

UPDATE commodities
SET commodity_pt   = 'Artefatos anciões',
    commodity_ptbz = 'Artefatos anciões'
WHERE LOWER(symbol) = LOWER('USSCargoAncientArtefact');

UPDATE commodities
SET commodity_pt   = 'Químicos experimentais',
    commodity_ptbz = 'Químicos experimentais'
WHERE LOWER(symbol) = LOWER('USSCargoExperimentalChemicals');

UPDATE commodities
SET commodity_pt   = 'Lagarta arbórea de Uszaian',
    commodity_ptbz = 'Lagarta arbórea de Uszaian'
WHERE LOWER(symbol) = LOWER('UszaianTreeGrub');

UPDATE commodities
SET commodity_pt   = 'Ovos milenares de Utgaroar',
    commodity_ptbz = 'Ovos milenares de Utgaroar'
WHERE LOWER(symbol) = LOWER('UtgaroarMillenialEggs');

UPDATE commodities
SET commodity_pt   = 'Dissidente da Utopia',
    commodity_ptbz = 'Dissidente da Utopia'
WHERE LOWER(commodity) = LOWER('Utopian Dissident');

UPDATE commodities
SET commodity_pt   = 'Mantimentos da Utopia',
    commodity_ptbz = 'Mantimentos da Utopia'
WHERE LOWER(commodity) = LOWER('Utopian Supplies');

UPDATE commodities
SET commodity_pt   = 'Publicidade da Utopia',
    commodity_ptbz = 'Publicidade da Utopia'
WHERE LOWER(commodity) = LOWER('Utopian Publicity');

UPDATE commodities
SET commodity_pt   = 'Asas de baixa gravidade de Uzumoku',
    commodity_ptbz = 'Asas de baixa gravidade de Uzumoku'
WHERE LOWER(symbol) = LOWER('UzumokuLowGWings');

UPDATE commodities
SET commodity_pt   = 'Pele de ceratomorfo de Vanayequi',
    commodity_ptbz = 'Pele de ceratomorfo de Vanayequi'
WHERE LOWER(symbol) = LOWER('VanayequiRhinoFur');

UPDATE commodities
SET commodity_pt   = 'Erva daninha de Vega',
    commodity_ptbz = 'Erva daninha de Vega'
WHERE LOWER(symbol) = LOWER('VegaSlimWeed');

UPDATE commodities
SET commodity_pt   = 'Esfoliante corporal de V Herculis',
    commodity_ptbz = 'Esfoliante corporal de V Herculis'
WHERE LOWER(symbol) = LOWER('VHerculisBodyRub');

UPDATE commodities
SET commodity_pt   = 'Renda vidavantiana',
    commodity_ptbz = 'Renda vidavantiana'
WHERE LOWER(symbol) = LOWER('VidavantianLace');

UPDATE commodities
SET commodity_pt   = 'Abelhas drones de Volkhab',
    commodity_ptbz = 'Abelhas drones de Volkhab'
WHERE LOWER(symbol) = LOWER('VolkhabBeeDrones');

UPDATE commodities
SET commodity_pt   = 'Purificadores de água',
    commodity_ptbz = 'Purificadores de água'
WHERE LOWER(symbol) = LOWER('WaterPurifiers');

UPDATE commodities
SET commodity_pt   = 'Águas de Shintara',
    commodity_ptbz = 'Águas de Shintara'
WHERE LOWER(symbol) = LOWER('WatersOfShintara');

UPDATE commodities
SET commodity_pt   = 'Pastilhas de trigo de Wheemete',
    commodity_ptbz = 'Pastilhas de trigo de Wheemete'
WHERE LOWER(symbol) = LOWER('WheemeteWheatCakes');

UPDATE commodities
SET commodity_pt   = 'Carne Kobe de Witchhaul',
    commodity_ptbz = 'Carne Kobe de Witchhaul'
WHERE LOWER(symbol) = LOWER('WitchhaulKobeBeef');

UPDATE commodities
SET commodity_pt   = 'Fôlego de lobo',
    commodity_ptbz = 'Fôlego de lobo'
WHERE LOWER(symbol) = LOWER('Wolf1301Fesh');

UPDATE commodities
SET commodity_pt   = 'Restos de componentes',
    commodity_ptbz = 'Restos de componentes'
WHERE LOWER(symbol) = LOWER('WreckageComponents');

UPDATE commodities
SET commodity_pt   = 'Sistemas hiperbóreos de Wulpa',
    commodity_ptbz = 'Sistemas hiperbóreos de Wulpa'
WHERE LOWER(symbol) = LOWER('WulpaHyperboreSystems');

UPDATE commodities
SET commodity_pt   = 'Espuma de Wuthielo Ku',
    commodity_ptbz = 'Espuma de Wuthielo Ku'
WHERE LOWER(symbol) = LOWER('WuthieloKuFroth');

UPDATE commodities
SET commodity_pt   = 'Mascotes biomórficos de Xihe',
    commodity_ptbz = 'Mascotes biomórficos de Xihe'
WHERE LOWER(symbol) = LOWER('XiheCompanions');

UPDATE commodities
SET commodity_pt   = 'Folhas de Yaso Kondi',
    commodity_ptbz = 'Folhas de Yaso Kondi'
WHERE LOWER(symbol) = LOWER('YasoKondiLeaf');

UPDATE commodities
SET commodity_pt   = 'Cola de larva de formiga de Zeessze',
    commodity_ptbz = 'Cola de larva de formiga de Zeessze'
WHERE LOWER(symbol) = LOWER('ZeesszeAntGlue');


-- Gap fill: rows where an existing locale column was left NULL by an earlier migration and
-- the same upstream file does have a translation. Guarded so no curated value is overwritten.


UPDATE commodities
SET commodity_es = 'Partículas de Anomalía'
WHERE LOWER(symbol) = LOWER('P_ParticulateSample')
  AND (commodity_es IS NULL OR commodity_es = '');

UPDATE commodities
SET commodity_ru = 'Планы атак'
WHERE LOWER(symbol) = LOWER('AssaultPlans')
  AND (commodity_ru IS NULL OR commodity_ru = '');

UPDATE commodities
SET commodity_ru = 'Бенитоит'
WHERE LOWER(symbol) = LOWER('Benitoite')
  AND (commodity_ru IS NULL OR commodity_ru = '');

UPDATE commodities
SET commodity_es = 'Apa Vietii'
WHERE LOWER(symbol) = LOWER('ApaVietii')
  AND (commodity_es IS NULL OR commodity_es = '');

UPDATE commodities
SET commodity_es = 'Duradrives'
WHERE LOWER(symbol) = LOWER('Duradrives')
  AND (commodity_es IS NULL OR commodity_es = '');

UPDATE commodities
SET commodity_es = 'Cápsula de Bioconservación Thargoide'
WHERE LOWER(symbol) = LOWER('ThargoidPod')
  AND (commodity_es IS NULL OR commodity_es = '');

UPDATE commodities
SET commodity_fr = 'Capsule de bioconfinement thargoïd'
WHERE LOWER(symbol) = LOWER('ThargoidPod')
  AND (commodity_fr IS NULL OR commodity_fr = '');

UPDATE commodities
SET commodity_ru = 'Таргоидская капсула для биоматериалов'
WHERE LOWER(symbol) = LOWER('ThargoidPod')
  AND (commodity_ru IS NULL OR commodity_ru = '');
