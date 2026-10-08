# Colonización

EliteIntel convierte un sitio de construcción de colonización en una lista de la compra en vivo. Sigue lo
que la obra todavía necesita, encuentra dónde comprarlo y traza la ruta hasta allí. Se acabó saltar entre
webs de mercados y hojas de cálculo.

**No** decide qué construir ni dónde. Eso es cosa tuya, arquitecto. Te ayuda a reunir la montaña de
mercancías que necesita la obra.

[[youtube:PnIlVZRdhKE]]

## Empezar a seguir una obra

**Aterriza en el sitio de construcción.** Ese es el disparador. Al posarte en el depósito, el juego envía el
manifiesto completo del sitio y ese sitio pasa a ser el que sigue EliteIntel. Abre la pantalla de
construcción y el [overlay del HUD](UI-HUD-Overlay) muestra la tarjeta **SITIO DE OBRA**: progreso, lo
pendiente y qué cargar en el próximo viaje.

No hay ningún seguimiento de toda la galaxia a tus espaldas. La app solo conoce el estado de la obra **según
tu último aterrizaje**. Otros comandantes pueden llevar carga al mismo depósito mientras no estás, así que la
tarjeta y Vega indican la antigüedad de los datos cuando superan una hora.

## Encontrar la siguiente mercancía

Pide ``Buscar mercancía de construcción`` (o *buscar carga para el sitio de construcción*, *buscar carga de
colonización*, etc.).

1. **Primero tu carrier.** Si tu carrier de flota o de escuadrón lleva mercancías que la obra necesita, Vega
   te envía allí antes que a cualquier mercado. Un carrier en tu sistema actual siempre gana. Uno más lejano
   tiene que merecer los saltos.
2. **Después, el mercado más cercano.** EliteIntel busca mercados a dos saltos de tu nave (según su alcance
   de salto) y, si no encuentra nada, a cuatro saltos. Primero las estaciones espaciales, después los
   asentamientos planetarios. Algunas mercancías, como el compuesto CMM, solo se venden en asentamientos
   planetarios. Sí, tendrás que aterrizar.
3. **Primero el mayor faltante.** La búsqueda parte de la mercancía que más te falta (acero, titanio,
   aluminio, el agujero más grande que tengas) y prefiere el mercado que más llena tu bodega con otras
   mercancías que necesita la obra.

Una vez encontrado el mercado, EliteIntel abre el mapa de la galaxia en la estrella. **No** confirma la
ruta. Es a propósito: tú decides si la fijas o si bajas al mapa del sistema para encontrar el asentamiento
concreto.

**¿Instalación nueva?** Las búsquedas en Spansh necesitan un punto de partida. La app busca primero en su
base de datos local las estaciones donde has atracado. Si no hay ninguna, pide a Spansh la estación más
cercana, lo que requiere tus coordenadas galácticas, y la app solo las conoce después de un salto FSD. Así
que vuela un poco con la app en marcha y atraca en algunas estaciones antes de empezar un proyecto de
construcción. Cuanto más la uses, más sabe.

## En el mercado

Al atracar, el overlay reordena la lista para poner primero las mercancías **que vende esta estación**.

- Una mercancía cargada en parte aparece en verde con las toneladas a bordo (por ejemplo ``16 T +44``).
- Cuando tienes suficiente de una mercancía, desaparece de la lista y la siguiente pendiente ocupa su línea.
- Cuando has comprado todo lo que esta estación puede darte, el nombre de la estación desaparece de la
  tarjeta. Todavía quedan mercancías por comprar, pero no aquí. Vuelve a pedir ``Buscar mercancía de
  construcción`` y Vega te envía al siguiente mercado adecuado.

Entrega o almacena en tu carrier y repite hasta terminar la obra.

## De vuelta a la obra

Di ``Llévame de vuelta al sitio de construcción``. Si la obra está en otro sistema, se traza la ruta hasta
allí. Si ya estás en su sistema, no hace falta ruta.

Pregunta ``¿Cómo va la construcción?`` o ``¿Cuánto nos queda por comprar?`` para un informe de progreso
hablado.

## La pega del carrier

Frontier no expone la carga de los carriers a herramientas de terceros. La única forma de que EliteIntel vea
tus reservas es a través del **mercado de mercancías** del carrier. Para que sea visible:

1. **Cierra el carrier** para que nadie más pueda atracar y comprar tu pila de construcción.
2. **Pon las mercancías a la venta** en el mercado del carrier.
3. **Abre el mercado del carrier** desde el panel. Ese es el momento en que la app lo lee.

A partir de ahí EliteIntel lleva la cuenta cuando transfieres carga entre el carrier y tu nave, y cuando
compras o vendes en tu propio carrier. Es un apaño, pero el juego no permite mucho más.

## Varios sitios de construcción

El último sitio donde aterrizaste es el actual. EliteIntel sigue una sola obra a la vez en el overlay.
Aterriza en otro depósito y ese toma el relevo. Vuelve a aterrizar en el primero y vuelve a ser el actual.

¿Quieres un descanso de la construcción? Di ``Olvida el sitio de construcción``. Vega te pide confirmación y
la tarjeta se calla. No se borra nada. Aterrizar en el sitio lo trae de vuelta.

## Lo que hace la IA (y lo que no)

La IA convierte lo que dices en acciones y decide qué te responde Vega. El seguimiento, las búsquedas y el
trazado de rutas son código normal que lee el diario del juego. No juega el ciclo de colonización por ti y no
ve nada que el juego no escriba. Sigues siendo tú quien pilota. La app te ahorra la navegación por la web.

Consulta [Todos los comandos](AllCommands) para la lista completa de frases de colonización.
