package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.cookstovecare.data.local.AuthDataStore
import com.example.cookstovecare.data.repository.CookstoveRepository
import com.example.cookstovecare.ui.theme.SuccessGreen
import com.example.cookstovecare.ui.viewmodel.TechnicianViewModel
import com.example.cookstovecare.ui.viewmodel.TechnicianViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Technician Task Board - Mobile-friendly vertical layout with sections:
 * Assigned, In Progress, Completed.
 * Technician sees only tasks assigned to them.
 * Transitions: Assigned → In Progress (Start), In Progress → Completed (Complete → Repair/Replacement form).
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
    val assignedList = assignedTasks.filter { it.statusEnum == TaskStatus.ASSIGNED }
    val inProgressList = assignedTasks.filter { it.statusEnum == TaskStatus.IN_PROGRESS }
    val completedList = assignedTasks.filter {
        it.statusEnum == TaskStatus.REPAIR_COMPLETED || it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED
    }

    var completeConfirmTask by remember { mutableStateOf<CookstoveTask?>(null) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.role_technician), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.logout))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Assigned section
            item {
                SectionHeader(
                    title = stringResource(R.string.status_assigned),
                    count = assignedList.size,
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }
            if (assignedList.isEmpty()) {
                item {
                    EmptySectionPlaceholder(stringResource(R.string.no_assigned_tasks))
                }
            } else {
                items(assignedList, key = { it.id }) { task ->
                    TechnicianTaskCard(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        actionLabel = stringResource(R.string.start),
                        onAction = { viewModel.moveToInProgress(task.id) },
                        isCompleted = false
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // In Progress section
            item {
                SectionHeader(
                    title = stringResource(R.string.in_progress_tasks),
                    count = inProgressList.size,
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            }
            if (inProgressList.isEmpty()) {
                item {
                    EmptySectionPlaceholder(stringResource(R.string.no_assigned_tasks))
                }
            } else {
                items(inProgressList, key = { it.id }) { task ->
                    TechnicianTaskCard(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        actionLabel = stringResource(R.string.complete),
                        onAction = { completeConfirmTask = task },
                        isCompleted = false
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Completed section (read-only)
            item {
                SectionHeader(
                    title = stringResource(R.string.completed_tasks),
                    count = completedList.size,
                    accentColor = SuccessGreen
                )
            }
            if (completedList.isEmpty()) {
                item {
                    EmptySectionPlaceholder(stringResource(R.string.no_assigned_tasks))
                }
            } else {
                items(completedList, key = { it.id }) { task ->
                    TechnicianTaskCard(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        actionLabel = null,
                        onAction = { },
                        isCompleted = true
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = accentColor
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptySectionPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TechnicianTaskCard(
    task: CookstoveTask,
    onClick: () -> Unit,
    actionLabel: String?,
    onAction: () -> Unit,
    isCompleted: Boolean
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.cookstoveNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormat.format(Date(task.collectionDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (task.typeOfProcess) {
                            "REPLACEMENT" -> stringResource(R.string.type_replacement)
                            "REPAIRING" -> stringResource(R.string.type_repairing)
                            else -> stringResource(R.string.type_repairing)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (!isCompleted && actionLabel != null) {
                    TextButton(onClick = { onAction() }) {
                        Text(actionLabel, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
