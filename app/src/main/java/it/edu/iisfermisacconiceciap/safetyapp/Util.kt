package it.edu.iisfermisacconiceciap.safetyapp

import android.content.Context
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
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
            if (false) "http://192.168.178.78:3500/" else "http://172.20.1.13/safetyApp/"

        var lastExceptionThrown = MutableLiveData<Exception?>(null)

        private val requesting = Mutex(false)
    }

    fun incrementPreferencesCounter(key: String) =
        CoroutineScope(Dispatchers.Default).launch { prefs.incrementInt(key) }

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

    fun dispatchRequest(endpoint: String, process: suspend (response: JSONObject) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            if (!requesting.tryLock(this@Util)) return@launch

            try {
                val response = try {
                    httpGET(URL(BASEURL + endpoint))
                } catch (e: Exception) {
                    when (e) {
                        is ConnectException -> {
                            incrementPreferencesCounter("total_unreachable")
                        }
                    }
                    throw e
                }

                try {
                    process(JSONTokener(response).nextValue() as JSONObject)
                    incrementPreferencesCounter("total_connections")
                } catch (e: Exception) {
                    when (e) {
                        is SocketTimeoutException, is ConnectException -> incrementPreferencesCounter(
                            "total_unreachable"
                        )

                        is JSONException -> throw JSONException("${e.message} in $response")
                    }
                    throw e
                }

                lastExceptionThrown.postValue(null)
            } catch (e: Exception) {
                lastExceptionThrown.postValue(e)
            }
        }.invokeOnCompletion {
            requesting.unlock(this@Util)
        }
    }


}