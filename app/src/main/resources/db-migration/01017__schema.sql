-- Retires the `materials` table in favour of a single symbol-keyed `material_names`.
--
-- WHY: `materials` was written from the journal's "Name" field (the FDev symbol) run
-- through capitalizeWords, while `material_names` held display names. The same material
-- therefore existed under two spellings -- 'Basicconductors' and 'Basic Conductors' --
-- and nothing joined them reliably. Worse, EDMaterialCaps.getMax() is keyed by display
-- name but was called with the symbol, so nearly every lookup missed and fell through to
-- the 300 default; that is the wrong maxCapacity data.
--
-- The fix is to key on the journal symbol, which is stable and never localized, and to
-- carry amount/maxCapacity on the same row as the translations.
--
-- Per the additive-only rule (V1.0 and V1.1 testers share one database file), the
-- `materials` table is NOT dropped here -- only orphaned. Its on-hand amounts are
-- copied across at the bottom of this migration.

ALTER TABLE material_names
    ADD COLUMN symbol TEXT;
ALTER TABLE material_names
    ADD COLUMN grade INTEGER;
ALTER TABLE material_names
    ADD COLUMN maxCapacity INTEGER;
ALTER TABLE material_names
    ADD COLUMN amount INTEGER NOT NULL DEFAULT 0;
ALTER TABLE material_names
    ADD COLUMN name_pt TEXT;
ALTER TABLE material_names
    ADD COLUMN name_ptbz TEXT;

-- Spoken forms that are not the game's display name: native phrasing for the
-- Thargoid/Guardian items FDev never translated (the DE/FR/PT clients show English
-- there, so name_de/name_fr/name_pt stay NULL and mirror the game), plus the
-- "Thargoid "-prefixed forms used by EDDI/Inara and the community.
CREATE TABLE IF NOT EXISTS material_aliases
(
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol TEXT NOT NULL,
    lang   TEXT NOT NULL,
    alias  TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_material_aliases ON material_aliases (symbol, lang, alias);
CREATE INDEX IF NOT EXISTS idx_material_aliases_lookup ON material_aliases (lang, alias);


-- ============================ Encoded ============================

UPDATE material_names
SET symbol = 'shieldpatternanalysis'
WHERE symbol IS NULL
  AND name = 'Aberrant Shield Pattern Analysis';
INSERT INTO material_names (symbol, name)
SELECT 'shieldpatternanalysis', 'Aberrant Shield Pattern Analysis'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'shieldpatternanalysis');
UPDATE material_names
SET name         = 'Aberrant Shield Pattern Analysis',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Abweichende Schildeinsatz-Analysen',
    name_es      = 'Análisis de patrones de escudo aberrantes',
    name_fr      = 'Analyse de modèle de bouclier aberrante',
    name_ru      = 'Анализ аномального поведения щита',
    name_uk      = 'Аналіз аномальної поведінки щита',
    name_it      = 'Analisi del Pattern di Scudo Aberrante',
    name_pt      = 'Análise de padrão de escudos aberrante',
    name_ptbz    = 'Análise de padrão de escudos aberrante'
WHERE symbol = 'shieldpatternanalysis';

UPDATE material_names
SET symbol = 'compactemissionsdata'
WHERE symbol IS NULL
  AND name = 'Abnormal Compact Emissions Data';
INSERT INTO material_names (symbol, name)
SELECT 'compactemissionsdata', 'Abnormal Compact Emissions Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'compactemissionsdata');
UPDATE material_names
SET name         = 'Abnormal Compact Emissions Data',
    materialType = 'Encoded',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Anormale kompakte Emissionsdaten',
    name_es      = 'Compresión de datos de transmisiones anormal',
    name_fr      = 'Données d’émissions compactes anormales',
    name_ru      = 'Аномальные компактные данные об излучении',
    name_uk      = 'Аномальні компактні дані про випромінювання',
    name_it      = 'Dati Compressi di Emissioni Anomale',
    name_pt      = 'Dados de emissão compactos anormais',
    name_ptbz    = 'Dados de emissão compactos anormais'
WHERE symbol = 'compactemissionsdata';

UPDATE material_names
SET symbol = 'adaptiveencryptors'
WHERE symbol IS NULL
  AND name = 'Adaptive Encryptors Capture';
INSERT INTO material_names (symbol, name)
SELECT 'adaptiveencryptors', 'Adaptive Encryptors Capture'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'adaptiveencryptors');
UPDATE material_names
SET name         = 'Adaptive Encryptors Capture',
    materialType = 'Encoded',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Adaptive Verschlüsselungserfassung',
    name_es      = 'Captura de encriptadores adaptativos',
    name_fr      = 'Capture de cryptage évolutif',
    name_ru      = 'Захват адаптивного шифровальщика',
    name_uk      = 'Захоплення адаптивного шифрувальника',
    name_it      = 'Cattura di Cifratori Adattivi',
    name_pt      = 'Encriptadores adaptativos capturados',
    name_ptbz    = 'Encriptadores adaptativos capturados'
WHERE symbol = 'adaptiveencryptors';

UPDATE material_names
SET symbol = 'bulkscandata'
WHERE symbol IS NULL
  AND name = 'Anomalous Bulk Scan Data';
INSERT INTO material_names (symbol, name)
SELECT 'bulkscandata', 'Anomalous Bulk Scan Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'bulkscandata');
UPDATE material_names
SET name         = 'Anomalous Bulk Scan Data',
    materialType = 'Encoded',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Anormale Massen-Scan-Daten',
    name_es      = 'Datos de escáner en bruto anómalos',
    name_fr      = 'Fichier volumineux de données d’analyse anormal',
    name_ru      = 'Аномальный массив данных сканирования',
    name_uk      = 'Аномальний масив даних сканування',
    name_it      = 'Dati di Scansione di Massa Anomali',
    name_pt      = 'Dados brutos de escâner anômalos',
    name_ptbz    = 'Dados brutos de escâner anômalos'
WHERE symbol = 'bulkscandata';

UPDATE material_names
SET symbol = 'fsdtelemetry'
WHERE symbol IS NULL
  AND name = 'Anomalous FSD Telemetry';
INSERT INTO material_names (symbol, name)
SELECT 'fsdtelemetry', 'Anomalous FSD Telemetry'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'fsdtelemetry');
UPDATE material_names
SET name         = 'Anomalous FSD Telemetry',
    materialType = 'Encoded',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Anormale FSA-Telemetrie',
    name_es      = 'Telemetría de MDD anómala',
    name_fr      = 'Télémétrie FSD anormale',
    name_ru      = 'Аномальная телеметрия FSD',
    name_uk      = 'Аномальна телеметрія FSD',
    name_it      = 'Telemetria Anomala FSD',
    name_pt      = 'Telemetria anômala de MDD',
    name_ptbz    = 'Telemetria anômala de MDD'
WHERE symbol = 'fsdtelemetry';

UPDATE material_names
SET symbol = 'disruptedwakeechoes'
WHERE symbol IS NULL
  AND name = 'Atypical Disrupted Wake Echoes';
INSERT INTO material_names (symbol, name)
SELECT 'disruptedwakeechoes', 'Atypical Disrupted Wake Echoes'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'disruptedwakeechoes');
UPDATE material_names
SET name         = 'Atypical Disrupted Wake Echoes',
    materialType = 'Encoded',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Atypische FSA-Stör-Aufzeichnungen',
    name_es      = 'Ecos de estelas interrumpidas atípicos',
    name_fr      = 'Échos de sillages perturbés atypiques',
    name_ru      = 'Атипичное эхо поврежденного следа',
    name_uk      = 'Атипове відлуння пошкодженого сліду',
    name_it      = 'Echi Atipici di Scie Interrotte',
    name_pt      = 'Interferência atípica no eco de rastros',
    name_ptbz    = 'Interferência atípica no eco de rastros'
WHERE symbol = 'disruptedwakeechoes';

UPDATE material_names
SET symbol = 'encryptionarchives'
WHERE symbol IS NULL
  AND name = 'Atypical Encryption Archives';
INSERT INTO material_names (symbol, name)
SELECT 'encryptionarchives', 'Atypical Encryption Archives'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'encryptionarchives');
UPDATE material_names
SET name         = 'Atypical Encryption Archives',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Atypische Verschlüsselungsarchive',
    name_es      = 'Archivos encriptados atípicos',
    name_fr      = 'Archives cryptées atypiques',
    name_ru      = 'Нетипичные архивы шифрования',
    name_uk      = 'Нетипові архіви шифрування',
    name_it      = 'Archivi Cifrati Atipici',
    name_pt      = 'Arquivos de encriptação atípicos',
    name_ptbz    = 'Arquivos de encriptação atípicos'
WHERE symbol = 'encryptionarchives';

UPDATE material_names
SET symbol = 'scandatabanks'
WHERE symbol IS NULL
  AND name = 'Classified Scan Databanks';
INSERT INTO material_names (symbol, name)
SELECT 'scandatabanks', 'Classified Scan Databanks'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'scandatabanks');
UPDATE material_names
SET name         = 'Classified Scan Databanks',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Scan-Datenbanken unter Verschluss',
    name_es      = 'Datos de escáner clasificados',
    name_fr      = 'Banques de données d’analyse classifiées',
    name_ru      = 'Засекреченные базы данных сканирования',
    name_uk      = 'Засекречені бази даних сканування',
    name_it      = 'Banche dati di scansione classificate',
    name_pt      = 'Banco de dados de escaneamento confidenciais',
    name_ptbz    = 'Banco de dados de escaneamento confidenciais'
WHERE symbol = 'scandatabanks';

UPDATE material_names
SET symbol = 'classifiedscandata'
WHERE symbol IS NULL
  AND name = 'Classified Scan Fragment';
INSERT INTO material_names (symbol, name)
SELECT 'classifiedscandata', 'Classified Scan Fragment'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'classifiedscandata');
UPDATE material_names
SET name         = 'Classified Scan Fragment',
    materialType = 'Encoded',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Geheimes Scan-Fragment',
    name_es      = 'Fragmento de escáner clasificado',
    name_fr      = 'Données d’analyse classifiées parcellaires',
    name_ru      = 'Засекреченные фрагменты данных сканирования',
    name_uk      = 'Засекречені фрагменти даних сканування',
    name_it      = 'Frammento di Scansione Classificata',
    name_pt      = 'Fragmentos de escaneamentos confidenciais',
    name_ptbz    = 'Fragmentos de escaneamentos confidenciais'
WHERE symbol = 'classifiedscandata';

UPDATE material_names
SET symbol = 'industrialfirmware'
WHERE symbol IS NULL
  AND name = 'Cracked Industrial Firmware';
INSERT INTO material_names (symbol, name)
SELECT 'industrialfirmware', 'Cracked Industrial Firmware'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'industrialfirmware');
UPDATE material_names
SET name         = 'Cracked Industrial Firmware',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Gecrackte Industrie-Firmware',
    name_es      = 'Firmware industrial pirateado',
    name_fr      = 'Micrologiciel industriel piraté',
    name_ru      = 'Взломанные промышленные микропрограммы',
    name_uk      = 'Зламані промислові мікропрограми',
    name_it      = 'Firmware Industriale Compromesso',
    name_pt      = 'Firmware industrial quebrado',
    name_ptbz    = 'Firmware industrial quebrado'
WHERE symbol = 'industrialfirmware';

UPDATE material_names
SET symbol = 'dataminedwake'
WHERE symbol IS NULL
  AND name = 'Datamined Wake Exceptions';
INSERT INTO material_names (symbol, name)
SELECT 'dataminedwake', 'Datamined Wake Exceptions'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'dataminedwake');
UPDATE material_names
SET name         = 'Datamined Wake Exceptions',
    materialType = 'Encoded',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'FSA-Daten-Cache-Ausnahmen',
    name_es      = 'Excepciones en análisis de estelas',
    name_fr      = 'Explorations de données de sillages anormales',
    name_ru      = 'Исключения из глубинного анализа данных следа',
    name_uk      = 'Винятки з глибинного аналізу даних сліду',
    name_it      = 'Eccezioni Analisi di Scie',
    name_pt      = 'Exceções de dados processados de rastros',
    name_ptbz    = 'Exceções de dados processados de rastros'
WHERE symbol = 'dataminedwake';

UPDATE material_names
SET symbol = 'decodedemissiondata'
WHERE symbol IS NULL
  AND name = 'Decoded Emission Data';
INSERT INTO material_names (symbol, name)
SELECT 'decodedemissiondata', 'Decoded Emission Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'decodedemissiondata');
UPDATE material_names
SET name         = 'Decoded Emission Data',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Entschlüsselte Emissionsdaten',
    name_es      = 'Datos de emisión descodificados',
    name_fr      = 'Données d’émissions décodées',
    name_ru      = 'Расшифрованные данные об излучении',
    name_uk      = 'Розшифровані дані про випромінювання',
    name_it      = 'Dati Decifrati di Emissione',
    name_pt      = 'Dados de emissão decodificados',
    name_ptbz    = 'Dados de emissão decodificados'
WHERE symbol = 'decodedemissiondata';

UPDATE material_names
SET symbol = 'shieldcyclerecordings'
WHERE symbol IS NULL
  AND name = 'Distorted Shield Cycle Recordings';
INSERT INTO material_names (symbol, name)
SELECT 'shieldcyclerecordings', 'Distorted Shield Cycle Recordings'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'shieldcyclerecordings');
UPDATE material_names
SET name         = 'Distorted Shield Cycle Recordings',
    materialType = 'Encoded',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Gestörte Schildzyklus-Aufzeichnungen',
    name_es      = 'Registros de ciclo de escudo distorsionados',
    name_fr      = 'Enregistrements de cycles de bouclier déformés',
    name_ru      = 'Поврежденные цикличные записи щита',
    name_uk      = 'Пошкоджені циклічні записи щита',
    name_it      = 'Registrazioni Distorte dei Cicli di Scudo',
    name_pt      = 'Registros distorcidos de ciclos de escudo',
    name_ptbz    = 'Registros distorcidos de ciclos de escudo'
WHERE symbol = 'shieldcyclerecordings';

UPDATE material_names
SET symbol = 'encodedscandata'
WHERE symbol IS NULL
  AND name = 'Divergent Scan Data';
INSERT INTO material_names (symbol, name)
SELECT 'encodedscandata', 'Divergent Scan Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'encodedscandata');
UPDATE material_names
SET name         = 'Divergent Scan Data',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Divergente Scandaten',
    name_es      = 'Datos de escáner divergentes',
    name_fr      = 'Données d’analyse divergentes',
    name_ru      = 'Неформатные данные сканирования',
    name_uk      = 'Неформатні дані сканування',
    name_it      = 'Dati di Scansioni Divergenti',
    name_pt      = 'Dados escaneados divergentes',
    name_ptbz    = 'Dados escaneados divergentes'
WHERE symbol = 'encodedscandata';

UPDATE material_names
SET symbol = 'hyperspacetrajectories'
WHERE symbol IS NULL
  AND name = 'Eccentric Hyperspace Trajectories';
INSERT INTO material_names (symbol, name)
SELECT 'hyperspacetrajectories', 'Eccentric Hyperspace Trajectories'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'hyperspacetrajectories');
UPDATE material_names
SET name         = 'Eccentric Hyperspace Trajectories',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Exzentrische Hyperraum-Routen',
    name_es      = 'Trayectorias de hiperespacio excéntricas',
    name_fr      = 'Trajectoires d’hyperespace excentriques',
    name_ru      = 'Аномальные траектории в гиперпространстве',
    name_uk      = 'Аномальні траєкторії в гіперпросторі',
    name_it      = 'Tratte Iperspaziali Eccentriche',
    name_pt      = 'Trajetórias excêntricas de hiperespaço',
    name_ptbz    = 'Trajetórias excêntricas de hiperespaço'
WHERE symbol = 'hyperspacetrajectories';

UPDATE material_names
SET symbol = 'scrambledemissiondata'
WHERE symbol IS NULL
  AND name = 'Exceptional Scrambled Emission Data';
INSERT INTO material_names (symbol, name)
SELECT 'scrambledemissiondata', 'Exceptional Scrambled Emission Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'scrambledemissiondata');
UPDATE material_names
SET name         = 'Exceptional Scrambled Emission Data',
    materialType = 'Encoded',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Außergewöhnliche verschlüsselte Emissionsdaten',
    name_es      = 'Datos de transmisiones codificadas excepcionales',
    name_fr      = 'Données d’émissions brouillées exceptionnelles',
    name_ru      = 'Исключительные зашифрованные данные об излучении',
    name_uk      = 'Виняткові зашифровані дані про випромінювання',
    name_it      = 'Emissioni Cifrate Eccezionali',
    name_pt      = 'Exceção nos dados de emissão embaralhados',
    name_ptbz    = 'Exceção nos dados de emissão embaralhados'
WHERE symbol = 'scrambledemissiondata';

UPDATE material_names
SET symbol = 'guardian_moduleblueprint'
WHERE symbol IS NULL
  AND name = 'Guardian Module Blueprint Fragment';
INSERT INTO material_names (symbol, name)
SELECT 'guardian_moduleblueprint', 'Guardian Module Blueprint Fragment'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'guardian_moduleblueprint');
UPDATE material_names
SET name         = 'Guardian Module Blueprint Fragment',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Guardian-Modulbauplansegment',
    name_es      = 'Segmento de plano de módulo de guardián',
    name_fr      = 'Fragment de plan de module - Guardians',
    name_ru      = 'Фрагмент чертежа модуля Стражей',
    name_uk      = 'Фрагмент креслення модуля Стражів',
    name_it      = 'Frammento Di Schema del Modulo Guardian',
    name_pt      = 'Segmento de diagrama de módulo Guardian',
    name_ptbz    = 'Segmento de diagrama de módulo Guardian'
WHERE symbol = 'guardian_moduleblueprint';

UPDATE material_names
SET symbol = 'guardian_vesselblueprint'
WHERE symbol IS NULL
  AND name = 'Guardian Vessel Blueprint Fragment';
INSERT INTO material_names (symbol, name)
SELECT 'guardian_vesselblueprint', 'Guardian Vessel Blueprint Fragment'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'guardian_vesselblueprint');
UPDATE material_names
SET name         = 'Guardian Vessel Blueprint Fragment',
    materialType = 'Encoded',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Guardian-Schiffsbauplansegment',
    name_es      = 'Segmento de plano de nave de guardián',
    name_fr      = 'Fragment de plan de vaisseau - Guardians',
    name_ru      = 'Фрагмент чертежа судна Стражей',
    name_uk      = 'Фрагмент креслення судна Стражів',
    name_it      = 'Frammento di Schema della Nave Guardian',
    name_pt      = 'Segmento de diagrama de nave Guardian',
    name_ptbz    = 'Segmento de diagrama de nave Guardian'
WHERE symbol = 'guardian_vesselblueprint';

UPDATE material_names
SET symbol = 'guardian_weaponblueprint'
WHERE symbol IS NULL
  AND name = 'Guardian Weapon Blueprint Fragment';
INSERT INTO material_names (symbol, name)
SELECT 'guardian_weaponblueprint', 'Guardian Weapon Blueprint Fragment'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'guardian_weaponblueprint');
UPDATE material_names
SET name         = 'Guardian Weapon Blueprint Fragment',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Guardian-Waffenbauplansegment',
    name_es      = 'Segmento de plano de armamento de guardián',
    name_fr      = 'Fragment de plan d’arme - Guardians',
    name_ru      = 'Фрагмент чертежа оружия Стражей',
    name_uk      = 'Фрагмент креслення зброї Стражів',
    name_it      = 'Frammento di Schema Arma Guardian',
    name_pt      = 'Segmento de diagrama de arma Guardian',
    name_ptbz    = 'Segmento de diagrama de arma Guardian'
WHERE symbol = 'guardian_weaponblueprint';

UPDATE material_names
SET symbol = 'shieldsoakanalysis'
WHERE symbol IS NULL
  AND name = 'Inconsistent Shield Soak Analysis';
INSERT INTO material_names (symbol, name)
SELECT 'shieldsoakanalysis', 'Inconsistent Shield Soak Analysis'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'shieldsoakanalysis');
UPDATE material_names
SET name         = 'Inconsistent Shield Soak Analysis',
    materialType = 'Encoded',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Inkonsistente Schildleistungsanalysen',
    name_es      = 'Análisis de absorción de escudos inconsistente',
    name_fr      = 'Analyse d’absorption de bouclier incohérente',
    name_ru      = 'Неполный анализ поглощения щита',
    name_uk      = 'Неповний аналіз поглинання щита',
    name_it      = 'Analisi Incoerente di Assorbimento degli Scudi',
    name_pt      = 'Análise inconsistente de escudo atingidos',
    name_ptbz    = 'Análise inconsistente de escudo atingidos'
WHERE symbol = 'shieldsoakanalysis';

UPDATE material_names
SET symbol = 'archivedemissiondata'
WHERE symbol IS NULL
  AND name = 'Irregular Emission Data';
INSERT INTO material_names (symbol, name)
SELECT 'archivedemissiondata', 'Irregular Emission Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'archivedemissiondata');
UPDATE material_names
SET name         = 'Irregular Emission Data',
    materialType = 'Encoded',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Irreguläre Emissionsdaten',
    name_es      = 'Datos de emisión irregulares',
    name_fr      = 'Données d’émissions aberrantes',
    name_ru      = 'Нестандартные данные об излучении',
    name_uk      = 'Нестандартні дані про випромінювання',
    name_it      = 'Dati Irregolari di Emissione',
    name_pt      = 'Dados de emissão irregulares',
    name_ptbz    = 'Dados de emissão irregulares'
WHERE symbol = 'archivedemissiondata';

UPDATE material_names
SET symbol = 'tg_shutdowndata'
WHERE symbol IS NULL
  AND name = 'Massive Energy Surge Analytics (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_shutdowndata', 'Massive Energy Surge Analytics'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_shutdowndata');
UPDATE material_names
SET name         = 'Massive Energy Surge Analytics',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = NULL,
    name_es      = 'Análisis de Sobrecarga de Energía Masiva(Thargoide)',
    name_fr      = NULL,
    name_ru      = 'Параметры сильного энергетического импульса',
    name_uk      = 'Параметри сильного енергетичного імпульсу',
    name_it      = 'Analisi di Onda Energetica Massiva',
    name_pt      = NULL,
    name_ptbz    = NULL
WHERE symbol = 'tg_shutdowndata';

UPDATE material_names
SET symbol = 'consumerfirmware'
WHERE symbol IS NULL
  AND name = 'Modified Consumer Firmware';
INSERT INTO material_names (symbol, name)
SELECT 'consumerfirmware', 'Modified Consumer Firmware'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'consumerfirmware');
UPDATE material_names
SET name         = 'Modified Consumer Firmware',
    materialType = 'Encoded',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Modifizierte Consumer-Firmware',
    name_es      = 'Firmware de consumo modificado',
    name_fr      = 'Micrologiciel consommateur modifié',
    name_ru      = 'Измененные пользовательские микропрограммы',
    name_uk      = 'Змінені користувацькі мікропрограми',
    name_it      = 'Firmware Consumer Modificato',
    name_pt      = 'Firmware de consumo modificado',
    name_ptbz    = 'Firmware de consumo modificado'
WHERE symbol = 'consumerfirmware';

UPDATE material_names
SET symbol = 'embeddedfirmware'
WHERE symbol IS NULL
  AND name = 'Modified Embedded Firmware';
INSERT INTO material_names (symbol, name)
SELECT 'embeddedfirmware', 'Modified Embedded Firmware'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'embeddedfirmware');
UPDATE material_names
SET name         = 'Modified Embedded Firmware',
    materialType = 'Encoded',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Modifizierte integrierte Firmware',
    name_es      = 'Firmware integrado modificado',
    name_fr      = 'Micrologiciel intégré modifié',
    name_ru      = 'Измененные встроенные микропрограммы',
    name_uk      = 'Змінені вбудовані мікропрограми',
    name_it      = 'Firmware Embedded Modificato',
    name_pt      = 'Firmware embutido modificado',
    name_ptbz    = 'Firmware embutido modificado'
WHERE symbol = 'embeddedfirmware';

UPDATE material_names
SET symbol = 'symmetrickeys'
WHERE symbol IS NULL
  AND name = 'Open Symmetric Keys';
INSERT INTO material_names (symbol, name)
SELECT 'symmetrickeys', 'Open Symmetric Keys'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'symmetrickeys');
UPDATE material_names
SET name         = 'Open Symmetric Keys',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Offene symmetrische Schlüssel',
    name_es      = 'Claves simétricas abiertas',
    name_fr      = 'Clés symétriques ouvertes',
    name_ru      = 'Открытые симметричные ключи',
    name_uk      = 'Відкриті симетричні ключі',
    name_it      = 'Chiavi Simmetriche Aperte',
    name_pt      = 'Chaves simétricas abertas',
    name_ptbz    = 'Chaves simétricas abertas'
WHERE symbol = 'symmetrickeys';

UPDATE material_names
SET symbol = 'ancientbiologicaldata'
WHERE symbol IS NULL
  AND name = 'Pattern Alpha Obelisk Data (Guardian)';
INSERT INTO material_names (symbol, name)
SELECT 'ancientbiologicaldata', 'Pattern Alpha Obelisk Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'ancientbiologicaldata');
UPDATE material_names
SET name         = 'Pattern Alpha Obelisk Data',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Alpha-Muster-Obeliskendaten',
    name_es      = 'Datos de obelisco de patrón alfa',
    name_fr      = 'Données d’obélisque de type alpha',
    name_ru      = 'Данные с обелиска «Альфа»',
    name_uk      = 'Дані з обеліска «Альфа»',
    name_it      = 'Dati Obelisco di Tipo Alpha',
    name_pt      = 'Dados de Obelisco de Padrão Alfa',
    name_ptbz    = 'Dados de Obelisco de Padrão Alfa'
WHERE symbol = 'ancientbiologicaldata';

UPDATE material_names
SET symbol = 'ancientculturaldata'
WHERE symbol IS NULL
  AND name = 'Pattern Beta Obelisk Data (Guardian)';
INSERT INTO material_names (symbol, name)
SELECT 'ancientculturaldata', 'Pattern Beta Obelisk Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'ancientculturaldata');
UPDATE material_names
SET name         = 'Pattern Beta Obelisk Data',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Beta-Muster-Obeliskendaten',
    name_es      = 'Datos de obelisco de patrón beta',
    name_fr      = 'Données d’obélisque de type bêta',
    name_ru      = 'Данные с обелиска «Бета»',
    name_uk      = 'Дані з обеліска «Бета»',
    name_it      = 'Dati Obelisco di Tipo Beta',
    name_pt      = 'Dados de Obelisco de Padrão Beta',
    name_ptbz    = 'Dados de Obelisco de Padrão Beta'
WHERE symbol = 'ancientculturaldata';

UPDATE material_names
SET symbol = 'ancientlanguagedata'
WHERE symbol IS NULL
  AND name = 'Pattern Delta Obelisk Data (Guardian)';
INSERT INTO material_names (symbol, name)
SELECT 'ancientlanguagedata', 'Pattern Delta Obelisk Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'ancientlanguagedata');
UPDATE material_names
SET name         = 'Pattern Delta Obelisk Data',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Delta-Muster-Obeliskendaten',
    name_es      = 'Datos de obelisco de patrón delta',
    name_fr      = 'Données d’obélisque de type delta',
    name_ru      = 'Данные с обелиска «Дельта»',
    name_uk      = 'Дані з обеліска «Дельта»',
    name_it      = 'Dati Obelisco di Tipo Delta',
    name_pt      = 'Dados de Obelisco de Padrão Delta',
    name_ptbz    = 'Dados de Obelisco de Padrão Delta'
WHERE symbol = 'ancientlanguagedata';

UPDATE material_names
SET symbol = 'ancienttechnologicaldata'
WHERE symbol IS NULL
  AND name = 'Pattern Epsilon Obelisk Data (Guardian)';
INSERT INTO material_names (symbol, name)
SELECT 'ancienttechnologicaldata', 'Pattern Epsilon Obelisk Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'ancienttechnologicaldata');
UPDATE material_names
SET name         = 'Pattern Epsilon Obelisk Data',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Epsilon-Muster-Obeliskendaten',
    name_es      = 'Datos de obelisco de patrón epsilon',
    name_fr      = 'Données d’obélisque de type epsilon',
    name_ru      = 'Данные с обелиска «Эпсилон»',
    name_uk      = 'Дані з обеліска «Епсилон»',
    name_it      = 'Dati Obelisco di Tipo Epsilon',
    name_pt      = 'Dados de Obelisco de Padrão Epsilon',
    name_ptbz    = 'Dados de Obelisco de Padrão Epsilon'
WHERE symbol = 'ancienttechnologicaldata';

UPDATE material_names
SET symbol = 'ancienthistoricaldata'
WHERE symbol IS NULL
  AND name = 'Pattern Gamma Obelisk Data (Guardian)';
INSERT INTO material_names (symbol, name)
SELECT 'ancienthistoricaldata', 'Pattern Gamma Obelisk Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'ancienthistoricaldata');
UPDATE material_names
SET name         = 'Pattern Gamma Obelisk Data',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Gamma-Muster-Obeliskendaten',
    name_es      = 'Datos de obelisco de patrón gamma',
    name_fr      = 'Données d’obélisque de type gamma',
    name_ru      = 'Данные с обелиска «Гамма»',
    name_uk      = 'Дані з обеліска «Гамма»',
    name_it      = 'Dati Obelisco di Tipo Gamma',
    name_pt      = 'Dados de Obelisco de Padrão Gama',
    name_ptbz    = 'Dados de Obelisco de Padrão Gama'
WHERE symbol = 'ancienthistoricaldata';

UPDATE material_names
SET symbol = 'shieldfrequencydata'
WHERE symbol IS NULL
  AND name = 'Peculiar Shield Frequency Data';
INSERT INTO material_names (symbol, name)
SELECT 'shieldfrequencydata', 'Peculiar Shield Frequency Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'shieldfrequencydata');
UPDATE material_names
SET name         = 'Peculiar Shield Frequency Data',
    materialType = 'Encoded',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Verdächtige Schildfrequenz-Daten',
    name_es      = 'Datos de frecuencias de escudo peculiares',
    name_fr      = 'Données de fréquences de bouclier singulières',
    name_ru      = 'Специфические данные о частоте щитов',
    name_uk      = 'Специфічні дані про частоту щитів',
    name_it      = 'Dati di Frequenza degli Scudi Peculiari',
    name_pt      = 'Dados da frequência de escudos peculiares',
    name_ptbz    = 'Dados da frequência de escudos peculiares'
WHERE symbol = 'shieldfrequencydata';

UPDATE material_names
SET symbol = 'securityfirmware'
WHERE symbol IS NULL
  AND name = 'Security Firmware Patch';
INSERT INTO material_names (symbol, name)
SELECT 'securityfirmware', 'Security Firmware Patch'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'securityfirmware');
UPDATE material_names
SET name         = 'Security Firmware Patch',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Sicherheits-Firmware-Patch',
    name_es      = 'Parche de firmware de seguridad',
    name_fr      = 'Mise à jour de micrologiciel de sécurité',
    name_ru      = 'Обновление для защитной микропрограммы',
    name_uk      = 'Оновлення для захисної мікропрограми',
    name_it      = 'Patch di Sicurezza Firmware',
    name_pt      = 'Atualização de firmware de segurança',
    name_ptbz    = 'Atualização de firmware de segurança'
WHERE symbol = 'securityfirmware';

UPDATE material_names
SET symbol = 'tg_shipflightdata'
WHERE symbol IS NULL
  AND name = 'Ship Flight Data (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_shipflightdata', 'Ship Flight Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_shipflightdata');
UPDATE material_names
SET name         = 'Ship Flight Data',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Schiffsflugdaten',
    name_es      = 'Datos de vuelos de nave',
    name_fr      = 'Données de vol de vaisseau',
    name_ru      = 'Полетные данные корабля',
    name_uk      = 'Польотні дані корабля',
    name_it      = 'Dati di Volo della Nave',
    name_pt      = 'Dados de Voo da Nave',
    name_ptbz    = 'Dados de Voo da Nave'
WHERE symbol = 'tg_shipflightdata';

UPDATE material_names
SET symbol = 'tg_shipsystemsdata'
WHERE symbol IS NULL
  AND name = 'Ship Systems Data (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_shipsystemsdata', 'Ship Systems Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_shipsystemsdata');
UPDATE material_names
SET name         = 'Ship Systems Data',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Schiffssysteme-Daten',
    name_es      = 'Datos de sistemas de nave',
    name_fr      = 'Données de systèmes de vaisseau',
    name_ru      = 'Данные бортовых систем корабля',
    name_uk      = 'Дані бортових систем корабля',
    name_it      = 'Dati dei Sistemi della Nave',
    name_pt      = 'Dados do Sistema da Nave',
    name_ptbz    = 'Dados do Sistema da Nave'
WHERE symbol = 'tg_shipsystemsdata';

UPDATE material_names
SET symbol = 'legacyfirmware'
WHERE symbol IS NULL
  AND name = 'Specialised Legacy Firmware';
INSERT INTO material_names (symbol, name)
SELECT 'legacyfirmware', 'Specialised Legacy Firmware'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'legacyfirmware');
UPDATE material_names
SET name         = 'Specialised Legacy Firmware',
    materialType = 'Encoded',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Spezial-Legacy-Firmware',
    name_es      = 'Firmware heredado especializado',
    name_fr      = 'Micrologiciel spécialisé périmé',
    name_ru      = 'Специальные микропрограммы предыдущего поколения',
    name_uk      = 'Спеціальні мікропрограми попереднього покоління',
    name_it      = 'Firmware Legacy Specializzato',
    name_pt      = 'Firmware especializado antigo',
    name_ptbz    = 'Firmware especializado antigo'
WHERE symbol = 'legacyfirmware';

UPDATE material_names
SET symbol = 'wakesolutions'
WHERE symbol IS NULL
  AND name = 'Strange Wake Solutions';
INSERT INTO material_names (symbol, name)
SELECT 'wakesolutions', 'Strange Wake Solutions'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'wakesolutions');
UPDATE material_names
SET name         = 'Strange Wake Solutions',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Seltsame FSA-Zielorte',
    name_es      = 'Extrañas soluciones de estelas',
    name_fr      = 'Solutions de sillage anormales',
    name_ru      = 'Странные расчеты следа',
    name_uk      = 'Дивні розрахунки сліду',
    name_it      = 'Soluzioni di Scie Anomale',
    name_pt      = 'Soluções de rastro estranhas',
    name_ptbz    = 'Soluções de rastro estranhas'
WHERE symbol = 'wakesolutions';

UPDATE material_names
SET symbol = 'encryptioncodes'
WHERE symbol IS NULL
  AND name = 'Tagged Encryption Codes';
INSERT INTO material_names (symbol, name)
SELECT 'encryptioncodes', 'Tagged Encryption Codes'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'encryptioncodes');
UPDATE material_names
SET name         = 'Tagged Encryption Codes',
    materialType = 'Encoded',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Getaggte Verschlüsselungscodes',
    name_es      = 'Códigos de encriptación marcados',
    name_fr      = 'Clés de cryptage balisées',
    name_ru      = 'Меченые шифровальные коды',
    name_uk      = 'Мічені шифрувальні коди',
    name_it      = 'Codici di Cifratura Etichettati',
    name_pt      = 'Códigos de encriptação rotulados',
    name_ptbz    = 'Códigos de encriptação rotulados'
WHERE symbol = 'encryptioncodes';

UPDATE material_names
SET symbol = 'tg_interdictiondata'
WHERE symbol IS NULL
  AND name = 'Thargoid Interdiction Telemetry (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_interdictiondata', 'Thargoid Interdiction Telemetry'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_interdictiondata');
UPDATE material_names
SET name         = 'Thargoid Interdiction Telemetry',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = NULL,
    name_es      = 'Telemetría de Interdicción Thargoide',
    name_fr      = NULL,
    name_ru      = 'Телеметрия перехвата таргоидами',
    name_uk      = 'Телеметрія перехоплення таргоїдами',
    name_it      = 'Telemetria di Interdizione',
    name_pt      = NULL,
    name_ptbz    = NULL
WHERE symbol = 'tg_interdictiondata';

UPDATE material_names
SET symbol = 'tg_compositiondata'
WHERE symbol IS NULL
  AND name = 'Thargoid Material Composition Data';
INSERT INTO material_names (symbol, name)
SELECT 'tg_compositiondata', 'Thargoid Material Composition Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_compositiondata');
UPDATE material_names
SET name         = 'Thargoid Material Composition Data',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Materialzusammensetzungsdaten der Thargoiden',
    name_es      = 'Datos de composición material Thargoide',
    name_fr      = 'Données de composition de matériau thargoid',
    name_ru      = 'Данные о составе таргоидских материалов',
    name_uk      = 'Дані про склад таргоїдських матеріалів',
    name_it      = 'Dati sulla Composizione dei Materiali Thargoid',
    name_pt      = 'Dados de Composição de Material Thargoid',
    name_ptbz    = 'Dados de Composição de Material Thargoid'
WHERE symbol = 'tg_compositiondata';

UPDATE material_names
SET symbol = 'tg_residuedata'
WHERE symbol IS NULL
  AND name = 'Thargoid Residue Data';
INSERT INTO material_names (symbol, name)
SELECT 'tg_residuedata', 'Thargoid Residue Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_residuedata');
UPDATE material_names
SET name         = 'Thargoid Residue Data',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Thargoiden-Rückstandsdaten',
    name_es      = 'Datos residuales Thargoides',
    name_fr      = 'Données de résidu thargoid',
    name_ru      = 'Данные об осадке таргоидского происхождения',
    name_uk      = 'Дані про осад таргоїдського походження',
    name_it      = 'Dati di Residui Thargoid',
    name_pt      = 'Dados de Resíduos Thargoid',
    name_ptbz    = 'Dados de Resíduos Thargoid'
WHERE symbol = 'tg_residuedata';

UPDATE material_names
SET symbol = 'unknownshipsignature'
WHERE symbol IS NULL
  AND name = 'Thargoid Ship Signature';
INSERT INTO material_names (symbol, name)
SELECT 'unknownshipsignature', 'Thargoid Ship Signature'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'unknownshipsignature');
UPDATE material_names
SET name         = 'Thargoid Ship Signature',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Thargoiden-Schiffssignatur',
    name_es      = 'Firma térmica de nave Thargoide',
    name_fr      = 'Signature de vaisseau thargoid',
    name_ru      = 'Сигнатура таргоидского корабля',
    name_uk      = 'Сигнатура таргоїдського корабля',
    name_it      = 'Firma di Nave Thargoid',
    name_pt      = 'Assinatura de Nave Thargoid',
    name_ptbz    = 'Assinatura de Nave Thargoid'
WHERE symbol = 'unknownshipsignature';

UPDATE material_names
SET symbol = 'tg_structuraldata'
WHERE symbol IS NULL
  AND name = 'Thargoid Structural Data';
INSERT INTO material_names (symbol, name)
SELECT 'tg_structuraldata', 'Thargoid Structural Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_structuraldata');
UPDATE material_names
SET name         = 'Thargoid Structural Data',
    materialType = 'Encoded',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Thargoiden-Strukturdaten',
    name_es      = 'Datos estructurales Thargoides',
    name_fr      = 'Données de structure thargoid',
    name_ru      = 'Данные о структуре таргоидского объекта',
    name_uk      = 'Дані про структуру таргоїдського об''єкта',
    name_it      = 'Dati Strutturali Thargoid',
    name_pt      = 'Dados de Estrutura Thargoid',
    name_ptbz    = 'Dados de Estrutura Thargoid'
WHERE symbol = 'tg_structuraldata';

UPDATE material_names
SET symbol = 'unknownwakedata'
WHERE symbol IS NULL
  AND name = 'Thargoid Wake Data';
INSERT INTO material_names (symbol, name)
SELECT 'unknownwakedata', 'Thargoid Wake Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'unknownwakedata');
UPDATE material_names
SET name         = 'Thargoid Wake Data',
    materialType = 'Encoded',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Thargoiden-Sogwolkendaten',
    name_es      = 'Datos de estela Thargoide',
    name_fr      = 'Données de sillage thargoid',
    name_ru      = 'Данные следа таргоидского корабля',
    name_uk      = 'Дані сліду таргоїдського корабля',
    name_it      = 'Dati di Scia Thargoid',
    name_pt      = 'Dados de Rastro Thargoid',
    name_ptbz    = 'Dados de Rastro Thargoid'
WHERE symbol = 'unknownwakedata';

UPDATE material_names
SET symbol = 'emissiondata'
WHERE symbol IS NULL
  AND name = 'Unexpected Emission Data';
INSERT INTO material_names (symbol, name)
SELECT 'emissiondata', 'Unexpected Emission Data'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'emissiondata');
UPDATE material_names
SET name         = 'Unexpected Emission Data',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Unerwartete Emissionsdaten',
    name_es      = 'Datos de emisión inesperados',
    name_fr      = 'Données d’émissions inattendues',
    name_ru      = 'Неожиданные данные об излучении',
    name_uk      = 'Неочікувані дані про випромінювання',
    name_it      = 'Dati di Emissioni Anomale',
    name_pt      = 'Dados de emissão inesperados',
    name_ptbz    = 'Dados de emissão inesperados'
WHERE symbol = 'emissiondata';

UPDATE material_names
SET symbol = 'scanarchives'
WHERE symbol IS NULL
  AND name = 'Unidentified Scan Archives';
INSERT INTO material_names (symbol, name)
SELECT 'scanarchives', 'Unidentified Scan Archives'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'scanarchives');
UPDATE material_names
SET name         = 'Unidentified Scan Archives',
    materialType = 'Encoded',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Unidentifizierte Scan-Archive',
    name_es      = 'Archivos de escáner no identificados',
    name_fr      = 'Données d’analyse archivées non identifiées',
    name_ru      = 'Неопознанные архивы сканирования',
    name_uk      = 'Невпізнані архіви сканування',
    name_it      = 'Archivi di Scansioni Non Identificate',
    name_pt      = 'Arquivos de escaneamento não identificados',
    name_ptbz    = 'Arquivos de escaneamento não identificados'
WHERE symbol = 'scanarchives';

UPDATE material_names
SET symbol = 'unknown'
WHERE symbol IS NULL
  AND name = 'Unknown';
INSERT INTO material_names (symbol, name)
SELECT 'unknown', 'Unknown'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'unknown');
UPDATE material_names
SET name         = 'Unknown',
    materialType = 'Encoded',
    grade        = NULL,
    maxCapacity  = NULL,
    name_de      = 'Unbekannt',
    name_es      = 'Desconocido',
    name_fr      = 'Inconnu',
    name_ru      = 'Неизвестно',
    name_uk      = 'Невідомо',
    name_it      = 'Sconosciuto',
    name_pt      = 'Desconhecido',
    name_ptbz    = 'Desconhecido'
WHERE symbol = 'unknown';

UPDATE material_names
SET symbol = 'shielddensityreports'
WHERE symbol IS NULL
  AND name = 'Untypical Shield Scans';
INSERT INTO material_names (symbol, name)
SELECT 'shielddensityreports', 'Untypical Shield Scans'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'shielddensityreports');
UPDATE material_names
SET name         = 'Untypical Shield Scans',
    materialType = 'Encoded',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Untypische Schildscans',
    name_es      = 'Escáner de escudos atípico',
    name_fr      = 'Analyses de bouclier atypiques',
    name_ru      = 'Нетипичные данные сканирования щитов',
    name_uk      = 'Нетипові дані сканування щитів',
    name_it      = 'Scansioni di Scudi Anomale',
    name_pt      = 'Escaneamentos de escudo atípicos',
    name_ptbz    = 'Escaneamentos de escudo atípicos'
WHERE symbol = 'shielddensityreports';

UPDATE material_names
SET symbol = 'encryptedfiles'
WHERE symbol IS NULL
  AND name = 'Unusual Encrypted Files';
INSERT INTO material_names (symbol, name)
SELECT 'encryptedfiles', 'Unusual Encrypted Files'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'encryptedfiles');
UPDATE material_names
SET name         = 'Unusual Encrypted Files',
    materialType = 'Encoded',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Ungewöhnliche verschlüsselte Files',
    name_es      = 'Ficheros encriptados inusuales',
    name_fr      = 'Fichiers cryptés inhabituels',
    name_ru      = 'Особые зашифрованные файлы',
    name_uk      = 'Особливі зашифровані файли',
    name_it      = 'File Cifrati Insoliti',
    name_pt      = 'Arquivos criptografados incomuns',
    name_ptbz    = 'Arquivos criptografados incomuns'
WHERE symbol = 'encryptedfiles';


-- ============================ Manufactured ============================

UPDATE material_names
SET symbol = 'basicconductors'
WHERE symbol IS NULL
  AND name = 'Basic Conductors';
INSERT INTO material_names (symbol, name)
SELECT 'basicconductors', 'Basic Conductors'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'basicconductors');
UPDATE material_names
SET name         = 'Basic Conductors',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Einfache Leiter',
    name_es      = 'Conductores básicos',
    name_fr      = 'Conducteurs simples',
    name_ru      = 'Простые проводники',
    name_uk      = 'Прості провідники',
    name_it      = 'Conduttori di Base',
    name_pt      = 'Condutores básicos',
    name_ptbz    = 'Condutores básicos'
WHERE symbol = 'basicconductors';

UPDATE material_names
SET symbol = 'tg_biomechanicalconduits'
WHERE symbol IS NULL
  AND name = 'Bio-Mechanical Conduits (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_biomechanicalconduits', 'Bio-Mechanical Conduits'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_biomechanicalconduits');
UPDATE material_names
SET name         = 'Bio-Mechanical Conduits',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Biomechanische Leiter',
    name_es      = 'Conductos biomecánicos',
    name_fr      = 'Conduits biomécaniques',
    name_ru      = 'Биомеханические энергопроводники',
    name_uk      = 'Біомеханічні енергопроводи',
    name_it      = 'Condotti Bio‑Meccanici',
    name_pt      = 'Condutor Biomecânico',
    name_ptbz    = 'Condutor Biomecânico'
WHERE symbol = 'tg_biomechanicalconduits';

UPDATE material_names
SET symbol = 'biotechconductors'
WHERE symbol IS NULL
  AND name = 'Biotech Conductors';
INSERT INTO material_names (symbol, name)
SELECT 'biotechconductors', 'Biotech Conductors'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'biotechconductors');
UPDATE material_names
SET name         = 'Biotech Conductors',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Biotech-Leiter',
    name_es      = 'Conductores biotecnológicos',
    name_fr      = 'Conducteurs biotechniques',
    name_ru      = 'Биотехнические проводники',
    name_uk      = 'Біотехнічні провідники',
    name_it      = 'Conduttori Biotecnologici',
    name_pt      = 'Condutores biotecnológicos',
    name_ptbz    = 'Condutores biotecnológicos'
WHERE symbol = 'biotechconductors';

UPDATE material_names
SET symbol = 'tg_causticcrystal'
WHERE symbol IS NULL
  AND name = 'Caustic Crystal (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_causticcrystal', 'Caustic Crystal'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_causticcrystal');
UPDATE material_names
SET name         = 'Caustic Crystal',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = NULL,
    name_es      = 'Cristal Caústico',
    name_fr      = NULL,
    name_ru      = 'Едкий кристалл',
    name_uk      = 'Їдкий кристал',
    name_it      = 'Cristallo Caustico',
    name_pt      = NULL,
    name_ptbz    = NULL
WHERE symbol = 'tg_causticcrystal';

UPDATE material_names
SET symbol = 'tg_causticshard'
WHERE symbol IS NULL
  AND name = 'Caustic Shard (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_causticshard', 'Caustic Shard'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_causticshard');
UPDATE material_names
SET name         = 'Caustic Shard',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = NULL,
    name_es      = 'Fragmento Caústico',
    name_fr      = NULL,
    name_ru      = 'Едкий осколок',
    name_uk      = 'Їдкий уламок',
    name_it      = 'Scheggia Caustica',
    name_pt      = NULL,
    name_ptbz    = NULL
WHERE symbol = 'tg_causticshard';

UPDATE material_names
SET symbol = 'chemicaldistillery'
WHERE symbol IS NULL
  AND name = 'Chemical Distillery';
INSERT INTO material_names (symbol, name)
SELECT 'chemicaldistillery', 'Chemical Distillery'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'chemicaldistillery');
UPDATE material_names
SET name         = 'Chemical Distillery',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Chemiedestillerie',
    name_es      = 'Destilería química',
    name_fr      = 'Distillerie chimique',
    name_ru      = 'Оборудование для перегонки химикатов',
    name_uk      = 'Обладнання для перегонки хімікатів',
    name_it      = 'Distilleria Chimica',
    name_pt      = 'Destilaria química',
    name_ptbz    = 'Destilaria química'
WHERE symbol = 'chemicaldistillery';

UPDATE material_names
SET symbol = 'chemicalmanipulators'
WHERE symbol IS NULL
  AND name = 'Chemical Manipulators';
INSERT INTO material_names (symbol, name)
SELECT 'chemicalmanipulators', 'Chemical Manipulators'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'chemicalmanipulators');
UPDATE material_names
SET name         = 'Chemical Manipulators',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Chemische Manipulatoren',
    name_es      = 'Manipuladores químicos',
    name_fr      = 'Manipulateurs chimiques',
    name_ru      = 'Манипуляторы для работы с химикатами',
    name_uk      = 'Маніпулятори для роботи з хімікатами',
    name_it      = 'Manipolatori Chimici',
    name_pt      = 'Manipuladores químicos',
    name_ptbz    = 'Manipuladores químicos'
WHERE symbol = 'chemicalmanipulators';

UPDATE material_names
SET symbol = 'chemicalprocessors'
WHERE symbol IS NULL
  AND name = 'Chemical Processors';
INSERT INTO material_names (symbol, name)
SELECT 'chemicalprocessors', 'Chemical Processors'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'chemicalprocessors');
UPDATE material_names
SET name         = 'Chemical Processors',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Chemische Prozessoren',
    name_es      = 'Procesadores químicos',
    name_fr      = 'Processeurs chimiques',
    name_ru      = 'Оборудование для химобработки',
    name_uk      = 'Обладнання для хімобробки',
    name_it      = 'Processori Chimici',
    name_pt      = 'Processadores químicos',
    name_ptbz    = 'Processadores químicos'
WHERE symbol = 'chemicalprocessors';

UPDATE material_names
SET symbol = 'chemicalstorageunits'
WHERE symbol IS NULL
  AND name = 'Chemical Storage Units';
INSERT INTO material_names (symbol, name)
SELECT 'chemicalstorageunits', 'Chemical Storage Units'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'chemicalstorageunits');
UPDATE material_names
SET name         = 'Chemical Storage Units',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Lagerungseinheiten für Chemiestoffe',
    name_es      = 'Unidades de almacenamiento químico',
    name_fr      = 'Unités de stockage chimique',
    name_ru      = 'Контейнеры для химикатов',
    name_uk      = 'Контейнери для хімікатів',
    name_it      = 'Unità di Stoccaggio Chimico',
    name_pt      = 'Unidades de armazenamento químico',
    name_ptbz    = 'Unidades de armazenamento químico'
WHERE symbol = 'chemicalstorageunits';

UPDATE material_names
SET symbol = 'compactcomposites'
WHERE symbol IS NULL
  AND name = 'Compact Composites';
INSERT INTO material_names (symbol, name)
SELECT 'compactcomposites', 'Compact Composites'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'compactcomposites');
UPDATE material_names
SET name         = 'Compact Composites',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Kompaktkomposite',
    name_es      = 'Compuestos compactos',
    name_fr      = 'Composites compacts',
    name_ru      = 'Спрессованные композиты',
    name_uk      = 'Спресовані композити',
    name_it      = 'Compositi Compatti',
    name_pt      = 'Compostos compactos',
    name_ptbz    = 'Compostos compactos'
WHERE symbol = 'compactcomposites';

UPDATE material_names
SET symbol = 'compoundshielding'
WHERE symbol IS NULL
  AND name = 'Compound Shielding';
INSERT INTO material_names (symbol, name)
SELECT 'compoundshielding', 'Compound Shielding'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'compoundshielding');
UPDATE material_names
SET name         = 'Compound Shielding',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Verbundschilde',
    name_es      = 'Escudos compuestos',
    name_fr      = 'Protection composite',
    name_ru      = 'Многоступенчатая защита',
    name_uk      = 'Багатоступеневий захист',
    name_it      = 'Schermature Composite',
    name_pt      = 'Proteção composta',
    name_ptbz    = 'Proteção composta'
WHERE symbol = 'compoundshielding';

UPDATE material_names
SET symbol = 'conductiveceramics'
WHERE symbol IS NULL
  AND name = 'Conductive Ceramics';
INSERT INTO material_names (symbol, name)
SELECT 'conductiveceramics', 'Conductive Ceramics'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'conductiveceramics');
UPDATE material_names
SET name         = 'Conductive Ceramics',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Elektrokeramiken',
    name_es      = 'Cerámicas conductivas',
    name_fr      = 'Conducteurs en céramique',
    name_ru      = 'Проводящая керамика',
    name_uk      = 'Провідна кераміка',
    name_it      = 'Ceramiche Conduttive',
    name_pt      = 'Cerâmicas condutoras',
    name_ptbz    = 'Cerâmicas condutoras'
WHERE symbol = 'conductiveceramics';

UPDATE material_names
SET symbol = 'conductivecomponents'
WHERE symbol IS NULL
  AND name = 'Conductive Components';
INSERT INTO material_names (symbol, name)
SELECT 'conductivecomponents', 'Conductive Components'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'conductivecomponents');
UPDATE material_names
SET name         = 'Conductive Components',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Leitfähige Komponenten',
    name_es      = 'Componentes conductivos',
    name_fr      = 'Composants conducteurs',
    name_ru      = 'Проводящие компоненты',
    name_uk      = 'Провідні компоненти',
    name_it      = 'Componenti Conduttivi',
    name_pt      = 'Componentes condutores',
    name_ptbz    = 'Componentes condutores'
WHERE symbol = 'conductivecomponents';

UPDATE material_names
SET symbol = 'conductivepolymers'
WHERE symbol IS NULL
  AND name = 'Conductive Polymers';
INSERT INTO material_names (symbol, name)
SELECT 'conductivepolymers', 'Conductive Polymers'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'conductivepolymers');
UPDATE material_names
SET name         = 'Conductive Polymers',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Leitfähige Polymere',
    name_es      = 'Polímeros conductivos',
    name_fr      = 'Conducteurs en polymères',
    name_ru      = 'Проводящие полимеры',
    name_uk      = 'Провідні полімери',
    name_it      = 'Polimeri conduttivi',
    name_pt      = 'Polímeros condutores',
    name_ptbz    = 'Polímeros condutores'
WHERE symbol = 'conductivepolymers';

UPDATE material_names
SET symbol = 'configurablecomponents'
WHERE symbol IS NULL
  AND name = 'Configurable Components';
INSERT INTO material_names (symbol, name)
SELECT 'configurablecomponents', 'Configurable Components'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'configurablecomponents');
UPDATE material_names
SET name         = 'Configurable Components',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Konfigurierbare Komponenten',
    name_es      = 'Componentes configurables',
    name_fr      = 'Composants paramétrables',
    name_ru      = 'Настраиваемые компоненты',
    name_uk      = 'Налаштовувані компоненти',
    name_it      = 'Componenti Configurabili',
    name_pt      = 'Componentes configuráveis',
    name_ptbz    = 'Componentes configuráveis'
WHERE symbol = 'configurablecomponents';

UPDATE material_names
SET symbol = 'fedcorecomposites'
WHERE symbol IS NULL
  AND name = 'Core Dynamics Composites';
INSERT INTO material_names (symbol, name)
SELECT 'fedcorecomposites', 'Core Dynamics Composites'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'fedcorecomposites');
UPDATE material_names
SET name         = 'Core Dynamics Composites',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Core Dynamics Kompositwerkstoffe',
    name_es      = 'Compuestos de Core Dynamics',
    name_fr      = 'Composites Core Dynamics',
    name_ru      = 'Композиты Core Dynamics',
    name_uk      = 'Композити Core Dynamics',
    name_it      = 'Compositi Core Dynamics',
    name_pt      = 'Compostos da Core Dynamics',
    name_ptbz    = 'Compostos da Core Dynamics'
WHERE symbol = 'fedcorecomposites';

UPDATE material_names
SET symbol = 'tg_causticgeneratorparts'
WHERE symbol IS NULL
  AND name = 'Corrosive Mechanisms (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_causticgeneratorparts', 'Corrosive Mechanisms'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_causticgeneratorparts');
UPDATE material_names
SET name         = 'Corrosive Mechanisms',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = NULL,
    name_es      = 'Mecanismos Corrosivos',
    name_fr      = NULL,
    name_ru      = 'Разъедающие механизмы',
    name_uk      = 'Роз''їдаючі механізми',
    name_it      = 'Meccanismi Corrosivi',
    name_pt      = NULL,
    name_ptbz    = NULL
WHERE symbol = 'tg_causticgeneratorparts';

UPDATE material_names
SET symbol = 'crystalshards'
WHERE symbol IS NULL
  AND name = 'Crystal Shards';
INSERT INTO material_names (symbol, name)
SELECT 'crystalshards', 'Crystal Shards'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'crystalshards');
UPDATE material_names
SET name         = 'Crystal Shards',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Kristallscherben',
    name_es      = 'Piedras de cristal',
    name_fr      = 'Éclats de cristal',
    name_ru      = 'Осколки кристаллов',
    name_uk      = 'Уламки кристалів',
    name_it      = 'Schegge Cristalline',
    name_pt      = 'Fragmentos de cristais',
    name_ptbz    = 'Fragmentos de cristais'
WHERE symbol = 'crystalshards';

UPDATE material_names
SET symbol = 'electrochemicalarrays'
WHERE symbol IS NULL
  AND name = 'Electrochemical Arrays';
INSERT INTO material_names (symbol, name)
SELECT 'electrochemicalarrays', 'Electrochemical Arrays'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'electrochemicalarrays');
UPDATE material_names
SET name         = 'Electrochemical Arrays',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Elektrochemische Detektoren',
    name_es      = 'Matriz electroquímica',
    name_fr      = 'Réseaux électrochimiques',
    name_ru      = 'Электрохимические массивы',
    name_uk      = 'Електрохімічні масиви',
    name_it      = 'Array Elettrochimici',
    name_pt      = 'Matrizes eletroquímicas',
    name_ptbz    = 'Matrizes eletroquímicas'
WHERE symbol = 'electrochemicalarrays';

UPDATE material_names
SET symbol = 'exquisitefocuscrystals'
WHERE symbol IS NULL
  AND name = 'Exquisite Focus Crystals';
INSERT INTO material_names (symbol, name)
SELECT 'exquisitefocuscrystals', 'Exquisite Focus Crystals'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'exquisitefocuscrystals');
UPDATE material_names
SET name         = 'Exquisite Focus Crystals',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Erlesene Laserkristalle',
    name_es      = 'Cristales de enfoque exquisitos',
    name_fr      = 'Cristaux de focalisation sans défaut',
    name_ru      = 'Отборные фокусировочные кристаллы',
    name_uk      = 'Добірні фокусувальні кристали',
    name_it      = 'Cristalli di Focalizzazione Preziosi',
    name_pt      = 'Cristais de focalização fino',
    name_ptbz    = 'Cristais de focalização fino'
WHERE symbol = 'exquisitefocuscrystals';

UPDATE material_names
SET symbol = 'filamentcomposites'
WHERE symbol IS NULL
  AND name = 'Filament Composites';
INSERT INTO material_names (symbol, name)
SELECT 'filamentcomposites', 'Filament Composites'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'filamentcomposites');
UPDATE material_names
SET name         = 'Filament Composites',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Filament-Komposite',
    name_es      = 'Compuestos de filamentos',
    name_fr      = 'Composites filamentaires',
    name_ru      = 'Волокнистые композиты',
    name_uk      = 'Волокнисті композити',
    name_it      = 'Compositi a Filamenti',
    name_pt      = 'Compostos filamentares',
    name_ptbz    = 'Compostos filamentares'
WHERE symbol = 'filamentcomposites';

UPDATE material_names
SET symbol = 'uncutfocuscrystals'
WHERE symbol IS NULL
  AND name = 'Flawed Focus Crystals';
INSERT INTO material_names (symbol, name)
SELECT 'uncutfocuscrystals', 'Flawed Focus Crystals'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'uncutfocuscrystals');
UPDATE material_names
SET name         = 'Flawed Focus Crystals',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Fehlerhafte Fokuskristalle',
    name_es      = 'Cristales de convergencia imperfectos',
    name_fr      = 'Cristaux de focalisation imparfaits',
    name_ru      = 'Поврежденные фокусировочные кристаллы',
    name_uk      = 'Пошкоджені фокусувальні кристали',
    name_it      = 'Cristalli di Focalizzazione Difettosi',
    name_pt      = 'Cristais de focalização falhos',
    name_ptbz    = 'Cristais de focalização falhos'
WHERE symbol = 'uncutfocuscrystals';

UPDATE material_names
SET symbol = 'focuscrystals'
WHERE symbol IS NULL
  AND name = 'Focus Crystals';
INSERT INTO material_names (symbol, name)
SELECT 'focuscrystals', 'Focus Crystals'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'focuscrystals');
UPDATE material_names
SET name         = 'Focus Crystals',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Laserkristalle',
    name_es      = 'Cristales de enfoque',
    name_fr      = 'Cristaux de focalisation',
    name_ru      = 'Фокусировочные кристаллы',
    name_uk      = 'Фокусувальні кристали',
    name_it      = 'Cristalli di Focalizzazione',
    name_pt      = 'Cristais de focalização',
    name_ptbz    = 'Cristais de focalização'
WHERE symbol = 'focuscrystals';

UPDATE material_names
SET symbol = 'galvanisingalloys'
WHERE symbol IS NULL
  AND name = 'Galvanising Alloys';
INSERT INTO material_names (symbol, name)
SELECT 'galvanisingalloys', 'Galvanising Alloys'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'galvanisingalloys');
UPDATE material_names
SET name         = 'Galvanising Alloys',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Galvanisierende Legierungen',
    name_es      = 'Aleaciones galvanizadas',
    name_fr      = 'Alliages galvaniques',
    name_ru      = 'Сплавы для гальванизации',
    name_uk      = 'Сплави для гальванізації',
    name_it      = 'Leghe Galvanizzate',
    name_pt      = 'Ligas galvanizadas',
    name_ptbz    = 'Ligas galvanizadas'
WHERE symbol = 'galvanisingalloys';

UPDATE material_names
SET symbol = 'gridresistors'
WHERE symbol IS NULL
  AND name = 'Grid Resistors';
INSERT INTO material_names (symbol, name)
SELECT 'gridresistors', 'Grid Resistors'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'gridresistors');
UPDATE material_names
SET name         = 'Grid Resistors',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Gitterwiderstände',
    name_es      = 'Red resistiva',
    name_fr      = 'Résistance à grille',
    name_ru      = 'Наборные резисторы',
    name_uk      = 'Набірні резистори',
    name_it      = 'Resistori a Griglia',
    name_pt      = 'Resistores de grade',
    name_ptbz    = 'Resistores de grade'
WHERE symbol = 'gridresistors';

UPDATE material_names
SET symbol = 'guardian_powercell'
WHERE symbol IS NULL
  AND name = 'Guardian Power Cell';
INSERT INTO material_names (symbol, name)
SELECT 'guardian_powercell', 'Guardian Power Cell'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'guardian_powercell');
UPDATE material_names
SET name         = 'Guardian Power Cell',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Guardian-Energiezelle',
    name_es      = 'Célula de energía de guardián',
    name_fr      = 'Cellule d’énergie - Guardians',
    name_ru      = 'Энергоячейка Стражей',
    name_uk      = 'Енергокомірка Стражів',
    name_it      = 'Cella di Energia Guardian',
    name_pt      = 'Bateria Guardian',
    name_ptbz    = 'Bateria Guardian'
WHERE symbol = 'guardian_powercell';

UPDATE material_names
SET symbol = 'guardian_powerconduit'
WHERE symbol IS NULL
  AND name = 'Guardian Power Conduit';
INSERT INTO material_names (symbol, name)
SELECT 'guardian_powerconduit', 'Guardian Power Conduit'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'guardian_powerconduit');
UPDATE material_names
SET name         = 'Guardian Power Conduit',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Guardian-Energieleiter',
    name_es      = 'Conducto de energía de guardián',
    name_fr      = 'Conduit d’énergie - Guardians',
    name_ru      = 'Энергопроводники Стражей',
    name_uk      = 'Енергопроводи Стражів',
    name_it      = 'Condotto di Energia Guardian',
    name_pt      = 'Condutores de potência Guardian',
    name_ptbz    = 'Condutores de potência Guardian'
WHERE symbol = 'guardian_powerconduit';

UPDATE material_names
SET symbol = 'guardian_sentinel_weaponparts'
WHERE symbol IS NULL
  AND name = 'Guardian Sentinel Weapon Parts';
INSERT INTO material_names (symbol, name)
SELECT 'guardian_sentinel_weaponparts', 'Guardian Sentinel Weapon Parts'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'guardian_sentinel_weaponparts');
UPDATE material_names
SET name         = 'Guardian Sentinel Weapon Parts',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Guardian-Wache-Waffenteile',
    name_es      = 'Piezas de armamento de centinela guardián',
    name_fr      = 'Pièce d’armement de sentinelle - Guardians',
    name_ru      = 'Детали вооружения часовых Стражей',
    name_uk      = 'Деталі озброєння вартових Стражів',
    name_it      = 'Componenti Arma della Sentinella Guardian',
    name_pt      = 'Peças de armas Sentinela Guardian',
    name_ptbz    = 'Peças de armas Sentinela Guardian'
WHERE symbol = 'guardian_sentinel_weaponparts';

UPDATE material_names
SET symbol = 'guardian_techcomponent'
WHERE symbol IS NULL
  AND name = 'Guardian Technology Component';
INSERT INTO material_names (symbol, name)
SELECT 'guardian_techcomponent', 'Guardian Technology Component'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'guardian_techcomponent');
UPDATE material_names
SET name         = 'Guardian Technology Component',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Guardian-Technologiekomponenten',
    name_es      = 'Componente tecnológico de guardián',
    name_fr      = 'Composant technologique - Guardians',
    name_ru      = 'Компоненты технологий Стражей',
    name_uk      = 'Компоненти технологій Стражів',
    name_it      = 'Componente Tecnologico Guardian',
    name_pt      = 'Componente de tecnologia Guardian',
    name_ptbz    = 'Componente de tecnologia Guardian'
WHERE symbol = 'guardian_techcomponent';

UPDATE material_names
SET symbol = 'guardian_sentinel_wreckagecomponents'
WHERE symbol IS NULL
  AND name = 'Guardian Wreckage Components';
INSERT INTO material_names (symbol, name)
SELECT 'guardian_sentinel_wreckagecomponents', 'Guardian Wreckage Components'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'guardian_sentinel_wreckagecomponents');
UPDATE material_names
SET name         = 'Guardian Wreckage Components',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Guardian-Wache-Wrackteilkomponenten',
    name_es      = 'Restos de accidentes de centinela guardián',
    name_fr      = 'Débris de sentinelle - Guardians',
    name_ru      = 'Обломки кораблекрушения Стражей',
    name_uk      = 'Уламки корабельної аварії Стражів',
    name_it      = 'Componenti dei Relitti Guardian',
    name_pt      = 'Componentes de destroços Sentinela Guardian',
    name_ptbz    = 'Componentes de destroços Sentinela Guardian'
WHERE symbol = 'guardian_sentinel_wreckagecomponents';

UPDATE material_names
SET symbol = 'tg_abrasion03'
WHERE symbol IS NULL
  AND name = 'Hardened Surface Fragments';
INSERT INTO material_names (symbol, name)
SELECT 'tg_abrasion03', 'Hardened Surface Fragments'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_abrasion03');
UPDATE material_names
SET name         = 'Hardened Surface Fragments',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = NULL,
    name_es      = 'Fragmentos de superficie endurecida',
    name_fr      = 'Fragments superficiels durcis',
    name_ru      = 'Окаменелые фрагменты поверхности',
    name_uk      = 'Затверділі фрагменти поверхні',
    name_it      = 'Frammenti di Superficie Indurita',
    name_pt      = 'Fragmentos de superfície endurecida',
    name_ptbz    = 'Fragmentos de superfície endurecida'
WHERE symbol = 'tg_abrasion03';

UPDATE material_names
SET symbol = 'heatconductionwiring'
WHERE symbol IS NULL
  AND name = 'Heat Conduction Wiring';
INSERT INTO material_names (symbol, name)
SELECT 'heatconductionwiring', 'Heat Conduction Wiring'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'heatconductionwiring');
UPDATE material_names
SET name         = 'Heat Conduction Wiring',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Wärmeleitungsverdrahtung',
    name_es      = 'Cableado de conducción calorífica',
    name_fr      = 'Câblage de conduction thermique',
    name_ru      = 'Теплопроводящие провода',
    name_uk      = 'Теплопровідні дроти',
    name_it      = 'Cablaggio Conduttivo Termico',
    name_pt      = 'Fiação de condução térmica',
    name_ptbz    = 'Fiação de condução térmica'
WHERE symbol = 'heatconductionwiring';

UPDATE material_names
SET symbol = 'heatdispersionplate'
WHERE symbol IS NULL
  AND name = 'Heat Dispersion Plate';
INSERT INTO material_names (symbol, name)
SELECT 'heatdispersionplate', 'Heat Dispersion Plate'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'heatdispersionplate');
UPDATE material_names
SET name         = 'Heat Dispersion Plate',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Wärmeverteilungsplatte',
    name_es      = 'Placa de dispersión de calor',
    name_fr      = 'Plaque de dissipation thermique',
    name_ru      = 'Теплорассеивающая пластина',
    name_uk      = 'Теплорозсіювальна пластина',
    name_it      = 'Piastra di Dispersione Termica',
    name_pt      = 'Placa de dispersão térmica',
    name_ptbz    = 'Placa de dispersão térmica'
WHERE symbol = 'heatdispersionplate';

UPDATE material_names
SET symbol = 'heatexchangers'
WHERE symbol IS NULL
  AND name = 'Heat Exchangers';
INSERT INTO material_names (symbol, name)
SELECT 'heatexchangers', 'Heat Exchangers'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'heatexchangers');
UPDATE material_names
SET name         = 'Heat Exchangers',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Wärmeaustauscher',
    name_es      = 'Intercambiadores de calor',
    name_fr      = 'Échangeurs de chaleur',
    name_ru      = 'Теплообменные агрегаты',
    name_uk      = 'Теплообмінні агрегати',
    name_it      = 'Scambiatori di Calore',
    name_pt      = 'Trocadores térmicos',
    name_ptbz    = 'Trocadores térmicos'
WHERE symbol = 'heatexchangers';

UPDATE material_names
SET symbol = 'tg_abrasion01'
WHERE symbol IS NULL
  AND name = 'Heat Exposure Specimen';
INSERT INTO material_names (symbol, name)
SELECT 'tg_abrasion01', 'Heat Exposure Specimen'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_abrasion01');
UPDATE material_names
SET name         = 'Heat Exposure Specimen',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = NULL,
    name_es      = 'Especimen de Exposición Térmica',
    name_fr      = 'Spécimen exposé à la chaleur',
    name_ru      = 'Образец теплового воздействия',
    name_uk      = 'Зразок теплового впливу',
    name_it      = 'Campione di Esposizione Termica',
    name_pt      = 'Espécime de exposição ao calor',
    name_ptbz    = 'Espécime de exposição ao calor'
WHERE symbol = 'tg_abrasion01';

UPDATE material_names
SET symbol = 'heatresistantceramics'
WHERE symbol IS NULL
  AND name = 'Heat Resistant Ceramics';
INSERT INTO material_names (symbol, name)
SELECT 'heatresistantceramics', 'Heat Resistant Ceramics'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'heatresistantceramics');
UPDATE material_names
SET name         = 'Heat Resistant Ceramics',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Hitzefeste Keramik',
    name_es      = 'Cerámicas resistentes al calor',
    name_fr      = 'Céramiques résistantes à la chaleur',
    name_ru      = 'Жаропрочная керамика',
    name_uk      = 'Жаростійка кераміка',
    name_it      = 'Ceramiche Resistenti al Calore',
    name_pt      = 'Cerâmicas termoresistentes',
    name_ptbz    = 'Cerâmicas termoresistentes'
WHERE symbol = 'heatresistantceramics';

UPDATE material_names
SET symbol = 'heatvanes'
WHERE symbol IS NULL
  AND name = 'Heat Vanes';
INSERT INTO material_names (symbol, name)
SELECT 'heatvanes', 'Heat Vanes'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'heatvanes');
UPDATE material_names
SET name         = 'Heat Vanes',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Wärmeleitbleche',
    name_es      = 'Palas térmicas',
    name_fr      = 'Vannes thermiques',
    name_ru      = 'Тепловые заслонки',
    name_uk      = 'Теплові заслінки',
    name_it      = 'Alette di Dispersione Termica',
    name_pt      = 'Ventoinha térmica',
    name_ptbz    = 'Ventoinha térmica'
WHERE symbol = 'heatvanes';

UPDATE material_names
SET symbol = 'highdensitycomposites'
WHERE symbol IS NULL
  AND name = 'High Density Composites';
INSERT INTO material_names (symbol, name)
SELECT 'highdensitycomposites', 'High Density Composites'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'highdensitycomposites');
UPDATE material_names
SET name         = 'High Density Composites',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Komposite hoher Dichte',
    name_es      = 'Compuestos de alta densidad',
    name_fr      = 'Composites à haute densité',
    name_ru      = 'Высокоплотностные композиты',
    name_uk      = 'Високощільні композити',
    name_it      = 'Compositi ad Alta Densità',
    name_pt      = 'Compostos de alta densidade',
    name_ptbz    = 'Compostos de alta densidade'
WHERE symbol = 'highdensitycomposites';

UPDATE material_names
SET symbol = 'hybridcapacitors'
WHERE symbol IS NULL
  AND name = 'Hybrid Capacitors';
INSERT INTO material_names (symbol, name)
SELECT 'hybridcapacitors', 'Hybrid Capacitors'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'hybridcapacitors');
UPDATE material_names
SET name         = 'Hybrid Capacitors',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Hybridkondensatoren',
    name_es      = 'Capacitadores híbridos',
    name_fr      = 'Condensateurs hybrides',
    name_ru      = 'Гибридные конденсаторы',
    name_uk      = 'Гібридні конденсатори',
    name_it      = 'Capacitori Ibridi',
    name_pt      = 'Capacitores híbridos',
    name_ptbz    = 'Capacitores híbridos'
WHERE symbol = 'hybridcapacitors';

UPDATE material_names
SET symbol = 'imperialshielding'
WHERE symbol IS NULL
  AND name = 'Imperial Shielding';
INSERT INTO material_names (symbol, name)
SELECT 'imperialshielding', 'Imperial Shielding'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'imperialshielding');
UPDATE material_names
SET name         = 'Imperial Shielding',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Imperiale Schilde',
    name_es      = 'Escudos imperiales',
    name_fr      = 'Protection impériale',
    name_ru      = 'Имперская защита',
    name_uk      = 'Імперський захист',
    name_it      = 'Schermatura Imperiale',
    name_pt      = 'Proteção imperial',
    name_ptbz    = 'Proteção imperial'
WHERE symbol = 'imperialshielding';

UPDATE material_names
SET symbol = 'improvisedcomponents'
WHERE symbol IS NULL
  AND name = 'Improvised Components';
INSERT INTO material_names (symbol, name)
SELECT 'improvisedcomponents', 'Improvised Components'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'improvisedcomponents');
UPDATE material_names
SET name         = 'Improvised Components',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Behelfskomponenten',
    name_es      = 'Componentes improvisados',
    name_fr      = 'Composants improvisés',
    name_ru      = 'Кустарные компоненты',
    name_uk      = 'Кустарні компоненти',
    name_it      = 'Componenti Improvvisati',
    name_pt      = 'Componentes improvisados',
    name_ptbz    = 'Componentes improvisados'
WHERE symbol = 'improvisedcomponents';

UPDATE material_names
SET symbol = 'mechanicalcomponents'
WHERE symbol IS NULL
  AND name = 'Mechanical Components';
INSERT INTO material_names (symbol, name)
SELECT 'mechanicalcomponents', 'Mechanical Components'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'mechanicalcomponents');
UPDATE material_names
SET name         = 'Mechanical Components',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Mechanische Komponenten',
    name_es      = 'Componentes mecánicos',
    name_fr      = 'Composants mécaniques',
    name_ru      = 'Механические компоненты',
    name_uk      = 'Механічні компоненти',
    name_it      = 'Componenti Meccanici',
    name_pt      = 'Componentes mecânicos',
    name_ptbz    = 'Componentes mecânicos'
WHERE symbol = 'mechanicalcomponents';

UPDATE material_names
SET symbol = 'mechanicalequipment'
WHERE symbol IS NULL
  AND name = 'Mechanical Equipment';
INSERT INTO material_names (symbol, name)
SELECT 'mechanicalequipment', 'Mechanical Equipment'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'mechanicalequipment');
UPDATE material_names
SET name         = 'Mechanical Equipment',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Mechanisches Equipment',
    name_es      = 'Equipamiento mecánico',
    name_fr      = 'Équipement mécanique',
    name_ru      = 'Механическое оборудование',
    name_uk      = 'Механічне обладнання',
    name_it      = 'Equipaggiamento Meccanico',
    name_pt      = 'Equipamento mecânico',
    name_ptbz    = 'Equipamento mecânico'
WHERE symbol = 'mechanicalequipment';

UPDATE material_names
SET symbol = 'mechanicalscrap'
WHERE symbol IS NULL
  AND name = 'Mechanical Scrap';
INSERT INTO material_names (symbol, name)
SELECT 'mechanicalscrap', 'Mechanical Scrap'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'mechanicalscrap');
UPDATE material_names
SET name         = 'Mechanical Scrap',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Mechanischer Schrott',
    name_es      = 'Chatarra mecánica',
    name_fr      = 'Ferraille mécanique',
    name_ru      = 'Механические отходы',
    name_uk      = 'Механічні відходи',
    name_it      = 'Rottami Meccanici',
    name_pt      = 'Sucata mecânica',
    name_ptbz    = 'Sucata mecânica'
WHERE symbol = 'mechanicalscrap';

UPDATE material_names
SET symbol = 'militarygradealloys'
WHERE symbol IS NULL
  AND name = 'Military Grade Alloys';
INSERT INTO material_names (symbol, name)
SELECT 'militarygradealloys', 'Military Grade Alloys'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'militarygradealloys');
UPDATE material_names
SET name         = 'Military Grade Alloys',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Militärqualitätslegierungen',
    name_es      = 'Aleaciones de grado militar',
    name_fr      = 'Alliages militaires',
    name_ru      = 'Сплавы военного класса',
    name_uk      = 'Сплави військового класу',
    name_it      = 'Leghe di Grado Militare',
    name_pt      = 'Ligas nível militar',
    name_ptbz    = 'Ligas nível militar'
WHERE symbol = 'militarygradealloys';

UPDATE material_names
SET symbol = 'militarysupercapacitors'
WHERE symbol IS NULL
  AND name = 'Military Supercapacitors';
INSERT INTO material_names (symbol, name)
SELECT 'militarysupercapacitors', 'Military Supercapacitors'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'militarysupercapacitors');
UPDATE material_names
SET name         = 'Military Supercapacitors',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Militärische Superkondensatoren',
    name_es      = 'Supercapacitadores militares',
    name_fr      = 'Supercondensateurs militaires',
    name_ru      = 'Военные суперконденсаторы',
    name_uk      = 'Військові суперконденсатори',
    name_it      = 'Supercapacitori Militari',
    name_pt      = 'Supercapacitores militares',
    name_ptbz    = 'Supercapacitores militares'
WHERE symbol = 'militarysupercapacitors';

UPDATE material_names
SET symbol = 'pharmaceuticalisolators'
WHERE symbol IS NULL
  AND name = 'Pharmaceutical Isolators';
INSERT INTO material_names (symbol, name)
SELECT 'pharmaceuticalisolators', 'Pharmaceutical Isolators'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'pharmaceuticalisolators');
UPDATE material_names
SET name         = 'Pharmaceutical Isolators',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Pharmazeutische Isolatoren',
    name_es      = 'Aislantes farmacéuticos',
    name_fr      = 'Isolants pharmaceutiques',
    name_ru      = 'Фармацевтические изоляционные материалы',
    name_uk      = 'Фармацевтичні ізоляційні матеріали',
    name_it      = 'Isolatori Farmaceutici',
    name_pt      = 'Isolantes farmacêuticos',
    name_ptbz    = 'Isolantes farmacêuticos'
WHERE symbol = 'pharmaceuticalisolators';

UPDATE material_names
SET symbol = 'phasealloys'
WHERE symbol IS NULL
  AND name = 'Phase Alloys';
INSERT INTO material_names (symbol, name)
SELECT 'phasealloys', 'Phase Alloys'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'phasealloys');
UPDATE material_names
SET name         = 'Phase Alloys',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Phasenlegierungen',
    name_es      = 'Aleaciones de fase',
    name_fr      = 'Alliages de phase',
    name_ru      = 'Фазовые сплавы',
    name_uk      = 'Фазові сплави',
    name_it      = 'Leghe Fase',
    name_pt      = 'Ligas de fase',
    name_ptbz    = 'Ligas de fase'
WHERE symbol = 'phasealloys';

UPDATE material_names
SET symbol = 'tg_abrasion02'
WHERE symbol IS NULL
  AND name = 'Phasing Membrane Residue';
INSERT INTO material_names (symbol, name)
SELECT 'tg_abrasion02', 'Phasing Membrane Residue'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_abrasion02');
UPDATE material_names
SET name         = 'Phasing Membrane Residue',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Phasenmembranreste',
    name_es      = 'Residuo de Membrana Fásica',
    name_fr      = NULL,
    name_ru      = 'Остаток фазирующей мембраны',
    name_uk      = 'Залишок фазувальної мембрани',
    name_it      = 'Residuo di Membrana Fase',
    name_pt      = 'Resíduo de membrana faseada',
    name_ptbz    = 'Resíduo de membrana faseada'
WHERE symbol = 'tg_abrasion02';

UPDATE material_names
SET symbol = 'polymercapacitors'
WHERE symbol IS NULL
  AND name = 'Polymer Capacitors';
INSERT INTO material_names (symbol, name)
SELECT 'polymercapacitors', 'Polymer Capacitors'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'polymercapacitors');
UPDATE material_names
SET name         = 'Polymer Capacitors',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Polymerkondensatoren',
    name_es      = 'Capacitadores de polímeros',
    name_fr      = 'Condensateurs en polymères',
    name_ru      = 'Полимерные конденсаторы',
    name_uk      = 'Полімерні конденсатори',
    name_it      = 'Capacitori Polimerici',
    name_pt      = 'Capacitores de polímeros',
    name_ptbz    = 'Capacitores de polímeros'
WHERE symbol = 'polymercapacitors';

UPDATE material_names
SET symbol = 'precipitatedalloys'
WHERE symbol IS NULL
  AND name = 'Precipitated Alloys';
INSERT INTO material_names (symbol, name)
SELECT 'precipitatedalloys', 'Precipitated Alloys'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'precipitatedalloys');
UPDATE material_names
SET name         = 'Precipitated Alloys',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Gehärtete Legierungen',
    name_es      = 'Aleaciones de precipitación',
    name_fr      = 'Alliages précipités',
    name_ru      = 'Осажденные сплавы',
    name_uk      = 'Осаджені сплави',
    name_it      = 'Leghe Precipitate',
    name_pt      = 'Ligas precipitadas',
    name_ptbz    = 'Ligas precipitadas'
WHERE symbol = 'precipitatedalloys';

UPDATE material_names
SET symbol = 'fedproprietarycomposites'
WHERE symbol IS NULL
  AND name = 'Proprietary Composites';
INSERT INTO material_names (symbol, name)
SELECT 'fedproprietarycomposites', 'Proprietary Composites'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'fedproprietarycomposites');
UPDATE material_names
SET name         = 'Proprietary Composites',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Kompositwerkstoffe',
    name_es      = 'Compuestos con patente',
    name_fr      = 'Composites brevetés',
    name_ru      = 'Патентованные композиты',
    name_uk      = 'Патентовані композити',
    name_it      = 'Compositi Proprietari',
    name_pt      = 'Compostos proprietários',
    name_ptbz    = 'Compostos proprietários'
WHERE symbol = 'fedproprietarycomposites';

UPDATE material_names
SET symbol = 'tg_propulsionelement'
WHERE symbol IS NULL
  AND name = 'Propulsion Elements (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_propulsionelement', 'Propulsion Elements'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_propulsionelement');
UPDATE material_names
SET name         = 'Propulsion Elements',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Schubantriebelemente',
    name_es      = 'Elementos de propulsión',
    name_fr      = 'Éléments de propulsion',
    name_ru      = 'Реактивные элементы',
    name_uk      = 'Реактивні елементи',
    name_it      = 'Elementi di Propulsione',
    name_pt      = 'Elementos de Propulsão',
    name_ptbz    = 'Elementos de Propulsão'
WHERE symbol = 'tg_propulsionelement';

UPDATE material_names
SET symbol = 'protoheatradiators'
WHERE symbol IS NULL
  AND name = 'Proto Heat Radiators';
INSERT INTO material_names (symbol, name)
SELECT 'protoheatradiators', 'Proto Heat Radiators'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'protoheatradiators');
UPDATE material_names
SET name         = 'Proto Heat Radiators',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Proto-Wärmestrahler',
    name_es      = 'Protorradiadores térmicos',
    name_fr      = 'Proto-radiateurs',
    name_ru      = 'Прототипы теплоизлучателей',
    name_uk      = 'Прототипи тепловипромінювачів',
    name_it      = 'Radiatori Proto‑Termici',
    name_pt      = 'Proto radiadores térmicos',
    name_ptbz    = 'Proto radiadores térmicos'
WHERE symbol = 'protoheatradiators';

UPDATE material_names
SET symbol = 'protolightalloys'
WHERE symbol IS NULL
  AND name = 'Proto Light Alloys';
INSERT INTO material_names (symbol, name)
SELECT 'protolightalloys', 'Proto Light Alloys'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'protolightalloys');
UPDATE material_names
SET name         = 'Proto Light Alloys',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Leichte Legierungen (Proto)',
    name_es      = 'Protoaleaciones ligeras',
    name_fr      = 'Proto-alliages légers',
    name_ru      = 'Опытные легкие сплавы',
    name_uk      = 'Дослідні легкі сплави',
    name_it      = 'Leghe Proto‑Luminose',
    name_pt      = 'Proto ligas leves',
    name_ptbz    = 'Proto ligas leves'
WHERE symbol = 'protolightalloys';

UPDATE material_names
SET symbol = 'protoradiolicalloys'
WHERE symbol IS NULL
  AND name = 'Proto Radiolic Alloys';
INSERT INTO material_names (symbol, name)
SELECT 'protoradiolicalloys', 'Proto Radiolic Alloys'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'protoradiolicalloys');
UPDATE material_names
SET name         = 'Proto Radiolic Alloys',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Radiologische Legierungen (Proto)',
    name_es      = 'Aleaciones protorradiadas',
    name_fr      = 'Proto-alliages radiologiques',
    name_ru      = 'Сплавы для изготовления зондов',
    name_uk      = 'Сплави для виготовлення зондів',
    name_it      = 'Leghe Proto‑Radioliche',
    name_pt      = 'Proto ligas radiólicas',
    name_ptbz    = 'Proto ligas radiólicas'
WHERE symbol = 'protoradiolicalloys';

UPDATE material_names
SET symbol = 'refinedfocuscrystals'
WHERE symbol IS NULL
  AND name = 'Refined Focus Crystals';
INSERT INTO material_names (symbol, name)
SELECT 'refinedfocuscrystals', 'Refined Focus Crystals'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'refinedfocuscrystals');
UPDATE material_names
SET name         = 'Refined Focus Crystals',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Raffinierte Laserkristalle',
    name_es      = 'Cristales de enfoque refinados',
    name_fr      = 'Cristaux de focalisation raffinés',
    name_ru      = 'Обработанные фокусировочные кристаллы',
    name_uk      = 'Оброблені фокусувальні кристали',
    name_it      = 'Cristalli Focali Raffinati',
    name_pt      = 'Cristais de focalização refinado',
    name_ptbz    = 'Cristais de focalização refinado'
WHERE symbol = 'refinedfocuscrystals';

UPDATE material_names
SET symbol = 'salvagedalloys'
WHERE symbol IS NULL
  AND name = 'Salvaged Alloys';
INSERT INTO material_names (symbol, name)
SELECT 'salvagedalloys', 'Salvaged Alloys'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'salvagedalloys');
UPDATE material_names
SET name         = 'Salvaged Alloys',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Geborgene Legierungen',
    name_es      = 'Aleaciones recuperadas',
    name_fr      = 'Alliages récupérés',
    name_ru      = 'Захваченные сплавы',
    name_uk      = 'Захоплені сплави',
    name_it      = 'Leghe Recuperate',
    name_pt      = 'Ligas recuperadas',
    name_ptbz    = 'Ligas recuperadas'
WHERE symbol = 'salvagedalloys';

UPDATE material_names
SET symbol = 'unknownenergysource'
WHERE symbol IS NULL
  AND name = 'Sensor Fragment';
INSERT INTO material_names (symbol, name)
SELECT 'unknownenergysource', 'Sensor Fragment'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'unknownenergysource');
UPDATE material_names
SET name         = 'Sensor Fragment',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Sensorenfragment',
    name_es      = 'Fragmento de sensor',
    name_fr      = 'Fragment de capteur',
    name_ru      = 'Обломок сенсора',
    name_uk      = 'Уламок сенсора',
    name_it      = 'Frammento Sensore',
    name_pt      = 'Fragmento de sensor',
    name_ptbz    = 'Fragmento de sensor'
WHERE symbol = 'unknownenergysource';

UPDATE material_names
SET symbol = 'shieldemitters'
WHERE symbol IS NULL
  AND name = 'Shield Emitters';
INSERT INTO material_names (symbol, name)
SELECT 'shieldemitters', 'Shield Emitters'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'shieldemitters');
UPDATE material_names
SET name         = 'Shield Emitters',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Schildemitter',
    name_es      = 'Emisor de escudos',
    name_fr      = 'Émetteurs de bouclier',
    name_ru      = 'Щитоизлучатели',
    name_uk      = 'Щитовипромінювачі',
    name_it      = 'Emettitori di Scudo',
    name_pt      = 'Emissores de escudo',
    name_ptbz    = 'Emissores de escudo'
WHERE symbol = 'shieldemitters';

UPDATE material_names
SET symbol = 'shieldingsensors'
WHERE symbol IS NULL
  AND name = 'Shielding Sensors';
INSERT INTO material_names (symbol, name)
SELECT 'shieldingsensors', 'Shielding Sensors'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'shieldingsensors');
UPDATE material_names
SET name         = 'Shielding Sensors',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Schildsensoren',
    name_es      = 'Sensores de escudo',
    name_fr      = 'Capteurs de bouclier',
    name_ru      = 'Сенсоры системы экранирования',
    name_uk      = 'Сенсори системи екранування',
    name_it      = 'Sensori di Schermatura',
    name_pt      = 'Sensores para proteção',
    name_ptbz    = 'Sensores para proteção'
WHERE symbol = 'shieldingsensors';

UPDATE material_names
SET symbol = 'unknowncorechip'
WHERE symbol IS NULL
  AND name = 'Tactical Core Chip';
INSERT INTO material_names (symbol, name)
SELECT 'unknowncorechip', 'Tactical Core Chip'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'unknowncorechip');
UPDATE material_names
SET name         = 'Tactical Core Chip',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = NULL,
    name_es      = 'Chip de núcleo táctico',
    name_fr      = 'Puce tactique principale',
    name_ru      = 'Чип тактического ядра',
    name_uk      = 'Чип тактичного ядра',
    name_it      = 'Chip Nucleo Tattico',
    name_pt      = NULL,
    name_ptbz    = NULL
WHERE symbol = 'unknowncorechip';

UPDATE material_names
SET symbol = 'temperedalloys'
WHERE symbol IS NULL
  AND name = 'Tempered Alloys';
INSERT INTO material_names (symbol, name)
SELECT 'temperedalloys', 'Tempered Alloys'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'temperedalloys');
UPDATE material_names
SET name         = 'Tempered Alloys',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Vergütete Legierungen',
    name_es      = 'Aleaciones templadas',
    name_fr      = 'Alliages trempés',
    name_ru      = 'Закаленные сплавы',
    name_uk      = 'Загартовані сплави',
    name_it      = 'Leghe Temprate',
    name_pt      = 'Ligas temperadas',
    name_ptbz    = 'Ligas temperadas'
WHERE symbol = 'temperedalloys';

UPDATE material_names
SET symbol = 'unknowncarapace'
WHERE symbol IS NULL
  AND name = 'Thargoid Carapace';
INSERT INTO material_names (symbol, name)
SELECT 'unknowncarapace', 'Thargoid Carapace'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'unknowncarapace');
UPDATE material_names
SET name         = 'Thargoid Carapace',
    materialType = 'Manufactured',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Thargoiden-Krustenschale',
    name_es      = 'Caparazón Thargoide',
    name_fr      = 'Carapace thargoid',
    name_ru      = 'Таргоидский панцирь',
    name_uk      = 'Таргоїдський панцир',
    name_it      = 'Carapace Thargoid',
    name_pt      = 'Carapaça Thargoid',
    name_ptbz    = 'Carapaça Thargoid'
WHERE symbol = 'unknowncarapace';

UPDATE material_names
SET symbol = 'unknownenergycell'
WHERE symbol IS NULL
  AND name = 'Thargoid Energy Cell';
INSERT INTO material_names (symbol, name)
SELECT 'unknownenergycell', 'Thargoid Energy Cell'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'unknownenergycell');
UPDATE material_names
SET name         = 'Thargoid Energy Cell',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Thargoiden-Energiezelle',
    name_es      = 'Célula de energía Thargoide',
    name_fr      = 'Cellule d’énergie thargoid',
    name_ru      = 'Таргоидская энергоячейка',
    name_uk      = 'Таргоїдська енергокомірка',
    name_it      = 'Cella di Energia Thargoid',
    name_pt      = 'Célula de Energia Thargoid',
    name_ptbz    = 'Célula de Energia Thargoid'
WHERE symbol = 'unknownenergycell';

UPDATE material_names
SET symbol = 'unknownorganiccircuitry'
WHERE symbol IS NULL
  AND name = 'Thargoid Organic Circuitry';
INSERT INTO material_names (symbol, name)
SELECT 'unknownorganiccircuitry', 'Thargoid Organic Circuitry'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'unknownorganiccircuitry');
UPDATE material_names
SET name         = 'Thargoid Organic Circuitry',
    materialType = 'Manufactured',
    grade        = 5,
    maxCapacity  = 100,
    name_de      = 'Organischer Schaltkreis der Thargoiden',
    name_es      = 'Circuitería orgánica Thargoide',
    name_fr      = 'Circuits organiques thargoids',
    name_ru      = 'Таргоидская органическая схема',
    name_uk      = 'Таргоїдська органічна схема',
    name_it      = 'Circuiti Organici Thargoid',
    name_pt      = 'Circuito Orgânico Thargoid',
    name_ptbz    = 'Circuito Orgânico Thargoid'
WHERE symbol = 'unknownorganiccircuitry';

UPDATE material_names
SET symbol = 'unknowntechnologycomponents'
WHERE symbol IS NULL
  AND name = 'Thargoid Technological Components';
INSERT INTO material_names (symbol, name)
SELECT 'unknowntechnologycomponents', 'Thargoid Technological Components'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'unknowntechnologycomponents');
UPDATE material_names
SET name         = 'Thargoid Technological Components',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Technologiekomponenten der Thargoiden',
    name_es      = 'Componentes tecnológicos Thargoides',
    name_fr      = 'Composants technologiques thargoids',
    name_ru      = 'Компоненты таргоидской техники',
    name_uk      = 'Компоненти таргоїдської техніки',
    name_it      = 'Componenti Tecnologici Thargoid',
    name_pt      = 'Componentes Tecnológicos Thargoid',
    name_ptbz    = 'Componentes Tecnológicos Thargoid'
WHERE symbol = 'unknowntechnologycomponents';

UPDATE material_names
SET symbol = 'thermicalloys'
WHERE symbol IS NULL
  AND name = 'Thermic Alloys';
INSERT INTO material_names (symbol, name)
SELECT 'thermicalloys', 'Thermic Alloys'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'thermicalloys');
UPDATE material_names
SET name         = 'Thermic Alloys',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Thermische Legierungen',
    name_es      = 'Aleaciones térmicas',
    name_fr      = 'Alliages thermiques',
    name_ru      = 'Термические сплавы',
    name_uk      = 'Термічні сплави',
    name_it      = 'Leghe Termiche',
    name_pt      = 'Ligas térmicas',
    name_ptbz    = 'Ligas térmicas'
WHERE symbol = 'thermicalloys';

UPDATE material_names
SET symbol = 'tg_weaponparts'
WHERE symbol IS NULL
  AND name = 'Weapon Parts (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_weaponparts', 'Weapon Parts'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_weaponparts');
UPDATE material_names
SET name         = 'Weapon Parts',
    materialType = 'Manufactured',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Waffenteile',
    name_es      = 'Piezas de armamento',
    name_fr      = 'Pièces d’armement',
    name_ru      = 'Детали вооружения',
    name_uk      = 'Деталі озброєння',
    name_it      = 'Parti Arma',
    name_pt      = 'Peças de Arma',
    name_ptbz    = 'Peças de Arma'
WHERE symbol = 'tg_weaponparts';

UPDATE material_names
SET symbol = 'wornshieldemitters'
WHERE symbol IS NULL
  AND name = 'Worn Shield Emitters';
INSERT INTO material_names (symbol, name)
SELECT 'wornshieldemitters', 'Worn Shield Emitters'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'wornshieldemitters');
UPDATE material_names
SET name         = 'Worn Shield Emitters',
    materialType = 'Manufactured',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Gebrauchte Schildemitter',
    name_es      = 'Emisor de escudos desgastado',
    name_fr      = 'Émetteurs de bouclier usés',
    name_ru      = 'Изношенные щитоизлучатели',
    name_uk      = 'Зношені щитовипромінювачі',
    name_it      = 'Emettitori di Scudo Usurati',
    name_pt      = 'Emissores de escudo usado',
    name_ptbz    = 'Emissores de escudo usado'
WHERE symbol = 'wornshieldemitters';

UPDATE material_names
SET symbol = 'tg_wreckagecomponents'
WHERE symbol IS NULL
  AND name = 'Wreckage Components (Thargoid)';
INSERT INTO material_names (symbol, name)
SELECT 'tg_wreckagecomponents', 'Wreckage Components'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tg_wreckagecomponents');
UPDATE material_names
SET name         = 'Wreckage Components',
    materialType = 'Manufactured',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Wrackteilkomponenten',
    name_es      = 'Restos de accidentes',
    name_fr      = 'Débris d’épave',
    name_ru      = 'Обломки кораблекрушений',
    name_uk      = 'Уламки корабельних аварій',
    name_it      = 'Componenti di Relitto',
    name_pt      = 'Restos de componentes',
    name_ptbz    = 'Restos de componentes'
WHERE symbol = 'tg_wreckagecomponents';


-- ============================ Raw ============================

UPDATE material_names
SET symbol = 'antimony'
WHERE symbol IS NULL
  AND name = 'Antimony';
INSERT INTO material_names (symbol, name)
SELECT 'antimony', 'Antimony'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'antimony');
UPDATE material_names
SET name         = 'Antimony',
    materialType = 'Raw',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Antimon',
    name_es      = 'Antimonio',
    name_fr      = 'Antimoine',
    name_ru      = 'Сурьма',
    name_uk      = 'Сурма',
    name_it      = 'Antimonio',
    name_pt      = 'Antimônio',
    name_ptbz    = 'Antimônio'
WHERE symbol = 'antimony';

UPDATE material_names
SET symbol = 'arsenic'
WHERE symbol IS NULL
  AND name = 'Arsenic';
INSERT INTO material_names (symbol, name)
SELECT 'arsenic', 'Arsenic'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'arsenic');
UPDATE material_names
SET name         = 'Arsenic',
    materialType = 'Raw',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Arsen',
    name_es      = 'Arsénico',
    name_fr      = 'Arsenic',
    name_ru      = 'Мышьяк',
    name_uk      = 'Миш''як',
    name_it      = 'Arsenico',
    name_pt      = 'Arsênico',
    name_ptbz    = 'Arsênico'
WHERE symbol = 'arsenic';

UPDATE material_names
SET symbol = 'boron'
WHERE symbol IS NULL
  AND name = 'Boron';
INSERT INTO material_names (symbol, name)
SELECT 'boron', 'Boron'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'boron');
UPDATE material_names
SET name         = 'Boron',
    materialType = 'Raw',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Bor',
    name_es      = 'Boro',
    name_fr      = 'Bore',
    name_ru      = 'Бор',
    name_uk      = 'Бор',
    name_it      = 'Boro',
    name_pt      = 'Boro',
    name_ptbz    = 'Boro'
WHERE symbol = 'boron';

UPDATE material_names
SET symbol = 'cadmium'
WHERE symbol IS NULL
  AND name = 'Cadmium';
INSERT INTO material_names (symbol, name)
SELECT 'cadmium', 'Cadmium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'cadmium');
UPDATE material_names
SET name         = 'Cadmium',
    materialType = 'Raw',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Kadmium',
    name_es      = 'Cadmio',
    name_fr      = 'Cadmium',
    name_ru      = 'Кадмий',
    name_uk      = 'Кадмій',
    name_it      = 'Cadmio',
    name_pt      = 'Cádmio',
    name_ptbz    = 'Cádmio'
WHERE symbol = 'cadmium';

UPDATE material_names
SET symbol = 'carbon'
WHERE symbol IS NULL
  AND name = 'Carbon';
INSERT INTO material_names (symbol, name)
SELECT 'carbon', 'Carbon'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'carbon');
UPDATE material_names
SET name         = 'Carbon',
    materialType = 'Raw',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Kohlenstoff',
    name_es      = 'Carbono',
    name_fr      = 'Carbone',
    name_ru      = 'Углерод',
    name_uk      = 'Вуглець',
    name_it      = 'Carbonio',
    name_pt      = 'Carbono',
    name_ptbz    = 'Carbono'
WHERE symbol = 'carbon';

UPDATE material_names
SET symbol = 'chromium'
WHERE symbol IS NULL
  AND name = 'Chromium';
INSERT INTO material_names (symbol, name)
SELECT 'chromium', 'Chromium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'chromium');
UPDATE material_names
SET name         = 'Chromium',
    materialType = 'Raw',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Chrom',
    name_es      = 'Cromo',
    name_fr      = 'Chrome',
    name_ru      = 'Хром',
    name_uk      = 'Хром',
    name_it      = 'Cromo',
    name_pt      = 'Cromo',
    name_ptbz    = 'Cromo'
WHERE symbol = 'chromium';

UPDATE material_names
SET symbol = 'germanium'
WHERE symbol IS NULL
  AND name = 'Germanium';
INSERT INTO material_names (symbol, name)
SELECT 'germanium', 'Germanium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'germanium');
UPDATE material_names
SET name         = 'Germanium',
    materialType = 'Raw',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = NULL,
    name_es      = 'Germanio',
    name_fr      = 'Germanium',
    name_ru      = 'Германий',
    name_uk      = 'Германій',
    name_it      = 'Germanio',
    name_pt      = 'Germânio',
    name_ptbz    = 'Germânio'
WHERE symbol = 'germanium';

UPDATE material_names
SET symbol = 'iron'
WHERE symbol IS NULL
  AND name = 'Iron';
INSERT INTO material_names (symbol, name)
SELECT 'iron', 'Iron'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'iron');
UPDATE material_names
SET name         = 'Iron',
    materialType = 'Raw',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Eisen',
    name_es      = 'Hierro',
    name_fr      = 'Fer',
    name_ru      = 'Железо',
    name_uk      = 'Залізо',
    name_it      = 'Ferro',
    name_pt      = 'Ferro',
    name_ptbz    = 'Ferro'
WHERE symbol = 'iron';

UPDATE material_names
SET symbol = 'lead'
WHERE symbol IS NULL
  AND name = 'Lead';
INSERT INTO material_names (symbol, name)
SELECT 'lead', 'Lead'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'lead');
UPDATE material_names
SET name         = 'Lead',
    materialType = 'Raw',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Blei',
    name_es      = 'Plomo',
    name_fr      = 'Plomb',
    name_ru      = 'Свинец',
    name_uk      = 'Свинець',
    name_it      = 'Piombo',
    name_pt      = 'Chumbo',
    name_ptbz    = 'Chumbo'
WHERE symbol = 'lead';

UPDATE material_names
SET symbol = 'manganese'
WHERE symbol IS NULL
  AND name = 'Manganese';
INSERT INTO material_names (symbol, name)
SELECT 'manganese', 'Manganese'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'manganese');
UPDATE material_names
SET name         = 'Manganese',
    materialType = 'Raw',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Mangan',
    name_es      = 'Manganeso',
    name_fr      = 'Manganèse',
    name_ru      = 'Марганец',
    name_uk      = 'Марганець',
    name_it      = 'Manganese',
    name_pt      = 'Manganês',
    name_ptbz    = 'Manganês'
WHERE symbol = 'manganese';

UPDATE material_names
SET symbol = 'mercury'
WHERE symbol IS NULL
  AND name = 'Mercury';
INSERT INTO material_names (symbol, name)
SELECT 'mercury', 'Mercury'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'mercury');
UPDATE material_names
SET name         = 'Mercury',
    materialType = 'Raw',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Quecksilber',
    name_es      = 'Mercurio',
    name_fr      = 'Mercure',
    name_ru      = 'Ртуть',
    name_uk      = 'Ртуть',
    name_it      = 'Mercurio',
    name_pt      = 'Mercúrio',
    name_ptbz    = 'Mercúrio'
WHERE symbol = 'mercury';

UPDATE material_names
SET symbol = 'molybdenum'
WHERE symbol IS NULL
  AND name = 'Molybdenum';
INSERT INTO material_names (symbol, name)
SELECT 'molybdenum', 'Molybdenum'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'molybdenum');
UPDATE material_names
SET name         = 'Molybdenum',
    materialType = 'Raw',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Molibdän',
    name_es      = 'Molibdeno',
    name_fr      = 'Molybdène',
    name_ru      = 'Молибден',
    name_uk      = 'Молібден',
    name_it      = 'Molibdeno',
    name_pt      = 'Molibdênio',
    name_ptbz    = 'Molibdênio'
WHERE symbol = 'molybdenum';

UPDATE material_names
SET symbol = 'nickel'
WHERE symbol IS NULL
  AND name = 'Nickel';
INSERT INTO material_names (symbol, name)
SELECT 'nickel', 'Nickel'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'nickel');
UPDATE material_names
SET name         = 'Nickel',
    materialType = 'Raw',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = NULL,
    name_es      = 'Níquel',
    name_fr      = 'Nickel',
    name_ru      = 'Никель',
    name_uk      = 'Нікель',
    name_it      = 'Nichel',
    name_pt      = 'Níquel',
    name_ptbz    = 'Níquel'
WHERE symbol = 'nickel';

UPDATE material_names
SET symbol = 'niobium'
WHERE symbol IS NULL
  AND name = 'Niobium';
INSERT INTO material_names (symbol, name)
SELECT 'niobium', 'Niobium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'niobium');
UPDATE material_names
SET name         = 'Niobium',
    materialType = 'Raw',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = NULL,
    name_es      = 'Niobio',
    name_fr      = 'Niobium',
    name_ru      = 'Ниобий',
    name_uk      = 'Ніобій',
    name_it      = 'Niobio',
    name_pt      = 'Nióbio',
    name_ptbz    = 'Nióbio'
WHERE symbol = 'niobium';

UPDATE material_names
SET symbol = 'phosphorus'
WHERE symbol IS NULL
  AND name = 'Phosphorus';
INSERT INTO material_names (symbol, name)
SELECT 'phosphorus', 'Phosphorus'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'phosphorus');
UPDATE material_names
SET name         = 'Phosphorus',
    materialType = 'Raw',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Phosphor',
    name_es      = 'Fósforo',
    name_fr      = 'Phosphore',
    name_ru      = 'Фосфор',
    name_uk      = 'Фосфор',
    name_it      = 'Fosforo',
    name_pt      = 'Fósforo',
    name_ptbz    = 'Fósforo'
WHERE symbol = 'phosphorus';

UPDATE material_names
SET symbol = 'polonium'
WHERE symbol IS NULL
  AND name = 'Polonium';
INSERT INTO material_names (symbol, name)
SELECT 'polonium', 'Polonium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'polonium');
UPDATE material_names
SET name         = 'Polonium',
    materialType = 'Raw',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = NULL,
    name_es      = 'Polonio',
    name_fr      = 'Polonium',
    name_ru      = 'Полоний',
    name_uk      = 'Полоній',
    name_it      = 'Polonio',
    name_pt      = 'Polônio',
    name_ptbz    = 'Polônio'
WHERE symbol = 'polonium';

UPDATE material_names
SET symbol = 'rhenium'
WHERE symbol IS NULL
  AND name = 'Rhenium';
INSERT INTO material_names (symbol, name)
SELECT 'rhenium', 'Rhenium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'rhenium');
UPDATE material_names
SET name         = 'Rhenium',
    materialType = 'Raw',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = NULL,
    name_es      = 'Renio',
    name_fr      = 'Rhénium',
    name_ru      = 'Рений',
    name_uk      = 'Реній',
    name_it      = 'Renio',
    name_pt      = 'Rênio',
    name_ptbz    = 'Rênio'
WHERE symbol = 'rhenium';

UPDATE material_names
SET symbol = 'ruthenium'
WHERE symbol IS NULL
  AND name = 'Ruthenium';
INSERT INTO material_names (symbol, name)
SELECT 'ruthenium', 'Ruthenium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'ruthenium');
UPDATE material_names
SET name         = 'Ruthenium',
    materialType = 'Raw',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = NULL,
    name_es      = 'Rutenio',
    name_fr      = 'Ruthénium',
    name_ru      = 'Рутений',
    name_uk      = 'Рутеній',
    name_it      = 'Rutenio',
    name_pt      = 'Rutênio',
    name_ptbz    = 'Rutênio'
WHERE symbol = 'ruthenium';

UPDATE material_names
SET symbol = 'selenium'
WHERE symbol IS NULL
  AND name = 'Selenium';
INSERT INTO material_names (symbol, name)
SELECT 'selenium', 'Selenium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'selenium');
UPDATE material_names
SET name         = 'Selenium',
    materialType = 'Raw',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Selen',
    name_es      = 'Selenio',
    name_fr      = 'Sélénium',
    name_ru      = 'Селен',
    name_uk      = 'Селен',
    name_it      = 'Selenio',
    name_pt      = 'Selênio',
    name_ptbz    = 'Selênio'
WHERE symbol = 'selenium';

UPDATE material_names
SET symbol = 'sulphur'
WHERE symbol IS NULL
  AND name = 'Sulphur';
INSERT INTO material_names (symbol, name)
SELECT 'sulphur', 'Sulphur'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'sulphur');
UPDATE material_names
SET name         = 'Sulphur',
    materialType = 'Raw',
    grade        = 1,
    maxCapacity  = 300,
    name_de      = 'Schwefel',
    name_es      = 'Azufre',
    name_fr      = 'Soufre',
    name_ru      = 'Сера',
    name_uk      = 'Сірка',
    name_it      = 'Zolfo',
    name_pt      = 'Enxofre',
    name_ptbz    = 'Enxofre'
WHERE symbol = 'sulphur';

UPDATE material_names
SET symbol = 'technetium'
WHERE symbol IS NULL
  AND name = 'Technetium';
INSERT INTO material_names (symbol, name)
SELECT 'technetium', 'Technetium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'technetium');
UPDATE material_names
SET name         = 'Technetium',
    materialType = 'Raw',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = NULL,
    name_es      = 'Tecnecio',
    name_fr      = 'Technétium',
    name_ru      = 'Технеций',
    name_uk      = 'Технецій',
    name_it      = 'Tecnezio',
    name_pt      = 'Tecnécio',
    name_ptbz    = 'Tecnécio'
WHERE symbol = 'technetium';

UPDATE material_names
SET symbol = 'tellurium'
WHERE symbol IS NULL
  AND name = 'Tellurium';
INSERT INTO material_names (symbol, name)
SELECT 'tellurium', 'Tellurium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tellurium');
UPDATE material_names
SET name         = 'Tellurium',
    materialType = 'Raw',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = 'Tellur',
    name_es      = 'Teluro',
    name_fr      = 'Tellure',
    name_ru      = 'Теллур',
    name_uk      = 'Телур',
    name_it      = 'Tellurio',
    name_pt      = 'Telúrio',
    name_ptbz    = 'Telúrio'
WHERE symbol = 'tellurium';

UPDATE material_names
SET symbol = 'tin'
WHERE symbol IS NULL
  AND name = 'Tin';
INSERT INTO material_names (symbol, name)
SELECT 'tin', 'Tin'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tin');
UPDATE material_names
SET name         = 'Tin',
    materialType = 'Raw',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Zinn',
    name_es      = 'Estaño',
    name_fr      = 'Étain',
    name_ru      = 'Олово',
    name_uk      = 'Олово',
    name_it      = 'Stagno',
    name_pt      = 'Estanho',
    name_ptbz    = 'Estanho'
WHERE symbol = 'tin';

UPDATE material_names
SET symbol = 'tungsten'
WHERE symbol IS NULL
  AND name = 'Tungsten';
INSERT INTO material_names (symbol, name)
SELECT 'tungsten', 'Tungsten'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'tungsten');
UPDATE material_names
SET name         = 'Tungsten',
    materialType = 'Raw',
    grade        = 3,
    maxCapacity  = 200,
    name_de      = 'Wolfram',
    name_es      = 'Tungsteno',
    name_fr      = 'Tungstène',
    name_ru      = 'Вольфрам',
    name_uk      = 'Вольфрам',
    name_it      = 'Tungsteno',
    name_pt      = 'Tungstênio',
    name_ptbz    = 'Tungstênio'
WHERE symbol = 'tungsten';

UPDATE material_names
SET symbol = 'vanadium'
WHERE symbol IS NULL
  AND name = 'Vanadium';
INSERT INTO material_names (symbol, name)
SELECT 'vanadium', 'Vanadium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'vanadium');
UPDATE material_names
SET name         = 'Vanadium',
    materialType = 'Raw',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = NULL,
    name_es      = 'Vanadio',
    name_fr      = 'Vanadium',
    name_ru      = 'Ванадий',
    name_uk      = 'Ванадій',
    name_it      = 'Vanadio',
    name_pt      = 'Vanádio',
    name_ptbz    = 'Vanádio'
WHERE symbol = 'vanadium';

UPDATE material_names
SET symbol = 'yttrium'
WHERE symbol IS NULL
  AND name = 'Yttrium';
INSERT INTO material_names (symbol, name)
SELECT 'yttrium', 'Yttrium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'yttrium');
UPDATE material_names
SET name         = 'Yttrium',
    materialType = 'Raw',
    grade        = 4,
    maxCapacity  = 150,
    name_de      = NULL,
    name_es      = 'Ytrio',
    name_fr      = 'Yttrium',
    name_ru      = 'Иттрий',
    name_uk      = 'Ітрій',
    name_it      = 'Ittrio',
    name_pt      = 'Ítrio',
    name_ptbz    = 'Ítrio'
WHERE symbol = 'yttrium';

UPDATE material_names
SET symbol = 'zinc'
WHERE symbol IS NULL
  AND name = 'Zinc';
INSERT INTO material_names (symbol, name)
SELECT 'zinc', 'Zinc'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'zinc');
UPDATE material_names
SET name         = 'Zinc',
    materialType = 'Raw',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = 'Zink',
    name_es      = 'Zinc',
    name_fr      = 'Zinc',
    name_ru      = 'Цинк',
    name_uk      = 'Цинк',
    name_it      = 'Zinco',
    name_pt      = 'Zinco',
    name_ptbz    = 'Zinco'
WHERE symbol = 'zinc';

UPDATE material_names
SET symbol = 'zirconium'
WHERE symbol IS NULL
  AND name = 'Zirconium';
INSERT INTO material_names (symbol, name)
SELECT 'zirconium', 'Zirconium'
WHERE NOT EXISTS (SELECT 1 FROM material_names WHERE symbol = 'zirconium');
UPDATE material_names
SET name         = 'Zirconium',
    materialType = 'Raw',
    grade        = 2,
    maxCapacity  = 250,
    name_de      = NULL,
    name_es      = 'Circonio',
    name_fr      = 'Zirconium',
    name_ru      = 'Цирконий',
    name_uk      = 'Цирконій',
    name_it      = 'Zirconio',
    name_pt      = 'Zircônio',
    name_ptbz    = 'Zircônio'
WHERE symbol = 'zirconium';

CREATE UNIQUE INDEX IF NOT EXISTS idx_material_names_symbol ON material_names (symbol);

-- ============================ aliases ============================

INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('guardian_moduleblueprint', 'en', 'Guardian Module Blueprint Segment');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('guardian_vesselblueprint', 'en', 'Guardian Vessel Blueprint Segment');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('guardian_weaponblueprint', 'en', 'Guardian Weapon Blueprint Segment');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_biomechanicalconduits', 'en', 'Thargoid Bio-Mechanical Conduits');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_biomechanicalconduits', 'en', 'Bio Mechanical Conduits');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticcrystal', 'en', 'Thargoid Caustic Crystal');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticgeneratorparts', 'en', 'Thargoid Corrosive Mechanisms');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticshard', 'en', 'Thargoid Caustic Shard');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_propulsionelement', 'en', 'Thargoid Propulsion Element');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_propulsionelement', 'en', 'Thargoid Propulsion Elements');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_weaponparts', 'en', 'Thargoid Weapon Parts');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_wreckagecomponents', 'en', 'Thargoid Wreckage Components');

INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('niobium', 'de', 'Niob');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_abrasion01', 'de', 'Hitzeexpositionsprobe');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_abrasion01', 'de', 'Wärmeexpositionsprobe');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_abrasion03', 'de', 'Gehärtete Oberflächenfragmente');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticcrystal', 'de', 'Ätzender Kristall');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticgeneratorparts', 'de', 'Korrosive Mechanismen');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticgeneratorparts', 'de', 'Ätzende Mechanismen');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticshard', 'de', 'Ätzender Splitter');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticshard', 'de', 'Ätzende Scherbe');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_interdictiondata', 'de', 'Thargoiden-Abfangtelemetrie');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_interdictiondata', 'de', 'Abfangtelemetrie');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_shutdowndata', 'de', 'Massive Energiestoßanalyse');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_shutdowndata', 'de', 'Energiestoßanalyse');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('unknowncorechip', 'de', 'Taktischer Kernchip');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('zirconium', 'de', 'Zirkonium');

INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_abrasion02', 'fr', 'Résidu de membrane de phase');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticcrystal', 'fr', 'Cristal caustique');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticgeneratorparts', 'fr', 'Mécanismes corrosifs');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticshard', 'fr', 'Éclat caustique');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_interdictiondata', 'fr', 'Télémétrie d''interception thargoïde');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_interdictiondata', 'fr', 'Télémétrie d''interception');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_shutdowndata', 'fr', 'Analyse de surtension énergétique massive');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_shutdowndata', 'fr', 'Analyse de surtension');

INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticcrystal', 'pt', 'Cristal cáustico');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticgeneratorparts', 'pt', 'Mecanismos corrosivos');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticshard', 'pt', 'Fragmento cáustico');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_interdictiondata', 'pt', 'Telemetria de interdição Thargoid');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_shutdowndata', 'pt', 'Análise de surto massivo de energia');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('unknowncorechip', 'pt', 'Chip de núcleo tático');

INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticcrystal', 'ptbz', 'Cristal cáustico');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticgeneratorparts', 'ptbz', 'Mecanismos corrosivos');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_causticshard', 'ptbz', 'Fragmento cáustico');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_interdictiondata', 'ptbz', 'Telemetria de interdição Thargoid');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('tg_shutdowndata', 'ptbz', 'Análise de surto massivo de energia');
INSERT OR IGNORE INTO material_aliases (symbol, lang, alias)
VALUES ('unknowncorechip', 'ptbz', 'Chip de núcleo tático');

-- ================== carry on-hand amounts across ==================
-- Pass 1: rows MaterialsEventSubscriber wrote, keyed by capitalizeWords(symbol)
-- ('Basicconductors', 'Guardian_powercell'). These came from the Materials event,
-- which is a full inventory snapshot, so they are the authoritative counts.
UPDATE material_names
SET amount = COALESCE((SELECT m.amount
                       FROM materials m
                       WHERE LOWER(REPLACE(REPLACE(m.materialName, ' ', ''), '_', ''))
                                 = LOWER(REPLACE(material_names.symbol, '_', ''))
                       LIMIT 1), amount);

-- Pass 2: older rows keyed by display name ('Pattern Alpha Obelisk Data'), used only
-- where pass 1 found nothing.
UPDATE material_names
SET amount = COALESCE((SELECT m.amount
                       FROM materials m
                       WHERE LOWER(m.materialName) = LOWER(material_names.name)
                       LIMIT 1), amount)
WHERE amount = 0;

-- Clamp anything the old bad maxCapacity data let run over the real cap.
UPDATE material_names
SET amount = maxCapacity
WHERE maxCapacity IS NOT NULL
  AND amount > maxCapacity;
