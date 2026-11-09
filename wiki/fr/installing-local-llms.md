# Choisir votre LLM

Elite Intel a besoin d'un modèle de langage (LLM) pour comprendre vos commandes et répondre à vos
questions. Il y a deux façons d'en obtenir un, et les deux se configurent dans **Services →
Services IA → Modèle de langage (LLM)** :

| Option | Ce qu'il vous faut | Coût |
|--------|--------------------|------|
| **Local : LM Studio** | Un GPU puissant, [LM Studio](https://lmstudio.ai) et le modèle `google/gemma-4-e4b` | Gratuit. Tout reste sur votre PC |
| **Cloud** | Un compte et une clé API chez un fournisseur pris en charge | Le fournisseur facture votre consommation |

![Configuration du LLM](images/llm-setup.png)

---

## Option 1 : local (LM Studio)

La partie gauche du panneau, **LMStudio** :

| Champ | Valeur |
|-------|--------|
| **Adresse** | `http://localhost:1234/v1/chat/completions`. Si LM Studio tourne sur un autre PC de votre réseau, remplacez `localhost` par l'adresse IP de ce PC |
| **Modèle** | `google/gemma-4-e4b` |

LM Studio est le seul serveur d'inférence local pris en charge par Elite Intel, et
`google/gemma-4-e4b` le seul modèle local pris en charge. Il y a deux façons de le faire tourner.

### Sur votre PC de jeu : Elite Intel s'en charge

Si votre PC peut faire tourner le jeu et le modèle ensemble, un bouton apparaît sous les champs de
**LMStudio**. Appuyez sur **Installer LM Studio**, et Elite Intel :

1. installe LM Studio, sur le disque de votre choix,
2. télécharge `google/gemma-4-e4b` (environ 6 Go),
3. le charge sur votre carte graphique et vérifie qu'il y est bien,
4. le teste, et remplit **Adresse** et **Modèle** pour vous.

Chaque étape s'affiche pendant qu'elle se déroule, avec une barre de progression pour le téléchargement. Si une
étape échoue, la fenêtre dit laquelle et pourquoi, et appuyer à nouveau sur le bouton reprend à partir de là.
Prévoyez environ 10 Go d'espace disque. LM Studio est un logiciel gratuit d'Element Labs, qui ne fait pas
partie d'Elite Intel.

Ensuite, Elite Intel démarre LM Studio quand vous démarrez ses services et l'arrête quand vous les arrêtez ou
fermez l'application : le modèle n'occupe la mémoire de votre carte graphique que pendant qu'Elite Intel tourne.
Pour le retirer, appuyez sur **Désinstaller LM Studio** : il liste ce qui sera supprimé avant de supprimer quoi
que ce soit.

**Le bouton n'apparaît qu'avec une NVIDIA RTX 3090 (24 Go) ou mieux.** Le jeu prend 8,5 Go de mémoire vidéo ou
plus et le modèle environ 6 Go, et le navigateur, OBS et Windows en utilisent aussi, donc une carte plus petite
ne tient pas les deux de façon fiable. Les cartes AMD ne sont pas encore détectées : si vous en avez une,
suivez le [guide AMD](AMD-RX-7800XT-LLM-Setup).

Vous avez déjà LM Studio ? Le bouton affiche alors **Configurer LM Studio**. Il ajoute le modèle et remplit les
champs, et laisse sinon votre LM Studio exactement tel quel : Elite Intel ne démarre, n'arrête ni ne retire
jamais un LM Studio que vous avez installé vous-même.

### Sur un second PC : à configurer vous-même

Si votre PC de jeu ne peut pas tenir le jeu et le modèle ensemble, faites tourner LM Studio sur un autre PC de
votre réseau qui ne fait rien d'autre, et mettez son adresse dans **Adresse**. Le PC de jeu n'a alors aucun
travail d'inférence à faire.

Le PC du LLM a besoin de :

- **12 Go de mémoire vidéo**, ce qui laisse de la marge pour le modèle,
- **une puissance de calcul de classe RTX 3080 ou mieux.** Une carte plus lente peut charger le modèle, mais
  répond trop lentement pour être utile.

Un tableau de référence des GPU réalisé par **Kevin Rank** est disponible ici :
[Guide de référence GPU](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

| Guide | |
|-------|---|
| [LM Studio - Linux](Install-LM-Studio-Linux) | Installation en ligne de commande, et exécution en service dès le démarrage |
| [LM Studio - Windows](Install-LM-Studio-Windows) | Installation en ligne de commande, et démarrage à l'ouverture de session |
| [LM Studio sur Ubuntu Server](LM-Studio-Ubuntu-Server) | Installation headless sur une machine d'inférence dédiée |
| [LLM sur la série AMD RX](AMD-RX-7800XT-LLM-Setup) | Configuration propre à AMD |

### Pourquoi `google/gemma-4-e4b` précisément ?

Elite Intel est un analyseur de commandes et un outil d'analyse de données, pas un chatbot de
conversation. Savoir bavarder de façon naturelle ne suffit pas. Le modèle doit déduire la bonne
action de ce que vous dites, analyser les données du jeu et renvoyer des résultats structurés
plutôt qu'une dissertation ou du HTML. Tous les modèles de cette taille n'y arrivent pas de façon
fiable.

L'exigence incontournable est le **function calling** (appel de fonctions). Le companion d'Elite
Intel ne demande pas au modèle de décrire ce qu'il ferait. Il lui donne un ensemble d'outils et
attend qu'il en appelle un, avec des arguments. Un modèle incapable de produire un appel d'outil
bien formé ne peut pas piloter l'application du tout, aussi bien qu'il écrive.
`google/gemma-4-e4b` prend en charge le function calling.

Avec environ 6,3 Go, il tient dans la VRAM à côté du jeu sur une carte de 24 Go, avec de la marge.
Rien n'est déporté sur le CPU, donc les réponses restent rapides.

> **À propos du modèle retiré de la V1.0.** Les versions précédentes recommandaient
> `tulu-3.1-8b-supernova`. Il ne prend pas en charge le function calling, ne peut donc pas faire
> tourner le companion et ne fonctionne plus avec Elite Intel. Si vous suivez un ancien guide,
> ignorez-le et installez `google/gemma-4-e4b`.

---

## Option 2 : cloud

La partie droite du panneau, **Configuration cloud** :

1. Choisissez votre fournisseur dans la liste **Fournisseur**.
2. Collez la clé de ce fournisseur dans **Clé API**.
3. Cochez **Verrouillé** pour que la clé enregistrée ne puisse pas être modifiée par accident.
4. Cliquez sur **Enregistrer**, puis démarrez ou redémarrez les services dans l'onglet Vega.

C'est vous qui choisissez le fournisseur. Elite Intel n'essaie pas de le deviner à partir de la
clé, car les fournisseurs changent trop souvent le format de leurs clés pour que ce soit fiable.
Vous ne choisissez pas de modèle : Elite Intel prend celui qui convient à votre fournisseur.

| Fournisseur | Remarques |
|-------------|-----------|
| **Anthropic (Claude)** | Payant |
| **OpenAI** | Payant |
| **Google (Gemini)** | Payant |
| **xAI (Grok)** | Payant |
| **DeepSeek** | Payant |

Comment obtenir une clé chez chaque fournisseur : [Configurer un LLM cloud](cloud-llm-options).

---

## Lequel choisir ?

**Faites tourner LM Studio en local si :**
- votre PC de jeu a une NVIDIA RTX 3090 24 Go ou mieux : Elite Intel le configure pour vous
- ou vous avez un second PC sur votre réseau avec 12 Go de mémoire vidéo et une puissance de classe RTX 3080
- vous voulez que tout reste sur votre propre matériel

**Utilisez un fournisseur cloud si :**
- votre GPU ne peut pas faire tourner le modèle à côté du jeu, et vous n'avez pas de second PC pour cela
- vous préférez ne pas gérer de serveur d'inférence du tout

Vous hésitez ? Vous pouvez commencer par l'une ou l'autre option et en changer à tout moment sans réinstaller.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
