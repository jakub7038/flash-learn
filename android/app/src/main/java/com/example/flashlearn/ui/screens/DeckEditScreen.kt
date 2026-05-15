package com.example.flashlearn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flashlearn.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeckEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val errTitleRequired  = stringResource(R.string.error_title_required)
    val errTitleMinLength = stringResource(R.string.error_title_min_length)
    val errTitleMaxLength = stringResource(R.string.error_title_max_length)

    val isEditMode = uiState.title.isNotEmpty() || uiState.description.isNotEmpty()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (isEditMode) R.string.deck_edit_title else R.string.deck_new_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.save(errTitleRequired, errTitleMinLength, errTitleMaxLength)
                        },
                        enabled = !uiState.isLoading && !uiState.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.content_desc_save)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text(stringResource(R.string.label_deck_name)) },
                    isError = uiState.titleError != null,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uiState.titleError ?: "",
                                color = if (uiState.titleError != null) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${uiState.title.length}/${DeckEditViewModel.TITLE_MAX}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text(stringResource(R.string.label_description_optional)) },
                    supportingText = {
                        Text(
                            text = "${uiState.description.length}/${DeckEditViewModel.DESC_MAX}",
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                CategoryDropdown(
                    categories = uiState.categories,
                    selectedSlug = uiState.selectedCategorySlug,
                    isLoading = uiState.isCategoriesLoading,
                    onSelect = viewModel::onCategorySelected
                )

                Button(
                    onClick = {
                        viewModel.save(errTitleRequired, errTitleMinLength, errTitleMaxLength)
                    },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(if (isEditMode) R.string.btn_save_changes else R.string.btn_create_deck))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<com.example.flashlearn.data.remote.dto.CategoryDto>,
    selectedSlug: String?,
    isLoading: Boolean,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.slug == selectedSlug }
    val selectedName = selectedCategory?.name ?: categoryNameForSlug(selectedSlug)
    val selectedIcon = selectedCategory?.iconName?.let(::iconForCategoryName)
        ?: iconForCategorySlug(selectedSlug)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = if (isLoading) stringResource(R.string.category_loading) else selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = !isLoading,
            label = { Text(stringResource(R.string.label_category)) },
            leadingIcon = {
                Icon(
                    imageVector = selectedIcon,
                    contentDescription = null
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.category_none)) },
                leadingIcon = { Icon(Icons.Default.Style, contentDescription = null) },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = iconForCategoryName(category.iconName),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onSelect(category.slug)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun categoryNameForSlug(slug: String?): String = when (slug) {
    "jezyki" -> stringResource(R.string.marketplace_cat_languages)
    "programowanie" -> stringResource(R.string.marketplace_cat_programming)
    "matematyka" -> stringResource(R.string.marketplace_cat_math)
    "nauki-scisle" -> stringResource(R.string.marketplace_cat_science)
    "historia" -> stringResource(R.string.marketplace_cat_history)
    "inne" -> stringResource(R.string.marketplace_cat_other)
    else -> stringResource(R.string.category_none)
}
