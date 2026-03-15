package com.example.flashlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flashlearn.data.local.TokenManager
import com.example.flashlearn.data.remote.RetrofitClient
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
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                onNavigateToRegister = { navController.navigate("register") },
                                onNavigateToLogin = { navController.navigate("login") }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = { navController.navigate("login") },
                                onNavigateToLogin = { navController.navigate("login") }
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("logged_in") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = { navController.navigate("register") }
                            )
                        }
                        composable("logged_in") {
                            LoggedInScreen(
                                onLogout = {
                                    TokenManager.clearTokens()
                                    navController.navigate("home") {
                                        popUpTo("logged_in") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onNavigateToRegister: () -> Unit, onNavigateToLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FlashLearn",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zaloguj się")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zarejestruj się")
        }
    }
}

@Composable
fun LoggedInScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onLogout) {
            Text("Wyloguj")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    FlashLearnTheme {
        HomeScreen(onNavigateToRegister = {}, onNavigateToLogin = {})
    }
}
