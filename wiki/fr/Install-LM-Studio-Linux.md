## LLM local – Configuration Linux (LM Studio)

Faire tourner un LLM localement garde toutes les données privées et hors ligne. Aucun abonnement n'est requis. Des coûts matériels et électriques s'appliquent.

LM Studio est une alternative à Ollama. Il utilise les mêmes modèles et la même API compatible OpenAI. Le choix peut être modifié dans les paramètres à tout moment.

Il nécessite [LM Studio](https://lmstudio.ai) et un GPU suffisamment puissant.

---

### Matériel minimum

Pour faire tourner Elite Dangerous et le LLM sur la **même machine**, il faut au minimum un **NVIDIA RTX 3060 avec 12 Go de VRAM**. Les marges de performance sont limitées à cette configuration.

> **Conseil :** Elite Intel peut être dirigé vers une instance LM Studio fonctionnant sur un **PC séparé** de votre réseau. Si une deuxième machine dotée d'un GPU capable est disponible, le PC de jeu ne supporte aucune charge d'inférence dans cette configuration.

---

### Modèle recommandé

| Modèle | VRAM requise | Notes |
|---|---|---|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 Go | ✅ Recommandé. Rapide, précis, fonctionne très bien pour les commandes et les requêtes. |
| `tulu-3.1-8b-supernova` Q8_0 | ~8,5 Go | Qualité supérieure, si la marge de VRAM le permet. |
| `qwen3` 8B | ~8 Go | Expérimental. Attendez-vous à des commandes manquées et des hallucinations occasionnelles. |

---

[[youtube:2HGFmlZGK1g]]

---

### Étape 1 – Installer LM Studio

```shell
curl -fsSL https://lmstudio.ai/install.sh | bash
```

Le programme d'installation place tout dans `~/.lmstudio/` et ajoute l'outil CLI `lms`. Une fois terminé, ajoutez le CLI à votre PATH :

```shell
# Ajoutez ceci à votre ~/.bashrc
export PATH="$HOME/.lmstudio/bin:$PATH"
```

Puis rechargez votre shell :

```shell
source ~/.bashrc
```

Vérifiez que ça fonctionne :

```shell
lms --help
```

---

### Étape 2 – Télécharger le modèle

```shell
lms get tulu3.1
Searching for models with the term tulu3.1
No exact match found. Please choose a model from the list below.

? Select a model to download
❯ QuantFactory/Tulu-3.1-8B-SuperNova-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF
  QuantFactory/Tulu-3.1-8B-SuperNova-Smart-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-GGUF
  bunnycore/Tulu-3.1-8B-SuperNova-Smart-IQ4_XS-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-Smart-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-Smart-i1-GGUF
  matrixportalx/Tulu-3.1-8B-SuperNova-Q4_0-GGUF
  matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF

↑↓ navigate • ⏎ select
```
Utilisez les touches fléchées pour naviguer et Entrée pour sélectionner. Choisissez `matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`.

Pour lister les modèles téléchargés :

```shell
lms ls
```

C'est la procédure standard. Cependant, [LM Studio a un bogue connu](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). Dans certains cas, le téléchargement échoue avec :
```Error: No staff picks found with the specified search criteria.```

Si cela se produit, téléchargez le modèle manuellement :

```shell
curl -s "https://huggingface.co/api/models/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF" | grep -o '"rfilename":"[^"]*\.gguf"'
```
Puis importez-le :

```shell
lms import /path/to/tulu-3.1-8b-supernova-q4_k_m.gguf
```


---

### Étape 3 – Démarrer le serveur

Chargez le modèle et démarrez le serveur d'inférence :

```shell
lms load tulu-3.1-8b-supernova --context-length 8192 --gpu max
lms server start
```

`--gpu max` délègue l'inférence au GPU pour des performances maximales.

Vérifiez qu'il fonctionne :

```shell
curl http://localhost:1234/v1/models
```

Vous devriez recevoir une liste JSON des modèles chargés. La chaîne d'identifiant du modèle dans cette réponse est celle que vous devrez saisir dans le champ **LLM Model** d'Elite Intel.

Pour arrêter le serveur :

```shell
lms server stop
```

> ⚠️ **Important :** Le serveur LM Studio ne **survit pas** aux redémarrages. Exécutez `lms server start` à nouveau après chaque redémarrage, ou configurez le démarrage automatique optionnel ci-dessous.

---

### Étape 4 – (Optionnel) Démarrage automatique au démarrage

Pour démarrer LM Studio automatiquement, configurez-le comme service systemd **utilisateur**. Cela s'exécute sous votre propre session plutôt qu'en tant que service système. Il démarre après que l'environnement de bureau est opérationnel. Aucun accès root n'est requis.

Trouvez votre identifiant utilisateur (remplacez le nom d'utilisateur par votre nom réel) :
```shell
id -u YOUR_USER_NAME
```

Retenez ce numéro. Vous en aurez besoin pour la configuration ultérieure.

Créez le répertoire systemd utilisateur s'il n'existe pas :

```shell
mkdir -p ~/.config/systemd/user
```

Créez le fichier de service :

```shell
nano ~/.config/systemd/user/lmstudio.service
```

Collez-y ceci :

```ini
[Unit]
Description=LM Studio Server
After=network.target

[Service]
Type=oneshot
RemainAfterExit=yes
Environment="HOME=/home/YOUR_USERNAME"
Environment="PATH=/home/YOUR_USERNAME/.lmstudio/bin:/usr/local/bin:/usr/bin:/bin"
Environment="XDG_RUNTIME_DIR=/run/user/YOUR_UID"
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon up
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms load matrixportalx/tulu-3.1-8b-supernova --yes --context-length 8192
ExecStart=/home/YOUR_USERNAME/.lmstudio/bin/lms server start --bind 0.0.0.0 --port 1234
ExecStop=/home/YOUR_USERNAME/.lmstudio/bin/lms server stop
ExecStopPost=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon down

[Install]
WantedBy=default.target
```

Remplacez `YOUR_USERNAME` par votre nom d'utilisateur Linux et `YOUR_UID` par votre identifiant utilisateur. Pour trouver votre UID :

```shell
id -u
```

> ⚠️ **Pourquoi `XDG_RUNTIME_DIR` ?** Les services utilisateur s'exécutent dans un environnement épuré qui peut ne pas inclure les variables de session. LM Studio utilise `XDG_RUNTIME_DIR` pour l'IPC. Sans lui, le service peut échouer silencieusement même quand `lms` fonctionne correctement depuis le terminal. C'est la cause la plus fréquente d'échec du service lorsque l'exécution manuelle réussit.

Activez et démarrez-le :

```shell
systemctl --user daemon-reload
systemctl --user enable lmstudio.service
systemctl --user start lmstudio.service
```

Vérifiez qu'il fonctionne :

```shell
systemctl --user status lmstudio.service
curl http://localhost:1234/v1/models
```

> **Dépannage :** Si le service échoue, consultez le journal :
> ```shell
> journalctl --user -xeu lmstudio.service --no-pager | tail -40
> ```
> S'il indique « Failed to load model », exécutez `lms ls` et vérifiez que le nom du modèle correspond exactement à ce qui figure dans le fichier de service.

---

### Étape 4b – (Optionnel) Corriger l'inférence lente après le démarrage

Certains utilisateurs constatent des réponses d'inférence lentes lorsque LM Studio démarre au boot. Le problème se résout immédiatement après un redémarrage manuel du service. Cela est dû à une particularité de l'initialisation du démon LM Studio. Le premier démarrage à froid peut laisser le moteur d'inférence dans un état dégradé.

Si des réponses lentes apparaissent après un redémarrage et se résolvent après un redémarrage manuel, cette minuterie automatise le correctif.

Créez un service complémentaire :

```shell
nano ~/.config/systemd/user/lmstudio-restart.service
```

```ini
[Unit]
Description=LM Studio post-boot restart
After=lmstudio.service

[Service]
Type=oneshot
ExecStart=systemctl --user restart lmstudio.service
```

Créez la minuterie :

```shell
nano ~/.config/systemd/user/lmstudio-restart.timer
```

```ini
[Unit]
Description=Restart LM Studio 2 minutes after login

[Timer]
OnBootSec=2min
Unit=lmstudio-restart.service

[Install]
WantedBy=timers.target
```

Activez la minuterie :

```shell
systemctl --user daemon-reload
systemctl --user enable --now lmstudio-restart.timer
```

La minuterie attend 2 minutes après la connexion, redémarre le service LM Studio une fois, puis reste inactive. Si vous ne rencontrez pas d'inférence lente, cette étape n'est pas nécessaire.

---

### Désactiver le démarrage automatique d'Ollama (si installé)

Ollama s'installe par défaut en tant que service systemd activé. Pour utiliser LM Studio à la place et ne démarrer Ollama que sur demande :

```shell
sudo systemctl disable ollama.service
sudo systemctl stop ollama.service
```

---

### Étape 5 – Configurer Elite Intel

Ouvrez l'**onglet Paramètres** dans Elite Intel :

- Laissez le champ **LLM Key** vide (LM Studio local n'en requiert pas).
- **LLM Address** : définissez sur `http://localhost:1234/v1/chat/completions`. Si LM Studio est sur une autre machine, remplacez `localhost` par l'adresse IP de cette machine.
- **LLM Model** : collez la chaîne d'identifiant du modèle obtenue via `curl http://localhost:1234/v1/models`.
- **Command LLM** : définissez sur le même identifiant de modèle.
- **Query LLM** : définissez sur le même identifiant de modèle.
- Cliquez sur **Stop** puis **Start** dans l'onglet AI pour appliquer les modifications.

---

Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
