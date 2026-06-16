# Seguimiento de Misiones de Masacre de Piratas

EliteIntel rastrea las Misiones de Masacre de Piratas, incluyendo el recuento de bajas, las facciones objetivo y los cálculos de recompensa. Monitorea las bajas y los pagos durante toda la sesión.

## Primeros Pasos

Para completar eficientemente las Misiones de Masacre de Piratas:

1. **Encuentra un Punto Caliente**: Usa [INARA](https://inara.cz) para encontrar un sistema con un Sitio de Extracción de Recursos Peligroso (Haz RES) donde múltiples facciones ofrezcan misiones contra la misma facción pirata. Acepta múltiples misiones contra la misma facción. EliteIntel rastrea la facción objetivo, el recuento de bajas y los detalles de pago.
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

- **Apilamiento de Misiones**: Aceptar múltiples misiones contra la misma facción aumenta la eficiencia en créditos. Usa INARA para identificar sistemas con múltiples proveedores de misiones calificados.
- **Consultas en Lenguaje Natural**: Se acepta el habla natural. ``¿Cuántos piratas quedan por matar?`` y ``¿Puntuación de misión de piratas?`` son ambas válidas. Especifica que la consulta es sobre piratas para evitar confusión con otras misiones activas.
- **Cambio de Personalidad**: Cambia entre modos de personalidad según sea necesario para adaptarte a tu estilo de juego actual.


---

# Encontrar misiones de masacre de piratas
## Función Experimental

Por el momento, la búsqueda manual en INARA para el apilamiento de misiones de piratas sigue siendo la forma más efectiva de cazar recompensas. Sin embargo, la aplicación tiene una función experimental basada en datos de IN**T**RA (no IN**A**RA). Esta es una forma menos eficiente pero más inmersiva de jugar a la caza de recompensas, aunque depende de la disponibilidad de datos en IN**T**RA y de que su sitio esté operativo.

Para usar esta función, aborda tu nave cazarrecompensas y pregunta ``Encuéntranos algunos campos de caza (dentro de X años luz)``  la nave intentará establecer conexión con IN**T**RA y obtener los pares de sistema objetivo / sistema proveedor de misiones. El éxito o fracaso depende de que la API de IN**T**RA devuelva los datos y del número de otros cazarrecompensas que ejecuten EDMC con el plugin de IN**T**RA.

Si y cuando esos datos te sean devueltos, el ordenador de la nave te dirá que encontró X campos de caza. Pregunta ``traza una ruta al campo de caza para reconocimiento``. Elite Intel trazará la ruta al campo de caza más cercano de la lista. Debes volar allí y confirmar la presencia del Sitio de Recursos. Si hay uno y la tasa de aparición es de tu agrado, confirma o rechaza este sistema como campo de caza potencial con un comando de voz.

Elite Intel puede confirmarlo automáticamente como campo de caza si detecta los sitios RES en el diario al entrar o al escanear la baliza de navegación. Hay casos en que esto no ocurre porque no hay registros de sitios RES guardados por el juego en el diario. Se requiere confirmación manual.

Una vez satisfecho con el campo de caza, pregunta ``navega al sistema proveedor de misiones de masacre de piratas`` o algo similar. La aplicación trazará una ruta al sistema proveedor de misiones. Vuela hasta allí, aterriza en los puertos y recoge misiones contra piratas para la misma facción y ubicación del sistema de campo de caza. Cuando recojas tu primera misión de masacre, este par de sistemas quedará confirmado como campo de caza / proveedor de misiones. La aplicación te informará si hay otros sistemas que conoce con misiones contra la misma facción en el sistema objetivo  si, por supuesto, IN**T**RA tiene esos datos.

Cuando tengas tus misiones apiladas, pregunta ``traza ruta a misión activa`` y dispara a los piratas como de costumbre. Cuando hayas terminado las misiones, la misma solicitud ``traza ruta a misión activa`` te llevará a donde está el objetivo, esta vez el puerto donde recogiste la misión.

----
Comunidad 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
