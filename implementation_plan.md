# Mi Bus Rioja — App Android

App nativa Android (Kotlin + Jetpack Compose) que consume la API de `larioja.miredbus.com.ar` con funcionalidades extendidas de alertas GPS, planificador de viajes y notificaciones.

---

## Contexto & Endpoints confirmados

**Base URL:** `https://larioja.miredbus.com.ar`

| Endpoint | Descripción |
|----------|-------------|
| `GET /rest/getConfiguracion` | Config general: tarifas, centro mapa, tiles URL |
| `GET /rest/gruposLineas` | Árbol completo de líneas (TRONCALES, COMPLEMENTARIAS, RURALES, CHILECITO) |
| `GET /rest/rutaLinea/{codLinea}` | **Recorrido completo** de una línea: array de `nodos` con lat/lng + flag `parada` + `descripcionParada` |
| `GET /rest/posicion/{codLinea}` | **Posición en tiempo real** de los colectivos de una línea (polling cada 15s) |
| `GET /rest/paradasCercanas/{codLinea}/{codParada}` | Tiempos estimados de llegada a una parada ("cuando viene") |
| `GET /rest/getPuntosVentaCercanos` | Puntos de venta/recarga en el mapa |
| `GET /rest/tarjetas` + `/rest/checkCardCredit` | Consulta de saldo de tarjeta (sin login) |
| `GET /rest/checkCardCredit` | Saldo rápido con número de tarjeta |
| Mapa tiles principal: `https://mapmoblrj.red-bus.com.ar/tiles/{z}/{x}/{y}.png` | Tiles propios de La Rioja |
| Mapa tiles fallback: `https://b.tile.openstreetmap.org/{z}/{x}/{y}.png` | OpenStreetMap estándar |

> [!NOTE]
> Los endpoints de buses en tiempo real están **confirmados** por análisis del JS de la app web: `/rest/posicion/{codLinea}` y `/rest/rutaLinea/{codLinea}`. El tráfico del segundo CSV (`bus-tiempo-real.csv`) confirma que todo va al mismo host `larioja.miredbus.com.ar`. El polling de posiciones es cada **15 segundos** (como en la app original).

> [!NOTE]
> **Sin login**: La app solo usará endpoints públicos. La consulta de saldo se hace anónimamente con número de tarjeta + captcha.

---

## Pantallas de la App

### 1. Home (Bottom Nav con 5 tabs)
- 🗺️ Mapa en Tiempo Real
- 🧭 Planificador de Viaje
- 🔔 Alertas
- 💰 Tarifas
- 💳 Saldo / Tarjeta

### 2. Mapa en Tiempo Real
- Mapa OSM con tiles propios (`mapmoblrj.red-bus.com.ar`) + fallback OSM estándar
- Selector de línea con chips (grupos: Troncales, Complementarias, Rurales, Chilecito)
- Al seleccionar línea:
  - Dibuja **polyline del recorrido completo** con los nodos de `/rest/rutaLinea/{cod}`
  - Coloca **markers en cada parada** con popup de nombre de parada
  - Muestra **buses en tiempo real** (polling 15s a `/rest/posicion/{cod}`) con ícono rotado según orientación
  - Ajusta el viewport al bounds del recorrido
- Botón "Mi ubicación" (GPS) con marker del usuario
- Tap en parada → muestra tiempos estimados de llegada
- Tap en bus → muestra número interno

### 3. Planificador de Viaje
- Punto de origen = GPS automático
- Campo de destino con búsqueda
- Algoritmo de rutas: consulta `gruposLineas` + `paradasCercanas` para calcular combinaciones
- Muestra recorrido en mapa
- **Auto-configura alertas** de 100m antes de cada parada de transbordo

### 4. Alertas de Líneas
- Lista de alertas activas configuradas por el usuario
- Crear alerta: elegir línea + parada + distancia (50m / 100m / 200m / 500m)
- Servicio en segundo plano (ForegroundService) con geofencing
- Notificación push cuando el colectivo o la parada está a X distancia

### 5. Tarifas
- Carga desde `/rest/getConfiguracion`
- Cards con el precio por tipo de usuario

### 6. Consulta de Saldo
- Input número de tarjeta + captcha
- Muestra saldo, últimos movimientos
- Opción de vincular tarjeta a cuenta

### 7. Puntos de Venta
- Mapa con markers de puntos de recarga/venta
- Filtro por tipo (ATM / Común / Abono)
- Lista scrolleable con distancia calculada desde GPS

---

## Arquitectura

```
app/
├── data/
│   ├── api/
│   │   ├── MireBusApi.kt          # Retrofit: rutaLinea, posicion, gruposLineas, etc.
│   │   └── model/                 # DTOs: RouteNode, BusPosition, LineGroup, Tarifa...
│   ├── local/
│   │   ├── AppDatabase.kt         # Room DB
│   │   └── AlertDao.kt            # CRUD de alertas guardadas
│   └── repository/
│       ├── BusRepository.kt       # Combina API + Room
│       └── AlertRepository.kt
├── domain/
│   ├── model/                     # BusLine, BusStop, RouteNode, Alert, Card
│   ├── usecase/
│   │   ├── GetRouteUseCase.kt     # /rest/rutaLinea/{cod}
│   │   ├── GetBusPositionsUseCase.kt  # /rest/posicion/{cod}
│   │   ├── PlanTripUseCase.kt     # Calcula combinaciones
│   │   └── CreateAlertUseCase.kt
│   └── geofence/
│       └── GeofenceManager.kt
├── ui/
│   ├── theme/                     # MaterialTheme Dark + colores Rioja (naranja #FF6B35)
│   ├── map/                       # MapScreen + BusMapViewModel (OSMDroid)
│   ├── planner/                   # TripPlannerScreen + TripViewModel
│   ├── alerts/                    # AlertsScreen + AlertViewModel
│   ├── tariffs/                   # TarifasScreen (datos de /rest/getConfiguracion)
│   ├── balance/                   # SaldoScreen (consulta anónima)
│   └── sales/                     # PuntosVentaScreen
└── service/
    ├── AlertForegroundService.kt  # GPS + geofencing en background
    └── BusPollingService.kt       # Polling /rest/posicion cada 15s
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
./gradlew test          # Unit tests
./gradlew connectedTest # Instrumented tests
./gradlew lint          # Lint checks
```

### Verificación manual
- Confirmar que la app conecta correctamente a la API y muestra datos reales
- Verificar GPS y notificaciones en dispositivo físico o emulador con GPS simulado
- Probar planificador de rutas con combinaciones

---

## Verificación

### Tests automatizados
```bash
./gradlew test
./gradlew lint
./gradlew connectedAndroidTest
```

### Verificación manual
- Confirmar que el mapa carga tiles correctamente
- Verificar que el polyline del recorrido se dibuja al seleccionar una línea
- Verificar que las paradas aparecen como markers y muestran tiempos al tocar
- Verificar buses en tiempo real con polling (requiere que haya colectivos circulando)
- Probar GPS, alertas y notificaciones en dispositivo físico
- Probar planificador de viaje con origen GPS y destino manual
