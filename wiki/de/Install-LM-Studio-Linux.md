## Lokales LLM – Linux-Setup (LM Studio)

Ein lokales LLM zu betreiben hält alle Daten privat und offline. Es gibt keine Abonnementgebühren. Hardware- und Stromkosten fallen an.

LM Studio ist eine Alternative zu Ollama. Es verwendet dieselben Modelle und dieselbe OpenAI-kompatible API. Die Wahl kann jederzeit in den Einstellungen geändert werden.

Es erfordert [LM Studio](https://lmstudio.ai) und eine leistungsfähige GPU.

---

### Mindest-Hardware

Um Elite Dangerous und das LLM auf **demselben Rechner** zu betreiben, ist mindestens eine **NVIDIA RTX 3060 mit 12 GB VRAM** erforderlich. Bei dieser Spezifikation ist der Leistungsspielraum begrenzt.

> **Tipp:** Elite Intel kann auf eine LM Studio-Instanz verweisen, die auf einem **separaten PC** in deinem Netzwerk läuft. Wenn ein zweiter Rechner mit einer leistungsfähigen GPU verfügbar ist, trägt der Spiele-PC in dieser Konfiguration keine Inferenzlast.

---

### Empfohlenes Modell

| Modell | Benötigter VRAM | Hinweise |
|---|---|---|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 GB | ✅ Empfohlen für V1.0 |
| `google/gemma-4-e4b` | ~6,3 GB | ✅ Empfohlen für V1.1 |

> **Welches Modell?** `tulu-3.1-8b-supernova` ist das empfohlene Modell für **V1.0**. **V1.1** wechselt zu `google/gemma-4-e4b`, das die für die neue Begleiter-Funktion erforderliche Function-Calling-Unterstützung bietet. Die folgenden Befehle verwenden das V1.1-Modell – ersetze es bei V1.0 durch `tulu-3.1-8b-supernova`.

---

[[youtube:2HGFmlZGK1g]]

---

### Schritt 1 – LM Studio installieren

```shell
curl -fsSL https://lmstudio.ai/install.sh | bash
```

Das Installationsprogramm legt alles in `~/.lmstudio/` ab und fügt das `lms`-CLI-Werkzeug hinzu. Füge nach Abschluss das CLI zu deinem PATH hinzu:

```shell
# Dies zu deiner ~/.bashrc hinzufügen
export PATH="$HOME/.lmstudio/bin:$PATH"
```

Dann die Shell neu laden:

```shell
source ~/.bashrc
```

Prüfen, ob es funktioniert:

```shell
lms --help
```

---

### Schritt 2 – Das Modell herunterladen

Für **V1.1** lade `google/gemma-4-e4b` herunter:

```shell
lms get google/gemma-4-e4b
```

Für **V1.0** lade `tulu-3.1-8b-supernova` herunter:

```shell
lms get tulu3.1
Searching for models with the term tulu3.1
No exact match found. Please choose a model from the list below.

? Select a model to download
❯ QuantFactory/Tulu-3.1-8B-SuperNova-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF
  QuantFactory/Tulu-3.1-8B-SuperNova-Smart-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-GGUF
  bunnycore/Tulu-3.1-8B-SuperNova-Smart-IQ4_XS-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-Smart-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-Smart-i1-GGUF
  matrixportalx/Tulu-3.1-8B-SuperNova-Q4_0-GGUF
  matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF

↑↓ navigate • ⏎ select
```
Mit den Pfeiltasten navigieren und Enter zum Auswählen drücken. `matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF` auswählen.

Um heruntergeladene Modelle aufzulisten:

```shell
lms ls
```

Dies ist der Standardweg. Allerdings [hat LM Studio einen bekannten Fehler](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). In einigen Fällen schlägt der Download fehl mit:
```Error: No staff picks found with the specified search criteria.```

Wenn das auftritt, lade das Modell manuell herunter:

```shell
curl -s "https://huggingface.co/api/models/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF" | grep -o '"rfilename":"[^"]*\.gguf"'
```
Dann importieren:

```shell
lms import /path/to/tulu-3.1-8b-supernova-q4_k_m.gguf
```


---

### Schritt 3 – Den Server starten

Das Modell laden und den Inferenzserver starten:

```shell
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start
```

`--gpu max` verlagert die Inferenz auf die GPU für maximale Leistung.

Überprüfen, ob er läuft:

```shell
curl http://localhost:1234/v1/models
```

Du solltest eine JSON-Liste der geladenen Modelle erhalten. Die Modell-ID-Zeichenfolge in dieser Antwort gibst du in das Feld **LLM-Modell** von Elite Intel ein.

Den Server stoppen:

```shell
lms server stop
```

> ⚠️ **Wichtig:** Der LM Studio-Server **überlebt keine Neustarts**. Führe `lms server start` nach jedem Neustart erneut aus oder richte den optionalen Autostart unten ein.

---

### Schritt 4 – (Optional) Autostart beim Booten

Um LM Studio automatisch zu starten, richte es als **Benutzer**-systemd-Dienst ein. Dieser läuft unter deiner eigenen Sitzung und nicht als Systemdienst. Er startet, nachdem die Desktop-Umgebung hochgefahren ist. Root-Zugriff ist nicht erforderlich.

Finde deine Benutzer-ID heraus. (Ersetze den Benutzernamen durch deinen tatsächlichen Benutzernamen)
```shell
id -u DEIN_BENUTZERNAME
```

Merke dir diese Zahl. Du benötigst sie später für die Konfiguration.

Erstelle das Benutzer-systemd-Verzeichnis, falls es nicht existiert:

```shell
mkdir -p ~/.config/systemd/user
```

Erstelle die Service-Datei:

```shell
nano ~/.config/systemd/user/lmstudio.service
```

Folgenden Inhalt einfügen:

```ini
[Unit]
Description=LM Studio Server
After=network.target

[Service]
Type=oneshot
RemainAfterExit=yes
Environment="HOME=/home/DEIN_BENUTZERNAME"
Environment="PATH=/home/DEIN_BENUTZERNAME/.lmstudio/bin:/usr/local/bin:/usr/bin:/bin"
Environment="XDG_RUNTIME_DIR=/run/user/DEINE_UID"
ExecStartPre=/home/DEIN_BENUTZERNAME/.lmstudio/bin/lms daemon up
ExecStartPre=/home/DEIN_BENUTZERNAME/.lmstudio/bin/lms load google/gemma-4-e4b --yes --context-length 8192
ExecStart=/home/DEIN_BENUTZERNAME/.lmstudio/bin/lms server start --bind 0.0.0.0 --port 1234
ExecStop=/home/DEIN_BENUTZERNAME/.lmstudio/bin/lms server stop
ExecStopPost=/home/DEIN_BENUTZERNAME/.lmstudio/bin/lms daemon down

[Install]
WantedBy=default.target
```

`DEIN_BENUTZERNAME` durch deinen Linux-Benutzernamen und `DEINE_UID` durch deine Benutzer-ID ersetzen. So findest du deine UID:

```shell
id -u
```

> ⚠️ **Warum `XDG_RUNTIME_DIR`?** Benutzerdienste laufen in einer vereinfachten Umgebung, die möglicherweise keine Sitzungsvariablen enthält. LM Studio verwendet `XDG_RUNTIME_DIR` für IPC. Ohne diese Variable kann der Dienst stillschweigend fehlschlagen, auch wenn `lms` vom Terminal aus korrekt funktioniert. Dies ist die häufigste Ursache für Dienstfehler, wenn die manuelle Ausführung erfolgreich ist.

Aktivieren und starten:

```shell
systemctl --user daemon-reload
systemctl --user enable lmstudio.service
systemctl --user start lmstudio.service
```

Überprüfen, ob es läuft:

```shell
systemctl --user status lmstudio.service
curl http://localhost:1234/v1/models
```

> **Fehlerbehebung:** Wenn der Dienst fehlschlägt, Journal prüfen:
> ```shell
> journalctl --user -xeu lmstudio.service --no-pager | tail -40
> ```
> Wenn dort „Failed to load model" steht, `lms ls` ausführen und bestätigen, dass der Modellname genau dem in der Service-Datei entspricht.

---

### Schritt 4b – (Optional) Langsame Inferenz nach dem Boot beheben

Einige Benutzer erleben nach dem Start langsame Inferenzantworten von LM Studio. Das Problem löst sich sofort nach einem manuellen Dienst-Neustart. Dies wird durch eine Eigenart bei der Initialisierung des LM Studio-Daemons verursacht. Der erste Kaltstart kann die Inferenzlaufzeit in einem beeinträchtigten Zustand hinterlassen.

Wenn nach einem Neustart langsame Antworten auftreten und sich nach einem manuellen Neustart auflösen, automatisiert dieser Timer die Lösung.

Einen Begleitdienst erstellen:

```shell
nano ~/.config/systemd/user/lmstudio-restart.service
```

```ini
[Unit]
Description=LM Studio post-boot restart
After=lmstudio.service

[Service]
Type=oneshot
ExecStart=systemctl --user restart lmstudio.service
```

Den Timer erstellen:

```shell
nano ~/.config/systemd/user/lmstudio-restart.timer
```

```ini
[Unit]
Description=Restart LM Studio 2 minutes after login

[Timer]
OnBootSec=2min
Unit=lmstudio-restart.service

[Install]
WantedBy=timers.target
```

Den Timer aktivieren:

```shell
systemctl --user daemon-reload
systemctl --user enable --now lmstudio-restart.timer
```

Der Timer wartet 2 Minuten nach dem Login, startet den LM Studio-Dienst einmal neu und bleibt dann inaktiv. Wenn keine langsame Inferenz auftritt, ist dieser Schritt nicht erforderlich.

---

### Ollama-Autostart deaktivieren (falls installiert)

Ollama installiert sich standardmäßig als aktivierter systemd-Dienst. Um stattdessen LM Studio zu verwenden und Ollama nur bei Bedarf zu starten:

```shell
sudo systemctl disable ollama.service
sudo systemctl stop ollama.service
```

---

### Schritt 5 – Elite Intel konfigurieren

Öffne den **Einstellungs-Tab** in Elite Intel:

- Das Feld **LLM-Schlüssel** leer lassen (lokales LM Studio benötigt keinen Schlüssel).
- **LLM-Adresse**: auf `http://localhost:1234/v1/chat/completions` setzen. Wenn LM Studio auf einem anderen Rechner läuft, `localhost` durch die IP dieses Rechners ersetzen.
- **LLM-Modell**: die Modell-ID-Zeichenfolge aus `curl http://localhost:1234/v1/models` einfügen.
- **Befehls-LLM**: auf dieselbe Modell-ID setzen.
- **Abfrage-LLM**: auf dieselbe Modell-ID setzen.
- Auf dem KI-Tab auf **Stop** und dann auf **Start** klicken, um Änderungen zu übernehmen.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
