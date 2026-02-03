package com.example.cookstovecare.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cookstovecare.R
import com.example.cookstovecare.ui.theme.LocalWelcomeColors

/**
 * Welcome screen using system theme colors.
 * Horizontal gradient, scattered dots, illustration with floating elements,
 * heading, description, primary pill button.
 */
@Composable
fun WelcomeScreen(
    onLetsStart: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val welcomeColors = LocalWelcomeColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Illustration area: horizontal gradient from theme
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            welcomeColors.gradientStart,
                            welcomeColors.gradientMid1,
                            welcomeColors.gradientMid2,
                            welcomeColors.gradientEnd
                        )
                    )
                )
        ) {
            // Scattered translucent dots
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotPositions = listOf(
                    Offset(size.width * 0.08f, size.height * 0.12f),
                    Offset(size.width * 0.88f, size.height * 0.08f),
                    Offset(size.width * 0.18f, size.height * 0.72f),
                    Offset(size.width * 0.78f, size.height * 0.68f),
                    Offset(size.width * 0.48f, size.height * 0.18f),
                    Offset(size.width * 0.12f, size.height * 0.48f),
                    Offset(size.width * 0.92f, size.height * 0.38f),
                    Offset(size.width * 0.32f, size.height * 0.22f),
                    Offset(size.width * 0.68f, size.height * 0.58f),
                    Offset(size.width * 0.42f, size.height * 0.78f),
                    Offset(size.width * 0.22f, size.height * 0.32f),
                    Offset(size.width * 0.82f, size.height * 0.28f)
                )
                val dotColors = listOf(
                    welcomeColors.dotPink,
                    welcomeColors.dotBlue,
                    welcomeColors.dotYellow,
                    welcomeColors.dotPurple,
                    welcomeColors.dotGreen
                )
                dotPositions.forEachIndexed { i, pos ->
                    drawCircle(
                        color = dotColors[i % dotColors.size],
                        radius = 20.dp.toPx() + (i % 3) * 6,
                        center = pos
                    )
                }
            }

            // Illustration: cookstove + floating elements
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_welcome_cookstove),
                    contentDescription = null,
                    modifier = Modifier.size(160.dp)
                )
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .offset(x = (-90).dp, y = (-100).dp),
                    tint = welcomeColors.iconCalendar
                )
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = (-100).dp, y = 30.dp),
                    tint = welcomeColors.iconBuild
                )
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = null,
                    modifier = Modifier
                        .size(38.dp)
                        .offset(x = 90.dp, y = (-90).dp),
                    tint = welcomeColors.iconAssignment
                )
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .offset(x = 95.dp, y = 40.dp),
                    tint = welcomeColors.iconSettings
                )
            }
        }

        // Text + button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 32.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.welcome_heading),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.welcome_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onLetsStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = stringResource(R.string.lets_start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
