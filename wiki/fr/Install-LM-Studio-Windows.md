## LLM local – Configuration Windows (LM Studio)

Faire tourner un LLM localement garde toutes les données privées et hors ligne. Aucun abonnement n'est requis. Des coûts matériels et électriques s'appliquent.

Il nécessite [LM Studio](https://lmstudio.ai) et un GPU suffisamment puissant.

---

### À qui s'adresse ce guide

Ce guide installe LM Studio à la main sur un PC qui **ne fait tourner que le modèle** : un second PC de votre
réseau, pendant que le jeu tourne sur votre PC de jeu, qui ne supporte alors plus aucune charge d'inférence.

> **Le jeu et le modèle sur un même PC ?** Ce guide n'est alors pas nécessaire. Si votre PC de jeu a une NVIDIA
> RTX 3090 (24 Go) ou mieux, ou deux cartes ou plus totalisant environ 20 Go, Elite Intel installe et
> configure LM Studio pour vous : voir
> [Choisir votre LLM](installing-local-llms).

### Matériel du PC du LLM

- **12 Go de mémoire vidéo.** Le modèle en prend environ 6,3 Go, ce qui laisse de la marge.
- **Une puissance de calcul de classe RTX 3080 ou mieux.** Une carte plus lente peut charger le modèle, mais
  répond trop lentement pour être utile.

---

### Modèle recommandé

| Modèle | VRAM requise | Notes |
|---|---|---|
| `google/gemma-4-e4b` | ~6,3 Go | ✅ Requis |

> **Pourquoi ce modèle ?** Il prend en charge le function calling requis par le compagnon. Un modèle incapable
> d'émettre un appel d'outil ne peut pas piloter l'application, aussi bien écrive-t-il.
> Voir [Choisir votre LLM](installing-local-llms).

---

Tutoriel vidéo très détaillé par @DawnTreaderToolsoftheElite 

> **Note :** La vidéo montre d'anciennes versions de LM Studio et d'Elite Intel. Là où elle diffère des étapes ci-dessous, suivez les étapes ci-dessous.

[[youtube:F5RgRRePrTo]]

---

### Étape 1 – Installer LM Studio

Ouvrez **PowerShell** et exécutez :

```powershell
irm https://lmstudio.ai/install.ps1 | iex
```

Cela installe le CLI `lms` et **llmster**, le serveur d'arrière-plan de LM Studio. L'application de bureau LM Studio n'est **pas** installée. Ouvrez une **nouvelle** fenêtre PowerShell après l'installation pour que les modifications prennent effet.

Vérifiez que ça fonctionne :

```powershell
lms --help
```

> **Note :** Si l'application de bureau LM Studio est déjà installée, le CLI `lms` peut déjà être disponible. Exécutez `lms --help` avant de lancer le script d'installation.

---

### Étape 2 – Télécharger le modèle

Démarrez le serveur d'arrière-plan de LM Studio, puis téléchargez le modèle :

```powershell
lms daemon up
lms get google/gemma-4-e4b
```

Si le nom ne correspond pas exactement, `lms get` liste les candidats trouvés ; choisissez-en
un avec les flèches et Entrée.

Pour lister les modèles téléchargés :

```powershell
lms ls
```

[LM Studio a un bug connu](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). Dans certains cas, le téléchargement échoue avec :
```Error: No staff picks found with the specified search criteria.```

Dans ce cas, téléchargez le fichier GGUF à la main depuis la page du modèle sur HuggingFace, puis importez-le :

```powershell
lms import C:\chemin\vers\le-modele.gguf
```

---

### Étape 3 – Démarrer le serveur

Chargez le modèle et démarrez le serveur d'inférence :

```powershell
lms daemon up
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start --bind 0.0.0.0
```

`lms daemon up` ne fait rien si le serveur d'arrière-plan tourne déjà : vous pouvez donc l'inclure à chaque fois.

`--bind 0.0.0.0` permet aux autres PC de votre réseau de joindre le serveur, qui par défaut ne répond qu'à ce PC.

**NOTE** : `--context-length 8192` est obligatoire, et doit valoir exactement 8192 : avec une fenêtre plus petite le modèle hallucine, avec une plus grande chaque réponse devient trop lente.

> **Laissez entrer le PC de jeu.** La première fois que le serveur écoute sur le réseau, le pare-feu Windows Defender peut demander s'il faut l'autoriser. Autorisez-le sur les réseaux **privés**. S'il ne demande jamais et que le PC de jeu ne se connecte pas, ajoutez une règle entrante pour le port TCP 1234.

Vérifiez depuis le **PC de jeu** en ouvrant un navigateur à cette adresse, avec l'adresse du PC du LLM à la place de `LLM_PC_IP` :

```
http://LLM_PC_IP:1234/v1/models
```

Vous devriez recevoir une liste JSON des modèles chargés. La chaîne d'identifiant du modèle dans cette réponse est celle que vous devrez saisir dans le champ **Modèle** d'Elite Intel.

Pour arrêter le serveur :

```powershell
lms server stop
```

> ⚠️ **Important :** Le serveur LM Studio ne **survit pas** aux redémarrages. Exécutez à nouveau les trois commandes de l'étape 3 après chaque redémarrage, ou utilisez l'une des options de démarrage automatique ci-dessous.

---

### Étape 4 – (Optionnel) Démarrage automatique au démarrage

Deux options sont disponibles pour maintenir le serveur en fonctionnement après les redémarrages. Si vous avez suivi l'étape 1, utilisez l'**option A**. L'option B ne concerne que ceux qui ont aussi installé l'application de bureau LM Studio.

#### Option A – Planificateur de tâches (sans application de bureau)

Enregistrez d'abord ce script sous `start-lmstudio.ps1` dans un emplacement permanent (par ex. `C:\Scripts\`) :

```powershell
$lms = "$env:USERPROFILE\.lmstudio\bin\lms.exe"
& $lms daemon up
& $lms load google/gemma-4-e4b --yes --context-length 8192 --gpu max
& $lms server start --bind 0.0.0.0
```

Il démarre le serveur d'arrière-plan, charge le modèle avec les réglages dont Elite Intel a besoin et démarre le serveur d'inférence : les trois mêmes commandes qu'à l'étape 3.

Créez ensuite la tâche :

1. Appuyez sur **Win + R**, tapez `taskschd.msc`, appuyez sur Entrée.
2. Cliquez sur **Créer une tâche** dans le panneau de droite.
3. **Onglet Général** : Nommez-la `LM Studio Server`. Laissez **« Exécuter avec les privilèges les plus élevés »** décoché : LM Studio n'a pas besoin des droits d'administrateur.
4. **Onglet Déclencheurs** : Cliquez sur Nouveau → **« À la connexion »** → OK.
5. **Onglet Actions** : Cliquez sur Nouveau → **« Démarrer un programme »**.
   - Programme/script : `powershell.exe`
   - Ajouter des arguments : `-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "C:\Scripts\start-lmstudio.ps1"`

`-WindowStyle Hidden` évite qu'une fenêtre de console reste ouverte à chaque connexion. Une fenêtre peut apparaître brièvement au démarrage du script.

#### Option B – Application de bureau

Cette option n'est disponible que si l'application de bureau LM Studio est installée. L'étape 1 ne l'installe pas.

1. Ouvrez LM Studio et appuyez sur **Ctrl + ,** pour ouvrir les Paramètres.
2. Cochez **« Run LLM server on login »**.
3. Dans les paramètres du serveur, activez **Serve on Local Network**, pour que le PC de jeu puisse le joindre.
4. Fermer l'application la minimise dans la barre système et maintient le serveur en fonctionnement. Elle se restaure automatiquement à la prochaine connexion.

> ⚠️ **Longueur de contexte :** Avec cette option, LM Studio ne charge le modèle qu'à l'arrivée de la première requête, avec les réglages **par défaut** du modèle au lieu du `--context-length 8192` de l'étape 3. Ouvrez les réglages par défaut du modèle dans l'application de bureau et réglez la **longueur de contexte** sur **8192**. Sinon, la fenêtre de contexte risque d'être trop petite.

---

### Étape 5 – Configurer Elite Intel

**Sur le PC de jeu :** Dans Elite Intel, ouvrez **Services → Services IA**. Sous **Modèle de langage (LLM)**, sélectionnez **LMStudio** :

- **Adresse** : `http://LLM_PC_IP:1234/v1/chat/completions`, en remplaçant `LLM_PC_IP` par l'adresse du PC du LLM sur votre réseau (`ipconfig` sur le PC du LLM l'affiche).
- **Modèle** : collez la chaîne d'identifiant du modèle obtenue depuis `http://LLM_PC_IP:1234/v1/models`. Un seul modèle sert aux commandes comme aux requêtes.

Un LLM local ne nécessite pas de clé d'API.

Cliquez sur **Enregistrer**. Elite Intel redémarre alors le modèle de langage de lui-même.

---

Communauté 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
