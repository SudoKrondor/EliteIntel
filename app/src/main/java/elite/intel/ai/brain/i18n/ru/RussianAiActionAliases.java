package elite.intel.ai.brain.i18n.ru;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;
import elite.intel.session.Status;

import java.util.Map;
import java.util.Set;

import static elite.intel.ai.brain.actions.Commands.*;
import static elite.intel.ai.brain.actions.Queries.*;

public class RussianAiActionAliases implements AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("проснись", "слушай", "слушай меня", "активируйся");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("слушай меня", "слушай");
    }

    @Override
    public void addAliases(Map<String, String> map, Status status, boolean isDryRun) {
        // always available
        map.put("проснись, проснуться, слушай, слушай меня, активируйся, просыпайся", WAKEUP.getAction());
        map.put("спи, усни, иди спать, перейди в спящий режим, включи спящий режим, игнорируй меня, не слушай, не отслеживай, не слушай", SLEEP.getAction());
        map.put("перебей, прерви, останови речь, прекрати говорить, прервать, отставить, заткнись", INTERRUPT_TTS.getAction());

        // navigation
        map.put("отмени торговый маршрут, останови торговый маршрут, очисти торговый маршрут, прерви торговый маршрут, сбрось торговый маршрут", CANCEL_TRADE_ROUTE.getAction());
        map.put("лети к координатам {lat:X, lon:Y}, навигация к координатам {lat:X, lon:Y}, курс на координаты {lat:X, lon:Y}", NAVIGATE_TO_TARGET.getAction());
        map.put("навигация к активной миссии, проложи маршрут к активной миссии, проложи маршрут к миссии, веди к миссии, лети к миссии {key:X}", NAVIGATE_TO_NEXT_MISSION.getAction());
        map.put("удали все задания, удалить все задания, очистить задания", CLEAR_ALL_ACTIVE_MISSIONS.getAction());
        map.put("лети к авианосцу, навигация к авианосцу, курс к авианосцу, вернись к авианосцу, веди нас к авианосцу", NAVIGATE_TO_FLEET_CARRIER.getAction());
        map.put("навигация к зоне посадки, курс к зоне посадки, азимут к зоне посадки, обратно к зоне посадки", GET_HEADING_TO_LZ.getAction());
        map.put("навигация к следующей торговой остановке, лети к следующей торговой точке", NAVIGATE_TO_NEXT_TRADE_STOP.getAction());
        map.put("навигация из памяти, вставь из памяти, используй адрес из памяти", NAVIGATE_TO_ADDRESS_FROM_MEMORY.getAction());
        map.put("отмени навигацию, прерви навигацию, останови навигацию, сбрось маршрут", NAVIGATION_OFF.getAction());
        map.put("установи домашнюю систему, сделай текущую систему домашней, отметь домашнюю систему", SET_HOME_SYSTEM.getAction());
        map.put("веди домой, лети домой, навигация домой, вернись домой, проложи маршрут домой, курс домой", TAKE_ME_HOME.getAction());
        map.put("смотри вперёд, сбрось взгляд, сброс, смотреть вперёд, сбрось направление взгляда, взгляд по умолчанию, обзор по центру, вид по умолчанию, сбрось head look", RESET_HEAD_LOOK.getAction());

        if (status.isInMainShip() || isDryRun) {
            // navigation
            map.put("выбери пункт назначения FSD, установи цель FSD, выбери пункт назначения, установи пункт назначения, выбери цель маршрута", TARGET_DESTINATION.getAction());
            map.put("прыжок в гиперпространство, прыгай, гиперпрыжок, войти в гиперпространство, поехали, следующий маршрутный пункт", JUMP_TO_HYPERSPACE.getAction());
            map.put("выйти из суперкруиза, выйти из суперкруиза здесь, сбросить суперкруиз, выйти из сверхсвета, сбросься из суперкруиза здесь, дроп, выход", DROP_FROM_SUPER_CRUISE.getAction());
            map.put("войти в суперкруиз, суперкруиз, включи суперкруиз, световая скорость, на форсаж", ENTER_SUPER_CRUISE.getAction());
            map.put("запусти корабль, старт корабля, отстыковаться, покинуть станцию, выйти из порта", LAUNCH_SHIP.getAction());
            map.put("применить щитовую ячейку, запустить щитовую ячейку, активировать щитовую ячейку, банк щитовых ячеек, применить энергоячейку, использовать щитовую ячейку", DEPLOY_SHIELD_CELL.getAction());
            map.put("выпусти дипольные отражатели, сбрось дипольные отражатели, сбрось диполи, чаф, выбросить помехи, запустить помехи, скинь ловушки", DEPLOY_CHAFF.getAction());

            // speed / throttle
            map.put("останови двигатели, остановись, полный стоп, стоп корабль, заглуши двигатели, сбрось тягу, нулевая тяга, останови корабль", SET_SPEED_ZERO.getAction());
            map.put("такси к посадке, такси, автопосадка, автоматическая посадка, автопилот, такси", TAXI.getAction());
            map.put("четверть тяги, двадцать пять процентов, малая скорость, одна четверть, малый ход", SET_SPEED25.getAction());
            map.put("половина тяги, пятьдесят процентов, половина скорости, пол скорости", SET_SPEED50.getAction());
            map.put("три четверти тяги, семьдесят пять процентов, три четверти скорости", SET_SPEED75.getAction());
            map.put("полная тяга, сто процентов, полный ход, максимальная скорость, максимум тяги", SET_SPEED100.getAction());
            map.put("увеличь скорость на {key:X}, прибавь скорость на {key:X}", INCREASE_SPEED_BY.getAction());
            map.put("уменьши скорость на {key:X}, сбавь скорость на {key:X}", DECREASE_SPEED_BY.getAction());
            map.put("установи оптимальную скорость, оптимальная скорость подхода, оптимизируй скорость подхода", SET_OPTIMAL_SPEED.getAction());
            map.put("шасси, шасси вниз, выпусти шасси, опусти посадочное шасси", DEPLOY_LANDING_GEAR.getAction());
            map.put("убери шасси, шасси вверх, подними шасси, сложи посадочное шасси", RETRACT_LANDING_GEAR.getAction());
            map.put("запроси стыковку, стыковка со станцией, запроси посадку, запрос посадки, запроси парковку, попроси площадку, запроси площадку", REQUEST_DOCKING.getAction());

            // UI panels
            map.put("покажи панель истребителя, открой панель истребителя, отобрази панель истребителя", SHOW_FIGHTER_PANEL.getAction());

            // combat
            map.put("боевая готовность, к бою, выпусти оружие, оружие к бою, боевой режим, оружие свободно, вооружиться, подготовить оружие, выпусти хардпойнты, разверни хардпойнты, развернуть орудия", DEPLOY_HARDPOINTS.getAction());
            map.put("убери оружие, оружие убрать, снять оружие, выйти из боя, спрячь оружие, безопасный режим оружия, убери хардпойнты, сложи хардпойнты, отбой", RETRACT_HARDPOINTS.getAction());
            map.put("цель FSD {key:fsd}, цель двигатели {key:drive}, цель распределитель энергии {key:power distributor}, цель силовая установка {key:powerplant}, цель жизнеобеспечение {key:life support}", TARGET_SUB_SYSTEM.getAction());
            map.put("цель ведомый один, ведомый альфа", TARGET_WINGMAN0.getAction());
            map.put("цель ведомый два, ведомый браво", TARGET_WINGMAN1.getAction());
            map.put("цель ведомый три, ведомый чарли", TARGET_WINGMAN2.getAction());
            map.put("навигационная привязка крыла, привязка к ведомому, следовать за ведомым", WING_NAV_LOCK.getAction());
            map.put("приоритетная цель, цель с наибольшей угрозой, самая опасная цель, выбрать врага, следующий враг", SELECT_HIGHEST_THREAT.getAction());

            // vehicle deployment
            map.put("выпусти срв, запусти срв, выпусти транспорт, разверни SRV, высади срв", DEPLOY_SRV.getAction());
            map.put("выпусти теплоотвод, запусти теплоотвод, сбрось тепло, сброс тепла", DEPLOY_HEAT_SINK.getAction());

            // fighter orders
            map.put("выпусти истребитель, запусти истребитель, отправь истребитель", DEPLOY_FIGHTER.getAction());
            map.put("истребитель защищай корабль, истребитель оборона, приказ истребителю обороняться", FIGHTER_REQUEST_DEFENSIVE_BEHAVIOUR.getAction());
            map.put("истребитель атакуй мою цель, истребитель атаковать, фокус на цель, сосредоточься на цели", FIGHTER_REQUEST_FOCUS_TARGET.getAction());
            map.put("истребитель прекратить огонь, истребитель не стрелять, не открывать огонь", FIGHTER_REQUEST_HOLD_FIRE.getAction());
            map.put("истребитель вернись на корабль, истребитель стыковка, отозвать истребитель", FIGHTER_REQUEST_REQUEST_DOCK.getAction());
            map.put("истребитель свободный огонь, огонь по усмотрению, атаковать по усмотрению, атакуй самостоятельно, открыть огонь", FIGHTER_OPEN_ORDERS.getAction());
            map.put("выбери огневую группу {key:X}, огневая группа {key:X}, переключись на огневую группу {key:X}", SELECT_FIRE_GROUP_BY_NATO.getAction());
            map.put("рассчитать нейтронной маршрут {efficiency:X}", CALCULATE_NEUTRON_STAR_ROUTE.getAction());
            map.put("следующий прыжок нейтронной звезды, прокладка маршрута до следующей точки нейтронной звезды, следующая нейтронная звезда", PLOT_ROUTE_TO_NEXT_NEUTRON_STAR.getAction());
            map.put("очистить/удалить маршрут нейтронной звезды", CLEAR_NEUTRON_ROUTE.getAction());
        }

        if (status.isInMainShip() && !status.isDocked() || isDryRun) {
            map.put("станции в системе, какие станции, ближайшие станции, космопорты, космические станции, доступна ли стыковка", QUERY_STATIONS.getAction());
        }

        if (status.isInSrv() && status.isDocked() || isDryRun) {
            map.put("покажи панель сервисов, открой панель сервисов, отобрази панель сервисов станции", SHOW_STATION_SERVICES.getAction());
        }

        if (status.isInMainShip() || status.isInSrv() || isDryRun) {
            // flight / ship systems
            map.put("переключись в боевой режим, включи боевой режим, боевой", ACTIVATE_COMBAT_MODE.getAction());
            map.put("переключись в режим анализа, включи режим анализа", ACTIVATE_ANALYSIS_MODE.getAction());
            map.put("грузовой люк, грузозаборник, открой грузовой люк, закрой грузовой люк, выпусти грузозаборник, убери грузозаборник, cargo scoop", TOGGLE_CARGO_SCOOP.getAction());
            map.put("ночное видение, включи ночное видение, выключи ночное видение {state:true/false}", NIGHT_VISION_ON_OFF.getAction());
            map.put("фары, корабельный свет, выключи свет, включи свет, свет включить, свет выключить {state:true/false}", LIGHTS_ON_OFF.getAction());

            // UI panels
            map.put("покажи панель командира, открой центральную панель, открой панель ролей, открой планшет", SHOW_COMMANDER_PANEL.getAction());
            map.put("покажи панель экипажа, открой панель экипажа", SHOW_CREW.getAction());
            map.put("покажи домашнюю панель, открой внутреннюю панель", SHOW_INTERNAL_PANEL.getAction());
            map.put("покажи панель модулей, открой модули", SHOW_MODULES_PANEL.getAction());
            map.put("покажи огневые группы, открой огневые группы", SHOW_FIRE_GROUPS.getAction());
            map.put("покажи инвентарь, открой инвентарь, панель инвентаря", SHOW_INVENTORY_PANEL.getAction());
            map.put("покажи хранилище, открой склад, открой хранилище", SHOW_STORAGE_PANEL.getAction());

            // power
            map.put("энергию на щиты, максимум щитов, усилить щиты", INCREASE_SHIELDS_POWER.getAction());
            map.put("энергию на двигатели, максимум двигателей, усилить двигатели", INCREASE_ENGINES_POWER.getAction());
            map.put("энергию на оружие, максимум оружия, усилить оружие", INCREASE_WEAPONS_POWER.getAction());

            // vehicle deployment
            map.put("выйти из корабля, высадиться, покинуть корабль", DISEMBARK.getAction());
            map.put("сбалансируй энергию, баланс энергии, сбрось распределение энергии, распределить энергию поровну, уравновесить питание, сброс питания", RESET_POWER.getAction());
        }

        if (status.isInSrv() || isDryRun) {
            map.put("помощь вождения, ассистент вождения, ассистент срв {state:true/false}", DRIVE_ASSIST.getAction());
            map.put("вернуть срв, на борт корабля, вернуться в корабль, забрать срв, стыковка срв", RECOVER_SRV.getAction());
        }

        if (status.isInSrv() || status.isOnFoot() || isDryRun) {
            map.put("отправь корабль, отправь корабль на орбиту, корабль на орбиту, убери корабль с поверхности, отпусти корабль, свободен, вольно", DISMISS_SHIP.getAction());
            map.put("верни корабль на поверхность, забери меня, вызови корабль, посади корабль рядом", RETURN_TO_SURFACE.getAction());
        }

        // market / traders / brokers
        map.put("найди торговца сырьевыми материалами, сырьевой торговец, где обменять сырьевые материалы {key:X}", FIND_RAW_MATERIAL_TRADER.getAction());
        map.put("найди торговца закодированными материалами, торговец данными, где обменять данные {key:X}", FIND_ENCODED_MATERIAL_TRADER.getAction());
        map.put("найди торговца производственными материалами, торговец производственными материалами, где обменять производственные материалы {key:X}, manufactured materials", FIND_MANUFACTURED_MATERIAL_TRADER.getAction());
        map.put("найди человеческого техноброкера, человеческий технологический брокер {key:X}", FIND_HUMAN_TECHNOLOGY_BROKER.getAction());
        map.put("найди техноброкера Стражей, технологический брокер Стражей {key:X}, Guardian tech broker", FIND_GUARDIAN_TECHNOLOGY_BROKER.getAction());
        map.put("найди товар, найди ближайший товар, купить товар, где купить товар, найди рынок {key:X, max_distance:Y, state:true/false}", FIND_COMMODITY.getAction());
        map.put("найди ближайший авианосец, ближайший флит-кэрриер, ближайший fleet carrier", FIND_NEAREST_FLEET_CARRIER.getAction());

        // fleet carrier
        map.put("установи резерв топлива авианосца, резерв трития авианосца {key:X}", SET_CARRIER_FUEL_RESERVE.getAction());
        map.put("рассчитай маршрут авианосца, спланируй маршрут авианосца, маршрут прыжков авианосца", CALCULATE_FLEET_CARRIER_ROUTE.getAction());
        map.put("введи пункт назначения авианосца, установи пункт назначения авианосца, направление авианосца", ENTER_FLEET_CARRIER_DESTINATION.getAction());

        // squadron carrier
        map.put("маршрут авианосца эскадрильи, навигация авианосца эскадрильи, маршрут прыжков авианосца эскадрильи, сколько прыжков на маршруте авианосца эскадрильи, прыжков осталось у авианосца эскадрильи", SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("куда летит авианосец эскадрильи, пункт назначения авианосца эскадрильи, конечная точка авианосца эскадрильи, курс авианосца эскадрильи", SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
        map.put("тритий авианосца эскадрильи, топливо авианосца эскадрильи, запасы трития эскадрильи, уровень топлива авианосца эскадрильи, статус трития авианосца эскадрильи", SQUADRON_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("статус авианосца эскадрильи, финансы авианосца эскадрильи, баланс авианосца эскадрильи, обзор авианосца эскадрильи, средства авианосца эскадрильи, сколько авианосец эскадрильи может работать, статус топлива авианосца эскадрильи", SQUADRON_CARRIER_STATUS.getAction());
        map.put("eta авианосца эскадрильи, когда прибудет авианосец эскадрильи, сколько до прибытия авианосца эскадрильи, время прибытия авианосца эскадрильи, прибытие авианосца эскадрильи", SQUADRON_CARRIER_ETA.getAction());

        // trade
        map.put("рассчитай торговый маршрут, торговый маршрут", CALCULATE_TRADE_ROUTE.getAction());
        map.put("покажи параметры торгового маршрута, список параметров торговли", LIST_TRADE_ROUTE_PARAMETERS.getAction());
        map.put("монетизируй маршрут, оцени прибыль маршрута", MONETIZE_ROUTE.getAction());

        map.put("измени стартовый бюджет торгового профиля {key:X}", CHANGE_TRADE_PROFILE_SET_STARTING_BUDGET.getAction());
        map.put("измени максимальное число остановок торгового профиля {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_NUMBER_OF_STOPS.getAction());
        map.put("измени максимальное расстояние торгового профиля {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_DISTANCE_FROM_ENTRY.getAction());
        map.put("разрешить запрещенные товары в торговом профиле {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PROHIBITED_CARGO.getAction());
        map.put("разрешить планетарные порты в торговом профиле {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PLANETARY_PORT.getAction());
        map.put("разрешить системы с разрешением в торговом профиле {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PERMIT_SYSTEMS.getAction());
        map.put("разрешить опорные системы в торговом профиле {state:true/false}, разрешить strongholds в торговом профиле {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_STRONGHOLDS.getAction());

        // announcements / app settings
        map.put("переключи радио, радиотрафик, радиопередачи {state:true/false}", SET_RADIO_TRANSMISSION_MODE.getAction());
        map.put("объявления радарных контактов {state:true/false}", SET_RADAR_CONTACT_ANNOUNCEMENT.getAction());
        map.put("объявления открытий {state:true/false}", DISCOVERY_ON_OFF.getAction());
        map.put("объявления маршрута {state:true/false}", ROUTE_ON_OFF.getAction());
        map.put("переключить все объявления {state:true/false}, все объявления {state:true/false}", TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        map.put("очисти напоминания, сбрось напоминания", CLEAR_REMINDERS.getAction());
        map.put("установи напоминание {key:X}, напомни {key:X}", SET_REMINDER.getAction());
        map.put("напомни мне через {minutes:X} минут {key:Y}, таймер на {minutes:X} минут {key:Y}, напоминание через {minutes:X} минут {key:Y}", SET_TIMED_REMINDER.getAction());
        map.put("исследуй систему, сканируй систему, отсканируй систему, отсканируй систему", HONK_THE_SYSTEM.getAction());

        // UI panels
        map.put("активировать выбранное, активируй выбранное, подтверди выбор, нажми активную кнопку", ACTIVATE.getAction());
        map.put("покажи панель транзакций, открой транзакции", SHOW_TRANSACTIONS.getAction());
        map.put("покажи панель контактов, открой контакты", SHOW_CONTACTS.getAction());
        map.put("покажи панель навигации, открой навигацию", SHOW_NAVIGATION.getAction());
        map.put("покажи чат, открой чат, панель связи, коммс панель", SHOW_CHAT_PANEL.getAction());
        map.put("покажи почту, открой входящие, email", SHOW_INBOX_PANEL.getAction());
        map.put("покажи социальную панель, открой социальную панель", SHOW_SOCIAL_PANEL.getAction());
        map.put("покажи историю, открой историю", SHOW_HISTORY_PANEL.getAction());
        map.put("покажи эскадрилью, открой эскадрилью", SHOW_SQUADRON.getAction());
        map.put("покажи статус, открой панель статуса", SHOW_STATUS_PANEL.getAction());
        map.put("открой управление авианосцем, панель управления авианосцем, открой carrier management", DISPLAY_CARRIER_MANAGEMENT.getAction());
        map.put("открой карту галактики, покажи карту галактики", OPEN_GALAXY_MAP.getAction());
        map.put("открой карту системы, покажи локальную карту, системная карта", OPEN_SYSTEM_MAP.getAction());
        map.put("закрывай, закрой, закрыть панель, закрыть, назад, закрой меню, выйти из панели, закрыть панель", EXIT_CLOSE.getAction());

        // pirate massacre missions
        map.put("навигация к системе выдачи миссий, курс к системе провайдера миссий", RECON_PROVIDER_SYSTEM.getAction());
        map.put("навигация к пиратскому провайдеру миссий, лети к выдаче пиратских миссий", NAVIGATE_TO_PIRATE_MISSION_PROVIDER.getAction());
        map.put("активные миссии, текущие миссии, журнал миссий, какие миссии, статус миссий, наши миссии", ANALYZE_MISSIONS.getAction());
        map.put("пиратская миссия, счёт убийств, сколько убийств, прогресс миссии на уничтожение, прогресс massacre mission, сколько пиратов осталось", PIRATE_MISSION_PROGRESS.getAction());
        map.put("найди охотничьи угодья {key:X}, найди место охоты {key:X}", FIND_HUNTING_GROUNDS.getAction());
        map.put("разведка охотничьих угодий, навигация к целевой системе, курс к системе охоты", RECON_TARGET_SYSTEM.getAction());
        map.put("игнорируй охотничьи угодья, пропусти место охоты", IGNORE_HUNTING_GROUND.getAction());
        map.put("подтверди охотничьи угодья, подтверди целевую систему", CONFIRM_HUNTING_GROUND.getAction());

        // science / mining / biology
        map.put("добавь цель добычи {key:X}", ADD_MINING_TARGET.getAction());
        map.put("удали цель добычи {key:X}", REMOVE_MINING_TARGET.getAction());
        map.put("очисти цели добычи", CLEAR_MINING_TARGETS.getAction());
        map.put("объявления добычи {state:true/false}", MINING_ON_OFF.getAction());
        map.put("найди мозговые деревья {key:X, max_distance:Y}", FIND_BRAIN_TREES.getAction());
        map.put("найди место добычи, найди точку добычи, горячая точка добычи, хотспот, mining hotspot, где майнить, найди астероидное поле {key:X, max_distance:Y}", FIND_MINING_SITE.getAction());
        map.put("найди место добычи трития, найди тритиевое поле {key:X, max_distance:Y}", FIND_FLEET_CARRIER_FUEL_MINING_SITE.getAction());
        map.put("навигация к следующему биосэмплу, к следующему образцу, к следующей органике, к следующей записи кодекса", NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
        map.put("открой FSS, полноспектральное сканирование, FSS, открой скан, полный скан, ФСС", OPEN_FSS.getAction());
        map.put("найди ближайшую vista genomics, найди геномику", FIND_VISTA_GENOMICS.getAction());
        map.put("удали запись кодекса, удали этот кодекс, удали эту запись, удали эту органику", DELETE_CODEX_ENTRY.getAction());

        map.put("проверь бинды, отсутствующие клавиши, неназначенные клавиши, клавиатурные привязки, проверка клавиш", KEY_BINDINGS_ANALYSIS.getAction());
        map.put("биосигналы в системе выполнены, органика просканирована в системе, сколько биосэмплов в системе, биосигналы в системе, какие планеты имеют биосигналы, какие планеты еще нужно сканировать", BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        map.put("экзобиологические образцы, Какие биосканирования мы завершили?, биологические образцы, органика в локации, какие организмы, что осталось сканировать, оставшиеся организмы, прогресс экзобиологии, что просканировано здесь, какая органика на этой планете", EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        map.put("расстояние до последнего биосэмпла, как далеко до образца, расстояние до последней органики, дистанция до биосэмпла, навигация к биосэмплу", DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
        map.put("анализ биома, какой биом {key:X}, планетарный биом, анализ атмосферы, какая жизнь здесь, тип биома", PLANET_BIOME_ANALYSIS.getAction());
        map.put("звездные объекты, планеты в системе, посадочные планеты, планета или луна пригодна для посадки, тела в системе, какие планеты, сколько планет, кольца, ледяные кольца", QUERY_STELLAR_OBJETS.getAction());
        map.put("сигналы в системе, какие сигналы в системе, обнаруженные сигналы, сигналы FSS, горячие точки добычи, зоны добычи ресурсов, RES, конфликтные зоны, эмиссии, Что есть в этой системе?", QUERY_STELLAR_SIGNALS.getAction());
        map.put("геосигналы, геологические сигналы, вулканические сигналы, геологическая активность, вулканическая активность", QUERY_GEO_SIGNALS.getAction());

        map.put("авианосцы в системе, carriers в системе, сколько авианосцев, есть ли авианосцы рядом", QUERY_CARRIERS.getAction());
        map.put("анализ маршрута авианосца, навигация авианосца, путь авианосца, сколько прыжков на маршруте авианосца, сколько прыжков осталось у авианосца, прыжков осталось", FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("куда летит авианосец, следующий прыжок авианосца, конечная точка авианосца, пункт назначения авианосца", FLEET_CARRIER_ROUTE.getAction());
        map.put("тритий авианосца, топливо авианосца, сколько трития на авианосце, уровень трития, резерв трития", FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("статус авианосца, финансы авианосца, баланс авианосца, обзор авианосца, средства авианосца, сколько авианосец может работать, запас хода авианосца", FLEET_CARRIER_STATUS.getAction());
        map.put("eta авианосца, когда прибудет авианосец, сколько до прибытия авианосца, время прыжка авианосца", FLEET_CARRIER_ETA.getAction());
        map.put("расстояние до авианосца, где наш авианосец, как далеко авианосец, дистанция до авианосца", DISTANCE_TO_CARRIER.getAction());
        map.put("безопасность системы, контроль фракции, кто контролирует систему, уровень безопасности, владелец системы, доминирующая фракция", SYSTEM_SECURITY_ANALYSIS.getAction());
        map.put("торговый профиль, торговые настройки, параметры торговли, торговая конфигурация, критерии торговли", TRADE_PROFILE_ANALYSIS.getAction());
        map.put("расстояние до звездного объекта, как далеко до тела, расстояние до планеты {key:X}, расстояние до луны, дистанция до тела, расстояние до объекта", DISTANCE_TO_BODY.getAction());
        map.put("последний скан, что мы сканировали, последний просканированный объект, самый свежий скан, анализ последнего сканирование", LAST_SCAN_ANALYSIS.getAction());
        map.put("инвентарь материалов {key:X}, сколько предметов {key:X}, сколько материалов {key:X}, есть ли материал {key:X}, сколько {key:X} у нас есть, инженерный материал {key:X}, сырьевой материал {key:X}, производственный материал {key:X}, manufactured material {key:X}, закодированный материал {key:X}", MATERIALS_INVENTORY.getAction());
        map.put("материалы планеты, материалы здесь, какие материалы на этой планете, поверхностные материалы, залежи материалов, минералы на планете", PLANET_MATERIALS.getAction());
        map.put("прибыль исследования, стоимость открытий, сколько стоит исследование, ценность сканов, прибыль картографирования, ценность экзобиологии", EXPLORATION_PROFITS.getAction());
        map.put("в какой системе я нахожусь, в какой системе мы находимся, какая текущая система, где мой корабль, где мы сейчас, наше местоположение, длительность дня, информация о месте нахождения", CURRENT_LOCATION.getAction());
        map.put("информация о цели fsd, анализ пункта назначения, какую звезду мы выбрали, анализ fsd цели, цель фсд", FSD_TARGET_ANALYSIS.getAction());
        map.put("проложенный маршрут, топливо на следующей остановке, наличие топлива на маршруте, анализ маршрута, мы уже на месте, текущий маршрут, сколько прыжков осталось, следующая звезда scoopable", PLOTTED_ROUTE_ANALYSIS.getAction());
        map.put("торговый маршрут, текущий торговый план, чем мы торгуем, наш торговый план, торговые этапы", TRADE_ROUTE_ANALYSIS.getAction());
        map.put("оснащение, улучшения корабля, доступные модули, какие модули на станции, доступное оборудование, купить модули, части корабля", LOCAL_OUTFITTING.getAction());
        map.put("верфь, корабли на продажу, какие корабли на станции, купить корабль, доступные корабли, новый корабль", LOCAL_SHIPYARD.getAction());
        map.put("что в грузовом отсеке, что мы везем, содержимое груза, товары на борту, чем мы загружены, грузовой отсек, что есть в грузовом отсеке", CARGO_HOLD_CONTENTS.getAction());
        map.put("профиль игрока", PLAYER_PROFILE_ANALYSIS.getAction());
        map.put("снаряжение, есть ли на борту X, компоновка корабля, отчет о повреждениях, модули корабля, готовность к бою, оборудование корабля, характеристики корабля, на чем я лечу, что установлено, генератор щитов, усиление корпуса, сенсоры, двигатели, FSD, Frame Shift Drive, топливозаборник", SHIP_LOADOUT.getAction());
        map.put("детали станции, какие сервисы здесь, какие услуги здесь, сервисы на станции, что предлагает станция, информация о станции, сервисы станции, что есть на этой станции, доступные сервисы", STATION_DETAILS.getAction());
        map.put("на какой станции я нахожусь, на какой станции мы находимся, где я пристыкован, где мы пристыкованы, какая моя текущая станция, какая у нас текущая станция", STATION_DETAILS.getAction());
        map.put("награды за головы, общие баунти, сколько наград за головы, награды за розыск, заработок с баунти, bounty", TOTAL_BOUNTIES.getAction());
        map.put("расстояние до Пузыря, расстояние до Sol, расстояние до Сола, расстояние до Земли, как далеко от населенного космоса, как далеко от Пузыря, как далеко от цивилизации, расстояние от населенного космоса", DISTANCE_TO_BUBBLE.getAction());
        map.put("текущее время, сколько времени, земное время, галактическое время, UTC время, реальное время, который час", TIME_IN_ZONE.getAction());
        map.put("напоминание, какое было напоминание, напоминание пункта назначения, есть ли напоминания, что мы установили как напоминание", REMINDER.getAction());
        map.put("локальные рынки, рынки на станциях и поселениях, рынки на аванпостах в системе", ANALYZE_MARKETS.getAction());
    }
}
