# Reiter „Kommandant"

<img src="images/controller.png" class="inline" height="20" alt="Kommandant"> Wer du bist, was dein
Schiff automatisch für dich erledigt, worüber Vega ungefragt berichtet und mit welcher Stimme jeder
Rumpf deiner Flotte spricht.

![Reiter Kommandant](images/ui-tab-commander.png)

---

## Kommandantenprofil

**Kommandant-Name** — überschreibt deinen Namen im Spiel für die Sprachausgabe. Nutze das, wenn
Vega dein Handle verstümmelt oder du schlicht anders genannt werden möchtest. Wird gespeichert,
sobald du Enter drückst oder wegklickst.

> Der **Journal-Ordner** ist in V1.1 nach [Einstellungen → Allgemein](UI-Settings-Tab) gewandert,
> der **Belegungsordner** in den [Reiter Bindings](UI-Bindings-Tab).

---

## Schiffsoptionen

Automatisierungen, die Vega für dich ausführt. Jede ist ein einfacher Schalter, der sofort
durchschreibt. Für alle nützlich — und für Kommandanten mit Behinderung wirklich ermöglichend.

| Schalter | Funktion |
|--------|--------------|
| **Für FSD-Sprung automatisch beschleunigen** | Gibt vor einem Sprung Schub |
| **Lichter für FSD-Sprung automatisch ausschalten** | Schaltet die Schiffsbeleuchtung vor einem Sprung aus |
| **Nachtsicht für FSD-Sprung automatisch ausschalten** | Deaktiviert die Nachtsicht vor einem Sprung |
| **Waffen für FSD-Sprung automatisch einfahren** | Fährt die Hardpoints vor einem Sprung ein |
| **Fahrwerk für FSD-Sprung automatisch einfahren** | Zieht das Fahrwerk vor einem Sprung ein |
| **Frachtgreifer für FSD-Sprung automatisch einfahren** | Fährt den Greifer vor einem Sprung ein |
| **Fahrwerk beim Start automatisch einfahren** | Zieht das Fahrwerk nach dem Abheben ein |
| **UI automatisch verlassen, bevor ein anderes Panel geöffnet wird** | Schließt das offene Panel, bevor das nächste geöffnet wird, damit Panel-Befehle nicht kollidieren |
| **Lichter beim SRV-Aussetzen automatisch ausschalten** | Schaltet die Beleuchtung aus, wenn du den SRV aussetzt |
| **Jäger-Andocken bei FSD-Sprung anfordern und FTL abbrechen, falls Jäger außerhalb** | *Derzeit deaktiviert* — wartet auf einen Frontier-Fix für einen Nomad-bezogenen Fehler |

---

## Ansagen

Alles, was Vega ungefragt von sich aus meldet. Alle elf Schalter liegen jetzt an einem Ort, sodass
es einen einzigen Bildschirm gibt, den du prüfst, wenn etwas zu viel — oder zu wenig — redet.

![Ansagen](images/ui-commander-announcements.png)

| Schalter | Was du hörst |
|--------|---------------|
| **Entdeckungen ansagen** | Bemerkenswerte Himmelskörper, Erstentdeckungen, biologische Signale |
| **Routenfortschritt ansagen** | Wo du dich auf einer geplanten Route befindest |
| **Radarkontakte ansagen** | Schiffe, die auf dem Scanner erscheinen |
| **Bergbau ansagen** | Bergbauereignisse und Ausbeute |
| **Navigation ansagen** | Navigationsereignisse und Ankünfte |
| **Funkübertragungen** | Rollengetreues Funkgeplauder, in einer eigenen Funkstimme gesprochen |
| **Sprungziel ansagen** | Welches das nächste System ist |
| **Verkehr am Ziel ansagen** | Verkehrsmeldungen für dein Ziel |
| **Verluste am Ziel ansagen** | Jüngste Todesfälle im Zielsystem |
| **Verbleibende Sprünge ansagen** | Verbleibende Sprünge auf der Route |
| **Verfügbarkeit von Treibstoffsternen ansagen** | Ob das Ziel einen scoopbaren Stern hat |

Die ersten sechs lassen sich auch per Sprache umschalten, deshalb liest dieser Bildschirm sie neu
ein, sobald du den Reiter öffnest — ein gesprochenes `toggle all announcements` wird hier
widergespiegelt.

---

## Stimmenkonfiguration der Flotte

Eine Zeile je Schiff in deinem Besitz. Elite Intel entdeckt deine Flotte aus dem Spieljournal; du
fügst Schiffe nicht von Hand hinzu.

| Spalte | Anmerkungen |
|--------|-------|
| **Schiff** | Der von dir vergebene Schiffsname |
| **Schiffsmodell** | Der Rumpftyp |
| **Stimme** | Zum Auswählen klicken. Eine Änderung spielt sofort einen Demosatz in dieser Stimme ab, damit du sie anhören kannst |
| **Persönlichkeit** | `Professionell` · `Locker` · `Freundlich` · `Unberechenbar` · `Draufgängerisch` |
| **⚙** | Öffnet die Einstellungen dieses Schiffs |

**Zur Stimmenliste.** Schiffsstimmen sind weiblich. Welche Stimmen erscheinen, hängt von der unter
[Einstellungen → KI-Dienste](UI-Settings-Tab) gewählten Sprachausgabe ab:

- **Lokal (Kokoro)** — 53 Stimmen, beschriftet als `Name - Akzent`. Kein Schlüssel, kein Download,
  keine Einrichtung.
- **Cloud (Google)** — beschriftet als `Name - Akzent · HD` oder `· Standard`. Im Englischen
  unterscheidet der Akzent die Stimmen. In jeder anderen Sprache wird jede Stimme in dieser Sprache
  synthetisiert, daher zeigt die Beschriftung Geschlecht und Qualitätsstufe statt eines
  irreführenden englischen Akzents.

> Ein Wechsel der Sprachausgabe setzt die Stimme jedes Schiffs auf die Vorgabe der neuen Engine
> zurück. Die **Persönlichkeiten deiner Schiffe bleiben erhalten**. Die App warnt dich vorher.

---

## Schiffseinstellungen (die ⚙-Schaltfläche)

Einstellungen je Schiff, denn eine Bergbau-Python und eine Kampf-Corvette wollen nicht dasselbe
Verhalten.

![Schiffseinstellungen](images/ui-ship-settings.png)

**System beim Eintritt scannen** — führt bei der Ankunft in einem System einen Entdeckungsscan
durch. Wähle die **Feuergruppe** (A–H) und den **Auslöser** (1 oder 2), auf dem dein
Entdeckungsscanner liegt. Steht dein HUD im Kampfmodus, wechselt Elite Intel in den Analysemodus,
scannt und wechselt zurück.

**Materialhinweis bei hochwertigen Emissionen** — sagt dir, wenn ein Signal hochwertiger Emissionen
im System Materialien führt, für die sich ein Halt lohnt.

**Handelsprofil** — die Randbedingungen, an die sich Elite Intel hält, wenn es für dieses Schiff
eine Handelsroute plant. Jede davon lässt sich auch per Sprache setzen:
*„alter trade profile, set max stops to four"*.

| Einstellung | Bedeutung |
|---------|---------|
| **Planetare Häfen erlauben** | Oberflächenhäfen in Routen einbeziehen |
| **Verbotene Waren erlauben** | Fracht einbeziehen, die irgendwo auf der Route illegal ist |
| **Genehmigungspflichtige Systeme erlauben** | Systeme einbeziehen, die eine Genehmigung erfordern |
| **Flottenträger erlauben** | Flottenträger von Spielern als Märkte einbeziehen |
| **Hochburgsysteme erlauben** | Hochburgsysteme von Thargoiden/Mächten einbeziehen |
| **Max. Ls vom Eintritt** | Wie weit eine Station vom Ankunftsstern entfernt liegen darf |
| **Max. Stopps** | Anzahl der Etappen in der Route |
| **Startkapital** | Credits, die der Routenplaner ausgeben darf |

Wie Routen geflogen werden, steht unter [Handel & Profit](TradeRoutePlotting).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
