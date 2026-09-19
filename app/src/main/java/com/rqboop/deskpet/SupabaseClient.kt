package com.rqboop.deskpet

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private const val SUPABASE_URL = "https://dwkvmrrphzieccccqzbl.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_M9BYDb3I8yHSxM_pCBUjlg_b8rTe-Nb"
    private const val PET_ID = "rq-boop-pet"

    fun fetchState(): JSONObject? {
        try {
            val url = URL("$SUPABASE_URL/rest/v1/pet_state?pet_id=eq.$PET_ID&limit=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", SUPABASE_KEY)
            conn.setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val arr = org.json.JSONArray(body)
            return if (arr.length() > 0) arr.getJSONObject(0) else null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "fetchState failed: ${e.message}")
            return null
        }
    }

    fun pushEvent(eventType: String, payload: String) {
        try {
            val url = URL("$SUPABASE_URL/rest/v1/pet_events")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", SUPABASE_KEY)
            conn.setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
            conn.setRequestProperty("Prefer", "return=minimal")
            val body = JSONObject().apply {
                put("pet_id", PET_ID)
                put("event_type", eventType)
                put("payload", JSONObject(payload))
                put("client_id", "android-overlay-1")
            }.toString()
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray()) }
            android.util.Log.d(TAG, "pushEvent resp=${conn.responseCode} $eventType")
            conn.disconnect()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "pushEvent failed: ${e.message}")
        }
    }
}