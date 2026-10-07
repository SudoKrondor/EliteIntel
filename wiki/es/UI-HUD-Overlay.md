# Overlay HUD

Un overlay siempre visible que pone en pantalla tu objetivo actual y tu conversación con Vega —
en la ventana del juego o dentro de un visor VR.

![Overlay HUD en el juego](images/ui-overlay-ingame.png)

El overlay se ejecuta en su propio proceso, así que no compite con el juego ni con la app por el
hilo de la interfaz.

La tarjeta se dibuja siguiendo la geometría de la cabina en lugar de en ángulo recto con el
monitor, así que se inclina como los paneles de la propia nave en ese punto de la pantalla —
muévela y la inclinación cambia con ella. Sus filas son líneas inclinadas, por eso un valor puede
quedar bastante más bajo que su etiqueta: lee cada fila siguiendo la inclinación, como lees las
lecturas del juego a su lado. Cuánto parece caer un valor depende también del **TAMAÑO DEL
TEXTO** — la inclinación la fija la cabina, así que un texto más pequeño da filas más cortas y la
misma caída cruza más de ellas.

Actívalo con **MOSTRAR OVERLAY** en la [pestaña Vega](UI-Vega-Tab) y configúralo con **AJUSTES DE
OVERLAY** al lado. La app recuerda si lo dejaste activado y lo restaura en el siguiente arranque.

> Si falta el binario del overlay en la distribución, el interruptor lo indica en el registro de
> diagnósticos. No finge un overlay que no existe.

---

## Qué muestra

### Una tarjeta de objetivo

Solo cabe una tarjeta, así que el overlay muestra **lo más importante que estás haciendo ahora**
y cambia sola cuando eso cambia. No hay nada que configurar.

El trabajo que aceptaste siempre gana al que la app propone, en este orden:

| Rango | Tarjeta | Aparece cuando |
|------|------|--------------|
| 1 | **CONTRATO DE MASACRE** | Estás con misiones de masacre — bajas requeridas, pila, recompensa |
| 1 | **MISIÓN** | Has aceptado misiones — objetivo, carga o pasajeros, caducidad y recompensa de la destacada, más lo que vale el resto de la pila |
| 2 | **RUTA COMERCIAL** | Hay una ruta comercial trazada — mercancía, compra, venta, margen, tramo *n* de *m* |
| 2 | **OPORTUNIDAD DE CARGA** | Vega encontró un par compra/venta rentable para el espacio libre de tu viaje |
| 2 | **SITIO DE OBRA** | Transportas para una construcción de colonización — progreso, lo pendiente y qué cargar en el próximo viaje |
| 2 | **MERCANCIA HALLADA** / **LISTA DE COMPRA** / **VENDER CARGA** | Una búsqueda de mercancía encontró un mercado y hay una ruta trazada hasta él — qué comprar (o vender), existencias y precio |
| 3 | **MINERÍA** | Tienes objetivos de minería, una refinería instalada y no estás en supercrucero — bodega, drones, objetivos |
| 3 | **EXOBIOLOGÍA** | Quedan géneros por muestrear en este sistema — solo mientras *Anunciar descubrimientos* está activado |
| 3 | **CAZA RECOMPENSAS** | Estás en un sitio de extracción de recursos — tipo de sitio, recompensas en la bodega, bajas |
| 3 | **ZONA DE CONFLICTO** | Estás en una zona de conflicto — intensidad, tu bando, bonos de combate en mano |
| 3 | **RUTA TRAZADA** | Hay una ruta fijada — destino, siguiente sistema, saltos restantes |

La tarjeta de **ruta trazada** toma un título más concreto cuando Vega calculó el destino por ti y
la ruta sigue yendo allí: **COMERCIANTE DE MATERIALES**, **CORREDOR TECNOLÓGICO**, **FACTORES
INTERESTELARES**, **VISTA GENOMICS**, **REPOSTAJE** o **EQUIPAMIENTO**, con la estación y el tipo
añadidos. Trazar a otro sitio borra ese detalle, así que un encargo viejo nunca puede adueñarse de
la tarjeta.

La tarjeta de **misión** destaca la misión cuyo destino es el final de tu ruta trazada; si no, la
que caduca antes.

### La conversación

Bajo la tarjeta, el overlay va escribiendo el intercambio según ocurre — lo que dijiste, la
respuesta de Vega y el tráfico de radio —, cada uno con su color. Útil si juegas con la voz baja.

---

## Ajustes del overlay

![Ajustes del overlay](images/ui-overlay-settings.png)

**TRANSPARENCIA DEL FONDO** (0–100 %) y **TAMAÑO DEL TEXTO** (75–200 %) son dos controles
separados a propósito. Un único deslizador de «opacidad» desvanecería el texto junto con el
fondo, que es justo lo que hace ilegible un overlay atenuado sobre la superficie brillante de un
planeta. Atenúa el fondo; deja el texto en paz.

### Colores del texto

Un selector de color por función, para ajustar el overlay a los colores de tu cabina o a tu
vista:

**Título del objetivo** · **Correcto** · **Advertencia** · **Crítico** · **Etiquetas** · **Tus
palabras** · **IA de la nave** · **Tráfico de radio**

**Restablecer colores** devuelve cada color al que trae el overlay de serie.

### MOSTRAR EN

| Modo | Qué hace |
|------|--------------|
| **Monitor** | Una ventana de escritorio. El valor por defecto. La tarjeta se inclina según la cabina, y la inclinación cambia según dónde la coloques |
| **Visor VR** | Un overlay de SteamVR. Necesita SteamVR en marcha. Si no hay VR disponible, recurre a una ventana de escritorio, para que nunca te quedes sin nada |
| **Monitor y visor** | Ambos a la vez, con los mismos datos. Útil si vuelas en VR pero emites o grabas desde el monitor |
| **Ventana de captura VR** | Una ventana sencilla, plana y opaca para que la fije una herramienta de captura |

### Sobre la ventana de captura VR

Este modo **no** habla con SteamVR. Arranca tu herramienta de captura — Desktop+, OVR Toolkit o
Virtual Desktop — y elige la ventana llamada **«EliteIntel HUD (VR capture)»**.

Por qué existe: el modo SteamVR entrega al compositor una textura completa por cada carácter
escrito, y en un visor por streaming se ha reportado como un coste real de fotogramas. Una
herramienta de captura toma la ventana en la GPU a su propio ritmo, y te da controles de
colocación y curvatura que esta app no tiene.

Es un modo aparte en vez de «apunta tu herramienta a la ventana Monitor», porque esa ventana está
inclinada, es transparente y es una ventana de herramienta — y los selectores de captura las
filtran por completo.

### POSICIÓN EN EL VISOR

Ocho posiciones: **Arriba, Arriba a la derecha, A la derecha, Abajo a la derecha, Abajo, Abajo a
la izquierda, A la izquierda, Arriba a la izquierda.**

> **El HUD está fijo delante de tu asiento y no sigue tu cabeza.** La dirección que eliges se
> mide desde donde miras tras el *Reset Seated Position* de SteamVR — así que al recentrar la
> vista el HUD se mueve con la cabina, que es lo que quieres. Mira hacia otro lado y el HUD se
> queda donde lo dejaste, igual que un panel físico.

---

## Leerlo en otro idioma

Las etiquetas de las tarjetas siguen el idioma de la app, y los números se agrupan como lo hace
ese idioma. Los nombres que da el juego — sistemas, estaciones, mercancías — pasan sin cambios.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
