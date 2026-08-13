# Die Elite-Intel-Benutzeroberfläche

Elite Intel V1.1 ist in sechs Reiter am oberen Fensterrand gegliedert. Jeder davon verantwortet
einen eigenen Teil des Systems, und die meisten enthalten wiederum eigene Unterreiter.

Dieser Abschnitt führt durch jeden Reiter, jedes Bedienelement und dessen tatsächliche Funktion.

---

## Die sechs Reiter

| Reiter | Wofür er da ist |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | Die Kommandobrücke. Dienste starten und stoppen, die Konversation verfolgen, den Live-Status lesen, das HUD-Overlay im Spiel öffnen. |
| <img src="images/controller.png" class="inline" height="20" alt="Kommandant"> **[Kommandant](UI-Commander-Tab)** | Wer du bist und wie sich deine Schiffe verhalten. Automatisierungen, gesprochene Ansagen sowie Stimme und Persönlichkeit je Schiff. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Aktionen"> **[Aktionen](UI-Actions-Tab)** | Alles, was Elite Intel tun kann. Den Katalog der integrierten Befehle durchsuchen und eigene Makros bauen. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> **[Bindings](UI-Bindings-Tab)** | Deine Tastenbelegungen für Elite Dangerous. Lücken und Konflikte erkennen, bearbeiten und ins Spiel zurückschreiben. |
| <img src="images/settings.png" class="inline" height="20" alt="Einstellungen"> **[Einstellungen](UI-Settings-Tab)** | Der Unterbau. Sprache, Journal-Ordner, Sprachmodell, Sprachausgabe, Audio und Push-to-Talk. |
| <img src="images/stats.png" class="inline" height="20" alt="Statistik"> **[Statistik](UI-Stats-Tab)** | Token-Verbrauch und LLM-Telemetrie der aktuellen Sitzung. |

Dazu kommt das **[HUD-Overlay](UI-HUD-Overlay)** — ein eigenes, immer im Vordergrund liegendes
Fenster (und optional eine VR-Fläche), gesteuert vom Vega-Reiter.

---

## Wenn du zum ersten Mal startest

Elite Intel spricht seine Einrichtungswarnungen beim Start der Dienste laut aus, damit du nicht
suchen musst, was fehlt. Nach Wichtigkeit geordnet:

1. **Ein Sprachmodell.** Ohne das funktioniert nichts. Gehe zu
   [Einstellungen → KI-Dienste](UI-Settings-Tab) und füge entweder einen Cloud-API-Schlüssel ein
   oder verweise die App auf ein lokales Modell. Siehe [LLM auswählen](installing-local-llms).
2. **Der Journal-Ordner.** Ohne ihn ist Elite Intel blind für alles, was rund um dein Schiff
   geschieht. [Einstellungen → Allgemein](UI-Settings-Tab).
3. **Der Belegungsordner.** Ohne ihn kann Elite Intel dein Schiff nicht bedienen.
   [Bindings → Bindungsprofil](UI-Bindings-Tab).
4. **Audio kalibrieren.** Vor dem ersten Flug dringend empfohlen.
   [Vega-Reiter](UI-Vega-Tab) → **AUDIO KALIBRIEREN**.

---

## Überall geltende Konventionen

- **Das Fenster merkt sich nichts, was du nicht gespeichert hast.** Nur der Reiter
  *Einstellungen → KI-Dienste* arbeitet mit einem Entwurf: Er zeigt den Hinweis **Nicht
  gespeicherte Änderungen** und hält dich davon ab, den Reiter ohne Entscheidung zu verlassen.
  Jeder andere Schalter und Regler in der App schreibt sofort bei der Änderung durch.
- **Für Bindings gilt ein eigenes Entwurfsmodell.** Änderungen landen zuerst in einem Entwurf und
  werden erst nach **Auf Spiel anwenden** nach Elite Dangerous geschrieben.
- **Ein Sprachwechsel baut das Fenster neu auf.** Die Auswahl einer neuen Sprache unter
  *Einstellungen → Allgemein* rendert sofort jeden Reiter in dieser Sprache neu, und Vega sagt die
  Änderung an.
- **Neun Sprachen werden unterstützt:** Englisch, Spanisch, Französisch, Deutsch, Italienisch,
  Portugiesisch, brasilianisches Portugiesisch, Ukrainisch und Russisch.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
