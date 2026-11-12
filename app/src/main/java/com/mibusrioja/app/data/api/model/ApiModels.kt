package com.mibusrioja.app.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── Configuración general ─────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class ConfigResponse(
    @Json(name = "error") val error: Int = 0,
    @Json(name = "nombre") val nombre: String = "",
    @Json(name = "latitud") val latitud: Double = -29.4134003,
    @Json(name = "longitud") val longitud: Double = -66.8560292,
    @Json(name = "urlPrincipalMapas") val urlPrincipalMapas: String = "",
    @Json(name = "urlSecundariaMapas") val urlSecundariaMapas: String = "",
    @Json(name = "habilitarMapaBuses") val habilitarMapaBuses: Boolean = true,
    @Json(name = "habilitarCuandoViene") val habilitarCuandoViene: Boolean = true,
    @Json(name = "menu") val menu: List<MenuItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MenuItemDto(
    @Json(name = "titulo") val titulo: String = "",
    @Json(name = "icono") val icono: String = "",
    @Json(name = "tipo") val tipo: String = "",
    @Json(name = "contenidoJSON") val contenidoJSON: List<TarifaItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class TarifaItemDto(
    @Json(name = "key") val key: String = "",
    @Json(name = "value") val value: String = "",
    @Json(name = "icon") val icon: String? = null
)

// ── Grupos de Líneas ──────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class GruposLineasResponse(
    @Json(name = "error") val error: Int = 0,
    @Json(name = "grupos") val grupos: GrupoDto? = null
)

@JsonClass(generateAdapter = true)
data class GrupoDto(
    @Json(name = "codGrupo") val codGrupo: String = "",
    @Json(name = "subGrupos") val subGrupos: List<GrupoDto>? = null,
    @Json(name = "lineas") val lineas: List<LineaDto>? = null
)

@JsonClass(generateAdapter = true)
data class LineaDto(
    @Json(name = "codLinea") val codLinea: String = "",
    @Json(name = "descripcion") val descripcion: String = ""
)

// ── Ruta de línea (polyline + paradas) ───────────────────────────────────────
@JsonClass(generateAdapter = true)
data class RutaLineaResponse(
    @Json(name = "error") val error: Int? = null,
    @Json(name = "nodos") val nodos: List<NodoDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class NodoDto(
    @Json(name = "latitud") val latitud: Double = 0.0,
    @Json(name = "longitud") val longitud: Double = 0.0,
    @Json(name = "parada") val parada: Boolean = false,
    @Json(name = "codParada") val codParada: String? = null,
    @Json(name = "descripcionParada") val descripcionParada: String? = null
)

// ── Posiciones de buses (tiempo real) ─────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class PosicionResponse(
    @Json(name = "error") val error: Int? = null,
    @Json(name = "posiciones") val posiciones: List<PosicionDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PosicionDto(
    @Json(name = "latitud") val latitud: Double = 0.0,
    @Json(name = "longitud") val longitud: Double = 0.0,
    @Json(name = "orientacion") val orientacion: Float = 0f,
    @Json(name = "interno") val interno: String = "",
    @Json(name = "vehiculoRampa") val vehiculoRampa: Boolean = false,
    @Json(name = "fuera") val fuera: Boolean = false
)

// ── Paradas cercanas / cuando viene ──────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class ParadasCercanasResponse(
    @Json(name = "error") val error: Int? = null,
    @Json(name = "servicios") val servicios: List<ServicioDto>? = null,
    @Json(name = "horariosServicio") val horariosServicio: List<HorarioDto>? = null
)

@JsonClass(generateAdapter = true)
data class ServicioDto(
    @Json(name = "horariosServicio") val horariosServicio: List<HorarioDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HorarioDto(
    @Json(name = "tiempoFaltante") val tiempoFaltante: Int? = null,
    @Json(name = "interno") val interno: String? = null
)

// ── Puntos de venta ───────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class PuntosVentaResponse(
    @Json(name = "error") val error: Int = 0,
    @Json(name = "puntosVenta") val puntosVenta: List<PuntoVentaDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PuntoVentaDto(
    @Json(name = "nombre") val nombre: String = "",
    @Json(name = "direccion") val direccion: String = "",
    @Json(name = "latitud") val latitud: Double = 0.0,
    @Json(name = "longitud") val longitud: Double = 0.0,
    @Json(name = "tipo") val tipo: String = "comun",
    @Json(name = "horario") val horario: String? = null
)

// ── Saldo / tarjeta ───────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class SaldoResponse(
    @Json(name = "error") val error: Int = 0,
    @Json(name = "saldo") val saldo: Double? = null,
    @Json(name = "nroTarjeta") val nroTarjeta: String? = null,
    @Json(name = "tipo") val tipo: String? = null,
    @Json(name = "movimientos") val movimientos: List<MovimientoDto>? = null
)

@JsonClass(generateAdapter = true)
data class MovimientoDto(
    @Json(name = "fecha") val fecha: String = "",
    @Json(name = "monto") val monto: Double = 0.0,
    @Json(name = "tipo") val tipo: String = "",
    @Json(name = "descripcion") val descripcion: String? = null,
    @Json(name = "linea") val linea: String? = null
)
