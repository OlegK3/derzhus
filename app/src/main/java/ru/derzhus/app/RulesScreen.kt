package ru.derzhus.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val rules = listOf(
    "Не «чуть-чуть»" to "Один раз почти всегда возвращает всю привычку. Решение «сегодня нет» проще принять, чем решение «ровно столько, сколько нужно».",
    "Знай свои триггеры" to "Заранее запиши, когда тянет сильнее всего: после работы, в компании, в плохом настроении. Для каждого триггера придумай замену из раздела «Увлечения».",
    "Проси поддержку" to "Если тянет, напиши или позвони близкому человеку раньше, чем станет трудно. Срыв не делает тебя слабым, а серия и рекорд остаются с тобой."
)

/** «Три правила» для трудных моментов. */
@Composable
fun RulesScreen(onBack: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        BackLink(onBack)
        Text("Три правила", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "Короткая опора на каждый день.",
            color = Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rules.forEachIndexed { i, (title, text) ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("${i + 1}", color = Accent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(text, color = Muted, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
