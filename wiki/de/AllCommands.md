# Elite Intel: Befehle & Abfragen

Hallo Kommandant! Dies ist eine Übersicht darüber, was du **Vega**, die KI deines Schiffs,
fragen oder anweisen kannst. **Du musst nichts davon auswendig lernen** — sprich natürlich, und
Vega versteht, was du meinst. Die Phrasen unten sind Beispiele, keine Skripte: entscheidend ist
die Bedeutung, nicht der genaue Wortlaut.

> **Die verbindliche, immer aktuelle Liste steckt in der App.** Der
> [Reiter Aktionen → Integrierte Befehle](UI-Actions-Tab) listet jeden Befehl und jede Abfrage
> dieser Version, gefiltert auf das, was du dort, wo du gerade bist, nutzen kannst — mit den
> Trainingsphrasen in deiner Sprache. Von dort kannst du jeden mit einem Klick ausführen.

---

## Mit Vega sprechen

**Sprich natürlich.** *„Fahrwerk runter“*, *„Fahrwerk raus“* und *„Fahrwerk“* tun dasselbe. Je
klarer du sagst, was du willst, desto zuverlässiger passiert es.

**Sie mit Namen anzusprechen ist optional.** *„Vega, Fahrwerk runter“* funktioniert genau wie
*„Fahrwerk runter“*.

**Schlafen und Wecken.**

- *„Schlaf“* / *„ignoriere mich“* — Vega hört nicht mehr zu.
- *„Wach auf“* — sie hört wieder zu.
- *„Hör zu, …“* — bringt im Schlaf **einen** Befehl durch, ohne sie zu wecken:
  *„Hör zu, Sprung in den Hyperraum.“*

**Unterbrechen.** Sag *„unterbrich“*, um Vega mitten im Satz zu stoppen. Mit
[Push-to-Talk](UI-Settings-Tab) genügt es, die Taste zu drücken.

**Folgenreiche Befehle fragen nach.** Erinnerungen, Abbauziele oder aktive Missionen löschen,
eine Handels-, Neutronen- oder Trägerroute löschen, einen Codex-Eintrag löschen, ein Jagdgebiet
vergessen, eine Baustelle verwerfen, ein System aus Suchen ausschließen oder ein neues
Heimatsystem setzen — hier bittet Vega um Bestätigung. Antworte mit *ja*, um fortzufahren; *nein*
oder alles andere bricht ab.

---

## Befehle im Spielchat tippen

Die Spracherkennung wird manche Wörter immer verstümmeln — Systemnamen, Warennamen, *Tritium*.
Für solche Fälle **tippst du den Befehl in den Chat im Spiel**. Beginne die Zeile mit `@Vega`:

```
@Vega wo kann ich tritium kaufen
@Vega abbauziel hinzufügen painit
@vega, fahrwerk runter
```

- Groß- und Kleinschreibung ist egal, ein Komma oder Doppelpunkt nach dem Namen geht auch.
- Die Zeile wird genau so behandelt, als hättest du sie gesagt, und unterbricht Vega, falls sie
  gerade spricht.
- Nutze den **lokalen** Kanal. Gelesen werden nur deine eigenen gesendeten Zeilen — nichts, was
  ein anderer Kommandant tippt, kann Vega einen Befehl geben.

---

## ⚙️ App & Sitzung

- Schlafen / wach auf / *„hör zu, [Befehl]“* — siehe oben.
- *„Unterbrich“* — Sprechen stoppen.
- *„Selbstdiagnose“* / *„funktionierst du richtig?“* — Vega prüft sich selbst und berichtet.
- *„Wie spät ist es?“* — echte UTC-Zeit.
- *„Erinnerung setzen [Text]“* — eine stehende Notiz; *„was war die Erinnerung?“* liest sie vor.
- *„Erinnere mich in 10 Minuten den Träger zu prüfen“* — ein Countdown-Timer.
- *„Erinnerungen löschen.“*

### Ansagen an/aus

- *„Radar Ansagen aus“* / *„Radar Ansagen an“*
- *„Entdeckungsansagen an / aus“*
- *„Routenansagen an / aus“*
- *„Anflugansagen an / aus“* — Planetenanflug
- *„Mining Ansagen an / aus“*
- *„Frachtschaufel Ansagen an / aus“*
- *„Funk an / aus“* — Funkübertragungen
- *„Alle Ansagen ausschalten“*

Jede davon ist auch ein Schalter im [Reiter Kommandant → Ansagen](UI-Commander-Tab).

---

## 🎮 Schiffssteuerung

- **Fahrwerk:** *„Fahrwerk runter“* / *„Fahrwerk hoch“*, *„Fahrwerk einfahren“*
- **Waffen:** *„Hardpoints“*, *„Waffen ausfahren“* / *„Waffen einfahren“*, *„Waffen kalt“*
- **Frachtschaufel:** *„Frachtschaufel öffnen“*
- **Licht / Nachtsicht:** *„Scheinwerfer“*, *„Licht“*, *„Nachtsicht an“*
- **HUD-Modus:** *„Kampfmodus“* / *„Analysemodus“*
- **Verteidigung:** *„Wärmesenke“*, *„Schildzelle einsetzen“*, *„Täuschkörper abwerfen“*
- **Feuergruppen:** *„Gruppe Bravo“*, *„Feuergruppe 3“*
- **Energie:** *„Energie auf Schilde / Triebwerke / Waffen / Systeme“* (*„maximale Schilde“*),
  *„Energie ausgleichen“*
- **Kopfblick:** *„Blick nach vorne“* / *„Ansicht zentrieren“*
- **Aktivieren:** *„aktivieren“* — drückt, was im offenen Panel gewählt ist.

### Schub

- *„Triebwerke stoppen“* / *„anhalten“*
- *„Viertel Schub“*, *„halber Schub“*, *„drei Viertel Schub“*, *„voller Schub“*
- *„Geschwindigkeit erhöhen um 2“* / *„langsamer um 1“*
- *„Optimale Geschwindigkeit“* — setzt den Schub für den Supercruise-Anflug.

### Flug

- *„Schiff starten“* / *„starten“* — den Landeplatz verlassen.
- *„Andocken anfragen“* / *„Landeerlaubnis anfragen“*
- *„Autopilot“* / *„Taxi“* — die Landung dem Andockcomputer überlassen.
- *„In Supercruise gehen“* / *„Supercruise“*
- *„Sprung“* / *„Sprung in den Hyperraum“* / *„los gehts“* / *„nächster Wegpunkt“* — der
  eigentliche Sprung.
- *„Aus dem Supercruise fallen“* / *„hier rausfallen“*
- *„Sprungziel auswählen“* — wählt das nächste System deiner geplotteten Route.
- *„Scanne das System“* / *„System erkunden“* — feuert den Entdeckungsscanner (siehe die
  Scan-Einstellung pro Schiff im [Reiter Kommandant](UI-Commander-Tab)).
- *„FSS öffnen“* / *„vollständiger Spektralscan“* — öffnet den FSS und scannt.

---

## 🚙 SRV, Jäger & zu Fuß

- *„SRV ausfahren“* — öffnet die richtige Hangarbucht (lege deine Buchten in den
  ⚙-Einstellungen jedes Schiffs im [Reiter Kommandant](UI-Commander-Tab) fest).
- *„Nomad starten“*
- *„SRV bergen“* / *„zurück ins Schiff“* — aus dem SRV.
- *„Fahrassistenz an / aus“*
- *„Aussteigen“* / *„von Bord gehen“*
- *„Schiff wegschicken“* / *„weggetreten“* — Schiff in den Orbit schicken.
- *„Hol mich ab“* / *„zur Oberfläche zurückkehren“* — es zurückrufen.
- *„Stationsdienste“* — das Dienste-Panel, wenn du im SRV angedockt bist.

### Jäger-Befehle

- *„Jäger starten“*
- *„Jäger defensiv“* · *„Greife mein Ziel an“* · *„Feuer frei“* · *„Jäger Feuer einstellen“* ·
  *„Jäger zurückrufen“*

---

## ⚔️ Kampf & Missionen

- **Ziele:** *„höchste Bedrohung anvisieren“*, *„gefährlichstes Ziel“*, *„Feind auswählen“*
- **Subsysteme:** *„Ziel FSD“*, *„Ziel Kraftwerk“*, *„Ziel Triebwerke“*, *„Ziel
  Energieverteiler“*, *„Ziel Lebenserhaltung“*, *„Ziel Schild“*
- **Geschwader:** *„Wingman eins / zwei / drei anvisieren“* (oder *Alpha / Bravo / Charlie*),
  *„Wingman folgen“*
- *„Aktive Missionen“* / *„Missionslog“* — alles auf deiner Tafel.
- *„Navigiere zur aktiven Mission“*
- *„Missionsware finden“* — wo du kaufst, was eine aktive Mission noch braucht, und Route dorthin.
- *„Aktive Missionen löschen“*
- *„Gesamte Kopfgelder“* — gesammelte Kopfgelder.

### Piraten-Massaker stapeln

- *„Finde Piraten Massaker Missionen“* / *„wo kann ich Massaker Missionen stapeln“*
- *„Navigiere zum Piraten Missionsgeber“*
- *„Navigiere zum Piraten Missionsziel“*
- *„Wie viele Kills?“* / *„Kill Count“*

### Kopfgeldjagd & Konfliktzonen

- *„Jagdgebiet finden innerhalb von 100 Lichtjahren“* — ein System mit
  Ressourcenabbaustätten, aus Systemen, die du schon durchflogen hast.
- *„Journale nach Jagdgebieten durchsuchen“* — lernt RES-Systeme und Massaker-Missionsgeber aus
  deinen alten Journalen auf einen Schlag. Einmal nach der Installation ausführen.
- *„Dieses Jagdgebiet vergessen“*
- *„Finde eine Konfliktzone“* / *„wo ist der nächste Krieg?“*

Siehe [Piraten-Missionen](Pirate-Massacre-Mission-Tracking).

---

## 🧭 Navigation

Vega plottet Routen zum **Ergebnis einer Suche**, zu Orten, die sie schon kennt, oder zu
Oberflächenkoordinaten. Zu einem System, das du einfach laut nennst, kann sie **nicht**
navigieren — Namen sind genau das, woran die Spracherkennung am häufigsten scheitert, und ein
falscher Treffer schickt dich ans andere Ende der Blase. Nutze dafür *Navigation aus dem
Speicher* oder — für einen Ort, den du oft anfliegst — einen
[eigenen Befehl, der die Route für dich plottet](UI-Actions-Tab).

- *„Navigiere aus dem Speicher“* / *„aus Speicher einfügen“* — kopiere zuerst einen Systemnamen
  (von INARA, Spansh, aus einer Chatnachricht…) mit Strg+C; Vega öffnet die Galaxiekarte und
  plottet dorthin.
- *„Bring mich nach Hause“* / *„Heimatsystem setzen“*
- *„Navigiere zum Fleet Carrier“* / *„navigiere zum Squadron Carrier“*
- *„Navigiere zum nächsten Handelsstopp“*
- *„Navigation abbrechen“*

### Auf einem Planeten

- *„Navigiere zu Koordinaten Breite 12,5 Länge -40,2“* — Führung vom Orbit bis zur Stelle.
- *„Navigiere zur Landezone“* — zurück dorthin, wo dein Schiff gelandet ist.
- *„Zum nächsten Bio Sample navigieren“* / *„zur nächsten Probe“* — der nächste markierte
  Organismus.
- *„Codex Eintrag löschen“*

### Neutronen-Highway

- *„Berechne die Neutronenroute“* — kopiere vorher den Zielnamen aus der Galaxiekarte.
  Optionen: *„…Effizienz 60“*, *„…mit Supercharge“*.
- *„Nächster Neutronenstern“* — zum nächsten Neutronen-Wegpunkt plotten (oder lass das die
  Einstellung *Nächsten Neutronensprung automatisch plotten* im
  [Reiter Kommandant](UI-Commander-Tab) erledigen).
- *„Neutronensternroute löschen“*

### Orte finden

Jeder *Finde*-Befehl plottet eine Route zum Gefundenen und zeigt es im
[HUD-Overlay](UI-HUD-Overlay).

- *„Rohmaterialhändler finden“* / *„Datenhändler finden“* / *„hergestellte Materialien Händler
  finden“*
- *„Human Tech Broker finden“* / *„Guardian Tech Broker finden“*
- *„Nächste Vista Genomics finden“*
- *„Nächsten Interstellar Factor finden“* / *„wo kann ich mein Kopfgeld abbezahlen“*
- *„Tankstelle finden“* / *„ich brauche Treibstoff“*
- *„Nächsten Fleet Carrier finden“*
- *„Brain Trees innerhalb von 500 Lichtjahren finden“*
- *„Wo kann ich Painit innerhalb von 200 Lichtjahren abbauen“* / *„Abbauort finden“*
- *„Dieses System aus Suchen ausschließen“* (oder *„Zielsystem aus Suchen ausschließen“*) — wenn
  eine Suche dich immer wieder an einen Ort schickt, der nicht funktioniert. *„Dieses System
  wieder zulassen“*, gesagt aus dem System heraus, macht es rückgängig.

---

## 💰 Handel & Märkte

- *„Wo kann ich [Ware] kaufen?“* — funktioniert auch für Schiffsmodule; sag *nächste* oder
  *bester Preis*.
- *„Wo kann ich [Ware] verkaufen?“*
- *„Handelsroute berechnen“* — nutzt das Handelsprofil dieses Schiffs.
- *„Route monetarisieren“* — eine lohnende Fracht für die Reise, auf der du ohnehin bist.
- *„Aktuelle Handelsroute“* / *„aktueller Handelsplan“*
- *„Navigiere zum nächsten Handelsstopp“*
- *„Handelsroute abbrechen“*
- *„Lokale Märkte“* · *„Stationsdetails“* / *„welche Services hier“* · *„Outfitting“* · *„Werft“* /
  *„Schiffe zum Verkauf“*
- *„Was ist im Frachtraum?“*

### Handelsprofil

Auch pro Schiff in den ⚙-Einstellungen im [Reiter Kommandant](UI-Commander-Tab) editierbar.

- *„Handelsprofil“* — das aktuelle beschreiben.
- *„Handelsprofil Startbudget ändern 5 Millionen“*
- *„Handelsprofil maximale Stopps ändern 4“*
- *„Handelsprofil maximale Entfernung ändern 1000“*
- *„Verbotene Waren erlauben / verbieten“*
- *„Planetenhäfen erlauben“* / *„planetare Häfen verbieten“*
- *„Erlaubnissysteme erlauben“* / *„Permit Systeme verbieten“*
- *„Festungen erlauben“* / *„Strongholds verbieten“*

Siehe [Handel & Profit](TradeRoutePlotting) und [Galaxie erkunden](Search-galaxy-with-EliteIntel).

---

## 🏗️ Kolonisierung

- *„Baustellenware finden“* — was die Baustelle noch braucht, wo du es kaufst und wie du den
  Laderaum füllst.
- *„Baufortschritt“* / *„wie läuft der Bau?“*
- *„Bring mich zurück zur Baustelle“*
- *„Baustelle vergessen“* — nicht mehr verfolgen.

---

## 🛰️ Flottenträger

Sag *Carrier* — oder *Squadron Carrier* —, sonst glaubt Vega womöglich, du meinst das Schiff.

- *„Carrier Status“* — Treibstoff, Reichweite mit aktuellem Tritium, Finanzen.
- *„Carrier Treibstoffreserve setzen 200“* — zurückgehaltenes Tritium.
- *„Fleet Carrier Route berechnen“* — kopiere vorher den Zielnamen aus der Galaxiekarte.
- *„Carrier Ziel eingeben“* — bei geöffneter Galaxiekarte des Trägers tippt Vega die nächste
  Etappe ein und bestätigt sie.
- *„Carrier Route“* / *„Carrier Sprungroute“*
- *„Carrier ETA“* / *„wann kommt der Carrier an?“*
- *„Entfernung zum Carrier“*
- *„Carrier Route abbrechen“*
- *„Carrier im System“*

---

## 🌠 Erkundung & Exobiologie

- *„Wo sind wir?“* — aktueller Standort.
- *„Entfernung zur Bubble“* / *„Entfernung zu Sol“*
- *„Entfernung zum Planeten [Name]“*
- *„Letzter Scan“* — der zuletzt gescannte Körper.
- *„Planeten im System“* / *„landbare Planeten“*
- *„Signale im System“* · *„Geosignale“*
- *„Systemsicherheit“* / *„wer kontrolliert das System?“*
- *„FSD Ziel Info“* / *„Info zum nächsten Sprung“* — das System analysieren, in das du gleich
  springst.
- *„Geplante Route“* / *„verbleibende Sprünge“*
- *„Explorationsgewinn“* — was deine Scans wert sind.
- *„Planetenmaterialien“* — was auf diesem Körper ist.
- *„Bio Signale im System“* · *„Exobiologie Proben“* · *„Biom analysieren“*
- *„Entfernung zur letzten Bio Probe“*
- *„Wir haben diesen Körper bereits beprobt“* — als erledigt markieren, wenn du ihn vor der
  Installation beprobt hast.

### Exo-Meisterschaft

Sobald der Katalog im [Reiter Kommandant](UI-Commander-Tab) aktiviert ist:

- *„Bring mich zum nächsten Exobiologie-Ziel“* — das reichste System, das du noch nicht
  abgeerntet hast.
- *„Markiere dieses System als abgeerntet“* — ein ganzes System abschreiben, das du schon
  gemacht hast.

Siehe [Entdeckung & Exobiologie](Discovery-Assistance).

---

## ⛏️ Bergbau

- *„Abbauziel hinzufügen Painit“* / *„Abbauziel entfernen Painit“* / *„Abbauziele löschen“*
- *„Mining Ansagen an“* — Prospektortreffer für deine Ziele.
- *„Wo kann ich [Material] abbauen?“*

Mit gesetzten Zielen und eingebauter Raffinerie zeigt das [HUD-Overlay](UI-HUD-Overlay) eine
Bergbau-Karte.

---

## 👤 Kommandant & Schiff

- *„Spielerprofil“* — Ränge und Fortschritt.
- *„Schiffsausrüstung“* / *„Schiff Loadout“*
- *„Materialinventar“* / *„wie viel [Material] haben wir?“*

---

## 📺 Panels & Karten

Sag den Panelnamen, gern mit *anzeigen*:

- *Navigation* · *Transaktionen* · *Kontakte* · *Chat / Kommunikation* · *Posteingang* ·
  *Social Panel* · *Verlauf* · *Staffel* · *Status* · *Radar*
- *Commander Panel* (*Kneeboard*) · *Crew Panel* · *Home Panel* · *Module* · *Feuergruppen* ·
  *Inventar* · *Lager* · *Jäger Panel*
- *Carrier Management*
- *Galaxiekarte* · *Systemkarte*
- *„Nächstes / vorheriges Panel“*, *„nächste / vorherige Seite“* — Reiter in einem Panel
  durchschalten.
- *„Schließen“* / *„Panel schließen“* / *„Karte schließen“* — zurück zum HUD.

---

## 🎵 Musik

*„Musik abspielen“*, *„Musik pausieren“*, *„nächster Titel“*, *„vorheriger Titel“*, *„Playlist
neu starten“*, *„Musik zufällig abspielen“*, *„spiele das Lied [Titel]“*. Siehe
[Reiter Jukebox](UI-Jukebox-Tab).

---

## Deine eigenen Befehle

Was fehlt, baust du dir selbst: [Aktionen → Eigene Befehle](UI-Actions-Tab). Eigene Befehle
werden durch deine eigenen Phrasen ausgelöst, gesprochen oder im Chat getippt, genau wie die
integrierten.

---

Fly Dangerous, Kommandant! o7

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈 | Open Source [**GitHub**](https://github.com/SudoKrondor/EliteIntel) | [YouTube](https://www.youtube.com/@SudoKrondor) | [Twitch](https://www.twitch.tv/sudokrondor) | Creative Commons License |
