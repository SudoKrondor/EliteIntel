# Reiter Bindings

<img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> Elite Intel bedient
dein Schiff, indem es die Tasten drückt, auf die Elite Dangerous belegt ist. Hat eine Steuerung
keine Tastaturbelegung, kann Elite Intel sie nicht nutzen — hier findest du das heraus und
behebst es.

Zwei Unterreiter: **Bindungsprofil** und **Bindungsverwaltung**.

---

## Bindungsprofil

![Bindungsprofil](images/ui-tab-bindings-profile.png)

### Welche Datei verwendet wird

**Belegungsordner** — optional. Leer gelassen wird der Standardort von Elite Dangerous
verwendet; nutze die **⋮**-Auswahl, wenn deine Installation woanders liegt. Ein unbrauchbarer
Ordner wird abgelehnt und die vorherige Einstellung bleibt.

**Profil** — automatisch erkannt. Elite Intel liest den aktiven `StartPreset`-Eintrag und greift
notfalls auf die neueste `.binds`-Datei zurück.

**Datei** — die `.binds`-Datei, die gerade für Diagnose und Zuweisung verwendet wird.

Profil und Datei tragen je ein **ⓘ**, das genau erklärt, wie der Wert gewählt wurde.

> **„Keine Belegungen gefunden“ trotz richtigem Ordner?** Elite Dangerous schreibt erst dann eine
> `.binds`-Datei, wenn du etwas anpasst. Öffne im Spiel *Optionen → Steuerung*, ändere irgendeine
> Belegung, und Elite Intel findet die Datei.

### Tempo der Tastatureingabe

Ein Schieberegler **Schnell ↔ Langsam** für die Pause, die Elite Intel nach jedem an das Spiel
gesendeten Tastendruck hält. Schnell ist der Standard. Verschluckt das Spiel auf langsamerer
Hardware Tastendrücke aus einer Folge — ein Makro läuft nur halb, ein Panel öffnet auf dem
falschen Reiter —, schiebe ihn Richtung **Langsam**.

### Die Belegungstabellen

Zwei Reiter: **Verwendete Belegungen** und **Fehlende Belegungen**, jeweils mit Anzahl. Die
Zeilen sind unter den Überschriften des Spiels gruppiert — **Allgemeine Steuerung**,
**Schiffsteuerung**, **SRV-Steuerung**, **Steuerung zu Fuß**, **Sonstige Steuerung** — in derselben
Reihenfolge wie im Steuerungsbildschirm des Spiels, damit du beide nebeneinander lesen kannst.

**Suche** grenzt beide Tabellen beim Tippen ein. Sie durchsucht Bereich, Gruppe,
Steuerungsname und den rohen `.binds`-Tag. **Nur Konflikte anzeigen** filtert auf die Probleme.

| Spalte | Bedeutung |
|--------|---------|
| **Belegung** | Die Steuerung |
| **Primär** / **Sekundär** | Die zwei Plätze, die Elite Dangerous jeder Steuerung gibt |
| **Status** | `Fehlt` · `Keine Tastaturbelegung` (belegt, aber nur auf einem Controller) · `Nicht definiert` |
| **Schnellkorrektur** | Reiter *Fehlende*: weist dieser einen Steuerung eine sichere freie Tastaturtaste zu |
| **Löschen** | Reiter *Verwendete*: entfernt die Tastaturbelegung (Primär, Sekundär oder beide); Controller- und HOTAS-Belegungen bleiben unberührt |

> **HOTAS und Controller werden angezeigt, sind aber nicht bearbeitbar.** Elite Intel führt über
> Tastaturbelegungen aus, andere Geräte erscheinen nur zur Diagnose.

### Konflikte

Elite Dangerous wertet eine Tastenkombination nur dann als Konflikt, wenn sie *exakt* gleich ist
— `G` und `Shift+G` vertragen sich problemlos. Elite Intel nutzt dieselbe Regel und meldet also,
was auch das Spiel meldet.

Zeilen mit Konflikt sind rot, und beim Überfahren zeigt eine Zeile **Teilt *Taste* mit:** und
die Liste — für jeden Platz mit Konflikt, nicht nur den ersten.

Du siehst vielleicht auch **Schiff/SRV-Gegenstück - viele belegen es gleich wie:** auf einer
cyanfarbenen Zeile. Das ist kein Konflikt, sondern ein Vorschlag: einige Schiffs- und
SRV-Steuerungen werden üblicherweise auf dieselbe Taste gelegt.

Vega **spricht** außerdem über Belegungen, die tatsächlich etwas kaputt machen, und nennt die
Tasten im Diagnoseprotokoll:

- **Bewegung auf der Galaxiekarte und Navigation in der Oberfläche auf derselben Taste.** Das
  Plotten von Routen funktioniert erst, wenn Karte und Oberfläche getrennte Tasten haben.
- **Eine Steuerung auf deiner Spielmenü-Taste.** Elite öffnet das Spielmenü bei jeder
  Kombination, die auf diese Taste endet, also lässt sich die Steuerung nie drücken. Die Lösung:
  die Spielmenü-Belegung im Spiel löschen — Escape öffnet das Menü ohnehin.
- **Eine Steuerung auf einer Kombination, die das Betriebssystem zuerst abfängt** (etwa
  Alt+F4). Sie zu drücken schließt das Spiel oder beendet deine Sitzung.

### Eine Belegung bearbeiten

Klicke auf einen Platz, um den Zuweisungsdialog zu öffnen.

![Taste zuweisen](images/ui-bindings-assign.png)

Er zeigt die gewählte Belegung, den Platz und den aktuellen Wert. Dann **klicke in das Feld und
drücke die gewünschten Tasten** — Modifikatoren und Taste zusammen. Esc bricht ab.
Kombinationen mit bis zu drei Modifikatoren werden unterstützt.

Eine Live-Tastaturkarte zeigt, was frei ist: **Halte Strg/Shift/Alt, um die für diese
Kombination freien Tasten zu sehen — grün ist frei, rot ist belegt.** Reservierte Tasten (die
Spielmenü-Taste, Alt+F4, unter Linux Strg+Alt+F-Tasten) sind markiert und können nicht
zugewiesen werden.

**Belegung löschen** entfernt die Zuweisung.

### Fehlende Belegungen automatisch zuweisen

Ein Knopf, der **jeder** Steuerung ohne Tastaturbelegung sichere, layoutfreundliche
Tastaturtasten zuweist.

- Bestehende Belegungen werden nie geändert.
- Keine Taste wird doppelt vergeben.
- Die Änderungen landen **nur im Entwurf** — prüfe sie, dann Anwenden.

Er meldet, was er getan hat und was er übersprungen hat und warum: beide Plätze schon auf einem
Controller, keine sichere Taste mehr frei oder kein sicher bearbeitbarer Platz. Zwei Steuerungen
bleiben **absichtlich unbelegt**: das Spielmenü (Escape öffnet es bereits) und *gesamte Fracht
abwerfen* (es leert den Laderaum ins All, und kein Elite-Intel-Befehl drückt es — belege es von
Hand, wenn du es willst).

### Entwurf, Anwenden, Zurück

Änderungen gehen **nicht** direkt an Elite Dangerous. Sie sammeln sich in einem Entwurf, und
das Abzeichen zeigt **Entwurf** oder **Synchron**. Derselbe Zustand erscheint in der Anzeige
*Tasten* auf dem Vega-Reiter.

| Knopf | Was er tut |
|--------|--------------|
| **Anwenden** | Schreibt den Entwurf in deine `.binds`-Datei und sichert vorher eine Kopie der alten |
| **Zurück** | Verwirft den Entwurf und lädt neu aus der Spieldatei |

> **Öffne und schließe nach dem Anwenden den Steuerungsbildschirm in Elite Dangerous.** Das Spiel
> liest seine Belegungen nur beim Öffnen dieses Bildschirms neu ein. Vega sagt das auch laut.

Wurde die Belegungsdatei des Spiels geändert, nachdem dein Entwurf entstanden ist, verweigert
Anwenden und bittet dich, zuerst neu zu laden oder zu verwerfen, statt die Änderung eines anderen
stillschweigend zu überschreiben.

Schließt du die App mit einem nicht angewendeten Entwurf, wirst du gefragt: **Auf Spiel
anwenden**, **Entwurf behalten** oder **Verwerfen**.

---

## Bindungsverwaltung

![Bindungsverwaltung](images/ui-tab-bindings-management.png)

**Spieler-Backups** — Schnappschüsse, die du selbst mit **Jetzt sichern** anlegst, aufgelistet
nach Datum **Erstellt** und den **Dateien**, die jeder enthält. Leg einen an, bevor du
experimentierst.

(Unabhängig davon speichert jedes **Anwenden** vorher still eine Kopie der Spieldatei in
`elite-intel/bindings/backups/`. Das ist ein Sicherheitsnetz und wird hier nicht aufgelistet.)

| Knopf | Was er tut |
|--------|--------------|
| **In Entwurf wiederherstellen** | Lädt die Sicherung in deinen Entwurf, damit du sie prüfen kannst, bevor sie das Spiel berührt |
| **Live wiederherstellen** | Lädt sie und wendet sie direkt im Spiel an. Die üblichen Sicherheitsprüfungen laufen trotzdem |
| **Sicherung löschen** | Löscht die Sicherung endgültig |

Jede dieser Aktionen fragt vorher nach. Beide Wiederherstellungen ersetzen ungespeicherte
Änderungen im aktuellen Entwurf.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
