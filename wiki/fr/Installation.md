# Installation

Elite Intel **V1.1** est la version actuelle.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Téléchargez le [👉**programme d'installation**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Exécutez le programme d'installation et suivez les instructions à l'écran.
3. Configurer un modèle de langage. **Elite Intel ne peut pas fonctionner sans lui.** Deux options :
   - **LLM local** (gratuit, hors ligne) : voir le [**guide LLM local**](installing-local-llms).
     Nécessite un GPU performant.
   - **LLM cloud** (dispose d'un palier gratuit et se configure plus facilement) : voir
     [**Options de LLM cloud**](cloud-llm-options) pour obtenir une clé API, puis saisissez-la dans
     [**Paramètres → Services IA**](UI-Settings-Tab).
     Offre gratuite, sans carte bancaire : 👉 [**console.mistral.ai**](https://console.mistral.ai/) 👈

Configuration terminée. Ensuite : [**l'interface, onglet par onglet**](UI).

### Liste de vérification au premier lancement

Elite Intel énonce ces avertissements à voix haute au démarrage des services : vous serez donc
informé de tout ce qui manque — mais autant le faire d'emblée :

| Étape | Où |
|------|-------|
| Le pointer vers un modèle de langage | [Paramètres → Services IA](UI-Settings-Tab) |
| Vérifier le dossier du journal | [Paramètres → Général](UI-Settings-Tab) |
| Vérifier le dossier des assignations et corriger les manquantes | [Onglet Bindings](UI-Bindings-Tab) |
| Calibrer l'audio | [Onglet Vega](UI-Vega-Tab) → **CALIBRER L'AUDIO** |

---

### Désinstallation (Linux)

```shell
~/.var/app/elite.intel.app/uninstall
```

----
Pour tout problème, signalez-le sur Matrix. Les rapports de bugs et les pull requests sont les bienvenus.

Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
