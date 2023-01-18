package com.blockchain.home.presentation.introduction.composable

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Purple0000
import com.blockchain.home.presentation.R
import com.blockchain.walletmode.WalletMode

data class IntroductionScreenContent(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val tag: IntroductionScreenTag? = null,
    val forWalletMode: WalletMode? = null
)

data class IntroductionScreenTag(
    @StringRes val title: Int?,
    val titleColor: Color?,
    @StringRes val description: Int
)
sealed interface IntroductionScreensSetup {
    data class All(val isNewUser: Boolean) : IntroductionScreensSetup
    data class ModesOnly(val startMode: WalletMode) : IntroductionScreensSetup
}

fun introductionsScreens(introductionScreensSetup: IntroductionScreensSetup): List<IntroductionScreenContent> {
    return when (introductionScreensSetup) {
        is IntroductionScreensSetup.All -> {
            listOfNotNull(
                IntroductionScreenContent(
                    image = R.drawable.ic_intro_new_user,
                    title = R.string.intro_new_user_title,
                    description = R.string.intro_new_user_description,
                    tag = IntroductionScreenTag(
                        title = null,
                        titleColor = null,
                        description = R.string.intro_new_user_tag_description
                    )
                ).takeIf { introductionScreensSetup.isNewUser },
                IntroductionScreenContent(
                    image = R.drawable.ic_intro_old_user,
                    title = R.string.intro_existing_user_title,
                    description = R.string.intro_existing_user_description,
                ).takeIf { introductionScreensSetup.isNewUser.not() },
            ) + walletModeContent
        }
        is IntroductionScreensSetup.ModesOnly -> {
            walletModeContent.apply {
                val startItem = first { it.forWalletMode == introductionScreensSetup.startMode }
                remove(startItem)
                add(0, startItem)
            }
        }
    }
}

private val walletModeContent = mutableListOf(
    IntroductionScreenContent(
        image = R.drawable.ic_intro_custodial,
        title = R.string.intro_custodial_title,
        description = R.string.intro_custodial_description,
        tag = IntroductionScreenTag(
            title = R.string.intro_custodial_tag_title,
            titleColor = Blue600,
            description = R.string.intro_custodial_tag_description
        ),
        forWalletMode = WalletMode.CUSTODIAL
    ),
    IntroductionScreenContent(
        image = R.drawable.ic_intro_non_custodial,
        title = R.string.intro_non_custodial_title,
        description = R.string.intro_non_custodial_description,
        tag = IntroductionScreenTag(
            title = R.string.intro_non_custodial_tag_title,
            titleColor = Purple0000,
            description = R.string.intro_non_custodial_tag_description
        ),
        forWalletMode = WalletMode.NON_CUSTODIAL
    )
)
