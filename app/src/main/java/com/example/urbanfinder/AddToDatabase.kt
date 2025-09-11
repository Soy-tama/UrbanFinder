package com.example.urbanfinder

import android.annotation.SuppressLint
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import java.util.UUID
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import java.util.Date
import org.osmdroid.config.Configuration
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint
import okhttp3.Request
import java.util.Locale

@SuppressLint("AutoboxingStateCreation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToDatabase(navController: NavController, sessionState: SessionState, sessionViewModel: SessionViewModel) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var isNegotiable by remember { mutableStateOf(false) }
    var bedrooms by remember { mutableIntStateOf(0) }
    var kitchens by remember { mutableIntStateOf(0) }
    var livingRooms by remember { mutableIntStateOf(0) }
    var bathrooms by remember { mutableIntStateOf(0) }
    var ownerName by remember { mutableStateOf("") }
    var ownerContact1 by remember { mutableStateOf("") }
    var ownerContact2 by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var furnishings by remember { mutableStateOf("") }
    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedAddress by remember { mutableStateOf("") }
    var selectedLatitude by remember { mutableDoubleStateOf(0.0) }
    var selectedLongitude by remember { mutableStateOf(0.0) }

    var isLoading by remember { mutableStateOf(false) }

    var isForRent by remember { mutableStateOf(true) } // true=Rent, false=Buy

    // Property type dropdown state
    var propertyType by remember { mutableStateOf("") }
    val propertyTypes = listOf("House", "Apartment", "Land", "Commercial", "Others")
    var isPropertyTypeExpanded by remember { mutableStateOf(false) }

    // Furnishings dropdown state
    val furnishingsOptions = listOf("Fully Furnished", "Semi Furnished", "Unfurnished")
    var isFurnishingsExpanded by remember { mutableStateOf(false) }

    //Dialog Box
    var showStatusDialog by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        photoUris = uris
    }
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Add New Property",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Basic Information Section
                Text("Basic Information", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Property Title Field (takes 70% width)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Property Title*") },
                        modifier = Modifier.weight(7f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Buy/Rent Toggle
                    Column(
                        modifier = Modifier.weight(3f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isForRent) "For Rent" else "For Buy",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Switch(
                            checked = isForRent,
                            onCheckedChange = { isForRent = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.surfaceBright,
                                uncheckedThumbColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }

                //Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price* (NRs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    //Area
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("Area (sq ft)*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Property Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = isPropertyTypeExpanded,
                    onExpandedChange = { isPropertyTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = propertyType,
                        onValueChange = {},
                        label = { Text("Property Type*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPropertyTypeExpanded)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isPropertyTypeExpanded,
                        onDismissRequest = { isPropertyTypeExpanded = false }
                    ) {
                        propertyTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    propertyType = type
                                    isPropertyTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Furnishings Dropdown
                ExposedDropdownMenuBox(
                    expanded = isFurnishingsExpanded,
                    onExpandedChange = { isFurnishingsExpanded = it }
                ) {
                    OutlinedTextField(
                        value = furnishings,
                        onValueChange = {},
                        label = { Text("Furnishings*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFurnishingsExpanded)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isFurnishingsExpanded,
                        onDismissRequest = { isFurnishingsExpanded = false }
                    ) {
                        furnishingsOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    furnishings = option
                                    isFurnishingsExpanded = false
                                }
                            )
                        }
                    }
                }

                // Room Details Section
                Text(
                    text = "Room Details",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactRoomCounter(
                        label = "Bedrooms",
                        count = bedrooms,
                        onCountChange = { bedrooms = it },
                        modifier = Modifier.weight(1f)
                    )

                    CompactRoomCounter(
                        label = "Kitchens",
                        count = kitchens,
                        onCountChange = { kitchens = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactRoomCounter(
                        label = "Living Rooms",
                        count = livingRooms,
                        onCountChange = { livingRooms = it },
                        modifier = Modifier.weight(1f)
                    )

                    CompactRoomCounter(
                        label = "Bathrooms",
                        count = bathrooms,
                        onCountChange = { bathrooms = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Location Section
                Text("Location Details", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))

                Button(
                    onClick = { showLocationDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAddress.isEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    if (selectedAddress.isEmpty()) {
                        Text("Select Location on Map*")
                    } else {
                        Text("Location Selected ✓")
                    }
                }

                if (selectedAddress.isNotEmpty()) {
                    Text(
                        text = selectedAddress,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                        maxLines = 2
                    )
                }

                // Owner Information Section
                Text("Owner Information", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = ownerContact1,
                    onValueChange = { newValue ->
                        if (newValue.length <= 10 && newValue.all { it.isDigit() }) {
                            ownerContact1 = newValue
                        }
                    },
                    label = { Text("Primary Contact*") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = ownerContact1.isNotEmpty() && !isValidPhoneNumber(ownerContact1),
                    supportingText = {
                        if (ownerContact1.isNotEmpty() && !isValidPhoneNumber(ownerContact1)) {
                            Text("Must be exactly 10 digits")
                        }
                    }
                )

                OutlinedTextField(
                    value = ownerContact2,
                    onValueChange = { newValue ->
                        if (newValue.length <= 10 && newValue.all { it.isDigit() }) {
                            ownerContact2 = newValue
                        }
                    },
                    label = { Text("Secondary Contact") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = ownerContact2.isNotEmpty() && !isValidPhoneNumber(ownerContact2),
                    supportingText = {
                        if (ownerContact2.isNotEmpty() && !isValidPhoneNumber(ownerContact2)) {
                            Text("Must be exactly 10 digits")
                        }
                    }
                )

                // Photos Section
                Text("Photos (Minimum 2)*", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))

                Button(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Select Photos")
                }

                if (photoUris.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(photoUris.size) { index ->
                            AsyncImage(
                                model = photoUris[index],
                                contentDescription = "Property photo $index",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Text("${photoUris.size} photos selected", style = MaterialTheme.typography.bodySmall)
                }

                // Description Section
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Submit Button
                Button(
                    onClick = {
                        if (validateInputs(
                                title,
                                price,
                                area,
                                propertyType,
                                furnishings,
                                ownerContact1,
                                ownerContact2,
                                photoUris
                            )) {
                            isLoading = true
                            statusMessage = "Uploading property details..."

                            val propertyData: HashMap<String, Any> = hashMapOf(
                                "title" to title,
                                "forRent" to isForRent,
                                "price" to price.toDouble(),
                                "area" to area,
                                "isNegotiable" to isNegotiable,
                                "bedrooms" to bedrooms,
                                "kitchens" to kitchens,
                                "livingRooms" to livingRooms,
                                "bathrooms" to bathrooms,
                                "ownerName" to ownerName,
                                "ownerContact1" to ownerContact1,
                                "ownerContact2" to ownerContact2,
                                "propertyType" to propertyType,
                                "furnishings" to furnishings,
                                "description" to message,
                                "address" to selectedAddress,
                                "latitude" to selectedLatitude,
                                "longitude" to selectedLongitude,
                                "postedBy" to (sessionState.userId ?: ""),
                                "postedAt" to Date(),
                                "status" to "Available",
                                "approval" to "pending"
                            )

                            uploadPhotosAndSaveProperty(
                                propertyData = propertyData,
                                photoUris = photoUris,
                                onSuccess = {
                                    isLoading = false
                                    isSuccess = true
                                    statusMessage = "Property added successfully!"
                                    showStatusDialog = true
                                },
                                onError = { error ->
                                    isLoading = false
                                    isSuccess = false
                                    statusMessage = "Error: ${error ?: "Unknown error"}"
                                    showStatusDialog = true
                                }
                            )
                        } else {
                            isSuccess = false
                            statusMessage = when {
                                !isValidPhoneNumber(ownerContact1) -> "Primary contact must be 10 digits"
                                photoUris.size < 3 -> "Please upload at least 3 photos"
                                title.isEmpty() -> "Please enter a property title"
                                else -> "Please fill all required fields"
                            }
                            showStatusDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Submit Property")
                    }
                }
            }
        }
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = {
                Text(
                    if (isSuccess) "Success" else "Error",
                    color = if (isSuccess) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            },
            text = { Text(statusMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showStatusDialog = false
                        if (isSuccess) {
                            // Clear all form fields
                            title = ""
                            price = ""
                            area = ""
                            isNegotiable = false
                            bedrooms = 0
                            kitchens = 0
                            livingRooms = 0
                            bathrooms = 0
                            ownerName = ""
                            ownerContact1 = ""
                            ownerContact2 = ""
                            propertyType = ""
                            furnishings = ""
                            selectedAddress = ""
                            photoUris = emptyList()
                            selectedLatitude = 0.0
                            selectedLongitude = 0.0
                            message = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showLocationDialog) {
        LocationDialog(
            onDismiss = { showLocationDialog = false },
            onLocationSelected = { address, lat, lng ->
                selectedAddress = address
                selectedLatitude = lat
                selectedLongitude = lng
            }
        )
    }
}

@Composable
fun CompactRoomCounter(
    label: String,
    count: Int,
    onCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { if (count > 0) onCountChange(count - 1) },
                    modifier = Modifier.size(24.dp),
                    enabled = count > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { if (count < 20) onCountChange(count + 1) },
                    modifier = Modifier.size(24.dp),
                    enabled = count < 20
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun isValidPhoneNumber(phone: String): Boolean {
    return phone.length == 10 && phone.all { it.isDigit() }
}

fun validateInputs(
    title: String,
    price: String,
    area: String,
    propertyType: String,
    furnishings: String,
    ownerContact1: String,
    ownerContact2: String,
    photoUris: List<Uri>
): Boolean {
    return title.isNotEmpty() &&
            price.isNotEmpty() && price.toDoubleOrNull() != null &&
            area.isNotEmpty() &&
            propertyType.isNotEmpty() &&
            furnishings.isNotEmpty() &&
            isValidPhoneNumber(ownerContact1) &&
            isValidPhoneNumber(ownerContact2) &&
            photoUris.size >= 2
}

suspend fun uploadPhotosToFirebaseStorage(photoUris: List<Uri>): List<String> {
    val storage = FirebaseStorage.getInstance()
    val uploadedPhotoUrls = mutableListOf<String>()

    photoUris.forEach { uri ->
        val photoRef = storage.reference.child("property_photos/${UUID.randomUUID()}.jpg")
        try {
            // First upload the file data from the URI
            val uploadTask = photoRef.putFile(uri).await()
            // Then get the download URL
            val downloadUrl = photoRef.downloadUrl.await().toString()
            uploadedPhotoUrls.add(downloadUrl)
        } catch (e: Exception) {
            throw Exception("Photo upload failed: ${e.message}")
        }
    }
    return uploadedPhotoUrls
}

fun uploadPhotosAndSaveProperty(
    propertyData: HashMap<String, Any>,
    photoUris: List<Uri>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    MainScope().launch {
        try {
            val uploadedPhotoUrls = if (photoUris.isNotEmpty()) {
                uploadPhotosToFirebaseStorage(photoUris)
            } else {
                emptyList()
            }
            val db = FirebaseFirestore.getInstance()
            val propertyId = db.collection("properties").document().id
            val finalPropertyData = propertyData.toMutableMap().apply {
                put("id", propertyId)
                if (uploadedPhotoUrls.isNotEmpty()) {
                    put("photoUrls", uploadedPhotoUrls)
                }
            }
            db.collection("properties")
                .document(propertyId)
                .set(finalPropertyData)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Unknown error")
                }
        } catch (e: Exception) {
            onError(e.message ?: "Photo upload failed")
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun LocationDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var selectedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var address by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).height(500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
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
                                    selectedGeoPoint = it
                                    mapView?.post { mapView?.invalidate() }
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

                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            controller.setZoom(15.0)
                            controller.setCenter(GeoPoint(27.7172, 85.3240))
                            setMultiTouchControls(true)

                            marker = Marker(this).apply {
                                position = GeoPoint(27.7172, 85.3240)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                overlays.add(this)
                            }

                            setOnTouchListener { _, event ->
                                if (event.action == MotionEvent.ACTION_UP) {
                                    val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as? GeoPoint
                                    geoPoint?.let {
                                        selectedGeoPoint = it
                                        marker?.position = it
                                        post { invalidate() }

                                        coroutineScope.launch {
                                            isLoading = true
                                            reverseGeocode(context, it.latitude, it.longitude) { result ->
                                                address = result.ifEmpty { "Unknown Location" }
                                            }
                                            isLoading = false
                                        }
                                    }
                                }
                                false
                            }
                            mapView = this
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(12.dp))
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            selectedGeoPoint?.let {
                                onLocationSelected(address, it.latitude, it.longitude)
                                onDismiss()
                            }
                        },
                        enabled = selectedGeoPoint != null
                    ) { Text("Confirm Location") }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onPause()
            mapView?.onDetach()
        }
    }
}

fun reverseGeocode(context: Context, lat: Double, lon: Double, onAddressReceived: (String) -> Unit) {
    val geocoder = Geocoder(context, Locale.getDefault())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                if (addresses.isNotEmpty()) {
                    val address = addresses[0].getAddressLine(0) ?: "Unknown location"
                    onAddressReceived(address)
                } else {
                    onAddressReceived("No address found")
                }
            }

            override fun onError(errorMessage: String?) {
                onAddressReceived("Geocoder error: $errorMessage")
            }
        })
    } else {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0].getAddressLine(0) ?: "Unknown location"
                        onAddressReceived(address)
                    } else {
                        onAddressReceived("No address found")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onAddressReceived("Failed to get address")
                }
            }
        }
    }
}

suspend fun geocodeLocation(context: Context, query: String): GeoPoint? = withContext(Dispatchers.IO) {
    try {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery"

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", context.packageName)
                    .build()
                chain.proceed(request)
            }
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        val jsonString = response.body?.string()

        if (!jsonString.isNullOrEmpty()) {
            val jsonArray = org.json.JSONArray(jsonString)
            if (jsonArray.length() > 0) {
                val firstResult = jsonArray.getJSONObject(0)
                GeoPoint(
                    firstResult.getDouble("lat"),
                    firstResult.getDouble("lon")
                )
            } else null
        } else null
    } catch (e: Exception) {
        null
    }
}