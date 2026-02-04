package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.cookstovecare.data.TechnicianSkillType
import com.example.cookstovecare.data.repository.TechnicianWithCount
import com.example.cookstovecare.ui.viewmodel.TechniciansListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniciansListScreen(
    viewModel: TechniciansListViewModel,
    onBack: (() -> Unit)? = null,
    onCreateTechnician: () -> Unit,
    onEditTechnician: (Long) -> Unit
) {
    val techniciansWithCounts by viewModel.techniciansWithCounts.collectAsState(initial = emptyList())
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            val message = when (msg) {
                "cannot_disable_technician_with_active_tasks" ->
                    context.getString(R.string.cannot_disable_technician)
                else -> msg
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_technicians), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onCreateTechnician) {
                        Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.create_technician))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (techniciansWithCounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.no_technicians),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = onCreateTechnician) {
                                Text(stringResource(R.string.create_technician), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else {
                items(techniciansWithCounts) { item ->
                    TechnicianCard(
                        technicianWithCount = item,
                        onEdit = { onEditTechnician(item.technician.id) },
                        onActiveChange = { newActive ->
                            viewModel.setTechnicianActive(item.technician.id, newActive) { }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TechnicianCard(
    technicianWithCount: TechnicianWithCount,
    onEdit: () -> Unit,
    onActiveChange: (Boolean) -> Unit
) {
    val tech = technicianWithCount.technician
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tech.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tech.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.technician_id) + ": ${tech.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.assigned_count, technicianWithCount.assignedTaskCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (tech.skillType) {
                            TechnicianSkillType.REPAIR -> stringResource(R.string.skill_repair)
                            TechnicianSkillType.REPLACEMENT -> stringResource(R.string.skill_replacement)
                            TechnicianSkillType.BOTH -> stringResource(R.string.skill_both)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (tech.isActive) stringResource(R.string.active) else stringResource(R.string.inactive),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tech.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_technician))
                }
                Switch(
                    checked = tech.isActive,
                    onCheckedChange = { onActiveChange(it) }
                )
            }
        }
    }
}
