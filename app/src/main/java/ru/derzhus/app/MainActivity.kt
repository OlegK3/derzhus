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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        Notifier.ensureChannel(this)
        setContent {
            DerzhusTheme {
                Surface(color = Bg, modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

/**
 * Дополнительные экраны поверх вкладок кодируются строкой, чтобы пережить поворот экрана:
 * "" — нет, "goal:<id>", "edit:<id>" (пустой id — новая цель), "habit", "craving", "stats".
 */
@Composable
fun AppRoot(vm: MainVm = viewModel()) {
    val ctx = LocalContext.current
    val data = vm.data

    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var overlay by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Секундомер на главном экране + смена календарного дня для наград и виджета
    LaunchedEffect(Unit) {
        var ticks = 0
        while (true) {
            now = System.currentTimeMillis()
            if (ticks % 30 == 0) vm.sync()
            ticks++
            delay(1000)
        }
    }

    // Напоминания: расписание следует настройкам
    LaunchedEffect(data.settings.remindersOn, data.settings.reminderHour) {
        Scheduler.apply(ctx, data.settings)
    }

    // Разрешение на уведомления (Android 13+)
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(data.habits.isNotEmpty()) {
        if (data.habits.isNotEmpty() && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = overlay.isNotEmpty()) { overlay = "" }
    BackHandler(enabled = overlay.isEmpty() && tab != MainTab.Home) { tab = MainTab.Home }

    Box(Modifier.fillMaxSize()) {
        when {
            // Первый запуск: сначала создаём привычку
            data.habits.isEmpty() -> Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                AddHabitScreen(canCancel = false, onCancel = {}, onSave = { vm.addHabit(it) })
            }

            // Один раз после первой привычки (и один раз для тех, кто обновился): необязательный шаг
            !data.settings.distractionAsked -> Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                DistractionStepScreen(
                    onContinue = { text -> vm.updateSettings { it.copy(distraction = text.trim(), distractionAsked = true) } },
                    onSkip = { vm.updateSettings { it.copy(distractionAsked = true) } }
                )
            }

            overlay.isNotEmpty() -> Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Overlay(vm, now, overlay) { overlay = it }
            }

            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).statusBarsPadding()) {
                    when (tab) {
                        MainTab.Home -> HomeScreen(
                            data = data,
                            now = now,
                            onSelectHabit = { vm.setMainHabit(it) },
                            onAddHabit = { overlay = "habit" },
                            onOpenGoal = { overlay = "goal:$it" },
                            onAddGoal = { overlay = "edit:" },
                            onCraving = { overlay = "craving" },
                            onRelapse = { vm.relapse(it.id) },
                            onOpenAwards = { tab = MainTab.Awards },
                            onOpenGoals = { tab = MainTab.Goals },
                            onOpenProfile = { tab = MainTab.Profile }
                        )

                        MainTab.Goals -> GoalsScreen(
                            data = data,
                            onOpen = { overlay = "goal:$it" },
                            onAdd = { overlay = "edit:" },
                            onEdit = { overlay = "edit:$it" },
                            onDelete = { vm.deleteGoal(it) },
                            onMakeMain = { vm.makeMainGoal(it) }
                        )

                        MainTab.Hobbies -> HobbiesScreen(data) { vm.toggleInterest(it) }
                        MainTab.Awards -> AchievementsScreen(data, now)
                        MainTab.Profile -> ProfileScreen(
                            vm = vm,
                            now = now,
                            onAddHabit = { overlay = "habit" },
                            onStats = { overlay = "stats" }
                        )
                    }
                }
                Box(Modifier.background(Surface1).navigationBarsPadding()) {
                    BottomBar(tab) { tab = it }
                }
            }
        }

        // Поздравление с новой наградой (один раз: после показа награды помечаются как увиденные)
        val fresh = Game.awards.filter { it.id in data.unlocked && it.id !in data.celebrated }
        if (fresh.isNotEmpty() && data.habits.isNotEmpty()) {
            CelebrationOverlay(fresh.last()) { vm.markCelebrated() }
        }
    }
}

@Composable
private fun Overlay(vm: MainVm, now: Long, key: String, setOverlay: (String) -> Unit) {
    val data = vm.data
    when {
        key == "habit" -> AddHabitScreen(
            canCancel = true,
            onCancel = { setOverlay("") },
            onSave = { vm.addHabit(it); setOverlay("") }
        )

        key == "craving" -> {
            val habit = data.mainHabit
            if (habit != null) CravingScreen(habit, now, data.settings.distraction) { setOverlay("") } else CloseOverlay(setOverlay)
        }

        key == "stats" -> StatsScreen(data, now) { setOverlay("") }

        key.startsWith("goal:") -> {
            val id = key.removePrefix("goal:")
            if (data.goals.any { it.id == id }) {
                GoalDetailScreen(vm, id, onBack = { setOverlay("") }, onEdit = { setOverlay("edit:$id") })
            } else {
                CloseOverlay(setOverlay)
            }
        }

        key.startsWith("edit:") -> {
            val id = key.removePrefix("edit:")
            GoalEditScreen(vm, id.ifEmpty { null }, onClose = { setOverlay("") })
        }

        else -> CloseOverlay(setOverlay)
    }
}

/** Закрывает экран, который стал недоступен (например, цель удалена), вне композиции. */
@Composable
private fun CloseOverlay(setOverlay: (String) -> Unit) {
    LaunchedEffect(Unit) { setOverlay("") }
}
