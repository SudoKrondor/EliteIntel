# Reiter „Bindings"

<img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> **Neu in V1.1.**
Belegungen waren eine Ecke des Reiters Aktionen; sie haben jetzt einen eigenen Reiter mit
vollwertigem Editor.

Elite Intel bedient dein Schiff, indem es die Tasten drückt, auf die Elite Dangerous belegt ist.
Hat eine Steuerung keine Tastaturbelegung, kann Elite Intel sie nicht nutzen — in diesem Reiter
findest du das heraus und behebst es.

Zwei Unterreiter: **Bindungsprofil** und **Bindungsverwaltung**.

---

## Bindungsprofil

![Bindungsprofil](images/ui-tab-bindings-profile.png)

### Welche Datei verwendet wird

**Profil** — automatisch erkannt. Elite Intel liest den aktiven `StartPreset`-Eintrag und greift
notfalls auf die neueste `.binds`-Datei zurück.

**Datei** — die `.binds`-Datei, die aktuell für Diagnose und Zuweisung genutzt wird.

**Belegungsordner** — optional. Lass ihn leer, dann wird der Standardort von Elite Dangerous
verwendet; setze ihn, wenn deine Installation an einem ungewöhnlichen Ort liegt.

Beide Felder haben eine ⓘ-Schaltfläche, die genau erklärt, wie der Wert zustande kam.

### Die Belegungstabellen

Zwei Tabellen: **Verwendete Belegungen** und **Fehlende Belegungen**, jeweils mit einer Anzahl.
Belegungen sind nach Kategorie gruppiert:

Schiff / Flug · Kampf · UI-Panels · Karten · Erkundung · Kamera · SRV · Zu Fuß · Sonstiges

| Spalte | Bedeutung |
|--------|---------|
| **Belegung** | Die Steuerung |
| **Primär** / **Sekundär** | Die zwei Plätze, die Elite Dangerous jeder Steuerung gibt |
| **Status** | `Fehlt` · `Keine Tastaturbelegung` (belegt, aber nur auf einem Controller) · `Nicht definiert` |
| **Schnellkorrektur** | Weist genau dieser einen Steuerung eine sichere freie Taste zu |
| **Löschen** | Entfernt die Tastaturbelegung und lässt Controller- und HOTAS-Belegungen unberührt |

> **HOTAS und Controller werden angezeigt, sind aber nicht bearbeitbar.** Elite Intel führt über
> Tastaturbelegungen aus, andere Geräte erscheinen daher nur zur Diagnose.

**Nur Konflikte anzeigen** filtert die Tabellen auf die Problemfälle.

### Konflikte

Elite Dangerous wertet einen Akkord nur dann als Konflikt, wenn es *exakt* derselbe Akkord ist —
`G` und `Shift+G` koexistieren problemlos. Elite Intel nutzt dieselbe Regel und meldet damit genau
das, was auch das Spiel meldet.

Konfliktzeilen sind eingefärbt; beim Überfahren erscheint **Teilt *Taste* mit:** samt Liste.

Du siehst eventuell auch **Schiff/SRV-Zwilling — viele belegen es gleich wie:** — kein Konflikt,
sondern ein Vorschlag. Manche Schiffs- und SRV-Steuerungen werden üblicherweise auf dieselbe Taste
gelegt.

### Eine Belegung bearbeiten

Klicke einen Platz an, um den Zuweisungsdialog zu öffnen.

![Taste zuweisen](images/ui-bindings-assign.png)

Er zeigt die gewählte Belegung, den Platz und den aktuellen Wert. Dann **klicke ins Feld und drücke
die gewünschten Tasten** — Modifikatoren und Taste zusammen. Esc bricht ab. Akkorde mit mehreren
Modifikatoren werden unterstützt.

Eine Live-Tastaturkarte zeigt, was verfügbar ist: **Halte Strg/Umschalt/Alt gedrückt, um die für
diese Kombination freien Tasten zu sehen — grün ist frei, rot ist bereits belegt.** Vom
Betriebssystem reservierte Tasten sind markiert und können nicht zugewiesen werden.

**Belegung löschen** entfernt die Zuweisung.

### Fehlende beheben

Eine Schaltfläche, die **jeder** Steuerung ohne Tastaturbelegung sichere, layoutfreundliche
Tastaturtasten zuweist.

- Bestehende Belegungen werden nie verändert.
- Keine Taste wird je doppelt verwendet.
- Die Änderungen gehen **nur in deinen Entwurf**.

Sie meldet, was sie getan hat, und was sie übersprungen hat und warum: beide Plätze bereits auf
einem Controller, keine freie sichere Taste mehr übrig, oder kein Platz, der sich sicher bearbeiten
ließe.

### Entwurf, Anwenden, Zurücksetzen

Änderungen gehen **nicht** direkt an Elite Dangerous. Sie sammeln sich in einem Entwurf, und das
Statusabzeichen zeigt **Entwurf — nicht auf Spiel angewendet** oder **Synchron**. Derselbe Zustand
erscheint in der Anzeige *Tasten* im Vega-Reiter.

| Schaltfläche | Funktion |
|--------|--------------|
| **Auf Spiel anwenden** | Schreibt den Entwurf in deine `.binds`-Datei und legt vorher eine Sicherung an |
| **Aus Spiel zurücksetzen** | Verwirft den Entwurf und lädt neu aus der Spieldatei |

> **Öffne und schließe nach dem Anwenden den Steuerungsbildschirm in Elite Dangerous.** Das Spiel
> liest seine Belegungen nur beim Öffnen dieses Bildschirms neu ein. Elite Intel sagt das auch laut.

Hat sich die Belegungsdatei des Spiels nach dem Anlegen deines Entwurfs geändert, verweigert
Anwenden und bittet dich, zuerst neu zu laden oder zu verwerfen, statt stillschweigend die Änderung
eines anderen zu überschreiben.

Schließt du die App mit einem nicht angewendeten Entwurf, wirst du gefragt, ob du **Auf Spiel
anwenden**, **Entwurf behalten** oder **Verwerfen** willst.

---

## Bindungsverwaltung

![Bindungsverwaltung](images/ui-tab-bindings-management.png)

Deine Belegungssicherungen, gelistet nach dem Datum unter **Erstellt** und den **Dateien**, die
jede enthält. Elite Intel legt vor jedem Anwenden automatisch eine an; **Jetzt sichern** legt eine
auf Zuruf an.

| Schaltfläche | Funktion |
|--------|--------------|
| **In Entwurf wiederherstellen** | Lädt die Sicherung in deinen Entwurf, damit du sie prüfen kannst, bevor sie das Spiel berührt |
| **Live wiederherstellen** | Lädt sie und wendet sie direkt auf das Spiel an. Die üblichen Sicherheitsprüfungen laufen weiterhin |

Beide ersetzen ungespeicherte Änderungen im aktuellen Entwurf, und beide fragen vorher nach.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
