package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cookstovecare.R
import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.entity.Technician
import com.example.cookstovecare.ui.viewmodel.SupervisorTaskListViewModel
import com.example.cookstovecare.ui.viewmodel.SupervisorTaskListViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorTaskListScreen(
    viewModel: SupervisorTaskListViewModel,
    onTaskClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val technicians by viewModel.technicians.collectAsState(initial = emptyList())
    var statusFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var technicianFilterId by remember { mutableStateOf<Long?>(null) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var technicianMenuExpanded by remember { mutableStateOf(false) }

    val filteredTasks = tasks.filter { task ->
        val statusMatch = statusFilter == null || task.statusEnum == statusFilter
        val techMatch = technicianFilterId == null || task.assignedToTechnicianId == technicianFilterId
        statusMatch && techMatch
    }

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.view_tasks), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        FilterChip(
                            selected = statusFilter != null,
                            onClick = { statusMenuExpanded = true },
                            label = {
                                Text(
                                    text = statusFilter?.let { stringResource(statusLabel(it)) }
                                        ?: stringResource(R.string.filter_by_status)
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.all_statuses)) },
                                onClick = {
                                    statusFilter = null
                                    statusMenuExpanded = false
                                }
                            )
                            TaskStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(statusLabel(status))) },
                                    onClick = {
                                        statusFilter = status
                                        statusMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Box {
                        FilterChip(
                            selected = technicianFilterId != null,
                            onClick = { technicianMenuExpanded = true },
                            label = {
                                Text(
                                    text = technicianFilterId?.let { id ->
                                        technicians.find { it.id == id }?.name ?: stringResource(R.string.filter_by_technician)
                                    } ?: stringResource(R.string.filter_by_technician)
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = technicianMenuExpanded,
                            onDismissRequest = { technicianMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.all_technicians)) },
                                onClick = {
                                    technicianFilterId = null
                                    technicianMenuExpanded = false
                                }
                            )
                            technicians.forEach { tech ->
                                DropdownMenuItem(
                                    text = { Text(tech.name) },
                                    onClick = {
                                        technicianFilterId = tech.id
                                        technicianMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Text(
                            text = stringResource(R.string.no_tasks),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            } else {
                items(filteredTasks) { task ->
                    SupervisorTaskCard(
                        task = task,
                        technicians = technicians,
                        dateFormat = dateFormat,
                        onClick = { onTaskClick(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SupervisorTaskCard(
    task: CookstoveTask,
    technicians: List<Technician>,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val assignedName = task.assignedToTechnicianId?.let { id ->
        technicians.find { it.id == id }?.name
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = task.cookstoveNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            task.customerName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = dateFormat.format(Date(task.collectionDate)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (task.statusEnum) {
                        TaskStatus.COLLECTED -> stringResource(R.string.status_collected)
                        TaskStatus.ASSIGNED -> stringResource(R.string.status_assigned)
                        TaskStatus.IN_PROGRESS -> stringResource(R.string.status_processing)
                        TaskStatus.REPAIR_COMPLETED -> stringResource(R.string.status_repair_completed)
                        TaskStatus.REPLACEMENT_COMPLETED -> stringResource(R.string.status_replacement_completed)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = assignedName ?: stringResource(R.string.unassigned_tasks),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun statusLabel(status: TaskStatus): Int = when (status) {
    TaskStatus.COLLECTED -> R.string.status_collected
    TaskStatus.ASSIGNED -> R.string.status_assigned
    TaskStatus.IN_PROGRESS -> R.string.status_processing
    TaskStatus.REPAIR_COMPLETED -> R.string.status_repair_completed
    TaskStatus.REPLACEMENT_COMPLETED -> R.string.status_replacement_completed
}
