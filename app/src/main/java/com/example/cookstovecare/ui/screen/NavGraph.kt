package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cookstovecare.CookstoveCareApplication
import com.example.cookstovecare.navigation.NavRoutes
import com.example.cookstovecare.ui.viewmodel.AuthViewModel
import com.example.cookstovecare.ui.viewmodel.AuthViewModelFactory
import com.example.cookstovecare.ui.viewmodel.DashboardViewModel
import com.example.cookstovecare.ui.viewmodel.DashboardViewModelFactory
import com.example.cookstovecare.ui.viewmodel.RepairFormViewModel
import com.example.cookstovecare.ui.viewmodel.RepairFormViewModelFactory
import com.example.cookstovecare.ui.viewmodel.ReplacementFormViewModel
import com.example.cookstovecare.ui.viewmodel.ReplacementFormViewModelFactory
import com.example.cookstovecare.ui.viewmodel.TaskDetailViewModel
import com.example.cookstovecare.ui.viewmodel.TaskDetailViewModelFactory
import kotlinx.coroutines.launch

/**
 * Navigation graph for the app.
 * Auth-aware: shows Dashboard if logged in, else Welcome/Auth flow.
 * Login persists until logout.
 */
@Composable
fun CookstoveCareNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val app = LocalContext.current.applicationContext as CookstoveCareApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val isLoggedIn by app.authDataStore.isLoggedIn.collectAsState(initial = null)

    when (isLoggedIn) {
        null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            key(isLoggedIn) {
                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn == true) NavRoutes.DASHBOARD else NavRoutes.WELCOME
                ) {
        composable(NavRoutes.WELCOME) {
            WelcomeScreen(
                onLetsStart = {
                    navController.navigate(NavRoutes.AUTH)
                }
            )
        }

        composable(NavRoutes.AUTH) {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(app.authDataStore)
            )
            RepairCenterAuthScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.WELCOME) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.DASHBOARD) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModelFactory(repository)
            )
            DashboardScreen(
                viewModel = viewModel,
                repository = repository,
                authDataStore = app.authDataStore,
                initialEditTaskId = null,
                onTaskClick = { taskId -> navController.navigate(NavRoutes.taskDetail(taskId)) },
                onLogout = {
                    scope.launch { app.authDataStore.logout() }
                }
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
                onLogout = {
                    scope.launch { app.authDataStore.logout() }
                }
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
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.REPAIR_FORM,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            val viewModel: RepairFormViewModel = viewModel(
                factory = RepairFormViewModelFactory(
                    repository = repository,
                    taskId = taskId
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
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            val viewModel: ReplacementFormViewModel = viewModel(
                factory = ReplacementFormViewModelFactory(
                    repository = repository,
                    taskId = taskId
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
        }
    }
}
