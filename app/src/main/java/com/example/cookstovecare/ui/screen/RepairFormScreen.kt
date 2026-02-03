package com.example.cookstovecare.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.cookstovecare.R
import com.example.cookstovecare.ui.viewmodel.RepairFormViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
 * Repair Form screen: Repair date, Parts Replaced checklist, Repair Notes,
 * Before/After images (required), Submit.
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
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val beforeImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setBeforeRepairImageUri(uri?.toString())
    }
    val afterImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setAfterRepairImageUri(uri?.toString())
    }

    val errorMessageRes = when (uiState.errorMessage) {
        "repair_date_before_collection" -> R.string.error_repair_date_before_collection
        "parts_or_notes_required" -> R.string.error_parts_or_notes_required
        "parts_or_notes_or_type_required" -> R.string.error_type_of_repair_required
        "images_required" -> R.string.error_images_required
        else -> null
    }

    // Show confirmation and navigate on success
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
            // 1. Pre-filled from task: Cookstove Number, Received Date, Received Image
            Text(
                text = stringResource(R.string.cookstove_number),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = uiState.cookstoveNumber,
                onValueChange = {},
                label = { Text(stringResource(R.string.cookstove_number)) },
                placeholder = { Text(stringResource(R.string.cookstove_number)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                text = stringResource(R.string.received_date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.collectionDateMillis > 0L) {
                            dateFormat.format(Date(uiState.collectionDateMillis))
                        } else {
                            stringResource(R.string.loading)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                }
            }

            Text(
                text = stringResource(R.string.received_image),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ImagePickerCard(
                imageUri = uiState.beforeRepairImageUri,
                onSelectClick = { beforeImagePicker.launch("image/*") }
            )

            // 2. Type of Repair (multi-select)
            Text(
                text = stringResource(R.string.type_of_repair),
                style = MaterialTheme.typography.titleSmall
            )
            RepairTypeOfRepairCheckboxes(
                selectedTypes = uiState.selectedTypesOfRepair,
                onTypeToggled = { viewModel.toggleTypeOfRepair(it) }
            )

            // 3. Submit Date and Image
            Text(
                text = stringResource(R.string.submit_date),
                style = MaterialTheme.typography.titleSmall
            )
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = uiState.repairCompletionDateMillis }
                    android.app.DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            cal.set(y, m, d)
                            viewModel.updateRepairDate(cal.timeInMillis)
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
                    Text(
                        text = dateFormat.format(Date(uiState.repairCompletionDateMillis)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                }
            }
            Text(
                text = stringResource(R.string.submit_image),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ImagePickerCard(
                imageUri = uiState.afterRepairImageUri,
                onSelectClick = { afterImagePicker.launch("image/*") }
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

@Composable
private fun ImagePickerCard(
    imageUri: String?,
    onSelectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        if (imageUri != null && imageUri.isNotBlank()) {
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
