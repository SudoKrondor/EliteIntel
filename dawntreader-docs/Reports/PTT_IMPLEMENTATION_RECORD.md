# Push-to-Talk (PTT) Implementation Record

This is a from-scratch rebuild blueprint for the "Push to Talk via SDL3 controller button"
feature added to the Settings panel's new **Input** tab. It documents every file touched,
every class/method involved (new and pre-existing/reused), the exact event flow between SDL3
controller polling and Elite Intel's sleep/wake subsystem, and both the **Toggle** and **Hold**
mode code paths in full. Anyone re-implementing this in a different UI (e.g. a web frontend,
a different desktop toolkit, or a headless config) should be able to do so from this document
alone, using the "Rebuild blueprint" section at the end as the architecture-agnostic core.

---

## 1. Feature summary

A new **Input** settings tab lets the user:

1. Enable/disable "Push to Talk" (checkbox, default OFF).
2. Pick a connected SDL3-recognized controller from a combo box.
3. Pick a button on that controller from a second combo box ("Button 1".."Button N").
4. Pick a trigger mode via two radio buttons:
   - **Toggle to sleep / wake** (default) — each button *press* toggles between
     sleeping/awake.
   - **Hold to wake** — holding the button down wakes the assistant; releasing it puts it
     back to sleep.

When the configured button transitions state, the feature calls the **exact same two-line
code path** used by the `wakeup`/`sleep` voice commands, so:

- `GameSessionDao` persistence (`PrivacyModeOn`) updates identically.
- The AI tab's "Sleep / Wake Up" checkbox updates identically (any trigger source).
- The "I am listening." / "I am sleeping." TTS announcement plays identically.

All configuration (enabled flag, selected controller/button, mode) is **session-only** — it
resets to defaults on every app restart. No new DB schema, no new SDL3 context/thread (it
reuses the existing `SdlInputService` singleton and poll loop).

---

## 2. File inventory

### New files

| File | Purpose |
|---|---|
| `app/src/main/java/elite/intel/ui/view/settings/InputSettingsPanel.java` | The entire feature: UI + SDL event subscriptions + sleep/wake dispatch. |

### Modified files

| File | Change |
|---|---|
| `app/src/main/java/elite/intel/ui/view/SettingsTabPanel.java` | Registers `InputSettingsPanel` as a new tab between Audio and Cloud Services; wires `initData()`/`dispose()`. |
| `app/src/main/resources/i18n/gui.properties` | New `settings.tab.input` tab label + 7-key "Input settings tab" section (English/root bundle only). |

### Pre-existing files this feature depends on (read-only — **not modified**)

| File | Role |
|---|---|
| `app/src/main/java/elite/intel/starvizion/input/SdlInputService.java` | SDL3 singleton poll loop — source of controller/button events. |
| `app/src/main/java/elite/intel/starvizion/model/SvDevice.java` | Device record (`id`, `name`, `axisCount`, `buttonCount`). |
| `app/src/main/java/elite/intel/starvizion/event/SvDeviceConnectedEvent.java` | Published on controller plug-in. |
| `app/src/main/java/elite/intel/starvizion/event/SvDeviceDisconnectedEvent.java` | Published on controller unplug. |
| `app/src/main/java/elite/intel/starvizion/event/SvButtonStateEvent.java` | Published on every button press/release transition. |
| `app/src/main/java/elite/intel/ui/event/VoiceInputModeToggleEvent.java` | The event that drives sleep/wake UI sync + persistence bridge. |
| `app/src/main/java/elite/intel/session/SystemSession.java` | `stopStartListening(boolean)` / `isSleepingModeOn()` — persists/reads `PrivacyModeOn`. |
| `app/src/main/java/elite/intel/db/dao/GameSessionDao.java` | DB-backed `PrivacyModeOn` flag (the actual persisted sleep/wake state). |
| `app/src/main/java/elite/intel/ai/brain/actions/handlers/commands/StartListeningHandler.java` | Voice "wakeup" command handler — code path copied verbatim. |
| `app/src/main/java/elite/intel/ai/brain/actions/handlers/commands/IgnoreMeHandler.java` | Voice "sleep" command handler — code path copied verbatim. |
| `app/src/main/java/elite/intel/ui/view/AiTabPanel.java` | Owns the "Sleep / Wake Up" checkbox; subscribes to `VoiceInputModeToggleEvent` to sync it. |
| `app/src/main/java/elite/intel/ui/controller/AppController.java` | Bridges `VoiceInputModeToggleEvent` → `ToggleWakeWordEvent` → persistence + TTS announcement. |
| `app/src/main/java/elite/intel/ui/event/ToggleWakeWordEvent.java` | Event used by the bridge above. |
| `app/src/main/java/elite/intel/ui/view/AppTheme.java` | Shared Swing styling helpers (`baseGbc`, `nextRow`, `addLabel`, `addField`, color constants). |
| `app/src/main/java/elite/intel/gameapi/EventBusManager.java` | Guava `EventBus` wrapper (`publish`/`register`/`unregister`). |

---

## 3. High-level data flow

```
                         ┌────────────────────────────────────────┐
                         │     SdlInputService (singleton)         │
                         │  dedicated "starvizion-sdl" platform    │
                         │  thread, ~60Hz poll loop (sdlLoop())     │
                         └──────────────┬───────────────────────────┘
                                         │ EventBusManager.publish(...)
                ┌────────────────────────┼────────────────────────────┐
                │                         │                            │
   SvDeviceConnectedEvent(SvDevice)  SvButtonStateEvent(   SvDeviceDisconnectedEvent(int)
                │                    deviceId, buttonIndex,            │
                │                    pressed)                          │
                ▼                         ▼                            ▼
        ┌──────────────────────────────────────────────────────────────┐
        │              InputSettingsPanel (@Subscribe, on bus)          │
        │                                                                │
        │  onDeviceConnected / onDeviceDisconnected                     │
        │     -> refresh "Controller" combo (EDT, via invokeLater)      │
        │                                                                │
        │  onButtonState(SvButtonStateEvent event)                      │
        │     -> filter: pushToTalkEnabled? matches selectedDevice/     │
        │        selectedButtonIndex?                                   │
        │     -> Toggle mode: on press, toggleSleepWake()               │
        │     -> Hold mode:   on press -> wakeUp(); on release -> sleep()│
        └───────────────────────────┬────────────────────────────────────┘
                                      │
                  wakeUp() / sleep()  │  (verbatim copy of
                                      │   StartListeningHandler / IgnoreMeHandler)
                                      ▼
        ┌──────────────────────────────────────────────────────────────┐
        │ SystemSession.getInstance().stopStartListening(bool)          │
        │   -> GameSessionDao: persists PrivacyModeOn (DB)               │
        │                                                                │
        │ EventBusManager.publish(new VoiceInputModeToggleEvent(bool))   │
        │   -> AiTabPanel.onVoiceInputModeToggle: syncs the "Sleep /     │
        │      Wake Up" checkbox (any trigger source, EDT-safe)          │
        │   -> AppController.onStreamModeToggle: re-publishes            │
        │      ToggleWakeWordEvent(bool)                                  │
        │        -> AppController.toggleStreamingMode:                   │
        │             - calls stopStartListening again (idempotent)      │
        │             - publishes AiVoxResponseEvent(                    │
        │                 "I am sleeping."/"I am listening." TTS text)   │
        └──────────────────────────────────────────────────────────────┘
```

---

## 4. Component-by-component breakdown

### 4.1 `SdlInputService` (existing, unmodified — the input source)

Package: `elite.intel.starvizion.input`

- **Singleton**: `SdlInputService.getInstance()` (double-checked locking on `instance`).
- **`start()`**: `if (!running.compareAndSet(false, true)) return;` then spawns a single
  platform thread named `"starvizion-sdl"` running `sdlLoop()`. **Idempotent** — safe to call
  even if another part of the app (e.g. `StarVizionTabPanel`) already started it.
- **`stop()`**: sets `running = false`, joins the thread (3s timeout).
- **`isAvailable()`**: `true` once `SDL_Init(SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD)` succeeds.
- **`getConnectedDevices()`**: returns an unmodifiable snapshot copy of
  `CopyOnWriteArrayList<SvDevice> connectedDevices` — **safe to call from any thread**
  (this is what the Swing EDT calls to populate the controller combo).
- **`sdlLoop()`** (runs on the `starvizion-sdl` thread):
  1. `initSdl()` — calls `SDL_Init`, publishes `SvServiceStateEvent(true/false, error)`.
  2. Loop while `running.get()`:
     - `SDL_PumpEvents()`.
     - Enumerate joystick IDs via `SDLJoystick.SDL_GetJoysticks()`.
     - New IDs → `onDeviceAdded(id)`: opens the joystick (`SDL_OpenJoystick`), reads
       `SDL_GetNumJoystickAxes`/`SDL_GetNumJoystickButtons`/`SDL_GetJoystickNameForID`,
       builds an `SvDevice`, adds it to `connectedDevices`, publishes
       `SvDeviceConnectedEvent(device)`.
     - Removed IDs → `onDeviceRemoved(id)`: closes the handle, removes from
       `connectedDevices`, publishes `SvDeviceDisconnectedEvent(id)`.
     - For each open handle → `pollJoystick(id, handle)` (see below).
     - `pollKeyboard()` (unrelated to PTT — keyboard scancode polling for the Keyboard
       Vizlet feature).
     - `Thread.sleep(POLL_INTERVAL_MS)` — **`POLL_INTERVAL_MS = 16`** (~60 Hz).
- **`pollJoystick(int id, long handle)`** — THE method that produces PTT's input signal:
  ```java
  for (int b = 0; b < prevBtn.length; b++) {
      boolean pressed = SDLJoystick.SDL_GetJoystickButton(handle, b);
      if (pressed != prevBtn[b]) {
          prevBtn[b] = pressed;
          EventBusManager.publish(new SvButtonStateEvent(id, b, pressed));
      }
  }
  ```
  Critically: **`SvButtonStateEvent` is published only on a state *transition*** (press OR
  release), never continuously/per-poll while held. Both Toggle and Hold mode logic in
  `InputSettingsPanel` rely on this — there is no debouncing or repeat-suppression to add.
  Also note: `b` (the `buttonIndex` in the event) is **0-based**.

This class is completely unmodified by this feature. It does not need to depend on
`InputSettingsPanel` at all — `InputSettingsPanel` is purely a *consumer* of its events via
the shared `EventBusManager`.

### 4.2 Supporting records/events (existing, unmodified)

```java
// app/src/main/java/elite/intel/starvizion/model/SvDevice.java
public record SvDevice(int id, String name, int axisCount, int buttonCount) {
    @Override
    public String toString() { return name; }  // <- lets a default JComboBox renderer show the name
}

// app/src/main/java/elite/intel/starvizion/event/SvDeviceConnectedEvent.java
public record SvDeviceConnectedEvent(SvDevice device) {}

// app/src/main/java/elite/intel/starvizion/event/SvDeviceDisconnectedEvent.java
public record SvDeviceDisconnectedEvent(int deviceId) {}

// app/src/main/java/elite/intel/starvizion/event/SvButtonStateEvent.java
public record SvButtonStateEvent(int deviceId, int buttonIndex, boolean pressed) {}
```

### 4.3 `InputSettingsPanel` (NEW — the entire feature)

Package: `elite.intel.ui.view.settings`. Extends `JPanel`.

#### Fields

```java
private JCheckBox enablePushToTalkCheck;
private JComboBox<Object> controllerCombo;   // items: String placeholder OR SvDevice
private JComboBox<String> buttonCombo;       // items: String placeholder OR "Button N"
private JRadioButton toggleModeRadio;        // default selected = true
private JRadioButton holdModeRadio;

// Mirrors of the Swing selection state above, read from the SDL poll thread.
private volatile boolean pushToTalkEnabled = false;
private volatile SvDevice selectedDevice = null;
private volatile int selectedButtonIndex = -1; // 0-based SDL button index, -1 = none
private volatile boolean toggleMode = true;
```

The `volatile` fields are the **entire** thread-safety mechanism (see §6). Everything else is
either pure Swing (EDT-only) or pure event-bus plumbing.

#### Constructor / lifecycle

```java
public InputSettingsPanel() {
    EventBusManager.register(this);   // subscribes onDeviceConnected/Disconnected/onButtonState
    buildUi();
}

public void dispose() {
    EventBusManager.unregister(this);
}

public void initData() {
    refreshControllerCombo();          // called once at app startup by SettingsTabPanel.initData()
}
```

#### UI construction — `buildUi()`

Standard "one bordered section" shape shared by every settings sub-panel
(`AudioSettingsPanel`, `CloudServicesSettingsPanel`, `LocalLlmSettingsPanel`):

- `setLayout(new BorderLayout())`.
- A `fields` `JPanel` with `GridBagLayout`, constraints from `AppTheme.baseGbc()`
  (insets `(6,6,6,6)`, anchor `WEST`, fill `NONE`, `weightx=weighty=0`).
- Row 0 (`gc.gridx=0; gc.gridwidth=2`): `enablePushToTalkCheck` — a `JCheckBox` spanning
  both grid columns.
- Row 1 (`AppTheme.nextRow(gc)`): label "Controller" via `addLabel(...)`, then
  `controllerCombo` via `addField(fields, controllerCombo, gc, 1, 1.0)` (column 1, fills
  horizontally, weight 1.0).
- Row 2: same shape for "Button" / `buttonCombo`.
- Rows 3–4: `toggleModeRadio` and `holdModeRadio`, each `gc.gridx=0; gc.gridwidth=2` (full
  width, stacked vertically via successive `nextRow(gc)`).
- `fields` gets `BorderFactory.createCompoundBorder(new LineBorder(BUTTON_BG, 1),
  BorderFactory.createEmptyBorder(8,8,8,8))`.
- `fields` is wrapped in a `content` `JPanel` (`BoxLayout.PAGE_AXIS`,
  `EmptyBorder(12,12,12,12)`), added to `this` at `BorderLayout.NORTH`.
- Finally: `setControlsEnabled(false)` — everything except the enable checkbox starts
  disabled.

Initial combo contents (before `initData()`/SDL data arrives):
- `controllerCombo`: one item — the placeholder string `getText("settings.input.controller.placeholder")`
  ("Select a controller").
- `buttonCombo`: one item — `getText("settings.input.button.placeholder")` ("Select a
  button").

#### Listener wiring (all set up inside `buildUi()`)

```java
enablePushToTalkCheck.addActionListener(e -> onPushToTalkToggled());
controllerCombo.addActionListener(e -> onControllerSelected());
buttonCombo.addActionListener(e -> onButtonSelected());
toggleModeRadio.addActionListener(e -> toggleMode = true);
holdModeRadio.addActionListener(e -> toggleMode = false);
```

`toggleModeRadio` and `holdModeRadio` are in one `ButtonGroup` (mutually exclusive),
`toggleModeRadio` constructed with `selected = true` (the DEFAULT mode).

#### Handlers

```java
private void onPushToTalkToggled() {
    boolean enabled = enablePushToTalkCheck.isSelected();
    pushToTalkEnabled = enabled;          // volatile write — visible to SDL thread
    setControlsEnabled(enabled);
    if (enabled) {
        SdlInputService.getInstance().start();  // idempotent; ensures poll loop is running
        refreshControllerCombo();
    }
}

private void setControlsEnabled(boolean enabled) {
    controllerCombo.setEnabled(enabled);
    buttonCombo.setEnabled(enabled);
    toggleModeRadio.setEnabled(enabled);
    holdModeRadio.setEnabled(enabled);
}

private void onControllerSelected() {
    Object selected = controllerCombo.getSelectedItem();
    SvDevice device = (selected instanceof SvDevice d) ? d : null; // null if placeholder String selected
    selectedDevice = device;              // volatile write
    populateButtonCombo(device);
}

private void onButtonSelected() {
    selectedButtonIndex = buttonCombo.getSelectedIndex() - 1; // index 0 = placeholder -> -1
}
```

#### Combo population

```java
private void refreshControllerCombo() {
    SvDevice previouslySelected = selectedDevice;

    controllerCombo.removeAllItems();
    controllerCombo.addItem(getText("settings.input.controller.placeholder"));
    for (SvDevice device : SdlInputService.getInstance().getConnectedDevices()) {
        controllerCombo.addItem(device);   // default renderer shows SvDevice.toString() == name
    }

    // Re-select the previously chosen device by id, if it's still connected.
    if (previouslySelected != null) {
        for (int i = 1; i < controllerCombo.getItemCount(); i++) {
            if (controllerCombo.getItemAt(i) instanceof SvDevice d && d.id() == previouslySelected.id()) {
                controllerCombo.setSelectedIndex(i);
                return;
            }
        }
    }
    controllerCombo.setSelectedIndex(0); // falls back to placeholder
}

private void populateButtonCombo(SvDevice device) {
    buttonCombo.removeAllItems();
    buttonCombo.addItem(getText("settings.input.button.placeholder"));
    if (device != null) {
        for (int i = 1; i <= device.buttonCount(); i++) {
            buttonCombo.addItem("Button " + i);   // 1-based display label
        }
    }
    buttonCombo.setSelectedIndex(0);
}
```

Note the index relationship: display label "Button N" is at combo index `N` (placeholder is
index 0), and corresponds to **0-based SDL button index `N-1`** — hence
`buttonCombo.getSelectedIndex() - 1` in `onButtonSelected()`.

#### SDL event subscriptions

```java
@Subscribe
public void onDeviceConnected(SvDeviceConnectedEvent event) {
    SwingUtilities.invokeLater(this::refreshControllerCombo);
}

@Subscribe
public void onDeviceDisconnected(SvDeviceDisconnectedEvent event) {
    SwingUtilities.invokeLater(() -> {
        if (selectedDevice != null && selectedDevice.id() == event.deviceId()) {
            selectedDevice = null;
            selectedButtonIndex = -1;
            populateButtonCombo(null);
        }
        refreshControllerCombo();
    });
}
```

Both of these run on the SDL thread when published, hence `SwingUtilities.invokeLater(...)`
to touch Swing components on the EDT.

#### The PTT trigger — `onButtonState`

```java
@Subscribe
public void onButtonState(SvButtonStateEvent event) {
    if (!pushToTalkEnabled) return;

    SvDevice device = selectedDevice;
    int buttonIndex = selectedButtonIndex;
    if (device == null || buttonIndex < 0) return;
    if (event.deviceId() != device.id() || event.buttonIndex() != buttonIndex) return;

    if (toggleMode) {
        if (event.pressed()) toggleSleepWake();
    } else {
        if (event.pressed()) wakeUp(); else sleep();
    }
}
```

This runs **on the SDL poll thread** (`starvizion-sdl`), never the EDT. It does NOT touch any
Swing component — only reads the four `volatile` fields and calls plain POJO methods
(`SystemSession`, `EventBusManager`).

#### Sleep/Wake dispatch — verbatim-copied code paths

```java
private void toggleSleepWake() {
    if (SystemSession.getInstance().isSleepingModeOn()) wakeUp(); else sleep();
}

private void wakeUp() {
    SystemSession.getInstance().stopStartListening(false);
    EventBusManager.publish(new VoiceInputModeToggleEvent(false));
}

private void sleep() {
    SystemSession.getInstance().stopStartListening(true);
    EventBusManager.publish(new VoiceInputModeToggleEvent(true));
}
```

`wakeUp()` is byte-for-byte identical to `StartListeningHandler.handle()`'s body; `sleep()`
is byte-for-byte identical to `IgnoreMeHandler.handle()`'s body.

### 4.4 `SettingsTabPanel` wiring (modified)

```java
// new import
import elite.intel.ui.view.settings.InputSettingsPanel;

// new field, constructed alongside the other tab panels
private final InputSettingsPanel inputPanel = new InputSettingsPanel();

public void dispose() {
    inputPanel.dispose();              // <- new: unregister from EventBusManager
    EventBusManager.unregister(this);
}

private void buildUi() {
    ...
    tabs.addTab(getText("settings.tab.localLlm"), scaledIcon("/images/local-llm.png"), localLlmPanel);
    tabs.addTab(getText("settings.tab.audio"), scaledIcon("/images/audio.png"), audioPanel);
    tabs.addTab(getText("settings.tab.input"), scaledIcon("/images/controller.png"), inputPanel); // <- new, between audio and cloud
    tabs.addTab(getText("settings.tab.cloudServices"), scaledIcon("/images/cloud.png"), cloudPanel);
    ...
}

public void initData() {
    localLlmPanel.initData();
    audioPanel.initData();
    inputPanel.initData();             // <- new: populates the controller combo on startup
    cloudPanel.initData();
}
```

Tab order is determined purely by `addTab` call order — Input is inserted between Audio and
Cloud Services. Icon reused: `/images/controller.png` (42x42 via `scaledIcon`, already an
existing resource, otherwise only used for the top-level "Player" tab in `AppView` — a
different tab bar, no collision).

### 4.5 i18n — `gui.properties` (English/root bundle only)

```properties
# in the "Settings tab labels" group, between audio and cloudServices:
settings.tab.audio=Audio
settings.tab.input=Input
settings.tab.cloudServices=Cloud Services

# new standalone section:
# Input settings tab
settings.input.enablePushToTalk=Enable Push to Talk
settings.input.controller=Controller
settings.input.controller.placeholder=Select a controller
settings.input.button=Button
settings.input.button.placeholder=Select a button
settings.input.mode.toggle=Toggle to sleep / wake
settings.input.mode.hold=Hold to wake
```

No other locale `.properties` files were touched. `MultiLingualTextProvider.getText(key)`
falls back to the root bundle for missing keys in other locales (existing precedent, e.g.
the Keyboard Vizlet feature).

---

## 5. The sleep/wake subsystem (existing — what PTT plugs into)

This is the system PTT reuses without modification. Understanding it fully is required to
reproduce PTT's effects in another architecture.

### 5.1 Persisted state

`GameSessionDao.GameSession.privacyModeOn` (boolean column, `getPrivacyModeOn()` /
`setPrivacyModeOn(Boolean)`), loaded/saved via JDBI3 in `app/src/main/java/elite/intel/db/dao/GameSessionDao.java`.

- **`true`** = asleep / ignoring voice input.
- **`false`** = awake / listening.

### 5.2 `SystemSession` accessors

```java
// app/src/main/java/elite/intel/session/SystemSession.java
public boolean isSleepingModeOn() {
    return Database.withDao(GameSessionDao.class, dao -> {
        GameSessionDao.GameSession session = dao.get();
        return session.getPrivacyModeOn();
    });
}

public void stopStartListening(boolean streamingModeOn) {
    Database.withDao(GameSessionDao.class, dao -> {
        GameSessionDao.GameSession session = dao.get();
        session.setPrivacyModeOn(streamingModeOn);
        dao.save(session);
        return null;
    });
}
```

`stopStartListening(true)` = go to sleep (persist `PrivacyModeOn = true`).
`stopStartListening(false)` = wake up (persist `PrivacyModeOn = false`).

### 5.3 `VoiceInputModeToggleEvent` — the "I changed sleep state" notification

```java
// app/src/main/java/elite/intel/ui/event/VoiceInputModeToggleEvent.java
public class VoiceInputModeToggleEvent {
    private boolean isStreaming;
    public VoiceInputModeToggleEvent(boolean isStreaming) { this.isStreaming = isStreaming; }
    public boolean isStreaming() { return isStreaming; }
}
```

**Naming is misleading but consistent**: despite the field name `isStreaming`, the semantics
match `PrivacyModeOn` exactly — **`true` = sleeping** (checkbox checked), **`false` = awake**
(checkbox unchecked).

Two subscribers react to this event:

**(a) `AiTabPanel.onVoiceInputModeToggle`** — keeps the visible checkbox in sync regardless of
trigger source:
```java
// app/src/main/java/elite/intel/ui/view/AiTabPanel.java
@Subscribe
public void onVoiceInputModeToggle(VoiceInputModeToggleEvent event) {
    SwingUtilities.invokeLater(() -> toggleWakeWordOnOff.setSelected(event.isStreaming()));
}
```
(`toggleWakeWordOnOff` is the "Sleep / Wake Up" `JCheckBox`, label key `ai.sleepWake`.)

**(b) `AppController.onStreamModeToggle`** — bridges back into the voice-command toggle path:
```java
// app/src/main/java/elite/intel/ui/controller/AppController.java
@Subscribe
public void onStreamModeToggle(VoiceInputModeToggleEvent event) {
    EventBusManager.publish(new ToggleWakeWordEvent(event.isStreaming()));
}

@Subscribe
public void toggleStreamingMode(ToggleWakeWordEvent event) {
    appendToLog("Voice input mode toggle");
    systemSession.stopStartListening(event.isOn());
    EventBusManager.publish(new AiVoxResponseEvent(
        event.isOn() ? ignoreModeOnMessage() : ignoreModeOffMessage()));
}

private String ignoreModeOffMessage() { return StringUtls.localizedSpeech("speech.ignoreModeOff"); }
private String ignoreModeOnMessage()  { return StringUtls.localizedSpeech("speech.ignoreModeOn");  }
```

So a single `EventBusManager.publish(new VoiceInputModeToggleEvent(bool))` call:
1. Directly flips the AI tab checkbox (via (a)).
2. Re-persists `PrivacyModeOn` (idempotent — already set by `stopStartListening` in
   `wakeUp()`/`sleep()`, but re-set again via the bridge (b) — harmless).
3. Triggers the spoken "I am sleeping." / "I am listening." TTS announcement (via (b) →
   `AiVoxResponseEvent` → TTS subsystem `elite.intel.ai.mouth`).

### 5.4 `ToggleWakeWordEvent` (existing, unmodified)

```java
// app/src/main/java/elite/intel/ui/event/ToggleWakeWordEvent.java
public class ToggleWakeWordEvent {
    private boolean isOn;
    public ToggleWakeWordEvent(boolean isSleepingMode) { this.isOn = isSleepingMode; }
    public boolean isOn() { return isOn; }
}
```

### 5.5 Voice command handlers (existing, unmodified — the templates PTT copied)

```java
// app/src/main/java/elite/intel/ai/brain/actions/handlers/commands/StartListeningHandler.java
// bound to Commands.WAKEUP ("wakeup", null, StartListeningHandler.class)
public class StartListeningHandler implements CommandHandler {
    @Override
    public void handle(String action, JsonObject params, String responseText) {
        SystemSession.getInstance().stopStartListening(false);
        EventBusManager.publish(new VoiceInputModeToggleEvent(false));
    }
}

// app/src/main/java/elite/intel/ai/brain/actions/handlers/commands/IgnoreMeHandler.java
// bound to Commands.SLEEP ("sleep", null, IgnoreMeHandler.class)
public class IgnoreMeHandler implements CommandHandler {
    @Override
    public void handle(String action, JsonObject params, String responseText) {
        SystemSession.getInstance().stopStartListening(true);
        EventBusManager.publish(new VoiceInputModeToggleEvent(true));
    }
}
```

---

## 6. Exact code paths for Toggle vs Hold mode

Both start from the same dispatcher:

```java
@Subscribe
public void onButtonState(SvButtonStateEvent event) {
    if (!pushToTalkEnabled) return;                                   // (1) feature must be enabled

    SvDevice device = selectedDevice;
    int buttonIndex = selectedButtonIndex;
    if (device == null || buttonIndex < 0) return;                    // (2) a controller+button must be configured

    if (event.deviceId() != device.id()
        || event.buttonIndex() != buttonIndex) return;                // (3) must be THIS button on THIS device

    if (toggleMode) {
        if (event.pressed()) toggleSleepWake();                        // (4a) TOGGLE
    } else {
        if (event.pressed()) wakeUp(); else sleep();                   // (4b) HOLD
    }
}
```

### 6.1 Toggle mode ("Toggle to sleep / wake" — default)

- Trigger: `event.pressed() == true` (button-down transition). **Button-up transitions are
  ignored entirely** — no `else` branch for `toggleMode` + release.
- Action: `toggleSleepWake()`:
  ```java
  private void toggleSleepWake() {
      if (SystemSession.getInstance().isSleepingModeOn()) wakeUp(); else sleep();
  }
  ```
  - Reads the **current persisted** `PrivacyModeOn` from the DB at the moment of the press
    (not any locally cached state) — so it stays correct even if sleep state changed via
    voice command since the last button press.
  - If currently asleep (`PrivacyModeOn == true`) → `wakeUp()`.
  - If currently awake (`PrivacyModeOn == false`) → `sleep()`.
- Each subsequent press flips it again (true toggle).

### 6.2 Hold mode ("Hold to wake")

- Trigger: every transition of the configured button, both directions:
  - `event.pressed() == true` (button-down) → `wakeUp()`.
  - `event.pressed() == false` (button-up) → `sleep()`.
- No state inspection — purely momentary. Holding the button = awake; releasing = asleep,
  unconditionally, regardless of what `isSleepingModeOn()` was before.
- If the user was already awake (e.g. via voice "wakeup") and then holds + releases the PTT
  button, the net effect is: stays awake while held, goes to sleep on release — i.e. Hold
  mode's release **always** forces sleep.

### 6.3 `wakeUp()` / `sleep()` (shared by both modes)

```java
private void wakeUp() {
    SystemSession.getInstance().stopStartListening(false);
    EventBusManager.publish(new VoiceInputModeToggleEvent(false));
}

private void sleep() {
    SystemSession.getInstance().stopStartListening(true);
    EventBusManager.publish(new VoiceInputModeToggleEvent(true));
}
```

Each call: (1) persists `PrivacyModeOn`, (2) publishes the toggle event that updates the
AI-tab checkbox and triggers the spoken announcement (§5.3).

---

## 7. Threading & concurrency model

| Component | Thread |
|---|---|
| `SdlInputService.sdlLoop()` / `pollJoystick()` | dedicated platform thread `"starvizion-sdl"`, ~60Hz |
| `EventBusManager` dispatch (Guava `EventBus`) | synchronous, on the **publisher's** thread by default — so `@Subscribe` methods run on whichever thread called `publish()` |
| `InputSettingsPanel.onDeviceConnected/Disconnected/onButtonState` | called on the `starvizion-sdl` thread (because `SdlInputService` publishes from there) |
| `InputSettingsPanel` Swing listeners (`onPushToTalkToggled`, `onControllerSelected`, `onButtonSelected`, radio listeners) | Swing EDT |
| `SystemSession`/`GameSessionDao`/HikariCP | thread-safe (connection-pooled JDBI3) — safe to call from `starvizion-sdl` |

**Rule enforced by this implementation**: `onButtonState` (SDL thread) never touches a Swing
component. It only reads four `volatile` fields (`pushToTalkEnabled`, `selectedDevice`,
`selectedButtonIndex`, `toggleMode`), each written exclusively by an EDT-bound
`ActionListener`. `onDeviceConnected`/`onDeviceDisconnected` *do* touch Swing components, so
they wrap their work in `SwingUtilities.invokeLater(...)`.

If rebuilding in a non-Swing UI, the equivalent rule is: **the input-polling thread must never
block on or directly mutate UI state** — mirror UI-selected config into plain
fields/atomics/whatever your UI framework's thread-safe primitive is, and have the polling
callback read only those.

---

## 8. Rebuild blueprint (architecture-agnostic core)

If reimplementing this feature with a different UI (web, JavaFX, CLI config file, etc.), split
it into two layers:

### 8.1 `PushToTalkController` (pure logic, no UI dependency)

A plain class (not necessarily a `JPanel`) that:

- **State** (settable by whatever UI you build):
  - `boolean enabled`
  - `int selectedDeviceId` (or device reference)
  - `int selectedButtonIndex` (0-based)
  - `enum Mode { TOGGLE, HOLD }`
- **Subscribes** to `SvButtonStateEvent` on `EventBusManager` (constructor:
  `EventBusManager.register(this)`; teardown: `EventBusManager.unregister(this)`).
- **`onButtonState(SvButtonStateEvent event)`** — identical filter + branch logic from §6,
  calling `wakeUp()`/`sleep()`/`toggleSleepWake()`.
- **`wakeUp()`/`sleep()`/`toggleSleepWake()`** — identical bodies from §4.3/§6, using
  `SystemSession.getInstance()` and `EventBusManager.publish(new VoiceInputModeToggleEvent(...))`.
- Exposes simple setters (`setEnabled`, `setSelectedDevice`, `setSelectedButtonIndex`,
  `setMode`) — these are the only points your UI needs to call, and they're the only writers
  of the controller's internal state. If your UI framework's event/property model isn't
  inherently EDT-like, ensure these setters are atomic (volatile fields, `AtomicReference`,
  etc.) the same way `InputSettingsPanel` does.

### 8.2 UI layer (whatever framework)

Responsibilities, regardless of framework:

1. **Enable toggle** → on enable, call `SdlInputService.getInstance().start()` (idempotent)
   and populate the controller list from `SdlInputService.getInstance().getConnectedDevices()`.
2. **Controller picker** → list of `SvDevice` (display `device.name()`/`toString()`);
   on change, call `controller.setSelectedDevice(device)` and repopulate the button picker
   from `device.buttonCount()` (labels "Button 1".."Button N", mapping label N → 0-based
   index N-1).
3. **Button picker** → on change, call `controller.setSelectedButtonIndex(idx)`.
4. **Mode picker** (two mutually-exclusive options) → on change, call
   `controller.setMode(Mode.TOGGLE | Mode.HOLD)`.
5. Optionally subscribe to `SvDeviceConnectedEvent`/`SvDeviceDisconnectedEvent` to keep the
   controller list live.
6. **Do not** duplicate sleep/wake logic — always route through
   `SystemSession.stopStartListening(bool)` + `EventBusManager.publish(new
   VoiceInputModeToggleEvent(bool))` (or, even better, through the shared
   `PushToTalkController.wakeUp()`/`sleep()` from §8.1) so the AI tab checkbox and TTS
   announcement stay consistent no matter which UI triggered it.

### 8.3 Minimal dependency surface

To reproduce this feature elsewhere you need exactly these existing types:

- `elite.intel.gameapi.EventBusManager` (`publish`, `register`, `unregister`)
- `elite.intel.starvizion.input.SdlInputService` (`getInstance()`, `start()`,
  `getConnectedDevices()`)
- `elite.intel.starvizion.model.SvDevice` (`id()`, `name()`/`toString()`, `buttonCount()`)
- `elite.intel.starvizion.event.{SvDeviceConnectedEvent, SvDeviceDisconnectedEvent,
  SvButtonStateEvent}`
- `elite.intel.session.SystemSession` (`getInstance()`, `stopStartListening(boolean)`,
  `isSleepingModeOn()`)
- `elite.intel.ui.event.VoiceInputModeToggleEvent`

No DB schema changes, no new EventBus events, no new SDL3 initialization — everything is
additive composition of existing singletons/events.

---

## 9. Known limitations / explicitly out of scope

- **No persistence** of push-to-talk configuration (enabled flag, selected controller/button,
  mode) — resets to defaults (disabled, no controller/button selected, Toggle mode) on every
  app restart. This was an explicit requirement ("session-only"), not an oversight.
- **No manual hardware test** was performed in the implementing session (no live
  controller/SDL runtime available). Logic correctness was verified by reading
  `SdlInputService.pollJoystick()` directly; this path does not depend on `SDL_INIT_VIDEO`,
  so it is unaffected by the keyboard-polling caveat documented in `KEYBOARD_DEBUG.md`.
- **Toggle mode ignores button-release** entirely — only press transitions matter.
- **Hold mode's release unconditionally calls `sleep()`** — there is no "was already awake
  before I pressed" memory; releasing always re-asserts the sleeping state.
- Button display labels are generic ("Button 1".."Button N") — no support for renaming
  buttons or detecting semantic button names (e.g. "A"/"B" on a gamepad) beyond what
  `SDL_GetJoystickNameForID` already provides for the device name.

---

## 10. Build verification

```
./gradlew shadowJar
BUILD SUCCESSFUL
```

`distribution/elite_intel.jar` rebuilt successfully with the new tab/panel included.
