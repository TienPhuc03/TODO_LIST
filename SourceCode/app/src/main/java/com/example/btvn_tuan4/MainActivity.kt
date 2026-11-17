package com.example.btvn_tuan4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.btvn_tuan4.model.AuthUser
import com.example.btvn_tuan4.notification.DeviceTokenRepository
import com.example.btvn_tuan4.notification.FirestoreNotificationLogRepository
import com.example.btvn_tuan4.notification.NotificationChannels
import com.example.btvn_tuan4.notification.ReminderScheduler
import com.example.btvn_tuan4.notification.NotificationDisplay
import com.example.btvn_tuan4.repository.AuthRepository
import com.example.btvn_tuan4.repository.FirestoreTaskRepository
import com.example.btvn_tuan4.repository.IAuthRepository
import com.example.btvn_tuan4.repository.IOnboardingRepository
import com.example.btvn_tuan4.repository.ITaskRepository
import com.example.btvn_tuan4.repository.OnboardingRepository
import com.example.btvn_tuan4.ui.screens.HomeScreen
import com.example.btvn_tuan4.ui.screens.LoginScreen
import com.example.btvn_tuan4.ui.screens.OnboardingScreen
import com.example.btvn_tuan4.ui.screens.ProfileScreen
import com.example.btvn_tuan4.ui.screens.StartSplashScreen
import com.example.btvn_tuan4.ui.screens.TaskDetailScreen
import com.example.btvn_tuan4.ui.screens.TaskListScreen
import kotlinx.coroutines.launch

// Activity khoi chay ung dung UTH SmartTasks
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationChannels.create(this)

        // Mo tu thong bao: ghi "opened" (tracking) va chuyen den task_detail. Bo qua khi xoay man hinh
        val deepLinkTaskId = if (savedInstanceState == null) intent.getStringExtra(NotificationDisplay.EXTRA_TASK_ID) else null
        val eventId = intent.getStringExtra(NotificationDisplay.EXTRA_EVENT_ID)
        val userId = intent.getStringExtra(NotificationDisplay.EXTRA_USER_ID)
        if (deepLinkTaskId != null && eventId != null && userId != null) {
            lifecycleScope.launch {
                runCatching { FirestoreNotificationLogRepository().markOpened(userId, eventId) }
            }
        }

        setContent {
            MaterialTheme {
                AppNavigation(deepLinkTaskId = deepLinkTaskId)
            }
        }
    }
}

// Dieu huong toan bo ung dung
@Composable
fun AppNavigation(deepLinkTaskId: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Khoi tao cac repository dung chung
    val onboardingRepository: IOnboardingRepository = remember { OnboardingRepository() }
    val authRepository: IAuthRepository = remember { AuthRepository() }
    val taskRepository: ITaskRepository = remember { FirestoreTaskRepository(ReminderScheduler(context)) }
    val totalSteps = onboardingRepository.getTotalSteps()

    // Kiem tra trang thai dang nhap ban dau
    val startDestination = if (authRepository.getCurrentUser() != null) "home" else "start_splash"

    // Chi mo chi tiet khi da dang nhap (start = home), tranh bo qua man dang nhap
    LaunchedEffect(deepLinkTaskId) {
        if (deepLinkTaskId != null && startDestination == "home") {
            navController.navigate("task_detail/$deepLinkTaskId")
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Man hinh Splash
        composable("start_splash") {
            StartSplashScreen(
                appMetadata = onboardingRepository.getAppMetaData(),
                onNavigateNext = { navController.navigate("onboarding/1") }
            )
        }

        // 2. Man hinh Onboarding buoc 1
        composable("onboarding/1") {
            val step = onboardingRepository.getOnBoardingStep(0)
            if (step != null) {
                OnboardingScreen(
                    step = step,
                    totalSteps = totalSteps,
                    onNext = { navController.navigate("onboarding/2") },
                    onSkip = { navController.navigate("login") }
                )
            }
        }

        // 3. Man hinh Onboarding buoc 2
        composable("onboarding/2") {
            val step = onboardingRepository.getOnBoardingStep(1)
            if (step != null) {
                OnboardingScreen(
                    step = step,
                    totalSteps = totalSteps,
                    onNext = { navController.navigate("onboarding/3") },
                    onSkip = { navController.navigate("login") }
                )
            }
        }

        // 4. Man hinh Onboarding buoc 3
        composable("onboarding/3") {
            val step = onboardingRepository.getOnBoardingStep(2)
            if (step != null) {
                OnboardingScreen(
                    step = step,
                    totalSteps = totalSteps,
                    onNext = { navController.navigate("login") },
                    onSkip = { navController.navigate("login") }
                )
            }
        }

        // 5. Man hinh dang nhap
        composable("login") {
            LoginScreen(
                authRepository = authRepository,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // 6. Flow Trang chu (UI + API)
        composable("home") {
            // Xin quyen thong bao (Android 13+). Tu choi thi app van chay, worker ghi skipped_permission
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            val currentUser = authRepository.getCurrentUser() ?: AuthUser(
                uid = "default_user",
                displayName = "Khoa",
                email = "khoa@uth.edu.vn",
                photoUrl = null,
                isGoogleLinked = true
            )
            // Dang ky FCM token cua may nay cho user (Phase B); loi mang thi bo qua, lan sau mo app se thu lai
            LaunchedEffect(currentUser.uid) {
                runCatching { DeviceTokenRepository().registerCurrentDevice(currentUser.uid) }
            }
            HomeScreen(
                authUser = currentUser,
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToTaskList = {
                    navController.navigate("task_list") {
                        launchSingleTop = true
                    }
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate("task_detail/$taskId")
                },
                taskRepository = taskRepository
            )
        }

        // 7. Flow Danh sach (UI + API)
        composable("task_list") {
            val currentUser = authRepository.getCurrentUser() ?: AuthUser(
                uid = "default_user",
                displayName = "Khoa",
                email = "khoa@uth.edu.vn",
                photoUrl = null,
                isGoogleLinked = true
            )
            TaskListScreen(
                authUser = currentUser,
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate("task_detail/$taskId")
                },
                taskRepository = taskRepository
            )
        }

        // 8. Flow Chi tiet (UI + API)
        composable(
            route = "task_detail/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: "2"
            val currentUser = authRepository.getCurrentUser() ?: AuthUser(
                uid = "default_user",
                displayName = "Khoa",
                email = "khoa@uth.edu.vn",
                photoUrl = null,
                isGoogleLinked = true
            )
            TaskDetailScreen(
                taskId = taskId,
                authUser = currentUser,
                onNavigateBack = {
                    navController.popBackStack()
                },
                taskRepository = taskRepository
            )
        }

        // 9. Man hinh ho so ca nhan
        composable("profile") {
            val currentUser = authRepository.getCurrentUser() ?: AuthUser(
                uid = "default_user",
                displayName = "Khoa",
                email = "khoa@uth.edu.vn",
                photoUrl = null,
                isGoogleLinked = true
            )
            ProfileScreen(
                authUser = currentUser,
                authRepository = authRepository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOut = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                taskRepository = taskRepository
            )
        }
    }
}
