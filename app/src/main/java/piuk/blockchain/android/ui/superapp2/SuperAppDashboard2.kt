package piuk.blockchain.android.ui.superapp2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R

@Composable
fun SuperAppDashboard2() {
    var y by remember {
        mutableStateOf(0F)
    }


    var offset = 0F

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                println("----- onPreScroll $available")

                val old = offset
                offset  = available.y

                val now = old - offset
                println("----- onPreScroll $now")

                return Offset(0f, now)
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .nestedScroll(nestedScrollConnection)) {

        ///////
        ///////
        ///////
        ///////
        ///////
        ///////
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .height(54.dp)
                    .fillMaxWidth()
                    .background(Color.Red)
            ) {
                com.blockchain.componentlib.basic.Image(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(start = dimensionResource(R.dimen.tiny_margin)),
                    imageResource = ImageResource.Local(R.drawable.ic_total_balance_demo)
                )
            }

            Row(
                modifier = Modifier
                    .height(54.dp)
                    .fillMaxWidth()
                    .background(Color.Green),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = dimensionResource(R.dimen.tiny_margin)),
                    style = AppTheme.typography.title3,
                    color = Color.Black,
                    text = "trading"
                )

                Spacer(modifier = Modifier.size(32.dp))

                Text(
                    modifier = Modifier
                        .padding(start = dimensionResource(R.dimen.tiny_margin)),
                    style = AppTheme.typography.title3,
                    color = Color.Black,
                    text = "defi"
                )
            }
        }

        ////////
        ////////
        ////////
        ////////
        ////////
        val aaaaa = mutableListOf<String>()
        (0..40).forEach { aaaaa.add("abc $it") }

        LazyColumn(
            //            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = y }
                //                .pointerInput(Unit) {
                //                    detectTapGestures(
                //                        onPress = {
                //                            scope.coroutineContext.cancelChildren()
                //                            coroutineScopeAnim.coroutineContext.cancelChildren()
                //                            animate = false
                //                        }
                //                    )
                //                }
                .background(Color(0XFFF1F2F7), RoundedCornerShape(20.dp)),
        ) {
            items(
                items = aaaaa,
            ) {
                Text(
                    modifier = Modifier.padding(dimensionResource(R.dimen.very_small_margin)),
                    style = AppTheme.typography.title3,
                    color = Color.Black,
                    text = it
                )
            }

            item {
                Spacer(Modifier.size(dimensionResource(R.dimen.epic_margin)))
            }
        }
    }
}