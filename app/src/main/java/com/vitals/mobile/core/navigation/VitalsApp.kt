package com.vitals.mobile.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vitals.mobile.core.designsystem.VitalsTheme
import com.vitals.mobile.core.designsystem.components.VitalsBottomNavBar
import com.vitals.mobile.feature.auth.AuthScreen

@Composable
fun VitalsApp() {
    val appViewModel: AppViewModel = hiltViewModel()
    val isLoggedIn by appViewModel.isLoggedIn.collectAsState()

    when (isLoggedIn) {
        null -> LoadingScreen()
        false -> AuthScreen(onAuthenticated = { /* session flow drives recomposition automatically */ })
        true -> MainScaffold()
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VitalsTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = VitalsTheme.colors.primary)
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull { it.route in NavRoutes.bottomNavRoutes }?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != null) {
                VitalsBottomNavBar(
                    selectedRoute = currentRoute,
                    onSelect = { route ->
                        if (route == currentRoute) return@VitalsBottomNavBar
                        // Start tab ("Путь"): popBackStack is more reliable than navigate+restoreState.
                        if (route == NavRoutes.PATH) {
                            val restored = navController.popBackStack(NavRoutes.PATH, inclusive = false)
                            if (!restored) {
                                navController.navigate(NavRoutes.PATH) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            return@VitalsBottomNavBar
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
        containerColor = VitalsTheme.colors.background,
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.PATH,
            modifier = Modifier.padding(paddingValues),
        ) {
            vitalsNavGraph(navController)
        }
    }
}
