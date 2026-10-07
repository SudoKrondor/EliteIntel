# Pestaña Jukebox

<img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> Tu propia música, desde
tus propios archivos, sonando por debajo de Vega en vez de por encima. Cada vez que Vega habla,
la música baja automáticamente y vuelve a subir cuando termina — no te pierdes ningún aviso.

Nada que instalar, sin cuenta, sin servicio de streaming. La pestaña va de arriba abajo:
**Biblioteca de música**, **Lista de reproducción**, **Reproducción**.

---

## Biblioteca de música

De dónde sale la música.

- **Examinar...** — elige una carpeta de música. Elite Intel la recorre, subcarpetas incluidas, y
  añade a la lista cada archivo reproducible que encuentra. Los archivos entran en orden de
  carpeta, así que los capítulos de un audiolibro llegan en secuencia.
- **Volver a explorar** — revisa de nuevo la carpeta en busca de archivos añadidos después.

**Formatos admitidos:** MP3, FLAC, M4A / M4B (AAC), OGG / OGA (Vorbis) y WAV. Los archivos que el
Jukebox no puede reproducir (WMA, Apple Lossless, compras con DRM, Opus) se omiten.

---

## Lista de reproducción

La lista es la cola: lo que ves es el orden en que suena.

| Columna | Significado |
|--------|---------|
| **#** | Posición. Un ▶ marca la pista que suena |
| **Título** · **Artista** · **Álbum** | Leídos de las etiquetas de los archivos. Una biblioteca grande se completa en unos segundos |
| **Duración** | Duración de la pista |

- **Doble clic** en una pista para reproducirla.
- **Arrastra** filas para reordenarlas. El orden se guarda.
- Hacer clic en la cabecera de una columna **no** ordena — eso tiraría un orden que montaste a
  mano. Ordenar está en el menú contextual.

Un archivo que ha desaparecido del disco se marca como **falta**.

### Menú contextual

| Elemento | Qué hace |
|------|--------------|
| **Reproducir ahora** | Reproduce la pista seleccionada |
| **Reproducir a continuación** | Mueve las pistas seleccionadas justo detrás de la actual |
| **Quitar de la lista** | Las quita de la lista (los archivos en disco no se tocan) |
| **Mostrar en el gestor de archivos** | Abre la carpeta que contiene el archivo |
| **Copiar artista y título** | Al portapapeles |
| **Añadir carpeta...** | Añade la música de otra carpeta |
| **Importar lista...** | Añade las pistas que nombra una lista `.m3u` / `.m3u8` |
| **Quitar archivos que faltan** | Elimina cada entrada cuyo archivo ya no está |
| **Vaciar la lista** | Quita todas las pistas (pregunta antes; los archivos en disco no se borran) |
| **Ordenar por** → Título / Artista / Carpeta | Una ordenación puntual que reescribe el orden de la lista |

---

## Reproducción

- **El cabezal** — por dónde vas en la pista. Arrástralo para saltar; la reproducción salta al
  soltar.
- **Anterior · Reproducir/Pausa · Detener · Siguiente** — los controles de transporte. **Detener**
  rebobina la pista actual al principio; **Pausa** conserva el punto.
- **Orden** — *Secuencial* o *Aleatorio*.
- **Volumen** — el nivel propio de la música. Está aquí y no en los ajustes de Audio para que
  nunca bajes a Vega por error.

Tu punto dentro de una pista se recuerda entre sesiones — útil para audiolibros —, pero el
Jukebox nunca empieza a sonar solo al arrancar la app.

---

## Órdenes de voz

Cada orden de música nombra la *música*, una *pista* o una *canción*, para que nunca choque con
órdenes de la nave.

| Di | Qué ocurre |
|-----|--------------|
| *«Reproducir música»* / *«poner música»* | Iniciar o reanudar |
| *«Pausar la música»* / *«parar la música»* | Pausa — «reproducir música» sigue donde lo dejó |
| *«Siguiente pista»* / *«saltar esta canción»* | Pista siguiente |
| *«Pista anterior»* | Pista anterior |
| *«Reiniciar la lista»* | Vuelve a la primera pista |
| *«Mezclar la música»* / *«música aleatoria»* | Orden aleatorio |
| *«Pon la canción Rocket Man»* | Busca una pista por título o artista y la reproduce. Si nada coincide bien, Vega lo dice en vez de poner la equivocada |

También funcionan escritas en el chat del juego — ver [Todos los comandos](AllCommands).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
