package com.example.urbanfinder

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PropertyListingScreen(
    navController: NavController,
    propertyViewModel: PropertyViewModel,
    sessionState: SessionState,
    sessionViewModel: SessionViewModel
) {
    // State management
    val propertyList by propertyViewModel.properties.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }

    // Fetch properties on first load
    LaunchedEffect(true) {
        propertyViewModel.fetchProperties()
    }

    // Fetch user data when authenticated
    LaunchedEffect(sessionState.isAuthenticated, sessionState.userId) {
        sessionState.userId?.let { userId ->
            propertyViewModel.fetchUserData(userId)
            propertyViewModel.fetchUserBookmarks(userId)
        }
    }

    val user by propertyViewModel.userData.collectAsState(initial = null)

    // Pull-to-refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            propertyViewModel.fetchProperties()
            refreshing = false
        }
    )

    Scaffold(
        topBar = {
            TopBarWithSearch(
                searchQuery = searchQuery, // ✅ No need for .value
                onSearchQueryChange = { searchQuery = it }, // ✅ Correctly updates the state
                navController = navController
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        },
        containerColor = MaterialTheme.colorScheme.background
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
                if (sessionState.isAuthenticated) {
                    Text(
                        text = "      Welcome, ${user?.fullName ?: "User"}!",
                        fontSize = 24.sp,
                        color = Color(0xFF4CAF50)
                    )
                } else {
                    Text(
                        text = "Welcome to the App!",
                        fontSize = 24.sp,
                        color = Color.Gray
                    )
                }
                QuickSearchCategories(navController,propertyViewModel)
                Spacer(modifier = Modifier.height(10.dp))
                RentPlacesSection(navController, propertyList,propertyViewModel,sessionState)
                Spacer(modifier = Modifier.height(10.dp))
                HouseListingSection(navController, propertyList,propertyViewModel,sessionState)
                Spacer(modifier = Modifier.height(10.dp))
                ViewApartments(navController, propertyList,propertyViewModel,sessionState)
                Spacer(modifier = Modifier.height(10.dp))
                ViewCommercial(navController, propertyList,propertyViewModel,sessionState)
                Spacer(modifier = Modifier.height(10.dp))
                TrendingPlacesSection(navController, propertyList,propertyViewModel,sessionState)
                Spacer(modifier = Modifier.height(10.dp))
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

// Experimental Stuffs
@Composable
fun TopBarWithSearch(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    navController: NavController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Better padding
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start, // Align content properly
            modifier = Modifier.padding(start = 8.dp, end = 8.dp)
        ) {

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                placeholder = {
                    Text(
                        text = "Search properties...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .weight(1f) // Makes the search bar take remaining space
                    .padding(end = 8.dp), // Prevents tight spacing
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )
            IconButton(
                onClick = {
                    navController.navigate(search_filter) {
                        launchSingleTop = true
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Search Filter",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}



//Search Category
@Composable
fun QuickSearchCategories(navController: NavController, propertyViewModel: PropertyViewModel) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        CategoryButton(navController,text = "House", icon = R.drawable.home_icon,propertyViewModel)
        CategoryButton(navController,text = "Apartment", icon = R.drawable.custom_apartment_icon,propertyViewModel)
        CategoryButton(navController,text = "Rent", icon = R.drawable.rent_icon,propertyViewModel)
        CategoryButton(navController,text = "Land", icon = R.drawable.land_icon,propertyViewModel)
    }
}

//Rent a Place Section
@Composable
fun RentPlacesSection(
    navController: NavController,
    propertyList: List<Property>,
    propertyViewModel: PropertyViewModel,
    sessionState: SessionState
) {
    val rentProperties = propertyList.filter { it.approval=="approved"} // Filter only rental properties

    Column {
        SectionHeader(title = "Explore", navController)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(
                count = rentProperties.size,
                key = { index -> rentProperties[index].id }
            ) { index ->
                val item = rentProperties[index]

                RentPlaceholder(
                    navController = navController,
                    id = item.id,
                    title = item.title,
                    price = item.price,
                    location = item.address,
                    area = item.area,
                    imageUrl = item.photoUrls[0],
                    propertyViewModel = propertyViewModel,
                    status=item.status,
                    sessionState = sessionState
                )
            }
        }
    }
}

//View Apartments Section
@Composable
fun ViewApartments(
    navController: NavController,
    propertyList: List<Property>,
    propertyViewModel: PropertyViewModel,
    sessionState: SessionState
) {
    val houseProperties = propertyList.filter { it.propertyType == "Apartment" && it.approval=="approved"}

    Column {
        SectionHeader(title = "Apartments", navController)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(
                count = houseProperties.size,
                key = { index -> "house_${houseProperties[index].id}" }
            ) { index ->
                val item = houseProperties[index]

                PropertyCard(
                    navController = navController,
                    id = item.id,
                    title = item.title,
                    price = item.price,
                    location = item.address,
                    status = item.status,
                    area = item.area,
                    imageUrl = item.photoUrls[0],
                    propertyViewModel = propertyViewModel,
                    sessionState = sessionState
                )
            }
        }
    }
}

//View Commercial Section
@Composable
fun ViewCommercial(
    navController: NavController,
    propertyList: List<Property>,
    propertyViewModel: PropertyViewModel,
    sessionState: SessionState
) {
    val houseProperties = propertyList.filter { it.propertyType == "Commercial" && it.approval=="approved"}

    Column {
        SectionHeader(title = "Commercial Spaces", navController)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(
                count = houseProperties.size,
                key = { index -> "house_${houseProperties[index].id}" }
            ) { index ->
                val item = houseProperties[index]

                PropertyCard(
                    navController = navController,
                    id = item.id,
                    title = item.title,
                    price = item.price,
                    location = item.address,
                    status = item.status,
                    area = item.area,
                    imageUrl = item.photoUrls[0],
                    propertyViewModel = propertyViewModel,
                    sessionState = sessionState
                )
            }
        }
    }
}

//Buy Houses Section
@Composable
fun HouseListingSection(
    navController: NavController,
    propertyList: List<Property>,
    propertyViewModel: PropertyViewModel,
    sessionState: SessionState
) {
    val houseProperties = propertyList.filter { it.propertyType == "House" && it.approval=="approved"} // Filter only houses

    Column {
        SectionHeader(title = "Houses", navController)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(
                count = houseProperties.size,
                key = { index -> "house_${houseProperties[index].id}" }
            ) { index ->
                val item = houseProperties[index]

                PropertyCard(
                    navController = navController,
                    id = item.id,
                    title = item.title,
                    price = item.price,
                    location = item.address,
                    status = item.status,
                    area = item.area,
                    imageUrl = item.photoUrls[0],
                    propertyViewModel = propertyViewModel,
                    sessionState = sessionState
                )
            }
        }
    }
}

//Check Out Land Places Section
@Composable
fun TrendingPlacesSection(
    navController: NavController,
    propertyList: List<Property>,
    propertyViewModel: PropertyViewModel,
    sessionState: SessionState
) {
    val landProperties = propertyList.filter { it.propertyType == "Land" && it.approval=="approved"} // Filter only lands

    Column {
        SectionHeader(title = "Buy Lands", navController)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(
                count = landProperties.size,
                key = { index -> "land_${landProperties[index].id}" }
            ) { index ->
                val item = landProperties[index]

                PropertyCard(
                    navController = navController,
                    id = item.id,
                    title = item.title,
                    price = item.price,
                    location = item.address,
                    status = item.status,
                    area = item.area,
                    imageUrl = item.photoUrls[0],
                    propertyViewModel = propertyViewModel,
                    sessionState = sessionState
                )
            }
        }
    }
}


//Bottom Navigation Bar
@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val surfaceColor = MaterialTheme.colorScheme.surface

    val items = listOf(
        Triple(R.drawable.home_icon, "Home", ScreenA),
        Triple(R.drawable.menu, "Listing", ScreenD),
        Triple(R.drawable.saved_icon, "Saved", SavedScreen),
        Triple(R.drawable.profile_icon, "Profile", ProfileScreen)
    )

    //Bottom Bar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        // Bottom Navigation Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceColor)
        )

        // Navigation Items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, (icon, label, route) ->
                if (index == 2) {
                    Spacer(modifier = Modifier.width(80.dp).clip(RoundedCornerShape(16.dp)))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                ) {
                    val selected = currentRoute == route
                    Image(
                        painter = painterResource(id = icon),
                        contentDescription = label,
                        colorFilter = if (selected) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                navController.navigate(ScreenC) {
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-34).dp),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 15.dp
            )
        ) {
            Image(
                painter = painterResource(R.drawable.add_icon),
                contentDescription = "Add Button",
                modifier = Modifier.size(64.dp)
            )
        }

    }
}


//See All Button
@Composable
fun SectionHeader(title: String, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = { navController.navigate(ScreenD) }) {
            Text("See all")
        }
    }
}

//BLUEPRINTS & DATA CLASSES (Data Classes so that everything doesn't have to be a function parameter)

//House Apartment Rent Land Button
@Composable
fun CategoryButton(navController: NavController,text: String, icon: Int,propertyViewModel: PropertyViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable  {
                propertyViewModel.getFilteredProperties(
                    selectedBedrooms = "Any",
                    selectedFurnishing = "Any",
                    selectedBathrooms = "Any",
                    selectedAreaFrom = "1",
                    selectedAreaTo =  "600000",
                    selectedPriceFrom = "1",
                    selectedPriceTo ="1000000000" ,
                    selectedPropertyType = when (text) {"Rent" -> "Any" else -> text}
                ) { success, message, _ ->
                    if (success) {
                        Log.d("FILTER", "Navigation to result screen")
                        navController.navigate(results) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    } else {
                        Log.e("FILTER", "Filter failed: $message")
                    }
                }
            }
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = text,
                modifier = Modifier.size(40.dp)  // Adjust size if needed
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

//Buy Houses and Trending Places Section Blueprint
@Composable
fun PropertyCard(
    navController: NavController,
    id: String,
    title: String,
    price: Int,
    location: String,
    area: String,
    imageUrl: String,
    sessionState: SessionState,
    propertyViewModel: PropertyViewModel,
    status: String // String-based status
) {
    val isBookmarked by propertyViewModel.isPropertyBookmarked(id).collectAsState(initial = false)

    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(200.dp)
            .height(240.dp)
            .clickable {
                navController.navigate(ScreenB(id = id))
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            var isLoading by remember { mutableStateOf(true) }
            var isError by remember { mutableStateOf(false) }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            if (isError) {
                Image(
                    painter = painterResource(id = R.drawable.house1),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = {
                    isLoading = false
                    isError = true
                }
            )

            // Price and Status tag stacked
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = "Rs. $price",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp)) // Adds spacing between price and status

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (status.lowercase()) {
                            "available" -> Color(0xFF4CAF50) // Green
                            "sold" -> Color(0xFFF44336) // Red
                            "pending" -> Color(0xFFFFC107) // Yellow
                            else -> Color.Gray // Default gray
                        }
                    )
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (sessionState.isAuthenticated && sessionState.userId != null) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    BookmarkButton(
                        propertyId = id,
                        userId = sessionState.userId,
                        isBookmarked = isBookmarked,
                        onBookmarkClick = {
                            propertyViewModel.toggleBookmark(id, sessionState.userId)
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 200f
                        )
                    )
            )

            // Filtered location (removes address code and postal part)
            val filteredLocation = location.substringAfter(", ").substringBeforeLast(",")

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.7f),
                            offset = Offset(0f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = filteredLocation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.7f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Text(
                    text = "$area sq feet",
                    style = MaterialTheme.typography.bodySmall.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.7f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun BookmarkButton(
    propertyId: String,
    userId: String?,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit
) {
    // Add local loading state
    var isLoading by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(12.dp)
            .size(40.dp)
            .clickable(enabled = !isLoading) {
                isLoading = true
                onBookmarkClick()
                // Reset loading after a short delay
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    isLoading = false
                }
            },
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isBookmarked)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = if (isBookmarked)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isBookmarked) "Remove from saved" else "Save property",
                    tint = if (isBookmarked)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RentPlaceholder(
    navController: NavController,
    id: String,
    title: String,
    price: Int,
    location: String,
    area: String,
    imageUrl: String,
    status: String,
    sessionState: SessionState,
    propertyViewModel: PropertyViewModel
) {
    val isBookmarked by propertyViewModel.isPropertyBookmarked(id).collectAsState(initial = false)

    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(250.dp)
            .height(380.dp)
            .clickable {
                navController.navigate(ScreenB(id = id))
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            var isLoading by remember { mutableStateOf(true) }
            var isError by remember { mutableStateOf(false) }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (isError) {
                Image(
                    painter = painterResource(id = R.drawable.house1),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = {
                    isLoading = false
                    isError = true
                }
            )

            // Top-left: Price tag with availability badge
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price tag
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = "Rs. $price",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Availability Badge (Green for Available, Red for Sold)
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (status == "Available") Color(0xFF4CAF50) // Green
                        else Color(0xFFD32F2F) // Red
                    )
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bookmark button at top right
            if (sessionState.isAuthenticated && sessionState.userId != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                ) {
                    BookmarkButton(
                        propertyId = id,
                        userId = sessionState.userId,
                        isBookmarked = isBookmarked,
                        onBookmarkClick = {
                            propertyViewModel.toggleBookmark(id, sessionState.userId)
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 300f
                        )
                    )
            )

            // Content overlay at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.7f),
                            offset = Offset(0f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.7f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Text(
                    text = "$area sq feet",
                    style = MaterialTheme.typography.bodySmall.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.7f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}