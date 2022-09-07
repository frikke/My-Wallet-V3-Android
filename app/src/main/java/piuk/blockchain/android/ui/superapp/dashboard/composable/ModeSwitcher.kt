package piuk.blockchain.android.ui.superapp.dashboard.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import kotlinx.coroutines.cancelChildren

@Composable
fun ModeSwitcher(
    modifier: Modifier = Modifier,
    onTradingClick: () -> Unit,
    onDefiClick: () -> Unit
) {
    val modes = listOf("Trading", "DeFi")

    val coroutineScopeAnim = rememberCoroutineScope()

    var mode by remember {
        mutableStateOf("Trading")
    }

    var selectedMode by remember {
        mutableStateOf("Trading")
    }

    var maxIndicatorWidth = 16F

    Row(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1F))

        var width = remember { Animatable(0F) }

        LaunchedEffect(mode) {
            width.snapTo(maxIndicatorWidth - width.value)
            selectedMode = mode
            width.animateTo(
                targetValue = 16F,
                animationSpec = tween(
                    durationMillis = 400
                )
            )
        }


        Column(
            modifier = Modifier
                .fillMaxHeight()
                .clickableNoEffect {
                    coroutineScopeAnim.coroutineContext.cancelChildren()
                    mode = "Trading"
                    onTradingClick()
                    //                    coroutineScopeAnim.launch {
                    //                        width.animateTo(
                    //                            targetValue = if (tradingEnabled) 16F else 0F,
                    //                            animationSpec = tween(
                    //                                durationMillis = 400
                    //                            )
                    //                        )
                    //                    }
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier,
                //                                        .padding(start = dimensionResource(R.dimen.tiny_margin)),
                //                                        .clickableNoEffect {
                //                                            shouldFlash = true
                //                                            switch = true
                //                                        },
                style = AppTheme.typography.title3,
                //                                    color = Color.Black,
                text = "Trading"
            )

            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if(selectedMode == "Trading") width.value.dp else (maxIndicatorWidth - width.value).dp)
                    .background(
                        color = Color.White.copy(alpha = if(selectedMode == "Trading")(width.value / maxIndicatorWidth) else 1 - (width.value / maxIndicatorWidth)),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.size(32.dp))

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .clickableNoEffect {
                    coroutineScopeAnim.coroutineContext.cancelChildren()
                    mode = "DeFi"
                    onDefiClick()
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier,
                //                                        .padding(start = dimensionResource(R.dimen.tiny_margin)),
                //                                        .clickableNoEffect {
                //                                            shouldFlash = true
                //                                            switch = true
                //                                        },
                style = AppTheme.typography.title3,
                //                                    color = Color.Black,
                text = "DeFi"
            )

            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if(selectedMode == "DeFi") width.value.dp else (maxIndicatorWidth - width.value).dp)
                    .background(
                        color = Color.White.copy(alpha = if(selectedMode == "DeFi")(width.value / maxIndicatorWidth) else 1 - (width.value / maxIndicatorWidth)),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.weight(1F))
    }
}