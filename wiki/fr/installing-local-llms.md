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
| **Modèle requis**             | `google/gemma-4-e4b`                        | `google/gemma-4-e4b`                                                                                         |
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

Le développeur utilise LM Studio avec `google/gemma-4-e4b` (~6,3 Go). Le même modèle sous Ollama
est nettement plus lent. D'autres modèles peuvent fonctionner, sans garantie. Signalez vos retours
de compatibilité sur Matrix.

## Pourquoi `google/gemma-4-e4b` en particulier ?

Elite Intel est un analyseur de commandes et un outil d'analyse de données, pas un chatbot
conversationnel. Cela impose des exigences précises au modèle. Produire une conversation naturelle
ne suffit pas. Le modèle doit déduire correctement les actions à partir de la voix, effectuer une
analyse de données structurée et renvoyer les résultats sous forme de données structurées, pas d'un
essai en markdown ou en HTML. Tous les modèles de cette taille n'y parviennent pas de façon fiable.

L'exigence incontournable est le **function calling**. Le compagnon d'Elite Intel ne demande pas au
modèle de décrire ce qu'il ferait : il lui propose un ensemble d'outils et attend qu'il en appelle
un, avec des arguments. Un modèle incapable d'émettre un appel d'outil bien formé ne peut pas
piloter l'application du tout, aussi bien écrive-t-il. `google/gemma-4-e4b` le prend en charge.

Avec environ 6,3 Go, il tient en VRAM à côté du jeu sur une carte de 24 Go avec de la marge, ce qui
évite le déport vers le CPU et maintient le débit d'inférence.

> **À propos du modèle retiré de la V1.0.** Les versions précédentes recommandaient
> `tulu-3.1-8b-supernova`. Il ne prend pas en charge le function calling, ne peut donc pas exécuter
> le compagnon et n'est plus utilisable avec Elite Intel. Si vous suivez un guide ancien,
> ignorez-le et installez `google/gemma-4-e4b`.

## Puis-je utiliser un modèle différent ?

D'autres modèles peuvent être utilisés, mais ils doivent prendre en charge le function calling.
Sans cela, l'application ne peut rien exécuter.

L'échec le plus fréquent avec un modèle alternatif est un format de réponse incorrect : le modèle
renvoie de la prose décrivant une action au lieu d'appeler réellement l'outil.

--- 

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
