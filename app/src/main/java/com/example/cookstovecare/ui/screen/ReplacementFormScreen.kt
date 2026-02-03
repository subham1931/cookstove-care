package com.example.cookstovecare.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.cookstovecare.R
import com.example.cookstovecare.ui.viewmodel.ReplacementFormViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Replacement Form screen: Old/New cookstove images (required), Old/New numbers,
 * Replacement date (auto=today), Submit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplacementFormScreen(
    viewModel: ReplacementFormViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val oldImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        viewModel.setOldCookstoveImageUri(uri?.toString())
    }
    val newImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        viewModel.setNewCookstoveImageUri(uri?.toString())
    }

    val errorMessageRes = when (uiState.errorMessage) {
        "old_new_numbers_same" -> R.string.error_old_new_same
        "duplicate_new_cookstove_number" -> R.string.error_duplicate_new_cookstove
        "images_required" -> R.string.error_images_required
        else -> null
    }

    androidx.compose.runtime.LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.replacement_saved),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.replacement_form_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Old Cookstove Number (read-only, from task)
            OutlinedTextField(
                value = uiState.oldCookstoveNumber,
                onValueChange = { },
                label = { Text(stringResource(R.string.old_cookstove_number)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true
            )

            // New Cookstove Number
            OutlinedTextField(
                value = uiState.newCookstoveNumber,
                onValueChange = { viewModel.updateNewCookstoveNumber(it) },
                label = { Text(stringResource(R.string.new_cookstove_number)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessageRes != null
            )

            // Replacement Date (auto = today, read-only)
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.replacement_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(uiState.replacementDateMillis)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Old Cookstove Image
            Text(
                text = stringResource(R.string.old_cookstove_image),
                style = MaterialTheme.typography.titleSmall
            )
            ReplacementImagePickerCard(
                imageUri = uiState.oldCookstoveImageUri,
                onSelectClick = { oldImagePicker.launch("image/*") }
            )

            // New Cookstove Image
            Text(
                text = stringResource(R.string.new_cookstove_image),
                style = MaterialTheme.typography.titleSmall
            )
            ReplacementImagePickerCard(
                imageUri = uiState.newCookstoveImageUri,
                onSelectClick = { newImagePicker.launch("image/*") }
            )

            if (errorMessageRes != null) {
                Text(
                    text = stringResource(errorMessageRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.submit(
                        onSuccess = onSuccess,
                        onError = { }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(stringResource(R.string.submit))
            }
        }
    }
}

@Composable
private fun ReplacementImagePickerCard(
    imageUri: String?,
    onSelectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        if (imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
        }
        Button(
            onClick = onSelectClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.select_image))
        }
    }
}
