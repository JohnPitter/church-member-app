package com.churchmanagement.mobile.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Brush animado (shimmer) para placeholders de carregamento. */
@Composable
fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.onSurface
    val colors = listOf(
        base.copy(alpha = 0.06f),
        base.copy(alpha = 0.16f),
        base.copy(alpha = 0.06f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )
    return Brush.linearGradient(
        colors = colors,
        start = Offset(x, 0f),
        end = Offset(x + 300f, 300f),
    )
}

@Composable
fun SkeletonBox(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    Spacer(modifier.clip(shape).background(brush))
}

/** Placeholder de um card de lista (imagem + título + subtítulo). */
@Composable
fun CardSkeleton(brush: Brush, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column {
            SkeletonBox(
                brush = brush,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(0.dp),
            )
            Column(Modifier.padding(16.dp)) {
                SkeletonBox(brush, Modifier.fillMaxWidth(0.7f).height(18.dp))
                Spacer(Modifier.height(10.dp))
                SkeletonBox(brush, Modifier.fillMaxWidth(0.4f).height(14.dp))
            }
        }
    }
}

/** Lista de placeholders, para o estado de carregamento das telas de lista. */
@Composable
fun ListSkeleton(modifier: Modifier = Modifier, count: Int = 5) {
    val brush = shimmerBrush()
    Column(
        modifier = modifier.fillMaxSize().padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(count) { CardSkeleton(brush) }
    }
}
