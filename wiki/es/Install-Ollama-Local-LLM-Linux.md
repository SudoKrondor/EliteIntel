## LLM local - Configuración en Linux (Ollama)

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
| `Tulu-3.1-8B-SuperNova-Q4_K_M`| ~5 GB | ✅ Recomendado. Fiable para comandos y consultas. |
| `qwen3` 8B | ~8 GB | Experimental. Se esperan comandos fallidos y alucinaciones ocasionales. |

> **Nota:** Para la inferencia local más rápida, considera [LM Studio](Install-LM-Studio-Linux) con `matrixportalx/tulu-3.1-8b-supernova`. En las pruebas, es notablemente más rápido que Ollama en el mismo hardware con el mismo modelo.

---

### Paso 1 - Instalar Ollama

```shell
curl -fsSL https://ollama.com/install.sh | sh
```

Ollama se instala como un servicio de systemd y se inicia automáticamente.

---

### Paso 2 - Descargar un modelo recomendado

```shell
ollama pull hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF
```

O alternativas experimentales:

```shell
ollama pull qwen3:8b
```

---

### Paso 3 - (Opcional) Ajustar el servicio de Ollama

Ollama funciona sin ajustes. La siguiente configuración mejora la gestión de la VRAM cuando se ejecuta junto a Elite Dangerous.

```shell
sudo nano /etc/systemd/system/ollama.service.d/override.conf
```

Pega esto:

```ini
[Service]
Environment="OLLAMA_MAX_VRAM=14000000000"
Environment="OLLAMA_DEBUG=0"
Environment="OLLAMA_NUM_PARALLEL=3"
Environment="OLLAMA_MAX_LOADED_MODELS=1"
Environment="OLLAMA_FLASH_ATTENTION=1"
Environment="OLLAMA_KEEP_ALIVE=-1"
Nice=10
IOSchedulingClass=best-effort
IOSchedulingPriority=5
```

Luego recarga y reinicia:

```shell
sudo systemctl daemon-reload
sudo systemctl restart ollama.service
```

#### Qué hacen estos ajustes

**`OLLAMA_MAX_VRAM`**: Límite máximo de VRAM que puede usar Ollama, en bytes. `14000000000` = 14 GB. Deja el resto para Elite Dangerous. Ajústalo según tu GPU y los requisitos del juego.

**`OLLAMA_NUM_PARALLEL`**: Número de solicitudes que Ollama gestiona simultáneamente. Elite Intel realiza llamadas asíncronas, por lo que establecer esto demasiado bajo provoca fallos. `3` cubre el solapamiento típico de comandos y consultas sin sobreasignar.

**`OLLAMA_MAX_LOADED_MODELS`**: Mantiene solo un modelo en la VRAM a la vez.

**`OLLAMA_FLASH_ATTENTION`**: Activa Flash Attention, que reduce el uso del ancho de banda de memoria durante la inferencia. Generalmente más rápido, especialmente para solicitudes repetidas.

**`OLLAMA_KEEP_ALIVE=-1`**: Mantiene el modelo cargado en la VRAM indefinidamente. Sin esto, Ollama puede descargar el modelo tras un período de inactividad, incurriendo en una penalización de recarga en la siguiente solicitud.

---

### Paso 4 - Configurar Elite Intel

Abre la **pestaña Ajustes** en Elite Intel:

- Deja el campo **Clave LLM** en blanco (Ollama local no requiere ninguna).
- **Dirección LLM** tiene como valor predeterminado `http://localhost:11434/api/chat`. Si Ollama está en otra máquina, sustituye `localhost` por la IP de esa máquina.
- **LLM de comandos**: establécelo en `hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF:latest` (o el nombre que muestra `ollama ls`).
- **LLM de consultas**: establécelo en `hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF:latest` (o el nombre que muestra `ollama ls`).
- Haz clic en **Detener** y luego en **Iniciar** en la pestaña de IA para aplicar los cambios.

---

Comunidad 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
