package com.example.urbanfinder

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun Profile(
    navController: NavController,
    sessionState: SessionState,
    sessionViewModel: SessionViewModel,
    propertyViewModel: PropertyViewModel
) {
    val scope = rememberCoroutineScope()
    val userData = propertyViewModel.userData.collectAsState()

    // Effect to fetch user data when authenticated
    LaunchedEffect(sessionState.isAuthenticated) {
        if (sessionState.isAuthenticated && sessionState.userId != null) {
            propertyViewModel.fetchUserData(sessionState.userId)
        }
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
                .padding(horizontal = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(25.dp))

            if (sessionState.isAuthenticated) {
                // Logged in state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile Picture - Show user's image if available, otherwise show default icon
                    if (!userData.value?.profilePic.isNullOrEmpty()) {
                        AsyncImage(
                            model = userData.value?.profilePic,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.profile_icon),
                            error = painterResource(id = R.drawable.profile_icon)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.profile_icon),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User Name
                    Text(
                        text = userData.value?.fullName ?: "Loading...",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // User Email
                    Text(
                        text = userData.value?.email ?: "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    sessionViewModel.logout()
                                    navController.navigate(ScreenA) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Handle logout error if needed
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            } else {
                // Log in and Sign up buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { navController.navigate(LoginScreen) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                    ) {
                        Text(text = "Log in", color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { navController.navigate(SignupScreen) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text(text = "Sign up", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(thickness = 1.dp, color = Color.Gray)

            if (!sessionState.isAuthenticated) {
                Text(
                    text = "Meanwhile",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                if (sessionState.isAuthenticated) {
                    MenuItem("My Documents", Icons.Default.Settings) {
                        navController.navigate(documentPage)
                    }

                    MenuItem("My Preference", Icons.Default.Favorite) {
                        navController.navigate(Preferences)
                    }

                    MenuItem("My Properties", Icons.Default.Email) {
                        navController.navigate(UserProperties)
                    }

                    MenuItem("My Favorites", Icons.Default.Favorite) {
                        navController.navigate(SavedScreen)
                    }
                }

                MenuItem("About Us", Icons.Default.Home) {
                    navController.navigate(AboutUs)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun MenuItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}