package com.qasimzaka.lab4_ex1.screens

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.rotate
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

@Composable
fun MapScreen(modifier: Modifier) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var zoomLevel by remember { mutableStateOf(10f) } // Default zoom level
    val markers = remember { mutableStateListOf<Marker>() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Handle MapView lifecycle
    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onResume()

        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Display the MapView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { view ->
                view.getMapAsync { googleMap ->
                    setupMap(
                        googleMap,
                        context,
                        markers,
                        zoomLevel,
                        onZoomChange = { zoomLevel = it },
                        fusedLocationClient = fusedLocationClient
                    )
                }
            }
        )

        // Search Bar for location
        var searchText by remember { mutableStateOf(TextFieldValue("")) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
        ) {
            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box(Modifier.padding(8.dp)) {
                        if (searchText.text.isEmpty()) {
                            Text(
                                text = "Search location...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            Button(
                onClick = {
                    searchLocation(searchText.text, mapView, context)
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Search")
            }
        }

        // Vertical Zoom Slider and Current Location Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 32.dp)
                .width(120.dp), // Fixed width for alignment
            horizontalAlignment = Alignment.CenterHorizontally // Align items centrally
        ) {
            Slider(
                value = zoomLevel,
                onValueChange = { zoomLevel = it },
                valueRange = 5f..20f,
                modifier = Modifier
                    .height(150.dp)
                    .rotate(90f) // Rotate the slider to make it vertical
            )
            Spacer(modifier = Modifier.height(8.dp))
            FloatingActionButton(
                onClick = {
                    recenterToCurrentLocation(mapView, fusedLocationClient, context)
                },
                modifier = Modifier
                    .size(48.dp) // Match button size to slider width
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Current Location"
                )
            }
        }
    }
}

private fun setupMap(
    map: GoogleMap,
    context: Context,
    markers: MutableList<Marker>,
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    fusedLocationClient: FusedLocationProviderClient
) {
    // Enable location layer for the blue dot
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        map.isMyLocationEnabled = true
    }

    // Add marker on long-press
    map.setOnMapLongClickListener { latLng ->
        showMarkerDialog(context) { markerName ->
            val marker = map.addMarker(
                MarkerOptions().position(latLng).title(markerName)
            )
            markers.add(marker!!)
            marker?.showInfoWindow()
            Toast.makeText(context, "Marker added: $markerName", Toast.LENGTH_SHORT).show()
        }
    }

    // Show info window and option to remove marker
    map.setOnMarkerClickListener { marker ->
        AlertDialog.Builder(context)
            .setTitle(marker.title)
            .setMessage("Do you want to remove this marker?")
            .setPositiveButton("Yes") { _, _ ->
                markers.remove(marker)
                marker.remove()
                Toast.makeText(context, "Marker removed: ${marker.title}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
        true
    }

    // Update zoom level dynamically
    map.setOnCameraMoveListener {
        onZoomChange(map.cameraPosition.zoom)
    }

    // Adjust map zoom when slider changes
    map.setOnCameraIdleListener {
        map.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel))
    }
}

private fun showMarkerDialog(context: Context, onNameSelected: (String) -> Unit) {
    val input = EditText(context)
    AlertDialog.Builder(context)
        .setTitle("Enter Marker Name")
        .setView(input)
        .setPositiveButton("OK") { _, _ ->
            val name = input.text.toString().takeIf { it.isNotBlank() } ?: "Unnamed Marker"
            onNameSelected(name)
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

private fun searchLocation(query: String, mapView: MapView, context: Context) {
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val addresses: List<Address> = geocoder.getFromLocationName(query, 1) ?: return
        if (addresses.isNotEmpty()) {
            val location = addresses[0]
            val latLng = LatLng(location.latitude, location.longitude)
            mapView.getMapAsync { googleMap ->
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
            }
        } else {
            Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error searching location", Toast.LENGTH_SHORT).show()
    }
}

private fun recenterToCurrentLocation(
    mapView: MapView,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                mapView.getMapAsync { googleMap ->
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
            } else {
                Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        Toast.makeText(context, "Location permission is required to recenter the map.", Toast.LENGTH_SHORT).show()
    }
}
