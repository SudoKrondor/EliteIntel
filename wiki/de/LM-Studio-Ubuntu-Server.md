# LM Studio auf einem Headless-Ubuntu-Server

> Diese Anleitung wurde von **Wim** bereitgestellt.

> **Elite Intel:** Diese Anleitung richtet einen eigenen Rechner im LAN ein, der LM Studio für Elite Intel betreibt. Danach im [Reiter Dienste](UI-Settings-Tab) als **Adresse** `http://SERVER_IP:1234/v1/chat/completions` eintragen und das unterstützte Modell **`google/gemma-4-e4b`** laden. Der Server muss über localhost hinaus lauschen (`--bind 0.0.0.0`, siehe Teil 2 § 9), und die Firewall muss den Gaming-PC zulassen.

---

# Teil 1: Ubuntu Server als Host für lokale KI / LM Studio

**Anleitung für eine saubere Basisinstallation**  
**Empfohlene Basis:** Ubuntu Server 24.04 LTS (amd64)  
**Stand des Dokuments:** 12. September 2026

## 1. Zweck

Diese Anleitung bereitet einen sauberen Ubuntu-Server für einen Headless-KI-Einsatz wie LM Studio / llmster vor. Sie endet bewusst vor der Installation von LM Studio selbst. Ziel ist ein kleiner, nachvollziehbarer Server mit funktionierendem Netzwerk, SSH, Updates, Firewall und – falls zutreffend – einem geprüften NVIDIA-Grafiktreiber.

Dies ist keine Anleitung zur Wiederherstellung oder Hardware-Fehlersuche. Sie setzt voraus, dass der Rechner mit Standard-CPU-/RAM-Einstellungen stabil läuft und wichtige Daten bereits gesichert sind.

## 2. Welche Ubuntu-Version?

Für einen konservativen, langlebigen KI-Server verwendet diese Anleitung **Ubuntu Server 24.04 LTS (Noble Numbat)**. Canonical bietet für Ubuntu 24.04 LTS Standard-Sicherheitswartung bis Mai 2029. Ubuntu 26.04 LTS ist ebenfalls verfügbar und die aktuellere LTS, doch 24.04 wird weiterhin voll unterstützt und ist eine ausgereifte Basis für Systeme, bei denen Stabilität wichtiger ist als der neueste Kernel und die neueste Userspace-Software.

Für eine ganz neue Installation, bei der neuere Hardware den aktuellsten Kernel-/Treiber-Stack benötigt, ist Ubuntu 26.04 LTS ebenfalls eine sinnvolle Wahl. Die Befehle in dieser Anleitung sind bewusst konventionell gehalten und sollten weitgehend gleich bleiben.

Offizielle Referenzen:
- https://ubuntu.com/about/release-cycle
- https://documentation.ubuntu.com/release-notes/24.04/
- https://ubuntu.com/server/docs/tutorial/basic-installation/

## 3. Vor der Installation

Folgendes bereithalten:

- Einen 64-Bit-Intel/AMD-Rechner (amd64) für diese Anleitung.
- Einen USB-Stick mit dem Ubuntu-Server-ISO.
- Möglichst kabelgebundenes Ethernet.
- Den gewünschten Hostnamen, Benutzernamen und ein starkes Passwort.
- Eine Entscheidung, welche Festplatte Ubuntu löschen darf.
- Für GPU-Inferenz eine unterstützte, korrekt eingebaute und mit Strom versorgte NVIDIA-GPU.

Lief der Rechner zuvor mit übertakteter CPU oder übertaktetem RAM, vor der Inbetriebnahme als Server auf stabile Standardwerte zurücksetzen. KI-Laufzeitumgebungen und das Kompilieren von Kernelmodulen können Instabilitäten aufdecken, die bei leichterer Last nicht auftreten.

## 4. Ubuntu Server installieren

Das Ubuntu-Server-Installationsprogramm starten und dem normalen Ablauf folgen. Für einen einfachen dedizierten Server:

1. Sprache und Tastaturlayout wählen.
2. Das kabelgebundene Netzwerk zunächst per DHCP konfigurieren lassen.
3. Die vorgesehene Systemfestplatte verwenden. **Die Partitionierung kann vorhandene Daten zerstören**, daher die gewählte Festplatte sorgfältig prüfen.
4. Den Administratorbenutzer und den Hostnamen anlegen.
5. **Install OpenSSH server** auswählen, wenn angeboten. Falls nicht ausgewählt, lässt es sich nach dem ersten Start nachinstallieren.
6. Optionale Server-Snaps überspringen, sofern sie nicht ausdrücklich benötigt werden.
7. Die Installation abschließen und neu starten.

## 5. Erste Anmeldung und Systemaktualisierung

Lokal oder per SSH anmelden, dann den Paketindex und die installierten Pakete aktualisieren:

```bash
sudo apt update
sudo apt upgrade -y
```

Meldet Ubuntu, dass ein Neustart erforderlich ist:

```bash
sudo reboot
```

Nach dem erneuten Verbinden Release und Kernel prüfen:

```bash
lsb_release -a
uname -r
```

## 6. Ein kleines Administrationswerkzeug-Set installieren

Die folgenden Pakete sind auf einem Headless-KI-Server nützlich:

```bash
sudo apt install -y \
  openssh-server \
  curl \
  ca-certificates \
  jq \
  htop \
  nvtop \
  ufw
```

`curl` wird vom LM-Studio-Installer und für API-Tests verwendet, `jq` macht JSON-Ausgaben lesbar, `htop` überwacht CPU/RAM, `nvtop` überwacht GPUs und `ufw` stellt eine einfache Host-Firewall bereit.

Prüfen, ob SSH aktiv ist:

```bash
systemctl status ssh --no-pager
```

## 7. SSH-Zugang

Die aktuelle Adresse des Servers ermitteln:

```bash
ip -br address
```

Von einem anderen Rechner im LAN:

```bash
ssh YOUR_USER@SERVER_IP
```

Für die regelmäßige Nutzung ist Public-Key-Authentifizierung per SSH dem wiederholten Eingeben eines Passworts vorzuziehen. Auf dem Client-Rechner:

```bash
ssh-keygen -t ed25519
ssh-copy-id YOUR_USER@SERVER_IP
```

Eine neue SSH-Sitzung testen, bevor Einstellungen zur Passwort-Authentifizierung geändert werden. Sperren Sie sich nicht selbst aus einem entfernten Server aus.

Offizielle OpenSSH-Referenz:
https://ubuntu.com/server/docs/how-to/security/openssh-server/

## 8. Einfache Firewall

Vor dem Aktivieren von UFW SSH zulassen:

```bash
sudo ufw allow OpenSSH
sudo ufw enable
sudo ufw status verbose
```

Bei einem dedizierten KI-Server im Heimnetz/LAN ist es besser, Anwendungsports nur für vertrauenswürdige Clients oder das vertrauenswürdige LAN freizugeben, statt wahllos auf allen Netzwerkschnittstellen.

Der Standard-API-Port von LM Studio ist 1234. **Noch nicht öffnen.** Die passende Regel erst hinzufügen, wenn LM Studio installiert ist und feststeht, welche Clients es erreichen sollen.

Offizielle Firewall-Referenz:
https://ubuntu.com/server/docs/how-to/security/firewalls/

## 9. NVIDIA-Grafiktreiber

Diesen Abschnitt überspringen, wenn der Server CPU-Inferenz oder einen Nicht-NVIDIA-Beschleuniger nutzen wird.

Zuerst das System aktualisieren und bei Bedarf neu starten. Dann Ubuntu passende NVIDIA-Compute-Treiber ermitteln lassen:

```bash
sudo ubuntu-drivers list --gpgpu
```

Für Compute-/Server-Workloads dokumentiert Ubuntu die automatische Installation mit:

```bash
sudo ubuntu-drivers install --gpgpu
```

Nach der Installation neu starten:

```bash
sudo reboot
```

Dann die GPU prüfen:

```bash
nvidia-smi
```

Ein erfolgreiches `nvidia-smi` sollte GPU, Treiberversion, Temperatur, VRAM und Prozesstabelle anzeigen.

### Hinweis zu Secure Boot

Secure Boot erfordert kompatible, signierte Kernelmodule. Das Werkzeug `ubuntu-drivers` ist der bevorzugte Installationsweg, da es dafür ausgelegt ist, passende Ubuntu-paketierte Treiber auszuwählen. Lädt das NVIDIA-Modul nicht, zuerst den Secure-Boot-Status prüfen, statt die Treiber wiederholt neu zu installieren:

```bash
mokutil --sb-state
```

Secure Boot nicht routinemäßig als ersten Schritt deaktivieren. Hat eine bestimmte Firmware-/Treiber-Kombination Probleme mit Signierung oder MOK-Registrierung, dies gezielt lösen und die BIOS-Änderung dokumentieren.

Offizielle Referenz zu NVIDIA unter Ubuntu:
https://ubuntu.com/server/docs/how-to/graphics/install-nvidia-drivers/

## 10. Abschließende Bereitschaftsprüfung

Ausführen:

```bash
hostnamectl
ip -br address
systemctl is-active ssh
sudo ufw status
free -h
df -h /
nvidia-smi    # NVIDIA systems only
```

An diesem Punkt sollte der Rechner Folgendes haben:

- eine unterstützte Ubuntu-LTS-Installation;
- aktuelle Paket-Updates;
- funktionierende LAN-Verbindung;
- funktionierenden SSH-Zugang;
- eine einfache Firewall;
- nützliche Administrationswerkzeuge;
- einen geprüften NVIDIA-Treiber, falls zutreffend.

Der Server ist nun bereit für die Installation von LM Studio / llmster.

## 11. Was bewusst nicht behandelt wird

Diese Basis konfiguriert kein NAS/Samba, Docker, Datenbanken, Webserver, RAID, eigene Festplattenlayouts, VPNs, Pi-hole/DNS, Desktopumgebungen, CUDA Toolkit oder anwendungsspezifische Software. Diese gehören in separate Einrichtungsanleitungen.

Ein eigenständiges CUDA Toolkit ist **nicht nötig, nur um LM Studio mit einer unterstützten NVIDIA-GPU zu betreiben**; den von LM Studio unterstützten Laufzeit-/Treiber-Stack verwenden, sofern nicht eine andere Anwendung ausdrücklich das CUDA-Entwicklungs-Toolkit benötigt.

## 12. Grundlagen der Wartung

Regelmäßig normale Updates einspielen:

```bash
sudo apt update
sudo apt upgrade
```

Prüfen, ob ein Neustart aussteht:

```bash
if [ -f /run/reboot-required ]; then
  cat /run/reboot-required
fi
```

Vor einem großen Ubuntu-Release-Upgrade die Release Notes dieser Version lesen und den Server sichern. Release-Upgrades sind etwas anderes als gewöhnliche Paket-Updates.

---

## Offizielle Quellen

- Ubuntu-Server-Dokumentation: https://ubuntu.com/server/docs/
- Ubuntu-Release-Zyklus: https://ubuntu.com/about/release-cycle
- Release Notes Ubuntu 24.04 LTS: https://documentation.ubuntu.com/release-notes/24.04/
- Basisinstallation: https://ubuntu.com/server/docs/tutorial/basic-installation/
- Paketverwaltung: https://ubuntu.com/server/docs/how-to/software/package-management/
- OpenSSH: https://ubuntu.com/server/docs/how-to/security/openssh-server/
- UFW/Firewall: https://ubuntu.com/server/docs/how-to/security/firewalls/
- NVIDIA-Treiber: https://ubuntu.com/server/docs/how-to/graphics/install-nvidia-drivers/

---

# Teil 2: LM Studio als Headless-Server und Modellverwaltung

**Installation, Modellwahl, Quantisierung, API-Dienst und Modellwechsel**  
**Ziel:** Linux/Ubuntu-Headless-Server  
**Stand des Dokuments:** 12. September 2026

## 1. Zweck

Diese Anleitung beginnt mit einem sauberen Ubuntu-Server, der bereits funktionierendes Netzwerk, SSH und – für NVIDIA-Beschleunigung – einen geprüften GPU-Treiber hat. Sie installiert die Headless-Laufzeit von LM Studio, erklärt, wie Modelle ausgewählt und heruntergeladen werden, stellt bei Bedarf die lokale API bereit und liefert einen interaktiven Bash-Modellumschalter.

Die serverseitige Komponente von LM Studio heißt **llmster**. Das Kommandozeilenprogramm zu ihrer Steuerung heißt **lms**. Ein grafischer Desktop ist nicht erforderlich.

Offizielle Headless-Dokumentation:
https://lmstudio.ai/docs/developer/core/headless

## 2. Drei Namen, die man kennen sollte

- **LM Studio** ist das Gesamtprodukt und Ökosystem.
- **llmster** ist der Headless-/Server-Daemon. Er betreibt den Inferenzdienst ohne GUI.
- **lms** ist die CLI zum Herunterladen, Laden, Entladen, Untersuchen und Bereitstellen von Modellen.

Auf einem dedizierten Linux-Inferenzserver ist llmster + lms normalerweise die sauberste Lösung.

## 3. llmster / lms installieren

Als der normale Benutzer installieren, der den Dienst ausführen wird, nicht als root:

```bash
curl -fsSL https://lmstudio.ai/install.sh | bash
```

Den Daemon starten:

```bash
lms daemon up
```

Die CLI prüfen:

```bash
lms --version
lms --help
```

Findet die Shell `lms` nicht, eine neue Login-Shell starten oder das Binärverzeichnis von LM Studio zum PATH der aktuellen Shell hinzufügen:

```bash
export PATH="$HOME/.lmstudio/bin:$PATH"
```

Um das für Bash dauerhaft festzulegen:

```bash
echo 'export PATH="$HOME/.lmstudio/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

## 4. Modelle finden und herunterladen

`lms get` ohne Modell zeigt Empfehlungen:

```bash
lms get
```

Ein Modell über seinen Katalognamen herunterladen:

```bash
lms get publisher/model-name
```

Das genaue Modell wird in dieser Anleitung bewusst dem Leser überlassen und nicht fest vorgegeben.

Heruntergeladene Modelle auflisten:

```bash
lms ls
```

Heruntergeladene Varianten/Quantisierungen anzeigen, sofern vorhanden:

```bash
lms ls --variants
```

JSON-Ausgabe ist für Skripte nützlich:

```bash
lms ls --json | jq
```

LM Studio kann unterstützte Modelle aus seinem Katalog bzw. dem Hugging-Face-Ökosystem herunterladen. Die aktuelle CLI unterstützt auch die Angabe einer Quantisierung beim Herunterladen, zum Beispiel:

```bash
lms get publisher/model-name@q4_k_m
```

Die tatsächlich für dieses Modell angebotenen Quantisierungsnamen verwenden; nicht jedes Modell wird in jedem Format veröffentlicht.

Offizielle Dokumentation zum Herunterladen von Modellen:
https://lmstudio.ai/docs/cli/local-models/get

## 5. Modellgröße und Quantisierung – eine Erklärung für Einsteiger

Ein Modell enthält Milliarden gelernter Zahlenwerte, sogenannte **Gewichte**. Höhere numerische Präzision speichert diese Werte genauer, verbraucht aber mehr Speicherplatz und RAM/VRAM. **Quantisierung** komprimiert das Modell, indem diese Werte mit weniger Bits dargestellt werden.

Ein hilfreiches Bild ist eine hochwertige Audioaufnahme: Q8 bewahrt mehr Details, Q6 komprimiert etwas stärker und Q4 noch stärker. Es bleiben Versionen desselben trainierten Modells; Q8 fügt nicht auf magische Weise neues Training oder Wissen hinzu.

Typische Einordnung:

| Quantisierung | Speicherbedarf | Genauigkeit | Praktische Rolle |
|---|---|---|---|
| Q4-Klasse | Niedriger | Gut | Hervorragender Ausgangspunkt; reicht oft für Chat/Tool-Nutzung |
| Q5/Q6-Klasse | Mittel | Höher | Gute Balance aus Qualität und Speicher |
| Q8-Klasse | Höher | Sehr hoch | Nutzen, wenn die zusätzliche Genauigkeit messbar hilft |

LM Studio empfiehlt selbst, eine 4-Bit-Option oder höher zu wählen, wenn der Rechner sie tragen kann.

### Die wichtige Regel

**Nicht Q8 wählen, nur weil 8 größer ist als 4.** Die kleinste Quantisierung wählen, die die tatsächliche Arbeitslast zuverlässig bewältigt. Zusätzliche Präzision ist nur dann etwas wert, wenn sie Antworten, Schlussfolgerungen, Tool-Nutzung oder Stabilität so verbessert, dass sie ihren dauerhaft größeren Speicherbedarf rechtfertigt.

## 6. Modellgröße ist nicht gleich Modellintelligenz

Quantisierung und Modellgröße sind unterschiedliche Entscheidungen.

Ein größeres, leistungsfähigeres Modell mit moderater Quantisierung kann bei schwierigem Schlussfolgern ein kleineres Modell in Q8 übertreffen. Umgekehrt kann ein kleines Q4-Modell ideal für schnelle Befehlsinterpretation sein.

Bei der Modellwahl berücksichtigen:

1. **Aufgabe:** Chat, Programmieren, Tool-/Function-Calling, Bildverständnis, Datenanalyse, Embeddings usw.
2. **Modellarchitektur/Parameterzahl:** Größere Modelle können oft mehr, brauchen aber mehr Speicher und sind ggf. langsamer.
3. **Quantisierung:** Q4/Q6/Q8 verschiebt das Verhältnis von Genauigkeit zu Speicher bei diesem Modell.
4. **Kontextlänge:** Größerer Kontext kann zusätzlichen Speicher verbrauchen.
5. **Passt es auf die GPU:** Modell und Arbeitskontext im VRAM zu halten, ergibt in der Regel die beste GPU-Inferenz.
6. **Tool-Unterstützung:** Benötigt eine Anwendung natives Function-/Tool-Calling, ein Modell wählen, das dafür trainiert ist und es unterstützt.

Die richtige Wahl hängt von der Arbeitslast ab. Mit echten Prompts testen, statt sich nur auf Benchmark-Werte zu verlassen.

## 7. Speicherbedarf vor dem Laden abschätzen

LM Studio kann den Ressourcenbedarf abschätzen, ohne das Modell zu laden:

```bash
lms load publisher/model-name \
  --context-length 8192 \
  --gpu max \
  --estimate-only
```

Das ist eine Schätzung, keine Garantie. Das tatsächlich geladene Modell mit `lms ps`, `nvidia-smi` und echter Inferenz prüfen.

## 8. Modelle laden und entladen

Ein heruntergeladenes Modell laden:

```bash
lms load publisher/model-name --context-length 8192 --gpu max
```

`--gpu max` bittet LM Studio, so viel wie möglich auf die GPU auszulagern. Ohne die Option kann LM Studio automatisch wählen.

Anzeigen, was geladen ist:

```bash
lms ps
```

Ein Modell interaktiv oder per Schlüssel entladen:

```bash
lms unload publisher/model-name
```

Alles entladen:

```bash
lms unload --all
```

Offizielle Referenz:
https://lmstudio.ai/docs/cli/local-models/load

## 9. Die HTTP-API starten

Für Zugriff nur vom Server selbst:

```bash
lms server start --bind 127.0.0.1 --port 1234
```

Prüfen:

```bash
curl -s http://127.0.0.1:1234/v1/models | jq
```

Um auf allen IPv4-Schnittstellen zu lauschen:

```bash
lms server start --bind 0.0.0.0 --port 1234
```

**Sicherheit:** Das Binden an `0.0.0.0` macht den Dienst in allen erreichbaren Netzen verfügbar. Die LM-Studio-Dokumentation empfiehlt Authentifizierung, wenn er über localhost hinaus erreichbar ist. Mindestens Port 1234 per UFW auf vertrauenswürdige LAN-Clients oder ein vertrauenswürdiges Subnetz beschränken. Eine nicht authentifizierte Inferenz-API niemals direkt ins öffentliche Internet stellen.

Beispiel für einen vertrauenswürdigen Client:

```bash
sudo ufw allow from CLIENT_IP to any port 1234 proto tcp
```

Beispiel für ein vertrauenswürdiges privates Subnetz (durch das eigene ersetzen):

```bash
sudo ufw allow from 192.168.1.0/24 to any port 1234 proto tcp
```

Offizielle Server-Referenz:
https://lmstudio.ai/docs/cli/serve/server-start

## 10. Eine Inferenzanfrage testen

Die OpenAI-kompatible API lässt sich mit `curl` testen:

```bash
curl -s http://127.0.0.1:1234/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "publisher/model-name",
    "messages": [
      {"role": "user", "content": "Reply with exactly: Local inference works."}
    ],
    "temperature": 0
  }' | jq
```

Die Modellkennung durch die von `lms ps` oder `/v1/models` angezeigte ersetzen.

## 11. LM Studio beim Booten automatisch starten

LM Studio dokumentiert den Betrieb von llmster als systemd-Startaufgabe. `YOUR_USER`, den Modellschlüssel, die Kontextlänge und die Netzwerkbindung durch eigene Werte ersetzen.

Den Dienst anlegen:

```bash
sudo nano /etc/systemd/system/lmstudio.service
```

Beispiel:

```ini
[Unit]
Description=LM Studio Headless Server
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
User=YOUR_USER
Environment="HOME=/home/YOUR_USER"

ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms daemon up
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms load publisher/model-name --context-length 8192 --gpu max --yes
ExecStart=/home/YOUR_USER/.lmstudio/bin/lms server start --bind 127.0.0.1 --port 1234
ExecStop=/home/YOUR_USER/.lmstudio/bin/lms daemon down

[Install]
WantedBy=multi-user.target
```

Aktivieren und starten:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now lmstudio.service
systemctl status lmstudio.service --no-pager
```

Dann prüfen:

```bash
lms ps
curl -s http://127.0.0.1:1234/v1/models | jq
```

### Falls ein schneller CLI-Start `Text file busy` erzeugt

Auf manchen Systemen kann ein zweiter `lms`-Aufruf direkt nach `lms daemon up` mit dem Daemon-Start kollidieren. Nur wenn dieser Fehler tatsächlich auftritt, eine kurze Pause zwischen den beiden `ExecStartPre`-Zeilen einfügen:

```ini
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms daemon up
ExecStartPre=/bin/sleep 5
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms load ...
```

Keine Workarounds vorsorglich einbauen, wenn der einfachere Dienst funktioniert.

Offizielle Referenz zur Startaufgabe:
https://lmstudio.ai/docs/developer/core/headless_llmster

## 12. Interaktiver Modellumschalter

Das Begleitskript `lmstudio-model-switcher.sh` (vollständiges Listing am Ende dieser Seite) bietet ein einfaches nummeriertes Menü der heruntergeladenen **Modellschlüssel**. Es:

1. stellt sicher, dass llmster läuft;
2. liest die heruntergeladenen Modelle aus `lms ls --json`;
3. fragt, welches Modell verwendet werden soll;
4. entlädt die aktuell geladenen Modelle;
5. lädt das gewählte Modell mit einstellbarer Kontext-/GPU-Vorgabe;
6. lässt den API-Dienst laufen bzw. startet ihn bei Bedarf;
7. zeigt `lms ps` an und prüft `/v1/models`.

Installieren:

```bash
mkdir -p ~/bin
cp lmstudio-model-switcher.sh ~/bin/
chmod +x ~/bin/lmstudio-model-switcher.sh
```

Mit den konservativen Standardwerten ausführen (8192 Token Kontext, GPU max, API auf localhost):

```bash
~/bin/lmstudio-model-switcher.sh
```

Einstellungen für einen einzelnen Aufruf überschreiben:

```bash
LMS_CONTEXT_LENGTH=16384 \
LMS_GPU=max \
LMS_BIND=0.0.0.0 \
LMS_PORT=1234 \
~/bin/lmstudio-model-switcher.sh
```

### Wichtig: Modellvarianten / Quantisierungen

LM Studio unterscheidet zwischen einem **logischen Modellschlüssel** und den heruntergeladenen Quantisierungsvarianten. Das Herunterladen von `publisher/model@q4_k_m` legt fest, welche Quantisierung gespeichert wird, während beim Laden üblicherweise der von `lms ls` gemeldete logische Modellschlüssel verwendet wird.

Wer mehrere Quantisierungen desselben logischen Modells vorhält, kann sie so ansehen:

```bash
lms ls --variants
lms ls --json | jq
```

Das Verhalten bei der Variantenauswahl hat sich zwischen LM-Studio-CLI-Versionen geändert; daher vor dem Automatisieren eines Quantisierungswechsels `lms get --help` / `lms load --help` der installierten Version prüfen. Das mitgelieferte Skript wechselt bewusst **Modellschlüssel** – das ist für eine allgemeine Anleitung die stabile und verständliche Operation.

## 13. Das Standardmodell beim Booten ändern

Der interaktive Umschalter ändert, was **jetzt** geladen ist. Lädt der systemd-Dienst beim Booten ausdrücklich ein Modell, wird nach einem Neustart wieder das in `/etc/systemd/system/lmstudio.service` eingetragene Modell geladen.

Um ein anderes Modell dauerhaft zum Boot-Standard zu machen:

```bash
sudo nano /etc/systemd/system/lmstudio.service
```

Die Zeile `lms load ...` ändern, dann:

```bash
sudo systemctl daemon-reload
sudo systemctl restart lmstudio.service
```

Eine fortgeschrittenere Einrichtung kann JIT-Modellladen verwenden, statt beim Booten ein festes Modell zu laden. LM Studio unterstützt JIT-Laden und automatisches Entladen; das ist nützlich, wenn Anwendungen Modelle dynamisch wählen.

## 14. Den Server überwachen

Geladene Modelle:

```bash
lms ps
```

GPU-Momentaufnahme:

```bash
nvidia-smi
```

Alle zwei Sekunden aktualisieren:

```bash
watch -n 2 nvidia-smi
```

CPU/RAM:

```bash
htop
```

Interaktiver GPU-Monitor:

```bash
nvtop
```

LM-Studio-Logs lassen sich auch über die CLI streamen:

```bash
lms log stream
```

## 15. Ein praktischer Ablauf zur Modellwahl

Ein sinnvoller Weg zur Modellwahl:

1. Zuerst die tatsächliche Arbeitslast festlegen.
2. Modelle mit der nötigen Fähigkeit ermitteln (zum Beispiel Tool-Nutzung, Programmieren, Bildverständnis oder Schlussfolgern).
3. Wenn verfügbar, zuerst eine Variante der Q4-Klasse herunterladen.
4. Bei knappem Speicher vor dem Laden `--estimate-only` verwenden.
5. Echte Prompts und Tool-Aufrufe testen.
6. Nur zu Q5/Q6 wechseln, wenn Q4 spürbar an Zuverlässigkeit oder Qualität verliert.
7. Nur zu Q8 wechseln, wenn die Verbesserung sichtbar ist und den zusätzlichen RAM/VRAM wert ist.
8. Reicht selbst Q8 eines kleinen Modells nicht aus, ein **größeres/besseres Modell mit moderater Quantisierung** testen, statt anzunehmen, dass noch mehr Präzision das Problem löst.

So bleibt die Modellwahl empirisch statt zahlengetrieben: **Das beste Modell ist die kleinste/schnellste Konfiguration, die die Anforderungen der Anwendung zuverlässig erfüllt.**

## 16. Checkliste zur Fehlersuche

Verhält sich LM Studio nicht wie erwartet, in dieser Reihenfolge prüfen:

```bash
lms --version
lms daemon up
lms ls
lms ps
lms server status
nvidia-smi
free -h
```

Bei einem Modell, das möglicherweise nicht passt, zuerst abschätzen:

```bash
lms load publisher/model-name --context-length 8192 --gpu max --estimate-only
```

Kann sich ein entfernter Client nicht verbinden, zuerst lokal prüfen:

```bash
curl -s http://127.0.0.1:1234/v1/models | jq
```

Dann prüfen, ob der Server auf der vorgesehenen Adresse und dem vorgesehenen Port lauscht:

```bash
ss -ltnp | grep 1234
sudo ufw status numbered
```

Funktioniert der lokale Zugriff, der entfernte aber nicht, Bind-Adresse, Firewall-Regeln, Routing und API-Authentifizierung untersuchen, bevor das Modell gewechselt wird.

## 17. Was diese Anleitung bewusst offen lässt

Sie schreibt kein universelles Modell vor. Hardware, Arbeitslast, Kontextlänge, Modellarchitektur und benötigte Fähigkeiten unterscheiden sich dafür zu stark. Sie erfordert außerdem kein separates CUDA Toolkit, kein Docker, keine Desktop-GUI und keine anwendungsspezifischen Integrationen.

Sobald der Server stabil läuft, können anwendungsspezifische Anleitungen ihr eigenes Modell, ihren Kontext, API-Endpunkt, ihre Authentifizierung und Benchmark-Anforderungen festlegen.

---

## Offizielle Quellen

- LM Studio CLI: https://lmstudio.ai/docs/cli
- Headless / llmster: https://lmstudio.ai/docs/developer/core/headless
- Linux-Startaufgabe: https://lmstudio.ai/docs/developer/core/headless_llmster
- Modelle herunterladen (`lms get`): https://lmstudio.ai/docs/cli/local-models/get
- Modelle laden/entladen (`lms load`): https://lmstudio.ai/docs/cli/local-models/load
- API-Server starten: https://lmstudio.ai/docs/cli/serve/server-start
- Überblick Modell-Download/Quantisierung: https://lmstudio.ai/docs/app/basics/download-model
- REST-API-Schnellstart: https://lmstudio.ai/docs/developer/rest/quickstart

---

## Anhang: `lmstudio-model-switcher.sh`

Als `lmstudio-model-switcher.sh` speichern und dann wie in Teil 2 § 12 beschrieben installieren.

```bash
#!/usr/bin/env bash
set -euo pipefail

# LM Studio interactive model switcher
# Optional environment overrides:
#   LMS_CONTEXT_LENGTH=16384 LMS_GPU=max LMS_BIND=0.0.0.0 LMS_PORT=1234 ./lmstudio-model-switcher.sh

CONTEXT_LENGTH="${LMS_CONTEXT_LENGTH:-8192}"
GPU="${LMS_GPU:-max}"
BIND="${LMS_BIND:-127.0.0.1}"
PORT="${LMS_PORT:-1234}"

need() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Error: '$1' is required but was not found in PATH." >&2
    exit 1
  }
}

need lms
need jq

# Make sure the headless daemon is available.
lms daemon up >/dev/null

mapfile -t MODELS < <(
  lms ls --json | jq -r '
    if type == "array" then .[]
    elif has("models") then .models[]
    else empty end
    | .modelKey // .model_key // empty
  ' | awk 'NF' | sort -u
)

if ((${#MODELS[@]} == 0)); then
  echo "No downloaded models were found."
  echo "Download one first, for example: lms get <publisher/model>"
  exit 1
fi

echo
echo "Downloaded LM Studio models"
echo "---------------------------"
for i in "${!MODELS[@]}"; do
  printf '  %2d) %s\n' "$((i + 1))" "${MODELS[$i]}"
done
echo "   0) Cancel"
echo

while true; do
  read -r -p "Choose a model [0-${#MODELS[@]}]: " CHOICE
  [[ "$CHOICE" =~ ^[0-9]+$ ]] || { echo "Please enter a number."; continue; }
  (( CHOICE == 0 )) && exit 0
  (( CHOICE >= 1 && CHOICE <= ${#MODELS[@]} )) && break
  echo "Choice out of range."
done

MODEL="${MODELS[$((CHOICE - 1))]}"

echo
echo "Selected: $MODEL"
echo "Context : $CONTEXT_LENGTH"
echo "GPU     : $GPU"
echo

read -r -p "Unload the current model and load this one? [y/N]: " CONFIRM
[[ "$CONFIRM" =~ ^[Yy]$ ]] || { echo "Cancelled."; exit 0; }

echo "Unloading currently loaded models..."
lms unload --all || true

echo "Loading $MODEL..."
lms load "$MODEL" --context-length "$CONTEXT_LENGTH" --gpu "$GPU" --yes

# Start the HTTP server only if it is not already running.
if ! lms server status >/dev/null 2>&1; then
  echo "Starting LM Studio server on $BIND:$PORT..."
  lms server start --bind "$BIND" --port "$PORT"
fi

echo
echo "Current LM Studio state:"
lms ps

echo
echo "API model list:"
if command -v curl >/dev/null 2>&1; then
  curl -fsS "http://127.0.0.1:${PORT}/v1/models" | jq . || \
    echo "API check failed. If the server uses another port, set LMS_PORT accordingly."
else
  echo "curl is not installed; skipping API check."
fi
```
