package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cookstovecare.R
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
    ALL(TaskStatus.entries),
    TO_DO(listOf(TaskStatus.ASSIGNED)),
    IN_PROGRESS(listOf(TaskStatus.IN_PROGRESS)),
    COMPLETED(listOf(TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED))
}

/** Bottom navigation tabs */
private enum class TechnicianBottomTab(val titleRes: Int) {
    TASKS(R.string.nav_tasks),
    WORK_SUMMARY(R.string.work_summary),
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
    onCompleteRepair: (Long) -> Unit,
    onCompleteReplacement: (Long) -> Unit,
    onLogout: () -> Unit
) {
    val assignedTasks by viewModel.assignedTasks.collectAsState(initial = emptyList())
    val technician by viewModel.technicianDetails.collectAsState(initial = null)
    val phoneNumber by authDataStore.phoneNumber.collectAsState(initial = "")
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedBottomTab by remember { mutableStateOf(TechnicianBottomTab.TASKS) }
    var completeConfirmTask by remember { mutableStateOf<CookstoveTask?>(null) }

    val selectedFilter = TechnicianFilter.entries[selectedTabIndex]
    val filteredTasks = assignedTasks.filter { task ->
        task.statusEnum in selectedFilter.statuses
    }

    if (completeConfirmTask != null) {
        val task = completeConfirmTask!!
        AlertDialog(
            onDismissRequest = { completeConfirmTask = null },
            title = { Text(stringResource(R.string.complete)) },
            text = { Text(stringResource(R.string.confirm_complete_task)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        completeConfirmTask = null
                        when (task.typeOfProcess) {
                            "REPLACEMENT" -> onCompleteReplacement(task.id)
                            else -> onCompleteRepair(task.id)
                        }
                    }
                ) {
                    Text(stringResource(R.string.complete), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { completeConfirmTask = null }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    val displayName = technician?.name?.takeIf { it.isNotBlank() } ?: phoneNumber.ifBlank { stringResource(R.string.nav_profile) }

    Scaffold(
        topBar = {
            when (selectedBottomTab) {
                TechnicianBottomTab.WORK_SUMMARY -> TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.work_summary),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                )
                TechnicianBottomTab.PROFILE -> TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.nav_profile),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                )
                else -> { /* Tasks: no top bar, greeting in content */ }
            }
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
                                    TechnicianBottomTab.TASKS -> Icons.Default.Assignment
                                    TechnicianBottomTab.WORK_SUMMARY -> Icons.Default.Assessment
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
                TechnicianTaskScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    greetingText = getGreetingText(),
                    technicianId = technicianId,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    filteredTasks = filteredTasks,
                    onTaskClick = onTaskClick,
                    onStart = { viewModel.moveToInProgress(it) },
                    onComplete = { completeConfirmTask = it }
                )
            }
            TechnicianBottomTab.WORK_SUMMARY -> {
                TechnicianWorkSummaryScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    assignedTasks = assignedTasks,
                    repository = repository,
                    onTaskClick = onTaskClick
                )
            }
            TechnicianBottomTab.PROFILE -> {
                val assignedCount = assignedTasks.count { it.statusEnum == TaskStatus.ASSIGNED }
                val inProgressCount = assignedTasks.count { it.statusEnum == TaskStatus.IN_PROGRESS }
                val completedCount = assignedTasks.count {
                    it.statusEnum == TaskStatus.REPAIR_COMPLETED || it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED
                }
                ProfileScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    displayName = technician?.name?.takeIf { it.isNotBlank() } ?: phoneNumber.ifBlank { stringResource(R.string.nav_profile) },
                    displayPhone = technician?.phoneNumber?.takeIf { it.isNotBlank() } ?: phoneNumber,
                    role = com.example.cookstovecare.data.UserRole.TECHNICIAN,
                    id = if (technicianId > 0) technicianId.toString() else null,
                    status = technician?.let { if (it.isActive) stringResource(R.string.active) else stringResource(R.string.inactive) },
                    tasksAssigned = assignedCount,
                    inProgress = inProgressCount,
                    completed = completedCount,
                    showWorkSummary = false,
                    onLogout = onLogout
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
    technicianId: Long,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    filteredTasks: List<CookstoveTask>,
    onTaskClick: (Long) -> Unit,
    onStart: (Long) -> Unit,
    onComplete: (CookstoveTask) -> Unit
) {
    Column(modifier = modifier) {
        GreetingHeader(
            greetingText = greetingText,
            technicianId = technicianId
        )
        TaskStatusTabs(
            selectedIndex = selectedTabIndex,
            onTabSelected = onTabSelected
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

/** Header: greeting, role, technician ID. Left aligned, 16–20dp padding. */
@Composable
private fun GreetingHeader(
    greetingText: String,
    technicianId: Long
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = greetingText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.role_technician),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (technicianId > 0) {
            Text(
                text = stringResource(R.string.profile_technician_id) + " $technicianId",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Segmented-control style tabs. Rounded container, no underline. */
@Composable
private fun TaskStatusTabs(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TechnicianFilter.entries.forEachIndexed { index, filter ->
                val isSelected = selectedIndex == index
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    onClick = { onTabSelected(index) }
                ) {
                    Text(
                        text = when (filter) {
                            TechnicianFilter.ALL -> stringResource(R.string.filter_all)
                            TechnicianFilter.TO_DO -> stringResource(R.string.filter_tab_assigned)
                            TechnicianFilter.IN_PROGRESS -> stringResource(R.string.filter_tab_active)
                            TechnicianFilter.COMPLETED -> stringResource(R.string.filter_tab_done)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
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
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val collectionDateText = dateFormat.format(Date(task.collectionDate))
    val isCompleted = task.statusEnum == TaskStatus.REPAIR_COMPLETED || task.statusEnum == TaskStatus.REPLACEMENT_COMPLETED

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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top row: Cookstove number (left), Status chip (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.cookstoveNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TaskStatusChip(status = task.statusEnum)
            }
            // Middle row: Task type chip, Spacer, Collection date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TaskTypeChip(typeOfProcess = task.typeOfProcess)
                Text(
                    text = collectionDateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Bottom row: Primary action button
            TaskPrimaryActionButton(
                status = task.statusEnum,
                onStart = onStart,
                onComplete = onComplete,
                onView = onClick
            )
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

