## Lokales LLM – Linux-Setup (Ollama)

Ein lokales LLM zu betreiben hält alle Daten privat und offline. Es gibt keine Abonnementgebühren. Hardware- und Stromkosten fallen an.

Es erfordert [Ollama](https://ollama.com) und eine leistungsfähige GPU.

---

### Mindest-Hardware

Um Elite Dangerous und das LLM auf **demselben Rechner** zu betreiben, ist mindestens eine **NVIDIA RTX 3060 mit 12 GB VRAM** erforderlich. Bei dieser Spezifikation ist der Leistungsspielraum begrenzt.

> **Tipp:** Elite Intel kann auf eine Ollama-Instanz verweisen, die auf einem **separaten PC** in deinem Netzwerk läuft. Wenn ein zweiter Rechner mit einer leistungsfähigen GPU verfügbar ist, trägt der Spiele-PC in dieser Konfiguration keine Inferenzlast.

---

### Empfohlenes Modell

| Modell | Benötigter VRAM | Hinweise |
|---|---|---|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 GB | ✅ Empfohlen für V1.0 |
| `google/gemma-4-e4b` | ~6,3 GB | ✅ Empfohlen für V1.1 |

> **Welches Modell?** `tulu-3.1-8b-supernova` ist das empfohlene Modell für **V1.0**. **V1.1** wechselt zu `google/gemma-4-e4b`, das die für die neue Begleiter-Funktion erforderliche Function-Calling-Unterstützung bietet. Die folgenden Befehle verwenden das V1.1-Modell – ersetze es bei V1.0 durch `tulu-3.1-8b-supernova`.

> **Hinweis:** Für die schnellste lokale Inferenz empfiehlt sich [LM Studio](Install-LM-Studio-Linux) mit `matrixportalx/tulu-3.1-8b-supernova`. In Tests war es auf derselben Hardware mit demselben Modell deutlich schneller als Ollama.

---

### Schritt 1 – Ollama installieren

```shell
curl -fsSL https://ollama.com/install.sh | sh
```

Ollama installiert sich als systemd-Dienst und startet automatisch.

---

### Schritt 2 – Ein empfohlenes Modell herunterladen

Für **V1.1** lade `google/gemma-4-e4b` herunter:

```shell
ollama pull google/gemma-4-e4b
```

Für **V1.0** lade `tulu-3.1-8b-supernova` herunter:

```shell
ollama pull hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF
```

---

### Schritt 3 – (Optional) Den Ollama-Dienst anpassen

Ollama funktioniert ohne Anpassung. Die folgende Konfiguration verbessert das VRAM-Management beim parallelen Betrieb mit Elite Dangerous.

```shell
sudo nano /etc/systemd/system/ollama.service.d/override.conf
```

Folgenden Inhalt einfügen:

```ini
[Service]
Environment="OLLAMA_MAX_VRAM=14000000000"
Environment="OLLAMA_DEBUG=0"
Environment="OLLAMA_NUM_PARALLEL=3"
Environment="OLLAMA_MAX_LOADED_MODELS=1"
Environment="OLLAMA_FLASH_ATTENTION=1"
Environment="OLLAMA_KEEP_ALIVE=-1"
Nice=10
IOSchedulingClass=best-effort
IOSchedulingPriority=5
```

Dann neu laden und neu starten:

```shell
sudo systemctl daemon-reload
sudo systemctl restart ollama.service
```

#### Was diese Einstellungen bewirken

**`OLLAMA_MAX_VRAM`**: Harte Obergrenze für den VRAM, den Ollama nutzen kann, in Bytes. `14000000000` = 14 GB. Lässt den Rest für Elite Dangerous. Nach GPU und Spielanforderungen anpassen.

**`OLLAMA_NUM_PARALLEL`**: Anzahl der gleichzeitig verarbeiteten Anfragen. Elite Intel stellt asynchrone Aufrufe, daher verursacht ein zu niedriger Wert Fehler. `3` deckt die typische Überschneidung von Befehlen und Abfragen ohne Überbelegung ab.

**`OLLAMA_MAX_LOADED_MODELS`**: Hält nur ein Modell gleichzeitig im VRAM.

**`OLLAMA_FLASH_ATTENTION`**: Aktiviert Flash Attention, was den Speicherbandbreitenverbrauch während der Inferenz reduziert. Generell schneller, besonders bei wiederholten Anfragen.

**`OLLAMA_KEEP_ALIVE=-1`**: Hält das Modell dauerhaft im VRAM geladen. Ohne diese Einstellung kann Ollama das Modell nach einer Inaktivitätsperiode entladen, was beim nächsten Aufruf eine Neuladelatenz verursacht.

---

### Schritt 4 – Elite Intel konfigurieren

Öffne den **Einstellungs-Tab** in Elite Intel:

- Das Feld **LLM-Schlüssel** leer lassen (lokales Ollama benötigt keinen Schlüssel).
- **LLM-Adresse** ist standardmäßig `http://localhost:11434/api/chat`. Wenn Ollama auf einem anderen Rechner läuft, `localhost` durch die IP dieses Rechners ersetzen.
- **Befehls-LLM**: auf `google/gemma-4-e4b` setzen (oder den Namen, der bei `ollama ls` angezeigt wird).
- **Abfrage-LLM**: auf `google/gemma-4-e4b` setzen (oder den Namen, der bei `ollama ls` angezeigt wird).
- Auf dem KI-Tab auf **Stop** und dann auf **Start** klicken, um Änderungen zu übernehmen.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
