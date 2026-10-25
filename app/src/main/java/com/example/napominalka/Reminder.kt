package com.example.napominalka

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Reminder(
    val id: Long,
    val text: String,
    val time: Long,
    val done: Boolean = false
)

object ReminderStore {
    private const val PREFS = "reminders"
    private const val KEY = "list"

    fun load(ctx: Context): List<Reminder> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Reminder(o.getLong("id"), o.getString("text"), o.getLong("time"), o.optBoolean("done"))
        }.sortedBy { it.time }
    }

    fun save(ctx: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("text", r.text)
                    .put("time", r.time)
                    .put("done", r.done)
            )
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun markDone(ctx: Context, id: Long) {
        save(ctx, load(ctx).map { if (it.id == id) it.copy(done = true) else it })
    }
}
