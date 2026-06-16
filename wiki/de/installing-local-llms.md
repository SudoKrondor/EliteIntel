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
| **Bevorzugtes Modell**        | [tulu-3.1-8b-supernova Q4_K_M](https://huggingface.co/mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF) | [tulu-3.1-8b-supernova Q4_K_M](https://huggingface.co/mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF) |
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

Der Entwickler verwendet LM Studio mit [`matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`](https://huggingface.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF). Dieses Modell bietet schnelle Inferenz. Dasselbe Modell unter Ollama läuft merklich langsamer. Die App ist für dieses Modell optimiert. Andere Modelle können funktionieren, sind aber nicht garantiert. Melde Kompatibilitätsergebnisse auf Matrix.

## Warum genau tulu3.1:8b Supernova?

Elite Intel ist ein Befehls-Parser und ein Datenanalyse-Tool, kein konversationeller Chatbot. Das stellt spezifische Anforderungen an das Modell. Natürlich klingende Unterhaltung zu erzeugen reicht nicht aus. Das Modell muss Aktionen aus Spracheingabe korrekt ableiten und strukturierte Datenanalyse durchführen. Es muss Ergebnisse in formatiertem JSON zurückgeben, nicht in einem Markdown-Essay oder HTML. Nicht alle Modelle dieser Größe erfüllen diese Aufgabe zuverlässig.

## Tulu 3 (das Basis-Trainingsrezept) ist wirklich außergewöhnlich

[Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF](https://huggingface.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF/tree/main)

Die meisten Instruction-Modelle werden mit RLHF trainiert, das ein gelerntes Belohnungsmodell zur Bewertung von Ausgaben verwendet. Dieses Belohnungsmodell ist selbst ein neuronales Netz und erbt daher alle üblichen Verzerrungen und Inkonsistenzen. Tulu 3 ersetzt dies durch RLVR (Reinforcement Learning with Verifiable Rewards). Anstelle eines gelernten Belohnungsmodells verwendet das Training eine deterministische Bewertungsfunktion: Die Antwort ist entweder korrekt oder nicht. Binär, ohne Verzerrung. Dies ist besonders wirkungsvoll bei Instruction-Following-Aufgaben, bei denen das Belohnungssignal objektiv ist.

Die Trainingspipeline verfolgt einen vierstufigen Ansatz: Datenkuration für Kernfähigkeiten, überwachtes Feintuning, Direct Preference Optimization und darüber hinaus RLVR zur Schärfung der verifizierbaren Aufgabenleistung. Jede Stufe baut auf der vorherigen auf. Deshalb erreicht Tulu 3 auf der 8B-Llama-Basis Ergebnisse, die die Instruct-Versionen von Llama 3.1, Qwen 2.5, Mistral und sogar geschlossene Modelle wie GPT-4o-mini und Claude 3.5 Haiku übertreffen.

Für EliteIntel ist die Befehlsklassifikationsstufe eine Instruction-Following-Aufgabe mit verifizierbaren korrekten Antworten (JSON-Aktion X vs. Y). Dies ist genau der Aufgabentyp, den RLVR optimiert. Das Modell ist speziell für deterministischen strukturierten Output trainiert.

## Warum die „Supernova"-Variante

Die Supernova-Variante unterscheidet sich vom Standard-Tulu 3. Tulu-3.1-8B-SuperNova entsteht durch eine lineare Zusammenführung von drei Modellen: Llama-3.1-MedIT-SUN-8B (Medizin/Reasoning), Llama-3.1-Tulu-3-8B (Instruction Following) und Llama-3.1-SuperNova-Lite (Arcees destilliertes Modell), jedes mit gleichem Gewicht von 1.0 über mergekit.

Das SuperNova-Lite-Elternmodell ist ein destilliertes Modell aus einer größeren Arcee-Basis, das eine Wissensdichte jenseits eines Standard-8B-Modells bietet. Die lineare Zusammenführung mittelt Gewichtstensoren direkt und kombiniert Wissen ohne zusätzlichen Trainingsaufwand. Dies erzielt besonders starke Ergebnisse bei Instruction-Following-Aufgaben, wie sein IFEval-Score belegt.

**Leistung**: Das Modell verwendet eine 8B-Llama-Architektur. Bei Q4_K_M-Quantisierung auf einer 3090 mit 24 GB passt es neben dem Spiel in den VRAM mit etwas Reserve. Dies vermeidet CPU-Offload und erhält maximalen Inferenzdurchsatz. Vergleichbare Qwen-Modelle verwenden andere Attention-Head-Konfigurationen (wie Qwen2.5's GQA-Verhältnis), die im GGUF-Backend von llama.cpp langsamer sein können.

Es läuft auch auf einer 12-GB-VRAM-Karte, wenn keine anderen VRAM-intensiven Prozesse aktiv sind. Dafür muss das Spiel auf einer separaten GPU oder einem anderen Rechner laufen.

## Kann ich ein anderes Modell verwenden?

Alternative Modelle können verwendet werden, werden aber wahrscheinlich nicht die Geschwindigkeit und Genauigkeit von tulu3.1-supernova erreichen.

Häufige Probleme mit alternativen Modellen sind ein falsches Antwortformat.
Der häufigste Fehler ist, dass das Modell einen Markdown-Essay statt einer strukturierten Aktion oder eines Analyseergebnisses zurückgibt.

--- 

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
