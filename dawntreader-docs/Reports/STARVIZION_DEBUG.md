# StarVizion Debug Logging Changes

This documents two changes made to surface SDL3 / device-detection activity for the
StarVizion prototype, plus the rebuild result.

## 1. Logger level override (`app/src/main/resources/log4j2.xml`)

Added a logger override for the `elite.intel.starvizion` package, more specific than the
existing `elite.intel` (`error`) entry, so it wins for the StarVizion subtree:

```xml
<!-- Specific logger levels -->
<Logger name="elite.intel" level="error"/>
<!-- More specific than elite.intel above, so this wins for the StarVizion subtree -->
<Logger name="elite.intel.starvizion" level="debug"/>
```

Effect: all `log.debug` / `log.info` / `log.warn` / `log.error` calls anywhere under
`elite.intel.starvizion.*` (including `SdlInputService`, the overlay vizlets, and
`StarVizionTabPanel`) now reach `logs/elite-intel.log` and the console, instead of being
dropped below `error`.

## 2. New logging in `SdlInputService.java`

Three additions, all scoped to SDL3 joystick enumeration / device open:

- **`enumerateJoystickIds()`** — if `SDL_GetJoysticks()` returns `null` (an SDL-level
  failure, not just "no devices"), log it as an error with the raw SDL error string:

  ```java
  } else {
      log.error("SDL_GetJoysticks() returned null: {}", SDLError.SDL_GetError());
  }
  ```

- **`sdlLoop()`** — on the first poll iteration after `SDL_Init` succeeds, log the full
  set of device IDs the initial enumeration found:

  ```java
  if (firstPoll) {
      log.debug("StarVizion initial joystick enumeration: {} device(s) found, ids={}", currentIds.size(), currentIds);
      firstPoll = false;
  }
  ```

- **`onDeviceAdded(int id)`** — log every newly-discovered device ID before attempting to
  open it, and escalate the open-failure log from `warn` to `error` (it already included
  the raw SDL error string):

  ```java
  log.debug("StarVizion joystick enumeration found device id={}", id);
  long handle = SDLJoystick.SDL_OpenJoystick(id);
  if (handle == 0L) {
      log.error("StarVizion SDL_OpenJoystick({}) failed: {}", id, SDLError.SDL_GetError());
      return;
  }
  ```

### Net result

- Every device ID seen by `SDL_GetJoysticks()` is now logged (initial enumeration line +
  per-device line on first sight).
- Every device that fails `SDL_OpenJoystick` is logged at `error` with the raw
  `SDL_GetError()` string.
- A `null` return from `SDL_GetJoysticks()` itself (distinct from "zero devices") is now
  logged at `error` with the raw SDL error string, instead of silently being treated as
  "no devices found".
- Existing logs (`SDL_Init failed`, `SDL3 native libraries not available`,
  `StarVizion SDL3 initialized`, `StarVizion device connected/disconnected`,
  `StarVizion SDL3 service stopped`) are now visible too, since the package is no longer
  capped at `error`.

Note: `SdlInputService.start()` is still only triggered lazily when the StarVizion tab is
activated (`StarVizionTabPanel.activate()`) — none of this fires until that happens.

## 3. Rebuild

Ran:

```bash
./gradlew shadowJar
```

Result: **BUILD SUCCESSFUL in 26s** (7 actionable tasks: 3 executed, 4 up-to-date).

Output jar updated: `distribution/elite_intel.jar` (96,211,986 bytes,
2026-06-09 19:14:44).
