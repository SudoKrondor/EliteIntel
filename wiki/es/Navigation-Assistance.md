# Asistencia de navegación

EliteIntel (EI) proporciona asistencia de navegación en *Elite Dangerous*. Admite el cálculo de rutas por coordenadas galácticas para Portaflota, navegación por coordenadas de superficie en planetas y lunas, y seguimiento de muestras de exobiología.

## Planificación de rutas para Portaflota

EliteIntel utiliza [Spansh](https://spansh.co.uk) para planificar las rutas de salto de los Portaflota. Para calcular una ruta:

1. **Abrir el mapa galáctico**: Cualquier vista del mapa galáctico funciona. El mapa específico del Portaflota no es necesario en este paso.
2. **Copiar el nombre del destino**: Selecciona el sistema estelar de destino. Haz clic en el botón de copiar (el último botón a la derecha en la interfaz del mapa galáctico). El nombre del sistema quedará en tu portapapeles.
3. **Solicitar el cálculo de la ruta**: Di "Calcula la ruta del Portaflota." EliteIntel lee el nombre del sistema desde el portapapeles, comprueba la posición actual del portador y consulta Spansh para generar una ruta. La ruta se guarda en la sesión actual.

**Nota**: La base de datos de Spansh puede no incluir todos los sistemas. Si el sistema actual del portador es desconocido para Spansh, EliteIntel selecciona la estrella conocida más cercana dentro del alcance de salto como punto de partida. Si el sistema de destino no está en Spansh, puede ser necesaria una alternativa cercana.

### Ejecución de la ruta

Una vez calculada la ruta:

1. **Abrir el mapa galáctico del Portaflota**: Accede al mapa desde el menú de gestión del portador y haz clic en el campo de texto superior.
2. **Obtener el siguiente destino**: Di "Introduce el siguiente destino del Portaflota." EliteIntel pega el nombre del siguiente sistema en el campo.
3. **Programar el salto**: Confirma el salto en el juego. Repite para cada tramo de la ruta.

## Navegación por coordenadas de superficie

EliteIntel proporciona navegación guiada por voz hacia coordenadas específicas en un planeta o luna. No se requiere ningún marcador de navegación.

1. **Iniciar la navegación**: Di "Navegar a latitud 41.4325 longitud -75.2309" (sustituye con tus coordenadas objetivo). EliteIntel te guía desde la órbita hasta la superficie.
2. **Aproximación orbital**: Mantén una velocidad moderada en supercruise. Hay un ligero retraso en la síntesis de voz, así que evita velocidades excesivas para poder seguir las instrucciones con precisión. Si vas demasiado lento, la nave puede salir prematuramente del supercruise. Navegar el lado oscuro de un planeta requiere vuelo instrumental.
3. **Fase de planeo**: A aproximadamente 400 km de la superficie, EliteIntel proporciona actualizaciones del ángulo de planeo como "Ángulo de planeo pronunciado, -40 grados." La decisión de planear o reposicionarse para un mejor ángulo es del piloto. EliteIntel solo proporciona orientación.
4. **Navegación en superficie**: Tras aterrizar, EliteIntel te guía hasta a 1.000 metros del objetivo y te indica que busques un lugar para aterrizar. A partir de ese punto, EliteIntel continúa dirigiéndote en el SRV o a pie hasta que estés a menos de 50 metros del objetivo.
5. **Cancelar la navegación**: Di "Cancelar navegación" en cualquier momento para detenerla. Se acepta lenguaje natural.

## Notas de uso

- **Lenguaje natural**: No se requiere sintaxis de comandos formal. Se acepta el habla natural.
- **Navegación en el lado oscuro**: Navegar hacia coordenadas en el lado oscuro de un planeta requiere vuelo instrumental.
- **Limitaciones de Spansh**: Si Spansh no reconoce tu sistema actual, prueba con una estrella adyacente para iniciar la ruta.

----
Comunidad 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
