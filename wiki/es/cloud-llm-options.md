# ¿Sin hardware local? Usa un LLM en la nube.


**LLM (cerebro de la IA)**

*Opción en la nube:* abre **Servicios → Servicios de IA → Configuración en la nube**, elige tu
proveedor en la lista **Proveedor**, pega la clave API de ese proveedor, marca **Bloqueado** y
pulsa **Guardar**.

El proveedor lo eliges tú. Elite Intel no lo adivina a partir de la clave, porque los proveedores
cambian el formato de sus claves con demasiada frecuencia como para que eso sea fiable. El modelo
no lo eliges: Elite Intel lo selecciona para tu proveedor:

| Proveedor | Modelo usado |
|-----------|--------------|
| **xAI** | `grok-4-1-fast-non-reasoning` |
| **OpenAI** | `gpt-5.4-mini` |
| **Gemini** (Generative Language API) | `gemini-3.1-flash-lite-preview` |
| **Anthropic / Claude** | `claude-haiku-4-5` |
| **DeepSeek** | `deepseek-v4-flash` |


El coste varía según el servicio en la nube que elijas y el tiempo que juegues.

### Opción A: clave API de xAI
1. Ve a la [xAI Console](https://console.x.ai/).
2. Regístrate o inicia sesión.
3. Ve a la sección de API y genera una nueva clave API.
4. Añade saldo a tu cuenta.
5. En **Servicios → Servicios de IA → Configuración en la nube**, elige **xAI (Grok)** como proveedor, pega la clave en **Clave API**, marca **Bloqueado** y pulsa **Guardar**.

### Opción B: clave API de OpenAI
1. Ve a la [OpenAI Platform](https://platform.openai.com/).
2. Regístrate o inicia sesión.
3. Ve a la sección de API y genera una nueva clave API.
4. En **Servicios → Servicios de IA → Configuración en la nube**, elige **OpenAI** como proveedor, pega la clave en **Clave API**, marca **Bloqueado** y pulsa **Guardar**.

### Opción C: clave API de Anthropic/Claude
1. Ve a la [Claude Platform](https://platform.claude.com).
2. Inicia sesión con tu correo o con Google. Nota: el acceso se hace con un enlace mágico que se envía a tu correo.
3. Ve a **Settings → Billing** y añade saldo antes de crear una clave. Una clave creada en una cuenta sin saldo no funciona, aunque añadas saldo después.
4. Ve a **API Keys** y crea una clave.
5. En **Servicios → Servicios de IA → Configuración en la nube**, elige **Anthropic (Claude)** como proveedor, pega la clave en **Clave API**, marca **Bloqueado**, pulsa **Guardar** y luego inicia o reinicia los servicios en la pestaña Vega.

### Opción D: clave API de Google Gemini
1. Ve a [Google AI Studio](https://aistudio.google.com/api-keys) e inicia sesión con tu cuenta de Google.
2. Haz clic en el **icono de la llave** abajo a la izquierda.
3. Haz clic en **Create API key**. Si te pide crear un proyecto, crea uno con el nombre que quieras.
4. Te pedirá una cuenta de pago (facturación). Puedes usar tu cuenta de Google.
5. Copia la clave.
6. En **Servicios → Servicios de IA → Configuración en la nube**, elige **Google (Gemini)** como proveedor, pega la clave en **Clave API**, marca **Bloqueado**, pulsa **Guardar** y luego inicia o reinicia los servicios en la pestaña Vega.

### Conseguir una clave de Google TTS

1. Ve a la [Google Cloud Console](https://console.cloud.google.com/).
2. Inicia sesión o crea una cuenta.
3. Crea un proyecto nuevo.
4. Activa la **Cloud Text-to-Speech API**.
5. Ve a **Credentials**, crea una clave API y cópiala.
6. **Restringe la clave**: haz clic en la clave que acabas de crear. En la página de detalles, haz clic en **Restrict key**. Aparece una lista desplegable. Marca cada API que hayas activado (STT y/o TTS) y haz clic en **Save**.
7. Pega la clave en **Servicios → Servicios de IA → Voz (TTS) → Clave de Google TTS**, marca **Bloqueado** y pulsa **Guardar**.
