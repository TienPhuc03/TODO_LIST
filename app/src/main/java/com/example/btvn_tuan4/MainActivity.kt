package com.example.btvn_tuan4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.btvn_tuan4.model.AuthUser
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

// Activity khoi chay ung dung UTH SmartTasks
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

// Dieu huong toan bo ung dung
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Khoi tao cac repository dung chung
    val onboardingRepository: IOnboardingRepository = remember { OnboardingRepository() }
    val authRepository: IAuthRepository = remember { AuthRepository() }
    val taskRepository: ITaskRepository = remember { FirestoreTaskRepository() }
    val totalSteps = onboardingRepository.getTotalSteps()

    // Kiem tra trang thai dang nhap ban dau
    val startDestination = if (authRepository.getCurrentUser() != null) "home" else "start_splash"

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
            val currentUser = authRepository.getCurrentUser() ?: AuthUser(
                uid = "default_user",
                displayName = "Khoa",
                email = "khoa@uth.edu.vn",
                photoUrl = null,
                isGoogleLinked = true
            )
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
