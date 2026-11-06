# BindForge — der Reiter Bindings

<img src="images/keyboard.png" class="inline" height="20" alt="Bindings"> **BindForge** bearbeitet
und schützt deine Steuerung für Elite Dangerous von außerhalb des Spiels: Tastatur, Maus, Joystick,
HOTAS und Pedale. Es kümmert sich um alle Dateien dahinter — deine `.binds`-Dateien, die Preset-Datei,
die festlegt, welche davon geladen wird, sowie die Dateien `DeviceMappings.xml` und `.buttonMap`, die
deine Controller benennen und ihre Tasten beschriften.

Es liegt im Reiter **Bindings**, der drei Unterreiter und darüber eine Leiste hat:

| Teil | Wofür |
|------|-------|
| **[Anwenden-Leiste](#the-apply-bar)** | Das eine **Anwenden** und **Verwerfen** für alles in diesem Reiter |
| **[Bindungsprofil](#binding-profile)** | Der Bind Editor. Jede Steuerung des Spiels, mit den Ansichten **Spielmodus**, **Anomalien** und **Einstellungen** |
| **[Bindungsverwaltung](#binding-management)** | Der File Manager. Deine Spielinstallationen, deine Belegungsdateien und deine Sicherungen |
| **[Alias-Designer](#alias-designer)** | Gib deinen Controllern echte Namen und beschrifte ihre Tasten, Achsen und Hats |

> **Vega drückt weiterhin nur Tastaturtasten.** Du kannst jetzt jedes Gerät auf jeden Slot legen,
> aber damit Vega eine Steuerung bedienen kann, braucht sie eine Tastaturtaste. Die Spalte **KI-Zugriff**
> zeigt dir, bei welchen das so ist.

---

## Beim ersten Öffnen

Bevor BindForge etwas ändert, richtet es sich einmal ein. **Vorher wird alles gesichert, und diese
Sicherung wird für immer aufbewahrt.**

1. **Installationen bestätigen.** BindForge listet jede Kopie von Elite Dangerous auf, die es auf
   diesem PC gefunden hat. Korrigiere die Liste mit **Fehlende hinzufügen** oder **Entfernen** und
   drücke dann **Bestätigen**.
2. **Abgleichen, falls nötig.** Enthalten deine Installationen unterschiedliche Gerätedateien, nimm
   die Dateien einer Installation für alle, oder wähle **Zusammenführen** und beantworte jede Zeile,
   die mit *wählen* markiert ist.
3. **Arbeitskopie erstellen.** BindForge legt eine eigene Kopie deiner Steuerungsdateien an. Diese
   Kopie bearbeitest du, und die Dateien des Spiels bleiben unberührt, bis du **Anwenden** drückst.
4. **Controller benennen.** Jeder angeschlossene Controller ohne Namen bekommt das Angebot für
   [einen Namen](#naming-a-controller). Du kannst das **Überspringen**.

Bis du fertig bist, wird die Ersteinrichtung bei jedem Öffnen des Reiters erneut angeboten.

### Neu in Elite Dangerous?

Ein ganz neuer Kommandant hat noch gar keine Steuerungsdateien, und Vega sagt dir das. Schließe dein
Kommandantentraining ab und stelle dann im Spiel unter *Optionen → Steuerung* die UI-Bewegung der
Galaxiekarte auf die Pfeiltasten und übernimm das. Dadurch entstehen die Dateien, und Vega kann sie
lesen.

Hast du die Dateien des Spiels, aber noch keine eigene Belegungsdatei, erscheint in der Anwenden-Leiste
ein Knopf **Erstellen**, und die Editor-Reiter bleiben ausgegraut. Wähle unter **Ausgehend von** ein
Preset von Frontier, gib deinem einen **Namen** und drücke **Anwenden**, um es ins Spiel zu schreiben.
Bist du ein zurückkehrender Spieler, stelle stattdessen eine Sicherung aus den
[Spielersicherungen](#player-backups) wieder her.

---

<a id="the-apply-bar"></a>

## Die Anwenden-Leiste

Nichts, was du in diesen Reitern tust, berührt das Spiel, bevor du **Anwenden** drückst. Deine
Änderungen sammeln sich, und **Anwenden** und **Verwerfen** leuchten auf, sobald es welche gibt.

| Knopf | Was er tut |
|-------|------------|
| **Anwenden** | Sichert alles, schreibt dann deine Änderungen in die Dateien des Spiels und meldet, was pro Installation geschrieben wurde |
| **Verwerfen** | Wirft alle nicht angewendeten Änderungen weg. Die Dateien des Spiels werden nicht berührt |

> **Schließe den Steuerungsbildschirm des Spiels, bevor du anwendest.** Änderungen, die gleichzeitig
> in BindForge und im Spiel gemacht werden, können verloren gehen. Damit das Spiel eine geänderte
> Belegungsdatei lädt, öffne und schließe nach dem Anwenden seinen Steuerungsbildschirm. Vega erinnert
> dich daran.

Verlässt du den Reiter oder schließt die App mit nicht angewendeten Änderungen, wirst du gefragt:
**Anwenden**, **Verwerfen** oder **Zurück**.

### Wenn das Spiel deine Dateien geändert hat

Das Spiel kann seine eigenen Dateien ändern, und ein Spiel-Update kann sie zurücksetzen. BindForge
vergleicht sie mit seiner Arbeitskopie, wenn du den Reiter in einer Sitzung zum ersten Mal öffnest, und
erneut bei jedem Anwenden. Weichen sie ab, listet es auf, was sich geändert hat, und lässt dich wählen:

| Auswahl | Was passiert |
|---------|--------------|
| **Meine zurückschreiben** | Deine Arbeitskopie geht zurück ins Spiel. Meist richtig, wenn eine Datei nach einem Update zurückgesetzt aussieht |
| **Zusammenführen...** | Jede abweichende Zeile wird eine Reihe. Klicke auf den Wert, den du behalten willst, deinen oder den des Spiels |
| **Dateien des Spiels übernehmen** | Die Dateien des Spiels ersetzen deine Arbeitskopie |

Vor jedem dieser Schreibvorgänge wird alles gesichert, und du kannst die Warnung schließen, um später
zu entscheiden. Schließt du sie während eines Anwendens, wird es abgebrochen und deine Änderungen
bleiben erhalten.

**Eine neue Version deiner Belegungsdatei.** Spiel-Updates speichern deine Belegungsdatei manchmal
unter neuem Namen mit neuen Steuerungen darin. BindForge schreibt deine alte Datei nie darüber. Wurden
alle deine Belegungen übernommen, musst du nur **Zustimmen**. Weichen einige ab, behält **Abgleichen...**
die neuen Steuerungen und fragt bei jeder abweichenden Zeile nach.

---

<a id="binding-profile"></a>

## Bindungsprofil

![Bindungsprofil](images/ui-tab-bindings-profile.png)

Das ist der Bind Editor. Die Zeile **Bearbeitet** oben nennt die Belegungsdatei, die du gerade siehst.
Um eine andere zu öffnen, nutze [Belegungsdatei](#bindings-file) unter *Bindungsverwaltung*.

**Tempo der Tastatureingabe** ist ein Regler **Schnell ↔ Langsam** für die Pause, die Elite Intel nach
jedem an das Spiel gesendeten Tastendruck hält. Verschluckt das Spiel auf langsamerer Hardware
Tastendrücke — ein Makro läuft nur halb, ein Panel öffnet im falschen Reiter —, schiebe ihn Richtung
**Langsam**.

**Bindungen suchen...** am unteren Rand funktioniert in allen drei Ansichten. Es durchsucht Abschnitt,
Gruppe, Steuerungsname und rohen `.binds`-Tag sowie die Controller- und Tastennamen einer Belegung.

### Spielmodus

Jede Steuerung, gruppiert unter den Überschriften des Spiels und in derselben Reihenfolge wie sein
Steuerungsbildschirm, sodass du beide nebeneinander lesen kannst. Reiter oben schränken die Liste auf
**Alle**, **Allgemein**, **Schiff**, **SRV** oder **Zu Fuß** ein, jeweils mit Anzahl. Darunter liegen
zwei Listen: **Verwendete Belegungen** und **Fehlende Belegungen**.

| Spalte | Bedeutung |
|--------|-----------|
| **Belegung** | Die Steuerung |
| **Primär** / **Sekundär** | Die zwei Slots, die Elite Dangerous jeder Steuerung gibt, mit deinen eigenen Namen für Controller und Tasten |
| **KI-Zugriff** | Ob Vega diese Steuerung bedienen kann (siehe unten) |
| **Status** | `Fehlt` · `Keine Tastaturbelegung` (belegt, aber nur auf einem Controller) · `Nicht definiert` |
| **Schnellkorrektur** | Liste *Fehlende*: gibt dieser einen Steuerung eine sichere freie Tastaturtaste |
| **Löschen** | Liste *Verwendete*: entfernt die Tastaturbelegung; Controller- und HOTAS-Belegungen bleiben |

| KI-Zugriff | Bedeutung |
|------------|-----------|
| **Von Vega genutzt** | Vega drückt diese Steuerung mit ihrer Tastaturtaste. Lass eine Taste darauf |
| **Für Vega unerreichbar** | Vega braucht diese Steuerung, aber sie hat keine Tastaturtaste. Lege eine auf den leeren Slot — deine Controller-Belegung funktioniert weiter |
| **Vega braucht eine Taste** | Vega braucht diese Steuerung, und nichts ist darauf gelegt |

Neben der Suche liegen die beiden Filter und der Knopf für die automatische Zuweisung:

- **Nur Konflikte anzeigen** — nur die Zeilen mit Konflikt.
- **Nur von Vega benötigte anzeigen** — nur die Steuerungen, die Vega drückt, für ihre integrierten
  Befehle und deine eigenen Befehle.
- **[VEGA-Pflichtbelegungen automatisch zuweisen](#auto-assign-vega-required-binds)** — siehe unten.

### Eine Taste zuweisen

Klicke auf einen der beiden Slots einer Zeile, um **Steuerung zuweisen** zu öffnen. Der Dialog zeigt
beide Slots als Karten, und die hervorgehobene Karte bekommt deine nächste Eingabe. Klicke auf die
andere Karte, um stattdessen diese zu füllen, oder tausche Primär und Sekundär mit dem Knopf zwischen
ihnen.

![Steuerung zuweisen](images/ui-bindings-assign.png)

Unter den Karten gibt es einen Reiter **Tastatur** und je einen für jeden angeschlossenen Controller.

- **Tastatur** — klicke in das Feld und drücke die Tasten zusammen mit Modifikatoren, bis zu drei. Esc
  bricht ab. Halte Strg, Umschalt oder Alt, um die Tastaturkarte für diese Kombination zu sehen:
  **Grün ist frei, Rot ist belegt.** Reservierte Tasten, etwa die Spielmenü-Taste und Alt+F4, lassen
  sich nicht zuweisen.
- **Ein Controller** — drücke eine Taste oder einen Hat am Controller, oder klicke in der Liste
  **Tasten** oder **Hats** darauf. Bereits gehaltene Tasten werden zu Modifikatoren. Grün ist frei, Rot
  ist von einer anderen Steuerung belegt, Blau ist, was diese Steuerung jetzt hält. Ein Controller
  braucht zuerst einen Namen aus dem [Alias-Designer](#alias-designer).

Eine Eingabe, die schon eine andere Steuerung hält, lässt sich nicht zuweisen. Die Karte nennt diese
Steuerung, und **Speichern** bleibt gedimmt, bis du sie dort freigibst. Würde eine Änderung einer von
Vega genutzten Steuerung die letzte Tastaturtaste nehmen, warnt dich der Dialog. **Speichern** behält
deine Änderungen, bereit für **Anwenden**.

> **Gamepads werden bisher nur teilweise unterstützt.** Gamepad-Sticks lassen sich hier noch nicht
> zuweisen.

### Eine Achse zuweisen

Achsensteuerungen wie Nicken, Gieren und Schub stehen zwischen den anderen Zeilen und öffnen einen
eigenen Dialog.

![Achse zuweisen](images/ui-bindings-axis.png)

Bewege eine Achse am Controller oder klicke in der Liste auf eine. Stelle dann **Richtung** (*Normal*
oder *Invertiert*), **Totzone** und, bei den Steuerungen, die das haben, **Schubbereich** (*Voller
Bereich* oder *Nur vorwärts*) ein. Mehrere Steuerungen können sich eine Achse teilen, und das ist
normal: Der Dialog sagt dir, was noch darauf liegt, und du entscheidest.

### Anomalien

![Anomalien](images/ui-bindings-anomalies.png)

Die Zeilen mit Konflikt an einem Ort gesammelt, mit ihrer Anzahl auf dem Reiter. Es sind dieselben
Zeilen, die der Spielmodus rot zeichnet, und ein Klick öffnet denselben Dialog. **Warnt dich Vega vor
einem Belegungskonflikt, schau hier nach.**

Elite Dangerous sieht einen Konflikt nur, wenn eine Kombination *exakt* gleich ist — `G` und
`Umschalt+G` vertragen sich problemlos. Elite Intel folgt derselben Regel. Steuerungen, die nie
gleichzeitig aktiv sein können, etwa eine Schiffs- und eine SRV-Steuerung, werden nicht gemeldet. Fahre
über eine Zeile, um **Teilt *Taste* mit:** und die Liste zu sehen, für jeden Slot mit Konflikt.

Du siehst vielleicht auch **Schiff/SRV-Gegenstück - viele belegen es gleich wie:** auf einer Zeile. Das
ist ein Vorschlag, kein Konflikt: Manche Schiffs- und SRV-Steuerungen liegen üblicherweise auf derselben
Taste.

Vega **spricht** außerdem über Belegungen, die wirklich etwas kaputt machen, und nennt die Tasten im
Diagnoseprotokoll:

- **Bewegung auf der Galaxiekarte und Oberflächennavigation auf derselben Taste.** Routenplanung kann
  nicht funktionieren, bis Karte und Oberfläche getrennte Tasten haben.
- **Eine Steuerung auf deiner Spielmenü-Taste.** Elite öffnet das Spielmenü bei jeder Kombination, die
  auf dieser Taste endet, sodass die Steuerung nie gedrückt werden kann. Die Lösung: die
  Spielmenü-Belegung im Spiel löschen — Escape öffnet dieses Menü sowieso.
- **Eine Steuerung auf einer Kombination, die das Betriebssystem zuerst nimmt** (etwa Alt+F4). Sie zu
  drücken schließt das Spiel oder beendet deine Sitzung.

### Einstellungen

![Einstellungen der Belegungen](images/ui-bindings-settings.png)

Die Empfindlichkeiten, Totzonen und Kurven deiner Belegungsdatei, gruppiert wie im Spiel. Jede hat einen
Regler und daneben eine Zahl. Ziehe den Regler, oder tippe eine Zahl und drücke Enter.

Der Regler deckt denselben Bereich ab wie der Regler des Spiels. Die Zahl ist der genaue Wert und der
einzige Weg über die Grenzen des Reglers hinaus. Ein Wert, der bereits außerhalb liegt, bleibt, bis du
ihn bewegst.

<a id="auto-assign-vega-required-binds"></a>

### VEGA-Pflichtbelegungen automatisch zuweisen

Ein Knopf, der jeder Steuerung, die **Vega drückt** und die keine Taste hat, eine sichere,
layoutfreundliche Tastaturtaste gibt, auch den Steuerungen, die deine eigenen Befehle nutzen. Andere
Steuerungen bleiben, wie sie sind.

- Bestehende Belegungen auf jedem Gerät werden nie geändert. Nur ein leerer Slot wird gefüllt.
- Keine Taste wird doppelt vergeben.
- Die Änderungen gehen in deine Bearbeitung, sodass du sie vor dem **Anwenden** prüfen kannst.

Es meldet, was es getan hat, was es übersprungen hat und warum: beide Slots schon auf einem Controller,
keine freie sichere Taste mehr, oder kein sicher bearbeitbarer Slot. Zwei Steuerungen bleiben **absichtlich
unbelegt**: das Spielmenü (Escape öffnet es schon) und *gesamte Fracht abwerfen* (es leert den Laderaum
ins All, und kein Befehl von Elite Intel drückt es).

---

<a id="binding-management"></a>

## Bindungsverwaltung

Der File Manager, mit zwei eigenen Reitern: **Spielinstallationen** und **Spielersicherungen**.

### Spielinstallationen

![Spielinstallationen](images/ui-bindings-install-locations.png)

**Benutzerkonfigurationsordner** ist der Ort deiner `.binds`-Dateien und der Preset-Datei, und alle
Installationen teilen ihn sich. Er wird automatisch erkannt. Nutze die Auswahl **⋮**, falls deiner an
einem ungewöhnlichen Ort liegt.

> **„Belegungen nicht gefunden" trotz richtigem Ordner?** Elite Dangerous schreibt erst dann eine
> `.binds`-Datei, wenn du etwas angepasst hast. Öffne im Spiel *Optionen → Steuerung*, ändere eine
> beliebige Belegung, und Elite Intel findet die Datei.

<a id="bindings-file"></a>

**Belegungsdatei** listet deine Belegungsdateien einschließlich nicht angewendeter Änderungen. Wähle eine
und drücke **Laden**, um sie im Bindungsprofil zu öffnen, oder tippe einen neuen Namen und drücke
**Umbenennen**. Eine Umbenennung ist eine Änderung wie jede andere und erreicht das Spiel, wenn du
**Anwenden** drückst.

**Spielinstallationen** listet jede Kopie des Spiels, die BindForge kennt. Jede hat ihre eigenen
Dateien `DeviceMappings.xml` und `.buttonMap`, und BindForge hält sie identisch.

| Spalte | Bedeutung |
|--------|-----------|
| **Vertrieb** | Woher diese Kopie stammt: Steam, Epic, Frontier oder Oculus |
| **Speicherort** | Ihr Ordner |
| **Quelle** | *Erkannt* oder *Manuell hinzugefügt* |
| **Gerätedateien** | Wie viele Geräteeinträge und Button-Maps sie enthält und wie viele davon verwaist sind |
| **Status** | *Stimmt überein* mit deiner Arbeitskopie, *Weicht ab* oder *Nicht gefunden* |

**Fehlende hinzufügen** ergänzt eine Installation, die BindForge nicht erkannt hat. **Entfernen** sorgt
dafür, dass BindForge eine nicht mehr sichert oder beschreibt. Verschiebst du das Spiel, drücke
**Entfernen** und dann **Fehlende hinzufügen**.

<a id="player-backups"></a>

### Spielersicherungen

![Spielersicherungen](images/ui-tab-bindings-management.png)

Deine Steuerung zu schützen ist der Hauptgrund, warum es BindForge gibt. Eine aufwendige Einrichtung
kostet Stunden, und ein Spiel-Update kann sie zurücksetzen. BindForge sichert alles von selbst, und jede
Sicherung ist eine datierte `.zip` mit deinen Belegungsdateien, der Preset-Datei und den Gerätedateien
aus jeder Installation.

Die Spalte **Anlass** sagt, warum es jede Sicherung gibt, und davon hängt ab, wie lange sie bleibt:

| Anlass | Wann | Aufbewahrung |
|--------|------|--------------|
| **Täglich** | Beim ersten Start von Elite Intel an jedem Tag | Nach Alter: standardmäßig 30 Tage, oder 60, 90 oder 180 |
| **Vor APPLY**, **Vor RESTORE**, **Vor der Prüfung**, **Vor Änderungen** | Automatisch, bevor BindForge etwas schreibt | Die neuesten 100 als Standard, bis zu 1000 |
| **Benutzer** | Wenn du **Jetzt sichern** drückst | Bis du sie löschst |
| **Ersteinrichtung** | Vor der Ersteinrichtung | Für immer |

| Knopf | Was er tut |
|-------|------------|
| **Jetzt sichern** | Selbst eine Sicherung anlegen. Tu das, bevor du zu experimentieren beginnst |
| **Wiederherstellen** | Spielt die gewählte Sicherung zurück: jede Belegungsdatei, die Preset-Datei und die Gerätedateien in jeder Installation. Deine aktuellen Dateien werden vorher gesichert |
| **Sicherungsdatei suchen** | Stellt eine Sicherung wieder her, die nicht in der Liste steht, etwa von einem anderen Rechner |
| **Löschen** | Löscht eine deiner eigenen *Benutzer*-Sicherungen endgültig |

> **Stelle nur deine eigenen Sicherungen wieder her.** Die Sicherung eines anderen Spielers enthält
> dessen Controller, nicht deine.

**Sicherungseinstellungen** legt fest, wohin Sicherungen gehen (**Sicherungen speichern in**, mit
**Durchsuchen...** und **Standard verwenden**), und die beiden Aufbewahrungsregeln. Ein Ordner innerhalb
einer Spielinstallation wird abgelehnt, weil ein Spiel-Update deine Sicherungen mitlöschen würde.

---

<a id="alias-designer"></a>

## Alias-Designer

![Alias-Designer](images/ui-bindings-alias-designer.png)

Von Haus aus kennt das Spiel viele Controller nur über eine Hardware-ID, und sein Steuerungsbildschirm
zeigt ihre Tasten als anonyme Nummern. Der Alias-Designer gibt jedem Controller einen echten Namen und
jeder Taste, Achse und jedem Hat eine Beschriftung deiner Wahl. Die Beschriftungen erscheinen im
Steuerungsbildschirm des Spiels und im Bindungsprofil.

**Meine Geräte** listet deine Controller: **Gerät**, **VID / PID**, **Status** (*Angeschlossen* oder
*Fehlt*) und **Alias**. **Integrierte Geräte** ist eine schreibgeschützte Liste der Controller, die
Frontier mit dem Spiel ausliefert. **Erneut prüfen** liest neu, was angeschlossen ist.

<a id="naming-a-controller"></a>

### Einen Controller benennen

Hat ein Controller keinen Namen, führt dich **Controller benennen** hindurch. Das läuft nach der
Ersteinrichtung und bei **Erneut prüfen**, und du kannst es für einen einzelnen Controller über sein
Zahnrad starten. **Überspringen** nimmt den Standardnamen.

Ein Name besteht aus den Buchstaben A bis Z, Ziffern, `-` und `_`, beginnt mit einem Buchstaben und hat
keine Leerzeichen oder Akzente. Er darf einem schon verwendeten Namen nicht zu ähnlich sein. Belegungen,
die den Controller nur über seine Hardware-ID kennen, wechseln beim Anwenden zum neuen Namen.

### Der Geräte-Editor

Drücke das **Zahnrad** in der Zeile eines Geräts, um ihn zu öffnen.

![Geräte-Editor](images/ui-bindings-device-editor.png)

- **Gerät** — Name, VID, PID, Hardware-Name und Status. Ändere hier den Namen, und **Anwenden** benennt
  es in jeder Installation um und verschiebt die Belegungen, die es nutzen.
- **Achsen**, **Tasten**, **Hats** — tippe für jede Eingabe eine Beschriftung. Reiter erscheinen nur für
  die Eingabearten, die der Controller hat. Betätige eine Eingabe, und ihr Feld **leuchtet auf**, sodass
  du siehst, welche welche ist; Achsen zeigen eine Live-Anzeige.

**Speichern** behält deine Änderungen für **Anwenden**; bis dahin erreicht nichts das Spiel.

- **Leeren** entfernt beim Anwenden den Eintrag und die Beschriftungen des Geräts aus deinen Spieldateien.
- **Beschriftungen zurücksetzen** stellt die Standardbeschriftungen wieder her.

Bei einem integrierten Gerät von Frontier kannst du die Beschriftungen ändern. Für einen eigenen Namen
schließe es an und tippe den Namen ins Feld: Dein Eintrag kommt vor den von Frontier, und tippst du
wieder Frontiers Namen, gehst du zurück.

### Wenn sich deine Hardware ändert

Fehlt ein benannter Controller, den deine Belegungen nutzen, sagt Vega es dir: Seine Belegungen werden
nicht geladen, bis er angeschlossen ist. Auch ein Firmware-Update kann die Produkt-ID eines Controllers
ändern, und ein Ersatzgerät hat eine neue. BindForge kann das erkennen, und **Ist das derselbe
Controller?** fragt, welcher angeschlossene Controller es ist. Antworte, und seine Belegungen folgen ihm
beim Anwenden.

> Belegungen verweisen über die Nummer auf Tasten. Ist der Ersatz ein anderes Modell, landen manche
> Belegungen vielleicht auf unerwarteten Tasten. **Im Bind Editor prüfen** bringt dich direkt dorthin.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
