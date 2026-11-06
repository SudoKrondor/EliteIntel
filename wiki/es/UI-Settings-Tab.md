# Pestaña Servicios

<img src="images/services.png" class="inline" height="20" alt="Servicios"> La fontanería. El selector de
**Idioma** vale para todo y está arriba, y después hay tres subpestañas: **Servicios de IA**, **Audio** y **Push To
Talk**. El pie contiene [**Copia de ajustes y datos**](#backup-settings-and-data), **Restaurar...** y el botón de
actualización — *La aplicación está actualizada* o *Actualización disponible*.

---

## Idioma

El idioma de tus órdenes de voz y de la propia interfaz de la app. Al elegir uno, la ventana entera se redibuja al
momento y Vega anuncia el cambio en voz alta.

El selector está **bloqueado** para que un clic o un desplazamiento accidental no cambie tu idioma. Desmarca el
candado de al lado para cambiarlo; después vuelve a bloquearse solo.

Admitidos: inglés, ruso, ucraniano, alemán, francés, español, italiano, portugués, portugués de Brasil.

> La **carpeta del journal** ahora está en la [pestaña Comandante](UI-Commander-Tab), y la **carpeta de bindings** en
> la [pestaña Bindings](UI-Bindings-Tab).

---

## Servicios de IA

![Servicios de IA](images/ui-tab-settings-ai.png)

Dos selectores — uno para el modelo de lenguaje y otro para la voz — y el lado que no se usa se
atenúa para que quede claro cuál está activo.

Es la única pestaña de la app que trabaja con un **borrador**. No se escribe nada hasta que
pulsas **Guardar**, y si intentas salir con cambios sin guardar te pregunta *Guardar*,
*Descartar* o *Seguir editando*.

### Modelo de lenguaje (LLM)

Cambia entre **LMSTudio** (local) y **Configuración en la nube**.

**LMSTudio**

| Campo | Notas |
|-------|-------|
| **Dirección** | Por defecto, la URL de LM Studio, `http://localhost:1234/v1/chat/completions`. Pon la IP de otra máquina si la inferencia corre en otro equipo de tu red |
| **Modelo** | El nombre del modelo. Un solo modelo atiende órdenes y consultas |

El modelo local admitido es **`google/gemma-4-e4b`**. Elite Intel te avisa al arrancar si tu
modelo local es otro; otros modelos pueden funcionar mal o no funcionar.

Guías: [LM Studio en Linux](Install-LM-Studio-Linux) ·
[LM Studio en Windows](Install-LM-Studio-Windows) ·
[Serie AMD RX](AMD-RX-7800XT-LLM-Setup) ·
[Servidor Ubuntu headless](LM-Studio-Ubuntu-Server)

**Configuración en la nube**

| Campo | Notas |
|-------|-------|
| **Proveedor** | Elige uno: **Anthropic (Claude)**, **OpenAI**, **Google (Gemini)**, **xAI (Grok)**, **DeepSeek**, **Mistral** |
| **Clave API** | Tu clave para ese proveedor, con una casilla **Bloqueado** al lado para que una clave guardada no se edite por accidente. Desmárcala para cambiarla |

No eliges modelo — se selecciona automáticamente el adecuado para tu proveedor.

Una clave pertenece a un proveedor: al elegir otro proveedor, el campo de la clave se vacía para
que pegues la de ese proveedor, y al volver a elegir el proveedor guardado vuelve su clave, así
que un clic accidental no cuesta nada. **Guardar** sigue en gris hasta que proveedor y clave
están rellenos.

Mistral tiene un nivel gratuito y es la forma más fácil de empezar.
Cómo conseguir una clave de cada proveedor: [Opciones de LLM en la nube](cloud-llm-options).

### Voz (TTS)

Cambia entre **Síntesis de voz local** y **Nube · Google**.

| Motor | Dónde se ejecuta | Notas |
|-------|------------------|-------|
| **Supertonic 3** | En tu PC | El predeterminado. 45 voces, y cada voz habla todos los idiomas de la app y del juego. Sin clave, y nada sale de tu PC |
| **Google** | Servidores de Google | Google Cloud Text-to-Speech. Necesita una **Clave de Google TTS** (con la misma casilla Bloqueado). Tiene un deslizador **Tono de Google WaveNet**, que usan las voces WaveNet, y otro **Velocidad de voz de Google** |

El tráfico de radio siempre lo dice Supertonic 3, sea cual sea el motor que elijas para Vega.

> Cambiar de motor restablece la voz de cada nave a la predeterminada del nuevo motor. Las personalidades se conservan.
> Se te pide confirmación antes.

<a id="voice-tuning"></a>

### Ajuste de voces

![Ajuste de voces](images/ui-voice-tuning.png)

Las voces difieren un poco en volumen y ritmo, así que **Ajuste de voces…** te deja adaptar cada una por separado. La
lista de la izquierda se agrupa en **Voces femeninas** y **Voces masculinas**; selecciona una para ver sus ajustes.

| Control | Qué hace |
|---------|----------|
| **Velocidad** | Lo rápido que habla esta voz |
| **Volumen** | Un ajuste de volumen para esta voz |
| **Ecualizador** | Seis bandas para dar forma al sonido de esta voz |
| **Probar** | Reproduce una frase con los ajustes actuales |
| **Restablecer voz** | Devuelve esta voz a sus valores predeterminados |

No hay botón Guardar: un cambio se guarda en cuanto sueltas el deslizador. Puedes abrir Ajuste de voces sea cual sea el
motor que hable por Vega, porque Supertonic 3 pone voz a la radio de todos modos.

### Pie

**Restaurar valores predeterminados** devuelve el modelo de lenguaje a LM Studio local con la
dirección y el modelo por defecto, y guarda al momento. **Guardar** aplica todo lo demás; está en
gris hasta que algo cambia de verdad, y entonces aparece al lado el aviso **Cambios sin guardar**.

Al guardar solo se reinicia lo necesario — cambiar el modelo o la clave reinicia el cerebro;
cambiar el motor de voz o su clave reinicia la voz.

---

## Audio

![Ajustes de audio](images/ui-tab-settings-audio.png)

### Dispositivos de audio

Listas **Mic.** y **Altavoz**, o *(Predeterminado del sistema)*. Los mismos selectores están en el
botón **Dispositivos de audio** de la pestaña Vega. El cambio se aplica al momento — solo se
reinicia el servicio que usa el dispositivo.

**Activar reducción de ruido** con intensidad **Bajo / Medio / Alto**. Empieza en Medio. Alto es
para salas realmente ruidosas — es agresivo, y filtrar de más puede costarte precisión en la
transcripción.

Debajo de los dispositivos hay dos pestañas.

### Niveles de audio

| Deslizador | Qué hace |
|--------|--------------|
| **Volumen de voz** | Lo alto que habla Vega |
| **Volumen de radio** | Lo altas que suenan las transmisiones de radio. En gris mientras las transmisiones de radio están desactivadas |
| **Volumen de pitidos** | El pitido de confirmación — suena cuando el reconocimiento de voz ha terminado y el modelo de lenguaje tiene tu entrada |
| **Hilos de STT** | Hilos de CPU para la transcripción (4–11). Es una petición mínima, no una reserva: la app pide tantos, usa los que le da el procesador y los libera al terminar |

Lo rápido que habla Vega se ajusta por voz en [Ajuste de voces](#voice-tuning), o con **Velocidad de voz de Google**
para una voz de Google. El volumen de la música está en la [pestaña Jukebox](UI-Jukebox-Tab), para que nunca bajes el
equivocado.

### Audio de transmisiones

Cómo suenan los mensajes de radio.

| Control | Qué hace |
|---------|--------------|
| **Pitido de radio al principio y al final de cada mensaje** | Tonos de squelch alrededor de cada transmisión, con su propio deslizador de volumen |
| **Efecto de radio** | Un sonido de radio más marcado y degradado |
| **Aplicar los efectos seleccionados al chat de radio y a los mensajes de PNJ** | Los tonos y el efecto de radio de arriba se aplican al tráfico de radio. Desactivado, la voz de radio conserva su filtro estándar |
| **Aplicar los efectos seleccionados a VEGA a pie o en un SRV** | Vega suena como por el comunicador cuando no estás en la nave |

### Monitor de micrófono

Un medidor en vivo en el lado derecho. Se lee así:

- **FLOOR** — tu nivel de ruido cuando *no* hablas.
- **GATE** — el umbral. El audio por encima de la puerta se captura para transcribir; cuando
  baja de ella, lo capturado se transcribe y se envía al modelo de lenguaje.
- **CLIP** — estás saturando el micrófono. Todo lo que llega ahí se transcribe mal.

El estado indica **OPEN**, **MARGINAL**, **CLOSED** o **HOT** (saturando). Bajo el medidor aparece
un aviso en lenguaje claro cuando algo va mal: *Micrófono sin calibrar*, o *Micrófono demasiado
débil para la sala* — sube el nivel de entrada en la configuración de sonido del sistema
operativo y vuelve a calibrar. Si el micrófono está bien, no aparece ningún aviso.

Si el medidor no muestra una separación clara entre FLOOR y tu nivel al hablar, ejecuta
**Calibrar audio** en la pestaña Vega — ajusta la puerta por ti y te avisa si la separación es
demasiado pequeña para trabajar.

---

## Push To Talk

![Push to Talk](images/ui-tab-settings-push-to-talk.png)

Con Push to Talk activado, el micrófono está cerrado hasta que mantienes un botón. Lo que capte
sin el botón pulsado se descarta como ruido ambiente.

| Control | Notas |
|---------|-------|
| **Activar Push to Talk** | El interruptor principal |
| **Controlador** | Cualquier mando o HOTAS conectado. Tu controlador guardado se vuelve a seleccionar solo cuando se reconecta |
| **Botón** | Qué botón de él, con la etiqueta que le diste en el [Diseñador de alias](UI-Bindings-Tab) |
| **Botón del ratón** | Un segundo disparador: *Botón central*, *Botón 4 (atrás)* o *Botón 5 (adelante)*. Útil a pie o en el SRV, cuando el HOTAS queda lejos. El izquierdo y el derecho no se ofrecen — con ellos disparas tus armas |

Mantén el botón, habla, suelta. Pulsarlo también **corta a Vega a media frase**, así que nunca
tienes que esperar a que termine.

Mientras Push to Talk está activo, el botón **DORMIR / DESPERTAR** de la pestaña Vega está
desactivado — el botón es la puerta. Un cambio aquí surte efecto en la siguiente pulsación, y el
botón funciona tanto si abres esta pestaña como si no.

---

<a id="backup-settings-and-data"></a>

## Copia de ajustes y datos

Para pasar a un ordenador nuevo, o simplemente por precaución. **Copia de ajustes y datos** escribe un `.zip` con tus
ajustes, bases de datos, comandos personalizados y las copias de BindForge.

**Las claves API necesitan una contraseña.** Elige una y pulsa **Bloquear claves y guardar** para incluir tus claves en
la copia. Necesitarás la contraseña para restaurarlas. Si eliges **Sin claves API**, la copia no contiene ninguna y las
introduces de nuevo después de restaurar.

**Restaurar...** devuelve una copia. Lo que sustituye se guarda en una carpeta de seguridad dentro de la carpeta de datos
de la app, y Elite Intel se reinicia para terminar. Una copia hecha con una versión más reciente requiere actualizar antes
la app. Las carpetas que no existen en el ordenador nuevo se restablecen, así que comprueba después la carpeta del journal
en la pestaña Comandante y la de bindings en la pestaña Bindings.

> Esto hace una copia de **Elite Intel**. Para copiar tus **archivos de controles del juego**, usa
> [Copias del jugador](UI-Bindings-Tab) en la pestaña Bindings.

---

## Dónde se guardan los ajustes

Todos los ajustes y datos se guardan en tu PC:

- **Linux:** `~/.local/share/elite-intel/` (o `$XDG_DATA_HOME/elite-intel/`)
- **Windows:** `%LOCALAPPDATA%\elite-intel\`

La base de datos está en `db`, los comandos personalizados en `custom-commands` (con su propio `backups`), y las copias de
tus archivos de controles en `playerbackups`, salvo que hayas elegido otra carpeta para ellas.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
