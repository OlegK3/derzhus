
package ru.derzhus.app

import android.content.Context
import android.content.Intent

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.height
import androidx.compose.ui.unit.DpSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class DerzhusWidget : GlanceAppWidget() {

    // Два размера: компактный счётчик (2x2) и широкий вариант с целью (4x2)
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, WIDE))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val data = Store.data(context)
        val now = System.currentTimeMillis()

        provideContent {
            WidgetContent(
                context = context,
                habit = data.mainHabit,
                goal = data.mainGoal,
                now = now
            )
        }
    }

    companion object {
        val COMPACT = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(240.dp, 110.dp)
    }
}

class DerzhusWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget =
        DerzhusWidget()
}

// Текст лежит на тёмной полупрозрачной подложке поверх фото леса, поэтому светлый
private val WScrim = ColorProvider(Color(0x990D2A1A))
private val WOnPhoto = ColorProvider(Color(0xFFFFFFFF))
private val WOnPhotoSoft = ColorProvider(Color(0xFFE3F1E6))
private val WBar = ColorProvider(Color(0xFF6FD08C))
private val WBarTrack = ColorProvider(Color(0x55FFFFFF))
private val WCardBg = ColorProvider(Color(0xFF0D5039))

@Composable
private fun WidgetContent(
    context: Context,
    habit: Habit?,
    goal: Goal?,
    now: Long
) {
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val wide = LocalSize.current.width >= DerzhusWidget.WIDE.width

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WCardBg)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity(intent = openAppIntent))
    ) {
        Image(
            provider = ImageProvider(R.drawable.bg_widget_forest),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize().cornerRadius(24.dp)
        )
        Box(
            modifier = GlanceModifier.fillMaxSize().background(WScrim).cornerRadius(24.dp)
        ) {}
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (habit == null) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Новая жизнь",
                        style = TextStyle(color = WOnPhoto, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Открой приложение и добавь первую привычку",
                        style = TextStyle(color = WOnPhotoSoft, fontSize = 13.sp)
                    )
                }
            } else {
                val days = Calc.fullDays(habit, now)
                Column(modifier = if (wide) GlanceModifier.width(110.dp) else GlanceModifier.defaultWeight()) {
                    Text(
                        text = "$days",
                        style = TextStyle(color = WOnPhoto, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Дней на новом пути",
                        style = TextStyle(color = WOnPhotoSoft, fontSize = 13.sp)
                    )
                }
                if (wide) {
                    Column(modifier = GlanceModifier.defaultWeight().padding(start = 8.dp)) {
                        if (goal != null) {
                            Text(
                                text = goal.title,
                                maxLines = 1,
                                style = TextStyle(color = WOnPhoto, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${Money.rub(goal.savedKop)} из ${Money.rub(goal.costKop)}",
                                maxLines = 1,
                                style = TextStyle(color = WOnPhotoSoft, fontSize = 13.sp)
                            )
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = goal.progress,
                                modifier = GlanceModifier.fillMaxWidth(),
                                color = WBar,
                                backgroundColor = WBarTrack
                            )
                        } else {
                            val line = if (habit.pricePerUnit > 0) {
                                "Сэкономлено ${Money.rub(Calc.savedKop(habit, now))}"
                            } else {
                                "Обошёлся без ${Calc.count(Calc.avoided(habit, now))} ${habit.unitLabel}"
                            }
                            Text(
                                text = habit.title,
                                maxLines = 1,
                                style = TextStyle(color = WOnPhoto, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = line,
                                maxLines = 1,
                                style = TextStyle(color = WOnPhotoSoft, fontSize = 13.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
