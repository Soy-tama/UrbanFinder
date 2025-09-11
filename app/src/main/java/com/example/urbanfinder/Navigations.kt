package com.example.urbanfinder

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Composable
fun MyAppNavigation(
    propertyViewModel: PropertyViewModel,
    sessionViewModel: SessionViewModel = LocalSessionManager.current
) {
    val navController = rememberNavController()
    val sessionState by sessionViewModel.sessionState.collectAsState()

    // List of screens allowed without authentication
    listOf(
        ScreenA::class.qualifiedName,
        ScreenB::class.qualifiedName,
        ScreenD::class.qualifiedName,
        ProfileScreen::class.qualifiedName,
        LoginScreen::class.qualifiedName,
        results::class.qualifiedName,
        search_filter::class.qualifiedName,
        SignupScreen::class.qualifiedName
    )

    NavHost(navController = navController, startDestination = ScreenA::class.qualifiedName ?: "") {
        composable<ScreenA> {
            PropertyListingScreen(
                navController = navController,
                propertyViewModel = propertyViewModel,
                sessionState = sessionState,
                sessionViewModel = sessionViewModel
            )
        }

        composable<ScreenB> {
            val parameters = it.toRoute<ScreenB>()
            PropertyDetailsScreen(
                navController = navController,
                id = parameters.id,
                propertyViewModel = propertyViewModel,
                sessionState = sessionState,
                sessionViewModel = sessionViewModel
            )
        }

        composable<ScreenC> {
            // Check authentication before showing protected screen
            if (sessionState.isAuthenticated) {
                AddToDatabase(
                    navController = navController,
                    sessionState = sessionState,
                    sessionViewModel = sessionViewModel
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(ProfileScreen::class.qualifiedName ?: "") {
                        popUpTo(navController.graph.startDestinationId)
                    }
                }
            }
        }

        composable<ScreenD> {
            ListingScreen(
                navController = navController,
                propertyViewModel = propertyViewModel,
                sessionState = sessionState,
                sessionViewModel = sessionViewModel
            )
        }

        composable<SavedScreen> {
            // Check authentication before showing protected screen
            if (sessionState.isAuthenticated) {
                SavedScreen(
                    navController = navController,
                    sessionState = sessionState,
                    sessionViewModel = sessionViewModel
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(ProfileScreen::class.qualifiedName ?: "") {
                        popUpTo(navController.graph.startDestinationId)
                    }
                }
            }
        }

        composable<ProfileScreen> {
            Profile(
                navController = navController,
                sessionState = sessionState,
                sessionViewModel = sessionViewModel,
                propertyViewModel = propertyViewModel
            )
        }

        composable<LoginScreen> {
            LoginScreen(
                navController = navController,
                sessionViewModel = sessionViewModel
            )
        }

        composable<SignupScreen> {
            Signup(
                navController = navController,
                sessionViewModel = sessionViewModel
            )
        }

        composable<AboutUs> {
            AboutUs(
                navController = navController,
                sessionViewModel = sessionViewModel
            )
        }

        composable<UserProperties> {
            UserProperties(
                navController = navController,
                sessionViewModel = sessionViewModel,
                propertyViewModel = propertyViewModel
            )
        }

        composable<Preferences> {
            Preferences(
                navController = navController,
                sessionViewModel = sessionViewModel,
                propertyViewModel = propertyViewModel,
                sessionState=sessionState
            )
        }

        composable<search_filter> {
            Filter(
                navController = navController,
                sessionViewModel = sessionViewModel,
                propertyViewModel = propertyViewModel,
                sessionState=sessionState
            )
        }

        composable<results> {
            ResultsScreen(
                navController = navController,
                propertyViewModel = propertyViewModel
            )
        }

        composable<documentPage> {
            DocumentsScreen(
                navController = navController,
                propertyViewModel = propertyViewModel,
                sessionState = sessionState
            )
        }
    }
}

// Serializable route objects remain unchanged
@Serializable
object ScreenA

@Serializable
data class ScreenB(
    val id: String
)

@Serializable
object ScreenC

@Serializable
object ScreenD

@Serializable
object SavedScreen

@Serializable
object ProfileScreen

@Serializable
object LoginScreen

@Serializable
object SignupScreen

@Serializable
object search_filter

@Serializable
object results

@Serializable
object documentPage

@Serializable
object AboutUs

@Serializable
object UserProperties

@Serializable
object Preferences
