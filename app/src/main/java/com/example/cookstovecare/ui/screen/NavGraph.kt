package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cookstovecare.CookstoveCareApplication
import com.example.cookstovecare.R
import com.example.cookstovecare.data.UserRole
import com.example.cookstovecare.navigation.NavRoutes
import com.example.cookstovecare.ui.viewmodel.AddReturnViewModelFactory
import com.example.cookstovecare.ui.viewmodel.AssignTaskViewModelFactory
import com.example.cookstovecare.ui.viewmodel.AuthViewModel
import com.example.cookstovecare.ui.viewmodel.AuthViewModelFactory
import com.example.cookstovecare.ui.viewmodel.CreateTechnicianViewModelFactory
import com.example.cookstovecare.ui.viewmodel.EditTechnicianViewModelFactory
import com.example.cookstovecare.ui.viewmodel.DashboardViewModel
import com.example.cookstovecare.ui.viewmodel.DashboardViewModelFactory
import com.example.cookstovecare.ui.viewmodel.RepairFormViewModel
import com.example.cookstovecare.ui.viewmodel.RepairFormViewModelFactory
import com.example.cookstovecare.ui.viewmodel.ReplacementFormViewModel
import com.example.cookstovecare.ui.viewmodel.ReplacementFormViewModelFactory
import com.example.cookstovecare.ui.viewmodel.SupervisorTaskListViewModelFactory
import com.example.cookstovecare.ui.viewmodel.SupervisorViewModelFactory
import com.example.cookstovecare.ui.viewmodel.TaskDetailViewModel
import com.example.cookstovecare.ui.viewmodel.TaskDetailViewModelFactory
import com.example.cookstovecare.ui.viewmodel.TechnicianViewModelFactory
import com.example.cookstovecare.ui.viewmodel.TechnicianDetailViewModelFactory
import com.example.cookstovecare.ui.viewmodel.TechniciansListViewModelFactory
import kotlinx.coroutines.launch

/**
 * Navigation graph for the app.
 * Auth-aware: shows role-specific dashboard if logged in, else Welcome/Auth flow.
 */
@Composable
fun CookstoveCareNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val app = LocalContext.current.applicationContext as CookstoveCareApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val isLoggedIn by app.authDataStore.isLoggedIn.collectAsState(initial = null)
    val userRole by app.authDataStore.userRole.collectAsState(initial = UserRole.FIELD_OFFICER)
    val technicianId by app.authDataStore.technicianId.collectAsState(initial = null)

    val startDestination = when {
        isLoggedIn != true -> NavRoutes.WELCOME
        userRole == UserRole.SUPERVISOR -> NavRoutes.SUPERVISOR_DASHBOARD
        userRole == UserRole.TECHNICIAN -> NavRoutes.TECHNICIAN_DASHBOARD
        else -> NavRoutes.FIELD_OFFICER_DASHBOARD
    }

    when (isLoggedIn) {
        null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            key(isLoggedIn, userRole) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(NavRoutes.WELCOME) {
                        WelcomeScreen(
                            onLetsStart = { navController.navigate(NavRoutes.AUTH) }
                        )
                    }

                    composable(NavRoutes.AUTH) {
                        val authViewModel: AuthViewModel = viewModel(
                            factory = AuthViewModelFactory(app.authDataStore, repository)
                        )
                        RepairCenterAuthScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = { role ->
                                val dest = when (role) {
                                    UserRole.SUPERVISOR -> NavRoutes.SUPERVISOR_DASHBOARD
                                    UserRole.TECHNICIAN -> NavRoutes.TECHNICIAN_DASHBOARD
                                    else -> NavRoutes.FIELD_OFFICER_DASHBOARD
                                }
                                navController.navigate(dest) { popUpTo(NavRoutes.WELCOME) { inclusive = true } }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(NavRoutes.FIELD_OFFICER_DASHBOARD) {
                        val viewModel: DashboardViewModel = viewModel(
                            factory = DashboardViewModelFactory(repository)
                        )
                        DashboardScreen(
                            viewModel = viewModel,
                            repository = repository,
                            authDataStore = app.authDataStore,
                            initialEditTaskId = null,
                            onTaskClick = { taskId -> navController.navigate(NavRoutes.taskDetail(taskId)) },
                            onLogout = { scope.launch { app.authDataStore.logout() } },
                            onClearAllData = { scope.launch { repository.clearAllData() } }
                        )
                    }

                    composable(NavRoutes.SUPERVISOR_DASHBOARD) {
                        val viewModel: com.example.cookstovecare.ui.viewmodel.SupervisorViewModel = viewModel(
                            factory = SupervisorViewModelFactory(repository)
                        )
                        SupervisorDashboardScreen(
                            viewModel = viewModel,
                            repository = repository,
                            authDataStore = app.authDataStore,
                            navController = navController,
                            onTaskClick = { taskId -> navController.navigate(NavRoutes.taskDetail(taskId)) },
                            onCreateTechnician = { navController.navigate(NavRoutes.CREATE_TECHNICIAN) },
                            onLogout = { scope.launch { app.authDataStore.logout() } },
                            onClearAllData = { scope.launch { repository.clearAllData() } }
                        )
                    }

                    composable(NavRoutes.SUPERVISOR_TASK_LIST) {
                        val viewModel: com.example.cookstovecare.ui.viewmodel.SupervisorTaskListViewModel = viewModel(
                            factory = SupervisorTaskListViewModelFactory(repository)
                        )
                        val centerName by app.authDataStore.centerName.collectAsState(initial = "")
                        val phoneNumber by app.authDataStore.phoneNumber.collectAsState(initial = "")
                        val displayName = centerName.ifBlank { phoneNumber }.ifBlank { stringResource(R.string.nav_profile) }
                        SupervisorTaskListScreen(
                            viewModel = viewModel,
                            displayName = displayName,
                            onTaskClick = { taskId -> navController.navigate(NavRoutes.taskDetail(taskId)) },
                            onAssignTask = { taskId -> navController.navigate(NavRoutes.assignTask(taskId)) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(NavRoutes.TECHNICIAN_DASHBOARD) {
                        val techId = technicianId ?: 0L
                        val viewModel: com.example.cookstovecare.ui.viewmodel.TechnicianViewModel = viewModel(
                            factory = TechnicianViewModelFactory(repository, techId)
                        )
                        TechnicianDashboardScreen(
                            viewModel = viewModel,
                            repository = repository,
                            authDataStore = app.authDataStore,
                            technicianId = techId,
                            onTaskClick = { taskId -> navController.navigate(NavRoutes.taskDetail(taskId)) },
                            onCompleteRepair = { taskId -> navController.navigate(NavRoutes.repairForm(taskId)) },
                            onCompleteReplacement = { taskId -> navController.navigate(NavRoutes.replacementForm(taskId)) },
                            onLogout = { scope.launch { app.authDataStore.logout() } },
                            onClearAllData = { scope.launch { repository.clearAllData() } }
                        )
                    }

                    composable(
                        route = NavRoutes.DASHBOARD_EDIT,
                        arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                        val viewModel: DashboardViewModel = viewModel(
                            factory = DashboardViewModelFactory(repository)
                        )
                        DashboardScreen(
                            viewModel = viewModel,
                            repository = repository,
                            authDataStore = app.authDataStore,
                            initialEditTaskId = taskId,
                            onTaskClick = { id -> navController.navigate(NavRoutes.taskDetail(id)) },
                            onLogout = { scope.launch { app.authDataStore.logout() } },
                            onClearAllData = { scope.launch { repository.clearAllData() } }
                        )
                    }

                    composable(
                        route = NavRoutes.TASK_DETAIL,
                        arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                        val viewModel: TaskDetailViewModel = viewModel(
                            factory = TaskDetailViewModelFactory(repository, taskId),
                            viewModelStoreOwner = backStackEntry
                        )
                        val backToDashboard = when (userRole) {
                            UserRole.SUPERVISOR -> NavRoutes.SUPERVISOR_DASHBOARD
                            UserRole.TECHNICIAN -> NavRoutes.TECHNICIAN_DASHBOARD
                            else -> NavRoutes.FIELD_OFFICER_DASHBOARD
                        }
                        TaskDetailScreen(
                            viewModel = viewModel,
                            userRole = userRole,
                            onRepairClick = { navController.navigate(NavRoutes.repairForm(taskId)) },
                            onReplacementClick = { navController.navigate(NavRoutes.replacementForm(taskId)) },
                            onAddReturnClick = if (userRole == UserRole.FIELD_OFFICER) {
                                { navController.navigate(NavRoutes.addReturnForm(taskId)) }
                            } else null,
                            onAssignTaskClick = if (userRole == UserRole.SUPERVISOR) {
                                { navController.navigate(NavRoutes.assignTask(taskId)) }
                            } else null,
                            canEditCompletedReport = userRole != UserRole.TECHNICIAN,
                            onBack = {
                                if (userRole == UserRole.SUPERVISOR) {
                                    navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.set("returnTab", 0)
                                }
                                if (!navController.popBackStack()) {
                                    navController.navigate(backToDashboard) {
                                        popUpTo(backToDashboard) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }

                    composable(
                        route = NavRoutes.REPAIR_FORM,
                        arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                        val viewModel: RepairFormViewModel = viewModel(
                            factory = RepairFormViewModelFactory(repository, taskId),
                            viewModelStoreOwner = backStackEntry
                        )
                        val backTo = when (userRole) {
                            UserRole.TECHNICIAN -> NavRoutes.TECHNICIAN_DASHBOARD
                            else -> NavRoutes.FIELD_OFFICER_DASHBOARD
                        }
                        RepairFormScreen(
                            viewModel = viewModel,
                            onSuccess = {
                                navController.navigate(backTo) {
                                    popUpTo(backTo) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = NavRoutes.REPLACEMENT_FORM,
                        arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                        val viewModel: ReplacementFormViewModel = viewModel(
                            factory = ReplacementFormViewModelFactory(repository, taskId),
                            viewModelStoreOwner = backStackEntry
                        )
                        val backTo = when (userRole) {
                            UserRole.TECHNICIAN -> NavRoutes.TECHNICIAN_DASHBOARD
                            else -> NavRoutes.FIELD_OFFICER_DASHBOARD
                        }
                        ReplacementFormScreen(
                            viewModel = viewModel,
                            onSuccess = {
                                navController.navigate(backTo) {
                                    popUpTo(backTo) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = NavRoutes.ADD_RETURN_FORM,
                        arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                        val viewModel: com.example.cookstovecare.ui.viewmodel.AddReturnViewModel = viewModel(
                            factory = AddReturnViewModelFactory(repository, taskId),
                            viewModelStoreOwner = backStackEntry
                        )
                        AddReturnFormScreen(
                            taskId = taskId,
                            viewModel = viewModel,
                            onSuccess = {
                                navController.navigate(NavRoutes.FIELD_OFFICER_DASHBOARD) {
                                    popUpTo(NavRoutes.FIELD_OFFICER_DASHBOARD) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = NavRoutes.ASSIGN_TASK,
                        arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                        val viewModel: com.example.cookstovecare.ui.viewmodel.AssignTaskViewModel = viewModel(
                            factory = AssignTaskViewModelFactory(repository)
                        )
                        AssignTaskScreen(
                            taskId = taskId,
                            viewModel = viewModel,
                            repository = repository,
                            onAssigned = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(NavRoutes.TECHNICIANS_LIST) {
                        val viewModel: com.example.cookstovecare.ui.viewmodel.TechniciansListViewModel = viewModel(
                            factory = TechniciansListViewModelFactory(repository)
                        )
                        TechniciansListScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onCreateTechnician = { navController.navigate(NavRoutes.CREATE_TECHNICIAN) },
                            onTechnicianClick = { id -> navController.navigate(NavRoutes.technicianDetail(id)) }
                        )
                    }

                    composable(
                        route = NavRoutes.TECHNICIAN_DETAIL,
                        arguments = listOf(navArgument("technicianId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val technicianId = backStackEntry.arguments?.getLong("technicianId") ?: 0L
                        val viewModel: com.example.cookstovecare.ui.viewmodel.TechnicianDetailViewModel = viewModel(
                            factory = TechnicianDetailViewModelFactory(repository, technicianId)
                        )
                        TechnicianDetailScreen(
                            technicianId = technicianId,
                            viewModel = viewModel,
                            onEdit = { navController.navigate(NavRoutes.editTechnician(technicianId)) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(NavRoutes.CREATE_TECHNICIAN) {
                        val viewModel: com.example.cookstovecare.ui.viewmodel.CreateTechnicianViewModel = viewModel(
                            factory = CreateTechnicianViewModelFactory(repository)
                        )
                        CreateTechnicianScreen(
                            viewModel = viewModel,
                            onSuccess = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = NavRoutes.EDIT_TECHNICIAN,
                        arguments = listOf(navArgument("technicianId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val technicianId = backStackEntry.arguments?.getLong("technicianId") ?: 0L
                        val viewModel: com.example.cookstovecare.ui.viewmodel.EditTechnicianViewModel = viewModel(
                            factory = EditTechnicianViewModelFactory(repository, technicianId)
                        )
                        EditTechnicianScreen(
                            technicianId = technicianId,
                            viewModel = viewModel,
                            onSuccess = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
