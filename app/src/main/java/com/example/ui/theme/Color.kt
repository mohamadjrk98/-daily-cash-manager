package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette — Clean Modern Financial Ledger
val LightPrimary = Color(0xFF0F2942)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD6E4F0)
val LightOnPrimaryContainer = Color(0xFF0A1D30)

val LightSecondary = Color(0xFF334E68)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE2E8F0)
val LightOnSecondaryContainer = Color(0xFF1E293B)

val LightTertiary = Color(0xFF0D9488)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFCCFBF1)
val LightOnTertiaryContainer = Color(0xFF115E59)

val LightBackground = Color(0xFFF4F6F9)
val LightOnBackground = Color(0xFF0F172A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFEDF1F7)
val LightOnSurfaceVariant = Color(0xFF526071)
val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)

// Dark Palette — Deep Obsidian Ledger
val DarkPrimary = Color(0xFF38BDF8)
val DarkOnPrimary = Color(0xFF082032)
val DarkPrimaryContainer = Color(0xFF12344D)
val DarkOnPrimaryContainer = Color(0xFFE0F2FE)

val DarkSecondary = Color(0xFF94A3B8)
val DarkOnSecondary = Color(0xFF0F172A)
val DarkSecondaryContainer = Color(0xFF1E293B)
val DarkOnSecondaryContainer = Color(0xFFE2E8F0)

val DarkTertiary = Color(0xFF2DD4BF)
val DarkOnTertiary = Color(0xFF042F2E)
val DarkTertiaryContainer = Color(0xFF134E4A)
val DarkOnTertiaryContainer = Color(0xFFCCFBF1)

val DarkBackground = Color(0xFF0B1220)
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkSurface = Color(0xFF131C2E)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkSurfaceVariant = Color(0xFF1C273D)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)
val DarkOutlineVariant = Color(0xFF243147)

@Immutable
data class FinancialColors(
    val inflow: Color,
    val inflowContainer: Color,
    val onInflowContainer: Color,
    val outflow: Color,
    val outflowContainer: Color,
    val onOutflowContainer: Color,
    val cashBadge: Color,
    val cashBadgeContainer: Color,
    val shamCashBadge: Color,
    val shamCashBadgeContainer: Color,
    val cardBorder: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
)

val LightFinancialColors = FinancialColors(
    inflow = Color(0xFF059669),
    inflowContainer = Color(0xFFD1FAE5),
    onInflowContainer = Color(0xFF065F46),
    outflow = Color(0xFFE11D48),
    outflowContainer = Color(0xFFFFE4E6),
    onOutflowContainer = Color(0xFF9F1239),
    cashBadge = Color(0xFFB45309),
    cashBadgeContainer = Color(0xFFFEF3C7),
    shamCashBadge = Color(0xFF1D4ED8),
    shamCashBadgeContainer = Color(0xFFDBEAFE),
    cardBorder = Color(0xFFE2E8F0),
    heroGradientStart = Color(0xFF0F2942),
    heroGradientEnd = Color(0xFF1B4468),
)

val DarkFinancialColors = FinancialColors(
    inflow = Color(0xFF34D399),
    inflowContainer = Color(0xFF064E3B),
    onInflowContainer = Color(0xFFA7F3D0),
    outflow = Color(0xFFFB7185),
    outflowContainer = Color(0xFF4C0519),
    onOutflowContainer = Color(0xFFFECDD3),
    cashBadge = Color(0xFFFBBF24),
    cashBadgeContainer = Color(0xFF451A03),
    shamCashBadge = Color(0xFF60A5FA),
    shamCashBadgeContainer = Color(0xFF1E3A8A),
    cardBorder = Color(0xFF243147),
    heroGradientStart = Color(0xFF122B45),
    heroGradientEnd = Color(0xFF1E3A5F),
)

val LocalFinancialColors = staticCompositionLocalOf { LightFinancialColors }
