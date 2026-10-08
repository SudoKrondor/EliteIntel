# Kolonisierung

EliteIntel macht aus einer Kolonisierungsbaustelle eine Live-Einkaufsliste. Es verfolgt, was der Bau noch
braucht, findet heraus, wo du es kaufst, und plottet die Route dorthin. Kein Hin- und Herwechseln mehr
zwischen Markt-Webseiten und Tabellen.

Es entscheidet **nicht**, was oder wo gebaut wird. Das ist deine Sache, Architekt. Es hilft dir, den Berg an
Waren zusammenzutragen, den der Bau braucht.

[[youtube:PnIlVZRdhKE]]

## Einen Bau verfolgen

**Lande auf der Baustelle.** Das ist der Auslöser. Sobald du am Depot aufsetzt, schickt das Spiel das
vollständige Manifest der Baustelle, und diese Baustelle wird zu der, die EliteIntel verfolgt. Öffne den
Baubildschirm und das [HUD-Overlay](UI-HUD-Overlay) zeigt die Karte **BAUSTELLE**: Fortschritt,
Ausstehendes und was du beim nächsten Flug laden sollst.

Hinter deinem Rücken wird nicht die ganze Galaxie verfolgt. Die App kennt den Stand des Baus nur **von
deiner letzten Landung**. Andere Kommandanten können in deiner Abwesenheit zum selben Depot liefern, deshalb
nennen Karte und Vega das Alter der Daten, sobald sie älter als eine Stunde sind.

## Die nächste Ware finden

Frag ``Baustellenware finden`` (oder *Fracht für die Baustelle finden*, *Kolonisierungsfracht finden* usw.).

1. **Zuerst dein Carrier.** Hat dein Flotten- oder Staffelträger Waren an Bord, die der Bau braucht,
   schickt Vega dich vor jedem Markt dorthin. Ein Carrier in deinem aktuellen System gewinnt immer. Einer
   weiter weg muss die Sprünge wert sein.
2. **Dann der nächste Markt.** EliteIntel sucht Märkte innerhalb von zwei Sprüngen von deinem Schiff (nach
   seiner Sprungreichweite), dann innerhalb von vier Sprüngen, wenn nichts auftaucht. Raumstationen zuerst,
   planetare Siedlungen danach. Manche Waren, etwa CMM-Verbundstoff, gibt es nur in planetaren Siedlungen.
   Ja, du musst landen.
3. **Größter Fehlbestand zuerst.** Die Suche richtet sich nach der Ware, die dir am meisten fehlt (Stahl,
   Titan, Aluminium, was auch immer das größte Loch ist), und bevorzugt den Markt, der deinen Laderaum am
   besten mit weiteren Waren füllt, die der Bau braucht.

Ist ein Markt gefunden, öffnet EliteIntel die Galaxiekarte auf dem Stern. Die Route wird **nicht**
bestätigt. Das ist Absicht: Du entscheidest, ob du sie festlegst oder in die Systemkarte gehst, um die
eigentliche Siedlung zu finden.

**Neu installiert?** Spansh-Suchen brauchen einen Ausgangspunkt. Die App sucht zuerst in ihrer lokalen
Datenbank nach Stationen, an denen du angedockt hast. Gibt es keine, fragt sie Spansh nach der nächsten
Station. Dafür braucht sie deine galaktischen Koordinaten, und die erfährt die App erst nach einem
FSD-Sprung. Flieg also ein wenig mit laufender App herum und dock an ein paar Stationen an, bevor du ein
Bauprojekt beginnst. Je mehr du sie nutzt, desto mehr weiß sie.

## Am Markt

Wenn du andockst, sortiert das Overlay die Liste so, dass die Waren, **die diese Station verkauft**, oben
stehen.

- Eine teilweise geladene Ware erscheint grün mit den Tonnen an Bord (zum Beispiel ``16 T +44``).
- Sobald du genug von einer Ware hast, verschwindet sie aus der Liste und die nächste ausstehende Ware
  rückt nach.
- Hast du alles gekauft, was diese Station liefern kann, verschwindet der Stationsname von der Karte. Es
  gibt noch Waren zu kaufen, nur nicht hier. Frag erneut ``Baustellenware finden`` und Vega schickt dich zum
  nächsten passenden Markt.

Abliefern oder auf dem Carrier lagern, dann wiederholen, bis der Bau fertig ist.

## Zurück zur Baustelle

Sag ``Bring mich zurück zur Baustelle``. Liegt der Bau in einem anderen System, wird die Route dorthin
geplottet. Bist du schon in seinem System, braucht es keine Route.

Frag ``Wie läuft der Bau?`` oder ``Wie viel müssen wir noch kaufen?`` für einen gesprochenen
Fortschrittsbericht.

## Der Haken mit dem Carrier

Frontier gibt die Fracht von Carriern nicht an Drittanbieter-Tools weiter. EliteIntel sieht deinen Vorrat
nur über den **Warenmarkt** des Carriers. So machst du ihn sichtbar:

1. **Sperr den Carrier ab**, damit niemand andocken und deinen Baustellenvorrat kaufen kann.
2. **Biete die Waren zum Verkauf an** im Markt des Carriers.
3. **Öffne den Markt des Carriers** über das Panel. In diesem Moment liest die App ihn ein.

Danach zählt EliteIntel mit, wenn du Fracht zwischen Carrier und Schiff umlädst und wenn du auf deinem
eigenen Carrier kaufst oder verkaufst. Es ist ein Workaround, aber mehr lässt das Spiel kaum zu.

## Mehrere Baustellen

Die Baustelle, auf der du zuletzt gelandet bist, ist die aktuelle. EliteIntel verfolgt im Overlay einen Bau
zur Zeit. Lande an einem anderen Depot und dieses übernimmt. Lande wieder am ersten und es ist wieder
aktuell.

Lust auf eine Pause vom Bauen? Sag ``Baustelle vergessen``. Vega bittet um Bestätigung, dann wird die Karte
still. Gelöscht wird nichts. Eine Landung auf der Baustelle holt sie zurück.

## Was die KI tut (und was nicht)

Die KI macht aus dem, was du sagst, Aktionen und entscheidet, was Vega antwortet. Verfolgung, Suche und
Routenplanung sind normaler Code, der dein Spiel-Journal liest. Sie spielt den Kolonisierungszyklus nicht
für dich und sieht nichts, was das Spiel nicht aufschreibt. Fliegen tust immer noch du. Die App erspart dir
das Surfen im Web.

Siehe [Alle Befehle](AllCommands) für die vollständige Liste der Kolonisierungsphrasen.
