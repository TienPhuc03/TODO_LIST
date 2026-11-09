package com.example.btvn_tuan4.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.btvn_tuan4.model.AuthUser
import com.example.btvn_tuan4.model.SubTask
import com.example.btvn_tuan4.model.TodoTask
import com.example.btvn_tuan4.repository.FirestoreTaskRepository
import com.example.btvn_tuan4.repository.ITaskRepository
import com.example.btvn_tuan4.ui.components.UthColors
import kotlinx.coroutines.launch

// Man hinh Chi tiet cong viec (Flow Chi tiet UI + API: GET, PATCH, DELETE /mock/teach/tasks/{id})
@Composable
fun TaskDetailScreen(
    taskId: String,
    authUser: AuthUser,
    onNavigateBack: () -> Unit,
    taskRepository: ITaskRepository = remember { FirestoreTaskRepository() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Lang nghe danh sach cong viec realtime
    val taskList by taskRepository.getTasksFlow(authUser.uid).collectAsState(initial = emptyList())
    var apiLoadedTask by remember { mutableStateOf<TodoTask?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Trang thai mo BottomSheet sua cong viec
    var showEditSheet by remember { mutableStateOf(false) }

    // Trang thai nhap viec nho moi
    var newSubtaskTitle by rememberSaveable { mutableStateOf("") }

    // Trang thai sua ten mot viec nho
    var editingSubtask by remember { mutableStateOf<SubTask?>(null) }
    var editingSubtaskText by remember { mutableStateOf("") }

    // 1. Goi API GET /mock/teach/tasks/{id} khi mo man hinh Chi tiet
    LaunchedEffect(taskId) {
        isLoading = true
        val detailResult = taskRepository.fetchTaskDetailFromApi(authUser.uid, taskId)
        detailResult.onSuccess { fetched ->
            apiLoadedTask = fetched
        }
        isLoading = false
    }

    // Uu tien lay task tu StateFlow de cap nhat tuc thi khi nguoi dung thao tac
    val currentTask = taskList.find { it.id == taskId } ?: apiLoadedTask

    if (currentTask == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = UthColors.Primary)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Không tìm thấy công việc",
                        fontSize = 16.sp,
                        color = UthColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Quay lại")
                    }
                }
            }
        }
        return
    }

    val subtasks = currentTask.subtasks
    val completedSubtasksCount = subtasks.count { it.done }
    val totalSubtasksCount = subtasks.size
    val subtaskProgress = if (totalSubtasksCount > 0) {
        completedSubtasksCount.toFloat() / totalSubtasksCount.toFloat()
    } else {
        if (currentTask.isCompleted) 1f else 0f
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            // Thanh tieu de tren: < Trang chu | Sua
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onNavigateBack() }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Quay lại",
                        tint = UthColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Trang chủ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = UthColors.Primary
                    )
                }

                Text(
                    text = "Sửa",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UthColors.Primary,
                    modifier = Modifier
                        .clickable { showEditSheet = true }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                )
            }
        },
        bottomBar = {
            // Khu vuc nut hanh dong duoi cung: Danh dau hoan thanh (PATCH) & Xoa cong viec (DELETE)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val newDoneState = !currentTask.isCompleted
                            taskRepository.updateTaskStatus(
                                userId = authUser.uid,
                                taskId = currentTask.id,
                                isCompleted = newDoneState
                            )
                            apiLoadedTask = currentTask.copy(isCompleted = newDoneState)
                            val msg = if (newDoneState) "Đã đánh dấu hoàn thành!" else "Đã chuyển về trạng thái cần làm!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UthColors.Primary)
                ) {
                    Text(
                        text = if (currentTask.isCompleted) "Đánh dấu chưa xong" else "Đánh dấu hoàn thành",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Xóa công việc",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                taskRepository.deleteTask(authUser.uid, currentTask.id)
                                Toast.makeText(context, "Đã xóa công việc!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        }
                        .padding(vertical = 6.dp, horizontal = 16.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hang huy hieu: Danh muc (Hoc tap / Cuoc song) & Muc uu tien (Uu tien vua / Uu tien cao / Uu tien thap)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isStudy = currentTask.displayCategory == "Học tập"
                val catBg = if (isStudy) Color(0xFFEFF6FF) else Color(0xFFFFFBEB)
                val catText = if (isStudy) Color(0xFF2563EB) else Color(0xFFD97706)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(catBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentTask.displayCategory,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = catText
                    )
                }

                val priorityBg = when (currentTask.apiPriority) {
                    "high" -> Color(0xFFFEE2E2)
                    "low" -> Color(0xFFF1F5F9)
                    else -> Color(0xFFFEF3C7)
                }
                val priorityText = when (currentTask.apiPriority) {
                    "high" -> Color(0xFFDC2626)
                    "low" -> Color(0xFF64748B)
                    else -> Color(0xFFB45309)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(priorityBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentTask.displayPriorityBadge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = priorityText
                    )
                }
            }

            // Tieu de cong viec co lon
            Text(
                text = currentTask.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = UthColors.TextPrimary,
                lineHeight = 28.sp
            )

            // The thong tin Han chot & Nhac nho
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Hạn chót",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Text(
                            text = currentTask.displayDeadline,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = UthColors.TextPrimary
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Nhắc nhở",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Text(
                            text = if (currentTask.remindMe) "Trước 15 phút" else "Không nhắc",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = UthColors.TextPrimary
                        )
                    }
                }
            }

            // Muc: Ghi chu
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ghi chú",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Text(
                        text = currentTask.note.ifBlank { "Không có ghi chú thêm cho công việc này." },
                        fontSize = 14.sp,
                        color = if (currentTask.note.isBlank()) Color(0xFF94A3B8) else UthColors.TextPrimary,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Muc: Viec nho (Subtasks) kem thanh tien do va thao tac Them / Sua / Xoa / Tick
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Việc nhỏ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "$completedSubtasksCount/$totalSubtasksCount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                // Thanh tien do viec nho
                LinearProgressIndicator(
                    progress = { subtaskProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = UthColors.Primary,
                    trackColor = Color(0xFFE2E8F0)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        subtasks.forEachIndexed { index, sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val updatedSubtasks = subtasks.map {
                                                if (it.id == sub.id) it.copy(done = !it.done) else it
                                            }
                                            val updatedTask = currentTask.copy(subtasks = updatedSubtasks)
                                            apiLoadedTask = updatedTask
                                            coroutineScope.launch {
                                                taskRepository.updateTask(authUser.uid, updatedTask)
                                            }
                                        }
                                ) {
                                    // Checkbox vuong bo goc cho Viec nho chuan Hinh 3
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (sub.done) UthColors.Primary else Color.Transparent)
                                            .border(
                                                width = if (sub.done) 0.dp else 1.5.dp,
                                                color = if (sub.done) Color.Transparent else Color(0xFFCBD5E1),
                                                shape = RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (sub.done) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = sub.title,
                                        fontSize = 14.sp,
                                        color = if (sub.done) Color(0xFF94A3B8) else UthColors.TextPrimary,
                                        textDecoration = if (sub.done) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Hai nut icon Sua (cay but) va Xoa (thung rac)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Sửa việc nhỏ",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(17.dp)
                                            .clickable {
                                                editingSubtask = sub
                                                editingSubtaskText = sub.title
                                            }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Xóa việc nhỏ",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                val updatedSubtasks = subtasks.filterNot { it.id == sub.id }
                                                val updatedTask = currentTask.copy(subtasks = updatedSubtasks)
                                                apiLoadedTask = updatedTask
                                                coroutineScope.launch {
                                                    taskRepository.updateTask(authUser.uid, updatedTask)
                                                }
                                            }
                                    )
                                }
                            }

                            if (index < subtasks.lastIndex || true) {
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            }
                        }

                        // Dong "+ Them viec nho ... Them"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                BasicTextField(
                                    value = newSubtaskTitle,
                                    onValueChange = { newSubtaskTitle = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        color = UthColors.TextPrimary
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (newSubtaskTitle.isEmpty()) {
                                            Text(
                                                text = "Thêm việc nhỏ",
                                                fontSize = 14.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Text(
                                text = "Thêm",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (newSubtaskTitle.isNotBlank()) UthColors.Primary else Color(0xFF64748B),
                                modifier = Modifier
                                    .clickable {
                                        val titleToAdd = newSubtaskTitle.trim().ifEmpty {
                                            "Việc nhỏ mới ${subtasks.size + 1}"
                                        }
                                        val newSub = SubTask(
                                            id = "sub_${currentTask.id}_${System.currentTimeMillis()}",
                                            title = titleToAdd,
                                            done = false
                                        )
                                        val updatedTask = currentTask.copy(subtasks = subtasks + newSub)
                                        apiLoadedTask = updatedTask
                                        newSubtaskTitle = ""
                                        coroutineScope.launch {
                                            taskRepository.updateTask(authUser.uid, updatedTask)
                                        }
                                    }
                                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Hop thoai sua ten viec nho
    if (editingSubtask != null) {
        AlertDialog(
            onDismissRequest = { editingSubtask = null },
            title = { Text("Sửa việc nhỏ", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                OutlinedTextField(
                    value = editingSubtaskText,
                    onValueChange = { editingSubtaskText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = editingSubtask
                        if (target != null && editingSubtaskText.isNotBlank()) {
                            val updatedSubtasks = subtasks.map {
                                if (it.id == target.id) it.copy(title = editingSubtaskText.trim()) else it
                            }
                            val updatedTask = currentTask.copy(subtasks = updatedSubtasks)
                            apiLoadedTask = updatedTask
                            coroutineScope.launch {
                                taskRepository.updateTask(authUser.uid, updatedTask)
                            }
                        }
                        editingSubtask = null
                    }
                ) {
                    Text("Lưu", color = UthColors.Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSubtask = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Bottom sheet sua thong tin cong viec khi bam "Sua" o goc phai tren
    if (showEditSheet) {
        AddTaskBottomSheet(
            initialTask = currentTask,
            onDismiss = { showEditSheet = false },
            onConfirm = { updatedTask ->
                apiLoadedTask = updatedTask
                coroutineScope.launch {
                    taskRepository.updateTask(authUser.uid, updatedTask)
                    Toast.makeText(context, "Đã cập nhật công việc (PATCH /mock/teach/tasks/${updatedTask.id})!", Toast.LENGTH_SHORT).show()
                    showEditSheet = false
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TaskDetailScreenPreview() {
    MaterialTheme {
        TaskDetailScreen(
            taskId = "2",
            authUser = AuthUser(uid = "123", displayName = "Khoa"),
            onNavigateBack = {}
        )
    }
}
