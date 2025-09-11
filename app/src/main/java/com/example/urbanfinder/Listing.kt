package com.example.urbanfinder

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ListingScreen(
    navController: NavController,
    propertyViewModel: PropertyViewModel,
    sessionState: SessionState,
    sessionViewModel: SessionViewModel
) {
    val propertyList by propertyViewModel.properties.collectAsState()
    val scrollState = rememberScrollState()
    val searchQuery = remember { mutableStateOf("") }


    Scaffold(
        topBar = {
            TopBarWithSearch(
                searchQuery = searchQuery.value,
                onSearchQueryChange = {
                    searchQuery.value = it
                    propertyViewModel.filterPropertiesByQuery(it)
                },
                navController = navController
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            ListingSectionHeader(title = "Listings")
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                propertyList.forEachIndexed { _, property ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                            navController.navigate(ScreenB(id = property.id))
                        }
                    ) {
                        PropertyCard(property = property)
                    }
                }
            }
        }
    }
}


@Composable
fun ListingSectionHeader(title: String) {
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
    }
}

@Composable
fun PropertyCard(property: Property) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        // Carousel for property images
        PropertyImagesCarousel(propertyImages = listOf(property.photoUrls[0]))

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = property.title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 20.sp
        )
        Text(
            text = "Rs. ${property.price}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        PropertyDetailsRow()
        Spacer(modifier = Modifier.height(8.dp))

        Divider(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth(),
            color = Color.Gray
        )
        ActionButtonsRow() // Keeps only action buttons
    }
}



@Composable
fun PropertyImagesCarousel(propertyImages: List<String>) {
    val pagerState = rememberPagerState(pageCount = { propertyImages.size })

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
                model = propertyImages[page], // Using Coil to load URL-based images
                contentDescription = "Property Image ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Page Indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(propertyImages.size) { index ->
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


@Composable
fun PropertyDetailsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        PropertyFeature((R.drawable.round_bed_24), label = " Beds")
        PropertyFeature((R.drawable.outline_kitchen_24), label = "Kitchen")
        PropertyFeature((R.drawable.baseline_checkroom_24), label = "Living Room")
        PropertyFeature((R.drawable.outline_bathroom_24), label = " Bathrooms")
    }
}

@Composable
fun ActionButtonsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp), // Optional padding for the Row
        horizontalArrangement = Arrangement.SpaceBetween, // Space between text and icons
        verticalAlignment = Alignment.CenterVertically // Vertically center the content
    ) {
        // Left side: Updated Text
        Text(
            text = "Updated 16 hours ago",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Right side: Icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Spacing between icons
        ) {
            IconButton(onClick = { /* Call action */ }) {
                Icon(imageVector = Icons.Default.Call, contentDescription = "Call")
            }
            IconButton(onClick = { /* Email action */ }) {
                Icon(imageVector = Icons.Default.Email, contentDescription = "Email")
            }
            IconButton(onClick = { /* WhatsApp action */ }) {
                Icon(imageVector = Icons.Default.Favorite, contentDescription = "WhatsApp")
            }
            IconButton(onClick = { /* Share action */ }) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
            }
        }
        }
}