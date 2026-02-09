package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.local.AuthDataStore
import com.example.cookstovecare.data.local.FieldOfficerInfo
import com.example.cookstovecare.data.repository.CookstoveRepository
import com.example.cookstovecare.ui.theme.AuthGradientStart
import com.example.cookstovecare.ui.theme.AuthGradientStartDark
import com.example.cookstovecare.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldOfficerDetailScreen(
    officerPhone: String,
    authDataStore: AuthDataStore,
    repository: CookstoveRepository,
    onTaskClick: (Long) -> Unit = {},
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) AuthGradientStartDark else AuthGradientStart

    // Load field officer info
    var officer by remember { mutableStateOf<FieldOfficerInfo?>(null) }
    LaunchedEffect(officerPhone) {
        val officers = authDataStore.getAllFieldOfficers()
        officer = officers.find { it.phoneNumber == officerPhone }
    }

    // Load tasks created by this officer
    val allTasks by repository.getAllTasks().collectAsState(initial = emptyList())
    val officerTasks = allTasks.filter { it.createdByFieldOfficer == officerPhone }

    val pendingTasks = officerTasks.filter {
        it.statusEnum == TaskStatus.COLLECTED ||
        it.statusEnum == TaskStatus.ASSIGNED ||
        it.statusEnum == TaskStatus.IN_PROGRESS
    }
    val completedTasks = officerTasks.filter {
        it.statusEnum == TaskStatus.REPAIR_COMPLETED ||
        it.statusEnum == TaskStatus.REPLACEMENT_COMPLETED ||
        it.statusEnum == TaskStatus.DISTRIBUTED
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Pending, 2 = Completed
    val displayedTasks = when (selectedTab) {
        1 -> pendingTasks
        2 -> completedTasks
        else -> officerTasks
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.field_officer_details), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        }
    ) { innerPadding ->
        val off = officer
        if (off == null) {
            // Loading or not found - show phone as fallback
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar header
                item {
                    FieldOfficerAvatarCard(displayName = officerPhone, profileImageUri = null)
                }
                // Details card
                item {
                    FieldOfficerInfoCard(
                        name = officerPhone,
                        phoneNumber = officerPhone,
                        status = stringResource(R.string.active)
                    )
                }
                // Tab selector
                item {
                    FieldOfficerTabSelector(
                        allCount = officerTasks.size,
                        pendingCount = pendingTasks.size,
                        completedCount = completedTasks.size,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        headerColor = headerColor
                    )
                }
                // Task list
                if (displayedTasks.isEmpty()) {
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
                    items(displayedTasks) { task ->
                        FieldOfficerTaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar header
                item {
                    FieldOfficerAvatarCard(
                        displayName = off.displayName,
                        profileImageUri = off.profileImageUri
                    )
                }

                // Details card
                item {
                    FieldOfficerInfoCard(
                        name = off.displayName,
                        phoneNumber = off.phoneNumber,
                        status = stringResource(R.string.active)
                    )
                }

                // Tab selector
                item {
                    FieldOfficerTabSelector(
                        allCount = officerTasks.size,
                        pendingCount = pendingTasks.size,
                        completedCount = completedTasks.size,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        headerColor = headerColor
                    )
                }

                // Task list
                if (displayedTasks.isEmpty()) {
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
                    items(displayedTasks) { task ->
                        FieldOfficerTaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldOfficerAvatarCard(
    displayName: String,
    profileImageUri: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val initial = displayName.take(1).uppercase().ifBlank { "" }
                        if (initial.isNotEmpty() && !initial[0].isDigit()) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldOfficerInfoCard(
    name: String,
    phoneNumber: String,
    status: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.field_officer_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FieldOfficerDetailRow(
                label = stringResource(R.string.field_officer_name),
                value = name
            )
            FieldOfficerDetailRow(
                label = stringResource(R.string.field_officer_phone),
                value = phoneNumber
            )
            FieldOfficerDetailRow(
                label = stringResource(R.string.profile_status),
                value = status
            )
        }
    }
}

@Composable
private fun FieldOfficerTabSelector(
    allCount: Int,
    pendingCount: Int,
    completedCount: Int,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    headerColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val tabs = listOf(
            "All ($allCount)",
            "Pending ($pendingCount)",
            "Completed ($completedCount)"
        )
        tabs.forEachIndexed { index, label ->
            val isSelected = selectedTab == index
            Surface(
                onClick = { onTabSelected(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) headerColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FieldOfficerTaskCard(
    task: CookstoveTask,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val processText = task.typeOfProcess?.let { type ->
        when (type) {
            "REPAIRING" -> stringResource(R.string.type_repairing)
            "REPLACEMENT" -> stringResource(R.string.type_replacement)
            else -> type
        }
    }

    val statusText = when (task.statusEnum) {
        TaskStatus.COLLECTED -> "New"
        TaskStatus.ASSIGNED -> "Assigned"
        TaskStatus.IN_PROGRESS -> "In Progress"
        TaskStatus.REPAIR_COMPLETED -> "Repaired"
        TaskStatus.REPLACEMENT_COMPLETED -> "Replaced"
        TaskStatus.DISTRIBUTED -> "Delivered"
    }
    val statusColor = when (task.statusEnum) {
        TaskStatus.COLLECTED -> Color(0xFF2196F3)
        TaskStatus.ASSIGNED -> Color(0xFF9C27B0)
        TaskStatus.IN_PROGRESS -> Color(0xFFFF9800)
        TaskStatus.REPAIR_COMPLETED, TaskStatus.REPLACEMENT_COMPLETED -> SuccessGreen
        TaskStatus.DISTRIBUTED -> Color(0xFF4CAF50)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image
            Box(
                modifier = Modifier
                    .size(52.dp)
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (processText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = processText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Status badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun FieldOfficerDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
