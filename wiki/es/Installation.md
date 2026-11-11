# Instalación

Elite Intel **V1.2** es la versión actual.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Descarga el [👉**instalador**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Ejecuta el instalador y sigue las instrucciones en pantalla.
3. Configurar un modelo de lenguaje. **Elite Intel no puede funcionar sin uno.** Dos opciones:
   - **LLM local** (gratis, sin conexión): consulta la [**guía de LLM local**](installing-local-llms).
     Requiere hardware de GPU capaz.
   - **LLM en la nube** (de pago y más fácil de configurar): consulta
     [**Opciones de LLM en la nube**](cloud-llm-options) para obtener una clave API, y después
     introdúcela en [**Servicios → Servicios de IA**](UI-Settings-Tab).

Configuración completada. Siguiente: [**la interfaz, pestaña a pestaña**](UI).

### Lista de comprobación del primer arranque

Elite Intel dice estos avisos en voz alta al arrancar los servicios, así que te enterarás de
cualquier cosa que falte, pero conviene hacerlos de antemano:

| Paso | Dónde |
|------|-------|
| Apuntarlo a un modelo de lenguaje | [Servicios → Servicios de IA](UI-Settings-Tab) |
| Comprobar la carpeta del diario | [Pestaña Comandante](UI-Commander-Tab) |
| Dejar que BindForge se configure y dar a Vega las teclas que necesita | [Pestaña BindForge](UI-Bindings-Tab) |
| Calibrar el audio | [Pestaña Vega](UI-Vega-Tab) → **CALIBRAR AUDIO** |

---

### ¿Nuevo en Elite Dangerous?

El juego crea sus archivos de controles solo cuando has cambiado algo, y sin ellos Elite Intel no puede manejar
tu nave. Completa tu entrenamiento de comandante y luego, en *Opciones → Controles* del juego, pon el movimiento
de la interfaz del mapa galáctico en las teclas de flecha y aplica.

### Pasar a otro ordenador

[**Copia de ajustes y datos**](UI-Settings-Tab), en la pestaña Servicios, guarda tus ajustes, tus datos y tus
comandos personalizados en un archivo que puedes restaurar en el ordenador nuevo.

---

### Desinstalación (Linux)

```shell
~/.var/app/elite.intel.app/uninstall
```

----
Para problemas, repórtalos en Matrix. Los informes de errores y los pull requests son bienvenidos.

Comunidad 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
