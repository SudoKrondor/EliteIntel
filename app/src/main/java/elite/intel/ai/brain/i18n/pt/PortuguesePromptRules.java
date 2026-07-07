package elite.intel.ai.brain.i18n.pt;

import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;
import elite.intel.ai.brain.i18n.PromptLanguageRules;


public class PortuguesePromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "Portuguese";
    }

    @Override
    public String queryStarterExamples() {
        return "o que, onde, como, qual, quais, por que, há, tem, quanto, quantos, me diga, me diz";
    }

    @Override
    public String commandVerbExamples() {
        return "mostrar / abrir / encontrar / procurar / ativar / desativar / navegar / traçar / implantar / recolher / ligar / desligar";
    }

    @Override
    public String queryPhraseExamples() {
        return "onde / o que / quanto / quantos / há / tem / qual / quais / em que estação / em que sistema";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- classify commands like interrompa, cala a boca, silêncio, quieto, para de falar to interrupt action → ");
        sb.append(InterruptCommand.ID);
        sb.append("\n");

        sb.append("- classify queries about location like quanto tempo dura um dia aqui to action → ");
        sb.append(AnalyzeCurrentLocationQuery.ID);
        sb.append("\n");

        sb.append("- classify bio signals: Quais planetas / ainda precisam ser escaneados biologicamente ou organicamente? / são pousáveis / têm biossinais etc. → ");
        sb.append(AnalyzeBioScansStarSystemQuery.ID);
        sb.append("\n");

        sb.append("- classify DISTANCE QUERIES about bio sample: distância até a última amostra biológica, quão longe está a amostra etc. → ");
        sb.append(AnalyzeDistanceFromLastBioSampleQuery.ID);
        sb.append(" ONLY when asking HOW FAR. NEVER when navigating.\n");
        sb.append("- HARD RULE: navegação / vá até / voe até / para a próxima amostra biológica / códex → ");
        sb.append(NavigateToBioSampleCodexEntryCommand.ID);
        sb.append(" ONLY. NEVER query_distance_to_bio_sample for navigation commands.\n");

        sb.append("- classify commands to fire on target such as: abrir fogo, fogo, ataque, atacar → ");
        sb.append(FighterFireAtWillCommand.ID);
        sb.append("\n");

        sb.append("- HARD RULE: generic scan-the-system commands explore o sistema, escaneie o sistema, escanear o sistema, varrer o sistema → ");
        sb.append(RunSystemScanCommand.ID);
        sb.append(" ONLY. Use ");
        sb.append(OpenFssScanSystemCommand.ID);
        sb.append(" ONLY for explicit full-spectrum terms: fss, varredura espectral completa, varredura espectral, varredura completa, escaneamento do sistema.\n");

        sb.append("- classify command modo de análise, modo de exploração → ");
        sb.append(SwitchToAnalysisModeCommand.ID);
        sb.append("\n");

        sb.append("- classify command modo de combate, combate → ");
        sb.append(SwitchToCombatModeCommand.ID);
        sb.append("\n");

        sb.append("- classify chaff/decoy commands: contramedidas, chaff → ");
        sb.append(DeployChaffCommand.ID);
        sb.append(" but heat venting dissipador de calor, dissipar calor → ");
        sb.append(DeployHeatSinkCommand.ID);
        sb.append("\n");

        sb.append("- classify cancelar navegação, abortar rota, desligar navegação → ");
        sb.append(CancelNavigationCommand.ID);
        sb.append(" NEVER ");
        sb.append(ToggleLightsOnOffCommand.ID);
        sb.append("\n");

        sb.append("- classify squadron carrier rota final (destination) → ");
        sb.append(AnalyzeSquadronCarrierFinalDestinationQuery.ID);
        sb.append(" with squadron carrier rota (route) → ");
        sb.append(AnalyzeSquadronCarrierRouteQuery.ID);
        sb.append("\n");

        sb.append(" - classify entrar em supercruise, supercruise, ativar supercruise, velocidade da luz as ");
        sb.append(EnterSuperCruiseCommand.ID);
        sb.append(" never as ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append(" - classify salto para o hiperespaço, salto, saltar, hipersalto, para o hiperespaço, vamos lá, próximo ponto de rota as ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append(" - classify dispensar nave, mandar a nave embora, nave para a órbita as ");
        sb.append(DismissShipToOrbitCommand.ID);
        sb.append(" never as ");
        sb.append(JumpToHyperspaceCommand.ID);
        sb.append("\n");

        sb.append(" - classify questions about combustível, trítio do carrier as ");
        sb.append(AnalyzeFleetCarrierDataQuery.ID);
        sb.append("\n");

        sb.append(" - classify carrier alcance / alcance de salto (the carrier's JUMP RANGE) as ");
        sb.append(AnalyzeFleetCarrierDataQuery.ID);
        sb.append(", NOT ");
        sb.append(AnalyzeDistanceFromFleetCarrierQuery.ID);
        sb.append(" (which is only how far the carrier is away)\n");

        sb.append(" - classify questions about Distância até a Terra, quão longe está a Terra, quão longe está a civilização as ");
        sb.append(AnalyzeDistanceFromTheBubbleQuery.ID);
        sb.append("\n");

        sb.append(" - classify atracagem automática, pouso automático as ");
        sb.append(TaxiToLandingPadCommand.ID);
        sb.append("\n");

        sb.append("- HARD RULE: a carrier question WITHOUT the word 'squadron' (esquadrão) is about the FLEET carrier → ");
        sb.append(AnalyzeFleetCarrierDataQuery.ID);
        sb.append("; ONLY when 'squadron' (esquadrão) is present use the squadron action → ");
        sb.append(AnalyzeSquadronCarrierDataQuery.ID);
        sb.append("\n");

        sb.append("- require very high probability match for action → ");
        sb.append(ClearActiveMissionsCommand.ID);
        sb.append("\n");

        sb.append("- classify the carrier fuel reserve order: reserva de combustível 15000, definir reserva de combustível, reserva de trítio → ");
        sb.append(SetCarrierFuelReserveCommand.ID);
        sb.append(" (the number is the reserve amount)\n");

        sb.append("- classify abrir painel de função, painel de função, painel do comandante → ");
        sb.append(ShowCommanderPanelCommand.ID);
        sb.append(" NEVER ");
        sb.append(ShowInternalPanelCommand.ID);
        sb.append("\n");

        sb.append("- classify onde estamos, qual é a nossa localização, nossa posição, onde estou → ");
        sb.append(AnalyzeCurrentLocationQuery.ID);
        sb.append("\n");

        sb.append("- classify finanças do fleet carrier, finanças do carrier, saldo/fundos do carrier, quanto tempo podemos operar → ");
        sb.append(AnalyzeFleetCarrierDataQuery.ID);
        sb.append(" (carrier without 'squadron'/'esquadrão' = FLEET carrier)\n");

        return sb.toString();
    }
}
