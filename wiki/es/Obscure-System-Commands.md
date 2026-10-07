# Comandos del sistema en detalle

[Todos los comandos](AllCommands) enumera lo que puedes decir. Esta página trata los comandos que
necesitan algo más de explicación: qué hacer *antes* de decirlos, qué hacen de verdad y los
detalles a tener en cuenta.

Todo esto funciona hablado, escrito en el chat del juego como `@Vega …` o con un clic en la
[pestaña Acciones](UI-Actions-Tab).

---

## Ajustes que puedes cambiar por voz

Cada anuncio hablado tiene un interruptor por voz, y cada uno equivale a un interruptor de la
[pestaña Comandante → Anuncios](UI-Commander-Tab), donde ves de un vistazo qué está activado.

- **Anuncios de ruta**: *«Desactiva los anuncios de ruta.»* Silencia todo lo que se dice alrededor
  de un salto — siguiente sistema, tráfico, bajas, llegada, saltos restantes, estrella
  recolectable. Las consultas manuales siguen funcionando.
- **Anuncios de descubrimiento**: *«Activa los anuncios de descubrimiento.»* Primeros
  descubrimientos, cuerpos valiosos, señales biológicas. También controla la tarjeta de
  Exobiología del [overlay HUD](UI-HUD-Overlay).
- **Aproximación planetaria**: *«Activa los anuncios de aproximación.»*
- **Anuncios de minería**: *«Activa los anuncios de minería.»* Impactos del prospector para tus
  objetivos. Primero fija objetivos: *«Agregar objetivo de minería painita.»* Sin objetivos no hay
  nada que anunciar.
- **Recogidas del cargo scoop**: *«Activa los anuncios de recogida.»*
- **Contactos de radar**: *«Desactiva los anuncios de radar.»*
- **Radio**: *«Enciende la radio.»* Tráfico de radio del juego — amenazas piratas, control de
  tráfico — con sus propias voces de radio. Volumen y efectos en [Ajustes → Audio](UI-Settings-Tab).
- **Todo a la vez**: *«Desactiva todos los anuncios.»*
- **Visión nocturna / luces / asistente de conducción**: *«Activar visión nocturna.»* *«Apagar
  luces.»* *«Asistente de conducción.»*

> **¿Emitiendo o volando en ala?** No existe un «modo streaming». Para que Vega no reaccione a
> otras voces, mándala a dormir (*«Duerme»*) y antepón a la orden ocasional *«Escúchame, …»* — o
> usa [push-to-talk](UI-Settings-Tab), que lo ignora todo mientras no mantengas el botón.

---

## Navegación y búsqueda de lugares

Vega traza rutas al resultado de una búsqueda, a lugares que ya conoce o a coordenadas de
superficie. No traza rutas a un sistema que nombras en voz alta — ver
[comando personalizado para lugares que visitas a menudo](UI-Actions-Tab).

- **Navegar a coordenadas**: *«Navega a las coordenadas latitud 41,43 longitud -75,23.»* Guía
  desde la órbita hasta el punto en el cuerpo actual o al que te acercas. En el lado oscuro vuelas
  por instrumentos.
- **Siguiente muestra biológica / entrada del codex**: *«Navegar a la siguiente muestra
  biológica.»* Te guía al organismo guardado más cercano en este planeta. *«Eliminar entrada del
  codex»* descarta la que sigues.
- **Zona de aterrizaje**: *«Navega a la zona de aterrizaje.»* Vuelve a donde aterrizó tu nave.
- **Tu nave nodriza**: *«Navega al fleet carrier»* / *«navega al carrier del escuadrón.»* Traza a
  su última ubicación conocida — o a tu sistema hogar si no se conoce ninguna.
- **Hogar**: *«Establecer sistema hogar»* marca dónde estás (pide confirmación); *«llévame a
  casa»* traza la vuelta.
- **Navegar desde memoria**: copia con Ctrl+C el nombre de un sistema desde INARA, Spansh o un
  mensaje de chat y di *«navega desde memoria.»* Vega abre el mapa galáctico y traza la ruta.
- **Ruta del carrier**: abre el mapa galáctico, selecciona el destino, copia su nombre y di
  *«calcular ruta de fleet carrier.»* La ruta viene de Spansh, así que el sistema debe estar ahí.
- **Siguiente destino del carrier**: abre el mapa galáctico *del carrier* y di *«ingresar destino
  del carrier.»* Vega escribe el siguiente tramo de la ruta guardada y lo confirma — repítelo tras
  cada salto.
- **Ruta de neutrones**: copia el nombre del destino desde el mapa galáctico y di *«calcular ruta
  de neutrones»* (opcional *«…eficiencia 60»*, *«…con sobrecarga»*). Tras cada impulso, *«siguiente
  estrella de neutrones»* — o activa *Trazar la siguiente ruta de neutrones al impulsar en el
  cono* en la [pestaña Comandante](UI-Commander-Tab).
- **Comerciantes e intermediarios**: *«Buscar comerciante de materiales sin procesar /
  codificados / fabricados»*, *«buscar intermediario tecnológico humano / guardian»*, *«buscar
  Vista Genomics más cercano»*, *«buscar factores interestelares más cercanos.»* Vega traza la ruta
  y deja un recordatorio con la estación; al llegar, pregunta *«¿cuál era el recordatorio?»*
- **Brain trees**: *«Buscar brain trees para [material] en un radio de 500 años luz.»* Encuentra un
  brain tree guardián que da ese material sin procesar.
- **Sitios de minería**: *«¿Dónde puedo minar osmio en 200 años luz?»* También sirve para tritio.
- **Comprar y vender**: *«¿Dónde puedo comprar bromellita?»* — añade *más cercano* o *mejor
  precio*; también vale para módulos. *«¿Dónde puedo vender oro?»*
- **Combustible**: *«Buscar estación de combustible»* / *«necesito combustible.»*
- **Malos resultados de búsqueda**: si una búsqueda te manda una y otra vez a un sitio que no
  sirve, di *«excluir este sistema de las búsquedas»* (o *«excluir ese sistema»* para tu
  destino). Se deshace desde dentro: *«desbloquear este sistema.»*

---

## Combate y misiones

- **Aprende primero de tu historial**: *«Analizar diarios en busca de zonas de caza.»* Lee tus
  journals guardados y aprende cada sistema con sitios de extracción de recursos y cada proveedor
  de masacres piratas que hayas visto. Hazlo una vez tras instalar.
- **Zonas de caza**: *«Buscar zona de caza en 100 años luz.»* Un sistema con sitios de extracción
  de recursos, de entre los que has recorrido. *«Olvida esta zona de caza»* quita una.
- **Apilar masacres**: *«Buscar misiones de masacre de piratas»*, *«navegar al proveedor de misión
  pirata»*, *«navegar al objetivo de misión pirata»*, *«¿cuántas bajas?»*
- **Zonas de conflicto**: *«Buscar una zona de conflicto.»*
- **Misiones**: *«Navega a la misión activa.»* *«Buscar carga para la misión»* busca dónde comprar
  lo que aún necesita una misión activa — la que caduca antes y cuya carga no llevas ya.
- **Subsistemas**: *«Apunta al FSD»* (también motores, distribuidor de energía, planta de energía,
  soporte vital, escudo).

---

## Atajos de control de la nave

- **Distribución de energía**: *«Energía a escudos»*, *«máximo motores»*, *«igualar energía.»* Una
  orden ajusta todos los pips.
- **Cerrar / salir**: *«Cerrar»* o *«salir»* abandona el panel o el mapa abierto.
- **Escaneo**: *«Escanear el sistema»* dispara el escáner de descubrimiento en el grupo de fuego que
  fijaste por nave (pestaña Comandante → ⚙). *«Abrir FSS»* abre el escáner de espectro completo.
- **Velocidad óptima**: *«Velocidad óptima.»* Pone el acelerador al 75 % — el punto dulce del
  supercrucero. Dila unos 20 segundos antes del objetivo para no dar vueltas a su alrededor.
- **Fijar el siguiente sistema de la ruta**: *«Seleccionar destino FSD.»*
- **Wing nav lock**: *«Seguir al wingman.»*
- **Grupos de fuego**: *«Grupo de fuego bravo»* — letras OTAN o números.
- **Despedir / recoger**: *«Despedir nave»* la envía a órbita; *«recógeme»* la trae de vuelta.

---

## Utilidades y sesión

- **Recordatorios**: *«Establecer recordatorio, recoger painita en Hutton Orbital.»* Se guarda
  hasta que lo borres; *«¿cuál era el recordatorio?»* lo lee. *«Borrar recordatorios»* pide
  confirmación.
- **Temporizadores**: *«Recuérdame en 20 minutos revisar el carrier.»*
- **Monetizar ruta**: *«Monetizar ruta.»* Encuentra un par compra/venta rentable a lo largo de la
  ruta trazada y lo guarda como recordatorio — comercio, no exploración. Aparece en el
  [overlay HUD](UI-HUD-Overlay) como *Oportunidad de carga*.
- **Autodiagnóstico**: *«Haz un diagnóstico»* / *«¿funcionas bien?»* Prueba la conexión con el
  modelo de lenguaje e informa de si responde y con qué rapidez.
- **Interrumpir**: *«Interrumpe.»* Corta a Vega a media frase. Con push-to-talk, pulsar el botón
  hace lo mismo.
- **Análisis de bioma**: *«Analizar bioma»* (o nombra un planeta). Dice qué es probable que crezca
  allí antes de aterrizar.
- **Finanzas y alcance del carrier**: *«Estado del carrier»* / *«finanzas del carrier.»*
  Combustible, reserva, alcance de salto, saldo y autonomía.
- **Sitios de construcción**: *«Progreso del sitio de construcción»*, *«buscar mercancía de
  construcción»*, *«llévame de vuelta al sitio de construcción»*, *«olvida el sitio de
  construcción.»*

---

## Notas de uso

- **Lenguaje natural**: sin sintaxis fija. Di lo que quieres decir.
- **Los nombres, al chat**: los nombres de sistemas, estaciones y mercancías son lo que más le
  cuesta al reconocimiento de voz. Escribe esas órdenes: `@Vega dónde puedo comprar tritio`.
- **Las órdenes destructivas preguntan antes**: borrar recordatorios, objetivos de minería,
  misiones o una ruta, eliminar una entrada del codex, olvidar una zona de caza, excluir un sistema
  o fijar un nuevo hogar. Responde *sí*; cualquier otra cosa cancela.
- **VR**: la distribución de energía y cerrar/salir te ahorran buscar en menús con el visor.

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
