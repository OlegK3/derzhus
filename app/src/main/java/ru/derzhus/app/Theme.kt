package ru.derzhus.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File

// ---------- Палитра (по восьмиэкранному макету) ----------
val Bg = Color(0xFF080B0A)          // графитовый, почти чёрный
val Surface1 = Color(0xFF121715)    // карточка
val Surface2 = Color(0xFF1A221E)    // приподнятая карточка
val Border = Color(0xFF26302B)      // тонкая граница
val Accent = Color(0xFF4ADE80)      // яркий зелёный
val AccentDeep = Color(0xFF22A45A)
val AccentSoft = Color(0xFF16301F)
val AccentLight = Color(0xFF86EFAC)
val Gold = Color(0xFFF2B33D)
val TextMain = Color(0xFFF4F7F5)
val Muted = Color(0xFF9BA7A1)
val Warn = Color(0xFFF59E0B)
val Danger = Color(0xFFEF6B5B)

@Composable
fun DerzhusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color(0xFF05210F),
            background = Bg,
            onBackground = TextMain,
            surface = Surface1,
            onSurface = TextMain,
            surfaceVariant = Surface2,
            onSurfaceVariant = Muted,
            secondaryContainer = AccentSoft,
            onSecondaryContainer = TextMain,
            outline = Border
        ),
        content = content
    )
}

// ---------- Общие компоненты ----------

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val base = modifier
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Surface2, Surface1)))
        .border(1.dp, Border, shape)
    val m = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(m.padding(padding), content = content)
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: String? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val brush = if (enabled)
        Brush.horizontalGradient(listOf(Color(0xFF7CF0A6), Color(0xFF3FD06F)))
    else
        Brush.horizontalGradient(listOf(Color(0xFF26302B), Color(0xFF26302B)))
    Box(
        modifier
            .height(54.dp)
            .clip(shape)
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            (if (leading != null) "$leading  " else "") + text,
            color = if (enabled) Color(0xFF05210F) else Muted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        )
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = TextMain) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(48.dp)
            .clip(shape)
            .border(1.dp, Border, shape)
            .background(Surface1)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) Accent else Surface1)
            .border(1.dp, if (selected) Accent else Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color(0xFF05210F) else TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AppProgress(fraction: Float, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val shape = RoundedCornerShape(50)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(Color(0xFF232C28))) {
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .height(height)
                    .clip(shape)
                    .background(Brush.horizontalGradient(listOf(AccentDeep, Accent, AccentLight)))
            )
        }
    }
}

@Composable
fun AppField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    placeholder: String? = null,
    error: Boolean = false
) {
    Column(modifier) {
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            isError = error,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            suffix = if (suffix != null) {
                { Text(suffix, color = Muted) }
            } else null,
            placeholder = if (placeholder != null) {
                { Text(placeholder, color = Muted) }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Border,
                errorBorderColor = Danger,
                focusedContainerColor = Surface1,
                unfocusedContainerColor = Surface1,
                errorContainerColor = Surface1,
                cursorColor = Accent,
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain,
                errorTextColor = TextMain
            )
        )
    }
}

@Composable
fun AppSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Accent,
            checkedBorderColor = Accent,
            uncheckedThumbColor = Muted,
            uncheckedTrackColor = Surface2,
            uncheckedBorderColor = Border
        )
    )
}

@Composable
fun AppDialog(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(Surface1)
                .border(1.dp, Border, shape)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = TextMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Muted)
                }
            }
            content()
        }
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = TextMain,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) trailing()
    }
}

/** Нажатие без ряби (для текстовых ссылок и строк). */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

@Composable
fun BackLink(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        "← Назад",
        color = Muted,
        fontSize = 15.sp,
        modifier = modifier.clickableNoRipple(onBack).padding(vertical = 8.dp)
    )
}

/** Файл фотографии цели во внутреннем хранилище. */
fun photoFile(ctx: Context, name: String): File =
    File(File(ctx.filesDir, "photos"), File(name).name)

/** Фотография цели; если её нет — тёмная заглушка с горами. */
@Composable
fun GoalImage(file: File?, modifier: Modifier = Modifier, emojiSize: TextUnit = 40.sp) {
    if (file != null && file.exists()) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier.background(Brush.linearGradient(listOf(Color(0xFF1E3A2F), Color(0xFF0F1C17)))),
            contentAlignment = Alignment.Center
        ) {
            Text("🏔️", fontSize = emojiSize)
        }
    }
}

// ---------- Нижняя навигация ----------

enum class MainTab(val label: String) {
    Home("Главная"),
    Goals("Цели"),
    Hobbies("Увлечения"),
    Awards("Достижения"),
    Profile("Профиль")
}

@Composable
fun NavIcon(tab: MainTab, tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    when (tab) {
        MainTab.Home -> Icon(Icons.Outlined.Home, contentDescription = null, tint = tint, modifier = modifier)
        MainTab.Profile -> Icon(Icons.Outlined.Person, contentDescription = null, tint = tint, modifier = modifier)
        MainTab.Goals -> Canvas(modifier) {
            val c = center
            val r = size.minDimension / 2
            val s = Stroke(2.dp.toPx())
            drawCircle(tint, radius = r * 0.9f, center = c, style = s)
            drawCircle(tint, radius = r * 0.55f, center = c, style = s)
            drawCircle(tint, radius = r * 0.18f, center = c)
        }
        MainTab.Hobbies -> Canvas(modifier) {
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(w * 0.15f, h * 0.85f)
                cubicTo(w * 0.1f, h * 0.3f, w * 0.5f, h * 0.1f, w * 0.9f, h * 0.1f)
                cubicTo(w * 0.9f, h * 0.55f, w * 0.6f, h * 0.9f, w * 0.15f, h * 0.85f)
                close()
            }
            drawPath(p, tint, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(
                tint, Offset(w * 0.15f, h * 0.85f), Offset(w * 0.55f, h * 0.45f),
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
            )
        }
        MainTab.Awards -> Canvas(modifier) {
            val w = size.width
            val h = size.height
            val sw = 2.dp.toPx()
            val cup = Path().apply {
                moveTo(w * 0.25f, h * 0.12f)
                lineTo(w * 0.75f, h * 0.12f)
                lineTo(w * 0.75f, h * 0.4f)
                cubicTo(w * 0.75f, h * 0.62f, w * 0.25f, h * 0.62f, w * 0.25f, h * 0.4f)
                close()
            }
            drawPath(cup, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(tint, Offset(w * 0.5f, h * 0.58f), Offset(w * 0.5f, h * 0.8f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.32f, h * 0.88f), Offset(w * 0.68f, h * 0.88f), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun BottomBar(current: MainTab, onSelect: (MainTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF0B100E))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            MainTab.entries.forEach { t ->
                val sel = t == current
                val tint = if (sel) Accent else Muted
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(t) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavIcon(t, tint)
                    Text(
                        t.label,
                        color = tint,
                        fontSize = 10.sp,
                        maxLines = 1,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
