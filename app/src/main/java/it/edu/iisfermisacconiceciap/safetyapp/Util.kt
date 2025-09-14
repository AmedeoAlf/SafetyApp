package it.edu.iisfermisacconiceciap.safetyapp

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
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

class Util(private val ctx: Context) {
    companion object {
        val BASEURL =
            if (false) "http://192.168.178.78:3500/" else "http://172.20.1.13/safetyApp/"

        var lastExceptionThrown = MutableSharedFlow<Exception?>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    fun incrementPreferencesCounter(key: String) =
        CoroutineScope(Dispatchers.Default).launch { PreferencesManager(ctx).incrementInt(key) }

    @Throws(Exception::class)
    fun httpGET(url: URL): String {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 2000
        connection.readTimeout = 1000

        return try {
            connection.inputStream.reader().readText()
        } finally {
            connection.disconnect()
        }
    }

    // TODO log more usefully
    private var requesting = false
    fun dispatchRequest(endpoint: String, process: suspend (response: JSONObject) -> Unit) {
        if (requesting) return
        CoroutineScope(Dispatchers.IO).launch {
            requesting = true

            val response = try {
                httpGET(URL(BASEURL + endpoint))
            } catch (e: Exception) {
                when (e) {
                    is ConnectException -> {
                        incrementPreferencesCounter("total_unreachable")
                    }
                }
                lastExceptionThrown.emit(e)
                return@launch
            }

            try {
                process(JSONTokener(response).nextValue() as JSONObject)
                incrementPreferencesCounter("total_connections")
                lastExceptionThrown.emit(null)
            } catch (e: Exception) {
                var e = e
                when (e) {
                    is SocketTimeoutException, is ConnectException -> {
                        incrementPreferencesCounter("total_unreachable")
                    }

                    is JSONException -> {
                        e = JSONException("${e.message} in $response")
                    }
                }
                lastExceptionThrown.emit(e)
            }
        }.invokeOnCompletion {
            requesting = false
        }
    }


}