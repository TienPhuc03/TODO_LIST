package com.example.btvn_tuan4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UthTodoLogo (
    modifier: Modifier = Modifier, isSmall: Boolean = false
){
    val boxSize = if (isSmall) 32.dp else 90.dp
    val cornerRadius = if (isSmall) 8.dp else 22.dp

    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ){
        Column(
            modifier = Modifier
                .padding(if (isSmall) 4.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isSmall) 3.dp else 8.dp)
        ) {
            //da hoan thanh
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size (if (isSmall) 6.dp else 16.dp)
                        .clip(CircleShape)
                        .background(UthColors.Primary)
                )
                 Spacer(modifier = Modifier.width(if (isSmall) 3.dp else 8.dp))

                Box(
                    modifier = Modifier
                        .width( if  (isSmall) 14.dp else 36.dp)
                        .height(if (isSmall) 2.dp else 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(UthColors.Primary)
                )
            }
            repeat(2){
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size (if (isSmall) 6.dp else 16.dp)
                            .clip(CircleShape)
                            .background(UthColors.Primary)
                    )
                    Spacer(modifier = Modifier.width(if (isSmall) 3.dp else 8.dp))

                    Box(
                        modifier = Modifier
                            .width( if  (isSmall) 14.dp else 36.dp)
                            .height(if (isSmall) 2.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(UthColors.Primary)
                    )
                }
            }
        }
    }
}

//ve da hinh tuy vao loai srceen con de ve hinh tuong ung
@Composable
fun RenderIllustration (Illustration: OnBoardingIllustration, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(UthColors.SurfaceCard),
        contentAlignment = Alignment.Center
    ) {
        when (Illustration){
            //khung the srceen 1
            is OnBoardingIllustration.ChecklistIllustration -> {
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(18.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Illustration.tasks.forEach {  task ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (task.isChecked) UthColors.Primary else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (task.isChecked){
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if  (task.isChecked) Color(0xFFE2E8F0) else Color(0xFFF1F5F9))
                                )
                            }
                        }
                    }
                }
            }
            is OnBoardingIllustration.CategoryIllustration -> {
                //khung the chua srceen 2
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding( vertical = 24.dp , horizontal = 18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Illustration.categories.forEach {
                            categoryItem ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(categoryItem.backgroundColor)
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = categoryItem.label,
                                    color = categoryItem.textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            is OnBoardingIllustration.ReminderIllustration -> {
                //khung the srceen 3
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(vertical = 28.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = UthColors.Primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))

                        //the nhac nho
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 14.dp , vertical = 8.dp)
                        ) {
                            Text(
                                text = "${Illustration.title} lúc ${Illustration.time}",
                                color = UthColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicator (currentStep: Int, totalSteps: Int, modifier: Modifier = Modifier){
    Row(
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for ( i in 1..totalSteps){
            val isActive = i == currentStep
            Box (
                modifier = Modifier
                    .size (width = if (isActive) 16.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if(isActive) UthColors.Primary else UthColors.DotInactive )
            )
        }
    }
}

