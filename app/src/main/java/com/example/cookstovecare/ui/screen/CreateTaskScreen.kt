package com.example.cookstovecare.ui.screen

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.cookstovecare.R
import com.example.cookstovecare.ui.components.ImagePickerCard
import com.example.cookstovecare.ui.viewmodel.CreateTaskViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Success dialog shown when a task is created. Use at Dashboard level (not inside Create Task modal)
 * so it appears over the dashboard, not over the Create Task sheet.
 */
@Composable
fun TaskCreatedSuccessDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.task_created_success), maxLines = 1) },
        text = { Text(stringResource(R.string.task_created_success_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok), color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

/**
 * Create Task form content for use in a modal or other container.
 * Cookstove Number, Customer Name, Collection Date, Received Product Image, Type of Process.
 * On successful save, calls onTaskCreatedSuccess to close modal; parent shows success dialog.
 */
private const val TYPE_REPAIRING = "REPAIRING"
private const val TYPE_REPLACEMENT = "REPLACEMENT"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskFormContent(
    viewModel: CreateTaskViewModel,
    onTaskCreatedSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val receivedImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setReceivedProductImageUri(uri?.toString())
    }

    var currentCameraUri by remember { mutableStateOf<Uri?>(null) }
    val receivedImageCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentCameraUri != null) {
            viewModel.setReceivedProductImageUri(currentCameraUri.toString())
        }
        currentCameraUri = null
    }

    val errorMessageRes = when (uiState.errorMessage) {
        "empty_cookstove_number" -> R.string.error_empty_cookstove_number
        "duplicate_cookstove_number" -> R.string.error_duplicate_cookstove_number
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (uiState.createdTaskId != null) {
            LaunchedEffect(uiState.createdTaskId) {
                onTaskCreatedSuccess()
            }
        } else {
            // Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.cookstoveNumber,
                        onValueChange = { viewModel.updateCookstoveNumber(it) },
                        label = { Text(stringResource(R.string.cookstove_number)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = errorMessageRes != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )
                    OutlinedTextField(
                        value = uiState.customerName,
                        onValueChange = { viewModel.updateCustomerName(it) },
                        label = { Text(stringResource(R.string.customer_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (uiState.isLookingUpCustomer) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else if (uiState.isCustomerNameAutoFilled) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        supportingText = if (uiState.isCustomerNameAutoFilled) {
                            { Text(stringResource(R.string.auto_filled_from_database)) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = uiState.collectionDateMillis }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        cal.set(y, m, d)
                                        viewModel.updateCollectionDate(cal.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dateFormat.format(Date(uiState.collectionDateMillis)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.received_product_image),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                        ImagePickerCard(
                            imageUri = uiState.receivedProductImageUri,
                            onTakePhoto = {
                                val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                currentCameraUri = uri
                                receivedImageCamera.launch(uri)
                            },
                            onChooseFromGallery = { receivedImagePicker.launch("image/*") },
                            onClearClick = { viewModel.setReceivedProductImageUri(null) }
                        )

                    Text(
                        text = stringResource(R.string.type_of_process),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CreateTaskTypeOfProcessDropdown(
                        selectedType = uiState.typeOfProcess,
                        onTypeSelected = { viewModel.setTypeOfProcess(it) }
                    )
                }
            }

            if (errorMessageRes != null) {
                Text(
                    text = stringResource(errorMessageRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveTask(onError = { }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (uiState.isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).padding(end = 12.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.save_task),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskTypeOfProcessDropdown(
    selectedType: String?,
    onTypeSelected: (String?) -> Unit
) {
    val options = listOf(
        TYPE_REPAIRING to R.string.type_repairing,
        TYPE_REPLACEMENT to R.string.type_replacement
    )
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedType?.let { type ->
                options.find { it.first == type }?.let { stringResource(it.second) } ?: ""
            } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.type_of_process)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.type_repairing)) },
                onClick = {
                    onTypeSelected(TYPE_REPAIRING)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.type_replacement)) },
                onClick = {
                    onTypeSelected(TYPE_REPLACEMENT)
                    expanded = false
                }
            )
        }
    }
}
