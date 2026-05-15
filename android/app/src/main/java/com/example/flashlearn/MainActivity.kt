package com.example.flashlearn

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.flashlearn.data.local.TokenManager
import com.example.flashlearn.notification.ReminderScheduler
import com.example.flashlearn.ui.screens.DeckDetailScreen
import com.example.flashlearn.ui.screens.DeckEditScreen
import com.example.flashlearn.ui.screens.FlashcardEditScreen
import com.example.flashlearn.ui.screens.FlashcardListScreen
import com.example.flashlearn.ui.screens.LearnScreen
import com.example.flashlearn.ui.screens.LoginScreen
import com.example.flashlearn.ui.screens.MainScreen
import com.example.flashlearn.ui.screens.MarketplaceDeckDetailScreen // Dodany import
import com.example.flashlearn.ui.screens.RegisterScreen
import com.example.flashlearn.ui.theme.FlashLearnTheme
import dagger.hilt.android.AndroidEntryPoint

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

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        ReminderScheduler.schedule(this, prefs)
        TokenManager.init(applicationContext)

        val authprefs = applicationContext.getSharedPreferences("flashlearn_prefs", Context.MODE_PRIVATE)
        // Poprawka 1: Użycie authprefs zamiast prefs do weryfikacji tokena
        val hasToken = authprefs.getString("access_token", null) != null

        enableEdgeToEdge()
        setContent {
            FlashLearnTheme {
                val navController = rememberNavController()
                val startDestination = if (hasToken) "main" else "login"

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
                                navController.navigate("login") {
                                    popUpTo(0)
                                }
                            },
                            onNavigateToCreateDeck = {
                                navController.navigate("deck/create")
                            },
                            onNavigateToDeckDetail = { deckId ->
                                navController.navigate("deck/$deckId/detail")
                            },
                            onNavigateToEditDeck = { deckId ->
                                navController.navigate("deck/edit/$deckId")
                            },
                            onNavigateToLearn = { deckId ->
                                navController.navigate("learn/$deckId")
                            },
                            onNavigateToMarketplaceDetail = { deckId ->
                                navController.navigate("marketplace_detail/$deckId")
                            }
                        )
                    }

                    composable(
                        route = "marketplace_detail/{deckId}",
                        arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                    ) {
                        // Poprawka 2: Wywołanie bezpośrednie z importem
                        MarketplaceDeckDetailScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLocalDecks = {
                                navController.popBackStack("main", inclusive = false)
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
                    ) {
                        DeckEditScreen(
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
                            onNavigateToEditDeck = { id -> navController.navigate("deck/edit/$id") },
                            onNavigateToLearn = { id -> navController.navigate("learn/$id") },
                            onNavigateToFlashcards = { id -> navController.navigate("deck/$id/flashcards") }
                        )
                    }
                    composable(
                        route = "learn/{deckId}",
                        arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                    ) {
                        LearnScreen(
                            onNavigateBack = { navController.popBackStack() }
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
                    ) {
                        FlashcardEditScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "flashcard/edit/{flashcardId}",
                        arguments = listOf(navArgument("flashcardId") { type = NavType.LongType })
                    ) {
                        FlashcardEditScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}