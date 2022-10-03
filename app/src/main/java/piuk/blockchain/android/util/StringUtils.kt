package piuk.blockchain.android.util

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.ContextCompat
import com.blockchain.componentlib.theme.AppTheme

class StringUtils(private val context: Context) {

    @Deprecated("Don't be get getting strings in non-UI code")
    fun getString(@StringRes stringId: Int): String {
        return context.getString(stringId)
    }

    @Deprecated("Use the companion version")
    fun getStringWithMappedAnnotations(
        @StringRes stringId: Int,
        linksMap: Map<String, Uri?>,
        ctx: Context,
        onClick: () -> Unit = {}
    ): CharSequence = getStringWithMappedAnnotations(ctx, stringId, linksMap, onClick)

    companion object {
        private const val EMPTY_SPACE = " "
        const val TAG_URL = "link"

        @Composable
        fun Spanned.toAnnotatedString(
            linksMap: Map<String, String>? = null
        ): AnnotatedString {
            val primaryColor = AppTheme.colors.primary
            return remember {
                buildAnnotatedString {
                    val rawText = this@toAnnotatedString
                    append(rawText.toString())

                    for (span in rawText.getSpans(0, rawText.length, Any::class.java)) {
                        val start = rawText.getSpanStart(span)
                        val end = rawText.getSpanEnd(span)
                        when (span) {
                            is StyleSpan -> when (span.style) {
                                Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                                Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                                Typeface.BOLD_ITALIC -> addStyle(
                                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end
                                )
                            }
                            is UnderlineSpan -> addStyle(
                                SpanStyle(textDecoration = TextDecoration.Underline), start, end
                            )
                            is ForegroundColorSpan -> addStyle(
                                SpanStyle(color = Color(span.foregroundColor)), start, end
                            )
                            is android.text.Annotation -> {
                                val url = linksMap?.get(span.value)
                                if (span.key == "link" && url != null) {
                                    addStringAnnotation(
                                        tag = TAG_URL,
                                        annotation = url,
                                        start = start,
                                        end = end
                                    )
                                    addStyle(SpanStyle(color = primaryColor), start, end)
                                }
                                if (span.key == "font" && span.value == "bold") {
                                    addStyle(
                                        SpanStyle(fontWeight = FontWeight.Bold),
                                        start,
                                        end
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun getAnnotatedStringWithMappedAnnotations(
            context: Context,
            @StringRes stringId: Int,
            linksMap: Map<String, String>
        ): AnnotatedString {
            val text = context.getText(stringId)
            val rawText = text as? SpannedString ?: return AnnotatedString(text.toString())

            return rawText.toAnnotatedString(linksMap)
        }

        fun getStringWithMappedAnnotations(
            context: Context,
            @StringRes stringId: Int,
            linksMap: Map<String, StringAnnotationClickEvent>
        ): CharSequence {

            val text = context.getText(stringId)
            val rawText = text as? SpannedString ?: return text
            val out = SpannableString(rawText)
            for (annotation in rawText.getSpans(0, rawText.length, android.text.Annotation::class.java)) {
                if (annotation.key == "link") {
                    out.setSpan(
                        ClickableSpanWithoutUnderline {
                            linksMap[annotation.value]?.let { clickEvent ->
                                when (clickEvent) {
                                    is StringAnnotationClickEvent.OpenUri -> {
                                        Intent(Intent.ACTION_VIEW, clickEvent.uri)
                                            .apply { addFlags(FLAG_ACTIVITY_NEW_TASK) }
                                            .also { context.startActivity(it) }
                                    }

                                    is StringAnnotationClickEvent.CustomCta -> {
                                        clickEvent()
                                    }

                                    StringAnnotationClickEvent.NoEvent -> {
                                        // no action
                                    }
                                }
                            }
                        },
                        rawText.getSpanStart(annotation),
                        rawText.getSpanEnd(annotation),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (annotation.key == "font" && annotation.value == "bold") {
                    out.setSpan(
                        StyleSpan(Typeface.BOLD),
                        rawText.getSpanStart(annotation),
                        rawText.getSpanEnd(annotation),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            return out
        }

        // TODO (othman): refactor to use the one with StringAnnotationClickEvent,
        //  will create a pr following this one
        @Deprecated("use the one with StringAnnotationClickEvent")
        fun getStringWithMappedAnnotations(
            context: Context,
            @StringRes stringId: Int,
            linksMap: Map<String, Uri?>,
            onClick: () -> Unit = {}
        ): CharSequence {

            val text = context.getText(stringId)
            val rawText = text as? SpannedString ?: return text
            val out = SpannableString(rawText)
            for (annotation in rawText.getSpans(0, rawText.length, android.text.Annotation::class.java)) {
                if (annotation.key == "link") {
                    out.setSpan(
                        ClickableSpanWithoutUnderline {
                            linksMap[annotation.value]?.let {
                                val intent = Intent(Intent.ACTION_VIEW, it).addFlags(FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                            onClick()
                        },
                        rawText.getSpanStart(annotation),
                        rawText.getSpanEnd(annotation),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (annotation.key == "font" && annotation.value == "bold") {
                    out.setSpan(
                        StyleSpan(Typeface.BOLD),
                        rawText.getSpanStart(annotation),
                        rawText.getSpanEnd(annotation),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            return out
        }

        fun getResolvedStringWithAppendedMappedLearnMore(
            staticText: String,
            @StringRes textToMap: Int,
            url: String,
            context: Context,
            @ColorRes linkColour: Int,
            onClick: () -> Unit = {}
        ): SpannableStringBuilder {
            val map = mapOf("learn_more_link" to Uri.parse(url))
            val learnMoreLink = getStringWithMappedAnnotations(
                context,
                textToMap,
                map,
                onClick
            )

            val sb = SpannableStringBuilder()
                .append(staticText)
                .append(EMPTY_SPACE)
                .append(learnMoreLink)
            sb.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, linkColour)),
                staticText.length, staticText.length + EMPTY_SPACE.length + learnMoreLink.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return sb
        }
    }
}

class ClickableSpanWithoutUnderline(val onClick: () -> Unit) : ClickableSpan() {
    override fun onClick(widget: View) {
        onClick.invoke()
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
    }
}

sealed interface StringAnnotationClickEvent {
    data class OpenUri(val uri: Uri) : StringAnnotationClickEvent

    data class CustomCta(val onClick: () -> Unit) : StringAnnotationClickEvent {
        operator fun invoke() = onClick()
    }

    object NoEvent : StringAnnotationClickEvent
}
