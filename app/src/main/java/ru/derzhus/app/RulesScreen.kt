package ru.derzhus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val rules = listOf(
    "Не «чуть-чуть»" to "Один раз почти всегда возвращает всю привычку. Решение «сегодня нет» проще принять, чем решение «ровно столько, сколько нужно».",
    "Знай свои триггеры" to "Заранее запиши, когда тянет сильнее всего: после работы, в компании, в плохом настроении. Для каждого триггера придумай замену из раздела «Увлечения».",
    "Проси поддержку" to "Если тянет, напиши или позвони близкому человеку раньше, чем станет трудно. Срыв не делает тебя слабым, а серия и рекорд остаются с тобой."
)

/** «Три правила» для трудных моментов. Фото сверху, заголовок и стрелка на светлой подложке. */
@Composable
fun RulesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth()) {
            PhotoBackdrop(Photos.sunrise, Modifier.fillMaxWidth().height(180.dp), fadeTop = true, fadeBottom = true)
            BackHeader("Три правила", onBack)
        }
        Column(
            Modifier.padding(horizontal = Dim.PagePadding).padding(bottom = Dim.PagePadding),
            verticalArrangement = Arrangement.spacedBy(Dim.CardGap)
        ) {
            Text("Короткая опора на каждый день.", style = AppType.BodyMuted)
            rules.forEachIndexed { i, (title, text) ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(Accent),
                            contentAlignment = Alignment.Center
                        ) { Text("${i + 1}", color = OnAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(title, style = AppType.Section)
                            Text(text, style = AppType.BodyMuted.copy(lineHeight = 21.sp), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
