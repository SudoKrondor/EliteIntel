## LLM local - Configuration Linux (Ollama)

Exécuter un LLM local maintient toutes les données privées et hors ligne. Aucun abonnement n'est requis. Des coûts matériels et énergétiques s'appliquent.

Cela nécessite [Ollama](https://ollama.com) et un GPU performant.

---

### Matériel minimum

Pour faire fonctionner Elite Dangerous et le LLM sur la **même machine**, un minimum d'un **NVIDIA RTX 3060 avec 12 Go de VRAM** est requis. La marge de performance est limitée à cette configuration.

> **Conseil :** Elite Intel peut être pointé vers une instance Ollama fonctionnant sur un **PC séparé** de votre réseau. Si une seconde machine avec un GPU capable est disponible, le PC de jeu ne supporte aucune charge d'inférence dans cette configuration.

---

### Modèle recommandé

| Modèle | VRAM requise | Notes |
|---|---|---|
| `Tulu-3.1-8B-SuperNova-Q4_K_M`| ~5 Go | ✅ Recommandé. Fiable pour les commandes et les requêtes. |
| `qwen3` 8B | ~8 Go | Expérimental. Attendez-vous à des commandes manquées et à des hallucinations occasionnelles. |

> **Remarque :** Pour l'inférence locale la plus rapide, envisagez [LM Studio](Install-LM-Studio-Linux) avec `matrixportalx/tulu-3.1-8b-supernova`. Lors des tests, il est nettement plus rapide qu'Ollama sur le même matériel avec le même modèle.

---

### Étape 1 - Installer Ollama

```shell
curl -fsSL https://ollama.com/install.sh | sh
```

Ollama s'installe en tant que service systemd et démarre automatiquement.

---

### Étape 2 - Télécharger un modèle recommandé

```shell
ollama pull hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF
```

Ou des alternatives expérimentales :

```shell
ollama pull qwen3:8b
```

---

### Étape 3 - (Optionnel) Configurer le service Ollama

Ollama fonctionne sans configuration particulière. La configuration suivante améliore la gestion de la VRAM lors de l'exécution parallèle avec Elite Dangerous.

```shell
sudo nano /etc/systemd/system/ollama.service.d/override.conf
```

Collez ceci :

```ini
[Service]
Environment="OLLAMA_MAX_VRAM=14000000000"
Environment="OLLAMA_DEBUG=0"
Environment="OLLAMA_NUM_PARALLEL=3"
Environment="OLLAMA_MAX_LOADED_MODELS=1"
Environment="OLLAMA_FLASH_ATTENTION=1"
Environment="OLLAMA_KEEP_ALIVE=-1"
Nice=10
IOSchedulingClass=best-effort
IOSchedulingPriority=5
```

Puis rechargez et redémarrez :

```shell
sudo systemctl daemon-reload
sudo systemctl restart ollama.service
```

#### Ce que font ces paramètres

**`OLLAMA_MAX_VRAM`** : Limite stricte de la VRAM qu'Ollama peut utiliser, en octets. `14000000000` = 14 Go. Laisse le reste pour Elite Dangerous. Ajustez selon votre GPU et les exigences du jeu.

**`OLLAMA_NUM_PARALLEL`** : Nombre de requêtes qu'Ollama gère simultanément. Elite Intel effectue des appels asynchrones, donc une valeur trop basse provoque des échecs. `3` couvre le chevauchement typique de commandes et de requêtes sans surallouer.

**`OLLAMA_MAX_LOADED_MODELS`** : Ne conserve qu'un seul modèle en VRAM à la fois.

**`OLLAMA_FLASH_ATTENTION`** : Active Flash Attention, qui réduit l'utilisation de la bande passante mémoire lors de l'inférence. Généralement plus rapide, surtout pour les requêtes répétées.

**`OLLAMA_KEEP_ALIVE=-1`** : Maintient le modèle chargé en VRAM indéfiniment. Sans cela, Ollama peut décharger le modèle après une période d'inactivité, entraînant un délai de rechargement lors de la prochaine requête.

---

### Étape 4 - Configurer Elite Intel

Ouvrez l'onglet **Settings** dans Elite Intel :

- Laissez le champ **LLM Key** vide (Ollama local n'en requiert pas).
- **LLM Address** est par défaut `http://localhost:11434/api/chat`. Si Ollama se trouve sur une autre machine, remplacez `localhost` par l'adresse IP de cette machine.
- **Command LLM** : définissez sur `hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF:latest` (ou le nom affiché par `ollama ls`).
- **Query LLM** : définissez sur `hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF:latest` (ou le nom affiché par `ollama ls`).
- Cliquez sur **Stop** puis **Start** dans l'onglet AI pour appliquer les modifications.

---

Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
