# HUD-Overlay

**Neu in V1.1.** Ein Overlay, das immer im Vordergrund liegt und deine aktuellen Ziele auf den
Bildschirm bringt — im Spielfenster oder im VR-Headset.

![HUD-Overlay im Spiel](images/ui-overlay-ingame.png)

Es ersetzt das alte OBS-Overlay-Fenster. Das Overlay läuft in einem eigenen Prozess und
konkurriert daher weder mit dem Spiel noch mit der App um den Oberflächen-Thread.

Die Karte wird nach der Geometrie des Cockpits gezeichnet, nicht rechtwinklig zum Monitor. Sie
neigt sich also so, wie sich die Panels des Schiffs an dieser Stelle des Bildschirms neigen —
verschiebst du sie, ändert sich die Neigung entsprechend. Ihre Zeilen sind schräge Linien, weshalb
ein Wert deutlich tiefer sitzen kann als die Beschriftung, zu der er gehört: Lies jede Zeile
entlang der Schräge, genau wie die Anzeigen des Spiels daneben. Wie weit ein Wert abzurutschen
scheint, hängt außer von der Platzierung auch von der **TEXTGRÖSSE** ab — die Neigung gibt das
Cockpit vor, kleinerer Text bedeutet also kürzere Zeilen, und derselbe Versatz überspannt mehr
davon.

Schalte es mit **OVERLAY ANZEIGEN** im [Vega-Reiter](UI-Vega-Tab) ein und konfiguriere es mit
**OVERLAY-EINSTELLUNGEN** daneben.

> Fehlt die Overlay-Binärdatei in der Distribution, bleibt der Schalter aus und sagt das im
> Diagnoseprotokoll. Es wird kein Overlay behauptet, das es nicht gibt.

---

## Was es anzeigt

Das Overlay zeichnet **Karten** — eine je aktivem Ziel, abgeleitet aus dem, was du tatsächlich
tust. Karten erscheinen und verschwinden von selbst; es gibt nichts zu konfigurieren.

| Karte | Erscheint, wenn |
|------|--------------|
| **EXOBIOLOGIE** | Du Organismen beprobst — Gattung und was noch zu finden ist |
| **VERNICHTUNGSAUFTRAG** | Du Vernichtungsmissionen fliegst — nötige Abschüsse, Stapel, Belohnung |
| **BERGBAU** | Du Bergbau betreibst — Laderaum, Limpets, Zielware |
| **HANDELSROUTE** | Eine Handelsroute geplant ist — Ware, Einkauf, Verkauf, Marge, Etappe *n* von *m* |
| **FRACHTCHANCE** | Für deine Ladung eine gewinnbringende Fracht entdeckt wurde |
| **MISSION** | Eine hervorgehobene Mission — Ziel, Fracht oder Passagiere, Ablauf, Belohnung |
| **GEPLANTE ROUTE** | Eine Route gesetzt ist — Ziel, nächstes System, verbleibende Sprünge |
| **MATERIALHÄNDLER** · **TECHNOLOGIEHÄNDLER** · **INTERSTELLARE FAKTOREN** · **VISTA GENOMICS** | Du eine Zielerinnerung gesetzt hast, um einen davon aufzusuchen |

Die Karte der hervorgehobenen Mission wählt ihre Mission so, wie du es tun würdest: zuerst die am
Ziel deiner Route, dann eine in deinem aktuellen System, dann die zuletzt angenommene.

---

## Overlay-Einstellungen

![Overlay-Einstellungen](images/ui-overlay-settings.png)

**HINTERGRUND-TRANSPARENZ** (0–100 %) und **TEXTGRÖSSE** (75–200 %) sind bewusst zwei getrennte
Regler. Ein einzelner „Deckkraft"-Regler würde den Text zusammen mit dem Hintergrund ausblenden —
und genau das macht ein abgedunkeltes Overlay über einer hellen Planetenoberfläche unlesbar. Blende
den Hintergrund aus, lass den Text in Ruhe.

### ANZEIGE AUF

| Modus | Was er tut |
|------|--------------|
| **Monitor** | Ein Desktop-Fenster. Die Vorgabe, und was jede Version vor V1.1 getan hat. Die Karte neigt sich passend zum Cockpit, und die Neigung ändert sich mit der Platzierung — siehe oben |
| **VR-Headset** | Ein SteamVR-Overlay. Benötigt ein laufendes SteamVR. Ist VR nicht verfügbar, fällt es auf ein Desktop-Fenster zurück, du stehst also nie ohne da |
| **Monitor und Headset** | Beides gleichzeitig, mit identischen Daten versorgt. Nützlich, wenn du in VR fliegst, aber vom Monitor streamst oder aufnimmst |
| **VR-Aufnahmefenster** | Ein schlichtes, flaches, undurchsichtiges Fenster, das ein Aufnahmewerkzeug anheften kann |

### Zum VR-Aufnahmefenster

Dieser Modus spricht **nicht** mit SteamVR. Starte dein Aufnahmewerkzeug — Desktop+, OVR Toolkit
oder Virtual Desktop — und wähle das Fenster mit dem Namen **„EliteIntel HUD (VR capture)"**.

Warum es das gibt: Der SteamVR-Modus übergibt dem Compositor pro getipptem Zeichen eine vollständige
Textur, und auf einem gestreamten Headset wurde das als echte Einbuße bei der Bildrate gemeldet.
Ein Aufnahmewerkzeug greift das Fenster auf der GPU nach eigenem Zeitplan ab und bietet dir
Platzierungs- und Krümmungsregler, die diese App nicht hat.

Es ist ein eigener Modus statt „richte dein Aufnahmewerkzeug auf das Monitor-Fenster", weil jenes
Fenster geneigt und durchsichtig ist und als Werkzeugfenster gilt — und solche filtern
Aufnahme-Auswahldialoge vollständig heraus.

### POSITION IM HEADSET

Acht Platzierungen: **Oben, Oben rechts, Rechts, Unten rechts, Unten, Unten links, Links, Oben
links.**

> **Das HUD ist vor deinem Sitz fixiert und folgt deinem Kopf nicht.** Die gewählte Richtung wird
> von dort aus gemessen, wohin du nach SteamVRs *Sitzposition zurücksetzen* blickst — ein Neuzentrieren
> deiner Ansicht verschiebt das HUD also mitsamt dem Cockpit, und genau das willst du. Schaust du
> weg, bleibt das HUD, wo du es gelassen hast, exakt wie ein physisches Panel.

---

## In einer anderen Sprache lesen

Die Kartenbeschriftungen folgen der Sprache der App, und Zahlen werden so gruppiert, wie diese
Sprache gruppiert. Vom Spiel gelieferte Namen — Systeme, Stationen, Waren — gehen unverändert durch.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
