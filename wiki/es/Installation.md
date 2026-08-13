# Instalación

Elite Intel **V1.1** es la versión actual.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Descarga el [👉**instalador**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Ejecuta el instalador y sigue las instrucciones en pantalla.
3. Configurar un modelo de lenguaje. Dos opciones:
   - **LLM local** (gratis, sin conexión): consulta la [**guía de LLM local**](installing-local-llms).
     Requiere hardware de GPU capaz.
   - **LLM en la nube** (tiene plan gratuito y es más fácil de configurar): consulta
     [**Opciones de LLM en la nube**](cloud-llm-options) para obtener una clave API, y después
     introdúcela en [**Ajustes → Servicios de IA**](UI-Settings-Tab).

Configuración completada. Siguiente: [**la interfaz, pestaña a pestaña**](UI).

### Lista de comprobación del primer arranque

Elite Intel dice estos avisos en voz alta al arrancar los servicios, así que te enterarás de
cualquier cosa que falte, pero conviene hacerlos de antemano:

| Paso | Dónde |
|------|-------|
| Apuntarlo a un modelo de lenguaje | [Ajustes → Servicios de IA](UI-Settings-Tab) |
| Comprobar la carpeta del diario | [Ajustes → General](UI-Settings-Tab) |
| Comprobar la carpeta de asignaciones y corregir las que falten | [Pestaña Bindings](UI-Bindings-Tab) |
| Calibrar el audio | [Pestaña Vega](UI-Vega-Tab) → **CALIBRAR AUDIO** |

---

### Desinstalación (Linux)

```shell
~/.var/app/elite.intel.app/uninstall
```

----
Para problemas, repórtalos en Matrix. Los informes de errores y los pull requests son bienvenidos.

Comunidad 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
