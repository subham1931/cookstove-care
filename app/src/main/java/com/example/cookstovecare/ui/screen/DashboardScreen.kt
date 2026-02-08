package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cookstovecare.R
import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.UserRole
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.local.AuthDataStore
import com.example.cookstovecare.data.repository.CookstoveRepository
import com.example.cookstovecare.ui.theme.AuthGradientStart
import com.example.cookstovecare.ui.theme.AuthGradientStartDark
import com.example.cookstovecare.ui.theme.SuccessGreen
import com.example.cookstovecare.ui.viewmodel.CreateTaskViewModel
import com.example.cookstovecare.ui.viewmodel.CreateTaskViewModelFactory
import com.example.cookstovecare.ui.viewmodel.DashboardViewModel
import com.example.cookstovecare.ui.viewmodel.EditTaskViewModel
import com.example.cookstovecare.ui.viewmodel.EditTaskViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel


/** Field Officer bottom nav tabs */
private enum class FieldOfficerTab(val titleRes: Int) {
    TASKS(R.string.nav_tasks),
    ORDERS(R.string.dashboard_orders),
    PROFILE(R.string.nav_profile)
}

/**
 * Field Officer dashboard: Tasks | Profile bottom nav.
 * Material 3, 8dp spacing grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    repository: CookstoveRepository,
    authDataStore: AuthDataStore,
    initialEditTaskId: Long? = null,
    onTaskClick: (Long) -> Unit,
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onClearAllData: (() -> Unit)? = null
) {
    val tasks by viewModel.tasks.collectAsState()
    val phoneNumber by authDataStore.phoneNumber.collectAsState(initial = "")
    val centerName by authDataStore.centerName.collectAsState(initial = "")
    val profileImageUri by authDataStore.profileImageUri.collectAsState(initial = null)
    val userRole by authDataStore.userRole.collectAsState(initial = UserRole.FIELD_OFFICER)
    var selectedBottomTab by rememberSaveable { mutableStateOf(FieldOfficerTab.TASKS) }
    var showCreateTaskModal by remember { mutableStateOf(false) }
    var showTaskCreatedSuccess by remember { mutableStateOf(false) }
    var editTaskId by remember(initialEditTaskId) { mutableStateOf<Long?>(initialEditTaskId) }
    val createTaskViewModel: CreateTaskViewModel = viewModel(
        factory = CreateTaskViewModelFactory(repository, authDataStore)
    )
    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pendingTasks = tasks.filter {
        it.statusEnum == TaskStatus.COLLECTED || it.statusEnum == TaskStatus.ASSIGNED || it.statusEnum == TaskStatus.IN_PROGRESS
    }
    val completedTasks = tasks.filter {
        it.statusEnum == TaskStatus.REPAIR_COMPLETED || it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED
    }
    val distributedTasks = tasks.filter {
        it.statusEnum == TaskStatus.DISTRIBUTED
    }
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0 = Pending, 1 = Completed

    val displayName = centerName.ifBlank { phoneNumber }.ifBlank { stringResource(R.string.nav_profile) }

    Scaffold(
        floatingActionButton = {
            if (selectedBottomTab == FieldOfficerTab.TASKS) {
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
                FieldOfficerTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedBottomTab == tab,
                        onClick = { selectedBottomTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    FieldOfficerTab.TASKS -> Icons.Default.Home
                                    FieldOfficerTab.ORDERS -> Icons.Default.ShoppingCart
                                    FieldOfficerTab.PROFILE -> Icons.Default.Person
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
            FieldOfficerTab.TASKS -> {
                val isDark = isSystemInDarkTheme()
                val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart
                val greetingText = getGreetingText()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(
                                    RoundedCornerShape(
                                        bottomStart = 32.dp,
                                        bottomEnd = 32.dp
                                    )
                                )
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

                    item {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 16.dp)
                        ) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.weight(1f),
                        icon = { },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.dashboard_pending),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTab == 0) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${pendingTasks.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedTab == 0) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    )
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.weight(1f),
                        icon = { },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.dashboard_completed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTab == 1) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${completedTasks.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedTab == 1) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    )
                }
            }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    when (selectedTab) {
                        0 -> {
                            if (pendingTasks.isNotEmpty()) {
                                items(pendingTasks, key = { it.id }) { task ->
                                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                        TaskListItem(
                                            task = task,
                                            onClick = { onTaskClick(task.id) },
                                            onUpdateClick = { editTaskId = task.id },
                                            onDeleteClick = { viewModel.deleteTask(task.id) }
                                        )
                                    }
                                }
                            } else {
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
                            }
                        }
                        1 -> {
                            if (completedTasks.isNotEmpty()) {
                                items(completedTasks, key = { it.id }) { task ->
                                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                        TaskListItem(
                                            task = task,
                                            onClick = { onTaskClick(task.id) },
                                            onUpdateClick = { editTaskId = task.id },
                                            onDeleteClick = { viewModel.deleteTask(task.id) }
                                        )
                                    }
                                }
                            } else {
                                item {
                                    Text(
                                        text = stringResource(R.string.no_completed_tasks),
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
            }
            FieldOfficerTab.ORDERS -> {
                val isDark = isSystemInDarkTheme()
                val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart
                
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
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
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
                        Column {
                            Text(
                                text = stringResource(R.string.dashboard_orders),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Month and Year selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                                    verticalAlignment = Alignment.CenterVertically
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
                                modifier = Modifier
                                    .clickable { showYearPicker = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
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
                    val listState = rememberLazyListState(
                        initialFirstVisibleItemIndex = initialIndex
                    )
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
                                horizontalAlignment = Alignment.CenterHorizontally
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
                                    TaskListItem(
                                        task = task,
                                        onClick = { onTaskClick(task.id) },
                                        onUpdateClick = if (task.statusEnum != TaskStatus.DISTRIBUTED && 
                                            task.statusEnum != TaskStatus.REPAIR_COMPLETED && 
                                            task.statusEnum != TaskStatus.REPLACEMENT_COMPLETED) {
                                            { editTaskId = task.id }
                                        } else null,
                                        onDeleteClick = if (task.statusEnum != TaskStatus.DISTRIBUTED) {
                                            { viewModel.deleteTask(task.id) }
                                        } else null
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
            FieldOfficerTab.PROFILE -> {
                val assignedCount = tasks.count { it.statusEnum == TaskStatus.ASSIGNED }
                val inProgressCount = tasks.count { it.statusEnum == TaskStatus.IN_PROGRESS }
                val completedCount = tasks.count {
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
                    inProgress = inProgressCount,
                    completed = completedCount,
                    onLogout = onLogout,
                    onClearAllData = onClearAllData
                )
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
private fun getGreetingText(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> stringResource(R.string.greeting_good_morning)
        hour in 12..16 -> stringResource(R.string.greeting_good_afternoon)
        hour in 17..21 -> stringResource(R.string.greeting_good_evening)
        else -> stringResource(R.string.greeting_welcome)
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
    onUpdateClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
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
        TaskStatus.COLLECTED -> stringResource(R.string.not_assigned)
        TaskStatus.ASSIGNED -> stringResource(R.string.status_assigned)
        TaskStatus.IN_PROGRESS -> stringResource(R.string.in_progress_tasks)
        TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED ->
            stringResource(R.string.status_completed)
        TaskStatus.DISTRIBUTED -> stringResource(R.string.status_distributed)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_task_confirm)) },
            text = { Text(stringResource(R.string.delete_task_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick?.invoke()
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
                        TaskStatus.COLLECTED -> MaterialTheme.colorScheme.error
                        TaskStatus.ASSIGNED -> MaterialTheme.colorScheme.primary
                        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
                        else -> SuccessGreen
                    }
                )
            }
            val isCompleted = task.statusEnum == TaskStatus.REPAIR_COMPLETED ||
                task.statusEnum == TaskStatus.REPLACEMENT_COMPLETED
            val showMenuButton = onUpdateClick != null || onDeleteClick != null
            if (showMenuButton) {
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
                        if (!isCompleted && onUpdateClick != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.update), style = MaterialTheme.typography.bodyLarge) },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    showMenu = false
                                    onUpdateClick.invoke()
                                }
                            )
                            if (onDeleteClick != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }
                        if (onDeleteClick != null) {
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
    }
}
