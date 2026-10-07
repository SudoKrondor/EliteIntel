# La interfaz de Elite Intel

Elite Intel se organiza en siete pestañas en la parte superior de la ventana. Cada una se ocupa
de una parte distinta del sistema, y la mayoría contiene subpestañas propias.

Esta sección recorre cada pestaña, cada control y lo que realmente hace.

---

## Las siete pestañas

| Pestaña | Para qué sirve |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | La cubierta de vuelo. Arrancar y detener servicios, seguir la conversación, leer el estado en vivo, abrir el overlay HUD del juego. |
| <img src="images/controller.png" class="inline" height="20" alt="Comandante"> **[Comandante](UI-Commander-Tab)** | Quién eres y cómo se comportan tus naves. Voces y personalidades de la flota, automatizaciones, anuncios hablados y el catálogo de Exo-Maestría. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Acciones"> **[Acciones](UI-Actions-Tab)** | Todo lo que Elite Intel puede hacer. Explora el catálogo de comandos integrados y crea tus propias macros. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> **[Bindings](UI-Bindings-Tab)** | Tus asignaciones de teclas de Elite Dangerous. Detecta huecos y conflictos, edítalas y escríbelas de vuelta en el juego. |
| <img src="images/settings.png" class="inline" height="20" alt="Ajustes"> **[Ajustes](UI-Settings-Tab)** | La fontanería. Idioma, carpeta del journal, modelo de lenguaje, motor de voz, audio y push-to-talk. |
| <img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> **[Jukebox](UI-Jukebox-Tab)** | Tu propia música, sonando por debajo de Vega y atenuada automáticamente cuando ella habla. |
| <img src="images/stats.png" class="inline" height="20" alt="Estadísticas"> **[Estadísticas](UI-Stats-Tab)** | Uso de tokens y telemetría del LLM de la sesión actual. |

Además está el **[Overlay HUD](UI-HUD-Overlay)** — una ventana independiente siempre visible (y
una superficie VR opcional) controlada desde la pestaña Vega.

---

## Si es tu primer arranque

Elite Intel pronuncia en voz alta sus avisos de configuración al arrancar los servicios, así que
no tienes que buscar qué falta. Por orden de importancia:

1. **Un modelo de lenguaje.** Sin él no funciona nada. Ve a
   [Ajustes → Servicios de IA](UI-Settings-Tab) y elige un proveedor en la nube y pega su clave
   API, o apunta la app a un modelo local. Consulta [Elige tu LLM](installing-local-llms).
2. **La carpeta del journal.** Sin ella Elite Intel no ve nada de lo que ocurre alrededor de tu
   nave. [Ajustes → General](UI-Settings-Tab).
3. **La carpeta de bindings.** Sin ella Elite Intel no puede manejar tu nave.
   [Bindings → Perfil de bindings](UI-Bindings-Tab). Si la carpeta es correcta pero Vega sigue
   sin encontrar tus bindings, abre *Opciones → Controles* en el juego y cambia cualquier
   asignación — el juego solo escribe un archivo de bindings cuando has personalizado algo.
4. **Calibrar el audio.** Muy recomendable antes del primer vuelo.
   [Pestaña Vega](UI-Vega-Tab) → **Calibrar audio**.

> Elite Intel está hecho para **Elite Dangerous Odyssey**. En Horizons, Vega te avisa al arrancar
> de que gran parte no funcionará.

---

## Convenciones que valen en todas partes

- **La mayoría de los controles guardan al instante.** Interruptores, deslizadores y listas se
  aplican en cuanto los cambias; no hay botón Guardar que olvidar.
- **Dos excepciones trabajan con un borrador.** *Ajustes → Servicios de IA* guarda tus cambios
  hasta que pulses **Guardar**, y si intentas salir con cambios sin guardar te pregunta *Guardar*,
  *Descartar* o *Seguir editando*. Las asignaciones de teclas se acumulan en un borrador que solo
  se escribe en Elite Dangerous al pulsar **Aplicar**.
- **Cambiar de idioma reconstruye la ventana.** Al elegir otro idioma en *Ajustes → General*,
  todas las pestañas se redibujan en ese idioma al momento, y Vega anuncia el cambio.
- **Se admiten nueve idiomas:** inglés, español, francés, alemán, italiano, portugués, portugués
  de Brasil, ucraniano y ruso.
- **Varios comandantes en un mismo PC.** Elite Intel guarda los datos de cada comandante por
  separado y cambia automáticamente cuando otro comandante entra en el juego — la lista de flota
  y los ajustes por comandante le siguen.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
