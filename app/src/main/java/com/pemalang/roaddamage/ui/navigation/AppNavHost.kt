package com.pemalang.roaddamage.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pemalang.roaddamage.ui.screens.HomeScreen
import com.pemalang.roaddamage.ui.screens.OnboardingScreen
import com.pemalang.roaddamage.ui.screens.SettingsScreen
import com.pemalang.roaddamage.ui.screens.SplashScreen
import com.pemalang.roaddamage.ui.screens.TripListScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.pemalang.roaddamage.ui.screens.RecordingViewModel
import com.pemalang.roaddamage.ui.screens.TripListViewModel
import com.pemalang.roaddamage.ui.screens.SettingsViewModel

object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Trips = "trips"
    const val Detail = "detail/{tripId}"
    const val Settings = "settings"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val recordingViewModel: RecordingViewModel = hiltViewModel()
    val tripListViewModel: TripListViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(
                navController = navController,
                startDestination = Routes.Splash,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
        ) {
            composable(Routes.Splash) {
                SplashScreen(
                        onFinished = { completed ->
                            val targetRoute = if (completed) Routes.Home else Routes.Onboarding
                            navController.navigate(targetRoute) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        }
                )
            }
            composable(Routes.Onboarding) {
                OnboardingScreen(
                        onContinue = {
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.Onboarding) { inclusive = true }
                            }
                        }
                )
            }
            composable(Routes.Home) {
                HomeScreen(
                        onStartRecording = {},
                        onOpenTrips = { navController.navigate(Routes.Trips) },
                        onOpenSettings = { navController.navigate(Routes.Settings) },
                        vm = recordingViewModel
                )
            }
            composable(Routes.Trips) {
                com.pemalang.roaddamage.ui.screens.TripListScreen(
                        onOpenTrip = { trip -> navController.navigate("detail/${trip.tripId}") },
                        onNavigateHome = {
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.Home) { inclusive = true }
                            }
                        },
                        onNavigateSettings = { navController.navigate(Routes.Settings) },
                        vm = tripListViewModel
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateHome = {
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.Home) { inclusive = true }
                            }
                        },
                        onNavigateTrips = { navController.navigate(Routes.Trips) },
                        vm = settingsViewModel
                )
            }
            composable(
                    route = Routes.Detail,
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                com.pemalang.roaddamage.ui.screens.TripDetailScreen(
                        tripId = tripId,
                        onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
