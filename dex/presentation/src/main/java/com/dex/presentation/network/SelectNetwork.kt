package com.dex.presentation.network

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun SelectNetwork(
    closeClicked: () -> Unit,
    viewModel: SelectNetworkViewModel = getViewModel(scope = payloadScope)
) {
    val viewState: SelectNetworkViewState by viewModel.viewState.collectAsStateLifecycleAware()
    SelectNetworkScreen(
        networks = viewState.networks,
        networkOnClick = { network ->
            viewModel.onIntent(SelectNetworkIntent.UpdateSelectedNetwork(chainId = network.chainId))
            closeClicked()
        },
        closeClicked = closeClicked
    )
}

@Composable
private fun SelectNetworkScreen(
    networks: DataResource<List<DexNetworkViewState>>,
    networkOnClick: (DexNetworkViewState) -> Unit,
    closeClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
    ) {
        SheetHeader(
            title = stringResource(id = com.blockchain.stringResources.R.string.select_network),
            onClosePress = closeClicked
        )

        Networks(
            networks = networks,
            networkOnClick = networkOnClick
        )
    }
}

@Composable
private fun ColumnScope.Networks(
    networks: DataResource<List<DexNetworkViewState>>,
    networkOnClick: (DexNetworkViewState) -> Unit
) {
    when (networks) {
        DataResource.Loading -> {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            ShimmerLoadingCard()
        }

        is DataResource.Error -> {
            // todo
        }

        is DataResource.Data -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.smallSpacing)
            ) {
                roundedCornersItems(networks.data) {
                    Network(
                        network = it,
                        onClick = networkOnClick
                    )
                }
            }
        }
    }
}

@Composable
private fun Network(
    network: DexNetworkViewState,
    onClick: (DexNetworkViewState) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = { onClick(network) })
            .padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            ImageResource.Remote(
                url = network.logo,
                size = AppTheme.dimensions.standardSpacing
            )
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        Text(
            modifier = Modifier.weight(1F),
            text = network.name,
            style = AppTheme.typography.paragraph2,
            color = AppTheme.colors.title
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        if (network.selected) {
            Image(
                Icons.Filled.Check
                    .withSize(AppTheme.dimensions.standardSpacing)
                    .withTint(AppTheme.colors.primary)
            )
        } else {
            val color = AppColors.medium
            Canvas(
                modifier = Modifier.size(AppTheme.dimensions.standardSpacing),
                onDraw = {
                    drawCircle(
                        color = color,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSelectNetworkScreen() {
    SelectNetworkScreen(
        networks = DataResource.Data(
            listOf(
                DexNetworkViewState(
                    chainId = 0,
                    logo = "",
                    name = "Ethereum",
                    selected = true
                ),
                DexNetworkViewState(
                    chainId = 0,
                    logo = "",
                    name = "Polygon",
                    selected = false
                )
            )
        ),
        networkOnClick = {},
        closeClicked = {}
    )
}

@Preview
@Composable
private fun PreviewNetworkSelected() {
    Network(
        network = DexNetworkViewState(
            chainId = 0,
            logo = "",
            name = "Ethereum",
            selected = true
        ),
        onClick = {}
    )
}

@Preview
@Composable
private fun PreviewNetworkUnselected() {
    Network(
        network = DexNetworkViewState(
            chainId = 0,
            logo = "",
            name = "Ethereum",
            selected = false
        ),
        onClick = {}
    )
}
