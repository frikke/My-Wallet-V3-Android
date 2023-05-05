package com.blockchain.componentlib.anim

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import kotlinx.coroutines.delay

@Composable
fun AnimatedAmountCounter(
    modifier: Modifier = Modifier,
    amountText: String,
    style: ComposeTypographies,
    color: ComposeColors,
    gravity: ComposeGravities,
    duration: Long = 666L,
) {
    var oldCount by remember {
        mutableStateOf(amountText)
    }
    SideEffect {
        oldCount = amountText
    }

    Row(modifier = modifier) {
        val oldCountString = oldCount
        for (i in amountText.indices) {
            val oldChar = oldCountString.getOrNull(i)
            val newChar = amountText[i]
            val char = if (oldChar == newChar) {
                oldCountString[i]
            } else {
                amountText[i]
            }
            if (char.isDigit() && (oldChar == null || oldChar.isDigit())) {
                val numbers = remember(char) {
                    val start = oldChar?.toString()?.toIntOrNull() ?: 0
                    val end = char.toString().toIntOrNull() ?: 0
                    createBetweenNumbersList(start, end)
                }
                NumberSwitchAnimation(
                    numbers = numbers,
                    style = style,
                    color = color,
                    gravity = gravity,
                    duration = duration
                )
            } else {
                AnimatedTextContent(
                    text = char.toString(),
                    style = style,
                    color = color,
                    gravity = gravity
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NumberSwitchAnimation(
    numbers: List<Int>,
    style: ComposeTypographies,
    color: ComposeColors,
    gravity: ComposeGravities,
    duration: Long
) {
    var currentNumberIndex by remember(numbers) { mutableStateOf(if (numbers.size > 1) 1 else 0) }
    val factor = if (numbers.first() > numbers.last()) -1 else 1
    val animSpec = if (currentNumberIndex == numbers.size - 1) {
        spring<IntOffset>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
    } else tween(durationMillis = (duration.toInt()) / 2, easing = LinearEasing)

    AnimatedContent(
        targetState = numbers[currentNumberIndex],
        transitionSpec = {
            slideInVertically(
                initialOffsetY = { factor * it },
                animationSpec = animSpec
            ) with slideOutVertically(
                targetOffsetY = { -1 * factor * it },
                animationSpec = animSpec
            )
        },

    ) { number ->
        AnimatedTextContent(number.toString(), style, color, gravity)
    }
    LaunchedEffect(currentNumberIndex, numbers) {
        while (currentNumberIndex < numbers.size - 1) {
            delay(duration / numbers.size)
            currentNumberIndex++
        }
    }
}

@Composable
private fun AnimatedTextContent(
    text: String,
    style: ComposeTypographies,
    color: ComposeColors,
    gravity: ComposeGravities
) {
    SimpleText(
        text = text,
        style = style,
        color = color,
        gravity = gravity,
    )
}

private fun createBetweenNumbersList(startInt: Int, endInt: Int): List<Int> {
    return if (startInt <= endInt) {
        (startInt..endInt).toList()
    } else {
        (endInt..startInt).toList().reversed()
    }
}
