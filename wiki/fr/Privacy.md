# Politique de confidentialité d'EliteIntel

Cette politique explique quelles données sont traitées, comment elles sont utilisées, et quels choix sont disponibles.

*Dernière mise à jour : 25 octobre 2025*

## Vue d'ensemble

EliteIntel est une application open source disponible sur [GitHub](https://github.com/stone-alex/EliteIntel). Elle utilise la reconnaissance vocale (STT), la synthèse vocale (TTS) et des grands modèles de langage (LLM) pour traiter les données du jeu.

L'application peut fonctionner entièrement hors ligne en utilisant un STT local (NVIDIA Parakeet), un LLM local (Ollama) et un TTS local (Kokoro TTS). Aucune donnée ne quitte la machine dans cette configuration. Lorsque des services cloud sont utilisés, les données sont transmises comme décrit ci-dessous.

## Quelles données sont traitées ?

Aucune information personnelle n'est collectée, y compris les noms, adresses ou données de localisation. Les types de données suivants sont traités :

- **Clés API** : Utilisées pour authentifier les requêtes vers les services TTS et LLM cloud. Stockées de manière chiffrée dans une base de données SQLite locale. Transmises uniquement dans les en-têtes de requête aux services concernés (Google pour TTS ; xAI, OpenAI ou Anthropic pour LLM).

- **Données textuelles (TTS)** : Lors de l'utilisation de Google TTS, le texte de la réponse est envoyé à Google. Lors de l'utilisation de Kokoro TTS, aucune donnée ne quitte la machine.

- **Données de jeu (LLM)** : Les données de jeu pertinentes (détails des missions, données de marché, résultats de scan, etc.) sont envoyées au LLM configuré. Le nom du Commander n'est jamais transmis. L'IA vous désigne par le titre, le grade honorifique ou le surnom que vous avez configuré.

## Comment ces données sont-elles utilisées ?

- **Clés API** : Conservées dans la base de données locale et utilisées uniquement pour authentifier les requêtes vers des services tiers.

- **Audio et texte** : Envoyés à Google pour le traitement TTS uniquement. Google traite les données conformément à sa propre politique de confidentialité. La conservation des données à des fins d'amélioration du service est désactivée par défaut dans l'utilisation standard de l'API.

- **Données de jeu** : L'application ne diffuse pas en continu tous les événements du jeu vers le LLM. Elle collecte et stocke localement les données pertinentes, puis envoie des extraits ciblés lorsqu'une commande ou une requête est émise. Le LLM n'a pas d'accès permanent aux données du jeu.

## Où vont les données ?

- **Google (TTS)** : Le texte est envoyé aux services Google Cloud. Régi par la [Politique de confidentialité de Google](https://policies.google.com/privacy).

- **xAI / OpenAI / Anthropic (LLM)** : Les extraits de données de jeu sont envoyés au LLM cloud configuré. Régis par leurs politiques de confidentialité respectives.

- **Nulle part ailleurs** : Aucune donnée n'est stockée en externe, vendue ou partagée avec des tiers. Le code source complet est disponible sur [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel).

## Droits et choix

- **Inspecter le code** : Le code source complet est disponible sur GitHub.
- **Passer entièrement hors ligne** : Utilisez Parakeet, Ollama ou LM Studio, et Kokoro TTS. Aucune donnée n'est transmise dans cette configuration.
- **Configurer les fournisseurs** : Sélectionnez le LLM cloud à utiliser, le cas échéant. Les clés API sont gérées dans la base de données locale.
- **Supprimer vos données** : Aucune donnée n'est stockée en externe. Pour les données détenues par Google, xAI, OpenAI ou Anthropic, référez-vous à leurs politiques respectives.

## Sécurité

Les clés API sont stockées de manière chiffrée dans une base de données locale et transmises uniquement dans les en-têtes de requête. L'application respecte les Conditions d'utilisation d'Elite Dangerous. Elle ne lit pas la mémoire du jeu et n'utilise pas d'overlays. Le code source ouvert permet la révision par la communauté.

## Modifications de cette politique

Les mises à jour de la politique sont mentionnées dans le dépôt GitHub et peuvent apparaître dans l'application. Suivez [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel) pour les changements.

## Questions

Ouvrez un ticket sur GitHub ou contactez via Matrix.

----
Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
