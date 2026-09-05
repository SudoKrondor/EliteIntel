package elite.intel.ai.hands;

/**
 * Represents the available actions and their associated game commands for
 * the in-game control system. The class helps in mapping specific actions
 * to their respective commands, which are linked with identifiers and
 * associated classes for controlling the game behavior.
 */
public class Bindings {

    /**
     * Reads at the declaration site as {@code BINDING_GALAXY_MAP("GalaxyMapOpen", DRIVEN)}: EliteIntel
     * presses this control itself. Declared on the outer class because an enum constant may only be
     * handed a compile-time constant, not a field of its own enum.
     */
    static final boolean DRIVEN = true;

    /**
     * Enumeration representing various game commands and their associated attributes.
     * Each game command is linked with a binding name, an action, and a controller class.
     * This enum is used to map specific in-game actions to identifiers for processing and handling user inputs.
     * <p>
     * The list is the whole of Elite's control set, not the part EliteIntel uses: the bindings editor
     * shows every control, and a custom command may tap any of them. The {@code DRIVEN} entries are the
     * subset a built-in command presses, and they are the only ones the startup missing-binding warning
     * speaks about - see {@link BindingsMonitor#requiredGameBindings()}.
     */
    public enum GameCommand {

        BINDING_UIFOCUS("UIFocus"),

        /// ------------------------------------------------------------------------------------------------------------
        /// mappings for customCommands
        BINDING_TOGGLE_CARGO_SCOOP("ToggleCargoScoop", DRIVEN),
        BINDING_TOGGLE_CARGO_SCOOP_BUGGY("ToggleCargoScoop_Buggy", DRIVEN),
        BINDING_HARDPOINTS_TOGGLE("DeployHardpointToggle", DRIVEN),
        BINDING_FOCUS_COMMS_PANEL("FocusCommsPanel", DRIVEN),
        BINDING_FOCUS_COMMS_PANEL_BUGGY("FocusCommsPanel_Buggy", DRIVEN),
        BINDING_FOCUS_LEFT_PANEL("FocusLeftPanel", DRIVEN),
        BINDING_FOCUS_CONTACTS_PANEL_BUGGY("FocusLeftPanel_Buggy", DRIVEN),
        BINDING_FOCUS_LEFT_PANEL_BUGGY("FocusLeftPanel_Buggy", DRIVEN),
        BINDING_FOCUS_INTERNAL_PANEL("FocusRightPanel", DRIVEN),
        BINDING_FOCUS_INTERNAL_PANEL_BUGGY("FocusRightPanel_Buggy", DRIVEN),
        BINDING_FOCUS_STATUS_PANEL("FocusRightPanel", DRIVEN),
        BINDING_FOCUS_STATUS_PANEL_BUGGY("FocusRightPanel_Buggy", DRIVEN),
        BINDING_FOCUS_ROLE_PANEL_BUGGY("FocusRadarPanel_Buggy", DRIVEN),
        BINDING_FOCUS_ROLE_PANEL("FocusRadarPanel", DRIVEN),
        BINDING_FOCUS_LOADOUT_PANEL_BUGGY("FocusRadarPanel_Buggy", DRIVEN),
        BINDING_LANDING_GEAR_TOGGLE("LandingGearToggle", DRIVEN),
        BINDING_RESET_POWER_DISTRIBUTION("ResetPowerDistribution", DRIVEN),
        BINDING_RESET_POWER_DISTRIBUTION_BUGGY("ResetPowerDistribution_Buggy", DRIVEN),

        BINDING_INCREASE_ENGINES_POWER("IncreaseEnginesPower", DRIVEN),
        BINDING_INCREASE_SYSTEMS_POWER("IncreaseSystemsPower", DRIVEN),
        BINDING_INCREASE_SHIELDS_POWER("IncreaseSystemsPower", DRIVEN),
        BINDING_INCREASE_WEAPONS_POWER("IncreaseWeaponsPower", DRIVEN),

        BINDING_INCREASE_ENGINES_POWER_BUGGY("IncreaseEnginesPower_Buggy", DRIVEN),
        BINDING_INCREASE_SYSTEMS_POWER_BUGGY("IncreaseSystemsPower_Buggy", DRIVEN),
        BINDING_INCREASE_SHIELDS_POWER_BUGGY("IncreaseSystemsPower_Buggy", DRIVEN),
        BINDING_INCREASE_WEAPONS_POWER_BUGGY("IncreaseWeaponsPower_Buggy", DRIVEN),

        BINDING_GALAXY_MAP("GalaxyMapOpen", DRIVEN),
        BINDING_GALAXY_MAP_BUGGY("GalaxyMapOpen_Buggy", DRIVEN),
        BINDING_GALAXY_MAP_HUMANOID("GalaxyMapOpen_Humanoid", DRIVEN),

        BINDING_LOCAL_MAP("SystemMapOpen", DRIVEN),
        BINDING_LOCAL_MAP_BUGGY("SystemMapOpen_Buggy", DRIVEN),
        BINDING_SYSTEM_MAP_HUMANOID("SystemMapOpen_Humanoid", DRIVEN),

        BINDING_EXPLORATION_FSSDISCOVERY_SCAN("ExplorationFSSEnter", DRIVEN),

        BINDING_CAM_ZOOM_IN("CamZoomIn", DRIVEN),
        BINDING_CAM_ZOOM_OUT("CamZoomOut"),
        BINDING_EXIT_KEY("UI_Back", DRIVEN),
        BINDING_UI_DOWN("UI_Down", DRIVEN),
        BINDING_UI_LEFT("UI_Left", DRIVEN),
        BINDING_UI_RIGHT("UI_Right", DRIVEN),
        BINDING_UI_SELECT("UI_Select", DRIVEN),
        BINDING_UI_TOGGLE("UI_Toggle"),
        BINDING_UI_UP("UI_Up", DRIVEN),

        BINDING_EXIT_SUPERCRUISE("Supercruise", DRIVEN),
        BINDING_JUMP_TO_HYPERSPACE("Hyperspace", DRIVEN),
        BINDING_ENTER_SUPERCRUISE("Supercruise", DRIVEN),


        //TODO: convert to custom
        BINDING_ACTIVATE_ANALYSIS_MODE("PlayerHUDModeToggle", DRIVEN),
        BINDING_ACTIVATE_ANALYSIS_MODE_BUGGY("PlayerHUDModeToggle_Buggy", DRIVEN),
        BINDING_ACTIVATE_COMBAT_MODE("PlayerHUDModeToggle", DRIVEN),
        BINDING_ACTIVATE_COMBAT_MODE_BUGGY("PlayerHUDModeToggle_Buggy", DRIVEN),


        /// ------------------------------------------------------------------------------------------------------------
        /// basic commands
        BINDING_ACTIVATE("UI_Select", DRIVEN),
        BINDING_NIGHT_VISION_TOGGLE("NightVisionToggle", DRIVEN),
        BINDING_CYCLE_NEXT_PAGE("CycleNextPage", DRIVEN),
        BINDING_CYCLE_NEXT_PANEL("CycleNextPanel", DRIVEN),
        BINDING_CYCLE_PREVIOUS_PAGE("CyclePreviousPage", DRIVEN),
        BINDING_CYCLE_PREVIOUS_PANEL("CyclePreviousPanel", DRIVEN),
        BINDING_HEAD_LOOK_RESET("HeadLookReset", DRIVEN),

        BINDING_CYCLE_NEXT_SUBSYSTEM("CycleNextSubsystem", DRIVEN),
        BINDING_CYCLE_PREVIOUS_SUBSYSTEM("CyclePreviousSubsystem", DRIVEN),

        BINDING_DEPLOY_HEAT_SINK("DeployHeatSink", DRIVEN),
        BINDING_USE_SHIELD_CELL("UseShieldCell", DRIVEN),
        BINDING_FIRE_CHAFF_LAUNCHER("FireChaffLauncher", DRIVEN),
        BINDING_DRIVE_ASSIST("ToggleDriveAssist", DRIVEN),
        BINDING_EXPLORATION_FSSQUIT("ExplorationFSSQuit", DRIVEN),

        EXPLORATION_SAAEXIT_THIRD_PERSON("ExplorationSAAExitThirdPerson"),

        BINDING_RADAR_DECREASE_RANGE("RadarDecreaseRange"),
        BINDING_RADAR_INCREASE_RANGE("RadarIncreaseRange"),

        BINDING_RECALL_DISMISS_SHIP("RecallDismissShip", DRIVEN),

        BINDING_REQUEST_DEFENSIVE_BEHAVIOUR("OrderDefensiveBehaviour", DRIVEN),
        BINDING_REQUEST_FOCUS_TARGET("OrderFocusTarget", DRIVEN),
        BINDING_REQUEST_HOLD_FIRE("OrderHoldFire", DRIVEN),
        BINDING_REQUEST_REQUEST_DOCK("OrderRequestDock", DRIVEN),
        OPEN_ORDERS("OpenOrders"),

        BINDING_SELECT_TARGETS_TARGET("SelectTargetsTarget"),

        BINDING_PLANETARY_APPROACH_SPEED75("SetSpeed25", DRIVEN),
        BINDING_SET_SPEED25("SetSpeed25", DRIVEN),
        BINDING_SET_SPEED50("SetSpeed50", DRIVEN),
        BINDING_SET_SPEED75("SetSpeed75", DRIVEN),
        BINDING_SET_OPTIMAL_SPEED("SetSpeed75", DRIVEN),
        BINDING_SET_SPEED100("SetSpeed100", DRIVEN),
        BINDING_SET_SPEED_ZERO("SetSpeedZero", DRIVEN),
        BINDING_INCREASE_SPEED("ForwardKey", DRIVEN),
        BINDING_DECREASE_SPEED("BackwardKey", DRIVEN),

        BINDING_SHIP_LIGHTS_TOGGLE("ShipSpotLightToggle", DRIVEN),
        BINDING_BUGGY_LIGHTS_TOGGLE("HeadlightsBuggyButton", DRIVEN),

        BINDING_SELECT_HIGHEST_THREAT("SelectHighestThreat", DRIVEN),

        BINDING_TARGET_NEXT_ROUTE_SYSTEM("TargetNextRouteSystem", DRIVEN),
        BINDING_ON_FOOT_WHEEL("HumanoidOpenAccessPanelButton", DRIVEN),
        BINDING_TARGET_WINGMAN0("TargetWingman0", DRIVEN),
        BINDING_TARGET_WINGMAN1("TargetWingman1", DRIVEN),
        BINDING_TARGET_WINGMAN2("TargetWingman2", DRIVEN),
        BINDING_WING_NAV_LOCK("WingNavLock", DRIVEN),

        /// These two trigger must operate only in analysis mode for safety - hands off the trigger!
        BINDING_PRIMARY_FIRE("PrimaryFire", DRIVEN),
        BINDING_SECONDARY_FIRE("SecondaryFire", DRIVEN),
        BINDING_CYCLE_NEXT_FIRE_GROUP("CycleFireGroupNext", DRIVEN),
        BINDING_AUTO_BREAK_BUGGY_BUTTON("AutoBreakBuggyButton"),
        BINDING_BACKWARD_KEY("BackwardKey", DRIVEN),
        BINDING_BACKWARD_THRUST_BUTTON("BackwardThrustButton"),
        BINDING_BACKWARD_THRUST_BUTTON_LANDING("BackwardThrustButton_Landing"),
        BINDING_BLOCK_MOUSE_DECAY("BlockMouseDecay"),
        BINDING_BUGGY_CYCLE_FIRE_GROUP_NEXT("BuggyCycleFireGroupNext"),
        BINDING_BUGGY_CYCLE_FIRE_GROUP_PREVIOUS("BuggyCycleFireGroupPrevious"),
        BINDING_BUGGY_PITCH_DOWN_BUTTON("BuggyPitchDownButton"),
        BINDING_BUGGY_PITCH_UP_BUTTON("BuggyPitchUpButton"),
        BINDING_BUGGY_PRIMARY_FIRE_BUTTON("BuggyPrimaryFireButton"),
        BINDING_BUGGY_ROLL_LEFT_BUTTON("BuggyRollLeftButton"),
        BINDING_BUGGY_ROLL_RIGHT_BUTTON("BuggyRollRightButton"),
        BINDING_BUGGY_SECONDARY_FIRE_BUTTON("BuggySecondaryFireButton"),
        BINDING_BUGGY_REVERSE_THROTTLE_INPUT("BuggyToggleReverseThrottleInput"),
        BINDING_BUGGY_TURRET_PITCH_DOWN_BUTTON("BuggyTurretPitchDownButton"),
        BINDING_BUGGY_TURRET_PITCH_UP_BUTTON("BuggyTurretPitchUpButton"),
        BINDING_BUGGY_TURRET_YAW_LEFT_BUTTON("BuggyTurretYawLeftButton"),
        BINDING_BUGGY_TURRET_YAW_RIGHT_BUTTON("BuggyTurretYawRightButton"),
        BINDING_CAM_PITCH_DOWN("CamPitchDown"),
        BINDING_CAM_PITCH_UP("CamPitchUp"),
        BINDING_CAM_TRANSLATE_BACKWARD("CamTranslateBackward"),
        BINDING_CAM_TRANSLATE_DOWN("CamTranslateDown"),
        BINDING_CAM_TRANSLATE_FORWARD("CamTranslateForward"),
        BINDING_CAM_TRANSLATE_LEFT("CamTranslateLeft"),
        BINDING_CAM_TRANSLATE_RIGHT("CamTranslateRight"),
        BINDING_CAM_TRANSLATE_UP("CamTranslateUp"),
        BINDING_CAM_TRANSLATE_ZHOLD("CamTranslateZHold"),
        BINDING_CAM_YAW_LEFT("CamYawLeft"),
        BINDING_CAM_YAW_RIGHT("CamYawRight"),
        BINDING_CHARGE_ECM("ChargeECM"),
        BINDING_COMMANDER_CREATOR_REDO("CommanderCreator_Redo"),
        BINDING_COMMANDER_CREATOR_ROTATION_MOUSE_TOGGLE("CommanderCreator_Rotation_MouseToggle"),
        BINDING_COMMANDER_CREATOR_UNDO("CommanderCreator_Undo"),
        BINDING_CYCLE_FIRE_GROUP_NEXT("CycleFireGroupNext", DRIVEN),
        BINDING_CYCLE_FIRE_GROUP_PREVIOUS("CycleFireGroupPrevious"),
        BINDING_CYCLE_NEXT_HOSTILE_TARGET("CycleNextHostileTarget"),
        BINDING_CYCLE_NEXT_TARGET("CycleNextTarget"),
        BINDING_CYCLE_PREVIOUS_HOSTILE_TARGET("CyclePreviousHostileTarget"),
        BINDING_CYCLE_PREVIOUS_TARGET("CyclePreviousTarget"),
        BINDING_DECREASE_SPEED_BUTTON_MAX("DecreaseSpeedButtonMax"),
        BINDING_DEPLOY_HARDPOINT_TOGGLE("DeployHardpointToggle", DRIVEN),
        BINDING_DISABLE_ROTATION_CORRECT_TOGGLE("DisableRotationCorrectToggle"),
        BINDING_DOWN_THRUST_BUTTON("DownThrustButton"),
        BINDING_DOWN_THRUST_BUTTON_LANDING("DownThrustButton_Landing"),
        BINDING_EJECT_ALL_CARGO("EjectAllCargo"),
        BINDING_EJECT_ALL_CARGO_BUGGY("EjectAllCargo_Buggy"),
        BINDING_ENGINE_COLOUR_TOGGLE("EngineColourToggle"),
        BINDING_EXIT_SETTLEMENT_PLACEMENT_CAMERA("ExitSettlementPlacementCamera"),
        BINDING_EXPLORATION_FSSCAMERA_PITCH_DECREASE_BUTTON("ExplorationFSSCameraPitchDecreaseButton"),
        BINDING_EXPLORATION_FSSCAMERA_PITCH_INCREASE_BUTTON("ExplorationFSSCameraPitchIncreaseButton"),
        BINDING_EXPLORATION_FSSCAMERA_YAW_DECREASE_BUTTON("ExplorationFSSCameraYawDecreaseButton"),
        BINDING_EXPLORATION_FSSCAMERA_YAW_INCREASE_BUTTON("ExplorationFSSCameraYawIncreaseButton"),
        BINDING_EXPLORATION_FSSENTER("ExplorationFSSEnter", DRIVEN),
        BINDING_EXPLORATION_FSSMINI_ZOOM_IN("ExplorationFSSMiniZoomIn"),
        BINDING_EXPLORATION_FSSMINI_ZOOM_OUT("ExplorationFSSMiniZoomOut"),
        BINDING_EXPLORATION_FSSRADIO_TUNING_X_DECREASE("ExplorationFSSRadioTuningX_Decrease"),
        BINDING_EXPLORATION_FSSRADIO_TUNING_X_INCREASE("ExplorationFSSRadioTuningX_Increase"),
        BINDING_EXPLORATION_FSSSHOW_HELP("ExplorationFSSShowHelp"),
        BINDING_EXPLORATION_FSSTARGET("ExplorationFSSTarget"),
        BINDING_EXPLORATION_FSSZOOM_IN("ExplorationFSSZoomIn"),
        BINDING_EXPLORATION_FSSZOOM_OUT("ExplorationFSSZoomOut"),
        BINDING_EXPLORATION_SAACHANGE_SCANNED_AREA_VIEW_TOGGLE("ExplorationSAAChangeScannedAreaViewToggle"),
        BINDING_EXPLORATION_SAAEXIT_THIRD_PERSON("ExplorationSAAExitThirdPerson"),
        BINDING_EXPLORATION_SAANEXT_GENUS("ExplorationSAANextGenus"),
        BINDING_EXPLORATION_SAAPREVIOUS_GENUS("ExplorationSAAPreviousGenus"),
        BINDING_FSTOP_DEC("FStopDec"),
        BINDING_FSTOP_INC("FStopInc"),
        BINDING_FIX_CAMERA_RELATIVE_TOGGLE("FixCameraRelativeToggle"),
        BINDING_FIX_CAMERA_WORLD_TOGGLE("FixCameraWorldToggle"),
        BINDING_FOCUS_COMMS_PANEL_HUMANOID("FocusCommsPanel_Humanoid"),
        BINDING_FOCUS_RADAR_PANEL("FocusRadarPanel", DRIVEN),
        BINDING_FOCUS_RADAR_PANEL_BUGGY("FocusRadarPanel_Buggy", DRIVEN),
        BINDING_FOCUS_RIGHT_PANEL("FocusRightPanel", DRIVEN),
        BINDING_FOCUS_RIGHT_PANEL_BUGGY("FocusRightPanel_Buggy", DRIVEN),
        BINDING_FORWARD_KEY("ForwardKey", DRIVEN),
        BINDING_FORWARD_THRUST_BUTTON("ForwardThrustButton"),
        BINDING_FORWARD_THRUST_BUTTON_LANDING("ForwardThrustButton_Landing"),
        BINDING_FREE_CAM_SPEED_DEC("FreeCamSpeedDec"),
        BINDING_FREE_CAM_SPEED_INC("FreeCamSpeedInc"),
        BINDING_FREE_CAM_HUD("FreeCamToggleHUD"),
        BINDING_FRIENDS_MENU("FriendsMenu"),
        BINDING_GALAXY_MAP_HOME("GalaxyMapHome"),
        BINDING_GALNET_AUDIO_CLEAR_QUEUE("GalnetAudio_ClearQueue"),
        BINDING_GALNET_AUDIO_PLAY_PAUSE("GalnetAudio_Play_Pause"),
        BINDING_GALNET_AUDIO_SKIP_BACKWARD("GalnetAudio_SkipBackward"),
        BINDING_GALNET_AUDIO_SKIP_FORWARD("GalnetAudio_SkipForward"),
        BINDING_HMDRESET("HMDReset"),
        BINDING_HEAD_LOOK_PITCH_DOWN("HeadLookPitchDown"),
        BINDING_HEAD_LOOK_PITCH_UP("HeadLookPitchUp"),
        BINDING_HEAD_LOOK_TOGGLE("HeadLookToggle"),
        BINDING_HEAD_LOOK_BUGGY("HeadLookToggle_Buggy"),
        BINDING_HEAD_LOOK_YAW_LEFT("HeadLookYawLeft"),
        BINDING_HEAD_LOOK_YAW_RIGHT("HeadLookYawRight"),
        BINDING_HEADLIGHTS_BUGGY_BUTTON("HeadlightsBuggyButton", DRIVEN),
        BINDING_HUMANOID_BACKWARD_BUTTON("HumanoidBackwardButton"),
        BINDING_HUMANOID_BATTERY("HumanoidBattery"),
        BINDING_HUMANOID_CLEAR_AUTHORITY_LEVEL("HumanoidClearAuthorityLevel"),
        BINDING_HUMANOID_CONFLICT_CONTEXTUAL_UIBUTTON("HumanoidConflictContextualUIButton"),
        BINDING_HUMANOID_CROUCH_BUTTON("HumanoidCrouchButton"),
        BINDING_HUMANOID_EMOTE_SLOT1("HumanoidEmoteSlot1"),
        BINDING_HUMANOID_EMOTE_SLOT2("HumanoidEmoteSlot2"),
        BINDING_HUMANOID_EMOTE_SLOT3("HumanoidEmoteSlot3"),
        BINDING_HUMANOID_EMOTE_SLOT4("HumanoidEmoteSlot4"),
        BINDING_HUMANOID_EMOTE_SLOT5("HumanoidEmoteSlot5"),
        BINDING_HUMANOID_EMOTE_SLOT6("HumanoidEmoteSlot6"),
        BINDING_HUMANOID_EMOTE_SLOT7("HumanoidEmoteSlot7"),
        BINDING_HUMANOID_EMOTE_SLOT8("HumanoidEmoteSlot8"),
        BINDING_HUMANOID_EMOTE_WHEEL_BUTTON("HumanoidEmoteWheelButton"),
        BINDING_HUMANOID_FORWARD_BUTTON("HumanoidForwardButton"),
        BINDING_HUMANOID_HEALTH_PACK("HumanoidHealthPack"),
        BINDING_HUMANOID_HIDE_WEAPON_BUTTON("HumanoidHideWeaponButton"),
        BINDING_HUMANOID_ITEM_WHEEL_BUTTON("HumanoidItemWheelButton"),
        BINDING_HUMANOID_ITEM_WHEEL_BUTTON_XLEFT("HumanoidItemWheelButton_XLeft"),
        BINDING_HUMANOID_ITEM_WHEEL_BUTTON_XRIGHT("HumanoidItemWheelButton_XRight"),
        BINDING_HUMANOID_ITEM_WHEEL_BUTTON_YDOWN("HumanoidItemWheelButton_YDown"),
        BINDING_HUMANOID_ITEM_WHEEL_BUTTON_YUP("HumanoidItemWheelButton_YUp"),
        BINDING_HUMANOID_JUMP_BUTTON("HumanoidJumpButton"),
        BINDING_HUMANOID_MELEE_BUTTON("HumanoidMeleeButton"),
        BINDING_HUMANOID_ACCESS_PANEL_BUTTON("HumanoidOpenAccessPanelButton", DRIVEN),
        BINDING_HUMANOID_PING("HumanoidPing"),
        BINDING_HUMANOID_PITCH_DOWN_BUTTON("HumanoidPitchDownButton"),
        BINDING_HUMANOID_PITCH_UP_BUTTON("HumanoidPitchUpButton"),
        BINDING_HUMANOID_PRIMARY_FIRE_BUTTON("HumanoidPrimaryFireButton"),
        BINDING_HUMANOID_RELOAD_BUTTON("HumanoidReloadButton"),
        BINDING_HUMANOID_ROTATE_LEFT_BUTTON("HumanoidRotateLeftButton"),
        BINDING_HUMANOID_ROTATE_RIGHT_BUTTON("HumanoidRotateRightButton"),
        BINDING_HUMANOID_SELECT_EMPGRENADE("HumanoidSelectEMPGrenade"),
        BINDING_HUMANOID_SELECT_FRAG_GRENADE("HumanoidSelectFragGrenade"),
        BINDING_HUMANOID_SELECT_NEXT_GRENADE_TYPE_BUTTON("HumanoidSelectNextGrenadeTypeButton"),
        BINDING_HUMANOID_SELECT_NEXT_WEAPON_BUTTON("HumanoidSelectNextWeaponButton"),
        BINDING_HUMANOID_SELECT_PREVIOUS_GRENADE_TYPE_BUTTON("HumanoidSelectPreviousGrenadeTypeButton"),
        BINDING_HUMANOID_SELECT_PREVIOUS_WEAPON_BUTTON("HumanoidSelectPreviousWeaponButton"),
        BINDING_HUMANOID_SELECT_PRIMARY_WEAPON_BUTTON("HumanoidSelectPrimaryWeaponButton"),
        BINDING_HUMANOID_SELECT_SECONDARY_WEAPON_BUTTON("HumanoidSelectSecondaryWeaponButton"),
        BINDING_HUMANOID_SELECT_SHIELD_GRENADE("HumanoidSelectShieldGrenade"),
        BINDING_HUMANOID_SELECT_UTILITY_WEAPON_BUTTON("HumanoidSelectUtilityWeaponButton"),
        BINDING_HUMANOID_SPRINT_BUTTON("HumanoidSprintButton"),
        BINDING_HUMANOID_STRAFE_LEFT_BUTTON("HumanoidStrafeLeftButton"),
        BINDING_HUMANOID_STRAFE_RIGHT_BUTTON("HumanoidStrafeRightButton"),
        BINDING_HUMANOID_SWITCH_TO_COMP_ANALYSER("HumanoidSwitchToCompAnalyser"),
        BINDING_HUMANOID_SWITCH_TO_RECHARGE_TOOL("HumanoidSwitchToRechargeTool"),
        BINDING_HUMANOID_SWITCH_TO_SUIT_TOOL("HumanoidSwitchToSuitTool"),
        BINDING_HUMANOID_SWITCH_WEAPON("HumanoidSwitchWeapon"),
        BINDING_HUMANOID_THROW_GRENADE_BUTTON("HumanoidThrowGrenadeButton"),
        BINDING_HUMANOID_FLASHLIGHT_BUTTON("HumanoidToggleFlashlightButton"),
        BINDING_HUMANOID_MISSION_HELP_PANEL_BUTTON("HumanoidToggleMissionHelpPanelButton"),
        BINDING_HUMANOID_NIGHT_VISION_BUTTON("HumanoidToggleNightVisionButton", DRIVEN),
        BINDING_HUMANOID_SHIELDS_BUTTON("HumanoidToggleShieldsButton"),
        BINDING_HUMANOID_TOOL_MODE_BUTTON("HumanoidToggleToolModeButton"),
        BINDING_HUMANOID_UTILITY_WHEEL_CYCLE_MODE("HumanoidUtilityWheelCycleMode"),
        BINDING_HUMANOID_WALK_BUTTON("HumanoidWalkButton"),
        BINDING_INCREASE_SPEED_BUTTON_MAX("IncreaseSpeedButtonMax"),
        BINDING_LEFT_THRUST_BUTTON("LeftThrustButton"),
        BINDING_LEFT_THRUST_BUTTON_LANDING("LeftThrustButton_Landing"),
        BINDING_MICROPHONE_MUTE("MicrophoneMute"),
        BINDING_MOUSE_RESET("MouseReset"),
        BINDING_MOVE_FREE_CAM_BACKWARDS("MoveFreeCamBackwards"),
        BINDING_MOVE_FREE_CAM_DOWN("MoveFreeCamDown"),
        BINDING_MOVE_FREE_CAM_FORWARD("MoveFreeCamForward"),
        BINDING_MOVE_FREE_CAM_LEFT("MoveFreeCamLeft"),
        BINDING_MOVE_FREE_CAM_RIGHT("MoveFreeCamRight"),
        BINDING_MOVE_FREE_CAM_UP("MoveFreeCamUp"),
        BINDING_MOVE_PLACEMENT_CAM_BACKWARDS("MovePlacementCamBackwards"),
        BINDING_MOVE_PLACEMENT_CAM_DOWN("MovePlacementCamDown"),
        BINDING_MOVE_PLACEMENT_CAM_FORWARD("MovePlacementCamForward"),
        BINDING_MOVE_PLACEMENT_CAM_LEFT("MovePlacementCamLeft"),
        BINDING_MOVE_PLACEMENT_CAM_RIGHT("MovePlacementCamRight"),
        BINDING_MOVE_PLACEMENT_CAM_UP("MovePlacementCamUp"),
        BINDING_MULTI_CREW_COCKPIT_UICYCLE_BACKWARD("MultiCrewCockpitUICycleBackward"),
        BINDING_MULTI_CREW_COCKPIT_UICYCLE_FORWARD("MultiCrewCockpitUICycleForward"),
        BINDING_MULTI_CREW_PRIMARY_FIRE("MultiCrewPrimaryFire"),
        BINDING_MULTI_CREW_PRIMARY_UTILITY_FIRE("MultiCrewPrimaryUtilityFire"),
        BINDING_MULTI_CREW_SECONDARY_FIRE("MultiCrewSecondaryFire"),
        BINDING_MULTI_CREW_SECONDARY_UTILITY_FIRE("MultiCrewSecondaryUtilityFire"),
        BINDING_MULTI_CREW_THIRD_PERSON_FOV_IN_BUTTON("MultiCrewThirdPersonFovInButton"),
        BINDING_MULTI_CREW_THIRD_PERSON_FOV_OUT_BUTTON("MultiCrewThirdPersonFovOutButton"),
        BINDING_MULTI_CREW_THIRD_PERSON_PITCH_DOWN_BUTTON("MultiCrewThirdPersonPitchDownButton"),
        BINDING_MULTI_CREW_THIRD_PERSON_PITCH_UP_BUTTON("MultiCrewThirdPersonPitchUpButton"),
        BINDING_MULTI_CREW_THIRD_PERSON_YAW_LEFT_BUTTON("MultiCrewThirdPersonYawLeftButton"),
        BINDING_MULTI_CREW_THIRD_PERSON_YAW_RIGHT_BUTTON("MultiCrewThirdPersonYawRightButton"),
        BINDING_MULTI_CREW_MODE("MultiCrewToggleMode"),
        BINDING_OPEN_CODEX_GO_TO_DISCOVERY("OpenCodexGoToDiscovery"),
        BINDING_OPEN_CODEX_GO_TO_DISCOVERY_BUGGY("OpenCodexGoToDiscovery_Buggy"),
        BINDING_OPEN_ORDERS("OpenOrders"),
        BINDING_ORBIT_LINES_TOGGLE("OrbitLinesToggle"),
        BINDING_REQUEST_AGGRESSIVE_BEHAVIOUR("OrderAggressiveBehaviour"),
        BINDING_REQUEST_FOLLOW("OrderFollow"),
        BINDING_REQUEST_HOLD_POSITION("OrderHoldPosition"),
        BINDING_PAUSE("Pause"),
        BINDING_PHOTO_CAMERA_TOGGLE("PhotoCameraToggle"),
        BINDING_PHOTO_CAMERA_BUGGY("PhotoCameraToggle_Buggy"),
        BINDING_PHOTO_CAMERA_HUMANOID("PhotoCameraToggle_Humanoid"),
        BINDING_PITCH_CAMERA_DOWN("PitchCameraDown"),
        BINDING_PITCH_CAMERA_UP("PitchCameraUp"),
        BINDING_PITCH_DOWN_BUTTON("PitchDownButton"),
        BINDING_PITCH_DOWN_BUTTON_LANDING("PitchDownButton_Landing"),
        BINDING_PITCH_PLACEMENT_CAMERA_DOWN("PitchPlacementCameraDown"),
        BINDING_PITCH_PLACEMENT_CAMERA_UP("PitchPlacementCameraUp"),
        BINDING_PITCH_UP_BUTTON("PitchUpButton"),
        BINDING_PITCH_UP_BUTTON_LANDING("PitchUpButton_Landing"),
        BINDING_PLACE_SETTLEMENT("PlaceSettlement"),
        BINDING_PLACEMENT_CAM_SPEED_DEC("PlacementCamSpeedDec"),
        BINDING_PLACEMENT_CAM_SPEED_INC("PlacementCamSpeedInc"),
        BINDING_PLAYER_HUDMODE_TOGGLE("PlayerHUDModeToggle", DRIVEN),
        BINDING_PLAYER_HUDMODE_BUGGY("PlayerHUDModeToggle_Buggy", DRIVEN),
        BINDING_QUICK_COMMS_PANEL("QuickCommsPanel"),
        BINDING_QUICK_COMMS_PANEL_BUGGY("QuickCommsPanel_Buggy"),
        BINDING_QUICK_COMMS_PANEL_HUMANOID("QuickCommsPanel_Humanoid"),
        BINDING_QUIT_CAMERA("QuitCamera"),
        BINDING_RIGHT_THRUST_BUTTON("RightThrustButton"),
        BINDING_RIGHT_THRUST_BUTTON_LANDING("RightThrustButton_Landing"),
        BINDING_ROLL_CAMERA_LEFT("RollCameraLeft"),
        BINDING_ROLL_CAMERA_RIGHT("RollCameraRight"),
        BINDING_ROLL_LEFT_BUTTON("RollLeftButton"),
        BINDING_ROLL_LEFT_BUTTON_LANDING("RollLeftButton_Landing"),
        BINDING_ROLL_RIGHT_BUTTON("RollRightButton"),
        BINDING_ROLL_RIGHT_BUTTON_LANDING("RollRightButton_Landing"),
        BINDING_SAATHIRD_PERSON_FOV_IN_BUTTON("SAAThirdPersonFovInButton"),
        BINDING_SAATHIRD_PERSON_FOV_OUT_BUTTON("SAAThirdPersonFovOutButton"),
        BINDING_SAATHIRD_PERSON_PITCH_DOWN_BUTTON("SAAThirdPersonPitchDownButton"),
        BINDING_SAATHIRD_PERSON_PITCH_UP_BUTTON("SAAThirdPersonPitchUpButton"),
        BINDING_SAATHIRD_PERSON_YAW_LEFT_BUTTON("SAAThirdPersonYawLeftButton"),
        BINDING_SAATHIRD_PERSON_YAW_RIGHT_BUTTON("SAAThirdPersonYawRightButton"),
        BINDING_SELECT_TARGET("SelectTarget"),
        BINDING_SELECT_TARGET_BUGGY("SelectTarget_Buggy"),
        BINDING_SET_SPEED_MINUS100("SetSpeedMinus100"),
        BINDING_SET_SPEED_MINUS25("SetSpeedMinus25"),
        BINDING_SET_SPEED_MINUS50("SetSpeedMinus50"),
        BINDING_SET_SPEED_MINUS75("SetSpeedMinus75"),
        BINDING_SHIP_SPOT_LIGHT_TOGGLE("ShipSpotLightToggle", DRIVEN),
        BINDING_SHOW_PGSCORE_SUMMARY_INPUT("ShowPGScoreSummaryInput"),
        BINDING_STEER_LEFT_BUTTON("SteerLeftButton"),
        BINDING_STEER_RIGHT_BUTTON("SteerRightButton"),
        BINDING_STORE_CAM_ZOOM_IN("StoreCamZoomIn"),
        BINDING_STORE_CAM_ZOOM_OUT("StoreCamZoomOut"),
        BINDING_STORE_ENABLE_ROTATION("StoreEnableRotation"),
        BINDING_STORE_TOGGLE("StoreToggle"),
        BINDING_SYSTEM_MAP("SystemMapOpen", DRIVEN),
        BINDING_SYSTEM_MAP_BUGGY("SystemMapOpen_Buggy", DRIVEN),
        BINDING_ADVANCE_MODE("ToggleAdvanceMode"),
        BINDING_BUGGY_TURRET_BUTTON("ToggleBuggyTurretButton"),
        BINDING_BUTTON_UP_INPUT("ToggleButtonUpInput"),
        BINDING_CARGO_SCOOP("ToggleCargoScoop", DRIVEN),
        BINDING_CARGO_SCOOP_BUGGY("ToggleCargoScoop_Buggy", DRIVEN),
        BINDING_FLIGHT_ASSIST("ToggleFlightAssist"),
        BINDING_FREE_CAM("ToggleFreeCam"),
        BINDING_REVERSE_THROTTLE_INPUT("ToggleReverseThrottleInput"),
        BINDING_REVERSE_THROTTLE_INPUT_FREE_CAM("ToggleReverseThrottleInputFreeCam"),
        BINDING_ROTATION_LOCK("ToggleRotationLock"),
        BINDING_TRIGGER_COLONISATION_MODULE("TriggerColonisationModule"),
        BINDING_TRIGGER_FIELD_NEUTRALISER("TriggerFieldNeutraliser"),
        BINDING_UIFOCUS_BUGGY("UIFocus_Buggy"),
        BINDING_UI_BACK("UI_Back", DRIVEN),
        BINDING_UP_THRUST_BUTTON("UpThrustButton"),
        BINDING_UP_THRUST_BUTTON_LANDING("UpThrustButton_Landing"),
        BINDING_USE_ALTERNATE_FLIGHT_VALUES_TOGGLE("UseAlternateFlightValuesToggle"),
        BINDING_USE_BOOST_JUICE("UseBoostJuice"),
        BINDING_VANITY_CAMERA_EIGHT("VanityCameraEight"),
        BINDING_VANITY_CAMERA_FIVE("VanityCameraFive"),
        BINDING_VANITY_CAMERA_FOUR("VanityCameraFour"),
        BINDING_VANITY_CAMERA_NINE("VanityCameraNine"),
        BINDING_VANITY_CAMERA_ONE("VanityCameraOne"),
        BINDING_VANITY_CAMERA_SCROLL_LEFT("VanityCameraScrollLeft"),
        BINDING_VANITY_CAMERA_SCROLL_RIGHT("VanityCameraScrollRight"),
        BINDING_VANITY_CAMERA_SEVEN("VanityCameraSeven"),
        BINDING_VANITY_CAMERA_SIX("VanityCameraSix"),
        BINDING_VANITY_CAMERA_TEN("VanityCameraTen"),
        BINDING_VANITY_CAMERA_THREE("VanityCameraThree"),
        BINDING_VANITY_CAMERA_TWO("VanityCameraTwo"),
        BINDING_VERTICAL_THRUSTERS_BUTTON("VerticalThrustersButton"),
        BINDING_WEAPON_COLOUR_TOGGLE("WeaponColourToggle"),
        BINDING_YAW_CAMERA_LEFT("YawCameraLeft"),
        BINDING_YAW_CAMERA_RIGHT("YawCameraRight"),
        BINDING_YAW_LEFT_BUTTON("YawLeftButton"),
        BINDING_YAW_LEFT_BUTTON_LANDING("YawLeftButton_Landing"),
        BINDING_YAW_PLACEMENT_CAMERA_LEFT("YawPlacementCameraLeft"),
        BINDING_YAW_PLACEMENT_CAMERA_RIGHT("YawPlacementCameraRight"),
        BINDING_YAW_RIGHT_BUTTON("YawRightButton"),
        BINDING_YAW_RIGHT_BUTTON_LANDING("YawRightButton_Landing"),
        BINDING_YAW_TO_ROLL_BUTTON("YawToRollButton"),
        ;
        //UP_THRUST_BUTTON("up_thrust_button", "UpThrustButton",GenericGameController.class),
        //DOWN_THRUST_BUTTON("down_thrust_button", "DownThrustButton",GenericGameController.class);


        ///
        private final String gameBinding;
        private final boolean drivenByApp;

        GameCommand(String gameBinding) {
            this(gameBinding, false);
        }

        GameCommand(String gameBinding, boolean drivenByApp) {
            this.gameBinding = gameBinding;
            this.drivenByApp = drivenByApp;
        }


        public String getGameBinding() {
            return gameBinding;
        }

        /**
         * True when a built-in command presses this control, which is what makes it worth warning the
         * commander that it is unbound. False for the rest of Elite's control set - emotes, vanity
         * cameras, Galnet audio, head look and the like - which EliteIntel carries only so the bindings
         * editor and custom command steps can name them.
         * <p>
         * Add {@code DRIVEN} when a new command starts pressing a control; {@code BindingsDrivenCommandsTest}
         * fails otherwise. Several constants name the same Elite control (a panel reached by two different
         * command names, say); the flag describes the control, so those carry it together.
         */
        public boolean isDrivenByApp() {
            return drivenByApp;
        }
    }
}