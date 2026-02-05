package com.example.cookstovecare.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cookstovecare.R
import com.example.cookstovecare.ui.components.ImagePickerCard
import com.example.cookstovecare.ui.viewmodel.ReplacementFormViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
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
    ) { uri: Uri? ->
        viewModel.setOldCookstoveImageUri(uri?.toString())
    }
    val newImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setNewCookstoveImageUri(uri?.toString())
    }

    var oldCameraUri by remember { mutableStateOf<Uri?>(null) }
    val oldImageCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && oldCameraUri != null) {
            viewModel.setOldCookstoveImageUri(oldCameraUri.toString())
        }
        oldCameraUri = null
    }
    var newCameraUri by remember { mutableStateOf<Uri?>(null) }
    val newImageCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && newCameraUri != null) {
            viewModel.setNewCookstoveImageUri(newCameraUri.toString())
        }
        newCameraUri = null
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

            // Collected Date (editable)
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = uiState.collectedDateMillis }
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(y, m, d)
                                viewModel.updateCollectedDate(cal.timeInMillis)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.collection_date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(Date(uiState.collectedDateMillis)),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Replacement Date (editable)
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = uiState.replacementDateMillis }
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(y, m, d)
                                viewModel.updateReplacementDate(cal.timeInMillis)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
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
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Old Cookstove Image
            Text(
                text = stringResource(R.string.old_cookstove_image),
                style = MaterialTheme.typography.titleSmall
            )
            ImagePickerCard(
                imageUri = uiState.oldCookstoveImageUri,
                onTakePhoto = {
                    val file = File(context.cacheDir, "camera_old_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    oldCameraUri = uri
                    oldImageCamera.launch(uri)
                },
                onChooseFromGallery = { oldImagePicker.launch("image/*") }
            )

            // New Cookstove Image
            Text(
                text = stringResource(R.string.new_cookstove_image),
                style = MaterialTheme.typography.titleSmall
            )
            ImagePickerCard(
                imageUri = uiState.newCookstoveImageUri,
                onTakePhoto = {
                    val file = File(context.cacheDir, "camera_new_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    newCameraUri = uri
                    newImageCamera.launch(uri)
                },
                onChooseFromGallery = { newImagePicker.launch("image/*") }
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

/**
 * Replacement Form content - can be used in a modal or standalone.
 */
@Composable
fun ReplacementFormContent(
    viewModel: ReplacementFormViewModel,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val oldImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setOldCookstoveImageUri(uri?.toString())
    }
    val newImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setNewCookstoveImageUri(uri?.toString())
    }

    var oldCameraUri by remember { mutableStateOf<Uri?>(null) }
    val oldImageCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && oldCameraUri != null) {
            viewModel.setOldCookstoveImageUri(oldCameraUri.toString())
        }
        oldCameraUri = null
    }
    var newCameraUri by remember { mutableStateOf<Uri?>(null) }
    val newImageCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && newCameraUri != null) {
            viewModel.setNewCookstoveImageUri(newCameraUri.toString())
        }
        newCameraUri = null
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

    Column(
        modifier = Modifier
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

        // Collected Date (editable)
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val cal = Calendar.getInstance().apply { timeInMillis = uiState.collectedDateMillis }
                    android.app.DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            cal.set(y, m, d)
                            viewModel.updateCollectedDate(cal.timeInMillis)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.collection_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(uiState.collectedDateMillis)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Replacement Date (editable)
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val cal = Calendar.getInstance().apply { timeInMillis = uiState.replacementDateMillis }
                    android.app.DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            cal.set(y, m, d)
                            viewModel.updateReplacementDate(cal.timeInMillis)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
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
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Old Cookstove Image
        Text(
            text = stringResource(R.string.old_cookstove_image),
            style = MaterialTheme.typography.titleSmall
        )
        ImagePickerCard(
            imageUri = uiState.oldCookstoveImageUri,
            onTakePhoto = {
                val file = File(context.cacheDir, "camera_old_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                oldCameraUri = uri
                oldImageCamera.launch(uri)
            },
            onChooseFromGallery = { oldImagePicker.launch("image/*") }
        )

        // New Cookstove Image
        Text(
            text = stringResource(R.string.new_cookstove_image),
            style = MaterialTheme.typography.titleSmall
        )
        ImagePickerCard(
            imageUri = uiState.newCookstoveImageUri,
            onTakePhoto = {
                val file = File(context.cacheDir, "camera_new_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                newCameraUri = uri
                newImageCamera.launch(uri)
            },
            onChooseFromGallery = { newImagePicker.launch("image/*") }
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

