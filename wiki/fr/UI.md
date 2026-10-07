# L'interface d'Elite Intel

Elite Intel est organisé en sept onglets en haut de la fenêtre. Chacun gère une partie distincte
du système, et la plupart contiennent leurs propres sous-onglets.

Cette section passe en revue chaque onglet, chaque contrôle, et ce qu'il fait réellement.

---

## Les sept onglets

| Onglet | À quoi il sert |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | Le poste de pilotage. Démarrer et arrêter les services, suivre la conversation, lire l'état en direct, ouvrir l'overlay HUD en jeu. |
| <img src="images/controller.png" class="inline" height="20" alt="Commandant"> **[Commandant](UI-Commander-Tab)** | Qui vous êtes et comment se comportent vos vaisseaux. Voix et personnalités de la flotte, automatisations, annonces vocales et catalogue Exo-Maîtrise. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Actions"> **[Actions](UI-Actions-Tab)** | Tout ce qu'Elite Intel sait faire. Parcourir le catalogue des commandes intégrées et créer vos propres macros. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> **[Bindings](UI-Bindings-Tab)** | Vos raccourcis Elite Dangerous. Repérer les manques et les conflits, les modifier et les réécrire dans le jeu. |
| <img src="images/settings.png" class="inline" height="20" alt="Paramètres"> **[Paramètres](UI-Settings-Tab)** | La tuyauterie. Langue, dossier du journal, modèle de langage, moteur de voix, audio et push-to-talk. |
| <img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> **[Jukebox](UI-Jukebox-Tab)** | Votre propre musique, jouée sous Vega et baissée automatiquement quand elle parle. |
| <img src="images/stats.png" class="inline" height="20" alt="Statistiques"> **[Statistiques](UI-Stats-Tab)** | Consommation de tokens et télémétrie LLM de la session en cours. |

S'y ajoute l'**[overlay HUD](UI-HUD-Overlay)** — une fenêtre séparée toujours au premier plan (et
une surface VR optionnelle), pilotée depuis l'onglet Vega.

---

## Premier lancement

Elite Intel énonce à voix haute ses avertissements de configuration au démarrage des services,
pour que vous n'ayez pas à chercher ce qui manque. Par ordre d'importance :

1. **Un modèle de langage.** Rien ne fonctionne sans lui. Allez dans
   [Paramètres → Services IA](UI-Settings-Tab) et choisissez un fournisseur cloud puis collez sa
   clé API, ou pointez l'application vers un modèle local. Voir
   [Choisir votre LLM](installing-local-llms).
2. **Le dossier du journal.** Sans lui, Elite Intel ne voit rien de ce qui se passe autour de
   votre vaisseau. [Paramètres → Général](UI-Settings-Tab).
3. **Le dossier des raccourcis.** Sans lui, Elite Intel ne peut pas piloter votre vaisseau.
   [Bindings → Profil de raccourcis](UI-Bindings-Tab). Si le dossier est correct mais que Vega ne
   trouve toujours pas vos raccourcis, ouvrez *Options → Commandes* dans le jeu et modifiez
   n'importe quel raccourci — le jeu n'écrit un fichier de raccourcis qu'après une personnalisation.
4. **Calibrer l'audio.** Fortement recommandé avant le premier vol.
   [Onglet Vega](UI-Vega-Tab) → **CALIBRER L'AUDIO**.

> Elite Intel est conçu pour **Elite Dangerous Odyssey**. Sous Horizons, Vega vous prévient au
> démarrage qu'une grande partie ne fonctionnera pas.

---

## Conventions valables partout

- **La plupart des contrôles enregistrent immédiatement.** Interrupteurs, curseurs et listes
  s'appliquent dès que vous les changez ; aucun bouton Enregistrer à oublier.
- **Deux exceptions fonctionnent avec un brouillon.** *Paramètres → Services IA* conserve vos
  modifications jusqu'à ce que vous cliquiez sur **Enregistrer**, et vous demande *Enregistrer*,
  *Abandonner* ou *Continuer l'édition* si vous quittez avec des modifications en attente. Les
  raccourcis s'accumulent dans un brouillon qui n'est écrit dans Elite Dangerous qu'au clic sur
  **Appliquer**.
- **Changer de langue reconstruit la fenêtre.** Choisir une autre langue dans *Paramètres →
  Général* redessine immédiatement tous les onglets dans cette langue, et Vega annonce le
  changement.
- **Neuf langues sont prises en charge :** anglais, espagnol, français, allemand, italien,
  portugais, portugais du Brésil, ukrainien et russe.
- **Plusieurs commandants sur un même PC.** Elite Intel garde les données de chaque commandant
  séparément et bascule automatiquement quand un autre commandant est chargé dans le jeu — la
  liste de la flotte et les réglages par commandant suivent.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
