package com.example.cookstovecare.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Welcome screen gradient and accent colors — light theme */
data class WelcomeColors(
    val gradientStart: Color,
    val gradientMid1: Color,
    val gradientMid2: Color,
    val gradientEnd: Color,
    val dotPink: Color,
    val dotBlue: Color,
    val dotYellow: Color,
    val dotPurple: Color,
    val dotGreen: Color,
    val iconCalendar: Color,
    val iconBuild: Color,
    val iconAssignment: Color,
    val iconSettings: Color
)

val LocalWelcomeColors = compositionLocalOf {
    WelcomeColors(
        gradientStart = WelcomeGradientGreen,
        gradientMid1 = WelcomeGradientYellow,
        gradientMid2 = WelcomeGradientOrange,
        gradientEnd = WelcomeGradientBlue,
        dotPink = WelcomeDotPink,
        dotBlue = WelcomeDotBlue,
        dotYellow = WelcomeDotYellow,
        dotPurple = WelcomeDotPurple,
        dotGreen = WelcomeDotGreen,
        iconCalendar = WelcomeIconCalendar,
        iconBuild = WelcomeIconBuild,
        iconAssignment = WelcomeIconAssignment,
        iconSettings = WelcomeIconSettings
    )
}

private val WelcomeColorsLight = WelcomeColors(
    gradientStart = WelcomeGradientGreen,
    gradientMid1 = WelcomeGradientYellow,
    gradientMid2 = WelcomeGradientOrange,
    gradientEnd = WelcomeGradientBlue,
    dotPink = WelcomeDotPink,
    dotBlue = WelcomeDotBlue,
    dotYellow = WelcomeDotYellow,
    dotPurple = WelcomeDotPurple,
    dotGreen = WelcomeDotGreen,
    iconCalendar = WelcomeIconCalendar,
    iconBuild = WelcomeIconBuild,
    iconAssignment = WelcomeIconAssignment,
    iconSettings = WelcomeIconSettings
)

private val WelcomeColorsDark = WelcomeColors(
    gradientStart = WelcomeGradientGreenDark,
    gradientMid1 = WelcomeGradientYellowDark,
    gradientMid2 = WelcomeGradientOrangeDark,
    gradientEnd = WelcomeGradientBlueDark,
    dotPink = WelcomeDotPinkDark,
    dotBlue = WelcomeDotBlueDark,
    dotYellow = WelcomeDotYellowDark,
    dotPurple = WelcomeDotPurpleDark,
    dotGreen = WelcomeDotGreenDark,
    iconCalendar = WelcomeIconCalendar,
    iconBuild = WelcomeIconBuild,
    iconAssignment = WelcomeIconAssignment,
    iconSettings = WelcomeIconSettings
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun CookstovecareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val welcomeColors = if (darkTheme) WelcomeColorsDark else WelcomeColorsLight

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalWelcomeColors provides welcomeColors,
            content = content
        )
    }
}