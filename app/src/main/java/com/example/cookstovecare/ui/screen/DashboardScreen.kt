package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cookstovecare.R
import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.local.AuthDataStore
import com.example.cookstovecare.data.repository.CookstoveRepository
import com.example.cookstovecare.ui.theme.SuccessGreen
import com.example.cookstovecare.ui.viewmodel.CreateTaskViewModel
import com.example.cookstovecare.ui.viewmodel.CreateTaskViewModelFactory
import com.example.cookstovecare.ui.viewmodel.DashboardViewModel
import com.example.cookstovecare.ui.viewmodel.EditTaskViewModel
import com.example.cookstovecare.ui.viewmodel.EditTaskViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel


/**
 * Dashboard screen inspired by modern task management UI.
 * Header with greeting, progress card, In Progress section, Task Groups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    repository: CookstoveRepository,
    authDataStore: AuthDataStore,
    initialEditTaskId: Long? = null,
    onTaskClick: (Long) -> Unit,
    onLogout: () -> Unit = {}
) {
    val tasks by viewModel.tasks.collectAsState()
    val phoneNumber by authDataStore.phoneNumber.collectAsState(initial = "")
    var showCreateTaskModal by remember { mutableStateOf(false) }
    var showTaskCreatedSuccess by remember { mutableStateOf(false) }
    var editTaskId by remember(initialEditTaskId) { mutableStateOf<Long?>(initialEditTaskId) }
    val createTaskViewModel: CreateTaskViewModel = viewModel(
        factory = CreateTaskViewModelFactory(repository)
    )
    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val inProgressTasks = tasks.filter { it.statusEnum == TaskStatus.COLLECTED }
    val completedTasks = tasks.filter {
        it.statusEnum == TaskStatus.REPAIR_COMPLETED || it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateTaskModal = true },
                content = {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_new_task))
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                DashboardHeader(
                    displayName = phoneNumber.ifBlank { "Repair Center" },
                    onLogout = onLogout
                )
            }

            if (inProgressTasks.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = stringResource(R.string.dashboard_in_progress),
                        count = inProgressTasks.size
                    )
                }
                items(inProgressTasks, key = { it.id }) { task ->
                    TaskListItem(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        onUpdateClick = { editTaskId = task.id },
                        onDeleteClick = { viewModel.deleteTask(task.id) }
                    )
                }
            }

            if (completedTasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.status_completed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(completedTasks, key = { it.id }) { task ->
                    TaskListItem(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        onUpdateClick = { editTaskId = task.id },
                        onDeleteClick = { viewModel.deleteTask(task.id) }
                    )
                }
            }

            if (tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_tasks),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showCreateTaskModal) {
            ModalBottomSheet(
                onDismissRequest = {
                    showCreateTaskModal = false
                    createTaskViewModel.resetAfterNavigation()
                },
                sheetState = createSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.create_task_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    CreateTaskFormContent(
                        viewModel = createTaskViewModel,
                        onTaskCreatedSuccess = {
                            showCreateTaskModal = false
                            showTaskCreatedSuccess = true
                        }
                    )
                }
            }
        }

        if (showTaskCreatedSuccess) {
            TaskCreatedSuccessDialog(
                onDismiss = {
                    showTaskCreatedSuccess = false
                    createTaskViewModel.resetAfterNavigation()
                }
            )
        }

        val taskIdToEdit = editTaskId
        if (taskIdToEdit != null) {
            val editTaskViewModel: EditTaskViewModel = viewModel(
                key = "edit_$taskIdToEdit",
                factory = EditTaskViewModelFactory(repository, taskIdToEdit)
            )
            ModalBottomSheet(
                onDismissRequest = { editTaskId = null },
                sheetState = editSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.edit_task_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    EditTaskFormContent(
                        viewModel = editTaskViewModel,
                        onUpdateSuccess = { editTaskId = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(displayName: String, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase().ifBlank { "R" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.dashboard_hello),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        IconButton(onClick = onLogout) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = stringResource(R.string.logout),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

@Composable
private fun TaskListItem(
    task: CookstoveTask,
    onClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val processText = task.typeOfProcess?.let { type ->
        when (type) {
            "REPAIRING" -> stringResource(R.string.type_repairing)
            "REPLACEMENT" -> stringResource(R.string.type_replacement)
            else -> type
        }
    }
    val statusSectionText = when (task.statusEnum) {
        TaskStatus.COLLECTED -> stringResource(R.string.status_processing)
        TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED ->
            stringResource(R.string.status_completed)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_task_confirm)) },
            text = { Text(stringResource(R.string.delete_task_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick()
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
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
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.cookstoveNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                processText?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = statusSectionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (task.statusEnum) {
                        TaskStatus.COLLECTED -> MaterialTheme.colorScheme.primary
                        else -> SuccessGreen
                    }
                )
            }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.update))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.update), style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {
                            showMenu = false
                            onUpdateClick()
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.delete), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }
}
