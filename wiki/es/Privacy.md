# Política de privacidad de EliteIntel

Esta política explica qué datos se manejan, cómo se utilizan y qué opciones están disponibles.

*Última actualización: 25 de octubre de 2025*

## Resumen

EliteIntel es una aplicación de código abierto disponible en [GitHub](https://github.com/stone-alex/EliteIntel). Utiliza reconocimiento de voz (STT), síntesis de voz (TTS) y modelos de lenguaje a gran escala (LLM) para procesar datos del juego.

La app puede ejecutarse completamente sin conexión usando STT local (NVIDIA Parakeet), LLM local (Ollama) y TTS local (Kokoro TTS). En esta configuración, ningún dato abandona la máquina. Cuando se utilizan servicios en la nube, los datos se transmiten tal como se describe a continuación.

## ¿Qué datos se manejan?

No se recopila información personal, incluyendo nombres, direcciones o datos de ubicación. Los siguientes tipos de datos son manejados:

- **Claves API**: Se utilizan para autenticar solicitudes a los servicios de TTS y LLM en la nube. Se almacenan cifradas en una base de datos SQLite local. Se transmiten únicamente en los encabezados de las solicitudes a los servicios correspondientes (Google para TTS; xAI, OpenAI o Anthropic para LLM).

- **Datos de texto (TTS)**: Al usar Google TTS, el texto de respuesta se envía a Google. Al usar Kokoro TTS, ningún dato abandona la máquina.

- **Datos del juego (LLM)**: Los datos relevantes del juego (detalles de misiones, datos de mercado, resultados de escaneos, etc.) se envían al LLM configurado. El nombre del Comandante nunca se transmite. La IA te dirige por el título, tratamiento o apodo que hayas configurado.

## ¿Cómo se usan estos datos?

- **Claves API**: Se conservan en la base de datos local y se usan únicamente para autenticar solicitudes a servicios de terceros.

- **Audio y texto**: Se envían a Google solo para el procesamiento de TTS. Google procesa los datos según su propia política de privacidad. La retención de datos para mejoras del servicio está desactivada por defecto en el uso estándar de la API.

- **Datos del juego**: La app no transmite todos los eventos del juego al LLM. Recopila y almacena los datos relevantes localmente y luego envía extractos específicos cuando se emite un comando o consulta. El LLM no tiene acceso persistente a los datos del juego.

## ¿Adónde van los datos?

- **Google (TTS)**: El texto se envía a los servicios en la nube de Google. Está regido por la [Política de privacidad de Google](https://policies.google.com/privacy).

- **xAI / OpenAI / Anthropic (LLM)**: Los extractos de datos del juego se envían al LLM en la nube que esté configurado. Están regidos por sus respectivas políticas de privacidad.

- **A ningún otro lugar**: Ningún dato se almacena externamente, se vende ni se comparte con terceros. El código fuente completo está disponible en [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel).

## Derechos y opciones

- **Inspeccionar el código**: El código fuente completo está disponible en GitHub.
- **Modo completamente sin conexión**: Usa Parakeet, Ollama o LM Studio, y Kokoro TTS. En esta configuración no se transmite ningún dato.
- **Configurar proveedores**: Selecciona qué LLM en la nube usar, si lo hay. Las claves API se gestionan en la base de datos local.
- **Eliminar tus datos**: No se almacenan datos externamente. Para los datos en poder de Google, xAI, OpenAI o Anthropic, consulta sus respectivas políticas.

## Seguridad

Las claves API se almacenan cifradas en una base de datos local y se transmiten únicamente en los encabezados de las solicitudes. La app cumple con los Términos de servicio de Elite Dangerous. No lee la memoria del juego ni usa superposiciones. El código fuente abierto permite la revisión por parte de la comunidad.

## Cambios en esta política

Las actualizaciones de la política se indican en el repositorio de GitHub y pueden aparecer en la aplicación. Consulta [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel) para ver los cambios.

## Preguntas

Abre un issue en GitHub o contacta a través de Matrix.

----
Comunidad 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
