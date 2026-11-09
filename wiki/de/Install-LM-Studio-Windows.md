## Lokales LLM – Windows-Setup (LM Studio)

Ein lokales LLM zu betreiben hält alle Daten privat und offline. Es gibt keine Abonnementgebühren. Hardware- und Stromkosten fallen an.

Es erfordert [LM Studio](https://lmstudio.ai) und eine leistungsfähige GPU.

---

### Mindest-Hardware

Um Elite Dangerous und das LLM auf **demselben Rechner** zu betreiben, ist mindestens eine **NVIDIA RTX 3060 mit 12 GB VRAM** erforderlich.

> **Tipp:** Elite Intel kann auf eine LM Studio-Instanz verweisen, die auf einem **separaten PC** in deinem Netzwerk läuft. Wenn ein zweiter Rechner mit einer leistungsfähigen GPU verfügbar ist, trägt der Spiele-PC in dieser Konfiguration keine Inferenzlast.

---

### Empfohlenes Modell

| Modell | Benötigter VRAM | Hinweise |
|---|---|---|
| `google/gemma-4-e4b` | ~6,3 GB | ✅ Erforderlich |

> **Warum dieses Modell?** Es unterstützt das Function Calling, das der Begleiter benötigt. Ein Modell, das keinen
> Tool-Aufruf erzeugen kann, kann die App nicht steuern – ganz gleich, wie gut es formuliert.
> Siehe [LLM auswählen](installing-local-llms).

---

Detailliertes (sehr detailliertes) Video-Tutorial von @DawnTreaderToolsoftheElite

> **Hinweis:** Das Video zeigt ältere Versionen von LM Studio und Elite Intel. Wo es von den Schritten unten abweicht, folge den Schritten unten.

[[youtube:F5RgRRePrTo]]

---

### Schritt 1 – LM Studio installieren

Öffne **PowerShell** und führe aus:

```powershell
irm https://lmstudio.ai/install.ps1 | iex
```

Damit werden das `lms`-CLI und **llmster**, der Hintergrundserver von LM Studio, installiert. Die LM Studio-Desktop-App wird dabei **nicht** installiert. Öffne nach der Installation ein **neues** PowerShell-Fenster, damit die Änderungen wirksam werden.

Prüfen, ob es funktioniert:

```powershell
lms --help
```

> **Hinweis:** Wenn die LM Studio-Desktop-App bereits installiert ist, ist das `lms`-CLI möglicherweise bereits verfügbar. Führe `lms --help` aus, bevor du das Installationsskript ausführst.

---

### Schritt 2 – Das Modell herunterladen

Starte den Hintergrundserver von LM Studio und lade dann das Modell herunter:

```powershell
lms daemon up
lms get google/gemma-4-e4b
```

Wenn der Name nicht exakt aufgelöst wird, listet `lms get` die gefundenen Kandidaten auf;
wähle einen mit den Pfeiltasten und Enter.

Um heruntergeladene Modelle aufzulisten:

```powershell
lms ls
```

[LM Studio hat einen bekannten Fehler](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). In manchen Fällen schlägt der Download fehl mit:
```Error: No staff picks found with the specified search criteria.```

Lade in diesem Fall die GGUF-Datei von der Seite des Modells auf HuggingFace von Hand herunter und importiere sie:

```powershell
lms import C:\pfad\zu\dem-modell.gguf
```

---

### Schritt 3 – Den Server starten

Das Modell laden und den Inferenzserver starten:

```powershell
lms daemon up
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start
```

`lms daemon up` tut nichts, wenn der Hintergrundserver bereits läuft. Du kannst es also jedes Mal mit ausführen.

**HINWEIS**: Der Flag `--context-length 8192` ist erforderlich. Ohne ihn ist das Kontextfenster möglicherweise zu klein, was zu Prompt-Abschneidungen, Fehlern und Halluzinationen führt.

Überprüfe, ob er läuft, indem du einen Browser oder ein anderes PowerShell-Fenster öffnest und zu folgendem navigierst:

```
http://localhost:1234/v1/models
```

Du solltest eine JSON-Liste der geladenen Modelle erhalten. Die Modell-ID-Zeichenfolge in dieser Antwort gibst du in das Feld **Modell** von Elite Intel ein.

Den Server stoppen:

```powershell
lms server stop
```

> ⚠️ **Wichtig:** Der LM Studio-Server **überlebt keine Neustarts**. Führe die drei Befehle aus Schritt 3 nach jedem Neustart erneut aus oder verwende eine der Autostart-Optionen unten.

---

### Schritt 4 – (Optional) Autostart beim Booten

Zwei Optionen stehen zur Verfügung, um den Server über Neustarts hinweg am Laufen zu halten. Wenn du Schritt 1 gefolgt bist, verwende **Option A**. Option B gilt nur, wenn zusätzlich die LM Studio-Desktop-App installiert ist.

#### Option A – Aufgabenplanung (ohne Desktop-App)

Speichere zuerst dieses Skript als `start-lmstudio.ps1` an einem dauerhaften Speicherort (z. B. `C:\Scripts\`):

```powershell
$lms = "$env:USERPROFILE\.lmstudio\bin\lms.exe"
& $lms daemon up
& $lms load google/gemma-4-e4b --yes --context-length 8192 --gpu max
& $lms server start
```

Es startet den Hintergrundserver, lädt das Modell mit den Einstellungen, die Elite Intel braucht, und startet den Inferenzserver: dieselben drei Befehle wie in Schritt 3.

Dann die Aufgabe erstellen:

1. **Win + R** drücken, `taskschd.msc` eingeben, Enter drücken.
2. Im rechten Panel auf **Aufgabe erstellen** klicken.
3. **Allgemein-Tab**: Als `LM Studio Server` benennen. **„Mit höchsten Privilegien ausführen"** nicht ankreuzen: LM Studio braucht keine Administratorrechte.
4. **Trigger-Tab**: Neu klicken → **„Bei Anmeldung"** → OK.
5. **Aktionen-Tab**: Neu klicken → **„Programm starten"**.
   - Programm/Skript: `powershell.exe`
   - Argumente hinzufügen: `-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "C:\Scripts\start-lmstudio.ps1"`

`-WindowStyle Hidden` verhindert, dass bei jeder Anmeldung ein Konsolenfenster offen bleibt. Beim Start des Skripts kann kurz ein Fenster aufblitzen.

#### Option B – Desktop-App

Diese Option gibt es nur, wenn die LM Studio-Desktop-App installiert ist. Schritt 1 installiert sie nicht.

1. LM Studio öffnen und **Ctrl + ,** drücken, um die Einstellungen zu öffnen.
2. „**LLM-Server beim Login starten**" ankreuzen.
3. Das Schließen der App minimiert sie in die Taskleiste und hält den Server am Laufen. Beim nächsten Login wird sie automatisch wiederhergestellt.

> ⚠️ **Kontextlänge:** Bei dieser Option lädt LM Studio das Modell erst bei der ersten Anfrage, und zwar mit den **Standardeinstellungen** des Modells statt mit `--context-length 8192` aus Schritt 3. Öffne in der Desktop-App die Standardeinstellungen des Modells und setze die **Kontextlänge** auf **8192**. Sonst ist das Kontextfenster möglicherweise zu klein.

---

### Schritt 5 – Elite Intel konfigurieren

Öffne in Elite Intel **Dienste → KI-Dienste**. Wähle unter **Sprachmodell (LLM)** die Option **LMSTudio**:

- **Adresse**: `http://localhost:1234/v1/chat/completions` (der Standardwert). Wenn LM Studio auf einem anderen Rechner läuft, `localhost` durch die IP dieses Rechners ersetzen.
- **Modell**: die Modell-ID-Zeichenfolge aus `http://localhost:1234/v1/models` einfügen. Ein Modell bedient sowohl Befehle als auch Abfragen.

Für ein lokales LLM wird kein API-Schlüssel benötigt.

Klicke auf **Speichern**. Elite Intel startet das Sprachmodell daraufhin selbst neu.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
