package com.example.urbanfinder
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.draw.clip
import org.osmdroid.util.GeoPoint
import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import org.osmdroid.views.overlay.Overlay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Filter(navController: NavController,sessionViewModel: SessionViewModel,propertyViewModel: PropertyViewModel = viewModel(), sessionState: SessionState) {
    var selectedType by remember { mutableStateOf("Both") }
    var commercialOnly by remember { mutableStateOf(false) }
    var selectedPropertyType by remember { mutableStateOf("Any") }
    var selectedPriceFrom by remember { mutableStateOf("1") }
    var selectedPriceTo by remember { mutableStateOf("1000000000") }
    var selectedBedrooms by remember { mutableStateOf("Any") }
    var selectedBathrooms by remember { mutableStateOf("Any") }
    var selectedFurnishing by remember { mutableStateOf("Any") }
    var selectedAreaFrom by remember { mutableStateOf("1") }
    var selectedAreaTo by remember { mutableStateOf("600000") }
    var selectedLocation by remember { mutableStateOf(GeoPoint(27.6708, 85.4164)) }
    var isExpanded by remember { mutableStateOf(true) }
    val context = LocalContext.current
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                FilterSection(title = "Property Type") {
                    ToggleButtonGroup(
                        options = listOf("Any", "House", "Land", "Apartment", "Rent"),
                        selectedOption = selectedPropertyType,
                        onOptionSelected = { selectedPropertyType = it }
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth(),
                    color = Color.Gray
                )
            }

            item {
                FilterSection(title = "Price Range") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DropdownSelector(
                            selectedOption = selectedPriceFrom,
                            options = listOf("1", "2", "5"),
                            onOptionSelected = { selectedPriceFrom = it }
                        )
                        Text("  To  ")
                        DropdownSelector(
                            selectedOption = selectedPriceTo,
                            options = listOf("100000000", "200000000", "1000000000"),
                            onOptionSelected = { selectedPriceTo = it }
                        )
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth(),
                    color = Color.Gray
                )
            }

            item {
                FilterSection(title = "Bedrooms") {
                    ToggleButtonGroup(
                        options = listOf("Any", "1", "2", "3", "4", "5"),
                        selectedOption = selectedBedrooms,
                        onOptionSelected = { selectedBedrooms = it }
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth(),
                    color = Color.Gray
                )
            }

            item {
                FilterSection(title = "Bathrooms") {
                    ToggleButtonGroup(
                        options = listOf("Any", "1", "2", "3", "4", "5"),
                        selectedOption = selectedBathrooms,
                        onOptionSelected = { selectedBathrooms = it }
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth(),
                    color = Color.Gray
                )
            }

            item {
                FilterSection(title = "Furnishings") {
                    ToggleButtonGroup(
                        options = listOf("Any", "Not Furnished", "Semi-Furnished", "Fully Furnished"),
                        selectedOption = selectedFurnishing,
                        onOptionSelected = { selectedFurnishing = it }
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth(),
                    color = Color.Gray
                )
            }

            item {
                FilterSection(title = "Area Range") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DropdownSelector(
                            selectedOption = selectedAreaFrom,
                            options = listOf(
                                "1",
                                "2",
                                "3",
                                "4",
                                "5",
                                "6"
                            ),
                            onOptionSelected = { selectedAreaFrom = it }
                        )
                        Text("  To  ")
                        DropdownSelector(
                            selectedOption = selectedAreaTo,
                            options = listOf(
                                "100000",
                                "200000",
                                "300000",
                                "400000",
                                "500000",
                                "600000"
                            ),
                            onOptionSelected = { selectedAreaTo = it }
                        )
                    }
                }
            }

            if(sessionState.isAuthenticated && sessionState.userId != null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Location selection section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Preferred location",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Map"
                            )
                        }

                        if (isExpanded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                SelectableLocationMap(
                                    initialLocation = selectedLocation,
                                    onLocationSelected = { newLocation ->
                                        selectedLocation = newLocation
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    propertyViewModel.getFilteredPropertie(
                                        selectedBedrooms = selectedBedrooms,
                                        selectedFurnishing = selectedFurnishing,
                                        selectedBathrooms = selectedBathrooms,
                                        selectedAreaFrom = selectedAreaFrom,
                                        selectedAreaTo = selectedAreaTo,
                                        selectedPriceFrom = selectedPriceFrom,
                                        selectedPriceTo = selectedPriceTo,
                                        selectedPropertyType = selectedPropertyType,
                                        location = selectedLocation
                                    ) { success, message ->
                                        if (success) {
                                            Log.d("FILTER", "Navigation to result screen")
                                            navController.navigate(results) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "No properties found matching your criteria",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            Log.e("FILTER", "Search failed: $message")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF009688) // Teal color
                                )
                            ) {
                                Text("SEARCH PROPERTIES", color = Color.White, fontSize = 16.sp)
                            }

                            Button(
                                onClick = {
                                    selectedType = "Both"
                                    commercialOnly = false
                                    selectedPropertyType = "Any"
                                    selectedPriceFrom = "1"
                                    selectedPriceTo = "1000000000"
                                    selectedBedrooms = "Any"
                                    selectedBathrooms = "Any"
                                    selectedFurnishing = "Any"
                                    selectedAreaTo = "1"
                                    selectedAreaFrom = "600000"
                                    selectedLocation = GeoPoint(27.6756, 85.3459)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("CLEAR", color = Color.White, fontSize = 16.sp)
                            }


                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    propertyViewModel.saveparameters(
                                        selectedBedrooms = selectedBedrooms,
                                        selectedFurnishing = selectedFurnishing,
                                        selectedBathrooms = selectedBathrooms,
                                        selectedAreaFrom = selectedAreaFrom,
                                        selectedAreaTo = selectedAreaTo,
                                        selectedPriceFrom = selectedPriceFrom,
                                        selectedPriceTo = selectedPriceTo,
                                        selectedPropertyType = selectedPropertyType,
                                        location = selectedLocation
                                    ) { success, message ->
                                        Toast.makeText(
                                            context,
                                            if (success) "Preferences saved successfully!" else "Failed to save preferences: $message",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF009688)
                                )
                            ) {
                                Text("SAVE PREFERENCES", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun SelectableLocationMap(
    initialLocation: GeoPoint,
    onLocationSelected: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    var mapView: MapView? by remember { mutableStateOf(null) }
    var marker: Marker? by remember { mutableStateOf(null) }
    var address by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
    ) {
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Search location") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        val point = geocodeLocation(context, "$address, Nepal")
                        point?.let {
                            mapView?.controller?.setCenter(it)
                            marker?.position = it
                            mapView?.post { mapView?.invalidate() }
                            onLocationSelected(it)
                        }
                        isLoading = false
                    }
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(300.dp)
                .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        controller.setCenter(initialLocation)
                        controller.setZoom(15.0)
                        setMultiTouchControls(true)

                        marker = Marker(this).apply {
                            position = initialLocation
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        overlays.add(marker)

                        overlays.add(object : Overlay() {
                            override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                                e?.let { event ->
                                    mapView?.let { mv ->
                                        val newLocation = GeoPoint(
                                            mv.projection.fromPixels(event.x.toInt(), event.y.toInt()).latitude,
                                            mv.projection.fromPixels(event.x.toInt(), event.y.toInt()).longitude
                                        )
                                        marker?.position = newLocation
                                        mv.invalidate()
                                        onLocationSelected(newLocation)
                                    }
                                }
                                return true
                            }
                        })
                    }
                },
                update = { view ->
                    mapView = view
                    marker?.position = initialLocation
                    view.controller.setCenter(initialLocation)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun GeoPoint(x0: Double, x1: Double) {
    TODO("Not yet implemented")
}

@Composable
fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))
        content()
        Spacer(modifier = Modifier.height(18.dp))
    }
}
@Composable
fun ToggleButtonGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        items(options) { option ->
            Button(
                onClick = { onOptionSelected(option) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (option == selectedOption) MaterialTheme.colorScheme.primary else Color.LightGray,
                    contentColor = if (option == selectedOption) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(option, fontSize = 14.sp)
            }
        }
    }
}
@Composable
fun DropdownSelector(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .background(Color.LightGray, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp)
            .wrapContentHeight()
    ) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(selectedOption, fontSize = 14.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
