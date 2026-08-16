# Elegir un servidor de inferencia local

Para ejecutar un LLM local con Elite Intel, se necesita un **servidor de inferencia**. Es un software que carga el modelo de IA y lo sirve a través de una API local. Es el equivalente local de un servicio de IA en la nube, pero funciona completamente en tu propio hardware.

Elite Intel utiliza **LM Studio** como servidor de inferencia. Funciona en Windows y Linux y expone una API compatible con OpenAI.

![loca llm ui](images/local-llm.png)

## Requisitos de GPU
Requisitos de hardware para ejecutar el juego y el LLM en la misma máquina:

- RTX 3090 24 GB de VRAM
- AMD RX 7800 XT

Si no dispones del hardware suficiente, utiliza el __servicio gratuito en la nube__ en
👉 **[console.mistral.ai](https://console.mistral.ai/)** 👈 — plan gratuito, sin tarjeta de crédito.
Pasos de configuración: [LLM gratuito en la nube](cloud-llm-options).

Una tabla de referencia de GPU proporcionada por **Kevin Rank** está disponible aquí:
[Guía de referencia de GPU](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

---
### Guías de instalación

| Servidor de inferencia                                |                                                                                        |
|-------------------------------------------------------|----------------------------------------------------------------------------------------|
| [✅ LM Studio - Linux](Install-LM-Studio-Linux)       | Rápido, mayor flexibilidad de modelos - la guía muestra cómo configurarlo como servidor |
| [✅ LM Studio - Windows](Install-LM-Studio-Windows)   | Rápido, mayor flexibilidad de modelos - incluye interfaz gráfica                        |
| [🆓 LLM gratuito en la nube](cloud-llm-options)       | Sin GPU - plan gratuito de Mistral, sin tarjeta de crédito                              |

---

### LM Studio de un vistazo

|                              | LM Studio                                             |
|------------------------------|-------------------------------------------------------|
| **Modelo requerido**         | `google/gemma-4-e4b`                                  |
| **Instalación**              | Un script y listo                                     |
| **Se ejecuta como**          | Inicio manual, o inicio automático opcional           |
| **Ajuste del modelo**        | Opciones al cargar                                    |
| **Inicio automático Windows**| Requiere la app de escritorio o el Programador de tareas |
| **Inicio automático Linux**  | Configuración manual de systemd (ver la guía de Linux) |
| **Fuente de modelos**        | HuggingFace (GGUF)                                    |
| **Puerto API**               | `1234`                                                |
| **Interfaz gráfica**         | Aplicación de escritorio opcional                     |

---

### Guía de selección

**Usa LM Studio en local cuando:**
- Tienes una NVIDIA RTX 3090 24 GB o equivalente o superior. La VRAM es el factor crítico, no la velocidad de la GPU. Una GPU con solo 12 GB de VRAM es insuficiente independientemente de la generación.
- Estás ejecutando Elite Dangerous y el LLM en la misma máquina
- Quieres apuntar Elite Intel a un PC separado en tu red
- Quieres una interfaz gráfica para explorar, descargar y gestionar modelos, o un servidor headless limpio en una máquina de inferencia dedicada

**Usa mejor el [LLM gratuito en la nube](cloud-llm-options) cuando:**
- Tu GPU no tiene VRAM suficiente para ejecutar un modelo junto al juego
- Prefieres no gestionar un servidor de inferencia local

---
## Recomendación del desarrollador

El desarrollador usa LM Studio con `google/gemma-4-e4b` (~6,3 GB). Otros modelos pueden funcionar,
pero no está garantizado. Comparte tus hallazgos de compatibilidad en Matrix.

## ¿Por qué `google/gemma-4-e4b` específicamente?

Elite Intel es un analizador de comandos y una herramienta de análisis de datos, no un chatbot
conversacional. Eso impone requisitos concretos al modelo. No basta con generar charla que suene
natural. El modelo debe inferir correctamente las acciones a partir de la voz, realizar análisis de
datos estructurados y devolver los resultados como datos estructurados, no como un ensayo en
markdown o HTML. No todos los modelos de este tamaño lo hacen de forma fiable.

El requisito ineludible es el **function calling**. El compañero de Elite Intel no pide al modelo
que describa lo que haría: le ofrece un conjunto de herramientas y espera que llame a una, con
argumentos. Un modelo que no puede emitir una llamada a herramienta bien formada no puede manejar
la aplicación en absoluto, por bien que escriba. `google/gemma-4-e4b` lo admite.

Con unos 6,3 GB cabe en la VRAM junto al juego en una tarjeta de 24 GB con margen, lo que evita
descargar trabajo a la CPU y mantiene alto el rendimiento de inferencia.

> **Sobre el modelo retirado de V1.0.** Las versiones anteriores recomendaban
> `tulu-3.1-8b-supernova`. No admite function calling, así que no puede ejecutar el compañero y ya
> no sirve con Elite Intel. Si sigues una guía antigua, ignórala e instala `google/gemma-4-e4b`.

## ¿Puedo usar un modelo diferente?

Se pueden usar modelos alternativos, pero deben admitir function calling. Sin eso la aplicación no
puede ejecutar nada.

El fallo más frecuente con un modelo alternativo es un formato de respuesta incorrecto: el modelo
devuelve prosa describiendo una acción en lugar de llamar realmente a la herramienta.

--- 

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
