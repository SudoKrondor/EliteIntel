## LLM local – Configuración en Windows (LM Studio)

Ejecutar un LLM localmente mantiene todos los datos privados y sin conexión. No hay cuotas de suscripción. Se aplican costes de hardware y electricidad.

LM Studio es una alternativa a Ollama. Utiliza los mismos modelos y la misma API compatible con OpenAI. La elección puede cambiarse en los ajustes en cualquier momento.

Requiere [LM Studio](https://lmstudio.ai) y una GPU suficientemente potente.

---

### Hardware mínimo

Para ejecutar Elite Dangerous y el LLM en la **misma máquina**, se requiere como mínimo una **NVIDIA RTX 3060 con 24 GB de VRAM**.

> **Consejo:** Elite Intel puede apuntarse a una instancia de LM Studio que se ejecute en un **PC separado** de tu red. Si hay disponible una segunda máquina con una GPU capaz, el PC del juego no llevará ninguna carga de inferencia en esta configuración.

---

### Modelo recomendado

| Modelo | VRAM requerida | Notas |
|---|---|---|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 GB | ✅ Recomendado para V1.0 |
| `google/gemma-4-e4b` | ~6,3 GB | ✅ Recomendado para V1.1 |

> **¿Qué modelo?** `tulu-3.1-8b-supernova` es el modelo recomendado para **V1.0**. **V1.1** cambia a `google/gemma-4-e4b`, que admite el function calling necesario para la nueva función de compañero. Los comandos siguientes usan el modelo de V1.1: en V1.0, sustitúyelo por `tulu-3.1-8b-supernova`.

---

Tutorial en vídeo muy detallado de @DawnTreaderToolsoftheElite 

[[youtube:F5RgRRePrTo]]

---

### Paso 1 – Instalar LM Studio

Abre **PowerShell** y ejecuta:

```powershell
irm https://lmstudio.ai/install.ps1 | iex
```

Esto instala la CLI `lms` y el entorno de ejecución de LM Studio. Abre una ventana **nueva** de PowerShell tras la instalación para que los cambios surtan efecto.

Verifica que funcionó:

```powershell
lms --help
```

> **Nota:** Si la aplicación de escritorio de LM Studio ya está instalada, la CLI `lms` puede estar ya disponible. Ejecuta `lms --help` antes de ejecutar el script de instalación.

---

### Paso 2 – Descargar el modelo

Para **V1.1**, descarga `google/gemma-4-e4b`:

```powershell
lms get google/gemma-4-e4b
```

Para **V1.0**, descarga `tulu-3.1-8b-supernova`:

```powershell
lms get matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF
```

o

```powershell
lms get Tulu-3.1
```
y elige la variante `matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF` (puede aparecer como `Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`).

Para listar los modelos descargados:

```powershell
lms ls
```

---

### Paso 3 – Iniciar el servidor

Carga el modelo e inicia el servidor de inferencia:

```powershell
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start
```

**NOTA**: El parámetro `--context-length 8192` es obligatorio. Sin él, la ventana de contexto puede ser demasiado pequeña, causando truncamiento del prompt, fallos y alucinaciones.

Verifica que está en ejecución abriendo un navegador u otra ventana de PowerShell y navegando a:

```
http://localhost:1234/v1/models
```

Deberías recibir una lista JSON de los modelos cargados. La cadena de ID del modelo en esa respuesta es lo que introducirás en el campo **LLM Model** de Elite Intel.

Para detener el servidor:

```powershell
lms server stop
```

> ⚠️ **Importante:** El servidor de LM Studio **no** sobrevive a los reinicios. Ejecuta `lms server start` de nuevo tras cada reinicio, o usa una de las opciones de auto-inicio que se describen a continuación.

---

### Paso 4 – (Opcional) Auto-inicio en el arranque

Hay dos opciones disponibles para mantener el servidor en ejecución entre reinicios.

#### Opción A – Aplicación de escritorio

Si la aplicación de escritorio de LM Studio está instalada, este es el enfoque más sencillo:

1. Abre LM Studio y presiona **Ctrl + ,** para abrir Ajustes.
2. Marca **«Run LLM server on login»**.
3. Cerrar la app la minimiza a la bandeja del sistema y mantiene el servidor en ejecución. Se restaura automáticamente en el próximo inicio de sesión.

#### Opción B – Programador de tareas (sin interfaz gráfica)

1. Presiona **Win + R**, escribe `taskschd.msc`, presiona Enter.
2. Haz clic en **Crear tarea** en el panel derecho.
3. **Pestaña General**: Nómbrala `LM Studio Server`. Marca **«Ejecutar con los privilegios más altos»**.
4. **Pestaña Desencadenadores**: Haz clic en Nuevo → **«Al iniciar sesión»** → Aceptar.
5. **Pestaña Acciones**: Haz clic en Nuevo → **«Iniciar un programa»**.
   - Programa/script: `%USERPROFILE%\.lmstudio\bin\lms.exe`
   - Agregar argumentos: `server start`

Para cargar también el modelo automáticamente, crea un archivo batch en su lugar:

```batch
@echo off
%USERPROFILE%\.lmstudio\bin\lms.exe daemon up
%USERPROFILE%\.lmstudio\bin\lms.exe load google/gemma-4-e4b --yes --context-length 8192 --gpu max
%USERPROFILE%\.lmstudio\bin\lms.exe server start
```

Guárdalo como `start-lmstudio.bat` en una ubicación permanente (p. ej., `C:\Scripts\`) y apunta la acción del Programador de tareas a ese archivo.

---

### Paso 5 – Configurar Elite Intel

Abre la **pestaña Ajustes** en Elite Intel:

- Deja el campo **LLM Key** en blanco (LM Studio local no requiere ninguna).
- **LLM Address**: establécela en `http://localhost:1234/v1/chat/completions`. Si LM Studio está en otra máquina, sustituye `localhost` por la IP de esa máquina.
- **LLM Model**: pega la cadena de ID del modelo de `http://localhost:1234/v1/models`.
- **Command LLM**: establécelo en el mismo ID de modelo.
- **Query LLM**: establécelo en el mismo ID de modelo.
- Haz clic en **Stop** y luego en **Start** en la pestaña AI para aplicar los cambios.

---

Comunidad 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
