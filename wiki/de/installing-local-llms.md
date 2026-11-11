# LLM auswählen

Elite Intel braucht ein Sprachmodell (LLM), um deine Befehle zu verstehen und deine Fragen zu
beantworten. Es gibt zwei Wege dorthin, und beide richtest du unter **Dienste → KI-Dienste →
Sprachmodell (LLM)** ein:

| Option | Was du brauchst | Kosten |
|--------|-----------------|--------|
| **Lokal: LM Studio** | Eine leistungsfähige GPU, [LM Studio](https://lmstudio.ai) und das Modell `google/gemma-4-e4b` | Kostenlos. Alles bleibt auf deinem PC |
| **Cloud** | Ein Konto und einen API-Schlüssel bei einem unterstützten Anbieter | Der Anbieter berechnet dir deine Nutzung |

![LLM-Einrichtung](images/llm-setup.png)

---

## Option 1: Lokal (LM Studio)

Die linke Seite des Bereichs, **LMStudio**:

| Feld | Wert |
|------|------|
| **Adresse** | `http://localhost:1234/v1/chat/completions`. Läuft LM Studio auf einem anderen PC in deinem Netzwerk, ersetze `localhost` durch dessen IP-Adresse |
| **Modell** | `google/gemma-4-e4b` |

LM Studio ist der einzige lokale Inferenzserver, den Elite Intel unterstützt, und
`google/gemma-4-e4b` das einzige unterstützte lokale Modell. Es gibt zwei Wege, es zu betreiben.

### Auf deinem Spiele-PC: Elite Intel richtet es ein

Kann dein PC Spiel und Modell zusammen betreiben, erscheint unter den Feldern von **LMStudio** eine
Schaltfläche. Drücke **LM Studio installieren**, und Elite Intel:

1. installiert LM Studio, auf dem Laufwerk deiner Wahl,
2. lädt `google/gemma-4-e4b` herunter (etwa 6 GB),
3. lädt es auf deine Grafikkarte und prüft, ob es wirklich dort gelandet ist,
4. testet es und trägt **Adresse** und **Modell** für dich ein.

Jeder Schritt wird angezeigt, während er läuft, mit einem Fortschrittsbalken für den Download. Schlägt ein
Schritt fehl, sagt der Dialog, welcher und warum, und ein erneuter Klick macht dort weiter. Rechne mit etwa
10 GB Speicherplatz. LM Studio ist kostenlose Software von Element Labs und nicht Teil von Elite Intel.

Danach startet Elite Intel LM Studio, wenn du seine Dienste startest, und beendet es, wenn du sie stoppst
oder die App schließt. Das Modell belegt den Speicher deiner Grafikkarte also nur, solange Elite Intel läuft.
Zum Entfernen drücke **LM Studio deinstallieren**: Es zeigt, was gelöscht wird, bevor es etwas löscht.

**Die Schaltfläche erscheint nur, wenn deine Grafikkarten Spiel und Modell zusammen halten können.** Das
Spiel braucht 8,5 GB Grafikspeicher oder mehr, das Modell etwa 6 GB, und Browser, OBS und Windows brauchen
auch etwas, deshalb fasst eine kleinere Karte nicht beides zuverlässig. Die Schaltfläche erscheint bei:

- **einer NVIDIA-Karte mit 24 GB**, RTX-30-Serie oder neuer: einer RTX 3090 oder besser.
- **zwei oder mehr NVIDIA-Karten mit zusammen etwa 20 GB**, jede RTX-30-Serie oder neuer mit mindestens
  8 GB — zum Beispiel eine 12-GB-RTX-5070 mit einer 8-GB-RTX-3070-Ti.
- **AMD-Karten mit demselben Speicher, als Beta.** Der Installationsdialog weist dich darauf hin. Klappt es
  bei dir nicht, richtet die [AMD-Anleitung](AMD-RX-7800XT-LLM-Setup) es von Hand ein.

NVIDIA- und AMD-Karten werden nie zusammengezählt.

Du hast LM Studio schon? Dann heißt die Schaltfläche **LM Studio einrichten**. Sie fügt das Modell hinzu und
füllt die Felder aus, und sonst bleibt dein LM Studio genau, wie es ist: Elite Intel startet, stoppt oder
entfernt nie ein LM Studio, das du selbst installiert hast.

### Auf einem zweiten PC: selbst einrichten

Kann dein Spiele-PC Spiel und Modell nicht zusammen halten, lass LM Studio auf einem anderen PC in deinem
Netzwerk laufen, der nichts als das Modell betreibt, und trage dessen Adresse unter **Adresse** ein. Der
Spiele-PC leistet dann keine Inferenzarbeit.

Der LLM-PC braucht:

- **12 GB Grafikspeicher**, das lässt Spielraum für das Modell,
- **Rechenleistung der Klasse RTX 3080 oder besser.** Eine langsamere Karte fasst das Modell, antwortet aber zu
  langsam, um nützlich zu sein.

Eine GPU-Referenztabelle von **Kevin Rank** findest du hier:
[GPU-Referenzleitfaden](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

| Anleitung | |
|-----------|---|
| [LM Studio – Linux](Install-LM-Studio-Linux) | Installation über die Kommandozeile und Betrieb als Dienst ab dem Systemstart |
| [LM Studio – Windows](Install-LM-Studio-Windows) | Installation über die Kommandozeile und Start bei der Anmeldung |
| [LM Studio auf Ubuntu Server](LM-Studio-Ubuntu-Server) | Headless-Installation auf einem eigenen Inferenzrechner |
| [LLMs auf der AMD-RX-Serie](AMD-RX-7800XT-LLM-Setup) | AMD-spezifische Einrichtung |

### Warum gerade `google/gemma-4-e4b`?

Elite Intel ist ein Befehlsparser und ein Werkzeug zur Datenanalyse, kein Chatbot für Plaudereien.
Natürlich klingendes Geplänkel reicht nicht. Das Modell muss aus deinen Worten die richtige Aktion
ableiten, Spieldaten analysieren und strukturierte Ergebnisse liefern statt eines Aufsatzes oder
HTML. Nicht jedes Modell dieser Größe schafft das zuverlässig.

Die harte Anforderung ist **Function Calling**. Der Companion von Elite Intel bittet das Modell
nicht, zu beschreiben, was es tun würde. Er gibt ihm eine Reihe von Werkzeugen und erwartet, dass
es eines davon mit Argumenten aufruft. Ein Modell, das keinen sauberen Werkzeugaufruf erzeugen
kann, kann die App überhaupt nicht steuern, egal wie gut es schreibt. `google/gemma-4-e4b`
unterstützt Function Calling.

Mit rund 6,3 GB passt es auf einer 24-GB-Karte mit reichlich Reserve neben dem Spiel in den VRAM.
Nichts wird auf die CPU ausgelagert, also bleiben die Antworten schnell.

> **Zum ausgemusterten V1.0-Modell.** Frühere Versionen empfahlen `tulu-3.1-8b-supernova`. Es
> unterstützt kein Function Calling, kann den Companion also nicht betreiben und funktioniert nicht
> mehr mit Elite Intel. Folgst du einer älteren Anleitung, ignoriere sie und installiere
> `google/gemma-4-e4b`.

---

## Option 2: Cloud

Die rechte Seite des Bereichs, **Cloud-Einrichtung**:

1. Wähle deinen Anbieter in der Liste **Anbieter**.
2. Füge den Schlüssel dieses Anbieters in **API-Schlüssel** ein.
3. Setze den Haken bei **Gesperrt**, damit der gespeicherte Schlüssel nicht versehentlich geändert wird.
4. Drücke **Speichern** und starte dann die Dienste im Reiter Vega (neu).

Den Anbieter wählst du selbst. Elite Intel versucht nicht, ihn am Schlüssel zu erkennen, denn die
Anbieter ändern ihre Schlüsselformate zu oft, als dass das zuverlässig wäre. Ein Modell wählst du
nicht: Elite Intel nimmt das passende für deinen Anbieter.

| Anbieter | Hinweise |
|----------|----------|
| **Anthropic (Claude)** | Kostenpflichtig |
| **OpenAI** | Kostenpflichtig |
| **Google (Gemini)** | Kostenpflichtig |
| **xAI (Grok)** | Kostenpflichtig |
| **DeepSeek** | Kostenpflichtig |

Wie du bei jedem Anbieter einen Schlüssel bekommst: [Cloud-LLM einrichten](cloud-llm-options).

---

## Was soll ich nehmen?

**Nimm LM Studio lokal, wenn:**
- dein Spiele-PC 24 GB Grafikspeicher auf einer Karte hat (eine RTX 3090 oder besser) oder etwa 20 GB
  auf zwei oder mehr: Elite Intel richtet es für dich ein
- oder du einen zweiten PC im Netzwerk mit 12 GB Grafikspeicher und Leistung der Klasse RTX 3080 hast
- alles auf deiner eigenen Hardware bleiben soll

**Nimm einen Cloud-Anbieter, wenn:**
- deine GPU das Modell nicht neben dem Spiel betreiben kann und du keinen zweiten PC dafür hast
- du überhaupt keinen Inferenzserver betreiben willst

Unsicher? Du kannst mit beidem anfangen und jederzeit ohne Neuinstallation wechseln.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
