package org.aryamahasangh.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.noto_sans_devanagari
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DisplayFontFamily() = FontFamily(Font(Res.font.noto_sans_devanagari))

// Default Material 3 typography values
@Composable
fun AppTypography() =
  Typography().run {
    val displayFontFamily = DisplayFontFamily()
    val bodyFontFamily = DisplayFontFamily()
    copy(
      displayLarge = displayLarge.copy(fontFamily = displayFontFamily, letterSpacing = 0.sp),
      displayMedium = displayMedium.copy(fontFamily = displayFontFamily, letterSpacing = 0.sp),
      displaySmall = displaySmall.copy(fontFamily = displayFontFamily, letterSpacing = 0.sp),
      headlineLarge = headlineLarge.copy(fontFamily = displayFontFamily, letterSpacing = 0.sp),
      headlineMedium = headlineMedium.copy(fontFamily = displayFontFamily, letterSpacing = 0.sp),
      headlineSmall = headlineSmall.copy(fontFamily = displayFontFamily, letterSpacing = 0.sp),
      titleLarge = titleLarge.copy(fontFamily = displayFontFamily, letterSpacing = 0.sp),
      titleMedium = titleMedium.copy(fontFamily = displayFontFamily, letterSpacing = 0.sp),
      titleSmall = titleSmall.copy(fontFamily = displayFontFamily, letterSpacing = 0.sp),
      bodyLarge = bodyLarge.copy(fontFamily = bodyFontFamily, letterSpacing = 0.sp),
      bodyMedium = bodyMedium.copy(fontFamily = bodyFontFamily, letterSpacing = 0.sp),
      bodySmall = bodySmall.copy(fontFamily = bodyFontFamily, letterSpacing = 0.sp),
      labelLarge = labelLarge.copy(fontFamily = bodyFontFamily, letterSpacing = 0.sp),
      labelMedium = labelMedium.copy(fontFamily = bodyFontFamily, letterSpacing = 0.sp),
      labelSmall = labelSmall.copy(fontFamily = bodyFontFamily, letterSpacing = 0.sp),
    )
  }
