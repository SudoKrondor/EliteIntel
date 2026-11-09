## Lokales LLM – Windows-Setup (LM Studio)

Ein lokales LLM zu betreiben hält alle Daten privat und offline. Es gibt keine Abonnementgebühren. Hardware- und Stromkosten fallen an.

Es erfordert [LM Studio](https://lmstudio.ai) und eine leistungsfähige GPU.

---

### Für wen diese Anleitung ist

Diese Anleitung richtet LM Studio von Hand auf einem PC ein, der **nur das Modell betreibt**: einem zweiten PC in
deinem Netzwerk, während das Spiel auf deinem Spiele-PC läuft, der dann keinerlei Inferenzlast trägt.

> **Spiel und Modell auf einem PC?** Dann brauchst du diese Anleitung nicht. Hat dein Spiele-PC eine NVIDIA RTX
> 3090 (24 GB) oder besser, installiert und richtet Elite Intel LM Studio für dich ein: siehe
> [LLM auswählen](installing-local-llms).

### Hardware für den LLM-PC

- **12 GB Grafikspeicher.** Das Modell braucht etwa 6,3 GB, das lässt Spielraum.
- **Rechenleistung der Klasse RTX 3080 oder besser.** Eine langsamere Karte fasst das Modell, antwortet aber zu
  langsam, um nützlich zu sein.

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
lms server start --bind 0.0.0.0
```

`lms daemon up` tut nichts, wenn der Hintergrundserver bereits läuft. Du kannst es also jedes Mal mit ausführen.

`--bind 0.0.0.0` macht den Server für andere PCs in deinem Netzwerk erreichbar. Ohne diese Angabe antwortet er nur diesem PC.

**HINWEIS**: `--context-length 8192` ist erforderlich, und zwar genau 8192: Mit einem kleineren Fenster halluziniert das Modell, mit einem größeren wird jede Antwort zu langsam.

> **Lass den Spiele-PC herein.** Wenn der Server zum ersten Mal im Netzwerk lauscht, fragt die Windows Defender Firewall möglicherweise, ob er zugelassen werden soll. Erlaube es für **private** Netzwerke. Fragt sie nie und der Spiele-PC kommt nicht durch, lege eine eingehende Regel für TCP-Port 1234 an.

Prüfe es vom **Spiele-PC** aus: Öffne einen Browser mit dieser Adresse, mit der Adresse des LLM-PCs anstelle von `LLM_PC_IP`:

```
http://LLM_PC_IP:1234/v1/models
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
& $lms server start --bind 0.0.0.0
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
3. Schalte in den Server-Einstellungen **Serve on Local Network** ein, damit der Spiele-PC ihn erreicht.
4. Das Schließen der App minimiert sie in die Taskleiste und hält den Server am Laufen. Beim nächsten Login wird sie automatisch wiederhergestellt.

> ⚠️ **Kontextlänge:** Bei dieser Option lädt LM Studio das Modell erst bei der ersten Anfrage, und zwar mit den **Standardeinstellungen** des Modells statt mit `--context-length 8192` aus Schritt 3. Öffne in der Desktop-App die Standardeinstellungen des Modells und setze die **Kontextlänge** auf **8192**. Sonst ist das Kontextfenster möglicherweise zu klein.

---

### Schritt 5 – Elite Intel konfigurieren

**Am Spiele-PC:** Öffne in Elite Intel **Dienste → KI-Dienste**. Wähle unter **Sprachmodell (LLM)** die Option **LMStudio**:

- **Adresse**: `http://LLM_PC_IP:1234/v1/chat/completions`, wobei `LLM_PC_IP` die Adresse des LLM-PCs in deinem Netzwerk ist (`ipconfig` auf dem LLM-PC zeigt sie).
- **Modell**: die Modell-ID-Zeichenfolge aus `http://LLM_PC_IP:1234/v1/models` einfügen. Ein Modell bedient sowohl Befehle als auch Abfragen.

Für ein lokales LLM wird kein API-Schlüssel benötigt.

Klicke auf **Speichern**. Elite Intel startet das Sprachmodell daraufhin selbst neu.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
