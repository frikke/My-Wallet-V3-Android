package piuk.blockchain.blockchain_component_library_catalog.preview.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.card.AnnouncementCard
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.card.CtaAnnouncementCard
import com.blockchain.componentlib.card.DefaultCard
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default Card", group = "Card")
@Composable
fun DefaultCard_Example() {
    AppTheme {
        AppSurface {
            DefaultCard(
                title = "Welcome to Blockchain!",
                subtitle = "This is your Portfolio view. Once you own and hold crypto, the balances display here.",
                iconResource = ImageResource.Local(R.drawable.logo_bitcoin, null),
                onClose = {

                }
            )
        }
    }
}

@Preview(name = "Default Card+CTA", group = "ToastAlert")
@Composable
fun DefaultCard_Cta() {
    AppTheme {
        AppSurface {
            DefaultCard(
                title = "Welcome to Blockchain!",
                subtitle = "This is your Portfolio view. Once you own and hold crypto, the balances display here.",
                iconResource = ImageResource.Local(R.drawable.logo_bitcoin, null),
                callToActionButton = CardButton("Notify Me"),
                onClose = {

                }
            )
        }
    }
}

@Preview(name = "Announcement Card", group = "Card")
@Composable
fun AnnouncementCard_Example() {
    AppTheme {
        AppSurface {
            AnnouncementCard(
                title = "New Asset",
                subtitle = "Dogecoin (DOGE) is now available on Blockchain.",
                iconResource = ImageResource.Local(R.drawable.logo_bitcoin, null),
                onClose = {}
            )
        }
    }
}

@Preview(name = "CtaAnnouncement Card", group = "Card")
@Composable
fun CtaAnnouncementCard_Example() {
    AppTheme {
        AppSurface {
            CtaAnnouncementCard(
                header = "1 UNI = $21.19",
                subheader = AnnotatedString("+$1.31 (5.22%) Today"),
                title = "Uniswap (UNI) is Now Trading",
                body = "Exchange, deposit, withdraw, or store UNI in your Blockchain.com Exchange account.",
                iconResource = ImageResource.Local(R.drawable.logo_bitcoin, null),
                borderColor = Color(0xFFFF007A),
                callToActionButton = CardButton("Trade UNI", Color(0xFFFF007A)),
                onClose = {}
            )
        }
    }
}