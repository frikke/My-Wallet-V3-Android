package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun TableRowText(
    startText: AnnotatedString,
    endText: AnnotatedString?,
    textStyle: TextStyle,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Text(
            text = startText,
            style = textStyle,
            modifier = Modifier.weight(1f),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (endText != null) {
            Spacer(Modifier.size(width = AppTheme.dimensions.xxxPaddingLarge, height = 0.dp))
            Text(
                text = endText,
                style = textStyle,
                modifier = Modifier.wrapContentSize(),
                color = textColor
            )
        }
    }
}

@Preview
@Composable
fun TableRowText_Basic() {
    AppTheme {
        Surface(color = Color.White) {
            TableRowText(
                startText = buildAnnotatedString {
                    append("Starting text")
                },
                endText = buildAnnotatedString {
                    append("Ending text")
                },
                textStyle = AppTheme.typography.body2,
                textColor = AppTheme.colors.title
            )
        }
    }
}
