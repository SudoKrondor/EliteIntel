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
| **Modelo preferido**         | [tulu-3.1-8b-supernova Q4_K_M](https://huggingface.co/mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF) | [tulu-3.1-8b-supernova Q4_K_M](https://huggingface.co/mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF) |
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

El desarrollador usa LM Studio con [`matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`](https://huggingface.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF). Este modelo proporciona inferencia rápida. El mismo modelo en Ollama funciona notablemente más lento. La aplicación está optimizada para este modelo. Otros modelos pueden funcionar, pero no están garantizados. Informa sobre los resultados de compatibilidad en Matrix.

## ¿Por qué tulu3.1:8b Supernova específicamente?

Elite Intel es un analizador de comandos y una herramienta de análisis de datos, no un chatbot conversacional. Esto impone requisitos específicos al modelo. Generar conversaciones que suenen naturales es insuficiente. El modelo debe inferir correctamente las acciones a partir de la entrada de voz y realizar análisis de datos estructurados. Debe devolver los resultados en JSON formateado, no en un ensayo o HTML. No todos los modelos de este tamaño realizan esta tarea de manera fiable.

## Tulu 3 (la receta de entrenamiento base) es genuinamente excepcional

[Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF](https://huggingface.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF/tree/main)

La mayoría de los modelos de instrucción se entrenan con RLHF, que utiliza un modelo de recompensa aprendido para evaluar las salidas. Ese modelo de recompensa es en sí mismo una red neuronal, por lo que hereda todos los sesgos e inconsistencias habituales. Tulu 3 reemplazó esto con RLVR (Aprendizaje por refuerzo con recompensas verificables). En lugar de un modelo de recompensa aprendido, el entrenamiento utiliza una función de puntuación determinista: la respuesta es correcta o no lo es. Binario, sin sesgos. Esto es especialmente impactante para las tareas de seguimiento de instrucciones, donde la señal de recompensa es objetiva.

El proceso de entrenamiento sigue un enfoque de cuatro etapas: curación de datos orientada a habilidades clave, ajuste fino supervisado, Optimización de Preferencia Directa y RLVR encima para mejorar el rendimiento en tareas verificables. Cada etapa se construye sobre la anterior. Por eso Tulu 3, sobre la base Llama de 8B, alcanza resultados que superan a las versiones instruct de Llama 3.1, Qwen 2.5, Mistral e incluso modelos cerrados como GPT-4o-mini y Claude 3.5 Haiku.

Para EliteIntel, la etapa de clasificación de comandos es una tarea de seguimiento de instrucciones con respuestas correctas verificables (acción JSON X frente a Y). Este es precisamente el tipo de tarea que optimiza RLVR. El modelo está entrenado específicamente para producir salidas estructuradas deterministas.

## Por qué la variante "Supernova"

La variante Supernova difiere del Tulu 3 estándar. Tulu-3.1-8B-SuperNova se crea mediante una fusión lineal de tres modelos: Llama-3.1-MedIT-SUN-8B (médico/razonamiento), Llama-3.1-Tulu-3-8B (seguimiento de instrucciones) y Llama-3.1-SuperNova-Lite (el modelo destilado de Arcee), cada uno con un peso igual de 1.0 usando mergekit.

El modelo padre SuperNova-Lite es un modelo destilado de una base Arcee más grande, lo que proporciona una densidad de conocimiento superior a la de un modelo estándar de 8B. La fusión lineal promedia directamente los tensores de peso, combinando el conocimiento sin cómputo de entrenamiento adicional. Esto consigue resultados especialmente sólidos en tareas de seguimiento de instrucciones, como lo demuestra su puntuación IFEval.

**Rendimiento**: El modelo usa una arquitectura Llama de 8B. Con la cuantización Q4_K_M en una 3090 de 24 GB, cabe en la VRAM junto al juego con margen. Esto evita la descarga a CPU y mantiene el máximo rendimiento de inferencia. Los modelos Qwen comparables usan configuraciones de cabezas de atención distintas (como el ratio GQA de Qwen2.5) que pueden ser más lentos en el backend GGUF de llama.cpp.

También funciona en una tarjeta con 12 GB de VRAM si no hay otras cargas de trabajo que consuman VRAM. Esto requiere que el juego se ejecute en una GPU o máquina independiente.

## ¿Puedo usar un modelo diferente?

Se pueden usar modelos alternativos, pero es poco probable que igualen la velocidad y precisión de tulu3.1-supernova.

Los problemas más comunes con los modelos alternativos incluyen un formato de respuesta incorrecto.
El error más frecuente es que el modelo devuelva un ensayo en lugar de una acción estructurada o un resultado de análisis.

--- 

Comunidad 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
