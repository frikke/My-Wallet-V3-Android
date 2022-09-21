package com.blockchain.componentlib.system

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark200
import com.blockchain.componentlib.theme.Dark700
import com.blockchain.componentlib.theme.Grey600

data class DialogueButton(
    val text: String,
    val onClick: () -> Unit
)

@Composable
fun DialogueCard(
    @DrawableRes icon: Int = ResourcesCompat.ID_NULL,
    title: String? = null,
    body: String,
    firstButton: DialogueButton,
    secondButton: DialogueButton? = null
) {

    val backgroundColor = if (!isSystemInDarkTheme()) {
        Color.White
    } else {
        Dark700
    }

    val bodyColor = if (!isSystemInDarkTheme()) {
        Grey600
    } else {
        Dark200
    }

    val headerAlignment = if (icon == ResourcesCompat.ID_NULL) {
        Alignment.Start
    } else {
        Alignment.CenterHorizontally
    }

    Card(
        elevation = 2.dp,
        shape = AppTheme.shapes.small,
        backgroundColor = backgroundColor,
        modifier = Modifier
            .padding(dimensionResource(R.dimen.smallest_spacing))
            .defaultMinSize(280.dp)
    ) {
        Surface(
            modifier = Modifier
                .padding(
                    top = dimensionResource(R.dimen.standard_spacing),
                    bottom = dimensionResource(R.dimen.medium_spacing),
                    start = dimensionResource(R.dimen.standard_spacing),
                    end = dimensionResource(R.dimen.standard_spacing)
                )
                .background(backgroundColor),
        ) {
            Column(
                modifier = Modifier.background(backgroundColor),
                horizontalAlignment = headerAlignment
            ) {
                if (icon != ResourcesCompat.ID_NULL) {
                    Image(
                        modifier = Modifier.padding(bottom = 8.dp),
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.Black)
                    )
                }

                if (title != null) {
                    Text(
                        modifier = Modifier
                            .background(backgroundColor)
                            .padding(top = 8.dp),
                        text = title,
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.title
                    )
                }

                Text(
                    modifier = Modifier
                        .background(backgroundColor)
                        .padding(
                            top = dimensionResource(R.dimen.medium_spacing),
                            bottom = dimensionResource(R.dimen.large_spacing)
                        ),
                    text = body,
                    style = AppTheme.typography.paragraph1,
                    color = bodyColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = firstButton.onClick) {
                        Text(firstButton.text, style = AppTheme.typography.paragraph2, color = AppTheme.colors.primary)
                    }

                    if (secondButton != null) {
                        TextButton(onClick = secondButton.onClick) {
                            Text(
                                secondButton.text,
                                style = AppTheme.typography.paragraph2,
                                color = AppTheme.colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DialogueCardPreview() {
    AppTheme {
        DialogueCard(
            body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ",
            firstButton = DialogueButton("Button 1", {})
        )
    }
}
