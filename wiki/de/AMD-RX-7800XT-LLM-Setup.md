# LLMs auf der AMD RX 7800 XT betreiben (ROCm-Anleitung)

> Diese Anleitung wurde von **Ian Wirtz** bereitgestellt.

> **Empfehlung:** LM Studio (`lms`) liefert in der Regel die besten Ergebnisse, Ollama ist jedoch eine brauchbare Alternative.

---

## Voraussetzungen

### Schritt 1 – `rocm-hip-runtime` installieren

Bevor LM Studio oder Ollama die GPU über ROCm nutzen können, benötigt das System die HIP-Basisbibliotheken im Benutzerbereich, um mit dem Kernel-Treiber zu kommunizieren.

**Arch Linux / CachyOS:**
```bash
sudo pacman -S rocm-hip-runtime
```

**Ubuntu / Debian:**
```bash
sudo apt install rocm-hip-runtime
```

**Fedora:**
```bash
sudo dnf install rocm-hip-runtime
```

> **GPU-Zugriffsrechte:** Dein Benutzer muss den Gruppen `render` und `video` angehören. Überprüfe das mit:
> ```bash
> groups
> ```
> Falls eine der Gruppen fehlt, füge dich selbst hinzu:
> ```bash
> sudo usermod -aG video,render $USER
> ```
> Du musst dich **vollständig ab- und wieder anmelden** (oder neu starten), damit die Gruppenänderung wirksam wird.

---

### Schritt 2 – `rocm-smi` installieren *(ggf. optional)*

Bei schnell aktualisierten Distributionen wie Arch werden die Verwaltungswerkzeuge nicht immer als direkte Abhängigkeit von `rocm-hip-runtime` mitgezogen. Installiere sie explizit, um Bibliotheksversionskonflikte zu vermeiden.

**Arch Linux / CachyOS:**
```bash
sudo pacman -S rocm-smi-lib
```

**Ubuntu / Debian:**

Debian trennt das CLI-Werkzeug und die Laufzeitbibliothek in separate Pakete.

```bash
# Kommandozeilen-Überwachungswerkzeug
sudo apt update && sudo apt install rocm-smi

# Laufzeitbibliotheken
sudo apt update && sudo apt install librocm-smi64-1
```

> **Tipp:** Falls der genaue Paketname unbekannt ist, gib `sudo apt install librocm-smi64` ein und drücke **Tab**, um die aktuelle Versionsnummer automatisch zu vervollständigen.

**Fedora:**
```bash
# CLI-Werkzeug
sudo dnf install rocm-smi

# C/C++-Entwicklungsbibliotheken und Header (entspricht rocm-smi-lib auf Arch)
sudo dnf install rocm-smi-devel
```

---

## Ein Modell ausführen

| Modell | Benötigter VRAM | Hinweise |
|---|---|---|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 GB | ✅ Empfohlen für V1.0 |
| `google/gemma-4-e4b` | ~6,3 GB | ✅ Empfohlen für V1.1 |

> **Welches Modell?** `tulu-3.1-8b-supernova` ist das empfohlene Modell für **V1.0**. **V1.1** wechselt zu `google/gemma-4-e4b`, das die für die neue Begleiter-Funktion erforderliche Function-Calling-Unterstützung bietet. Die folgenden Befehle verwenden das V1.1-Modell – ersetze es bei V1.0 durch `tulu-3.1-8b-supernova`.

### Schritt 3 – Ein Modell mit ROCm-Beschleunigung laden

Beim Aufruf von `lms load` müssen die Hardware-Beschleunigungsflags explizit angegeben werden. Der Flag `--gpu max` weist die Laufzeitumgebung an, das gesamte Modell in den VRAM zu laden.

```bash
HSA_OVERRIDE_GFX_VERSION=11.0.0 lms load google/gemma-4-e4b --context-length 8192 --gpu max
```

Das Präfix `HSA_OVERRIDE_GFX_VERSION=11.0.0` teilt dem ROCm-Stack mit, die RX 7800 XT als nativ unterstütztes Compute-Ziel zu behandeln und stille Fallback-Fehler zu umgehen.

---

### Schritt 4 – Die Konfiguration dauerhaft speichern

Um nicht jedes Mal die Umgebungsvariable voranstellen zu müssen, füge sie zu deinem Shell-Profil hinzu.

**Bash:**
```bash
echo 'export HSA_OVERRIDE_GFX_VERSION=11.0.0' >> ~/.bashrc
source ~/.bashrc
```

**Fish (CachyOS-Standard) – Option A: Universelle Variable (empfohlen)**

Einmalig setzen; Fish speichert sie automatisch über Neustarts hinweg, ohne weitere Konfiguration:
```fish
set -Ux HSA_OVERRIDE_GFX_VERSION 11.0.0
```

**Fish – Option B: Expliziter Eintrag in der Konfigurationsdatei**
```fish
echo 'set -gx HSA_OVERRIDE_GFX_VERSION 11.0.0' >> ~/.config/fish/config.fish
source ~/.config/fish/config.fish
```

**Ollama (systemd-Dienst):**

Da Ollama unter dem eigenen Systembenutzer `ollama` läuft, muss die Variable über eine systemd-Drop-in-Datei eingebunden werden:

```bash
sudo mkdir -p /etc/systemd/system/ollama.service.d
sudo nano /etc/systemd/system/ollama.service.d/override.conf
```

Folgenden Inhalt einfügen, dann speichern und beenden (`Ctrl+O`, `Enter`, `Ctrl+X`):

```ini
[Service]
Environment="HSA_OVERRIDE_GFX_VERSION=11.0.0"
```

Anschließend den Dienst neu laden und starten:
```bash
sudo systemctl daemon-reload
sudo systemctl restart ollama
```

---

## Überprüfung

### Schritt 5 – Bestätigen, dass der Kernel-Compute-Treiber geladen ist

Wenn ein Befehl hängt, ist möglicherweise die Kernel-Compute-Schicht (`amdkfd`) nicht initialisiert. Prüfe, ob das System die GPU als ROCm-Compute-Plattform erkennt:

```bash
rocminfo
```

Scrolle zum Anfang der Ausgabe. Wenn dort `Can't open /dev/kfd` oder ein Absturz erscheint, stellt der Linux-Kernel die Compute-Schnittstelle dem Benutzerbereich nicht zur Verfügung. Falls ein benutzerdefinierter oder besonders aktueller Kernel eingesetzt wird, versuche mit dem Standard- oder LTS-Kernel (`linux-lts`) zu booten, um eine Treiberregression auszuschließen.

---

### Schritt 6 – Server starten und VRAM-Nutzung prüfen

**LM Studio:**
```bash
lms server start
```

**Ollama:**
```bash
ollama serve
```

Anschließend prüfen, ob das Modell in den VRAM geladen wurde:
```bash
rocm-smi
```

**Im Leerlauf (kein Modell geladen):**

![rocm-smi-Ausgabe ohne laufendes Modell](images/rocm-smi-without-game-running-example.png)

Im Leerlauf zieht die GPU minimale Leistung (~9 W), Taktraten liegen nahe dem Minimum und die VRAM-Nutzung ist gering (~44 %).

**Unter Last (Modell und Spiel laufen gleichzeitig):**

![rocm-smi-Ausgabe mit laufendem Elite Dangerous und EliteIntel](images/rocm-smi-with-game-and-Elite-Intel-running-example.png)

Unter kombinierter Last sollte die VRAM-Nutzung deutlich ansteigen (in diesem Beispiel 71 %), die GPU-Auslastung zunehmen und die Leistungsaufnahme entsprechend steigen (~147 W). Dies bestätigt, dass das Modell im VRAM liegt und Inferenz auf der GPU ausgeführt wird.
