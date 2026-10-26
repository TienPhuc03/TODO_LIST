package com.example.btvn_tuan4

import androidx.compose.ui.graphics.Color

//dong goi thong tin
data class AppMetaData (
    val name: String,
    val slogan: String,
    val version: String
)
// sealed interface cho tinh da hinh cua cac thong tren 3 srceen
sealed interface OnBoardingIllustration{
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

data class TaskItem(
    val isChecked: Boolean,
)

data class CategoryItem(
    val label: String,
    val textColor: Color,
    val backgroundColor: Color
)


//dong goi toan bo thong tin tren 3 screen
data class OnBoardingStep (
    val stepNumber: Int,
    val title: String,
    val description: String,
    val buttonText: String,
    val illustration: OnBoardingIllustration,
    val isLastStep: Boolean = false
)
