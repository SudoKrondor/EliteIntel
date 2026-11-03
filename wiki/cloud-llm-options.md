
# No local hardware? Use a cloud LLM.


**LLM (AI Brain)**

*Cloud option:* open **Settings → AI Services → Cloud Setup**, pick your provider from the
**Provider** list, paste that provider's API key, tick **Locked**, and press **Save**.

You choose the provider yourself. Elite Intel does not guess it from the key, because providers
change their key formats too often for that to be reliable. You don't choose a model: Elite Intel
selects it for your provider:

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
1. Go to [Mistral Console](https://console.mistral.ai/)
2. Create account with a valid email you can verify.
3. NO CREDIT CARD NECESSARY
4. Create a "Organisation" (Call it whatever you want. For example "Elite Intel")
5. Generate an API key.
6. In **Settings → AI Services → Cloud Setup**, pick **Mistral** as the provider, paste the key, press **Save**, and restart services on the Vega tab.


### Option A: xAI API Key
1. Go to the [xAI Console](https://console.x.ai/).
2. Sign up or log in.
3. Navigate to the API section and generate a new API key.
4. Add credits to your account.
5. In **Settings → AI Services → Cloud Setup**, pick **xAI (Grok)** as the provider, paste the key into **API Key**, tick **Locked**, and press **Save**.

### Option B: OpenAI API Key
1. Go to the [OpenAI Platform](https://platform.openai.com/).
2. Sign up or log in.
3. Navigate to the API section and generate a new API key.
4. In **Settings → AI Services → Cloud Setup**, pick **OpenAI** as the provider, paste the key into **API Key**, tick **Locked**, and press **Save**.

### Option C: Anthropic/Claude API Key
1. Go to the [Claude Platform](https://platform.claude.com).
2. Sign in with email or Google. Note: authentication uses a magic link sent to your email.
3. Go to **Settings → Billing** and add credits before creating a key. A key created on an unfunded account does not function even if credits are added afterward.
4. Go to **API Keys** and create a key.
5. In **Settings → AI Services → Cloud Setup**, pick **Anthropic (Claude)** as the provider, paste the key into **API Key**, tick **Locked**, press **Save**, then start or restart services on the Vega tab.

### Option D: Google Gemini API Key
1. Go to [Google AI Studio](https://aistudio.google.com/api-keys) and sign in with your Google account.
2. Click the **key icon** at the bottom left.
3. Click **Create API key**. If it asks you to create a project, create one with any name you like.
4. It will ask for a payment (billing) account. Your Google account can be used for this.
5. Copy the key.
6. In **Settings → AI Services → Cloud Setup**, pick **Google (Gemini)** as the provider, paste the key into **API Key**, tick **Locked**, press **Save**, then start or restart services on the Vega tab.

### Getting a Google TTS Key

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Sign in or create an account.
3. Create a new project.
4. Enable the **Cloud Text-to-Speech API**.
5. Go to **Credentials**, create an API key, and copy it.
6. **Restrict the key**: Click the key you just created. On the key detail page, click **Restrict key**. A dropdown appears. Check each API you enabled (STT and/or TTS), then click **Save**.
7. Paste the key into **Settings → AI Services → Speech (TTS) → Google TTS Key**, tick **Locked**, and press **Save**.
