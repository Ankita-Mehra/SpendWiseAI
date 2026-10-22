package com.example.receiptai.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Tiny JSON-file persistence so the project has zero build-plugin requirements.
 * Once you're comfortable, swap this for Room (same public API: expenses, add, delete).
 */
class ExpenseStore(context: Context) {

    private val file = File(context.filesDir, "expenses.json")
    private val mutex = Mutex()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    suspend fun load() = mutex.withLock {
        val loaded = withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext emptyList<Expense>()
            try {
                val array = JSONArray(file.readText())
                List(array.length()) { i ->
                    val o = array.getJSONObject(i)
                    Expense(
                        id = o.getLong("id"),
                        store = o.getString("store"),
                        date = o.getString("date"),
                        total = o.getDouble("total"),
                        category = o.getString("category")
                    )
                }
            } catch (e: Exception) {
                emptyList() // corrupted file: start fresh instead of crashing
            }
        }
        _expenses.value = loaded.sortedNewestFirst()
    }

    suspend fun add(store: String, date: String, total: Double, category: String) = mutex.withLock {
        val expense = Expense(System.currentTimeMillis(), store, date, total, category)
        val updated = (_expenses.value + expense).sortedNewestFirst()
        _expenses.value = updated
        persist(updated)
    }

    suspend fun delete(id: Long) = mutex.withLock {
        val updated = _expenses.value.filterNot { it.id == id }
        _expenses.value = updated
        persist(updated)
    }

    private suspend fun persist(list: List<Expense>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("store", it.store)
                    .put("date", it.date)
                    .put("total", it.total)
                    .put("category", it.category)
            )
        }
        // write to a temp file first so a crash can't leave a half-written file
        val tmp = File(file.parentFile, "expenses.json.tmp")
        tmp.writeText(array.toString())
        tmp.renameTo(file)
    }

    private fun List<Expense>.sortedNewestFirst() =
        sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.id })
}
