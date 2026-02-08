package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CheckCircle
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.cookstovecare.R
import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.UserRole
import com.example.cookstovecare.ui.theme.SuccessGreen
import com.example.cookstovecare.ui.components.ImagePickerCard
import com.example.cookstovecare.ui.viewmodel.TaskDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Task Detail screen: Shows uploaded image, name, date, and Start Repairing / Replacement options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel,
    userRole: UserRole = UserRole.FIELD_OFFICER,
    onRepairClick: () -> Unit,
    onReplacementClick: () -> Unit,
    onAddReturnClick: (() -> Unit)? = null,
    onAssignTaskClick: (() -> Unit)? = null,
    canEditCompletedReport: Boolean = true,
    onCompleteOrder: ((Uri?) -> Unit)? = null,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    
    // Complete Order modal state
    var showCompleteOrderModal by remember { mutableStateOf(false) }
    var completeOrderImageUri by remember { mutableStateOf<Uri?>(null) }
    val completeOrderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val completeOrderImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { completeOrderImageUri = it } }
    
    // Status timeline bottom sheet state
    var showStatusTimeline by remember { mutableStateOf(false) }
    val statusTimelineSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.task_detail),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            val task = uiState.task
            if (task == null) {
                Text(
                    text = stringResource(R.string.task_not_found),
                    modifier = Modifier.padding(innerPadding).padding(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Single unified task detail card
                    val repairData = uiState.repairData
                    val replacementData = uiState.replacementData
                    val receivedImageUri = when {
                        repairData != null -> repairData.beforeRepairImageUri.takeIf { it.isNotBlank() }
                            ?: task.receivedProductImageUri
                        replacementData != null -> replacementData.oldCookstoveImageUri
                        task.receivedProductImageUri != null -> task.receivedProductImageUri
                        else -> null
                    }
                    val completionImageUri = when {
                        repairData != null -> repairData.afterRepairImageUri
                        replacementData != null -> replacementData.newCookstoveImageUri
                        else -> null
                    }
                    val completionDate = when {
                        repairData != null -> repairData.repairCompletionDate
                        replacementData != null -> replacementData.replacementDate
                        else -> null
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val isTechnician = userRole == UserRole.TECHNICIAN
                            val isCompleted = task.statusEnum == TaskStatus.REPAIR_COMPLETED || task.statusEnum == TaskStatus.REPLACEMENT_COMPLETED

                            if (isTechnician) {
                                // Technician view: simplified layout by phase
                                if (isCompleted) {
                                    // Completed: Received image, Cookstove number, Task type, Type of repairing, Repair completion date, Image
                                    TaskDetailImageRow(
                                        label = stringResource(R.string.received_image),
                                        imageUri = receivedImageUri
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OfficeDataItem(
                                            icon = Icons.Default.Tag,
                                            label = stringResource(R.string.cookstove_number),
                                            value = task.cookstoveNumber
                                        )
                                        task.typeOfProcess?.let { type ->
                                            OfficeDataItem(
                                                icon = Icons.Default.Build,
                                                label = stringResource(R.string.task_type),
                                                value = when (type) {
                                                    "REPAIRING" -> stringResource(R.string.type_repairing)
                                                    "REPLACEMENT" -> stringResource(R.string.type_replacement)
                                                    else -> type
                                                }
                                            )
                                        }
                                    }
                                    if (repairData != null) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                        TaskDetailTypeOfRepairCard(
                                            label = stringResource(R.string.type_of_repair),
                                            selectedTypes = repairData.typesOfRepair.toSet()
                                        )
                                    }
                                    completionDate?.let { millis ->
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                        OfficeDataItem(
                                            icon = Icons.Default.CalendarToday,
                                            label = if (repairData != null) stringResource(R.string.repair_completion_date)
                                            else stringResource(R.string.replacement_date),
                                            value = dateFormat.format(Date(millis))
                                        )
                                    }
                                    // Only show after repair image if it exists and is not blank
                                    completionImageUri?.takeIf { it.isNotBlank() }?.let { uri ->
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TaskDetailImageRow(
                                            label = stringResource(R.string.after_repair_image_label),
                                            imageUri = uri
                                        )
                                    }
                                } else {
                                    // Assigned / To Do / Active: Received image, Number, Date, Task type
                                    TaskDetailImageRow(
                                        label = stringResource(R.string.received_image),
                                        imageUri = receivedImageUri
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OfficeDataItem(
                                            icon = Icons.Default.Tag,
                                            label = stringResource(R.string.cookstove_number),
                                            value = task.cookstoveNumber
                                        )
                                        OfficeDataItem(
                                            icon = Icons.Default.CalendarToday,
                                            label = stringResource(R.string.collection_date),
                                            value = dateFormat.format(Date(task.collectionDate))
                                        )
                                        task.typeOfProcess?.let { type ->
                                            OfficeDataItem(
                                                icon = Icons.Default.Build,
                                                label = stringResource(R.string.task_type),
                                                value = when (type) {
                                                    "REPAIRING" -> stringResource(R.string.type_repairing)
                                                    "REPLACEMENT" -> stringResource(R.string.type_replacement)
                                                    else -> type
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Non-technician: full layout (original)
                                TaskDetailImageRow(
                                    label = stringResource(R.string.received_image),
                                    imageUri = receivedImageUri
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OfficeDataItem(
                                        icon = Icons.Default.Tag,
                                        label = stringResource(R.string.cookstove_number),
                                        value = task.cookstoveNumber
                                    )
                                    task.customerName?.let { name ->
                                        OfficeDataItem(
                                            icon = Icons.Default.Person,
                                            label = stringResource(R.string.customer),
                                            value = name
                                        )
                                    }
                                    task.typeOfProcess?.let { type ->
                                        OfficeDataItem(
                                            icon = Icons.Default.Build,
                                            label = stringResource(R.string.task_type),
                                            value = when (type) {
                                                "REPAIRING" -> stringResource(R.string.type_repairing)
                                                "REPLACEMENT" -> stringResource(R.string.type_replacement)
                                                else -> type
                                            }
                                        )
                                    }
                                    OfficeDataItem(
                                        icon = Icons.Default.CalendarToday,
                                        label = stringResource(R.string.collection_date),
                                        value = dateFormat.format(Date(task.collectionDate))
                                    )
                                    OfficeDataItem(
                                        icon = Icons.AutoMirrored.Filled.Assignment,
                                        label = stringResource(R.string.task_status),
                                        value = when (task.statusEnum) {
                                            TaskStatus.COLLECTED -> stringResource(R.string.status_collected)
                                            TaskStatus.ASSIGNED -> stringResource(R.string.status_assigned)
                                            TaskStatus.IN_PROGRESS -> stringResource(R.string.status_processing)
                                            TaskStatus.REPAIR_COMPLETED -> stringResource(R.string.status_repair_completed)
                                            TaskStatus.REPLACEMENT_COMPLETED -> stringResource(R.string.status_replacement_completed)
                                            TaskStatus.DISTRIBUTED -> stringResource(R.string.status_distributed)
                                        },
                                        isStatus = true,
                                        statusEnum = task.statusEnum,
                                        onClick = { showStatusTimeline = true }
                                    )
                                }
                                if (repairData != null) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    TaskDetailTypeOfRepairCard(
                                        label = stringResource(R.string.type_of_repair),
                                        selectedTypes = repairData.typesOfRepair.toSet()
                                    )
                                }
                                if (completionImageUri != null || completionDate != null || (replacementData?.collectedDate != null) || task.returnDate != null) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    replacementData?.collectedDate?.let { millis ->
                                        OfficeDataItem(
                                            icon = Icons.Default.CalendarToday,
                                            label = stringResource(R.string.collection_date),
                                            value = dateFormat.format(Date(millis))
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    // Distribution date and image (for Field Officer after completing order)
                                    task.distributionDate?.let { millis ->
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OfficeDataItem(
                                            icon = Icons.Default.CalendarToday,
                                            label = stringResource(R.string.distribution_date),
                                            value = dateFormat.format(Date(millis))
                                        )
                                    }
                                    task.distributionImageUri?.let { uri ->
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TaskDetailImageRow(
                                            label = stringResource(R.string.distribution_image),
                                            imageUri = uri
                                        )
                                    }
                                    
                                    task.returnDate?.let { millis ->
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OfficeDataItem(
                                            icon = Icons.Default.CalendarToday,
                                            label = stringResource(R.string.return_date),
                                            value = dateFormat.format(Date(millis))
                                        )
                                    }
                                    task.returnImageUri?.let { uri ->
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TaskDetailImageRow(
                                            label = stringResource(R.string.return_image),
                                            imageUri = uri
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Supervisor: Assign button when unassigned, or "Assigned to [name]" when assigned
                    if (onAssignTaskClick != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (task.assignedToTechnicianId == null) {
                            Button(
                                onClick = onAssignTaskClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(stringResource(R.string.assign_to_technician))
                            }
                        } else {
                            val assignedName = uiState.assignedTechnicianName ?: stringResource(R.string.technician_name)
                            val canChangeTechnician = task.statusEnum == TaskStatus.ASSIGNED
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessGreen.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Assignment,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp),
                                        tint = SuccessGreen
                                    )
                                    Text(
                                        text = stringResource(R.string.assigned_to_tech, assignedName),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (canChangeTechnician) {
                                        TextButton(
                                            onClick = onAssignTaskClick
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.change))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Complete form button (when collected) - Technician only (not Field Officer)
                    if (onAssignTaskClick == null && viewModel.canProceedToRepairOrReplacement && userRole == UserRole.TECHNICIAN) {
                        Spacer(modifier = Modifier.height(8.dp))
                        when (task.typeOfProcess) {
                            "REPAIRING" -> {
                                Button(
                                    onClick = onRepairClick,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text(stringResource(R.string.complete_repairing))
                                }
                            }
                            "REPLACEMENT" -> {
                                Button(
                                    onClick = onReplacementClick,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text(stringResource(R.string.complete_replacement))
                                }
                            }
                            else -> {
                                Button(
                                    onClick = onRepairClick,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text(stringResource(R.string.complete_repairing))
                                }
                                Button(
                                    onClick = onReplacementClick,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text(stringResource(R.string.complete_replacement))
                                }
                            }
                        }
                    }

                    // Complete Order button (for Field Officer when task is completed by technician and not yet distributed)
                    if (userRole == UserRole.FIELD_OFFICER && (repairData != null || replacementData != null) && task.distributionDate == null && onCompleteOrder != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showCompleteOrderModal = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.complete_order))
                        }
                    }
                    
                    // Add Return (when completed and return not yet added - for Supervisor only)
                    if (userRole == UserRole.SUPERVISOR && onAssignTaskClick == null && (repairData != null || replacementData != null) && task.returnDate == null && onAddReturnClick != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onAddReturnClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Text(stringResource(R.string.add_return))
                        }
                    }

                    // Edit submitted report (when completed) - Supervisor only; Field Officer and Technician cannot edit
                    if (userRole == UserRole.SUPERVISOR && onAssignTaskClick == null && canEditCompletedReport && (repairData != null || replacementData != null)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = when {
                                repairData != null -> onRepairClick
                                else -> onReplacementClick
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.edit_submitted_report))
                        }
                    }
                }
            }
        }
    }
    
    // Complete Order Modal
    if (showCompleteOrderModal) {
        ModalBottomSheet(
            onDismissRequest = { 
                showCompleteOrderModal = false
                completeOrderImageUri = null
            },
            sheetState = completeOrderSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.complete_order_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Image picker
                Text(
                    text = stringResource(R.string.complete_order_image),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                ImagePickerCard(
                    imageUri = completeOrderImageUri?.toString(),
                    onTakePhoto = { /* Camera not needed for now */ },
                    onChooseFromGallery = { completeOrderImagePicker.launch("image/*") }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        onCompleteOrder?.invoke(completeOrderImageUri)
                        showCompleteOrderModal = false
                        completeOrderImageUri = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.complete_order))
                }
            }
        }
    }
    
    // Status Timeline Modal
    if (showStatusTimeline) {
        val task = uiState.task
        if (task != null) {
            ModalBottomSheet(
                onDismissRequest = { showStatusTimeline = false },
                sheetState = statusTimelineSheetState
            ) {
                StatusTimelineContent(
                    currentStatus = task.statusEnum,
                    collectionDate = task.collectionDate,
                    isAssigned = task.assignedToTechnicianId != null,
                    workStartedAt = task.workStartedAt,
                    completedAt = task.completedAt,
                    distributionDate = task.distributionDate
                )
            }
        }
    }
}

private val TYPE_OF_REPAIR_OPTIONS = listOf(
    "TOP_PLACE" to R.string.type_repair_top_place,
    "DOOR_REPAIR" to R.string.type_repair_door_repair,
    "BOTTOM_REPAIR" to R.string.type_repair_bottom_repair
)

@Composable
private fun TaskDetailTypeOfRepairCard(
    label: String,
    selectedTypes: Set<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                            onCheckedChange = { },
                            enabled = false
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
}

@Composable
private fun TaskDetailImageRow(label: String, imageUri: String?) {
    var showFullImage by remember { mutableStateOf(false) }
    val hasImage = imageUri != null && imageUri.isNotBlank()

    if (showFullImage && hasImage) {
        Dialog(
            onDismissRequest = { showFullImage = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showFullImage = false }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .then(
                    if (hasImage) Modifier.clickable { showFullImage = true } else Modifier
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            if (hasImage) {
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
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ImageNotSupported,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = stringResource(R.string.no_image),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficeDataItem(
    icon: ImageVector,
    label: String,
    value: String,
    isStatus: Boolean = false,
    statusEnum: TaskStatus? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (isStatus && statusEnum != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (statusEnum) {
                            TaskStatus.COLLECTED -> MaterialTheme.colorScheme.primaryContainer
                            else -> SuccessGreen.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = when (statusEnum) {
                                TaskStatus.COLLECTED -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> SuccessGreen
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            // Arrow indicator for clickable items
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Flipkart-style status timeline content */
@Composable
private fun StatusTimelineContent(
    currentStatus: TaskStatus,
    collectionDate: Long,
    isAssigned: Boolean,
    workStartedAt: Long?,
    completedAt: Long?,
    distributionDate: Long?
) {
    val dateTimeFormat = SimpleDateFormat("EEE, d MMM ''yy - h:mma", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, d MMM ''yy", Locale.getDefault())
    
    // Define status steps
    val steps = listOf(
        TimelineStep(
            title = stringResource(R.string.timeline_order_created),
            isCompleted = true, // Always completed if task exists
            date = dateFormat.format(Date(collectionDate)),
            description = stringResource(R.string.timeline_order_collected)
        ),
        TimelineStep(
            title = stringResource(R.string.status_assigned),
            isCompleted = currentStatus.ordinal >= TaskStatus.ASSIGNED.ordinal,
            date = if (isAssigned) stringResource(R.string.timeline_technician_assigned) else null,
            description = if (isAssigned) stringResource(R.string.timeline_assigned_desc) else null
        ),
        TimelineStep(
            title = stringResource(R.string.status_processing),
            isCompleted = currentStatus.ordinal >= TaskStatus.IN_PROGRESS.ordinal,
            date = workStartedAt?.let { dateTimeFormat.format(Date(it)) },
            description = if (workStartedAt != null) stringResource(R.string.timeline_processing_desc) else null
        ),
        TimelineStep(
            title = stringResource(R.string.status_repaired),
            isCompleted = currentStatus.ordinal >= TaskStatus.REPAIR_COMPLETED.ordinal || 
                         currentStatus == TaskStatus.REPLACEMENT_COMPLETED,
            date = completedAt?.let { dateTimeFormat.format(Date(it)) },
            description = if (completedAt != null) stringResource(R.string.timeline_repaired_desc) else null
        ),
        TimelineStep(
            title = stringResource(R.string.status_distributed),
            isCompleted = currentStatus == TaskStatus.DISTRIBUTED,
            date = distributionDate?.let { dateTimeFormat.format(Date(it)) },
            description = if (distributionDate != null) stringResource(R.string.timeline_distributed_desc) else null
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.order_tracking),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        steps.forEachIndexed { index, step ->
            TimelineStepRow(
                step = step,
                isLast = index == steps.lastIndex
            )
        }
    }
}

private data class TimelineStep(
    val title: String,
    val isCompleted: Boolean,
    val date: String?,
    val description: String?
)

@Composable
private fun TimelineStepRow(
    step: TimelineStep,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline indicator column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Circle indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (step.isCompleted) SuccessGreen else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (step.isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
            
            // Vertical line (except for last item)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(if (step.description != null) 80.dp else 40.dp)
                        .background(
                            if (step.isCompleted) SuccessGreen else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Content column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            // Title with date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (step.isCompleted) FontWeight.Bold else FontWeight.Normal,
                    color = if (step.isCompleted) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                if (step.date != null && step.isCompleted) {
                    Text(
                        text = step.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Description
            if (step.description != null && step.isCompleted) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

