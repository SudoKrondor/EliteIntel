# Reiter Dienste

<img src="images/services.png" class="inline" height="20" alt="Dienste"> Der Unterbau. Die Auswahl
**Sprache** gilt überall und sitzt ganz oben, dann folgen drei Unterreiter: **KI-Dienste**, **Audio** und
**Push To Talk**. Die Fußzeile enthält [**Einstellungen & Daten sichern**](#backup-settings-and-data),
**Wiederherstellen...** und den Update-Knopf — *App ist auf dem neuesten Stand* oder *Update verfügbar*.

---

## Sprache

Die Sprache deiner Sprachbefehle und der App-Oberfläche. Eine Auswahl stellt das ganze Fenster sofort neu
dar, und Vega sagt den Wechsel laut an.

Die Auswahl ist **gesperrt**, damit ein versehentlicher Klick oder Scroll deine Sprache nicht ändert.
Entferne den Haken am Schloss daneben, um sie zu ändern; danach sperrt sie sich wieder.

Unterstützt: Englisch, Russisch, Ukrainisch, Deutsch, Französisch, Spanisch, Italienisch, Portugiesisch,
brasilianisches Portugiesisch.

> Der **Journal-Ordner** liegt jetzt im [Reiter Kommandant](UI-Commander-Tab), der **Belegungsordner** im
> [Reiter Bindings](UI-Bindings-Tab).

---

## KI-Dienste

![KI-Dienste](images/ui-tab-settings-ai.png)

Zwei Schalter — einer für das Sprachmodell, einer für die Sprachausgabe — und die ungenutzte
Seite ist jeweils abgedunkelt, damit klar ist, welche aktiv ist.

Das ist der einzige Reiter der App, der mit einem **Entwurf** arbeitet. Nichts wird geschrieben,
bis du **Speichern** drückst, und wer mit offenen Änderungen den Reiter verlässt, wird nach
*Speichern*, *Verwerfen* oder *Weiter bearbeiten* gefragt.

### Sprachmodell (LLM)

Wechsle zwischen **LMSTudio** (lokal) und **Cloud-Einrichtung**.

**LMSTudio**

| Feld | Hinweise |
|-------|-------|
| **Adresse** | Standard ist die URL von LM Studio, `http://localhost:1234/v1/chat/completions`. Trage die IP eines anderen Rechners ein, wenn die Inferenz anderswo in deinem LAN läuft |
| **Modell** | Der Modellname. Ein Modell bedient sowohl Befehle als auch Abfragen |

Das unterstützte lokale Modell ist **`google/gemma-4-e4b`**. Elite Intel warnt beim Start, wenn
dein lokales Modell ein anderes ist; andere Modelle funktionieren womöglich schlecht oder gar
nicht.

Einrichtungsanleitungen: [LM Studio unter Linux](Install-LM-Studio-Linux) ·
[LM Studio unter Windows](Install-LM-Studio-Windows) ·
[AMD-RX-Serie](AMD-RX-7800XT-LLM-Setup) ·
[Headless-Ubuntu-Server](LM-Studio-Ubuntu-Server)

**Cloud-Einrichtung**

| Feld | Hinweise |
|-------|-------|
| **Anbieter** | Wähle einen: **Anthropic (Claude)**, **OpenAI**, **Google (Gemini)**, **xAI (Grok)**, **DeepSeek**, **Mistral** |
| **API-Schlüssel** | Dein Schlüssel für diesen Anbieter, mit einem Kästchen **Gesperrt** daneben, damit ein gespeicherter Schlüssel nicht versehentlich geändert wird. Entferne den Haken, um ihn zu ändern |

Ein Modell wählst du nicht — das passende wird für deinen Anbieter automatisch ausgewählt.

Ein Schlüssel gehört zu einem Anbieter: Wählst du einen anderen Anbieter, wird das
Schlüsselfeld geleert, damit du dessen Schlüssel einfügen kannst; wählst du deinen gespeicherten
Anbieter erneut, kommt sein Schlüssel zurück — ein versehentlicher Klick kostet also nichts.
**Speichern** bleibt ausgegraut, bis Anbieter und Schlüssel ausgefüllt sind.

Mistral hat ein kostenloses Kontingent und ist der einfachste Einstieg.
Wie du bei jedem Anbieter einen Schlüssel bekommst, steht unter [Cloud-LLM-Optionen](cloud-llm-options).

### Sprache (TTS)

Wechsle zwischen **Lokale Sprachsynthese** und **Cloud · Google**.

| Engine | Wo sie läuft | Hinweise |
|--------|--------------|----------|
| **Supertonic 3** | Auf deinem PC | Der Standard. 45 Stimmen, und jede Stimme spricht jede Sprache der App und des Spiels. Kein Schlüssel, nichts verlässt deinen PC |
| **Google** | Googles Server | Google Cloud Text-to-Speech. Braucht einen **Google-TTS-Schlüssel** (mit demselben Kästchen Gesperrt). Mit dem Regler **Google-WaveNet-Tonhöhe**, den die WaveNet-Stimmen nutzen, und dem Regler **Google-Sprechgeschwindigkeit** |

Funkverkehr spricht immer Supertonic 3, egal welche Engine du für Vega wählst.

> Ein Wechsel der Engine setzt die Stimme jedes Schiffs auf die Standardstimme der neuen Engine zurück.
> Schiffspersönlichkeiten bleiben erhalten. Du wirst vorher um Bestätigung gebeten.

<a id="voice-tuning"></a>

### Stimmen abstimmen

![Stimmen abstimmen](images/ui-voice-tuning.png)

Die Stimmen unterscheiden sich etwas in Lautstärke und Tempo, daher kannst du mit **Stimmen abstimmen…** jede
einzeln anpassen. Die Liste links ist in **Weibliche Stimmen** und **Männliche Stimmen** gegliedert; wähle eine,
um ihre Einstellungen zu sehen.

| Bedienelement | Was es tut |
|---------------|------------|
| **Geschwindigkeit** | Wie schnell diese Stimme spricht |
| **Lautstärke** | Eine Lautstärkekorrektur für diese Stimme |
| **Equalizer** | Sechs Bänder, um den Klang dieser Stimme zu formen |
| **Testen** | Spielt einen Satz mit den aktuellen Einstellungen ab |
| **Stimme zurücksetzen** | Setzt diese Stimme auf ihre Standardwerte zurück |

Es gibt keinen Speichern-Knopf: Eine Änderung wird gespeichert, sobald du den Regler loslässt. Du kannst
Stimmen abstimmen öffnen, egal welche Engine für Vega spricht, denn Supertonic 3 spricht in jedem Fall den Funk.

### Fußzeile

**Standardwerte wiederherstellen** setzt das Sprachmodell auf lokales LM Studio mit
Standardadresse und -modell zurück und speichert sofort. **Speichern** übernimmt alles andere;
es ist ausgegraut, bis sich tatsächlich etwas ändert, und daneben erscheint dann der Hinweis
**Nicht gespeicherte Änderungen**.

Beim Speichern startet nur neu, was nötig ist — ein neues Modell oder ein neuer Schlüssel
startet das Gehirn neu, eine neue Sprachausgabe oder ihr Schlüssel die Stimme.

---

## Audio

![Audio-Einstellungen](images/ui-tab-settings-audio.png)

### Audiogeräte

Auswahllisten **Mikro** und **Lautspr.**, oder *(Systemstandard)*. Dieselbe Auswahl gibt es über
den Knopf **Audiogeräte** im Vega-Reiter. Eine Änderung wirkt sofort — nur der Dienst, der das
Gerät nutzt, startet neu.

**Geräuschunterdrückung aktivieren** mit der Stärke **Niedrig / Mittel / Hoch**. Beginne mit
Mittel. Hoch ist für wirklich laute Räume — es ist aggressiv, und zu viel Filterung kann die
Erkennungsgenauigkeit kosten.

Unter den Geräten liegen zwei Reiter.

### Audiopegel

| Regler | Was er tut |
|--------|--------------|
| **Sprachlautstärke** | Wie laut Vega spricht |
| **Funklautstärke** | Wie laut Funkübertragungen sind. Ausgegraut, solange Funkübertragungen aus sind |
| **Signaltonlautstärke** | Der Bestätigungston — er ertönt, wenn die Spracherkennung fertig ist und das Sprachmodell deine Eingabe hat |
| **STT-Threads** | CPU-Threads für die Spracherkennung (4–11). Eine Mindestanforderung, keine Reservierung: die App fordert so viele an, nutzt, was der Prozessor hergibt, und gibt sie nach getaner Arbeit frei |

Wie schnell Vega spricht, stellst du je Stimme unter [Stimmen abstimmen](#voice-tuning) ein, bei einer
Google-Stimme mit **Google-Sprechgeschwindigkeit**. Die Musiklautstärke liegt im
[Reiter Jukebox](UI-Jukebox-Tab), damit du nie die falsche herunterdrehst.

### Funkübertragung

Wie Funknachrichten klingen.

| Element | Was es tut |
|---------|--------------|
| **Funk-Piepton am Anfang und Ende jeder Nachricht** | Rauschsperren-Töne um jede Übertragung, mit eigenem Lautstärkeregler |
| **Funkeffekt** | Ein stärkerer, verzerrter Funkklang |
| **Ausgewählte Effekte auf Funkchat und NPC-Nachrichten anwenden** | Töne und Funkeffekt oben gelten für den Funkverkehr. Ausgeschaltet behält Funksprache ihren Standardfilter |
| **Ausgewählte Effekte auf VEGA zu Fuß oder im SRV anwenden** | Vega klingt wie über Funk, wenn du nicht im Schiff bist |

### Mikrofonmonitor

Ein Live-Pegelmesser an der rechten Seite. So liest du ihn:

- **FLOOR** — dein Geräuschpegel, wenn du *nicht* sprichst.
- **GATE** — die Schwelle. Audio über dem Gate wird für die Erkennung aufgenommen; fällt es
  darunter, wird das Aufgenommene transkribiert und an das Sprachmodell geschickt.
- **CLIP** — du übersteuerst das Mikrofon. Alles dort oben wird schlecht erkannt.

Der Status zeigt **OPEN**, **MARGINAL**, **CLOSED** oder **HOT** (Übersteuerung). Unter dem Messer
erscheint ein Klartext-Hinweis, wenn etwas nicht stimmt: *Mikrofon nicht kalibriert* oder
*Mikrofon für den Raum zu leise* — erhöhe den Eingangspegel in den Soundeinstellungen deines
Betriebssystems und kalibriere neu. Ist das Mikrofon in Ordnung, wird kein Hinweis angezeigt.

Zeigt der Messer keinen klaren Abstand zwischen FLOOR und deinem Sprechpegel, führe **AUDIO
KALIBRIEREN** im Vega-Reiter aus — es setzt das Gate für dich und warnt, wenn der Abstand zu
klein ist, um damit zu arbeiten.

---

## Push To Talk

![Push to Talk](images/ui-tab-settings-push-to-talk.png)

Mit Push-to-Talk ist das Mikrofon zu, bis du eine Taste hältst. Was es ohne gehaltene Taste
aufnimmt, wird als Raumgeräusch verworfen.

| Element | Hinweise |
|---------|-------|
| **Push-to-Talk aktivieren** | Der Hauptschalter |
| **Controller** | Jeder angeschlossene Gamecontroller oder HOTAS. Dein gespeicherter Controller wird beim erneuten Verbinden automatisch wieder gewählt |
| **Taste** | Welche Taste darauf, mit der Beschriftung aus dem [Alias-Designer](UI-Bindings-Tab) |
| **Maustaste** | Ein zweiter Auslöser: *Mittlere Taste*, *Taste 4 (Zurück)* oder *Taste 5 (Vorwärts)*. Praktisch zu Fuß oder im SRV, wenn dein HOTAS außer Reichweite ist. Links und rechts werden nicht angeboten — damit feuerst du deine Waffen |

Taste halten, sprechen, loslassen. Das Drücken **unterbricht Vega auch mitten im Satz**, du musst
also nie warten, bis sie fertig ist.

Solange Push-to-Talk aktiv ist, ist der Knopf **SCHLAFEN / AUFWACHEN** im Vega-Reiter gesperrt —
die Taste ist das Tor. Eine Änderung hier wirkt beim nächsten Druck, und die Taste funktioniert,
ob du diesen Reiter je öffnest oder nicht.

---

<a id="backup-settings-and-data"></a>

## Einstellungen & Daten sichern

Für den Umzug auf einen neuen Rechner, oder einfach zur Vorsicht. **Einstellungen & Daten sichern** schreibt eine
`.zip` mit deinen Einstellungen, Datenbanken, eigenen Befehlen und den Sicherungen von BindForge.

**API-Schlüssel brauchen ein Passwort.** Wähle eines und drücke **Schlüssel sperren und speichern**, um deine
Schlüssel in die Sicherung einzuschließen. Zum Wiederherstellen brauchst du das Passwort. Wählst du **Ohne
API-Schlüssel**, enthält die Sicherung keine, und du gibst sie nach dem Wiederherstellen neu ein.

**Wiederherstellen...** spielt eine Sicherung zurück. Was sie ersetzt, bleibt in einem Sicherheitsordner im
App-Datenordner, und Elite Intel startet neu, um abzuschließen. Eine Sicherung aus einer neueren Version verlangt
zuerst ein App-Update. Ordner, die es auf dem neuen Rechner nicht gibt, werden zurückgesetzt; prüfe danach also den
Journal-Ordner im Reiter Kommandant und den Belegungsordner im Reiter Bindings.

> Das sichert **Elite Intel**. Um deine **Steuerungsdateien des Spiels** zu sichern, nutze die
> [Spielersicherungen](UI-Bindings-Tab) im Reiter Bindings.

---

## Wo die Einstellungen liegen

Alle Einstellungen und Daten werden auf deinem PC gespeichert:

- **Linux:** `~/.local/share/elite-intel/` (oder `$XDG_DATA_HOME/elite-intel/`)
- **Windows:** `%LOCALAPPDATA%\elite-intel\`

Die Datenbank liegt in `db`, eigene Befehle in `custom-commands` (mit eigenem `backups`) und die Sicherungen
deiner Steuerungsdateien in `playerbackups`, sofern du keinen anderen Ordner gewählt hast.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
