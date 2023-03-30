package com.blockchain.presentation.customviews.kyc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.common.R
import com.blockchain.common.databinding.DialogSheetKycUpgradeNowBinding
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.emptySubscribe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

class KycUpgradeNowSheet : SlidingModalBottomDialog<DialogSheetKycUpgradeNowBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun startKycClicked()
    }

    private val disposables = CompositeDisposable()

    private val kycService: KycService by scopedInject()

    private var ctaClicked = false
    private val getHighestTier: Single<KycTier> by lazy {
        kycService.getHighestApprovedTierLevelLegacy().cache()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // This is because we need to show this as a regular fragment as well as a BottomSheet
        if (!showsDialog) {
            val binding = initBinding(inflater, container)
            initControls(binding)
            return binding.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetKycUpgradeNowBinding =
        DialogSheetKycUpgradeNowBinding.inflate(inflater, container, false).apply {
            composeView.setContent {
                KycUpgradeNowScreen(
                    startKycClicked = {
                        ctaClicked = true
                        startKycClicked()
                    }
                )
            }
        }

    override fun initControls(binding: DialogSheetKycUpgradeNowBinding): Unit = with(binding) {
        if (!showsDialog) {
            sheetIndicator.gone()
            toolbar.gone()
        } else {
            toolbar.apply {
                startNavigationButton = null
                endNavigationBarButtons = listOf(
                    NavigationBarButton.Icon(
                        drawable = R.drawable.ic_close_circle_v2,
                        color = null,
                        contentDescription = R.string.accessibility_close,
                        onIconClick = {
                            if (showsDialog) dismiss()
                        }
                    )
                )
            }
        }
        // prep stream for onDestroyView call
        getHighestTier.emptySubscribe()
    }

    private fun startKycClicked() {
        (host as Host).startKycClicked()
        if (showsDialog) dismiss()
    }

    override fun onDestroyView() {
        if (!ctaClicked) {
            disposables += getHighestTier.subscribe { highestTier ->
                analytics.logEvent(KycUpgradeNowDismissed(highestTier))
            }
        }
        disposables.dispose()
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): KycUpgradeNowSheet = KycUpgradeNowSheet()
    }
}
