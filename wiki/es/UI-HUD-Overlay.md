# Overlay HUD

**Nuevo en V1.1.** Un overlay siempre visible que pone tus objetivos actuales en pantalla, en la
ventana del juego o dentro de un visor VR.

![Overlay HUD en el juego](images/ui-overlay-ingame.png)

Sustituye a la antigua ventana de overlay para OBS. El overlay corre fuera del proceso, así que no
compite con el juego ni con la aplicación por el hilo de interfaz.

La tarjeta se dibuja según la geometría de la cabina, no en ángulo recto con tu monitor, así que se
inclina como se inclinan los propios paneles de la nave en ese punto de la pantalla: si la mueves,
la inclinación cambia en consecuencia. Sus filas son líneas inclinadas, y por eso un valor puede
quedar bastante más abajo que la etiqueta a la que pertenece: lee cada fila siguiendo la
inclinación, igual que lees las lecturas del propio juego que tiene al lado. Cuánto parece caer un
valor depende del **TAMAÑO DEL TEXTO** además de la colocación: la inclinación la fija la cabina,
así que un texto más pequeño implica filas más cortas y la misma caída cruza más de ellas.

Actívalo con **MOSTRAR OVERLAY** en la [pestaña Vega](UI-Vega-Tab) y configúralo con **AJUSTES DE
OVERLAY**, al lado.

> Si falta el binario del overlay en la distribución, el interruptor se queda apagado y lo dice en
> el registro de diagnóstico. No fingirá un overlay que no existe.

---

## Qué muestra

El overlay dibuja **tarjetas**: una por objetivo activo, derivada de lo que estás haciendo
realmente. Las tarjetas aparecen y desaparecen solas; no hay nada que configurar.

| Tarjeta | Aparece cuando |
|------|--------------|
| **EXOBIOLOGÍA** | Estás muestreando orgánicos: género y qué queda por encontrar |
| **CONTRATO DE MASACRE** | Estás haciendo misiones de masacre: bajas necesarias, pila, recompensa |
| **MINERÍA** | Estás minando: bodega, drones, mercancía objetivo |
| **RUTA COMERCIAL** | Hay una ruta comercial trazada: mercancía, compra, venta, margen, tramo *n* de *m* |
| **OPORTUNIDAD DE CARGA** | Se ha detectado una carga rentable para lo que llevas |
| **MISIÓN** | Una misión destacada: objetivo, carga o pasajeros, caducidad, recompensa |
| **RUTA TRAZADA** | Hay una ruta fijada: destino, siguiente sistema, saltos restantes |
| **COMERCIANTE DE MATERIALES** · **CORREDOR TECNOLÓGICO** · **FACTORES INTERESTELARES** · **VISTA GENOMICS** | Has puesto un recordatorio de destino para visitar uno |

La tarjeta de misión destacada elige su misión como lo harías tú: primero la que está en el destino
de tu ruta, luego una de tu sistema actual, y después la aceptada más recientemente.

---

## Ajustes del overlay

![Ajustes del overlay](images/ui-overlay-settings.png)

**TRANSPARENCIA DEL FONDO** (0–100 %) y **TAMAÑO DEL TEXTO** (75–200 %) son dos controles separados
a propósito. Un único deslizador de «opacidad» atenuaría el texto junto con el fondo, que es
justamente lo que vuelve ilegible un overlay atenuado sobre una superficie planetaria brillante.
Atenúa el fondo; deja el texto en paz.

### MOSTRAR EN

| Modo | Qué hace |
|------|--------------|
| **Monitor** | Una ventana de escritorio. Lo predeterminado, y lo que hacía toda versión anterior a V1.1. La tarjeta se inclina para acompañar a la cabina, y la inclinación cambia con la colocación: véase arriba |
| **Visor VR** | Un overlay de SteamVR. Requiere SteamVR en marcha. Si la VR no está disponible, recae en una ventana de escritorio, así que nunca te quedas sin nada |
| **Monitor y visor** | Ambos a la vez, alimentados con datos idénticos. Útil si vuelas en VR pero emites o grabas desde el monitor |
| **Ventana de captura VR** | Una ventana lisa, plana y opaca para que una herramienta de captura la fije |

### Sobre la ventana de captura VR

Este modo **no** habla con SteamVR. Arranca tu herramienta de captura —Desktop+, OVR Toolkit o
Virtual Desktop— y elige la ventana llamada **«EliteIntel HUD (VR capture)»**.

Por qué existe: el modo SteamVR entrega al compositor una textura completa por cada carácter
escrito, y en un visor por streaming eso se ha reportado como un coste real de tasa de fotogramas.
Una herramienta de captura toma la ventana en la GPU a su propio ritmo, y te da controles de
colocación y curvatura que esta aplicación no tiene.

Es un modo aparte en lugar de «apunta tu herramienta de captura a la ventana de Monitor» porque esa
ventana se inclina, es translúcida y es una ventana de herramienta, y los selectores de captura las
filtran por completo.

### POSICIÓN EN EL VISOR

Ocho colocaciones: **Arriba, Arriba a la derecha, A la derecha, Abajo a la derecha, Abajo, Abajo a
la izquierda, A la izquierda, Arriba a la izquierda.**

> **El HUD está fijo delante de tu asiento y no sigue tu cabeza.** La dirección que elijas se mide
> desde donde miras tras el *Restablecer posición sentada* de SteamVR, así que recentrar la vista
> mueve el HUD junto con la cabina, que es lo que quieres. Si miras a otro lado, el HUD se queda
> donde lo dejaste, exactamente como un panel físico.

---

## Leerlo en otro idioma

Las etiquetas de las tarjetas siguen el idioma de la aplicación, y los números se agrupan como los
agrupa ese idioma. Los nombres que proporciona el juego —sistemas, estaciones, mercancías— pasan sin
tocarse.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
