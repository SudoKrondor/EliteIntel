# Color Themes

**Status:** built 2026-11-14, slices 1 to 6, on `V1.2`. Not yet clicked through in the real app.

The commander picks, makes, imports and exports color themes for the app window. The tab is the last one under
**Commander**, named **Color Themes**. Applying a theme takes effect when the app restarts.

The **HUD overlay is not part of this.** Its colors stay where they are, in the overlay settings dialog, and they
can already change live. Nor are the Input Monitor's readout windows, whose colors are set in their own dialogs. **Fonts are not part of a theme** and cannot be changed from the interface.

---

## Why

About 8% of men have red-green color blindness, and the app's audience is, by its YouTube viewers, all men aged
35 to 70. With age, the eye's lens yellows and blue-yellow contrast drops, so blue-yellow problems matter more
than their rarity at birth suggests.

On 2026-11-14 the default palette was retuned for color blindness: success `7CE8A8`, warning `8FA6FF`, danger
`E8602C`, and system log text `76848D`. `HudPalette` explains the tuning. A default can only be tuned so far for
everyone at once, so the rest is up to the commander's own eyes and screen.

---

## Decisions

| Question | Decision |
|---|---|
| Applying a theme | Save it, then **ask** whether to restart now or later. Never restart on its own. |
| Switching live | No. `HudPalette` stays `static final`, and filling it once at startup avoids a large refactor. |
| Preview | Rendered by a short-lived second Java process from the real widgets, and clickable (see Preview). |
| Preview refresh | After each color is picked, not while the picker is dragged. A delay of about a second is fine. |
| Built-in themes | **Default** (the tuned palette), **High contrast**, **Clockwork Orange**, **Coffee**, **Hackers Green** and **Purple Rain** (added later). All read-only, so editing one makes a copy first. |
| Scope | One choice for the user, the same whichever commander is loaded: it is the same eyes. |
| Storage | Each theme is a JSON file, so it can be exported and shared. Only the active choice is in the database. |
| Tab name | "Color Themes". The one other accessibility request (a context-aware FSD toggle) is a checkbox under Global Ship Settings, not part of this. |

---

## The tab

```
+------------------+----------------------------------------------+-----------------+
| THEMES           |  VIEW AS  [Normal][Deuter.][Protan.][Tritan.]|  COLORS         |
|  > Default    *  |                                              |  [##] [##] [##] |
|    High contrast |   (preview image: buttons, fields, combo,    |  [##] [##] [##] |
|    My Night Ops  |    checkbox, tabs, table rows, status        |  ...            |
|                  |    readouts, mic meter, log lines - in       |                 |
|                  |    several states)                           |  ! 2 contrast   |
| [New] [Duplicate]|                                              |    warnings     |
| [Rename][Delete] |   Click any part to change its color.        |                 |
| [Import][Export] |                                              |  [Revert][APPLY]|
+------------------+----------------------------------------------+-----------------+
```

Built in slice 4 (`elite.intel.ui.screen.colorthemes.ColorThemesTabPanel`, the last tab under Commander). The
View-as switch and the contrast warnings come in slice 5.

* The list marks the theme the app is drawn in ("in use") and, once another is applied, that one too ("after
  restart"). Marking the applied theme "in use" before a restart was wrong: the app still drew the old one. Selecting a theme shows it in the preview without applying it. A
  theme with unsaved changes asks before another is selected.
* **Pointing** at a part of the preview, or at a swatch, dims everything else (`RoleMap.spotlight`, veiled in the
  modal scrim color) so where that color is used stands out. A tint over the part was tried first and barely
  showed on thin text. **Backgrounds are never spotlit** (`RoleMap.isSurface`: a role covering 2.5% or more of
  the preview; six do, from 3% to 40%, everything else is under 2%). Spotlighting one left the background clear
  and dimmed every widget on it, so the whole view went dark, which the user found confusing (2026-11-14). Over a
  background only the hand cursor says it can be changed, and a swatch shared by a background and smaller parts
  spotlights only the parts.
* **Pointing reaches** a part up to 5 px away (`RoleMap.roleAt` with a reach): on a background, the nearest part
  within reach; on a part, the smallest part within half of it, so text on a button wins near the text and the
  fill stays reachable in the padding. An exact-pixel lookup made text and thin frames nearly impossible to point
  at, the gaps between strokes being background (user feedback, 2026-11-14).
* **Pointing at a swatch isolates** its color instead of dimming the rest (`RoleMap.isolate`): everything it does
  not paint is covered opaquely, near-black behind a light color and light grey behind a dark one, and here
  backgrounds *are* shown, since the question is "what does this color paint?". A swatch the preview cannot show
  at all names its parts in plain words instead ("Not in the preview: Credit balance, Ship name in the top bar",
  `themes.part.<wireName>`). The tooltip counted a part the preview had no picture of, which read as a
  contradiction (2026-11-14). `ThemePreviewProcessTest` fails if a part the preview stops drawing has no name. Pointing at the
  preview itself, and at a contrast warning, still dims.
* **Colors** is every distinct color in the theme as a `HudColorSwatch`, with the number of parts using it as its
  tooltip, in two groups: **Backgrounds** and **Text and lines** (`ThemeRole.Kind`). A color both kinds show is a
  swatch in each group. Clicking one changes that color everywhere a role of its kind uses it, so changing text never
  changes a background. One swatch for both moved the application background along with selected text in Default,
  and a grey a commander gave a background and some text moved both, which surprised the user (2026-11-14). No swatch carries a name like "Text Field Frame
  Color": the preview is how the commander finds a color. The one exception is a part the preview cannot draw
  (above), which has no picture to find it by.
* **Revert** discards unsaved edits. **Save** writes the theme without making it the one in use (added in the
  build: a commander making a theme to share should not have to switch to it). **Apply** saves, makes it the one in
  use, and asks "Restart now / Later" - only when `AppRelauncher.canRelaunch()`. With no launcher to start (a run
  from the IDE) the button could not work, so the dialog only says to restart by hand. Until the app restarts, the footer says the
  theme in use changes on restart.
* **Built-in themes:** the first change to one creates the copy straight away ("Default copy"), selects it, and
  says so. The change then goes into the copy.
* **Export** writes the theme as it looks now, unsaved changes included: what the commander sees is what they share.
* **Rename** changes only the name inside the file. The file name, which is the id the active-theme setting
  stores, stays, so renaming the theme in use does not lose it (`ThemeLibrary`).
* The preview renders only while the tab is showing: the Commander screen builds every tab at startup, and the
  two render processes must not start on every app start.
* A picked color keeps the alpha of the color it replaces (`ThemeDraft`): the color picker has no alpha, and the
  see-through accent would otherwise turn solid.

**Known gap:** a swatch-only role (one the preview cannot show) that shares its color with other roles can only
be changed together with them. `shipNameText` and `creditsText` share the accent, for example. Splitting them
needs a way to pick a single role without the preview. Left out on purpose (2026-11-14): wait for feedback
from commanders after V1.2 ships.

### Changing a color: one part or every part

Several roles share a color. The accent cyan, for example, paints about 12 of them. When the commander picks a new
color for a part, the dialog asks:

* **Every part with this color** (the default): every role of the same kind (background, or text and lines) whose
  current color equals the clicked role's color takes the new color.
* **Only this part**: just the clicked role changes.

The grouping is by current color, not by a fixed base color in the file, so it follows whatever the commander
has already split apart.

---

## Preview

### Why a second process

The widgets read their colors from `HudPalette` and from the `UIManager` defaults that `AppView` installs, and
both are fixed for the life of the process. The running app therefore cannot draw a real widget in a draft palette.

* A hand-painted mock would be a second copy of every widget's drawing code, and it drifts.
* Loading the widget classes a second time in their own classloader still shares the global `UIManager`, so
  parts of each control would show the live theme. Each rebuild could also leak a classloader.

### How it works

Built in slice 3 (`elite.intel.ui.theme.preview`). `ThemePreviewProcess` starts `ThemePreviewRenderer` as a second
process on `java.home`'s own `java` (not this process's command: the installed app is started by a native launcher)
and `java.class.path`, headless, with:

* `-Dlog4j2.configurationFile=log4j2-theme-preview.xml`: errors to stderr only. **Never the app's `log4j2.xml`**,
  which rolls the app's log on every start and keeps three, so a preview on every color pick would push the
  commander's log history out within minutes;
* `-Delite.intel.db.url` pointing at a throwaway in-memory database, so a widget that reached for the database by
  mistake could never touch the commander's data;
* the request as arguments (`ThemePreviewRequest`: preview or role map, language, text anti-aliasing, LCD
  contrast, screen scale), and the draft theme's JSON on stdin. The PNG comes back on stdout.

The renderer installs the draft theme, then the look and feel through `HudLookAndFeel`, the one setup the app
uses too (moved out of `App` and `AppView` for this). It builds `ThemePreviewSample` and paints it, at the
screen's scale, onto an opaque image. One render takes about a second, JVM start included.

**Matching the app pixel for pixel.** A headless process gets no desktop text hints, so its text came out
heavier: 0.55% of the pixels differed, all of them text. The editor passes the app's own text anti-aliasing hint
(`UIManager`'s `KEY_TEXT_ANTIALIASING`, LCD on most desktops) and the renderer installs it. With that, a headless
render and the app's own were **identical, 0 of 774,076 pixels apart** (spike, 2026-11-14). LCD text needs an
opaque image, so the preview is never drawn with transparency.

**The sample** (`ThemePreviewSample`) is built from the real factories, in the app's language (passed in:
the plain `getText` would ask the database): the main and section tab rows, fields (at rest and disabled), a
combo, checkboxes, a segmented control, a slider, buttons (at rest, hovered, pressed, subtle, disabled), the five
status badges, a telemetry readout, the chat and the system log, a table with a selected row, and a dialog.
The dialog is added after the dark-palette pass, as real dialogs are. No widget in it may need the database or a
session.

**The role map** is the same sample rendered with every role in its own opaque code color (`RoleMap`: a grid
of four levels per channel) and text anti-aliasing off. It does not depend on the theme, so it is drawn once per
language and screen. A click reads the code under it, and on an anti-aliased edge takes the code most pixels
within 2 px carry. In the spike, 98.7% of the pixels mapped exactly.

**39 of the 49 roles can be clicked in the preview.** The other 10 are edited from the swatches only. (Three more,
`radioTransmissionLogText`, `streamOverlayBackground` and `warningPanelBackground`, were painted by nothing at all
and were deleted on 2026-11-14.)

* needing the database or a session: `shipNameText` (top status bar), `creditsText` (commander block);
* impossible without a real screen: `inputFocus` (keyboard focus), `selectionBackground` (text selected by mouse),
  `modalScrim` (the veil over the window while a dialog is open);
* only in one specialised dialog: `assignedInputBackground` (bindings capture);
* drawn only blended: `commanderMarker` (the chat rail is see-through);
* set on a widget the sample does show, but painted over before it reaches the screen: `readoutLabel` (the
  telemetry label), `logBackground` and `monospaceText` (a text area draws as a field). Why, role by role, is not
  yet checked: these may be dead in practice too.

Colors a widget derives from a role, such as `HudButton`'s glow, show up blended and map back only roughly.

### View as

The **View as** switch (a segmented control beside the preview's hint) puts the preview image through the
Machado 2009 simulation for deuteranopia, protanopia or tritanopia (`ColorVisionSimulation`, in linear light). It
costs no new render, and clicks still find their roles. It is the same model used to tune the default palette:
`ColorVisionSimulationTest` pins it to the values that tuning was measured with.

### Contrast warnings

`ThemeContrast` pairs each of the 20 text roles with the background it is drawn on, read from the widgets that
use them (a map of its own rather than a third field on every `ThemeRole`: most roles are not text). Disabled text
is left out, as WCAG exempts it. A see-through color is laid over its background first, so the ratio is what
reaches the screen. Any pair below 4.5:1 is listed under the swatches as "Hard to read": the text color on its
background as an "Aa" sample, and the ratio. Pointing at one spotlights that text in the preview. **Clicking it
fixes it** (`ThemeDraft.makeReadable`), without a picker: a commander who sees the warning cannot be expected to
know which way to move the color, or how far. In order, and a step is taken only if no other pair becomes hard to
read:

1. the text, moved further the way it already leans (light text lighter, dark text darker), mixed toward white or
   black only as far as 4.6:1 needs, so it keeps its hue;
2. the background, moved away from the text the same way;
3. a balance, when the colors cannot all reach 4.5:1 together: the move that most raises the worst ratio among the
   pairs these two roles are part of. The tab then says the text is as readable as these colors allow.

**A color is never flipped from light to dark.** The first version did that: white selected text on a light
purple accent (4.4:1) was "fixed" to black, which read 1.8:1 on the dark purple selected tabs (Purple Rain,
2026-11-14). Two causes, both fixed: selected text was checked against the accent only, though it also sits on the
two selected-tab fills and the pressed button (`ThemeContrast.PAIRS`); and the fix was allowed to flip. Some
combinations cannot be fixed fully, where one role is both text and the fill under other text (21:1 is the whole
range, 4.5 x 4.5 = 20.25). The accent was the worst case until the accent fill became a role of its own: Purple
Rain now fixes fully, the fill darkening under white text while the accent text keeps its contrast. **Fix all** applies the same rules in passes
and stops when a pass changes nothing. A fixed color is opaque. Warnings never stop a save: the commander's eyes and screen are theirs. Default and High
contrast have none.

---

## Themes

### The role table

`ThemeRole` (built in slice 2) holds one entry for each `HudPalette` color role a theme can set, 49 of the 50:

* a stable **wire name** for the JSON (`primaryAction`, `success`, `fieldBackground`...), which never changes
  when the Java constant is renamed;
* its default color, as `0xAARRGGBB`. This is the only place an app-window color literal is written: the old
  `HUD_COLOR_<HEX>` raw constants are gone;
* its **kind**: `BACKGROUND` (a fill other parts sit on) or `FOREGROUND` (text, lines, frames and marks). The
  editor groups swatches and "every part with this color" by it;
* for text roles, the background role it is drawn on, for the contrast warnings. That is added in slice 5, with
  the warnings.

Each `HudPalette` role is `ActiveTheme.color(ThemeRole.X)` and stays `public static final`.
`HUD_COLOR_ROLE_DISABLED_ICON` is the one role a theme does not set: it stays derived from
`HUD_COLOR_ROLE_DISABLED` at half alpha. `HUD_COLOR_ROLE_SELECTION_BACKGROUND` became a role of its own
(`selectionBackground`, the frame blue at half alpha by default). So did the commander's chat lane
(`userInputLogText`, `commanderMarker`): it used to alias success, and now starts at the same color.

**The accent fill** (`accentFill`, added 2026-11-14) is the accent where it fills an area under selected text: the
selected segment, selected rows and items in tables, lists and combos, selected text in fields, pressed subtle
buttons and steppers, selected toggles, held keys. `primaryAction` keeps the accent as text, lines, frames and marks.
They were one role, so a commander changing the accent text changed every selection with it. A theme that leaves
`accentFill` out shows its own `primaryAction` there, not the Default's cyan (`ThemeRole.follows`), so themes made
before the split look as they did. An `accentFill` equal to the theme's accent is not written.

`ED_HUD_REFERENCE.md`'s rules still hold: widgets name roles, and no widget hard-codes a color.

### File format

```json
{
  "format": 1,
  "name": "My Night Ops",
  "author": "CMDR Example",
  "colors": {
    "success": "#7CE8A8",
    "primaryAction": "#A633D7E8"
  }
}
```

* `#RRGGBB`, or `#AARRGGBB` for a color with alpha.
* **Only roles that differ from Default are written.** A role this file leaves out takes the Default value, so a
  role added in a later version simply takes its default.
* **A role this build does not know is dropped, not refused.** The same goes for a malformed value. Both are
  logged. The theme loads, which is the same rule `HudOverlayColor.parseOverrides` follows.
* A `format` newer than this build is refused on import, with a message.
* Import caps the file at 64 KB. A theme from a stranger is data: it holds colors and a name, and nothing in it
  is run.

### Where they live

* **Built-ins** ship in the jar: Default is the `ThemeRole` defaults, and High contrast is
  `resources/themes/high-contrast.json`. They are not editable, and they are not files in the data folder.
* **The commander's themes** go in `AppPaths.getAppDataDir()/themes/`, one file each, written through
  `AtomicFiles`. The file name is derived from the theme name. Not the install folder: on Windows it needs
  administrator rights, it is read-only in the Flatpak, and the updater replaces it.
* `themes` joins `AppDataArchive.FOLDERS`, so a backup carries the themes.
* **Import** copies a chosen file into `themes/`. When the name is already taken, it asks: replace, or keep both
  (the import gets " (2)").
* **Export** writes the selected theme to a file the commander picks. A built-in can be exported too, as a
  starting point for others.

### The active choice

The shared migration `12025__ui_theme.sql` adds `game_session.uiTheme TEXT NOT NULL DEFAULT ''`.

* `''` means Default, `builtin:high-contrast` means the built-in, and anything else is a file name in `themes/`.
* **The app always starts.** A missing or unreadable theme falls back to Default and logs why. It does not
  clear the setting, because a file may only be missing for now.
* Deleting the active theme sets the choice back to Default and says that it takes effect on restart.

### Startup

`App.main` runs `PendingRestore`, then `Database.init`, and only after that builds any UI. The active theme is
read right after `Database.init` and handed to `ActiveTheme` before anything touches `HudPalette`.

**To verify while building:** nothing touches `HudPalette` before that point (a restore notice, a setup dialog,
a splash). A guard in `ActiveTheme` should log loudly if `HudPalette` initializes before a theme was installed,
so such a regression shows up as a log line instead of a silently wrong theme.

---

## High contrast

High contrast is built for low vision and glare, not for one type of color blindness:

* a darker background, near-black, with brighter panel borders and field frames;
* body text at 7:1 or better, and secondary text at 4.5:1 or better;
* status colors at least as far apart as Default's under all three simulations.

Built in slice 5: `resources/themes/high-contrast.json`, id `builtin:high-contrast` (`ThemeStore.HIGH_CONTRAST_ID`),
listed after Default. What it holds, checked with the same scripts that tuned Default:

* black backgrounds (`000000`, panels `0A0D10`), white body text, frames `A0B4C0` (9.8:1 on black);
* **every text pair at 7.1:1 or better**, not only body text (`ThemeContrastTest` holds it to that);
* an opaque accent `5FD3ED` (Default's is see-through), with black text on it;
* states success `B5FEB6`, warning `6793FA`, danger `F37344`, from a search for colors that stay apart under all
  three simulations at 7:1 on black. The worst pair is 14.2 against Default's 12.4. Bright colors crowd together:
  the first draft, Default's hues brightened, fell to 4.0 under tritanopia. Under deuteranopia, OK and OFFLINE
  come out as pale cream and gold: apart by lightness more than by hue.

## Clockwork Orange

The third built-in (`resources/themes/clockwork-orange.json`, `ThemeStore.CLOCKWORK_ORANGE_ID`), added
2026-11-14 from the user's own theme: orange like the game's HUD and the app before V1.2. Two changes made it fit
to ship, both checked with the same scripts:

* the accent was see-through (`A6FF6600`, shown as `AC490A`, 3.3:1). It is now opaque `FF6600`: 6.3:1, and 6.3:1
  for the dark text on it;
* STANDBY (`FF6600`) and OFFLINE (`E8602C`) were the same orange, 4.9 apart with normal vision and 0.5 under
  tritanopia, and INFO was the accent, a third orange. STANDBY is now Default's blue `8FA6FF` and INFO a light grey
  `D8DEE3`; OFFLINE stays red-orange. The worst badge pair is 7.3 (OK and INFO under deuteranopia), against
  Default's 12.4. A pink danger would have kept every state apart from the orange accent too (11.3), but the user
  chose a red danger. The cost: danger text stays close to the orange controls for color-blind eyes (4.9).
* the selected main tab's fill was see-through `CC6600` (shown as `9D5107`): dark text on it read 3.2:1, found
  once selected text was checked on the tab fills. It is opaque `CC6600` now, 4.8:1.

---

## Colors outside the palette

Slice 1 (done 2026-11-14) swept the whole app for colors that do not come from `HudPalette`:

* **Fixed:** `InputMonitorPalette` (the Input Monitor tab) held three literal colors. It now names
  `HudPalette` roles (secondary text and danger), and stays as the pattern for a feature's own color names.
* **Fixed:** `AppTheme.applyDarkPalette` forced every `JEditorPane` to white and black. It had no visible
  effect, because `MarkdownViewPanel` turns its pane transparent and colors its HTML from roles, but it would
  have beaten a theme on any later editor pane. It now uses the background and primary text roles.
* **Follows a theme already:** the widgets that build an alpha variant or a mix from a role (`HudButton`,
  `HudStepper`, `StatusBadge`, `HudStatCell`, `HudLogArea`, `HudMicMeter`), and `HudTransportIcons`, whose
  white is only a mask that the tint pass replaces.
* **Out of scope on purpose:** the Input Monitor's readout windows (`ui/inputmonitor/overlay/`:
  `ReadoutWindow`, `AxesReadout`, `ButtonReadout`, `CounterReadout`, `KeyboardReadout`). Like the HUD overlay
  they float over the game, and the commander already picks their colors in each readout's own settings
  dialog. A theme for the app window does not reach them.

## Build slices

1. **Stragglers into the palette.** Done 2026-11-14, see above. No visible change.
2. **Theme model and startup.** Done 2026-11-14: `ThemeRole`, `ColorTheme`, `ThemeFile`, `ThemeStore`,
   `ActiveTheme`, the migration, the startup load in `App.main`, and `themes` in the backup. Tests: a file round trip, unknown and malformed roles, a missing or broken
   active theme falling back to Default, and a newer `format` refused.
3. **Spike, then the preview renderer.** Done 2026-11-14: the spike matched the app pixel for pixel at about a
   second a render, then `HudLookAndFeel`, `ThemePreviewSample`, `ThemePreviewRenderer`, `ThemePreviewProcess`
   and `RoleMap`.
4. **The Color Themes tab.** Done 2026-11-14. The list, the preview, the swatches, the one-part or every-part dialog, Import,
   Export, Apply and the restart prompt. Bundle keys in all nine locales.
5. **High contrast, View as, contrast warnings.** Done 2026-11-14.
6. **Site.** Done 2026-11-14 on `gh-pages-1.2` (screenshot `ui-tab-commander-color-themes.png` is on the
   `SCREENSHOTS.md` list). A Color Themes section on `UI-Commander-Tab.html` in all six locales, plus a line on the
   home page that the default palette is tuned for color blindness.
