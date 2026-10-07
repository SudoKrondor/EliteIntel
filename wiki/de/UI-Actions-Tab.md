# Reiter Aktionen

<img src="images/keys-binding.png" class="inline" height="20" alt="Aktionen"> Alles, was Elite
Intel tun kann, und alles, was du ihm beigebracht hast. Zwei Unterreiter: **Integrierte Befehle**
und **Eigene Befehle**.

---

## Integrierte Befehle

![Integrierte Befehle](images/ui-tab-actions-builtin.png)

Das ist die Antwort auf *„Was kann ich gerade sagen?“* — nicht nur auf *„Was kann diese Version
überhaupt?“*

### Die Bereichsauswahl

Die Auswahl oben links enthält **ALLE** sowie jede körperliche Situation, in der du sein kannst:
im Schiff, im SRV, im Jäger, im Taxi; zu Fuß (Station, Hangar, Sozialbereich, Planet); angedockt,
gelandet, im Gleitflug, im Supercruise, an einem Ring, im Orbit, im tiefen Raum.

- Sie **folgt dem laufenden Spiel** — steig aus dem Schiff, und die Auswahl springt von selbst auf
  *Zu Fuß*, und die Liste darunter ändert sich mit.
- Sobald du einen Bereich von Hand wählst, **folgt sie nicht mehr** und bleibt, wo du sie
  hingesetzt hast.
- **ALLE** listet jede Aktion dieser Version auf, auch solche, die du gerade nicht nutzen kannst.
  Eine bestimmte Situation listet **nur, was dort nutzbar ist**.
- Läuft das Spiel nicht, zeigt die Auswahl **ALLE**.

Daneben zeigt ein schreibgeschütztes Feld **Ort** den konkreten Standort, den das Spiel meldet —
Station, Körper oder System.

### Suche

Ein schlichter, wörtlicher Textfilter über die gelisteten Aktionen: ihre Namen, ihre
Aktionsschlüssel und die gesprochenen Phrasen, die sie auslösen. Gesucht wird genau, was du
tippst.

> Das ist bewusst **nicht** Vegas Zuordnung. Vega ordnet nach *Bedeutung* zu, also würde ein
> getipptes „finden“ dort Befehle liefern, die kein Wort damit teilen, ohne dass du siehst warum.
> Zum Lesen einer Liste willst du eine wörtliche Suche.

### Verfügbare Befehle und Abfragen

Eine gemeinsame, alphabetisch sortierte Liste über drei Spalten mit integrierten Aktionen,
deinen eigenen Makros und Abfragen für den gewählten Bereich. Sie aktualisiert sich live aus
Spielereignissen, solange der Reiter offen ist — auch ein eigener Befehl, den du währenddessen
anlegst, erscheint.

**Klicke auf einen Eintrag** (oder wähle ihn und drücke Enter), um seine Details zu öffnen.

### Befehlsdetails

| Feld | Bedeutung |
|-------|---------|
| **Befehlsname** | Der lesbare Name |
| **Aktionsschlüssel** | Der interne Bezeichner — unter diesem Namen sieht ihn das Sprachmodell |
| **Befehlstyp** | `Integrierte Belegung` (drückt eine Taste) · `Integrierte Aktion` (tut etwas in der App) · `Integrierte Abfrage` (beantwortet eine Frage) · `Eigener Befehl` (deiner) |
| **Beschreibung** | Was er tut |
| **Trainingsphrasen** | Die gesprochenen Phrasen, die zu ihm führen, in deiner aktuellen Sprache |

Knöpfe:

- **Ausführen** — führt ihn sofort aus der App aus, ohne zu sprechen. Braucht der Befehl
  Parameter, erscheint zuerst ein kleines Formular.
- **Korrektur vorschlagen** — öffnet ein vorausgefülltes GitHub-Issue mit Befehls-ID, deiner
  Sprache, den aktuellen Phrasen und deinen Vorschlägen. So werden die nicht-englischen
  Phrasen besser — bitte nutze es.
- **Zurück** — schließt den Dialog.

Siehe auch: [Alle Befehle & Abfragen](AllCommands).

---

## Eigene Befehle

![Eigene Befehle](images/ui-tab-actions-custom.png)

> Schritt-für-Schritt-Anleitung: [Eigene Befehle erstellen](Custom-Commands).

Deine eigenen Makros — eine benannte Folge von Schritten, ausgelöst durch etwas, das du sagst
(oder im Spielchat tippst). Im Geist ähnlich wie VoiceAttack, aber nach Bedeutung zugeordnet
statt nach exakter Phrase.

Die Tabelle zeigt für jeden Befehl **Name** und **Trainingsphrasen**, darüber ein Suchfeld.
**Klicke auf eine Zeile**, um die Details zu öffnen: Sie zeigen die Schritt-**Sequenz** und bieten
**Ausführen**, **Bearbeiten**, **Duplizieren** und **Löschen**.

Oben:

| Knopf | Was er tut |
|--------|--------------|
| **Neu** | Einen Befehl anlegen |
| **Exportieren** | Befehle auswählen und in eine Datei schreiben, die du teilen kannst |
| **Importieren** | Befehle aus einer Datei lesen. Der Importdialog markiert jeden Eintrag als *Bereit*, *Konflikt* (sein Aktionsschlüssel existiert schon und wird überschrieben) oder *Ungültig*. Ein Import **ersetzt** deinen aktuellen Satz — der wird vorher gesichert, und danach wird dir **Sicherungsordner öffnen** angeboten |
| **Aus Sicherung wiederherstellen** | Holt den Satz zurück, der durch einen Import ersetzt wurde |

**Duplizieren** öffnet den Editor mit Namen und Schritten einer Kopie, aber **ohne Phrasen und
ohne Aktionsschlüssel** — schreib frische Phrasen dafür, damit die beiden Befehle gut
unterscheidbar bleiben.

> Wird die Datei der eigenen Befehle beim Start als beschädigt erkannt, lädt Elite Intel
> automatisch aus der Sicherung und sagt dir das.

### Der Befehlseditor

![Editor für eigene Befehle](images/ui-custom-command-editor.png)

**Befehlsidentität**

| Feld | Hinweise |
|-------|-------|
| **Name** | Wie du ihn nennst |
| **Was du sagst** | Die Phrasen, mit denen du ihn auslöst — **eine pro Zeile** |
| **Aktionsschlüssel** | Der interne Bezeichner. Drück **Erzeugen**, und das Sprachmodell schreibt ihn aus deinen Phrasen. Er ist immer englisches snake_case, egal in welcher Sprache deine Phrasen sind, weil er zu einem Werkzeugnamen wird, den das Modell sieht — deshalb kann er nicht von Hand eingetippt werden. Füge vor dem Erzeugen mindestens eine Phrase hinzu |

**Schritte** — die Abfolge, der Reihe nach. Schritte hinzufügen, bearbeiten, entfernen und nach
oben oder unten verschieben.

| Schritttyp | Felder | Wofür |
|-----------|--------|------------|
| **Binding-Tap** | Bindung | Eine belegte Steuerung einmal drücken |
| **Binding-Halten** | Bindung, Dauer ms | Eine belegte Steuerung halten |
| **Verzögerung** | Dauer ms | Zwischen Schritten warten |
| **Sprechen** | Text | Vega etwas sagen lassen |
| **Tastendruck** | Rohtaste, Modifikator, Dauer ms | Eine Taste drücken, die im Spiel mit nichts belegt ist |
| **Text eingeben** | Text | Text in das Feld tippen, das gerade den Fokus hat |

**Text eingeben** landet dort, wo der Tastaturfokus ist. Öffne das Textfeld mit einem früheren
Schritt — zum Beispiel **Tastendruck: Enter** für den Comms-Chat — sonst kommt der Text als
Tastendrücke bei deiner Schiffssteuerung an.

Nimm, wo möglich, **Binding**-Schritte statt **Tastendruck** — Bindungen folgen den Tasten, die
das Spiel tatsächlich nutzt, und überstehen es, wenn du eine Steuerung neu belegst.

### Beispiel: eine Route zu einem Ort, den du oft anfliegst

Vegas integrierte Navigation plottet eine Route nur zum **Ergebnis einer Suche** (ein Händler,
ein Markt, ein Jagdgebiet…), zu Orten, die sie schon kennt (Heimat, dein Träger, eine Mission),
oder zum System in deiner Zwischenablage. Zu einem System, das du einfach laut nennst, plottet
sie nicht — Namen sind genau das, woran die Spracherkennung am häufigsten scheitert. Für einen
Ort, den du regelmäßig anfliegst, erledigt ein eigener Befehl das exakt, jedes Mal:

![Eigener Befehl, der eine Route zum Jameson Memorial plottet](images/ui-custom-command-navigation.png)

**Was du sagst:** *Route zum Jameson Memorial*, *bring mich zum Jameson Memorial*, *Jameson
Memorial Route* — dann den Aktionsschlüssel **Erzeugen**.

| # | Schritt | Wert | Dauer ms | Was er tut |
|---|------|-------|-------------|--------------|
| 1 | Binding-Tap | GALAXYMAPOPEN | | Galaxiekarte öffnen |
| 2 | Verzögerung | | 1000 | Die Karte laden lassen |
| 3 | Binding-Halten | CAMZOOMIN | 500 | Die Kartenkamera zoomen |
| 4 | Binding-Tap | UI_LEFT | | Zum Suchfeld gehen |
| 5 | Binding-Tap | UI_RIGHT | | |
| 6 | Binding-Tap | UI_SELECT | | Das Suchfeld öffnen |
| 7 | Text eingeben | SHINRARTA DEZHRA | | Den Systemnamen tippen — ohne Spracherkennung |
| 8 | Tastendruck | ENTER | 50 | Suchen |
| 9 | Verzögerung | | 500 | Auf das Ergebnis warten |
| 10 | Binding-Tap | UI_RIGHT | | In das Ergebnisfeld wechseln |
| 11 | Binding-Halten | UI_UP | 500 | Zum obersten Knopf hochlaufen |
| 12 | Binding-Tap | UI_SELECT | | Die Route plotten |
| 13 | Verzögerung | | 3000 | Warten, bis die Route geplottet ist |
| 14 | Binding-Tap | CAMYAWLEFT | | |

Für deinen eigenen kopierst du ihn mit **Duplizieren**, änderst Namen, Phrasen und das System im
Schritt **Text eingeben** und erzeugst einen neuen Schlüssel.

Tipps:

- **Phrasen können auch durch Kommas** getrennt werden, nicht nur durch neue Zeilen.
- **Verzögerungen hängen von deinem PC ab.** Ist die Karte noch nicht bereit, wenn der nächste
  Schritt kommt, erhöhe die Verzögerungen — oder schiebe **Tempo der Tastatureingabe** im
  [Reiter Bindings](UI-Bindings-Tab) Richtung Langsam.
- **Jeder Binding-Schritt braucht eine Tastaturbelegung** im Spiel. Ist eine Steuerung
  unbelegt, zeigt der [Reiter Bindings](UI-Bindings-Tab) sie unter *Fehlende Belegungen*.
- Vega öffnet und liest die Galaxiekarte für ihre eigenen Routenbefehle genauso, daher ist ein
  Befehl, der von einer **frisch geöffneten** Karte ausgeht, das zuverlässige Muster — starte
  nicht von einer bereits offenen Karte.

### Verwendung

Sprich ganz normal. Du musst keine Trainingsphrase Wort für Wort wiederholen — du musst
dieselbe Bedeutung vermitteln. Je deutlicher sich deine Phrasen von anderen Befehlen
unterscheiden, desto zuverlässiger wird deiner gewählt.

Vega sagt dir beim Start, wie viele eigene Befehle geladen wurden und wie viele die Prüfung nicht
bestanden haben.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
