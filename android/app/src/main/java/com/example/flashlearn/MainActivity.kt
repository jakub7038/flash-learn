package com.example.flashlearn

import android.content.Context
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
import com.example.flashlearn.notification.ReminderScheduler
import com.example.flashlearn.ui.screens.DeckDetailScreen
import com.example.flashlearn.ui.screens.DeckEditScreen
import com.example.flashlearn.ui.screens.FlashcardEditScreen
import com.example.flashlearn.ui.screens.FlashcardListScreen
import com.example.flashlearn.ui.screens.LoginScreen
import com.example.flashlearn.ui.screens.MainScreen
import com.example.flashlearn.ui.screens.RegisterScreen
import com.example.flashlearn.ui.theme.FlashLearnTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.appcompat.app.AppCompatActivity

import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.flashlearn.sync.ReminderWorker
import java.util.concurrent.TimeUnit
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.flashlearn.notification.NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val prefs = getSharedPreferences("settings", 0)
        ReminderScheduler.schedule(this, prefs)
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
                                    popUpTo(0)
                                }
                            },
                            onNavigateToRegister = { navController.navigate("register") }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = { 
                                navController.navigate("login") {
                                    popUpTo(0)
                                } 
                            },
                            onNavigateToLogin = { navController.popBackStack() }
                        )
                    }
                    composable("main") {
                        MainScreen(
                            onLogout = {
                                TokenManager.clearTokens()
                                navController.navigate("login") {
                                    popUpTo(0)
                                }
                            },
                            onNavigateToCreateDeck = {
                                navController.navigate("deck/create")
                            },
                            onNavigateToDeckDetail = { deckId ->
                                navController.navigate("deck/$deckId/detail")
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
                    composable(
                        route = "deck/{deckId}/detail",
                        arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                        DeckDetailScreen(
                            deckId = deckId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLearn = { /* docelowo: navController.navigate("learn/$deckId") */ },
                            onNavigateToFlashcards = { id -> navController.navigate("deck/$id/flashcards") }
                        )
                    }
                    composable(
                        route = "deck/{deckId}/flashcards",
                        arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                        FlashcardListScreen(
                            deckId = deckId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToCreateFlashcard = {
                                navController.navigate("flashcard/$deckId/create")
                            },
                            onNavigateToEditFlashcard = { flashcardId ->
                                navController.navigate("flashcard/edit/$flashcardId")
                            }
                        )
                    }
                    composable(
                        route = "flashcard/{deckId}/create",
                        arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                        FlashcardEditScreen(
                            deckId = deckId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "flashcard/edit/{flashcardId}",
                        arguments = listOf(navArgument("flashcardId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val flashcardId = backStackEntry.arguments?.getLong("flashcardId") ?: return@composable
                        FlashcardEditScreen(
                            flashcardId = flashcardId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
