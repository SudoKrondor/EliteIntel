ALTER TABLE game_session
    ADD COLUMN aiLanguage VARCHAR(10) NOT NULL DEFAULT 'EN';

-- Extend sub_system with machine_key for journal raw-key substring matching,
-- and locale label columns (es, fr, pt, ru) for V1.1 multi-language support.
--
-- machine_key is the identifying substring present in the journal's non-localised
-- Subsystem field after stripping the leading $ and trailing _name; suffix.
-- Lookup: SELECT subsystem FROM sub_system WHERE :strippedRawKey LIKE '%' || machine_key || '%'
--
-- Locale labels sourced from:
-- https://github.com/jixxed/ed-odyssey-materials-helper/blob/master/application/src/main/resources/locale/ships/modules.csv

ALTER TABLE sub_system
    ADD COLUMN machine_key TEXT;
ALTER TABLE sub_system
    ADD COLUMN label_es TEXT;
ALTER TABLE sub_system
    ADD COLUMN label_fr TEXT;
ALTER TABLE sub_system
    ADD COLUMN label_pt TEXT;
ALTER TABLE sub_system
    ADD COLUMN label_ru TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_sub_system_machine_key
    ON sub_system (machine_key) WHERE machine_key IS NOT NULL;

-- Internal modules
UPDATE sub_system
SET machine_key='ext_drive',
    label_es=NULL,
    label_fr=NULL,
    label_pt=NULL,
    label_ru=NULL
WHERE subsystem = 'Drive';
UPDATE sub_system
SET machine_key='int_powerplant',
    label_es='Núcleo de Energía',
    label_fr='Générateur',
    label_pt='Gerador de Energia',
    label_ru='Силовая установка'
WHERE subsystem = 'Power Plant';
UPDATE sub_system
SET machine_key='int_shieldgenerator',
    label_es='Generador de Escudos',
    label_fr='Générateur de bouclier',
    label_pt='Gerador de Escudo',
    label_ru='Щитогенератор'
WHERE subsystem = 'Shield Generator';
UPDATE sub_system
SET machine_key='int_hyperdrive',
    label_es='Motor de Distorsión',
    label_fr='Réacteur FSD',
    label_pt='Motor de Distorção de Fase',
    label_ru='Двигатель FSD'
WHERE subsystem = 'FSD';
UPDATE sub_system
SET machine_key='int_lifesupport',
    label_es='Soporte Vital',
    label_fr='Systèmes de survie',
    label_pt='Suporte de Vida',
    label_ru='Система жизнеобеспечения'
WHERE subsystem = 'Life Support';
UPDATE sub_system
SET machine_key='int_powerdistributor',
    label_es='Distribuidor de Energía',
    label_fr='Répartiteur de puissance',
    label_pt='Distribuidor de Energia',
    label_ru='Распределитель питания'
WHERE subsystem = 'Power Distributor';
UPDATE sub_system
SET machine_key='int_shieldcellbank',
    label_es='Acumulador de Escudos',
    label_fr='Réserve de cellules d''énergie',
    label_pt='Banco de Célula de Escudo',
    label_ru='Щитонакопитель'
WHERE subsystem = 'Shield Cell Bank';
UPDATE sub_system
SET machine_key='int_refinery',
    label_es='Refinería',
    label_fr='Raffinerie',
    label_pt='Refinaria',
    label_ru='Очиститель'
WHERE subsystem = 'Refinery';
UPDATE sub_system
SET machine_key='modularcargobaydoor',
    label_es='Compuerta de Bodega de Carga',
    label_fr=NULL,
    label_pt='Porta do compartimento de carga',
    label_ru='Люк грузового ковша'
WHERE subsystem = 'Cargo Hatch';
UPDATE sub_system
SET machine_key='dronecontrol_collection',
    label_es='Lanzador Dron Colector',
    label_fr='Contrôleur de drones collecteurs',
    label_pt='Controlador de Drone Coletor',
    label_ru='Контроллер дрона-сборщика'
WHERE subsystem = 'Collector';
UPDATE sub_system
SET machine_key='dronecontrol_resourcesiphon',
    label_es='Lanzador de Drones',
    label_fr=NULL,
    label_pt='Controlador de Drones',
    label_ru='Контроллер дронов'
WHERE subsystem = 'Hatch Breaker';

-- Hardpoints
UPDATE sub_system
SET machine_key='hpt_beamlaser',
    label_es='Láser de Rayo',
    label_fr='Faisceau laser',
    label_pt='Laser Contínuo',
    label_ru='Пучковый лазер'
WHERE subsystem = 'Beam Laser';
UPDATE sub_system
SET machine_key='hpt_cannon',
    label_es='Cañón',
    label_fr='Canon',
    label_pt='Canhão',
    label_ru='Орудие'
WHERE subsystem = 'Cannon';
UPDATE sub_system
SET machine_key='hpt_mininglaser',
    label_es='Láser de Minería',
    label_fr='Laser minier',
    label_pt='Laser de Mineração',
    label_ru='Проходочный лазер'
WHERE subsystem = 'Mining Laser';
UPDATE sub_system
SET machine_key='hpt_minelauncher',
    label_es='Lanzaminas',
    label_fr='Lance-mines',
    label_pt='Lança Minas',
    label_ru='Минирующее устройство'
WHERE subsystem = 'Mine Launcher';
UPDATE sub_system
SET machine_key='hpt_heatsinklauncher',
    label_es='Eyector Térmico',
    label_fr=NULL,
    label_pt='Dissipador Térmico',
    label_ru='Теплоотводная катапульта'
WHERE subsystem = 'Heatsink';
UPDATE sub_system
SET machine_key='hpt_plasmapointdefence',
    label_es='Defensa de Punto',
    label_fr='Tourelle de Défense Ponctuelle',
    label_pt='Defesa de Ponto',
    label_ru='Турель точечной обороны'
WHERE subsystem = 'Point Defence Turret';
UPDATE sub_system
SET machine_key='hpt_electroniccountermeasure',
    label_es='Contramedida Electrónica',
    label_fr='Contre-mesure électronique',
    label_pt='Contramedida Eletrônica',
    label_ru='Радиоэлектронное подавление'
WHERE subsystem = 'ECM';
UPDATE sub_system
SET machine_key='hpt_shieldbooster',
    label_es='Potenciador de Escudos',
    label_fr='Survolteur de bouclier',
    label_pt='Potencializador de Escudo',
    label_ru='Усилитель щита'
WHERE subsystem = 'Shield Booster';
UPDATE sub_system
SET machine_key='missilerack',
    label_es='Lanzamisiles',
    label_fr='Batterie de missiles',
    label_pt='Estante de Mísseis',
    label_ru='Блок ракет'
WHERE subsystem = 'Missile Rack';
UPDATE sub_system
SET machine_key='hpt_cargoscanner',
    label_es='Escáner de Manifiesto',
    label_fr='Scanner de cargo',
    label_pt='Scanner de Carga',
    label_ru='Сканер груза'
WHERE subsystem = 'Manifest Scanner';