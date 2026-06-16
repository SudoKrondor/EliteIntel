# Opciones de interfaz y configuración

### Pestaña IA <img src="images/ai.png" class="inline" height="20" alt="AI">

Esta es la pestaña principal/predeterminada.

|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![tab-ai-buttons.png](images/tab-ai-buttons.png) | - **Iniciar / Detener servicios**: Activa o desactiva la pila de IA.<br/>- **Activo/Reposo**: En modo activo la app escucha todo el tiempo; en modo reposo ignora la entrada salvo que se presione el botón PPH, se use la palabra de paso "Listen" o se emita el comando "Wake up!".<br/>- **Superposición OBS**: Muestra una ventana de superposición negra con la interacción Comandante / IA. Agrégala a OBS con fondo de clave de color negro.<br/>- **Dispositivos de audio**: Selecciona el dispositivo de audio de entrada/salida. **Calibrar audio**: Ejecuta la calibración de audio para un mejor rendimiento. |
|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |


---

### Pestaña Jugador <img src="images/controller.png" class="inline" height="20" alt="Player">

![playertab](images/tab-player.png)

- **Nombre del Comandante**: Usa este campo para anular tu nombre en el juego en la síntesis de voz.
- **Opciones de nave**: Puedes activar y desactivar estas automatizaciones. Útil para Comandantes con discapacidades.
- **Gestión de flota**: Asigna voces, personalidades y cadencia a naves individuales. La personalidad solo funciona con LLM en la nube. El icono de engranaje abre las propiedades de la nave, como el auto-honk y el perfil comercial.

![popup-ship-properties.png](images/popup-ship-properties.png)

- **Honk del sistema al entrar**: Selecciona el grupo de fuego y el gatillo. Si esta opción está marcada, la nave realizará el escaneo de descubrimiento al entrar. Si tu HUD está en modo Combate, cambiará a Análisis, realizará el escaneo y volverá.
- **Personalizar tu perfil comercial**: Estos parámetros pueden establecerse en la interfaz de usuario o mediante comandos de voz: "alter/change trade profile set [parámetro] to [valor]"

---



### Pestaña Acciones <img src="images/keys-binding.png" class="inline" height="20" alt="Actions">

![tab-actions.png](images/tab-actions.png)

La pestaña **Acciones / Vínculos** tiene tres secciones: Vínculos, Comandos integrados y Comandos personalizados.

- El directorio **Vínculos** es donde se encuentra el archivo de vínculos del juego. Sin él, la app no puede operar los controles del juego.
- **Perfil** es tu perfil de vínculos actual en el juego.
- **Archivo** es el archivo que contiene los vínculos que estás usando actualmente.

Puedes modificar tus vínculos usando esta pantalla y guardarlos como un nuevo perfil.

__NOTA: Los HOTAS/MANDOS se muestran pero no se pueden configurar desde esta pantalla. Solo vínculos de teclado (sujeto a cambios en el futuro)__


**Acciones / Comandos integrados**

![tab-action-build-in-commands.png](images/tab-action-build-in-commands.png)


Proporciona una lista de comandos integrados. Hacer doble clic en uno mostrará un cuadro de diálogo con información sobre el comando y te permitirá proponer una mejor traducción para la localización.

---

### Ajustes / Pestaña LLM local <img src="images/settings.png" class="inline" height="20" alt="Settings">
- Establece la dirección de tu servidor de inferencia. Por defecto es `localhost` con la URL de Ollama.
- Proporciona los nombres de los modelos a usar. Consulta la [guía de LLM local](installing-local-llms).
- Botones de opción **Host LLM**: Selecciona entre Ollama y LM Studio.
- **Casilla Usar**: Actívala para usar el modelo local en lugar de la nube.

---

### Ajustes / Audio <img src="images/mic.png" class="inline" height="20" alt="Audio">
- **Volumen de voz**: Controla el volumen de la síntesis de voz.
- **Velocidad de voz TTS**: Controla la velocidad de la síntesis de voz.
- **Volumen del pitido**: Controla el volumen del indicador sonoro. Indica que el STT ha terminado de procesar y que el LLM ha recibido la entrada.
- **Hilos STT**: Establece la asignación de hilos para el procesamiento STT. Es un ajuste de mínimo/máximo. La app solicita el mínimo pero usa lo que el procesador proporciona. Los hilos se liberan tras completar el procesamiento.
- **Usar síntesis de voz local**: Anula la clave TTS en la nube y usa el TTS local.
- **Visualizador de onda de audio**: Muestra un gráfico dinámico de la entrada de audio. Muestra el suelo de ruido, la señal de audio, las zonas de puerta y el recorte si está presente.


### Ajustes / Pestaña LLM en la nube <img src="images/cloud.png" class="inline" height="20" alt="Cloud">
- **Clave LLM en la nube**: Introduce tu clave API. Proveedores admitidos: Gemini, OpenAI, Grok, Mistral, Deepseek y Anthropic/Claude.
- **Clave TTS en la nube**: Introduce tu clave API. Proveedor admitido: Google.
- **Nota**: Desmarca la casilla "Usar" en LLM local. Esta anula la clave LLM en la nube.


---

**LLM (cerebro de IA)**

*Opción en la nube:* Introduce tu clave API para Mistral, xAI, OpenAI o Anthropic/Claude. La app usa un modelo fijo por proveedor:
- **Mistral**: 'mistral-small-2506' (Gratis con límite por hora)
- **xAI**: `grok-4-1-fast-non-reasoning`
- **OpenAI**: `gpt-4.1-mini` (comandos) / `gpt-5.2` (consultas)
- **Gemini Generative Language API**: `gemini-3.1-flash-lite-preview` para comandos y consultas
- **Anthropic/Claude**

*Opción local:* Deja la clave en blanco, rellena los campos de LLM local que aparecen a continuación y marca **☑ Usar** junto al LLM local. Consulta la [guía de LLM local (Linux)](Install-Ollama-Local-LLM-Linux) / [guía de LLM local (Windows)](Install-Ollama-Local-LLM-Windows).
- **Dirección LLM**: por defecto es `localhost`. Sustitúyela por la IP de otro PC si Ollama se ejecuta en una máquina separada.
- **LLM de comandos**: gestiona la interpretación de comandos de voz.
- **LLM de consultas**: gestiona el análisis de datos. `tulu3:8b` es el mínimo. Los modelos más grandes producen mejores resultados.

---

# ¿Sin hardware local? Usa un LLM en la nube.

El coste variará según el servicio en la nube que elijas y cuánto tiempo juegues.

### OPCIÓN GRATUITA EN LA NUBE: Mistral
1. Ve a [Mistral Console](https://console.mistral.ai/home)
2. Crea una cuenta con un correo electrónico válido que puedas verificar.
3. NO SE NECESITA TARJETA DE CRÉDITO
4. Crea una "Organización" (llámala como quieras, por ejemplo "Elite Intel")
5. Genera una clave API. Introdúcela en la app y reinicia la app.


### Opción A: Clave API de xAI
1. Ve a la [consola de xAI](https://console.x.ai/).
2. Regístrate o inicia sesión.
3. Ve a la sección de API y genera una nueva clave API.
4. Añade créditos a tu cuenta.
5. Pega la clave en el campo **LLM** y marca la casilla de bloqueo.

### Opción B: Clave API de OpenAI
1. Ve a la [Plataforma de OpenAI](https://platform.openai.com/).
2. Regístrate o inicia sesión.
3. Ve a la sección de API y genera una nueva clave API.
4. Pega la clave en el campo **LLM** y marca la casilla de bloqueo.

### Opción C: Clave API de Anthropic/Claude
1. Ve a la [Plataforma de Claude](https://platform.claude.com).
2. Inicia sesión con correo electrónico o Google. Nota: la autenticación usa un enlace mágico enviado a tu correo electrónico.
3. Ve a **Ajustes → Facturación** y añade créditos antes de crear una clave. Una clave creada en una cuenta sin fondos no funciona aunque se añadan créditos después.
4. Ve a **Claves API** y crea una clave.
5. Pégala en el campo **LLM**, marca la casilla de bloqueo y arranca o reinicia los servicios en la pestaña de IA.

### Obtener una clave de Google TTS (14 voces)

1. Ve a la [Consola de Google Cloud](https://console.cloud.google.com/).
2. Inicia sesión o crea una cuenta.
3. Crea un nuevo proyecto.
4. Activa la **API de lenguaje generativo** para LLM y/o la **API de síntesis de voz de Cloud** para TTS.
5. Ve a **Credenciales**, crea una clave API y cópiala.
6. **Restringe la clave**: Haz clic en la clave que acabas de crear. En la página de detalles de la clave, haz clic en **Restringir clave**. Aparecerá un desplegable. Marca cada API que hayas habilitado (STT y/o TTS) y luego haz clic en **Guardar**.
7. Pega la clave en los campos **Reconocimiento de voz** y/o **Síntesis de voz** de la app. Marca las casillas de bloqueo.

---

## Directorio de ajustes y datos de la app

Los ajustes y datos de la app se almacenan en una base de datos SQLite ubicada en:
- **Linux:** `~/.local/share/elite-intel/elite-intel/db/`
- **Windows:** `%APPDATA%\elite-intel\db\`

----
Para problemas, contacta a través de Matrix. Los informes de errores y los pull requests son bienvenidos.

Comunidad 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
