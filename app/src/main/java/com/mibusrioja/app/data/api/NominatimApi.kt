package com.mibusrioja.app.data.api

import com.mibusrioja.app.data.api.model.NominatimPlaceDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {

    @GET("search")
    suspend fun search(
        @Query("q") q: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 6,
        @Query("viewbox") viewbox: String = "-67.05,-29.25,-66.70,-29.55",
        @Query("bounded") bounded: Int = 1,
        @Query("countrycodes") cc: String = "ar",
        @Query("accept-language") lang: String = "es"
    ): List<NominatimPlaceDto>
}
