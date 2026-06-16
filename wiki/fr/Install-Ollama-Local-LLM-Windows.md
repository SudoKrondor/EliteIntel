## LLM local - Configuration Windows (Ollama)

Exécuter un LLM local maintient toutes les données privées et hors ligne. Il n'y a pas de frais d'abonnement. Des coûts matériels et électriques s'appliquent.

Il nécessite [Ollama](https://ollama.com) et un GPU capable.

---

### Configuration matérielle minimale

Pour exécuter Elite Dangerous et le LLM sur la **même machine**, un minimum de **NVIDIA RTX 3060 avec 12 Go de VRAM** est requis. La marge de performance est limitée à cette spécification.

> **Conseil :** Elite Intel peut être dirigé vers une instance Ollama fonctionnant sur un **PC séparé** de votre réseau. Si une deuxième machine avec un GPU capable est disponible, le PC de jeu ne supporte aucune charge d'inférence dans cette configuration.

---

### Modèle recommandé

| Modèle | VRAM requise | Notes |
|---|---|---|
| `tulu3:8b` Q4_K_M | ~5 Go | ✅ Recommandé. Fiable pour les commandes et les requêtes. |
| `qwen3` 8B | ~8 Go | Expérimental. Attendez-vous à des commandes manquées occasionnelles et à des hallucinations. |

> **Note :** Pour l'inférence locale la plus rapide, envisagez [LM Studio](Install-LM-Studio-Windows) avec `matrixportalx/tulu-3.1-8b-supernova`. Dans nos tests, il est notablement plus rapide qu'Ollama sur le même matériel avec le même modèle.

---

### Étape 1 - Installer Ollama

- Rendez-vous sur [https://ollama.com/download](https://ollama.com/download)
- Téléchargez et exécutez `OllamaSetup.exe`. Aucun droit administrateur requis.
- Ollama s'installe et s'exécute dans la barre des tâches. Il démarre automatiquement à la connexion.

---

### Étape 2 - Télécharger un modèle

Ouvrez **l'Invite de commandes** ou **PowerShell** et exécutez :

```shell
ollama pull tulu3:8b
```

Ou des alternatives expérimentales :

```shell
ollama pull qwen3:8b
```

---

### Étape 3 - (Optionnel) Optimiser la configuration

Ollama fonctionne sans optimisation. La configuration suivante améliore la gestion de la VRAM lors de l'exécution en parallèle avec Elite Dangerous.

Sous Windows, Ollama lit la configuration depuis les **variables d'environnement utilisateur**.

1. Faites un clic droit sur l'icône Ollama dans la barre des tâches et sélectionnez **Quitter**.
2. Ouvrez les **Paramètres** et recherchez "variables d'environnement".
3. Cliquez sur **"Modifier les variables d'environnement pour votre compte"**.
4. Ajoutez chaque variable ci-dessous en utilisant **Nouveau** :

| Variable | Valeur | Notes |
|---|---|---|
| `OLLAMA_MAX_VRAM` | `14000000000` | Limite à 14 Go. Ajustez selon votre GPU et les besoins du jeu. |
| `OLLAMA_NUM_PARALLEL` | `3` | Couvre les schémas d'appels asynchrones d'Elite Intel sans sur-allocation. |
| `OLLAMA_MAX_LOADED_MODELS` | `1` | Un seul modèle en VRAM à la fois. |
| `OLLAMA_FLASH_ATTENTION` | `1` | Inférence plus rapide. |
| `OLLAMA_KEEP_ALIVE` | `-1` | Maintient le modèle chargé en permanence. |

5. Cliquez sur **OK**. Relancez Ollama depuis le menu Démarrer.

#### Ce que font ces paramètres

**`OLLAMA_MAX_VRAM`** : Limite stricte de la VRAM qu'Ollama peut utiliser, en octets. Laisse le reste pour Elite Dangerous. Ajustez selon votre GPU et les besoins du jeu.

**`OLLAMA_NUM_PARALLEL`** : Nombre de requêtes qu'Ollama traite simultanément. Elite Intel effectue des appels asynchrones, donc régler cette valeur trop bas provoque des échecs. `3` couvre le chevauchement typique commandes/requêtes sans sur-allocation.

**`OLLAMA_MAX_LOADED_MODELS`** : Ne conserve qu'un seul modèle en VRAM à la fois.

**`OLLAMA_FLASH_ATTENTION`** : Active Flash Attention, qui réduit l'utilisation de la bande passante mémoire lors de l'inférence. Généralement plus rapide, surtout pour les requêtes répétées.

**`OLLAMA_KEEP_ALIVE=-1`** : Maintient le modèle chargé en VRAM indéfiniment. Sans cela, Ollama peut décharger le modèle après une période d'inactivité, entraînant un délai de rechargement à la prochaine requête.

---

### Étape 4 - Configurer Elite Intel

Ouvrez l'**onglet Paramètres** dans Elite Intel :

- Laissez le champ **Clé LLM** vide (Ollama local n'en requiert pas).
- **Adresse LLM** par défaut : `http://localhost:11434/api/chat`. Si Ollama est sur une autre machine, remplacez `localhost` par l'adresse IP de cette machine.
- **Modèle LLM** : définissez sur `tulu3:8b`.
- **LLM de commande** : définissez sur `tulu3:8b`.
- **LLM de requête** : définissez sur `tulu3:8b`.
- Cliquez sur **Stop** puis **Start** dans l'onglet IA pour appliquer les modifications.

---

Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
