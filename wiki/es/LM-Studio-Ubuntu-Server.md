# LM Studio en un servidor Ubuntu headless

> Guía proporcionada por **Wim**

> **Elite Intel:** esta guía monta una máquina dedicada en la red local que ejecuta LM Studio para Elite Intel. Al terminar, pon en **Dirección** de la [pestaña Servicios](UI-Settings-Tab) `http://SERVER_IP:1234/v1/chat/completions` y carga el modelo compatible, **`google/gemma-4-e4b`**. El servidor debe escuchar más allá de localhost (`--bind 0.0.0.0`, ver Parte 2 § 9) y el cortafuegos debe permitir el PC de juego.

---

# Parte 1: Ubuntu Server como anfitrión de IA local / LM Studio

**Guía de instalación base limpia**  
**Base recomendada:** Ubuntu Server 24.04 LTS (amd64)  
**Fecha del documento:** 12 de septiembre de 2026

## 1. Propósito

Esta guía prepara una máquina Ubuntu Server limpia para una carga de IA local headless como LM Studio / llmster. Se detiene a propósito antes de instalar LM Studio. El objetivo es un servidor pequeño y comprensible con red, SSH, actualizaciones y cortafuegos funcionando y, cuando corresponda, un controlador gráfico NVIDIA validado.

No es una guía de recuperación ni de diagnóstico de hardware. Supone que la máquina es estable con los ajustes de CPU/RAM de fábrica y que los datos importantes ya tienen copia de seguridad.

## 2. ¿Qué versión de Ubuntu?

Para un servidor de IA conservador y de larga duración, esta guía usa **Ubuntu Server 24.04 LTS (Noble Numbat)**. Canonical ofrece mantenimiento de seguridad estándar para Ubuntu 24.04 LTS hasta mayo de 2029. Ubuntu 26.04 LTS también está disponible y es la LTS más reciente, pero la 24.04 sigue plenamente soportada y es una base madura para sistemas donde la estabilidad importa más que tener el kernel y el espacio de usuario más nuevos.

Para un despliegue totalmente nuevo en el que hardware reciente requiera la pila de kernel/controladores más actual, Ubuntu 26.04 LTS también es una opción razonable. Los comandos de esta guía son intencionadamente convencionales y deberían seguir siendo muy parecidos.

Referencias oficiales:
- https://ubuntu.com/about/release-cycle
- https://documentation.ubuntu.com/release-notes/24.04/
- https://ubuntu.com/server/docs/tutorial/basic-installation/

## 3. Antes de la instalación

Ten preparado:

- Una máquina Intel/AMD de 64 bits (amd64) para esta guía.
- Un USB con la ISO de Ubuntu Server.
- Ethernet por cable si es posible.
- El nombre de equipo, nombre de usuario y una contraseña robusta previstos.
- La decisión de qué disco puede borrar Ubuntu.
- Para inferencia por GPU, una GPU NVIDIA compatible, bien instalada y alimentada.

Si la máquina funcionaba antes con CPU o RAM con overclock, vuelve a ajustes de fábrica estables antes de ponerla en servicio como servidor. Los entornos de ejecución de IA y la compilación de módulos del kernel pueden sacar a la luz inestabilidades que cargas más ligeras no muestran.

## 4. Instalar Ubuntu Server

Arranca el instalador de Ubuntu Server y sigue el flujo normal. Para un servidor dedicado sencillo:

1. Elige el idioma y la distribución del teclado.
2. Deja que el instalador configure la red por cable con DHCP al principio.
3. Usa el disco de sistema previsto. **El particionado puede destruir datos existentes**, así que comprueba con cuidado el disco seleccionado.
4. Crea el usuario administrador y el nombre de equipo.
5. Selecciona **Install OpenSSH server** cuando se ofrezca. Si no se seleccionó, puede instalarse tras el primer arranque.
6. Omite los snaps opcionales del servidor salvo que los necesites expresamente.
7. Completa la instalación y reinicia.

## 5. Primer inicio de sesión y actualización del sistema

Inicia sesión localmente o por SSH y actualiza el índice de paquetes y los paquetes instalados:

```bash
sudo apt update
sudo apt upgrade -y
```

Si Ubuntu indica que hace falta reiniciar:

```bash
sudo reboot
```

Tras volver a conectarte, confirma la versión y el kernel:

```bash
lsb_release -a
uname -r
```

## 6. Instalar un pequeño kit de administración

Los siguientes paquetes son útiles en un servidor de IA headless:

```bash
sudo apt install -y \
  openssh-server \
  curl \
  ca-certificates \
  jq \
  htop \
  nvtop \
  ufw
```

`curl` lo usa el instalador de LM Studio y las pruebas de la API, `jq` hace legible la salida JSON, `htop` vigila CPU/RAM, `nvtop` vigila las GPU y `ufw` ofrece un cortafuegos de host sencillo.

Confirma que SSH está activo:

```bash
systemctl status ssh --no-pager
```

## 7. Acceso SSH

Averigua la dirección actual del servidor:

```bash
ip -br address
```

Desde otra máquina de la red local:

```bash
ssh YOUR_USER@SERVER_IP
```

Para el uso habitual, la autenticación SSH con clave pública es preferible a teclear una contraseña una y otra vez. En la máquina cliente:

```bash
ssh-keygen -t ed25519
ssh-copy-id YOUR_USER@SERVER_IP
```

Prueba una sesión SSH nueva antes de cambiar ajustes de autenticación por contraseña. No te quedes sin acceso a un servidor remoto.

Referencia oficial de OpenSSH:
https://ubuntu.com/server/docs/how-to/security/openssh-server/

## 8. Cortafuegos básico

Antes de activar UFW, permite SSH:

```bash
sudo ufw allow OpenSSH
sudo ufw enable
sudo ufw status verbose
```

En un servidor de IA dedicado en casa/red local, es mejor exponer los puertos de aplicación solo a clientes de confianza o a la red local de confianza, en vez de a todas las interfaces de red sin distinción.

El puerto de API por defecto de LM Studio es el 1234. **No lo abras todavía.** Añade la regla adecuada cuando LM Studio esté instalado y sepas qué clientes deben acceder a él.

Referencia oficial del cortafuegos:
https://ubuntu.com/server/docs/how-to/security/firewalls/

## 9. Controlador gráfico NVIDIA

Omite esta sección si el servidor usará inferencia por CPU o un acelerador que no sea NVIDIA.

Primero actualiza el sistema y reinicia si es necesario. Después deja que Ubuntu identifique los controladores de cómputo NVIDIA adecuados:

```bash
sudo ubuntu-drivers list --gpgpu
```

Para una carga de cómputo/servidor, Ubuntu documenta la instalación automática con:

```bash
sudo ubuntu-drivers install --gpgpu
```

Reinicia tras la instalación:

```bash
sudo reboot
```

Y valida la GPU:

```bash
nvidia-smi
```

Un `nvidia-smi` correcto debe mostrar la GPU, la versión del controlador, la temperatura, la VRAM y la tabla de procesos.

### Nota sobre Secure Boot

Secure Boot requiere módulos del kernel firmados y compatibles. La herramienta `ubuntu-drivers` de Ubuntu es la vía de instalación preferida porque está diseñada para seleccionar adecuadamente controladores empaquetados por Ubuntu. Si el módulo NVIDIA no carga, comprueba el estado de Secure Boot antes de reinstalar controladores una y otra vez:

```bash
mokutil --sb-state
```

No desactives Secure Boot como primer paso rutinario. Si tu combinación de firmware/controlador tiene problemas de firma o de registro MOK, resuélvelo de forma deliberada y documenta el cambio en la BIOS.

Referencia oficial de NVIDIA en Ubuntu:
https://ubuntu.com/server/docs/how-to/graphics/install-nvidia-drivers/

## 10. Comprobación final

Ejecuta:

```bash
hostnamectl
ip -br address
systemctl is-active ssh
sudo ufw status
free -h
df -h /
nvidia-smi    # NVIDIA systems only
```

En este punto la máquina debería tener:

- una instalación de Ubuntu LTS soportada;
- los paquetes actualizados;
- conectividad de red local funcionando;
- acceso SSH funcionando;
- un cortafuegos básico;
- herramientas de administración útiles;
- un controlador NVIDIA validado, si corresponde.

El servidor ya está listo para instalar LM Studio / llmster.

## 11. Lo que no se cubre a propósito

Esta base no configura almacenamiento NAS/Samba, Docker, bases de datos, servidores web, RAID, particionados personalizados, VPN, Pi-hole/DNS, entornos de escritorio, CUDA Toolkit ni software específico de aplicaciones. Eso corresponde a guías de despliegue separadas.

Un CUDA Toolkit independiente **no es necesario solo para ejecutar LM Studio con una GPU NVIDIA compatible**; usa la pila de ejecución/controladores que soporta LM Studio, salvo que otra aplicación requiera expresamente el kit de desarrollo de CUDA.

## 12. Mantenimiento básico

Aplica periódicamente las actualizaciones normales:

```bash
sudo apt update
sudo apt upgrade
```

Comprueba si hay un reinicio pendiente:

```bash
if [ -f /run/reboot-required ]; then
  cat /run/reboot-required
fi
```

Antes de una actualización de versión mayor de Ubuntu, lee las notas de esa versión y haz copia de seguridad del servidor. Las actualizaciones de versión mayor son distintas de las actualizaciones normales de paquetes.

---

## Fuentes oficiales

- Documentación de Ubuntu Server: https://ubuntu.com/server/docs/
- Ciclo de versiones de Ubuntu: https://ubuntu.com/about/release-cycle
- Notas de versión de Ubuntu 24.04 LTS: https://documentation.ubuntu.com/release-notes/24.04/
- Instalación básica: https://ubuntu.com/server/docs/tutorial/basic-installation/
- Gestión de paquetes: https://ubuntu.com/server/docs/how-to/software/package-management/
- OpenSSH: https://ubuntu.com/server/docs/how-to/security/openssh-server/
- UFW/cortafuegos: https://ubuntu.com/server/docs/how-to/security/firewalls/
- Controladores NVIDIA: https://ubuntu.com/server/docs/how-to/graphics/install-nvidia-drivers/

---

# Parte 2: servidor LM Studio headless y gestión de modelos

**Instalación, elección de modelo, cuantización, servicio de API y cambio de modelo**  
**Destino:** servidor Linux/Ubuntu headless  
**Fecha del documento:** 12 de septiembre de 2026

## 1. Propósito

Esta guía parte de un servidor Ubuntu limpio que ya tiene red y SSH funcionando y, para la aceleración NVIDIA, un controlador de GPU validado. Instala el entorno headless de LM Studio, explica cómo elegir y descargar modelos, expone la API local si se desea y proporciona un selector de modelos interactivo en Bash.

El componente nativo de servidor de LM Studio se llama **llmster**. El programa de línea de comandos que lo controla se llama **lms**. No hace falta escritorio gráfico.

Documentación oficial headless:
https://lmstudio.ai/docs/developer/core/headless

## 2. Tres nombres que conviene entender

- **LM Studio** es el producto y el ecosistema en conjunto.
- **llmster** es el demonio headless/servidor. Ejecuta el servicio de inferencia sin interfaz gráfica.
- **lms** es la CLI para descargar, cargar, descargar de memoria, inspeccionar y servir modelos.

En un servidor de inferencia Linux dedicado, llmster + lms suele ser la configuración más limpia.

## 3. Instalar llmster / lms

Instala como el usuario normal que ejecutará el servicio, no como root:

```bash
curl -fsSL https://lmstudio.ai/install.sh | bash
```

Arranca el demonio:

```bash
lms daemon up
```

Comprueba la CLI:

```bash
lms --version
lms --help
```

Si el shell no encuentra `lms`, abre un nuevo shell de inicio de sesión o añade el directorio de binarios de LM Studio al PATH del shell actual:

```bash
export PATH="$HOME/.lmstudio/bin:$PATH"
```

Para dejarlo fijado en Bash:

```bash
echo 'export PATH="$HOME/.lmstudio/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

## 4. Buscar y descargar modelos

Ejecutar `lms get` sin modelo muestra recomendaciones:

```bash
lms get
```

Descarga un modelo por su nombre de catálogo:

```bash
lms get publisher/model-name
```

El modelo exacto se deja a propósito a elección del lector en lugar de fijarlo en esta guía.

Lista los modelos descargados:

```bash
lms ls
```

Muestra las variantes/cuantizaciones descargadas cuando existan:

```bash
lms ls --variants
```

La salida JSON es útil para scripts:

```bash
lms ls --json | jq
```

LM Studio puede descargar modelos compatibles desde su catálogo/el ecosistema de Hugging Face. La CLI actual también permite indicar una cuantización al descargar, por ejemplo:

```bash
lms get publisher/model-name@q4_k_m
```

Usa los nombres de cuantización que realmente se ofrecen para ese modelo; no todos los modelos se publican en todos los formatos.

Documentación oficial de descarga de modelos:
https://lmstudio.ai/docs/cli/local-models/get

## 5. Tamaño del modelo frente a cuantización: explicación para principiantes

Un modelo contiene miles de millones de valores numéricos aprendidos llamados **pesos**. Una mayor precisión numérica guarda esos valores con más fidelidad, pero ocupa más disco y más RAM/VRAM. La **cuantización** comprime el modelo representando esos valores con menos bits.

Una imagen útil es una grabación de audio de alta calidad: Q8 conserva más detalle, Q6 comprime algo más y Q4 todavía más. Siguen siendo versiones del mismo modelo entrenado; Q8 no añade por arte de magia nuevo entrenamiento ni conocimiento.

Interpretación típica:

| Cuantización | Uso de memoria | Fidelidad | Papel práctico |
|---|---|---|---|
| Clase Q4 | Menor | Buena | Excelente punto de partida; a menudo suficiente para chat/uso de herramientas |
| Clase Q5/Q6 | Media | Mayor | Buen equilibrio entre calidad y memoria |
| Clase Q8 | Mayor | Muy alta | Úsala cuando la fidelidad extra ayude de forma medible |

La propia recomendación de LM Studio es elegir una opción de 4 bits o superior cuando la máquina pueda soportarla.

### La regla importante

**No elijas Q8 solo porque 8 es mayor que 4.** Elige la cuantización más pequeña que realice tu carga real de forma fiable. La precisión extra solo vale la pena si mejora las respuestas, el razonamiento, el uso de herramientas o la estabilidad lo suficiente como para justificar su mayor consumo permanente de memoria.

## 6. El tamaño del modelo no es su inteligencia

La cuantización y el tamaño del modelo son decisiones distintas.

Un modelo más grande y capaz con cuantización moderada puede superar a un modelo más pequeño en Q8 en razonamientos difíciles. Por el contrario, un modelo pequeño en Q4 puede ser ideal para interpretar órdenes rápidamente.

Al elegir un modelo, ten en cuenta:

1. **Tarea:** chat, programación, llamada a herramientas/funciones, visión, análisis de datos, embeddings, etc.
2. **Arquitectura/número de parámetros:** los modelos más grandes suelen ser más capaces, pero necesitan más memoria y pueden ser más lentos.
3. **Cuantización:** Q4/Q6/Q8 cambia el equilibrio fidelidad/memoria de ese modelo.
4. **Longitud de contexto:** un contexto mayor puede consumir memoria adicional.
5. **Encaje en la GPU:** mantener el modelo y el contexto de trabajo en VRAM suele dar la mejor inferencia por GPU.
6. **Soporte de herramientas:** si una aplicación requiere llamada nativa a funciones/herramientas, elige un modelo entrenado y compatible para ello.

La elección correcta depende de la carga de trabajo. Prueba con prompts reales en vez de fiarte solo de las puntuaciones de benchmarks.

## 7. Estimar la memoria antes de cargar

LM Studio puede estimar el uso de recursos sin cargar el modelo:

```bash
lms load publisher/model-name \
  --context-length 8192 \
  --gpu max \
  --estimate-only
```

Es una estimación, no una garantía. Valida el modelo realmente cargado con `lms ps`, `nvidia-smi` e inferencia real.

## 8. Cargar y descargar modelos

Carga un modelo descargado:

```bash
lms load publisher/model-name --context-length 8192 --gpu max
```

`--gpu max` pide a LM Studio que descargue en la GPU todo lo posible. Si se omite, LM Studio puede elegir automáticamente.

Mira qué está cargado:

```bash
lms ps
```

Descarga de memoria un modelo de forma interactiva o por su clave:

```bash
lms unload publisher/model-name
```

Descárgalo todo:

```bash
lms unload --all
```

Referencia oficial:
https://lmstudio.ai/docs/cli/local-models/load

## 9. Iniciar la API HTTP

Para acceso solo desde el propio servidor:

```bash
lms server start --bind 127.0.0.1 --port 1234
```

Comprueba:

```bash
curl -s http://127.0.0.1:1234/v1/models | jq
```

Para escuchar en todas las interfaces IPv4:

```bash
lms server start --bind 0.0.0.0 --port 1234
```

**Seguridad:** enlazar a `0.0.0.0` expone el servicio a todas las redes alcanzables. La documentación de LM Studio recomienda autenticación al exponerlo más allá de localhost. Como mínimo, restringe el puerto 1234 con UFW a clientes de confianza de la red local o a una subred de confianza. No expongas nunca una API de inferencia sin autenticación directamente a Internet.

Ejemplo para un cliente de confianza:

```bash
sudo ufw allow from CLIENT_IP to any port 1234 proto tcp
```

Ejemplo para una subred privada de confianza (sustitúyela por la tuya):

```bash
sudo ufw allow from 192.168.1.0/24 to any port 1234 proto tcp
```

Referencia oficial del servidor:
https://lmstudio.ai/docs/cli/serve/server-start

## 10. Probar una petición de inferencia

La API compatible con OpenAI puede probarse con `curl`:

```bash
curl -s http://127.0.0.1:1234/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "publisher/model-name",
    "messages": [
      {"role": "user", "content": "Reply with exactly: Local inference works."}
    ],
    "temperature": 0
  }' | jq
```

Sustituye el identificador del modelo por el que muestran `lms ps` o `/v1/models`.

## 11. Arrancar LM Studio automáticamente al inicio

LM Studio documenta cómo ejecutar llmster como tarea de arranque de systemd. Sustituye `YOUR_USER`, la clave del modelo, la longitud de contexto y el enlace de red por tus propios valores.

Crea el servicio:

```bash
sudo nano /etc/systemd/system/lmstudio.service
```

Ejemplo:

```ini
[Unit]
Description=LM Studio Headless Server
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
User=YOUR_USER
Environment="HOME=/home/YOUR_USER"

ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms daemon up
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms load publisher/model-name --context-length 8192 --gpu max --yes
ExecStart=/home/YOUR_USER/.lmstudio/bin/lms server start --bind 127.0.0.1 --port 1234
ExecStop=/home/YOUR_USER/.lmstudio/bin/lms daemon down

[Install]
WantedBy=multi-user.target
```

Actívalo e inícialo:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now lmstudio.service
systemctl status lmstudio.service --no-pager
```

Y comprueba:

```bash
lms ps
curl -s http://127.0.0.1:1234/v1/models | jq
```

### Si un arranque rápido de la CLI produce `Text file busy`

En algunos sistemas, una segunda llamada a `lms` justo después de `lms daemon up` puede competir con el arranque del demonio. Solo si te encuentras realmente con ese error, añade una pequeña espera entre las dos líneas `ExecStartPre`:

```ini
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms daemon up
ExecStartPre=/bin/sleep 5
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms load ...
```

No añadas soluciones provisionales por adelantado si el servicio sencillo funciona.

Referencia oficial de la tarea de arranque:
https://lmstudio.ai/docs/developer/core/headless_llmster

## 12. Selector de modelos interactivo

El script complementario `lmstudio-model-switcher.sh` (listado completo al final de esta página) ofrece un menú numerado sencillo con las **claves de los modelos** descargados. Este script:

1. se asegura de que llmster está en marcha;
2. lee los modelos descargados de `lms ls --json`;
3. pregunta qué modelo usar;
4. descarga de memoria el modelo o modelos cargados;
5. carga el modelo elegido con una política de contexto/GPU configurable;
6. mantiene o inicia el servicio de API según haga falta;
7. muestra `lms ps` y comprueba `/v1/models`.

Instálalo:

```bash
mkdir -p ~/bin
cp lmstudio-model-switcher.sh ~/bin/
chmod +x ~/bin/lmstudio-model-switcher.sh
```

Ejecútalo con sus valores por defecto prudentes (contexto de 8192 tokens, GPU max, API en localhost):

```bash
~/bin/lmstudio-model-switcher.sh
```

Cambia los ajustes para una sola ejecución:

```bash
LMS_CONTEXT_LENGTH=16384 \
LMS_GPU=max \
LMS_BIND=0.0.0.0 \
LMS_PORT=1234 \
~/bin/lmstudio-model-switcher.sh
```

### Importante: variantes / cuantizaciones de modelos

LM Studio distingue entre una **clave de modelo lógica** y sus variantes de cuantización descargadas. Descargar `publisher/model@q4_k_m` elige qué cuantización guardar, mientras que la carga suele usar la clave de modelo lógica que indica `lms ls`.

Si guardas varias cuantizaciones del mismo modelo lógico, examínalas con:

```bash
lms ls --variants
lms ls --json | jq
```

El comportamiento de selección de variantes ha cambiado entre versiones de la CLI de LM Studio, así que revisa `lms get --help` / `lms load --help` de la versión instalada antes de automatizar el cambio de cuantización. El script incluido cambia a propósito de **clave de modelo**, que es la operación estable y comprensible para una guía general.

## 13. Cambiar el modelo por defecto al arrancar

El selector interactivo cambia lo que está cargado **ahora**. Si el servicio de systemd carga explícitamente un modelo al arrancar, un reinicio volverá a cargar el modelo escrito en `/etc/systemd/system/lmstudio.service`.

Para que otro modelo sea el predeterminado permanente al arrancar:

```bash
sudo nano /etc/systemd/system/lmstudio.service
```

Cambia la línea `lms load ...` y luego:

```bash
sudo systemctl daemon-reload
sudo systemctl restart lmstudio.service
```

Un despliegue más avanzado puede usar la carga de modelos JIT en lugar de fijar uno al arrancar. LM Studio admite carga JIT y desalojo automático; es útil cuando las aplicaciones eligen los modelos de forma dinámica.

## 14. Vigilar el servidor

Modelos cargados:

```bash
lms ps
```

Instantánea de la GPU:

```bash
nvidia-smi
```

Actualizar cada dos segundos:

```bash
watch -n 2 nvidia-smi
```

CPU/RAM:

```bash
htop
```

Monitor de GPU interactivo:

```bash
nvtop
```

Los registros de LM Studio también pueden seguirse desde la CLI:

```bash
lms log stream
```

## 15. Un flujo práctico para elegir modelo

Una forma sensata de elegir un modelo:

1. Define primero la carga de trabajo real.
2. Identifica modelos con la capacidad necesaria (por ejemplo uso de herramientas, programación, visión o razonamiento).
3. Descarga primero una variante de clase Q4 cuando exista.
4. Usa `--estimate-only` antes de cargar si la memoria va justa.
5. Prueba prompts y llamadas a herramientas reales.
6. Pasa a Q5/Q6 solo si Q4 pierde fiabilidad o calidad de forma apreciable.
7. Pasa a Q8 solo si la mejora es visible y compensa la RAM/VRAM extra.
8. Si ni siquiera el Q8 de un modelo pequeño es suficientemente capaz, prueba un **modelo más grande/mejor con cuantización moderada** en lugar de suponer que aún más precisión resolverá el problema.

Así la elección del modelo es empírica y no numérica: **el mejor modelo es la configuración más pequeña/rápida que cumple de forma fiable las necesidades de la aplicación.**

## 16. Lista de comprobación para solucionar problemas

Si LM Studio no se comporta como se espera, comprueba en este orden:

```bash
lms --version
lms daemon up
lms ls
lms ps
lms server status
nvidia-smi
free -h
```

Para un modelo que quizá no quepa, estima primero:

```bash
lms load publisher/model-name --context-length 8192 --gpu max --estimate-only
```

Para un cliente remoto que no puede conectarse, comprueba primero en local:

```bash
curl -s http://127.0.0.1:1234/v1/models | jq
```

Después comprueba que el servidor escucha en la dirección y el puerto previstos:

```bash
ss -ltnp | grep 1234
sudo ufw status numbered
```

Si el acceso local funciona pero el remoto no, revisa la dirección de enlace, las reglas del cortafuegos, el enrutamiento y la autenticación de la API antes de cambiar de modelo.

## 17. Lo que esta guía deja abierto a propósito

No prescribe un modelo universal. El hardware, la carga de trabajo, la longitud de contexto, la arquitectura del modelo y las capacidades necesarias varían demasiado para ello. Tampoco requiere un CUDA Toolkit aparte, Docker, una interfaz de escritorio ni integraciones específicas de aplicaciones.

Una vez estable el servidor, las guías específicas de cada aplicación pueden definir su propio modelo, contexto, endpoint de API, autenticación y requisitos de rendimiento.

---

## Fuentes oficiales

- CLI de LM Studio: https://lmstudio.ai/docs/cli
- Headless / llmster: https://lmstudio.ai/docs/developer/core/headless
- Tarea de arranque en Linux: https://lmstudio.ai/docs/developer/core/headless_llmster
- Descargar modelos (`lms get`): https://lmstudio.ai/docs/cli/local-models/get
- Cargar/descargar modelos (`lms load`): https://lmstudio.ai/docs/cli/local-models/load
- Iniciar el servidor de API: https://lmstudio.ai/docs/cli/serve/server-start
- Descarga de modelos / cuantización: https://lmstudio.ai/docs/app/basics/download-model
- Inicio rápido de la API REST: https://lmstudio.ai/docs/developer/rest/quickstart

---

## Apéndice: `lmstudio-model-switcher.sh`

Guárdalo como `lmstudio-model-switcher.sh` e instálalo como se indica en la Parte 2 § 12.

```bash
#!/usr/bin/env bash
set -euo pipefail

# LM Studio interactive model switcher
# Optional environment overrides:
#   LMS_CONTEXT_LENGTH=16384 LMS_GPU=max LMS_BIND=0.0.0.0 LMS_PORT=1234 ./lmstudio-model-switcher.sh

CONTEXT_LENGTH="${LMS_CONTEXT_LENGTH:-8192}"
GPU="${LMS_GPU:-max}"
BIND="${LMS_BIND:-127.0.0.1}"
PORT="${LMS_PORT:-1234}"

need() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Error: '$1' is required but was not found in PATH." >&2
    exit 1
  }
}

need lms
need jq

# Make sure the headless daemon is available.
lms daemon up >/dev/null

mapfile -t MODELS < <(
  lms ls --json | jq -r '
    if type == "array" then .[]
    elif has("models") then .models[]
    else empty end
    | .modelKey // .model_key // empty
  ' | awk 'NF' | sort -u
)

if ((${#MODELS[@]} == 0)); then
  echo "No downloaded models were found."
  echo "Download one first, for example: lms get <publisher/model>"
  exit 1
fi

echo
echo "Downloaded LM Studio models"
echo "---------------------------"
for i in "${!MODELS[@]}"; do
  printf '  %2d) %s\n' "$((i + 1))" "${MODELS[$i]}"
done
echo "   0) Cancel"
echo

while true; do
  read -r -p "Choose a model [0-${#MODELS[@]}]: " CHOICE
  [[ "$CHOICE" =~ ^[0-9]+$ ]] || { echo "Please enter a number."; continue; }
  (( CHOICE == 0 )) && exit 0
  (( CHOICE >= 1 && CHOICE <= ${#MODELS[@]} )) && break
  echo "Choice out of range."
done

MODEL="${MODELS[$((CHOICE - 1))]}"

echo
echo "Selected: $MODEL"
echo "Context : $CONTEXT_LENGTH"
echo "GPU     : $GPU"
echo

read -r -p "Unload the current model and load this one? [y/N]: " CONFIRM
[[ "$CONFIRM" =~ ^[Yy]$ ]] || { echo "Cancelled."; exit 0; }

echo "Unloading currently loaded models..."
lms unload --all || true

echo "Loading $MODEL..."
lms load "$MODEL" --context-length "$CONTEXT_LENGTH" --gpu "$GPU" --yes

# Start the HTTP server only if it is not already running.
if ! lms server status >/dev/null 2>&1; then
  echo "Starting LM Studio server on $BIND:$PORT..."
  lms server start --bind "$BIND" --port "$PORT"
fi

echo
echo "Current LM Studio state:"
lms ps

echo
echo "API model list:"
if command -v curl >/dev/null 2>&1; then
  curl -fsS "http://127.0.0.1:${PORT}/v1/models" | jq . || \
    echo "API check failed. If the server uses another port, set LMS_PORT accordingly."
else
  echo "curl is not installed; skipping API check."
fi
```
