
# ¿Sin hardware local? Usa un LLM en la nube.


**LLM (cerebro de IA)**

*Opción en la nube:* Introduce tu clave API para Mistral, xAI, OpenAI o Anthropic/Claude. La aplicación utiliza un modelo fijo por proveedor:
- **Mistral**: 'mistral-small-2506' **(Plan gratuito)**
- **xAI**: `grok-4-1-fast-non-reasoning`
- **OpenAI**: `gpt-4.1-mini` (comandos) / `gpt-5.2` (consultas)
- **Gemini Generative Language API**: `gemini-3.1-flash-lite-preview` para comandos y consultas
- **Anthropic/Claude**
- **DeepSeek**


El coste variará según el servicio en la nube que elijas y el tiempo que juegues.

### Opción GRATUITA en la nube: Mistral
1. Ve a la [consola de Mistral](https://console.mistral.ai/home)
2. Crea una cuenta con un correo electrónico válido que puedas verificar.
3. NO ES NECESARIA TARJETA DE CRÉDITO
4. Crea una "Organización" (ponle el nombre que quieras, por ejemplo "Elite Intel")
5. Genera una clave API. Introduce esa clave en la aplicación y reiníciala.


### Opción A: Clave API de xAI
1. Ve a la [consola de xAI](https://console.x.ai/).
2. Regístrate o inicia sesión.
3. Ve a la sección de API y genera una nueva clave API.
4. Añade créditos a tu cuenta.
5. Pega la clave en el campo **LLM** y marca la casilla de bloqueo.

### Opción B: Clave API de OpenAI
1. Ve a la [plataforma de OpenAI](https://platform.openai.com/).
2. Regístrate o inicia sesión.
3. Ve a la sección de API y genera una nueva clave API.
4. Pega la clave en el campo **LLM** y marca la casilla de bloqueo.

### Opción C: Clave API de Anthropic/Claude
1. Ve a la [plataforma de Claude](https://platform.claude.com).
2. Inicia sesión con correo electrónico o Google. Nota: la autenticación utiliza un enlace mágico enviado a tu correo.
3. Ve a **Configuración → Facturación** y añade créditos antes de crear una clave. Una clave creada en una cuenta sin fondos no funciona aunque se añadan créditos después.
4. Ve a **Claves API** y crea una clave.
5. Pégala en el campo **LLM**, marca la casilla de bloqueo e inicia o reinicia los servicios en la pestaña de IA.

### Obtener una clave de Google TTS (14 voces)

1. Ve a la [consola de Google Cloud](https://console.cloud.google.com/).
2. Inicia sesión o crea una cuenta.
3. Crea un nuevo proyecto.
4. Activa la **Generative Language API** para el LLM y/o la **Cloud Text-to-Speech API** para TTS.
5. Ve a **Credenciales**, crea una clave API y cópiala.
6. **Restringe la clave**: Haz clic en la clave que acabas de crear. En la página de detalles, haz clic en **Restringir clave**. Aparecerá un menú desplegable. Marca cada API que hayas activado (STT y/o TTS) y haz clic en **Guardar**.
7. Pega la clave en los campos **Reconocimiento de voz** y/o **Síntesis de voz** en la aplicación. Marca las casillas de bloqueo.
