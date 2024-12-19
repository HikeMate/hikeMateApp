package ch.hikemate.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ch.hikemate.app.R

val kaushanTitleFontFamily = FontFamily(Font(R.font.kaushan_script, FontWeight.Bold))

// Set of Material typography styles to start with
val Typography =
    Typography(
        displayLarge =
            TextStyle(
                color = Color.White,
                fontFamily = kaushanTitleFontFamily,
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
            ),
        headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
        headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        bodyLarge =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
            ),
        bodyMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
        labelLarge =
            TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
        titleLarge = TextStyle(fontWeight = FontWeight(600), fontSize = 20.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    )
