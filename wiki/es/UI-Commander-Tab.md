# Pestaña Comandante

<img src="images/controller.png" class="inline" height="20" alt="Comandante"> Quién eres, con qué
voz habla cada casco de tu flota, qué hace tu nave por ti automáticamente, qué te cuenta Vega sin
que se lo pidas, y el catálogo de Exo-Maestría.

![Pestaña Comandante](images/ui-tab-commander.png)

Arriba está la franja **Perfil del comandante**; debajo hay cuatro subpestañas: **Gestión de
flota**, **Ajustes globales de nave**, **Anuncios** y **Exo-Maestría**.

---

## Perfil del comandante

**Nombre del comandante** — el nombre que Vega usa para ti de vez en cuando. Úsalo si Vega
destroza tu nombre del juego o si simplemente prefieres que te llamen de otra forma. Se guarda al
pulsar Enter o al hacer clic fuera.

**Dirigirse a mí** — desactivado, Vega nunca se dirige a ti: ni nombre, ni rango, ni título.

> La **carpeta del journal** está en [Ajustes → General](UI-Settings-Tab), y la **carpeta de
> bindings** en la [pestaña Bindings](UI-Bindings-Tab).

---

## Gestión de flota

Una fila por cada nave que posees, seguida de tu **nave nodriza** y tu **nave nodriza de
escuadrón**, si las tienes. Elite Intel descubre tu flota a partir del journal del juego; nunca
añades naves a mano. Si juegas con varios comandantes en un mismo PC, la lista sigue al
comandante cargado en el juego.

| Columna | Notas |
|--------|-------|
| **Nave** | El nombre de tu nave (para una nave nodriza, su nombre e indicativo) |
| **Modelo de nave** | El tipo de casco, o *Nave nodriza* / *Nave nodriza de escuadrón* |
| **Voz** | Haz clic para elegir. Al cambiarla se reproduce enseguida una frase con esa voz para que la escuches |
| **Personalidad** | `Profesional` · `Casual` · `Amigable` · `Desquiciado` · `Seven of Nine` · `Mercenario bocazas` · `Tu ex novio` · `Tu ex novia` · `Rebelde` |
| **⚙** | Abre los ajustes de esa nave (ver abajo) |

**Sobre la lista de voces.** Se ofrecen todas las voces del motor de voz elegido en
[Ajustes → Servicios de IA](UI-Settings-Tab), masculinas y femeninas — la voz que elijas también
decide si Vega habla de sí misma en masculino o en femenino en esa nave.

- **Kokoro** y **Supertonic 3** (locales) — etiquetadas como `Nombre - acento`.
- **Google** (nube) — etiquetadas como `Nombre - acento · HD` o `· Estándar`. En inglés, el
  acento distingue las voces. En cualquier otro idioma cada voz se sintetiza en ese idioma, así
  que la etiqueta muestra el género y el nivel de calidad en lugar de un acento inglés engañoso.
- **Microsoft Edge** (nube) — etiquetadas como `Nombre - acento`.

> Cambiar el motor de voz restablece la voz de cada nave a la predeterminada del nuevo motor. Las
> **personalidades se conservan**. La app pregunta antes de hacerlo.

**Las filas de naves nodriza** solo tienen voz — Vega no tripula una nave nodriza, así que no
tiene personalidad ni ajustes. La voz es la con la que responde su **control de tráfico** por
radio. Déjala en **Aleatoria** y cada vez responde un controlador distinto; si eliges una, la
prueba suena como una transmisión de radio, porque es la única forma en que la oirás.

---

## Ajustes de nave (el botón ⚙)

Ajustes por nave, porque una Python minera y una Corvette de combate no quieren el mismo
comportamiento. Los cambios se guardan al cerrar el diálogo con **Atrás**.

![Ajustes de nave](images/ui-ship-settings.png)

**Hacer honk del sistema al entrar** — lanza un escaneo de descubrimiento al llegar a un sistema.
Elige el **Grupo de fuego** (A–H) y el **Disparador** (1 o 2) donde va montado tu escáner de
descubrimiento. Si tu HUD está en modo combate, Elite Intel cambia a análisis, escanea y vuelve.

**Bahías de vehículos** — lo que guardas en cada bahía del hangar de vehículos (**Bahía 1–4**:
*Vacía*, *Scarab*, *Scorpion* o *Rhino*). El journal del juego nombra el hangar pero nunca su
contenido, así que así es como *«desplegar SRV»* abre la bahía correcta — y sabe si la nave debe
aterrizar antes (Scarab, Scorpion) o puede quedarse suspendida (Rhino). Siempre se muestran las
cuatro bahías, sea cual sea el hangar instalado, para que tus elecciones sobrevivan a un cambio
de equipamiento.

**Aviso de materiales en emisiones de alto grado** — te avisa cuando una señal de emisiones de
alto grado en el sistema lleva materiales por los que vale la pena parar.

**Perfil comercial** — las restricciones que Elite Intel respeta al trazar una ruta comercial
para esta nave. Todas se pueden fijar también por voz:
*«cambiar máximas paradas del perfil comercial cuatro»*.

| Ajuste | Significado |
|---------|---------|
| **Permitir puertos planetarios** | Incluir puertos de superficie en las rutas |
| **Permitir carga prohibida** | Incluir carga que es ilegal en algún punto de la ruta |
| **Permitir sistemas bloqueados por permiso** | Incluir sistemas que necesitan permiso |
| **Permitir Fleet Carriers** | Incluir naves nodriza de jugadores como mercados |
| **Permitir sistemas Stronghold** | Incluir sistemas bastión thargoides/de poder |
| **Máx. Ls desde la llegada** | A qué distancia de la estrella de llegada puede estar una estación |
| **Máx. paradas** | Número de tramos de la ruta |
| **Capital inicial** | Créditos que el planificador puede gastar |

Cómo se vuelan las rutas: [Comercio y beneficios](TradeRoutePlotting).

---

## Ajustes globales de nave

Automatizaciones que Vega realiza por ti, en todas las naves. Cada una es un interruptor que se
guarda al instante. Útiles para todos, y una verdadera ayuda para comandantes con discapacidad.

| Interruptor | Qué hace |
|--------|--------------|
| **Acelerar automáticamente para FTL** | Da empuje antes de un salto |
| **Apagar luces automáticamente para FTL** | Apaga las luces de la nave antes de un salto |
| **Desactivar visión nocturna automáticamente para FTL** | Quita la visión nocturna antes de un salto |
| **Retraer anclajes automáticamente para FTL** | Retrae las armas antes de un salto |
| **Subir tren de aterrizaje automáticamente para FTL** | Sube el tren antes de un salto |
| **Retraer cargo scoop automáticamente para FTL** | Retrae el cargo scoop antes de un salto |
| **Subir tren automáticamente al despegar** | Sube el tren tras despegar |
| **Apagar luces automáticamente al desplegar SRV** | Apaga las luces al desplegar el SRV |
| **Trazar la siguiente ruta de neutrones al impulsar en el cono** | En una [ruta de neutrones](AllCommands), traza el siguiente punto de neutrones en cuanto atraviesas el cono con el impulso |

---

## Anuncios

Todo lo que Vega dice sin que se lo pidas, en un solo lugar — una única pantalla que revisar
cuando algo habla demasiado, o demasiado poco.

![Anuncios](images/ui-commander-announcements.png)

| Interruptor | Qué oyes |
|--------|---------------|
| **Anunciar descubrimientos** | Cuerpos notables, primeros descubrimientos, señales biológicas. También controla la tarjeta de Exobiología del [overlay HUD](UI-HUD-Overlay) |
| **Anunciar aproximación planetaria** | Datos del cuerpo al que te aproximas |
| **Anunciar contactos de radar** | Naves que aparecen en el escáner |
| **Anunciar minería** | Impactos de prospector y hallazgos para tus objetivos de minería |
| **Anunciar recogidas del cargo scoop** | Lo que acabas de recoger |
| **Anunciar navegación** | Eventos de navegación y llegadas |
| **Transmisiones de radio** | Charla de radio del juego, con voces de radio propias |
| **Anuncios de ruta** | Interruptor principal de todo lo que se dice alrededor de un salto. Los de debajo solo funcionan mientras está activado |
| &nbsp;&nbsp;↳ **Anunciar el destino del salto** | Cuál es el siguiente sistema |
| &nbsp;&nbsp;↳ **Anunciar el tráfico del destino** | Informes de tráfico de tu destino |
| &nbsp;&nbsp;↳ **Anunciar las bajas del destino** | Muertes recientes en el sistema de destino |
| &nbsp;&nbsp;↳ **Anunciar la llegada** | Una frase al llegar |
| &nbsp;&nbsp;↳ **Anunciar los saltos restantes** | Saltos que quedan en la ruta |
| &nbsp;&nbsp;&nbsp;&nbsp;↳ **Anunciar disponibilidad de estrella de combustible** | Si el destino tiene una estrella recolectable — se dice dentro de la frase de saltos restantes |

La mayoría también se puede cambiar por voz (*«desactiva los anuncios de radar»*, *«desactiva
todos los anuncios»*), por eso la página los vuelve a leer cada vez que abres la pestaña.

---

## Exo-Maestría

Un catálogo de sistemas estelares cerca de la burbuja cuyos planetas albergan exobiología de alto
valor, recopilado por la comunidad a través de Spansh.

1. Pulsa **Activar Exo-Maestría**. El catálogo se descarga y se importa, cada mitad con su propia
   barra de progreso.
2. Una vez cargado, la página muestra **Sistemas estelares**, **Planetas y lunas**, el **Valor
   previsto** de todo el catálogo y cuánto has **Cosechado**.
3. En vuelo, di *«llévame al siguiente sitio de exobiología»* y Vega traza un rumbo al sistema más
   rico que aún no hayas agotado.

Los planetas que terminas se marcan al escanear. Lo que muestreaste antes de instalar Elite Intel
se puede dar por hecho por voz — *«ya hemos muestreado este cuerpo»*, o para todo un sistema,
*«marcar este sistema como cosechado»*.

**Desactivar Exo-Maestría** elimina el catálogo de tu ordenador, tras preguntar. Los planetas que
ya muestreaste quedan registrados, así que reactivarlo más adelante no te enviará de vuelta a
ellos.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
