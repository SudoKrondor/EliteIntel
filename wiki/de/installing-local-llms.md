# Einen lokalen Inferenzserver wählen

Um ein lokales LLM mit Elite Intel zu betreiben, ist ein **Inferenzserver** erforderlich. Das ist eine Software, die das KI-Modell lädt und es über eine lokale API bereitstellt. Es ist das lokale Äquivalent eines Cloud-KI-Dienstes und läuft vollständig auf deiner eigenen Hardware.

Elite Intel nutzt **LM Studio** als Inferenzserver. Er läuft unter Windows und Linux und stellt eine OpenAI-kompatible API bereit.

![loca llm ui](images/local-llm.png)

## GPU-Anforderungen
Hardwareanforderungen, um Spiel und LLM auf demselben Rechner zu betreiben:

- RTX 3090 24 GB VRAM
- AMD RX 7800 XT

Wenn du nicht genug Hardware hast, nutze den __kostenlosen Cloud-Dienst__ unter
👉 **[console.mistral.ai](https://console.mistral.ai/)** 👈 — kostenloser Tarif, ohne Kreditkarte.
Einrichtungsschritte: [Kostenloses Cloud-LLM](cloud-llm-options).

Eine GPU-Referenztabelle von **Kevin Rank** ist hier verfügbar:
[GPU-Referenzleitfaden](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

---
### Installationsanleitungen

| Inferenzserver                                        |                                                                           |
|-------------------------------------------------------|---------------------------------------------------------------------------|
| [✅ LM Studio - Linux](Install-LM-Studio-Linux)       | Schnell, mehr Modellflexibilität – Anleitung zeigt die Server-Einrichtung |
| [✅ LM Studio - Windows](Install-LM-Studio-Windows)   | Schnell, mehr Modellflexibilität – mit GUI                                |
| [🆓 Kostenloses Cloud-LLM](cloud-llm-options)         | Keine GPU nötig – kostenloser Mistral-Tarif, ohne Kreditkarte             |

---

### LM Studio auf einen Blick

|                               | LM Studio                                              |
|-------------------------------|--------------------------------------------------------|
| **Benötigtes Modell**         | `google/gemma-4-e4b`                                   |
| **Installation**              | Ein Skript, fertig                                     |
| **Läuft als**                 | Manueller Start, oder optionaler Autostart             |
| **Modell-Tuning**             | Flags beim Laden                                       |
| **Windows-Autostart**         | Erfordert Desktop-App oder Aufgabenplanung             |
| **Linux-Autostart**           | Manuelle systemd-Einrichtung (siehe Linux-Anleitung)   |
| **Modellquelle**              | HuggingFace (GGUF)                                     |
| **API-Port**                  | `1234`                                                 |
| **GUI**                       | Optionale Desktop-App                                  |

---

### Auswahlhilfe

**LM Studio lokal betreiben, wenn:**
- Du eine NVIDIA RTX 3090 24 GB oder besser hast. VRAM ist der entscheidende Faktor, nicht die GPU-Geschwindigkeit. Eine GPU mit nur 12 GB VRAM ist unzureichend, unabhängig von der Generation.
- Du Elite Dangerous und das LLM auf demselben Rechner betreibst
- Du Elite Intel auf einen separaten PC in deinem Netzwerk verweisen möchtest
- Du eine Desktop-GUI zum Durchsuchen, Herunterladen und Verwalten von Modellen möchtest oder einen sauberen Headless-Server auf einem dedizierten Inferenzrechner

**Stattdessen das [kostenlose Cloud-LLM](cloud-llm-options) nutzen, wenn:**
- Deine GPU nicht genug VRAM hat, um ein Modell neben dem Spiel zu betreiben
- Du keinen lokalen Inferenzserver betreiben möchtest

---
## Empfehlung des Entwicklers

Der Entwickler verwendet LM Studio mit `google/gemma-4-e4b` (~6,3 GB). Andere Modelle können
funktionieren, sind aber nicht garantiert. Melde Kompatibilitätsergebnisse auf Matrix.

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
