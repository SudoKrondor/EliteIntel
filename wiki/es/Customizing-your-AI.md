# Personalizar Elite Intel

Elite Intel funciona como la voz y el cerebro de IA de la nave. Con Kokoro TTS como motor de síntesis de voz predeterminado integrado y la mayoría de los usuarios funcionando completamente sin conexión (Kokoro TTS y un LLM local mediante Ollama), esta guía explica cómo personalizar la voz y los ajustes de personalidad.

## Voces

Kokoro TTS es el motor TTS predeterminado. Funciona sin conexión y sin dependencia de la nube. Voces disponibles:

- **Mujer americana**: Heart, Bella, Nicole (susurro), Sky, Anna
- **Hombre americano**: Michael
- **Mujer británica**: Isabella, Emma
- **Hombre británico**: George, Jason, Daniel

Google TTS (nube) también está disponible cuando está habilitado. Su conjunto de voces es independiente del de Kokoro TTS.

**Nota**: A partir de la versión 0351, la voz de la nave, la personalidad y la cadencia solo pueden cambiarse desde la interfaz de usuario, no mediante comandos de voz. La IA selecciona entre el motor activo (Kokoro TTS por defecto).

## Identidad de la nave

La IA habla como la nave. Usa "yo", "mi" y "mí" cuando se refiere a sí misma.

Ejemplos:
- "¿Cuál es tu equipamiento?" devuelve los módulos actuales de la nave.
- "¿Cuál es tu alcance de salto?" devuelve el alcance actual con y sin carga.
- "¿Cuánto combustible tengo?" devuelve el nivel de combustible de la nave.

Para consultar información del Portaflota, dirígete al Portaflota explícitamente:
- "¿Cuál es el alcance del Portaflota?"
- "Cuéntame sobre el alcance de salto del Portaflota."

De lo contrario, la IA asume que la consulta se refiere a la nave.

## Personalidades, perfiles y cadencia

El **modo sin conexión (Kokoro TTS + LLM local)** no admite personalidades personalizadas, perfiles de facción (Imperial/Federal/Alianza) ni configuraciones de cadencia especiales. El modelo local responde con su estilo de entrenamiento predeterminado.

Los **usuarios de LLM en la nube** (Claude, Grok, OpenAI, etc.) tienen acceso al conjunto completo de funciones:
- Personalidades Profesional / Amigable / Descontrolado / Rebelde
- Perfiles Imperial / Federal / Alianza (afecta el tono y la fraseología)
- Control de temperatura (baja = rápido y conciso, alta = creativo y más lento)

**Nota**: A partir de la versión 0351, la personalidad y la cadencia solo pueden cambiarse desde la interfaz de usuario.

El modo sin conexión ofrece baja latencia sin coste de API, con respuestas sencillas. Los LLM en la nube ofrecen opciones adicionales de personalidad.

## Consejos

- Comienza con los valores predeterminados de Kokoro TTS. Prueba diferentes voces para encontrar la que prefieras. Las voces masculinas británicas (George, Jason, Daniel) complementan una estética imperial incluso sin compatibilidad con perfiles.
- El modo sin conexión es rápido y gratuito. El modo en la nube ofrece opciones adicionales de personalidad pero genera costes de API.
- Experimentar con la formulación puede mejorar la experiencia con las consultas en que la nave habla en primera persona.

Reporta problemas, comportamientos inesperados o sugerencias en Matrix.

Comunidad 👉 [**Matrix**](https://matrix.to/#/#krondor:matrix.org) 👈
