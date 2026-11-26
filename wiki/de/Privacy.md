# EliteIntel Datenschutzrichtlinie

Diese Richtlinie erläutert, welche Daten verarbeitet werden, wie sie verwendet werden und welche Wahlmöglichkeiten bestehen.

*Zuletzt aktualisiert: 14. November 2026*

## Überblick

EliteIntel ist eine Open-Source-Anwendung, die auf [GitHub](https://github.com/stone-alex/EliteIntel) verfügbar ist. Sie verwendet Sprache-zu-Text (STT), Text-zu-Sprache (TTS) und große Sprachmodelle (LLM), um Spieldaten zu verarbeiten.

Die App kann vollständig offline betrieben werden, indem lokale STT (NVIDIA Parakeet), ein lokales LLM (LM Studio) und lokale TTS (Supertonic) verwendet werden. In dieser Konfiguration verlässt außer der unten beschriebenen täglichen Installationszählung nichts das Gerät. Bei Nutzung von Cloud-Diensten werden Daten wie unten beschrieben übertragen.

## Welche Daten werden verarbeitet?

Es werden keine personenbezogenen Daten erfasst, darunter Namen, Adressen oder Standortdaten. Folgende Datentypen werden verarbeitet:

- **API-Schlüssel**: Dienen zur Authentifizierung von Anfragen an Cloud-TTS- und LLM-Dienste. Werden verschlüsselt in einer lokalen SQLite-Datenbank gespeichert. Werden nur in Anfrage-Headern an die jeweiligen Dienste übertragen (Google für TTS; xAI, OpenAI oder Anthropic für LLM).

- **Textdaten (TTS)**: Bei Verwendung von Google TTS wird der Antworttext an Google gesendet. Bei Verwendung von Supertonic TTS verlässt kein Datum das Gerät.

- **Spieldaten (LLM)**: Relevante Spieldaten (Missionsdetails, Marktdaten, Scanergebnisse usw.) werden an das konfigurierte LLM gesendet. Der Commander-Name wird nie übertragen. Die KI spricht dich mit deinem konfigurierten Titel, Anrede oder Spitznamen an.

- **Installationsstatistik**: Einmal am Tag sendet die App ihre Versionsnummer und ihr Betriebssystem (Windows oder Linux) an Google Firebase, unter einer zufälligen anonymen Kennung, die für diese Installation erzeugt wurde. Sonst wird nichts gesendet: kein Commander-Name, keine Spieldaten, keine Einstellungen. Sie dient nur dazu, zu zählen, wie viele Installationen aktiv sind und mit welcher Version und welchem Betriebssystem.

- **Massaker-Sets (nur mit Zustimmung)**: Nur solange das Teilen eingeschaltet ist (Tab Kommandant, Seite Massaker-Sets, standardmäßig aus), sendet die App die Piraten-Massaker-Sets, die sie aus den Journalen gelernt hat: Namen von Sternsystemen, Fraktionen und Stationen, die Zahl der Ressourcenabbaugebiete und den Tag, an dem ein Set zuletzt gesehen wurde. Kein Commander-Name, keine Missions-ID, keine Belohnung und nichts anderes, das einen Spieler erkennbar macht, wird gesendet. Die Sets aller Installationen werden geprüft und unter [elite-intel.org/data/massacre-sets.json](https://www.elite-intel.org/data/massacre-sets.json) veröffentlicht, ohne anzugeben, wer sie gemeldet hat.

## Wie werden diese Daten verwendet?

- **API-Schlüssel**: Werden in der lokalen Datenbank gespeichert und ausschließlich zur Authentifizierung von Anfragen an Drittanbieter-Dienste verwendet.

- **Audio und Text**: Werden nur zur TTS-Verarbeitung an Google gesendet. Google verarbeitet die Daten gemäß seiner eigenen Datenschutzrichtlinie. Die Datenspeicherung zur Serviceverbesserung ist bei der Standard-API-Nutzung standardmäßig deaktiviert.

- **Spieldaten**: Die App überträgt nicht alle Spielereignisse an das LLM. Sie sammelt und speichert relevante Daten lokal und sendet dann gezielte Auszüge, wenn ein Befehl oder eine Abfrage eingegeben wird. Das LLM hat keinen dauerhaften Zugriff auf Spieldaten.

## Wohin gehen die Daten?

- **Google (TTS)**: Text wird an Google Cloud-Dienste gesendet. Unterliegt der [Datenschutzrichtlinie von Google](https://policies.google.com/privacy).

- **xAI / OpenAI / Anthropic (LLM)**: Auszüge aus Spieldaten werden an das jeweilige konfigurierte Cloud-LLM gesendet. Unterliegen den jeweiligen Datenschutzrichtlinien.

- **Google Firebase (Installationsstatistik und Massaker-Sets)**: Gespeichert in Google Cloud Firestore unter der anonymen Installationskennung. Jede Installation kann nur ihre eigenen Einträge schreiben, und über die App kann nichts zurückgelesen werden. Ein nächtlicher Job der EliteIntel-Betreuer liest sie, um die zusammengefassten Zahlen und die geprüften Sets zu veröffentlichen. Wie bei jeder Internetanfrage sieht Google die IP-Adresse der Verbindung; dafür gilt die [Datenschutzrichtlinie von Google](https://policies.google.com/privacy). EliteIntel speichert sie nicht.

- **Nirgendwo sonst**: Es werden keine Daten verkauft oder an Dritte weitergegeben. Der vollständige Quellcode ist unter [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel) verfügbar.

## Rechte und Auswahlmöglichkeiten

- **Code einsehen**: Der vollständige Quellcode ist auf GitHub verfügbar.
- **Vollständig offline gehen**: Parakeet, LM Studio und Supertonic verwenden. Außer der täglichen Installationszählung werden in dieser Konfiguration keine Daten übertragen.
- **Anbieter konfigurieren**: Wähle, welches Cloud-LLM verwendet werden soll, falls überhaupt. API-Schlüssel werden in der lokalen Datenbank verwaltet.
- **Daten löschen**: Wenn du das Teilen der Massaker-Sets ausschaltest, werden die Sets dieser Installation aus Firebase gelöscht. Die Installationsstatistik enthält keine personenbezogenen Daten. Für Daten, die bei Google, xAI, OpenAI oder Anthropic gespeichert sind, bitte die jeweiligen Richtlinien beachten.

## Sicherheit

API-Schlüssel werden verschlüsselt in einer lokalen Datenbank gespeichert und nur in Anfrage-Headern übertragen. Die App hält die Nutzungsbedingungen von Elite Dangerous ein. Sie liest keinen Spielspeicher aus und verwendet keine Overlays. Der Open-Source-Quellcode ermöglicht eine Überprüfung durch die Community.

## Änderungen dieser Richtlinie

Aktualisierungen der Richtlinie werden im GitHub-Repository vermerkt und können in der App erscheinen. Änderungen unter [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel) verfolgen.

## Fragen

Bitte ein Issue auf GitHub eröffnen oder über Matrix Kontakt aufnehmen.

----
Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
