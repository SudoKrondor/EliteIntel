## LLM local – Configuration Linux (LM Studio)

Faire tourner un LLM localement garde toutes les données privées et hors ligne. Aucun abonnement n'est requis. Des coûts matériels et électriques s'appliquent.

Il nécessite [LM Studio](https://lmstudio.ai) et un GPU suffisamment puissant.

---

### À qui s'adresse ce guide

Ce guide installe LM Studio à la main sur un PC qui **ne fait tourner que le modèle** : un second PC de votre
réseau, pendant que le jeu tourne sur votre PC de jeu, qui ne supporte alors plus aucune charge d'inférence.

> **Le jeu et le modèle sur un même PC ?** Ce guide n'est alors pas nécessaire. Si votre PC de jeu a une NVIDIA
> RTX 3090 (24 Go) ou mieux, Elite Intel installe et configure LM Studio pour vous : voir
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
lms server start --bind 0.0.0.0
```

`--gpu max` délègue l'inférence au GPU pour des performances maximales. `lms daemon up` ne fait rien si le serveur d'arrière-plan tourne déjà : vous pouvez donc l'inclure à chaque fois. `--bind 0.0.0.0` permet aux autres PC de votre réseau de joindre le serveur, qui par défaut ne répond qu'à ce PC.

**NOTE** : `--context-length 8192` est obligatoire, et doit valoir exactement 8192 : avec une fenêtre plus petite le modèle hallucine, avec une plus grande chaque réponse devient trop lente.

> **Laissez entrer le PC de jeu.** Si un pare-feu tourne sur ce PC, autorisez le port TCP 1234 depuis votre PC de jeu. Avec `ufw`, par exemple : `sudo ufw allow from GAMING_PC_IP to any port 1234 proto tcp`.

Vérifiez depuis le **PC de jeu**, avec l'adresse du PC du LLM à la place de `LLM_PC_IP` :

```shell
curl http://LLM_PC_IP:1234/v1/models
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

> **Réseau :** `--bind 0.0.0.0` permet à votre PC de jeu de joindre le serveur. Autorisez le port TCP 1234 dans le pare-feu de ce PC, comme à l'étape 3.

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

**Sur le PC de jeu :** Dans Elite Intel, ouvrez **Services → Services IA**. Sous **Modèle de langage (LLM)**, sélectionnez **LMStudio** :

- **Adresse** : `http://LLM_PC_IP:1234/v1/chat/completions`, en remplaçant `LLM_PC_IP` par l'adresse du PC du LLM sur votre réseau (`ip addr` sur le PC du LLM l'affiche).
- **Modèle** : collez la chaîne d'identifiant du modèle obtenue depuis `http://LLM_PC_IP:1234/v1/models`. Un seul modèle sert aux commandes comme aux requêtes.

Un LLM local ne nécessite pas de clé d'API.

Cliquez sur **Enregistrer**. Elite Intel redémarre alors le modèle de langage de lui-même.

---

Communauté 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
