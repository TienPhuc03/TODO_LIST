package com.example.btvn_tuan4.model

import androidx.compose.ui.graphics.Color

// Dong goi thong tin ung dung
data class AppMetaData(
    val name: String,
    val slogan: String,
    val version: String
)

// Sealed interface da hinh cho hinh minh hoa onboarding
sealed interface OnBoardingIllustration {
    data class ChecklistIllustration(
        val tasks: List<TaskItem>
    ) : OnBoardingIllustration

    data class CategoryIllustration(
        val categories: List<CategoryItem>
    ) : OnBoardingIllustration

    data class ReminderIllustration(
        val title: String,
        val time: String
    ) : OnBoardingIllustration
}

// Item cong viec checklist
data class TaskItem(
    val isChecked: Boolean
)

// Item the danh muc
data class CategoryItem(
    val label: String,
    val textColor: Color,
    val backgroundColor: Color
)

// Dong goi thong tin moi buoc onboarding
data class OnBoardingStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val buttonText: String,
    val illustration: OnBoardingIllustration,
    val isLastStep: Boolean = false
)
