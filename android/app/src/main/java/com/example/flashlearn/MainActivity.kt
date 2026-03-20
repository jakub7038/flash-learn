package com.example.flashlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.flashlearn.data.local.TokenManager
import com.example.flashlearn.data.remote.RetrofitClient
import com.example.flashlearn.ui.screens.DeckEditScreen
import com.example.flashlearn.ui.screens.LoginScreen
import com.example.flashlearn.ui.screens.MainScreen
import com.example.flashlearn.ui.screens.RegisterScreen
import com.example.flashlearn.ui.theme.FlashLearnTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            FlashLearnTheme {
                val navController = rememberNavController()
                val startDestination = if (TokenManager.getAccessToken() != null) "main" else "login"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = { navController.navigate("register") }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = { navController.navigate("login") },
                            onNavigateToLogin = { navController.navigate("login") }
                        )
                    }
                    composable("main") {
                        MainScreen(
                            onLogout = {
                                TokenManager.clearTokens()
                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true }
                                }
                            },
                            onNavigateToCreateDeck = {
                                navController.navigate("deck/create")
                            }
                        )
                    }
                    composable("deck/create") {
                        DeckEditScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "deck/edit/{deckId}",
                        arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                        DeckEditScreen(
                            deckId = deckId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
