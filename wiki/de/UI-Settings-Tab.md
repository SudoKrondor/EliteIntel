# Reiter Einstellungen

<img src="images/settings.png" class="inline" height="20" alt="Einstellungen"> Der Unterbau. Ein
Streifen **Allgemein**, der überall gilt, dann drei Unterreiter: **KI-Dienste**, **Audio** und
**Push To Talk**. Der Update-Knopf sitzt in der Fußzeile — *App ist auf dem neuesten Stand* oder
*Update verfügbar*.

---

## Allgemein

Über den Unterreitern angezeigt, weil es für alle gilt.

**Sprache** — die Sprache deiner Sprachbefehle und der App-Oberfläche. Eine Auswahl stellt das
ganze Fenster sofort neu dar, und Vega sagt den Wechsel laut an.

Unterstützt: Englisch, Russisch, Ukrainisch, Deutsch, Französisch, Spanisch, Italienisch,
Portugiesisch (Portugal), Portugiesisch (Brasilien).

**Journal-Ordner** — wo Elite Dangerous seine Journaldateien schreibt. Optional: leer gelassen
wird der Standardort deiner Plattform verwendet. So weiß Elite Intel, was um dein Schiff herum
passiert; ist er falsch, ist die App praktisch blind und sagt das beim Start. Ein unbrauchbarer
Ordner wird abgelehnt und die vorherige Einstellung bleibt.

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
[AMD-RX-Serie](AMD-RX-7800XT-LLM-Setup)

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

Wechsle zwischen **Lokal · Kokoro / Supertonic** und **Cloud · Google / Edge**. Jede Seite hat
einen zweiten Schalter zur Wahl der Engine.

| Engine | Wo sie läuft | Hinweise |
|--------|---------------|-------|
| **Kokoro** | Auf deinem PC | Der Standard. Kein Schlüssel, nichts verlässt deinen PC. Kann kein Kyrillisch aussprechen — siehe unten |
| **Supertonic 3** | Auf deinem PC | Zehn Stimmen, alle Sprachen einschließlich Russisch und Ukrainisch. Kein Schlüssel. Mit dem Regler **Supertonic-3-Verstärkung (0–100 %)** hebst du die Lautstärke an |
| **Google** | Googles Server | Google Cloud Text-to-Speech. Braucht einen **Google-TTS-Schlüssel** (mit demselben Kästchen Gesperrt). Mit dem Regler **Google-WaveNet-Tonhöhe** für die WaveNet-Stimmen |
| **Microsoft Edge** | Microsofts Server | Microsofts Online-Vorlesestimmen. Kein Schlüssel nötig |

**Kokoro und Kyrillisch.** Ist die App-Sprache Russisch oder Ukrainisch — oder schreibt dein
*Spielclient* den Funkverkehr auf Russisch —, ist das Kokoro-Segment ausgegraut, ein Banner
erklärt den Grund, und Supertonic 3 spricht stattdessen.

> Ein Engine-Wechsel setzt die Stimme jedes Schiffs auf die Standardstimme der neuen Engine
> zurück. Die Persönlichkeiten der Schiffe bleiben erhalten. Du wirst vorher um Bestätigung
> gebeten.

### Fußzeile

**Standardwerte wiederherstellen** setzt das Sprachmodell auf lokales LM Studio mit
Standardadresse und -modell zurück und speichert sofort. **Speichern** übernimmt alles andere;
es ist ausgegraut, bis sich tatsächlich etwas ändert, und daneben erscheint dann der Hinweis
**Nicht gespeicherte Änderungen**.

Beim Speichern startet nur neu, was nötig ist — ein neues Modell oder ein neuer Schlüssel
startet das Gehirn neu, eine neue Sprachausgabe oder ihr Schlüssel die Stimme. Tonhöhe und
Verstärkung wirken ohne Neustart.

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
| **TTS-Sprechgeschwindigkeit** | Wie schnell Vega spricht |
| **Signaltonlautstärke** | Der Bestätigungston — er ertönt, wenn die Spracherkennung fertig ist und das Sprachmodell deine Eingabe hat |
| **STT-Threads** | CPU-Threads für die Spracherkennung (4–11). Eine Mindestanforderung, keine Reservierung: die App fordert so viele an, nutzt, was der Prozessor hergibt, und gibt sie nach getaner Arbeit frei |

Die Musiklautstärke ist nicht hier — sie liegt im [Reiter Jukebox](UI-Jukebox-Tab), damit du nie
die falsche herunterdrehst.

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
| **Taste** | Welche Taste darauf |
| **Maustaste** | Ein zweiter Auslöser: *Mittlere Taste*, *Taste 4 (Zurück)* oder *Taste 5 (Vorwärts)*. Praktisch zu Fuß oder im SRV, wenn dein HOTAS außer Reichweite ist. Links und rechts werden nicht angeboten — damit feuerst du deine Waffen |

Taste halten, sprechen, loslassen. Das Drücken **unterbricht Vega auch mitten im Satz**, du musst
also nie warten, bis sie fertig ist.

Solange Push-to-Talk aktiv ist, ist der Knopf **SCHLAFEN / AUFWACHEN** im Vega-Reiter gesperrt —
die Taste ist das Tor. Eine Änderung hier wirkt beim nächsten Druck, und die Taste funktioniert,
ob du diesen Reiter je öffnest oder nicht.

---

## Wo die Einstellungen liegen

Alle Einstellungen und Daten werden auf deinem PC gespeichert:

- **Linux:** `~/.local/share/elite-intel/` (oder `$XDG_DATA_HOME/elite-intel/`)
- **Windows:** `%LOCALAPPDATA%\elite-intel\`

Die Datenbank liegt in `db`, eigene Befehle in `custom-commands` (mit eigenem `backups`), deine
manuellen Belegungs-Schnappschüsse in `playerbackups` und die automatischen Kopien vor dem
Anwenden in `bindings/backups`.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
