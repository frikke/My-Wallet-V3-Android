package piuk.blockchain.android.ui.collapseheader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import kotlinx.coroutines.launch
import piuk.blockchain.android.R

@Composable
fun Main() {
    var trading by remember {
        mutableStateOf(true)
    }

    var shouldFlash by remember { mutableStateOf(false) }

    val animateDown by animateIntAsState(
        targetValue = if (shouldFlash) 200 else 0,
        finishedListener = {
            if(shouldFlash)  {
                trading = trading.not()
                shouldFlash = false
            }
        },
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 0
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Text(
                modifier = Modifier
                    .padding(start = dimensionResource(R.dimen.tiny_margin))
                    .clickableNoEffect {
                        shouldFlash = true
                    },
                style = AppTheme.typography.title3,
                color = Color.Black,
                text = "TRADING"
            )

            Spacer(modifier = Modifier.size(32.dp))

            Text(
                modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                style = AppTheme.typography.title3,
                color = Color.Black,
                text = "DEFI"
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                (0..40).forEach {
                    Text(
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = "txt $it"
                    )
                }
            }

            Box(modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset {
                    IntOffset(
                        x = 0,
                        animateDown
                    )
                }) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .background(Color.Cyan)

                ) {
                    Text(
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = "home"
                    )

                    Text(
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = "trade"
                    )

                    Text(
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = if(trading) "card" else "nft"
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMain() {
    Main()
}