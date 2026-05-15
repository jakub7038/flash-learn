package com.example.flashlearn.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.vector.ImageVector

fun iconForCategoryName(name: String?): ImageVector = when (name) {
    "language" -> Icons.Default.Language
    "code" -> Icons.Default.Code
    "calculate" -> Icons.Default.Calculate
    "science" -> Icons.Default.Science
    "history_edu" -> Icons.Default.HistoryEdu
    "more_horiz" -> Icons.Default.MoreHoriz
    "apps" -> Icons.Default.Apps
    else -> Icons.Default.Style
}

fun iconForCategorySlug(slug: String?): ImageVector = when (slug) {
    "jezyki" -> Icons.Default.Language
    "programowanie" -> Icons.Default.Code
    "matematyka" -> Icons.Default.Calculate
    "nauki-scisle" -> Icons.Default.Science
    "historia" -> Icons.Default.HistoryEdu
    "inne" -> Icons.Default.MoreHoriz
    else -> Icons.Default.Style
}
