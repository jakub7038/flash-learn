package com.example.flashlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Learn : BottomNavItem("learn", "Nauka", Icons.Default.PlayArrow)
    object MyDecks : BottomNavItem("my_decks", "Moje talie", Icons.AutoMirrored.Filled.List)
    object Create : BottomNavItem("create", "Dodaj", Icons.Default.Add)
    object Explore : BottomNavItem("explore", "Przeglądaj", Icons.Default.Search)
    object Profile : BottomNavItem("profile", "Profil", Icons.Default.Person)
}

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToCreateDeck: () -> Unit = {}
) {
    var selectedItem by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Learn) }

    val items = listOf(
        BottomNavItem.Learn,
        BottomNavItem.MyDecks,
        BottomNavItem.Create,
        BottomNavItem.Explore,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                items.forEach { item ->
                    val isSelected = selectedItem == item
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { selectedItem = item }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedItem) {
                BottomNavItem.Learn -> LearnScreen()
                BottomNavItem.MyDecks -> DeckListScreen(
                    onNavigateToCreateDeck = onNavigateToCreateDeck
                )
                BottomNavItem.Create -> CreateScreen()
                BottomNavItem.Explore -> MarketplaceScreen()
                BottomNavItem.Profile -> DashboardScreen(onLogout = onLogout)
            }
        }
    }
}
