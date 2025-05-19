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

class Util(private val ctx: Context) {
    companion object {
        //        const val BASEURL = "http://192.168.178.22:3500/"
        const val BASEURL = "http://172.20.1.13/safetyApp/"
    }

    fun incrementPreferencesCounter(key: String) =
        CoroutineScope(Dispatchers.Default).launch { PreferencesManager(ctx).incrementInt(key) }

    fun doRequest(endpoint: String, process: suspend (response: JSONObject) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val connection = URL(BASEURL + endpoint).openConnection() as HttpURLConnection
            try {
                process(
                    JSONTokener(
                        connection.inputStream.reader().readLines().joinToString("\n")
                    ).nextValue() as JSONObject
                )
                incrementPreferencesCounter("total_connections")
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