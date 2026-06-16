# Benutzeroberfläche und Konfigurationsoptionen

### KI-Tab <img src="images/ai.png" class="inline" height="20" alt="AI">

Dies ist der Haupt-/Standard-Tab.

|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![tab-ai-buttons.png](images/tab-ai-buttons.png) | - **Dienste starten / stoppen**: Schaltet den KI-Stack ein/aus.<br/>- **Wach/Schlaf**: Im Wach-Modus hört die App ständig zu. Im Schlaf-Modus ignoriert die App Eingaben, es sei denn, der PTT-Knopf wird gedrückt, das Umgehungswort „Listen" wird verwendet oder der Befehl „Wake up!" gegeben.<br/>- **OBS-Overlay**: Zeigt ein schwarzes Overlay-Fenster mit der Commander/KI-Interaktion. In OBS einfügen, schwarzen Hintergrund ausblenden.<br/>- **Audiogeräte**: Eingabe-/Ausgabegerät auswählen. **Audio kalibrieren**: Audiokalibrierung für bessere Leistung ausführen. |
|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |


---

### Spieler-Tab <img src="images/controller.png" class="inline" height="20" alt="Player">

![playertab](images/tab-player.png)

- **Commander-Name**: Verwende dieses Feld, um deinen In-Game-Namen für die Sprachausgabe zu überschreiben.
- **Schiffsoptionen**: Diese Automatisierungen können ein- und ausgeschaltet werden. Nützlich für Commander mit Behinderungen.
- **Flottenmanagement**: Weise einzelnen Schiffen Stimmen, Persönlichkeiten und Sprachrhythmus zu. Persönlichkeiten funktionieren nur mit Cloud-LLMs. Das Zahnradsymbol öffnet die Schiffseigenschaften wie Auto-Honk und Handelsprofil.

![popup-ship-properties.png](images/popup-ship-properties.png)

- **System-Scan beim Eintritt**: Feuergruppe und Abzug auswählen. Wenn diese Option aktiviert ist, führt das Schiff beim Betreten eines Systems automatisch einen Entdeckungsscan durch. Falls das HUD im Kampfmodus ist, wechselt es in den Analysemodus, führt den Scan durch und wechselt wieder zurück.
- **Handelsprofil anpassen**: Diese Parameter können über die Benutzeroberfläche oder per Sprachbefehl gesetzt werden: „alter/change trade profile set [Parameter] to [Wert]"

---



### Aktionen-Tab <img src="images/keys-binding.png" class="inline" height="20" alt="Actions">

![tab-actions.png](images/tab-actions.png)

Der Tab **Aktionen / Belegungen** hat drei Abschnitte: Belegungen, Integrierte Befehle und Benutzerdefinierte Befehle.

- **Belegungen**: Verzeichnis, in dem sich deine Spielbelegungsdatei befindet. Ohne sie kann die App keine Spielsteuerung ausführen.
- **Profil**: Dein aktuelles In-Game-Belegungsprofil.
- **Datei**: Die Datei, die die aktuell verwendeten Belegungen enthält.

Du kannst deine Belegungen in diesem Bildschirm bearbeiten und als neues Profil speichern.

__HINWEIS – HOTAS/CONTROLLER werden angezeigt, können aber nicht über diesen Bildschirm konfiguriert werden. Nur Tastaturbelegungen (kann sich in Zukunft ändern).__


**Aktionen / Integrierte Befehle**

![tab-action-build-in-commands.png](images/tab-action-build-in-commands.png)


Zeigt eine Liste der integrierten Befehle. Ein Doppelklick auf einen Befehl öffnet ein Dialogfeld mit Informationen zum Befehl und ermöglicht das Vorschlagen einer besseren Übersetzung für die Lokalisierung.

---

### Einstellungen / Lokaler LLM-Tab <img src="images/settings.png" class="inline" height="20" alt="Settings">
- Gib die Adresse deines Inferenzservers ein. Standard ist `localhost` mit der Ollama-URL.
- Gib die Namen der zu verwendenden Modelle an. Siehe die [Lokale LLM-Anleitung](installing-local-llms).
- **LLM-Host**-Optionsfelder: Wähle zwischen Ollama und LM Studio.
- **Verwenden-Kontrollkästchen**: Aktivieren, um das lokale Modell anstelle der Cloud zu nutzen.

---

### Einstellungen / Audio <img src="images/mic.png" class="inline" height="20" alt="Audio">
- **Sprachlautstärke**: Steuert die Lautstärke der Sprachsynthese.
- **TTS-Sprechgeschwindigkeit**: Steuert die Geschwindigkeit der Sprachsynthese.
- **Piepton-Lautstärke**: Steuert die Lautstärke des Piepton-Indikators. Zeigt an, dass STT die Verarbeitung abgeschlossen hat und das LLM die Eingabe erhalten hat.
- **STT-Threads**: Legt die Thread-Zuweisung für die STT-Verarbeitung fest. Min/Max-Einstellung. Die App fordert das Minimum an, nutzt aber was der Prozessor bereitstellt. Threads werden nach der Verarbeitung freigegeben.
- **Lokale Text-to-Speech verwenden**: Überschreibt den Cloud-TTS-Schlüssel und verwendet lokale TTS.
- **Audio-Wellenform-Visualisierer**: Zeigt ein dynamisches Diagramm der Audioeingabe. Zeigt Rauschpegel, Audiosignal, Gate-Zonen und ggf. Übersteuerung an.


### Einstellungen / Cloud-LLM-Tab <img src="images/cloud.png" class="inline" height="20" alt="Cloud">
- **Cloud-LLM-Schlüssel**: Gib deinen API-Schlüssel ein. Unterstützte Anbieter: Gemini, OpenAI, Grok, Mistral, Deepseek und Anthropic/Claude.
- **Cloud-TTS-Schlüssel**: Gib deinen API-Schlüssel ein. Unterstützter Anbieter: Google.
- **Hinweis**: Das Kontrollkästchen „Verwenden" im Bereich Lokales LLM deaktivieren. Es überschreibt den Cloud-LLM-Schlüssel.


---

**LLM (KI-Gehirn)**

*Cloud-Option:* Gib deinen API-Schlüssel für Mistral, xAI, OpenAI oder Anthropic/Claude ein. Die App verwendet ein festes Modell pro Anbieter:
- **Mistral**: 'mistral-small-2506' (Kostenlos mit stündlichem Limit)
- **xAI**: `grok-4-1-fast-non-reasoning`
- **OpenAI**: `gpt-4.1-mini` (Befehle) / `gpt-5.2` (Abfragen)
- **Gemini Generative Language API**: `gemini-3.1-flash-lite-preview` für Befehle und Abfragen
- **Anthropic/Claude**

*Lokale Option:* Schlüssel leer lassen, lokale LLM-Felder ausfüllen und **☑ Verwenden** neben dem lokalen LLM aktivieren. Siehe [Lokale LLM-Anleitung (Linux)](Install-Ollama-Local-LLM-Linux) / [Lokale LLM-Anleitung (Windows)](Install-Ollama-Local-LLM-Windows).
- **LLM-Adresse**: Standard ist `localhost`. Durch die IP eines anderen PCs ersetzen, wenn Ollama auf einem separaten Rechner läuft.
- **Befehls-LLM**: verarbeitet die Interpretation von Sprachbefehlen.
- **Abfrage-LLM**: verarbeitet die Datenanalyse. `tulu3:8b` ist das Minimum. Größere Modelle liefern bessere Ergebnisse.

---

# Keine lokale Hardware? Nutze ein Cloud-LLM.

Die Kosten variieren je nach gewähltem Cloud-Dienst und Spielzeit.

### KOSTENLOSE CLOUD-Option: Mistral
1. Gehe zur [Mistral Console](https://console.mistral.ai/home)
2. Konto mit einer gültigen, verifizierbaren E-Mail-Adresse erstellen.
3. KEINE KREDITKARTE ERFORDERLICH
4. Eine „Organisation" erstellen (beliebiger Name, z. B. „Elite Intel").
5. Einen API-Schlüssel generieren. Den Schlüssel in die App eingeben und die App neu starten.


### Option A: xAI-API-Schlüssel
1. Gehe zur [xAI Console](https://console.x.ai/).
2. Registrieren oder anmelden.
3. Zum API-Bereich navigieren und einen neuen API-Schlüssel generieren.
4. Guthaben zum Konto hinzufügen.
5. Den Schlüssel in das Feld **LLM** einfügen und das Sperrfeld aktivieren.

### Option B: OpenAI-API-Schlüssel
1. Gehe zur [OpenAI Platform](https://platform.openai.com/).
2. Registrieren oder anmelden.
3. Zum API-Bereich navigieren und einen neuen API-Schlüssel generieren.
4. Den Schlüssel in das Feld **LLM** einfügen und das Sperrfeld aktivieren.

### Option C: Anthropic/Claude-API-Schlüssel
1. Gehe zur [Claude Platform](https://platform.claude.com).
2. Mit E-Mail oder Google anmelden. Hinweis: Die Authentifizierung erfolgt über einen Magic-Link per E-Mail.
3. Unter **Einstellungen → Abrechnung** Guthaben hinzufügen, bevor ein Schlüssel erstellt wird. Ein auf einem nicht aufgeladenen Konto erstellter Schlüssel funktioniert nicht, auch wenn danach Guthaben hinzugefügt wird.
4. Unter **API-Schlüssel** einen Schlüssel erstellen.
5. In das Feld **LLM** einfügen, Sperrfeld aktivieren und Dienste im KI-Tab starten oder neu starten.

### Google-TTS-Schlüssel erhalten (14 Stimmen)

1. Gehe zur [Google Cloud Console](https://console.cloud.google.com/).
2. Anmelden oder Konto erstellen.
3. Ein neues Projekt erstellen.
4. Die **Generative Language API** für LLM und/oder die **Cloud Text-to-Speech API** für TTS aktivieren.
5. Unter **Anmeldedaten** einen API-Schlüssel erstellen und kopieren.
6. **Schlüssel einschränken**: Auf den gerade erstellten Schlüssel klicken. Auf der Schlüsseldetailseite auf **Schlüssel einschränken** klicken. Ein Dropdown erscheint. Jede aktivierte API (STT und/oder TTS) ankreuzen und dann auf **Speichern** klicken.
7. Den Schlüssel in die Felder **Speech to Text** und/oder **Text to Speech** in der App einfügen. Sperrfelder aktivieren.

---

## App-Einstellungen und Datenverzeichnis

App-Einstellungen und Daten werden in einer SQLite-Datenbank gespeichert unter:
- **Linux:** `~/.local/share/elite-intel/elite-intel/db/`
- **Windows:** `%APPDATA%\elite-intel\db\`

----
Bei Problemen bitte über Matrix kontaktieren. Fehlerberichte und Pull Requests sind willkommen.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
