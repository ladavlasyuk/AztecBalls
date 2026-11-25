package com.AncientPar.AztecBalls.launch

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit

class TempleGateway(private val context: Context) {

    internal val attemptLimits = listOf(15L, 15L, 30L)

    private fun timedClient(timeoutSeconds: Long): OkHttpClient =
        OkHttpClient.Builder()
            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .cache(null)
            .build()

    internal suspend fun execute(
        endpointUrl: String,
        deadlines: List<Long> = attemptLimits
    ): String? = withContext(Dispatchers.IO) {
        for (deadline in deadlines) {
            val content = runCatching {
                val client = timedClient(deadline)
                val callRequest = Request.Builder()
                    .url(endpointUrl)
                    .cacheControl(CacheControl.Builder().noCache().noStore().build())
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .build()
                client.newCall(callRequest).execute().use { result ->
                    if (result.isSuccessful) result.body?.string() else null
                }
            }.onFailure { error ->
                if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    Log.w("TempleGateway", "Connection attempt failed: ${error.javaClass.simpleName}; cause=${error.cause?.javaClass?.simpleName}")
                }
            }.getOrNull()
            if (!content.isNullOrBlank()) return@withContext content
        }
        null
    }

    suspend fun requestRoute(): String? = execute(requestAddress())

    internal fun requestAddress(): String {
        val snapshot = HandsetSnapshot(context)
        val parameters = StringBuilder()
            .append("&p=GFhfdGdsGdGuhk")
            .append("&os=").append(snapshot.platformName())
            .append("&lng=").append(snapshot.language())
            .append("&loc=").append(snapshot.localeRegion())
            .append("&devicemodel=").append(snapshot.hardwareName())
            .append("&bs=").append(snapshot.chargeStateName())
            .append("&bl=").append(snapshot.chargeFraction())
            .append("&nc=").append(snapshot.carrierCountry())
            .append("&sm=").append(snapshot.simStatus())
            .append("&dv=").append(if (snapshot.developerSettingsActive()) "1" else "0")
            .toString()
        val token = Base64.encodeToString(parameters.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val endpointUrl = ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("token", token)
            .build().toString()
        return endpointUrl
    }

    companion object {
        const val ENDPOINT = "https://afdsodin.top/aztecballs-privacy/"
        const val POLICY_ADDRESS = "https://afdsodin.top/aztecballs-privacy/fullprivacy.php"
    }
}
