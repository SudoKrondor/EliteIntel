# Input settings tab — implementation report

Implements the plan from `INPUT_TAB_ANALYSIS.md`. New "Input" tab added to the Settings
panel, between Audio and Cloud Services.

## New files

- **`app/src/main/java/elite/intel/ui/view/settings/InputSettingsPanel.java`**

  Single "Push to Talk" section inside a bordered `fields` `GridBagLayout` panel (same
  shape as `AudioSettingsPanel`/`CloudServicesSettingsPanel`):

  - `JCheckBox` "Enable Push to Talk" (`settings.input.enablePushToTalk`, default
    unchecked). Toggling it enables/disables the four controls below and, when checked,
    calls `SdlInputService.getInstance().start()` (idempotent — no new SDL context/thread)
    and refreshes the controller list.
  - `JComboBox<Object>` "Controller" (`settings.input.controller`), placeholder
    `settings.input.controller.placeholder` = "Select a controller". Populated from
    `SdlInputService.getInstance().getConnectedDevices()` and kept live via
    `@Subscribe`d `SvDeviceConnectedEvent` / `SvDeviceDisconnectedEvent`. `SvDevice.toString()`
    returns its name, so the default renderer displays it correctly.
  - `JComboBox<String>` "Button" (`settings.input.button`), placeholder
    `settings.input.button.placeholder` = "Select a button", repopulated as "Button 1"..
    "Button N" from the selected device's `buttonCount()` whenever the controller selection
    changes. `buttonCombo.getSelectedIndex() - 1` gives the 0-based SDL button index used to
    match `SvButtonStateEvent.buttonIndex()`.
  - `ButtonGroup` of two `JRadioButton`s: "Toggle to sleep / wake"
    (`settings.input.mode.toggle`, default selected) and "Hold to wake"
    (`settings.input.mode.hold`).
  - `@Subscribe onButtonState(SvButtonStateEvent)`, filtered by `pushToTalkEnabled` and a
    matching `(deviceId, buttonIndex)`:
    - **Toggle mode** — reacts on press only; checks `SystemSession.isSleepingModeOn()` and
      calls `wakeUp()` if currently asleep, else `sleep()`.
    - **Hold mode** — `wakeUp()` on press, `sleep()` on release.
  - `wakeUp()` / `sleep()` are verbatim copies of `StartListeningHandler` /
    `IgnoreMeHandler`'s bodies: `SystemSession.getInstance().stopStartListening(false|true)`
    + `EventBusManager.publish(new VoiceInputModeToggleEvent(false|true))`. This reuses the
    exact voice-command code path, so the AI tab's "Sleep / Wake Up" checkbox and the spoken
    "I am listening." / "I am sleeping." announcement update identically regardless of
    whether the trigger was voice or the controller button.
  - All combo/radio selection state is mirrored into `volatile` fields
    (`pushToTalkEnabled`, `selectedDevice`, `selectedButtonIndex`, `toggleMode`), written
    only from Swing listeners (EDT) and read from the `onButtonState` subscriber, which runs
    on the SDL poll thread — no Swing component is touched off-EDT.
  - `EventBusManager.register(this)` in the constructor, `unregister` in `dispose()`.
  - Session-only: no DB schema changes, no `SystemSession` persistence for the push-to-talk
    config itself.

## Modified files

- **`app/src/main/java/elite/intel/ui/view/SettingsTabPanel.java`**
  - New `private final InputSettingsPanel inputPanel = new InputSettingsPanel();`.
  - `tabs.addTab(getText("settings.tab.input"), scaledIcon("/images/controller.png"), inputPanel)`
    added between the Audio and Cloud Services `addTab` calls.
  - `initData()` now also calls `inputPanel.initData()` (populates the controller combo on
    startup).
  - `dispose()` now also calls `inputPanel.dispose()` (unregisters from `EventBusManager`).

- **`app/src/main/resources/i18n/gui.properties`**
  - `settings.tab.input=Input`
  - New "Input settings tab" section: `settings.input.enablePushToTalk`,
    `settings.input.controller`, `settings.input.controller.placeholder`,
    `settings.input.button`, `settings.input.button.placeholder`,
    `settings.input.mode.toggle`, `settings.input.mode.hold`.
  - No other locale files touched — `MultiLingualTextProvider` falls back to the root
    bundle for missing keys (same precedent as `KEYBOARD_VIZLET_REPORT.md`).

## Icon

Reused `/images/controller.png` (existing asset, otherwise only used for the top-level
"Player" tab in `AppView` — a different tab bar, so no visual collision).

## Build result

```
./gradlew shadowJar
BUILD SUCCESSFUL
```

`distribution/elite_intel.jar` rebuilt successfully.

## Notes / things not done (out of scope for this prototype)

- No persistence: push-to-talk enable state, selected controller/button, and mode reset to
  defaults (disabled, "Select a controller"/"Select a button", Toggle mode) every app
  restart, per the "session-only" requirement.
- Not manually tested against a live controller in this session (no interactive game/SDL
  runtime available here) — logic was verified by reading `SdlInputService`'s existing
  `SvButtonStateEvent` publication (`pollJoystick()`), which does not depend on
  `SDL_INIT_VIDEO`, so it is unaffected by the keyboard-polling issue documented in
  `KEYBOARD_DEBUG.md`.
