package elite.intel.ai.brain.i18n.es;


import elite.intel.ai.brain.i18n.AiActionAliasProvider;
import elite.intel.session.Status;

import java.util.Map;
import java.util.Set;

import static elite.intel.ai.brain.actions.Commands.*;
import static elite.intel.ai.brain.actions.Queries.*;


public class SpanishAiActionAliases implements AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("despierta", "despiértate", "escucha", "escúchame");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("escúchame", "escucha");
    }

    @Override
    public void addAliases(Map<String, String> map, Status status, boolean isDryRun) {

        // always available
        map.put("despierta, despiértate, escucha, escúchame", WAKEUP.getAction());
        map.put("duerme, vete a dormir, ignórame, no me vigiles", SLEEP.getAction());
        map.put("interrumpe", INTERRUPT_TTS.getAction());

        // navigation
        map.put("cancelar ruta comercial, detener ruta comercial, borrar ruta comercial, abortar ruta comercial", CANCEL_TRADE_ROUTE.getAction());
        map.put("navega a las coordenadas {lat:X, lon:Y}", NAVIGATE_TO_TARGET.getAction());
        map.put("navega a la misión activa, traza ruta a la misión activa, traza ruta a la misión, llévame a la misión, ve a la misión {key:X}", NAVIGATE_TO_NEXT_MISSION.getAction());
        map.put("navega al fleet carrier, ve al carrier, rumbo al carrier, regresa al carrier, llévanos al carrier", NAVIGATE_TO_FLEET_CARRIER.getAction());
        map.put("navega a la zona de aterrizaje, rumbo a la zona de aterrizaje, dirección a la zona de aterrizaje, volver a la lz", GET_HEADING_TO_LZ.getAction());
        map.put("navega a la siguiente parada comercial, ve a la siguiente parada comercial", NAVIGATE_TO_NEXT_TRADE_STOP.getAction());
        map.put("navega desde memoria, pegar desde memoria", NAVIGATE_TO_ADDRESS_FROM_MEMORY.getAction());
        map.put("cancelar navegación, abortar navegación, detener navegación", NAVIGATION_OFF.getAction());
        map.put("establecer sistema hogar, establecer sistema actual como hogar, marcar sistema hogar", SET_HOME_SYSTEM.getAction());
        map.put("llévame a casa, ve a casa, navega a casa, regresa a casa, traza ruta a casa, rumbo a casa, llévanos a casa", TAKE_ME_HOME.getAction());
        map.put("restablecer vista de cabeza, restablecer vista de cabeza por defecto, restablecer vista de cabeza a neutral", RESET_HEAD_LOOK.getAction());
        if (status.isInMainShip() || isDryRun) {
            // navigation
            map.put("seleccionar destino fsd, fijar destino, establecer destino, seleccionar destino objetivo", TARGET_DESTINATION.getAction());
            map.put("salta al hiperespacio, salta, salto al hiperespacio, entrar al hiperespacio, vamos, siguiente punto de ruta", JUMP_TO_HYPERSPACE.getAction());
            map.put("sal de supercruise, salir aquí, salir del ftl, salir de supercruise, abandonar supercruise, caer aquí", DROP_FROM_SUPER_CRUISE.getAction());
            map.put("entrar en supercruise, supercruise, activar supercruise", ENTER_SUPER_CRUISE.getAction());
            map.put("despegar nave, despegar, separarse de la estación, salir del puerto, salir de la estación", LAUNCH_SHIP.getAction());
            // speed / throttle
            map.put("detener motores, parar aquí, alto total, detener todo, alto, apagar motores, cortar acelerador, acelerador a cero, detener nave", SET_SPEED_ZERO.getAction());
            map.put("taxi al punto de aterrizaje, taxi, aterrizaje automático, aterrizaje con piloto automático", TAXI.getAction());
            map.put("un cuarto de acelerador, 25 por ciento, velocidad lenta, un cuarto", SET_SPEED25.getAction());
            map.put("medio acelerador, 50 por ciento, media velocidad", SET_SPEED50.getAction());
            map.put("tres cuartos de acelerador, 75 por ciento, velocidad a tres cuartos", SET_SPEED75.getAction());
            map.put("acelerador al máximo, 100 por ciento, velocidad máxima, máxima velocidad, acelerador máximo", SET_SPEED100.getAction());
            map.put("aumenta velocidad en {key:X}", INCREASE_SPEED_BY.getAction());
            map.put("reduce velocidad en {key:X}", DECREASE_SPEED_BY.getAction());
            map.put("establecer velocidad óptima, velocidad óptima de aproximación, optimizar velocidad de aproximación", SET_OPTIMAL_SPEED.getAction());
            map.put("tren de aterrizaje, bajar tren de aterrizaje, desplegar tren de aterrizaje", DEPLOY_LANDING_GEAR.getAction());
            map.put("retraer tren de aterrizaje, subir tren de aterrizaje, guardar tren de aterrizaje", RETRACT_LANDING_GEAR.getAction());
            map.put("solicitar atraque, atracar en estación, solicitar aterrizaje, petición de atraque, pedir atraque, solicitar plataforma, estacionar nave", REQUEST_DOCKING.getAction());
            // UI panels
            map.put("mostrar panel del fighter, abrir panel del fighter, mostrar panel del caza", SHOW_FIGHTER_PANEL.getAction());
            map.put("desplegar célula de escudo, usar célula de escudo, activar célula de escudo, banco de células de escudo, activar banco de células de escudo", DEPLOY_SHIELD_CELL.getAction());
            map.put("desplegar chaff, lanzar chaff, usar chaff, disparar chaff, lanzar bengalas", DEPLOY_CHAFF.getAction());
            // combat
            map.put("desplegar anclajes, desplegar hardpoints, armas activas, listo para combate, armas libres, sacar armas, armar armas, armas listas", DEPLOY_HARDPOINTS.getAction());
            map.put("retraer anclajes, retraer hardpoints, armas frías, guardar armas, alto al combate, enfundar armas, bajar armas, asegurar armas", RETRACT_HARDPOINTS.getAction());
            map.put("objetivo fsd {key:fsd}, objetivo motores {key:drive}, objetivo distribuidor de energía {key:power distributor}, objetivo planta de energía {key:powerplant}, objetivo soporte vital {key:life support}", TARGET_SUB_SYSTEM.getAction());
            map.put("apuntar al compañero 1, wingman alpha", TARGET_WINGMAN0.getAction());
            map.put("apuntar al compañero 2, wingman bravo", TARGET_WINGMAN1.getAction());
            map.put("apuntar al compañero 3, wingman charlie", TARGET_WINGMAN2.getAction());
            map.put("wing nav lock, bloqueo de navegación del ala, seguir al wingman", WING_NAV_LOCK.getAction());
            map.put("objetivo prioritario, apuntar a la mayor amenaza, objetivo más peligroso, seleccionar hostil, siguiente enemigo, seleccionar enemigo", SELECT_HIGHEST_THREAT.getAction());
            // vehicle deployment
            map.put("desplegar srv, desplegar vehículo, lanzar srv, sacar srv", DEPLOY_SRV.getAction());
            map.put("desplegar disipador térmico, lanzar disipador térmico, descargar calor", DEPLOY_HEAT_SINK.getAction());
            // fighter orders
            map.put("desplegar fighter, lanzar fighter, sacar fighter", DEPLOY_FIGHTER.getAction());
            map.put("ordenar al fighter defender nave, fighter defender, fighter defensivo", FIGHTER_REQUEST_DEFENSIVE_BEHAVIOUR.getAction());
            map.put("ordenar al fighter atacar mi objetivo, fighter atacar, enfocar mi objetivo, enfocar objetivo, fighter enfocar objetivo", FIGHTER_REQUEST_FOCUS_TARGET.getAction());
            map.put("ordenar al fighter mantener fuego, fighter alto el fuego, fighter retirarse", FIGHTER_REQUEST_HOLD_FIRE.getAction());
            map.put("ordenar al fighter volver a la nave, fighter acoplar, recuperar fighter", FIGHTER_REQUEST_REQUEST_DOCK.getAction());
            map.put("fighter fuego a discreción, fuego a voluntad, atacar a voluntad", FIGHTER_OPEN_ORDERS.getAction());
            map.put("seleccionar grupo de fuego {key:X}, grupo de fuego {key:X}, cambiar al grupo de fuego {key:X}", SELECT_FIRE_GROUP_BY_NATO.getAction());
            map.put("próximo salto de estrella de neutrones, trazar la ruta al siguiente punto de ruta de la estrella de neutrones, siguiente estrella de neutrones", PLOT_ROUTE_TO_NEXT_NEUTRON_STAR.getAction());
            map.put("borrar/eliminar ruta de estrella de neutrones", CLEAR_NEUTRON_ROUTE.getAction());
        }

        if (status.isInMainShip() && !status.isDocked() || isDryRun) {
            map.put("estaciones en el sistema, qué estaciones, estaciones cercanas, puertos estelares, estaciones espaciales, atraque disponible", QUERY_STATIONS.getAction());
        }

        if (status.isInSrv() && status.isDocked() || isDryRun) {
            map.put("mostrar panel de servicios, abrir panel de servicios, mostrar panel de servicios de estación", SHOW_STATION_SERVICES.getAction());
        }

        if (status.isInMainShip() || status.isInSrv() || isDryRun) {
            // flight / ship systems
            map.put("cambiar a modo combate", ACTIVATE_COMBAT_MODE.getAction());
            map.put("cambiar a modo análisis", ACTIVATE_ANALYSIS_MODE.getAction());
            map.put("cargo scoop, abrir cargo scoop, cerrar cargo scoop, desplegar cargo scoop, retraer cargo scoop, abrir bodega de carga, cerrar bodega de carga", TOGGLE_CARGO_SCOOP.getAction());
            map.put("visión nocturna, activar visión nocturna, desactivar visión nocturna {state:true/false}", NIGHT_VISION_ON_OFF.getAction());
            map.put("luces, faros, apagar luces, encender luces, luces de la nave, luces encendidas, luces apagadas {state:true/false}", LIGHTS_ON_OFF.getAction());
            // UI panels
            map.put("mostrar panel del comandante, abrir panel del comandante, panel central, panel de rol, abrir kneeboard", SHOW_COMMANDER_PANEL.getAction());
            map.put("mostrar panel de tripulación, abrir panel de tripulación", SHOW_CREW.getAction());
            map.put("mostrar panel interno, abrir panel interno", SHOW_INTERNAL_PANEL.getAction());
            map.put("mostrar panel de módulos, abrir panel de módulos", SHOW_MODULES_PANEL.getAction());
            map.put("mostrar grupos de fuego, abrir grupos de fuego", SHOW_FIRE_GROUPS.getAction());
            map.put("mostrar panel de inventario, abrir panel de inventario", SHOW_INVENTORY_PANEL.getAction());
            map.put("mostrar panel de almacenamiento, abrir panel de almacenamiento", SHOW_STORAGE_PANEL.getAction());
            // power
            map.put("energía a escudos, máximo escudos, reforzar escudos", INCREASE_SHIELDS_POWER.getAction());
            map.put("energía a motores, máximo motores, reforzar motores", INCREASE_ENGINES_POWER.getAction());
            map.put("energía a armas, máximo armas, reforzar armas", INCREASE_WEAPONS_POWER.getAction());
            // vehicle deployment
            map.put("desembarcar, salir de la nave, bajar de la nave", DISEMBARK.getAction());
            map.put("igualar energía, balancear energía, restablecer energía, distribuir energía por igual", RESET_POWER.getAction());
        }

        if (status.isInSrv() || isDryRun) {
            map.put("asistente de conducción, asistencia de conducción, asistencia srv {state:true/false}", DRIVE_ASSIST.getAction());
            map.put("recuperar srv, abordar nave, regresar srv, recoger srv, acoplar srv", RECOVER_SRV.getAction());
        }

        if (status.isInSrv() || status.isOnFoot() || isDryRun) {
            map.put("despedir nave, enviar nave lejos, nave a órbita", DISMISS_SHIP.getAction());
            map.put("volver a superficie, recógeme", RETURN_TO_SURFACE.getAction());
        }

        // market / traders / brokers
        map.put("buscar comerciante de materiales sin procesar, comerciante de materiales sin procesar, dónde intercambiar materiales sin procesar {key:X}", FIND_RAW_MATERIAL_TRADER.getAction());
        map.put("buscar comerciante de materiales codificados, comerciante codificado, comerciante de datos {key:X}", FIND_ENCODED_MATERIAL_TRADER.getAction());
        map.put("buscar comerciante de materiales fabricados, comerciante fabricado {key:X}", FIND_MANUFACTURED_MATERIAL_TRADER.getAction());
        map.put("buscar intermediario tecnológico humano, human technology broker {key:X}", FIND_HUMAN_TECHNOLOGY_BROKER.getAction());
        map.put("buscar intermediario tecnológico guardian, guardian technology broker {key:X}", FIND_GUARDIAN_TECHNOLOGY_BROKER.getAction());
        map.put("buscar mercancía, buscar mercancía más cercana, comprar mercancía, dónde comprar, buscar mercado {key:X, max_distance:Y, state:true/false}", FIND_COMMODITY.getAction());
        map.put("buscar fleet carrier más cercano, carrier más cercano", FIND_NEAREST_FLEET_CARRIER.getAction());

        // fleet carrier
        map.put("establecer reserva de combustible del carrier, reserva de tritio del carrier {key:X}", SET_CARRIER_FUEL_RESERVE.getAction());
        map.put("calcular ruta de fleet carrier, planificar ruta del carrier, ruta de salto del carrier", CALCULATE_FLEET_CARRIER_ROUTE.getAction());
        map.put("ingresar destino del carrier, establecer destino del carrier, destino del carrier", ENTER_FLEET_CARRIER_DESTINATION.getAction());

        // squadron carrier
        map.put("ruta del carrier del escuadrón, navegación del carrier del escuadrón, ruta de salto del carrier del escuadrón, cuántos saltos en la ruta del carrier del escuadrón, saltos restantes del carrier del escuadrón", SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("a dónde va el carrier del escuadrón, rumbo del carrier del escuadrón, destino final del carrier del escuadrón", SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
        map.put("tritio del carrier del escuadrón, combustible del carrier del escuadrón, reserva de tritio del escuadrón, nivel de combustible del carrier del escuadrón, estado de tritio del carrier del escuadrón", SQUADRON_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("estado del carrier del escuadrón, finanzas del carrier del escuadrón, balance del carrier del escuadrón, resumen del carrier del escuadrón, fondos del carrier del escuadrón, cuánto tiempo podemos operar el carrier del escuadrón, estado de combustible del carrier del escuadrón", SQUADRON_CARRIER_STATUS.getAction());
        map.put("eta del carrier del escuadrón, cuándo llega el carrier del escuadrón, cuánto falta para el carrier del escuadrón, hora de llegada del carrier del escuadrón, llegada del carrier del escuadrón", SQUADRON_CARRIER_ETA.getAction());

        // trade
        map.put("calcular ruta comercial", CALCULATE_TRADE_ROUTE.getAction());
        map.put("listar parámetros de ruta comercial", LIST_TRADE_ROUTE_PARAMETERS.getAction());
        map.put("monetizar ruta", MONETIZE_ROUTE.getAction());

        map.put("cambiar presupuesto inicial del perfil comercial {key:X}", CHANGE_TRADE_PROFILE_SET_STARTING_BUDGET.getAction());
        map.put("cambiar máximas paradas del perfil comercial {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_NUMBER_OF_STOPS.getAction());
        map.put("cambiar distancia máxima del perfil comercial {key:X}", CHANGE_TRADE_PROFILE_SET_MAX_DISTANCE_FROM_ENTRY.getAction());
        map.put("cambiar permitir carga prohibida en perfil comercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PROHIBITED_CARGO.getAction());
        map.put("cambiar permitir puertos planetarios en perfil comercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PLANETARY_PORT.getAction());
        map.put("cambiar permitir sistemas con permiso en perfil comercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_PERMIT_SYSTEMS.getAction());
        map.put("cambiar permitir strongholds en perfil comercial {state:true/false}", CHANGE_TRADE_PROFILE_SET_ALLOW_STRONGHOLDS.getAction());

        // announcements / app settings
        map.put("activar radio, tráfico de radio, transmisiones de radio {state:true/false}", SET_RADIO_TRANSMISSION_MODE.getAction());
        map.put("anuncios de contactos de radar {state:true/false}", SET_RADAR_CONTACT_ANNOUNCEMENT.getAction());
        map.put("anuncios de descubrimiento {state:true/false}", DISCOVERY_ON_OFF.getAction());
        map.put("anuncios de ruta {state:true/false}", ROUTE_ON_OFF.getAction());
        map.put("conmutar todos los anuncios {state:true/false}", TOGGLE_ALL_ANNOUNCEMENTS.getAction());
        map.put("borrar recordatorios", CLEAR_REMINDERS.getAction());
        map.put("establecer recordatorio {key:X}", SET_REMINDER.getAction());
        map.put("recuérdame en {minutes:X} minutos {key:Y}, poner temporizador de {minutes:X} minutos {key:Y}, recordatorio temporizado {minutes:X} minutos {key:Y}", SET_TIMED_REMINDER.getAction());

        // UI panels
        map.put("activar", ACTIVATE.getAction());
        map.put("mostrar panel de transacciones, abrir panel de transacciones", SHOW_TRANSACTIONS.getAction());
        map.put("mostrar panel de contactos, abrir panel de contactos", SHOW_CONTACTS.getAction());
        map.put("mostrar panel de navegación, abrir panel de navegación", SHOW_NAVIGATION.getAction());
        map.put("mostrar panel de chat, abrir panel de chat, panel de comunicaciones", SHOW_CHAT_PANEL.getAction());
        map.put("mostrar panel de bandeja de entrada, abrir bandeja de entrada, abrir correo", SHOW_INBOX_PANEL.getAction());
        map.put("mostrar panel social, abrir panel social", SHOW_SOCIAL_PANEL.getAction());
        map.put("mostrar panel de historial, abrir panel de historial", SHOW_HISTORY_PANEL.getAction());
        map.put("mostrar panel de escuadrón, abrir panel de escuadrón", SHOW_SQUADRON.getAction());
        map.put("mostrar panel de estado, abrir panel de estado", SHOW_STATUS_PANEL.getAction());
        map.put("mostrar panel de gestión del carrier, abrir panel de gestión del carrier", DISPLAY_CARRIER_MANAGEMENT.getAction());
        map.put("mostrar mapa galáctico, abrir mapa galáctico", OPEN_GALAXY_MAP.getAction());
        map.put("mostrar mapa del sistema, abrir mapa del sistema", OPEN_SYSTEM_MAP.getAction());
        map.put("salir, cerrar panel, cerrar", EXIT_CLOSE.getAction());

        // pirate massacre missions
        map.put("navegar al sistema del proveedor de misión", RECON_PROVIDER_SYSTEM.getAction());
        map.put("navegar al proveedor de misión pirata", NAVIGATE_TO_PIRATE_MISSION_PROVIDER.getAction());
        map.put("misiones activas, misiones actuales, registro de misiones, qué misiones, estado de misiones, cuáles son nuestras misiones, misiones en curso, tablero de misiones", ANALYZE_MISSIONS.getAction());
        map.put("misión pirata, conteo de bajas, cuántas bajas, progreso de misión de masacre, bajas restantes, progreso de masacre, piratas restantes, bajas piratas, progreso de caza de recompensas", PIRATE_MISSION_PROGRESS.getAction());
        map.put("buscar zona de caza {key:X}", FIND_HUNTING_GROUNDS.getAction());
        map.put("reconocer zona de caza, navegar al sistema objetivo, navegar a la zona de caza", RECON_TARGET_SYSTEM.getAction());
        map.put("ignorar zona de caza", IGNORE_HUNTING_GROUND.getAction());
        map.put("confirmar zona de caza, confirmar sistema estelar objetivo", CONFIRM_HUNTING_GROUND.getAction());
        map.put("borrar misiones activas, borrar todas las misiones activas", CLEAR_ALL_ACTIVE_MISSIONS.getAction());

        // science / mining / biology
        map.put("agregar objetivo de minería {key:X}", ADD_MINING_TARGET.getAction());
        map.put("quitar objetivo de minería {key:X}", REMOVE_MINING_TARGET.getAction());
        map.put("borrar objetivos de minería", CLEAR_MINING_TARGETS.getAction());
        map.put("anuncios de minería {state:true/false}", MINING_ON_OFF.getAction());
        map.put("buscar brain trees {key:X, max_distance:Y}", FIND_BRAIN_TREES.getAction());
        map.put("buscar sitio de minería, buscar ubicación de minería, buscar hotspot de minería, dónde minar, buscar campo de asteroides {key:X, max_distance:Y}", FIND_MINING_SITE.getAction());
        map.put("buscar sitio de minería de tritio, buscar campo de tritio {key:X, max_distance:Y}", FIND_FLEET_CARRIER_FUEL_MINING_SITE.getAction());
        map.put("navegar a la siguiente muestra biológica, ir a la siguiente muestra, navegar al siguiente orgánico, entrada del codex, navegar a la entrada del codex, navegar al codex", NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
        map.put("escanear el sistema, abrir fss, escaneo completo, escaneo de espectro completo, escaneo del sistema, escaneo de descubrimiento, fss, escanear sistema", OPEN_FSS.getAction());
        map.put("buscar vista genomics más cercano, buscar genomics, vista genomics", FIND_VISTA_GENOMICS.getAction());
        map.put("eliminar entrada del codex, eliminar este codex, eliminar esta entrada, eliminar este orgánico", DELETE_CODEX_ENTRY.getAction());

        map.put("revisar key bindings, key bindings faltantes, teclas sin asignar, asignaciones de teclado, revisar bindings, bindings faltantes", KEY_BINDINGS_ANALYSIS.getAction());
        map.put("señales biológicas completadas en el sistema estelar, orgánicos escaneados en el sistema estelar, cuántas muestras biológicas en el sistema estelar, señales biológicas en el sistema, muestras biológicas en el sistema estelar, qué planetas tienen señales biológicas, qué planetas aún necesitan escaneos biológicos, qué planetas aún necesitan escaneo, progreso de escaneo biológico", BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
        map.put("muestras de exobiología, muestras biológicas, orgánicos en la ubicación, qué organismos, qué falta por escanear, organismos restantes, muestras restantes, progreso de exobiología, escaneo restante, qué escaneos biológicos se completaron, orgánicos en este planeta, biología en este planeta, qué organismos hay aquí, progreso de muestras biológicas en el planeta, qué se ha escaneado aquí, qué orgánicos quedan por escanear", EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
        map.put("distancia a la última muestra biológica, qué tan lejos está la muestra, qué tan lejos está el último organismo, alcance a muestra biológica, qué tan lejos está el organismo anterior, navegar a muestra biológica", DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
        map.put("analizar bioma, análisis de bioma, qué bioma {key:X}, bioma planetario, análisis de atmósfera, qué vida hay aquí, tipo de bioma", PLANET_BIOME_ANALYSIS.getAction());
        map.put("objetos estelares, planetas en el sistema, planetas aterrizables, si el planeta o la luna es aterrizable, cuerpos en el sistema, qué planetas, cuántos planetas, cuerpos del sistema, cuerpos estelares, anillos de hielo, anillos planetarios, anillos en el sistema, tiene anillos, sistema de anillos", QUERY_STELLAR_OBJETS.getAction());
        map.put("señales en el sistema, qué señales hay en este sistema, qué hay en este sistema, qué se detecta en el sistema, qué señales hay aquí, qué señales ves, qué señales detectas, señales fss, hotspots de minería, sitios de extracción de recursos, qué señales, zonas de conflicto, emisiones, señales no identificadas, señales del sistema, señales detectadas, señales anómalas", QUERY_STELLAR_SIGNALS.getAction());
        map.put("señales geológicas, señales volcánicas, actividad geológica, actividad volcánica, geología en el sistema", QUERY_GEO_SIGNALS.getAction());

        map.put("fleet carriers en el sistema, carriers en el sistema, cuántos carriers, fleet carriers aquí, hay carriers cerca", QUERY_CARRIERS.getAction());
        map.put("ruta del carrier, navegación del carrier, ruta de salto del carrier, viaje del carrier, plan de viaje del carrier, cuántos saltos en la ruta del carrier, saltos restantes del carrier", FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
        map.put("ruta del carrier, a dónde va el carrier, siguiente salto del carrier, hacia dónde se dirige el carrier, rumbo del carrier, destino final del carrier", FLEET_CARRIER_ROUTE.getAction());
        //map.put("tritio del carrier, combustible del carrier, cuánto tritio, reserva de tritio, nivel de tritio, nivel de combustible del carrier, estado de tritio del carrier", FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
        map.put("estado del carrier, finanzas del carrier, balance del carrier, resumen del carrier, fondos del carrier, cuánto tiempo podemos operar el carrier, estado de combustible del carrier, alcance de salto del carrier", FLEET_CARRIER_STATUS.getAction());
        map.put("eta del carrier, cuándo llega el carrier, cuánto falta para el carrier, hora de llegada del carrier, llegada del carrier, cuándo salta el carrier, hora de salto del carrier", FLEET_CARRIER_ETA.getAction());
        map.put("distancia al carrier, dónde está nuestro carrier, qué tan lejos está el carrier, alcance al carrier, proximidad del carrier", DISTANCE_TO_CARRIER.getAction());
        map.put("seguridad del sistema, control de facción, quién controla, lucha de poder, nivel de seguridad, quién posee este sistema, facción dominante, poder controlador", SYSTEM_SECURITY_ANALYSIS.getAction());
        map.put("perfil comercial, ajustes comerciales, parámetros comerciales, configuración comercial, criterios comerciales", TRADE_PROFILE_ANALYSIS.getAction());
        map.put("distancia a objeto estelar, qué tan lejos está el cuerpo, distancia al planeta {key:X}, alcance al planeta, qué tan lejos está la luna, qué tan lejos está la estación, alcance al cuerpo", DISTANCE_TO_BODY.getAction());
        map.put("último escaneo, qué escaneamos, último objeto escaneado, escaneo más reciente, escaneo reciente", LAST_SCAN_ANALYSIS.getAction());
        map.put("inventario de materiales {key:X}, cuántos objetos {key:X}, cuánto material {key:X}, tenemos material {key:X}, cuánto {key:X} tenemos, material de ingeniería {key:X}, material sin procesar {key:X}, material fabricado {key:X}, material codificado {key:X}, existencias de material {key:X}", MATERIALS_INVENTORY.getAction());
        map.put("materiales planetarios, materiales aquí, qué materiales hay en este planeta, materiales de superficie, qué materiales hay aquí, depósitos de materiales, minerales en el planeta", PLANET_MATERIALS.getAction());
        map.put("ganancias de exploración, ganancias por descubrimientos, cuánto vale la exploración, valor de exploración, valor de escaneo, ganancia por cartografiado, valor de exobiología, ganancias por escaneos", EXPLORATION_PROFITS.getAction());
        map.put("consultar ubicación actual, dónde estamos, nuestra posición, en qué sistema estamos, dónde estoy, nuestras coordenadas, sistema actual, en qué planeta estamos, nuestra posición actual, duración del día, en qué sistema estoy, cuál es el sistema actual, dónde está mi nave, dónde estamos ahora, nuestra ubicación, posición actual", CURRENT_LOCATION.getAction());
        map.put("información del objetivo fsd, analizar destino, a qué estrella apuntamos, analizar objetivo fsd, información del objetivo fsd", FSD_TARGET_ANALYSIS.getAction());
        map.put("ruta trazada, combustible en la siguiente parada, disponibilidad de combustible en la ruta, análisis de ruta, ya llegamos, ruta actual, ruta de navegación, saltos restantes, cuántos saltos, siguiente estrella scoopable, parada de combustible", PLOTTED_ROUTE_ANALYSIS.getAction());
        map.put("ruta comercial, ruta de comercio, plan comercial actual, qué estamos comerciando, nuestro plan comercial, itinerario comercial, tramos comerciales", TRADE_ROUTE_ANALYSIS.getAction());
        map.put("outfitting, mejoras de nave, módulos disponibles, qué módulos hay en la estación, equipamiento disponible, comprar módulos, partes de nave, equipamiento de estación", LOCAL_OUTFITTING.getAction());
        map.put("shipyard, naves en venta, qué naves hay en la estación, comprar una nave, naves disponibles, naves para comprar, nave nueva", LOCAL_SHIPYARD.getAction());
        map.put("qué hay en nuestra bodega, qué estamos llevando, contenido de carga, mercancías a bordo, qué estamos transportando, contenido de la bodega", CARGO_HOLD_CONTENTS.getAction());
        map.put("perfil del jugador", PLAYER_PROFILE_ANALYSIS.getAction());
        map.put("configuración de la nave, informe de daños, módulos de la nave, informe de preparación para combate, equipamiento de la nave, especificaciones de la nave, qué estoy pilotando, con qué estamos equipados, lo tienes equipado, generador de escudos, refuerzo de casco, sensores, propulsores, frameshift, fuel scoop, instalado", SHIP_LOADOUT.getAction());
        map.put("detalles de la estación, qué servicios hay aquí, qué servicios tiene esta estación, servicios en esta estación, qué ofrece la estación, información de la estación, instalaciones de la estación, qué hay en esta estación, servicios disponibles", STATION_DETAILS.getAction());
        map.put("en qué estación estoy, en qué estación estamos, dónde estoy atracado, dónde estamos atracados, cuál es mi estación actual, cuál es nuestra estación actual, estación actual", STATION_DETAILS.getAction());
        map.put("recompensas, recompensas totales, recompensas cobradas, cuánto en recompensas, ganancias por recompensas, créditos por recompensas", TOTAL_BOUNTIES.getAction());
        map.put("distancia a la burbuja, distancia a sol, distancia desde sol, distancia a la tierra, qué tan lejos de sol, qué tan lejos de la burbuja, qué tan lejos del espacio habitado, distancia desde el espacio habitado, qué tan lejos de la civilización", DISTANCE_TO_BUBBLE.getAction());
        map.put("hora actual, qué hora es, hora en la tierra, hora galáctica, hora utc, hora real", TIME_IN_ZONE.getAction());
        map.put("recordatorio, cuál era el recordatorio, recordatorio de destino, hay recordatorios, recuperar recordatorio, qué establecimos como recordatorio", REMINDER.getAction());
        map.put("mercados locales, mercados en estaciones y asentamientos, mercados en puestos avanzados del sistema", ANALYZE_MARKETS.getAction());
    }
}