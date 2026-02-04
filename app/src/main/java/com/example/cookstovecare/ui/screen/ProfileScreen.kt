package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cookstovecare.R
import com.example.cookstovecare.data.UserRole

/**
 * Shared Profile screen for all roles (Field Officer, Supervisor, Technician).
 * Material 3 compliant: Header, Work Summary, Settings/Actions, Support, Logout.
 */
@Composable
fun ProfileScreen(
    displayName: String,
    displayPhone: String,
    role: UserRole,
    id: String? = null,
    status: String? = null,
    tasksAssigned: Int = 0,
    inProgress: Int = 0,
    completed: Int = 0,
    isOnline: Boolean = true,
    lastSyncTime: String? = null,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roleLabel = when (role) {
        UserRole.FIELD_OFFICER -> stringResource(R.string.role_field_officer)
        UserRole.SUPERVISOR -> stringResource(R.string.role_supervisor)
        UserRole.TECHNICIAN -> stringResource(R.string.role_technician)
    }
    val roleSecondary = buildString {
        append(roleLabel)
        id?.let { append(" • #").append(it) }
        status?.let { append(" • ").append(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeader(
                displayName = displayName,
                displayPhone = displayPhone,
                roleSecondary = roleSecondary
            )

            WorkSummaryCard(
                tasksAssigned = tasksAssigned,
                inProgress = inProgress,
                completed = completed
            )

            ProfileActionList(
                isOnline = isOnline,
                lastSyncTime = lastSyncTime
            )

            SupportSection()
        }

        LogoutButton(onLogout = onLogout)
    }
}

@Composable
private fun ProfileHeader(
    displayName: String,
    displayPhone: String,
    roleSecondary: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val initial = displayName.take(1).uppercase().ifBlank { "" }
                if (initial.isNotEmpty()) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = displayName.ifBlank { "—" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = displayPhone.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = roleSecondary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkSummaryCard(
    tasksAssigned: Int,
    inProgress: Int,
    completed: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.work_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            WorkSummaryRow(
                label = stringResource(R.string.tasks_assigned),
                value = tasksAssigned
            )
            WorkSummaryRow(
                label = stringResource(R.string.in_progress_tasks),
                value = inProgress
            )
            WorkSummaryRow(
                label = stringResource(R.string.completed_tasks),
                value = completed
            )
        }
    }
}

@Composable
private fun WorkSummaryRow(
    label: String,
    value: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProfileActionList(
    isOnline: Boolean,
    lastSyncTime: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        ProfileActionItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.change_password),
            subtitle = null,
            onClick = { /* stub */ }
        )
        ProfileActionItem(
            icon = Icons.Default.Language,
            title = stringResource(R.string.app_language),
            subtitle = null,
            onClick = { /* stub */ }
        )
        ProfileActionItem(
            icon = Icons.Default.Sync,
            title = stringResource(R.string.sync_status),
            subtitle = when {
                isOnline -> lastSyncTime?.let { stringResource(R.string.last_sync, it) }
                    ?: stringResource(R.string.sync_online)
                else -> lastSyncTime?.let { stringResource(R.string.last_sync, it) }
                    ?: stringResource(R.string.never_synced)
            },
            onClick = { /* stub */ }
        )
    }
}

@Composable
private fun ProfileActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null
) {
    val modifier = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
        .padding(vertical = 12.dp)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let { sub ->
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SupportSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        ProfileActionItem(
            icon = Icons.AutoMirrored.Filled.Help,
            title = stringResource(R.string.help_support),
            subtitle = null,
            onClick = { /* stub */ }
        )
        ProfileActionItem(
            icon = Icons.Default.Info,
            title = stringResource(R.string.app_version),
            subtitle = getAppVersion(),
            onClick = null
        )
    }
}

@Composable
private fun getAppVersion(): String {
    val context = LocalContext.current
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "—"
    } catch (_: Exception) {
        "—"
    }
}

@Composable
private fun LogoutButton(
    onLogout: () -> Unit
) {
    OutlinedButton(
        onClick = onLogout,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.logout))
    }
}
