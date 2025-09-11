package com.example.urbanfinder

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.URLEncoder

@SuppressLint("ClickableViewAccessibility")
@Composable
fun LocationMap(
    startPoint: GeoPoint,
    endPoint: GeoPoint
) {
    val context = LocalContext.current
    var mapView: MapView? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Map Container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
            .height(300.dp)
            .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.apply {
                        setZoom(14.0)
                        setCenter(startPoint)
                    }
                    setMultiTouchControls(true)

                    // Key modification: Implement custom scroll handling
                    setOnTouchListener { v, _ ->
                        // Request parent to disallow intercepting touch events
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        false // Return false to allow the map to handle its own touch events
                    }
                }
            },
            update = { view ->
                mapView = view

                // Add markers for start and end points
                view.overlays.clear()
                Marker(view).apply {
                    position = startPoint
                    title = "Start"
                    view.overlays.add(this)
                }
                Marker(view).apply {
                    position = endPoint
                    title = "End"
                    view.overlays.add(this)
                }

                // Fetch and draw the route
                coroutineScope.launch {
                    val route = fetchRoute(startPoint, endPoint)
                    route?.let { points ->
                        val routeLine = Polyline().apply {
                            setPoints(points)
                            paint.color = Color.Blue.toArgb()
                            paint.strokeWidth = 5f
                        }
                        view.overlays.add(routeLine)
                        view.invalidate()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
suspend fun fetchRoute(start: GeoPoint, end: GeoPoint): List<GeoPoint>? =
    withContext(Dispatchers.IO) {
        try {
            val url = "https://routing.openstreetmap.de/routed-car/route/v1/driving/" +
                    "${end.longitude},${end.latitude};" +
                    "${start.longitude},${start.latitude}?" +
                    "overview=full&geometries=geojson"

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val jsonString = response.body?.string()

            if (jsonString.isNullOrEmpty()) return@withContext null

            val jsonObject = JSONObject(jsonString)
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")

                (0 until coordinates.length()).map { i ->
                    val coord = coordinates.getJSONArray(i)
                    GeoPoint(coord.getDouble(1), coord.getDouble(0))
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

suspend fun geocodeLocation(locationName: String): GeoPoint? =
    withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(locationName, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery"

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "YourAppName")
                .build()

            val response = client.newCall(request).execute()
            val jsonString = response.body?.string()

            if (jsonString.isNullOrEmpty()) return@withContext null

            val jsonArray = JSONArray(jsonString)
            if (jsonArray.length() > 0) {
                val location = jsonArray.getJSONObject(0)
                val lat = location.getDouble("lat")
                val lon = location.getDouble("lon")
                GeoPoint(lat, lon)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }