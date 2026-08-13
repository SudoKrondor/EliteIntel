
# No local hardware? Use a cloud LLM.


**LLM (AI Brain)**

*Cloud option:* paste your API key into **Settings → AI Services → Cloud Setup** and press
**Save**. You do not pick a model — Elite Intel recognises the provider from your key and
selects the model itself:

| Provider | Model used |
|----------|------------|
| **Mistral** *(free tier)* | `mistral-small-2506` |
| **xAI** | `grok-4-1-fast-non-reasoning` |
| **OpenAI** | `gpt-5.4-mini` |
| **Gemini** (Generative Language API) | `gemini-3.1-flash-lite-preview` |
| **Anthropic / Claude** | `claude-haiku-4-5` |
| **DeepSeek** | `deepseek-v4-flash` |


Cost will be different depending on which Cloud service you choose, and how long you play.

### FREE CLOUD Option: Mistral
1. Go to [Mistral Console](https://console.mistral.ai/home)
2. Create account with a valid email you can verify.
3. NO CREDIT CARD NECESSARY
4. Create a "Organisation" (Call it whatever you want. For example "Elite Intel")
5. Generate an API key. Paste it into **Settings → AI Services → Cloud Setup**, press **Save**, and restart services on the Vega tab.


### Option A: xAI API Key
1. Go to the [xAI Console](https://console.x.ai/).
2. Sign up or log in.
3. Navigate to the API section and generate a new API key.
4. Add credits to your account.
5. Paste the key into the **API Key** field on *Settings → AI Services*, tick **Locked**, and press **Save**.

### Option B: OpenAI API Key
1. Go to the [OpenAI Platform](https://platform.openai.com/).
2. Sign up or log in.
3. Navigate to the API section and generate a new API key.
4. Paste the key into the **API Key** field on *Settings → AI Services*, tick **Locked**, and press **Save**.

### Option C: Anthropic/Claude API Key
1. Go to the [Claude Platform](https://platform.claude.com).
2. Sign in with email or Google. Note: authentication uses a magic link sent to your email.
3. Go to **Settings → Billing** and add credits before creating a key. A key created on an unfunded account does not function even if credits are added afterward.
4. Go to **API Keys** and create a key.
5. Paste it into the **API Key** field on *Settings → AI Services*, tick **Locked**, press **Save**, then start or restart services on the Vega tab.

### Getting a Google TTS Key

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Sign in or create an account.
3. Create a new project.
4. Enable the **Generative Language API** for LLM and/or **Cloud Text-to-Speech API** for TTS.
5. Go to **Credentials**, create an API key, and copy it.
6. **Restrict the key**: Click the key you just created. On the key detail page, click **Restrict key**. A dropdown appears. Check each API you enabled (STT and/or TTS), then click **Save**.
7. Paste the key into **Settings → AI Services → Speech (TTS) → Google TTS Key**, tick **Locked**, and press **Save**.
