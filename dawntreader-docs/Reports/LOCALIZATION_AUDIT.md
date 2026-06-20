# InputSettingsPanel — Localization Audit

Audit only — no code changes. Scans
`app/src/main/java/elite/intel/ui/view/settings/InputSettingsPanel.java` for hardcoded
English UI strings (labels, button text, placeholders, tooltips) that bypass
`getText()` / `MultiLingualTextProvider` and should be moved into
`app/src/main/resources/i18n/gui.properties`.

## Method

Grepped the file for all double-quoted string literals and checked each one against
`getText("...")` usage.

## Findings

### 1. Hardcoded per-button combo item: `"Button " + i`

- **Location:** `populateButtonCombo(Device device)`,
  `app/src/main/java/elite/intel/ui/view/settings/InputSettingsPanel.java:237`
  ```java
  for (int i = 1; i <= device.buttonCount(); i++) {
      buttonCombo.addItem("Button " + i);
  }
  ```
- **Issue:** Every item in the button-selection combo (`Button 1`, `Button 2`, ...) is
  built from the literal English word `"Button "`. This is the only string in the file
  that does not go through `getText()`.
- **Recommended `gui.properties` key:** `settings.input.button.label`, with a
  `MessageFormat` placeholder for the button number:
  ```properties
  settings.input.button.label=Button {0}
  ```
- **Recommended call site change:**
  ```java
  buttonCombo.addItem(getText("settings.input.button.label", i));
  ```
  `MultiLingualTextProvider.getText(String key, Object... args)` already applies
  `MessageFormat.format(pattern, args)` when `args` is non-empty, so this requires no
  changes to the provider itself.
- **Naming note:** this is distinct from the two existing keys already used elsewhere in
  this file:
  - `settings.input.button=Button` — the row label ("Button:" next to the combo).
  - `settings.input.button.placeholder=Select a button` — the combo's placeholder item.

  Both already exist in `gui.properties` (lines 404–405) and are correctly referenced via
  `getText()`. The new `settings.input.button.label` key would sit alongside them.

## Everything else: clean

All other string literals in the file are either:

- Javadoc/comment text (not user-facing), or
- Already routed through `getText(...)`:
  - `settings.input.enablePushToTalk`
  - `settings.input.controller`
  - `settings.input.controller.placeholder` (used twice — `buildUi()` and
    `refreshControllerCombo()`)
  - `settings.input.button`
  - `settings.input.button.placeholder` (used twice — `buildUi()` and
    `populateButtonCombo()`)
  - `settings.input.mode.toggle`
  - `settings.input.mode.hold`

No other hardcoded labels, button text, placeholders, or tooltips were found.

## Aside: locale coverage of existing `settings.input.*` keys

Not part of the requested audit (no hardcoded strings involved), but noted while
checking `gui.properties`: the `settings.input.*` keys (including the seven listed
above) exist only in the base `app/src/main/resources/i18n/gui.properties` — they are
absent from `gui_de.properties`, `gui_es.properties`, `gui_fr.properties`,
`gui_ru.properties`, and `gui_uk.properties`. `MultiLingualTextProvider.resolveText()`
falls back to the base bundle for missing keys, so this isn't a bug, but the entire
"Input" settings tab (and any new `settings.input.button.label` key) currently renders
in English regardless of the selected language.
