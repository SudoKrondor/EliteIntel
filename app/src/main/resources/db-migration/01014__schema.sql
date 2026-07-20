-- Add non-localized game symbol (FDevIDs 'symbol') to commodities so cargo
-- Inventory 'Name' (e.g. "atmosphericextractors") can be matched back to a row.
-- Source: EDCD/FDevIDs commodity.csv + rare_commodity.csv (rare not distinguished).
ALTER TABLE commodities
    ADD COLUMN symbol TEXT;

UPDATE commodities
SET symbol = 'AiRelics'
WHERE LOWER(commodity) = LOWER('AI Relics');
UPDATE commodities
SET symbol = 'AZCancriFormula42'
WHERE LOWER(commodity) = LOWER('AZ Cancri Formula 42');
UPDATE commodities
SET symbol = 'AdvancedCatalysers'
WHERE LOWER(commodity) = LOWER('Advanced Catalysers');
UPDATE commodities
SET symbol = 'AdvancedMedicines'
WHERE LOWER(commodity) = LOWER('Advanced Medicines');
UPDATE commodities
SET symbol = 'CetiAepyornisEgg'
WHERE LOWER(commodity) = LOWER('Aepyornis Egg');
UPDATE commodities
SET symbol = 'AganippeRush'
WHERE LOWER(commodity) = LOWER('Aganippe Rush');
UPDATE commodities
SET symbol = 'AgriculturalMedicines'
WHERE LOWER(commodity) = LOWER('Agri-Medicines');
UPDATE commodities
SET symbol = 'AgronomicTreatment'
WHERE LOWER(commodity) = LOWER('Agronomic Treatment');
UPDATE commodities
SET symbol = 'AlacarakmoSkinArt'
WHERE LOWER(commodity) = LOWER('Alacarakmo Skin Art');
UPDATE commodities
SET symbol = 'AlbinoQuechuaMammoth'
WHERE LOWER(commodity) = LOWER('Albino Quechua Mammoth Meat');
UPDATE commodities
SET symbol = 'Alexandrite'
WHERE LOWER(commodity) = LOWER('Alexandrite');
UPDATE commodities
SET symbol = 'Algae'
WHERE LOWER(commodity) = LOWER('Algae');
UPDATE commodities
SET symbol = 'AltairianSkin'
WHERE LOWER(commodity) = LOWER('Altairian Skin');
UPDATE commodities
SET symbol = 'Aluminium'
WHERE LOWER(commodity) = LOWER('Aluminium');
UPDATE commodities
SET symbol = 'AlyaBodilySoap'
WHERE LOWER(commodity) = LOWER('Alya Body Soap');
UPDATE commodities
SET symbol = 'USSCargoAncientArtefact'
WHERE LOWER(commodity) = LOWER('Ancient Artefact');
UPDATE commodities
SET symbol = 'AncientKey'
WHERE LOWER(commodity) = LOWER('Ancient Key (Guardian)');
UPDATE commodities
SET symbol = 'AnduligaFireWorks'
WHERE LOWER(commodity) = LOWER('Anduliga Fire Works');
UPDATE commodities
SET symbol = 'Animalmeat'
WHERE LOWER(commodity) = LOWER('Animal Meat');
UPDATE commodities
SET symbol = 'AnimalMonitors'
WHERE LOWER(commodity) = LOWER('Animal Monitors');
UPDATE commodities
SET symbol = 'P_ParticulateSample'
WHERE LOWER(commodity) = LOWER('Anomaly Particles');
UPDATE commodities
SET symbol = 'AntimatterContainmentUnit'
WHERE LOWER(commodity) = LOWER('Antimatter Containment Unit');
UPDATE commodities
SET symbol = 'AntiqueJewellery'
WHERE LOWER(commodity) = LOWER('Antique Jewellery');
UPDATE commodities
SET symbol = 'Antiquities'
WHERE LOWER(commodity) = LOWER('Antiquities');
UPDATE commodities
SET symbol = 'AnyNaCoffee'
WHERE LOWER(commodity) = LOWER('Any Na Coffee');
UPDATE commodities
SET symbol = 'ApaVietii'
WHERE LOWER(commodity) = LOWER('Apa Vietii');
UPDATE commodities
SET symbol = 'AquaponicSystems'
WHERE LOWER(commodity) = LOWER('Aquaponic Systems');
UPDATE commodities
SET symbol = 'AroucaConventualSweets'
WHERE LOWER(commodity) = LOWER('Arouca Conventual Sweets');
UPDATE commodities
SET symbol = 'ArticulationMotors'
WHERE LOWER(commodity) = LOWER('Articulation Motors');
UPDATE commodities
SET symbol = 'AssaultPlans'
WHERE LOWER(commodity) = LOWER('Assault Plans');
UPDATE commodities
SET symbol = 'AtmosphericExtractors'
WHERE LOWER(commodity) = LOWER('Atmospheric Processors');
UPDATE commodities
SET symbol = 'AutoFabricators'
WHERE LOWER(commodity) = LOWER('Auto-Fabricators');
UPDATE commodities
SET symbol = 'BlueMilk'
WHERE LOWER(commodity) = LOWER('Azure Milk');
UPDATE commodities
SET symbol = 'BakedGreebles'
WHERE LOWER(commodity) = LOWER('Baked Greebles');
UPDATE commodities
SET symbol = 'BaltahSineVacuumKrill'
WHERE LOWER(commodity) = LOWER('Baltah’sine Vacuum Krill');
UPDATE commodities
SET symbol = 'BankiAmphibiousLeather'
WHERE LOWER(commodity) = LOWER('Banki Amphibious Leather');
UPDATE commodities
SET symbol = 'BasicMedicines'
WHERE LOWER(commodity) = LOWER('Basic Medicines');
UPDATE commodities
SET symbol = 'BastSnakeGin'
WHERE LOWER(commodity) = LOWER('Bast Snake Gin');
UPDATE commodities
SET symbol = 'BattleWeapons'
WHERE LOWER(commodity) = LOWER('Battle Weapons');
UPDATE commodities
SET symbol = 'Bauxite'
WHERE LOWER(commodity) = LOWER('Bauxite');
UPDATE commodities
SET symbol = 'Beer'
WHERE LOWER(commodity) = LOWER('Beer');
UPDATE commodities
SET symbol = 'BelalansRayLeather'
WHERE LOWER(commodity) = LOWER('Belalans Ray Leather');
UPDATE commodities
SET symbol = 'Benitoite'
WHERE LOWER(commodity) = LOWER('Benitoite');
UPDATE commodities
SET symbol = 'Bertrandite'
WHERE LOWER(commodity) = LOWER('Bertrandite');
UPDATE commodities
SET symbol = 'Beryllium'
WHERE LOWER(commodity) = LOWER('Beryllium');
UPDATE commodities
SET symbol = 'BioReducingLichen'
WHERE LOWER(commodity) = LOWER('Bioreducing Lichen');
UPDATE commodities
SET symbol = 'Biowaste'
WHERE LOWER(commodity) = LOWER('Biowaste');
UPDATE commodities
SET symbol = 'Bismuth'
WHERE LOWER(commodity) = LOWER('Bismuth');
UPDATE commodities
SET symbol = 'USSCargoBlackBox'
WHERE LOWER(commodity) = LOWER('Black Box');
UPDATE commodities
SET symbol = 'ThargoidBoneFragments'
WHERE LOWER(commodity) = LOWER('Bone Fragments');
UPDATE commodities
SET symbol = 'BootlegLiquor'
WHERE LOWER(commodity) = LOWER('Bootleg Liquor');
UPDATE commodities
SET symbol = 'BorasetaniPathogenetics'
WHERE LOWER(commodity) = LOWER('Borasetani Pathogenetics');
UPDATE commodities
SET symbol = 'Bromellite'
WHERE LOWER(commodity) = LOWER('Bromellite');
UPDATE commodities
SET symbol = 'BuckyballBeerMats'
WHERE LOWER(commodity) = LOWER('Buckyball Beer Mats');
UPDATE commodities
SET symbol = 'BuildingFabricators'
WHERE LOWER(commodity) = LOWER('Building Fabricators');
UPDATE commodities
SET symbol = 'BurnhamBileDistillate'
WHERE LOWER(commodity) = LOWER('Burnham Bile Distillate');
UPDATE commodities
SET symbol = 'CD75CatCoffee'
WHERE LOWER(commodity) = LOWER('CD-75 Kitten Brand Coffee');
UPDATE commodities
SET symbol = 'CMMComposite'
WHERE LOWER(commodity) = LOWER('CMM Composite');
UPDATE commodities
SET symbol = 'ThargoidGeneratorTissueSample'
WHERE LOWER(commodity) = LOWER('Caustic Tissue Sample (Thargoid)');
UPDATE commodities
SET symbol = 'CentauriMegaGin'
WHERE LOWER(commodity) = LOWER('Centauri Mega Gin');
UPDATE commodities
SET symbol = 'CeramicComposites'
WHERE LOWER(commodity) = LOWER('Ceramic Composites');
UPDATE commodities
SET symbol = 'CeremonialHeikeTea'
WHERE LOWER(commodity) = LOWER('Ceremonial Heike Tea');
UPDATE commodities
SET symbol = 'CetiRabbits'
WHERE LOWER(commodity) = LOWER('Ceti Rabbits');
UPDATE commodities
SET symbol = 'ChameleonCloth'
WHERE LOWER(commodity) = LOWER('Chameleon Cloth');
UPDATE commodities
SET symbol = 'ChateauDeAegaeon'
WHERE LOWER(commodity) = LOWER('Chateau De Aegaeon');
UPDATE commodities
SET symbol = 'ChemicalWaste'
WHERE LOWER(commodity) = LOWER('Chemical Waste');
UPDATE commodities
SET symbol = 'CherbonesBloodCrystals'
WHERE LOWER(commodity) = LOWER('Cherbones Blood Crystals');
UPDATE commodities
SET symbol = 'ChiEridaniMarinePaste'
WHERE LOWER(commodity) = LOWER('Chi Eridani Marine Paste');
UPDATE commodities
SET symbol = 'ClassifiedExperimentalEquipment'
WHERE LOWER(commodity) = LOWER('Classified Experimental Equipment');
UPDATE commodities
SET symbol = 'Clothing'
WHERE LOWER(commodity) = LOWER('Clothing');
UPDATE commodities
SET symbol = 'Cobalt'
WHERE LOWER(commodity) = LOWER('Cobalt');
UPDATE commodities
SET symbol = 'Coffee'
WHERE LOWER(commodity) = LOWER('Coffee');
UPDATE commodities
SET symbol = 'Coltan'
WHERE LOWER(commodity) = LOWER('Coltan');
UPDATE commodities
SET symbol = 'CombatStabilisers'
WHERE LOWER(commodity) = LOWER('Combat Stabilisers');
UPDATE commodities
SET symbol = 'ComercialSamples'
WHERE LOWER(commodity) = LOWER('Commercial Samples');
UPDATE commodities
SET symbol = 'ComputerComponents'
WHERE LOWER(commodity) = LOWER('Computer Components');
UPDATE commodities
SET symbol = 'ConductiveFabrics'
WHERE LOWER(commodity) = LOWER('Conductive Fabrics');
UPDATE commodities
SET symbol = 'ConsumerTechnology'
WHERE LOWER(commodity) = LOWER('Consumer Technology');
UPDATE commodities
SET symbol = 'Copper'
WHERE LOWER(commodity) = LOWER('Copper');
UPDATE commodities
SET symbol = 'CoquimSpongiformVictuals'
WHERE LOWER(commodity) = LOWER('Coquim Spongiform Victuals');
UPDATE commodities
SET symbol = 'CoralSap'
WHERE LOWER(commodity) = LOWER('Coral Sap');
UPDATE commodities
SET symbol = 'AnimalEffigies'
WHERE LOWER(commodity) = LOWER('Crom Silver Fesh');
UPDATE commodities
SET symbol = 'CropHarvesters'
WHERE LOWER(commodity) = LOWER('Crop Harvesters');
UPDATE commodities
SET symbol = 'Cryolite'
WHERE LOWER(commodity) = LOWER('Cryolite');
UPDATE commodities
SET symbol = 'CrystallineSpheres'
WHERE LOWER(commodity) = LOWER('Crystalline Spheres');
UPDATE commodities
SET symbol = 'ThargoidCystSpecimen'
WHERE LOWER(commodity) = LOWER('Cyst Specimen');
UPDATE commodities
SET symbol = 'DamagedEscapePod'
WHERE LOWER(commodity) = LOWER('Damaged Escape Pod');
UPDATE commodities
SET symbol = 'DamnaCarapaces'
WHERE LOWER(commodity) = LOWER('Damna Carapaces');
UPDATE commodities
SET symbol = 'DataCore'
WHERE LOWER(commodity) = LOWER('Data Core');
UPDATE commodities
SET symbol = 'DeltaPhoenicisPalms'
WHERE LOWER(commodity) = LOWER('Delta Phoenicis Palms');
UPDATE commodities
SET symbol = 'DeuringasTruffles'
WHERE LOWER(commodity) = LOWER('Deuringas Truffles');
UPDATE commodities
SET symbol = 'DiplomaticBag'
WHERE LOWER(commodity) = LOWER('Diplomatic Bag');
UPDATE commodities
SET symbol = 'DisoMaCorn'
WHERE LOWER(commodity) = LOWER('Diso Ma Corn');
UPDATE commodities
SET symbol = 'DomesticAppliances'
WHERE LOWER(commodity) = LOWER('Domestic Appliances');
UPDATE commodities
SET symbol = 'Duradrives'
WHERE LOWER(commodity) = LOWER('Duradrives');
UPDATE commodities
SET symbol = 'EarthRelics'
WHERE LOWER(commodity) = LOWER('Earth Relics');
UPDATE commodities
SET symbol = 'AerialEdenApple'
WHERE LOWER(commodity) = LOWER('Eden Apples Of Aerial');
UPDATE commodities
SET symbol = 'EleuThermals'
WHERE LOWER(commodity) = LOWER('Eleu Thermals');
UPDATE commodities
SET symbol = 'EmergencyPowerCells'
WHERE LOWER(commodity) = LOWER('Emergency Power Cells');
UPDATE commodities
SET symbol = 'EncryptedCorrespondence'
WHERE LOWER(commodity) = LOWER('Encrypted Correspondence');
UPDATE commodities
SET symbol = 'EncriptedDataStorage'
WHERE LOWER(commodity) = LOWER('Encrypted Data Storage');
UPDATE commodities
SET symbol = 'PowerGridAssembly'
WHERE LOWER(commodity) = LOWER('Energy Grid Assembly');
UPDATE commodities
SET symbol = 'EraninPearlWhisky'
WHERE LOWER(commodity) = LOWER('Eranin Pearl Whisky');
UPDATE commodities
SET symbol = 'EshuUmbrellas'
WHERE LOWER(commodity) = LOWER('Eshu Umbrellas');
UPDATE commodities
SET symbol = 'EsusekuCaviar'
WHERE LOWER(commodity) = LOWER('Esuseku Caviar');
UPDATE commodities
SET symbol = 'EthgrezeTeaBuds'
WHERE LOWER(commodity) = LOWER('Ethgreze Tea Buds');
UPDATE commodities
SET symbol = 'EvacuationShelter'
WHERE LOWER(commodity) = LOWER('Evacuation Shelter');
UPDATE commodities
SET symbol = 'ExhaustManifold'
WHERE LOWER(commodity) = LOWER('Exhaust Manifold');
UPDATE commodities
SET symbol = 'USSCargoExperimentalChemicals'
WHERE LOWER(commodity) = LOWER('Experimental Chemicals');
UPDATE commodities
SET symbol = 'Explosives'
WHERE LOWER(commodity) = LOWER('Explosives');
UPDATE commodities
SET symbol = 'Fish'
WHERE LOWER(commodity) = LOWER('Fish');
UPDATE commodities
SET symbol = 'FoodCartridges'
WHERE LOWER(commodity) = LOWER('Food Cartridges');
UPDATE commodities
SET symbol = 'FossilRemnants'
WHERE LOWER(commodity) = LOWER('Fossil Remnants');
UPDATE commodities
SET symbol = 'FruitAndVegetables'
WHERE LOWER(commodity) = LOWER('Fruit and Vegetables');
UPDATE commodities
SET symbol = 'FujinTea'
WHERE LOWER(commodity) = LOWER('Fujin Tea');
UPDATE commodities
SET symbol = 'GalacticTravelGuide'
WHERE LOWER(commodity) = LOWER('Galactic Travel Guide');
UPDATE commodities
SET symbol = 'Gallite'
WHERE LOWER(commodity) = LOWER('Gallite');
UPDATE commodities
SET symbol = 'Gallium'
WHERE LOWER(commodity) = LOWER('Gallium');
UPDATE commodities
SET symbol = 'GeawenDanceDust'
WHERE LOWER(commodity) = LOWER('Geawen Dance Dust');
UPDATE commodities
SET symbol = 'GeneBank'
WHERE LOWER(commodity) = LOWER('Gene Bank');
UPDATE commodities
SET symbol = 'GeologicalEquipment'
WHERE LOWER(commodity) = LOWER('Geological Equipment');
UPDATE commodities
SET symbol = 'GeologicalSamples'
WHERE LOWER(commodity) = LOWER('Geological Samples');
UPDATE commodities
SET symbol = 'GerasianGueuzeBeer'
WHERE LOWER(commodity) = LOWER('Gerasian Gueuze Beer');
UPDATE commodities
SET symbol = 'GiantIrukamaSnails'
WHERE LOWER(commodity) = LOWER('Giant Irukama Snails');
UPDATE commodities
SET symbol = 'GiantVerrix'
WHERE LOWER(commodity) = LOWER('Giant Verrix');
UPDATE commodities
SET symbol = 'GilyaSignatureWeapons'
WHERE LOWER(commodity) = LOWER('Gilya Signature Weapons');
UPDATE commodities
SET symbol = 'Gold'
WHERE LOWER(commodity) = LOWER('Gold');
UPDATE commodities
SET symbol = 'GomanYauponCoffee'
WHERE LOWER(commodity) = LOWER('Goman Yaupon Coffee');
UPDATE commodities
SET symbol = 'Goslarite'
WHERE LOWER(commodity) = LOWER('Goslarite');
UPDATE commodities
SET symbol = 'Grain'
WHERE LOWER(commodity) = LOWER('Grain');
UPDATE commodities
SET symbol = 'Grandidierite'
WHERE LOWER(commodity) = LOWER('Grandidierite');
UPDATE commodities
SET symbol = 'AncientCasket'
WHERE LOWER(commodity) = LOWER('Guardian Casket');
UPDATE commodities
SET symbol = 'AncientOrb'
WHERE LOWER(commodity) = LOWER('Guardian Orb');
UPDATE commodities
SET symbol = 'AncientRelic'
WHERE LOWER(commodity) = LOWER('Guardian Relic');
UPDATE commodities
SET symbol = 'AncientTablet'
WHERE LOWER(commodity) = LOWER('Guardian Tablet');
UPDATE commodities
SET symbol = 'AncientTotem'
WHERE LOWER(commodity) = LOWER('Guardian Totem');
UPDATE commodities
SET symbol = 'AncientUrn'
WHERE LOWER(commodity) = LOWER('Guardian Urn');
UPDATE commodities
SET symbol = 'HazardousEnvironmentSuits'
WHERE LOWER(commodity) = LOWER('H.E. Suits');
UPDATE commodities
SET symbol = 'HIP10175BushMeat'
WHERE LOWER(commodity) = LOWER('HIP 10175 Bush Meat');
UPDATE commodities
SET symbol = 'HIP118311Swarm'
WHERE LOWER(commodity) = LOWER('HIP 118311 Swarm');
UPDATE commodities
SET symbol = 'HIP41181Squid'
WHERE LOWER(commodity) = LOWER('HIP Proto-Squid');
UPDATE commodities
SET symbol = 'HNShockMount'
WHERE LOWER(commodity) = LOWER('HN Shock Mount');
UPDATE commodities
SET symbol = 'HR7221Wheat'
WHERE LOWER(commodity) = LOWER('HR 7221 Wheat');
UPDATE commodities
SET symbol = 'Haematite'
WHERE LOWER(commodity) = LOWER('Haematite');
UPDATE commodities
SET symbol = 'Hafnium178'
WHERE LOWER(commodity) = LOWER('Hafnium 178');
UPDATE commodities
SET symbol = 'HaidneBlackBrew'
WHERE LOWER(commodity) = LOWER('Haiden Black Brew');
UPDATE commodities
SET symbol = 'DiagnosticSensor'
WHERE LOWER(commodity) = LOWER('Hardware Diagnostic Sensor');
UPDATE commodities
SET symbol = 'HarmaSilverSeaRum'
WHERE LOWER(commodity) = LOWER('Harma Silver Sea Rum');
UPDATE commodities
SET symbol = 'HavasupaiDreamCatcher'
WHERE LOWER(commodity) = LOWER('Havasupai Dream Catcher');
UPDATE commodities
SET symbol = 'HeatsinkInterlink'
WHERE LOWER(commodity) = LOWER('Heatsink Interlink');
UPDATE commodities
SET symbol = 'HelvetitjPearls'
WHERE LOWER(commodity) = LOWER('Helvetitj Pearls');
UPDATE commodities
SET symbol = 'HIPOrganophosphates'
WHERE LOWER(commodity) = LOWER('Hip Organophosphates');
UPDATE commodities
SET symbol = 'HolvaDuellingBlades'
WHERE LOWER(commodity) = LOWER('Holva Duelling Blades');
UPDATE commodities
SET symbol = 'HonestyPills'
WHERE LOWER(commodity) = LOWER('Honesty Pills');
UPDATE commodities
SET symbol = 'Hostage'
WHERE LOWER(commodity) = LOWER('Hostages');
UPDATE commodities
SET symbol = 'HydrogenFuel'
WHERE LOWER(commodity) = LOWER('Hydrogen Fuel');
UPDATE commodities
SET symbol = 'HydrogenPeroxide'
WHERE LOWER(commodity) = LOWER('Hydrogen Peroxide');
UPDATE commodities
SET symbol = 'ImperialSlaves'
WHERE LOWER(commodity) = LOWER('Imperial Slaves');
UPDATE commodities
SET symbol = 'UnknownMineral'
WHERE LOWER(commodity) = LOWER('Impure Spire Mineral');
UPDATE commodities
SET symbol = 'IndiBourbon'
WHERE LOWER(commodity) = LOWER('Indi Bourbon');
UPDATE commodities
SET symbol = 'Indite'
WHERE LOWER(commodity) = LOWER('Indite');
UPDATE commodities
SET symbol = 'Indium'
WHERE LOWER(commodity) = LOWER('Indium');
UPDATE commodities
SET symbol = 'InsulatingMembrane'
WHERE LOWER(commodity) = LOWER('Insulating Membrane');
UPDATE commodities
SET symbol = 'IonDistributor'
WHERE LOWER(commodity) = LOWER('Ion Distributor');
UPDATE commodities
SET symbol = 'Jadeite'
WHERE LOWER(commodity) = LOWER('Jadeite');
UPDATE commodities
SET symbol = 'JaquesQuinentianStill'
WHERE LOWER(commodity) = LOWER('Jaques Quinentian Still');
UPDATE commodities
SET symbol = 'JaradharrePuzzlebox'
WHERE LOWER(commodity) = LOWER('Jaradharre Puzzle Box');
UPDATE commodities
SET symbol = 'JarouaRice'
WHERE LOWER(commodity) = LOWER('Jaroua Rice');
UPDATE commodities
SET symbol = 'JotunMookah'
WHERE LOWER(commodity) = LOWER('Jotun Mookah');
UPDATE commodities
SET symbol = 'KachiriginLeaches'
WHERE LOWER(commodity) = LOWER('Kachirigin Filter Leeches');
UPDATE commodities
SET symbol = 'KamitraCigars'
WHERE LOWER(commodity) = LOWER('Kamitra Cigars');
UPDATE commodities
SET symbol = 'KamorinHistoricWeapons'
WHERE LOWER(commodity) = LOWER('Kamorin Historic Weapons');
UPDATE commodities
SET symbol = 'KaretiiCouture'
WHERE LOWER(commodity) = LOWER('Karetii Couture');
UPDATE commodities
SET symbol = 'KarsukiLocusts'
WHERE LOWER(commodity) = LOWER('Karsuki Locusts');
UPDATE commodities
SET symbol = 'KinagoInstruments'
WHERE LOWER(commodity) = LOWER('Kinago Violins');
UPDATE commodities
SET symbol = 'KonggaAle'
WHERE LOWER(commodity) = LOWER('Kongga Ale');
UPDATE commodities
SET symbol = 'KorroKungPellets'
WHERE LOWER(commodity) = LOWER('Koro Kung Pellets');
UPDATE commodities
SET symbol = 'LTTHyperSweet'
WHERE LOWER(commodity) = LOWER('LTT Hyper Sweet');
UPDATE commodities
SET symbol = 'TerrainEnrichmentSystems'
WHERE LOWER(commodity) = LOWER('Land Enrichment Systems');
UPDATE commodities
SET symbol = 'Landmines'
WHERE LOWER(commodity) = LOWER('Landmines');
UPDATE commodities
SET symbol = 'Lanthanum'
WHERE LOWER(commodity) = LOWER('Lanthanum');
UPDATE commodities
SET symbol = 'LargeExplorationDataCash'
WHERE LOWER(commodity) = LOWER('Large Survey Data Cache');
UPDATE commodities
SET symbol = 'LavianBrandy'
WHERE LOWER(commodity) = LOWER('Lavian Brandy');
UPDATE commodities
SET symbol = 'Leather'
WHERE LOWER(commodity) = LOWER('Leather');
UPDATE commodities
SET symbol = 'AlienEggs'
WHERE LOWER(commodity) = LOWER('Leathery Eggs');
UPDATE commodities
SET symbol = 'LeestianEvilJuice'
WHERE LOWER(commodity) = LOWER('Leestian Evil Juice');
UPDATE commodities
SET symbol = 'Lepidolite'
WHERE LOWER(commodity) = LOWER('Lepidolite');
UPDATE commodities
SET symbol = 'Drones'
WHERE LOWER(commodity) = LOWER('Limpets');
UPDATE commodities
SET symbol = 'LiquidOxygen'
WHERE LOWER(commodity) = LOWER('Liquid Oxygen');
UPDATE commodities
SET symbol = 'LiquidOxygen'
WHERE LOWER(commodity) = LOWER('Liquid oxygen');
UPDATE commodities
SET symbol = 'Liquor'
WHERE LOWER(commodity) = LOWER('Liquor');
UPDATE commodities
SET symbol = 'Lithium'
WHERE LOWER(commodity) = LOWER('Lithium');
UPDATE commodities
SET symbol = 'LithiumHydroxide'
WHERE LOWER(commodity) = LOWER('Lithium Hydroxide');
UPDATE commodities
SET symbol = 'LiveHecateSeaWorms'
WHERE LOWER(commodity) = LOWER('Live Hecate Sea Worms');
UPDATE commodities
SET symbol = 'LowTemperatureDiamond'
WHERE LOWER(commodity) = LOWER('Low Temperature Diamonds');
UPDATE commodities
SET symbol = 'TransgenicOnionHead'
WHERE LOWER(commodity) = LOWER('Lucan Onionhead');
UPDATE commodities
SET symbol = 'LyraeWeed'
WHERE LOWER(commodity) = LOWER('Lyrae Weed');
UPDATE commodities
SET symbol = 'MagneticEmitterCoil'
WHERE LOWER(commodity) = LOWER('Magnetic Emitter Coil');
UPDATE commodities
SET symbol = 'MarineSupplies'
WHERE LOWER(commodity) = LOWER('Marine Equipment');
UPDATE commodities
SET symbol = 'MasterChefs'
WHERE LOWER(commodity) = LOWER('Master Chefs');
UPDATE commodities
SET symbol = 'MechucosHighTea'
WHERE LOWER(commodity) = LOWER('Mechucos High Tea');
UPDATE commodities
SET symbol = 'MedbStarlube'
WHERE LOWER(commodity) = LOWER('Medb Starlube');
UPDATE commodities
SET symbol = 'MedicalDiagnosticEquipment'
WHERE LOWER(commodity) = LOWER('Medical Diagnostic Equipment');
UPDATE commodities
SET symbol = 'MetaAlloys'
WHERE LOWER(commodity) = LOWER('Meta-Alloys');
UPDATE commodities
SET symbol = 'MethaneClathrate'
WHERE LOWER(commodity) = LOWER('Methane Clathrate');
UPDATE commodities
SET symbol = 'MethanolMonohydrateCrystals'
WHERE LOWER(commodity) = LOWER('Methanol Monohydrate Crystals');
UPDATE commodities
SET symbol = 'MicroControllers'
WHERE LOWER(commodity) = LOWER('Micro Controllers');
UPDATE commodities
SET symbol = 'CoolingHoses'
WHERE LOWER(commodity) = LOWER('Micro-weave Cooling Hoses');
UPDATE commodities
SET symbol = 'HeliostaticFurnaces'
WHERE LOWER(commodity) = LOWER('Microbial Furnaces');
UPDATE commodities
SET symbol = 'MilitaryGradeFabrics'
WHERE LOWER(commodity) = LOWER('Military Grade Fabrics');
UPDATE commodities
SET symbol = 'MilitaryIntelligence'
WHERE LOWER(commodity) = LOWER('Military Intelligence');
UPDATE commodities
SET symbol = 'USSCargoMilitaryPlans'
WHERE LOWER(commodity) = LOWER('Military Plans');
UPDATE commodities
SET symbol = 'MineralExtractors'
WHERE LOWER(commodity) = LOWER('Mineral Extractors');
UPDATE commodities
SET symbol = 'MineralOil'
WHERE LOWER(commodity) = LOWER('Mineral Oil');
UPDATE commodities
SET symbol = 'ModularTerminals'
WHERE LOWER(commodity) = LOWER('Modular Terminals');
UPDATE commodities
SET symbol = 'Moissanite'
WHERE LOWER(commodity) = LOWER('Moissanite');
UPDATE commodities
SET symbol = 'MokojingBeastFeast'
WHERE LOWER(commodity) = LOWER('Mokojing Beast Feast');
UPDATE commodities
SET symbol = 'M_TissueSample_Nerves'
WHERE LOWER(commodity) = LOWER('Mollusc Brain Tissue');
UPDATE commodities
SET symbol = 'M_TissueSample_Fluid'
WHERE LOWER(commodity) = LOWER('Mollusc Fluid');
UPDATE commodities
SET symbol = 'M3_TissueSample_Membrane'
WHERE LOWER(commodity) = LOWER('Mollusc Membrane');
UPDATE commodities
SET symbol = 'M3_TissueSample_Mycelium'
WHERE LOWER(commodity) = LOWER('Mollusc Mycelium');
UPDATE commodities
SET symbol = 'M_TissueSample_Soft'
WHERE LOWER(commodity) = LOWER('Mollusc Soft Tissue');
UPDATE commodities
SET symbol = 'M3_TissueSample_Spores'
WHERE LOWER(commodity) = LOWER('Mollusc Spores');
UPDATE commodities
SET symbol = 'MomusBogSpaniel'
WHERE LOWER(commodity) = LOWER('Momus Bog Spaniel');
UPDATE commodities
SET symbol = 'Monazite'
WHERE LOWER(commodity) = LOWER('Monazite');
UPDATE commodities
SET symbol = 'MotronaExperienceJelly'
WHERE LOWER(commodity) = LOWER('Motrona Experience Jelly');
UPDATE commodities
SET symbol = 'MukusubiiChitinOs'
WHERE LOWER(commodity) = LOWER('Mukusubii Chitin-os');
UPDATE commodities
SET symbol = 'MulachiGiantFungus'
WHERE LOWER(commodity) = LOWER('Mulachi Giant Fungus');
UPDATE commodities
SET symbol = 'MuTomImager'
WHERE LOWER(commodity) = LOWER('Muon Imager');
UPDATE commodities
SET symbol = 'Musgravite'
WHERE LOWER(commodity) = LOWER('Musgravite');
UPDATE commodities
SET symbol = 'MysteriousIdol'
WHERE LOWER(commodity) = LOWER('Mysterious Idol');
UPDATE commodities
SET symbol = 'Nanobreakers'
WHERE LOWER(commodity) = LOWER('Nanobreakers');
UPDATE commodities
SET symbol = 'Nanomedicines'
WHERE LOWER(commodity) = LOWER('Nanomedicines');
UPDATE commodities
SET symbol = 'BasicNarcotics'
WHERE LOWER(commodity) = LOWER('Narcotics');
UPDATE commodities
SET symbol = 'NaturalFabrics'
WHERE LOWER(commodity) = LOWER('Natural Fabrics');
UPDATE commodities
SET symbol = 'NeofabricInsulation'
WHERE LOWER(commodity) = LOWER('Neofabric Insulation');
UPDATE commodities
SET symbol = 'NeritusBerries'
WHERE LOWER(commodity) = LOWER('Neritus Berries');
UPDATE commodities
SET symbol = 'NerveAgents'
WHERE LOWER(commodity) = LOWER('Nerve Agents');
UPDATE commodities
SET symbol = 'NgadandariFireOpals'
WHERE LOWER(commodity) = LOWER('Ngadandari Fire Opals');
UPDATE commodities
SET symbol = 'NgunaModernAntiques'
WHERE LOWER(commodity) = LOWER('Nguna Modern Antiques');
UPDATE commodities
SET symbol = 'NjangariSaddles'
WHERE LOWER(commodity) = LOWER('Njangari Saddles');
UPDATE commodities
SET symbol = 'NonEuclidianExotanks'
WHERE LOWER(commodity) = LOWER('Non Euclidian Exotanks');
UPDATE commodities
SET symbol = 'NonLethalWeapons'
WHERE LOWER(commodity) = LOWER('Non-Lethal Weapons');
UPDATE commodities
SET symbol = 'OccupiedCryoPod'
WHERE LOWER(commodity) = LOWER('Occupied Escape Pod');
UPDATE commodities
SET symbol = 'OchoengChillies'
WHERE LOWER(commodity) = LOWER('Ochoeng Chillies');
UPDATE commodities
SET symbol = 'OnionHead'
WHERE LOWER(commodity) = LOWER('Onionhead');
UPDATE commodities
SET symbol = 'OnionHeadA'
WHERE LOWER(commodity) = LOWER('Onionhead Alpha Strain');
UPDATE commodities
SET symbol = 'OnionHeadB'
WHERE LOWER(commodity) = LOWER('Onionhead Beta Strain');
UPDATE commodities
SET symbol = 'OnionHeadC'
WHERE LOWER(commodity) = LOWER('Onionhead Gamma Strain');
UPDATE commodities
SET symbol = 'OphiuchiExinoArtefacts'
WHERE LOWER(commodity) = LOWER('Ophiuch Exino Artefacts');
UPDATE commodities
SET symbol = 'ThargoidOrganSample'
WHERE LOWER(commodity) = LOWER('Organ Sample');
UPDATE commodities
SET symbol = 'OrrerianViciousBrew'
WHERE LOWER(commodity) = LOWER('Orrerian Vicious Brew');
UPDATE commodities
SET symbol = 'Osmium'
WHERE LOWER(commodity) = LOWER('Osmium');
UPDATE commodities
SET symbol = 'Painite'
WHERE LOWER(commodity) = LOWER('Painite');
UPDATE commodities
SET symbol = 'Palladium'
WHERE LOWER(commodity) = LOWER('Palladium');
UPDATE commodities
SET symbol = 'PantaaPrayerSticks'
WHERE LOWER(commodity) = LOWER('Pantaa Prayer Sticks');
UPDATE commodities
SET symbol = 'PavonisEarGrubs'
WHERE LOWER(commodity) = LOWER('Pavonis Ear Grubs');
UPDATE commodities
SET symbol = 'PerformanceEnhancers'
WHERE LOWER(commodity) = LOWER('Performance Enhancers');
UPDATE commodities
SET symbol = 'PersonalEffects'
WHERE LOWER(commodity) = LOWER('Personal Effects');
UPDATE commodities
SET symbol = 'PersonalWeapons'
WHERE LOWER(commodity) = LOWER('Personal Weapons');
UPDATE commodities
SET symbol = 'Pesticides'
WHERE LOWER(commodity) = LOWER('Pesticides');
UPDATE commodities
SET symbol = 'Platinum'
WHERE LOWER(commodity) = LOWER('Platinum');
UPDATE commodities
SET symbol = 'PlatinumAloy'
WHERE LOWER(commodity) = LOWER('Platinum Alloy');
UPDATE commodities
SET symbol = 'S_TissueSample_Cells'
WHERE LOWER(commodity) = LOWER('Pod Core Tissue');
UPDATE commodities
SET symbol = 'S_TissueSample_Surface'
WHERE LOWER(commodity) = LOWER('Pod Dead Tissue');
UPDATE commodities
SET symbol = 'S6_TissueSample_Mesoglea'
WHERE LOWER(commodity) = LOWER('Pod Mesoglea');
UPDATE commodities
SET symbol = 'S6_TissueSample_Cells'
WHERE LOWER(commodity) = LOWER('Pod Outer Tissue');
UPDATE commodities
SET symbol = 'S6_TissueSample_Coenosarc'
WHERE LOWER(commodity) = LOWER('Pod Shell Tissue');
UPDATE commodities
SET symbol = 'S_TissueSample_Core'
WHERE LOWER(commodity) = LOWER('Pod Surface Tissue');
UPDATE commodities
SET symbol = 'S9_TissueSample_Shell'
WHERE LOWER(commodity) = LOWER('Pod Tissue');
UPDATE commodities
SET symbol = 'PoliticalPrisoner'
WHERE LOWER(commodity) = LOWER('Political Prisoners');
UPDATE commodities
SET symbol = 'Polymers'
WHERE LOWER(commodity) = LOWER('Polymers');
UPDATE commodities
SET symbol = 'PowerConverter'
WHERE LOWER(commodity) = LOWER('Power Converter');
UPDATE commodities
SET symbol = 'PowerGenerators'
WHERE LOWER(commodity) = LOWER('Power Generators');
UPDATE commodities
SET symbol = 'PowerTransferConduits'
WHERE LOWER(commodity) = LOWER('Power Transfer Bus');
UPDATE commodities
SET symbol = 'Praseodymium'
WHERE LOWER(commodity) = LOWER('Praseodymium');
UPDATE commodities
SET symbol = 'PreciousGems'
WHERE LOWER(commodity) = LOWER('Precious Gems');
UPDATE commodities
SET symbol = 'ProgenitorCells'
WHERE LOWER(commodity) = LOWER('Progenitor Cells');
UPDATE commodities
SET symbol = 'ProhibitedResearchMaterials'
WHERE LOWER(commodity) = LOWER('Prohibited Research Materials');
UPDATE commodities
SET symbol = 'UnknownSack'
WHERE LOWER(commodity) = LOWER('Protective Membrane Scrap');
UPDATE commodities
SET symbol = 'USSCargoPrototypeTech'
WHERE LOWER(commodity) = LOWER('Prototype Tech');
UPDATE commodities
SET symbol = 'Pyrophyllite'
WHERE LOWER(commodity) = LOWER('Pyrophyllite');
UPDATE commodities
SET symbol = 'RadiationBaffle'
WHERE LOWER(commodity) = LOWER('Radiation Baffle');
UPDATE commodities
SET symbol = 'RajukruStoves'
WHERE LOWER(commodity) = LOWER('Rajukru Multi-Stoves');
UPDATE commodities
SET symbol = 'RapaBaoSnakeSkins'
WHERE LOWER(commodity) = LOWER('Rapa Bao Snake Skins');
UPDATE commodities
SET symbol = 'USSCargoRareArtwork'
WHERE LOWER(commodity) = LOWER('Rare Artwork');
UPDATE commodities
SET symbol = 'ReactiveArmour'
WHERE LOWER(commodity) = LOWER('Reactive Armour');
UPDATE commodities
SET symbol = 'USSCargoRebelTransmissions'
WHERE LOWER(commodity) = LOWER('Rebel Transmissions');
UPDATE commodities
SET symbol = 'ReinforcedMountingPlate'
WHERE LOWER(commodity) = LOWER('Reinforced Mounting Plate');
UPDATE commodities
SET symbol = 'ResonatingSeparators'
WHERE LOWER(commodity) = LOWER('Resonating Separators');
UPDATE commodities
SET symbol = 'Rhodplumsite'
WHERE LOWER(commodity) = LOWER('Rhodplumsite');
UPDATE commodities
SET symbol = 'Robotics'
WHERE LOWER(commodity) = LOWER('Robotics');
UPDATE commodities
SET symbol = 'RockforthFertiliser'
WHERE LOWER(commodity) = LOWER('Rockforth Fertiliser');
UPDATE commodities
SET symbol = 'RusaniOldSmokey'
WHERE LOWER(commodity) = LOWER('Rusani Old Smokey');
UPDATE commodities
SET symbol = 'Rutile'
WHERE LOWER(commodity) = LOWER('Rutile');
UPDATE commodities
SET symbol = 'SAP8CoreContainer'
WHERE LOWER(commodity) = LOWER('SAP 8 Core Container');
UPDATE commodities
SET symbol = 'Samarium'
WHERE LOWER(commodity) = LOWER('Samarium');
UPDATE commodities
SET symbol = 'SanumaMEAT'
WHERE LOWER(commodity) = LOWER('Sanuma Decorative Meat');
UPDATE commodities
SET symbol = 'SaxonWine'
WHERE LOWER(commodity) = LOWER('Saxon Wine');
UPDATE commodities
SET symbol = 'ScientificResearch'
WHERE LOWER(commodity) = LOWER('Scientific Research');
UPDATE commodities
SET symbol = 'ScientificSamples'
WHERE LOWER(commodity) = LOWER('Scientific Samples');
UPDATE commodities
SET symbol = 'Scrap'
WHERE LOWER(commodity) = LOWER('Scrap');
UPDATE commodities
SET symbol = 'UnknownRefinedMineral'
WHERE LOWER(commodity) = LOWER('Semi-Refined Spire Mineral');
UPDATE commodities
SET symbol = 'Semiconductors'
WHERE LOWER(commodity) = LOWER('Semiconductors');
UPDATE commodities
SET symbol = 'Serendibite'
WHERE LOWER(commodity) = LOWER('Serendibite');
UPDATE commodities
SET symbol = 'ShansCharisOrchid'
WHERE LOWER(commodity) = LOWER('Shan’s Charis Orchid');
UPDATE commodities
SET symbol = 'Silver'
WHERE LOWER(commodity) = LOWER('Silver');
UPDATE commodities
SET symbol = 'SkimerComponents'
WHERE LOWER(commodity) = LOWER('Skimmer Components');
UPDATE commodities
SET symbol = 'Slaves'
WHERE LOWER(commodity) = LOWER('Slaves');
UPDATE commodities
SET symbol = 'SmallExplorationDataCash'
WHERE LOWER(commodity) = LOWER('Small Survey Data Cache');
UPDATE commodities
SET symbol = 'SoontillRelics'
WHERE LOWER(commodity) = LOWER('Soontill Relics');
UPDATE commodities
SET symbol = 'SothisCrystallineGold'
WHERE LOWER(commodity) = LOWER('Sothis Crystalline Gold');
UPDATE commodities
SET symbol = 'SpacePioneerRelics'
WHERE LOWER(commodity) = LOWER('Space Pioneer Relics');
UPDATE commodities
SET symbol = 'Steel'
WHERE LOWER(commodity) = LOWER('Steel');
UPDATE commodities
SET symbol = 'StructuralRegulators'
WHERE LOWER(commodity) = LOWER('Structural Regulators');
UPDATE commodities
SET symbol = 'Superconductors'
WHERE LOWER(commodity) = LOWER('Superconductors');
UPDATE commodities
SET symbol = 'SurfaceStabilisers'
WHERE LOWER(commodity) = LOWER('Surface Stabilisers');
UPDATE commodities
SET symbol = 'SurvivalEquipment'
WHERE LOWER(commodity) = LOWER('Survival Equipment');
UPDATE commodities
SET symbol = 'SyntheticFabrics'
WHERE LOWER(commodity) = LOWER('Synthetic Fabrics');
UPDATE commodities
SET symbol = 'SyntheticMeat'
WHERE LOWER(commodity) = LOWER('Synthetic Meat');
UPDATE commodities
SET symbol = 'SyntheticReagents'
WHERE LOWER(commodity) = LOWER('Synthetic Reagents');
UPDATE commodities
SET symbol = 'Taaffeite'
WHERE LOWER(commodity) = LOWER('Taaffeite');
UPDATE commodities
SET symbol = 'TacticalData'
WHERE LOWER(commodity) = LOWER('Tactical Data');
UPDATE commodities
SET symbol = 'TanmarkTranquilTea'
WHERE LOWER(commodity) = LOWER('Tanmark Tranquil Tea');
UPDATE commodities
SET symbol = 'Tantalum'
WHERE LOWER(commodity) = LOWER('Tantalum');
UPDATE commodities
SET symbol = 'TarachTorSpice'
WHERE LOWER(commodity) = LOWER('Tarach Spice');
UPDATE commodities
SET symbol = 'TauriChimes'
WHERE LOWER(commodity) = LOWER('Tauri Chimes');
UPDATE commodities
SET symbol = 'Tea'
WHERE LOWER(commodity) = LOWER('Tea');
UPDATE commodities
SET symbol = 'USSCargoTechnicalBlueprints'
WHERE LOWER(commodity) = LOWER('Technical Blueprints');
UPDATE commodities
SET symbol = 'TelemetrySuite'
WHERE LOWER(commodity) = LOWER('Telemetry Suite');
UPDATE commodities
SET symbol = 'TerraMaterBloodBores'
WHERE LOWER(commodity) = LOWER('Terra Mater Blood Bores');
UPDATE commodities
SET symbol = 'Thallium'
WHERE LOWER(commodity) = LOWER('Thallium');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType2'
WHERE LOWER(commodity) = LOWER('Thargoid Basilisk Tissue Sample');
UPDATE commodities
SET symbol = 'UnknownBiologicalMatter'
WHERE LOWER(commodity) = LOWER('Thargoid Biological Matter');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType1'
WHERE LOWER(commodity) = LOWER('Thargoid Cyclops Tissue Sample');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType6'
WHERE LOWER(commodity) = LOWER('Thargoid Glaive Tissue Sample');
UPDATE commodities
SET symbol = 'ThargoidHeart'
WHERE LOWER(commodity) = LOWER('Thargoid Heart');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType4'
WHERE LOWER(commodity) = LOWER('Thargoid Hydra Tissue Sample');
UPDATE commodities
SET symbol = 'UnknownArtifact3'
WHERE LOWER(commodity) = LOWER('Thargoid Link');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType3'
WHERE LOWER(commodity) = LOWER('Thargoid Medusa Tissue Sample');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType5'
WHERE LOWER(commodity) = LOWER('Thargoid Orthrus Tissue Sample');
UPDATE commodities
SET symbol = 'UnknownArtifact2'
WHERE LOWER(commodity) = LOWER('Thargoid Probe');
UPDATE commodities
SET symbol = 'UnknownResin'
WHERE LOWER(commodity) = LOWER('Thargoid Resin');
UPDATE commodities
SET symbol = 'ThargoidScoutTissueSample'
WHERE LOWER(commodity) = LOWER('Thargoid Scout Tissue Sample');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType7'
WHERE LOWER(commodity) = LOWER('Thargoid Scythe Tissue Sample');
UPDATE commodities
SET symbol = 'UnknownArtifact'
WHERE LOWER(commodity) = LOWER('Thargoid Sensor');
UPDATE commodities
SET symbol = 'UnknownTechnologySamples'
WHERE LOWER(commodity) = LOWER('Thargoid Technology Samples');
UPDATE commodities
SET symbol = 'TheHuttonMug'
WHERE LOWER(commodity) = LOWER('The Hutton Mug');
UPDATE commodities
SET symbol = 'WatersOfShintara'
WHERE LOWER(commodity) = LOWER('The Waters Of Shintara');
UPDATE commodities
SET symbol = 'ThermalCoolingUnits'
WHERE LOWER(commodity) = LOWER('Thermal Cooling Units');
UPDATE commodities
SET symbol = 'Thorium'
WHERE LOWER(commodity) = LOWER('Thorium');
UPDATE commodities
SET symbol = 'ThrutisCream'
WHERE LOWER(commodity) = LOWER('Thrutis Cream');
UPDATE commodities
SET symbol = 'TiegfriesSynthSilk'
WHERE LOWER(commodity) = LOWER('Tiegfries Synth Silk');
UPDATE commodities
SET symbol = 'TimeCapsule'
WHERE LOWER(commodity) = LOWER('Time Capsule');
UPDATE commodities
SET symbol = 'TiolceWaste2PasteUnits'
WHERE LOWER(commodity) = LOWER('Tiolce Waste2Paste Units');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType9a'
WHERE LOWER(commodity) = LOWER('Titan Deep Tissue Sample');
UPDATE commodities
SET symbol = 'ThargoidTitanDriveComponent'
WHERE LOWER(commodity) = LOWER('Titan Drive Component');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType10a'
WHERE LOWER(commodity) = LOWER('Titan Maw Deep Tissue Sample');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType10c'
WHERE LOWER(commodity) = LOWER('Titan Maw Partial Tissue Sample');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType10b'
WHERE LOWER(commodity) = LOWER('Titan Maw Tissue Sample');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType9c'
WHERE LOWER(commodity) = LOWER('Titan Partial Tissue Sample');
UPDATE commodities
SET symbol = 'ThargoidTissueSampleType9b'
WHERE LOWER(commodity) = LOWER('Titan Tissue Sample');
UPDATE commodities
SET symbol = 'Titanium'
WHERE LOWER(commodity) = LOWER('Titanium');
UPDATE commodities
SET symbol = 'Tobacco'
WHERE LOWER(commodity) = LOWER('Tobacco');
UPDATE commodities
SET symbol = 'ToxandjiVirocide'
WHERE LOWER(commodity) = LOWER('Toxandji Virocide');
UPDATE commodities
SET symbol = 'ToxicWaste'
WHERE LOWER(commodity) = LOWER('Toxic Waste');
UPDATE commodities
SET symbol = 'USSCargoTradeData'
WHERE LOWER(commodity) = LOWER('Trade Data');
UPDATE commodities
SET symbol = 'TrinketsOfFortune'
WHERE LOWER(commodity) = LOWER('Trinkets of Hidden Fortune');
UPDATE commodities
SET symbol = 'Tritium'
WHERE LOWER(commodity) = LOWER('Tritium');
UPDATE commodities
SET symbol = 'Advert1'
WHERE LOWER(commodity) = LOWER('Ultra-Compact Processor Prototypes');
UPDATE commodities
SET symbol = 'AncientRelicTG'
WHERE LOWER(commodity) = LOWER('Unclassified Relic');
UPDATE commodities
SET symbol = 'UnocuppiedEscapePod'
WHERE LOWER(commodity) = LOWER('Unoccupied Escape Pod');
UPDATE commodities
SET symbol = 'UnstableDataCore'
WHERE LOWER(commodity) = LOWER('Unstable Data Core');
UPDATE commodities
SET symbol = 'Uraninite'
WHERE LOWER(commodity) = LOWER('Uraninite');
UPDATE commodities
SET symbol = 'Uranium'
WHERE LOWER(commodity) = LOWER('Uranium');
UPDATE commodities
SET symbol = 'UszaianTreeGrub'
WHERE LOWER(commodity) = LOWER('Uszaian Tree Grub');
UPDATE commodities
SET symbol = 'UtgaroarMillenialEggs'
WHERE LOWER(commodity) = LOWER('Utgaroar Millennial Eggs');
UPDATE commodities
SET symbol = 'UzumokuLowGWings'
WHERE LOWER(commodity) = LOWER('Uzumoku Low-G Wings');
UPDATE commodities
SET symbol = 'VHerculisBodyRub'
WHERE LOWER(commodity) = LOWER('V Herculis Body Rub');
UPDATE commodities
SET symbol = 'VanayequiRhinoFur'
WHERE LOWER(commodity) = LOWER('Vanayequi Ceratomorpha Fur');
UPDATE commodities
SET symbol = 'VegaSlimWeed'
WHERE LOWER(commodity) = LOWER('Vega Slimweed');
UPDATE commodities
SET symbol = 'VidavantianLace'
WHERE LOWER(commodity) = LOWER('Vidavantian Lace');
UPDATE commodities
SET symbol = 'LFTVoidExtractCoffee'
WHERE LOWER(commodity) = LOWER('Void Extract Coffee');
UPDATE commodities
SET symbol = 'Opal'
WHERE LOWER(commodity) = LOWER('Void Opal');
UPDATE commodities
SET symbol = 'VolkhabBeeDrones'
WHERE LOWER(commodity) = LOWER('Volkhab Bee Drones');
UPDATE commodities
SET symbol = 'Water'
WHERE LOWER(commodity) = LOWER('Water');
UPDATE commodities
SET symbol = 'WaterPurifiers'
WHERE LOWER(commodity) = LOWER('Water Purifiers');
UPDATE commodities
SET symbol = 'WheemeteWheatCakes'
WHERE LOWER(commodity) = LOWER('Wheemete Wheat Cakes');
UPDATE commodities
SET symbol = 'Wine'
WHERE LOWER(commodity) = LOWER('Wine');
UPDATE commodities
SET symbol = 'WitchhaulKobeBeef'
WHERE LOWER(commodity) = LOWER('Witchhaul Kobe Beef');
UPDATE commodities
SET symbol = 'Wolf1301Fesh'
WHERE LOWER(commodity) = LOWER('Wolf Fesh');
UPDATE commodities
SET symbol = 'WreckageComponents'
WHERE LOWER(commodity) = LOWER('Wreckage Components');
UPDATE commodities
SET symbol = 'WulpaHyperboreSystems'
WHERE LOWER(commodity) = LOWER('Wulpa Hyperbore Systems');
UPDATE commodities
SET symbol = 'WuthieloKuFroth'
WHERE LOWER(commodity) = LOWER('Wuthielo Ku Froth');
UPDATE commodities
SET symbol = 'XiheCompanions'
WHERE LOWER(commodity) = LOWER('Xihe Biomorphic Companions');
UPDATE commodities
SET symbol = 'YasoKondiLeaf'
WHERE LOWER(commodity) = LOWER('Yaso Kondi Leaf');
UPDATE commodities
SET symbol = 'ZeesszeAntGlue'
WHERE LOWER(commodity) = LOWER('Zeessze Ant Grub Glue');

-- Renamed by Frontier (Personal Gifts -> Festive Gifts) but symbol unchanged:
UPDATE commodities
SET symbol = 'PersonalGifts'
WHERE LOWER(commodity) = LOWER('Personal Gifts');

-- Present in FDevIDs but missing from the table:
INSERT OR IGNORE INTO commodities (commodity, symbol)
VALUES ('Xenobiological Prison Pod', 'ThargoidPod');
UPDATE commodities
SET symbol = 'ThargoidPod'
WHERE LOWER(commodity) = LOWER('Xenobiological Prison Pod');

CREATE INDEX IF NOT EXISTS idx_commodities_symbol ON commodities (LOWER(symbol));
