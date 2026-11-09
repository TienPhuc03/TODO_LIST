package com.example.btvn_tuan4.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.btvn_tuan4.model.AuthUser
import com.example.btvn_tuan4.model.TodoTask
import com.example.btvn_tuan4.repository.FirestoreTaskRepository
import com.example.btvn_tuan4.repository.ITaskRepository
import com.example.btvn_tuan4.ui.components.UthColors
import kotlinx.coroutines.launch

// Man hinh Danh sach cong viec (Flow Danh sach UI + API)
@Composable
fun TaskListScreen(
    authUser: AuthUser,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    taskRepository: ITaskRepository = remember { FirestoreTaskRepository() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("Tất cả") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Goi API GET /mock/teach/tasks khi mo man hinh Danh sach
    LaunchedEffect(authUser.uid) {
        taskRepository.fetchTasksFromApi(authUser.uid)
    }

    // Lang nghe danh sach cong viec realtime
    val allTasks by taskRepository.getTasksFlow(authUser.uid).collectAsState(initial = emptyList())

    // Dem so viec chua xong tren toan bo danh sach
    val totalPendingCount = allTasks.count { !it.isCompleted }

    // Loc theo tu khoa tim kiem va bo loc danh muc
    val filteredTasks = remember(allTasks, searchQuery, selectedCategoryFilter) {
        allTasks.filter { task ->
            val matchesCategory = when (selectedCategoryFilter) {
                "Học tập" -> task.displayCategory == "Học tập"
                "Cuộc sống" -> task.displayCategory == "Cuộc sống"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    task.title.contains(searchQuery.trim(), ignoreCase = true) ||
                    task.note.contains(searchQuery.trim(), ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val pendingTasks = remember(filteredTasks) { filteredTasks.filter { !it.isCompleted } }
    val completedTasks = remember(filteredTasks) { filteredTasks.filter { it.isCompleted } }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            HomeBottomNavigationBar(
                selectedTab = "list",
                onTabSelected = { tab ->
                    when (tab) {
                        "home" -> onNavigateToHome()
                        "settings" -> onNavigateToProfile()
                        "notifications" -> {
                            Toast.makeText(context, "Bạn còn $totalPendingCount việc chưa xong!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onAddNewTaskClick = {
                    showAddDialog = true
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Danh sach & X viec chua xong
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Danh sách",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = UthColors.TextPrimary
                    )
                    Text(
                        text = "$totalPendingCount việc chưa xong",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = UthColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            // O tim kiem cong viec
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = UthColors.TextPrimary
                                ),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Tìm công việc",
                                            fontSize = 14.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa tìm kiếm",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }
            }

            // Hang nut loc danh muc: Tat ca - Hoc tap - Cuoc song
            item {
                val categories = listOf("Tất cả", "Học tập", "Cuộc sống")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategoryFilter == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFF0F172A) else Color.White)
                                .border(
                                    width = if (isSelected) 0.dp else 1.dp,
                                    color = if (isSelected) Color.Transparent else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedCategoryFilter = category }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // Nhom 1: CAN LAM
            if (pendingTasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CẦN LÀM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                }

                items(pendingTasks, key = { "pending_${it.id}" }) { task ->
                    TaskListPendingItemCard(
                        task = task,
                        onToggle = {
                            coroutineScope.launch {
                                taskRepository.updateTaskStatus(
                                    userId = authUser.uid,
                                    taskId = task.id,
                                    isCompleted = true
                                )
                            }
                        },
                        onClick = { onNavigateToTaskDetail(task.id) }
                    )
                }
            }

            // Nhom 2: DA XONG
            if (completedTasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ĐÃ XONG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                }

                items(completedTasks, key = { "done_${it.id}" }) { task ->
                    TaskListCompletedItemCard(
                        task = task,
                        onToggle = {
                            coroutineScope.launch {
                                taskRepository.updateTaskStatus(
                                    userId = authUser.uid,
                                    taskId = task.id,
                                    isCompleted = false
                                )
                            }
                        },
                        onClick = { onNavigateToTaskDetail(task.id) }
                    )
                }
            }

            // Trang thai trong khi khong co ket qua khop
            if (pendingTasks.isEmpty() && completedTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không tìm thấy công việc phù hợp.",
                            fontSize = 14.sp,
                            color = UthColors.TextHint
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Bottom sheet them viec moi
    if (showAddDialog) {
        AddTaskBottomSheet(
            onDismiss = { showAddDialog = false },
            onConfirm = { newTask ->
                coroutineScope.launch {
                    taskRepository.addTask(authUser.uid, newTask)
                    Toast.makeText(context, "Đã thêm công việc mới!", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                }
            }
        )
    }
}

// The cong viec trong muc CAN LAM (co nhan Danh muc, Han chot va nhan "Gap" neu uu tien cao)
@Composable
fun TaskListPendingItemCard(
    task: TodoTask,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Vong tron checkbox chua hoan thanh
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                        .clickable { onToggle() }
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = UthColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isStudy = task.displayCategory == "Học tập"
                        val badgeBg = if (isStudy) Color(0xFFEFF6FF) else Color(0xFFFFFBEB)
                        val badgeText = if (isStudy) Color(0xFF2563EB) else Color(0xFFD97706)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.displayCategory,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeText
                            )
                        }

                        Text(
                            text = task.displayDeadline,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Huy hieu "Gap" mau do neu priority == "high" / "Cao" (chuan theo Hinh 2)
            if (task.isUrgent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEE2E2))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Gấp",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

// The cong viec trong muc DA XONG (gon gang voi tich xanh va chu gach ngang chuan Hinh 2)
@Composable
fun TaskListCompletedItemCard(
    task: TodoTask,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(UthColors.Primary)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                textDecoration = TextDecoration.LineThrough
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TaskListScreenPreview() {
    MaterialTheme {
        TaskListScreen(
            authUser = AuthUser(uid = "123", displayName = "Khoa"),
            onNavigateToHome = {},
            onNavigateToProfile = {},
            onNavigateToTaskDetail = {}
        )
    }
}
