# Interface utilisateur et options de configuration

### Onglet IA <img src="images/ai.png" class="inline" height="20" alt="AI">

Il s'agit de l'onglet principal/par défaut.

|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![tab-ai-buttons.png](images/tab-ai-buttons.png) | - **Démarrer / Arrêter les services**: Active ou désactive la pile IA.<br/>- **Éveil/Veille**: En mode Éveil, l'application écoute en permanence ; en mode Veille, elle ignore les entrées à moins qu'un bouton PTT soit pressé, que le mot de contournement « Listen » soit utilisé ou que la commande « Wake up! » soit émise.<br/>- **OBS Overlay**: Affiche une fenêtre de superposition noire avec les interactions Commandant / IA. À ajouter dans OBS en masquant le fond noir.<br/>- **Périphériques audio**: Sélectionnez le périphérique audio d'entrée/sortie. **Calibrer l'audio**: Lance la calibration audio pour de meilleures performances. |
|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |


---

### Onglet Joueur <img src="images/controller.png" class="inline" height="20" alt="Player">

![playertab](images/tab-player.png)

- **Nom du Commandant**: Utilisez ce champ pour remplacer votre nom en jeu pour la synthèse vocale.
- **Options du vaisseau**: Vous pouvez activer ou désactiver ces automatisations. Utile pour les commandants en situation de handicap.
- **Gestion de la flotte**: Attribuez des voix, des personnalités et un cadence aux vaisseaux individuels. La personnalité ne fonctionne qu'avec les LLM en cloud. L'icône d'engrenage ouvre les propriétés du vaisseau, telles que le honk automatique et le profil commercial.

![popup-ship-properties.png](images/popup-ship-properties.png)

- **Honk au saut dans le système**: Sélectionnez le groupe de feu et la gâchette. Si cette option est cochée, le vaisseau effectuera un scan de découverte à l'entrée. Si votre HUD est en mode Combat, il passera en mode Analyse, effectuera le scan, puis reviendra au mode précédent.
- **Personnaliser votre profil commercial**: Ces paramètres peuvent être définis via l'interface utilisateur ou par commandes vocales : « alter/change trade profile set [paramètre] to [valeur] »

---



### Onglet Actions <img src="images/keys-binding.png" class="inline" height="20" alt="Actions">

![tab-actions.png](images/tab-actions.png)

L'onglet **Actions / Liaisons** comporte trois sections : Liaisons, Commandes intégrées et Commandes personnalisées.

- **Liaisons** : répertoire où se trouve votre fichier de liaisons du jeu. Sans celui-ci, l'application ne peut pas contrôler le jeu.
- **Profil** : votre profil de liaisons actuel en jeu.
- **Fichier** : le fichier contenant les liaisons actuellement utilisées.

Vous pouvez modifier vos liaisons depuis cet écran et les enregistrer en tant que nouveau profil.

__REMARQUE – Les HOTAS/CONTRÔLEURS sont affichés mais ne peuvent pas être configurés depuis cet écran. Les liaisons clavier uniquement (susceptible d'évoluer dans le futur).__


**Actions / Commandes intégrées**

![tab-action-build-in-commands.png](images/tab-action-build-in-commands.png)


Fournit une liste de commandes intégrées. Un double-clic sur l'une d'elles affiche une boîte de dialogue avec des informations sur la commande et vous permet de proposer une meilleure traduction pour la localisation.

---

### Paramètres / Onglet LLM local <img src="images/settings.png" class="inline" height="20" alt="Settings">
- Définissez l'adresse de votre serveur d'inférence. Par défaut : `localhost` avec l'URL Ollama.
- Indiquez les noms des modèles à utiliser. Voir le [guide LLM local](installing-local-llms).
- **Hôte LLM** boutons radio : Choisissez entre Ollama et LM Studio.
- **Case à cocher Utiliser**: Activez pour utiliser le modèle local à la place du cloud.

---

### Paramètres / Audio <img src="images/mic.png" class="inline" height="20" alt="Audio">
- **Volume de la parole**: Contrôle le volume de la synthèse vocale.
- **Vitesse de la voix TTS**: Contrôle la vitesse de la synthèse vocale.
- **Volume du bip**: Contrôle le volume de l'indicateur sonore. Indique que le STT a terminé le traitement et que le LLM a reçu l'entrée.
- **Threads STT**: Définit l'allocation de threads pour le traitement STT. Il s'agit d'un réglage min/max. L'application demande le minimum mais utilise ce que le processeur fournit. Les threads sont libérés après traitement.
- **Utiliser la synthèse vocale locale**: Remplace la clé TTS cloud et utilise la TTS locale.
- **Visualiseur d'onde audio**: Affiche un graphe dynamique de l'entrée audio. Montre le plancher de bruit, le signal audio, les zones de porte et les écrêtages éventuels.


### Paramètres / Onglet LLM cloud <img src="images/cloud.png" class="inline" height="20" alt="Cloud">
- **Clé LLM cloud**: Entrez votre clé API. Fournisseurs pris en charge : Gemini, OpenAI, Grok, Mistral, Deepseek et Anthropic/Claude.
- **Clé TTS cloud**: Entrez votre clé API. Fournisseur pris en charge : Google.
- **Remarque**: Décochez la case « Utiliser » dans le LLM local. Elle prend le dessus sur la clé LLM cloud.


---

**LLM (Cerveau IA)**

*Option cloud :* Entrez votre clé API pour Mistral, xAI, OpenAI ou Anthropic/Claude. L'application utilise un modèle fixe par fournisseur :
- **Mistral**: 'mistral-small-2506' (Gratuit avec limite horaire)
- **xAI**: `grok-4-1-fast-non-reasoning`
- **OpenAI**: `gpt-4.1-mini` (commandes) / `gpt-5.2` (requêtes)
- **Gemini Generative Language API**: `gemini-3.1-flash-lite-preview` pour les commandes et requêtes
- **Anthropic/Claude**

*Option locale :* Laissez la clé vide, remplissez les champs LLM local ci-dessous et cochez **☑ Utiliser** à côté du LLM local. Voir [Guide LLM local (Linux)](Install-Ollama-Local-LLM-Linux) / [Guide LLM local (Windows)](Install-Ollama-Local-LLM-Windows).
- **Adresse LLM**: par défaut `localhost`. Remplacez par l'IP d'un autre PC si Ollama s'exécute sur une machine séparée.
- **LLM de commandes**: gère l'interprétation des commandes vocales.
- **LLM de requêtes**: gère l'analyse des données. `tulu3:8b` est le minimum. Les modèles plus grands produisent de meilleurs résultats.

---

# Pas de matériel local ? Utilisez un LLM cloud.

Le coût variera en fonction du service cloud choisi et de la durée de jeu.

### Option CLOUD GRATUITE : Mistral
1. Rendez-vous sur la [Console Mistral](https://console.mistral.ai/home)
2. Créez un compte avec une adresse e-mail valide que vous pouvez vérifier.
3. AUCUNE CARTE BANCAIRE NÉCESSAIRE
4. Créez une « Organisation » (appelez-la comme vous voulez, par exemple « Elite Intel »)
5. Générez une clé API. Entrez cette clé dans l'application et redémarrez-la.


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

---

## Paramètres de l'application et répertoire de données

Les paramètres et données de l'application sont stockés dans une base de données SQLite située à :
- **Linux:** `~/.local/share/elite-intel/elite-intel/db/`
- **Windows:** `%APPDATA%\elite-intel\db\`

----
Pour tout problème, contactez-nous via Matrix. Les rapports de bugs et les pull requests sont les bienvenus.

Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
