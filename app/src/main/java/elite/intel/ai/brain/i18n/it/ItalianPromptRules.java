package elite.intel.ai.brain.i18n.it;

import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;
import elite.intel.ai.brain.i18n.PromptLanguageRules;


public class ItalianPromptRules implements PromptLanguageRules {

    @Override
    public String languageName() {
        return "Italian";
    }

    @Override
    public String queryStarterExamples() {
        return "cosa, dove, come, qual, quali, perché, c'è, ci sono, quanti, quanta, dimmi, abbiamo, siamo, trova, cerca, proponi, stato, distanza, posizione, livello, informazioni, dettagli, rapporto, analisi, studia";
    }

    @Override
    public String commandVerbExamples() {
        return "apri / mostra / visualizza / attiva / disattiva / naviga / traccia / punta / mira / seleziona / blocca / imposta / definisci / regola / schiera / ritira / accendi / spegni / entra / esci / configura";
    }

    @Override
    public String queryPhraseExamples() {
        return "dove / cosa / quanto / quanti / ci sono / qual è lo stato / dammi le informazioni / dammi i dettagli / rapporto / quanti salti / che distanza / che rotta / ci sono segnali / cosa trasportiamo / siamo arrivati / quale stazione";
    }

    @Override
    public String disambiguationHints() {
        StringBuilder sb = new StringBuilder();

        sb.append("- Classify Italian as INTENT + SUBJECT + COMPLEMENT; never use one keyword alone.\n");
        sb.append("- INFO: rapporto/stato/informazioni/quanti/dove/trova/cerca. ACTION: apri/attiva/disattiva/naviga/traccia/punta/seleziona/blocca/regola/schiera.\n");
        sb.append("- Require explicit destructive intent for ").append(ClearActiveMissionsCommand.ID).append(".\n");

        sb.append("- COPY ACTION NAMES EXACTLY. Never invent synonyms or rename actions. Fleet carrier status is exactly ");
        sb.append(AnalyzeFleetCarrierDataQuery.ID);
        sb.append(". Fighter focus is exactly ");
        sb.append(FighterAttackTargetCommand.ID);
        sb.append(".\n");

        sb.append("- stai zitto/chiudi la bocca/silenzio/smettila di parlare → ");
        sb.append(InterruptCommand.ID);
        sb.append(".\n");

        sb.append("- estrai/abbassa/schiera + carrello → ");
        sb.append(DeployLandingGearCommand.ID);

        sb.append("; ritira/alza/rientra + carrello → ");
        sb.append(RetractLandingGearCommand.ID);
        sb.append("; estrai or schiera never retracts.\n");

        sb.append("- potenza/energia/priorità + scudi or sistemi → ");
        sb.append(TransferPowerToShieldsCommand.ID);
        sb.append("; motori → ").append(TransferPowerToEnginesCommand.ID);
        sb.append("; armi → ").append(TransferPowerToWeaponsCommand.ID);
        sb.append("; bilancia/reset/distribuisci → ").append(EqualizePowerCommand.ID).append(".\n");
        sb.append("- With power words, sistemi=ship SYS. With rotta/mappa/FSD/scansione/stazione/pianeta, sistema=star system. \"mira al sistema di supporto vitale\" → ").append(TargetSubsystemCommand.ID).append(".\n");

        sb.append("- disattiva/silenzia + tutte le notifiche/annunci vocali → ");
        sb.append(ToggleAllAnnouncementsCommand.ID);
        sb.append("; never ");
        sb.append(ToggleDiscoveryAnnouncementsCommand.ID);
        sb.append(".\n");
        sb.append("- apri/mostra/visualizza + pannello/comandi + caccia → ");
        sb.append(ShowFighterPanelCommand.ID);
        sb.append("; schiera/lancia/fai uscire il caccia → ");
        sb.append(DeployFighterCommand.ID);
        sb.append(".\n");

        sb.append("- fleet carrier + stato/rapporto/finanze/fondi/autonomia/portata → ");
        sb.append(AnalyzeFleetCarrierDataQuery.ID);
        sb.append(". squadron carrier + stato/rapporto/finanze/fondi/carburante/trizio → ");
        sb.append(AnalyzeSquadronCarrierDataQuery.ID);
        sb.append("; rotta/itinerario required for ");
        sb.append(AnalyzeSquadronCarrierRouteQuery.ID);
        sb.append(".\n");
        sb.append("- HARD RULE: if the words squadron/squadrone appear anywhere in a carrier request (e.g. fondi dello squadron carrier, stato dello squadron carrier), use the squadron carrier action → ");
        sb.append(AnalyzeSquadronCarrierDataQuery.ID);
        sb.append(", NEVER the fleet carrier action ");
        sb.append(AnalyzeFleetCarrierDataQuery.ID);
        sb.append(".\n");
        sb.append("- ").append(FindNearestFleetCarrierCommand.ID).append(" requires explicit \"più vicina\"/\"nearest\" or trova/cerca; e.g. \"trova/cerca portanavi più vicina\". ");
        sb.append("Any other reference to the fleet carrier (rotta verso, vai verso, raggiungi, torna, dirigiti, portami alla fleet carrier/portanavi) → ").append(NavigateToFleetCarrierCommand.ID).append(".\n");

        sb.append("- attiva alone → ").append(ActivateUiControlCommand.ID);
        sb.append("; attiva pilota automatico/taxi → ").append(TaxiToLandingPadCommand.ID);
        sb.append("; attiva/avvia salto or reattore FSD → ").append(JumpToHyperspaceCommand.ID).append(".\n");
        sb.append("- ferma/fermati/alt alone → ").append(SetSpeedZeroCommand.ID);
        sb.append("; zitto/silenzio/smettila di parlare → ").append(InterruptCommand.ID).append(".\n");
        sb.append("- INFO + itinerario/salti → ").append(AnalyzeRouterQuery.ID);
        sb.append("; ACTION traccia/naviga/imposta/definisci uses the matching navigation action.\n");

        sb.append("- mira/seleziona/blocca + destinazione/prossima tappa → ");
        sb.append(TargetDestinationCommand.ID);
        sb.append("; INFO about destinazione FSD/prossima destinazione → ");
        sb.append(AnalyzeFsdTargetQuery.ID);
        sb.append(". \"mira\" alone has no action.\n");
        sb.append("- ");
        sb.append(SelectFireGroupByNatoCommand.ID);
        sb.append(" requires explicit gruppo di fuoco/gruppo armi/fire group + NATO identifier; never for destination/route.\n");
        sb.append("- mira + gregario/compagno d'ala → wingman action; mira + ship component → ").append(TargetSubsystemCommand.ID);
        sb.append("; il caccia attacca il mio bersaglio → fighter-focus action.\n");

        sb.append("- bio nel sistema/quali pianeti → ");
        sb.append(AnalyzeBioScansStarSystemQuery.ID);
        sb.append("; bio qui/su questo pianeta → ");
        sb.append(AnalyzeBioSamplesPlanetSurfaceQuery.ID);
        sb.append("; geologia/segnali geologici → ").append(AnalyzeGeologyInStarSystemQuery.ID).append(".\n");
        sb.append("- dove attraccare/stazioni → ").append(AnalyzeStationsQuery.ID);
        sb.append("; mercati/commercio without commodity → ").append(AnalyzeMarketsQuery.ID).append(".\n");

        sb.append("- Any \"scoop\"/\"vano di carico\" → ").append(ToggleCargoScoopCommand.ID);
        sb.append("; pannello/inventario/cargo → ").append(ShowInventoryPanelCommand.ID).append(".\n");

        sb.append("- HARD RULE: generic scan-the-system commands honk, esplora il sistema, esegui una scansione del sistema → ");
        sb.append(RunSystemScanCommand.ID);
        sb.append(" ONLY. Use ");
        sb.append(OpenFssScanSystemCommand.ID);
        sb.append(" ONLY for explicit full-spectrum terms: apri FSS, scansione a spettro completo, FSS.\n");

        sb.append("- INFO noun phrase \"inventario/lista/scorta dei materiali\" without apri/mostra/visualizza and without pannello → ");
        sb.append(AnalyseMaterialsQuery.ID);
        sb.append(". Only ACTION apri/mostra/visualizza + pannello/inventario della nave/cargo → ");
        sb.append(ShowInventoryPanelCommand.ID);
        sb.append(". Never open the inventory panel for \"inventario dei materiali\".\n");
        sb.append("- apri/mostra/visualizza + stato/condizione della nave → ");
        sb.append(ShowStatusPanelCommand.ID);
        sb.append("; INFO moduli/equipaggiamento/danni/specifiche → ").append(AnalyzeShipLoadoutQuery.ID).append(".\n");
        sb.append("- full cargo list → ").append(AnalyzeCargoHoldQuery.ID);
        sb.append("; specific cargo commodity or engineering material → ").append(AnalyseMaterialsQuery.ID).append(".\n");
        sb.append("- abbiamo/hai/è equipaggiata + installed ship module (raccoglitore di carburante, scudo, sensori, propulsori, ecc.) → ");
        sb.append(AnalyzeShipLoadoutQuery.ID);
        sb.append("; never ").append(AnalyseMaterialsQuery.ID).append(".\n");
        sb.append("- concentrati sul mio bersaglio/il caccia attacca il mio bersaglio → ");
        sb.append(FighterAttackTargetCommand.ID);
        sb.append(".\n");

        sb.append("- aumenta/incrementa/alza la velocità (di X) → ").append(IncreaseSpeedCommand.ID);
        sb.append("; diminuisci/riduci/abbassa la velocità → ").append(DecreaseSpeedCommand.ID);
        sb.append(". NEVER confuse aumenta with diminuisci.\n");

        sb.append("- esci/uscire/scendi/droppa da supercruise or velocità luce → ").append(DropFromSuperCruiseCommand.ID);
        sb.append("; entra/entrare in supercruise or velocità luce → ").append(EnterSuperCruiseCommand.ID);
        sb.append(". NEVER confuse esci (exit) with entra (enter).\n");

        sb.append("- armi attive/armi pronte/pronti al combattimento/tira fuori le armi/schiera le armi → ").append(DeployHardpointsCommand.ID);
        sb.append(" (deploy hardpoints), NEVER ").append(TransferPowerToWeaponsCommand.ID);
        sb.append(". Only potenza/energia alle armi → ").append(TransferPowerToWeaponsCommand.ID).append(".\n");

        sb.append("- scudi/sistemi/motori/armi + al 100/al 100%/al 100 per cento/al massimo/massimizza/potenzia → transfer power: scudi or sistemi → ").append(TransferPowerToShieldsCommand.ID);
        sb.append(", motori → ").append(TransferPowerToEnginesCommand.ID);
        sb.append(", armi → ").append(TransferPowerToWeaponsCommand.ID);
        sb.append(". \"al 100%\"/\"al 100 per cento\" on scudi/motori/armi is POWER, NEVER ").append(SetSpeed100Command.ID);
        sb.append(" (which is ONLY for \"velocità al 100\").\n");

        sb.append("- apri FSS, apri FSS ed esegui scansione, scansione a spettro completo → ").append(OpenFssScanSystemCommand.ID).append(".\n");

        sb.append("- vai/naviga/dirigiti alla voce del codex, al prossimo codex, al prossimo campione biologico/organico → ").append(NavigateToBioSampleCodexEntryCommand.ID).append(".\n");

        sb.append("- calcola rotta verso la stella di neutroni, rotta neutronica, percorso a stelle di neutroni → ").append(CalculateNeutronStarRouteCommand.ID).append(".\n");

        sb.append("- attiva/disattiva + tutte le comunicazioni/tutti gli annunci/tutte le notifiche → ").append(ToggleAllAnnouncementsCommand.ID).append(".\n");

        sb.append("- promemoria legato a un evento (es. al prossimo stop, al prossimo rifornimento) → ").append(SetReminderCommand.ID);
        sb.append("; SOLO con un orario o conto alla rovescia esplicito → ").append(SetTimedReminderCommand.ID).append(".\n");

        sb.append("- visualizza/apri/mostra la mappa della galassia or mappa galattica → ").append(DisplayOpenGalaxyMapCommand.ID).append(".\n");

        sb.append("- CARRIER: \"squadriglia\"/\"squadrone\" = squadron carrier; \"portanavi\"/\"fleet carrier\" WITHOUT squadriglia/squadrone = FLEET carrier.\n");
        sb.append("- dove sta andando/destinazione finale della portanavi della squadriglia/dello squadrone → ").append(AnalyzeSquadronCarrierFinalDestinationQuery.ID);
        sb.append("; della portanavi (fleet) → ").append(AnalyzeFleetCarrierFinalDestinationQuery.ID).append(".\n");
        sb.append("- tempo di arrivo/quando arriva la portanavi della squadriglia/dello squadrone → ").append(AnalyzeSquadronCarrierETAQuery.ID).append(".\n");
        sb.append("- dirigiti/vai verso la portanavi della squadriglia/dello squadrone → ").append(NavigateToSquadronCarrierCommand.ID);
        sb.append("; dirigiti/vai alla portanavi or fleet carrier (senza squadriglia/squadrone) → ").append(NavigateToFleetCarrierCommand.ID).append(".\n");

        sb.append("- vai/traccia il percorso/dirigiti alla missione attiva or alla missione → ").append(NavigateToMissionTargetCommand.ID);
        sb.append(" (navigation, NOT the missions query).\n");

        sb.append("- richiesta di atterraggio, richiedi atterraggio, contatta la torre di controllo, richiedi una piattaforma di atterraggio → ").append(RequestDockingCommand.ID);
        sb.append(", NEVER ").append(NavigateToLandingZoneCommand.ID).append(".\n");

        sb.append("- accendi/spegni visione notturna or night vision → ").append(ToggleNightVisionOnOffCommand.ID).append(".\n");
        sb.append("- fari accesi/spenti, accendi/spegni i fari → ").append(ToggleLightsOnOffCommand.ID).append(".\n");
        sb.append("- attiva/scarica heat sink or dissipatore di calore → ").append(DeployHeatSinkCommand.ID).append(".\n");
        sb.append("- attiva/usa SCB, cella scudo, cella di ricarica scudi → ").append(DeployShieldCellCommand.ID).append(".\n");
        sb.append("- apri/chiudi vano di carico or scoop → ").append(ToggleCargoScoopCommand.ID);
        sb.append(", NEVER il carrello/landing gear ").append(RetractLandingGearCommand.ID).append(".\n");

        sb.append("- WEAPONS: armi attive/armi pronte/pronti al combattimento → ").append(DeployHardpointsCommand.ID);
        sb.append(" (deploy hardpoints); massimizza/potenzia armi, armi al massimo/al massimo livello/al 100%/al 100 per cento → ").append(TransferPowerToWeaponsCommand.ID);
        sb.append(" (power). \"attive/pronte\" = hardpoints; \"massimizza/al massimo/al 100%\" = power. NEVER confuse them.\n");
        return sb.toString();
    }
}
