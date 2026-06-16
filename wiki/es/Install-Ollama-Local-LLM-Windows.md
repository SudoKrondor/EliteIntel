## LLM local - Configuración en Windows (Ollama)

Ejecutar un LLM local mantiene todos los datos privados y sin conexión. No hay cuotas de suscripción. Se aplican costes de hardware y electricidad.

Requiere [Ollama](https://ollama.com) y una GPU capaz.

---

### Hardware mínimo

Para ejecutar Elite Dangerous y el LLM en la **misma máquina**, se requiere como mínimo una **NVIDIA RTX 3060 con 12 GB de VRAM**. El margen de rendimiento es limitado con esta especificación.

> **Consejo:** Elite Intel puede apuntarse a una instancia de Ollama que se ejecute en un **PC separado** de tu red. Si hay disponible una segunda máquina con una GPU capaz, el PC del juego no llevará ninguna carga de inferencia en esta configuración.

---

### Modelo recomendado

| Modelo | VRAM requerida | Notas |
|---|---|---|
| `tulu3:8b` Q4_K_M | ~5 GB | ✅ Recomendado. Fiable para comandos y consultas. |
| `qwen3` 8B | ~8 GB | Experimental. Se esperan comandos fallidos y alucinaciones ocasionales. |

> **Nota:** Para la inferencia local más rápida, considera [LM Studio](Install-LM-Studio-Windows) con `matrixportalx/tulu-3.1-8b-supernova`. En las pruebas, es notablemente más rápido que Ollama en el mismo hardware con el mismo modelo.

---

### Paso 1 - Instalar Ollama

- Ve a [https://ollama.com/download](https://ollama.com/download)
- Descarga y ejecuta `OllamaSetup.exe`. No se requieren derechos de administrador.
- Ollama se instala y se ejecuta en la bandeja del sistema. Se inicia automáticamente al iniciar sesión.

---

### Paso 2 - Descargar un modelo

Abre el **Símbolo del sistema** o **PowerShell** y ejecuta:

```shell
ollama pull tulu3:8b
```

O alternativas experimentales:

```shell
ollama pull qwen3:8b
```

---

### Paso 3 - (Opcional) Ajustar la configuración

Ollama funciona sin ajustes. La siguiente configuración mejora la gestión de la VRAM cuando se ejecuta junto a Elite Dangerous.

En Windows, Ollama lee la configuración desde las **variables de entorno del usuario**.

1. Haz clic derecho en el icono de Ollama en la bandeja del sistema y selecciona **Salir**.
2. Abre **Configuración** y busca "variables de entorno".
3. Haz clic en **"Editar las variables de entorno para tu cuenta"**.
4. Agrega cada variable a continuación usando **Nueva**:

| Variable | Valor | Notas |
|---|---|---|
| `OLLAMA_MAX_VRAM` | `14000000000` | Límite de 14 GB. Ajústalo según tu GPU y los requisitos del juego. |
| `OLLAMA_NUM_PARALLEL` | `3` | Cubre los patrones de llamadas asíncronas de Elite Intel sin sobreasignar. |
| `OLLAMA_MAX_LOADED_MODELS` | `1` | Un modelo en la VRAM a la vez. |
| `OLLAMA_FLASH_ATTENTION` | `1` | Inferencia más rápida. |
| `OLLAMA_KEEP_ALIVE` | `-1` | Mantiene el modelo cargado permanentemente. |

5. Haz clic en **Aceptar**. Vuelve a iniciar Ollama desde el Menú de inicio.

#### Qué hacen estos ajustes

**`OLLAMA_MAX_VRAM`**: Límite máximo de VRAM que puede usar Ollama, en bytes. Deja el resto para Elite Dangerous. Ajústalo según tu GPU y los requisitos del juego.

**`OLLAMA_NUM_PARALLEL`**: Número de solicitudes que Ollama gestiona simultáneamente. Elite Intel realiza llamadas asíncronas, por lo que establecer esto demasiado bajo provoca fallos. `3` cubre el solapamiento típico de comandos y consultas sin sobreasignar.

**`OLLAMA_MAX_LOADED_MODELS`**: Mantiene solo un modelo en la VRAM a la vez.

**`OLLAMA_FLASH_ATTENTION`**: Activa Flash Attention, que reduce el uso del ancho de banda de memoria durante la inferencia. Generalmente más rápido, especialmente para solicitudes repetidas.

**`OLLAMA_KEEP_ALIVE=-1`**: Mantiene el modelo cargado en la VRAM indefinidamente. Sin esto, Ollama puede descargar el modelo tras un período de inactividad, incurriendo en una penalización de recarga en la siguiente solicitud.

---

### Paso 4 - Configurar Elite Intel

Abre la **pestaña Ajustes** en Elite Intel:

- Deja el campo **Clave LLM** en blanco (Ollama local no requiere ninguna).
- **Dirección LLM** tiene como valor predeterminado `http://localhost:11434/api/chat`. Si Ollama está en otra máquina, sustituye `localhost` por la IP de esa máquina.
- **Modelo LLM**: establécelo en `tulu3:8b`.
- **LLM de comandos**: establécelo en `tulu3:8b`.
- **LLM de consultas**: establécelo en `tulu3:8b`.
- Haz clic en **Detener** y luego en **Iniciar** en la pestaña de IA para aplicar los cambios.

---

Comunidad 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
