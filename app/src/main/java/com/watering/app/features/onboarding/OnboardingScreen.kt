package com.watering.app.features.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.watering.app.R
import com.watering.app.ui.theme.AppBackgroundGradient
import com.watering.app.ui.theme.AquaCtaContentColor
import com.watering.app.ui.theme.GoalGlassesEquivalentRow

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    // 다크모드 전환 등 Activity/Compose 재생성 시 진행 중이던 페이지·입력값이 1페이지로 리셋되던
    // 문제(Fable UI 리뷰 2026-07-08 Top 5 #5) — remember 대신 rememberSaveable로 재생성에서 복원
    var page by rememberSaveable { mutableIntStateOf(0) }
    var dailyGoal by rememberSaveable { mutableIntStateOf(8) }
    var cupSize by rememberSaveable { mutableIntStateOf(200) }

    // 알림 권한 결과를 받으면 바로 온보딩 완료
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.completeOnboarding(dailyGoal, cupSize, granted)
        onComplete()
    }

    // Scaffold/Surface 없이 Column만 두면 색 미지정 Text가 테마와 무관하게 기본 검정으로
    // 렌더링되는 Compose 기본 동작 때문에 다크 모드에서 글자가 안 보이는 문제가 있었음 —
    // Surface로 감싸 LocalContentColor를 배경에 맞게 올바르게 전파시킨다
    // 2026-07-08: Surface의 color를 그대로 배경으로 쓰면 커스텀 안 된 M3 기본 다크 배경(순검정)이
    // 노출돼 다른 화면의 딥 틸 그라데이션(AppBackgroundGradient)과 달라 보이는 문제(Fable UI 리뷰
    // findings) — Box로 그라데이션을 깔고, Surface는 투명하게 하되 contentColor만 명시해 텍스트
    // 색 전파 역할은 그대로 유지
    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundGradient)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(64.dp))

                // 뒤로가기(1페이지 이상일 때만) + 페이지 인디케이터 — 목표 잔 수를 잘못 정한 채
                // 다음 페이지로 넘어가도 온보딩 안에서 정정할 수 있도록 함
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (page > 0) {
                        IconButton(
                            onClick = { page-- },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_back)
                            )
                        }
                    }
                    PageIndicator(
                        currentPage = page,
                        totalPages = 3,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(Modifier.height(48.dp))

                // 페이지 콘텐츠
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (page) {
                        0 -> WelcomePage()
                        1 -> GoalPage(
                            goal = dailyGoal,
                            cupSize = cupSize,
                            onGoalChange = { dailyGoal = it },
                            onCupSizeChange = { cupSize = it }
                        )
                        2 -> NotificationPage()
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 다음 / 시작 버튼
                Button(
                    onClick = {
                        if (page < 2) {
                            page++
                        } else {
                            // 알림 페이지에서 "시작하기" → 권한 요청 후 완료
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.completeOnboarding(dailyGoal, cupSize, true)
                                onComplete()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    // 파스텔 아쿠아 배경 위 흰 글자 저대비(약 1.8:1) 수정 — AquaCtaContentColor 주석 참고
                    colors = ButtonDefaults.buttonColors(contentColor = AquaCtaContentColor)
                ) {
                    Text(
                        text = if (page < 2) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_start),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 버튼이 화면 맨 아래에 거의 붙어 보인다는 피드백으로 하단 여백을 넓혀 버튼을
                // 위로 올림(웹 목업 3안 중 "C. 최소 변경" 채택) — 버튼 위치는 이 하단 Spacer
                // 값에 의해서만 결정됨(weight(1f) 콘텐츠 Box가 나머지 공간을 흡수하는 구조라
                // 버튼~콘텐츠 사이 Spacer(32dp)를 건드려도 화면상 버튼 위치는 안 바뀜)
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun PageIndicator(currentPage: Int, totalPages: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                    .background(
                        color = if (index == currentPage)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.WaterDrop,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(40.dp))

        FeatureRow(
            icon = Icons.Default.TouchApp,
            text = stringResource(R.string.onboarding_feature_widget)
        )
        Spacer(Modifier.height(16.dp))
        FeatureRow(
            icon = Icons.Default.NotificationsActive,
            text = stringResource(R.string.onboarding_feature_notification)
        )
        Spacer(Modifier.height(16.dp))
        FeatureRow(
            icon = Icons.Default.Star,
            text = stringResource(R.string.onboarding_feature_streak)
        )
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalPage(
    goal: Int,
    cupSize: Int,
    onGoalChange: (Int) -> Unit,
    onCupSizeChange: (Int) -> Unit
) {
    val cupSizes = listOf(150, 200, 250, 300, 350, 500)
    // "하루 목표"/"컵 크기" 라벨 색 — 다크모드 분기 없이 AquaCtaContentColor(진남색)만 쓰면
    // 다크 배경(딥 틸)과 명도가 거의 같아 텍스트가 안 보이던 버그 수정(2026-07-10, 실기기 확인).
    // 웹 목업 6안 비교 후 다크모드는 onSurfaceVariant(서브타이틀과 같은 톤)로 확정, 라이트는 기존 유지
    val labelColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else AquaCtaContentColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.onboarding_goal_title),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_goal_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        // 하루 목표 +/- 선택
        // 라벨 색: 파스텔 아쿠아 primary가 라벤더~크림 배경 위에서 저대비(Fable UI 리뷰 findings,
        // 2026-07-08) — 웹 목업 비교 후 기존 확립된 진남색(AquaCtaContentColor)으로 통일 (라이트 모드)
        Text(
            text = stringResource(R.string.label_daily_goal),
            style = MaterialTheme.typography.labelLarge,
            color = labelColor
        )

        Spacer(Modifier.height(12.dp))

        // ml이 주역, 잔 수는 보조 정보 — +/- 한 번의 증감폭이 선택된 컵 크기와 같아서
        // (goal은 여전히 "잔 수"로 저장/계산되고, 화면에는 goal * cupSize를 ml로 보여줌)
        // 항상 컵 크기의 정확한 배수가 되어 반올림 오차 없이 "N잔에 해당해요"를 보여줄 수 있음
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onGoalChange(goal - 1) },
                enabled = goal > 1,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.content_description_goal_decrease))
            }
            Text(
                text = stringResource(R.string.onboarding_goal_ml, goal * cupSize),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 140.dp)
            )
            IconButton(
                onClick = { onGoalChange(goal + 1) },
                enabled = goal < 20,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_goal_increase))
            }
        }

        Spacer(Modifier.height(32.dp))

        // 컵 크기 선택
        // "하루 목표" 라벨과 달리 fillMaxWidth()에 textAlign 지정이 빠져 좌측 정렬로 렌더링되던
        // 정렬 불일치 버그 수정(Fable UI 리뷰 findings) — 가운데 정렬로 통일, 색도 위와 동일하게 교체
        Text(
            text = stringResource(R.string.label_cup_size),
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cupSizes.forEach { size ->
                FilterChip(
                    selected = cupSize == size,
                    onClick = { onCupSizeChange(size) },
                    label = { Text("${size}ml") }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        GoalGlassesEquivalentRow(glasses = goal)
    }
}

@Composable
private fun NotificationPage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_notification_title),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_notification_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_notification_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
