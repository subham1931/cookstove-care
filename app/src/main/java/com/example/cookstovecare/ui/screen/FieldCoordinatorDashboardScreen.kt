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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cookstovecare.R
import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.UserRole
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.local.AuthDataStore
import com.example.cookstovecare.data.local.FieldOfficerInfo
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.cookstovecare.data.repository.CookstoveRepository
import com.example.cookstovecare.navigation.NavRoutes
import com.example.cookstovecare.ui.theme.AuthGradientStart
import com.example.cookstovecare.ui.theme.AuthGradientStartDark
import com.example.cookstovecare.ui.theme.SuccessGreen
import com.example.cookstovecare.ui.viewmodel.CreateTaskViewModel
import com.example.cookstovecare.ui.viewmodel.CreateTaskViewModelFactory
import com.example.cookstovecare.ui.viewmodel.SupervisorViewModel
import com.example.cookstovecare.ui.viewmodel.SupervisorViewModelFactory

/** Field Coordinator bottom nav tabs: Home -> Orders -> Field Officers -> Profile */
private enum class FieldCoordinatorTab(val titleRes: Int) {
    HOME(R.string.nav_tasks),
    ORDERS(R.string.filter_orders),
    FIELD_OFFICERS(R.string.nav_field_officers),
    PROFILE(R.string.nav_profile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldCoordinatorDashboardScreen(
    viewModel: SupervisorViewModel,
    repository: CookstoveRepository,
    authDataStore: AuthDataStore,
    navController: NavController,
    onTaskClick: (Long) -> Unit,
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit,
    onClearAllData: (() -> Unit)? = null
) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val phoneNumber by authDataStore.phoneNumber.collectAsState(initial = "")
    val centerName by authDataStore.centerName.collectAsState(initial = "")
    val profileImageUri by authDataStore.profileImageUri.collectAsState(initial = null)
    val userRole by authDataStore.userRole.collectAsState(initial = UserRole.FIELD_COORDINATOR)
    var selectedBottomTab by rememberSaveable { mutableStateOf(FieldCoordinatorTab.HOME) }
    var homeFilterIndex by rememberSaveable { mutableStateOf(0) }
    
    // One-time cleanup: reset wrongly backfilled field officer assignments
    LaunchedEffect(Unit) {
        repository.resetWrongBackfillOnce()
    }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        navController.getBackStackEntry(NavRoutes.FIELD_COORDINATOR_DASHBOARD)?.savedStateHandle?.get<Int>("returnTab")?.let { tabOrdinal ->
            if (tabOrdinal in FieldCoordinatorTab.entries.indices) {
                selectedBottomTab = FieldCoordinatorTab.entries[tabOrdinal]
            }
            navController.getBackStackEntry(NavRoutes.FIELD_COORDINATOR_DASHBOARD)?.savedStateHandle?.remove<Int>("returnTab")
        }
        navController.getBackStackEntry(NavRoutes.FIELD_COORDINATOR_DASHBOARD)?.savedStateHandle?.get<Int>("returnFilterIndex")?.let { filterIndex ->
            homeFilterIndex = filterIndex
            navController.getBackStackEntry(NavRoutes.FIELD_COORDINATOR_DASHBOARD)?.savedStateHandle?.remove<Int>("returnFilterIndex")
        }
    }
    
    val displayName = centerName.ifBlank { phoneNumber }.ifBlank { stringResource(R.string.nav_profile) }
    
    // Create task state
    var showCreateTaskModal by remember { mutableStateOf(false) }
    var showTaskCreatedSuccess by remember { mutableStateOf(false) }
    val createTaskViewModel: CreateTaskViewModel = viewModel(
        factory = CreateTaskViewModelFactory(repository, authDataStore)
    )
    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            when (selectedBottomTab) {
                FieldCoordinatorTab.HOME, FieldCoordinatorTab.FIELD_OFFICERS, FieldCoordinatorTab.PROFILE, FieldCoordinatorTab.ORDERS -> { /* Child screens provide their own header */ }
            }
        },
        floatingActionButton = {
            if (selectedBottomTab == FieldCoordinatorTab.HOME) {
                FloatingActionButton(
                    onClick = { showCreateTaskModal = true },
                    content = {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_new_task))
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                FieldCoordinatorTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedBottomTab == tab,
                        onClick = { selectedBottomTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    FieldCoordinatorTab.HOME -> Icons.Default.Home
                                    FieldCoordinatorTab.ORDERS -> Icons.Default.Assignment
                                    FieldCoordinatorTab.FIELD_OFFICERS -> Icons.Default.Group
                                    FieldCoordinatorTab.PROFILE -> Icons.Default.Person
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
            FieldCoordinatorTab.HOME -> {
                // Home shows greeting, stats, and recent orders
                FieldCoordinatorHomeContent(
                    tasks = tasks,
                    displayName = displayName,
                    authDataStore = authDataStore,
                    onTaskClick = { taskId ->
                        navController.getBackStackEntry(NavRoutes.FIELD_COORDINATOR_DASHBOARD)?.savedStateHandle?.set("returnTab", FieldCoordinatorTab.HOME.ordinal)
                        onTaskClick(taskId)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            FieldCoordinatorTab.ORDERS -> {
                // Orders shows date-based orders view with field officer filter
                FieldCoordinatorWorkSummaryContent(
                    tasks = tasks,
                    displayName = displayName,
                    authDataStore = authDataStore,
                    onTaskClick = { taskId ->
                        navController.getBackStackEntry(NavRoutes.FIELD_COORDINATOR_DASHBOARD)?.savedStateHandle?.set("returnTab", FieldCoordinatorTab.ORDERS.ordinal)
                        onTaskClick(taskId)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            FieldCoordinatorTab.FIELD_OFFICERS -> {
                FieldOfficersListContent(
                    authDataStore = authDataStore,
                    tasks = tasks,
                    onTaskClick = { taskId ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("from_tab", FieldCoordinatorTab.FIELD_OFFICERS.name)
                        onTaskClick(taskId)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            FieldCoordinatorTab.PROFILE -> {
                val assignedCount = tasks.count { it.statusEnum == TaskStatus.ASSIGNED }
                val inProgressCount = tasks.count { it.statusEnum == TaskStatus.IN_PROGRESS }
                val completedCount = tasks.count {
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
                    inProgress = inProgressCount,
                    completed = completedCount,
                    showWorkSummary = true,
                    showSyncStatus = true,
                    onLogout = onLogout,
                    onClearAllData = onClearAllData
                )
            }
        }
    }
    
    // Create Task Modal Bottom Sheet
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
    
    // Success dialog
    if (showTaskCreatedSuccess) {
        TaskCreatedSuccessDialog(
            onDismiss = {
                showTaskCreatedSuccess = false
                createTaskViewModel.resetAfterNavigation()
            }
        )
    }
}

/** Field Coordinator Home Content with greeting header and task overview */
@Composable
private fun FieldCoordinatorHomeContent(
    tasks: List<CookstoveTask>,
    displayName: String,
    authDataStore: AuthDataStore,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart
    
    val greetingText = getGreetingText()
    
    // Fetch field officers for name lookup
    var fieldOfficers by remember { mutableStateOf<List<FieldOfficerInfo>>(emptyList()) }
    LaunchedEffect(Unit) {
        fieldOfficers = authDataStore.getAllFieldOfficers()
    }
    
    // Task counts
    val ordersCount = tasks.count { it.statusEnum == TaskStatus.COLLECTED }
    val assignedCount = tasks.count { it.statusEnum == TaskStatus.ASSIGNED || it.statusEnum == TaskStatus.IN_PROGRESS }
    val completedCount = tasks.count { 
        it.statusEnum == TaskStatus.REPAIR_COMPLETED || 
        it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED ||
        it.statusEnum == TaskStatus.DISTRIBUTED 
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(headerColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.lets_tackle_tasks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
        
        // Stats cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.filter_orders),
                    count = ordersCount,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.status_assigned),
                    count = assignedCount,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.status_completed),
                    count = completedCount,
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Recent orders header
        item {
            Text(
                text = "Recent Orders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        
        // Task list
        if (tasks.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_orders_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        } else {
            items(tasks.take(10)) { task ->
                val officerName = fieldOfficers.find { it.phoneNumber == task.createdByFieldOfficer }?.displayName
                    ?: task.createdByFieldOfficer
                    ?: "Unknown"
                FieldCoordinatorTaskCard(
                    task = task,
                    fieldOfficerName = officerName,
                    onClick = { onTaskClick(task.id) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FieldCoordinatorTaskCard(
    task: CookstoveTask,
    fieldOfficerName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statusText = when (task.statusEnum) {
        TaskStatus.COLLECTED -> stringResource(R.string.status_new)
        TaskStatus.ASSIGNED -> stringResource(R.string.status_assigned)
        TaskStatus.IN_PROGRESS -> stringResource(R.string.status_processing)
        TaskStatus.REPAIR_COMPLETED -> stringResource(R.string.status_repaired)
        TaskStatus.REPLACEMENT_COMPLETED -> stringResource(R.string.status_replaced)
        TaskStatus.DISTRIBUTED -> stringResource(R.string.status_distributed)
    }
    
    val statusColor = when (task.statusEnum) {
        TaskStatus.COLLECTED -> Color(0xFF9E9E9E)
        TaskStatus.ASSIGNED -> Color(0xFF2196F3)
        TaskStatus.IN_PROGRESS -> Color(0xFFFF9800)
        TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED -> Color(0xFF4CAF50)
        TaskStatus.DISTRIBUTED -> SuccessGreen
    }
    
    val processText = task.typeOfProcess?.let { type ->
        when (type) {
            "REPAIRING" -> "Repairing"
            "REPLACEMENT" -> "Replacement"
            else -> type
        }
    }
    val processColor = when (task.typeOfProcess) {
        "REPAIRING" -> Color(0xFFFF9800)
        "REPLACEMENT" -> Color(0xFF2196F3)
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            Box(
                modifier = Modifier
                    .size(60.dp)
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
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Middle: Cookstove number + badges row
            Column(modifier = Modifier.weight(1f)) {
                // Cookstove number
                Text(
                    text = task.cookstoveNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Badges row: Process type + Status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Process badge (Repairing / Replacement)
                    if (processText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = processColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = processText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = processColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            
            // Right side: Field Officer name
            Text(
                text = fieldOfficerName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** Field Coordinator Work Summary with date picker UI */
@Composable
private fun FieldCoordinatorWorkSummaryContent(
    tasks: List<CookstoveTask>,
    displayName: String,
    authDataStore: AuthDataStore,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart
    val context = LocalContext.current
    
    val todayCalendar = java.util.Calendar.getInstance()
    val todayDay = todayCalendar.get(java.util.Calendar.DAY_OF_MONTH)
    val todayMonth = todayCalendar.get(java.util.Calendar.MONTH)
    val todayYear = todayCalendar.get(java.util.Calendar.YEAR)
    
    var selectedMonth by remember { mutableStateOf(todayMonth) }
    var selectedYear by remember { mutableStateOf(todayYear) }
    var selectedDay by remember { mutableStateOf(todayDay) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    
    // Field Officer filter state
    var fieldOfficers by remember { mutableStateOf<List<FieldOfficerInfo>>(emptyList()) }
    var selectedFieldOfficer by remember { mutableStateOf<String?>(null) } // null = All
    var showFieldOfficerPicker by remember { mutableStateOf(false) }
    
    // Status filter state: null = All, or specific status
    var selectedStatusFilter by remember { mutableStateOf<Int?>(null) } // null=All, 0=New, 1=Assigned, 2=Processing, 3=Repaired, 4=Delivered
    
    // Fetch field officers
    LaunchedEffect(Unit) {
        fieldOfficers = authDataStore.getAllFieldOfficers()
    }
    
    val selectedCalendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.MONTH, selectedMonth)
        set(java.util.Calendar.YEAR, selectedYear)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = selectedCalendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    
    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }
    
    // Status filter matching
    fun matchesStatus(task: CookstoveTask): Boolean {
        if (selectedStatusFilter == null) return true
        return when (selectedStatusFilter) {
            0 -> task.statusEnum == TaskStatus.COLLECTED // New
            1 -> task.statusEnum == TaskStatus.ASSIGNED // Assigned
            2 -> task.statusEnum == TaskStatus.IN_PROGRESS // Processing
            3 -> task.statusEnum == TaskStatus.REPAIR_COMPLETED || task.statusEnum == TaskStatus.REPLACEMENT_COMPLETED // Repaired
            4 -> task.statusEnum == TaskStatus.DISTRIBUTED // Delivered
            else -> true
        }
    }
    
    // Filter tasks by date AND field officer AND status
    val filteredTasks = tasks.filter { task ->
        val taskCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = task.createdAt
        }
        val dateMatch = taskCalendar.get(java.util.Calendar.DAY_OF_MONTH) == selectedDay &&
            taskCalendar.get(java.util.Calendar.MONTH) == selectedMonth &&
            taskCalendar.get(java.util.Calendar.YEAR) == selectedYear
        val officerMatch = selectedFieldOfficer == null || task.createdByFieldOfficer == selectedFieldOfficer
        val statusMatch = matchesStatus(task)
        dateMatch && officerMatch && statusMatch
    }
    
    // Count for header - filtered by officer and status only (not date)
    val officerFilteredTotal = tasks.count { task ->
        val officerMatch = selectedFieldOfficer == null || task.createdByFieldOfficer == selectedFieldOfficer
        val statusMatch = matchesStatus(task)
        officerMatch && statusMatch
    }
    
    val monthNames = listOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")
    val years = (todayYear - 5..todayYear + 1).toList()
    
    val selectedOfficerName = fieldOfficers.find { it.phoneNumber == selectedFieldOfficer }?.displayName

    Column(modifier = modifier) {
        // Orders Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(headerColor)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.filter_orders),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$officerFilteredTotal ${stringResource(R.string.total_orders)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                // Orders icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Field Officer Filter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Surface(
                onClick = { showFieldOfficerPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedFieldOfficer != null) headerColor.copy(alpha = 0.15f) 
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (selectedFieldOfficer != null) 
                    androidx.compose.foundation.BorderStroke(1.dp, headerColor) 
                    else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selectedFieldOfficer != null) headerColor 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (selectedFieldOfficer == null) "All Field Officers"
                                else selectedOfficerName ?: selectedFieldOfficer!!,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedFieldOfficer != null) FontWeight.Medium else FontWeight.Normal,
                            color = if (selectedFieldOfficer != null) headerColor 
                                else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(
                expanded = showFieldOfficerPicker, 
                onDismissRequest = { showFieldOfficerPicker = false }
            ) {
                // "All" option
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("All Field Officers")
                        }
                    },
                    onClick = { 
                        selectedFieldOfficer = null
                        showFieldOfficerPicker = false 
                    },
                    leadingIcon = if (selectedFieldOfficer == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp)) }
                    } else null
                )
                // Individual field officers
                fieldOfficers.forEach { officer ->
                    val isSelected = selectedFieldOfficer == officer.phoneNumber
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = officer.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (officer.name != null) {
                                        Text(
                                            text = officer.phoneNumber,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        onClick = { 
                            selectedFieldOfficer = officer.phoneNumber
                            showFieldOfficerPicker = false 
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Status Filter Chips
        val statusNewLabel = stringResource(R.string.status_new)
        val statusAssignedLabel = stringResource(R.string.status_assigned)
        val statusProcessingLabel = stringResource(R.string.status_processing)
        val statusRepairedLabel = stringResource(R.string.status_repaired)
        val statusDeliveredLabel = stringResource(R.string.status_distributed)
        
        val statusFilters = remember(statusNewLabel, statusAssignedLabel, statusProcessingLabel, statusRepairedLabel, statusDeliveredLabel) {
            listOf(
                null to "All",
                0 to statusNewLabel,
                1 to statusAssignedLabel,
                2 to statusProcessingLabel,
                3 to statusRepairedLabel,
                4 to statusDeliveredLabel
            )
        }
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(statusFilters.size) { index ->
                val (filterValue, label) = statusFilters[index]
                val isSelected = selectedStatusFilter == filterValue
                val chipColor = when (filterValue) {
                    0 -> Color(0xFF9E9E9E)       // New - grey
                    1 -> Color(0xFF2196F3)        // Assigned - blue
                    2 -> Color(0xFFFF9800)        // Processing - orange
                    3 -> Color(0xFF4CAF50)        // Repaired - green
                    4 -> SuccessGreen             // Delivered - success green
                    else -> headerColor           // All - primary
                }
                Surface(
                    onClick = { selectedStatusFilter = filterValue },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) chipColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outlineVariant
                    ) else null
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Month and Year selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month dropdown
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { showMonthPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(monthNames[selectedMonth], style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = showMonthPicker, onDismissRequest = { showMonthPicker = false }) {
                    monthNames.forEachIndexed { index, month ->
                        DropdownMenuItem(
                            text = { Text(month) },
                            onClick = { selectedMonth = index; showMonthPicker = false }
                        )
                    }
                }
            }
            
            // Year dropdown
            Box {
                Surface(
                    onClick = { showYearPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$selectedYear", style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = showYearPicker, onDismissRequest = { showYearPicker = false }) {
                    years.forEach { year ->
                        DropdownMenuItem(
                            text = { Text("$year") },
                            onClick = { selectedYear = year; showYearPicker = false }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date picker row
        val dateListState = rememberLazyListState()
        
        // Scroll so selected day appears at the end (right side)
        LaunchedEffect(selectedDay, selectedMonth, selectedYear) {
            val targetIndex = (selectedDay - 1).coerceIn(0, daysInMonth - 1)
            // Show selected day at the right edge (~5 items visible, so offset back by 5)
            val scrollIndex = (targetIndex - 5).coerceAtLeast(0)
            dateListState.animateScrollToItem(scrollIndex)
        }
        
        LazyRow(
            state = dateListState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysInMonth) { index ->
                val day = index + 1
                val isSelected = selectedDay == day
                val isToday = todayDay == day && todayMonth == selectedMonth && todayYear == selectedYear
                val isFuture = (selectedYear > todayYear) ||
                    (selectedYear == todayYear && selectedMonth > todayMonth) ||
                    (selectedYear == todayYear && selectedMonth == todayMonth && day > todayDay)
                val dayOfWeek = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, selectedYear)
                    set(java.util.Calendar.MONTH, selectedMonth)
                    set(java.util.Calendar.DAY_OF_MONTH, day)
                }.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SHORT, java.util.Locale.getDefault())
                
                Surface(
                    onClick = { if (!isFuture) selectedDay = day },
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isFuture -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
                        isSelected -> headerColor
                        isToday -> headerColor.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayOfWeek ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                isSelected -> Color.White
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = "$day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                isSelected -> Color.White
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Orders count
        Text(
            text = "${filteredTasks.size} ${stringResource(R.string.orders_on_date)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Task list
        if (filteredTasks.isEmpty()) {
            Text(
                text = stringResource(R.string.no_orders_on_date),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks) { task ->
                    FieldCoordinatorProgressCard(
                        task = task,
                        onClick = { onTaskClick(task.id) }
                    )
                }
            }
        }
    }
}

/** Field Officers List content */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldOfficersListContent(
    authDataStore: AuthDataStore,
    tasks: List<CookstoveTask>,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart
    val scope = rememberCoroutineScope()
    var fieldOfficers by remember { mutableStateOf<List<FieldOfficerInfo>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }
    var selectedOfficer by remember { mutableStateOf<FieldOfficerInfo?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    // Fetch field officers on launch and on refresh
    LaunchedEffect(refreshKey) {
        val officers = authDataStore.getAllFieldOfficers()
        android.util.Log.d("FieldOfficers", "Fetched ${officers.size} field officers: ${officers.map { it.phoneNumber }}")
        fieldOfficers = officers
    }
    
    // Field Officer Detail Bottom Sheet
    if (selectedOfficer != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedOfficer = null },
            sheetState = sheetState
        ) {
            FieldOfficerDetailContent(
                officer = selectedOfficer!!,
                tasks = tasks,
                allFieldOfficers = fieldOfficers,
                onTaskClick = onTaskClick,
                onClose = { selectedOfficer = null }
            )
        }
    }

    Column(modifier = modifier) {
        // Header with refresh button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(headerColor)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.manage_field_officers),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Refresh button
                    IconButton(
                        onClick = { refreshKey++ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "${fieldOfficers.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        if (fieldOfficers.isEmpty()) {
            // Empty state
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.no_field_officers),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            )
        } else {
            // Field Officers list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(fieldOfficers) { officer ->
                    FieldOfficerCard(
                        officer = officer,
                        onClick = { selectedOfficer = officer }
                    )
                }
            }
        }
    }
}

/** Field Officer card */
@Composable
private fun FieldOfficerCard(
    officer: FieldOfficerInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image or placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (officer.profileImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(officer.profileImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Officer info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = officer.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (officer.name != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = officer.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Role badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SuccessGreen.copy(alpha = 0.2f)
            ) {
                Text(
                    text = stringResource(R.string.role_field_officer),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = SuccessGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun getGreetingText(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> stringResource(R.string.greeting_good_morning)
        hour in 12..16 -> stringResource(R.string.greeting_good_afternoon)
        hour in 17..21 -> stringResource(R.string.greeting_good_evening)
        else -> stringResource(R.string.greeting_welcome)
    }
}

/** Progressive order card with progress bar */
@Composable
private fun FieldCoordinatorProgressCard(
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
            FieldCoordinatorProgressBar(currentStep = currentStep, typeOfProcess = task.typeOfProcess)
        }
    }
}

/** Horizontal progress bar showing order status flow */
@Composable
private fun FieldCoordinatorProgressBar(currentStep: Int, typeOfProcess: String? = null) {
    val completionLabel = if (typeOfProcess == "REPLACEMENT")
        stringResource(R.string.status_replaced)
    else
        stringResource(R.string.status_repaired)
    val steps = listOf(
        stringResource(R.string.status_new),
        stringResource(R.string.status_assigned),
        stringResource(R.string.status_processing),
        completionLabel,
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

/** Field Officer Detail Content - shows overview of their orders */
@Composable
private fun FieldOfficerDetailContent(
    officer: FieldOfficerInfo,
    tasks: List<CookstoveTask>,
    allFieldOfficers: List<FieldOfficerInfo>,
    onTaskClick: (Long) -> Unit,
    onClose: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart
    val context = LocalContext.current
    
    // Filter tasks created by this Field Officer only
    val officerTasks = tasks.filter { it.createdByFieldOfficer == officer.phoneNumber }
    
    // Categorize tasks
    val allOrders = officerTasks
    val pendingOrders = officerTasks.filter { 
        it.statusEnum == TaskStatus.COLLECTED || 
        it.statusEnum == TaskStatus.ASSIGNED || 
        it.statusEnum == TaskStatus.IN_PROGRESS ||
        it.statusEnum == TaskStatus.REPAIR_COMPLETED ||
        it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED
    }
    val deliveredOrders = officerTasks.filter { 
        it.statusEnum == TaskStatus.DISTRIBUTED 
    }
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Pending, 2 = Delivered
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
    ) {
        // Header with officer info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile image or placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (officer.profileImageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(officer.profileImageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = officer.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (officer.name != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = officer.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        
        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                title = stringResource(R.string.dashboard_all_orders),
                count = allOrders.size,
                color = MaterialTheme.colorScheme.primary
            )
            StatCard(
                title = stringResource(R.string.dashboard_pending),
                count = pendingOrders.size,
                color = MaterialTheme.colorScheme.tertiary
            )
            StatCard(
                title = stringResource(R.string.status_distributed),
                count = deliveredOrders.size,
                color = SuccessGreen
            )
        }
        
        // Tab selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                stringResource(R.string.dashboard_all_orders) to 0,
                stringResource(R.string.dashboard_pending) to 1,
                stringResource(R.string.status_distributed) to 2
            ).forEach { (label, index) ->
                val isSelected = selectedTab == index
                Surface(
                    onClick = { selectedTab = index },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Orders list based on selected tab
        val displayTasks = when (selectedTab) {
            0 -> allOrders
            1 -> pendingOrders
            2 -> deliveredOrders
            else -> allOrders
        }
        
        if (displayTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_orders_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayTasks) { task ->
                    FieldOfficerOrderCard(
                        task = task,
                        onClick = { onTaskClick(task.id) }
                    )
                }
            }
        }
    }
}

/** Stat card for Field Officer overview */
@Composable
private fun StatCard(
    title: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Simple order card for Field Officer detail view */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldOfficerOrderCard(
    task: CookstoveTask,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val statusColor = when (task.statusEnum) {
        TaskStatus.DISTRIBUTED -> SuccessGreen
        TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED -> Color(0xFF2196F3)
        TaskStatus.IN_PROGRESS -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.tertiary
    }
    val statusText = when (task.statusEnum) {
        TaskStatus.COLLECTED -> stringResource(R.string.status_new)
        TaskStatus.ASSIGNED -> stringResource(R.string.status_assigned)
        TaskStatus.IN_PROGRESS -> stringResource(R.string.status_processing)
        TaskStatus.REPAIR_COMPLETED -> stringResource(R.string.status_repaired)
        TaskStatus.REPLACEMENT_COMPLETED -> stringResource(R.string.status_replaced)
        TaskStatus.DISTRIBUTED -> stringResource(R.string.status_distributed)
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.cookstoveNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                task.typeOfProcess?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Status badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
