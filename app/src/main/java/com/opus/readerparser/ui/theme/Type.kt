package com.opus.readerparser.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography()

/**
 * Body text style used inside the novel reader. Font size and family are
 * overridden at runtime from [com.opus.readerparser.domain.model.AppSettings].
 * This serves as the fallback default.
 */
val NovelReaderBodyStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.15.sp,
)
