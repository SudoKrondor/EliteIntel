# Onglet Paramètres

<img src="images/settings.png" class="inline" height="20" alt="Paramètres"> La tuyauterie. Une bande
**Général** qui s'applique partout, puis trois sous-onglets : **Services IA**, **Audio** et **Push To
Talk**.

---

## Général

Affichée au-dessus des sous-onglets, parce qu'elle s'applique à tous.

**Langue** — la langue de vos commandes vocales comme de l'interface de l'application. En choisir une
réaffiche toute la fenêtre immédiatement, et Vega annonce le changement à voix haute.

Prises en charge : anglais, espagnol, français, allemand, italien, portugais, portugais brésilien,
ukrainien, russe.

**Dossier du journal** — l'endroit où Elite Dangerous écrit ses fichiers de journal. Facultatif :
laissez vide et l'emplacement standard de votre plateforme est utilisé. C'est par là qu'Elite Intel
sait ce qui se passe autour de votre vaisseau : s'il est incorrect, l'application est effectivement
aveugle, et elle le dira au démarrage.

---

## Services IA

![Services IA](images/ui-tab-settings-ai.png)

**Réécrit en V1.1.** Les anciennes cases « Utiliser » éparpillées ont disparu. Il y a désormais deux
commutateurs — un pour le modèle de langage, un pour la voix — et le côté inutilisé de chacun est
grisé, si bien qu'on voit immédiatement lequel est actif.

C'est aussi le seul onglet de l'application à travailler sur un **brouillon**. Rien n'est écrit tant
que vous n'appuyez pas sur **Enregistrer**, et tenter de partir avec des modifications non
enregistrées vous propose *Enregistrer*, *Annuler* ou *Continuer à modifier*.

### Modèle de langage (LLM)

Basculez entre **Configuration locale** et **Configuration cloud**.

**Configuration locale**

| Champ | Remarques |
|-------|-------|
| **Adresse** | Par défaut, l'URL habituelle de LM Studio. Pointez-la sur l'IP d'une autre machine si l'inférence tourne ailleurs sur votre réseau |
| **Modèle** | Le nom du modèle. **Un seul champ** — la V1.1 utilise un modèle unique pour les commandes et les requêtes |

Le modèle local par défaut et recommandé est **`google/gemma-4-e4b`**. Elite Intel vous avertit au
démarrage si votre modèle local est un autre ; d'autres modèles peuvent mal fonctionner, voire pas du
tout.

Guides d'installation : [LM Studio sous Linux](Install-LM-Studio-Linux) ·
[LM Studio sous Windows](Install-LM-Studio-Windows) ·
[Série AMD RX](AMD-RX-7800XT-LLM-Setup)

**Configuration cloud**

Un champ : votre **Clé API**, avec une case **Verrouillé** à côté pour qu'une clé enregistrée ne soit
pas modifiée par accident. Décochez Verrouillé pour la changer.

Fournisseurs pris en charge : **Gemini, Grok, OpenAI, Claude, Deepseek, Mistral.**

> Vous ne choisissez plus de modèle. Elite Intel reconnaît le fournisseur à la forme de votre clé et
> sélectionne lui-même le bon modèle.

Mistral propose un palier gratuit et constitue le moyen le plus simple de démarrer.
Voir [Options de LLM cloud](cloud-llm-options) pour savoir comment obtenir une clé chez chaque
fournisseur.

### Voix (TTS)

Basculez entre **Local · Kokoro** et **Cloud · Google**.

- **Local · Kokoro** n'a aucune configuration. 53 voix, intégrées, sans clé ni téléchargement.
- **Cloud · Google** nécessite une **Clé Google TTS**, avec la même case Verrouillé.

> Changer de moteur réinitialise la voix de chaque vaisseau à la voix par défaut du nouveau moteur.
> Les personnalités des vaisseaux sont conservées. Une confirmation vous est demandée au préalable.

### Pied de page

**Restaurer les valeurs par défaut** ramène la configuration du modèle de langage à LM Studio local
avec le modèle par défaut, et enregistre immédiatement. **Enregistrer** valide tout le reste ; le
bouton est grisé tant que rien ne change réellement, et l'indication **Modifications non
enregistrées** apparaît à côté dès que c'est le cas.

L'enregistrement ne redémarre que le nécessaire : changer le modèle redémarre le cerveau, changer la
clé vocale redémarre la bouche.

---

## Audio

![Paramètres audio](images/ui-tab-settings-audio.png)

### Périphériques audio

Listes déroulantes **Micro** et **Haut-parl.**, ou *(Défaut système)*. Les mêmes sélecteurs sont
accessibles depuis le bouton **Périphériques audio** de l'onglet Vega.

> Les changements de périphérique prennent effet au **prochain démarrage des services**.

**Activer la réduction du bruit** avec une intensité **Faible / Moyenne / Élevée**. Commencez à
Moyenne. Élevée est réservée aux pièces vraiment bruyantes — elle est agressive, et un filtrage
excessif peut vous coûter en précision de transcription.

### Niveaux audio

| Curseur | Ce qu'il fait |
|--------|--------------|
| **Volume de la voix** | Le volume auquel Vega parle |
| **Vitesse TTS** | La vitesse à laquelle Vega parle |
| **Volume des bips** | Le bip de confirmation — il se déclenche quand la reconnaissance vocale a terminé et que le modèle de langage a votre entrée |
| **Threads STT** | Threads CPU pour la transcription (4–11). Une demande minimale, pas une réservation : l'application en demande autant, utilise ce que le processeur lui donne, et les libère une fois le travail fait |

### Moniteur du microphone

Un vumètre en direct le long du bord droit. Trois choses à y lire :

- **FLOOR** — votre niveau de bruit quand vous ne parlez *pas*.
- **GATE** — le seuil. L'audio au-dessus de la porte est transmis pour transcription ; quand il
  redescend en dessous, ce qui a été capturé est transcrit et envoyé au modèle de langage.
- **CLIP** — vous saturez le microphone. Tout ce qui dépasse cette ligne se transcrit mal.

Vous voulez un écart net entre FLOOR et votre niveau de parole, et rien qui touche CLIP. Si ce n'est
pas ce que vous voyez, lancez **CALIBRER L'AUDIO** dans l'onglet Vega — il règle la porte pour vous
et vous prévient si l'écart entre la voix et le bruit est trop faible pour travailler.

---

## Push To Talk

![Push to talk](images/ui-tab-settings-push-to-talk.png)

Le push-to-talk fonctionne avec un **bouton de manette ou de HOTAS**, pas du clavier. Vous cédez un
bouton et vous gagnez un microphone fermé, sauf quand vous voulez l'ouvrir.

| Réglage | Remarques |
|---------|-------|
| **Activer Push to Talk** | L'interrupteur principal. Tout le reste est désactivé tant qu'il n'est pas activé |
| **Contrôleur** | N'importe quelle manette connectée qu'Elite Intel détecte. Il resélectionne automatiquement votre manette enregistrée lorsqu'elle se reconnecte |
| **Bouton** | Quel bouton dessus |

Deux modes :

- **Basculer veille / réveil** — le bouton fait passer Vega de l'endormissement à l'écoute. Endormie,
  Vega ignore tout sauf `Wake up!`, et le mot de contournement `listen` / `listen up` laisse toujours
  passer une commande unique : *« Listen up — lower the landing gear. »*
- **Push To Talk** — Vega ignore tout par défaut. Maintenez le bouton, écoutez le bip, parlez,
  relâchez. Un second bip confirme que votre entrée est en cours de traitement.

Tant que le push-to-talk est actif, le bouton **Sommeil / Réveil** de l'onglet Vega est désactivé :
c'est le bouton de la manette qui fait office de porte.

Le bouton fonctionne que vous ouvriez cet onglet ou non.

---

## Où vivent les paramètres

Tous les paramètres et données sont stockés dans une base SQLite :

- **Linux :** `~/.local/share/elite-intel/elite-intel/db/`
- **Windows :** `%APPDATA%\elite-intel\db\`

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
