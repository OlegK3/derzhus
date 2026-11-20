package ru.derzhus.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/** Единственный источник состояния: UI только читает data и вызывает методы ниже. */
class MainVm(app: Application) : AndroidViewModel(app) {
    private val repo = Repo(app)

    var data by mutableStateOf(AppData())
        private set

    init {
        val synced = Game.sync(repo.load(), System.currentTimeMillis())
        data = synced
        repo.save(synced) // создаёт data.json (в том числе после миграции со старой версии)
    }

    fun update(f: (AppData) -> AppData) {
        val n = Game.sync(f(data), System.currentTimeMillis())
        if (n != data) {
            data = n
            repo.save(n)
            refreshWidget()
        }
    }

    fun sync() = update { it }

    private fun refreshWidget() {
        viewModelScope.launch {
            try {
                DerzhusWidget().updateAll(getApplication<Application>())
            } catch (e: Exception) {
                // виджет может быть не добавлен на экран
            }
        }
    }

    // ---------- Привычки ----------

    fun addHabit(h: Habit) = update { d ->
        val s = if (d.settings.mainHabitId == null || d.habits.isEmpty()) d.settings.copy(mainHabitId = h.id) else d.settings
        d.copy(habits = d.habits + h, settings = s)
    }

    fun updateHabit(h: Habit) = update { d -> d.copy(habits = d.habits.map { if (it.id == h.id) h else it }) }

    fun setMainHabit(id: String) = update { d -> d.copy(settings = d.settings.copy(mainHabitId = id)) }

    fun deleteHabit(id: String) = update { d ->
        val rest = d.habits.filter { it.id != id }
        val main = if (d.settings.mainHabitId == id) rest.firstOrNull()?.id else d.settings.mainHabitId
        d.copy(habits = rest, settings = d.settings.copy(mainHabitId = main))
    }

    /** Срыв без наказаний: рекорд сохраняется, начинается новая серия. */
    fun relapse(id: String) = update { d ->
        val now = System.currentTimeMillis()
        d.copy(
            habits = d.habits.map {
                if (it.id == id) it.copy(startMillis = now, bestMillis = Calc.recordMs(it, now)) else it
            }
        )
    }

    // ---------- Цели ----------

    private fun newDeposit(amountKop: Long, kind: String, comment: String) =
        Deposit(UUID.randomUUID().toString(), System.currentTimeMillis(), amountKop, kind, comment)

    /** Создание (existing == null) или редактирование цели. Главной может быть только одна. */
    fun saveGoal(
        existing: Goal?,
        title: String,
        costKop: Long,
        savedKop: Long,
        photos: List<String>,
        isMain: Boolean,
        note: String
    ) {
        if (existing != null) {
            val removed = existing.photos.filter { it !in photos }
            repo.deletePhotos(removed)
        }
        update { d ->
            val now = System.currentTimeMillis()
            val goal: Goal = if (existing == null) {
                Goal(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    costKop = costKop,
                    photos = photos,
                    createdAt = now,
                    isMain = isMain,
                    achievedAt = null,
                    history = if (savedKop > 0) listOf(newDeposit(savedKop, "initial", "Начальная сумма")) else emptyList(),
                    note = note
                )
            } else {
                val cur = d.goals.firstOrNull { it.id == existing.id } ?: existing
                val delta = savedKop - cur.savedKop
                cur.copy(
                    title = title,
                    costKop = costKop,
                    photos = photos,
                    isMain = isMain,
                    note = note,
                    history = if (delta != 0L) cur.history + newDeposit(delta, "adjust", "Корректировка") else cur.history
                )
            }
            val list = if (existing == null) d.goals + goal else d.goals.map { if (it.id == goal.id) goal else it }
            d.copy(goals = list.map { if (it.id != goal.id && goal.isMain) it.copy(isMain = false) else it })
        }
    }

    fun updateGoal(id: String, f: (Goal) -> Goal) =
        update { d -> d.copy(goals = d.goals.map { if (it.id == id) f(it) else it }) }

    fun makeMainGoal(id: String) =
        update { d -> d.copy(goals = d.goals.map { it.copy(isMain = it.id == id) }) }

    fun deleteGoal(id: String) {
        val g = data.goals.firstOrNull { it.id == id } ?: return
        repo.deletePhotos(g.photos)
        update { d -> d.copy(goals = d.goals.filter { it.id != id }) }
    }

    fun addDeposit(goalId: String, amountKop: Long, comment: String) =
        updateGoal(goalId) { it.copy(history = it.history + newDeposit(amountKop, "deposit", comment)) }

    /** «Изменить цель»: новая стоимость и/или корректировка накопленной суммы (с записью в историю). */
    fun changeGoal(goalId: String, title: String, costKop: Long, newSavedKop: Long) =
        updateGoal(goalId) { g ->
            val delta = newSavedKop - g.savedKop
            g.copy(
                title = title,
                costKop = costKop,
                history = if (delta != 0L) g.history + newDeposit(delta, "adjust", "Корректировка") else g.history
            )
        }

    // ---------- Фотографии ----------

    suspend fun importPhotos(uris: List<Uri>): List<String> =
        withContext(Dispatchers.IO) { uris.mapNotNull { repo.importPhoto(it) } }

    fun deletePhotoFiles(names: Collection<String>) = repo.deletePhotos(names)

    // ---------- Настройки ----------

    fun updateSettings(f: (Settings) -> Settings) = update { d -> d.copy(settings = f(d.settings)) }

    fun toggleInterest(id: String) = updateSettings {
        it.copy(interests = if (id in it.interests) it.interests - id else it.interests + id)
    }

    fun markCelebrated() = update { d -> d.copy(celebrated = d.celebrated + d.unlocked.keys) }

    /** Параметры расчёта привычки: количество в день и расходы в день (₽ в копейках). */
    fun setHabitCosts(id: String, unitsPerDay: Double, perDayKop: Long) = update { d ->
        val price = BigDecimal(perDayKop).movePointLeft(2)
            .divide(BigDecimal.valueOf(unitsPerDay), 6, RoundingMode.HALF_UP).toDouble()
        d.copy(habits = d.habits.map { if (it.id == id) it.copy(unitsPerDay = unitsPerDay, pricePerUnit = price) else it })
    }

    // ---------- Резервная копия ----------

    suspend fun exportTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val snapshot = data
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { repo.exportZip(it, snapshot) } != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importFrom(uri: Uri): Boolean {
        val loaded: AppData = withContext(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { repo.importZip(it) }
            } catch (e: Exception) {
                null
            }
        } ?: return false
        val synced = Game.sync(loaded, System.currentTimeMillis())
        data = synced
        repo.save(synced)
        repo.purgeOrphans(synced)
        refreshWidget()
        return true
    }
}
