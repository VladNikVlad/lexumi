package com.lexumi.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lexumi.app.R

// Handwritten brand font, matching the Lexumi logo wordmark
val MarckScript = FontFamily(Font(R.font.marck_script, FontWeight.Normal))
val BadScript = FontFamily(Font(R.font.bad_script, FontWeight.Normal))

val LexumiTypography = Typography(
    displayLarge = TextStyle(fontFamily = MarckScript, fontWeight = FontWeight.Normal, fontSize = 56.sp),
    headlineLarge = TextStyle(fontFamily = MarckScript, fontWeight = FontWeight.Normal, fontSize = 34.sp),
    headlineMedium = TextStyle(fontFamily = MarckScript, fontWeight = FontWeight.Normal, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = MarckScript, fontWeight = FontWeight.Normal, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp),
)
