# Pestaña Acciones

<img src="images/keys-binding.png" class="inline" height="20" alt="Acciones"> Todo lo que Elite
Intel sabe hacer, y todo lo que tú le has enseñado. Dos subpestañas: **Comandos integrados** y
**Comandos personalizados**.

---

## Comandos integrados

![Comandos integrados](images/ui-tab-actions-builtin.png)

Esta es la respuesta a *«¿qué puedo decir ahora mismo?»*, no solo a *«¿qué sabe hacer esta
versión?»*.

### El selector de situación

El selector de arriba a la izquierda contiene **TODOS**, más cada situación física en la que puedes
estar: en nave, en SRV, en caza, en taxi, a pie; atracado, aterrizado, planeando, en supercrucero,
en un anillo, en órbita, en espacio profundo.

- **Sigue al juego en vivo**: sal de tu nave y el selector pasa solo a *A pie*, y la lista de abajo
  cambia con él.
- En cuanto eliges una situación a mano, **deja de seguir** y se queda donde la pusiste.
- **TODOS** enumera todas las acciones de esta versión, incluidas las que no puedes usar donde
  estás. Una situación concreta enumera **solo lo que sirve allí**.

Al lado, un campo de solo lectura **Lugar** muestra la ubicación concreta que informa el juego:
estación, cuerpo o sistema.

### Búsqueda

Un filtro de texto llano y literal sobre las acciones listadas: sus nombres, sus claves de acción y
las frases habladas que las activan. Lo que escribes es lo que se busca.

> Esto **no** es, deliberadamente, el enrutado del compañero. El despacho de Vega ordena por
> *significado*, así que escribir «buscar» allí sacaría comandos que no comparten ni una palabra
> contigo y sin forma de ver por qué. Cuando lees una lista, la búsqueda literal es la que quieres.

### Comandos y consultas disponibles

Una única lista combinada, ordenada alfabéticamente en tres columnas, con las acciones integradas,
tus macros personalizadas y las consultas para la situación elegida. Se actualiza en vivo a partir
de los eventos del juego mientras la pestaña está abierta.

**Haz doble clic en cualquier entrada** para abrir sus detalles.

### Detalles del comando

| Campo | Significado |
|-------|---------|
| **Nombre del comando** | El nombre legible por personas |
| **Clave de acción** | El identificador interno: este es el nombre que ve el modelo de lenguaje |
| **Tipo de comando** | `Binding integrado` (pulsa una tecla) · `Acción integrada` (hace algo en la aplicación) · `Consulta integrada` (responde una pregunta) · `Comando personalizado` (tuyo) |
| **Descripción** | Qué hace |
| **Frases de entrenamiento** | Las frases habladas que llevan a él, en tu idioma actual |

Tres botones:

- **Ejecutar**: lo ejecuta ahora mismo desde la aplicación, sin hablar. Si el comando admite
  parámetros, primero aparece un pequeño formulario.
- **Sugerir una mejor traducción**: abre una incidencia de GitHub precargada con el id del comando,
  tu idioma y las frases actuales, para que propongas una redacción mejor para tu locale. Así es
  como mejoran los conjuntos de frases no ingleses; úsalo, por favor.
- **Cerrar**

Véase también: [Todos los comandos](AllCommands).

---

## Comandos personalizados

![Comandos personalizados](images/ui-tab-actions-custom.png)

Tus propias macros: una secuencia de pasos con nombre, activada por cosas que dices. Parecido en
espíritu a VoiceAttack, pero emparejado por significado y no por una frase exacta.

La tabla lista el **Nombre** de cada comando y sus **Frases de entrenamiento**, con un cuadro de
búsqueda encima.

| Botón | Qué hace |
|--------|--------------|
| **Nuevo** | Crear un comando |
| **Editar** | Editar el comando seleccionado |
| **Eliminar** | Eliminar el comando seleccionado (con confirmación) |
| **Exportar** | Escribir los comandos seleccionados en un archivo que puedas compartir |
| **Importar** | Leer comandos de un archivo. Tu conjunto actual se respalda antes |
| **Restaurar desde copia de seguridad** | Recuperar el conjunto que sustituyó una importación |
| **Abrir carpeta de copias de seguridad** | Abre la carpeta en disco |

> Si alguna vez el archivo de comandos personalizados aparece corrupto al arrancar, Elite Intel
> carga automáticamente desde la copia de seguridad y te dice que lo ha hecho.

### El editor de comandos

![Editor de comandos personalizados](images/ui-custom-command-editor.png)

**Identidad del comando**

| Campo | Notas |
|-------|-------|
| **Nombre** | Cómo lo llamas |
| **Descripción** | Qué hace |
| **Lo que dirás** | Las frases que usarías para ejecutarlo: **una por línea** |
| **Clave de acción** | El identificador interno. Pulsa **Generar** y el modelo de lenguaje te escribe uno a partir de tus frases. Debe ser snake_case ASCII, porque se convierte en un nombre de herramienta que ve el modelo: deja que lo haga el botón Generar. Añade al menos una frase antes de generar |

**Pasos**: la secuencia, en orden. Añade, edita, elimina y mueve pasos arriba y abajo.

| Tipo de paso | Campos | Para qué sirve |
|-----------|--------|------------|
| **Pulsación de binding** | Binding | Pulsar una vez un control asignado |
| **Mantener binding** | Binding, duración en ms | Mantener pulsado un control asignado |
| **Retraso** | Duración en ms | Esperar entre pasos |
| **Hablar** | Texto | Hacer que Vega diga algo |
| **Tecla directa** | Tecla directa, modificador | Pulsar una tecla que no esté asignada a nada en el juego |

Prefiere pasos de **Binding** antes que **Tecla directa** siempre que puedas: los bindings siguen a
las teclas que el juego usa realmente, así que sobreviven a que reasignes un control.

### Cómo usarlos

Habla con normalidad. No tienes que reproducir una frase de entrenamiento palabra por palabra:
tienes que transmitir el mismo significado. Cuanto más se distingan tus frases de las de otros
comandos, con más fiabilidad se elegirá el tuyo.

Vega te dice al arrancar cuántos comandos personalizados se cargaron y cuántos no pasaron la
validación.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
