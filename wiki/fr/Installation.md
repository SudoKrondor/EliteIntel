# Installation

Elite Intel **V1.2** est la version actuelle.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Téléchargez le [👉**programme d'installation**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Exécutez le programme d'installation et suivez les instructions à l'écran.
3. Configurer un modèle de langage. **Elite Intel ne peut pas fonctionner sans lui.** Deux options :
   - **LLM local** (gratuit, hors ligne) : voir le [**guide LLM local**](installing-local-llms).
     Nécessite un GPU performant.
   - **LLM cloud** (dispose d'un palier gratuit et se configure plus facilement) : voir
     [**Options de LLM cloud**](cloud-llm-options) pour obtenir une clé API, puis saisissez-la dans
     [**Services → Services IA**](UI-Settings-Tab).
     Offre gratuite, sans carte bancaire : 👉 [**console.mistral.ai**](https://console.mistral.ai/) 👈

Configuration terminée. Ensuite : [**l'interface, onglet par onglet**](UI).

### Liste de vérification au premier lancement

Elite Intel énonce ces avertissements à voix haute au démarrage des services : vous serez donc
informé de tout ce qui manque — mais autant le faire d'emblée :

| Étape | Où |
|------|-------|
| Le pointer vers un modèle de langage | [Services → Services IA](UI-Settings-Tab) |
| Vérifier le dossier du journal | [Onglet Commandant](UI-Commander-Tab) |
| Laisser BindForge se configurer et donner à Vega les touches nécessaires | [Onglet Commandes](UI-Bindings-Tab) |
| Calibrer l'audio | [Onglet Vega](UI-Vega-Tab) → **CALIBRER L'AUDIO** |

---

### Nouveau dans Elite Dangerous ?

Le jeu ne crée ses fichiers de commandes qu'une fois que vous avez modifié quelque chose, et sans eux Elite Intel ne peut
pas piloter votre vaisseau. Terminez votre entraînement de commandant, puis dans *Options → Commandes* du jeu, réglez le
déplacement de l'interface de la carte de la galaxie sur les touches fléchées et appliquez.

### Passer à un autre ordinateur

[**Sauvegarder paramètres et données**](UI-Settings-Tab), dans l'onglet Services, regroupe vos réglages, vos données et vos
commandes personnalisées dans un seul fichier que vous pouvez restaurer sur le nouvel ordinateur.

---

### Désinstallation (Linux)

```shell
~/.var/app/elite.intel.app/uninstall
```

----
Pour tout problème, signalez-le sur Matrix. Les rapports de bugs et les pull requests sont les bienvenus.

Communauté 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
