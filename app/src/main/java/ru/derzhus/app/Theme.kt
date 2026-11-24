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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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

// ---------- Палитра (design_tokens.json, светлый вариант №1) ----------
val Bg = Color(0xFFF7F5EF)          // background
val Surface1 = Color(0xFFFFFFFF)    // surface
val Surface2 = Color(0xFFF0F3EB)    // surface_alt
val Border = Color(0xFFDDE4DA)      // outline
val Accent = Color(0xFF168A43)      // primary
val AccentDeep = Color(0xFF075A36)  // primary_dark
val AccentSoft = Color(0xFFD8F2D6)  // primary_light
val AccentLight = Color(0xFF3FBF63) // яркий конец градиента прогресса
val Gold = Color(0xFFE8AE31)        // warning
val TextMain = Color(0xFF17231D)    // text_primary
val Muted = Color(0xFF55635B)       // text_secondary
val Warn = Color(0xFFE8AE31)
val Danger = Color(0xFFB3392B)
val OnAccent = Color(0xFFFFFFFF)
val ProgressTrack = Color(0xFFE3E9DE)
val Scrim = Color(0x300F2518)       // scrim

@Composable
fun DerzhusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent,
            onPrimary = OnAccent,
            primaryContainer = AccentSoft,
            onPrimaryContainer = AccentDeep,
            secondary = AccentDeep,
            onSecondary = OnAccent,
            secondaryContainer = AccentSoft,
            onSecondaryContainer = AccentDeep,
            background = Bg,
            onBackground = TextMain,
            surface = Surface1,
            onSurface = TextMain,
            surfaceVariant = Surface2,
            onSurfaceVariant = Muted,
            surfaceContainerLowest = Surface1,
            surfaceContainerLow = Surface1,
            surfaceContainer = Surface1,
            surfaceContainerHigh = Surface1,
            surfaceContainerHighest = Surface2,
            outline = Border,
            outlineVariant = Border,
            error = Danger
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
    val shape = RoundedCornerShape(Dim.CardRadius)
    val base = modifier
        .shadow(2.dp, shape, ambientColor = Color(0x33203A2A), spotColor = Color(0x33203A2A))
        .clip(shape)
        .background(Surface1)
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
    val shape = RoundedCornerShape(Dim.ButtonRadius)
    Box(
        modifier
            .defaultMinSize(minHeight = 54.dp)
            .clip(shape)
            .background(if (enabled) Accent else Surface2)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            (if (leading != null) "$leading  " else "") + text,
            color = if (enabled) OnAccent else Muted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = TextMain) {
    val shape = RoundedCornerShape(Dim.ButtonRadius)
    Box(
        modifier
            .defaultMinSize(minHeight = Dim.MinTouch)
            .clip(shape)
            .border(1.dp, Border, shape)
            .background(Surface1)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(if (selected) Accent else Surface1)
            .border(1.dp, if (selected) Accent else Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) OnAccent else TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AppProgress(fraction: Float, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val shape = RoundedCornerShape(50)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(ProgressTrack)) {
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .height(height)
                    .clip(shape)
                    .background(Brush.horizontalGradient(listOf(Accent, AccentLight)))
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
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFB9C4BC),
            uncheckedBorderColor = Color(0xFFB9C4BC)
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

/** Фотография цели; если её нет, показывается нейтральный пейзаж из комплекта. */
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
        Image(
            painter = painterResource(Photos.sunrise),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
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

@DrawableRes
fun MainTab.iconRes(): Int = when (this) {
    MainTab.Home -> R.drawable.ic_home
    MainTab.Goals -> R.drawable.ic_goals
    MainTab.Hobbies -> R.drawable.ic_hobbies
    MainTab.Awards -> R.drawable.ic_achievements
    MainTab.Profile -> R.drawable.ic_profile
}

@Composable
fun NavIcon(tab: MainTab, tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Icon(painterResource(tab.iconRes()), contentDescription = null, tint = tint, modifier = modifier)
}

@Composable
fun BottomBar(current: MainTab, onSelect: (MainTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Surface1)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
        Row(
            Modifier.fillMaxWidth().height(Dim.BottomNavHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { t ->
                val sel = t == current
                val tint = if (sel) Accent else Muted
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = Dim.MinTouch)
                        .selectable(selected = sel, role = Role.Tab, onClick = { onSelect(t) })
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NavIcon(t, tint)
                    Text(
                        t.label,
                        color = tint,
                        fontSize = 11.sp,
                        maxLines = 1,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
