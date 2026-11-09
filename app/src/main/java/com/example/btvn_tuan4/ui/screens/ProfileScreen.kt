package com.example.btvn_tuan4.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.btvn_tuan4.model.AuthUser
import com.example.btvn_tuan4.repository.AuthRepository
import com.example.btvn_tuan4.repository.FirestoreTaskRepository
import com.example.btvn_tuan4.repository.IAuthRepository
import com.example.btvn_tuan4.repository.ITaskRepository
import com.example.btvn_tuan4.ui.components.UthColors

// Man hinh ho so nguoi dung dong bo du lieu cong viec thuc te tu Firestore
@Composable
fun ProfileScreen(
    authUser: AuthUser,
    authRepository: IAuthRepository,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    taskRepository: ITaskRepository = remember { FirestoreTaskRepository() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Thu thap du lieu danh sach cong viec realtime tu Repository
    val taskList by taskRepository.getTasksFlow(authUser.uid).collectAsState(initial = emptyList())
    val completedCount = taskList.count { it.isCompleted }
    val pendingCount = taskList.count { !it.isCompleted }
    val progressPercent = if (taskList.isNotEmpty()) (completedCount * 100 / taskList.size) else 0

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            // Thanh tieu de tren voi nut quay lai
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onNavigateBack() }
                        .padding(end = 8.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = UthColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cài đặt",
                        fontSize = 15.sp,
                        color = UthColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Hồ sơ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = UthColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 65.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Avatar nguoi dung
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDBEAFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authUser.initials,
                        color = UthColors.Primary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ten nguoi dung
                Text(
                    text = authUser.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = UthColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Email nguoi dung
                Text(
                    text = authUser.email,
                    fontSize = 13.sp,
                    color = UthColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Huy hieu trang thai tai khoan
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (authUser.isGoogleLinked) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (authUser.isGoogleLinked) Color(0xFFA7F3D0) else Color(0xFFCBD5E1)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (authUser.isGoogleLinked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = if (authUser.isGoogleLinked) "Đã liên kết tài khoản Google" else "Tài khoản khách (Chưa liên kết)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (authUser.isGoogleLinked) Color(0xFF059669) else Color(0xFF475569)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // The thong ke chi so thuc te dong bo voi Firestore
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatColumn(number = completedCount.toString(), label = "Việc đã xong")

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        StatColumn(number = pendingCount.toString(), label = "Đang chờ")

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        StatColumn(number = "$progressPercent%", label = "Hoàn thành")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // The bieu do tuan dong bo tien do thuc te
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tuần này",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = UthColors.TextPrimary
                            )
                            Text(
                                text = "Đã xong: $completedCount/${taskList.size} việc",
                                fontSize = 12.sp,
                                color = UthColors.TextHint
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Cac cot bieu do theo ngay
                        val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                        val completedHeight = (20 + minOf(completedCount * 10, 45)).dp
                        val heights = listOf(28.dp, completedHeight, 18.dp, 22.dp, 18.dp, 18.dp, 20.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(75.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            days.forEachIndexed { index, day ->
                                val isHighlighted = day == "T3"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height(heights[index])
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isHighlighted) UthColors.Primary else Color(0xFFDBEAFE)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = day,
                                        fontSize = 11.sp,
                                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isHighlighted) UthColors.Primary else UthColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Nut dang xuat tai khoan
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        authRepository.signOut()
                        Toast.makeText(context, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show()
                        onSignOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Đăng xuất",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đăng xuất",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

// Cot hien thi chi so thong ke
@Composable
fun StatColumn(number: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = UthColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = UthColors.TextSecondary
        )
    }
}

// Preview man hinh ho so
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(
            authUser = AuthUser(
                uid = "123",
                displayName = "Minh Anh",
                email = "minhanh@example.com",
                photoUrl = null,
                isGoogleLinked = true
            ),
            authRepository = AuthRepository(),
            onNavigateBack = {},
            onSignOut = {}
        )
    }
}
