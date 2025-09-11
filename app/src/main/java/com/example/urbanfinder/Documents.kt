package com.example.urbanfinder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    navController: NavController,
    sessionState: SessionState,
    propertyViewModel: PropertyViewModel
) {
    val userData by propertyViewModel.userData.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Keep track of fullscreen image
    var fullscreenImageUri by remember { mutableStateOf<Uri?>(null) }

    // Editable fields
    var mobileNumber by remember { mutableStateOf(userData?.mobileNumber ?: "") }
    var bankDetails by remember { mutableStateOf(userData?.bankDetails ?: "") }

    // Image URIs
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var nidImageUri by remember { mutableStateOf<Uri?>(null) }
    var landDocImageUri by remember { mutableStateOf<Uri?>(null) }
    var completionDocImageUri by remember { mutableStateOf<Uri?>(null) }

    // Temporary URIs for new uploads
    var tempProfileUri by remember { mutableStateOf<Uri?>(null) }
    var tempNidUri by remember { mutableStateOf<Uri?>(null) }
    var tempLandDocUri by remember { mutableStateOf<Uri?>(null) }
    var tempCompletionDocUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for image selection
    val profileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { tempProfileUri = it }
    }

    val nidLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { tempNidUri = it }
    }

    val landDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { tempLandDocUri = it }
    }

    val completionDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { tempCompletionDocUri = it }
    }

    LaunchedEffect(userData) {
        userData?.let { it ->
            mobileNumber = it.mobileNumber ?: ""
            bankDetails = it.bankDetails ?: ""
            profileImageUri = it.profilePic?.let { Uri.parse(it) }
            nidImageUri = it.nationalId?.let { Uri.parse(it) }
            landDocImageUri = it.landDocument?.let { Uri.parse(it) }
            completionDocImageUri = it.completionDocument?.let { Uri.parse(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Documents") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Personal Information Section
            DocumentSection(
                title = "Personal Information",
                icon = Icons.Default.Person,
                isEditing = isEditing,
                fields = listOf(
                    DocumentField("Mobile Number", mobileNumber) { mobileNumber = it }
                ),
                imageFields = listOf {
                    if (isEditing) {
                        ImageUploadField(
                            label = "Profile Picture",
                            currentImageUri = profileImageUri,
                            tempImageUri = tempProfileUri,
                            onSelectImage = { profileLauncher.launch("image/*") },
                            onRemoveImage = { tempProfileUri = null },
                            isProfilePicture = true
                        )
                    } else {
                        ImageDisplayField(
                            label = "Profile Picture",
                            imageUri = profileImageUri,
                            isProfilePicture = true,
                            onImageClick = { uri -> fullscreenImageUri = uri }
                        )
                    }
                }
            )

            //Property Documents
            DocumentSection(
                title = "Property Documents * ",
                icon = Icons.Default.Home,
                isEditing = isEditing,
                fields = emptyList(),
                imageFields = listOf(
                    {
                        if (isEditing) {
                            ImageUploadField(
                                label = "National ID",
                                currentImageUri = nidImageUri,
                                tempImageUri = tempNidUri,
                                onSelectImage = { nidLauncher.launch("image/*") },
                                onRemoveImage = { tempNidUri = null },
                                isNationalID = true
                            )
                        } else {
                            ImageDisplayField(
                                label = "National ID",
                                imageUri = nidImageUri,
                                onImageClick = { uri -> fullscreenImageUri = uri }
                            )
                        }
                    },
                    {
                        if (isEditing) {
                            ImageUploadField(
                                label = "Land Document",
                                currentImageUri = landDocImageUri,
                                tempImageUri = tempLandDocUri,
                                onSelectImage = { landDocLauncher.launch("image/*") },
                                onRemoveImage = { tempLandDocUri = null }
                            )
                        } else {
                            ImageDisplayField(
                                label = "Land Document",
                                imageUri = landDocImageUri,
                                onImageClick = { uri -> fullscreenImageUri = uri }
                            )
                        }
                    },
                    {
                        if (isEditing) {
                            ImageUploadField(
                                label = "Completion Document",
                                currentImageUri = completionDocImageUri,
                                tempImageUri = tempCompletionDocUri,
                                onSelectImage = { completionDocLauncher.launch("image/*") },
                                onRemoveImage = { tempCompletionDocUri = null }
                            )
                        } else {
                            ImageDisplayField(
                                label = "Completion Document",
                                imageUri = completionDocImageUri,
                                onImageClick = { uri -> fullscreenImageUri = uri }
                            )
                        }
                    }
                )
            )

            // Financial Information
            DocumentSection(
                title = "Bank Details",
                icon = Icons.Default.AccountCircle,
                isEditing = isEditing,
                fields = listOf(
                    DocumentField("Bank Account Details", bankDetails) { bankDetails = it }
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isEditing = false
                            tempProfileUri = null
                            tempNidUri = null
                            tempLandDocUri = null
                            tempCompletionDocUri = null
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            isLoading = true // Set loading to true before starting the coroutine
                            scope.launch {
                                try {
                                    // Handle document uploads with cleanup of old files
                                    if (tempProfileUri != null || tempNidUri != null || tempLandDocUri != null || tempCompletionDocUri != null) {
                                        propertyViewModel.uploadDocuments(
                                            userId = sessionState.userId!!,
                                            profilePic= tempProfileUri,
                                            nidUri = tempNidUri,
                                            landDocUri = tempLandDocUri,
                                            completionDocUri = tempCompletionDocUri,
                                            // Pass current URIs for deletion
                                            oldProfileUrl = if (tempProfileUri != null) userData?.profilePic else null,
                                            oldNidUrl = if (tempNidUri != null) userData?.nationalId else null,
                                            oldLandUrl = if (tempLandDocUri != null) userData?.landDocument else null,
                                            oldCompletionUrl = if (tempCompletionDocUri != null) userData?.completionDocument else null,
                                            onSuccess = { profileUrl, nidUrl, landUrl, completionUrl ->
                                                propertyViewModel.updateUserDocuments(
                                                    userId = sessionState.userId,
                                                    mobileNumber = mobileNumber,
                                                    profilePic = profileUrl ?: userData?.profilePic,
                                                    nationalId = nidUrl ?: userData?.nationalId,
                                                    landDocument = landUrl ?: userData?.landDocument,
                                                    completionDocument = (completionUrl ?: userData?.completionDocument).toString(),
                                                    bankDetails = bankDetails
                                                )
                                                dialogMessage = "Documents updated successfully!"
                                                showDialog = true
                                                isLoading = false
                                            },
                                            onError = { message ->
                                                dialogMessage = message ?: "Failed to upload documents"
                                                showDialog = true
                                                isLoading = false
                                            }
                                        )
                                    } else {
                                        propertyViewModel.updateUserDocuments(
                                            userId = sessionState.userId!!,
                                            mobileNumber = mobileNumber,
                                            profilePic = userData?.profilePic,
                                            nationalId = userData?.nationalId,
                                            landDocument = userData?.landDocument,
                                            completionDocument = userData?.completionDocument,
                                            bankDetails = bankDetails
                                        )
                                        dialogMessage = "Documents updated successfully!"
                                        showDialog = true
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    dialogMessage = e.message ?: "An error occurred"
                                    showDialog = true
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                    ) {
                        if (isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving...", color = Color.White)
                            }
                        } else {
                            Text("Save Changes")
                        }
                    }
                }
            } else {
                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                ) {
                    Text("Edit Documents")
                }
            }
        }
    }

    // Display status dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                if (dialogMessage.contains("success", ignoreCase = true)) {
                    isEditing = false
                    navController.popBackStack()
                }
            },
            title = { Text("Status") },
            text = { Text(dialogMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        if (dialogMessage.contains("success", ignoreCase = true)) {
                            isEditing = false
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Fullscreen image dialog
    if (fullscreenImageUri != null) {
        Dialog(
            onDismissRequest = { fullscreenImageUri = null },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = fullscreenImageUri,
                    contentDescription = "Full screen image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Error loading image", color = Color.White)
                            }
                        }
                        else -> {
                            SubcomposeAsyncImageContent()
                        }
                    }
                }

                // Close button
                IconButton(
                    onClick = { fullscreenImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentSection(
    title: String,
    icon: ImageVector,
    isEditing: Boolean,
    fields: List<DocumentField> = emptyList(),
    imageFields: List<@Composable () -> Unit> = emptyList()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF009688))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF009688)
                )
            }

            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)

            fields.forEach { field ->
                Column {
                    Text(
                        text = field.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isEditing) {
                        OutlinedTextField(
                            value = field.value,
                            onValueChange = field.onValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = field.label != "Bank Account Details",
                            maxLines = if (field.label == "Bank Account Details") 3 else 1
                        )
                    } else {
                        Text(
                            text = field.value.ifEmpty { "Not provided" },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // Render each image field
            imageFields.forEach { imageField ->
                imageField()
            }
        }
    }
}

@Composable
fun ImageUploadField(
    label: String,
    currentImageUri: Uri?,
    tempImageUri: Uri?,
    onSelectImage: () -> Unit,
    onRemoveImage: () -> Unit,
    isProfilePicture: Boolean = false,
    isNationalID: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Determine image height based on document type
        val imageHeight = when {
            isProfilePicture -> 150.dp
            isNationalID -> 120.dp
            else -> 200.dp
        }

        val imageShape = if (isProfilePicture) CircleShape else RoundedCornerShape(8.dp)

        // Show current image or placeholder
        if (currentImageUri != null && tempImageUri == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
            ) {
                SubcomposeAsyncImage(
                    model = currentImageUri,
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(imageShape),
                    contentScale = if (isProfilePicture) ContentScale.Crop else ContentScale.FillWidth
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Error loading image")
                            }
                        }
                        else -> {
                            SubcomposeAsyncImageContent()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Show temporary selected image
        tempImageUri?.let { uri ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
            ) {
                SubcomposeAsyncImage(
                    model = uri,
                    contentDescription = "Selected $label",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(imageShape),
                    contentScale = if (isProfilePicture) ContentScale.Crop else ContentScale.FillWidth
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Error loading image")
                            }
                        }
                        else -> {
                            SubcomposeAsyncImageContent()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSelectImage,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.DarkGray
                )
            ) {
                Text("Select ${if (currentImageUri != null || tempImageUri != null) "New" else ""} $label")
            }

            if (currentImageUri != null || tempImageUri != null) {
                Button(
                    onClick = onRemoveImage,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
fun ImageDisplayField(
    label: String,
    imageUri: Uri?,
    isProfilePicture: Boolean = false,
    onImageClick: (Uri) -> Unit
) {
    Column (
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Define common image shape
        val imageShape = if (isProfilePicture) CircleShape else RoundedCornerShape(8.dp)

        if (imageUri != null) {
            SubcomposeAsyncImage(
                model = imageUri,
                contentDescription = label,
                modifier = if (isProfilePicture) {
                    Modifier
                        .size(200.dp)
                        .clip(imageShape)
                        .clickable { onImageClick(imageUri) }
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(imageShape)
                        .clickable { onImageClick(imageUri) }
                },
                contentScale = if (isProfilePicture) ContentScale.Crop else ContentScale.FillWidth
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Error loading image")
                        }
                    }
                    else -> {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
        } else {
            Box(
                modifier = if (isProfilePicture) {
                    Modifier
                        .size(200.dp)
                        .background(Color.LightGray.copy(alpha = 0.2f), shape = imageShape)
                        .border(1.dp, Color.Gray, shape = imageShape)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray.copy(alpha = 0.2f), shape = imageShape)
                        .border(1.dp, Color.Gray, shape = imageShape)
                },
                contentAlignment = Alignment.Center
            ) {
                Text("No $label uploaded", color = Color.Gray)
            }
        }
    }
}


data class DocumentField(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit
)