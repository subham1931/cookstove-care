package com.example.cookstovecare.ui.screen

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.cookstovecare.R
import com.example.cookstovecare.data.local.FieldOfficerInfo
import com.example.cookstovecare.ui.components.ImagePickerCard
import com.example.cookstovecare.ui.viewmodel.CreateTaskViewModel
import java.io.File
import java.io.FileOutputStream

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
    modifier: Modifier = Modifier,
    isFieldCoordinator: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Load field officers when opened as Field Coordinator
    if (isFieldCoordinator) {
        LaunchedEffect(Unit) {
            viewModel.loadFieldOfficers()
        }
    }

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
                    OutlinedTextField(
                        value = uiState.deliveryAddress,
                        onValueChange = { viewModel.updateDeliveryAddress(it) },
                        label = { Text(stringResource(R.string.delivery_address)) },
                        placeholder = { Text(stringResource(R.string.delivery_address_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (uiState.isAddressAutoFilled) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        supportingText = if (uiState.isAddressAutoFilled) {
                            { Text(stringResource(R.string.auto_filled_from_database)) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    OutlinedTextField(
                        value = uiState.temporaryCookstoveNumber,
                        onValueChange = { viewModel.updateTemporaryCookstoveNumber(it) },
                        label = { Text("Temporary Cookstove Number") },
                        placeholder = { Text("Enter if provided") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
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

                    // Field Officer selector - only for Field Coordinators
                    if (isFieldCoordinator) {
                        Text(
                            text = "Assign Field Officer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FieldOfficerDropdown(
                            fieldOfficers = uiState.fieldOfficers,
                            selectedPhoneNumber = uiState.selectedFieldOfficerId,
                            onSelected = { viewModel.setSelectedFieldOfficer(it) }
                        )
                    }
                }
            }

            // Acknowledgement & Signature Section
            var isAcknowledged by remember { mutableStateOf(false) }
            val signaturePaths = remember { mutableStateListOf<List<Offset>>() }

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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Acknowledgement",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = isAcknowledged,
                            onCheckedChange = { isAcknowledged = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "I hereby confirm that I have received a temporary cookstove provided as a replacement for my previous cookstove, which will remain in use until the original cookstove is repaired or replaced.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )

                    Text(
                        text = "Customer Signature",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SignaturePad(
                        paths = signaturePaths,
                        onNewPath = { path -> signaturePaths.add(path) },
                        onClear = {
                            signaturePaths.clear()
                            viewModel.setCustomerSignatureUri(null)
                        }
                    )

                    // Save signature when paths change
                    if (signaturePaths.isNotEmpty()) {
                        LaunchedEffect(signaturePaths.size) {
                            val file = File(context.cacheDir, "signature_${System.currentTimeMillis()}.png")
                            saveSignatureBitmap(signaturePaths, file)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            viewModel.setCustomerSignatureUri(uri.toString())
                        }
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldOfficerDropdown(
    fieldOfficers: List<FieldOfficerInfo>,
    selectedPhoneNumber: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOfficer = fieldOfficers.find { it.phoneNumber == selectedPhoneNumber }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOfficer?.displayName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Field Officer") },
            placeholder = { Text("Select Field Officer") },
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
            if (fieldOfficers.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "No field officers available",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                fieldOfficers.forEach { officer ->
                    val isSelected = officer.phoneNumber == selectedPhoneNumber
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = officer.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (officer.name != null) {
                                    Text(
                                        text = officer.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelected(officer.phoneNumber)
                            expanded = false
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SignaturePad(
    paths: List<List<Offset>>,
    onNewPath: (List<Offset>) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = listOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath = currentPath + change.position
                        },
                        onDragEnd = {
                            if (currentPath.size > 1) {
                                onNewPath(currentPath)
                            }
                            currentPath = emptyList()
                        },
                        onDragCancel = {
                            currentPath = emptyList()
                        }
                    )
                }
        ) {
            val strokeColor = MaterialTheme.colorScheme.onSurface

            Canvas(modifier = Modifier.fillMaxSize()) {
                val allPaths = if (currentPath.isNotEmpty()) paths + listOf(currentPath) else paths
                allPaths.forEach { points ->
                    if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                quadraticBezierTo(
                                    prev.x, prev.y,
                                    (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f
                                )
                            }
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = 4f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            if (paths.isEmpty() && currentPath.isEmpty()) {
                Text(
                    text = "Sign here",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        if (paths.isNotEmpty()) {
            TextButton(
                onClick = onClear,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text("Clear Signature")
            }
        }
    }
}

/** Render signature paths to a bitmap and save as PNG file */
private fun saveSignatureBitmap(paths: List<List<Offset>>, file: File) {
    val width = 800
    val height = 400
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 4f
        style = android.graphics.Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }

    // Scale factor from screen coordinates to bitmap
    val scaleX = width.toFloat() / 1080f
    val scaleY = height.toFloat() / 600f

    paths.forEach { points ->
        if (points.size > 1) {
            val path = android.graphics.Path()
            path.moveTo(points[0].x * scaleX, points[0].y * scaleY)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                path.quadTo(
                    prev.x * scaleX, prev.y * scaleY,
                    (prev.x + curr.x) / 2f * scaleX, (prev.y + curr.y) / 2f * scaleY
                )
            }
            canvas.drawPath(path, paint)
        }
    }

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    bitmap.recycle()
}
