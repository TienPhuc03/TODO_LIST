package com.example.btvn_tuan4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import com.example.btvn_tuan4.model.AppMetaData
import com.example.btvn_tuan4.model.OnBoardingStep
import com.example.btvn_tuan4.repository.OnboardingRepository
import com.example.btvn_tuan4.ui.components.RenderIllustration
import com.example.btvn_tuan4.ui.components.StepIndicator
import com.example.btvn_tuan4.ui.components.UthColors
import com.example.btvn_tuan4.ui.components.UthTodoLogo

// Man hinh khoi dong Splash
@Composable
fun StartSplashScreen(
    appMetadata: AppMetaData,
    onNavigateNext: () -> Unit
) {
    // Tu dong chuyen man hinh sau 3s
    LaunchedEffect(Unit) {
        delay(3000)
        onNavigateNext()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UthColors.Primary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable { onNavigateNext() }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UthTodoLogo()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = appMetadata.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = appMetadata.slogan,
                color = Color(0xFFDCE7FE),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = appMetadata.version,
            color = Color(0xFFA5C2FE),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

// Man hinh Onboarding dung chung
@Composable
fun OnboardingScreen(
    step: OnBoardingStep,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Scaffold(
        containerColor = UthColors.Background,
        topBar = {
            // Thanh tieu de tren co dem an toan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UthTodoLogo(isSmall = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UTH Todo",
                        color = UthColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                Text(
                    text = "Bỏ qua",
                    color = UthColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onSkip() }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Hinh minh hoa cho tung buoc
            RenderIllustration(Illustration = step.illustration)

            // Noi dung huong dan va cham chi bao
            Column {
                Text(
                    text = step.title,
                    color = UthColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = step.description,
                    color = UthColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Cham chi bao vi tri buoc hien tai
                StepIndicator(currentStep = step.stepNumber, totalSteps = totalSteps)
            }

            // Nut chuyen tiep sang buoc tiep theo
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UthColors.Primary)
            ) {
                Text(
                    text = step.buttonText,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Preview man hinh Splash
@Preview(showBackground = true, showSystemUi = true, name = "1. Start Screen")
@Composable
fun StartScreenPreview() {
    val repository = OnboardingRepository()
    MaterialTheme {
        StartSplashScreen(
            appMetadata = repository.getAppMetaData(),
            onNavigateNext = {}
        )
    }
}

// Preview Onboarding buoc 1
@Preview(showBackground = true, showSystemUi = true, name = "2. Onboarding - Bước 1")
@Composable
fun OnboardingStep1Preview() {
    val repository = OnboardingRepository()
    val step = repository.getOnBoardingStep(0)!!
    MaterialTheme {
        OnboardingScreen(
            step = step,
            totalSteps = repository.getTotalSteps(),
            onNext = {},
            onSkip = {}
        )
    }
}

// Preview Onboarding buoc 2
@Preview(showBackground = true, showSystemUi = true, name = "3. Onboarding - Bước 2")
@Composable
fun OnboardingStep2Preview() {
    val repository = OnboardingRepository()
    val step = repository.getOnBoardingStep(1)!!
    MaterialTheme {
        OnboardingScreen(
            step = step,
            totalSteps = repository.getTotalSteps(),
            onNext = {},
            onSkip = {}
        )
    }
}

// Preview Onboarding buoc 3
@Preview(showBackground = true, showSystemUi = true, name = "4. Onboarding - Bước 3")
@Composable
fun OnboardingStep3Preview() {
    val repository = OnboardingRepository()
    val step = repository.getOnBoardingStep(2)!!
    MaterialTheme {
        OnboardingScreen(
            step = step,
            totalSteps = repository.getTotalSteps(),
            onNext = {},
            onSkip = {}
        )
    }
}
