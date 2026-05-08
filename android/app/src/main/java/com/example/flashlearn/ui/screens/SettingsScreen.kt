package com.example.flashlearn.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flashlearn.ui.settings.SettingsViewModel
import com.example.flashlearn.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleNotifications(true)
        } else {
            viewModel.toggleNotifications(false)
        }
    }

    BackHandler(onBack = onBack)

    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_notifications),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Switch(
                        checked = uiState.isNotificationsEnabled,
                        onCheckedChange = { isChecked ->

                            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.toggleNotifications(isChecked)
                            }
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()

                        .clickable { showTimePicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_notification_time),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = String.format("%02d:%02d", uiState.notificationHour, uiState.notificationMinute),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                var expanded by remember { mutableStateOf(false) }
                val languages = listOf("pl" to "Polski", "en" to "English")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(text = languages.find { it.first == uiState.language }?.second ?: "Polski")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },

                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            languages.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.updateLanguage(code)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                var themeExpanded by remember { mutableStateOf(false) }
                val themes = listOf("system" to "Systemowy", "light" to "Jasny", "dark" to "Ciemny")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Motyw aplikacji",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Box {
                        TextButton(onClick = { themeExpanded = true }) {
                            Text(text = themes.find { it.first == uiState.theme }?.second ?: "Systemowy")
                        }
                        DropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false },
                            // Tło wymuszone na Surface
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            themes.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.updateTheme(code)
                                        themeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }


            if (showTimePicker) {
                Dialog(onDismissRequest = { showTimePicker = false }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val timePickerState = rememberTimePickerState(
                                initialHour = uiState.notificationHour,
                                initialMinute = uiState.notificationMinute,
                                is24Hour = true
                            )


                            TimePicker(
                                state = timePickerState,
                                colors = TimePickerDefaults.colors(
                                    clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                                    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                                    clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                    selectorColor = MaterialTheme.colorScheme.primary,
                                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showTimePicker = false }) {
                                    Text(stringResource(R.string.btn_cancel))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        viewModel.updateNotificationTime(
                                            timePickerState.hour,
                                            timePickerState.minute
                                        )
                                        showTimePicker = false
                                    }
                                ) {
                                    Text(stringResource(R.string.btn_save_changes))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}