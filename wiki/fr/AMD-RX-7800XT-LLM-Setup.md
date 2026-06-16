# Exécuter des LLMs sur AMD RX 7800 XT (Guide ROCm)

> Guide fourni par **Ian Wirtz**

> **Recommandé :** LM Studio (`lms`) donne généralement les meilleurs résultats, mais Ollama est une alternative viable.

---

## Prérequis

### Étape 1 - Installer `rocm-hip-runtime`

Avant que LM Studio ou Ollama puisse utiliser votre GPU via ROCm, le système a besoin des bibliothèques HIP de l'espace utilisateur pour communiquer avec le pilote du noyau.

**Arch Linux / CachyOS :**
```bash
sudo pacman -S rocm-hip-runtime
```

**Ubuntu / Debian :**
```bash
sudo apt install rocm-hip-runtime
```

**Fedora :**
```bash
sudo dnf install rocm-hip-runtime
```

> **Permissions d'accès au GPU :** Votre utilisateur doit appartenir aux groupes `render` et `video`. Vérifiez avec :
> ```bash
> groups
> ```
> Si l'un des groupes est manquant, ajoutez-vous :
> ```bash
> sudo usermod -aG video,render $USER
> ```
> Vous devez **vous déconnecter et vous reconnecter complètement** (ou redémarrer) pour que le changement de groupe prenne effet.

---

### Étape 2 - Installer `rocm-smi` *(peut être optionnel)*

Sur les distributions à évolution rapide comme Arch, les outils de gestion ne sont pas toujours inclus comme dépendance stricte de `rocm-hip-runtime`. Installez-les explicitement pour éviter les conflits de versions de bibliothèques.

**Arch Linux / CachyOS :**
```bash
sudo pacman -S rocm-smi-lib
```

**Ubuntu / Debian :**

Debian sépare l'outil en ligne de commande et sa bibliothèque d'exécution en paquets distincts.

```bash
# Outil de surveillance en ligne de commande
sudo apt update && sudo apt install rocm-smi

# Bibliothèques d'exécution
sudo apt update && sudo apt install librocm-smi64-1
```

> **Conseil :** Si le nom exact du paquet est incertain, tapez `sudo apt install librocm-smi64` et appuyez sur **Tab** pour compléter automatiquement le suffixe de version actuel.

**Fedora :**
```bash
# Outil CLI
sudo dnf install rocm-smi

# Bibliothèques de développement C/C++ et en-têtes (équivalent à rocm-smi-lib sur Arch)
sudo dnf install rocm-smi-devel
```

---

## Exécuter un modèle

### Étape 3 - Charger un modèle avec l'accélération ROCm

Lors de l'invocation de `lms load`, passez explicitement les indicateurs d'accélération matérielle. L'indicateur `--gpu max` demande à l'environnement d'exécution de charger l'intégralité du modèle dans la VRAM.

```bash
HSA_OVERRIDE_GFX_VERSION=11.0.0 lms load tulu-3.1-8b-supernova --context-length 8192 --gpu max
```

Le préfixe `HSA_OVERRIDE_GFX_VERSION=11.0.0` indique à la pile ROCm de traiter le RX 7800 XT comme une cible de calcul nativement prise en charge, évitant ainsi les échecs silencieux de repli.

---

### Étape 4 - Rendre la configuration permanente

Pour éviter de préfixer chaque commande avec la variable d'environnement, ajoutez-la à votre profil de shell.

**Bash :**
```bash
echo 'export HSA_OVERRIDE_GFX_VERSION=11.0.0' >> ~/.bashrc
source ~/.bashrc
```

**Fish (défaut CachyOS) - Option A : Variable universelle (recommandée)**

Définissez-la une fois ; Fish la conserve automatiquement entre les redémarrages sans configuration supplémentaire :
```fish
set -Ux HSA_OVERRIDE_GFX_VERSION 11.0.0
```

**Fish - Option B : Entrée explicite dans le fichier de configuration**
```fish
echo 'set -gx HSA_OVERRIDE_GFX_VERSION 11.0.0' >> ~/.config/fish/config.fish
source ~/.config/fish/config.fish
```

**Ollama (service systemd) :**

Comme Ollama s'exécute sous son propre utilisateur système `ollama`, la variable doit être injectée via un drop-in systemd :

```bash
sudo mkdir -p /etc/systemd/system/ollama.service.d
sudo nano /etc/systemd/system/ollama.service.d/override.conf
```

Collez ce qui suit, puis sauvegardez et quittez (`Ctrl+O`, `Entrée`, `Ctrl+X`) :

```ini
[Service]
Environment="HSA_OVERRIDE_GFX_VERSION=11.0.0"
```

Puis rechargez et redémarrez le service :
```bash
sudo systemctl daemon-reload
sudo systemctl restart ollama
```

---

## Vérification

### Étape 5 - Confirmer que le pilote de calcul du noyau est chargé

Si une commande se bloque, la couche de calcul du noyau (`amdkfd`) n'est peut-être pas initialisée. Vérifiez si le système expose votre GPU en tant que plateforme de calcul ROCm :

```bash
rocminfo
```

Faites défiler jusqu'en haut de la sortie. Si vous voyez `Can't open /dev/kfd` ou un crash, le noyau Linux n'expose pas l'interface de calcul à l'espace utilisateur. Si vous utilisez un noyau personnalisé ou de pointe, essayez de démarrer avec le noyau standard ou LTS (`linux-lts`) pour exclure une régression de pilote.

---

### Étape 6 - Démarrer le serveur et vérifier l'utilisation de la VRAM

**LM Studio :**
```bash
lms server start
```

**Ollama :**
```bash
ollama serve
```

Puis confirmez que le modèle est chargé en VRAM :
```bash
rocm-smi
```

**En veille (aucun modèle chargé) :**

![rocm-smi output with no model running](images/rocm-smi-without-game-running-example.png)

En veille, le GPU consomme une puissance minimale (~9W), les horloges sont proches du plancher, et l'utilisation de la VRAM est faible (~44%).

**Sous charge (modèle + jeu en cours simultanément) :**

![rocm-smi output with Elite Dangerous and EliteIntel running](images/rocm-smi-with-game-and-Elite-Intel-running-example.png)

Sous charge combinée, vous devriez observer une augmentation significative de l'utilisation de la VRAM (71% dans cet exemple), une hausse de l'utilisation du GPU et une augmentation de la consommation électrique (~147W). Cela confirme que le modèle réside en VRAM et que l'inférence s'exécute sur le GPU.
