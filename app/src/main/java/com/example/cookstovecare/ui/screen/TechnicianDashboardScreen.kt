package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.cookstovecare.ui.viewmodel.RepairFormViewModelFactory
import com.example.cookstovecare.ui.viewmodel.ReplacementFormViewModelFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cookstovecare.R
import com.example.cookstovecare.ui.theme.AuthGradientStart
import com.example.cookstovecare.ui.theme.AuthGradientStartDark
import com.example.cookstovecare.ui.theme.SuccessGreen
import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.entity.Technician
import com.example.cookstovecare.data.local.AuthDataStore
import com.example.cookstovecare.data.repository.CookstoveRepository
import com.example.cookstovecare.ui.viewmodel.TechnicianViewModel
import com.example.cookstovecare.ui.viewmodel.TechnicianViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Status filter for technician tasks */
private enum class TechnicianFilter(val statuses: List<TaskStatus>) {
    NEW_TASK(listOf(TaskStatus.ASSIGNED)),
    ACTIVE(listOf(TaskStatus.IN_PROGRESS)),
    COMPLETED(listOf(TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED, TaskStatus.DISTRIBUTED))
}

/** Bottom navigation tabs */
private enum class TechnicianBottomTab(val titleRes: Int) {
    TASKS(R.string.nav_tasks),
    ORDERS(R.string.filter_orders),
    PROFILE(R.string.nav_profile)
}

/**
 * Technician Task Board - Status filter chips and task cards with status tags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianDashboardScreen(
    viewModel: TechnicianViewModel,
    repository: CookstoveRepository,
    authDataStore: AuthDataStore,
    technicianId: Long,
    onTaskClick: (Long) -> Unit,
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit,
    onClearAllData: (() -> Unit)? = null
) {
    val assignedTasks by viewModel.assignedTasks.collectAsState(initial = emptyList())
    val technician by viewModel.technicianDetails.collectAsState(initial = null)
    val phoneNumber by authDataStore.phoneNumber.collectAsState(initial = "")
    val profileImageUri by authDataStore.profileImageUri.collectAsState(initial = null)
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    var selectedBottomTab by rememberSaveable { mutableStateOf(TechnicianBottomTab.TASKS) }
    
    // Modal state for repair/replacement forms
    var repairTaskId by remember { mutableStateOf<Long?>(null) }
    var replacementTaskId by remember { mutableStateOf<Long?>(null) }
    val repairSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val replacementSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val selectedFilter = TechnicianFilter.entries[selectedTabIndex]
    val filteredTasks = assignedTasks.filter { task ->
        task.statusEnum in selectedFilter.statuses
    }

    val displayName = technician?.name?.takeIf { it.isNotBlank() } ?: phoneNumber.ifBlank { stringResource(R.string.nav_profile) }

    Scaffold(
        topBar = {
            /* Profile screen provides its own header, Tasks has greeting in content */
        },
        bottomBar = {
            NavigationBar {
                TechnicianBottomTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedBottomTab == tab,
                        onClick = { selectedBottomTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    TechnicianBottomTab.TASKS -> Icons.Default.Home
                                    TechnicianBottomTab.ORDERS -> Icons.Default.Assignment
                                    TechnicianBottomTab.PROFILE -> Icons.Default.Person
                                },
                                contentDescription = stringResource(tab.titleRes)
                            )
                        },
                        label = { Text(stringResource(tab.titleRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedBottomTab) {
            TechnicianBottomTab.TASKS -> {
                val todoCount = assignedTasks.count { it.statusEnum == TaskStatus.ASSIGNED }
                val inProgressCount = assignedTasks.count { it.statusEnum == TaskStatus.IN_PROGRESS }
                val doneCount = assignedTasks.count {
                    it.statusEnum == TaskStatus.REPAIR_COMPLETED || 
                    it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED ||
                    it.statusEnum == TaskStatus.DISTRIBUTED
                }
                TechnicianTaskScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    greetingText = getGreetingText(),
                    technicianName = displayName,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    filteredTasks = filteredTasks,
                    todoCount = todoCount,
                    inProgressCount = inProgressCount,
                    doneCount = doneCount,
                    onTaskClick = onTaskClick,
                    onStart = { viewModel.moveToInProgress(it) },
                    onComplete = { task ->
                        // Show the form in a modal
                        when (task.typeOfProcess) {
                            "REPLACEMENT" -> replacementTaskId = task.id
                            else -> repairTaskId = task.id
                        }
                    }
                )
            }
            TechnicianBottomTab.ORDERS -> {
                TechnicianOrdersContent(
                    tasks = assignedTasks,
                    onTaskClick = onTaskClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            TechnicianBottomTab.PROFILE -> {
                val assignedCount = assignedTasks.count { it.statusEnum == TaskStatus.ASSIGNED }
                val inProgressCount = assignedTasks.count { it.statusEnum == TaskStatus.IN_PROGRESS }
                val completedCount = assignedTasks.count {
                    it.statusEnum == TaskStatus.REPAIR_COMPLETED || 
                    it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED ||
                    it.statusEnum == TaskStatus.DISTRIBUTED
                }
                ProfileScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    displayName = technician?.name?.takeIf { it.isNotBlank() } ?: phoneNumber.ifBlank { stringResource(R.string.nav_profile) },
                    displayPhone = technician?.phoneNumber?.takeIf { it.isNotBlank() } ?: phoneNumber,
                    role = com.example.cookstovecare.data.UserRole.TECHNICIAN,
                    profileImageUri = profileImageUri,
                    onEditProfile = onEditProfile,
                    id = if (technicianId > 0) technicianId.toString() else null,
                    status = technician?.let { if (it.isActive) stringResource(R.string.active) else stringResource(R.string.inactive) },
                    tasksAssigned = assignedCount,
                    inProgress = inProgressCount,
                    completed = completedCount,
                    showWorkSummary = false,
                    onLogout = onLogout,
                    onClearAllData = onClearAllData
                )
            }
        }
    }
    
    // Repair Form Modal
    if (repairTaskId != null) {
        val repairViewModel: com.example.cookstovecare.ui.viewmodel.RepairFormViewModel = viewModel(
            factory = RepairFormViewModelFactory(repository, repairTaskId!!),
            key = "repair_$repairTaskId"
        )
        ModalBottomSheet(
            onDismissRequest = { repairTaskId = null },
            sheetState = repairSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.repair_form_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                RepairFormContent(
                    viewModel = repairViewModel,
                    onSuccess = { repairTaskId = null }
                )
            }
        }
    }
    
    // Replacement Form Modal
    if (replacementTaskId != null) {
        val replacementViewModel: com.example.cookstovecare.ui.viewmodel.ReplacementFormViewModel = viewModel(
            factory = ReplacementFormViewModelFactory(repository, replacementTaskId!!),
            key = "replacement_$replacementTaskId"
        )
        ModalBottomSheet(
            onDismissRequest = { replacementTaskId = null },
            sheetState = replacementSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.replacement_form_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                ReplacementFormContent(
                    viewModel = replacementViewModel,
                    onSuccess = { replacementTaskId = null }
                )
            }
        }
    }
}

/** Returns greeting string based on time of day. */
@Composable
private fun getGreetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> stringResource(R.string.greeting_good_morning)
        hour in 12..16 -> stringResource(R.string.greeting_good_afternoon)
        hour in 17..21 -> stringResource(R.string.greeting_good_evening)
        else -> stringResource(R.string.greeting_welcome)
    }
}

/**
 * Technician Task screen: header, segmented tabs, task list.
 * Uses existing ViewModel state for filtering.
 */
@Composable
private fun TechnicianTaskScreen(
    modifier: Modifier = Modifier,
    greetingText: String,
    technicianName: String,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    filteredTasks: List<CookstoveTask>,
    todoCount: Int,
    inProgressCount: Int,
    doneCount: Int,
    onTaskClick: (Long) -> Unit,
    onStart: (Long) -> Unit,
    onComplete: (CookstoveTask) -> Unit
) {
    Column(modifier = modifier) {
        GreetingHeader(
            greetingText = greetingText,
            technicianName = technicianName
        )
        TaskStatusTabs(
            selectedIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            todoCount = todoCount,
            inProgressCount = inProgressCount,
            doneCount = doneCount
        )
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyTaskState()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TechnicianTaskCard(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        onStart = { onStart(task.id) },
                        onComplete = { onComplete(task) }
                    )
                }
            }
        }
    }
}

/** Purple styled header with greeting, name, and subtitle. */
@Composable
private fun GreetingHeader(
    greetingText: String,
    technicianName: String
) {
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    bottomStart = 32.dp,
                    bottomEnd = 32.dp
                )
            )
            .background(headerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = greetingText,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = technicianName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.lets_tackle_tasks),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

/** Segmented-control style tabs. Dark rounded container with purple selected state. */
@Composable
private fun TaskStatusTabs(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    todoCount: Int,
    inProgressCount: Int,
    doneCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = androidx.compose.ui.graphics.Color(0xFF2A2A2A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            TechnicianFilter.entries.forEachIndexed { index, filter ->
                val isSelected = selectedIndex == index
                val (label, count) = when (filter) {
                    TechnicianFilter.NEW_TASK -> stringResource(R.string.filter_to_do) to todoCount
                    TechnicianFilter.ACTIVE -> stringResource(R.string.in_progress_tasks) to inProgressCount
                    TechnicianFilter.COMPLETED -> stringResource(R.string.filter_tab_done) to doneCount
                }
                Surface(
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    onClick = { onTabSelected(index) }
                ) {
                    Text(
                        text = "$label $count",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                        }
                    )
                }
            }
        }
    }
}

// ========== UI REFACTOR: Technician Task Card ==========
// Redesigned card: Top row (cookstove + status chip), Middle row (task type + date), Bottom row (primary action).
// Material 3 compliant, 16dp corners, no dividers, completed cards de-emphasized.

/** Main task card composable. Layout: cookstove + status | task type + date | primary action button. */
@Composable
private fun TechnicianTaskCard(
    task: CookstoveTask,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val isCompleted = task.statusEnum == TaskStatus.REPAIR_COMPLETED || 
        task.statusEnum == TaskStatus.REPLACEMENT_COMPLETED ||
        task.statusEnum == TaskStatus.DISTRIBUTED
    
    // Determine status text for technician view
    val statusText = when (task.statusEnum) {
        TaskStatus.ASSIGNED -> stringResource(R.string.status_assigned)
        TaskStatus.IN_PROGRESS -> stringResource(R.string.status_processing)
        TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED, TaskStatus.DISTRIBUTED -> 
            stringResource(R.string.status_completed)
        else -> stringResource(R.string.status_assigned)
    }
    
    val statusColor = when (task.statusEnum) {
        TaskStatus.ASSIGNED -> MaterialTheme.colorScheme.primary
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        else -> SuccessGreen
    }

    val isDark = isSystemInDarkTheme()
    val primaryColor = if (isDark) AuthGradientStartDark else AuthGradientStart

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Product image
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.receivedProductImageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(task.receivedProductImageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Middle: Cookstove number
                Text(
                    text = task.cookstoveNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // Right side: Status and date
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Status text
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                    // Show completed date for completed tasks
                    if (isCompleted && task.completedAt != null) {
                        Text(
                            text = dateFormat.format(Date(task.completedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            
            // Action buttons for non-completed tasks
            if (!isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                when (task.statusEnum) {
                    TaskStatus.ASSIGNED -> {
                        Button(
                            onClick = onStart,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.start_task),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    TaskStatus.IN_PROGRESS -> {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.complete_task),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

/** Status chip: Assigned / In Progress / Completed. Uses Material color scheme. */
@Composable
private fun TaskStatusChip(status: TaskStatus) {
    val (labelRes, containerColor, contentColor) = when (status) {
        TaskStatus.ASSIGNED -> Triple(
            R.string.status_assigned,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        TaskStatus.IN_PROGRESS -> Triple(
            R.string.in_progress_tasks,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED -> Triple(
            R.string.completed_tasks,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        TaskStatus.COLLECTED -> Triple(
            R.string.status_assigned,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        TaskStatus.DISTRIBUTED -> Triple(
            R.string.status_distributed,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Task type chip: Repair / Replacement. */
@Composable
private fun TaskTypeChip(typeOfProcess: String?) {
    val (labelRes, containerColor, contentColor) = when (typeOfProcess) {
        "REPLACEMENT" -> Triple(
            R.string.type_replacement,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> Triple(
            R.string.type_repairing,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Primary action: Assigned→Start, In Progress→Continue, Completed→View. Filled for active, tonal for completed. */
@Composable
private fun TaskPrimaryActionButton(
    status: TaskStatus,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onView: () -> Unit
) {
    when (status) {
        TaskStatus.ASSIGNED -> {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.start))
            }
        }
        TaskStatus.IN_PROGRESS -> {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.complete))
            }
        }
        TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED -> {
            FilledTonalButton(
                onClick = onView,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.view))
            }
        }
        else -> { /* COLLECTED: no primary action for technician */ }
    }
}

/** Empty state: icon, title, subtitle. Centered, muted colors. */
@Composable
private fun EmptyTaskState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AssignmentTurnedIn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = stringResource(R.string.empty_tasks_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.empty_tasks_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

/** Technician Orders Content - date-based view of completed orders */
@Composable
private fun TechnicianOrdersContent(
    tasks: List<CookstoveTask>,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart

    val todayCalendar = Calendar.getInstance()
    val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)
    val todayMonth = todayCalendar.get(Calendar.MONTH)
    val todayYear = todayCalendar.get(Calendar.YEAR)

    var selectedMonth by remember { mutableStateOf(todayMonth) }
    var selectedYear by remember { mutableStateOf(todayYear) }
    var selectedDay by remember { mutableStateOf(todayDay) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    // Only show tasks the technician has completed (not delivered - that's done by others)
    val completedTasks = tasks.filter {
        it.statusEnum == TaskStatus.REPAIR_COMPLETED ||
        it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED
    }

    val selectedCalendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, selectedMonth)
        set(Calendar.YEAR, selectedYear)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }

    // Filter by date
    val filteredTasks = completedTasks.filter { task ->
        val taskCalendar = Calendar.getInstance().apply {
            timeInMillis = task.completedAt ?: task.createdAt
        }
        taskCalendar.get(Calendar.DAY_OF_MONTH) == selectedDay &&
            taskCalendar.get(Calendar.MONTH) == selectedMonth &&
            taskCalendar.get(Calendar.YEAR) == selectedYear
    }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val years = (todayYear - 5..todayYear + 1).toList()
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(modifier = modifier) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(headerColor)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.filter_orders),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Month & Year pickers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month picker
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { showMonthPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthNames[selectedMonth],
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = showMonthPicker,
                    onDismissRequest = { showMonthPicker = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    monthNames.forEachIndexed { index, month ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    month,
                                    fontWeight = if (index == selectedMonth) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedMonth = index
                                showMonthPicker = false
                            }
                        )
                    }
                }
            }

            // Year picker
            Box {
                Surface(
                    onClick = { showYearPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$selectedYear",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = showYearPicker,
                    onDismissRequest = { showYearPicker = false }
                ) {
                    years.forEach { year ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "$year",
                                    fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedYear = year
                                showYearPicker = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Day selector (horizontal scroll)
        val dateListState = rememberLazyListState()

        LaunchedEffect(selectedDay, selectedMonth, selectedYear) {
            val targetIndex = (selectedDay - 1).coerceIn(0, daysInMonth - 1)
            val scrollIndex = (targetIndex - 5).coerceAtLeast(0)
            dateListState.animateScrollToItem(scrollIndex)
        }

        LazyRow(
            state = dateListState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                val dayOfWeek = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
                val isSelected = day == selectedDay
                val isToday = day == todayDay && selectedMonth == todayMonth && selectedYear == todayYear

                Surface(
                    onClick = { selectedDay = day },
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isSelected -> headerColor
                        else -> Color.Transparent
                    },
                    modifier = Modifier.width(52.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = dayOfWeek,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White
                                else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Orders count
        Text(
            text = "${filteredTasks.size} completed orders",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Orders list
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No completed orders on this date",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TechnicianOrderCard(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

/** Order card for technician orders view */
@Composable
private fun TechnicianOrderCard(
    task: CookstoveTask,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val processText = task.typeOfProcess?.let { type ->
        when (type) {
            "REPAIRING" -> stringResource(R.string.type_repairing)
            "REPLACEMENT" -> stringResource(R.string.type_replacement)
            else -> type
        }
    }
    val statusText = when (task.statusEnum) {
        TaskStatus.REPAIR_COMPLETED -> "Repaired"
        TaskStatus.REPLACEMENT_COMPLETED -> "Replaced"
        else -> ""
    }
    val statusColor = SuccessGreen

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (task.receivedProductImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(task.receivedProductImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.cookstoveNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (processText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = processText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Status badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }
        }
    }
}

