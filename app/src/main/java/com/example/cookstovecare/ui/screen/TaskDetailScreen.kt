package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.cookstovecare.R
import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.ui.theme.SuccessGreen
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
    onRepairClick: () -> Unit,
    onReplacementClick: () -> Unit,
    onAddReturnClick: (() -> Unit)? = null,
    onAssignTaskClick: (() -> Unit)? = null,
    canEditCompletedReport: Boolean = true,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

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
                        repairData != null -> repairData.beforeRepairImageUri
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
                            // Received image
                            TaskDetailImageRow(
                                label = stringResource(R.string.received_image),
                                imageUri = receivedImageUri
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            // Office data — modern info grid
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
                                    },
                                    isStatus = true,
                                    statusEnum = task.statusEnum
                                )
                            }

                            // When completed: add type of repair done (for repair tasks), completion date, and image
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
                                completionDate?.let { millis ->
                                    OfficeDataItem(
                                        icon = Icons.Default.CalendarToday,
                                        label = if (repairData != null) stringResource(R.string.repair_completion_date)
                                        else stringResource(R.string.replacement_date),
                                        value = dateFormat.format(Date(millis))
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                completionImageUri?.let { uri ->
                                    TaskDetailImageRow(
                                        label = stringResource(R.string.completion_image),
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

                    // Supervisor: Assign to Technician (when collected, before technician starts)
                    if (onAssignTaskClick != null && task.statusEnum == TaskStatus.COLLECTED) {
                        Spacer(modifier = Modifier.height(8.dp))
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
                    }

                    // Complete form button (when collected) - Technician/Field Officer only
                    if (onAssignTaskClick == null && viewModel.canProceedToRepairOrReplacement) {
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

                    // Add Return (when completed and return not yet added - for Field Officer only)
                    if (onAssignTaskClick == null && (repairData != null || replacementData != null) && task.returnDate == null && onAddReturnClick != null) {
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

                    // Edit submitted report (when completed) - Field Officer only; Technician cannot edit
                    if (onAssignTaskClick == null && canEditCompletedReport && (repairData != null || replacementData != null)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
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
    statusEnum: TaskStatus? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
        }
    }
}

