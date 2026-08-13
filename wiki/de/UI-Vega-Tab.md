# Reiter „Vega"

<img src="images/ai.png" class="inline" height="20" alt="Vega"> Der Standardreiter, und der, den
du beim Fliegen offen lässt. Er startet und stoppt den KI-Stack, zeigt, was Vega gehört und gesagt
hat, meldet den Zustand jedes Subsystems und öffnet das Overlay im Spiel.

![Reiter Vega](images/ui-tab-vega.png)

Der Reiter ist in vier Zonen aufgeteilt: die Protokolle **Konversation** und **Diagnose** auf der
linken Seite, **Schnellstatus** und **SCHNELLZUGRIFF** in der rechten Seitenleiste sowie die
Telemetrieleiste **Systemzusammenfassung** am unteren Rand.

---

## Konversation

Alles, was du gesagt hast, und alles, was Vega geantwortet hat, in einem Strom. Deine Zeilen sind
linksbündig, Vegas Antworten rechtsbündig, damit eine lange Sitzung auf einen Blick lesbar bleibt.

## Diagnose / Systemmeldungen

Das technische Protokoll — Dienststarts, Kalibrierungsergebnisse, Belegungswarnungen,
Dateioperationen. Es wird nie gesprochen; es existiert, damit du siehst, was die App gerade tut.

Vier Schaltflächen sitzen in der Abschnittskopfzeile:

| Schaltfläche | Funktion |
|--------|--------------|
| **Kopieren** | Kopiert den im Protokoll markierten Text in die Zwischenablage. Nur aktiv, wenn eine Auswahl besteht. |
| **Diagnosepaket speichern** | Schreibt ein `.zip` mit Zeitstempel, das Systemprotokoll, Anwendungsprotokoll, deine aktive Journaldatei und deine Belegungen enthält. **Das ist es, was du an einen Fehlerbericht anhängst.** |
| **Vega-Speicher ausgeben** | Schreibt eine JSON-Momentaufnahme von Vegas Arbeitsspeicher für die aktuelle Sitzung. Nur verfügbar, während die Dienste laufen. |
| **Löschen** | Leert das Diagnoseprotokoll und dessen Export-Mitschrift. |

---

## Schnellstatus

Sechs Live-Anzeigen. Jede zeigt einen Zustand und eine Farbe, sodass ein Blick genügt, um zu
sehen, ob der Stack gesund ist.

| Anzeige | Zustände |
|---------|--------|
| **STT** | `Bereit` (Dienste gestoppt) · `Schlafend` (ignoriert dich) · `Höre zu` |
| **KI** | `Bereit` · `Offline` (keine Verbindung möglich) · oder der Name des Anbieters, der tatsächlich antwortet |
| **TTS** | `Bereit` · `Lokal` (Kokoro) · `Cloud` (Google) |
| **Bindings** | `OK` oder `N fehlend` |
| **Befehle** | Wie viele eigene Befehle geladen sind |
| **Tasten** | `Synchron` mit dem Spiel oder `Geändert` — du hast einen nicht angewendeten Belegungsentwurf |

Die Anzeige **KI** lohnt einen Blick. Sie meldet nicht, was du *konfiguriert* hast, sondern welcher
Anbieter die letzte Anfrage tatsächlich beantwortet hat.

---

## SCHNELLZUGRIFF

| Schaltfläche | Funktion |
|--------|--------------|
| **DIENSTE STARTEN / STOPPEN** | Schaltet den gesamten KI-Stack um. Die Schaltfläche deaktiviert sich während des Startens oder Stoppens selbst, damit sie nicht doppelt auslöst. |
| **SCHLAFEN / AUFWACHEN** | Im Modus *Aufwachen* hört Vega durchgehend zu. Im Modus *Schlafen* ignoriert es dich, sofern du nicht das Umgehungswort `listen` verwendest oder `Wake up!` sagst. Deaktiviert, solange Push-to-Talk aktiv ist — im PTT-Modus *ist* die Taste das Tor. |
| **OVERLAY ANZEIGEN / AUSBLENDEN** | Zeigt das immer im Vordergrund liegende [HUD-Overlay](UI-HUD-Overlay). Fehlt die Overlay-Binärdatei, bleibt die Schaltfläche ehrlich und meldet den Fehlschlag im Protokoll, statt ein Overlay zu behaupten, das es nicht gibt. |
| **OVERLAY-EINSTELLUNGEN** | Öffnet die [HUD-Overlay-Einstellungen](UI-HUD-Overlay) — Transparenz, Textgröße und wo es gezeichnet wird (Monitor, VR-Headset, beides). |
| **Audiogeräte** | Öffnet den Dialog für die Audioschnittstelle, um Mikrofon und Lautsprecher zu wählen. Änderungen greifen beim nächsten Dienststart. |
| **AUDIO KALIBRIEREN** | Misst deinen Geräuschpegel und deine Sprechlautstärke und setzt das Audio-Gate. Nur verfügbar, während die Dienste laufen. Führe das einmal vor deinem ersten Flug aus, und erneut, wenn du Mikrofon oder Raum wechselst. |
| **Aktualisieren** | Erscheint, wenn eine neue Version verfügbar ist. |

Zwischen den beiden Schaltflächengruppen sitzt der **Kommandantenblock** — dein Name, dein Schiff,
die Uhr und dein aktueller Guthabenstand.

---

## Systemzusammenfassung

Eine Telemetrieleiste aus sechs Blöcken am unteren Rand des Reiters:

| Block | Bedeutung |
|-------|---------|
| **LLM-Modell** | Das Modell, das die letzte Anfrage bedient hat |
| **Sitzungszeit** | Zeit seit dem Start der Dienste |
| **Tokens verwendet** | Prompt + Antwort + Cache, für die Sitzung |
| **Tokens / Stunde** | Eine hochgerechnete Rate. Bleibt die ersten 10 Minuten leer, während Daten gesammelt werden |
| **Cache-Ersparnis** | Aus dem Cache bediente Tokens. `0` wird bewusst angezeigt — das ist eine Information, keine fehlende Angabe |
| **Letzte Geschwindigkeit** | Tokens pro Sekunde bei der letzten Antwort |

Die vollständige Aufschlüsselung findest du im [Reiter Statistik](UI-Stats-Tab).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
