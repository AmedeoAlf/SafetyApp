package it.edu.iisfermisacconiceciap.safetyapp

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

fun JSONObject.getStringOrNull(key: String): String? {
    return if (has(key)) getString(key) else null
}

fun JSONObject.getIntOrNull(key: String): Int? {
    return if (has(key)) getInt(key) else null
}

class Util(ctx: Context) {
    private val prefs = PreferencesManager(ctx)

    companion object {
        val BASEURL =
            if (true) "http://192.168.178.78:3500/" else "http://172.20.1.13/safetyApp/"
    }

    fun dispatchIncrement(key: String) =
        CoroutineScope(Dispatchers.Default).launch { prefs.incrementInt(key) }

    @Throws(IOException::class, SocketTimeoutException::class)
    suspend fun httpGET(url: URL) = withContext(Dispatchers.IO) {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 2000
        connection.readTimeout = 1000

        return@withContext try {
            connection.inputStream.reader().readText()
        } finally {
            connection.disconnect()
        }
    }

    @Throws(SocketTimeoutException::class, ConnectException::class, JSONException::class)
    suspend fun getJSONObject(route: String) =
        JSONTokener(httpGET(URL(BASEURL + route))).nextValue() as JSONObject

}