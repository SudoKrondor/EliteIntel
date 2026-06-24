# Ejecutar LLMs en AMD RX 7800 XT (Guía ROCm)

> Guía proporcionada por **Ian Wirtz**

> **Recomendado:** LM Studio (`lms`) tiende a dar los mejores resultados, pero Ollama es una alternativa viable.

---

## Requisitos previos

### Paso 1 - Instalar `rocm-hip-runtime`

Antes de que LM Studio u Ollama puedan usar tu GPU mediante ROCm, el sistema necesita las bibliotecas HIP de espacio de usuario para comunicarse con el controlador del kernel.

**Arch Linux / CachyOS:**
```bash
sudo pacman -S rocm-hip-runtime
```

**Ubuntu / Debian:**
```bash
sudo apt install rocm-hip-runtime
```

**Fedora:**
```bash
sudo dnf install rocm-hip-runtime
```

> **Permisos de acceso a GPU:** Tu usuario debe pertenecer a los grupos `render` y `video`. Compruébalo con:
> ```bash
> groups
> ```
> Si falta alguno de los grupos, agrégalo:
> ```bash
> sudo usermod -aG video,render $USER
> ```
> Debes **cerrar sesión completamente y volver a iniciarla** (o reiniciar) para que el cambio de grupo surta efecto.

---

### Paso 2 - Instalar `rocm-smi` *(puede ser opcional)*

En distribuciones de actualización rápida como Arch, las herramientas de administración no siempre se incluyen como dependencia estricta de `rocm-hip-runtime`. Instálalas explícitamente para evitar incompatibilidades de versiones de bibliotecas.

**Arch Linux / CachyOS:**
```bash
sudo pacman -S rocm-smi-lib
```

**Ubuntu / Debian:**

Debian divide la herramienta de línea de comandos y su biblioteca de tiempo de ejecución en paquetes separados.

```bash
# Herramienta de monitoreo en línea de comandos
sudo apt update && sudo apt install rocm-smi

# Bibliotecas de tiempo de ejecución
sudo apt update && sudo apt install librocm-smi64-1
```

> **Consejo:** Si el nombre exacto del paquete es incierto, escribe `sudo apt install librocm-smi64` y presiona **Tab** para autocompletar el sufijo de versión actual.

**Fedora:**
```bash
# Herramienta CLI
sudo dnf install rocm-smi

# Bibliotecas de desarrollo C/C++ y cabeceras (equivalente a rocm-smi-lib en Arch)
sudo dnf install rocm-smi-devel
```

---

## Ejecutar un modelo

| Modelo | VRAM requerida | Notas |
|---|---|---|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 GB | ✅ Recomendado para V1.0 |
| `google/gemma-4-e4b` | ~6,3 GB | ✅ Recomendado para V1.1 |

> **¿Qué modelo?** `tulu-3.1-8b-supernova` es el modelo recomendado para **V1.0**. **V1.1** cambia a `google/gemma-4-e4b`, que admite el function calling necesario para la nueva función de compañero. Los comandos siguientes usan el modelo de V1.1: en V1.0, sustitúyelo por `tulu-3.1-8b-supernova`.

### Paso 3 - Cargar un modelo con aceleración ROCm

Al invocar `lms load`, pasa los indicadores de aceleración de hardware explícitamente. El indicador `--gpu max` le indica al tiempo de ejecución que cargue el modelo completo en la VRAM.

```bash
HSA_OVERRIDE_GFX_VERSION=11.0.0 lms load google/gemma-4-e4b --context-length 8192 --gpu max
```

El prefijo `HSA_OVERRIDE_GFX_VERSION=11.0.0` le indica a la pila ROCm que trate la RX 7800 XT como un objetivo de cómputo compatible de forma nativa, evitando fallos de reserva silenciosa.

---

### Paso 4 - Hacer la configuración permanente

Para evitar prefijar cada comando con la variable de entorno, agrégala a tu perfil de shell.

**Bash:**
```bash
echo 'export HSA_OVERRIDE_GFX_VERSION=11.0.0' >> ~/.bashrc
source ~/.bashrc
```

**Fish (predeterminado de CachyOS) - Opción A: Variable Universal (recomendado)**

Configúrala una vez; Fish la persiste automáticamente entre reinicios sin ninguna configuración adicional:
```fish
set -Ux HSA_OVERRIDE_GFX_VERSION 11.0.0
```

**Fish - Opción B: Entrada explícita en el archivo de configuración**
```fish
echo 'set -gx HSA_OVERRIDE_GFX_VERSION 11.0.0' >> ~/.config/fish/config.fish
source ~/.config/fish/config.fish
```

**Ollama (servicio systemd):**

Dado que Ollama se ejecuta bajo su propio usuario del sistema `ollama`, la variable debe inyectarse mediante un archivo de anulación de systemd:

```bash
sudo mkdir -p /etc/systemd/system/ollama.service.d
sudo nano /etc/systemd/system/ollama.service.d/override.conf
```

Pega lo siguiente, luego guarda y sal (`Ctrl+O`, `Enter`, `Ctrl+X`):

```ini
[Service]
Environment="HSA_OVERRIDE_GFX_VERSION=11.0.0"
```

Luego recarga y reinicia el servicio:
```bash
sudo systemctl daemon-reload
sudo systemctl restart ollama
```

---

## Verificación

### Paso 5 - Confirmar que el controlador de cómputo del kernel está cargado

Si un comando se cuelga, es posible que la capa de cómputo del kernel (`amdkfd`) no se haya inicializado. Comprueba si el sistema expone tu GPU como plataforma de cómputo ROCm:

```bash
rocminfo
```

Desplázate hasta la parte superior de la salida. Si ves `Can't open /dev/kfd` o un fallo, el kernel de Linux no está exponiendo la interfaz de cómputo al espacio de usuario. Si estás usando un kernel personalizado o de vanguardia, intenta arrancar con el kernel estable o LTS (`linux-lts`) para descartar una regresión del controlador.

---

### Paso 6 - Iniciar el servidor y verificar el uso de VRAM

**LM Studio:**
```bash
lms server start
```

**Ollama:**
```bash
ollama serve
```

Luego confirma que el modelo está cargado en la VRAM:
```bash
rocm-smi
```

**En reposo (sin modelo cargado):**

![rocm-smi output with no model running](images/rocm-smi-without-game-running-example.png)

En reposo, la GPU consume una potencia mínima (~9 W), los relojes están cerca del mínimo y el uso de VRAM es bajo (~44%).

**Bajo carga (modelo y juego ejecutándose simultáneamente):**

![rocm-smi output with Elite Dangerous and EliteIntel running](images/rocm-smi-with-game-and-Elite-Intel-running-example.png)

Bajo carga combinada deberías ver cómo el uso de VRAM aumenta significativamente (71% en este ejemplo), sube la utilización de la GPU y el consumo de energía crece proporcionalmente (~147 W). Esto confirma que el modelo reside en la VRAM y que la inferencia se está ejecutando en la GPU.
