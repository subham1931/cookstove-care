package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Work summary for a single date */
private data class DateWorkSummary(
    val dateMillis: Long,
    val tasksAssigned: Int,
    val inProgress: Int,
    val completed: Int
)

/** Tab for work summary: Assigned, In Progress, Completed */
private enum class WorkSummaryTab(val labelRes: Int) {
    ASSIGNED(R.string.tasks_assigned),
    IN_PROGRESS(R.string.in_progress_tasks),
    COMPLETED(R.string.completed_tasks)
}

/**
 * Technician Work Summary screen - schedule-style layout.
 * Left: dates. Right: tabs (Assigned, In Progress, Completed) with task cards.
 */
@Composable
fun TechnicianWorkSummaryScreen(
    assignedTasks: List<CookstoveTask>,
    repository: CookstoveRepository,
    onTaskClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val shortDateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    var dateSummaries by remember { mutableStateOf<List<DateWorkSummary>>(emptyList()) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedMonth by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.MONTH))
    }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(WorkSummaryTab.ASSIGNED) }
    var tasksForDate by remember {
        mutableStateOf<Triple<List<CookstoveTask>, List<CookstoveTask>, List<CookstoveTask>>>(Triple(emptyList(), emptyList(), emptyList()))
    }

    LaunchedEffect(assignedTasks) {
        isLoading = true
        dateSummaries = withContext(Dispatchers.IO) {
            buildDateSummaries(assignedTasks, repository)
        }
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYearVal = now.get(Calendar.YEAR)
        selectedMonth = currentMonth
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedDateMillis = todayCal.timeInMillis
        isLoading = false
    }

    LaunchedEffect(assignedTasks, selectedDateMillis, repository) {
        val dateMillis = selectedDateMillis ?: return@LaunchedEffect
        tasksForDate = withContext(Dispatchers.IO) {
            getTasksForDate(assignedTasks, dateMillis, repository)
        }
    }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val selectedSummary = selectedDateMillis?.let { millis ->
        dateSummaries.find { it.dateMillis == millis }
            ?: DateWorkSummary(dateMillis = millis, tasksAssigned = 0, inProgress = 0, completed = 0)
    }
    val sortedDates = remember(selectedMonth, currentYear, dateSummaries) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, selectedMonth)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        (1..daysInMonth).map { day ->
            Calendar.getInstance().apply {
                set(currentYear, selectedMonth, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.sorted()
    }
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val monthNames = remember {
        (0..11).map { month ->
            Calendar.getInstance().apply { set(Calendar.MONTH, month) }
                .getDisplayName(Calendar.MONTH, java.util.Calendar.SHORT, Locale.getDefault())!!
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Upper section: month cards (date-card style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            (0..11).forEach { month ->
                val isSelected = month == selectedMonth
                Surface(
                    modifier = Modifier
                        .width(64.dp)
                        .clickable {
                            selectedMonth = month
                            val now = Calendar.getInstance()
                            val isCurrentMonth = month == now.get(Calendar.MONTH) && currentYear == now.get(Calendar.YEAR)
                            selectedDateMillis = if (isCurrentMonth) {
                                now.apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            } else {
                                Calendar.getInstance().apply {
                                    set(currentYear, month, 1, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = currentYear.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = monthNames[month],
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        // Schedule-style: left = dates, right = content
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Left: vertical date list (like time axis)
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (sortedDates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_tasks),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(items = sortedDates, key = { it }) { dateMillis ->
                            val isSelected = dateMillis == selectedDateMillis
                            val isToday = dateMillis == todayStart
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .then(
                                            if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary)
                                            else Modifier
                                        )
                                        .clickable { selectedDateMillis = dateMillis }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = shortDateFormat.format(Date(dateMillis)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Right: content blocks (work details for selected date)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                if (selectedSummary != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = dateFormat.format(Date(selectedSummary.dateMillis)),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Tabs: Assigned, In Progress, Completed
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                WorkSummaryTab.entries.forEach { tab ->
                                    val isSelected = selectedTab == tab
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            androidx.compose.ui.graphics.Color.Transparent
                                        },
                                        onClick = { selectedTab = tab }
                                    ) {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 10.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // Task cards for selected tab
                        val (assigned, inProgress, completed) = tasksForDate
                        val taskList = when (selectedTab) {
                            WorkSummaryTab.ASSIGNED -> assigned
                            WorkSummaryTab.IN_PROGRESS -> inProgress
                            WorkSummaryTab.COMPLETED -> completed
                        }
                        if (taskList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_tasks),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 0.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(taskList, key = { it.id }) { task ->
                                    WorkSummaryTaskCard(
                                        task = task,
                                        onClick = { onTaskClick(task.id) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.select_date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private suspend fun buildDateSummaries(
    tasks: List<CookstoveTask>,
    repository: CookstoveRepository
): List<DateWorkSummary> {
    val dayStart = mutableMapOf<Long, Triple<Int, Int, Int>>()

    fun addToDate(millis: Long, assigned: Int = 0, inProgress: Int = 0, completed: Int = 0) {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val day = cal.timeInMillis
        val current = dayStart.getOrDefault(day, Triple(0, 0, 0))
        dayStart[day] = Triple(
            current.first + assigned,
            current.second + inProgress,
            current.third + completed
        )
    }

    for (task in tasks) {
        val collectionDay = Calendar.getInstance().apply { timeInMillis = task.collectionDate }
            .apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        when (task.statusEnum) {
            TaskStatus.ASSIGNED -> addToDate(collectionDay, assigned = 1)
            TaskStatus.IN_PROGRESS -> {
                task.workStartedAt?.let { started ->
                    val startedDay = Calendar.getInstance().apply { timeInMillis = started }
                        .apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    addToDate(startedDay, inProgress = 1)
                } ?: addToDate(collectionDay, inProgress = 1)
            }
            TaskStatus.REPAIR_COMPLETED -> {
                val repair = repository.getRepairDataByTaskId(task.id)
                if (repair != null) {
                    val completedDay = Calendar.getInstance().apply { timeInMillis = repair.repairCompletionDate }
                        .apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    addToDate(completedDay, completed = 1)
                }
            }
            TaskStatus.REPLACEMENT_COMPLETED -> {
                val replacement = repository.getReplacementDataByTaskId(task.id)
                if (replacement != null) {
                    val completedDay = Calendar.getInstance().apply { timeInMillis = replacement.replacementDate }
                        .apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    addToDate(completedDay, completed = 1)
                }
            }
            else -> { /* COLLECTED - not assigned to technician */ }
        }
    }

    return dayStart.map { (dateMillis, triple) ->
        DateWorkSummary(
            dateMillis = dateMillis,
            tasksAssigned = triple.first,
            inProgress = triple.second,
            completed = triple.third
        )
    }
}

/** Returns (assigned, inProgress, completed) tasks for the given date. */
private suspend fun getTasksForDate(
    tasks: List<CookstoveTask>,
    dateMillis: Long,
    repository: CookstoveRepository
): Triple<List<CookstoveTask>, List<CookstoveTask>, List<CookstoveTask>> {
    fun toDayStart(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    val targetDay = toDayStart(dateMillis)
    val assigned = mutableListOf<CookstoveTask>()
    val inProgress = mutableListOf<CookstoveTask>()
    val completed = mutableListOf<CookstoveTask>()

    for (task in tasks) {
        val collectionDay = toDayStart(task.collectionDate)
        when (task.statusEnum) {
            TaskStatus.ASSIGNED -> if (collectionDay == targetDay) assigned.add(task)
            TaskStatus.IN_PROGRESS -> {
                val startedDay = task.workStartedAt?.let { toDayStart(it) } ?: collectionDay
                if (startedDay == targetDay) inProgress.add(task)
            }
            TaskStatus.REPAIR_COMPLETED -> {
                val repair = repository.getRepairDataByTaskId(task.id)
                if (repair != null && toDayStart(repair.repairCompletionDate) == targetDay) {
                    completed.add(task)
                }
            }
            TaskStatus.REPLACEMENT_COMPLETED -> {
                val replacement = repository.getReplacementDataByTaskId(task.id)
                if (replacement != null && toDayStart(replacement.replacementDate) == targetDay) {
                    completed.add(task)
                }
            }
            else -> { /* skip */ }
        }
    }
    return Triple(assigned, inProgress, completed)
}

@Composable
private fun WorkSummaryTaskCard(
    task: CookstoveTask,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val collectionDateText = dateFormat.format(Date(task.collectionDate))
    val isCompleted = task.statusEnum == TaskStatus.REPAIR_COMPLETED || task.statusEnum == TaskStatus.REPLACEMENT_COMPLETED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Number (left), Progress (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.cookstoveNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                WorkSummaryTaskStatusChip(status = task.statusEnum)
            }
            // Row 2: Type of work (left), Date (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkSummaryTaskTypeChip(typeOfProcess = task.typeOfProcess)
                Text(
                    text = collectionDateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WorkSummaryTaskStatusChip(status: TaskStatus) {
    val (labelRes, containerColor, contentColor) = when (status) {
        TaskStatus.ASSIGNED -> Triple(
            R.string.status_assigned,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        TaskStatus.IN_PROGRESS -> Triple(
            R.string.in_progress_tasks,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED -> Triple(
            R.string.completed_tasks,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        TaskStatus.COLLECTED -> Triple(
            R.string.status_assigned,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun WorkSummaryTaskTypeChip(typeOfProcess: String?) {
    val label = when (typeOfProcess?.uppercase()) {
        "REPAIRING" -> stringResource(R.string.type_repairing)
        "REPLACEMENT" -> stringResource(R.string.type_replacement)
        "REPAIR" -> stringResource(R.string.repair)
        else -> typeOfProcess ?: "-"
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
