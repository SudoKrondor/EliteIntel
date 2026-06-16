# Choisir un serveur d'inférence local

Pour exécuter un LLM local avec Elite Intel, un **serveur d'inférence** est requis. Il s'agit d'un logiciel qui charge le modèle IA et le sert via une API locale. C'est l'équivalent local d'un service IA cloud, fonctionnant entièrement sur votre propre matériel.

Elite Intel prend en charge deux serveurs d'inférence : **Ollama** et **LM Studio**. Les deux sont compatibles et utilisent les mêmes modèles. Le choix peut être modifié dans les paramètres à tout moment.

![loca llm ui](images/local-llm.png)

## Exigences GPU
Configuration matérielle requise pour faire tourner le jeu et le LLM sur la même machine :

- RTX 3090 24 Go VRAM
- AMD RX 7800 XT

Si vous ne disposez pas du matériel suffisant, utilisez le __[service cloud gratuit](https://v2.auth.mistral.ai/login)__



Un tableau de référence GPU fourni par **Kevin Rank** est disponible ici :
[Guide de référence GPU](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

---
### Guides d'installation

| Serveur d'inférence                                     |                                                                     |
|------------------------------------------------------|---------------------------------------------------------------------|
| [✅ LM Studio - Linux](Install-LM-Studio-Linux)       | Rapide, plus de flexibilité pour les modèles - le guide montre comment le configurer comme serveur |
| [✅ LM Studio - Windows](Install-LM-Studio-Windows)   | Rapide, plus de flexibilité pour les modèles - interface graphique disponible |
| [Ollama - Linux](Install-Ollama-Local-LLM-Linux)     | Recommandé si vous avez le matériel pour le faire tourner |
| [Ollama - Windows](Install-Ollama-Local-LLM-Windows) | Recommandé si vous avez le matériel pour le faire tourner |

---

### Ollama vs. LM Studio en un coup d'œil

|                        | Ollama                              | LM Studio                                                                                                    |
|------------------------|-------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **Vitesse**            | Plus lent                           | Plus rapide                                                                                                  |
| **Modèle préféré**     | [tulu-3.1-8b-supernova Q4_K_M](https://huggingface.co/mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF)|  [tulu-3.1-8b-supernova Q4_K_M](https://huggingface.co/mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF) |
| **Idéal pour**         | Configuration simple, maintenance minimale | Plus de contrôle sur le chargement des modèles |
| **Installation**       | Un script, c'est tout               | Un script, c'est tout                                                                                        |
| **Fonctionne en tant que** | Service système (démarrage auto au boot) | Démarrage manuel, ou démarrage auto optionnel |
| **Réglage du modèle**  | Modelfile intégré au modèle         | Options au moment du chargement                                                                              |
| **Démarrage auto Windows** | ✅ Fonctionne par défaut          | Nécessite l'application de bureau ou le Planificateur de tâches |
| **Démarrage auto Linux** | ✅ Service systemd inclus          | Configuration systemd manuelle                                                                               |
| **Source du modèle**   | Bibliothèque Ollama                 | HuggingFace (GGUF)                                                                                           |
| **Port API**           | `11434`                             | `1234`                                                                                                       |
| **Interface graphique** | Aucune (CLI uniquement)            | Application de bureau optionnelle                                                                            |

---

### Guide de sélection

**Utilisez Ollama quand :**
- Vous souhaitez une installation simple avec une configuration minimale
- Vous êtes sur Windows et préférez ne pas configurer le démarrage manuellement
- Vous débutez avec les LLMs locaux

**Utilisez LM Studio quand :**
- Vous souhaitez une interface graphique pour parcourir, télécharger et gérer les modèles
- Vous êtes déjà familier avec HuggingFace et les fichiers de modèles GGUF
- Vous souhaitez expérimenter avec différents modèles sans écrire de Modelfiles
- Vous utilisez une machine dédiée à l'inférence et avez besoin d'un serveur headless propre

**L'une ou l'autre option convient quand :**
- Vous disposez d'un NVIDIA RTX 3090 24 Go équivalent ou supérieur. La VRAM est le facteur critique, pas la vitesse du GPU. Un GPU avec seulement 12 Go de VRAM est insuffisant quelle que soit sa génération.
- Vous exécutez Elite Dangerous et le LLM sur la même machine
- Vous souhaitez pointer Elite Intel vers un PC séparé sur votre réseau

---
## Recommandation du développeur

Le développeur utilise LM Studio avec [`matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`](https://huggingface.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF). Ce modèle offre une inférence rapide. Le même modèle sur Ollama est notablement plus lent. L'application est optimisée pour ce modèle. D'autres modèles peuvent fonctionner mais ne sont pas garantis. Signalez les résultats de compatibilité sur Matrix.

## Pourquoi tulu3.1:8b Supernova en particulier ?

Elite Intel est un analyseur de commandes et un outil d'analyse de données, pas un chatbot conversationnel. Cela impose des exigences spécifiques au modèle. Générer des bavardages au son naturel est insuffisant. Le modèle doit inférer correctement des actions à partir de l'entrée vocale et effectuer une analyse de données structurée. Il doit retourner des résultats en JSON formaté, et non un essai en balisage ou en HTML. Tous les modèles de cette taille ne réalisent pas cette tâche de manière fiable.

## Tulu 3 (la recette d'entraînement de base) est véritablement exceptionnel

[Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF](https://huggingface.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF/tree/main)

La plupart des modèles d'instruction sont entraînés avec le RLHF, qui utilise un modèle de récompense appris pour juger les sorties. Ce modèle de récompense est lui-même un réseau de neurones, il hérite donc de tous les biais et incohérences habituels. Tulu 3 a remplacé cela par le RLVR (Apprentissage par renforcement avec récompenses vérifiables). Au lieu d'un modèle de récompense appris, l'entraînement utilise une fonction de notation déterministe : la réponse est soit correcte, soit elle ne l'est pas. Binaire, sans biais. Cela est particulièrement impactant pour les tâches de suivi d'instructions, où le signal de récompense est objectif.

Le pipeline d'entraînement est une approche en quatre étapes : curation des données ciblant les compétences fondamentales, ajustement fin supervisé, Optimisation directe des préférences (DPO), et RLVR en complément pour affiner les performances sur les tâches vérifiables. Chaque étape s'appuie sur la précédente. C'est pourquoi Tulu 3, sur la base Llama 8B, obtient des résultats surpassant les versions instruct de Llama 3.1, Qwen 2.5, Mistral, et même des modèles fermés comme GPT-4o-mini et Claude 3.5 Haiku.

Pour EliteIntel, l'étape de classification des commandes est une tâche de suivi d'instructions avec des réponses correctes vérifiables (action JSON X vs. Y). C'est précisément le type de tâche que le RLVR optimise. Le modèle est entraîné spécifiquement pour une sortie structurée déterministe.

## Pourquoi la variante « Supernova »

La variante Supernova diffère du Tulu 3 standard. Tulu-3.1-8B-SuperNova est créé via une fusion linéaire de trois modèles : Llama-3.1-MedIT-SUN-8B (médecine/raisonnement), Llama-3.1-Tulu-3-8B (suivi d'instructions) et Llama-3.1-SuperNova-Lite (le modèle distillé d'Arcee), chacun contribuant à parts égales avec un poids de 1,0 en utilisant mergekit.

Le parent SuperNova-Lite est un modèle distillé d'une base Arcee plus grande, offrant une densité de connaissances supérieure à un modèle 8B standard. La fusion linéaire moyenne directement les tenseurs de poids, combinant les connaissances sans calcul d'entraînement supplémentaire. Cela produit des résultats particulièrement solides sur les tâches de suivi d'instructions, comme le démontre son score IFEval.

**Performances** : Le modèle utilise une architecture Llama 8B. À la quantification Q4_K_M sur une 3090 24 Go, il tient en VRAM aux côtés du jeu avec une marge. Cela évite le déchargement sur CPU et maintient un débit d'inférence maximal. Les modèles Qwen comparables utilisent des configurations de têtes d'attention différentes (comme le ratio GQA de Qwen2.5) qui peuvent s'exécuter plus lentement dans le backend GGUF de llama.cpp.

Il fonctionne également sur une carte de 12 Go de VRAM si aucun autre workload consommateur de VRAM n'est présent. Cela nécessite que le jeu soit exécuté sur un GPU ou une machine séparée.

## Puis-je utiliser un modèle différent ?

Des modèles alternatifs peuvent être utilisés, mais ils sont peu susceptibles d'égaler la vitesse et la précision de tulu3.1-supernova.

Les problèmes courants avec les modèles alternatifs incluent un format de réponse incorrect.
L'erreur la plus fréquente est que le modèle retourne un essai en balisage au lieu d'une action structurée ou d'un résultat d'analyse.

--- 

Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
