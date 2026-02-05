package com.example.cookstovecare.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.cookstovecare.ui.viewmodel.RepairFormViewModel
import java.io.File

private const val TYPE_TOP_PLACE = "TOP_PLACE"
private const val TYPE_DOOR_REPAIR = "DOOR_REPAIR"
private const val TYPE_BOTTOM_REPAIR = "BOTTOM_REPAIR"

/** Type of repair options */
private val TYPE_OF_REPAIR_OPTIONS = listOf(
    TYPE_TOP_PLACE to R.string.type_repair_top_place,
    TYPE_DOOR_REPAIR to R.string.type_repair_door_repair,
    TYPE_BOTTOM_REPAIR to R.string.type_repair_bottom_repair
)

/**
 * Repair Form screen (simplified for technician).
 * Technician submits only: Type of repair + After repair image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairFormScreen(
    viewModel: RepairFormViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val afterImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setAfterRepairImageUri(uri?.toString())
    }
    var afterCameraUri by remember { mutableStateOf<Uri?>(null) }
    val afterImageCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && afterCameraUri != null) {
            viewModel.setAfterRepairImageUri(afterCameraUri.toString())
        }
        afterCameraUri = null
    }

    val errorMessageRes = when (uiState.errorMessage) {
        "repair_date_before_collection" -> R.string.error_repair_date_before_collection
        "parts_or_notes_or_type_required" -> R.string.error_type_of_repair_required
        "images_required" -> R.string.error_images_required
        else -> null
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.repair_saved),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.repair_form_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!uiState.taskDataLoaded) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Type of Repair (required)
                Text(
                    text = stringResource(R.string.type_of_repair),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                RepairTypeOfRepairCheckboxes(
                    selectedTypes = uiState.selectedTypesOfRepair,
                    onTypeToggled = { viewModel.toggleTypeOfRepair(it) }
                )

                // 2. After Repair Image (required)
                Text(
                    text = stringResource(R.string.after_repair_image),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ImagePickerCard(
                    imageUri = uiState.afterRepairImageUri,
                    onTakePhoto = {
                        val file = File(context.cacheDir, "camera_after_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        afterCameraUri = uri
                        afterImageCamera.launch(uri)
                    },
                    onChooseFromGallery = { afterImagePicker.launch("image/*") }
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
}

/**
 * Repair Form content - can be used in a modal or standalone.
 * Simplified for technicians: only type of repair selection.
 */
@Composable
fun RepairFormContent(
    viewModel: RepairFormViewModel,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val errorMessageRes = when (uiState.errorMessage) {
        "repair_date_before_collection" -> R.string.error_repair_date_before_collection
        "parts_or_notes_or_type_required" -> R.string.error_type_of_repair_required
        else -> null
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.repair_saved),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            onSuccess()
        }
    }

    if (!uiState.taskDataLoaded) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type of Repair (required)
            Text(
                text = stringResource(R.string.type_of_repair),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            RepairTypeOfRepairCheckboxes(
                selectedTypes = uiState.selectedTypesOfRepair,
                onTypeToggled = { viewModel.toggleTypeOfRepair(it) }
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
private fun RepairTypeOfRepairCheckboxes(
    selectedTypes: Set<String>,
    onTypeToggled: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TYPE_OF_REPAIR_OPTIONS.forEach { (typeKey, labelRes) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = typeKey in selectedTypes,
                        onCheckedChange = { onTypeToggled(typeKey) }
                    )
                    Text(
                        text = stringResource(labelRes),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
