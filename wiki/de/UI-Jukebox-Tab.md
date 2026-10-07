# Reiter Jukebox

<img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> Deine eigene Musik, aus
deinen eigenen Dateien, unter Vega gespielt statt über sie. Sobald Vega spricht, wird die Musik
automatisch leiser und kommt danach wieder hoch — du verpasst keine Meldung.

Nichts zu installieren, kein Konto, kein Streamingdienst. Der Reiter ist von oben nach unten
gegliedert: **Musikbibliothek**, **Wiedergabeliste**, **Wiedergabe**.

---

## Musikbibliothek

Woher die Musik kommt.

- **Durchsuchen...** — einen Musikordner wählen. Elite Intel durchläuft ihn samt Unterordnern und
  fügt jede abspielbare Datei der Wiedergabeliste hinzu. Dateien kommen in Ordnerreihenfolge
  hinzu, sodass Hörbuchkapitel in der richtigen Folge ankommen.
- **Neu einlesen** — den Ordner erneut nach neu hinzugekommenen Dateien durchsuchen.

**Unterstützte Formate:** MP3, FLAC, M4A / M4B (AAC), OGG / OGA (Vorbis) und WAV. Dateien, die die
Jukebox nicht abspielen kann (WMA, Apple Lossless, DRM-geschützte Käufe, Opus), werden
übersprungen.

---

## Wiedergabeliste

Die Wiedergabeliste ist die Warteschlange: was du siehst, ist die Reihenfolge, in der gespielt
wird.

| Spalte | Bedeutung |
|--------|---------|
| **#** | Position. Ein ▶ markiert den laufenden Titel |
| **Titel** · **Interpret** · **Album** | Aus den Tags der Dateien gelesen. Eine große Bibliothek füllt sich in ein paar Sekunden |
| **Dauer** | Länge des Titels |

- **Doppelklick** auf einen Titel spielt ihn ab.
- **Ziehe** Zeilen, um sie umzuordnen. Die Reihenfolge wird gespeichert.
- Ein Klick auf einen Spaltenkopf sortiert **nicht** — das würde eine von Hand angelegte
  Reihenfolge wegwerfen. Sortieren steckt stattdessen im Rechtsklickmenü.

Eine Datei, die von der Festplatte verschwunden ist, wird als **fehlt** markiert.

### Rechtsklickmenü

| Eintrag | Was er tut |
|------|--------------|
| **Jetzt abspielen** | Den gewählten Titel spielen |
| **Als Nächstes abspielen** | Die gewählten Titel direkt hinter den aktuellen schieben |
| **Aus der Liste entfernen** | Aus der Liste nehmen (die Dateien auf der Festplatte bleiben unberührt) |
| **Im Dateimanager anzeigen** | Den Ordner mit der Datei öffnen |
| **Interpret und Titel kopieren** | In die Zwischenablage |
| **Ordner hinzufügen...** | Musik aus einem weiteren Ordner hinzufügen |
| **Wiedergabeliste importieren...** | Die Titel hinzufügen, die eine `.m3u` / `.m3u8`-Liste nennt |
| **Fehlende Dateien entfernen** | Jeden Eintrag entfernen, dessen Datei fehlt |
| **Liste leeren** | Alle Titel entfernen (fragt vorher; Dateien auf der Festplatte werden nicht gelöscht) |
| **Sortieren nach** → Titel / Interpret / Ordner | Eine einmalige Sortierung, die die Reihenfolge der Liste neu schreibt |

---

## Wiedergabe

- **Der Abspielkopf** — wie weit du im Titel bist. Ziehe ihn zum Springen; die Wiedergabe springt,
  wenn du loslässt.
- **Zurück · Abspielen/Pause · Stopp · Weiter** — die Transportknöpfe. **Stopp** spult den
  aktuellen Titel an den Anfang zurück; **Pause** behält die Stelle.
- **Reihenfolge** — *Der Reihe nach* oder *Zufällig*.
- **Lautstärke** — der eigene Pegel der Musik. Er ist hier und nicht bei den Audioeinstellungen,
  damit du nie versehentlich Vega leiser drehst.

Deine Stelle im Titel bleibt zwischen Sitzungen erhalten — praktisch für Hörbücher —, aber die
Jukebox fängt beim Start der App nie von selbst an zu spielen.

---

## Sprachbefehle

Jeder Musikbefehl nennt *Musik*, einen *Titel* oder ein *Lied*, damit er nie mit
Schiffsbefehlen wie „Stopp“ oder „nächstes“ kollidiert.

| Sag | Was passiert |
|-----|--------------|
| *„Musik abspielen“* / *„Musik starten“* | Starten oder fortsetzen |
| *„Musik pausieren“* / *„Musik stoppen“* | Pause — „Musik abspielen“ macht da weiter, wo sie aufgehört hat |
| *„Nächster Titel“* / *„Titel überspringen“* | Nächster Titel |
| *„Vorheriger Titel“* | Vorheriger Titel |
| *„Playlist neu starten“* | Zurück zum ersten Titel |
| *„Musik zufällig abspielen“* / *„Musik mischen“* | Zufällige Reihenfolge |
| *„Spiele das Lied Rocket Man“* | Einen Titel nach Name oder Interpret suchen und abspielen. Passt nichts eindeutig, sagt Vega das, statt das Falsche zu spielen |

Das funktioniert auch getippt im Spielchat — siehe [Alle Befehle](AllCommands).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
