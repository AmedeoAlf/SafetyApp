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
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL

class Util(private val ctx: Context) {
    companion object {
//        const val BASEURL = "http://192.168.178.22:3500/"

        const val BASEURL = "http://172.20.1.13/safetyApp/"
        var lastThrownException = MutableSharedFlow<Exception?>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    fun incrementPreferencesCounter(key: String) =
        CoroutineScope(Dispatchers.Default).launch { PreferencesManager(ctx).incrementInt(key) }

    // TODO make cleaner, log more usefully
    fun doRequest(endpoint: String, process: suspend (response: JSONObject) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val connection = URL(BASEURL + endpoint).openConnection().apply {
                connectTimeout = 3000
                readTimeout = 3000
            } as HttpURLConnection
            try {
                try {
                    process(
                        JSONTokener(
                            connection.inputStream.reader().readLines().joinToString("\n")
                        ).nextValue() as JSONObject
                    )
                } catch (e: Exception) {
                    lastThrownException.emit(e)
                    throw e
                }
                incrementPreferencesCounter("total_connections")
                lastThrownException.emit(null)
            } catch (_: SocketTimeoutException) {
                incrementPreferencesCounter("total_unreachable")
            } catch (_: ConnectException) {
                incrementPreferencesCounter("total_unreachable")
            } catch (e: NoRouteToHostException) {
                println("No clue what this is ${e.message}")
            } catch (e: FileNotFoundException) {
                println(e)
            } catch (e: JSONException) {
                println(e.message)
            } finally {
                connection.disconnect()
            }
        }
    }


}