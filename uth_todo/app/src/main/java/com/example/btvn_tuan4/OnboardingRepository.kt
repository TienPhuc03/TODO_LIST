package com.example.btvn_tuan4



interface IOnboardingRepository{
    fun getAppMetaData(): AppMetaData
    fun getOnBoardingSteps(): List<OnBoardingStep>
    fun getOnBoardingStep(stepIndex: Int): OnBoardingStep?
    fun getTotalSteps(): Int
}

class OnboardingRepository : IOnboardingRepository{
    private val appMetaData = AppMetaData(
        name = "UTH Todo",
        slogan = "Việc học, việc nhà - gọn gàng mỗi ngày",
        version = "2.0.0.6"
    )

    private val steps = listOf(
        OnBoardingStep(
            stepNumber = 1,
            title = "Mọi việc gọn gàng trong một chỗ",
            description = "Ghi lại bài tập, việc nhà, lịch hẹn. Không còn quên việc nào.",
            buttonText = "Tiếp tục",
            illustration = OnBoardingIllustration.ChecklistIllustration(
                tasks = listOf(
                    TaskItem(isChecked = true),
                    TaskItem(isChecked = false),
                    TaskItem(isChecked = false),
                )
            ),
            isLastStep = false
        ),

        OnBoardingStep(
            stepNumber = 2,
            title = "Chia theo Học tập và Cuộc sống",
            description = "Mỗi việc có nhóm riêng, nhìn là biết hôm nay cần lo gì.",
            buttonText = "Tiếp tục",
            illustration = OnBoardingIllustration.CategoryIllustration(
                categories = listOf(
                    CategoryItem(
                        label = "Học tập",
                        textColor = UthColors.CategoryStudyText,
                        backgroundColor = UthColors.CategoryStudyBg
                    ),
                    CategoryItem(
                        label = "Cuộc sống",
                        textColor = UthColors.CategoryLifeText,
                        backgroundColor = UthColors.CategoryLifeBg
                    )
                )
            ),
            isLastStep = false
        ),
        OnBoardingStep(
            stepNumber = 3,
            title = "Nhắc đúng lúc",
            description = "Bật thông báo để UTH Todo nhắc bạn trước hạn chót.",
            buttonText = "Bắt đầu ngay",
            illustration = OnBoardingIllustration.ReminderIllustration(
                title = "Code Lập trình di động",
                time = "19:00"
            ),
            isLastStep = true
        )

    )

    override fun getAppMetaData(): AppMetaData = appMetaData
    override fun getOnBoardingSteps(): List<OnBoardingStep> = steps
    override fun getOnBoardingStep(stepIndex: Int): OnBoardingStep? = steps.getOrNull(stepIndex)
    override fun getTotalSteps(): Int = steps.size
}

