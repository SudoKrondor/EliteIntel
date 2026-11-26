# LM Studio sur un serveur Ubuntu headless

> Guide fourni par **Wim**

> **Elite Intel :** ce guide met en place une machine dédiée sur le réseau local qui fait tourner LM Studio pour Elite Intel. Une fois terminé, renseignez l'**Adresse** dans l'[onglet Services](UI-Settings-Tab) avec `http://SERVER_IP:1234/v1/chat/completions` et chargez le modèle pris en charge, **`google/gemma-4-e4b`**. Le serveur doit écouter au-delà de localhost (`--bind 0.0.0.0`, voir Partie 2 § 9) et le pare-feu doit autoriser le PC de jeu.

---

# Partie 1 : Ubuntu Server comme hôte d'IA locale / LM Studio

**Guide d'installation de base propre**  
**Base recommandée :** Ubuntu Server 24.04 LTS (amd64)  
**Date du document :** 12 septembre 2026

## 1. Objectif

Ce guide prépare une machine Ubuntu Server propre pour une charge d'IA locale headless telle que LM Studio / llmster. Il s'arrête volontairement avant l'installation de LM Studio lui-même. Le but est un serveur réduit et compréhensible, avec réseau, SSH, mises à jour et pare-feu fonctionnels et, le cas échéant, un pilote graphique NVIDIA validé.

Ce n'est pas un guide de récupération ni de dépannage matériel. Il suppose que la machine est stable avec les réglages CPU/RAM d'origine et que les données importantes ont déjà été sauvegardées.

## 2. Quelle version d'Ubuntu ?

Pour un serveur d'IA conservateur et durable, ce guide utilise **Ubuntu Server 24.04 LTS (Noble Numbat)**. Canonical assure la maintenance de sécurité standard d'Ubuntu 24.04 LTS jusqu'en mai 2029. Ubuntu 26.04 LTS est également disponible et constitue la LTS la plus récente, mais la 24.04 reste entièrement prise en charge et offre une base mature pour les systèmes où la stabilité compte davantage que le noyau et l'espace utilisateur les plus récents.

Pour un tout nouveau déploiement où du matériel récent exige la pile noyau/pilotes la plus récente, Ubuntu 26.04 LTS est aussi un choix raisonnable. Les commandes de ce guide sont volontairement classiques et devraient rester très similaires.

Références officielles :
- https://ubuntu.com/about/release-cycle
- https://documentation.ubuntu.com/release-notes/24.04/
- https://ubuntu.com/server/docs/tutorial/basic-installation/

## 3. Avant l'installation

Préparez :

- Une machine Intel/AMD 64 bits (amd64) pour ce guide.
- Une clé USB contenant l'ISO d'Ubuntu Server.
- Une connexion Ethernet filaire si possible.
- Le nom d'hôte, le nom d'utilisateur et un mot de passe robuste prévus.
- La décision du disque qu'Ubuntu a le droit d'effacer.
- Pour l'inférence GPU, un GPU NVIDIA pris en charge, correctement installé et alimenté.

Si la machine fonctionnait auparavant avec un CPU ou une RAM overclockés, revenez à des réglages d'origine stables avant de la mettre en service comme serveur. Les environnements d'exécution d'IA et la compilation de modules noyau peuvent révéler des instabilités que des charges plus légères ne montrent pas.

## 4. Installer Ubuntu Server

Démarrez l'installateur d'Ubuntu Server et suivez le déroulement normal. Pour un serveur dédié simple :

1. Choisissez la langue et la disposition du clavier.
2. Laissez l'installateur configurer le réseau filaire en DHCP dans un premier temps.
3. Utilisez le disque système prévu. **Le partitionnement peut détruire des données existantes**, vérifiez donc soigneusement le disque sélectionné.
4. Créez l'utilisateur administrateur et le nom d'hôte.
5. Sélectionnez **Install OpenSSH server** lorsque c'est proposé. S'il n'a pas été sélectionné, il peut être installé après le premier démarrage.
6. Ignorez les snaps serveur optionnels sauf besoin précis.
7. Terminez l'installation et redémarrez.

## 5. Première connexion et mise à jour du système

Connectez-vous localement ou par SSH, puis mettez à jour l'index des paquets et les paquets installés :

```bash
sudo apt update
sudo apt upgrade -y
```

Si Ubuntu indique qu'un redémarrage est nécessaire :

```bash
sudo reboot
```

Après reconnexion, vérifiez la version et le noyau :

```bash
lsb_release -a
uname -r
```

## 6. Installer une petite boîte à outils d'administration

Les paquets suivants sont utiles sur un serveur d'IA headless :

```bash
sudo apt install -y \
  openssh-server \
  curl \
  ca-certificates \
  jq \
  htop \
  nvtop \
  ufw
```

`curl` est utilisé par l'installateur de LM Studio et pour les tests d'API, `jq` rend la sortie JSON lisible, `htop` surveille le CPU/la RAM, `nvtop` surveille les GPU et `ufw` fournit un pare-feu d'hôte simple.

Vérifiez que SSH est activé :

```bash
systemctl status ssh --no-pager
```

## 7. Accès SSH

Trouvez l'adresse actuelle du serveur :

```bash
ip -br address
```

Depuis une autre machine du réseau local :

```bash
ssh YOUR_USER@SERVER_IP
```

Pour un usage régulier, l'authentification SSH par clé publique est préférable à la saisie répétée d'un mot de passe. Sur la machine cliente :

```bash
ssh-keygen -t ed25519
ssh-copy-id YOUR_USER@SERVER_IP
```

Testez une nouvelle session SSH avant de modifier les réglages d'authentification par mot de passe. Ne vous enfermez pas dehors d'un serveur distant.

Référence OpenSSH officielle :
https://ubuntu.com/server/docs/how-to/security/openssh-server/

## 8. Pare-feu de base

Avant d'activer UFW, autorisez SSH :

```bash
sudo ufw allow OpenSSH
sudo ufw enable
sudo ufw status verbose
```

Pour un serveur d'IA dédié à domicile/sur le réseau local, mieux vaut n'exposer les ports applicatifs qu'aux clients de confiance ou au réseau local de confiance plutôt qu'indistinctement à toutes les interfaces réseau.

Le port API par défaut de LM Studio est 1234. **Ne l'ouvrez pas encore.** Ajoutez la règle appropriée une fois LM Studio installé et lorsque vous savez quels clients doivent y accéder.

Référence pare-feu officielle :
https://ubuntu.com/server/docs/how-to/security/firewalls/

## 9. Pilote graphique NVIDIA

Ignorez cette section si le serveur utilisera l'inférence CPU ou un accélérateur non NVIDIA.

Mettez d'abord le système à jour et redémarrez si nécessaire. Laissez ensuite Ubuntu identifier les pilotes de calcul NVIDIA adaptés :

```bash
sudo ubuntu-drivers list --gpgpu
```

Pour une charge de calcul/serveur, Ubuntu documente l'installation automatique avec :

```bash
sudo ubuntu-drivers install --gpgpu
```

Redémarrez après l'installation :

```bash
sudo reboot
```

Puis validez le GPU :

```bash
nvidia-smi
```

Un `nvidia-smi` réussi doit afficher le GPU, la version du pilote, la température, la VRAM et la table des processus.

### Remarque sur Secure Boot

Secure Boot exige des modules noyau signés compatibles. L'outil `ubuntu-drivers` d'Ubuntu est la méthode d'installation privilégiée, car il est conçu pour sélectionner correctement les pilotes empaquetés par Ubuntu. Si le module NVIDIA ne se charge pas, vérifiez l'état de Secure Boot avant de réinstaller les pilotes à répétition :

```bash
mokutil --sb-state
```

Ne désactivez pas Secure Boot par simple réflexe en première étape. Si votre combinaison micrologiciel/pilote pose des problèmes de signature ou d'enregistrement MOK, traitez-les délibérément et documentez la modification du BIOS.

Référence officielle NVIDIA sous Ubuntu :
https://ubuntu.com/server/docs/how-to/graphics/install-nvidia-drivers/

## 10. Vérification finale

Exécutez :

```bash
hostnamectl
ip -br address
systemctl is-active ssh
sudo ufw status
free -h
df -h /
nvidia-smi    # NVIDIA systems only
```

À ce stade, la machine doit disposer de :

- une installation Ubuntu LTS prise en charge ;
- des mises à jour de paquets à jour ;
- une connectivité réseau local fonctionnelle ;
- un accès SSH fonctionnel ;
- un pare-feu de base ;
- des outils d'administration utiles ;
- un pilote NVIDIA validé, le cas échéant.

Le serveur est maintenant prêt pour l'installation de LM Studio / llmster.

## 11. Ce qui n'est volontairement pas couvert

Cette base ne configure pas de stockage NAS/Samba, Docker, bases de données, serveurs web, RAID, partitionnements personnalisés, VPN, Pi-hole/DNS, environnements de bureau, CUDA Toolkit ni logiciels propres à une application. Ceux-ci relèvent de guides de déploiement séparés.

Un CUDA Toolkit autonome **n'est pas nécessaire simplement pour faire tourner LM Studio avec un GPU NVIDIA pris en charge** ; utilisez la pile d'exécution/pilotes prise en charge par LM Studio, sauf si une autre application exige spécifiquement le kit de développement CUDA.

## 12. Maintenance de base

Appliquez régulièrement les mises à jour normales :

```bash
sudo apt update
sudo apt upgrade
```

Vérifiez si un redémarrage est en attente :

```bash
if [ -f /run/reboot-required ]; then
  cat /run/reboot-required
fi
```

Avant une mise à niveau majeure d'Ubuntu, lisez les notes de version correspondantes et sauvegardez le serveur. Les mises à niveau de version majeure sont différentes des mises à jour ordinaires de paquets.

---

## Sources officielles

- Documentation Ubuntu Server : https://ubuntu.com/server/docs/
- Cycle de publication d'Ubuntu : https://ubuntu.com/about/release-cycle
- Notes de version d'Ubuntu 24.04 LTS : https://documentation.ubuntu.com/release-notes/24.04/
- Installation de base : https://ubuntu.com/server/docs/tutorial/basic-installation/
- Gestion des paquets : https://ubuntu.com/server/docs/how-to/software/package-management/
- OpenSSH : https://ubuntu.com/server/docs/how-to/security/openssh-server/
- UFW/pare-feu : https://ubuntu.com/server/docs/how-to/security/firewalls/
- Pilotes NVIDIA : https://ubuntu.com/server/docs/how-to/graphics/install-nvidia-drivers/

---

# Partie 2 : serveur LM Studio headless et gestion des modèles

**Installation, choix du modèle, quantification, service API et changement de modèle**  
**Cible :** serveur Linux/Ubuntu headless  
**Date du document :** 12 septembre 2026

## 1. Objectif

Ce guide part d'un serveur Ubuntu propre disposant déjà d'un réseau et d'un SSH fonctionnels et, pour l'accélération NVIDIA, d'un pilote GPU validé. Il installe l'environnement headless de LM Studio, explique comment choisir et télécharger des modèles, expose l'API locale si souhaité et fournit un sélecteur de modèle interactif en Bash.

Le composant serveur natif de LM Studio s'appelle **llmster**. Le programme en ligne de commande qui le pilote s'appelle **lms**. Aucun bureau graphique n'est nécessaire.

Documentation headless officielle :
https://lmstudio.ai/docs/developer/core/headless

## 2. Trois noms à comprendre

- **LM Studio** est le produit et l'écosystème dans son ensemble.
- **llmster** est le démon headless/serveur. Il fait tourner le service d'inférence sans interface graphique.
- **lms** est la CLI qui sert à télécharger, charger, décharger, inspecter et servir les modèles.

Sur un serveur d'inférence Linux dédié, llmster + lms est normalement l'agencement le plus propre.

## 3. Installer llmster / lms

Installez en tant qu'utilisateur normal qui fera tourner le service, pas en root :

```bash
curl -fsSL https://lmstudio.ai/install.sh | bash
```

Démarrez le démon :

```bash
lms daemon up
```

Vérifiez la CLI :

```bash
lms --version
lms --help
```

Si le shell ne trouve pas `lms`, ouvrez un nouveau shell de connexion ou ajoutez le répertoire binaire de LM Studio au PATH du shell courant :

```bash
export PATH="$HOME/.lmstudio/bin:$PATH"
```

Pour le rendre explicite dans Bash :

```bash
echo 'export PATH="$HOME/.lmstudio/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

## 4. Trouver et télécharger des modèles

Lancer `lms get` sans modèle affiche des recommandations :

```bash
lms get
```

Téléchargez un modèle par son nom de catalogue :

```bash
lms get publisher/model-name
```

Le modèle exact est délibérément laissé au choix du lecteur plutôt que figé dans ce guide.

Listez les modèles téléchargés :

```bash
lms ls
```

Affichez les variantes/quantifications téléchargées lorsqu'elles existent :

```bash
lms ls --variants
```

La sortie JSON est utile pour les scripts :

```bash
lms ls --json | jq
```

LM Studio peut télécharger les modèles pris en charge depuis son catalogue/l'écosystème Hugging Face. La CLI actuelle permet aussi de préciser une quantification lors du téléchargement, par exemple :

```bash
lms get publisher/model-name@q4_k_m
```

Utilisez les noms de quantification réellement proposés pour ce modèle ; tous les modèles ne sont pas publiés dans tous les formats.

Documentation officielle du téléchargement de modèles :
https://lmstudio.ai/docs/cli/local-models/get

## 5. Taille du modèle et quantification – explication pour débutants

Un modèle contient des milliards de valeurs numériques apprises appelées **poids**. Une précision numérique plus élevée conserve ces valeurs plus fidèlement mais consomme plus d'espace disque et de RAM/VRAM. La **quantification** compresse le modèle en représentant ces valeurs avec moins de bits.

Une image utile est celle d'un enregistrement audio de haute qualité : Q8 préserve plus de détails, Q6 compresse un peu plus et Q4 encore davantage. Ce sont toujours des versions du même modèle entraîné ; Q8 n'ajoute pas comme par magie un nouvel entraînement ni de nouvelles connaissances.

Interprétation typique :

| Quantification | Mémoire | Fidélité | Rôle pratique |
|---|---|---|---|
| Classe Q4 | Plus faible | Bonne | Excellent point de départ ; souvent suffisant pour le chat/l'usage d'outils |
| Classe Q5/Q6 | Moyenne | Plus élevée | Bon équilibre entre qualité et mémoire |
| Classe Q8 | Plus élevée | Très élevée | À utiliser quand la fidélité supplémentaire aide de façon mesurable |

LM Studio recommande lui-même de choisir une option 4 bits ou plus lorsque la machine peut la supporter.

### La règle importante

**Ne choisissez pas Q8 simplement parce que 8 est plus grand que 4.** Choisissez la plus petite quantification qui accomplit votre charge réelle de façon fiable. Une précision supplémentaire n'a de valeur que si elle améliore les réponses, le raisonnement, l'usage d'outils ou la stabilité suffisamment pour justifier son empreinte mémoire durablement plus grande.

## 6. La taille du modèle n'est pas son intelligence

Quantification et taille du modèle sont deux décisions différentes.

Un modèle plus grand et plus capable en quantification modérée peut surpasser un modèle plus petit en Q8 sur un raisonnement difficile. À l'inverse, un petit modèle Q4 peut être idéal pour interpréter rapidement des commandes.

Pour choisir un modèle, tenez compte de :

1. **La tâche :** chat, programmation, appel d'outils/de fonctions, vision, analyse de données, embeddings, etc.
2. **L'architecture/le nombre de paramètres :** les modèles plus grands sont souvent plus capables mais demandent plus de mémoire et peuvent être plus lents.
3. **La quantification :** Q4/Q6/Q8 modifie le compromis fidélité/mémoire de ce modèle.
4. **La longueur de contexte :** un contexte plus grand peut consommer davantage de mémoire.
5. **L'ajustement au GPU :** garder le modèle et le contexte de travail en VRAM donne généralement la meilleure inférence GPU.
6. **La prise en charge des outils :** si une application exige l'appel natif de fonctions/d'outils, choisissez un modèle entraîné et pris en charge pour cela.

Le bon choix dépend de la charge de travail. Testez de vrais prompts plutôt que de vous fier uniquement aux scores de benchmarks.

## 7. Estimer la mémoire avant le chargement

LM Studio peut estimer la consommation de ressources sans charger le modèle :

```bash
lms load publisher/model-name \
  --context-length 8192 \
  --gpu max \
  --estimate-only
```

Il s'agit d'une estimation, pas d'une garantie. Validez le modèle réellement chargé avec `lms ps`, `nvidia-smi` et une vraie inférence.

## 8. Charger et décharger des modèles

Chargez un modèle téléchargé :

```bash
lms load publisher/model-name --context-length 8192 --gpu max
```

`--gpu max` demande à LM Studio de déporter autant que possible sur le GPU. Sans cette option, LM Studio peut choisir automatiquement.

Voyez ce qui est chargé :

```bash
lms ps
```

Déchargez un modèle de façon interactive ou par sa clé :

```bash
lms unload publisher/model-name
```

Déchargez tout :

```bash
lms unload --all
```

Référence officielle :
https://lmstudio.ai/docs/cli/local-models/load

## 9. Démarrer l'API HTTP

Pour un accès depuis le serveur lui-même uniquement :

```bash
lms server start --bind 127.0.0.1 --port 1234
```

Vérifiez :

```bash
curl -s http://127.0.0.1:1234/v1/models | jq
```

Pour écouter sur toutes les interfaces IPv4 :

```bash
lms server start --bind 0.0.0.0 --port 1234
```

**Sécurité :** lier à `0.0.0.0` expose le service à tous les réseaux joignables. La documentation de LM Studio recommande une authentification lorsque le service est exposé au-delà de localhost. Au minimum, restreignez le port 1234 avec UFW aux clients de confiance du réseau local ou à un sous-réseau de confiance. N'exposez jamais une API d'inférence non authentifiée directement sur Internet.

Exemple pour un client de confiance :

```bash
sudo ufw allow from CLIENT_IP to any port 1234 proto tcp
```

Exemple pour un sous-réseau privé de confiance (remplacez-le par le vôtre) :

```bash
sudo ufw allow from 192.168.1.0/24 to any port 1234 proto tcp
```

Référence serveur officielle :
https://lmstudio.ai/docs/cli/serve/server-start

## 10. Tester une requête d'inférence

L'API compatible OpenAI peut être testée avec `curl` :

```bash
curl -s http://127.0.0.1:1234/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "publisher/model-name",
    "messages": [
      {"role": "user", "content": "Reply with exactly: Local inference works."}
    ],
    "temperature": 0
  }' | jq
```

Remplacez l'identifiant du modèle par celui affiché par `lms ps` ou `/v1/models`.

## 11. Démarrer LM Studio automatiquement au boot

LM Studio documente l'exécution de llmster comme tâche de démarrage systemd. Remplacez `YOUR_USER`, la clé du modèle, la longueur de contexte et l'adresse réseau par vos propres valeurs.

Créez le service :

```bash
sudo nano /etc/systemd/system/lmstudio.service
```

Exemple :

```ini
[Unit]
Description=LM Studio Headless Server
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
User=YOUR_USER
Environment="HOME=/home/YOUR_USER"

ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms daemon up
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms load publisher/model-name --context-length 8192 --gpu max --yes
ExecStart=/home/YOUR_USER/.lmstudio/bin/lms server start --bind 127.0.0.1 --port 1234
ExecStop=/home/YOUR_USER/.lmstudio/bin/lms daemon down

[Install]
WantedBy=multi-user.target
```

Activez-le et démarrez-le :

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now lmstudio.service
systemctl status lmstudio.service --no-pager
```

Puis vérifiez :

```bash
lms ps
curl -s http://127.0.0.1:1234/v1/models | jq
```

### Si un démarrage rapide de la CLI produit `Text file busy`

Sur certains systèmes, un second appel à `lms` juste après `lms daemon up` peut entrer en concurrence avec le démarrage du démon. Seulement si vous rencontrez réellement cette erreur, ajoutez un court délai entre les deux lignes `ExecStartPre` :

```ini
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms daemon up
ExecStartPre=/bin/sleep 5
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms load ...
```

N'ajoutez pas de contournements par anticipation si le service plus simple fonctionne.

Référence officielle de la tâche de démarrage :
https://lmstudio.ai/docs/developer/core/headless_llmster

## 12. Sélecteur de modèle interactif

Le script compagnon `lmstudio-model-switcher.sh` (listing complet en fin de page) propose un menu numéroté simple des **clés de modèles** téléchargés. Il :

1. s'assure que llmster tourne ;
2. lit les modèles téléchargés depuis `lms ls --json` ;
3. demande quel modèle utiliser ;
4. décharge le ou les modèles actuellement chargés ;
5. charge le modèle choisi avec une politique contexte/GPU configurable ;
6. laisse ou démarre le service API selon le besoin ;
7. affiche `lms ps` et vérifie `/v1/models`.

Installez-le :

```bash
mkdir -p ~/bin
cp lmstudio-model-switcher.sh ~/bin/
chmod +x ~/bin/lmstudio-model-switcher.sh
```

Lancez-le avec ses valeurs par défaut prudentes (contexte de 8192 tokens, GPU max, API sur localhost) :

```bash
~/bin/lmstudio-model-switcher.sh
```

Remplacez les réglages pour une seule exécution :

```bash
LMS_CONTEXT_LENGTH=16384 \
LMS_GPU=max \
LMS_BIND=0.0.0.0 \
LMS_PORT=1234 \
~/bin/lmstudio-model-switcher.sh
```

### Important : variantes / quantifications de modèles

LM Studio distingue une **clé de modèle logique** de ses variantes de quantification téléchargées. Télécharger `publisher/model@q4_k_m` choisit une quantification à stocker, tandis que le chargement utilise couramment la clé de modèle logique indiquée par `lms ls`.

Si vous conservez plusieurs quantifications du même modèle logique, inspectez-les avec :

```bash
lms ls --variants
lms ls --json | jq
```

Le comportement de sélection des variantes a changé d'une version de la CLI LM Studio à l'autre ; consultez donc `lms get --help` / `lms load --help` de la version installée avant d'automatiser un changement de quantification. Le script fourni change volontairement de **clé de modèle**, l'opération stable et compréhensible pour un guide généraliste.

## 13. Changer le modèle par défaut au démarrage

Le sélecteur interactif change ce qui est chargé **maintenant**. Si le service systemd charge explicitement un modèle au démarrage, un redémarrage rechargera le modèle écrit dans `/etc/systemd/system/lmstudio.service`.

Pour faire d'un autre modèle le modèle de démarrage permanent :

```bash
sudo nano /etc/systemd/system/lmstudio.service
```

Modifiez la ligne `lms load ...`, puis :

```bash
sudo systemctl daemon-reload
sudo systemctl restart lmstudio.service
```

Un déploiement plus avancé peut utiliser le chargement JIT des modèles au lieu d'en épingler un au démarrage. LM Studio prend en charge le chargement JIT et l'éviction automatique ; c'est utile lorsque les applications choisissent les modèles dynamiquement.

## 14. Surveiller le serveur

Modèles chargés :

```bash
lms ps
```

Instantané du GPU :

```bash
nvidia-smi
```

Rafraîchir toutes les deux secondes :

```bash
watch -n 2 nvidia-smi
```

CPU/RAM :

```bash
htop
```

Moniteur GPU interactif :

```bash
nvtop
```

Les journaux de LM Studio peuvent aussi être suivis depuis la CLI :

```bash
lms log stream
```

## 15. Une méthode pratique de choix du modèle

Une façon raisonnable de choisir un modèle :

1. Définir d'abord la charge de travail réelle.
2. Identifier les modèles dotés de la capacité requise (par exemple usage d'outils, programmation, vision ou raisonnement).
3. Télécharger d'abord une variante de classe Q4 lorsqu'elle existe.
4. Utiliser `--estimate-only` avant le chargement si la mémoire est juste.
5. Tester de vrais prompts et appels d'outils.
6. Passer à Q5/Q6 seulement si Q4 perd en fiabilité ou en qualité de façon significative.
7. Passer à Q8 seulement si l'amélioration est visible et vaut la RAM/VRAM supplémentaire.
8. Si même le Q8 d'un petit modèle n'est pas assez capable, tester un **modèle plus grand/meilleur en quantification modérée** plutôt que de supposer qu'encore plus de précision résoudra le problème.

Le choix du modèle reste ainsi empirique plutôt que numérique : **le meilleur modèle est la configuration la plus petite/rapide qui répond de façon fiable aux besoins de l'application.**

## 16. Liste de contrôle de dépannage

Si LM Studio ne se comporte pas comme prévu, vérifiez dans cet ordre :

```bash
lms --version
lms daemon up
lms ls
lms ps
lms server status
nvidia-smi
free -h
```

Pour un modèle qui pourrait ne pas tenir, estimez d'abord :

```bash
lms load publisher/model-name --context-length 8192 --gpu max --estimate-only
```

Pour un client distant qui n'arrive pas à se connecter, vérifiez d'abord en local :

```bash
curl -s http://127.0.0.1:1234/v1/models | jq
```

Puis vérifiez que le serveur écoute sur l'adresse et le port prévus :

```bash
ss -ltnp | grep 1234
sudo ufw status numbered
```

Si l'accès local fonctionne mais pas l'accès distant, examinez l'adresse d'écoute, les règles du pare-feu, le routage et l'authentification de l'API avant de changer de modèle.

## 17. Ce que ce guide laisse volontairement ouvert

Il ne prescrit pas de modèle universel. Le matériel, la charge de travail, la longueur de contexte, l'architecture du modèle et les capacités requises varient trop pour cela. Il n'exige pas non plus de CUDA Toolkit séparé, de Docker, d'interface de bureau ni d'intégrations propres à une application.

Une fois le serveur stable, des guides propres à chaque application peuvent définir leur modèle, leur contexte, leur point d'accès API, leur authentification et leurs exigences de benchmark.

---

## Sources officielles

- CLI LM Studio : https://lmstudio.ai/docs/cli
- Headless / llmster : https://lmstudio.ai/docs/developer/core/headless
- Tâche de démarrage Linux : https://lmstudio.ai/docs/developer/core/headless_llmster
- Télécharger des modèles (`lms get`) : https://lmstudio.ai/docs/cli/local-models/get
- Charger/décharger des modèles (`lms load`) : https://lmstudio.ai/docs/cli/local-models/load
- Démarrer le serveur API : https://lmstudio.ai/docs/cli/serve/server-start
- Téléchargement de modèles / quantification : https://lmstudio.ai/docs/app/basics/download-model
- Démarrage rapide de l'API REST : https://lmstudio.ai/docs/developer/rest/quickstart

---

## Annexe : `lmstudio-model-switcher.sh`

Enregistrez ceci sous `lmstudio-model-switcher.sh`, puis installez-le comme indiqué en Partie 2 § 12.

```bash
#!/usr/bin/env bash
set -euo pipefail

# LM Studio interactive model switcher
# Optional environment overrides:
#   LMS_CONTEXT_LENGTH=16384 LMS_GPU=max LMS_BIND=0.0.0.0 LMS_PORT=1234 ./lmstudio-model-switcher.sh

CONTEXT_LENGTH="${LMS_CONTEXT_LENGTH:-8192}"
GPU="${LMS_GPU:-max}"
BIND="${LMS_BIND:-127.0.0.1}"
PORT="${LMS_PORT:-1234}"

need() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Error: '$1' is required but was not found in PATH." >&2
    exit 1
  }
}

need lms
need jq

# Make sure the headless daemon is available.
lms daemon up >/dev/null

mapfile -t MODELS < <(
  lms ls --json | jq -r '
    if type == "array" then .[]
    elif has("models") then .models[]
    else empty end
    | .modelKey // .model_key // empty
  ' | awk 'NF' | sort -u
)

if ((${#MODELS[@]} == 0)); then
  echo "No downloaded models were found."
  echo "Download one first, for example: lms get <publisher/model>"
  exit 1
fi

echo
echo "Downloaded LM Studio models"
echo "---------------------------"
for i in "${!MODELS[@]}"; do
  printf '  %2d) %s\n' "$((i + 1))" "${MODELS[$i]}"
done
echo "   0) Cancel"
echo

while true; do
  read -r -p "Choose a model [0-${#MODELS[@]}]: " CHOICE
  [[ "$CHOICE" =~ ^[0-9]+$ ]] || { echo "Please enter a number."; continue; }
  (( CHOICE == 0 )) && exit 0
  (( CHOICE >= 1 && CHOICE <= ${#MODELS[@]} )) && break
  echo "Choice out of range."
done

MODEL="${MODELS[$((CHOICE - 1))]}"

echo
echo "Selected: $MODEL"
echo "Context : $CONTEXT_LENGTH"
echo "GPU     : $GPU"
echo

read -r -p "Unload the current model and load this one? [y/N]: " CONFIRM
[[ "$CONFIRM" =~ ^[Yy]$ ]] || { echo "Cancelled."; exit 0; }

echo "Unloading currently loaded models..."
lms unload --all || true

echo "Loading $MODEL..."
lms load "$MODEL" --context-length "$CONTEXT_LENGTH" --gpu "$GPU" --yes

# Start the HTTP server only if it is not already running.
if ! lms server status >/dev/null 2>&1; then
  echo "Starting LM Studio server on $BIND:$PORT..."
  lms server start --bind "$BIND" --port "$PORT"
fi

echo
echo "Current LM Studio state:"
lms ps

echo
echo "API model list:"
if command -v curl >/dev/null 2>&1; then
  curl -fsS "http://127.0.0.1:${PORT}/v1/models" | jq . || \
    echo "API check failed. If the server uses another port, set LMS_PORT accordingly."
else
  echo "curl is not installed; skipping API check."
fi
```
