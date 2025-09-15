package it.edu.iisfermisacconiceciap.safetyapp

import org.json.JSONException
import org.json.JSONObject

class ServerErrorException(response: JSONObject) :
    Exception("\"ERROR\"=\"${response.getStringOrNull("ERROR")}\" nella risposta del server")

data class EmergencyState(
    val isEmergency: Boolean,
    val currEmergency: String,
    val currDescrizione: String,
    val error: Exception?,
) {
    companion object {
        val STARTING_STATE = EmergencyState(
            false,
            "Nessuna emergenza",
            "Nessuna descrizione",
            Exception("Nessuna risposta dal server")
        )
    }

    fun updateWith(json: JSONObject): EmergencyState {
        val error = when {
            json.has("ERROR") -> ServerErrorException(json)
            !json.has("STATO") -> JSONException("STATO non impostato")
            else -> null
        }
        return EmergencyState(
            isEmergency = json.getIntOrNull("STATO")?.let { it != 0 } ?: this.isEmergency,
            error = error,
            currEmergency = json.getStringOrNull("MESSAGGIO") ?: this.currEmergency,
            currDescrizione = json.getStringOrNull("DESCRIZIONE") ?: this.currDescrizione,
        )
    }

    fun updateWith(error: Exception): EmergencyState {
        return EmergencyState(
            isEmergency = this.isEmergency,
            error = error,
            currEmergency = this.currEmergency,
            currDescrizione = this.currDescrizione,
        )
    }
}