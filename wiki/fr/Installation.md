### La version publiée est V1.0, différente de ce que vous voyez sur les captures d'écran.

### Si vous souhaitez la version V1.1, rejoignez l'équipe de bêta-test. 
### 👉[**Rejoindre l'équipe de bêta-test V1.1 ici**](https://matrix.to/#/#krondor:matrix.org)👈

---

## <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Téléchargez le [👉**programme d'installation**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Exécutez le programme d'installation et suivez les instructions à l'écran.
   - **Parakeet STT** (reconnaissance vocale locale) et **Kokoro TTS** (synthèse vocale locale) sont tous deux inclus. Aucune étape ou service supplémentaire n'est requis.
3. Configurez un LLM. Deux options sont disponibles :
   - **LLM local** (gratuit, hors ligne) : Consultez le [**guide LLM local**](installing-local-llms). Nécessite un GPU capable.
   - **LLM cloud** (plus facile à configurer) : Consultez le guide [**Configurer l'application**](UI-and-Configuration-Options) pour la configuration de la clé API.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux
### Installation (toute distribution avec interface graphique - sans sudo)
1. Téléchargez le script d'installation :

```shell
curl -L -o installer.sh https://raw.githubusercontent.com/stone-alex/EliteIntel/refs/heads/master/distribution/installer.sh
```

2. Rendez le script exécutable et exécutez-le :
```shell
chmod +x installer.sh
./installer.sh
```
L'application s'installe dans `~/.var/app/elite.intel.app`.
**Parakeet STT** et **Kokoro TTS** sont tous deux inclus dans l'application. Aucune installation supplémentaire n'est nécessaire. Activez-les dans l'application via les cases à cocher **Onglet Paramètres ☑ Utiliser**.

3. Configurez un LLM. Deux options sont disponibles :
   - **LLM local** (gratuit, hors ligne) : Consultez le [**guide LLM local**](installing-local-llms). Nécessite un GPU capable.
   - **LLM cloud** (plus facile à configurer) : Consultez le guide [**Configurer l'application**](UI-and-Configuration-Options) pour la configuration de la clé API.

Installation terminée. Consultez [**Configurer l'application**](Configuration) pour les étapes suivantes.

---

### Désinstallation

Utilisez l'option `-d` pour supprimer l'application. Le programme d'installation vous demandera confirmation avant de supprimer les données de configuration et de clé API.

```shell
bash installer.sh -d
```

----
Pour tout problème, signalez-le sur Matrix. Les rapports de bugs et les pull requests sont les bienvenus.

Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
