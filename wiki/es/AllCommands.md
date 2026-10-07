# Órdenes y consultas de Elite Intel

¡Hola, comandante! Esta es una referencia de lo que puedes preguntar u ordenar a **Vega**, la IA
de tu nave. **No necesitas memorizar nada** — habla con naturalidad y Vega entiende lo que
quieres decir. Las frases de abajo son ejemplos, no guiones: lo que importa es el significado, no
las palabras exactas.

> **La lista definitiva y siempre actualizada está en la app.** La
> [pestaña Acciones → Comandos integrados](UI-Actions-Tab) muestra cada orden y consulta de esta
> versión, filtradas a lo que puedes usar donde estás ahora, con las frases de entrenamiento en tu
> propio idioma. Desde ahí puedes ejecutar cualquiera con un clic.

---

## Hablar con Vega

**Habla con naturalidad.** *«Baja el tren de aterrizaje»*, *«desplegar tren de aterrizaje»* y
*«tren de aterrizaje»* hacen lo mismo. Cuanto más claro digas lo que quieres, con más fiabilidad
ocurre.

**Llamarla por su nombre es opcional.** *«Vega, tren de aterrizaje»* funciona igual que *«tren de
aterrizaje»*.

**Dormir y despertar.**

- *«Duerme»* / *«ignórame»* — Vega deja de escuchar.
- *«Despierta»* — vuelve a escuchar.
- *«Escúchame, …»* — mientras duerme, deja pasar **una** orden sin despertarla:
  *«Escúchame, salta al hiperespacio.»*

**Interrumpir.** Di *«interrumpe»* o *«silencio»* para cortar a Vega a media frase. Con
[push-to-talk](UI-Settings-Tab) activado, basta con pulsar el botón.

**Las órdenes destructivas preguntan antes.** Borrar recordatorios, objetivos de minería o
misiones activas, borrar una ruta comercial, de neutrones o del carrier, eliminar una entrada del
codex, olvidar una zona de caza, descartar un sitio de construcción, excluir un sistema de las
búsquedas o fijar un nuevo sistema hogar — Vega te pide confirmación. Responde *sí* para seguir;
*no*, o cualquier otra cosa, lo cancela.

---

## Escribir órdenes en el chat del juego

El reconocimiento de voz siempre destrozará algunas palabras — nombres de sistemas, de
mercancías, *tritio*. Para esos casos, **escribe la orden en el chat del juego**. Empieza la línea
con `@Vega`:

```
@Vega dónde puedo comprar tritio
@Vega agregar objetivo de minería painita
@vega, baja el tren de aterrizaje
```

- Da igual mayúsculas o minúsculas, y se admite una coma o dos puntos tras el nombre.
- La línea se trata exactamente como si la hubieras dicho, e interrumpe a Vega si está hablando.
- Usa el canal **local**. Solo se leen tus propias líneas enviadas — nada de lo que escriba otro
  comandante puede dar órdenes a Vega.

---

## ⚙️ App y sesión

- Dormir / despierta / *«escúchame, [orden]»* — ver arriba.
- *«Interrumpe»* — dejar de hablar.
- *«Haz un diagnóstico»* / *«¿funcionas bien?»* — Vega se revisa e informa.
- *«¿Qué hora es?»* — hora UTC real.
- *«Establecer recordatorio [texto]»* — una nota permanente; *«¿cuál era el recordatorio?»* la
  lee.
- *«Recuérdame en 10 minutos revisar el carrier»* — un temporizador.
- *«Borrar recordatorios.»*

### Anuncios sí/no

- *«Desactiva los anuncios de radar»* / *«activa los anuncios de radar»*
- *«Activa / desactiva los anuncios de descubrimiento»*
- *«Activa / desactiva los anuncios de ruta»*
- *«Activa / desactiva los anuncios de aproximación»* — aproximación planetaria
- *«Activa / desactiva los anuncios de minería»*
- *«Activa los anuncios de recogida»* — cargo scoop
- *«Enciende / apaga la radio»* — transmisiones de radio
- *«Desactiva todos los anuncios»*

Cada uno es también un interruptor en la [pestaña Comandante → Anuncios](UI-Commander-Tab).

---

## 🎮 Controles de la nave

- **Tren de aterrizaje:** *«bajar tren de aterrizaje»* / *«subir tren de aterrizaje»*
- **Armas:** *«hardpoints»*, *«desplegar anclajes»* / *«retraer anclajes»*, *«armas frías»*
- **Cargo scoop:** *«abrir / cerrar cargo scoop»*
- **Luces / visión nocturna:** *«luces»*, *«apagar luces»*, *«visión nocturna»*
- **Modo del HUD:** *«modo combate»* / *«modo análisis»*
- **Defensa:** *«disipador térmico»*, *«célula de escudo»*, *«chaff»* / *«lanzar contramedidas»*
- **Grupos de fuego:** *«grupo de fuego bravo»*, *«seleccionar grupo de fuego 3»*
- **Energía:** *«energía a escudos / motores / armas / sistemas»* (*«máximo escudos»*),
  *«igualar energía»*
- **Vista de cabeza:** *«restablecer vista de cabeza»*
- **Activar:** *«activar»* — pulsa lo que esté seleccionado en el panel abierto.

### Acelerador

- *«Detener motores»* / *«alto total»*
- *«Un cuarto de acelerador»*, *«medio acelerador»*, *«tres cuartos de acelerador»*, *«acelerador
  al máximo»*
- *«Aumenta velocidad en 2»* / *«reduce velocidad en 1»*
- *«Velocidad óptima»* — ajusta el acelerador para la aproximación en supercrucero.

### Vuelo

- *«Despegar»* / *«despegar nave»* — dejar la plataforma.
- *«Solicitar atraque»* / *«solicitar aterrizaje»*
- *«Taxi»* / *«aterrizaje automático»* — deja el aterrizaje al ordenador de atraque.
- *«Activar supercrucero»* / *«entrar a supercrucero»*
- *«Salta»* / *«salta al hiperespacio»* — el salto en sí.
- *«Salir aquí»* / *«caer aquí»*
- *«Seleccionar destino FSD»* — selecciona el siguiente sistema de tu ruta trazada.
- *«Escanear el sistema»* / *«escaneo de descubrimiento»* — dispara el escáner de descubrimiento
  (ver el ajuste de honk por nave en la [pestaña Comandante](UI-Commander-Tab)).
- *«Abrir FSS»* / *«escaneo de espectro completo»* — abre el FSS y escanea.

---

## 🚙 SRV, caza y a pie

- *«Desplegar SRV»* — abre la bahía correcta del hangar (fija tus bahías en los ajustes ⚙ de cada
  nave en la [pestaña Comandante](UI-Commander-Tab)).
- *«Desplegar nomad»*
- *«Recuperar SRV»* / *«abordar nave»* — desde el SRV.
- *«Asistente de conducción»* (activar / desactivar)
- *«Desembarcar»* / *«bajar de la nave»*
- *«Despedir nave»* / *«nave a órbita»* — enviar la nave a órbita.
- *«Recógeme»* / *«volver a superficie»* — llamarla de vuelta.
- *«Servicios de estación»* — el panel de servicios, atracado en el SRV.

### Órdenes al caza

- *«Desplegar fighter»*
- *«Fighter defender»* · *«Atacar mi objetivo»* · *«Fuego a voluntad»* · *«Fighter alto el
  fuego»* · *«Recuperar fighter»*

---

## ⚔️ Combate y misiones

- **Objetivos:** *«apuntar a la mayor amenaza»*, *«objetivo prioritario»*
- **Subsistemas:** *«apunta al FSD»*, *«objetivo motores»*, *«objetivo distribuidor de energía»*,
  y planta de energía, soporte vital, escudo
- **Ala:** *«apuntar al compañero 1 / 2 / 3»* (o *wingman alpha / bravo / charlie*), *«seguir al
  wingman»*
- *«Misiones activas»* / *«registro de misiones»* — todo lo de tu tablón.
- *«Navega a la misión activa»*
- *«Buscar carga para la misión»* — dónde comprar lo que aún necesita una misión activa, y trazar
  la ruta.
- *«Borrar misiones activas»*
- *«Recompensas totales»* — recompensas cobradas.

### Apilar masacres piratas

- *«Buscar misiones de masacre de piratas»*
- *«Navegar al proveedor de misión pirata»*
- *«Navegar al objetivo de misión pirata»*
- *«¿Cuántas bajas?»* / *«conteo de bajas»*

### Caza de recompensas y zonas de conflicto

- *«Buscar zona de caza en 100 años luz»* — un sistema con sitios de extracción de recursos, de
  entre los que ya has visitado.
- *«Analizar diarios en busca de zonas de caza»* — aprende de golpe sistemas con RES y
  proveedores de masacre a partir de tus journals antiguos. Hazlo una vez tras instalar.
- *«Olvida esta zona de caza»*
- *«Buscar una zona de conflicto»* / *«¿dónde está la guerra más cercana?»*

Ver [Misiones piratas](Pirate-Massacre-Mission-Tracking).

---

## 🧭 Navegación

Vega traza rutas hacia **el resultado de una búsqueda**, hacia lugares que ya conoce o hacia
coordenadas de superficie. **No** puede navegar a un sistema que simplemente nombras en voz alta
— los nombres son donde más falla el reconocimiento de voz, y un error te manda al otro lado de
la burbuja. Para eso usa *navegar desde memoria* o — para un lugar al que vas a menudo — un
[comando personalizado que traza la ruta por ti](UI-Actions-Tab).

- *«Navega desde memoria»* / *«pegar desde memoria»* — copia antes con Ctrl+C el nombre de un
  sistema (de INARA, Spansh, un mensaje de chat…); Vega abre el mapa galáctico y traza la ruta.
- *«Llévame a casa»* / *«establecer sistema hogar»*
- *«Navega al fleet carrier»* / *«navega al carrier del escuadrón»*
- *«Navega a la siguiente parada comercial»*
- *«Cancelar navegación»*

### En un planeta

- *«Navega a las coordenadas latitud 12,5 longitud -40,2»* — guía desde la órbita hasta el punto.
- *«Navega a la zona de aterrizaje»* — vuelve a donde aterrizó tu nave.
- *«Navegar a la siguiente muestra biológica»* — el orgánico marcado más cercano.
- *«Eliminar entrada del codex»*

### Autopista de neutrones

- *«Calcular ruta de neutrones»* — copia antes el nombre del destino desde el mapa galáctico.
  Opciones: *«…eficiencia 60»*, *«…con sobrecarga»*.
- *«Siguiente estrella de neutrones»* — traza al siguiente punto de neutrones (o deja que lo haga
  el ajuste *Trazar la siguiente ruta de neutrones* de la [pestaña Comandante](UI-Commander-Tab)).
- *«Borrar ruta de estrella de neutrones»*

### Buscar lugares

Cada orden de *buscar* traza una ruta a lo que encuentra y lo pone en el
[overlay HUD](UI-HUD-Overlay).

- *«Buscar comerciante de materiales sin procesar / codificados / fabricados»*
- *«Buscar intermediario tecnológico humano / guardian»*
- *«Buscar Vista Genomics más cercano»*
- *«Buscar factores interestelares más cercanos»* — para pagar tus multas y recompensas.
- *«Buscar estación de combustible»* / *«necesito combustible»*
- *«Buscar fleet carrier más cercano»*
- *«Buscar brain trees en un radio de 500 años luz»*
- *«¿Dónde puedo minar painita?»* / *«buscar sitio de minería»*
- *«Excluir este sistema de las búsquedas»* (o *«ese sistema»* — tu destino) — cuando una búsqueda
  te manda una y otra vez a un sitio que no sirve. *«Desbloquear este sistema»*, dicho desde
  dentro de él, lo deshace.

---

## 💰 Comercio y mercados

- *«¿Dónde puedo comprar [mercancía]?»* — también sirve para módulos de nave; di *más cercano* o
  *mejor precio*.
- *«¿Dónde puedo vender [mercancía]?»*
- *«Calcular ruta comercial»* — usa el perfil comercial de esta nave.
- *«Monetizar ruta»* — una carga rentable para el viaje que ya haces.
- *«Ruta comercial»* / *«plan comercial actual»*
- *«Navega a la siguiente parada comercial»*
- *«Cancelar ruta comercial»*
- *«Mercados locales»* · *«Detalles de la estación»* / *«¿qué servicios hay aquí?»* ·
  *«Outfitting»* · *«Naves en venta»*
- *«¿Qué hay en nuestra bodega?»*

### Perfil comercial

También se edita por nave en los ajustes ⚙ de la [pestaña Comandante](UI-Commander-Tab).

- *«Perfil comercial»* — describe el actual.
- *«Cambiar presupuesto inicial del perfil comercial 5 millones»*
- *«Cambiar máximas paradas del perfil comercial 4»*
- *«Cambiar distancia máxima del perfil comercial 1000»*
- *«Permite / no permitas carga prohibida»*
- *«Permite / no permitas puertos planetarios»*
- *«Permite / no permitas sistemas con permiso»*
- *«Permite / no permitas strongholds»*

Ver [Comercio y beneficios](TradeRoutePlotting) y [Buscar en la galaxia](Search-galaxy-with-EliteIntel).

---

## 🏗️ Colonización

- *«Buscar mercancía de construcción»* — lo que aún necesita el sitio de construcción, dónde
  comprarlo y cómo llenar la bodega.
- *«¿Cómo va la construcción?»* / *«progreso del sitio de construcción»*
- *«Llévame de vuelta al sitio de construcción»*
- *«Olvida el sitio de construcción»* — dejar de seguirlo.

---

## 🛰️ Nave nodriza

Di *carrier* — o *carrier del escuadrón* — o Vega puede creer que hablas de la nave.

- *«Estado del carrier»* — combustible, alcance con el tritio actual, finanzas.
- *«Reserva de tritio del carrier 200»* — tritio reservado.
- *«Calcular ruta de fleet carrier»* — copia antes el nombre del destino desde el mapa galáctico.
- *«Ingresar destino del carrier»* — con el mapa galáctico del carrier abierto, Vega escribe el
  siguiente tramo y lo confirma.
- *«Ruta del carrier»* / *«ruta de salto del carrier»*
- *«ETA del carrier»* / *«¿cuándo llega el carrier?»*
- *«Distancia al carrier»*
- *«Cancelar ruta del carrier»*
- *«Carriers en el sistema»*

---

## 🌠 Exploración y exobiología

- *«¿Dónde estamos?»* — ubicación actual.
- *«Distancia a la burbuja»* / *«distancia a Sol»*
- *«Distancia al planeta [nombre]»*
- *«Último escaneo»* — el último cuerpo que escaneaste.
- *«Planetas en el sistema»* / *«planetas aterrizables»*
- *«Señales en el sistema»* · *«Señales geológicas»*
- *«Seguridad del sistema»* / *«¿quién controla?»*
- *«Información del objetivo FSD»* / *«analizar destino»* — analiza el sistema al que vas a saltar.
- *«Ruta trazada»* / *«disponibilidad de combustible en la ruta»*
- *«Ganancias de exploración»* — lo que valen tus escaneos.
- *«Materiales planetarios»* — qué hay en este cuerpo.
- *«Señales biológicas en el sistema»* · *«Muestras de exobiología»* · *«Analizar bioma»*
- *«Distancia a la última muestra biológica»*
- *«Ya hemos muestreado este cuerpo»* — márcalo como hecho si lo muestreaste antes de instalar.

### Exo-Maestría

Cuando el catálogo esté activado en la [pestaña Comandante](UI-Commander-Tab):

- *«Llévame al siguiente sitio de exobiología»* — el sistema más rico que aún no has agotado.
- *«Marcar este sistema como cosechado»* — dar por hecho un sistema entero.

Ver [Descubrimiento y exobiología](Discovery-Assistance).

---

## ⛏️ Minería

- *«Agregar objetivo de minería painita»* / *«quitar objetivo de minería painita»* / *«borrar
  objetivos de minería»*
- *«Activa los anuncios de minería»* — impactos del prospector para tus objetivos.
- *«¿Dónde puedo minar [material]?»*

Con objetivos fijados y una refinería instalada, el [overlay HUD](UI-HUD-Overlay) muestra una
tarjeta de minería.

---

## 👤 Comandante y nave

- *«Perfil del jugador»* — rangos y progreso.
- *«Configuración de la nave»* / *«módulos de la nave»*
- *«Inventario de materiales»* / *«¿cuánto [material] tenemos?»*

---

## 📺 Paneles y mapas

Di el nombre del panel, con *mostrar* si quieres:

- *Navegación* · *Transacciones* · *Contactos* · *Chat / comunicaciones* · *Bandeja de entrada* ·
  *Panel social* · *Historial* · *Escuadrón* · *Estado* · *Radar*
- *Panel del comandante* · *Tripulación* · *Panel interno* · *Módulos* · *Grupos de fuego* ·
  *Inventario* · *Almacenamiento* · *Panel del fighter*
- *Gestión del carrier*
- *Mapa galáctico* · *Mapa del sistema*
- *«Siguiente / anterior panel»*, *«siguiente / anterior página»* — recorrer pestañas dentro de un
  panel.
- *«Cerrar»* / *«salir»* / *«cerrar mapa»* — volver al HUD.

---

## 🎵 Música

*«Reproducir música»*, *«pausar la música»*, *«siguiente pista»*, *«pista anterior»*, *«reiniciar
la lista»*, *«mezclar la música»*, *«pon la canción [título]»*. Ver la
[pestaña Jukebox](UI-Jukebox-Tab).

---

## Tus propias órdenes

Lo que falte, lo construyes tú: [Acciones → Comandos personalizados](UI-Actions-Tab). Los
comandos personalizados se activan con tus propias frases, dichas o escritas en el chat, igual que
los integrados.

---

¡Vuela peligrosamente, comandante! o7

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈 | Open Source [**GitHub**](https://github.com/SudoKrondor/EliteIntel) | [YouTube](https://www.youtube.com/@SudoKrondor) | [Twitch](https://www.twitch.tv/sudokrondor) | Creative Commons License |
