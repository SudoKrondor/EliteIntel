### La versión publicada es V1.0 y difiere de lo que ves en las capturas de pantalla.

### Si quieres la versión V1.1, únete al equipo de pruebas beta.
### 👉[**Únete al equipo de pruebas beta V1.1 aquí**](https://matrix.to/#/#krondor:matrix.org)👈

---

## <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Descarga el [👉**instalador**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Ejecuta el instalador y sigue las instrucciones en pantalla.
   - **Parakeet STT** (reconocimiento de voz local) y **Kokoro TTS** (síntesis de voz local) están ambos incluidos. No se requieren pasos ni servicios adicionales.
3. Configura un LLM. Hay dos opciones disponibles:
   - **LLM local** (gratuito, sin conexión): Consulta la [**guía de LLM local**](installing-local-llms). Requiere hardware de GPU adecuado.
   - **LLM en la nube** (más fácil de configurar): Consulta la guía [**Configurar la app**](UI-and-Configuration-Options) para la configuración de claves API.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux
### Instalación (cualquier distribución de escritorio - no se requiere sudo)
1. Descarga el script instalador:

```shell
curl -L -o installer.sh https://raw.githubusercontent.com/stone-alex/EliteIntel/refs/heads/master/distribution/installer.sh
```

2. Dale permisos de ejecución al script y ejecútalo:
```shell
chmod +x installer.sh
./installer.sh
```
La app se instala en `~/.var/app/elite.intel.app`.
Tanto **Parakeet STT** como **Kokoro TTS** están incluidos en la app. No se necesita instalación adicional. Actívalos en la app a través de las casillas **Pestaña de Ajustes ☑ Usar**.

3. Configura un LLM. Hay dos opciones disponibles:
   - **LLM local** (gratuito, sin conexión): Consulta la [**guía de LLM local**](installing-local-llms). Requiere hardware de GPU adecuado.
   - **LLM en la nube** (más fácil de configurar): Consulta la guía [**Configurar la app**](UI-and-Configuration-Options) para la configuración de claves API.

Instalación completa. Consulta [**Configurar la app**](Configuration) para los próximos pasos.

---

### Desinstalación

Usa el indicador `-d` para eliminar la app. El instalador pedirá confirmación antes de borrar los datos de configuración y claves API.

```shell
bash installer.sh -d
```

----
Para problemas, repórtalos en Matrix. Los informes de errores y los pull requests son bienvenidos.

Comunidad 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
