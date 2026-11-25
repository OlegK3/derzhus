package ru.derzhus.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Максимальная длина личной записи «Что помогает отвлечься». */
const val DISTRACTION_MAX = 300

/**
 * Необязательный шаг «Что поможет тебе отвлечься?». Текст остаётся на устройстве.
 * «Пропустить» и «Продолжить» с пустым полем равнозначны: больше шаг не показывается.
 */
@Composable
fun DistractionStepScreen(onContinue: (String) -> Unit, onSkip: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        PhotoBackdrop(Photos.sunrise, Modifier.fillMaxWidth().height(230.dp), fadeTop = false, fadeBottom = true)
        Column(
            Modifier.padding(horizontal = Dim.PagePadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Что поможет тебе отвлечься?", style = AppType.Title.copy(fontSize = 26.sp), textAlign = TextAlign.Center)
            Text(
                "В трудную минуту важно вспомнить, что помогает именно тебе. Этот шаг необязателен.",
                style = AppType.BodyMuted, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)
            )
            AppField(
                "Мой способ", text, { text = it.take(DISTRACTION_MAX) },
                Modifier.fillMaxWidth(), singleLine = false,
                placeholder = "Например, прогуляться, позвонить другу, послушать музыку…"
            )
            Text(
                "Запись хранится только на этом телефоне.",
                style = AppType.Caption, modifier = Modifier.padding(top = 6.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            PrimaryButton("Продолжить", { onContinue(text) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            GhostButton("Пропустить", onSkip, Modifier.fillMaxWidth(), color = Muted)
            Spacer(Modifier.height(Dim.PagePadding))
        }
    }
}
