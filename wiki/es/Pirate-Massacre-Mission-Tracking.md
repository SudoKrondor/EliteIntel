# Seguimiento de Misiones de Masacre de Piratas

EliteIntel rastrea las Misiones de Masacre de Piratas, incluyendo el recuento de bajas, las facciones objetivo y los cálculos de recompensa. Monitorea las bajas y los pagos durante toda la sesión.

## Primeros Pasos

Para completar eficientemente las Misiones de Masacre de Piratas:

1. **Encuentra un conjunto**: Pide ``buscar misiones de masacre de piratas`` (ver *Encontrar conjuntos de masacre* más
   abajo) o busca uno en [INARA](https://inara.cz): un sistema donde varias facciones ofrecen misiones contra la misma
   facción pirata, junto a un sistema con un Sitio de Extracción de Recursos (RES). Acepta tantas misiones contra esa
   facción como puedas. EliteIntel rastrea la facción objetivo, el recuento de bajas y los detalles de pago.
2. **Consulta el Estado de las Misiones**: Después de aceptar misiones, hazle preguntas a EliteIntel como ``¿Cuántas bajas quedan en las misiones de piratas?`` o ``¿Cuál es el pago de la masacre de piratas?`` EliteIntel devuelve el total de bajas requeridas, los créditos potenciales y un desglose por facción. Especifica que la consulta es sobre piratas para evitar confusión con otros tipos de misiones.

## Combatir Piratas en el Haz RES

1. **Viaja al Sitio**: Vuela al sistema objetivo y entra en el Haz RES. Escanea las naves cercanas.
2. **Identificación de Objetivos**: EliteIntel anuncia lo siguiente cuando se escanean naves:
    - **Piratas no relacionados con misiones**: ``Objetivo legal, [tipo de nave], [rango del piloto], [recompensa].`` Estas naves son objetivos válidos pero no cuentan para los objetivos de misión.
    - **Piratas de misión**: ``¡Objetivo de misión! [tipo de nave], [rango del piloto], [recompensa].`` Estos son los objetivos principales.
    - EliteIntel no anuncia naves limpias o aliadas, manteniendo la salida de audio relevante.
3. **Confirmación de Baja**: Cuando se elimina un pirata, EliteIntel anuncia ``Baja Confirmada`` junto con la recompensa ganada. Las bajas de objetivos de misión se anuncian como ``Baja de Misión Confirmada`` con el pago asociado.
4. **Consultas de Progreso**: Pregunta ``¿Cuántas bajas quedan?`` en cualquier momento. EliteIntel devuelve el total de bajas restantes y un desglose por facción.


## Mejores Prácticas

_**Canjea todas las recompensas antes de tomar tus misiones de masacre privadas**_  una baja confirmada es un vale de recompensa contra la facción

- **Apilamiento de Misiones**: Aceptar múltiples misiones contra la misma facción aumenta la eficiencia en créditos.
  Pide a EliteIntel la pila más profunda cercana (ver *Encontrar conjuntos de masacre* más abajo).
- **Consultas en Lenguaje Natural**: Se acepta el habla natural. ``¿Cuántos piratas quedan por matar?`` y ``¿Puntuación de misión de piratas?`` son ambas válidas. Especifica que la consulta es sobre piratas para evitar confusión con otras misiones activas.
- **Cambio de Personalidad**: Cambia entre modos de personalidad según sea necesario para adaptarte a tu estilo de juego actual.


---

# Encontrar conjuntos de masacre

Un **conjunto de masacre** es un proveedor de misiones y una zona de caza: facciones de un sistema ofrecen misiones de
masacre de piratas contra una facción pirata de un sistema cercano, y ese sistema tiene un Sitio de Extracción de
Recursos (RES) donde cazar. Cuantas más facciones ofrecen misiones en el sistema proveedor, más profunda es la pila: sus
misiones corren en paralelo, así que las mismas bajas se pagan varias veces.

## Cómo aprende EliteIntel los conjuntos

EliteIntel aprende conjuntos de tus propios vuelos. No hace falta ningún sitio web ni cuenta.

- **Misiones que aceptas.** Al aceptar una misión de masacre de piratas, EliteIntel registra el sistema proveedor, la
  estación, la facción y el objetivo.
- **Sitios de recursos que ves.** Al llegar a un sistema, el juego enumera sus fuentes de señal, Sitios de Extracción
  de Recursos incluidos. EliteIntel también se entera de los sitios de recursos que los juegos de otros comandantes
  informan a EDDN, la red pública de datos de Elite Dangerous, así que conoce sistemas que nunca has visitado.

Un conjunto necesita las dos mitades. Una misión contra un sistema sin sitio de recursos conocido se guarda, pero no se
ofrece hasta que se vea un sitio de recursos allí.

**Empieza con tu historial.** Tus diarios antiguos guardan años de esto. Usa **Importar diarios antiguos** en la
[pestaña Comandante](UI-Commander-Tab.html), o di ``analizar diarios en busca de zonas de caza``.

## Pedir un conjunto

| Di | Qué ocurre |
|---|---|
| ``buscar misiones de masacre de piratas en 50 años luz`` | Vega nombra el mejor conjunto al alcance: el sistema proveedor y cuántas facciones ofrecen misiones allí, la facción pirata y el sistema objetivo, sus sitios de recursos y qué estaciones visitar. Primero las pilas más profundas, luego las más cercanas. Sin alcance, busca a 100 años luz. |
| ``navegar al proveedor de misión pirata`` | Traza una ruta a un proveedor: primero a uno de las misiones que ya llevas, luego a uno de la zona de caza en la que estás, luego al mejor conjunto cercano. Un recordatorio nombra el sistema objetivo. |
| ``navegar al objetivo de misión pirata`` | Traza una ruta a la zona de caza de las misiones que llevas. |
| ``buscar zona de caza`` | El Sitio de Extracción de Recursos más cercano que vale la pena solo por las recompensas, sin misiones. |
| ``olvida esta zona de caza`` | Dicho en una zona de caza que no te gusta (demasiado fácil, demasiado difícil, demasiado lejos de la estrella, el poder equivocado). Vega pregunta primero y después no vuelve a ofrecerla, ni ningún conjunto que te envíe allí. |

## Compartir conjuntos con otros comandantes

Compartir está desactivado hasta que lo activas en la página **Conjuntos de masacre** de la
[pestaña Comandante](UI-Commander-Tab.html).

Funciona en ambos sentidos: **mientras compartes, también recibes los conjuntos que otros comandantes han compartido.**
Cuantos más comandantes compartan, más conjuntos habrá para todos.

**Qué se comparte:** para cada conjunto, los nombres de sistemas estelares, facciones y estaciones, cuántos sitios de
recursos de cada grado tiene el objetivo y el día en que se vio el conjunto por última vez. Cuenta cada comandante de
tu ordenador, incluidos los que importaste.

**Qué nunca se comparte:** tu nombre de comandante, tus misiones, recompensas ni nada que diga quién eres. Consulta la
[Política de privacidad](Privacy.html).

Qué pasa con los conjuntos:

1. Tus conjuntos se suben al activar el uso compartido y, después, como mucho una vez al día, cuando has aprendido
   otros nuevos.
2. Cada noche se comprueban los conjuntos de todos los comandantes con datos de la galaxia. Los sistemas deben existir,
   el objetivo debe estar a 20 años luz o menos del proveedor, y la facción pirata debe estar de verdad en el sistema
   objetivo y ser una Anarquía. Los conjuntos que fallan se descartan. El resto se publica, sin decir quién los envió.
3. Una vez al día EliteIntel descarga los conjuntos publicados. Cuando pides un conjunto, se ofrecen junto a los tuyos.

**Un conjunto compartido solo se ofrece cuando tus propios datos muestran un sitio de recursos en su objetivo**, de tus
visitas o de EDDN. Es lo único que los datos de la galaxia no pueden comprobar. Si un conjunto compartido no aparece,
vuela a su sistema objetivo o espera a que EDDN lo informe.

**Vega te avisa cuando un conjunto viene de otros comandantes.** Tómalo como una pista que comprobar, no como algo que
hayas volado tú. Si no te convence, ``olvida esta zona de caza`` funciona igual que con tus propios conjuntos.

**Tu propio registro va primero.** Cuando conoces un conjunto tú mismo, se usa tu versión, y entre conjuntos de la
misma profundidad, los tuyos se ofrecen antes que los informados.

Si no compartes y EliteIntel no conoce ningún conjunto cercano, Vega te avisa cuando otros comandantes han informado de
alguno en la zona.

Desactivar el uso compartido quita tus conjuntos de los datos compartidos, y EliteIntel deja de usar los de los demás.

----
Comunidad 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
