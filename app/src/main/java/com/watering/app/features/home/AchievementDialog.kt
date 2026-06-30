package com.watering.app.features.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
            // 파티클
            ParticleLayer()

            // 카드
            Surface(
                modifier = Modifier
                    .scale(cardScale.value)
                    .alpha(cardAlpha.value)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1A2A4A), Color(0xFF0D1B2A))
                            ),
                            RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 40.dp, vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = achievement.emoji,
                            fontSize = 80.sp,
                            modifier = Modifier.scale(emojiScale.value)
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = achievement.title,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00B4D8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(textAlpha.value)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = achievement.message,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(textAlpha.value)
                        )
                        Spacer(Modifier.height(32.dp))
                        Text(
                            text = "탭해서 닫기",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.3f),
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
        val alpha by transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                tween((2000 / p.speed).toInt(), p.delay, FastOutSlowInEasing),
                RepeatMode.Restart
            ),
            label = "a"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = "✦",
                fontSize = p.size.sp,
                color = Color(0xFF00B4D8).copy(alpha = (0.6f - progress * 0.6f).coerceIn(0f, 1f)),
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
