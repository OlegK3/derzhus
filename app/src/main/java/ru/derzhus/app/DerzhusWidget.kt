package ru.derzhus.app

import android.content.Context
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
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val habit = Store.load(context).firstOrNull()
        val now = System.currentTimeMillis()
        provideContent { WidgetContent(habit, now) }
    }
}

class DerzhusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DerzhusWidget()
}

private val WBg = ColorProvider(Color(0xFF0E1513))
private val WAccent = ColorProvider(Color(0xFF5EEAD4))
private val WText = ColorProvider(Color(0xFFE6F2EF))
private val WMuted = ColorProvider(Color(0xFF8AA39C))

@Composable
private fun WidgetContent(h: Habit?, now: Long) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WBg)
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (h == null) {
            Text("Держусь", style = TextStyle(color = WAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold))
            Text("Открой приложение и добавь первую цель", style = TextStyle(color = WMuted, fontSize = 13.sp))
        } else {
            val d = Calc.fullDays(h, now)
            Text(h.title, style = TextStyle(color = WMuted, fontSize = 13.sp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$d", style = TextStyle(color = WAccent, fontSize = 44.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.width(6.dp))
                Text(Calc.dayWord(d), style = TextStyle(color = WText, fontSize = 16.sp))
            }
            val line = if (h.pricePerUnit > 0)
                "Сэкономлено ${Calc.money(Calc.saved(h, now))}"
            else
                "Обошёлся без ${Calc.count(Calc.avoided(h, now))} ${h.unitLabel}"
            Text(line, style = TextStyle(color = WText, fontSize = 14.sp))
        }
    }
}
