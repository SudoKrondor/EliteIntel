# Pestaña Acciones

<img src="images/keys-binding.png" class="inline" height="20" alt="Acciones"> Todo lo que Elite
Intel puede hacer, y todo lo que le has enseñado a hacer. Dos subpestañas: **Comandos
integrados** y **Comandos personalizados**.

---

## Comandos integrados

![Comandos integrados](images/ui-tab-actions-builtin.png)

Es la respuesta a *«¿qué puedo decir ahora mismo?»* — no solo a *«¿qué sabe hacer esta versión?»*

### El selector de ámbito

El selector de arriba a la izquierda contiene **TODOS**, más cada situación física en la que
puedes estar: en la nave, en el SRV, en el caza, en el taxi; a pie (estación, hangar, zona
social, planeta); atracado, aterrizado, planeando, en supercrucero, en un anillo, en órbita, en
el espacio profundo.

- **Sigue al juego en vivo** — sal de la nave y el selector pasa solo a *A pie*, y la lista de
  abajo cambia con él.
- En cuanto eliges un ámbito a mano, **deja de seguir** y se queda donde lo pusiste.
- **TODOS** lista cada acción de esta versión, incluidas las que no puedes usar donde estás. Una
  situación concreta lista **solo lo que se puede usar allí**.
- Si el juego no está en marcha, el selector muestra **TODOS**.

A su lado, un campo de solo lectura **Lugar** muestra la ubicación concreta que informa el juego
— estación, cuerpo o sistema.

### Buscar

Un filtro de texto simple y literal sobre las acciones listadas: sus nombres, sus claves de
acción y las frases habladas que las activan. Se busca exactamente lo que escribes.

> Esto **no** es, a propósito, el enrutamiento de Vega. Vega empareja por *significado*, así que
> escribir «buscar» allí devolvería comandos que no comparten ninguna palabra con ello, sin forma
> de ver por qué. Para leer una lista quieres una búsqueda literal.

### Comandos y consultas disponibles

Una lista única, ordenada alfabéticamente en tres columnas, con acciones integradas, tus macros
personalizadas y consultas del ámbito elegido. Se actualiza en vivo con los eventos del juego
mientras la pestaña está abierta — incluido un comando personalizado que crees mientras tanto.

**Haz clic en cualquier entrada** (o selecciónala y pulsa Enter) para abrir sus detalles.

### Detalles del comando

| Campo | Significado |
|-------|---------|
| **Nombre del comando** | El nombre legible |
| **Clave de acción** | El identificador interno — es el nombre que ve el modelo de lenguaje |
| **Tipo de comando** | `Binding integrado` (pulsa una tecla) · `Acción integrada` (hace algo en la app) · `Consulta integrada` (responde una pregunta) · `Comando personalizado` (tuyo) |
| **Descripción** | Lo que hace |
| **Frases de entrenamiento** | Las frases habladas que llevan a él, en tu idioma actual |

Botones:

- **Ejecutar** — lo ejecuta ya desde la app, sin hablar. Si el comando lleva parámetros, primero
  aparece un pequeño formulario.
- **Sugerir una mejor traducción** — abre una incidencia de GitHub ya rellenada con el id del
  comando, tu idioma, las frases actuales y las que propones, para que sugieras mejores frases
  para tu idioma. Así mejoran las frases que no están en inglés; úsalo, por favor.
- **Atrás** — cierra el diálogo.

Ver también: [Todos los comandos y consultas](AllCommands).

---

## Comandos personalizados

![Comandos personalizados](images/ui-tab-actions-custom.png)

> Guía paso a paso: [Crear tus propios comandos](Custom-Commands).

Tus propias macros — una secuencia de pasos con nombre, activada por algo que dices (o escribes
en el chat del juego). Parecido en espíritu a VoiceAttack, pero emparejado por significado y no
por una frase exacta.

La tabla muestra el **Nombre** y las **Frases de entrenamiento** de cada comando, con un buscador
encima. **Haz clic en una fila** para abrir sus detalles, que muestran la **Secuencia** de pasos y
ofrecen **Ejecutar**, **Editar**, **Duplicar** y **Eliminar**.

Arriba:

| Botón | Qué hace |
|--------|--------------|
| **Nuevo** | Crear un comando |
| **Exportar** | Elegir comandos y escribirlos en un archivo que puedes compartir |
| **Importar** | Leer comandos de un archivo. El diálogo de importación marca cada entrada como *Listo*, *Conflicto* (su clave de acción ya existe y se sobrescribirá) o *No válido*. Importar **sustituye** tu conjunto actual — antes se hace una copia de seguridad, y después se te ofrece **Abrir carpeta de copias de seguridad** |
| **Restaurar desde copia de seguridad** | Recupera el conjunto que sustituyó una importación |

**Duplicar** abre el editor con el nombre y los pasos de una copia, pero **sin frases y sin clave
de acción** — escribe frases nuevas, para que los dos comandos sigan siendo fáciles de distinguir.

> Si el archivo de comandos personalizados aparece dañado al arrancar, Elite Intel carga
> automáticamente desde la copia de seguridad y te lo dice.

### El editor de comandos

![Editor de comandos personalizados](images/ui-custom-command-editor.png)

**Identidad del comando**

| Campo | Notas |
|-------|-------|
| **Nombre** | Cómo lo llamas |
| **Lo que dirás** | Las frases con las que lo ejecutarías — **una por línea** |
| **Clave de acción** | El identificador interno. Pulsa **Generar** y el modelo de lenguaje la escribe a partir de tus frases. Siempre es snake_case en inglés, sea cual sea el idioma de tus frases, porque se convierte en un nombre de herramienta que ve el modelo — por eso no se puede escribir a mano. Añade al menos una frase antes de generar |

**Pasos** — la secuencia, en orden. Añade, edita, quita y mueve pasos arriba y abajo.

| Tipo de paso | Campos | Para qué |
|-----------|--------|------------|
| **Pulsación de binding** | Binding | Pulsar una vez un control asignado |
| **Mantener binding** | Binding, Duración ms | Mantener pulsado un control asignado |
| **Retraso** | Duración ms | Esperar entre pasos |
| **Hablar** | Texto | Que Vega diga algo |
| **Tecla directa** | Tecla directa, Modificador, Duración ms | Pulsar una tecla que no está asignada a nada en el juego |
| **Escribir texto** | Texto | Escribir texto en el campo que tenga el foco |

**Escribir texto** va adonde esté el foco del teclado. Abre el campo de texto con un paso
anterior — por ejemplo **Tecla directa: Enter** para abrir el chat de comunicaciones — o el texto
llegará a los controles de tu nave como pulsaciones de teclas.

Prefiere los pasos de **binding** a **Tecla directa** cuando puedas — los bindings siguen las
teclas que el juego usa de verdad, así que sobreviven a que reasignes un control.

### Ejemplo: una ruta a un lugar que visitas a menudo

La navegación integrada de Vega solo traza una ruta hacia el **resultado de una búsqueda** (un
comerciante, un mercado, una zona de caza…), hacia lugares que ya conoce (casa, tu nave nodriza,
una misión) o hacia el sistema de tu portapapeles. No traza una ruta a un sistema que simplemente
nombras en voz alta — los nombres son justo donde más falla el reconocimiento de voz. Para un
lugar al que vuelas a menudo, un comando personalizado lo hace exacto, siempre:

![Comando personalizado que traza una ruta a Jameson Memorial](images/ui-custom-command-navigation.png)

**Lo que dirás:** *ruta a jameson memorial*, *llévame a jameson memorial*, *jameson memorial
ruta* — y luego **Generar** la clave de acción.

| # | Paso | Valor | Duración ms | Qué hace |
|---|------|-------|-------------|--------------|
| 1 | Pulsación de binding | GALAXYMAPOPEN | | Abrir el mapa galáctico |
| 2 | Retraso | | 1000 | Dejar que cargue el mapa |
| 3 | Mantener binding | CAMZOOMIN | 500 | Hacer zoom con la cámara del mapa |
| 4 | Pulsación de binding | UI_LEFT | | Ir al campo de búsqueda |
| 5 | Pulsación de binding | UI_RIGHT | | |
| 6 | Pulsación de binding | UI_SELECT | | Abrir el campo de búsqueda |
| 7 | Escribir texto | SHINRARTA DEZHRA | | Escribir el nombre del sistema — sin reconocimiento de voz |
| 8 | Tecla directa | ENTER | 50 | Buscar |
| 9 | Retraso | | 500 | Esperar el resultado |
| 10 | Pulsación de binding | UI_RIGHT | | Entrar en el panel de resultados |
| 11 | Mantener binding | UI_UP | 500 | Subir hasta el botón superior |
| 12 | Pulsación de binding | UI_SELECT | | Trazar la ruta |
| 13 | Retraso | | 3000 | Esperar a que se trace la ruta |
| 14 | Pulsación de binding | CAMYAWLEFT | | |

Para hacer el tuyo, cópialo con **Duplicar**, cambia el nombre, las frases y el sistema del paso
**Escribir texto**, y genera una clave nueva.

Consejos:

- **Las frases se pueden separar con comas**, además de con saltos de línea.
- **Los retrasos dependen de tu PC.** Si el mapa no está listo cuando llega el siguiente paso,
  aumenta los retrasos — o mueve **Ritmo de entrada de teclas** de la
  [pestaña Bindings](UI-Bindings-Tab) hacia Lento.
- **Cada paso de binding necesita una asignación de teclado** en el juego. Si un control no está
  asignado, la [pestaña Bindings](UI-Bindings-Tab) lo muestra en *Bindings faltantes*.
- Vega abre y lee el mapa galáctico del mismo modo para sus propios comandos de ruta, así que un
  comando que parte de un mapa **recién abierto** es el patrón fiable — no empieces con un mapa
  que ya está abierto.

### Cómo usarlos

Habla con normalidad. No tienes que reproducir una frase de entrenamiento palabra por palabra —
tienes que transmitir el mismo significado. Cuanto más distintas sean tus frases de las de otros
comandos, con más fiabilidad se elegirá el tuyo.

Vega te dice al arrancar cuántos comandos personalizados se cargaron y cuántos no pasaron la
validación.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
