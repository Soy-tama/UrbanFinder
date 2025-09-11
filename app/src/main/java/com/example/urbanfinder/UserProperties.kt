package com.example.urbanfinder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProperties(
    navController: NavController,
    sessionViewModel: SessionViewModel,
    propertyViewModel: PropertyViewModel
) {
    val sessionState by sessionViewModel.sessionState.collectAsState()
    val propertiesState by propertyViewModel.properties.collectAsState()
    val deleteResult by propertyViewModel.deleteResult.collectAsState()
    val isDeleting by propertyViewModel.isDeleting.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val userProperties = remember(propertiesState, sessionState.userId) {
        propertiesState.filter { it.postedBy == sessionState.userId }
    }

    // Fetch properties when screen loads
    LaunchedEffect(sessionState.userId) {
        if (sessionState.isAuthenticated && sessionState.userId != null) {
            propertyViewModel.fetchProperties()
        }
    }

    // Handle delete results
    LaunchedEffect(deleteResult) {
        when (deleteResult) {
            is PropertyViewModel.DeleteResult.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Property deleted successfully",
                    duration = SnackbarDuration.Short
                )
                propertyViewModel._deleteResult.value = null // Reset the state
            }
            is PropertyViewModel.DeleteResult.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Error: ${(deleteResult as PropertyViewModel.DeleteResult.Error).message}",
                    duration = SnackbarDuration.Long
                )
                propertyViewModel._deleteResult.value = null // Reset the state
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Properties",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { propertyViewModel.fetchProperties() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (sessionState.isAuthenticated) {
                FloatingActionButton(
                    onClick = { navController.navigate(ScreenC) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Property")
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(navController)
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isDeleting -> LoadingContent()
                !sessionState.isAuthenticated -> NotLoggedInContent(navController)
                userProperties.isEmpty() -> EmptyPropertiesContent(navController)
                else -> UserPropertiesContent(
                    navController = navController,
                    properties = userProperties,
                    propertyViewModel = propertyViewModel
                )
            }
        }
    }
}

@Composable
private fun NotLoggedInContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sign in to view your properties",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { navController.navigate(LoginScreen) },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Sign In")
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyPropertiesContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No properties posted yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Properties you post will appear here",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun UserPropertiesContent(
    navController: NavController,
    properties: List<Property>,
    propertyViewModel: PropertyViewModel
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(properties) { property ->
            UserPropertyCard(
                property = property,
                navController = navController,
                onStatusChange = { newStatus ->
                    propertyViewModel.updatePropertyStatus(property.id, newStatus)
                },
                propertyViewModel = propertyViewModel
            )
        }
    }
}

@Composable
private fun UserPropertyCard(
    property: Property,
    navController: NavController,
    onStatusChange: (String) -> Unit,
    propertyViewModel: PropertyViewModel
) {
    var currentStatus by remember { mutableStateOf(property.status) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Property") },
            text = { Text("Are you sure you want to delete this property? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        propertyViewModel.deleteProperty(
                            propertyId = property.id,
                            photoUrls = property.photoUrls
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate(ScreenB(property.id))
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Property image
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (property.photoUrls.isNotEmpty()) {
                        AsyncImage(
                            model = property.photoUrls[0],
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "No image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Property details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = property.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rs. ${property.price}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = property.address,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    // Status and Approval chips
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Status chip
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when (currentStatus.lowercase()) {
                                        "available" -> Color.Green.copy(alpha = 0.2f)
                                        "sold" -> Color.Red.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentStatus.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (currentStatus.lowercase()) {
                                    "available" -> Color.Black
                                    "sold" -> Color.Red
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        // Approval chip
                        if (property.approval.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = when (property.approval.lowercase()) {
                                            "approved" -> Color.Green.copy(alpha = 0.2f)
                                            "pending" -> Color.Yellow.copy(alpha = 0.2f)
                                            "rejected" -> Color.Red.copy(alpha = 0.2f)
                                            else -> MaterialTheme.colorScheme.surface
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = property.approval.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (property.approval.lowercase()) {
                                        "approved" -> Color.Black
                                        "pending" -> Color(0xFFF57F17)
                                        "rejected" -> Color.Red
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }

                    // Rejection reason
                    if (property.approval.equals("rejected", ignoreCase = true) && property.rejectionReason.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reason: ${property.rejectionReason}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                }

                // Status toggle switch
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Switch(
                        checked = currentStatus.equals("Available", ignoreCase = true),
                        onCheckedChange = { isAvailable ->
                            val newStatus = if (isAvailable) "Available" else "Sold"
                            currentStatus = newStatus
                            onStatusChange(newStatus)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Blue,
                            checkedTrackColor = Color.Blue.copy(alpha = 0.2f),
                            uncheckedThumbColor = Color.Red,
                            uncheckedTrackColor = Color.Red.copy(alpha = 0.2f)
                        )
                    )
                    Text(
                        text = if (currentStatus.equals("Available", ignoreCase = true)) "Available" else "Sold",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Delete button
            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC94444)) // Dark red
            ) {
                Text("Delete Property")
            }
        }
    }
}