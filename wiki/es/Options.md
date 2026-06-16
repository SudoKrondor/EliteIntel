# Opciones de interfaz y configuración

### Pestaña IA <img src="images/ai.png" class="inline" height="20" alt="AI">

Esta es la pestaña principal/predeterminada.

|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![tab-ai-buttons.png](images/tab-ai-buttons.png) | - **Iniciar / Detener servicios**: Activa o desactiva la pila de IA.<br/>- **Despertar/Dormir**: En modo Despertar la aplicación escucha todo el tiempo. En modo Dormir, la aplicación ignorará la entrada a menos que se presione el botón PTT, se use la palabra de omisión "Listen" o se emita el comando "Wake up!".<br/>- **Overlay OBS**: Muestra una ventana de superposición negra con la interacción del Comandante / IA. Agrégalo a OBS desactivando el fondo negro.<br/>- **Dispositivos de audio**: Seleccionar el dispositivo de audio para entrada/salida. **Calibrar audio**: Ejecutar la calibración de audio para un mejor rendimiento. |
|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |


---

### Pestaña Jugador <img src="images/controller.png" class="inline" height="20" alt="Player">

![playertab](images/tab-player.png)

- **Nombre del Comandante**: Usa este campo para reemplazar tu nombre en el juego para la síntesis de voz.
- **Opciones de nave**: Puedes activar o desactivar estas automatizaciones. Útil para comandantes con discapacidades.
- **Gestión de flota**: Asigna voces, personalidades y cadencia a naves individuales. La personalidad solo funciona con LLMs en la nube. El icono de engranaje abre las propiedades de la nave, como el honk automático y el perfil de comercio.

![popup-ship-properties.png](images/popup-ship-properties.png)

- **Honk automático al entrar al sistema**: Selecciona el grupo de fuego y el disparador. Si esta opción está marcada, la nave realizará un escaneo de descubrimiento al entrar. Si el HUD está en modo Combate, cambiará a Análisis, realizará el escaneo y volverá al modo anterior.
- **Personalizar tu perfil de comercio**: Estos parámetros pueden establecerse desde la interfaz de usuario, o mediante comandos de voz: "alter/change trade profile set [parámetro] to [valor]"

---



### Pestaña Acciones <img src="images/keys-binding.png" class="inline" height="20" alt="Actions">

![tab-actions.png](images/tab-actions.png)

La pestaña **Acciones / Vínculos** tiene tres secciones: Vínculos, Comandos integrados y Comandos personalizados.

- **Vínculos**: directorio donde se encuentra tu archivo de vínculos del juego. Sin él, la aplicación no puede controlar el juego.
- **Perfil**: tu perfil de vínculos actual en el juego.
- **Archivo**: el archivo que contiene los vínculos que estás usando actualmente.

Puedes modificar tus vínculos desde esta pantalla y guardarlos como un nuevo perfil.

__NOTA  Los HOTAS/CONTROLADORES se muestran pero no pueden configurarse desde esta pantalla. Solo vínculos de teclado (sujeto a cambios en el futuro).__


**Acciones / Comandos integrados**

![tab-action-build-in-commands.png](images/tab-action-build-in-commands.png)


Proporciona una lista de comandos integrados. Hacer doble clic en uno mostrará un cuadro de diálogo con información sobre el comando y permitirá proponer una mejor traducción para la localización.

**Comandos personalizados**

![acttions cuystom commands](images/tab-actions-custom-commands.png)

Esta pantalla te permite definir una acción personalizada que la aplicación ejecutará a tu comando.

- Haz clic en el botón NUEVO para abrir una ventana emergente donde puedes definir tu acción personalizada.

![popup-custom-action.png](images/popup-custom-action.png)

- Ingresa el nombre de la acción. NOTA: El nombre de la acción debe contener palabras (tokens) separadas por guiones bajos _
- Proporciona un nombre para tu acción personalizada.
- Proporciona una descripción para tu acción personalizada.
- Ingresa palabras de entrenamiento, que son tokens de significado. El LLM intentará hacer coincidir el comando hablado con la acción usando la mayor probabilidad. Cuanto más probables sean tus tokens para coincidir con la acción, más posibilidades hay de que sea devuelta.

Para usar acciones personalizadas, habla con naturalidad. No necesitas memorizar las palabras exactas, pero debes transmitir un significado preciso para que el LLM asocie tu comando con la acción con mayor probabilidad.

---


Para problemas, contacta a través de Matrix. Los informes de errores y las pull requests son bienvenidos.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
