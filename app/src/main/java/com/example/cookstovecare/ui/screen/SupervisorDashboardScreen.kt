package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.cookstovecare.data.UserRole
import com.example.cookstovecare.data.local.AuthDataStore
import com.example.cookstovecare.data.repository.CookstoveRepository
import com.example.cookstovecare.ui.theme.SuccessGreen
import com.example.cookstovecare.ui.viewmodel.SupervisorTaskListViewModel
import com.example.cookstovecare.ui.viewmodel.SupervisorTaskListViewModelFactory
import com.example.cookstovecare.ui.viewmodel.SupervisorViewModel
import com.example.cookstovecare.ui.viewmodel.SupervisorViewModelFactory
import com.example.cookstovecare.ui.viewmodel.TechniciansListViewModel
import com.example.cookstovecare.ui.viewmodel.TechniciansListViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel

/** Supervisor bottom nav tabs: Dashboard -> Tasks -> Technicians -> Profile */
private enum class SupervisorTab(val titleRes: Int) {
    DASHBOARD(R.string.nav_dashboard),
    TASKS(R.string.nav_tasks),
    TECHNICIANS(R.string.nav_technicians),
    PROFILE(R.string.nav_profile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboardScreen(
    viewModel: SupervisorViewModel,
    repository: CookstoveRepository,
    authDataStore: AuthDataStore,
    onTaskClick: (Long) -> Unit,
    onCreateTechnician: () -> Unit,
    onEditTechnician: (Long) -> Unit,
    onLogout: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val phoneNumber by authDataStore.phoneNumber.collectAsState(initial = "")
    val centerName by authDataStore.centerName.collectAsState(initial = "")
    val userRole by authDataStore.userRole.collectAsState(initial = UserRole.SUPERVISOR)
    var selectedBottomTab by remember { mutableStateOf(SupervisorTab.DASHBOARD) }
    val displayName = centerName.ifBlank { phoneNumber }.ifBlank { stringResource(R.string.nav_profile) }
    val totalTasks = tasks.size
    val unassignedCount = tasks.count { it.statusEnum == TaskStatus.COLLECTED }
    val inProgressCount = tasks.count { it.statusEnum == TaskStatus.ASSIGNED || it.statusEnum == TaskStatus.IN_PROGRESS }
    val completedCount = tasks.count {
        it.statusEnum == TaskStatus.REPAIR_COMPLETED || it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED
    }

    Scaffold(
        topBar = {
            when (selectedBottomTab) {
                SupervisorTab.DASHBOARD -> TopAppBar(
                    title = { Text(stringResource(R.string.role_supervisor), fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onCreateTechnician) {
                            Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.create_technician))
                        }
                    }
                )
                SupervisorTab.TASKS, SupervisorTab.TECHNICIANS -> { /* Child screens provide their own TopAppBar */ }
                SupervisorTab.PROFILE -> TopAppBar(
                    title = { Text(stringResource(R.string.nav_profile), fontWeight = FontWeight.Bold) }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                SupervisorTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedBottomTab == tab,
                        onClick = { selectedBottomTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    SupervisorTab.DASHBOARD -> Icons.Default.Dashboard
                                    SupervisorTab.TASKS -> Icons.Default.Assignment
                                    SupervisorTab.TECHNICIANS -> Icons.Default.Group
                                    SupervisorTab.PROFILE -> Icons.Default.Person
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
            SupervisorTab.DASHBOARD -> DashboardTabContent(
                modifier = Modifier.padding(innerPadding),
                totalTasks = totalTasks,
                unassignedCount = unassignedCount,
                inProgressCount = inProgressCount,
                completedCount = completedCount
            )
            SupervisorTab.TASKS -> {
                val taskListViewModel: SupervisorTaskListViewModel = viewModel(
                    factory = SupervisorTaskListViewModelFactory(repository)
                )
                SupervisorTaskListScreen(
                    viewModel = taskListViewModel,
                    onTaskClick = onTaskClick,
                    onBack = null
                )
            }
            SupervisorTab.TECHNICIANS -> {
                val techniciansViewModel: TechniciansListViewModel = viewModel(
                    factory = TechniciansListViewModelFactory(repository)
                )
                TechniciansListScreen(
                    viewModel = techniciansViewModel,
                    onBack = null,
                    onCreateTechnician = onCreateTechnician,
                    onEditTechnician = onEditTechnician
                )
            }
            SupervisorTab.PROFILE -> {
                val assignedCount = tasks.count { it.statusEnum == TaskStatus.ASSIGNED }
                val inProgressCountProfile = tasks.count { it.statusEnum == TaskStatus.IN_PROGRESS }
                val completedCountProfile = tasks.count {
                    it.statusEnum == TaskStatus.REPAIR_COMPLETED || it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED
                }
                ProfileScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    displayName = displayName,
                    displayPhone = phoneNumber,
                    role = userRole,
                    tasksAssigned = assignedCount,
                    inProgress = inProgressCountProfile,
                    completed = completedCountProfile,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun DashboardTabContent(
    modifier: Modifier = Modifier,
    totalTasks: Int,
    unassignedCount: Int,
    inProgressCount: Int,
    completedCount: Int
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = stringResource(R.string.total_tasks),
                        count = totalTasks,
                        icon = Icons.Default.Assignment,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = stringResource(R.string.unassigned_tasks),
                        count = unassignedCount,
                        icon = Icons.Default.Schedule,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = stringResource(R.string.in_progress_tasks),
                        count = inProgressCount,
                        icon = Icons.Default.Schedule,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = stringResource(R.string.completed_tasks),
                        count = completedCount,
                        icon = Icons.Default.CheckCircle,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = accentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
