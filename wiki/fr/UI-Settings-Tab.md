# Onglet Paramètres

<img src="images/settings.png" class="inline" height="20" alt="Paramètres"> La tuyauterie. Une bande
**Général** valable partout, puis trois sous-onglets : **Services IA**, **Audio** et **Push To
Talk**. Le bouton de mise à jour est en pied de page — *L'application est à jour* ou *Mise à jour
disponible*.

---

## Général

Affiché au-dessus des sous-onglets, parce qu'il s'applique à tous.

**Langue** — la langue de vos commandes vocales et de l'interface de l'application. La changer
redessine toute la fenêtre immédiatement, et Vega annonce le changement à voix haute.

Prises en charge : anglais, russe, ukrainien, allemand, français, espagnol, italien, portugais,
portugais du Brésil.

**Dossier du journal** — l'endroit où Elite Dangerous écrit ses fichiers de journal. Facultatif :
laissé vide, l'emplacement standard de votre plateforme est utilisé. C'est ainsi qu'Elite Intel
sait ce qui se passe autour de votre vaisseau ; s'il est faux, l'application est pratiquement
aveugle et le dit au démarrage. Un dossier inutilisable est refusé et le réglage précédent est
conservé.

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

Guides : [LM Studio sous Linux](Install-LM-Studio-Linux) ·
[LM Studio sous Windows](Install-LM-Studio-Windows) ·
[Série AMD RX](AMD-RX-7800XT-LLM-Setup)

**Configuration cloud**

| Champ | Remarques |
|-------|-------|
| **Fournisseur** | Choisissez-en un : **Anthropic (Claude)**, **OpenAI**, **Google (Gemini)**, **xAI (Grok)**, **DeepSeek**, **Mistral** |
| **Clé API** | Votre clé pour ce fournisseur, avec une case **Verrouillé** à côté pour qu'une clé enregistrée ne soit pas modifiée par accident. Décochez-la pour la changer |

Vous ne choisissez pas de modèle — le bon est sélectionné automatiquement pour votre fournisseur.

Une clé appartient à un fournisseur : choisir un autre fournisseur vide le champ de la clé pour
coller celle de ce fournisseur, et rechoisir votre fournisseur enregistré fait revenir sa clé ; un
clic accidentel ne coûte donc rien. **Enregistrer** reste grisé tant que fournisseur et clé ne
sont pas renseignés.

Mistral propose une offre gratuite et c'est le moyen le plus simple de commencer.
Comment obtenir une clé chez chaque fournisseur : [Options LLM cloud](cloud-llm-options).

### Voix (TTS)

Basculez entre **Local · Kokoro / Supertonic** et **Cloud · Google / Edge**. Chaque côté a un
second sélecteur pour choisir le moteur.

| Moteur | Où il tourne | Remarques |
|--------|---------------|-------|
| **Kokoro** | Sur votre PC | Le moteur par défaut. Pas de clé, rien ne quitte votre PC. Ne sait pas prononcer le cyrillique — voir plus bas |
| **Supertonic 3** | Sur votre PC | Dix voix, toutes les langues y compris le russe et l'ukrainien. Pas de clé. Un curseur **Amplification Supertonic 3 (0–100 %)** relève son niveau |
| **Google** | Serveurs de Google | Google Cloud Text-to-Speech. Nécessite une **Clé Google TTS** (avec la même case Verrouillé). Un curseur **Hauteur Google WaveNet** pour les voix WaveNet |
| **Microsoft Edge** | Serveurs de Microsoft | Les voix de lecture à voix haute en ligne de Microsoft. Pas de clé |

**Kokoro et le cyrillique.** Quand la langue de l'application est le russe ou l'ukrainien — ou
quand votre *client de jeu* écrit les échanges radio en russe —, le segment Kokoro est grisé, un
bandeau explique pourquoi, et Supertonic 3 parle à sa place.

> Changer de moteur remet la voix de chaque vaisseau à la voix par défaut du nouveau moteur. Les
> personnalités des vaisseaux sont conservées. Une confirmation vous est demandée avant.

### Pied de page

**Restaurer les valeurs par défaut** remet le modèle de langage sur LM Studio local avec l'adresse
et le modèle par défaut, et enregistre aussitôt. **Enregistrer** valide tout le reste ; il reste
grisé tant que rien ne change vraiment, et la mention **Modifications non enregistrées** apparaît
à côté dès que c'est le cas.

L'enregistrement ne redémarre que le nécessaire — changer le modèle ou la clé redémarre le
cerveau, changer le moteur de voix ou sa clé redémarre la voix. Les curseurs de hauteur et
d'amplification s'appliquent sans redémarrage.

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
| **Vitesse TTS** | La vitesse à laquelle Vega parle |
| **Volume des bips** | Le bip de confirmation — il retentit quand la reconnaissance vocale a fini et que le modèle de langage a votre demande |
| **Threads STT** | Threads CPU pour la transcription (4–11). Un minimum demandé, pas une réservation : l'application en demande autant, utilise ce que le processeur lui donne et les libère une fois le travail fini |

Le volume de la musique n'est pas ici — il est dans l'[onglet Jukebox](UI-Jukebox-Tab), pour ne
jamais baisser le mauvais.

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
| **Bouton** | Quel bouton dessus |
| **Bouton de souris** | Un second déclencheur : *Bouton du milieu*, *Bouton 4 (précédent)* ou *Bouton 5 (suivant)*. Pratique à pied ou en SRV, quand le HOTAS est hors de portée. Les boutons gauche et droit ne sont pas proposés — ils tirent avec vos armes |

Maintenez le bouton, parlez, relâchez. Appuyer **coupe aussi Vega en pleine phrase**, vous n'avez
donc jamais à attendre qu'elle ait fini.

Tant que le Push to Talk est actif, le bouton **DORMIR / SE RÉVEILLER** de l'onglet Vega est
désactivé — le bouton est la porte. Un changement ici s'applique dès l'appui suivant, et le bouton
fonctionne que vous ouvriez cet onglet ou non.

---

## Où sont stockés les réglages

Tous les réglages et données sont stockés sur votre PC :

- **Linux :** `~/.local/share/elite-intel/` (ou `$XDG_DATA_HOME/elite-intel/`)
- **Windows :** `%LOCALAPPDATA%\elite-intel\`

La base de données est dans `db`, les commandes perso dans `custom-commands` (avec son propre
`backups`), vos instantanés manuels de raccourcis dans `playerbackups`, et les copies automatiques
faites avant Appliquer dans `bindings/backups`.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
