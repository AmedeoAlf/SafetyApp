package it.edu.iisfermisacconiceciap.safetyapp

import android.content.Context
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
    fun incrementPreferencesCounter(key: String) {
        CoroutineScope(Dispatchers.Default).launch {
            PreferencesManager(ctx).incrementInt(
                key
            )
        }
    }

    fun doRequest(url: URL, process: (response: JSONObject) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                with(url.openConnection() as HttpURLConnection) {
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
            } catch (e: FileNotFoundException) {
                println("Couldn't find endpoint on server ($e)")
            } catch (e: NoRouteToHostException) {
                println("No clue what this is ${e.message}")
            } catch (e: JSONException) {
                println(e.message)
            }
        }
    }
}