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

class Util(private val ctx: Context) {
    companion object {
        val BASEURL =
            if (false) "http://192.168.178.22:3500/" else "http://172.20.1.13/safetyApp/"

        var lastExceptionThrown = MutableSharedFlow<Exception?>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    fun incrementPreferencesCounter(key: String) =
        CoroutineScope(Dispatchers.Default).launch { PreferencesManager(ctx).incrementInt(key) }

    // TODO log more usefully
    fun doRequest(endpoint: String, process: suspend (response: JSONObject) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val connection = URL(BASEURL + endpoint).openConnection().apply {
                connectTimeout = 3000
                readTimeout = 3000
            } as HttpURLConnection
            val response = connection.inputStream.reader().readText()
            runCatching {
                process(JSONTokener(response).nextValue() as JSONObject)
                incrementPreferencesCounter("total_connections")
                lastExceptionThrown.emit(null)
            }.onFailure { e ->
                var e = e as Exception
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
            connection.disconnect()
        }
    }


}