package ru.derzhus.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.max

/** Сериализация AppData в JSON (формат версии 2). */
object Json {
    private fun <T> JSONArray.objs(f: (JSONObject) -> T): List<T> =
        (0 until length()).map { f(getJSONObject(it)) }

    private fun JSONArray.strs(): List<String> = (0 until length()).map { getString(it) }

    fun toJson(d: AppData): JSONObject {
        val o = JSONObject()
        o.put("version", 2)
        o.put("habits", JSONArray().also { a -> d.habits.forEach { a.put(habitToJson(it)) } })
        o.put("goals", JSONArray().also { a -> d.goals.forEach { a.put(goalToJson(it)) } })
        val s = d.settings
        o.put(
            "settings",
            JSONObject()
                .put("userName", s.userName)
                .put("avatar", s.avatar ?: JSONObject.NULL)
                .put("mainHabitId", s.mainHabitId ?: JSONObject.NULL)
                .put("paceMode", s.paceMode)
                .put("customPaceKop", s.customPaceKop)
                .put("remindersOn", s.remindersOn)
                .put("reminderHour", s.reminderHour)
                .put("interests", JSONArray(s.interests.toList()))
                .put("distraction", s.distraction)
                .put("distractionAsked", s.distractionAsked)
        )
        val u = JSONObject()
        d.unlocked.forEach { (k, v) -> u.put(k, v) }
        o.put("unlocked", u)
        o.put("celebrated", JSONArray(d.celebrated.toList()))
        return o
    }

    private fun habitToJson(h: Habit) = JSONObject()
        .put("id", h.id)
        .put("name", h.name)
        .put("unitLabel", h.unitLabel)
        .put("unitsPerDay", h.unitsPerDay)
        .put("pricePerUnit", h.pricePerUnit)
        .put("startMillis", h.startMillis)
        .put("bestMillis", h.bestMillis)

    private fun goalToJson(g: Goal) = JSONObject()
        .put("id", g.id)
        .put("title", g.title)
        .put("costKop", g.costKop)
        .put("photos", JSONArray(g.photos))
        .put("createdAt", g.createdAt)
        .put("isMain", g.isMain)
        .put("achievedAt", g.achievedAt ?: JSONObject.NULL)
        .put("note", g.note)
        .put(
            "history",
            JSONArray().also { a ->
                g.history.forEach { h ->
                    a.put(
                        JSONObject()
                            .put("id", h.id)
                            .put("ts", h.ts)
                            .put("amountKop", h.amountKop)
                            .put("kind", h.kind)
                            .put("comment", h.comment)
                    )
                }
            }
        )

    fun habitFromJson(o: JSONObject) = Habit(
        id = o.getString("id"),
        name = o.getString("name"),
        unitLabel = o.getString("unitLabel"),
        unitsPerDay = o.getDouble("unitsPerDay"),
        pricePerUnit = o.getDouble("pricePerUnit"),
        startMillis = o.getLong("startMillis"),
        bestMillis = o.optLong("bestMillis", 0L)
    )

    private fun goalFromJson(o: JSONObject) = Goal(
        id = o.getString("id"),
        title = o.getString("title"),
        costKop = o.getLong("costKop"),
        photos = o.optJSONArray("photos")?.strs() ?: emptyList(),
        createdAt = o.optLong("createdAt", 0L),
        isMain = o.optBoolean("isMain", false),
        achievedAt = if (o.isNull("achievedAt")) null else o.getLong("achievedAt"),
        history = o.optJSONArray("history")?.objs { h ->
            Deposit(
                id = h.getString("id"),
                ts = h.getLong("ts"),
                amountKop = h.getLong("amountKop"),
                kind = h.optString("kind", "deposit"),
                comment = h.optString("comment", "")
            )
        } ?: emptyList(),
        note = o.optString("note", "")
    )

    fun fromJson(o: JSONObject): AppData {
        val so = o.optJSONObject("settings")
        val def = Settings()
        val settings = if (so == null) def else Settings(
            userName = so.optString("userName", def.userName),
            avatar = if (so.isNull("avatar")) null else so.getString("avatar"),
            mainHabitId = if (so.isNull("mainHabitId")) null else so.getString("mainHabitId"),
            paceMode = so.optString("paceMode", def.paceMode),
            customPaceKop = so.optLong("customPaceKop", 0L),
            remindersOn = so.optBoolean("remindersOn", true),
            reminderHour = so.optInt("reminderHour", 9),
            interests = so.optJSONArray("interests")?.strs()?.toSet() ?: emptySet(),
            distraction = so.optString("distraction", ""),
            distractionAsked = so.optBoolean("distractionAsked", false)
        )
        val uo = o.optJSONObject("unlocked")
        val unlocked: Map<String, Long> =
            if (uo == null) emptyMap() else uo.keys().asSequence().associateWith { uo.getLong(it) }
        return AppData(
            habits = o.optJSONArray("habits")?.objs { habitFromJson(it) } ?: emptyList(),
            goals = o.optJSONArray("goals")?.objs { goalFromJson(it) } ?: emptyList(),
            settings = settings,
            unlocked = unlocked,
            celebrated = o.optJSONArray("celebrated")?.strs()?.toSet() ?: emptySet()
        )
    }
}

/** Локальное хранилище: data.json + папка photos. Работает полностью офлайн. */
class Repo(private val ctx: Context) {
    private val file = File(ctx.filesDir, "data.json")
    private val backup = File(ctx.filesDir, "data.json.bak")
    val photosDir: File = File(ctx.filesDir, "photos").also { it.mkdirs() }

    fun load(): AppData {
        if (file.exists()) {
            try {
                return Json.fromJson(JSONObject(file.readText()))
            } catch (e: Exception) {
                runCatching { file.copyTo(File(ctx.filesDir, "data.json.corrupt"), overwrite = true) }
            }
            if (backup.exists()) {
                try {
                    return Json.fromJson(JSONObject(backup.readText()))
                } catch (e: Exception) {
                    // продолжаем: попробуем миграцию
                }
            }
        }
        return migrateLegacy()
    }

    /** Миграция данных первой версии приложения (SharedPreferences "derzhus"/"habits"). */
    private fun migrateLegacy(): AppData {
        val raw = ctx.getSharedPreferences("derzhus", Context.MODE_PRIVATE).getString("habits", null)
            ?: return AppData()
        return try {
            val arr = JSONArray(raw)
            val habits = (0 until arr.length()).map { Json.habitFromJson(arr.getJSONObject(it)) }
            AppData(habits = habits, settings = Settings(mainHabitId = habits.firstOrNull()?.id))
        } catch (e: Exception) {
            AppData()
        }
    }

    fun save(d: AppData) {
        val tmp = File(ctx.filesDir, "data.json.tmp")
        tmp.writeText(Json.toJson(d).toString())
        if (file.exists()) runCatching { file.copyTo(backup, overwrite = true) }
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    // ---------- Фотографии ----------

    fun photo(name: String): File = File(photosDir, File(name).name)

    /** Копирует выбранное фото в приватное хранилище (с уменьшением и учётом поворота). */
    fun importPhoto(uri: Uri): String? {
        return try {
            val resolver = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > 2000) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            val orientation = try {
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            val deg = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val out: Bitmap = if (deg == 0f) bmp else
                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
            val name = UUID.randomUUID().toString() + ".jpg"
            FileOutputStream(File(photosDir, name)).use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            name
        } catch (e: Exception) {
            null
        }
    }

    fun deletePhotos(names: Collection<String>) {
        names.forEach { runCatching { photo(it).delete() } }
    }

    fun purgeOrphans(d: AppData) {
        val used = (d.goals.flatMap { it.photos } + listOfNotNull(d.settings.avatar)).toSet()
        photosDir.listFiles()?.forEach { if (it.name !in used) it.delete() }
    }

    // ---------- Экспорт / импорт ----------

    /** Архив: data.json + папка photos. Фотографии сохраняются вместе с данными. */
    fun exportZip(out: OutputStream, d: AppData) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(Json.toJson(d).toString().toByteArray())
            zip.closeEntry()
            val used = (d.goals.flatMap { it.photos } + listOfNotNull(d.settings.avatar)).distinct()
            used.forEach { n ->
                val f = photo(n)
                if (f.exists()) {
                    zip.putNextEntry(ZipEntry("photos/" + f.name))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /** Читает архив, копирует фото и возвращает данные. null — файл не подходит. */
    fun importZip(input: InputStream): AppData? {
        val tmpDir = File(ctx.cacheDir, "import")
        tmpDir.deleteRecursively()
        tmpDir.mkdirs()
        var json: String? = null
        try {
            ZipInputStream(input).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    val name = e.name
                    if (name == "data.json") {
                        json = zip.readBytes().toString(Charsets.UTF_8)
                    } else if (name.startsWith("photos/") && !e.isDirectory) {
                        val base = name.removePrefix("photos/")
                        val safe = base.isNotEmpty() && !base.contains('/') &&
                            !base.contains('\\') && !base.contains("..")
                        if (safe) File(tmpDir, base).outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    e = zip.nextEntry
                }
            }
            val text = json ?: return null
            val data = Json.fromJson(JSONObject(text))
            tmpDir.listFiles()?.forEach { it.copyTo(File(photosDir, it.name), overwrite = true) }
            return data
        } catch (e: Exception) {
            return null
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}

/** Короткий доступ для виджета и уведомлений. */
object Store {
    fun load(ctx: Context): List<Habit> = Repo(ctx).load().habits
    fun main(ctx: Context): Habit? = Repo(ctx).load().mainHabit
    fun data(ctx: Context): AppData = Repo(ctx).load()
}
