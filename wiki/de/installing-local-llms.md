# Einen lokalen Inferenzserver wählen

Um ein lokales LLM mit Elite Intel zu betreiben, ist ein **Inferenzserver** erforderlich. Das ist eine Software, die das KI-Modell lädt und es über eine lokale API bereitstellt. Es ist das lokale Äquivalent eines Cloud-KI-Dienstes und läuft vollständig auf deiner eigenen Hardware.

Elite Intel unterstützt zwei Inferenzserver: **Ollama** und **LM Studio**. Beide sind kompatibel und verwenden dieselben Modelle. Die Auswahl kann jederzeit in den Einstellungen geändert werden.

![loca llm ui](images/local-llm.png)

## GPU-Anforderungen
Hardwareanforderungen, um Spiel und LLM auf demselben Rechner zu betreiben:

- RTX 3090 24 GB VRAM
- AMD RX 7800 XT

Wenn du nicht genug Hardware hast, nutze den __[kostenlosen Cloud-Dienst](https://v2.auth.mistral.ai/login)__

Eine GPU-Referenztabelle von **Kevin Rank** ist hier verfügbar:
[GPU-Referenzleitfaden](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

---
### Installationsanleitungen

| Inferenzserver                                        |                                                                                            |
|-------------------------------------------------------|--------------------------------------------------------------------------------------------|
| [✅ LM Studio - Linux](Install-LM-Studio-Linux)       | Schnell, mehr Modellflexibilität – Anleitung zeigt die Server-Einrichtung                  |
| [✅ LM Studio - Windows](Install-LM-Studio-Windows)   | Schnell, mehr Modellflexibilität – mit GUI                                                 |
| [Ollama - Linux](Install-Ollama-Local-LLM-Linux)     | Empfohlen, wenn du die nötige Hardware hast                                                |
| [Ollama - Windows](Install-Ollama-Local-LLM-Windows) | Empfohlen, wenn du die nötige Hardware hast                                                |

---

### Ollama vs. LM Studio auf einen Blick

|                               | Ollama                                      | LM Studio                                                                                                    |
|-------------------------------|---------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **Geschwindigkeit**           | Langsamer                                   | Schneller                                                                                                    |
| **Erforderliches Modell**     | `google/gemma-4-e4b`                        | `google/gemma-4-e4b`                                                                                         |
| **Am besten geeignet für**   | Einfache Einrichtung, minimaler Wartungsaufwand | Mehr Kontrolle über das Laden von Modellen                                                               |
| **Installation**              | Ein Skript, fertig                          | Ein Skript, fertig                                                                                           |
| **Läuft als**                 | Systemdienst (startet automatisch beim Boot) | Manueller Start oder optionaler Autostart                                                                   |
| **Modell-Tuning**             | Modelfile im Modell integriert              | Parameter beim Laden                                                                                         |
| **Windows-Autostart**         | ✅ Funktioniert direkt                       | Erfordert Desktop-App oder Aufgabenplanung                                                                   |
| **Linux-Autostart**           | ✅ systemd-Dienst inklusive                 | Manuelle systemd-Einrichtung                                                                                 |
| **Modellquelle**              | Ollama-Bibliothek                           | HuggingFace (GGUF)                                                                                           |
| **API-Port**                  | `11434`                                     | `1234`                                                                                                       |
| **GUI**                       | Keine (nur CLI)                             | Optionale Desktop-App                                                                                        |

---

### Auswahlhilfe

**Ollama verwenden, wenn:**
- Du eine einfache Installation mit minimalem laufenden Konfigurationsaufwand möchtest
- Du unter Windows bist und den Startup nicht manuell konfigurieren möchtest
- Du neu bei lokalen LLMs bist

**LM Studio verwenden, wenn:**
- Du eine Desktop-GUI zum Durchsuchen, Herunterladen und Verwalten von Modellen möchtest
- Du bereits mit HuggingFace und GGUF-Modelldateien vertraut bist
- Du mit verschiedenen Modellen experimentieren möchtest, ohne Modelfiles zu schreiben
- Du einen dedizierten Inferenzrechner betreibst und einen sauberen Headless-Server benötigst

**Beide Optionen funktionieren, wenn:**
- Du eine NVIDIA RTX 3090 24 GB oder besser hast. VRAM ist der entscheidende Faktor, nicht die GPU-Geschwindigkeit. Eine GPU mit nur 12 GB VRAM ist unzureichend, unabhängig von der Generation.
- Du Elite Dangerous und das LLM auf demselben Rechner betreibst
- Du Elite Intel auf einen separaten PC in deinem Netzwerk verweisen möchtest

---
## Empfehlung des Entwicklers

Der Entwickler verwendet LM Studio mit `google/gemma-4-e4b` (~6,3 GB). Dasselbe Modell unter
Ollama läuft merklich langsamer. Andere Modelle können funktionieren, sind aber nicht garantiert.
Melde Kompatibilitätsergebnisse auf Matrix.

## Warum genau `google/gemma-4-e4b`?

Elite Intel ist ein Befehls-Parser und ein Datenanalyse-Tool, kein konversationeller Chatbot. Das
stellt spezifische Anforderungen an das Modell. Natürlich klingende Unterhaltung zu erzeugen reicht
nicht aus. Das Modell muss Aktionen aus Spracheingabe korrekt ableiten, strukturierte Datenanalyse
durchführen und Ergebnisse als strukturierte Daten zurückgeben – nicht als Markdown-Essay oder
HTML. Nicht alle Modelle dieser Größe erfüllen das zuverlässig.

Die harte Anforderung ist **Function Calling**. Der Begleiter von Elite Intel bittet das Modell
nicht, zu beschreiben, was es tun würde – er bietet ihm eine Reihe von Werkzeugen an und erwartet,
dass es eines davon mit Argumenten aufruft. Ein Modell, das keinen wohlgeformten Tool-Aufruf
erzeugen kann, kann die App überhaupt nicht steuern, ganz gleich, wie gut es formuliert.
`google/gemma-4-e4b` unterstützt das.

Mit rund 6,3 GB passt es auf einer 24-GB-Karte neben dem Spiel in den VRAM, mit etwas Reserve. Das
vermeidet CPU-Offload und hält den Inferenzdurchsatz hoch.

> **Zum eingestellten V1.0-Modell.** Frühere Versionen empfahlen `tulu-3.1-8b-supernova`. Es
> unterstützt kein Function Calling, kann den Begleiter also nicht ausführen und ist mit Elite
> Intel nicht mehr verwendbar. Wenn du einer älteren Anleitung folgst, ignoriere sie und
> installiere `google/gemma-4-e4b`.

## Kann ich ein anderes Modell verwenden?

Alternative Modelle können verwendet werden, müssen aber Function Calling unterstützen. Ohne das
kann die App nichts ausführen.

Der häufigste Fehlschlag mit einem alternativen Modell ist ein falsches Antwortformat – das Modell
gibt Prosa zurück, die eine Aktion beschreibt, statt das Werkzeug tatsächlich aufzurufen.

--- 

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
