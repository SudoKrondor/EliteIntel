-- Add language columns and materialType to material_names
ALTER TABLE material_names
    ADD COLUMN materialType TEXT;
ALTER TABLE material_names
    ADD COLUMN name_de TEXT;
ALTER TABLE material_names
    ADD COLUMN name_fr TEXT;
ALTER TABLE material_names
    ADD COLUMN name_es TEXT;
ALTER TABLE material_names
    ADD COLUMN name_ru TEXT;
ALTER TABLE material_names
    ADD COLUMN name_uk TEXT;

-- raw.csv
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Antimony', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Antimon',
    name_es      = 'Antimonio',
    name_fr      = 'Antimoine',
    name_ru      = 'Сурьма'
WHERE LOWER(name) = LOWER('Antimony');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Arsenic', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Arsen',
    name_es      = 'Arsénico',
    name_fr      = 'Arsenic',
    name_ru      = 'Мышьяк'
WHERE LOWER(name) = LOWER('Arsenic');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Boron', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Bor',
    name_es      = 'Boro',
    name_fr      = 'Bore',
    name_ru      = 'Бор'
WHERE LOWER(name) = LOWER('Boron');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Cadmium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Kadmium',
    name_es      = 'Cadmio',
    name_fr      = 'Cadmium',
    name_ru      = 'Кадмий'
WHERE LOWER(name) = LOWER('Cadmium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Carbon', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Kohlenstoff',
    name_es      = 'Carbono',
    name_fr      = 'Carbone',
    name_ru      = 'Углерод'
WHERE LOWER(name) = LOWER('Carbon');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Chromium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Chrom',
    name_es      = 'Cromo',
    name_fr      = 'Chrome',
    name_ru      = 'Хром'
WHERE LOWER(name) = LOWER('Chromium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Germanium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_es      = 'Germanio',
    name_fr      = 'Germanium',
    name_ru      = 'Германий'
WHERE LOWER(name) = LOWER('Germanium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Iron', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Eisen',
    name_es      = 'Hierro',
    name_fr      = 'Fer',
    name_ru      = 'Железо'
WHERE LOWER(name) = LOWER('Iron');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Lead', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Blei',
    name_es      = 'Plomo',
    name_fr      = 'Plomb',
    name_ru      = 'Свинец'
WHERE LOWER(name) = LOWER('Lead');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Manganese', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Mangan',
    name_es      = 'Manganeso',
    name_fr      = 'Manganèse',
    name_ru      = 'Марганец'
WHERE LOWER(name) = LOWER('Manganese');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Mercury', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Quecksilber',
    name_es      = 'Mercurio',
    name_fr      = 'Mercure',
    name_ru      = 'Ртуть'
WHERE LOWER(name) = LOWER('Mercury');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Molybdenum', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Molibdän',
    name_es      = 'Molibdeno',
    name_fr      = 'Molybdène',
    name_ru      = 'Молибден'
WHERE LOWER(name) = LOWER('Molybdenum');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Nickel', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_es      = 'Níquel',
    name_fr      = 'Nickel',
    name_ru      = 'Никель'
WHERE LOWER(name) = LOWER('Nickel');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Niobium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_es      = 'Niobio',
    name_fr      = 'Niobium',
    name_ru      = 'Ниобий'
WHERE LOWER(name) = LOWER('Niobium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Phosphorus', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Phosphor',
    name_es      = 'Fósforo',
    name_fr      = 'Phosphore',
    name_ru      = 'Фосфор'
WHERE LOWER(name) = LOWER('Phosphorus');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Polonium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_es      = 'Polonio',
    name_fr      = 'Polonium',
    name_ru      = 'Полоний'
WHERE LOWER(name) = LOWER('Polonium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Rhenium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_es      = 'Renio',
    name_fr      = 'Rhénium',
    name_ru      = 'Рений'
WHERE LOWER(name) = LOWER('Rhenium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Ruthenium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_es      = 'Rutenio',
    name_fr      = 'Ruthénium',
    name_ru      = 'Рутений'
WHERE LOWER(name) = LOWER('Ruthenium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Selenium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Selen',
    name_es      = 'Selenio',
    name_fr      = 'Sélénium',
    name_ru      = 'Селен'
WHERE LOWER(name) = LOWER('Selenium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Sulphur', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Schwefel',
    name_es      = 'Azufre',
    name_fr      = 'Soufre',
    name_ru      = 'Сера'
WHERE LOWER(name) = LOWER('Sulphur');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Technetium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_es      = 'Tecnecio',
    name_fr      = 'Technétium',
    name_ru      = 'Технеций'
WHERE LOWER(name) = LOWER('Technetium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Tellurium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Tellur',
    name_es      = 'Teluro',
    name_fr      = 'Tellure',
    name_ru      = 'Теллур'
WHERE LOWER(name) = LOWER('Tellurium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Tin', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Zinn',
    name_es      = 'Estaño',
    name_fr      = 'Étain',
    name_ru      = 'Олово'
WHERE LOWER(name) = LOWER('Tin');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Tungsten', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Wolfram',
    name_es      = 'Tungsteno',
    name_fr      = 'Tungstène',
    name_ru      = 'Вольфрам'
WHERE LOWER(name) = LOWER('Tungsten');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Unknown', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Unbekannt',
    name_es      = 'Desconocido',
    name_fr      = 'Inconnu',
    name_ru      = 'Неизвестно'
WHERE LOWER(name) = LOWER('Unknown');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Vanadium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_es      = 'Vanadio',
    name_fr      = 'Vanadium',
    name_ru      = 'Ванадий'
WHERE LOWER(name) = LOWER('Vanadium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Yttrium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_es      = 'Ytrio',
    name_fr      = 'Yttrium',
    name_ru      = 'Иттрий'
WHERE LOWER(name) = LOWER('Yttrium');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Zinc', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_de      = 'Zink',
    name_es      = 'Zinc',
    name_fr      = 'Zinc',
    name_ru      = 'Цинк'
WHERE LOWER(name) = LOWER('Zinc');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Zirconium', 'Raw');
UPDATE material_names
SET materialType = 'Raw',
    name_es      = 'Circonio',
    name_fr      = 'Zirconium',
    name_ru      = 'Цирконий'
WHERE LOWER(name) = LOWER('Zirconium');

-- encoded.csv
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Adaptive Encryptors Capture', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Adaptive Verschlüsselungserfassung',
    name_es      = 'Captura de encriptadores adaptativos',
    name_fr      = 'Capture de cryptage évolutif',
    name_ru      = 'Захват адаптивного шифровальщика'
WHERE LOWER(name) = LOWER('Adaptive Encryptors Capture');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Irregular Emission Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Irreguläre Emissionsdaten',
    name_es      = 'Datos de emisión irregulares',
    name_fr      = 'Données d’émissions aberrantes',
    name_ru      = 'Нестандартные данные об излучении'
WHERE LOWER(name) = LOWER('Irregular Emission Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Anomalous Bulk Scan Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Anormale Massen-Scan-Daten',
    name_es      = 'Datos de escáner en bruto anómalos',
    name_fr      = 'Fichier volumineux de données d’analyse anormal',
    name_ru      = 'Аномальный массив данных сканирования'
WHERE LOWER(name) = LOWER('Anomalous Bulk Scan Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Classified Scan Fragment', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Geheimes Scan-Fragment',
    name_es      = 'Fragmento de escáner clasificado',
    name_fr      = 'Données d’analyse classifiées parcellaires',
    name_ru      = 'Засекреченные фрагменты данных сканирования'
WHERE LOWER(name) = LOWER('Classified Scan Fragment');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Abnormal Compact Emissions Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Anormale kompakte Emissionsdaten',
    name_es      = 'Compresión de datos de transmisiones anormal',
    name_fr      = 'Données d’émissions compactes anormales',
    name_ru      = 'Аномальные компактные данные об излучении'
WHERE LOWER(name) = LOWER('Abnormal Compact Emissions Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Modified Consumer Firmware', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Modifizierte Consumer-Firmware',
    name_es      = 'Firmware de consumo modificado',
    name_fr      = 'Micrologiciel consommateur modifié',
    name_ru      = 'Измененные пользовательские микропрограммы'
WHERE LOWER(name) = LOWER('Modified Consumer Firmware');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Datamined Wake Exceptions', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'FSA-Daten-Cache-Ausnahmen',
    name_es      = 'Excepciones en análisis de estelas',
    name_fr      = 'Explorations de données de sillages anormales',
    name_ru      = 'Исключения из глубинного анализа данных следа'
WHERE LOWER(name) = LOWER('Datamined Wake Exceptions');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Decoded Emission Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Entschlüsselte Emissionsdaten',
    name_es      = 'Datos de emisión descodificados',
    name_fr      = 'Données d’émissions décodées',
    name_ru      = 'Расшифрованные данные об излучении'
WHERE LOWER(name) = LOWER('Decoded Emission Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Atypical Disrupted Wake Echoes', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Atypische FSA-Stör-Aufzeichnungen',
    name_es      = 'Ecos de estelas interrumpidas atípicos',
    name_fr      = 'Échos de sillages perturbés atypiques',
    name_ru      = 'Атипичное эхо поврежденного следа'
WHERE LOWER(name) = LOWER('Atypical Disrupted Wake Echoes');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Modified Embedded Firmware', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Modifizierte integrierte Firmware',
    name_es      = 'Firmware integrado modificado',
    name_fr      = 'Micrologiciel intégré modifié',
    name_ru      = 'Измененные встроенные микропрограммы'
WHERE LOWER(name) = LOWER('Modified Embedded Firmware');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Unexpected Emission Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Unerwartete Emissionsdaten',
    name_es      = 'Datos de emisión inesperados',
    name_fr      = 'Données d’émissions inattendues',
    name_ru      = 'Неожиданные данные об излучении'
WHERE LOWER(name) = LOWER('Unexpected Emission Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Divergent Scan Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Divergente Scandaten',
    name_es      = 'Datos de escáner divergentes',
    name_fr      = 'Données d’analyse divergentes',
    name_ru      = 'Неформатные данные сканирования'
WHERE LOWER(name) = LOWER('Divergent Scan Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Unusual Encrypted Files', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Ungewöhnliche verschlüsselte Files',
    name_es      = 'Ficheros encriptados inusuales',
    name_fr      = 'Fichiers cryptés inhabituels',
    name_ru      = 'Особые зашифрованные файлы'
WHERE LOWER(name) = LOWER('Unusual Encrypted Files');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Atypical Encryption Archives', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Atypische Verschlüsselungsarchive',
    name_es      = 'Archivos encriptados atípicos',
    name_fr      = 'Archives cryptées atypiques',
    name_ru      = 'Нетипичные архивы шифрования'
WHERE LOWER(name) = LOWER('Atypical Encryption Archives');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Tagged Encryption Codes', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Getaggte Verschlüsselungscodes',
    name_es      = 'Códigos de encriptación marcados',
    name_fr      = 'Clés de cryptage balisées',
    name_ru      = 'Меченые шифровальные коды'
WHERE LOWER(name) = LOWER('Tagged Encryption Codes');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Anomalous FSD Telemetry', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Anormale FSA-Telemetrie',
    name_es      = 'Telemetría de MDD anómala',
    name_fr      = 'Télémétrie FSD anormale',
    name_ru      = 'Аномальная телеметрия FSD'
WHERE LOWER(name) = LOWER('Anomalous FSD Telemetry');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Guardian Module Blueprint Fragment', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Guardian-Modulbauplansegment',
    name_es      = 'Segmento de plano de módulo de guardián',
    name_fr      = 'Fragment de plan de module - Guardians',
    name_ru      = 'Фрагмент чертежа модуля Стражей'
WHERE LOWER(name) = LOWER('Guardian Module Blueprint Fragment');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Guardian Vessel Blueprint Fragment', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Guardian-Schiffsbauplansegment',
    name_es      = 'Segmento de plano de nave de guardián',
    name_fr      = 'Fragment de plan de vaisseau - Guardians',
    name_ru      = 'Фрагмент чертежа судна Стражей'
WHERE LOWER(name) = LOWER('Guardian Vessel Blueprint Fragment');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Guardian Weapon Blueprint Fragment', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Guardian-Waffenbauplansegment',
    name_es      = 'Segmento de plano de armamento de guardián',
    name_fr      = 'Fragment de plan d’arme - Guardians',
    name_ru      = 'Фрагмент чертежа оружия Стражей'
WHERE LOWER(name) = LOWER('Guardian Weapon Blueprint Fragment');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Eccentric Hyperspace Trajectories', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Exzentrische Hyperraum-Routen',
    name_es      = 'Trayectorias de hiperespacio excéntricas',
    name_fr      = 'Trajectoires d’hyperespace excentriques',
    name_ru      = 'Аномальные траектории в гиперпространстве'
WHERE LOWER(name) = LOWER('Eccentric Hyperspace Trajectories');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Cracked Industrial Firmware', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Gecrackte Industrie-Firmware',
    name_es      = 'Firmware industrial pirateado',
    name_fr      = 'Micrologiciel industriel piraté',
    name_ru      = 'Взломанные промышленные микропрограммы'
WHERE LOWER(name) = LOWER('Cracked Industrial Firmware');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Specialised Legacy Firmware', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Spezial-Legacy-Firmware',
    name_es      = 'Firmware heredado especializado',
    name_fr      = 'Micrologiciel spécialisé périmé',
    name_ru      = 'Специальные микропрограммы предыдущего поколения'
WHERE LOWER(name) = LOWER('Specialised Legacy Firmware');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Unidentified Scan Archives', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Unidentifizierte Scan-Archive',
    name_es      = 'Archivos de escáner no identificados',
    name_fr      = 'Données d’analyse archivées non identifiées',
    name_ru      = 'Неопознанные архивы сканирования'
WHERE LOWER(name) = LOWER('Unidentified Scan Archives');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Classified Scan Databanks', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Scan-Datenbanken unter Verschluss',
    name_es      = 'Datos de escáner clasificados',
    name_fr      = 'Banques de données d’analyse classifiées',
    name_ru      = 'Засекреченные базы данных сканирования'
WHERE LOWER(name) = LOWER('Classified Scan Databanks');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Exceptional Scrambled Emission Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Außergewöhnliche verschlüsselte Emissionsdaten',
    name_es      = 'Datos de transmisiones codificadas excepcionales',
    name_fr      = 'Données d’émissions brouillées exceptionnelles',
    name_ru      = 'Исключительные зашифрованные данные об излучении'
WHERE LOWER(name) = LOWER('Exceptional Scrambled Emission Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Security Firmware Patch', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Sicherheits-Firmware-Patch',
    name_es      = 'Parche de firmware de seguridad',
    name_fr      = 'Mise à jour de micrologiciel de sécurité',
    name_ru      = 'Обновление для защитной микропрограммы'
WHERE LOWER(name) = LOWER('Security Firmware Patch');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Distorted Shield Cycle Recordings', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Gestörte Schildzyklus-Aufzeichnungen',
    name_es      = 'Registros de ciclo de escudo distorsionados',
    name_fr      = 'Enregistrements de cycles de bouclier déformés',
    name_ru      = 'Поврежденные цикличные записи щита'
WHERE LOWER(name) = LOWER('Distorted Shield Cycle Recordings');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Untypical Shield Scans', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Untypische Schildscans',
    name_es      = 'Escáner de escudos atípico',
    name_fr      = 'Analyses de bouclier atypiques',
    name_ru      = 'Нетипичные данные сканирования щитов'
WHERE LOWER(name) = LOWER('Untypical Shield Scans');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Peculiar Shield Frequency Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Verdächtige Schildfrequenz-Daten',
    name_es      = 'Datos de frecuencias de escudo peculiares',
    name_fr      = 'Données de fréquences de bouclier singulières',
    name_ru      = 'Специфические данные о частоте щитов'
WHERE LOWER(name) = LOWER('Peculiar Shield Frequency Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Aberrant Shield Pattern Analysis', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Abweichende Schildeinsatz-Analysen',
    name_es      = 'Análisis de patrones de escudo aberrantes',
    name_fr      = 'Analyse de modèle de bouclier aberrante',
    name_ru      = 'Анализ аномального поведения щита'
WHERE LOWER(name) = LOWER('Aberrant Shield Pattern Analysis');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Inconsistent Shield Soak Analysis', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Inkonsistente Schildleistungsanalysen',
    name_es      = 'Análisis de absorción de escudos inconsistente',
    name_fr      = 'Analyse d’absorption de bouclier incohérente',
    name_ru      = 'Неполный анализ поглощения щита'
WHERE LOWER(name) = LOWER('Inconsistent Shield Soak Analysis');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Open Symmetric Keys', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Offene symmetrische Schlüssel',
    name_es      = 'Claves simétricas abiertas',
    name_fr      = 'Clés symétriques ouvertes',
    name_ru      = 'Открытые симметричные ключи'
WHERE LOWER(name) = LOWER('Open Symmetric Keys');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thargoid Material Composition Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Materialzusammensetzungsdaten der Thargoiden',
    name_es      = 'Datos de composición material Thargoide',
    name_fr      = 'Données de composition de matériau thargoid',
    name_ru      = 'Данные о составе таргоидских материалов'
WHERE LOWER(name) = LOWER('Thargoid Material Composition Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thargoid Residue Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Thargoiden-Rückstandsdaten',
    name_es      = 'Datos residuales Thargoides',
    name_fr      = 'Données de résidu thargoid',
    name_ru      = 'Данные об осадке таргоидского происхождения'
WHERE LOWER(name) = LOWER('Thargoid Residue Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thargoid Structural Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Thargoiden-Strukturdaten',
    name_es      = 'Datos estructurales Thargoides',
    name_fr      = 'Données de structure thargoid',
    name_ru      = 'Данные о структуре таргоидского объекта'
WHERE LOWER(name) = LOWER('Thargoid Structural Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Unknown', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Unbekannt',
    name_es      = 'Desconocido',
    name_fr      = 'Inconnu',
    name_ru      = 'Неизвестно'
WHERE LOWER(name) = LOWER('Unknown');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thargoid Ship Signature', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Thargoiden-Schiffssignatur',
    name_es      = 'Firma térmica de nave Thargoide',
    name_fr      = 'Signature de vaisseau thargoid',
    name_ru      = 'Сигнатура таргоидского корабля'
WHERE LOWER(name) = LOWER('Thargoid Ship Signature');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thargoid Wake Data', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Thargoiden-Sogwolkendaten',
    name_es      = 'Datos de estela Thargoide',
    name_fr      = 'Données de sillage thargoid',
    name_ru      = 'Данные следа таргоидского корабля'
WHERE LOWER(name) = LOWER('Thargoid Wake Data');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Strange Wake Solutions', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Seltsame FSA-Zielorte',
    name_es      = 'Extrañas soluciones de estelas',
    name_fr      = 'Solutions de sillage anormales',
    name_ru      = 'Странные расчеты следа'
WHERE LOWER(name) = LOWER('Strange Wake Solutions');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Ship Flight Data (Thargoid)', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Schiffsflugdaten',
    name_es      = 'Datos de vuelos de nave',
    name_fr      = 'Données de vol de vaisseau',
    name_ru      = 'Полетные данные корабля'
WHERE LOWER(name) = LOWER('Ship Flight Data (Thargoid)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Ship Systems Data (Thargoid)', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Schiffssysteme-Daten',
    name_es      = 'Datos de sistemas de nave',
    name_fr      = 'Données de systèmes de vaisseau',
    name_ru      = 'Данные бортовых систем корабля'
WHERE LOWER(name) = LOWER('Ship Systems Data (Thargoid)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Pattern Alpha Obelisk Data (Guardian)', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Alpha-Muster-Obeliskendaten',
    name_es      = 'Datos de obelisco de patrón alfa',
    name_fr      = 'Données d’obélisque de type alpha',
    name_ru      = 'Данные с обелиска «Альфа»'
WHERE LOWER(name) = LOWER('Pattern Alpha Obelisk Data (Guardian)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Pattern Beta Obelisk Data (Guardian)', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Beta-Muster-Obeliskendaten',
    name_es      = 'Datos de obelisco de patrón beta',
    name_fr      = 'Données d’obélisque de type bêta',
    name_ru      = 'Данные с обелиска «Бета»'
WHERE LOWER(name) = LOWER('Pattern Beta Obelisk Data (Guardian)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Pattern Gamma Obelisk Data (Guardian)', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Gamma-Muster-Obeliskendaten',
    name_es      = 'Datos de obelisco de patrón gamma',
    name_fr      = 'Données d’obélisque de type gamma',
    name_ru      = 'Данные с обелиска «Гамма»'
WHERE LOWER(name) = LOWER('Pattern Gamma Obelisk Data (Guardian)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Pattern Delta Obelisk Data (Guardian)', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Delta-Muster-Obeliskendaten',
    name_es      = 'Datos de obelisco de patrón delta',
    name_fr      = 'Données d’obélisque de type delta',
    name_ru      = 'Данные с обелиска «Дельта»'
WHERE LOWER(name) = LOWER('Pattern Delta Obelisk Data (Guardian)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Pattern Epsilon Obelisk Data (Guardian)', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_de      = 'Epsilon-Muster-Obeliskendaten',
    name_es      = 'Datos de obelisco de patrón epsilon',
    name_fr      = 'Données d’obélisque de type epsilon',
    name_ru      = 'Данные с обелиска «Эпсилон»'
WHERE LOWER(name) = LOWER('Pattern Epsilon Obelisk Data (Guardian)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thargoid Interdiction Telemetry (Thargoid)', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_es      = 'Telemetría de Interdicción Thargoide',
    name_ru      = 'Телеметрия перехвата таргоидами'
WHERE LOWER(name) = LOWER('Thargoid Interdiction Telemetry (Thargoid)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Massive Energy Surge Analytics (Thargoid)', 'Encoded');
UPDATE material_names
SET materialType = 'Encoded',
    name_es      = 'Análisis de Sobrecarga de Energía Masiva(Thargoide)',
    name_ru      = 'Параметры сильного энергетического импульса'
WHERE LOWER(name) = LOWER('Massive Energy Surge Analytics (Thargoid)');

-- manufactured.csv
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Basic Conductors', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Einfache Leiter',
    name_es      = 'Conductores básicos',
    name_fr      = 'Conducteurs simples',
    name_ru      = 'Простые проводники'
WHERE LOWER(name) = LOWER('Basic Conductors');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Biotech Conductors', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Biotech-Leiter',
    name_es      = 'Conductores biotecnológicos',
    name_fr      = 'Conducteurs biotechniques',
    name_ru      = 'Биотехнические проводники'
WHERE LOWER(name) = LOWER('Biotech Conductors');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Chemical Distillery', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Chemiedestillerie',
    name_es      = 'Destilería química',
    name_fr      = 'Distillerie chimique',
    name_ru      = 'Оборудование для перегонки химикатов'
WHERE LOWER(name) = LOWER('Chemical Distillery');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Chemical Manipulators', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Chemische Manipulatoren',
    name_es      = 'Manipuladores químicos',
    name_fr      = 'Manipulateurs chimiques',
    name_ru      = 'Манипуляторы для работы с химикатами'
WHERE LOWER(name) = LOWER('Chemical Manipulators');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Chemical Processors', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Chemische Prozessoren',
    name_es      = 'Procesadores químicos',
    name_fr      = 'Processeurs chimiques',
    name_ru      = 'Оборудование для химобработки'
WHERE LOWER(name) = LOWER('Chemical Processors');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Chemical Storage Units', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Lagerungseinheiten für Chemiestoffe',
    name_es      = 'Unidades de almacenamiento químico',
    name_fr      = 'Unités de stockage chimique',
    name_ru      = 'Контейнеры для химикатов'
WHERE LOWER(name) = LOWER('Chemical Storage Units');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Compact Composites', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Kompaktkomposite',
    name_es      = 'Compuestos compactos',
    name_fr      = 'Composites compacts',
    name_ru      = 'Спрессованные композиты'
WHERE LOWER(name) = LOWER('Compact Composites');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Compound Shielding', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Verbundschilde',
    name_es      = 'Escudos compuestos',
    name_fr      = 'Protection composite',
    name_ru      = 'Многоступенчатая защита'
WHERE LOWER(name) = LOWER('Compound Shielding');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Conductive Ceramics', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Elektrokeramiken',
    name_es      = 'Cerámicas conductivas',
    name_fr      = 'Conducteurs en céramique',
    name_ru      = 'Проводящая керамика'
WHERE LOWER(name) = LOWER('Conductive Ceramics');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Conductive Components', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Leitfähige Komponenten',
    name_es      = 'Componentes conductivos',
    name_fr      = 'Composants conducteurs',
    name_ru      = 'Проводящие компоненты'
WHERE LOWER(name) = LOWER('Conductive Components');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Conductive Polymers', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Leitfähige Polymere',
    name_es      = 'Polímeros conductivos',
    name_fr      = 'Conducteurs en polymères',
    name_ru      = 'Проводящие полимеры'
WHERE LOWER(name) = LOWER('Conductive Polymers');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Configurable Components', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Konfigurierbare Komponenten',
    name_es      = 'Componentes configurables',
    name_fr      = 'Composants paramétrables',
    name_ru      = 'Настраиваемые компоненты'
WHERE LOWER(name) = LOWER('Configurable Components');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Crystal Shards', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Kristallscherben',
    name_es      = 'Piedras de cristal',
    name_fr      = 'Éclats de cristal',
    name_ru      = 'Осколки кристаллов'
WHERE LOWER(name) = LOWER('Crystal Shards');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Electrochemical Arrays', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Elektrochemische Detektoren',
    name_es      = 'Matriz electroquímica',
    name_fr      = 'Réseaux électrochimiques',
    name_ru      = 'Электрохимические массивы'
WHERE LOWER(name) = LOWER('Electrochemical Arrays');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Exquisite Focus Crystals', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Erlesene Laserkristalle',
    name_es      = 'Cristales de enfoque exquisitos',
    name_fr      = 'Cristaux de focalisation sans défaut',
    name_ru      = 'Отборные фокусировочные кристаллы'
WHERE LOWER(name) = LOWER('Exquisite Focus Crystals');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Core Dynamics Composites', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Core Dynamics Kompositwerkstoffe',
    name_es      = 'Compuestos de Core Dynamics',
    name_fr      = 'Composites Core Dynamics',
    name_ru      = 'Композиты Core Dynamics'
WHERE LOWER(name) = LOWER('Core Dynamics Composites');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Proprietary Composites', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Kompositwerkstoffe',
    name_es      = 'Compuestos con patente',
    name_fr      = 'Composites brevetés',
    name_ru      = 'Патентованные композиты'
WHERE LOWER(name) = LOWER('Proprietary Composites');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Filament Composites', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Filament-Komposite',
    name_es      = 'Compuestos de filamentos',
    name_fr      = 'Composites filamentaires',
    name_ru      = 'Волокнистые композиты'
WHERE LOWER(name) = LOWER('Filament Composites');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Focus Crystals', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Laserkristalle',
    name_es      = 'Cristales de enfoque',
    name_fr      = 'Cristaux de focalisation',
    name_ru      = 'Фокусировочные кристаллы'
WHERE LOWER(name) = LOWER('Focus Crystals');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Galvanising Alloys', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Galvanisierende Legierungen',
    name_es      = 'Aleaciones galvanizadas',
    name_fr      = 'Alliages galvaniques',
    name_ru      = 'Сплавы для гальванизации'
WHERE LOWER(name) = LOWER('Galvanising Alloys');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Grid Resistors', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Gitterwiderstände',
    name_es      = 'Red resistiva',
    name_fr      = 'Résistance à grille',
    name_ru      = 'Наборные резисторы'
WHERE LOWER(name) = LOWER('Grid Resistors');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Guardian Power Cell', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Guardian-Energiezelle',
    name_es      = 'Célula de energía de guardián',
    name_fr      = 'Cellule d’énergie - Guardians',
    name_ru      = 'Энергоячейка Стражей'
WHERE LOWER(name) = LOWER('Guardian Power Cell');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Guardian Power Conduit', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Guardian-Energieleiter',
    name_es      = 'Conducto de energía de guardián',
    name_fr      = 'Conduit d’énergie - Guardians',
    name_ru      = 'Энергопроводники Стражей'
WHERE LOWER(name) = LOWER('Guardian Power Conduit');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Guardian Sentinel Weapon Parts', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Guardian-Wache-Waffenteile',
    name_es      = 'Piezas de armamento de centinela guardián',
    name_fr      = 'Pièce d’armement de sentinelle - Guardians',
    name_ru      = 'Детали вооружения часовых Стражей'
WHERE LOWER(name) = LOWER('Guardian Sentinel Weapon Parts');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Guardian Wreckage Components', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Guardian-Wache-Wrackteilkomponenten',
    name_es      = 'Restos de accidentes de centinela guardián',
    name_fr      = 'Débris de sentinelle - Guardians',
    name_ru      = 'Обломки кораблекрушения Стражей'
WHERE LOWER(name) = LOWER('Guardian Wreckage Components');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Guardian Technology Component', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Guardian-Technologiekomponenten',
    name_es      = 'Componente tecnológico de guardián',
    name_fr      = 'Composant technologique - Guardians',
    name_ru      = 'Компоненты технологий Стражей'
WHERE LOWER(name) = LOWER('Guardian Technology Component');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Heat Conduction Wiring', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Wärmeleitungsverdrahtung',
    name_es      = 'Cableado de conducción calorífica',
    name_fr      = 'Câblage de conduction thermique',
    name_ru      = 'Теплопроводящие провода'
WHERE LOWER(name) = LOWER('Heat Conduction Wiring');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Heat Dispersion Plate', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Wärmeverteilungsplatte',
    name_es      = 'Placa de dispersión de calor',
    name_fr      = 'Plaque de dissipation thermique',
    name_ru      = 'Теплорассеивающая пластина'
WHERE LOWER(name) = LOWER('Heat Dispersion Plate');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Heat Exchangers', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Wärmeaustauscher',
    name_es      = 'Intercambiadores de calor',
    name_fr      = 'Échangeurs de chaleur',
    name_ru      = 'Теплообменные агрегаты'
WHERE LOWER(name) = LOWER('Heat Exchangers');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Heat Resistant Ceramics', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Hitzefeste Keramik',
    name_es      = 'Cerámicas resistentes al calor',
    name_fr      = 'Céramiques résistantes à la chaleur',
    name_ru      = 'Жаропрочная керамика'
WHERE LOWER(name) = LOWER('Heat Resistant Ceramics');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Heat Vanes', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Wärmeleitbleche',
    name_es      = 'Palas térmicas',
    name_fr      = 'Vannes thermiques',
    name_ru      = 'Тепловые заслонки'
WHERE LOWER(name) = LOWER('Heat Vanes');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('High Density Composites', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Komposite hoher Dichte',
    name_es      = 'Compuestos de alta densidad',
    name_fr      = 'Composites à haute densité',
    name_ru      = 'Высокоплотностные композиты'
WHERE LOWER(name) = LOWER('High Density Composites');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Hybrid Capacitors', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Hybridkondensatoren',
    name_es      = 'Capacitadores híbridos',
    name_fr      = 'Condensateurs hybrides',
    name_ru      = 'Гибридные конденсаторы'
WHERE LOWER(name) = LOWER('Hybrid Capacitors');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Imperial Shielding', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Imperiale Schilde',
    name_es      = 'Escudos imperiales',
    name_fr      = 'Protection impériale',
    name_ru      = 'Имперская защита'
WHERE LOWER(name) = LOWER('Imperial Shielding');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Improvised Components', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Behelfskomponenten',
    name_es      = 'Componentes improvisados',
    name_fr      = 'Composants improvisés',
    name_ru      = 'Кустарные компоненты'
WHERE LOWER(name) = LOWER('Improvised Components');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Mechanical Components', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Mechanische Komponenten',
    name_es      = 'Componentes mecánicos',
    name_fr      = 'Composants mécaniques',
    name_ru      = 'Механические компоненты'
WHERE LOWER(name) = LOWER('Mechanical Components');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Mechanical Equipment', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Mechanisches Equipment',
    name_es      = 'Equipamiento mecánico',
    name_fr      = 'Équipement mécanique',
    name_ru      = 'Механическое оборудование'
WHERE LOWER(name) = LOWER('Mechanical Equipment');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Mechanical Scrap', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Mechanischer Schrott',
    name_es      = 'Chatarra mecánica',
    name_fr      = 'Ferraille mécanique',
    name_ru      = 'Механические отходы'
WHERE LOWER(name) = LOWER('Mechanical Scrap');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Military Grade Alloys', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Militärqualitätslegierungen',
    name_es      = 'Aleaciones de grado militar',
    name_fr      = 'Alliages militaires',
    name_ru      = 'Сплавы военного класса'
WHERE LOWER(name) = LOWER('Military Grade Alloys');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Military Supercapacitors', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Militärische Superkondensatoren',
    name_es      = 'Supercapacitadores militares',
    name_fr      = 'Supercondensateurs militaires',
    name_ru      = 'Военные суперконденсаторы'
WHERE LOWER(name) = LOWER('Military Supercapacitors');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Pharmaceutical Isolators', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Pharmazeutische Isolatoren',
    name_es      = 'Aislantes farmacéuticos',
    name_fr      = 'Isolants pharmaceutiques',
    name_ru      = 'Фармацевтические изоляционные материалы'
WHERE LOWER(name) = LOWER('Pharmaceutical Isolators');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Phase Alloys', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Phasenlegierungen',
    name_es      = 'Aleaciones de fase',
    name_fr      = 'Alliages de phase',
    name_ru      = 'Фазовые сплавы'
WHERE LOWER(name) = LOWER('Phase Alloys');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Polymer Capacitors', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Polymerkondensatoren',
    name_es      = 'Capacitadores de polímeros',
    name_fr      = 'Condensateurs en polymères',
    name_ru      = 'Полимерные конденсаторы'
WHERE LOWER(name) = LOWER('Polymer Capacitors');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Precipitated Alloys', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Gehärtete Legierungen',
    name_es      = 'Aleaciones de precipitación',
    name_fr      = 'Alliages précipités',
    name_ru      = 'Осажденные сплавы'
WHERE LOWER(name) = LOWER('Precipitated Alloys');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Proto Heat Radiators', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Proto-Wärmestrahler',
    name_es      = 'Protorradiadores térmicos',
    name_fr      = 'Proto-radiateurs',
    name_ru      = 'Прототипы теплоизлучателей'
WHERE LOWER(name) = LOWER('Proto Heat Radiators');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Proto Light Alloys', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Leichte Legierungen (Proto)',
    name_es      = 'Protoaleaciones ligeras',
    name_fr      = 'Proto-alliages légers',
    name_ru      = 'Опытные легкие сплавы'
WHERE LOWER(name) = LOWER('Proto Light Alloys');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Proto Radiolic Alloys', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Radiologische Legierungen (Proto)',
    name_es      = 'Aleaciones protorradiadas',
    name_fr      = 'Proto-alliages radiologiques',
    name_ru      = 'Сплавы для изготовления зондов'
WHERE LOWER(name) = LOWER('Proto Radiolic Alloys');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Refined Focus Crystals', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Raffinierte Laserkristalle',
    name_es      = 'Cristales de enfoque refinados',
    name_fr      = 'Cristaux de focalisation raffinés',
    name_ru      = 'Обработанные фокусировочные кристаллы'
WHERE LOWER(name) = LOWER('Refined Focus Crystals');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Salvaged Alloys', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Geborgene Legierungen',
    name_es      = 'Aleaciones recuperadas',
    name_fr      = 'Alliages récupérés',
    name_ru      = 'Захваченные сплавы'
WHERE LOWER(name) = LOWER('Salvaged Alloys');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Shield Emitters', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Schildemitter',
    name_es      = 'Emisor de escudos',
    name_fr      = 'Émetteurs de bouclier',
    name_ru      = 'Щитоизлучатели'
WHERE LOWER(name) = LOWER('Shield Emitters');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Shielding Sensors', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Schildsensoren',
    name_es      = 'Sensores de escudo',
    name_fr      = 'Capteurs de bouclier',
    name_ru      = 'Сенсоры системы экранирования'
WHERE LOWER(name) = LOWER('Shielding Sensors');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Tempered Alloys', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Vergütete Legierungen',
    name_es      = 'Aleaciones templadas',
    name_fr      = 'Alliages trempés',
    name_ru      = 'Закаленные сплавы'
WHERE LOWER(name) = LOWER('Tempered Alloys');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thermic Alloys', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Thermische Legierungen',
    name_es      = 'Aleaciones térmicas',
    name_fr      = 'Alliages thermiques',
    name_ru      = 'Термические сплавы'
WHERE LOWER(name) = LOWER('Thermic Alloys');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Flawed Focus Crystals', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Fehlerhafte Fokuskristalle',
    name_es      = 'Cristales de convergencia imperfectos',
    name_fr      = 'Cristaux de focalisation imparfaits',
    name_ru      = 'Поврежденные фокусировочные кристаллы'
WHERE LOWER(name) = LOWER('Flawed Focus Crystals');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Unknown', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Unbekannt',
    name_es      = 'Desconocido',
    name_fr      = 'Inconnu',
    name_ru      = 'Неизвестно'
WHERE LOWER(name) = LOWER('Unknown');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thargoid Carapace', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Thargoiden-Krustenschale',
    name_es      = 'Caparazón Thargoide',
    name_fr      = 'Carapace thargoid',
    name_ru      = 'Таргоидский панцирь'
WHERE LOWER(name) = LOWER('Thargoid Carapace');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thargoid Energy Cell', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Thargoiden-Energiezelle',
    name_es      = 'Célula de energía Thargoide',
    name_fr      = 'Cellule d’énergie thargoid',
    name_ru      = 'Таргоидская энергоячейка'
WHERE LOWER(name) = LOWER('Thargoid Energy Cell');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Sensor Fragment', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Sensorenfragment',
    name_es      = 'Fragmento de sensor',
    name_fr      = 'Fragment de capteur',
    name_ru      = 'Обломок сенсора'
WHERE LOWER(name) = LOWER('Sensor Fragment');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thargoid Organic Circuitry', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Organischer Schaltkreis der Thargoiden',
    name_es      = 'Circuitería orgánica Thargoide',
    name_fr      = 'Circuits organiques thargoids',
    name_ru      = 'Таргоидская органическая схема'
WHERE LOWER(name) = LOWER('Thargoid Organic Circuitry');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Thargoid Technological Components', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Technologiekomponenten der Thargoiden',
    name_es      = 'Componentes tecnológicos Thargoides',
    name_fr      = 'Composants technologiques thargoids',
    name_ru      = 'Компоненты таргоидской техники'
WHERE LOWER(name) = LOWER('Thargoid Technological Components');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Worn Shield Emitters', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Gebrauchte Schildemitter',
    name_es      = 'Emisor de escudos desgastado',
    name_fr      = 'Émetteurs de bouclier usés',
    name_ru      = 'Изношенные щитоизлучатели'
WHERE LOWER(name) = LOWER('Worn Shield Emitters');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Bio-Mechanical Conduits (Thargoid)', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Biomechanische Leiter',
    name_es      = 'Conductos biomecánicos',
    name_fr      = 'Conduits biomécaniques',
    name_ru      = 'Биомеханические энергопроводники'
WHERE LOWER(name) = LOWER('Bio-Mechanical Conduits (Thargoid)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Propulsion Elements (Thargoid)', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Schubantriebelemente',
    name_es      = 'Elementos de propulsión',
    name_fr      = 'Éléments de propulsion',
    name_ru      = 'Реактивные элементы'
WHERE LOWER(name) = LOWER('Propulsion Elements (Thargoid)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Weapon Parts (Thargoid)', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Waffenteile',
    name_es      = 'Piezas de armamento',
    name_fr      = 'Pièces d’armement',
    name_ru      = 'Детали вооружения'
WHERE LOWER(name) = LOWER('Weapon Parts (Thargoid)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Wreckage Components (Thargoid)', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Wrackteilkomponenten',
    name_es      = 'Restos de accidentes',
    name_fr      = 'Débris d’épave',
    name_ru      = 'Обломки кораблекрушений'
WHERE LOWER(name) = LOWER('Wreckage Components (Thargoid)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Corrosive Mechanisms (Thargoid)', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_es      = 'Mecanismos Corrosivos',
    name_ru      = 'Разъедающие механизмы'
WHERE LOWER(name) = LOWER('Corrosive Mechanisms (Thargoid)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Caustic Shard (Thargoid)', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_es      = 'Fragmento Caústico',
    name_ru      = 'Едкий осколок'
WHERE LOWER(name) = LOWER('Caustic Shard (Thargoid)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Caustic Crystal (Thargoid)', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_es      = 'Cristal Caústico',
    name_ru      = 'Едкий кристалл'
WHERE LOWER(name) = LOWER('Caustic Crystal (Thargoid)');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Heat Exposure Specimen', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_es      = 'Especimen de Exposición Térmica',
    name_fr      = 'Spécimen exposé à la chaleur',
    name_ru      = 'Образец теплового воздействия'
WHERE LOWER(name) = LOWER('Heat Exposure Specimen');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Phasing Membrane Residue', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_de      = 'Phasenmembranreste',
    name_es      = 'Residuo de Membrana Fásica',
    name_ru      = 'Остаток фазирующей мембраны'
WHERE LOWER(name) = LOWER('Phasing Membrane Residue');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Hardened Surface Fragments', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_es      = 'Fragmentos de superficie endurecida',
    name_fr      = 'Fragments superficiels durcis',
    name_ru      = 'Окаменелые фрагменты поверхности'
WHERE LOWER(name) = LOWER('Hardened Surface Fragments');
INSERT OR IGNORE INTO material_names (name, materialType)
VALUES ('Tactical Core Chip', 'Manufactured');
UPDATE material_names
SET materialType = 'Manufactured',
    name_es      = 'Chip de núcleo táctico',
    name_fr      = 'Puce tactique principale',
    name_ru      = 'Чип тактического ядра'
WHERE LOWER(name) = LOWER('Tactical Core Chip');

-- Add language columns to commodities
ALTER TABLE commodities
    ADD COLUMN commodity_de TEXT;
ALTER TABLE commodities
    ADD COLUMN commodity_fr TEXT;
ALTER TABLE commodities
    ADD COLUMN commodity_es TEXT;
ALTER TABLE commodities
    ADD COLUMN commodity_ru TEXT;
ALTER TABLE commodities
    ADD COLUMN commodity_uk TEXT;

-- commodity.csv
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Advanced Catalysers');
UPDATE commodities
SET commodity_de = 'Fortschr. Katalysatoren',
    commodity_es = 'Catalizadores avanzados',
    commodity_fr = 'Catalyseurs complexes',
    commodity_ru = 'Улучшенные катализаторы'
WHERE LOWER(commodity) = LOWER('Advanced Catalysers');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Advanced Medicines');
UPDATE commodities
SET commodity_de = 'Fortschrittliche Medikamente',
    commodity_es = 'Medicinas avanzadas',
    commodity_fr = 'Médicaments complexes',
    commodity_ru = 'Новейшие лекарства'
WHERE LOWER(commodity) = LOWER('Advanced Medicines');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ultra-Compact Processor Prototypes');
UPDATE commodities
SET commodity_de = 'Ultrakompakte Prozessorprototypen',
    commodity_es = 'Prototipos de procesador ultracompactos',
    commodity_fr = 'Prototypes de processeurs ultra-compacts',
    commodity_ru = 'Экспериментальные ультракомпактные процессоры'
WHERE LOWER(commodity) = LOWER('Ultra-Compact Processor Prototypes');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Eden Apples Of Aerial');
UPDATE commodities
SET commodity_de = 'Eden-Luftäpfel',
    commodity_es = 'Manzanas del Edén de Aerial',
    commodity_fr = 'Pommes d’eden d’Aerial',
    commodity_ru = 'Райские яблоки с Эриала'
WHERE LOWER(commodity) = LOWER('Eden Apples Of Aerial');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Aganippe Rush');
UPDATE commodities
SET commodity_de = 'Aganippe-Hirnhetzer',
    commodity_es = 'Incrementador de Aganippe',
    commodity_fr = 'Rush d’Aganippe',
    commodity_ru = 'Аганиппийский кайф'
WHERE LOWER(commodity) = LOWER('Aganippe Rush');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Agri-Medicines');
UPDATE commodities
SET commodity_de = 'Agrar-Medikamente',
    commodity_es = 'Medicinas agrícolas',
    commodity_fr = 'Agri-médicament(s)',
    commodity_ru = 'Ветмедикаменты'
WHERE LOWER(commodity) = LOWER('Agri-Medicines');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Agronomic Treatment');
UPDATE commodities
SET commodity_de = 'Agronomisches Mittel',
    commodity_es = 'Traitement agronomique',
    commodity_fr = 'Tratamiento agronómico',
    commodity_ru = 'Средство очистки почвы'
WHERE LOWER(commodity) = LOWER('Agronomic Treatment');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('AI Relics');
UPDATE commodities
SET commodity_de = 'KI-Relikte',
    commodity_es = 'Reliquias de IA',
    commodity_fr = 'Reliquats d’IA',
    commodity_ru = 'Фрагменты ИИ'
WHERE LOWER(commodity) = LOWER('AI Relics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Aisling Media Materials');
UPDATE commodities
SET commodity_de = 'Aislings Medienmaterialien',
    commodity_es = 'Materiales mediáticos de Aisling',
    commodity_fr = 'Dossiers de presse d’Aisling',
    commodity_ru = 'Агитационные материалы Айслинг'
WHERE LOWER(commodity) = LOWER('Aisling Media Materials');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Aisling Sealed Contracts');
UPDATE commodities
SET commodity_de = 'Aislings abgeschlossene Verträge',
    commodity_es = 'Contratos sellados de Aisling',
    commodity_fr = 'Contrats sous scellés d’Aisling',
    commodity_ru = 'Запечатанные контракты Айслинг'
WHERE LOWER(commodity) = LOWER('Aisling Sealed Contracts');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Aisling Programme Materials');
UPDATE commodities
SET commodity_de = 'Aislings Programmmaterialien',
    commodity_es = 'Materiales de programa de Aisling',
    commodity_fr = 'Programmes d’Aisling',
    commodity_ru = 'Политическая программа Айслинг'
WHERE LOWER(commodity) = LOWER('Aisling Programme Materials');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Alacarakmo Skin Art');
UPDATE commodities
SET commodity_de = 'Alacarakmo-Hautinlays',
    commodity_es = 'Tatuajes de Alacarakmo',
    commodity_fr = 'Art épidermique d’Alacarakmo',
    commodity_ru = 'Алакаракмовские татуировки'
WHERE LOWER(commodity) = LOWER('Alacarakmo Skin Art');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Albino Quechua Mammoth Meat');
UPDATE commodities
SET commodity_de = 'Albinomammut-Fleisch',
    commodity_es = 'Carne mamut albino Quechua',
    commodity_fr = 'Viande de mammouth albino quechuan',
    commodity_ru = 'Мясо белого кечуанского мамонта'
WHERE LOWER(commodity) = LOWER('Albino Quechua Mammoth Meat');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Alexandrite');
UPDATE commodities
SET commodity_de = 'Alexandrit',
    commodity_es = 'Alejandrita',
    commodity_fr = 'Alexandrite',
    commodity_ru = 'Александрит'
WHERE LOWER(commodity) = LOWER('Alexandrite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Algae');
UPDATE commodities
SET commodity_de = 'Algen',
    commodity_es = 'Algas',
    commodity_fr = 'Algue(s)',
    commodity_ru = 'Водоросли'
WHERE LOWER(commodity) = LOWER('Algae');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Leathery Eggs');
UPDATE commodities
SET commodity_de = 'Ledrige Eier',
    commodity_es = 'Huevos ornamentales',
    commodity_fr = 'Œufs antiques',
    commodity_ru = 'Кожистые яйца'
WHERE LOWER(commodity) = LOWER('Leathery Eggs');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Alliance Legislative Contracts');
UPDATE commodities
SET commodity_de = 'Gesetzesverträge der Allianz',
    commodity_es = 'Contratos legislativos de la Alianza',
    commodity_fr = 'Accords législatifs de l’Alliance',
    commodity_ru = 'Законодательные договоры Альянса'
WHERE LOWER(commodity) = LOWER('Alliance Legislative Contracts');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Alliance Legislative Records');
UPDATE commodities
SET commodity_de = 'Gesetzeslisten der Allianz',
    commodity_es = 'Registros legislativos de la Alianza',
    commodity_fr = 'Dossiers législatifs de l’Alliance',
    commodity_ru = 'Законодательные отчеты Альянса'
WHERE LOWER(commodity) = LOWER('Alliance Legislative Records');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Alliance Trade Agreements');
UPDATE commodities
SET commodity_de = 'Handelsabkommen der Allianz',
    commodity_es = 'Acuerdos comerciales de la Alianza',
    commodity_fr = 'Accords commerciaux de l’Alliance',
    commodity_ru = 'Торговые соглашения Альянса'
WHERE LOWER(commodity) = LOWER('Alliance Trade Agreements');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Altairian Skin');
UPDATE commodities
SET commodity_de = 'Altair-Haut',
    commodity_es = 'Piel altairiana',
    commodity_fr = 'Peau d’Altaïr',
    commodity_ru = 'Альтаирская кожа'
WHERE LOWER(commodity) = LOWER('Altairian Skin');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Aluminium');
UPDATE commodities
SET commodity_es = 'Aluminio',
    commodity_fr = 'Aluminium',
    commodity_ru = 'Алюминий'
WHERE LOWER(commodity) = LOWER('Aluminium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Alya Body Soap');
UPDATE commodities
SET commodity_de = 'Alya-Körperseife',
    commodity_es = 'Jabón corporal de Alya',
    commodity_fr = 'Savon corporel d’Alya',
    commodity_ru = 'Алийское мыло для тела'
WHERE LOWER(commodity) = LOWER('Alya Body Soap');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Guardian Casket');
UPDATE commodities
SET commodity_de = 'Guardianschatulle',
    commodity_es = 'Féretro guardián',
    commodity_fr = 'Cercueil des Guardians',
    commodity_ru = 'Шкатулка Стражей'
WHERE LOWER(commodity) = LOWER('Guardian Casket');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ancient Key (Guardian)');
UPDATE commodities
SET commodity_de = 'Uralter Schlüssel',
    commodity_es = 'Llave antigua',
    commodity_fr = 'Clé antique',
    commodity_ru = 'Древний ключ'
WHERE LOWER(commodity) = LOWER('Ancient Key (Guardian)');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Guardian Orb');
UPDATE commodities
SET commodity_de = 'Guardiankugel',
    commodity_es = 'Orbe guardián',
    commodity_fr = 'Orbe des Guardians',
    commodity_ru = 'Сфера Стражей'
WHERE LOWER(commodity) = LOWER('Guardian Orb');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Guardian Relic');
UPDATE commodities
SET commodity_de = 'Guardian-Relikt',
    commodity_es = 'Reliquia guardián',
    commodity_fr = 'Relique des Guardians',
    commodity_ru = 'Реликвия Стражей'
WHERE LOWER(commodity) = LOWER('Guardian Relic');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Unclassified Relic');
UPDATE commodities
SET commodity_de = 'Nicht klassifiziertes Relikt',
    commodity_es = 'Reliquia no clasificada',
    commodity_fr = 'Relique non classée',
    commodity_ru = 'Неопознанная реликвия'
WHERE LOWER(commodity) = LOWER('Unclassified Relic');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Guardian Tablet');
UPDATE commodities
SET commodity_de = 'Guardiantafel',
    commodity_es = 'Tablilla guardián',
    commodity_fr = 'Tablette des Guardians',
    commodity_ru = 'Табличка Стражей'
WHERE LOWER(commodity) = LOWER('Guardian Tablet');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Guardian Totem');
UPDATE commodities
SET commodity_de = 'Guardian-Totem',
    commodity_es = 'Tótem guardián',
    commodity_fr = 'Totem des Guardians',
    commodity_ru = 'Тотем Стражей'
WHERE LOWER(commodity) = LOWER('Guardian Totem');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Guardian Urn');
UPDATE commodities
SET commodity_de = 'Guardian-Urne',
    commodity_es = 'Urna guardián',
    commodity_fr = 'Urne des Guardians',
    commodity_ru = 'Урна Стражей'
WHERE LOWER(commodity) = LOWER('Guardian Urn');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Anduliga Fire Works');
UPDATE commodities
SET commodity_de = 'Anduliga-Feuerwerk',
    commodity_es = 'Fuegos artificiales de Anduliga',
    commodity_fr = 'Feux d’artifice d’Anduliga',
    commodity_ru = 'Андулигские фейерверки'
WHERE LOWER(commodity) = LOWER('Anduliga Fire Works');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Crom Silver Fesh');
UPDATE commodities
SET commodity_es = 'Aliento de plata de Crom',
    commodity_fr = 'Fesh argenté de Crom',
    commodity_ru = 'Кромская серебряная дурь'
WHERE LOWER(commodity) = LOWER('Crom Silver Fesh');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Animal Meat');
UPDATE commodities
SET commodity_de = 'Tierfleisch',
    commodity_es = 'Carne de animales',
    commodity_fr = 'Viande',
    commodity_ru = 'Мясо животных'
WHERE LOWER(commodity) = LOWER('Animal Meat');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Animal Monitors');
UPDATE commodities
SET commodity_de = 'Tierüberwachung',
    commodity_es = 'Monitores de animales',
    commodity_fr = 'Sys. surveillance animale',
    commodity_ru = 'Мониторы фауны'
WHERE LOWER(commodity) = LOWER('Animal Monitors');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Antimatter Containment Unit');
UPDATE commodities
SET commodity_de = 'Antimaterie-Transporteinheit',
    commodity_es = 'Unidad de confinamiento de antimateria',
    commodity_fr = 'Unité de confinement d’antimatière',
    commodity_ru = 'Контейнер с антиматерией'
WHERE LOWER(commodity) = LOWER('Antimatter Containment Unit');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Antique Jewellery');
UPDATE commodities
SET commodity_de = 'Antiker Schmuck',
    commodity_es = 'Joyería antigua',
    commodity_fr = 'Bijoux antiques',
    commodity_ru = 'Древние ювелирные украшения'
WHERE LOWER(commodity) = LOWER('Antique Jewellery');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Antiquities');
UPDATE commodities
SET commodity_de = 'Antiquitäten',
    commodity_es = 'Antigüedades',
    commodity_fr = 'Antiquités',
    commodity_ru = 'Древние реликвии'
WHERE LOWER(commodity) = LOWER('Antiquities');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Any Na Coffee');
UPDATE commodities
SET commodity_es = 'Café de Any Na',
    commodity_fr = 'Café Any Na',
    commodity_ru = 'Кофе Any Na'
WHERE LOWER(commodity) = LOWER('Any Na Coffee');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Apa Vietii');
UPDATE commodities
SET commodity_fr = 'Apa Vietii',
    commodity_ru = 'Apa Vietii'
WHERE LOWER(commodity) = LOWER('Apa Vietii');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Aquaponic Systems');
UPDATE commodities
SET commodity_de = 'Aquaponiksysteme',
    commodity_es = 'Sistemas de hidroponía',
    commodity_fr = 'Systèmes aquaponiques',
    commodity_ru = 'Аквапонные системы'
WHERE LOWER(commodity) = LOWER('Aquaponic Systems');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Arouca Conventual Sweets');
UPDATE commodities
SET commodity_de = 'Arouca-Klosterbonbons',
    commodity_es = 'Dulces conventuales de Arouca',
    commodity_fr = 'Bonbons monastiques d’Arouca',
    commodity_ru = 'Монастырские сладости Ароуки'
WHERE LOWER(commodity) = LOWER('Arouca Conventual Sweets');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Articulation Motors');
UPDATE commodities
SET commodity_de = 'Gelenkmotoren',
    commodity_es = 'Motores de articulación',
    commodity_fr = 'Moteurs à articulation',
    commodity_ru = 'Шарнирные моторы'
WHERE LOWER(commodity) = LOWER('Articulation Motors');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Assault Plans');
UPDATE commodities
SET commodity_de = 'Angriffspläne',
    commodity_es = 'Planes de asalto',
    commodity_fr = 'Plans d’assaut',
    commodity_ru = 'Планы атак'
WHERE LOWER(commodity) = LOWER('Assault Plans');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Atmospheric Processors');
UPDATE commodities
SET commodity_de = 'Atmosphärenprozessoren',
    commodity_es = 'Procesadores atmosféricos',
    commodity_fr = 'Processeurs atmosphériques',
    commodity_ru = 'Атмосферный процессор'
WHERE LOWER(commodity) = LOWER('Atmospheric Processors');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Auto-Fabricators');
UPDATE commodities
SET commodity_de = 'Fabrikatoren',
    commodity_es = 'Autofabricantes',
    commodity_fr = 'Dispositifs d’autofabrication',
    commodity_ru = 'Автосинтезаторы'
WHERE LOWER(commodity) = LOWER('Auto-Fabricators');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('AZ Cancri Formula 42');
UPDATE commodities
SET commodity_de = 'AZ Cancri Formel 42',
    commodity_es = 'Fórmula 42 de AZ Cancri',
    commodity_fr = 'Formule 42 d’AZ Cancri',
    commodity_ru = 'Формула 42 AZ Cancri'
WHERE LOWER(commodity) = LOWER('AZ Cancri Formula 42');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Baked Greebles');
UPDATE commodities
SET commodity_de = 'Gebackene Greebles',
    commodity_es = 'Cocido de greebles',
    commodity_fr = 'Greebles cuites',
    commodity_ru = 'Печеные гриблы'
WHERE LOWER(commodity) = LOWER('Baked Greebles');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Baltah’sine Vacuum Krill');
UPDATE commodities
SET commodity_de = 'Vakuumkrill von Baltah’sine',
    commodity_es = 'Krill del vacío de Baltah’Sine',
    commodity_fr = 'Crevettes stellaires de Baltah’sine',
    commodity_ru = 'Вакуумный криль с Балтах’сине'
WHERE LOWER(commodity) = LOWER('Baltah’sine Vacuum Krill');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Banki Amphibious Leather');
UPDATE commodities
SET commodity_de = 'Banki-Amphibienleder',
    commodity_es = 'Cuero anfibio de Banki',
    commodity_fr = 'Cuir d’amphibien de Banki',
    commodity_ru = 'Кожа банкийских амфибий'
WHERE LOWER(commodity) = LOWER('Banki Amphibious Leather');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Basic Medicines');
UPDATE commodities
SET commodity_de = 'Allgemeine Medikamente',
    commodity_es = 'Medicinas básicas',
    commodity_fr = 'Médicament(s) simple(s)',
    commodity_ru = 'Основные лекарства'
WHERE LOWER(commodity) = LOWER('Basic Medicines');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Narcotics');
UPDATE commodities
SET commodity_de = 'Drogen',
    commodity_es = 'Narcóticos',
    commodity_fr = 'Narcotique(s)',
    commodity_ru = 'Наркотики'
WHERE LOWER(commodity) = LOWER('Narcotics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Bast Snake Gin');
UPDATE commodities
SET commodity_es = 'Elixir de serpiente de Bast',
    commodity_fr = 'Gin de serpent Bast',
    commodity_ru = 'Настойка из бастетовых змей'
WHERE LOWER(commodity) = LOWER('Bast Snake Gin');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Battle Weapons');
UPDATE commodities
SET commodity_de = 'Kriegswaffen',
    commodity_es = 'Armas de batalla',
    commodity_fr = 'Armes militaires',
    commodity_ru = 'Военное оружие'
WHERE LOWER(commodity) = LOWER('Battle Weapons');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Bauxite');
UPDATE commodities
SET commodity_de = 'Bauxit',
    commodity_es = 'Bauxita',
    commodity_fr = 'Bauxite',
    commodity_ru = 'Боксит'
WHERE LOWER(commodity) = LOWER('Bauxite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Beer');
UPDATE commodities
SET commodity_de = 'Bier',
    commodity_es = 'Cerveza',
    commodity_fr = 'Bière',
    commodity_ru = 'Пиво'
WHERE LOWER(commodity) = LOWER('Beer');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Belalans Ray Leather');
UPDATE commodities
SET commodity_de = 'Belalans-Rochenleder',
    commodity_es = 'Cuero de raya de Belalans',
    commodity_fr = 'Cuir de raie de Belalans',
    commodity_ru = 'Кожа белаланских скатов'
WHERE LOWER(commodity) = LOWER('Belalans Ray Leather');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Benitoite');
UPDATE commodities
SET commodity_de = 'Benitoit',
    commodity_es = 'Benitoíta',
    commodity_fr = 'Bénitoïte',
    commodity_ru = 'Бенитоит'
WHERE LOWER(commodity) = LOWER('Benitoite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Bertrandite');
UPDATE commodities
SET commodity_de = 'Bertrandit',
    commodity_es = 'Bertrandita',
    commodity_fr = 'Bertrandite',
    commodity_ru = 'Бертрандит'
WHERE LOWER(commodity) = LOWER('Bertrandite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Beryllium');
UPDATE commodities
SET commodity_es = 'Berilio',
    commodity_fr = 'Béryllium',
    commodity_ru = 'Бериллий'
WHERE LOWER(commodity) = LOWER('Beryllium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Bioreducing Lichen');
UPDATE commodities
SET commodity_de = 'Bioreduzierende Flechten',
    commodity_es = 'Líquenes biorreductores',
    commodity_fr = 'Lichen bioréducteur',
    commodity_ru = 'Лишайник-биоредуктор'
WHERE LOWER(commodity) = LOWER('Bioreducing Lichen');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Biowaste');
UPDATE commodities
SET commodity_de = 'Biomüll',
    commodity_es = 'Residuos biológicos',
    commodity_fr = 'Biodéchets',
    commodity_ru = 'Биоотходы'
WHERE LOWER(commodity) = LOWER('Biowaste');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Bismuth');
UPDATE commodities
SET commodity_de = 'Bismut',
    commodity_es = 'Bismuto',
    commodity_fr = 'Bismuth',
    commodity_ru = 'Висмут'
WHERE LOWER(commodity) = LOWER('Bismuth');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Azure Milk');
UPDATE commodities
SET commodity_de = 'Azurmilch',
    commodity_es = 'Leche de azure',
    commodity_fr = 'Lait d’Azure',
    commodity_ru = 'Лазурное молоко'
WHERE LOWER(commodity) = LOWER('Azure Milk');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Bootleg Liquor');
UPDATE commodities
SET commodity_de = 'Schmuggelschnaps',
    commodity_es = 'Licor de caña',
    commodity_fr = 'Liqueur de contrebande',
    commodity_ru = 'Самогон'
WHERE LOWER(commodity) = LOWER('Bootleg Liquor');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Borasetani Pathogenetics');
UPDATE commodities
SET commodity_de = 'Borasetani-Pathogenetika',
    commodity_es = 'Patógenos de Borasetani',
    commodity_fr = 'Fléau de Borasetani',
    commodity_ru = 'Боразетанские патогены'
WHERE LOWER(commodity) = LOWER('Borasetani Pathogenetics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Bromellite');
UPDATE commodities
SET commodity_de = 'Bromellit',
    commodity_es = 'Bromellita',
    commodity_fr = 'Bromellite',
    commodity_ru = 'Бромеллит'
WHERE LOWER(commodity) = LOWER('Bromellite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Buckyball Beer Mats');
UPDATE commodities
SET commodity_de = 'Buckyball-Bierdeckel',
    commodity_es = 'Posavasos Buckyball',
    commodity_fr = 'Sous-bocks Buckyball',
    commodity_ru = 'Подставка под пиво Buckyball'
WHERE LOWER(commodity) = LOWER('Buckyball Beer Mats');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Building Fabricators');
UPDATE commodities
SET commodity_de = 'Baufabrikatoren',
    commodity_es = 'Constructores',
    commodity_fr = 'Auto-bâtisseurs',
    commodity_ru = 'Строительные синтезаторы'
WHERE LOWER(commodity) = LOWER('Building Fabricators');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Burnham Bile Distillate');
UPDATE commodities
SET commodity_de = 'Burnham-Körpersaftdestillat',
    commodity_es = 'Licor bilioso de Burnham',
    commodity_fr = 'Distillé bileux de Burnham',
    commodity_ru = 'Бернхемский желчный дистиллят'
WHERE LOWER(commodity) = LOWER('Burnham Bile Distillate');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('CD-75 Kitten Brand Coffee');
UPDATE commodities
SET commodity_es = 'Café marca Kitten de CD-75',
    commodity_fr = 'Café Kitten CD-75',
    commodity_ru = 'Кошачий кофе CD-75'
WHERE LOWER(commodity) = LOWER('CD-75 Kitten Brand Coffee');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Centauri Mega Gin');
UPDATE commodities
SET commodity_es = 'Ginebra Mega de Alpha Centauri',
    commodity_fr = 'Gin Centauri Mega',
    commodity_ru = 'Кентаврский мега-джин'
WHERE LOWER(commodity) = LOWER('Centauri Mega Gin');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ceramic Composites');
UPDATE commodities
SET commodity_de = 'Keramik-Verbundwerkstoffe',
    commodity_es = 'Compuestos cerámicos',
    commodity_fr = 'Composés en céramique',
    commodity_ru = 'Керамокомпозиты'
WHERE LOWER(commodity) = LOWER('Ceramic Composites');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ceremonial Heike Tea');
UPDATE commodities
SET commodity_de = 'Heike-Zeremonientee',
    commodity_es = 'Té ceremonial de Heike',
    commodity_fr = 'Thé de cérémonie de Heike',
    commodity_ru = 'Церемониальный чай Heike'
WHERE LOWER(commodity) = LOWER('Ceremonial Heike Tea');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Aepyornis Egg');
UPDATE commodities
SET commodity_de = 'Aepyornis-Eier',
    commodity_es = 'Huevo de aepyornis',
    commodity_fr = 'Œuf d’Aepyornis',
    commodity_ru = 'Яйцо эпиорниса'
WHERE LOWER(commodity) = LOWER('Aepyornis Egg');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ceti Rabbits');
UPDATE commodities
SET commodity_de = 'Ceti-Kaninchen',
    commodity_es = 'Conejos de 47 Ceti',
    commodity_fr = 'Lapins de Ceti',
    commodity_ru = 'Кролики с Кита'
WHERE LOWER(commodity) = LOWER('Ceti Rabbits');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Chameleon Cloth');
UPDATE commodities
SET commodity_de = 'Chamäleon-Kleidung',
    commodity_es = 'Ropa mimética',
    commodity_fr = 'Tissu caméléon',
    commodity_ru = 'Ткань-хамелеон'
WHERE LOWER(commodity) = LOWER('Chameleon Cloth');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Chateau De Aegaeon');
UPDATE commodities
SET commodity_es = 'Cheateau de Aegaeon',
    commodity_fr = 'Château Aegaeon',
    commodity_ru = 'Шато де Эгеон'
WHERE LOWER(commodity) = LOWER('Chateau De Aegaeon');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Chemical Waste');
UPDATE commodities
SET commodity_de = 'Chemiemüll',
    commodity_es = 'Residuos químicos',
    commodity_fr = 'Déchets chimiques',
    commodity_ru = 'Радиоактивные материалы'
WHERE LOWER(commodity) = LOWER('Chemical Waste');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Cherbones Blood Crystals');
UPDATE commodities
SET commodity_de = 'Cherbones-Blutkristalle',
    commodity_es = 'Cristales sangre de Cherbones',
    commodity_fr = 'Cristaux de sang de Cherbones',
    commodity_ru = 'Чербонские кровавые кристаллы'
WHERE LOWER(commodity) = LOWER('Cherbones Blood Crystals');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Chi Eridani Marine Paste');
UPDATE commodities
SET commodity_de = 'Chi-Eridani-Meerespaste',
    commodity_es = 'Pasta marina de Chi Eridani',
    commodity_fr = 'Pâte marine de Chi Eridani',
    commodity_ru = 'Морская паста с Чи Эридана'
WHERE LOWER(commodity) = LOWER('Chi Eridani Marine Paste');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Classified Experimental Equipment');
UPDATE commodities
SET commodity_de = 'Klassifizierte experimentelle Ausrüstung',
    commodity_es = 'Équipement expérimental classifié',
    commodity_fr = 'Equipo experimental clasificado',
    commodity_ru = 'Классифицированное экспериментальное оборудование'
WHERE LOWER(commodity) = LOWER('Classified Experimental Equipment');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Clothing');
UPDATE commodities
SET commodity_de = 'Kleidung',
    commodity_es = 'Ropa',
    commodity_fr = 'Vêtements',
    commodity_ru = 'Одежда'
WHERE LOWER(commodity) = LOWER('Clothing');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('CMM Composite');
UPDATE commodities
SET commodity_de = 'CMM-Komposit',
    commodity_es = 'Compuestos CMM',
    commodity_fr = 'Composite MMC',
    commodity_ru = 'CMM-композит'
WHERE LOWER(commodity) = LOWER('CMM Composite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Cobalt');
UPDATE commodities
SET commodity_es = 'Cobalto',
    commodity_fr = 'Cobalt',
    commodity_ru = 'Кобальт'
WHERE LOWER(commodity) = LOWER('Cobalt');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Coffee');
UPDATE commodities
SET commodity_de = 'Kaffee',
    commodity_es = 'Café',
    commodity_fr = 'Café(s)',
    commodity_ru = 'Кофе'
WHERE LOWER(commodity) = LOWER('Coffee');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Coltan');
UPDATE commodities
SET commodity_es = 'Coltán',
    commodity_fr = 'Coltan',
    commodity_ru = 'Колтан'
WHERE LOWER(commodity) = LOWER('Coltan');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Combat Stabilisers');
UPDATE commodities
SET commodity_de = 'Kampfstabilisatoren',
    commodity_es = 'Estabilizadores de combate',
    commodity_fr = 'Stabilisateurs de combat',
    commodity_ru = 'Боевые стабилизаторы'
WHERE LOWER(commodity) = LOWER('Combat Stabilisers');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Commercial Samples');
UPDATE commodities
SET commodity_de = 'Werbeproben',
    commodity_es = 'Muestras comerciales',
    commodity_fr = 'Échantillons commerciaux',
    commodity_ru = 'Рекламные образцы'
WHERE LOWER(commodity) = LOWER('Commercial Samples');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Computer Components');
UPDATE commodities
SET commodity_de = 'Computerteile',
    commodity_es = 'Componentes informáticos',
    commodity_fr = 'Composants d’Ordinateur',
    commodity_ru = 'Компьютерные компоненты'
WHERE LOWER(commodity) = LOWER('Computer Components');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Conductive Fabrics');
UPDATE commodities
SET commodity_de = 'Leitfähige Stoffe',
    commodity_es = 'Tejidos conductivos',
    commodity_fr = 'Tissus conducteurs',
    commodity_ru = 'Проводящая ткань'
WHERE LOWER(commodity) = LOWER('Conductive Fabrics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Consumer Technology');
UPDATE commodities
SET commodity_de = 'Unterhaltungselektronik',
    commodity_es = 'Tecnología de consumo',
    commodity_fr = 'Électronique grand public',
    commodity_ru = 'Потребительские товары'
WHERE LOWER(commodity) = LOWER('Consumer Technology');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Micro-weave Cooling Hoses');
UPDATE commodities
SET commodity_de = 'Mikroflecht-Kühlschläuche',
    commodity_es = 'Mangueras de microtejidos',
    commodity_fr = 'Tuyaux de refroidissement',
    commodity_ru = 'Шланги системы охлаждения малых диаметров'
WHERE LOWER(commodity) = LOWER('Micro-weave Cooling Hoses');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Copper');
UPDATE commodities
SET commodity_de = 'Kupfer',
    commodity_es = 'Cobre',
    commodity_fr = 'Cuivre',
    commodity_ru = 'Медь'
WHERE LOWER(commodity) = LOWER('Copper');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Coquim Spongiform Victuals');
UPDATE commodities
SET commodity_de = 'Coquim Schwammkost',
    commodity_es = 'Víveres espongiformes de Coquim',
    commodity_fr = 'Rations spongiformes de Coquim',
    commodity_ru = 'Губковые продукты с Кокуима'
WHERE LOWER(commodity) = LOWER('Coquim Spongiform Victuals');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Coral Sap');
UPDATE commodities
SET commodity_es = 'Savia del Árbol Coral',
    commodity_ru = 'Коралловая смола'
WHERE LOWER(commodity) = LOWER('Coral Sap');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Revolutionary supplies');
UPDATE commodities
SET commodity_de = 'Revolutionsmaterialien',
    commodity_es = 'Suministros revolucionarios',
    commodity_fr = 'Matériel révolutionnaire',
    commodity_ru = 'Припасы революционеров'
WHERE LOWER(commodity) = LOWER('Revolutionary supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Crop Harvesters');
UPDATE commodities
SET commodity_de = 'Erntemaschinen',
    commodity_es = 'Segadoras de cultivos',
    commodity_fr = 'Moissonneuses',
    commodity_ru = 'Уборочный комбайн'
WHERE LOWER(commodity) = LOWER('Crop Harvesters');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Cryolite');
UPDATE commodities
SET commodity_de = 'Kriolyth',
    commodity_es = 'Criolita',
    commodity_fr = 'Cyolite',
    commodity_ru = 'Криолит'
WHERE LOWER(commodity) = LOWER('Cryolite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Crystalline Spheres');
UPDATE commodities
SET commodity_de = 'Kristallkugeln',
    commodity_es = 'Esferas cristalinas',
    commodity_fr = 'Sphères cristallines',
    commodity_ru = 'Прозрачные сферы'
WHERE LOWER(commodity) = LOWER('Crystalline Spheres');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Damaged Escape Pod');
UPDATE commodities
SET commodity_de = 'Beschädigte Rettungskapsel',
    commodity_es = 'Cápsula de escape dañada',
    commodity_fr = 'Nacelle d’évacuation endommagée',
    commodity_ru = 'Поврежденная спасательная капсула'
WHERE LOWER(commodity) = LOWER('Damaged Escape Pod');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Damna Carapaces');
UPDATE commodities
SET commodity_de = 'Damna-Schildpanzer',
    commodity_es = 'Caparazones de Damna',
    commodity_fr = 'Carapaces de Damna',
    commodity_ru = 'Панцири с Дамны'
WHERE LOWER(commodity) = LOWER('Damna Carapaces');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Data Core');
UPDATE commodities
SET commodity_de = 'Datenkern',
    commodity_es = 'Núcleo de datos',
    commodity_fr = 'Centre de données',
    commodity_ru = 'Ядро данных'
WHERE LOWER(commodity) = LOWER('Data Core');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Delta Phoenicis Palms');
UPDATE commodities
SET commodity_de = 'Delta-Phönix-Palmen',
    commodity_es = 'Palmeras de Delta Phoenicis',
    commodity_fr = 'Palmiers de Delta Phoenicis',
    commodity_ru = 'Фениксовые пальмы с Дельты'
WHERE LOWER(commodity) = LOWER('Delta Phoenicis Palms');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Deuringas Truffles');
UPDATE commodities
SET commodity_de = 'Deuringas-Trüffel',
    commodity_es = 'Trufas de Deuringas',
    commodity_fr = 'Truffes de Deuringas',
    commodity_ru = 'Трюфели с Деуринги'
WHERE LOWER(commodity) = LOWER('Deuringas Truffles');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Hardware Diagnostic Sensor');
UPDATE commodities
SET commodity_de = 'Hardware-Diagnostiksensor',
    commodity_es = 'Sensor diagnóstico de hardware',
    commodity_fr = 'Capteur diagnostic d’équipement',
    commodity_ru = 'Сенсор диагностики оборудования'
WHERE LOWER(commodity) = LOWER('Hardware Diagnostic Sensor');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Diplomatic Bag');
UPDATE commodities
SET commodity_de = 'Diplomatentasche',
    commodity_es = 'Valija diplomática',
    commodity_fr = 'Mallette(s) diplomatique(s)',
    commodity_ru = 'Дипломатическая сумка'
WHERE LOWER(commodity) = LOWER('Diplomatic Bag');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Diso Ma Corn');
UPDATE commodities
SET commodity_de = 'Diso-Ma-Mais',
    commodity_es = 'Maíz ma de Diso',
    commodity_fr = 'Ma Corn de Diso',
    commodity_ru = 'Зерна дисо ма'
WHERE LOWER(commodity) = LOWER('Diso Ma Corn');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Domestic Appliances');
UPDATE commodities
SET commodity_de = 'Haushaltsgeräte',
    commodity_es = 'Electrodomésticos',
    commodity_fr = 'Équipement ménager',
    commodity_ru = 'Бытовая техника'
WHERE LOWER(commodity) = LOWER('Domestic Appliances');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Limpets');
UPDATE commodities
SET commodity_es = 'Drones',
    commodity_fr = 'Drones',
    commodity_ru = 'Дроны'
WHERE LOWER(commodity) = LOWER('Limpets');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Duradrives');
UPDATE commodities
SET commodity_fr = 'Duradrives',
    commodity_ru = 'Дюрадрайвы'
WHERE LOWER(commodity) = LOWER('Duradrives');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Earth Relics');
UPDATE commodities
SET commodity_de = 'Erdrelikte',
    commodity_es = 'Reliquias de la vieja Tierra',
    commodity_fr = 'Reliques de la Terre',
    commodity_ru = 'Реликвии с Земли'
WHERE LOWER(commodity) = LOWER('Earth Relics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Eleu Thermals');
UPDATE commodities
SET commodity_de = 'Eleu-Thermotextilien',
    commodity_es = 'Tejidos térmicos de eleu',
    commodity_fr = 'Sous-vêtements thermiques d’Eleu',
    commodity_ru = 'Элеусская теплая одежда'
WHERE LOWER(commodity) = LOWER('Eleu Thermals');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Emergency Power Cells');
UPDATE commodities
SET commodity_de = 'Notfall-Energiezellen',
    commodity_es = 'Células energía auxiliar',
    commodity_fr = 'Cellules d’énergie de secours',
    commodity_ru = 'Аварийные энергоячейки'
WHERE LOWER(commodity) = LOWER('Emergency Power Cells');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Encrypted Data Storage');
UPDATE commodities
SET commodity_de = 'Verschlüsselter Datenträger',
    commodity_es = 'Datos encriptados',
    commodity_fr = 'Périphérique de stockage crypté',
    commodity_ru = 'Зашифрованный носитель данных'
WHERE LOWER(commodity) = LOWER('Encrypted Data Storage');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Encrypted Correspondence');
UPDATE commodities
SET commodity_de = 'Verschlüsselte Korrespondenz',
    commodity_es = 'Correspondencia encriptada',
    commodity_fr = 'Correspondance cryptée',
    commodity_ru = 'Шифрованная переписка'
WHERE LOWER(commodity) = LOWER('Encrypted Correspondence');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Eranin Pearl Whisky');
UPDATE commodities
SET commodity_es = 'Whisky perlado de Eranin',
    commodity_fr = 'Whisky Eranin Pearl',
    commodity_ru = 'Эранинское жемчужное виски'
WHERE LOWER(commodity) = LOWER('Eranin Pearl Whisky');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Eshu Umbrellas');
UPDATE commodities
SET commodity_de = 'Eshu-Regenschirme',
    commodity_es = 'Paraguas de Eshu',
    commodity_fr = 'Ombrelles eshu',
    commodity_ru = 'Зонтики Эшу'
WHERE LOWER(commodity) = LOWER('Eshu Umbrellas');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Esuseku Caviar');
UPDATE commodities
SET commodity_de = 'Esuseku-Kaviar',
    commodity_es = 'Caviar de Esuseku',
    commodity_fr = 'Caviar d’Esuseku',
    commodity_ru = 'Эсусекусская осетровая икра'
WHERE LOWER(commodity) = LOWER('Esuseku Caviar');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ethgreze Tea Buds');
UPDATE commodities
SET commodity_de = 'Ethgreze-Teeknospen',
    commodity_es = 'Brotes de té de Ethgreze',
    commodity_fr = 'Bourgeons de thé d’Ethgreze',
    commodity_ru = 'Чайные бутоны Этгриза'
WHERE LOWER(commodity) = LOWER('Ethgreze Tea Buds');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Evacuation Shelter');
UPDATE commodities
SET commodity_de = 'Evakuierungsschutz',
    commodity_es = 'Refugio de evacuación',
    commodity_fr = 'Abri d’urgence',
    commodity_ru = 'Эвакуационное убежище'
WHERE LOWER(commodity) = LOWER('Evacuation Shelter');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Exhaust Manifold');
UPDATE commodities
SET commodity_de = 'Krümmer',
    commodity_es = 'Colector de escape',
    commodity_fr = 'Collecteur d’échappement',
    commodity_ru = 'Выпускной коллектор'
WHERE LOWER(commodity) = LOWER('Exhaust Manifold');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Explosives');
UPDATE commodities
SET commodity_de = 'Sprengstoffe',
    commodity_es = 'Explosivos',
    commodity_fr = 'Explosif(s)',
    commodity_ru = 'Взрывчатка'
WHERE LOWER(commodity) = LOWER('Explosives');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Liberal Federal Aid');
UPDATE commodities
SET commodity_de = 'Liberale Föderiertenhilfe',
    commodity_es = 'Ayuda federal liberal',
    commodity_fr = 'Cargaisons d’aide humanitaire libérales',
    commodity_ru = 'Либеральные субсидии Федерации'
WHERE LOWER(commodity) = LOWER('Liberal Federal Aid');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Liberal Federal Packages');
UPDATE commodities
SET commodity_de = 'Liberales Föderiertenpaket',
    commodity_es = 'Paquetes federales liberales',
    commodity_fr = 'Colis fédéraux libéraux',
    commodity_ru = 'Либеральный груз Федерации'
WHERE LOWER(commodity) = LOWER('Liberal Federal Packages');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Fish');
UPDATE commodities
SET commodity_de = 'Fisch',
    commodity_es = 'Pescado',
    commodity_fr = 'Poisson(s)',
    commodity_ru = 'Рыба'
WHERE LOWER(commodity) = LOWER('Fish');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Food Cartridges');
UPDATE commodities
SET commodity_de = 'Nahrungskartuschen',
    commodity_es = 'Cartuchos de alimentos',
    commodity_fr = 'Cartouche(s) alimentaire(s)',
    commodity_ru = 'Пищевые брикеты'
WHERE LOWER(commodity) = LOWER('Food Cartridges');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Fossil Remnants');
UPDATE commodities
SET commodity_de = 'Fossile Überreste',
    commodity_es = 'Restos de fósiles',
    commodity_fr = 'Fossiles',
    commodity_ru = 'Ископаемые останки'
WHERE LOWER(commodity) = LOWER('Fossil Remnants');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Fruit and Vegetables');
UPDATE commodities
SET commodity_de = 'Obst und Gemüse',
    commodity_es = 'Frutas y verduras',
    commodity_fr = 'Fruits et légumes',
    commodity_ru = 'Фрукты и овощи'
WHERE LOWER(commodity) = LOWER('Fruit and Vegetables');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Fujin Tea');
UPDATE commodities
SET commodity_de = 'Fujin-Tee',
    commodity_es = 'Té de Fujin',
    commodity_fr = 'Thé de Fujin',
    commodity_ru = 'Фудзинский чай'
WHERE LOWER(commodity) = LOWER('Fujin Tea');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Galactic Travel Guide');
UPDATE commodities
SET commodity_de = 'Galaktischer Reiseführer',
    commodity_es = 'Guía de viaje galáctico',
    commodity_fr = 'Guide de voyage galactique',
    commodity_ru = 'Путеводитель галактического путешественника'
WHERE LOWER(commodity) = LOWER('Galactic Travel Guide');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Gallite');
UPDATE commodities
SET commodity_de = 'Gallit',
    commodity_es = 'Galita',
    commodity_fr = 'Gallite',
    commodity_ru = 'Галлит'
WHERE LOWER(commodity) = LOWER('Gallite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Gallium');
UPDATE commodities
SET commodity_es = 'Galio',
    commodity_fr = 'Gallium',
    commodity_ru = 'Галлий'
WHERE LOWER(commodity) = LOWER('Gallium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Geawen Dance Dust');
UPDATE commodities
SET commodity_de = 'Geawen-Tanzstaub',
    commodity_es = 'Polvo de baile de Geawen',
    commodity_fr = 'Poussière de danse Geawen',
    commodity_ru = 'Геавенская танцевальная пыль'
WHERE LOWER(commodity) = LOWER('Geawen Dance Dust');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Gene Bank');
UPDATE commodities
SET commodity_de = 'Gen-Datenbank',
    commodity_es = 'Bancos de genes',
    commodity_fr = 'Banque de gènes',
    commodity_ru = 'Генотека'
WHERE LOWER(commodity) = LOWER('Gene Bank');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Geological Equipment');
UPDATE commodities
SET commodity_de = 'Geologie-Ausrüstung',
    commodity_es = 'Equipamiento geológico',
    commodity_fr = 'Équipement géologique',
    commodity_ru = 'Геологическое оборудование'
WHERE LOWER(commodity) = LOWER('Geological Equipment');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Geological Samples');
UPDATE commodities
SET commodity_de = 'Geologische Proben',
    commodity_es = 'Muestras geológicas',
    commodity_fr = 'Échantillons géologiques',
    commodity_ru = 'Образцы породы'
WHERE LOWER(commodity) = LOWER('Geological Samples');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Gerasian Gueuze Beer');
UPDATE commodities
SET commodity_de = 'Gersianisches Gueuze-Bier',
    commodity_es = 'Cerveza gerasiana Gueuze',
    commodity_fr = 'Gueuze gerasianne',
    commodity_ru = 'Герасианское пиво гез'
WHERE LOWER(commodity) = LOWER('Gerasian Gueuze Beer');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Giant Irukama Snails');
UPDATE commodities
SET commodity_de = 'Irukama-Riesenschnecken',
    commodity_es = 'Caracoles gigantes de Ikurama',
    commodity_fr = 'Escargots géants d’Irukama',
    commodity_ru = 'Огромные улитки с Ирукамы'
WHERE LOWER(commodity) = LOWER('Giant Irukama Snails');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Giant Verrix');
UPDATE commodities
SET commodity_de = 'Riesen-Verrix',
    commodity_es = 'Verrix gigante',
    commodity_fr = 'Verrix géant',
    commodity_ru = 'Гигант Веррикс'
WHERE LOWER(commodity) = LOWER('Giant Verrix');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Gilya Signature Weapons');
UPDATE commodities
SET commodity_de = 'Gilya-Signaturwaffen',
    commodity_es = 'Armas personalizables de Gilya',
    commodity_fr = 'Armes personnalisées Gilyanes',
    commodity_ru = 'Именное оружие из системы Гилья'
WHERE LOWER(commodity) = LOWER('Gilya Signature Weapons');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Gold');
UPDATE commodities
SET commodity_es = 'Oro',
    commodity_fr = 'Or',
    commodity_ru = 'Золото'
WHERE LOWER(commodity) = LOWER('Gold');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Goman Yaupon Coffee');
UPDATE commodities
SET commodity_es = 'Café Yaupon de Goman',
    commodity_fr = 'Café Goman Yaupon',
    commodity_ru = 'Кофе Goman Yaupon'
WHERE LOWER(commodity) = LOWER('Goman Yaupon Coffee');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Goslarite');
UPDATE commodities
SET commodity_de = 'Goslarit',
    commodity_es = 'Goslarita',
    commodity_fr = 'Goslarite',
    commodity_ru = 'Госларит'
WHERE LOWER(commodity) = LOWER('Goslarite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Grain');
UPDATE commodities
SET commodity_de = 'Getreide',
    commodity_es = 'Grano',
    commodity_fr = 'Céréales',
    commodity_ru = 'Зерно'
WHERE LOWER(commodity) = LOWER('Grain');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Grandidierite');
UPDATE commodities
SET commodity_de = 'Grandidierit',
    commodity_es = 'Grandidierita',
    commodity_fr = 'Grandidiérite',
    commodity_ru = 'Грандидьерит'
WHERE LOWER(commodity) = LOWER('Grandidierite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Grom Counter Intelligence');
UPDATE commodities
SET commodity_de = 'Grom-Spionageabwehr',
    commodity_es = 'Contrainteligencia de Grom',
    commodity_fr = 'Renseignements militaires de Grom',
    commodity_ru = 'Разведданные Грома'
WHERE LOWER(commodity) = LOWER('Grom Counter Intelligence');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Yuri Grom’s Military Supplies');
UPDATE commodities
SET commodity_de = 'Yuri Groms Militärausrüstung',
    commodity_es = 'Suministros militares de Yuri Grom',
    commodity_fr = 'Fournitures militaires de Yuri Grom',
    commodity_ru = 'Военные припасы Юрия Грома'
WHERE LOWER(commodity) = LOWER('Yuri Grom’s Military Supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Haematite');
UPDATE commodities
SET commodity_es = 'Hematita',
    commodity_fr = 'Haematite',
    commodity_ru = 'Гематит'
WHERE LOWER(commodity) = LOWER('Haematite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Hafnium 178');
UPDATE commodities
SET commodity_es = 'Hafnio 178',
    commodity_fr = 'Hafnium 178',
    commodity_ru = 'Гафний-178'
WHERE LOWER(commodity) = LOWER('Hafnium 178');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Haiden Black Brew');
UPDATE commodities
SET commodity_de = 'Haiden-Schwarzbier',
    commodity_es = 'Infusión negra de Haiden',
    commodity_fr = 'Tisane noire de Haiden',
    commodity_ru = 'Гайденовский черный напиток'
WHERE LOWER(commodity) = LOWER('Haiden Black Brew');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Harma Silver Sea Rum');
UPDATE commodities
SET commodity_de = 'Harma Silberseerum',
    commodity_es = 'Ron Harma Silver Sea',
    commodity_fr = 'Rhum marin Harma Silver',
    commodity_ru = 'Ром «Серебряное море Harma»'
WHERE LOWER(commodity) = LOWER('Harma Silver Sea Rum');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Havasupai Dream Catcher');
UPDATE commodities
SET commodity_de = 'Havasupai-Traumfänger',
    commodity_es = 'Atrapasueños de Havasupai',
    commodity_fr = 'Attrape-rêves Havasupai',
    commodity_ru = 'Хавасупайский ловец снов'
WHERE LOWER(commodity) = LOWER('Havasupai Dream Catcher');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('H.E. Suits');
UPDATE commodities
SET commodity_de = 'Schutzanzüge',
    commodity_es = 'Trajes de protección',
    commodity_fr = 'Combinaisons de protection',
    commodity_ru = 'Защитные костюмы'
WHERE LOWER(commodity) = LOWER('H.E. Suits');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Heatsink Interlink');
UPDATE commodities
SET commodity_de = 'Kühlkörperverbinder',
    commodity_es = 'Interconect. de eyector térmico',
    commodity_fr = 'Interconnexion dissipateur therm.',
    commodity_ru = 'Радиаторный соединитель'
WHERE LOWER(commodity) = LOWER('Heatsink Interlink');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Microbial Furnaces');
UPDATE commodities
SET commodity_de = 'Mikrobielle Öfen',
    commodity_es = 'Hornos microbianos',
    commodity_fr = 'Hauts fourneaux microbiens',
    commodity_ru = 'Микробные печи'
WHERE LOWER(commodity) = LOWER('Microbial Furnaces');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Helvetitj Pearls');
UPDATE commodities
SET commodity_de = 'Helvetitj-Perlen',
    commodity_es = 'Perlas de Helvetitj',
    commodity_fr = 'Perles de Helvetitj',
    commodity_ru = 'Жемчуг с Хельветитч'
WHERE LOWER(commodity) = LOWER('Helvetitj Pearls');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('HIP 10175 Bush Meat');
UPDATE commodities
SET commodity_de = 'Wildfleisch von HIP 10175',
    commodity_es = 'Carne herbácea de HIP 10175',
    commodity_fr = 'Viande de gibier de HIP 10175',
    commodity_ru = 'Мясо лесной дичи с HIP 10175'
WHERE LOWER(commodity) = LOWER('HIP 10175 Bush Meat');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('HIP Proto-Squid');
UPDATE commodities
SET commodity_de = 'HIP-Proto-Kalmar',
    commodity_es = 'Protocalamar de HIP 41181',
    commodity_fr = 'Proto-calamar de HIP',
    commodity_ru = 'Протокальмар с HIP'
WHERE LOWER(commodity) = LOWER('HIP Proto-Squid');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('HIP 118311 Swarm');
UPDATE commodities
SET commodity_es = 'SWARM de HIP 118311',
    commodity_fr = 'Essaim de HIP 118311',
    commodity_ru = 'Рой HIP 118311'
WHERE LOWER(commodity) = LOWER('HIP 118311 Swarm');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Hip Organophosphates');
UPDATE commodities
SET commodity_de = 'HIP-Organophosphate',
    commodity_es = 'Organofosfatos de HIP 80364',
    commodity_fr = 'Organophosphates de Hip',
    commodity_ru = 'Мощные органофосфаты'
WHERE LOWER(commodity) = LOWER('Hip Organophosphates');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('HN Shock Mount');
UPDATE commodities
SET commodity_de = 'HN-Dämpferaufhängung',
    commodity_es = 'Suspensión HN',
    commodity_fr = 'Protection antichocs HP',
    commodity_ru = 'Разрядная установка HN'
WHERE LOWER(commodity) = LOWER('HN Shock Mount');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Holva Duelling Blades');
UPDATE commodities
SET commodity_de = 'Holva-Duellschwerter',
    commodity_es = 'Espadas de duelo de Holva',
    commodity_fr = 'Lame de duel de Holva',
    commodity_ru = 'Хольванские дуэльные клинки'
WHERE LOWER(commodity) = LOWER('Holva Duelling Blades');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Honesty Pills');
UPDATE commodities
SET commodity_de = 'Ehrlichkeitspillen',
    commodity_es = 'Píldoras de la Honestidad',
    commodity_fr = 'Pilules d’honnêteté',
    commodity_ru = 'Пилюли честности'
WHERE LOWER(commodity) = LOWER('Honesty Pills');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Hostages');
UPDATE commodities
SET commodity_de = 'Geiseln',
    commodity_es = 'Rehenes',
    commodity_fr = 'Otages',
    commodity_ru = 'Заложники'
WHERE LOWER(commodity) = LOWER('Hostages');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('HR 7221 Wheat');
UPDATE commodities
SET commodity_de = 'HR 7221-Weizen',
    commodity_es = 'Trigo de HR 7221',
    commodity_fr = 'Blé de HR 7221',
    commodity_ru = 'Пшеница HR 7221'
WHERE LOWER(commodity) = LOWER('HR 7221 Wheat');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Hydrogen Fuel');
UPDATE commodities
SET commodity_de = 'Wasserstoff-Treibstoff',
    commodity_es = 'Combustible de hidrógeno',
    commodity_fr = 'Carburant à base d’hydrogène',
    commodity_ru = 'Водородное топливо'
WHERE LOWER(commodity) = LOWER('Hydrogen Fuel');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Hydrogen Peroxide');
UPDATE commodities
SET commodity_de = 'Wasserstoffperoxid',
    commodity_es = 'Peróxido de hidrógeno',
    commodity_fr = 'Peroxyde d’hydrogène',
    commodity_ru = 'Пероксид водорода'
WHERE LOWER(commodity) = LOWER('Hydrogen Peroxide');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Kumo Contraband Package');
UPDATE commodities
SET commodity_de = 'Kumo-Schmuggelwarenpaket',
    commodity_es = 'Paquete de contrabando de Kumo Crew',
    commodity_fr = 'Marchandises de contrebande de Kumo',
    commodity_ru = 'Контрабанда Kumo'
WHERE LOWER(commodity) = LOWER('Kumo Contraband Package');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Torval Political Prisoners');
UPDATE commodities
SET commodity_de = 'Torvals politische Gefangene',
    commodity_es = 'Prisioneros políticos de Torval',
    commodity_fr = 'Prisonniers politiques de Torval',
    commodity_ru = 'Политические заключенные Торвал'
WHERE LOWER(commodity) = LOWER('Torval Political Prisoners');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Imperial Slaves');
UPDATE commodities
SET commodity_de = 'Imperiale Sklaven',
    commodity_es = 'Esclavos imperiales',
    commodity_fr = 'Esclaves impériaux',
    commodity_ru = 'Имперские рабы'
WHERE LOWER(commodity) = LOWER('Imperial Slaves');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Indi Bourbon');
UPDATE commodities
SET commodity_es = 'Bourbon de Epsilon Indi',
    commodity_fr = 'Bourbon Indi',
    commodity_ru = 'Инди-бурбон'
WHERE LOWER(commodity) = LOWER('Indi Bourbon');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Indite');
UPDATE commodities
SET commodity_de = 'Indit',
    commodity_es = 'Indita',
    commodity_fr = 'Indite',
    commodity_ru = 'Индит'
WHERE LOWER(commodity) = LOWER('Indite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Indium');
UPDATE commodities
SET commodity_es = 'Indio',
    commodity_fr = 'Indium',
    commodity_ru = 'Индий'
WHERE LOWER(commodity) = LOWER('Indium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Insulating Membrane');
UPDATE commodities
SET commodity_de = 'Isoliermembran',
    commodity_es = 'Membrana aislante',
    commodity_fr = 'Membrane isolante',
    commodity_ru = 'Изолирующая мембрана'
WHERE LOWER(commodity) = LOWER('Insulating Membrane');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ion Distributor');
UPDATE commodities
SET commodity_de = 'Ionenverteiler',
    commodity_es = 'Distribuidor de iones',
    commodity_fr = 'Distributeurs d’ions',
    commodity_ru = 'Ионный распределитель'
WHERE LOWER(commodity) = LOWER('Ion Distributor');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Jadeite');
UPDATE commodities
SET commodity_de = 'Jadeit',
    commodity_es = 'Jadeíta',
    commodity_fr = 'Jadéite',
    commodity_ru = 'Жадеит'
WHERE LOWER(commodity) = LOWER('Jadeite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Jaques Quinentian Still');
UPDATE commodities
SET commodity_de = 'Jaques Quinent-Destille',
    commodity_es = 'Alambique Jaques Quinentian',
    commodity_fr = 'Brûlerie quinentienne de Jaques',
    commodity_ru = 'Дистиллятор Жака Квиненциана'
WHERE LOWER(commodity) = LOWER('Jaques Quinentian Still');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Jaradharre Puzzle Box');
UPDATE commodities
SET commodity_de = 'Jaradharre Puzzle-Box',
    commodity_es = 'Rompecabezas de Jaradharre',
    commodity_fr = 'Boîte de jeux de Jaradharre',
    commodity_ru = 'Джарадхаррская головоломка'
WHERE LOWER(commodity) = LOWER('Jaradharre Puzzle Box');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Jaroua Rice');
UPDATE commodities
SET commodity_de = 'Jaroua-Reis',
    commodity_es = 'Arroz de Jaroua',
    commodity_fr = 'Riz jarouan',
    commodity_ru = 'Жаруйский рис'
WHERE LOWER(commodity) = LOWER('Jaroua Rice');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Jotun Mookah');
UPDATE commodities
SET commodity_es = 'Mookah de Jotun',
    commodity_fr = 'Mookah de Jotun',
    commodity_ru = 'Йотунская моока'
WHERE LOWER(commodity) = LOWER('Jotun Mookah');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Kachirigin Filter Leeches');
UPDATE commodities
SET commodity_de = 'Kachirigin-Filter-Egel',
    commodity_es = 'Sanguijuelas filtradoras de Kachigirin',
    commodity_fr = 'Sangsues de Kachirigin',
    commodity_ru = 'Очищающие пиявки с Качиригина'
WHERE LOWER(commodity) = LOWER('Kachirigin Filter Leeches');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Kaine Aid Supplies');
UPDATE commodities
SET commodity_ru = 'Гуманитарная помощь Кейн'
WHERE LOWER(commodity) = LOWER('Kaine Aid Supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Kaine Lobbying Materials');
UPDATE commodities
SET commodity_ru = 'Лоббистские материалы Кейн'
WHERE LOWER(commodity) = LOWER('Kaine Lobbying Materials');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Kaine Misinformation');
UPDATE commodities
SET commodity_ru = 'Дезинформация Кейн'
WHERE LOWER(commodity) = LOWER('Kaine Misinformation');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Kamitra Cigars');
UPDATE commodities
SET commodity_de = 'Kamitra-Zigarren',
    commodity_es = 'Puros de Kamitra',
    commodity_fr = 'Cigares de Kamitra',
    commodity_ru = 'Сигара Kamitra'
WHERE LOWER(commodity) = LOWER('Kamitra Cigars');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Kamorin Historic Weapons');
UPDATE commodities
SET commodity_de = 'Histor. Kamorin-Waffen',
    commodity_es = 'Armas históricas de Kamorin',
    commodity_fr = 'Armes historiques de Kamorin',
    commodity_ru = 'Каморинское историческое оружие'
WHERE LOWER(commodity) = LOWER('Kamorin Historic Weapons');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Karetii Couture');
UPDATE commodities
SET commodity_es = 'Alta costura de Karetii',
    commodity_fr = 'Haute couture karetii',
    commodity_ru = 'Модные вещи от Karetii'
WHERE LOWER(commodity) = LOWER('Karetii Couture');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Karsuki Locusts');
UPDATE commodities
SET commodity_de = 'Karsuki-Heuschrecke',
    commodity_es = 'Langostas de Karsuki Ti',
    commodity_fr = 'Sauterelles de Karsuki',
    commodity_ru = 'Саранча с Карсуки'
WHERE LOWER(commodity) = LOWER('Karsuki Locusts');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Kinago Violins');
UPDATE commodities
SET commodity_de = 'Kinago-Geigen',
    commodity_es = 'Violines de Kinago',
    commodity_fr = 'Violons de Kinago',
    commodity_ru = 'Скрипки Kinago'
WHERE LOWER(commodity) = LOWER('Kinago Violins');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Kongga Ale');
UPDATE commodities
SET commodity_es = 'Cerveza de Kongga',
    commodity_fr = 'Bière de Kongga',
    commodity_ru = 'Эль с Кунгги'
WHERE LOWER(commodity) = LOWER('Kongga Ale');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Koro Kung Pellets');
UPDATE commodities
SET commodity_de = 'Korro-Kung-Pellets',
    commodity_es = 'Gránulos de Korro Kung',
    commodity_fr = 'Boulettes Korro Kung',
    commodity_ru = 'Корокунгские катышки'
WHERE LOWER(commodity) = LOWER('Koro Kung Pellets');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Landmines');
UPDATE commodities
SET commodity_de = 'Landminen',
    commodity_es = 'Minas terrestres',
    commodity_fr = 'Mines terrestres',
    commodity_ru = 'Мины'
WHERE LOWER(commodity) = LOWER('Landmines');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lanthanum');
UPDATE commodities
SET commodity_de = 'Lanthan',
    commodity_es = 'Lantano',
    commodity_fr = 'Lanthane',
    commodity_ru = 'Лантан'
WHERE LOWER(commodity) = LOWER('Lanthanum');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Large Survey Data Cache');
UPDATE commodities
SET commodity_de = 'Großer Erkundungsdatenspeicher',
    commodity_es = 'Memorias de reconocimiento grandes',
    commodity_fr = 'Important lot de données d’explorations',
    commodity_ru = 'Большой пакет с данными исследования'
WHERE LOWER(commodity) = LOWER('Large Survey Data Cache');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lavian Brandy');
UPDATE commodities
SET commodity_de = 'Lave-Brandy',
    commodity_es = 'Brandy laviano',
    commodity_fr = 'Brandy lavien',
    commodity_ru = 'Лавианский бренди'
WHERE LOWER(commodity) = LOWER('Lavian Brandy');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lavigny Corruption Reports');
UPDATE commodities
SET commodity_de = 'Lavignys Korruptionsberichte',
    commodity_es = 'Informes de corrupción de Lavigny',
    commodity_fr = 'Rapports de corruption d’Arissa',
    commodity_ru = 'Отчеты Лавиньи о коррупции'
WHERE LOWER(commodity) = LOWER('Lavigny Corruption Reports');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lavigny Field Supplies');
UPDATE commodities
SET commodity_de = 'Lavignys Feldversorgung',
    commodity_es = 'Suministros de campaña de Lavigny',
    commodity_fr = 'Ravitaillement militaire d’Arissa',
    commodity_ru = 'Полевые припасы Лавиньи'
WHERE LOWER(commodity) = LOWER('Lavigny Field Supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lavigny Garrison Supplies');
UPDATE commodities
SET commodity_de = 'Lavignys Garnisonsversorgung',
    commodity_es = 'Suministros de guarnición de Lavigny',
    commodity_fr = 'Ravitaillement de garnison d’Arissa',
    commodity_ru = 'Гарнизонные припасы Лавиньи'
WHERE LOWER(commodity) = LOWER('Lavigny Garrison Supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lavigny Strategic Reports');
UPDATE commodities
SET commodity_ru = 'Оперативный доклад Лавиньи'
WHERE LOWER(commodity) = LOWER('Lavigny Strategic Reports');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Leather');
UPDATE commodities
SET commodity_de = 'Leder',
    commodity_es = 'Cuero',
    commodity_fr = 'Cuir',
    commodity_ru = 'Кожа'
WHERE LOWER(commodity) = LOWER('Leather');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Leestian Evil Juice');
UPDATE commodities
SET commodity_de = 'Leesti-Teufelssaft',
    commodity_es = 'Zumo diabólico leestiano',
    commodity_fr = 'Leestian Evil Juice',
    commodity_ru = 'Леестийский сок зла'
WHERE LOWER(commodity) = LOWER('Leestian Evil Juice');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lepidolite');
UPDATE commodities
SET commodity_de = 'Lepidolith',
    commodity_es = 'Lepidolita',
    commodity_fr = 'Lépidolite',
    commodity_ru = 'Лепидолит'
WHERE LOWER(commodity) = LOWER('Lepidolite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Void Extract Coffee');
UPDATE commodities
SET commodity_de = 'Weltraumvakuum-Kaffee',
    commodity_es = 'Extracto de café al vacío',
    commodity_fr = 'Café du néant',
    commodity_ru = 'Кофе Экстракт пустоты'
WHERE LOWER(commodity) = LOWER('Void Extract Coffee');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Liberal Propaganda');
UPDATE commodities
SET commodity_de = 'Liberale Propaganda',
    commodity_es = 'Propaganda liberal',
    commodity_fr = 'Programmes libéraux',
    commodity_ru = 'Либеральная пропаганда'
WHERE LOWER(commodity) = LOWER('Liberal Propaganda');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Liquid oxygen');
UPDATE commodities
SET commodity_de = 'Flüssiger Sauerstoff',
    commodity_es = 'Oxígeno líquido',
    commodity_fr = 'Oxygène liquide',
    commodity_ru = 'Жидкий кислород'
WHERE LOWER(commodity) = LOWER('Liquid oxygen');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Liquor');
UPDATE commodities
SET commodity_de = 'Spirituosen',
    commodity_es = 'Licores',
    commodity_fr = 'Spiritueux',
    commodity_ru = 'Спиртное'
WHERE LOWER(commodity) = LOWER('Liquor');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lithium');
UPDATE commodities
SET commodity_es = 'Litio',
    commodity_fr = 'Lithium',
    commodity_ru = 'Литий'
WHERE LOWER(commodity) = LOWER('Lithium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lithium Hydroxide');
UPDATE commodities
SET commodity_de = 'Lithiumhydroxid',
    commodity_es = 'Hidróxido de litio',
    commodity_fr = 'Hydroxyde de lithium',
    commodity_ru = 'Гидроксид лития'
WHERE LOWER(commodity) = LOWER('Lithium Hydroxide');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Live Hecate Sea Worms');
UPDATE commodities
SET commodity_de = 'Live-Hecate-Seewürmer',
    commodity_es = 'Gusanos marinos de Hecate',
    commodity_fr = 'Vers marins vivants d’Hecate',
    commodity_ru = 'Живые морские черви с Гекаты'
WHERE LOWER(commodity) = LOWER('Live Hecate Sea Worms');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Marked Military Arms');
UPDATE commodities
SET commodity_de = 'Markierte Militärwaffen',
    commodity_es = 'Armamento militar marcado',
    commodity_fr = 'Armes militaires marquées',
    commodity_ru = 'Помеченное оружие'
WHERE LOWER(commodity) = LOWER('Marked Military Arms');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Low Temperature Diamonds');
UPDATE commodities
SET commodity_de = 'Niedertemperaturdiamanten',
    commodity_es = 'Diamante de baja temperatura',
    commodity_fr = 'Diamants basse température',
    commodity_ru = 'Низкотемпературные алмазы'
WHERE LOWER(commodity) = LOWER('Low Temperature Diamonds');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('LTT Hyper Sweet');
UPDATE commodities
SET commodity_de = 'LTT-Hypersüße',
    commodity_es = 'Hiperdulce LTT',
    commodity_fr = 'Hyper sucre LTT',
    commodity_ru = 'Гиперсладости LTT'
WHERE LOWER(commodity) = LOWER('LTT Hyper Sweet');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lyrae Weed');
UPDATE commodities
SET commodity_es = 'Mala hierba de Lyrae',
    commodity_fr = 'Herbe Lyrane',
    commodity_ru = 'Трава лираи'
WHERE LOWER(commodity) = LOWER('Lyrae Weed');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mollusc Membrane');
UPDATE commodities
SET commodity_de = 'Molluskenmembran',
    commodity_es = 'Membrana de molusco',
    commodity_fr = 'Membrane de mollusque',
    commodity_ru = 'Мембрана моллюска'
WHERE LOWER(commodity) = LOWER('Mollusc Membrane');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mollusc Mycelium');
UPDATE commodities
SET commodity_de = 'Molluskenmyzel',
    commodity_es = 'Mycelium de molusco',
    commodity_fr = 'Mycélium de mollusque',
    commodity_ru = 'Мицелий моллюска'
WHERE LOWER(commodity) = LOWER('Mollusc Mycelium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mollusc Spores');
UPDATE commodities
SET commodity_de = 'Molluskensporen',
    commodity_es = 'Esporas de molusco',
    commodity_fr = 'Spores de mollusque',
    commodity_ru = 'Споры моллюска'
WHERE LOWER(commodity) = LOWER('Mollusc Spores');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mollusc Fluid');
UPDATE commodities
SET commodity_de = 'Molluskenflüssigkeit',
    commodity_es = 'Fluido de molusco',
    commodity_fr = 'Fluides de mollusque',
    commodity_ru = 'Жидкость моллюска'
WHERE LOWER(commodity) = LOWER('Mollusc Fluid');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mollusc Brain Tissue');
UPDATE commodities
SET commodity_de = 'Mollusken-Hirnmasse',
    commodity_es = 'Tejido de cerebro de molusco',
    commodity_fr = 'Matière cérébrale de mollusque',
    commodity_ru = 'Мозговое вещество моллюска'
WHERE LOWER(commodity) = LOWER('Mollusc Brain Tissue');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mollusc Soft Tissue');
UPDATE commodities
SET commodity_de = 'Mollusken-Weichgewebe',
    commodity_es = 'Tejido suave de molusco',
    commodity_fr = 'Tissu mou de mollusque',
    commodity_ru = 'Мягкие ткани моллюска'
WHERE LOWER(commodity) = LOWER('Mollusc Soft Tissue');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Magnetic Emitter Coil');
UPDATE commodities
SET commodity_de = 'Magnetische Emitterspule',
    commodity_es = 'Bobina de emisión magnética',
    commodity_fr = 'Bobine d’émission magnétique',
    commodity_ru = 'Спираль магнитного излучателя'
WHERE LOWER(commodity) = LOWER('Magnetic Emitter Coil');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Marine Equipment');
UPDATE commodities
SET commodity_de = 'Maritimausstattung',
    commodity_es = 'Equipamiento marino',
    commodity_fr = 'Équipement aquamarin',
    commodity_ru = 'Морское оборудование'
WHERE LOWER(commodity) = LOWER('Marine Equipment');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Marked Slaves');
UPDATE commodities
SET commodity_de = 'Markierte Sklaven',
    commodity_es = 'Esclavos marcados',
    commodity_fr = 'Esclaves marqués',
    commodity_ru = 'Помеченные рабы'
WHERE LOWER(commodity) = LOWER('Marked Slaves');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Master Chefs');
UPDATE commodities
SET commodity_es = 'Maestros de cocina',
    commodity_fr = 'Spartan-chefs',
    commodity_ru = 'Мастер-шефы'
WHERE LOWER(commodity) = LOWER('Master Chefs');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mechucos High Tea');
UPDATE commodities
SET commodity_de = 'Mechucos-High-Tee',
    commodity_es = 'Alto té de Mechucos',
    commodity_fr = 'Haut thé de Mechucos',
    commodity_ru = 'Мечукосский кайфочай'
WHERE LOWER(commodity) = LOWER('Mechucos High Tea');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Medb Starlube');
UPDATE commodities
SET commodity_de = 'Medb-Sternschmiere',
    commodity_es = 'Lubricante de Medb',
    commodity_fr = 'Lubrifiant stellaire Medb',
    commodity_ru = 'Звездная смазка с Медба'
WHERE LOWER(commodity) = LOWER('Medb Starlube');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Medical Diagnostic Equipment');
UPDATE commodities
SET commodity_de = 'Medizinische Diagnostikausrüstung',
    commodity_es = 'Equipo de diagnóstico médico',
    commodity_fr = 'Équipement de diagnostic médical',
    commodity_ru = 'Диагностическое медоборудование'
WHERE LOWER(commodity) = LOWER('Medical Diagnostic Equipment');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Meta-Alloys');
UPDATE commodities
SET commodity_de = 'Meta-Legierungen',
    commodity_es = 'Metaaleaciones',
    commodity_fr = 'Méta-alliages',
    commodity_ru = 'Метасплавы'
WHERE LOWER(commodity) = LOWER('Meta-Alloys');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Methane Clathrate');
UPDATE commodities
SET commodity_de = 'Methanklathrat',
    commodity_es = 'Hidrato de metano',
    commodity_fr = 'Hydrate de méthane',
    commodity_ru = 'Клатрат метана'
WHERE LOWER(commodity) = LOWER('Methane Clathrate');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Methanol Monohydrate Crystals');
UPDATE commodities
SET commodity_de = 'Methanol-Monohydrat-Kristalle',
    commodity_es = 'Cristales de monohidrato de metanol',
    commodity_fr = 'Cristaux de méthanol monohydraté',
    commodity_ru = 'Кристаллы моногидрата метанола'
WHERE LOWER(commodity) = LOWER('Methanol Monohydrate Crystals');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Micro Controllers');
UPDATE commodities
SET commodity_de = 'Mikrocontroller',
    commodity_es = 'Microcontroladores',
    commodity_fr = 'Microcontrôleurs',
    commodity_ru = 'Микроконтроллеры'
WHERE LOWER(commodity) = LOWER('Micro Controllers');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Military Grade Fabrics');
UPDATE commodities
SET commodity_de = 'Militärische Stoffe',
    commodity_es = 'Tejido de categoría militar',
    commodity_fr = 'Tissus militaires',
    commodity_ru = 'Ткани военного класса'
WHERE LOWER(commodity) = LOWER('Military Grade Fabrics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Military Intelligence');
UPDATE commodities
SET commodity_de = 'Militärische Geheimdokumente',
    commodity_es = 'Inteligencia militar',
    commodity_fr = 'Renseignements militaires',
    commodity_ru = 'Разведданные'
WHERE LOWER(commodity) = LOWER('Military Intelligence');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mineral Extractors');
UPDATE commodities
SET commodity_de = 'Mineralextraktoren',
    commodity_es = 'Extractores de minerales',
    commodity_fr = 'Extracteurs de minerai',
    commodity_ru = 'Экстракторы минералов'
WHERE LOWER(commodity) = LOWER('Mineral Extractors');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mineral Oil');
UPDATE commodities
SET commodity_de = 'Mineralöl',
    commodity_es = 'Aceite mineral',
    commodity_fr = 'Huile(s) minérale(s)',
    commodity_ru = 'Нефтепродукты'
WHERE LOWER(commodity) = LOWER('Mineral Oil');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Modular Terminals');
UPDATE commodities
SET commodity_de = 'Modulterminals',
    commodity_es = 'Terminales modulares',
    commodity_fr = 'Terminaux modulaires',
    commodity_ru = 'Модульные терминалы'
WHERE LOWER(commodity) = LOWER('Modular Terminals');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Moissanite');
UPDATE commodities
SET commodity_de = 'Moissanit',
    commodity_es = 'Moissanita',
    commodity_fr = 'Carbure de silicium',
    commodity_ru = 'Муассанит'
WHERE LOWER(commodity) = LOWER('Moissanite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mokojing Beast Feast');
UPDATE commodities
SET commodity_de = 'Mokojing Monstermüsli',
    commodity_es = 'Banquete Bestial de Mokojin',
    commodity_fr = 'Céréanimales de Mokojing',
    commodity_ru = 'Мокоджингское зверское яство'
WHERE LOWER(commodity) = LOWER('Mokojing Beast Feast');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Momus Bog Spaniel');
UPDATE commodities
SET commodity_de = 'Momus-Sumpfspaniel',
    commodity_es = 'Spaniel de pantano de Momus Reach',
    commodity_fr = 'Épagneul fangeux de Momus',
    commodity_ru = 'Момусовский болотный спаниель'
WHERE LOWER(commodity) = LOWER('Momus Bog Spaniel');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Monazite');
UPDATE commodities
SET commodity_de = 'Monazit',
    commodity_es = 'Monacita',
    commodity_fr = 'Monazite',
    commodity_ru = 'Монацит'
WHERE LOWER(commodity) = LOWER('Monazite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Motrona Experience Jelly');
UPDATE commodities
SET commodity_de = 'Motrona-Erfahrungsgel',
    commodity_es = 'Gelatina de la Experiencia de Dea Motrona',
    commodity_fr = 'Gelée d’expérience de Motrona',
    commodity_ru = 'Мотронское желе'
WHERE LOWER(commodity) = LOWER('Motrona Experience Jelly');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mukusubii Chitin-os');
UPDATE commodities
SET commodity_es = 'Quitiaros de Mukusubii',
    commodity_fr = 'Os chitineux de Mukusubii',
    commodity_ru = 'Мукусубские хитинос'
WHERE LOWER(commodity) = LOWER('Mukusubii Chitin-os');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mulachi Giant Fungus');
UPDATE commodities
SET commodity_de = 'Mulachi-Riesenpilz',
    commodity_es = 'Hongo gigante de Mulachi',
    commodity_fr = 'Champignons géants de Mulachi',
    commodity_ru = 'Гигатские грибы с Мулачи'
WHERE LOWER(commodity) = LOWER('Mulachi Giant Fungus');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Musgravite');
UPDATE commodities
SET commodity_de = 'Musgravit',
    commodity_es = 'Musgravita',
    commodity_fr = 'Musgravite',
    commodity_ru = 'Мусгравит'
WHERE LOWER(commodity) = LOWER('Musgravite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Muon Imager');
UPDATE commodities
SET commodity_de = 'Myon-Bildgeber',
    commodity_es = 'Escáner muónico',
    commodity_fr = 'Dispositif d’imagerie muonique',
    commodity_ru = 'Мюонное видеоустройство'
WHERE LOWER(commodity) = LOWER('Muon Imager');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Mysterious Idol');
UPDATE commodities
SET commodity_de = 'Mysteriöses Idol',
    commodity_es = 'Ídolo misterioso',
    commodity_fr = 'Idole mystérieuse',
    commodity_ru = 'Таинственный идол'
WHERE LOWER(commodity) = LOWER('Mysterious Idol');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Nanobreakers');
UPDATE commodities
SET commodity_de = 'Nanozertrümmerer',
    commodity_es = 'Nanorrompedores',
    commodity_fr = 'Nanodestructeurs',
    commodity_ru = 'Нанопрерыватели'
WHERE LOWER(commodity) = LOWER('Nanobreakers');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Nanomedicines');
UPDATE commodities
SET commodity_de = 'Nanomedikamente',
    commodity_es = 'Nanomedicinas',
    commodity_fr = 'Nanomédicaments',
    commodity_ru = 'Нанолекарства'
WHERE LOWER(commodity) = LOWER('Nanomedicines');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Natural Fabrics');
UPDATE commodities
SET commodity_de = 'Naturfasern',
    commodity_es = 'Tejidos naturales',
    commodity_fr = 'Fibre(s) textile(s) naturelle(s)',
    commodity_ru = 'Натуральная ткань'
WHERE LOWER(commodity) = LOWER('Natural Fabrics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Neofabric Insulation');
UPDATE commodities
SET commodity_de = 'Neogewebe-Isolierung',
    commodity_es = 'Neotejido aislante',
    commodity_fr = 'Isolant en néotextile',
    commodity_ru = 'Высокотехнологичная изоляция'
WHERE LOWER(commodity) = LOWER('Neofabric Insulation');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Neritus Berries');
UPDATE commodities
SET commodity_de = 'Neritus-Beeren',
    commodity_es = 'Bayas de Neritus',
    commodity_fr = 'Baies de Neritus',
    commodity_ru = 'Нерутские ягоды'
WHERE LOWER(commodity) = LOWER('Neritus Berries');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Nerve Agents');
UPDATE commodities
SET commodity_de = 'Nervengas',
    commodity_es = 'Agentes nerviosos',
    commodity_fr = 'Agents neurotoxiques',
    commodity_ru = 'Агенты нервно-паралитического действия'
WHERE LOWER(commodity) = LOWER('Nerve Agents');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ngadandari Fire Opals');
UPDATE commodities
SET commodity_de = 'Ngadandari-Feueropale',
    commodity_es = 'Ópalos ígneos de Ngandari',
    commodity_fr = 'Opales de feu de Ngadandari',
    commodity_ru = 'Нгадандарийские огненные опалы'
WHERE LOWER(commodity) = LOWER('Ngadandari Fire Opals');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Nguna Modern Antiques');
UPDATE commodities
SET commodity_de = 'Nguna Moderne Antiquitäten',
    commodity_es = 'Antigüedades modernas de Nguna',
    commodity_fr = 'Antiquités modernes de Nguna',
    commodity_ru = 'Современные древности с Нгуны'
WHERE LOWER(commodity) = LOWER('Nguna Modern Antiques');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Njangari Saddles');
UPDATE commodities
SET commodity_de = 'Njangari-Sättel',
    commodity_es = 'Sillas de montar de Njangari',
    commodity_fr = 'Selles de Njangari',
    commodity_ru = 'Седла Njangari'
WHERE LOWER(commodity) = LOWER('Njangari Saddles');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Non Euclidian Exotanks');
UPDATE commodities
SET commodity_de = 'Nichteuklidische Exotanks',
    commodity_es = 'Exotanques no euclídeos',
    commodity_fr = 'Vivariums non euclidiens',
    commodity_ru = 'Неевклидовы экзобаки'
WHERE LOWER(commodity) = LOWER('Non Euclidian Exotanks');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Non-Lethal Weapons');
UPDATE commodities
SET commodity_de = 'Nichttödliche Waffen',
    commodity_es = 'Armas no letales',
    commodity_fr = 'Armes incapacitantes',
    commodity_ru = 'Нелетальное оружие'
WHERE LOWER(commodity) = LOWER('Non-Lethal Weapons');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Occupied Escape Pod');
UPDATE commodities
SET commodity_de = 'Besetzte Rettungskapsel',
    commodity_es = 'Cápsula de escape ocupada',
    commodity_fr = 'Nacelle d’évacuation occupée',
    commodity_ru = 'Спасательная капсула с пассажиром'
WHERE LOWER(commodity) = LOWER('Occupied Escape Pod');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ochoeng Chillies');
UPDATE commodities
SET commodity_de = 'Ochoeng-Chili',
    commodity_es = 'Chiles de Ochoeng',
    commodity_fr = 'Piments d’Ochoeng',
    commodity_ru = 'Перчик чили с Очоенга'
WHERE LOWER(commodity) = LOWER('Ochoeng Chillies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Onionhead');
UPDATE commodities
SET commodity_es = 'Cebollazo',
    commodity_fr = 'Tête d’oignon',
    commodity_ru = 'Луковая головка'
WHERE LOWER(commodity) = LOWER('Onionhead');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Onionhead Alpha Strain');
UPDATE commodities
SET commodity_de = 'Onionhead (Alpha)',
    commodity_es = 'Cepa alfa de cebollazo',
    commodity_fr = 'Variété alpha de tête d’oignon',
    commodity_ru = 'Луковая головка, сорт альфа'
WHERE LOWER(commodity) = LOWER('Onionhead Alpha Strain');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Onionhead Beta Strain');
UPDATE commodities
SET commodity_de = 'Onionhead (Beta)',
    commodity_es = 'Cepa beta de cebollazo',
    commodity_fr = 'Variété bêta de tête d’oignon',
    commodity_ru = 'Луковая головка, сорт бета'
WHERE LOWER(commodity) = LOWER('Onionhead Beta Strain');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Onionhead Gamma Strain');
UPDATE commodities
SET commodity_de = 'Onionhead Gamma -Stamm',
    commodity_es = 'Souche gamma oignon',
    commodity_fr = 'Cepa gamma de cabeza de cebolla',
    commodity_ru = 'Лукхеда гамма -штамм'
WHERE LOWER(commodity) = LOWER('Onionhead Gamma Strain');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Onionhead Derivatives');
UPDATE commodities
SET commodity_de = 'Onionhead-Derivantien',
    commodity_es = 'Derivados de Cebollazo',
    commodity_fr = 'Produits dérivés à base de tête d’oignon',
    commodity_ru = 'Компоненты луковых головок'
WHERE LOWER(commodity) = LOWER('Onionhead Derivatives');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Onionhead Samples');
UPDATE commodities
SET commodity_de = 'Onionhead-Proben',
    commodity_es = 'Muestras de Cebollazo',
    commodity_fr = 'Échantillons de tête d’oignon',
    commodity_ru = 'Образцы луковых головок'
WHERE LOWER(commodity) = LOWER('Onionhead Samples');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Void Opal');
UPDATE commodities
SET commodity_de = 'Leerenopal',
    commodity_es = 'Ópalo de vacío',
    commodity_fr = 'Opale du vide',
    commodity_ru = 'Опал бездны'
WHERE LOWER(commodity) = LOWER('Void Opal');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ophiuch Exino Artefacts');
UPDATE commodities
SET commodity_de = 'Ophiuch-Exino-Artefakte',
    commodity_es = 'Artefactos de los Ophiuch Exino',
    commodity_fr = 'Artefacts Ophiuch Exino',
    commodity_ru = 'Артефакты офиух экзино'
WHERE LOWER(commodity) = LOWER('Ophiuch Exino Artefacts');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Orrerian Vicious Brew');
UPDATE commodities
SET commodity_de = 'Orrerianisches Immerwach',
    commodity_es = 'Fermento vicioso orreriano',
    commodity_fr = 'Vicieuse d’Orrere',
    commodity_ru = 'Оррерское жуткое пойло'
WHERE LOWER(commodity) = LOWER('Orrerian Vicious Brew');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Osmium');
UPDATE commodities
SET commodity_es = 'Osmio',
    commodity_fr = 'Osmium',
    commodity_ru = 'Осмий'
WHERE LOWER(commodity) = LOWER('Osmium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Out Of Date Goods');
UPDATE commodities
SET commodity_de = 'Abgelaufene Waren',
    commodity_es = 'Bienes caducados',
    commodity_fr = 'Marchandises périmées',
    commodity_ru = 'Устаревшие товары'
WHERE LOWER(commodity) = LOWER('Out Of Date Goods');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Anomaly Particles');
UPDATE commodities
SET commodity_fr = 'Particules d’anomalie',
    commodity_ru = 'Аномальные частицы'
WHERE LOWER(commodity) = LOWER('Anomaly Particles');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Painite');
UPDATE commodities
SET commodity_de = 'Painit',
    commodity_es = 'Painita',
    commodity_fr = 'Painite',
    commodity_ru = 'Пейнит'
WHERE LOWER(commodity) = LOWER('Painite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Palladium');
UPDATE commodities
SET commodity_es = 'Paladio',
    commodity_fr = 'Palladium',
    commodity_ru = 'Палладий'
WHERE LOWER(commodity) = LOWER('Palladium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pantaa Prayer Sticks');
UPDATE commodities
SET commodity_de = 'Pantaa-Weihrauchstöcke',
    commodity_es = 'Palos ceremoniales de los Pantaa',
    commodity_fr = 'Bâtonnets d’encens de Panta',
    commodity_ru = 'Пантаские молитвенные палочки'
WHERE LOWER(commodity) = LOWER('Pantaa Prayer Sticks');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Patreus Field Supplies');
UPDATE commodities
SET commodity_de = 'Patreus’ Feldversorgung',
    commodity_es = 'Suministros de campaña de Patreus',
    commodity_fr = 'Ravitaillement militaire de Patreus',
    commodity_ru = 'Полевые припасы Патреуса'
WHERE LOWER(commodity) = LOWER('Patreus Field Supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Patreus Garrison Supplies');
UPDATE commodities
SET commodity_de = 'Patreus’ Garnisonsversorgung',
    commodity_es = 'Suministros de guarnición de Patreus',
    commodity_fr = 'Ravitaillement de garnison de Patreus',
    commodity_ru = 'Гарнизонные припасы Патреуса'
WHERE LOWER(commodity) = LOWER('Patreus Garrison Supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pavonis Ear Grubs');
UPDATE commodities
SET commodity_de = 'Pavonis-Ohrenlarven',
    commodity_es = 'Larvorejas de Pavonis',
    commodity_fr = 'Larves Oreilles de Pavonis',
    commodity_ru = 'Павлинские ухочерви'
WHERE LOWER(commodity) = LOWER('Pavonis Ear Grubs');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Performance Enhancers');
UPDATE commodities
SET commodity_de = 'Leistungssteigerer',
    commodity_es = 'Potenciadores de rendimiento',
    commodity_fr = 'Produit(s) dopant(s)',
    commodity_ru = 'Стимуляторы'
WHERE LOWER(commodity) = LOWER('Performance Enhancers');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Personal Effects');
UPDATE commodities
SET commodity_de = 'Persönliches',
    commodity_es = 'Efectos personales',
    commodity_fr = 'Effets personnels',
    commodity_ru = 'Личные вещи'
WHERE LOWER(commodity) = LOWER('Personal Effects');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Personal Gifts');
UPDATE commodities
SET commodity_de = 'Persönliche Geschenke',
    commodity_es = 'Regalos personales',
    commodity_fr = 'Cadeaux personnels',
    commodity_ru = 'Персональные подарки'
WHERE LOWER(commodity) = LOWER('Personal Gifts');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Personal Weapons');
UPDATE commodities
SET commodity_de = 'Persönliche Waffen',
    commodity_es = 'Armas personales',
    commodity_fr = 'Armes de poing',
    commodity_ru = 'Личное оружие'
WHERE LOWER(commodity) = LOWER('Personal Weapons');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pesticides');
UPDATE commodities
SET commodity_de = 'Pestizide',
    commodity_es = 'Pesticidas',
    commodity_fr = 'Pesticide(s)',
    commodity_ru = 'Пестициды'
WHERE LOWER(commodity) = LOWER('Pesticides');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Platinum');
UPDATE commodities
SET commodity_de = 'Platin',
    commodity_es = 'Platino',
    commodity_fr = 'Platine',
    commodity_ru = 'Платина'
WHERE LOWER(commodity) = LOWER('Platinum');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Platinum Alloy');
UPDATE commodities
SET commodity_de = 'Platinlegierung',
    commodity_es = 'Aleación de platino',
    commodity_fr = 'Alliage de platine',
    commodity_ru = 'Платиновый сплав'
WHERE LOWER(commodity) = LOWER('Platinum Alloy');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Political Prisoners');
UPDATE commodities
SET commodity_de = 'Politische Gefangene',
    commodity_es = 'Prisioneros políticos',
    commodity_fr = 'Prisonniers politiques',
    commodity_ru = 'Политзаключенные'
WHERE LOWER(commodity) = LOWER('Political Prisoners');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Polymers');
UPDATE commodities
SET commodity_de = 'Polymere',
    commodity_es = 'Polímeros',
    commodity_fr = 'Polymère(s)',
    commodity_ru = 'Полимеры'
WHERE LOWER(commodity) = LOWER('Polymers');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Power Converter');
UPDATE commodities
SET commodity_de = 'Energiekonverter',
    commodity_es = 'Convertidor de energía',
    commodity_fr = 'Convertisseur d’énergie',
    commodity_ru = 'Преобразователь энергии'
WHERE LOWER(commodity) = LOWER('Power Converter');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Power Generators');
UPDATE commodities
SET commodity_de = 'Stromerzeuger',
    commodity_es = 'Generadores de energía',
    commodity_fr = 'Générateurs',
    commodity_ru = 'Электрогенераторы'
WHERE LOWER(commodity) = LOWER('Power Generators');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Energy Grid Assembly');
UPDATE commodities
SET commodity_de = 'Energienetz-Gruppe',
    commodity_es = 'Red de energía',
    commodity_fr = 'Système de réseau d’alimentation',
    commodity_ru = 'Электросеть в сборе'
WHERE LOWER(commodity) = LOWER('Energy Grid Assembly');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Power Transfer Bus');
UPDATE commodities
SET commodity_de = 'Energietransferbus',
    commodity_es = 'Conductos de transf. de energía',
    commodity_fr = 'Conduits de transfert d’énergie',
    commodity_ru = 'Энергообменная шина'
WHERE LOWER(commodity) = LOWER('Power Transfer Bus');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Praseodymium');
UPDATE commodities
SET commodity_de = 'Praseodym',
    commodity_es = 'Praseodimio',
    commodity_fr = 'Praséodyme',
    commodity_ru = 'Празеодим'
WHERE LOWER(commodity) = LOWER('Praseodymium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Precious Gems');
UPDATE commodities
SET commodity_de = 'Edelsteine',
    commodity_es = 'Piedras preciosas',
    commodity_fr = 'Pierres précieuses',
    commodity_ru = 'Драгоценные камни'
WHERE LOWER(commodity) = LOWER('Precious Gems');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Progenitor Cells');
UPDATE commodities
SET commodity_de = 'Vorläuferzellen',
    commodity_es = 'Células madre',
    commodity_fr = 'Cellules souches',
    commodity_ru = 'Прогениторные клетки'
WHERE LOWER(commodity) = LOWER('Progenitor Cells');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Prohibited Research Materials');
UPDATE commodities
SET commodity_de = 'Verbotene Forschungsmaterialien',
    commodity_es = 'Materiales de investigación prohibida',
    commodity_fr = 'Matériaux de recherches interdits',
    commodity_ru = 'Запретные материалы исследований'
WHERE LOWER(commodity) = LOWER('Prohibited Research Materials');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pyrophyllite');
UPDATE commodities
SET commodity_de = 'Pyrophyllit',
    commodity_es = 'Pirofilita',
    commodity_fr = 'Pyrophyllite',
    commodity_ru = 'Пирофиллит'
WHERE LOWER(commodity) = LOWER('Pyrophyllite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Radiation Baffle');
UPDATE commodities
SET commodity_de = 'Strahlungsabweiser',
    commodity_es = 'Deflector de radiación',
    commodity_fr = 'Écran antiradiation',
    commodity_ru = 'Отражатель излучения'
WHERE LOWER(commodity) = LOWER('Radiation Baffle');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Rajukru Multi-Stoves');
UPDATE commodities
SET commodity_de = 'Rajukru-Universalöfen',
    commodity_es = 'Multiestufas de Rajukru',
    commodity_fr = 'Réchaud universel de Rajukru',
    commodity_ru = 'Мультипечи Rajukru'
WHERE LOWER(commodity) = LOWER('Rajukru Multi-Stoves');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Rapa Bao Snake Skins');
UPDATE commodities
SET commodity_de = 'Rapa-Bao-Schlangenhäute',
    commodity_es = 'Pieles de serpiente de Rapa Bao',
    commodity_fr = 'Peau de serpent Rapa Bao',
    commodity_ru = 'Змеиные шкуры с Рапа Бао'
WHERE LOWER(commodity) = LOWER('Rapa Bao Snake Skins');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Reactive Armour');
UPDATE commodities
SET commodity_de = 'Reaktivrüstung',
    commodity_es = 'Blindaje reactivo',
    commodity_fr = 'Protection réactive',
    commodity_ru = 'Реактивная защита'
WHERE LOWER(commodity) = LOWER('Reactive Armour');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Reinforced Mounting Plate');
UPDATE commodities
SET commodity_de = 'Verstärkte Trägerplatte',
    commodity_es = 'Placa de anclaje reforzada',
    commodity_fr = 'Plaque de montage renforcée',
    commodity_ru = 'Усиленная монтажная плита'
WHERE LOWER(commodity) = LOWER('Reinforced Mounting Plate');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Hudson’s Field Supplies');
UPDATE commodities
SET commodity_de = 'Hudsons Feldversorgung',
    commodity_es = 'Suministros de campaña de Hudson',
    commodity_fr = 'Ravitaillement militaire de Hudson',
    commodity_ru = 'Полевые припасы Хадсона'
WHERE LOWER(commodity) = LOWER('Hudson’s Field Supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Hudson Garrison Supplies');
UPDATE commodities
SET commodity_de = 'Hudsons Garnisonsversorgung',
    commodity_es = 'Suministros de guarnición de Hudson',
    commodity_fr = 'Hudsons Garnisonsversorgung',
    commodity_ru = 'Гарнизонные припасы Хадсона'
WHERE LOWER(commodity) = LOWER('Hudson Garrison Supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Resonating Separators');
UPDATE commodities
SET commodity_de = 'Resonanzabgrenzer',
    commodity_es = 'Separadores resonantes',
    commodity_fr = 'Séparateurs à résonance',
    commodity_ru = 'Резонансные сепараторы'
WHERE LOWER(commodity) = LOWER('Resonating Separators');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Hudson’s Restricted Intel');
UPDATE commodities
SET commodity_de = 'Hudsons Geheiminformationen',
    commodity_es = 'Inteligencia restringida de Hudson',
    commodity_fr = 'Renseignements confidentiels de Hudson',
    commodity_ru = 'Секретные разведданные Хадсона'
WHERE LOWER(commodity) = LOWER('Hudson’s Restricted Intel');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Core Restricted Package');
UPDATE commodities
SET commodity_de = 'Vertrauliches Paket der Core Holding',
    commodity_es = 'Paquete restringido de Core',
    commodity_fr = 'Colis scellés Core Holding',
    commodity_ru = 'Секретный груз Core'
WHERE LOWER(commodity) = LOWER('Core Restricted Package');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Rhodplumsite');
UPDATE commodities
SET commodity_de = 'Rhodplumsit',
    commodity_es = 'Rhodplumsita',
    commodity_fr = 'Rhodplumsite',
    commodity_ru = 'Родплумсайт'
WHERE LOWER(commodity) = LOWER('Rhodplumsite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Robotics');
UPDATE commodities
SET commodity_de = 'Roboter',
    commodity_es = 'Robótica',
    commodity_fr = 'Robots',
    commodity_ru = 'Роботы'
WHERE LOWER(commodity) = LOWER('Robotics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Rockforth Fertiliser');
UPDATE commodities
SET commodity_de = 'Rockforth-Dünger',
    commodity_es = 'Fertilizante Rockforth',
    commodity_fr = 'Engrais Rockforth',
    commodity_ru = 'Удобрение Rockforth'
WHERE LOWER(commodity) = LOWER('Rockforth Fertiliser');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Rusani Old Smokey');
UPDATE commodities
SET commodity_es = 'Old Smokey de Rusani',
    commodity_fr = 'Rusani Old Smokey',
    commodity_ru = 'Старые папиросы Rusani'
WHERE LOWER(commodity) = LOWER('Rusani Old Smokey');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Rutile');
UPDATE commodities
SET commodity_de = 'Rutil',
    commodity_es = 'Rutilo',
    commodity_fr = 'Rutile',
    commodity_ru = 'Рутил'
WHERE LOWER(commodity) = LOWER('Rutile');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pod Outer Tissue');
UPDATE commodities
SET commodity_de = 'Hülsen-Außengewebe',
    commodity_es = 'Tejido exterior de cápsula',
    commodity_fr = 'Tissu extérieur de cosse',
    commodity_ru = 'Внешняя ткань семянки'
WHERE LOWER(commodity) = LOWER('Pod Outer Tissue');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pod Shell Tissue');
UPDATE commodities
SET commodity_de = 'Hülsen-Schalengewebe',
    commodity_es = 'Tejido de cáscara de cápsula',
    commodity_fr = 'Tissu de coque de cosse',
    commodity_ru = 'Ткань оболочки семянки'
WHERE LOWER(commodity) = LOWER('Pod Shell Tissue');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pod Mesoglea');
UPDATE commodities
SET commodity_de = 'Hülsen-Mesoglea',
    commodity_es = 'Vaina de mesoglea',
    commodity_fr = 'Mésoglée de cosse',
    commodity_ru = 'Мезоглея семянки'
WHERE LOWER(commodity) = LOWER('Pod Mesoglea');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pod Tissue');
UPDATE commodities
SET commodity_de = 'Hülsengewebe',
    commodity_es = 'Tejido de la vaina',
    commodity_fr = 'Tissu de cosse',
    commodity_ru = 'Ткань семянки'
WHERE LOWER(commodity) = LOWER('Pod Tissue');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pod Core Tissue');
UPDATE commodities
SET commodity_de = 'Hülsenkerngewebe',
    commodity_es = 'Tejido de núcleo de cápsula',
    commodity_fr = 'Tissu de noyau de cosse',
    commodity_ru = 'Ткань ядра семянки'
WHERE LOWER(commodity) = LOWER('Pod Core Tissue');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pod Surface Tissue');
UPDATE commodities
SET commodity_de = 'Hülsenoberflächegewebe',
    commodity_es = 'Tejido de superficie de cápsula',
    commodity_fr = 'Tissu de surface de cosse',
    commodity_ru = 'Поверхностная ткань семянки'
WHERE LOWER(commodity) = LOWER('Pod Surface Tissue');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Pod Dead Tissue');
UPDATE commodities
SET commodity_de = 'Hülsentotgewebe',
    commodity_es = 'Tejido muerto de cápsula',
    commodity_fr = 'Tissu mort de cosse',
    commodity_ru = 'Мертвая ткань семянки'
WHERE LOWER(commodity) = LOWER('Pod Dead Tissue');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Samarium');
UPDATE commodities
SET commodity_es = 'Samario',
    commodity_fr = 'Samarium',
    commodity_ru = 'Самарий'
WHERE LOWER(commodity) = LOWER('Samarium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Sanuma Decorative Meat');
UPDATE commodities
SET commodity_de = 'Sanuma-Dekorfleisch',
    commodity_es = 'Carne artística de Sanuma',
    commodity_fr = 'Viande ornementale de Sanuma',
    commodity_ru = 'Декоративное мясо с Касанумы'
WHERE LOWER(commodity) = LOWER('Sanuma Decorative Meat');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('SAP 8 Core Container');
UPDATE commodities
SET commodity_de = 'Container mit SAP 8-Kern',
    commodity_es = 'Núcleos SAP-8',
    commodity_fr = 'Conteneur fusion SAP 8',
    commodity_ru = 'Контейнер «SAP 8 Core»'
WHERE LOWER(commodity) = LOWER('SAP 8 Core Container');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Saxon Wine');
UPDATE commodities
SET commodity_de = 'Sachsenwein',
    commodity_es = 'Vino sajón',
    commodity_fr = 'Vin saxon',
    commodity_ru = 'Саксонское вино'
WHERE LOWER(commodity) = LOWER('Saxon Wine');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Scientific Research');
UPDATE commodities
SET commodity_de = 'Wissenschaftliche Forschung',
    commodity_es = 'Investigaciones científicas',
    commodity_fr = 'Recherches scientifiques',
    commodity_ru = 'Материалы исследования'
WHERE LOWER(commodity) = LOWER('Scientific Research');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Scientific Samples');
UPDATE commodities
SET commodity_de = 'Wissenschaftliche Proben',
    commodity_es = 'Muestras científicas',
    commodity_fr = 'Échantillons scientifiques',
    commodity_ru = 'Научные образцы'
WHERE LOWER(commodity) = LOWER('Scientific Samples');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Scrap');
UPDATE commodities
SET commodity_de = 'Schrott',
    commodity_es = 'Chatarra',
    commodity_fr = 'Ferraille',
    commodity_ru = 'Утильсырье'
WHERE LOWER(commodity) = LOWER('Scrap');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Semiconductors');
UPDATE commodities
SET commodity_de = 'Halbleiter',
    commodity_es = 'Semiconductores',
    commodity_fr = 'Semi-conducteur(s)',
    commodity_ru = 'Полупроводники'
WHERE LOWER(commodity) = LOWER('Semiconductors');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Serendibite');
UPDATE commodities
SET commodity_de = 'Serendibit',
    commodity_es = 'Serendibita',
    commodity_fr = 'Serendibite',
    commodity_ru = 'Серендибит'
WHERE LOWER(commodity) = LOWER('Serendibite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Shan’s Charis Orchid');
UPDATE commodities
SET commodity_de = 'Shans Charis-Orchidee',
    commodity_es = 'Orquídea Charis de Shan',
    commodity_fr = 'Orchidée Shan Charis',
    commodity_ru = 'Орхидея Shan Charis'
WHERE LOWER(commodity) = LOWER('Shan’s Charis Orchid');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Silver');
UPDATE commodities
SET commodity_de = 'Silber',
    commodity_es = 'Plata',
    commodity_fr = 'Argent',
    commodity_ru = 'Серебро'
WHERE LOWER(commodity) = LOWER('Silver');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Sirius Corporate Contracts');
UPDATE commodities
SET commodity_de = 'Sirius Unternehmensverträge',
    commodity_es = 'Contratos corporativos de Sirius',
    commodity_fr = 'Contrats de Sirius',
    commodity_ru = 'Корпоративные контракты Sirius'
WHERE LOWER(commodity) = LOWER('Sirius Corporate Contracts');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Sirius Franchise Package');
UPDATE commodities
SET commodity_de = 'Sirius Franchise-Paket',
    commodity_es = 'Paquete de franquicia de Sirius',
    commodity_fr = 'Dossiers marketing de Sirius',
    commodity_ru = 'Посылка с товаром Sirius'
WHERE LOWER(commodity) = LOWER('Sirius Franchise Package');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Sirius Industrial Equipment');
UPDATE commodities
SET commodity_de = 'Sirius Industrieausrüstung',
    commodity_es = 'Equipo industrial de Sirius',
    commodity_fr = 'Équipements industriels de Sirius',
    commodity_ru = 'Промышленное оборудование Sirius'
WHERE LOWER(commodity) = LOWER('Sirius Industrial Equipment');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Skimmer Components');
UPDATE commodities
SET commodity_de = 'OF-Drohnen-Komponenten',
    commodity_es = 'Componentes de deslizador',
    commodity_fr = 'Composants de protecteurs',
    commodity_ru = 'Детали оборон. беспилотников'
WHERE LOWER(commodity) = LOWER('Skimmer Components');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Slaves');
UPDATE commodities
SET commodity_de = 'Sklaven',
    commodity_es = 'Esclavos',
    commodity_fr = 'Esclaves',
    commodity_ru = 'Рабы'
WHERE LOWER(commodity) = LOWER('Slaves');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Small Survey Data Cache');
UPDATE commodities
SET commodity_de = 'Kleiner Erkundungsdatenspeicher',
    commodity_es = 'Memorias de reconocimiento pequeñas',
    commodity_fr = 'Petit lot de données d’explorations',
    commodity_ru = 'Малый пакет с данными исследования'
WHERE LOWER(commodity) = LOWER('Small Survey Data Cache');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Soontill Relics');
UPDATE commodities
SET commodity_de = 'Soontill-Relikte',
    commodity_es = 'Reliquias de Soontill',
    commodity_fr = 'Reliques de Soontill',
    commodity_ru = 'Реликты Сунтилла'
WHERE LOWER(commodity) = LOWER('Soontill Relics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Sothis Crystalline Gold');
UPDATE commodities
SET commodity_de = 'Sothis-Kristallingold',
    commodity_es = 'Oro cristalino de Sothis',
    commodity_fr = 'Or cristallin de Sothis',
    commodity_ru = 'Кристаллическое золото системы Sothis'
WHERE LOWER(commodity) = LOWER('Sothis Crystalline Gold');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Space Pioneer Relics');
UPDATE commodities
SET commodity_de = 'Weltraumpionier-Relikte',
    commodity_es = 'Reliquias de pioneros espaciales',
    commodity_fr = 'Reliques des pionniers de l’espace',
    commodity_ru = 'Следы первопроходцев космоса'
WHERE LOWER(commodity) = LOWER('Space Pioneer Relics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Steel');
UPDATE commodities
SET commodity_es = 'Acero',
    commodity_fr = 'Acier',
    commodity_ru = 'Сталь'
WHERE LOWER(commodity) = LOWER('Steel');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Structural Regulators');
UPDATE commodities
SET commodity_de = 'Strukturregulatoren',
    commodity_es = 'Reguladores estructurales',
    commodity_fr = 'Régulateur(s) structurel(s)',
    commodity_ru = 'Конструкционные регуляторы'
WHERE LOWER(commodity) = LOWER('Structural Regulators');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Superconductors');
UPDATE commodities
SET commodity_de = 'Supraleiter',
    commodity_es = 'Superconductores',
    commodity_fr = 'Supraconducteur(s)',
    commodity_ru = 'Сверхпроводники'
WHERE LOWER(commodity) = LOWER('Superconductors');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Surface Stabilisers');
UPDATE commodities
SET commodity_de = 'Oberflächenstabilisierer',
    commodity_es = 'Estabilizadores de superficie',
    commodity_fr = 'Stabilisateurs de surface',
    commodity_ru = 'Стабилизаторы поверхности'
WHERE LOWER(commodity) = LOWER('Surface Stabilisers');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Survival Equipment');
UPDATE commodities
SET commodity_de = 'Überlebensausrüstung',
    commodity_es = 'Equipamiento de supervivencia',
    commodity_fr = 'Équipement de survie',
    commodity_ru = 'Снаряжение для выживания'
WHERE LOWER(commodity) = LOWER('Survival Equipment');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Synthetic Fabrics');
UPDATE commodities
SET commodity_de = 'Chemiefasern',
    commodity_es = 'Tejidos sintéticos',
    commodity_fr = 'Tissu(s) synthétique(s)',
    commodity_ru = 'Синтетическая ткань'
WHERE LOWER(commodity) = LOWER('Synthetic Fabrics');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Synthetic Meat');
UPDATE commodities
SET commodity_de = 'Künstliches Fleisch',
    commodity_es = 'Carne sintética',
    commodity_fr = 'Viande synthétique',
    commodity_ru = 'Синтетическое мясо'
WHERE LOWER(commodity) = LOWER('Synthetic Meat');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Synthetic Reagents');
UPDATE commodities
SET commodity_de = 'Synthetische Reagenzstoffe',
    commodity_es = 'Reagentes sintéticos',
    commodity_fr = 'Réactifs synthétiques',
    commodity_ru = 'Синтетические реагенты'
WHERE LOWER(commodity) = LOWER('Synthetic Reagents');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Taaffeite');
UPDATE commodities
SET commodity_de = 'Taaffeit',
    commodity_es = 'Taaffeíta',
    commodity_fr = 'Taafféite',
    commodity_ru = 'Тааффеит'
WHERE LOWER(commodity) = LOWER('Taaffeite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Tactical Data');
UPDATE commodities
SET commodity_de = 'Aufklärungsdaten',
    commodity_es = 'Datos tácticos',
    commodity_fr = 'Données tactiques',
    commodity_ru = 'Тактические данные'
WHERE LOWER(commodity) = LOWER('Tactical Data');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Tanmark Tranquil Tea');
UPDATE commodities
SET commodity_de = 'Tanmark-Besänftigungstee',
    commodity_es = 'Té relajante de Tanmark',
    commodity_fr = 'Thé de la tranquillité de Tanmark',
    commodity_ru = 'Успокаивающий чай Tanmark'
WHERE LOWER(commodity) = LOWER('Tanmark Tranquil Tea');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Tantalum');
UPDATE commodities
SET commodity_de = 'Tantal',
    commodity_es = 'Tantalio',
    commodity_fr = 'Tantale',
    commodity_ru = 'Тантал'
WHERE LOWER(commodity) = LOWER('Tantalum');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Tarach Spice');
UPDATE commodities
SET commodity_es = 'Especia de Tarach Tor',
    commodity_fr = 'Épice de Tarash',
    commodity_ru = 'Таракская пряность'
WHERE LOWER(commodity) = LOWER('Tarach Spice');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Tauri Chimes');
UPDATE commodities
SET commodity_de = 'Tauri-Glockenspiel',
    commodity_es = 'Carillones de 39 Tauri',
    commodity_fr = 'Carillons taurins',
    commodity_ru = 'Колокольчики с Тельца'
WHERE LOWER(commodity) = LOWER('Tauri Chimes');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Tea');
UPDATE commodities
SET commodity_de = 'Tee',
    commodity_es = 'Té',
    commodity_fr = 'Thé(s)',
    commodity_ru = 'Чай'
WHERE LOWER(commodity) = LOWER('Tea');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Telemetry Suite');
UPDATE commodities
SET commodity_de = 'Telemetrie-Paket',
    commodity_es = 'Paquete de telemetría',
    commodity_fr = 'Système de télémétrie',
    commodity_ru = 'Телеметрический комплект'
WHERE LOWER(commodity) = LOWER('Telemetry Suite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Land Enrichment Systems');
UPDATE commodities
SET commodity_de = 'Landanreicherungssysteme',
    commodity_es = 'Enriquecimiento terrestre',
    commodity_fr = 'Sys. enrichissement sols',
    commodity_ru = 'Системы обогащения почвы'
WHERE LOWER(commodity) = LOWER('Land Enrichment Systems');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Terra Mater Blood Bores');
UPDATE commodities
SET commodity_de = 'Terra-Mater-Berserker',
    commodity_es = 'Potenciadores sanguíneos de Terra Mater',
    commodity_fr = 'Plasma sanguin de Terra Mater',
    commodity_ru = 'Кровяные усилители с Терра матер'
WHERE LOWER(commodity) = LOWER('Terra Mater Blood Bores');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thallium');
UPDATE commodities
SET commodity_es = 'Talio',
    commodity_fr = 'Thallium',
    commodity_ru = 'Таллий'
WHERE LOWER(commodity) = LOWER('Thallium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Bone Fragments');
UPDATE commodities
SET commodity_es = 'Fragmentos de Hueso',
    commodity_fr = 'Fragments d''os',
    commodity_ru = 'Фрагменты кости'
WHERE LOWER(commodity) = LOWER('Bone Fragments');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Cyst Specimen');
UPDATE commodities
SET commodity_es = 'Especimen de Quiste',
    commodity_ru = 'Образец кисты'
WHERE LOWER(commodity) = LOWER('Cyst Specimen');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Caustic Tissue Sample (Thargoid)');
UPDATE commodities
SET commodity_de = 'Thargoidengenerator-Gewebeprobe',
    commodity_es = 'Muestra de tejido de Generador Thargoide',
    commodity_fr = 'Échantillon de tissu d’un Générateur thargoid',
    commodity_ru = 'Образец ткани таргоидского генератора коррозии'
WHERE LOWER(commodity) = LOWER('Caustic Tissue Sample (Thargoid)');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Heart');
UPDATE commodities
SET commodity_de = 'Thargoidenherz',
    commodity_es = 'Corazón Thargoide',
    commodity_fr = 'Cœur thargoid',
    commodity_ru = 'Таргоидское «сердце»'
WHERE LOWER(commodity) = LOWER('Thargoid Heart');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Organ Sample');
UPDATE commodities
SET commodity_es = 'Muestra de Órgano',
    commodity_fr = 'Échantillon d''organe',
    commodity_ru = 'Образец органа'
WHERE LOWER(commodity) = LOWER('Organ Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Bio-storage Capsule');
UPDATE commodities
SET commodity_es = 'Cápsula de Bioconservación Thargoide',
    commodity_fr = 'Capsule de bioconfinement thargoïd',
    commodity_ru = 'Таргоидская капсула для биоматериалов'
WHERE LOWER(commodity) = LOWER('Thargoid Bio-storage Capsule');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Scout Tissue Sample');
UPDATE commodities
SET commodity_de = 'Gewebeprobe von Thargoiden-Späher',
    commodity_es = 'Muestra de tejido de explorador Thargoide',
    commodity_fr = 'Échantillon de tissu thargoid - éclaireur',
    commodity_ru = 'Образец тканей таргоида-разведчика'
WHERE LOWER(commodity) = LOWER('Thargoid Scout Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Titan Deep Tissue Sample');
UPDATE commodities
SET commodity_es = 'Muestra de tejido de Titán Profunda',
    commodity_fr = 'Échantillon de tissu profond — Titan',
    commodity_ru = 'Образец глубокой ткани Титана'
WHERE LOWER(commodity) = LOWER('Titan Deep Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Titan Tissue Sample');
UPDATE commodities
SET commodity_es = 'Muestra de tejido de Titán',
    commodity_fr = 'Échantillon de tissu — Titan',
    commodity_ru = 'Образец ткани Титана'
WHERE LOWER(commodity) = LOWER('Titan Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Titan Partial Tissue Sample');
UPDATE commodities
SET commodity_es = 'Muestra parcial de tejido de Titán',
    commodity_fr = 'Échantillon de tissu partiel — Titan',
    commodity_ru = 'Неполный образец ткани Титана'
WHERE LOWER(commodity) = LOWER('Titan Partial Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Cyclops Tissue Sample');
UPDATE commodities
SET commodity_de = 'Gewebeprobe von Thargoidenschiffstyp Cyclops',
    commodity_es = 'Muestra de tejido de cíclope Thargoide',
    commodity_fr = 'Échantillon de tissu thargoid - Cyclops',
    commodity_ru = 'Образец ткани таргоидского корабля «Циклоп»'
WHERE LOWER(commodity) = LOWER('Thargoid Cyclops Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Titan Maw Deep Tissue Sample');
UPDATE commodities
SET commodity_es = 'Muestra de Fauces profunda de Titán',
    commodity_fr = 'Échantillon de tissu profond de gueule — Titan',
    commodity_ru = 'Образец глубокой ткани пасти Титана'
WHERE LOWER(commodity) = LOWER('Titan Maw Deep Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Titan Maw Tissue Sample');
UPDATE commodities
SET commodity_es = 'Muestra de Fauces de Titán',
    commodity_fr = 'Échantillon de tissu de gueule — Titan',
    commodity_ru = 'Образец ткани пасти Титана'
WHERE LOWER(commodity) = LOWER('Titan Maw Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Titan Maw Partial Tissue Sample');
UPDATE commodities
SET commodity_es = 'Muestra parcial d Fauces de Titán',
    commodity_fr = 'Échantillon de tissu de gueule partiel — Titan',
    commodity_ru = 'Неполный образец ткани пасти Титана'
WHERE LOWER(commodity) = LOWER('Titan Maw Partial Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Basilisk Tissue Sample');
UPDATE commodities
SET commodity_de = 'Gewebeprobe von Thargoidenschiffstyp Basilisk',
    commodity_es = 'Muestra de tejido de basilisco Thargoide',
    commodity_fr = 'Échantillon de tissu thargoid - Basilisk',
    commodity_ru = 'Образец ткани таргоидского корабля «Василиск»'
WHERE LOWER(commodity) = LOWER('Thargoid Basilisk Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Medusa Tissue Sample');
UPDATE commodities
SET commodity_de = 'Gewebeprobe von Thargoidenschiffstyp Medusa',
    commodity_es = 'Muestra de tejido de medusa Thargoide',
    commodity_fr = 'Échantillon de tissu thargoid - Medusa',
    commodity_ru = 'Образец ткани таргоидского корабля «Медуза»'
WHERE LOWER(commodity) = LOWER('Thargoid Medusa Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Hydra Tissue Sample');
UPDATE commodities
SET commodity_de = 'Thargoidenhydra-Gewebeprobe',
    commodity_es = 'Muestra de tejido de Hidra Thargoide',
    commodity_fr = 'Échantillon de tissu d’un Hydra thargoid',
    commodity_ru = 'Образец ткани таргоидского корабля «Гидра»'
WHERE LOWER(commodity) = LOWER('Thargoid Hydra Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Orthrus Tissue Sample');
UPDATE commodities
SET commodity_de = 'ThargoidenOrthrus-Gewebeprobe',
    commodity_es = 'Muestra de tejido de Orthrus Thargoide',
    commodity_fr = 'Échantillon de tissu d’un Orthrus thargoid',
    commodity_ru = 'Образец ткани таргоидского корабля «Орф»'
WHERE LOWER(commodity) = LOWER('Thargoid Orthrus Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Glaive Tissue Sample');
UPDATE commodities
SET commodity_es = 'Muestra de tejido de Glaive Thargoide',
    commodity_fr = 'Échantillon de tissu thargoïd - Glaive',
    commodity_ru = 'Образец ткани таргоидского корабля «Глефа»'
WHERE LOWER(commodity) = LOWER('Thargoid Glaive Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Scythe Tissue Sample');
UPDATE commodities
SET commodity_es = 'Muestra de tejido de Scythe Thargoide',
    commodity_fr = 'Échantillon de tissu thargoïd - Scythe',
    commodity_ru = 'Образец ткани таргоидского корабля «Коса»'
WHERE LOWER(commodity) = LOWER('Thargoid Scythe Tissue Sample');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Titan Drive Component');
UPDATE commodities
SET commodity_es = 'Componente de Motor de Titán',
    commodity_ru = 'Компонент двигателя титана'
WHERE LOWER(commodity) = LOWER('Titan Drive Component');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('The Hutton Mug');
UPDATE commodities
SET commodity_de = 'Hutton-Becher',
    commodity_es = 'Taza de Hutton',
    commodity_fr = 'La tasse Hutton',
    commodity_ru = 'Кружка Hutton'
WHERE LOWER(commodity) = LOWER('The Hutton Mug');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thermal Cooling Units');
UPDATE commodities
SET commodity_de = 'Thermal-Kühleinheiten',
    commodity_es = 'Unidades de enfriamiento térmico',
    commodity_fr = 'Unités de refroidissement',
    commodity_ru = 'Термальные охладители'
WHERE LOWER(commodity) = LOWER('Thermal Cooling Units');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thorium');
UPDATE commodities
SET commodity_es = 'Torio',
    commodity_fr = 'Thorium',
    commodity_ru = 'Торий'
WHERE LOWER(commodity) = LOWER('Thorium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thrutis Cream');
UPDATE commodities
SET commodity_es = 'Crema de Thrutis',
    commodity_fr = 'Crème de Thrutis',
    commodity_ru = 'Трутисские сливки'
WHERE LOWER(commodity) = LOWER('Thrutis Cream');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Tiegfries Synth Silk');
UPDATE commodities
SET commodity_de = 'Tiegfries-Synthetikseide',
    commodity_es = 'Seda sintética de Tiegfries',
    commodity_fr = 'Soie synthétique de Tiegfries',
    commodity_ru = 'Тигфрайский синтешелк'
WHERE LOWER(commodity) = LOWER('Tiegfries Synth Silk');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Time Capsule');
UPDATE commodities
SET commodity_de = 'Zeitkapsel',
    commodity_es = 'Capsulas del tiempo',
    commodity_fr = 'Capsule temporelle',
    commodity_ru = 'Мемориальная капсула'
WHERE LOWER(commodity) = LOWER('Time Capsule');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Tiolce Waste2Paste Units');
UPDATE commodities
SET commodity_de = 'Müll-zu-Paste-Einheiten',
    commodity_es = 'Pastaplasta de Tiolce',
    commodity_fr = 'Pâte 2 Déchets de Tiolce',
    commodity_ru = 'Устройства Мусор-в-пасту Tiolce'
WHERE LOWER(commodity) = LOWER('Tiolce Waste2Paste Units');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Titanium');
UPDATE commodities
SET commodity_de = 'Titan',
    commodity_es = 'Titanio',
    commodity_fr = 'Titane',
    commodity_ru = 'Титан'
WHERE LOWER(commodity) = LOWER('Titanium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Tobacco');
UPDATE commodities
SET commodity_de = 'Tabak',
    commodity_es = 'Tabaco',
    commodity_fr = 'Tabac',
    commodity_ru = 'Табак'
WHERE LOWER(commodity) = LOWER('Tobacco');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Torval Trade Agreements');
UPDATE commodities
SET commodity_de = 'Torvals Handelsabkommen',
    commodity_es = 'Acuerdos comerciales de Torval',
    commodity_fr = 'Accords commerciaux de Torval',
    commodity_ru = 'Торговые соглашения Торвал'
WHERE LOWER(commodity) = LOWER('Torval Trade Agreements');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Torval Deeds');
UPDATE commodities
SET commodity_de = 'Torvals Urkunden',
    commodity_es = 'Escrituras de Torval',
    commodity_fr = 'Actes juridiques de Torval',
    commodity_ru = 'Договора Торвал'
WHERE LOWER(commodity) = LOWER('Torval Deeds');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Toxandji Virocide');
UPDATE commodities
SET commodity_es = 'Virocida de Toxandji',
    commodity_fr = 'Virocide Toxandji',
    commodity_ru = 'Токсанджийские вирициды'
WHERE LOWER(commodity) = LOWER('Toxandji Virocide');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Toxic Waste');
UPDATE commodities
SET commodity_de = 'Giftmüll',
    commodity_es = 'Residuos tóxicos',
    commodity_fr = 'Déchets toxiques',
    commodity_ru = 'Токсичные отходы'
WHERE LOWER(commodity) = LOWER('Toxic Waste');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Lucan Onionhead');
UPDATE commodities
SET commodity_es = 'Cebollazo de Lucan',
    commodity_fr = 'Tête d’oignon lucane',
    commodity_ru = 'Луканская луковая головка'
WHERE LOWER(commodity) = LOWER('Lucan Onionhead');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Trinkets of Hidden Fortune');
UPDATE commodities
SET commodity_de = 'Schmuckstücke von unschätzbarem Wert',
    commodity_es = 'Baratijas de fortuna oculta',
    commodity_fr = 'Amulettes porte-bonheur',
    commodity_ru = 'Безделушки таинственной Фортуны'
WHERE LOWER(commodity) = LOWER('Trinkets of Hidden Fortune');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Tritium');
UPDATE commodities
SET commodity_es = 'Tritio',
    commodity_fr = 'Tritium',
    commodity_ru = 'Тритий'
WHERE LOWER(commodity) = LOWER('Tritium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Grom Underground Support');
UPDATE commodities
SET commodity_de = 'Untergrund-Support für Grom',
    commodity_es = 'Apoyo encubierto de Grom',
    commodity_fr = 'Ravitaillement clandestin de Grom',
    commodity_ru = 'Подпольная поддержка Грома'
WHERE LOWER(commodity) = LOWER('Grom Underground Support');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Unknown');
UPDATE commodities
SET commodity_de = 'Unbekannt',
    commodity_es = 'Desconocido',
    commodity_fr = 'Inconnue',
    commodity_ru = 'Неизвестный'
WHERE LOWER(commodity) = LOWER('Unknown');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Sensor');
UPDATE commodities
SET commodity_de = 'Thargoidensensor',
    commodity_es = 'Sensor Thargoide',
    commodity_fr = 'Capteur thargoid',
    commodity_ru = 'Таргоидский сенсор'
WHERE LOWER(commodity) = LOWER('Thargoid Sensor');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Probe');
UPDATE commodities
SET commodity_de = 'Thargoidensonde',
    commodity_es = 'Sonda Thargoide',
    commodity_fr = 'Sonde thargoid',
    commodity_ru = 'Таргоидский зонд'
WHERE LOWER(commodity) = LOWER('Thargoid Probe');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Link');
UPDATE commodities
SET commodity_de = 'Thargoidenlink',
    commodity_es = 'Enlace Thargoide',
    commodity_fr = 'Liaison thargoid',
    commodity_ru = 'Таргоидское звено'
WHERE LOWER(commodity) = LOWER('Thargoid Link');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Biological Matter');
UPDATE commodities
SET commodity_de = 'Biologische Materie der Thargoiden',
    commodity_es = 'Materia biológica Thargoide',
    commodity_fr = 'Matière biologique thargoid',
    commodity_ru = 'Таргоидская биомасса'
WHERE LOWER(commodity) = LOWER('Thargoid Biological Matter');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Impure Spire Mineral');
UPDATE commodities
SET commodity_es = 'Mineral de aguja impuro',
    commodity_ru = 'Неочищенный минерал со шпилей'
WHERE LOWER(commodity) = LOWER('Impure Spire Mineral');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Semi-Refined Spire Mineral');
UPDATE commodities
SET commodity_es = 'Mineral de aguja Semi-Refinado',
    commodity_ru = 'Полуочищенный минерал со шпилей'
WHERE LOWER(commodity) = LOWER('Semi-Refined Spire Mineral');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Resin');
UPDATE commodities
SET commodity_de = 'Thargoidenharz',
    commodity_es = 'Resina Thargoide',
    commodity_fr = 'Résine thargoid',
    commodity_ru = 'Таргоидская смола'
WHERE LOWER(commodity) = LOWER('Thargoid Resin');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Protective Membrane Scrap');
UPDATE commodities
SET commodity_de = 'Unbrauchbare Schutzmembran',
    commodity_es = 'Fragmento de membrana protectora',
    commodity_ru = 'Остатки защитной мембраны'
WHERE LOWER(commodity) = LOWER('Protective Membrane Scrap');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Thargoid Technology Samples');
UPDATE commodities
SET commodity_de = 'Technologieproben der Thargoiden',
    commodity_es = 'Muestras de tecnología Thargoide',
    commodity_fr = 'Échantillons de technologie thargoid',
    commodity_ru = 'Образцы таргоидских технологий'
WHERE LOWER(commodity) = LOWER('Thargoid Technology Samples');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Unmarked Military supplies');
UPDATE commodities
SET commodity_de = 'Unmarkierte Militärmaterialien',
    commodity_es = 'Suministros militares sin marcar',
    commodity_fr = 'Surplus militaire banalisé',
    commodity_ru = 'Военные припасы без меток'
WHERE LOWER(commodity) = LOWER('Unmarked Military supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Unoccupied Escape Pod');
UPDATE commodities
SET commodity_de = 'Unbesetzte Fluchtkapsel',
    commodity_es = 'Cápsula de escape desocupada',
    commodity_fr = 'Pod évadé inoccupé',
    commodity_ru = 'Пустая спасательная капсула'
WHERE LOWER(commodity) = LOWER('Unoccupied Escape Pod');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Unstable Data Core');
UPDATE commodities
SET commodity_de = 'Instabiler Datenkern',
    commodity_es = 'Unidad de datos inestable',
    commodity_fr = 'Centre de données instable',
    commodity_ru = 'Нестабильное ядро данных'
WHERE LOWER(commodity) = LOWER('Unstable Data Core');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Uraninite');
UPDATE commodities
SET commodity_de = 'Uraninit',
    commodity_es = 'Uraninita',
    commodity_fr = 'Uraninite',
    commodity_ru = 'Уранинит'
WHERE LOWER(commodity) = LOWER('Uraninite');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Uranium');
UPDATE commodities
SET commodity_de = 'Uran',
    commodity_es = 'Uranio',
    commodity_fr = 'Uranium',
    commodity_ru = 'Уран'
WHERE LOWER(commodity) = LOWER('Uranium');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Ancient Artefact');
UPDATE commodities
SET commodity_de = 'Uraltes Artefakt',
    commodity_es = 'Artefactos antiguos',
    commodity_fr = 'Relique antique',
    commodity_ru = 'Древний артефакт'
WHERE LOWER(commodity) = LOWER('Ancient Artefact');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Black Box');
UPDATE commodities
SET commodity_es = 'Cajas negras',
    commodity_fr = 'Boîte noire',
    commodity_ru = 'Черный ящик'
WHERE LOWER(commodity) = LOWER('Black Box');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Experimental Chemicals');
UPDATE commodities
SET commodity_de = 'Experimentelle Chemikalien',
    commodity_es = 'Químicos experimentales',
    commodity_fr = 'Produits chimiques expérimentaux',
    commodity_ru = 'Экспериментальные химикаты'
WHERE LOWER(commodity) = LOWER('Experimental Chemicals');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Military Plans');
UPDATE commodities
SET commodity_de = 'Militärpläne',
    commodity_es = 'Planes militares',
    commodity_fr = 'Plans militaires',
    commodity_ru = 'Военные планы'
WHERE LOWER(commodity) = LOWER('Military Plans');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Prototype Tech');
UPDATE commodities
SET commodity_de = 'Prototyp-Technologien',
    commodity_es = 'Prototipos tecnológicos',
    commodity_fr = 'Prototype technologique',
    commodity_ru = 'Экспериментальная техника'
WHERE LOWER(commodity) = LOWER('Prototype Tech');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Rare Artwork');
UPDATE commodities
SET commodity_de = 'Seltene Kunstgegenstände',
    commodity_es = 'Arte poco común',
    commodity_fr = 'Œuvre d’art',
    commodity_ru = 'Редкие произведения искусства'
WHERE LOWER(commodity) = LOWER('Rare Artwork');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Rebel Transmissions');
UPDATE commodities
SET commodity_de = 'Rebellenübertragungen',
    commodity_es = 'Transmisiones rebeldes',
    commodity_fr = 'Transmissions rebelles',
    commodity_ru = 'Переговоры повстанцев'
WHERE LOWER(commodity) = LOWER('Rebel Transmissions');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Technical Blueprints');
UPDATE commodities
SET commodity_de = 'Technische Baupläne',
    commodity_es = 'Planos técnicos',
    commodity_fr = 'Plans industriels',
    commodity_ru = 'Промышленные чертежи'
WHERE LOWER(commodity) = LOWER('Technical Blueprints');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Trade Data');
UPDATE commodities
SET commodity_de = 'Handelsdaten',
    commodity_es = 'Datos comerciales',
    commodity_fr = 'Données commerciales',
    commodity_ru = 'Торговая информация'
WHERE LOWER(commodity) = LOWER('Trade Data');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Uszaian Tree Grub');
UPDATE commodities
SET commodity_de = 'Uszaian-Baumlarven',
    commodity_es = 'Larva de árbol uszaiana',
    commodity_fr = 'Larve uszaienne',
    commodity_ru = 'Ушжаанский росток дерева'
WHERE LOWER(commodity) = LOWER('Uszaian Tree Grub');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Utgaroar Millennial Eggs');
UPDATE commodities
SET commodity_de = 'Utgaroar-Milleniumeier',
    commodity_es = 'Huevos milenarios de Utgaroar',
    commodity_fr = 'Œufs millénaires d’Utgaroar',
    commodity_ru = 'Миллениальные яйца с Утгарора'
WHERE LOWER(commodity) = LOWER('Utgaroar Millennial Eggs');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Utopian Dissident');
UPDATE commodities
SET commodity_de = 'Utopia-Dissidenten',
    commodity_es = 'Disidentes de Utopian',
    commodity_fr = 'Dissidents utopiens',
    commodity_ru = 'Диссиденты Утопии'
WHERE LOWER(commodity) = LOWER('Utopian Dissident');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Utopian Supplies');
UPDATE commodities
SET commodity_de = 'Utopia-Versorgungsmaterialien',
    commodity_es = 'Suministros de Utopian',
    commodity_fr = 'Marchandises d’Utopia',
    commodity_ru = 'Припасы Утопии'
WHERE LOWER(commodity) = LOWER('Utopian Supplies');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Utopian Publicity');
UPDATE commodities
SET commodity_de = 'Utopia-PR-Dokumente',
    commodity_es = 'Publicidad de Utopian',
    commodity_fr = 'Pamphlets d’Utopia',
    commodity_ru = 'Публикации Утопии'
WHERE LOWER(commodity) = LOWER('Utopian Publicity');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Uzumoku Low-G Wings');
UPDATE commodities
SET commodity_de = 'Uzumoku Leichtkraftflügel',
    commodity_es = 'Alas de baja gravedad de Uzumoku',
    commodity_fr = 'Ailes à faible gravité Uzumoku',
    commodity_ru = 'Узумокские крылья для малой гравитации'
WHERE LOWER(commodity) = LOWER('Uzumoku Low-G Wings');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Vanayequi Ceratomorpha Fur');
UPDATE commodities
SET commodity_de = 'Vanayequi-Ceratomorpha-Fell',
    commodity_es = 'Piel de ceratomorfo de Vanayequi',
    commodity_fr = 'Fourrure de cératomorphe de Vanayequi',
    commodity_ru = 'Мех носорогов с Ванайеку'
WHERE LOWER(commodity) = LOWER('Vanayequi Ceratomorpha Fur');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Vega Slimweed');
UPDATE commodities
SET commodity_de = 'Vega-Schlankkraut',
    commodity_es = 'Planta parásita de Vega',
    commodity_fr = 'Algues amincissantes de Vega',
    commodity_ru = 'Водоросли для похудения с Веги'
WHERE LOWER(commodity) = LOWER('Vega Slimweed');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('V Herculis Body Rub');
UPDATE commodities
SET commodity_de = 'V Herculis Körperschrubber',
    commodity_es = 'Exfoliantes de V1090 Herculis',
    commodity_fr = 'Lotion de soin V Herculis',
    commodity_ru = 'Скраб с V Геркулеса'
WHERE LOWER(commodity) = LOWER('V Herculis Body Rub');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Vidavantian Lace');
UPDATE commodities
SET commodity_de = 'Vidava-Spitze',
    commodity_es = 'Cordón vidavantino',
    commodity_fr = 'Dentelle vidavantienne',
    commodity_ru = 'Видавантийский шнурок'
WHERE LOWER(commodity) = LOWER('Vidavantian Lace');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Volkhab Bee Drones');
UPDATE commodities
SET commodity_de = 'Volkhab-Bienendrohnen',
    commodity_es = 'Abejas mecánicas de Volkhab',
    commodity_fr = 'Drones butineurs Volkhab',
    commodity_ru = 'Дроны Volkhab Bee'
WHERE LOWER(commodity) = LOWER('Volkhab Bee Drones');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Water');
UPDATE commodities
SET commodity_de = 'Wasser',
    commodity_es = 'Agua',
    commodity_fr = 'Eau',
    commodity_ru = 'Вода'
WHERE LOWER(commodity) = LOWER('Water');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Water Purifiers');
UPDATE commodities
SET commodity_de = 'Wasserreiniger',
    commodity_es = 'Purificadores de agua',
    commodity_fr = 'Purificateurs d’eau',
    commodity_ru = 'Водоочистители'
WHERE LOWER(commodity) = LOWER('Water Purifiers');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('The Waters Of Shintara');
UPDATE commodities
SET commodity_de = 'Shintara-Wasser',
    commodity_es = 'Aguas de Shintara',
    commodity_fr = 'Eau de Shintara',
    commodity_ru = 'Воды Шинрарты'
WHERE LOWER(commodity) = LOWER('The Waters Of Shintara');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Wheemete Wheat Cakes');
UPDATE commodities
SET commodity_de = 'Wheemete Weizenküchlein',
    commodity_es = 'Pasteles de trigo de Wheemete',
    commodity_fr = 'Gâteaux de blé Wheemete',
    commodity_ru = 'Уимитские пшеничные галеты'
WHERE LOWER(commodity) = LOWER('Wheemete Wheat Cakes');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Wine');
UPDATE commodities
SET commodity_de = 'Wein',
    commodity_es = 'Vino',
    commodity_fr = 'Vin',
    commodity_ru = 'Вино'
WHERE LOWER(commodity) = LOWER('Wine');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Witchhaul Kobe Beef');
UPDATE commodities
SET commodity_de = 'Witchhaul Kobe-Rindfleisch',
    commodity_es = 'Carne de buey kobe de Witchhaul',
    commodity_fr = 'Bœuf de Kobe de Witchhaul',
    commodity_ru = 'Мясо кобе с Вичхола'
WHERE LOWER(commodity) = LOWER('Witchhaul Kobe Beef');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Wolf Fesh');
UPDATE commodities
SET commodity_es = 'Jadeo del Lobo',
    commodity_fr = 'Wolf fesh',
    commodity_ru = 'Волчья дурь'
WHERE LOWER(commodity) = LOWER('Wolf Fesh');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Wreckage Components');
UPDATE commodities
SET commodity_de = 'Wrackteilkomponenten',
    commodity_es = 'Restos de accidentes',
    commodity_fr = 'Débris d’épave',
    commodity_ru = 'Обломки кораблекрушений'
WHERE LOWER(commodity) = LOWER('Wreckage Components');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Wulpa Hyperbore Systems');
UPDATE commodities
SET commodity_de = 'Wulpa-Superbohrer-Systeme',
    commodity_es = 'Sistemas de hipertaladros de Wulpa',
    commodity_fr = 'Système hyperbore Wulpa',
    commodity_ru = 'Гипербольные системы вульпа'
WHERE LOWER(commodity) = LOWER('Wulpa Hyperbore Systems');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Wuthielo Ku Froth');
UPDATE commodities
SET commodity_es = 'Espuma de Wuthielo Ku',
    commodity_fr = 'Mousse de Wuthielo Ku',
    commodity_ru = 'Пиво Ку с Вутхиело'
WHERE LOWER(commodity) = LOWER('Wuthielo Ku Froth');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Xihe Biomorphic Companions');
UPDATE commodities
SET commodity_es = 'Mascotas biomórficas de Xihe',
    commodity_fr = 'Compagnons biomorphiques de Xihe',
    commodity_ru = 'Биоморфные спутники Xihe'
WHERE LOWER(commodity) = LOWER('Xihe Biomorphic Companions');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Yaso Kondi Leaf');
UPDATE commodities
SET commodity_de = 'Yaso-Kondi-Blätter',
    commodity_es = 'Hojas de Yaso Kondi',
    commodity_fr = 'Feuille de Yaso Kondi',
    commodity_ru = 'Лист ясо Конди'
WHERE LOWER(commodity) = LOWER('Yaso Kondi Leaf');
INSERT OR IGNORE INTO commodities (commodity)
VALUES ('Zeessze Ant Grub Glue');
UPDATE commodities
SET commodity_de = 'Zeessze-Ameisenkleber',
    commodity_es = 'Pegamento de larvas de hormiga de Zeessze',
    commodity_fr = 'Colle de larve de fourmi de Zeessze',
    commodity_ru = 'Зесский муравьиный клей'
WHERE LOWER(commodity) = LOWER('Zeessze Ant Grub Glue');


-- Remove mining commodities that were incorrectly inserted into material_names
-- by migrations 00011, 00047, and 00059. These belong in commodities only.
DELETE
FROM material_names
WHERE LOWER(name) IN (
                      'alexandrite',
                      'bauxite',
                      'benitoite',
                      'bertrandite',
                      'bromellite',
                      'cobalt',
                      'coltan',
                      'gallite',
                      'grandidierite',
                      'hydrogen peroxide',
                      'indite',
                      'lepidolite',
                      'liquid oxygen',
                      'lithium hydroxide',
                      'low temperature diamonds',
                      'methane clathrate',
                      'methanol monohydrate crystals',
                      'monazite',
                      'musgravite',
                      'painite',
                      'platinum',
                      'praseodymium',
                      'rhodplumsite',
                      'rutile',
                      'samarium',
                      'serendibite',
                      'tritium',
                      'uraninite',
                      'void opal',
                      'water',
                      'gold',
                      'silver',
                      'osmium'
    );