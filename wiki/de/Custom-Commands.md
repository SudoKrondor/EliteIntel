# Eigene Befehle erstellen

Ein eigener Befehl ist ein Makro: eine Liste von Schritten — Tastendrücke, Pausen, getippter Text,
gesprochene Zeilen —, die abläuft, wenn du eine deiner Phrasen sagst. Nimm einen, wenn die
integrierten Befehle nicht tun, was du willst, oder wenn du immer wieder dieselbe Tastenfolge
drückst.

Eigene Befehle liegen im [Reiter Aktionen → Eigene Befehle](UI-Actions-Tab). Diese Seite baut zwei
davon von Grund auf.

---

## Bevor du anfängst

- **Ein Sprachmodell muss laufen.** Es erzeugt den Aktionsschlüssel und ordnet deine Phrasen zu.
  Starte zuerst die Dienste im [Vega-Reiter](UI-Vega-Tab).
- **Jede verwendete Steuerung braucht eine Tastaturbelegung.** Elite Intel drückt Tasten; eine
  Steuerung, die nur auf deinem HOTAS liegt, kann ein Makro nicht drücken. Der
  [Reiter Bindings](UI-Bindings-Tab) zeigt, was fehlt, und **Fehlende Belegungen automatisch
  zuweisen** behebt es in einem Rutsch.

---

## Beispiel 1: Gegenmaßnahmen auf ein Wort

Ziel: *„Gegenmaßnahmen los“* sagen und Wärmesenke, Täuschkörper und Schildzelle bekommen, mit
Bestätigung von Vega.

### 1. Befehl anlegen

Öffne **Aktionen → Eigene Befehle** und drücke **Neu**.

### 2. Namen und Phrasen eintragen

- **Name:** `Gegenmaßnahmen`
- **Was du sagst:** eine Phrase pro Zeile (Kommas gehen auch):

  ```
  gegenmaßnahmen los
  alle gegenmaßnahmen
  verteidigungspaket
  ```

Schreib die Phrasen so, wie du sie unter Druck wirklich sagen würdest. Sie müssen sich **von
integrierten Befehlen unterscheiden** — eine Phrase, die schon einem integrierten Befehl gehört
(*„Wärmesenke“*, *„Täuschkörper einsetzen“*), wird abgelehnt, weil Vega die beiden nie
auseinanderhalten könnte.

### 3. Aktionsschlüssel erzeugen

Drück **Erzeugen**. Das Sprachmodell macht aus deinen Phrasen einen englischen `snake_case`-Schlüssel
wie `deploy_all_countermeasures`. Von Hand eintippen kannst du ihn nicht — er wird zu dem Namen, den
das Modell sieht, und muss daher in einer Form sein, die das Modell immer wiedergeben kann. Er ist
englisch, auch wenn deine Phrasen es nicht sind.

### 4. Schritte hinzufügen

Drück für jeden Schritt **Schritt hinzufügen**. Wähle im Dialog den **Typ** und füll aus, was dieser
Typ verlangt. Die Liste **Bindung** ist wie der Steuerungsbildschirm des Spiels beschriftet —
*Schiffsteuerung / Cooling / Deploy Heatsink* — und du kannst darin tippen, um zu filtern (probier
`heat`, `srv`, `chaff`). Die Namen der Steuerungen stehen dort auf Englisch.

| # | Typ | Wert | Dauer ms |
|---|------|-------|-------------|
| 1 | Binding-Tap | Deploy Heatsink | |
| 2 | Verzögerung | | 150 |
| 3 | Binding-Tap | Use Chaff Launcher | |
| 4 | Verzögerung | | 150 |
| 5 | Binding-Tap | Use Shield Cell | |
| 6 | Sprechen | Gegenmaßnahmen ausgelöst. | |

Mit **Schritt bearbeiten**, **Schritt entfernen** und den Knöpfen **▲ ▼** korrigierst du die
Reihenfolge.

### 5. Speichern und testen

Drück **Speichern**. Stimmt etwas nicht, sagt dir eine Prüfmeldung was — siehe *Fehlersuche* unten
auf dieser Seite. Der Befehl erscheint jetzt in der Liste.

Klick auf seine Zeile und dann **Ausführen**, um ihn ohne Sprechen zu testen. Funktioniert er, sag
ihn: *„Gegenmaßnahmen los.“* Du kannst ihn auch im Spielchat tippen: `@Vega gegenmaßnahmen los`.

---

## Beispiel 2: eine Route zu einem Ort, den du oft anfliegst

Zu einem laut genannten System plottet Vega keine Route — Namen sind genau das, woran die
Spracherkennung am häufigsten scheitert. Für einen Ort, den du regelmäßig anfliegst, bedient ein
Makro die Galaxiekarte für dich, jedes Mal gleich.

![Eigener Befehl, der eine Route zum Jameson Memorial plottet](images/ui-custom-command-navigation.png)

**Was du sagst:** *Route zum Jameson Memorial*, *bring mich zum Jameson Memorial*, *Jameson
Memorial Route*. Dann **Erzeugen**.

| # | Typ | Wert | Dauer ms | Was er tut |
|---|------|-------|-------------|--------------|
| 1 | Binding-Tap | GALAXYMAPOPEN | | Galaxiekarte öffnen |
| 2 | Verzögerung | | 1000 | Die Karte laden lassen |
| 3 | Binding-Halten | CAMZOOMIN | 500 | Die Kartenkamera zoomen |
| 4 | Binding-Tap | UI_LEFT | | Zum Suchfeld gehen |
| 5 | Binding-Tap | UI_RIGHT | | |
| 6 | Binding-Tap | UI_SELECT | | Das Suchfeld öffnen |
| 7 | Text eingeben | SHINRARTA DEZHRA | | Den Systemnamen tippen |
| 8 | Tastendruck | ENTER | 50 | Suchen |
| 9 | Verzögerung | | 500 | Auf das Ergebnis warten |
| 10 | Binding-Tap | UI_RIGHT | | In das Ergebnisfeld wechseln |
| 11 | Binding-Halten | UI_UP | 500 | Zum obersten Knopf hochlaufen |
| 12 | Binding-Tap | UI_SELECT | | Die Route plotten |
| 13 | Verzögerung | | 3000 | Warten, bis die Route geplottet ist |
| 14 | Binding-Tap | CAMYAWLEFT | | |

Für einen anderen Ort wählst du diesen Befehl, drückst **Duplizieren**, änderst Namen, Phrasen und
das System in Schritt 7 und lässt einen neuen Schlüssel **Erzeugen**. Duplizieren lässt Phrasen und
Schlüssel absichtlich leer, damit die beiden Befehle nie gleich klingen.

Starte so ein Makro **im Flug**, mit geschlossener Galaxiekarte — der erste Schritt öffnet sie, und
eine schon offene Karte würde er stattdessen schließen.

---

## Die Schritttypen

| Typ | Felder | Wofür |
|------|--------|------------|
| **Binding-Tap** | Bindung | Eine Spielsteuerung einmal drücken |
| **Binding-Halten** | Bindung, Dauer ms | Eine Spielsteuerung halten — Schub, Kamera, durch ein Menü scrollen |
| **Verzögerung** | Dauer ms | Dem Spiel Zeit geben: ein Panel öffnet, eine Karte lädt, eine Suche läuft |
| **Sprechen** | Text | Vega etwas sagen lassen — eine Bestätigung oder was das Makro gerade getan hat |
| **Tastendruck** | Rohtaste, Modifikator, Dauer ms | Eine Taste drücken, die keine Spielbelegung ist: Enter, Escape, ein Buchstabe. Die Dauer ist die Haltezeit |
| **Text eingeben** | Text | Zeichen in das Feld tippen, das den Fokus hat — ein Suchfeld, die Chatzeile |

**Binding-Schritte sind besser als Tastendruck.** Eine Bindung folgt der Taste, die das Spiel
tatsächlich nutzt; das Makro funktioniert also weiter, wenn du die Steuerung neu belegst. Nimm
Tastendruck nur für Tasten ohne Spielbelegung, etwa Enter in einem Textfeld.

**Text eingeben landet dort, wo der Tastaturfokus ist.** Öffne zuerst das Feld — mit einem
Binding-Schritt, der ein Suchfeld öffnet, oder mit **Tastendruck: Enter** für den Comms-Chat. Sonst
kommen die Buchstaben als Tastendrücke bei deiner Schiffssteuerung an.

---

## Tipps

- **Verzögerungen sind die übliche Lösung.** Wird ein Schritt scheinbar übersprungen, war das Spiel
  noch nicht bereit. Verlängere die Verzögerung davor oder schiebe **Tempo der Tastatureingabe** im
  [Reiter Bindings](UI-Bindings-Tab) Richtung Langsam — das fügt nach jedem gesendeten Tastendruck
  eine Pause ein.
- **Mehrere Phrasen, eindeutige Bedeutung.** Du musst keine Phrase wörtlich sagen; Vega ordnet nach
  Bedeutung zu. Je mehr sich deine Phrasen von anderen Befehlen unterscheiden, desto zuverlässiger
  wird deiner gewählt.
- **Sprechen-Schritte lassen Makros wie einen Teil des Schiffs wirken** und bestätigen, dass das
  Makro wirklich lief.
- **Teile sie.** **Exportieren** schreibt ausgewählte Befehle in eine Datei; **Importieren** liest
  eine. Ein Import ersetzt deinen aktuellen Satz, der aber vorher gesichert wird — **Aus Sicherung
  wiederherstellen** holt ihn zurück.
- **Achte auf die Startmeldung.** Beim Start der Dienste sagt Vega, wie viele eigene Befehle geladen
  wurden und wie viele die Prüfung nicht bestanden.

---

## Fehlersuche

Die Prüfmeldungen beim Speichern erscheinen auf Englisch.

| Meldung oder Symptom | Was tun |
|--------------------|------------|
| *Füge oben mindestens eine Formulierung hinzu, bevor du einen Schlüssel erzeugst.* | Erst mindestens eine Phrase schreiben |
| *Aktionsschlüssel konnte nicht erzeugt werden.* | Das Sprachmodell hat nicht geantwortet. Prüfe die KI-Anzeige im Vega-Reiter und versuch es erneut |
| *Phrase collides with a built-in action alias* | Diese Phrase startet schon einen integrierten Befehl. Formuliere sie um |
| *Phrase collides with another custom command* / *Duplicate phrase* | Zwei Befehle — oder einer doppelt — nutzen diese Phrase. Ändere eine |
| *Action key collides with a built-in command* / *must be unique* | Formuliere die Phrasen um und **Erzeugen** erneut |
| *At least one step is required* / *… is required* / *durationMs must be positive* | Einem Schritt fehlt Bindung, Text, Taste oder Dauer |
| Das Makro läuft, aber etwas wird übersprungen | Verzögerung davor einfügen oder verlängern, oder Tempo der Tastatureingabe verlangsamen |
| Vega reagiert mit etwas anderem | Phrasen unverwechselbarer machen oder getippt ausführen: `@Vega <Phrase>` |
| Ein Binding-Schritt tut nichts | Diese Steuerung hat keine Tastaturbelegung — siehe [Reiter Bindings](UI-Bindings-Tab) |

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
