package com.AncientPar.AztecBalls.launch

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal sealed interface Arrival {
    object Game : Arrival
    object Unavailable : Arrival
    data class Website(val address: String) : Arrival
}

internal class ArrivalRouter(context: Context, private val fetch: suspend () -> String?) {
    private val routes = context.getSharedPreferences("aztec_arrival_state", Context.MODE_PRIVATE)

    suspend fun resolve(): Arrival = withContext(Dispatchers.IO) {
        val remembered = routes.getString("landing_address", null)
        if (!remembered.isNullOrBlank()) return@withContext Arrival.Website(remembered)
        val response = fetch()
        if (response.isNullOrBlank()) return@withContext Arrival.Unavailable
        val route = readWebRoute(response) ?: return@withContext Arrival.Game
        routes.edit().putString("landing_address", route.url)
            .putString("landing_ticket", route.ticket).apply()
        Arrival.Website(route.url)
    }
}
