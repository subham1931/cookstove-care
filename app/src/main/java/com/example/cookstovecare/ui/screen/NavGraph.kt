package com.example.cookstovecare.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cookstovecare.CookstoveCareApplication
import com.example.cookstovecare.navigation.NavRoutes
import com.example.cookstovecare.ui.viewmodel.DashboardViewModel
import com.example.cookstovecare.ui.viewmodel.DashboardViewModelFactory
import com.example.cookstovecare.ui.viewmodel.RepairFormViewModel
import com.example.cookstovecare.ui.viewmodel.RepairFormViewModelFactory
import com.example.cookstovecare.ui.viewmodel.ReplacementFormViewModel
import com.example.cookstovecare.ui.viewmodel.ReplacementFormViewModelFactory
import com.example.cookstovecare.ui.viewmodel.TaskDetailViewModel
import com.example.cookstovecare.ui.viewmodel.TaskDetailViewModelFactory

/**
 * Navigation graph for the app.
 * Wires all screens and ViewModels.
 */
@Composable
fun CookstoveCareNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val app = LocalContext.current.applicationContext as CookstoveCareApplication
    val repository = app.repository

    NavHost(
        navController = navController,
        startDestination = NavRoutes.DASHBOARD
    ) {
        composable(NavRoutes.DASHBOARD) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModelFactory(repository)
            )
            DashboardScreen(
                viewModel = viewModel,
                repository = repository,
                initialEditTaskId = null,
                onTaskClick = { taskId -> navController.navigate(NavRoutes.taskDetail(taskId)) }
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
                initialEditTaskId = taskId,
                onTaskClick = { id -> navController.navigate(NavRoutes.taskDetail(id)) }
            )
        }

        composable(
            route = NavRoutes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            val viewModel: TaskDetailViewModel = viewModel(
                factory = TaskDetailViewModelFactory(
                    repository = repository,
                    taskId = taskId
                ),
                viewModelStoreOwner = backStackEntry
            )
            TaskDetailScreen(
                viewModel = viewModel,
                onRepairClick = {
                    val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                    navController.navigate(NavRoutes.repairForm(taskId))
                },
                onReplacementClick = {
                    val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                    navController.navigate(NavRoutes.replacementForm(taskId))
                },
                onEditClick = {
                    val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                    navController.popBackStack()
                    navController.navigate(NavRoutes.dashboardEdit(taskId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.REPAIR_FORM,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val viewModel: RepairFormViewModel = viewModel(
                factory = RepairFormViewModelFactory(
                    repository = repository,
                    savedStateHandle = backStackEntry.savedStateHandle
                ),
                viewModelStoreOwner = backStackEntry
            )
            RepairFormScreen(
                viewModel = viewModel,
                onSuccess = {
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.DASHBOARD) { inclusive = true }
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
            val viewModel: ReplacementFormViewModel = viewModel(
                factory = ReplacementFormViewModelFactory(
                    repository = repository,
                    savedStateHandle = backStackEntry.savedStateHandle
                ),
                viewModelStoreOwner = backStackEntry
            )
            ReplacementFormScreen(
                viewModel = viewModel,
                onSuccess = {
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.DASHBOARD) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
