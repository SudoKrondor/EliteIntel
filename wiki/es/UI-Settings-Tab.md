# Pestaña Ajustes

<img src="images/settings.png" class="inline" height="20" alt="Ajustes"> La fontanería. Una
franja **General** que vale para todo, y luego tres subpestañas: **Servicios de IA**, **Audio** y
**Push To Talk**. El botón de actualización está en el pie — *La aplicación está actualizada* o
*Actualización disponible*.

---

## General

Se muestra encima de las subpestañas porque se aplica a todas.

**Idioma** — el idioma de tus órdenes de voz y de la propia interfaz de la app. Al elegir uno, la
ventana entera se redibuja al momento y Vega anuncia el cambio en voz alta.

Admitidos: inglés, ruso, ucraniano, alemán, francés, español, italiano, portugués, portugués de
Brasil.

**Directorio de Journal** — donde Elite Dangerous escribe sus archivos de journal. Opcional: si
lo dejas vacío se usa la ubicación estándar de tu plataforma. Así sabe Elite Intel qué ocurre
alrededor de tu nave; si está mal, la app queda prácticamente ciega y lo dice al arrancar. Una
carpeta inservible se rechaza y se mantiene el ajuste anterior.

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
[Serie AMD RX](AMD-RX-7800XT-LLM-Setup)

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

Cambia entre **Local · Kokoro / Supertonic** y **Nube · Google / Edge**. Cada lado tiene un
segundo selector para elegir el motor.

| Motor | Dónde se ejecuta | Notas |
|--------|---------------|-------|
| **Kokoro** | En tu PC | El predeterminado. Sin clave, nada sale de tu PC. No sabe pronunciar el cirílico — ver abajo |
| **Supertonic 3** | En tu PC | Diez voces, todos los idiomas, incluidos ruso y ucraniano. Sin clave. Tiene un deslizador **Amplificación de Supertonic 3 (0–100 %)** para subir su nivel |
| **Google** | Servidores de Google | Google Cloud Text-to-Speech. Necesita una **Clave de Google TTS** (con la misma casilla Bloqueado). Tiene un deslizador **Tono de Google WaveNet** para las voces WaveNet |
| **Microsoft Edge** | Servidores de Microsoft | Las voces de lectura en voz alta en línea de Microsoft. Sin clave |

**Kokoro y el cirílico.** Cuando el idioma de la app es ruso o ucraniano — o cuando tu *cliente
del juego* escribe la charla de radio en ruso —, el segmento de Kokoro aparece en gris, un aviso
explica por qué, y habla Supertonic 3 en su lugar.

> Cambiar de motor restablece la voz de cada nave a la voz predeterminada del nuevo motor. Las
> personalidades de las naves se conservan. Se te pide confirmación antes.

### Pie

**Restaurar valores predeterminados** devuelve el modelo de lenguaje a LM Studio local con la
dirección y el modelo por defecto, y guarda al momento. **Guardar** aplica todo lo demás; está en
gris hasta que algo cambia de verdad, y entonces aparece al lado el aviso **Cambios sin guardar**.

Al guardar solo se reinicia lo necesario — cambiar el modelo o la clave reinicia el cerebro;
cambiar el motor de voz o su clave reinicia la voz. Los deslizadores de tono y amplificación se
aplican sin reiniciar.

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
| **Velocidad de voz TTS** | Lo rápido que habla Vega |
| **Volumen de pitidos** | El pitido de confirmación — suena cuando el reconocimiento de voz ha terminado y el modelo de lenguaje tiene tu entrada |
| **Hilos de STT** | Hilos de CPU para la transcripción (4–11). Es una petición mínima, no una reserva: la app pide tantos, usa los que le da el procesador y los libera al terminar |

El volumen de la música no está aquí — está en la [pestaña Jukebox](UI-Jukebox-Tab), para que
nunca bajes el equivocado.

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
| **Botón** | Qué botón de él |
| **Botón del ratón** | Un segundo disparador: *Botón central*, *Botón 4 (atrás)* o *Botón 5 (adelante)*. Útil a pie o en el SRV, cuando el HOTAS queda lejos. El izquierdo y el derecho no se ofrecen — con ellos disparas tus armas |

Mantén el botón, habla, suelta. Pulsarlo también **corta a Vega a media frase**, así que nunca
tienes que esperar a que termine.

Mientras Push to Talk está activo, el botón **DORMIR / DESPERTAR** de la pestaña Vega está
desactivado — el botón es la puerta. Un cambio aquí surte efecto en la siguiente pulsación, y el
botón funciona tanto si abres esta pestaña como si no.

---

## Dónde se guardan los ajustes

Todos los ajustes y datos se guardan en tu PC:

- **Linux:** `~/.local/share/elite-intel/` (o `$XDG_DATA_HOME/elite-intel/`)
- **Windows:** `%LOCALAPPDATA%\elite-intel\`

La base de datos está en `db`, los comandos personalizados en `custom-commands` (con su propio
`backups`), tus instantáneas manuales de bindings en `playerbackups`, y las copias automáticas
previas a Aplicar en `bindings/backups`.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
