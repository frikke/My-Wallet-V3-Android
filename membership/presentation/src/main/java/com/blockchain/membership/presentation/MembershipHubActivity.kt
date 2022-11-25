package com.blockchain.membership.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme

class MembershipHubActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            ComposeView(this).apply {
                setContent {
                    AppTheme {
                        Column {
                            SimpleText(
                                text = "Memberships go here",
                                style = ComposeTypographies.Title2,
                                color = ComposeColors.Title,
                                gravity = ComposeGravities.Centre
                            )
                            PrimaryButton(
                                text = "Launch App Icon Change",
                                onClick = {
                                    setResult(
                                        RESULT_OK,
                                        Intent().apply {
                                            putExtra(START_ICON_CHANGE, true)
                                        }
                                    )
                                    finish()
                                }
                            )
                            PrimaryButton(text = "Launch Referrals", onClick = {
                                setResult(
                                    RESULT_OK,
                                    Intent().apply {
                                        putExtra(START_REFERRALS, true)
                                    }
                                )
                                finish()
                            })
                        }
                    }
                }
            }
        )
    }

    companion object {
        const val START_ICON_CHANGE = "START_ICON_CHANGE"
        const val START_REFERRALS = "START_REFERRALS"
        fun newIntent(context: Context): Intent =
            Intent(context, MembershipHubActivity::class.java)
    }
}
