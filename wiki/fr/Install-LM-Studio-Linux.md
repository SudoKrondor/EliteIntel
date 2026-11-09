## LLM local – Configuration Linux (LM Studio)

Faire tourner un LLM localement garde toutes les données privées et hors ligne. Aucun abonnement n'est requis. Des coûts matériels et électriques s'appliquent.

Il nécessite [LM Studio](https://lmstudio.ai) et un GPU suffisamment puissant.

---

### Matériel minimum

Pour faire tourner Elite Dangerous et le LLM sur la **même machine**, il faut au minimum un **NVIDIA RTX 3060 avec 12 Go de VRAM**. Les marges de performance sont limitées à cette configuration.

> **Conseil :** Elite Intel peut être dirigé vers une instance LM Studio fonctionnant sur un **PC séparé** de votre réseau. Si une deuxième machine dotée d'un GPU capable est disponible, le PC de jeu ne supporte aucune charge d'inférence dans cette configuration.

---

### Modèle recommandé

| Modèle | VRAM requise | Notes |
|---|---|---|
| `google/gemma-4-e4b` | ~6,3 Go | ✅ Requis |

> **Pourquoi ce modèle ?** Il prend en charge le function calling requis par le compagnon. Un modèle incapable
> d'émettre un appel d'outil ne peut pas piloter l'application, aussi bien écrive-t-il.
> Voir [Choisir votre LLM](installing-local-llms).

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

Démarrez le serveur d'arrière-plan de LM Studio, puis téléchargez le modèle :

```shell
lms daemon up
lms get google/gemma-4-e4b
```
Si le nom ne correspond pas exactement, `lms get` liste les candidats trouvés ; choisissez-en
un avec les flèches et Entrée.

Pour lister les modèles téléchargés :

```shell
lms ls
```

C'est la procédure standard. Cependant, [LM Studio a un bogue connu](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). Dans certains cas, le téléchargement échoue avec :
```Error: No staff picks found with the specified search criteria.```

Si cela se produit, téléchargez le GGUF à la main depuis la page HuggingFace du modèle, puis importez-le :

```shell
lms import /path/to/the-model.gguf
```

---

### Étape 3 – Démarrer le serveur

Chargez le modèle et démarrez le serveur d'inférence :

```shell
lms daemon up
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start
```

`--gpu max` délègue l'inférence au GPU pour des performances maximales. `lms daemon up` ne fait rien si le serveur d'arrière-plan tourne déjà : vous pouvez donc l'inclure à chaque fois.

Vérifiez qu'il fonctionne :

```shell
curl http://localhost:1234/v1/models
```

Vous devriez recevoir une liste JSON des modèles chargés. La chaîne d'identifiant du modèle dans cette réponse est celle que vous devrez saisir dans le champ **Modèle** d'Elite Intel.

Pour arrêter le serveur :

```shell
lms server stop
```

> ⚠️ **Important :** Le serveur LM Studio ne **survit pas** aux redémarrages. Exécutez à nouveau les trois commandes de l'étape 3 après chaque redémarrage, ou configurez le démarrage automatique optionnel ci-dessous.

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
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms load google/gemma-4-e4b --yes --context-length 8192 --gpu max
ExecStart=/home/YOUR_USERNAME/.lmstudio/bin/lms server start --bind 127.0.0.1 --port 1234
ExecStop=/home/YOUR_USERNAME/.lmstudio/bin/lms server stop
ExecStopPost=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon down

[Install]
WantedBy=default.target
```

Remplacez `YOUR_USERNAME` par votre nom d'utilisateur Linux et `YOUR_UID` par votre identifiant utilisateur. Pour trouver votre UID :

```shell
id -u
```

> **Le jeu sur un autre PC ?** Avec `--bind 127.0.0.1`, le serveur n'est joignable que depuis cette machine. Si Elite Intel tourne sur un autre PC, remplacez-le par `--bind 0.0.0.0` pour que les autres machines de votre réseau puissent le joindre.

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

Certains utilisateurs constatent des réponses d'inférence lentes lorsque LM Studio démarre au boot. Le problème se résout immédiatement après un redémarrage manuel du service. La cause n'est pas confirmée.

Vérifiez d'abord que la ligne `lms load` de votre fichier de service se termine par `--gpu max`. Les anciennes versions de cette page l'omettaient. Sans cette option, LM Studio décide lui-même quelle part du modèle va sur le GPU, et au démarrage il peut en placer une partie sur le CPU. Si vous l'ajoutez, exécutez `systemctl --user daemon-reload` et redémarrez pour tester.

Si des réponses lentes apparaissent encore après un redémarrage et se résolvent après un redémarrage manuel, cette minuterie automatise le correctif.

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
Description=Restart LM Studio 2 minutes after boot

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

La minuterie attend 2 minutes après le démarrage, redémarre le service LM Studio une fois, puis reste inactive. Si vous ne rencontrez pas d'inférence lente, cette étape n'est pas nécessaire.

---

### Étape 5 – Configurer Elite Intel

Dans Elite Intel, ouvrez **Paramètres → Services IA**. Sous **Modèle de langage (LLM)**, sélectionnez **LMStudio** :

- **Adresse** : `http://localhost:1234/v1/chat/completions` (la valeur par défaut). Si LM Studio est sur une autre machine, remplacez `localhost` par l'adresse IP de cette machine.
- **Modèle** : collez la chaîne d'identifiant du modèle obtenue via `curl http://localhost:1234/v1/models`. Un seul modèle sert aux commandes comme aux requêtes.

Un LLM local ne nécessite pas de clé d'API.

Cliquez sur **Enregistrer**. Elite Intel redémarre alors le modèle de langage de lui-même.

---

Communauté 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
