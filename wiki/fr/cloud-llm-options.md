
# Pas de matériel local ? Utilisez un LLM cloud.


**LLM (Cerveau IA)**

*Option cloud :* Entrez votre clé API pour Mistral, xAI, OpenAI ou Anthropic/Claude. L'application utilise un modèle fixe par fournisseur :
- **Mistral**: 'mistral-small-2506' **(Niveau gratuit)**
- **xAI**: `grok-4-1-fast-non-reasoning`
- **OpenAI**: `gpt-4.1-mini` (commandes) / `gpt-5.2` (requêtes)
- **Gemini Generative Language API**: `gemini-3.1-flash-lite-preview` pour les commandes et requêtes
- **Anthropic/Claude**
- **DeepSeek**


Le coût variera en fonction du service cloud choisi et de la durée de jeu.

### Option CLOUD GRATUITE : Mistral
1. Rendez-vous sur la [Console Mistral](https://console.mistral.ai/home)
2. Créez un compte avec une adresse e-mail valide que vous pouvez vérifier.
3. AUCUNE CARTE BANCAIRE NÉCESSAIRE
4. Créez une « Organisation » (appelez-la comme vous voulez, par exemple « Elite Intel »)
5. Générez une clé API. Entrez cette clé dans l'application et redémarrez l'application.


### Option A : Clé API xAI
1. Rendez-vous sur la [Console xAI](https://console.x.ai/).
2. Inscrivez-vous ou connectez-vous.
3. Accédez à la section API et générez une nouvelle clé API.
4. Ajoutez des crédits à votre compte.
5. Collez la clé dans le champ **LLM** et cochez la case de verrouillage.

### Option B : Clé API OpenAI
1. Rendez-vous sur la [Plateforme OpenAI](https://platform.openai.com/).
2. Inscrivez-vous ou connectez-vous.
3. Accédez à la section API et générez une nouvelle clé API.
4. Collez la clé dans le champ **LLM** et cochez la case de verrouillage.

### Option C : Clé API Anthropic/Claude
1. Rendez-vous sur la [Plateforme Claude](https://platform.claude.com).
2. Connectez-vous avec votre e-mail ou Google. Remarque : l'authentification utilise un lien magique envoyé à votre adresse e-mail.
3. Accédez à **Paramètres → Facturation** et ajoutez des crédits avant de créer une clé. Une clé créée sur un compte non approvisionné ne fonctionne pas même si des crédits sont ajoutés ultérieurement.
4. Accédez à **Clés API** et créez une clé.
5. Collez-la dans le champ **LLM**, cochez la case de verrouillage, puis démarrez ou redémarrez les services dans l'onglet IA.

### Obtenir une clé Google TTS (14 voix)

1. Rendez-vous sur la [Console Google Cloud](https://console.cloud.google.com/).
2. Connectez-vous ou créez un compte.
3. Créez un nouveau projet.
4. Activez l'**API Generative Language** pour le LLM et/ou l'**API Cloud Text-to-Speech** pour la TTS.
5. Accédez à **Identifiants**, créez une clé API et copiez-la.
6. **Restreindre la clé** : Cliquez sur la clé que vous venez de créer. Sur la page de détail de la clé, cliquez sur **Restreindre la clé**. Un menu déroulant apparaît. Cochez chaque API activée (STT et/ou TTS), puis cliquez sur **Enregistrer**.
7. Collez la clé dans les champs **Reconnaissance vocale** et/ou **Synthèse vocale** de l'application. Cochez les cases de verrouillage.
