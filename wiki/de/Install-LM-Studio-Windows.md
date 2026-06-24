## Lokales LLM – Windows-Setup (LM Studio)

Ein lokales LLM zu betreiben hält alle Daten privat und offline. Es gibt keine Abonnementgebühren. Hardware- und Stromkosten fallen an.

LM Studio ist eine Alternative zu Ollama. Es verwendet dieselben Modelle und dieselbe OpenAI-kompatible API. Die Wahl kann jederzeit in den Einstellungen geändert werden.

Es erfordert [LM Studio](https://lmstudio.ai) und eine leistungsfähige GPU.

---

### Mindest-Hardware

Um Elite Dangerous und das LLM auf **demselben Rechner** zu betreiben, ist mindestens eine **NVIDIA RTX 3060 mit 24 GB VRAM** erforderlich.

> **Tipp:** Elite Intel kann auf eine LM Studio-Instanz verweisen, die auf einem **separaten PC** in deinem Netzwerk läuft. Wenn ein zweiter Rechner mit einer leistungsfähigen GPU verfügbar ist, trägt der Spiele-PC in dieser Konfiguration keine Inferenzlast.

---

### Empfohlenes Modell

| Modell | Benötigter VRAM | Hinweise |
|---|---|---|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 GB | ✅ Empfohlen für V1.0 |
| `google/gemma-4-e4b` | ~6,3 GB | ✅ Empfohlen für V1.1 |

> **Welches Modell?** `tulu-3.1-8b-supernova` ist das empfohlene Modell für **V1.0**. **V1.1** wechselt zu `google/gemma-4-e4b`, das die für die neue Begleiter-Funktion erforderliche Function-Calling-Unterstützung bietet. Die folgenden Befehle verwenden das V1.1-Modell – ersetze es bei V1.0 durch `tulu-3.1-8b-supernova`.

---

Detailliertes (sehr detailliertes) Video-Tutorial von @DawnTreaderToolsoftheElite

[[youtube:F5RgRRePrTo]]

---

### Schritt 1 – LM Studio installieren

Öffne **PowerShell** und führe aus:

```powershell
irm https://lmstudio.ai/install.ps1 | iex
```

Damit werden das `lms`-CLI und die LM Studio-Laufzeit installiert. Öffne nach der Installation ein **neues** PowerShell-Fenster, damit die Änderungen wirksam werden.

Prüfen, ob es funktioniert:

```powershell
lms --help
```

> **Hinweis:** Wenn die LM Studio-Desktop-App bereits installiert ist, ist das `lms`-CLI möglicherweise bereits verfügbar. Führe `lms --help` aus, bevor du das Installationsskript ausführst.

---

### Schritt 2 – Das Modell herunterladen

Für **V1.1** lade `google/gemma-4-e4b` herunter:

```powershell
lms get google/gemma-4-e4b
```

Für **V1.0** lade `tulu-3.1-8b-supernova` herunter:

```powershell
lms get matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF
```

oder

```powershell
lms get Tulu-3.1
```
und die Variante `matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF` auswählen (kann auch als `Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF` aufgelistet sein).

Um heruntergeladene Modelle aufzulisten:

```powershell
lms ls
```

---

### Schritt 3 – Den Server starten

Das Modell laden und den Inferenzserver starten:

```powershell
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start
```

**HINWEIS**: Der Flag `--context-length 8192` ist erforderlich. Ohne ihn ist das Kontextfenster möglicherweise zu klein, was zu Prompt-Abschneidungen, Fehlern und Halluzinationen führt.

Überprüfe, ob er läuft, indem du einen Browser oder ein anderes PowerShell-Fenster öffnest und zu folgendem navigierst:

```
http://localhost:1234/v1/models
```

Du solltest eine JSON-Liste der geladenen Modelle erhalten. Die Modell-ID-Zeichenfolge in dieser Antwort gibst du in das Feld **LLM-Modell** von Elite Intel ein.

Den Server stoppen:

```powershell
lms server stop
```

> ⚠️ **Wichtig:** Der LM Studio-Server **überlebt keine Neustarts**. Führe `lms server start` nach jedem Neustart erneut aus oder verwende eine der Autostart-Optionen unten.

---

### Schritt 4 – (Optional) Autostart beim Booten

Zwei Optionen stehen zur Verfügung, um den Server über Neustarts hinweg am Laufen zu halten.

#### Option A – Desktop-App

Wenn die LM Studio-Desktop-App installiert ist, ist dies der einfachste Ansatz:

1. LM Studio öffnen und **Ctrl + ,** drücken, um die Einstellungen zu öffnen.
2. „**LLM-Server beim Login starten**" ankreuzen.
3. Das Schließen der App minimiert sie in die Taskleiste und hält den Server am Laufen. Beim nächsten Login wird sie automatisch wiederhergestellt.

#### Option B – Aufgabenplanung (Ohne GUI / Headless)

1. **Win + R** drücken, `taskschd.msc` eingeben, Enter drücken.
2. Im rechten Panel auf **Aufgabe erstellen** klicken.
3. **Allgemein-Tab**: Als `LM Studio Server` benennen. **„Mit höchsten Privilegien ausführen"** ankreuzen.
4. **Trigger-Tab**: Neu klicken → **„Bei Anmeldung"** → OK.
5. **Aktionen-Tab**: Neu klicken → **„Programm starten"**.
   - Programm/Skript: `%USERPROFILE%\.lmstudio\bin\lms.exe`
   - Argumente hinzufügen: `server start`

Um auch das Modell automatisch zu laden, stattdessen eine Batch-Datei erstellen:

```batch
@echo off
%USERPROFILE%\.lmstudio\bin\lms.exe daemon up
%USERPROFILE%\.lmstudio\bin\lms.exe load google/gemma-4-e4b --yes --context-length 8192 --gpu max
%USERPROFILE%\.lmstudio\bin\lms.exe server start
```

Als `start-lmstudio.bat` an einem dauerhaften Speicherort (z. B. `C:\Scripts\`) speichern und die Aufgabenplanungsaktion auf diese Datei verweisen.

---

### Schritt 5 – Elite Intel konfigurieren

Öffne den **Einstellungs-Tab** in Elite Intel:

- Das Feld **LLM-Schlüssel** leer lassen (lokales LM Studio benötigt keinen Schlüssel).
- **LLM-Adresse**: auf `http://localhost:1234/v1/chat/completions` setzen. Wenn LM Studio auf einem anderen Rechner läuft, `localhost` durch die IP dieses Rechners ersetzen.
- **LLM-Modell**: die Modell-ID-Zeichenfolge aus `http://localhost:1234/v1/models` einfügen.
- **Befehls-LLM**: auf dieselbe Modell-ID setzen.
- **Abfrage-LLM**: auf dieselbe Modell-ID setzen.
- Auf dem KI-Tab auf **Stop** und dann auf **Start** klicken, um Änderungen zu übernehmen.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
