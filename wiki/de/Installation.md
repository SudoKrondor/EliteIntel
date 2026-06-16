### Die veröffentlichte Version ist V1.0 und unterscheidet sich von dem, was auf Screenshots zu sehen ist.

### Wenn du Version V1.1 möchtest, tritt dem Beta-Test-Team bei.
### 👉[**Hier dem Beta-Test-Team V1.1 beitreten**](https://matrix.to/#/#krondor:matrix.org)👈

---

## <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Den [👉**Installer**👈](https://github.com/stone-alex/EliteIntel/releases) herunterladen.
2. Den Installer ausführen und den Anweisungen auf dem Bildschirm folgen.
   - **Parakeet STT** (lokale Spracherkennung) und **Kokoro TTS** (lokale Text-to-Speech) sind beide enthalten. Keine zusätzlichen Schritte oder Dienste erforderlich.
3. Ein LLM einrichten. Zwei Optionen stehen zur Verfügung:
   - **Lokales LLM** (kostenlos, offline): Siehe die [**Lokale LLM-Anleitung**](installing-local-llms). Erfordert leistungsfähige GPU-Hardware.
   - **Cloud-LLM** (einfacher einzurichten): Siehe die Anleitung [**App konfigurieren**](UI-and-Configuration-Options) für die API-Schlüssel-Einrichtung.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux
### Installation (jede Desktop-Distribution – kein sudo erforderlich)
1. Das Installationsskript herunterladen:

```shell
curl -L -o installer.sh https://raw.githubusercontent.com/stone-alex/EliteIntel/refs/heads/master/distribution/installer.sh
```

2. Das Skript ausführbar machen und ausführen:
```shell
chmod +x installer.sh
./installer.sh
```
Die App wird in `~/.var/app/elite.intel.app` installiert.
Sowohl **Parakeet STT** als auch **Kokoro TTS** sind in der App gebündelt. Keine zusätzliche Installation erforderlich. Im Einstellungs-Tab per **☑ Verwenden**-Kontrollkästchen aktivieren.

3. Ein LLM einrichten. Zwei Optionen stehen zur Verfügung:
   - **Lokales LLM** (kostenlos, offline): Siehe die [**Lokale LLM-Anleitung**](installing-local-llms). Erfordert leistungsfähige GPU-Hardware.
   - **Cloud-LLM** (einfacher einzurichten): Siehe die Anleitung [**App konfigurieren**](UI-and-Configuration-Options) für die API-Schlüssel-Einrichtung.

Setup abgeschlossen. Nächste Schritte unter [**App konfigurieren**](Configuration).

---

### Deinstallation

Verwende den `-d`-Flag, um die App zu entfernen. Der Installer fragt vor dem Löschen von Konfigurations- und API-Schlüsseldaten nach.

```shell
bash installer.sh -d
```

----
Bei Problemen bitte auf Matrix melden. Fehlermeldungen und Pull Requests sind willkommen.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
