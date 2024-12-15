package com.qasimzaka.lab4_ex1

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.qasimzaka.lab4_ex1.screens.MapScreen
import com.qasimzaka.lab4_ex1.ui.theme.Qasimzaka_COMP304Lab4_Ex1Theme
import com.qasimzaka.lab4_ex1.workers.SyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var geofencingClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestLocationPermission() // Request location permissions

        // Initialize Geofencing Client
        geofencingClient = LocationServices.getGeofencingClient(this)

        // Set up the content
        setContent {
            Qasimzaka_COMP304Lab4_Ex1Theme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Navigation Host
                    NavHost(
                        navController = navController,
                        startDestination = "map" // Set map screen as the default
                    ) {
                        composable("map") {
                            MapScreen(modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }

        // Set up geofencing
        setupGeofencing()

        // Schedule a background task using WorkManager
        setupWorkManager()
    }

    /**
     * Request Location Permission
     */
    private fun requestLocationPermission() {
        val locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("MainActivity", "Location permission granted.")
            } else {
                Log.d("MainActivity", "Location permission denied.")
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Setup WorkManager to schedule background tasks
     */
    private fun setupWorkManager() {
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES // Run every 15 minutes
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }

    /**
     * Setup Geofencing
     */
    private fun setupGeofencing() {
        val geofence = Geofence.Builder()
            .setRequestId("MyGeofence") // Unique ID for the geofence
            .setCircularRegion(
                -34.0, 151.0, // Example latitude and longitude
                100f // Radius in meters
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val geofencePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d("MainActivity", "Geofence added successfully.")
                }
                .addOnFailureListener {
                    Log.e("MainActivity", "Failed to add geofence", it)
                }
        } else {
            Log.e("MainActivity", "Location permission not granted for geofencing.")
        }
    }
}
