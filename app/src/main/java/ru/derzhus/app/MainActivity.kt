package ru.derzhus.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data object Add : Screen
    data object Craving : Screen
    data object AwardList : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        Notifier.ensureChannel(this)
        Scheduler.schedule(this)
        setContent {
            DerzhusTheme {
                Surface(color = Bg, modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var habits by remember { mutableStateOf(Store.load(ctx)) }
    var selectedId by remember { mutableStateOf(habits.firstOrNull()?.id) }
    var screen by remember { mutableStateOf<Screen>(if (habits.isEmpty()) Screen.Add else Screen.Home) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    fun commit(list: List<Habit>) {
        habits = list
        Store.save(ctx, list)
        scope.launch { DerzhusWidget().updateAll(ctx) }
    }

    // Разрешение на уведомления (Android 13+), спрашиваем после первой привычки
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(habits.isNotEmpty()) {
        if (habits.isNotEmpty() && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = screen != Screen.Home && habits.isNotEmpty()) { screen = Screen.Home }

    val habit = habits.firstOrNull { it.id == selectedId } ?: habits.firstOrNull()

    when (screen) {
        Screen.Add -> AddScreen(
            canCancel = habits.isNotEmpty(),
            onCancel = { screen = Screen.Home },
            onSave = { h ->
                commit(habits + h)
                selectedId = h.id
                screen = Screen.Home
            }
        )

        Screen.Home -> if (habit != null) HomeScreen(
            habits = habits,
            habit = habit,
            now = now,
            onSelect = { selectedId = it },
            onAdd = { screen = Screen.Add },
            onCraving = { screen = Screen.Craving },
            onAwards = { screen = Screen.AwardList },
            onUpdate = { upd -> commit(habits.map { if (it.id == upd.id) upd else it }) },
            onDelete = { del ->
                val rest = habits.filter { it.id != del.id }
                commit(rest)
                selectedId = rest.firstOrNull()?.id
                if (rest.isEmpty()) screen = Screen.Add
            }
        )

        Screen.Craving -> if (habit != null) CravingScreen(habit, now) { screen = Screen.Home }

        Screen.AwardList -> if (habit != null) AwardsScreen(habit, now) { screen = Screen.Home }
    }
}
