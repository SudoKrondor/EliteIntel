## LLM local – Configuración en Windows (LM Studio)

Ejecutar un LLM localmente mantiene todos los datos privados y sin conexión. No hay cuotas de suscripción. Se aplican costes de hardware y electricidad.

Requiere [LM Studio](https://lmstudio.ai) y una GPU suficientemente potente.

---

### Para quién es esta guía

Esta guía configura LM Studio a mano en un PC que **solo ejecuta el modelo**: un segundo PC de tu red, mientras el
juego corre en tu PC de juego, que así no lleva ninguna carga de inferencia.

> **¿El juego y el modelo en un mismo PC?** Entonces no necesitas esta guía. Si tu PC de juego tiene una NVIDIA
> RTX 3090 (24 GB) o superior, Elite Intel instala y configura LM Studio por ti: consulta
> [Elegir tu LLM](installing-local-llms).

### Hardware para el PC del LLM

- **12 GB de memoria de vídeo.** El modelo ocupa unos 6,3 GB, así que queda margen.
- **Potencia de cálculo de la clase RTX 3080 o superior.** Una tarjeta más lenta puede cargar el modelo, pero
  responde demasiado despacio para ser útil.

---

### Modelo recomendado

| Modelo | VRAM requerida | Notas |
|---|---|---|
| `google/gemma-4-e4b` | ~6,3 GB | ✅ Requerido |

> **¿Por qué este modelo?** Admite el function calling que necesita el compañero. Un modelo que no puede emitir una
> llamada a una herramienta no puede manejar la aplicación, por bien que escriba.
> Consulta [Elegir tu LLM](installing-local-llms).

---

Tutorial en vídeo muy detallado de @DawnTreaderToolsoftheElite 

> **Nota:** El vídeo muestra versiones antiguas de LM Studio y de Elite Intel. Donde difiera de los pasos siguientes, sigue los pasos siguientes.

[[youtube:F5RgRRePrTo]]

---

### Paso 1 – Instalar LM Studio

Abre **PowerShell** y ejecuta:

```powershell
irm https://lmstudio.ai/install.ps1 | iex
```

Esto instala la CLI `lms` y **llmster**, el servidor en segundo plano de LM Studio. **No** instala la aplicación de escritorio de LM Studio. Abre una ventana **nueva** de PowerShell tras la instalación para que los cambios surtan efecto.

Verifica que funcionó:

```powershell
lms --help
```

> **Nota:** Si la aplicación de escritorio de LM Studio ya está instalada, la CLI `lms` puede estar ya disponible. Ejecuta `lms --help` antes de ejecutar el script de instalación.

---

### Paso 2 – Descargar el modelo

Inicia el servidor en segundo plano de LM Studio y luego descarga el modelo:

```powershell
lms daemon up
lms get google/gemma-4-e4b
```

Si el nombre no coincide exactamente, `lms get` muestra los candidatos encontrados; elige
uno con las flechas y Enter.

Para listar los modelos descargados:

```powershell
lms ls
```

[LM Studio tiene un fallo conocido](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). En algunos casos la descarga falla con:
```Error: No staff picks found with the specified search criteria.```

Si ocurre, descarga a mano el archivo GGUF desde la página del modelo en HuggingFace e impórtalo:

```powershell
lms import C:\ruta\al\modelo.gguf
```

---

### Paso 3 – Iniciar el servidor

Carga el modelo e inicia el servidor de inferencia:

```powershell
lms daemon up
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start --bind 0.0.0.0
```

`lms daemon up` no hace nada si el servidor en segundo plano ya está en ejecución, así que puedes incluirlo siempre.

`--bind 0.0.0.0` hace que otros PC de tu red puedan llegar al servidor, que por defecto solo responde a este.

**NOTA**: `--context-length 8192` es obligatorio, y debe ser exactamente 8192: con una ventana más pequeña el modelo alucina, y con una más grande cada respuesta es demasiado lenta.

> **Deja entrar al PC de juego.** La primera vez que el servidor escucha en la red, el Firewall de Windows Defender puede preguntar si lo permite. Permítelo en redes **privadas**. Si nunca pregunta y el PC de juego no puede conectar, añade una regla de entrada para el puerto TCP 1234.

Compruébalo desde el **PC de juego** abriendo un navegador en esta dirección, con la dirección del PC del LLM en lugar de `LLM_PC_IP`:

```
http://LLM_PC_IP:1234/v1/models
```

Deberías recibir una lista JSON de los modelos cargados. La cadena de ID del modelo en esa respuesta es lo que introducirás en el campo **Modelo** de Elite Intel.

Para detener el servidor:

```powershell
lms server stop
```

> ⚠️ **Importante:** El servidor de LM Studio **no** sobrevive a los reinicios. Ejecuta de nuevo los tres comandos del paso 3 tras cada reinicio, o usa una de las opciones de auto-inicio que se describen a continuación.

---

### Paso 4 – (Opcional) Auto-inicio en el arranque

Hay dos opciones disponibles para mantener el servidor en ejecución entre reinicios. Si seguiste el paso 1, usa la **opción A**. La opción B es solo para quien tenga además instalada la aplicación de escritorio de LM Studio.

#### Opción A – Programador de tareas (sin aplicación de escritorio)

Primero, guarda este script como `start-lmstudio.ps1` en una ubicación permanente (p. ej., `C:\Scripts\`):

```powershell
$lms = "$env:USERPROFILE\.lmstudio\bin\lms.exe"
& $lms daemon up
& $lms load google/gemma-4-e4b --yes --context-length 8192 --gpu max
& $lms server start --bind 0.0.0.0
```

Inicia el servidor en segundo plano, carga el modelo con los ajustes que necesita Elite Intel e inicia el servidor de inferencia: los mismos tres comandos del paso 3.

Después, crea la tarea:

1. Presiona **Win + R**, escribe `taskschd.msc`, presiona Enter.
2. Haz clic en **Crear tarea** en el panel derecho.
3. **Pestaña General**: Nómbrala `LM Studio Server`. Deja **«Ejecutar con los privilegios más altos»** sin marcar: LM Studio no necesita derechos de administrador.
4. **Pestaña Desencadenadores**: Haz clic en Nuevo → **«Al iniciar sesión»** → Aceptar.
5. **Pestaña Acciones**: Haz clic en Nuevo → **«Iniciar un programa»**.
   - Programa/script: `powershell.exe`
   - Agregar argumentos: `-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "C:\Scripts\start-lmstudio.ps1"`

`-WindowStyle Hidden` evita que quede abierta una ventana de consola en cada inicio de sesión. Puede aparecer una ventana un instante mientras arranca el script.

#### Opción B – Aplicación de escritorio

Esta opción solo está disponible si la aplicación de escritorio de LM Studio está instalada. El paso 1 no la instala.

1. Abre LM Studio y presiona **Ctrl + ,** para abrir Ajustes.
2. Marca **«Run LLM server on login»**.
3. En los ajustes del servidor, activa **Serve on Local Network** para que el PC de juego pueda llegar a él.
4. Cerrar la app la minimiza a la bandeja del sistema y mantiene el servidor en ejecución. Se restaura automáticamente en el próximo inicio de sesión.

> ⚠️ **Longitud de contexto:** Con esta opción, LM Studio carga el modelo solo cuando llega la primera petición, con los ajustes **predeterminados** del modelo en lugar del `--context-length 8192` del paso 3. Abre los ajustes predeterminados del modelo en la aplicación de escritorio y fija la **longitud de contexto** en **8192**. Si no, la ventana de contexto puede ser demasiado pequeña.

---

### Paso 5 – Configurar Elite Intel

**En el PC de juego:** En Elite Intel, abre **Servicios → Servicios de IA**. En **Modelo de lenguaje (LLM)**, selecciona **LMStudio**:

- **Dirección**: `http://LLM_PC_IP:1234/v1/chat/completions`, cambiando `LLM_PC_IP` por la dirección del PC del LLM en tu red (`ipconfig` en el PC del LLM la muestra).
- **Modelo**: pega la cadena de ID del modelo de `http://LLM_PC_IP:1234/v1/models`. Un solo modelo atiende tanto los comandos como las consultas.

Un LLM local no necesita clave de API.

Haz clic en **Guardar**. Elite Intel reinicia el modelo de lenguaje por sí solo.

---

Comunidad 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
