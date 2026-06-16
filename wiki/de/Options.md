# Oberflächen- und Konfigurationsoptionen

### KI-Tab <img src="images/ai.png" class="inline" height="20" alt="AI">

Dies ist der Haupt-/Standardtab.

|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![tab-ai-buttons.png](images/tab-ai-buttons.png) | - **Dienste starten / stoppen**: Schaltet den KI-Stack ein oder aus.<br/>- **Wach/Schlaf**: Im Wach-Modus hört die App ständig zu. Im Schlaf-Modus ignoriert die App Eingaben, es sei denn, der PTT-Knopf wird gedrückt, das Übergangswort "Listen" wird verwendet oder der Befehl "Wake up!" wird gegeben.<br/>- **OBS-Overlay**: Zeigt ein schwarzes Overlay-Fenster mit der Interaktion zwischen Commander und KI. In OBS einfügen und den schwarzen Hintergrund ausblenden.<br/>- **Audiogeräte**: Eingabe-/Ausgabegerät auswählen. **Audio kalibrieren**: Audiokalibrierung für bessere Leistung ausführen. |
|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |


---

### Spieler-Tab <img src="images/controller.png" class="inline" height="20" alt="Player">

![playertab](images/tab-player.png)

- **Commander-Name**: Verwende dieses Feld, um deinen In-Game-Namen für die Sprachausgabe zu überschreiben.
- **Schiffsoptionen**: Diese Automatisierungen können ein- und ausgeschaltet werden. Nützlich für Commander mit Behinderungen.
- **Flottenmanagement**: Weise einzelnen Schiffen Stimmen, Persönlichkeiten und Kadenz zu. Persönlichkeiten funktionieren nur mit Cloud-LLMs. Das Zahnradsymbol öffnet die Schiffseigenschaften wie Auto-Honk und Handelsprofil.

![popup-ship-properties.png](images/popup-ship-properties.png)

- **System-Honk beim Eintritt**: Feuergruppe und Abzug auswählen. Wenn diese Option aktiviert ist, führt das Schiff beim Eintritt einen Entdeckungsscan durch. Falls das HUD im Kampfmodus ist, wechselt es in den Analysemodus, führt den Scan durch und wechselt wieder zurück.
- **Handelsprofil anpassen**: Diese Parameter können über die Benutzeroberfläche oder per Sprachbefehl gesetzt werden: "alter/change trade profile set [Parameter] to [Wert]"

---



### Aktionen-Tab <img src="images/keys-binding.png" class="inline" height="20" alt="Actions">

![tab-actions.png](images/tab-actions.png)

Der Tab **Aktionen / Belegungen** hat drei Abschnitte: Belegungen, Integrierte Befehle und Benutzerdefinierte Befehle.

- **Belegungen**: Verzeichnis, in dem sich deine Spielbelegungsdatei befindet. Ohne diese Datei kann die App keine Spielsteuerung ausführen.
- **Profil**: Dein aktuelles In-Game-Belegungsprofil.
- **Datei**: Die Datei, die die aktuell verwendeten Belegungen enthält.

Du kannst deine Belegungen in diesem Bildschirm bearbeiten und als neues Profil speichern.

__HINWEIS  HOTAS/CONTROLLER werden angezeigt, können aber nicht über diesen Bildschirm konfiguriert werden. Nur Tastaturbelegungen (kann sich in Zukunft ändern).__


**Aktionen / Integrierte Befehle**

![tab-action-build-in-commands.png](images/tab-action-build-in-commands.png)


Zeigt eine Liste der integrierten Befehle. Ein Doppelklick auf einen Befehl öffnet ein Dialogfeld mit Informationen zum Befehl und ermöglicht das Vorschlagen einer besseren Übersetzung für die Lokalisierung.

**Benutzerdefinierte Befehle**

![acttions cuystom commands](images/tab-actions-custom-commands.png)

In diesem Bildschirm kannst du eine benutzerdefinierte Aktion definieren, die die App auf deinen Befehl hin ausführt.

- Klicke auf die Schaltfläche NEU, um ein Popup-Fenster zu öffnen, in dem du deine benutzerdefinierte Aktion definieren kannst.

![popup-custom-action.png](images/popup-custom-action.png)

- Gib den Aktionsnamen ein. HINWEIS: Der Aktionsname muss Wörter (Tokens) enthalten, die durch Unterstriche _ getrennt sind.
- Gib einen Namen für deine benutzerdefinierte Aktion an.
- Gib eine Beschreibung für deine benutzerdefinierte Aktion an.
- Gib Trainingswörter ein – das sind Bedeutungs-Tokens. Das LLM versucht, den gesprochenen Befehl anhand der höchsten Wahrscheinlichkeit der Aktion zuzuordnen. Je wahrscheinlicher deine Tokens auf die Aktion passen, desto häufiger wird sie zurückgegeben.

Um benutzerdefinierte Aktionen zu verwenden, sprich ganz natürlich. Du musst dir die genauen Wörter nicht merken, aber du musst eine präzise Bedeutung vermitteln, damit das LLM deinen Befehl mit der höchsten Wahrscheinlichkeit der richtigen Aktion zuordnet.

---


Bei Problemen bitte über Matrix kontaktieren. Fehlerberichte und Pull Requests sind willkommen.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
