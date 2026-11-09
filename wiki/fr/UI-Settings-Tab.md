# Onglet Services

<img src="images/services.png" class="inline" height="20" alt="Services"> La tuyauterie. Le sélecteur de **Langue**
s'applique partout et se trouve en haut, puis viennent trois sous-onglets : **Services IA**, **Audio** et **Appuyer pour
parler**. Le pied de page contient [**Sauvegarder paramètres et données**](#backup-settings-and-data), **Restaurer...** et
le bouton de mise à jour — *L'application est à jour* ou *Mise à jour disponible*.

---

## Langue

La langue de vos commandes vocales et de l'interface de l'application. La changer redessine toute la fenêtre immédiatement,
et Vega annonce le changement à voix haute.

Le sélecteur est **verrouillé** pour qu'un clic ou un défilement accidentel ne change pas votre langue. Décochez le cadenas
à côté pour la changer ; il se reverrouille ensuite tout seul.

Prises en charge : anglais, russe, ukrainien, allemand, français, espagnol, italien, portugais, portugais du Brésil.

> Le **dossier du journal** se trouve désormais dans l'[onglet Commandant](UI-Commander-Tab), et le **dossier des raccourcis**
> dans l'[onglet Commandes](UI-Bindings-Tab).

---

## Services IA

![Services IA](images/ui-tab-settings-ai.png)

Deux sélecteurs — un pour le modèle de langage, un pour la voix — et le côté inutilisé de chacun
est grisé pour qu'on voie clairement lequel est actif.

C'est le seul onglet de l'application qui fonctionne avec un **brouillon**. Rien n'est écrit tant
que vous n'avez pas cliqué sur **Enregistrer**, et quitter l'onglet avec des modifications en
attente vous propose *Enregistrer*, *Abandonner* ou *Continuer l'édition*.

### Modèle de langage (LLM)

Basculez entre **LMStudio** (local) et **Configuration cloud**.

**LMStudio**

| Champ | Remarques |
|-------|-------|
| **Adresse** | Par défaut l'URL de LM Studio, `http://localhost:1234/v1/chat/completions`. Indiquez l'IP d'une autre machine si l'inférence tourne ailleurs sur votre réseau local |
| **Modèle** | Le nom du modèle. Un seul modèle sert les commandes comme les requêtes |

Le modèle local pris en charge est **`google/gemma-4-e4b`**. Elite Intel vous prévient au démarrage
si votre modèle local est différent ; d'autres modèles peuvent mal fonctionner, voire pas du tout.

Sur un PC avec une NVIDIA RTX 3090 (24 Go) ou mieux, un bouton sous ces champs configure LM Studio pour vous :

| Bouton | Ce qu'il fait |
|--------|---------------|
| **Installer LM Studio** | Installe LM Studio, télécharge le modèle, le charge sur la carte graphique et remplit Adresse et Modèle |
| **Configurer LM Studio** | Pour un LM Studio que vous avez déjà : ajoute le modèle et remplit les champs, rien de plus |
| **Reprendre la configuration** | Reprend une installation interrompue |
| **Désinstaller LM Studio** | Retire le LM Studio installé par Elite Intel, avec ses modèles, après les avoir listés |

Un LM Studio installé ainsi démarre et s'arrête avec les services d'Elite Intel. Détails dans
[Choisir votre LLM](installing-local-llms).

Guides pour LM Studio sur un second PC : [LM Studio sous Linux](Install-LM-Studio-Linux) ·
[LM Studio sous Windows](Install-LM-Studio-Windows) ·
[Série AMD RX](AMD-RX-7800XT-LLM-Setup) ·
[Serveur Ubuntu headless](LM-Studio-Ubuntu-Server)

**Configuration cloud**

| Champ | Remarques |
|-------|-------|
| **Fournisseur** | Choisissez-en un : **Anthropic (Claude)**, **OpenAI**, **Google (Gemini)**, **xAI (Grok)**, **DeepSeek** |
| **Clé API** | Votre clé pour ce fournisseur, avec une case **Verrouillé** à côté pour qu'une clé enregistrée ne soit pas modifiée par accident. Décochez-la pour la changer |

Vous ne choisissez pas de modèle — le bon est sélectionné automatiquement pour votre fournisseur.

Une clé appartient à un fournisseur : choisir un autre fournisseur vide le champ de la clé pour
coller celle de ce fournisseur, et rechoisir votre fournisseur enregistré fait revenir sa clé ; un
clic accidentel ne coûte donc rien. **Enregistrer** reste grisé tant que fournisseur et clé ne
sont pas renseignés.

Comment obtenir une clé chez chaque fournisseur : [Options LLM cloud](cloud-llm-options).

### Voix (TTS)

Basculez entre **Synthèse vocale locale** et **Cloud · Google**.

| Moteur | Où il tourne | Notes |
|--------|--------------|-------|
| **Supertonic 3** | Sur votre PC | Le moteur par défaut. 45 voix, et chaque voix parle toutes les langues de l'application et du jeu. Pas de clé, et rien ne quitte votre PC |
| **Google** | Serveurs de Google | Google Cloud Text-to-Speech. Nécessite une **Clé Google TTS** (avec la même case Verrouillé). Un curseur **Hauteur Google WaveNet**, utilisé par les voix WaveNet, et un curseur **Vitesse de la voix Google** |

Le trafic radio est toujours parlé par Supertonic 3, quel que soit le moteur choisi pour Vega.

> Changer de moteur remet la voix de chaque vaisseau à celle par défaut du nouveau moteur. Les personnalités sont conservées.
> Une confirmation vous est demandée avant.

<a id="voice-tuning"></a>

### Réglage des voix

![Réglage des voix](images/ui-voice-tuning.png)

Les voix diffèrent un peu en volume et en rythme, donc **Réglage des voix…** vous permet d'ajuster chacune séparément. La
liste de gauche est groupée en **Voix féminines** et **Voix masculines** ; sélectionnez-en une pour voir ses réglages.

| Contrôle | Ce qu'il fait |
|----------|---------------|
| **Vitesse** | La vitesse à laquelle cette voix parle |
| **Volume** | Un ajustement de volume pour cette voix |
| **Égaliseur** | Six bandes pour façonner le son de cette voix |
| **Tester** | Joue une phrase avec les réglages actuels |
| **Réinitialiser la voix** | Remet cette voix à ses valeurs par défaut |

Il n'y a pas de bouton Enregistrer : un changement est enregistré dès que vous relâchez le curseur. Vous pouvez ouvrir le
Réglage des voix quel que soit le moteur qui parle pour Vega, car Supertonic 3 fait de toute façon la voix de la radio.

### Pied de page

**Restaurer les valeurs par défaut** remet le modèle de langage sur LM Studio local avec l'adresse
et le modèle par défaut, et enregistre aussitôt. **Enregistrer** valide tout le reste ; il reste
grisé tant que rien ne change vraiment, et la mention **Modifications non enregistrées** apparaît
à côté dès que c'est le cas.

L'enregistrement ne redémarre que le nécessaire — changer le modèle ou la clé redémarre le
cerveau, changer le moteur de voix ou sa clé redémarre la voix.

---

## Audio

![Paramètres audio](images/ui-tab-settings-audio.png)

### Périphériques audio

Listes **Micro** et **Haut-parl.**, ou *(Par défaut du système)*. Les mêmes sélecteurs sont
accessibles via le bouton **Périphériques audio** de l'onglet Vega. Un changement s'applique tout
de suite — seul le service qui utilise le périphérique redémarre.

**Activer la réduction du bruit** avec une intensité **Faible / Moyenne / Élevée**. Commencez par
Moyenne. Élevée est pour les pièces vraiment bruyantes — elle est agressive, et trop filtrer peut
coûter de la précision de transcription.

Sous les périphériques se trouvent deux onglets.

### Niveaux audio

| Curseur | Ce qu'il fait |
|--------|--------------|
| **Volume de la voix** | Le volume de Vega |
| **Volume de la radio** | Le volume des transmissions radio. Grisé tant que les transmissions radio sont désactivées |
| **Volume des bips** | Le bip de confirmation — il retentit quand la reconnaissance vocale a fini et que le modèle de langage a votre demande |
| **Threads STT** | Threads CPU pour la transcription (4–11). Un minimum demandé, pas une réservation : l'application en demande autant, utilise ce que le processeur lui donne et les libère une fois le travail fini |

La vitesse à laquelle Vega parle se règle par voix dans le [Réglage des voix](#voice-tuning), ou avec **Vitesse de la voix
Google** pour une voix Google. Le volume de la musique est dans l'[onglet Jukebox](UI-Jukebox-Tab), pour ne jamais baisser le
mauvais.

### Audio des transmissions

Le son des messages radio.

| Contrôle | Ce qu'il fait |
|---------|--------------|
| **Bip radio au début et à la fin de chaque message** | Des tonalités autour de chaque transmission, avec leur propre curseur de volume |
| **Effet radio** | Un son radio plus marqué, dégradé |
| **Appliquer les effets sélectionnés au chat radio et aux messages des PNJ** | Les tonalités et l'effet radio ci-dessus s'appliquent au trafic radio. Désactivé, la voix radio garde son filtre standard |
| **Appliquer les effets sélectionnés à VEGA à pied ou en SRV** | Vega sonne comme à la radio quand vous n'êtes pas dans le vaisseau |

### Moniteur microphone

Un vumètre en direct sur le côté droit. Il se lit ainsi :

- **FLOOR** — votre niveau de bruit quand vous ne parlez *pas*.
- **GATE** — le seuil. L'audio au-dessus du seuil est capté pour la transcription ; quand il
  repasse dessous, ce qui a été capté est transcrit et envoyé au modèle de langage.
- **CLIP** — vous saturez le micro. Tout ce qui monte là-haut est mal transcrit.

L'état affiche **OPEN**, **MARGINAL**, **CLOSED** ou **HOT** (saturation). Sous le vumètre, un
message en clair apparaît quand quelque chose ne va pas : *Microphone non calibré*, ou *Microphone
trop faible pour la pièce* — augmentez le niveau d'entrée dans les paramètres audio du système,
puis recalibrez. Quand le micro va bien, aucun message n'est affiché.

Si le vumètre ne montre pas un écart net entre FLOOR et votre niveau de parole, lancez **CALIBRER
L'AUDIO** dans l'onglet Vega — il règle le seuil pour vous, et vous prévient si l'écart est trop
faible pour travailler.

---

## Push To Talk

![Push to Talk](images/ui-tab-settings-push-to-talk.png)

Avec le Push to Talk activé, le micro est fermé tant que vous ne maintenez pas un bouton. Ce qu'il
capte sans le bouton enfoncé est jeté comme bruit ambiant.

| Contrôle | Remarques |
|---------|-------|
| **Activer Push to Talk** | L'interrupteur principal |
| **Contrôleur** | Toute manette ou HOTAS connecté. Votre contrôleur enregistré est resélectionné automatiquement à la reconnexion |
| **Bouton** | Quel bouton dessus, avec le libellé que vous lui avez donné dans le [Concepteur d'alias](UI-Bindings-Tab) |
| **Bouton de souris** | Un second déclencheur : *Bouton du milieu*, *Bouton 4 (précédent)* ou *Bouton 5 (suivant)*. Pratique à pied ou en SRV, quand le HOTAS est hors de portée. Les boutons gauche et droit ne sont pas proposés — ils tirent avec vos armes |

Maintenez le bouton, parlez, relâchez. Appuyer **coupe aussi Vega en pleine phrase**, vous n'avez
donc jamais à attendre qu'elle ait fini.

Tant que le Push to Talk est actif, le bouton **DORMIR / SE RÉVEILLER** de l'onglet Vega est
désactivé — le bouton est la porte. Un changement ici s'applique dès l'appui suivant, et le bouton
fonctionne que vous ouvriez cet onglet ou non.

---

<a id="backup-settings-and-data"></a>

## Sauvegarder paramètres et données

Pour passer à un nouvel ordinateur, ou simplement par précaution. **Sauvegarder paramètres et données** écrit un `.zip`
contenant vos réglages, bases de données, commandes personnalisées et les sauvegardes de BindForge.

**Les clés API nécessitent un mot de passe.** Choisissez-en un et cliquez sur **Verrouiller les clés et enregistrer** pour
inclure vos clés dans la sauvegarde. Il vous faudra ce mot de passe pour les restaurer. Si vous choisissez **Sans clés API**,
la sauvegarde n'en contient aucune et vous les saisissez à nouveau après la restauration.

**Restaurer...** remet une sauvegarde. Ce qu'elle remplace est gardé dans un dossier de sécurité dans le dossier de données de
l'application, et Elite Intel redémarre pour terminer. Une sauvegarde faite par une version plus récente nécessite d'abord de
mettre à jour l'application. Les dossiers qui n'existent pas sur le nouvel ordinateur sont réinitialisés ; vérifiez donc ensuite
le dossier du journal dans l'onglet Commandant et celui des raccourcis dans l'onglet Commandes.

> Ceci sauvegarde **Elite Intel**. Pour sauvegarder vos **fichiers de commandes du jeu**, utilisez les
> [Sauvegardes du joueur](UI-Bindings-Tab) dans l'onglet Commandes.

---

## Où sont stockés les réglages

Tous les réglages et données sont stockés sur votre PC :

- **Linux :** `~/.local/share/elite-intel/` (ou `$XDG_DATA_HOME/elite-intel/`)
- **Windows :** `%LOCALAPPDATA%\elite-intel\`

La base de données est dans `db`, les commandes perso dans `custom-commands` (avec son propre `backups`), et les sauvegardes de
vos fichiers de commandes dans `playerbackups`, sauf si vous avez choisi un autre dossier pour elles.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
