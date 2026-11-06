# Keine lokale Hardware? Nutze ein Cloud-LLM.


**LLM (KI-Gehirn)**

*Cloud-Option:* Öffne **Einstellungen → KI-Dienste → Cloud-Einrichtung**, wähle deinen Anbieter in
der Liste **Anbieter**, füge den API-Schlüssel dieses Anbieters ein, setze den Haken bei
**Gesperrt** und drücke **Speichern**.

Den Anbieter wählst du selbst. Elite Intel errät ihn nicht am Schlüssel, denn die Anbieter ändern
ihre Schlüsselformate zu oft, als dass das zuverlässig wäre. Ein Modell wählst du nicht: Elite
Intel wählt es für deinen Anbieter aus:

| Anbieter | Verwendetes Modell |
|----------|--------------------|
| **xAI** | `grok-4-1-fast-non-reasoning` |
| **OpenAI** | `gpt-5.4-mini` |
| **Gemini** (Generative Language API) | `gemini-3.1-flash-lite-preview` |
| **Anthropic / Claude** | `claude-haiku-4-5` |
| **DeepSeek** | `deepseek-v4-flash` |


Die Kosten hängen davon ab, welchen Cloud-Dienst du wählst und wie lange du spielst.

### Option A: xAI-API-Schlüssel
1. Gehe zur [xAI Console](https://console.x.ai/).
2. Registriere dich oder melde dich an.
3. Navigiere zum API-Bereich und generiere einen neuen API-Schlüssel.
4. Lade Guthaben auf dein Konto.
5. Wähle unter **Einstellungen → KI-Dienste → Cloud-Einrichtung** den Anbieter **xAI (Grok)**, füge den Schlüssel in **API-Schlüssel** ein, setze den Haken bei **Gesperrt** und drücke **Speichern**.

### Option B: OpenAI-API-Schlüssel
1. Gehe zur [OpenAI Platform](https://platform.openai.com/).
2. Registriere dich oder melde dich an.
3. Navigiere zum API-Bereich und generiere einen neuen API-Schlüssel.
4. Wähle unter **Einstellungen → KI-Dienste → Cloud-Einrichtung** den Anbieter **OpenAI**, füge den Schlüssel in **API-Schlüssel** ein, setze den Haken bei **Gesperrt** und drücke **Speichern**.

### Option C: Anthropic/Claude-API-Schlüssel
1. Gehe zur [Claude Platform](https://platform.claude.com).
2. Melde dich per E-Mail oder Google an. Hinweis: Die Anmeldung läuft über einen Magic Link, der an deine E-Mail-Adresse geschickt wird.
3. Gehe zu **Settings → Billing** und lade Guthaben auf, bevor du einen Schlüssel erstellst. Ein Schlüssel, der auf einem Konto ohne Guthaben erstellt wurde, funktioniert nicht, auch wenn später Guthaben hinzukommt.
4. Gehe zu **API Keys** und erstelle einen Schlüssel.
5. Wähle unter **Einstellungen → KI-Dienste → Cloud-Einrichtung** den Anbieter **Anthropic (Claude)**, füge den Schlüssel in **API-Schlüssel** ein, setze den Haken bei **Gesperrt**, drücke **Speichern** und starte dann die Dienste im Reiter Vega (neu).

### Option D: Google-Gemini-API-Schlüssel
1. Gehe zu [Google AI Studio](https://aistudio.google.com/api-keys) und melde dich mit deinem Google-Konto an.
2. Klicke unten links auf das **Schlüssel-Symbol**.
3. Klicke auf **Create API key**. Falls du aufgefordert wirst, ein Projekt zu erstellen, erstelle eines mit einem beliebigen Namen.
4. Du wirst nach einem Zahlungskonto (Billing) gefragt. Dafür kannst du dein Google-Konto verwenden.
5. Kopiere den Schlüssel.
6. Wähle unter **Einstellungen → KI-Dienste → Cloud-Einrichtung** den Anbieter **Google (Gemini)**, füge den Schlüssel in **API-Schlüssel** ein, setze den Haken bei **Gesperrt**, drücke **Speichern** und starte dann die Dienste im Reiter Vega (neu).

### Einen Google-TTS-Schlüssel besorgen

1. Gehe zur [Google Cloud Console](https://console.cloud.google.com/).
2. Melde dich an oder erstelle ein Konto.
3. Erstelle ein neues Projekt.
4. Aktiviere die **Cloud Text-to-Speech API**.
5. Gehe zu **Credentials**, erstelle einen API-Schlüssel und kopiere ihn.
6. **Schränke den Schlüssel ein**: Klicke auf den eben erstellten Schlüssel. Klicke auf der Detailseite auf **Restrict key**. Eine Auswahlliste erscheint. Hake jede API an, die du aktiviert hast (STT und/oder TTS), und klicke auf **Save**.
7. Füge den Schlüssel unter **Einstellungen → KI-Dienste → Sprache (TTS) → Google-TTS-Schlüssel** ein, setze den Haken bei **Gesperrt** und drücke **Speichern**.
