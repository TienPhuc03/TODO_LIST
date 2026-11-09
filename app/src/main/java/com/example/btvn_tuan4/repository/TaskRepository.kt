    package com.example.btvn_tuan4.repository

import android.util.Log
import com.example.btvn_tuan4.model.AuthUser
import com.example.btvn_tuan4.model.SubTask
import com.example.btvn_tuan4.model.TodoTask
import com.example.btvn_tuan4.network.TaskApiService
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

// Interface quan ly cong viec Todo ket hop REST API that va Cloud Firestore
interface ITaskRepository {
    fun getTasksFlow(userId: String): Flow<List<TodoTask>>
    suspend fun fetchTasksFromApi(userId: String): Result<List<TodoTask>>
    suspend fun fetchTaskDetailFromApi(userId: String, taskId: String): Result<TodoTask>
    suspend fun addTask(userId: String, task: TodoTask): Result<String>
    suspend fun updateTaskStatus(userId: String, taskId: String, isCompleted: Boolean): Result<Unit>
    suspend fun updateTask(userId: String, task: TodoTask): Result<Unit>
    suspend fun deleteTask(userId: String, taskId: String): Result<Unit>
    suspend fun resetToApiSampleData(userId: String): Result<List<TodoTask>>
    suspend fun seedInitialTasksIfEmpty(userId: String): Result<Unit>
    suspend fun syncUserProfile(user: AuthUser): Result<Unit>
}

// Trien khai Repository goi truc tiep REST API (/mock/teach/tasks) va dong bo Firestore
class FirestoreTaskRepository : ITaskRepository {

    // Khoi tao Firestore an toan
    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirestoreTaskRepo", "Lỗi khởi tạo FirebaseFirestore: ${e.message}")
            null
        }
    }

    // StateFlow quan ly danh sach cong viec tap trung (Khoi tao rong, duoc nap tu API that)
    private val _tasksFlow = MutableStateFlow<List<TodoTask>>(emptyList())

    // Cache danh sach cong viec tai ve tu API that
    private var latestApiTasks: List<TodoTask> = emptyList()

    // Luu cac ID cong viec ma nguoi dung da chu dong bam Xoa trong phien lam viec
    private val deletedTaskIds = mutableSetOf<String>()

    // Danh dau da dong bo day du task API cho tung userId
    private val syncedApiUsers = mutableSetOf<String>()

    private var activeListener: ListenerRegistration? = null
    private var currentListeningUserId: String? = null

    // Lang nghe danh sach cong viec realtime
    override fun getTasksFlow(userId: String): Flow<List<TodoTask>> {
        val validUserId = userId.ifEmpty { "default_user" }
        attachFirestoreListener(validUserId)
        return _tasksFlow.asStateFlow()
    }

    // Gan listener lang nghe thay doi tu Firestore theo tung userId
    private fun attachFirestoreListener(userId: String) {
        if (currentListeningUserId == userId && activeListener != null) return

        activeListener?.remove()
        currentListeningUserId = userId

        val db = firestore ?: return
        val tasksRef = db.collection("users")
            .document(userId)
            .collection("tasks")

        activeListener = tasksRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("FirestoreTaskRepo", "Lỗi lắng nghe Firestore: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty && latestApiTasks.isNotEmpty()) {
                val remoteTasks = snapshot.documents.mapNotNull { doc ->
                    documentToTodoTask(doc)
                }
                val merged = mergeRemoteWithApiTasks(
                    remoteTasks = remoteTasks,
                    apiTasks = latestApiTasks
                )
                _tasksFlow.value = merged
            }
        }
    }

    // Hop nhat du lieu tren Firestore voi danh sach cong viec tai tu API that (/mock/teach/tasks)
    private fun mergeRemoteWithApiTasks(
        remoteTasks: List<TodoTask>,
        apiTasks: List<TodoTask>
    ): List<TodoTask> {
        val apiIds = apiTasks.map { it.id }.toSet()
        val apiTitlesNormalized = apiTasks.map { it.title.trim().lowercase() }.toSet()

        val remoteByApiId = remoteTasks
            .filter { it.id in apiIds && it.id !in deletedTaskIds }
            .associateBy { it.id }

        val hasAllApiIdsInRemote = apiIds.count { it in remoteByApiId.keys } >= 4
        val standardList = apiTasks
            .filter { it.id !in deletedTaskIds }
            .mapIndexed { idx, apiTask ->
                val remoteMatch = remoteByApiId[apiTask.id]
                if (remoteMatch != null && hasAllApiIdsInRemote) {
                    remoteMatch.copy(
                        subtasks = if (remoteMatch.subtasks.isEmpty() && apiTask.subtasks.isNotEmpty()) {
                            apiTask.subtasks
                        } else {
                            remoteMatch.subtasks
                        },
                        createdAt = (idx + 1) * 1000L
                    )
                } else {
                    apiTask.copy(createdAt = (idx + 1) * 1000L)
                }
            }

        // Giu lai cac cong viec moi do nguoi dung tu them
        val userAddedTasks = remoteTasks.filter { remote ->
            remote.id !in apiIds &&
                    remote.id !in deletedTaskIds &&
                    remote.title.trim().lowercase() !in apiTitlesNormalized
        }

        return (standardList + userAddedTasks).sortedBy { it.createdAt }
    }

    // Chuyen doi DocumentSnapshot cua Firestore sang TodoTask an toan (bao gom ca subtasks)
    private fun documentToTodoTask(doc: DocumentSnapshot): TodoTask? {
        val data = doc.data ?: return null
        val id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id
        val title = (data["title"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val note = (data["note"] as? String) ?: ""
        val categoryName = (data["categoryName"] as? String) ?: "Học tập"
        val date = (data["date"] as? String) ?: "2026-10-20"
        val time = (data["time"] as? String) ?: "14:00"
        val remindMe = (data["remindMe"] as? Boolean) ?: true
        val priority = (data["priority"] as? String) ?: "Vừa"
        val isCompleted = (data["isCompleted"] as? Boolean) ?: false
        val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

        val rawSubtasks = data["subtasks"] as? List<*>
        val subtasks = rawSubtasks?.mapIndexedNotNull { idx, item ->
            val map = item as? Map<*, *> ?: return@mapIndexedNotNull null
            SubTask(
                id = (map["id"] as? String)?.takeIf { it.isNotBlank() } ?: "sub_${id}_${idx + 1}",
                title = (map["title"] as? String) ?: "",
                done = (map["done"] as? Boolean) ?: false
            )
        } ?: emptyList()

        return TodoTask(
            id = id,
            title = title,
            note = note,
            categoryName = categoryName,
            date = date,
            time = time,
            remindMe = remindMe,
            priority = priority,
            isCompleted = isCompleted,
            subtasks = subtasks,
            createdAt = createdAt
        )
    }

    // 1. Goi API that GET /mock/teach/tasks khi mo man hinh Trang chu hoac Danh sach
    override suspend fun fetchTasksFromApi(userId: String): Result<List<TodoTask>> {
        val validUserId = userId.ifEmpty { "default_user" }
        val apiResult = TaskApiService.fetchAllTasks()

        return apiResult.onSuccess { apiTasks ->
            latestApiTasks = apiTasks
            val db = firestore
            if (db != null) {
                try {
                    val tasksCol = db.collection("users")
                        .document(validUserId)
                        .collection("tasks")
                    val existingSnap = tasksCol.get().await()
                    val remoteTasks = existingSnap.documents.mapNotNull { documentToTodoTask(it) }

                    val apiIds = apiTasks.map { it.id }.toSet()
                    val apiTitlesNormalized = apiTasks.map { it.title.trim().lowercase() }.toSet()
                    val existingApiCount = remoteTasks.count { it.id in apiIds }

                    if (existingApiCount < apiTasks.size && validUserId !in syncedApiUsers) {
                        syncedApiUsers.add(validUserId)
                        val mergedTasks = mergeRemoteWithApiTasks(remoteTasks, apiTasks)
                        _tasksFlow.value = mergedTasks

                        val batch = db.batch()
                        existingSnap.documents.forEach { doc ->
                            val docTask = documentToTodoTask(doc)
                            if (docTask != null &&
                                doc.id !in apiIds &&
                                docTask.title.trim().lowercase() in apiTitlesNormalized
                            ) {
                                batch.delete(doc.reference)
                            }
                        }
                        mergedTasks.forEach { task ->
                            batch.set(tasksCol.document(task.id), task.toMap())
                        }
                        batch.commit().await()
                    } else {
                        val mergedTasks = mergeRemoteWithApiTasks(remoteTasks, apiTasks)
                        _tasksFlow.value = mergedTasks
                    }
                } catch (e: Exception) {
                    Log.w("FirestoreTaskRepo", "Không thể đồng bộ Firestore, hiển thị trực tiếp từ API: ${e.message}")
                    _tasksFlow.value = apiTasks.filter { it.id !in deletedTaskIds }
                }
            } else {
                _tasksFlow.value = apiTasks.filter { it.id !in deletedTaskIds }
            }
        }
    }

    // 2. Goi API that GET /mock/teach/tasks/{id} khi mo man hinh Chi tiet
    override suspend fun fetchTaskDetailFromApi(userId: String, taskId: String): Result<TodoTask> {
        val apiResult = TaskApiService.fetchTaskById(taskId)
        val localTask = _tasksFlow.value.find { it.id == taskId }

        return if (localTask != null) {
            val mergedTask = apiResult.getOrNull()?.let { fromApi ->
                if (localTask.subtasks.isEmpty() && fromApi.subtasks.isNotEmpty()) {
                    localTask.copy(subtasks = fromApi.subtasks)
                } else {
                    localTask
                }
            } ?: localTask
            Result.success(mergedTask)
        } else {
            apiResult
        }
    }

    // 3. Them cong viec moi (POST /mock/teach/tasks + Firestore)
    override suspend fun addTask(userId: String, task: TodoTask): Result<String> {
        val validUserId = userId.ifEmpty { "default_user" }
        val db = firestore

        val newId = task.id.ifBlank {
            val maxNumericId = _tasksFlow.value.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 6
            (maxNumericId + 1).toString()
        }
        val taskWithId = task.copy(id = newId, createdAt = System.currentTimeMillis())

        // Cap nhat Optimistic UI ngay lap tuc
        _tasksFlow.value = _tasksFlow.value + taskWithId

        // Goi POST /mock/teach/tasks
        TaskApiService.createTaskOnApi(taskWithId)

        return try {
            if (db != null) {
                val userDocRef = db.collection("users").document(validUserId)
                userDocRef.set(
                    mapOf("userId" to validUserId, "updatedAt" to System.currentTimeMillis()),
                    SetOptions.merge()
                ).await()

                userDocRef.collection("tasks").document(newId).set(taskWithId.toMap()).await()
            }
            Result.success(newId)
        } catch (e: Exception) {
            Log.e("FirestoreTaskRepo", "Lỗi thêm task vào Firestore: ${e.message}", e)
            Result.success(newId)
        }
    }

    // 4. Cap nhat trang thai hoan thanh (PATCH /mock/teach/tasks/{id} + Firestore)
    override suspend fun updateTaskStatus(
        userId: String,
        taskId: String,
        isCompleted: Boolean
    ): Result<Unit> {
        val currentTask = _tasksFlow.value.find { it.id == taskId }
            ?: return Result.failure(Exception("Không tìm thấy công việc"))
        val updated = currentTask.copy(isCompleted = isCompleted)
        return updateTask(userId, updated)
    }

    // 5. Cap nhat toan bo thong tin cong viec va subtasks (PATCH /mock/teach/tasks/{id} + Firestore)
    override suspend fun updateTask(userId: String, task: TodoTask): Result<Unit> {
        val validUserId = userId.ifEmpty { "default_user" }
        val db = firestore

        // 1. Cap nhat tuc thi tren StateFlow
        _tasksFlow.value = _tasksFlow.value.map {
            if (it.id == task.id) task else it
        }

        // 2. Goi PATCH /mock/teach/tasks/{id}
        TaskApiService.patchTaskOnApi(task.id, task)

        // 3. Dong bo len Cloud Firestore
        return try {
            if (db != null) {
                db.collection("users")
                    .document(validUserId)
                    .collection("tasks")
                    .document(task.id)
                    .set(task.toMap())
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreTaskRepo", "Lỗi cập nhật Firestore: ${e.message}")
            Result.success(Unit)
        }
    }

    // 6. Xoa cong viec (DELETE /mock/teach/tasks/{id} + Firestore)
    override suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val validUserId = userId.ifEmpty { "default_user" }
        val db = firestore

        deletedTaskIds.add(taskId)
        _tasksFlow.value = _tasksFlow.value.filterNot { it.id == taskId }

        // Goi DELETE /mock/teach/tasks/{id}
        TaskApiService.deleteTaskOnApi(taskId)

        return try {
            if (db != null) {
                db.collection("users")
                    .document(validUserId)
                    .collection("tasks")
                    .document(taskId)
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreTaskRepo", "Lỗi xóa task trên Firestore: ${e.message}")
            Result.success(Unit)
        }
    }

    // Tai lai du lieu truc tiep tu API that (GET /mock/teach/tasks)
    override suspend fun resetToApiSampleData(userId: String): Result<List<TodoTask>> {
        val validUserId = userId.ifEmpty { "default_user" }
        deletedTaskIds.clear()
        val apiResult = TaskApiService.fetchAllTasks()
        return apiResult.onSuccess { apiTasks ->
            latestApiTasks = apiTasks
            _tasksFlow.value = apiTasks
            val db = firestore
            if (db != null) {
                try {
                    val tasksCol = db.collection("users").document(validUserId).collection("tasks")
                    val existing = tasksCol.get().await()
                    val batch = db.batch()
                    existing.documents.forEach { batch.delete(it.reference) }
                    apiTasks.forEach { task ->
                        batch.set(tasksCol.document(task.id), task.toMap())
                    }
                    batch.commit().await()
                } catch (e: Exception) {
                    Log.w("FirestoreTaskRepo", "Lỗi đồng bộ lại Firestore từ API: ${e.message}")
                }
            }
        }
    }

    // Khoi tao du lieu ban dau tu API that
    override suspend fun seedInitialTasksIfEmpty(userId: String): Result<Unit> {
        fetchTasksFromApi(userId)
        return Result.success(Unit)
    }

    // Dong bo ho so nguoi dung len Cloud Firestore
    override suspend fun syncUserProfile(user: AuthUser): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        if (user.uid.isBlank() || user.isAnonymous) return Result.success(Unit)

        return try {
            val userDocRef = db.collection("users").document(user.uid)
            userDocRef.set(
                mapOf(
                    "userId" to user.uid,
                    "name" to user.displayName,
                    "email" to user.email,
                    "lastActive" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("FirestoreTaskRepo", "Lỗi đồng bộ hồ sơ: ${e.message}")
            Result.success(Unit)
        }
    }
}
