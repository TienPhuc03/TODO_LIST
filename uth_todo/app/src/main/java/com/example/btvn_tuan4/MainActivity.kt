package com.example.btvn_tuan4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Khởi tạo đối tượng Repository theo OOP
    val repository: IOnboardingRepository = remember { OnboardingRepository() }
    val totalSteps = repository.getTotalSteps()

    NavHost(
        navController = navController,
        startDestination = "start_splash"
    ) {
        // 1. Màn hình Start (Splash)
        composable("start_splash") {
            StartSplashScreen(
                appMetadata = repository.getAppMetaData(),
                onNavigateNext = { navController.navigate("onboarding/1") }
            )
        }

        // 2. Màn hình Onboarding 1
        composable("onboarding/1") {
            val step = repository.getOnBoardingStep(0)
            if (step != null) {
                OnboardingScreen(
                    step = step,
                    totalSteps = totalSteps,
                    onNext = { navController.navigate("onboarding/2") },
                    onSkip = { navController.navigate("home") }
                )
            }
        }

        // 3. Màn hình Onboarding 2
        composable("onboarding/2") {
            val step = repository.getOnBoardingStep(1)
            if (step != null) {
                OnboardingScreen(
                    step = step,
                    totalSteps = totalSteps,
                    onNext = { navController.navigate("onboarding/3") },
                    onSkip = { navController.navigate("home") }
                )
            }
        }

        // 4. Màn hình Onboarding 3
        composable("onboarding/3") {
            val step = repository.getOnBoardingStep(2)
            if (step != null) {
                OnboardingScreen(
                    step = step,
                    totalSteps = totalSteps,
                    onNext = { navController.navigate("home") },
                    onSkip = { navController.navigate("home") }
                )
            }
        }

        // 5. Màn hình chính sau khi hoàn thành Onboarding
        composable("home") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Chào mừng bạn đến với UTH Todo!", fontSize = 18.sp)
            }
        }
    }
}