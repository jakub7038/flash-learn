package com.example.flashlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flashlearn.data.local.TokenManager
import com.example.flashlearn.data.remote.RetrofitClient
import com.example.flashlearn.ui.screens.DashboardScreen
import com.example.flashlearn.ui.screens.DeckListScreen
import com.example.flashlearn.ui.screens.LoginScreen
import com.example.flashlearn.ui.screens.RegisterScreen
import com.example.flashlearn.ui.theme.FlashLearnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(applicationContext)
        TokenManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            FlashLearnTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
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
                        composable("dashboard") {
                            DashboardScreen(
                                onNavigateToDeckList = { navController.navigate("deck_list") },
                                onLogout = {
                                    TokenManager.clearTokens()
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("deck_list") {
                            DeckListScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
