# La interfaz de Elite Intel

Elite Intel V1.1 se organiza en seis pestañas en la parte superior de la ventana. Cada una se
ocupa de una parte distinta del sistema y la mayoría contiene sus propias subpestañas.

Esta sección recorre cada pestaña, cada control y lo que hace realmente.

---

## Las seis pestañas

| Pestaña | Para qué sirve |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | El puente de mando. Iniciar y detener servicios, seguir la conversación, leer el estado en vivo y abrir el overlay HUD dentro del juego. |
| <img src="images/controller.png" class="inline" height="20" alt="Comandante"> **[Comandante](UI-Commander-Tab)** | Quién eres y cómo se comportan tus naves. Automatizaciones, anuncios hablados, y voz y personalidad por nave. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Acciones"> **[Acciones](UI-Actions-Tab)** | Todo lo que Elite Intel puede hacer. Explorar el catálogo de comandos integrados y crear tus propias macros. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> **[Bindings](UI-Bindings-Tab)** | Tus asignaciones de teclas de Elite Dangerous. Detectar huecos y conflictos, editarlas y devolverlas al juego. |
| <img src="images/settings.png" class="inline" height="20" alt="Ajustes"> **[Ajustes](UI-Settings-Tab)** | La fontanería. Idioma, carpeta del diario, modelo de lenguaje, motor de voz, audio y pulsar para hablar. |
| <img src="images/stats.png" class="inline" height="20" alt="Estadísticas"> **[Estadísticas](UI-Stats-Tab)** | Uso de tokens y telemetría del LLM de la sesión actual. |

También está el **[overlay HUD](UI-HUD-Overlay)**: una ventana independiente siempre visible (y
opcionalmente una superficie VR) que se controla desde la pestaña Vega.

---

## Si es tu primera vez

Elite Intel dice en voz alta sus avisos de configuración al arrancar los servicios, así que no
tienes que buscar qué falta. Por orden de importancia:

1. **Un modelo de lenguaje.** Sin él no funciona nada. Ve a
   [Ajustes → Servicios de IA](UI-Settings-Tab) y pega una clave de API en la nube o apunta la
   aplicación a un modelo local. Consulta [Elegir tu LLM](installing-local-llms).
2. **La carpeta del diario.** Sin ella Elite Intel está ciego a todo lo que ocurre alrededor de
   tu nave. [Ajustes → Común](UI-Settings-Tab).
3. **La carpeta de asignaciones.** Sin ella Elite Intel no puede pilotar tu nave.
   [Bindings → Perfil de asignaciones](UI-Bindings-Tab).
4. **Calibrar el audio.** Muy recomendable antes del primer vuelo.
   [Pestaña Vega](UI-Vega-Tab) → **CALIBRAR AUDIO**.

---

## Convenciones que se aplican en todas partes

- **La ventana no recuerda nada que no hayas guardado.** Solo la pestaña *Ajustes → Servicios de
  IA* trabaja sobre un borrador: muestra el aviso **Cambios sin guardar** y no te deja salir de la
  pestaña sin decidir. Cualquier otro interruptor o deslizador de la aplicación se escribe en el
  momento en que lo cambias.
- **A los bindings se les aplica un modelo de borrador aparte.** Las ediciones van primero a un
  borrador y solo se escriben en Elite Dangerous al pulsar **Aplicar al juego**.
- **Cambiar de idioma reconstruye la ventana.** Seleccionar un idioma nuevo en *Ajustes → Común*
  vuelve a renderizar cada pestaña en ese idioma de inmediato, y Vega anuncia el cambio.
- **Se admiten nueve idiomas:** inglés, español, francés, alemán, italiano, portugués, portugués
  de Brasil, ucraniano y ruso.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
