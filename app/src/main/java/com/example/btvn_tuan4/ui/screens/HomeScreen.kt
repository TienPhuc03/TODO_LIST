package com.example.btvn_tuan4.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

// Man hinh chinh Trang chu (Flow Trang chu UI + API)
@Composable
fun HomeScreen(
    authUser: AuthUser,
    onNavigateToProfile: () -> Unit,
    onNavigateToTaskList: () -> Unit = {},
    onNavigateToTaskDetail: (String) -> Unit = {},
    taskRepository: ITaskRepository = remember { FirestoreTaskRepository() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Trang thai mo hop thoai them viec
    var showAddDialog by remember { mutableStateOf(false) }

    // Khi mo man hinh: Dong bo ho so & goi API GET /mock/teach/tasks
    LaunchedEffect(authUser.uid) {
        taskRepository.syncUserProfile(authUser)
        taskRepository.fetchTasksFromApi(authUser.uid)
    }

    // Lang nghe danh sach viec realtime tu Repository
    val taskList by taskRepository.getTasksFlow(authUser.uid).collectAsState(initial = emptyList())

    // Loc danh sach "Viec hom nay" de hien thi va tinh tien do hom nay (chuan theo anh de bai: 2/4 viec - 50%)
    val todayTasks = remember(taskList) {
        val filtered = taskList.filter { it.isToday }
        if (filtered.isNotEmpty()) filtered else taskList
    }

    val completedTodayCount = todayTasks.count { it.isCompleted }
    val totalTodayCount = todayTasks.size
    val progressFraction = if (totalTodayCount > 0) {
        completedTodayCount.toFloat() / totalTodayCount.toFloat()
    } else {
        0f
    }
    val progressPercent = (progressFraction * 100).toInt()

    // So viec con lai theo tung danh muc tren toan bo danh sach
    val studyRemaining = taskList.count { it.displayCategory == "Học tập" && !it.isCompleted }
    val lifeRemaining = taskList.count { it.displayCategory == "Cuộc sống" && !it.isCompleted }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            HomeBottomNavigationBar(
                selectedTab = "home",
                onTabSelected = { tab ->
                    when (tab) {
                        "list" -> onNavigateToTaskList()
                        "settings" -> onNavigateToProfile()
                        "notifications" -> {
                            Toast.makeText(context, "Bạn có $studyRemaining việc Học tập và $lifeRemaining việc Cuộc sống cần làm!", Toast.LENGTH_SHORT).show()
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Thanh thong tin nguoi dung va chuong thong bao
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onNavigateToProfile() }
                            .padding(vertical = 4.dp)
                    ) {
                        // Avatar ten viet tat
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = authUser.initials,
                                color = UthColors.Primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Thứ Ba, 20 tháng 10",
                                fontSize = 12.sp,
                                color = UthColors.TextSecondary
                            )
                            Text(
                                text = if (authUser.displayName.isBlank() || authUser.displayName == "Minh Anh") "Chào bạn!" else "Chào ${authUser.displayName}!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = UthColors.TextPrimary
                            )
                        }
                    }

                    // Nut chuong thong bao
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .clickable {
                                coroutineScope.launch {
                                    taskRepository.resetToApiSampleData(authUser.uid)
                                    Toast.makeText(context, "Đã đồng bộ lại dữ liệu mẫu từ API!", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Thông báo",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(22.dp)
                        )
                        // Cham do thong bao
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = 8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                    }
                }
            }

            // The tien do hoan thanh hom nay
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = UthColors.Primary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tiến độ hôm nay",
                                fontSize = 13.sp,
                                color = Color(0xFFDCE7FE)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$completedTodayCount/$totalTodayCount việc",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    totalTodayCount == 0 -> "Bắt đầu từ việc nhỏ nhất nhé."
                                    progressPercent >= 100 -> "Xuất sắc! Bạn đã xong mọi việc."
                                    progressPercent >= 50 -> "Đã hơn nửa chặng, cố lên!"
                                    else -> "Khởi đầu ngày mới thật năng suất nhé!"
                                },
                                fontSize = 12.sp,
                                color = Color(0xFFDCE7FE)
                            )
                        }

                        // Vong tron phan tram tien do
                        Box(
                            modifier = Modifier.size(68.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.fillMaxSize(),
                                color = Color.White,
                                trackColor = Color(0xFF1D4ED8),
                                strokeWidth = 6.dp
                            )
                            Text(
                                text = "$progressPercent%",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Danh muc Hoc tap va Cuoc song
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CategoryCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToTaskList() },
                        title = "Học tập",
                        subtitle = "$studyRemaining việc còn lại",
                        icon = Icons.Default.School,
                        bgColor = Color(0xFFEFF6FF),
                        iconTint = Color(0xFF2563EB)
                    )
                    CategoryCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToTaskList() },
                        title = "Cuộc sống",
                        subtitle = "$lifeRemaining việc còn lại",
                        icon = Icons.Default.Home,
                        bgColor = Color(0xFFFFFBEB),
                        iconTint = Color(0xFFD97706)
                    )
                }
            }

            // Tieu de danh sach Viec hom nay & Xem tat ca
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Việc hôm nay",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = UthColors.TextPrimary
                    )
                    Text(
                        text = "Xem tất cả",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = UthColors.Primary,
                        modifier = Modifier.clickable { onNavigateToTaskList() }
                    )
                }
            }

            // Danh sach cong viec hom nay
            if (todayTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có công việc nào. Bấm nút (+) để thêm!",
                            color = UthColors.TextHint,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(todayTasks, key = { it.id }) { task ->
                    TaskItemRow(
                        task = task,
                        onToggle = {
                            coroutineScope.launch {
                                taskRepository.updateTaskStatus(
                                    userId = authUser.uid,
                                    taskId = task.id,
                                    isCompleted = !task.isCompleted
                                )
                            }
                        },
                        onClick = {
                            onNavigateToTaskDetail(task.id)
                        }
                    )
                }
            }

            // Khoang cach cuoi danh sach
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Hop thoai them cong viec moi dang Bottom Sheet chuan theo thiet ke
    if (showAddDialog) {
        AddTaskBottomSheet(
            onDismiss = { showAddDialog = false },
            onConfirm = { newTask ->
                coroutineScope.launch {
                    val result = taskRepository.addTask(authUser.uid, newTask)
                    result.onSuccess {
                        Toast.makeText(context, "Đã thêm công việc mới (POST /mock/teach/tasks)!", Toast.LENGTH_SHORT).show()
                    }.onFailure { error ->
                        Toast.makeText(context, "Lỗi: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                    showAddDialog = false
                }
            }
        )
    }
}

// Bottom Sheet them hoac sua cong viec theo dung thiet ke mau
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    initialTask: TodoTask? = null,
    onDismiss: () -> Unit,
    onConfirm: (TodoTask) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var taskTitle by rememberSaveable { mutableStateOf(initialTask?.title ?: "") }
    var taskNote by rememberSaveable { mutableStateOf(initialTask?.note ?: "") }
    var selectedCategory by rememberSaveable { mutableStateOf(initialTask?.displayCategory ?: "Học tập") }
    var taskDate by rememberSaveable { mutableStateOf(initialTask?.date ?: "2026-10-20") }
    var taskTime by rememberSaveable { mutableStateOf(initialTask?.time ?: "19:00") }
    var remindMe by rememberSaveable { mutableStateOf(initialTask?.remindMe ?: true) }
    var selectedPriority by rememberSaveable { mutableStateOf(initialTask?.displayPriorityShort ?: "Vừa") }
    val scrollState = rememberScrollState()

    val datePresets = listOf(
        "2026-10-20" to "Hôm nay, 20/10",
        "2026-10-21" to "Ngày mai, 21/10",
        "2026-10-24" to "Tuần này, 24/10"
    )
    val timePresets = listOf("06:30", "09:00", "14:00", "17:30", "19:00", "21:00")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFFF8FAFC),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Thanh tieu de tren: Huy - Viec moi / Sua viec - Luu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hủy",
                    color = UthColors.Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onDismiss() }
                )
                Text(
                    text = if (initialTask == null) "Việc mới" else "Sửa công việc",
                    color = UthColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Lưu",
                    color = if (taskTitle.isNotBlank()) UthColors.Primary else Color(0xFF94A3B8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = taskTitle.isNotBlank()) {
                        onConfirm(
                            (initialTask ?: TodoTask()).copy(
                                title = taskTitle.trim(),
                                note = taskNote.trim(),
                                categoryName = selectedCategory,
                                date = taskDate,
                                time = taskTime,
                                remindMe = remindMe,
                                priority = selectedPriority
                            )
                        )
                    }
                )
            }

            // Noi dung form nhap cuon linh hoat
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // The 1: Ten cong viec va Ghi chu
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Tên công việc",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = UthColors.TextPrimary
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (taskTitle.isEmpty()) {
                                    Text(
                                        text = "Ôn chương 4 – Giải thuật",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp
                        )

                        Text(
                            text = "Ghi chú",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = taskNote,
                            onValueChange = { taskNote = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = UthColors.TextPrimary
                            ),
                            decorationBox = { innerTextField ->
                                if (taskNote.isEmpty()) {
                                    Text(
                                        text = "Thêm ghi chú (không bắt buộc)",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Muc: Nhom
                Text(
                    text = "Nhóm",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isStudy = selectedCategory == "Học tập"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isStudy) Color(0xFFEFF6FF) else Color.White)
                            .border(
                                width = if (isStudy) 1.5.dp else 1.dp,
                                color = if (isStudy) UthColors.Primary else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedCategory = "Học tập" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Học tập",
                            fontSize = 15.sp,
                            fontWeight = if (isStudy) FontWeight.Bold else FontWeight.Medium,
                            color = if (isStudy) UthColors.Primary else Color(0xFF334155)
                        )
                    }

                    val isLife = selectedCategory == "Cuộc sống"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isLife) Color(0xFFEFF6FF) else Color.White)
                            .border(
                                width = if (isLife) 1.5.dp else 1.dp,
                                color = if (isLife) UthColors.Primary else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedCategory = "Cuộc sống" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cuộc sống",
                            fontSize = 15.sp,
                            fontWeight = if (isLife) FontWeight.Bold else FontWeight.Medium,
                            color = if (isLife) UthColors.Primary else Color(0xFF334155)
                        )
                    }
                }

                // The 2: Ngay, Gio, Nhac toi
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        // Dong Ngay (bam de chuyen doi nhanh giua Hom nay / Ngay mai / Tuan nay)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val currentIndex = datePresets.indexOfFirst { it.first == taskDate }
                                    val nextIndex = (currentIndex + 1) % datePresets.size
                                    taskDate = datePresets[nextIndex].first
                                }
                                .padding(vertical = 10.dp),
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Ngày",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            val displayDateLabel = datePresets.find { it.first == taskDate }?.second ?: taskDate
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = displayDateLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)

                        // Dong Gio (bam de doi nhanh mốc giờ)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val currentIndex = timePresets.indexOf(taskTime)
                                    val nextIndex = (currentIndex + 1) % timePresets.size
                                    taskTime = timePresets[nextIndex]
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Giờ",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = taskTime,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)

                        // Dong Nhac toi
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
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
                                Column {
                                    Text(
                                        text = "Nhắc tôi",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Trước 15 phút",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Switch(
                                checked = remindMe,
                                onCheckedChange = { remindMe = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF22C55E),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                // Muc: Muc uu tien
                Text(
                    text = "Mức ưu tiên",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val priorities = listOf("Thấp", "Vừa", "Cao")
                    priorities.forEach { p ->
                        val isSelected = selectedPriority == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { selectedPriority = p }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Nut Luu cong viec
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            onConfirm(
                                (initialTask ?: TodoTask()).copy(
                                    title = taskTitle.trim(),
                                    note = taskNote.trim(),
                                    categoryName = selectedCategory,
                                    date = taskDate,
                                    time = taskTime,
                                    remindMe = remindMe,
                                    priority = selectedPriority
                                )
                            )
                        }
                    },
                    enabled = taskTitle.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UthColors.Primary)
                ) {
                    Text(
                        text = "Lưu công việc",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// The the hien tung danh muc
@Composable
fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    bgColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

// Dong hien thi thong tin mot cong viec tren Trang chu
@Composable
fun TaskItemRow(
    task: TodoTask,
    onToggle: () -> Unit,
    onClick: () -> Unit = {},
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
                // Checkbox danh dau hoan thanh (bam rieng de tick)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (task.isCompleted) UthColors.Primary else Color.Transparent)
                        .border(
                            width = if (task.isCompleted) 0.dp else 1.5.dp,
                            color = if (task.isCompleted) Color.Transparent else Color(0xFFCBD5E1),
                            shape = CircleShape
                        )
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = if (task.isCompleted) FontWeight.Medium else FontWeight.SemiBold,
                        color = if (task.isCompleted) Color(0xFF94A3B8) else UthColors.TextPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Nhan the loai tag pill bo goc
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
                            text = task.time,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Thanh dieu huong duoi day man hinh
@Composable
fun HomeBottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onAddNewTaskClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Trang chủ",
                isSelected = selectedTab == "home",
                onClick = { onTabSelected("home") }
            )

            BottomNavItem(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                label = "Danh sách",
                isSelected = selectedTab == "list",
                onClick = { onTabSelected("list") }
            )

            // Nut them viec moi
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(UthColors.Primary)
                    .clickable { onAddNewTaskClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm việc mới",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            BottomNavItem(
                icon = Icons.Default.Notifications,
                label = "Thông báo",
                isSelected = selectedTab == "notifications",
                onClick = { onTabSelected("notifications") }
            )

            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "Cài đặt",
                isSelected = selectedTab == "settings",
                onClick = { onTabSelected("settings") }
            )
        }
    }
}

// Item dieu huong
@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) UthColors.Primary else Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) UthColors.Primary else Color(0xFF94A3B8)
        )
    }
}

// Preview man hinh chinh
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            authUser = AuthUser(
                uid = "123",
                displayName = "Khoa",
                email = "khoa@example.com",
                photoUrl = null
            ),
            onNavigateToProfile = {}
        )
    }
}
