package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.ui.theme.AuthGradientStart
import com.example.cookstovecare.ui.theme.AuthGradientStartDark
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
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
    var homeFilterIndex by rememberSaveable { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.get<Int>("returnTab")?.let { tabOrdinal ->
            if (tabOrdinal in SupervisorTab.entries.indices) {
                selectedBottomTab = SupervisorTab.entries[tabOrdinal]
            }
            navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.remove<Int>("returnTab")
        }
        navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.get<Int>("returnFilterIndex")?.let { filterIndex ->
            homeFilterIndex = filterIndex
            navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.remove<Int>("returnFilterIndex")
        }
    }
    val displayName = centerName.ifBlank { phoneNumber }.ifBlank { stringResource(R.string.nav_profile) }

    Scaffold(
        topBar = {
            when (selectedBottomTab) {
                SupervisorTab.TASKS, SupervisorTab.TECHNICIANS, SupervisorTab.PROFILE, SupervisorTab.WORK_SUMMARY -> { /* Child screens provide their own header */ }
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
                                    SupervisorTab.TASKS -> Icons.Default.Home
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
                    onTaskClick = { taskId, filterIndex ->
                        navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.set("returnTab", SupervisorTab.TASKS.ordinal)
                        navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.set("returnFilterIndex", filterIndex)
                        homeFilterIndex = filterIndex
                        onTaskClick(taskId)
                    },
                    onAssignTask = { taskId -> navController.navigate(NavRoutes.assignTask(taskId)) },
                    onBack = null,
                    initialFilterIndex = homeFilterIndex
                )
            }
            SupervisorTab.WORK_SUMMARY -> {
                SupervisorWorkSummaryContent(
                    tasks = tasks,
                    onTaskClick = { taskId ->
                        navController.getBackStackEntry(NavRoutes.SUPERVISOR_DASHBOARD)?.savedStateHandle?.set("returnTab", SupervisorTab.WORK_SUMMARY.ordinal)
                        onTaskClick(taskId)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
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
                    it.statusEnum == TaskStatus.REPAIR_COMPLETED || 
                    it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED ||
                    it.statusEnum == TaskStatus.DISTRIBUTED
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

/** Supervisor Work Summary with date picker UI */
@Composable
private fun SupervisorWorkSummaryContent(
    tasks: List<CookstoveTask>,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart
    val context = LocalContext.current
    
    // Get current date info
    val todayCalendar = java.util.Calendar.getInstance()
    val todayDay = todayCalendar.get(java.util.Calendar.DAY_OF_MONTH)
    val todayMonth = todayCalendar.get(java.util.Calendar.MONTH)
    val todayYear = todayCalendar.get(java.util.Calendar.YEAR)
    
    // State for selected month, year, and day
    var selectedMonth by remember { mutableStateOf(todayMonth) }
    var selectedYear by remember { mutableStateOf(todayYear) }
    var selectedDay by remember { mutableStateOf(todayDay) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    
    // Calculate days in selected month
    val selectedCalendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.MONTH, selectedMonth)
        set(java.util.Calendar.YEAR, selectedYear)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = selectedCalendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    
    // Adjust selected day if it exceeds days in month
    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }
    
    // Filter tasks by selected date
    val filteredTasks = tasks.filter { task ->
        val taskCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = task.createdAt
        }
        taskCalendar.get(java.util.Calendar.DAY_OF_MONTH) == selectedDay &&
        taskCalendar.get(java.util.Calendar.MONTH) == selectedMonth &&
        taskCalendar.get(java.util.Calendar.YEAR) == selectedYear
    }
    
    val monthNames = listOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")
    val years = (todayYear - 5..todayYear + 1).toList()
    
    Column(modifier = modifier) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        bottomStart = 32.dp,
                        bottomEnd = 32.dp
                    )
                )
                .background(headerColor)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.work_summary),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Month and Year selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Month dropdown
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMonthPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthNames[selectedMonth],
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DropdownMenu(
                    expanded = showMonthPicker,
                    onDismissRequest = { showMonthPicker = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    monthNames.forEachIndexed { index, month ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    month,
                                    fontWeight = if (index == selectedMonth) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedMonth = index
                                showMonthPicker = false
                            }
                        )
                    }
                }
            }
            
            // Year dropdown
            Box {
                Surface(
                    modifier = Modifier.clickable { showYearPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$selectedYear",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DropdownMenu(
                    expanded = showYearPicker,
                    onDismissRequest = { showYearPicker = false }
                ) {
                    years.forEach { year ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "$year",
                                    fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedYear = year
                                showYearPicker = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date picker row
        val isCurrentMonth = selectedMonth == todayMonth && selectedYear == todayYear
        val initialIndex = if (isCurrentMonth) maxOf(0, todayDay - 3) else 0
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
        
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysInMonth) { index ->
                val day = index + 1
                val dayCalendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, selectedYear)
                    set(java.util.Calendar.MONTH, selectedMonth)
                    set(java.util.Calendar.DAY_OF_MONTH, day)
                }
                val isToday = day == todayDay && selectedMonth == todayMonth && selectedYear == todayYear
                val dayOfWeek = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                    .format(dayCalendar.time)
                val isSelected = selectedDay == day
                
                Column(
                    modifier = Modifier
                        .width(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) headerColor
                            else if (isToday) headerColor.copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                        .clickable { selectedDay = day }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayOfWeek,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White 
                            else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Orders count for selected date
        Text(
            text = "${filteredTasks.size} ${stringResource(R.string.orders_on_date)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Orders list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredTasks.isNotEmpty()) {
                items(filteredTasks, key = { it.id }) { task ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SupervisorWorkSummaryCard(
                            task = task,
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.no_orders_on_date),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            }
        }
    }
}

/** Work Summary order card with progress bar */
@Composable
private fun SupervisorWorkSummaryCard(
    task: CookstoveTask,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Determine current step (0-4)
    val currentStep = when (task.statusEnum) {
        TaskStatus.COLLECTED -> 0
        TaskStatus.ASSIGNED -> 1
        TaskStatus.IN_PROGRESS -> 2
        TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED -> 3
        TaskStatus.DISTRIBUTED -> 4
    }
    
    val processText = task.typeOfProcess?.let { type ->
        when (type) {
            "REPAIRING" -> stringResource(R.string.type_repairing)
            "REPLACEMENT" -> stringResource(R.string.type_replacement)
            else -> type
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row with image and info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product image
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Task info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.cookstoveNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    processText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            OrderProgressBar(currentStep = currentStep)
        }
    }
}

/** Horizontal progress bar showing order status flow */
@Composable
private fun OrderProgressBar(currentStep: Int) {
    val steps = listOf(
        stringResource(R.string.status_new),
        stringResource(R.string.status_assigned),
        stringResource(R.string.status_processing),
        stringResource(R.string.status_repaired),
        stringResource(R.string.status_distributed)
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, label ->
            val isCompleted = index <= currentStep
            val isCurrent = index == currentStep
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Step indicator with connecting line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left line (except for first step)
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    if (index <= currentStep) SuccessGreen 
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    // Circle indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) SuccessGreen 
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                        }
                    }
                    
                    // Right line (except for last step)
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    if (index < currentStep) SuccessGreen 
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Label
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
