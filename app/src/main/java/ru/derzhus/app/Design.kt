package ru.derzhus.app

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Размеры из design_tokens.json. */
object Dim {
    val CardRadius = 20.dp
    val ButtonRadius = 16.dp
    val PagePadding = 16.dp
    val CardGap = 12.dp
    val BottomNavHeight = 72.dp
    val MinTouch = 48.dp
}

/** Типографика из design_tokens.json (system sans-serif / Roboto). Размеры в sp масштабируются системой. */
object AppType {
    val Title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
    val Section = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
    val Body = TextStyle(fontSize = 14.sp, color = TextMain)
    val BodyMuted = TextStyle(fontSize = 14.sp, color = Muted)
    val Caption = TextStyle(fontSize = 12.sp, color = Muted)
    val Metric = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextMain)
}

/** Стартовые фотографии из дизайн-комплекта. Фото пользователя всегда важнее. */
object Photos {
    @DrawableRes val sunrise = R.drawable.photo_sunrise
    @DrawableRes val forest = R.drawable.photo_forest
    @DrawableRes val motorcycle = R.drawable.photo_motorcycle
    @DrawableRes val boat = R.drawable.photo_boat
    @DrawableRes val diving = R.drawable.photo_diving
    @DrawableRes val hiking = R.drawable.photo_hiking
}

/** Единая векторная иконка из набора комплекта; цвет задаётся tint. */
@Composable
fun AppIcon(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    tint: Color = Accent,
    size: Dp = 24.dp,
    contentDescription: String? = null
) {
    Icon(
        painter = painterResource(id),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/** Иконка в мягком зелёном квадрате (метрики, строки настроек). */
@Composable
fun IconBadge(@DrawableRes icon: Int, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(AccentSoft),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(icon, tint = AccentDeep, size = size * 0.55f)
    }
}

/**
 * Пейзаж-подложка: фото во всю область с плавным переходом в фон страницы
 * сверху и/или снизу, чтобы текст рядом оставался читаемым.
 */
@Composable
fun PhotoBackdrop(
    @DrawableRes photo: Int,
    modifier: Modifier = Modifier,
    fadeTop: Boolean = false,
    fadeBottom: Boolean = true
) {
    Box(modifier) {
        Image(
            painter = painterResource(photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (fadeTop) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Bg, 0.3f to Color.Transparent)
                )
            )
        }
        if (fadeBottom) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.65f to Color.Transparent, 1f to Bg)
                )
            )
        }
    }
}
