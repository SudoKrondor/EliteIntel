# Reiter „Aktionen"

<img src="images/keys-binding.png" class="inline" height="20" alt="Aktionen"> Alles, was Elite
Intel kann, und alles, was du ihm beigebracht hast. Zwei Unterreiter: **Integrierte Befehle** und
**Eigene Befehle**.

---

## Integrierte Befehle

![Integrierte Befehle](images/ui-tab-actions-builtin.png)

Das ist die Antwort auf *„Was kann ich gerade jetzt sagen?"* — nicht bloß auf *„Was beherrscht
dieser Build überhaupt?"*

### Die Situationsauswahl

Die Auswahl oben links enthält **ALLE** sowie jede physische Situation, in der du sein kannst: im
Schiff, im SRV, im Jäger, im Taxi, zu Fuß; angedockt, gelandet, im Gleitflug, im Überlichtflug, an
einem Ring, im Orbit, im tiefen Raum.

- Sie **folgt dem laufenden Spiel** — steig aus deinem Schiff aus, und die Auswahl springt von
  selbst auf *Zu Fuß*, und die Liste darunter ändert sich mit.
- Sobald du eine Situation von Hand wählst, **hört sie auf zu folgen** und bleibt, wo du sie
  hingestellt hast.
- **ALLE** listet jede Aktion dieses Builds auf, auch solche, die du an deinem Ort nicht nutzen
  kannst. Eine konkrete Situation listet **nur das, was dort brauchbar ist**.

Daneben zeigt ein schreibgeschütztes Feld **Ort** den konkreten Aufenthaltsort, den das Spiel
meldet — Station, Himmelskörper oder System.

### Suche

Ein schlichter, wörtlicher Textfilter über die aufgelisteten Aktionen: ihre Namen, ihre
Aktionsschlüssel und die gesprochenen Phrasen, die sie auslösen. Was du tippst, wird genau so
gesucht.

> Das ist bewusst **nicht** die Zuordnung des Begleiters. Vegas Dispatch bewertet nach *Bedeutung*,
> ein getipptes „finden" würde dort also Befehle hervorholen, die kein Wort damit teilen, ohne dass
> du sehen könntest, warum. Beim Lesen einer Liste willst du die wörtliche Suche.

### Verfügbare Befehle und Abfragen

Eine kombinierte, alphabetisch sortierte Liste über drei Spalten, mit integrierten Aktionen, deinen
eigenen Makros und Abfragen für die gewählte Situation. Sie aktualisiert sich live aus
Spielereignissen, solange der Reiter offen ist.

**Doppelklicke einen Eintrag**, um seine Details zu öffnen.

### Befehlsdetails

| Feld | Bedeutung |
|-------|---------|
| **Befehlsname** | Der menschenlesbare Name |
| **Aktionsschlüssel** | Die interne Kennung — das ist der Name, den das Sprachmodell sieht |
| **Befehlstyp** | `Integrierte Belegung` (drückt eine Taste) · `Integrierte Aktion` (tut etwas in der App) · `Integrierte Abfrage` (beantwortet eine Frage) · `Eigener Befehl` (deiner) |
| **Beschreibung** | Was er tut |
| **Trainingsphrasen** | Die gesprochenen Phrasen, die dorthin führen, in deiner aktuellen Sprache |

Drei Schaltflächen:

- **Ausführen** — führt ihn sofort aus der App aus, ohne zu sprechen. Nimmt der Befehl Parameter
  entgegen, erscheint zuerst ein kleines Formular.
- **Korrektur vorschlagen** — öffnet ein vorausgefülltes GitHub-Issue mit der Befehls-ID, deiner
  Sprache und den aktuellen Phrasen, damit du bessere Formulierungen für deine Locale vorschlagen
  kannst. So werden die nicht-englischen Phrasensätze besser; bitte nutze das.
- **Schließen**

Siehe auch: [Alle Befehle](AllCommands).

---

## Eigene Befehle

![Eigene Befehle](images/ui-tab-actions-custom.png)

Deine eigenen Makros — eine benannte Schrittfolge, ausgelöst durch Dinge, die du sagst. Im Geiste
ähnlich zu VoiceAttack, aber nach Bedeutung abgeglichen statt nach exakter Phrase.

Die Tabelle listet **Name** und **Trainingsphrasen** jedes Befehls, mit einem Suchfeld darüber.

| Schaltfläche | Funktion |
|--------|--------------|
| **Neu** | Einen Befehl anlegen |
| **Bearbeiten** | Den gewählten Befehl bearbeiten |
| **Löschen** | Den gewählten Befehl löschen (mit Rückfrage) |
| **Exportieren** | Gewählte Befehle in eine teilbare Datei schreiben |
| **Importieren** | Befehle aus einer Datei lesen. Dein aktueller Satz wird vorher gesichert |
| **Aus Sicherung wiederherstellen** | Den Satz zurückholen, den ein Import ersetzt hat |
| **Sicherungsordner öffnen** | Öffnet den Ordner auf der Festplatte |

> Wird die Datei der eigenen Befehle beim Start je als beschädigt erkannt, lädt Elite Intel
> automatisch aus der Sicherung und sagt dir, dass es das getan hat.

### Der Befehlseditor

![Editor für eigene Befehle](images/ui-custom-command-editor.png)

**Befehlsidentität**

| Feld | Anmerkungen |
|-------|-------|
| **Name** | Wie du ihn nennst |
| **Beschreibung** | Was er tut |
| **Was du sagst** | Die Phrasen, mit denen du ihn ausführen würdest — **eine pro Zeile** |
| **Aktionsschlüssel** | Die interne Kennung. Drücke **Erzeugen**, und das Sprachmodell schreibt dir aus deinen Phrasen einen. Er muss ASCII-snake_case sein, weil er zu einem Werkzeugnamen wird, den das Modell sieht — überlass das also der Schaltfläche „Erzeugen". Füge vor dem Erzeugen mindestens eine Phrase hinzu |

**Schritte** — die Abfolge, in ihrer Reihenfolge. Schritte hinzufügen, bearbeiten, entfernen sowie
nach oben und unten verschieben.

| Schrittart | Felder | Wofür |
|-----------|--------|------------|
| **Binding-Tap** | Belegung | Eine belegte Steuerung einmal drücken |
| **Binding-Halten** | Belegung, Dauer in ms | Eine belegte Steuerung gedrückt halten |
| **Verzögerung** | Dauer in ms | Zwischen Schritten warten |
| **Sprechen** | Text | Vega etwas sagen lassen |
| **Tastendruck** | Tastendruck, Modifikator | Eine Taste drücken, die im Spiel nichts belegt |

Bevorzuge **Belegungs**-Schritte gegenüber **Tastendruck**, wo es geht — Belegungen folgen den
Tasten, die das Spiel tatsächlich benutzt, und überstehen daher ein Neubelegen einer Steuerung.

### Ihre Verwendung

Sprich normal. Du musst eine Trainingsphrase nicht Wort für Wort wiedergeben — du musst dieselbe
Bedeutung transportieren. Je deutlicher sich deine Phrasen von anderen Befehlen unterscheiden, desto
zuverlässiger wird deiner gewählt.

Vega sagt dir beim Start, wie viele eigene Befehle geladen wurden und wie viele die Prüfung nicht
bestanden haben.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
