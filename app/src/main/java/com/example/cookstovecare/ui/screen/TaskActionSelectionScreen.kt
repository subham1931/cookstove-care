package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cookstovecare.R

/**
 * Task Action Selection: After task creation, show Repair and Replacement buttons.
 */
@Composable
fun TaskActionSelectionScreen(
    onRepairClick: () -> Unit,
    onReplacementClick: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TextButton(onClick = onBack) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.choose_action),
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Button(
                onClick = onRepairClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.repair))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onReplacementClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.replacement))
            }
        }
    }
}
