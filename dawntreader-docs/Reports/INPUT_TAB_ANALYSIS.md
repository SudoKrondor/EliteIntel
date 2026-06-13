# Input settings tab — pre-implementation analysis

Investigation only, per task instructions. No code changed in this file's commit.

---

## 1. How the existing Sleep / Wake Up checkbox works

The checkbox lives in `AiTabPanel` (`AiTabPanel.java:63-67`, label key `ai.sleepWake` =
"Sleep / Wake Up"):

```java
toggleWakeWordOnOff = new JCheckBox(getText("ai.sleepWake"), false);
toggleWakeWordOnOff.addActionListener(
        e -> EventBusManager.publish(new ToggleWakeWordEvent(toggleWakeWordOnOff.isSelected())));
```

**User click path:** `ToggleWakeWordEvent(isOn)` → `AppController.toggleStreamingMode`
(`AppController.java:93-98`):

```java
systemSession.stopStartListening(event.isOn());
EventBusManager.publish(new AiVoxResponseEvent(event.isOn() ? ignoreModeOnMessage() : ignoreModeOffMessage()));
```

`SystemSession.stopStartListening(boolean streamingModeOn)` (`SystemSession.java:98-105`)
persists `GameSession.PrivacyModeOn = streamingModeOn`. `SystemSession.isSleepingModeOn()`
(`SystemSession.java:91-96`) reads that same flag back — **`PrivacyModeOn == true` means
"asleep/ignoring", `false` means "awake/listening"**.

**Checkbox-state sync path (the important one for this task):** `AiTabPanel` separately
subscribes to `VoiceInputModeToggleEvent` (`AiTabPanel.java:176-179`):

```java
@Subscribe
public void onVoiceInputModeToggle(VoiceInputModeToggleEvent event) {
    SwingUtilities.invokeLater(() -> toggleWakeWordOnOff.setSelected(event.isStreaming()));
}
```

And `AppController` bridges the two events (`AppController.java:88-91`):

```java
@Subscribe
public void onStreamModeToggle(VoiceInputModeToggleEvent event) {
    EventBusManager.publish(new ToggleWakeWordEvent(event.isStreaming()));
}
```

So `VoiceInputModeToggleEvent(boolean isStreaming)` is the single event that both (a) updates
the AI tab checkbox directly, and (b) — via `AppController`'s bridge — re-enters
`toggleStreamingMode`, which persists `PrivacyModeOn` again (idempotent) and fires the
"I am sleeping." / "I am listening." TTS announcement. Despite the field being named
`isStreaming`, **`true` = sleeping/checkbox checked, `false` = awake/checkbox unchecked** —
this matches `PrivacyModeOn` exactly.

**Conclusion:** publishing `VoiceInputModeToggleEvent(true|false)` (plus calling
`SystemSession.stopStartListening(true|false)` directly, to avoid relying on the indirection
through `AppController`) reproduces the entire voice-command side effect set — DB persistence,
checkbox update, and spoken announcement — with one call.

---

## 2. Voice command "sleep" / "wake up" handler chain

`Commands.java`: `WAKEUP ("wakeup", null, StartListeningHandler.class)`,
`SLEEP ("sleep", null, IgnoreMeHandler.class)`.

**Wake Up** — `StartListeningHandler.handle()`:
```java
SystemSession.getInstance().stopStartListening(false);
EventBusManager.publish(new VoiceInputModeToggleEvent(false));
```

**Sleep** — `IgnoreMeHandler.handle()`:
```java
SystemSession.getInstance().stopStartListening(true);
EventBusManager.publish(new VoiceInputModeToggleEvent(true));
```

These two snippets are exactly what the new Input tab's push-to-talk logic calls for
Wake/Sleep — no new logic, just the same two calls reused verbatim.

---

## 3. Settings tab registration pattern

`SettingsTabPanel` (`SettingsTabPanel.java`):
- One field per sub-panel (`localLlmPanel`, `audioPanel`, `cloudPanel`), constructed eagerly
  as instance initializers.
- `buildUi()` registers each via `tabs.addTab(getText("settings.tab.X"), scaledIcon("/images/X.png"), xPanel)`
  — call order = visual tab order. New "Input" tab goes between the `audio` and `cloudServices`
  calls.
- `scaledIcon()` loads a 42x42 `/images/*.png` resource.
- `initData()` calls each sub-panel's `initData()`.
- `dispose()` only unregisters `SettingsTabPanel` itself from `EventBusManager` — sub-panels
  that register themselves (none currently do) would need their own `dispose()` called here
  too. **The new `InputSettingsPanel` will register on `EventBusManager` (to receive SDL
  device/button events), so `SettingsTabPanel.dispose()` must be extended to call
  `inputPanel.dispose()`.**
- i18n: only `gui.properties` (English/root) gets new keys — other locale `.properties` files
  are not touched; `MultiLingualTextProvider` falls back to the root bundle for missing keys
  (same precedent as `KEYBOARD_VIZLET_REPORT.md`).
- Icon: `/images/controller.png` exists and is unused within the Settings tab bar (it's
  currently used for the top-level "Player" tab in `AppView`, a different tab bar) — good fit
  for "Input".

---

## SDL3 / StarVizion APIs available (no new context/thread needed)

- `SdlInputService.getInstance()` — singleton. `.start()` is idempotent
  (`running.compareAndSet(false, true)`), safe to call even if `StarVizionTabPanel` already
  started it (or hasn't yet) — calling it from the new Input tab guarantees the poll loop is
  running so devices enumerate.
- `.isAvailable()` — `true` once `SDL_Init` succeeded.
- `.getConnectedDevices()` → `List<SvDevice>` snapshot, safe from any thread.
- `SvDevice(int id, String name, int axisCount, int buttonCount)` — `toString()` returns
  `name`, so a default `JComboBox` renderer displays it correctly.
- `SvDeviceConnectedEvent(SvDevice device)` / `SvDeviceDisconnectedEvent(int deviceId)` —
  published on (dis)connect; subscribe to keep the "Controller" combo live.
- `SvButtonStateEvent(int deviceId, int buttonIndex, boolean pressed)` — published on every
  button state *transition* (not continuously). `buttonIndex` is **0-based**.
  `SdlInputService.pollJoystick()` does not depend on `SDL_INIT_VIDEO` (per
  `KEYBOARD_DEBUG.md`'s root-cause analysis — that gap only affects keyboard scancode state),
  so joystick-button push-to-talk works regardless of the keyboard/video init issue.

All of the above are `@Subscribe`d from `InputSettingsPanel` itself
(`EventBusManager.register(this)` in its constructor, `unregister` in `dispose()`), which —
per `SvButtonStateEvent` arriving off-EDT on the SDL thread — means the panel must NOT read
`JComboBox`/`JCheckBox`/`JRadioButton` state directly from the subscriber. Selection state
(`selectedDevice`, `selectedButtonIndex`, `pushToTalkEnabled`, `toggleMode`) will be mirrored
into plain `volatile` fields, updated only from Swing listeners (EDT) and read from the SDL
thread.

---

## UI conventions to follow

- `AppTheme.baseGbc()` / `nextRow()` / `addLabel()` / `addField()` / `addCheck()` —
  `GridBagLayout` helpers used by every settings panel.
- Bordered `fields` `JPanel` (`LineBorder(BUTTON_BG, 1)` + `EmptyBorder(8,8,8,8)`) wrapped in a
  `BoxLayout.PAGE_AXIS` `content` panel added at `BorderLayout.NORTH` — the standard
  "one section" shape (see `AudioSettingsPanel`, `CloudServicesSettingsPanel`).
- `JRadioButton` + `ButtonGroup` precedent: `LocalLlmSettingsPanel` (`ollamaRadio`/
  `lmStudioRadio`).
- Enable/disable cascading precedent: `AppTheme.bindLock(JCheckBox, JComponent)` toggles a
  single field's enabled state from a checkbox — the Input tab needs the same idea applied to
  four components at once, so a small local helper (not `bindLock`, which is 1:1) will do it.

---

## Plan

1. New `elite.intel.ui.view.settings.InputSettingsPanel`:
   - "Enable Push to Talk" checkbox (default unchecked).
   - "Controller" `JComboBox<Object>` (placeholder string + `SvDevice` items), populated from
     `SdlInputService.getInstance().getConnectedDevices()` and kept live via
     `SvDeviceConnectedEvent`/`SvDeviceDisconnectedEvent`.
   - "Button" `JComboBox<String>` ("Select a button", "Button 1".."Button N"), repopulated
     when the controller selection changes, using the selected `SvDevice.buttonCount()`.
   - `ButtonGroup` of two `JRadioButton`s: "Toggle to sleep / wake" (default selected),
     "Hold to wake".
   - All four controls disabled until "Enable Push to Talk" is checked; checking it also
     calls `SdlInputService.getInstance().start()` and refreshes the controller list.
   - `@Subscribe onButtonState(SvButtonStateEvent)`: filtered by `pushToTalkEnabled` +
     matching `(deviceId, buttonIndex)`; Toggle mode reacts on press only and branches on
     `SystemSession.isSleepingModeOn()`; Hold mode reacts on press (wake) and release (sleep).
   - Wake = `SystemSession.getInstance().stopStartListening(false)` +
     `EventBusManager.publish(new VoiceInputModeToggleEvent(false))`.
     Sleep = same with `true`.
2. Wire into `SettingsTabPanel`: new field, `tabs.addTab(getText("settings.tab.input"),
   scaledIcon("/images/controller.png"), inputPanel)` between audio and cloud, `initData()`
   call, `dispose()` call.
3. New `gui.properties` keys: `settings.tab.input`, `settings.input.enablePushToTalk`,
   `settings.input.controller`, `settings.input.controller.placeholder`,
   `settings.input.button`, `settings.input.button.placeholder`,
   `settings.input.mode.toggle`, `settings.input.mode.hold`.
4. `./gradlew shadowJar`.
