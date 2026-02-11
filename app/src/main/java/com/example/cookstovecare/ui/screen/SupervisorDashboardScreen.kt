package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.LaunchedEffect
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
    // Technicians and demo data are now seeded globally in NavGraph

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
                    repository = repository,
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

/** Supervisor Work Summary with date picker UI and filter bottom sheet */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupervisorWorkSummaryContent(
    tasks: List<CookstoveTask>,
    repository: CookstoveRepository,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart
    
    // Load technicians
    val technicians by repository.getAllTechnicians().collectAsState(initial = emptyList())
    
    // Get current date info
    val todayCalendar = java.util.Calendar.getInstance()
    val todayDay = todayCalendar.get(java.util.Calendar.DAY_OF_MONTH)
    val todayMonth = todayCalendar.get(java.util.Calendar.MONTH)
    val todayYear = todayCalendar.get(java.util.Calendar.YEAR)
    
    // State for selected month, year, and day
    var selectedMonth by remember { mutableStateOf(todayMonth) }
    var selectedYear by remember { mutableStateOf(todayYear) }
    var selectedDay by remember { mutableStateOf(todayDay) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    
    // Filter state
    var selectedStatusFilter by remember { mutableStateOf<Int?>(null) }
    var selectedTechnicianId by remember { mutableStateOf<Long?>(null) }
    
    val statusFilters = listOf(
        null to "All",
        0 to "New",
        1 to "Assigned",
        2 to "Processing",
        3 to "Repaired",
        4 to "Delivered"
    )
    val activeFilterCount = listOfNotNull(
        selectedStatusFilter,
        selectedTechnicianId
    ).size
    
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
    val dateFilteredTasks = tasks.filter { task ->
        val taskCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = task.createdAt
        }
        taskCalendar.get(java.util.Calendar.DAY_OF_MONTH) == selectedDay &&
        taskCalendar.get(java.util.Calendar.MONTH) == selectedMonth &&
        taskCalendar.get(java.util.Calendar.YEAR) == selectedYear
    }
    
    // Apply status filter
    val statusFilteredTasks = if (selectedStatusFilter != null) {
        dateFilteredTasks.filter { task ->
            val step = when (task.statusEnum) {
                TaskStatus.COLLECTED -> 0
                TaskStatus.ASSIGNED -> 1
                TaskStatus.IN_PROGRESS -> 2
                TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED -> 3
                TaskStatus.DISTRIBUTED -> 4
            }
            step == selectedStatusFilter
        }
    } else dateFilteredTasks
    
    // Apply technician filter
    val filteredTasks = if (selectedTechnicianId != null) {
        statusFilteredTasks.filter { it.assignedToTechnicianId == selectedTechnicianId }
    } else statusFilteredTasks
    
    val monthNames = listOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")
    
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
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Month nav row: [<] February 2026 [>] ... [filter icon]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Month navigation
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (selectedMonth == 0) {
                            selectedMonth = 11
                            selectedYear -= 1
                        } else {
                            selectedMonth -= 1
                        }
                        selectedDay = 1
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Tappable month-year text with year grid picker
                Box {
                    Surface(
                        onClick = { showMonthYearPicker = !showMonthYearPicker },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${monthNames[selectedMonth]} $selectedYear",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Year grid picker dropdown
                    DropdownMenu(
                        expanded = showMonthYearPicker,
                        onDismissRequest = { showMonthYearPicker = false }
                    ) {
                        val startYear = 1990
                        val allYears = (startYear..todayYear).toList().reversed()
                        val columns = 4
                        val rowCount = (allYears.size + columns - 1) / columns
                        val selectedIndex = allYears.indexOf(selectedYear)
                        val selectedRowIndex = if (selectedIndex >= 0) selectedIndex / columns else 0
                        val yearListState = rememberLazyListState()
                        
                        LaunchedEffect(showMonthYearPicker) {
                            if (showMonthYearPicker) {
                                yearListState.scrollToItem((selectedRowIndex - 1).coerceAtLeast(0))
                            }
                        }
                        
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .width(260.dp)
                        ) {
                            Text(
                                text = "Select Year",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            LazyColumn(
                                state = yearListState,
                                modifier = Modifier.height(240.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(rowCount) { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        for (col in 0 until columns) {
                                            val index = row * columns + col
                                            if (index < allYears.size) {
                                                val year = allYears[index]
                                                val isSelected = selectedYear == year
                                                Surface(
                                                    onClick = {
                                                        selectedYear = year
                                                        if (year == todayYear && selectedMonth > todayMonth) {
                                                            selectedMonth = todayMonth
                                                        }
                                                        selectedDay = 1
                                                        showMonthYearPicker = false
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected) headerColor
                                                        else Color.Transparent,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = "$year",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.White
                                                            else MaterialTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 10.dp)
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                IconButton(
                    onClick = {
                        val isCurrentMonth = selectedMonth == todayMonth && selectedYear == todayYear
                        if (!isCurrentMonth) {
                            if (selectedMonth == 11) {
                                selectedMonth = 0
                                selectedYear += 1
                            } else {
                                selectedMonth += 1
                            }
                            selectedDay = 1
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    val canGoNext = !(selectedMonth == todayMonth && selectedYear == todayYear)
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = if (canGoNext) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
            
            // Filter icon with badge
            Box {
                IconButton(onClick = { showFilterSheet = true }) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = if (activeFilterCount > 0) headerColor
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (activeFilterCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(headerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$activeFilterCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Date picker row
        val listState = rememberLazyListState()
        LaunchedEffect(selectedDay, selectedMonth, selectedYear) {
            val targetIndex = (selectedDay - 1).coerceIn(0, daysInMonth - 1)
            val scrollIndex = (targetIndex - 5).coerceAtLeast(0)
            listState.animateScrollToItem(scrollIndex)
        }
        
        LazyRow(
            state = listState,
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
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Orders count for selected date
        Text(
            text = "${filteredTasks.size} ${stringResource(R.string.orders_on_date)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Orders list
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
                items(filteredTasks, key = { it.id }) { task ->
                    SupervisorWorkSummaryCard(
                        task = task,
                        onClick = { onTaskClick(task.id) }
                    )
                }
            }
        }
    }
    
    // Filter Bottom Sheet
    if (showFilterSheet) {
        var statusDropdownExpanded by remember { mutableStateOf(false) }
        var technicianDropdownExpanded by remember { mutableStateOf(false) }
        val selectedStatusLabel = statusFilters.find { it.first == selectedStatusFilter }?.second ?: "All"
        val selectedTechnicianLabel = if (selectedTechnicianId != null) {
            technicians.find { it.id == selectedTechnicianId }?.name ?: "Unknown"
        } else "All Technicians"
        
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter & Sort",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeFilterCount > 0) {
                        Surface(
                            onClick = {
                                selectedStatusFilter = null
                                selectedTechnicianId = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Reset All",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                
                // Technician dropdown
                Text(
                    text = "Technician",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { technicianDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (selectedTechnicianId != null) headerColor
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedTechnicianLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTechnicianId != null) FontWeight.Medium else FontWeight.Normal,
                                color = if (selectedTechnicianId != null) headerColor
                                    else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = technicianDropdownExpanded,
                        onDismissRequest = { technicianDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        // All option
                        DropdownMenuItem(
                            text = { Text("All Technicians") },
                            onClick = {
                                selectedTechnicianId = null
                                technicianDropdownExpanded = false
                            },
                            leadingIcon = if (selectedTechnicianId == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                        // Individual technicians
                        technicians.forEach { technician ->
                            val isSelected = selectedTechnicianId == technician.id
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            technician.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = technician.phoneNumber,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedTechnicianId = technician.id
                                    technicianDropdownExpanded = false
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
                
                // Order Status dropdown
                Text(
                    text = "Order Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { statusDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (selectedStatusFilter != null) headerColor
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (selectedStatusFilter != null) {
                                    val dotColor = when (selectedStatusFilter) {
                                        0 -> Color(0xFF9E9E9E)
                                        1 -> Color(0xFF2196F3)
                                        2 -> Color(0xFFFF9800)
                                        3 -> Color(0xFF4CAF50)
                                        4 -> SuccessGreen
                                        else -> headerColor
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                                Text(
                                    text = selectedStatusLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedStatusFilter != null) FontWeight.Medium else FontWeight.Normal,
                                    color = if (selectedStatusFilter != null) headerColor
                                        else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        statusFilters.forEach { (filterValue, label) ->
                            val isSelected = selectedStatusFilter == filterValue
                            val dotColor = when (filterValue) {
                                0 -> Color(0xFF9E9E9E)
                                1 -> Color(0xFF2196F3)
                                2 -> Color(0xFFFF9800)
                                3 -> Color(0xFF4CAF50)
                                4 -> SuccessGreen
                                else -> null
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (dotColor != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                        Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                onClick = {
                                    selectedStatusFilter = filterValue
                                    statusDropdownExpanded = false
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
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
            OrderProgressBar(currentStep = currentStep, typeOfProcess = task.typeOfProcess)
        }
    }
}

/** Horizontal progress bar showing order status flow */
@Composable
private fun OrderProgressBar(currentStep: Int, typeOfProcess: String? = null) {
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
