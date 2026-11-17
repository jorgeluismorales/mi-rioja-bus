# Mi Bus Rioja

Aplicación Android no oficial para el transporte público de La Rioja (Argentina). Muestra los colectivos en tiempo real, planifica viajes entre dos puntos de la ciudad, programa alertas de proximidad y permite consultar tarifas, saldo de la tarjeta y puntos de recarga.

> **Aviso:** Mi Bus Rioja es un proyecto independiente y sin fines de lucro. No mantiene relación, vínculo ni afiliación alguna con Red Bus, Rioja Bus ni con organismos públicos de la provincia de La Rioja. Las marcas mencionadas pertenecen a sus respectivos titulares.

## Funcionalidades

- **Mapa en tiempo real**: recorrido y paradas de cada línea, posición de los colectivos cada 1 s con flecha de rumbo, ícono diferenciado para unidades con rampa, agrupamiento de marcadores y detalle del colectivo seleccionado. Recuerda la última línea elegida.
- **Planificador de viaje**: origen por GPS o dirección, búsqueda de destinos, destinos frecuentes y cálculo de recorridos directos o con un transbordo, con mapa del itinerario y duración estimada.
- **Alertas de proximidad**: aviso por notificación cuando el colectivo o el usuario se acercan a una parada; se pueden activar para los pasos de un viaje planificado. Servicio en primer plano que se detiene solo al no quedar alertas activas.
- **Tarifas vigentes**: valores dinámicos del servicio, con descuentos calculados y pase libre.
- **Saldo de tarjeta**: consulta anónima (sin login) con verificación captcha; opción de recordar el número de tarjeta.
- **Puntos de venta y recarga**: comercios cercanos ordenados por distancia, sobre el mapa y en lista.

## Stack

- Kotlin 2.0 · Jetpack Compose · Material 3 · Navigation Compose
- Hilt · Retrofit + Moshi + OkHttp · Room · DataStore · Coroutines/Flow
- osmdroid (OpenStreetMap) · Google Play Services Location · Nominatim para búsqueda de lugares
- minSdk 26 (Android 8.0) · targetSdk 35

## Compilar

Requisitos: JDK 17 y Android SDK (plataforma 35).

```bash
./gradlew :app:assembleDebug        # APK en app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # tests unitarios
./gradlew :app:lintDebug            # lint
```

Crear `local.properties` con la ruta del SDK si no está configurada:

```
sdk.dir=/ruta/a/Android/Sdk
```

## Estructura

```
app/src/main/java/com/mibusrioja/app
├── data/        API (Retrofit), repositorios, Room, DataStore, tiles del mapa
├── domain/      modelos, casos de uso (planificador, tarifas, saldo), geoutils
├── service/     servicio en primer plano de alertas
├── ui/          pantallas Compose (map, planner, alerts, tariffs, balance, sales, about)
└── util/        reporte de crashes/ANR
```

## Origen de los datos

Los recorridos, posiciones, tarifas, saldo y puntos de venta se consultan desde el servicio público de `larioja.miredbus.com.ar`. Los mapas son de OpenStreetMap. La aplicación no tiene publicidad ni servidores propios: ubicación, alertas y tarjeta guardada permanecen en el dispositivo.

## Licencia

Proyecto personal. Si querés reutilizar el código, abrí un issue o contactá al autor.
