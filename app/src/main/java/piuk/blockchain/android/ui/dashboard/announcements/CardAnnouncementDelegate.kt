package piuk.blockchain.android.ui.dashboard.announcements

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.CardButton
import piuk.blockchain.android.databinding.CtaAnnouncementCardViewBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class CardAnnouncementDelegate<in T>(private val analytics: Analytics) : AdapterDelegate<T> {

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) {
        val announcement = items[position] as ComponentAnnouncementCard

        (holder as BorderedCardAnnouncementViewHolder).bind(announcement, analytics)
        analytics.logEvent(AnnouncementAnalyticsEvent.CardShown(announcement.name))
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position]
        return item is ComponentAnnouncementCard
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {

        val binding = CtaAnnouncementCardViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BorderedCardAnnouncementViewHolder(binding)
    }

    private class BorderedCardAnnouncementViewHolder constructor(
        binding: CtaAnnouncementCardViewBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        val container = binding.root
        fun bind(announcement: ComponentAnnouncementCard, analytics: Analytics) {

            with(container) {
                header = getHeader(announcement)
                subheader = getSubHeader(announcement)
                title = getTitle(announcement)
                if (announcement.background != 0) {
                    container.setBackgroundResource(announcement.background)
                } else {
                    container.setBackgroundColor(android.graphics.Color.WHITE)
                }

                body = getBody(announcement)
                iconResource = getImage(announcement)
                callToActionButton = getCTAButton(announcement, analytics)
                borderColor = Color(announcement.buttonColor)
                onClose = {
                    if (announcement.dismissRule != DismissRule.CardPersistent) {
                        analytics.logEvent(AnnouncementAnalyticsEvent.CardDismissed(announcement.name))
                        announcement.dismissClicked()
                    }
                }
            }
        }

        private fun getHeader(announcement: ComponentAnnouncementCard) =
            when {
                announcement.headerText != 0 -> context.getString(
                    announcement.headerText,
                    *announcement.headerFormatParams
                )
                announcement.header != null -> announcement.header
                else -> ""
            }

        private fun getSubHeader(announcement: ComponentAnnouncementCard) =
            buildAnnotatedString {
                when {
                    announcement.subHeaderText != 0 && announcement.subHeaderSuffixText != 0 -> {
                        appendSubHeader(this, announcement)
                        appendSubHeaderSuffix(this, announcement)
                    }
                    announcement.subHeaderText != 0 -> {
                        appendSubHeader(this, announcement)
                    }
                    announcement.subHeader != null -> announcement.subHeader
                    else -> { }
                }
            }

        private fun appendSubHeader(
            builder: AnnotatedString.Builder,
            announcement: ComponentAnnouncementCard
        ) {
            builder.appendWithStyle(
                text = context.getString(
                    announcement.subHeaderText,
                    *announcement.subheaderFormatParams
                ),
                spanStyle = announcement.subHeaderColour.takeIf { it != 0 }?.let { subHeaderColour ->
                    SpanStyle(
                        color = Color(
                            ContextCompat.getColor(context, subHeaderColour)
                        )
                    )
                }
            )
        }

        private fun appendSubHeaderSuffix(
            builder: AnnotatedString.Builder,
            announcement: ComponentAnnouncementCard
        ) {
            val subHeaderSuffix = context.getString(announcement.subHeaderSuffixText)
            builder.appendWithStyle(
                text = " $subHeaderSuffix",
                spanStyle = announcement.subHeaderSuffixColour.takeIf { it != 0 }?.let { subHeaderSuffixColour ->
                    SpanStyle(color = Color(ContextCompat.getColor(context, subHeaderSuffixColour)))
                }
            )
        }

        private fun AnnotatedString.Builder.appendWithStyle(text: String, spanStyle: SpanStyle? = null) {
            spanStyle?.let {
                withStyle(style = spanStyle) {
                    append(text)
                }
            } ?: append(text)
        }

        private fun getBody(announcement: ComponentAnnouncementCard) =
            when {
                announcement.bodyText != 0 -> context.getString(
                    announcement.bodyText, *announcement.bodyFormatParams
                )
                announcement.body != null -> announcement.body
                // TODO(dtverdota): Add support for spannable: announcement.bodyTextSpannable != null
                else -> ""
            }

        private fun getCTAButton(
            announcement: ComponentAnnouncementCard,
            analytics: Analytics
        ) = if (announcement.ctaText != 0) {
            CardButton(
                text = context.getString(announcement.ctaText, *announcement.ctaFormatParams),
                backgroundColor = Color(announcement.buttonColor),
                onClick = {
                    analytics.logEvent(AnnouncementAnalyticsEvent.CardActioned(announcement.name))
                    announcement.ctaClicked()
                }
            )
        } else {
            CardButton("")
        }

        private fun getImage(announcement: ComponentAnnouncementCard) = when {
            announcement.iconImage != 0 -> {
                check(announcement.iconUrl.isEmpty()) { "Can't set both a drawable and a URL on an announcement" }
                ImageResource.Local(announcement.iconImage)
            }
            announcement.iconUrl.isNotEmpty() -> {
                check(announcement.iconImage == 0) { "Can't set both a drawable and a URL on an announcement" }
                ImageResource.Remote(url = announcement.iconUrl)
            }
            else -> ImageResource.None
        }

        private fun getTitle(announcement: ComponentAnnouncementCard) =
            when {
                announcement.titleText != 0 -> context.getString(
                    announcement.titleText,
                    *announcement.titleFormatParams
                )
                announcement.title != null -> announcement.title
                else -> ""
            }
    }
}
