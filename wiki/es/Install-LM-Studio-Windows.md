## LLM local – Configuración en Windows (LM Studio)

Ejecutar un LLM localmente mantiene todos los datos privados y sin conexión. No hay cuotas de suscripción. Se aplican costes de hardware y electricidad.

Requiere [LM Studio](https://lmstudio.ai) y una GPU suficientemente potente.

---

### Hardware mínimo

Para ejecutar Elite Dangerous y el LLM en la **misma máquina**, se requiere como mínimo una **NVIDIA RTX 3060 con 12 GB de VRAM**.

> **Consejo:** Elite Intel puede apuntarse a una instancia de LM Studio que se ejecute en un **PC separado** de tu red. Si hay disponible una segunda máquina con una GPU capaz, el PC del juego no llevará ninguna carga de inferencia en esta configuración.

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
lms server start
```

`lms daemon up` no hace nada si el servidor en segundo plano ya está en ejecución, así que puedes incluirlo siempre.

**NOTA**: El parámetro `--context-length 8192` es obligatorio. Sin él, la ventana de contexto puede ser demasiado pequeña, causando truncamiento del prompt, fallos y alucinaciones.

Verifica que está en ejecución abriendo un navegador u otra ventana de PowerShell y navegando a:

```
http://localhost:1234/v1/models
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
& $lms server start
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
3. Cerrar la app la minimiza a la bandeja del sistema y mantiene el servidor en ejecución. Se restaura automáticamente en el próximo inicio de sesión.

> ⚠️ **Longitud de contexto:** Con esta opción, LM Studio carga el modelo solo cuando llega la primera petición, con los ajustes **predeterminados** del modelo en lugar del `--context-length 8192` del paso 3. Abre los ajustes predeterminados del modelo en la aplicación de escritorio y fija la **longitud de contexto** en **8192**. Si no, la ventana de contexto puede ser demasiado pequeña.

---

### Paso 5 – Configurar Elite Intel

En Elite Intel, abre **Servicios → Servicios de IA**. En **Modelo de lenguaje (LLM)**, selecciona **LMStudio**:

- **Dirección**: `http://localhost:1234/v1/chat/completions` (el valor por defecto). Si LM Studio está en otra máquina, sustituye `localhost` por la IP de esa máquina.
- **Modelo**: pega la cadena de ID del modelo de `http://localhost:1234/v1/models`. Un solo modelo atiende tanto los comandos como las consultas.

Un LLM local no necesita clave de API.

Haz clic en **Guardar**. Elite Intel reinicia el modelo de lenguaje por sí solo.

---

Comunidad 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
