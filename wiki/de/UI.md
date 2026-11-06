# Die Elite-Intel-Benutzeroberfläche

Elite Intel ist in sieben Reiter am oberen Fensterrand gegliedert. Jeder davon verantwortet
einen eigenen Teil des Systems, und die meisten enthalten wiederum eigene Unterreiter.

Dieser Abschnitt führt durch jeden Reiter, jedes Bedienelement und dessen tatsächliche Funktion.

---

## Die sieben Reiter

| Reiter | Wofür er da ist |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | Die Kommandobrücke. Dienste starten und stoppen, die Konversation verfolgen, den Live-Status lesen, das HUD-Overlay im Spiel öffnen. |
| <img src="images/commander.png" class="inline" height="20" alt="Kommandant"> **[Kommandant](UI-Commander-Tab)** | Wer du bist und wie sich deine Schiffe verhalten. Der Journal-Ordner, Stimmen und Persönlichkeiten der Flotte, Automatiken, gesprochene Ansagen, der Exo-Meisterschaft-Katalog und deine Lesezeichen. |
| <img src="images/actions.png" class="inline" height="20" alt="Aktionen"> **[Aktionen](UI-Actions-Tab)** | Alles, was Elite Intel tun kann. Den Katalog der integrierten Befehle durchsuchen und eigene Makros bauen. |
| <img src="images/keyboard.png" class="inline" height="20" alt="Bindings"> **[Bindings](UI-Bindings-Tab)** | **BindForge.** Bearbeite jede Steuerung von Elite Dangerous auf jedem Gerät, benenne deine Controller und sichere alle deine Steuerungsdateien. |
| <img src="images/services.png" class="inline" height="20" alt="Dienste"> **[Dienste](UI-Settings-Tab)** | Der Unterbau. Sprache, Sprachmodell, Sprachausgabe, Audio, Push-to-Talk und eine Sicherung deiner Einstellungen und Daten. |
| <img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> **[Jukebox](UI-Jukebox-Tab)** | Deine eigene Musik — unter Vega abgespielt und automatisch leiser, sobald sie spricht. |
| <img src="images/stats.png" class="inline" height="20" alt="Statistik"> **[Statistik](UI-Stats-Tab)** | Token-Verbrauch und LLM-Telemetrie der aktuellen Sitzung. |

Dazu kommt das **[HUD-Overlay](UI-HUD-Overlay)** — ein eigenes, immer im Vordergrund liegendes
Fenster (und optional eine VR-Fläche), gesteuert vom Vega-Reiter.

---

## Wenn du zum ersten Mal startest

Elite Intel spricht seine Einrichtungswarnungen beim Start der Dienste laut aus, damit du nicht
suchen musst, was fehlt. Nach Wichtigkeit geordnet:

1. **Ein Sprachmodell.** Ohne geht nichts. Öffne
   [Dienste → KI-Dienste](UI-Settings-Tab) und wähle entweder einen Cloud-Anbieter und füge
   seinen API-Schlüssel ein, oder richte die App auf ein lokales Modell. Siehe
   [LLM auswählen](installing-local-llms).
2. **Der Journal-Ordner.** Ohne ihn ist Elite Intel blind für alles, was um dein Schiff herum
   passiert. [Kommandant → Kommandantenprofil](UI-Commander-Tab).
3. **Deine Steuerung.** Ohne Belegungsdatei kann Elite Intel dein Schiff nicht bedienen. Öffne einmal
   den [Reiter Bindings](UI-Bindings-Tab), damit sich BindForge einrichten kann, und drücke dann
   **VEGA-Pflichtbelegungen automatisch zuweisen**, um Vega die nötigen Tasten zu geben. Sagt Vega, dass
   deine Steuerungsdateien noch nicht angelegt sind, schließe dein Kommandantentraining ab und stelle
   dann im Spiel unter *Optionen → Steuerung* die UI-Bewegung der Galaxiekarte auf die Pfeiltasten und
   übernimm das.

4. **Audio kalibrieren.** Vor dem ersten Flug dringend empfohlen.
   [Vega-Reiter](UI-Vega-Tab) → **AUDIO KALIBRIEREN**.

> Elite Intel ist für **Elite Dangerous Odyssey** gebaut. Unter Horizons warnt Vega beim Start,
> dass vieles nicht funktionieren wird.

---

## Konventionen, die überall gelten

- **Die meisten Bedienelemente speichern sofort.** Schalter, Schieberegler und Auswahllisten
  werden direkt übernommen; es gibt keinen Speichern-Knopf, den man vergessen könnte.
- **Zwei Ausnahmen warten auf dich.** *Dienste → KI-Dienste* hält deine Änderungen, bis du
  **Speichern** drückst, und fragt beim Verlassen mit offenen Änderungen nach *Speichern*, *Verwerfen*
  oder *Weiter bearbeiten*. Änderungen im Reiter Bindings werden bis **Anwenden** zurückgehalten, und
  alles wird gesichert, bevor sie Elite Dangerous erreichen.
- **Ein Sprachwechsel baut das Fenster neu auf.** Wählst du im Reiter *Dienste* eine neue Sprache, wird
  jeder Reiter sofort in dieser Sprache dargestellt, und Vega sagt den Wechsel an.
- **Neun Sprachen werden unterstützt:** Englisch, Russisch, Ukrainisch, Deutsch, Französisch, Spanisch,
  Italienisch, Portugiesisch und brasilianisches Portugiesisch.

- **Mehrere Kommandanten auf einem PC.** Elite Intel hält die Daten jedes Kommandanten getrennt
  und wechselt automatisch, sobald ein anderer Kommandant im Spiel geladen wird — Flottenliste
  und kommandantenbezogene Einstellungen ziehen mit.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
