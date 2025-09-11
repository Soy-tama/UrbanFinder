package com.example.urbanfinder

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Preferences(
    navController: NavController,
    propertyViewModel: PropertyViewModel,
    sessionState: SessionState,
    sessionViewModel: SessionViewModel
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var matchedProperties by remember { mutableStateOf<List<Property>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch matched properties when screen loads
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                isLoading = true
                val triggers = FirebaseFirestore.getInstance()
                    .collection("trigger")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                if (triggers.isEmpty) {
                    errorMessage = "No property preferences found"
                    isLoading = false
                    return@LaunchedEffect
                }

                // Get all matched property IDs from all triggers
                val allMatchedIds = triggers.documents.flatMap { doc ->
                    (doc.get("matchedPropertyIds") as? List<*> ?: emptyList())
                }.distinct()

                if (allMatchedIds.isEmpty()) {
                    errorMessage = "No matching properties found for your preferences"
                    isLoading = false
                    return@LaunchedEffect
                }

                // Fetch all matched properties
                val properties = FirebaseFirestore.getInstance()
                    .collection("properties")
                    .whereIn("id", allMatchedIds)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
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

                matchedProperties = properties
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Error loading preferences: ${e.message}"
                isLoading = false
            }
        } else {
            errorMessage = "Please sign in to view your preferences"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Property Preferences") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> CenterLoading()
                errorMessage != null -> ErrorMessage(errorMessage!!)
                matchedProperties.isEmpty() -> EmptyPreferences()
                else -> MatchedPropertiesList(
                    properties = matchedProperties,
                    navController = navController,
                    propertyViewModel = propertyViewModel
                )
            }
        }
    }
}

@Composable
private fun CenterLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun EmptyPreferences() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No matching properties found for your preferences",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We'll notify you when new properties match your criteria",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
private fun MatchedPropertiesList(
    properties: List<Property>,
    navController: NavController,
    propertyViewModel: PropertyViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(properties) { property ->
            PropertyCard(
                property = property,
                onClick = { navController.navigate(ScreenB(property.id)) },
                onBookmarkClick = {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        propertyViewModel.toggleBookmark(property.id, userId)
                    }
                },
                isBookmarked = propertyViewModel.bookmarkedProperties.value.contains(property.id)
            )
        }
    }
}

@Composable
private fun PropertyCard(
    property: Property,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    isBookmarked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Property image (first image if available)
            if (property.photoUrls.isNotEmpty()) {
                AsyncImage(
                    model = property.photoUrls[0],
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Property details
            Text(
                text = property.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = property.address,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Rs. ${property.price}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Property features
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${property.beds} Beds")
                Text("${property.bathrooms} Baths")
                Text("${property.area} sq. ft.")
            }

            // Bookmark button
            IconButton(
                onClick = { onBookmarkClick() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Bookmark"
                )
            }
        }
    }
}