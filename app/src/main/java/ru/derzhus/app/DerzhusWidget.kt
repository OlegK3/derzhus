
package ru.derzhus.app

import android.content.Context
import android.content.Intent

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class DerzhusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val habit = Store.main(context)
        val now = System.currentTimeMillis()

        provideContent {
            WidgetContent(
                context = context,
                habit = habit,
                now = now
            )
        }
    }
}

class DerzhusWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget =
        DerzhusWidget()
}

private val WBg = ColorProvider(Color(0xFF0E1513))
private val WAccent = ColorProvider(Color(0xFF5EEAD4))
private val WText = ColorProvider(Color(0xFFE6F2EF))
private val WMuted = ColorProvider(Color(0xFF8AA39C))

@Composable
private fun WidgetContent(
    context: Context,
    habit: Habit?,
    now: Long
) {

    val openAppIntent = Intent(
        context,
        MainActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WBg)
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(
                actionStartActivity(
                    intent = openAppIntent
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (habit == null) {

            Text(
                text = "Держусь",
                style = TextStyle(
                    color = WAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "Открой приложение и добавь первую цель",
                style = TextStyle(
                    color = WMuted,
                    fontSize = 13.sp
                )
            )

        } else {

            val days = Calc.fullDays(habit, now)

            Text(
                text = habit.title,
                style = TextStyle(
                    color = WMuted,
                    fontSize = 13.sp
                )
            )

            Row(
                verticalAlignment = Alignment.Bottom
            ) {

                Text(
                    text = "$days",
                    style = TextStyle(
                        color = WAccent,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(
                    modifier = GlanceModifier.width(6.dp)
                )

                Text(
                    text = Calc.dayWord(days),
                    style = TextStyle(
                        color = WText,
                        fontSize = 16.sp
                    )
                )
            }

            val line = if (habit.pricePerUnit > 0) {
                "Сэкономлено ${
                    Money.rub(Calc.savedKop(habit, now))
                }"
            } else {
                "Обошёлся без ${
                    Calc.count(Calc.avoided(habit, now))
                } ${habit.unitLabel}"
            }

            Text(
                text = line,
                style = TextStyle(
                    color = WText,
                    fontSize = 14.sp
                )
            )
        }
    }
}
