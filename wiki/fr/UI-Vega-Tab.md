# Onglet Vega

<img src="images/ai.png" class="inline" height="20" alt="Vega"> L'onglet par défaut, celui que vous
laissez ouvert en vol. Il démarre et arrête la pile IA, montre ce que Vega a entendu et dit, signale
l'état de chaque sous-système et ouvre l'overlay en jeu.

![Onglet Vega](images/ui-tab-vega.png)

L'onglet est organisé en quatre zones : les journaux **Conversation** et **Diagnostics** à gauche,
**Statut rapide** et **Raccourcis** dans la barre latérale droite, et la bande de télémétrie
**Résumé système** en bas.

---

## Conversation

Tout ce que vous avez dit et tout ce que Vega a répondu, en un seul flux. Vos lignes sont à
gauche, les réponses de Vega à droite, pour qu'une longue session reste lisible d'un coup d'œil.
Les commandes tapées dans le chat du jeu (voir [Toutes les commandes](AllCommands)) apparaissent
ici exactement comme les commandes parlées.

## Diagnostics / messages système

Le journal technique — démarrages de services, résultats de calibration, avertissements de
raccourcis, ce que la reconnaissance vocale a entendu (`STT: [...]`), opérations sur les fichiers.
Il n'est jamais lu à voix haute ; il sert à voir ce que fait l'application.

Quatre boutons se trouvent dans l'en-tête de la section :

| Bouton | Ce qu'il fait |
|--------|--------------|
| **Copier** | Copie dans le presse-papiers le texte sélectionné dans le journal. |
| **Enregistrer le paquet d'assistance** | Écrit un `.zip` horodaté pour un rapport de bug : ce journal, le journal de l'application, votre journal de jeu et les fichiers d'état en direct du jeu, vos raccourcis, vos commandes personnalisées, un résumé du matériel et un résumé des *niveaux* du micro (jamais d'audio). Ce qui n'a pas pu être collecté est listé dans le paquet. **C'est ce qu'il faut joindre à un rapport de bug.** |
| **Vider la mémoire de Vega** | Écrit un instantané JSON de la mémoire de travail de Vega pour la session en cours. Ne fonctionne que si les services tournent. |
| **Effacer** | Vide le journal de diagnostics. |

---

## Statut rapide

Six indicateurs en direct. Chacun affiche un état et une couleur, pour voir d'un coup d'œil si la
pile est en bonne santé.

| Indicateur | États |
|---------|--------|
| **STT** | `Veille` (services arrêtés) · `Écoute` · `Endormi` (vous ignore) · `Push to Talk` (seul le bouton assigné ouvre le micro) |
| **IA** | `Veille` · `Hors ligne` (connexion impossible) · le nom du fournisseur qui a réellement répondu, ou `Actif` |
| **TTS** | `Veille` · `Local` (Kokoro / Supertonic) · `Cloud` (Google / Microsoft Edge) |
| **Touches** | `OK`, ou `N manquant` |
| **Commandes** | Combien de commandes personnalisées sont chargées |
| **Mappage** | `Synchronisé` avec le jeu, ou `Modifié` — vous avez un brouillon de raccourcis non appliqué |

L'indicateur **IA** mérite qu'on le surveille. Il n'indique pas ce que vous avez *configuré*, mais
quel fournisseur a réellement répondu.

---

## Raccourcis

| Bouton | Ce qu'il fait |
|--------|--------------|
| **DÉMARRER / ARRÊTER LES SERVICES** | Active ou coupe toute la pile IA. Le bouton se désactive pendant le démarrage ou l'arrêt pour ne pas être déclenché deux fois. |
| **DORMIR / SE RÉVEILLER** | Éveillée, Vega écoute en continu. Endormie, elle ignore tout sauf une phrase de réveil (`réveille-toi`) ou un ordre précédé de `écoute-moi` — *« Écoute-moi, sors le train d'atterrissage. »* Désactivé tant que le Push to Talk est actif : c'est alors le bouton assigné qui sert de porte. |
| **AFFICHER / MASQUER OVERLAY** | Affiche l'[overlay HUD](UI-HUD-Overlay) toujours au premier plan. L'application se souvient de votre choix et le rétablit au lancement suivant. Si le binaire de l'overlay manque, l'interrupteur le signale dans le journal au lieu de prétendre afficher un overlay inexistant. |
| **RÉGLAGES DE L'OVERLAY** | Ouvre les [réglages de l'overlay HUD](UI-HUD-Overlay) — transparence, taille du texte, couleurs et lieu d'affichage (écran, casque VR, les deux, ou une fenêtre de capture). |
| **Périphériques audio** | Choisir le micro et le haut-parleur. Le changement s'applique tout de suite : seule la reconnaissance vocale (micro) ou la voix (haut-parleur) redémarre. |
| **CALIBRER L'AUDIO** | Mesure le bruit de fond et votre niveau de parole et règle le seuil vocal. Disponible seulement quand les services tournent. À faire une fois avant le premier vol, puis à chaque changement de micro ou de pièce. |
| **Mise à jour** | Apparaît quand une nouvelle version est disponible. |

Entre les deux groupes de boutons se trouve le **bloc commandant** — votre nom, votre vaisseau,
l'heure et votre solde de crédits en direct.

---

## Résumé système

Une bande de télémétrie de six blocs en bas de l'onglet :

| Bloc | Signification |
|-------|---------|
| **Modèle LLM** | Le modèle qui a servi la requête la plus récente |
| **Durée de session** | Temps écoulé depuis le démarrage des services |
| **Tokens utilisés** | Prompt + réponse + cache, pour la session |
| **Tokens / heure** | Un rythme projeté. Reste vide pendant les 10 premières minutes, le temps de collecter des données |
| **Économie cache** | Tokens servis depuis le cache. Le `0` est affiché exprès — c'est une information, pas une donnée manquante |
| **Dernière vitesse** | Tokens par seconde de la dernière réponse |

Pour le détail complet, voir l'[onglet Statistiques](UI-Stats-Tab).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
