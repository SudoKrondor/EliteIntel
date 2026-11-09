## LLM local – Configuración en Linux (LM Studio)

Ejecutar un LLM localmente mantiene todos los datos privados y sin conexión. No hay cuotas de suscripción. Se aplican costes de hardware y electricidad.

Requiere [LM Studio](https://lmstudio.ai) y una GPU suficientemente potente.

---

### Hardware mínimo

Para ejecutar Elite Dangerous y el LLM en la **misma máquina**, se requiere como mínimo una **NVIDIA RTX 3060 con 12 GB de VRAM**. El margen de rendimiento es limitado con esta especificación.

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

[[youtube:2HGFmlZGK1g]]

---

### Paso 1 – Instalar LM Studio

```shell
curl -fsSL https://lmstudio.ai/install.sh | bash
```

El instalador coloca todo en `~/.lmstudio/` y añade la herramienta CLI `lms`. Cuando termine, agrega el CLI a tu PATH:

```shell
# Añade esto a tu ~/.bashrc
export PATH="$HOME/.lmstudio/bin:$PATH"
```

Luego recarga tu shell:

```shell
source ~/.bashrc
```

Verifica que funcionó:

```shell
lms --help
```

---

### Paso 2 – Descargar el modelo

Inicia el servidor en segundo plano de LM Studio y luego descarga el modelo:

```shell
lms daemon up
lms get google/gemma-4-e4b
```
Si el nombre no coincide exactamente, `lms get` muestra los candidatos encontrados; elige
uno con las flechas y Enter.

Para listar los modelos descargados:

```shell
lms ls
```

Esa es la ruta estándar. Sin embargo, [LM Studio tiene un bug conocido](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). En algunos casos la descarga falla con:
```Error: No staff picks found with the specified search criteria.```

Si ocurre, descarga el GGUF a mano desde la página del modelo en HuggingFace e impórtalo:

```shell
lms import /path/to/the-model.gguf
```

---

### Paso 3 – Iniciar el servidor

Carga el modelo e inicia el servidor de inferencia:

```shell
lms daemon up
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start
```

`--gpu max` delega la inferencia a la GPU para el máximo rendimiento. `lms daemon up` no hace nada si el servidor en segundo plano ya está en ejecución, así que puedes incluirlo siempre.

Verifica que está en ejecución:

```shell
curl http://localhost:1234/v1/models
```

Deberías recibir una lista JSON de los modelos cargados. La cadena de ID del modelo en esa respuesta es lo que introducirás en el campo **Modelo** de Elite Intel.

Para detener el servidor:

```shell
lms server stop
```

> ⚠️ **Importante:** El servidor de LM Studio **no** sobrevive a los reinicios. Ejecuta de nuevo los tres comandos del paso 3 tras cada reinicio, o configura el auto-inicio opcional que se describe a continuación.

---

### Paso 4 – (Opcional) Auto-inicio en el arranque

Para iniciar LM Studio automáticamente, configúralo como un servicio **de usuario** de systemd. Esto se ejecuta bajo tu propia sesión en lugar de como servicio del sistema. Se inicia después de que el entorno de escritorio esté activo. No se requiere acceso de root.

Encuentra tu ID de usuario (sustituye el nombre de usuario por tu nombre real):
```shell
id -u YOUR_USER_NAME
```

Recuerda este número. Lo necesitarás para la configuración más adelante.

Crea el directorio de usuario de systemd si no existe:

```shell
mkdir -p ~/.config/systemd/user
```

Crea el archivo de servicio:

```shell
nano ~/.config/systemd/user/lmstudio.service
```

Pega esto:

```ini
[Unit]
Description=LM Studio Server
After=network.target

[Service]
Type=oneshot
RemainAfterExit=yes
Environment="HOME=/home/YOUR_USERNAME"
Environment="PATH=/home/YOUR_USERNAME/.lmstudio/bin:/usr/local/bin:/usr/bin:/bin"
Environment="XDG_RUNTIME_DIR=/run/user/YOUR_UID"
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon up
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms load google/gemma-4-e4b --yes --context-length 8192 --gpu max
ExecStart=/home/YOUR_USERNAME/.lmstudio/bin/lms server start --bind 127.0.0.1 --port 1234
ExecStop=/home/YOUR_USERNAME/.lmstudio/bin/lms server stop
ExecStopPost=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon down

[Install]
WantedBy=default.target
```

Sustituye `YOUR_USERNAME` por tu nombre de usuario de Linux y `YOUR_UID` por tu ID de usuario. Para encontrar tu UID:

```shell
id -u
```

> **¿El juego en otro PC?** Con `--bind 127.0.0.1` el servidor solo es accesible desde esta máquina. Si Elite Intel se ejecuta en otro PC, cámbialo a `--bind 0.0.0.0` para que otras máquinas de tu red puedan acceder a él.

> ⚠️ **¿Por qué `XDG_RUNTIME_DIR`?** Los servicios de usuario se ejecutan en un entorno simplificado que puede no incluir las variables de sesión. LM Studio usa `XDG_RUNTIME_DIR` para IPC. Sin él, el servicio puede fallar silenciosamente incluso cuando `lms` funciona correctamente desde el terminal. Esta es la causa más común de fallo del servicio cuando la ejecución manual funciona.

Activa e inicia el servicio:

```shell
systemctl --user daemon-reload
systemctl --user enable lmstudio.service
systemctl --user start lmstudio.service
```

Verifica que está en ejecución:

```shell
systemctl --user status lmstudio.service
curl http://localhost:1234/v1/models
```

> **Resolución de problemas:** Si el servicio falla, comprueba el diario:
> ```shell
> journalctl --user -xeu lmstudio.service --no-pager | tail -40
> ```
> Si informa «Failed to load model», ejecuta `lms ls` y confirma que el nombre del modelo coincide exactamente con lo que hay en el archivo de servicio.

---

### Paso 4b – (Opcional) Corregir la inferencia lenta después del arranque

Algunos usuarios experimentan respuestas de inferencia lentas cuando LM Studio se inicia en el arranque. El problema se resuelve inmediatamente tras un reinicio manual del servicio. La causa no está confirmada.

Primero, comprueba que la línea `lms load` de tu archivo de servicio termina en `--gpu max`. Las versiones anteriores de esta página lo omitían. Sin ello, LM Studio decide por su cuenta cuánto del modelo va a la GPU, y en el arranque puede dejar parte del modelo en la CPU. Si lo añades, ejecuta `systemctl --user daemon-reload` y reinicia para comprobarlo.

Si después siguen apareciendo respuestas lentas tras un reinicio y se resuelven con un reinicio manual, este temporizador automatiza la corrección.

Crea un servicio complementario:

```shell
nano ~/.config/systemd/user/lmstudio-restart.service
```

```ini
[Unit]
Description=LM Studio post-boot restart
After=lmstudio.service

[Service]
Type=oneshot
ExecStart=systemctl --user restart lmstudio.service
```

Crea el temporizador:

```shell
nano ~/.config/systemd/user/lmstudio-restart.timer
```

```ini
[Unit]
Description=Restart LM Studio 2 minutes after boot

[Timer]
OnBootSec=2min
Unit=lmstudio-restart.service

[Install]
WantedBy=timers.target
```

Activa el temporizador:

```shell
systemctl --user daemon-reload
systemctl --user enable --now lmstudio-restart.timer
```

El temporizador espera 2 minutos después del arranque, reinicia el servicio de LM Studio una vez y luego permanece inactivo. Si no experimentas inferencia lenta, este paso no es necesario.

---

### Paso 5 – Configurar Elite Intel

En Elite Intel, abre **Servicios → Servicios de IA**. En **Modelo de lenguaje (LLM)**, selecciona **LMStudio**:

- **Dirección**: `http://localhost:1234/v1/chat/completions` (el valor por defecto). Si LM Studio está en otra máquina, sustituye `localhost` por la IP de esa máquina.
- **Modelo**: pega la cadena de ID del modelo de `curl http://localhost:1234/v1/models`. Un solo modelo atiende tanto los comandos como las consultas.

Un LLM local no necesita clave de API.

Haz clic en **Guardar**. Elite Intel reinicia el modelo de lenguaje por sí solo.

---

Comunidad 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
