# EliteIntel Datenschutzrichtlinie

Diese Richtlinie erläutert, welche Daten verarbeitet werden, wie sie verwendet werden und welche Wahlmöglichkeiten bestehen.

*Zuletzt aktualisiert: 25. Oktober 2025*

## Überblick

EliteIntel ist eine Open-Source-Anwendung, die auf [GitHub](https://github.com/stone-alex/EliteIntel) verfügbar ist. Sie verwendet Sprache-zu-Text (STT), Text-zu-Sprache (TTS) und große Sprachmodelle (LLM), um Spieldaten zu verarbeiten.

Die App kann vollständig offline betrieben werden, indem lokale STT (NVIDIA Parakeet), ein lokales LLM (Ollama) und lokale TTS (Kokoro) verwendet werden. In dieser Konfiguration verlässt kein Datum das Gerät. Bei Nutzung von Cloud-Diensten werden Daten wie unten beschrieben übertragen.

## Welche Daten werden verarbeitet?

Es werden keine personenbezogenen Daten erfasst, darunter Namen, Adressen oder Standortdaten. Folgende Datentypen werden verarbeitet:

- **API-Schlüssel**: Dienen zur Authentifizierung von Anfragen an Cloud-TTS- und LLM-Dienste. Werden verschlüsselt in einer lokalen SQLite-Datenbank gespeichert. Werden nur in Anfrage-Headern an die jeweiligen Dienste übertragen (Google für TTS; xAI, OpenAI oder Anthropic für LLM).

- **Textdaten (TTS)**: Bei Verwendung von Google TTS wird der Antworttext an Google gesendet. Bei Verwendung von Kokoro TTS verlässt kein Datum das Gerät.

- **Spieldaten (LLM)**: Relevante Spieldaten (Missionsdetails, Marktdaten, Scanergebnisse usw.) werden an das konfigurierte LLM gesendet. Der Commander-Name wird nie übertragen. Die KI spricht dich mit deinem konfigurierten Titel, Anrede oder Spitznamen an.

## Wie werden diese Daten verwendet?

- **API-Schlüssel**: Werden in der lokalen Datenbank gespeichert und ausschließlich zur Authentifizierung von Anfragen an Drittanbieter-Dienste verwendet.

- **Audio und Text**: Werden nur zur TTS-Verarbeitung an Google gesendet. Google verarbeitet die Daten gemäß seiner eigenen Datenschutzrichtlinie. Die Datenspeicherung zur Serviceverbesserung ist bei der Standard-API-Nutzung standardmäßig deaktiviert.

- **Spieldaten**: Die App überträgt nicht alle Spielereignisse an das LLM. Sie sammelt und speichert relevante Daten lokal und sendet dann gezielte Auszüge, wenn ein Befehl oder eine Abfrage eingegeben wird. Das LLM hat keinen dauerhaften Zugriff auf Spieldaten.

## Wohin gehen die Daten?

- **Google (TTS)**: Text wird an Google Cloud-Dienste gesendet. Unterliegt der [Datenschutzrichtlinie von Google](https://policies.google.com/privacy).

- **xAI / OpenAI / Anthropic (LLM)**: Auszüge aus Spieldaten werden an das jeweilige konfigurierte Cloud-LLM gesendet. Unterliegen den jeweiligen Datenschutzrichtlinien.

- **Nirgendwo sonst**: Es werden keine Daten extern gespeichert, verkauft oder an Dritte weitergegeben. Der vollständige Quellcode ist unter [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel) verfügbar.

## Rechte und Auswahlmöglichkeiten

- **Code einsehen**: Der vollständige Quellcode ist auf GitHub verfügbar.
- **Vollständig offline gehen**: Parakeet, Ollama oder LM Studio und Kokoro verwenden. In dieser Konfiguration werden keine Daten übertragen.
- **Anbieter konfigurieren**: Wähle, welches Cloud-LLM verwendet werden soll, falls überhaupt. API-Schlüssel werden in der lokalen Datenbank verwaltet.
- **Daten löschen**: Es werden keine Daten extern gespeichert. Für Daten, die bei Google, xAI, OpenAI oder Anthropic gespeichert sind, bitte die jeweiligen Richtlinien beachten.

## Sicherheit

API-Schlüssel werden verschlüsselt in einer lokalen Datenbank gespeichert und nur in Anfrage-Headern übertragen. Die App hält die Nutzungsbedingungen von Elite Dangerous ein. Sie liest keinen Spielspeicher aus und verwendet keine Overlays. Der Open-Source-Quellcode ermöglicht eine Überprüfung durch die Community.

## Änderungen dieser Richtlinie

Aktualisierungen der Richtlinie werden im GitHub-Repository vermerkt und können in der App erscheinen. Änderungen unter [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel) verfolgen.

## Fragen

Bitte ein Issue auf GitHub eröffnen oder über Matrix Kontakt aufnehmen.

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
