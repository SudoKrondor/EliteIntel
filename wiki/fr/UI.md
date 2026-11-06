# L'interface d'Elite Intel

Elite Intel est organisé en sept onglets en haut de la fenêtre. Chacun gère une partie distincte
du système, et la plupart contiennent leurs propres sous-onglets.

Cette section passe en revue chaque onglet, chaque contrôle, et ce qu'il fait réellement.

---

## Les sept onglets

| Onglet | À quoi il sert |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | Le poste de pilotage. Démarrer et arrêter les services, suivre la conversation, lire l'état en direct, ouvrir l'overlay HUD en jeu. |
| <img src="images/commander.png" class="inline" height="20" alt="Commandant"> **[Commandant](UI-Commander-Tab)** | Qui vous êtes et comment se comportent vos vaisseaux. Le dossier du journal, voix et personnalités de la flotte, automatisations, annonces vocales, le catalogue Exo-Maîtrise et vos signets. |
| <img src="images/actions.png" class="inline" height="20" alt="Actions"> **[Actions](UI-Actions-Tab)** | Tout ce qu'Elite Intel sait faire. Parcourir le catalogue des commandes intégrées et créer vos propres macros. |
| <img src="images/keyboard.png" class="inline" height="20" alt="Commandes"> **[Commandes](UI-Bindings-Tab)** | **BindForge.** Modifiez toutes les commandes d'Elite Dangerous sur n'importe quel périphérique, nommez vos contrôleurs et sauvegardez tous vos fichiers de commandes. |
| <img src="images/services.png" class="inline" height="20" alt="Services"> **[Services](UI-Settings-Tab)** | La tuyauterie. Langue, modèle de langage, moteur de voix, audio, push-to-talk et une sauvegarde de vos réglages et données. |
| <img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> **[Jukebox](UI-Jukebox-Tab)** | Votre propre musique, jouée sous Vega et baissée automatiquement quand elle parle. |
| <img src="images/stats.png" class="inline" height="20" alt="Statistiques"> **[Statistiques](UI-Stats-Tab)** | Consommation de tokens et télémétrie LLM de la session en cours. |

S'y ajoute l'**[overlay HUD](UI-HUD-Overlay)** — une fenêtre séparée toujours au premier plan (et
une surface VR optionnelle), pilotée depuis l'onglet Vega.

---

## Premier lancement

Elite Intel énonce à voix haute ses avertissements de configuration au démarrage des services,
pour que vous n'ayez pas à chercher ce qui manque. Par ordre d'importance :

1. **Un modèle de langage.** Rien ne fonctionne sans lui. Allez dans
   [Services → Services IA](UI-Settings-Tab) et choisissez un fournisseur cloud puis collez sa
   clé API, ou pointez l'application vers un modèle local. Voir
   [Choisir votre LLM](installing-local-llms).
2. **Le dossier du journal.** Sans lui, Elite Intel ne voit rien de ce qui se passe autour de
   votre vaisseau. [Commandant → Profil commandant](UI-Commander-Tab).
3. **Vos commandes.** Sans fichier de raccourcis, Elite Intel ne peut pas piloter votre vaisseau. Ouvrez une
   fois l'[onglet Commandes](UI-Bindings-Tab) pour que BindForge se configure, puis cliquez sur **Attribuer
   automatiquement les raccourcis requis par VEGA** pour donner à Vega les touches dont elle a besoin. Si Vega
   dit que vos fichiers de commandes n'existent pas encore, terminez votre entraînement de commandant, puis dans
   *Options → Commandes* du jeu, réglez le déplacement de l'interface de la carte de la galaxie sur les touches
   fléchées et appliquez.

4. **Calibrer l'audio.** Fortement recommandé avant le premier vol.
   [Onglet Vega](UI-Vega-Tab) → **CALIBRER L'AUDIO**.

> Elite Intel est conçu pour **Elite Dangerous Odyssey**. Sous Horizons, Vega vous prévient au
> démarrage qu'une grande partie ne fonctionnera pas.

---

## Conventions valables partout

- **La plupart des contrôles enregistrent immédiatement.** Interrupteurs, curseurs et listes
  s'appliquent dès que vous les changez ; aucun bouton Enregistrer à oublier.
- **Deux exceptions vous attendent.** *Services → Services IA* conserve vos modifications jusqu'à ce que vous
  cliquiez sur **Enregistrer**, et vous demande *Enregistrer*, *Abandonner* ou *Continuer l'édition* si vous
  quittez avec des modifications en attente. Les modifications de l'onglet Commandes attendent que vous cliquiez
  sur **Appliquer**, et tout est sauvegardé avant qu'elles n'atteignent Elite Dangerous.
- **Changer de langue reconstruit la fenêtre.** Choisir une autre langue dans l'onglet *Services* redessine
  immédiatement tous les onglets dans cette langue, et Vega annonce le changement.
- **Neuf langues sont prises en charge :** anglais, russe, ukrainien, allemand, français, espagnol, italien,
  portugais et portugais du Brésil.

- **Plusieurs commandants sur un même PC.** Elite Intel garde les données de chaque commandant
  séparément et bascule automatiquement quand un autre commandant est chargé dans le jeu — la
  liste de la flotte et les réglages par commandant suivent.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
