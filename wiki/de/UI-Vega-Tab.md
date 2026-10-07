# Reiter Vega

<img src="images/ai.png" class="inline" height="20" alt="Vega"> Der Standardreiter — der, den du
im Flug offen lässt. Er startet und stoppt den KI-Stack, zeigt, was Vega gehört und gesagt hat,
meldet den Zustand jedes Subsystems und öffnet das Overlay im Spiel.

![Reiter Vega](images/ui-tab-vega.png)

Der Reiter ist in vier Zonen gegliedert: die Protokolle **Konversation** und **Diagnose** links,
**Schnellstatus** und **Schnellzugriff** in der rechten Seitenleiste und der Telemetriestreifen
**Systemzusammenfassung** unten.

---

## Konversation

Alles, was du gesagt hast, und alles, was Vega geantwortet hat, in einem Strom. Deine Zeilen
stehen links, Vegas Antworten rechts, damit auch eine lange Sitzung auf einen Blick lesbar
bleibt. Befehle, die du im Spielchat tippst (siehe [Alle Befehle](AllCommands)), erscheinen hier
genau wie gesprochene.

## Diagnose / Systemmeldungen

Das technische Protokoll — Dienststarts, Kalibrierergebnisse, Belegungswarnungen, was die
Spracherkennung gehört hat (`STT: [...]`), Dateioperationen. Es wird nie vorgelesen; es zeigt
dir, was die App gerade tut.

Vier Knöpfe sitzen im Abschnittskopf:

| Knopf | Was er tut |
|--------|--------------|
| **Kopieren** | Kopiert den im Protokoll markierten Text in die Zwischenablage. |
| **Support-Paket speichern** | Schreibt ein `.zip` mit Zeitstempel für einen Fehlerbericht: dieses Protokoll, das Anwendungsprotokoll, dein Journal und die Live-Statusdateien des Spiels, deine Belegungen, deine eigenen Befehle, eine Hardware-Übersicht und eine Zusammenfassung der Mikrofon-*Pegel* (nie Audio). Was nicht gesammelt werden konnte, steht im Paket selbst. **Das hängst du an einen Fehlerbericht an.** |
| **Vega-Speicher ausgeben** | Schreibt einen JSON-Schnappschuss von Vegas Arbeitsgedächtnis der aktuellen Sitzung. Funktioniert nur bei laufenden Diensten. |
| **Leeren** | Leert das Diagnoseprotokoll. |

---

## Schnellstatus

Sechs Live-Anzeigen. Jede zeigt einen Zustand und eine Farbe, sodass ein Blick genügt, um zu
sehen, ob der Stack gesund ist.

| Anzeige | Zustände |
|---------|--------|
| **STT** | `Bereit` (Dienste gestoppt) · `Höre zu` · `Schlafend` (ignoriert dich) · `Push-to-Talk` (nur die zugewiesene Taste öffnet das Mikrofon) |
| **KI** | `Bereit` · `Offline` (keine Verbindung) · der Name des Anbieters, der tatsächlich geantwortet hat, oder `Aktiv` |
| **TTS** | `Bereit` · `Lokal` (Kokoro / Supertonic) · `Cloud` (Google / Microsoft Edge) |
| **Belegungen** | `OK` oder `N fehlend` |
| **Befehle** | Wie viele eigene Befehle geladen sind |
| **Tasten** | `Synchron` mit dem Spiel oder `Geändert` — es gibt einen nicht angewendeten Belegungsentwurf |

Die **KI**-Anzeige lohnt einen Blick. Sie meldet nicht, was du *eingestellt* hast, sondern
welcher Anbieter tatsächlich geantwortet hat.

---

## Schnellzugriff

| Knopf | Was er tut |
|--------|--------------|
| **DIENSTE STARTEN / STOPPEN** | Schaltet den gesamten KI-Stack. Der Knopf sperrt sich während des Startens oder Stoppens, damit er nicht doppelt ausgelöst wird. |
| **SCHLAFEN / AUFWACHEN** | Wach hört Vega ständig zu. Schlafend ignoriert sie alles außer einer Weckphrase (`wach auf`) oder einem Befehl mit vorangestelltem `hör zu` — *„Hör zu, Fahrwerk runter.“* Gesperrt, solange Push-to-Talk aktiv ist: dann ist die zugewiesene Taste das Tor. |
| **OVERLAY ANZEIGEN / AUSBLENDEN** | Zeigt das immer im Vordergrund liegende [HUD-Overlay](UI-HUD-Overlay). Die App merkt sich, wie du es verlassen hast, und stellt es beim nächsten Start wieder her. Fehlt die Overlay-Datei, meldet der Schalter das im Protokoll, statt ein Overlay vorzutäuschen, das es nicht gibt. |
| **OVERLAY-EINSTELLUNGEN** | Öffnet die [HUD-Overlay-Einstellungen](UI-HUD-Overlay) — Transparenz, Textgröße, Farben und wo es gezeichnet wird (Monitor, VR-Headset, beides oder ein Aufnahmefenster). |
| **Audiogeräte** | Mikrofon und Lautsprecher wählen. Eine Änderung wirkt sofort: nur die Spracherkennung (Mikrofon) bzw. die Stimme (Lautsprecher) startet neu. |
| **AUDIO KALIBRIEREN** | Misst Grundrauschen und Sprechpegel und setzt das Sprachgate. Nur bei laufenden Diensten verfügbar. Einmal vor dem ersten Flug ausführen und erneut, wenn du Mikrofon oder Raum wechselst. |
| **Update** | Erscheint, wenn eine neue Version verfügbar ist. |

Zwischen den beiden Knopfgruppen sitzt der **Kommandantenblock** — dein Name, dein Schiff, die
Uhr und dein aktueller Kontostand.

---

## Systemzusammenfassung

Ein Telemetriestreifen mit sechs Blöcken am unteren Rand:

| Block | Bedeutung |
|-------|---------|
| **LLM-Modell** | Das Modell, das die letzte Anfrage bedient hat |
| **Sitzungszeit** | Zeit seit dem Start der Dienste |
| **Tokens verwendet** | Prompt + Antwort + Cache, für die Sitzung |
| **Tokens / Stunde** | Eine hochgerechnete Rate. Bleibt in den ersten 10 Minuten leer, während Daten gesammelt werden |
| **Cache-Ersparnis** | Aus dem Cache bediente Tokens. `0` wird bewusst angezeigt — das ist eine Information, keine fehlenden Daten |
| **Letzte Geschwindigkeit** | Tokens pro Sekunde der letzten Antwort |

Die vollständige Aufschlüsselung findest du im [Reiter Statistik](UI-Stats-Tab).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
