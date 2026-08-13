# Elegir un servidor de inferencia local

Para ejecutar un LLM local con Elite Intel, se necesita un **servidor de inferencia**. Es un software que carga el modelo de IA y lo sirve a través de una API local. Es el equivalente local de un servicio de IA en la nube, pero funciona completamente en tu propio hardware.

Elite Intel es compatible con dos servidores de inferencia: **Ollama** y **LM Studio**. Ambos son compatibles y utilizan los mismos modelos. La elección puede cambiarse en la configuración en cualquier momento.

![loca llm ui](images/local-llm.png)

## Requisitos de GPU
Requisitos de hardware para ejecutar el juego y el LLM en la misma máquina:

- RTX 3090 24 GB de VRAM
- AMD RX 7800 XT

Si no dispones del hardware suficiente, utiliza el __[servicio gratuito en la nube](https://v2.auth.mistral.ai/login)__

Una tabla de referencia de GPU proporcionada por **Kevin Rank** está disponible aquí:
[Guía de referencia de GPU](https://docs.google.com/spreadsheets/d/1ZyPgTvlVg7ueemHEV-3J3j3tAynShIyxTs8rd59rips/edit?usp=sharing)

---
### Guías de instalación

| Servidor de inferencia                                |                                                                                       |
|-------------------------------------------------------|---------------------------------------------------------------------------------------|
| [✅ LM Studio - Linux](Install-LM-Studio-Linux)       | Rápido, mayor flexibilidad de modelos  la guía muestra cómo configurarlo como servidor |
| [✅ LM Studio - Windows](Install-LM-Studio-Windows)   | Rápido, mayor flexibilidad de modelos  incluye interfaz gráfica                       |
| [Ollama - Linux](Install-Ollama-Local-LLM-Linux)     | Recomendado si tienes el hardware necesario para ejecutarlo                            |
| [Ollama - Windows](Install-Ollama-Local-LLM-Windows) | Recomendado si tienes el hardware necesario para ejecutarlo                            |

---

### Ollama vs. LM Studio de un vistazo

|                              | Ollama                                     | LM Studio                                                                                                    |
|------------------------------|--------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **Velocidad**                | Más lento                                  | Más rápido                                                                                                   |
| **Modelo requerido**          | `google/gemma-4-e4b`                        | `google/gemma-4-e4b`                                                                                         |
| **Ideal para**               | Configuración sencilla, mantenimiento mínimo | Mayor control sobre la carga del modelo                                                                     |
| **Instalación**              | Un script y listo                          | Un script y listo                                                                                            |
| **Se ejecuta como**          | Servicio del sistema (inicio automático)   | Inicio manual o inicio automático opcional                                                                   |
| **Ajuste de modelo**         | Modelfile integrado en el modelo           | Parámetros al momento de carga                                                                               |
| **Inicio automático Windows**| ✅ Funciona sin configuración adicional     | Requiere la aplicación de escritorio o el Programador de tareas                                              |
| **Inicio automático Linux**  | ✅ Servicio systemd incluido               | Configuración manual de systemd                                                                              |
| **Fuente de modelos**        | Biblioteca de Ollama                       | HuggingFace (GGUF)                                                                                           |
| **Puerto API**               | `11434`                                    | `1234`                                                                                                       |
| **Interfaz gráfica**         | Ninguna (solo CLI)                         | Aplicación de escritorio opcional                                                                            |

---

### Guía de selección

**Usa Ollama cuando:**
- Quieres una instalación sencilla con configuración continua mínima
- Estás en Windows y prefieres no configurar el inicio manualmente
- Eres nuevo en los LLM locales

**Usa LM Studio cuando:**
- Quieres una interfaz gráfica para explorar, descargar y gestionar modelos
- Ya estás familiarizado con HuggingFace y los archivos de modelo GGUF
- Quieres experimentar con diferentes modelos sin escribir Modelfiles
- Estás ejecutando una máquina de inferencia dedicada y necesitas un servidor headless limpio

**Cualquier opción funciona cuando:**
- Tienes una NVIDIA RTX 3090 24 GB o equivalente o superior. La VRAM es el factor crítico, no la velocidad de la GPU. Una GPU con solo 12 GB de VRAM es insuficiente independientemente de la generación.
- Estás ejecutando Elite Dangerous y el LLM en la misma máquina
- Quieres apuntar Elite Intel a un PC separado en tu red

---
## Recomendación del desarrollador

El desarrollador usa LM Studio con `google/gemma-4-e4b` (~6,3 GB). El mismo modelo en Ollama va
notablemente más lento. Otros modelos pueden funcionar, pero no está garantizado. Comparte tus
hallazgos de compatibilidad en Matrix.

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
