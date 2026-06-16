# Elite Intel anpassen

Elite Intel fungiert als Schiffsstimme und KI-Gehirn. Mit Kokoro als integrierter Standard-TTS-Engine und den meisten Nutzern im vollständig Offline-Betrieb (Kokoro TTS und ein lokales LLM via Ollama) erklärt diese Anleitung, wie man Stimme und Persönlichkeitseinstellungen anpassen kann.

## Stimmen

Kokoro ist die Standard-TTS-Engine. Sie läuft offline ohne Cloud-Abhängigkeit. Verfügbare Stimmen:

- **Amerikanisch weiblich**: Heart, Bella, Nicole (flüsternd), Sky, Anna
- **Amerikanisch männlich**: Michael
- **Britisch weiblich**: Isabella, Emma
- **Britisch männlich**: George, Jason, Daniel

Google TTS (Cloud) ist ebenfalls verfügbar, wenn aktiviert. Dessen Stimmensatz ist von dem von Kokoro getrennt.

**Hinweis**: Ab Version 0351 können Schiffsstimme, Persönlichkeit und Sprachrhythmus nur noch über die Benutzeroberfläche geändert werden, nicht mehr per Sprachbefehl. Die KI wählt aus der aktiven Engine (standardmäßig Kokoro).

## Schiffsidentität

Die KI spricht als das Schiff. Sie verwendet „Ich", „mein" und „mir", wenn sie sich auf sich selbst bezieht.

Beispiele:
- „Was ist deine Ausrüstung?" gibt die aktuellen Schiffsmodule zurück.
- „Was ist deine Sprungreichweite?" gibt die aktuelle beladene und unbeladene Reichweite zurück.
- „Wie viel Treibstoff habe ich?" gibt den Treibstoffstand des Schiffes zurück.

Um stattdessen Informationen über den Flottenträger abzufragen, sprich den Träger explizit an:
- „Was ist die Reichweite des Flottenträgers?"
- „Erzähl mir von der Sprungreichweite des Trägers."

Andernfalls nimmt die KI an, dass sich die Abfrage auf das Schiff bezieht.

## Persönlichkeiten, Profile und Sprachrhythmus

**Offline-Modus (Kokoro + lokales LLM)** unterstützt keine benutzerdefinierten Persönlichkeiten, Fraktionsprofile (Imperial/Föderalistisch/Allianz) oder besondere Sprachrhythmuseinstellungen. Das lokale Modell antwortet in seinem standardmäßig trainierten Stil.

**Cloud-LLM-Nutzer** (Claude, Grok, OpenAI usw.) haben Zugang zum vollen Funktionsumfang:
- Professionell / Freundlich / Enthemmt / Rogue-Persönlichkeiten
- Imperiale / Föderalistische / Allianz-Profile (beeinflusst Ton und Formulierung)
- Temperatursteuerung (niedrig = schnell und präzise, hoch = kreativ und langsamer)

**Hinweis**: Ab Version 0351 können Persönlichkeit und Sprachrhythmus nur noch über die Benutzeroberfläche geändert werden.

Der Offline-Modus bietet niedrige Latenz ohne API-Kosten, mit schlichten Antworten. Cloud-LLMs bieten zusätzliche Persönlichkeitsoptionen.

## Tipps

- Beginne mit den Kokoro-Standardeinstellungen. Probiere verschiedene Stimmen aus, um eine bevorzugte Option zu finden. Britisch männliche Stimmen (George, Jason, Daniel) passen gut zu einer imperialen Ästhetik, auch ohne Profilunterstützung.
- Der Offline-Modus ist schnell und kostenlos. Der Cloud-Modus bietet zusätzliche Persönlichkeitsoptionen, verursacht jedoch API-Kosten.
- Experimentieren mit Formulierungen kann das Erlebnis bei Schiff-als-Sprecher-Abfragen verbessern.

Probleme, unerwartetes Verhalten oder Vorschläge bitte auf Matrix melden.

Community 👉 [**Matrix**](https://matrix.to/#/#krondor:matrix.org) 👈
