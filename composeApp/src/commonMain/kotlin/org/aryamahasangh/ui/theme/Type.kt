package org.aryamahasangh.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.noto_sans_devanagari
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DisplayFontFamily() = FontFamily(Font(Res.font.noto_sans_devanagari))

// Default Material 3 typography values
@Composable
fun AppTypography() = Typography().run {
  val displayFontFamily = DisplayFontFamily()
  val bodyFontFamily = DisplayFontFamily()
  copy(
    displayLarge = displayLarge.copy(fontFamily = displayFontFamily),
    displayMedium = displayMedium.copy(fontFamily = displayFontFamily),
    displaySmall = displaySmall.copy(fontFamily = displayFontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = displayFontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = displayFontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = displayFontFamily),
    titleLarge = titleLarge.copy(fontFamily = displayFontFamily),
    titleMedium = titleMedium.copy(fontFamily = displayFontFamily),
    titleSmall = titleSmall.copy(fontFamily = displayFontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = bodyFontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = bodyFontFamily),
    bodySmall = bodySmall.copy(fontFamily = bodyFontFamily),
    labelLarge = labelLarge.copy(fontFamily = bodyFontFamily),
    labelMedium = labelMedium.copy(fontFamily = bodyFontFamily),
    labelSmall = labelSmall.copy(fontFamily = bodyFontFamily),
  )
}


