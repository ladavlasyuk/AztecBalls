package com.AncientPar.AztecBalls.launch

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

internal data class WebRoute(val url: String, val ticket: String? = null)

internal fun readWebRoute(reply: String): WebRoute? {
    val body = reply.trimStart()
    if (body.startsWith("{") || body.startsWith("[")) {
        val stream = JSONTokener(body)
        try {
            while (stream.nextClean() != '\u0000') {
                stream.back()
                locateAddress(stream.nextValue())?.let { return WebRoute(it) }
            }
        } catch (_: JSONException) {
            return null
        }
        return null
    }

    val parts = reply.split('#', limit = 2)
    if (parts.size != 2 || parts.any { it.isBlank() }) return null
    return WebRoute(parts[1], parts[0])
}

private fun locateAddress(value: Any?): String? {
    when (value) {
        is JSONObject -> {
            val candidate = (value.opt("rdstr") as? String)?.trim()
            val address = when {
                candidate == null -> null
                candidate.startsWith("https\\://") || candidate.startsWith("http\\://") ->
                    candidate.replaceFirst("\\:", ":")
                else -> candidate
            }
            if (address?.toHttpUrlOrNull() != null) return address
            for (key in value.keys()) {
                locateAddress(value.opt(key))?.let { return it }
            }
        }
        is JSONArray -> for (index in 0 until value.length()) {
            locateAddress(value.opt(index))?.let { return it }
        }
    }
    return null
}
