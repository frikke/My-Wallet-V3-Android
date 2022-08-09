package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.view.isVisible
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.AnnouncementCardView
import com.blockchain.componentlib.card.CallOutCardView
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.card.CtaAnnouncementCardView
import com.blockchain.componentlib.card.CustomBackgroundCardView
import com.blockchain.componentlib.card.DefaultCardView
import piuk.blockchain.blockchain_component_library_catalog.R

class CardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cards)

        findViewById<DefaultCardView>(R.id.default_card).apply {
            title = "Welcome to Blockchain!"
            subtitle = "This is your Portfolio view. Once you own and hold crypto, the balances display here."
            iconResource = ImageResource.Local(R.drawable.logo_bitcoin, null)
            onClose = {
                this.isVisible = false
            }
        }

        findViewById<DefaultCardView>(R.id.default_card_cta).apply {
            title = "Welcome to Blockchain!"
            subtitle = "This is your Portfolio view. Once you own and hold crypto, the balances display here."
            iconResource = ImageResource.Local(R.drawable.logo_bitcoin, null)
            callToActionButton = CardButton("Notify Me")
            onClose = {
                this.isVisible = false
            }
        }

        findViewById<CallOutCardView>(R.id.call_out_card).apply {
            title = "Buy More Crypto"
            subtitle = "Upgrade Your Wallet"
            iconResource = ImageResource.Local(R.drawable.logo_bitcoin, null)
            callToActionButton = CardButton("GO")
        }

        findViewById<AnnouncementCardView>(R.id.announcement_card).apply {
            title = "New Asset"
            subtitle = "Dogecoin (DOGE) is now available on Blockchain."
            iconResource = ImageResource.Local(R.drawable.logo_bitcoin, null)
            onClose = {
                this.isVisible = false
            }
        }

        val greenSubheader = "+$1.31 (5.22%)"
        val defaultSubheader = " Today"

        findViewById<CtaAnnouncementCardView>(R.id.cta_announcement_card).apply {
            header = "1 UNI = $21.19"
            subheader = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(0xFF17CE73))) {
                    append(greenSubheader)
                }
                withStyle(style = SpanStyle(color = Color(0xFF98A1B2))) {
                    append(defaultSubheader)
                }
            }
            title = "Uniswap (UNI) is Now Trading"
            body = "Exchange, deposit, withdraw, or store UNI in your Blockchain.com Exchange account."
            iconResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.logo_bitcoin, null)
            borderColor = Color(0xFFFF007A)
            callToActionButton = CardButton("Trade UNI", Color(0xFFFF007A))
            onClose = {
                this.isVisible = false
            }
        }

        findViewById<CustomBackgroundCardView>(R.id.cta_custom_bckg_card).apply {
            title = "Complete the promo"
            subtitle = "Verify your ID and refer some friends"
            iconResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_star)
            backgroundResource =
                ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_blockchain_logo_with_text)
            isCloseable = true
        }
    }
}
