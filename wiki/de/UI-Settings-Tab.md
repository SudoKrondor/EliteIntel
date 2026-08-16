# Reiter „Einstellungen"

<img src="images/settings.png" class="inline" height="20" alt="Einstellungen"> Der Unterbau. Eine
Leiste **Allgemein**, die überall gilt, und darunter drei Unterreiter: **KI-Dienste**, **Audio** und
**Push To Talk**.

---

## Allgemein

Über den Unterreitern angezeigt, weil sie für alle gilt.

**Sprache** — die Sprache sowohl deiner Sprachbefehle als auch der Oberfläche der App. Eine Auswahl
rendert das gesamte Fenster sofort neu, und Vega sagt die Änderung laut an.

Unterstützt: Englisch, Spanisch, Französisch, Deutsch, Italienisch, Portugiesisch, brasilianisches
Portugiesisch, Ukrainisch, Russisch.

**Journal-Ordner** — wo Elite Dangerous seine Journaldateien schreibt. Optional: Lass ihn leer, dann
wird der Standardort deiner Plattform verwendet. Darüber weiß Elite Intel, was rund um dein Schiff
geschieht — ist er falsch, ist die App faktisch blind, und sie sagt das beim Start.

---

## KI-Dienste

![KI-Dienste](images/ui-tab-settings-ai.png)

**In V1.1 neu geschrieben.** Die alten verstreuten „Verwenden"-Kästchen sind verschwunden. Es gibt
jetzt zwei Umschalter — einen für das Sprachmodell, einen für die Sprachausgabe — und die jeweils
ungenutzte Seite ist abgedunkelt, sodass offensichtlich ist, welche aktiv ist.

Das ist außerdem der einzige Reiter der App, der mit einem **Entwurf** arbeitet. Nichts wird
geschrieben, bevor du **Speichern** drückst, und der Versuch, mit ungespeicherten Änderungen zu
gehen, fragt nach *Speichern*, *Verwerfen* oder *Weiter bearbeiten*.

### Sprachmodell (KI)

Wechsle zwischen **Lokale Einrichtung** und **Cloud-Einrichtung**.

**Lokale Einrichtung**

| Feld | Anmerkungen |
|-------|-------|
| **Adresse** | Vorbelegt mit der üblichen URL von LM Studio. Richte sie auf die IP eines anderen Rechners, wenn die Inferenz woanders in deinem LAN läuft |
| **Modell** | Der Modellname. **Ein Feld** — V1.1 nutzt ein einziges Modell für Befehle und Abfragen |

Das voreingestellte und empfohlene lokale Modell ist **`google/gemma-4-e4b`**. Elite Intel warnt
dich beim Start, wenn dein lokales Modell ein anderes ist; andere Modelle können schlecht oder gar
nicht funktionieren.

Einrichtungsanleitungen: [LM Studio unter Linux](Install-LM-Studio-Linux) ·
[LM Studio unter Windows](Install-LM-Studio-Windows) ·
[AMD RX Series](AMD-RX-7800XT-LLM-Setup)

**Cloud-Einrichtung**

Ein Feld: dein **API-Schlüssel**, daneben ein Kästchen **Gesperrt**, damit ein gespeicherter
Schlüssel nicht versehentlich bearbeitet wird. Hake Gesperrt ab, um ihn zu ändern.

Unterstützte Anbieter: **Gemini, Grok, OpenAI, Claude, Deepseek, Mistral.**

> Du wählst kein Modell mehr aus. Elite Intel erkennt den Anbieter an der Form deines Schlüssels und
> wählt das passende Modell selbst.

Mistral hat ein kostenloses Kontingent und ist der einfachste Einstieg.
Wie du bei jedem Anbieter an einen Schlüssel kommst, steht unter
[Cloud-LLM-Optionen](cloud-llm-options).

### Sprachausgabe (TTS)

Wechsle zwischen **Lokal · Kokoro** und **Cloud · Google**.

- **Lokal · Kokoro** hat überhaupt keine Konfiguration. 53 Stimmen, eingebaut, kein Schlüssel, kein
  Download.
- **Cloud · Google** benötigt einen **Google-TTS-Schlüssel**, mit demselben Kästchen „Gesperrt".

> Ein Wechsel der Engine setzt die Stimme jedes Schiffs auf die Standardstimme der neuen Engine
> zurück. Die Persönlichkeiten der Schiffe bleiben erhalten. Du wirst vorher um Bestätigung gebeten.

### Fußzeile

**Standardwerte wiederherstellen** setzt die Konfiguration des Sprachmodells auf lokales LM Studio
mit dem Standardmodell zurück und speichert sofort. **Speichern** übernimmt alles Übrige; es ist
ausgegraut, bis sich tatsächlich etwas ändert, und dann erscheint daneben der Hinweis **Nicht
gespeicherte Änderungen**.

Das Speichern startet nur neu, was nötig ist — eine Modelländerung startet das Gehirn neu, eine
Änderung des Sprachschlüssels den Mund.

---

## Audio

![Audio-Einstellungen](images/ui-tab-settings-audio.png)

### Audiogeräte

Auswahllisten **Mikro** und **Lautspr.**, oder *(Systemstandard)*. Dieselben Auswahllisten erreichst
du über die Schaltfläche **Audiogeräte** im Vega-Reiter.

> Geräteänderungen greifen beim **nächsten Dienststart**.

**Geräuschunterdrückung aktivieren** mit den Stärken **Niedrig / Mittel / Hoch**. Beginne bei
Mittel. Hoch ist für wirklich laute Räume gedacht — es arbeitet aggressiv, und Überfilterung kann
dich Transkriptionsgenauigkeit kosten.

### Audiopegel

| Regler | Funktion |
|--------|--------------|
| **Sprachlautstärke** | Wie laut Vega spricht |
| **TTS-Sprechgeschwindigkeit** | Wie schnell Vega spricht |
| **Signaltonlautstärke** | Der Bestätigungston — er ertönt, wenn die Spracherkennung fertig ist und das Sprachmodell deine Eingabe hat |
| **STT-Threads** | CPU-Threads für die Transkription (4–11). Eine Mindestanforderung, keine Reservierung: Die App fragt diese Anzahl an, nutzt, was der Prozessor hergibt, und gibt sie nach getaner Arbeit wieder frei |

### Mikrofonmonitor

Eine Live-Anzeige an der rechten Seite. Drei Dinge sind daran abzulesen:

- **FLOOR** — dein Geräuschpegel, wenn du *nicht* sprichst.
- **GATE** — die Schwelle. Audio oberhalb des Gates wird zur Transkription gestreamt; fällt es
  darunter, wird das Aufgenommene transkribiert und an das Sprachmodell geschickt.
- **CLIP** — du übersteuerst das Mikrofon. Alles oberhalb dieser Linie transkribiert schlecht.

Du willst einen deutlichen Abstand zwischen FLOOR und deinem Sprechpegel, und nichts, was CLIP
berührt. Sieht es anders aus, führe im Vega-Reiter **AUDIO KALIBRIEREN** aus — es setzt das Gate für
dich und warnt dich, wenn der Abstand zwischen Sprache und Rauschen zu klein zum Arbeiten ist.

---

## Push To Talk

![Push to Talk](images/ui-tab-settings-push-to-talk.png)

Push-to-Talk arbeitet mit einer **Taste am Gamecontroller oder HOTAS**, nicht mit der Tastatur. Du
gibst eine Taste her und bekommst dafür ein Mikrofon, das geschlossen ist, außer wenn du es offen
haben willst.

| Bedienelement | Anmerkungen |
|---------|-------|
| **Push-to-Talk aktivieren** | Der Hauptschalter. Alles andere ist deaktiviert, bis er an ist |
| **Controller** | Jeder verbundene Controller, den Elite Intel sieht. Es wählt deinen gespeicherten Controller nach dem Wiederverbinden automatisch neu aus |
| **Taste** | Welche Taste darauf |

Zwei Modi:

- **Umschalten zum Schlafen / Aufwachen** — die Taste schaltet Vega zwischen Schlafen und Zuhören
  um. Im Schlaf ignoriert Vega alles außer `Wake up!`, und das Umgehungswort `listen` /
  `listen up` bringt weiterhin einen einzelnen Befehl durch: *„Listen up — lower the landing gear."*
- **Push To Talk** — Vega ignoriert standardmäßig alles. Taste halten, Signalton hören, sprechen,
  loslassen. Ein zweiter Signalton bestätigt, dass deine Eingabe verarbeitet wird.

Solange Push-to-Talk aktiv ist, ist die Schaltfläche **SCHLAFEN / AUFWACHEN** im Vega-Reiter
deaktiviert — die Controller-Taste ist das Tor.

Die Taste funktioniert, ob du diesen Reiter je öffnest oder nicht.

---

## Wo die Einstellungen liegen

Alle Einstellungen und Daten liegen in einer SQLite-Datenbank:

- **Linux:** `~/.local/share/elite-intel/elite-intel/db/`
- **Windows:** `%APPDATA%\elite-intel\db\`

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
