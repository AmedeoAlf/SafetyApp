package it.edu.iisfermisacconiceciap.safetyapp

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.URL

class Util(val ctx: Context) {
    companion object {
        val baseURL = "http://172.20.1.13/safetyApp/"
    }
    fun incrementPreferencesCounter(key: String) {
        CoroutineScope(Dispatchers.Default).launch {
            PreferencesManager(ctx).incrementInt(
                key
            )
        }
    }

    fun doRequest(endpoint: String, process: (response: JSONObject) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                with(URL(baseURL + endpoint).openConnection() as HttpURLConnection) {
                    process(
                        JSONTokener(
                            inputStream.reader().readLines().joinToString("\n")
                        ).nextValue() as JSONObject
                    )
                    disconnect()
                }
                incrementPreferencesCounter("total_connections")
            } catch (_: ConnectException) {
                incrementPreferencesCounter("total_unreachable")
            } catch (e: NoRouteToHostException) {
                println("No clue what this is ${e.message}")
            } catch (e: JSONException) {
                println(e.message)
            }
        }
    }
}