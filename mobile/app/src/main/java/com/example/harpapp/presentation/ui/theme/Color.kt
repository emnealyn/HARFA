package com.example.harpapp.presentation.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightPrimary = Color(0xFF6B0F1A)
val LightOnPrimary = Color(0xFFF5ECDD)
val LightHeader = Color(0xFF7A1F2B)
val LightOnHeader = Color(0xFFF5ECDD)
val LightSecondary = Color(0xFF7A5C4A)
val LightOnSecondary = Color(0xFFF5ECDD)
val LightSecondaryContainer = Color(0xFFE6D6C3)
val LightOnSecondaryContainer = Color(0xFF3A2A20)
val LightTertiary = Color(0xFF5E4636)
val LightTertiaryContainer = Color(0xFFE9DCCB)
val LightOnTertiaryContainer = Color(0xFF2E2018)
val LightBackground = Color(0xFFEFE6D8)
val LightSurface = Color(0xFFF7F0E4)
val LightOnSurface = Color(0xFF2B1D16)
val LightSurfaceVariant = Color(0xFFE6DACB)
val LightOnSurfaceVariant = Color(0xFF5A4A40)
val LightSurfaceContainerLowest = Color(0xFFFBF7F0)
val LightSurfaceContainerLow = Color(0xFFF4ECDF)
val LightSurfaceContainer = Color(0xFFEFE5D6)
val LightSurfaceContainerHigh = Color(0xFFE9DECD)
val LightSurfaceContainerHighest = Color(0xFFE3D6C4)
val LightOutline = Color(0xFF8C7462)
val LightOutlineVariant = Color(0xFFC9B59E)
val LightInverseSurface = Color(0xFF3A2E27)
val LightInverseOnSurface = Color(0xFFF3E9D9)
val LightInversePrimary = Color(0xFFE3A3A0)
val LightError = Color(0xFFAC271C)
val LightOnError = Color(0xFFF5ECDD)
val LightErrorContainer = Color(0x33D0402E)
val LightOnErrorContainer = Color(0xFF6A1208)

val DarkPrimary = Color(0xFFB32D3A)
val DarkOnPrimary = Color(0xFFFBF1E4)
val DarkLibraryHeader = Color(0xFF5A1D24)
val DarkOnLibraryHeader = Color(0xFFF1DED6)
val DarkSecondary = Color(0xFF8C6C51)
val DarkOnSecondary = Color(0xFFFBF1E4)
val DarkSecondaryContainer = Color(0xFF4A3A30)
val DarkOnSecondaryContainer = Color(0xFFE8C9A8)
val DarkTertiary = Color(0xFFC08A5A)
val DarkTertiaryContainer = Color(0xFF3E3029)
val DarkOnTertiaryContainer = Color(0xFFE9DCCB)
val DarkBackground = Color(0xFF1C1613)
val DarkSurface = Color(0xFF221B17)
val DarkOnSurface = Color(0xFFEADFD0)
val DarkSurfaceVariant = Color(0xFF3A2F29)
val DarkOnSurfaceVariant = Color(0xFFC9B39C)
val DarkSurfaceContainerLowest = Color(0xFF16110E)
val DarkSurfaceContainerLow = Color(0xFF1F1814)
val DarkSurfaceContainer = Color(0xFF261E19)
val DarkSurfaceContainerHigh = Color(0xFF30261F)
val DarkSurfaceContainerHighest = Color(0xFF3A2F28)
val DarkOutline = Color(0xFF8F735C)
val DarkOutlineVariant = Color(0xFF4D3F36)
val DarkInverseSurface = Color(0xFFEADFD0)
val DarkInverseOnSurface = Color(0xFF2B221D)
val DarkInversePrimary = Color(0xFF6B0F1A)
val DarkError = Color(0xFFF07A66)
val DarkOnError = Color(0xFF1C1613)
val DarkErrorContainer = Color(0x40D84A38)
val DarkOnErrorContainer = Color(0xFFFFD2C7)

@Immutable
data class ExtendedColors(
    val connected: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val topBarTitle: Color,
    val navSelected: Color,
    val difficultyEasy: Color,
    val difficultyMedium: Color,
    val difficultyHard: Color,
    val leverFlat: Color,
    val leverNatural: Color,
    val leverSharp: Color,
    val leverContent: Color,
    val noteUpcoming: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

val LightExtendedColors = ExtendedColors(
    connected = Color(0xFF2E8B3E),
    success = Color(0xFF2E8B3E),
    successContainer = Color(0x292E8B3E),
    onSuccessContainer = Color(0xFF1E4D24),
    topBarTitle = LightPrimary,
    navSelected = LightPrimary,
    difficultyEasy = Color(0xFF3E9A2E),
    difficultyMedium = Color(0xFFE09400),
    difficultyHard = Color(0xFFD93A2B),
    leverFlat = Color(0xFF5A1A22),
    leverNatural = Color(0xFF7A5C4A),
    leverSharp = Color(0xFFC25A3C),
    leverContent = LightPrimary,
    noteUpcoming = LightPrimary,
    warning = Color(0xFF845A00),
    warningContainer = Color(0x38EAA800),
    onWarningContainer = Color(0xFF573A00)
)

val DarkExtendedColors = ExtendedColors(
    connected = Color(0xFF43B04A),
    success = Color(0xFF43B04A),
    successContainer = Color(0x3843B04A),
    onSuccessContainer = Color(0xFFC8EBC0),
    topBarTitle = DarkOnSurface,
    navSelected = Color(0xFFE8737B),
    difficultyEasy = Color(0xFF5DBB3F),
    difficultyMedium = Color(0xFFF2A900),
    difficultyHard = Color(0xFFEF4B3A),
    leverFlat = Color(0xFF9E3A44),
    leverNatural = Color(0xFFB08560),
    leverSharp = Color(0xFFB85C44),
    leverContent = DarkOnSurface,
    noteUpcoming = Color(0xFFE8737B),
    warning = Color(0xFFFDC434),
    warningContainer = Color(0x48F2A33A),
    onWarningContainer = Color(0xFFFFE3B0)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
