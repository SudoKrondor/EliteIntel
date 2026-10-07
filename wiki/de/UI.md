# Die Elite-Intel-Benutzeroberfläche

Elite Intel ist in sieben Reiter am oberen Fensterrand gegliedert. Jeder davon verantwortet
einen eigenen Teil des Systems, und die meisten enthalten wiederum eigene Unterreiter.

Dieser Abschnitt führt durch jeden Reiter, jedes Bedienelement und dessen tatsächliche Funktion.

---

## Die sieben Reiter

| Reiter | Wofür er da ist |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | Die Kommandobrücke. Dienste starten und stoppen, die Konversation verfolgen, den Live-Status lesen, das HUD-Overlay im Spiel öffnen. |
| <img src="images/controller.png" class="inline" height="20" alt="Kommandant"> **[Kommandant](UI-Commander-Tab)** | Wer du bist und wie sich deine Schiffe verhalten. Stimmen und Persönlichkeiten der Flotte, Automatisierungen, gesprochene Ansagen und der Exo-Meisterschaft-Katalog. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Aktionen"> **[Aktionen](UI-Actions-Tab)** | Alles, was Elite Intel tun kann. Den Katalog der integrierten Befehle durchsuchen und eigene Makros bauen. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> **[Bindings](UI-Bindings-Tab)** | Deine Tastenbelegungen für Elite Dangerous. Lücken und Konflikte erkennen, bearbeiten und ins Spiel zurückschreiben. |
| <img src="images/settings.png" class="inline" height="20" alt="Einstellungen"> **[Einstellungen](UI-Settings-Tab)** | Der Unterbau. Sprache, Journal-Ordner, Sprachmodell, Sprachausgabe, Audio und Push-to-Talk. |
| <img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> **[Jukebox](UI-Jukebox-Tab)** | Deine eigene Musik — unter Vega abgespielt und automatisch leiser, sobald sie spricht. |
| <img src="images/stats.png" class="inline" height="20" alt="Statistik"> **[Statistik](UI-Stats-Tab)** | Token-Verbrauch und LLM-Telemetrie der aktuellen Sitzung. |

Dazu kommt das **[HUD-Overlay](UI-HUD-Overlay)** — ein eigenes, immer im Vordergrund liegendes
Fenster (und optional eine VR-Fläche), gesteuert vom Vega-Reiter.

---

## Wenn du zum ersten Mal startest

Elite Intel spricht seine Einrichtungswarnungen beim Start der Dienste laut aus, damit du nicht
suchen musst, was fehlt. Nach Wichtigkeit geordnet:

1. **Ein Sprachmodell.** Ohne geht nichts. Öffne
   [Einstellungen → KI-Dienste](UI-Settings-Tab) und wähle entweder einen Cloud-Anbieter und füge
   seinen API-Schlüssel ein, oder richte die App auf ein lokales Modell. Siehe
   [LLM auswählen](installing-local-llms).
2. **Der Journal-Ordner.** Ohne ihn ist Elite Intel blind für alles, was um dein Schiff herum
   passiert. [Einstellungen → Allgemein](UI-Settings-Tab).
3. **Der Belegungsordner.** Ohne ihn kann Elite Intel dein Schiff nicht bedienen.
   [Bindings → Bindungsprofil](UI-Bindings-Tab). Stimmt der Ordner, und Vega findet trotzdem
   keine Belegungen, öffne im Spiel *Optionen → Steuerung* und ändere irgendeine Belegung — das
   Spiel schreibt eine Belegungsdatei erst, wenn du etwas angepasst hast.
4. **Audio kalibrieren.** Vor dem ersten Flug dringend empfohlen.
   [Vega-Reiter](UI-Vega-Tab) → **AUDIO KALIBRIEREN**.

> Elite Intel ist für **Elite Dangerous Odyssey** gebaut. Unter Horizons warnt Vega beim Start,
> dass vieles nicht funktionieren wird.

---

## Konventionen, die überall gelten

- **Die meisten Bedienelemente speichern sofort.** Schalter, Schieberegler und Auswahllisten
  werden direkt übernommen; es gibt keinen Speichern-Knopf, den man vergessen könnte.
- **Zwei Ausnahmen arbeiten mit einem Entwurf.** *Einstellungen → KI-Dienste* hält deine
  Änderungen, bis du **Speichern** drückst, und fragt beim Verlassen mit offenen Änderungen nach
  *Speichern*, *Verwerfen* oder *Weiter bearbeiten*. Tastenbelegungen sammeln sich in einem
  Entwurf, der erst mit **Anwenden** in Elite Dangerous geschrieben wird.
- **Ein Sprachwechsel baut das Fenster neu auf.** Wählst du in *Einstellungen → Allgemein* eine
  neue Sprache, wird jeder Reiter sofort in dieser Sprache dargestellt, und Vega sagt den Wechsel
  an.
- **Neun Sprachen werden unterstützt:** Englisch, Spanisch, Französisch, Deutsch, Italienisch,
  Portugiesisch, brasilianisches Portugiesisch, Ukrainisch und Russisch.
- **Mehrere Kommandanten auf einem PC.** Elite Intel hält die Daten jedes Kommandanten getrennt
  und wechselt automatisch, sobald ein anderer Kommandant im Spiel geladen wird — Flottenliste
  und kommandantenbezogene Einstellungen ziehen mit.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
