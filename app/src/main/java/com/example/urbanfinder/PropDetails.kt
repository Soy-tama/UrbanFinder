package com.example.urbanfinder

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import org.osmdroid.util.GeoPoint


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PropertyDetailsScreen(
    navController: NavController,
    id: String,
    propertyViewModel: PropertyViewModel,
    sessionState: SessionState,
    sessionViewModel: SessionViewModel
) {
    val properties by propertyViewModel.properties.collectAsState()
    //Find the received id details
    val property = properties.find { it.id == id }

    var isExpanded by remember { mutableStateOf(true) }

    // State for pull-to-refresh
    val refreshing = remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing.value,
        onRefresh = {
            refreshing.value = true
            propertyViewModel.fetchProperties()
            refreshing.value = false
        }
    )

    if (property != null) {
        Scaffold(
            bottomBar = {
                ContactActionBar(
                    phoneNumber = property.ownerContact1,
                    emailAddress = "contact@example.com", // Replace with actual email if available
                    whatsappNumber = property.ownerContact2,
                    onMessageClick = { /* Handle message click */ }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pullRefresh(pullRefreshState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Property Images Carousel
                    PropertyImages(property.photoUrls)

                    // Pricing and Details
                    PricingDetails(
                        price = property.price,
                        neg = property.isNegotiable,
                        title = property.title,
                        address=property.address,
                        beds = property.beds,
                        kitchen = property.kitchens,
                        living = property.livingRooms,
                        bathroom = property.bathrooms,
                        propertyType=property.propertyType,
                        hasVideo = property.id == "hsJiUknPGQDxAFDSGOhQ",
                        sessionState=sessionState,
                        navController=navController
                    )

                    // Divider
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth(),
                        color = Color.LightGray.copy(alpha = 0.5f)
                    )

                    // Property Owner Section
                    PropertyOwner(
                        owner = property.ownerName,
                        contact1 = property.ownerContact1,
                        contact2 = property.ownerContact2,
                        area = property.area,
                        sessionState = sessionState
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    if (sessionState.isAuthenticated && sessionState.userId != null) {
                        //Map
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Header with title and toggle button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isExpanded = !isExpanded } // Toggle visibility
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Location Map",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Map"
                                    )
                                }

                                // Expandable Map Section
                                if (isExpanded) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        LocationMap(
                                            GeoPoint(property.latitude, property.longitude),
                                            GeoPoint(
                                                27.6708,
                                                85.4164
                                            )  // Replace with current user location
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Expandable Property Details
                    ExpandablePropertyDetails(property.description)
                }

                // Pull-to-refresh indicator
                PullRefreshIndicator(
                    refreshing = refreshing.value,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    } else {
        // Show loading or error message if the property is not found
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Loading property details...",
                style = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, color = Color.Gray)
            )
        }
    }
}

//Sliding Images Section
@Composable
fun PropertyImages(imageUrls: List<String>) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Horizontal pager to display images
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = imageUrls[page],
                contentDescription = "Property Image ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.error_image), // Add a placeholder drawable
                error = painterResource(R.drawable.error) // Add an error drawable
            )
        }

        // Page Indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(imageUrls.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.5f)
                        )
                )
            }
        }
    }
}


//Below the Image Section
@Composable
private fun PricingDetails(
    price: Int,
    neg: Boolean,
    title: String,
    address: String,
    beds: Int,
    kitchen: Int,
    living: Int,
    bathroom: Int,
    propertyType: String,
    hasVideo: Boolean = false,
    sessionState: SessionState,
    navController: NavController
) {
    var showVideo by remember { mutableStateOf(false) }

    if (showVideo) {
        AlertDialog(
            onDismissRequest = { showVideo = false },
            confirmButton = {},
            dismissButton = {},
            text = {
                FullScreenVideoPlayer(
                    onDismiss = { showVideo = false }
                )
            },
            modifier = Modifier.fillMaxSize()
        )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rs. $price",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if(sessionState.isAuthenticated && sessionState.userId != null && hasVideo) {
                    Row {
                        val context = LocalContext.current

                        IconButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        context,
                                        VideoPlayerActivity::class.java
                                    )
                                )
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.video),
                                contentDescription = "Video Tour",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val intent = Intent(context, Landscape360ViewerActivity::class.java)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.i360),
                                contentDescription = "360 View",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = if (neg) "Negotiable" else "Not Negotiable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = address,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show property features only if the type is not "Land" or "Commercial"
            if (propertyType.lowercase() !in listOf("land", "commercial")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    PropertyFeature(R.drawable.round_bed_24, label = "$beds Beds")
                    PropertyFeature(R.drawable.outline_kitchen_24, label = "$kitchen Kitchen")
                    PropertyFeature(R.drawable.baseline_checkroom_24, label = "$living Living Room")
                    PropertyFeature(R.drawable.outline_bathroom_24, label = "$bathroom Bathrooms")
                }
            }
        }
    }
}


@Composable
fun FullScreenVideoPlayer(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(
                "android.resource://${context.packageName}/${R.raw.tour}"
            )
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}


@Composable
fun PropertyOwner(owner: String, contact1: String, contact2: String, area: String, sessionState: SessionState) {
    // Property Owner Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            // Owner Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Owner",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = owner,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(7.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Contact Section
            if (sessionState.isAuthenticated && sessionState.userId != null)
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$contact1   $contact2",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Land Area Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Land Area: $area",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!sessionState.isAuthenticated && sessionState.userId == null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Login to see more details",
                        color = Color(0xFF2196F3), // bluish tone
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandablePropertyDetails(description: String) {
    // Track the expanded state
    var expanded by remember { mutableStateOf(false) }

    // Card with expandable details
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with a label and an expandable icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },  // Toggle expand/collapse
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "More Info",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "More about this property",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand/Collapse",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(5.dp))
            }

            if (expanded) {
                Text(
                    text = "\n$description",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Composable
fun PropertyFeature(icon: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ContactActionBar(
    phoneNumber: String,
    emailAddress: String,
    whatsappNumber: String,
    onMessageClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),  // Reduced height
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),  // Reduced vertical padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left aligned icons group
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.weight(1f)
            ) {
                // Phone Call Button
                ContactButton(
                    icon = R.drawable.phonecall_icon,
                    backgroundColor = Color(0xFF007AFF),
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Message Button
                ContactButton(
                    icon = R.drawable.messenger_icon,
                    backgroundColor = Color(0xFF00E2BE),
                    onClick = onMessageClick
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Email Button
                ContactButton(
                    icon = R.drawable.mail_icon,
                    backgroundColor = Color(0xFFFF3B30),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$emailAddress")
                        }
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // WhatsApp Button
                ContactButton(
                    icon = R.drawable.whatsapp_icon,
                    backgroundColor = Color(0xFF25D366),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/$whatsappNumber")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Share Button (Right aligned)
            ContactButton(
                icon = R.drawable.share_icon,
                backgroundColor = Color(0xFF34C759),
                onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out this property!")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                }
            )
        }
    }
}

@Composable
private fun ContactButton(
    icon: Int,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)  // Reduced from 56.dp to 44.dp
            .clip(RoundedCornerShape(10.dp))  // Slightly reduced corner radius
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(22.dp),  // Reduced from 28.dp to 22.dp
            colorFilter = ColorFilter.tint(Color.White)
        )
    }
}