# Personnaliser Elite Intel

Elite Intel fonctionne comme la voix et le cerveau IA du vaisseau. Avec Kokoro TTS comme moteur de synthèse vocale par défaut et la plupart des utilisateurs fonctionnant entièrement hors ligne (Kokoro TTS et un LLM local via Ollama), ce guide explique comment personnaliser les paramètres de voix et de personnalité.

## Voix

Kokoro TTS est le moteur de synthèse vocale par défaut. Il fonctionne hors ligne sans dépendance au cloud. Voix disponibles :

- **Américaine féminine** : Heart, Bella, Nicole (chuchotement), Sky, Anna
- **Américaine masculine** : Michael
- **Britannique féminine** : Isabella, Emma
- **Britannique masculine** : George, Jason, Daniel

Google TTS (cloud) est également disponible lorsqu'il est activé. Son ensemble de voix est distinct de celui de Kokoro TTS.

**Note** : À partir de la version 0351, la voix du vaisseau, la personnalité et la cadence ne peuvent être modifiées que dans l'interface, pas par commande vocale. L'IA sélectionne parmi le moteur actif (Kokoro TTS par défaut).

## Identité du vaisseau

L'IA parle en tant que vaisseau. Elle utilise "je", "mon" et "moi" pour se désigner elle-même.

Exemples :
- "Quelle est votre configuration ?" retourne les modules actuels du vaisseau.
- "Quelle est votre portée de saut ?" retourne la portée actuelle chargée et déchargée.
- "Combien de carburant ai-je ?" retourne le niveau de carburant du vaisseau.

Pour interroger les informations du Fleet Carrier à la place, adressez-vous explicitement au carrier :
- "Quelle est la portée du Fleet Carrier ?"
- "Parlez-moi de la portée de saut du carrier."

Sinon, l'IA suppose que la requête concerne le vaisseau.

## Personnalités, profils et cadence

**Mode hors ligne (Kokoro TTS + LLM local)** ne prend pas en charge les personnalités personnalisées, les profils de faction (Impérial/Fédéral/Alliance), ou les paramètres de cadence spéciaux. Le modèle local répond dans son style d'entraînement par défaut.

**Utilisateurs de LLM cloud** (Claude, Grok, OpenAI, etc.) ont accès à l'ensemble des fonctionnalités :
- Personnalités Professionnelle / Amicale / Déjantée / Rebelle
- Profils Impérial / Fédéral / Alliance (affecte le ton et la formulation)
- Contrôle de la température (bas = rapide et concis, haut = créatif et plus lent)

**Note** : À partir de la version 0351, la personnalité et la cadence ne peuvent être modifiées que dans l'interface.

Le mode hors ligne offre une faible latence sans coût API, avec des réponses simples. Les LLMs cloud offrent des options de personnalité supplémentaires.

## Conseils

- Commencez avec les paramètres par défaut de Kokoro TTS. Essayez différentes voix pour trouver votre préférence. Les voix masculines britanniques (George, Jason, Daniel) s'accordent bien avec une esthétique impériale même sans prise en charge des profils.
- Le mode hors ligne est rapide et gratuit. Le mode cloud offre des options de personnalité supplémentaires mais entraîne des coûts API.
- Expérimenter avec les formulations peut améliorer l'expérience pour les requêtes en mode vaisseau-narrateur.

Signalez les problèmes, comportements inattendus ou suggestions sur Matrix.

Communauté 👉 [**Matrix**](https://matrix.to/#/#krondor:matrix.org) 👈
