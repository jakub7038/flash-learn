package com.example.flashlearn.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flashlearn.R
import com.example.flashlearn.ui.network.NetworkViewModel
import kotlinx.coroutines.delay

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Learn : BottomNavItem("learn", R.string.nav_learn, Icons.Default.PlayArrow)
    object MyDecks : BottomNavItem("my_decks", R.string.nav_my_decks, Icons.AutoMirrored.Filled.List)
    object Create : BottomNavItem("create", R.string.nav_create, Icons.Default.Add)
    object Explore : BottomNavItem("explore", R.string.nav_explore, Icons.Default.Search)
    object Profile : BottomNavItem("profile", R.string.nav_profile, Icons.Default.Person)
}

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToCreateDeck: () -> Unit = {},
    onNavigateToDeckDetail: (deckId: Long) -> Unit = {},
    onNavigateToLearn: (deckId: Long) -> Unit = {},
    networkViewModel: NetworkViewModel = hiltViewModel()
) {
    val items = listOf(
        BottomNavItem.Learn,
        BottomNavItem.MyDecks,
        BottomNavItem.Create,
        BottomNavItem.Explore,
        BottomNavItem.Profile
    )

    val isOnline by networkViewModel.isOnline.collectAsStateWithLifecycle()
    var showReconnectedBanner by remember { mutableStateOf(false) }
    var wasOffline by remember { mutableStateOf(false) }

    // Stan zarządzający wyświetlaniem okna ustawień na podstronie profilu
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        if (!isOnline) {
            wasOffline = true
        } else if (wasOffline) {
            showReconnectedBanner = true
            delay(3_000)
            showReconnectedBanner = false
            wasOffline = false
        }
    }

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedItem = items[selectedIndex]

    // Resetowanie flagi ustawień, jeśli użytkownik zmieni zakładkę na dolnym pasku
    LaunchedEffect(selectedIndex) {
        if (items[selectedIndex] != BottomNavItem.Profile) {
            isSettingsOpen = false
        }
    }

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
                        val title = stringResource(item.titleRes)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { selectedIndex = items.indexOf(item) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = title,
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
        Column(modifier = Modifier.padding(paddingValues)) {
            AnimatedVisibility(
                visible = !isOnline,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OfflineBanner()
            }
            AnimatedVisibility(
                visible = showReconnectedBanner,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ReconnectedBanner()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedItem) {
                    BottomNavItem.Learn -> LearnHomeScreen(
                        onNavigateToLearn = onNavigateToLearn
                    )
                    BottomNavItem.MyDecks -> DeckListScreen(
                        onNavigateToCreateDeck = onNavigateToCreateDeck,
                        onNavigateToDeckDetail = onNavigateToDeckDetail
                    )
                    BottomNavItem.Create -> CreateScreen()
                    BottomNavItem.Explore -> MarketplaceScreen()
                    BottomNavItem.Profile -> {
                        if (isSettingsOpen) {
                            SettingsScreen(
                                onBack = { isSettingsOpen = false }
                            )
                        } else {
                            DashboardScreen(
                                onLogout = onLogout,
                                onNavigateToSettings = { isSettingsOpen = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.offline_banner_message),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ReconnectedBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.reconnected_banner_message),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.bodySmall
        )
    }
}