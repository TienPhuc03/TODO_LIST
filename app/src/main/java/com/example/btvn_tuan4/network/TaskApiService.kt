package com.example.btvn_tuan4.network

import android.util.Log
import com.example.btvn_tuan4.model.SubTask
import com.example.btvn_tuan4.model.TodoTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object TaskApiService {

    const val BASE_URL = "https://share.tryapps.store/mock/teach"
    const val API_TOKEN = "TuanTruong-uth-student"

    private const val TIMEOUT_MS = 10000

    // 1. GET /mock/teach/tasks
    suspend fun fetchAllTasks(): Result<List<TodoTask>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/tasks")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $API_TOKEN")
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                    it.readText()
                }
                val parsed = parseTasksJson(body)
                Log.i("TaskApiService", "GET /tasks thanh cong (HTTP $code): Da tai ${parsed.size} cong viec tu API that")
                Result.success(parsed)
            } else {
                val msg = "GET /tasks that bai voi HTTP $code"
                Log.e("TaskApiService", msg)
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("TaskApiService", "Loi ket noi GET /tasks: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 2. GET /mock/teach/tasks/{id}
    suspend fun fetchTaskById(taskId: String): Result<TodoTask> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/tasks/$taskId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $API_TOKEN")
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                    it.readText()
                }
                val trimmed = body.trim()
                val task = if (trimmed.startsWith("[")) {
                    val list = parseTasksJson(trimmed)
                    list.find { it.id == taskId } ?: list.firstOrNull()
                        ?: throw Exception("Khong tim thay cong viec id=$taskId")
                } else {
                    val jsonObj = JSONObject(trimmed)
                    val dataObj = jsonObj.optJSONObject("data") ?: jsonObj
                    parseSingleTaskJson(dataObj)
                }
                Log.i("TaskApiService", "GET /tasks/$taskId thanh cong (HTTP $code): ${task.title}")
                Result.success(task)
            } else {
                val msg = "GET /tasks/$taskId that bai voi HTTP $code"
                Log.e("TaskApiService", msg)
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("TaskApiService", "Loi ket noi GET /tasks/$taskId: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 3. POST /mock/teach/tasks
    suspend fun createTaskOnApi(task: TodoTask): Result<TodoTask> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/tasks")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $API_TOKEN")
            }

            val payload = taskToJsonObject(task)
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            Log.i("TaskApiService", "POST /tasks thanh cong (HTTP $code)")
            if (code in 200..299) {
                Result.success(task)
            } else {
                Result.failure(Exception("POST /tasks tra ve HTTP $code"))
            }
        } catch (e: Exception) {
            Log.e("TaskApiService", "Loi ket noi POST /tasks: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 4. PATCH /mock/teach/tasks/{id}
    suspend fun patchTaskOnApi(taskId: String, updatedTask: TodoTask): Result<TodoTask> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/tasks/$taskId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                try {
                    requestMethod = "PATCH"
                } catch (_: Exception) {
                    requestMethod = "POST"
                    setRequestProperty("X-HTTP-Method-Override", "PATCH")
                }
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $API_TOKEN")
            }

            val payload = taskToJsonObject(updatedTask)
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            Log.i("TaskApiService", "PATCH /tasks/$taskId thanh cong (HTTP $code)")
            if (code in 200..299) {
                Result.success(updatedTask)
            } else {
                Result.failure(Exception("PATCH /tasks/$taskId tra ve HTTP $code"))
            }
        } catch (e: Exception) {
            Log.e("TaskApiService", "Loi ket noi PATCH /tasks/$taskId: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 5. DELETE /mock/teach/tasks/{id}
    suspend fun deleteTaskOnApi(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/tasks/$taskId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $API_TOKEN")
            }

            val code = conn.responseCode
            Log.i("TaskApiService", "DELETE /tasks/$taskId thanh cong (HTTP $code)")
            if (code in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("DELETE /tasks/$taskId tra ve HTTP $code"))
            }
        } catch (e: Exception) {
            Log.e("TaskApiService", "Loi ket noi DELETE /tasks/$taskId: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Chuyen doi danh sach JSON tu API sang List<TodoTask>
    private fun parseTasksJson(rawJson: String): List<TodoTask> {
        val trimmed = rawJson.trim()
        val jsonArray = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val root = JSONObject(trimmed)
            root.optJSONArray("data") ?: root.optJSONArray("tasks") ?: JSONArray()
        }

        val list = mutableListOf<TodoTask>()
        for (i in 0 until jsonArray.length()) {
            val itemObj = jsonArray.optJSONObject(i) ?: continue
            list.add(parseSingleTaskJson(itemObj, index = i))
        }
        return list
    }

    // Chuyen doi 1 doi tuong JSON sang TodoTask theo dung 10 truong cua API
    private fun parseSingleTaskJson(obj: JSONObject, index: Int = 0): TodoTask {
        val id = obj.opt("id")?.toString()?.takeIf { it.isNotBlank() } ?: "${index + 1}"
        val title = obj.optString("title", "")
        val note = obj.optString("note", "")
        val rawCategory = obj.optString("category", obj.optString("categoryName", "study"))
        val date = obj.optString("date", "2026-10-20")
        val time = obj.optString("time", "14:00")
        val rawPriority = obj.optString("priority", "medium")
        val remind = obj.optBoolean("remind", obj.optBoolean("remindMe", true))
        val done = obj.optBoolean("done", obj.optBoolean("isCompleted", false))

        val subtasksArray = obj.optJSONArray("subtasks")
        val subtasks = mutableListOf<SubTask>()
        if (subtasksArray != null) {
            for (j in 0 until subtasksArray.length()) {
                val subObj = subtasksArray.optJSONObject(j)
                if (subObj != null) {
                    subtasks.add(
                        SubTask(
                            id = subObj.opt("id")?.toString()?.takeIf { it.isNotBlank() } ?: "sub_${id}_${j + 1}",
                            title = subObj.optString("title", subObj.optString("text", "")),
                            done = subObj.optBoolean("done", subObj.optBoolean("isCompleted", false))
                        )
                    )
                }
            }
        }

        return TodoTask(
            id = id,
            title = title,
            note = note,
            categoryName = TodoTask.categoryFromApi(rawCategory),
            date = date,
            time = time,
            remindMe = remind,
            priority = TodoTask.priorityFromApi(rawPriority),
            isCompleted = done,
            subtasks = subtasks,
            createdAt = (index + 1) * 1000L
        )
    }

    // Dong goi TodoTask thanh JSONObject gui len POST / PATCH API
    private fun taskToJsonObject(task: TodoTask): JSONObject {
        val subtasksJson = JSONArray()
        task.subtasks.forEach { sub ->
            val subObj = JSONObject().apply {
                put("title", sub.title)
                put("done", sub.done)
            }
            subtasksJson.put(subObj)
        }

        return JSONObject().apply {
            val numericId = task.id.toIntOrNull()
            if (numericId != null) put("id", numericId) else if (task.id.isNotBlank()) put("id", task.id)
            put("title", task.title)
            put("note", task.note)
            put("category", task.apiCategory)
            put("date", task.date)
            put("time", task.time)
            put("priority", task.apiPriority)
            put("remind", task.remindMe)
            put("done", task.isCompleted)
            put("subtasks", subtasksJson)
        }
    }
}
