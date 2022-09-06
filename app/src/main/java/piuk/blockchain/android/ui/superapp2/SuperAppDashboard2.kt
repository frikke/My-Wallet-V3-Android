package piuk.blockchain.android.ui.superapp2

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.compose.rememberNavController
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.END_DEFI
import com.blockchain.componentlib.theme.END_TRADING
import com.blockchain.componentlib.theme.START_DEFI
import com.blockchain.componentlib.theme.START_TRADING
import com.blockchain.componentlib.utils.clickableNoEffect
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.superapp.dashboard.composable.BottomNavigationC
import piuk.blockchain.android.ui.superapp.dashboard.composable.NavigationGraph
import piuk.blockchain.android.ui.superapp.dashboard.toolbar.EnterAlwaysCollapsedState
import piuk.blockchain.android.ui.superapp.dashboard.toolbar.ToolbarState

@Composable
private fun rememberToolbarState(toolbarHeightRange: IntRange): ToolbarState {
    return rememberSaveable(saver = EnterAlwaysCollapsedState.Saver) {
        EnterAlwaysCollapsedState(toolbarHeightRange)
    }
}

//lateinit var toolbarState: EnterAlwaysCollapsedState

@Composable
fun SuperAppDashboard2() {
    //    var heightIs by remember {
    //        mutableStateOf(0)
    //    }

    val minHeight = LocalDensity.current.run { 54.dp.toPx() }
    val maxHeight = LocalDensity.current.run { 108.dp.toPx() }

    val listState = rememberLazyListState()

    var toolbarState = rememberToolbarState(minHeight.toInt()..maxHeight.toInt())

    //    if (heightIs > 0) {
    //        println("-----  heightIs ${heightIs}")
    //        toolbarState = rememberToolbarState((heightIs / 2)..(heightIs))
    //    }
    var firstVisibleItemIndex by remember {
        mutableStateOf(0)
    }

    var firstVisibleItemScrollOffset by remember {
        mutableStateOf(0)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                //                if (::toolbarState.isInitialized) {
                toolbarState.scrollTopLimitReached =
                    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

                toolbarState.scrollOffset = toolbarState.scrollOffset - available.y
                return Offset(0f, toolbarState.consumed)
                //                }

                //                return super.onPreScroll(available, source)
            }
        }
    }

    // list y
    var headerBottomY by remember {
        mutableStateOf(0F)
    }
    //

    // background color
    var switch by remember { mutableStateOf(true) }
    val startColor by animateColorAsState(
        targetValue = if (switch) START_TRADING else START_DEFI,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 0
        )
    )

    val endColor by animateColorAsState(
        targetValue = if (switch) END_TRADING else END_DEFI,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 0
        )
    )
    //

    val navController = rememberNavController()

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        startColor,
                        endColor
                    )
                )
            )
    ) {
        val (statusBar, navBar, content, nav) = createRefs()

        Column(
            modifier = Modifier
                .constrainAs(content) {
                    start.linkTo(parent.start)
                    top.linkTo(statusBar.bottom)
                    end.linkTo(parent.end)
                    bottom.linkTo(navBar.top)
                    height = Dimension.fillToConstraints
                }

                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {

            /////// header
            ///////
            ///////
            ///////
            ///////
            ///////
            Column(modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    //                if (::toolbarState.isInitialized) {
                    translationY = -toolbarState.scrollOffset
                    headerBottomY = -toolbarState.scrollOffset
                    //                }
                }
                .onGloballyPositioned { coordinates ->
                    //                println("-----  coordinates.size.height ${coordinates.size.height}")
                    //                heightIs = coordinates.size.height
                }
            ) {
                Box(
                    modifier = Modifier
                        .height(54.dp)
                        .fillMaxWidth()
                    /*.background(Color.Red)*/
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
                    /*.background(Color.Green)*/,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = dimensionResource(R.dimen.tiny_margin))
                            .clickableNoEffect { switch = true },
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = "trading"
                    )

                    Spacer(modifier = Modifier.size(32.dp))

                    Text(
                        modifier = Modifier
                            .padding(start = dimensionResource(R.dimen.tiny_margin))
                            .clickableNoEffect { switch = false },
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = "defi"
                    )
                }
            }

            //////// content
            ////////
            ////////
            ////////
            ////////
            NavigationGraph(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = headerBottomY
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                            }
                        )
                    },
                navController = navController
            ) {
                firstVisibleItemIndex = it.first
                firstVisibleItemScrollOffset = it.second
            }
        }

        BottomNavigationC(
            modifier = Modifier
                .padding(20.dp)
                .constrainAs(nav) {
                    start.linkTo(parent.start)
                    bottom.linkTo(navBar.top)
                    end.linkTo(parent.end)
                },
            navController
        ) {
        }

        // status bar
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            modifier = Modifier
                .constrainAs(statusBar) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }

                .fillMaxWidth()
                .height(statusBarHeight)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            startColor,
                            endColor
                        )
                    )
                )
        )

        // nav bar
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier
                .constrainAs(navBar) {
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }

                .fillMaxWidth()
                .height(navBarHeight)
                .background(Color.Blue.copy(alpha = 0.5F))
        )
    }
}