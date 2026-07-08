package com.watering.app.features.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.watering.app.R
import com.watering.app.core.model.Achievement
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun AchievementDialog(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        val bgAlpha = remember { Animatable(0f) }
        val cardScale = remember { Animatable(0.6f) }
        val cardAlpha = remember { Animatable(0f) }
        val emojiScale = remember { Animatable(0f) }
        val textAlpha = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            launch { bgAlpha.animateTo(1f, tween(300)) }
            delay(100)
            launch {
                cardAlpha.animateTo(1f, tween(250))
                cardScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
            }
            delay(200)
            launch {
                emojiScale.animateTo(1.2f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
                emojiScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
            }
            delay(350)
            textAlpha.animateTo(1f, tween(300))
        }

        // 다크모드 카드 톤 (Fable UI 리뷰 findings — 성취 다이얼로그, 2026-07-08)
        // 라이트 연노랑 그라데이션을 다크에서도 그대로 써서 눈부심 문제가 있던 것을, v0.31.26
        // (다크모드 안내 배너)에서 확립한 "딥 틸 채우기 + 골드 보더" 패턴으로 통일
        val isDark = isSystemInDarkTheme()
        val cardBorderColor = if (isDark) Color(0xFFF5C860) else Color(0xFFE9DC86)
        val titleColor = if (isDark) Color(0xFFF7EDB8) else Color(0xFF7A6B28)
        val messageColor = if (isDark) Color(0xFFCBB980) else Color(0xFF96874A)
        // "탭해서 닫기" 힌트가 실제 액션 텍스트인데도 타이틀·메시지보다 옅은 색이던 위계 역전 수정
        // — 라이트는 타이틀보다 더 진한 올리브로, 다크는 보더와 동일한 골드로 가장 눈에 띄게
        val hintColor = if (isDark) Color(0xFFF5C860) else Color(0xFF6B5D1F)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(bgAlpha.value)
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // 파티클 — 상태바 영역까지 침범하던 문제(Fable UI 리뷰 findings) 수정, 콘텐츠 영역으로 제한
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                ParticleLayer()
            }

            // 카드 바깥으로 은은하게 퍼지는 앰비언트 발광
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .alpha(cardAlpha.value)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFE9DC86).copy(alpha = 0.32f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 카드 — "탭해서 닫기" 안내대로 카드 전체도 탭하면 닫히게 함(예전엔 onClick={}로 탭을
            // 삼켜서 안내 라벨을 정확히 눌러야만 닫혔음, 2026-07-08 Fable UI 리뷰로 발견)
            Surface(
                modifier = Modifier
                    .scale(cardScale.value)
                    .alpha(cardAlpha.value)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .then(
                            if (isDark) {
                                Modifier.background(Color(0xFF1C4D49), RoundedCornerShape(32.dp))
                            } else {
                                Modifier.background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFFFFFEF4), Color(0xFFF7EDB8))
                                    ),
                                    RoundedCornerShape(32.dp)
                                )
                            }
                        )
                        .border(if (isDark) 1.5.dp else 1.dp, cardBorderColor, RoundedCornerShape(32.dp))
                        .padding(horizontal = 40.dp, vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            // 이모지 뒤 은은한 발광 효과
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .scale(emojiScale.value)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFFFFFFFF).copy(alpha = 0.5f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            Text(
                                text = achievement.emoji,
                                fontSize = 80.sp,
                                modifier = Modifier.scale(emojiScale.value)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = stringResource(achievement.titleRes),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(textAlpha.value)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(achievement.messageRes),
                            fontSize = 16.sp,
                            color = messageColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(textAlpha.value)
                        )
                        Spacer(Modifier.height(32.dp))
                        Text(
                            text = stringResource(R.string.achievement_dismiss_hint),
                            fontSize = 13.sp,
                            color = hintColor,
                            modifier = Modifier
                                .alpha(textAlpha.value)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticleLayer() {
    val particles = remember {
        List(20) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 14f + 6f,
                speed = Random.nextFloat() * 0.4f + 0.2f,
                delay = Random.nextInt(600)
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "particles")

    particles.forEach { p ->
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween((2000 / p.speed).toInt(), p.delay, LinearEasing),
                RepeatMode.Restart
            ),
            label = "p"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = "✦",
                fontSize = p.size.sp,
                color = Color(0xFFE0CB54).copy(alpha = (0.6f - progress * 0.6f).coerceIn(0f, 1f)),
                modifier = Modifier.offset {
                    IntOffset(
                        x = (p.x * 900).dp.roundToPx(),
                        y = ((p.y - progress * 0.3f) * 1800).dp.roundToPx()
                    )
                }
            )
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val delay: Int
)
