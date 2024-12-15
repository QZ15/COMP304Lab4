package com.qasimzaka.lab4_ex1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Safely get the GeofencingEvent from the intent
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        // Check if the geofencingEvent is null or has errors
        if (geofencingEvent == null) {
            Log.e("GeofenceBroadcastReceiver", "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceBroadcastReceiver", "Geofence error: ${geofencingEvent.errorCode}")
            return
        }

        // Handle geofence transition
        when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d("GeofenceBroadcastReceiver", "Entered geofence")
                // Optional: Add logic to notify the user
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d("GeofenceBroadcastReceiver", "Exited geofence")
                // Optional: Add logic to notify the user
            }
            else -> {
                Log.d("GeofenceBroadcastReceiver", "Unknown geofence transition")
            }
        }
    }
}
