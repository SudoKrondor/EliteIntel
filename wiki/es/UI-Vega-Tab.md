# Pestaña Vega

<img src="images/ai.png" class="inline" height="20" alt="Vega"> La pestaña por defecto, y la que
dejas abierta mientras vuelas. Arranca y detiene la pila de IA, muestra lo que Vega oyó y dijo,
informa del estado de cada subsistema y abre el overlay dentro del juego.

![Pestaña Vega](images/ui-tab-vega.png)

La pestaña se reparte en cuatro zonas: los registros de **conversación** y **diagnóstico** a la
izquierda, **Estado rápido** y **Accesos directos** en la barra lateral derecha, y la franja de
telemetría **Resumen del sistema** en la parte inferior.

---

## Conversación

Todo lo que dijiste y todo lo que Vega respondió, en un único flujo. Tus líneas van alineadas a la
izquierda y las respuestas de Vega a la derecha, para que una sesión larga siga siendo legible de
un vistazo.

## Diagnóstico / Mensajes del sistema

El registro técnico: arranques de servicios, resultados de calibración, avisos de asignaciones,
operaciones de archivo. Nunca se habla; existe para que veas qué está haciendo la aplicación.

Hay cuatro botones en la cabecera de la sección:

| Botón | Qué hace |
|--------|--------------|
| **Copiar** | Copia al portapapeles el texto que hayas seleccionado en el registro. Solo se activa cuando hay una selección. |
| **Guardar paquete de depuración** | Escribe un `.zip` con marca de tiempo que contiene el registro del sistema, el registro de la aplicación, tu archivo de diario en vivo y tus asignaciones. **Esto es lo que hay que adjuntar a un informe de error.** |
| **Volcar la memoria de Vega** | Escribe una instantánea JSON de la memoria de trabajo de Vega para la sesión actual. Solo disponible mientras los servicios están en marcha. |
| **Borrar** | Vacía el registro de diagnóstico y su transcripción de exportación. |

---

## Estado rápido

Seis lecturas en vivo. Cada una muestra un estado y un color, así que un vistazo basta para saber
si la pila está sana.

| Lectura | Estados |
|---------|--------|
| **STT** | `En espera` (servicios detenidos) · `En pausa` (te ignora) · `Escuchando` |
| **IA** | `En espera` · `Sin conexión` (no pudo conectar) · o el nombre del proveedor que está respondiendo de verdad |
| **TTS** | `En espera` · `Local` (Kokoro) · `Nube` (Google) |
| **Bindings** | `Todo correcto`, o `N sin asignar` |
| **Comandos** | Cuántos comandos personalizados hay cargados |
| **Teclas** | `Sincronizado` con el juego, o `Modificado`: tienes un borrador de asignaciones sin aplicar |

La lectura de **IA** merece atención. No informa de lo que *configuraste*, informa de qué proveedor
respondió realmente a la última petición.

---

## Accesos directos

| Botón | Qué hace |
|--------|--------------|
| **Iniciar / Detener servicios** | Conmuta toda la pila de IA. El botón se desactiva solo mientras arranca o se detiene, para que no pueda dispararse dos veces. |
| **Dormir / Despertar** | En modo *despierto* Vega escucha continuamente. En modo *dormido* te ignora salvo que uses la palabra de paso `listen` o digas `Wake up!`. Se desactiva mientras pulsar para hablar está activo: en modo PTT el botón *es* la compuerta. |
| **Mostrar / Ocultar overlay** | Muestra el [overlay HUD](UI-HUD-Overlay) siempre visible. Si falta el binario del overlay, el botón se mantiene honesto e informa del fallo en el registro en lugar de fingir un overlay que no existe. |
| **Ajustes de overlay** | Abre los [ajustes del overlay HUD](UI-HUD-Overlay): transparencia, tamaño del texto y dónde se dibuja (monitor, visor VR, ambos). |
| **Dispositivos de audio** | Abre el diálogo de interfaz de audio para elegir micrófono y altavoz. Los cambios se aplican en el siguiente arranque de servicios. |
| **Calibrar audio** | Mide tu ruido de fondo y tu nivel de voz y fija la compuerta de audio. Solo disponible mientras los servicios están en marcha. Ejecútalo una vez antes de tu primer vuelo, y de nuevo si cambias de micrófono o de sala. |
| **Actualizar** | Aparece cuando hay una versión nueva disponible. |

Entre los dos grupos de botones está el **bloque del comandante**: tu nombre, tu nave, el reloj y
tu saldo de créditos en vivo.

---

## Resumen del sistema

Una franja de telemetría de seis bloques en la parte inferior de la pestaña:

| Bloque | Significado |
|-------|---------|
| **Modelo LLM** | El modelo que atendió la petición más reciente |
| **Tiempo de sesión** | Tiempo desde que arrancaron los servicios |
| **Tokens usados** | Prompt + respuesta + caché, de la sesión |
| **Tokens / hora** | Una tasa proyectada. Permanece en blanco los primeros 10 minutos mientras recopila datos |
| **Caché ahorrada** | Tokens servidos desde caché. El `0` se muestra a propósito: es información, no un dato ausente |
| **Última velocidad** | Tokens por segundo en la última respuesta |

Para el desglose completo, consulta la [pestaña Estadísticas](UI-Stats-Tab).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
