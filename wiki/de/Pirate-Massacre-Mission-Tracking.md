# Verfolgung von Piraten-Massaker-Missionen

EliteIntel verfolgt Piraten-Massaker-Missionen, einschließlich Kill-Zählungen, Zielfraktionen und Auszahlungsberechnungen. Es überwacht Kills und Auszahlungen während der gesamten Sitzung.

## Erste Schritte

Um Piraten-Massaker-Missionen effizient abzuschließen:

1. **Finde ein Set**: Frage ``finde piraten massaker missionen`` (siehe *Massaker-Sets finden* unten) oder suche eines auf
   [INARA](https://inara.cz): ein System, in dem mehrere Fraktionen Missionen gegen dieselbe Piratenfraktion anbieten,
   neben einem System mit einem Ressourcenabbaugebiet (RES). Nimm so viele Missionen gegen diese Fraktion an wie
   möglich. EliteIntel verfolgt die Zielfraktion, die Kill-Anzahl und die Auszahlungsdetails.
2. **Missionsstatus abfragen**: Nachdem du Missionen angenommen hast, stelle EliteIntel Fragen wie ``Wie viele Kills sind noch übrig für Piratenmissionen?`` oder ``Was ist die Auszahlung des Piraten-Massakers?`` EliteIntel gibt die Gesamtzahl der benötigten Kills, potenzielle Credits und eine fraktionsweise Aufschlüsselung zurück. Gib an, dass die Anfrage piratenbezogen ist, um Verwechslungen mit anderen Missionstypen zu vermeiden.

## Piraten im Haz RES bekämpfen

1. **Zum Standort fliegen**: Fliege zum Zielsystem und betrete das Haz RES. Scanne nahegelegene Schiffe.
2. **Zielidentifikation**: EliteIntel gibt folgendes bekannt, wenn Schiffe gescannt werden:
    - **Nicht-missions-bezogene Piraten**: ``Legales Ziel, [Schiffstyp], [Pilotenrang], [Kopfgeldprämie].`` Diese Schiffe sind gültige Ziele, zählen aber nicht für Missionsziele.
    - **Missionspiraten**: ``Missionsziel! [Schiffstyp], [Pilotenrang], [Kopfgeldprämie].`` Dies sind die Hauptziele.
    - EliteIntel gibt keine sauberen oder freundlichen Schiffe bekannt, wodurch die Audioausgabe relevant bleibt.
3. **Kill-Bestätigung**: Wenn ein Pirat eliminiert wird, gibt EliteIntel ``Kill bestätigt`` zusammen mit dem verdienten Kopfgeld bekannt. Kills von Missionszielen werden als ``Missions-Kill bestätigt`` mit der zugehörigen Auszahlung bekannt gegeben.
4. **Fortschrittsabfragen**: Frage jederzeit ``Wie viele Kills sind noch übrig?``. EliteIntel gibt die verbleibenden Kills insgesamt und eine fraktionsweise Aufschlüsselung zurück.


## Bewährte Vorgehensweisen

_**Löse alle Kopfgelder ein, bevor du deine privaten Massaker-Missionen aufnimmst**_  ein bestätigter Kill ist ein Kopfgeldgutschein gegen die Fraktion

- **Missions-Stacking**: Das Annehmen mehrerer Missionen gegen dieselbe Fraktion erhöht die Credit-Effizienz. Frage
  EliteIntel nach dem tiefsten Stapel in der Nähe (siehe *Massaker-Sets finden* unten).
- **Natürlichsprachliche Abfragen**: Natürliche Sprache wird akzeptiert. ``Wie viele Piraten sind noch zu töten?`` und ``Piraten-Missionspunktzahl?`` sind beide gültig. Gib an, dass die Anfrage piratenbezogen ist, um Verwechslungen mit anderen aktiven Missionen zu vermeiden.
- **Persönlichkeitswechsel**: Wechsle je nach Bedarf zwischen Persönlichkeitsmodi, um deinen aktuellen Spielstil zu unterstützen.


---

# Massaker-Sets finden

Ein **Massaker-Set** besteht aus einem Missionsgeber und einem Jagdgebiet: Fraktionen in einem System bieten
Piraten-Massaker-Missionen gegen eine Piratenfraktion in einem nahen System an, und dieses System hat ein
Ressourcenabbaugebiet (RES), in dem gejagt wird. Je mehr Fraktionen im Missionsgeber-System Aufträge vergeben, desto
tiefer der Stapel: Ihre Missionen laufen nebeneinander, sodass dieselben Abschüsse mehrfach bezahlt werden.

## Wie EliteIntel Sets lernt

EliteIntel lernt Sets aus deinen eigenen Flügen. Keine Website und kein Konto sind nötig.

- **Missionen, die du annimmst.** Wenn du eine Piraten-Massaker-Mission annimmst, merkt sich EliteIntel das
  Missionsgeber-System, die Station, die Fraktion und das Ziel.
- **Ressourcenabbaugebiete, die du siehst.** Wenn du in einem System ankommst, listet das Spiel seine Signalquellen
  auf, Ressourcenabbaugebiete eingeschlossen. EliteIntel erfährt außerdem von den Ressourcenabbaugebieten, die die
  Spiele anderer Kommandanten an EDDN melden, das öffentliche Datennetz von Elite Dangerous. So kennt es auch Systeme,
  in denen du nie warst.

Ein Set braucht beide Hälften. Eine Mission gegen ein System ohne bekanntes Ressourcenabbaugebiet bleibt gespeichert,
wird aber erst angeboten, wenn dort ein Ressourcenabbaugebiet gesehen wurde.

**Fang mit deiner Geschichte an.** Deine alten Journale enthalten Jahre davon. Nutze **Alte Journale importieren** im
[Reiter Kommandant](UI-Commander-Tab.html) oder sage ``journale nach jagdgebieten durchsuchen``.

## Nach einem Set fragen

| Sage | Was passiert |
|---|---|
| ``finde piraten massaker missionen innerhalb von 50 lichtjahren`` | Vega nennt das beste Set in Reichweite: das Missionsgeber-System und wie viele Fraktionen dort Aufträge vergeben, die Piratenfraktion und das Zielsystem, seine Ressourcenabbaugebiete und welche Stationen du anfliegen sollst. Tiefere Stapel zuerst, dann nähere. Ohne Reichweite sucht es 100 Lichtjahre weit. |
| ``navigiere zum piraten missionsgeber`` | Plant eine Route zu einem Missionsgeber: zuerst zu einem für die Missionen, die du schon hast, dann zu einem für das Jagdgebiet, in dem du bist, dann zum besten Set in der Nähe. Eine Erinnerung nennt das Zielsystem. |
| ``navigiere zum piraten missionsziel`` | Plant eine Route zum Jagdgebiet der Missionen, die du hast. |
| ``jagdgebiet finden`` | Das nächste Ressourcenabbaugebiet, das sich allein für Kopfgelder lohnt, ohne Missionen. |
| ``dieses jagdgebiet vergessen`` | In einem Jagdgebiet gesagt, das dir nicht gefällt (zu leicht, zu schwer, zu weit vom Stern, die falsche Macht). Vega fragt zuerst und bietet es danach nie wieder an, auch kein Set, das dich dorthin schickt. |

## Sets mit anderen Kommandanten teilen

Das Teilen ist aus, bis du es auf der Seite **Massaker-Sets** im [Reiter Kommandant](UI-Commander-Tab.html) einschaltest.

Es gilt in beide Richtungen: **Solange du teilst, bekommst du auch die Sets, die andere Kommandanten geteilt haben.**
Je mehr Kommandanten teilen, desto mehr Sets gibt es für alle.

**Was geteilt wird:** für jedes Set die Namen von Sternsystemen, Fraktionen und Stationen, wie viele
Ressourcenabbaugebiete jeder Stufe das Ziel hat, und der Tag, an dem das Set zuletzt gesehen wurde. Jeder Kommandant
auf deinem Computer zählt mit, auch importierte.

**Was nie geteilt wird:** dein Kommandantenname, deine Missionen, Belohnungen oder sonst etwas, das verrät, wer du
bist. Siehe die [Datenschutzrichtlinie](Privacy.html).

Was mit den Sets passiert:

1. Deine Sets werden hochgeladen, wenn du das Teilen einschaltest, und danach höchstens einmal am Tag, wenn du neue
   gelernt hast.
2. Jede Nacht werden die Sets aller Kommandanten mit Galaxiedaten abgeglichen. Die Systeme müssen existieren, das Ziel
   darf höchstens 20 Lichtjahre vom Missionsgeber entfernt sein, und die Piratenfraktion muss wirklich im Zielsystem
   sein und eine Anarchie sein. Sets, die durchfallen, werden verworfen. Der Rest wird veröffentlicht, ohne zu sagen,
   wer ihn geschickt hat.
3. Einmal am Tag lädt EliteIntel die veröffentlichten Sets herunter. Wenn du nach einem Set fragst, werden sie neben
   deinen eigenen angeboten.

**Ein geteiltes Set wird nur angeboten, wenn deine eigenen Daten ein Ressourcenabbaugebiet in seinem Ziel zeigen**,
aus deinen Besuchen oder von EDDN. Das ist das Einzige, was Galaxiedaten nicht prüfen können. Taucht ein geteiltes Set
nicht auf, flieg zu seinem Zielsystem oder warte, bis EDDN es meldet.

**Vega sagt dir, wenn ein Set von anderen Kommandanten stammt.** Betrachte es als Hinweis, den du prüfst, nicht als
etwas, das du selbst geflogen bist. Gefällt es dir nicht, funktioniert ``dieses jagdgebiet vergessen`` genauso wie bei
deinen eigenen Sets.

**Deine eigenen Daten gehen vor.** Wenn du ein Set selbst kennst, wird deine Version verwendet, und bei gleich tiefen
Sets werden deine vor gemeldeten angeboten.

Wenn du nicht teilst und EliteIntel kein Set in der Nähe kennt, sagt dir Vega, wenn andere Kommandanten in der Gegend
welche gemeldet haben.

Schaltest du das Teilen aus, werden deine Sets aus den geteilten Daten entfernt, und EliteIntel nutzt die der anderen
nicht mehr.

----
Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
