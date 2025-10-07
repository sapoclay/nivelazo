# NivelAzo 

<img width="1024" height="1536" alt="logo" src="https://github.com/user-attachments/assets/7b918506-b785-44b4-8841-aa52e3807515" />

Esta es una pequeña aplicación Android simple que utiliza el acelerómetro para mostrar un nivel virtual. Permite calibrar y mostrar una "bola" que indica el nivel en la pantalla. Diseñada para Android (Kotlin/Java, Gradle).

## Características

- Detección de inclinación usando el sensor acelerómetro.
- Interfaz visual con bola indicadora del nivel.
- Soporte de orientación con fullSensor para rotación completa.
- Calibración simple desde la interfaz.
- Soporte mínimo para pruebas y ejecución desde Android Studio.

## Requisitos

<img width="398" height="881" alt="NivelAzon-funcionando" src="https://github.com/user-attachments/assets/1e8494b6-e434-4b13-85b7-d1e52d2dfedd" />

- Sistemas: Linux (desarrollo).
- Android Studio Narwhal 3 Feature Drop | 2025.1.3 (recomendado).
- JDK 11+.
- Android SDK con API mínima y herramientas instaladas (`build.gradle`).
- Dispositivo o emulador con sensor de acelerómetro (la app requiere acelerómetro).

## Configuración relevante

- La app requiere acceso al sensor acelerómetro (configurado en el manifiesto).
- La `Activity` principal está configurada con `screenOrientation="fullSensor"` y `configChanges` para manejar cambios de orientación sin reinicio completo.

## Problemas conocidos / Notas de usabilidad

### Según pruebas internas y feedback:

- Al apoyar el teléfono sobre el lateral en modo vertical, la interfaz no siempre centra la bola correctamente; la bola puede aparecer en el lateral.
- Al poner el teléfono en `landscape`, la app puede no marcar el nivel correctamente en todas las orientaciones.
- En `landscape` la bola puede quedar siempre a la derecha si la pantalla está rotada hacia un lado (solo funciona bien cuando la pantalla mira hacia arriba).
- Recomendación: revisar la lógica de mapeo de ejes del acelerómetro según la rotación del dispositivo (`Display.getRotation()` y remapear ejes con `SensorManager.remapCoordinateSystem` o adaptar la interpretación de los valores X/Y/Z según `rotation`).

### Sugerencias de solución (tareas para desarrollar)

- Implementar remapeo de ejes por rotación del display.
- Revisar el cálculo del centro y límites de la UI para que responda al tamaño real disponible cuando la actividad está apoyada lateralmente.
- Añadir tests instrumentados que simulen diferentes rotaciones y valores del sensor.

## Contribuir

- Crear un _branch_ por funcionalidad y abrir Pull Request.
- Añadir descripciones claras y pasos para reproducir bugs.
- Incluir pruebas cuando sea posible.

## Licencia

- Añadir la licencia deseada en `LICENSE`.

## Contacto

- Autor/maintainer: `sapoclay` (https://github.com/sapoclay).
