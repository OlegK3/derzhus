package ru.derzhus.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF0E1513)
val Surface1 = Color(0xFF17211E)
val Surface2 = Color(0xFF1F2D29)
val Accent = Color(0xFF5EEAD4)
val TextMain = Color(0xFFE6F2EF)
val Muted = Color(0xFF8AA39C)
val Warn = Color(0xFFF59E0B)

@Composable
fun DerzhusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color(0xFF00201C),
            background = Bg,
            onBackground = TextMain,
            surface = Surface1,
            onSurface = TextMain,
            surfaceVariant = Surface2,
            onSurfaceVariant = Muted,
            secondaryContainer = Color(0xFF1F3D38),
            onSecondaryContainer = TextMain,
            outline = Color(0xFF3A4E49)
        ),
        content = content
    )
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Surface1)
            .padding(16.dp)
    ) {
        Text(label, color = Muted, fontSize = 13.sp)
        Text(value, color = TextMain, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
