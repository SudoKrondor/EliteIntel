
### <img src="images/settings.png" class="inline" height="20" alt="Settings"> Einstellungen / Lokaler LLM-Tab

![tab-settings-ai-services.png](images/tab-settings-ai-services.png)

**Sprache**
- Wähle deine Sprache. Unterstützte Sprachen sind Englisch, Spanisch, Französisch, Deutsch, Ukrainisch und Russisch.

**Konversationsmodus (an/aus)**
- Der „Konversationsmodus" ermöglicht es dir, mit dem LLM zu chatten. Wenn er ausgeschaltet ist (Standard), läuft das LLM im strikten Befehlsmodus. Es verarbeitet nur Befehle und führt Abfragen und Aktionen durch, ignoriert jedoch alle sinnlosen Eingaben.

**Journal-Verzeichnis**
- Speicherort deines Spiel-Journal-Verzeichnisses. Darüber erkennt Elite Intel deine Spielsitzung.

---

### LLM-Optionen
**Lokales LLM**

- Wähle einen Inferenz-Engine-Host. Ollama oder LMStudio (schnellere Option)
- Gib im Feld ADRESSE die Adresse deines Inferenzservers ein. Entweder localhost, wenn du ihn auf demselben Rechner betreibst, oder eine IP-Adresse des Computers in deinem lokalen Netzwerk. Gib Portnummer und URI für den API-Endpunkt an.
- Gib den Namen des Modells im Feld „Befehls-Modell" ein. Dieses Modell wird für die Klassifizierung der Benutzereingabe verwendet.
- Gib den Namen des Modells im Feld „Abfrage-Modell" ein. Dieses Modell wird für Abfragen und natürlichsprachliche Antworten verwendet.
- HINWEIS: Du kannst dasselbe Modell für beides verwenden, besonders wenn du keine Hardware hast, um mehr als ein Modell zu betreiben.

**Cloud-LLM**

Wenn du keine Hardware für ein lokales LLM hast, kannst du stattdessen eine Cloud-Instanz verwenden.

- [**Mistral Console**](https://console.mistral.ai/home) hat einen **kostenlosen Tarif** und ist einfach einzurichten.
- Alternativ kannst du Claude, Gemini, Grok (xAi), Open AI oder DeepSeek verwenden. Melde dich bei der API-Konsole deines bevorzugten LLM-Anbieters an und erstelle einen API-Schlüssel.
- Gib den Schlüssel in das API-Schlüssel-Feld ein, sperr das Feld und klicke auf „Verwenden", damit die App weiß, dass du ein Cloud-LLM nutzt.
- Starte die Dienste im vorderen Tab neu, damit die Änderungen wirksam werden.

**HINWEIS** 👉 [Mehr zu Cloud-LLMs hier](cloud-llm-options) 👈

---


### <img src="images/mic.png" class="inline" height="20" alt="Audio"> Einstellungen / Audio

Konfiguriere deine Audioeinstellungen.

![tab-settings-audio.png](images/tab-settings-audio.png)

Die Dropdown-Menüs **Mikrofon** und **Lautsprecher** ermöglichen die Auswahl der Audio-Ein- und Ausgabeleitungen. Die Änderung wird wirksam, wenn du die Dienste im vorderen Tab neu startest.

- **Sprachlautstärke**: Steuert die Lautstärke der Sprachsynthese.
- **TTS-Sprechgeschwindigkeit**: Steuert die Geschwindigkeit der Sprachsynthese.
- **Piepton-Lautstärke**: Steuert die Lautstärke des Piepton-Indikators. Zeigt an, dass STT die Verarbeitung abgeschlossen hat und der LLM die Eingabe erhalten hat.
- **STT-Threads**: Legt die Thread-Zuweisung für die STT-Verarbeitung fest. Dies ist eine Min/Max-Einstellung. Die App fordert das Minimum an, nutzt aber was der Prozessor bereitstellt. Threads werden nach Abschluss der Verarbeitung freigegeben.

- **Mikrofon-Monitor**
- FLOOR-Pegel (der Geräuschpegel, wenn du nicht sprichst),
- GATE-Pegel, zeigt den Audio-Gate-Pegel an. Wenn Audio über dem Gate liegt, werden die Daten zur Transkription an Parakeet gesendet. Wenn der Audiopegel unter den Gate-Pegel fällt, wird das empfangene Audio in Text umgewandelt und zur Klassifizierung an das LLM gesendet.
- CLIP zeigt an, dass du das Mikrofon übersteuert, wenn deine Eingabe diese Linie überschreitet. In diesem Fall wird die Transkription ungenau.


### <img src="images/controller.png" class="inline" height="20" alt="1PTT"> Einstellungen / Push To Talk

![tab-settings-push-to-talk.png](images/tab-settings-push-to-talk.png)

**PTT (Push To Talk) konfigurieren**

Push To Talk funktioniert nur mit einem Controller, nicht mit einer Tastatur. Ja, du musst einen Knopf deines Controllers opfern, erhältst aber Zugang zu über 200 Befehlen/Abfragen.

PTT-Einstellungen haben zwei Modi.

- **Schlaf/Wach umschalten** Diese Option schaltet die App einfach zwischen Schlaf- und Wachmodus um. Im Schlafmodus ignoriert die App alle Spracheingaben außer dem Befehl „Wake Up!". Das Umgehungswort „listen" oder „listen up" umgeht den Schlafmodus. _„Listen up!, Lower the landing gear."_ wird durchgeleitet.
- **PTT-Modus** Im reinen Push-To-Talk-Modus „schläft" die App und ignoriert alle Eingaben. Wenn der PTT-Knopf auf dem Controller gedrückt und gehalten wird, ertönt ein Piepton. Sag deinen Befehl oder deine Abfrage und lass den Knopf los. Ein weiterer Piepton zeigt an, dass deine Eingabe verarbeitet wird.

---

### <img src="images/stats.png" class="inline" height="20" alt="Stats"> Einstellungen / Statistiken

![tab-stats.png](images/tab-stats.png)

Der Statistik-Tab zeigt dir deinen Token-Verbrauch. Token sind grundlegende Einheiten der LLM-Berechnung. Ein Token entspricht einem einzelnen Wort oder einer Zahl.

Die Cloud-Modell-Integration ist pro Anbieter für maximales Token-Caching optimiert. Gecachte Token sind entweder kostenlos oder werden zu einem niedrigeren Satz abgerechnet. Dies hängt vom Anbieter ab. Im Durchschnitt verbraucht die App rund 250.000 Token pro Stunde insgesamt. Einige Cloud-Anbieter können bis zu 80 % davon cachen, andere etwa 40 %. Dies hängt von der gewählten Cloud ab.

Die Schätzung wird auf Basis deiner Nutzung angezeigt, sobald deine Sitzung länger als 15 Minuten läuft. Es handelt sich um eine ungefähre Berechnung.

Lokale LLMs zeigen keine gecachten Token an. Diese Information ist für lokale LLMs nicht relevant.
