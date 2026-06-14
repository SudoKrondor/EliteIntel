# EliteIntel KI-Abfragen & Befehle – Leitfaden (Deutsch)

Hey Commander! Dies ist eine Referenz für die Arten von Dingen, die du deinem **Elite Intel**-Assistenten sagen oder fragen kannst.
**Idealerweise musst du dir nichts davon merken** – sprich einfach natürlich, und die App erkennt, was du meinst. Diese Liste zeigt dir, was möglich ist, nicht was du auswendig lernen musst.

## Erstmal das Wichtigste. Probleme?

- Wenn die App zufällige Befehle ausführt: Brain-Problem (LLM zu schwach oder fehlerhafte Konfiguration)
- Wenn die App Befehle ausführt, diese aber nicht aktiviert werden: Hands-Problem (Tastenbelegungen)
- Wenn die App dich schlecht hört: Ears-Problem (lauter Raum, schlechter Noise-to-RMS-Abstand, Audio nicht kalibriert usw.)
- Wenn die App nicht spricht: Mouth-Problem. Audio-Routing prüfen (Betriebssystemebene).

### [Vollständiges Wiki](https://github.com/SudoKrondor/EliteIntel/wiki)

## Audio-Eingang

**Kalibriere das Audio in der App.** Wenn der Unterschied zwischen Noise Floor und RMS zu gering ist (z. B. unter 400), kann die App dich schlecht verstehen. Ein guter Wert ist mindestens 800–1000. Lautsprecher und Mikrofon vertragen sich nicht. Kopfhörer mit Mikrofon werden empfohlen.

## App

- Fehlende oder nicht belegte Tastenbindungen prüfen: **„key bindings prüfen"** / **„fehlende tastenbelegung"**
- Schlafen / Aufwachen: **„schlaf"** / **„wach auf"**
  (Im Schlafmodus werden Spracheingaben ignoriert. Bypass: **„hör mir zu [Befehl]"**)

## Erkundung & Position

- Entdeckungsansagen aktivieren/deaktivieren: **„entdeckungsansagen"**
- Wo sind wir gerade? **„aktueller standort"** / **„wo sind wir"** / **„in welchem system sind wir"**
- Wie weit bis zur Bubble / zum Fleet Carrier / zum letzten Bio-Sample? **„entfernung zur bubble"** / **„entfernung zum carrier"** / **„entfernung zur letzten bio probe"**
- Wie weit bis zu einem bestimmten Planeten, Mond oder einer Station? **„entfernung zum planeten [Name]"**
- Welche Materialien sind auf diesem Planeten verfügbar? **„planetenmaterialien"** / **„materialien hier"**
- Den letzten Scan / Bodendaten analysieren: **„letzter scan"**
- Was ist die ETA für unseren Fleet-Carrier-Sprung? **„carrier eta"** / **„wann kommt der carrier an"**
- Wie spät ist es / aktuelle UTC-Zeit? **„aktuelle zeit"** / **„wie spät ist es"**
- Biom des Sternsystems analysieren: **„biom analysieren"** / **„welches biom [Name]"**
- Welche Planeten benötigen noch Bio-Scans? **„bio signale im system"** / **„welche planeten müssen noch gescannt werden"**
- Welche Bio-Scans haben wir abgeschlossen? **„exobiologie proben"** / **„organik auf diesem planeten"**
- Landbare Planeten oder Monde im System? **„landbare planeten"** / **„stellare objekte"**
- Welche Signale gibt es in diesem System? **„signale im system"** / **„fss signale"**
- Welche Planeten haben Geo-Signale? **„geosignale"**
- FSS öffnen und scannen: **„system scannen"** / **„fss öffnen"** / **„discovery scan"**
- Systemsicherheit / Fraktionskontrolle: **„systemsicherheit"** / **„wer kontrolliert das system"**
- Spielerprofil / Ränge / Statistiken: **„spielerprofil"** / **„commander profil"**

## Exobiologie

- Welche Bio-Scans haben wir abgeschlossen? **„exobiologie proben"** / **„organik auf diesem planeten"**
- Erkundungsgewinnpotenzial in diesem System: **„explorationsgewinn"**
- Letztes Bio-Sample – Position und Entfernung: **„entfernung zur letzten bio probe"**
- Zum nächsten Bio-Sample / Codex-Eintrag navigieren: **„zum nächsten bio sample navigieren"** / **„zum codex eintrag navigieren"**
- Welche Organik ist auf diesem Planeten? **„organik auf diesem planeten"** / **„exobiologie proben"**
- Biom-Analyse für [Sternsystem / Planet]: **„biom analysieren"** / **„welches biom [Name]"**

## Fleet Carrier

Achte darauf, „Carrier" zu erwähnen, sonst könnte die App denken, du meinst dein Schiff!

- Carrier-Reichweite / Status / Treibstoff: **„carrier status"** / **„carrier reichweite"**
- Carrier-Treibstoffreserve setzen: **„carrier treibstoffreserve setzen [Menge]"**
- Wohin springt der Carrier? **„wohin fliegt der carrier"** / **„carrier nächster sprung"**
- Wie lange bis zur Carrier-Ankunft? **„carrier eta"** / **„wie lange bis carrier ankunft"**
- Wie lange können wir mit den aktuellen Mitteln operieren? **„wie lange kann der carrier betrieben werden"**
- Wie weit kann der Carrier mit dem aktuellen Tritium springen? **„carrier reichweite"**
- Was ist auf der Carrier-Route? **„carrier route"** / **„wie viele sprünge auf der carrier route"**
- Entfernung zum Fleet Carrier: **„entfernung zum carrier"**
- Fleet Carriers in diesem System? **„fleet carrier im system"** / **„carrier in der nähe"**

## Schiff & Systeme

- Routenansagen aktivieren/deaktivieren: **„routenansagen"**
- Ist der nächste Stern scoopable? **„geplante route"** / **„nächste tankbare sonne"**
- FSD-Ziel analysieren: **„fsd ziel info"** / **„ziel analysieren"**
- Was ist in meinem Frachtraum? **„was ist im frachtraum"** / **„was tragen wir"**
- Wie ist das Loadout? **„schiff loadout"** / **„schiffsmodule"**
- Route analysieren: **„geplante route"** / **„routenanalyse"**
- Treibstoffverfügbarkeit auf der Route: **„treibstoffverfügbarkeit auf der route"**
- Haben wir [Material]? **„material inventar [Name]"** / **„haben wir material [Name]"**
- Rang / Spielerprofil: **„spielerprofil"**

## Stationen & Märkte

- Welche Dienste gibt es auf lokalen Stationen? **„stationsdetails"** / **„welche services hier"**
- Outfitting / Schiffsteile / Module zum Verkauf? **„outfitting"** / **„verfügbare module"**
- Schiffe zum Verkauf? **„werft"** / **„schiffe zum verkauf"**
- Route monetarisieren: **„route monetarisieren"**
- Handelsroute berechnen: **„handelsroute berechnen"**
- Wo kann ich [Ware] kaufen/verkaufen? **„ware finden [Name]"** / **„wo kaufen"**
- Was ist auf dem lokalen Markt? **„lokale märkte"**
- Erinnerung setzen: **„erinnerung setzen [Text]"** / **„erinnere mich [Text]"**
- Aktuelle Handelsroute / Handelsplan: **„handelsroute"** / **„aktueller handelsplan"**

## Handelsprofil-Einstellungen

- Startbudget ändern: **„handelsprofil startbudget ändern [Betrag]"**
- Maximale Entfernung ändern: **„handelsprofil maximale entfernung ändern [X]"**
- Maximale Stopps ändern: **„handelsprofil maximale stopps ändern [N]"**
- Verbotene Waren erlauben: **„verbotene waren im handelsprofil erlauben"**
- Planetare Häfen erlauben: **„planetare häfen im handelsprofil erlauben"**
- Permit-Systeme erlauben: **„permit systeme im handelsprofil erlauben"**
- Strongholds erlauben: **„strongholds im handelsprofil erlauben"**
- Handelsprofil anzeigen: **„handelsprofil"** / **„handelseinstellungen"**
- Handelsrouten-Parameter auflisten: **„handelsroutenparameter auflisten"**

## ⚔️ Kampf & Missionen

- Radarkontakt-Ansagen: **„radarkontakt ansagen"**
- Jagdgebiet finden: **„jagdgebiet finden [System]"**
- Jagdgebiet erkunden: **„jagdgebiet erkunden"**
- Jagdgebiet ignorieren: **„jagdgebiet ignorieren"**
- Jagdgebiet bestätigen: **„jagdgebiet bestätigen"**
- Zum Missionsgeber-System navigieren: **„navigiere zum missionsgeber system"**
- Zum Piraten-Missionsgeber navigieren: **„navigiere zum piraten missionsgeber"**
- Zur aktiven Mission navigieren: **„navigiere zur aktiven mission"**
- Wie viele Piraten-Kills noch? **„piratenmission"** / **„wie viele kills"**
- Massacre-Missionsfortschritt: **„massacre mission fortschritt"** / **„verbleibende kills"**
- Welche aktiven Missionen habe ich? **„aktive missionen"**
- Gesamte Kopfgelder dieser Sitzung: **„kopfgelder"** / **„bounty gesammelt"**
- Subsystem anvisieren: **„ziel fsd"** / **„ziel triebwerke"** / **„ziel energieverteiler"** / **„ziel kraftwerk"** / **„ziel lebenserhaltung"**
- Wingman anvisieren: **„wingman eins anvisieren"** / **„wingman zwei anvisieren"** / **„wingman drei anvisieren"**
- Prioritätsziel / höchste Bedrohung: **„prioritätsziel"** / **„höchste bedrohung anvisieren"** / **„feind auswählen"**

## 🧭 Navigationsbefehle

- Zu Koordinaten navigieren: **„navigiere zu koordinaten [Lat] [Lon]"**
- Zur Landezone navigieren: **„navigiere zur landezone"** / **„zurück zur lz"**
- Zum nächsten Bio-Sample navigieren: **„zum nächsten bio sample navigieren"**
- Nächsten Fleet Carrier finden: **„nächsten fleet carrier finden"**
- Zum Carrier navigieren: **„navigiere zum fleet carrier"** / **„zurück zum carrier"**
- Navigiere zu / fliege zu / Kurs auf [Ziel]: **„navigiere zu [Ziel]"** / **„fliege zu [Ziel]"** / **„kurs auf [Ziel]"**
- Nächste Handelsstop: **„navigiere zum nächsten handelsstopp"**
- Fleet-Carrier-Route berechnen: **„fleet carrier route berechnen"**
- Carrier-Ziel eingeben: **„carrier ziel eingeben"**
- Handelsroute berechnen: **„handelsroute berechnen"**
- Nächste Vista Genomics finden: **„nächste vista genomics finden"**
- Nächsten Human / Guardian Tech Broker finden: **„human tech broker finden"** / **„guardian tech broker finden"**
- Nächsten Material-Trader finden: **„rohmaterialhändler finden"** / **„datenhändler finden"** / **„hergestellte materialien händler finden"**
- Brain Trees finden: **„brain trees finden [max Distanz]"**
- Mining-Standort finden: **„mining site finden [Material]"**
- Tritium-Mining-Standort finden: **„tritium mining site finden"**
- Wo kann ich [Ware] kaufen? **„ware finden [Name]"**
- Jagdgebiet finden: **„jagdgebiet finden"**
- Navigation abbrechen: **„navigation abbrechen"** / **„route abbrechen"**
- Route nach Hause planen: **„bring mich nach hause"** / **„fliege nach hause"**
- Als Heimatsystem setzen: **„heimatsystem setzen"**
- Optimale Geschwindigkeit setzen: **„optimale geschwindigkeit setzen"**
- Geschwindigkeit erhöhen / verringern um [X]: **„geschwindigkeit erhöhen um [X]"** / **„geschwindigkeit verringern um [X]"**
- Carrier-Treibstoffreserve setzen: **„carrier treibstoffreserve setzen [Menge]"**
- FSD-Ziel auswählen: **„fsd ziel auswählen"**
- Wing Nav Lock: **„wing nav lock"**
- Aus dem Speicher navigieren: **„navigiere aus dem speicher"**

## 🎮 Schiffsbedienung

- Fahrwerk ausfahren / einfahren: **„fahrwerk"** / **„fahrwerk einfahren"**
- Hardpoints ausfahren / einfahren: **„waffen ausfahren"** / **„waffen heiß"** / **„waffen einfahren"**
- Heat Sink abwerfen: **„heat sink abwerfen"**
- SRV ausfahren / Bergen: **„srv ausfahren"** / **„srv bergen"**
- Schiff betreten: **„zurück ins schiff"**
- Aussteigen: **„aussteigen"**
- Frachtluke: **„frachtschaufel öffnen"** / **„frachtluke schließen"**
- Landeerlaubnis anfragen: **„landeerlaubnis anfragen"** / **„docking anfragen"**
- Schiff starten: **„schiff starten"** / **„abdocken"**
- Autopilot-Landung: **„autolandung"** / **„taxi zur landung"**
- Supercruise aktivieren: **„in supercruise gehen"**
- Hyperraumsprung: **„sprung in den hyperraum"** / **„sprung"**
- Supercruise verlassen: **„aus dem supercruise fallen"** / **„raus hier"**
- Voller Stopp: **„voller stopp"** / **„triebwerke stoppen"**
- Geschwindigkeit auf viertel / halb / drei viertel / voll: **„viertel schub"** / **„halber schub"** / **„drei viertel schub"** / **„voller schub"**
- Energie auf Schilde / Triebwerke / Waffen. Ausgleichen: **„energie auf schilde"** / **„energie auf triebwerke"** / **„energie auf waffen"** / **„energie ausgleichen"**
- Kampfmodus / Analysemodus: **„in den kampfmodus wechseln"** / **„in den analysemodus wechseln"**
- Nachtsicht: **„nachtsicht"**
- Scheinwerfer: **„scheinwerfer"**
- Fahrassistenz (SRV): **„fahrassistenz"**
- Schiff wegschicken: **„schiff wegschicken"** / **„schiff in den orbit"**
- Zurück zur Oberfläche: **„zur oberfläche zurückkehren"** / **„hol mich ab"**
- FSS öffnen: **„system scannen"** / **„fss öffnen"** / **„honk"**
- Galaxiekarte / Systemkarte: **„galaxiekarte öffnen"** / **„systemkarte öffnen"**
- Panel schließen: **„panel schließen"** / **„raus"**
- TTS unterbrechen: **„unterbrich"** / **„hör auf zu reden"**
- Aktivieren: **„aktivieren"**

## 🎙️ Jäger-Befehle

- Jäger starten: **„jäger starten"**
- Jäger soll Schiff verteidigen: **„jäger verteidige das schiff"** / **„jäger defensiv"**
- Jäger soll mein Ziel angreifen: **„jäger greife mein ziel an"** / **„ziel fokussieren"**
- Jäger soll Feuer einstellen: **„jäger feuer einstellen"**
- Jäger zurück zum Schiff: **„jäger zurück zum schiff"** / **„jäger zurückrufen"**
- Jäger Feuer frei: **„jäger feuer frei"** / **„nach eigenem ermessen angreifen"**

## 📺 UI-Panels

Sage **„anzeigen"**, **„öffnen"** oder **„öffne"** gefolgt vom Panel-Namen:

- Navigationspanel: **„navigation anzeigen"**
- Transaktionspanel: **„transaktionen anzeigen"**
- Kontaktepanel: **„kontakte anzeigen"**
- Chat / Comms-Panel: **„chat anzeigen"**
- E-Mail-Posteingang: **„posteingang anzeigen"**
- Social-Panel: **„social panel anzeigen"**
- Verlaufspanel: **„verlauf anzeigen"**
- Staffel-Panel: **„staffel anzeigen"**
- Status-Panel: **„status anzeigen"**
- Commander-Panel / Kneeboard: **„commander panel anzeigen"**
- Crew-Panel: **„crew panel anzeigen"**
- Home-Panel: **„home panel anzeigen"**
- Module-Panel: **„module panel anzeigen"**
- Feuergruppen: **„feuergruppen anzeigen"**
- Inventar-Panel: **„inventar anzeigen"**
- Lager-Panel: **„lager anzeigen"**
- Jäger-Panel: **„jäger panel anzeigen"**
- Carrier-Verwaltung: **„carrier management anzeigen"**
- Galaxiekarte: **„galaxiekarte öffnen"**
- Systemkarte: **„systemkarte öffnen"**
- Servicepanel (SRV angedockt): **„services panel anzeigen"**

Weitere Panel-Befehle:
- Panel schließen: **„panel schließen"** / **„raus"** / **„verlassen"**

## ⚙️ App & Sitzungsbefehle

- **„schlaf"** / **„ignoriere mich"** / **„nicht überwachen"** → App in den Schlafmodus versetzen
- **„wach auf"** → normales Zuhören fortsetzen
- **„hör mir zu [Befehl]"** → Bypass: leitet einen einzelnen Befehl durch, während die App schläft. Das Präfix wird vor Weiterleitung entfernt. Beispiel: *„hör mir zu, sprung in den hyperraum"*
- Erinnerung setzen: **„erinnerung setzen [Text]"** / **„erinnere mich [Text]"**
- Erinnerungen löschen: **„erinnerungen löschen"**
- Routenansagen: **„routenansagen"**
- Entdeckungsansagen: **„entdeckungsansagen"**
- Mining-Ansagen: **„mining ansagen"**
- Radio-Chatter: **„radio umschalten"**
- Alle Ansagen deaktivieren: **„alle ansagen umschalten"**
- Radarkontakt-Ansagen: **„radarkontakt ansagen"**
- Mining-Ziel hinzufügen: **„mining ziel hinzufügen [Material]"**
- Mining-Ziel entfernen: **„mining ziel entfernen [Material]"**
- Mining-Ziele löschen: **„mining ziele löschen"**
- Codex-Eintrag löschen: **„codex eintrag löschen"**

## 💬 Allgemeiner Chat (Konversationsmodus muss EIN sein)

Standardmäßig läuft die App im **Strikten Modus**: Eingaben, die keinem bekannten Befehl entsprechen, werden stillschweigend ignoriert. Dies ist beabsichtigt – es verhindert, dass STT-Rauschen und Hintergrundgespräche zufällige Aktionen auslösen.

Aktiviere den **Konversationsmodus** im Einstellungs-Tab, um freies Gespräch zu ermöglichen. Wenn aktiviert, fällt alles ohne Befehlstreffer auf allgemeines Gespräch zurück – Spiellore, reale Themen, Schiffsbauten, alles.

Lokale LLMs antworten, wirken aber steif. Cloud-LLMs (Claude, OpenAI, xAI, Mistral, Deepseek) werden für Gespräche empfohlen.

---

[Weitere Befehle hier](https://github.com/SudoKrondor/EliteIntel/wiki/Obscure-System-Commands)

Fly Dangerous, Commander! o7

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈 | Open Source [**GitHub**](https://github.com/SudoKrondor/EliteIntel) | [YouTube](https://www.youtube.com/@SudoKrondor) | [Twitch](https://www.twitch.tv/sudokrondor) | Creative Commons License |
