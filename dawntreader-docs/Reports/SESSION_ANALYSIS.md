# SystemSession & PlayerSession — Analysis

Analysis only — no code changes. Covers `elite.intel.session.SystemSession` and
`elite.intel.session.PlayerSession`, plus the DAOs/managers they sit on top of.

## 1. What `SystemSession` holds and provides

`SystemSession` (`elite.intel.session.SystemSession`) is a singleton facade over the
single-row `game_session` table (`GameSessionDao`), with one extra dependency on
`ShipManager`/`ShipDao` for the *currently piloted ship's* voice/personality settings. It is
best described as **app/runtime configuration**, not gameplay state — nothing in it changes as
a result of flying around the galaxy.

What it provides access to:

- **Voice & TTS settings** — `getGoogleVoice()` / `getKokoroVoice()` (resolved from the current
  ship's `voice` column via `ShipManager`, defaulting to STEVE/BELLA), `setSpeechSpeed()` /
  `getSpeechSpeed()`, `setBeepVolume()` / `getBeepVolume()`, `getVoiceVolume()` /
  `setVoiceVolume()`.
- **AI personality** — `getAIPersonality()` (`ShipPersonality`) and `getAICadence()`
  (`ShipCadence`), both read from the current ship's row, defaulting to `CASUAL`/`IMPERIAL`.
- **Sleep / privacy (push-to-talk) mode** — `isSleepingModeOn()` / `stopStartListening(boolean)`,
  backed by `game_session.privacyModeOn`. This is the same flag flipped by the WAKEUP/SLEEP voice
  commands and by `InputSettingsPanel`'s push-to-talk button handling.
- **Voice-activity thresholds** — `getRmsThresholdHigh/Low()` / setters, cached in memory
  (`rms`/`floor` fields) after first DB read.
- **Encrypted API keys** — `getTtsApiKey()` / `setTtsApiKey()` and `getAiApiKey()` /
  `setAiApiKey()`, stored encrypted via `Cypher` in `encryptedTTSKey` / `encryptedLLMKey`.
- **Local LLM / TTS configuration** — `useLocalCommandLlm()` / `useLocalQueryLlm()` /
  `useLocalTTS()` toggles, `getLocalLlmProvider()` (`LocalLlmProvider` enum, defaults to
  `OLLAMA`), and address/model getters/setters for both Ollama (`getOllamaAddress()`,
  `getOllamaCommandModel()`, `getOllamaQueryModel()`) and LM Studio (`getLmStudioAddress()`,
  etc.).
- **STT configuration** — `getSttThreads()` / `setSttThreads()`.
- **Language** — `getLanguage()` / `setLanguage()` (shared between GUI and voice command
  aliases, persisted in the legacy `aiLanguage` column).
- **Audio devices** — `getAudioInputDevice()` / `getAudioOutputDevice()` and setters — these are
  **microphone/speaker** device names for STT/TTS, not joystick/HOTAS devices.
- **Conversational mode** — `setConversationalMode()` / `conversationalModeOn()`.
- **Misc** — `getDesignation()` (current ship's name, via `ShipManager`/`ShipDao`),
  `readVersionFromResources()` (reads `/version.txt` from the jar), `clearChatHistory()`
  (delegates to `ChatHistoryDao`).

`SystemSession` does **not** register on the EventBus and has no `@Subscribe` methods — it is a
pure data-access facade, called synchronously by UI/settings code and command handlers.

## 2. What `PlayerSession` holds and provides

`PlayerSession` (`elite.intel.session.PlayerSession`) is a singleton that aggregates ~15
narrower manager singletons (`ShipScansManager`, `MissionManager`, `BountyManager`,
`MiningTargetManager`, `StationMarketsManager`, `RankAndProgressManager`,
`FleetCarrierManager`, `SquadronCarrierManager`, `BioSamplesManager`, `ShipLoadoutManager`,
`GenusAnnouncementManager`, `CargoHoldManager`, `ReputationManager`, `TargetLocationManager`,
`FsdTargetManager`, `LocationManager`) plus direct reads/writes against the single-row `player`
table (`PlayerDao`). This is the **commander/gameplay state** model — everything that describes
"who the player is and what's happened to them this campaign."

Grouped by what it provides:

- **Commander identity & display name**
  - `getPlayerName()` / `setPlayerName()`, `getAlternativeName()` / `setAlternativeName()`,
    `getInGameName()` / `setInGameName()`, `getPlayerHighestMilitaryRank()` /
    `setPlayerHighestMilitaryRank()`, `getPlayerMissionStatement()` / `setPlayerMissionStatement()`.
  - `getConfiguredPlayerName()` — fallback chain: `alternativeName` → `playerName` →
    `inGameName` → literal `"Commander"`.
  - `getVariablePlayerName()` — builds a list from alternative name, an honorific
    (`Ranks.getPlayerHonorific()`), player name, and localized military rank, then picks one at
    random — used to vary how the AI addresses the player in speech.
  - `setCurrentShip()` / `setCurrentShipName()` (setters only — no corresponding getters in
    `PlayerSession`; the values live in `PlayerDao.Player.currentShip` /
    `currentShipName` and would need to be read via `PlayerDao` directly if needed elsewhere).
- **Economy / lifetime stats** — bounty totals (`addBountyReward`, `getTotalBountyClaimed`,
  `setBountyCollectedLiveTime`/`getBountyCollectedLiveTime`, `clearBounties`), trade profits
  (`getTradeProfits` / `setMarketProfits`), `getPersonalCredits` /
  `setPersonalCreditsAvailable`, `getHighestTransaction` / `setHighestSingleTransaction`,
  exploration/exobiology profits, `getTotalDistanceTraveled` / `getTotalHyperspaceDistance`,
  `getTotalSystemsVisited`, `setInsuranceClaims`, `setShipsOwned`, `setGoodsSoldThisSession`,
  `setCrewWagsPayout`, `setCurrentWealth`, `setSpeciesFirstLogged`.
- **Location & navigation**
  - `setCurrentLocationId(bodyId, systemAddress)` / `getLocationData()` (current body + system
    address pair).
  - `getPrimaryStarName()` / `setCurrentPrimaryStarName()`.
  - `getLastScan()` / `setLastScan()` — last-scanned body, resolved via `LocationManager`.
  - `getHomeSystem()` / `setHomeSystem()`.
  - `getTracking()` / `setTracking()` — a `TargetLocation` via `TargetLocationManager`.
  - `getFsdTarget()` / `setFsdTarget()` — current FSD jump target via `FsdTargetManager`.
  - `getFinalDestination()` / `setFinalDestination()`.
  - `getLastKnownCarrierLocation()` / `setLastKnownCarrierLocation()`,
    `getCarrierDepartureTime()` / `setCarrierDepartureTime()`.
- **Ship state**
  - `getShipLoadout()` / `setShipLoadout()` — full `ShipLoadOutDto` (modules, hardpoints, etc.)
    via `ShipLoadoutManager`.
  - `getShipCargo()` / `setShipCargo()` — current cargo manifest via `CargoHoldManager`.
  - `isShipAutoDeparted()` / `setShipAutoDeparted()` — **in-memory only** flag, not persisted.
  - `getCurrentPartial()` / `setCurrentPartial()` — current exobiology genus in progress.
- **Fleet carriers** — `getFleetCarrierData()` / `setFleetCarrierData()`,
  `getSquadronCarrierData()` / `setSquadronCarrierData()`, `setCarrierStats()`.
- **Missions, bounties, mining, markets, bio samples, reputation, ship scans** —
  `addMission`/`removeMission`/`getMission`, `getBounties()` (+ `addBounty`/`removeBounty`),
  `getMiningTargets()` (+ add/remove/clear), `saveMarket()`, `getBioCompletedSamples()` (+
  `addBioSample`/`clearBioSamples`), `setReputation()`, `putShipScan`/`getShipScan`/`clearShipScans`
  (raw scan JSON cached by key).
- **Announcement toggles** — radio transmission, mining, navigation, discovery, route, and radar
  contact announcements (`is*AnnouncementOn()` / `set*AnnouncementOn()`), plus
  `addAnnouncedGenusPayment` / `paymentHasBeenAnnounced` / `clearGenusPaymentAnnounced` to avoid
  re-announcing the same exobiology payout.
- **Game/version metadata** — `setGameVersion()` / `setGameBuild()` (setters only, no getters
  exposed on `PlayerSession`).
- **File system paths** — `getJournalPath()`/`setJournalPath()` and
  `getBindingsDir()`/`setBindingsDir()` (see section 3).
- **Session UUID** — `getUUD()` — a random UUID generated once per process start (not
  persisted), used as a session identifier.
- **Rank/progress** — `getRankAndProgressDto()` / `setRankAndProgressDto()` via
  `RankAndProgressManager`.

## 3. Relevance to BindForge

### Game running state
**Neither `SystemSession` nor `PlayerSession` tracks whether Elite Dangerous itself is
running.** There is no `isGameRunning()`/process-check method in either class or in anything
they directly depend on (`PlayerDao`, `GameSessionDao`, `ShipDao`, the gameplay managers). The
closest related concept in the `elite.intel.session` package is the **sibling class**
`elite.intel.session.Status` (backed by `StatusDao`, decoding the journal's `Status.json` via
`StatusFlags`) — it exposes things like `isDocked()`, `isInMainShip()`, `isOnFoot()`,
`isInSupercruise()`, etc. That reflects the *player's in-game state* (and, indirectly, that the
journal/`Status.json` is being actively updated), but it is not referenced by `SystemSession` or
`PlayerSession`, and it's still not a direct "is the `.exe` running" check. If BindForge needs an
actual game-running signal, it isn't modeled anywhere in this part of the codebase yet.

### File paths
This is where the real overlap is, both on `PlayerSession`:

- **`PlayerSession.getBindingsDir()` / `setBindingsDir(String)`** — returns the directory
  containing the game's `.binds` files. If `player.bindings_dir` is unset, it defaults per-OS:
  - Windows: `%USERPROFILE%\AppData\Local\Frontier Developments\Elite Dangerous\Options\Bindings`
  - Linux: `~/.var/app/elite.intel.app/ed-bindings`
  - macOS: `~/Library/Application Support/Frontier Developments/Elite Dangerous/Options/Bindings`

  This is **the** entry point BindForge needs to locate `.binds` files to read/write axis and
  button bindings.
- **`PlayerSession.getJournalPath()` / `setJournalPath(String)`** — analogous OS-aware lookup
  for the journal directory (`Saved Games\Frontier Developments\Elite Dangerous` on Windows,
  etc.). Less directly relevant to BindForge itself, but useful if BindForge ever needs to
  correlate bindings against journal-reported hardware (e.g. via the `LoadGame`/`Loadout`
  events parsed elsewhere).

Both getters follow the same pattern: DB override via `PlayerDao.Player` (trimmed to `null` if
blank) falling back to a hardcoded OS-specific default from `OsDetector`.

### Commander identity
- `PlayerSession.getConfiguredPlayerName()` and `getVariablePlayerName()` are the two
  "display name for this commander" accessors — `getConfiguredPlayerName()` is the
  deterministic one (alternativeName → playerName → inGameName → "Commander") and is the one
  BindForge would want for e.g. labeling a saved binding profile per-commander.
- `getPlayerHighestMilitaryRank()`, `getAlternativeName()`, `getPlayerName()`, `getInGameName()`
  are the raw underlying fields if finer control is needed.
- `SystemSession.getDesignation()` is **not** commander identity — it's the *current ship's*
  name (e.g. "Indomitable Will"), sourced via `ShipManager`/`ShipDao`. Worth being careful not to
  conflate the two when BindForge needs to decide what to call a saved profile (ship vs.
  commander).

## 4. Relevance to `elite.intel.devices`

Overall: **very little**. `SystemSession` and `PlayerSession` model app configuration and
commander/gameplay progress respectively — neither has any notion of joysticks, HOTAS devices,
SDL3, or `.binds` axis/button mappings. That's intentionally the new `elite.intel.devices`
package's exclusive domain (per `EliteIntel_Devices_Spec.md`).

The one indirect link:

- **`PlayerSession.getBindingsDir()`** is the directory where `.binds` files live, and per the
  devices spec, `DeviceIdentity.bindsHexId` (VID+PID hex) is meant to correlate against the
  `Device=` attribute inside those `.binds` files. Any future code that cross-references a
  connected `elite.intel.devices.model.Device` against its `.binds` entries (BindForge, or a
  "this Vizlet button maps to control X" feature) would use `PlayerSession.getBindingsDir()` to
  find the files — `elite.intel.devices` itself has no reason to read `PlayerSession`.

One naming trap to flag: `SystemSession.getAudioInputDevice()` / `getAudioOutputDevice()` sound
device-related but are about the **microphone/speaker** used by STT/TTS — completely unrelated
to `elite.intel.devices`'s SDL3 joystick/HOTAS devices. No relationship, just a name collision
risk for anyone skimming both APIs.

## 5. Events published / subscribed

- **`PlayerSession`**
  - Registers itself on the bus in its constructor: `EventBusManager.register(this)`.
  - Exactly one `@Subscribe` handler: `onBounty(BountyDto data)` — forwards the bounty straight
    into `BountyManager.add()`. This is the only reactive/event-driven entry point into
    `PlayerSession`; every other method is a plain synchronous call from other code (command
    handlers, query handlers, journal subscribers, etc.) that read/write through it.
  - Publishes nothing itself.
- **`SystemSession`**
  - Does not call `EventBusManager.register()` and has no `@Subscribe` methods. Purely a
    passive, synchronously-called configuration facade — every read/write is a direct
    `Database.withDao(...)` call triggered by the caller.
  - Publishes nothing.
- **Sibling classes worth noting** (not referenced by either `SystemSession` or
  `PlayerSession`, but live in the same `elite.intel.session` package and came up while tracing
  dependencies): `ClearSessionCacheEvent` and `LoadSessionEvent` are `BaseEvent` subclasses
  registered in `elite.intel.gameapi.journal.EventRegistry` and published from `App.java` at
  startup — they're part of the journal-replay/cache-priming bootstrap sequence, not something
  either session class emits or listens for.
