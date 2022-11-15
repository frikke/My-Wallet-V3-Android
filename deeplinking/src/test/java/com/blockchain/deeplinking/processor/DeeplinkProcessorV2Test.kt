package com.blockchain.deeplinking.processor

import android.net.Uri
import com.blockchain.deeplinking.navigation.Destination
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeeplinkProcessorV2Test {

    private val deeplinkProcessorV2Subject = DeeplinkProcessorV2()

    @Test
    fun process() {
    }

    @Test
    fun `test parse of assetview deeplink URI`() {
        val assetViewTestURL = Uri.parse("https://www.login.blockchain.com/app/asset?code=BTC")
        val test = deeplinkProcessorV2Subject.process(assetViewTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetViewDestination &&
                (deeplinkResult.destination as Destination.AssetViewDestination).networkTicker == "BTC"
        }
    }

    @Test
    fun `given buy uri with only crypto ticker when parsing then link is valid`() {
        val assetBuyTestURL = Uri.parse("https://www.login.blockchain.com/app/asset/buy?code=BTC")
        val test = deeplinkProcessorV2Subject.process(assetBuyTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetBuyDestination &&
                (deeplinkResult.destination as Destination.AssetBuyDestination).networkTicker == "BTC"
        }
    }

    @Test
    fun `given buy uri with no crypto ticker when parsing then link is valid`() {
        val assetBuyTestURL = Uri.parse("https://www.login.blockchain.com/app/asset/buy")
        val test = deeplinkProcessorV2Subject.process(assetBuyTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultUnknownLink
        }
    }

    @Test
    fun `given buy uri with crypto and amount when parsing then link is valid`() {
        val assetBuyTestURL = Uri.parse("https://www.login.blockchain.com/app/asset/buy?code=BTC&amount=50")
        val test = deeplinkProcessorV2Subject.process(assetBuyTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetBuyDestination &&
                (deeplinkResult.destination as Destination.AssetBuyDestination).networkTicker == "BTC" &&
                (deeplinkResult.destination as Destination.AssetBuyDestination).amount == "50"
        }
    }

    @Test
    fun `given buy uri with crypto, amount and fiat, when parsing then link is valid`() {
        val assetBuyTestURL =
            Uri.parse("https://www.login.blockchain.com/app/asset/buy?code=BTC&amount=50&currency=GBP")
        val test = deeplinkProcessorV2Subject.process(assetBuyTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetBuyDestination &&
                (deeplinkResult.destination as Destination.AssetBuyDestination).networkTicker == "BTC" &&
                (deeplinkResult.destination as Destination.AssetBuyDestination).fiatTicker == "GBP" &&
                (deeplinkResult.destination as Destination.AssetBuyDestination).amount == "50"
        }
    }

    @Test
    fun `given receive deeplink uri when it has a crypto ticker then destination is Receive`() {
        val receiveUrl = Uri.parse("https://www.login.blockchain.com/app/asset/receive?code=BTC")
        val test = deeplinkProcessorV2Subject.process(receiveUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetReceiveDestination &&
                (deeplinkResult.destination as Destination.AssetReceiveDestination).networkTicker == "BTC"
        }
    }

    @Test
    fun `given receive deeplink uri when it does not have a crypto ticker then destination is Unknown`() {
        val receiveUrl = Uri.parse("https://www.login.blockchain.com/app/asset/receive")
        val test = deeplinkProcessorV2Subject.process(receiveUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultUnknownLink &&
                deeplinkResult.uri == receiveUrl
        }
    }

    @Test
    fun `given sell deeplink uri when it has a crypto ticker then destination is Receive`() {
        val sellUrl = Uri.parse("https://www.login.blockchain.com/app/asset/sell?code=BTC")
        val test = deeplinkProcessorV2Subject.process(sellUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetSellDestination &&
                (deeplinkResult.destination as Destination.AssetSellDestination).networkTicker == "BTC"
        }
    }

    @Test
    fun `given sell deeplink uri when it does not have a crypto ticker then destination is Unknown`() {
        val sellUrl = Uri.parse("https://www.login.blockchain.com/app/asset/sell")
        val test = deeplinkProcessorV2Subject.process(sellUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultUnknownLink &&
                deeplinkResult.uri == sellUrl
        }
    }

    @Test
    fun `given swap deeplink uri when it does not have a crypto ticker then destination is Unknown`() {
        val swapUrl = Uri.parse("https://www.login.blockchain.com/app/asset/swap")
        val test = deeplinkProcessorV2Subject.process(swapUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultUnknownLink &&
                deeplinkResult.uri == swapUrl
        }
    }

    @Test
    fun `given swap deeplink uri when it has a crypto ticker then destination is Receive`() {
        val swapUrl = Uri.parse("https://www.login.blockchain.com/app/asset/swap?code=BTC")
        val test = deeplinkProcessorV2Subject.process(swapUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetSwapDestination &&
                (deeplinkResult.destination as Destination.AssetSwapDestination).networkTicker == "BTC"
        }
    }

    @Test
    fun `given rewards deposit deeplink uri when it does not have a crypto ticker then destination is Unknown`() {
        val depositUrl = Uri.parse("https://www.login.blockchain.com/app/asset/rewards/deposit")
        val test = deeplinkProcessorV2Subject.process(depositUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultUnknownLink &&
                deeplinkResult.uri == depositUrl
        }
    }

    @Test
    fun `given rewards deposit deeplink uri when it has a crypto ticker then destination is Rewards Deposit`() {
        val depositUrl = Uri.parse("https://www.login.blockchain.com/app/asset/rewards/deposit?code=BTC")
        val test = deeplinkProcessorV2Subject.process(depositUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.RewardsDepositDestination &&
                (deeplinkResult.destination as Destination.RewardsDepositDestination).networkTicker == "BTC"
        }
    }

    @Test
    fun `given rewards summary deeplink uri when it does not have a crypto ticker then destination is Unknown`() {
        val summaryUrl = Uri.parse("https://www.login.blockchain.com/app/asset/rewards/summary")
        val test = deeplinkProcessorV2Subject.process(summaryUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultUnknownLink &&
                deeplinkResult.uri == summaryUrl
        }
    }

    @Test
    fun `given rewards summary deeplink uri when it has a crypto ticker then destination is Rewards Summary`() {
        val summaryUrl = Uri.parse("https://www.login.blockchain.com/app/asset/rewards/summary?code=BTC")
        val test = deeplinkProcessorV2Subject.process(summaryUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.RewardsSummaryDestination &&
                (deeplinkResult.destination as Destination.RewardsSummaryDestination).networkTicker == "BTC"
        }
    }

    @Test
    fun `given fiat deposit summary deeplink uri when it has a fiat ticker then destination is FiatDeposit`() {
        val depositUrl = Uri.parse("https://www.login.blockchain.com/app/fiat/deposit?currency=GBP")
        val test = deeplinkProcessorV2Subject.process(depositUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.FiatDepositDestination &&
                (deeplinkResult.destination as Destination.FiatDepositDestination).fiatTicker == "GBP"
        }
    }

    @Test
    fun `given fiat deposit summary deeplink uri when no fiat ticker then destination is Unknown`() {
        val depositUrl = Uri.parse("https://www.login.blockchain.com/app/fiat/deposit")
        val test = deeplinkProcessorV2Subject.process(depositUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultUnknownLink
        }
    }

    @Test
    fun `given add card deeplink uri then destination is Add card`() {
        val depositUrl = Uri.parse("https://www.login.blockchain.com/app/settings/add/card")
        val test = deeplinkProcessorV2Subject.process(depositUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.SettingsAddCardDestination
        }
    }

    @Test
    fun `given add bank deeplink uri then destination is Add bank`() {
        val depositUrl = Uri.parse("https://www.login.blockchain.com/app/settings/add/bank")
        val test = deeplinkProcessorV2Subject.process(depositUrl).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.SettingsAddBankDestination
        }
    }

    @Test
    fun `test parse of activityView deeplink URI`() {
        val activityViewTestURL = Uri.parse("https://www.login.blockchain.com/app/activity")
        val test = deeplinkProcessorV2Subject.process(activityViewTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.ActivityDestination
        }
    }

    @Test
    fun `test parse of assetSend deeplink URI`() {
        val assetSendTestURL = Uri.parse(
            "https://www.login.blockchain.com/app/asset/send?" +
                "code=BTC&" +
                "amount=0.1&" +
                "address=2bIRbcq3xIgHSUgBaWenCCU0jh6KI2F2cf"
        )
        val test = deeplinkProcessorV2Subject.process(assetSendTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetSendDestination &&
                (deeplinkResult.destination as Destination.AssetSendDestination).networkTicker == "BTC" &&
                (deeplinkResult.destination as Destination.AssetSendDestination).amount == "0.1" &&
                (deeplinkResult.destination as Destination.AssetSendDestination).accountAddress == "2bIRbcq3xIgHSUgBaWenCCU0jh6KI2F2cf"
        }
    }

    @Test
    fun `test parse of link new card deeplink URI`() {
        val assetNewCardTestURL = Uri.parse(
            "https://www.login.blockchain.com/app/transaction/try/different/card?code=BTC"
        )
        val test = deeplinkProcessorV2Subject.process(assetNewCardTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetEnterAmountLinkCardDestination &&
                (deeplinkResult.destination as Destination.AssetEnterAmountLinkCardDestination).networkTicker == "BTC"
        }
    }

    @Test
    fun `test parse of choose new payment method deeplink URI`() {
        val assetNewPaymentMethodTestURL = Uri.parse(
            "https://www.login.blockchain.com/app/transaction/try/different/payment_method?code=BTC"
        )
        val test = deeplinkProcessorV2Subject.process(assetNewPaymentMethodTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetEnterAmountNewMethodDestination &&
                (deeplinkResult.destination as Destination.AssetEnterAmountNewMethodDestination).networkTicker == "BTC"
        }
    }

    @Test
    fun `test parse of kyc deeplink URI`() {
        val assetKycTestURL = Uri.parse(
            "https://www.login.blockchain.com/app/kyc"
        )
        val test = deeplinkProcessorV2Subject.process(assetKycTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess && deeplinkResult.destination is Destination.StartKycDestination
        }
    }

    @Test
    fun `test parse of back to enter amount deeplink URI`() {
        val assetBackToEnterAmountTestURL = Uri.parse(
            "https://www.login.blockchain.com/app/transaction/back/to/enter_amount?code=BTC"
        )
        val test = deeplinkProcessorV2Subject.process(assetBackToEnterAmountTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.AssetEnterAmountDestination &&
                (deeplinkResult.destination as Destination.AssetEnterAmountDestination).networkTicker == "BTC"
        }
    }

    @Test
    fun `test parse of customer support deeplink URI`() {
        val customerSupportTestURL = Uri.parse(
            "https://www.login.blockchain.com/app/contact/customer/support"
        )
        val test = deeplinkProcessorV2Subject.process(customerSupportTestURL).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.CustomerSupportDestination
        }
    }

    @Test
    fun `test parse of referral code deeplink URI`() {
        val referralCodeUri = Uri.parse(
            "https://www.login.blockchain.com/app/referral"
        )
        val test = deeplinkProcessorV2Subject.process(referralCodeUri).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.ReferralDestination
        }
    }

    @Test
    fun `test parse of external link deeplink URI`() {
        val expectedUrl = "https://www.google.com"
        val externalLinkUri = Uri.parse(expectedUrl)

        val test = deeplinkProcessorV2Subject.process(externalLinkUri).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultUnknownLink && deeplinkResult.uri == externalLinkUri
        }
    }

    @Test
    fun `test parse of dashboard deeplink URI`() {
        val externalLinkUri = Uri.parse(
            "https://www.login.blockchain.com/app/go/to/dashboard"
        )

        val test = deeplinkProcessorV2Subject.process(externalLinkUri).test()
        test.assertValue { deeplinkResult ->
            deeplinkResult is DeepLinkResult.DeepLinkResultSuccess &&
                deeplinkResult.destination is Destination.DashboardDestination
        }
    }
}
