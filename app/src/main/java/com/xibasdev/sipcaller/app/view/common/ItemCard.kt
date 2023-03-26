package com.xibasdev.sipcaller.app.view.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun ItemCard(
    itemIndex: Int,
    visibleProvider: () -> Boolean,
    modifier: Modifier,
    content: @Composable (contentHeight: Dp, actionButtonsHeight: Dp) -> Unit
) {

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth()
            .wrapContentHeight()
    ) {
        val primaryContentHeight = 50.dp
        val secondaryContentHeight = 30.dp
        val bottomGapHeight = 2.dp

        val visible = visibleProvider()

        if (!visible) {
            Spacer(
                modifier = Modifier.fillMaxWidth()
                    .height(primaryContentHeight + secondaryContentHeight)
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = expandIn(
                expandFrom = Alignment.TopCenter,
                initialSize = { IntSize(0, 100) }
            ) + fadeIn(),
            exit = shrinkOut(
                shrinkTowards = Alignment.CenterEnd,
                targetSize = { IntSize(0, 100) }
            ) + fadeOut(),
            modifier = Modifier.fillMaxWidth()
                .height(primaryContentHeight + secondaryContentHeight)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .clip(shape = RoundedCornerShape(7.dp))
                    .background(color = if (itemIndex == 0) Color.Black else Color.DarkGray)
            ) {
                content(primaryContentHeight, secondaryContentHeight - bottomGapHeight)

                Spacer(modifier = Modifier.padding(bottom = bottomGapHeight))
            }
        }
    }
}
