# UI and Configuration Options

### AI Tab <img src="images/ai.png" class="inline" height="20" alt="AI">
This is the main/default tab.
- **Start / Stop Services**: Toggles the AI stack on/off. Stop and restart here after any service setting change. Services do not reload automatically.
- **Calibrate**: Calibrates the app to your room noise level, microphone, and audio setup. Run on first use and after any hardware change.
- **OBS Overlay**: When ON, the app displays an animated black window showing text from your input and AI responses. Add it to OBS as a screen capture, apply a Chroma Key filter, set the color to black, and set Similarity, Smoothness, and Key Color Spill Reduction to 0. Note: if the window is relaunched, it must be re-added in OBS.
- **Listen to me / Ignore me**: Toggles input monitoring. When ignoring, the app does not respond unless trigger words such as "ship" or "listen" are detected. Say "stop ignoring me" to resume.
- **Detailed log on/off**: Toggles detailed logging.

---

### Player Tab <img src="images/controller.png" class="inline" height="20" alt="Player">
- **Commander Name**: If your in-game name is difficult for Text-to-Speech to pronounce, enter an easier alternative here.
- **Journal and Bindings Directory**: On Windows this defaults to the standard location automatically. On Linux, set up symlinks as described in the [installation guide](Installation-and-Configuration).
- **Fleet Management**: Assign voices, personalities, and cadence to individual ships. Personality and cadence require a cloud LLM. Settings can also be changed via voice command: "Change Name to George."


### Actions Tab <img src="images/keys-binding.png" class="inline" height="20" alt="Actions">
- **Commander Name**: If your in-game name is difficult for Text-to-Speech to pronounce, enter an easier alternative here.
- **Journal and Bindings Directory**: On Windows this defaults to the standard location automatically. On Linux, set up symlinks as described in the [installation guide](Installation-and-Configuration).
- **Fleet Management**: Assign voices, personalities, and cadence to individual ships. Personality and cadence require a cloud LLM. Settings can also be changed via voice command: "Change Name to George."



---

### Settings / Local LLM Tab <img src="images/settings.png" class="inline" height="20" alt="Settings">
- Set the address of your inference server. Defaults to `localhost` with the Ollama URL.
- Provide the names of the models to use. See the [Local LLM guide](installing-local-llms).
- **LLM host** radio buttons: Select between Ollama and LM Studio.
- **Use checkbox**: Enable to use the local model instead of the cloud.

---

### Settings / Audio <img src="images/mic.png" class="inline" height="20" alt="Audio">
- **Speech Volume**: Controls the volume of speech synthesis.
- **TTS Voice Speed**: Controls the speed of speech synthesis.
- **Beep Volume**: Controls the volume of the beep indicator. Indicates that STT has finished processing and the LLM has received input.
- **STT Threads**: Sets the thread allocation for STT processing. This is a min/max setting. The app requests the minimum but uses what the processor provides. Threads are released after processing completes.
- **Use Local Text To Speech**: Overrides the cloud TTS key and uses local TTS.
- **Audio Wave Visualizer**: Displays a dynamic graph of the audio input. Shows the noise floor, audio signal, gate zones, and clipping if present.


### Settings / Cloud LLM Tab <img src="images/cloud.png" class="inline" height="20" alt="Cloud">
- **Cloud LLM Key**: Enter your API key. Supported providers: Gemini, OpenAI, Grok, Mistral, Deepseek, and Anthropic/Claude.
- **Cloud TTS Key**: Enter your API key. Supported provider: Google.
- **Note**: Uncheck the "Use" checkbox in Local LLM. It overrides the cloud LLM key.


---

**LLM (AI Brain)**

*Cloud option:* Enter your API key for Mistral, xAI, OpenAI, or Anthropic/Claude. The app uses a fixed model per provider:
- **Mistral**: 'mistral-small-2506' (Free with hourly limit)
- **xAI**: `grok-4-1-fast-non-reasoning`
- **OpenAI**: `gpt-4.1-mini` (commands) / `gpt-5.2` (queries)
- **Gemini Generative Language API**: `gemini-3.1-flash-lite-preview` for commands and queries
- **Anthropic/Claude**

*Local option:* Leave the key blank, fill in the local LLM fields below, and check **☑ Use** next to the local LLM. See [Local LLM guide (Linux)](Install-Ollama-Local-LLM-Linux) / [Local LLM guide (Windows)](Install-Ollama-Local-LLM-Windows).
- **LLM Address**: defaults to `localhost`. Replace with the IP of another PC if Ollama runs on a separate machine.
- **Command LLM**: handles voice command interpretation.
- **Query LLM**: handles data analysis. `tulu3:8b` is the minimum. Larger models produce better results.

---

# No local hardware? Use a cloud LLM.

Cost will be different depending on which Cloud service you choose, and how long you play.

### FREE CLOUD Option: Mistral
1. Go to [Mistral Console](https://console.mistral.ai/home)
2. Create account with a valid email you can verify.
3. NO CREDIT CARD NECESSARY
4. Create a "Organisation" (Call it whatever you want. For example "Elite Intel")
5. Generate an API key. Enter that key in to the app and restart the app.


### Option A: xAI API Key
1. Go to the [xAI Console](https://console.x.ai/).
2. Sign up or log in.
3. Navigate to the API section and generate a new API key.
4. Add credits to your account.
5. Paste the key into the **LLM** field and check the lock box.

### Option B: OpenAI API Key
1. Go to the [OpenAI Platform](https://platform.openai.com/).
2. Sign up or log in.
3. Navigate to the API section and generate a new API key.
4. Paste the key into the **LLM** field and check the lock box.

### Option C: Anthropic/Claude API Key
1. Go to the [Claude Platform](https://platform.claude.com).
2. Sign in with email or Google. Note: authentication uses a magic link sent to your email.
3. Go to **Settings → Billing** and add credits before creating a key. A key created on an unfunded account does not function even if credits are added afterward.
4. Go to **API Keys** and create a key.
5. Paste it into the **LLM** field, check the lock box, and start or restart services on the AI tab.

### Getting a Google TTS Key (14 voices)

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Sign in or create an account.
3. Create a new project.
4. Enable the **Generative Language API** for LLM and/or **Cloud Text-to-Speech API** for TTS.
5. Go to **Credentials**, create an API key, and copy it.
6. **Restrict the key**: Click the key you just created. On the key detail page, click **Restrict key**. A dropdown appears. Check each API you enabled (STT and/or TTS), then click **Save**.
7. Paste the key into the **Speech to Text** and/or **Text to Speech** fields in the app. Check the lock boxes.

---

## App Settings and Data Directory

App settings and data are stored in a SQLite database located at:
- **Linux:** `~/.local/share/elite-intel/elite-intel/db/`
- **Windows:** `%APPDATA%\elite-intel\db\`

----
For issues, contact via Matrix. Bug reports and pull requests are welcome.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
