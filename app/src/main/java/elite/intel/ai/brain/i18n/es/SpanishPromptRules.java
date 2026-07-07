package elite.intel.ai.brain.i18n.es;

import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;
import elite.intel.ai.brain.i18n.PromptLanguageRules;


public class SpanishPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "Spanish";
    }

    @Override
    public String queryStarterExamples() {
        return "qué, dónde, cómo, cuál, cuáles, por qué, hay, cuánto, cuántos, dime";
    }

    @Override
    public String commandVerbExamples() {
        return "muestra / abre / encuentra / busca / activa / desactiva / navega / traza / despliega / retrae / enciende / apaga";
    }

    @Override
    public String queryPhraseExamples() {
        return "dónde / qué / cuánto / cuántos / hay / cuál / cuáles / en qué estación / en qué sistema";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- classify commands like interrumpe, cállate, silencio, deja de hablar to interrupt action → ");
        sb.append(InterruptCommand.ID);
        sb.append("\n");

        sb.append("- classify queries about location like cuánto dura el día aquí, dónde estamos, cuál es nuestra ubicación → ");
        sb.append(AnalyzeCurrentLocationQuery.ID);
        sb.append("\n");

        sb.append("- classify bio signals: qué planetas / aún necesitan escaneo biológico u orgánico / son aterrizables / tienen señales biológicas → ");
        sb.append(AnalyzeBioScansStarSystemQuery.ID);
        sb.append("\n");

        sb.append("- classify DISTANCE QUERIES about bio sample: distancia a la última muestra biológica, qué tan lejos está la muestra → ");
        sb.append(AnalyzeDistanceFromLastBioSampleQuery.ID);
        sb.append(" ONLY when asking HOW FAR. NEVER when navigating.\n");
        sb.append("- HARD RULE: navegación / ve a / vuela a / a la próxima muestra biológica / codex → ");
        sb.append(NavigateToBioSampleCodexEntryCommand.ID);
        sb.append(" ONLY. NEVER a distance query for navigation commands.\n");

        sb.append("- classify commands to fire on target such as: abrir fuego, fuego, ataque, atacar → ");
        sb.append(FighterFireAtWillCommand.ID);
        sb.append("\n");

        sb.append("- HARD RULE: generic scan-the-system commands explora el sistema, escanea el sistema, sondea el sistema, barre el sistema → ");
        sb.append(RunSystemScanCommand.ID);
        sb.append(" ONLY. Use ");
        sb.append(OpenFssScanSystemCommand.ID);
        sb.append(" ONLY for explicit full-spectrum terms: fss, escaneo de espectro completo, escaneo completo, abrir fss.\n");

        sb.append("- classify command modo de análisis, modo de exploración → ");
        sb.append(SwitchToAnalysisModeCommand.ID);
        sb.append("; modo de combate, combate → ");
        sb.append(SwitchToCombatModeCommand.ID);
        sb.append("\n");

        sb.append("- classify chaff/decoy commands: chaff, contramedidas → ");
        sb.append(DeployChaffCommand.ID);
        sb.append("; heat venting disipador térmico, descargar calor → ");
        sb.append(DeployHeatSinkCommand.ID);
        sb.append("\n");

        sb.append("- classify cancelar navegación, abortar ruta, desactivar navegación → ");
        sb.append(CancelNavigationCommand.ID);
        sb.append(" NEVER ");
        sb.append(ToggleLightsOnOffCommand.ID);
        sb.append("\n");

        sb.append("- classify entrar en supercruise, supercruise, activar supercruise, velocidad de la luz → ");
        sb.append(EnterSuperCruiseCommand.ID);
        sb.append("; salir de supercruise, salir de la velocidad luz, cae aquí, sal aquí → ");
        sb.append(DropFromSuperCruiseCommand.ID);
        sb.append(". NEVER confuse entrar (enter) with salir (exit).\n");

        sb.append("- classify salto al hiperespacio, salta, hipersalto, vamos, siguiente punto de ruta → ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append("- classify aumenta/incrementa la velocidad → ");
        sb.append(IncreaseSpeedCommand.ID);
        sb.append("; reduce/disminuye/baja la velocidad → ");
        sb.append(DecreaseSpeedCommand.ID);
        sb.append(". NEVER confuse the two.\n");

        sb.append("- WEAPONS: armas activas/armas listas/listos para el combate/saca las armas → ");
        sb.append(DeployHardpointsCommand.ID);
        sb.append(" (deploy hardpoints), NEVER ");
        sb.append(TransferPowerToWeaponsCommand.ID);
        sb.append(". Only energía a las armas → ");
        sb.append(TransferPowerToWeaponsCommand.ID);
        sb.append(".\n");

        sb.append("- escudos/motores/armas/sistemas + al 100/al 100%/al máximo/maximiza/potencia → transfer power: escudos or sistemas → ");
        sb.append(TransferPowerToShieldsCommand.ID);
        sb.append(", motores → ");
        sb.append(TransferPowerToEnginesCommand.ID);
        sb.append(", armas → ");
        sb.append(TransferPowerToWeaponsCommand.ID);
        sb.append(". \"al 100%\" on escudos/motores/armas is POWER, NEVER ");
        sb.append(SetSpeed100Command.ID);
        sb.append(" (which is ONLY for \"velocidad al 100\").\n");

        sb.append("- classify questions about combustible, tritio del carrier, finanzas del carrier, cuánto tiempo podemos operar → ");
        sb.append(AnalyzeFleetCarrierDataQuery.ID);
        sb.append("\n");

        sb.append("- classify carrier alcance / alcance de salto (the carrier's JUMP RANGE) → ");
        sb.append(AnalyzeFleetCarrierDataQuery.ID);
        sb.append(", NOT ");
        sb.append(AnalyzeDistanceFromFleetCarrierQuery.ID);
        sb.append(" (which is only how far the carrier is away)\n");

        sb.append("- classify distancia al carrier, qué tan lejos está el carrier, proximidad del carrier → ");
        sb.append(AnalyzeDistanceFromFleetCarrierQuery.ID);
        sb.append(" (how far the carrier is), NOT its status.\n");

        sb.append("- classify questions about distancia a la Tierra, qué tan lejos de la Tierra, qué tan lejos de la civilización → ");
        sb.append(AnalyzeDistanceFromTheBubbleQuery.ID);
        sb.append("\n");

        sb.append("- classify atraque automático, aterrizaje automático → ");
        sb.append(TaxiToLandingPadCommand.ID);
        sb.append("\n");

        sb.append("- HARD RULE: a carrier request WITHOUT the word 'escuadrón' is about the FLEET carrier; ONLY when 'escuadrón' is present use the squadron action.\n");
        sb.append("- a dónde va el carrier → ");
        sb.append(AnalyzeFleetCarrierFinalDestinationQuery.ID);
        sb.append("; a dónde va el carrier del escuadrón → ");
        sb.append(AnalyzeSquadronCarrierFinalDestinationQuery.ID);
        sb.append("\n");
        sb.append("- cuándo llega / eta del carrier → ");
        sb.append(AnalyzeFleetCarrierETAQuery.ID);
        sb.append("; del carrier del escuadrón → ");
        sb.append(AnalyzeSquadronCarrierETAQuery.ID);
        sb.append("\n");
        sb.append("- estado/finanzas del carrier → ");
        sb.append(AnalyzeFleetCarrierDataQuery.ID);
        sb.append("; del carrier del escuadrón → ");
        sb.append(AnalyzeSquadronCarrierDataQuery.ID);
        sb.append("\n");
        sb.append("- navega al carrier → ");
        sb.append(NavigateToFleetCarrierCommand.ID);
        sb.append("; navega al carrier del escuadrón → ");
        sb.append(NavigateToSquadronCarrierCommand.ID);
        sb.append("\n");

        sb.append("- classify perfil del jugador, informe del comandante, rangos/progreso del comandante → ");
        sb.append(AnalyzePlayerProfileQuery.ID);
        sb.append("\n");

        sb.append("- classify activa/desactiva todos los anuncios or todas las comunicaciones → ");
        sb.append(ToggleAllAnnouncementsCommand.ID);
        sb.append("\n");

        sb.append("- classify abre/muestra/visualiza el mapa galáctico or mapa de la galaxia → ");
        sb.append(DisplayOpenGalaxyMapCommand.ID);
        sb.append("\n");

        sb.append("- classify a reminder tied to an event (en la próxima parada, al repostar) → ");
        sb.append(SetReminderCommand.ID);
        sb.append("; ONLY with an explicit time or countdown → ");
        sb.append(SetTimedReminderCommand.ID);
        sb.append("\n");

        sb.append("- classify solicitar atraque, solicitar aterrizaje, contacta la torre, pide una plataforma → ");
        sb.append(RequestDockingCommand.ID);
        sb.append(", NEVER ");
        sb.append(NavigateToLandingZoneCommand.ID);
        sb.append("\n");

        sb.append("- classify enciende/apaga visión nocturna → ");
        sb.append(ToggleNightVisionOnOffCommand.ID);
        sb.append("; luces encendidas/apagadas, enciende/apaga las luces → ");
        sb.append(ToggleLightsOnOffCommand.ID);
        sb.append("\n");

        sb.append("- classify abrir/cerrar bodega de carga, cargo scoop → ");
        sb.append(ToggleCargoScoopCommand.ID);
        sb.append("\n");

        sb.append("- classify activa/usa célula de escudo, SCB → ");
        sb.append(DeployShieldCellCommand.ID);
        sb.append("\n");

        sb.append("- classify ingresar/establecer destino del carrier → ");
        sb.append(EnterFleetCarrierDestinationCommand.ID);
        sb.append("\n");

        sb.append("- classify NAVIGATION to the active mission: navega/traza ruta/ve a la misión activa or a la misión → ");
        sb.append(NavigateToMissionTargetCommand.ID);
        sb.append(" (a navigation command, NOT the missions query).\n");

        sb.append("- classify buscar sitio de minería / campo de asteroides / dónde minar + a commodity (oro, alexandrita, pintita, ...) → ");
        sb.append(FindMiningSiteCommand.ID);
        sb.append(" (extract the commodity verbatim as the key).\n");

        sb.append("- require very high probability match for action → ");
        sb.append(ClearActiveMissionsCommand.ID);
        sb.append("\n");

        return sb.toString();
    }
}
