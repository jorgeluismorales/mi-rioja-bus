# Mi Bus Rioja — App Android

App nativa Android (Kotlin + Jetpack Compose) que consume la API de `larioja.miredbus.com.ar` con funcionalidades extendidas de alertas GPS, planificador de viajes y notificaciones.

---

## Estado general (auditoría 2026-11-12)

| Área | Estado | Notas |
|------|--------|-------|
| Bottom nav 5 tabs + tema oscuro naranja | ✅ COMPLETADO | Inter vía Google Fonts descargables (fallback sans-serif sin GMS) |
| Mapa tiempo real (recorrido, paradas, buses, GPS, arribos) | ✅ COMPLETADO | Corregidos endpoint `posicionesBuses`, campo `codigoParada`, polling 5 s en mapa, fallback de tiles |
| Selector de líneas por grupo | ✅ CORREGIDO | Antes mostraba sólo "RIOJA BUS"/"RIOJA BUS CHILECITO"; ahora TRONCALES / COMPLEMENTARIAS / RURALES / SERVICIOS ESPECIALES / CHILECITO |
| Planificador de viaje | ✅ COMPLETADO | Directo + 1 transbordo real, mapa del recorrido, búsqueda de destino (Nominatim), alertas automáticas |
| Alertas GPS (ForegroundService) | ✅ COMPLETADO | Proximidad del usuario **y** del colectivo; se autoapaga sin alertas activas; chequeo de permisos |
| Tarifas | ✅ COMPLETADO | Corregido crash por claves duplicadas ("Rurales:" ×5) y filtrado de valores "$0000,00" |
| Consulta de saldo | ✅ COMPLETADO | POST real + captcha de imagen con cookie de sesión. Movimientos / vincular tarjeta: **no disponibles sin login** (ver pendientes) |
| Puntos de venta | ✅ COMPLETADO | Estaba implementado pero **sin ruta de navegación**; ahora accesible desde Saldo. Endpoint y tipos reales |
| Tests unitarios | ✅ 31 tests JVM | Fixtures reales capturados en `app/src/test/resources/fixtures/` |
| Build / lint | ✅ `assembleDebug`, `testDebugUnitTest`, `lintDebug` en verde | Requiere JDK 17 (`gradle.properties` → `org.gradle.java.home`) |

### Pendientes / limitaciones técnicas
- **Últimos movimientos y vincular tarjeta**: la API anónima (`resultadoSaldo`) sólo devuelve saldo, tipo, estado y fecha. Los movimientos (`tarjetaInternal/showTransactions`) y la vinculación requieren login (`loginInternal/login`), descartado por el plan ("sin login"). La pantalla lo informa y enlaza al portal oficial.
- **"Cuando viene"**: `horariosEstimados/{linea}/{parada}` responde hoy `horariosServicio: []` para todas las paradas (la config indica `habilitarCuandoVieneMapBus:false`). La UI muestra estado vacío; se mostrará automáticamente cuando el servidor devuelva datos (`tiempoFaltante` en segundos).
- **Tiles propios** (`mapmoblrj.red-bus.com.ar`): hoy redirigen todos a `tile_404.png`. `TileSourceProvider` prueba un tile real al iniciar y cae a OSM si no responde una imagen.
- **Geofencing API de Google**: se usa `FusedLocationProviderClient` (balanced power, 10 s) dentro del ForegroundService en vez de la Geofencing API, porque las alertas por *colectivo* requieren polling de `posicionesBuses` y un geofence sólo cubriría al usuario. Posible optimización futura de batería.
- **Verificación manual en dispositivo** (GPS, notificaciones, tiles, mapa) pendiente: no hay emulador/dispositivo en este entorno; sólo se validó build, lint y tests JVM contra respuestas reales de la API.
- Ícono de launcher: adaptive icon vectorial (`mipmap-anydpi-v26`); no hay PNG legacy (minSdk 26, no hace falta).

---

## Contexto & Endpoints confirmados (verificados en vivo el 2026-11-12)

**Base URL:** `https://larioja.miredbus.com.ar`

| Endpoint | Descripción |
|----------|-------------|
| `GET /rest/getConfiguracion` | Config general: tarifas (`menu[tipo=tarifas].contenidoJSON`), centro mapa, tiles URL |
| `GET /rest/gruposLineas` | Árbol: raíz → "RIOJA BUS" (TRONCALES, COMPLEMENTARIAS, RURALES, SERVICIOS ESPECIALES) y "RIOJA BUS CHILECITO" (DISTRITOS → …) |
| `GET /rest/rutaLinea/{codLinea}` | Recorrido: `nodos[{latitud, longitud, parada, codigoParada, descripcionParada}]` |
| `GET /rest/posicionesBuses/{codLinea}` | Posición en tiempo real: `posiciones[{interno, latitud, longitud, orientacion, proximaParada, vehiculoRampa, vehiculoNoVisibles}]` (polling 5 s en mapa, 15 s en servicio de alertas) |
| `GET /rest/horariosEstimados/{codLinea}/{codParada}` | "Cuando viene": `horariosServicio[{interno, tiempoFaltante(seg)}]` |
| `GET /rest/getPuntosVentaCercanos/{lat}/{lng}/1` | Puntos de venta: `puntosVenta[{id, tipo(1-6), nombre, domicilio, detalleDomicilio, latitud, longitud}]` |
| `POST /rest/tarjetaInternal/resultadoSaldo` | Saldo anónimo. Body `{verificacionCaptcha, nroExternoTarjeta}` → `{error, numeroTarjeta, tipoTarjeta, estadoTarjeta, saldos:[{label:"Saldo Principal (Dinero)", value:"2300.00 pasajes"}], fechaSaldo:"09/11/2026 22:19"}` (respuesta real tarjeta 3276226). `value` es **string con sufijo** → se parsea con regex. error 1 = captcha, 2 = nro inválido, 5 = tarjeta sin saldo online. La web oficial usa Cloudflare Turnstile por defecto (header `X-Use-New-Captcha: true`) y cae a `captcha.png` + cookie `JSESSIONID` como fallback: la app usa este segundo modo, mismo contrato |
| `GET /captcha.png?t=…` | Imagen captcha ligada a la cookie de sesión (OkHttp con CookieJar) |
| `GET nominatim.openstreetmap.org/search` | Búsqueda de destinos del planificador (viewbox La Rioja) |
| Mapa tiles principal: `https://mapmoblrj.red-bus.com.ar/tiles/{z}/{x}/{y}.png` | Tiles propios (hoy caídos → fallback automático) |
| Mapa tiles fallback: `https://b.tile.openstreetmap.org/{z}/{x}/{y}.png` | OpenStreetMap estándar |

> [!WARNING]
> Los endpoints que figuraban originalmente (`/rest/posicion/{cod}`, `/rest/paradasCercanas/{l}/{p}`, `GET /rest/checkCardCredit`, `/rest/tarjetas`) **devuelven 404**. Los reales se obtuvieron desofuscando `js/endpoints.min.js`, `mapa-de-buses.min.js`, `puntos-de-venta.min.js` y `CardCredit.min.js` de la web oficial y se verificaron con `curl`. El servidor exige un `User-Agent` razonable (WAF).

> [!NOTE]
> **Sin login**: La app solo usará endpoints públicos. La consulta de saldo se hace anónimamente con número de tarjeta + captcha.

---

## Pantallas de la App

### 1. Home (Bottom Nav con 5 tabs)
- 🗺️ Mapa en Tiempo Real ✅
- 🧭 Planificador de Viaje ✅
- 🔔 Alertas ✅
- 💰 Tarifas ✅
- 💳 Saldo / Tarjeta ✅ (desde acá se accede a Puntos de Venta)

### 2. Mapa en Tiempo Real ✅
- Mapa OSM con tiles propios (`mapmoblrj.red-bus.com.ar`) + fallback OSM estándar (fallback automático por `TileSourceProvider`)
- Selector de línea con chips (grupos: Troncales, Complementarias, Rurales, Chilecito)
- Al seleccionar línea:
  - Dibuja **polyline del recorrido completo** con los nodos de `/rest/rutaLinea/{cod}`
  - Coloca **markers en cada parada** con popup de nombre de parada
  - Muestra **buses en tiempo real** (polling 5 s a `/rest/posicionesBuses/{cod}`) con pictograma de bus siempre derecho y flecha exterior según orientación; buses con rampa en azul + insignia ♿; la última línea elegida se guarda en DataStore y se restaura al abrir
  - Ajusta el viewport al bounds del recorrido
- Botón "Mi ubicación" (GPS) con marker del usuario
- Tap en parada → muestra tiempos estimados de llegada (hoy el servidor devuelve lista vacía; ver pendientes)
- Tap en bus → muestra número interno

### 3. Planificador de Viaje ✅
- Punto de origen = GPS automático
- Campo de destino con búsqueda (presets + Nominatim acotado a La Rioja)
- Algoritmo de rutas: `gruposLineas` + `rutaLinea` de todas las líneas (cacheadas); directo ≤800 m a pie, o 1 transbordo con paradas a ≤300 m entre líneas
- Muestra recorrido en mapa (polylines por tramo, paradas de subida/bajada/transbordo, origen y destino)
- **Auto-configura alertas** de 100m en cada parada de transbordo y en la parada de bajada final (sin duplicar), e inicia el servicio

### 4. Alertas de Líneas ✅
- Lista de alertas activas configuradas por el usuario
- Crear alerta: elegir línea + parada + distancia (50m / 100m / 200m / 500m)
- Servicio en segundo plano (ForegroundService) con FusedLocation (ver nota sobre Geofencing API) + polling de `posicionesBuses` cada 15 s
- Notificación push cuando el colectivo o la parada está a X distancia

### 5. Tarifas ✅
- Carga desde `/rest/getConfiguracion`
- Cards con el precio por tipo de usuario

### 6. Consulta de Saldo ✅ (parcial por API)
- Input número de tarjeta + captcha de imagen (con refresco, 192×67 dp)
- Muestra saldo, etiqueta (`saldos[0].label`), tipo, estado y fecha del saldo
- Bug corregido (2ª ronda): el saldo salía `$0` porque `value` real es `"2300.00 pasajes"` y el parser esperaba sólo número → `parseSaldoValue` extrae el primer número (fixture `resultadoSaldo_ok.json`)
- ⚠️ Últimos movimientos: NO disponibles en la API anónima (requieren login) — se informa en pantalla
- ⚠️ Opción de vincular tarjeta a cuenta: requiere login → fuera del alcance "sin login"

### 7. Puntos de Venta ✅ (acceso desde Saldo → "Puntos de recarga cercanos")
- Mapa con markers de puntos de recarga/venta
- Filtro por tipo (ATM / Común / Abono) — mapeo desde `tipo` 1-6 de la API
- Lista scrolleable con distancia calculada desde GPS
- ✅ Crash al abrir la pantalla (Xiaomi/Android 15) **corregido**: stacktrace real = `NPE MapView.getRepository()` en `Marker(map)` dentro del `LaunchedEffect`. Causa real: el `factory` del `AndroidView` escribe `mapViewRef = this` durante el apply, antes de que corran los efectos; el `DisposableEffect(lifecycleOwner, mapViewRef)` se registraba con clave `null` pero su cuerpo leía el state (ya = mapa). En la recomposición siguiente la clave cambia → `onDispose` → `map.onDetach()` **sobre el mapa vivo**; luego el `LaunchedEffect` creaba `Marker(map)` sobre un mapa destruido. Fix (3 mapas): leer el valor en composición (`val lifecycleMap = mapViewRef`) y usarlo como clave y dentro del efecto; además `setDestroyMode(false)` y guard `tileRequestCompleteHandler == null` en Puntos de venta. Cambios previos: eliminado `Configuration.load()` de osmdroid (el mapa principal no lo usa y funciona), `userAgentValue` se fija en el factory, y corregido `DisposableEffect` que llamaba `onDetach()` sobre el MapView recién creado (afectaba Mapa/Planificador/Puntos de venta). Se agregó `CrashReporter`: si la app se cierra por error, al reabrir muestra el stacktrace con botón "Compartir" para diagnosticar sin adb.

---

## Rediseño UI (2026-11-13) ✅

Sistema de diseño compartido en `ui/components/AppComponents.kt` (Dimens, ScreenHeader, AppCard, PrimaryButton, SecondaryButton, AppTextField, EmptyState, StatusPill, SectionTitle, AppBottomSheet, ErrorBanner). Dark slate + naranja, sin duplicar componentes por pantalla.

| Pantalla | Cambios | Decisiones / discrepancias con la especificación |
|---|---|---|
| Mapa | Chips superiores reemplazados por botón flotante "Filtros" (sheet con grupo, línea, "Mostrar paradas", "Solo con rampa"); clustering de colectivos por zoom (`domain/geo/BusClusterer.kt`, grilla Web-Mercator 64 px, sin cluster desde zoom 17); sheet oscuro al tocar un bus (interno, línea, próxima parada, rampa, rumbo, "actualizado hace Xs") | La API `posicionesBuses` **no publica patente ni ETA por colectivo** y `horariosEstimados` devuelve `[]` → no se inventan; el sheet lo aclara |
| Planificador | Tarjeta "Tu ubicación actual / Cerca de vos" con edición manual del origen (GPS o búsqueda Nominatim), sin coordenadas crudas; buscador "¿A dónde querés ir en La Rioja?"; chips Plaza 25 de Mayo / UNLaR; "Tus destinos frecuentes" persistidos en Room (`frequent_destinations`, DB v2 + `MIGRATION_1_2`, se registran al planificar con éxito, borrables) | — |
| Alertas | Sin FAB; header "Mis Alertas / Avisos de proximidad a tus paradas"; estado vacío con botón "Crear primera alerta de proximidad"; con alertas: botón "Nueva alerta" bajo el header + cards | — |
| Tarifas | Cards uniformes; precio final calculado en `domain/usecase/TarifaPresenter.kt` | Backend devuelve `JUB-PEN: 40% Dto.` y `Discapacidad: PASE LIBRE` (no precios) → se calcula 40% sobre "Usuario Común" ($1.000,00 → **$600,00**) y Pase Libre → $0,00, mostrando la etiqueta. Entradas `$0000,00` se descartan |
| Saldo | Tarjeta digital "Mi Bus Rioja - Red Bus"; formulario sólo con número + switch "Guardar tarjeta para futuras consultas" (DataStore `user_prefs`) + "Consultar saldo" | **El captcha no se puede eliminar**: la API exige `verificacionCaptcha` (error 1 si falta). Se movió a un segundo paso (bottom sheet "Verificación de seguridad") que aparece al tocar "Consultar saldo". El número de tarjeta es un identificador impreso, no un secreto → almacenamiento app-private sin cifrado adicional |

## Arquitectura

```
app/
├── data/
│   ├── api/
│   │   ├── MireBusApi.kt          # Retrofit: rutaLinea, posicionesBuses, horariosEstimados, resultadoSaldo...
│   │   ├── NominatimApi.kt        # Búsqueda de destinos (OSM)
│   │   ├── NetworkModule.kt       # OkHttp (UA + CookieJar) + Retrofit (Hilt)
│   │   └── model/ApiModels.kt     # DTOs Moshi
│   ├── local/
│   │   └── AppDatabase.kt         # Room DB + AlertDao
│   └── repository/
│   │   ├── BusRepository.kt       # API + caché en memoria (config, líneas, recorridos)
│   │   └── AlertRepository.kt
│   └── map/
│       └── TileSourceProvider.kt  # Elige tiles propios u OSM probando un tile real
├── domain/
│   ├── model/                     # BusLine, BusStop, RouteNode, Alert, Card
│   ├── geo/GeoUtils.kt            # Haversine puro (testeable en JVM)
│   ├── usecase/
│   │   ├── UseCases.kt            # GetLines/GetConfig/GetRoute/GetBusPositions/GetArrivalTimes/GetPuntosVenta/CheckBalance/SearchPlaces
│   │   ├── PlanTripUseCase.kt     # Directo + 1 transbordo
│   │   └── AlertUseCases.kt
│   └── geofence/
│       └── GeofenceManager.kt     # Arranca el ForegroundService (chequea permisos)
├── ui/
│   ├── theme/                     # MaterialTheme Dark + colores Rioja (naranja #FF6B35)
│   ├── map/                       # MapScreen + BusMapViewModel + MapMarkers (OSMDroid)
│   ├── planner/                   # TripPlannerScreen + TripViewModel
│   ├── alerts/                    # AlertsScreen + AlertViewModel
│   ├── tariffs/                   # TarifasScreen (datos de /rest/getConfiguracion)
│   ├── balance/                   # SaldoScreen (consulta anónima)
│   └── sales/                     # SalePointsScreen (ruta "sales", sin tab)
└── service/
    ├── AlertForegroundService.kt  # GPS + polling de buses (15 s) + notificaciones
    └── NotificationHelper.kt      # (BusPollingService se integró en el ForegroundService)
```

**Stack tecnológico:**
- Kotlin + Jetpack Compose
- Navigation Compose (Bottom Nav)
- Retrofit 2 + OkHttp (API REST)
- Osmdroid (mapas OpenStreetMap)
- Room (base de datos local)
- Hilt (inyección de dependencias)
- WorkManager (tareas en background)
- Google Location Services + Geofencing API
- Coroutines + Flow
- Material Design 3

---

## Diseño Visual

- **Tema:** Dark mode predominante, con acentos en naranja/ámbar (#FF6B35) que refleja la identidad de Rioja Bus
- **Tipografía:** Inter (Google Fonts via downloadable fonts)
- **Estilo:** Glassmorphism en cards, gradientes suaves, animaciones fluidas
- **Bottom Navigation** con iconos animados
- **Cards** con fondo translúcido sobre fondo oscuro

---

## Funcionalidades Nuevas (vs app original)

### Sistema de Alertas GPS
1. Usuario elige una línea + parada específica + distancia umbral
2. `AlertForegroundService` escucha posición GPS continuamente
3. Cuando la distancia al colectivo (o a la parada) < umbral → **notificación push**
4. Geofencing API de Google para eficiencia en batería

### Planificador de Viajes con Combinaciones
1. Ingresa destino → app calcula ruta
2. Si requiere combinación: muestra paso a paso con líneas y paradas de transbordo
3. **Auto-crea alertas** de 100m para cada parada de transbordo
4. Notificación "Bajate en X paradas" cuando el colectivo se acerca

---

## Verificación

### Tests automatizados
```bash
# Requiere JDK 17: gradle.properties fija org.gradle.java.home=~/.local/share/jdk/jdk-17.0.21+9
./gradlew :app:assembleDebug       # ✅ OK
./gradlew :app:testDebugUnitTest   # ✅ 45 tests, 0 fallos
./gradlew :app:lintDebug           # ✅ 0 errores (warnings informativos)
./gradlew connectedAndroidTest     # ⏳ sin dispositivo/emulador en este entorno
```

Fixtures reales de la API en `app/src/test/resources/fixtures/`; `FakeMireBusApi` los sirve a los casos de uso.

### Verificación manual (pendiente, requiere dispositivo)
- Confirmar que el mapa carga tiles correctamente
- Verificar que el polyline del recorrido se dibuja al seleccionar una línea
- Verificar que las paradas aparecen como markers y muestran tiempos al tocar
- Verificar buses en tiempo real con polling (requiere que haya colectivos circulando)
- Probar GPS, alertas y notificaciones en dispositivo físico
- Probar planificador de viaje con origen GPS y destino manual
