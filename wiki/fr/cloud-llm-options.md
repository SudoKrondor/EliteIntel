# Pas de matériel local ? Utilisez un LLM cloud.


**LLM (cerveau de l'IA)**

*Option cloud :* ouvrez **Services → Services IA → Configuration cloud**, choisissez votre
fournisseur dans la liste **Fournisseur**, collez la clé API de ce fournisseur, cochez
**Verrouillé** et cliquez sur **Enregistrer**.

C'est vous qui choisissez le fournisseur. Elite Intel ne le devine pas à partir de la clé, car les
fournisseurs changent trop souvent le format de leurs clés pour que ce soit fiable. Vous ne
choisissez pas de modèle : Elite Intel le sélectionne pour votre fournisseur :

| Fournisseur | Modèle utilisé |
|-------------|----------------|
| **Mistral** *(offre gratuite)* | `mistral-small-2506` |
| **xAI** | `grok-4-1-fast-non-reasoning` |
| **OpenAI** | `gpt-5.4-mini` |
| **Gemini** (Generative Language API) | `gemini-3.1-flash-lite-preview` |
| **Anthropic / Claude** | `claude-haiku-4-5` |
| **DeepSeek** | `deepseek-v4-flash` |


Le coût dépend du service cloud choisi et de votre temps de jeu.

### Option cloud GRATUITE : Mistral
1. Allez sur la [Mistral Console](https://console.mistral.ai/)
2. Créez un compte avec une adresse e-mail valide que vous pouvez vérifier.
3. AUCUNE CARTE BANCAIRE NÉCESSAIRE
4. Créez une « Organisation » (nommez-la comme vous voulez, par exemple « Elite Intel »)
5. Générez une clé API.
6. Dans **Services → Services IA → Configuration cloud**, choisissez **Mistral** comme fournisseur, collez la clé, cliquez sur **Enregistrer** et redémarrez les services dans l'onglet Vega.


### Option A : clé API xAI
1. Allez sur la [xAI Console](https://console.x.ai/).
2. Inscrivez-vous ou connectez-vous.
3. Accédez à la section API et générez une nouvelle clé API.
4. Ajoutez des crédits à votre compte.
5. Dans **Services → Services IA → Configuration cloud**, choisissez **xAI (Grok)** comme fournisseur, collez la clé dans **Clé API**, cochez **Verrouillé** et cliquez sur **Enregistrer**.

### Option B : clé API OpenAI
1. Allez sur l'[OpenAI Platform](https://platform.openai.com/).
2. Inscrivez-vous ou connectez-vous.
3. Accédez à la section API et générez une nouvelle clé API.
4. Dans **Services → Services IA → Configuration cloud**, choisissez **OpenAI** comme fournisseur, collez la clé dans **Clé API**, cochez **Verrouillé** et cliquez sur **Enregistrer**.

### Option C : clé API Anthropic/Claude
1. Allez sur la [Claude Platform](https://platform.claude.com).
2. Connectez-vous par e-mail ou avec Google. Remarque : la connexion se fait par un lien magique envoyé à votre adresse e-mail.
3. Allez dans **Settings → Billing** et ajoutez des crédits avant de créer une clé. Une clé créée sur un compte non approvisionné ne fonctionne pas, même si des crédits sont ajoutés ensuite.
4. Allez dans **API Keys** et créez une clé.
5. Dans **Services → Services IA → Configuration cloud**, choisissez **Anthropic (Claude)** comme fournisseur, collez la clé dans **Clé API**, cochez **Verrouillé**, cliquez sur **Enregistrer**, puis démarrez ou redémarrez les services dans l'onglet Vega.

### Option D : clé API Google Gemini
1. Allez sur [Google AI Studio](https://aistudio.google.com/api-keys) et connectez-vous avec votre compte Google.
2. Cliquez sur l'**icône de clé** en bas à gauche.
3. Cliquez sur **Create API key**. S'il vous demande de créer un projet, créez-en un avec le nom de votre choix.
4. Un compte de paiement (facturation) vous sera demandé. Vous pouvez utiliser votre compte Google.
5. Copiez la clé.
6. Dans **Services → Services IA → Configuration cloud**, choisissez **Google (Gemini)** comme fournisseur, collez la clé dans **Clé API**, cochez **Verrouillé**, cliquez sur **Enregistrer**, puis démarrez ou redémarrez les services dans l'onglet Vega.

### Obtenir une clé Google TTS

1. Allez sur la [Google Cloud Console](https://console.cloud.google.com/).
2. Connectez-vous ou créez un compte.
3. Créez un nouveau projet.
4. Activez la **Cloud Text-to-Speech API**.
5. Allez dans **Credentials**, créez une clé API et copiez-la.
6. **Restreignez la clé** : cliquez sur la clé que vous venez de créer. Sur la page de détails, cliquez sur **Restrict key**. Une liste déroulante apparaît. Cochez chaque API que vous avez activée (STT et/ou TTS), puis cliquez sur **Save**.
7. Collez la clé dans **Services → Services IA → Voix (TTS) → Clé Google TTS**, cochez **Verrouillé** et cliquez sur **Enregistrer**.
