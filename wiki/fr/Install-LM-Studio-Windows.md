## LLM local – Configuration Windows (LM Studio)

Faire tourner un LLM localement garde toutes les données privées et hors ligne. Aucun abonnement n'est requis. Des coûts matériels et électriques s'appliquent.

LM Studio est une alternative à Ollama. Il utilise les mêmes modèles et la même API compatible OpenAI. Le choix peut être modifié dans les paramètres à tout moment.

Il nécessite [LM Studio](https://lmstudio.ai) et un GPU suffisamment puissant.

---

### Matériel minimum

Pour faire tourner Elite Dangerous et le LLM sur la **même machine**, il faut au minimum un **NVIDIA RTX 3060 avec 24 Go de VRAM**.

> **Conseil :** Elite Intel peut être dirigé vers une instance LM Studio fonctionnant sur un **PC séparé** de votre réseau. Si une deuxième machine dotée d'un GPU capable est disponible, le PC de jeu ne supporte aucune charge d'inférence dans cette configuration.

---

### Modèle recommandé

| Modèle | VRAM requise | Notes |
|---|---|---|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 Go | ✅ Recommandé. Rapide, précis, fonctionne très bien pour les commandes et les requêtes. |
| `tulu-3.1-8b-supernova` Q8_0 | ~8,5 Go | Qualité supérieure, si la marge de VRAM le permet. |
| `qwen3` 8B | ~8 Go | Expérimental. Attendez-vous à des commandes manquées et des hallucinations occasionnelles. |

---

Tutoriel vidéo très détaillé par @DawnTreaderToolsoftheElite 

[[youtube:F5RgRRePrTo]]

---

### Étape 1 – Installer LM Studio

Ouvrez **PowerShell** et exécutez :

```powershell
irm https://lmstudio.ai/install.ps1 | iex
```

Cela installe le CLI `lms` et l'environnement d'exécution LM Studio. Ouvrez une **nouvelle** fenêtre PowerShell après l'installation pour que les modifications prennent effet.

Vérifiez que ça fonctionne :

```powershell
lms --help
```

> **Note :** Si l'application de bureau LM Studio est déjà installée, le CLI `lms` peut déjà être disponible. Exécutez `lms --help` avant de lancer le script d'installation.

---

### Étape 2 – Télécharger le modèle

```powershell
lms get matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF
```

ou

```powershell
lms get Tulu-3.1
```
et choisissez la variante `matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF` (peut être listée comme `Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`).

Pour lister les modèles téléchargés :

```powershell
lms ls
```

---

### Étape 3 – Démarrer le serveur

Chargez le modèle et démarrez le serveur d'inférence :

```powershell
lms load tulu-3.1-8b-supernova --context-length 8192 --gpu max
lms server start
```

**NOTE** : L'option `--context-length 8192` est obligatoire. Sans elle, la fenêtre de contexte peut être trop petite, provoquant une troncature des invites, des échecs et des hallucinations.

Vérifiez qu'il fonctionne en ouvrant un navigateur ou une autre fenêtre PowerShell et en accédant à :

```
http://localhost:1234/v1/models
```

Vous devriez recevoir une liste JSON des modèles chargés. La chaîne d'identifiant du modèle dans cette réponse est celle que vous devrez saisir dans le champ **LLM Model** d'Elite Intel.

Pour arrêter le serveur :

```powershell
lms server stop
```

> ⚠️ **Important :** Le serveur LM Studio ne **survit pas** aux redémarrages. Exécutez `lms server start` à nouveau après chaque redémarrage, ou utilisez l'une des options de démarrage automatique ci-dessous.

---

### Étape 4 – (Optionnel) Démarrage automatique au démarrage

Deux options sont disponibles pour maintenir le serveur en fonctionnement après les redémarrages.

#### Option A – Application de bureau

Si l'application de bureau LM Studio est installée, c'est l'approche la plus simple :

1. Ouvrez LM Studio et appuyez sur **Ctrl + ,** pour ouvrir les Paramètres.
2. Cochez **« Run LLM server on login »**.
3. Fermer l'application la minimise dans la barre système et maintient le serveur en fonctionnement. Elle se restaure automatiquement à la prochaine connexion.

#### Option B – Planificateur de tâches (sans interface graphique)

1. Appuyez sur **Win + R**, tapez `taskschd.msc`, appuyez sur Entrée.
2. Cliquez sur **Créer une tâche** dans le panneau de droite.
3. **Onglet Général** : Nommez-la `LM Studio Server`. Cochez **« Exécuter avec les privilèges les plus élevés »**.
4. **Onglet Déclencheurs** : Cliquez sur Nouveau → **« À la connexion »** → OK.
5. **Onglet Actions** : Cliquez sur Nouveau → **« Démarrer un programme »**.
   - Programme/script : `%USERPROFILE%\.lmstudio\bin\lms.exe`
   - Ajouter des arguments : `server start`

Pour charger également le modèle automatiquement, créez plutôt un fichier batch :

```batch
@echo off
%USERPROFILE%\.lmstudio\bin\lms.exe daemon up
%USERPROFILE%\.lmstudio\bin\lms.exe load tulu-3.1-8b-supernova --yes --context-length 8192 --gpu max
%USERPROFILE%\.lmstudio\bin\lms.exe server start
```

Enregistrez-le sous `start-lmstudio.bat` dans un emplacement permanent (par ex. `C:\Scripts\`) et faites pointer l'action du Planificateur de tâches vers ce fichier.

---

### Étape 5 – Configurer Elite Intel

Ouvrez l'**onglet Paramètres** dans Elite Intel :

- Laissez le champ **LLM Key** vide (LM Studio local n'en requiert pas).
- **LLM Address** : définissez sur `http://localhost:1234/v1/chat/completions`. Si LM Studio est sur une autre machine, remplacez `localhost` par l'adresse IP de cette machine.
- **LLM Model** : collez la chaîne d'identifiant du modèle obtenue depuis `http://localhost:1234/v1/models`.
- **Command LLM** : définissez sur le même identifiant de modèle.
- **Query LLM** : définissez sur le même identifiant de modèle.
- Cliquez sur **Stop** puis **Start** dans l'onglet AI pour appliquer les modifications.

---

Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
