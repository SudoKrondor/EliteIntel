# BindForge: la pestaña Bindings

<img src="images/keyboard.png" class="inline" height="20" alt="Bindings"> **BindForge** edita y
protege tus controles de Elite Dangerous desde fuera del juego: teclado, ratón, joystick, HOTAS y
pedales. Se ocupa de todos los archivos que hay detrás: tus archivos `.binds`, el archivo de preset que
indica cuál se carga, y los archivos `DeviceMappings.xml` y `.buttonMap`, que dan nombre a tus mandos y
etiquetan sus botones.

Está en la pestaña **Bindings**, que tiene tres subpestañas y una barra encima de ellas:

| Parte | Para qué sirve |
|-------|----------------|
| **[Barra de Aplicar](#the-apply-bar)** | El único **Aplicar** y **Descartar** para todo lo de esta pestaña |
| **[Perfil de bindings](#binding-profile)** | El Bind Editor. Todos los controles del juego, con las vistas **Modo de juego**, **Anomalías** y **Ajustes** |
| **[Gestión de bindings](#binding-management)** | El File Manager. Tus instalaciones del juego, tus archivos de bindings y tus copias de seguridad |
| **[Diseñador de alias](#alias-designer)** | Da nombres reales a tus mandos y etiqueta sus botones, ejes y hats |

> **Vega sigue pulsando solo teclas del teclado.** Ahora puedes asignar cualquier dispositivo a
> cualquier ranura, pero un control necesita una tecla para que Vega pueda manejarlo. La columna
> **Acceso IA** te muestra cuáles la tienen.

---

## La primera vez que la abres

Antes de cambiar nada, BindForge se configura una vez. **Primero se hace una copia de seguridad de todo,
y esa copia se conserva para siempre.**

1. **Confirma tus instalaciones.** BindForge muestra cada copia de Elite Dangerous que encontró en este
   PC. Corrige la lista con **Añadir que falta** o **Quitar** y pulsa **Confirmar**.
2. **Concilia, si hace falta.** Si tus instalaciones tienen archivos de dispositivo distintos, usa los
   de una instalación para todas, o pulsa **Combinar** y responde cada fila marcada como *elegir*.
3. **Crea tu copia de trabajo.** BindForge hace su propia copia de tus archivos de controles. Esa es la
   copia que editas, y los archivos del juego no se tocan hasta que pulses **Aplicar**.
4. **Pon nombre a tus mandos.** A cada mando conectado sin nombre se le ofrece
   [un nombre](#naming-a-controller). Puedes **Omitir** este paso.

Hasta que termines, la configuración inicial se vuelve a ofrecer cada vez que abres la pestaña.

### ¿Nuevo en Elite Dangerous?

Un comandante totalmente nuevo no tiene archivos de controles, y Vega te lo dice. Completa tu
entrenamiento de comandante y luego, en *Opciones → Controles* del juego, pon el movimiento de la
interfaz del mapa galáctico en las teclas de flecha y aplica. Eso crea los archivos y Vega ya puede
leerlos.

Si tienes los archivos del juego pero ningún archivo de bindings propio, aparece un botón **Crear** en
la barra de Aplicar y las pestañas del editor quedan en gris. Elige en **A partir de** uno de los presets
de Frontier, ponle un **Nombre** al tuyo y pulsa **Aplicar** para escribirlo en el juego. Si vuelves al
juego, restaura en su lugar una copia desde [Copias del jugador](#player-backups).

---

<a id="the-apply-bar"></a>

## La barra de Aplicar

Nada de lo que hagas en estas pestañas toca el juego hasta que pulses **Aplicar**. Tus cambios se van
acumulando, y **Aplicar** y **Descartar** se iluminan en cuanto hay alguno.

| Botón | Qué hace |
|-------|----------|
| **Aplicar** | Hace una copia de todo, escribe tus cambios en los archivos del juego e informa de lo escrito en cada instalación |
| **Descartar** | Desecha todos los cambios sin aplicar. Los archivos del juego no se tocan |

> **Cierra la pantalla de Controles del juego antes de aplicar.** Los cambios hechos a la vez en
> BindForge y en el juego pueden perderse. Para que el juego cargue un archivo de bindings modificado,
> abre y cierra su pantalla de Controles después de aplicar. Vega te lo recuerda.

Si sales de la pestaña o cierras la app con cambios sin aplicar, se te pregunta si quieres **Aplicar**,
**Descartar** o **Volver**.

### Cuando el juego cambió tus archivos

El juego puede cambiar sus propios archivos, y una actualización puede restablecerlos. BindForge los
compara con su copia de trabajo la primera vez que abres la pestaña en cada sesión, y otra vez en cada
Aplicar. Si difieren, muestra qué cambió y te deja elegir:

| Opción | Qué ocurre |
|--------|------------|
| **Volver a poner los míos** | Tu copia de trabajo vuelve al juego. Suele ser lo correcto si un archivo parece restablecido por una actualización |
| **Combinar...** | Cada línea distinta es una fila. Haz clic en el valor que quieres conservar, el tuyo o el del juego |
| **Usar los archivos del juego** | Los archivos del juego sustituyen a tu copia de trabajo |

Antes de cualquiera de estas escrituras se hace una copia de todo, y puedes cerrar el aviso para decidir
más tarde. Cerrarlo durante un Aplicar lo cancela y conserva tus cambios.

**Una versión nueva de tu archivo de bindings.** A veces las actualizaciones guardan tu archivo con otro
nombre y con controles nuevos. BindForge nunca escribe tu archivo antiguo encima. Si todos tus bindings
pasaron, solo tienes que **Aprobar**. Si algunos difieren, **Conciliar...** conserva los controles nuevos
y te pregunta por cada línea distinta.

---

<a id="binding-profile"></a>

## Perfil de bindings

![Perfil de bindings](images/ui-tab-bindings-profile.png)

Este es el Bind Editor. La línea **Editando** de arriba indica el archivo de bindings que estás viendo.
Para abrir otro, usa [Archivo de bindings](#bindings-file) en *Gestión de bindings*.

**Ritmo de entrada de teclas** es un deslizador **Rápido ↔ Lento** para la pausa que Elite Intel hace
tras cada pulsación que envía al juego. Si el juego pierde pulsaciones en equipos más lentos (una macro
que se ejecuta a medias, un panel que se abre en la pestaña equivocada), muévelo hacia **Lento**.

**Buscar bindings...**, en la parte inferior, funciona en las tres vistas. Busca en la sección, el grupo,
el nombre del control y la etiqueta `.binds` en bruto, además de los nombres de mandos y botones de un
binding.

### Modo de juego

Todos los controles, agrupados bajo los encabezados del propio juego y en el mismo orden que su pantalla
de Controles, para que puedas leer ambas a la vez. Las pestañas de arriba reducen la lista a **Todo**,
**General**, **Nave**, **SRV** o **A pie**, cada una con su recuento. Debajo hay dos listas: **Bindings
usados** y **Bindings faltantes**.

| Columna | Significado |
|---------|-------------|
| **Binding** | El control |
| **Primario** / **Secundario** | Las dos ranuras que Elite Dangerous da a cada control, con tus propios nombres de mandos y botones |
| **Acceso IA** | Si Vega puede manejar este control (ver abajo) |
| **Estado** | `Faltante` · `Sin teclado` (asignado, pero solo a un mando) · `No definido` |
| **Corrección rápida** | Lista de *faltantes*: da a este control una tecla libre y segura |
| **Borrar** | Lista de *usados*: quita la asignación de teclado y deja intactas las de mando y HOTAS |

| Acceso IA | Significado |
|-----------|-------------|
| **Vega lo usa** | Vega pulsa este control con su tecla. Mantén una tecla en él |
| **Vega no puede usarlo** | Vega necesita este control, pero no tiene tecla. Pon una en la ranura vacía: tu asignación de mando sigue funcionando |
| **Vega necesita una tecla** | Vega necesita este control y no tiene nada asignado |

Junto a la búsqueda están los dos filtros y el botón de autoasignación:

- **Mostrar solo conflictos**: solo las filas en conflicto.
- **Mostrar solo lo que necesita Vega**: solo los controles que pulsa Vega, para sus comandos integrados
  y tus comandos personalizados.
- **[Autoasignar bindings requeridos por VEGA](#auto-assign-vega-required-binds)**: ver abajo.

### Asignar un botón o una tecla

Haz clic en cualquiera de las dos ranuras de una fila para abrir **Asignar control**. El diálogo muestra
ambas ranuras como tarjetas, y la resaltada es la que recibirá tu siguiente entrada. Haz clic en la otra
para llenar esa, o usa el botón entre ellas para intercambiar Primario y Secundario.

![Asignar un control](images/ui-bindings-assign.png)

Bajo las tarjetas hay una pestaña **Teclado** y otra por cada mando conectado.

- **Teclado**: haz clic en el campo y pulsa las teclas junto con los modificadores, hasta tres. Esc
  cancela. Mantén Ctrl, Mayús o Alt para ver el mapa del teclado de esa combinación: **verde está libre,
  rojo ya está en uso.** Las teclas reservadas, como la del menú del juego y Alt+F4, no se pueden asignar.
- **Un mando**: pulsa un botón o un hat del mando, o haz clic en uno de la lista **Botones** o **Hats**.
  Los botones que ya tienes pulsados se convierten en modificadores. Verde está libre, rojo lo usa otro
  control, y azul es lo que tiene ahora este control. Un mando necesita antes un nombre del
  [Diseñador de alias](#alias-designer).

Una entrada que ya tiene otro control no se puede asignar. La tarjeta nombra ese control, y **Guardar**
sigue atenuado hasta que la liberes allí. Si un cambio fuera a quitar la última tecla de un control que
Vega usa, el diálogo te avisa. **Guardar** conserva tus cambios, listos para **Aplicar**.

> **Los gamepads solo tienen soporte parcial por ahora.** Las palancas del gamepad todavía no se pueden
> asignar aquí.

### Asignar un eje

Los controles de eje, como cabeceo, guiñada y acelerador, están entre las demás filas y abren su propio
diálogo.

![Asignar un eje](images/ui-bindings-axis.png)

Mueve un eje del mando o haz clic en uno de la lista. Después ajusta su **Dirección** (*Normal* o
*Invertido*), su **Zona muerta** y, en los controles que lo tienen, su **Rango del acelerador** (*Rango
completo* o *Solo avance*). Varios controles pueden compartir un eje, y es normal: el diálogo te dice qué
más hay en él, y tú decides.

### Anomalías

![Anomalías](images/ui-bindings-anomalies.png)

Las filas en conflicto reunidas en un solo lugar, con su recuento en la pestaña. Son las mismas filas que
el Modo de juego dibuja en rojo, y al hacer clic se abre el mismo diálogo. **Cuando Vega te avise de un
conflicto de bindings, mira aquí.**

Elite Dangerous solo considera conflicto una combinación *exactamente* igual: `G` y `Mayús+G` conviven sin
problema. Elite Intel sigue la misma regla. Los controles que nunca pueden estar activos a la vez, como
uno de nave y uno de SRV, no se marcan. Pasa el ratón sobre una fila para ver **Comparte *tecla* con:** y
la lista, para cada ranura en conflicto.

También puedes ver **Equivalente nave/SRV - muchos lo asignan igual que:** en una fila. No es un conflicto
sino una sugerencia: algunos controles de nave y de SRV suelen asignarse a la misma tecla.

Vega además **habla** de los bindings que de verdad rompen algo, y nombra las teclas en el registro de
diagnóstico:

- **Movimiento del mapa galáctico y navegación de la interfaz en la misma tecla.** Trazar rutas no puede
  funcionar hasta que el mapa y la interfaz tengan teclas separadas.
- **Un control en tu tecla del menú del juego.** Elite abre el menú con cualquier combinación que termine
  en esa tecla, así que el control nunca se podrá pulsar. La solución es borrar esa asignación en el
  juego: Escape abre ese menú de todos modos.
- **Un control en una combinación que el sistema operativo captura primero** (como Alt+F4). Pulsarlo
  cierra el juego o abandona tu sesión.

### Ajustes

![Ajustes de bindings](images/ui-bindings-settings.png)

Las sensibilidades, zonas muertas y curvas de potencia de tu archivo de bindings, agrupadas como en el
juego. Cada una tiene un deslizador y un número al lado. Arrastra el deslizador, o escribe un número y
pulsa Enter.

El deslizador cubre el mismo rango que el del juego. El número es el valor exacto, y la única forma de
pasar de los límites del deslizador. Un valor que ya esté fuera se queda como está hasta que lo muevas.

<a id="auto-assign-vega-required-binds"></a>

### Autoasignar bindings requeridos por VEGA

Un botón que da una tecla segura y adaptada a tu distribución a cada control que **Vega pulsa** y que no
tiene tecla, incluidos los que usan tus comandos personalizados. Los demás controles se quedan como están.

- Los bindings existentes de cualquier dispositivo nunca cambian. Solo se llena una ranura vacía.
- Ninguna tecla se reutiliza.
- Los cambios se quedan en tu edición, para que los revises antes de **Aplicar**.

Informa de lo que hizo, de lo que omitió y por qué: las dos ranuras ya en un mando, ninguna tecla segura
libre, o ninguna ranura editable con seguridad. Dos controles se dejan **sin asignar a propósito**: el
menú del juego (Escape ya lo abre) y *expulsar toda la carga* (vacía la bodega al espacio, y ningún comando
de Elite Intel lo pulsa).

---

<a id="binding-management"></a>

## Gestión de bindings

El File Manager, con dos pestañas propias: **Ubicaciones del juego** y **Copias del jugador**.

### Ubicaciones del juego

![Ubicaciones del juego](images/ui-bindings-install-locations.png)

**Carpeta de configuración del usuario** es donde están tus archivos `.binds` y el archivo de preset, y
todas las instalaciones la comparten. Se detecta automáticamente. Usa el selector **⋮** si la tuya está en
un lugar poco habitual.

> **¿«No encuentra los bindings» con la carpeta correcta?** Elite Dangerous no escribe un archivo `.binds`
> hasta que personalizas algo. Abre *Opciones → Controles* en el juego, cambia cualquier asignación, y
> Elite Intel detecta el archivo.

<a id="bindings-file"></a>

**Archivo de bindings** muestra tus archivos de bindings, incluidos los cambios sin aplicar. Elige uno y
pulsa **Cargar** para abrirlo en Perfil de bindings, o escribe un nombre nuevo y pulsa **Renombrar**. Un
cambio de nombre es un cambio más, y llega al juego cuando pulsas **Aplicar**.

**Instalaciones del juego** muestra cada copia del juego que BindForge conoce. Cada una tiene sus propios
archivos `DeviceMappings.xml` y `.buttonMap`, y BindForge los mantiene idénticos.

| Columna | Significado |
|---------|-------------|
| **Tienda** | De dónde viene esta copia: Steam, Epic, Frontier u Oculus |
| **Ubicación** | Su carpeta |
| **Origen** | *Detectada* o *Añadida a mano* |
| **Archivos de dispositivo** | Cuántas entradas de dispositivo y button maps tiene, y cuántos están huérfanos |
| **Estado** | *Coincide* con tu copia de trabajo, *Difiere* o *No encontrada* |

**Añadir que falta** añade una instalación que BindForge no detectó. **Quitar** hace que BindForge deje de
copiar o escribir en ella. Si mueves el juego, pulsa **Quitar** y luego **Añadir que falta**.

<a id="player-backups"></a>

### Copias del jugador

![Copias del jugador](images/ui-tab-bindings-management.png)

Proteger tus controles es la razón principal de que exista BindForge. Una configuración compleja lleva
horas, y una actualización del juego puede restablecerla. BindForge lo copia todo por su cuenta, y cada
copia es un `.zip` con fecha que contiene tus archivos de bindings, el archivo de preset y los archivos de
dispositivo de cada instalación.

La columna **Motivo** indica por qué existe cada copia, y de eso depende cuánto se conserva:

| Motivo | Cuándo | Se conserva |
|--------|--------|-------------|
| **Diaria** | La primera vez que Elite Intel arranca cada día | Por antigüedad: 30 días por defecto, o 60, 90 o 180 |
| **Antes de APPLY**, **Antes de RESTORE**, **Antes de la comprobación**, **Antes de cambios** | Automáticamente, antes de que BindForge escriba algo | Las 100 más recientes por defecto, hasta 1000 |
| **Usuario** | Cuando pulsas **Copiar ahora** | Hasta que la borres |
| **Configuración inicial** | Antes de la configuración inicial | Para siempre |

| Botón | Qué hace |
|-------|----------|
| **Copiar ahora** | Haz tú mismo una copia. Hazla antes de empezar a experimentar |
| **Restaurar** | Devuelve la copia seleccionada: cada archivo de bindings, el de preset y los archivos de dispositivo de cada instalación. Antes se copian tus archivos actuales |
| **Buscar archivo de copia de seguridad** | Restaura una copia que no está en la lista, como una que traes de otro ordenador |
| **Eliminar** | Elimina definitivamente una de tus copias de *Usuario* |

> **Restaura solo tus propias copias.** La copia de otro jugador contiene sus mandos, no los tuyos.

**Ajustes de copia de seguridad** define adónde van las copias (**Guardar copias en**, con **Examinar...**
y **Usar predeterminada**) y las dos reglas de conservación. Una carpeta dentro de una instalación del
juego se rechaza, porque una actualización borraría tus copias con ella.

---

<a id="alias-designer"></a>

## Diseñador de alias

![Diseñador de alias](images/ui-bindings-alias-designer.png)

De fábrica, el juego conoce muchos mandos solo por un ID de hardware, y su pantalla de Controles muestra
sus botones como números anónimos. El Diseñador de alias da a cada mando un nombre real, y a cada botón,
eje y hat la etiqueta que elijas. Las etiquetas aparecen en la pantalla de Controles del juego y en Perfil
de bindings.

**Mis dispositivos** muestra tus mandos: **Dispositivo**, **VID / PID**, **Estado** (*Conectado* o
*Ausente*) y **Alias**. **Dispositivos integrados** es una lista de solo lectura de los mandos que Frontier
incluye en el juego. **Comprobar de nuevo** vuelve a leer lo que está conectado.

<a id="naming-a-controller"></a>

### Poner nombre a un mando

Cuando un mando no tiene nombre, **Pon nombre a tus mandos** te guía. Se ejecuta tras la configuración
inicial y con **Comprobar de nuevo**, y puedes iniciarlo para un solo mando desde su engranaje. **Omitir**
usa el nombre predeterminado.

Un nombre usa las letras de la A a la Z, dígitos, `-` y `_`, y empieza por una letra, sin espacios ni
acentos. No puede parecerse demasiado a uno ya usado. Los bindings que conocen el mando solo por su ID de
hardware pasan al nombre nuevo al aplicar.

### El Editor de dispositivos

Pulsa el **engranaje** en la fila de un dispositivo para abrirlo.

![Editor de dispositivos](images/ui-bindings-device-editor.png)

- **Dispositivo**: el nombre, VID, PID, nombre del hardware y estado. Cambia aquí el nombre, y **Aplicar**
  lo renombra en cada instalación y mueve los bindings que lo usan.
- **Ejes**, **Botones**, **Hats**: escribe una etiqueta para cada entrada. Las pestañas solo aparecen para
  los tipos de entrada que tiene el mando. Usa una entrada y su casilla **se ilumina**, para que veas cuál
  es cuál, y los ejes muestran un medidor en vivo.

**Guardar** conserva tus cambios para **Aplicar**; hasta entonces nada llega al juego.

- **Borrar** quita la entrada y las etiquetas del dispositivo de tus archivos del juego al aplicar.
- **Restablecer etiquetas** devuelve las etiquetas predeterminadas.

En un dispositivo integrado de Frontier puedes cambiar las etiquetas. Para usar un nombre propio, conéctalo
y escribe el nombre en la casilla: tu entrada va delante de la de Frontier, y al escribir de nuevo el nombre
de Frontier vuelves atrás.

### Cuando cambia tu hardware

Si falta un mando con nombre que usan tus bindings, Vega te lo dice: sus bindings no se cargarán hasta que
esté conectado. Una actualización de firmware también puede cambiar el ID de producto de un mando, y uno de
repuesto tiene otro nuevo. BindForge puede detectarlo, y **¿Es el mismo mando?** pregunta cuál de los
conectados es. Responde, y sus bindings lo seguirán al aplicar.

> Los bindings se refieren a los botones por número. Si el repuesto es de otro modelo, algunos pueden acabar
> en botones inesperados. **Revisar en el Bind Editor** te lleva directamente a ellos.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
