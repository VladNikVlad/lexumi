package com.lexumi.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PillShape = RoundedCornerShape(28.dp)
val CardShape = RoundedCornerShape(20.dp)

val LexumiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = CardShape,
    large = PillShape,
)
