
# No local hardware? Use a cloud LLM.


**LLM (AI Brain)**

*Cloud option:* Enter your API key for Mistral, xAI, OpenAI, or Anthropic/Claude. The app uses a fixed model per provider:
- **Mistral**: 'mistral-small-2506' **(Free Tear)**
- **xAI**: `grok-4-1-fast-non-reasoning`
- **OpenAI**: `gpt-4.1-mini` (commands) / `gpt-5.2` (queries)
- **Gemini Generative Language API**: `gemini-3.1-flash-lite-preview` for commands and queries
- **Anthropic/Claude**
- **DeepSeek**


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
