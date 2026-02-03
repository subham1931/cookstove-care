package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cookstovecare.R
import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.entity.Technician
import com.example.cookstovecare.data.local.AuthDataStore
import com.example.cookstovecare.data.repository.CookstoveRepository
import com.example.cookstovecare.ui.theme.SuccessGreen
import com.example.cookstovecare.ui.viewmodel.TechnicianViewModel
import com.example.cookstovecare.ui.viewmodel.TechnicianViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
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
            TopAppBar(
                title = {
                    when (selectedBottomTab) {
                        TechnicianBottomTab.TASKS -> {
                            Column {
                                Text(
                                    text = stringResource(R.string.welcome_back),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        TechnicianBottomTab.PROFILE -> {
                            Text(
                                text = stringResource(R.string.nav_profile),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Tab row - fixed width, no scroll
                    TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                    )
                }
            ) {
                TechnicianFilter.entries.forEachIndexed { index, filter ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                when (filter) {
                                    TechnicianFilter.ALL -> stringResource(R.string.filter_all)
                                    TechnicianFilter.TO_DO -> stringResource(R.string.filter_to_do)
                                    TechnicianFilter.IN_PROGRESS -> stringResource(R.string.in_progress_tasks)
                                    TechnicianFilter.COMPLETED -> stringResource(R.string.completed_tasks)
                                }
                            )
                        }
                    )
                }
            }

            // Task list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_assigned_tasks),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        TodayTaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onStart = { viewModel.moveToInProgress(task.id) },
                            onComplete = { completeConfirmTask = task }
                        )
                    }
                }
            }
                }
            }
            TechnicianBottomTab.PROFILE -> {
                TechnicianProfileContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    technician = technician,
                    technicianId = technicianId,
                    phoneNumber = phoneNumber,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun TechnicianProfileContent(
    modifier: Modifier = Modifier,
    technician: Technician?,
    technicianId: Long,
    phoneNumber: String,
    onLogout: () -> Unit
) {
    val displayName = technician?.name?.takeIf { it.isNotBlank() } ?: phoneNumber.ifBlank { stringResource(R.string.nav_profile) }
    val displayPhone = technician?.phoneNumber?.takeIf { it.isNotBlank() } ?: phoneNumber
    val statusText = if (technician?.isActive == true) stringResource(R.string.active) else stringResource(R.string.inactive)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // 1. Profile header – compact, left-aligned hierarchy
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = displayName.take(1).uppercase().ifBlank { "" }
                    if (initial.isNotEmpty()) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = displayPhone.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2. Info card – Role, Technician ID, Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileInfoRow(
                    label = stringResource(R.string.profile_role),
                    value = stringResource(R.string.role_technician)
                )
                ProfileInfoRow(
                    label = stringResource(R.string.profile_technician_id),
                    value = if (technicianId > 0) technicianId.toString() else "—"
                )
                ProfileInfoRow(
                    label = stringResource(R.string.profile_status),
                    value = statusText
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 3. Logout – OutlinedButton, error color
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.logout))
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TodayTaskCard(
    task: CookstoveTask,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val displayTime = dateFormat.format(Date(task.collectionDate))
    val taskType = when (task.typeOfProcess) {
        "REPLACEMENT" -> stringResource(R.string.type_replacement)
        else -> stringResource(R.string.type_repairing)
    }
    val statusRes = when (task.statusEnum) {
        TaskStatus.ASSIGNED -> R.string.filter_to_do
        TaskStatus.IN_PROGRESS -> R.string.in_progress_tasks
        else -> R.string.completed_tasks
    }
    val statusColor = when (task.statusEnum) {
        TaskStatus.ASSIGNED -> MaterialTheme.colorScheme.primary
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        else -> SuccessGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (task.typeOfProcess) {
                        "REPLACEMENT" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Icon(
                        imageVector = if (task.typeOfProcess == "REPLACEMENT") Icons.Default.SwapHoriz else Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp),
                        tint = when (task.typeOfProcess) {
                            "REPLACEMENT" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.cookstoveNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = taskType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = displayTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = stringResource(statusRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                when (task.statusEnum) {
                    TaskStatus.ASSIGNED -> {
                        TextButton(onClick = { onStart() }) {
                            Text(stringResource(R.string.start), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    TaskStatus.IN_PROGRESS -> {
                        TextButton(onClick = { onComplete() }) {
                            Text(stringResource(R.string.complete), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    else -> { }
                }
            }
        }
    }
}
