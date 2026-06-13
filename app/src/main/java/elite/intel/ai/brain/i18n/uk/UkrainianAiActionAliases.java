package elite.intel.ai.brain.i18n.uk;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;
import elite.intel.session.Status;

import java.util.Map;
import java.util.Set;

import static elite.intel.ai.brain.actions.Commands.*;
import static elite.intel.ai.brain.actions.Queries.*;

public class UkrainianAiActionAliases implements AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("прокинься", "слухай", "слухай мене", "активуйся");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("слухай мене", "слухай");
    }

    @Override
    public void addAliases(Map<String, String> map, Status status, boolean isDryRun) {
        // always available
        map.put("прокинься, слухай, слухай мене, активуйся", WAKEUP.getAction());
        map.put("спи, засни, йди спати, ігноруй мене, не слухай, не відстежуй", SLEEP.getAction());
        map.put("перебий, перерви, зупини мову, припини говорити", INTERRUPT_TTS.getAction());

        // navigation
        map.put("скасуй торговий маршрут, зупини торговий маршрут, очисти торговий маршрут, перерви торговий маршрут, скинь торговий маршрут", CANCEL_TRADE_ROUTE.getAction());
        map.put("лети до координат {lat:X, lon:Y}, навігація до координат {lat:X, lon:Y}, курс на координати {lat:X, lon:Y}", NAVIGATE_TO_TARGET.getAction());
        map.put("навігація до активної місії, проклади маршрут до активної місії, проклади маршрут до місії, веди до місії, лети до місії {key:X}", NAVIGATE_TO_NEXT_MISSION.getAction());
        map.put("лети до авіаносця, навігація до авіаносця, курс до авіаносця, повернись до авіаносця, веди нас до авіаносця", NAVIGATE_TO_FLEET_CARRIER.getAction());
        map.put("навігація до зони посадки, курс до зони посадки, азимут до зони посадки, назад до зони посадки", GET_HEADING_TO_LZ.getAction());
        map.put("навігація до наступної торгової зупинки, лети до наступної торгової точки", NAVIGATE_TO_NEXT_TRADE_STOP.getAction());
        map.put("навігація з пам'яті, встав із пам'яті, використай адресу з пам'яті", NAVIGATE_TO_ADDRESS_FROM_MEMORY.getAction());
        map.put("скасуй навігацію, перерви навігацію, зупини навігацію, скинь маршрут", NAVIGATION_OFF.getAction());
        map.put("встанови домашню систему, зроби поточну систему домашньою, познач домашню систему", SET_HOME_SYSTEM.getAction());
        map.put("веди додому, лети додому, навігація додому, повернись додому, проклади маршрут додому, курс додому", TAKE_ME_HOME.getAction());
        map.put("скинь погляд, скинь напрям погляду, погляд за замовчуванням, вид за замовчуванням, скинь head look", RESET_HEAD_LOOK.getAction());

        if (status.isInMainShip() || isDryRun) {
            // navigation
            map.put("вибери fsd пункт призначення, вибери пункт призначення, встанови пункт призначення, вибери ціль маршруту", TARGET_DESTINATION.getAction());
            map.put("стрибок у гіперпростір, стрибай, гіперстрибок, увійти в гіперпростір, поїхали, наступна точка маршруту", JUMP_TO_HYPERSPACE.getAction());
            map.put("вийти із суперкруїзу, вийди тут, скинути суперкруїз, вийти зі надсвітла, скинься тут", DROP_FROM_SUPER_CRUISE.getAction());
            map.put("увійти в суперкруїз, суперкруїз, увімкни суперкруїз", ENTER_SUPER_CRUISE.getAction());
            map.put("запусти корабель, старт, відстикуватись, покинути станцію, вийти з порту", LAUNCH_SHIP.getAction());
            map.put("застосувати щитову комірку, запустити щитову комірку, активувати щитову комірку, банк щитових комірок, застосувати енергетичну комірку, використати щитову комірку", DEPLOY_SHIELD_CELL.getAction());
            map.put("запустити пастки, використати пастки, випустити пастки, активувати пастки, запустити теплові пастки, скинути пастки", DEPLOY_CHAFF.getAction());

            // speed / throttle
            map.put("зупини двигуни, стоп, повний стоп, зупинись, заглуши двигуни, скинь тягу, нульова тяга, зупини корабель", SET_SPEED_ZERO.getAction());
            map.put("таксі до посадки, таксі, автопосадка, автоматична посадка", TAXI.getAction());
            map.put("чверть тяги, двадцять п'ять відсотків, мала швидкість, одна чверть", SET_SPEED25.getAction());
            map.put("половина тяги, п'ятдесят відсотків, половина швидкості", SET_SPEED50.getAction());
            map.put("три чверті тяги, сімдесят п'ять відсотків, три чверті швидкості", SET_SPEED75.getAction());
            map.put("повна тяга, сто відсотків, повний хід, максимальна швидкість, максимум тяги", SET_SPEED100.getAction());
            map.put("збільш швидкість на {key:X}, додай швидкість на {key:X}", INCREASE_SPEED_BY.getAction());
            map.put("зменш швидкість на {key:X}, знизь швидкість на {key:X}", DECREASE_SPEED_BY.getAction());
            map.put("встанови оптимальну швидкість, оптимальна швидкість підходу, оптимізуй швидкість підходу", SET_OPTIMAL_SPEED.getAction());
            map.put("шасі, шасі вниз, випусти шасі, опусти посадкове шасі", DEPLOY_LANDING_GEAR.getAction());
            map.put("прибери шасі, шасі вгору, підніми шасі, склади посадкове шасі", RETRACT_LANDING_GEAR.getAction());
            map.put("запроси стикування, стикування зі станцією, запроси посадку, запит посадки, попроси паркування, запроси майданчик", REQUEST_DOCKING.getAction());

            // UI panels
            map.put("покажи панель винищувача, відкрий панель винищувача, відобрази панель винищувача", SHOW_FIGHTER_PANEL.getAction());

            // combat
            map.put("випусти зброю, зброю до бою, бойова готовність, зброя вільна, озброїтись, підготувати зброю", DEPLOY_HARDPOINTS.getAction());
            map.put("прибери зброю, зброю прибрати, сховай зброю, вийти з бою, безпечний режим зброї", RETRACT_HARDPOINTS.getAction());
            map.put("ціль fsd {key:fsd}, ціль двигуни {key:drive}, ціль розподільник живлення {key:power distributor}, ціль силова установка {key:powerplant}, ціль життєзабезпечення {key:life support}", TARGET_SUB_SYSTEM.getAction());
            map.put("ціль напарник один, напарник альфа", TARGET_WINGMAN0.getAction());
            map.put("ціль напарник два, напарник браво", TARGET_WINGMAN1.getAction());
            map.put("ціль напарник три, напарник чарлі", TARGET_WINGMAN2.getAction());
            map.put("навігаційна прив'язка крила, прив'язка до напарника, слідувати за напарником", WING_NAV_LOCK.getAction());
            map.put("пріоритетна ціль, ціль з найбільшою загрозою, найнебезпечніша ціль, вибрати ворога, наступний ворог", SELECT_HIGHEST_THREAT.getAction());

            // vehicle deployment
            map.put("випусти срв, запусти срв, випусти транспорт, висади срв", DEPLOY_SRV.getAction());
            map.put("випусти тепловідвід, запусти тепловідвід, скинь тепло", DEPLOY_HEAT_SINK.getAction());

            // fighter orders
            map.put("випусти винищувач, запусти винищувач, відправ винищувач", DEPLOY_FIGHTER.getAction());
            map.put("винищувач захищай корабель, винищувач оборона, наказ винищувачу оборонятись", FIGHTER_REQUEST_DEFENSIVE_BEHAVIOUR.getAction());
            map.put("винищувач атакуй мою ціль, винищувач атакувати, фокус на ціль, зосередься на цілі", FIGHTER_REQUEST_FOCUS_TARGET.getAction());
            map.put("винищувач припинити вогонь, винищувач не стріляти, тримати вогонь", FIGHTER_REQUEST_HOLD_FIRE.getAction());
            map.put("винищувач повернись на корабель, винищувач стикування, відкликати винищувач", FIGHTER_REQUEST_REQUEST_DOCK.getAction());
            map.put("винищувач вільний вогонь, вогонь за готовністю, атакувати на власний розсуд", FIGHTER_OPEN_ORDERS.getAction());
            map.put("вибери вогневу групу {key:X}, вогнева група {key:X}, перемкнись на вогневу групу {key:X}", SELECT_FIRE_GROUP_BY_NATO.getAction());
            map.put("розрахувати нейтронний маршрут {efficiency:X}", CALCULATE_NEUTRON_STAR_ROUTE.getAction());
            map.put("наступний стрибок нейтронної зірки, прокласти маршрут до наступної маршрутної точки нейтронної зірки, наступна нейтронна зірка", PLOT_ROUTE_TO_NEXT_NEUTRON_STAR.getAction());
            map.put("очистити/видалити маршрут нейтронної зірки", CLEAR_NEUTRON_ROUTE.getAction());
        }

        if (status.isInMainShip() && !status.isDocked() || isDryRun) {
            map.put("станції в системі, які станції, найближчі станції, космопорти, космічні станції, чи доступне стикування", QUERY_STATIONS.getAction());
        }

        if (status.isInSrv() && status.isDocked() || isDryRun) {
            map.put("покажи панель сервісів, відкрий панель сервісів, відобрази панель сервісів станції", SHOW_STATION_SERVICES.getAction());
        }

        if (status.isInMainShip() || status.isInSrv() || isDryRun) {
            // flight / ship systems
            map.put("перемкнись у бойовий режим, увімкни бойовий режим", ACTIVATE_COMBAT_MODE.getAction());
            map.put("перемкнись у режим аналізу, увімкни режим аналізу", ACTIVATE_ANALYSIS_MODE.getAction());
            map.put("вантажний ківш, відкрий вантажний ківш, закрий вантажний ківш, випусти вантажний ківш, прибери вантажний ківш, відкрий вантажний люк, закрий вантажний люк", TOGGLE_CARGO_SCOOP.getAction());
            map.put("нічне бачення, увімкни нічне бачення, вимкни нічне бачення {state:true/false}", NIGHT_VISION_ON_OFF.getAction());
            map.put("фари, світло, вимкни світло, увімкни світло, корабельне світло, світло увімкнути, світло вимкнути {state:true/false}", LIGHTS_ON_OFF.getAction());

            // UI panels
            map.put("покажи панель командира, відкрий центральну панель, відкрий панель ролей, відкрий планшет", SHOW_COMMANDER_PANEL.getAction());
            map.put("покажи панель екіпажу, відкрий панель екіпажу", SHOW_CREW.getAction());
            map.put("покажи домашню панель, відкрий внутрішню панель", SHOW_INTERNAL_PANEL.getAction());
            map.put("покажи панель модулів, відкрий модулі", SHOW_MODULES_PANEL.getAction());
            map.put("покажи вогневі групи, відкрий вогневі групи", SHOW_FIRE_GROUPS.getAction());
            map.put("покажи інвентар, відкрий інвентар", SHOW_INVENTORY_PANEL.getAction());
            map.put("покажи сховище, відкрий склад, відкрий сховище", SHOW_STORAGE_PANEL.getAction());

            // power
            map.put("енергію на щити, максимум щитів, посилити щити", INCREASE_SHIELDS_POWER.getAction());
            map.put("енергію на двигуни, максимум двигунів, посилити двигуни", INCREASE_ENGINES_POWER.getAction());
            map.put("енергію на зброю, максимум зброї, посилити зброю", INCREASE_WEAPONS_POWER.getAction());

            // vehicle deployment
            map.put("вийти з корабля, висадитися, покинути корабель", DISEMBARK.getAction());
            map.put("збалансуй енергію, баланс енергії, скинь розподіл енергії, розподілити енергію порівну", RESET_POWER.getAction());
        }

        if (status.isInSrv() || isDryRun) {
            map.put("допомога керування, асистент керування, асистент срв {state:true/false}", DRIVE_ASSIST.getAction());
            map.put("повернути срв, на борт корабля, повернутися в корабель, забрати срв, стикування срв", RECOVER_SRV.getAction());
        }

        if (status.isInSrv() || status.isOnFoot() || isDryRun) {
            map.put("відправ корабель, корабель на орбіту, відкликати корабель з поверхні", DISMISS_SHIP.getAction());
            map.put("повернись на поверхню, забери мене, виклич корабель", RETURN_TO_SURFACE.getAction());
        }

        // market / traders / brokers
        map.put("знайди торговця сировиною, торговець raw матеріалами, де обміняти raw матеріали {key:X}", FIND_RAW_MATERIAL_TRADER.getAction());
        map.put("знайди торговця закодованими матеріалами, encoded trader, торговець даними {key:X}", FIND_ENCODED_MATERIAL_TRADER.getAction());
        map.put("знайди торговця виготовленими матеріалами, manufactured trader, торговець manufactured матеріалами {key:X}", FIND_MANUFACTURED_MATERIAL_TRADER.getAction());
        map.put("знайди людського техноброкера, human tech broker {key:X}", FIND_HUMAN_TECHNOLOGY_BROKER.getAction());
        map.put("знайди guardian техноброкера, guardian technology broker {key:X}", FIND_GUARDIAN_TECHNOLOGY_BROKER.getAction());
        map.put("знайди товар, знайди найближчий товар, купити товар, де купити, знайди ринок {key:X, max_distance:Y, state:true/false}", FIND_COMMODITY.getAction());
        map.put("знайди найближчий авіаносець, найближчий carrier", FIND_NEAREST_FLEET_CARRIER.getAction());

        // fleet carrier
        map.put("встанови резерв палива авіаносця, резерв тритію авіаносця {key:X}", SET_CARRIER_FUEL_RESERVE.getAction());
        map.put("розрахуй маршрут авіаносця, сплануй маршрут авіаносця, маршрут стрибків авіаносця", CALCULATE_FLEET_CARRIER_ROUTE.getAction());
        map.put("введи пункт призначення авіаносця, встанови пункт призначення авіаносця, пункт призначення авіаносця", ENTER_FLEET_CARRIER_DESTINATION.getAction());

        // squadron carrier
        map.put("маршрут авіаносця ескадрильї, навігація авіаносця ескадрильї, шлях авіаносця ескадрильї, скільки стрибків на маршруті авіаносця ескадрильї, стрибків залишилось у авіаносця ескадрильї", SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("куди летить авіаносець ескадрильї, кінцева точка авіаносця ескадрильї, пункт призначення авіаносця ескадрильї, курс авіаносця ескадрильї", SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
        map.put("тритій авіаносця ескадрильї, паливо авіаносця ескадрильї, запаси тритію ескадрильї, рівень палива авіаносця ескадрильї, статус тритію авіаносця ескадрильї", SQUADRON_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("статус авіаносця ескадрильї, фінанси авіаносця ескадрильї, баланс авіаносця ескадрильї, огляд авіаносця ескадрильї, кошти авіаносця ескадрильї, скільки авіаносець ескадрильї може працювати, статус палива авіаносця ескадрильї", SQUADRON_CARRIER_STATUS.getAction());
        map.put("eta авіаносця ескадрильї, коли прибуде авіаносець ескадрильї, скільки до прибуття авіаносця ескадрильї, час прибуття авіаносця ескадрильї, прибуття авіаносця ескадрильї", SQUADRON_CARRIER_ETA.getAction());

        // trade
        map.put("розрахуй торговий маршрут", CALCULATE_TRADE_ROUTE.getAction());
        map.put("список параметрів торгового маршруту, покажи параметри торгового маршруту", LIST_TRADE_ROUTE_PARAMETERS.getAction());
        map.put("монетизуй маршрут, зроби маршрут прибутковим", MONETIZE_ROUTE.getAction());

        map.put("змінити стартовий бюджет торгового профілю {key:X}", CHANGE_TRADE_PROFILE_SET_STARTING_BUDGET.getAction());
        map.put("змінити максимальну кількість зупинок торгового профілю {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_NUMBER_OF_STOPS.getAction());
        map.put("змінити максимальну відстань від входу торгового профілю {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_DISTANCE_FROM_ENTRY.getAction());
        map.put("змінити торговий профіль дозволити заборонений вантаж {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PROHIBITED_CARGO.getAction());
        map.put("змінити торговий профіль дозволити планетарний порт {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PLANETARY_PORT.getAction());
        map.put("змінити торговий профіль дозволити системи з дозволом {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PERMIT_SYSTEMS.getAction());
        map.put("змінити торговий профіль дозволити strongholds {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_STRONGHOLDS.getAction());

        // announcements / app settings
        map.put("перемкни радіо, радіотрафік, радіопередачі {state:true/false}", SET_RADIO_TRANSMISSION_MODE.getAction());
        map.put("оголошення радарних контактів {state:true/false}", SET_RADAR_CONTACT_ANNOUNCEMENT.getAction());
        map.put("оголошення відкриттів {state:true/false}", DISCOVERY_ON_OFF.getAction());
        map.put("оголошення маршруту {state:true/false}", ROUTE_ON_OFF.getAction());
        map.put("перемкнути всі оголошення {state:true/false}, всі оголошення {state:true/false}", TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        map.put("очисти нагадування", CLEAR_REMINDERS.getAction());
        map.put("встанови нагадування {key:X}", SET_REMINDER.getAction());
        map.put("нагадай мені через {minutes:X} хвилин {key:Y}, таймер на {minutes:X} хвилин {key:Y}, нагадування через {minutes:X} хвилин {key:Y}", SET_TIMED_REMINDER.getAction());
        map.put("досліджуй систему, скануй систему, відскануй систему", HONK_THE_SYSTEM.getAction());

        // UI panels
        map.put("активуй, натисни активувати", ACTIVATE.getAction());
        map.put("покажи панель транзакцій, відкрий транзакції", SHOW_TRANSACTIONS.getAction());
        map.put("покажи контакти, відкрий панель контактів", SHOW_CONTACTS.getAction());
        map.put("покажи панель навігації, відкрий навігацію", SHOW_NAVIGATION.getAction());
        map.put("покажи чат, відкрий чат, панель зв'язку, коммс панель", SHOW_CHAT_PANEL.getAction());
        map.put("покажи пошту, відкрий вхідні, email", SHOW_INBOX_PANEL.getAction());
        map.put("покажи соціальну панель, відкрий соціальну панель", SHOW_SOCIAL_PANEL.getAction());
        map.put("покажи історію, відкрий історію", SHOW_HISTORY_PANEL.getAction());
        map.put("покажи ескадрилью, відкрий ескадрилью", SHOW_SQUADRON.getAction());
        map.put("покажи статус, відкрий панель статусу", SHOW_STATUS_PANEL.getAction());
        map.put("покажи керування авіаносцем, відкрий carrier management", DISPLAY_CARRIER_MANAGEMENT.getAction());
        map.put("відкрий карту галактики, покажи карту галактики", OPEN_GALAXY_MAP.getAction());
        map.put("відкрий карту системи, покажи локальну карту, системна карта", OPEN_SYSTEM_MAP.getAction());
        map.put("закрий панель, вийти, закрити", EXIT_CLOSE.getAction());

        // pirate massacre missions
        map.put("навігація до системи видачі місій, курс до системи провайдера місій", RECON_PROVIDER_SYSTEM.getAction());
        map.put("навігація до піратського провайдера місій, лети до видачі піратських місій", NAVIGATE_TO_PIRATE_MISSION_PROVIDER.getAction());
        map.put("активні місії, поточні місії, журнал місій, які місії, статус місій, наші місії", ANALYZE_MISSIONS.getAction());
        map.put("піратська місія, рахунок вбивств, скільки вбивств, прогрес massacre місії, скільки піратів залишилось", PIRATE_MISSION_PROGRESS.getAction());
        map.put("знайди мисливські угіддя {key:X}, знайди місце полювання {key:X}", FIND_HUNTING_GROUNDS.getAction());
        map.put("розвідка мисливських угідь, навігація до цільової системи, курс до системи полювання", RECON_TARGET_SYSTEM.getAction());
        map.put("ігноруй мисливські угіддя, пропусти місце полювання", IGNORE_HUNTING_GROUND.getAction());
        map.put("підтверди мисливські угіддя, підтверди цільову зоряну систему", CONFIRM_HUNTING_GROUND.getAction());
        map.put("очистити активні місії, очистити всі активні місії", CLEAR_ALL_ACTIVE_MISSIONS.getAction());

        // science / mining / biology
        map.put("додай ціль видобутку {key:X}", ADD_MINING_TARGET.getAction());
        map.put("видали ціль видобутку {key:X}", REMOVE_MINING_TARGET.getAction());
        map.put("очисти цілі видобутку", CLEAR_MINING_TARGETS.getAction());
        map.put("оголошення видобутку {state:true/false}", MINING_ON_OFF.getAction());
        map.put("знайди мозкові дерева {key:X, max_distance:Y}", FIND_BRAIN_TREES.getAction());
        map.put("знайди місце видобутку, знайди точку видобутку, знайди hotspot, де майнити, знайди астероїдне поле {key:X, max_distance:Y}", FIND_MINING_SITE.getAction());
        map.put("знайди місце видобутку тритію, знайди тритієве поле {key:X, max_distance:Y}", FIND_FLEET_CARRIER_FUEL_MINING_SITE.getAction());
        map.put("навігація до наступного біозразка, до наступного зразка, до наступної органіки, запис кодексу, навігація до кодексу", NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
        map.put("проскануй систему, відкрий fss, повний скан, honk, скан системи, full spectrum scan", OPEN_FSS.getAction());
        map.put("знайди найближчу vista genomics, знайди геноміку", FIND_VISTA_GENOMICS.getAction());
        map.put("видали запис кодексу, видали цей кодекс, видали цей запис, видали цю органіку", DELETE_CODEX_ENTRY.getAction());

        map.put("перевір бинди, відсутні клавіші, неназначені клавіші, клавіатурні прив'язки, перевірка клавіш", KEY_BINDINGS_ANALYSIS.getAction());
        map.put("біосигнали в системі виконані, органіка просканована в системі, скільки біозразків у системі, біосигнали в системі, які планети мають біосигнали, які планети ще треба сканувати", BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        map.put("екзобіологічні зразки, біологічні зразки, органіка в локації, які організми, що залишилось сканувати, організми залишились, прогрес екзобіології, що проскановано тут, яка органіка на цій планеті", EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        map.put("відстань до останнього біозразка, як далеко до зразка, відстань до останньої органіки, дистанція до біозразка, навігація до біозразка", DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
        map.put("аналіз біому, який біом {key:X}, планетарний біом, аналіз атмосфери, яке життя тут, тип біому", PLANET_BIOME_ANALYSIS.getAction());
        map.put("зоряні об'єкти, планети в системі, посадкові планети, планета чи місяць придатні для посадки, тіла в системі, які планети, скільки планет, кільця, крижані кільця", QUERY_STELLAR_OBJETS.getAction());
        map.put("сигнали в системі, які сигнали в системі, що є в системі, виявлені сигнали, fss сигнали, hotspots, resource extraction sites, конфліктні зони, емісії", QUERY_STELLAR_SIGNALS.getAction());
        map.put("геосигнали, геологічні сигнали, вулканічні сигнали, геологічна активність, вулканічна активність", QUERY_GEO_SIGNALS.getAction());

        map.put("авіаносці в системі, carriers у системі, скільки авіаносців, чи є авіаносці поруч", QUERY_CARRIERS.getAction());
        map.put("маршрут авіаносця, навігація авіаносця, шлях авіаносця, скільки стрибків на маршруті авіаносця, стрибків залишилось", FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("маршрут авіаносця, куди летить авіаносець, наступний стрибок авіаносця, кінцева точка авіаносця", FLEET_CARRIER_ROUTE.getAction());
        //map.put("тритій авіаносця, паливо авіаносця, скільки тритію, рівень тритію, резерв тритію", FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("статус авіаносця, фінанси авіаносця, баланс авіаносця, огляд авіаносця, кошти авіаносця, скільки авіаносець може працювати, запас ходу авіаносця", FLEET_CARRIER_STATUS.getAction());
        map.put("eta авіаносця, коли прибуде авіаносець, скільки до прибуття авіаносця, час стрибка авіаносця", FLEET_CARRIER_ETA.getAction());


        map.put("відстань до авіаносця, де наш авіаносець, як далеко авіаносець, дистанція до авіаносця", DISTANCE_TO_CARRIER.getAction());
        map.put("безпека системи, контроль фракції, хто контролює систему, рівень безпеки, власник системи, домінуюча фракція", SYSTEM_SECURITY_ANALYSIS.getAction());
        map.put("торговий профіль, торгові налаштування, параметри торгівлі, торгова конфігурація, критерії торгівлі", TRADE_PROFILE_ANALYSIS.getAction());
        map.put("відстань до зоряного об'єкта, як далеко до тіла, відстань до планети {key:X}, відстань до місяця, як далеко до станції, дистанція до тіла", DISTANCE_TO_BODY.getAction());
        map.put("останній скан, що ми сканували, останній просканований об'єкт, найсвіжіший скан", LAST_SCAN_ANALYSIS.getAction());
        map.put("інвентар матеріалів {key:X}, скільки предметів {key:X}, скільки матеріалів {key:X}, чи є матеріал {key:X}, скільки {key:X} у нас є, інженерний матеріал {key:X}, сировинний матеріал {key:X}, manufactured матеріал {key:X}, закодований матеріал {key:X}", MATERIALS_INVENTORY.getAction());
        map.put("матеріали планети, матеріали тут, які матеріали на цій планеті, поверхневі матеріали, поклади матеріалів, мінерали на планеті", PLANET_MATERIALS.getAction());
        map.put("прибуток дослідження, вартість відкриттів, скільки коштує дослідження, цінність сканів, прибуток картографування, цінність екзобіології", EXPLORATION_PROFITS.getAction());
        map.put("де я знаходжуся, де ми знаходимося, наша позиція, в якій системі ми, де я, наші координати, поточна система, на якій планеті ми, поточна позиція, довжина дня", CURRENT_LOCATION.getAction());
        map.put("в якій системі я знаходжусь, в якій системі ми знаходимось, яка поточна система, де мій корабель, де ми зараз, наше місцезнаходження", CURRENT_LOCATION.getAction());
        map.put("інформація про ціль fsd, аналіз пункту призначення, яку зірку ми вибрали, аналіз fsd цілі", FSD_TARGET_ANALYSIS.getAction());
        map.put("прокладений маршрут, паливо на наступній зупинці, наявність палива на маршруті, аналіз маршруту, ми вже на місці, поточний маршрут, скільки стрибків залишилось, наступна зірка scoopable", PLOTTED_ROUTE_ANALYSIS.getAction());
        map.put("торговий маршрут, поточний торговий план, чим ми торгуємо, наш торговий план, торгові етапи", TRADE_ROUTE_ANALYSIS.getAction());
        map.put("оснащення, покращення корабля, доступні модулі, які модулі на станції, доступне обладнання, купити модулі, частини корабля", LOCAL_OUTFITTING.getAction());
        map.put("верф, кораблі на продаж, які кораблі на станції, купити корабель, доступні кораблі, новий корабель", LOCAL_SHIPYARD.getAction());
        map.put("що у вантажному відсіку, що ми веземо, вміст вантажу, товари на борту, чим ми завантажені", CARGO_HOLD_CONTENTS.getAction());
        map.put("профіль гравця", PLAYER_PROFILE_ANALYSIS.getAction());
        map.put("компонування корабля, звіт про пошкодження, модулі корабля, готовність до бою, обладнання корабля, характеристики корабля, на чому я лечу, що встановлено, генератор щитів, посилення корпусу, сенсори, двигуни, frameshift, паливний ківш", SHIP_LOADOUT.getAction());
        map.put("деталі станції, які сервіси тут, які послуги тут, сервіси на станції, що пропонує станція, інформація про станцію, обладнання станції, що є на цій станції, доступні сервіси", STATION_DETAILS.getAction());
        map.put("на якій станції я знаходжусь, на якій станції ми знаходимось, де я пристикований, де ми пристиковані, яка моя поточна станція, яка у нас поточна станція", STATION_DETAILS.getAction());
        map.put("нагороди за голови, загальний bounty, скільки bounty, заробіток з bounty, кредити за bounty", TOTAL_BOUNTIES.getAction());
        map.put("відстань до бульбашки, відстань до sol, відстань від sol, відстань до землі, як далеко від бульбашки, як далеко від цивілізації, відстань від населеного космосу", DISTANCE_TO_BUBBLE.getAction());
        map.put("поточний час, скільки часу, час на землі, галактичний час, utc час, реальний час", TIME_IN_ZONE.getAction());
        map.put("нагадування, яке було нагадування, нагадування пункту призначення, чи є нагадування, що ми встановили як нагадування", REMINDER.getAction());
        map.put("локальні ринки, ринки на станціях і поселеннях, ринки на аванпостах у системі", ANALYZE_MARKETS.getAction());
    }
}
