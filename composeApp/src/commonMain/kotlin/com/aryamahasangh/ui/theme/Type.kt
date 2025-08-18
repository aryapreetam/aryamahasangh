package com.aryamahasangh.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.TextUnit
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.noto_sans_devanagari
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DisplayFontFamily() = FontFamily(Font(Res.font.noto_sans_devanagari))

// Default Material 3 typography values
@Composable
fun AppTypography(): Typography {
  val displayFontFamily = DisplayFontFamily()
  val bodyFontFamily = DisplayFontFamily()

  // Ensure Hindi locale to enable correct Devanagari shaping rules on all platforms
  val devanagariLocale = LocaleList(listOf(Locale("hi")))

  return Typography().run {
    copy(
      displayLarge = displayLarge.copy(
        fontFamily = displayFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        // Allow bold synthesis so bold text remains bold even if font doesn't contain bold weight
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      displayMedium = displayMedium.copy(
        fontFamily = displayFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      displaySmall = displaySmall.copy(
        fontFamily = displayFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      headlineLarge = headlineLarge.copy(
        fontFamily = displayFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      headlineMedium = headlineMedium.copy(
        fontFamily = displayFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      headlineSmall = headlineSmall.copy(
        fontFamily = displayFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      titleLarge = titleLarge.copy(
        fontFamily = displayFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      titleMedium = titleMedium.copy(
        fontFamily = displayFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      titleSmall = titleSmall.copy(
        fontFamily = displayFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      bodyLarge = bodyLarge.copy(
        fontFamily = bodyFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      bodyMedium = bodyMedium.copy(
        fontFamily = bodyFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      bodySmall = bodySmall.copy(
        fontFamily = bodyFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      labelLarge = labelLarge.copy(
        fontFamily = bodyFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      labelMedium = labelMedium.copy(
        fontFamily = bodyFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      ),
      labelSmall = labelSmall.copy(
        fontFamily = bodyFontFamily,
        letterSpacing = TextUnit.Unspecified,
        fontFeatureSettings = "liga on, clig on, rlig on, abvs on, blws on, akhn on, cjct on",
        fontSynthesis = FontSynthesis.Weight,
        localeList = devanagariLocale
      )
    )
  }
}
