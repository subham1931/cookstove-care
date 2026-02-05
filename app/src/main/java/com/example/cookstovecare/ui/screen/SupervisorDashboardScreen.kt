package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cookstovecare.R
import com.example.cookstovecare.navigation.NavRoutes
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

/** Supervisor bottom nav tabs: Tasks -> Work Summary -> Technicians -> Profile */
private enum class SupervisorTab(val titleRes: Int) {
    TASKS(R.string.nav_tasks),
    WORK_SUMMARY(R.string.work_summary),
    TECHNICIANS(R.string.nav_technicians),
    PROFILE(R.string.nav_profile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboardScreen(
    viewModel: SupervisorViewModel,
    repository: CookstoveRepository,
    authDataStore: AuthDataStore,
    navController: NavController,
    onTaskClick: (Long) -> Unit,
    onCreateTechnician: () -> Unit,
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit,
    onClearAllData: (() -> Unit)? = null
) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val phoneNumber by authDataStore.phoneNumber.collectAsState(initial = "")
    val centerName by authDataStore.centerName.collectAsState(initial = "")
    val profileImageUri by authDataStore.profileImageUri.collectAsState(initial = null)
    val userRole by authDataStore.userRole.collectAsState(initial = UserRole.SUPERVISOR)
    var selectedBottomTab by rememberSaveable { mutableStateOf(SupervisorTab.TASKS) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.get<Int>("returnTab")?.let { tabOrdinal ->
            if (tabOrdinal in SupervisorTab.entries.indices) {
                selectedBottomTab = SupervisorTab.entries[tabOrdinal]
            }
            navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.remove<Int>("returnTab")
        }
    }
    val displayName = centerName.ifBlank { phoneNumber }.ifBlank { stringResource(R.string.nav_profile) }

    Scaffold(
        topBar = {
            when (selectedBottomTab) {
                SupervisorTab.TASKS, SupervisorTab.TECHNICIANS, SupervisorTab.PROFILE -> { /* Child screens provide their own header */ }
                SupervisorTab.WORK_SUMMARY -> TopAppBar(
                    title = { Text(stringResource(R.string.work_summary), fontWeight = FontWeight.Bold) }
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
                                    SupervisorTab.TASKS -> Icons.Default.Assignment
                                    SupervisorTab.WORK_SUMMARY -> Icons.Default.Assessment
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
            SupervisorTab.TASKS -> {
                val taskListViewModel: SupervisorTaskListViewModel = viewModel(
                    factory = SupervisorTaskListViewModelFactory(repository)
                )
                SupervisorTaskListScreen(
                    viewModel = taskListViewModel,
                    displayName = displayName,
                    onTaskClick = onTaskClick,
                    onAssignTask = { taskId -> navController.navigate(NavRoutes.assignTask(taskId)) },
                    onBack = null
                )
            }
            SupervisorTab.WORK_SUMMARY -> {
                TechnicianWorkSummaryScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    assignedTasks = tasks,
                    repository = repository,
                    onTaskClick = onTaskClick
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
                    onTechnicianClick = { id ->
                        navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.set("returnTab", SupervisorTab.TECHNICIANS.ordinal)
                        navController.navigate(NavRoutes.technicianDetail(id))
                    }
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
                    profileImageUri = profileImageUri,
                    onEditProfile = onEditProfile,
                    tasksAssigned = assignedCount,
                    inProgress = inProgressCountProfile,
                    completed = completedCountProfile,
                    showWorkSummary = false,
                    showSyncStatus = false,
                    onLogout = onLogout,
                    onClearAllData = onClearAllData
                )
            }
        }
    }
}

