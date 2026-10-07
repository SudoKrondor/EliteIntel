# Systembefehle im Detail

[Alle Befehle](AllCommands) listet, was du sagen kannst. Diese Seite behandelt die Befehle, die
etwas mehr Erklärung brauchen: was du *vorher* tun musst, was sie tatsächlich tun und worauf du
achten solltest.

Alles hier funktioniert gesprochen, im Spielchat als `@Vega …` getippt oder per Klick im
[Reiter Aktionen](UI-Actions-Tab).

---

## Einstellungen, die du per Stimme umschaltest

Jede gesprochene Ansage hat einen Sprachschalter, und jeder entspricht einem Schalter im
[Reiter Kommandant → Ansagen](UI-Commander-Tab) — dort siehst du auf einen Blick, was an ist.

- **Routenansagen**: *„Routenansagen aus.“* Stellt alles stumm, was rund um einen Sprung gesagt
  wird — nächstes System, Verkehr, Verluste, Ankunft, verbleibende Sprünge, schöpfbarer Stern.
  Manuelle Abfragen funktionieren weiter.
- **Entdeckungsansagen**: *„Entdeckungsansagen an.“* Erstentdeckungen, wertvolle Körper,
  biologische Signale. Steuert auch die Exobiologie-Karte im [HUD-Overlay](UI-HUD-Overlay).
- **Planetenanflug**: *„Anflugansagen an.“*
- **Bergbau-Ansagen**: *„Mining Ansagen an.“* Prospektortreffer für deine Abbauziele. Erst Ziele
  setzen: *„Abbauziel hinzufügen Painit.“* Ohne Ziele gibt es nichts anzusagen.
- **Frachtschaufel-Aufnahmen**: *„Frachtschaufel Ansagen an.“*
- **Radarkontakte**: *„Radar Ansagen aus.“*
- **Funk**: *„Funk an.“* Funkverkehr in der Spielwelt — Piratendrohungen, Flugkontrolle — mit
  eigenen Funkstimmen. Lautstärke und Effekte liegen unter [Einstellungen → Audio](UI-Settings-Tab).
- **Alles auf einmal**: *„Alle Ansagen ausschalten.“*
- **Nachtsicht / Licht / Fahrassistenz**: *„Nachtsicht an.“* *„Licht.“* *„Fahrassistenz aus.“*

> **Beim Streamen oder im Geschwader?** Einen eigenen „Streaming-Modus“ gibt es nicht. Damit Vega
> nicht auf andere Stimmen reagiert, schick sie schlafen (*„Schlaf“*) und stell gelegentlichen
> Befehlen *„Hör zu, …“* voran — oder nutze [Push-to-Talk](UI-Settings-Tab), das alles ignoriert,
> solange die Taste nicht gehalten wird.

---

## Navigation & Orte finden

Vega plottet Routen zum Ergebnis einer Suche, zu Orten, die sie kennt, oder zu
Oberflächenkoordinaten. Zu einem laut genannten System plottet sie nicht — siehe
[eigener Befehl für Orte, die du oft anfliegst](UI-Actions-Tab).

- **Navigation zu Koordinaten**: *„Navigiere zu Koordinaten Breite 41,43 Länge -75,23.“* Führung
  vom Orbit bis zur Stelle auf dem aktuellen oder angeflogenen Körper. Auf der Nachtseite fliegst
  du nach Instrumenten.
- **Nächste Bio-Probe / Codex-Eintrag**: *„Zum nächsten Bio Sample navigieren.“* Führt dich zum
  nächsten gespeicherten Organismus auf diesem Planeten. *„Codex Eintrag löschen“* verwirft den,
  dem du gerade folgst.
- **Landezone**: *„Navigiere zur Landezone.“* Zurück dorthin, wo dein Schiff zuletzt gelandet ist.
- **Dein Träger**: *„Navigiere zum Fleet Carrier“* / *„navigiere zum Squadron Carrier.“* Plottet
  zum letzten bekannten Standort — oder zum Heimatsystem, wenn kein Träger bekannt ist.
- **Heimat**: *„Heimatsystem setzen“* markiert, wo du gerade bist (mit Rückfrage); *„bring mich
  nach Hause“* plottet zurück.
- **Navigation aus dem Speicher**: Systemnamen mit Strg+C aus INARA, Spansh oder einer
  Chatnachricht kopieren, dann *„navigiere aus dem Speicher.“* Vega öffnet die Galaxiekarte und
  plottet dorthin.
- **Trägerroute**: Galaxiekarte öffnen, Ziel wählen, Namen kopieren, dann *„Fleet Carrier Route
  berechnen.“* Die Route kommt von Spansh, das System muss dort bekannt sein.
- **Nächstes Trägerziel eingeben**: die Galaxiekarte *des Trägers* öffnen und *„Carrier Ziel
  eingeben“* sagen. Vega tippt die nächste Etappe der gespeicherten Route ein und bestätigt sie —
  nach jedem Sprung wiederholen.
- **Neutronenroute**: Zielnamen aus der Galaxiekarte kopieren und *„berechne die Neutronenroute“*
  sagen (optional *„…Effizienz 60“*, *„…mit Supercharge“*). Nach jedem Boost *„nächster
  Neutronenstern“* — oder *Nächsten Neutronensprung beim Kegel-Boost automatisch plotten* im
  [Reiter Kommandant](UI-Commander-Tab) einschalten.
- **Händler und Broker**: *„Rohmaterialhändler finden“*, *„Datenhändler finden“*, *„Human Tech
  Broker finden“*, *„nächste Vista Genomics finden“*, *„nächsten Interstellar Factor finden.“* Vega
  plottet die Route und hinterlässt eine Erinnerung mit der Station; frag bei Ankunft *„was war die
  Erinnerung?“*
- **Brain Trees**: *„Brain Trees für [Material] innerhalb von 500 Lichtjahren finden.“* Findet
  einen Guardian-Brain-Tree, der dieses Rohmaterial liefert.
- **Abbauorte**: *„Wo kann ich Osmium innerhalb von 200 Lichtjahren abbauen?“* Funktioniert auch
  für Tritium.
- **Kaufen und verkaufen**: *„Wo kann ich Bromellit kaufen?“* — mit *nächste* oder *bester
  Preis*; geht auch für Schiffsmodule. *„Wo kann ich Gold verkaufen?“*
- **Treibstoff**: *„Tankstelle finden“* / *„ich brauche Treibstoff.“*
- **Schlechte Suchergebnisse**: Schickt dich eine Suche immer wieder an einen Ort, der nicht
  funktioniert, sag *„dieses System aus Suchen ausschließen“* (oder *„Zielsystem aus Suchen
  ausschließen“*). Rückgängig aus dem System heraus: *„dieses System wieder zulassen.“*

---

## Kampf & Missionen

- **Zuerst aus der Vergangenheit lernen**: *„Journale nach Jagdgebieten durchsuchen.“* Liest
  deine gespeicherten Spieljournale und lernt jedes System mit Ressourcenabbaustätten und jeden
  Piraten-Massaker-Missionsgeber, den du je gesehen hast. Einmal nach der Installation ausführen.
- **Jagdgebiete**: *„Jagdgebiet finden innerhalb von 100 Lichtjahren.“* Ein System mit
  Ressourcenabbaustätten aus den Systemen, die du durchflogen hast. *„Dieses Jagdgebiet vergessen“*
  entfernt eins.
- **Massaker stapeln**: *„Finde Piraten Massaker Missionen“*, *„navigiere zum Piraten
  Missionsgeber“*, *„navigiere zum Piraten Missionsziel“*, *„wie viele Kills?“*
- **Konfliktzonen**: *„Finde eine Konfliktzone.“*
- **Missionen**: *„Navigiere zur aktiven Mission.“* *„Missionsware finden“* sucht, wo du kaufst,
  was eine aktive Mission noch braucht — die, die zuerst abläuft und deren Fracht du noch nicht an
  Bord hast.
- **Subsysteme**: *„Ziel Kraftwerk“* (auch Triebwerke, FSD, Energieverteiler, Lebenserhaltung,
  Schild).

---

## Abkürzungen für die Schiffssteuerung

- **Energieverteilung**: *„Energie auf Schilde“*, *„maximale Triebwerke“*, *„Energie
  ausgleichen.“* Ein Befehl setzt alle Pips.
- **Schließen / verlassen**: *„Schließen“* oder *„raus“* verlässt das offene Panel oder die Karte.
- **Scan**: *„Scanne das System“* löst den Entdeckungsscanner auf der Feuergruppe aus, die du pro
  Schiff eingestellt hast (Reiter Kommandant → ⚙). *„FSS öffnen“* öffnet den FSS.
- **Optimale Geschwindigkeit**: *„Optimale Geschwindigkeit.“* Setzt den Schub auf 75 % — den
  Supercruise-Sweetspot. Etwa 20 Sekunden vor dem Ziel sagen, damit du nicht drumherum kreist.
- **Nächstes System der Route anvisieren**: *„Sprungziel auswählen.“*
- **Wing Nav Lock**: *„Wingman folgen.“*
- **Feuergruppen**: *„Gruppe Bravo“* — NATO-Buchstaben oder Zahlen.
- **Wegschicken / zurückrufen**: *„Schiff wegschicken“* schickt es in den Orbit; *„hol mich ab“*
  holt es zurück.

---

## Hilfs- & Sitzungsbefehle

- **Erinnerungen**: *„Erinnerung setzen, Painit bei Hutton Orbital abholen.“* Bleibt gespeichert,
  bis du sie löschst; *„was war die Erinnerung?“* liest sie vor. *„Erinnerungen löschen“* fragt
  vorher nach.
- **Timer**: *„Erinnere mich in 20 Minuten den Träger zu prüfen.“*
- **Route monetarisieren**: *„Route monetarisieren.“* Findet ein lohnendes Kauf-/Verkaufspaar
  entlang deiner geplotteten Route und speichert es als Erinnerung — Handel, nicht Erkundung. Im
  [HUD-Overlay](UI-HUD-Overlay) erscheint es als *Frachtchance*.
- **Selbstdiagnose**: *„Selbstdiagnose“* / *„funktionierst du richtig?“* Testet die Verbindung zum
  Sprachmodell und meldet, ob und wie schnell es antwortet.
- **Unterbrechen**: *„Unterbrich.“* Stoppt Vega mitten im Satz. Mit Push-to-Talk tut das Drücken
  der Taste dasselbe.
- **Biom-Analyse**: *„Biom analysieren“* (oder einen Planeten nennen). Sagt, was dort
  wahrscheinlich wächst, bevor du landest.
- **Trägerfinanzen und Reichweite**: *„Carrier Status“* / *„Carrier Finanzen.“* Treibstoff,
  Reserve, Sprungreichweite, Kontostand und Laufzeit.
- **Baustellen**: *„Baufortschritt“*, *„Baustellenware finden“*, *„bring mich zurück zur
  Baustelle“*, *„Baustelle vergessen.“*

---

## Hinweise

- **Natürliche Sprache**: keine feste Syntax. Sag, was du meinst.
- **Namen in den Chat**: System-, Stations- und Warennamen bereiten der Spracherkennung die meisten
  Probleme. Tipp solche Befehle: `@Vega wo kann ich tritium kaufen`.
- **Folgenreiche Befehle fragen nach**: Erinnerungen, Abbauziele, Missionen oder eine Route
  löschen, einen Codex-Eintrag löschen, ein Jagdgebiet vergessen, ein System ausschließen oder ein
  neues Heimatsystem setzen. Antworte mit *ja*; alles andere bricht ab.
- **VR**: Energieverteilung und Schließen ersparen dir die Suche in Menüs im Headset.

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
