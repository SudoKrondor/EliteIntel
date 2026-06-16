# Handelsrouten-Planung

Elite Intel berechnet Handelsrouten und leitet Frachtoperationen Abschnitt für Abschnitt. Sag „Berechne mir eine profitable Handelsroute", um zu beginnen.

### Wie die Handelsrouten-Berechnung funktioniert

[[youtube:lQU5wnCS0rQ]]


1. **Frachtkapazität ist erforderlich.**
   Wenn das aktuelle Schiff keine Frachtkapazität hat, fordert die App dich auf, zu einem Schiff mit mindestens einem Frachtgestell zu wechseln.

2. **Handelsprofil (pro Schiff)**
   Jedes Schiff hat sein eigenes Handelsprofil. Richte es einmalig ein. Elite Intel behält das Profil für zukünftige Sitzungen.

3. **Profil einrichten oder ändern** (nur Sprachbefehle)
   Sag „Handelsprofil ändern" gefolgt von dem Parameter, den du ändern möchtest.

   **Beispiel für die Ersteinrichtung** (Mindestanforderung):
    - „Handelsprofil ändern, Startkapital zwanzig Millionen"
    - „Handelsprofil ändern, maximale Entfernung vom Stern sechstausend Lichtsekunden"
    - „Handelsprofil ändern, maximale Stopps acht"

   > Hinweis: Sag „maximale Stopps acht", nicht „auf acht". Die Spracherkennung kann Präpositionen als Zahlen transkribieren, was zu einer falschen Stoppanzahl führt.

   Optionale Parameter:
    - „Handelsprofil ändern, Erlaubnissysteme zulassen"
    - „Handelsprofil ändern, Planetenhäfen zulassen"
    - „Handelsprofil ändern, Flottenträger zulassen" (Hinweis: Die Position des Flottenträgers kann sich vor der Ankunft ändern)
    - „Handelsprofil ändern, verbotene Waren zulassen"

4. **Route berechnen**
   Sag „Handelsroute berechnen" oder „Profitable Handelsroute planen".
   Die Berechnung dauert in der Regel 30 bis 60 Sekunden.

5. **Abschnitt für Abschnitt navigieren**
   Sag „Route zum nächsten Handelsstopp planen", wenn du bereit bist. Synonyme für den Stationstyp werden akzeptiert. Der Befehl erfordert **Planen** + **Handels** + ein Stations-Synonym.

6. **Bei der Ankunft**
   Elite Intel gibt die Andockstation und die zu kaufende oder zu verkaufende Ware bekannt.
   Im Zweifelsfall frage „Was kaufe ich hier?" oder „Wo verkaufe ich diese Fracht?" Gib Kaufen oder Verkaufen an, um Mehrdeutigkeiten zu vermeiden.

7. **Nach dem Beladen**
   Sag erneut „Route zum nächsten Handelsstopp planen". Elite Intel plant die Route zum Verkaufsort mit der höchsten Gewinnspanne.

8. **Routenpersistenz**
   Die Route bleibt bestehen, bis sie abgeschlossen, überschrieben oder abgebrochen wird. Um eine neue Route zu starten, leere zuerst den Frachtraum.
   (Geplante Funktion: automatische Frachtliquidation an den Meistbietenden.)

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
