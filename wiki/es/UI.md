# La interfaz de Elite Intel

Elite Intel se organiza en siete pestañas en la parte superior de la ventana. Cada una se ocupa
de una parte distinta del sistema, y la mayoría contiene subpestañas propias.

Esta sección recorre cada pestaña, cada control y lo que realmente hace.

---

## Las siete pestañas

| Pestaña | Para qué sirve |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | La cubierta de vuelo. Arrancar y detener servicios, seguir la conversación, leer el estado en vivo, abrir el overlay HUD del juego. |
| <img src="images/commander.png" class="inline" height="20" alt="Comandante"> **[Comandante](UI-Commander-Tab)** | Quién eres y cómo se comportan tus naves. La carpeta del journal, voces y personalidades de la flota, automatizaciones, anuncios hablados, el catálogo de Exo-Maestría y tus marcadores. |
| <img src="images/actions.png" class="inline" height="20" alt="Acciones"> **[Acciones](UI-Actions-Tab)** | Todo lo que Elite Intel puede hacer. Explora el catálogo de comandos integrados y crea tus propias macros. |
| <img src="images/keyboard.png" class="inline" height="20" alt="Bindings"> **[Bindings](UI-Bindings-Tab)** | **BindForge.** Edita cualquier control de Elite Dangerous en cualquier dispositivo, pon nombre a tus mandos y haz copias de todos tus archivos de controles. |
| <img src="images/services.png" class="inline" height="20" alt="Servicios"> **[Servicios](UI-Settings-Tab)** | La fontanería. Idioma, modelo de lenguaje, motor de voz, audio, push-to-talk y una copia de tus ajustes y datos. |
| <img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> **[Jukebox](UI-Jukebox-Tab)** | Tu propia música, sonando por debajo de Vega y atenuada automáticamente cuando ella habla. |
| <img src="images/stats.png" class="inline" height="20" alt="Estadísticas"> **[Estadísticas](UI-Stats-Tab)** | Uso de tokens y telemetría del LLM de la sesión actual. |

Además está el **[Overlay HUD](UI-HUD-Overlay)** — una ventana independiente siempre visible (y
una superficie VR opcional) controlada desde la pestaña Vega.

---

## Si es tu primer arranque

Elite Intel pronuncia en voz alta sus avisos de configuración al arrancar los servicios, así que
no tienes que buscar qué falta. Por orden de importancia:

1. **Un modelo de lenguaje.** Sin él no funciona nada. Ve a
   [Servicios → Servicios de IA](UI-Settings-Tab) y elige un proveedor en la nube y pega su clave
   API, o apunta la app a un modelo local. Consulta [Elige tu LLM](installing-local-llms).
2. **La carpeta del journal.** Sin ella Elite Intel no ve nada de lo que ocurre alrededor de tu
   nave. [Comandante → Perfil del comandante](UI-Commander-Tab).
3. **Tus controles.** Sin un archivo de bindings, Elite Intel no puede manejar tu nave. Abre una vez la
   [pestaña Bindings](UI-Bindings-Tab) para que BindForge se configure, y pulsa **Autoasignar bindings
   requeridos por VEGA** para darle a Vega las teclas que necesita. Si Vega dice que tus archivos de
   controles aún no existen, completa tu entrenamiento de comandante y luego, en *Opciones → Controles*
   del juego, pon el movimiento de la interfaz del mapa galáctico en las teclas de flecha y aplica.

4. **Calibrar el audio.** Muy recomendable antes del primer vuelo.
   [Pestaña Vega](UI-Vega-Tab) → **Calibrar audio**.

> Elite Intel está hecho para **Elite Dangerous Odyssey**. En Horizons, Vega te avisa al arrancar
> de que gran parte no funcionará.

---

## Convenciones que valen en todas partes

- **La mayoría de los controles guardan al instante.** Interruptores, deslizadores y listas se
  aplican en cuanto los cambias; no hay botón Guardar que olvidar.
- **Dos excepciones esperan por ti.** *Servicios → Servicios de IA* guarda tus cambios hasta que pulses
  **Guardar**, y si intentas salir con cambios sin guardar te pregunta *Guardar*, *Descartar* o *Seguir
  editando*. Los cambios de la pestaña Bindings esperan hasta que pulses **Aplicar**, y se hace una copia
  de todo antes de que lleguen a Elite Dangerous.
- **Cambiar de idioma reconstruye la ventana.** Al elegir otro idioma en la pestaña *Servicios*, todas las
  pestañas se redibujan en ese idioma al momento, y Vega anuncia el cambio.
- **Se admiten nueve idiomas:** inglés, ruso, ucraniano, alemán, francés, español, italiano, portugués y
  portugués de Brasil.

- **Varios comandantes en un mismo PC.** Elite Intel guarda los datos de cada comandante por
  separado y cambia automáticamente cuando otro comandante entra en el juego — la lista de flota
  y los ajustes por comandante le siguen.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
