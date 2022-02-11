package com.blockchain.walletconnect.ui.dapps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi.MviComposeFragment
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.koin.scopedInject
import com.blockchain.walletconnect.R
import com.blockchain.walletconnect.domain.WalletConnectSession

class DappsListFragment :
    MviComposeFragment<DappsListModel, DappsListIntent, DappsListState>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.process(DappsListIntent.LoadDapps)
    }

    companion object {
        fun newInstance() = DappsListFragment()
    }

    override fun onResume() {
        super.onResume()
        updateToolbar(
            toolbarTitle = getString(R.string.account_wallet_connect),
            menuItems = emptyList()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Dapps(model)
            }
        }
    }
    override val model: DappsListModel by scopedInject()
}

@Composable
private fun Dapps(model: DappsListModel) {
    val state by model.state.subscribeAsState(null)
    state?.let {
        if (it.connectedSessions.isEmpty()) {
            renderNoDapps()
        } else {
            dappsList(it.connectedSessions)
        }
    }
}

@Composable
private fun dappsList(sessions: List<WalletConnectSession>) {
    LazyColumn {
        items(
            items = sessions,
            itemContent = {
                DappListItem(it)
            }
        )
    }
}

@Composable
private fun DappListItem(session: WalletConnectSession) {
    Column {
        DefaultTableRow(
            startImageResource = ImageResource.Remote(
                session.dAppInfo.peerMeta.uiIcon()
            ),
            primaryText = session.dAppInfo.peerMeta.name,
            secondaryText = session.dAppInfo.peerMeta.url,
            endImageResource = ImageResource.Local(R.drawable.ic_more_horizontal),
            onClick = {}
        )
        Divider(color = AppTheme.colors.light, thickness = 1.dp)
    }
}

@Composable
private fun renderNoDapps() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = dimensionResource(R.dimen.size_epic)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            contentScale = ContentScale.None,
            imageResource = ImageResource.Local(R.drawable.ic_world_blue)
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))
        Text(
            text = stringResource(R.string.no_dapps_connected),
            style = AppTheme.typography.title3,
            color = AppTheme.colors.title
        )
        Text(
            text = stringResource(R.string.connect_with_wallet_connect),
            style = AppTheme.typography.paragraph1,
            color = AppTheme.colors.medium
        )
    }
}
