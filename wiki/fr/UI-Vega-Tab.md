# Onglet Vega

<img src="images/ai.png" class="inline" height="20" alt="Vega"> L'onglet par défaut, celui que vous
laissez ouvert en vol. Il démarre et arrête la pile IA, montre ce que Vega a entendu et dit, signale
l'état de chaque sous-système et ouvre l'overlay en jeu.

![Onglet Vega](images/ui-tab-vega.png)

L'onglet se répartit en quatre zones : les journaux **conversation** et **diagnostic** sur la
gauche, **État rapide** et **Raccourcis** dans la barre latérale droite, et la bande de télémétrie
**Résumé système** en bas.

---

## Conversation

Tout ce que vous avez dit et tout ce que Vega a répondu, en un seul flux. Vos lignes sont alignées
à gauche, les réponses de Vega à droite, pour qu'une longue session reste lisible d'un coup d'œil.

## Diagnostic / Messages système

Le journal technique : démarrages de services, résultats de calibration, avertissements
d'assignations, opérations sur fichiers. Il n'est jamais énoncé ; il existe pour que vous voyiez ce
que fait l'application.

Quatre boutons se trouvent dans l'en-tête de section :

| Bouton | Ce qu'il fait |
|--------|--------------|
| **Copier** | Copie dans le presse-papiers le texte que vous avez sélectionné dans le journal. Actif uniquement s'il y a une sélection. |
| **Enregistrer le paquet de débogage** | Écrit un `.zip` horodaté contenant le journal système, le journal applicatif, votre fichier de journal en cours et vos assignations. **C'est ce qu'il faut joindre à un rapport de bug.** |
| **Vider la mémoire de Vega** | Écrit un instantané JSON de la mémoire de travail de Vega pour la session en cours. Disponible uniquement pendant que les services tournent. |
| **Effacer** | Vide le journal de diagnostic et sa transcription d'export. |

---

## État rapide

Six indicateurs en direct. Chacun affiche un état et une couleur, si bien qu'un coup d'œil suffit à
savoir si la pile est en bonne santé.

| Indicateur | États |
|---------|--------|
| **STT** | `Veille` (services arrêtés) · `Endormi` (vous ignore) · `Écoute` |
| **IA** | `Veille` · `Hors ligne` (connexion impossible) · ou le nom du fournisseur qui répond réellement |
| **TTS** | `Veille` · `Local` (Kokoro) · `Cloud` (Google) |
| **Bindings** | `OK`, ou `N manquant` |
| **Commandes** | Combien de commandes personnalisées sont chargées |
| **Touches** | `Synchronisé` avec le jeu, ou `Modifié` — vous avez un brouillon d'assignations non appliqué |

L'indicateur **IA** mérite d'être surveillé. Il ne signale pas ce que vous avez *configuré*, il
signale quel fournisseur a réellement répondu à la dernière requête.

---

## Raccourcis

| Bouton | Ce qu'il fait |
|--------|--------------|
| **Démarrer / Arrêter les services** | Bascule toute la pile IA. Le bouton se désactive de lui-même pendant le démarrage ou l'arrêt, pour ne pas être déclenché deux fois. |
| **Sommeil / Réveil** | En mode *réveil*, Vega écoute en continu. En mode *sommeil*, il vous ignore sauf si vous employez le mot de contournement `listen` ou dites `Wake up!`. Désactivé tant que le push-to-talk est actif : en mode PTT, le bouton *est* la porte. |
| **Afficher / Masquer overlay** | Affiche l'[overlay HUD](UI-HUD-Overlay) toujours au premier plan. Si le binaire de l'overlay est absent, le bouton reste honnête et signale l'échec dans le journal au lieu de prétendre à un overlay inexistant. |
| **Réglages de l'overlay** | Ouvre les [réglages de l'overlay HUD](UI-HUD-Overlay) : transparence, taille du texte et emplacement de tracé (moniteur, casque VR, les deux). |
| **Périphériques audio** | Ouvre la boîte de dialogue d'interface audio pour choisir micro et haut-parleur. Les changements prennent effet au prochain démarrage des services. |
| **Calibrer l'audio** | Mesure votre bruit de fond et votre niveau de voix, et règle la porte audio. Disponible uniquement pendant que les services tournent. Lancez-le une fois avant votre premier vol, et de nouveau si vous changez de micro ou de pièce. |
| **Mettre à jour** | Apparaît lorsqu'une nouvelle version est disponible. |

Entre les deux groupes de boutons se trouve le **bloc commandant** : votre nom, votre vaisseau,
l'horloge et votre solde de crédits en direct.

---

## Résumé système

Une bande de télémétrie de six blocs en bas de l'onglet :

| Bloc | Signification |
|-------|---------|
| **Modèle LLM** | Le modèle qui a traité la requête la plus récente |
| **Durée de session** | Temps écoulé depuis le démarrage des services |
| **Tokens utilisés** | Prompt + réponse + cache, pour la session |
| **Tokens / heure** | Un taux projeté. Reste vide les 10 premières minutes, le temps de collecter des données |
| **Économie cache** | Tokens servis depuis le cache. Le `0` est affiché délibérément : c'est une information, pas une donnée manquante |
| **Dernière vitesse** | Tokens par seconde sur la dernière réponse |

Pour le détail complet, voir l'[onglet Statistiques](UI-Stats-Tab).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
