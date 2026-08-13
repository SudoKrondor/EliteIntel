# L'interface d'Elite Intel

Elite Intel V1.1 s'organise en six onglets en haut de la fenêtre. Chacun prend en charge une
partie distincte du système, et la plupart contiennent leurs propres sous-onglets.

Cette section parcourt chaque onglet, chaque commande, et ce qu'elle fait réellement.

---

## Les six onglets

| Onglet | À quoi il sert |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | La passerelle. Démarrer et arrêter les services, suivre la conversation, lire l'état en direct, ouvrir l'overlay HUD en jeu. |
| <img src="images/controller.png" class="inline" height="20" alt="Commandant"> **[Commandant](UI-Commander-Tab)** | Qui vous êtes et comment se comportent vos vaisseaux. Automatisations, annonces vocales, voix et personnalité par vaisseau. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Actions"> **[Actions](UI-Actions-Tab)** | Tout ce qu'Elite Intel sait faire. Parcourir le catalogue des commandes intégrées et construire vos propres macros. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> **[Bindings](UI-Bindings-Tab)** | Vos assignations de touches Elite Dangerous. Repérer les manques et les conflits, les modifier et les réécrire dans le jeu. |
| <img src="images/settings.png" class="inline" height="20" alt="Paramètres"> **[Paramètres](UI-Settings-Tab)** | La tuyauterie. Langue, dossier du journal, modèle de langage, moteur vocal, audio et push-to-talk. |
| <img src="images/stats.png" class="inline" height="20" alt="Statistiques"> **[Statistiques](UI-Stats-Tab)** | Consommation de tokens et télémétrie du LLM pour la session en cours. |

Il y a aussi l'**[overlay HUD](UI-HUD-Overlay)** — une fenêtre distincte toujours au premier plan
(et éventuellement une surface VR), pilotée depuis l'onglet Vega.

---

## S'il s'agit de votre premier lancement

Elite Intel énonce à voix haute ses avertissements de configuration au démarrage des services,
pour vous éviter de chercher ce qui manque. Par ordre d'importance :

1. **Un modèle de langage.** Rien ne fonctionne sans lui. Allez dans
   [Paramètres → Services IA](UI-Settings-Tab) et collez une clé d'API cloud, ou pointez
   l'application vers un modèle local. Voir [Choisir votre LLM](installing-local-llms).
2. **Le dossier du journal.** Sans lui, Elite Intel est aveugle à tout ce qui se passe autour de
   votre vaisseau. [Paramètres → Commun](UI-Settings-Tab).
3. **Le dossier des assignations.** Sans lui, Elite Intel ne peut pas piloter votre vaisseau.
   [Bindings → Profil d'assignations](UI-Bindings-Tab).
4. **Calibrer l'audio.** Fortement recommandé avant le premier vol.
   [Onglet Vega](UI-Vega-Tab) → **CALIBRER L'AUDIO**.

---

## Conventions valables partout

- **La fenêtre ne retient rien que vous n'ayez enregistré.** Seul l'onglet *Paramètres → Services
  IA* travaille sur un brouillon : il affiche l'indication **Modifications non enregistrées** et
  vous empêche de quitter l'onglet sans trancher. Tout autre interrupteur ou curseur de
  l'application est écrit dès que vous le modifiez.
- **Un modèle de brouillon distinct s'applique aux bindings.** Les modifications vont d'abord dans
  un brouillon et ne sont écrites dans Elite Dangerous qu'après **Appliquer au jeu**.
- **Changer de langue reconstruit la fenêtre.** Sélectionner une nouvelle langue dans *Paramètres
  → Commun* réaffiche immédiatement chaque onglet dans cette langue, et Vega annonce le changement.
- **Neuf langues sont prises en charge :** anglais, espagnol, français, allemand, italien,
  portugais, portugais brésilien, ukrainien et russe.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
