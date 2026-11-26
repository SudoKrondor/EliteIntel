# Politique de confidentialité d'EliteIntel

Cette politique explique quelles données sont traitées, comment elles sont utilisées, et quels choix sont disponibles.

*Dernière mise à jour : 14 novembre 2026*

## Vue d'ensemble

EliteIntel est une application open source disponible sur [GitHub](https://github.com/stone-alex/EliteIntel). Elle utilise la reconnaissance vocale (STT), la synthèse vocale (TTS) et des grands modèles de langage (LLM) pour traiter les données du jeu.

L'application peut fonctionner entièrement hors ligne en utilisant un STT local (NVIDIA Parakeet), un LLM local (LM Studio) et un TTS local (Supertonic TTS). Dans cette configuration, rien ne quitte la machine à part le décompte quotidien des installations décrit ci-dessous. Lorsque des services cloud sont utilisés, les données sont transmises comme décrit ci-dessous.

## Quelles données sont traitées ?

Aucune information personnelle n'est collectée, y compris les noms, adresses ou données de localisation. Les types de données suivants sont traités :

- **Clés API** : Utilisées pour authentifier les requêtes vers les services TTS et LLM cloud. Stockées de manière chiffrée dans une base de données SQLite locale. Transmises uniquement dans les en-têtes de requête aux services concernés (Google pour TTS ; xAI, OpenAI ou Anthropic pour LLM).

- **Données textuelles (TTS)** : Lors de l'utilisation de Google TTS, le texte de la réponse est envoyé à Google. Lors de l'utilisation de Supertonic TTS, aucune donnée ne quitte la machine.

- **Données de jeu (LLM)** : Les données de jeu pertinentes (détails des missions, données de marché, résultats de scan, etc.) sont envoyées au LLM configuré. Le nom du Commander n'est jamais transmis. L'IA vous désigne par le titre, le grade honorifique ou le surnom que vous avez configuré.

- **Statistiques d'installation** : Une fois par jour, l'application envoie son numéro de version et son système d'exploitation (Windows ou Linux) à Google Firebase, sous un identifiant anonyme aléatoire créé pour cette installation. Rien d'autre n'est envoyé : ni nom de Commander, ni données de jeu, ni réglages. Cela sert uniquement à compter combien d'installations sont actives, et avec quelle version et quel système d'exploitation.

- **Ensembles de massacre (sur consentement)** : Uniquement lorsque le partage est activé (onglet Commandant, page Ensembles de massacre, désactivé par défaut), l'application envoie les ensembles de missions de massacre de pirates qu'elle a appris des journaux : noms des systèmes stellaires, des factions et des stations, nombre de sites d'extraction de ressources, et le jour où un ensemble a été vu pour la dernière fois. Aucun nom de Commander, identifiant de mission, récompense ni rien d'autre permettant d'identifier un joueur n'est envoyé. Les ensembles de toutes les installations sont vérifiés et publiés sur [elite-intel.org/data/massacre-sets.json](https://www.elite-intel.org/data/massacre-sets.json), sans indiquer qui les a signalés.

## Comment ces données sont-elles utilisées ?

- **Clés API** : Conservées dans la base de données locale et utilisées uniquement pour authentifier les requêtes vers des services tiers.

- **Audio et texte** : Envoyés à Google pour le traitement TTS uniquement. Google traite les données conformément à sa propre politique de confidentialité. La conservation des données à des fins d'amélioration du service est désactivée par défaut dans l'utilisation standard de l'API.

- **Données de jeu** : L'application ne diffuse pas en continu tous les événements du jeu vers le LLM. Elle collecte et stocke localement les données pertinentes, puis envoie des extraits ciblés lorsqu'une commande ou une requête est émise. Le LLM n'a pas d'accès permanent aux données du jeu.

## Où vont les données ?

- **Google (TTS)** : Le texte est envoyé aux services Google Cloud. Régi par la [Politique de confidentialité de Google](https://policies.google.com/privacy).

- **xAI / OpenAI / Anthropic (LLM)** : Les extraits de données de jeu sont envoyés au LLM cloud configuré. Régis par leurs politiques de confidentialité respectives.

- **Google Firebase (statistiques d'installation et ensembles de massacre)** : Stockés dans Google Cloud Firestore sous l'identifiant anonyme de l'installation. Chaque installation ne peut écrire que ses propres enregistrements, et rien ne peut être relu par l'application. Une tâche nocturne gérée par les mainteneurs d'EliteIntel les lit pour publier les décomptes combinés et les ensembles vérifiés. Comme pour toute requête internet, Google voit l'adresse IP de connexion, régie par la [Politique de confidentialité de Google](https://policies.google.com/privacy). EliteIntel ne la conserve pas.

- **Nulle part ailleurs** : Aucune donnée n'est vendue ou partagée avec des tiers. Le code source complet est disponible sur [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel).

## Droits et choix

- **Inspecter le code** : Le code source complet est disponible sur GitHub.
- **Passer entièrement hors ligne** : Utilisez Parakeet, LM Studio et Supertonic TTS. À part le décompte quotidien des installations, aucune donnée n'est transmise dans cette configuration.
- **Configurer les fournisseurs** : Sélectionnez le LLM cloud à utiliser, le cas échéant. Les clés API sont gérées dans la base de données locale.
- **Supprimer vos données** : Désactiver le partage des ensembles de massacre supprime de Firebase les ensembles de cette installation. Les statistiques d'installation ne contiennent aucune donnée personnelle. Pour les données détenues par Google, xAI, OpenAI ou Anthropic, référez-vous à leurs politiques respectives.

## Sécurité

Les clés API sont stockées de manière chiffrée dans une base de données locale et transmises uniquement dans les en-têtes de requête. L'application respecte les Conditions d'utilisation d'Elite Dangerous. Elle ne lit pas la mémoire du jeu et n'utilise pas d'overlays. Le code source ouvert permet la révision par la communauté.

## Modifications de cette politique

Les mises à jour de la politique sont mentionnées dans le dépôt GitHub et peuvent apparaître dans l'application. Suivez [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel) pour les changements.

## Questions

Ouvrez un ticket sur GitHub ou contactez via Matrix.

----
Communauté 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
