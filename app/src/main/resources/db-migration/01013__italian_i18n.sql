-- Add language columns and materialType to material_names

ALTER TABLE material_names
ADD COLUMN name_it TEXT;

-- raw.csv

UPDATE material_names
SET name_it = 'Antimonio'
WHERE LOWER(name) = LOWER('Antimony');

UPDATE material_names
SET name_it = 'Arsenico'
WHERE LOWER(name) = LOWER('Arsenic');

UPDATE material_names
SET name_it = 'Boro'
WHERE LOWER(name) = LOWER('Boron');

UPDATE material_names
SET name_it = 'Cadmio'
WHERE LOWER(name) = LOWER('Cadmium');

UPDATE material_names
SET name_it = 'Carbonio'
WHERE LOWER(name) = LOWER('Carbon');

UPDATE material_names
SET name_it = 'Cromo'
WHERE LOWER(name) = LOWER('Chromium');

UPDATE material_names
SET name_it = 'Germanio'
WHERE LOWER(name) = LOWER('Germanium');

UPDATE material_names
SET name_it = 'Ferro'
WHERE LOWER(name) = LOWER('Iron');

UPDATE material_names
SET name_it = 'Piombo'
WHERE LOWER(name) = LOWER('Lead');

UPDATE material_names
SET name_it = 'Manganese'
WHERE LOWER(name) = LOWER('Manganese');

UPDATE material_names
SET name_it = 'Mercurio'
WHERE LOWER(name) = LOWER('Mercury');

UPDATE material_names
SET name_it = 'Molibdeno'
WHERE LOWER(name) = LOWER('Molybdenum');

UPDATE material_names
SET name_it = 'Nichel'
WHERE LOWER(name) = LOWER('Nickel');

UPDATE material_names
SET name_it = 'Niobio'
WHERE LOWER(name) = LOWER('Niobium');

UPDATE material_names
SET name_it = 'Fosforo'
WHERE LOWER(name) = LOWER('Phosphorus');

UPDATE material_names
SET name_it = 'Polonio'
WHERE LOWER(name) = LOWER('Polonium');

UPDATE material_names
SET name_it = 'Renio'
WHERE LOWER(name) = LOWER('Rhenium');

UPDATE material_names
SET name_it = 'Rutenio'
WHERE LOWER(name) = LOWER('Ruthenium');

UPDATE material_names
SET name_it = 'Selenio'
WHERE LOWER(name) = LOWER('Selenium');

UPDATE material_names
SET name_it = 'Zolfo'
WHERE LOWER(name) = LOWER('Sulphur');

UPDATE material_names
SET name_it = 'Tecnezio'
WHERE LOWER(name) = LOWER('Technetium');

UPDATE material_names
SET name_it = 'Tellurio'
WHERE LOWER(name) = LOWER('Tellurium');

UPDATE material_names
SET name_it = 'Stagno'
WHERE LOWER(name) = LOWER('Tin');

UPDATE material_names
SET name_it = 'Tungsteno'
WHERE LOWER(name) = LOWER('Tungsten');

UPDATE material_names
SET name_it = 'Vanadio'
WHERE LOWER(name) = LOWER('Vanadium');

UPDATE material_names
SET name_it = 'Ittrio'
WHERE LOWER(name) = LOWER('Yttrium');

UPDATE material_names
SET name_it = 'Zinco'
WHERE LOWER(name) = LOWER('Zinc');

UPDATE material_names
SET name_it = 'Zirconio'
WHERE LOWER(name) = LOWER('Zirconium');

-- encoded.csv

UPDATE material_names
SET name_it = 'Analisi del Pattern di Scudo Aberrante'
WHERE LOWER(name) = LOWER('Aberrant Shield Pattern Analysis');

UPDATE material_names
SET name_it = 'Dati Compressi di Emissioni Anomale'
WHERE LOWER(name) = LOWER('Abnormal Compact Emissions Data');

UPDATE material_names
SET name_it = 'Cattura di Cifratori Adattivi'
WHERE LOWER(name) = LOWER('Adaptive Encryptors Capture');

UPDATE material_names
SET name_it = 'Dati di Scansione di Massa Anomali'
WHERE LOWER(name) = LOWER('Anomalous Bulk Scan Data');

UPDATE material_names
SET name_it = 'Telemetria Anomala FSD'
WHERE LOWER(name) = LOWER('Anomalous FSD Telemetry');

UPDATE material_names
SET name_it = 'Echi Atipici di Scie Interrotte'
WHERE LOWER(name) = LOWER('Atypical Disrupted Wake Echoes');

UPDATE material_names
SET name_it = 'Archivi Cifrati Atipici'
WHERE LOWER(name) = LOWER('Atypical Encryption Archives');

UPDATE material_names
SET name_it = 'Banche dati di scansione classificate'
WHERE LOWER(name) = LOWER('Classified Scan Databanks');

UPDATE material_names
SET name_it = 'Frammento di Scansione Classificata'
WHERE LOWER(name) = LOWER('Classified Scan Fragment');

UPDATE material_names
SET name_it = 'Firmware Industriale Compromesso'
WHERE LOWER(name) = LOWER('Cracked Industrial Firmware');

UPDATE material_names
SET name_it = 'Eccezioni Analisi di Scie'
WHERE LOWER(name) = LOWER('Datamined Wake Exceptions');

UPDATE material_names
SET name_it = 'Dati Decifrati di Emissione'
WHERE LOWER(name) = LOWER('Decoded Emission Data');

UPDATE material_names
SET name_it = 'Registrazioni Distorte dei Cicli di Scudo'
WHERE LOWER(name) = LOWER('Distorted Shield Cycle Recordings');

UPDATE material_names
SET name_it = 'Dati di Scansioni Divergenti'
WHERE LOWER(name) = LOWER('Divergent Scan Data');

UPDATE material_names
SET name_it = 'Tratte Iperspaziali Eccentriche'
WHERE LOWER(name) = LOWER('Eccentric Hyperspace Trajectories');

UPDATE material_names
SET name_it = 'Emissioni Cifrate Eccezionali'
WHERE LOWER(name) = LOWER('Exceptional Scrambled Emission Data');

UPDATE material_names
SET name_it = 'Frammento Di Schema del Modulo Guardian'
WHERE LOWER(name) = LOWER('Guardian Module Blueprint Fragment');

UPDATE material_names
SET name_it = 'Frammento di Schema della Nave Guardian'
WHERE LOWER(name) = LOWER('Guardian Vessel Blueprint Fragment');

UPDATE material_names
SET name_it = 'Frammento di Schema Arma Guardian'
WHERE LOWER(name) = LOWER('Guardian Weapon Blueprint Fragment');

UPDATE material_names
SET name_it = 'Analisi Incoerente di Assorbimento degli Scudi'
WHERE LOWER(name) = LOWER('Inconsistent Shield Soak Analysis');

UPDATE material_names
SET name_it = 'Dati Irregolari di Emissione'
WHERE LOWER(name) = LOWER('Irregular Emission Data');

UPDATE material_names
SET name_it = 'Analisi di Onda Energetica Massiva (Thargoid)'
WHERE LOWER(name) = LOWER('Massive Energy Surge Analytics (Thargoid)');

UPDATE material_names
SET name_it = 'Firmware Consumer Modificato'
WHERE LOWER(name) = LOWER('Modified Consumer Firmware');

UPDATE material_names
SET name_it = 'Firmware Embedded Modificato'
WHERE LOWER(name) = LOWER('Modified Embedded Firmware');

UPDATE material_names
SET name_it = 'Chiavi Simmetriche Aperte'
WHERE LOWER(name) = LOWER('Open Symmetric Keys');

UPDATE material_names
SET name_it = 'Dati Obelisco di Tipo Alpha (Guardian)'
WHERE LOWER(name) = LOWER('Pattern Alpha Obelisk Data (Guardian)');

UPDATE material_names
SET name_it = 'Dati Obelisco di Tipo Beta (Guardian)'
WHERE LOWER(name) = LOWER('Pattern Beta Obelisk Data (Guardian)');

UPDATE material_names
SET name_it = 'Dati Obelisco di Tipo Delta (Guardian)'
WHERE LOWER(name) = LOWER('Pattern Delta Obelisk Data (Guardian)');

UPDATE material_names
SET name_it = 'Dati Obelisco di Tipo Epsilon (Guardian)'
WHERE LOWER(name) = LOWER('Pattern Epsilon Obelisk Data (Guardian)');

UPDATE material_names
SET name_it = 'Dati Obelisco di Tipo Gamma (Guardian)'
WHERE LOWER(name) = LOWER('Pattern Gamma Obelisk Data (Guardian)');

UPDATE material_names
SET name_it = 'Dati di Frequenza degli Scudi Peculiari'
WHERE LOWER(name) = LOWER('Peculiar Shield Frequency Data');

UPDATE material_names
SET name_it = 'Patch di Sicurezza Firmware'
WHERE LOWER(name) = LOWER('Security Firmware Patch');

UPDATE material_names
SET name_it = 'Dati di Volo della Nave (Thargoid)'
WHERE LOWER(name) = LOWER('Ship Flight Data (Thargoid)');

UPDATE material_names
SET name_it = 'Dati dei Sistemi della Nave (Thargoid)'
WHERE LOWER(name) = LOWER('Ship Systems Data (Thargoid)');

UPDATE material_names
SET name_it = 'Firmware Legacy Specializzato'
WHERE LOWER(name) = LOWER('Specialised Legacy Firmware');

UPDATE material_names
SET name_it = 'Soluzioni di Scie Anomale'
WHERE LOWER(name) = LOWER('Strange Wake Solutions');

UPDATE material_names
SET name_it = 'Codici di Cifratura Etichettati'
WHERE LOWER(name) = LOWER('Tagged Encryption Codes');

UPDATE material_names
SET name_it = 'Telemetria di Interdizione (Thargoid)'
WHERE LOWER(name) = LOWER('Thargoid Interdiction Telemetry (Thargoid)');

UPDATE material_names
SET name_it = 'Dati sulla Composizione dei Materiali Thargoid'
WHERE LOWER(name) = LOWER('Thargoid Material Composition Data');

UPDATE material_names
SET name_it = 'Dati di Residui Thargoid'
WHERE LOWER(name) = LOWER('Thargoid Residue Data');

UPDATE material_names
SET name_it = 'Firma di Nave Thargoid'
WHERE LOWER(name) = LOWER('Thargoid Ship Signature');

UPDATE material_names
SET name_it = 'Dati Strutturali Thargoid'
WHERE LOWER(name) = LOWER('Thargoid Structural Data');

UPDATE material_names
SET name_it = 'Dati di Scia Thargoid'
WHERE LOWER(name) = LOWER('Thargoid Wake Data');

UPDATE material_names
SET name_it = 'Dati di Emissioni Anomale'
WHERE LOWER(name) = LOWER('Unexpected Emission Data');

UPDATE material_names
SET name_it = 'Archivi di Scansioni Non Identificate'
WHERE LOWER(name) = LOWER('Unidentified Scan Archives');

UPDATE material_names
SET name_it = 'Scansioni di Scudi Anomale'
WHERE LOWER(name) = LOWER('Untypical Shield Scans');

UPDATE material_names
SET name_it = 'File Cifrati Insoliti'
WHERE LOWER(name) = LOWER('Unusual Encrypted Files');

-- manufactured.csv

UPDATE material_names
SET name_it = 'Conduttori di Base'
WHERE LOWER(name) = LOWER('Basic Conductors');

UPDATE material_names
SET name_it = 'Condotti Bio‑Meccanici (Thargoid)'
WHERE LOWER(name) = LOWER('Bio-Mechanical Conduits (Thargoid)');

UPDATE material_names
SET name_it = 'Conduttori Biotecnologici'
WHERE LOWER(name) = LOWER('Biotech Conductors');

UPDATE material_names
SET name_it = 'Cristallo Caustico (Thargoid)'
WHERE LOWER(name) = LOWER('Caustic Crystal (Thargoid)');

UPDATE material_names
SET name_it = 'Scheggia Caustica (Thargoid)'
WHERE LOWER(name) = LOWER('Caustic Shard (Thargoid)');

UPDATE material_names
SET name_it = 'Distilleria Chimica'
WHERE LOWER(name) = LOWER('Chemical Distillery');

UPDATE material_names
SET name_it = 'Manipolatori Chimici'
WHERE LOWER(name) = LOWER('Chemical Manipulators');

UPDATE material_names
SET name_it = 'Processori Chimici'
WHERE LOWER(name) = LOWER('Chemical Processors');

UPDATE material_names
SET name_it = 'Unità di Stoccaggio Chimico'
WHERE LOWER(name) = LOWER('Chemical Storage Units');

UPDATE material_names
SET name_it = 'Compositi Compatti'
WHERE LOWER(name) = LOWER('Compact Composites');

UPDATE material_names
SET name_it = 'Schermature Composite'
WHERE LOWER(name) = LOWER('Compound Shielding');

UPDATE material_names
SET name_it = 'Ceramiche Conduttive'
WHERE LOWER(name) = LOWER('Conductive Ceramics');

UPDATE material_names
SET name_it = 'Componenti Conduttivi'
WHERE LOWER(name) = LOWER('Conductive Components');

UPDATE material_names
SET name_it = 'Polimeri conduttivi'
WHERE LOWER(name) = LOWER('Conductive Polymers');

UPDATE material_names
SET name_it = 'Componenti Configurabili'
WHERE LOWER(name) = LOWER('Configurable Components');

UPDATE material_names
SET name_it = 'Compositi Core Dynamics'
WHERE LOWER(name) = LOWER('Core Dynamics Composites');

UPDATE material_names
SET name_it = 'Meccanismi Corrosivi (Thargoid)'
WHERE LOWER(name) = LOWER('Corrosive Mechanisms (Thargoid)');

UPDATE material_names
SET name_it = 'Schegge Cristalline'
WHERE LOWER(name) = LOWER('Crystal Shards');

UPDATE material_names
SET name_it = 'Array Elettrochimici'
WHERE LOWER(name) = LOWER('Electrochemical Arrays');

UPDATE material_names
SET name_it = 'Cristalli di Focalizzazione Preziosi'
WHERE LOWER(name) = LOWER('Exquisite Focus Crystals');

UPDATE material_names
SET name_it = 'Compositi a Filamenti'
WHERE LOWER(name) = LOWER('Filament Composites');

UPDATE material_names
SET name_it = 'Cristalli di Focalizzazione Difettosi'
WHERE LOWER(name) = LOWER('Flawed Focus Crystals');

UPDATE material_names
SET name_it = 'Cristalli di Focalizzazione'
WHERE LOWER(name) = LOWER('Focus Crystals');

UPDATE material_names
SET name_it = 'Leghe Galvanizzate'
WHERE LOWER(name) = LOWER('Galvanising Alloys');

UPDATE material_names
SET name_it = 'Resistori a Griglia'
WHERE LOWER(name) = LOWER('Grid Resistors');

UPDATE material_names
SET name_it = 'Cella di Energia Guardian'
WHERE LOWER(name) = LOWER('Guardian Power Cell');

UPDATE material_names
SET name_it = 'Condotto di Energia Guardian'
WHERE LOWER(name) = LOWER('Guardian Power Conduit');

UPDATE material_names
SET name_it = 'Componenti Arma della Sentinella Guardian'
WHERE LOWER(name) = LOWER('Guardian Sentinel Weapon Parts');

UPDATE material_names
SET name_it = 'Componente Tecnologico Guardian'
WHERE LOWER(name) = LOWER('Guardian Technology Component');

UPDATE material_names
SET name_it = 'Componenti dei Relitti Guardian'
WHERE LOWER(name) = LOWER('Guardian Wreckage Components');

UPDATE material_names
SET name_it = 'Frammenti di Superficie Indurita'
WHERE LOWER(name) = LOWER('Hardened Surface Fragments');

UPDATE material_names
SET name_it = 'Cablaggio Conduttivo Termico'
WHERE LOWER(name) = LOWER('Heat Conduction Wiring');

UPDATE material_names
SET name_it = 'Piastra di Dispersione Termica'
WHERE LOWER(name) = LOWER('Heat Dispersion Plate');

UPDATE material_names
SET name_it = 'Scambiatori di Calore'
WHERE LOWER(name) = LOWER('Heat Exchangers');

UPDATE material_names
SET name_it = 'Campione di Esposizione Termica'
WHERE LOWER(name) = LOWER('Heat Exposure Specimen');

UPDATE material_names
SET name_it = 'Ceramiche Resistenti al Calore'
WHERE LOWER(name) = LOWER('Heat Resistant Ceramics');

UPDATE material_names
SET name_it = 'Alette di Dispersione Termica'
WHERE LOWER(name) = LOWER('Heat Vanes');

UPDATE material_names
SET name_it = 'Compositi ad Alta Densità'
WHERE LOWER(name) = LOWER('High Density Composites');

UPDATE material_names
SET name_it = 'Capacitori Ibridi'
WHERE LOWER(name) = LOWER('Hybrid Capacitors');

UPDATE material_names
SET name_it = 'Schermatura Imperiale'
WHERE LOWER(name) = LOWER('Imperial Shielding');

UPDATE material_names
SET name_it = 'Componenti Improvvisati'
WHERE LOWER(name) = LOWER('Improvised Components');

UPDATE material_names
SET name_it = 'Componenti Meccanici'
WHERE LOWER(name) = LOWER('Mechanical Components');

UPDATE material_names
SET name_it = 'Equipaggiamento Meccanico'
WHERE LOWER(name) = LOWER('Mechanical Equipment');

UPDATE material_names
SET name_it = 'Rottami Meccanici'
WHERE LOWER(name) = LOWER('Mechanical Scrap');

UPDATE material_names
SET name_it = 'Leghe di Grado Militare'
WHERE LOWER(name) = LOWER('Military Grade Alloys');

UPDATE material_names
SET name_it = 'Supercapacitori Militari'
WHERE LOWER(name) = LOWER('Military Supercapacitors');

UPDATE material_names
SET name_it = 'Isolatori Farmaceutici'
WHERE LOWER(name) = LOWER('Pharmaceutical Isolators');

UPDATE material_names
SET name_it = 'Leghe Fase'
WHERE LOWER(name) = LOWER('Phase Alloys');

UPDATE material_names
SET name_it = 'Residuo di Membrana Fase'
WHERE LOWER(name) = LOWER('Phasing Membrane Residue');

UPDATE material_names
SET name_it = 'Capacitori Polimerici'
WHERE LOWER(name) = LOWER('Polymer Capacitors');

UPDATE material_names
SET name_it = 'Leghe Precipitate'
WHERE LOWER(name) = LOWER('Precipitated Alloys');

UPDATE material_names
SET name_it = 'Compositi Proprietari'
WHERE LOWER(name) = LOWER('Proprietary Composites');

UPDATE material_names
SET name_it = 'Elementi di Propulsione (Thargoid)'
WHERE LOWER(name) = LOWER('Propulsion Elements (Thargoid)');

UPDATE material_names
SET name_it = 'Radiatori Proto‑Termici'
WHERE LOWER(name) = LOWER('Proto Heat Radiators');

UPDATE material_names
SET name_it = 'Leghe Proto‑Luminose'
WHERE LOWER(name) = LOWER('Proto Light Alloys');

UPDATE material_names
SET name_it = 'Leghe Proto‑Radioliche'
WHERE LOWER(name) = LOWER('Proto Radiolic Alloys');

UPDATE material_names
SET name_it = 'Cristalli Focali Raffinati'
WHERE LOWER(name) = LOWER('Refined Focus Crystals');

UPDATE material_names
SET name_it = 'Leghe Recuperate'
WHERE LOWER(name) = LOWER('Salvaged Alloys');

UPDATE material_names
SET name_it = 'Frammento Sensore'
WHERE LOWER(name) = LOWER('Sensor Fragment');

UPDATE material_names
SET name_it = 'Emettitori di Scudo'
WHERE LOWER(name) = LOWER('Shield Emitters');

UPDATE material_names
SET name_it = 'Sensori di Schermatura'
WHERE LOWER(name) = LOWER('Shielding Sensors');

UPDATE material_names
SET name_it = 'Chip Nucleo Tattico'
WHERE LOWER(name) = LOWER('Tactical Core Chip');

UPDATE material_names
SET name_it = 'Leghe Temprate'
WHERE LOWER(name) = LOWER('Tempered Alloys');

UPDATE material_names
SET name_it = 'Carapace Thargoid'
WHERE LOWER(name) = LOWER('Thargoid Carapace');

UPDATE material_names
SET name_it = 'Cella di Energia Thargoid'
WHERE LOWER(name) = LOWER('Thargoid Energy Cell');

UPDATE material_names
SET name_it = 'Circuiti Organici Thargoid'
WHERE LOWER(name) = LOWER('Thargoid Organic Circuitry');

UPDATE material_names
SET name_it = 'Componenti Tecnologici Thargoid'
WHERE LOWER(name) = LOWER('Thargoid Technological Components');

UPDATE material_names
SET name_it = 'Leghe Termiche'
WHERE LOWER(name) = LOWER('Thermic Alloys');

UPDATE material_names
SET name_it = 'Parti Arma (Thargoid)'
WHERE LOWER(name) = LOWER('Weapon Parts (Thargoid)');

UPDATE material_names
SET name_it = 'Emettitori di Scudo Usurati'
WHERE LOWER(name) = LOWER('Worn Shield Emitters');

UPDATE material_names
SET name_it = 'Componenti di Relitto (Thargoid)'
WHERE LOWER(name) = LOWER('Wreckage Components (Thargoid)');

UPDATE material_names
SET name_it = 'Sconosciuto'
WHERE LOWER(name) = LOWER('Unknown');


-- Add language columns to commodities
ALTER TABLE commodities
ADD COLUMN commodity_it TEXT;

-- commodity.csv

UPDATE commodities
SET commodity_it = 'Catalizzatori Avanzati'
WHERE LOWER(commodity) = LOWER('Advanced Catalysers');

UPDATE commodities
SET commodity_it = 'Medicinali Avanzati'
WHERE LOWER(commodity) = LOWER('Advanced Medicines');

UPDATE commodities
SET commodity_it = 'Trattamento agronomico'
WHERE LOWER(commodity) = LOWER('Agronomic Treatment');

UPDATE commodities
SET commodity_it = 'Reliquie IA'
WHERE LOWER(commodity) = LOWER('AI Relics');

UPDATE commodities
SET commodity_it = 'Monitor per animali'
WHERE LOWER(commodity) = LOWER('Animal Monitors');

UPDATE commodities
SET commodity_it = 'Prototipi di processori ultra-compatti'
WHERE LOWER(commodity) = LOWER('Ultra-Compact Processor Prototypes');

UPDATE commodities
SET commodity_it = 'Mele Eden di Aerial'
WHERE LOWER(commodity) = LOWER('Eden Apples Of Aerial');

UPDATE commodities
SET commodity_it = 'Stimolante Aganippe'
WHERE LOWER(commodity) = LOWER('Aganippe Rush');

UPDATE commodities
SET commodity_it = 'Medicinali agricoli'
WHERE LOWER(commodity) = LOWER('Agri-Medicines');



UPDATE commodities
SET commodity_it = 'Materiali Mediatici Aisling'
WHERE LOWER(commodity) = LOWER('Aisling Media Materials');

UPDATE commodities
SET commodity_it = 'Contratti Sigillati Aisling'
WHERE LOWER(commodity) = LOWER('Aisling Sealed Contracts');

UPDATE commodities
SET commodity_it = 'Materiali del Programma Aisling'
WHERE LOWER(commodity) = LOWER('Aisling Programme Materials');

UPDATE commodities
SET commodity_it = 'Arte Cutanea Alacarakmo'
WHERE LOWER(commodity) = LOWER('Alacarakmo Skin Art');

UPDATE commodities
SET commodity_it = 'Carne di Mammut Albino Quechua'
WHERE LOWER(commodity) = LOWER('Albino Quechua Mammoth Meat');

UPDATE commodities
SET commodity_it = 'Alessandrite'
WHERE LOWER(commodity) = LOWER('Alexandrite');

UPDATE commodities
SET commodity_it = 'Alghe'
WHERE LOWER(commodity) = LOWER('Algae');

UPDATE commodities
SET commodity_it = 'Uova Coriacee'
WHERE LOWER(commodity) = LOWER('Leathery Eggs');

UPDATE commodities
SET commodity_it = 'Contratti Legislativi dell’Alleanza'
WHERE LOWER(commodity) = LOWER('Alliance Legislative Contracts');

UPDATE commodities
SET commodity_it = 'Registri Legislativi dell’Alleanza'
WHERE LOWER(commodity) = LOWER('Alliance Legislative Records');

UPDATE commodities
SET commodity_it = 'Accordi Commerciali dell’Alleanza'
WHERE LOWER(commodity) = LOWER('Alliance Trade Agreements');

UPDATE commodities
SET commodity_it = 'Pelle Altairiana'
WHERE LOWER(commodity) = LOWER('Altairian Skin');

UPDATE commodities
SET commodity_it = 'Alluminio'
WHERE LOWER(commodity) = LOWER('Aluminium');

UPDATE commodities
SET commodity_it = 'Sapone Corpo di Alya'
WHERE LOWER(commodity) = LOWER('Alya Body Soap');

UPDATE commodities
SET commodity_it = 'Scrigno dei Guardians'
WHERE LOWER(commodity) = LOWER('Guardian Casket');

UPDATE commodities
SET commodity_it = 'Chiave antica'
WHERE LOWER(commodity) = LOWER('Ancient Key (Guardian)');

UPDATE commodities
SET commodity_it = 'Globo dei Guardians'
WHERE LOWER(commodity) = LOWER('Guardian Orb');

UPDATE commodities
SET commodity_it = 'Reliquia dei Guardians'
WHERE LOWER(commodity) = LOWER('Guardian Relic');

UPDATE commodities
SET commodity_it = 'Reliquia non classificata'
WHERE LOWER(commodity) = LOWER('Unclassified Relic');

UPDATE commodities
SET commodity_it = 'Tavoletta dei Guardians'
WHERE LOWER(commodity) = LOWER('Guardian Tablet');

UPDATE commodities
SET commodity_it = 'Totem dei Guardians'
WHERE LOWER(commodity) = LOWER('Guardian Totem');

UPDATE commodities
SET commodity_it = 'Urna dei Guardians'
WHERE LOWER(commodity) = LOWER('Guardian Urn');

UPDATE commodities
SET commodity_it = 'Fuochi d’Artificio di Anduliga'
WHERE LOWER(commodity) = LOWER('Anduliga Fire Works');

UPDATE commodities
SET commodity_it = 'Silver Fesh di Crom'
WHERE LOWER(commodity) = LOWER('Crom Silver Fesh');

UPDATE commodities
SET commodity_it = 'Carne Animale'
WHERE LOWER(commodity) = LOWER('Animal Meat');

UPDATE commodities
SET commodity_it = 'Unità di Contenimento Antimateria'
WHERE LOWER(commodity) = LOWER('Antimatter Containment Unit');

UPDATE commodities
SET commodity_it = 'Gioielli antichi'
WHERE LOWER(commodity) = LOWER('Antique Jewellery');

UPDATE commodities
SET commodity_it = 'Antichità'
WHERE LOWER(commodity) = LOWER('Antiquities');

UPDATE commodities
SET commodity_it = 'Caffè Any Na'
WHERE LOWER(commodity) = LOWER('Any Na Coffee');

UPDATE commodities
SET commodity_it = 'Apa Vietii'
WHERE LOWER(commodity) = LOWER('Apa Vietii');

UPDATE commodities
SET commodity_it = 'Sistemi Acquaponici'
WHERE LOWER(commodity) = LOWER('Aquaponic Systems');

UPDATE commodities
SET commodity_it = 'Dolci Conventuali di Arouca'
WHERE LOWER(commodity) = LOWER('Arouca Conventual Sweets');

UPDATE commodities
SET commodity_it = 'Motori Articolati'
WHERE LOWER(commodity) = LOWER('Articulation Motors');

UPDATE commodities
SET commodity_it = 'Piani d’assalto'
WHERE LOWER(commodity) = LOWER('Assault Plans');

UPDATE commodities
SET commodity_it = 'Processori atmosferici'
WHERE LOWER(commodity) = LOWER('Atmospheric Processors');

UPDATE commodities
SET commodity_it = 'Auto‑Costruttori'
WHERE LOWER(commodity) = LOWER('Auto-Fabricators');

UPDATE commodities
SET commodity_it = 'Formula 42 di AZ Cancri'
WHERE LOWER(commodity) = LOWER('AZ Cancri Formula 42');

UPDATE commodities
SET commodity_it = 'Greebles al forno'
WHERE LOWER(commodity) = LOWER('Baked Greebles');

UPDATE commodities
SET commodity_it = 'Baltah’sine Vacuum Krill'
WHERE LOWER(commodity) = LOWER('Baltah’sine Vacuum Krill');

UPDATE commodities
SET commodity_it = 'Cuoio Anfibio di Banki'
WHERE LOWER(commodity) = LOWER('Banki Amphibious Leather');

UPDATE commodities
SET commodity_it = 'Medicinali di Base'
WHERE LOWER(commodity) = LOWER('Basic Medicines');

UPDATE commodities
SET commodity_it = 'Narcotici'
WHERE LOWER(commodity) = LOWER('Narcotics');

UPDATE commodities
SET commodity_it = 'Gin al Serpente Bast'
WHERE LOWER(commodity) = LOWER('Bast Snake Gin');

UPDATE commodities
SET commodity_it = 'Armi da battaglia'
WHERE LOWER(commodity) = LOWER('Battle Weapons');

UPDATE commodities
SET commodity_it = 'Bauxite'
WHERE LOWER(commodity) = LOWER('Bauxite');

UPDATE commodities
SET commodity_it = 'Birra'
WHERE LOWER(commodity) = LOWER('Beer');

UPDATE commodities
SET commodity_it = 'Cuoio di Razza Belalans'
WHERE LOWER(commodity) = LOWER('Belalans Ray Leather');

UPDATE commodities
SET commodity_it = 'Benitoite'
WHERE LOWER(commodity) = LOWER('Benitoite');

UPDATE commodities
SET commodity_it = 'Bertrandite'
WHERE LOWER(commodity) = LOWER('Bertrandite');

UPDATE commodities
SET commodity_it = 'Berillio'
WHERE LOWER(commodity) = LOWER('Beryllium');

UPDATE commodities
SET commodity_it = 'Lichene Bioreducente'
WHERE LOWER(commodity) = LOWER('Bioreducing Lichen');

UPDATE commodities
SET commodity_it = 'Rifiuti biologici'
WHERE LOWER(commodity) = LOWER('Biowaste');

UPDATE commodities
SET commodity_it = 'Bismuto'
WHERE LOWER(commodity) = LOWER('Bismuth');

UPDATE commodities
SET commodity_it = 'Latte Azzurro'
WHERE LOWER(commodity) = LOWER('Azure Milk');

UPDATE commodities
SET commodity_it = 'Liquore di contrabbando'
WHERE LOWER(commodity) = LOWER('Bootleg Liquor');

UPDATE commodities
SET commodity_it = 'Patogenetica Borasetani'
WHERE LOWER(commodity) = LOWER('Borasetani Pathogenetics');

UPDATE commodities
SET commodity_it = 'Bromellite'
WHERE LOWER(commodity) = LOWER('Bromellite');

UPDATE commodities
SET commodity_it = 'Sottobicchieri Birra Buckyball'
WHERE LOWER(commodity) = LOWER('Buckyball Beer Mats');

UPDATE commodities
SET commodity_it = 'Costruttori Edilizi'
WHERE LOWER(commodity) = LOWER('Building Fabricators');

UPDATE commodities
SET commodity_it = 'Distillato di Bile Burnham'
WHERE LOWER(commodity) = LOWER('Burnham Bile Distillate');

UPDATE commodities
SET commodity_it = 'Caffè CD-75 Kitten Brand'
WHERE LOWER(commodity) = LOWER('CD-75 Kitten Brand Coffee');

UPDATE commodities
SET commodity_it = 'Mega Gin di Centauri'
WHERE LOWER(commodity) = LOWER('Centauri Mega Gin');

UPDATE commodities
SET commodity_it = 'Compositi ceramici'
WHERE LOWER(commodity) = LOWER('Ceramic Composites');

UPDATE commodities
SET commodity_it = 'Tè cerimoniale Heike'
WHERE LOWER(commodity) = LOWER('Ceremonial Heike Tea');

UPDATE commodities
SET commodity_it = 'Uovo di Aepyornis'
WHERE LOWER(commodity) = LOWER('Aepyornis Egg');

UPDATE commodities
SET commodity_it = 'Conigli di Ceti'
WHERE LOWER(commodity) = LOWER('Ceti Rabbits');

UPDATE commodities
SET commodity_it = 'Tessuto Camaleonte'
WHERE LOWER(commodity) = LOWER('Chameleon Cloth');

UPDATE commodities
SET commodity_it = 'Chateau de Aegaeon'
WHERE LOWER(commodity) = LOWER('Chateau De Aegaeon');

UPDATE commodities
SET commodity_it = 'Sostanze Chimiche'
WHERE LOWER(commodity) = LOWER('Chemicals');

UPDATE commodities
SET commodity_it = 'Rifiuti Chimici'
WHERE LOWER(commodity) = LOWER('Chemical Waste');

UPDATE commodities
SET commodity_it = 'Cristalli di Sangue Cherbones'
WHERE LOWER(commodity) = LOWER('Cherbones Blood Crystals');

UPDATE commodities
SET commodity_it = 'Pasta Marina di Chi Eridani'
WHERE LOWER(commodity) = LOWER('Chi Eridani Marine Paste');

UPDATE commodities
SET commodity_it = 'Equipaggiamento Sperimentale Classificato'
WHERE LOWER(commodity) = LOWER('Classified Experimental Equipment');

UPDATE commodities
SET commodity_it = 'Vestiti'
WHERE LOWER(commodity) = LOWER('Clothing');

UPDATE commodities
SET commodity_it = 'Composito CMM'
WHERE LOWER(commodity) = LOWER('CMM Composite');

UPDATE commodities
SET commodity_it = 'Cobalto'
WHERE LOWER(commodity) = LOWER('Cobalt');

UPDATE commodities
SET commodity_it = 'Caffè'
WHERE LOWER(commodity) = LOWER('Coffee');

UPDATE commodities
SET commodity_it = 'Coltan'
WHERE LOWER(commodity) = LOWER('Coltan');

UPDATE commodities
SET commodity_it = 'Stabilizzatori da combattimento'
WHERE LOWER(commodity) = LOWER('Combat Stabilisers');

UPDATE commodities
SET commodity_it = 'Campioni commerciali'
WHERE LOWER(commodity) = LOWER('Commercial Samples');

UPDATE commodities
SET commodity_it = 'Componenti per computer'
WHERE LOWER(commodity) = LOWER('Computer Components');

UPDATE commodities
SET commodity_it = 'Tessuti conduttivi'
WHERE LOWER(commodity) = LOWER('Conductive Fabrics');

UPDATE commodities
SET commodity_it = 'Tecnologia di consumo'
WHERE LOWER(commodity) = LOWER('Consumer Technology');

UPDATE commodities
SET commodity_it = 'Tubi di raffreddamento micro-intrecciati'
WHERE LOWER(commodity) = LOWER('Micro-weave Cooling Hoses');

UPDATE commodities
SET commodity_it = 'Rame'
WHERE LOWER(commodity) = LOWER('Copper');

UPDATE commodities
SET commodity_it = 'Viveri Spongiformi di Coquim'
WHERE LOWER(commodity) = LOWER('Coquim Spongiform Victuals');

UPDATE commodities
SET commodity_it = 'Linfa Corallina'
WHERE LOWER(commodity) = LOWER('Coral Sap');

UPDATE commodities
SET commodity_it = 'Rifornimenti Rivoluzionari'
WHERE LOWER(commodity) = LOWER('Revolutionary supplies');

UPDATE commodities
SET commodity_it = 'Mietitrici Agricole'
WHERE LOWER(commodity) = LOWER('Crop Harvesters');

UPDATE commodities
SET commodity_it = 'Criolite'
WHERE LOWER(commodity) = LOWER('Cryolite');

UPDATE commodities
SET commodity_it = 'Sfere cristalline'
WHERE LOWER(commodity) = LOWER('Crystalline Spheres');

UPDATE commodities
SET commodity_it = 'Capsula di salvataggio danneggiata'
WHERE LOWER(commodity) = LOWER('Damaged Escape Pod');

UPDATE commodities
SET commodity_it = 'Carapaci di Damna'
WHERE LOWER(commodity) = LOWER('Damna Carapaces');

UPDATE commodities
SET commodity_it = 'Nucleo dati'
WHERE LOWER(commodity) = LOWER('Data Core');

UPDATE commodities
SET commodity_it = 'Palme di Delta Phoenicis'
WHERE LOWER(commodity) = LOWER('Delta Phoenicis Palms');

UPDATE commodities
SET commodity_it = 'Tartufo di Deuringas'
WHERE LOWER(commodity) = LOWER('Deuringas Truffles');

UPDATE commodities
SET commodity_it = 'Sensore diagnostico hardware'
WHERE LOWER(commodity) = LOWER('Hardware Diagnostic Sensor');

UPDATE commodities
SET commodity_it = 'Valigia diplomatica'
WHERE LOWER(commodity) = LOWER('Diplomatic Bag');

UPDATE commodities
SET commodity_it = 'Mais Ma di Diso'
WHERE LOWER(commodity) = LOWER('Diso Ma Corn');

UPDATE commodities
SET commodity_it = 'Elettrodomestici'
WHERE LOWER(commodity) = LOWER('Domestic Appliances');

UPDATE commodities
SET commodity_it = 'Limpets'
WHERE LOWER(commodity) = LOWER('Limpets');

UPDATE commodities
SET commodity_it = 'Duradrives'
WHERE LOWER(commodity) = LOWER('Duradrives');

UPDATE commodities
SET commodity_it = 'Reliquie della Terra'
WHERE LOWER(commodity) = LOWER('Earth Relics');

UPDATE commodities
SET commodity_it = 'Termali di Eleu'
WHERE LOWER(commodity) = LOWER('Eleu Thermals');

UPDATE commodities
SET commodity_it = 'Celle di energia di emergenza'
WHERE LOWER(commodity) = LOWER('Emergency Power Cells');

UPDATE commodities
SET commodity_it = 'Archivio dati cifrato'
WHERE LOWER(commodity) = LOWER('Encrypted Data Storage');

UPDATE commodities
SET commodity_it = 'Corrispondenza cifrata'
WHERE LOWER(commodity) = LOWER('Encrypted Correspondence');

UPDATE commodities
SET commodity_it = 'Whisky perlaceo di Eranin'
WHERE LOWER(commodity) = LOWER('Eranin Pearl Whisky');

UPDATE commodities
SET commodity_it = 'Ombrelli di Eshu'
WHERE LOWER(commodity) = LOWER('Eshu Umbrellas');

UPDATE commodities
SET commodity_it = 'Caviale di Esuseku'
WHERE LOWER(commodity) = LOWER('Esuseku Caviar');

UPDATE commodities
SET commodity_it = 'Germogli di tè di Ethgreze'
WHERE LOWER(commodity) = LOWER('Ethgreze Tea Buds');

UPDATE commodities
SET commodity_it = 'Rifugio per evacuazione'
WHERE LOWER(commodity) = LOWER('Evacuation Shelter');

UPDATE commodities
SET commodity_it = 'Collettore di Scarico'
WHERE LOWER(commodity) = LOWER('Exhaust Manifold');

UPDATE commodities
SET commodity_it = 'Esplosivi'
WHERE LOWER(commodity) = LOWER('Explosives');

UPDATE commodities
SET commodity_it = 'Aiuti Federali Liberali'
WHERE LOWER(commodity) = LOWER('Liberal Federal Aid');

UPDATE commodities
SET commodity_it = 'Pacchi Federali Liberali'
WHERE LOWER(commodity) = LOWER('Liberal Federal Packages');

UPDATE commodities
SET commodity_it = 'Pesce'
WHERE LOWER(commodity) = LOWER('Fish');

UPDATE commodities
SET commodity_it = 'Cartucce alimentari'
WHERE LOWER(commodity) = LOWER('Food Cartridges');

UPDATE commodities
SET commodity_it = 'Resti fossili'
WHERE LOWER(commodity) = LOWER('Fossil Remnants');

UPDATE commodities
SET commodity_it = 'Frutta e verdura'
WHERE LOWER(commodity) = LOWER('Fruit and Vegetables');

UPDATE commodities
SET commodity_it = ' Tè di Fujin'
WHERE LOWER(commodity) = LOWER('Fujin Tea');

UPDATE commodities
SET commodity_it = 'Guida Turistica Galattica'
WHERE LOWER(commodity) = LOWER('Galactic Travel Guide');

UPDATE commodities
SET commodity_it = 'Gallite'
WHERE LOWER(commodity) = LOWER('Gallite');

UPDATE commodities
SET commodity_it = 'Gallio'
WHERE LOWER(commodity) = LOWER('Gallium');

UPDATE commodities
SET commodity_it = 'Polvere da danza Geawen'
WHERE LOWER(commodity) = LOWER('Geawen Dance Dust');

UPDATE commodities
SET commodity_it = 'Banca genetica'
WHERE LOWER(commodity) = LOWER('Gene Bank');

UPDATE commodities
SET commodity_it = 'Attrezzatura geologica'
WHERE LOWER(commodity) = LOWER('Geological Equipment');

UPDATE commodities
SET commodity_it = 'Campioni geologici'
WHERE LOWER(commodity) = LOWER('Geological Samples');

UPDATE commodities
SET commodity_it = 'Birra Gueuze Gerasiana'
WHERE LOWER(commodity) = LOWER('Gerasian Gueuze Beer');

UPDATE commodities
SET commodity_it = 'Lumache giganti di Irukama'
WHERE LOWER(commodity) = LOWER('Giant Irukama Snails');

UPDATE commodities
SET commodity_it = 'Verrix Giganti'
WHERE LOWER(commodity) = LOWER('Giant Verrix');

UPDATE commodities
SET commodity_it = 'Armi d’autore Gilya'
WHERE LOWER(commodity) = LOWER('Gilya Signature Weapons');

UPDATE commodities
SET commodity_it = 'Oro'
WHERE LOWER(commodity) = LOWER('Gold');

-- commodities.csv


UPDATE commodities
SET commodity_it = 'Caffè Yaupon di Goman'
WHERE LOWER(commodity) = LOWER('Goman Yaupon Coffee');

UPDATE commodities
SET commodity_it = 'Goslarite'
WHERE LOWER(commodity) = LOWER('Goslarite');

UPDATE commodities
SET commodity_it = 'Grano'
WHERE LOWER(commodity) = LOWER('Grain');

UPDATE commodities
SET commodity_it = 'Grandidierite'
WHERE LOWER(commodity) = LOWER('Grandidierite');

UPDATE commodities
SET commodity_it = 'Controspionaggio Grom'
WHERE LOWER(commodity) = LOWER('Grom Counter Intelligence');

UPDATE commodities
SET commodity_it = 'Rifornimenti Militari di Yuri Grom'
WHERE LOWER(commodity) = LOWER('Yuri Grom’s Military Supplies');

UPDATE commodities
SET commodity_it = 'Ematite'
WHERE LOWER(commodity) = LOWER('Haematite');

UPDATE commodities
SET commodity_it = 'Afnio 178'
WHERE LOWER(commodity) = LOWER('Hafnium 178');

UPDATE commodities
SET commodity_it = 'Infuso Nero Haiden'
WHERE LOWER(commodity) = LOWER('Haiden Black Brew');

UPDATE commodities
SET commodity_it = 'Rum del Mare d’Argento di Harma'
WHERE LOWER(commodity) = LOWER('Harma Silver Sea Rum');

UPDATE commodities
SET commodity_it = 'Acchiappasogni Havasupai'
WHERE LOWER(commodity) = LOWER('Havasupai Dream Catcher');

UPDATE commodities
SET commodity_it = 'Tute H.E.'
WHERE LOWER(commodity) = LOWER('H.E. Suits');

UPDATE commodities
SET commodity_it = 'Collegamento Dissipatore'
WHERE LOWER(commodity) = LOWER('Heatsink Interlink');

UPDATE commodities
SET commodity_it = 'Forni Microbici'
WHERE LOWER(commodity) = LOWER('Microbial Furnaces');

UPDATE commodities
SET commodity_it = 'Perle di Helvetitj'
WHERE LOWER(commodity) = LOWER('Helvetitj Pearls');

UPDATE commodities
SET commodity_it = 'Carne selvatica di HIP 10175'
WHERE LOWER(commodity) = LOWER('HIP 10175 Bush Meat');

UPDATE commodities
SET commodity_it = 'Proto Calamaro di HIP'
WHERE LOWER(commodity) = LOWER('HIP Proto-Squid');

UPDATE commodities
SET commodity_it = 'Sciame di HIP 118311'
WHERE LOWER(commodity) = LOWER('HIP 118311 Swarm');

UPDATE commodities
SET commodity_it = 'Organofosfati di HIP'
WHERE LOWER(commodity) = LOWER('Hip Organophosphates');

UPDATE commodities
SET commodity_it = 'Supporto antiurto HN'
WHERE LOWER(commodity) = LOWER('HN Shock Mount');

UPDATE commodities
SET commodity_it = 'Lame da duello Holva'
WHERE LOWER(commodity) = LOWER('Holva Duelling Blades');

UPDATE commodities
SET commodity_it = 'Pillole della verità'
WHERE LOWER(commodity) = LOWER('Honesty Pills');

UPDATE commodities
SET commodity_it = 'Ostaggi'
WHERE LOWER(commodity) = LOWER('Hostages');

UPDATE commodities
SET commodity_it = 'Grano di HR 7221'
WHERE LOWER(commodity) = LOWER('HR 7221 Wheat');

UPDATE commodities
SET commodity_it = 'Carburante a Idrogeno'
WHERE LOWER(commodity) = LOWER('Hydrogen Fuel');

UPDATE commodities
SET commodity_it = 'Perossido di Idrogeno'
WHERE LOWER(commodity) = LOWER('Hydrogen Peroxide');

UPDATE commodities
SET commodity_it = 'Pacco di Contrabbando Kumo'
WHERE LOWER(commodity) = LOWER('Kumo Contraband Package');

UPDATE commodities
SET commodity_it = 'Prigionieri Politici Torval'
WHERE LOWER(commodity) = LOWER('Torval Political Prisoners');

UPDATE commodities
SET commodity_it = 'Schiavi Imperiali'
WHERE LOWER(commodity) = LOWER('Imperial Slaves');

UPDATE commodities
SET commodity_it = 'Bourbon di Indi'
WHERE LOWER(commodity) = LOWER('Indi Bourbon');

UPDATE commodities
SET commodity_it = 'Indite'
WHERE LOWER(commodity) = LOWER('Indite');

UPDATE commodities
SET commodity_it = 'Indio'
WHERE LOWER(commodity) = LOWER('Indium');

UPDATE commodities
SET commodity_it = 'Membrana Isolante'
WHERE LOWER(commodity) = LOWER('Insulating Membrane');

UPDATE commodities
SET commodity_it = 'Distributore Ionico'
WHERE LOWER(commodity) = LOWER('Ion Distributor');

UPDATE commodities
SET commodity_it = 'Giadeite'
WHERE LOWER(commodity) = LOWER('Jadeite');

UPDATE commodities
SET commodity_it = 'Distillatore Quinentian di Jaques'
WHERE LOWER(commodity) = LOWER('Jaques Quinentian Still');

UPDATE commodities
SET commodity_it = 'Scatola Rompicapo di Jaradharre'
WHERE LOWER(commodity) = LOWER('Jaradharre Puzzle Box');

UPDATE commodities
SET commodity_it = 'Riso di Jaroua'
WHERE LOWER(commodity) = LOWER('Jaroua Rice');

UPDATE commodities
SET commodity_it = 'Jotun Mookah'
WHERE LOWER(commodity) = LOWER('Jotun Mookah');

UPDATE commodities
SET commodity_it = 'Sanguisughe Filtranti di Kachirigin'
WHERE LOWER(commodity) = LOWER('Kachirigin Filter Leeches');

UPDATE commodities
SET commodity_it = 'Rifornimenti di Aiuto Kaine'
WHERE LOWER(commodity) = LOWER('Kaine Aid Supplies');

UPDATE commodities
SET commodity_it = 'Materiali di Lobbying Kaine'
WHERE LOWER(commodity) = LOWER('Kaine Lobbying Materials');

UPDATE commodities
SET commodity_it = 'Disinformazione Kaine'
WHERE LOWER(commodity) = LOWER('Kaine Misinformation');

UPDATE commodities
SET commodity_it = 'Sigari Kamitra'
WHERE LOWER(commodity) = LOWER('Kamitra Cigars');

UPDATE commodities
SET commodity_it = 'Armi Storiche di Kamorin'
WHERE LOWER(commodity) = LOWER('Kamorin Historic Weapons');

UPDATE commodities
SET commodity_it = 'Couture di Karetii'
WHERE LOWER(commodity) = LOWER('Karetii Couture');

UPDATE commodities
SET commodity_it = 'Locuste di Karsuki'
WHERE LOWER(commodity) = LOWER('Karsuki Locusts');

UPDATE commodities
SET commodity_it = 'Violini di Kinago'
WHERE LOWER(commodity) = LOWER('Kinago Violins');

UPDATE commodities
SET commodity_it = 'Kongga Ale'
WHERE LOWER(commodity) = LOWER('Kongga Ale');

UPDATE commodities
SET commodity_it = 'Pellet di Koro Kung'
WHERE LOWER(commodity) = LOWER('Koro Kung Pellets');

UPDATE commodities
SET commodity_it = 'Mine Terrestri'
WHERE LOWER(commodity) = LOWER('Landmines');

UPDATE commodities
SET commodity_it = 'Lantanio'
WHERE LOWER(commodity) = LOWER('Lanthanum');

UPDATE commodities
SET commodity_it = 'Grande Memoria Dati Esplorazione'
WHERE LOWER(commodity) = LOWER('Large Survey Data Cache');

UPDATE commodities
SET commodity_it = 'Brandy Laviano'
WHERE LOWER(commodity) = LOWER('Lavian Brandy');

UPDATE commodities
SET commodity_it = 'Rapporti di Corruzione Lavigny'
WHERE LOWER(commodity) = LOWER('Lavigny Corruption Reports');

UPDATE commodities
SET commodity_it = 'Rifornimenti da Campo Lavigny'
WHERE LOWER(commodity) = LOWER('Lavigny Field Supplies');

UPDATE commodities
SET commodity_it = 'Rifornimenti di Guarnigione Lavigny'
WHERE LOWER(commodity) = LOWER('Lavigny Garrison Supplies');

UPDATE commodities
SET commodity_it = 'Rapporti Strategici Lavigny'
WHERE LOWER(commodity) = LOWER('Lavigny Strategic Reports');

UPDATE commodities
SET commodity_it = 'Pelle'
WHERE LOWER(commodity) = LOWER('Leather');

UPDATE commodities
SET commodity_it = 'Succo del Diavolo Leestiano'
WHERE LOWER(commodity) = LOWER('Leestian Evil Juice');

UPDATE commodities
SET commodity_it = 'Droghe Legali'
WHERE LOWER(commodity) = LOWER('Legal Drugs');

UPDATE commodities
SET commodity_it = 'Lepidolite'
WHERE LOWER(commodity) = LOWER('Lepidolite');

UPDATE commodities
SET commodity_it = 'Caffè Estratto nel Vuoto'
WHERE LOWER(commodity) = LOWER('Void Extract Coffee');

UPDATE commodities
SET commodity_it = 'Propaganda Liberale'
WHERE LOWER(commodity) = LOWER('Liberal Propaganda');

UPDATE commodities
SET commodity_it = 'Ossigeno Liquido'
WHERE LOWER(commodity) = LOWER('Liquid oxygen');

UPDATE commodities
SET commodity_it = 'Liquore'
WHERE LOWER(commodity) = LOWER('Liquor');

UPDATE commodities
SET commodity_it = 'Litio'
WHERE LOWER(commodity) = LOWER('Lithium');

UPDATE commodities
SET commodity_it = 'Idrossido di litio'
WHERE LOWER(commodity) = LOWER('Lithium Hydroxide');

UPDATE commodities
SET commodity_it = 'Vermi Marini Vivi di Hecate'
WHERE LOWER(commodity) = LOWER('Live Hecate Sea Worms');

UPDATE commodities
SET commodity_it = 'Armi militari marcate'
WHERE LOWER(commodity) = LOWER('Marked Military Arms');

UPDATE commodities
SET commodity_it = 'Diamanti a Bassa Temperatura'
WHERE LOWER(commodity) = LOWER('Low Temperature Diamonds');

UPDATE commodities
SET commodity_it = 'Iperdolce di LTT'
WHERE LOWER(commodity) = LOWER('LTT Hyper Sweet');

UPDATE commodities
SET commodity_it = 'Erba di Lyrae'
WHERE LOWER(commodity) = LOWER('Lyrae Weed');

UPDATE commodities
SET commodity_it = 'Membrana di Mollusco'
WHERE LOWER(commodity) = LOWER('Mollusc Membrane');

UPDATE commodities
SET commodity_it = 'Micelio di mollusco'
WHERE LOWER(commodity) = LOWER('Mollusc Mycelium');

UPDATE commodities
SET commodity_it = 'Spore di Mollusco'
WHERE LOWER(commodity) = LOWER('Mollusc Spores');

UPDATE commodities
SET commodity_it = 'Fluido di Mollusco'
WHERE LOWER(commodity) = LOWER('Mollusc Fluid');

UPDATE commodities
SET commodity_it = 'Tessuto Cerebrale di Mollusco'
WHERE LOWER(commodity) = LOWER('Mollusc Brain Tissue');

UPDATE commodities
SET commodity_it = 'Tessuto Molle di Mollusco'
WHERE LOWER(commodity) = LOWER('Mollusc Soft Tissue');

UPDATE commodities
SET commodity_it = 'Bobina Emettitore Magnetico'
WHERE LOWER(commodity) = LOWER('Magnetic Emitter Coil');

UPDATE commodities
SET commodity_it = 'Attrezzatura Marina'
WHERE LOWER(commodity) = LOWER('Marine Equipment');

UPDATE commodities
SET commodity_it = 'Schiavi marcati'
WHERE LOWER(commodity) = LOWER('Marked Slaves');

UPDATE commodities
SET commodity_it = 'Master Chefs'
WHERE LOWER(commodity) = LOWER('Master Chefs');

UPDATE commodities
SET commodity_it = 'Tè High di Mechucos'
WHERE LOWER(commodity) = LOWER('Mechucos High Tea');

UPDATE commodities
SET commodity_it = 'Lubrificante Stellare di Medb'
WHERE LOWER(commodity) = LOWER('Medb Starlube');

UPDATE commodities
SET commodity_it = 'Apparecchi Diagnostici Medici'
WHERE LOWER(commodity) = LOWER('Medical Diagnostic Equipment');

UPDATE commodities
SET commodity_it = 'Meta-Leghe'
WHERE LOWER(commodity) = LOWER('Meta-Alloys');

UPDATE commodities
SET commodity_it = 'Clatrato di Metano'
WHERE LOWER(commodity) = LOWER('Methane Clathrate');

UPDATE commodities
SET commodity_it = 'Cristalli di Monoidrato di Metanolo'
WHERE LOWER(commodity) = LOWER('Methanol Monohydrate Crystals');

UPDATE commodities
SET commodity_it = 'Microcontrollori'
WHERE LOWER(commodity) = LOWER('Micro Controllers');

UPDATE commodities
SET commodity_it = 'Tessuti di Grado militare'
WHERE LOWER(commodity) = LOWER('Military Grade Fabrics');

UPDATE commodities
SET commodity_it = 'Informazioni militari'
WHERE LOWER(commodity) = LOWER('Military Intelligence');

UPDATE commodities
SET commodity_it = 'Estrattori di Minerali'
WHERE LOWER(commodity) = LOWER('Mineral Extractors');

UPDATE commodities
SET commodity_it = 'Olio minerale'
WHERE LOWER(commodity) = LOWER('Mineral Oil');

UPDATE commodities
SET commodity_it = 'Terminali Modulari'
WHERE LOWER(commodity) = LOWER('Modular Terminals');

UPDATE commodities
SET commodity_it = 'Moissanite'
WHERE LOWER(commodity) = LOWER('Moissanite');

UPDATE commodities
SET commodity_it = 'Beast Feast di Mokojing'
WHERE LOWER(commodity) = LOWER('Mokojing Beast Feast');

UPDATE commodities
SET commodity_it = 'Spaniel delle Paludi di Momus'
WHERE LOWER(commodity) = LOWER('Momus Bog Spaniel');

UPDATE commodities
SET commodity_it = 'Monazite'
WHERE LOWER(commodity) = LOWER('Monazite');

UPDATE commodities
SET commodity_it = 'Experience Jelly di Motrona'
WHERE LOWER(commodity) = LOWER('Motrona Experience Jelly');

UPDATE commodities
SET commodity_it = 'Chitin‑os di Mukusubii'
WHERE LOWER(commodity) = LOWER('Mukusubii Chitin-os');

UPDATE commodities
SET commodity_it = 'Fungo gigante di Mulachi'
WHERE LOWER(commodity) = LOWER('Mulachi Giant Fungus');

UPDATE commodities
SET commodity_it = 'Musgravite'
WHERE LOWER(commodity) = LOWER('Musgravite');

UPDATE commodities
SET commodity_it = 'Imager a muoni'
WHERE LOWER(commodity) = LOWER('Muon Imager');

UPDATE commodities
SET commodity_it = 'Idolo Misterioso'
WHERE LOWER(commodity) = LOWER('Mysterious Idol');

UPDATE commodities
SET commodity_it = 'Nanofratturatori'
WHERE LOWER(commodity) = LOWER('Nanobreakers');

UPDATE commodities
SET commodity_it = 'Nanomedicine'
WHERE LOWER(commodity) = LOWER('Nanomedicines');

UPDATE commodities
SET commodity_it = 'Tessuti naturali'
WHERE LOWER(commodity) = LOWER('Natural Fabrics');

UPDATE commodities
SET commodity_it = 'Isolamento neotessile'
WHERE LOWER(commodity) = LOWER('Neofabric Insulation');

UPDATE commodities
SET commodity_it = 'Bacche di Neritus'
WHERE LOWER(commodity) = LOWER('Neritus Berries');

UPDATE commodities
SET commodity_it = 'Agenti Nervini'
WHERE LOWER(commodity) = LOWER('Nerve Agents');

UPDATE commodities
SET commodity_it = 'Opali di Fuoco di Ngadandari'
WHERE LOWER(commodity) = LOWER('Ngadandari Fire Opals');

UPDATE commodities
SET commodity_it = 'Antichità Moderne di Nguna'
WHERE LOWER(commodity) = LOWER('Nguna Modern Antiques');

UPDATE commodities
SET commodity_it = 'Selle di Njangari'
WHERE LOWER(commodity) = LOWER('Njangari Saddles');

UPDATE commodities
SET commodity_it = 'Esotank non euclidei'
WHERE LOWER(commodity) = LOWER('Non Euclidian Exotanks');

UPDATE commodities
SET commodity_it = 'Armi non Letali'
WHERE LOWER(commodity) = LOWER('Non-Lethal Weapons');

UPDATE commodities
SET commodity_it = 'Capsula di Salvataggio Occupata'
WHERE LOWER(commodity) = LOWER('Occupied Escape Pod');

UPDATE commodities
SET commodity_it = 'Peperoncini di Ochoeng'
WHERE LOWER(commodity) = LOWER('Ochoeng Chillies');

UPDATE commodities
SET commodity_it = 'Onionhead'
WHERE LOWER(commodity) = LOWER('Onionhead');

UPDATE commodities
SET commodity_it = 'Onionhead Ceppo Alpha'
WHERE LOWER(commodity) = LOWER('Onionhead Alpha Strain');

UPDATE commodities
SET commodity_it = 'Onionhead Ceppo Beta'
WHERE LOWER(commodity) = LOWER('Onionhead Beta Strain');

UPDATE commodities
SET commodity_it = 'Onionhead Ceppo Gamma'
WHERE LOWER(commodity) = LOWER('Onionhead Gamma Strain');

UPDATE commodities
SET commodity_it = 'Derivati di Onionhead'
WHERE LOWER(commodity) = LOWER('Onionhead Derivatives');

UPDATE commodities
SET commodity_it = 'Campioni di Onionhead'
WHERE LOWER(commodity) = LOWER('Onionhead Samples');

UPDATE commodities
SET commodity_it = 'Opale del Vuoto'
WHERE LOWER(commodity) = LOWER('Void Opal');

UPDATE commodities
SET commodity_it = 'Manufatti di Exino Ophiuch'
WHERE LOWER(commodity) = LOWER('Ophiuch Exino Artefacts');

UPDATE commodities
SET commodity_it = 'Infuso Vizioso Orreriano'
WHERE LOWER(commodity) = LOWER('Orrerian Vicious Brew');

UPDATE commodities
SET commodity_it = 'Osmio'
WHERE LOWER(commodity) = LOWER('Osmium');

UPDATE commodities
SET commodity_it = 'Merci Scadute'
WHERE LOWER(commodity) = LOWER('Out Of Date Goods');

UPDATE commodities
SET commodity_it = 'Particelle di Anomalie'
WHERE LOWER(commodity) = LOWER('Anomaly Particles');

UPDATE commodities
SET commodity_it = 'Painite'
WHERE LOWER(commodity) = LOWER('Painite');

UPDATE commodities
SET commodity_it = 'Palladio'
WHERE LOWER(commodity) = LOWER('Palladium');

UPDATE commodities
SET commodity_it = 'Bastoni Cerimoniali di Pantaa'
WHERE LOWER(commodity) = LOWER('Pantaa Prayer Sticks');

UPDATE commodities
SET commodity_it = 'Rifornimenti da Campo Patreus'
WHERE LOWER(commodity) = LOWER('Patreus Field Supplies');

UPDATE commodities
SET commodity_it = 'Rifornimenti di Guarnigione Patreus'
WHERE LOWER(commodity) = LOWER('Patreus Garrison Supplies');

UPDATE commodities
SET commodity_it = 'Larve auricolari di Pavonis'
WHERE LOWER(commodity) = LOWER('Pavonis Ear Grubs');

UPDATE commodities
SET commodity_it = 'Potenziatori di prestazioni'
WHERE LOWER(commodity) = LOWER('Performance Enhancers');

UPDATE commodities
SET commodity_it = 'Effetti personali'
WHERE LOWER(commodity) = LOWER('Personal Effects');

UPDATE commodities
SET commodity_it = 'Regali personali'
WHERE LOWER(commodity) = LOWER('Personal Gifts');

UPDATE commodities
SET commodity_it = 'Armi personali'
WHERE LOWER(commodity) = LOWER('Personal Weapons');

UPDATE commodities
SET commodity_it = 'Pesticidi'
WHERE LOWER(commodity) = LOWER('Pesticides');

UPDATE commodities
SET commodity_it = 'Platino'
WHERE LOWER(commodity) = LOWER('Platinum');

UPDATE commodities
SET commodity_it = 'Lega di Platino'
WHERE LOWER(commodity) = LOWER('Platinum Alloy');

UPDATE commodities
SET commodity_it = 'Prigionieri Politici'
WHERE LOWER(commodity) = LOWER('Political Prisoners');

UPDATE commodities
SET commodity_it = 'Polimeri'
WHERE LOWER(commodity) = LOWER('Polymers');

UPDATE commodities
SET commodity_it = 'Convertitore di Energia'
WHERE LOWER(commodity) = LOWER('Power Converter');

UPDATE commodities
SET commodity_it = 'Generatori di Energia'
WHERE LOWER(commodity) = LOWER('Power Generators');

UPDATE commodities
SET commodity_it = 'Rete di Energia'
WHERE LOWER(commodity) = LOWER('Energy Grid Assembly');

UPDATE commodities
SET commodity_it = 'Barra di Trasferimento Energia'
WHERE LOWER(commodity) = LOWER('Power Transfer Bus');

UPDATE commodities
SET commodity_it = 'Praseodimio'
WHERE LOWER(commodity) = LOWER('Praseodymium');

UPDATE commodities
SET commodity_it = 'Gemme preziose'
WHERE LOWER(commodity) = LOWER('Precious Gems');

UPDATE commodities
SET commodity_it = 'Cellule progenitrici'
WHERE LOWER(commodity) = LOWER('Progenitor Cells');

UPDATE commodities
SET commodity_it = 'Materiali di ricerca proibiti'
WHERE LOWER(commodity) = LOWER('Prohibited Research Materials');

UPDATE commodities
SET commodity_it = 'Pirofillite'
WHERE LOWER(commodity) = LOWER('Pyrophyllite');

UPDATE commodities
SET commodity_it = 'Schermo antiradiazioni'
WHERE LOWER(commodity) = LOWER('Radiation Baffle');

UPDATE commodities
SET commodity_it = 'Multifornelli di Rajukru'
WHERE LOWER(commodity) = LOWER('Rajukru Multi-Stoves');

UPDATE commodities
SET commodity_it = 'Pelli di Serpente di Rapa Bao'
WHERE LOWER(commodity) = LOWER('Rapa Bao Snake Skins');

UPDATE commodities
SET commodity_it = 'Armatura Reattiva'
WHERE LOWER(commodity) = LOWER('Reactive Armour');

UPDATE commodities
SET commodity_it = 'Piastra di Montaggio Rinforzata'
WHERE LOWER(commodity) = LOWER('Reinforced Mounting Plate');

UPDATE commodities
SET commodity_it = 'Rifornimenti da Campo Hudson'
WHERE LOWER(commodity) = LOWER('Hudson’s Field Supplies');

UPDATE commodities
SET commodity_it = 'Rifornimenti di Guarnigione Hudson'
WHERE LOWER(commodity) = LOWER('Hudson Garrison Supplies');

UPDATE commodities
SET commodity_it = 'Separatori Risonanti'
WHERE LOWER(commodity) = LOWER('Resonating Separators');

UPDATE commodities
SET commodity_it = 'Informazioni riservate Hudson'
WHERE LOWER(commodity) = LOWER('Hudson’s Restricted Intel');

UPDATE commodities
SET commodity_it = 'Pacco Riservato di Core'
WHERE LOWER(commodity) = LOWER('Core Restricted Package');

UPDATE commodities
SET commodity_it = 'Rhodplumsite'
WHERE LOWER(commodity) = LOWER('Rhodplumsite');

UPDATE commodities
SET commodity_it = 'Robotica'
WHERE LOWER(commodity) = LOWER('Robotics');

UPDATE commodities
SET commodity_it = 'Fertilizzante Rockforth'
WHERE LOWER(commodity) = LOWER('Rockforth Fertiliser');

UPDATE commodities
SET commodity_it = 'Old Smokey di Rusani'
WHERE LOWER(commodity) = LOWER('Rusani Old Smokey');

UPDATE commodities
SET commodity_it = 'Rutilo'
WHERE LOWER(commodity) = LOWER('Rutile');

UPDATE commodities
SET commodity_it = 'Tessuto esterno del bacello'
WHERE LOWER(commodity) = LOWER('Pod Outer Tissue');

UPDATE commodities
SET commodity_it = 'Tessuto del Guscio del Bacello'
WHERE LOWER(commodity) = LOWER('Pod Shell Tissue');

UPDATE commodities
SET commodity_it = 'Mesoglea del Bacello'
WHERE LOWER(commodity) = LOWER('Pod Mesoglea');

UPDATE commodities
SET commodity_it = 'Tessuto del Bacello'
WHERE LOWER(commodity) = LOWER('Pod Tissue');

UPDATE commodities
SET commodity_it = 'Tessuto Interno del Bacello'
WHERE LOWER(commodity) = LOWER('Pod Core Tissue');

UPDATE commodities
SET commodity_it = 'Tessuto Superficiale del Bacello'
WHERE LOWER(commodity) = LOWER('Pod Surface Tissue');

UPDATE commodities
SET commodity_it = 'Tessuto Morto del Bacello'
WHERE LOWER(commodity) = LOWER('Pod Dead Tissue');

UPDATE commodities
SET commodity_it = 'Samario'
WHERE LOWER(commodity) = LOWER('Samarium');

UPDATE commodities
SET commodity_it = 'Carne decorativa di Sanuma'
WHERE LOWER(commodity) = LOWER('Sanuma Decorative Meat');

UPDATE commodities
SET commodity_it = 'Contenitore Nucleo SAP 8'
WHERE LOWER(commodity) = LOWER('SAP 8 Core Container');

UPDATE commodities
SET commodity_it = 'Vino Sassone'
WHERE LOWER(commodity) = LOWER('Saxon Wine');

UPDATE commodities
SET commodity_it = 'Ricerca Scientifica'
WHERE LOWER(commodity) = LOWER('Scientific Research');

UPDATE commodities
SET commodity_it = 'Campioni Scientifici'
WHERE LOWER(commodity) = LOWER('Scientific Samples');

UPDATE commodities
SET commodity_it = 'Rottami'
WHERE LOWER(commodity) = LOWER('Scrap');

UPDATE commodities
SET commodity_it = 'Semiconduttori'
WHERE LOWER(commodity) = LOWER('Semiconductors');

UPDATE commodities
SET commodity_it = 'Serendibite'
WHERE LOWER(commodity) = LOWER('Serendibite');

UPDATE commodities
SET commodity_it = 'Orchidea Charis di Shan'
WHERE LOWER(commodity) = LOWER('Shan’s Charis Orchid');

UPDATE commodities
SET commodity_it = 'Argento'
WHERE LOWER(commodity) = LOWER('Silver');

UPDATE commodities
SET commodity_it = 'Contratti Corporate di Sirius'
WHERE LOWER(commodity) = LOWER('Sirius Corporate Contracts');

UPDATE commodities
SET commodity_it = 'Pacco Franchising di Sirius'
WHERE LOWER(commodity) = LOWER('Sirius Franchise Package');

UPDATE commodities
SET commodity_it = 'Attrezzatura Industriale di Sirius'
WHERE LOWER(commodity) = LOWER('Sirius Industrial Equipment');

UPDATE commodities
SET commodity_it = 'Componenti Skimmer'
WHERE LOWER(commodity) = LOWER('Skimmer Components');

UPDATE commodities
SET commodity_it = 'Schiavi'
WHERE LOWER(commodity) = LOWER('Slaves');

UPDATE commodities
SET commodity_it = 'Piccola Memoria Dati Esplorazione'
WHERE LOWER(commodity) = LOWER('Small Survey Data Cache');

UPDATE commodities
SET commodity_it = 'Reliquie di Soontill'
WHERE LOWER(commodity) = LOWER('Soontill Relics');

UPDATE commodities
SET commodity_it = 'Oro cristallino di Sothis'
WHERE LOWER(commodity) = LOWER('Sothis Crystalline Gold');

UPDATE commodities
SET commodity_it = 'Reliquie dei Pionieri dello Spazio'
WHERE LOWER(commodity) = LOWER('Space Pioneer Relics');

UPDATE commodities
SET commodity_it = 'Acciaio'
WHERE LOWER(commodity) = LOWER('Steel');

UPDATE commodities
SET commodity_it = 'Regolatori Strutturali'
WHERE LOWER(commodity) = LOWER('Structural Regulators');

UPDATE commodities
SET commodity_it = 'Superconduttori'
WHERE LOWER(commodity) = LOWER('Superconductors');

UPDATE commodities
SET commodity_it = 'Stabilizzatori di superficie'
WHERE LOWER(commodity) = LOWER('Surface Stabilisers');

UPDATE commodities
SET commodity_it = 'Equipaggiamento di sopravvivenza'
WHERE LOWER(commodity) = LOWER('Survival Equipment');

UPDATE commodities
SET commodity_it = 'Tessuti sintetici'
WHERE LOWER(commodity) = LOWER('Synthetic Fabrics');

UPDATE commodities
SET commodity_it = 'Carne sintetica'
WHERE LOWER(commodity) = LOWER('Synthetic Meat');

UPDATE commodities
SET commodity_it = 'Reagenti sintetici'
WHERE LOWER(commodity) = LOWER('Synthetic Reagents');

UPDATE commodities
SET commodity_it = 'Taaffeite'
WHERE LOWER(commodity) = LOWER('Taaffeite');

UPDATE commodities
SET commodity_it = 'Dati tattici'
WHERE LOWER(commodity) = LOWER('Tactical Data');

UPDATE commodities
SET commodity_it = 'Tè Tranquil di Tanmark'
WHERE LOWER(commodity) = LOWER('Tanmark Tranquil Tea');

UPDATE commodities
SET commodity_it = 'Tantalio'
WHERE LOWER(commodity) = LOWER('Tantalum');

UPDATE commodities
SET commodity_it = 'Spezia di Tarach'
WHERE LOWER(commodity) = LOWER('Tarach Spice');

UPDATE commodities
SET commodity_it = ' Campane di Tauri'
WHERE LOWER(commodity) = LOWER('Tauri Chimes');

UPDATE commodities
SET commodity_it = 'Tè'
WHERE LOWER(commodity) = LOWER('Tea');

UPDATE commodities
SET commodity_it = 'Suite di telemetria'
WHERE LOWER(commodity) = LOWER('Telemetry Suite');

UPDATE commodities
SET commodity_it = 'Sistemi di arricchimento del suolo'
WHERE LOWER(commodity) = LOWER('Land Enrichment Systems');

UPDATE commodities
SET commodity_it = 'Potenziatore Sanguigno di Terra Mater'
WHERE LOWER(commodity) = LOWER('Terra Mater Blood Bores');

UPDATE commodities
SET commodity_it = 'Tallio'
WHERE LOWER(commodity) = LOWER('Thallium');

UPDATE commodities
SET commodity_it = 'Frammenti di osso'
WHERE LOWER(commodity) = LOWER('Bone Fragments');

UPDATE commodities
SET commodity_it = 'Campione di cisti'
WHERE LOWER(commodity) = LOWER('Cyst Specimen');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Caustico (Thargoid)'
WHERE LOWER(commodity) = LOWER('Caustic Tissue Sample (Thargoid)');

UPDATE commodities
SET commodity_it = 'Cuore Thargoid'
WHERE LOWER(commodity) = LOWER('Thargoid Heart');

UPDATE commodities
SET commodity_it = 'Campione di organo'
WHERE LOWER(commodity) = LOWER('Organ Sample');

UPDATE commodities
SET commodity_it = 'Capsula di Bioconservazione Thargoid'
WHERE LOWER(commodity) = LOWER('Thargoid Bio-storage Capsule');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Thargoid Scout'
WHERE LOWER(commodity) = LOWER('Thargoid Scout Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Profondo Titan'
WHERE LOWER(commodity) = LOWER('Titan Deep Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Titan'
WHERE LOWER(commodity) = LOWER('Titan Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione Parziale di Tessuto Titan'
WHERE LOWER(commodity) = LOWER('Titan Partial Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Thargoid Cyclops'
WHERE LOWER(commodity) = LOWER('Thargoid Cyclops Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Profondo della Bocca Titan'
WHERE LOWER(commodity) = LOWER('Titan Maw Deep Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto della Bocca Titan'
WHERE LOWER(commodity) = LOWER('Titan Maw Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione Parziale di Tessuto della Bocca Titan'
WHERE LOWER(commodity) = LOWER('Titan Maw Partial Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Thargoid Basilisk'
WHERE LOWER(commodity) = LOWER('Thargoid Basilisk Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Thargoid Medusa'
WHERE LOWER(commodity) = LOWER('Thargoid Medusa Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Thargoid Hydra'
WHERE LOWER(commodity) = LOWER('Thargoid Hydra Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Thargoid Orthrus'
WHERE LOWER(commodity) = LOWER('Thargoid Orthrus Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Thargoid Glaive'
WHERE LOWER(commodity) = LOWER('Thargoid Glaive Tissue Sample');

UPDATE commodities
SET commodity_it = 'Campione di Tessuto Thargoid Scythe'
WHERE LOWER(commodity) = LOWER('Thargoid Scythe Tissue Sample');

UPDATE commodities
SET commodity_it = 'Componente Motore Titan'
WHERE LOWER(commodity) = LOWER('Titan Drive Component');

UPDATE commodities
SET commodity_it = 'Boccale di Hutton'
WHERE LOWER(commodity) = LOWER('The Hutton Mug');

UPDATE commodities
SET commodity_it = 'Unità di raffreddamento termico'
WHERE LOWER(commodity) = LOWER('Thermal Cooling Units');

UPDATE commodities
SET commodity_it = 'Torio'
WHERE LOWER(commodity) = LOWER('Thorium');

UPDATE commodities
SET commodity_it = 'Crema di Thrutis'
WHERE LOWER(commodity) = LOWER('Thrutis Cream');

UPDATE commodities
SET commodity_it = 'Seta sintetica Tiegfries'
WHERE LOWER(commodity) = LOWER('Tiegfries Synth Silk');

UPDATE commodities
SET commodity_it = 'Capsula del tempo'
WHERE LOWER(commodity) = LOWER('Time Capsule');

UPDATE commodities
SET commodity_it = 'Unità Waste2Paste di Tiolce'
WHERE LOWER(commodity) = LOWER('Tiolce Waste2Paste Units');

UPDATE commodities
SET commodity_it = 'Titanio'
WHERE LOWER(commodity) = LOWER('Titanium');

UPDATE commodities
SET commodity_it = 'Tabacco'
WHERE LOWER(commodity) = LOWER('Tobacco');

UPDATE commodities
SET commodity_it = 'Accordi commerciali Torval'
WHERE LOWER(commodity) = LOWER('Torval Trade Agreements');

UPDATE commodities
SET commodity_it = 'Atti Torval'
WHERE LOWER(commodity) = LOWER('Torval Deeds');

UPDATE commodities
SET commodity_it = 'Virocida di Toxandji'
WHERE LOWER(commodity) = LOWER('Toxandji Virocide');

UPDATE commodities
SET commodity_it = 'Rifiuti tossici'
WHERE LOWER(commodity) = LOWER('Toxic Waste');

UPDATE commodities
SET commodity_it = 'Onionhead di Lucan'
WHERE LOWER(commodity) = LOWER('Lucan Onionhead');

UPDATE commodities
SET commodity_it = 'Amuleti della Fortuna Nascosta'
WHERE LOWER(commodity) = LOWER('Trinkets of Hidden Fortune');

UPDATE commodities
SET commodity_it = 'Tritio'
WHERE LOWER(commodity) = LOWER('Tritium');

UPDATE commodities
SET commodity_it = 'Supporto Clandestino Grom'
WHERE LOWER(commodity) = LOWER('Grom Underground Support');

UPDATE commodities
SET commodity_it = 'Sconosciuto'
WHERE LOWER(commodity) = LOWER('Unknown');

UPDATE commodities
SET commodity_it = 'Sensore Thargoid'
WHERE LOWER(commodity) = LOWER('Thargoid Sensor');

UPDATE commodities
SET commodity_it = 'Sonda Thargoid'
WHERE LOWER(commodity) = LOWER('Thargoid Probe');

UPDATE commodities
SET commodity_it = 'Collegamento Thargoid'
WHERE LOWER(commodity) = LOWER('Thargoid Link');

UPDATE commodities
SET commodity_it = 'Materia Biologica Thargoid'
WHERE LOWER(commodity) = LOWER('Thargoid Biological Matter');

UPDATE commodities
SET commodity_it = 'Minerale Impuro delle Spire'
WHERE LOWER(commodity) = LOWER('Impure Spire Mineral');

UPDATE commodities
SET commodity_it = 'Minerale Semilavorato delle Spire'
WHERE LOWER(commodity) = LOWER('Semi-Refined Spire Mineral');

UPDATE commodities
SET commodity_it = 'Resina Thargoid'
WHERE LOWER(commodity) = LOWER('Thargoid Resin');

UPDATE commodities
SET commodity_it = 'Scarti di Membrana Protettiva'
WHERE LOWER(commodity) = LOWER('Protective Membrane Scrap');

UPDATE commodities
SET commodity_it = 'Campioni di Tecnologia Thargoid'
WHERE LOWER(commodity) = LOWER('Thargoid Technology Samples');

UPDATE commodities
SET commodity_it = 'Rifornimenti militari non marcati'
WHERE LOWER(commodity) = LOWER('Unmarked Military supplies');

UPDATE commodities
SET commodity_it = 'Capsula di Salvataggio Vuota'
WHERE LOWER(commodity) = LOWER('Unoccupied Escape Pod');

UPDATE commodities
SET commodity_it = 'Nucleo Dati Instabile'
WHERE LOWER(commodity) = LOWER('Unstable Data Core');

UPDATE commodities
SET commodity_it = 'Uraninite'
WHERE LOWER(commodity) = LOWER('Uraninite');

UPDATE commodities
SET commodity_it = 'Uranio'
WHERE LOWER(commodity) = LOWER('Uranium');

UPDATE commodities
SET commodity_it = 'Artefatto antico'
WHERE LOWER(commodity) = LOWER('Ancient Artefact');

UPDATE commodities
SET commodity_it = 'Scatola nera'
WHERE LOWER(commodity) = LOWER('Black Box');

UPDATE commodities
SET commodity_it = 'Sostanze chimiche sperimentali'
WHERE LOWER(commodity) = LOWER('Experimental Chemicals');

UPDATE commodities
SET commodity_it = 'Piani militari'
WHERE LOWER(commodity) = LOWER('Military Plans');

UPDATE commodities
SET commodity_it = 'Prototipo tecnologico'
WHERE LOWER(commodity) = LOWER('Prototype Tech');

UPDATE commodities
SET commodity_it = 'Opera d’arte rara'
WHERE LOWER(commodity) = LOWER('Rare Artwork');

UPDATE commodities
SET commodity_it = 'Trasmissioni ribelli'
WHERE LOWER(commodity) = LOWER('Rebel Transmissions');

UPDATE commodities
SET commodity_it = 'Progetti tecnici'
WHERE LOWER(commodity) = LOWER('Technical Blueprints');

UPDATE commodities
SET commodity_it = 'Dati commerciali'
WHERE LOWER(commodity) = LOWER('Trade Data');

UPDATE commodities
SET commodity_it = 'Larva dell’albero di Uszaian'
WHERE LOWER(commodity) = LOWER('Uszaian Tree Grub');

UPDATE commodities
SET commodity_it = 'Uova Millenarie di Utgaroar'
WHERE LOWER(commodity) = LOWER('Utgaroar Millennial Eggs');

UPDATE commodities
SET commodity_it = 'Dissidente di Utopian'
WHERE LOWER(commodity) = LOWER('Utopian Dissident');

UPDATE commodities
SET commodity_it = 'Rifornimenti di Utopian'
WHERE LOWER(commodity) = LOWER('Utopian Supplies');

UPDATE commodities
SET commodity_it = 'Pubblicità di Utopian'
WHERE LOWER(commodity) = LOWER('Utopian Publicity');

UPDATE commodities
SET commodity_it = 'Ali a Bassa Gravità di Uzumoku'
WHERE LOWER(commodity) = LOWER('Uzumoku Low-G Wings');

UPDATE commodities
SET commodity_it = 'Pelliccia Ceratomorpha di Vanayequi'
WHERE LOWER(commodity) = LOWER('Vanayequi Ceratomorpha Fur');

UPDATE commodities
SET commodity_it = 'Erba Sottile di Vega'
WHERE LOWER(commodity) = LOWER('Vega Slimweed');

UPDATE commodities
SET commodity_it = 'Unguento Corporeo V Herculis'
WHERE LOWER(commodity) = LOWER('V Herculis Body Rub');

UPDATE commodities
SET commodity_it = 'Merletto di Vidavantian'
WHERE LOWER(commodity) = LOWER('Vidavantian Lace');

UPDATE commodities
SET commodity_it = 'Droni Ape di Volkhab'
WHERE LOWER(commodity) = LOWER('Volkhab Bee Drones');

UPDATE commodities
SET commodity_it = 'Acqua'
WHERE LOWER(commodity) = LOWER('Water');

UPDATE commodities
SET commodity_it = 'Purificatori d’acqua'
WHERE LOWER(commodity) = LOWER('Water Purifiers');

UPDATE commodities
SET commodity_it = 'Acqua di Shintara'
WHERE LOWER(commodity) = LOWER('The Waters Of Shintara');

UPDATE commodities
SET commodity_it = 'Armi'
WHERE LOWER(commodity) = LOWER('Weapons');

UPDATE commodities
SET commodity_it = 'Torte di Grano Wheemete'
WHERE LOWER(commodity) = LOWER('Wheemete Wheat Cakes');

UPDATE commodities
SET commodity_it = 'Vino'
WHERE LOWER(commodity) = LOWER('Wine');

UPDATE commodities
SET commodity_it = 'Carne Kobe di Witchhaul'
WHERE LOWER(commodity) = LOWER('Witchhaul Kobe Beef');

UPDATE commodities
SET commodity_it = 'Wolf Fesh'
WHERE LOWER(commodity) = LOWER('Wolf Fesh');

UPDATE commodities
SET commodity_it = 'Componenti Relitto'
WHERE LOWER(commodity) = LOWER('Wreckage Components');

UPDATE commodities
SET commodity_it = 'Sistemi Iperborei Wulpa'
WHERE LOWER(commodity) = LOWER('Wulpa Hyperbore Systems');

UPDATE commodities
SET commodity_it = 'Spuma di Wuthielo Ku'
WHERE LOWER(commodity) = LOWER('Wuthielo Ku Froth');

UPDATE commodities
SET commodity_it = 'Compagni biomorfici di Xihe'
WHERE LOWER(commodity) = LOWER('Xihe Biomorphic Companions');

UPDATE commodities
SET commodity_it = 'Foglia Yaso Kondi'
WHERE LOWER(commodity) = LOWER('Yaso Kondi Leaf');

UPDATE commodities
SET commodity_it = 'Colla di Larve di ormica Zeessze'
WHERE LOWER(commodity) = LOWER('Zeessze Ant Grub Glue');


