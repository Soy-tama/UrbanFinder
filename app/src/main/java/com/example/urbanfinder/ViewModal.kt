package com.example.urbanfinder

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint
import java.util.Date
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


/*-------------------------------------------Model Class for Property--------------------------------------------------------*/
data class Property(
    val id: String,
    val title: String = "",
    val price: Int = 0,
    val forRent: Boolean = true,
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val propertyType: String = "",
    val status: String = "",
    val description: String = "",
    val area: String = "",
    val beds: Int = 0,
    val bathrooms: Int = 0,
    val livingRooms: Int = 0,
    val kitchens: Int = 0,
    val furnishings: String = "",
    val isNegotiable: Boolean = false,
    val ownerName: String = "",
    val ownerContact1: String = "",
    val ownerContact2: String = "",
    val postedBy: String = "",
    val postedAt: Date = Date(),
    val photoUrls: List<String> = emptyList(),
    val approval: String = "",
    val approvedAt: Date = Date(),
    val rejectedAt: Date = Date(),
    val rejectionReason: String = ""
)

/*-------------------------------------------Model Class for User--------------------------------------------------------*/
data class User(
    val userId: String = "",
    val fullName: String? = null,
    val email: String? = null,
    val profilePic: String? = null,
    val mobileNumber: String? = null,
    val nationalId: String? = null,
    val landDocument: String? = null,
    val completionDocument: String? = null,
    val bankDetails: String? = null
)

class PropertyViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _properties = MutableStateFlow<List<Property>>(emptyList())
    val properties: StateFlow<List<Property>> get() = _properties

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData

    private val bookmarksCollection = firestore.collection("bookmarks")
    private val _bookmarkedProperties = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedProperties: StateFlow<Set<String>> = _bookmarkedProperties.asStateFlow()

    private val _filteredProperties = MutableStateFlow<List<Property>>(emptyList())
    val filteredProperties: StateFlow<List<Property>> = _filteredProperties

    init {
        fetchProperties()
    }


    /*---------------------------------------Fetch all the data of Properties---------------------------------------------*/
    fun fetchProperties() {
        viewModelScope.launch {
            try {
                firestore.collection("properties")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val propertyList = snapshot.documents.mapNotNull { doc ->
                            Property(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                price = doc.getLong("price")?.toInt() ?: 0,
                                forRent = doc.getBoolean("forRent") ?: true,
                                address = doc.getString("address") ?: "",
                                latitude = doc.getDouble("latitude") ?: 0.0,
                                longitude = doc.getDouble("longitude") ?: 0.0,
                                propertyType = doc.getString("propertyType") ?: "",
                                status = doc.getString("status") ?: "",
                                description = doc.getString("description") ?: "",
                                area = doc.getString("area") ?: "",
                                beds = doc.getLong("bedrooms")?.toInt() ?: 0,
                                bathrooms = doc.getLong("bathrooms")?.toInt() ?: 0,
                                livingRooms = doc.getLong("livingRooms")?.toInt() ?: 0,
                                kitchens = doc.getLong("kitchens")?.toInt() ?: 0,
                                furnishings = doc.getString("furnishings") ?: "",
                                isNegotiable = doc.getBoolean("isNegotiable") ?: false,
                                ownerName = doc.getString("ownerName") ?: "",
                                ownerContact1 = doc.getString("ownerContact1") ?: "",
                                ownerContact2 = doc.getString("ownerContact2") ?: "",
                                postedBy = doc.getString("postedBy") ?: "",
                                postedAt = doc.getTimestamp("postedAt")?.toDate() ?: Date(),
                                photoUrls = (doc.get("photoUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                approval = doc.getString("approval") ?: "",
                                approvedAt = doc.getTimestamp("approvedAt")?.toDate() ?: Date(),
                                rejectedAt = doc.getTimestamp("rejectedAt")?.toDate() ?: Date(),
                                rejectionReason = doc.getString("rejectionReason") ?: ""
                            )
                        }
                        _properties.value = propertyList
                    }
            } catch (e: Exception) {
                Log.e("PropertyViewModel", "Error fetching properties", e)
                _properties.value = emptyList()
            }
        }
    }

    /*------------------------------------Fetch Specific Parameters of Properties for Filtering---------------------------*/
    fun filterPropertiesByQuery(query: String) {
        _properties.value = _properties.value.filter { property ->
            property.title.contains(query, ignoreCase = true) ||
                    property.address.contains(query, ignoreCase = true)
        }
    }

    fun getNearbyProperties(
        location: GeoPoint,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Constants for distance calculation (100 km radius)
                val radiusInKm = 9.0
                val earthRadius = 6371.0 // Earth's radius in km

                firestore.collection("properties")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val nearbyProperties = snapshot.documents.mapNotNull { doc ->
                            try {
                                val property = Property(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    price = doc.getLong("price")?.toInt() ?: 0,
                                    forRent = doc.getBoolean("forRent") ?: true,
                                    address = doc.getString("address") ?: "",
                                    latitude = doc.getDouble("latitude") ?: 0.0,
                                    longitude = doc.getDouble("longitude") ?: 0.0,
                                    // Include all other property fields as in your existing code
                                    propertyType = doc.getString("propertyType") ?: "",
                                    status = doc.getString("status") ?: "",
                                    description = doc.getString("description") ?: "",
                                    area = doc.getString("area") ?: "",
                                    beds = doc.getLong("bedrooms")?.toInt() ?: 0,
                                    bathrooms = doc.getLong("bathrooms")?.toInt() ?: 0,
                                    livingRooms = doc.getLong("livingRooms")?.toInt() ?: 0,
                                    kitchens = doc.getLong("kitchens")?.toInt() ?: 0,
                                    furnishings = doc.getString("furnishings") ?: "",
                                    isNegotiable = doc.getBoolean("isNegotiable") ?: false,
                                    ownerName = doc.getString("ownerName") ?: "",
                                    ownerContact1 = doc.getString("ownerContact1") ?: "",
                                    ownerContact2 = doc.getString("ownerContact2") ?: "",
                                    postedBy = doc.getString("postedBy") ?: "",
                                    postedAt = doc.getTimestamp("postedAt")?.toDate() ?: Date(),
                                    photoUrls = (doc.get("photoUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                )

                                // Calculate distance between property and selected location
                                val lat1 = Math.toRadians(location.latitude)
                                val lon1 = Math.toRadians(location.longitude)
                                val lat2 = Math.toRadians(property.latitude)
                                val lon2 = Math.toRadians(property.longitude)

                                val dLat = lat2 - lat1
                                val dLon = lon2 - lon1

                                val a = sin(dLat / 2).pow(2) +
                                        cos(lat1) * cos(lat2) *
                                        sin(dLon / 2).pow(2)
                                val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                                val distance = earthRadius * c

                                // Only include properties within 100 km radius
                                if (distance <= radiusInKm) property else null
                            } catch (e: Exception) {
                                Log.e("NearbyProperties", "Error mapping property", e)
                                null
                            }
                        }

                        // Store the filtered properties
                        _filteredProperties.value = nearbyProperties
                        callback(true, "Found ${nearbyProperties.size} properties nearby")
                    }
                    .addOnFailureListener { e ->
                        Log.e("NearbyProperties", "Firestore query failed: ${e.message}")
                        callback(false, "Failed to fetch properties: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("NearbyProperties", "Exception: ${e.message}")
                callback(false, "Error: ${e.message}")
            }
        }
    }

    fun saveparameters(
        selectedBathrooms: String,
        selectedBedrooms: String,
        selectedFurnishing: String,
        selectedAreaFrom: String,
        selectedAreaTo: String,
        selectedPriceFrom: String,
        selectedPriceTo: String,
        selectedPropertyType: String,
        location: GeoPoint,
        callback: (Boolean, String) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            callback(false, "User not logged in")
            return
        }

        val triggerData = mapOf(
            "userId" to userId,
            "bathrooms" to selectedBathrooms,
            "bedrooms" to selectedBedrooms,
            "furnishing" to selectedFurnishing,
            "areaFrom" to selectedAreaFrom,
            "areaTo" to selectedAreaTo,
            "priceFrom" to selectedPriceFrom,
            "priceTo" to selectedPriceTo,
            "propertyType" to selectedPropertyType,
            "location" to location,
            "timestamp" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("trigger")
            .add(triggerData) // This creates a new document with a random ID
            .addOnSuccessListener {
                callback(true, "Preferences saved successfully")
            }
            .addOnFailureListener { e ->
                callback(false, "Failed to save preferences: ${e.message}")
            }
    }



    /*------------------------------------------------Return Filtered Parameters------------------------------------------*/
    fun getFilteredProperties(
        selectedBathrooms: String,
        selectedBedrooms: String,
        selectedFurnishing: String,
        selectedAreaFrom: String,
        selectedAreaTo: String,
        selectedPriceFrom: String,
        selectedPriceTo: String,
        selectedPropertyType: String,
        callback: (Boolean, String, List<Property>?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val priceFrom = selectedPriceFrom.toDoubleOrNull() ?: 0.0
                val priceTo = selectedPriceTo.toDoubleOrNull() ?: Double.MAX_VALUE
                val areaFrom = selectedAreaFrom.toDoubleOrNull() ?: 0.0
                val areaTo = selectedAreaTo.toDoubleOrNull() ?: Double.MAX_VALUE

                firestore.collection("properties")
                    .whereGreaterThanOrEqualTo("price", priceFrom)
                    .whereLessThanOrEqualTo("price", priceTo)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val propertyList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val property = Property(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    price = doc.getLong("price")?.toInt() ?: 0,
                                    forRent = doc.getBoolean("forRent") ?: true,
                                    address = doc.getString("address") ?: "",
                                    latitude = doc.getDouble("latitude") ?: 0.0,
                                    longitude = doc.getDouble("longitude") ?: 0.0,
                                    propertyType = doc.getString("propertyType") ?: "",
                                    status = doc.getString("status") ?: "",
                                    description = doc.getString("description") ?: "",
                                    area = doc.getString("area") ?: "",
                                    beds = doc.getLong("bedrooms")?.toInt() ?: 0,
                                    bathrooms = doc.getLong("bathrooms")?.toInt() ?: 0,
                                    livingRooms = doc.getLong("livingRooms")?.toInt() ?: 0,
                                    kitchens = doc.getLong("kitchens")?.toInt() ?: 0,
                                    furnishings = doc.getString("furnishings") ?: "",
                                    isNegotiable = doc.getBoolean("isNegotiable") ?: false,
                                    ownerName = doc.getString("ownerName") ?: "",
                                    ownerContact1 = doc.getString("ownerContact1") ?: "",
                                    ownerContact2 = doc.getString("ownerContact2") ?: "",
                                    postedBy = doc.getString("postedBy") ?: "",
                                    postedAt = doc.getTimestamp("postedAt")?.toDate() ?: Date(),
                                    photoUrls = (doc.get("photoUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                )

                                // Apply all filters
                                val areaValue = property.area.toDoubleOrNull() ?: 0.0
                                val passesFilters =
                                    (selectedPropertyType == "Any" || property.propertyType == selectedPropertyType.trim()) &&
                                            (selectedFurnishing == "Any" || property.furnishings == selectedFurnishing.trim()) &&
                                            (selectedBedrooms == "Any" || property.beds == selectedBedrooms.toIntOrNull()) &&
                                            (selectedBathrooms == "Any" || property.bathrooms == selectedBathrooms.toIntOrNull()) &&
                                            (areaValue in areaFrom..areaTo)

                                if (passesFilters) property else null
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (propertyList.isEmpty()) {
                            _filteredProperties.value = emptyList()
                            callback(false, "No properties match your filters", null)
                        } else {
                            _filteredProperties.value = propertyList
                            callback(true, "Found ${propertyList.size} matching properties", propertyList)
                        }
                    }
                    .addOnFailureListener { e ->
                        _filteredProperties.value = emptyList()
                        callback(false, "Search failed: ${e.message ?: "Unknown error"}", null)
                    }
            } catch (e: Exception) {
                _filteredProperties.value = emptyList()
                callback(false, "Error: ${e.message ?: "Unknown error"}", null)
            }
        }
    }

    fun getFilteredPropertie(
        selectedBathrooms: String,
        selectedBedrooms: String,
        selectedFurnishing: String,
        selectedAreaFrom: String,
        selectedAreaTo: String,
        selectedPriceFrom: String,
        selectedPriceTo: String,
        selectedPropertyType: String,
        location: GeoPoint? = null,
        callback: (Boolean, List<Property>?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val priceFrom = selectedPriceFrom.toDoubleOrNull() ?: 0.0
                val priceTo = selectedPriceTo.toDoubleOrNull() ?: Double.MAX_VALUE
                val areaFrom = selectedAreaFrom.toDoubleOrNull() ?: 0.0
                val areaTo = selectedAreaTo.toDoubleOrNull() ?: Double.MAX_VALUE

                val radiusInKm = 1.0
                val earthRadius = 6371.0

                firestore.collection("properties")
                    .whereGreaterThanOrEqualTo("price", priceFrom)
                    .whereLessThanOrEqualTo("price", priceTo)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val propertiesWithDistance = snapshot.documents.mapNotNull { doc ->
                            try {
                                val property = Property(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    price = doc.getLong("price")?.toInt() ?: 0,
                                    forRent = doc.getBoolean("forRent") ?: true,
                                    address = doc.getString("address") ?: "",
                                    latitude = doc.getDouble("latitude") ?: 0.0,
                                    longitude = doc.getDouble("longitude") ?: 0.0,
                                    propertyType = doc.getString("propertyType") ?: "",
                                    status = doc.getString("status") ?: "",
                                    description = doc.getString("description") ?: "",
                                    area = doc.getString("area") ?: "",
                                    beds = doc.getLong("bedrooms")?.toInt() ?: 0,
                                    bathrooms = doc.getLong("bathrooms")?.toInt() ?: 0,
                                    livingRooms = doc.getLong("livingRooms")?.toInt() ?: 0,
                                    kitchens = doc.getLong("kitchens")?.toInt() ?: 0,
                                    furnishings = doc.getString("furnishings") ?: "",
                                    isNegotiable = doc.getBoolean("isNegotiable") ?: false,
                                    ownerName = doc.getString("ownerName") ?: "",
                                    ownerContact1 = doc.getString("ownerContact1") ?: "",
                                    ownerContact2 = doc.getString("ownerContact2") ?: "",
                                    postedBy = doc.getString("postedBy") ?: "",
                                    postedAt = doc.getTimestamp("postedAt")?.toDate() ?: Date(),
                                    photoUrls = (doc.get("photoUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                )

                                val areaValue = property.area.toDoubleOrNull() ?: 0.0
                                val passesBasicFilters =
                                    (selectedPropertyType == "Any" || property.propertyType == selectedPropertyType.trim()) &&
                                            (selectedFurnishing == "Any" || property.furnishings == selectedFurnishing.trim()) &&
                                            (selectedBedrooms == "Any" || property.beds == selectedBedrooms.toIntOrNull()) &&
                                            (selectedBathrooms == "Any" || property.bathrooms == selectedBathrooms.toIntOrNull()) &&
                                            (areaValue in areaFrom..areaTo)

                                if (!passesBasicFilters) return@mapNotNull null

                                if (location != null) {
                                    val lat1 = Math.toRadians(location.latitude)
                                    val lon1 = Math.toRadians(location.longitude)
                                    val lat2 = Math.toRadians(property.latitude)
                                    val lon2 = Math.toRadians(property.longitude)

                                    val dLat = lat2 - lat1
                                    val dLon = lon2 - lon1

                                    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
                                    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                                    val distance = earthRadius * c

                                    if (distance > radiusInKm) return@mapNotNull null
                                }

                                property
                            } catch (e: Exception) {
                                Log.e("PropertyFilter", "Error mapping property", e)
                                null
                            }
                        }

                        _filteredProperties.value = propertiesWithDistance
                        callback(true, propertiesWithDistance)
                    }
                    .addOnFailureListener { e ->
                        Log.e("PropertyFilter", "Firestore query failed: ${e.message}")
                        callback(false, null)
                    }
            } catch (e: Exception) {
                Log.e("PropertyFilter", "Exception: ${e.message}")
                callback(false, null)
            }
        }
      }

    // Add this data class at the top of your file
    data class PropertyWithDistance(
        val property: Property,
        val distance: Double?
    )

    // Haversine distance calculation
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat/2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon/2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1-a))
        return earthRadius * c
    }


    /*-------------------------------------------Fetch User Data--------------------------------------------------------*/
    fun fetchUserData(userId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val user = User(
                                userId = document.getString("userId") ?: "",
                                fullName = document.getString("fullName") ?: "",
                                email = document.getString("email") ?: "",
                                mobileNumber = document.getString("mobileNumber") ?: "",
                                profilePic = document.getString("profilePictureUrl") ?: "",
                                nationalId = document.getString("nationalId") ?: "",
                                landDocument = document.getString("landDocument") ?: "",
                                completionDocument = document.getString("completionDocument") ?: "",
                                bankDetails = document.getString("bankDetails") ?: ""
                            )
                            _userData.value = user
                        } else {
                            Log.e("UserViewModel", "User not found")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("UserViewModel", "Error fetching user", exception)
                    }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error in fetchUserData", e)
            }
        }
    }

    /*----------------------------Upload Documents like NID/Profile into Storage-----------------------------------------*/
    fun uploadDocuments(
        userId: String,
        profilePic: Uri?,
        nidUri: Uri?,
        landDocUri: Uri?,
        completionDocUri: Uri?,
        oldProfileUrl: String?,
        oldNidUrl: String?,
        oldLandUrl: String?,
        oldCompletionUrl: String?,
        onSuccess: (profileUrl: String?, nidUrl: String?, landUrl: String?, completionUrl: String?) -> Unit,
        onError: (message: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val deleteTasks = mutableListOf<Deferred<Unit>>()

                // Delete old images
                oldProfileUrl?.let {
                    deleteTasks.add(async { deleteFileFromStorage(it) })
                }
                oldNidUrl?.let {
                    deleteTasks.add(async { deleteFileFromStorage(it) })
                }
                oldLandUrl?.let {
                    deleteTasks.add(async { deleteFileFromStorage(it) })
                }
                oldCompletionUrl?.let {
                    deleteTasks.add(async { deleteFileFromStorage(it) })
                }

                deleteTasks.awaitAll()

                // Upload new images
                val profileUrl = profilePic?.let { uploadImageToStorage(userId, "profile", it) }
                val nidUrl = nidUri?.let { uploadImageToStorage(userId, "nid", it) }
                val landUrl = landDocUri?.let { uploadImageToStorage(userId, "land", it) }
                val completionUrl = completionDocUri?.let { uploadImageToStorage(userId, "completion", it) }

                onSuccess(profileUrl, nidUrl, landUrl, completionUrl)
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }

    /*----------------------------------Delete Photos from the Storage--------------------------------------------------------*/
    private suspend fun deleteFileFromStorage(fileUrl: String) {
        val storageRef = Firebase.storage.getReferenceFromUrl(fileUrl)
        storageRef.delete().await()
    }

    /*--------------------------------Delete Property from Firestore and Storage----------------------------------------------*/
    sealed class DeleteResult {
        data object Success : DeleteResult()
        data class Error(val message: String) : DeleteResult()
    }

    val _deleteResult = MutableStateFlow<DeleteResult?>(null)
    val deleteResult: StateFlow<DeleteResult?> = _deleteResult.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    fun deleteProperty(propertyId: String, photoUrls: List<String>) {
        viewModelScope.launch {
            _isDeleting.value = true
            try {
                // First delete all images from storage
                photoUrls.forEach { url ->
                    try {
                        deleteFileFromStorage(url)
                    } catch (e: Exception) {
                        Log.e("DeleteProperty", "Error deleting image $url: ${e.message}")
                        // Continue with deletion even if some images fail
                    }
                }

                // Then delete the property document
                firestore.collection("properties").document(propertyId).delete().await()

                // Delete all bookmarks referencing this property
                val bookmarkQuery = bookmarksCollection
                    .whereEqualTo("propertyId", propertyId)
                    .get()
                    .await()

                // Create a list of delete tasks
                val deleteTasks = bookmarkQuery.documents.map { doc ->
                    async {
                        bookmarksCollection.document(doc.id).delete().await()
                    }
                }

                // Wait for all bookmark deletions to complete
                deleteTasks.awaitAll()

                // Also remove from local bookmarked properties if present
                _bookmarkedProperties.value -= propertyId

                // Refresh properties
                fetchProperties()
                _deleteResult.value = DeleteResult.Success
            } catch (e: Exception) {
                Log.e("DeleteProperty", "Error deleting property", e)
                _deleteResult.value = DeleteResult.Error(e.message ?: "Failed to delete property")
            } finally {
                _isDeleting.value = false
            }
        }
    }

    /*-------------------------------------------Update Status of Property----------------------------------------------------*/
    fun updatePropertyStatus(propertyId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                firestore.collection("properties").document(propertyId)
                    .update("status", newStatus)
                    .await()
                fetchProperties() // Refresh the list
            } catch (e: Exception) {
                Log.e("PropertyViewModel", "Error updating status", e)
            }
        }
    }

    /*-------------------------------------------Upload Images to Storage--------------------------------------------------------*/
    private suspend fun uploadImageToStorage(userId: String, docType: String, uri: Uri): String {
        val storageRef = Firebase.storage.reference
        val fileRef = storageRef.child("documents/$userId/$docType-${System.currentTimeMillis()}.jpg")

        return fileRef.putFile(uri)
            .await()
            .storage
            .downloadUrl
            .await()
            .toString()
    }

    /*-------------------------------------------Update Documents--------------------------------------------------------*/
    fun updateUserDocuments(
        userId: String,
        mobileNumber: String,
        nationalId: String?,
        landDocument: String?,
        completionDocument: String?,
        bankDetails: String,
        profilePic: String?
    ) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId).update(
                    mapOf(
                        "mobileNumber" to mobileNumber,
                        "nationalId" to nationalId,
                        "landDocument" to landDocument,
                        "completionDocument" to completionDocument,
                        "bankDetails" to bankDetails,
                        "profilePictureUrl" to profilePic
                    )
                )
                fetchUserData(userId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /*-------------------------------------------Self Explanatory--------------------------------------------------------*/
    fun isPropertyBookmarked(propertyId: String): StateFlow<Boolean> =
        bookmarkedProperties.map { it.contains(propertyId) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                false
            )

    /*-------------------------------------------Self Explanatory--------------------------------------------------------*/
    fun fetchUserBookmarks(userId: String) {
        viewModelScope.launch {
            bookmarksCollection
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("PropertyViewModel", "Error fetching bookmarks", e)
                        return@addSnapshotListener
                    }

                    val bookmarkedIds =
                        snapshot?.documents?.mapNotNull { it.getString("propertyId") }
                        ?.toSet() ?: emptySet()
                    _bookmarkedProperties.value = bookmarkedIds
                }
        }
    }

    /*-------------------------------------------Self Explanatory--------------------------------------------------------*/
    fun toggleBookmark(propertyId: String, userId: String) {
        viewModelScope.launch {
            try {
                // First, query to check if bookmark exists
                val query = bookmarksCollection
                    .whereEqualTo("propertyId", propertyId)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                if (query.isEmpty) {
                    // Not bookmarked - add it
                    val bookmark = hashMapOf(
                        "propertyId" to propertyId,
                        "userId" to userId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                    bookmarksCollection.add(bookmark)
                        .addOnSuccessListener {
                            // Update local state
                            _bookmarkedProperties.value += propertyId
                            Log.d("PropertyViewModel", "Bookmark added successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("PropertyViewModel", "Error adding bookmark", e)
                        }
                } else {
                    // Already bookmarked - remove it
                    query.documents.forEach { doc ->
                        bookmarksCollection.document(doc.id).delete()
                            .addOnSuccessListener {
                                // Update local state
                                _bookmarkedProperties.value -= propertyId
                                Log.d("PropertyViewModel", "Bookmark removed successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e("PropertyViewModel", "Error removing bookmark", e)
                            }
                    }
                }
            } catch (e: Exception) {
                Log.e("PropertyViewModel", "Error toggling bookmark", e)
            }
        }
    }
}

/*-------------------------------------------Splash Screen UI and Other--------------------------------------------------*/

@Composable
fun SplashScreenApp() {
    var isLoading by remember { mutableStateOf(true) }

    // Creating the SessionViewModel instance using the Factory
    val sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory)

    LaunchedEffect(Unit) {
        delay(2000)
        isLoading = false
    }

    if (isLoading) {
        SplashScreen()
    } else {
        // Providing the SessionViewModel to the CompositionLocalProvider
        CompositionLocalProvider(LocalSessionManager provides sessionViewModel) {
            MyAppNavigation(
                propertyViewModel = viewModel()
            )
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.homepage_icon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "UrbanFinder",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF69B4), // Neon Pink
                            Color(0xFF4C4CFF), // Neon Blue
                            Color(0xFF00FF00)  // Neon Green
                        )
                    ),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
